package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Whose side a Hydra soldier is on (3.6, the plan's 15.9). Hydra never hurt one another (the ally rule every area
 * attack asks, and their own bolts and targets) — unless one of them is a <b>traitor</b>: turned by a hit of Loki's
 * Scepter (MCU: Hydra kept the Scepter), by the Mind Stone's command ({@link #turn}, the seam the stones' lane calls),
 * possessed by the Phoenix or steered by Professor X. The others see the traitor and fight him; he fights them.
 *
 * <p>A turned soldier ({@link HydraSoldierEntity#turnTo}) has blue eyes, follows the one who turned him, attacks
 * Hydra, never raises the alarm, and is an ally of his leader for every area attack; after {@code [hydra]
 * scepterTurnTicks} (1200; an officer half) the hold fades and he is hostile again.
 */
public final class HydraLoyalty {

    /**
     * Loki's Scepter hit {@code target} (a melee hit that landed).
     *
     * @return true when the hit turned a Hydra soldier (then the Scepter's old confusion is skipped); false for
     * anything else
     */
    public static boolean onScepterHit(LivingEntity target, LivingEntity attacker) {
        if (!(target instanceof HydraSoldierEntity soldier) || !(attacker instanceof Player) || !soldier.isAlive()
                || !scepterTurns()) {
            return false;
        }
        int ticks = turnTicks();
        return soldier.turnTo(attacker, soldier instanceof HydraOfficerEntity ? ticks / 2 : ticks);
    }

    /**
     * Turns a Hydra soldier to {@code leader}'s side for {@code ticks} (the Mind Stone's command calls it for a Hydra
     * creature: lane L2).
     *
     * @return false if {@code mob} is not a Hydra soldier, or would not turn
     */
    public static boolean turn(LivingEntity mob, LivingEntity leader, int ticks) {
        return mob instanceof HydraSoldierEntity soldier && soldier.turnTo(leader, ticks);
    }

    /** Whether the Scepter turns soldiers (the server's option; on before the config is loaded). */
    public static boolean scepterTurns() {
        return !EnhancedConfig.SPEC.isLoaded() || HydraConfig.SCEPTER_TURNS.get();
    }

    /** How long a hit of the Scepter turns a soldier (an officer half). */
    public static int turnTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.SCEPTER_TURN_TICKS.get() : 1200;
    }

    /** Whether this is Hydra (a soldier or an officer, or anything in {@code #enhanced:hydra}). */
    public static boolean isHydra(Entity e) {
        return e instanceof HydraSoldierEntity || e.getType().is(ModTags.HYDRA);
    }

    /**
     * Whether a Hydra creature fights against Hydra now: turned (the Scepter, the Mind Stone), possessed by the
     * Phoenix or steered by Professor X.
     */
    public static boolean isTraitor(Entity e) {
        return (e instanceof HydraSoldierEntity soldier && soldier.isTurned()) || PhoenixAbilities.isHost(e)
                || MindLink.isHost(e);
    }

    /** Whether two creatures are Hydra on the same side: both Hydra and neither a traitor. */
    public static boolean sameSide(LivingEntity a, LivingEntity b) {
        return a != b && isHydra(a) && isHydra(b) && !isTraitor(a) && !isTraitor(b);
    }

    /** Whether {@code soldier} is a soldier turned to {@code leader}'s side. */
    public static boolean turnedFor(LivingEntity soldier, LivingEntity leader) {
        return soldier instanceof HydraSoldierEntity s && s.turnedBy(leader);
    }

    /**
     * The ally rule of every area attack ({@code AreaTargets.addAllyRule}): Hydra spare Hydra on their side, and a
     * hero spares the soldiers he turned.
     */
    public static boolean allies(LivingEntity owner, LivingEntity target) {
        return sameSide(owner, target) || turnedFor(target, owner);
    }

    private HydraLoyalty() {
    }
}
