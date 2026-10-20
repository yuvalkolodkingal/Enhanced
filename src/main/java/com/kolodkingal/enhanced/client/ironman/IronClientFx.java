package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.IronManActing;
import com.kolodkingal.enhanced.client.render.IronManBeamFx;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * The client's side of Iron Man's effects (3.8, lane L3): what each {@link IronFxPayload} of the classic suits and the
 * suit-up draws (the gesture it starts is {@link IronManActing}'s): the repulsor's flash along its ray, the flares, the
 * missiles' puffs, the crash's dust, the punch's sparks, the Laser Burst's ring, the energy running out, a suit going
 * offline; the suit-up's sparks, steam and nanite glitter; and the Unibeam's own drawer (from the chest). Lanes L4 and
 * L7 register the drawers of their own kinds (a later registration of a kind replaces this one).
 */
public final class IronClientFx {
    private static final Vector3f RED = new Vector3f(1.0F, 0.3F, 0.25F);

    /** From the client setup: the drawers of the classic suits' kinds and the Unibeam's beam. */
    public static void setup() {
        int[] kinds = {IronFxPayload.SUIT_UP, IronFxPayload.SUIT_DOWN, IronFxPayload.SUIT_SKIP,
                IronFxPayload.REPULSOR, IronFxPayload.DOUBLE_BLAST, IronFxPayload.FLARES, IronFxPayload.MISSILES,
                IronFxPayload.FLAME_ON, IronFxPayload.FLAME_OFF, IronFxPayload.ARM_ROCKET, IronFxPayload.BOOST,
                IronFxPayload.CRASH, IronFxPayload.BRACE, IronFxPayload.PUNCH, IronFxPayload.DASH,
                IronFxPayload.LASER_BURST, IronFxPayload.ENERGY_EMPTY, IronFxPayload.OFFLINE};
        for (int kind : kinds) {
            IronFx.register(kind, IronClientFx::draw);
        }
        BeamRenderer.registerDrawer(AvengersBeams.UNIBEAM, IronManBeamFx.UNIBEAM);
    }

    /** One effect: its gesture, then its particles. */
    static void draw(IronFxPayload payload) {
        IronManActing.onPayload(payload);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity who = payload.entityId() < 0 ? null : level.getEntity(payload.entityId());
        List<Vec3> points = payload.points();
        RandomSource random = level.random;
        switch (payload.kind()) {
            case IronFxPayload.SUIT_UP, IronFxPayload.SUIT_DOWN -> suitUp(level, who, payload.arg(),
                    payload.kind() == IronFxPayload.SUIT_UP);
            case IronFxPayload.SUIT_SKIP -> {
                if (who != null) {
                    burst(level, who.position().add(0, who.getBbHeight() * 0.6, 0), 0x9FE8FF, 12, 0.6);
                }
            }
            case IronFxPayload.REPULSOR, IronFxPayload.DOUBLE_BLAST -> {
                if (points.size() >= 2) {
                    boolean both = points.size() >= 3;
                    IronWorldFx.ray(level, points.get(0), points.get(1), both ? 1.4F : 1.0F);
                    if (both) {
                        IronWorldFx.ray(level, points.get(2), points.get(1), 1.4F);
                    }
                    burst(level, points.get(1), 0xFFFFFF, 8, 0.25);
                }
            }
            case IronFxPayload.FLARES -> {
                for (Vec3 at : points) {
                    level.addParticle(ParticleTypes.FIREWORK, at.x, at.y, at.z, random.nextGaussian() * 0.05,
                            -0.05, random.nextGaussian() * 0.05);
                    level.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, 0, -0.02, 0);
                }
            }
            case IronFxPayload.MISSILES, IronFxPayload.ARM_ROCKET -> {
                for (Vec3 at : points) {
                    for (int i = 0; i < 4; i++) {
                        level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, random.nextGaussian() * 0.03,
                                0.04, random.nextGaussian() * 0.03);
                    }
                }
            }
            case IronFxPayload.CRASH -> {
                if (!points.isEmpty()) {
                    Vec3 at = points.get(0);
                    for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI * 2 / 24;
                        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x + Math.cos(a) * 0.6, at.y + 0.1,
                                at.z + Math.sin(a) * 0.6, Math.cos(a) * 0.12, 0.02, Math.sin(a) * 0.12);
                    }
                }
            }
            case IronFxPayload.PUNCH -> {
                if (points.size() >= 2) {
                    Vec3 at = points.get(1);
                    for (int i = 0; i < 8; i++) {
                        level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, random.nextGaussian() * 0.3,
                                random.nextGaussian() * 0.3, random.nextGaussian() * 0.3);
                    }
                }
            }
            case IronFxPayload.DASH -> {
                if (who != null) {
                    burst(level, who.position().add(0, 0.2, 0), 0x9FE8FF, 10, 0.3);
                }
            }
            case IronFxPayload.LASER_BURST -> {
                if (!points.isEmpty()) {
                    Vec3 c = points.get(0);
                    double r = Math.max(1, payload.arg());
                    IronWorldFx.burst(level, c, (float) r);
                    DustParticleOptions dust = new DustParticleOptions(RED, 1.0F);
                    for (int i = 0; i < 72; i++) {
                        double a = i * Math.PI * 2 / 72;
                        level.addParticle(dust, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0, 0, 0);
                    }
                    for (int i = 1; i < points.size(); i++) {
                        burst(level, points.get(i), 0xFF6A50, 6, 0.2);
                    }
                }
            }
            case IronFxPayload.ENERGY_EMPTY -> {
                if (who != null) {
                    for (int i = 0; i < 10; i++) {
                        level.addParticle(ParticleTypes.SMOKE, who.getX() + random.nextGaussian() * 0.2,
                                who.getY() + 0.1, who.getZ() + random.nextGaussian() * 0.2, 0, 0.05, 0);
                    }
                }
            }
            case IronFxPayload.OFFLINE -> {
                if (who != null) {
                    for (int i = 0; i < 12; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, who.getX() + random.nextGaussian() * 0.3,
                                who.getY() + 1.0 + random.nextGaussian() * 0.4, who.getZ()
                                        + random.nextGaussian() * 0.3, 0, 0, 0);
                    }
                }
            }
            default -> {
            }
        }
    }

    /**
     * A suit goes on or comes off: sparks and steam for the Mark 1, nanite glitter for the nanotech suits, blue sparks.
     */
    private static void suitUp(ClientLevel level, Entity who, int arg, boolean up) {
        if (who == null) {
            return;
        }
        SuitKind kind = SuitKind.of((byte) arg);
        RandomSource random = level.random;
        Vec3 chest = who.position().add(0, who.getBbHeight() * 0.68, 0);
        if (kind == SuitKind.MARK_1) {
            for (int i = 0; i < 10; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, chest.x + random.nextGaussian() * 0.3,
                        chest.y + random.nextGaussian() * 0.5, chest.z + random.nextGaussian() * 0.3, 0, 0, 0);
            }
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.CLOUD, chest.x, chest.y + 0.2, chest.z, random.nextGaussian() * 0.02,
                        0.05, random.nextGaussian() * 0.02);
            }
        } else if (kind != null && kind.nanotech()) {
            for (int i = 0; i < 20; i++) {
                level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? 0xFFD24A : 0xB3121B, 0.4F),
                        chest.x + random.nextGaussian() * 0.2, chest.y + random.nextGaussian() * 0.3,
                        chest.z + random.nextGaussian() * 0.2, random.nextGaussian() * 0.04, random.nextGaussian()
                                * 0.04, random.nextGaussian() * 0.04);
            }
        } else {
            burst(level, chest, up ? 0x9FE8FF : 0xC8C8C8, 10, 0.5);
        }
    }

    private static void burst(ClientLevel level, Vec3 at, int color, int count, double spread) {
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(color, 0.5F), at.x + random.nextGaussian() * spread * 0.3,
                    at.y + random.nextGaussian() * spread * 0.3, at.z + random.nextGaussian() * spread * 0.3,
                    random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
        }
    }

    /** Every client tick: the thrusters' sparks under a flying suit's boots (the cones are the layer's). */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || level.getGameTime() % 2 != 0) {
            return;
        }
        for (Player p : level.players()) {
            IronManState state = IronSuits.state(p);
            if (!state.has(IronManState.POWERED) || !p.getAbilities().flying && !com.kolodkingal.enhanced.power.Powers
                    .get(p).flying()) {
                continue;
            }
            level.addParticle(new EnergyParticleOptions(0x9FE8FF, 0.35F), p.getX() + level.random.nextGaussian()
                    * 0.08, p.getY() - 0.1, p.getZ() + level.random.nextGaussian() * 0.08, 0, -0.08, 0);
        }
    }

    public static void clear() {
        IronWorldFx.clear();
    }

    private IronClientFx() {
    }
}
