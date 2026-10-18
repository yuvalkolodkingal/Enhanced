package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A Bifrost rune stone's memory (3.5): the game time from which it can call the Bifrost again. Nothing else. */
public class BifrostRuneStoneBlockEntity extends BlockEntity {
    private long readyAt;

    public BifrostRuneStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIFROST_RUNE_STONE.get(), pos, state);
    }

    public long readyAt() {
        return readyAt;
    }

    public void setReadyAt(long time) {
        readyAt = time;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ReadyAt", readyAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readyAt = tag.getLong("ReadyAt");
    }
}
