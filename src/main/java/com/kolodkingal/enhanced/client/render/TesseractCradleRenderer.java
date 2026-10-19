package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.block.TesseractCradleBlock;
import com.kolodkingal.enhanced.block.TesseractCradleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Tesseract on its cradle (3.6, Avengers Tower's roof): while the cradle is full, the cube floats over it, tilted
 * on a corner, turning and bobbing: a bright core in a shell of blue glass, drawn at full brightness (it glows).
 */
public class TesseractCradleRenderer implements BlockEntityRenderer<TesseractCradleBlockEntity> {
    public TesseractCradleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TesseractCradleBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        BlockState state = be.getBlockState();
        if (be.getLevel() == null || !state.hasProperty(TesseractCradleBlock.FULL)
                || !state.getValue(TesseractCradleBlock.FULL)) {
            return;
        }
        float time = (be.getLevel().getGameTime() % 24000L) + partialTick;
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        pose.pushPose();
        pose.translate(0.5, 1.2 + 0.06 * Math.sin(time / 16.0), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
        pose.mulPose(Axis.XP.rotationDegrees(35.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
        // the bright core, then the glass shell round it
        cube(blocks, Blocks.SEA_LANTERN.defaultBlockState(), 0.26F, pose, buffers, overlay);
        cube(blocks, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 0.46F, pose, buffers, overlay);
        pose.popPose();
    }

    private static void cube(BlockRenderDispatcher blocks, BlockState state, float size, PoseStack pose,
            MultiBufferSource buffers, int overlay) {
        pose.pushPose();
        pose.scale(size, size, size);
        pose.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TesseractCradleBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 2, 0);
    }
}
