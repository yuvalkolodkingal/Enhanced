package com.kolodkingal.enhanced.client.wolverine;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.wolverine.Berserker;
import com.kolodkingal.enhanced.wolverine.Claws;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What Wolverine sees over the world (3.7): the red edge of the Berserker Rage on his own screen (it pulses with his
 * heaving breath; still, without the pulse, with {@code reduceMotion}), and the silver flash of the Adamantium
 * Injection. And his HUD lines ({@link #status}, for {@code WolverineClient.hudStatus}): the combo's step, the claws
 * out or in and the climb, the Rage's seconds as a red bar, Track's way and distance.
 */
public final class WolverineHud implements LayeredDraw.Layer {
    /** How long the injection's flash lasts (ticks). */
    static final int FLASH_TICKS = 16;
    private static long flashAt = Long.MIN_VALUE;
    /** The arrows of Track's way, by eighths of a turn from straight ahead, clockwise. */
    private static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190",
            "\u2196"};
    /** How long after a slash its step stays on the HUD (the combo's reset). */
    static final int COMBO_SHOWN = 30;
    /** The local player's last slash: its step (0-2) and the game time. */
    private static int comboStep = -1;
    private static long comboAt = Long.MIN_VALUE / 2;

    /** The status after an ability's line on his HUD. */
    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        switch (ability) {
            case 1 -> {
                if (comboStep >= 0 && now - comboAt <= COMBO_SHOWN && now >= comboAt) {
                    return Component.translatable("hud.enhanced.wolverine.combo", comboStep + 1)
                            .withStyle(ChatFormatting.GOLD);
                }
            }
            case 2 -> {
                if (Claws.out(data)) {
                    Component line = Component.translatable("hud.enhanced.wolverine.claws_out")
                            .withStyle(ChatFormatting.WHITE);
                    return data.toggle(Toggle.CLAW_CLIMB) ? line.copy().append(Component.translatable(
                            "hud.enhanced.wolverine.climbing").withStyle(ChatFormatting.AQUA)) : line;
                }
                return Component.translatable("hud.enhanced.wolverine.claws_in").withStyle(ChatFormatting.DARK_GRAY);
            }
            case 4 -> {
                long left = Berserker.left(player);
                if (left > 0) {
                    return Component.translatable("hud.enhanced.wolverine.rage", bar(left), (left + 19) / 20)
                            .withStyle(ChatFormatting.RED);
                }
            }
            case 5 -> {
                if (data.toggle(Toggle.SCENT_TRACK)) {
                    Vec3 at = ScentView.trackedAt();
                    if (at == null) {
                        return Component.translatable("hud.enhanced.wolverine.track_faint")
                                .withStyle(ChatFormatting.GOLD);
                    }
                    return Component.translatable("hud.enhanced.wolverine.track", arrow(player, at),
                            ScentView.trackedDistance(player)).withStyle(ChatFormatting.GOLD);
                }
            }
            default -> {
            }
        }
        return PowerHud.cooldown(data, ability, now);
    }

    /** The Rage's bar: ten marks, a mark a second, the ones still left lit. */
    static String bar(long left) {
        int lit = (int) Math.ceil(left / 20.0);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            b.append(i < lit ? '\u258c' : '\u2591');
        }
        return b.toString();
    }

    /** The arrow from his look toward {@code at}. */
    static String arrow(LocalPlayer player, Vec3 at) {
        double dx = at.x - player.getX();
        double dz = at.z - player.getZ();
        float toward = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float rel = Mth.wrapDegrees(toward - player.getYRot());
        return ARROWS[Math.floorMod(Math.round(rel / 45.0F), 8)];
    }

    /** A payload of the local player's own slash: its step for the HUD. */
    public static void onOwnPayload(WolverineFxPayload payload, long now) {
        if (payload.kind() == WolverineFxPayload.SLASH) {
            comboStep = payload.arg();
            comboAt = now;
        }
    }

    public static void clear() {
        comboStep = -1;
        flashAt = Long.MIN_VALUE;
    }

    /** The injection's silver flash on this screen. */
    public static void flash() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            flashAt = mc.level.getGameTime();
        }
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        float pt = delta.getGameTimeDeltaPartialTick(false);
        float since = mc.level.getGameTime() - flashAt + pt;
        if (since >= 0.0F && since < FLASH_TICKS) {
            float k = 1.0F - since / FLASH_TICKS;
            int a = (int) (k * k * 200);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0xE4EAF0);
        }
        if (mc.options.hideGui || !Berserker.raging(player)) {
            return;
        }
        float pulse = EnhancedClientConfig.reducedMotion() ? 0.7F
                : 0.6F + 0.4F * (0.5F + 0.5F * Mth.sin((mc.level.getGameTime() + pt) * 0.35F));
        long left = Berserker.left(player);
        if (left < 20) {
            pulse *= left / 20.0F;
        }
        edge(g, pulse);
    }

    /** A red edge round the screen, fading inward. */
    private static void edge(GuiGraphics g, float strength) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int depth = Math.max(12, Math.min(w, h) / 9);
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float k = 1.0F - i / (float) steps;
            int a = (int) (k * k * 110 * strength);
            int color = (a << 24) | 0xB01010;
            int d0 = depth * i / steps;
            int d1 = depth * (i + 1) / steps;
            g.fill(0, d0, w, d1, color);
            g.fill(0, h - d1, w, h - d0, color);
            g.fill(d0, d1, d1, h - d1, color);
            g.fill(w - d1, d1, w - d0, h - d1, color);
        }
    }
}
