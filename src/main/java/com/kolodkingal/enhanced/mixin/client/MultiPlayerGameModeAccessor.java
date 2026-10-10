package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The pause after breaking a block before the next one can be broken, which Quicksilver cuts while he is quick. */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyDelay")
    int enhanced$getDestroyDelay();

    @Accessor("destroyDelay")
    void enhanced$setDestroyDelay(int ticks);
}
