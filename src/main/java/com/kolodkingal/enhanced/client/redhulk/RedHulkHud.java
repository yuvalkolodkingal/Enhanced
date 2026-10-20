package com.kolodkingal.enhanced.client.redhulk;

import com.kolodkingal.enhanced.client.BossBarOverride;
import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.client.render.RedHulkActing;
import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.redhulk.Anger;
import com.kolodkingal.enhanced.redhulk.RedHulkState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Red Hulk's HUD (3.8, PLAN 4.6), under the powers' panel: the <b>Anger bar</b> (red, then orange, then white-hot as it
 * fills; little flames over it from 75, a pulse from 90; the mark at {@code earlyBurstMin}, where B can let it out),
 * Ross's line ("Ross" and the key that makes him Red Hulk, his anger rising at half the rate), the Super Jump's
 * <b>charge ring</b> round the crosshair while he crouches, and the Energy Absorb's time left. And the burst's orange
 * flash over the whole screen.
 */
public final class RedHulkHud implements LayeredDraw.Layer {
    /** The panel's height: the powers' panel above it is 12 + 5 lines of 10. */
    private static final int PANEL = 12 + 5 * 10 + 6;
    private static final int BAR_W = 100;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        float flash = RedHulkEffects.flash();
        if (flash > 0.01F) {
            int a = (int) (flash * 160);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0xFF7A2A);
        }
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || player.isDeadOrDying()) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.RED_HULK) {
            return;
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        float pt = delta.getGameTimeDeltaPartialTick(false);
        float time = now + pt;
        RedHulkState state = Anger.get(player);
        float anger = state.anger();
        int x = 4;
        int y = 4 + BossBarOverride.clearance(x + 160) + PANEL;
        boolean ross = data.toggle(Toggle.ROSS_FORM);
        int lines = ross ? 2 : 1;
        boolean absorbing = data.is(Live.RED_HULK_ABSORBING);
        if (absorbing) {
            lines++;
        }
        g.fill(x - 2, y - 2, x + BAR_W + 62, y + lines * 11 + 1, 0x70000000);
        Component label = Component.translatable("hud.enhanced.red_hulk.anger", Math.round(anger));
        int labelColor = anger >= 90 ? 0xFFFFE6B0 : anger >= 75 ? 0xFFFFA040 : 0xFFFFFFFF;
        g.drawString(font, label, x, y, labelColor, true);
        int bx = x + 58;
        int by = y + 1;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + 7, 0xFF180808);
        int filled = Math.round(BAR_W * Mth.clamp(anger / Anger.MAX, 0.0F, 1.0F));
        for (int i = 0; i < filled; i++) {
            float t = i / (float) BAR_W;
            int c = barColor(t);
            if (anger >= 90.0F) {
                float pulse = 0.5F + 0.5F * Mth.sin(time * 0.6F);
                c = lerp(c, 0xFFFFF4D0, 0.35F * pulse);
            }
            g.fill(bx + i, by, bx + i + 1, by + 6, c);
        }
        if (anger >= 75.0F) {
            // Little flames licking up from the bar.
            for (int i = 2; i < filled; i += 7) {
                float f = Mth.sin(time * 0.5F + i * 1.7F);
                int h = 2 + Math.round(2.0F * (0.5F + 0.5F * f));
                g.fill(bx + i, by - h, bx + i + 2, by, f > 0.0F ? 0xC0FFC040 : 0xC0FF6A1A);
            }
        }
        int mark = bx + Math.round(BAR_W * (float) earlyMin() / Anger.MAX);
        g.fill(mark, by - 1, mark + 1, by + 7, 0xFFFFFFFF);
        g.fill(bx + BAR_W - 1, by - 1, bx + BAR_W, by + 7, 0xFFFFFFFF);
        int line = y + 11;
        if (ross) {
            Component hint = Component.translatable("hud.enhanced.red_hulk.ross_hint",
                    KeyBindings.ability(5).getTranslatedKeyMessage()).withStyle(ChatFormatting.RED);
            g.drawString(font, hint, x, line, 0xFFFFFFFF, true);
            line += 11;
        }
        if (absorbing) {
            long left = Math.max(0L, state.absorbUntil() - now);
            float share = Mth.clamp(left / (float) absorbMax(), 0.0F, 1.0F);
            g.drawString(font, Component.translatable("hud.enhanced.red_hulk.absorb"), x, line, 0xFFFF6A6A, true);
            g.fill(bx - 1, line, bx + BAR_W + 1, line + 6, 0xFF180808);
            g.fill(bx, line + 1, bx + Math.round(BAR_W * share), line + 5, 0xFFE0352A);
        }
        float charge = RedHulkActing.localCharge();
        if (charge >= 0.0F && data.is(Live.RED_HULK_JUMP_CHARGING)) {
            ring(g, g.guiWidth() / 2, g.guiHeight() / 2, charge);
        }
    }

    /** The Super Jump's charge: a ring of dots round the crosshair, filling clockwise, white when full. */
    private static void ring(GuiGraphics g, int cx, int cy, float charge) {
        int dots = 24;
        int lit = Math.round(dots * charge);
        for (int i = 0; i < dots; i++) {
            double a = -Math.PI / 2.0 + i * 2.0 * Math.PI / dots;
            int px = cx + (int) Math.round(Math.cos(a) * 14.0);
            int py = cy + (int) Math.round(Math.sin(a) * 14.0);
            int c = i < lit ? (charge >= 1.0F ? 0xFFFFF4D0 : barColor(i / (float) dots)) : 0x60FFFFFF;
            g.fill(px - 1, py - 1, px + 1, py + 1, c);
        }
    }

    /** Red at the bottom of the bar, orange in the middle, white-hot at the top. */
    static int barColor(float t) {
        if (t < 0.5F) {
            return lerp(0xFFB0180F, 0xFFFF6A1A, t * 2.0F);
        }
        return lerp(0xFFFF6A1A, 0xFFFFE6B0, (t - 0.5F) * 2.0F);
    }

    private static double earlyMin() {
        return RedHulkConfig.EARLY_BURST_MIN == null ? 50.0 : safe(() -> RedHulkConfig.EARLY_BURST_MIN.get(), 50.0);
    }

    private static int absorbMax() {
        return (int) safe(() -> (double) RedHulkConfig.ABSORB_MAX_TICKS.get(), 80.0);
    }

    /** A server option the client may not have (a client on a server: the defaults). */
    private static double safe(java.util.function.Supplier<Double> value, double fallback) {
        try {
            return value.get();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int lerp(int a, int b, float t) {
        int al = (int) Mth.lerp(t, (a >>> 24) & 0xFF, (b >>> 24) & 0xFF);
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gg = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return al << 24 | r << 16 | gg << 8 | bl;
    }
}
