package com.kolodkingal.enhanced.client.captain;

import com.kolodkingal.enhanced.captain.CaptainAbilities;
import com.kolodkingal.enhanced.client.render.CaptainActing;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What everyone near sees of Captain America's moves (3.8), drawn from {@link CaptainFxPayload}: a rush of air at the
 * throw, a glint at the catch, the spark and the streak of a ricochet from foe to foe, the silver ring of a block and
 * the sparks of a reflected shot, the slam's ring of dust, the leap's and the charge's dust, the bash's spark, the
 * blue-white ring of Assemble and the lights over his allies, Inspire's golden sparkles, the punches' and kicks' hits.
 * Each kind also starts its gesture ({@link CaptainActing#onPayload}).
 */
public final class CaptainEffects {
    private static final int SILVER = CaptainAbilities.SILVER;
    private static final int BLUE = 0x6FA0FF;
    private static final int RED = CaptainAbilities.RED;
    private static final int GOLD = 0xFFD86A;

    /** Registers a drawer for every kind (the client setup calls it once). */
    public static void register() {
        for (int kind = CaptainFxPayload.THROW; kind <= CaptainFxPayload.FLIP; kind++) {
            CaptainFx.register(kind, CaptainEffects::draw);
        }
    }

    static void draw(CaptainFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        CaptainActing.onPayload(p);
        Entity who = p.entityId() >= 0 ? level.getEntity(p.entityId()) : null;
        Vec3 at = p.points().isEmpty() ? (who != null ? who.position().add(0, 1.0, 0) : null) : p.points().get(0);
        if (at == null) {
            return;
        }
        RandomSource r = level.random;
        switch (p.kind()) {
            case CaptainFxPayload.THROW -> {
                for (int i = 0; i < 8; i++) {
                    level.addParticle(ParticleTypes.CLOUD, at.x + r.nextGaussian() * 0.15, at.y
                            + r.nextGaussian() * 0.1, at.z + r.nextGaussian() * 0.15, 0, 0.01, 0);
                }
            }
            case CaptainFxPayload.CATCH -> burst(level, at, SILVER, 6, 0.12, r);
            case CaptainFxPayload.RICOCHET -> {
                for (int i = 0; i < 10; i++) {
                    level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, r.nextGaussian() * 0.35,
                            r.nextDouble() * 0.3, r.nextGaussian() * 0.35);
                }
                burst(level, at, SILVER, 8, 0.2, r);
                if (p.points().size() > 1) {
                    streak(level, at, p.points().get(1), r);
                }
            }
            case CaptainFxPayload.BLOCK -> ring(level, at, who, SILVER, 0.55, 14);
            case CaptainFxPayload.REFLECT -> {
                for (int i = 0; i < 10; i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, r.nextGaussian() * 0.25,
                            r.nextGaussian() * 0.25, r.nextGaussian() * 0.25);
                }
                ring(level, at, who, SILVER, 0.45, 10);
            }
            case CaptainFxPayload.SLAM -> {
                if (p.arg() > 0) {
                    shockwave(level, at, p.arg() == 2 ? 4.0 : 3.0, r);
                    for (int i = 1; i < p.points().size(); i++) {
                        Vec3 hit = p.points().get(i);
                        level.addParticle(ParticleTypes.CRIT, hit.x, hit.y + 0.8, hit.z, 0, 0.3, 0);
                    }
                }
            }
            case CaptainFxPayload.LEAP -> {
                if (who instanceof AbstractClientPlayer player) {
                    CaptainActing.dust(level, player, 12);
                }
            }
            case CaptainFxPayload.CHARGE -> {
                if (p.arg() == 1) {
                    for (int i = 0; i < 10; i++) {
                        level.addParticle(ParticleTypes.CRIT, at.x, at.y + 1.0, at.z, r.nextGaussian() * 0.3,
                                r.nextDouble() * 0.3, r.nextGaussian() * 0.3);
                    }
                }
            }
            case CaptainFxPayload.BASH -> {
                if (!p.points().isEmpty()) {
                    for (int i = 0; i < 12; i++) {
                        level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, r.nextGaussian() * 0.4,
                                r.nextDouble() * 0.3, r.nextGaussian() * 0.4);
                    }
                    burst(level, at, SILVER, 6, 0.18, r);
                }
            }
            case CaptainFxPayload.ASSEMBLE -> {
                if (who != null) {
                    Vec3 feet = who.position();
                    for (int i = 0; i < 48; i++) {
                        double a = Mth.TWO_PI * i / 48.0;
                        double cos = Math.cos(a);
                        double sin = Math.sin(a);
                        level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? BLUE : SILVER, 0.9F),
                                feet.x + cos * 0.6, feet.y + 0.15, feet.z + sin * 0.6, cos * 0.45, 0.01, sin * 0.45);
                    }
                    Vec3 top = feet.add(0, who.getBbHeight() + 0.6, 0);
                    burst(level, top, BLUE, 10, 0.2, r);
                }
                for (int i = who != null ? 1 : 0; i < p.points().size(); i++) {
                    Vec3 ally = p.points().get(i);
                    for (int k = 0; k < 6; k++) {
                        level.addParticle(new EnergyParticleOptions(k % 2 == 0 ? BLUE : RED, 0.6F),
                                ally.x + r.nextGaussian() * 0.3, ally.y + 1.8 + r.nextDouble() * 0.4,
                                ally.z + r.nextGaussian() * 0.3, 0, 0.03, 0);
                    }
                }
            }
            case CaptainFxPayload.INSPIRE -> {
                for (Vec3 ally : p.points()) {
                    for (int k = 0; k < 8; k++) {
                        level.addParticle(new EnergyParticleOptions(GOLD, 0.6F), ally.x + r.nextGaussian() * 0.35,
                                ally.y + 0.3 + r.nextDouble() * 1.6, ally.z + r.nextGaussian() * 0.35, 0, 0.04, 0);
                    }
                    level.addParticle(ParticleTypes.HEART, ally.x, ally.y + 2.1, ally.z, 0, 0.05, 0);
                }
            }
            case CaptainFxPayload.PUNCH, CaptainFxPayload.KICK, CaptainFxPayload.FLIP -> {
                if (!p.points().isEmpty()) {
                    int n = p.kind() == CaptainFxPayload.PUNCH ? 5 : 10;
                    for (int i = 0; i < n; i++) {
                        level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, r.nextGaussian() * 0.3,
                                r.nextDouble() * 0.25, r.nextGaussian() * 0.3);
                    }
                    if (p.kind() == CaptainFxPayload.KICK) {
                        level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0, 0, 0);
                    }
                }
            }
            default -> {
            }
        }
    }

    /** A little burst of light. */
    static void burst(ClientLevel level, Vec3 at, int color, int count, double speed, RandomSource r) {
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(color, 0.7F), at.x, at.y, at.z, r.nextGaussian() * speed,
                    r.nextGaussian() * speed, r.nextGaussian() * speed);
        }
    }

    /** The shield's way from one foe to the next: a faint silver streak. */
    static void streak(ClientLevel level, Vec3 from, Vec3 to, RandomSource r) {
        Vec3 d = to.subtract(from);
        int n = Math.max(2, (int) (d.length() * 3.0));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.addParticle(new EnergyParticleOptions(SILVER, 0.35F), p.x, p.y, p.z, 0, 0, 0);
        }
    }

    /**
     * A ring of light at the shield's face, square to the way the blow came (round {@code who}'s chest when it came
     * from nowhere in particular).
     */
    static void ring(ClientLevel level, Vec3 at, Entity who, int color, double radius, int count) {
        Vec3 normal = who != null ? at.subtract(who.position().add(0, who.getBbHeight() * 0.6, 0)) : new Vec3(1, 0,
                0);
        normal = new Vec3(normal.x, 0, normal.z);
        normal = normal.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : normal.normalize();
        Vec3 side = new Vec3(-normal.z, 0, normal.x);
        Vec3 up = new Vec3(0, 1, 0);
        for (int i = 0; i < count; i++) {
            double a = Mth.TWO_PI * i / count;
            Vec3 dir = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vec3 p = at.add(dir.scale(radius));
            level.addParticle(new EnergyParticleOptions(color, 0.6F), p.x, p.y, p.z, dir.x * 0.06, dir.y * 0.06,
                    dir.z * 0.06);
        }
    }

    /** The slam's ring of dust and stone running out over the ground. */
    static void shockwave(ClientLevel level, Vec3 at, double radius, RandomSource r) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.2, at.z);
        BlockState ground = level.getBlockState(below);
        int count = 40;
        for (int i = 0; i < count; i++) {
            double a = Mth.TWO_PI * i / count;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            if (!ground.isAir()) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x + cos * 0.5, at.y + 0.1,
                        at.z + sin * 0.5, cos * radius * 0.12, 0.12, sin * radius * 0.12);
            }
            if (i % 3 == 0) {
                level.addParticle(ParticleTypes.POOF, at.x + cos * radius * 0.6, at.y + 0.2, at.z + sin * radius
                        * 0.6, cos * 0.05, 0.02, sin * 0.05);
            }
        }
        level.addParticle(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 0, 0, 0);
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.CRIT, at.x, at.y + 0.3, at.z, r.nextGaussian() * 0.4, 0.3,
                    r.nextGaussian() * 0.4);
        }
    }

    private CaptainEffects() {
    }
}
