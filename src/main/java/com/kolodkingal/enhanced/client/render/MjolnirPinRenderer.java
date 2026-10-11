package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.MjolnirPinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * Mjolnir lying across the chest of whoever it pins down, as big as they are, its handle across their body and a little
 * heavier on one side. It follows them frame by frame (not tick by tick), and is not drawn on your own chest in first
 * person, where it would be in your face.
 */
public class MjolnirPinRenderer extends EntityRenderer<MjolnirPinEntity> {
    /**
     * The hammer is drawn this big for each block of the target's width, or of its height, whichever asks for more...
     */
    private static final float PER_WIDTH = 1.75F;
    private static final float PER_HEIGHT = 0.58F;
    /** ...and never smaller than this (a mouse) nor bigger than that (a giant). */
    private static final float MIN_SCALE = 0.8F;
    private static final float MAX_SCALE = 2.2F;

    private final ItemRenderer itemRenderer;

    public MjolnirPinRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(MjolnirPinEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity target = entity.level().getEntity(entity.targetEntityId());
        Minecraft mc = Minecraft.getInstance();
        if (target != null && target == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        poseStack.pushPose();
        float facing = 0.0F;
        float scale = 1.0F;
        if (target != null) {
            // Stuck to the target as it is drawn right now, and lying on its chest.
            Vec3 chest = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.62, 0);
            Vec3 here = entity.getPosition(partialTick);
            poseStack.translate(chest.x - here.x, chest.y - here.y, chest.z - here.z);
            if (target instanceof LivingEntity living) {
                facing = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            }
            scale = Mth.clamp(Math.max(target.getBbWidth() * PER_WIDTH, target.getBbHeight() * PER_HEIGHT), MIN_SCALE, MAX_SCALE);
        }
        float settle = Mth.sin((entity.tickCount + partialTick) * 0.12F) * 0.01F;
        poseStack.translate(0.0F, settle, 0.0F);
        // Across the body, a little tilted: the handle lower on one side, as if the head were what weighs it down.
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing));
        poseStack.mulPose(Axis.ZP.rotationDegrees(88.0F));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(entity.hammer(), ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(MjolnirPinEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
