package com.kolodkingal.enhanced.client.love;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.BodyLean;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * How Love's Eye Laser is drawn (3.8), as a {@link BeamRenderer.Drawer} of the shared beam system ({@code love_eyes}):
 * two thin, straight violet beams, one from each eye, meeting where she looks and running on together through every
 * creature to the block that stops them (as the server's beam does). A lilac core, a violet body, a deep purple fringe,
 * pulses running out from her eyes, a small flare at each eye and a bigger one where they land. In first person the two
 * start just below and to either side of the view. Everything moves with the frame's time, partial ticks included.
 */
public final class LoveEyeBeamFx {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    /** The widths (blocks) of its fringe, body, light and core, and their alphas. */
    private static final float[] WIDTHS = {0.16F, 0.09F, 0.045F, 0.018F};
    private static final float[] ALPHAS = {0.25F, 0.55F, 0.75F, 0.85F};
    /** Half the distance between her eyes (blocks), and how far in front of her face the beams start. */
    private static final double EYE_HALF = 0.07;
    private static final double AHEAD = 0.26;

    public static final BeamRenderer.Drawer DRAWER = new BeamRenderer.Drawer() {
        @Override
        public Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick) {
            Vec3 eye = player.getEyePosition(partialTick);
            return BodyLean.follow(player, eye, partialTick).add(player.getViewVector(partialTick).scale(AHEAD));
        }

        @Override
        public Vec3 end(AbstractClientPlayer player, BeamStyle style, float range, Vec3 crosshair, float partialTick) {
            Vec3 end = Raycasts.block(player, range, partialTick).getLocation();
            WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(player.level(), player,
                    player.getEyePosition(partialTick), end, partialTick);
            return shield != null ? shield.point() : end;
        }

        @Override
        public void draw(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose, VertexConsumer vc,
                         Vec3 a, Vec3 b, float partialTick, float time) {
            Vec3 look = player.getViewVector(partialTick);
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
            for (int side = -1; side <= 1; side += 2) {
                Vec3 from = firstPerson
                        ? a.add(look.scale(0.3)).add(right.scale(0.07 * side)).add(up.scale(-0.09))
                        : a.add(right.scale(EYE_HALF * side));
                beam(pose, vc, from, b, style, time, player.getId() * 31 + side, firstPerson ? 0.6F : 1.0F);
            }
            float flicker = 0.9F + 0.1F * Mth.sin(time * 2.7F + player.getId());
            flare(pose, vc, b, 0.32F * flicker, style, b.subtract(a));
        }
    };

    /** One straight beam in layers, cut into pieces whose widths swell with pulses running out from the eye. */
    private static void beam(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, BeamStyle style, float time, int seed,
                             float scale) {
        Vec3 path = b.subtract(a);
        double length = path.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = path.scale(1.0 / length);
        int[] colors = {style.fringe(), style.body(), style.main(), style.core()};
        int pieces = Math.max(1, (int) Math.ceil(length / 0.5));
        float flicker = 0.88F + 0.12F * Mth.sin(time * 3.3F + seed) * Mth.cos(time * 1.9F);
        for (int i = 0; i < pieces; i++) {
            double s0 = length * i / pieces;
            double s1 = length * (i + 1) / pieces;
            Vec3 p = a.add(dir.scale(s0));
            Vec3 q = a.add(dir.scale(s1));
            for (int layer = 0; layer < WIDTHS.length; layer++) {
                float w0 = width(WIDTHS[layer] * scale, s0, time);
                float w1 = width(WIDTHS[layer] * scale, s1, time);
                int c = EnergyGeometry.argb(colors[layer], ALPHAS[layer] * (layer < 2 ? flicker : 1.0F));
                EnergyGeometry.ribbon(pose, vc, p, q, w0, w1, c, c);
            }
        }
        float near = (float) Mth.clamp(a.length() / 2.5, 0.15, 1.0);
        flare(pose, vc, a, 0.1F * scale * near * flicker, style, dir);
    }

    private static float width(float full, double along, float time) {
        float grow = (float) Math.min(1.0, 0.3 + along / 1.2 * 0.7);
        float pulse = 1.0F + 0.22F * Mth.sin((float) (along * 2.6 - time * 1.6));
        return full * grow * pulse;
    }

    private static void flare(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, BeamStyle style, Vec3 dir) {
        Vec3 toCamera = center.scale(-1);
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.8F, radius * 0.45F, 14,
                EnergyGeometry.argb(style.fringe(), 0.25F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.5F, radius * 0.4F, 14,
                EnergyGeometry.argb(style.main(), 0.5F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.22F, radius * 0.3F, 12,
                EnergyGeometry.argb(style.core(), 0.9F));
        if (dir.lengthSqr() > 1.0E-6) {
            EnergyGeometry.ring(pose, vc, center, dir, radius * 0.9F, radius * 0.12F, 18,
                    EnergyGeometry.argb(style.main(), 0.3F));
        }
    }

    private LoveEyeBeamFx() {
    }
}
