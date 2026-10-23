package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Iron Man's short flashes in the world (3.8, lane L3): a repulsor's ray (a white-hot core in a blue glow from the palm
 * to where it landed, thinning away over 5 ticks) and the Mark 5's Laser Burst (a thin red-white ring that sweeps once
 * round him at chest height over 8 ticks and fades). Kept for a few ticks after their effect arrives
 * ({@link IronClientFx}), drawn after the particles in the additive energy batch, every size a function of the frame's
 * time (partial ticks included).
 */
@Subscriber(side = Side.CLIENT)
public final class IronWorldFx {
    /** At most this many flashes at once (a fight of many Iron Men stays cheap). */
    static final int MAX = 48;
    static final int RAY_TICKS = 5;
    static final int BURST_TICKS = 10;

    private record Flash(boolean ring, Vec3 a, Vec3 b, float size, long start) {
    }

    private static final List<Flash> FLASHES = new ArrayList<>();

    /** A repulsor's ray from {@code from} to {@code to} ({@code size} 1: one palm; 1.6: both). */
    public static void ray(ClientLevel level, Vec3 from, Vec3 to, float size) {
        add(new Flash(false, from, to, size, level.getGameTime()));
    }

    /** The Laser Burst's ring round {@code centre}, of {@code radius}. */
    public static void burst(ClientLevel level, Vec3 centre, float radius) {
        add(new Flash(true, centre, centre, radius, level.getGameTime()));
    }

    private static void add(Flash flash) {
        if (FLASHES.size() >= MAX) {
            FLASHES.remove(0);
        }
        FLASHES.add(flash);
    }

    public static void clear() {
        FLASHES.clear();
    }

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FLASHES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            FLASHES.clear();
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = level.getGameTime() + partial;
        FLASHES.removeIf(f -> now - f.start() > (f.ring() ? BURST_TICKS : RAY_TICKS) || now < f.start() - 1);
        if (FLASHES.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Flash f : FLASHES) {
            float age = now - f.start();
            if (f.ring()) {
                ring(pose, vc, f, camera, age);
            } else {
                ray(pose, vc, f, camera, age);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    /** The ray: three layers that thin and fade as it ages; a flare where it lands. */
    private static void ray(Matrix4f pose, VertexConsumer vc, Flash f, Vec3 camera, float age) {
        float k = 1.0F - Mth.clamp(age / RAY_TICKS, 0.0F, 1.0F);
        Vec3 a = f.a().subtract(camera);
        Vec3 b = f.b().subtract(camera);
        float s = f.size();
        EnergyGeometry.ribbon(pose, vc, a, b, 0.32F * s * k, 0.22F * s * k, EnergyGeometry.argb(0x3FA8E8, 0.35F * k),
                EnergyGeometry.argb(0x3FA8E8, 0.25F * k));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.16F * s * k, 0.12F * s * k, EnergyGeometry.argb(0x9FE8FF, 0.7F * k),
                EnergyGeometry.argb(0x9FE8FF, 0.6F * k));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.06F * s * k, 0.05F * s * k, EnergyGeometry.argb(0xFFFFFF, 0.95F * k),
                EnergyGeometry.argb(0xFFFFFF, 0.9F * k));
        float flare = (0.35F + 0.4F * (1.0F - k)) * s;
        EnergyGeometry.ring(pose, vc, b, b.scale(-1), flare, 0.18F * s, 14, EnergyGeometry.argb(0x9FE8FF, 0.6F * k));
        EnergyGeometry.ring(pose, vc, a, a.scale(-1), 0.12F * s, 0.1F * s, 10, EnergyGeometry.argb(0xFFFFFF, 0.8F * k));
    }

    /** The burst: a faint whole ring, and the bright sweep that runs round it once in its first 8 ticks. */
    private static void ring(Matrix4f pose, VertexConsumer vc, Flash f, Vec3 camera, float age) {
        float k = 1.0F - Mth.clamp(age / BURST_TICKS, 0.0F, 1.0F);
        Vec3 c = f.a().subtract(camera);
        float r = f.size();
        Vec3 up = new Vec3(0, 1, 0);
        EnergyGeometry.ring(pose, vc, c, up, r, 0.05F, 48, EnergyGeometry.argb(0xFF5040, 0.45F * k));
        float sweep = Mth.clamp(age / (BURST_TICKS - 2.0F), 0.0F, 1.0F) * Mth.TWO_PI;
        int pieces = 16;
        for (int i = 0; i < pieces; i++) {
            float a0 = sweep - (i + 1) * 0.12F;
            float a1 = sweep - i * 0.12F;
            if (a1 < 0.0F) {
                continue;
            }
            Vec3 p = c.add(Math.cos(a0) * r, 0, Math.sin(a0) * r);
            Vec3 q = c.add(Math.cos(a1) * r, 0, Math.sin(a1) * r);
            float fade = (1.0F - i / (float) pieces) * k;
            EnergyGeometry.ribbon(pose, vc, p, q, 0.12F, 0.12F, EnergyGeometry.argb(0xFF6A50, 0.8F * fade),
                    EnergyGeometry.argb(0xFFFFFF, 0.9F * fade));
        }
    }

    private IronWorldFx() {
    }
}
