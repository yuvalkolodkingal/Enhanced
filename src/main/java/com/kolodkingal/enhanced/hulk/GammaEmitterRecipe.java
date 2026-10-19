package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * The Gamma Emitter's recipe (3.6): a shaped recipe that matches only while {@code [gamma] emitterCraftable} is on
 * (type {@code enhanced:gamma_emitter}, the same fields as {@code minecraft:crafting_shaped}).
 */
public class GammaEmitterRecipe extends ShapedRecipe {
    public GammaEmitterRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                              net.minecraft.world.item.ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    static GammaEmitterRecipe of(ShapedRecipe shaped) {
        return new GammaEmitterRecipe(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(null),
                shaped.showNotification());
    }

    /** Whether the config lets the emitter be crafted (yes when it is not loaded). */
    public static boolean craftable() {
        return HulkAbilities.cfg(HulkConfig.EMITTER_CRAFTABLE, true);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return craftable() && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GAMMA_EMITTER.get();
    }

    /** The serializer: the shaped recipe's own codecs, read as this kind. */
    public static class Serializer implements RecipeSerializer<GammaEmitterRecipe> {
        private static final MapCodec<GammaEmitterRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(GammaEmitterRecipe::of, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, GammaEmitterRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(GammaEmitterRecipe::of, r -> r);

        @Override
        public MapCodec<GammaEmitterRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GammaEmitterRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
