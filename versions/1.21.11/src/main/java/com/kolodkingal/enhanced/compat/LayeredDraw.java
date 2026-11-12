package com.kolodkingal.enhanced.compat;
//#norenames

import net.neoforged.neoforge.client.gui.GuiLayer;

/** 1.21.11 only: vanilla's {@code LayeredDraw} is gone from 1.21.6; a HUD layer of the mod is NeoForge's {@code GuiLayer}. */
public final class LayeredDraw {
    private LayeredDraw() {
    }

    /** 1.21.1's {@code LayeredDraw.Layer}. */
    public interface Layer extends GuiLayer {
    }

    /** A NeoForge layer as one of the mod's (the same layer if it is one). */
    public static Layer of(GuiLayer layer) {
        return layer instanceof Layer l ? l : layer::render;
    }
}
