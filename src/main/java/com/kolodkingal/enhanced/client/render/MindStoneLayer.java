package com.kolodkingal.enhanced.client.render;

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
import org.joml.Matrix4f;

/**
 * The Mind Stone glowing in Vision's forehead: an amber gem with a slow, breathing halo (hidden in disguise). White
 * Vision has no stone: a small hexagonal socket of copper circuitry glows faintly cyan in its place.
 */
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
        draw(poseStack, buffer, getParentModel().head, pulse, boost, "white_vision".equals(data.variant()));
    }

    /**
     * The stone (or White Vision's socket) on the forehead of {@code head}, in the pose the head is in.
     *
     * @param pulse how bright the slow breathing of the halo is just now (0.5 to 1)
     * @param boost how much wider the halo is (1.6 while he fires the beam)
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, ModelPart head, float pulse, float boost, boolean white) {
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        // Middle of the forehead, just in front of the face.
        poseStack.translate(0.0F, -6.0F / 16.0F, -4.15F / 16.0F);
        Matrix4f m = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        if (white) {
            hexagon(vc, m, 1.1F / 16.0F, EnergyGeometry.argb(0xDBC49C, 0.7F));
            diamond(vc, m, 0.45F / 16.0F, EnergyGeometry.argb(0xE8FBFF, 0.9F));
            halo(vc, m, 2.6F / 16.0F * pulse * boost, EnergyGeometry.argb(0x8FDFFF, 0.35F * pulse));
        } else {
            diamond(vc, m, 1.3F / 16.0F, 0xFFF0CC5A);
            diamond(vc, m, 0.7F / 16.0F, 0xFFFFF6D5);
            halo(vc, m, 3.0F / 16.0F * pulse * boost, EnergyGeometry.argb(0xF5D77A, 0.45F * pulse));
            halo(vc, m, 5.5F / 16.0F * pulse * boost, EnergyGeometry.argb(0xD6A83A, 0.18F * pulse));
        }
        poseStack.popPose();
    }

    private static void diamond(VertexConsumer vc, Matrix4f m, float s, int argb) {
        vc.addVertex(m, 0, -s, 0).setColor(argb);
        vc.addVertex(m, s, 0, 0).setColor(argb);
        vc.addVertex(m, 0, s, 0).setColor(argb);
        vc.addVertex(m, -s, 0, 0).setColor(argb);
    }

    /** A flat hexagon outline (White Vision's empty socket). */
    private static void hexagon(VertexConsumer vc, Matrix4f m, float r, int argb) {
        float inner = r * 0.7F;
        for (int i = 0; i < 6; i++) {
            float a0 = Mth.TWO_PI * i / 6 + Mth.PI / 6;
            float a1 = Mth.TWO_PI * (i + 1) / 6 + Mth.PI / 6;
            vc.addVertex(m, Mth.cos(a0) * inner, Mth.sin(a0) * inner, 0).setColor(argb);
            vc.addVertex(m, Mth.cos(a0) * r, Mth.sin(a0) * r, 0).setColor(argb);
            vc.addVertex(m, Mth.cos(a1) * r, Mth.sin(a1) * r, 0).setColor(argb);
            vc.addVertex(m, Mth.cos(a1) * inner, Mth.sin(a1) * inner, 0).setColor(argb);
        }
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
