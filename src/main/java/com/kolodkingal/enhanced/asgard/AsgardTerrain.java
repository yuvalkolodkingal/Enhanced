package com.kolodkingal.enhanced.asgard;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

/**
 * The land of Asgard (3.5): pure functions of a column (x, z), the same in every world (a fixed noise seed, never the
 * world's). A floating disk over the void: the land (radius {@link #edgeRadius} by direction: 235 blocks, more to the
 * south-west and the south-east where the houses, the stables and the barracks are), its districts levelled into pads
 * ({@link CityPlan#padAt}), cliffs down to the sea, the sea out to the stone lip at radius 377-381 that holds it, the cosmic
 * falls hanging off the lip, and the underside: an inverted, jagged cone from y 40 at the rim to y -48 under the centre.
 * A chasm west of the palace goes right through the land (the gardens' waters fall into it).
 */
public final class AsgardTerrain {
    /** The fixed seed of Asgard's noise ("ASGARDL"). */
    public static final long SEED = 0x4153474152444CL;
    private static final SimplexNoise NOISE = new SimplexNoise(new LegacyRandomSource(SEED));
    private static final SimplexNoise UNDER = new SimplexNoise(new LegacyRandomSource(SEED + 1));

    /** The inner edge of the stone lip, its outer edge, and the outer edge of the falls hanging on it. */
    public static final double LIP_INNER = 377.0;
    public static final double LIP_OUTER = 381.0;
    public static final double FALLS_OUTER = 382.5;
    /** The top of the lip (it holds the sea, whose surface is {@link CityPlan#SEA_LEVEL}). */
    public static final int LIP_TOP = 63;
    /** How wide the cliffs are, inside the edge of the land. */
    public static final double CLIFF_WIDTH = 15.0;
    /** The chasm west of the palace: an ellipse through the whole land. */
    public static final double CHASM_X = -172.0;
    public static final double CHASM_Z = -60.0;
    public static final double CHASM_RX = 10.0;
    public static final double CHASM_RZ = 40.0;

    /** What a column of Asgard is. */
    public enum Kind {
        /** The land (and its cliffs): solid from the underside to the top; water over it up to the sea level if it is lower. */
        LAND,
        /** The sea: its floor at {@code top}, water above it to the sea level. */
        SEA,
        /** The stone lip at the rim that holds the sea. */
        LIP,
        /** The falls hanging off the lip: no ground, falls blocks from {@code top} down to {@code bottom}. */
        FALLS,
        /** Nothing: the void (beyond the falls, and the chasm). */
        VOID
    }

    /**
     * One column: what it is, its top block (the land's or the sea floor's; the falls' highest block) and its bottom block
     * (the underside; the falls' lowest), and the district pad it belongs to (null: the wild land, the sea...).
     */
    public record Column(Kind kind, int top, int bottom, @Nullable CityPlan.Pad pad) {
        public boolean solid() {
            return kind == Kind.LAND || kind == Kind.SEA || kind == Kind.LIP;
        }

        /** The highest water block over this column (none if below {@code top}). */
        public int waterTop() {
            return kind == Kind.LAND || kind == Kind.SEA ? CityPlan.SEA_LEVEL : Integer.MIN_VALUE;
        }
    }

    /** The low noise of the land, about -3 to 3. */
    public static double noise(double x, double z) {
        return NOISE.getValue(x / 90.0, z / 90.0) * 2.0 + NOISE.getValue(x / 37.0, z / 37.0);
    }

    /** The edge of the land in the direction of (x, z): 235, out to 280 to the south-west and 270 to the south-east. */
    public static double edgeRadius(double x, double z) {
        double a = Math.atan2(z, x);
        return CityPlan.LAND_RADIUS + 45.0 * lobe(a, Math.toRadians(135.0)) + 35.0 * lobe(a, Math.toRadians(47.0));
    }

    private static double lobe(double a, double centre) {
        double c = Math.cos(a - centre);
        return c <= 0 ? 0 : Math.pow(c, 8);
    }

    /** The height of the wild land (no pad): about 72 in the middle, 64 near the cliffs, the noise on it. */
    public static int naturalTop(double x, double z) {
        double r = Math.sqrt(x * x + z * z);
        double plateau = Mth.clamp((210.0 - r) / 110.0, 0.0, 1.0);
        plateau = plateau * plateau * (3 - 2 * plateau);
        return (int) Math.floor(64.0 + 8.0 * plateau + noise(x, z));
    }

