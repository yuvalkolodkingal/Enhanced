package com.kolodkingal.enhanced.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The abilities as a wheel around the player's own face, open while the wheel key is held: every ability of the
 * panel (the ones on Sneak too), and above the wheel the name, level, key and state of the one the mouse points at.
 * It only shows; nothing is cast from it. Like {@link PowerPanels} it knows neither mod.
 */
public final class PowerWheelScreen extends Screen {
    private record Entry(PowerPanels.Row row, PowerPanels.Slot slot, boolean sneak) {
    }

    private final Supplier<PowerPanels.Model> model;
    private final InputConstants.Key holdKey;
    private final Component sneakWord;
    private final Component readyWord;
    private final long openedAt = Util.getMillis();
    private int selected = -1;

    /**
     * @param model     the panel's model, asked every frame
     * @param holdKey   the key that keeps the wheel open (it closes when the key is let go)
     * @param sneakWord how "Sneak +" is written before a key
     * @param readyWord "Ready"
     */
    public PowerWheelScreen(Supplier<PowerPanels.Model> model, InputConstants.Key holdKey, Component sneakWord,
                            Component readyWord) {
        super(Component.empty());
        this.model = model;
        this.holdKey = holdKey;
        this.sneakWord = sneakWord;
        this.readyWord = readyWord;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // no blur and no dirt: the world stays in view behind the wheel
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        boolean held = holdKey.getType() == InputConstants.Type.MOUSE
                ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, holdKey.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                : holdKey.getValue() > 0 && InputConstants.isKeyDown(window, holdKey.getValue());
        if (!held || mc.player == null) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PowerPanels.Model m = model.get();
        if (m == null) {
            return;
        }
        List<Entry> entries = new ArrayList<>();
        for (PowerPanels.Row row : m.rows) {
            if (row.plain() != null) {
                entries.add(new Entry(row, row.plain(), false));
            }
        }
        for (PowerPanels.Row row : m.rows) {
            if (row.sneak() != null) {
                entries.add(new Entry(row, row.sneak(), true));
            }
        }
        int n = entries.size();
        if (n == 0) {
            return;
        }
        long now = Util.getMillis();
        float open = Mth.clamp((now - openedAt) / 180.0F, 0.0F, 1.0F);
        open = 1.0F - (1.0F - open) * (1.0F - open);
        int cx = width / 2;
        int cy = height / 2 + 14;
        int radius = Math.round((n > 6 ? 78 : 60) * open);
        int size = 22;
        g.fill(0, 0, width, height, (Math.round(0x50 * open) << 24));

        // which slot the mouse points at: the nearest by angle, once the mouse has left the middle
        double mx = mouseX - cx;
        double my = mouseY - cy;
        if (mx * mx + my * my > 14 * 14) {
            double angle = Math.atan2(my, mx) + Math.PI / 2;
            selected = Math.floorMod((int) Math.round(angle / (Math.PI * 2) * n), n);
        }
        int accent = 0xFF000000 | m.color;
        // the spokes between the slots, and the slice of the one pointed at
        for (int i = 0; i < n; i++) {
            double a = (i + 0.5) / n * Math.PI * 2 - Math.PI / 2;
            line(g, cx, cy, a, 16, radius + 16, 0xA0F3E6C4);
        }
        if (selected >= 0 && selected < n) {
            double a0 = (selected - 0.5) / n * Math.PI * 2 - Math.PI / 2;
            double a1 = (selected + 0.5) / n * Math.PI * 2 - Math.PI / 2;
            for (double a = a0; a <= a1; a += 0.02) {
                line(g, cx, cy, a, 18, radius + 14, (0x38 << 24) | (m.color & 0xFFFFFF));
            }
        }
        for (int i = 0; i < n; i++) {
            Entry e = entries.get(i);
            double a = (double) i / n * Math.PI * 2 - Math.PI / 2;
            int s = i == selected ? size + 4 : size;
            int x = cx + (int) Math.round(Math.cos(a) * radius) - s / 2;
            int y = cy + (int) Math.round(Math.sin(a) * radius) - s / 2;
            tile(g, m, e, x, y, s, i == selected);
        }
        // the player's own face in the middle
        Minecraft mc = Minecraft.getInstance();
        int face = 24;
        g.fill(cx - face / 2 - 2, cy - face / 2 - 2, cx + face / 2 + 2, cy + face / 2 + 2, 0xFFF3E6C4);
        if (mc.player != null) {
            PlayerFaceRenderer.draw(g, mc.player.getSkin(), cx - face / 2, cy - face / 2, face);
        } else {
            g.fill(cx - face / 2, cy - face / 2, cx + face / 2, cy + face / 2, accent);
        }
        // the details of the one pointed at
        Entry e = selected >= 0 && selected < n ? entries.get(selected) : null;
        int top = Math.max(6, cy - radius - 58);
        if (e == null) {
            g.drawCenteredString(font, m.title, cx, top + 12, 0xFFFFFFFF);
            return;
        }
        Component name = e.slot.name().copy().withStyle(style -> style.withUnderlined(true));
        g.drawCenteredString(font, name, cx, top, 0xFFFFFFFF);
        Component key = e.sneak ? sneakWord.copy().append(" + [").append(e.row.key()).append("]")
                : Component.literal("[").append(e.row.key()).append("]");
        g.drawCenteredString(font, key, cx, top + 12, 0xFFFFCF5A);
        int y = top + 23;
        if (e.slot.level() >= 0) {
            int w = e.slot.maxLevel() * 7;
            for (int s = 0; s < e.slot.maxLevel(); s++) {
                g.fill(cx - w / 2 + s * 7, y + 1, cx - w / 2 + s * 7 + 5, y + 6, s < e.slot.level() ? 0xFFFFC43A : 0x60FFC43A);
            }
            y += 10;
        }
        Component status = status(e.slot);
        g.drawCenteredString(font, status, cx, y, e.slot.cooldownLeft() > 0.0F ? 0xFFB8C0D0 : 0xFF6FE08A);
    }

