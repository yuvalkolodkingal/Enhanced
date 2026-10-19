package com.kolodkingal.enhanced.client.healing;

import com.kolodkingal.enhanced.client.BossBarOverride;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroKeys;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.healing.Downed;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.healing.Regeneration;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * The healing factor's HUD (3.7, the plan's 2.7), for a healer (Wolverine, Deadpool, a custom hero with the healing
 * factor): under his powers' panel a line with the healing factor's icon (a green "+" in a ring) and "Healing factor: ON
 * (+3.0/s)", or, poisoned, the icon red and crossed out and "Healing factor OFF — poisoned!"; and while he is DOWN the
 * edges of the screen darken (breathing slowly) and a ring in the middle fills as "Healing… 3" counts down to his rise.
 * Other lanes' panels under the powers' panel start at {@link #bottom}.
 */
public final class HealingHud implements LayeredDraw.Layer {
    /** The height of the healing line's box. */
    public static final int HEIGHT = 14;
    private static final int GREEN = 0xFF63E07A;
    private static final int RED = 0xFFE04848;
    /** The icon, 9 x 9: the ring ({@code o}) and the plus ({@code +}). */
    private static final String[] ICON = {
            "..ooooo..",
            ".o.....o.",
            "o...+...o",
            "o...+...o",
            "o.+++++.o",
            "o...+...o",
            "o...+...o",
            ".o.....o.",
            "..ooooo.."
    };

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || player.isDeadOrDying()
                || !HealingFactor.has(player)) {
            return;
        }
        long now = mc.level.getGameTime();
        float pt = delta.getGameTimeDeltaPartialTick(false);
        boolean down = HealingFactor.isDown(player);
        if (down) {
            drawDown(g, mc, player, now, pt);
        }
        if (mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        PowerData data = Powers.get(player);
        boolean off = HealingFactor.suppressed(player);
        Component line;
        int color;
        if (down) {
            line = Component.translatable("hud.enhanced.healing.down", secondsLeft(player, now));
            color = 0xFFE8FFEE;
        } else if (off) {
            line = Component.translatable("hud.enhanced.healing.off");
            float pulse = 0.5F + 0.5F * Mth.sin((now + pt) * 0.35F);
            color = lerp(0xFFB03030, 0xFFFF6060, pulse);
        } else {
            double rate = Regeneration.rate(player, now);
            line = Component.translatable("hud.enhanced.healing.on", String.format(Locale.ROOT, "%.1f", rate));
            color = 0xFFFFFFFF;
        }
        Font font = mc.font;
        int x = 4;
        int y = top(data);
        int width = 12 + font.width(line) + 2;
        g.fill(x - 2, y - 2, x + width + 2, y + HEIGHT - 2, 0x70000000);
        icon(g, x, y, off ? RED : GREEN, off);
        g.drawString(font, line, x + 12, y + 1, color, true);
    }

    /** Where the healing line begins: under the powers' panel (the custom hero's is as long as his lines). */
    static int top(PowerData data) {
        int lines;
        if (data.power() == Power.CUSTOM) {
            HeroKit kit = data.kit();
            lines = CustomHero.KEYS + 1; // (his keys' lines, and the line of what needs no key: his body's healing)
            if (kit != null) {
                for (int column = 1; column <= CustomHero.KEYS; column++) {
                    lines += kit.sneak(column) != null ? 1 : 0;
                }
            }
        } else {
            lines = PowerData.ABILITIES + (HeroKeys.yRow(data) != null ? 1 : 0);
        }
        return 4 + BossBarOverride.clearance(4 + 180) + 12 + lines * 10 + 6;
    }

    /** The first free line under the healing line (for the panels of other lanes under the powers' panel). */
    public static int bottom(PowerData data) {
        return top(data) + HEIGHT + 2;
    }

    /** The healing factor's icon at ({@code x}, {@code y}); {@code off}: crossed out. */
    static void icon(GuiGraphics g, int x, int y, int color, boolean off) {
        for (int r = 0; r < ICON.length; r++) {
            for (int c = 0; c < ICON[r].length(); c++) {
                char ch = ICON[r].charAt(c);
                if (ch == 'o') {
                    g.fill(x + c, y + r, x + c + 1, y + r + 1, color);
                } else if (ch == '+') {
                    g.fill(x + c, y + r, x + c + 1, y + r + 1, off ? 0xFF803030 : 0xFFFFFFFF);
                }
            }
        }
        if (off) {
            for (int i = 0; i < 9; i++) {
                g.fill(x + 8 - i, y + i, x + 9 - i, y + i + 1, 0xFFFF7070);
            }
        }
    }

    /** The seconds until he rises (at least 1 while he is DOWN). */
    static int secondsLeft(LocalPlayer player, long now) {
        long until = player.getData(ModAttachments.HEALING).downUntil();
        return (int) Math.max(1, (until - now + 19) / 20);
    }

    /** DOWN: the edges darken (a slow breath), a ring fills in the middle with "Healing… 3" under it. */
    private static void drawDown(GuiGraphics g, Minecraft mc, LocalPlayer player, long now, float pt) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        float breath = 0.5F + 0.5F * Mth.sin((now + pt) * 0.12F);
        int alpha = (int) (150 + 50 * breath);
        int edge = Math.max(24, Math.min(w, h) / 4);
        int dark = alpha << 24 | 0x06140A;
        g.fillGradient(0, 0, w, edge, dark, 0x00000000);
        g.fillGradient(0, h - edge, w, h, 0x00000000, dark);
        int strips = 16;
        for (int i = 0; i < strips; i++) {
            int a = (int) (alpha * (1.0F - (i + 0.5F) / strips));
            int color = a << 24 | 0x06140A;
            int sw = Math.max(1, edge / strips);
            g.fill(i * sw, 0, (i + 1) * sw, h, color);
            g.fill(w - (i + 1) * sw, 0, w - i * sw, h, color);
        }
        long until = player.getData(ModAttachments.HEALING).downUntil();
        float total = Math.max(1, Downed.downTicks());
        float done = Mth.clamp(1.0F - (until - now - pt) / total, 0.0F, 1.0F);
        int cx = w / 2;
        int cy = h / 2 + 34;
        int radius = 12;
        for (int i = 0; i < 48; i++) {
            float a = Mth.TWO_PI * i / 48 - Mth.HALF_PI;
            int px = cx + Math.round(radius * Mth.cos(a));
            int py = cy + Math.round(radius * Mth.sin(a));
            boolean lit = i < Math.round(done * 48);
            g.fill(px - 1, py - 1, px + 1, py + 1, lit ? GREEN : 0x80304030);
        }
        icon(g, cx - 4, cy - 4, GREEN, false);
        Component text = Component.translatable("hud.enhanced.healing.down", secondsLeft(player, now));
        g.drawCenteredString(mc.font, text, cx, cy + radius + 6, 0xFFE8FFEE);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gg = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return 0xFF000000 | r << 16 | gg << 8 | bl;
    }
}
