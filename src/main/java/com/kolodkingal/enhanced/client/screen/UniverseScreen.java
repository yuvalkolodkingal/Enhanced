package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.multiverse.UniverseRecipe;
import com.kolodkingal.enhanced.network.UniverseNamePayload;
import com.kolodkingal.enhanced.network.UniverseChoicePayload;
import com.kolodkingal.enhanced.network.UniverseScreenPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The Multiverse screen (3.19): seven universes to choose from (616, 10005, 2149, the series' three What Ifs and a random
 * universe), drawn as windows on a starfield as the Space Stone's Realm Picker draws its realms, each a little picture
 * made in code: the green hills of 616, the X-Men's school under a blue sky in 10005, a red sky over the dead in 2149, a
 * giant in green gamma light, Hydra's grey city with its smoking chimneys, Ultron's ruins under red eyes, and the
 * Watcher's great eye over the random universe, in the colour of its sky. A universe the server does not allow now is
 * dimmed, with the reason on it. Choosing one (a click, or the keys 1-7) sends {@link UniverseChoicePayload}; typing a
 * name in the box under the windows and pressing Enter (3.19.1) sends {@link UniverseNamePayload}, the universe those
 * words make. The server checks again. The title says how one travels (the Darkhold's Dream Walking, the Sling Ring,
 * Reed Richards' bridge).
 */
public class UniverseScreen extends Screen {
    /** The multiverse's blue, for the frames. */
    private static final int BLUE = 0x3FA9FF;
    private static final int PALE = 0xB8E4FF;

    private final List<UniverseScreenPayload.UniverseOption> realms;
    private int hovered = -1;
    private EditBox nameBox;
    private String typed = "";

    private UniverseScreen(int mode, List<UniverseScreenPayload.UniverseOption> realms) {
        super(Component.translatable("screen.enhanced.multiverse.title." + (mode == 0 ? "dream" : mode == 1 ? "ring" : "bridge")));
        this.realms = realms;
    }

    /** The server asks for the screen with the universes it allows now. */
    public static void open(UniverseScreenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || payload.universes().isEmpty()) {
            return;
        }
        mc.setScreen(new UniverseScreen(payload.mode(), payload.universes()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- layout ----

    private int count() {
        return Math.min(UniverseScreenPayload.MAX, realms.size());
    }

    /** Room above the windows for the title, under each window for its name, between the rows, for the name box and the hint. */
    private static final int TITLE_SPACE = 24;
    private static final int LABEL_SPACE = 16;
    private static final int ROW_GAP = 6;
    private static final int HINT_SPACE = 44;
    private static final int COLUMNS = 4;
    private static final int GAP = 12;

    /** The windows' width: as wide as the screen allows, but low enough that both rows, names, box and hint fit. */
    private int windowWidth() {
        int byHeight = (height - TITLE_SPACE - HINT_SPACE - 2 * LABEL_SPACE - ROW_GAP - 8) / 2 * 5 / 3;
        return Mth.clamp(Math.min((width - 24 - GAP * (COLUMNS - 1)) / COLUMNS, byHeight), 50, 130);
    }

    private int windowHeight() {
        return windowWidth() * 3 / 5;
    }

    private int left(int index) {
        int w = windowWidth();
        int inRow = Math.min(COLUMNS, count() - (index / COLUMNS) * COLUMNS);
        return (width - (inRow * w + (inRow - 1) * GAP)) / 2 + (index % COLUMNS) * (w + GAP);
    }

    private int top(int index) {
        int row = windowHeight() + LABEL_SPACE + ROW_GAP;
        int block = TITLE_SPACE + 2 * row - ROW_GAP + HINT_SPACE;
        return Math.max(0, (height - block) / 2) + TITLE_SPACE + (index / COLUMNS) * row;
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(220, width - 40);
        int boxTop = top(COLUMNS) + windowHeight() + LABEL_SPACE + 2;
        nameBox = new EditBox(font, (width - boxWidth) / 2, Math.min(height - 34, boxTop), boxWidth, 18,
                Component.translatable("screen.enhanced.multiverse.name"));
        nameBox.setMaxLength(UniverseNamePayload.MAX);
        nameBox.setHint(Component.translatable("screen.enhanced.multiverse.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(typed);
        nameBox.setResponder(value -> typed = value);
        addRenderableWidget(nameBox);
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
            UniverseScreenPayload.UniverseOption realm = realms.get(i);
            int x = left(i);
            int y = top(i);
            int w = windowWidth();
            int h = windowHeight();
            boolean hover = i == hovered && realm.allowed();
            int frame = hover ? 0xFFFFFFFF : realm.allowed() ? argb(BLUE, 1.0F) : 0xFF4A5060;
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, frame);
            g.enableScissor(x, y, x + w, y + h);
            picture(g, realm.universe(), x, y, w, h, time);
            g.disableScissor();
            if (!realm.allowed()) {
                g.fill(x, y, x + w, y + h, 0xA0101018);
            } else if (hover) {
                g.fill(x, y, x + w, y + h, 0x2050B0FF);
            }
            Component label = switch (realm.universe()) {
                case Multiverse.GAMMA -> Component.translatable("screen.enhanced.multiverse.gamma");
                case Multiverse.HYDRA -> Component.translatable("screen.enhanced.multiverse.hydra");
                case Multiverse.ULTRON -> Component.translatable("screen.enhanced.multiverse.ultron");
                default -> Component.translatable("screen.enhanced.multiverse.earth", realm.universe());
            };
            Component name = Component.literal((i + 1) + ". ").append(label);
            g.drawCenteredString(font, name, x + w / 2, y + h + 4, realm.allowed() ? 0xFFFFFF : 0x8890A0);
            if (Multiverse.isWhatIf(realm.universe())) {
                List<FormattedCharSequence> what = font.split(UniverseRecipe.of(realm.universe()).title(), w + 10);
                if (!what.isEmpty()) {
                    g.drawCenteredString(font, what.get(0), x + w / 2, y + 3, 0xFFF2C0);
                }
            }
            if (!realm.allowed() && !realm.reason().isEmpty()) {
                List<FormattedCharSequence> lines = font.split(Component.translatable(realm.reason()), w - 6);
                for (int l = 0; l < Math.min(3, lines.size()); l++) {
                    g.drawCenteredString(font, lines.get(l), x + w / 2, y + h / 2 - 10 + l * 10, 0xFFD0D0);
                }
            }
        }
        g.drawCenteredString(font, Component.translatable("screen.enhanced.multiverse.hint"), width / 2,
                Math.min(height - 12, nameBox.getY() + 24), 0x8AA0C0);
    }

    /** The universe's picture, in its window. */
    private static void picture(GuiGraphics g, int universe, int x, int y, int w, int h, float time) {
        switch (universe) {
            case Multiverse.MAIN -> {
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
            case Multiverse.XMEN -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF2E6FD0, 0xFFA8D2FF);
                hills(g, x, y, w, h, 0.72F, 0.08F, 1.6F, 0xFF3E8A34, 0.5F);
                // The school: a long house with a roof and two towers, its windows lit.
                int base = y + h * 78 / 100;
                int left = x + w / 5;
                int right = x + w * 4 / 5;
                g.fill(left, base - h / 4, right, base, 0xFF8A6A52);
                for (int r = 0; r < h / 8; r++) {
                    g.fill(left + r, base - h / 4 - r, right - r, base - h / 4 - r + 1, 0xFF4A3A44);
                }
                g.fill(left - w / 20, base - h * 2 / 5, left + w / 20, base, 0xFF7A5C48);
                g.fill(right - w / 20, base - h * 2 / 5, right + w / 20, base, 0xFF7A5C48);
                for (int wx = left + w / 20; wx < right - w / 20; wx += Math.max(4, w / 14)) {
                    float lit = 0.6F + 0.4F * Mth.sin(time * 0.05F + wx);
                    g.fill(wx, base - h / 6, wx + 2, base - h / 6 + 3, argb(0xFFE08A, lit));
                }
                // The X, in the sky.
                int cx = x + w / 2;
                int cy = y + h / 5;
                for (int d = -h / 10; d <= h / 10; d++) {
                    g.fill(cx + d - 1, cy + d, cx + d + 1, cy + d + 1, 0xFFFFD23A);
                    g.fill(cx - d - 1, cy + d, cx - d + 1, cy + d + 1, 0xFFFFD23A);
                }
                g.fill(x, base, x + w, y + h, 0xFF356E2C);
            }
            case Multiverse.ZOMBIES -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF3A0A0A, 0xFF8A3A1A);
                disc(g, x + w / 4, y + h / 4, h / 9, 0xFFD8C090);
                hills(g, x, y, w, h, 0.7F, 0.1F, 1.8F, 0xFF2E2A22, 0.0F);
                // The dead walk: green heads under iron helmets, swaying.
                for (int z = 0; z < 6; z++) {
                    int zx = x + w * (8 + z * 16) / 100;
                    int sway = (int) (Mth.sin(time * 0.08F + z * 1.7F) * 2.0F);
                    int top = y + h * 62 / 100 + (z % 2) * h / 14;
                    int s = Math.max(3, w / 22);
                    g.fill(zx - s + sway, top, zx + s + sway, top + s * 2, 0xFF4F8A3A);
                    g.fill(zx - s - 1 + sway, top - 2, zx + s + 1 + sway, top + 2, 0xFF9AA0A8);
                    g.fill(zx - s + 1 + sway, top + s, zx - s + 3 + sway, top + s + 2, 0xFF1A1A1A);
                    g.fill(zx + s - 3 + sway, top + s, zx + s - 1 + sway, top + s + 2, 0xFF1A1A1A);
                    g.fill(zx - s / 2 + sway, top + s * 2, zx + s / 2 + sway, y + h, 0xFF2E5A7A);
                }
            }
            case Multiverse.GAMMA -> {
                // Green gamma light, and a giant of it stepping over the hills.
                g.fillGradient(x, y, x + w, y + h, 0xFF0E3A10, 0xFF6BE04A);
                float pulse = 0.5F + 0.5F * Mth.sin(time * 0.08F);
                disc(g, x + w * 3 / 4, y + h / 4, h / 7, argb(0xD8FF9A, 0.6F + 0.4F * pulse));
                hills(g, x, y, w, h, 0.78F, 0.08F, 1.4F, 0xFF2E6A1E, 0.3F);
                int gx = x + w / 3;
                int base = y + h * 82 / 100;
                int s = Math.max(4, h / 7);
                g.fill(gx - s, base - s * 4, gx + s, base - s * 2, 0xFF3F9A2E);        // the body
                g.fill(gx - s / 2, base - s * 5, gx + s / 2, base - s * 4, 0xFF4FB43A); // the head
                g.fill(gx - s * 2, base - s * 4, gx - s, base - s * 2 - s / 2, 0xFF3F9A2E); // the arms
                g.fill(gx + s, base - s * 4, gx + s * 2, base - s * 2 - s / 2, 0xFF3F9A2E);
                g.fill(gx - s, base - s * 2, gx - s / 4, base, 0xFF4A2E7A);            // purple trousers
                g.fill(gx + s / 4, base - s * 2, gx + s, base, 0xFF4A2E7A);
                for (int i = 0; i < 10; i++) {
                    int sx = x + (int) (hash(i + 40) * w);
                    int sy = y + (int) ((hash(i + 60) * h + time * 0.3F * (1 + i % 3)) % h);
                    g.fill(sx, sy, sx + 1, sy + 1, 0xFFC8FF9A);
                }
            }
            case Multiverse.HYDRA -> {
                // A grey city under smog: blocks of flats with red stripes, chimneys smoking, the black Citadel.
                g.fillGradient(x, y, x + w, y + h, 0xFF4A4642, 0xFF8A8278);
                int base = y + h * 86 / 100;
                for (int b = 0; b < 7; b++) {
                    int bx = x + b * w / 7;
                    int bh = (int) (h * (0.25F + hash(b + 120) * 0.35F));
                    g.fill(bx + 1, base - bh, bx + w / 7 - 1, base, 0xFF6E6C6A);
                    g.fill(bx + w / 14 - 1, base - bh, bx + w / 14 + 1, base, 0xFFB02020);
                }
                int cx = x + w / 2;
                g.fill(cx - w / 12, y + h / 8, cx + w / 12, base, 0xFF141416);
                g.fill(cx - 1, y + h / 8, cx + 1, base, 0xFFC0141E);
                for (int c = 0; c < 2; c++) {
                    int sx = x + w * (15 + c * 65) / 100;
                    g.fill(sx, base - h / 2, sx + 3, base, 0xFF7A3A2A);
                    for (int puff = 0; puff < 4; puff++) {
                        float t = (time * 0.04F + puff * 0.25F + c * 0.5F) % 1.0F;
                        ellipse(g, sx + 1 + (int) (t * 8), base - h / 2 - (int) (t * h / 3), 2 + (int) (t * 4), 1 + (int) (t * 2),
                                argb(0x3A3836, 0.7F * (1.0F - t)));
                    }
                }
                g.fill(x, base, x + w, y + h, 0xFF202022);
            }
            case Multiverse.ULTRON -> {
                // A dead city under a red sky, and Ultron's eyes in the dark.
                g.fillGradient(x, y, x + w, y + h, 0xFF1A0606, 0xFF5A1A12);
                int base = y + h * 86 / 100;
                for (int b = 0; b < 8; b++) {
                    int bx = x + b * w / 8;
                    int bh = (int) (h * (0.15F + hash(b + 220) * 0.4F));
                    for (int col = bx + 1; col < bx + w / 8 - 1; col++) {
                        int jag = (int) (hash(col * 7 + b) * h * 0.08F);
                        g.fill(col, base - bh + jag, col + 1, base, 0xFF2A2626);
                    }
                }
                for (int e = 0; e < 4; e++) {
                    int ex = x + (int) ((hash(e + 300) * w + time * 0.2F * (e + 1)) % w);
                    int ey = y + h / 6 + (int) (hash(e + 310) * h / 3) + (int) (Mth.sin(time * 0.1F + e) * 2);
                    float glow = 0.6F + 0.4F * Mth.sin(time * 0.2F + e);
                    g.fill(ex, ey, ex + 2, ey + 1, argb(0xFF2A1A, glow));
                    g.fill(ex + 3, ey, ex + 5, ey + 1, argb(0xFF2A1A, glow));
                }
                g.fill(x, base, x + w, y + h, 0xFF141010);
            }
            default -> {
                // The Watcher's eye over the infinite multiverse, in the colour of the universe's sky.
                int sky = UniverseRecipe.of(universe).fog();
                g.fillGradient(x, y, x + w, y + h, 0xFF0A0420, sky < 0 ? 0xFF2A1050 : 0xFF000000 | darker(sky));
                for (int i = 0; i < 40; i++) {
                    int sx = x + (int) (hash(i + universe % 1000 + 700) * w);
                    int sy = y + (int) (hash(i + universe % 1000 + 800) * h);
                    g.fill(sx, sy, sx + 1, sy + 1, argb(0xE0D0FF, 0.4F + 0.6F * hash(i + 900)));
                }
                int cx = x + w / 2;
                int cy = y + h / 2 + 4;
                for (int ring = 3; ring >= 1; ring--) {
                    float pulse = 0.5F + 0.5F * Mth.sin(time * 0.1F + ring);
                    ellipse(g, cx, cy, w * ring / 9, h * ring / 14, argb(ring == 1 ? 0xFFFFFF : 0x9A70FF, 0.25F + 0.2F * pulse));
                }
                ellipse(g, cx, cy, w / 5, h / 9, 0xFFF4EEFF);
                disc(g, cx, cy, h / 12, 0xFF6A3AC8);
                disc(g, cx, cy, h / 24, 0xFF100820);
            }
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
        if (nameBox != null && nameBox.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int index = at(mouseX, mouseY);
        if (button == 0 && index >= 0 && realms.get(index).allowed()) {
            choose(index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !typed.isBlank()) {
                travelTo(typed.trim());
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }
        // 1-7 choose a universe from the keyboard.
        int index = key - GLFW.GLFW_KEY_1;
        if (index >= 0 && index < count() && realms.get(index).allowed()) {
            choose(index);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    /** The typed name: the universe those words make. */
    private void travelTo(String words) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SPACE_STONE.get(), 0.8F, 0.5F));
        Net.sendToServer(new UniverseNamePayload(words));
        onClose();
    }

    private void choose(int index) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SPACE_STONE.get(), 0.8F, 0.6F));
        Net.sendToServer(new UniverseChoicePayload(index));
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

    private static int darker(int rgb) {
        return (((rgb >> 16) & 0xFF) * 3 / 5 << 16) | (((rgb >> 8) & 0xFF) * 3 / 5 << 8) | ((rgb & 0xFF) * 3 / 5);
    }

    private static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
