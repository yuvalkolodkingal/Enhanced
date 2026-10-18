package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * A footing under a template set on uneven ground: under the lowest block of every column of the template, where the
 * ground falls away (air, water, plants), the column is filled down to the ground, at most {@code max_depth} blocks. A
 * column of stone or bricks goes on in its own block; anything else (grass, a path, a block that falls) stands on
 * {@code block}. It must be the last processor of its list: it adds blocks, and it only touches the part of the template
 * that is being placed (the chunk being generated).
 */
public class FoundationProcessor extends StructureProcessor {
    public static final MapCodec<FoundationProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("block").forGetter(p -> p.block),
            Codec.intRange(1, 64).optionalFieldOf("max_depth", 16).forGetter(p -> p.maxDepth)
    ).apply(instance, FoundationProcessor::new));

    private final BlockState block;
    private final int maxDepth;

    public FoundationProcessor(BlockState block, int maxDepth) {
        this.block = block;
        this.maxDepth = maxDepth;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.FOUNDATION.get();
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfos, List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
            StructurePlaceSettings settings) {
        BoundingBox box = settings.getBoundingBox();
        if (box == null) {
            return processedBlockInfos;
        }
        Long2ObjectOpenHashMap<StructureTemplate.StructureBlockInfo> lowest = new Long2ObjectOpenHashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            BlockPos at = info.pos();
            if (info.state().isAir() || info.state().is(Blocks.STRUCTURE_VOID) || at.getX() < box.minX() || at.getX() > box.maxX()
                    || at.getZ() < box.minZ() || at.getZ() > box.maxZ()) {
                continue;
            }
            long column = BlockPos.asLong(at.getX(), 0, at.getZ());
            StructureTemplate.StructureBlockInfo known = lowest.get(column);
            if (known == null || at.getY() < known.pos().getY()) {
                lowest.put(column, info);
            }
        }
        if (lowest.isEmpty()) {
            return processedBlockInfos;
        }
        List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(processedBlockInfos);
        for (StructureTemplate.StructureBlockInfo info : lowest.values()) {
            BlockState fill = continues(info.state()) ? info.state() : block;
            BlockPos.MutableBlockPos at = info.pos().mutable();
            for (int depth = 0; depth < maxDepth; depth++) {
                at.move(0, -1, 0);
                if (at.getY() < level.getMinBuildHeight() || !box.isInside(at)) {
                    break;
                }
                BlockState there = level.getBlockState(at);
                if (!there.isAir() && !there.canBeReplaced() && there.getFluidState().isEmpty()) {
                    break;
                }
                out.add(new StructureTemplate.StructureBlockInfo(at.immutable(), fill, null));
            }
        }
        return out;
    }

    /** Whether a column standing on this block goes down in the same block (stone, bricks), rather than on the footing. */
    static boolean continues(BlockState state) {
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && !state.hasBlockEntity()
                && !(state.getBlock() instanceof FallingBlock) && !state.is(BlockTags.DIRT) && !state.is(Blocks.DIRT_PATH)
                && state.getFluidState().isEmpty();
    }
}
