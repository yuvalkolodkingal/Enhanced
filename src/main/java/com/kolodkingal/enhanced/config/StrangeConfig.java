package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Doctor Strange, the Eye of Agamotto and the Sling Ring (3.6). Every option of the plan is defined here by the 3.6
 * skeleton (lane L3 reads them; the Time Stone's numbers behind the Eye are {@code StonesConfig}'s); {@code
 * EnhancedConfig} calls {@link #define} while it builds the server config (TOML sections {@code [doctor_strange]},
 * {@code [eye_of_agamotto]} and {@code [sling_ring]}). They are read from the server and synced to clients, and read
 * when used (never cached).
 */
public final class StrangeConfig {
    /** The TOML sections of these options. */
    public static final String SECTION_DOCTOR_STRANGE = "doctor_strange";
    public static final String SECTION_EYE_OF_AGAMOTTO = "eye_of_agamotto";
    public static final String SECTION_SLING_RING = "sling_ring";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ConfigSpec.DoubleValue WHIP_DAMAGE1;
    public static ConfigSpec.DoubleValue WHIP_DAMAGE2;
    public static ConfigSpec.DoubleValue WHIP_DAMAGE3;
    public static ConfigSpec.DoubleValue WHIP_REACH;
    public static ConfigSpec.IntValue GRAPPLE_RANGE;
    public static ConfigSpec.IntValue GRAPPLE_COOLDOWN;
    public static ConfigSpec.IntValue SHIELD_SECONDS;
    public static ConfigSpec.IntValue SHIELD_COOLDOWN;
    public static ConfigSpec.IntValue DOME_SECONDS;
    public static ConfigSpec.DoubleValue DOME_RADIUS;
    public static ConfigSpec.IntValue DOME_COOLDOWN;
    public static ConfigSpec.IntValue BANDS_RANGE;
    public static ConfigSpec.IntValue BANDS_TICKS;
    public static ConfigSpec.IntValue BANDS_BOSS_TICKS;
    public static ConfigSpec.IntValue BANDS_COOLDOWN;
    public static ConfigSpec.IntValue ASTRAL_SECONDS;
    public static ConfigSpec.IntValue ASTRAL_RANGE;
    public static ConfigSpec.IntValue ASTRAL_COOLDOWN;
    public static ConfigSpec.IntValue PORTAL_RANGE;
    public static ConfigSpec.IntValue PORTAL_SECONDS;
    public static ConfigSpec.IntValue PORTAL_COOLDOWN;
    public static ConfigSpec.IntValue MIRROR_RADIUS;
    public static ConfigSpec.IntValue MIRROR_SECONDS;
    public static ConfigSpec.IntValue MIRROR_COOLDOWN;
    public static ConfigSpec.DoubleValue MIRROR_DAMAGE_BONUS;
    public static ConfigSpec.DoubleValue HEALTH_BONUS;
    public static ConfigSpec.DoubleValue ARMOR_BONUS;
    public static ConfigSpec.DoubleValue SAFE_FALL;
    public static ConfigSpec.IntValue WARD_RADIUS;
    public static ConfigSpec.IntValue EYE_AWAKEN_TICKS;
    public static ConfigSpec.BooleanValue EYE_OWNER_ONLY;
    public static ConfigSpec.BooleanValue EYE_NON_SORCERER_MESSAGE;
    public static ConfigSpec.IntValue RING_PLACES;
    public static ConfigSpec.IntValue RING_COOLDOWN;
    public static ConfigSpec.BooleanValue RING_CROSS_DIMENSION;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("doctor_strange");
        FLIGHT_SPEED = builder.comment("Flight speed on the Cloak of Levitation (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.06, 0.01, 0.5);
        WHIP_DAMAGE1 = builder.comment("Damage of the first lash of the Eldritch Whip.")
                .defineInRange("whipDamage1", 7.0, 0.0, 100.0);
        WHIP_DAMAGE2 = builder.comment("Damage of the second lash.")
                .defineInRange("whipDamage2", 7.0, 0.0, 100.0);
        WHIP_DAMAGE3 = builder.comment("Damage of the third lash (it wraps and lifts).")
                .defineInRange("whipDamage3", 10.0, 0.0, 100.0);
        WHIP_REACH = builder.comment("Reach (blocks) of the Eldritch Whip.")
                .defineInRange("whipReach", 8.0, 2.0, 32.0);
        GRAPPLE_RANGE = builder.comment("Range (blocks) of the Whip Grapple.")
                .defineInRange("grappleRange", 24, 4, 64);
        GRAPPLE_COOLDOWN = builder.comment("Cooldown (ticks) of the Whip Grapple.")
                .defineInRange("grappleCooldown", 60, 0, 72000);
        SHIELD_SECONDS = builder.comment("Longest (seconds) the Shield of the Seraphim is held.")
                .defineInRange("shieldSeconds", 10, 1, 120);
        SHIELD_COOLDOWN = builder.comment("Cooldown (ticks) of the Shield of the Seraphim.")
                .defineInRange("shieldCooldown", 120, 0, 72000);
        DOME_SECONDS = builder.comment("Seconds the Seraphim Dome stands.")
                .defineInRange("domeSeconds", 6, 1, 60);
        DOME_RADIUS = builder.comment("Radius (blocks) of the Seraphim Dome.")
                .defineInRange("domeRadius", 4.0, 1.0, 16.0);
        DOME_COOLDOWN = builder.comment("Cooldown (ticks) of the Seraphim Dome.")
                .defineInRange("domeCooldown", 400, 0, 72000);
        BANDS_RANGE = builder.comment("Range (blocks) of the Crimson Bands of Cyttorak.")
                .defineInRange("bandsRange", 20, 4, 64);
        BANDS_TICKS = builder.comment("Ticks the Crimson Bands hold a creature.")
                .defineInRange("bandsTicks", 80, 10, 600);
        BANDS_BOSS_TICKS = builder.comment("Ticks the Crimson Bands hold a boss.")
                .defineInRange("bandsBossTicks", 20, 0, 600);
        BANDS_COOLDOWN = builder.comment("Cooldown (ticks) of the Crimson Bands.")
                .defineInRange("bandsCooldown", 200, 0, 72000);
        ASTRAL_SECONDS = builder.comment("Longest (seconds) the Astral Projection lasts.")
                .defineInRange("astralSeconds", 30, 5, 300);
        ASTRAL_RANGE = builder.comment("How far (blocks) the spirit may go from the body.")
                .defineInRange("astralRange", 64, 8, 256);
        ASTRAL_COOLDOWN = builder.comment("Cooldown (ticks) of the Astral Projection.")
                .defineInRange("astralCooldown", 300, 0, 72000);
        PORTAL_RANGE = builder.comment("Range (blocks) of the Sling-Ring Portal's far side.")
                .defineInRange("portalRange", 64, 4, 256);
        PORTAL_SECONDS = builder.comment("Seconds a pair of portals stays open.")
                .defineInRange("portalSeconds", 8, 1, 60);
        PORTAL_COOLDOWN = builder.comment("Cooldown (ticks) of the Sling-Ring Portal.")
                .defineInRange("portalCooldown", 100, 0, 72000);
        MIRROR_RADIUS = builder.comment("Radius (blocks) of the Mirror Dimension.")
                .defineInRange("mirrorRadius", 14, 4, 48);
        MIRROR_SECONDS = builder.comment("Seconds the Mirror Dimension lasts.")
                .defineInRange("mirrorSeconds", 20, 5, 120);
        MIRROR_COOLDOWN = builder.comment("Cooldown (ticks) of the Mirror Dimension.")
                .defineInRange("mirrorCooldown", 1200, 0, 72000);
        MIRROR_DAMAGE_BONUS = builder.comment("How much more his spells hurt inside the Mirror Dimension (0.2: +20%).")
                .defineInRange("mirrorDamageBonus", 0.2, 0.0, 2.0);
        HEALTH_BONUS = builder.comment("Health his body adds.")
                .defineInRange("healthBonus", 6.0, 0.0, 40.0);
        ARMOR_BONUS = builder.comment("Armour his body adds.")
                .defineInRange("armorBonus", 2.0, 0.0, 20.0);
        SAFE_FALL = builder.comment("Falls (blocks) the Cloak catches without damage.")
                .defineInRange("safeFall", 12.0, 0.0, 64.0);
        WARD_RADIUS = builder.comment("Radius of the ward he raises with the Book of the Vishanti (he is a sorcerer;",
                "the Scarlet Witch's default).")
                .defineInRange("wardRadius", 32, 0, 128);
        builder.pop();
        builder.push("eye_of_agamotto");
        EYE_AWAKEN_TICKS = builder.comment("Ticks a powerless player holds the Eye to become Doctor Strange.")
                .defineInRange("awakenTicks", 60, 10, 400);
        EYE_OWNER_ONLY = builder.comment("Whether an Eye bound to a Doctor Strange awakens nobody else while he is",
                "one.")
                .define("ownerOnly", true);
        EYE_NON_SORCERER_MESSAGE = builder.comment("Whether a non-sorcerer is told that the Eye stays closed.")
                .define("nonSorcererMessage", true);
        builder.pop();
        builder.push("sling_ring");
        RING_PLACES = builder.comment("How many places a Sling Ring keeps.")
                .defineInRange("places", 8, 1, 16);
        RING_COOLDOWN = builder.comment("Cooldown (ticks) of the Sling Ring's portal to a place.")
                .defineInRange("cooldown", 200, 0, 72000);
        RING_CROSS_DIMENSION = builder.comment("Whether the Sling Ring opens a portal to a place in another dimension",
                "(never Asgard).")
                .define("crossDimension", true);
        builder.pop();
    }

    private StrangeConfig() {
    }
}
