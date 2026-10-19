package com.kolodkingal.enhanced.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

/**
 * The 3.6 skeleton's stand-in generation of the three new kinds of site ({@link NetherSiteStructure}, {@link
 * CarvedSiteStructure}, {@link TowerSiteStructure}), so that their structures generate, {@code /locate} finds them and
 * the tests compile before lane L4 writes the real ones: the template (a 3 x 3 x 3 marker until L4's templates land)
 * set down whole, unturned, on the first ground of the chunk's centre column. Lane L4 replaces every use of it and
 * deletes it.
 */
final class PlaceholderSites {
    /** No processors: the marker is placed as it is. */
    private static final Holder<StructureProcessorList> NONE = Holder.direct(new StructureProcessorList(List.of()));

    /** The template set down with its layer 0 at {@code y}, centred on the chunk. */
    static Optional<Structure.GenerationStub> at(Structure.GenerationContext context, ResourceLocation template,
            int y) {
        StructureTemplate built = context.structureTemplateManager().getOrCreate(template);
        Vec3i size = built.getSize();
        if (size.getX() == 0 || size.getZ() == 0) {
            return Optional.empty();
        }
        ChunkPos chunk = context.chunkPos();
        BlockPos center = new BlockPos(chunk.getMiddleBlockX(), y, chunk.getMiddleBlockZ());
        return Optional.of(new Structure.GenerationStub(center, builder -> builder.addPiece(
                new TemplateSitePiece(context.structureTemplateManager(), template, NONE, Rotation.NONE, center,
                        size, 0))));
    }

    /**
     * The surface of the chunk's centre column (no plants yet), or empty above the void (an End chunk without an
     * island).
     */
    static Optional<Structure.GenerationStub> onSurface(Structure.GenerationContext context,
            ResourceLocation template) {
        ChunkPos chunk = context.chunkPos();
        int top = SiteGround.surface(context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
        if (top <= context.heightAccessor().getMinBuildHeight() + 1) {
            return Optional.empty();
        }
        return at(context, template, top);
    }

    /**
     * The first solid floor from {@code from} upward with {@code headroom} blocks of air over it (a Nether cavern's
     * floor).
     */
    static Optional<Structure.GenerationStub> onCavernFloor(Structure.GenerationContext context,
            ResourceLocation template, int from, int to,
                                                            int headroom) {
        ChunkPos chunk = context.chunkPos();
        NoiseColumn column = context.chunkGenerator().getBaseColumn(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                context.heightAccessor(), context.randomState());
        for (int y = from; y < to; y++) {
            BlockState floor = column.getBlock(y);
            if (floor.isAir() || !floor.getFluidState().isEmpty()) {
                continue;
            }
            boolean room = true;
            for (int up = 1; up <= headroom && room; up++) {
                room = column.getBlock(y + up).isAir();
            }
            if (room) {
                return at(context, template, y + 1);
            }
        }
        return Optional.empty();
    }

    private PlaceholderSites() {
    }
}
