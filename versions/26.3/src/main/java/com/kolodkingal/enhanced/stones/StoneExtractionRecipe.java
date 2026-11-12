package com.kolodkingal.enhanced.stones;
//#norenames

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
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.21.4's copy of the stone extraction (the recipe API changed in 1.21.2): one ingredient anywhere in the grid gives
 * the stone and leaves the empty vessel in its slot.
 */
public class StoneExtractionRecipe implements CraftingRecipe {
    private final String group;
    private final Ingredient ingredient;
    // (26.3: a recipe holds item templates; an item's components are not known while recipes are read)
    private final net.minecraft.world.item.ItemStackTemplate result;
    private final net.minecraft.world.item.ItemStackTemplate remainder;

    public StoneExtractionRecipe(String group, Ingredient ingredient, net.minecraft.world.item.ItemStackTemplate result,
                                 net.minecraft.world.item.ItemStackTemplate remainder) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.remainder = remainder;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStack result() {
        return result.create();
    }

    /** The empty vessel left in the ingredient's slot. */
    public ItemStack remainder() {
        return remainder.create();
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
    public ItemStack assemble(CraftingInput input) {
        return result.create();
    }

    /** 1.21.1's result getter (no longer the recipe API's). */
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.create();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> left = CraftingRecipe.super.getRemainingItems(input);
        for (int i = 0; i < input.size(); i++) {
            if (ingredient.test(input.getItem(i))) {
                left.set(i, remainder.create());
            }
        }
        return left;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredient);
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModRecipes.STONE_EXTRACTION.get();
    }

    /** The recipe's JSON: {@code group} (optional), {@code ingredient}, {@code result}, {@code remainder}. */
    private static final MapCodec<StoneExtractionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.ingredient),
            net.minecraft.world.item.ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
            net.minecraft.world.item.ItemStackTemplate.CODEC.fieldOf("remainder").forGetter(r -> r.remainder)
    ).apply(i, StoneExtractionRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, StoneExtractionRecipe> STREAM_CODEC =
            StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, r -> r.group,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
            net.minecraft.world.item.ItemStackTemplate.STREAM_CODEC, r -> r.result,
            net.minecraft.world.item.ItemStackTemplate.STREAM_CODEC, r -> r.remainder,
            StoneExtractionRecipe::new);

    public static final RecipeSerializer<StoneExtractionRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
