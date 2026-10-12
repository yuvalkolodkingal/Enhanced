package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;

/**
 * The arithmetic of the fight's clock, as pure functions: how many stones he has after so many ticks of fight, when the
 * next one comes, how much health he has for so many fighters, how much harder he hits as the stones and the enrage
 * grow, and how long he rests between moves.
 */
public final class ThanosClock {
    /** The warning before a stone arrives: its socket pulses and its hum plays for the last ten seconds. */
    public static final int WARNING_TICKS = 200;

    /**
     * The stones he has after {@code combatTicks} of fight: Power from the start, one more each {@code
     * stoneIntervalTicks}, never more than {@code maxStones} (and never more than the five he can hold).
     */
    public static int stonesAt(long combatTicks) {
        long interval = ThanosBossConfig.STONE_INTERVAL_TICKS.get();
        long earned = 1 + combatTicks / interval;
        return (int) Math.min(Math.min(earned, ThanosBossConfig.MAX_STONES.get()), StoneKind.granted().size());
    }

    /** Ticks until the next stone, or -1 if he has the last one he can have. */
    public static int nextStoneIn(long combatTicks, int stones) {
        if (stones >= Math.min(ThanosBossConfig.MAX_STONES.get(), StoneKind.granted().size())) {
            return -1;
        }
        long due = (long) stones * ThanosBossConfig.STONE_INTERVAL_TICKS.get();
        return (int) Math.max(0L, due - combatTicks);
    }

    /**
     * His maximum health for {@code fighters} fighters: the base for one, half as much again for each other (up to
     * {@code maxFightersCounted}), times the shared difficulty scale.
     */
    public static double healthFor(int fighters) {
        int counted = Math.max(1, Math.min(fighters, ThanosBossConfig.MAX_FIGHTERS_COUNTED.get()));
        double perExtra = ThanosBossConfig.HEALTH_PER_EXTRA_PLAYER.get();
        return ThanosBossConfig.HEALTH.get() * (1.0 + perExtra * (counted - 1)) * BossConfig.DIFFICULTY_SCALE.get();
    }

    /**
     * The enrage: a slow growth of his damage once {@code ticksSinceLastStone} passes {@code enrageAfterTicks}, a step
     * per minute up to the most the config allows. 0 before.
     */
    public static double enrage(long ticksSinceLastStone) {
        long over = ticksSinceLastStone - ThanosBossConfig.ENRAGE_AFTER_TICKS.get();
        if (over <= 0) {
            return 0.0;
        }
        return Math.min(ThanosBossConfig.ENRAGE_MAX.get(), ThanosBossConfig.ENRAGE_STEP_PER_MINUTE.get() * over / 1200.0);
    }

    /** What every damage of his is multiplied by: the difficulty, the stones beyond the first and the enrage. */
    public static float damageScale(int stones, long ticksSinceLastStone) {
        double perStone = 1.0 + ThanosBossConfig.DAMAGE_SCALE_PER_STONE.get() * Math.max(0, stones - 1);
        return (float) (BossConfig.DIFFICULTY_SCALE.get() * perStone * (1.0 + enrage(ticksSinceLastStone)));
    }

    /**
     * The rest after a move: {@code max(10, 26 - 3 * stones)} ticks, a little shorter for each extra fighter (never
     * under 4).
     */
    public static int recoverTicks(int stones, int fighters) {
        int gap = Math.max(10, 26 - 3 * stones) - ThanosBossConfig.GAP_PER_EXTRA_PLAYER.get() * Math.max(0, fighters - 1);
        return Math.max(4, gap);
    }

    private ThanosClock() {
    }
}
