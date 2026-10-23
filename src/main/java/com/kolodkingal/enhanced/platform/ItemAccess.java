package com.kolodkingal.enhanced.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The items a block holds when it is not a vanilla {@code Container} (another mod's storage): NeoForge's item handler
 * capability, Fabric's Transfer API. The method names are the item handler's, so a call site changes only its types.
 */
public interface ItemAccess {
    /**
     * The slots of the block at {@code pos} seen from {@code side} (null: from inside), or null if it holds no items.
     * {@code state} and {@code entity} may be null; passing them saves the loader a lookup.
     */
    @Nullable
    Slots blockSlots(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity,
                     @Nullable Direction side);

    /** {@link #blockSlots(Level, BlockPos, BlockState, BlockEntity, Direction)} from inside. */
    @Nullable
    default Slots blockSlots(Level level, BlockPos pos) {
        return blockSlots(level, pos, null, null, null);
    }

    /** A view of a block's slots. */
    interface Slots {
        int getSlots();

        /** What is in the slot (do not change it: take with {@link #extractItem}). */
        ItemStack getStackInSlot(int slot);

        /** Takes up to {@code amount} items out of the slot (or only says what it would take, if {@code simulate}). */
        ItemStack extractItem(int slot, int amount, boolean simulate);

        /**
         * Puts {@code stack} into the slot (or only says what would be left, if {@code simulate}); returns what did not
         * fit. {@code stack} itself is not changed.
         */
        ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
    }
}
