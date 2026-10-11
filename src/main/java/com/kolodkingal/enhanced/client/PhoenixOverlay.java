package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.render.PhoenixFx;
import com.kolodkingal.enhanced.client.render.PhoenixMath;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * The Phoenix's light on the screen: a gold flash for everyone caught in a Burst (full at the first tick, gone after
 * six; off with reduced motion), and a faint gold vignette round the edge of the screen of a Phoenix whose charge has
 * passed nine tenths.
 */
public final class PhoenixOverlay implements LayeredDraw.Layer {
    /** How many one-pixel frames the vignette is built of, working in from the edge. */
    private static final int VIGNETTE_DEPTH = 28;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        float time = mc.level.getGameTime() + deltaTracker.getGameTimeDeltaPartialTick(false);
        float flash = PhoenixFx.screenFlash(mc, time);
        if (flash > 0.0F && !mc.options.hideGui) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), argb(PhoenixMath.FIRE_GOLD, 0.45F * flash));
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), argb(PhoenixMath.FIRE_CORE, 0.15F * flash));
        }
        PowerData data = Powers.get(player);
        if (data.power() == Power.PHOENIX && data.is(Live.PHOENIX_CHARGING) && !mc.options.hideGui) {
            float charge = PhoenixFx.chargeOf(player, time);
            if (charge >= PhoenixMath.BRIGHT_CHARGE) {
                float pulse = EnhancedClientConfig.reducedMotion() ? 0.8F : 0.75F + 0.25F * Mth.sin(time * 0.6F);
                float strength = (charge - PhoenixMath.BRIGHT_CHARGE) / (1.0F - PhoenixMath.BRIGHT_CHARGE) * 0.5F + 0.5F;
                for (int i = 0; i < VIGNETTE_DEPTH; i++) {
                    float fall = 1.0F - i / (float) VIGNETTE_DEPTH;
                    graphics.renderOutline(i, i, graphics.guiWidth() - 2 * i, graphics.guiHeight() - 2 * i,
                            argb(PhoenixMath.FIRE_GOLD, 0.45F * fall * fall * pulse * strength));
                }
            }
        }
    }

    private static int argb(int rgb, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
