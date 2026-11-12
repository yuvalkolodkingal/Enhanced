package com.kolodkingal.enhanced.wolverine;
//#norenames

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
public class InjectionRecipe extends com.kolodkingal.enhanced.compat.ShapedBase {
    /** The serializer: the shaped recipe's own fields, read as this kind (26.3). */
    public static final RecipeSerializer<InjectionRecipe> SERIALIZER = serializer(InjectionRecipe::new);

    public InjectionRecipe(net.minecraft.world.item.crafting.Recipe.CommonInfo common,
            net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern,
            net.minecraft.world.item.ItemStackTemplate result) {
        super(common, book, pattern, result);
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
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return as(ModRecipes.ADAMANTIUM_INJECTION.get());
    }

}
