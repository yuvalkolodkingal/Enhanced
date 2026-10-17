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

    // ===== agatha (3.3) =====
    // Agatha's dark magic: her Purple Beam is the Darkhold's own (DARKHOLD_BEAM); these two and that one are the tag
    // enhanced:dark_magic (ModTags.DARK_MAGIC), what the ward of the Book of the Vishanti keeps out.
    /** The Power Drain's hit on a creature: its life drawn out into her. */
    public static final ResourceKey<DamageType> AGATHA_DRAIN = key("agatha_drain");
    /** The burning of the Rune Trap on what stands bound in it. */
    public static final ResourceKey<DamageType> AGATHA_RUNES = key("agatha_runes");

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
