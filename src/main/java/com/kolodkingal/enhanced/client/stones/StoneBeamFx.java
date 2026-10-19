package com.kolodkingal.enhanced.client.stones;

import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.HandTracker;
import com.kolodkingal.enhanced.client.render.WorldEffectsRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * How the stones' beams are drawn (3.6), as {@link BeamRenderer.Drawer}s of the shared beam system: straight from the
 * hand that holds the stone, never the jagged bolt of the magic beams. The Mind Stone's is Vision's: a pale gold beam
 * with a white-gold core and soft pulses running out along it. The Power Stone's is half as wide again, violet, with
 * a pale lilac core, coiling strands of energy round it and a swelling flare where it lands (where it bursts). Every
 * width moves with the frame's time, so nothing jumps between frames.
 */
public final class StoneBeamFx {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** The widths (blocks) of the fringe, body, light, inner light and core, and their opacities. */
    private static final float[] MIND_WIDTHS = {0.30F, 0.19F, 0.11F, 0.05F, 0.02F};
    private static final float[] MIND_ALPHAS = {0.2F, 0.45F, 0.7F, 0.6F, 0.8F};
    private static final float[] POWER_WIDTHS = {0.48F, 0.30F, 0.17F, 0.08F, 0.03F};
    private static final float[] POWER_ALPHAS = {0.24F, 0.5F, 0.7F, 0.6F, 0.85F};

    public static final BeamRenderer.Drawer MIND = drawer(false);
    public static final BeamRenderer.Drawer POWER = drawer(true);

    private static BeamRenderer.Drawer drawer(boolean power) {
        return new BeamRenderer.Drawer() {
            @Override
            public Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick) {
                HumanoidArm arm = StoneArmLayer.stoneArm(player);
                if (arm != player.getMainArm()) {
                    Vec3 tracked = HandTracker.hand(player, arm, partialTick);
                    if (tracked != null) {
                        return tracked;
                    }
                }
                return WorldEffectsRenderer.handPosition(mc, player, partialTick);
            }

            @Override
            public Vec3 end(AbstractClientPlayer player, BeamStyle style, float range, Vec3 crosshair,
                            float partialTick) {
                return crosshair;
            }

            @Override
            public void draw(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose,
                             VertexConsumer vc, Vec3 a, Vec3 b, float partialTick, float time) {
                beam(pose, vc, a, b, style, time, power, player.getId());
            }
        };
    }

    /** A straight beam in layers, cut into short pieces whose widths swell with pulses running out from the hand. */
    private static void beam(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, BeamStyle style, float time,
                             boolean power, int seed) {
        Vec3 path = b.subtract(a);
        double length = path.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = path.scale(1.0 / length);
        float[] widths = power ? POWER_WIDTHS : MIND_WIDTHS;
        float[] alphas = power ? POWER_ALPHAS : MIND_ALPHAS;
        int[] colors = {style.fringe(), style.body(), style.main(), style.inner(), style.core()};
        int pieces = Math.max(1, (int) Math.ceil(length / 0.6));
        float flicker = 0.9F + 0.1F * Mth.sin(time * 3.1F + seed) * Mth.cos(time * 1.7F);
        for (int i = 0; i < pieces; i++) {
            double s0 = length * i / pieces;
            double s1 = length * (i + 1) / pieces;
            Vec3 p = a.add(dir.scale(s0));
            Vec3 q = a.add(dir.scale(s1));
            for (int layer = 0; layer < widths.length; layer++) {
                float w0 = width(widths[layer], s0, time, power);
                float w1 = width(widths[layer], s1, time, power);
                int c = EnergyGeometry.argb(colors[layer], alphas[layer] * (layer < 2 ? flicker : 1.0F));
                EnergyGeometry.ribbon(pose, vc, p, q, w0, w1, c, c);
            }
        }
        if (power) {
            strands(pose, vc, a, dir, length, style, time);
        }
        float near = (float) Mth.clamp(a.length() / 2.5, 0.15, 1.0);
        flare(pose, vc, a, (power ? 0.28F : 0.18F) * near * flicker, style, dir);
        float land = power ? 0.6F + 0.25F * Mth.sin(time * 0.6F) : 0.36F;
        flare(pose, vc, b, land * flicker, style, dir);
    }

    /** The taper at the start, and the pulses that run outward (faster and harder for the Power Stone). */
    private static float width(float full, double along, float time, boolean power) {
        float grow = (float) Math.min(1.0, 0.25 + along / 1.5 * 0.75);
        float pulse = power ? 1.0F + 0.25F * Mth.sin((float) (along * 3.0 - time * 2.0))
                : 1.0F + 0.15F * Mth.sin((float) (along * 2.0 - time * 1.2));
        return full * grow * pulse;
    }

    /** Two strands of violet that coil round the Power Stone's beam. */
    private static void strands(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 dir, double length, BeamStyle style,
                                float time) {
        Vec3 u = dir.cross(UP);
        u = u.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = dir.cross(u).normalize();
        int pieces = Math.max(2, (int) (length / 0.4));
        for (int k = 0; k < 2; k++) {
            double phase0 = k * Math.PI + time * 0.5;
            Vec3 prev = null;
            for (int i = 0; i <= pieces; i++) {
                double s = length * i / pieces;
                double radius = Math.min(0.32, 0.06 + s * 0.12);
                double angle = phase0 + s * 1.4;
                Vec3 at = a.add(dir.scale(s)).add(u.scale(Math.cos(angle) * radius))
                        .add(v.scale(Math.sin(angle) * radius));
                if (prev != null) {
                    int c = EnergyGeometry.argb(style.inner(), 0.45F);
                    EnergyGeometry.ribbon(pose, vc, prev, at, 0.05F, 0.05F, c, c);
                }
                prev = at;
            }
        }
    }

    /** A glow facing the camera where the beam leaves or lands, and a ring round it facing along the beam. */
    private static void flare(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, BeamStyle style, Vec3 dir) {
        Vec3 toCamera = center.scale(-1);
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.8F, radius * 0.45F, 14,
                EnergyGeometry.argb(style.fringe(), 0.25F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.5F, radius * 0.4F, 14,
                EnergyGeometry.argb(style.main(), 0.45F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.22F, radius * 0.3F, 12,
                EnergyGeometry.argb(style.core(), 0.9F));
        EnergyGeometry.ring(pose, vc, center, dir, radius * 0.9F, radius * 0.12F, 18,
                EnergyGeometry.argb(style.main(), 0.3F));
    }

    private StoneBeamFx() {
    }
}
