package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The rules the three boss fights (Ultron, Thanos, Apocalypse) share: how many may run, what happens to a fight that is
 * lost or abandoned, who earns the reward. One section for all three, so they behave alike; the numbers that belong to
 * one boss are in its own config class. {@code EnhancedConfig} calls {@link #define} while it builds the server config
 * (TOML section {@code [boss]}).
 */
public final class BossConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "boss";

    public static ModConfigSpec.IntValue MAX_ACTIVE_FIGHTS;
    public static ModConfigSpec.IntValue WIPE_SECONDS;
    public static ModConfigSpec.IntValue ABANDON_SECONDS;
    public static ModConfigSpec.IntValue MAX_FIGHT_MINUTES;
    public static ModConfigSpec.IntValue REWARD_MIN_SECONDS;
    public static ModConfigSpec.BooleanValue CREATIVE_GETS_REWARD;
    public static ModConfigSpec.IntValue XP_REWARD_PER_PLAYER;
    public static ModConfigSpec.BooleanValue ANNOUNCE_FIGHTS;
    public static ModConfigSpec.BooleanValue PROTECT_ARENA;
    public static ModConfigSpec.DoubleValue DIFFICULTY_SCALE;

    public static void define(ModConfigSpec.Builder builder) {
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
                        "fight lasts, so the world is the same when it ends. (Thanos's arena is never protected.)")
                .define("protectArena", true);
        DIFFICULTY_SCALE = builder.comment("Multiplies the health and the damage of every boss (1.0 = as designed, 0.5 = half as hard).")
                .defineInRange("difficultyScale", 1.0, 0.1, 10.0);
        builder.pop();
    }

    private BossConfig() {
    }
}
