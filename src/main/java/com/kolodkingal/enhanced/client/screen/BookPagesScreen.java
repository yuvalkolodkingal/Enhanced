package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.ability.UltimateWanda;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * (3.19.2) The pages of a book in the hand, to read: the Read Book key (default K) opens them while the Darkhold or the
 * Book of the Vishanti is held, and the arrows (or the buttons, or A and D) turn them.
 * <ul>
 *   <li>The Darkhold, open as in the films: charred pages of ash, their burnt edges smouldering. Four pages: the prophecy
 *   of the Scarlet Witch, the powers of the Darkhold, the prophecy of Billy the Demiurge, and what chaos magic is.</li>
 *   <li>The Book of the Vishanti, in its blue and its light. Two pages: the Vishanti (Agamotto, Oshtur, Hoggoth), and a
 *   page that changes for whoever reads it: not the dark that the Darkhold sees in them but their potential, and the good
 *   they can do.</li>
 * </ul>
 * Put the book away and the pages close.
 */
public class BookPagesScreen extends Screen {
    public enum Kind {
        DARKHOLD("darkhold", 4),
        VISHANTI("vishanti", 2);

        final String id;
        final int pages;

        Kind(String id, int pages) {
            this.id = id;
            this.pages = pages;
        }
    }

    private static final int PAGE_W = 170;
    /** The page's height: as tall as the screen allows, within reason. */
    private int pageH = 220;
    private static final int GAP = 14;

    private final Kind kind;
    private int page;

    public BookPagesScreen(Kind kind) {
        super(Component.translatable("book.enhanced." + kind.id + ".title"));
        this.kind = kind;
    }

    /** Which book the player holds, if any (the Darkhold first). */
    public static Kind held(Player player) {
        if (DarkholdBook.holds(player)) {
            return Kind.DARKHOLD;
        }
        return VishantiBook.holds(player) ? Kind.VISHANTI : null;
    }

    /** Opens the pages of the book in the hand; false if no book is held. */
    public static boolean openHeld() {
        Minecraft mc = Minecraft.getInstance();
        Kind kind = mc.player == null ? null : held(mc.player);
        if (kind == null) {
            return false;
        }
        mc.setScreen(new BookPagesScreen(kind));
        return true;
    }

    /** The key of the potential page for this reader (Vishanti page 2). */
    public static String potentialKey(Player player) {
        PowerData data = Powers.get(player);
        String id = UltimateWanda.is(data) ? "ultimate_wanda" : data.power().name().toLowerCase(Locale.ROOT);
        String key = "book.enhanced.vishanti.potential." + id;
        return I18n.exists(key) ? key : "book.enhanced.vishanti.potential.other";
    }

