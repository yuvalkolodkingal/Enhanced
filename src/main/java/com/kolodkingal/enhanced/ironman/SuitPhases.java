package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The suit-up and the take-off (3.8, PLAN 5.8): a phase UP or DOWN of one suit, {@code phaseTicks} long, kept in {@link
 * IronManState} (synced to everyone who sees him, so a player who starts tracking him mid-phase draws it from its
 * current tick) with {@link Live#IRON_SUITING} set while it plays and an {@link IronFxPayload} SUIT_UP / SUIT_DOWN /
 * SUIT_SKIP for the sounds and particles.
 *
 * <p>The rules that keep it from being an exploit: a new phase cannot begin within {@code suitPhaseCooldown} ticks of
 * the last one's end ("The suit is still settling."); during UP incoming damage is only cut ({@code
 * suitUpDamageFactor}, never to nothing), and never during DOWN; H during a phase skips it to its end (and so ends the
 * protection that very tick). He is rooted through both (the nanotech suits' UP at half speed), uses no item, flies and
 * moves nothing. Anything that interrupts it (death, the power lost, a new dimension, a logout, the reactor taken off)
 * {@link #complete completes} it at once, silently.
 *
 * <p>Where the item is: a classic suit is moved to the chest slot at the <em>start</em> of UP (a crash in the middle
 * never loses it) and off it at the <em>end</em> of DOWN; a nanotech suit is {@link IronManState#NANO_OUT} from the
 * start of UP to the end of DOWN. {@link SuitControl} does the moving; this class keeps the time. Lane L4's Hulkbuster
 * (Veronica's frame, the growth) plugs in with a {@link Listener}.
 */
public final class SuitPhases {
    /** What another lane does at a suit's phases (lane L4's Hulkbuster). */
    public interface Listener {
        /**
         * Whether the phase may begin; when not, the listener tells him why.
         *
         * @param byHand the suit was put on the chest slot by hand (not by H or by using the item)
         */
        default boolean mayBegin(ServerPlayer player, SuitKind kind, boolean up, boolean byHand) {
            return true;
        }

        /** The phase has begun (the state is set). */
        default void begun(ServerPlayer player, SuitKind kind, boolean up) {
        }

        /** Every tick of the phase ({@code tick} 1 to {@code ticks}), before the end is checked. */
        default void ticked(ServerPlayer player, SuitKind kind, boolean up, int tick, int ticks) {
        }

        /** The phase has ended: {@code completed} (to its end state) or aborted (the suit off). */
        default void ended(ServerPlayer player, SuitKind kind, boolean up, boolean completed) {
        }
    }

    private static final Map<SuitKind, List<Listener>> LISTENERS = new EnumMap<>(SuitKind.class);
    /** When each Iron Man's last phase ended (game time): the next waits {@code suitPhaseCooldown}. */
    private static final Map<UUID, Long> LAST_END = new HashMap<>();
    /** Whether a phase that is playing was begun by hand (the Hulkbuster's short version). */
    private static final Map<UUID, Boolean> BY_HAND = new HashMap<>();

    /** Lane L4 (or any other) listens to a suit's phases. */
    public static void listen(SuitKind kind, Listener listener) {
        LISTENERS.computeIfAbsent(kind, k -> new ArrayList<>()).add(listener);
    }

    private static List<Listener> listeners(SuitKind kind) {
        return LISTENERS.getOrDefault(kind, List.of());
    }

    /** The ticks a phase of this suit lasts. */
    public static int length(SuitKind kind, boolean up) {
        return up ? kind.upTicks() : kind.downTicks();
    }

    /** Begins a phase of its usual length (by H or by using the item). */
    public static boolean begin(ServerPlayer player, SuitKind kind, boolean up) {
        return begin(player, kind, up, length(kind, up), false);
    }

    /** Begins a phase of {@code ticks} (the Hulkbuster's by-hand version is shorter). */
    public static boolean begin(ServerPlayer player, SuitKind kind, boolean up, int ticks) {
        return begin(player, kind, up, ticks, false);
    }

    /**
     * Begins a phase, unless one plays, the last ended less than {@code suitPhaseCooldown} ago ("The suit is still
     * settling.") or a listener refuses. The caller then moves the item (UP: onto the chest at once).
     */
    public static boolean begin(ServerPlayer player, SuitKind kind, boolean up, int ticks, boolean byHand) {
        if (!mayBegin(player, kind, up, byHand)) {
            return false;
        }
        long now = player.level().getGameTime();
        IronManState state = IronSuits.state(player);
        SuitKind worn = IronSuits.worn(player);
        if (worn != null) {
            IronManAbilities.endMoves(player, worn);
        }
        IronSuits.setState(player, state.withPhase(up ? IronManState.UP : IronManState.DOWN, kind, now,
                Math.max(1, ticks)).with(IronManState.POWERED, false));
        BY_HAND.put(player.getUUID(), byHand);
        PowerData data = Powers.get(player);
        data.set(Live.IRON_SUITING, true);
        player.stopUsingItem();
        IronManAbilities.send(player.serverLevel(), player, up ? IronFxPayload.SUIT_UP : IronFxPayload.SUIT_DOWN,
                List.of(player.position()), kind.ordinal());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound(kind, up),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        for (Listener l : listeners(kind)) {
            l.begun(player, kind, up);
        }
        Powers.syncIfDirty(player);
        return true;
    }

    /** Whether a phase may begin now (no phase, the cooldown over, the listeners agree); tells him why not. */
    public static boolean mayBegin(ServerPlayer player, SuitKind kind, boolean up, boolean byHand) {
        if (IronSuits.state(player).inPhase()) {
            return false;
        }
        if (settling(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.iron_man.settling")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        for (Listener l : listeners(kind)) {
            if (!l.mayBegin(player, kind, up, byHand)) {
                return false;
            }
        }
        return true;
    }

    /** Whether the last phase ended less than {@code suitPhaseCooldown} ticks ago. */
    public static boolean settling(ServerPlayer player) {
        Long last = LAST_END.get(player.getUUID());
        long now = player.level().getGameTime();
        int cooldown = IronManAbilities.cfg(IronManConfig.SUIT_PHASE_COOLDOWN, 40);
        return last != null && now >= last && now - last < cooldown;
    }

    /** Whether the phase that plays was begun by hand. */
    public static boolean byHand(ServerPlayer player) {
        return BY_HAND.getOrDefault(player.getUUID(), false);
    }

    /** The phase's tick (1 on the first tick after it began), or 0 when none plays. */
    public static int elapsed(ServerPlayer player) {
        IronManState state = IronSuits.state(player);
        return state.inPhase() ? (int) Math.max(0, player.level().getGameTime() - state.phaseStart()) : 0;
    }

    /** Every tick of an Iron Man: the phase runs on, and ends at its length. */
    static void tick(ServerPlayer player, long now) {
        IronManState state = IronSuits.state(player);
        if (!state.inPhase()) {
            return;
        }
        SuitKind kind = state.phaseKind();
        if (kind == null) {
            finish(player, true, true);
            return;
        }
        int tick = (int) Math.max(0, now - state.phaseStart());
        boolean up = state.phase() == IronManState.UP;
        for (Listener l : listeners(kind)) {
            l.ticked(player, kind, up, tick, state.phaseTicks());
        }
        state = IronSuits.state(player);
        if (state.inPhase() && now - state.phaseStart() >= state.phaseTicks()) {
            finish(player, true, false);
        }
    }

    /** H during a phase: it jumps to its end (the protection ends this very tick). */
    public static void skip(ServerPlayer player) {
        IronManState state = IronSuits.state(player);
        SuitKind kind = state.phaseKind();
        if (kind == null) {
            return;
        }
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.SUIT_SKIP, List.of(player.position()),
                kind.ordinal());
        finish(player, true, false);
    }

    /** Something interrupted the phase (death, the power lost, a dimension, a logout): it completes at once. */
    public static void complete(ServerPlayer player, boolean silent) {
        if (IronSuits.state(player).inPhase()) {
            finish(player, true, silent);
        }
    }

    /** The phase is given up: the suit ends off (a Hulkbuster with no room steps out). */
    public static void abort(ServerPlayer player) {
        if (IronSuits.state(player).inPhase()) {
            finish(player, false, true);
        }
    }

    /**
     * The end of a phase. UP completed: the suit is on and works from the next tick (the HUD boots). DOWN completed, or
     * either aborted: the suit comes off (a classic one into the inventory, the Mark 5 folded into the hand, a nanotech
     * one back into the reactor).
     */
    private static void finish(ServerPlayer player, boolean completed, boolean silent) {
        IronManState state = IronSuits.state(player);
        SuitKind kind = state.phaseKind();
        boolean up = state.phase() == IronManState.UP;
        IronSuits.setState(player, state.withPhase(IronManState.NONE, null, 0L, 0));
        LAST_END.put(player.getUUID(), player.level().getGameTime());
        BY_HAND.remove(player.getUUID());
        PowerData data = Powers.get(player);
        data.set(Live.IRON_SUITING, false);
        if (kind != null) {
            if (!up || !completed) {
                SuitControl.takeOff(player, kind);
            } else {
                award(player);
                if (!silent) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.IRON_HUD_BOOT.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
                }
            }
            for (Listener l : listeners(kind)) {
                l.ended(player, kind, up, completed);
            }
        }
        Powers.syncIfDirty(player);
    }

    /** The advancement "Suit up" (its criterion is granted here, at the end of his first suit-up). */
    private static void award(ServerPlayer player) {
        net.minecraft.advancements.AdvancementHolder advancement = player.server.getAdvancements()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("enhanced", "avengers/suit_up"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "granted");
        }
    }

    /** The damage he takes during a phase ({@code suitUpDamageFactor} of it during UP; all of it otherwise). */
    public static float protect(ServerPlayer player, float amount) {
        IronManState state = IronSuits.state(player);
        if (state.phase() != IronManState.UP) {
            return amount;
        }
        double factor = IronManAbilities.cfg(IronManConfig.SUIT_UP_DAMAGE_FACTOR, 0.5);
        return (float) (amount * Math.max(0.0, factor));
    }

    /** The sound a phase begins with (the choreographies' own clanks and whirs are lane L7's, on the client). */
    private static SoundEvent sound(SuitKind kind, boolean up) {
        return switch (kind) {
            case MARK_1 -> ModSounds.MARK1_CLANK.get();
            case MARK_5 -> up ? ModSounds.MARK5_SUITCASE_OPEN.get() : ModSounds.MARK5_SUITCASE_CLOSE.get();
            case MARK_50, MARK_85 -> up ? ModSounds.NANO_FLOW.get() : ModSounds.NANO_RETRACT.get();
            default -> up ? ModSounds.SUIT_UP_GENERIC.get() : ModSounds.SUIT_DOWN_GENERIC.get();
        };
    }

    static void forget(UUID id) {
        LAST_END.remove(id);
        BY_HAND.remove(id);
    }

    static void clear() {
        LAST_END.clear();
        BY_HAND.clear();
    }

    /** The phase's suit, or null. */
    @Nullable
    public static SuitKind playing(ServerPlayer player) {
        return IronSuits.state(player).phaseKind();
    }

    private SuitPhases() {
    }
}
