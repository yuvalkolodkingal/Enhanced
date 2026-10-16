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
    // ---- the ending ----
    public static ModConfigSpec.IntValue ENDING_FAREWELL_SECONDS;

    // ---- trial 1: the Parkour ----
    public static ModConfigSpec.IntValue PARKOUR_PADS;
    public static ModConfigSpec.IntValue PARKOUR_DIFFICULTY;
    public static ModConfigSpec.IntValue PARKOUR_CHECKPOINT_EVERY;
    public static ModConfigSpec.IntValue PARKOUR_TIME_LIMIT_SECONDS;

    // ---- trial 3: the Ice-Boat Race ----
    public static ModConfigSpec.IntValue BOAT_RACE_LENGTH;
    public static ModConfigSpec.IntValue BOAT_RACE_TRACK_WIDTH;
    public static ModConfigSpec.IntValue BOAT_RACE_DIFFICULTY;
    public static ModConfigSpec.IntValue BOAT_RACE_SHORTCUTS;
    public static ModConfigSpec.BooleanValue BOAT_RACE_OBSTACLES;
    public static ModConfigSpec.BooleanValue BOAT_RACE_BLUE_ICE;
    public static ModConfigSpec.IntValue BOAT_RACE_CHECKPOINT_EVERY;
    public static ModConfigSpec.IntValue BOAT_RACE_TIME_LIMIT_SECONDS;
    public static ModConfigSpec.DoubleValue BOAT_RACE_PAR_FACTOR;
    public static ModConfigSpec.IntValue BOAT_RACE_RESPAWN_COOLDOWN_SECONDS;
    public static ModConfigSpec.IntValue BOAT_RACE_COUNTDOWN_SECONDS;
    // ---- trial 2: Copy the Structure ----
    public static ModConfigSpec.IntValue STRUCTURE_COPY_TIER;
    public static ModConfigSpec.IntValue STRUCTURE_COPY_TIME_LIMIT_SECONDS;
    public static ModConfigSpec.IntValue STRUCTURE_COPY_SUPPLY_PERCENT;
    public static ModConfigSpec.BooleanValue STRUCTURE_COPY_AUTO_CHECK;
    public static ModConfigSpec.BooleanValue STRUCTURE_COPY_HINTS;
    public static ModConfigSpec.IntValue STRUCTURE_COPY_HINT_SECONDS;
    public static ModConfigSpec.IntValue STRUCTURE_COPY_HINT_BLOCKS;
    public static ModConfigSpec.IntValue STRUCTURE_COPY_MARKS;

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
        ENDING_FAREWELL_SECONDS = builder.comment("The ending: after the player has chosen Billy or Tommy in the Road (the Gene is used up and the power awakens), the Road",
                        "says its farewell in a column of light for this many seconds, and then sends him back to where he entered.",
                        "0 = he is sent back at once.")
                .defineInRange("endingFarewellSeconds", 5, 0, 30);

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

        BOAT_RACE_LENGTH = builder.comment("Trial 3, the Ice-Boat Race: how long the course is, from the start line to the finish line, in blocks.",
                        "The course winds through the Road's cell, so it is never longer than about 1400; the shortcuts make the shortest way shorter.")
                .defineInRange("boatRaceLength", 800, 200, 1400);
        BOAT_RACE_TRACK_WIDTH = builder.comment("Trial 3, the Ice-Boat Race: how wide the track is, in blocks (an odd number; the narrow stretches are 5 wide).")
                .defineInRange("boatRaceTrackWidth", 9, 7, 15);
        BOAT_RACE_DIFFICULTY = builder.comment("Trial 3, the Ice-Boat Race: 1 = easy (wide curves, one hairpin, no snow walls), 2 = normal, 3 = hard (tighter curves,",
                        "three hairpins, more snow). Every curve can be driven at the speed the boat can reach on the straight before it.")
                .defineInRange("boatRaceDifficulty", 2, 1, 3);
        BOAT_RACE_SHORTCUTS = builder.comment("Trial 3, the Ice-Boat Race: how many shortcuts the course may have. A shortcut is a straight across a loop of the track with a",
                        "gap in it (the boat has to be fast to jump it, and falls into the void if it is not); the long way round is always open.")
                .defineInRange("boatRaceShortcuts", 1, 0, 3);
        BOAT_RACE_OBSTACLES = builder.comment("Trial 3, the Ice-Boat Race: snow drifts (rough snow slows the boat) and snow walls to wind between. Nothing traps the boat.")
                .define("boatRaceObstacles", true);
        BOAT_RACE_BLUE_ICE = builder.comment("Trial 3, the Ice-Boat Race: stripes of blue ice (the slipperiest ice, the boat gains speed on it) in the track.")
                .define("boatRaceBlueIce", true);
        BOAT_RACE_CHECKPOINT_EVERY = builder.comment("Trial 3, the Ice-Boat Race: a checkpoint about every this many blocks. A boat that falls off the track is put back",
                        "on the last one.")
                .defineInRange("boatRaceCheckpointEvery", 100, 40, 400);
        BOAT_RACE_TIME_LIMIT_SECONDS = builder.comment("Trial 3, the Ice-Boat Race: seconds allowed from the start of the trial (a clock shows at the top of the screen).",
                        "When it runs out the player is put back at the start. 0 = no limit.")
                .defineInRange("boatRaceTimeLimitSeconds", 240, 0, 3600);
        BOAT_RACE_PAR_FACTOR = builder.comment("Trial 3, the Ice-Boat Race: the par time is the time the boat's physics allow on this course (the fastest way through every curve)",
                        "times this factor. The player is told how he did against it.")
                .defineInRange("boatRaceParFactor", 1.4, 1.0, 5.0);
        BOAT_RACE_RESPAWN_COOLDOWN_SECONDS = builder.comment("Trial 3, the Ice-Boat Race: a player who is stuck can sneak (Shift) to be put back on the last checkpoint.",
                        "This many seconds have to pass between two such returns (a sneak before that only keeps him in the boat).")
                .defineInRange("boatRaceRespawnCooldownSeconds", 5, 0, 120);
        BOAT_RACE_COUNTDOWN_SECONDS = builder.comment("Trial 3, the Ice-Boat Race: the countdown (3, 2, 1, go) once the player is in his boat, before the barrier at the start line opens.")
                .defineInRange("boatRaceCountdownSeconds", 3, 1, 10);
        defineStructureCopy(builder);
        builder.pop();
    }

    /** The options of trial 2, Copy the Structure (inside the {@code witches_road} section). */
    private static void defineStructureCopy(ModConfigSpec.Builder builder) {
        STRUCTURE_COPY_TIER = builder.comment("Trial 2, Copy the Structure: how big the model is. 1 = small (up to 5x5x5 blocks), 2 = medium (up to 9x9x7),",
                        "3 = large (up to 11x11x9). 0 = it depends on the player (each player gets the same size every time, but not the same for everyone).")
                .defineInRange("structureCopyTier", 2, 0, 3);
        STRUCTURE_COPY_TIME_LIMIT_SECONDS = builder.comment("Trial 2, Copy the Structure: seconds a player may stay in the trial. There is no failing: this is only",
                        "so that nobody leaves a room of the Road standing for ever. When it runs out the room is taken down and the player is sent back to",
                        "where he entered, with his Gene and his progress (the next time he builds a new model from the beginning). 0 = no limit.")
                .defineInRange("structureCopyTimeLimitSeconds", 5400, 0, 86400);
        STRUCTURE_COPY_SUPPLY_PERCENT = builder.comment("Trial 2, Copy the Structure: how much of every block the model needs the player is given, in percent",
                        "(at least 16 of each). The supply chest on the platform fills itself up again, so he cannot run out.")
                .defineInRange("structureCopySupplyPercent", 150, 100, 1000);
        STRUCTURE_COPY_AUTO_CHECK = builder.comment("Trial 2, Copy the Structure: the trial is won as soon as the plot looks exactly like the model, without pressing",
                        "Check. Off: the player has to press the Check button.")
                .define("structureCopyAutoCheck", true);
        STRUCTURE_COPY_HINTS = builder.comment("Trial 2, Copy the Structure: the Hint button shows ghost outlines of the blocks that are missing.",
                        "It costs nothing. Off: the button does nothing.")
                .define("structureCopyHints", true);
        STRUCTURE_COPY_HINT_SECONDS = builder.comment("Trial 2, Copy the Structure: how long the ghost blocks of a hint stay.")
                .defineInRange("structureCopyHintSeconds", 8, 1, 60);
        STRUCTURE_COPY_HINT_BLOCKS = builder.comment("Trial 2, Copy the Structure: the most ghost blocks a hint shows (the lowest ones that are missing first).")
                .defineInRange("structureCopyHintBlocks", 40, 1, 300);
        STRUCTURE_COPY_MARKS = builder.comment("Trial 2, Copy the Structure: the most places that Check marks with glowing outlines (missing, wrong or extra blocks).",
                        "The count of correct blocks is always shown in full.")
                .defineInRange("structureCopyMarks", 40, 1, 300);
    }

    private WitchesRoadConfig() {
    }
}
