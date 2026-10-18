package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of Professor X (Charles Xavier, 3.3). Its options are defined in {@link #define}, which {@code
 * EnhancedConfig} calls while it builds the server config (TOML section {@code [professor_x]}); they are read from the
 * server and synced to clients, and read when used (never cached), so an edit applies at once.
 *
 * <p>Times are in ticks (20 to the second) unless the name says seconds.
 */
public final class XavierConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "professor_x";

    // ---- The wheelchair ----
    /**
     * Assigned by {@link #define} when the config is built, which is at mod construction: before anything reads it (it
     * cannot be final because the options are defined in a method, not in a static block).
     */
    public static ModConfigSpec.IntValue WHEELCHAIR_RITUAL_TICKS;

    // ---- Mental Blast (R) ----
    public static ModConfigSpec.IntValue BLAST_RANGE;
    public static ModConfigSpec.DoubleValue BLAST_DAMAGE;
    public static ModConfigSpec.IntValue BLAST_DIZZY_TICKS;
    public static ModConfigSpec.IntValue BLAST_COOLDOWN;

    // ---- Hear Thoughts (G) ----
    public static ModConfigSpec.IntValue THOUGHTS_RANGE;

    // ---- Take Over a Body (V) ----
    public static ModConfigSpec.IntValue LINK_RANGE;
    public static ModConfigSpec.IntValue LINK_MAX_DISTANCE;
    public static ModConfigSpec.IntValue LINK_CREATURE_TICKS;
    public static ModConfigSpec.IntValue LINK_PLAYER_TICKS;
    public static ModConfigSpec.IntValue LINK_COOLDOWN;
    public static ModConfigSpec.BooleanValue LINK_BREAKS_ON_HURT;
    public static ModConfigSpec.DoubleValue BACKLASH_DAMAGE;
    public static ModConfigSpec.IntValue BACKLASH_STUN_TICKS;

    // ---- Freeze Minds (B) ----
    public static ModConfigSpec.IntValue FREEZE_RADIUS;
    public static ModConfigSpec.IntValue FREEZE_TICKS;
    public static ModConfigSpec.IntValue FREEZE_COOLDOWN;

    // ---- Cerebro (N) ----
    public static ModConfigSpec.IntValue CEREBRO_RANGE;
    public static ModConfigSpec.IntValue CEREBRO_TICKS;
    public static ModConfigSpec.IntValue CEREBRO_COOLDOWN;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        WHEELCHAIR_RITUAL_TICKS = builder.comment("How long (ticks) right-click is held on Professor X's wheelchair to awaken as him.")
                .defineInRange("wheelchairRitualTicks", 60, 1, 600);

        BLAST_RANGE = builder.comment("How far (blocks) the Mental Blast reaches (line of sight needed).")
                .defineInRange("blastRange", 24, 4, 64);
        BLAST_DAMAGE = builder.comment("Damage of the Mental Blast (it ignores armour; players only with PvP on).")
                .defineInRange("blastDamage", 6.0, 0.0, 100.0);
        BLAST_DIZZY_TICKS = builder.comment("How long whoever the Mental Blast hits is dizzy and slowed.")
                .defineInRange("blastDizzyTicks", 60, 0, 600);
        BLAST_COOLDOWN = builder.comment("Cooldown of the Mental Blast (ability 1), in ticks.")
                .defineInRange("blastCooldown", 40, 0, 72000);

        THOUGHTS_RANGE = builder.comment("How far away (blocks) Professor X hears the thoughts of players and creatures while Hear Thoughts is on.")
                .defineInRange("thoughtsRange", 24, 4, 64);

        LINK_RANGE = builder.comment("How far (blocks) Professor X can reach a body to take it over (line of sight needed).")
                .defineInRange("linkRange", 24, 2, 64);
        LINK_MAX_DISTANCE = builder.comment("A body he steers that goes further than this (blocks) from his own is let go.")
                .defineInRange("linkMaxDistance", 48, 4, 128);
        LINK_CREATURE_TICKS = builder.comment("How long (ticks) he can stay in a creature's body (600 = 30 s).")
                .defineInRange("linkCreatureTicks", 600, 20, 6000);
        LINK_PLAYER_TICKS = builder.comment("How long (ticks) he can steer a player's body (needs PvP). 0 turns it off for players.")
                .defineInRange("linkPlayerTicks", 160, 0, 1200);
        LINK_COOLDOWN = builder.comment("Cooldown of Take Over a Body (ability 3) after he leaves a body or is thrown out, in ticks.")
                .defineInRange("linkCooldown", 300, 0, 72000);
        LINK_BREAKS_ON_HURT = builder.comment("If true, any damage to his own body (left behind while he steers another) brings him back.")
                .define("linkBreaksOnHurt", true);
        BACKLASH_DAMAGE = builder.comment("Damage he takes when a stronger telepath (the Scarlet Witch, the Phoenix, Billy) throws him out.")
                .defineInRange("backlashDamage", 4.0, 0.0, 40.0);
        BACKLASH_STUN_TICKS = builder.comment("How long he is stunned (dizzy and slowed) after he is thrown out.")
                .defineInRange("backlashStunTicks", 60, 0, 600);

        FREEZE_RADIUS = builder.comment("Radius (blocks) of Freeze Minds.")
                .defineInRange("freezeRadius", 12, 2, 48);
        FREEZE_TICKS = builder.comment("How long Freeze Minds holds everyone still (players only with PvP on).")
                .defineInRange("freezeTicks", 100, 10, 1200);
        FREEZE_COOLDOWN = builder.comment("Cooldown of Freeze Minds (ability 4), in ticks (600 = 30 s).")
                .defineInRange("freezeCooldown", 600, 0, 72000);

        CEREBRO_RANGE = builder.comment("How far (blocks) Cerebro finds the minds of players, in his dimension.")
                .defineInRange("cerebroRange", 256, 16, 4096);
        CEREBRO_TICKS = builder.comment("How long Cerebro shows the minds it found (200 = 10 s).")
                .defineInRange("cerebroTicks", 200, 20, 1200);
        CEREBRO_COOLDOWN = builder.comment("Cooldown of Cerebro (ability 5), in ticks (1200 = 60 s).")
                .defineInRange("cerebroCooldown", 1200, 0, 72000);
        builder.pop();
    }

    private XavierConfig() {
    }
}
