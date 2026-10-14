package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Billy, an apprentice who learns (D-B 8): his magic meter, what a spell costs him, his practice and his ranks, Mom's
 * help, and what he is told. Pure arithmetic over his {@link BuildData} and the game time, the same on both sides: the
 * server pays and books with it, his client shows the cost and the meter with it. His rank's numbers are
 * {@link BillyRanks}'.
 *
 * <ul>
 *     <li><b>The meter</b> refills by itself, also outside build mode and while he is away: it is stored as a value at
 *     a game time and computed when read ({@link BuildData#meterNow}). A spell costs one per block it changes (a Move
 *     one per block moved), near a Scarlet Witch {@code momCostFactor} of that, rounded up.</li>
 *     <li><b>Whole or nothing</b>: a spell he cannot pay does not start. More than his whole meter is {@code TOO_BIG}
 *     ("600 of 512"); more than he has now, {@code REFILLING} with the seconds to wait. Undo gives back what it
 *     restores; redo pays again.</li>
 *     <li><b>Practice</b> is every block he really changed (not halved by Mom); an undo takes back the practice of what
 *     it restores, so filling and undoing cannot farm ranks, but his rank never goes down. A rank reached is a moment:
 *     the new numbers apply at once and the meter fills to its new top as a gift.</li>
 * </ul>
 *
 * <p>Every game time here is the level's ({@code level.getGameTime()}), which every dimension of a server shares and a
 * client is told, so the meter reads the same on both sides.
 */
public final class BillyMagic {
    /** How long after "Mom is near" it is said again (once a minute at most, D-B 8.3). */
    public static final int MOM_SAID_TICKS = 1200;

    /**
     * The cost of a spell that changes {@code blocks} blocks: the blocks, or near a Scarlet Witch {@code momCostFactor}
     * of them rounded up (100 cost 50, 101 cost 51).
     */
    public static int cost(long blocks, boolean momNear) {
        long whole = Math.max(0L, blocks);
        if (momNear) {
            whole = (long) Math.ceil(whole * momCostFactor() - 1.0E-9);
        }
        return (int) Math.min(Integer.MAX_VALUE, whole);
    }

    /** His meter's top at the rank that applies to him (512 at rank 1). */
    public static int top(BuildData data) {
        return BillyRanks.meter(BillyRanks.rank(data));
    }

    /** His meter now, in whole blocks of magic (what the badge shows). */
    public static int meter(BuildData data, long gameTime) {
        return Mth.floor(BillyRanks.meterNow(data, gameTime));
    }

    /**
     * Whether he can pay {@code cost} now: {@link SpellPlanner#NONE} if he can, {@link BuildVerdictPayload#TOO_BIG} if
     * it is more than his whole meter (or more than he has while his meter never refills, {@code refill} 0), else
     * {@link BuildVerdictPayload#REFILLING} ({@link #secondsToPay} says how long).
     */
    public static int verdict(BuildData data, long gameTime, int cost) {
        double now = BillyRanks.meterNow(data, gameTime);
        if (cost <= now + 1.0E-9) {
            return SpellPlanner.NONE;
        }
        if (cost > top(data) || refill(data) <= 0.0) {
            return BuildVerdictPayload.TOO_BIG;
        }
        return BuildVerdictPayload.REFILLING;
    }

    /**
     * The whole seconds until his meter holds {@code cost}: 40 short at 16 a second is 3; 0 if it does now,
     * {@link Integer#MAX_VALUE} if it never will ({@code refill} 0).
     */
    public static int secondsToPay(BuildData data, long gameTime, int cost) {
        double missing = cost - BillyRanks.meterNow(data, gameTime);
        double rate = refill(data);
        if (missing <= 1.0E-9) {
            return 0;
        }
        if (rate <= 0.0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(missing / rate - 1.0E-9));
    }

    /** His meter after paying {@code cost} at {@code gameTime} (never below 0). */
    public static BuildData pay(BuildData data, long gameTime, int cost) {
        return data.withMeter(Math.max(0.0, BillyRanks.meterNow(data, gameTime) - Math.max(0, cost)), gameTime);
    }

    /**
     * His meter after {@code amount} comes back at {@code gameTime} (an undo, an unused reservation; never above its
     * top).
     */
    public static BuildData refund(BuildData data, long gameTime, int amount) {
        double back = BillyRanks.meterNow(data, gameTime) + Math.max(0, amount);
        return data.withMeter(Math.min(top(data), back), gameTime);
    }

    /** His meter set by an operator ({@code /enhanced build meter}), between 0 and its top. */
    public static BuildData setMeter(BuildData data, long gameTime, int value) {
        return data.withMeter(Mth.clamp(value, 0, top(data)), gameTime);
    }

    /** The blocks of magic he gets back each second at the rank that applies to him. */
    public static double refill(BuildData data) {
        return BillyRanks.refill(BillyRanks.rank(data));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Practice and ranks

    /**
     * His data after practice, and whether he reached a new rank by it.
     *
     * @param data     what he has now
     * @param rankedUp whether {@code data}'s rank is new (the moment: the flare, the fanfare, {@link #rankUp})
     */
    public record Practiced(BuildData data, boolean rankedUp) {
    }

    /**
     * Books {@code blocks} blocks of practice (the blocks his spell really changed) at {@code gameTime}. When they
     * reach a rank's threshold (capped by {@code maxRank}) he learns it: the new rank, and the meter full at its new
     * top.
     */
    public static Practiced practise(BuildData data, long gameTime, long blocks) {
        long practice = data.practice() + Math.max(0L, blocks);
        BuildData now = data.withPractice(practice < 0 ? Long.MAX_VALUE : practice);
        int rank = BillyRanks.rankFor(now.practice());
        if (rank <= data.rank()) {
            return new Practiced(now, false);
        }
        return new Practiced(now.withRank(rank).withMeter(BuildData.FULL, gameTime), true);
    }

    /** Takes back the practice of {@code blocks} blocks an undo restored; the rank he learned stays. */
    public static BuildData unpractise(BuildData data, long blocks) {
        return data.withPractice(Math.max(0L, data.practice() - Math.max(0L, blocks)));
    }

    /**
     * His rank set by an operator ({@code /enhanced build rank}): that rank, his practice at least its threshold (and
     * at most it when the rank goes down, so his next spell does not undo the change), the meter full at its top.
     */
    public static BuildData setRank(BuildData data, long gameTime, int rank) {
        int to = Mth.clamp(rank, BillyRanks.MIN_RANK, BillyRanks.MAX_RANK);
        long threshold = BillyRanks.practiceFor(to);
        long practice = to < data.rank() ? threshold : Math.max(data.practice(), threshold);
        return data.withRank(to).withPractice(practice).withMeter(BuildData.FULL, gameTime);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Pauses and Mom

    /** Whether he is still in the pause after his last spell (C10, {@code WAIT}). */
    public static boolean paused(BuildData data, long gameTime) {
        return gameTime < data.pauseUntil();
    }

    /** His data with the pause after a spell that ended at {@code gameTime} ({@code castPause} ticks). */
    public static BuildData pauseAfter(BuildData data, long gameTime, int castPause) {
        return data.withPauseUntil(gameTime + Math.max(0, castPause));
    }

    /**
     * The Scarlet Witch who helps him: the nearest player with that power, alive and not a Spectator, within
     * {@code momRange} blocks of him in his dimension; null when none is (or {@code momRange} is 0).
     */
    public static @Nullable Player mom(Player billy) {
        int range = momRange();
        if (range <= 0) {
            return null;
        }
        Player nearest = null;
        double best = (double) range * range;
        for (Player player : billy.level().players()) {
            if (player == billy || !player.isAlive() || player.isSpectator() || !Powers.is(player, Power.SCARLET_WITCH)) {
                continue;
            }
            double distance = player.distanceToSqr(billy);
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    /** Whether "Mom is near" may be said again, last said at {@code lastSaid} (once a minute at most). */
    public static boolean saysMomAgain(long lastSaid, long gameTime) {
        return gameTime - lastSaid >= MOM_SAID_TICKS;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What he is told (D-B 8.4; his side and reach are every hero's: Limits.tooWide, Limits.tooFar)

    /** "Your magic is refilling: %s more seconds" ({@code REFILLING}). */
    public static Component refilling(int seconds) {
        return Component.translatable("message.enhanced.build.refilling", seconds);
    }

    /** "Bigger than your magic yet: %s of %s. Make it smaller, try a hollow style, or cast it near a Scarlet Witch". */
    public static Component tooBig(int cost, int meter) {
        return Component.translatable("message.enhanced.build.too_big", cost, meter);
    }

    /** "Wait for your spell to finish" ({@code WAIT}). */
    public static Component waitForSpell() {
        return Component.translatable("message.enhanced.build.wait");
    }

    /** "TNT, lava and fire are too dangerous for your magic" ({@code VOLATILE}). */
    public static Component tooDangerous() {
        return Component.translatable("message.enhanced.build.volatile");
    }

    /** "Your oldest spell can no longer be undone" (his eleventh spell at rank 1). */
    public static Component forgotten() {
        return Component.translatable("message.enhanced.build.forgotten");
    }

    /** "Mom is near: your magic costs half". */
    public static Component momNear() {
        return Component.translatable("message.enhanced.build.mom");
    }

    /** "You learned more magic! Rank 2: Spellcaster. Your magic now holds 1024 blocks". */
    public static Component rankUp(int rank) {
        return Component.translatable("message.enhanced.build.rank_up", rank, Component.translatable(BillyRanks.nameKey(rank)),
                BillyRanks.meter(rank));
    }

    /**
     * What he is told when he cannot pay {@code cost} now ({@link #verdict}): too big, with the cost and his top (or
     * what he has, when his meter never refills); refilling, with the seconds; null when he can pay.
     */
    public static @Nullable Component cannotPay(BuildData data, long gameTime, int cost) {
        return switch (verdict(data, gameTime, cost)) {
            case BuildVerdictPayload.TOO_BIG -> tooBig(cost, cost > top(data) ? top(data) : meter(data, gameTime));
            case BuildVerdictPayload.REFILLING -> refilling(secondsToPay(data, gameTime, cost));
            default -> null;
        };
    }

    private static double momCostFactor() {
        return EnhancedConfig.SPEC.isLoaded() ? BuildConfig.BILLY_MOM_COST_FACTOR.get() : 0.5;
    }

    private static int momRange() {
        return EnhancedConfig.SPEC.isLoaded() ? BuildConfig.BILLY_MOM_RANGE.get() : 16;
    }

    private BillyMagic() {
    }
}
