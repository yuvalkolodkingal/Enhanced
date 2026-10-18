package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import com.kolodkingal.enhanced.client.screen.HeroEditorScreen;
import com.kolodkingal.enhanced.custom.Catalog;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CatalogSearch;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.custom.KeyLayout;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Resource;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Powers tab (3.4): the whole catalogue, the thirty generic powers and every built-in hero's abilities, in six kinds
 * (Body, Move, Energy, Elements, Mind, and Heroes, which shows the heroes' faces and then one hero's abilities), with a
 * search box that finds a power by its name or line in the player's language, its id or its hero's name. Powers of one
 * family (all the beams, all the shields...) share one card with a style switcher. On the right, the power clicked: what
 * it does, Add or Remove, Try (on the preview), its options or (a hero's ability) its Sneak move on or off, its strength
 * with the numbers it gives and its points, its own colour, and why it cannot be picked when it cannot.
 */
public final class PowersTab extends EditorTab {
    private static final String[] CATEGORIES = {"body", "move", "energy", "element", "mind", "heroes"};
    private static final int[] SWATCHES = {-1, 0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x00ACC1, 0x1E88E5, 0x8E24AA, 0xD81B60, 0xF5F5F5};

    private String category = "energy";
    @Nullable
    private Power hero;
    private String query = "";
    private boolean searching;
    private CatalogEntry selected = PowerModule.ENERGY_BLAST;
    /** Where the numbers and the colours go (worked out by {@link #init}, drawn by {@link #render}). */
    private int numbersY;
    private int coloursY = -1;
    private int detailX;
    private int detailW;

    public PowersTab(HeroEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int x, int y, int w, int h) {
        int chipW = (w - (CATEGORIES.length - 1) * 2) / CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length; i++) {
            String c = CATEGORIES[i];
            int color = i < 5 ? PowerModule.Group.values()[i].color : 0xF5A623;
            add(new CreatorUi.BigButton(x + i * (chipW + 2), y, chipW, 16, Component.translatable("screen.enhanced.hero_creator.cat." + c), null,
                    color, () -> {
                category = c;
                hero = null;
                query = "";
                screen.resetPage("powers");
                screen.rebuild();
            }).selected(query.isEmpty() && c.equals(category)));
        }
        int lw = Math.max(110, w * 52 / 100);
        detailX = x + lw + 6;
        detailW = w - lw - 6;
        EditBox search = add(new EditBox(font(), x + 1, y + 20, lw - 2, 14, Component.translatable("screen.enhanced.hero_creator.search")));
        search.setHint(Component.translatable("screen.enhanced.hero_creator.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(32);
        search.setValue(query);
        search.setResponder(text -> {
            if (!text.equals(query)) {
                query = text;
                searching = true;
                screen.resetPage("powers");
                screen.rebuild();
            }
        });
        if (searching) {
            screen.setFocused(search);
            search.setFocused(true);
            search.moveCursorToEnd(false);
        }
        cards("powers", list(), x, y + 38, lw, h - 38, 62, 18);
        detail(detailX, y + 20, detailW, h - 20);
    }

    // ---- the list ----

    private List<Card> list() {
        List<Card> cards = new ArrayList<>();
        if (!query.isBlank()) {
            for (CatalogEntry e : Catalog.all()) {
                if (CatalogSearch.matches(e, query, I18n::get)) {
                    cards.add(card(e, CreatorUi.name(e)));
                }
            }
            return cards;
        }
        if (category.equals("heroes")) {
            if (hero == null) {
                for (Power p : HeroLooks.heroes()) {
                    if (!HeroAbility.of(p).isEmpty()) {
                        cards.add(Card.of(p.displayName().copy(), p.titleColor(), false, () -> {
                            hero = p;
                            screen.resetPage("powers");
                            screen.rebuild();
                        }).face(StartTab.face(p)));
                    }
                }
            } else {
                cards.add(Card.of(Component.translatable("screen.enhanced.hero_creator.all_heroes"), 0xB0B0B0, false, () -> {
                    hero = null;
                    screen.resetPage("powers");
                    screen.rebuild();
                }).face(StartTab.face(hero)));
                for (HeroAbility a : HeroAbility.of(hero)) {
                    cards.add(card(a, CreatorUi.name(a)));
                }
            }
            return cards;
        }
        PowerModule.Group group = PowerModule.Group.values()[List.of(CATEGORIES).indexOf(category)];
        Set<CatalogEntry.Family> shown = new HashSet<>();
        for (CatalogEntry e : Catalog.all()) {
            CatalogEntry.Family f = e.family();
            if (f != null) {
                List<CatalogEntry> members = Catalog.family(f);
                if (members.get(0).group() == group && shown.add(f)) {
                    cards.add(card(face(members), Component.translatable("family.enhanced." + f.name().toLowerCase(Locale.ROOT)), true));
                }
            } else if (e.group() == group) {
                cards.add(card(e, CreatorUi.name(e)));
            }
        }
        return cards;
    }

    /** The member a family's card stands for: the one clicked, else one picked, else the first. */
    private CatalogEntry face(List<CatalogEntry> members) {
        if (members.contains(selected)) {
            return selected;
        }
        for (CatalogEntry e : members) {
            if (draft().has(e)) {
                return e;
            }
        }
        return members.get(0);
    }

    private Card card(CatalogEntry e, MutableComponent label) {
        return card(e, label, false);
    }

    /** A card of the list: one entry, or ({@code family}) the card of the entry's whole family. */
    private Card card(CatalogEntry e, MutableComponent label, boolean family) {
        boolean picked = family ? Catalog.family(e.family()).stream().anyMatch(draft()::has) : draft().has(e);
        boolean isSelected = family ? selected.family() == e.family() : e == selected;
        int color = why(e) != null ? 0x505050 : picked ? e.group().color : CreatorUi.darken(e.group().color);
        MutableComponent shown = picked ? Component.literal("✔ ").append(label) : label;
        return Card.of(shown, color, isSelected, () -> {
            selected = e;
            searching = false;
            screen.rebuild();
        }).icon(CreatorUi.icon(e.icon())).tooltip(CreatorUi.name(e).withStyle(s -> s.withColor(e.group().color))
                .append("\n").append(CreatorUi.desc(e).withStyle(ChatFormatting.GRAY)));
    }

    // ---- the power clicked ----

    private void detail(int x, int y, int w, int h) {
        CatalogEntry e = selected;
        CustomHero.Pick pick = draft().pick(e);
        int yy = y + 42;
        // The family's styles.
        if (e.family() != null) {
            List<Card> styles = new ArrayList<>();
            for (CatalogEntry m : Catalog.family(e.family())) {
                Component tip = m.hero() == null ? CreatorUi.name(m) : CreatorUi.name(m).append(" · ").append(m.hero().displayName());
                styles.add(new Card(Component.empty(), null, CreatorUi.icon(m.icon()), null, draft().has(m) ? m.group().color : 0x707070,
                        m == e, true, tip, () -> {
                    selected = m;
                    screen.rebuild();
                }));
            }
            yy = flow(styles, x, yy, w, 20, 20);
        }
        Component reason = why(e);
        int half = (w - GAP) / 2;
        CreatorUi.BigButton toggle = add(new CreatorUi.BigButton(x, yy, half, 20,
                Component.translatable(pick == null ? "screen.enhanced.hero_creator.add" : "screen.enhanced.hero_creator.remove"), null,
                pick == null ? 0x7CFC7C : 0xFF7070, () -> {
            if (draft().has(e)) {
                set(draft().without(e));
            } else {
                add(e);
            }
        }));
        toggle.active = pick != null || reason == null;
        if (reason != null && pick == null) {
            toggle.setTooltip(Tooltip.create(reason));
        }
        CreatorUi.BigButton tryIt = add(new CreatorUi.BigButton(x + half + GAP, yy, half, 20, Component.translatable("screen.enhanced.hero_creator.try"),
                null, draft().glow(), () -> screen.tryEntry(e)));
        tryIt.active = screen.puppet() != null && e.act(pick == null ? "" : pick.option()) != PowerModule.Act.NONE;
        yy += 22;
        if (pick != null) {
            if (e instanceof PowerModule m && m.options().size() > 1) {
                List<Card> options = new ArrayList<>();
                for (PowerModule.Option o : m.options()) {
                    options.add(Card.of(Component.translatable(m.optionKey(o.id())), m.group.color, o.id().equals(pick.option()),
                            () -> option(m, o)).tooltip(Component.translatable(m.optionKey(o.id()) + ".desc")));
                }
                int ow = Math.max(40, (w - (options.size() - 1) * GAP) / options.size());
                yy = flow(options, x, yy, w, ow, 18) + 1;
            } else if (e instanceof HeroAbility a && a.hasAlt()) {
                boolean on = HeroAbility.BOTH.equals(pick.option());
                add(new CreatorUi.BigButton(x, yy, w, 18, Component.translatable(on ? "screen.enhanced.hero_creator.sneak_on"
                        : "screen.enhanced.hero_creator.sneak_off", CreatorUi.altName(a)), null, on ? 0x7CFC7C : 0x909090,
                        () -> sneakMove(a, !on)).selected(on)).setTooltip(Tooltip.create(Component.translatable(
                        "screen.enhanced.hero_creator.sneak.desc", CreatorUi.altName(a), CreatorUi.badge(a.alt()))));
                yy += 21;
            }
            if (e.scales()) {
                int max = limits().maxStrength();
                add(new CreatorUi.IntSlider(x, yy, w, 18, 1, max, Math.min(max, pick.strength()),
                        v -> Component.translatable("screen.enhanced.hero_creator.strength", v), v -> {
                    CustomHero.Pick p = draft().pick(e);
                    if (p != null && p.strength() != v) {
                        screen.update(draft().with(p.withStrength(v)));
                    }
                }));
                yy += 21;
            }
        }
        numbersY = yy;
        coloursY = -1;
        if (pick != null && e instanceof PowerModule) {
            int size = 11;
            coloursY = y + h - size - 2;
            int sx = x + Math.min(w / 3, font().width(Component.translatable("screen.enhanced.hero_creator.own_color")) + 4);
            int per = Math.max(1, Math.min(SWATCHES.length, (x + w - sx) / (size + 2)));
            for (int i = 0; i < per; i++) {
                int c = SWATCHES[i];
                add(new CreatorUi.Swatch(sx + i * (size + 2), coloursY, size, c < 0 ? draft().color() : c, chosen -> {
                    CustomHero.Pick p = draft().pick(e);
                    if (p != null) {
                        set(draft().with(p.withColor(c)));
                    }
                }, () -> {
                    CustomHero.Pick p = draft().pick(e);
                    return p == null ? -2 : p.color() < 0 ? draft().color() : p.color();
                }));
            }
        }
    }

    private void add(CatalogEntry e) {
        String option = e instanceof PowerModule m ? m.defaultOption().id() : HeroAbility.BOTH;
        // 3.5: a hero's ability and its variant row: the variant is the row's, the ability keeps its main action.
        if (e instanceof HeroAbility a && a.variant() != null && draft().has(a.variant())) {
            option = HeroAbility.MAIN;
        }
        int strength = e.scales() ? CustomHeroJson.DEFAULT_STRENGTH : HeroAbility.FIXED_STRENGTH;
        CustomHero hero = draft().with(new CustomHero.Pick(e.id(), strength, option, -1));
        if (e instanceof HeroAbility v && v.parent() != null && hero.pick(v.parent()) != null) {
            CustomHero.Pick parent = hero.pick(v.parent());
            hero = hero.with(parent.withOption(HeroAbility.MAIN));
            for (int c = 1; c <= CustomHero.KEYS; c++) {
                if ((v.parent().id() + "#" + HeroKit.ALT).equals(hero.binding(c, true))) {
                    hero = hero.withBinding(c, true, "");
                }
            }
        }
        set(autoKey(hero, e));
        screen.say(Component.translatable("screen.enhanced.hero_creator.added", CreatorUi.name(e)), true);
    }

    /** A power just picked (or switched to an option with a key) goes on its own key if free, else on the first free one. */
    private static CustomHero autoKey(CustomHero hero, CatalogEntry e) {
        CustomHero.Pick pick = hero.pick(e);
        if (pick == null || pick.act() == PowerModule.Act.NONE || e instanceof HeroAbility a && !a.takesBox()) {
            return hero;
        }
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            if (e.id().equals(hero.binding(c, false))) {
                return hero;
            }
        }
        int first = KeyLayout.preferred(e);
        for (int i = 0; i < CustomHero.KEYS; i++) {
            int c = (first - 1 + i) % CustomHero.KEYS + 1;
            if (hero.binding(c, false).isEmpty()) {
                CustomHero laid = hero.withKey(c, e.id());
                if (e instanceof HeroAbility a && a.hasAlt() && HeroAbility.BOTH.equals(pick.option())) {
                    laid = laid.withBinding(c, true, a.id() + "#" + HeroKit.ALT);
                }
                return laid;
            }
        }
        return hero;
    }

    private void option(PowerModule m, PowerModule.Option o) {
        CustomHero.Pick p = draft().pick(m);
        if (p == null) {
            return;
        }
        CustomHero hero = draft().with(p.withOption(o.id()));
        if (o.act() == PowerModule.Act.NONE) {
            for (int c = 1; c <= CustomHero.KEYS; c++) {
                if (m.id.equals(hero.binding(c, false))) {
                    hero = hero.withKey(c, "").withBinding(c, true, "");
                }
            }
        } else {
            hero = autoKey(hero, m);
        }
        set(hero);
    }

    /** A hero's ability with its Sneak move on (it takes the Sneak cell of its key) or off. */
    private void sneakMove(HeroAbility a, boolean on) {
        CustomHero.Pick p = draft().pick(a);
        if (p == null) {
            return;
        }
        CustomHero hero = draft().with(p.withOption(on ? HeroAbility.BOTH : HeroAbility.MAIN));
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            if (a.id().equals(hero.binding(c, false))) {
                hero = hero.withBinding(c, true, on ? a.id() + "#" + HeroKit.ALT : "");
            } else if (on && a.id().equals(hero.binding(c, true))) {
                hero = hero.withBinding(c, true, "");
            }
        }
        set(hero);
    }

    /** Why an entry cannot be picked now, or null when it can. */
    @Nullable
    private Component why(CatalogEntry e) {
        CustomHeroRules.Limits limits = limits();
        if (!limits.allows(e)) {
            return Component.translatable("screen.enhanced.hero_creator.why.server");
        }
        if (e instanceof HeroAbility && !limits.heroModules()) {
            return Component.translatable("screen.enhanced.hero_creator.why.hero_modules");
        }
        if (e.world() && !limits.worldPowers()) {
            return Component.translatable("screen.enhanced.hero_creator.why.world");
        }
        if (draft().has(e)) {
            return null;
        }
        if (draft().powers().size() >= limits.maxPowers()) {
            return Component.translatable("screen.enhanced.hero_creator.why.full", limits.maxPowers());
        }
        if (e instanceof HeroAbility a && !a.requires().isEmpty() && draft().powers().stream().noneMatch(p -> a.requires().contains(p.id()))) {
            HeroAbility needed = HeroAbility.byId(a.requires().get(0));
            return Component.translatable("screen.enhanced.hero_creator.why.requires", needed == null ? Component.literal(a.requires().get(0))
                    : CreatorUi.name(needed));
        }
        Power owner = e.hero() == null ? Power.NONE : e.hero();
        Set<Resource> mine = EnumSet.noneOf(Resource.class);
        mine.addAll(e.resources(e instanceof PowerModule m ? m.defaultOption().id() : HeroAbility.BOTH));
        for (CustomHero.Pick p : draft().powers()) {
            CatalogEntry other = p.entry();
            if (other == null) {
                continue;
            }
            Power theirs = other.hero() == null ? Power.NONE : other.hero();
            if (theirs == owner) {
                continue;
            }
            for (Resource r : other.resources(p.option())) {
                if (mine.contains(r)) {
                    return Component.translatable("screen.enhanced.hero_creator.why.clash", Component.translatable(CustomHeroRules.resourceKey(r)),
                            CreatorUi.name(other));
                }
            }
        }
        return null;
    }

    // ---- words ----

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
        CatalogEntry e = selected;
        int dx = detailX;
        int dw = detailW;
        int dy = y + 20;
        text(g, CreatorUi.name(e).withStyle(s -> s.withColor(e.group().color).withBold(true)), dx, dy, dw, 0xFFFFFF);
        Component who = e.hero() != null ? Component.translatable("screen.enhanced.hero_creator.of_hero", e.hero().displayName())
                : Component.translatable("screen.enhanced.hero_creator.cat." + CATEGORIES[e.group().ordinal()]);
        text(g, who.copy().withStyle(ChatFormatting.GRAY), dx, dy + 10, dw, 0xFFFFFF);
        List<FormattedCharSequence> desc = font().split(CreatorUi.desc(e).withStyle(ChatFormatting.WHITE), dw);
        for (int i = 0; i < Math.min(2, desc.size()); i++) {
            g.drawString(font(), desc.get(i), dx, dy + 20 + i * 9, 0xFFFFFF, true);
        }
        CustomHero.Pick pick = draft().pick(e);
        String option = pick == null ? (e instanceof PowerModule m ? m.defaultOption().id() : HeroAbility.BOTH) : pick.option();
        int strength = pick == null ? (e.scales() ? CustomHeroJson.DEFAULT_STRENGTH : HeroAbility.FIXED_STRENGTH) : pick.strength();
        int ny = numbersY;
        MutableComponent line = Component.translatable("screen.enhanced.hero_creator.costs", e.cost(strength)).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("  ")).append(CreatorUi.badge(e.act(option)).withStyle(ChatFormatting.AQUA));
        text(g, line, dx, ny, dw, 0xFFFFFF);
        ny += 11;
        Component reason = why(e);
        if (reason != null && pick == null) {
            for (FormattedCharSequence l : font().split(FormattedText.composite(reason.copy().withStyle(ChatFormatting.RED)), dw)) {
                g.drawString(font(), l, dx, ny, 0xFFFFFF, true);
                ny += 9;
            }
            return;
        }
        List<Component> numbers = new ArrayList<>();
        if (e instanceof PowerModule m) {
            for (ModuleStats.Stat stat : ModuleStats.of(m, option)) {
                numbers.add(Component.translatable(stat.nameKey()).withStyle(ChatFormatting.GRAY).append(": ")
                        .append(Component.translatable(stat.unitKey(), stat.shown(strength)).withStyle(s -> s.withColor(draft().colorOf(pick)))));
            }
        } else if (e instanceof HeroAbility a) {
            float f = 1.0F + (float) limits().spread() * (Math.max(1, Math.min(10, strength)) - 5) / 5.0F;
            if (a.scales(HeroAbility.Scales.DAMAGE)) {
                numbers.add(Component.translatable("screen.enhanced.hero_creator.stat.damage", times(f)).withStyle(ChatFormatting.GRAY));
            }
            if (a.scales(HeroAbility.Scales.COOLDOWN)) {
                numbers.add(Component.translatable("screen.enhanced.hero_creator.stat.cooldown", times(1.0F / f)).withStyle(ChatFormatting.GRAY));
            }
            if (a.scales(HeroAbility.Scales.BODY)) {
                numbers.add(Component.translatable("screen.enhanced.hero_creator.stat.body", times(f)).withStyle(ChatFormatting.GRAY));
            }
            if (a.hasAlt() && (pick == null || HeroAbility.BOTH.equals(pick.option()))) {
                numbers.add(Component.translatable("screen.enhanced.hero_creator.stat.sneak", CreatorUi.altName(a)).withStyle(ChatFormatting.GRAY));
            }
        }
        int colW = Math.max(40, (dw - 4) / 2);
        int bottom = coloursY >= 0 ? coloursY - 2 : y + h;
        for (int i = 0; i < numbers.size(); i++) {
            int row = numbers.size() > 3 ? i / 2 : i;
            int col = numbers.size() > 3 ? i % 2 : 0;
            int ty = ny + row * 10;
            if (ty + 9 > bottom) {
                break;
            }
            text(g, numbers.get(i), dx + col * (colW + 4), ty, numbers.size() > 3 ? colW : dw, 0xFFFFFF);
        }
        if (coloursY >= 0) {
            text(g, Component.translatable("screen.enhanced.hero_creator.own_color").withStyle(ChatFormatting.GRAY), dx, coloursY + 2, dw / 3,
                    0xFFFFFF);
        }
        if (!query.isBlank() && list().isEmpty()) {
            text(g, Component.translatable("screen.enhanced.hero_creator.search.none").withStyle(ChatFormatting.GRAY), x, y + 40, detailX - x - 6,
                    0xFFFFFF);
        }
    }

    private static String times(float f) {
        return String.format(Locale.ROOT, "%.2f", f).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