    @Override
    protected void init() {
        pageH = Mth.clamp(height - 64, 140, 250);
        int y = top() + pageH + 8;
        int cx = width / 2;
        // (in a right-to-left language the book opens the other way: the first page on the right, the next to the left)
        boolean rtl = rtl();
        addRenderableWidget(Button.builder(Component.literal("\u25C0"), b -> turn(rtl ? 1 : -1)).bounds(cx - 120, y, 30, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(cx - 50, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("\u25B6"), b -> turn(rtl ? -1 : 1)).bounds(cx + 90, y, 30, 20).build());
    }

    private static boolean rtl() {
        return net.minecraft.locale.Language.getInstance().isDefaultRightToLeft();
    }

    private int top() {
        return Math.max(4, (height - pageH - 32) / 2);
    }

    /** One spread shows two pages; turning moves a spread. */
    private int spreads() {
        return (kind.pages + 1) / 2;
    }

    private void turn(int by) {
        page = Mth.clamp(page + by, 0, spreads() - 1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            turn(rtl() ? 1 : -1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            turn(rtl() ? -1 : 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive() || held(player) != kind) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        float time = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + partialTick;
        int top = top();
        int left = width / 2 - PAGE_W - GAP / 2;
        int right = width / 2 + GAP / 2;
        if (kind == Kind.DARKHOLD) {
            darkholdSpread(graphics, left, top, time);
        } else {
            vishantiSpread(graphics, left, top, time);
        }
        for (int side = 0; side < 2; side++) {
            int index = page * 2 + side;
            if (index < kind.pages) {
                page(graphics, index, (side == 0) != rtl() ? left : right, top);
            }
        }
    }

    /** The text of page {@code index} (from 0) at its place. */
    private void page(GuiGraphics graphics, int index, int x, int y) {
        boolean dark = kind == Kind.DARKHOLD;
        int titleColor = dark ? 0xFF5A0A0A : 0xFFFFFFFF;
        int textColor = dark ? 0xFF1E1614 : 0xFFDCEBFF;
        Component pageTitle;
        Component body;
        Player player = Minecraft.getInstance().player;
        if (kind == Kind.VISHANTI && index == 1 && player != null) {
            pageTitle = Component.translatable("book.enhanced.vishanti.page2.title", player.getDisplayName());
            body = Component.translatable(potentialKey(player));
        } else {
            String base = "book.enhanced." + kind.id + ".page" + (index + 1);
            pageTitle = Component.translatable(base + ".title");
            body = Component.translatable(base);
        }
        int inner = PAGE_W - 22;
        int ty = y + 14;
        for (FormattedCharSequence line : font.split(pageTitle, inner)) {
            graphics.drawString(font, line, x + PAGE_W / 2 - font.width(line) / 2, ty, titleColor, false);
            ty += 10;
        }
        ty += 6;
        List<FormattedCharSequence> lines = font.split(body, inner);
        int room = (y + pageH - 22 - ty) / 9;
        for (int i = 0; i < Math.min(room, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), x + 11, ty + i * 9, textColor, false);
        }
        Component number = Component.translatable("book.enhanced.page_of", index + 1, kind.pages);
        graphics.drawString(font, number, x + PAGE_W / 2 - font.width(number) / 2, y + pageH - 14, dark ? 0xFF4A3A34 : 0xFF8FB8FF, false);
    }

    /** The Darkhold open: pages of ash, smudged, their edges burnt black and glowing with embers. */
    private void darkholdSpread(GuiGraphics g, int left, int top, float time) {
        int w = PAGE_W * 2 + GAP;
        g.fill(left - 6, top - 6, left + w + 6, top + pageH + 6, 0xFF120C0C);
        g.fill(left + PAGE_W, top - 4, left + PAGE_W + GAP, top + pageH + 4, 0xFF231816);
        for (int side = 0; side < 2; side++) {
            int x = side == 0 ? left : left + PAGE_W + GAP;
            g.fill(x, top, x + PAGE_W, top + pageH, 0xFFB4AEA3);
            RandomSource smudge = RandomSource.create(77L + side);
            for (int i = 0; i < 40; i++) {
                int sx = x + smudge.nextInt(PAGE_W - 12);
                int sy = top + smudge.nextInt(pageH - 12);
                int s = 4 + smudge.nextInt(10);
                g.fill(sx, sy, sx + s, sy + s / 2 + 2, ((0x18 + smudge.nextInt(3) * 0x10) << 24) | 0x8A8278);
            }
            // its burnt edge, bitten into the page, with embers that come and go
            RandomSource burn = RandomSource.create(991L + side);
            int step = 6;
            for (int e = 0; e < (PAGE_W + pageH) * 2 / step; e++) {
                int d = e * step;
                int ex;
                int ey;
                if (d < PAGE_W) {
                    ex = x + d;
                    ey = top;
                } else if (d < PAGE_W + pageH) {
                    ex = x + PAGE_W - 6;
                    ey = top + d - PAGE_W;
                } else if (d < PAGE_W * 2 + pageH) {
                    ex = x + PAGE_W - (d - PAGE_W - pageH) - 6;
                    ey = top + pageH - 6;
                } else {
                    ex = x;
                    ey = top + pageH - (d - PAGE_W * 2 - pageH) - 6;
                }
                int bite = 3 + burn.nextInt(6);
                g.fill(ex, ey, ex + 6, ey + 6, 0xFF1A1110);
                g.fill(ex + 1, ey + 1, ex + 1 + bite / 2, ey + 1 + bite / 2, 0xFF2B1D19);
                float flicker = Mth.sin(time * 0.35F + e * 1.7F + side * 3.0F);
                if (flicker > 0.55F) {
                    int ember = flicker > 0.85F ? 0xFFFFB040 : 0xFFE0580E;
                    g.fill(ex + 2, ey + 2, ex + 4, ey + 4, ember);
                }
            }
        }
        // sparks rising off it
        for (int i = 0; i < 14; i++) {
            float rise = (time * 0.6F + i * 37) % (pageH + 30);
            int sx = left + (int) ((i * 53 + Mth.sin(time * 0.05F + i) * 8) % w);
            int sy = top + pageH - (int) rise;
            g.fill(sx, sy, sx + 2, sy + 2, i % 3 == 0 ? 0xFF2A2222 : 0xFFFF7A1A);
        }
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withBold(true).withColor(0xC0352A)), width / 2, top - 16, 0xFFFFFFFF);
    }

