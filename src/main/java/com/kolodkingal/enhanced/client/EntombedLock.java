package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.MovementInputUpdateEvent;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.client.player.Input;

/**
 * While a cocoon of sand holds you, you can look around but your own keys do not move you: the server holds you in place
 * regardless (see {@code SandCocoonEntity}), so this only spares you the rubber-banding of walking into the sand.
 */
@Subscriber(side = Side.CLIENT)
public final class EntombedLock {

    @Subscribe
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity().hasEffect(ModEffects.ENTOMBED)) {
            Input input = event.getInput();
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.shiftKeyDown = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
        }
    }

    private EntombedLock() {
    }
}
