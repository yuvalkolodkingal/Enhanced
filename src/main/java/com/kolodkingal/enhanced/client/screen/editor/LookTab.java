package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.custom.CustomLooks;
import com.kolodkingal.enhanced.client.custom.HeroFiles;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.custom.LookCatalog;
import com.kolodkingal.enhanced.custom.SkinCheck;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Look tab (3.4): in seven parts. <b>Skin</b>: the player's own, the skin of a look of one of the mod's heroes (and a
 * copy of it to paint), or a skin file (dropped on the window, picked, or from {@code enhanced/skins/}), with slim or
 * wide arms. <b>Name and colours</b>: the name, the main and glow colours. Then the five layers, <b>Cape</b>, <b>Hat</b>,
 * <b>Visor</b>, <b>Eyes</b> and <b>Aura</b>: a button for each style, a palette where the style takes a colour (the first
 * swatch is the hero's own colour), for the eyes the row of the face they sit on, and for the eyes and the aura when
 * they show (always, while a power is on, while flying). The preview shows every layer whatever its "when".
 */
public final class LookTab extends EditorTab {
    private static final String[] PARTS = {"skin", "name", "cape", "hat", "visor", "eyes", "aura"};

    private String part = "skin";
    /** On the skin part: which of the three kinds of look is shown. */
    private String lookKind;
    private int skinPage;

    public LookTab(HeroEditorScreen screen) {
        super(screen);
        CustomHero start = screen.draft();
        lookKind = start.wearsSkin() ? CustomHero.LOOK_SKIN : CustomHero.LOOK_OWN.equals(start.look()) ? CustomHero.LOOK_OWN : "hero";
    }

    @Override
    public void init(int x, int y, int w, int h) {
        int chipW = (w - (PARTS.length - 1) * 2) / PARTS.length;
        for (int i = 0; i < PARTS.length; i++) {
            String p = PARTS[i];
            add(new CreatorUi.BigButton(x + i * (chipW + 2), y, chipW, 16, Component.translatable("screen.enhanced.hero_creator.look.part." + p),
                    null, draft().color(), () -> {
                part = p;
                screen.rebuild();
            }).selected(p.equals(part)));
        }
        int top = y + 20;
        switch (part) {
            case "skin" -> initSkin(x, top, w, h - 20);
            case "name" -> initName(x, top, w);
            default -> initLayer(x, top, w, h - 20, List.of(PARTS).indexOf(part) - 2);
        }
    }

    // ---- the skin ----

    private void initSkin(int x, int y, int w, int h) {
        if (!lookKind.equals("hero") && !draft().wearsSkin() && !CustomHero.LOOK_OWN.equals(draft().look())) {
            lookKind = "hero";
        }
        String[] kinds = {CustomHero.LOOK_OWN, "hero", CustomHero.LOOK_SKIN};
        int kw = (w - 2 * GAP) / 3;
        for (int i = 0; i < kinds.length; i++) {
            String kind = kinds[i];
            add(new CreatorUi.BigButton(x + i * (kw + GAP), y, kw, 20, Component.translatable("screen.enhanced.hero_creator.look." + kind), null,
                    draft().color(), () -> {
                lookKind = kind;
                if (kind.equals(CustomHero.LOOK_OWN)) {
                    set(draft().withLook(CustomHero.LOOK_OWN, false));
                } else if (kind.equals(CustomHero.LOOK_SKIN) && draft().skin().length > 0) {
                    set(draft().withLook(CustomHero.LOOK_SKIN, draft().slim()));
                } else {
                    screen.rebuild();
                }
            }).selected(kind.equals(lookKind)));
        }
        int yy = y + 36;
        if (lookKind.equals("hero")) {
            List<Card> cards = new ArrayList<>();
            for (HeroLooks.Look look : HeroLooks.all()) {
                cards.add(new Card(Component.empty(), null, null, look.skin(), look.power().titleColor(), look.ref().equals(draft().look()), true,
                        Component.translatable(look.nameKey()).append(" (").append(look.power().displayName()).append(")"),
                        () -> set(draft().withLook(look.ref(), look.slim()))));
            }
            yy = flow(cards, x, yy, w, 20, 20) + 2;
            HeroLooks.Look current = HeroLooks.byRef(draft().look());
            CreatorUi.BigButton copy = add(new CreatorUi.BigButton(x, yy, Math.min(200, w), 20,
                    Component.translatable("screen.enhanced.hero_creator.look.copy"), null, draft().glow(), () -> {
                HeroLooks.Look look = HeroLooks.byRef(draft().look());
                Optional<Path> file = look == null ? Optional.empty() : HeroFiles.copyOfLook(look);
                if (file.isPresent()) {
                    screen.say(Component.translatable("screen.enhanced.hero_creator.look.copied", file.get().getFileName().toString()), true);
                    HeroFiles.open(HeroFiles.skinsDir());
                } else {
                    screen.say(Component.translatable("screen.enhanced.hero_creator.look.copy_failed"), false);
                }
            }));
            copy.active = current != null;
        } else if (lookKind.equals(CustomHero.LOOK_SKIN)) {
            int bw = (w - 2 * GAP) / 3;
            add(new CreatorUi.BigButton(x, yy + 4, bw, 20, Component.translatable("screen.enhanced.hero_creator.look.pick"), null, draft().color(),
                    () -> HeroFiles.pickSkin(Component.translatable("screen.enhanced.hero_creator.look.pick").getString())
                            .thenAccept(CreatorUi.onRenderThread(chosen -> chosen.ifPresent(this::loadSkin)))));
            add(new CreatorUi.BigButton(x + bw + GAP, yy + 4, bw, 20, Component.translatable("screen.enhanced.hero_creator.look.folder"), null,
                    draft().color(), () -> HeroFiles.open(HeroFiles.skinsDir())));
            add(new CreatorUi.BigButton(x + 2 * (bw + GAP), yy + 4, bw, 20,
                    Component.translatable(draft().slim() ? "screen.enhanced.hero_creator.look.slim" : "screen.enhanced.hero_creator.look.wide"),
                    null, draft().glow(), () -> set(draft().withLook(draft().look(), !draft().slim()))));
            List<Path> skins = HeroFiles.skins();
            List<Card> files = new ArrayList<>();
            for (Path file : skins) {
                files.add(Card.of(Component.literal(file.getFileName().toString()), 0xB0B0B0, false, () -> loadSkin(file)));
            }
            cards("skins", files, x, yy + 28, w, y + h - (yy + 28), 120, 18);
        }
    }

    /** A skin file chosen (picked, dropped or from the list): checked as the server will check it, then worn. */
    public void loadSkin(Path file) {
        byte[] bytes = HeroFiles.readSkin(file);
        SkinCheck.Verdict verdict = bytes == null ? SkinCheck.Verdict.BROKEN
                : SkinCheck.check(bytes, Math.min(limits().maxSkinBytes(), CustomHero.MAX_SKIN_WIRE));
        if (verdict != SkinCheck.Verdict.OK) {
            screen.say(Component.translatable(verdict.messageKey(), limits().maxSkinBytes() / 1024), false);
            return;
        }
        lookKind = CustomHero.LOOK_SKIN;
        part = "skin";
        set(draft().withSkin(bytes).withLook(CustomHero.LOOK_SKIN, draft().slim()));
        screen.say(Component.translatable("screen.enhanced.hero_creator.look.loaded", file.getFileName().toString()), true);
    }

    // ---- the name and colours ----

    private void initName(int x, int y, int w) {
        EditBox name = add(new EditBox(font(), x, y + 10, Math.min(200, w), 18, Component.translatable("screen.enhanced.hero_creator.name")));
        name.setMaxLength(CustomHero.MAX_NAME);
        name.setValue(draft().name());
        name.setHint(Component.translatable("screen.enhanced.hero_creator.name.hint").withStyle(ChatFormatting.DARK_GRAY));
        name.setResponder(text -> screen.update(draft().withName(text)));
        colourPicker(x, y + 44, w, true);
        colourPicker(x, y + 94, w, false);
    }

    private void colourPicker(int x, int y, int w, boolean main) {
        int size = Math.max(10, Math.min(16, w / HeroEditorScreen.PALETTE.length - 2));
        for (int i = 0; i < HeroEditorScreen.PALETTE.length; i++) {
            int rgb = HeroEditorScreen.PALETTE[i];
            add(new CreatorUi.Swatch(x + i * (size + 2), y, size, rgb, c -> set(main ? draft().withColors(c, draft().glow())
                    : draft().withColors(draft().color(), c)), () -> main ? draft().color() : draft().glow()));
        }
        int[] hb = CreatorUi.hueBright(main ? draft().color() : draft().glow());
        int sw = Math.min(120, w / 2 - 4);
        add(new CreatorUi.IntSlider(x, y + size + 4, sw, 18, 0, 359, hb[0], v -> Component.translatable("screen.enhanced.hero_creator.hue", v),
                v -> screen.update(main ? draft().withColors(CreatorUi.hsv(v, hb[1]), draft().glow()) : draft().withColors(draft().color(), CreatorUi.hsv(v, hb[1])))));
        add(new CreatorUi.IntSlider(x + sw + 6, y + size + 4, sw, 18, 5, 100, Math.max(5, hb[1]),
                v -> Component.translatable("screen.enhanced.hero_creator.brightness", v),
                v -> screen.update(main ? draft().withColors(CreatorUi.hsv(hb[0], v), draft().glow()) : draft().withColors(draft().color(), CreatorUi.hsv(hb[0], v)))));
    }

    // ---- the layers ----

    private void initLayer(int x, int y, int w, int h, int kind) {
        if (!limits().layers()) {
            return;
        }
        CustomHero.Layer layer = draft().layers().get(kind);
        List<Card> styles = new ArrayList<>();
        for (String style : LookCatalog.styles(kind)) {
            styles.add(Card.of(styleName(kind, style), CustomHero.NONE.equals(style) ? 0x909090 : draft().glow(), style.equals(layer.style()),
                    () -> setLayer(kind, new CustomHero.Layer(style, layer.color(), layer.row(), layer.when())))
                    .icon(CreatorUi.icon(icon(kind, style))));
        }
        int yy = flow(styles, x, y, w, Math.max(70, (w - 2 * GAP) / 3), 18) + 4;
        if (layer.isNone()) {
            return;
        }
        if (LookCatalog.TINTED.contains(layer.style())) {
            int size = Math.max(10, Math.min(14, w / (HeroEditorScreen.PALETTE.length + 1) - 2));
            int lx = x + Math.min(w / 4, font().width(Component.translatable("screen.enhanced.hero_creator.look.color")) + 4);
            List<Integer> colours = new ArrayList<>();
            colours.add(-1);
            for (int c : HeroEditorScreen.PALETTE) {
                colours.add(c);
            }
            int per = Math.max(1, (x + w - lx) / (size + 2));
            for (int i = 0; i < colours.size(); i++) {
                int c = colours.get(i);
                CreatorUi.Swatch s = add(new CreatorUi.Swatch(lx + (i % per) * (size + 2), yy + (i / per) * (size + 2), size, c < 0 ? draft().color() : c,
                        chosen -> setLayer(kind, new CustomHero.Layer(layer.style(), c, layer.row(), layer.when())),
                        () -> draft().layers().get(kind).color() < 0 ? draft().color() : draft().layers().get(kind).color()));
                if (c < 0) {
                    s.setTooltip(Tooltip.create(Component.translatable("screen.enhanced.hero_creator.look.hero_color")));
                }
            }
            yy += ((colours.size() + per - 1) / per) * (size + 2) + 4;
        }
        if (kind == CustomLooks.EYES) {
            int bw = 20;
            int lx = x + Math.min(w / 3, font().width(Component.translatable("screen.enhanced.hero_creator.look.row", 4)) + 6);
            CreatorUi.BigButton up = add(new CreatorUi.BigButton(lx, yy, bw, 16, Component.literal("▲"), null, draft().glow(),
                    () -> setLayer(kind, new CustomHero.Layer(layer.style(), layer.color(), layer.row() - 1, layer.when()))));
            up.active = layer.row() > 0;
            CreatorUi.BigButton down = add(new CreatorUi.BigButton(lx + bw + GAP, yy, bw, 16, Component.literal("▼"), null, draft().glow(),
                    () -> setLayer(kind, new CustomHero.Layer(layer.style(), layer.color(), layer.row() + 1, layer.when()))));
            down.active = layer.row() < LookCatalog.EYE_ROWS - 1;
            yy += 20;
        }
        if (LookCatalog.hasWhen(kind)) {
            List<Card> whens = new ArrayList<>();
            for (String when : LookCatalog.WHEN) {
                whens.add(Card.of(Component.translatable("screen.enhanced.hero_creator.look.when." + when), draft().glow(), when.equals(layer.when()),
                        () -> setLayer(kind, new CustomHero.Layer(layer.style(), layer.color(), layer.row(), when))));
            }
            flow(whens, x, yy + 10, w, Math.max(60, (w - 2 * GAP) / 3), 18);
        }
    }

    private void setLayer(int kind, CustomHero.Layer layer) {
        set(draft().withLayers(draft().layers().with(kind, layer)));
    }

    /** The name of a layer's style ({@code look.enhanced.<kind>.<style>}). */
    public static Component styleName(int kind, String style) {
        return Component.translatable("look.enhanced." + CustomHero.Layers.KINDS.get(kind) + "." + style);
    }

    /** The picture of a layer's style (an item). */
    private static String icon(int kind, String style) {
        if (CustomHero.NONE.equals(style)) {
            return "minecraft:barrier";
        }
        return switch (kind) {
            case 0 -> style.equals("plain") ? "minecraft:white_banner" : style.startsWith("thor") ? "minecraft:red_banner"
                    : style.startsWith("loki") ? "minecraft:green_banner" : style.contains("vision") ? "minecraft:yellow_banner"
                    : style.startsWith("billy") ? "minecraft:light_blue_banner" : style.equals("apocalypse") ? "minecraft:blue_banner"
                    : style.equals("magneto") ? "minecraft:purple_banner" : "minecraft:red_banner";
            case 1 -> switch (style) {
                case "witch_hat" -> "minecraft:black_wool";
                case "billy_crown", "wanda_crown" -> "minecraft:golden_helmet";
                case "cerebro" -> "minecraft:chainmail_helmet";
                default -> "enhanced:magneto_helmet";
            };
            case 2 -> switch (style) {
                case "quicksilver_goggles" -> "enhanced:quicksilver_goggles";
                case "domino_mask", "full_mask" -> "minecraft:leather_helmet";
                default -> "enhanced:ruby_quartz_glasses";
            };
            case 3 -> switch (style) {
                case "lightning" -> "minecraft:lightning_rod";
                case "fire" -> "minecraft:blaze_powder";
                case "optic" -> "minecraft:redstone";
                case "machine" -> "minecraft:observer";
                case "black" -> "minecraft:ink_sac";
                default -> "minecraft:glowstone_dust";
            };
            default -> switch (style) {
                case "flames" -> "minecraft:blaze_powder";
                case "sand" -> "minecraft:sand";
                case "sparks" -> "minecraft:lightning_rod";
                case "mist" -> "minecraft:white_dye";
                default -> "minecraft:nether_star";
            };
        };
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        int top = y + 20;
        switch (part) {
            case "skin" -> {
                if (lookKind.equals(CustomHero.LOOK_OWN)) {
                    text(g, Component.translatable("screen.enhanced.hero_creator.look.own.desc").withStyle(ChatFormatting.GRAY), x, top + 26, w, 0xFFFFFF);
                } else if (lookKind.equals(CustomHero.LOOK_SKIN)) {
                    text(g, Component.translatable("screen.enhanced.hero_creator.look.drop", limits().maxSkinBytes() / 1024)
                            .withStyle(ChatFormatting.GRAY), x, top + 26, w, 0xFFFFFF);
                } else {
                    text(g, Component.translatable("screen.enhanced.hero_creator.look.hero.desc").withStyle(ChatFormatting.GRAY), x, top + 24, w,
                            0xFFFFFF);
                }
            }
            case "name" -> {
                text(g, Component.translatable("screen.enhanced.hero_creator.name").withStyle(ChatFormatting.WHITE), x, top, w, 0xFFFFFF);
                text(g, Component.translatable("screen.enhanced.hero_creator.color").withStyle(s -> s.withColor(draft().color())), x, top + 34, w,
                        0xFFFFFF);
                text(g, Component.translatable("screen.enhanced.hero_creator.glow").withStyle(s -> s.withColor(draft().glow())), x, top + 84, w,
                        0xFFFFFF);
            }
            default -> {
                int kind = List.of(PARTS).indexOf(part) - 2;
                if (!limits().layers()) {
                    text(g, Component.translatable("screen.enhanced.hero_creator.look.layers_off").withStyle(ChatFormatting.RED), x, top, w, 0xFFFFFF);
                    return;
                }
                renderLayerWords(g, x, top, w, kind);
            }
        }
    }

    private void renderLayerWords(GuiGraphics g, int x, int y, int w, int kind) {
        CustomHero.Layer layer = draft().layers().get(kind);
        if (layer.isNone()) {
            return;
        }
        int rows = (LookCatalog.styles(kind).size() + Math.max(1, (w + GAP) / (Math.max(70, (w - 2 * GAP) / 3) + GAP)) - 1)
                / Math.max(1, (w + GAP) / (Math.max(70, (w - 2 * GAP) / 3) + GAP));
        int yy = y + rows * (18 + GAP) + 4;
        if (LookCatalog.TINTED.contains(layer.style())) {
            text(g, Component.translatable("screen.enhanced.hero_creator.look.color").withStyle(ChatFormatting.GRAY), x, yy + 2, w / 4, 0xFFFFFF);
            int size = Math.max(10, Math.min(14, w / (HeroEditorScreen.PALETTE.length + 1) - 2));
            int lx = x + Math.min(w / 4, font().width(Component.translatable("screen.enhanced.hero_creator.look.color")) + 4);
            int per = Math.max(1, (x + w - lx) / (size + 2));
            yy += ((HeroEditorScreen.PALETTE.length + 1 + per - 1) / per) * (size + 2) + 4;
        }
        if (kind == CustomLooks.EYES) {
            text(g, Component.translatable("screen.enhanced.hero_creator.look.row", layer.row() + 1).withStyle(ChatFormatting.GRAY), x, yy + 4, w / 3,
                    0xFFFFFF);
            yy += 20;
        }
        if (LookCatalog.hasWhen(kind)) {
            text(g, Component.translatable("screen.enhanced.hero_creator.look.when").withStyle(ChatFormatting.GRAY), x, yy, w, 0xFFFFFF);
        }
    }
}
