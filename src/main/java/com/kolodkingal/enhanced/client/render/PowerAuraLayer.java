package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Swirling energy drawn over the body:
 * <ul>
 *     <li>the Scarlet Witch in flight: glowing red energy around both hands;</li>
 *     <li>Photon while phased: a white shimmer over her whole body.</li>
 * </ul>
 */
public class PowerAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation SWIRL = Enhanced.id("textures/entity/energy_swirl.png");

    private final PlayerModel<AbstractClientPlayer> model;

    public PowerAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, PlayerModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        boolean hands = data.power() == Power.SCARLET_WITCH && data.flying();
        boolean phased = data.power() == Power.PHOTON && data.isPhased();
        if ((!hands && !phased) || player.isInvisible()) {
            return;
        }
        float t = player.tickCount + partialTicks;
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        model.setAllVisible(phased);
        if (hands) {
            model.rightArm.visible = true;
            model.leftArm.visible = true;
            model.rightSleeve.visible = false;
            model.leftSleeve.visible = false;
        }
        int color = phased ? 0xB0E8F6FF : 0x90FF2A3A;
        VertexConsumer swirl = buffer.getBuffer(RenderType.energySwirl(SWIRL, (t * 0.02F) % 1F, (t * 0.013F) % 1F));
        model.renderToBuffer(poseStack, swirl, packedLight, OverlayTexture.NO_OVERLAY, color);

        if (hands) {
            VertexConsumer glow = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            handOrb(poseStack, glow, model.rightArm, -1F / 16F, t);
            handOrb(poseStack, glow, model.leftArm, 1F / 16F, t + 20F);
        }
    }

    /** A spinning ball of chaos energy cupped in the hand. */
    private static void handOrb(PoseStack poseStack, VertexConsumer vc, ModelPart arm, float x, float t) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.translate(x, 10F / 16F, 0F);
        float pulse = 0.85F + 0.15F * (float) Math.sin(t * 0.5F);
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 9F + i * 60F));
            poseStack.mulPose(Axis.XP.rotationDegrees(t * 6F + i * 45F));
            Matrix4f m = poseStack.last().pose();
            quad(vc, m, 0.28F * pulse, 0x80E0102A);
            quad(vc, m, 0.13F * pulse, 0xC0FF6A80);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float s, int argb) {
        vc.addVertex(m, -s, -s, 0).setColor(argb);
        vc.addVertex(m, s, -s, 0).setColor(argb);
        vc.addVertex(m, s, s, 0).setColor(argb);
        vc.addVertex(m, -s, s, 0).setColor(argb);
    }
}
