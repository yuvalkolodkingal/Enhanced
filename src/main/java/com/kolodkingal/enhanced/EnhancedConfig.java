package com.kolodkingal.enhanced;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side balance numbers. Lives in world/serverconfig/enhanced-server.toml and is synced to clients.
 */
public final class EnhancedConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- Magneto ----
    public static final ModConfigSpec.DoubleValue MAGNETO_FLIGHT_SPEED;
    public static final ModConfigSpec.IntValue TELEKINESIS_RANGE;
    public static final ModConfigSpec.DoubleValue THROW_SPEED;
    public static final ModConfigSpec.DoubleValue SHRAPNEL_DAMAGE;
    public static final ModConfigSpec.IntValue SHRAPNEL_PIERCE;
    public static final ModConfigSpec.IntValue SHRAPNEL_FIRE_INTERVAL;
    public static final ModConfigSpec.IntValue DISARM_RANGE;
    public static final ModConfigSpec.IntValue DISARM_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SHIELD_REFLECT_CHANCE;

    // ---- Scarlet Witch ----
    public static final ModConfigSpec.DoubleValue WANDA_FLIGHT_SPEED;
    public static final ModConfigSpec.DoubleValue CHAOS_BEAM_DAMAGE;
    public static final ModConfigSpec.IntValue CHAOS_BEAM_RANGE;
    public static final ModConfigSpec.IntValue CHAOS_BEAM_NAUSEA_TICKS;
    public static final ModConfigSpec.DoubleValue CHAOS_SHIELD_EXHAUSTION;
    public static final ModConfigSpec.DoubleValue CHAOS_SHIELD_EXPLOSION_FACTOR;
    public static final ModConfigSpec.IntValue CHAOS_SHIELD_WIND_UP_TICKS;
    public static final ModConfigSpec.IntValue CHAOS_SHIELD_MAX_TICKS;
    public static final ModConfigSpec.DoubleValue CHAOS_SHIELD_STRENGTH;
    public static final ModConfigSpec.IntValue CHAOS_SHIELD_COOLDOWN;
    public static final ModConfigSpec.IntValue CHAOS_SHIELD_BROKEN_COOLDOWN;
    public static final ModConfigSpec.IntValue BLOCK_LIFT_RANGE;
    public static final ModConfigSpec.DoubleValue BLOCK_LIFT_MAX_HARDNESS;
    public static final ModConfigSpec.IntValue HEX_DEFAULT_RADIUS;
    public static final ModConfigSpec.IntValue HEX_MIN_RADIUS;
    public static final ModConfigSpec.IntValue HEX_MAX_RADIUS;
    public static final ModConfigSpec.DoubleValue HEX_DEATH_CHANCE;
    public static final ModConfigSpec.DoubleValue HEX_HOLD_DAMAGE;
    public static final ModConfigSpec.DoubleValue HEX_HOLD_DAMAGE_MAX;
    public static final ModConfigSpec.DoubleValue HEX_HOLD_RELEASE_HEALTH;

    // ---- The Vision of the Hex ----
    public static final ModConfigSpec.BooleanValue HEX_VISION_ENABLED;
    public static final ModConfigSpec.DoubleValue HEX_VISION_HEALTH;
    public static final ModConfigSpec.DoubleValue HEX_VISION_PUNCH_DAMAGE;
    public static final ModConfigSpec.DoubleValue HEX_VISION_BEAM_DAMAGE;
    public static final ModConfigSpec.IntValue HEX_VISION_BEAM_RANGE;
    public static final ModConfigSpec.IntValue HEX_VISION_REBUILD_TICKS;

    // ---- Vision ----
    public static final ModConfigSpec.DoubleValue VISION_FLIGHT_SPEED;
    public static final ModConfigSpec.DoubleValue MIND_STONE_BEAM_DAMAGE;

    // ---- Quicksilver ----
    public static final ModConfigSpec.IntValue SLOW_MOTION_FACTOR;
    public static final ModConfigSpec.IntValue SLOW_MOTION_RADIUS;
    public static final ModConfigSpec.DoubleValue QUICKSILVER_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue QUICKSILVER_EXHAUSTION;
    public static final ModConfigSpec.DoubleValue QUICKSILVER_ACTION_SPEED;

    // ---- Billy (Wiccan) ----
    public static final ModConfigSpec.DoubleValue BILLY_FLIGHT_SPEED;
    public static final ModConfigSpec.DoubleValue HEX_BOLT_DAMAGE;
    public static final ModConfigSpec.IntValue FREEZE_DURATION;
    public static final ModConfigSpec.IntValue NOISE_RANGE;

    // ---- Tommy (Speed) ----
    public static final ModConfigSpec.DoubleValue TOMMY_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue TOMMY_EXHAUSTION;

    // ---- Loki ----
    public static final ModConfigSpec.DoubleValue LOKI_DAGGER_DAMAGE;
    public static final ModConfigSpec.IntValue SCEPTER_CONFUSION_TICKS;
    public static final ModConfigSpec.IntValue SCEPTER_NAUSEA_TICKS;
    public static final ModConfigSpec.IntValue SCEPTER_SLOWNESS_TICKS;

    // ---- Thor ----
    public static final ModConfigSpec.DoubleValue THOR_FLIGHT_SPEED;
    public static final ModConfigSpec.BooleanValue MJOLNIR_ANYONE_WORTHY;
    public static final ModConfigSpec.BooleanValue MJOLNIR_OWNER_ONLY;
    public static final ModConfigSpec.DoubleValue MJOLNIR_THROW_DAMAGE;
    public static final ModConfigSpec.DoubleValue STORMBREAKER_THROW_DAMAGE;
    public static final ModConfigSpec.IntValue MJOLNIR_THROW_RANGE;
    public static final ModConfigSpec.IntValue MJOLNIR_RECALL_RANGE;
    public static final ModConfigSpec.DoubleValue THOR_BEAM_DAMAGE;
    public static final ModConfigSpec.IntValue THOR_BEAM_INTERVAL;
    public static final ModConfigSpec.IntValue THOR_BEAM_RANGE;
    public static final ModConfigSpec.IntValue MJOLNIR_BEAM_RANGE;
    public static final ModConfigSpec.IntValue STORMBREAKER_BEAM_RANGE;
    public static final ModConfigSpec.DoubleValue MJOLNIR_BEAM_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STORMBREAKER_BEAM_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue THOR_BEAM_THUNDER_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue THOR_BEAM_EXHAUSTION;
    public static final ModConfigSpec.DoubleValue GOD_OF_THUNDER_EXHAUSTION;
    public static final ModConfigSpec.IntValue BIFROST_COOLDOWN;
    public static final ModConfigSpec.IntValue MJOLNIR_SHATTER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue MJOLNIR_SHATTER_DAMAGE;
    public static final ModConfigSpec.IntValue MJOLNIR_PIN_DURATION;

    static {
        BUILDER.push("magneto");
        MAGNETO_FLIGHT_SPEED = BUILDER.comment("Magneto's flying speed (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.075, 0.01, 0.5);
        TELEKINESIS_RANGE = BUILDER.comment("How far away (blocks) Magneto and Wanda can grab a target with telekinesis.")
                .defineInRange("telekinesisRange", 32, 4, 128);
        THROW_SPEED = BUILDER.comment("Launch speed (blocks per tick) of a telekinetic throw.")
                .defineInRange("throwSpeed", 2.6, 0.5, 6.0);
        SHRAPNEL_DAMAGE = BUILDER.comment("Damage of one iron shrapnel shard.")
                .defineInRange("shrapnelDamage", 4.0, 0.0, 100.0);
        SHRAPNEL_PIERCE = BUILDER.comment("How many creatures a single shard can pierce through.")
                .defineInRange("shrapnelPierce", 3, 1, 16);
        SHRAPNEL_FIRE_INTERVAL = BUILDER.comment("Ticks between shards while the Shrapnel Storm is held (2 = 10 shards/second).")
                .defineInRange("shrapnelFireInterval", 2, 1, 20);
        DISARM_RANGE = BUILDER.comment("Range of Armor & Weapon Strip.")
                .defineInRange("disarmRange", 32, 4, 128);
        DISARM_COOLDOWN = BUILDER.comment("Cooldown of Armor & Weapon Strip, in ticks.")
                .defineInRange("disarmCooldown", 60, 0, 6000);
        SHIELD_REFLECT_CHANCE = BUILDER.comment("Chance that a metal projectile caught by the Magnetic Shield flies back at the shooter (otherwise it drops).")
                .defineInRange("shieldReflectChance", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("scarlet_witch");
        WANDA_FLIGHT_SPEED = BUILDER.comment("Wanda's flying speed (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.09, 0.01, 0.5);
        CHAOS_BEAM_DAMAGE = BUILDER.comment("Damage per Chaos Beam hit (5 = 2.5 hearts). Minecraft's damage cooldown allows two hits per second.")
                .defineInRange("chaosBeamDamage", 5.0, 0.0, 100.0);
        CHAOS_BEAM_RANGE = BUILDER.comment("Range in blocks of the Chaos Beam, and also of Vision's Mind Stone beam.")
                .defineInRange("chaosBeamRange", 32, 4, 128);
        CHAOS_BEAM_NAUSEA_TICKS = BUILDER.comment("Duration of the Nausea the Chaos Beam inflicts, in ticks.")
                .defineInRange("chaosBeamNauseaTicks", 100, 0, 6000);
        CHAOS_SHIELD_EXHAUSTION = BUILDER.comment("Food exhaustion per tick while the Chaos Shield is held (4.0 exhaustion = 1 food point).")
                .defineInRange("chaosShieldExhaustion", 0.1, 0.0, 4.0);
        CHAOS_SHIELD_EXPLOSION_FACTOR = BUILDER.comment("Fraction of explosion damage that still gets through the Chaos Shield.")
                .defineInRange("chaosShieldExplosionFactor", 0.25, 0.0, 1.0);
        CHAOS_SHIELD_WIND_UP_TICKS = BUILDER.comment("How long the Chaos Shield takes to form after B is pressed, in ticks (15 = 3/4 of a second).",
                        "It blocks nothing until it is complete.")
                .defineInRange("chaosShieldWindUpTicks", 15, 0, 200);
        CHAOS_SHIELD_MAX_TICKS = BUILDER.comment("How long a complete Chaos Shield can be held before it drops, in ticks (160 = 8 seconds).")
                .defineInRange("chaosShieldMaxTicks", 160, 1, 6000);
        CHAOS_SHIELD_STRENGTH = BUILDER.comment("Points of damage the Chaos Shield can stop before it shatters. Everything it stops wears it down:",
                        "the damage of a projectile, and the explosion damage it takes away.")
                .defineInRange("chaosShieldStrength", 30.0, 1.0, 1000.0);
        CHAOS_SHIELD_COOLDOWN = BUILDER.comment("Ticks before the Chaos Shield can be raised again after it drops (120 = 6 seconds).")
                .defineInRange("chaosShieldCooldown", 120, 0, 6000);
        CHAOS_SHIELD_BROKEN_COOLDOWN = BUILDER.comment("Ticks before the Chaos Shield can be raised again after it shattered or was held to its",
                        "limit (300 = 15 seconds).")
                .defineInRange("chaosShieldBrokenCooldown", 300, 0, 6000);
        BLOCK_LIFT_RANGE = BUILDER.comment("How far away (blocks) Wanda can tear a block out of the ground.")
                .defineInRange("blockLiftRange", 24, 2, 128);
        BLOCK_LIFT_MAX_HARDNESS = BUILDER.comment("Hardest block Wanda can lift (stone = 1.5, iron block = 5, obsidian = 50).")
                .defineInRange("blockLiftMaxHardness", 5.0, 0.0, 100.0);
        HEX_DEFAULT_RADIUS = BUILDER.comment("Radius of a newly created Hex.")
                .defineInRange("hexDefaultRadius", 32, 4, 256);
        HEX_MIN_RADIUS = BUILDER.defineInRange("hexMinRadius", 8, 4, 256);
        HEX_MAX_RADIUS = BUILDER.defineInRange("hexMaxRadius", 96, 4, 256);
        HEX_DEATH_CHANCE = BUILDER.comment("Chance that an unauthorised creature without powers dies when crossing the Hex wall.")
                .defineInRange("hexDeathChance", 0.95, 0.0, 1.0);
        HEX_HOLD_DAMAGE = BUILDER.comment("Damage Vision takes the first time he is thrown back at the wall of a Hex he is inside (2 = one heart).",
                        "He can't leave a standing Hex, and comes apart a little at its edge; it never kills him.")
                .defineInRange("hexHoldDamage", 2.0, 0.0, 20.0);
        HEX_HOLD_DAMAGE_MAX = BUILDER.comment("The most one throw back at the wall can do to Vision. Each time he pushes again within three",
                        "seconds it grows by hexHoldDamage, up to this; it never takes his last half heart.")
                .defineInRange("hexHoldDamageMax", 8.0, 0.0, 40.0);
        HEX_HOLD_RELEASE_HEALTH = BUILDER.comment("A badly hurt Wanda can no longer hold Vision in her Hex: at or below this share of her health",
                        "(0.3 = 30%) he can leave it. Her death (which dissolves the Hex) always frees him; 0 = only that does.")
                .defineInRange("hexHoldReleaseHealth", 0.3, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("hex_vision");
        HEX_VISION_ENABLED = BUILDER.comment("If true (the default), a Hex brings a Vision to fight at Wanda's side while it stands.")
                .define("enabled", true);
        HEX_VISION_HEALTH = BUILDER.comment("Health of the Vision of the Hex (an Iron Golem has 100; 80 = 40 hearts).")
                .defineInRange("health", 80.0, 1.0, 1000.0);
        HEX_VISION_PUNCH_DAMAGE = BUILDER.comment("Damage of one of his dense punches, about one a second (12 = 6 hearts).")
                .defineInRange("punchDamage", 12.0, 0.0, 100.0);
        HEX_VISION_BEAM_DAMAGE = BUILDER.comment("Damage per hit of his Mind Stone beam, two hits a second.")
                .defineInRange("beamDamage", 4.0, 0.0, 100.0);
        HEX_VISION_BEAM_RANGE = BUILDER.comment("How far (blocks) his Mind Stone beam reaches. Up close he punches instead.")
                .defineInRange("beamRange", 16, 2, 64);
        HEX_VISION_REBUILD_TICKS = BUILDER.comment("How long (ticks) Wanda needs to rebuild him after he falls, while the Hex stands (1200 = a minute).")
                .defineInRange("rebuildTicks", 1200, 20, 72000);
        BUILDER.pop();

        BUILDER.push("vision");
        VISION_FLIGHT_SPEED = BUILDER.defineInRange("flightSpeed", 0.08, 0.01, 0.5);
        MIND_STONE_BEAM_DAMAGE = BUILDER.comment("Damage per hit of the Mind Stone beam (two hits per second at most).")
                .defineInRange("mindStoneBeamDamage", 6.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("quicksilver");
        SLOW_MOTION_FACTOR = BUILDER.comment("During Quicksilver Time the world around him runs this many times slower.")
                .defineInRange("slowMotionFactor", 5, 2, 20);
        SLOW_MOTION_RADIUS = BUILDER.comment("How far (blocks) from Quicksilver time slows down.")
                .defineInRange("slowMotionRadius", 32, 4, 128);
        QUICKSILVER_SPEED_BONUS = BUILDER.comment("Extra movement speed while he runs in Quicksilver Time or Super Speed",
                        "(3.0 = four times as fast).")
                .defineInRange("speedBonus", 3.0, 0.0, 10.0);
        QUICKSILVER_EXHAUSTION = BUILDER.comment("Food exhaustion per tick while he runs in Quicksilver Time or Super Speed",
                        "(4.0 = 1 food point). Quicksilver Time also costs half of this while he is not running.")
                .defineInRange("exhaustion", 0.03, 0.0, 4.0);
        QUICKSILVER_ACTION_SPEED = BUILDER.comment("While Quicksilver Time or Super Speed is on he mines, attacks, eats, drinks,",
                        "draws bows, loads crossbows, swims and places blocks this many times as fast (4.0 = four times as fast).")
                .defineInRange("actionSpeed", 4.0, 1.0, 10.0);
        BUILDER.pop();

        BUILDER.push("billy");
        BILLY_FLIGHT_SPEED = BUILDER.defineInRange("flightSpeed", 0.08, 0.01, 0.5);
        HEX_BOLT_DAMAGE = BUILDER.comment("Damage of a Hex Bolt to the creature it strikes; each creature it forks to takes half.")
                .defineInRange("hexBoltDamage", 6.0, 0.0, 100.0);
        FREEZE_DURATION = BUILDER.comment("How long (ticks) Billy's Freeze holds creatures spellbound.")
                .defineInRange("freezeDuration", 100, 1, 1200);
        NOISE_RANGE = BUILDER.comment("How far away (blocks) Billy hears the thoughts of creatures while The Noise is on.")
                .defineInRange("noiseRange", 32, 4, 64);
        BUILDER.pop();

        BUILDER.push("tommy");
        TOMMY_SPEED_BONUS = BUILDER.comment("Extra movement speed while Tommy runs at super speed (2.5 = three and a half times as fast).")
                .defineInRange("speedBonus", 2.5, 0.0, 10.0);
        TOMMY_EXHAUSTION = BUILDER.comment("Food exhaustion per tick while Tommy runs at super speed (4.0 = 1 food point).")
                .defineInRange("exhaustion", 0.02, 0.0, 4.0);
        BUILDER.pop();

        BUILDER.push("loki");
        LOKI_DAGGER_DAMAGE = BUILDER.comment("Damage of one of Loki's conjured daggers.")
                .defineInRange("daggerDamage", 5.0, 0.0, 100.0);
        SCEPTER_CONFUSION_TICKS = BUILDER.comment("How long a creature stays confused after a melee hit with Loki's Scepter, in ticks (120 = 6 seconds).")
                .defineInRange("scepterConfusionTicks", 120, 0, 6000);
        SCEPTER_NAUSEA_TICKS = BUILDER.comment("How long a player gets Nausea after a melee hit with Loki's Scepter, in ticks (160 = 8 seconds).")
                .defineInRange("scepterNauseaTicks", 160, 0, 6000);
        SCEPTER_SLOWNESS_TICKS = BUILDER.comment("How long a player gets Slowness I after a melee hit with Loki's Scepter, in ticks (60 = 3 seconds).")
                .defineInRange("scepterSlownessTicks", 60, 0, 6000);
        BUILDER.pop();

        BUILDER.push("thor");
        THOR_FLIGHT_SPEED = BUILDER.comment("Thor's flying speed while he holds Mjolnir or Stormbreaker (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.11, 0.01, 0.5);
        MJOLNIR_ANYONE_WORTHY = BUILDER.comment("If true, everyone is worthy of Mjolnir. If false (the default), only Thor, Vision,",
                        "Creative and Spectator players and those who have earned the Hero of the Village advancement are worthy: they can",
                        "claim a Mjolnir nobody owns yet (see mjolnirOwnerOnly), and lift it.")
                .define("anyoneWorthy", false);
        MJOLNIR_OWNER_ONLY = BUILDER.comment("If true (the default), each Mjolnir belongs to the first worthy player who lifts it, and",
                        "only they can lift it from then on (Creative and Spectator players always can, without taking it).",
                        "If false, Mjolnirs are never claimed: the old rule, where any worthy player can lift any Mjolnir.")
                .define("mjolnirOwnerOnly", true);
        MJOLNIR_THROW_DAMAGE = BUILDER.comment("Damage of a thrown Mjolnir, both ways.")
                .defineInRange("throwDamage", 10.0, 0.0, 100.0);
        STORMBREAKER_THROW_DAMAGE = BUILDER.comment("Damage of a thrown Stormbreaker, both ways (a fifth more than Mjolnir's).")
                .defineInRange("stormbreakerThrowDamage", 12.0, 0.0, 100.0);
        MJOLNIR_THROW_RANGE = BUILDER.comment("How far (blocks) a thrown Mjolnir or Stormbreaker flies before it turns back.")
                .defineInRange("throwRange", 40, 4, 128);
        MJOLNIR_RECALL_RANGE = BUILDER.comment("How far away (blocks) Thor can call Mjolnir or Stormbreaker back from.")
                .defineInRange("recallRange", 64, 4, 256);
        THOR_BEAM_DAMAGE = BUILDER.comment("Damage of one hit of Thor's lightning beam, bare-handed (2 = one heart).",
                        "Mjolnir in hand multiplies it by beamWeaponMultiplier, Stormbreaker by stormbreakerBeamMultiplier, the God",
                        "of Thunder by godOfThunderBeamMultiplier on top.")
                .defineInRange("beamDamage", 2.0, 0.0, 100.0);
        THOR_BEAM_INTERVAL = BUILDER.comment("Ticks between two hits of the lightning beam (5 = four hits a second). The beam ignores",
                        "the creature's damage cooldown, so every hit lands.")
                .defineInRange("beamInterval", 5, 1, 40);
        THOR_BEAM_RANGE = BUILDER.comment("How far (blocks) Thor's lightning beam reaches bare-handed.")
                .defineInRange("beamRange", 24, 4, 128);
        MJOLNIR_BEAM_RANGE = BUILDER.comment("How far (blocks) the beam reaches with Mjolnir in hand.")
                .defineInRange("beamWeaponRange", 48, 4, 256);
        STORMBREAKER_BEAM_RANGE = BUILDER.comment("How far (blocks) the beam reaches with Stormbreaker in hand (a fifth farther than Mjolnir's).")
                .defineInRange("stormbreakerBeamRange", 58, 4, 256);
        MJOLNIR_BEAM_MULTIPLIER = BUILDER.comment("What Mjolnir in hand multiplies the beam's damage by.",
                        "(With Mjolnir or Stormbreaker in hand the first hit on each creature also has the usual effects of lightning on it.)")
                .defineInRange("beamWeaponMultiplier", 2.0, 1.0, 20.0);
        STORMBREAKER_BEAM_MULTIPLIER = BUILDER.comment("What Stormbreaker in hand multiplies the beam's damage by (a fifth more than Mjolnir's).")
                .defineInRange("stormbreakerBeamMultiplier", 2.4, 1.0, 20.0);
        THOR_BEAM_THUNDER_MULTIPLIER = BUILDER.comment("What the God of Thunder multiplies the beam's damage by. Every hit also arcs to the",
                        "next two creatures nearby.")
                .defineInRange("godOfThunderBeamMultiplier", 1.5, 1.0, 20.0);
        THOR_BEAM_EXHAUSTION = BUILDER.comment("Food exhaustion per tick while the lightning beam is fired (4.0 = 1 food point).")
                .defineInRange("beamExhaustion", 0.03, 0.0, 4.0);
        GOD_OF_THUNDER_EXHAUSTION = BUILDER.comment("Food exhaustion per tick while the God of Thunder is unleashed (4.0 = 1 food point).")
                .defineInRange("godOfThunderExhaustion", 0.04, 0.0, 4.0);
        BIFROST_COOLDOWN = BUILDER.comment("Cooldown of the Bifrost (Thor's, or anyone's who wields Stormbreaker or Mjolnir), in ticks.")
                .defineInRange("bifrostCooldown", 1200, 0, 72000);
        MJOLNIR_SHATTER_COOLDOWN = BUILDER.comment("Cooldown of shattering Mjolnir (Sneak + R), in ticks, from the moment it shatters.")
                .defineInRange("shatterCooldown", 300, 0, 72000);
        MJOLNIR_SHATTER_DAMAGE = BUILDER.comment("Damage of each fragment of a shattered Mjolnir to the enemy it strikes.")
                .defineInRange("shatterDamage", 6.0, 0.0, 100.0);
        MJOLNIR_PIN_DURATION = BUILDER.comment("How long (ticks) Mjolnir pins someone down before it lets go by itself (1200 = a minute).")
                .defineInRange("pinDuration", 1200, 20, 72000);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private EnhancedConfig() {
    }
}
