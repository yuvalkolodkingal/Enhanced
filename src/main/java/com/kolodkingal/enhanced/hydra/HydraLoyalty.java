package com.kolodkingal.enhanced.hydra;

import net.minecraft.world.entity.LivingEntity;

/**
 * Hydra soldiers turned to a hero's side (3.6, the plan's 15.9): a hit of Loki's Scepter turns a soldier for {@code
 * [hydra] scepterTurnTicks}. The 3.6 skeleton writes the seam the Scepter calls; lane L8 owns the class and writes its
 * body.
 */
public final class HydraLoyalty {

    /**
     * Loki's Scepter hit {@code target} (a melee hit that landed).
     *
     * @return true when the hit turned a Hydra soldier (then the Scepter's old confusion is skipped); false for
     * anything else
     */
    public static boolean onScepterHit(LivingEntity target, LivingEntity attacker) {
        return false;
    }

    private HydraLoyalty() {
    }
}
