package com.kolodkingal.enhanced.healing;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;

/**
 * The regeneration of a healer (3.7, the plan's 2.2): {@code regenPerSecond} (1.0) always, also in a fight, and
 * {@code regenOutOfCombat} (3.0) once he has taken no damage for {@code outOfCombatTicks} (60): "watch the wound close".
 * It is paid for with food ({@code exhaustionPerHealth} per health healed); at food 0 it is a quarter (never nothing).
 * Wolverine's Berserker Rage multiplies it ({@code [wolverine] berserkRegenFactor}). Nothing while poison has switched
 * the healing off, and nothing while he is DOWN (he rises with his own health).
 *
 * <p>It heals in steps every {@link #STEP} ticks of the game's time (so two healers heal on the same ticks, and a test
 * can count them). Both sides: the HUD reads {@link #rate} for "+1.0/s".
 */
public final class Regeneration {
    /** Ticks between two steps of healing. */
    public static final int STEP = 10;
    /** The share of the regeneration left when he is starving (food 0). */
    public static final float STARVING = 0.25F;

    private Regeneration() {
    }

    public static double perSecond() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.REGEN_PER_SECOND.get() : 1.0;
    }

    public static double outOfCombat() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.REGEN_OUT_OF_COMBAT.get() : 3.0;
    }

    public static int outOfCombatTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.OUT_OF_COMBAT_TICKS.get() : 60;
    }

    public static double exhaustionPerHealth() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.EXHAUSTION_PER_HEALTH.get() : 0.05;
    }

    static double berserkFactor() {
        return EnhancedConfig.SPEC.isLoaded() && WolverineConfig.BERSERK_REGEN_FACTOR != null
                ? WolverineConfig.BERSERK_REGEN_FACTOR.get() : 2.0;
    }

    /** Whether he has been out of harm long enough for the faster healing at game time {@code now}. */
    public static boolean outOfHarm(Player player, long now) {
        HealingState state = ModAttachments.HEALING.get(player);
        return now - state.lastHarm() >= outOfCombatTicks();
    }

    /**
     * Health a second he heals at game time {@code now}: 0 without a working healing factor (none, poisoned) or while
     * DOWN; else the always or the out-of-harm rate, times the Rage's factor, a quarter when starving.
     */
    public static double rate(Player player, long now) {
        if (!HealingFactor.active(player) || HealingFactor.isDown(player)) {
            return 0.0;
        }
        double rate = outOfHarm(player, now) ? outOfCombat() : perSecond();
        if (Powers.get(player).is(Live.WOLVERINE_BERSERK)) {
            rate *= berserkFactor();
        }
        if (player.getFoodData().getFoodLevel() <= 0) {
            rate *= STARVING;
        }
        return rate;
    }

    /** One tick: on every {@link #STEP}th tick of the game he heals a step's share of his rate, and pays for it. */
    public static void tick(Player player) {
        long now = player.level().getGameTime();
        if (now % STEP != 0 || !player.isAlive() || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        double rate = rate(player, now);
        if (rate <= 0.0) {
            return;
        }
        float before = player.getHealth();
        player.heal((float) (rate * STEP / 20.0));
        float healed = player.getHealth() - before;
        if (healed > 0.0F) {
            player.causeFoodExhaustion((float) (healed * exhaustionPerHealth()));
        }
    }
}
