package com.kolodkingal.enhanced.hulk;
//#norenames

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
public class GammaEmitterRecipe extends com.kolodkingal.enhanced.compat.ShapedBase {
    /** The serializer: the shaped recipe's own fields, read as this kind (26.3). */
    public static final RecipeSerializer<GammaEmitterRecipe> SERIALIZER = serializer(GammaEmitterRecipe::new);

    public GammaEmitterRecipe(net.minecraft.world.item.crafting.Recipe.CommonInfo common,
            net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern,
            net.minecraft.world.item.ItemStackTemplate result) {
        super(common, book, pattern, result);
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
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return as(ModRecipes.GAMMA_EMITTER.get());
    }

}
