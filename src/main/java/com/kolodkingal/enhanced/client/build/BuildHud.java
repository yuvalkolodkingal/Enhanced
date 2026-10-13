package com.kolodkingal.enhanced.client.build;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * Build Magic on the builder's screen (D-B 6.3 to 6.7), the layer {@code enhanced:build_magic} registered below the debug
 * overlay: the badge next to the crosshair (the spell, the material, the size, Billy's meter and rank), the hold and
 * progress rings, the done pop, the hint strip at the bottom right, the first-time tips, the crosshair rune and the faint
 * edge tint while building. The sixth line of the hero panel ({@code PowerHud}) is not drawn here.
 *
 * <p>A stub of the skeleton (3.1, step S): it draws nothing yet. Lane BC fills it.
 */
public final class BuildHud implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
    }
}
