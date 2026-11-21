package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.Catalog;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.PowerModule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 3.18.1, the witches' levels as data (pure: no world, no player, so both sides and the tests read it): the experience
 * each level needs, what each of a book's five levels opens and how strong it makes each spell (the tables of the design;
 * level 5 is exactly the book's definition of before), the cap that keeps every ability of a real hero at its strength 5
 * ({@link com.kolodkingal.enhanced.custom.HeroKit#factor}(5) = 1.0: never stronger than the hero himself), the cantrips
 * that fill the empty keys ({@link #fill}) and the five levels of a gift of the Demiurge ({@link #giftLevel}).
 */
public final class SpellLevels {
    /** The highest level. */
    public static final int MAX = 5;
    /** The experience each level needs, from level 1 (0) to level 5 (850). */
    private static final int[] XP = {0, 100, 250, 500, 850};

    /**
     * The role of a power, for the cantrips: each empty key gets the cantrip of the next role in this order that none of
     * the witch's powers has yet (protection first: what a weak witch needs most).
     */
    public enum Role {
        PROTECTION("ward"), MOBILITY("blink"), HEALING("mend"), CONTROL("repel"), ATTACK("spark"), HELPER("light");

        /** The cantrip's option of {@link PowerModule#CANTRIP}. */
        public final String cantrip;

        Role(String cantrip) {
            this.cantrip = cantrip;
        }

        @Nullable
        public static Role ofCantrip(String option) {
            for (Role r : values()) {
                if (r.cantrip.equals(option)) {
                    return r;
                }
            }
            return null;
        }
    }

    /** The strength of each of a book's spells at levels 1 to 5 (0: not open yet), by pick id. */
    private static final Map<Spellbook, Map<String, int[]>> STRENGTHS = Map.of(
            Spellbook.ETHEREAL, Map.of(
                    "agatha.purple_beam", new int[]{2, 3, 3, 4, 5},
                    "agatha.body", new int[]{1, 2, 2, 3, 3},
                    "flight", new int[]{0, 2, 3, 3, 4},
                    "magic", new int[]{0, 0, 4, 5, 6},
                    "agatha.shield", new int[]{0, 0, 0, 4, 5},
                    "agatha.runes", new int[]{0, 0, 0, 0, 5}),
            Spellbook.ELDRITCH, Map.of(
                    "magic", new int[]{2, 3, 3, 4, 5},
                    "strange.whip", new int[]{0, 3, 4, 4, 5},
                    "strange.shields", new int[]{0, 0, 4, 4, 5},
                    "strange.portal", new int[]{0, 0, 0, 4, 5},
                    "strange.mirror", new int[]{0, 0, 0, 0, 5},
                    "strange.astral", new int[]{0, 0, 0, 0, 5}),
            Spellbook.GREEN, Map.of(
                    "earth", new int[]{2, 3, 3, 4, 5},
                    "healing", new int[]{2, 3, 3, 4, 5},
                    "nature", new int[]{0, 3, 4, 4, 5},
                    "agatha.runes", new int[]{0, 0, 0, 2, 2}));

    /** The level that opens a Sneak cell naming another option of its module ("earth#wall": the Green witch's level 3). */
    private static final Map<String, Integer> OPTION_CELLS = Map.of("earth#wall", 3);

    /** The bindings a level adds after the rules (an option on a plain key, which the creator's rules keep for Sneak). */
    private record Extra(Spellbook book, int level, int column, String cell) {
    }

    /** 3.18.1: the Green witch's new spell, the Root Grasp, on B at level 5 (N stays a cantrip). */
    private static final List<Extra> EXTRAS = List.of(new Extra(Spellbook.GREEN, 5, 4, "nature#roots"));

    /** The share of a gift's strength each level gives (level 5: all of it). */
    private static final double[] GIFT_SHARE = {0.4, 0.55, 0.7, 0.85, 1.0};

    private SpellLevels() {
    }

    // ---- experience ----

    /** The experience level {@code level} (1-5) needs. */
    public static int threshold(int level) {
        return XP[Math.max(1, Math.min(MAX, level)) - 1];
    }

    /** The level {@code xp} points reach, at most {@code maxLevel}. */
    public static int levelFor(int xp, int maxLevel) {
        int level = 1;
        for (int l = 2; l <= MAX; l++) {
            if (xp >= XP[l - 1]) {
                level = l;
            }
        }
        return Math.max(1, Math.min(Math.max(1, Math.min(MAX, maxLevel)), level));
    }

    // ---- a book's levels ----

    /** The strength of a book's spell at a level (0: not open), by its pick id. */
    public static int strength(Spellbook book, String id, int level) {
        int[] row = STRENGTHS.get(book).get(HeroAbility.canonicalId(id));
        return row == null ? 0 : row[Math.max(1, Math.min(MAX, level)) - 1];
    }

    /** Whether a binding of the book's definition is open at a level. */
    private static boolean open(Spellbook book, String cell, int level) {
        if (cell.isEmpty()) {
            return true;
        }
        Integer optionLevel = OPTION_CELLS.get(cell);
        if (optionLevel != null && level < optionLevel) {
            return false;
        }
        int hash = cell.indexOf('#');
        return strength(book, hash < 0 ? cell : cell.substring(0, hash), level) > 0;
    }

    /**
     * The book's definition at a level, before the rules: its picks open at that level, at that level's strengths, on
     * their keys (the keys of the rest left empty). At level 5 it is {@code raw} itself.
     */
    public static CustomHero bookLevel(Spellbook book, CustomHero raw, int level) {
        List<CustomHero.Pick> picks = new ArrayList<>();
        for (CustomHero.Pick pick : raw.powers()) {
            int s = strength(book, pick.id(), level);
            if (s > 0) {
                picks.add(pick.withStrength(s));
            }
        }
        List<String> keys = new ArrayList<>(raw.keys());
        keys.replaceAll(k -> open(book, k, level) ? k : "");
        List<String> sneak = new ArrayList<>(raw.sneakKeys());
        sneak.replaceAll(k -> open(book, k, level) ? k : "");
        return raw.withPowers(picks).withKeys(keys).withSneakKeys(sneak);
    }

    /** The bindings the book's level adds after the rules ({@link #EXTRAS}). */
    public static CustomHero extras(Spellbook book, CustomHero ruled, int level) {
        CustomHero out = ruled;
        for (Extra e : EXTRAS) {
            if (e.book() == book && level >= e.level()) {
                out = out.withBinding(e.column(), false, e.cell());
            }
        }
        return out;
    }

    /**
     * The level at which each key (R G V B N) of a book first holds a spell of its own (0: never; a cantrip is no spell
     * of its own), from the definitions of its five levels without cantrips.
     */
    public static int[] keyLevels(java.util.function.IntFunction<CustomHero> levels) {
        int[] out = new int[CustomHero.KEYS];
        for (int level = 1; level <= MAX; level++) {
            CustomHero def = levels.apply(level);
            for (int c = 1; c <= CustomHero.KEYS; c++) {
                if (out[c - 1] == 0 && !def.binding(c, false).isEmpty()) {
                    out[c - 1] = level;
                }
            }
        }
        return out;
    }

    // ---- never stronger than the real hero ----

    /**
     * The definition with every ability of a real hero ({@link HeroAbility}) at most at its strength 5, his own power
     * ({@code HeroKit.factor(5)} = 1.0); generic powers are nobody's and keep theirs.
     */
    public static CustomHero capToHero(CustomHero def) {
        List<CustomHero.Pick> picks = new ArrayList<>(def.powers());
        boolean changed = false;
        for (int i = 0; i < picks.size(); i++) {
            CustomHero.Pick p = picks.get(i);
            if (p.entry() instanceof HeroAbility && p.strength() > HeroAbility.FIXED_STRENGTH) {
                picks.set(i, p.withStrength(HeroAbility.FIXED_STRENGTH));
                changed = true;
            }
        }
        return changed ? def.withPowers(picks) : def;
    }

    // ---- the cantrips ----

    /** The role of a power in one of its options (null: none, as a durable body). */
    @Nullable
    public static Role role(String id, String option) {
        CatalogEntry e = Catalog.byId(id);
        if (e instanceof PowerModule m) {
            return switch (m) {
                case FORCE_FIELD -> Role.PROTECTION;
                case FLIGHT, SUPER_SPEED, TELEPORT, WALL_CRAWL, PHASING -> Role.MOBILITY;
                case HEALING -> Role.HEALING;
                case MAGIC -> "ward".equals(option) ? Role.HEALING : "curse".equals(option) ? Role.CONTROL : Role.ATTACK;
                case TELEKINESIS, MIND_CONTROL, ANIMAL_CONTROL, TIME -> Role.CONTROL;
                case NATURE -> "roots".equals(option) ? Role.CONTROL : Role.HELPER;
                case SUPER_SENSES, TELEPATHY, TECHNOPATHY, INVISIBILITY, SHAPESHIFT, DUPLICATION -> Role.HELPER;
                case WEATHER -> "cycle".equals(option) ? Role.HELPER : Role.ATTACK;
                case CANTRIP -> Role.ofCantrip(option);
                case DURABILITY -> null;
                default -> Role.ATTACK;
            };
        }
        if (e instanceof HeroAbility a) {
            switch (a) {
                case AGATHA_BODY:
                    return null;
                case AGATHA_RUNES:
                case STRANGE_MIRROR:
                case STRANGE_ASTRAL:
                    return Role.CONTROL;
                case STRANGE_PORTAL:
                    return Role.MOBILITY;
                default:
                    break;
            }
            if (a.family() == CatalogEntry.Family.SHIELD) {
                return Role.PROTECTION;
            }
            if (a.passive()) {
                return null;
            }
            return switch (a.group()) {
                case MOVE -> Role.MOBILITY;
                case MIND -> Role.CONTROL;
                case BODY -> null;
                default -> Role.ATTACK;
            };
        }
        return e == null ? null : Role.ATTACK;
    }

    /** The roles a definition's powers already have: what is on its keys (each binding's own action), and its passives. */
    public static Set<Role> roles(CustomHero def) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        Set<String> bound = new LinkedHashSet<>();
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            for (boolean sneak : new boolean[]{false, true}) {
                String cell = def.binding(c, sneak);
                if (cell.isEmpty()) {
                    continue;
                }
                int hash = cell.indexOf('#');
                String id = hash < 0 ? cell : cell.substring(0, hash);
                String action = hash < 0 ? null : cell.substring(hash + 1);
                CustomHero.Pick pick = pickOf(def, id);
                if (pick == null) {
                    continue;
                }
                bound.add(pick.id());
                String option = action == null || com.kolodkingal.enhanced.custom.HeroKit.ALT.equals(action)
                        || com.kolodkingal.enhanced.custom.HeroKit.MAIN.equals(action) ? pick.option()
                        : action;
                Role r = role(pick.id(), option);
                if (r != null) {
                    roles.add(r);
                }
            }
        }
        for (CustomHero.Pick pick : def.powers()) {
            if (!bound.contains(pick.id())) {
                Role r = role(pick.id(), pick.option());
                if (r != null) {
                    roles.add(r);
                }
            }
        }
        return roles;
    }

    @Nullable
    private static CustomHero.Pick pickOf(CustomHero def, String id) {
        String wanted = HeroAbility.canonicalId(id.trim().toLowerCase(Locale.ROOT));
        for (CustomHero.Pick p : def.powers()) {
            if (p.id().equals(wanted)) {
                return p;
            }
        }
        return null;
    }

    /**
     * The definition with a cantrip on each empty plain key, R to N: each gets the next role (protection, mobility,
     * healing, control, attack, helper) that none of its powers has yet; when every role is had, the next not given yet.
     * The cantrips are one pick of {@link PowerModule#CANTRIP} at {@code strength} (the witch's level) and a cell
     * {@code cantrip#<option>} on each key. A definition with no empty key is returned as it is.
     */
    public static CustomHero fill(CustomHero def, int strength) {
        List<String> keys = new ArrayList<>(def.keys());
        Set<Role> had = roles(def);
        Set<Role> given = EnumSet.noneOf(Role.class);
        String first = null;
        for (int c = 0; c < CustomHero.KEYS; c++) {
            if (!keys.get(c).isEmpty()) {
                continue;
            }
            Role role = null;
            for (Role r : Role.values()) {
                if (!had.contains(r) && !given.contains(r)) {
                    role = r;
                    break;
                }
            }
            if (role == null) {
                for (Role r : Role.values()) {
                    if (!given.contains(r)) {
                        role = r;
                        break;
                    }
                }
            }
            if (role == null) {
                break;
            }
            given.add(role);
            keys.set(c, PowerModule.CANTRIP.id + "#" + role.cantrip);
            if (first == null) {
                first = role.cantrip;
            }
        }
        if (first == null) {
            return def;
        }
        List<CustomHero.Pick> picks = new ArrayList<>(def.powers());
        picks.removeIf(p -> p.id().equals(PowerModule.CANTRIP.id));
        picks.add(new CustomHero.Pick(PowerModule.CANTRIP.id, Math.max(1, Math.min(MAX, strength)), first, -1));
        return def.withPowers(picks).withKeys(keys);
    }

    // ---- naming what a level opens ----

    /**
     * A binding cell as its spell is named: the pick's id, with the action a Sneak cell or an option names
     * ({@code agatha.purple_beam#alt}, {@code earth#wall}), and a book's spell by its option ({@code magic#orb}: the
     * Ethereal Orb, not Magic). Empty when the cell names nothing picked.
     */
    public static String displayCell(CustomHero def, String cell) {
        if (cell.isEmpty()) {
            return "";
        }
        int hash = cell.indexOf('#');
        CustomHero.Pick pick = pickOf(def, hash < 0 ? cell : cell.substring(0, hash));
        if (pick == null) {
            return "";
        }
        if (hash >= 0) {
            return pick.id() + cell.substring(hash);
        }
        if (pick.entry() instanceof PowerModule m && m != PowerModule.FLIGHT && m.options().size() > 1 && m.option(pick.option()) != null) {
            return pick.id() + "#" + pick.option();
        }
        return pick.id();
    }

    /** Whether a picked power is on none of a definition's ten bindings (a power that needs no key). */
    public static boolean onNoKey(CustomHero def, String id) {
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            for (boolean sneak : new boolean[]{false, true}) {
                String cell = def.binding(c, sneak);
                int hash = cell.indexOf('#');
                if ((hash < 0 ? cell : cell.substring(0, hash)).equals(id)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The name of what a cell ({@link #displayCell}) does, in the player's language. */
    public static net.minecraft.network.chat.MutableComponent label(String cell) {
        int hash = cell.indexOf('#');
        String id = hash < 0 ? cell : cell.substring(0, hash);
        String action = hash < 0 ? null : cell.substring(hash + 1);
        CatalogEntry entry = Catalog.byId(id);
        if (entry instanceof HeroAbility a) {
            if (!a.isVariant() && com.kolodkingal.enhanced.custom.HeroKit.ALT.equals(action)) {
                return net.minecraft.network.chat.Component.translatable(a.altKey());
            }
            return net.minecraft.network.chat.Component.translatable(a.nameKey());
        }
        if (entry instanceof PowerModule m && action != null && m.option(action) != null) {
            return net.minecraft.network.chat.Component.translatable(m.optionKey(action));
        }
        return entry != null ? net.minecraft.network.chat.Component.translatable(entry.nameKey())
                : net.minecraft.network.chat.Component.literal(id);
    }

    /** Whether a binding cell is a cantrip. */
    public static boolean isCantrip(String cell) {
        return cell.startsWith(PowerModule.CANTRIP.id + "#");
    }

    // ---- a gift of the Demiurge ----

    /**
     * The rows of a gift in their key order: for each key R to N that holds anything (plain or with Sneak), the picks it
     * names. The five levels open them in this order (level 5: all of them).
     */
    public static List<Set<String>> giftUnits(CustomHero gift) {
        List<Set<String>> units = new ArrayList<>();
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            Set<String> unit = new LinkedHashSet<>();
            for (boolean sneak : new boolean[]{false, true}) {
                String cell = gift.binding(c, sneak);
                if (cell.isEmpty() || isCantrip(cell)) {
                    continue;
                }
                int hash = cell.indexOf('#');
                CustomHero.Pick pick = pickOf(gift, hash < 0 ? cell : cell.substring(0, hash));
                if (pick != null) {
                    unit.add(pick.id());
                }
            }
            if (!unit.isEmpty()) {
                units.add(unit);
            }
        }
        return units;
    }

    /** How many of a gift's {@code units} keys a level opens (spread over the five levels; level 5: all). */
    public static int giftOpen(int units, int level) {
        int l = Math.max(1, Math.min(MAX, level));
        return units == 0 ? 0 : (int) Math.ceil(units * l / (double) MAX);
    }

    /**
     * A gift's definition at a level, before the cantrips: its passive powers (on no key) from level 1, its keys opened in
     * their order ({@link #giftUnits}, {@link #giftOpen}), every power at the level's share of its strength (40% at level
     * 1, all of it at level 5, never below 1) and capped at the real hero's ({@link #capToHero}).
     */
    public static CustomHero giftLevel(CustomHero gift, int level) {
        int l = Math.max(1, Math.min(MAX, level));
        List<Set<String>> units = giftUnits(gift);
        int open = giftOpen(units.size(), l);
        Set<String> opened = new LinkedHashSet<>();
        Set<String> bound = new LinkedHashSet<>();
        for (int i = 0; i < units.size(); i++) {
            bound.addAll(units.get(i));
            if (i < open) {
                opened.addAll(units.get(i));
            }
        }
        List<CustomHero.Pick> picks = new ArrayList<>();
        for (CustomHero.Pick p : gift.powers()) {
            if (p.id().equals(PowerModule.CANTRIP.id)) {
                continue;
            }
            if (opened.contains(p.id()) || !bound.contains(p.id())) {
                int s = Math.max(1, (int) Math.round(p.strength() * GIFT_SHARE[l - 1]));
                picks.add(p.withStrength(Math.min(p.strength(), s)));
            }
        }
        List<String> keys = new ArrayList<>();
        List<String> sneak = new ArrayList<>();
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            keys.add(cellOpen(gift, gift.binding(c, false), opened));
            sneak.add(cellOpen(gift, gift.binding(c, true), opened));
        }
        return capToHero(gift.withPowers(picks).withKeys(keys).withSneakKeys(sneak));
    }

    private static String cellOpen(CustomHero gift, String cell, Set<String> opened) {
        if (cell.isEmpty() || isCantrip(cell)) {
            return "";
        }
        int hash = cell.indexOf('#');
        CustomHero.Pick pick = pickOf(gift, hash < 0 ? cell : cell.substring(0, hash));
        return pick != null && opened.contains(pick.id()) ? cell : "";
    }
}
