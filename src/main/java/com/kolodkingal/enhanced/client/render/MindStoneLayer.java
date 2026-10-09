package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** The Mind Stone glowing in Vision's forehead: a bright gem with a slow, breathing halo (hidden in disguise). */
public class MindStoneLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public MindStoneLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (data.power() != Power.VISION || data.disguise() || player.isInvisible()) {
            return;
        }
        float pulse = 0.75F + 0.25F * Mth.sin(ageInTicks * 0.12F);
        float boost = data.beamActive() ? 1.6F : 1.0F;
        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        // Middle of the forehead, just in front of the face.
        poseStack.translate(0.0F, -6.0F / 16.0F, -4.15F / 16.0F);
        Matrix4f m = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        diamond(vc, m, 1.3F / 16.0F, 0xFFFFE066);
        diamond(vc, m, 0.7F / 16.0F, 0xFFFFFFE0);
        halo(vc, m, 3.0F / 16.0F * pulse * boost, EnergyGeometry.argb(0xFFD23A, 0.45F * pulse));
        halo(vc, m, 5.5F / 16.0F * pulse * boost, EnergyGeometry.argb(0xFFB020, 0.18F * pulse));
        poseStack.popPose();
    }

    private static void diamond(VertexConsumer vc, Matrix4f m, float s, int argb) {
        vc.addVertex(m, 0, -s, 0).setColor(argb);
        vc.addVertex(m, s, 0, 0).setColor(argb);
        vc.addVertex(m, 0, s, 0).setColor(argb);
        vc.addVertex(m, -s, 0, 0).setColor(argb);
    }

    /** A soft round glow: full colour in the middle, fading to nothing at the rim (no hard edges in daylight). */
    private static void halo(VertexConsumer vc, Matrix4f m, float r, int argb) {
        int rim = argb & 0x00FFFFFF;
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            vc.addVertex(m, 0, 0, -0.001F).setColor(argb);
            vc.addVertex(m, 0, 0, -0.001F).setColor(argb);
            vc.addVertex(m, Mth.cos(a1) * r, Mth.sin(a1) * r, -0.001F).setColor(rim);
            vc.addVertex(m, Mth.cos(a0) * r, Mth.sin(a0) * r, -0.001F).setColor(rim);
        }
    }
}
