package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * What a Hydra soldier leaves (3.6, the plan's 15.7). The common drops are each rank's loot table ({@code
 * entities/hydra_soldier} for a rifleman, {@code hydra_soldier_enforcer}, {@code hydra_soldier_sniper}, {@code
 * hydra_officer}): iron nuggets and gunpowder, an iron ingot, emeralds. The rare ones need a player's kill (Looting
 * adds 5 % to each chance a level) and never come from reinforcements (no farm): a Hydra energy cell ({@code [hydra]
 * energyCellDrop} 20 % for a rifleman, three quarters of it for an enforcer, half as much again for a sniper, always one
 * from the officer), the sniper's spyglass (10 %), the officer's blaster ({@code blasterDrop} 10 %, when {@code
 * blasterEnabled}) and his Hydra intel ({@code intelMapDrop} 25 %: a map to the nearest Avengers Tower, {@link
 * HydraIntel}).
 */
public final class HydraDrops {
    public static final ResourceKey<LootTable> RIFLEMAN = table("hydra_soldier");
    public static final ResourceKey<LootTable> ENFORCER = table("hydra_soldier_enforcer");
    public static final ResourceKey<LootTable> SNIPER = table("hydra_soldier_sniper");
    public static final ResourceKey<LootTable> OFFICER = table("hydra_officer");
    /** Looting's extra chance per level for every rare drop. */
    public static final double LOOTING_BONUS = 0.05;
    /** The sniper's spyglass. */
    public static final double SPYGLASS = 0.10;

    private static ResourceKey<LootTable> table(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Enhanced.id("entities/" + name));
    }

    /** The loot table of a soldier's rank. */
    static ResourceKey<LootTable> table(HydraSoldierEntity soldier) {
        if (soldier.isOfficer()) {
            return OFFICER;
        }
        return switch (soldier.rank()) {
            case ENFORCER -> ENFORCER;
            case SNIPER -> SNIPER;
            default -> RIFLEMAN;
        };
    }

    /** The chance (before Looting) that a soldier of this kind leaves an energy cell. */
    public static double cellChance(HydraSoldierEntity soldier) {
        if (soldier.isOfficer()) {
            return 1.0;
        }
        double base = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.ENERGY_CELL_DROP.get() : 0.2;
        return switch (soldier.rank()) {
            case ENFORCER -> base * 0.75;
            case SNIPER -> base * 1.5;
            default -> base;
        };
    }

    /** The rare drops of a death (a player's kill only; never a reinforcement's). */
    static void dropRare(ServerLevel level, HydraSoldierEntity soldier, DamageSource source, boolean byPlayer) {
        if (!byPlayer || soldier.isReinforcement()) {
            return;
        }
        double bonus = LOOTING_BONUS * looting(level, source);
        if (roll(soldier, cellChance(soldier) + bonus)) {
            soldier.spawnAtLocation(new ItemStack(ModItems.HYDRA_ENERGY_CELL.get()));
        }
        if (!soldier.isOfficer() && soldier.rank() == Rank.SNIPER && roll(soldier, SPYGLASS + bonus)) {
            soldier.spawnAtLocation(new ItemStack(Items.SPYGLASS));
        }
        if (!soldier.isOfficer()) {
            return;
        }
        boolean blasters = !EnhancedConfig.SPEC.isLoaded() || HydraConfig.BLASTER_ENABLED.get();
        double blaster = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BLASTER_DROP.get() : 0.1;
        if (blasters && roll(soldier, blaster + bonus)) {
            soldier.spawnAtLocation(new ItemStack(ModItems.HYDRA_BLASTER.get()));
        }
        double intel = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.INTEL_MAP_DROP.get() : 0.25;
        if (roll(soldier, intel + bonus)) {
            ItemStack map = HydraIntel.make(level, soldier.blockPosition());
            if (!map.isEmpty()) {
                soldier.spawnAtLocation(map);
            }
        }
    }

    private static boolean roll(HydraSoldierEntity soldier, double chance) {
        return chance > 0.0 && soldier.getRandom().nextDouble() < chance;
    }

    /** The killer's Looting level. */
    private static int looting(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity killer)) {
            return 0;
        }
        return level.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(r -> r.get(Enchantments.LOOTING))
                .map(looting -> EnchantmentHelper.getEnchantmentLevel(looting, killer))
                .orElse(0);
    }

    private HydraDrops() {
    }
}
