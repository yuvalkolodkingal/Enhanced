package com.kolodkingal.enhanced.nano;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The Horton manor (3.12): the house of the family that built the first android, on flat dry land. Built by code
 * ({@link HortonManorPiece}); its nanotech storage wakes Nano Ultron.
 */
public class HortonManorStructure extends Structure {
    public static final MapCodec<HortonManorStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance)).apply(instance, HortonManorStructure::new));

    public HortonManorStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int dx = -HortonManorPiece.HALF_X; dx <= HortonManorPiece.HALF_X; dx += HortonManorPiece.HALF_X) {
            for (int dz = -HortonManorPiece.HALF_Z; dz <= HortonManorPiece.HALF_Z; dz += HortonManorPiece.HALF_Z) {
                int h = context.chunkGenerator().getFirstOccupiedHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                low = Math.min(low, h);
                high = Math.max(high, h);
            }
        }
        if (high - low > 4 || low <= context.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }
        BlockPos at = new BlockPos(x, low, z);
        return Optional.of(new GenerationStub(at, builder -> builder.addPiece(new HortonManorPiece(at))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.HORTON_MANOR.get();
    }
}
