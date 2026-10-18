package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.block.asgard.BifrostMechanismBlockEntity;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Bifrost mechanism's ring (3.5): two rings of gold trim round the Hofund socket, turning against each other, slowly
 * at rest and fast for three seconds whenever the Bifrost touches down or leaves near it (the angle is a smooth function
 * of the time and the partial tick: {@link BifrostMechanismBlockEntity#angle}).
 */
public class BifrostMechanismRenderer implements BlockEntityRenderer<BifrostMechanismBlockEntity> {
    private static final int OUTER = 28;
    private static final int INNER = 18;

    public BifrostMechanismRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BifrostMechanismBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        double time = be.getLevel().getGameTime() + partialTick;
        float angle = BifrostMechanismBlockEntity.angle(be.spinStart(), time);
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        BlockState trim = ModBlocks.ASGARD_GOLD_TRIM.get().defaultBlockState();
        BlockState plate = ModBlocks.ASGARD_GOLD_PLATING.get().defaultBlockState();
        ring(pose, buffers, blocks, trim, angle, 3.3, 1.7, OUTER, 0.42F, light, overlay);
        ring(pose, buffers, blocks, plate, -angle * 1.6F, 2.3, 1.45, INNER, 0.32F, light, overlay);
    }

    private static void ring(PoseStack pose, MultiBufferSource buffers, BlockRenderDispatcher blocks, BlockState state, float angle,
                             double radius, double height, int segments, float size, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, height, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        for (int i = 0; i < segments; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(360.0F * i / segments));
            pose.translate(radius, 0, 0);
            pose.scale(size, size * 0.5F, size * 1.6F);
            pose.translate(-0.5, -0.5, -0.5);
            blocks.renderSingleBlock(state, pose, buffers, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BifrostMechanismBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BifrostMechanismBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(4.0, 2.0, 4.0);
    }
}
