package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Monster;

/**
 * The Double-Edged Sword in the Mad Titan's right hand. It is gone from the hand while the sword is out (thrown), and
 * whirls about its grip when the pose says so (the wind-up of the Throw, the leap of the Smash).
 */
public class ThanosSwordLayer<T extends Monster> extends RenderLayer<T, ThanosModel<T>> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/thanos_sword.png");

    private final ThanosSwordModel sword;

    public ThanosSwordLayer(RenderLayerParent<T, ThanosModel<T>> parent, ThanosSwordModel sword) {
        super(parent);
        this.sword = sword;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || entity instanceof ThanosEntity boss && boss.swordAway()) {
            return;
        }
        ThanosModel<T> model = getParentModel();
        poseStack.pushPose();
        model.translateToSwordArm(poseStack);
        // In the fist, held like a staff: the blades up and down, tilted a little forward.
        poseStack.translate(0.0F, 14.0F / 16.0F, -2.0F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(model.spin * ageInTicks));
        sword.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
    }
}
