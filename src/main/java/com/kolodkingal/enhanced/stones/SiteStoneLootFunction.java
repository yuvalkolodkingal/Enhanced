package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.registry.ModLootFunctions;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Locale;

/**
 * The loot function {@code enhanced:site_stone} (3.6): a chest that holds a site stone (the End tunnel's vault: the Orb
 * with the Power Stone) gives it only to an opener the ledger lets take it ({@code [stones] uniqueness}), with the
 * opener in the context ({@code this_entity}); anyone else gets a note ("The Orb is gone."). Registered by the 3.6
 * skeleton with a body that changes nothing; lane L1 owns the class and writes it.
 */
public class SiteStoneLootFunction extends LootItemConditionalFunction {
    /** A stone by its name ({@code power}). */
    private static final Codec<StoneKind> KIND = Codec.STRING.xmap(s -> StoneKind.valueOf(s.toUpperCase(Locale.ROOT)),
            k -> k.name().toLowerCase(Locale.ROOT));

    public static final MapCodec<SiteStoneLootFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).and(
            KIND.fieldOf("stone").forGetter(f -> f.stone)
    ).apply(i, SiteStoneLootFunction::new));

    private final StoneKind stone;

    public SiteStoneLootFunction(List<LootItemCondition> conditions, StoneKind stone) {
        super(conditions);
        this.stone = stone;
    }

    /** The site stone it guards. */
    public StoneKind stone() {
        return stone;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        return stack;
    }

    @Override
    public LootItemFunctionType<SiteStoneLootFunction> getType() {
        return ModLootFunctions.SITE_STONE.get();
    }
}
