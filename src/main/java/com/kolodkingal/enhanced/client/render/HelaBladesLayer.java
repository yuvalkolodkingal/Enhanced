package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Hela's blades in her hands (3.5, PLAN 7.3, the stance {@link Stance#HELA_BLADES}): while her form is on, a necrosword
 * ({@link NecroswordModel}) in each empty hand, grip in the fist and the blade along the line of the hand, its green edge
 * glowing; the hand that holds Death's Grasp out is empty (its fingers are spread). An item in a hand takes the blade's
 * place. Item-less: the blades are not items and cannot be dropped.
 */
public class HelaBladesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final NecroswordModel blade;

    public HelaBladesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, NecroswordModel blade) {
        super(parent);
        this.blade = blade;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (player.isInvisible() || !data.has(HeroAbility.HELA_GODDESS) || !data.toggle(Toggle.HELA_FORM)) {
            return;
        }
        boolean grasping = data.is(Live.HELA_GRASPING);
        boolean rightMain = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        if (player.getItemBySlot(rightMain ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND).isEmpty() && !(grasping && rightMain)) {
            draw(poseStack, buffer, packedLight, getParentModel().rightArm, true);
        }
        if (player.getItemBySlot(rightMain ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND).isEmpty() && !(grasping && !rightMain)) {
            draw(poseStack, buffer, packedLight, getParentModel().leftArm, false);
        }
    }

    private void draw(PoseStack poseStack, MultiBufferSource buffer, int light, ModelPart arm, boolean right) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.translate((right ? -1.0F : 1.0F) / 16.0F, 10.0F / 16.0F, -1.0F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(right ? 8.0F : -8.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        blade.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(NecroswordModel.TEXTURE)), light, OverlayTexture.NO_OVERLAY, -1);
        blade.renderEdges(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(NecroswordModel.TEXTURE)), OverlayTexture.NO_OVERLAY,
                EnergyGeometry.argb(HelaRules.PALE, 0.85F));
        poseStack.popPose();
    }
}
