package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * What Iron Man sees (3.8, PLAN 5.9, lane L3): a panel at the left of the screen, under its middle, while he is Iron
 * Man, with the suit's name, the energy bar (blue; amber under a quarter; red and blinking under a tenth), its
 * durability and the warnings ("No reactor!", "Suit offline!", the suit-up under way); unsuited, "Tony · no suit (H:
 * suit up)" and the suit H would put on. In first person a thin helmet frame (blue brackets at the screen's corners,
 * altitude and speed while flying; client option {@code ironHelmetHud}) and the faceplate closing over the edges at the
 * end of a suit-up ({@link HelmetOverlay}). His HUD lines ({@link #status}): each key's wait.
 */
public final class IronHud implements LayeredDraw.Layer {
    /** The frame's blue, and the bar's three colours. */
    static final int BLUE = 0x9FE8FF;
    static final int AMBER = 0xFFB338;
    static final int RED = 0xFF4040;
    static final int BAR_WIDTH = 80;
    /** Lines other lanes add under the panel (lane L4: the drones' seconds, the charged Refocuser, the locks). */
    private static final List<BiFunction<LocalPlayer, IronManState, Component>> LINES = new CopyOnWriteArrayList<>();

    /**
     * Adds a line under the suit's panel (from the client setup): asked every frame while he is suited; null shows
     * nothing.
     */
    public static void addLine(BiFunction<LocalPlayer, IronManState, Component> line) {
        LINES.add(line);
    }

    /** The status after an ability's line on his HUD: its wait, as everyone's. */
    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        return PowerHud.cooldown(data, ability, now);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        if (Powers.power(player) != Power.IRON_MAN) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.CUSTOM && data.acts(Power.IRON_MAN) && !mc.options.hideGui) {
                // (A custom hero with Iron Man's rows: his suit energy meter alone.)
                bar(g, mc, player, 4, g.guiHeight() / 2 + 16);
            }
            return;
        }
        float pt = delta.getGameTimeDeltaPartialTick(false);
        boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
        IronManState state = IronSuits.state(player);
        if (firstPerson) {
            HelmetOverlay.render(g, mc, player, state, pt);
        }
        if (mc.options.hideGui) {
            return;
        }
        SuitKind kind = state.phaseKind() != null ? state.phaseKind() : SuitKind.of(state.suit());
        if (firstPerson && kind != null && state.has(IronManState.POWERED) && EnhancedClientConfig.ironHelmetHud()) {
            frame(g, mc, player);
        }
        panel(g, mc, player, state, kind);
    }

    /** The panel: the suit, its energy and durability, the warnings; or Tony's line. */
    private static void panel(GuiGraphics g, Minecraft mc, LocalPlayer player, IronManState state,
                              @Nullable SuitKind kind) {
        Font font = mc.font;
        // (Left, just under the middle of the screen: clear of the hotbar, the hearts, the powers' panel above and the
        // effects' icons at the top right.)
        int x = 4;
        int y = g.guiHeight() / 2 + 16;
        if (kind == null) {
            g.drawString(font, Component.translatable("hud.enhanced.iron_man.unsuited"), x, y + 10, 0xC8C8C8, true);
            SuitKind next = nextSuit(player);
            if (next != null) {
                g.drawString(font, Component.translatable("hud.enhanced.iron_man.next",
                        Component.translatable(IronSuits.item(next).getDescriptionId())), x, y + 21, 0x8FD8FF, true);
            }
            return;
        }
        Component name = Component.translatable(IronSuits.item(kind).getDescriptionId())
                .withStyle(ChatFormatting.BOLD);
        g.drawString(font, name, x, y, 0xFFD24A, true);
        bar(g, mc, player, x, y + 11);
        // Durability, and what is wrong.
        ItemStack stack = IronSuits.durabilityStack(player);
        int line = y + 21;
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            int max = Math.max(1, stack.getMaxDamage() - 1);
            int left = Math.max(0, max - stack.getDamageValue());
            int percent = Math.round(100.0F * left / max);
            g.drawString(font, Component.translatable("hud.enhanced.iron_man.durability", percent), x, line,
                    percent < 20 ? RED : 0xC8C8C8, true);
            line += 10;
        }
        Component warning = warning(player, state, kind);
        if (warning != null) {
            g.drawString(font, warning, x, line, RED, true);
            line += 10;
        }
        for (BiFunction<LocalPlayer, IronManState, Component> extra : LINES) {
            Component text = extra.apply(player, state);
            if (text != null) {
                g.drawString(font, text, x, line, 0xC8C8C8, true);
                line += 10;
            }
        }
    }

    /** The energy bar at ({@code x}, {@code by}): blue; amber under a quarter; red and blinking under a tenth. */
    static void bar(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int by) {
        float frac = SuitEnergy.fraction(player);
        int color = frac < 0.25F ? (frac < 0.10F ? RED : AMBER) : BLUE;
        boolean blink = frac < 0.10F && (mc.level.getGameTime() / 6) % 2 == 0;
        g.fill(x, by, x + BAR_WIDTH + 2, by + 6, 0xA0000000);
        if (!blink) {
            g.fill(x + 1, by + 1, x + 1 + Math.round(BAR_WIDTH * frac), by + 5, 0xFF000000 | color);
        }
        g.drawString(mc.font, String.format(Locale.ROOT, "%d", Math.round(SuitEnergy.get(player))),
                x + BAR_WIDTH + 6, by - 1, 0xFF000000 | color, true);
    }

    /** The warning under the panel, or null. */
    @Nullable
    static Component warning(LocalPlayer player, IronManState state, SuitKind kind) {
        if (state.phase() == IronManState.UP) {
            return Component.translatable("hud.enhanced.iron_man.suiting").withStyle(ChatFormatting.AQUA);
        }
        if (state.phase() == IronManState.DOWN) {
            return Component.translatable("hud.enhanced.iron_man.taking_off").withStyle(ChatFormatting.AQUA);
        }
        if (!IronSuits.reactorFor(player, kind)) {
            return Component.translatable("message.enhanced.iron_man.no_reactor");
        }
        if (IronSuits.broken(player)) {
            return Component.translatable("message.enhanced.iron_man.offline");
        }
        if (SuitEnergy.empty(player)) {
            return Component.translatable("message.enhanced.iron_man.energy_empty");
        }
        return null;
    }

    /** The suit H would put on (the client's own copy of the rule: the last worn, the suitcase, Mark 3, Mark 1). */
    @Nullable
    static SuitKind nextSuit(LocalPlayer player) {
        if (IronSuits.nanoReactorOn(player)) {
            SuitKind selected = SuitKind.of(IronSuits.state(player).nanoSuit());
            return selected != null && IronSuits.carries(player, selected) ? selected : null;
        }
        SuitKind last = SuitKind.of(IronSuits.state(player).lastSuit());
        if (last != null && last.classic() && last != SuitKind.HULKBUSTER && IronSuits.carries(player, last)) {
            return last;
        }
        for (SuitKind kind : new SuitKind[]{SuitKind.MARK_5, SuitKind.MARK_3, SuitKind.MARK_1}) {
            if (IronSuits.carries(player, kind)) {
                return kind;
            }
        }
        return null;
    }

    /** The helmet frame: blue brackets at the corners, a soft line at the top, altitude and speed while flying. */
    private static void frame(GuiGraphics g, Minecraft mc, LocalPlayer player) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int c = 0x909FE8FF;
        int len = Math.max(18, Math.min(w, h) / 10);
        int m = 10;
        // The four corners.
        g.fill(m, m, m + len, m + 1, c);
        g.fill(m, m, m + 1, m + len, c);
        g.fill(w - m - len, m, w - m, m + 1, c);
        g.fill(w - m - 1, m, w - m, m + len, c);
        g.fill(m, h - m - 1, m + len, h - m, c);
        g.fill(m, h - m - len, m + 1, h - m, c);
        g.fill(w - m - len, h - m - 1, w - m, h - m, c);
        g.fill(w - m - 1, h - m - len, w - m, h - m, c);
        // A curved line along the top: the helmet's brow.
        for (int i = 0; i < 40; i++) {
            float t = i / 39.0F;
            int px = Math.round(Mth.lerp(t, w * 0.3F, w * 0.7F));
            int py = m + 4 + Math.round(6.0F * (float) Math.pow(2.0F * t - 1.0F, 2));
            g.fill(px, py, px + 3, py + 1, 0x609FE8FF);
        }
        if (player.getAbilities().flying) {
            Font font = mc.font;
            String alt = String.format(Locale.ROOT, "%d", Math.round(player.getY()));
            double speed = player.getDeltaMovement().length() * 20.0;
            String spd = String.format(Locale.ROOT, "%.1f", speed);
            g.drawString(font, Component.translatable("hud.enhanced.iron_man.altitude", alt), w - m - 70, h / 2 - 10,
                    0xFF000000 | BLUE, true);
            g.drawString(font, Component.translatable("hud.enhanced.iron_man.speed", spd), w - m - 70, h / 2 + 2,
                    0xFF000000 | BLUE, true);
        }
    }
}
