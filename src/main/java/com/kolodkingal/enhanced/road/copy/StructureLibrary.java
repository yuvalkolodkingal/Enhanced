package com.kolodkingal.enhanced.road.copy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;
import java.util.SplittableRandom;

/**
 * The models the Copy the Structure trial shows, made from a seed: five themes (a cottage, a tower, a bridge arch, a temple, a
 * witch's cauldron altar), each in three sizes. Tier 1 fits in 5x5x5, tier 2 in 9x9x7 and tier 3 in 11x11x9, and the bigger ones
 * take more kinds of block and more patience. The seed chooses the theme and everything that can vary (the woods, the stones, the
 * colours, which stones are mossy). Pure code: the same seed and tier give the same model on every machine.
 *
 * <p>Rules every model keeps, which {@link StructureModel#problems()} and the tests check: every block can be put down by hand
 * (it has its own item), every block touches another one on the way to the floor (so a block can always be placed against
 * something), a lantern always has something to stand on or hang from, no more than {@link #MAX_KINDS} kinds of block (they fit
 * the hotbar and one chest), and no stairs meet at a corner (the game rounds such stairs off by itself, which would make two
 * different ways of laying them look the same).
 */
public final class StructureLibrary {
    /** The themes, in the order the seed picks from. */
    public static final List<String> THEMES = List.of("hut", "tower", "bridge", "temple", "altar");
    /** The most kinds of block (items) one model uses. */
    public static final int MAX_KINDS = 9;

    /** A wood: its planks, log, stairs, slab and fence. */
    private record Wood(Block planks, Block log, Block stairs, Block slab, Block fence) {
    }

    /** A stone: its block, stairs, slab and wall. */
    private record Stone(Block block, Block stairs, Block slab, Block wall) {
    }

    /** What a temple is built of: floor, pillar, altar block, slab. */
    private record Order(Block floor, Block pillar, Block altar, Block slab) {
    }

    private static final Wood[] WOODS = {
            new Wood(Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.OAK_FENCE),
            new Wood(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE),
            new Wood(Blocks.BIRCH_PLANKS, Blocks.BIRCH_LOG, Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB, Blocks.BIRCH_FENCE),
            new Wood(Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_FENCE),
            new Wood(Blocks.ACACIA_PLANKS, Blocks.ACACIA_LOG, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB, Blocks.ACACIA_FENCE)};
    private static final Stone[] STONES = {
            new Stone(Blocks.COBBLESTONE, Blocks.COBBLESTONE_STAIRS, Blocks.COBBLESTONE_SLAB, Blocks.COBBLESTONE_WALL),
            new Stone(Blocks.STONE_BRICKS, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICK_WALL),
            new Stone(Blocks.BRICKS, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, Blocks.BRICK_WALL),
            new Stone(Blocks.SANDSTONE, Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE_SLAB, Blocks.SANDSTONE_WALL),
            new Stone(Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_WALL)};
    private static final Block[] COLOURS = {Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.PURPLE_WOOL,
            Blocks.ORANGE_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.MAGENTA_WOOL, Blocks.WHITE_WOOL};
    private static final Order[] ORDERS = {
            new Order(Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_SLAB),
            new Order(Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_BASALT, Blocks.CHISELED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE_SLAB),
            new Order(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK, Blocks.PURPUR_SLAB)};

    /** The size a tier fits in: width (x), depth (z), height (y). */
    public static int[] box(int tier) {
        return switch (clampTier(tier)) {
            case 1 -> new int[]{5, 5, 5};
            case 2 -> new int[]{9, 9, 7};
            default -> new int[]{11, 11, 9};
        };
    }

    public static int clampTier(int tier) {
        return Math.max(1, Math.min(3, tier));
    }

    /** The model for a seed at a tier (the seed chooses the theme). */
    public static StructureModel generate(long seed, int tier) {
        SplittableRandom random = new SplittableRandom(seed);
        return generate(seed, tier, random.nextInt(THEMES.size()));
    }

    /** The model of a given theme (an index into {@link #THEMES}) for a seed at a tier. */
    public static StructureModel generate(long seed, int tier, int theme) {
        tier = clampTier(tier);
        SplittableRandom random = new SplittableRandom(seed * 31 + theme + 1);
        Grid grid = switch (Math.floorMod(theme, THEMES.size())) {
            case 0 -> hut(tier, random);
            case 1 -> tower(tier, random);
            case 2 -> bridge(tier, random);
            case 3 -> temple(tier, random);
            default -> altar(tier, random);
        };
        return grid.finish(THEMES.get(Math.floorMod(theme, THEMES.size())), tier);
    }

