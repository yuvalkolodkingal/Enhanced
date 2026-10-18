package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * How Cyclops's two beams are drawn (3.3), as {@link BeamRenderer.Drawer}s of the shared beam system: straight, from his
 * eyes, never the jagged bolt of the magic beams. The Optic Blast is a tight ruby beam with a hot pink-white core and
 * pulses that run along it; the giant beam is three times as wide, with a dark red fringe, strands of heat spiralling
 * round it, and it goes through creatures to the block that stops it. Everything moves with the frame's time (partial ticks
 * included), so nothing jumps from one frame to the next.
 *
 * <p>In first person the beam cannot leave the camera itself (it would be a dot over the crosshair): it is drawn as two
 * beams from just below and either side of the view, one for each eye, meeting where he looks.
 */
public final class CyclopsBeamFx {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** The controlled Optic Blast: the widths (blocks) of its fringe, body, light, inner light and core. */
    private static final float[] OPTIC_WIDTHS = {0.34F, 0.21F, 0.12F, 0.05F, 0.02F};
    private static final float[] OPTIC_ALPHAS = {0.22F, 0.5F, 0.7F, 0.55F, 0.75F};
    /** The giant beam. */
    private static final float[] UNLEASHED_WIDTHS = {1.55F, 1.0F, 0.6F, 0.2F, 0.07F};
    private static final float[] UNLEASHED_ALPHAS = {0.24F, 0.42F, 0.5F, 0.45F, 0.7F};

    /** The Optic Blast: from the eyes to what the crosshair is on. */
    public static final BeamRenderer.Drawer OPTIC = new BeamRenderer.Drawer() {
        @Override
        public Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick) {
            return eyes(player, partialTick);
        }

        @Override
        public Vec3 end(AbstractClientPlayer player, BeamStyle style, float range, Vec3 crosshair, float partialTick) {
            return crosshair;
        }

