package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * While Mjolnir pins you down, or Sokovia carries you into the sky, you can look around, but you can't walk, jump, swim
 * up or start to fly: your own client stops the keys from moving you (the server holds you in place regardless, see
 * {@code MjolnirPin}).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class PinLock {

    @SubscribeEvent
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
