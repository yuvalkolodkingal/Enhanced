package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A stone crafted out of its vessel (3.6, recipe type {@code enhanced:stone_extraction}, the plan's 1.3): one
 * ingredient alone in the grid (Loki's Scepter, the Tesseract) gives the stone ({@code result}), and the ingredient's
 * slot keeps the empty vessel ({@code remainder}). A crafting recipe: it works in any crafting table and shows in the
 * recipe book. The 3.6 skeleton writes it whole, so the recipes load; lane L1 owns the class (and the advancements that
 * unlock the recipes).
 */
public class StoneExtractionRecipe implements CraftingRecipe {
    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final ItemStack remainder;

    public StoneExtractionRecipe(String group, Ingredient ingredient, ItemStack result, ItemStack remainder) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.remainder = remainder;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStack result() {
        return result;
    }

    /** The empty vessel left in the ingredient's slot. */
    public ItemStack remainder() {
        return remainder;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 1) {
            return false;
        }
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) {
                return ingredient.test(stack);
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> left = CraftingRecipe.super.getRemainingItems(input);
        for (int i = 0; i < input.size(); i++) {
            if (ingredient.test(input.getItem(i))) {
                left.set(i, remainder.copy());
            }
        }
        return left;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.STONE_EXTRACTION.get();
    }

    /** The recipe's JSON: {@code group} (optional), {@code ingredient}, {@code result}, {@code remainder}. */
    public static final class Serializer implements RecipeSerializer<StoneExtractionRecipe> {
        private static final MapCodec<StoneExtractionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                ItemStack.STRICT_CODEC.fieldOf("remainder").forGetter(r -> r.remainder)
        ).apply(i, StoneExtractionRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, StoneExtractionRecipe> STREAM_CODEC =
                StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
                ItemStack.STREAM_CODEC, r -> r.result,
                ItemStack.STREAM_CODEC, r -> r.remainder,
                StoneExtractionRecipe::new);

        @Override
        public MapCodec<StoneExtractionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StoneExtractionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
