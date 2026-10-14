package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Build Magic tests share (lane BS): a little world of blocks kept in a map, recipes written the way a client
 * would send them, and a pocket the tests fill by hand.
 */
final class BuildTestSupport {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** A world of blocks in a map, air everywhere else; no block entities, no chunks to load. */
    static final class World implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        World set(BlockPos pos, BlockState state) {
            if (state.isAir()) {
                blocks.remove(pos);
            } else {
                blocks.put(pos.immutable(), state);
            }
            return this;
        }

        World set(int x, int y, int z, BlockState state) {
            return set(new BlockPos(x, y, z), state);
        }

        /** Every cell of the box. */
        World fill(BoundingBox box, BlockState state) {
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                set(pos, state);
            }
            return this;
        }

        /** Writes every cell the plan changes, as a job would (with no rule of the world in the way). */
        World build(SpellPlanner.Plan plan) {
            List<SpellPlanner.Cell> cells = new ArrayList<>();
            plan.walk(this).forEachRemaining(cells::add);
            for (SpellPlanner.Cell cell : cells) {
                set(cell.pos(), cell.state());
            }
            return this;
        }

        int count(BlockState state) {
            return (int) blocks.values().stream().filter(state::equals).count();
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, AIR);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }

    /** A pocket of copied cells, filled by hand; a cell never set is absent. */
    static final class Pocket implements SpellPlanner.Source {
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final BlockState[] cells;

        Pocket(int sizeX, int sizeY, int sizeZ) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.cells = new BlockState[sizeX * sizeY * sizeZ];
        }

        Pocket set(int x, int y, int z, BlockState state) {
            cells[(y * sizeZ + z) * sizeX + x] = state;
            return this;
        }

        @Override
        public int sizeX() {
            return sizeX;
        }

        @Override
        public int sizeY() {
            return sizeY;
        }

        @Override
        public int sizeZ() {
            return sizeZ;
        }

        @Override
        public @Nullable BlockState state(int x, int y, int z) {
            if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
                return null;
            }
            return cells[(y * sizeZ + z) * sizeX + x];
        }
    }

    /** A recipe, written field by field; every field starts as a client leaves it when it does not use it. */
    static final class Recipe {
        private final Spell spell;
        private SpellRecipe.Op op = SpellRecipe.Op.CAST;
        private int style;
        private int region = SpellRecipe.NO_REGION;
        private BlockPos a = BlockPos.ZERO;
        private BlockPos b = BlockPos.ZERO;
        private BlockPos anchor = BlockPos.ZERO;
        private int face = SpellRecipe.NO_FACE;
        private int size;
        private int size2;
        private int turn;
        private int flip = SpellRecipe.NO_FLIP;
        private int flags;
        private long seed;

        Recipe(Spell spell) {
            this.spell = spell;
        }

        Recipe op(SpellRecipe.Op op) {
            this.op = op;
            return this;
        }

        Recipe style(Enum<?> style) {
            this.style = style.ordinal();
            return this;
        }

        Recipe box(BlockPos a, BlockPos b) {
            this.region = SpellRecipe.BOX;
            this.a = a;
            this.b = b;
            return this;
        }

        Recipe box(BoundingBox box) {
            return box(new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
        }

        /** The region is the selected thing the test hands the planner in its setup. */
        Recipe mask() {
            this.region = SpellRecipe.MASK;
            return this;
        }

        Recipe at(BlockPos anchor, @Nullable Direction face) {
            this.anchor = anchor;
            this.face = face == null ? SpellRecipe.NO_FACE : face.get3DDataValue();
            return this;
        }

        Recipe size(int size) {
            this.size = size;
            return this;
        }

        Recipe size2(int size2) {
            this.size2 = size2;
            return this;
        }

        Recipe turn(int turn) {
            this.turn = turn;
            return this;
        }

        Recipe flip(int flip) {
            this.flip = flip;
            return this;
        }

        Recipe hollow(boolean hollow) {
            this.flags = hollow ? flags | SpellRecipe.HOLLOW : flags & ~SpellRecipe.HOLLOW;
            return this;
        }

        Recipe seed(long seed) {
            this.seed = seed;
            return this;
        }

        SpellRecipe build() {
            return new SpellRecipe(spell, op, style, region, a, b, List.of(), anchor, face, size, size2, turn, flip, flags, SpellRecipe.HAND, seed, 0);
        }
    }

    static Recipe recipe(Spell spell) {
        return new Recipe(spell);
    }

    /** Every cell the plan changes in the world, in its order. */
    static List<SpellPlanner.Cell> cells(SpellPlanner.Plan plan, BlockGetter world) {
        List<SpellPlanner.Cell> cells = new ArrayList<>();
        plan.walk(world).forEachRemaining(cells::add);
        return cells;
    }

    private BuildTestSupport() {
    }
}
