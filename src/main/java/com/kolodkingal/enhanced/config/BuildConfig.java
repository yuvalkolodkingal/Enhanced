package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * The numbers of Build Magic, the building spells of the Scarlet Witch and Billy (3.1): the rules every builder shares
 * (TOML section {@code [build_magic]}), Wanda's own ({@code [build_magic_wanda]}: her magic has no limit, only technical
 * safety) and Billy's ({@code [build_magic_billy]}: his magic meter and his ten ranks). Its options are defined in
 * {@link #define}, which {@code EnhancedConfig} calls while it builds the server config; it pushes the three sections
 * itself. Ticks are 1/20 s. Every option is read when it is used and never cached, so editing the config file changes it
 * live; a value read on the client is guarded with {@code EnhancedConfig.SPEC.isLoaded()}.
 *
 * <p>Billy's numbers are those of his first rank, an Apprentice; {@code BillyRanks} grows them rank by rank. A family that
 * wants him as strong as his mother sets the nine practice numbers 0 and a large meter. A comment names another option
 * with its section ({@code build_magic_billy.meter}), never as {@code billy.x}, which is the {@code [billy]} section of
 * his other powers.
 */
public final class BuildConfig {
    /** The TOML section of the rules every builder shares. */
    public static final String SECTION = "build_magic";
    /** The TOML section of the Scarlet Witch's own numbers. */
    public static final String WANDA_SECTION = "build_magic_wanda";
    /** The TOML section of Billy's own numbers. */
    public static final String BILLY_SECTION = "build_magic_billy";

    /** What a spell does with a container that holds items in its way ({@code build_magic.fullContainers}). */
    public enum FullContainers {
        /** It is left as it is, and the spell says how many were left. */
        SKIP,
        /** Its items spill on the spot, and the spell goes on. */
        SPILL
    }

    // ---- [build_magic]: the server ----
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue OPERATORS_ONLY;
    public static ModConfigSpec.BooleanValue REQUIRE_MATERIALS;
    public static ModConfigSpec.IntValue MILLIS_PER_TICK;
    public static ModConfigSpec.IntValue BLOCKS_PER_TICK;
    public static ModConfigSpec.IntValue SECTIONS_PER_TICK;
    public static ModConfigSpec.IntValue MIN_SPELL_TICKS;
    public static ModConfigSpec.IntValue MAX_QUEUED;
    public static ModConfigSpec.IntValue MAX_SPELLS_TOTAL;
    public static ModConfigSpec.IntValue HOLD_TO_CONFIRM_BLOCKS;
    public static ModConfigSpec.IntValue HOLD_TO_CONFIRM_TICKS;
    public static ModConfigSpec.IntValue HUGE_SPELL_BLOCKS;
    public static ModConfigSpec.IntValue HUGE_HOLD_TICKS;
    // ---- [build_magic]: where and on what ----
    public static ModConfigSpec.BooleanValue ALL_ARENAS_PROTECTED;
    public static ModConfigSpec.BooleanValue HEX_IS_A_CLAIM;
    public static ModConfigSpec.EnumValue<FullContainers> FULL_CONTAINERS;
    public static ModConfigSpec.BooleanValue LIFT_CREATURES;
    public static ModConfigSpec.BooleanValue MOVE_CARRIES_CREATURES;
    public static ModConfigSpec.BooleanValue HOLD_GRAVITY_BLOCKS;
    public static ModConfigSpec.BooleanValue POST_EVENTS_PER_BLOCK;
    public static ModConfigSpec.IntValue PREVIEW_CHECKS;
    // ---- [build_magic]: undo, the pocket, memory and disk ----
    public static ModConfigSpec.BooleanValue UNDO_SURVIVES_RESTART;
    public static ModConfigSpec.IntValue UNDO_MEMORY_MB;
    public static ModConfigSpec.IntValue UNDO_DISK_MB;
    public static ModConfigSpec.IntValue SERVER_UNDO_MEMORY_MB;
    public static ModConfigSpec.IntValue SERVER_UNDO_DISK_MB;
    public static ModConfigSpec.IntValue POCKET_MAX_MB;
    public static ModConfigSpec.IntValue FINISH_ON_STOP_BLOCKS;
    public static ModConfigSpec.IntValue MAX_TICKET_CHUNKS;
    public static ModConfigSpec.IntValue MAX_REQUESTS_PER_SECOND;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DENIED_DIMENSIONS;

    // ---- [build_magic_wanda] ----
    public static ModConfigSpec.BooleanValue WANDA_ENABLED;
    public static ModConfigSpec.IntValue WANDA_REACH;
    public static ModConfigSpec.IntValue WANDA_UNDO_STEPS;
    public static ModConfigSpec.IntValue WANDA_MAX_BLOCKS;
    public static ModConfigSpec.BooleanValue WANDA_COPIES_CONTENTS;
    public static ModConfigSpec.IntValue WANDA_VOLATILE_PER_SPELL;
    public static ModConfigSpec.DoubleValue WANDA_HEX_BOOST;
    public static ModConfigSpec.IntValue WANDA_MAGIC_SELECT_MAX;
    public static ModConfigSpec.IntValue WANDA_BRUSH_MAX;
    public static ModConfigSpec.IntValue WANDA_SHAPE_MAX_RADIUS;
    public static ModConfigSpec.IntValue WANDA_REPEAT_MAX;
    public static ModConfigSpec.IntValue WANDA_INSTANT_MAX_BLOCKS;
    public static ModConfigSpec.IntValue WANDA_INSTANT_MAX_MILLIS;

    // ---- [build_magic_billy]: his numbers as an Apprentice, and how he grows ----
    public static ModConfigSpec.BooleanValue BILLY_ENABLED;
    public static ModConfigSpec.IntValue BILLY_METER;
    public static ModConfigSpec.DoubleValue BILLY_REFILL;
    public static ModConfigSpec.IntValue BILLY_MAX_SIDE;
    public static ModConfigSpec.IntValue BILLY_REACH;
    public static ModConfigSpec.IntValue BILLY_UNDO_STEPS;
    public static ModConfigSpec.IntValue BILLY_REPEAT_MAX;
    /** The practice each rank needs ({@code rank2Practice} to {@code rank10Practice}), by rank - 1; the first is unused (null). */
    public static final ModConfigSpec.IntValue[] BILLY_PRACTICE = new ModConfigSpec.IntValue[10];
    public static ModConfigSpec.IntValue BILLY_MAX_RANK;
    public static ModConfigSpec.IntValue BILLY_CAST_PAUSE;
    public static ModConfigSpec.IntValue BILLY_MOM_RANGE;
    public static ModConfigSpec.DoubleValue BILLY_MOM_COST_FACTOR;
    public static ModConfigSpec.BooleanValue BILLY_COPIES_CONTENTS;
    public static ModConfigSpec.BooleanValue BILLY_LIMITS_IN_CREATIVE;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Build Magic exists on this server.")
                .define("enabled", true);
        OPERATORS_ONLY = builder.comment("Only operators (permission level 2) may use it.")
                .define("operatorsOnly", false);
        REQUIRE_MATERIALS = builder.comment("Off: spells conjure their blocks from magic. On: in Survival they take one item per block from the inventory,",
                        "vanished blocks go into it, and undo gives back. Creative never pays.")
                .define("requireMaterials", false);
        MILLIS_PER_TICK = builder.comment("Milliseconds of each server tick that all spells together may use (half while the server is slow).")
                .defineInRange("millisPerTick", 8, 1, 40);
        BLOCKS_PER_TICK = builder.comment("The most block writes all spells together may make in one tick (reads count a quarter).",
                        "Every changed block is also sent to each player who sees it: lower this on a server with a slow upload.")
                .defineInRange("blocksPerTick", 20000, 256, 200000);
        SECTIONS_PER_TICK = builder.comment("The most 16x16x16 sections spells may start changing in one tick (bounds what clients download).")
                .defineInRange("sectionsPerTick", 16, 1, 256);
        MIN_SPELL_TICKS = builder.comment("A spell takes at least this long, so it is seen growing.")
                .defineInRange("minSpellTicks", 10, 1, 100);
        MAX_QUEUED = builder.comment("Spells of one Scarlet Witch that may wait behind the one at work (Billy has none).")
                .defineInRange("maxQueued", 4, 0, 16);
        MAX_SPELLS_TOTAL = builder.comment("Spells working or waiting on the whole server.")
                .defineInRange("maxSpellsTotal", 16, 1, 256);
        HOLD_TO_CONFIRM_BLOCKS = builder.comment("A spell that removes or replaces more existing blocks than this needs the right button held",
                        "(0: every such spell).")
                .defineInRange("holdToConfirmBlocks", 64, 0, 1000000);
        HOLD_TO_CONFIRM_TICKS = builder.comment("How long that hold is.")
                .defineInRange("holdToConfirmTicks", 20, 0, 100);
        HUGE_SPELL_BLOCKS = builder.comment("A spell over this many blocks needs the longer hold.")
                .defineInRange("hugeSpellBlocks", 1000000, 1000, 2147483647);
        HUGE_HOLD_TICKS = builder.comment("How long that hold is.")
                .defineInRange("hugeHoldTicks", 40, 0, 200);

        ALL_ARENAS_PROTECTED = builder.comment("No Build Magic spell and no Magnetic Pull changes a block in the arena of any running boss fight,",
                        "Thanos's included. Off: only the protected arenas (and always the island in the sky). One switch for both,",
                        "so the two never disagree about where they may change a block.")
                .define("allArenasProtected", true);
        HEX_IS_A_CLAIM = builder.comment("Inside a Hex, only its owner and the players she allowed may build with Build Magic or pull with the",
                        "Magnetic Pull. (Telekinesis on one block asks the claims as a hand does, and is not covered.)")
                .define("hexIsAClaim", false);
        FULL_CONTAINERS = builder.comment("A container that holds items in a spell's way: SKIP leaves it as it is, SPILL spills its items on the spot.",
                        "A Move always carries it.")
                .defineEnum("fullContainers", FullContainers.SKIP);
        LIFT_CREATURES = builder.comment("Creatures inside new blocks are lifted on top (other players only with PvP on; never a creature Mjolnir",
                        "pins). Off: their cells stay empty.")
                .define("liftCreatures", true);
        MOVE_CARRIES_CREATURES = builder.comment("Move takes the creatures, frames, paintings and armour stands inside along (not one Mjolnir pins).")
                .define("moveCarriesCreatures", true);
        HOLD_GRAVITY_BLOCKS = builder.comment("Sand, gravel and other falling blocks a spell places in the air stay where magic put them until",
                        "something touches them.")
                .define("holdGravityBlocks", true);
        POST_EVENTS_PER_BLOCK = builder.comment("Ask claim mods (the break and place events) for every block; off: once per 16x16x16 section.")
                .define("postEventsPerBlock", true);
        PREVIEW_CHECKS = builder.comment("Blocks the server checks to preview a spell (more: a spread sample); 0: no marks.")
                .defineInRange("previewChecks", 8192, 0, 65536);

        UNDO_SURVIVES_RESTART = builder.comment("The undo history and the pocket are kept on disk with the world.")
                .define("undoSurvivesRestart", true);
        UNDO_MEMORY_MB = builder.comment("Memory (MiB) one player's undo history may use; past it the oldest spells leave memory (they stay on disk).")
                .defineInRange("undoMemoryMb", 64, 4, 4096);
        UNDO_DISK_MB = builder.comment("Disk (MiB) one player's undo history may use; past it the oldest spells are forgotten.")
                .defineInRange("undoDiskMb", 512, 0, 1048576);
        SERVER_UNDO_MEMORY_MB = builder.comment("Memory (MiB) all undo histories together may use; past it, the spells of the player using the most",
                        "leave memory (they stay on disk).")
                .defineInRange("serverUndoMemoryMb", 512, 64, 65536);
        SERVER_UNDO_DISK_MB = builder.comment("Disk (MiB) all undo histories together may use; past it the oldest spell of the player using the most",
                        "is forgotten.")
                .defineInRange("serverUndoDiskMb", 4096, 0, 4194304);
        POCKET_MAX_MB = builder.comment("The biggest copy (compressed, MiB) one player may hold.")
                .defineInRange("pocketMaxMb", 64, 1, 4096);
        FINISH_ON_STOP_BLOCKS = builder.comment("When the server stops, the spells still working are finished while their blocks left add up to at most",
                        "this; the largest are stopped first (and can be undone).")
                .defineInRange("finishOnStopBlocks", 200000, 0, 10000000);
        MAX_TICKET_CHUNKS = builder.comment("Chunks one spell keeps loaded at once.")
                .defineInRange("maxTicketChunks", 256, 16, 4096);
        MAX_REQUESTS_PER_SECOND = builder.comment("Build messages a client may send per second; more are dropped.")
                .defineInRange("maxRequestsPerSecond", 40, 5, 200);
        DENIED_DIMENSIONS = builder.comment("Dimensions where Build Magic does not work (ids, e.g. \"minecraft:the_end\").")
                .defineListAllowEmpty("deniedDimensions", List.of(), () -> "minecraft:the_end",
                        element -> element instanceof String);
        builder.pop();

        builder.push(WANDA_SECTION);
        WANDA_ENABLED = builder.comment("The Scarlet Witch has Build Magic.")
                .define("enabled", true);
        WANDA_REACH = builder.comment("How far (blocks) she points: anchors, shapes, picks. Her spells themselves have no size limit.")
                .defineInRange("reach", 256, 16, 1024);
        WANDA_UNDO_STEPS = builder.comment("0: as many spells as build_magic.undoMemoryMb and undoDiskMb hold. Otherwise the most she can undo.")
                .defineInRange("undoSteps", 0, 0, 100000);
        WANDA_MAX_BLOCKS = builder.comment("0: no ceiling. Otherwise the most blocks one of her spells may cover (for big public servers).")
                .defineInRange("maxBlocks", 0, 0, 2147483647);
        WANDA_COPIES_CONTENTS = builder.comment("Her copies keep what is inside chests and other containers.")
                .define("copiesContents", true);
        WANDA_VOLATILE_PER_SPELL = builder.comment("The most TNT, lava and fire blocks one of her spells may make.")
                .defineInRange("volatilePerSpell", 4096, 0, 1000000);
        WANDA_HEX_BOOST = builder.comment("Inside her own Hex her spells get this many shares of the server's work.")
                .defineInRange("hexBoost", 2.0, 1.0, 8.0);
        WANDA_MAGIC_SELECT_MAX = builder.comment("The most blocks her double click selects (a selection is held block by block, so it stops at a million).")
                .defineInRange("magicSelectMax", 100000, 1000, 1000000);
        WANDA_BRUSH_MAX = builder.comment("Her biggest Paint and Land brush (radius). Technical, not a limit of her magic: a brush pulse every 4 ticks",
                        "of radius 16 is about 17,000 blocks, which the server keeps up with while she sweeps; at 32 (137,000) the",
                        "stroke would trail seconds behind the cursor.")
                .defineInRange("brushMax", 16, 1, 64);
        WANDA_SHAPE_MAX_RADIUS = builder.comment("Her biggest shape (radius).")
                .defineInRange("shapeMaxRadius", 512, 8, 1024);
        WANDA_REPEAT_MAX = builder.comment("0: no count, only the spell's blocks. Otherwise the most copies in a Repeat.")
                .defineInRange("repeatMax", 0, 0, 65536);
        WANDA_INSTANT_MAX_BLOCKS = builder.comment("Her Instant level (3.2): while its switch is on in build mode, a spell, an undo or a redo of at most this many",
                        "blocks is written all at once, in the tick it is cast (no growing, no waiting). A bigger one goes at normal speed and",
                        "she is told. 0: no Instant level at all. Every rule still holds: arenas, claims, Hexes and loaded chunks only.",
                        "The server stands still while it writes: 250,000 blocks take about a second, a million about four (with the undo kept",
                        "on disk), so raise this only on a strong server.")
                .defineInRange("instantMaxBlocks", 250000, 0, 100000000);
        WANDA_INSTANT_MAX_MILLIS = builder.comment("The longest (milliseconds) one Instant spell may hold the server's tick (never more than a third of",
                        "the server's max-tick-time, which kills a server stuck for longer). When the time is up, the blocks left are written at",
                        "normal speed over the next ticks and she is told.")
                .defineInRange("instantMaxMillis", 5000, 100, 50000);
        builder.pop();

        builder.push(BILLY_SECTION);
        BILLY_ENABLED = builder.comment("Billy has Build Magic.")
                .define("enabled", true);
        BILLY_METER = builder.comment("His magic meter (blocks) as an Apprentice; it doubles with each rank to rank 4, and reaches 64 times as much at rank 10.")
                .defineInRange("meter", 512, 16, 65536);
        BILLY_REFILL = builder.comment("Blocks of magic he gets back each second as an Apprentice; half of it more each rank to rank 4, 12 times as much at rank 10.")
                .defineInRange("refill", 16.0, 0.0, 4096.0);
        BILLY_MAX_SIDE = builder.comment("The longest side of anything he builds, copies or selects as an Apprentice; +8 each rank (96 at rank 10).")
                .defineInRange("maxSide", 24, 1, 256);
        BILLY_REACH = builder.comment("How far (blocks) he points as an Apprentice; +8 each rank (96 at rank 10).")
                .defineInRange("reach", 24, 2, 128);
        BILLY_UNDO_STEPS = builder.comment("Spells he can undo as an Apprentice; +2 each rank (28 at rank 10).")
                .defineInRange("undoSteps", 10, 1, 100);
        BILLY_REPEAT_MAX = builder.comment("The most copies in a Repeat as an Apprentice; +4 each rank (44 at rank 10).")
                .defineInRange("repeatMax", 8, 1, 64);
        String[] names = {null, "a Spellcaster", "a Sorcerer", "Wiccan", "an Enchanter", "a Mystic", "a Wizard", "an Archmage", "a Reality Weaver",
                "the Chaos Master"};
        int[] practice = {0, 2000, 8000, 30000, 60000, 120000, 220000, 380000, 600000, 1000000};
        for (int rank = 2; rank <= BILLY_PRACTICE.length; rank++) {
            BILLY_PRACTICE[rank - 1] = builder.comment("Blocks of magic he must cast to become " + names[rank - 1] + " (rank " + rank + ").")
                    .defineInRange("rank" + rank + "Practice", practice[rank - 1], 0, 10000000);
        }
        BILLY_MAX_RANK = builder.comment("The highest rank he can reach (1: he never grows; 10: the Chaos Master). Ranks 1 to 4 are those of 3.1.")
                .defineInRange("maxRank", 10, 1, 10);
        BILLY_CAST_PAUSE = builder.comment("Ticks after one of his spells before the next.")
                .defineInRange("castPause", 10, 0, 200);
        BILLY_MOM_RANGE = builder.comment("How near (blocks) a Scarlet Witch must be to help him (0: never).")
                .defineInRange("momRange", 16, 0, 128);
        BILLY_MOM_COST_FACTOR = builder.comment("What his spells cost near her (0.5: half).")
                .defineInRange("momCostFactor", 0.5, 0.0, 1.0);
        BILLY_COPIES_CONTENTS = builder.comment("His copies keep what is inside containers (off: they come out empty, so nothing is duplicated).")
                .define("copiesContents", false);
        BILLY_LIMITS_IN_CREATIVE = builder.comment("His limits apply in Creative too.")
                .define("limitsInCreative", true);
        builder.pop();
    }

    private BuildConfig() {
    }
}