    /** The sea floor: about 40. */
    public static int seaFloor(double x, double z) {
        return (int) Math.floor(40.0 + NOISE.getValue(x / 45.0, z / 45.0) * 4.0);
    }

    /** Whether (x, z) is in the chasm west of the palace. */
    public static boolean inChasm(double x, double z) {
        double dx = (x - CHASM_X) / CHASM_RX;
        double dz = (z - CHASM_Z) / CHASM_RZ;
        double wobble = 0.08 * NOISE.getValue(x / 9.0, z / 9.0);
        return dx * dx + dz * dz <= 1.0 + wobble;
    }

    /** The bottom of the floating land under (x, z): -48 under the centre, about 40 at the rim, with stalactites. */
    public static int underside(double x, double z) {
        double r = Math.sqrt(x * x + z * z);
        double base = -48.0 + 88.0 * Math.pow(Math.min(r, LIP_OUTER) / LIP_OUTER, 1.4) + UNDER.getValue(x / 30.0, z / 30.0) * 6.0;
        // stalactites on a hashed grid: some cells hang a spike down from their middle
        int cx = Math.floorDiv((int) Math.floor(x), 14);
        int cz = Math.floorDiv((int) Math.floor(z), 14);
        long h = hash(cx, cz);
        if ((h & 7) < 3) {
            double sx = cx * 14 + 7 + ((h >> 4) & 3) - 1.5;
            double sz = cz * 14 + 7 + ((h >> 6) & 3) - 1.5;
            double d = Math.hypot(x - sx, z - sz);
            double radius = 4.0 + ((h >> 8) & 3);
            if (d < radius) {
                base -= (14.0 + ((h >> 10) & 15)) * Math.pow(1.0 - d / radius, 1.5);
            }
        }
        return (int) Math.floor(base);
    }

    /** A fixed hash of a grid cell. */
    public static long hash(int x, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ SEED;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h & Long.MAX_VALUE;
    }

    /** The column at block (x, z) (its middle: x + 0.5, z + 0.5). */
    public static Column column(int x, int z) {
        double px = x + 0.5;
        double pz = z + 0.5;
        double r = Math.sqrt(px * px + pz * pz);
        if (r >= FALLS_OUTER) {
            return new Column(Kind.VOID, Integer.MIN_VALUE, Integer.MIN_VALUE, null);
        }
        if (r >= LIP_OUTER) {
            long h = hash(x, z);
            // the falls thin out below y 0: some stop high, most go far down into the void
            int bottom = (h & 3) == 0 ? (int) ((h >> 3) % 30) : -40 - (int) ((h >> 3) % 20);
            return new Column(Kind.FALLS, CityPlan.SEA_LEVEL, bottom, null);
        }
        int under = underside(px, pz);
        if (r >= LIP_INNER) {
            return new Column(Kind.LIP, LIP_TOP, under, null);
        }
        CityPlan.Pad pad = CityPlan.padAt(x, z);
        if (pad == null && inChasm(px, pz)) {
            return new Column(Kind.VOID, Integer.MIN_VALUE, Integer.MIN_VALUE, null);
        }
        if (pad != null) {
            return new Column(Kind.LAND, pad.top(), Math.min(under, pad.top() - 8), pad);
        }
        double edge = edgeRadius(px, pz);
        if (r > edge) {
            return new Column(Kind.SEA, Math.min(seaFloor(px, pz), CityPlan.SEA_LEVEL - 4), under, null);
        }
        int top = naturalTop(px, pz);
        if (r > edge - CLIFF_WIDTH) {
            double t = (r - (edge - CLIFF_WIDTH)) / CLIFF_WIDTH;
            top = (int) Math.floor(top - (top - seaFloor(px, pz)) * Math.pow(t, 1.6));
        }
        return new Column(Kind.LAND, top, Math.min(under, top - 4), null);
    }

    /** The top block of the land (or the sea floor) at (x, z), or {@link Integer#MIN_VALUE} where there is none. */
    public static int top(int x, int z) {
        Column c = column(x, z);
        return c.solid() ? c.top() : Integer.MIN_VALUE;
    }

    private AsgardTerrain() {
    }
}
