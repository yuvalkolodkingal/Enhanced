package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The world's own music (3.5): the songs a server owner puts in {@code <world>/serverconfig/enhanced/music/} to replace the
 * Walkman songs and the Witches' Road for every player. TOML section {@code [music]}; {@code EnhancedConfig} calls
 * {@link #define}. Read when used, never cached.
 */
public final class MusicConfig {
    public static final String SECTION = "music";

    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue MAX_FILE_KB;
    public static ModConfigSpec.IntValue CHUNKS_PER_TICK;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Send the songs in <world>/serverconfig/enhanced/music/ to the players (see the README.txt there).",
                        "Off: every player hears the built-in music (or his own).")
                .define("enabled", true);
        MAX_FILE_KB = builder.comment("The largest song file the server sends, in KB; a bigger one is left out (and logged).")
                .defineInRange("maxFileKB", 8192, 16, 65536);
        CHUNKS_PER_TICK = builder.comment("How many pieces of a song (30 KB each) a player is sent per tick while he downloads one",
                        "(2 = about 1.2 MB a second).")
                .defineInRange("chunksPerTick", 2, 1, 16);
        builder.pop();
    }

    /** The largest file sent, in bytes (8 MB until the config has loaded). */
    public static int maxBytes() {
        return (MAX_FILE_KB == null || !com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded() ? 8192 : MAX_FILE_KB.get()) * 1024;
    }

    public static boolean enabled() {
        return ENABLED == null || !com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded() || ENABLED.get();
    }

    public static int chunksPerTick() {
        return CHUNKS_PER_TICK == null || !com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded() ? 2 : CHUNKS_PER_TICK.get();
    }

    private MusicConfig() {
    }
}
