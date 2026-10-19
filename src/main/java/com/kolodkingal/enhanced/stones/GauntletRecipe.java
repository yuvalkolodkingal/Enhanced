package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * The empty Infinity Gauntlet's recipe (3.6, recipe type {@code enhanced:gauntlet_shaped}, the plan's 1.3): a shaped
 * crafting recipe like any other (its JSON is a {@code crafting_shaped}'s), which matches only while {@code [gauntlet]
 * craftable} is on, so a server can turn the crafted Gauntlet off without a data pack.
 */
public class GauntletRecipe extends ShapedRecipe {
    public GauntletRecipe(ShapedRecipe shaped) {
        super(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(null),
                shaped.showNotification());
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GAUNTLET_SHAPED.get();
    }

    /** The JSON of a {@code crafting_shaped} recipe. */
    public static final class Serializer implements RecipeSerializer<GauntletRecipe> {
        private static final MapCodec<GauntletRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(GauntletRecipe::new,
                r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, GauntletRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(GauntletRecipe::new, r -> r);

        @Override
        public MapCodec<GauntletRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GauntletRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
