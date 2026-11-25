package com.kolodkingal.enhanced.nexus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * (3.19.2) The island of the Nexus ({@link Nexus}), built once: a disc of black stone 33 blocks across hanging in the void,
 * its underside a rough cone of obsidian and crying obsidian; tiles of purple light in its floor and a great amethyst
 * crystal at its heart. All round it, instead of lamps, floating orbs of energy (a light at the heart of a ball of purple,
 * blue and magenta glass) and floating crystals of amethyst on shards of rock, and a few small islets further out.
 * Built with the same seed every time.
 */
public final class NexusBuilder {
    /** Raised when the island changes, so an old Nexus is built again. */
    public static final int VERSION = 1;
    public static final int RADIUS = 16;

    private static final Block[] GLASS = {Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS,
            Blocks.LIGHT_BLUE_STAINED_GLASS};

    private NexusBuilder() {
    }

    public static void build(ServerLevel level) {
        RandomSource random = RandomSource.create(0x4E45585553L);
        int g = Nexus.GROUND;
        island(level, random, 0, g, 0, RADIUS, true);
        // the heart: a great crystal of amethyst on a dais of crying obsidian
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(level, x, g, z, Blocks.CRYING_OBSIDIAN);
            }
        }
        for (int y = 1; y <= 4; y++) {
            set(level, 0, g + y, 0, Blocks.AMETHYST_BLOCK);
        }
        set(level, 0, g + 5, 0, Blocks.AMETHYST_CLUSTER);
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            set(level, d[0], g + 1, d[1], Blocks.AMETHYST_BLOCK);
            set(level, d[0], g + 2, d[1], Blocks.LARGE_AMETHYST_BUD);
        }
        set(level, 0, g - 1, 0, Blocks.SEA_LANTERN);
        // tiles of purple light in a ring round the heart
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            int x = Mth.floor(Math.cos(a) * 9.5);
            int z = Mth.floor(Math.sin(a) * 9.5);
            set(level, x, g, z, Blocks.PURPLE_STAINED_GLASS);
            set(level, x, g - 1, z, Blocks.SEA_LANTERN);
        }
        // floating orbs of energy
        for (int i = 0; i < 14; i++) {
            double a = Math.PI * 2 * i / 14 + random.nextDouble() * 0.3;
            double r = 20 + random.nextDouble() * 26;
            int x = Mth.floor(Math.cos(a) * r);
            int z = Mth.floor(Math.sin(a) * r);
            int y = g - 6 + random.nextInt(34);
            orb(level, x, y, z, GLASS[i % GLASS.length], i % 3 == 0 ? Blocks.SHROOMLIGHT : Blocks.SEA_LANTERN);
        }
        // floating crystals
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12 + 0.26 + random.nextDouble() * 0.3;
            double r = 22 + random.nextDouble() * 34;
            int x = Mth.floor(Math.cos(a) * r);
            int z = Mth.floor(Math.sin(a) * r);
            int y = g - 12 + random.nextInt(36);
            crystal(level, random, x, y, z);
        }
        // a few small islets further out
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.5;
            double r = 60 + random.nextDouble() * 20;
            int x = Mth.floor(Math.cos(a) * r);
            int z = Mth.floor(Math.sin(a) * r);
            int y = g - 10 + random.nextInt(24);
            island(level, random, x, y, z, 3 + random.nextInt(4), false);
            set(level, x, y + 1, z, Blocks.AMETHYST_BLOCK);
            set(level, x, y + 2, z, Blocks.AMETHYST_CLUSTER);
            set(level, x, y - 1, z, Blocks.SEA_LANTERN);
        }
    }

    /** A floating disc of black stone, its underside a rough cone. */
    static void island(ServerLevel level, RandomSource random, int cx, int top, int cz, int radius, boolean main) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > radius + 0.3) {
                    continue;
                }
                Block floor;
                if (d > radius - 1.2) {
                    floor = Blocks.OBSIDIAN;
                } else if (main && d >= 5.5 && d < 6.5) {
                    floor = Blocks.CRYING_OBSIDIAN;
                } else {
                    int pick = random.nextInt(10);
                    floor = pick < 5 ? Blocks.POLISHED_BLACKSTONE_BRICKS : pick < 8 ? Blocks.POLISHED_BLACKSTONE
                            : pick < 9 ? Blocks.GILDED_BLACKSTONE : Blocks.DEEPSLATE_TILES;
                }
                set(level, cx + x, top, cz + z, floor);
                int depth = (int) Math.round((radius - d) * 0.85) + random.nextInt(3);
                for (int y = 1; y <= depth; y++) {
                    int pick = random.nextInt(12);
                    Block under = pick == 0 ? Blocks.CRYING_OBSIDIAN : pick < 5 ? Blocks.OBSIDIAN : Blocks.BLACKSTONE;
                    set(level, cx + x, top - y, cz + z, under);
                }
            }
        }
    }

    /** A floating orb of energy: a light at the heart of a ball of glass. */
    static void orb(ServerLevel level, int cx, int cy, int cz, Block glass, Block light) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d <= 0.5) {
                        set(level, cx, cy, cz, light);
                    } else if (d <= 1.8) {
                        set(level, cx + x, cy + y, cz + z, d <= 1.0 ? light : glass);
                    }
                }
            }
        }
    }

    /** A floating crystal: a spike of amethyst on a shard of rock, crowned with a cluster. */
    static void crystal(ServerLevel level, RandomSource random, int cx, int cy, int cz) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(level, cx + x, cy, cz + z, Math.abs(x) + Math.abs(z) == 2 ? Blocks.BLACKSTONE : Blocks.CRYING_OBSIDIAN);
            }
        }
        set(level, cx, cy - 1, cz, Blocks.OBSIDIAN);
        set(level, cx, cy - 2, cz, Blocks.BLACKSTONE);
        int height = 3 + random.nextInt(4);
        for (int y = 1; y <= height; y++) {
            set(level, cx, cy + y, cz, y == 2 ? Blocks.SEA_LANTERN : Blocks.AMETHYST_BLOCK);
        }
        set(level, cx, cy + height + 1, cz, Blocks.AMETHYST_CLUSTER);
        set(level, cx + 1, cy + 1, cz, Blocks.MEDIUM_AMETHYST_BUD);
        set(level, cx - 1, cy + 1, cz, Blocks.SMALL_AMETHYST_BUD);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