    // ------------------------------------------------------------------------------------------------------------
    // A box of blocks to draw in

    private static final class Grid {
        final int sx;
        final int sy;
        final int sz;
        final BlockState[] cells;

        Grid(int sx, int sy, int sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.cells = new BlockState[sx * sy * sz];
            java.util.Arrays.fill(cells, StructureModel.air());
        }

        void set(int x, int y, int z, BlockState state) {
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) {
                throw new IllegalArgumentException("Outside the box: " + x + "," + y + "," + z);
            }
            cells[(y * sz + z) * sx + x] = state;
        }

        void set(int x, int y, int z, Block block) {
            set(x, y, z, block.defaultBlockState());
        }

        BlockState get(int x, int y, int z) {
            return cells[(y * sz + z) * sx + x];
        }

        void clear(int x, int y, int z) {
            set(x, y, z, StructureModel.air());
        }

        void box(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    for (int x = x1; x <= x2; x++) {
                        set(x, y, z, block);
                    }
                }
            }
        }

        /** Cuts the box down to the cells that hold something, so that the model is exactly as big as it looks. */
        StructureModel finish(String theme, int tier) {
            int minX = sx;
            int minY = sy;
            int minZ = sz;
            int maxX = -1;
            int maxY = -1;
            int maxZ = -1;
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    for (int x = 0; x < sx; x++) {
                        if (!get(x, y, z).isAir()) {
                            minX = Math.min(minX, x);
                            minY = Math.min(minY, y);
                            minZ = Math.min(minZ, z);
                            maxX = Math.max(maxX, x);
                            maxY = Math.max(maxY, y);
                            maxZ = Math.max(maxZ, z);
                        }
                    }
                }
            }
            if (maxX < 0) {
                throw new IllegalStateException("An empty model: " + theme);
            }
            // The floor level stays where it is: the model sits on the ground, whatever is at the bottom.
            int ny = maxY + 1;
            int nx = maxX - minX + 1;
            int nz = maxZ - minZ + 1;
            BlockState[] out = new BlockState[nx * ny * nz];
            for (int y = 0; y < ny; y++) {
                for (int z = 0; z < nz; z++) {
                    for (int x = 0; x < nx; x++) {
                        out[(y * nz + z) * nx + x] = get(minX + x, y, minZ + z);
                    }
                }
            }
            return new StructureModel(theme, tier, nx, ny, nz, out);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Pieces

    private static BlockState stairs(Block block, Direction facing, boolean top) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState slab(Block block, SlabType type) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    private static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState lantern(boolean hanging) {
        return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging);
    }

    private static <T> T pick(T[] all, SplittableRandom random) {
        return all[random.nextInt(all.length)];
    }

    /** A yes/no for a cell that depends only on the salt and the cell, so that a pattern (moss, say) is the same however often it is asked. */
    private static boolean sprinkle(long salt, int x, int y, int z, int percent) {
        long h = salt ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return Math.floorMod(h >>> 20, 100L) < percent;
    }

    // ------------------------------------------------------------------------------------------------------------
    // 1. The cottage

    private static Grid hut(int tier, SplittableRandom random) {
        int[] box = box(tier);
        int w = tier == 1 ? 5 : tier == 2 ? 7 : 9;
        int d = tier == 1 ? 5 : tier == 2 ? 9 : 11;
        int half = (w - 1) / 2;
        int wallH = box[2] - 1 - half;
        Wood walls = pick(WOODS, random);
        Wood roof = pick(WOODS, random);
        Stone plinth = pick(STONES, random);
        boolean chimney = tier > 1;
        Grid g = new Grid(w + 1, box[2], d);
        int door = w / 2;
        for (int y = 0; y < wallH; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean edge = x == 0 || x == w - 1 || z == 0 || z == d - 1;
                    if (!edge) {
                        continue;
                    }
                    boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                    if (corner) {
                        g.set(x, y, z, log(walls.log, Direction.Axis.Y));
                    } else if (y == 0 && tier > 1) {
                        g.set(x, y, z, plinth.block);
                    } else {
                        g.set(x, y, z, walls.planks);
                    }
                }
            }
        }
        // The door, and windows: one block of glass in the side walls (and in the front and back from tier 2).
        g.clear(door, 0, d - 1);
        g.clear(door, 1, d - 1);
        int windowY = 1;
        for (int z = 2; z < d - 2; z += 4) {
            g.set(0, windowY, z, Blocks.GLASS);
            g.set(w - 1, windowY, z, Blocks.GLASS);
        }
        if (tier > 1) {
            g.set(door - 2, windowY, d - 1, Blocks.GLASS);
            g.set(door + 2, windowY, d - 1, Blocks.GLASS);
            g.set(door, windowY, 0, Blocks.GLASS);
        }
        // The roof: stairs rising to the ridge along the length, the gable ends filled in.
        for (int k = 0; k < half; k++) {
            for (int z = 0; z < d; z++) {
                g.set(k, wallH + k, z, stairs(roof.stairs, Direction.EAST, false));
                g.set(w - 1 - k, wallH + k, z, stairs(roof.stairs, Direction.WEST, false));
            }
            for (int x = k + 1; x <= w - 2 - k; x++) {
                g.set(x, wallH + k, 0, roof.planks);
                g.set(x, wallH + k, d - 1, roof.planks);
            }
        }
        for (int z = 0; z < d; z++) {
            g.set(half, wallH + half, z, roof.planks);
        }
        if (chimney) {
            for (int y = 0; y <= wallH + 1; y++) {
                g.set(w, y, 1, plinth.block);
            }
        }
        return g;
    }

    // ------------------------------------------------------------------------------------------------------------
    // 2. The tower

    private static Grid tower(int tier, SplittableRandom random) {
        int s = tier == 1 ? 5 : tier == 2 ? 7 : 9;
        int h = box(tier)[2];
        Stone wall = pick(STONES, random);
        Stone base;
        do {
            base = pick(STONES, random);
        } while (base == wall);
        Wood floor = pick(WOODS, random);
        Block flag = pick(COLOURS, random);
        long salt = random.nextLong();
        int rows = tier == 1 ? h - 1 : h - 2;
        Grid g = new Grid(s, h, s);
        int c = s / 2;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < s; x++) {
                for (int z = 0; z < s; z++) {
                    if (x != 0 && x != s - 1 && z != 0 && z != s - 1) {
                        continue;
                    }
                    Block block = y == 0 ? base.block : wall.block;
                    if (y > 0 && tier > 1 && sprinkle(salt, x, y, z, 18)) {
                        block = Blocks.MOSSY_STONE_BRICKS;
                    }
                    g.set(x, y, z, block);
                }
            }
        }
        // The parapet on top: every other block of the rim.
        for (int x = 0; x < s; x++) {
            for (int z = 0; z < s; z++) {
                if ((x == 0 || x == s - 1 || z == 0 || z == s - 1) && (x + z) % 2 == 0) {
                    g.set(x, rows, z, wall.block);
                }
            }
        }
        // The door and the slits of glass.
        g.clear(c, 0, s - 1);
        g.clear(c, 1, s - 1);
        g.set(0, 2, c, Blocks.GLASS);
        g.set(s - 1, 2, c, Blocks.GLASS);
        // The roof walk: planks inside the rim.
        for (int x = 1; x < s - 1; x++) {
            for (int z = 1; z < s - 1; z++) {
                g.set(x, rows - 1, z, floor.planks);
            }
        }
        if (tier == 1) {
            g.set(c, rows, c, lantern(false));
        } else {
            g.set(c, rows - 2, 0, Blocks.GLASS);
            g.set(c, 3, s - 1, Blocks.GLASS);
            // A ring of slabs lying upside down under the parapet, a flagpole in the middle.
            for (int x = 0; x < s; x++) {
                for (int z = 0; z < s; z++) {
                    if (x == 0 || x == s - 1 || z == 0 || z == s - 1) {
                        g.set(x, rows - 1, z, slab(wall.slab, SlabType.TOP));
                    }
                }
            }
            g.set(c, rows, c, floor.fence);
            g.set(c, rows + 1, c, floor.fence);
            g.set(c + 1, rows + 1, c, flag);
            if (tier == 3) {
                g.set(c + 2, rows + 1, c, flag);
            }
        }
        return g;
    }

    // ------------------------------------------------------------------------------------------------------------
    // 3. The bridge arch

    private static Grid bridge(int tier, SplittableRandom random) {
        int length = tier == 1 ? 5 : tier == 2 ? 9 : 11;
        int width = tier == 1 ? 5 : tier == 2 ? 7 : 9;
        int rise = tier == 1 ? 2 : tier == 2 ? 3 : 5;
        Stone under = random.nextBoolean() ? STONES[1] : STONES[0];
        Wood deck = pick(WOODS, random);
        Stone rail = pick(STONES, random);
        boolean woodRamps = random.nextBoolean();
        long salt = random.nextLong();
        Grid g = new Grid(length, rise + 3, width);
        for (int x = 0; x < length; x++) {
            int top = Math.min(Math.min(x, length - 1 - x), rise);
            boolean flat = top == rise;
            for (int z = 0; z < width; z++) {
                for (int y = 0; y < top; y++) {
                    boolean tunnel = flat && y < rise - 1;
                    if (!tunnel) {
                        boolean mossy = tier > 1 && y < top - 1 && sprinkle(salt, x, y, z, 15);
                        g.set(x, y, z, mossy ? Blocks.MOSSY_STONE_BRICKS : under.block);
                    }
                }
                if (flat) {
                    g.set(x, top, z, deck.planks);
                } else {
                    Block stairs = woodRamps ? deck.stairs : under.stairs;
                    g.set(x, top, z, stairs(stairs, x < length / 2 ? Direction.EAST : Direction.WEST, false));
                }
            }
            for (int z : new int[]{0, width - 1}) {
                g.set(x, top + 1, z, rail.wall);
            }
        }
        if (tier > 1) {
            for (int x : new int[]{0, length - 1}) {
                for (int z : new int[]{0, width - 1}) {
                    g.set(x, 2, z, lantern(false));
                }
            }
        }
        return g;
    }

    // ------------------------------------------------------------------------------------------------------------
    // 4. The temple

    private static Grid temple(int tier, SplittableRandom random) {
        int s = tier == 1 ? 5 : tier == 2 ? 9 : 11;
        int colH = tier == 1 ? 2 : tier == 2 ? 4 : 5;
        int inset = tier == 1 ? 0 : 1;
        int h = tier == 1 ? 4 : tier == 2 ? 7 : 9;
        Order order = pick(ORDERS, random);
        Block a = pick(COLOURS, random);
        Block b;
        do {
            b = pick(COLOURS, random);
        } while (b == a);
        Grid g = new Grid(s, h, s);
        int c = s / 2;
        // The floor: rings of two colours round the middle.
        for (int x = 0; x < s; x++) {
            for (int z = 0; z < s; z++) {
                int ring = Math.abs(x - c) + Math.abs(z - c);
                g.set(x, 0, z, ring % 4 == 0 ? a : ring % 4 == 2 ? b : order.floor);
            }
        }
        // Columns: round the edge of the colonnade.
        int last = s - 1 - inset;
        int step = tier == 3 ? 2 : (last - inset) / 2;
        for (int x = inset; x <= last; x += step) {
            for (int z = inset; z <= last; z += step) {
                if (x != inset && x != last && z != inset && z != last) {
                    continue;
                }
                for (int y = 1; y <= colH; y++) {
                    g.set(x, y, z, log(order.pillar, Direction.Axis.Y));
                }
            }
        }
        // The frieze: a ring of the two colours joining the columns (tier 2 and up), then the roof of slabs with an open eye in the middle.
        int roofY = colH + 1;
        if (tier > 1) {
            for (int x = inset; x <= last; x++) {
                for (int z = inset; z <= last; z++) {
                    if (x == inset || x == last || z == inset || z == last) {
                        g.set(x, colH + 1, z, (x + z) % 2 == 0 ? a : b);
                    }
                }
            }
            roofY = colH + 2;
        }
        for (int x = inset; x <= last; x++) {
            for (int z = inset; z <= last; z++) {
                if (x == c && z == c) {
                    continue;
                }
                g.set(x, roofY, z, slab(order.slab, SlabType.BOTTOM));
            }
        }
        if (tier == 3) {
            for (int x = c - 1; x <= c + 1; x++) {
                for (int z = c - 1; z <= c + 1; z++) {
                    if (x != c || z != c) {
                        g.set(x, roofY + 1, z, a);
                    }
                }
            }
        }
        // The altar in the middle, a lantern on top.
        g.set(c, 1, c, order.altar);
        if (tier > 1) {
            g.set(c, 2, c, b);
            g.set(c, 3, c, lantern(false));
        } else {
            g.set(c, 2, c, lantern(false));
        }
        return g;
    }

    // ------------------------------------------------------------------------------------------------------------
    // 5. The witch's cauldron altar

    private static Grid altar(int tier, SplittableRandom random) {
        int s = tier == 1 ? 5 : tier == 2 ? 9 : 11;
        int h = box(tier)[2];
        int c = s / 2;
        Grid g = new Grid(s, h, s);
        boolean heat = random.nextInt(4) != 0;
        long salt = random.nextLong();
        for (int x = 0; x < s; x++) {
            for (int z = 0; z < s; z++) {
                int ring = Math.max(Math.abs(x - c), Math.abs(z - c));
                Block block;
                if (ring == 0) {
                    block = heat ? Blocks.MAGMA_BLOCK : Blocks.STONE_BRICKS;
                } else if (tier > 1 && ring == 2) {
                    block = Blocks.DEEPSLATE_BRICKS;
                } else if (sprinkle(salt, x, 0, z, 35)) {
                    block = Blocks.MOSSY_COBBLESTONE;
                } else {
                    block = Blocks.STONE_BRICKS;
                }
                g.set(x, 0, z, block);
            }
        }
        g.set(c, 1, c, Blocks.CAULDRON);
        // The steps up to the circle, on the front edge, rising away from the player.
        int stepsFrom = tier == 1 ? 1 : 3;
        int stepsTo = s - 1 - stepsFrom;
        for (int x = stepsFrom; x <= stepsTo; x++) {
            g.set(x, 0, s - 1, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
        }
        // Two carved pumpkins, looking out from the front corners.
        BlockState pumpkin = Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH);
        g.set(0, 1, s - 1, pumpkin);
        g.set(s - 1, 1, s - 1, pumpkin);
        // The frame: posts with logs across the top, soul lanterns hanging from them.
        if (tier == 1) {
            frame(g, 4, 3, List.of(1, 2, 3));
        } else {
            int post = tier == 2 ? 5 : 7;
            int lo = 1;
            int hi = s - 2;
            int mid = c;
            List<int[]> posts = new java.util.ArrayList<>(List.of(new int[]{lo, lo}, new int[]{hi, lo}, new int[]{lo, hi}, new int[]{hi, hi}));
            if (tier == 3) {
                posts.addAll(List.of(new int[]{mid, lo}, new int[]{mid, hi}, new int[]{lo, mid}, new int[]{hi, mid}));
            }
            for (int[] p : posts) {
                for (int y = 1; y <= post; y++) {
                    g.set(p[0], y, p[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
                }
            }
            int beamY = post + 1;
            for (int x = lo; x <= hi; x++) {
                g.set(x, beamY, lo, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
                g.set(x, beamY, hi, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
            }
            for (int z = lo + 1; z < hi; z++) {
                g.set(lo, beamY, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
                g.set(hi, beamY, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
            }
            int[] hang = tier == 2 ? new int[]{mid} : new int[]{lo + 2, hi - 2};
            for (int at : hang) {
                g.set(at, post, lo, lantern(true));
                g.set(at, post, hi, lantern(true));
                g.set(lo, post, at, lantern(true));
                g.set(hi, post, at, lantern(true));
            }
        }
        return g;
    }

    /** A small gallows for the tiny altar: two posts at the back with a log across and lanterns hanging from it. */
    private static void frame(Grid g, int beamY, int postTop, List<Integer> lanterns) {
        int last = g.sx - 1;
        for (int y = 1; y <= postTop; y++) {
            g.set(0, y, 0, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            g.set(last, y, 0, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        for (int x = 0; x <= last; x++) {
            g.set(x, beamY, 0, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        }
        for (int x : lanterns) {
            g.set(x, postTop, 0, lantern(true));
        }
    }

    private StructureLibrary() {
    }
}
