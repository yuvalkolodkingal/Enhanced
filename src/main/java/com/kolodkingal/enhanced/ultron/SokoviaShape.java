package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.config.UltronBossConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shape of the island of Sokovia, as pure functions of a {@link Spec}: the same numbers on the server (which builds
 * the island in the sky) and on every client (which draws it rising), so nothing but the Spec travels between them.
 *
 * <ul>
 *     <li><b>The rim is wavy.</b> {@code rim(theta) = R + 2.2 n1(theta) + 1.0 n2(theta)}, kept within {@code R - 3 ..
 *     R + 3}, with {@code n1} and {@code n2} periodic value noises of 3 and 7 lobes seeded by the fight. A column
 *     {@code (dx, dz)} (blocks from the middle) is in the island if it is not farther than the rim at its angle.</li>
 *     <li><b>The cone is rock that is made up</b>, not copied: under the skin of every column, a paraboloid
 *     ({@code max(2, round(cone x (1 - d^2)))} blocks deep, with {@code d} the distance over the rim) of stone,
 *     andesite, cobblestone, tuff, coarse dirt, mossy cobblestone and, low down, deepslate, chosen by a hash of the
 *     seed and the cell. Never a block that falls, a fluid or a block entity: the rubble underside of the film's
 *     city, and nothing of the real ground under the skin (caves, ores and lava stay where they are).</li>
 *     <li><b>The move</b> is an ease: {@code offset(t) = H x smootherstep(t / ticks)}, a function of the time since the
 *     move began, so the server's carried players and the clients' picture of the island agree to the tick.</li>
 * </ul>
 */
public final class SokoviaShape {
    /** How far the rim may wander from the nominal radius, either way. */
    public static final int RIM_SLACK = 3;

    /** Everything that makes the island, which the server and the clients share. */
    public record Spec(int radius, int height, int skin, int cover, int cone, long seed) {
        /** The numbers of this server's config, for a fight with this seed. */
        public static Spec ofConfig(long seed) {
            return new Spec(UltronBossConfig.ISLAND_RADIUS.get(), UltronBossConfig.LIFT_HEIGHT.get(), UltronBossConfig.SKIN_DEPTH.get(),
                    UltronBossConfig.COVER_HEIGHT.get(), UltronBossConfig.CONE_DEPTH.get(), seed);
        }

        /** The farthest any column of the island can be from its middle. */
        public int extent() {
            return radius + RIM_SLACK;
        }
    }

    /** The radius of the rim at this angle (radians). */
    public static double rim(Spec spec, double theta) {
        double angle = theta % Mth.TWO_PI;
        if (angle < 0.0) {
            angle += Mth.TWO_PI;
        }
        double rim = spec.radius + 2.2 * noise(spec.seed, 3, angle) + 1.0 * noise(spec.seed, 7, angle);
        return Mth.clamp(rim, spec.radius - RIM_SLACK, spec.radius + RIM_SLACK);
    }

    /** Whether the column {@code dx, dz} blocks from the middle is in the island. */
    public static boolean inside(Spec spec, int dx, int dz) {
        int d2 = dx * dx + dz * dz;
        if (d2 == 0) {
            return true;
        }
        if (d2 > spec.extent() * spec.extent()) {
            return false;
        }
        double rim = rim(spec, Math.atan2(dz, dx));
        return d2 <= rim * rim;
    }

    /** How many blocks of cone hang under the skin of this column (0 for a column outside the island). */
    public static int coneDepth(Spec spec, int dx, int dz) {
        if (!inside(spec, dx, dz)) {
            return 0;
        }
        double rim = Math.max(1.0, rim(spec, Math.atan2(dz, dx)));
        double d = Math.min(1.0, Math.sqrt(dx * dx + dz * dz) / rim);
        return Math.max(2, (int) Math.round(spec.cone * (1.0 - d * d)));
    }

    /**
     * The rock of the cone at depth {@code k} (0 is just under the skin) of a column {@code depth} blocks deep. It is
     * chosen by a hash of the seed and the cell: 50 percent stone, 14 andesite, 10 cobblestone, 8 tuff, 6 coarse dirt,
     * 5 mossy cobblestone and 7 deepslate (only in the lowest 40 percent of the column, stone elsewhere).
     */
    public static BlockState coneBlock(Spec spec, int dx, int dz, int k, int depth) {
        int roll = (int) Long.remainderUnsigned(hash(spec.seed, dx, k, dz) >>> 1, 100);
        if (roll < 50) {
            return Blocks.STONE.defaultBlockState();
        } else if (roll < 64) {
            return Blocks.ANDESITE.defaultBlockState();
        } else if (roll < 74) {
            return Blocks.COBBLESTONE.defaultBlockState();
        } else if (roll < 82) {
            return Blocks.TUFF.defaultBlockState();
        } else if (roll < 88) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        } else if (roll < 93) {
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        return k >= depth * 0.6 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    /** Whether the state is one of the cone's rocks (what the island is made of when it is not the real ground). */
    public static boolean isConeRock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.ANDESITE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.TUFF)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.DEEPSLATE);
    }

    // ---- the move ----

    /** The smoother step: {@code 6u^5 - 15u^4 + 10u^3}, 0 at 0 and 1 at 1, with no speed at either end. */
    public static double smoother(double u) {
        double x = Mth.clamp(u, 0.0, 1.0);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    /**
     * How far up the island is {@code t} ticks into a move of {@code ticks} ticks that takes it up {@code height}
     * blocks.
     */
    public static double offset(int height, int ticks, double t) {
        return height * smoother(t / Math.max(1, ticks));
    }

    // ---- noise ----

    /** A periodic value noise in -1..1 with this many lobes round the circle, at an angle in 0..2 pi. */
    private static double noise(long seed, int lobes, double angle) {
        double x = angle / Mth.TWO_PI * lobes;
        int i = (int) Math.floor(x);
        double f = x - i;
        double a = lattice(seed, lobes, Math.floorMod(i, lobes));
        double b = lattice(seed, lobes, Math.floorMod(i + 1, lobes));
        double s = f * f * (3.0 - 2.0 * f);
        return a + (b - a) * s;
    }

    private static double lattice(long seed, int lobes, int index) {
        long h = hash(seed, lobes, index, 0x5EED);
        return ((h >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    /** A 64-bit mix of a seed and three numbers (SplitMix64): the same on every machine. */
    static long hash(long seed, int a, int b, int c) {
        long h = mix(seed ^ 0x9E3779B97F4A7C15L);
        h = mix(h + a);
        h = mix(h + b * 0x632BE59BD9B4E019L);
        return mix(h + c);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private SokoviaShape() {
    }
}