    /** The Book of the Vishanti open: pages of deep blue in a frame of silver-blue, a light shining from its heart. */
    private void vishantiSpread(GuiGraphics g, int left, int top, float time) {
        int w = PAGE_W * 2 + GAP;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.12F);
        // the light behind it
        for (int r = 6; r >= 1; r--) {
            int a = (int) ((10 + 8 * pulse) * (7 - r) / 6.0F);
            g.fill(left - 6 - r * 5, top - 6 - r * 5, left + w + 6 + r * 5, top + pageH + 6 + r * 5, (a << 24) | 0xBFE4FF);
        }
        g.fill(left - 6, top - 6, left + w + 6, top + pageH + 6, 0xFF0A1A5C);
        g.fill(left - 4, top - 4, left + w + 4, top + pageH + 4, 0xFF9CC4FF);
        g.fill(left - 3, top - 3, left + w + 3, top + pageH + 3, 0xFF0E2470);
        for (int side = 0; side < 2; side++) {
            int x = side == 0 ? left : left + PAGE_W + GAP;
            g.fill(x, top, x + PAGE_W, top + pageH, 0xFF13307F);
            g.fill(x + 4, top + 4, x + PAGE_W - 4, top + 5, 0xFF5F95FF);
            g.fill(x + 4, top + pageH - 5, x + PAGE_W - 4, top + pageH - 4, 0xFF5F95FF);
            g.fill(x + 4, top + 4, x + 5, top + pageH - 4, 0xFF5F95FF);
            g.fill(x + PAGE_W - 5, top + 4, x + PAGE_W - 4, top + pageH - 4, 0xFF5F95FF);
            // little lights drifting over the page
            for (int i = 0; i < 9; i++) {
                float t = time * 0.02F + i * 0.7F + side;
                int sx = x + 8 + (int) ((Mth.sin(t * 1.3F) * 0.5F + 0.5F) * (PAGE_W - 18));
                int sy = top + 8 + (int) ((Mth.cos(t * 0.9F + i) * 0.5F + 0.5F) * (pageH - 18));
                int a = (int) (90 + 120 * (0.5F + 0.5F * Mth.sin(time * 0.2F + i * 2.0F)));
                g.fill(sx, sy, sx + 2, sy + 2, (a << 24) | 0xE8F6FF);
            }
        }
        // the spine shines
        int glow = (int) (150 + 100 * pulse);
        g.fill(left + PAGE_W, top - 3, left + PAGE_W + GAP, top + pageH + 3, (glow << 24) | 0xDFF2FF);
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withBold(true).withColor(0xBFE4FF)), width / 2, top - 18, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