        @Override
        public void draw(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b,
                         float partialTick, float time) {
            beams(mc, player, style, pose, vc, a, b, partialTick, time, OPTIC_WIDTHS, OPTIC_ALPHAS, false);
        }
    };

    /** The giant beam: from the eyes through every creature to the block (or the Chaos Shield) that stops it. */
    public static final BeamRenderer.Drawer UNLEASHED = new BeamRenderer.Drawer() {
        @Override
        public Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick) {
            return eyes(player, partialTick);
        }

        @Override
        public Vec3 end(AbstractClientPlayer player, BeamStyle style, float range, Vec3 crosshair, float partialTick) {
            Vec3 end = Raycasts.block(player, range, partialTick).getLocation();
            WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(player.level(), player, player.getEyePosition(partialTick), end, partialTick);
            return shield != null ? shield.point() : end;
        }

        @Override
        public void draw(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b,
                         float partialTick, float time) {
            beams(mc, player, style, pose, vc, a, b, partialTick, time, UNLEASHED_WIDTHS, UNLEASHED_ALPHAS, true);
        }
    };

    /** Where his beams leave: just in front of the visor, where the body is drawn (it follows the lean of his stances). */
    public static Vec3 eyes(AbstractClientPlayer player, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        return BodyLean.follow(player, eye, partialTick).add(look.scale(0.3));
    }

    private static boolean firstPerson(Minecraft mc, AbstractClientPlayer player) {
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /** One beam, or in first person two (one for each eye), from {@code a} to {@code b} (camera-relative). */
    private static void beams(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b,
                              float partialTick, float time, float[] widths, float[] alphas, boolean giant) {
        if (!firstPerson(mc, player)) {
            beam(pose, vc, a, b, style, time, widths, alphas, 1.0F, giant, player.getId());
            return;
        }
        Vec3 look = player.getViewVector(partialTick);
        Vec3 right = look.cross(UP);
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        // The camera is at the eye: the two beams start a little ahead of it, below it and to each side.
        Vec3 eye = a.subtract(look.scale(0.3));
        float scale = giant ? 0.4F : 0.45F;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 from = eye.add(look.scale(0.6)).add(right.scale(0.06 * side)).add(up.scale(-0.08));
            beam(pose, vc, from, b, style, time, widths, alphas, scale, giant, player.getId() * 31 + side);
        }
    }

    /**
     * A straight beam in layers (fringe, body, light, inner light, core), cut into short pieces whose widths swell with pulses
     * that run along it from the eyes; it starts at a quarter of its width and grows to the whole within two blocks (so it
     * never fills the view). The giant beam adds strands of heat that spiral round it.
     */
    private static void beam(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, BeamStyle style, float time, float[] widths, float[] alphas,
                             float scale, boolean giant, int seed) {
        Vec3 path = b.subtract(a);
        double length = path.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = path.scale(1.0 / length);
        int[] colors = {style.fringe(), style.body(), style.main(), style.inner(), style.core()};
        double step = giant ? 0.8 : 0.6;
        int pieces = Math.max(1, (int) Math.ceil(length / step));
        float flicker = 0.9F + 0.1F * Mth.sin(time * 3.1F + seed) * Mth.cos(time * 1.7F);
        for (int i = 0; i < pieces; i++) {
            double s0 = length * i / pieces;
            double s1 = length * (i + 1) / pieces;
            Vec3 p = a.add(dir.scale(s0));
            Vec3 q = a.add(dir.scale(s1));
            for (int layer = 0; layer < widths.length; layer++) {
                float w0 = width(widths[layer] * scale, s0, time, giant);
                float w1 = width(widths[layer] * scale, s1, time, giant);
                int c = EnergyGeometry.argb(colors[layer], alphas[layer] * (layer < 2 ? flicker : 1.0F));
                EnergyGeometry.ribbon(pose, vc, p, q, w0, w1, c, c);
            }
        }
        if (giant) {
            strands(pose, vc, a, dir, length, style, time, scale);
        }
        // Flares where it leaves (smaller the closer to the camera) and where it lands.
        float near = (float) Mth.clamp(a.length() / 2.5, 0.15, 1.0);
        flare(pose, vc, a, (giant ? 0.45F : 0.18F) * scale * near * flicker, style, dir);
        flare(pose, vc, b, (giant ? 1.1F : 0.38F) * scale * flicker, style, dir);
    }

    /** The width of a layer {@code along} blocks from the eyes: the taper at the start and the pulses that run outward. */
    private static float width(float full, double along, float time, boolean giant) {
        if (giant) {
            return CyclopsHeroMath.unleashedWidth(full, along, time);
        }
        float grow = (float) Math.min(1.0, 0.25 + along / 1.5 * 0.75);
        float pulse = 1.0F + 0.18F * Mth.sin((float) (along * 2.2 - time * 1.4));
        return full * grow * pulse;
    }

    /** Three strands of heat that spiral round the giant beam as it pours out. */
    private static void strands(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 dir, double length, BeamStyle style, float time, float scale) {
        Vec3 u = dir.cross(UP);
        u = u.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = dir.cross(u).normalize();
        int pieces = Math.max(2, (int) (length / 0.5));
        for (int k = 0; k < 3; k++) {
            double phase0 = k * Mth.TWO_PI / 3.0 + time * 0.35;
            Vec3 prev = null;
            for (int i = 0; i <= pieces; i++) {
                double s = length * i / pieces;
                double radius = scale * Math.min(0.55, 0.12 + s * 0.2) * (0.85 + 0.15 * Math.sin(time * 0.9 + s));
                double angle = phase0 + s * 0.9;
                Vec3 at = a.add(dir.scale(s)).add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
                if (prev != null) {
                    EnergyGeometry.ribbon(pose, vc, prev, at, 0.07F * scale, 0.07F * scale, EnergyGeometry.argb(style.main(), 0.5F),
                            EnergyGeometry.argb(style.main(), 0.5F));
                }
                prev = at;
            }
        }
    }

    /** A glow facing the camera where the beam leaves or lands, and a ring round it facing along the beam. */
    private static void flare(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, BeamStyle style, Vec3 dir) {
        Vec3 toCamera = center.scale(-1);
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.8F, radius * 0.45F, 14, EnergyGeometry.argb(style.fringe(), 0.25F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.5F, radius * 0.4F, 14, EnergyGeometry.argb(style.main(), 0.45F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.22F, radius * 0.3F, 12, EnergyGeometry.argb(style.core(), 0.9F));
        EnergyGeometry.ring(pose, vc, center, dir, radius * 0.9F, radius * 0.12F, 18, EnergyGeometry.argb(style.main(), 0.3F));
    }

    private CyclopsBeamFx() {
    }
}
