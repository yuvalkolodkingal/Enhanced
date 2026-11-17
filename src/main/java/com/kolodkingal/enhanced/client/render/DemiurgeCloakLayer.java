package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.magic.DemiurgeFx;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Demiurge's red cloak (3.17, Ultimate Mode: Billy only), a cloth of its own on his body like Doctor Strange's
 * ({@link CloakLayer}, whose sway it shares), never the vanilla cape and never light:
 * <ul>
 *   <li>at rest, a red scarf wound round his neck and a red cape hanging down his back;</li>
 *   <li>while he flies or uses a power ({@code DemiurgeFx.out}, eased, so it grows and falls back), the great cloak: the
 *   cape grows long and wide and flares out behind and to both sides in a fan, its edges lifting like wings, its hem torn
 *   into long tongues that wave; and the hood comes up over his head from the scarf, the face in its shadow.</li>
 * </ul>
 * Its texture is {@code billy_demiurge_cloak.png}: the outside x 0-31, y 0-47 (the tongues of the hem are cut out of its
 * lower third), the lining x 32-63 (dark, stars caught in it), the scarf and hood's outside x 0-31, y 48-63, their inside
 * x 32-63. Nobody but the Demiurge wears it: with Ultimate Mode off it draws nothing.
 */
public class DemiurgeCloakLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/suit/billy_demiurge_cloak.png");
    /** The cape's grid: columns across, rows down. */
    private static final int NX = 12;
    private static final int NY = 14;
    /** The hood's grid: round the head, and up over it. */
    private static final int HOOD_ROUND = 14;
    private static final int HOOD_UP = 7;
    private static final float HOOD_ARC = (float) Math.toRadians(128);
    /** The scarf: segments round the neck. */
    private static final int SCARF_SEGS = 14;

    public DemiurgeCloakLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!Magic.isDemiurge(player) || player.isInvisible() || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }
        PowerData data = Powers.get(player);
        float out = DemiurgeFx.out(player);
        CloakLayer.Sway sway = CloakLayer.sway(player, data, partialTicks, ageInTicks + player.getId() * 3.7F);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        draw(poseStack, buffers, light, overlay, getParentModel().body, getParentModel().head, sway, out);
    }

    /** Draws the cape and scarf on {@code body} and the hood on {@code head} (the pose stack at the model's root). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, ModelPart body,
                            ModelPart head, CloakLayer.Sway sway, float out) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        body.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        Vec3[][] grid = cloth(sway, out);
        // (at rest only the upper part of the cloth's picture is used: its hem is straight; grown, all of it, the tongues too)
        float vSpan = Mth.lerp(out, 0.5F, 1.0F) * 48.0F / 64.0F;
        for (int j = 0; j < NY; j++) {
            for (int i = 0; i < NX; i++) {
                Vec3 a = grid[j][i];
                Vec3 b = grid[j][i + 1];
                Vec3 c = grid[j + 1][i + 1];
                Vec3 d = grid[j + 1][i];
                Vector3f n = normal(a, b, d);
                float u0 = (float) i / NX * 0.5F;
                float u1 = (float) (i + 1) / NX * 0.5F;
                float v0 = (float) j / NY * vSpan;
                float v1 = (float) (j + 1) / NY * vSpan;
                quad(vc, pose, a, b, c, d, u0, u1, v0, v1, n, light, overlay);
                Vec3 in = new Vec3(n.x(), n.y(), n.z()).scale(-0.12);
                quad(vc, pose, a.add(in), d.add(in), c.add(in), b.add(in), 0.5F + u0, 0.5F + u1, v0, v1,
                        new Vector3f(n).negate(), light, overlay);
            }
        }
        scarf(vc, pose, sway, light, overlay);
        poseStack.popPose();
        if (out > 0.03F) {
            poseStack.pushPose();
            head.translateAndRotate(poseStack);
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            hood(vc, poseStack.last(), sway, out, light, overlay);
            poseStack.popPose();
        }
    }

    /**
     * The cape's grid points (body pixels: x across, y down, z behind). At rest it hangs like a cape, a little out. Grown
     * ({@code out} toward 1) it is half as long again and three times as wide at the hem, the rows fan outward, the edges
     * lift (the further from the middle, the higher), and long slow waves run down it.
     */
    static Vec3[][] cloth(CloakLayer.Sway sway, float out) {
        Vec3[][] grid = new Vec3[NY + 1][NX + 1];
        float wave = sway.calm() ? 0.0F : 1.0F;
        float length = Mth.lerp(out, 19.0F, 38.0F);
        float topHalf = Mth.lerp(out, 5.2F, 6.6F);
        float hemHalf = Mth.lerp(out, 6.8F, 25.0F);
        float billow = Math.max(sway.billow(), Mth.lerp(out, 6.0F, 34.0F));
        for (int i = 0; i <= NX; i++) {
            float fx = (float) i / NX * 2.0F - 1.0F;
            float y = 0.4F;
            float z = 2.4F;
            for (int j = 0; j <= NY; j++) {
                float v = (float) j / NY;
                // (it widens slowly at first, then flares)
                float half = Mth.lerp(v * v * (3.0F - 2.0F * v), topHalf, hemHalf + billow * 0.02F);
                float x = fx * half + sway.side() * 0.03F * v * 16.0F;
                float wrap = Math.max(0.0F, 1.0F - v * 4.0F) * 3.0F * fx * fx;
                grid[j][i] = new Vec3(x, y, z - wrap);
                if (j == NY) {
                    break;
                }
                float angle = billow * (0.3F + 0.7F * v) + sway.catchUp() * v * v
                        + out * 46.0F * Math.abs(fx) * v                                   // the edges lift like wings
                        + wave * (4.0F + 9.0F * out) * Mth.sin(sway.time() * 0.11F - v * 4.5F + fx * 2.1F)
                        + wave * (2.0F + 5.0F * out) * Mth.sin(sway.time() * 0.07F + fx * 3.3F) * v;
                angle = Mth.clamp(angle, 0.0F, 150.0F) * Mth.DEG_TO_RAD;
                float step = length / NY;
                y += step * Mth.cos(angle);
                z += step * Mth.sin(angle);
            }
        }
        return grid;
    }

    /** The scarf: a thick band wound all the way round his neck, a little wider at the front where it is knotted. */
    private static void scarf(VertexConsumer vc, PoseStack.Pose pose, CloakLayer.Sway sway, int light, int overlay) {
        float breathe = sway.calm() ? 0.0F : 0.12F * Mth.sin(sway.time() * 0.09F);
        for (int s = 0; s < SCARF_SEGS; s++) {
            float p0 = Mth.TWO_PI * s / SCARF_SEGS;
            float p1 = Mth.TWO_PI * (s + 1) / SCARF_SEGS;
            for (int r = 0; r < 2; r++) {
                Vec3 a = scarfPoint(p0, r * 0.5F, breathe);
                Vec3 b = scarfPoint(p1, r * 0.5F, breathe);
                Vec3 c = scarfPoint(p1, (r + 1) * 0.5F, breathe);
                Vec3 d = scarfPoint(p0, (r + 1) * 0.5F, breathe);
                Vector3f n = normal(a, b, d);
                float u0 = (float) s / SCARF_SEGS * 0.5F;
                float u1 = (float) (s + 1) / SCARF_SEGS * 0.5F;
                float v0 = (56.0F + 4.0F * r) / 64.0F;
                float v1 = (60.0F + 4.0F * r) / 64.0F;
                quad(vc, pose, a, b, c, d, u0, u1, v0, v1, n, light, overlay); // (one skin: its inside is never seen)
            }
        }
    }

    /** A point of the scarf at {@code phi} round the neck (0: straight behind) and {@code h} down it (0 its top, 1 its foot). */
    private static Vec3 scarfPoint(float phi, float h, float breathe) {
        float front = Math.max(0.0F, -Mth.cos(phi));               // 1 at the throat
        float bulge = Mth.sin(h * Mth.PI);                         // thickest in its middle
        // (it lies on his shoulders round the foot of the neck, under the chin: never over the face)
        float rx = 4.4F + 0.9F * bulge + breathe;
        float rz = 2.5F + 0.9F * bulge + 0.4F * front + breathe;
        float y = Mth.lerp(h, -0.9F, 2.3F + 2.0F * front);        // it hangs lower at the throat
        return new Vec3(rx * Mth.sin(phi), y, rz * Mth.cos(phi));
    }

    /**
     * The hood, on the head (head pixels: the head is x -4 to 4, y -8 to 0, z -4 to 4): a cowl round the back and sides
     * and over the top, open at the face, its rim standing a little forward of the brow so the face is in its shadow. It
     * comes up from the neck as {@code out} grows.
     */
    private static void hood(VertexConsumer vc, PoseStack.Pose pose, CloakLayer.Sway sway, float out, int light, int overlay) {
        float up = Mth.clamp(out * 1.25F, 0.0F, 1.0F);
        float flutter = sway.calm() ? 0.0F : 0.15F * Mth.sin(sway.time() * 0.1F);
        for (int s = 0; s < HOOD_ROUND; s++) {
            float p0 = -HOOD_ARC + 2.0F * HOOD_ARC * s / HOOD_ROUND;
            float p1 = -HOOD_ARC + 2.0F * HOOD_ARC * (s + 1) / HOOD_ROUND;
            for (int r = 0; r < HOOD_UP; r++) {
                float h0 = (float) r / HOOD_UP * up;
                float h1 = (float) (r + 1) / HOOD_UP * up;
                Vec3 a = hoodPoint(p0, h0, flutter);
                Vec3 b = hoodPoint(p1, h0, flutter);
                Vec3 c = hoodPoint(p1, h1, flutter);
                Vec3 d = hoodPoint(p0, h1, flutter);
                Vector3f n = normal(a, d, b); // (outward: away from the head)
                float u0 = (float) s / HOOD_ROUND * 0.5F;
                float u1 = (float) (s + 1) / HOOD_ROUND * 0.5F;
                float v0 = (56.0F - 8.0F * h0) / 64.0F;
                float v1 = (56.0F - 8.0F * h1) / 64.0F;
                quad(vc, pose, a, b, c, d, u0, u1, v0, v1, n, light, overlay);
                Vec3 in = new Vec3(n.x(), n.y(), n.z()).scale(-0.1);
                quad(vc, pose, a.add(in), d.add(in), c.add(in), b.add(in), 0.5F + u0, 0.5F + u1, v0, v1,
                        new Vector3f(n).negate(), light, overlay);
            }
        }
    }

    /**
     * A point of the hood at {@code phi} round the head (as a share of the hood's arc there; 0: straight behind) and
     * {@code h} up it (0 at the neck, 1 at the crown). Low down it is open in front, the width of the face; higher, its
     * two rims come round and meet over the brow, and the top stands a little forward, so the face is in its shadow.
     */
    private static Vec3 hoodPoint(float phi, float h, float flutter) {
        float elevation = Mth.lerp(h, -42.0F, 90.0F);
        float e = elevation * Mth.DEG_TO_RAD;
        float r = 5.3F + flutter * h;
        // (a boxy dome: the head it covers is a box)
        float ring = r * (float) Math.pow(Math.max(0.0F, Mth.cos(e)), 0.45);
        float closing = Mth.clamp((elevation - 15.0F) / 40.0F, 0.0F, 1.0F);
        float angle = phi / HOOD_ARC * Mth.lerp(closing, HOOD_ARC, Mth.PI);
        float forward = Math.max(0.0F, Mth.sin(e)) * 1.3F + Math.max(0.0F, Math.abs(phi) / HOOD_ARC - 0.7F) * 3.0F;
        return new Vec3(ring * Mth.sin(angle), -4.3F - r * Mth.sin(e), ring * Mth.cos(angle) + 0.4F - forward);
    }

    private static Vector3f normal(Vec3 a, Vec3 b, Vec3 d) {
        Vec3 n = b.subtract(a).cross(d.subtract(a));
        return n.lengthSqr() < 1.0E-8 ? new Vector3f(0, 0, 1) : n.normalize().toVector3f();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1,
                             float v0, float v1, Vector3f n, int light, int overlay) {
        vertex(vc, pose, a, u0, v0, n, light, overlay);
        vertex(vc, pose, b, u1, v0, n, light, overlay);
        vertex(vc, pose, c, u1, v1, n, light, overlay);
        vertex(vc, pose, d, u0, v1, n, light, overlay);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vector3f n, int light,
                               int overlay) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(overlay)
                .setLight(light).setNormal(pose, n.x(), n.y(), n.z());
    }
}
