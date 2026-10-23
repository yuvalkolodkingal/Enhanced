package com.kolodkingal.enhanced.config;


import com.kolodkingal.enhanced.config.core.ConfigSpec;
import java.util.List;

/**
 * The balance numbers of Spider-Man (DESIGN section 14). Its options are defined in {@link #define}, which
 * {@code EnhancedConfig} calls while it builds the server config (TOML section {@code [spider_man]}). Ticks are 1/20 s;
 * "exhaustion" 4.0 is one food point. Every option is read when it is used and never cached, so editing the config
 * file changes it live, like the other heroes' options.
 */
public final class SpiderConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "spider_man";

    // ---- body and falls ----
    public static ConfigSpec.DoubleValue SPEED_BONUS;
    public static ConfigSpec.DoubleValue JUMP_BONUS;
    public static ConfigSpec.DoubleValue STRENGTH_BONUS;
    public static ConfigSpec.DoubleValue FALL_DAMAGE_REDUCTION;
    public static ConfigSpec.IntValue BITE_DELAY_TICKS;

    // ---- spider-sense ----
    public static ConfigSpec.IntValue SPIDER_SENSE_RANGE;
    public static ConfigSpec.IntValue SPIDER_SENSE_INTERVAL;

    // ---- Web Shot, organic webs and web blocks ----
    public static ConfigSpec.IntValue WEB_SHOT_RANGE;
    public static ConfigSpec.IntValue WEB_SHOT_COOLDOWN;
    public static ConfigSpec.DoubleValue WEB_SHOT_EXHAUSTION;
    public static ConfigSpec.IntValue WEBBED_TICKS;
    public static ConfigSpec.DoubleValue ORGANIC_WEB_FACTOR;
    public static ConfigSpec.DoubleValue ORGANIC_WEB_COST_FACTOR;
    public static ConfigSpec.BooleanValue WEB_BLOCKS;
    public static ConfigSpec.IntValue WEB_BLOCK_TICKS;
    public static ConfigSpec.IntValue WEB_PATCH_RADIUS;
    public static ConfigSpec.IntValue WEB_BLOCK_CAP;

    // ---- Web Burst ----
    public static ConfigSpec.IntValue WEB_BURST_STRANDS;
    public static ConfigSpec.IntValue WEB_BURST_RANGE;
    public static ConfigSpec.IntValue WEB_BURST_COOLDOWN;
    public static ConfigSpec.DoubleValue WEB_BURST_EXHAUSTION;
    public static ConfigSpec.IntValue WEB_BURST_WEBBED_TICKS;

    // ---- Web Zip and Web Swing ----
    public static ConfigSpec.IntValue ZIP_RANGE;
    public static ConfigSpec.DoubleValue ZIP_SPEED;
    public static ConfigSpec.IntValue ZIP_COOLDOWN;
    public static ConfigSpec.IntValue ZIP_MAX_TICKS;
    public static ConfigSpec.DoubleValue ZIP_EXHAUSTION;
    public static ConfigSpec.DoubleValue ZIP_KICK_DAMAGE;
    public static ConfigSpec.IntValue SWING_RANGE;
    public static ConfigSpec.IntValue SWING_MAX_TICKS;
    public static ConfigSpec.DoubleValue SWING_MAX_SPEED;
    public static ConfigSpec.DoubleValue SWING_GRAVITY;
    public static ConfigSpec.DoubleValue SWING_EXHAUSTION;

    // ---- Web Assist (3.3) ----
    public static ConfigSpec.BooleanValue WEB_ASSIST;
    public static ConfigSpec.ConfigValue<String> WEB_ASSIST_OPEN_LAND;
    public static ConfigSpec.IntValue WEB_ASSIST_RANGE;
    public static ConfigSpec.IntValue SKY_ANCHOR_MAX_TICKS;
    public static ConfigSpec.IntValue WEB_BOOST_COOLDOWN;

    // ---- Wall Crawl ----
    public static ConfigSpec.DoubleValue CRAWL_SPEED;
    public static ConfigSpec.DoubleValue CRAWL_EXHAUSTION;
    public static ConfigSpec.DoubleValue LEAP_SPEED;

    // ---- suits and the Iron Spider ----
    public static ConfigSpec.DoubleValue HOMEMADE_ARMOR;
    public static ConfigSpec.DoubleValue BRAND_NEW_DAY_ARMOR;
    public static ConfigSpec.DoubleValue BRAND_NEW_DAY_TOUGHNESS;
    public static ConfigSpec.DoubleValue IRON_SPIDER_ARMOR;
    public static ConfigSpec.DoubleValue IRON_SPIDER_TOUGHNESS;
    public static ConfigSpec.DoubleValue IRON_SPIDER_KNOCKBACK_RESISTANCE;
    public static ConfigSpec.IntValue IRON_SPIDER_STRIP_TICKS;
    public static ConfigSpec.DoubleValue LEGS_DAMAGE;
    public static ConfigSpec.IntValue LEGS_INTERVAL;
    public static ConfigSpec.DoubleValue LEGS_RANGE;
    public static ConfigSpec.DoubleValue LEGS_REACH_BONUS;
    public static ConfigSpec.DoubleValue LEGS_EXHAUSTION;
    public static ConfigSpec.DoubleValue INSTANT_KILL_RANGE;
    public static ConfigSpec.IntValue INSTANT_KILL_INTERVAL;
    public static ConfigSpec.DoubleValue INSTANT_KILL_DAMAGE;
    public static ConfigSpec.IntValue INSTANT_KILL_MAX_TICKS;
    public static ConfigSpec.IntValue INSTANT_KILL_COOLDOWN;
    public static ConfigSpec.DoubleValue INSTANT_KILL_EXHAUSTION;
    public static ConfigSpec.IntValue INSTANT_KILL_CONFIRM_TICKS;
    public static ConfigSpec.BooleanValue INSTANT_KILL_PLAYERS;

    // ---- Black Eyes ----
    public static ConfigSpec.DoubleValue BLACK_EYES_SPEED_BONUS;
    public static ConfigSpec.DoubleValue BLACK_EYES_STRENGTH_BONUS;
    public static ConfigSpec.DoubleValue BLACK_EYES_ATTACK_SPEED_BONUS;
    public static ConfigSpec.DoubleValue BLACK_EYES_JUMP_BONUS;
    public static ConfigSpec.DoubleValue BLACK_EYES_KNOCKBACK_RESISTANCE;
    public static ConfigSpec.DoubleValue BLACK_EYES_EXHAUSTION;
    public static ConfigSpec.IntValue BLACK_EYES_MAX_TICKS;
    public static ConfigSpec.IntValue BLACK_EYES_COOLDOWN;
    public static ConfigSpec.IntValue BLACK_EYES_CRASH_TICKS;
    public static ConfigSpec.DoubleValue BLACK_EYES_PANIC_HEALTH;
    public static ConfigSpec.DoubleValue BLACK_EYES_SENSE_FACTOR;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);

        SPEED_BONUS = builder.comment("Extra walking speed of Spider-Man (a fraction of the normal speed).")
                .defineInRange("speedBonus", 0.15, 0.0, 2.0);
        JUMP_BONUS = builder.comment("Extra jump strength of Spider-Man (0.3 is a jump of about 2.1 blocks instead of 1.25).")
                .defineInRange("jumpBonus", 0.30, 0.0, 2.0);
        STRENGTH_BONUS = builder.comment("Extra melee damage of Spider-Man (his bare fists hit for 3).")
                .defineInRange("strengthBonus", 2.0, 0.0, 20.0);
        FALL_DAMAGE_REDUCTION = builder.comment("The part of fall damage Spider-Man does not take.")
                .defineInRange("fallDamageReduction", 0.5, 0.0, 1.0);
        BITE_DELAY_TICKS = builder.comment("How long the Radioactive Spider's bite takes to change a player (a sick 7 seconds).")
                .defineInRange("biteDelayTicks", 140, 20, 1200);

        SPIDER_SENSE_RANGE = builder.comment("How far (blocks) Spider-Man feels danger coming.")
                .defineInRange("spiderSenseRange", 12, 4, 48);
        SPIDER_SENSE_INTERVAL = builder.comment("Ticks between two scans of his spider-sense for danger.")
                .defineInRange("spiderSenseInterval", 5, 1, 40);

        WEB_SHOT_RANGE = builder.comment("Range (blocks) of a Web Shot.")
                .defineInRange("webShotRange", 24, 4, 64);
        WEB_SHOT_COOLDOWN = builder.comment("Ticks between two Web Shots (hidden: only the Web Burst's cooldown is shown).")
                .defineInRange("webShotCooldown", 10, 0, 200);
        WEB_SHOT_EXHAUSTION = builder.comment("Food exhaustion of one Web Shot (4.0 = 1 food point).")
                .defineInRange("webShotExhaustion", 0.4, 0.0, 4.0);
        WEBBED_TICKS = builder.comment("How long a creature hit by a Web Shot stays webbed (20 ticks = 1 second).")
                .defineInRange("webbedTicks", 120, 20, 1200);
        ORGANIC_WEB_FACTOR = builder.comment("Organic webs (the Brand New Day look, or the Black Eyes) last this many times as long.")
                .defineInRange("organicWebFactor", 1.5, 1.0, 5.0);
        ORGANIC_WEB_COST_FACTOR = builder.comment("Organic webs cost this many times the hunger.")
                .defineInRange("organicWebCostFactor", 2.0, 1.0, 8.0);
        WEB_BLOCKS = builder.comment("Whether webs also leave temporary web blocks (the cocoon round a creature, the patch on a wall).",
                        "With false, webs only entangle creatures.")
                .define("webBlocks", true);
        WEB_BLOCK_TICKS = builder.comment("How long a web block lasts before it disappears (it only ever replaces air).")
                .defineInRange("webBlockTicks", 200, 20, 6000);
        WEB_PATCH_RADIUS = builder.comment("The radius of the patch a Web Shot leaves on a wall (1 = 3 x 3 blocks).")
                .defineInRange("webPatchRadius", 1, 0, 2);
        WEB_BLOCK_CAP = builder.comment("The most web blocks one Spider-Man can have at once; the oldest go first.")
                .defineInRange("webBlockCap", 64, 8, 512);

        WEB_BURST_STRANDS = builder.comment("Strands of a Web Burst (Sneak + R), spread evenly in every direction.")
                .defineInRange("webBurstStrands", 24, 6, 96);
        WEB_BURST_RANGE = builder.comment("How far (blocks) a strand of the burst flies.")
                .defineInRange("webBurstRange", 10, 3, 24);
        WEB_BURST_COOLDOWN = builder.comment("Cooldown of the Web Burst in ticks.")
                .defineInRange("webBurstCooldown", 240, 0, 6000);
        WEB_BURST_EXHAUSTION = builder.comment("Food exhaustion of one Web Burst (4.0 = 1 food point).")
                .defineInRange("webBurstExhaustion", 2.0, 0.0, 8.0);
        WEB_BURST_WEBBED_TICKS = builder.comment("How long a Web Burst keeps a creature webbed.")
                .defineInRange("webBurstWebbedTicks", 80, 20, 1200);

        ZIP_RANGE = builder.comment("How far (blocks) a Web Zip reaches.")
                .defineInRange("zipRange", 40, 8, 96);
        ZIP_SPEED = builder.comment("Speed of a zip in blocks per tick, before his speed factor (1.3 is 26 blocks a second).")
                .defineInRange("zipSpeed", 1.3, 0.3, 3.0);
        ZIP_COOLDOWN = builder.comment("Ticks before he can zip again (small, so zips chain).")
                .defineInRange("zipCooldown", 6, 0, 200);
        ZIP_MAX_TICKS = builder.comment("The longest a zip can take, in ticks.")
                .defineInRange("zipMaxTicks", 60, 10, 400);
        ZIP_EXHAUSTION = builder.comment("Food exhaustion of one zip (4.0 = 1 food point).")
                .defineInRange("zipExhaustion", 0.3, 0.0, 4.0);
        ZIP_KICK_DAMAGE = builder.comment("Damage of the kick when a zip aimed at a creature ends on it (0 = no kick, the zip only goes to blocks).")
                .defineInRange("zipKickDamage", 4.0, 0.0, 40.0);
        SWING_RANGE = builder.comment("How far (blocks) the anchor of a Web Swing can be.")
                .defineInRange("swingRange", 40, 8, 96);
        SWING_MAX_TICKS = builder.comment("The longest a swing can last, in ticks.")
                .defineInRange("swingMaxTicks", 300, 20, 2400);
        SWING_MAX_SPEED = builder.comment("His top speed while swinging, in blocks per tick.")
                .defineInRange("swingMaxSpeed", 1.8, 0.5, 3.0);
        SWING_GRAVITY = builder.comment("Gravity while he swings, in blocks per tick squared (vanilla's is 0.08).")
                .defineInRange("swingGravity", 0.06, 0.02, 0.12);
        SWING_EXHAUSTION = builder.comment("Food exhaustion per tick while he swings (4.0 = 1 food point).")
                .defineInRange("swingExhaustion", 0.01, 0.0, 1.0);
        WEB_ASSIST = builder.comment("Whether Web Assist may be used: the swing chooses its own anchor ahead and above (trees count), keeps",
                        "clear of the ground and the leaves, lets go at the best moment and swings on while G is held. Each Spider-Man",
                        "switches it with the Web Assist key (J); it is on for him from the start. Off: every swing is manual.")
                .define("webAssist", true);
        WEB_ASSIST_OPEN_LAND = builder.comment("What Web Assist does where there is nothing to swing from: \"sky\" (a web to the open sky ahead",
                        "and above), \"zip\" (a short web boost forward and up) or \"none\".")
                .define("webAssistOpenLand", "sky", value -> value instanceof String name && List.of("sky", "zip", "none").contains(name));
        WEB_ASSIST_RANGE = builder.comment("How far (blocks) Web Assist looks for an anchor (never further than swingRange).")
                .defineInRange("webAssistRange", 30, 8, 96);
        SKY_ANCHOR_MAX_TICKS = builder.comment("The longest a swing on a sky anchor lasts, in ticks.")
                .defineInRange("skyAnchorMaxTicks", 60, 10, 400);
        WEB_BOOST_COOLDOWN = builder.comment("Ticks between two web boosts (the \"zip\" of webAssistOpenLand).")
                .defineInRange("webBoostCooldown", 20, 0, 400);

        CRAWL_SPEED = builder.comment("His speed on a wall or ceiling in blocks per tick, before his speed factor.")
                .defineInRange("crawlSpeed", 0.22, 0.05, 0.6);
        CRAWL_EXHAUSTION = builder.comment("Food exhaustion per tick while he moves on a wall or ceiling.")
                .defineInRange("crawlExhaustion", 0.005, 0.0, 1.0);
        LEAP_SPEED = builder.comment("Speed of a leap off a wall, in blocks per tick, before his speed factor.")
                .defineInRange("leapSpeed", 0.9, 0.3, 2.0);

        HOMEMADE_ARMOR = builder.comment("Armor of the Homemade Suit (and of his own skin): a hoodie, no protection.")
                .defineInRange("homemadeArmor", 0.0, 0.0, 30.0);
        BRAND_NEW_DAY_ARMOR = builder.comment("Armor of the Brand New Day suit.")
                .defineInRange("brandNewDayArmor", 8.0, 0.0, 30.0);
        BRAND_NEW_DAY_TOUGHNESS = builder.comment("Armor toughness of the Brand New Day suit.")
                .defineInRange("brandNewDayToughness", 2.0, 0.0, 20.0);
        IRON_SPIDER_ARMOR = builder.comment("Armor of the Iron Spider.")
                .defineInRange("ironSpiderArmor", 14.0, 0.0, 30.0);
        IRON_SPIDER_TOUGHNESS = builder.comment("Armor toughness of the Iron Spider.")
                .defineInRange("ironSpiderToughness", 6.0, 0.0, 20.0);
        IRON_SPIDER_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the Iron Spider.")
                .defineInRange("ironSpiderKnockbackResistance", 0.2, 0.0, 1.0);
        IRON_SPIDER_STRIP_TICKS = builder.comment("How long Magneto's Armor and Weapon Strip scatters the nanites of the Iron Spider",
                        "(its armor, its legs and Instant Kill are off meanwhile).")
                .defineInRange("ironSpiderStripTicks", 200, 0, 6000);
        LEGS_DAMAGE = builder.comment("Damage of one jab of the Iron Spider's legs (also all they do to a boss or a boss's minion).")
                .defineInRange("legsDamage", 5.0, 0.0, 100.0);
        LEGS_INTERVAL = builder.comment("Ticks between two jabs of the legs when they guard him.")
                .defineInRange("legsInterval", 20, 4, 200);
        LEGS_RANGE = builder.comment("How far (blocks) the legs jab a threat.")
                .defineInRange("legsRange", 4.0, 1.0, 10.0);
        LEGS_REACH_BONUS = builder.comment("Extra attack reach (blocks) with the legs out.")
                .defineInRange("legsReachBonus", 2.0, 0.0, 6.0);
        LEGS_EXHAUSTION = builder.comment("Food exhaustion per tick with the legs out.")
                .defineInRange("legsExhaustion", 0.01, 0.0, 1.0);
        INSTANT_KILL_RANGE = builder.comment("How far (blocks) Instant Kill reaches.")
                .defineInRange("instantKillRange", 5.0, 1.0, 12.0);
        INSTANT_KILL_INTERVAL = builder.comment("Ticks between the strikes of Instant Kill (each of the four legs strikes one threat).")
                .defineInRange("instantKillInterval", 5, 1, 40);
        INSTANT_KILL_DAMAGE = builder.comment("Damage of one strike of Instant Kill (a kill). Bosses and their minions take only legsDamage.")
                .defineInRange("instantKillDamage", 1000.0, 1.0, 10000.0);
        INSTANT_KILL_MAX_TICKS = builder.comment("The longest Instant Kill lasts, in ticks.")
                .defineInRange("instantKillMaxTicks", 200, 20, 2400);
        INSTANT_KILL_COOLDOWN = builder.comment("Cooldown in ticks after Instant Kill ends.")
                .defineInRange("instantKillCooldown", 600, 0, 12000);
        INSTANT_KILL_EXHAUSTION = builder.comment("Food exhaustion per tick while Instant Kill lasts (4.0 = 1 food point).")
                .defineInRange("instantKillExhaustion", 0.08, 0.0, 4.0);
        INSTANT_KILL_CONFIRM_TICKS = builder.comment("The window in ticks to press Sneak + B a second time to confirm Instant Kill.")
                .defineInRange("instantKillConfirmTicks", 60, 20, 200);
        INSTANT_KILL_PLAYERS = builder.comment("Whether Instant Kill may strike players (it never does by default; PvP must be on as well).")
                .define("instantKillPlayers", false);

        BLACK_EYES_SPEED_BONUS = builder.comment("Extra speed (a fraction, on top of his own) while the Black Eyes are on.")
                .defineInRange("blackEyesSpeedBonus", 0.6, 0.0, 5.0);
        BLACK_EYES_STRENGTH_BONUS = builder.comment("Extra melee damage while the Black Eyes are on",
                        "(bare fists: (1 + 2 + 4) x 6 swings a second = 42 damage a second).")
                .defineInRange("blackEyesStrengthBonus", 4.0, 0.0, 40.0);
        BLACK_EYES_ATTACK_SPEED_BONUS = builder.comment("Extra attack speed (a fraction) while the Black Eyes are on.")
                .defineInRange("blackEyesAttackSpeedBonus", 0.5, 0.0, 3.0);
        BLACK_EYES_JUMP_BONUS = builder.comment("Extra jump strength (a fraction) while the Black Eyes are on.")
                .defineInRange("blackEyesJumpBonus", 0.25, 0.0, 2.0);
        BLACK_EYES_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance while the Black Eyes are on.")
                .defineInRange("blackEyesKnockbackResistance", 0.3, 0.0, 1.0);
        BLACK_EYES_EXHAUSTION = builder.comment("Food exhaustion per tick while the Black Eyes are on (0.05 = one food point every 4 seconds).")
                .defineInRange("blackEyesExhaustion", 0.05, 0.0, 4.0);
        BLACK_EYES_MAX_TICKS = builder.comment("The longest the surge of the Black Eyes lasts, in ticks.")
                .defineInRange("blackEyesMaxTicks", 600, 40, 6000);
        BLACK_EYES_COOLDOWN = builder.comment("Cooldown in ticks after the surge ends (half of it when he calms himself).")
                .defineInRange("blackEyesCooldown", 600, 0, 12000);
        BLACK_EYES_CRASH_TICKS = builder.comment("How long the crash (Slowness II and Weakness) lasts when the surge runs out.")
                .defineInRange("blackEyesCrashTicks", 200, 0, 2400);
        BLACK_EYES_PANIC_HEALTH = builder.comment("If above 0: the Black Eyes switch on by themselves when his health falls to this many points",
                        "(and cannot be calmed for 5 seconds). 0 = never.")
                .defineInRange("blackEyesPanicHealth", 0.0, 0.0, 20.0);
        BLACK_EYES_SENSE_FACTOR = builder.comment("How much the Black Eyes widen his spider-sense (and how much faster it scans).")
                .defineInRange("blackEyesSenseFactor", 2.0, 1.0, 4.0);

        builder.pop();
    }

    private SpiderConfig() {
    }
}
