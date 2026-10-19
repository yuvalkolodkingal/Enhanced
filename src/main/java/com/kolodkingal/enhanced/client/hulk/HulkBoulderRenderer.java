package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.hulk.HulkBoulderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hulk's boulder in flight (3.6): the model of the block it was torn from (the one he stood on), a little bigger
 * than a block and tumbling end over end as it flies.
 */
public class HulkBoulderRenderer extends EntityRenderer<HulkBoulderEntity> {
    /** A torn-up boulder is a little bigger than one block. */
    public static final float SIZE = 1.15F;

    public HulkBoulderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(HulkBoulderEntity boulder, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int light) {
        BlockState block = boulder.block();
        if (block.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        float age = boulder.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.0, 0.5, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 18.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 7.0F));
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block, poseStack, buffers, light,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(boulder, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(HulkBoulderEntity boulder) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
