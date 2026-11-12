//#norenames
package com.kolodkingal.enhanced.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * 1.21.11 text drawing with the colour rule of 1.21.1.
 *
 * <p>On 1.21.1 {@code Font} treated a colour whose top six bits were zero as opaque, so the mod
 * passes plain {@code 0xRRGGBB} colours everywhere. Since 1.21.6 {@link GuiGraphics} skips text
 * whose alpha is zero, so every call goes through
 * {@link #opaque(int)} first.
 */
public final class GuiText {
    private GuiText() {
    }

    public static int opaque(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    public static void drawString(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, opaque(color));
    }

    public static void drawString(GuiGraphics g, Font font, String text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, opaque(color), shadow);
    }

    public static void drawString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.drawString(font, text, x, y, opaque(color));
    }

    public static void drawString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color,
            boolean shadow) {
        g.drawString(font, text, x, y, opaque(color), shadow);
    }

    public static void drawString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, opaque(color));
    }

    public static void drawString(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, opaque(color), shadow);
    }

    public static void drawCenteredString(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawCenteredString(font, text, x, y, opaque(color));
    }

    public static void drawCenteredString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawCenteredString(font, text, x, y, opaque(color));
    }

    public static void drawCenteredString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y,
            int color) {
        g.drawCenteredString(font, text, x, y, opaque(color));
    }

    public static void drawWordWrap(GuiGraphics g, Font font, FormattedText text, int x, int y, int width, int color) {
        g.drawWordWrap(font, text, x, y, width, opaque(color));
    }

    public static void drawWordWrap(GuiGraphics g, Font font, FormattedText text, int x, int y, int width, int color,
            boolean shadow) {
        g.drawWordWrap(font, text, x, y, width, opaque(color), shadow);
    }
}
