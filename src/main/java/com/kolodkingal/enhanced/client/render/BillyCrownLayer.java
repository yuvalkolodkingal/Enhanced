package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Billy's crown (Agatha All Along): M-shaped like his mother's, but softer and vine-like, in blue chrome that shades
 * from pale ice at the tips to deep navy at the band, glowing like a halo. It forms on his head (growing from the band
 * out to the tips) while he flies and for a moment after each spell. The "wiccan" look has it painted on the skin
 * already, and a helmet hides it.
 */
public class BillyCrownLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Blue chrome, from the tips down to the band. */
    private static final int[] CHROME = {0xDCEFFF, 0x8EBBEE, 0x3D6FC4, 0x1A3473};
    private static final float FORM_TICKS = 8.0F;
    private static final float FADE_TICKS = 12.0F;

    /** One stroke of the crown (one side; the other side is its mirror image), in head pixels. */
    private record Stroke(List<Vec3> points, float widthStart, float widthEnd, float delay) {
    }

    private static final List<Stroke> STROKES = new ArrayList<>();

    static {
        // The band dipping to a point in the middle of the forehead: the middle of the M.
        STROKES.add(new Stroke(bezier(v(4.1, -6.9, -3.4), v(2.2, -6.9, -4.6), v(0.9, -5.9, -4.65), v(0.0, -5.0, -4.65)), 0.9F, 0.45F, 0.0F));
        // Round the temples towards the back of the head.
        STROKES.add(new Stroke(bezier(v(4.1, -6.9, -3.4), v(4.5, -6.9, -2.0), v(4.5, -6.8, 0.0), v(4.4, -6.6, 1.8)), 0.6F, 0.2F, 0.0F));
        // The tall horns sweeping up, out and back: the outer strokes of the M.
        STROKES.add(new Stroke(bezier(v(3.6, -6.6, -3.8), v(5.4, -8.0, -3.6), v(5.3, -10.2, -2.8), v(4.3, -11.8, -1.9)), 1.1F, 0.15F, 0.15F));
        // From high on each horn down to the point on the forehead: the inner strokes of the M.
        STROKES.add(new Stroke(bezier(v(4.1, -10.4, -2.4), v(2.6, -9.0, -4.3), v(1.0, -6.6, -4.65), v(0.0, -5.2, -4.68)), 0.6F, 0.4F, 0.35F));
        // A vine curling off each horn.
        List<Vec3> curl = new ArrayList<>();
        for (int i = 0; i <= 12; i++) {
            double a = Math.toRadians(200 + i * 26);
            double r = 0.95 - i * 0.05;
            curl.add(v(5.35 + Math.cos(a) * r, -8.7 + Math.sin(a) * r, -3.35));
        }
        STROKES.add(new Stroke(curl, 0.4F, 0.15F, 0.5F));
    }

    /** How far the crown has formed (0-1), per player entity id, and the time it was last drawn. */
    private static final class Formation {
        float progress;
        float last = -1.0F;
    }

    private static final Map<Integer, Formation> FORMING = new HashMap<>();

    public BillyCrownLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        // (3.4) Billy's, or a custom hero's with one of Billy's abilities: CROWNED is set by Billy's code for either.
        if (!data.acts(Power.BILLY)) {
            FORMING.remove(player.getId());
            return;
        }
        boolean shown = data.is(Live.CROWNED) && !player.isInvisible() && !"wiccan".equals(Powers.variant(player).id())
                && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        Formation form = FORMING.computeIfAbsent(player.getId(), id -> new Formation());
        float dt = form.last < 0.0F ? 0.0F : Mth.clamp(ageInTicks - form.last, 0.0F, 4.0F);
        form.last = ageInTicks;
        form.progress = shown ? Math.min(1.0F, form.progress + dt / FORM_TICKS) : Math.max(0.0F, form.progress - dt / FADE_TICKS);
        if (!shown && form.progress <= 0.0F) {
            return;
        }
        // Growing in: each stroke starts after the ones it grows out of. Fading out: it just fades.
        float grown = shown ? form.progress : 1.0F;
        float alpha = shown ? 1.0F : form.progress;
        drawCrown(poseStack, buffer, getParentModel().head, ageInTicks, grown, alpha, -1);
    }

    /**
     * The crown on a head, {@code grown} (0-1) of it, faded to {@code alpha}: Billy's in its blue chrome ({@code tint} -1),
     * or (3.4) the crown of a custom hero's look, its chrome shaded toward his colour {@code tint} (0xRRGGBB).
     */
    public static void drawCrown(PoseStack poseStack, MultiBufferSource buffer, ModelPart head, float ageInTicks, float grown, float alpha,
                                 int tint) {
        float glint = (ageInTicks * 0.02F) % 1.6F - 0.3F;
        int halo = tint < 0 ? BillyAbilities.MAIN : tint;
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        Matrix4f m = poseStack.last().pose();
        float pulse = 0.85F + 0.15F * Mth.sin(ageInTicks * 0.15F);
        // Two passes, one buffer each: the chrome first, then the glow round it.
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = buffer.getBuffer(pass == 1 ? EnhancedRenderTypes.ENERGY_BEAM : EnhancedRenderTypes.ENERGY_VEIL);
            for (Stroke stroke : STROKES) {
                float length = Mth.clamp((grown * 1.6F - stroke.delay()) / (1.0F - stroke.delay()), 0.0F, 1.0F);
                if (length <= 0.0F) {
                    continue;
                }
                for (int side = -1; side <= 1; side += 2) {
                    List<Vec3> points = stroke.points();
                    if (side < 0) {
                        points = points.stream().map(p -> new Vec3(-p.x, p.y, p.z)).toList();
                    }
                    draw(m, vc, pass == 1, points, length, stroke.widthStart(), stroke.widthEnd(), alpha, pulse, glint, halo, tint);
                }
            }
        }
        poseStack.popPose();
    }

    /** One stroke: in chrome as two crossed ribbons (so it shows from every side), or the soft glow round it. */
    private static void draw(Matrix4f m, VertexConsumer vc, boolean halo, List<Vec3> points, float length,
                             float widthStart, float widthEnd, float alpha, float pulse, float glint, int haloRgb, int tint) {
        int last = Math.max(1, Math.round((points.size() - 1) * length));
        for (int i = 0; i < last; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            float ta = i / (float) (points.size() - 1);
            float tb = (i + 1) / (float) (points.size() - 1);
            float wa = Mth.lerp(ta, widthStart, widthEnd);
            float wb = Mth.lerp(tb, widthStart, widthEnd);
            Vec3 dir = b.subtract(a);
            Vec3 across = dir.cross(new Vec3(0, 0, 1));
            across = across.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : across.normalize();
            if (halo) {
                int glow = EnergyGeometry.argb(haloRgb, 0.22F * alpha * pulse);
                quad(m, vc, a, b, across, wa * 2.6F, wb * 2.6F, glow, glow);
                continue;
            }
            Vec3 depth = dir.cross(across).normalize();
            int ca = chrome(a, glint, alpha, tint);
            int cb = chrome(b, glint, alpha, tint);
            quad(m, vc, a, b, across, wa, wb, ca, cb);
            quad(m, vc, a, b, depth, wa * 0.8F, wb * 0.8F, ca, cb);
        }
    }

    /** The chrome colour at a point: by height, with a slow glint running up the crown. */
    private static int chrome(Vec3 p, float glint, float alpha, int tint) {
        float t = Mth.clamp((float) ((-p.y - 4.8) / 7.0), 0.0F, 1.0F);
        float f = (1.0F - t) * (CHROME.length - 1);
        int i = Math.min(CHROME.length - 2, (int) f);
        int rgb = lerpColor(CHROME[i], CHROME[i + 1], f - i);
        float shine = Math.max(0.0F, 1.0F - Math.abs(t - glint) * 6.0F);
        if (tint >= 0) {
            // The same light and shade, in the custom hero's colour.
            rgb = lerpColor(rgb, lerpColor(tint, rgb, 0.25F), 0.75F);
        }
        rgb = lerpColor(rgb, 0xFFFFFF, shine * 0.6F);
        return EnergyGeometry.argb(rgb, 0.95F * alpha);
    }

    private static int lerpColor(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }

    private static void quad(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 offset, float wa, float wb, int ca, int cb) {
        Vec3 oa = offset.scale(wa * 0.5);
        Vec3 ob = offset.scale(wb * 0.5);
        vertex(m, vc, a.add(oa), ca);
        vertex(m, vc, a.subtract(oa), ca);
        vertex(m, vc, b.subtract(ob), cb);
        vertex(m, vc, b.add(ob), cb);
    }

    private static void vertex(Matrix4f m, VertexConsumer vc, Vec3 p, int argb) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    /** A cubic Bezier curve through 13 points. */
    private static List<Vec3> bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i <= 12; i++) {
            double t = i / 12.0;
            double u = 1 - t;
            points.add(p0.scale(u * u * u).add(p1.scale(3 * u * u * t)).add(p2.scale(3 * u * t * t)).add(p3.scale(t * t * t)));
        }
        return points;
    }

    /** Forget every crown (leaving the world). */
    public static void clear() {
        FORMING.clear();
    }
}
