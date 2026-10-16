package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The numbers of the Witches' Road trials (3.2): the rules every trial shares, and the options of each trial (a trial
 * appends its own block here, with its own prefix: the Parkour's start with {@code parkour}). TOML section {@code
 * [witches_road]}. {@code EnhancedConfig} calls {@link #define} while it builds the server config; the fields are
 * assigned there. Ticks are 1/20 s. Every option is read when it is used and never cached, so editing the config file
 * changes it live.
 */
public final class WitchesRoadConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "witches_road";

    // ---- the Road itself ----
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue MAX_PLAYERS;
    public static ModConfigSpec.LongValue COURSE_SEED;
    public static ModConfigSpec.BooleanValue RESUME_ON_LOGIN;
    public static ModConfigSpec.BooleanValue RESUME_AT_CHECKPOINT;
    public static ModConfigSpec.BooleanValue ANNOUNCE;
    public static ModConfigSpec.BooleanValue VOICE;
    public static ModConfigSpec.BooleanValue HEAL_AT_CHECKPOINT;
    public static ModConfigSpec.IntValue FALL_DEPTH;
    public static ModConfigSpec.IntValue LEAVE_CONFIRM_SECONDS;
    public static ModConfigSpec.IntValue BUILD_BLOCKS_PER_TICK;
    public static ModConfigSpec.IntValue REMOVE_BLOCKS_PER_TICK;

    // ---- trial 1: the Parkour ----
    public static ModConfigSpec.IntValue PARKOUR_PADS;
    public static ModConfigSpec.IntValue PARKOUR_DIFFICULTY;
    public static ModConfigSpec.IntValue PARKOUR_CHECKPOINT_EVERY;
    public static ModConfigSpec.IntValue PARKOUR_TIME_LIMIT_SECONDS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether the Witches' Road has trials. On (the default): a player with no power who opens a Witches' Road",
                        "door with a Scarlet Witch Gene is asked to walk the Road (the trials), and chooses Billy or Tommy at the end.",
                        "Off: the door gives the instant choice of Enhanced 3.1, and nobody can enter the Road dimension.")
                .define("enabled", true);
        MAX_PLAYERS = builder.comment("The most players walking the trials at once on the whole server (each has a private place).",
                        "A player who would be one too many is told to wait.")
                .defineInRange("maxPlayers", 32, 1, 512);
        COURSE_SEED = builder.comment("Mixed into every player's course. The courses are the same for the same player every time;",
                        "change this to give every player new ones.")
                .defineInRange("courseSeed", 0L, Long.MIN_VALUE, Long.MAX_VALUE);
        RESUME_ON_LOGIN = builder.comment("A player who logs in inside the Road (the server stopped, or he left the game, in a trial) goes on at his",
                        "last checkpoint. Off: he is taken back to where he entered, with his progress kept.")
                .define("resumeOnLogin", true);
        RESUME_AT_CHECKPOINT = builder.comment("A player who left a trial and walks the Road again starts at the checkpoint he had reached.",
                        "Off: he starts the trial from its beginning.")
                .define("resumeAtCheckpoint", true);
        ANNOUNCE = builder.comment("The server is told in chat when somebody finishes a trial of the Road.")
                .define("announce", true);
        VOICE = builder.comment("The Road speaks to the player in the action bar and the title (the Voice of the Road).")
                .define("voice", true);
        HEAL_AT_CHECKPOINT = builder.comment("Reaching a checkpoint, or being put back at one, heals the player and fills his hunger.")
                .define("healAtCheckpoint", true);
        FALL_DEPTH = builder.comment("How far (blocks) below the lowest part of a stretch of the course a fall counts: the player is then put back",
                        "at his last checkpoint (nothing hurts him).")
                .defineInRange("fallDepth", 12, 3, 64);
        LEAVE_CONFIRM_SECONDS = builder.comment("Right-clicking the exit door asks for a second click within this many seconds to leave the Road.")
                .defineInRange("leaveConfirmSeconds", 5, 1, 60);
        BUILD_BLOCKS_PER_TICK = builder.comment("How many blocks of a new trial are placed in one tick (the Road is laid in a few ticks;",
                        "a lower number means a smaller hitch on a slow server and a longer wait at the door).")
                .defineInRange("buildBlocksPerTick", 20000, 500, 1000000);
        REMOVE_BLOCKS_PER_TICK = builder.comment("How many blocks of a finished trial are removed in one tick.")
                .defineInRange("removeBlocksPerTick", 20000, 500, 1000000);

        PARKOUR_PADS = builder.comment("Trial 1, the Parkour: how many jumps there are from the start to the finish.")
                .defineInRange("parkourPads", 40, 8, 150);
        PARKOUR_DIFFICULTY = builder.comment("Trial 1, the Parkour: 1 = easy (gaps of one or two blocks, wide stones), 2 = normal (gaps up to three",
                        "blocks late in the course, steps up and down), 3 = hard (smaller stones, gaps of three blocks sooner).",
                        "No jump is ever beyond what a running jump can do.")
                .defineInRange("parkourDifficulty", 2, 1, 3);
        PARKOUR_CHECKPOINT_EVERY = builder.comment("Trial 1, the Parkour: a checkpoint (a lit amethyst stone) after every this many jumps.")
                .defineInRange("parkourCheckpointEvery", 6, 2, 30);
        PARKOUR_TIME_LIMIT_SECONDS = builder.comment("Trial 1, the Parkour: seconds allowed from the start to the finish (the clock shows in the action bar).",
                        "When it runs out the player is put back at the start. 0 = no limit.")
                .defineInRange("parkourTimeLimitSeconds", 0, 0, 7200);
        builder.pop();
    }

    private WitchesRoadConfig() {
    }
}
