package com.kolodkingal.enhanced.love;

import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Love's abilities, the API the shared seams call (3.8, PLAN 10.6): {@code PowerTicker.applyPassives} asks {@link
 * #canFly}. The 3.8 skeleton writes a neutral {@link #canFly} and the plain {@link #holdsStormbreaker}; lane L8 fills
 * them (she flies while she holds Stormbreaker, {@code [love] flightNeedsAxe}) and keeps the signatures.
 */
public final class LoveAbilities {
    /** Whether she holds Stormbreaker in either hand. */
    public static boolean holdsStormbreaker(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModItems.STORMBREAKER.get())) {
                return true;
            }
        }
        return false;
    }

    /** Whether Love may fly now. Skeleton: never (lane L8: while she holds Stormbreaker, as Thor). */
    public static boolean canFly(ServerPlayer player) {
        return false;
    }

    private LoveAbilities() {
    }
}
