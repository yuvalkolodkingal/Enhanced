package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The numbers of the Hex Runes (3.3): the runes the Scarlet Witch inscribes on the four walls of her Hex, which leave every
 * other sorcerer inside it without powers. Its options are defined in {@link #define}, which {@code EnhancedConfig} calls while
 * it builds the server config (TOML section {@code [hex_runes]}); they are read from the server and synced to clients. Ticks
 * are 1/20 s; "exhaustion" is Minecraft's hunger currency (4.0 of it is one food point). Every option is read when it is used,
 * so editing the file changes it live.
 */
public final class HexRunesConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "hex_runes";

    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue COOLDOWN;
    public static ModConfigSpec.DoubleValue EXHAUSTION;

    public static void define(ModConfigSpec.Builder builder) {
        builder.comment("3.3: the Hex Runes. The Scarlet Witch inscribes them on the four walls of her Hex (and wipes them) from her Hex",
                "Control screen (N with a Hex up). While they glow, every other sorcerer inside the Hex cannot use powers: Loki, Billy,",
                "another Scarlet Witch, and anyone holding the Darkhold or the Book of the Vishanti. She keeps hers.").push(SECTION);
        ENABLED = builder.comment("The Hex Runes can be inscribed (false: the button does nothing and runes already up do nothing).")
                .define("enabled", true);
        COOLDOWN = builder.comment("Ticks after inscribing the runes before she can inscribe them again (wiping them is free and at once).")
                .defineInRange("cooldown", 100, 0, 72000);
        EXHAUSTION = builder.comment("Food exhaustion inscribing the runes costs (4.0 = 1 food point; Creative is free). On an empty stomach she",
                        "cannot inscribe them.")
                .defineInRange("exhaustion", 4.0, 0.0, 40.0);
        builder.pop();
    }

    private HexRunesConfig() {
    }
}
