package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.registry.ModLootFunctions;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Locale;

/**
 * The loot function {@code enhanced:site_stone} (3.6): a chest that holds a site stone (the End tunnel's vault: the Orb
 * with the Power Stone) gives it only to an opener the ledger lets take it ({@code [stones] uniqueness}), with the
 * opener in the context ({@code this_entity}); anyone else gets a note ("The Orb is gone."). The ledger counts it as
 * taken by the opener, whose name the stack (the Orb, or the stone) carries from then on. A chest emptied by a hopper
 * (no opener) gives the note. Registered by the 3.6 skeleton; lane L1 writes the body.
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
        Entity opener = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (opener instanceof ServerPlayer player && StoneLedger.mayTake(player, stone)) {
            StoneLedger.taken(player, stone);
            StoneLedger.stamp(stack, player.getUUID());
            return stack;
        }
        return note(stone);
    }

    /** The note left in the chest instead: "The Orb is gone." (the Power Stone's), "The Time Stone is gone." */
    public static ItemStack note(StoneKind stone) {
        ItemStack note = new ItemStack(Items.PAPER);
        note.set(DataComponents.CUSTOM_NAME, stone == StoneKind.POWER
                ? Component.translatable("item.enhanced.orb_gone")
                : Component.translatable("item.enhanced.site_stone_gone", stone.displayName()));
        return note;
    }

    @Override
    public LootItemFunctionType<SiteStoneLootFunction> getType() {
        return ModLootFunctions.SITE_STONE.get();
    }
}
