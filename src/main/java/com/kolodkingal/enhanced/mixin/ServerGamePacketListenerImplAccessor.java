package com.kolodkingal.enhanced.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The server's "floating too long" kick counter, which the mod resets for players it holds up (telekinesis, a carry,
 * a lift, a swing). Was an access transformer line before the port.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
    @Accessor("aboveGroundTickCount")
    int enhanced$getAboveGroundTickCount();

    @Accessor("aboveGroundTickCount")
    void enhanced$setAboveGroundTickCount(int ticks);
}
