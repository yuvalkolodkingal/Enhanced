package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Minecraft's pause between one right click and the next, which Quicksilver cuts while he is quick. */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay")
    int enhanced$getRightClickDelay();

    @Accessor("rightClickDelay")
    void enhanced$setRightClickDelay(int ticks);
}
