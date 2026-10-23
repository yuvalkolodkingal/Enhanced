package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.custom.HeroFiles;
import com.kolodkingal.enhanced.client.screen.HeroCreatorScreen;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The Save tab (3.3, 3.4): become the hero now; save him to {@code enhanced/heroes/} (and the clipboard), copy him, import
 * one from the clipboard, or load one of the saved heroes; and, in plain words, what the server's rules would change.
 */
public final class SaveTab extends EditorTab {
    private int notesY;

    public SaveTab(HeroEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int x, int y, int w, int h) {
        add(new CreatorUi.BigButton(x, y, Math.min(260, w), 32, Component.translatable("screen.enhanced.hero_creator.become"),
                Component.translatable("screen.enhanced.hero_creator.become.desc"), HeroCreatorScreen.GOLD, this::become)
                .icon(new ItemStack(ModItems.HERO_CREATOR.get())));
        int bw = (w - 2 * GAP) / 3;
        int yy = y + 36;
        add(new CreatorUi.BigButton(x, yy, bw, 20, Component.translatable("screen.enhanced.hero_creator.save_file"), null, draft().color(),
                this::export));
        add(new CreatorUi.BigButton(x + bw + GAP, yy, bw, 20, Component.translatable("screen.enhanced.hero_creator.copy"), null, draft().color(), () -> {
            HeroFiles.copy(screen.json());
            screen.say(Component.translatable("screen.enhanced.hero_creator.copied"), true);
        }));
        add(new CreatorUi.BigButton(x + 2 * (bw + GAP), yy, bw, 20, Component.translatable("screen.enhanced.hero_creator.paste"), null,
                draft().glow(), () -> load(CustomHeroJson.read(HeroFiles.clipboard()))));
        yy += 34;
        List<Card> files = new ArrayList<>();
        for (Path file : HeroFiles.heroes()) {
            String name = file.getFileName().toString();
            files.add(Card.of(Component.literal(name.substring(0, name.length() - 5)), 0xB0B0B0, false, () -> load(HeroFiles.read(file))));
        }
        int listH = Math.min(3 * (18 + GAP) + 18, Math.max(40, h / 3));
        cards("files", files, x, yy, w, listH, 110, 18);
        yy += listH + 2;
        add(new CreatorUi.BigButton(x, yy, Math.min(150, w), 16, Component.translatable("screen.enhanced.hero_creator.open_folder"), null, 0xB0B0B0,
                () -> HeroFiles.open(HeroFiles.heroesDir())));
        notesY = yy + 20;
    }

    private void become() {
        Net.sendToServer(HeroCreatorPayload.apply(draft()));
        screen.closeAll();
    }

    private void export() {
        CustomHero hero = CustomHeroRules.sanitize(draft(), limits()).hero();
        try {
            Path file = HeroFiles.export(hero);
            HeroFiles.copy(CustomHeroJson.write(hero));
            screen.say(Component.translatable("screen.enhanced.hero_creator.saved", "enhanced/heroes/" + file.getFileName()), true);
            screen.markSaved();
            screen.rebuild();
        } catch (IOException e) {
            screen.say(Component.translatable("screen.enhanced.hero_creator.save_failed"), false);
        }
    }

    /** A hero read from a file or the clipboard becomes the draft (what does not fit here is said). */
    public void load(CustomHeroJson.Read read) {
        if (read.hero() == null) {
            screen.say(Component.translatable(read.notes().isEmpty() ? "message.enhanced.hero_creator.import.not_a_hero" : read.notes().get(0)), false);
            return;
        }
        CustomHeroRules.Result result = CustomHeroRules.sanitize(read.hero(), limits());
        screen.replaceDraft(read.hero());
        screen.markSaved();
        boolean clean = result.notes().isEmpty() && read.notes().isEmpty();
        screen.say(Component.translatable(clean ? "screen.enhanced.hero_creator.loaded" : "screen.enhanced.hero_creator.loaded_changed",
                read.hero().name()), clean);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        text(g, Component.translatable("screen.enhanced.hero_creator.files").withStyle(ChatFormatting.GRAY), x, y + 60, w, 0xFFFFFF);
        List<CustomHeroRules.Note> notes = CustomHeroRules.sanitize(draft(), limits()).notes();
        int yy = notesY;
        if (notes.isEmpty()) {
            text(g, Component.translatable("screen.enhanced.hero_creator.all_fits").withStyle(ChatFormatting.GREEN), x, yy, w, 0xFFFFFF);
            return;
        }
        text(g, Component.translatable("screen.enhanced.hero_creator.will_change").withStyle(ChatFormatting.GOLD), x, yy, w, 0xFFFFFF);
        yy += 10;
        for (CustomHeroRules.Note note : notes) {
            for (FormattedCharSequence line : font().split(CustomHeroes.note(note).withStyle(ChatFormatting.YELLOW), w)) {
                if (yy > y + h - 9) {
                    return;
                }
                g.drawString(font(), line, x, yy, 0xFFFFFF, true);
                yy += 9;
            }
        }
    }
}
