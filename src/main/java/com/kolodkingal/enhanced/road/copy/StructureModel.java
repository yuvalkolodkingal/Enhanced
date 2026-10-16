package com.kolodkingal.enhanced.road.copy;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A structure the player is shown and has to copy: a box of block states, {@code x} east, {@code y} up, {@code z} south (its
 * front, the side the player looks at, is the greatest {@code z}). Plain data with no world: the library makes it from a seed
 * ({@link StructureLibrary}), the comparison reads it ({@link StructureCompare}), and the supply is worked out from it
 * ({@link StructureSupply}). An empty cell is air.
 */
public final class StructureModel {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final Direction[] FACES = Direction.values();

    private final String theme;
    private final int tier;
    private final int sx;
    private final int sy;
    private final int sz;
    private final BlockState[] cells;

    StructureModel(String theme, int tier, int sx, int sy, int sz, BlockState[] cells) {
        this.theme = theme;
        this.tier = tier;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.cells = cells.clone();
    }

    /** Which model of the library it is ({@code hut}, {@code tower}, ...). */
    public String theme() {
        return theme;
    }

    /** 1 (small) to 3 (large). */
    public int tier() {
        return tier;
    }

    public int sizeX() {
        return sx;
    }

    public int sizeY() {
        return sy;
    }

    public int sizeZ() {
        return sz;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz;
    }

    /** The block at a cell; air outside the box. */
    public BlockState at(int x, int y, int z) {
        return inside(x, y, z) ? cells[(y * sz + z) * sx + x] : AIR;
    }

    /** How many cells hold a block. */
    public int solidCount() {
        int n = 0;
        for (BlockState state : cells) {
            if (!state.isAir()) {
                n++;
            }
        }
        return n;
    }

    /**
     * The items the copy takes, most needed first: a block is one item, a double slab is two. Items that are not placed as
     * themselves (a block without an item) cannot be built by hand: {@link #problems()} says so.
     */
    public Map<Item, Integer> itemsNeeded() {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        List<Map.Entry<Item, Integer>> sorted = new ArrayList<>();
        Map<Item, Integer> raw = new LinkedHashMap<>();
        for (BlockState state : cells) {
            if (!state.isAir()) {
                raw.merge(state.getBlock().asItem(), StructureCompare.itemCount(state), Integer::sum);
            }
        }
        sorted.addAll(raw.entrySet());
        sorted.sort(Comparator.<Map.Entry<Item, Integer>>comparingInt(e -> -e.getValue())
                .thenComparing(e -> BuiltInRegistries.ITEM.getKey(e.getKey()).toString()));
        for (Map.Entry<Item, Integer> entry : sorted) {
            counts.put(entry.getKey(), entry.getValue());
        }
        return counts;
    }

    /**
     * What is wrong with the model as something to build by hand (an empty list for every model of the library): a block that
     * has no item, a block that floats (it touches nothing that stands on the floor, so no block could be put against it),
     * a lantern with nothing to stand on or hang from.
     */
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        if (solidCount() == 0) {
            problems.add("empty");
        }
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    BlockState state = at(x, y, z);
                    if (state.isAir()) {
                        continue;
                    }
                    Item item = state.getBlock().asItem();
                    if (!(item instanceof BlockItem blockItem) || blockItem.getBlock() != state.getBlock()) {
                        problems.add("no item for " + state.getBlock() + " at " + x + "," + y + "," + z);
                    }
                    if (state.getBlock() instanceof LanternBlock) {
                        boolean hanging = state.getValue(LanternBlock.HANGING);
                        BlockState hold = at(x, y + (hanging ? 1 : -1), z);
                        if (hold.isAir() || hold.getBlock() instanceof LanternBlock) {
                            problems.add("a lantern with nothing to " + (hanging ? "hang from" : "stand on") + " at " + x + "," + y + "," + z);
                        }
                    }
                }
            }
        }
        for (int i : unreachable()) {
            problems.add("floating block at " + (i % sx) + "," + (i / (sx * sz)) + "," + (i / sx % sz));
        }
        return problems;
    }

    /** The cells (as indexes) that no chain of touching blocks joins to the floor: each block of the model can be placed against another. */
    private List<Integer> unreachable() {
        boolean[] seen = new boolean[cells.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int z = 0; z < sz; z++) {
            for (int x = 0; x < sx; x++) {
                int i = z * sx + x;
                if (!cells[i].isAir()) {
                    seen[i] = true;
                    queue.add(i);
                }
            }
        }
        while (!queue.isEmpty()) {
            int i = queue.poll();
            int x = i % sx;
            int z = i / sx % sz;
            int y = i / (sx * sz);
            for (Direction face : FACES) {
                int nx = x + face.getStepX();
                int ny = y + face.getStepY();
                int nz = z + face.getStepZ();
                if (inside(nx, ny, nz)) {
                    int n = (ny * sz + nz) * sx + nx;
                    if (!seen[n] && !cells[n].isAir()) {
                        seen[n] = true;
                        queue.add(n);
                    }
                }
            }
        }
        List<Integer> lost = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            if (!cells[i].isAir() && !seen[i]) {
                lost.add(i);
            }
        }
        return lost;
    }

    /** The block that stands for an empty cell in the comparison. */
    static BlockState air() {
        return AIR;
    }
}
