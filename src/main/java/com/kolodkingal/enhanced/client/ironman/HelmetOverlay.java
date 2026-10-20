package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The faceplate in first person (3.8, PLAN 5.8, lane L3): at the end of a suit-up the helmet closes over the screen's
 * edges (dark plates slide in from the top and the bottom, leaving the eye slit) and opens again as the HUD boots ("the
 * eyes light up": a blue flash and "SYSTEMS ONLINE"); a take-off opens the faceplate at its start. Client option {@code
 * suitUpOverlay} (on); with {@code reduceMotion} the plates do not move: the screen only darkens and clears.
 */
public final class HelmetOverlay {
    /** The share of a suit-up at its end the faceplate's closing takes (the helmet is the last part on). */
    static final float CLOSE_SHARE = 0.25F;
    /** Ticks the boot's flash lasts after a suit-up ends. */
    static final int BOOT_TICKS = 14;
    /** The local player's last suit-up's end (client game time) and whether a phase played last frame. */
    private static long bootAt = Long.MIN_VALUE / 2;
    private static boolean wasUp;

    static void render(GuiGraphics g, Minecraft mc, LocalPlayer player, IronManState state, float pt) {
        long now = mc.level.getGameTime();
        boolean up = state.phase() == IronManState.UP;
        if (wasUp && !up && state.suit() != com.kolodkingal.enhanced.ironman.SuitKind.NONE) {
            bootAt = now;
        }
        wasUp = up;
        if (!EnhancedClientConfig.suitUpOverlay()) {
            return;
        }
        float closed = 0.0F;
        if (state.inPhase()) {
            float progress = SuitTimelines.progress(now, state.phaseStart(), state.phaseTicks(), pt);
            if (up) {
                closed = Mth.clamp((progress - (1.0F - CLOSE_SHARE)) / CLOSE_SHARE, 0.0F, 1.0F);
            } else {
                closed = 1.0F - Mth.clamp(progress / CLOSE_SHARE, 0.0F, 1.0F);
            }
        }
        float since = now - bootAt + pt;
        if (since >= 0.0F && since < BOOT_TICKS) {
            // The faceplate is shut and the HUD wakes: the plates draw back a little, a blue flash.
            float k = 1.0F - since / BOOT_TICKS;
            closed = Math.max(closed, 0.6F * k);
            int a = (int) (k * k * 120);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0x9FE8FF);
            if (since < BOOT_TICKS * 0.8F) {
                Component line = Component.translatable("hud.enhanced.iron_man.boot");
                int w = mc.font.width(line);
                g.drawString(mc.font, line, (g.guiWidth() - w) / 2, g.guiHeight() / 2 + 18,
                        ((int) (255 * k) << 24) | 0x9FE8FF, true);
            }
        }
        if (closed <= 0.001F) {
            return;
        }
        plates(g, closed, EnhancedClientConfig.reducedMotion());
    }

    /** The two plates over the screen's top and bottom, {@code closed} of the way to the eye slit. */
    static void plates(GuiGraphics g, float closed, boolean still) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (still) {
            int a = (int) (closed * 170);
            g.fill(0, 0, w, h, a << 24);
            return;
        }
        float eased = closed * closed * (3.0F - 2.0F * closed);
        int slit = Math.max(10, h / 9);
        int reach = Math.round((h / 2.0F - slit) * eased);
        int metal = 0xF0281A1C;
        int edge = 0xFF9FE8FF;
        g.fill(0, 0, w, reach, metal);
        g.fill(0, h - reach, w, h, metal);
        if (reach > 2) {
            g.fill(0, reach - 1, w, reach, edge);
            g.fill(0, h - reach, w, h - reach + 1, edge);
        }
    }

    private HelmetOverlay() {
    }
}
