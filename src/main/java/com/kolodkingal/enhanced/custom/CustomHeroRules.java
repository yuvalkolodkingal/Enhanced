package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CustomHeroConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The server's judgement of a custom hero (3.3): every definition a client sends or a file brings passes here before it
 * becomes anyone's hero. It never refuses a hero outright: it makes the best hero the rules allow, and says what it
 * changed (each note a lang key with its arguments, shown to the player). Pure (no world, no player), so the client's
 * screen runs it too, with the limits the server synced, and the tests run it directly.
 *
 * <p>In order: the name is cleaned (formatting codes and control characters out, at most {@link CustomHero#MAX_NAME}
 * characters, "My Hero" when nothing is left); the colours are made opaque RGB; powers this version does not know, the
 * server's disabled ones and repeats are dropped; strengths are kept between 1 and the server's highest; an unknown option
 * becomes the module's default; powers past {@code maxPowers}, then past the budget, are dropped from the end of the
 * list (the last picked go first); a key that names a power the hero does not have, or a passive one, is cleared; the
 * skin is checked ({@link SkinCheck}) and dropped if it is not a 64x64 PNG within the size limit; a look that names no
 * known hero look, or a missing skin, becomes the player's own.
 */
public final class CustomHeroRules {
    /** The name a hero gets when it is given none. */
    public static final String DEFAULT_NAME = "My Hero";

    /**
     * The limits a server sets.
     *
     * @param disabled ids of the powers no custom hero may have here
     */
    public record Limits(int budget, int maxPowers, int maxStrength, int maxSkinBytes, Set<String> disabled) {
        /** The defaults of the config (what a server that changed nothing has). */
        public static final Limits DEFAULT = new Limits(60, 8, PowerModule.MAX_STRENGTH, 16384, Set.of());

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
                    CustomHeroConfig.MAX_SKIN_BYTES.get(), disabled);
        }

        public boolean allows(PowerModule module) {
            return !disabled.contains(module.id);
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

        List<CustomHero.Pick> picks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CustomHero.Pick pick : in.powers()) {
            PowerModule module = pick.module();
            if (module == null) {
                notes.add(new Note("message.enhanced.hero_creator.rule.unknown", pick.id()));
                continue;
            }
            if (!limits.allows(module)) {
                notes.add(new Note("message.enhanced.hero_creator.rule.disabled", module.nameKey()));
                continue;
            }
            if (!seen.add(module.id)) {
                continue;
            }
            int strength = pick.strength();
            if (strength < 1 || strength > maxStrength) {
                strength = Math.max(1, Math.min(maxStrength, strength));
                notes.add(new Note("message.enhanced.hero_creator.rule.strength", module.nameKey(), strength));
            }
            String option = module.option(pick.option()) == null ? module.defaultOption().id() : pick.option();
            int color = pick.color() < 0 ? -1 : pick.color() & 0xFFFFFF;
            picks.add(new CustomHero.Pick(module.id, strength, option, color));
        }
        while (picks.size() > limits.maxPowers()) {
            CustomHero.Pick dropped = picks.remove(picks.size() - 1);
            notes.add(new Note("message.enhanced.hero_creator.rule.too_many", dropped.module().nameKey(), limits.maxPowers()));
        }
        while (cost(picks) > limits.budget() && !picks.isEmpty()) {
            CustomHero.Pick dropped = picks.remove(picks.size() - 1);
            notes.add(new Note("message.enhanced.hero_creator.rule.budget", dropped.module().nameKey(), limits.budget()));
        }

        List<String> keys = new ArrayList<>();
        CustomHero partial = in.withPowers(picks);
        for (int slot = 1; slot <= CustomHero.KEYS; slot++) {
            String id = in.keys().get(slot - 1).trim().toLowerCase(Locale.ROOT);
            PowerModule module = PowerModule.byId(id);
            CustomHero.Pick pick = module == null ? null : partial.pick(module);
            if (pick == null || pick.act() == PowerModule.Act.NONE || keys.contains(module.id)) {
                if (!id.isEmpty()) {
                    notes.add(new Note("message.enhanced.hero_creator.rule.key", slot));
                }
                keys.add("");
            } else {
                keys.add(module.id);
            }
        }

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
        CustomHero out = new CustomHero(CustomHero.VERSION, name, in.color() & 0xFFFFFF, in.glow() & 0xFFFFFF, look, slim, skin, picks, keys);
        return new Result(out, notes);
    }

    /** The power points a list of picks costs. */
    public static int cost(List<CustomHero.Pick> picks) {
        int total = 0;
        for (CustomHero.Pick pick : picks) {
            PowerModule module = pick.module();
            if (module != null) {
                total += module.cost(pick.strength());
            }
        }
        return total;
    }

    public static int cost(CustomHero hero) {
        return cost(hero.powers());
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
