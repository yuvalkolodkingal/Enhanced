package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * The tells that are read from the boss himself (DESIGN section 17, 3.6): his state, the game time it began and the point
 * his attack is aimed at are synced, so what he is about to do is drawn from them, and a player who comes into view late sees
 * the same. The five lines that run from his raised hand to the target of the Lances in the last six ticks (the aim locks at
 * tick 8), the lane that rises along the ground where the Wave will roll (faint while it still tracks, bright and pulsing once
 * it has locked at tick 12), the amber ring that follows the first player the Spikes will fall on until tick 8, the stripe of
 * sand that crawls across the ground to the feet of the one the grasp has chosen and the ring that closes on the spot, and the
 * dust and the sparks his body gives off in each state. What carries a position the entity does not (the second and later
 * rings of the Spikes, the lines of the barrage, the Mind Crush's ring, the portals) is {@link SandFx}'s payload effects.
 */
final class BossTells {
    /** The ticks at which the aim of each attack locks (the server's, DESIGN 3.6). */
    static final int LANCE_LOCK = 8;
    static final int WAVE_LOCK = 12;
    static final int SPIKES_LOCK = 8;
    static final int ENTOMB_LOCK = 20;
    /** How far the lane of the Wave runs, and how wide it is. */
    private static final double WAVE_LENGTH = 30.0;
    private static final double WAVE_WIDTH = 4.0;
    private static final double LANE_STEP = 1.5;

    /** Where the raised right hand of the boss is: what the lines of the Lances start from. */
    static Vec3 palm(ApocalypseEntity boss) {
        float yaw = Mth.DEG_TO_RAD * boss.yBodyRot;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        float scale = boss.isTrueForm() ? ApocalypseCubes.SCALE_TRUE : ApocalypseCubes.SCALE_ARMOURED;
        return boss.position().add(0.0, 1.43 * scale, 0.0).add(forward.scale(0.7 * scale)).add(right.scale(0.42 * scale));
    }

    // ---- the particles of his body ----

    static void tick(ClientLevel level, long now, float density, RandomSource random) {
        for (ApocalypseEntity boss : ApocalypseBosses.all()) {
            if (boss.isInvisible()) {
                continue;
            }
            float time = boss.stateTime(0.0F);
            ApocalypseState state = boss.state();
            switch (state) {
                case BACKHAND -> {
                    // A flash on the knuckle as the arm is drawn across the chest.
                    if (time >= 3.0F) {
                        Vec3 fist = boss.position().add(0.0, 1.6, 0.0);
                        level.addParticle(new EnergyParticleOptions(SandPalette.GOLD_CORE, 1.4F), fist.x, fist.y, fist.z, 0.0, 0.0, 0.0);
                    }
                }
                case TELL_BOULDERS -> {
                    // Sand tears out of the ground at his feet as the blocks rise.
                    if (time < SandMath.BOULDER_RISE + 4) {
                        for (int i = 0; i < Math.round(5 * density); i++) {
                            double angle = random.nextDouble() * Mth.TWO_PI;
                            double r = 1.0 + random.nextDouble() * 1.8;
                            level.addParticle(SandParticles.GRAIN, boss.getX() + Math.cos(angle) * r, boss.getY() + 0.1,
                                    boss.getZ() + Math.sin(angle) * r, 0.0, 0.15 + random.nextDouble() * 0.1, 0.0);
                        }
                    }
                }
                case TELL_WAVE, TELL_SPIKES, TELL_TORNADO -> {
                    // The ground rumbles: a little dust at his feet.
                    if (random.nextFloat() < 0.6F * density) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(SandParticles.haze(1.4F), boss.getX() + Math.cos(angle) * 1.2, boss.getY() + 0.1, boss.getZ() + Math.sin(angle) * 1.2, 0.0, 0.02, 0.0);
                    }
                }
                case HOVER, ARMOUR_FALL -> {
                    for (int i = 0; i < Math.round(3 * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(SandParticles.GRAIN, boss.getX() + Math.cos(angle) * 2.2, boss.getY() + 0.1,
                                boss.getZ() + Math.sin(angle) * 2.2, 0.0, 0.05, 0.0);
                    }
                }
                case KNEELING, STAGGERED -> {
                    // The cracks of the cap: sparks of gold rise from his head.
                    Vec3 head = boss.position().add(0.0, boss.getBbHeight() * 0.95, 0.0);
                    if (state == ApocalypseState.KNEELING || random.nextFloat() < 0.4F) {
                        for (int i = 0; i < Math.round((state == ApocalypseState.KNEELING ? 3 : 1) * density); i++) {
                            level.addParticle(new EnergyParticleOptions(SandPalette.GOLD, 1.0F), head.x + (random.nextDouble() - 0.5) * 0.6, head.y,
                                    head.z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.05 + random.nextDouble() * 0.05, 0.0);
                        }
                    }
                }
                default -> {
                }
            }
        }
    }

    // ---- what lies on the ground ----

    static void renderFlat(ClientLevel level, long now, float partial, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time, float calm) {
        for (ApocalypseEntity boss : ApocalypseBosses.all()) {
            if (boss.isInvisible()) {
                continue;
            }
            float age = boss.stateTime(partial);
            switch (boss.state()) {
                case TELL_WAVE -> lane(level, boss, age, pose, vc, camera, time, calm);
                case TELL_SPIKES -> {
                    if (age < SPIKES_LOCK) {
                        // The ring follows the first of his targets until the aim locks.
                        Vec3 aim = boss.aim();
                        double y = SandControl.groundAt(level, aim.x, aim.y + 1.0, aim.z) + 0.04;
                        Vec3 c = new Vec3(aim.x, y, aim.z).subtract(camera);
                        float u = age / SPIKES_LOCK;
                        SandShapes.flatDisc(pose, vc, c, 1.8, 32, argb(SandPalette.GOLD_EDGE, 0.08F * calm), argb(SandPalette.GOLD_EDGE, 0.12F * calm));
                        SandShapes.flatRing(pose, vc, c.add(0.0, 0.01, 0.0), 1.8, 0.14, 40, argb(SandPalette.GOLD, (0.25F + 0.2F * u) * calm));
                    }
                }
                case TELL_ENTOMB -> entomb(level, boss, age, pose, vc, camera, calm);
                default -> {
                }
            }
        }
    }

    /** The lane of the Wave: a band of the ground along the way it will roll, with chevrons running down it. */
    private static void lane(ClientLevel level, ApocalypseEntity boss, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time, float calm) {
        Vec3 flat = new Vec3(boss.aim().x - boss.getX(), 0.0, boss.aim().z - boss.getZ());
        if (flat.lengthSqr() < 1.0E-4) {
            flat = Vec3.directionFromRotation(0.0F, boss.yBodyRot);
        }
        Vec3 dir = flat.normalize();
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(WAVE_WIDTH * 0.5);
        boolean locked = age >= WAVE_LOCK;
        float u = AnimMath.ss(0.0F, ApocalypseState.TELL_WAVE.tellTicks(), age);
        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.9F);
        float base = (locked ? 0.26F + 0.12F * pulse : 0.07F + 0.10F * u) * calm;
        Vec3 origin = boss.position();
        int segments = (int) (WAVE_LENGTH / LANE_STEP);
        // The ground under the lane, read as it runs (the band follows it).
        double[] ground = new double[segments + 1];
        double hint = origin.y;
        for (int i = 0; i <= segments; i++) {
            Vec3 centre = origin.add(dir.scale(i * LANE_STEP));
            ground[i] = SandControl.groundAt(level, centre.x, hint + 1.0, centre.z) + 0.05;
            hint = ground[i];
        }
        for (int i = 1; i <= segments; i++) {
            Vec3 c0 = origin.add(dir.scale((i - 1) * LANE_STEP));
            Vec3 c1 = origin.add(dir.scale(i * LANE_STEP));
            float f0 = base * (1.0F - 0.7F * (i - 1) / segments);
            float f1 = base * (1.0F - 0.7F * (float) i / segments);
            quad(pose, vc, rel(c0.subtract(side), ground[i - 1], camera), rel(c0.add(side), ground[i - 1], camera), rel(c1.add(side), ground[i], camera),
                    rel(c1.subtract(side), ground[i], camera), argb(SandPalette.GOLD_EDGE, f0), argb(SandPalette.GOLD_EDGE, f0), argb(SandPalette.GOLD_EDGE, f1),
                    argb(SandPalette.GOLD_EDGE, f1));
        }
        // The edges of the lane, and chevrons running down it once it has locked (the way the wave will go).
        for (int sign = -1; sign <= 1; sign += 2) {
            for (int i = 1; i <= segments; i++) {
                Vec3 c0 = origin.add(dir.scale((i - 1) * LANE_STEP)).add(side.scale(sign * 0.97));
                Vec3 c1 = origin.add(dir.scale(i * LANE_STEP)).add(side.scale(sign * 0.97));
                float f = (0.35F + 0.4F * pulse) * (locked ? 1.0F : 0.35F) * (1.0F - 0.6F * (float) i / segments) * calm;
                quad(pose, vc, rel(c0, ground[i - 1] + 0.01, camera).add(side.normalize().scale(-0.07 * sign)), rel(c0, ground[i - 1] + 0.01, camera).add(side.normalize().scale(0.07 * sign)),
                        rel(c1, ground[i] + 0.01, camera).add(side.normalize().scale(0.07 * sign)), rel(c1, ground[i] + 0.01, camera).add(side.normalize().scale(-0.07 * sign)),
                        argb(SandPalette.GOLD, f), argb(SandPalette.GOLD, f), argb(SandPalette.GOLD, f), argb(SandPalette.GOLD, f));
            }
        }
        if (locked) {
            for (int k = 0; k < 4; k++) {
                double distance = (time * 1.4 + k * 7.5) % WAVE_LENGTH;
                int index = Mth.clamp((int) (distance / LANE_STEP), 0, segments);
                Vec3 at = origin.add(dir.scale(distance));
                float f = (1.0F - (float) (distance / WAVE_LENGTH)) * 0.7F * calm;
                SandShapes.lane(pose, vc, rel(at, ground[index] + 0.02, camera), dir, 1.3, 2.4, argb(SandPalette.GOLD, 0.0F), argb(SandPalette.GOLD_CORE, f));
            }
        }
    }

    /** A point of the ground in the camera's frame. */
    private static Vec3 rel(Vec3 at, double y, Vec3 camera) {
        return new Vec3(at.x - camera.x, y - camera.y, at.z - camera.z);
    }

    /** The grasp: a stripe of sand crawls across the ground to the feet of its victim, and a ring closes on the spot. */
    private static void entomb(ClientLevel level, ApocalypseEntity boss, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera, float calm) {
        Vec3 aim = boss.aim();
        Vec3 flat = new Vec3(aim.x - boss.getX(), 0.0, aim.z - boss.getZ());
        double length = flat.length();
        if (length < 0.5) {
            return;
        }
        Vec3 dir = flat.scale(1.0 / length);
        float crawl = SandMath.crawl(age);
        double reach = length * crawl;
        boolean locked = age >= ENTOMB_LOCK;
        Vec3 from = new Vec3(boss.getX(), boss.getY() + 0.06, boss.getZ()).subtract(camera);
        SandShapes.lane(pose, vc, from, dir, reach, 0.7, argb(SandPalette.SHADE, 0.15F * calm), argb(locked ? SandPalette.LIGHT : SandPalette.MAIN, 0.75F * calm));
        double y = SandControl.groundAt(level, aim.x, aim.y + 1.0, aim.z) + 0.05;
        Vec3 c = new Vec3(aim.x, y, aim.z).subtract(camera);
        // The spot: a ring of 1.5 blocks, which closes as the hand does.
        float close = AnimMath.ss(ENTOMB_LOCK, ApocalypseState.TELL_ENTOMB.tellTicks(), age);
        double radius = Mth.lerp(close, 1.5, 0.6);
        float alpha = (locked ? 0.7F : 0.3F * crawl) * calm;
        SandShapes.flatDisc(pose, vc, c, radius, 28, argb(SandPalette.GOLD_EDGE, 0.1F * alpha), argb(SandPalette.GOLD_EDGE, 0.25F * alpha));
        SandShapes.flatRing(pose, vc, c.add(0.0, 0.01, 0.0), radius, 0.16, 36, argb(locked ? SandPalette.GOLD_CORE : SandPalette.GOLD, alpha));
    }

    // ---- what glows ----

    static void renderGlow(ClientLevel level, long now, float partial, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time, float calm) {
        for (ApocalypseEntity boss : ApocalypseBosses.all()) {
            if (boss.isInvisible() || boss.state() != ApocalypseState.TELL_LANCE) {
                continue;
            }
            float age = boss.stateTime(partial);
            if (age < LANCE_LOCK - 2) {
                continue;
            }
            // Five thin lines from his palm to where the Lances will fly, fanned seven degrees either side: the aim is locked.
            Vec3 palm = palm(boss);
            Vec3 toward = boss.aim().subtract(palm);
            double length = Math.max(2.0, toward.length());
            Vec3 direction = toward.normalize();
            float u = AnimMath.ss(LANCE_LOCK - 2, ApocalypseState.TELL_LANCE.tellTicks(), age);
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(-7.0 + 3.5 * i);
                Vec3 d = new Vec3(direction.x * Math.cos(angle) - direction.z * Math.sin(angle), direction.y, direction.x * Math.sin(angle) + direction.z * Math.cos(angle));
                Vec3 a = palm.subtract(camera);
                Vec3 b = palm.add(d.scale(length)).subtract(camera);
                // (Thin, and the glow fainter at first: they are seen from the front, where a line is a hand's breadth wide.)
                EnergyGeometry.ribbon(pose, vc, a, b, 0.0F, 0.05F, argb(SandPalette.GOLD_EDGE, 0.0F), argb(SandPalette.GOLD_EDGE, (0.12F + 0.22F * u) * calm));
                EnergyGeometry.ribbon(pose, vc, a, b, 0.0F, 0.014F, argb(SandPalette.GOLD_CORE, 0.0F), argb(SandPalette.GOLD_CORE, (0.25F + 0.5F * u) * calm));
            }
        }
    }

    private static void quad(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(ca);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(cb);
        vc.addVertex(pose, (float) c.x, (float) c.y, (float) c.z).setColor(cc);
        vc.addVertex(pose, (float) d.x, (float) d.y, (float) d.z).setColor(cd);
    }

    private BossTells() {
    }
}
