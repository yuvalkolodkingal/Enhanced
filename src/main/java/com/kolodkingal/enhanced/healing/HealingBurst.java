package com.kolodkingal.enhanced.healing;

import net.minecraft.server.level.ServerPlayer;

/**
 * The Healing Burst (3.7, Sneak + B for both healers: the rows {@code wolverine.healing_burst} and {@code
 * deadpool.chimichanga}): a share of his max health at once, harmful effects (not Poison) cleared, a soft shimmer;
 * refused while poisoned. Lanes L2 and L3 call it from their keys; lane L1 writes its body (a stub of the 3.7 skeleton
 * until then: it heals nothing and says so).
 */
public final class HealingBurst {

    /**
     * Heals {@code player} by {@code fraction} of his max health now (Deadpool's chimichanga passes more). Returns
     * whether it healed: false when refused (poisoned), and then no cooldown should be spent. A player without the
     * healing factor (a custom hero with the row alone) is healed plainly.
     */
    public static boolean burst(ServerPlayer player, double fraction) {
        return false;
    }

    private HealingBurst() {
    }
}
