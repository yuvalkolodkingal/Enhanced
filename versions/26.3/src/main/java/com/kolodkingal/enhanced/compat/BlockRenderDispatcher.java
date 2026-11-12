package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

import java.util.function.Function;

/**
 * 26.3 only: 26.3 has no block render dispatcher. This one does what the mod asked of 1.21.1's: a single block drawn
 * where the pose is (submitted as vanilla's block models are), a block tesselated into a mesh as a chunk section would
 * (its quads into the buffer of each quad's layer), and a liquid.
 */
public final class BlockRenderDispatcher {
    private static final BlockRenderDispatcher INSTANCE = new BlockRenderDispatcher();
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    private BlockRenderDispatcher() {
    }

    public static BlockRenderDispatcher get() {
        return INSTANCE;
    }

    public BlockStateModel getBlockModel(BlockState state) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
    }

    /** 1.21.1's {@code renderSingleBlock}: the block's model at the pose's origin (0..1 on each axis). */
    public void renderSingleBlock(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockModelRenderState block = new BlockModelRenderState();
        new BlockModelResolver(Minecraft.getInstance().getModelManager()).update(block, state, DISPLAY);
        int outline = buffers instanceof SubmitBuffers sb ? sb.outlineColor() : 0;
        SubmitBuffers.submit(buffers, collector -> block.submitMultiLayer(pose, collector, light, overlay, outline));
    }

    /** The moving-block render type of a chunk layer (what the mod's meshes are kept by). */
    public static RenderType movingType(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    /**
     * A block as a chunk section draws it into the buffer of each of its quads' layer ({@code bufferFor} gets the
     * moving-block render type of the layer). The pose may only move the block (its translation is used).
     */
    public void renderBatched(BlockState state, BlockPos pos, BlockAndTintGetter view, PoseStack pose, boolean checkSides,
                              long seed, Function<RenderType, VertexConsumer> bufferFor) {
        Matrix4f m = pose.last().pose();
        ModelBlockRenderer renderer = new ModelBlockRenderer(Minecraft.getInstance().options.ambientOcclusion().get(), checkSides,
                Minecraft.getInstance().getBlockColors());
        renderer.tesselateBlock((x, y, z, quad, instance) -> bufferFor.apply(movingType(quad.materialInfo().layer()))
                        .putBlockBakedQuad(x, y, z, quad, instance),
                m.m30(), m.m31(), m.m32(), view, pos, state, getBlockModel(state), seed);
    }

    /** 1.21.1's {@code renderLiquid}: the liquid's faces, at the section-relative position of {@code pos}. */
    public void renderLiquid(BlockPos pos, BlockAndTintGetter view, VertexConsumer buffer, BlockState state, FluidState fluid) {
        new FluidRenderer(Minecraft.getInstance().getModelManager().getFluidStateModelSet())
                .tesselate(view, pos, layer -> buffer, state, fluid);
    }
}
