package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.SlowMotion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.FastColor;
import com.kolodkingal.enhanced.power.Power;

/**
 * A cool grey shade at the edges of the screen while time is slowed around you; (3.3) a teal one while it is Tommy's
 * slow time that holds you. The colour eases from one to the other.
 */
public class SlowMotionOverlay implements LayeredDraw.Layer {
    /** The shade of Quicksilver Time, and of Tommy's slow time (RGB). */
    private static final int QUICKSILVER_SHADE = 0x34404E;
    private static final int TOMMY_SHADE = 0x1E5A4E;
    private float strength;
    /** How much of the shade is Tommy's (eased, 0..1). */
    private float teal;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Power owner = mc.player != null && mc.level != null ? SlowMotion.zoneOwner(mc.level, mc.player.position()) : Power.NONE;
        boolean slowed = owner != Power.NONE;
        float target = slowed ? 1F : 0F;
        float k = Math.min(1F, deltaTracker.getRealtimeDeltaTicks() * 0.15F);
        strength += (target - strength) * k;
        if (slowed) {
            teal += ((owner == Power.TOMMY ? 1F : 0F) - teal) * k;
        }
        if (strength < 0.01F || mc.options.hideGui) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int shade = FastColor.ARGB32.lerp(teal, QUICKSILVER_SHADE, TOMMY_SHADE) & 0xFFFFFF;
        int edge = (int) (0x50 * strength) << 24 | shade;
        int mid = (int) (0x10 * strength) << 24 | shade;
        graphics.fillGradient(0, 0, w, h / 3, edge, mid);
        graphics.fill(0, h / 3, w, h - h / 3, mid);
        graphics.fillGradient(0, h - h / 3, w, h, mid, edge);
    }
}
