package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The numbers of the speedsters' 3.3 moves (Quicksilver and Tommy): running up walls and along ceilings, Tommy's slow
 * time, and the speed strikes (Dash Strike, Pebble Barrage, Sonic Boom). Its options are defined in {@link #define},
 * which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code [speedsters]}); they are read
 * from the server and synced to clients. Ticks are 1/20 s; speeds are blocks per tick (1.0 = 20 blocks a second);
 * "exhaustion" 4.0 is one food point. Every option is read when it is used, so editing the file changes it live.
 */
public final class SpeedConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "speedsters";

    // ---- wall and ceiling running ----
    public static ConfigSpec.BooleanValue SURFACE_RUN;
    public static ConfigSpec.BooleanValue CEILING_RUN;
    public static ConfigSpec.DoubleValue SURFACE_SPEED_FACTOR;
    public static ConfigSpec.DoubleValue SURFACE_MAX_SPEED;
    public static ConfigSpec.DoubleValue SURFACE_MIN_SPEED;

    // ---- Tommy's slow time ----
    public static ConfigSpec.DoubleValue TOMMY_TIME_EXHAUSTION;

    // ---- Dash Strike ----
    public static ConfigSpec.IntValue DASH_RANGE;
    public static ConfigSpec.DoubleValue DASH_DAMAGE;
    public static ConfigSpec.IntValue DASH_COOLDOWN;

    // ---- Pebble Barrage ----
    public static ConfigSpec.IntValue BARRAGE_PEBBLES;
    public static ConfigSpec.DoubleValue BARRAGE_DAMAGE;
    public static ConfigSpec.IntValue BARRAGE_RANGE;
    public static ConfigSpec.IntValue BARRAGE_COOLDOWN;

    // ---- Sonic Boom ----
    public static ConfigSpec.DoubleValue BOOM_RADIUS;
    public static ConfigSpec.DoubleValue BOOM_DAMAGE;
    public static ConfigSpec.IntValue BOOM_COOLDOWN;

    public static void define(ConfigSpec.Builder builder) {
        builder.comment("The speedsters (Quicksilver and Tommy): running up walls and along ceilings, Tommy's slow time and",
                "the speed strikes.").push(SECTION);
        SURFACE_RUN = builder.comment("Speedsters run up walls they sprint into at super speed (Quicksilver: Wall & Water Run on;",
                        "Tommy: Wall Run on). False: they stop at walls as in 3.2.")
                .define("wallRun", true);
        CEILING_RUN = builder.comment("A speedster running up a wall goes on along a ceiling or roof above it, and round the lip of an overhang.")
                .define("ceilingRun", true);
        SURFACE_SPEED_FACTOR = builder.comment("Speed on a wall or a ceiling, as a part of his speed on the ground (0.8 = four fifths).")
                .defineInRange("surfaceSpeedFactor", 0.8, 0.1, 2.0);
        SURFACE_MAX_SPEED = builder.comment("The fastest he runs on a wall or a ceiling, in blocks per tick (1.0 = 20 blocks a second).")
                .defineInRange("surfaceMaxSpeed", 1.0, 0.1, 3.0);
        SURFACE_MIN_SPEED = builder.comment("Below this speed (blocks per tick) he can no longer hold a wall or a ceiling and drops off.")
                .defineInRange("surfaceMinSpeed", 0.22, 0.01, 1.0);

        TOMMY_TIME_EXHAUSTION = builder.comment("Food exhaustion per tick of Tommy's slow time while he is not running (4.0 = 1 food point).",
                        "(Running costs the exhaustion of his Super Speed instead.)")
                .defineInRange("tommyTimeExhaustion", 0.01, 0.0, 4.0);

        DASH_RANGE = builder.comment("How far the Dash Strike (Sneak + G) carries him, in blocks.")
                .defineInRange("dashRange", 12, 2, 32);
        DASH_DAMAGE = builder.comment("Damage of the Dash Strike to each creature in its path.")
                .defineInRange("dashDamage", 5.0, 0.0, 100.0);
        DASH_COOLDOWN = builder.comment("Cooldown of the Dash Strike, in ticks.")
                .defineInRange("dashCooldown", 100, 0, 72000);

        BARRAGE_PEBBLES = builder.comment("How many pebbles Quicksilver's Pebble Barrage (N) flings.")
                .defineInRange("barragePebbles", 8, 1, 32);
        BARRAGE_DAMAGE = builder.comment("Damage of each pebble (it ignores the hit cooldown, so pebbles add up).")
                .defineInRange("barrageDamage", 1.5, 0.0, 50.0);
        BARRAGE_RANGE = builder.comment("How far the pebbles fly, in blocks.")
                .defineInRange("barrageRange", 24, 4, 64);
        BARRAGE_COOLDOWN = builder.comment("Cooldown of the Pebble Barrage, in ticks.")
                .defineInRange("barrageCooldown", 120, 0, 72000);

        BOOM_RADIUS = builder.comment("Radius of Tommy's Sonic Boom (Sneak + V), in blocks.")
                .defineInRange("sonicBoomRadius", 6.0, 1.0, 16.0);
        BOOM_DAMAGE = builder.comment("Damage of the Sonic Boom (half as much again when he was running at super speed).")
                .defineInRange("sonicBoomDamage", 4.0, 0.0, 100.0);
        BOOM_COOLDOWN = builder.comment("Cooldown of the Sonic Boom, in ticks.")
                .defineInRange("sonicBoomCooldown", 160, 0, 72000);
        builder.pop();
    }

    private SpeedConfig() {
    }
}
