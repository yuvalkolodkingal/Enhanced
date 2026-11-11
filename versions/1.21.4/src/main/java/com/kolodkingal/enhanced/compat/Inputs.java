package com.kolodkingal.enhanced.compat;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

/** 1.21.4 only: the movement keys are an immutable record on {@link ClientInput}; this sets one, as the fields were set. */
public final class Inputs {
    private Inputs() {
    }

    public static void set(ClientInput input, String key, boolean value) {
        Input k = input.keyPresses;
        input.keyPresses = new Input(
                key.equals("up") ? value : k.forward(),
                key.equals("down") ? value : k.backward(),
                key.equals("left") ? value : k.left(),
                key.equals("right") ? value : k.right(),
                key.equals("jumping") ? value : k.jump(),
                key.equals("shiftKeyDown") ? value : k.shift(),
                k.sprint());
    }
}
