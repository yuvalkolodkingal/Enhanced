package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The rules the three boss fights (Ultron, Thanos, Apocalypse) share: how many may run, what happens to a fight that is
 * lost or abandoned, who earns the reward, and (3.1) how every telekinesis holds a boss and borrows an arena's blocks.
 * One section for all three, so they behave alike; the numbers that belong to one boss (how long he can be held) are in
 * its own config class. {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML section
 * {@code [boss]}).
 */
public final class BossConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "boss";

    public static ConfigSpec.IntValue MAX_ACTIVE_FIGHTS;
    public static ConfigSpec.IntValue WIPE_SECONDS;
    public static ConfigSpec.IntValue ABANDON_SECONDS;
    public static ConfigSpec.IntValue MAX_FIGHT_MINUTES;
    public static ConfigSpec.IntValue REWARD_MIN_SECONDS;
    public static ConfigSpec.BooleanValue CREATIVE_GETS_REWARD;
    public static ConfigSpec.IntValue XP_REWARD_PER_PLAYER;
    public static ConfigSpec.BooleanValue ANNOUNCE_FIGHTS;
    public static ConfigSpec.BooleanValue PROTECT_ARENA;
    public static ConfigSpec.DoubleValue DIFFICULTY_SCALE;
    // 3.1: telekinesis in the boss fights
    public static ConfigSpec.BooleanValue TELEKINESIS_HOLDS_BOSSES;
    public static ConfigSpec.BooleanValue TELEKINESIS_HOLDS_VANILLA_BOSSES;
    public static ConfigSpec.IntValue VANILLA_HOLD_TICKS;
    public static ConfigSpec.BooleanValue TELEKINESIS_CATCHES_BOSS_SHOTS;
    public static ConfigSpec.IntValue REGRIP_TICKS;
    public static ConfigSpec.IntValue THROWN_STILL_TICKS;
    public static ConfigSpec.DoubleValue BILLY_HOLD_FACTOR;
    public static ConfigSpec.DoubleValue MAGNETO_HOLD_FACTOR;
    public static ConfigSpec.BooleanValue LEND_ARENA_BLOCKS;
    public static ConfigSpec.IntValue ARENA_MEND_TICKS;
    public static ConfigSpec.IntValue BORROWED_BLOCK_TICKS;
    public static ConfigSpec.IntValue MAX_STANDING_PER_PLAYER;
    public static ConfigSpec.IntValue MAX_BORROWED_BLOCKS;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        MAX_ACTIVE_FIGHTS = builder.comment("How many boss fights may run at once on the whole server (Ultron, Thanos and Apocalypse together).",
                        "A ritual that would start another one is refused.")
                .defineInRange("maxActiveFights", 1, 1, 8);
        WIPE_SECONDS = builder.comment("When every fighter is dead, the fight ends after this many seconds (the boss leaves, nobody is rewarded).")
                .defineInRange("wipeSeconds", 15, 1, 600);
        ABANDON_SECONDS = builder.comment("When no fighter has been near the boss for this many seconds, the fight ends and the boss leaves.")
                .defineInRange("abandonSeconds", 90, 10, 3600);
        MAX_FIGHT_MINUTES = builder.comment("No fight lasts longer than this: after it the boss leaves (a fail-safe against a stuck fight).")
                .defineInRange("maxFightMinutes", 25, 1, 240);
        REWARD_MIN_SECONDS = builder.comment("A fighter earns the reward only after being in the fight at least this many seconds",
                        "(and having hurt the boss; the player who lands the last blow earns it whatever the time).")
                .defineInRange("rewardMinSeconds", 60, 0, 3600);
        CREATIVE_GETS_REWARD = builder.comment("If true, Creative players who fought also earn the reward. They never do by default.")
                .define("creativeGetsReward", false);
        XP_REWARD_PER_PLAYER = builder.comment("Experience points every rewarded fighter gets when the boss falls.")
                .defineInRange("xpRewardPerPlayer", 1500, 0, 100000);
        ANNOUNCE_FIGHTS = builder.comment("If true (the default), the server is told in chat when a boss is called and when it falls.")
                .define("announceFights", true);
        PROTECT_ARENA = builder.comment("If true (the default), blocks in a boss's arena can't be broken, placed, burned or blown up while the",
                        "fight lasts, so the world is the same when it ends. (Thanos's arena is never protected.)",
                        "The floating island of Ultron's fight is always protected: it is a copy of the ground, not the world.")
                .define("protectArena", true);
        DIFFICULTY_SCALE = builder.comment("Multiplies the health and the damage of every boss (1.0 = as designed, 0.5 = half as hard).")
                .defineInRange("difficultyScale", 1.0, 0.1, 10.0);

        TELEKINESIS_HOLDS_BOSSES = builder.comment("Telekinesis (Wanda, Billy, the Phoenix, Magneto) can take hold of Ultron, Thanos and Apocalypse, as",
                        "bosses: for a short time, then they break free. Off: they cannot be held at all (\"too powerful to lift\", as in",
                        "3.0); Wanda's rip of Ultron's heart still works.")
                .define("telekinesisHoldsBosses", true);
        TELEKINESIS_HOLDS_VANILLA_BOSSES = builder.comment("Telekinesis can take hold of the Wither, the Warden and the Elder Guardian, as bosses: for a short time,",
                        "then they break free (the Ender Dragon is too vast to hold). Off: they are \"too powerful to lift\".")
                .define("telekinesisHoldsVanillaBosses", true);
        VANILLA_HOLD_TICKS = builder.comment("How long telekinesis holds the Wither, the Warden or the Elder Guardian before he breaks free (before the",
                        "hero's factor, billyHoldFactor for Billy; Magneto holds none of the three, they wear no metal).")
                .defineInRange("vanillaHoldTicks", 60, 10, 600);
        TELEKINESIS_CATCHES_BOSS_SHOTS = builder.comment("Billy and the Phoenix (and Magneto, for metal) can catch Thanos's sword, meteors and orbs, and what they",
                        "throw back is theirs (Ultron's bolts and Apocalypse's lances then hit their own side). Off: Thanos's shots",
                        "cannot be caught, and a caught bolt or lance stays the boss's (as in 3.0).")
                .define("telekinesisCatchesBossShots", true);
        REGRIP_TICKS = builder.comment("After a boss breaks free, is let go or lands from a throw, nobody can take hold of him again for this many ticks.")
                .defineInRange("regripTicks", 240, 0, 2400);
        THROWN_STILL_TICKS = builder.comment("A boss thrown by telekinesis does nothing for at most this many ticks of his flight (his regrip time",
                        "starts after it).")
                .defineInRange("thrownStillTicks", 20, 0, 40);
        BILLY_HOLD_FACTOR = builder.comment("Billy holds a boss this many times as long as Wanda (the bosses only: Thanos's sword he holds",
                        "thanos_boss.swordHoldTicks, as the Phoenix does).")
                .defineInRange("billyHoldFactor", 0.5, 0.1, 2.0);
        MAGNETO_HOLD_FACTOR = builder.comment("Magneto holds a metal boss, and every metal thing of a fight (Thanos's sword), this many times as long",
                        "as Wanda.")
                .defineInRange("magnetoHoldFactor", 1.5, 0.1, 3.0);
        LEND_ARENA_BLOCKS = builder.comment("Block telekinesis can borrow the blocks of a protected arena: the holes mend, a borrowed block crumbles",
                        "after a while, and everything goes back when the fight ends. Off: lifting is refused in the arenas (as in 3.0).")
                .define("lendArenaBlocks", true);
        ARENA_MEND_TICKS = builder.comment("Ticks after which the hole a borrowed block left mends.")
                .defineInRange("arenaMendTicks", 100, 20, 1200);
        BORROWED_BLOCK_TICKS = builder.comment("How long (ticks) a borrowed block that is set down stands before it crumbles.")
                .defineInRange("borrowedBlockTicks", 100, 20, 2400);
        MAX_STANDING_PER_PLAYER = builder.comment("At most this many borrowed blocks of one player stand at once; one more set down crumbles at once",
                        "(0: every borrowed block crumbles when it is set down).")
                .defineInRange("maxStandingPerPlayer", 3, 0, 48);
        MAX_BORROWED_BLOCKS = builder.comment("At most this many borrowed blocks (held, flying or standing) per fight.")
                .defineInRange("maxBorrowedBlocks", 48, 1, 512);
        builder.pop();
    }

    private BossConfig() {
    }
}
