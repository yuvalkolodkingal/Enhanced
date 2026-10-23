package com.kolodkingal.enhanced.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/** Drawing blocks' models the loader's way (port P0, PLAN2 3.3). Client only. */
public final class BlockModels {
    private BlockModels() {
    }

    /**
     * Draws the model of {@code state} at {@code pos} (as a chunk section would: {@code view} gives the neighbours and
     * the tints, {@code checkSides} hides the faces they cover) into the buffer of each of its render types, which
     * {@code bufferFor} gives. {@code pose} is where the block goes; seed {@code random} first
     * ({@code state.getSeed(pos)}). NeoForge: the model's data from {@code view}, then
     * {@code renderBatched(..., data, type)} for each of {@code getRenderTypes(state, random, data)}.
     */
    public static void renderBatched(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos,
                                     BlockAndTintGetter view, PoseStack pose, boolean checkSides, RandomSource random,
                                     Function<RenderType, VertexConsumer> bufferFor) {
        ClientServices.get().renderBatched(dispatcher, state, pos, view, pose, checkSides, random, bufferFor);
    }
}
