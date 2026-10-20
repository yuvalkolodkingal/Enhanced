package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.beam.BeamStyle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * How the Unibeam is drawn (3.8, lane L3), as a {@link BeamRenderer.Drawer} of the shared beam system: a straight,
 * steady white-blue beam from the arc reactor in the middle of his chest (never the jagged bolt of the magic beams),
 * thick at the reactor's ring and pulsing along its length, with a ring of light round the reactor and a flare where
 * it lands. Lanes L4 (the Mark 85's chest laser, the Hulkbuster's heavy Unibeam) may draw theirs with {@link
 * #chestBeam} at their own widths. Everything moves with the frame's time (partial ticks included).
 */
public final class IronManBeamFx {
    /** The Unibeam's layers (blocks): fringe, body, light, inner light, core (0.35 across, PLAN 12.2). */
    static final float[] UNIBEAM_WIDTHS = {0.35F, 0.24F, 0.15F, 0.07F, 0.03F};
    static final float[] UNIBEAM_ALPHAS = {0.22F, 0.45F, 0.65F, 0.6F, 0.85F};

    /** The Unibeam: from the chest's reactor to what the crosshair is on. */
    public static final BeamRenderer.Drawer UNIBEAM = new BeamRenderer.Drawer() {
        @Override
        public Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick) {
            if (firstPerson(mc, player)) {
                // (In first person the reactor is just under the camera: the beam starts a little ahead of it, so
                // its near end does not fill the bottom of the screen.)
                Vec3 look = player.getViewVector(partialTick);
                return player.getEyePosition(partialTick).add(look.scale(0.9)).add(0, -0.35, 0);
            }
            return reactor(player, partialTick);
        }

        @Override
        public Vec3 end(AbstractClientPlayer player, BeamStyle style, float range, Vec3 crosshair, float partialTick) {
            return crosshair;
        }

        @Override
        public void draw(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose, VertexConsumer vc,
                         Vec3 a, Vec3 b, float partialTick, float time) {
            chestBeam(pose, vc, a, b, style, time, UNIBEAM_WIDTHS, UNIBEAM_ALPHAS,
                    firstPerson(mc, player) ? 0.6F : 1.0F);
        }
    };

    /** Whether the camera is this player's own eyes. */
    static boolean firstPerson(Minecraft mc, AbstractClientPlayer player) {
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /** Where the arc reactor is drawn: the middle of his chest, a little before it (it follows his lean). */
    public static Vec3 reactor(AbstractClientPlayer player, float partialTick) {
        Vec3 base = player.getPosition(partialTick);
        float yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 chest = base.add(0, player.getBbHeight() * 0.70, 0).add(forward.scale(0.22));
        return BodyLean.follow(player, chest, partialTick);
    }

    /** A straight beam in layers from {@code a} to {@code b} (camera-relative), pulses running out along it. */
    public static void chestBeam(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, BeamStyle style, float time,
                                 float[] widths, float[] alphas, float scale) {
        Vec3 path = b.subtract(a);
        double length = path.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = path.scale(1.0 / length);
        int[] colors = {style.fringe(), style.body(), style.main(), style.inner(), style.core()};
        int pieces = Math.max(1, (int) Math.ceil(length / 0.6));
        for (int i = 0; i < pieces; i++) {
            double s0 = length * i / pieces;
            double s1 = length * (i + 1) / pieces;
            Vec3 p = a.add(dir.scale(s0));
            Vec3 q = a.add(dir.scale(s1));
            for (int layer = 0; layer < widths.length; layer++) {
                float w0 = width(widths[layer] * scale, s0, time);
                float w1 = width(widths[layer] * scale, s1, time);
                int c = EnergyGeometry.argb(colors[layer], alphas[layer]);
                EnergyGeometry.ribbon(pose, vc, p, q, w0, w1, c, c);
            }
        }
        float pulse = 0.9F + 0.1F * Mth.sin(time * 2.3F);
        EnergyGeometry.ring(pose, vc, a, dir, 0.24F * scale * pulse, 0.08F * scale, 18,
                EnergyGeometry.argb(style.main(), 0.6F));
        EnergyGeometry.ring(pose, vc, a, a.scale(-1), 0.14F * scale, 0.2F * scale, 14,
                EnergyGeometry.argb(style.core(), 0.9F));
        EnergyGeometry.ring(pose, vc, b, b.scale(-1), 0.5F * scale * pulse, 0.3F * scale, 16,
                EnergyGeometry.argb(style.main(), 0.45F));
        EnergyGeometry.ring(pose, vc, b, b.scale(-1), 0.22F * scale, 0.25F * scale, 12,
                EnergyGeometry.argb(style.core(), 0.85F));
    }

    /** A layer's width {@code along} blocks from the chest: full at the reactor, with pulses running outward. */
    private static float width(float full, double along, float time) {
        float grow = (float) Math.min(1.0, 0.7 + along * 0.3);
        float pulse = 1.0F + 0.12F * Mth.sin((float) (along * 1.8 - time * 1.2));
        return full * grow * pulse;
    }

    private IronManBeamFx() {
    }
}
