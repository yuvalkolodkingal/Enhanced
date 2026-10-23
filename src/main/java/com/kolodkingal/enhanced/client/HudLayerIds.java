package com.kolodkingal.enhanced.client;

import net.minecraft.resources.ResourceLocation;

/**
 * The names of the vanilla HUD layers the mod looks for in {@code RenderGuiLayerEvent} (port P0: the names NeoForge
 * gives them, {@code VanillaGuiLayers}, which every loader's bridge reports).
 */
public final class HudLayerIds {
    public static final ResourceLocation CROSSHAIR = ResourceLocation.withDefaultNamespace("crosshair");
    public static final ResourceLocation BOSS_OVERLAY = ResourceLocation.withDefaultNamespace("boss_overlay");
    public static final ResourceLocation OVERLAY_MESSAGE = ResourceLocation.withDefaultNamespace("overlay_message");

    private HudLayerIds() {
    }
}
