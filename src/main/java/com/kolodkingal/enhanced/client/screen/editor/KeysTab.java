package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.client.build.BuildKeys;
import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.KeyLayout;
import com.kolodkingal.enhanced.custom.PowerModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Keys tab (3.4): the five ability keys (by the player's own key names) in two rows, pressed and with Sneak, like a
 * built-in hero's ten moves. A box shows what it does with its picture and how it acts; a click opens the list of what
 * fits it by the rules (moving a power off the box it was on), or "Empty". A hero's ability with its Sneak move has that
 * move on the Sneak box under it, joined to it. Auto lays the keys out like a built-in hero's ({@link KeyLayout}); Try
 * plays the box last clicked on the preview. The powers that need no key are listed under the boxes.
 */
public final class KeysTab extends EditorTab {
    private int column = 1;
    private boolean sneak;
    private int gridX;
    private int gridY;
    private int cellW;
    private int cellH;
    private int labelW;

    public KeysTab(HeroEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int x, int y, int w, int h) {
        labelW = Math.min(52, w / 6);
        gridX = x + labelW + 2;
        gridY = y + 24;
        cellW = (w - labelW - 2 - 4 * GAP) / CustomHero.KEYS;
        cellH = Math.min(38, (h - 24 - 50) / 2);
        CustomHero hero = draft();
        for (int row = 0; row < 2; row++) {
            boolean s = row == 1;
            for (int c = 1; c <= CustomHero.KEYS; c++) {
                int col = c;
                Bound b = bound(hero, c, s);
                boolean linked = s && b != null && b.alt();
                Component label = b == null ? Component.translatable("screen.enhanced.hero_creator.key.empty") : b.name();
                Component sub = b == null ? null : linked ? Component.translatable("screen.enhanced.hero_creator.key.linked")
                        : CreatorUi.badge(b.act());
                int color = b == null ? 0x606060 : hero.colorOf(hero.pick(b.entry()));
                int cx = gridX + (c - 1) * (cellW + GAP);
                int cy = gridY + row * (cellH + GAP);
                if (b != null && hero.pick(b.entry()) != null) {
                    // 3.5: the box's colour button (added first: it is clicked before the box under it).
                    CatalogEntry picked = b.entry();
                    add(new CreatorUi.Swatch(cx + cellW - 10, cy + 2, 8, color, chosen
                            -> ColorPickerScreen.open(screen, picked), () -> -2))
                            .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                                    "screen.enhanced.hero_creator.color.button.desc", CreatorUi.name(picked))));
                }
                CreatorUi.BigButton cell = add(new CreatorUi.BigButton(gridX + (c - 1) * (cellW + GAP), gridY + row * (cellH + GAP + (s ? 0 : 0)),
                        cellW, cellH, label, sub, color, () -> click(col, s)).selected(col == column && s == sneak).stacked());
                if (b != null) {
                    cell.icon(CreatorUi.icon(b.entry().icon()));
                }
            }
        }
        int by = gridY + 2 * (cellH + GAP) + 2;
        int bw = Math.min(110, (w - 2 * GAP) / 3);
        add(new CreatorUi.BigButton(x, by, bw, 20, Component.translatable("screen.enhanced.hero_creator.auto"), null, 0xF5A623, () -> {
            set(KeyLayout.auto(draft(), limits()));
            screen.say(Component.translatable("screen.enhanced.hero_creator.auto.done"), true);
        })).setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.enhanced.hero_creator.auto.desc")));
        CreatorUi.BigButton tryIt = add(new CreatorUi.BigButton(x + bw + GAP, by, bw, 20, Component.translatable("screen.enhanced.hero_creator.try_key",
                key(column, sneak)), null, draft().glow(), () -> screen.tryBinding(column, sneak)));
        tryIt.active = screen.puppet() != null && bound(hero, column, sneak) != null;
        CreatorUi.BigButton clear = add(new CreatorUi.BigButton(x + 2 * (bw + GAP), by, bw, 20, Component.translatable("screen.enhanced.hero_creator.key.clear"),
                null, 0xFF7070, () -> set(draft().withBinding(column, sneak, ""))));
        clear.active = bound(hero, column, sneak) != null && !(sneak && bound(hero, column, true).alt());
    }

    /** What a box holds (as the rules leave it), or null. */
    private record Bound(CatalogEntry entry, String action, boolean alt, PowerModule.Act act, MutableComponent name) {
    }

    @Nullable
    private Bound bound(CustomHero hero, int column, boolean onSneak) {
        String text = ruled(hero).binding(column, onSneak);
        if (text.isEmpty()) {
            return null;
        }
        int hash = text.indexOf('#');
        String id = hash < 0 ? text : text.substring(0, hash);
        String action = hash < 0 ? HeroKit.MAIN : text.substring(hash + 1);
        CatalogEntry entry = com.kolodkingal.enhanced.custom.Catalog.byId(id);
        if (entry == null) {
            return null;
        }
        if (entry instanceof HeroAbility a) {
            boolean alt = HeroKit.ALT.equals(action);
            return new Bound(entry, action, alt, alt ? a.alt() : a.act(), alt ? CreatorUi.altName(a) : CreatorUi.name(a));
        }
        PowerModule m = (PowerModule) entry;
        CustomHero.Pick pick = hero.pick(m);
        String option = HeroKit.MAIN.equals(action) ? (pick == null ? m.defaultOption().id() : pick.option()) : action;
        MutableComponent name = HeroKit.MAIN.equals(action) ? CreatorUi.name(m) : Component.translatable(m.optionKey(option));
        return new Bound(entry, action, false, m.act(option), name);
    }

    @Nullable
    private CustomHero ruledFrom;
    private CustomHero ruled;

    /** The hero as the rules of the keys leave him (the last one kept: the skin's check is not free). */
    private CustomHero ruled(CustomHero hero) {
        if (hero != ruledFrom) {
            ruledFrom = hero;
            ruled = CustomHeroRules.sanitize(hero.withSkin(new byte[0]), keysOnly()).hero();
        }
        return ruled;
    }

    /** The rules of the keys alone (the budget and the count are the Powers tab's business). */
    private CustomHeroRules.Limits keysOnly() {
        return screen.previewLimits();
    }

    private void click(int c, boolean s) {
        column = c;
        sneak = s;
        CustomHero hero = draft();
        Bound b = bound(hero, c, s);
        if (s && b != null && b.alt()) {
            screen.say(Component.translatable("screen.enhanced.hero_creator.key.linked.desc", CreatorUi.name(b.entry())), true);
            screen.rebuild();
            return;
        }
        Bound plain = bound(hero, c, false);
        if (s && plain != null && plain.entry() instanceof HeroAbility a && a.hasAlt() && HeroAbility.BOTH.equals(hero.pick(a).option())) {
            screen.say(Component.translatable("screen.enhanced.hero_creator.key.linked.desc", CreatorUi.name(a)), true);
            screen.rebuild();
            return;
        }
        List<Popup.Item> items = new ArrayList<>();
        items.add(Popup.Item.of(Component.translatable("screen.enhanced.hero_creator.key.empty"), null, 0x909090, b == null,
                () -> set(clearOf(draft(), c, s))).icon("minecraft:barrier"));
        for (String candidate : candidates(hero, c, s)) {
            CustomHero placed = place(hero, c, s, candidate);
            if (!fits(placed, c, s, candidate)) {
                continue;
            }
            Bound would = bound(placed, c, s);
            if (would == null) {
                continue;
            }
            boolean current = b != null && candidate.equals(hero.binding(c, s));
            items.add(Popup.Item.of(would.name(), CreatorUi.badge(would.act()), hero.colorOf(hero.pick(would.entry())), current,
                    () -> set(place(draft(), c, s, candidate))).icon(would.entry().icon()));
        }
        screen.ask(new Popup(Component.translatable(s ? "screen.enhanced.hero_creator.key.choose_sneak" : "screen.enhanced.hero_creator.key.choose",
                key(c, false)), items));
    }

    /** What could go on a box: every picked power that takes a key, and on a Sneak box another press option of the power above. */
    private static List<String> candidates(CustomHero hero, int c, boolean s) {
        List<String> list = new ArrayList<>();
        for (CustomHero.Pick p : hero.powers()) {
            if (p.entry() != null && p.act() != PowerModule.Act.NONE) {
                list.add(p.id());
            }
        }
        if (s) {
            CustomHero.Pick above = null;
            String plain = hero.binding(c, false);
            for (CustomHero.Pick p : hero.powers()) {
                if (p.id().equals(plain)) {
                    above = p;
                }
            }
            if (above != null && above.module() != null) {
                for (PowerModule.Option o : above.module().options()) {
                    if (o.act() == PowerModule.Act.PRESS && !o.id().equals(above.option())) {
                        list.add(above.id() + "#" + o.id());
                    }
                }
            }
        }
        return list;
    }

    /** The hero with {@code text} on the box, taken off any other box it was on. */
    private static CustomHero place(CustomHero hero, int c, boolean s, String text) {
        CustomHero out = hero;
        for (int k = 1; k <= CustomHero.KEYS; k++) {
            for (boolean row : new boolean[]{false, true}) {
                if (text.equals(out.binding(k, row))) {
                    out = out.withBinding(k, row, "");
                    if (!row) {
                        // (the Sneak move of a hero's ability goes with it)
                        String alt = text + "#" + HeroKit.ALT;
                        if (alt.equals(out.binding(k, true))) {
                            out = out.withBinding(k, true, "");
                        }
                    }
                }
            }
        }
        out = out.withBinding(c, s, text);
        if (!s) {
            // Whatever was on the box goes; its Sneak move with it, and a hero's ability brings its own.
            CatalogEntry entry = com.kolodkingal.enhanced.custom.Catalog.byId(text);
            CustomHero.Pick pick = entry == null ? null : out.pick(entry);
            if (entry instanceof HeroAbility a && a.hasAlt() && pick != null && HeroAbility.BOTH.equals(pick.option())) {
                out = out.withBinding(c, true, a.id() + "#" + HeroKit.ALT);
            } else if (out.binding(c, true).endsWith("#" + HeroKit.ALT) || out.binding(c, true).contains("#")) {
                out = out.withBinding(c, true, "");
            }
        }
        return out;
    }

    private boolean fits(CustomHero placed, int c, boolean s, String text) {
        return text.equals(ruled(placed).binding(c, s));
    }

    private static CustomHero clearOf(CustomHero hero, int c, boolean s) {
        CustomHero out = hero.withBinding(c, s, "");
        if (!s && out.binding(c, true).contains("#")) {
            out = out.withBinding(c, true, "");
        }
        return out;
    }

    /** A key's name: [R], or [⇧R] with Sneak. */
    private static Component key(int column, boolean sneak) {
        Component name = KeyBindings.ability(column).getTranslatedKeyMessage();
        return sneak ? Component.translatable("hud.enhanced.custom.sneak", name) : name;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        text(g, Component.translatable("screen.enhanced.hero_creator.keys.hint").withStyle(ChatFormatting.GRAY), x, y, w, 0xFFFFFF);
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            Component k = Component.literal("[").append(KeyBindings.ability(c).getTranslatedKeyMessage()).append("]")
                    .withStyle(s -> s.withColor(draft().glow()).withBold(true));
            int kx = gridX + (c - 1) * (cellW + GAP) + cellW / 2 - Math.min(font().width(k), cellW) / 2;
            text(g, k, kx, y + 12, cellW, 0xFFFFFF);
        }
        text(g, Component.translatable("screen.enhanced.hero_creator.key.press").withStyle(ChatFormatting.YELLOW), x, gridY + cellH / 2 - 4,
                labelW, 0xFFFFFF);
        text(g, Component.translatable("screen.enhanced.hero_creator.key.sneak").withStyle(ChatFormatting.YELLOW), x,
                gridY + cellH + GAP + cellH / 2 - 4, labelW, 0xFFFFFF);
        // The joints between a hero's ability and its Sneak move.
        CustomHero hero = draft();
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            Bound s = bound(hero, c, true);
            if (s != null && s.alt()) {
                int cx = gridX + (c - 1) * (cellW + GAP);
                int color = 0xFF000000 | hero.colorOf(hero.pick(s.entry()));
                g.fill(cx + cellW / 2 - 3, gridY + cellH - 1, cx + cellW / 2 + 3, gridY + cellH + GAP + 1, color);
            }
        }
        // The powers that need no key.
        List<Component> passive = new ArrayList<>();
        Component ownKey = null;
        CustomHero ruled = ruled(hero);
        for (CustomHero.Pick p : hero.powers()) {
            CatalogEntry e = p.entry();
            if (e == null) {
                continue;
            }
            boolean onKey = false;
            for (int c = 1; c <= CustomHero.KEYS; c++) {
                onKey |= ruled.binding(c, false).equals(e.id()) || ruled.binding(c, true).startsWith(e.id());
            }
            if (e == HeroAbility.BILLY_BUILD_MAGIC || e == HeroAbility.WANDA_BUILD_MAGIC) {
                ownKey = Component.translatable("screen.enhanced.hero_creator.keys.own_key", CreatorUi.name(e).copy()
                        .withStyle(st -> st.withColor(draft().glow())), BuildKeys.MAGIC.getTranslatedKeyMessage());
            } else if (!onKey) {
                passive.add(CreatorUi.name(e));
            }
        }
        int ly = gridY + 2 * (cellH + GAP) + 26;
        if (!passive.isEmpty()) {
            MutableComponent line = Component.translatable("screen.enhanced.hero_creator.keys.no_key").withStyle(ChatFormatting.GRAY).append(" ");
            for (int i = 0; i < passive.size(); i++) {
                line.append(i == 0 ? Component.empty() : Component.literal(", ")).append(passive.get(i).copy().withStyle(s -> s.withColor(draft().glow())));
            }
            for (var l : font().split(line, w)) {
                if (ly > y + h - 9) {
                    break;
                }
                g.drawString(font(), l, x, ly, 0xFFFFFF, true);
                ly += 10;
            }
        }
        if (ownKey != null && ly <= y + h - 9) {
            // (Build Magic: its own key, no box)
            g.drawString(font(), Component.empty().append(ownKey).withStyle(ChatFormatting.GRAY).getVisualOrderText(), x, ly, 0xFFFFFF, true);
        }
    }
}
