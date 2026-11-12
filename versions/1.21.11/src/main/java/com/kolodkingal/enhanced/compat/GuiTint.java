package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * 1.21.11 only: 1.21.1's {@code RenderSystem.setShaderColor} (gone from 1.21.6). The colour is kept here and given to
 * the textured blits drawn after it (the GUI's blits take a colour), and to {@link Gl}'s draws.
 */
public final class GuiTint {
    private static int color = -1;

    private GuiTint() {
    }

    public static void set(float r, float g, float b, float a) {
        color = ARGB.colorFromFloat(a, r, g, b);
    }

    public static int color() {
        return color;
    }

    private static int with(int c) {
        return color == -1 ? c : ARGB.multiply(c, color);
    }

    public static void blit(GuiGraphics g, Identifier tex, int x, int y, float u, float v, int w, int h, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, tw, th, color);
    }

    public static void blit(GuiGraphics g, Identifier tex, int x, int y, float u, float v, int w, int h, int tw, int th, int c) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, tw, th, with(c));
    }

    public static void blit(GuiGraphics g, Identifier tex, int x, int y, float u, float v, int w, int h, int rw, int rh,
                            int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th, color);
    }

    public static void blit(GuiGraphics g, Identifier tex, int x, int y, float u, float v, int w, int h, int rw, int rh,
                            int tw, int th, int c) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th, with(c));
    }
}
