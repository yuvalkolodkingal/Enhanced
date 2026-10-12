package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * En Sabah Nur in the air: a slow swirl of sand motes round his feet, a few drifting off his shoulders, a ring of sand where
 * he leaves the ground. No trail of his own (he does not streak, he is simply lifted).
 */
final class ApocalypseFlight {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** The motes laid each tick, and the turn of the swirl per tick (radians). */
    static final int MOTES = 3;
    static final float SWIRL_SPEED = 0.28F;

    /** The take-off: a ring of sand on the ground at his feet and a burst of grains thrown outward. */
    static void takeoff(AbstractClientPlayer player) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible() || ApocalypseActing.quiet()) {
            return;
        }
        Vec3 feet = player.position();
        RandomSource r = level.random;
        FlightEffects.ring(level, player.getId(), feet.add(0, 0.06, 0), UP, 12, 0.5F, 3.4F, 0.2F, SandPalette.MAIN, 0.8F);
        int n = Math.round(16 * ApocalypseActing.size(player) + 4);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double speed = 0.08 + r.nextDouble() * 0.12;
            level.addParticle(ApocalypseActing.sand(), feet.x, feet.y + 0.1, feet.z, Math.cos(a) * speed, 0.08 + r.nextDouble() * 0.14, Math.sin(a) * speed);
        }
    }

    /** The motes of this tick: {@code MOTES} round the feet in a turning ring, and one off a shoulder now and then. */
    static void particles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density, boolean local) {
        RandomSource r = level.random;
        long now = level.getGameTime();
        float d = local ? density * 0.5F : density;
        Vec3 right = FlightEffects.at(fx, FlightEffects.FOOT_R, 0);
        Vec3 left = FlightEffects.at(fx, FlightEffects.FOOT_L, 0);
        Vec3 feet = right != null && left != null ? right.add(left).scale(0.5) : player.position();
        double age = player.tickCount;
        for (int i = 0; i < MOTES; i++) {
            if (r.nextFloat() > d) {
                continue;
            }
            double angle = age * SWIRL_SPEED + i * (Mth.TWO_PI / MOTES);
            double radius = 0.45 + 0.1 * Math.sin(age * 0.1 + i);
            double x = feet.x + Math.cos(angle) * radius;
            double y = feet.y + 0.05 + r.nextDouble() * 0.3;
            double z = feet.z + Math.sin(angle) * radius;
            // Along the circle, and slowly up.
            double vx = -Math.sin(angle) * 0.03;
            double vz = Math.cos(angle) * 0.03;
            if (i == 0) {
                level.addParticle(ApocalypseActing.sand(), x, y, z, vx, 0.01, vz);
            } else {
                level.addParticle(new EnergyParticleOptions(i == 1 ? SandPalette.MAIN : SandPalette.LIGHT, 0.7F), x, y, z, vx, 0.012, vz);
            }
        }
        // A few drifting off the shoulders.
        if (now % 3 == 0) {
            for (int source : new int[]{FlightEffects.SHOULDER_R, FlightEffects.SHOULDER_L}) {
                Vec3 p = FlightEffects.at(fx, source, 0);
                if (p != null && r.nextFloat() < d) {
                    level.addParticle(new EnergyParticleOptions(r.nextBoolean() ? SandPalette.LIGHT : SandPalette.GOLD, 0.6F),
                            p.x + (r.nextDouble() - 0.5) * 0.2, p.y + (r.nextDouble() - 0.5) * 0.2, p.z + (r.nextDouble() - 0.5) * 0.2,
                            (r.nextDouble() - 0.5) * 0.01, 0.008, (r.nextDouble() - 0.5) * 0.01);
                }
            }
        }
    }

    private ApocalypseFlight() {
    }
}
