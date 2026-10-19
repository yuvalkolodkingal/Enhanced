package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.registry.ModLootFunctions;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The loot function {@code enhanced:mark_spent} (3.7): a chest whose loot is rolled marks its site spent for the
 * stone {@code stone} ({@link SpentSites#mark}); the stack passes unchanged. On the Hydra outposts' vault chest
 * ({@code chests/hydra_outpost/vault}: its Scepter is the Mind Stone's vessel). Registered by the 3.7 skeleton; lane
 * L5 owns it.
 */
public class MarkSpentFunction extends LootItemConditionalFunction {
    public static final MapCodec<MarkSpentFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).and(
            LocatorState.KIND.fieldOf("stone").forGetter(f -> f.stone)
    ).apply(i, MarkSpentFunction::new));

    private final StoneKind stone;

    public MarkSpentFunction(List<LootItemCondition> conditions, StoneKind stone) {
        super(conditions);
        this.stone = stone;
    }

    /** The stone whose site it marks. */
    public StoneKind stone() {
        return stone;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin != null) {
            SpentSites.mark(context.getLevel(), stone, BlockPos.containing(origin));
        }
        return stack;
    }

    @Override
    public LootItemFunctionType<MarkSpentFunction> getType() {
        return ModLootFunctions.MARK_SPENT.get();
    }
}
