package com.kolodkingal.enhanced.platform.event.client;

import net.minecraft.world.entity.player.Player;

/** The field of view's modifier (sprinting, a bow...): change it. */
public class ComputeFovModifierEvent {
    private final Player player;
    private final float fovModifier;
    private float newFovModifier;

    public ComputeFovModifierEvent(Player player, float fovModifier) {
        this.player = player;
        this.fovModifier = fovModifier;
        this.newFovModifier = fovModifier;
    }

    public Player getPlayer() {
        return player;
    }

    public float getFovModifier() {
        return fovModifier;
    }

    public float getNewFovModifier() {
        return newFovModifier;
    }

    public void setNewFovModifier(float newFovModifier) {
        this.newFovModifier = newFovModifier;
    }
}