    private Component status(PowerPanels.Slot slot) {
        return slot.status() != null ? slot.status() : readyWord;
    }

    private void tile(GuiGraphics g, PowerPanels.Model m, Entry e, int x, int y, int s, boolean chosen) {
        g.fill(x + 1, y + 1, x + s + 1, y + s + 1, 0x80000000);
        g.fill(x, y, x + s, y + s, 0xFF0B0D12);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, 0xFF1D2330);
        g.renderOutline(x + 1, y + 1, s - 2, s - 2, chosen ? 0xFFFFCF5A : e.slot.on() ? 0xFF6FE08A : (0xC0 << 24) | (m.color & 0xFFFFFF));
        @Nullable PowerPanels.Icon icon = e.slot.icon();
        if (icon != null) {
            icon.draw(g, x + 3, y + 3, s - 6);
        } else {
            String letter = e.slot.name().getString();
            letter = letter.isEmpty() ? "?" : letter.substring(0, letter.offsetByCodePoints(0, 1));
            g.drawString(font, letter, x + (s - font.width(letter)) / 2 + 1, y + (s - 8) / 2, 0xFFFFFFFF, true);
        }
        if (e.slot.cooldownLeft() > 0.0F) {
            g.fill(x + 2, y + 2, x + s - 2, y + s - 2, 0x90000000);
            String left = Integer.toString(Mth.ceil(e.slot.cooldownLeft()));
            g.drawString(font, left, x + (s - font.width(left)) / 2 + 1, y + (s - 8) / 2, 0xFFFFFFFF, true);
        }
        if (e.sneak) {
            g.fill(x + s - 5, y + 2, x + s - 2, y + 5, 0xFFFFCF5A);
        }
    }

    /** A thin line from the middle outwards, drawn as small squares (the HUD has no line call). */
    private static void line(GuiGraphics g, int cx, int cy, double angle, int from, int to, int color) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (int r = from; r < to; r += 1) {
            int x = cx + (int) Math.round(cos * r);
            int y = cy + (int) Math.round(sin * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }
}
