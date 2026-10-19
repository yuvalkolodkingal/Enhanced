package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.config.WolverineConfig;
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
 * The Adamantium Injection's recipe (3.7), for servers without labs: a shaped recipe ({@code I N I / N E N / I N I}:
 * iron blocks, netherite ingots, an echo shard) that matches only while {@code [adamantium_injection] craftable} is on,
 * off by default: the injection comes from the lab's Weapon X rack (type {@code enhanced:adamantium_injection}, the
 * same fields as {@code minecraft:crafting_shaped}).
 */
public class InjectionRecipe extends ShapedRecipe {
    public InjectionRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                              net.minecraft.world.item.ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    static InjectionRecipe of(ShapedRecipe shaped) {
        return new InjectionRecipe(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(null),
                shaped.showNotification());
    }

    /** Whether the config lets the injection be crafted (no when it is not loaded, as by default). */
    public static boolean craftable() {
        return WolverineAbilities.cfg(WolverineConfig.INJECTION_CRAFTABLE, false);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return craftable() && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ADAMANTIUM_INJECTION.get();
    }

    /** The serializer: the shaped recipe's own codecs, read as this kind. */
    public static class Serializer implements RecipeSerializer<InjectionRecipe> {
        private static final MapCodec<InjectionRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(InjectionRecipe::of, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, InjectionRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(InjectionRecipe::of, r -> r);

        @Override
        public MapCodec<InjectionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InjectionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
