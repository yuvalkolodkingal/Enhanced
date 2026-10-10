package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Damage types are data-driven: see data/enhanced/damage_type. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> CHAOS_BEAM = key("chaos_beam");
    public static final ResourceKey<DamageType> SHRAPNEL = key("shrapnel");
    public static final ResourceKey<DamageType> TELEKINETIC_SLAM = key("telekinetic_slam");
    public static final ResourceKey<DamageType> HEX_WALL = key("hex_wall");
    public static final ResourceKey<DamageType> MIND_STONE = key("mind_stone");
    public static final ResourceKey<DamageType> PHOTON_BLAST = key("photon_blast");
    public static final ResourceKey<DamageType> ENERGY_BURST = key("energy_burst");
    public static final ResourceKey<DamageType> SPEED_BLITZ = key("speed_blitz");
    public static final ResourceKey<DamageType> MIND_STONE_BEAM = key("mind_stone_beam");
    public static final ResourceKey<DamageType> MOLECULAR_ACCELERATION = key("molecular_acceleration");
    public static final ResourceKey<DamageType> MJOLNIR = key("mjolnir");
    public static final ResourceKey<DamageType> STORMBREAKER = key("stormbreaker");
    public static final ResourceKey<DamageType> LOKI_DAGGER = key("loki_dagger");
    public static final ResourceKey<DamageType> HEX_BOLT = key("hex_bolt");

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
