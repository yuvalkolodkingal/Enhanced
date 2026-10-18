package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Where the city of Asgard is drawn during generation (3.5): one chunk, written directly (never through the level), and
 * never outside it, so any chunk is built alone. Blocks with a block entity get its data (or an empty one, made when the
 * chunk is finished); fences, panes, walls and stairs are marked so that their shapes join their neighbours when the
 * chunk is finished. The land's columns of the chunk and its border are cached ({@link #column}).
 */
public final class CityCanvas {
    private final ChunkAccess chunk;
    private final int minX;
    private final int minZ;
    private final BoundingBox box;
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private final AsgardTerrain.Column[] columns = new AsgardTerrain.Column[18 * 18];

    public CityCanvas(ChunkAccess chunk) {
        this.chunk = chunk;
        this.minX = chunk.getPos().getMinBlockX();
        this.minZ = chunk.getPos().getMinBlockZ();
        this.box = new BoundingBox(minX, chunk.getMinBuildHeight(), minZ, minX + 15, chunk.getMaxBuildHeight() - 1, minZ + 15);
    }

    public ChunkAccess chunk() {
        return chunk;
    }

    /** The chunk's box (all heights). */
    public BoundingBox box() {
        return box;
    }

    public int minX() {
        return minX;
    }

    public int minZ() {
        return minZ;
    }

    public boolean contains(int x, int y, int z) {
        return box.isInside(x, y, z);
    }

    /** The land's column at (x, z): cached within one block of the chunk. */
    public AsgardTerrain.Column column(int x, int z) {
        int i = x - minX + 1;
        int k = z - minZ + 1;
        if (i < 0 || i >= 18 || k < 0 || k >= 18) {
            return AsgardTerrain.column(x, z);
        }
        AsgardTerrain.Column c = columns[i * 18 + k];
        if (c == null) {
            c = AsgardTerrain.column(x, z);
            columns[i * 18 + k] = c;
        }
        return c;
    }

    /** The top block of the land at (x, z) (MIN_VALUE where there is none). */
    public int top(int x, int z) {
        AsgardTerrain.Column c = column(x, z);
        return c.solid() ? c.top() : Integer.MIN_VALUE;
    }

    public BlockState get(int x, int y, int z) {
        if (!contains(x, y, z)) {
            return Blocks.AIR.defaultBlockState();
        }
        return chunk.getBlockState(at.set(x, y, z));
    }

    /** Sets a block (inside the chunk only). */
    public void set(int x, int y, int z, BlockState state) {
        set(x, y, z, state, null);
    }

    /** Sets a block with its block entity's data (inside the chunk only). */
    public void set(int x, int y, int z, BlockState state, @Nullable CompoundTag data) {
        if (!contains(x, y, z)) {
            return;
        }
        at.set(x, y, z);
        BlockState old = chunk.setBlockState(at, state, false);
        if (state.hasBlockEntity()) {
            CompoundTag tag = data == null ? new CompoundTag() : data.copy();
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            if (!tag.contains("id")) {
                tag.putString("id", "DUMMY");
            }
            chunk.setBlockEntityNbt(tag);
        } else if (old != null && old.hasBlockEntity()) {
            chunk.removeBlockEntity(at);
        }
        if (joins(state)) {
            chunk.markPosForPostprocessing(at.immutable());
        }
    }

    private static boolean joins(BlockState state) {
        return state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof StairBlock || state.getBlock() instanceof FenceGateBlock;
    }
}
