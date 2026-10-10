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
    public static final ModConfigSpec.IntValue BLOCK_LIFT_RANGE;
    public static final ModConfigSpec.DoubleValue BLOCK_LIFT_MAX_HARDNESS;
    public static final ModConfigSpec.IntValue HEX_DEFAULT_RADIUS;
    public static final ModConfigSpec.IntValue HEX_MIN_RADIUS;
    public static final ModConfigSpec.IntValue HEX_MAX_RADIUS;
    public static final ModConfigSpec.DoubleValue HEX_DEATH_CHANCE;

    // ---- Vision ----
    public static final ModConfigSpec.DoubleValue VISION_FLIGHT_SPEED;
    public static final ModConfigSpec.DoubleValue MIND_STONE_BEAM_DAMAGE;

    // ---- Quicksilver ----
    public static final ModConfigSpec.IntValue SLOW_MOTION_FACTOR;
    public static final ModConfigSpec.IntValue SLOW_MOTION_RADIUS;
    public static final ModConfigSpec.DoubleValue QUICKSILVER_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue QUICKSILVER_EXHAUSTION;

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

    // ---- Thor ----
    public static final ModConfigSpec.DoubleValue THOR_FLIGHT_SPEED;
    public static final ModConfigSpec.BooleanValue MJOLNIR_ANYONE_WORTHY;
    public static final ModConfigSpec.DoubleValue MJOLNIR_THROW_DAMAGE;
    public static final ModConfigSpec.DoubleValue STORMBREAKER_THROW_DAMAGE;
    public static final ModConfigSpec.IntValue MJOLNIR_THROW_RANGE;
    public static final ModConfigSpec.IntValue MJOLNIR_RECALL_RANGE;
    public static final ModConfigSpec.DoubleValue THOR_LIGHTNING_DAMAGE;
    public static final ModConfigSpec.IntValue THOR_LIGHTNING_RANGE;
    public static final ModConfigSpec.DoubleValue GOD_OF_THUNDER_EXHAUSTION;
    public static final ModConfigSpec.IntValue BIFROST_COOLDOWN;

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
        QUICKSILVER_SPEED_BONUS = BUILDER.comment("Extra movement speed during Quicksilver Time (3.0 = four times as fast).")
                .defineInRange("speedBonus", 3.0, 0.0, 10.0);
        QUICKSILVER_EXHAUSTION = BUILDER.comment("Food exhaustion per tick during Quicksilver Time (4.0 = 1 food point).")
                .defineInRange("exhaustion", 0.03, 0.0, 4.0);
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
        BUILDER.pop();

        BUILDER.push("thor");
        THOR_FLIGHT_SPEED = BUILDER.comment("Thor's flying speed while he holds Mjolnir or Stormbreaker (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.11, 0.01, 0.5);
        MJOLNIR_ANYONE_WORTHY = BUILDER.comment("If true, everyone is worthy of Mjolnir. If false (the default), only Thor, Vision,",
                        "Creative players and those who have earned the Hero of the Village advancement can lift it.")
                .define("anyoneWorthy", false);
        MJOLNIR_THROW_DAMAGE = BUILDER.comment("Damage of a thrown Mjolnir, both ways.")
                .defineInRange("throwDamage", 10.0, 0.0, 100.0);
        STORMBREAKER_THROW_DAMAGE = BUILDER.comment("Damage of a thrown Stormbreaker, both ways.")
                .defineInRange("stormbreakerThrowDamage", 12.0, 0.0, 100.0);
        MJOLNIR_THROW_RANGE = BUILDER.comment("How far (blocks) a thrown Mjolnir or Stormbreaker flies before it turns back.")
                .defineInRange("throwRange", 40, 4, 128);
        MJOLNIR_RECALL_RANGE = BUILDER.comment("How far away (blocks) Thor can call Mjolnir or Stormbreaker back from.")
                .defineInRange("recallRange", 64, 4, 256);
        THOR_LIGHTNING_DAMAGE = BUILDER.comment("Damage of Thor's lightning to whatever it strikes.")
                .defineInRange("lightningDamage", 8.0, 0.0, 100.0);
        THOR_LIGHTNING_RANGE = BUILDER.comment("How far away (blocks) Thor can call lightning down.")
                .defineInRange("lightningRange", 64, 4, 256);
        GOD_OF_THUNDER_EXHAUSTION = BUILDER.comment("Food exhaustion per tick while the God of Thunder is unleashed (4.0 = 1 food point).")
                .defineInRange("godOfThunderExhaustion", 0.04, 0.0, 4.0);
        BIFROST_COOLDOWN = BUILDER.comment("Cooldown of the Bifrost (Thor's, or anyone's who wields Stormbreaker or Mjolnir), in ticks.")
                .defineInRange("bifrostCooldown", 1200, 0, 72000);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private EnhancedConfig() {
    }
}
