package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.config.SerumConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
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

import java.util.function.Supplier;

/**
 * 3.8: a shaped recipe that matches only while its config switch is on, off by default: the serum ({@code
 * [super_soldier_serum] craftable}, type {@code enhanced:serum}) and the two Arc Reactors ({@code [iron_man]
 * reactorsCraftable}, type {@code enhanced:arc_reactor}), for servers without Avengers Towers; in a tower they come
 * from its cases. The same fields as {@code minecraft:crafting_shaped}.
 */
public class SwitchedRecipe extends ShapedRecipe {
    /** Which switch a recipe of this kind obeys. */
    public enum Switch {
        SERUM(() -> SerumConfig.CRAFTABLE, ModRecipes.SERUM),
        ARC_REACTOR(() -> IronManConfig.REACTORS_CRAFTABLE, ModRecipes.ARC_REACTOR);

        private final Supplier<ConfigSpec.BooleanValue> option;
        private final Supplier<? extends RecipeSerializer<?>> serializer;

        Switch(Supplier<ConfigSpec.BooleanValue> option, Supplier<? extends RecipeSerializer<?>> serializer) {
            this.option = option;
            this.serializer = serializer;
        }

        /** Whether the switch is on (off where the config is not loaded, as by default). */
        public boolean on() {
            return EnhancedConfig.SPEC.isLoaded() && option.get().get();
        }
    }

    private final Switch kind;

    public SwitchedRecipe(Switch kind, String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                          ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.kind = kind;
    }

    static SwitchedRecipe of(Switch kind, ShapedRecipe shaped) {
        return new SwitchedRecipe(kind, shaped.getGroup(), shaped.category(), shaped.pattern,
                shaped.getResultItem(null), shaped.showNotification());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return kind.on() && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer.get();
    }

    /** The serializer of one switch: the shaped recipe's own codecs, read as this kind. */
    public static class Serializer implements RecipeSerializer<SwitchedRecipe> {
        private final MapCodec<SwitchedRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, SwitchedRecipe> streamCodec;

        public Serializer(Switch kind) {
            this.codec = ShapedRecipe.Serializer.CODEC.xmap(r -> of(kind, r), r -> r);
            this.streamCodec = ShapedRecipe.Serializer.STREAM_CODEC.map(r -> of(kind, r), r -> r);
        }

        @Override
        public MapCodec<SwitchedRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SwitchedRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
