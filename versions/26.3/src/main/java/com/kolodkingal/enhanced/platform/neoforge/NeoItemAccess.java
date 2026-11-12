package com.kolodkingal.enhanced.platform.neoforge;
//#norenames

import com.kolodkingal.enhanced.platform.ItemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/** 26.3: a block's item slots through NeoForge's resource handler (26.3 has no item handler). */
final class NeoItemAccess implements ItemAccess {
    @Override
    public @Nullable Slots blockSlots(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity,
                                      @Nullable Direction side) {
        var handler = state == null && entity == null
                ? level.getCapability(Capabilities.Item.BLOCK, pos, side)
                : level.getCapability(Capabilities.Item.BLOCK, pos, state, entity, side);
        return handler == null ? null : new Handler(handler);
    }

    private record Handler(ResourceHandler<ItemResource> handler) implements Slots {
        @Override
        public int getSlots() {
            return handler.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemResource r = handler.getResource(slot);
            return r.isEmpty() ? ItemStack.EMPTY : r.toStack(handler.getAmountAsInt(slot));
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemResource r = handler.getResource(slot);
            if (r.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int got = handler.extract(slot, r, amount, tx);
                if (!simulate) {
                    tx.commit();
                }
                return got <= 0 ? ItemStack.EMPTY : r.toStack(got);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int put = handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                if (!simulate) {
                    tx.commit();
                }
                return put >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - put);
            }
        }
    }
}
