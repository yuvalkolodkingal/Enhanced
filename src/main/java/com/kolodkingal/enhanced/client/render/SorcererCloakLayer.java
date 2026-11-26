package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
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
 * The Cloak of Levitation as the Scarlet Witch wears it as the Sorceress Supreme (Russell Dauterman's design for
 * Sorcerer Supreme, 2025): folded and attached at her waist, not on her shoulders. A cloth of its own on her body, like
 * Doctor Strange's ({@link CloakLayer}, whose sway it shares) and Billy the Demiurge's ({@link DemiurgeCloakLayer}, whose
 * growing it follows), never the vanilla cape. Three pieces, all round her waist:
 * <ul>
 *   <li>the train: scarlet with a wide gold border, hung from one hip round her back to the other and open in front, so
 *   her legs show; long behind her, where it sweeps out and glides just over the ground;</li>
 *   <li>the fold: the cloak's top turned over at the waist, a short flounce with a gold edge;</li>
 *   <li>the collar: its two points, standing out and back from her hips like blades.</li>
 * </ul>
 * While she flies or uses a power ({@code SorcererFx.out}, eased, so it grows and falls back) the train is a third longer
 * and fans out, its two front edges lifting like wings, long slow waves running down it; the fold lifts and the collar's
 * points grow. Its texture is {@code scarlet_witch_sorcerer_cloak.png} (made by {@code tools/gen_sorcerer_supreme.py}):
 * the train's outside x 0-31, y 0-39 and its lining x 32-63; the fold's outside x 0-31, y 40-49 and its inside x 32-63;
 * one collar point's outside x 0-15, y 50-63 (its tip at the top) and its inside x 16-31. Across, the train and the fold
 * run once round her: from the edge at her right hip, behind her, to the edge at her left hip.
 */
