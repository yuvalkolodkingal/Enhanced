package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.MovementInputUpdateEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.player.Input;

/**
 * While Mjolnir pins you down, or Sokovia carries you into the sky, you can look around, but you can't walk, jump, swim
 * up or start to fly: your own client stops the keys from moving you (the server holds you in place regardless, see
 * {@code MjolnirPin}).
 */
@Subscriber(side = Side.CLIENT)
public final class PinLock {

    @Subscribe
    public static void onMovementInput(MovementInputUpdateEvent event) {
        PowerData data = Powers.get(event.getEntity());
        if (data.is(Live.PINNED) || data.is(Live.LIFTED)) {
            Input input = event.getInput();
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
        }
    }

    private PinLock() {
    }
}
