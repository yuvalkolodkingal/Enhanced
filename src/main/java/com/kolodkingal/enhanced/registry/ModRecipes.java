package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.stones.StoneExtractionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own kinds of recipe (3.6, 3.7). */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Enhanced.MODID);

    // ===== stones (3.6) =====
    /**
     * A stone crafted out of its vessel, the empty vessel left in the grid ({@code enhanced:stone_extraction}; lane
     * L1).
     */
    public static final DeferredHolder<RecipeSerializer<?>, StoneExtractionRecipe.Serializer> STONE_EXTRACTION =
            SERIALIZERS.register("stone_extraction", StoneExtractionRecipe.Serializer::new);
    /**
     * The empty Infinity Gauntlet's shaped recipe, off with {@code [gauntlet] craftable} ({@code
     * enhanced:gauntlet_shaped}; lane L1).
     */
    public static final DeferredHolder<RecipeSerializer<?>, com.kolodkingal.enhanced.stones.GauntletRecipe.Serializer>
            GAUNTLET_SHAPED = SERIALIZERS.register("gauntlet_shaped",
            com.kolodkingal.enhanced.stones.GauntletRecipe.Serializer::new);

    // ===== hulk (3.6) =====
    /** The Gamma Emitter's shaped recipe, matched only while {@code [gamma] emitterCraftable} is on (lane L7). */
    public static final DeferredHolder<RecipeSerializer<?>, com.kolodkingal.enhanced.hulk.GammaEmitterRecipe.Serializer>
            GAMMA_EMITTER = SERIALIZERS.register("gamma_emitter",
            com.kolodkingal.enhanced.hulk.GammaEmitterRecipe.Serializer::new);

    // ===== wolverine (3.7) =====
    /**
     * The Adamantium Injection's shaped recipe, matched only while {@code [adamantium_injection] craftable} is on
     * ({@code enhanced:adamantium_injection}; lane L2).
     */
    public static final DeferredHolder<RecipeSerializer<?>,
            com.kolodkingal.enhanced.wolverine.InjectionRecipe.Serializer> ADAMANTIUM_INJECTION =
            SERIALIZERS.register("adamantium_injection",
                    com.kolodkingal.enhanced.wolverine.InjectionRecipe.Serializer::new);

    // ===== finders (3.7) =====
    /** The Stone Locator's shaped recipe, the core given back ({@code enhanced:stone_locator}; lane L5). */
    public static final DeferredHolder<RecipeSerializer<?>, com.kolodkingal.enhanced.finders.LocatorRecipe.Serializer>
            STONE_LOCATOR = SERIALIZERS.register("stone_locator",
            com.kolodkingal.enhanced.finders.LocatorRecipe.Serializer::new);

    // ===== serum (3.8) =====
    /** The serum's shaped recipe, matched only while {@code [super_soldier_serum] craftable} is on (lane L1). */
    public static final DeferredHolder<RecipeSerializer<?>, com.kolodkingal.enhanced.item.SwitchedRecipe.Serializer>
            SERUM = SERIALIZERS.register("serum", () -> new com.kolodkingal.enhanced.item.SwitchedRecipe.Serializer(
                    com.kolodkingal.enhanced.item.SwitchedRecipe.Switch.SERUM));
    // ===== iron_man (3.8) =====
    /** The Arc Reactors' shaped recipes, matched only while {@code [iron_man] reactorsCraftable} is on (lane L3). */
    public static final DeferredHolder<RecipeSerializer<?>, com.kolodkingal.enhanced.item.SwitchedRecipe.Serializer>
            ARC_REACTOR = SERIALIZERS.register("arc_reactor",
                    () -> new com.kolodkingal.enhanced.item.SwitchedRecipe.Serializer(
                            com.kolodkingal.enhanced.item.SwitchedRecipe.Switch.ARC_REACTOR));

    private ModRecipes() {
    }
}
