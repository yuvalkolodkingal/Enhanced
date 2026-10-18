package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Toggle;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * What a custom hero's definition means at run time (3.4), built once per definition on both sides and kept on his
 * {@code PowerData} ({@code data.kit()}, installed by {@code Powers.get}): which built-in heroes' abilities he has, which
 * key column each sits on, what each of his ten bindings (five keys, plain and with Sneak) does, how strong each entry
 * is, and his roles. Immutable; it trusts the definition to have passed {@link CustomHeroRules#sanitize} (a binding that
 * names nothing it can use is simply free).
 */
public final class HeroKit {
    /** The action of a binding that is a hero ability's main action, or a module in its picked option. */
    public static final String MAIN = "main";
    /** The action of a binding that is a hero ability's Sneak variant. */
    public static final String ALT = "alt";

    /**
     * One binding: what a key (plain or with Sneak) does.
     *
     * @param entry  the module or hero ability
     * @param action {@link #MAIN}, {@link #ALT} (a hero ability's Sneak variant) or one of a module's options
     * @param column the key, 1-5 (R G V B N)
     * @param sneak  whether it is the key's Sneak cell
     */
    public record Binding(CatalogEntry entry, String action, int column, boolean sneak) {
        public boolean isAlt() {
            return ALT.equals(action);
        }

        /** The hero ability, or null for a module. */
        @Nullable
        public HeroAbility ability() {
            return entry instanceof HeroAbility a ? a : null;
        }

        /** The module, or null for a hero ability. */
        @Nullable
        public PowerModule module() {
            return entry instanceof PowerModule m ? m : null;
        }
    }

    private final CustomHero hero;
    private final EnumSet<HeroAbility> rows = EnumSet.noneOf(HeroAbility.class);
    private final EnumSet<Power> sources = EnumSet.noneOf(Power.class);
    private final Map<HeroAbility, Integer> strengths = new EnumMap<>(HeroAbility.class);
    private final Map<PowerModule, Integer> moduleStrengths = new EnumMap<>(PowerModule.class);
    private final Binding[] plain = new Binding[CustomHero.KEYS + 1];
    private final Binding[] sneak = new Binding[CustomHero.KEYS + 1];
    private final Map<HeroAbility, Integer> columns = new EnumMap<>(HeroAbility.class);
    private final EnumSet<Trait> traits = EnumSet.noneOf(Trait.class);
    private final EnumSet<Toggle> defaultToggles = EnumSet.noneOf(Toggle.class);

    private HeroKit(CustomHero hero) {
        this.hero = hero;
        for (CustomHero.Pick pick : hero.powers()) {
            CatalogEntry entry = pick.entry();
            if (entry instanceof HeroAbility a) {
                rows.add(a);
                sources.add(a.hero);
                strengths.put(a, pick.strength());
            } else if (entry instanceof PowerModule m) {
                moduleStrengths.put(m, pick.strength());
            } else {
                continue;
            }
            traits.addAll(entry.traits());
        }
        for (int column = 1; column <= CustomHero.KEYS; column++) {
            plain[column] = parse(hero.binding(column, false), column, false);
            sneak[column] = parse(hero.binding(column, true), column, true);
        }
        for (int column = CustomHero.KEYS; column >= 1; column--) {
            // (The first column that holds it wins: walk backwards and overwrite.)
            for (Binding b : new Binding[]{sneak[column], plain[column]}) {
                if (b != null && b.ability() != null) {
                    columns.put(b.ability(), column);
                }
            }
        }
        if (rows.contains(HeroAbility.TOMMY_SPEED)) {
            defaultToggles.add(Toggle.SUPER_SPEED);
        }
        if (rows.contains(HeroAbility.SPIDER_CRAWL)) {
            defaultToggles.add(Toggle.WALL_CRAWL);
            CustomHero.Pick crawl = hero.pick(HeroAbility.SPIDER_CRAWL);
            if (crawl == null || !HeroAbility.MAIN.equals(crawl.option())) {
                defaultToggles.add(Toggle.SPIDER_SENSE);
            }
        }
    }

    /** The kit of a (sanitised) definition. */
    public static HeroKit of(CustomHero hero) {
        return new HeroKit(hero);
    }

