package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The nanotech suits' two lines over Iron Man's suit panel (3.8, lane L4; PLAN 5.9's "drones' time" and the
 * Refocuser's charge "a crackle on the HUD"): the drones' seconds while they fly with him, and "Refocuser charged!"
 * flickering while lightning's charge is in the suit. Lane L3's {@code IronHud} draws the panel below them.
 */
public final class NanoHud implements LayeredDraw.Layer {
    static final int DRONES = 0xFF8A5A;
    static final int CHARGED = 0x9FE8FF;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        Power power = Powers.power(player);
        if (power != Power.IRON_MAN && power != Power.CUSTOM) {
            return;
        }
        IronManState state = IronSuits.state(player);
        long now = mc.level.getGameTime();
        int x = 4;
        int y = g.guiHeight() - 58;
        if (state.has(IronManState.DRONES) && state.dronesUntil() > now) {
            long seconds = (state.dronesUntil() - now + 19) / 20;
            g.drawString(mc.font, Component.translatable("hud.enhanced.nanotech.drones", seconds), x, y, DRONES, true);
            y -= 10;
        }
        if (state.has(IronManState.CHARGED)) {
            float pt = delta.getGameTimeDeltaPartialTick(false);
            int alpha = (int) (170 + 85 * Math.sin((now + pt) * 1.3));
            g.drawString(mc.font, Component.translatable("hud.enhanced.nanotech.charged"), x, y,
                    (alpha << 24) | CHARGED, true);
        }
    }
}
