package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Deadpool and Dogpool (3.7). Every option of the plan is defined here by the 3.7 skeleton (lane L3 reads them);
 * {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML sections {@code [deadpool]} and
 * {@code [dogpool]}). They are read from the server and read when used (never cached).
 */
public final class DeadpoolConfig {
    /** The TOML sections of these options. */
    public static final String SECTION_DEADPOOL = "deadpool";
    public static final String SECTION_DOGPOOL = "dogpool";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.DoubleValue HEALTH_BONUS;
    public static ModConfigSpec.DoubleValue SPEED_BONUS;
    public static ModConfigSpec.DoubleValue ATTACK_BONUS;
    public static ModConfigSpec.DoubleValue ATTACK_SPEED_BONUS;
    public static ModConfigSpec.DoubleValue JUMP_BONUS;
    public static ModConfigSpec.DoubleValue SAFE_FALL;
    public static ModConfigSpec.DoubleValue KATANA_DAMAGE_1;
    public static ModConfigSpec.DoubleValue KATANA_DAMAGE_2;
    public static ModConfigSpec.DoubleValue KATANA_DAMAGE_3;
    public static ModConfigSpec.DoubleValue KATANA_REACH;
    public static ModConfigSpec.IntValue KATANA_GAP;
    public static ModConfigSpec.IntValue PARRY_MAX_TICKS;
    public static ModConfigSpec.DoubleValue PARRY_MELEE_REDUCTION;
    public static ModConfigSpec.IntValue PARRY_COOLDOWN;
    public static ModConfigSpec.DoubleValue PISTOL_DAMAGE;
    public static ModConfigSpec.IntValue PISTOL_RANGE;
    public static ModConfigSpec.IntValue MAGAZINE;
    public static ModConfigSpec.IntValue RELOAD_TICKS;
    public static ModConfigSpec.IntValue SHOT_GAP;
    public static ModConfigSpec.DoubleValue TRICK_SHOT_DAMAGE;
    public static ModConfigSpec.IntValue TRICK_SHOT_SEEK;
    public static ModConfigSpec.IntValue TRICK_SHOT_COOLDOWN;
    public static ModConfigSpec.IntValue BLINK_RANGE;
    public static ModConfigSpec.IntValue BLINK_COOLDOWN;
    public static ModConfigSpec.DoubleValue GLITCH_CHANCE;
    public static ModConfigSpec.IntValue DROP_IN_RANGE;
    public static ModConfigSpec.IntValue DROP_IN_HEIGHT;
    public static ModConfigSpec.DoubleValue DROP_IN_DAMAGE;
    public static ModConfigSpec.IntValue DROP_IN_COOLDOWN;
    public static ModConfigSpec.IntValue TAUNT_RADIUS;
    public static ModConfigSpec.IntValue TAUNT_TICKS;
    public static ModConfigSpec.IntValue TAUNT_COOLDOWN;
    public static ModConfigSpec.IntValue SMOKE_TICKS;
    public static ModConfigSpec.IntValue SMOKE_COOLDOWN;
    public static ModConfigSpec.DoubleValue CHIMICHANGA_FRACTION;
    public static ModConfigSpec.BooleanValue POD_AWAKENS;
    public static ModConfigSpec.IntValue POD_TICKS;
    public static ModConfigSpec.BooleanValue LINES_ENABLED;
    public static ModConfigSpec.IntValue LINE_GAP_TICKS;
    public static ModConfigSpec.BooleanValue DOGPOOL_ENABLED;
    public static ModConfigSpec.IntValue DOGPOOL_SECONDS;
    public static ModConfigSpec.DoubleValue DOGPOOL_HEALTH;
    public static ModConfigSpec.DoubleValue DOGPOOL_BITE_DAMAGE;
    public static ModConfigSpec.IntValue DOGPOOL_COOLDOWN;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("deadpool");
        HEALTH_BONUS = builder.comment("Max health added to his 20.")
                .defineInRange("healthBonus", 10.0, 0.0, 100.0);
        SPEED_BONUS = builder.comment("Movement speed added, as a fraction.")
                .defineInRange("speedBonus", 0.15, 0.0, 2.0);
        ATTACK_BONUS = builder.comment("Attack damage added to every blow.")
                .defineInRange("attackBonus", 2.0, 0.0, 40.0);
        ATTACK_SPEED_BONUS = builder.comment("Attack speed added.")
                .defineInRange("attackSpeedBonus", 0.4, 0.0, 4.0);
        JUMP_BONUS = builder.comment("Jump strength added.")
                .defineInRange("jumpBonus", 0.1, 0.0, 2.0);
        SAFE_FALL = builder.comment("Safe fall distance added (blocks).")
                .defineInRange("safeFall", 3.0, 0.0, 64.0);
        KATANA_DAMAGE_1 = builder.comment("Damage of the first hit of the katana combo.")
                .defineInRange("katanaDamage1", 5.0, 0.0, 100.0);
        KATANA_DAMAGE_2 = builder.comment("Damage of the second hit.")
                .defineInRange("katanaDamage2", 5.0, 0.0, 100.0);
        KATANA_DAMAGE_3 = builder.comment("Damage of the third hit (the X-cut).")
                .defineInRange("katanaDamage3", 8.0, 0.0, 100.0);
        KATANA_REACH = builder.comment("Reach (blocks) of the katanas.")
                .defineInRange("katanaReach", 3.5, 1.0, 8.0);
        KATANA_GAP = builder.comment("Ticks between two hits of the combo.")
                .defineInRange("katanaGap", 8, 1, 100);
        PARRY_MAX_TICKS = builder.comment("The longest (ticks) the Parry can be held.")
                .defineInRange("parryMaxTicks", 80, 10, 600);
        PARRY_MELEE_REDUCTION = builder.comment("How much of a blow from the front the Parry takes away.")
                .defineInRange("parryMeleeReduction", 0.6, 0.0, 1.0);
        PARRY_COOLDOWN = builder.comment("Cooldown (ticks) of the Parry.")
                .defineInRange("parryCooldown", 60, 0, 72000);
        PISTOL_DAMAGE = builder.comment("Damage of each pistol shot.")
                .defineInRange("pistolDamage", 4.0, 0.0, 100.0);
        PISTOL_RANGE = builder.comment("Range (blocks) of a shot.")
                .defineInRange("pistolRange", 48, 4, 128);
        MAGAZINE = builder.comment("Shots before the pistols reload.")
                .defineInRange("magazine", 16, 1, 128);
        RELOAD_TICKS = builder.comment("Ticks the reload takes.")
                .defineInRange("reloadTicks", 40, 0, 400);
        SHOT_GAP = builder.comment("Ticks between two pairs of shots.")
                .defineInRange("shotGap", 6, 1, 100);
        TRICK_SHOT_DAMAGE = builder.comment("Damage of the Trick Shot.")
                .defineInRange("trickShotDamage", 8.0, 0.0, 100.0);
        TRICK_SHOT_SEEK = builder.comment("Radius (blocks) round the bounce in which the Trick Shot finds its target.")
                .defineInRange("trickShotSeek", 12, 2, 48);
        TRICK_SHOT_COOLDOWN = builder.comment("Cooldown (ticks) of the Trick Shot.")
                .defineInRange("trickShotCooldown", 80, 0, 72000);
        BLINK_RANGE = builder.comment("Range (blocks) of the Teleport Belt.")
                .defineInRange("blinkRange", 24, 4, 128);
        BLINK_COOLDOWN = builder.comment("Cooldown (ticks) of the Teleport Belt.")
                .defineInRange("blinkCooldown", 40, 0, 72000);
        GLITCH_CHANCE = builder.comment("Chance that the belt glitches and he lands a few blocks off (always on a",
                "safe spot).")
                .defineInRange("glitchChance", 0.1, 0.0, 1.0);
        DROP_IN_RANGE = builder.comment("Range (blocks) of the Drop-In.")
                .defineInRange("dropInRange", 32, 4, 128);
        DROP_IN_HEIGHT = builder.comment("How high (blocks) above the foe the Drop-In teleports him.")
                .defineInRange("dropInHeight", 8, 2, 32);
        DROP_IN_DAMAGE = builder.comment("Damage of the Drop-In's landing.")
                .defineInRange("dropInDamage", 8.0, 0.0, 100.0);
        DROP_IN_COOLDOWN = builder.comment("Cooldown (ticks) of the Drop-In.")
                .defineInRange("dropInCooldown", 160, 0, 72000);
        TAUNT_RADIUS = builder.comment("Radius (blocks) in which monsters turn on him when he taunts.")
                .defineInRange("tauntRadius", 16, 2, 64);
        TAUNT_TICKS = builder.comment("Ticks the taunted monsters keep at him.")
                .defineInRange("tauntTicks", 100, 10, 1200);
        TAUNT_COOLDOWN = builder.comment("Cooldown (ticks) of the Taunt.")
                .defineInRange("tauntCooldown", 200, 0, 72000);
        SMOKE_TICKS = builder.comment("Ticks of Invisibility after the Smoke Vanish.")
                .defineInRange("smokeTicks", 80, 10, 1200);
        SMOKE_COOLDOWN = builder.comment("Cooldown (ticks) of the Smoke Vanish.")
                .defineInRange("smokeCooldown", 400, 0, 72000);
        CHIMICHANGA_FRACTION = builder.comment("Fraction of his max health the Chimichanga Break heals when he eats a",
                "chimichanga.")
                .defineInRange("chimichangaFraction", 1.0, 0.0, 1.0);
        POD_AWAKENS = builder.comment("Whether the Weapon X Pod makes a powerless player Deadpool.")
                .define("podAwakens", true);
        POD_TICKS = builder.comment("Ticks of the pod's treatment.")
                .defineInRange("podTicks", 100, 20, 1200);
        LINES_ENABLED = builder.comment("Whether Deadpool says his joke lines.")
                .define("linesEnabled", true);
        LINE_GAP_TICKS = builder.comment("The fewest ticks between two of his lines.")
                .defineInRange("lineGapTicks", 200, 0, 12000);
        builder.pop();
        builder.push("dogpool");
        DOGPOOL_ENABLED = builder.comment("Whether Deadpool can call Dogpool.")
                .define("enabled", true);
        DOGPOOL_SECONDS = builder.comment("Seconds Dogpool stays.")
                .defineInRange("seconds", 120, 10, 1200);
        DOGPOOL_HEALTH = builder.comment("Dogpool's health.")
                .defineInRange("health", 20.0, 1.0, 200.0);
        DOGPOOL_BITE_DAMAGE = builder.comment("Damage of Dogpool's bite.")
                .defineInRange("biteDamage", 4.0, 0.0, 100.0);
        DOGPOOL_COOLDOWN = builder.comment("Cooldown (ticks) of Dogpool, from the call.")
                .defineInRange("cooldown", 1200, 0, 72000);
        builder.pop();
    }

    private DeadpoolConfig() {
    }
}
