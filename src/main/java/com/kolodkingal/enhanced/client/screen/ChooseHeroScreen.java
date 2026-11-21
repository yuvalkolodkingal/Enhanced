package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.spellbook.Spellbook;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * "Choose a hero" (3.3): a card for every hero of the mod (from {@link Power} and {@link Variants}, so a hero added to the
 * mod appears here by itself), with its face and its name in its colour; a click shows its looks below, and
 * <i>Become!</i> makes the player that hero in that look at once.
 *
 * <p>3.18.1: the three witches of the spellbooks are heroes of their own here, after the mod's heroes (a card each, with
 * her book): <i>Become!</i> makes the player that book's witch, at her level in the book ({@code CustomHeroes.chooseWitch}).
 */
public class ChooseHeroScreen extends Screen {
    private static final int CARD_W = 82;
    private static final int CARD_H = 24;
    private static final int GAP = 3;
    private static final int TOP = 26;
    private static final int BOTTOM = 62;

    private final Screen parent;
    private final List<Power> heroes = HeroLooks.heroes();
    /** 3.18.1: the witches, after the heroes. */
    private final List<Spellbook> witches = List.of(Spellbook.values());
    @Nullable
    private Power chosen;
    @Nullable
    private Spellbook chosenWitch;
    private String look = "";
    private int scroll;

    public ChooseHeroScreen(Screen parent) {
        super(Component.translatable("screen.enhanced.hero_creator.choose"));
        this.parent = parent;
    }

    private int columns() {
        return Math.max(2, Math.min(6, (width - 16) / (CARD_W + GAP)));
    }

    private int visibleRows() {
        return Math.max(1, (height - TOP - BOTTOM) / (CARD_H + GAP));
    }

    private int rows() {
        return (heroes.size() + witches.size() + columns() - 1) / columns();
    }

