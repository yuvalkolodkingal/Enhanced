package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One of the six Infinity Stones as an item (3.6): {@code enhanced:space_stone}, {@code mind_stone}, {@code
 * reality_stone}, {@code power_stone}, {@code time_stone}, {@code soul_stone}, keyed by {@link StoneKind} (its colour
 * and voice). Stacks to one, epic, fire-proof. Registered by the 3.6 skeleton as a plain item; lane L1 owns the class
 * (who may hold it, the grace and the burn, the glow, the tooltip) and lane L2 its uses ({@code StoneUse}).
 */
public class InfinityStoneItem extends Item {
    /** The stone this item is. */
    public final StoneKind kind;

    public InfinityStoneItem(StoneKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
