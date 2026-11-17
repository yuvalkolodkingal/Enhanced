package com.kolodkingal.enhanced.client.magic;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicActionPayload;
import com.kolodkingal.enhanced.magic.Spell;
import com.kolodkingal.enhanced.platform.client.Keys;
import com.kolodkingal.enhanced.platform.net.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Spell Wheel (3.17): Sneak + K opens the caster's spells as rings of glowing seals round the crosshair (twelve on the
 * first ring, more on each ring out), each in its spell's colour with its first letter; the one the mouse points at
 * swells, and its name, mana, incantation and what it does are written in the middle. Letting go of K (or a click)
 * chooses it for K ({@code MagicActionPayload.SELECT_SPELL}). The game goes on behind it (it does not pause).
 */
public class SpellWheelScreen extends Screen {
    private static final int[] RING_SIZES = {12, 18, 24, 30};
    private final List<Spell> spells;
    @Nullable
    private final Spell selected;
    @Nullable
    private Spell hovered;
    private boolean chosen;
    private float clock;

    public SpellWheelScreen(List<Spell> spells, @Nullable Spell selected) {
        super(Component.translatable("screen.enhanced.spell_wheel"));
        this.spells = spells;
        this.selected = selected;
        this.hovered = selected;
    }

    /** Opens the wheel of the local player's spells. */
    public static void open(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        List<Spell> known = Magic.known(mc.player);
        if (known.isEmpty()) {
            mc.player.displayClientMessage(Component.translatable("message.enhanced.spell.none_known").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        mc.setScreen(new SpellWheelScreen(known, Spell.byId(Magic.data(mc.player).selected())));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        clock++;
        Minecraft mc = Minecraft.getInstance();
        InputConstants.Key key = Keys.boundKey(KeyBindings.SPELLS);
        if (clock > 4 && key.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue())) {
            choose();
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.SPELLS.matches(keyCode, scanCode)) {
            choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Spell at = at(mouseX, mouseY);
        if (at != null) {
            hovered = at;
        }
        choose();
        return true;
    }

    private void choose() {
        if (chosen) {
            return;
        }
        chosen = true;
        if (hovered != null && hovered != selected) {
            Net.sendToServer(new MagicActionPayload(MagicActionPayload.SELECT_SPELL, hovered.id(), "", 0));
        }
        onClose();
    }

    /** The radius of ring {@code ring}, fitted to the window. */
    private float radius(int ring) {
        float max = Math.min(width, height) / 2.0F - 18;
        float step = Math.min(30, max / (RING_SIZES.length + 0.6F));
        return step * (ring + 1.6F);
    }

    /** Where seal {@code index} sits, and its ring. */
    private float[] place(int index) {
        int ring = 0;
        int start = 0;
        while (ring < RING_SIZES.length - 1 && index >= start + RING_SIZES[ring]) {
            start += RING_SIZES[ring];
            ring++;
        }
        int inRing = Math.min(RING_SIZES[ring], spells.size() - start);
        double angle = (index - start) * Math.PI * 2 / Math.max(1, inRing) - Math.PI / 2 + ring * 0.13;
        float r = radius(ring);
        return new float[]{width / 2.0F + (float) Math.cos(angle) * r, height / 2.0F + (float) Math.sin(angle) * r, ring};
    }

    @Nullable
    private Spell at(double mouseX, double mouseY) {
        Spell best = null;
        double bestD = 14 * 14;
        for (int i = 0; i < spells.size(); i++) {
            float[] p = place(i);
            double d = (mouseX - p[0]) * (mouseX - p[0]) + (mouseY - p[1]) * (mouseY - p[1]);
            if (d < bestD) {
                bestD = d;
                best = spells.get(i);
            }
        }
        return best;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Spell pointed = at(mouseX, mouseY);
        if (pointed != null) {
            hovered = pointed;
        }
        Minecraft mc = Minecraft.getInstance();
        int cx = width / 2;
        int cy = height / 2;
        int rings = 1;
        for (int i = 0, n = 0; i < RING_SIZES.length && n < spells.size(); i++) {
            n += RING_SIZES[i];
            rings = i + 1;
        }
        disc(graphics, cx, cy, (int) radius(rings - 1) + 16, 0x70000000);
        disc(graphics, cx, cy, 60, 0x90000000);
        for (int i = 0; i < spells.size(); i++) {
            Spell spell = spells.get(i);
            float[] p = place(i);
            int x = Math.round(p[0]);
            int y = Math.round(p[1]);
            int color = mc.player != null ? Magic.color(mc.player, spell) : spell.color;
            boolean lit = spell == hovered;
            int r = lit ? 11 : 8;
            disc(graphics, x, y, r + 1, 0xFF000000 | darker(color));
            disc(graphics, x, y, r, 0xFF000000 | color);
            disc(graphics, x, y, r - 3, 0xE0101018);
            if (spell == selected) {
                graphics.renderOutline(x - r - 2, y - r - 2, 2 * r + 4, 2 * r + 4, 0xFFFFE07A);
            }
            String letter = spell.displayName().getString().substring(0, 1);
            graphics.drawCenteredString(font, letter, x, y - 4, 0xFF000000 | (lit ? 0xFFFFFF : color));
        }
        if (hovered != null) {
            int color = mc.player != null ? Magic.color(mc.player, hovered) : hovered.color;
            graphics.drawCenteredString(font, hovered.displayName().copy().withStyle(ChatFormatting.BOLD), cx, cy - 26, 0xFF000000 | color);
            graphics.drawCenteredString(font, Component.translatable("screen.enhanced.spell_wheel.cost", hovered.mana,
                    hovered.chant > 0 ? Component.translatable("screen.enhanced.spell_wheel.chant", Math.round(hovered.chant / 20.0F * 10) / 10.0F)
                            : Component.translatable("screen.enhanced.spell_wheel.instant")), cx, cy - 14, 0xFFB8C8E0);
            List<FormattedCharSequence> lines = font.split(hovered.description(), 110);
            for (int k = 0; k < Math.min(4, lines.size()); k++) {
                FormattedCharSequence line = lines.get(k);
                graphics.drawString(font, line, cx - font.width(line) / 2, cy - 2 + k * 10, 0xFFE0E6F0, true);
            }
        }
        graphics.drawCenteredString(font, Component.translatable("screen.enhanced.spell_wheel.hint").withStyle(ChatFormatting.GRAY),
                cx, height - 14, 0xFFFFFFFF);
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 0xFF) / 2;
        int g = (rgb >> 8 & 0xFF) / 2;
        int b = (rgb & 0xFF) / 2;
        return r << 16 | g << 8 | b;
    }

    /** A filled disc of square cells (cheap, and enough at this size). */
    private static void disc(GuiGraphics graphics, int cx, int cy, int radius, int argb) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(Math.max(0, radius * radius - dy * dy));
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, argb);
        }
    }
}
