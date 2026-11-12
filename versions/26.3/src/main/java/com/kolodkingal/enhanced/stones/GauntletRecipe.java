package com.kolodkingal.enhanced.stones;
//#norenames

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * The empty Infinity Gauntlet's recipe (3.6, recipe type {@code enhanced:gauntlet_shaped}, the plan's 1.3): a shaped
 * crafting recipe like any other (its JSON is a {@code crafting_shaped}'s), which matches only while {@code [gauntlet]
 * craftable} is on, so a server can turn the crafted Gauntlet off without a data pack.
 */
public class GauntletRecipe extends com.kolodkingal.enhanced.compat.ShapedBase {
    /** The serializer: the shaped recipe's own fields, read as this kind (26.3). */
    public static final RecipeSerializer<GauntletRecipe> SERIALIZER = serializer(GauntletRecipe::new);

    public GauntletRecipe(net.minecraft.world.item.crafting.Recipe.CommonInfo common,
            net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern,
            net.minecraft.world.item.ItemStackTemplate result) {
        super(common, book, pattern, result);
    }

    /** Whether the Gauntlet may be crafted now ({@code [gauntlet] craftable}). */
    public static boolean craftable() {
        return !EnhancedConfig.SPEC.isLoaded() || StonesConfig.GAUNTLET_CRAFTABLE.get();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return craftable() && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return as(ModRecipes.GAUNTLET_SHAPED.get());
    }

}
