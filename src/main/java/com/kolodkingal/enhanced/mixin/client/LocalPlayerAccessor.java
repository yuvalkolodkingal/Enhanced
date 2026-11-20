package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The ticks since the client last sent its position (vanilla sends it when the player moves, or once these reach 20).
 * Spider-Man's cling sets it to 20 so that vanilla's own move packet of the tick carries his position while he hangs
 * still: a second move packet of the mod's in the same tick is what the server of 26.x kicks for ("Invalid move player
 * packet received": one position a tick).
 */
@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Accessor("positionReminder")
    int enhanced$getPositionReminder();

    @Accessor("positionReminder")
    void enhanced$setPositionReminder(int ticks);
}
