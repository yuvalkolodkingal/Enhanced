package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.wolverine.ClawLayer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;

/**
 * The fins of Wolverine's classic cowl (3.7): two black blades swept up and back from his temples, flaring out at the
 * tips, over the cowl the skin draws on his head (the classic look only; a helmet hides them). Our own drawing of the
 * comics' shape: flat quads in the pixels of the head, a dark grey rim.
 */
public class WolverineCowlLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The fin's outline, as (z, y) in the pixels of the head: the front root, the front edge, the tip, the back. */
    static final float[][] FIN = {{-2.4F, -4.4F}, {-1.4F, -8.2F}, {3.6F, -12.2F}, {2.8F, -6.2F}};
    /** How far out from the middle of the head the fin sits (its root, its tip), and how thick it is. */
    static final float ROOT_X = 4.25F;
    static final float TIP_X = 5.1F;
    static final float THICK = 0.55F;
    static final int BLACK = 0x17171B;
    static final int RIM = 0x3A3A42;

    public WolverineCowlLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Whether the player wears the cowl now: Wolverine in the classic look, no helmet, seen. */
    public static boolean wears(AbstractClientPlayer player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.WOLVERINE && "classic".equals(Variants.current(data).id())
                && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !player.isInvisible();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (!wears(player) || !getParentModel().head.visible) {
            return;
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(ClawLayer.TEXTURE));
        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        Matrix4f pose = poseStack.last().pose();
        PoseStack.Pose last = poseStack.last();
        for (int s = -1; s <= 1; s += 2) {
            fin(pose, last, vc, packedLight, s);
        }
        poseStack.popPose();
    }

    /** One fin, on the side {@code s} (-1 right, 1 left): its two faces and its four edges. */
    private static void fin(Matrix4f pose, PoseStack.Pose last, VertexConsumer vc, int light, int s) {
        float[][] in = new float[4][3];
        float[][] out = new float[4][3];
        for (int i = 0; i < 4; i++) {
            float x = i == 2 ? TIP_X : ROOT_X;
            in[i] = new float[]{s * x, FIN[i][1], FIN[i][0]};
            out[i] = new float[]{s * (x + THICK), FIN[i][1], FIN[i][0]};
        }
        face(pose, last, vc, light, BLACK, -s, 0, 0, in[0], in[1], in[2], in[3]);
        face(pose, last, vc, light, BLACK, s, 0, 0, out[3], out[2], out[1], out[0]);
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            face(pose, last, vc, light, RIM, 0, i == 3 ? 1 : -1, i == 0 ? -1 : 1, in[i], out[i], out[j], in[j]);
        }
    }

    private static void face(Matrix4f pose, PoseStack.Pose last, VertexConsumer vc, int light, int rgb, float nx,
            float ny, float nz, float[] a, float[] b, float[] c, float[] d) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int bl = rgb & 0xFF;
        for (float[] p : new float[][]{a, b, c, d}) {
            // (The white corner of the claw texture, tinted.)
            vc.addVertex(pose, p[0], p[1], p[2]).setColor(r, g, bl, 255).setUv(0.9F, 0.1F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
        }
    }
}