public class SorcererCloakLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/suit/scarlet_witch_sorcerer_cloak.png");
    /** The train's grid: columns round her, rows down. */
    private static final int NX = 16;
    private static final int NY = 14;
    /** How far round her waist the cloth reaches on each side of straight behind her: its front is open. */
    private static final float ROUND = (float) Math.toRadians(118);
    /** The waist it hangs from (body pixels: the body is x -4 to 4, y 0 to 12, z -2 to 2): an oval a little off her hips. */
    private static final float WAIST_X = 4.5F;
    private static final float WAIST_Z = 2.6F;
    private static final float WAIST_Y = 9.0F;
    /** Standing, the train never goes lower than this (her feet are at 24); in the air it hangs as long as it is. */
    private static final float GROUND_Y = 23.3F;
    private static final float AIR_Y = 40.0F;
    private static final int FOLD_ROWS = 3;
    /** A collar point: rows from the waist to its tip, and where on the waist it stands (from straight behind her). */
    private static final int POINT_ROWS = 5;
    private static final float POINT_AT = (float) Math.toRadians(75);

    public SorcererCloakLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!SorcererFx.worn(player) || player.isInvisible() || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }
        PowerData data = Powers.get(player);
        float out = SorcererFx.out(player, partialTicks);
        float floor = Mth.lerp(SorcererFx.ground(player, partialTicks), AIR_Y, GROUND_Y);
        CloakLayer.Sway sway = CloakLayer.sway(player, data, partialTicks, ageInTicks + player.getId() * 3.7F);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        draw(poseStack, buffers, light, overlay, getParentModel().body, sway, out, floor);
    }

    /**
     * Draws the cloak on {@code body} (the pose stack at the model's root): {@code out} 0 at rest to 1 spread, {@code
     * floor} the lowest the train may hang (body pixels down from the neck).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, ModelPart body,
                            CloakLayer.Sway sway, float out, float floor) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        body.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        sheet(vc, pose, train(sway, out, floor), 0.0F, 0.0F, 32.0F, 40.0F, 32.0F, false, light, overlay);
        sheet(vc, pose, fold(sway, out), 0.0F, 40.0F, 32.0F, 10.0F, 32.0F, false, light, overlay);
        sheet(vc, pose, point(-1, sway, out), 0.0F, 50.0F, 16.0F, 14.0F, 16.0F, true, light, overlay);
        sheet(vc, pose, point(1, sway, out), 0.0F, 50.0F, 16.0F, 14.0F, 16.0F, true, light, overlay);
        poseStack.popPose();
    }

    /**
     * The train's grid points (body pixels: x across, y down, z behind). Each column hangs from its place on the waist
     * and is walked down row by row, every step turned out from her by an angle: a little at her sides, far more behind
     * her toward the hem (the train sweeps out), more with her speed and with {@code out} (most at the two front edges,
     * which lift like wings), and the waves. A step that would go under {@code floor} glides out along it instead.
     */
    static Vec3[][] train(CloakLayer.Sway sway, float out, float floor) {
        Vec3[][] grid = new Vec3[NY + 1][NX + 1];
        float wave = sway.calm() ? 0.0F : 1.0F;
        // (her own speed streams the cloth back behind her; moving or spread, the hem turns toward her back)
        float stream = Mth.clamp((sway.billow() - 6.0F) / 60.0F, 0.0F, 1.0F);
        float sweep = Math.max(out, 0.8F * stream);
        for (int i = 0; i <= NX; i++) {
            float f = (float) i / NX * 2.0F - 1.0F;
            float phi = f * ROUND;
            float edge = Math.abs(f);
            float behind = Math.max(0.0F, Mth.cos(phi));
            float length = (14.0F + 8.0F * behind * behind) * Mth.lerp(out, 1.0F, 1.3F);
            float step = length / NY;
            float x = WAIST_X * Mth.sin(phi);
            float y = WAIST_Y;
            float z = WAIST_Z * Mth.cos(phi);
            for (int j = 0; j <= NY; j++) {
                float v = (float) j / NY;
                grid[j][i] = new Vec3(x + sway.side() * 0.3F * v * v, y, z);
                if (j == NY) {
                    break;
                }
                float down = 0.3F + 0.7F * v;
                float turn = phi * (1.0F - 0.35F * sweep * down);
                float angle = 8.0F * (0.35F + 0.65F * v)
                        + 58.0F * behind * behind * v * v * v                                  // behind her: the train
                        + (6.0F + (sway.billow() - 6.0F) * (0.35F + 0.65F * behind)) * down
                        + sway.catchUp() * v * v * 0.7F
                        + out * (16.0F + 26.0F * edge) * v                                     // the edges lift like wings
                        + wave * (2.5F + 7.0F * out) * Mth.sin(sway.time() * 0.11F - v * 4.5F + phi * 1.9F)
                        + wave * (1.5F + 4.0F * out) * Mth.sin(sway.time() * 0.07F + phi * 2.7F) * v;
                angle = Mth.clamp(angle, 0.0F, 150.0F) * Mth.DEG_TO_RAD;
                float dy = step * Mth.cos(angle);
                if (y + dy > floor) {
                    dy = Math.max(0.0F, floor - y);
                }
                float dh = Mth.sqrt(Math.max(0.0F, step * step - dy * dy));
                float hx = Mth.sin(turn);
                float hz = Mth.cos(turn) + stream * (1.0F - behind) * down * 1.2F;
                float hl = Mth.sqrt(hx * hx + hz * hz);
                if (hl < 1.0E-4F) {
                    hx = 0.0F;
                    hz = 1.0F;
                    hl = 1.0F;
                }
                y += dy;
                x += dh * hx / hl;
                z += dh * hz / hl;
            }
        }
        return grid;
    }

    /** The fold: the cloak's top turned over at the waist, a short flounce standing off the train; it lifts with {@code out}. */
    static Vec3[][] fold(CloakLayer.Sway sway, float out) {
        Vec3[][] grid = new Vec3[FOLD_ROWS + 1][NX + 1];
        float wave = sway.calm() ? 0.0F : 1.0F;
        for (int i = 0; i <= NX; i++) {
            float phi = ((float) i / NX * 2.0F - 1.0F) * ROUND;
            float behind = Math.max(0.0F, Mth.cos(phi));
            float step = (4.4F + 1.6F * behind) / FOLD_ROWS;
            float x = (WAIST_X + 0.45F) * Mth.sin(phi);
            float y = WAIST_Y - 0.5F;
            float z = (WAIST_Z + 0.45F) * Mth.cos(phi);
            for (int j = 0; j <= FOLD_ROWS; j++) {
                float v = (float) j / FOLD_ROWS;
                grid[j][i] = new Vec3(x, y, z);
                if (j == FOLD_ROWS) {
                    break;
                }
                float angle = Mth.lerp(out, 20.0F, 58.0F) + 10.0F * v + (sway.billow() - 6.0F) * 0.4F * behind
                        + wave * (3.0F + 5.0F * out) * Mth.sin(sway.time() * 0.13F + phi * 2.3F - v * 2.0F);
                angle = Mth.clamp(angle, 0.0F, 120.0F) * Mth.DEG_TO_RAD;
                y += step * Mth.cos(angle);
                x += step * Mth.sin(angle) * Mth.sin(phi);
                z += step * Mth.sin(angle) * Mth.cos(phi);
            }
        }
        return grid;
    }

    /**
     * One point of the cloak's collar, at her right hip ({@code s} -1) or her left (1): a blade of cloth from the waist
     * out, back and up, curving upward, as wide as three pixels at the waist and sharp at its tip. Two columns (its two
     * edges), rows from the waist to the tip.
     */
    static Vec3[][] point(int s, CloakLayer.Sway sway, float out) {
        Vec3[][] grid = new Vec3[POINT_ROWS + 1][2];
        float flutter = sway.calm() ? 0.0F : 0.3F * Mth.sin(sway.time() * 0.09F + s);
        float reach = Mth.lerp(out, 4.2F, 6.2F);
        float rise = Mth.lerp(out, 6.0F, 8.4F);
        float bx = (WAIST_X + 0.3F) * Mth.sin(POINT_AT) * s;
        float by = WAIST_Y + 0.2F;
        float bz = (WAIST_Z + 0.3F) * Mth.cos(POINT_AT);
        float dx = 0.78F * s;
        float dz = 0.62F;
        for (int r = 0; r <= POINT_ROWS; r++) {
            float h = (float) r / POINT_ROWS;
            float along = reach * h + flutter * h;
            float cx = bx + dx * along;
            float cy = by - rise * h * Mth.sqrt(h);
            float cz = bz + dz * along;
            // across the blade: square to its curve, in the upright plane it lies in (at the waist: straight up and down)
            float ta = rise * 1.5F * Mth.sqrt(h);
            float tl = Mth.sqrt(ta * ta + reach * reach);
            float na = ta / tl;
            float nb = reach / tl;
            float half = 1.6F * (float) Math.pow(1.0F - h, 0.85);
            for (int c = 0; c < 2; c++) {
                float k = (c * 2 - 1) * half;
                grid[r][c] = new Vec3(cx + k * na * dx, cy + k * nb, cz + k * na * dz);
            }
        }
        return grid;
    }

    /**
     * Draws a grid of cloth: its outside from the texture's {@code u0, v0} ({@code uw} by {@code vh} pixels of the 64),
     * its inside a hair behind it from {@code inner} pixels further across. {@code tipUp}: the grid's last row is the
     * picture's top row.
     */
    private static void sheet(VertexConsumer vc, PoseStack.Pose pose, Vec3[][] grid, float u0, float v0, float uw, float vh,
                              float inner, boolean tipUp, int light, int overlay) {
        int rows = grid.length - 1;
        int cols = grid[0].length - 1;
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                Vec3 a = grid[j][i];
                Vec3 b = grid[j][i + 1];
                Vec3 c = grid[j + 1][i + 1];
                Vec3 d = grid[j + 1][i];
                Vector3f n = normal(a, b, d);
                float ua = (u0 + (float) i / cols * uw) / 64.0F;
                float ub = (u0 + (float) (i + 1) / cols * uw) / 64.0F;
                float va = (float) j / rows * vh;
                float vb = (float) (j + 1) / rows * vh;
                va = (tipUp ? v0 + vh - va : v0 + va) / 64.0F;
                vb = (tipUp ? v0 + vh - vb : v0 + vb) / 64.0F;
                quad(vc, pose, a, b, c, d, ua, ub, va, vb, n, light, overlay);
                Vec3 in = new Vec3(n.x(), n.y(), n.z()).scale(-0.12);
                float shift = inner / 64.0F;
                quad(vc, pose, a.add(in), d.add(in), c.add(in), b.add(in), shift + ua, shift + ub, va, vb,
                        new Vector3f(n).negate(), light, overlay);
            }
        }
    }

    private static Vector3f normal(Vec3 a, Vec3 b, Vec3 d) {
        Vec3 n = b.subtract(a).cross(d.subtract(a));
        return n.lengthSqr() < 1.0E-8 ? new Vector3f(0, 0, 1) : n.normalize().toVector3f();
    }

    /** A quad a, b, c, d with u from {@code u0} at a and d to {@code u1} at b and c, and v from {@code v0} at a and b. */
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
