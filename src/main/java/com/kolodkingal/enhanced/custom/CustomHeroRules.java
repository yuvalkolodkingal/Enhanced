package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.power.Power;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The server's judgement of a custom hero (3.3): every definition a client sends or a file brings passes here before it
 * becomes anyone's hero. It never refuses a hero outright: it makes the best hero the rules allow, and says what it
 * changed (each note a lang key with its arguments, shown to the player). Pure (no world, no player), so the client's
 * screen runs it too, with the limits the server synced, and the tests run it directly.
 *
 * <p>In order: the name is cleaned (formatting codes and control characters out, at most {@link CustomHero#MAX_NAME}
 * characters, "My Hero" when nothing is left); the colours are made opaque RGB; powers this version does not know, the
 * server's disabled ones (an id, or 3.4 {@code <hero>.*} for all of a hero's), the heroes' abilities when
 * {@code heroModules} is off, the world-scale ones when {@code worldPowers} is off, and repeats are dropped; strengths
 * are kept between 1 and the server's highest (an ability with nothing to scale is always 5); an unknown option becomes
 * the default ({@link HeroAbility#BOTH} for a hero's ability); a stance its power cannot take becomes automatic; an
 * ability whose needed one is missing is dropped ({@link HeroAbility#requires}); (3.5) a hero's ability picked with its
 * variant row keeps its main action only ({@link HeroAbility#MAIN}); a pick that drives a {@link Resource}
 * another owner's pick already drives is dropped; powers past {@code maxPowers}, then past the budget (a generic Sneak
 * variant costs half its power more), are dropped from the end of the list (the last picked go first); the bindings are
 * checked (below); moves and animation choices that name nothing known become automatic; layers not allowed or not known
 * are taken off; the skin is checked ({@link SkinCheck}) and dropped if it is not a 64x64 PNG within the size limit; a
 * look that names no known hero look, or a missing skin, becomes the player's own.
 *
 * <p>Bindings (3.4): a plain key names a picked power that takes a key. A hero's ability with a Sneak variant (its
 * option {@link HeroAbility#BOTH}) on plain key K puts its variant on K's Sneak cell ({@code <id>#alt}), whatever was
 * asked there. Otherwise a Sneak cell may name another <em>press</em> option of the module on its plain key
 * ({@code <module>#<option>}), or a picked power that takes a key and is not a hero's ability whose variant is on. Each
 * action is on one cell at most; whatever else a cell names is cleared.
 */
public final class CustomHeroRules {
    /** The name a hero gets when it is given none. */
    public static final String DEFAULT_NAME = "My Hero";

    /**
     * The limits a server sets.
     *
     * @param disabled    ids of the powers no custom hero may have here ({@code <hero>.*}: all of a hero's abilities)
     * @param heroModules 3.4: the built-in heroes' abilities may be picked
     * @param worldPowers 3.4: the world-scale abilities may be picked
     * @param layers      3.4: the look may have layers
     * @param spread      3.4: {@code heroStrengthSpread} (what the editor shows; the server reads the config when used)
     */
    public record Limits(int budget, int maxPowers, int maxStrength, int maxSkinBytes, Set<String> disabled, boolean heroModules,
                         boolean worldPowers, boolean layers, double spread) {
        /** The defaults of the config (what a server that changed nothing has). */
        public static final Limits DEFAULT = new Limits(90, 10, PowerModule.MAX_STRENGTH, 16384, Set.of(), true, true, true,
                HeroKit.DEFAULT_SPREAD);

        /** Limits as 3.3 had them: everything of 3.4 allowed. */
        public Limits(int budget, int maxPowers, int maxStrength, int maxSkinBytes, Set<String> disabled) {
            this(budget, maxPowers, maxStrength, maxSkinBytes, disabled, true, true, true, HeroKit.DEFAULT_SPREAD);
        }

        /** The limits of the server config (its defaults where it is not loaded, as on a client before it joins). */
        public static Limits current() {
            if (!EnhancedConfig.SPEC.isLoaded()) {
                return DEFAULT;
            }
            Set<String> disabled = new HashSet<>();
            for (String id : CustomHeroConfig.DISABLED_POWERS.get()) {
                disabled.add(id.trim().toLowerCase(Locale.ROOT));
            }
            return new Limits(CustomHeroConfig.BUDGET.get(), CustomHeroConfig.MAX_POWERS.get(), CustomHeroConfig.MAX_STRENGTH.get(),
                    CustomHeroConfig.MAX_SKIN_BYTES.get(), disabled, CustomHeroConfig.HERO_MODULES.get(), CustomHeroConfig.WORLD_POWERS.get(),
                    CustomHeroConfig.LAYERS.get(), CustomHeroConfig.HERO_STRENGTH_SPREAD.get());
        }

        public boolean allows(PowerModule module) {
            return !disabled.contains(module.id);
        }

        /** Whether the server's {@code disabledPowers} allow this entry (by its id, or all of its hero's). */
        public boolean allows(CatalogEntry entry) {
            if (disabled.contains(entry.id())) {
                return false;
            }
            if (entry instanceof HeroAbility a && a.parent() != null && disabled.contains(a.parent().id())) {
                return false; // (3.5: a variant row goes with its parent)
            }
            return !(entry instanceof HeroAbility a) || !disabled.contains(a.heroId() + ".*");
        }

        /** These limits with another budget. */
        public Limits withBudget(int budget) {
            return new Limits(budget, maxPowers, maxStrength, maxSkinBytes, disabled, heroModules, worldPowers, layers, spread);
        }
    }

    /** Something the rules changed: a lang key and its arguments. */
    public record Note(String key, Object... args) {
    }

    /** The hero the rules allow, and what was changed to get it. */
    public record Result(CustomHero hero, List<Note> notes) {
        public boolean changed() {
            return !notes.isEmpty();
        }
    }

    public static Result sanitize(CustomHero in, Limits limits) {
        List<Note> notes = new ArrayList<>();
        String name = cleanName(in.name());
        if (name.isEmpty()) {
            name = DEFAULT_NAME;
        }
        int maxStrength = Math.max(1, Math.min(PowerModule.MAX_STRENGTH, limits.maxStrength()));

        // ---- the picks ----
        List<CustomHero.Pick> picks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CustomHero.Pick pick : in.powers()) {
            CatalogEntry entry = pick.entry();
            if (entry == null) {
                notes.add(new Note("message.enhanced.hero_creator.rule.unknown", pick.id()));
                continue;
            }
            if (!limits.allows(entry)) {
                notes.add(new Note("message.enhanced.hero_creator.rule.disabled", entry.nameKey()));
                continue;
            }
            if (entry instanceof HeroAbility && !limits.heroModules()) {
                notes.add(new Note("message.enhanced.hero_creator.rule.hero_modules", entry.nameKey()));
                continue;
            }
            if (entry.world() && !limits.worldPowers()) {
                notes.add(new Note("message.enhanced.hero_creator.rule.world", entry.nameKey()));
                continue;
            }
            if (!seen.add(entry.id())) {
                continue;
            }
            int strength = pick.strength();
            if (!entry.scales()) {
                strength = HeroAbility.FIXED_STRENGTH;
            } else if (strength < 1 || strength > maxStrength) {
                strength = Math.max(1, Math.min(maxStrength, strength));
                notes.add(new Note("message.enhanced.hero_creator.rule.strength", entry.nameKey(), strength));
            }
            String option;
            if (entry instanceof HeroAbility a) {
                option = a.hasAlt() && HeroAbility.MAIN.equals(pick.option()) ? HeroAbility.MAIN : HeroAbility.BOTH;
            } else {
                PowerModule module = (PowerModule) entry;
                option = module.option(pick.option()) == null ? module.defaultOption().id() : pick.option();
            }
            int color = pick.color() < 0 ? -1 : pick.color() & 0xFFFFFF;
            String stance = AnimCatalog.isStance(pick.stance(), stanceAct(entry, option)) ? pick.stance() : CustomHero.AUTO;
            picks.add(new CustomHero.Pick(entry.id(), strength, option, color, stance));
        }

        // ---- what an ability needs ----
        boolean dropped = true;
        while (dropped) {
            dropped = false;
            for (int i = 0; i < picks.size(); i++) {
                if (picks.get(i).entry() instanceof HeroAbility a && !a.requires().isEmpty()
                        && picks.stream().noneMatch(p -> a.requires().contains(p.id()))) {
                    HeroAbility needed = HeroAbility.byId(a.requires().get(0));
                    notes.add(new Note("message.enhanced.hero_creator.rule.requires", a.nameKey(),
                            needed == null ? a.requires().get(0) : needed.nameKey()));
                    picks.remove(i);
                    dropped = true;
                    break;
                }
            }
        }

        // ---- 3.5: a variant row picked with its parent: the parent keeps its main action (the variant is the row's)
        // ----
        for (int i = 0; i < picks.size(); i++) {
            CustomHero.Pick pick = picks.get(i);
            if (pick.entry() instanceof HeroAbility a && a.hasAlt() && HeroAbility.BOTH.equals(pick.option())
                    && a.variant() != null
                    && find(picks, a.variant().id()) != null) {
                picks.set(i, pick.withOption(HeroAbility.MAIN));
            }
        }

        // ---- the resources: one owner each ----
        Map<Resource, CustomHero.Pick> holders = new EnumMap<>(Resource.class);
        List<CustomHero.Pick> kept = new ArrayList<>();
        for (CustomHero.Pick pick : picks) {
            CatalogEntry entry = pick.entry();
            CustomHero.Pick clash = null;
            Resource clashing = null;
            for (Resource r : entry.resources(pick.option())) {
                CustomHero.Pick holder = holders.get(r);
                if (holder != null && owner(holder.entry()) != owner(entry)) {
                    clash = holder;
                    clashing = r;
                    break;
                }
            }
            if (clash != null) {
                notes.add(new Note("message.enhanced.hero_creator.rule.resource", entry.nameKey(), resourceKey(clashing),
                        clash.entry().nameKey()));
                continue;
            }
            for (Resource r : entry.resources(pick.option())) {
                holders.putIfAbsent(r, pick);
            }
            kept.add(pick);
        }
        picks = kept;

        // ---- how many, and the budget ----
        while (picks.size() > limits.maxPowers()) {
            CustomHero.Pick out = picks.remove(picks.size() - 1);
            notes.add(new Note("message.enhanced.hero_creator.rule.too_many", out.entry().nameKey(), limits.maxPowers()));
        }
        Keys keys = keys(in, picks, null);
        while (cost(picks, keys.sneak) > limits.budget() && !picks.isEmpty()) {
            CustomHero.Pick out = picks.remove(picks.size() - 1);
            notes.add(new Note("message.enhanced.hero_creator.rule.budget", out.entry().nameKey(), limits.budget()));
            keys = keys(in, picks, null);
        }
        keys = keys(in, picks, notes);

        // ---- moves, animation, layers ----
        List<String> moves = new ArrayList<>(in.moves());
        moves.replaceAll(m -> AnimCatalog.isMove(m) ? m : CustomHero.AUTO);
        CustomHero.Anim a = in.anim();
        CustomHero.Anim anim = new CustomHero.Anim(choice(a.flight(), AnimCatalog.FLIGHT_STYLES), choice(a.run(), AnimCatalog.RUN_CHARACTERS),
                choice(a.speedRun(), AnimCatalog.SPEED_RUNS), choice(a.idle(), AnimCatalog.IDLES));
        List<CustomHero.Layer> layers = new ArrayList<>();
        for (int kind = 0; kind < CustomHero.Layers.KINDS.size(); kind++) {
            CustomHero.Layer layer = in.layers().get(kind);
            if (layer.isNone()) {
                layers.add(CustomHero.Layer.NONE_LAYER);
            } else if (!limits.layers() || !LookCatalog.styles(kind).contains(layer.style())) {
                notes.add(new Note("message.enhanced.hero_creator.rule.layer",
                        "message.enhanced.hero_creator.layer." + CustomHero.Layers.KINDS.get(kind)));
                layers.add(CustomHero.Layer.NONE_LAYER);
            } else {
                int color = layer.color() < 0 ? -1 : layer.color() & 0xFFFFFF;
                int row = kind == 3 ? Math.max(0, Math.min(LookCatalog.EYE_ROWS - 1, layer.row())) : 0;
                String when = LookCatalog.hasWhen(kind) && LookCatalog.WHEN.contains(layer.when()) ? layer.when() : CustomHero.Layer.ALWAYS;
                layers.add(new CustomHero.Layer(layer.style(), color, row, when));
            }
        }

        // ---- the skin and the look ----
        byte[] skin = in.skin();
        if (skin.length > 0) {
            SkinCheck.Verdict verdict = SkinCheck.check(skin, Math.min(limits.maxSkinBytes(), CustomHero.MAX_SKIN_WIRE));
            if (verdict != SkinCheck.Verdict.OK) {
                notes.add(new Note(verdict.messageKey(), limits.maxSkinBytes() / 1024));
                skin = new byte[0];
            }
        }
        String look = in.look();
        boolean slim = in.slim();
        if (CustomHero.LOOK_SKIN.equals(look)) {
            if (skin.length == 0) {
                look = CustomHero.LOOK_OWN;
                slim = false;
            }
        } else if (!CustomHero.LOOK_OWN.equals(look)) {
            HeroLooks.Look known = HeroLooks.byRef(look);
            if (known == null) {
                notes.add(new Note("message.enhanced.hero_creator.rule.look"));
                look = CustomHero.LOOK_OWN;
                slim = false;
            } else {
                slim = known.slim();
            }
        } else {
            slim = false;
        }
        CustomHero out = new CustomHero(CustomHero.VERSION, name, in.color() & 0xFFFFFF, in.glow() & 0xFFFFFF, look, slim, skin, picks,
                keys.plain, keys.sneak, moves, anim, CustomHero.Layers.of(layers));
        return new Result(out, notes);
    }

    /** How an entry acts for the stance it may take: held or switched on, by its main action or its Sneak variant. */
    private static PowerModule.Act stanceAct(CatalogEntry entry, String option) {
        PowerModule.Act act = entry.act(option);
        if (act == PowerModule.Act.HOLD || act == PowerModule.Act.TOGGLE) {
            return act;
        }
        PowerModule.Act alt = entry.alt();
        return alt != null && HeroAbility.BOTH.equals(option) ? alt : act;
    }

    /** Who owns a resource: the hero of an ability, or {@link Power#NONE} for every generic module. */
    private static Power owner(CatalogEntry entry) {
        Power hero = entry.hero();
        return hero == null ? Power.NONE : hero;
    }

    /** The lang key of a resource's name, for the notes. */
    public static String resourceKey(Resource r) {
        return "message.enhanced.hero_creator.resource." + r.name().toLowerCase(Locale.ROOT);
    }

    private static String choice(String value, List<String> allowed) {
        return CustomHero.AUTO.equals(value) || allowed.contains(value) ? value : CustomHero.AUTO;
    }

    /** The bindings the rules allow. */
    private record Keys(List<String> plain, List<String> sneak) {
    }

    /** The bindings of {@code in} the picks allow (see the class comment); notes the cleared ones when {@code notes} is given. */
    private static Keys keys(CustomHero in, List<CustomHero.Pick> picks, @Nullable List<Note> notes) {
        Map<Resource, Power> owners = new EnumMap<>(Resource.class);
        for (CustomHero.Pick pick : picks) {
            for (Resource r : pick.entry().resources(pick.option())) {
                owners.putIfAbsent(r, owner(pick.entry()));
            }
        }
        List<String> plain = new ArrayList<>();
        List<String> sneak = new ArrayList<>();
        Set<String> actions = new HashSet<>();
        // The plain keys first: every action they hold is taken before a Sneak cell may name it.
        for (int column = 1; column <= CustomHero.KEYS; column++) {
            String id = in.binding(column, false).trim().toLowerCase(Locale.ROOT);
            CustomHero.Pick pick = id.contains("#") ? null : find(picks, id);
            if (pick == null || !takesKey(pick) || !actions.add(pick.id() + "#" + HeroKit.MAIN)) {
                if (notes != null && !id.isEmpty()) {
                    notes.add(new Note("message.enhanced.hero_creator.rule.key", column));
                }
                plain.add("");
            } else {
                plain.add(pick.id());
            }
        }
        for (int column = 1; column <= CustomHero.KEYS; column++) {
            String asked = in.binding(column, true).trim().toLowerCase(Locale.ROOT);
            CustomHero.Pick onPlain = find(picks, plain.get(column - 1));
            if (onPlain != null && onPlain.entry() instanceof HeroAbility a && a.hasAlt() && HeroAbility.BOTH.equals(onPlain.option())) {
                String forced = a.id() + "#" + HeroKit.ALT;
                actions.add(forced);
                if (notes != null && !asked.isEmpty() && !asked.equals(forced)) {
                    notes.add(new Note("message.enhanced.hero_creator.rule.key", column));
                }
                sneak.add(forced);
                continue;
            }
            String ok = sneakCell(asked, onPlain, picks, owners, actions);
            if (ok == null) {
                if (notes != null && !asked.isEmpty()) {
                    notes.add(new Note("message.enhanced.hero_creator.rule.key", column));
                }
                sneak.add("");
            } else {
                sneak.add(ok);
            }
        }
        return new Keys(plain, sneak);
    }

    /** What a Sneak cell may hold of what was asked (taking its action), or null. */
    @Nullable
    private static String sneakCell(String asked, @Nullable CustomHero.Pick onPlain, List<CustomHero.Pick> picks,
                                    Map<Resource, Power> owners, Set<String> actions) {
        if (asked.isEmpty()) {
            return null;
        }
        int hash = asked.indexOf('#');
        if (hash >= 0) {
            String id = asked.substring(0, hash);
            String option = asked.substring(hash + 1);
            if (onPlain == null || !onPlain.id().equals(id) || !(onPlain.entry() instanceof PowerModule module)) {
                return null;
            }
            PowerModule.Option o = module.option(option);
            if (o == null || o.act() != PowerModule.Act.PRESS || option.equals(onPlain.option())) {
                return null;
            }
            for (Resource r : module.resources(option)) {
                Power owner = owners.get(r);
                if (owner != null && owner != Power.NONE) {
                    return null; // (that option drives what a hero's ability of his already drives)
                }
            }
            return actions.add(asked) ? asked : null;
        }
        CustomHero.Pick pick = find(picks, asked);
        if (pick == null || !takesKey(pick)) {
            return null;
        }
        if (pick.entry() instanceof HeroAbility a && a.hasAlt() && HeroAbility.BOTH.equals(pick.option())) {
            return null; // (its Sneak variant could go nowhere: it belongs on the Sneak cell of its own plain key)
        }
        return actions.add(pick.id() + "#" + HeroKit.MAIN) ? pick.id() : null;
    }

    /** Whether a pick may sit on a key box: it has an action, and is no hero's ability with a key of its own (Build Magic's Y). */
    private static boolean takesKey(CustomHero.Pick pick) {
        return pick.act() != PowerModule.Act.NONE && !(pick.entry() instanceof HeroAbility a && !a.takesBox());
    }

    @Nullable
    private static CustomHero.Pick find(List<CustomHero.Pick> picks, String id) {
        for (CustomHero.Pick pick : picks) {
            if (pick.id().equals(id)) {
                return pick;
            }
        }
        return null;
    }

    /** The power points a list of picks costs (without generic Sneak variants). */
    public static int cost(List<CustomHero.Pick> picks) {
        int total = 0;
        for (CustomHero.Pick pick : picks) {
            CatalogEntry entry = pick.entry();
            if (entry != null) {
                total += entry.cost(pick.strength());
            }
        }
        return total;
    }

    /**
     * The power points of picks with their Sneak bindings (3.4): a Sneak cell that holds another option of a module
     * ({@code <module>#<option>}) costs half that module more (rounded up).
     */
    public static int cost(List<CustomHero.Pick> picks, List<String> sneakKeys) {
        int total = cost(picks);
        for (String cell : sneakKeys) {
            int hash = cell.indexOf('#');
            if (hash > 0 && !cell.endsWith("#" + HeroKit.ALT)) {
                CustomHero.Pick pick = find(picks, cell.substring(0, hash));
                if (pick != null && pick.entry() != null) {
                    total += (pick.entry().cost(pick.strength()) + 1) / 2;
                }
            }
        }
        return total;
    }

    /** What a hero costs: his picks and his generic Sneak variants. */
    public static int cost(CustomHero hero) {
        return cost(hero.powers(), hero.sneakKeys());
    }

    /** A name with its formatting codes, control characters and outer spaces taken out, cut to the longest allowed. */
    public static String cleanName(String raw) {
        StringBuilder out = new StringBuilder();
        boolean skipNext = false;
        for (int cp : raw.codePoints().toArray()) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (cp == '§') {
                skipNext = true;
                continue;
            }
            if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT && cp != 0x200F && cp != 0x200E) {
                continue;
            }
            out.appendCodePoint(cp);
        }
        String name = out.toString().strip().replaceAll("\\s+", " ");
        if (name.codePointCount(0, name.length()) > CustomHero.MAX_NAME) {
            name = name.substring(0, name.offsetByCodePoints(0, CustomHero.MAX_NAME)).strip();
        }
        return name;
    }

    private CustomHeroRules() {
    }
}
