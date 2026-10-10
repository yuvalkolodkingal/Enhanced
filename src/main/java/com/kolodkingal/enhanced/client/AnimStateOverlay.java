package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.client.render.Stance;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

import java.util.Locale;

/** The numbers behind the local hero's pose (toggle with {@code /enhanced_anim state}): a debugging aid. */
public class AnimStateOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!HeroAnimator.debugOverlay || mc.player == null || mc.options.hideGui) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(mc.player);
        if (s == null) {
            graphics.drawString(mc.font, "no animation state", 4, 4, 0xFFFFFF);
            return;
        }
        // Below the powers HUD in the corner.
        int y = 140;
        String[] lines = {
                String.format(Locale.ROOT, "s %.2f  sSide %.2f  sH %.2f  u %.2f  turn %.2f", s.s, s.sSide, s.sH, s.u, s.turn),
                String.format(Locale.ROOT, "pitch %.1f (+%.1f)  roll %.1f  yaw %.1f  bob %.3f  drop %.3f", s.pitch.x, s.addPitch, s.roll, s.yawAdd, s.bob, s.drop.x),
                String.format(Locale.ROOT, "wAir %.2f  wRun %.2f  wSprint %.2f  wSpin %.2f  lead %.2f  spin %.0f", s.wAir, s.wRun, s.wSprint, s.wSpin, s.leadForce, s.spinAngle),
                String.format(Locale.ROOT, "run phase %.2f amp %.2f omega %.2f  speed %.3f  limb %.2f/%.2f", s.runPhase, s.runAmp, s.runOmega, s.speed, s.limbSwing, s.limbAmount),
        };
        for (String line : lines) {
            graphics.drawString(mc.font, line, 4, y, 0xFFFF55, true);
            y += 10;
        }
        StringBuilder stances = new StringBuilder("stances:");
        for (Stance stance : Stance.VALUES) {
            float w = s.stance(stance);
            if (w > 0.01F) {
                stances.append(String.format(Locale.ROOT, " %s %.2f", stance, w));
            }
        }
        graphics.drawString(mc.font, stances.toString(), 4, y, 0x88FF88, true);
        y += 10;
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active) {
                graphics.drawString(mc.font, String.format(Locale.ROOT, "%s t%.1f/%d w%.2f", slot.id, slot.t, slot.id.dur, slot.weight), 4, y, 0x88CCFF, true);
                y += 10;
            }
        }
    }
}
