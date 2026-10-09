package com.kolodkingal.enhanced.mixin;

import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vision's phasing: a phased Vision is treated like a spectator where the player's physics are concerned.
 * <ul>
 *     <li>{@code Player.tick} sets {@code noPhysics = isSpectator()} and keeps spectators off the ground: he passes
 *     through blocks (on his own client) and the server accepts the movement, and his client doesn't think he is
 *     standing on the floor he sinks into.</li>
 *     <li>{@code updatePlayerPose} doesn't squeeze him into a crouch or crawl while he passes through a floor.</li>
 * </ul>
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z"))
    private boolean enhanced$phaseThroughWalls(boolean spectator) {
        return spectator || VisionAbilities.isPhased((Player) (Object) this);
    }

    @ModifyExpressionValue(method = "updatePlayerPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z"))
    private boolean enhanced$standTallWhilePhased(boolean spectator) {
        return spectator || VisionAbilities.isPhased((Player) (Object) this);
    }
}
