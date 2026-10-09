package com.kolodkingal.enhanced.mixin;

import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vision's phasing: {@code Player.tick} sets {@code noPhysics = isSpectator()} every tick; a phased Vision gets no
 * physics too, so he passes through blocks (on his own client) and the server accepts the movement.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z", ordinal = 0))
    private boolean enhanced$phaseThroughWalls(boolean spectator) {
        return spectator || VisionAbilities.isPhased((Player) (Object) this);
    }
}
