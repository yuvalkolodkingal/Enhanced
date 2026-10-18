package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.custom.PresetBook;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.custom.HeroPresets;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * The Start tab (3.4): start from one of the heroes ("Thor-like": his own abilities, his look, cape and moves, in 60
 * points), one of six archetypes, or an empty hero. Picking one replaces the hero being made, after a "sure?" when it has
 * powers already.
 */
public final class StartTab extends EditorTab {

    public StartTab(HeroEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int x, int y, int w, int h) {
        List<Card> cards = new ArrayList<>();
        for (HeroPresets.Preset preset : PresetBook.all()) {
            int color = preset.hero().color();
            Card card = Card.of(Component.translatable(preset.nameKey()), color, false, () -> choose(preset))
                    .tooltip(Component.translatable(preset.descKey()));
            if (preset.like() != Power.NONE) {
                card = card.face(face(preset.like()));
            } else {
                card = card.icon(CreatorUi.icon(preset.icon()));
            }
            cards.add(card);
        }
        cards.add(Card.of(Component.translatable("screen.enhanced.hero_creator.start.empty"), 0xB0B0B0, false, this::empty)
                .icon(CreatorUi.icon("minecraft:paper")).tooltip(Component.translatable("screen.enhanced.hero_creator.start.empty.desc")));
        cards("start", cards, x, y + 14, w, h - 14, 86, 20);
    }

    /** The face of a hero's first look. */
    static ResourceLocation face(Power hero) {
        List<HeroLooks.Look> looks = HeroLooks.of(hero);
        return looks.isEmpty() ? null : looks.get(0).skin();
    }

    private void choose(HeroPresets.Preset preset) {
        Runnable apply = () -> {
            String name = Component.translatable(preset.nameKey()).getString();
            CustomHero hero = CustomHeroRules.sanitize(preset.hero(), limits()).hero().withName(name);
            screen.replaceDraft(hero);
            screen.say(Component.translatable("screen.enhanced.hero_creator.start.done", Component.translatable(preset.nameKey())), true);
        };
        confirm(Component.translatable(preset.nameKey()), apply);
    }

    private void empty() {
        confirm(Component.translatable("screen.enhanced.hero_creator.start.empty"), () -> screen.replaceDraft(CustomHero.EMPTY));
    }

    private void confirm(Component what, Runnable apply) {
        if (!screen.hasWork()) {
            apply.run();
            return;
        }
        screen.ask(new Popup(Component.translatable("screen.enhanced.hero_creator.start.sure", what), List.of(
                Popup.Item.of(Component.translatable("screen.enhanced.hero_creator.yes"), null, 0x7CFC7C, false, apply),
                Popup.Item.of(Component.translatable("screen.enhanced.hero_creator.no"), null, 0xFF7070, false, () -> { }))));
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        text(g, Component.translatable("screen.enhanced.hero_creator.start.hint").withStyle(ChatFormatting.GRAY), x, y + 2, w, 0xFFFFFF);
    }
}