    @Nullable
    private Binding parse(String text, int column, boolean onSneak) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int hash = text.indexOf('#');
        String id = hash < 0 ? text : text.substring(0, hash);
        String action = hash < 0 ? MAIN : text.substring(hash + 1);
        CatalogEntry entry = Catalog.byId(id);
        if (entry == null || hero.pick(entry) == null) {
            return null;
        }
        if (entry instanceof HeroAbility a) {
            if (a.passive() || ALT.equals(action) && !a.hasAlt() || !ALT.equals(action) && !MAIN.equals(action)) {
                return null;
            }
        } else if (!MAIN.equals(action) && entry.options().stream().noneMatch(o -> o.id().equals(action))) {
            return null;
        }
        return new Binding(entry, action, column, onSneak);
    }

    /** The definition this kit was built from. */
    public CustomHero hero() {
        return hero;
    }

    /** Whether the hero ability is picked. */
    public boolean has(HeroAbility a) {
        return rows.contains(a);
    }

    /** Whether any ability of this built-in hero is picked (his upkeep runs). */
    public boolean acts(Power hero) {
        return sources.contains(hero);
    }

    /** The built-in heroes whose abilities are picked. */
    public Set<Power> sources() {
        return Collections.unmodifiableSet(sources);
    }

    /** The hero abilities picked. */
    public Set<HeroAbility> rows() {
        return Collections.unmodifiableSet(rows);
    }

    /** The key column (1-5) a hero ability is on (its main action or its Sneak variant), or 0 when it is on no key. */
    public int column(HeroAbility a) {
        Integer column = columns.get(a);
        return column == null ? 0 : column;
    }

    /** What key {@code column} (1-5) does, or null when it is free. */
    @Nullable
    public Binding plain(int column) {
        return column < 1 || column > CustomHero.KEYS ? null : plain[column];
    }

    /** What key {@code column} (1-5) does with Sneak held, or null when its Sneak cell is free (then it does what it does). */
    @Nullable
    public Binding sneak(int column) {
        return column < 1 || column > CustomHero.KEYS ? null : sneak[column];
    }

    /** The binding of a key: its Sneak cell when Sneak is held and it has one, else its plain cell (or null). */
    @Nullable
    public Binding binding(int column, boolean sneakHeld) {
        Binding s = sneakHeld ? sneak(column) : null;
        return s != null ? s : plain(column);
    }

    /** The strength (1-10) of a picked entry, or 0. */
    public int strength(CatalogEntry e) {
        Integer s = e instanceof HeroAbility a ? strengths.get(a) : e instanceof PowerModule m ? moduleStrengths.get(m) : null;
        return s == null ? 0 : s;
    }

    /** How much more a hero ability does at its strength: {@link #factor} if it scales its damage, else 1. */
    public float damageFactor(HeroAbility a) {
        return a.scales(HeroAbility.Scales.DAMAGE) && has(a) ? factor(strength(a)) : 1.0F;
    }

    /** How much longer its cooldown is: 1 / {@link #factor} if it scales its cooldown, else 1. */
    public float cooldownFactor(HeroAbility a) {
        return a.scales(HeroAbility.Scales.COOLDOWN) && has(a) ? 1.0F / factor(strength(a)) : 1.0F;
    }

    /** How much of the hero's body it gives: {@link #factor} if it scales the body, else 1. */
    public float bodyFactor(HeroAbility a) {
        return a.scales(HeroAbility.Scales.BODY) && has(a) ? factor(strength(a)) : 1.0F;
    }

    /** The toggles that start on when he becomes this hero (Tommy's run, Spider-Man's crawl and sense). */
    public Set<Toggle> defaultToggles() {
        return Collections.unmodifiableSet(defaultToggles);
    }

    /** Whether one of his picks gives him this role. */
    public boolean hasTrait(Trait t) {
        return traits.contains(t);
    }

    /**
     * The factor of a hero ability at {@code strength} (1-10): {@code 1 + spread * (s - 5) / 5}, with the server's
     * {@code custom_hero.heroStrengthSpread} (0.5: strength 1 is 0.6, 5 is 1, 10 is 1.5). Read when used.
     */
    public static float factor(int strength) {
        double spread = EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.HERO_STRENGTH_SPREAD.get() : DEFAULT_SPREAD;
        int s = Math.max(1, Math.min(PowerModule.MAX_STRENGTH, strength));
        return (float) (1.0 + spread * (s - 5) / 5.0);
    }

    /** The default of {@code custom_hero.heroStrengthSpread}. */
    public static final double DEFAULT_SPREAD = 0.5;
}
