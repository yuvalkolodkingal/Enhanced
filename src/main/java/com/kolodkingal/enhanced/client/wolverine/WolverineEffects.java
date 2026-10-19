package com.kolodkingal.enhanced.client.wolverine;

import com.kolodkingal.enhanced.client.render.WolverineActing;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.wolverine.WolverineAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What everyone near sees of Wolverine's moves (3.7), drawn from {@link WolverineFxPayload}: a glint at the fists when
 * the claws come out (and, on his own screen, the silver flash of the injection), the three silver streaks of a slash
 * and its sweep, the ring of the spin, the burst of the pounce's stab, the red of the Rage and the puff when it ends,
 * the wisps of the Sniff drawn to his nose, the marks of his claws on a wall. Each kind also starts its gesture ({@link
 * WolverineActing#onPayload}).
 */
public final class WolverineEffects {
    private static final int SILVER = WolverineAbilities.SILVER;
    private static final int BRIGHT = 0xF6F9FC;

    /** Registers a drawer for every kind (the client setup calls it once). */
    public static void register() {
        for (int kind = WolverineFxPayload.SNIKT; kind <= WolverineFxPayload.CLIMB; kind++) {
            WolverineFx.register(kind, WolverineEffects::draw);
        }
    }

    static void draw(WolverineFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        WolverineActing.onPayload(p);
        Entity who = p.entityId() >= 0 ? level.getEntity(p.entityId()) : null;
        if (who != null && who == mc.player) {
            WolverineHud.onOwnPayload(p, level.getGameTime());
        }
        Vec3 at = p.points().isEmpty() ? (who != null ? who.position() : null) : p.points().get(0);
        if (at == null) {
            return;
        }
        RandomSource r = level.random;
        switch (p.kind()) {
            case WolverineFxPayload.SNIKT -> {
                if (p.arg() == 1 && who == mc.player) {
                    WolverineHud.flash();
                }
                fists(level, who, 6, r);
            }
            case WolverineFxPayload.SHINK -> fists(level, who, 2, r);
            case WolverineFxPayload.SLASH -> slash(level, who, at, p.arg(), r);
            case WolverineFxPayload.SPIN -> spin(level, at, Math.max(2, p.arg()), r);
            case WolverineFxPayload.POUNCE_HIT -> {
                if (p.arg() > 0) {
                    for (int i = 0; i < 14; i++) {
                        level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, r.nextGaussian() * 0.4,
                                r.nextDouble() * 0.4, r.nextGaussian() * 0.4);
                    }
                    burst(level, at, SILVER, 10, 0.25, r);
                } else {
                    ground(level, at, 10, r);
                }
            }
            case WolverineFxPayload.BERSERK_ON -> {
                if (who != null) {
                    Vec3 head = who.position().add(0, who.getBbHeight() + 0.2, 0);
                    for (int i = 0; i < 5; i++) {
                        level.addParticle(ParticleTypes.ANGRY_VILLAGER, head.x + r.nextGaussian() * 0.4, head.y
                                + r.nextDouble() * 0.3, head.z + r.nextGaussian() * 0.4, 0, 0, 0);
                    }
                    burst(level, who.position().add(0, 1.0, 0), WolverineAbilities.RAGE_RED, 24, 0.35, r);
                }
            }
            case WolverineFxPayload.BERSERK_OFF -> {
                if (who != null) {
                    for (int i = 0; i < 8; i++) {
                        level.addParticle(ParticleTypes.POOF, who.getX() + r.nextGaussian() * 0.3, who.getY()
                                + 1.2 + r.nextDouble() * 0.5, who.getZ() + r.nextGaussian() * 0.3, 0, 0.02, 0);
                    }
                }
            }
            case WolverineFxPayload.SNIFF -> {
                // Pale wisps drawn in to his nose.
                for (int i = 0; i < 16; i++) {
                    double a = r.nextDouble() * Mth.TWO_PI;
                    Vec3 from = at.add(Math.cos(a) * 1.6, r.nextDouble() * 0.8 - 0.3, Math.sin(a) * 1.6);
                    Vec3 v = at.subtract(from).scale(0.09);
                    level.addParticle(new EnergyParticleOptions(0xE8E2D0, 0.35F), from.x, from.y, from.z, v.x, v.y,
                            v.z);
                }
            }
            case WolverineFxPayload.CLIMB -> {
                BlockPos pos = BlockPos.containing(at);
                BlockState wall = level.getBlockState(pos);
                Vec3 him = p.points().size() > 1 ? p.points().get(1) : at;
                Vec3 face = at.add(him.subtract(at).multiply(1, 0, 1).normalize().scale(0.52));
                if (!wall.isAir()) {
                    for (int i = 0; i < 6; i++) {
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, wall), face.x, him.y + 1.4
                                + r.nextDouble() * 0.4, face.z, r.nextGaussian() * 0.05, -0.05,
                                r.nextGaussian() * 0.05);
                    }
                }
                level.addParticle(ParticleTypes.CRIT, face.x, him.y + 1.6, face.z, 0, 0, 0);
            }
            default -> {
            }
        }
    }

    /** A glint at his fists. */
    private static void fists(ClientLevel level, Entity who, int count, RandomSource r) {
        if (who == null) {
            return;
        }
        double yaw = Math.toRadians(who instanceof net.minecraft.world.entity.LivingEntity l ? l.yBodyRot
                : who.getYRot());
        Vec3 side = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        for (int s = -1; s <= 1; s += 2) {
            Vec3 fist = who.position().add(0, who.getBbHeight() * 0.48, 0).add(side.scale(0.38 * s))
                    .add(ahead.scale(0.35));
            for (int i = 0; i < count; i++) {
                level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? BRIGHT : SILVER, 0.3F), fist.x, fist.y,
                        fist.z, r.nextGaussian() * 0.03, r.nextDouble() * 0.04, r.nextGaussian() * 0.03);
            }
        }
    }

    /** Three silver streaks across where the claws passed, and the sweep's crescent. */
    private static void slash(ClientLevel level, Entity who, Vec3 at, int hit, RandomSource r) {
        Vec3 look = who != null ? who.getLookAngle() : new Vec3(0, 0, 1);
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        // The first hit falls right to left, the second left to right, the third both ways (a cross).
        double tilt = hit == 0 ? 0.8 : hit == 1 ? -0.8 : 0.0;
        for (int line = -1; line <= 1; line++) {
            Vec3 offset = new Vec3(0, line * 0.12, 0);
            for (int i = 0; i <= 7; i++) {
                double u = i / 7.0 - 0.5;
                Vec3 a = at.add(offset).add(side.scale(u * 0.9)).add(0, -u * tilt * 0.9, 0);
                level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? BRIGHT : SILVER, 0.22F), a.x, a.y, a.z,
                        0, 0, 0);
                if (hit == 2) {
                    Vec3 b = at.add(offset).add(side.scale(u * 0.9)).add(0, u * 0.8, 0);
                    level.addParticle(new EnergyParticleOptions(SILVER, 0.22F), b.x, b.y, b.z, 0, 0, 0);
                }
            }
        }
        level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0, 0, 0);
    }

    /** The spin's ring of sweeps and silver round him. */
    private static void spin(ClientLevel level, Vec3 at, int radius, RandomSource r) {
        for (int i = 0; i < 8; i++) {
            double a = i * Mth.TWO_PI / 8.0;
            level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x + Math.cos(a) * radius * 0.6, at.y + 1.0,
                    at.z + Math.sin(a) * radius * 0.6, 0, 0, 0);
        }
        for (int i = 0; i < 32; i++) {
            double a = i * Mth.TWO_PI / 32.0;
            level.addParticle(new EnergyParticleOptions(SILVER, 0.25F), at.x + Math.cos(a) * radius, at.y + 1.0,
                    at.z + Math.sin(a) * radius, -Math.sin(a) * 0.05, 0, Math.cos(a) * 0.05);
        }
    }

    private static void burst(ClientLevel level, Vec3 at, int color, int count, double speed, RandomSource r) {
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(color, 0.4F), at.x, at.y, at.z, r.nextGaussian() * speed,
                    r.nextGaussian() * speed, r.nextGaussian() * speed);
        }
    }

    /** The landing's dust in a ring. */
    private static void ground(ClientLevel level, Vec3 at, int count, RandomSource r) {
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.2, at.z));
        if (ground.isAir()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double a = i * Mth.TWO_PI / count;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x + Math.cos(a) * 0.5, at.y
                    + 0.1, at.z + Math.sin(a) * 0.5, Math.cos(a) * 0.15, 0.1, Math.sin(a) * 0.15);
        }
    }

    private WolverineEffects() {
    }
}
