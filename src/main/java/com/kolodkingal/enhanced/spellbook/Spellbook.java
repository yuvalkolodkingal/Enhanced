package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The three spellbooks and the kind of caster each makes of a player who has no power of his own:
 * <ul>
 * <li>{@link #ETHEREAL}, the Book of Ethereal Witchcraft (on the lectern in Agatha's room, in her cottage): Agatha's
 * magic without the power drain (her purple beam with its hex mark, flight, her shield, her rune trap and her durable body)
 * and, in the drain's place, the Ethereal Orb, a ball of purple energy. The book puts Agatha's witch hat on the caster;
 * using it again takes the hat off or puts it back on, and Sneak + use chooses its colour.</li>
 * <li>{@link #ELDRITCH}, the Book of Eldritch Witchcraft (in the library of the Sanctum Sanctorum, the mod's home of the
 * sorcerers of Kamar-Taj): Doctor Strange's Eldritch Whip (Sneak: the grapple), eldritch bolts, the Shields of the
 * Seraphim (Sneak: the dome), the Mirror Dimension (Sneak: the astral form) and the Sling Ring's portals.</li>
 * <li>{@link #GREEN}, the Book of Green Witchcraft (on a lectern in Rio Vidal's room, Death's, in the same cottage):
 * thrown earth (Sneak: an Earth Wall that crumbles by itself), growth round the caster, Agatha's rune trap in a weaker
 * form, and a body that heals faster.</li>
 * </ul>
 * A caster is a custom hero ({@code Power.CUSTOM}) whose definition is the book's ({@link #definition}): he keeps his own
 * skin, the keys and the panel are those of every custom hero, and every client knows his kind from his synced definition
 * ({@link #of(CustomHero)}). {@link Spellbooks} makes and unmakes him.
 */
public enum Spellbook {
    ETHEREAL("ethereal", "Ethereal Witch", 0xA64DFF, 0xE6C8FF),
    ELDRITCH("eldritch", "Eldritch Sorcerer", 0xF29A2E, 0xFFE0A8),
    GREEN("green", "Green Witch", 0x3FBF4A, 0xC8F5A0);

    /** The limits the books' own definitions are judged by: every power allowed (the server's creator limits are for heroes made in the creator). */
    private static final CustomHeroRules.Limits LIMITS = new CustomHeroRules.Limits(10_000, 20, PowerModule.MAX_STRENGTH, 16384,
            Set.of());

    public final String id;
    /** The name in the caster's definition (what an older client, or the creator, shows); the panel shows {@link #title}. */
    public final String casterName;
    public final int color;
    public final int glow;

    /** 3.18.1: the definition of each level (index 1-5), without the cantrips [0] and with them [1], judged once. */
    private final CustomHero[][] levels = new CustomHero[2][SpellLevels.MAX + 1];
    /** The definition of before the levels (all of the book, no cantrips): an older save's caster is known by it. */
    @Nullable
    private CustomHero legacy;

    Spellbook(String id, String casterName, int color, int glow) {
        this.id = id;
        this.casterName = casterName;
        this.color = color;
        this.glow = glow;
    }

    /** The caster's name, in the player's language ("Ethereal Witch"). */
    public Component title() {
        return Component.translatable("spellbook.enhanced." + id + ".caster");
    }

    /** The book's name ("Book of Ethereal Witchcraft"). */
    public Component bookName() {
        return Component.translatable("item.enhanced." + itemId());
    }

    /** The id of the book's item. */
    public String itemId() {
        return "book_of_" + id + "_witchcraft";
    }

    /**
     * The caster's definition at level 5 (all of the book), with the cantrips if the server gives them: see
     * {@link #definition(int, int, boolean)}.
     */
    public CustomHero definition(int hatColor, boolean hat) {
        return definition(SpellLevels.MAX, hatColor, hat);
    }

    /**
     * 3.18.1: the caster's definition at {@code level} (1-5): the book's powers that level opens, on their keys, at its
     * strengths ({@link SpellLevels}), a cantrip on each key still empty when the server gives them
     * ({@code spellbook_levels.defaults.enabled}), his own skin, and (the Ethereal witch) Agatha's hat in
     * {@code hatColor}, or no hat when {@code hat} is false.
     */
    public CustomHero definition(int level, int hatColor, boolean hat) {
        return withHat(base(level, WitchLevels.cantripsOn()), hatColor, hat);
    }

    /** {@code base} with (the Ethereal witch) Agatha's hat in {@code hatColor}, or as it is. */
    public CustomHero withHat(CustomHero base, int hatColor, boolean hat) {
        if (this != ETHEREAL || !hat) {
            return base;
        }
        return base.withLayers(base.layers().with(1, new CustomHero.Layer("witch_hat", hatColor & 0xFFFFFF, 0, CustomHero.Layer.ALWAYS)));
    }

    /**
     * 3.18.1: the definition of a level without a hat, judged once by the rules: the level's picks and keys
     * ({@link SpellLevels#bookLevel}), the bindings the level adds after the rules (the Green witch's Root Grasp), every
     * ability of a real hero at most at its strength 5 ({@link SpellLevels#capToHero}), and the cantrips if {@code cantrips}.
     * Level 5 without cantrips is the book's definition of before (and the Green witch's Root Grasp).
     */
    public synchronized CustomHero base(int level, boolean cantrips) {
        int l = Math.max(1, Math.min(SpellLevels.MAX, level));
        int f = cantrips ? 1 : 0;
        if (levels[f][l] == null) {
            CustomHero ruled = CustomHeroRules.sanitize(SpellLevels.bookLevel(this, raw(), l), LIMITS).hero();
            ruled = SpellLevels.capToHero(SpellLevels.extras(this, ruled, l));
            levels[f][l] = cantrips ? SpellLevels.fill(ruled, l) : ruled;
        }
        return levels[f][l];
    }

    /** The book's definition of before the levels (all of it, judged by the rules): how an older save's caster is known. */
    public synchronized CustomHero legacy() {
        if (legacy == null) {
            legacy = CustomHeroRules.sanitize(raw(), LIMITS).hero();
        }
        return legacy;
    }

    /** The book's whole definition, before the rules (level 5 is made from it). */
    public CustomHero raw() {
        CustomHero hero = CustomHero.EMPTY.withName(casterName).withColors(color, glow).withLook(CustomHero.LOOK_OWN, false);
        return switch (this) {
            // Agatha's keys, the Ethereal Orb on V where her drain was.
            case ETHEREAL -> hero.withPowers(List.of(
                            pick("agatha.purple_beam", 5, "both"),
                            pick("flight", 4, "hover"),
                            pick("magic", 6, "orb"),
                            pick("agatha.shield", 5, "both"),
                            pick("agatha.runes", 5, "both"),
                            pick("agatha.body", 3, "both")))
                    .withKeys(List.of("agatha.purple_beam", "flight", "magic", "agatha.shield", "agatha.runes"))
                    .withAnim(new CustomHero.Anim("agatha", "agatha", CustomHero.AUTO, CustomHero.NONE));
            // Strange's whip, eldritch bolts, his shields, the Mirror Dimension and the astral form, the portals.
            case ELDRITCH -> hero.withPowers(List.of(
                            pick("strange.whip", 5, "both"),
                            pick("magic", 5, "bolts"),
                            pick("strange.shields", 5, "both"),
                            pick("strange.mirror", 5, "both"),
                            pick("strange.astral", 5, "both"),
                            pick("strange.portal", 5, "main")))
                    .withKeys(List.of("strange.whip", "magic", "strange.shields", "strange.mirror", "strange.portal"))
                    .withSneakKeys(List.of("", "", "", "strange.astral", ""))
                    .withAnim(new CustomHero.Anim(CustomHero.AUTO, "doctor_strange", CustomHero.AUTO, CustomHero.NONE));
            // The green magic: earth thrown (Sneak: the wall), growth, a weak rune trap; a body that heals faster.
            case GREEN -> hero.withPowers(List.of(
                            pick("earth", 5, "throw"),
                            pick("nature", 5, "bloom"),
                            pick("agatha.runes", 2, "both"),
                            pick("healing", 5, "steady")))
                    .withKeys(List.of("earth", "nature", "agatha.runes", "", ""))
                    .withSneakKeys(List.of("earth#wall", "", "", "", ""));
        };
    }

    private static CustomHero.Pick pick(String id, int strength, String option) {
        return new CustomHero.Pick(id, strength, option, -1);
    }

    /** The book of this id, or null. */
    @Nullable
    public static Spellbook byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (Spellbook book : values()) {
            if (book.id.equals(wanted)) {
                return book;
            }
        }
        return null;
    }

    /** 3.18.1: a book and the level a caster's definition is of. */
    public record Level(Spellbook book, int level) {
    }

    /** The book whose definition this is (whatever its hat and its level), or null for any other hero. */
    @Nullable
    public static Spellbook of(CustomHero hero) {
        Level match = match(hero);
        return match == null ? null : match.book();
    }

    /**
     * 3.18.1: the book and level of a caster's definition (at any of its five levels, with or without the cantrips,
     * whatever its hat; the definition of before the levels is level 5), or null for any other hero.
     */
    @Nullable
    public static Level match(CustomHero hero) {
        for (Spellbook book : values()) {
            if (!hero.name().equals(book.casterName)) {
                continue;
            }
            for (int level = SpellLevels.MAX; level >= 1; level--) {
                if (same(hero, book.base(level, true)) || same(hero, book.base(level, false))) {
                    return new Level(book, level);
                }
            }
            if (same(hero, book.legacy())) {
                return new Level(book, SpellLevels.MAX);
            }
        }
        return null;
    }

    private static boolean same(CustomHero hero, CustomHero def) {
        return hero.name().equals(def.name()) && hero.powers().equals(def.powers()) && hero.keys().equals(def.keys())
                && hero.sneakKeys().equals(def.sneakKeys());
    }

    /** The book this player is the caster of (on either side), or null. */
    @Nullable
    public static Spellbook of(Player player) {
        return Powers.power(player) == Power.CUSTOM ? of(CustomHeroes.hero(player)) : null;
    }

    /** The caster's name for the panel and the messages, if this definition is a book's (else null). */
    @Nullable
    public static Component title(CustomHero hero) {
        Spellbook book = of(hero);
        return book == null ? null : book.title();
    }
}
