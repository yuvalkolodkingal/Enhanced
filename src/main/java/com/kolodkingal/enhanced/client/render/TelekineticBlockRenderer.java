package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Draws a lifted block, slowly turning in the air. */
public class TelekineticBlockRenderer extends EntityRenderer<TelekineticBlockEntity> {
    private final BlockRenderDispatcher blocks;

    public TelekineticBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TelekineticBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockState state = entity.getBlockState();
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        float t = entity.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.0, 0.5, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 3F));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(t * 0.1F) * 12F));
        poseStack.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TelekineticBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
