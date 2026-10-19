package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Cerebro console's block entity (3.7, lane L4): whether the console is in use, synced to the clients. While it is
 * not, the renderer draws Cerebro's helmet on its stand; while it is (lane L5's {@code CerebroRoom} sets it when a
 * telepath puts the helmet on), the stand is empty and the panels glow brighter.
 */
public class CerebroConsoleBlockEntity extends BlockEntity {
    private boolean inUse;

    public CerebroConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CEREBRO_CONSOLE.get(), pos, state);
    }

    public boolean inUse() {
        return inUse;
    }

    /** The helmet taken (true) or put back (false) (lane L5); the clients near are told. */
    public void setInUse(boolean inUse) {
        if (this.inUse == inUse) {
            return;
        }
        this.inUse = inUse;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("in_use", inUse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inUse = tag.getBoolean("in_use");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
