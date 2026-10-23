package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;

/** The client player's movement input of this tick: change it. */
public class MovementInputUpdateEvent extends PlayerEvent {
    private final Input input;

    public MovementInputUpdateEvent(Player entity, Input input) {
        super(entity);
        this.input = input;
    }

    public Input getInput() {
        return input;
    }
}
