package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.player.Player;

/** A player's tick (NeoForge's PlayerTickEvent). */
public abstract class PlayerTickEvent extends PlayerEvent {
    protected PlayerTickEvent(Player entity) {
        super(entity);
    }

    /** Before a player's tick (both sides). */
    public static class Pre extends PlayerTickEvent {
        public Pre(Player entity) {
            super(entity);
        }
    }

    /** After a player's tick (both sides). */
    public static class Post extends PlayerTickEvent {
        public Post(Player entity) {
            super(entity);
        }
    }
}
