package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import org.jetbrains.annotations.Nullable;

/**
 * 3.18.1, the witches' ability icons (pure: both sides and the tests read it). Every spell on a witch's keys has a symbol
 * of its own, as the heroes' abilities have ({@code textures/gui/ability/<hero>_<n>.png}, drawn by tools/symbols.py from
 * the motif tools/symbols.json gives its key): {@code ability.enhanced.<witch>.<spell>}, in her colours, where
 * {@code <witch>} is her hero id ({@link Spellbook#heroId}) and {@code <spell>} is what the binding does
 * ({@code agatha_purple_beam}, {@code magic_orb}, {@code earth_wall}, {@code nature_roots}, {@code cantrip_ward}...). The
 * cantrips of anyone else who has them (a witch of the Demiurge's gift, a custom hero the server gives them to) are
 * {@code ability.enhanced.cantrip.<cantrip>}.
 */
public final class WitchIcons {
    /** The "hero" of the cantrips of anyone who is no book's witch. */
    public static final String CANTRIP = "cantrip";

    private WitchIcons() {
    }

    /**
     * The icon key of a binding of this definition ({@code ability.enhanced.green_witch.nature_roots}), or null when it
     * has none (the definition is no book's and the binding no cantrip).
     */
    @Nullable
    public static String key(CustomHero hero, HeroKit.Binding b) {
        Spellbook book = Spellbook.of(hero);
        if (book != null) {
            String spell = spell(hero, b);
            return spell == null ? null : "ability.enhanced." + book.heroId + "." + spell;
        }
        if (b.module() == PowerModule.CANTRIP) {
            return "ability.enhanced." + CANTRIP + "." + b.action();
        }
        return null;
    }

    /** The texture path (under the mod's namespace) of an icon key. */
    public static String texture(String key) {
        return "textures/gui/ability/" + key.substring("ability.enhanced.".length()).replace('.', '_') + ".png";
    }

    /**
     * What a binding does, as an icon's name: a hero's ability by its id (its Sneak variant with {@code _alt}, a variant
     * row by its own id), a power by its id and the option it does ({@code magic_orb}; Flight, and a power of one
     * option, by its id alone), a cantrip as {@code cantrip_<cantrip>}. Null for a stone row.
     */
    @Nullable
    public static String spell(CustomHero hero, HeroKit.Binding b) {
        if (b.picked() instanceof HeroAbility v && v.isVariant()) {
            return slug(v.id());
        }
        CatalogEntry entry = b.entry();
        if (entry instanceof HeroAbility a) {
            return slug(a.id()) + (b.isAlt() ? "_alt" : "");
        }
        PowerModule m = b.module();
        if (m == null) {
            return null;
        }
        if (m == PowerModule.CANTRIP) {
            return CANTRIP + "_" + b.action();
        }
        if (!HeroKit.MAIN.equals(b.action())) {
            return slug(m.id) + "_" + b.action();
        }
        CustomHero.Pick pick = hero.pick(m);
        if (m == PowerModule.FLIGHT || pick == null || m.options().size() <= 1 || m.option(pick.option()) == null) {
            return slug(m.id);
        }
        return slug(m.id) + "_" + pick.option();
    }

    private static String slug(String id) {
        return id.replace('.', '_').replace('#', '_');
    }
}
