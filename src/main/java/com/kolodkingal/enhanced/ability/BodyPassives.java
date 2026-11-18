package com.kolodkingal.enhanced.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * (3.17) The always-on effects of the built-in heroes' bodies that the Hero Creator also offers as picks of their own, in one
 * place: the hero's code (Vision's synthetic body, Ultron's machine body) and a custom hero's pick call the same method, so
 * nothing is copied. Each is applied every tick, server side, by the hero's own tick.
 */
public final class BodyPassives {

    /** Never hungry (Vision's, Ultron's body). */
    public static void neverHungry(ServerPlayer player) {
        if (player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(20);
        }
    }

    /** Never out of breath: he breathes under water (Vision's, Ultron's body). */
    public static void neverBreathless(ServerPlayer player) {
        player.setAirSupply(player.getMaxAirSupply());
    }

    /** No Poison and no Wither runs in a machine (Ultron's body); the rest is {@code UltronEvents}, which refuses them. */
    public static void noPoison(ServerPlayer player) {
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
    }

    private BodyPassives() {
    }
}
