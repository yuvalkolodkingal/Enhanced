package com.kolodkingal.enhanced.platform.neoforge;

import com.kolodkingal.enhanced.platform.ItemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Blocks' items on NeoForge: the item handler capability. */
final class NeoItemAccess implements ItemAccess {
    @Override
    public @Nullable Slots blockSlots(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity,
                                      @Nullable Direction side) {
        IItemHandler handler = state == null && entity == null
                ? level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)
                : level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, entity, side);
        return handler == null ? null : new Handler(handler);
    }

    private record Handler(IItemHandler handler) implements Slots {
        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }
    }
}
