package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.StoneUse;
import com.kolodkingal.enhanced.stones.TimeRewind;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Time Stone's powers through the Eye of Agamotto (3.6): Slow Time (a switch) and Rewind, on Doctor Strange's Y
 * ({@code strange.eye} and its variant {@code strange.eye_rewind}) and on the Eye itself in a sorcerer's hand while the
 * stones' lane has not taken the Eye's use ({@code StoneUse.use} answers false).
 *
 * <p>Slow Time sets {@link Toggle#TIME_SLOW} and the synced {@link Live#TIME_SLOWING}: {@code SlowMotion} reads the
 * second as a Time Stone's zone ({@code StoneZones}, radius {@code [time_stone] slowRadius}) centred on him, and he is
 * not slowed; it lasts at most {@code slowMaxTicks}. Rewind is Thanos's ({@link TimeRewind}), the history kept while he
 * carries the Eye. Both need the Eye in his inventory ("The Eye of Agamotto is not with you."); a sorcerer silenced by
 * the Hex Runes cannot open it. Their recharge is the Eye's own item cooldown (so the hotbar shows it, and a Time Stone's
 * use through the same Eye shares it), stretched by a custom hero's strength.
 *
 * <p>This class owns only the slowing it started ({@link #SLOWING}): the bits a bare Time Stone set are the stones'.
 */
public final class EyeTime {
    /** Slow Time's hunger each tick it stays on. */
    static final float SLOW_EXHAUSTION = 0.02F;
    /** The Rewind's hunger, and the most health it gives back. */
    static final float REWIND_EXHAUSTION = 4.0F;
    static final float REWIND_HEAL = 20.0F;

    /** Who has the Eye's Slow Time on, and since when. */
    private static final Map<UUID, Long> SLOWING = new HashMap<>();

    // ---- the numbers ([time_stone]: the Eye holds the Time Stone) ----

    public static int slowMaxTicks() {
        return StrangeAbilities.cfgInt(() -> StonesConfig.TIME_SLOW_MAX_TICKS.get(), 200);
    }

    public static int slowCooldown() {
        return StrangeAbilities.cfgInt(() -> StonesConfig.TIME_SLOW_COOLDOWN.get(), 200);
    }

    public static int rewindSeconds() {
        return StrangeAbilities.cfgInt(() -> StonesConfig.TIME_REWIND_SECONDS.get(), 5);
    }

    public static int rewindCooldown() {
        return StrangeAbilities.cfgInt(() -> StonesConfig.TIME_REWIND_COOLDOWN.get(), 600);
    }

    // ---- queries ----

    /** The Eye of Agamotto. */
    public static Item eye() {
        return ModItems.EYE_OF_AGAMOTTO.get();
    }

    /** Whether the player carries an Eye of Agamotto (anywhere in his inventory). */
    public static boolean hasEye(Player player) {
        if (player.getOffhandItem().is(eye())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(eye())) {
                return true;
            }
        }
        return false;
    }

    /** Whether the Eye's Slow Time (this class's) is on for him. */
    public static boolean slowing(UUID player) {
        return SLOWING.containsKey(player);
    }

    /** The ticks his Slow Time has been on, or -1. */
    public static long slowAge(UUID player, long now) {
        Long since = SLOWING.get(player);
        return since == null ? -1 : now - since;
    }

    /** Whether the Eye recharges for him (its item cooldown). */
    public static boolean recharging(Player player) {
        return player.getCooldowns().isOnCooldown(eye());
    }

    /** Why the Eye will not open for him now (a lang key), or null. */
    @Nullable
    static String refusal(Player player) {
        if (!hasEye(player)) {
            return "message.enhanced.eye.missing";
        }
        if (HexRunes.silences(player)) {
            return "message.enhanced.eye.runes";
        }
        return null;
    }

    /** The Eye's recharge, at a custom hero's strength for his row. */
    static int scaled(PowerData data, @Nullable HeroAbility row, int ticks) {
        if (row != null && data.power() == Power.CUSTOM && data.kit() != null) {
            return Math.max(1, Math.round(ticks * data.kit().cooldownFactor(row)));
        }
        return ticks;
    }

    /**
     * Doctor Strange's Y (or his use of the Eye; {@code row} his Eye row): the Eye's time as its vessel through the
     * stones' single entry ({@code StoneUse.use(TIME, VESSEL)}); while that entry does not take it (or for a slowing
     * this class started), the Eye's own Slow Time ({@code alt}: Rewind). Without the Eye, or under the Hex Runes, a
     * refusal.
     */
    public static void strangeUse(ServerPlayer player, PowerData data, @Nullable HeroAbility row, boolean alt) {
        String refusal = refusal(player);
        if (refusal != null) {
            StrangeAbilities.say(player, refusal);
            return;
        }
        if (!SLOWING.containsKey(player.getUUID())
                && StoneUse.use(player, StoneKind.TIME, StoneUse.Source.VESSEL, alt, true)) {
            return;
        }
        if (alt) {
            rewind(player, data, row, true);
        } else {
            toggleSlow(player, data, row, true);
        }
    }

    // ---- Slow Time ----

    /**
     * Y (or the Eye's use): Slow Time on, or off if it is on. {@code row} is the ability it is used as (null: the Eye in
     * a sorcerer's hand); {@code tell}: say why it is refused.
     */
    public static void toggleSlow(ServerPlayer player, PowerData data, @Nullable HeroAbility row, boolean tell) {
        if (SLOWING.containsKey(player.getUUID())) {
            stop(player, data, true);
            return;
        }
        String refusal = refusal(player);
        if (refusal != null) {
            if (tell) {
                StrangeAbilities.say(player, refusal);
            }
            return;
        }
        if (recharging(player) || StrangeAbilities.refuseHungry(player)) {
            return;
        }
        long now = player.level().getGameTime();
        SLOWING.put(player.getUUID(), now);
        data.setToggle(Toggle.TIME_SLOW, true);
        data.set(Live.TIME_SLOWING, true);
        StrangeAbilities.sound(player, ModSounds.EYE_OPEN.get(), 1.0F, 1.0F);
        StrangeAbilities.sound(player, ModSounds.TIME_SLOW_ON.get(), 1.0F, 1.0F);
    }

    /** Slow Time ends (the switch, its time, the Eye gone, the power lost); with {@code recharge} the Eye recharges. */
    static void stop(ServerPlayer player, PowerData data, boolean recharge) {
        if (SLOWING.remove(player.getUUID()) == null) {
            return;
        }
        data.setToggle(Toggle.TIME_SLOW, false);
        data.set(Live.TIME_SLOWING, false);
        if (recharge) {
            player.getCooldowns().addCooldown(eye(), scaled(data, data.has(HeroAbility.STRANGE_EYE)
                    ? HeroAbility.STRANGE_EYE : null, slowCooldown()));
        }
        StrangeAbilities.sound(player, ModSounds.TIME_SLOW_OFF.get(), 1.0F, 1.0F);
        StrangeAbilities.sound(player, ModSounds.EYE_CLOSE.get(), 0.8F, 1.0F);
    }

    // ---- Rewind ----

    /**
     * Sneak + Y (or Sneak + the Eye's use): back to where he was a few seconds ago (Thanos's Rewind). Refused (and told)
     * without the Eye, silenced by the runes, with too little history or pinned by Mjolnir; then nothing recharges.
     */
    public static void rewind(ServerPlayer player, PowerData data, @Nullable HeroAbility row, boolean tell) {
        String refusal = refusal(player);
        if (refusal != null) {
            if (tell) {
                StrangeAbilities.say(player, refusal);
            }
            return;
        }
        if (recharging(player)) {
            return;
        }
        boolean works = !MjolnirPin.isPinned(player) && TimeRewind.historyTicks(player) >= TimeRewind.MIN_HISTORY;
        int cooldown = scaled(data, row, rewindCooldown());
        TimeRewind.rewind(player, data, new TimeRewind.Cost(row == null ? HeroAbility.STRANGE_EYE : row, cooldown,
                player.getAbilities().invulnerable ? 0.0F : REWIND_EXHAUSTION, REWIND_HEAL, StrangeAbilities.TIME_GREEN));
        if (works) {
            player.getCooldowns().addCooldown(eye(), cooldown);
            StrangeAbilities.sound(player, ModSounds.EYE_OPEN.get(), 0.8F, 0.8F);
            StrangeSignals.near(player.serverLevel(), player.position(),
                    com.kolodkingal.enhanced.network.StrangeFxPayload.SHIELD, player,
                    java.util.List.of(player.position().add(0, 1.0, 0)), StrangeSignals.EYE_REWIND);
        }
    }

    // ---- the tick ----

    /**
     * Every tick of a player with the Eye on his Y ({@code strange.eye}), or whose Eye's time is on: the history the
     * Rewind goes back into, and Slow Time's limits.
     */
    static void tick(ServerPlayer player, PowerData data, long now) {
        boolean row = data.has(HeroAbility.STRANGE_EYE);
        if (row && hasEye(player)) {
            TimeRewind.remember(player, now, rewindSeconds() * 20L);
        }
        Long since = SLOWING.get(player.getUUID());
        if (since == null) {
            return;
        }
        if (!hasEye(player) || HexRunes.silences(player) || !player.isAlive()) {
            stop(player, data, true);
            return;
        }
        if (now - since >= slowMaxTicks() || !StrangeAbilities.fed(player)) {
            stop(player, data, true);
            return;
        }
        data.setToggle(Toggle.TIME_SLOW, true);
        data.set(Live.TIME_SLOWING, true);
        StrangeAbilities.spend(player, data, SLOW_EXHAUSTION);
    }

    /**
     * Every tick of a player who is not Doctor Strange nor has his Eye's row but whose Eye's time is on (a sorcerer who
     * used the Eye): its limits ({@link StrangeServer#everyPlayerTick} calls this).
     */
    static void tickHolder(ServerPlayer player, PowerData data, long now) {
        if (SLOWING.containsKey(player.getUUID()) && !data.acts(Power.DOCTOR_STRANGE)) {
            if (!com.kolodkingal.enhanced.custom.Traits.has(data, com.kolodkingal.enhanced.custom.Trait.SORCERER)) {
                stop(player, data, false); // (no sorcerer any more: the Eye closes)
                return;
            }
            tick(player, data, now);
        }
    }

    /** He loses the power, dies, leaves: the Eye's time ends (without its recharge: nothing was used up). */
    static void endAll(ServerPlayer player, PowerData data) {
        stop(player, data, false);
    }

    static void clear() {
        SLOWING.clear();
    }

    private EyeTime() {
    }
}
