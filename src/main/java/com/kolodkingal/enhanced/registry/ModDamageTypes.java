package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Damage types are data-driven: see data/enhanced/damage_type. The ones of 3.0 sit in one region per hero (the
 * {@code // =====} lines), so the five lanes append to their own.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> CHAOS_BEAM = key("chaos_beam");
    public static final ResourceKey<DamageType> SHRAPNEL = key("shrapnel");
    public static final ResourceKey<DamageType> TELEKINETIC_SLAM = key("telekinetic_slam");
    public static final ResourceKey<DamageType> HEX_WALL = key("hex_wall");
    public static final ResourceKey<DamageType> MIND_STONE = key("mind_stone");
    public static final ResourceKey<DamageType> SPEED_BLITZ = key("speed_blitz");
    public static final ResourceKey<DamageType> MIND_STONE_BEAM = key("mind_stone_beam");
    public static final ResourceKey<DamageType> MOLECULAR_ACCELERATION = key("molecular_acceleration");
    public static final ResourceKey<DamageType> MJOLNIR = key("mjolnir");
    public static final ResourceKey<DamageType> STORMBREAKER = key("stormbreaker");
    public static final ResourceKey<DamageType> LOKI_DAGGER = key("loki_dagger");
    public static final ResourceKey<DamageType> HEX_BOLT = key("hex_bolt");
    public static final ResourceKey<DamageType> LIGHTNING_BEAM = key("lightning_beam");
    /** 3.2: the hits of Billy's continuous Hex Beam (they bypass the damage cooldown, as the lightning beam's do). */
    public static final ResourceKey<DamageType> HEX_BEAM = key("hex_beam");
    /** 3.2: the hits of the Darkhold's purple beam (like the Hex Beam's: no damage cooldown, no knockback). */
    public static final ResourceKey<DamageType> DARKHOLD_BEAM = key("darkhold_beam");

    // ===== phoenix =====
    public static final ResourceKey<DamageType> PHOENIX_BURST = key("phoenix_burst");

    // ===== spider =====
    public static final ResourceKey<DamageType> WEB_KICK = key("web_kick");
    public static final ResourceKey<DamageType> SPIDER_LEGS = key("spider_legs");

    // ===== thanos =====
    public static final ResourceKey<DamageType> THANOS_BLOW = key("thanos_blow");
    public static final ResourceKey<DamageType> THANOS_SWORD = key("thanos_sword");
    public static final ResourceKey<DamageType> STONE_POWER = key("stone_power");
    public static final ResourceKey<DamageType> STONE_BLAST = key("stone_blast");
    public static final ResourceKey<DamageType> STONE_SOUL = key("stone_soul");

    // ===== ultron =====
    public static final ResourceKey<DamageType> ULTRON_BOLT = key("ultron_bolt");
    public static final ResourceKey<DamageType> ULTRON_BEAM = key("ultron_beam");
    public static final ResourceKey<DamageType> ULTRON_SLAM = key("ultron_slam");

    // ===== apocalypse =====
    public static final ResourceKey<DamageType> SAND = key("sand");
    public static final ResourceKey<DamageType> APOCALYPSE_BLOW = key("apocalypse_blow");
    public static final ResourceKey<DamageType> ENTOMB = key("entomb");

    // ===== cyclops (3.3) =====
    /** Every blast of Cyclops's eyes: the optic blast, the giant beam, the ricochet, the concussion and the leap's. Not magic. */
    public static final ResourceKey<DamageType> OPTIC_BLAST = key("optic_blast");
    // ===== 3.3 custom heroes =====
    /** The blows, bolts, blasts and elements of a custom hero's powers (3.3). */
    public static final ResourceKey<DamageType> HERO_POWER = key("hero_power");
    /** The hits of a custom hero's continuous beam (Energy Blast's beam, the flamethrower, the water jet): no damage cooldown. */
    public static final ResourceKey<DamageType> HERO_BEAM = key("hero_beam");
    /** 3.4: a custom hero's Magic (its bolts): in {@code enhanced:magic_damage}, so the wards and the Darkhold's rules see it. */
    public static final ResourceKey<DamageType> HERO_MAGIC = key("hero_magic");
    /** 3.4: a custom hero's mind powers: in {@code enhanced:mind_power}. */
    public static final ResourceKey<DamageType> HERO_MIND = key("hero_mind");
    // ===== professor_x (3.3) =====
    /** Professor X's Mental Blast (it ignores armour), and the backlash of a stronger telepath who throws him out. */
    public static final ResourceKey<DamageType> MIND_BLAST = key("mind_blast");
    // ===== agatha (3.3) =====
    // Agatha's dark magic: her Purple Beam is the Darkhold's own (DARKHOLD_BEAM); these two and that one are the tag
    // enhanced:dark_magic (ModTags.DARK_MAGIC), what the ward of the Book of the Vishanti keeps out.
    /** The Power Drain's hit on a creature: its life drawn out into her. */
    public static final ResourceKey<DamageType> AGATHA_DRAIN = key("agatha_drain");
    /** The burning of the Rune Trap on what stands bound in it. */
    public static final ResourceKey<DamageType> AGATHA_RUNES = key("agatha_runes");

    // ===== asgard (3.5, the skeleton registers every one; each lane deals its own) =====
    /** Heimdall's Hofund: the slashes of the combo and the lunge (lane L2). */
    public static final ResourceKey<DamageType> HOFUND = key("hofund");
    /** The column of rainbow light of Heimdall's Bifrost Strike (lane L2). */
    public static final ResourceKey<DamageType> BIFROST_STRIKE = key("bifrost_strike");
    /** Gungnir: Odin's blast from its tip and the thrown spear (lane L3). */
    public static final ResourceKey<DamageType> GUNGNIR = key("gungnir");
    /** The Odinforce: Odin's shock wave and what Banish does to those it cannot banish (lane L3). */
    public static final ResourceKey<DamageType> ODINFORCE = key("odinforce");
    /** Hela's thrown necroswords, the hero's and the boss's (tags dark_magic, is_projectile; lane L4). */
    public static final ResourceKey<DamageType> NECROSWORD = key("necrosword");
    /** The blade spikes of Hela's Blade Wall, Burst and Storm (tag dark_magic; lane L4). */
    public static final ResourceKey<DamageType> BLADE_WALL = key("blade_wall");
    /** The bite of Fenris (lane L4). */
    public static final ResourceKey<DamageType> FENRIS_BITE = key("fenris_bite");

    // ===== stones and strange (3.6, the skeleton registers every one; each lane deals its own) =====
    /** Doctor Strange's Eldritch Whip (lane L3). */
    public static final ResourceKey<DamageType> ELDRITCH_WHIP = key("eldritch_whip");
    /** The Crimson Bands of Cyttorak binding a creature (lane L3). */
    public static final ResourceKey<DamageType> CRIMSON_BANDS = key("crimson_bands");
    /** The Power Stone's Purple Beam (lane L2). */
    public static final ResourceKey<DamageType> POWER_STONE_BEAM = key("power_stone_beam");
    /** The explosions at the end of the Purple Beam (tag is_explosion; lane L2). */
    public static final ResourceKey<DamageType> POWER_STONE_EXPLOSION = key("power_stone_explosion");
    /** A stone burning the hand of one who may not hold it (never kills: the amount is capped; lane L1). */
    public static final ResourceKey<DamageType> STONE_REJECT = key("stone_reject");
    /** The Snap's dust (never on players; for the logs; lane L1). */
    public static final ResourceKey<DamageType> SNAP_DUST = key("snap_dust");
    // ===== hulk (3.6) =====
    /** Hulk Smash (lane L7). */
    public static final ResourceKey<DamageType> HULK_SMASH = key("hulk_smash");
    /** The Grab Smash's slams (lane L7). */
    public static final ResourceKey<DamageType> HULK_GRAB = key("hulk_grab");
    /** The Thunder Clap's shockwave (lane L7). */
    public static final ResourceKey<DamageType> HULK_CLAP = key("hulk_clap");
    /** The Super Leap's landing (lane L7). */
    public static final ResourceKey<DamageType> HULK_LEAP = key("hulk_leap");
    /** The thrown boulder (tag is_projectile; lane L7). */
    public static final ResourceKey<DamageType> HULK_BOULDER = key("hulk_boulder");
    /** The Gamma Charge (lane L7). */
    public static final ResourceKey<DamageType> HULK_CHARGE = key("hulk_charge");
    // ===== hydra (3.6) =====
    /** A Hydra energy bolt: the rifles, the sniper, the officer's pistol, the blaster (tag is_projectile; lane L8). */
    public static final ResourceKey<DamageType> HYDRA_BOLT = key("hydra_bolt");
    /** The enforcer's stun baton (lane L8). */
    public static final ResourceKey<DamageType> HYDRA_BATON = key("hydra_baton");
    // ===== wolverine (3.7) =====
    /** Wolverine's claws: the Claw Slash combo, the Spin Slash and his claws-out empty hand (melee_blows; lane L2). */
    public static final ResourceKey<DamageType> WOLVERINE_CLAWS = key("wolverine_claws");
    /** Wolverine's Pounce landing on its prey (lane L2). */
    public static final ResourceKey<DamageType> WOLVERINE_POUNCE = key("wolverine_pounce");
    // ===== deadpool (3.7) =====
    /** Deadpool's katanas: the combo and the Drop-In (melee_blows; lane L3). */
    public static final ResourceKey<DamageType> DEADPOOL_KATANA = key("deadpool_katana");
    /** Deadpool's cartoon bullets: the pistols and the Trick Shot (is_projectile; lane L3). */
    public static final ResourceKey<DamageType> DEADPOOL_BULLET = key("deadpool_bullet");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Enhanced.id(name));
    }

    /**
     * Damage dealt by {@code direct} (what actually hit: a shard, a bolt, a thrown block) and caused by {@code causing}
     * (who gets the blame: kill credit, PvP rules, death messages, Magneto's helmet rule).
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity causing, @Nullable Entity direct) {
        // Built directly: DamageSources.source(type, a, b) takes the direct entity FIRST (vanilla calls it as
        // source(ARROW, arrow, shooter)), whatever its parameter names suggest.
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return level.damageSources().source(type);
    }

    private ModDamageTypes() {
    }
}
