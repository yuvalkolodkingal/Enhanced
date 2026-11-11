package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.5: the input's movement is one protected vector (the mod stops a held player's walking by zeroing it). */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("moveVector")
    void enhanced$setMoveVector(Vec2 vector);
}
