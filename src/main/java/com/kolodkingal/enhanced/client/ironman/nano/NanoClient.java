package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.client.ironman.IronFx;
import com.kolodkingal.enhanced.client.render.ArenaFx;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.ironman.IronMissileEntity;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The client side of the nanotech suits and the Hulkbuster (3.8, lane L4): the drawers of their effects ({@link
 * IronFx}: the blades coming out and in and their slashes, the shields' hits, the nano cannon's charge and blast, the
 * Lightning Refocuser's chain, the missiles' locks, the drones forming and going and their laser flashes, the grab and
 * the throw, Veronica's falling arm, the frame's landing), the wide chest beams' drawers ({@link NanoBeamDrawers}), the
 * homing missiles' blue trails, and the short-lived flashes in the world (drawn after the particles, everything
 * interpolated with the partial tick). The blades and shields on the body are {@link NanoGearLayer}'s, the drones
 * {@link NanoDroneRenderer}'s, the Hulkbuster's body {@code HulkbusterRenderer}'s.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class NanoClient {
    /** A flash in the world: what it is, its points, its argument, when it began (client game time) and how long. */
    private record Flash(int kind, List<Vec3> points, int arg, long start, int ticks) {
    }

    private static final List<Flash> FLASHES = new ArrayList<>();
    /** The creatures locked by someone's missiles (entity id → when the lock shows until). */
    private static final Map<Integer, Long> LOCKS = new HashMap<>();
    /** How long a lock's brackets show. */
    static final int LOCK_TICKS = 40;
    /** The drones' laser: red-gold. */
    static final int DRONE_CORE = 0xFFF0C8;
    static final int DRONE_BODY = 0xFF6A2A;
    static final int BOLT_CORE = 0xF4FDFF;
    static final int BOLT_BODY = 0x7FD8FF;

    /** Registers the drawers (from the client setup, region nanotech). */
    public static void setup() {
        int[] kinds = {IronFxPayload.BLADE_OUT, IronFxPayload.BLADE_IN, IronFxPayload.BLADE_SLASH,
                IronFxPayload.SHIELD_HIT, IronFxPayload.CANNON_CHARGE, IronFxPayload.CANNON_FIRE,
                IronFxPayload.REFOCUSER, IronFxPayload.LOCK, IronFxPayload.DRONES_IN, IronFxPayload.DRONES_OUT,
                IronFxPayload.DRONE_SHOT, IronFxPayload.GRAB, IronFxPayload.THROW, IronFxPayload.VERONICA,
                IronFxPayload.FRAME_LAND};
        for (int kind : kinds) {
            IronFx.register(kind, NanoClient::draw);
        }
        BeamRenderer.registerDrawer(AvengersBeams.CHEST_LASER, NanoBeamDrawers.CHEST_LASER);
        BeamRenderer.registerDrawer(AvengersBeams.HULKBUSTER_UNIBEAM, NanoBeamDrawers.HEAVY_UNIBEAM);
    }

    /** One effect arrived. */
    static void draw(IronFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource r = level.random;
        List<Vec3> points = payload.points();
        Vec3 first = points.isEmpty() ? null : points.get(0);
        long now = level.getGameTime();
        switch (payload.kind()) {
            case IronFxPayload.BLADE_OUT, IronFxPayload.BLADE_IN -> {
                for (Vec3 at : points) {
                    burst(level, at, 0x6FD8FF, 10, 0.12);
                }
            }
            case IronFxPayload.BLADE_SLASH -> {
                if (first != null) {
                    for (int i = 0; i < 6; i++) {
                        level.addParticle(ParticleTypes.SWEEP_ATTACK, first.x + r.nextGaussian() * 0.3, first.y
                                + r.nextGaussian() * 0.2, first.z + r.nextGaussian() * 0.3, 0, 0, 0);
                    }
                    burst(level, first, 0x8FE8FF, 8, 0.2);
                }
            }
            case IronFxPayload.SHIELD_HIT -> {
                if (first != null) {
                    burst(level, first, 0x9FE8FF, 14, 0.25);
                    for (int i = 0; i < 6; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, first.x, first.y, first.z,
                                r.nextGaussian() * 0.25, r.nextGaussian() * 0.25, r.nextGaussian() * 0.25);
                    }
                }
            }
            case IronFxPayload.CANNON_CHARGE -> {
                if (first != null) {
                    burst(level, first, 0x9FE8FF, 12, 0.15);
                }
            }
            case IronFxPayload.CANNON_FIRE -> {
                if (points.size() >= 2) {
                    FLASHES.add(new Flash(payload.kind(), points, 0, now, 8));
                    burst(level, points.get(1), 0xFFFFFF, 18, 0.35);
                }
            }
            case IronFxPayload.REFOCUSER -> {
                if (points.size() >= 2) {
                    FLASHES.add(new Flash(payload.kind(), points, payload.arg(), now, 10));
                    for (Vec3 at : points.subList(1, points.size())) {
                        burst(level, at, 0xBFF4FF, 10, 0.3);
                    }
                }
            }
            case IronFxPayload.LOCK -> LOCKS.put(payload.arg(), now + LOCK_TICKS);
            case IronFxPayload.DRONES_OUT, IronFxPayload.DRONES_IN -> {
                for (Vec3 at : points) {
                    burst(level, at.add(0, 0.2, 0), 0xD8323C, 12, 0.18);
                    burst(level, at.add(0, 0.2, 0), 0xE8B23A, 8, 0.12);
                }
            }
            case IronFxPayload.DRONE_SHOT -> {
                if (points.size() >= 2) {
                    FLASHES.add(new Flash(payload.kind(), points, 0, now, 6));
                    burst(level, points.get(1), 0xFFB040, 6, 0.2);
                }
            }
            case IronFxPayload.GRAB, IronFxPayload.THROW -> {
                if (first != null) {
                    for (int i = 0; i < 8; i++) {
                        level.addParticle(ParticleTypes.CRIT, first.x, first.y + 0.5, first.z,
                                r.nextGaussian() * 0.3, r.nextGaussian() * 0.3, r.nextGaussian() * 0.3);
                    }
                }
            }
            case IronFxPayload.VERONICA -> {
                if (points.size() >= 2) {
                    FLASHES.add(new Flash(payload.kind(), points, 0, now, Math.max(1, payload.arg())));
                }
            }
            case IronFxPayload.FRAME_LAND -> {
                if (first != null) {
                    ArenaFx.shake(0.5F, 14, first);
                    for (int i = 0; i < 30; i++) {
                        double a = i * Math.PI * 2 / 30;
                        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, first.x + Math.cos(a) * 1.5, first.y
                                + 0.1, first.z + Math.sin(a) * 1.5, Math.cos(a) * 0.08, 0.01, Math.sin(a) * 0.08);
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void burst(ClientLevel level, Vec3 at, int color, int count, double spread) {
        RandomSource r = level.random;
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(color, 0.8F), at.x, at.y, at.z, r.nextGaussian() * spread,
                    r.nextGaussian() * spread, r.nextGaussian() * spread);
        }
    }

    /** Whether someone's missiles have this creature locked now (the brackets). */
    public static boolean locked(Entity entity) {
        Long until = LOCKS.get(entity.getId());
        return until != null && entity.level().getGameTime() < until;
    }

    // ---- every client tick ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            if (level == null) {
                clear();
            }
            return;
        }
        long now = level.getGameTime();
        FLASHES.removeIf(f -> now - f.start() > f.ticks() + 1 || now < f.start());
        LOCKS.values().removeIf(until -> now >= until + 1 || until - now > LOCK_TICKS * 4);
        // The homing missiles' blue trail (the flame and smoke behind every missile are lane L3's).
        if (mc.player != null) {
            for (IronMissileEntity missile : level.getEntitiesOfClass(IronMissileEntity.class,
                    mc.player.getBoundingBox().inflate(64.0), m -> m.kind() == IronMissileEntity.HOMING)) {
                Vec3 at = missile.position();
                Vec3 back = missile.getDeltaMovement().scale(-0.5);
                level.addParticle(new EnergyParticleOptions(0x9FE8FF, 0.45F), at.x + back.x, at.y + back.y,
                        at.z + back.z, 0, 0, 0);
            }
        }
        // Veronica's arm leaves a trail of fire as it falls.
        for (Flash f : FLASHES) {
            if (f.kind() == IronFxPayload.VERONICA) {
                Vec3 at = veronica(f, 0.0F, now);
                level.addParticle(ParticleTypes.FLAME, at.x, at.y + 0.6, at.z, 0, 0.05, 0);
                level.addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 0, 0.02, 0);
            }
        }
    }

    /** Where Veronica's arm is in its fall (straight down, speeding up), at {@code now} + {@code partial}. */
    static Vec3 veronica(Flash f, float partial, long now) {
        float t = Mth.clamp((now - f.start() + partial) / f.ticks(), 0.0F, 1.0F);
        return f.points().get(0).lerp(f.points().get(1), t * t);
    }

    public static void clear() {
        FLASHES.clear();
        LOCKS.clear();
    }

    // ---- the flashes, drawn after the particles ----

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || FLASHES.isEmpty() && LOCKS.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        long now = level.getGameTime();
        float time = now + partial;
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Flash f : FLASHES) {
            float age = time - f.start();
            float fade = Mth.clamp(1.0F - age / f.ticks(), 0.0F, 1.0F);
            if (fade <= 0.0F) {
                continue;
            }
            switch (f.kind()) {
                case IronFxPayload.DRONE_SHOT -> laser(pose, vc, f.points().get(0).subtract(cam),
                        f.points().get(1).subtract(cam), 0.09F, DRONE_CORE, DRONE_BODY, fade);
                case IronFxPayload.CANNON_FIRE -> laser(pose, vc, f.points().get(0).subtract(cam),
                        f.points().get(1).subtract(cam), 0.45F, 0xFFFFFF, 0x7FD8FF, fade);
                case IronFxPayload.REFOCUSER -> {
                    float width = f.arg() == 1 ? 0.12F : 0.08F;
                    long seed = f.start() * 31L + (long) (age / 2.0F);
                    for (int i = 0; i + 1 < f.points().size(); i++) {
                        Vec3 a = f.points().get(i).subtract(cam);
                        Vec3 b = f.points().get(i + 1).subtract(cam);
                        EnergyGeometry.bolt(pose, vc, a, b, width * 2.2F, 0.35F,
                                EnergyGeometry.argb(BOLT_BODY, 0.55F * fade), seed + i);
                        EnergyGeometry.bolt(pose, vc, a, b, width, 0.35F, EnergyGeometry.argb(BOLT_CORE, fade),
                                seed + i);
                    }
                }
                case IronFxPayload.VERONICA -> {
                    Vec3 at = veronica(f, partial, now).subtract(cam);
                    Vec3 above = at.add(0, 3.0, 0);
                    EnergyGeometry.ribbon(pose, vc, at, above, 0.9F, 0.2F, EnergyGeometry.argb(0xFFB040, 0.8F),
                            EnergyGeometry.argb(0xFF6A2A, 0.0F));
                    EnergyGeometry.ribbon(pose, vc, at.add(0, -0.6, 0), at.add(0, 0.6, 0), 0.55F,
                            EnergyGeometry.argb(0xD8323C, 0.9F), EnergyGeometry.argb(0xE8B23A, 0.9F));
                }
                default -> {
                }
            }
        }
        for (Map.Entry<Integer, Long> lock : LOCKS.entrySet()) {
            Entity target = level.getEntity(lock.getKey());
            if (target == null || !target.isAlive() || now >= lock.getValue()) {
                continue;
            }
            brackets(pose, vc, target, camera, cam, partial, time);
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    /** A straight flash of light from {@code a} to {@code b}: a coloured body round a white core. */
    private static void laser(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, int core, int body,
                              float fade) {
        EnergyGeometry.ribbon(pose, vc, a, b, width * 2.5F, EnergyGeometry.argb(body, 0.45F * fade),
                EnergyGeometry.argb(body, 0.35F * fade));
        EnergyGeometry.ribbon(pose, vc, a, b, width, EnergyGeometry.argb(core, fade), EnergyGeometry.argb(core,
                0.8F * fade));
    }

    /** The lock's four red corners round a creature, facing the camera, turning slowly closed. */
    private static void brackets(Matrix4f pose, VertexConsumer vc, Entity target, Camera camera, Vec3 cam,
                                 float partial, float time) {
        Vec3 center = target.getPosition(partial).add(0, target.getBbHeight() * 0.5, 0).subtract(cam);
        float half = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.6F + 0.2F;
        half *= 1.0F + 0.08F * Mth.sin(time * 0.6F);
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1.0);
        Vec3 up = new Vec3(camera.getUpVector());
        int color = EnergyGeometry.argb(0xFF3A2A, 0.95F);
        float arm = half * 0.45F;
        float w = 0.05F + half * 0.02F;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                Vec3 corner = center.add(right.scale(sx * half)).add(up.scale(sy * half));
                EnergyGeometry.ribbon(pose, vc, corner, corner.add(right.scale(-sx * arm)), w, color, color);
                EnergyGeometry.ribbon(pose, vc, corner, corner.add(up.scale(-sy * arm)), w, color, color);
            }
        }
    }

    private NanoClient() {
    }
}
