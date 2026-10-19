package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.stones.SiteStoneLootFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own loot functions (3.6). */
public final class ModLootFunctions {
    public static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Enhanced.MODID);

    // ===== stones (3.6) =====
    /** A site stone in a chest, given by the ledger's rules ({@code enhanced:site_stone}; lane L1). */
    public static final DeferredHolder<LootItemFunctionType<?>,
            LootItemFunctionType<SiteStoneLootFunction>> SITE_STONE =
            FUNCTIONS.register("site_stone", () -> new LootItemFunctionType<>(SiteStoneLootFunction.CODEC));

    private ModLootFunctions() {
    }
}
