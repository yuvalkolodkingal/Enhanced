package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import net.minecraft.util.Mth;

/**
 * Billy's ten ranks (D-B 8.2, 3.2): an Apprentice who learns by doing, up to the Chaos Master. Every number grows from
 * five of {@code [build_magic_billy]}, those of his first rank:
 *
 * <ul>
 *     <li>meter = {@code meter} x {@link #METER_FACTORS}[rank - 1] (it doubles to rank 4, then 1.5 and 4/3 times in turn),
 *     refill = {@code refill} x {@link #REFILL_FACTORS}[rank - 1];</li>
 *     <li>side = {@code maxSide} + 8 x (rank - 1), reach = {@code reach} + 8 x (rank - 1);</li>
 *     <li>undo steps = {@code undoSteps} + 2 x (rank - 1), Repeat = {@code repeatMax} + 4 x (rank - 1);</li>
 *     <li>brush = side / 8, magic select = the meter's size.</li>
 * </ul>
 *
 * <p>With the defaults: meter 512, 1,024, 2,048, 4,096, 6,144, 8,192, 12,288, 16,384, 24,576, 32,768; refill 16, 24, 32,
 * 40, 56, 72, 96, 128, 160, 192 a second; side and reach 24 to 96; undos 10 to 28; Repeat 8 to 44; brush 3 to 12. Ranks
 * 1 to 4 are those of 3.1 exactly. He reaches a rank when his practice reaches its threshold ({@code rank2Practice} to
 * {@code rank10Practice}); {@code maxRank} caps the rank that applies (1: he never grows), while the rank he learned
 * stays in {@link BuildData} (it never goes down).
 *
 * <p>Read live from the config, on both sides (the server config is synced); before it has loaded on a client, the
 * defaults.
 */
public final class BillyRanks {
    public static final int MIN_RANK = 1;
    public static final int MAX_RANK = 10;
    /** How many times his first rank's meter each rank holds. */
    private static final int[] METER_FACTORS = {1, 2, 4, 8, 12, 16, 24, 32, 48, 64};
    /** How many times his first rank's refill each rank gets back each second. */
    private static final double[] REFILL_FACTORS = {1.0, 1.5, 2.0, 2.5, 3.5, 4.5, 6.0, 8.0, 10.0, 12.0};
    /** The practice each rank needs when the config is not loaded (index: rank - 1). */
    private static final long[] DEFAULT_PRACTICE = {0L, 2000L, 8000L, 30000L, 60000L, 120000L, 220000L, 380000L, 600000L, 1000000L};

    /** The rank that applies to {@code data}: the one he learned, capped by {@code maxRank}. */
    public static int rank(BuildData data) {
        return Mth.clamp(data.rank(), MIN_RANK, maxRank());
    }

    /** The highest rank {@code practice} blocks of magic reach (each threshold in turn), capped by {@code maxRank}. */
    public static int rankFor(long practice) {
        int rank = MIN_RANK;
        int cap = maxRank();
        while (rank < cap && practice >= practiceFor(rank + 1)) {
            rank++;
        }
        return rank;
    }

    /** The practice a rank needs (0 for an Apprentice). */
    public static long practiceFor(int rank) {
        int at = Mth.clamp(rank, MIN_RANK, MAX_RANK);
        if (at == MIN_RANK) {
            return 0L;
        }
        return loaded() ? BuildConfig.BILLY_PRACTICE[at - MIN_RANK].get() : DEFAULT_PRACTICE[at - MIN_RANK];
    }

    /** The highest rank he can reach ({@code build_magic_billy.maxRank}). */
    public static int maxRank() {
        return Mth.clamp(loaded() ? BuildConfig.BILLY_MAX_RANK.get() : MAX_RANK, MIN_RANK, MAX_RANK);
    }

    /** How many blocks of magic his meter holds. */
    public static int meter(int rank) {
        return (loaded() ? BuildConfig.BILLY_METER.get() : 512) * METER_FACTORS[steps(rank)];
    }

    /** How many blocks of magic he gets back each second. */
    public static double refill(int rank) {
        return (loaded() ? BuildConfig.BILLY_REFILL.get() : 16.0) * REFILL_FACTORS[steps(rank)];
    }

    /** The longest side of anything he builds, copies or selects. */
    public static int side(int rank) {
        return (loaded() ? BuildConfig.BILLY_MAX_SIDE.get() : 24) + 8 * steps(rank);
    }

    /** How far (blocks) he points: corners, anchors, picks. */
    public static int reach(int rank) {
        return (loaded() ? BuildConfig.BILLY_REACH.get() : 24) + 8 * steps(rank);
    }

    /** How many of his spells he can undo. */
    public static int undoSteps(int rank) {
        return (loaded() ? BuildConfig.BILLY_UNDO_STEPS.get() : 10) + 2 * steps(rank);
    }

    /** The most copies in one of his Repeats. */
    public static int repeatMax(int rank) {
        return (loaded() ? BuildConfig.BILLY_REPEAT_MAX.get() : 8) + 4 * steps(rank);
    }

    /** His biggest brush (radius): his side divided by 8. */
    public static int brush(int rank) {
        return side(rank) / 8;
    }

    /** The most blocks his double click selects: the size of his meter. */
    public static int magicSelect(int rank) {
        return meter(rank);
    }

    /** His meter now (game time {@code gameTime}), refilling at his rank's rate up to its top. */
    public static double meterNow(BuildData data, long gameTime) {
        int rank = rank(data);
        return data.meterNow(gameTime, meter(rank), refill(rank));
    }

    /** The lang key of a rank's name (Apprentice, Spellcaster, Sorcerer, Wiccan, Enchanter, Mystic, Wizard, Archmage, Reality Weaver, Chaos Master). */
    public static String nameKey(int rank) {
        return "hud.enhanced.build.rank." + Mth.clamp(rank, MIN_RANK, MAX_RANK);
    }

    /** How many ranks above an Apprentice. */
    private static int steps(int rank) {
        return Mth.clamp(rank, MIN_RANK, MAX_RANK) - MIN_RANK;
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    private BillyRanks() {
    }
}
