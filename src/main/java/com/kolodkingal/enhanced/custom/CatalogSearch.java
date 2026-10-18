package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.power.Power;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The search box of the Hero Creator's Powers tab (3.4): does an entry of the catalogue match what was typed? It looks at
 * the entry's name and line in the player's language, its options' names, its id and its hero's name, ignoring case,
 * accents and Hebrew vowel points (and the end forms of Hebrew letters, so a word typed half way still matches). Every
 * word typed must be found. Pure: the words come through {@code lang} (a lang key to its text), so the tests run it with
 * the English and the Hebrew files.
 */
public final class CatalogSearch {

    /** Whether {@code entry} matches {@code query} (an empty query matches everything). */
    public static boolean matches(CatalogEntry entry, String query, Function<String, String> lang) {
        String q = fold(query);
        if (q.isBlank()) {
            return true;
        }
        String text = fold(String.join(" ", words(entry, lang)));
        for (String word : q.trim().split("\\s+")) {
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /** Every text an entry is found by. */
    private static List<String> words(CatalogEntry entry, Function<String, String> lang) {
        List<String> words = new ArrayList<>();
        words.add(entry.id().replace('_', ' ').replace('.', ' '));
        words.add(text(lang, entry.nameKey()));
        words.add(text(lang, entry.descKey()));
        if (entry instanceof PowerModule m) {
            for (PowerModule.Option o : m.options()) {
                words.add(text(lang, m.optionKey(o.id())));
            }
        }
        if (entry instanceof HeroAbility a) {
            if (a.hasAlt()) {
                words.add(text(lang, a.altKey()));
            }
            Power hero = a.hero;
            words.add(text(lang, "power.enhanced." + hero.getSerializedName()));
            words.add(a.heroId());
        }
        return words;
    }

    private static String text(Function<String, String> lang, String key) {
        String s = lang.apply(key);
        return s == null || s.equals(key) ? "" : s;
    }

    /** Lower case, without accents, Hebrew points or end forms. */
    public static String fold(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            out.append(switch (c) {
                case 'ך' -> 'כ';
                case 'ם' -> 'מ';
                case 'ן' -> 'נ';
                case 'ף' -> 'פ';
                case 'ץ' -> 'צ';
                case '-', '_', '\'', '’' -> ' ';
                default -> c;
            });
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private CatalogSearch() {
    }
}
