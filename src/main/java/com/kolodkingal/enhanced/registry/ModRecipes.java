package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.stones.StoneExtractionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own kinds of recipe (3.6). */
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

    private ModRecipes() {
    }
}
