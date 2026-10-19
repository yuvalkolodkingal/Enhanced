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

import java.util.function.Consumer;

/**
 * The Weapon X Pod's block entity (3.7, lane L4; on its lower half): whether a treatment runs and since when (game
 * time), synced to the clients, which draw the hatch shut, the light brighter and the bubbles faster while it runs and
 * play the pod's hum. Lane L3's {@code PodTreatment} sets it ({@link #setRunning}); nothing else does.
 */
public class WeaponXPodBlockEntity extends BlockEntity {
    /** The client's tick of a pod (the hum), set by the client setup; the server never calls it. */
    public static Consumer<WeaponXPodBlockEntity> clientTick = pod -> {
    };

    private boolean running;
    private long since;

    public WeaponXPodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEAPON_X_POD.get(), pos, state);
    }

    public boolean running() {
        return running;
    }

    /** The game time the treatment began (while {@link #running}). */
    public long since() {
        return since;
    }

    /** Starts or stops a treatment (lane L3); the clients near are told. */
    public void setRunning(boolean running) {
        if (this.running == running) {
            return;
        }
        this.running = running;
        this.since = level == null ? 0 : level.getGameTime();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Nothing of a treatment is saved with the world: a treatment lives in the server's memory ({@code PodTreatment})
     * and never outlasts a restart, so a pod loaded from disk is always idle (its hatch open, no hum).
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("running");
        since = tag.getLong("since");
    }

    /** What the clients are told: whether a treatment runs, and since when. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("running", running);
        tag.putLong("since", since);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
