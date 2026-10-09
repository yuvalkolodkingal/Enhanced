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

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Enhanced.id(name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity causing, @Nullable Entity direct) {
        return level.damageSources().source(type, causing, direct);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return level.damageSources().source(type);
    }

    private ModDamageTypes() {
    }
}
