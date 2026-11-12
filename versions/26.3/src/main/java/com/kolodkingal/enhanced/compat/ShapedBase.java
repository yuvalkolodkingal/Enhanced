package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * 26.3 only: the base of the mod's shaped recipes (the same fields as {@code minecraft:crafting_shaped}, read as the
 * mod's own type). 26.3's recipe serializer is a record of two codecs, so each kind makes one with {@link #serializer}.
 */
public abstract class ShapedBase extends ShapedRecipe {
    /** What a kind is made from: the shaped recipe's own fields. */
    public interface Factory<T extends ShapedBase> {
        T make(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern, ItemStackTemplate result);
    }

    protected final ItemStackTemplate resultTemplate;

    protected ShapedBase(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern,
                         ItemStackTemplate result) {
        super(common, book, pattern, result);
        this.resultTemplate = result;
    }

    public static <T extends ShapedBase> RecipeSerializer<T> serializer(Factory<T> factory) {
        MapCodec<T> codec = RecordCodecBuilder.mapCodec(i -> i.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.resultTemplate)
        ).apply(i, factory::make));
        StreamCodec<RegistryFriendlyByteBuf, T> stream = StreamCodec.composite(
                Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
                CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
                ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
                ItemStackTemplate.STREAM_CODEC, o -> o.resultTemplate,
                factory::make);
        return new RecipeSerializer<>(codec, stream);
    }

    /** {@code getSerializer}'s answer: a serializer of this kind, as the shaped recipe's type it must return. */
    @SuppressWarnings("unchecked")
    protected static RecipeSerializer<ShapedRecipe> as(RecipeSerializer<?> serializer) {
        return (RecipeSerializer<ShapedRecipe>) serializer;
    }
}
