package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Remembers which Mjolnir is resting here (its name and any other data), so lifting it gives back the same hammer. */
public class MjolnirBlockEntity extends BlockEntity {
    private ItemStack hammer = ItemStack.EMPTY;

    public MjolnirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MJOLNIR.get(), pos, state);
    }

    public ItemStack hammer() {
        return hammer.isEmpty() ? new ItemStack(ModItems.MJOLNIR.get()) : hammer.copy();
    }

    public void setHammer(ItemStack stack) {
        hammer = stack.copyWithCount(1);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!hammer.isEmpty()) {
            tag.put("Hammer", hammer.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hammer = tag.contains("Hammer") ? ItemStack.parseOptional(registries, tag.getCompound("Hammer")) : ItemStack.EMPTY;
    }
}
