package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * The numbers of the Book of the Vishanti (3.2): the places a sorcerer saves and teleports to, and the ward that keeps
 * monsters out of a base. Its options are defined in {@link #define}, which {@code EnhancedConfig} calls while it builds
 * the server config (TOML section {@code [vishanti]}); they are read from the server and synced to clients. Ticks are
 * 1/20 s; "exhaustion" is Minecraft's hunger currency (4.0 of it is one food point); radii are in blocks. Every option is
 * read when it is used, so editing the file changes it live.
 */
public final class VishantiConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "vishanti";

    // ---- the book ----
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue TELEPORT_COOLDOWN;
    public static ModConfigSpec.IntValue TELEPORT_CHARGE_TICKS;
    public static ModConfigSpec.DoubleValue TELEPORT_EXHAUSTION;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DENIED_DIMENSIONS;

    // ---- the ward ----
    public static ModConfigSpec.BooleanValue WARDS_ENABLED;
    public static ModConfigSpec.IntValue MAX_WARDS;
    public static ModConfigSpec.IntValue WARD_COOLDOWN;
    public static ModConfigSpec.DoubleValue WARD_EXHAUSTION;
    public static ModConfigSpec.IntValue WARD_RADIUS_WANDA;
    public static ModConfigSpec.IntValue WARD_RADIUS_BILLY_MIN;
    public static ModConfigSpec.IntValue WARD_RADIUS_BILLY_MAX;
    public static ModConfigSpec.IntValue WARD_RADIUS_LOKI;
    public static ModConfigSpec.IntValue WARD_CHECK_TICKS;
    public static ModConfigSpec.DoubleValue WARD_PUSH;
    public static ModConfigSpec.BooleanValue WARD_BLOCKS_SPAWNS;

    /** The biggest ward there can be, whatever the radii above say (the sweep looks at this much space). */
    public static final int MAX_RADIUS = 128;

    public static void define(ModConfigSpec.Builder builder) {
        builder.comment("The Book of the Vishanti: a sorcerer (Loki, the Scarlet Witch or Billy) saves places and teleports to them,",
                "and raises a ward round a base that no monster can enter.").push(SECTION);
        ENABLED = builder.comment("The Book of the Vishanti works (false: it can be held but nobody can read it).")
                .define("enabled", true);
        TELEPORT_COOLDOWN = builder.comment("Cooldown of the book's teleport, in ticks (1200 = a minute).")
                .defineInRange("teleportCooldown", 600, 0, 72000);
        TELEPORT_CHARGE_TICKS = builder.comment("Ticks the sparks gather round the traveller before the book takes them.")
                .defineInRange("teleportChargeTicks", 20, 1, 200);
        TELEPORT_EXHAUSTION = builder.comment("Food exhaustion of one teleport (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("teleportExhaustion", 6.0, 0.0, 40.0);
        DENIED_DIMENSIONS = builder.comment("Dimensions where the book can neither save a place, raise a ward nor teleport, and that it",
                        "cannot take anyone into or out of (ids). The Witches' Road is always shut to it, whatever this says.")
                .defineListAllowEmpty("deniedDimensions", List.of("enhanced:witches_road"), () -> "enhanced:witches_road",
                        element -> element instanceof String);

        WARDS_ENABLED = builder.comment("Bases can carry a ward (false: bases are only places; existing wards sleep).")
                .define("wardsEnabled", true);
        MAX_WARDS = builder.comment("How many bases (so how many wards) one player can have at a time.")
                .defineInRange("maxWardsPerPlayer", 2, 1, 16);
        WARD_COOLDOWN = builder.comment("Cooldown, in ticks, between raising (or moving) one player's wards.")
                .defineInRange("wardCooldown", 200, 0, 72000);
        WARD_EXHAUSTION = builder.comment("Food exhaustion of raising a ward (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("wardExhaustion", 8.0, 0.0, 40.0);
        WARD_RADIUS_WANDA = builder.comment("Radius of the ward of the Scarlet Witch, the strongest sorcerer: the most a ward can be.")
                .defineInRange("wardRadiusWanda", 48, 4, MAX_RADIUS);
        WARD_RADIUS_BILLY_MIN = builder.comment("Radius of the ward of Billy at his first Build Magic rank (an Apprentice).")
                .defineInRange("wardRadiusBillyMin", 12, 4, MAX_RADIUS);
        WARD_RADIUS_BILLY_MAX = builder.comment("Radius of the ward of Billy at his highest Build Magic rank (build_magic_billy.maxRank);",
                        "between his first and highest rank it grows evenly.")
                .defineInRange("wardRadiusBillyMax", 40, 4, MAX_RADIUS);
        WARD_RADIUS_LOKI = builder.comment("Radius of the ward of Loki.")
                .defineInRange("wardRadiusLoki", 24, 4, MAX_RADIUS);
        WARD_CHECK_TICKS = builder.comment("Every how many ticks a ward looks for monsters inside it and pushes them out (one",
                        "spatial lookup per ward; more is cheaper, fewer keeps the edge tighter).")
                .defineInRange("wardCheckTicks", 5, 1, 40);
        WARD_PUSH = builder.comment("How hard a ward throws a monster at its edge back out (blocks per tick of speed).")
                .defineInRange("wardPush", 0.8, 0.1, 4.0);
        WARD_BLOCKS_SPAWNS = builder.comment("No monster spawns inside a ward (natural spawns, spawners, structures).")
                .define("wardBlocksSpawns", true);
        builder.pop();
    }

    private VishantiConfig() {
    }
}
