package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws Billy's Hex Barrier (3.2) for everyone who sees it: a tall, slightly curved, translucent blue plate held in front of
 * him on his raised forearm, following the way he looks. It has the engraved contour lines of a hologram
 * ({@code billy_barrier.png}) and, at the height of the hand, a rune seal ({@code billy_seal.png}: a ring of teeth that turns
 * slowly, a hexagram that flickers, a diamond and two arrows). Where it takes a hit it flickers, a ring runs across it and
 * blue lightning crackles over it; as its strength drops it cracks, fades and gutters. Its outline (a wavy top, rounded feet)
 * is the one the plate texture was drawn for ({@link #topEdge}, {@link #bottomEdge}).
 */
public final class BarrierRenderer {
    static final ResourceLocation PLATE = Enhanced.id("textures/entity/billy_barrier.png");
    static final ResourceLocation SEAL = Enhanced.id("textures/entity/billy_seal.png");

    private static final int COLUMNS = 14;
    private static final int ROWS = 20;
    /** How far the plate's sides curve toward him (blocks, at the edge): it wraps a little round the arm. */
    private static final float BOW = 0.10F;
    private static final double MAX_DISTANCE = 64.0;

    /** What the client remembers of one barrier: the most strength it was seen with, and when it last took a hit. */
    private static final class Mark {
        float peak;
        float strength;
        float hitAt = -100.0F;
        /** How far it had formed when last drawn up, and when it was lowered (NaN while it is up). */
        float formed;
        float loweredAt = Float.NaN;
    }

    /** How long a lowered barrier takes to fade away (3.3: it vanished from one frame to the next). */
    private static final float FADE_TICKS = 5.0F;

    private static final Map<Integer, Mark> MARKS = new HashMap<>();

    /** The top outline of the plate at {@code u} (-1 to 1 across), as a fraction of its half height. */
    static float topEdge(float u) {
        return 0.95F + 0.04F * Mth.sin(2.2F * u + 0.6F);
    }

    /** The bottom outline: the feet are rounded. */
    static float bottomEdge(float u) {
        return -0.96F + 0.10F * Math.abs(u * u * u);
    }

    public static void clear() {
        MARKS.clear();
    }

    /** Draws every barrier in range. */
    static void render(Minecraft mc, ClientLevel level, MultiBufferSource.BufferSource buffers, Matrix4f pose, Vec3 camera, float partialTick, float time) {
        boolean any = false;
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            boolean up = data.power() == Power.BILLY && data.chaosShieldActive();
            Mark lowered = MARKS.get(player.getId());
            if (!up && lowered != null && !player.isInvisible() && !player.isSpectator()
                    && data.power() == Power.BILLY) {
                // Lowered or broken: it fades away where he holds it.
                if (Float.isNaN(lowered.loweredAt)) {
                    lowered.loweredAt = time;
                }
                float out = 1.0F - (time - lowered.loweredAt) / FADE_TICKS;
                if (out > 0.0F && time >= lowered.loweredAt) {
                    any = true;
                    Vec3 body = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.55, 0);
                    draw(mc, player, lowered.formed, out, lowered, body, buffers, pose, camera, partialTick, time);
                    continue;
                }
            }
            if (!up || player.isInvisible() || player.isSpectator()) {
                MARKS.remove(player.getId());
                continue;
            }
            Vec3 body = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.55, 0);
            if (body.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            any = true;
            Mark mark = MARKS.computeIfAbsent(player.getId(), id -> new Mark());
            mark.loweredAt = Float.NaN;
            mark.formed = data.chaosShieldProgress(time);
            float strength = data.shieldStrength();
            if (strength > mark.peak || mark.peak <= 0.0F) {
                mark.peak = strength;
            } else if (strength < mark.strength - 0.01F) {
                mark.hitAt = time;
            }
            mark.strength = strength;
            draw(mc, player, mark.formed, 1.0F, mark, body, buffers, pose, camera, partialTick, time);
        }
        if (!any) {
            MARKS.clear();
        }
    }

    /**
     * Draws one barrier.
     *
     * @param formed how far it has formed (0 to 1)
     * @param out    how much of it is still there (1 while it is up; down to 0 as a lowered one fades)
     */
    private static void draw(Minecraft mc, AbstractClientPlayer player, float formed, float out, Mark mark, Vec3 body,
                             MultiBufferSource.BufferSource buffers, Matrix4f pose, Vec3 camera, float partialTick,
                                     float time) {
        float grow = formed * formed * (3.0F - 2.0F * formed);
        float fraction = mark.peak <= 0.0F ? 1.0F : Mth.clamp(mark.strength / mark.peak, 0.0F, 1.0F);
        boolean reduced = EnhancedClientConfig.reducedMotion();
        // It gutters as it nears breaking: a slow uneasy pulse, never a strobe.
        float flicker = fraction < 0.3F && !reduced ? 0.82F + 0.18F * Mth.sin(time * 0.45F) : 1.0F;
        float hit = Mth.clamp(1.0F - (time - mark.hitAt) / 7.0F, 0.0F, 1.0F);
        boolean mine = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        float see = mine ? 0.4F : 1.0F; // the plate is right in front of his own eyes: fainter there
        float alpha = (0.35F + 0.65F * grow) * (0.55F + 0.45F * fraction) * flicker * see * out;

        BillyBarrier.Frame plate = BillyBarrier.frame(body, player.getViewVector(partialTick));
        double width = BillyBarrier.width() * (0.4 + 0.6 * grow);
        double height = BillyBarrier.height() * (0.3 + 0.7 * grow);
        // Held on the off forearm: a little to that side.
        double side = player.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
        Vec3 center = plate.center().add(plate.right().scale(side * 0.22));
        Vec3 c = center.subtract(camera);

        // 1. the translucent body of the plate
        VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        surface(veil, pose, c, plate, width, height, alpha * (0.16F + 0.20F * hit), false);
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
        // 2. its engraved lines
        RenderType lines = EnhancedRenderTypes.energyField(PLATE);
        surface(buffers.getBuffer(lines), pose, c, plate, width, height, alpha * (0.85F + 0.4F * hit), true);
        buffers.endBatch(lines);
        // 3. the rune seal at the height of the hand: the teeth turn slowly, the hexagram flickers
        RenderType seal = EnhancedRenderTypes.energyField(SEAL);
        float turn = reduced ? 0.0F : time * 0.035F;
        float pulse = reduced ? 1.0F : 0.82F + 0.18F * Mth.sin(time * 1.7F) * Mth.cos(time * 0.9F);
        sealQuad(buffers.getBuffer(seal), pose, c, plate, width, height, turn, alpha * pulse * (0.9F + 0.5F * hit));
        buffers.endBatch(seal);
        // 4. crackle, cracks, the ring of a hit
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        // (the cracks stay plain even on his own plate, which is drawn faint: he must see it wearing)
        crackle(vc, pose, c, plate, width, height, mark, fraction, hit, player.getId(), time, alpha, alpha / see, reduced);
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    /** The point of the plate at {@code u} across and {@code t} up the outline (0 at the feet, 1 at the top), relative to the camera. */
    private static Vec3 point(Vec3 c, BillyBarrier.Frame plate, double width, double height, float u, float t) {
        float v = Mth.lerp(t, bottomEdge(u), topEdge(u));
        return c.add(plate.right().scale(u * width * 0.5)).add(plate.up().scale(v * height * 0.5)).add(plate.forward().scale(-BOW * u * u));
    }

    /** The plate as a grid of quads, plain or textured. */
    private static void surface(VertexConsumer vc, Matrix4f pose, Vec3 c, BillyBarrier.Frame plate, double width, double height, float alpha,
                                boolean textured) {
        int rgb = textured ? BillyAbilities.INNER : BillyAbilities.BODY;
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        for (int i = 0; i < COLUMNS; i++) {
            float u0 = -1.0F + 2.0F * i / COLUMNS;
            float u1 = -1.0F + 2.0F * (i + 1) / COLUMNS;
            for (int j = 0; j < ROWS; j++) {
                float t0 = j / (float) ROWS;
                float t1 = (j + 1) / (float) ROWS;
                vertex(vc, pose, point(c, plate, width, height, u0, t0), u0, t0, r, g, b, alpha, textured);
                vertex(vc, pose, point(c, plate, width, height, u1, t0), u1, t0, r, g, b, alpha, textured);
                vertex(vc, pose, point(c, plate, width, height, u1, t1), u1, t1, r, g, b, alpha, textured);
                vertex(vc, pose, point(c, plate, width, height, u0, t1), u0, t1, r, g, b, alpha, textured);
            }
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Vec3 p, float u, float t, float r, float g, float b, float alpha, boolean textured) {
        // (the texture was drawn with the outline's own coordinates: across 0..1, down from the top)
        float v = Mth.lerp(t, bottomEdge(u), topEdge(u));
        float texU = 0.5F + u * 0.5F;
        float texV = 0.5F - v * 0.5F;
        if (textured) {
            vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setUv(texU, texV).setColor(r, g, b, Mth.clamp(alpha, 0.0F, 1.0F));
        } else {
            vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Mth.clamp(alpha * (0.7F + 0.5F * Math.abs(u)), 0.0F, 1.0F));
        }
    }

    /** The seal: a square turning in the plane of the plate, a little in front of it, at the height of the hand. */
    private static void sealQuad(VertexConsumer vc, Matrix4f pose, Vec3 c, BillyBarrier.Frame plate, double width, double height, float turn, float alpha) {
        double half = width * 0.40;
        Vec3 middle = c.add(plate.up().scale(0.10 * height * 0.5)).add(plate.forward().scale(0.02));
        float cos = Mth.cos(turn);
        float sin = Mth.sin(turn);
        float[][] corner = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        float a = Mth.clamp(alpha, 0.0F, 1.0F);
        for (float[] k : corner) {
            float x = (k[0] * cos - k[1] * sin);
            float y = (k[0] * sin + k[1] * cos);
            Vec3 p = middle.add(plate.right().scale(x * half)).add(plate.up().scale(y * half));
            vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setUv(k[2], k[3]).setColor(1.0F, 1.0F, 1.0F, a);
        }
    }

    /** Tesla crackle over the plate, the cracks of a weak one, and the ring that runs out from a hit. */
    private static void crackle(VertexConsumer vc, Matrix4f pose, Vec3 c, BillyBarrier.Frame plate, double width, double height, Mark mark,
                                float fraction, float hit, int id, float time, float alpha, float crackAlpha, boolean reduced) {
        // A little lightning along the edge, more when it has been hit. (3.3) Each arc fades in and out over two ticks
        // instead of popping up somewhere new every other tick.
        int arcs = reduced ? 0 : 1 + (hit > 0.0F ? 3 : 0);
        EnergyGeometry.crossfade(id * 104729L, time / 2.0F, (random, weight) -> {
            for (int n = 0; n < arcs; n++) {
                Vec3 from = surfacePoint(c, plate, width, height, random.nextFloat() * 2.0F - 1.0F, random.nextFloat());
                Vec3 to = surfacePoint(c, plate, width, height,
                        Mth.clamp((float) (from.subtract(c).dot(plate.right()) / (width * 0.5))
                        + (random.nextFloat() - 0.5F) * 0.9F, -1.0F, 1.0F), random.nextFloat());
                jag(vc, pose, from, to, 0.5F + hit, alpha * weight, random);
            }
        });
        // Cracks: white jagged lines across it, more as its strength goes.
        int cracks = Math.round((1.0F - fraction) * 7.0F);
        RandomSource fixed = RandomSource.create(id * 31L + Math.round(mark.peak * 10.0F));
        for (int n = 0; n < cracks; n++) {
            Vec3 from = surfacePoint(c, plate, width, height, fixed.nextFloat() * 1.6F - 0.8F, 0.25F + fixed.nextFloat() * 0.5F);
            Vec3 to = surfacePoint(c, plate, width, height, fixed.nextFloat() * 1.6F - 0.8F, fixed.nextFloat());
            jag(vc, pose, from, to, 0.8F, crackAlpha, fixed);
        }
        // The ring of a hit runs out across the plate from its middle.
        if (hit > 0.0F) {
            float age = 1.0F - hit;
            float radius = (float) (width * 0.12 + age * width * 0.55);
            EnergyGeometry.ring(pose, vc, c.add(plate.forward().scale(0.03)), plate.forward(), radius, 0.05F + 0.04F * hit, 20,
                    EnergyGeometry.argb(BillyAbilities.MAIN, 0.8F * hit));
            EnergyGeometry.ring(pose, vc, c.add(plate.forward().scale(0.03)), plate.forward(), radius * 0.9F, 0.03F, 16,
                    EnergyGeometry.argb(BillyAbilities.CORE, 0.9F * hit));
        }
    }

    private static Vec3 surfacePoint(Vec3 c, BillyBarrier.Frame plate, double width, double height, float u, float t) {
        return point(c, plate, width, height, Mth.clamp(u, -1.0F, 1.0F), Mth.clamp(t, 0.0F, 1.0F)).add(plate.forward().scale(0.02));
    }

    /** One jagged arc in Billy's layers: the blue, the pale light and the white-hot core. */
    private static void jag(VertexConsumer vc, Matrix4f pose, Vec3 from, Vec3 to, float strength, float alpha, RandomSource random) {
        double length = from.distanceTo(to);
        if (length < 0.05) {
            return;
        }
        List<Vec3> path = EnergyGeometry.lightningPath(from, to, Math.max(0.05, length * 0.12), random);
        float a = Mth.clamp(alpha * strength, 0.0F, 1.0F);
        EnergyGeometry.polyline(pose, vc, path, 0.07F, EnergyGeometry.argb(BillyAbilities.BODY, 0.45F * a));
        EnergyGeometry.polyline(pose, vc, path, 0.035F, EnergyGeometry.argb(BillyAbilities.MAIN, 0.85F * a));
        EnergyGeometry.polyline(pose, vc, path, 0.014F, EnergyGeometry.argb(BillyAbilities.CORE, a));
    }

    private BarrierRenderer() {
    }
}
