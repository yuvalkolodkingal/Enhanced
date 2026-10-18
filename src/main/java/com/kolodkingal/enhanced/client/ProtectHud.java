package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.HexRunes;
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
 * What the walls of 3.3 show the one they concern: while Billy holds his Hex Barrier, a bar of its health under the crosshair
 * (blue, then yellow below half and red below a quarter; it fills as the barrier forms, slides down as it takes hits and flashes
 * white with each); and, to a sorcerer the Hex Runes hold, a red line above the action bar that says so. Nothing with the GUI hidden.
 */
public class ProtectHud implements LayeredDraw.Layer {
    private static final int WIDTH = 82;
    private static final int HEIGHT = 5;

    /** The fraction the bar shows (it eases toward the real one), the real one last frame, and when the last hit was seen. */
    private float shown = -1.0F;
    private float last = -1.0F;
    private float hitAt = -100.0F;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        float time = player.tickCount + partialTick;
        barrier(graphics, mc, player, time);
        if (HexRunes.silences(player)) {
            Component line = Component.translatable("hud.enhanced.hex_runes.silenced").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            Font font = mc.font;
            // Above the action bar (at the top it ran under the powers HUD's title line).
            int x = (graphics.guiWidth() - font.width(line)) / 2;
            int y = graphics.guiHeight() - 96;
            graphics.fill(x - 4, y - 3, x + font.width(line) + 4, y + 10, 0x90000000);
            graphics.drawString(font, line, x, y, 0xFFFFFF);
        }
    }

    private void barrier(GuiGraphics graphics, Minecraft mc, LocalPlayer player, float time) {
        PowerData data = Powers.get(player);
        if (data.power() != Power.BILLY || !data.chaosShieldActive() || !BillyBarrier.enabled()) {
            shown = -1.0F;
            last = -1.0F;
            return;
        }
        float max = BillyBarrier.maxHealth(player);
        boolean formed = data.chaosShieldUp(player.level().getGameTime());
        float real = formed ? Mth.clamp(data.shieldStrength() / Math.max(1.0F, max), 0.0F, 1.0F)
                : Mth.clamp(data.chaosShieldProgress(time), 0.0F, 1.0F) * Mth.clamp(data.shieldStrength() / Math.max(1.0F, max), 0.0F, 1.0F);
        if (last >= 0.0F && formed && real < last - 1.0E-3F) {
            hitAt = time;
        }
        last = real;
        // Ease toward the real health (no jump), quickly enough to read a hit at once.
        shown = shown < 0.0F ? real : shown + (real - shown) * 0.35F;
        int x = (graphics.guiWidth() - WIDTH) / 2;
        int y = graphics.guiHeight() / 2 + 14;
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xA0000000);
        int color = shown > 0.5F ? 0x5CC8FF : shown > 0.25F ? 0xF2D14B : 0xE0413F;
        float flash = Mth.clamp(1.0F - (time - hitAt) / 6.0F, 0.0F, 1.0F);
        int filled = Math.round(WIDTH * shown);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + HEIGHT, 0xFF000000 | mix(color, 0xFFFFFF, flash * 0.8F));
            graphics.fill(x, y, x + filled, y + 1, 0x60FFFFFF);
        }
        String numbers = (int) Math.ceil(Math.min(data.shieldStrength(), max)) + "/" + (int) Math.ceil(max);
        Font font = mc.font;
        graphics.drawString(font, numbers, x + WIDTH + 4, y - 2, formed ? 0xCFEFFF : 0xFFAA00);
    }

    private static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }
}
