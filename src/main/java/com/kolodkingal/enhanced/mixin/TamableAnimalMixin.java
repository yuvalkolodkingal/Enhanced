package com.kolodkingal.enhanced.mixin;

import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Vormir Cliff (3.6.1): a pet pushed off the edge does not teleport back to its owner while it falls into the
 * chasm (the follow and panic goals teleport a pet 12 blocks from its owner: a dog given to the fall was back by his
 * side, alive, before it reached the pit).
 */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {
    @Inject(method = "tryToTeleportToOwner", at = @At("HEAD"), cancellable = true)
    private void enhanced$fallsIntoTheChasm(CallbackInfo ci) {
        if (VormirTracker.falling((TamableAnimal) (Object) this)) {
            ci.cancel();
        }
    }
}