    @Override
    protected void init() {
        int cols = columns();
        scroll = Math.max(0, Math.min(scroll, rows() - visibleRows()));
        int left = width / 2 - (cols * (CARD_W + GAP) - GAP) / 2;
        for (int i = 0; i < heroes.size(); i++) {
            int row = i / cols - scroll;
            if (row < 0 || row >= visibleRows()) {
                continue;
            }
            Power power = heroes.get(i);
            int x = left + (i % cols) * (CARD_W + GAP);
            int y = TOP + row * (CARD_H + GAP);
            addRenderableWidget(new CreatorUi.BigButton(x, y, CARD_W, CARD_H, power.displayName(), null, power.titleColor(), () -> pick(power))
                    .face(face(power, "")).selected(power == chosen));
        }
        for (int j = 0; j < witches.size(); j++) {
            int i = heroes.size() + j;
            int row = i / cols - scroll;
            if (row < 0 || row >= visibleRows()) {
                continue;
            }
            Spellbook book = witches.get(j);
            int x = left + (i % cols) * (CARD_W + GAP);
            int y = TOP + row * (CARD_H + GAP);
            addRenderableWidget(new CreatorUi.BigButton(x, y, CARD_W, CARD_H, book.title(), null, book.color, () -> pickWitch(book))
                    .icon(bookStack(book)).selected(book == chosenWitch));
        }
        int bottom = height - BOTTOM + 4;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(6, 4, 50, 18).build());
        if (chosenWitch != null) {
            Spellbook book = chosenWitch;
            int becomeW = 96;
            addRenderableWidget(new CreatorUi.BigButton(width - 8 - becomeW, bottom, becomeW, 36,
                    Component.translatable("screen.enhanced.hero_creator.become"), null, book.color, () -> {
                        Net.sendToServer(HeroCreatorPayload.chooseWitch(book));
                        onClose();
                    }).icon(bookStack(book)));
            return;
        }
        if (chosen == null) {
            return;
        }
        List<Variant> looks = Variants.of(chosen);
        int lookW = 76;
        int becomeW = 96;
        int room = width - 16 - becomeW - 6;
        int perRow = Math.max(1, room / (lookW + GAP));
        int x0 = 8;
        for (int i = 0; i < looks.size() && i < perRow * 2; i++) {
            Variant v = looks.get(i);
            int x = x0 + (i % perRow) * (lookW + GAP);
            int y = bottom + (i / perRow) * (20 + GAP);
            addRenderableWidget(new CreatorUi.BigButton(x, y, lookW, 20, v.displayName(), null, v.color(), () -> {
                look = v.id();
                rebuild();
            }).face(face(chosen, v.id())).selected(v.id().equals(look) || look.isEmpty() && i == 0));
        }
        Power power = chosen;
        addRenderableWidget(new CreatorUi.BigButton(width - 8 - becomeW, bottom, becomeW, 36, Component.translatable("screen.enhanced.hero_creator.become"),
                null, power.titleColor(), () -> {
                    Net.sendToServer(HeroCreatorPayload.choose(power, look));
                    onClose();
                }).face(face(power, look)));
    }

    private void pick(Power power) {
        chosen = power;
        chosenWitch = null;
        look = "";
        rebuild();
    }

    /** 3.18.1: a witch chosen (she has no looks: her own skin, as on her book). */
    private void pickWitch(Spellbook book) {
        chosen = null;
        chosenWitch = book;
        look = "";
        rebuild();
    }

    /** 3.18.1: a witch's card shows her book. */
    static ItemStack bookStack(Spellbook book) {
        return new ItemStack(switch (book) {
            case ETHEREAL -> ModItems.BOOK_OF_ETHEREAL_WITCHCRAFT.get();
            case ELDRITCH -> ModItems.BOOK_OF_ELDRITCH_WITCHCRAFT.get();
            case GREEN -> ModItems.BOOK_OF_GREEN_WITCHCRAFT.get();
        });
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    /** The face of a hero in a look: its suit's, or the player's own for "your own skin". */
    private static ResourceLocation face(Power power, String lookId) {
        Variant v = Variants.get(power, lookId);
        if (v.skin() != null) {
            return v.skin();
        }
        List<HeroLooks.Look> looks = HeroLooks.of(power);
        if (!looks.isEmpty() && (Variants.of(power).isEmpty() || lookId.isEmpty())) {
            return looks.get(0).skin();
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.getSkinManager().getInsecureSkin(mc.getGameProfile()).texture();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int before = scroll;
        scroll = Math.max(0, Math.min(rows() - visibleRows(), scroll - (int) Math.signum(scrollY)));
        if (scroll != before) {
            rebuild();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(HeroCreatorScreen.GOLD).withBold(true)), width / 2, 8, 0xFFFFFF);
        if (rows() > visibleRows()) {
            g.drawCenteredString(font, Component.translatable("screen.enhanced.hero_creator.scroll").withStyle(ChatFormatting.DARK_GRAY),
                    width / 2, height - BOTTOM - 8, 0xFFFFFF);
        }
        if (chosenWitch != null) {
            // (3.18.1) what a witch is: her magic grows with her level in her book
            int textW = width - 16 - 96 - 12;
            int ty = height - BOTTOM + 6;
            g.drawString(font, chosenWitch.title().copy().withStyle(s -> s.withColor(chosenWitch.color).withBold(true)), 8, ty, 0xFFFFFF);
            for (net.minecraft.util.FormattedCharSequence line : font.split(Component.translatable("screen.enhanced.hero_creator.witch.desc",
                    chosenWitch.bookName()).withStyle(ChatFormatting.GRAY), textW)) {
                ty += 10;
                g.drawString(font, line, 8, ty, 0xFFFFFF);
            }
        } else if (chosen == null) {
            g.drawCenteredString(font, Component.translatable("screen.enhanced.hero_creator.pick_hero").withStyle(ChatFormatting.GRAY),
                    width / 2, height - BOTTOM + 12, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
