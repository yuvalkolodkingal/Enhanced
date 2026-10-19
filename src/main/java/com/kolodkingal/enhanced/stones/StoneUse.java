package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.StoneRow;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The single entry of a stone's use (3.6): the bare stone in a hand, the selected stone of a Gauntlet in a hand, a
 * vessel (Loki's Scepter, the Tesseract, the Eye of Agamotto) or a custom hero's stone row all come here. The 3.6
 * skeleton wrote the entry and the questions; lane L2 the body and every stone's power.
 *
 * <p>A press asks, in order: who may use the stone from where it is ({@link #refusal}), whether his hand is held
 * ({@link #restrained}), whether a switch of his is on (it can always be turned off), whether the stone's cooldown is
 * over ({@link StoneCooldowns}) and whether he has the food to feed it; then the stone acts. The uses that last while
 * the key or the use is held (the Mind Beam, the Purple Beam, the Soul Drain) are a {@link Hold}: one at a time, ticked
 * by {@link #tick}, ended by the release, the item leaving his hand, hunger or their own limit. Server thread only.
 */
public final class StoneUse {
    /** Where the stone that is used is. */
    public enum Source {
        /** The bare stone, in a hand. */
        BARE,
        /** The selected stone of a filled Gauntlet in a hand. */
        GAUNTLET,
        /** Its vessel: Loki's Scepter (Mind), the Tesseract (Space), the Eye of Agamotto (Time). */
        VESSEL,
        /** A custom hero's stone row ({@code StoneRowKeys}). */
        ROW
    }

    /**
     * One use of a stone: who, which stone, from where, which of its two uses ({@code alt}: the Sneak use), and for a
     * custom hero's row the key it is on (1-5; 0 for none), whose cooldown the HUD shows.
     */
    public record Cast(ServerPlayer player, PowerData data, StoneKind kind, Source source, boolean alt, int column) {
        /** The cooldown of this use: a custom hero's row stretches it by its strength and the server's scale. */
        public int cooldown(int ticks) {
            if (source != Source.ROW) {
                return ticks;
            }
            HeroKit kit = data.kit();
            float factor = kit == null ? 1.0F : kit.cooldownFactor(StoneRow.of(kind));
            double scale = EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.COOLDOWN_SCALE.get() : 1.0;
            return Math.max(0, Math.round(ticks * factor * (float) scale));
        }

        /** Starts the stone's cooldown (and, for a row, its key's on the HUD). */
        public void startCooldown(int ticks) {
            int scaled = cooldown(ticks);
            StoneCooldowns.start(player, kind, scaled);
            if (column > 0) {
                data.startCooldown(column, player.level().getGameTime(), scaled);
            }
        }

        public long now() {
            return player.level().getGameTime();
        }
    }

    /** A use that lasts while it is held: whose, since when, and how many ticks it has burned. */
    public static final class Hold {
        public final Cast cast;
        public final long started;
        public int ticks;

        Hold(Cast cast) {
            this.cast = cast;
            this.started = cast.now();
        }

        public StoneKind kind() {
            return cast.kind();
        }
    }

    /** Player -> the use he holds. */
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    /**
     * The player uses the stone ({@code alt}: its Sneak use; {@code down}: the key or the use went down, or came up for
     * a held power).
     *
     * @return whether a stone power took the use (it acted, or refused and said why); a press always is, a release
     * only if it let go of a hold of that stone
     */
    public static boolean use(ServerPlayer player, StoneKind kind, Source source, boolean alt, boolean down) {
        return use(player, kind, source, alt, down, 0);
    }

    /** {@link #use}, for a custom hero's stone row on the key {@code column}. */
    static boolean use(ServerPlayer player, StoneKind kind, Source source, boolean alt, boolean down, int column) {
        if (!down) {
            return release(player, kind);
        }
        if (player.isSpectator()) {
            return true;
        }
        PowerData data = Powers.get(player);
        // Loki's Scepter has one stone use: its Sneak is the Mind Beam, the stone's main use.
        boolean variant = alt && !(source == Source.VESSEL && kind == StoneKind.MIND);
        Cast cast = new Cast(player, data, kind, source, variant, column);
        Component refusal = refusal(player, kind, source);
        if (refusal != null) {
            player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        if (restrained(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.held"), true);
            return true;
        }
        // A switch that is on is turned off whatever the cooldown and the hunger say.
        if (kind == StoneKind.TIME && !variant && TimeStonePowers.slowing(player)) {
            TimeStonePowers.stopSlow(player, data, true);
            return true;
        }
        if (kind == StoneKind.REALITY && source == Source.ROW && !variant && data.toggle(Toggle.REALITY_VEIL)) {
            RealityBubbles.toggle(cast);
            return true;
        }
        if (HOLDS.containsKey(player.getUUID())) {
            return true; // (one held use at a time: a second key while a beam burns does nothing)
        }
        if (!StoneCooldowns.ready(player, kind, cast.now())) {
            return true;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.too_hungry"), true);
            return true;
        }
        switch (kind) {
            case REALITY -> {
                if (source == Source.ROW) {
                    if (variant) {
                        RealityBubbles.transmute(cast);
                    } else {
                        RealityBubbles.toggle(cast);
                    }
                } else if (variant) {
                    RealityBubbles.illusion(cast);
                } else {
                    RealityBubbles.transmute(cast);
                }
            }
            case MIND -> {
                if (variant) {
                    MindStonePowers.command(cast);
                } else {
                    MindStonePowers.beamStart(cast);
                }
            }
            case TIME -> {
                if (variant) {
                    TimeStonePowers.rewind(cast);
                } else {
                    TimeStonePowers.startSlow(cast);
                }
            }
            case POWER -> {
                if (variant) {
                    PowerBeam.bolt(cast);
                } else {
                    PowerBeam.beamStart(cast);
                }
            }
            case SPACE -> {
                if (variant) {
                    SpaceRealms.open(cast);
                } else {
                    SpaceRealms.step(cast);
                }
            }
            case SOUL -> {
                if (variant) {
                    SoulStonePowers.sight(cast);
                } else {
                    SoulStonePowers.drainStart(cast);
                }
            }
        }
        return true;
    }

    /**
     * Why the player may not use this stone from where it is, or null if he may. Bare: only one who may use it (the
     * Bearers; the Time Stone only a sorcerer). In a Gauntlet: one who may wield it (and the Time Stone only a
     * sorcerer). In its vessel: anyone, but the Eye opens only for a sorcerer. On a row: only while he carries it.
     * The Hex Runes silence a sorcerer's Time Stone wherever it is.
     */
    @Nullable
    public static Component refusal(Player player, StoneKind kind, Source source) {
        Component name = Component.translatable("item.enhanced." + kind.id() + "_stone");
        boolean sorcerer = Traits.has(player, Trait.SORCERER);
        if (kind == StoneKind.TIME && sorcerer && HexRunes.silences(player)) {
            return Component.translatable("message.enhanced.stones.use.runes");
        }
        return switch (source) {
            case BARE -> {
                if (!StoneRules.mayHold(player, kind)) {
                    yield Component.translatable("message.enhanced.stones.too_strong", name);
                }
                yield StoneRules.mayUse(player, kind) ? null
                        : Component.translatable("message.enhanced.stones.use.no_sorcerer");
            }
            case GAUNTLET -> {
                if (!StoneRules.mayWield(player)) {
                    yield Component.translatable("message.enhanced.gauntlet.not_yours");
                }
                yield kind != StoneKind.TIME || sorcerer ? null
                        : Component.translatable("message.enhanced.stones.use.no_sorcerer");
            }
            case VESSEL -> kind != StoneKind.TIME || sorcerer ? null
                    : Component.translatable("message.enhanced.stones.use.eye_closed");
            case ROW -> {
                if (kind == StoneKind.TIME && !sorcerer) {
                    yield Component.translatable("message.enhanced.stones.use.no_sorcerer");
                }
                yield carried(player, kind) ? null
                        : Component.translatable("message.enhanced.stones.not_carried", name);
            }
        };
    }

    /** With an empty hunger bar there is nothing to feed a stone (not in Creative). */
    public static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    /**
     * Whether a restrained hand cannot use a stone: held by Billy's spell, petrified, in the Soul World, time-locked,
     * pinned by Mjolnir (Thanos's rule).
     */
    public static boolean restrained(LivingEntity entity) {
        return ThanosAbilities.held(entity) || entity.hasEffect(ModEffects.TIME_LOCKED) || MjolnirPin.isPinned(entity);
    }

    // ---- the held uses ----

    /** Whether the player holds a use of a stone now (a beam, a drain). */
    public static boolean holding(LivingEntity entity) {
        return HOLDS.containsKey(entity.getUUID());
    }

    /** Whether the player holds a use of this stone now. */
    public static boolean holding(LivingEntity entity, StoneKind kind) {
        Hold hold = HOLDS.get(entity.getUUID());
        return hold != null && hold.kind() == kind;
    }

    /** The use the player holds, or null. */
    @Nullable
    public static Hold hold(Player player) {
        return HOLDS.get(player.getUUID());
    }

    /** A held use begins (the stone's power has started it). */
    static Hold begin(Cast cast) {
        Hold hold = new Hold(cast);
        HOLDS.put(cast.player().getUUID(), hold);
        return hold;
    }

    /** The key or the use came up: a hold of this stone ends (its cooldown for a release). */
    private static boolean release(ServerPlayer player, StoneKind kind) {
        Hold hold = HOLDS.get(player.getUUID());
        if (hold == null || hold.kind() != kind) {
            return false;
        }
        end(player, true);
        return true;
    }

    /**
     * Ends the player's hold, if he has one: {@code released} when he let go (or the item left his hand), false when
     * it ran out (its limit, hunger, the target): each stone starts the cooldown that fits.
     */
    public static void end(ServerPlayer player, boolean released) {
        Hold hold = HOLDS.remove(player.getUUID());
        if (hold == null) {
            return;
        }
        switch (hold.kind()) {
            case MIND -> MindStonePowers.beamEnd(hold, released);
            case POWER -> PowerBeam.beamEnd(hold, released);
            case SOUL -> SoulStonePowers.drainEnd(hold);
            default -> {
            }
        }
        if (hold.cast.source() != Source.ROW && player.isUsingItem() && player.getUseItem().is(holder(hold))) {
            player.stopUsingItem();
        }
    }

    /** The item a held use came from (what the player is using while it lasts). */
    private static Item holder(Hold hold) {
        return switch (hold.cast.source()) {
            case BARE -> ModItems.stone(hold.kind());
            case GAUNTLET -> ModItems.INFINITY_GAUNTLET.get();
            default -> {
                Item vessel = vessel(hold.kind());
                yield vessel != null ? vessel : ModItems.stone(hold.kind());
            }
        };
    }

    /**
     * Every tick of every player (alive, on the server): his hold burns or ends, the Reality Stone's bubbles, the Time
     * Stone's zone and history, the Soul Stone's sight and the Mind Stone's command run.
     */
    public static void tick(ServerPlayer player) {
        PowerData data = Powers.get(player);
        Hold hold = HOLDS.get(player.getUUID());
        if (hold != null) {
            hold.ticks++;
            boolean itemGone = hold.cast.source() != Source.ROW
                    && (!player.isUsingItem() || !player.getUseItem().is(holder(hold)));
            if (itemGone || player.isSpectator() || !player.isAlive()) {
                end(player, true);
            } else if (restrained(player) || starving(player)) {
                if (starving(player)) {
                    player.displayClientMessage(Component.translatable("message.enhanced.thanos.too_hungry"), true);
                }
                end(player, false);
            } else {
                boolean goesOn = switch (hold.kind()) {
                    case MIND -> MindStonePowers.beamTick(hold);
                    case POWER -> PowerBeam.beamTick(hold);
                    case SOUL -> SoulStonePowers.drainTick(hold);
                    default -> false;
                };
                if (!goesOn) {
                    end(player, false);
                }
            }
        }
        RealityBubbles.tick(player, data);
        TimeStonePowers.tick(player, data);
        SoulStonePowers.tick(player, data);
        MindStonePowers.tick(player);
        SpaceRealms.tick(player);
    }

    /** Everything the player's stones keep is forgotten (he logged out, died or the server stops). */
    public static void forget(ServerPlayer player) {
        Hold hold = HOLDS.remove(player.getUUID());
        if (hold != null && hold.kind() == StoneKind.SOUL) {
            SoulDrain.forget(player);
        } else if (hold != null) {
            ContinuousBeam.stop(player);
        }
        PowerData data = Powers.get(player);
        TimeStonePowers.forget(player, data);
        SoulStonePowers.forget(player, data);
        MindStonePowers.forget(player);
        RealityBubbles.forget(player);
        SpaceRealms.forget(player);
    }

    /** The server stopped. */
    public static void clear() {
        HOLDS.clear();
        StoneCooldowns.clear();
        TimeStonePowers.clear();
        SoulStonePowers.clear();
        MindStonePowers.clear();
        RealityBubbles.clear();
        SpaceRealms.clear();
        ThanosWants.clear();
    }

    // ---- what he carries ----

    /**
     * Whether the player carries the stone and may use it (a stone row's condition): the bare stone in his inventory,
     * in a filled Gauntlet he may wield, or its vessel (Loki's Scepter for the Mind Stone, the Tesseract for the Space
     * Stone, the Eye of Agamotto for the Time Stone, which only a sorcerer uses).
     */
    public static boolean carried(Player player, StoneKind kind) {
        if (!StoneRules.mayUse(player, kind) && vessel(kind) == null) {
            return false;
        }
        Item vessel = vessel(kind);
        for (ItemStack stack : player.getInventory().items) {
            if (carries(player, stack, kind, vessel)) {
                return true;
            }
        }
        return carries(player, player.getOffhandItem(), kind, vessel);
    }

    private static boolean carries(Player player, ItemStack stack, StoneKind kind, @Nullable Item vessel) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.stone(kind))) {
            return StoneRules.mayUse(player, kind);
        }
        if (vessel != null && stack.is(vessel)) {
            return kind != StoneKind.TIME || StoneRules.mayUse(player, kind);
        }
        GauntletContents gauntlet = stack.get(ModDataComponents.GAUNTLET.get());
        return stack.is(ModItems.INFINITY_GAUNTLET.get()) && gauntlet != null && !gauntlet.titan() && gauntlet.has(kind)
                && StoneRules.mayWield(player);
    }

    /**
     * Whether the player holds the stone in a hand and may use it now: bare, or selected in a Gauntlet he wields (the
     * Reality Stone's bubbles ask it every tick).
     */
    public static boolean inHand(Player player, StoneKind kind) {
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(ModItems.stone(kind)) && StoneRules.mayUse(player, kind)) {
                return true;
            }
            GauntletContents gauntlet = stack.get(ModDataComponents.GAUNTLET.get());
            if (stack.is(ModItems.INFINITY_GAUNTLET.get()) && gauntlet != null && !gauntlet.titan()
                    && gauntlet.selectedStone() == kind && StoneRules.mayWield(player)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the player carries a real stone (bare or in a Gauntlet), whatever he may do with it. */
    public static boolean carriesAnyStone(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isStone(stack)) {
                return true;
            }
        }
        return isStone(player.getOffhandItem());
    }

    private static boolean isStone(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (StoneKind kind : StoneKind.values()) {
            if (stack.is(ModItems.stone(kind))) {
                return true;
            }
        }
        GauntletContents gauntlet = stack.get(ModDataComponents.GAUNTLET.get());
        return stack.is(ModItems.INFINITY_GAUNTLET.get()) && gauntlet != null && !gauntlet.titan()
                && gauntlet.count() > 0;
    }

    /**
     * The vessel of a stone (the item that holds it and lets anyone use it), or null (Reality, Power, Soul have none).
     */
    @Nullable
    public static Item vessel(StoneKind kind) {
        return switch (kind) {
            case MIND -> ModItems.LOKIS_SCEPTER.get();
            case SPACE -> ModItems.TESSERACT.get();
            case TIME -> ModItems.EYE_OF_AGAMOTTO.get();
            default -> null;
        };
    }

    /** Whether the player is the hero Thanos (or a custom hero with his stones), whose own tick runs a Soul Drain. */
    static boolean isThanos(Player player) {
        return Powers.get(player).acts(Power.THANOS);
    }

    private StoneUse() {
    }
}
