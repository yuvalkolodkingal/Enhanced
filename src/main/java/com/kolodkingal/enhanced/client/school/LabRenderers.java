package com.kolodkingal.enhanced.client.school;

import com.kolodkingal.enhanced.block.AdamantiumRackBlock;
import com.kolodkingal.enhanced.block.AdamantiumRackBlockEntity;
import com.kolodkingal.enhanced.block.CerebroConsoleBlock;
import com.kolodkingal.enhanced.block.CerebroConsoleBlockEntity;
import com.kolodkingal.enhanced.block.WeaponXPodBlock;
import com.kolodkingal.enhanced.block.WeaponXPodBlockEntity;
import com.kolodkingal.enhanced.registry.ModItems;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The renderers of the lab's blocks with block entities (3.7, lane L4): the Weapon X rack's three injections (their own
 * item model, lane L2's), the Weapon X Pod's green light, its glass hatch (swung open when idle, shut while a treatment
 * runs) and the Cerebro console's helmet on its stand (gone while the console is in use). Each draws in the frame of a
 * block facing north and is turned to its block's facing.
 */
public final class LabRenderers {

    /** Turns the pose to a block's facing (about the block's middle), as its model is turned. */
    static void face(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180.0F) % 360.0F)));
        pose.translate(-0.5, 0.0, -0.5);
    }

    /** A block drawn as a box from (x0, y0, z0) to (x1, y1, z1) in sixteenths. */
    static void box(BlockRenderDispatcher blocks, BlockState state, PoseStack pose, MultiBufferSource buffers,
                    int light, double x0, double y0, double z0, double x1, double y1, double z1) {
        pose.pushPose();
        pose.translate(x0 / 16.0, y0 / 16.0, z0 / 16.0);
        pose.scale((float) (x1 - x0) / 16.0F, (float) (y1 - y0) / 16.0F, (float) (z1 - z0) / 16.0F);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** The rack: three injections standing in its slots, a little apart. */
    public static final class Rack implements BlockEntityRenderer<AdamantiumRackBlockEntity> {
        private final ItemStack injection = new ItemStack(ModItems.ADAMANTIUM_INJECTION.get());

        public Rack(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(AdamantiumRackBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            BlockState state = be.getBlockState();
            if (!state.hasProperty(AdamantiumRackBlock.FACING)) {
                return;
            }
            pose.pushPose();
            face(pose, state.getValue(AdamantiumRackBlock.FACING));
            for (int i = 0; i < 3; i++) {
                pose.pushPose();
                pose.translate((3.5 + i * 4.5) / 16.0, 7.5 / 16.0, 8.0 / 16.0);
                pose.mulPose(Axis.ZP.rotationDegrees(-45.0F));
                pose.scale(0.42F, 0.42F, 0.42F);
                Minecraft.getInstance().getItemRenderer().renderStatic(injection, ItemDisplayContext.FIXED, light,
                        OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), i);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /** The pod: its green light inside, and its glass hatch. */
    public static final class Pod implements BlockEntityRenderer<WeaponXPodBlockEntity> {
        public Pod(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(WeaponXPodBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            BlockState state = be.getBlockState();
            if (!state.hasProperty(WeaponXPodBlock.FACING) || be.getLevel() == null) {
                return;
            }
            BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            float time = be.getLevel().getGameTime() + partialTick;
            pose.pushPose();
            face(pose, state.getValue(WeaponXPodBlock.FACING));
            // the light: a green column at the back, pulsing (faster while a treatment runs)
            float pulse = 0.5F + 0.5F * (float) Math.sin(time / (be.running() ? 3.0F : 12.0F));
            box(blocks, Blocks.LIME_STAINED_GLASS.defaultBlockState(), pose, buffers, LightTexture.FULL_BRIGHT,
                    3, 1, 12.0 - pulse, 13, 31, 13.5);
            // the hatch: shut across the front while it runs, swung open on its left hinge while idle
            pose.pushPose();
            if (!be.running()) {
                pose.translate(2.0 / 16.0, 0.0, 1.0 / 16.0);
                pose.mulPose(Axis.YP.rotationDegrees(105.0F));
                pose.translate(-2.0 / 16.0, 0.0, -1.0 / 16.0);
            }
            box(blocks, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), pose, buffers, light, 2, 1, 0.5, 14, 31,
                    1.5);
            pose.popPose();
            pose.popPose();
        }

        // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
        public AABB getRenderBoundingBox(WeaponXPodBlockEntity be) {
            return new AABB(be.getBlockPos()).expandTowards(0, 1, 0).inflate(1.0);
        }
    }

    /**
     * The console: Cerebro's helmet on its stand, at the back of the desk; gone while the console is in use (its block
     * entity says so, or {@link #helmetOff} does: lane L5's {@code CerebroSphere.consoleOn}, wired by the integrator).
     */
    public static final class Console implements BlockEntityRenderer<CerebroConsoleBlockEntity> {
        /** Whether the console at a position is on (its helmet worn), as the client knows it. */
        public static java.util.function.Predicate<net.minecraft.core.BlockPos> helmetOff = pos -> false;

        public Console(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(CerebroConsoleBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            BlockState state = be.getBlockState();
            if (!state.hasProperty(CerebroConsoleBlock.FACING) || be.getLevel() == null) {
                return;
            }
            BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            pose.pushPose();
            face(pose, state.getValue(CerebroConsoleBlock.FACING));
            // the stand
            box(blocks, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), pose, buffers, light, 7, 12, 11, 9, 17, 13);
            if (!be.inUse() && !helmetOff.test(be.getBlockPos())) {
                // the helmet: a steel cap with a lit band round it, and two cables down into the desk
                box(blocks, Blocks.IRON_BLOCK.defaultBlockState(), pose, buffers, light, 4, 17, 8, 12, 20, 16);
                box(blocks, Blocks.IRON_BLOCK.defaultBlockState(), pose, buffers, light, 5, 20, 9, 11, 21.5, 15);
                box(blocks, Blocks.SEA_LANTERN.defaultBlockState(), pose, buffers, LightTexture.FULL_BRIGHT, 3.9,
                        17.5, 7.9, 12.1, 18.3, 16.1);
                box(blocks, Blocks.BLACK_CONCRETE.defaultBlockState(), pose, buffers, light, 5, 12, 14.5, 6, 18, 15.5);
                box(blocks, Blocks.BLACK_CONCRETE.defaultBlockState(), pose, buffers, light, 10, 12, 14.5, 11, 18,
                        15.5);
            }
            pose.popPose();
        }

        // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
        public AABB getRenderBoundingBox(CerebroConsoleBlockEntity be) {
            return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
        }
    }

    private LabRenderers() {
    }
}
