package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.odin.GungnirStand;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Gungnir's stand's block entity (3.5): it is there so that the client draws the spear on the stand. 3.6.1: it also
 * tells the clients the world's Gungnir owner ({@code [gungnir] onePerWorld}; read from {@code AsgardState} when the
 * stand is sent, never saved here), so the stand is drawn empty for everyone else.
 */
public class GungnirStandBlockEntity extends BlockEntity {
    /** On the client: the world's Gungnir owner, or null (none, or every victor may take one). */
    @Nullable
    private UUID owner;

    public GungnirStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUNGNIR_STAND.get(), pos, state);
    }

    /** Whether the stand shows the spear to this viewer: no owner yet, he is the owner, or he is in Creative. */
    public boolean shows(@Nullable Player viewer) {
        return owner == null || viewer == null || viewer.isCreative() || owner.equals(viewer.getUUID());
    }

    /** The world's owner has changed (server): the clients are told. */
    public void ownerChanged() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        UUID now = level instanceof ServerLevel server ? GungnirStand.owner(server.getServer()) : null;
        if (now != null) {
            tag.putUUID("owner", now);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
