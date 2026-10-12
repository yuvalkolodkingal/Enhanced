package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.render.ApocalypseAuraLayer;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * What the screen shows of Apocalypse when the world is not drawn round you: an amber shade at the edges while you are
 * held in a cocoon of sand (your own body is not drawn in first person, and the cocoon is seen from the inside), and a
 * blue one growing while you take in the Essence. Both ease in and out; both are gone with the interface.
 */
public class ApocalypseOverlay implements LayeredDraw.Layer {
    /** The shade of the cocoon (the sand) and of the transference (the soul of the Essence), RGB. */
    private static final int AMBER = 0x8C6B2F;
    private static final int SAPPHIRE = 0x3FA0FF;

    private float cocoon;
    private float essence;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            cocoon = 0.0F;
            essence = 0.0F;
            return;
        }
        float ease = Math.min(1.0F, deltaTracker.getRealtimeDeltaTicks() * 0.2F);
        cocoon += ((mc.player.hasEffect(ModEffects.ENTOMBED) ? 1.0F : 0.0F) - cocoon) * ease;
        essence += (ApocalypseAuraLayer.transference(mc.player) - essence) * ease;
        if (mc.options.hideGui) {
            return;
        }
        if (cocoon > 0.01F) {
            shade(graphics, AMBER, 0.55F * cocoon);
        }
        if (essence > 0.01F) {
            shade(graphics, SAPPHIRE, 0.45F * essence);
        }
    }

    /** A shade at the top and the bottom of the screen, strongest at the edge, as the other overlays of the mod. */
    private static void shade(GuiGraphics graphics, int rgb, float alpha) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int edge = (Math.round(255 * alpha) & 0xFF) << 24 | rgb;
        int mid = (Math.round(255 * alpha * 0.2F) & 0xFF) << 24 | rgb;
        graphics.fillGradient(0, 0, w, h / 3, edge, mid);
        graphics.fill(0, h / 3, w, h - h / 3, mid);
        graphics.fillGradient(0, h - h / 3, w, h, mid, edge);
    }
}
