package com.kolodkingal.enhanced.registry;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.reg.RegEntry;
import com.kolodkingal.enhanced.platform.reg.Registrar;
import com.kolodkingal.enhanced.platform.reg.Registrars;
import com.kolodkingal.enhanced.stones.StoneExtractionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** The mod's own kinds of recipe (3.6, 3.7). */
public final class ModRecipes {
    public static final Registrar<RecipeSerializer<?>> SERIALIZERS =
            Registrars.create(Registries.RECIPE_SERIALIZER, Enhanced.MODID);

    // ===== stones (3.6) =====
    /**
     * A stone crafted out of its vessel, the empty vessel left in the grid ({@code enhanced:stone_extraction}; lane
     * L1).
     */
    public static final RegEntry<RecipeSerializer<?>, RecipeSerializer<StoneExtractionRecipe>> STONE_EXTRACTION =
            SERIALIZERS.register("stone_extraction", () -> StoneExtractionRecipe.SERIALIZER);
    /**
     * The empty Infinity Gauntlet's shaped recipe, off with {@code [gauntlet] craftable} ({@code
     * enhanced:gauntlet_shaped}; lane L1).
     */
    public static final RegEntry<RecipeSerializer<?>, RecipeSerializer<com.kolodkingal.enhanced.stones.GauntletRecipe>>
            GAUNTLET_SHAPED = SERIALIZERS.register("gauntlet_shaped",
            () -> com.kolodkingal.enhanced.stones.GauntletRecipe.SERIALIZER);

    // ===== hulk (3.6) =====
    /** The Gamma Emitter's shaped recipe, matched only while {@code [gamma] emitterCraftable} is on (lane L7). */
    public static final RegEntry<RecipeSerializer<?>, RecipeSerializer<com.kolodkingal.enhanced.hulk.GammaEmitterRecipe>>
            GAMMA_EMITTER = SERIALIZERS.register("gamma_emitter",
            () -> com.kolodkingal.enhanced.hulk.GammaEmitterRecipe.SERIALIZER);

    // ===== wolverine (3.7) =====
    /**
     * The Adamantium Injection's shaped recipe, matched only while {@code [adamantium_injection] craftable} is on
     * ({@code enhanced:adamantium_injection}; lane L2).
     */
    public static final RegEntry<RecipeSerializer<?>, RecipeSerializer<com.kolodkingal.enhanced.wolverine.InjectionRecipe>> ADAMANTIUM_INJECTION =
            SERIALIZERS.register("adamantium_injection",
                    () -> com.kolodkingal.enhanced.wolverine.InjectionRecipe.SERIALIZER);

    // ===== finders (3.7) =====
    /** The Stone Locator's shaped recipe, the core given back ({@code enhanced:stone_locator}; lane L5). */
    public static final RegEntry<RecipeSerializer<?>, RecipeSerializer<com.kolodkingal.enhanced.finders.LocatorRecipe>>
            STONE_LOCATOR = SERIALIZERS.register("stone_locator",
            () -> com.kolodkingal.enhanced.finders.LocatorRecipe.SERIALIZER);

    // ===== serum (3.8) =====
    /** The serum's shaped recipe, matched only while {@code [super_soldier_serum] craftable} is on (lane L1). */
    public static final RegEntry<RecipeSerializer<?>, RecipeSerializer<com.kolodkingal.enhanced.item.SwitchedRecipe>>
            SERUM = SERIALIZERS.register("serum", () -> com.kolodkingal.enhanced.item.SwitchedRecipe.serializer(
                    com.kolodkingal.enhanced.item.SwitchedRecipe.Switch.SERUM));
    // ===== iron_man (3.8) =====
    /** The Arc Reactors' shaped recipes, matched only while {@code [iron_man] reactorsCraftable} is on (lane L3). */
    public static final RegEntry<RecipeSerializer<?>, RecipeSerializer<com.kolodkingal.enhanced.item.SwitchedRecipe>>
            ARC_REACTOR = SERIALIZERS.register("arc_reactor",
                    () -> com.kolodkingal.enhanced.item.SwitchedRecipe.serializer(
                            com.kolodkingal.enhanced.item.SwitchedRecipe.Switch.ARC_REACTOR));

    private ModRecipes() {
    }
}
