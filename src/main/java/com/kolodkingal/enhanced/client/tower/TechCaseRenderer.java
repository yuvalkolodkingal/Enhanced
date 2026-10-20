package com.kolodkingal.enhanced.client.tower;

import com.kolodkingal.enhanced.block.TechCaseBlock;
import com.kolodkingal.enhanced.block.TechCaseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A tech case's insides (3.8, lane L6): the item on show turning slowly over the plinth and bobbing a little, lit as
 * if by the case's own light, and the glass lid, which slides up when something is taken ({@code TowerFx}'s {@code
 * CASE_OPEN}) and back down when the case closes ({@code CASE_CLOSE}, or by itself 40 ticks after opening). Drawn in
 * the frame of a case facing north (the block model's) and turned to its facing; every motion is interpolated with the
 * partial tick.
 */
public class TechCaseRenderer implements BlockEntityRenderer<TechCaseBlockEntity> {
    /** The ticks the lid takes to slide all the way, how high it goes (sixteenths), and when it closes by itself. */
    static final float LID_SLIDE = 6.0F;
    static final float LID_RISE = 5.0F;
    static final float LID_SHUT_AFTER = 40.0F;

    public TechCaseRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** How far up the lid is (0 shut, 1 up) at {@code time} (game time plus the partial tick). */
    static float lift(TechCaseBlockEntity box, float time) {
        if (box.openedAt == Long.MIN_VALUE) {
            return 0.0F;
        }
        float opening = (time - box.openedAt) / LID_SLIDE;
        float shutting = box.closedAt >= box.openedAt ? (time - box.closedAt) / LID_SLIDE
                : (time - box.openedAt - LID_SHUT_AFTER) / LID_SLIDE;
        return Mth.clamp(Math.min(opening, 1.0F - Math.max(0.0F, shutting)), 0.0F, 1.0F);
    }

    @Override
    public void render(TechCaseBlockEntity box, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockState state = box.getBlockState();
        if (!state.hasProperty(TechCaseBlock.FACING) || box.getLevel() == null) {
            return;
        }
        float time = box.getLevel().getGameTime() + partialTick;
        Direction facing = state.getValue(TechCaseBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180.0F) % 360.0F)));
        pose.translate(-0.5, 0.0, -0.5);
        ItemStack shown = box.shown();
        if (!shown.isEmpty()) {
            pose.pushPose();
            float bob = 0.02F * Mth.sin(time / 12.0F);
            pose.translate(0.5, 10.0 / 16.0 + bob, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees((time * 1.5F) % 360.0F));
            pose.scale(0.55F, 0.55F, 0.55F);
            int lit = LightTexture.pack(Math.max(LightTexture.block(light), 12), LightTexture.sky(light));
            Minecraft.getInstance().getItemRenderer().renderStatic(shown, ItemDisplayContext.FIXED, lit,
                    OverlayTexture.NO_OVERLAY, pose, buffers, box.getLevel(), (int) box.getBlockPos().asLong());
            pose.popPose();
        }
        // the lid: a thin pane of glass over the case's walls
        float up = lift(box, time) * LID_RISE;
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        pose.pushPose();
        pose.translate(2.0 / 16.0, (15.0 + up) / 16.0, 2.0 / 16.0);
        pose.scale(12.0F / 16.0F, 1.0F / 16.0F, 12.0F / 16.0F);
        blocks.renderSingleBlock(Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), pose, buffers, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TechCaseBlockEntity box) {
        return new AABB(box.getBlockPos()).expandTowards(0.0, 0.5, 0.0);
    }
}
