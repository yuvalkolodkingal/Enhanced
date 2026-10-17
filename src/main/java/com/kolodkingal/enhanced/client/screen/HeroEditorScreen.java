package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.custom.HeroFiles;
import com.kolodkingal.enhanced.custom.CustomBeams;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.SkinCheck;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "Create my hero" (3.3, F1-F5): the editor of a custom hero, in five tabs with big buttons, and on the left a live preview
 * of the hero (his skin as a paper doll, his name, his colours as a beam would show them, and the power points he uses).
 * <ul>
 *     <li><b>Name and colours</b>: a name, and the main and glow colours (a palette and two sliders each).</li>
 *     <li><b>Powers</b>: the thirty powers of the catalogue as pictures, the points bar, and for the one clicked: pick or
 *     remove it, its option, its strength (1-10) with the numbers that strength gives, and its own colour.</li>
 *     <li><b>Keys</b>: the five ability keys, each a big button that steps through the powers that need a key.</li>
 *     <li><b>Look</b>: his own skin, the skin of a look of one of the mod's heroes (and a copy of it to paint), or a skin
 *     file (dropped on the window, picked with the system's dialog, or from {@code enhanced/skins/}).</li>
 *     <li><b>Save</b>: become the hero, export him to {@code enhanced/heroes/} and the clipboard, import one back.</li>
 * </ul>
 * The editor runs the server's rules ({@link CustomHeroRules}) with the limits the server synced, so what it shows is
 * what the server will accept; the server judges again.
 */
public class HeroEditorScreen extends Screen {
    private enum Tab {
        NAME, POWERS, KEYS, LOOK, SAVE
    }

    /** The palette of the colour pickers. */
    static final int[] PALETTE = {0xE53935, 0xFB8C00, 0xF5A623, 0xFDD835, 0x7CB342, 0x43A047, 0x00897B, 0x00ACC1,
            0x3FA9F5, 0x1E88E5, 0x3949AB, 0x8E24AA, 0xD81B60, 0xF5F5F5, 0x9E9E9E, 0x212121};

    private static final int LEFT = 78;
    private static final int TOP = 44;

    private final Screen parent;
    private CustomHero draft;
    private Tab tab = Tab.NAME;
    private PowerModule selected = PowerModule.FLIGHT;
    /** On the Look tab: which of the three kinds of look is being shown. */
    private String lookKind;
    @Nullable
    private Component status;
    private int statusColor = 0xFFFFFF;
    private long statusUntil;
    @Nullable
    private EditBox nameBox;
    private int skinPage;

    public HeroEditorScreen(Screen parent, CustomHero start) {
        super(Component.translatable("screen.enhanced.hero_creator.editor"));
        this.parent = parent;
        this.draft = start;
        this.lookKind = start.wearsSkin() ? CustomHero.LOOK_SKIN : CustomHero.LOOK_OWN.equals(start.look()) ? CustomHero.LOOK_OWN : "hero";
    }

    private CustomHeroRules.Limits limits() {
        return CustomHeroRules.Limits.current();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private void say(Component message, boolean good) {
        status = message;
        statusColor = good ? 0x7CFC7C : 0xFF7070;
        statusUntil = Util.getMillis() + 6000;
    }

    private int right() {
        return width - 8;
    }

    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        nameBox = null;
        int x0 = LEFT + 6;
        int tabW = Math.max(52, (right() - x0 - 4 * 3) / 5);
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            addRenderableWidget(new CreatorUi.BigButton(x0 + i * (tabW + 3), 20, tabW, 20,
                    Component.translatable("screen.enhanced.hero_creator.tab." + t.name().toLowerCase(java.util.Locale.ROOT)), null,
                    t == tab ? draft.color() : 0xA0A0A0, () -> {
                tab = t;
                rebuild();
            }).selected(t == tab));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(6, height - 24, LEFT - 6, 20).build());
        switch (tab) {
            case NAME -> initName(x0);
            case POWERS -> initPowers(x0);
            case KEYS -> initKeys(x0);
            case LOOK -> initLook(x0);
            case SAVE -> initSave(x0);
        }
    }

    // ---- Name and colours ----

    private void initName(int x0) {
        nameBox = new EditBox(font, x0, TOP + 12, Math.min(200, right() - x0), 20, Component.translatable("screen.enhanced.hero_creator.name"));
        nameBox.setMaxLength(CustomHero.MAX_NAME);
        nameBox.setValue(draft.name());
        nameBox.setHint(Component.translatable("screen.enhanced.hero_creator.name.hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setResponder(text -> draft = draft.withName(text));
        addRenderableWidget(nameBox);
        colourPicker(x0, TOP + 50, true);
        colourPicker(x0, TOP + 102, false);
    }

    private void colourPicker(int x0, int y, boolean main) {
        int size = Math.max(10, Math.min(16, (right() - x0) / PALETTE.length - 2));
        for (int i = 0; i < PALETTE.length; i++) {
            int rgb = PALETTE[i];
            addRenderableWidget(new CreatorUi.Swatch(x0 + i * (size + 2), y, size, rgb, c -> {
                draft = main ? draft.withColors(c, draft.glow()) : draft.withColors(draft.color(), c);
                rebuild();
            }, () -> main ? draft.color() : draft.glow()));
        }
        int[] hb = CreatorUi.hueBright(main ? draft.color() : draft.glow());
        int w = Math.min(120, (right() - x0) / 2 - 4);
        addRenderableWidget(new CreatorUi.IntSlider(x0, y + size + 4, w, 20, 0, 359, hb[0],
                v -> Component.translatable("screen.enhanced.hero_creator.hue", v), v -> setColour(main, CreatorUi.hsv(v, hb[1]))));
        addRenderableWidget(new CreatorUi.IntSlider(x0 + w + 6, y + size + 4, w, 20, 5, 100, Math.max(5, hb[1]),
                v -> Component.translatable("screen.enhanced.hero_creator.brightness", v), v -> setColour(main, CreatorUi.hsv(hb[0], v))));
    }

    private void setColour(boolean main, int rgb) {
        draft = main ? draft.withColors(rgb, draft.glow()) : draft.withColors(draft.color(), rgb);
    }

    // ---- Powers ----

    private int gridColumns(int x0) {
        return Math.max(6, Math.min(15, (right() - x0) / 22));
    }

    private void initPowers(int x0) {
        PowerModule[] all = PowerModule.values();
        int cols = gridColumns(x0);
        int gridTop = TOP + 14;
        for (int i = 0; i < all.length; i++) {
            PowerModule m = all[i];
            int x = x0 + (i % cols) * 22;
            int y = gridTop + (i / cols) * 22;
            boolean picked = draft.has(m);
            boolean allowed = limits().allows(m);
            CreatorUi.BigButton b = new CreatorUi.BigButton(x, y, 20, 20, Component.empty(), null, picked ? m.group.color : 0x505050, () -> {
                selected = m;
                rebuild();
            }).icon(CreatorUi.icon(m.icon)).selected(m == selected);
            b.active = allowed;
            b.setTooltip(Tooltip.create(Component.translatable(m.nameKey()).withStyle(s -> s.withColor(m.group.color))
                    .append("\n").append(Component.translatable(m.nameKey() + ".desc").withStyle(ChatFormatting.GRAY))));
            addRenderableWidget(b);
        }
        int rows = (all.length + cols - 1) / cols;
        int y = gridTop + rows * 22 + 16;
        PowerModule m = selected;
        CustomHero.Pick pick = draft.pick(m);
        boolean allowed = limits().allows(m);
        // Pick it or put it back.
        CreatorUi.BigButton toggle = new CreatorUi.BigButton(x0, y, 64, 20,
                Component.translatable(pick == null ? "screen.enhanced.hero_creator.add" : "screen.enhanced.hero_creator.remove"), null,
                pick == null ? 0x7CFC7C : 0xFF7070, () -> {
            if (draft.has(m)) {
                draft = draft.without(m);
            } else {
                draft = draft.with(new CustomHero.Pick(m.id, CustomHeroJson.DEFAULT_STRENGTH, m.defaultOption().id(), -1));
                autoKey(m);
            }
            rebuild();
        });
        toggle.active = allowed;
        addRenderableWidget(toggle);
        int ox = x0 + 70;
        String option = pick == null ? m.defaultOption().id() : pick.option();
        int optW = Math.min(76, (right() - ox) / Math.max(1, m.options().size()) - 3);
        for (int i = 0; i < m.options().size(); i++) {
            PowerModule.Option o = m.options().get(i);
            CreatorUi.BigButton b = new CreatorUi.BigButton(ox + i * (optW + 3), y, optW, 20, Component.translatable(m.optionKey(o.id())), null,
                    m.group.color, () -> {
                CustomHero.Pick p = draft.pick(m);
                if (p != null) {
                    draft = draft.with(p.withOption(o.id()));
                    if (o.act() == PowerModule.Act.NONE) {
                        int slot = draft.keyOf(m);
                        if (slot > 0) {
                            draft = draft.withKey(slot, "");
                        }
                    } else {
                        autoKey(m);
                    }
                    rebuild();
                }
            }).selected(o.id().equals(option));
            b.active = pick != null;
            b.setTooltip(Tooltip.create(Component.translatable(m.optionKey(o.id()) + ".desc")));
            addRenderableWidget(b);
        }
        if (pick != null) {
            int max = limits().maxStrength();
            addRenderableWidget(new CreatorUi.IntSlider(x0, y + 26, 140, 20, 1, max, Math.min(max, pick.strength()),
                    v -> Component.translatable("screen.enhanced.hero_creator.strength", v), v -> {
                CustomHero.Pick p = draft.pick(m);
                if (p != null && p.strength() != v) {
                    draft = draft.with(p.withStrength(v));
                }
            }));
            // Its own colour, or the hero's (the first swatch, framed when chosen).
            int sx = x0 + font.width(Component.translatable("screen.enhanced.hero_creator.own_color")) + 4;
            int sy = y + 52;
            int[] colours = {-1, 0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x00ACC1, 0x1E88E5, 0x8E24AA, 0xD81B60, 0xF5F5F5};
            for (int i = 0; i < colours.length; i++) {
                int c = colours[i];
                addRenderableWidget(new CreatorUi.Swatch(sx + i * 14, sy, 12, c < 0 ? draft.color() : c, picked -> {
                    CustomHero.Pick p = draft.pick(m);
                    if (p != null) {
                        draft = draft.with(p.withColor(c));
                        rebuild();
                    }
                }, () -> {
                    CustomHero.Pick p = draft.pick(m);
                    return p == null ? -2 : p.color() < 0 ? draft.color() : p.color();
                }));
            }
        }
    }

    /** A power just picked (or switched to an option with a key) goes on the first free key, if there is one. */
    private void autoKey(PowerModule m) {
        CustomHero.Pick p = draft.pick(m);
        if (p == null || p.act() == PowerModule.Act.NONE || draft.keyOf(m) > 0) {
            return;
        }
        for (int slot = 1; slot <= CustomHero.KEYS; slot++) {
            if (draft.keys().get(slot - 1).isEmpty()) {
                draft = draft.withKey(slot, m.id);
                return;
            }
        }
    }

    // ---- Keys ----

    private void initKeys(int x0) {
        List<PowerModule> withKeys = new ArrayList<>();
        for (CustomHero.Pick p : draft.powers()) {
            if (p.module() != null && p.act() != PowerModule.Act.NONE) {
                withKeys.add(p.module());
            }
        }
        int w = Math.min(220, right() - x0 - 40);
        for (int slot = 1; slot <= CustomHero.KEYS; slot++) {
            int s = slot;
            CustomHero.Pick on = draft.onKey(slot);
            PowerModule m = on == null ? null : on.module();
            Component label = m == null ? Component.translatable("screen.enhanced.hero_creator.key.nothing")
                    : Component.translatable(m.nameKey());
            Component sub = m == null ? null : Component.translatable(m.optionKey(on.option()));
            CreatorUi.BigButton b = new CreatorUi.BigButton(x0 + 36, TOP + 10 + (slot - 1) * 28, w, 24, label, sub,
                    m == null ? 0x707070 : draft.colorOf(on), () -> {
                // The next power that needs a key (and is not on another one), then nothing, then round again.
                int at = m == null ? -1 : withKeys.indexOf(m);
                String next = "";
                for (int k = at + 1; k < withKeys.size(); k++) {
                    PowerModule candidate = withKeys.get(k);
                    if (draft.keyOf(candidate) == 0) {
                        next = candidate.id;
                        break;
                    }
                }
                draft = draft.withKey(s, next);
                rebuild();
            });
            if (m != null) {
                b.icon(CreatorUi.icon(m.icon));
            }
            addRenderableWidget(b);
        }
    }

    // ---- Look ----

    private void initLook(int x0) {
        String[] kinds = {CustomHero.LOOK_OWN, "hero", CustomHero.LOOK_SKIN};
        int w = Math.min(110, (right() - x0) / 3 - 4);
        for (int i = 0; i < kinds.length; i++) {
            String kind = kinds[i];
            addRenderableWidget(new CreatorUi.BigButton(x0 + i * (w + 4), TOP + 6, w, 22,
                    Component.translatable("screen.enhanced.hero_creator.look." + kind), null, draft.color(), () -> {
                lookKind = kind;
                if (kind.equals(CustomHero.LOOK_OWN)) {
                    draft = draft.withLook(CustomHero.LOOK_OWN, false);
                } else if (kind.equals(CustomHero.LOOK_SKIN) && draft.skin().length > 0) {
                    draft = draft.withLook(CustomHero.LOOK_SKIN, draft.slim());
                }
                rebuild();
            }).selected(kind.equals(lookKind)));
        }
        int y = TOP + 34;
        if (lookKind.equals("hero")) {
            List<HeroLooks.Look> looks = HeroLooks.all();
            int cols = Math.max(6, (right() - x0) / 22);
            for (int i = 0; i < looks.size(); i++) {
                HeroLooks.Look look = looks.get(i);
                CreatorUi.BigButton b = new CreatorUi.BigButton(x0 + (i % cols) * 22, y + (i / cols) * 22, 20, 20, Component.empty(), null,
                        look.power().titleColor(), () -> {
                    draft = draft.withLook(look.ref(), look.slim());
                    rebuild();
                }).face(look.skin()).selected(look.ref().equals(draft.look()));
                b.setTooltip(Tooltip.create(Component.translatable(look.nameKey()).append(" (").append(look.power().displayName()).append(")")));
                addRenderableWidget(b);
            }
            int rows = (looks.size() + cols - 1) / cols;
            HeroLooks.Look current = HeroLooks.byRef(draft.look());
            CreatorUi.BigButton copy = new CreatorUi.BigButton(x0, y + rows * 22 + 6, Math.min(200, right() - x0), 22,
                    Component.translatable("screen.enhanced.hero_creator.look.copy"), null, draft.glow(), () -> {
                HeroLooks.Look look = HeroLooks.byRef(draft.look());
                Optional<Path> file = look == null ? Optional.empty() : HeroFiles.copyOfLook(look);
                if (file.isPresent()) {
                    say(Component.translatable("screen.enhanced.hero_creator.look.copied", file.get().getFileName().toString()), true);
                    HeroFiles.open(HeroFiles.skinsDir());
                } else {
                    say(Component.translatable("screen.enhanced.hero_creator.look.copy_failed"), false);
                }
            });
            copy.active = current != null;
            addRenderableWidget(copy);
        } else if (lookKind.equals(CustomHero.LOOK_SKIN)) {
            int bw = Math.min(118, (right() - x0) / 3 - 4);
            addRenderableWidget(new CreatorUi.BigButton(x0, y + 14, bw, 22, Component.translatable("screen.enhanced.hero_creator.look.pick"), null,
                    draft.color(), () -> HeroFiles.pickSkin(Component.translatable("screen.enhanced.hero_creator.look.pick").getString())
                    .thenAccept(CreatorUi.onRenderThread(chosen -> chosen.ifPresent(this::loadSkin)))));
            addRenderableWidget(new CreatorUi.BigButton(x0 + bw + 4, y + 14, bw, 22, Component.translatable("screen.enhanced.hero_creator.look.folder"),
                    null, draft.color(), () -> HeroFiles.open(HeroFiles.skinsDir())));
            addRenderableWidget(new CreatorUi.BigButton(x0 + 2 * (bw + 4), y + 14, bw, 22,
                    Component.translatable(draft.slim() ? "screen.enhanced.hero_creator.look.slim" : "screen.enhanced.hero_creator.look.wide"), null,
                    draft.glow(), () -> {
                draft = draft.withLook(draft.look(), !draft.slim());
                rebuild();
            }));
            List<Path> skins = HeroFiles.skins();
            int perPage = 6;
            skinPage = Math.max(0, Math.min(skinPage, (skins.size() - 1) / perPage));
            int fw = Math.min(150, (right() - x0) / 2 - 4);
            for (int i = 0; i < perPage && skinPage * perPage + i < skins.size(); i++) {
                Path file = skins.get(skinPage * perPage + i);
                addRenderableWidget(new CreatorUi.BigButton(x0 + (i % 2) * (fw + 4), y + 44 + (i / 2) * 22, fw, 20,
                        Component.literal(file.getFileName().toString()), null, 0xB0B0B0, () -> loadSkin(file)));
            }
            if (skins.size() > perPage) {
                addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                    skinPage = (skinPage + 1) % ((skins.size() + perPage - 1) / perPage);
                    rebuild();
                }).bounds(x0 + 2 * (fw + 4), y + 44, 20, 20).build());
            }
        }
    }

    /** A skin file chosen (picked, dropped or from the list): checked as the server will check it, then worn. */
    private void loadSkin(Path file) {
        byte[] bytes = HeroFiles.readSkin(file);
        SkinCheck.Verdict verdict = bytes == null ? SkinCheck.Verdict.BROKEN
                : SkinCheck.check(bytes, Math.min(limits().maxSkinBytes(), CustomHero.MAX_SKIN_WIRE));
        if (verdict != SkinCheck.Verdict.OK) {
            say(Component.translatable(verdict.messageKey(), limits().maxSkinBytes() / 1024), false);
            return;
        }
        draft = draft.withSkin(bytes).withLook(CustomHero.LOOK_SKIN, draft.slim());
        lookKind = CustomHero.LOOK_SKIN;
        say(Component.translatable("screen.enhanced.hero_creator.look.loaded", file.getFileName().toString()), true);
        rebuild();
    }

    @Override
    public void onFilesDrop(List<Path> dropped) {
        List<Path> pngs = HeroFiles.pngs(dropped);
        if (!pngs.isEmpty()) {
            tab = Tab.LOOK;
            loadSkin(pngs.get(0));
        } else if (!dropped.isEmpty() && dropped.get(0).getFileName().toString().endsWith(".json")) {
            load(HeroFiles.read(dropped.get(0)));
        }
    }

    // ---- Save ----

    private void initSave(int x0) {
        int w = Math.min(240, right() - x0);
        addRenderableWidget(new CreatorUi.BigButton(x0, TOP + 6, w, 34, Component.translatable("screen.enhanced.hero_creator.become"),
                Component.translatable("screen.enhanced.hero_creator.become.desc"), HeroCreatorScreen.GOLD, this::become)
                .icon(new ItemStack(com.kolodkingal.enhanced.registry.ModItems.HERO_CREATOR.get())));
        int bw = Math.min(110, (right() - x0) / 3 - 4);
        int y = TOP + 46;
        addRenderableWidget(new CreatorUi.BigButton(x0, y, bw, 22, Component.translatable("screen.enhanced.hero_creator.save_file"), null,
                draft.color(), this::export));
        addRenderableWidget(new CreatorUi.BigButton(x0 + bw + 4, y, bw, 22, Component.translatable("screen.enhanced.hero_creator.copy"), null,
                draft.color(), () -> {
            HeroFiles.copy(CustomHeroJson.write(sanitized().hero()));
            say(Component.translatable("screen.enhanced.hero_creator.copied"), true);
        }));
        addRenderableWidget(new CreatorUi.BigButton(x0 + 2 * (bw + 4), y, bw, 22, Component.translatable("screen.enhanced.hero_creator.paste"), null,
                draft.glow(), () -> load(CustomHeroJson.read(HeroFiles.clipboard()))));
        List<Path> files = HeroFiles.heroes();
        int fw = Math.min(150, (right() - x0) / 2 - 4);
        for (int i = 0; i < 6 && i < files.size(); i++) {
            Path file = files.get(i);
            String name = file.getFileName().toString();
            addRenderableWidget(new CreatorUi.BigButton(x0 + (i % 2) * (fw + 4), y + 40 + (i / 2) * 22, fw, 20,
                    Component.literal(name.substring(0, name.length() - 5)), null, 0xB0B0B0, () -> load(HeroFiles.read(file))));
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.enhanced.hero_creator.open_folder"), b -> HeroFiles.open(HeroFiles.heroesDir()))
                .bounds(x0, Math.min(height - 24, y + 40 + 3 * 22 + 2), Math.min(150, right() - x0), 20).build());
    }

    private CustomHeroRules.Result sanitized() {
        return CustomHeroRules.sanitize(draft, limits());
    }

    private void become() {
        PacketDistributor.sendToServer(HeroCreatorPayload.apply(draft));
        onClose();
    }

    private void export() {
        CustomHero hero = sanitized().hero();
        try {
            Path file = HeroFiles.export(hero);
            HeroFiles.copy(CustomHeroJson.write(hero));
            say(Component.translatable("screen.enhanced.hero_creator.saved", "enhanced/heroes/" + file.getFileName()), true);
            rebuild();
        } catch (IOException e) {
            say(Component.translatable("screen.enhanced.hero_creator.save_failed"), false);
        }
    }

    private void load(CustomHeroJson.Read read) {
        if (read.hero() == null) {
            say(Component.translatable(read.notes().isEmpty() ? "message.enhanced.hero_creator.import.not_a_hero" : read.notes().get(0)), false);
            return;
        }
        CustomHeroRules.Result result = CustomHeroRules.sanitize(read.hero(), limits());
        draft = read.hero();
        lookKind = draft.wearsSkin() ? CustomHero.LOOK_SKIN : CustomHero.LOOK_OWN.equals(draft.look()) ? CustomHero.LOOK_OWN : "hero";
        boolean clean = result.notes().isEmpty() && read.notes().isEmpty();
        say(Component.translatable(clean ? "screen.enhanced.hero_creator.loaded" : "screen.enhanced.hero_creator.loaded_changed",
                draft.name()), clean);
        rebuild();
    }

    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withColor(HeroCreatorScreen.GOLD).withBold(true)), width / 2, 6, 0xFFFFFF);
        renderPreview(g);
        int x0 = LEFT + 6;
        switch (tab) {
            case NAME -> {
                g.drawString(font, Component.translatable("screen.enhanced.hero_creator.name").withStyle(ChatFormatting.WHITE), x0, TOP, 0xFFFFFF);
                g.drawString(font, Component.translatable("screen.enhanced.hero_creator.color").withStyle(s -> s.withColor(draft.color())), x0, TOP + 40, 0xFFFFFF);
                g.drawString(font, Component.translatable("screen.enhanced.hero_creator.glow").withStyle(s -> s.withColor(draft.glow())), x0, TOP + 92, 0xFFFFFF);
            }
            case POWERS -> renderPowers(g, x0);
            case KEYS -> renderKeys(g, x0);
            case LOOK -> renderLook(g, x0);
            case SAVE -> renderSave(g, x0);
        }
        if (status != null && Util.getMillis() < statusUntil) {
            CreatorUi.drawFitted(g, font, status, LEFT + 6, height - 12, right() - LEFT - 6, statusColor);
        }
    }

    /** The live preview on the left: the paper doll, the name, a sample of the colours, the points used. */
    private void renderPreview(GuiGraphics g) {
        g.fill(2, 18, LEFT, height - 28, 0x60000000);
        g.renderOutline(2, 18, LEFT - 2, height - 46, 0xFF000000 | CreatorUi.darken(draft.color()));
        int scale = height >= 230 ? 2 : 1;
        int dollX = (LEFT + 2) / 2 - 8 * scale;
        int dollY = 24;
        CreatorUi.paperDoll(g, CreatorUi.lookOf(draft), dollX, dollY, scale);
        int y = dollY + 32 * scale + 4;
        String name = CustomHeroRules.cleanName(draft.name());
        Component shown = Component.literal(name.isEmpty() ? CustomHeroRules.DEFAULT_NAME : name).withStyle(s -> s.withColor(draft.color()).withBold(true));
        int w = font.width(shown);
        CreatorUi.drawFitted(g, font, shown, Math.max(4, (LEFT + 2) / 2 - w / 2), y, LEFT - 8, 0xFFFFFF);
        // The colours as his beam would show them: fringe, body, main, glow, core.
        int[] beam = {CustomBeams.mix(draft.color(), 0, 0.55F), CustomBeams.mix(draft.color(), 0, 0.3F), draft.color(), draft.glow(),
                CustomBeams.mix(draft.glow(), 0xFFFFFF, 0.65F), draft.glow(), draft.color(), CustomBeams.mix(draft.color(), 0, 0.3F),
                CustomBeams.mix(draft.color(), 0, 0.55F)};
        int bx = 8;
        int bw = (LEFT - 14) / beam.length;
        for (int i = 0; i < beam.length; i++) {
            g.fill(bx + i * bw, y + 12, bx + (i + 1) * bw, y + 18, 0xFF000000 | beam[i]);
        }
        int cost = CustomHeroRules.cost(draft);
        int budget = limits().budget();
        Component points = Component.translatable("screen.enhanced.hero_creator.points_short", cost, budget)
                .withStyle(cost > budget ? ChatFormatting.RED : ChatFormatting.GRAY);
        CreatorUi.drawFitted(g, font, points, 6, y + 22, LEFT - 10, 0xFFFFFF);
        Component count = Component.translatable("screen.enhanced.hero_creator.powers_short", draft.powers().size(), limits().maxPowers())
                .withStyle(draft.powers().size() > limits().maxPowers() ? ChatFormatting.RED : ChatFormatting.GRAY);
        CreatorUi.drawFitted(g, font, count, 6, y + 32, LEFT - 10, 0xFFFFFF);
    }

    private void renderPowers(GuiGraphics g, int x0) {
        int cost = CustomHeroRules.cost(draft);
        int budget = limits().budget();
        int barW = Math.min(200, right() - x0 - 110);
        g.drawString(font, Component.translatable("screen.enhanced.hero_creator.points", cost, budget)
                .withStyle(cost > budget ? ChatFormatting.RED : ChatFormatting.WHITE), x0, TOP + 2, 0xFFFFFF);
        int bx = x0 + 110;
        g.fill(bx, TOP + 3, bx + barW, TOP + 9, 0xFF303030);
        g.fill(bx, TOP + 3, bx + (int) (barW * Math.min(1F, cost / (float) budget)), TOP + 9, cost > budget ? 0xFFE53935 : 0xFF000000 | draft.color());
        int cols = gridColumns(x0);
        int rows = (PowerModule.values().length + cols - 1) / cols;
        int y = TOP + 14 + rows * 22 + 2;
        PowerModule m = selected;
        CustomHero.Pick pick = draft.pick(m);
        MutableComponent name = Component.translatable(m.nameKey()).withStyle(s -> s.withColor(m.group.color).withBold(true));
        if (!limits().allows(m)) {
            name.append(Component.literal(" ")).append(Component.translatable("screen.enhanced.hero_creator.disabled_power").withStyle(ChatFormatting.RED));
        }
        g.drawString(font, name, x0, y, 0xFFFFFF);
        CreatorUi.drawFitted(g, font, Component.translatable(m.nameKey() + ".desc").withStyle(ChatFormatting.GRAY), x0 + font.width(name) + 8, y,
                right() - x0 - font.width(name) - 8, 0xFFFFFF);
        int strength = pick == null ? CustomHeroJson.DEFAULT_STRENGTH : pick.strength();
        String option = pick == null ? m.defaultOption().id() : pick.option();
        // The numbers this strength gives (the live preview of F5), and what it costs.
        int sx = x0 + 150;
        int sy = y + 40;
        int colW = Math.max(40, (right() - sx) / 2 - 3);
        g.drawString(font, Component.translatable("screen.enhanced.hero_creator.costs", m.cost(strength))
                .withStyle(ChatFormatting.YELLOW), sx, sy, 0xFFFFFF);
        PowerModule.Act act = m.act(option);
        CreatorUi.drawFitted(g, font, Component.translatable("screen.enhanced.hero_creator.act." + act.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.AQUA), sx + colW + 4, sy, colW, 0xFFFFFF);
        int line = 0;
        for (ModuleStats.Stat stat : ModuleStats.of(m, option)) {
            Component text = Component.translatable(stat.nameKey()).withStyle(ChatFormatting.GRAY).append(": ")
                    .append(Component.translatable(stat.unitKey(), stat.shown(strength)).withStyle(s -> s.withColor(draft.colorOf(pick))));
            int col = line % 2;
            int row = line / 2;
            CreatorUi.drawFitted(g, font, text, sx + col * (colW + 4), sy + 12 + row * 10, colW, 0xFFFFFF);
            line++;
        }
        if (pick != null) {
            g.drawString(font, Component.translatable("screen.enhanced.hero_creator.own_color").withStyle(ChatFormatting.GRAY), x0, y + 68, 0xFFFFFF);
        }
    }

    private void renderKeys(GuiGraphics g, int x0) {
        g.drawString(font, Component.translatable("screen.enhanced.hero_creator.keys.hint").withStyle(ChatFormatting.GRAY), x0, TOP, 0xFFFFFF);
        for (int slot = 1; slot <= CustomHero.KEYS; slot++) {
            Component key = Component.literal("[").append(KeyBindings.ability(slot).getTranslatedKeyMessage()).append("]")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            g.drawString(font, key, x0, TOP + 18 + (slot - 1) * 28, 0xFFFFFF);
        }
        List<Component> passive = new ArrayList<>();
        for (CustomHero.Pick p : draft.powers()) {
            if (p.module() != null && draft.keyOf(p.module()) == 0) {
                passive.add(Component.translatable(p.module().nameKey()));
            }
        }
        if (!passive.isEmpty()) {
            MutableComponent text = Component.translatable("hud.enhanced.custom.always").withStyle(ChatFormatting.GRAY).append(" ");
            for (int i = 0; i < passive.size(); i++) {
                text.append(i == 0 ? Component.empty() : Component.literal(", ")).append(passive.get(i).copy().withStyle(s -> s.withColor(draft.glow())));
            }
            CreatorUi.drawFitted(g, font, text, x0, TOP + 12 + 5 * 28, right() - x0, 0xFFFFFF);
        }
    }

    private void renderLook(GuiGraphics g, int x0) {
        if (lookKind.equals(CustomHero.LOOK_OWN)) {
            g.drawString(font, Component.translatable("screen.enhanced.hero_creator.look.own.desc").withStyle(ChatFormatting.GRAY), x0, TOP + 36, 0xFFFFFF);
        } else if (lookKind.equals(CustomHero.LOOK_SKIN)) {
            Component text = Component.translatable("screen.enhanced.hero_creator.look.drop", limits().maxSkinBytes() / 1024)
                    .withStyle(ChatFormatting.GRAY);
            CreatorUi.drawFitted(g, font, text, x0, TOP + 36, right() - x0, 0xFFFFFF);
        }
    }

    private void renderSave(GuiGraphics g, int x0) {
        g.drawString(font, Component.translatable("screen.enhanced.hero_creator.files").withStyle(ChatFormatting.GRAY), x0, TOP + 76, 0xFFFFFF);
        List<CustomHeroRules.Note> notes = sanitized().notes();
        int y = TOP + 46 + 40 + 3 * 22 + 26;
        for (int i = 0; i < notes.size() && y < height - 16; i++, y += 10) {
            CreatorUi.drawFitted(g, font, CustomHeroes.note(notes.get(i)).withStyle(ChatFormatting.GOLD), x0, y, right() - x0, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
