package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.network.RealmScreenPayload;
import com.kolodkingal.enhanced.network.StoneActionPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The Space Stone's Realm Picker (3.6): four realms to choose from (the Overworld, the Nether, the End, Asgard), drawn
 * as windows on a starfield, each a little picture made in code (no textures): green hills under a blue sky, a red
 * cavern over a lava sea, a pale island in the void, and the golden spires of Asgard over the rainbow bridge. A realm
 * the server does not allow now is dimmed, with the reason on it. Choosing one (a click, or the keys 1-4) sends {@code
 * StoneActionPayload(REALM, index)}; the server checks again. The moving parts (stars, the lava, the bridge) move with
 * the frame's time.
 */
public class RealmScreen extends Screen {
    /** The Space Stone's blue, for the frames. */
    private static final int BLUE = 0x2F9BFF;
    private static final int PALE = 0xA8DBFF;

    private final List<RealmScreenPayload.RealmOption> realms;
    private int hovered = -1;

    private RealmScreen(List<RealmScreenPayload.RealmOption> realms) {
        super(Component.translatable("screen.enhanced.realms.title"));
        this.realms = realms;
    }

    /** The server asks for the screen with the realms it allows now. */
    public static void open(RealmScreenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || payload.realms().isEmpty()) {
            return;
        }
        mc.setScreen(new RealmScreen(payload.realms()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- layout ----

    private int count() {
        return Math.min(4, realms.size());
    }

    /** Room above the windows for the title, under each window for its name, between the rows, and for the hint. */
    private static final int TITLE_SPACE = 24;
    private static final int LABEL_SPACE = 16;
    private static final int ROW_GAP = 6;
    private static final int HINT_SPACE = 16;

    /** The windows' width: as wide as the screen allows, but low enough that both rows, names and hint fit. */
    private int windowWidth() {
        int byHeight = (height - TITLE_SPACE - HINT_SPACE - 2 * LABEL_SPACE - ROW_GAP - 8) / 2 * 5 / 3;
        return Mth.clamp(Math.min((width - 48) / 2, byHeight), 60, 150);
    }

    private int windowHeight() {
        return windowWidth() * 3 / 5;
    }

    private int left(int index) {
        int w = windowWidth();
        int gap = 16;
        return (width - (2 * w + gap)) / 2 + (index % 2) * (w + gap);
    }

    private int top(int index) {
        int row = windowHeight() + LABEL_SPACE + ROW_GAP;
        int block = TITLE_SPACE + 2 * row - ROW_GAP + HINT_SPACE;
        return Math.max(0, (height - block) / 2) + TITLE_SPACE + (index / 2) * row;
    }

    private int at(double mouseX, double mouseY) {
        for (int i = 0; i < count(); i++) {
            int x = left(i);
            int y = top(i);
            if (mouseX >= x && mouseX < x + windowWidth() && mouseY >= y && mouseY < y + windowHeight() + 12) {
                return i;
            }
        }
        return -1;
    }

    // ---- drawing ----

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float time = time(partialTick);
        g.fillGradient(0, 0, width, height, 0xF0050818, 0xF0101438);
        // The starfield: stars that stay where they are and twinkle.
        for (int i = 0; i < 160; i++) {
            int x = (int) (hash(i * 3) * width);
            int y = (int) (hash(i * 3 + 1) * height);
            float twinkle = 0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(time * (0.05F + hash(i * 3 + 2) * 0.1F) + i));
            int size = hash(i * 7) > 0.92F ? 2 : 1;
            g.fill(x, y, x + size, y + size, argb(i % 9 == 0 ? PALE : 0xFFFFFF, twinkle));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float time = time(partialTick);
        hovered = at(mouseX, mouseY);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, Math.max(4, top(0) - 18),
                PALE);
        for (int i = 0; i < count(); i++) {
            RealmScreenPayload.RealmOption realm = realms.get(i);
            int x = left(i);
            int y = top(i);
            int w = windowWidth();
            int h = windowHeight();
            boolean hover = i == hovered && realm.allowed();
            int frame = hover ? 0xFFFFFFFF : realm.allowed() ? argb(BLUE, 1.0F) : 0xFF4A5060;
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, frame);
            g.enableScissor(x, y, x + w, y + h);
            picture(g, realm.id(), x, y, w, h, time);
            g.disableScissor();
            if (!realm.allowed()) {
                g.fill(x, y, x + w, y + h, 0xA0101018);
            } else if (hover) {
                g.fill(x, y, x + w, y + h, 0x2050B0FF);
            }
            Component name = Component.literal((i + 1) + ". ")
                    .append(Component.translatable("screen.enhanced.realms." + realm.id()));
            g.drawCenteredString(font, name, x + w / 2, y + h + 4, realm.allowed() ? 0xFFFFFF : 0x8890A0);
            if (!realm.allowed() && !realm.reason().isEmpty()) {
                List<FormattedCharSequence> lines = font.split(Component.translatable(realm.reason()), w - 6);
                for (int l = 0; l < Math.min(3, lines.size()); l++) {
                    g.drawCenteredString(font, lines.get(l), x + w / 2, y + h / 2 - 10 + l * 10, 0xFFD0D0);
                }
            }
        }
        g.drawCenteredString(font, Component.translatable("screen.enhanced.realms.hint"), width / 2,
                Math.min(height - 10, top(2) + windowHeight() + LABEL_SPACE + 2), 0x8AA0C0);
    }

    /** The realm's picture, in its window. */
    private static void picture(GuiGraphics g, String id, int x, int y, int w, int h, float time) {
        switch (id) {
            case "overworld" -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF5AA8F0, 0xFFBFE4FF);
                disc(g, x + w * 3 / 4, y + h / 4, h / 8, 0xFFFFF2A8);
                for (int c = 0; c < 3; c++) {
                    int cx = x + (int) ((hash(c + 300) * w + time * 0.15F * (c + 1)) % (w + 30)) - 15;
                    ellipse(g, cx, y + h / 6 + c * h / 10, w / 10, h / 22, 0xD0FFFFFF);
                }
                hills(g, x, y, w, h, 0.62F, 0.16F, 1.3F, 0xFF4E9A3A, 0.0F);
                hills(g, x, y, w, h, 0.74F, 0.1F, 2.1F, 0xFF3C7F2C, 1.0F);
                g.fill(x, y + h * 88 / 100, x + w, y + h, 0xFF6B4A2B);
            }
            case "the_nether" -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF2A0606, 0xFF6A1410);
                hills(g, x, y, w, h, 0.18F, -0.14F, 2.7F, 0xFF4A0E0E, 0.0F);
                hills(g, x, y, w, h, 0.66F, 0.14F, 1.9F, 0xFF5E1612, 2.0F);
                for (int i = 0; i < w; i += 2) {
                    float wave = Mth.sin(i * 0.15F + time * 0.12F) * 2.0F;
                    int top = y + (int) (h * 0.82F + wave);
                    g.fill(x + i, top, x + i + 2, y + h, i % 6 == 0 ? 0xFFFFB13B : 0xFFFF7A1A);
                }
            }
            case "the_end" -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF06040C, 0xFF1A0F2A);
                for (int i = 0; i < 30; i++) {
                    int sx = x + (int) (hash(i + 400) * w);
                    int sy = y + (int) (hash(i + 500) * h * 0.6F);
                    g.fill(sx, sy, sx + 1, sy + 1, argb(0xE6D8FF, 0.5F + 0.5F * hash(i + 600)));
                }
                ellipse(g, x + w / 2, y + h * 3 / 4, w * 2 / 5, h / 7, 0xFFE8E2A6);
                ellipse(g, x + w / 2, y + h * 3 / 4 + h / 14, w / 3, h / 10, 0xFFB5AE7A);
                for (int i = -1; i <= 1; i++) {
                    int px = x + w / 2 + i * w / 6;
                    g.fill(px - 2, y + h * 2 / 5, px + 2, y + h * 3 / 4, 0xFF140C1E);
                    float glow = 0.6F + 0.4F * Mth.sin(time * 0.15F + i);
                    g.fill(px - 1, y + h * 2 / 5 - 3, px + 1, y + h * 2 / 5, argb(0xF0B8FF, glow));
                }
            }
            case "asgard" -> {
                g.fillGradient(x, y, x + w, y + h, 0xFFE88A3A, 0xFFFFD98A);
                int base = y + h * 82 / 100;
                int[] spires = {12, 22, 34, 46, 58, 70, 82};
                for (int s = 0; s < spires.length; s++) {
                    int sx = x + w * spires[s] / 100;
                    int tall = (int) (h * (0.3F + 0.45F * hash(s + 900)));
                    int half = Math.max(2, w / 40 + (s % 2));
                    g.fill(sx - half, base - tall, sx + half, base, 0xFFC8962E);
                    g.fill(sx - half + 1, base - tall, sx - half + 2, base, 0xFFFFE08A);
                    g.fill(sx - 1, base - tall - h / 10, sx + 1, base - tall, 0xFFFFE9A8);
                }
                int[] bridge = {0xFF6B6B, 0xFFB45C, 0xFFF07A, 0x86FF8F, 0x6FD6FF, 0x8C86FF};
                for (int b = 0; b < bridge.length; b++) {
                    float shimmer = 0.6F + 0.4F * Mth.sin(time * 0.2F + b);
                    g.fill(x, base + b * 2, x + w, base + b * 2 + 2, argb(bridge[b], shimmer));
                }
                g.fill(x, base + 12, x + w, y + h, 0xFF1A2A5A);
            }
            default -> g.fill(x, y, x + w, y + h, 0xFF202030);
        }
    }

    /** A row of rolling hills up from {@code level} (a fraction of the height), {@code swing} high (below: hanging). */
    private static void hills(GuiGraphics g, int x, int y, int w, int h, float level, float swing, float waves,
                              int color, float phase) {
        for (int i = 0; i < w; i++) {
            float t = (float) i / w;
            int top = y + (int) (h * (level - swing * 0.5F * (1.0F + Mth.sin(t * waves * Mth.TWO_PI + phase))));
            if (swing < 0.0F) {
                g.fill(x + i, y, x + i + 1, top, color);
            } else {
                g.fill(x + i, top, x + i + 1, y + h, color);
            }
        }
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void ellipse(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
        for (int dy = -ry; dy <= ry; dy++) {
            int half = (int) (rx * Math.sqrt(1.0 - (double) dy * dy / Math.max(1, ry * ry)));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = at(mouseX, mouseY);
        if (button == 0 && index >= 0 && realms.get(index).allowed()) {
            choose(index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        // 1-4 choose a realm from the keyboard.
        int index = key - GLFW.GLFW_KEY_1;
        if (index >= 0 && index < count() && realms.get(index).allowed()) {
            choose(index);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void choose(int index) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SPACE_STONE.get(), 1.2F, 0.6F));
        PacketDistributor.sendToServer(new StoneActionPayload(StoneActionPayload.REALM, index));
        onClose();
    }

    // ---- helpers ----

    private static float time(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() + partialTick : 0.0F;
    }

    /** A steady pseudo-random number in [0, 1) for {@code i}: the stars stay where they are. */
    private static float hash(int i) {
        int h = i * 0x27D4EB2D;
        h ^= h >>> 15;
        h *= 0x165667B1;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
