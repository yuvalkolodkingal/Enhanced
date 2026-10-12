package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.SandBoulderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** A boulder of Apocalypse's sand: a block of it, tumbling as it flies. */
public class SandBoulderRenderer extends EntityRenderer<SandBoulderEntity> {
    /** The block is drawn a little smaller than a whole one: the boulder's hitbox is small, and what hurts must look it. */
    private static final float SIZE = 0.8F;

    private final BlockRenderDispatcher blocks;

    public SandBoulderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SandBoulderEntity boulder, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = boulder.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0.0, boulder.getBbHeight() * 0.5, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 17.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(t * 23.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(t * 11.0F));
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(Blocks.SAND.defaultBlockState(), poseStack, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(boulder, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SandBoulderEntity boulder) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
