package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Agatha in the air: a thin violet haze drifting down off her hem (the magic that holds her up), a mote off her fingers now
 * and then, and at the take-off a ring of violet round her feet. No trail: a witch is lifted, she does not streak.
 */
final class AgathaFlight {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** The take-off: a ring of violet light on the ground at her feet and a few motes thrown outward. */
    static void takeoff(AbstractClientPlayer player) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible() || !EnhancedClientConfig.flightEffects()) {
            return;
        }
        Vec3 feet = player.position();
        RandomSource r = level.random;
        FlightEffects.ring(level, player.getId(), feet.add(0, 0.06, 0), UP, 12, 0.5F, 2.6F, 0.18F, AgathaAbilities.VIOLET, 0.7F);
        for (int i = 0; i < 12; i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double speed = 0.05 + r.nextDouble() * 0.08;
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? AgathaAbilities.LILAC : AgathaAbilities.VIOLET, 0.8F), feet.x, feet.y + 0.1,
                    feet.z, Math.cos(a) * speed, 0.03 + r.nextDouble() * 0.05, Math.sin(a) * speed);
        }
    }

    /** The motes of this tick: two sinking off the hem between her feet, one off a hand every few ticks (not the local hands). */
    static void particles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density, boolean local) {
        RandomSource r = level.random;
        Vec3 right = FlightEffects.at(fx, FlightEffects.FOOT_R, 0);
        Vec3 left = FlightEffects.at(fx, FlightEffects.FOOT_L, 0);
        Vec3 feet = right != null && left != null ? right.add(left).scale(0.5) : player.position();
        float d = local ? density * 0.5F : density;
        for (int i = 0; i < 2; i++) {
            if (r.nextFloat() > d) {
                continue;
            }
            double a = r.nextDouble() * Mth.TWO_PI;
            double radius = 0.15 + r.nextDouble() * 0.25;
            level.addParticle(new EnergyParticleOptions(i == 0 ? AgathaAbilities.VIOLET : AgathaAbilities.DEEP, 0.7F),
                    feet.x + Math.cos(a) * radius, feet.y + 0.1 + r.nextDouble() * 0.3, feet.z + Math.sin(a) * radius,
                    0.0, -0.015 - r.nextDouble() * 0.01, 0.0);
        }
        if (!local && level.getGameTime() % 4 == 0) {
            for (int source : new int[]{FlightEffects.HAND_R, FlightEffects.HAND_L}) {
                Vec3 p = FlightEffects.at(fx, source, 0);
                if (p != null && r.nextFloat() < d * 0.5F) {
                    level.addParticle(new EnergyParticleOptions(AgathaAbilities.LILAC, 0.5F), p.x, p.y, p.z, 0.0, -0.01, 0.0);
                }
            }
        }
    }

    private AgathaFlight() {
    }
}
