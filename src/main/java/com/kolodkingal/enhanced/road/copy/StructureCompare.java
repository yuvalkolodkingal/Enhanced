package com.kolodkingal.enhanced.road.copy;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The comparison of the player's plot with the model, block by block, as plain code that knows nothing of the world (the
 * plot is read through {@link Plot}). A block is right when it is the same block and every part of its state that can be
 * seen from the player's point of view is the same: which way a stair or a pumpkin faces, whether a stair or slab is in the upper
 * or lower half, a slab's kind (bottom, top, double), which way a log or pillar lies, whether a lantern hangs. Everything else is
 * ignored because the game works it out from the neighbours or because it makes no difference to the look (the shape a stair
 * takes at a corner, how a fence or wall joins its neighbours, water in a block, power).
 *
 * <p>Anything in the plot that the model has no block for is an extra, and the copy is not exact while there is one.
 */
public final class StructureCompare {
    /** How many layers above the model's own height the plot reaches (room for scaffolding, and extras there still count). */
    public static final int EXTRA_HEIGHT = 2;

    /** The parts of a block's state that count (their names in the block state). */
    private static final Set<String> COMPARED = Set.of(
            BlockStateProperties.FACING.getName(), BlockStateProperties.HALF.getName(), BlockStateProperties.SLAB_TYPE.getName(),
            BlockStateProperties.AXIS.getName(), BlockStateProperties.HANGING.getName());

    /** A cell of the plot, as an offset from its corner. */
    public record Cell(int x, int y, int z) {
    }

    /**
     * What the comparison found.
     *
     * @param correct blocks of the model that are in the plot, right
     * @param total   blocks the model has
     * @param missing cells of the model that are empty in the plot
     * @param wrong   cells of the model that hold another block (or another way round) in the plot
     * @param extra   cells outside the model that hold a block in the plot
     */
    public record Result(int correct, int total, List<Cell> missing, List<Cell> wrong, List<Cell> extra) {
        /** The plot looks exactly like the model. */
        public boolean perfect() {
            return correct == total && missing.isEmpty() && wrong.isEmpty() && extra.isEmpty();
        }

        /** How many cells are not as they should be. */
        public int problems() {
            return missing.size() + wrong.size() + extra.size();
        }

        /** Correct blocks as a whole percent of the model, but 100 only when {@link #perfect()}. */
        public int percent() {
            if (perfect()) {
                return 100;
            }
            return total == 0 ? 0 : Math.min(99, correct * 100 / total);
        }
    }

    /** Reads the plot: the block at an offset from its corner (air where there is none). */
    @FunctionalInterface
    public interface Plot {
        BlockState at(int x, int y, int z);
    }

    /** Whether the player's block shows what the model's block shows. */
    public static boolean same(BlockState model, BlockState actual) {
        if (model.getBlock() != actual.getBlock()) {
            return false;
        }
        for (Property<?> property : model.getProperties()) {
            if (COMPARED.contains(property.getName()) && !model.getValue(property).equals(actual.getValue(property))) {
                return false;
            }
        }
        return true;
    }

    /** How many of the block's item the block takes to build: two for a double slab, else one. */
    static int itemCount(BlockState state) {
        return state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
    }

    /** Compares the plot with the model. Cells are visited from the floor up, so the lists are in the order to fix them. */
    public static Result compare(StructureModel model, Plot plot) {
        List<Cell> missing = new ArrayList<>();
        List<Cell> wrong = new ArrayList<>();
        List<Cell> extra = new ArrayList<>();
        int correct = 0;
        int total = 0;
        for (int y = 0; y < model.sizeY() + EXTRA_HEIGHT; y++) {
            for (int z = 0; z < model.sizeZ(); z++) {
                for (int x = 0; x < model.sizeX(); x++) {
                    BlockState want = model.at(x, y, z);
                    BlockState have = plot.at(x, y, z);
                    if (want.isAir()) {
                        if (!have.isAir()) {
                            extra.add(new Cell(x, y, z));
                        }
                        continue;
                    }
                    total++;
                    if (have.isAir()) {
                        missing.add(new Cell(x, y, z));
                    } else if (same(want, have)) {
                        correct++;
                    } else {
                        wrong.add(new Cell(x, y, z));
                    }
                }
            }
        }
        return new Result(correct, total, missing, wrong, extra);
    }

    private StructureCompare() {
    }
}
