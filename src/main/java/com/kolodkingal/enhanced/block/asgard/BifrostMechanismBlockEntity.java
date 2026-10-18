package com.kolodkingal.enhanced.block.asgard;

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
 * The Bifrost mechanism of the Observatory (3.5): remembers when the Bifrost last opened or landed near it (a game time),
 * so that its ring spins up for {@link #SPIN_TICKS} ticks (drawn by the client's renderer from that time and the partial
 * tick). The time is sent to the clients that see it.
 */
public class BifrostMechanismBlockEntity extends BlockEntity {
    /** How long the ring spins after the Bifrost touched down or left. */
    public static final int SPIN_TICKS = 60;

    private long spinStart = Long.MIN_VALUE / 2;

    public BifrostMechanismBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIFROST_MECHANISM.get(), pos, state);
    }

    public long spinStart() {
        return spinStart;
    }

    /** Spins the ring up from {@code gameTime} on (server: saved and sent to the clients). */
    public void spin(long gameTime) {
        spinStart = gameTime;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The ring's angle (degrees) at {@code time} (ticks, may be fractional): it turns slowly always, and twice round more
     * during a spin, smoothly (so it never jumps).
     */
    public static float angle(long spinStart, double time) {
        double since = time - spinStart;
        double extra = 0;
        if (since >= SPIN_TICKS) {
            extra = 720;
        } else if (since > 0) {
            double t = since / SPIN_TICKS;
            extra = 720 * t * t * (3 - 2 * t);
        }
        return (float) ((time * 1.5 + extra) % 360.0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("SpinStart", spinStart);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spinStart = tag.contains("SpinStart") ? tag.getLong("SpinStart") : Long.MIN_VALUE / 2;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("SpinStart", spinStart);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
