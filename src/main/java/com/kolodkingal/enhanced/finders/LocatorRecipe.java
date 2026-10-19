package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.LocatorConfig;
import com.kolodkingal.enhanced.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * The Stone Locator's recipe (3.7, type {@code enhanced:stone_locator}, the same fields as {@code
 * minecraft:crafting_shaped}): {@code N K N / D S D / N D N}, four netherite blocks, three diamond blocks, a wither
 * skeleton skull and in the centre an Infinity Stone or a stone's vessel ({@code #enhanced:locator_cores}). It matches
 * only while {@code [stone_locator] craftable} is on. The 3.7 skeleton registers it as a plain shaped recipe; lane L5
 * gives the core back (the recipe's remainder, its components kept), tunes the result to the core's stone and refuses
 * a Gauntlet with no stone or the Titan's.
 */
public class LocatorRecipe extends ShapedRecipe {
    public LocatorRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result,
                         boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    static LocatorRecipe of(ShapedRecipe shaped) {
        return new LocatorRecipe(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(null),
                shaped.showNotification());
    }

    /** Whether the config lets the Locator be crafted (yes when it is not loaded). */
    public static boolean craftable() {
        return !EnhancedConfig.SPEC.isLoaded() || LocatorConfig.CRAFTABLE.get();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return craftable() && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.STONE_LOCATOR.get();
    }

    /** The serializer: the shaped recipe's own codecs, read as this kind. */
    public static class Serializer implements RecipeSerializer<LocatorRecipe> {
        private static final MapCodec<LocatorRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(LocatorRecipe::of, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, LocatorRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(LocatorRecipe::of, r -> r);

        @Override
        public MapCodec<LocatorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LocatorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
