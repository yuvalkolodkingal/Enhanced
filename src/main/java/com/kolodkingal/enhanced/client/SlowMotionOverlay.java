package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.SlowMotion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/** A cool blue tint at the edges of the screen while time is slowed around you. */
public class SlowMotionOverlay implements LayeredDraw.Layer {
    private float strength;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        boolean slowed = mc.player != null && mc.level != null && SlowMotion.isInZone(mc.level, mc.player.position());
        float target = slowed ? 1F : 0F;
        strength += (target - strength) * Math.min(1F, deltaTracker.getRealtimeDeltaTicks() * 0.15F);
        if (strength < 0.01F || mc.options.hideGui) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int edge = (int) (0x50 * strength) << 24 | 0x2A4D7A;
        int mid = (int) (0x10 * strength) << 24 | 0x2A4D7A;
        graphics.fillGradient(0, 0, w, h / 3, edge, mid);
        graphics.fill(0, h / 3, w, h - h / 3, mid);
        graphics.fillGradient(0, h - h / 3, w, h, mid, edge);
    }
}
