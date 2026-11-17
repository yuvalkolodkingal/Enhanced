package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Witchcraft (3.17): the witches' paths and their spells, the grimoires' spells, Billy's spells and his powers over space
 * and reality, the Coven's Road and the Witches' Mindscape. TOML section {@code [witchcraft]}; read when used.
 */
public final class WitchcraftConfig {
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.DoubleValue MANA_REGEN;
    public static ConfigSpec.IntValue COVEN_RADIUS;
    public static ConfigSpec.DoubleValue SPELL_DAMAGE;
    public static ConfigSpec.DoubleValue INTERRUPT_DAMAGE;
    public static ConfigSpec.DoubleValue CHANT_HELD_SPEED;
    public static ConfigSpec.IntValue CORRUPTION_PER_SPELL;
    public static ConfigSpec.BooleanValue SPELLS_HURT_PLAYERS;
    public static ConfigSpec.IntValue TEMP_BLOCK_SECONDS;
    public static ConfigSpec.IntValue HALLWAY_MAX_SECONDS;
    public static ConfigSpec.BooleanValue ROAD_REVIVES;
    public static ConfigSpec.IntValue ROAD_PORTAL_SECONDS;
    public static ConfigSpec.BooleanValue MINDSCAPE_PRIVATE_CHAT;
    public static ConfigSpec.BooleanValue ROAD_OFFERS_WITCHCRAFT;

    public static void define(ConfigSpec.Builder builder) {
        builder.comment("Witchcraft (3.17): a witch chooses a path (Green, Potions, Protection, Divination, or the rare Spirit witch",
                "who learns them all); a sorcerer has the mystic arts. The Spells key (the semicolon key unless changed) casts the",
                "chosen spell (hold it while chanting to chant faster; a heavy blow breaks the incantation off), and with Sneak opens",
                "the Spell Wheel; a witch's R G V B N (and with Sneak) are her first ten spells. The grimoires teach their own spells",
                "(the Darkhold's corrupt). A hero casts spells only with Ultimate Mode on: Billy as the Demiurge has all of them.")
                .push("witchcraft");
        ENABLED = builder.comment("Whether witchcraft and the spells are in the game at all.")
                .define("enabled", true);
        MANA_REGEN = builder.comment("Mana a caster gets back each second, before their level (twice as much near another caster: a coven).")
                .defineInRange("manaRegen", 3.0, 0.0, 100.0);
        COVEN_RADIUS = builder.comment("How near (blocks) another witch, sorcerer or Billy must be for the coven's faster mana.")
                .defineInRange("covenRadius", 16, 1, 128);
        SPELL_DAMAGE = builder.comment("How many times as much the spells hurt (1: as designed; spells grow with the caster's level).")
                .defineInRange("spellDamageFactor", 1.0, 0.0, 10.0);
        INTERRUPT_DAMAGE = builder.comment("A blow of at least this much damage breaks an incantation off (0: nothing does).")
                .defineInRange("interruptDamage", 4.0, 0.0, 100.0);
        CHANT_HELD_SPEED = builder.comment("How many times as fast an incantation goes while the Spells key is held.")
                .defineInRange("chantHeldSpeed", 2.0, 1.0, 10.0);
        CORRUPTION_PER_SPELL = builder.comment("How much corruption each of the Darkhold's spells adds (out of 100).")
                .defineInRange("corruptionPerSpell", 6, 0, 100);
        SPELLS_HURT_PLAYERS = builder.comment("Whether harmful spells touch other players (PvP must be on too).")
                .define("spellsHurtPlayers", true);
        TEMP_BLOCK_SECONDS = builder.comment("Seconds a conjured block (a bridge, a wall, a platform, thorns) lasts before it vanishes.")
                .defineInRange("conjuredBlockSeconds", 30, 3, 600);
        HALLWAY_MAX_SECONDS = builder.comment("The longest an Infinite Hallway holds anyone (it grows with Billy's rank up to this).")
                .defineInRange("hallwayMaxSeconds", 29, 3, 120);
        ROAD_REVIVES = builder.comment("Whether the Coven's Road may bring back the dead (a hardcore world's fallen players).")
                .define("roadRevives", true);
        ROAD_PORTAL_SECONDS = builder.comment("Seconds a portal to the Coven's Road stays open for others to step in.")
                .defineInRange("roadPortalSeconds", 15, 3, 120);
        MINDSCAPE_PRIVATE_CHAT = builder.comment("Whether chat in the Witches' Mindscape is heard only by those in it.")
                .define("mindscapePrivateChat", true);
        ROAD_OFFERS_WITCHCRAFT = builder.comment("Whether the end of the Coven's Road offers witchcraft to someone with no power.")
                .define("roadOffersWitchcraft", true);
        builder.pop();
    }

    private WitchcraftConfig() {
    }
}
