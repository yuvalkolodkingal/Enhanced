package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Seals a template's carved air against the world's fluids (3.6, {@code enhanced:seal}): where a block of the
 * template's own air has, beside it or over it, a fluid of the world (a lava pocket of the Nether, an aquifer under a
 * cellar) that is not one of the template's blocks, that fluid's place becomes {@code block}, so nothing pours into
 * the rooms, the path or the pit once the site stands. It only touches the part being placed (the chunk being
 * generated), and comes before {@code enhanced:foundation} in its list.
 */
public class SealProcessor extends StructureProcessor {
    public static final MapCodec<SealProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("block").forGetter(p -> p.block)
    ).apply(instance, SealProcessor::new));

    private final BlockState block;

    public SealProcessor(BlockState block) {
        this.block = block;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.SEAL.get();
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset,
            BlockPos pos, List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfos, StructurePlaceSettings settings) {
        BoundingBox box = settings.getBoundingBox();
        if (box == null) {
            return processedBlockInfos;
        }
        LongOpenHashSet template = new LongOpenHashSet(processedBlockInfos.size());
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            template.add(info.pos().asLong());
        }
        LongOpenHashSet sealed = new LongOpenHashSet();
        List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(processedBlockInfos);
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            if (!info.state().isAir()) {
                continue;
            }
            for (Direction side : Direction.values()) {
                if (side == Direction.DOWN) {
                    continue;
                }
                BlockPos there = info.pos().relative(side);
                long key = there.asLong();
                if (!box.isInside(there) || template.contains(key) || sealed.contains(key)) {
                    continue;
                }
                if (!level.getFluidState(there).isEmpty()) {
                    sealed.add(key);
                    out.add(new StructureTemplate.StructureBlockInfo(there, block, null));
                }
            }
        }
        return out;
    }
}
