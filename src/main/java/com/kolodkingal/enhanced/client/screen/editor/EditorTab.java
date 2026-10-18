package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One tab of the Hero Creator's editor (3.4): it puts its widgets in the page's box when the screen is laid out, and draws
 * its words over them. The draft and everything shared (the limits, the preview, the pop-up, the status line) are the
 * screen's. Helpers here lay out cards in pages and buttons in rows, the same way on every tab.
 */
public abstract class EditorTab {
    /** The gap between cards. */
    protected static final int GAP = 3;

    protected final HeroEditorScreen screen;

    protected EditorTab(HeroEditorScreen screen) {
        this.screen = screen;
    }

    /** Lays out its widgets in the box of the page. */
    public abstract void init(int x, int y, int w, int h);

    /** Draws its words (over the screen's background, under its widgets' tooltips). */
    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
    }

    protected CustomHero draft() {
        return screen.draft();
    }

    /** Changes the draft and lays the page out again. */
    protected void set(CustomHero hero) {
        screen.setDraft(hero);
    }

    protected CustomHeroRules.Limits limits() {
        return screen.limits();
    }

    protected Font font() {
        return Minecraft.getInstance().font;
    }

    protected <T extends AbstractWidget> T add(T widget) {
        return screen.add(widget);
    }

    protected void text(GuiGraphics g, Component text, int x, int y, int room, int color) {
        CreatorUi.drawFitted(g, font(), text, x, y, room, color);
    }

    /**
     * One card or button of a list.
     *
     * @param tooltip shown on hover, or null
     */
    public record Card(Component label, @Nullable Component sub, @Nullable ItemStack icon, @Nullable ResourceLocation face, int color,
                       boolean selected, boolean enabled, @Nullable Component tooltip, Runnable action) {
        public static Card of(Component label, int color, boolean selected, Runnable action) {
            return new Card(label, null, null, null, color, selected, true, null, action);
        }

        public Card sub(@Nullable Component sub) {
            return new Card(label, sub, icon, face, color, selected, enabled, tooltip, action);
        }

        public Card icon(@Nullable ItemStack icon) {
            return new Card(label, sub, icon, face, color, selected, enabled, tooltip, action);
        }

        public Card face(@Nullable ResourceLocation face) {
            return new Card(label, sub, icon, face, color, selected, enabled, tooltip, action);
        }

        public Card enabled(boolean enabled) {
            return new Card(label, sub, icon, face, color, selected, enabled, tooltip, action);
        }

        public Card tooltip(@Nullable Component tooltip) {
            return new Card(label, sub, icon, face, color, selected, enabled, tooltip, action);
        }
    }

    protected CreatorUi.BigButton button(Card c, int x, int y, int w, int h) {
        CreatorUi.BigButton b = new CreatorUi.BigButton(x, y, w, h, c.label(), c.sub(), c.color(), c.action()).icon(c.icon()).face(c.face())
                .selected(c.selected());
        b.active = c.enabled();
        if (c.tooltip() != null) {
            b.setTooltip(Tooltip.create(c.tooltip()));
        }
        return add(b);
    }

    /**
     * Cards in a grid filling {@code w} x {@code h}, as many columns of at least {@code minW} as fit, a page at a time
     * ({@code pageKey} remembers which), with arrows under them when there are more.
     */
    protected void cards(String pageKey, List<Card> cards, int x, int y, int w, int h, int minW, int cardH) {
        int cols = Math.max(1, (w + GAP) / (minW + GAP));
        int cardW = (w - (cols - 1) * GAP) / cols;
        int rows = Math.max(1, (h + GAP) / (cardH + GAP));
        if (cards.size() > cols * rows) {
            rows = Math.max(1, (h - 18 + GAP) / (cardH + GAP));
        }
        int perPage = cols * rows;
        int pages = Math.max(1, (cards.size() + perPage - 1) / perPage);
        int page = screen.page(pageKey, pages);
        for (int i = 0; i < perPage && page * perPage + i < cards.size(); i++) {
            Card c = cards.get(page * perPage + i);
            button(c, x + (i % cols) * (cardW + GAP), y + (i / cols) * (cardH + GAP), cardW, cardH);
        }
        if (pages > 1) {
            pager(pageKey, pages, x, y + rows * (cardH + GAP), w);
        }
    }

    /** The arrows of a list of {@code pages} pages, and where it is. */
    protected void pager(String pageKey, int pages, int x, int y, int w) {
        int page = screen.page(pageKey, pages);
        int bw = Math.min(40, w / 3);
        CreatorUi.BigButton back = add(new CreatorUi.BigButton(x, y, bw, 15, Component.literal("<"), null, 0xC0C0C0,
                () -> screen.turn(pageKey, pages, -1)));
        back.active = page > 0;
        CreatorUi.BigButton where = add(new CreatorUi.BigButton(x + bw + GAP, y, w - 2 * (bw + GAP), 15,
                Component.translatable("screen.enhanced.hero_creator.page", page + 1, pages), null, 0x808080, () -> { }));
        where.active = false;
        CreatorUi.BigButton next = add(new CreatorUi.BigButton(x + w - bw, y, bw, 15, Component.literal(">"), null, 0xC0C0C0,
                () -> screen.turn(pageKey, pages, 1)));
        next.active = page < pages - 1;
    }

    /** Buttons of width {@code cardW} one after another, wrapping at {@code w}; returns the y under the last row. */
    protected int flow(List<Card> cards, int x, int y, int w, int cardW, int cardH) {
        int cols = Math.max(1, (w + GAP) / (cardW + GAP));
        for (int i = 0; i < cards.size(); i++) {
            button(cards.get(i), x + (i % cols) * (cardW + GAP), y + (i / cols) * (cardH + GAP), cardW, cardH);
        }
        int rows = (cards.size() + cols - 1) / cols;
        return y + rows * (cardH + GAP);
    }
}
