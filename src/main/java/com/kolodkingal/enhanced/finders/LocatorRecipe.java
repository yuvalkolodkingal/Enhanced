package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.LocatorConfig;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModRecipes;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
 * only while {@code [stone_locator] craftable} is on.
 *
 * <p>The core is <b>given back</b> ({@code returnsCore}): it stays in the centre slot as the recipe's remainder, the
 * very stack (its taker's name, a Gauntlet's stones, every component) less nothing, so only the netherite, the diamonds
 * and the skull are used up. The Locator is <b>tuned</b> to the core's stone ({@code enhanced:locator {tuned}}: its
 * colour; the search is the same whatever it was tuned to): a Gauntlet's selected stone, or its first. A Gauntlet with
 * no stone, or the Titan's, is no core.
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

    /** Whether the core is given back (yes when the config is not loaded). */
    public static boolean returnsCore() {
        return !EnhancedConfig.SPEC.isLoaded() || LocatorConfig.RETURNS_CORE.get();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return craftable() && super.matches(input, level) && core(input) >= 0;
    }

    /** The slot of the input that holds the core (a locator core of a stone), or -1. */
    static int core(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModTags.LOCATOR_CORES)) {
                return StoneLocator.coreKind(stack) == null ? -1 : i;
            }
        }
        return -1;
    }

    /** The Locator, tuned to the core's stone. */
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        int core = core(input);
        StoneKind kind = core < 0 ? null : StoneLocator.coreKind(input.getItem(core));
        if (kind != null) {
            result.set(ModDataComponents.LOCATOR.get(), LocatorState.tunedTo(kind));
        }
        return result;
    }

    /** What stays in the grid: the core itself, unchanged (when {@code returnsCore}). */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> left = super.getRemainingItems(input);
        int core = core(input);
        if (core >= 0 && returnsCore()) {
            left.set(core, input.getItem(core).copyWithCount(1));
        }
        return left;
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
