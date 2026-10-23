package com.kolodkingal.enhanced.platform.ext;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/**
 * The loader's block entity hooks the mod's block entities answer (port P0, PLAN2 3.3), with the exact signatures of
 * NeoForge's {@code IBlockEntityExtension} methods of the same names (see {@link ItemExt}). Client side: the client
 * receives the block entity's data (the chunk's update tag, a data packet). Fabric (P1): vanilla loads both with
 * {@code loadWithComponents}; a mixin routes them here.
 */
public final class BlockEntityExt {
    private BlockEntityExt() {
    }

    /** The client's copy reads the server's data. */
    public interface UpdatePacket {
        void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider);

        void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider);
    }
}
