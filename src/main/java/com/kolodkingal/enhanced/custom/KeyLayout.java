package com.kolodkingal.enhanced.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * The Keys tab's "Auto" button (3.4): lays a hero's powers out on the five keys the way a built-in hero has them. A
 * built-in hero's ability goes on its own key (Thor's lightning on V) when that key is free; then the generic powers by
 * kind (Energy and Elements on R, Move on G, Mind on V, Body on B, anything else on N), each on the next free key after
 * its own when that one is taken; what still has no key takes a free Sneak cell. The Sneak variants of the heroes'
 * abilities go beside their main by the rules ({@link CustomHeroRules}), which the result has passed. Powers that need
 * no key (and Billy's Build Magic, which has its own Y key) get none. Pure.
 */
public final class KeyLayout {

    /** The hero with his keys laid out (his picks, moves, looks unchanged; the moves of the keys go back to automatic). */
    public static CustomHero auto(CustomHero hero, CustomHeroRules.Limits limits) {
        String[] plain = new String[CustomHero.KEYS + 1];
        String[] sneak = new String[CustomHero.KEYS + 1];
        List<CustomHero.Pick> later = new ArrayList<>();
        // The heroes' abilities first, on their home keys.
        for (CustomHero.Pick pick : hero.powers()) {
            CatalogEntry entry = pick.entry();
            if (entry == null || pick.act() == PowerModule.Act.NONE) {
                continue;
            }
            if (entry instanceof HeroAbility a) {
                if (a.home < 1 || a.home > CustomHero.KEYS) {
                    continue;
                }
                if (plain[a.home] == null) {
                    plain[a.home] = a.id();
                    continue;
                }
            }
            later.add(pick);
        }
        // Then the rest by kind, and what found no key.
        List<CustomHero.Pick> homeless = new ArrayList<>();
        for (CustomHero.Pick pick : later) {
            int column = freeFrom(plain, preferred(pick.entry()));
            if (column > 0) {
                plain[column] = pick.id();
            } else {
                homeless.add(pick);
            }
        }
        for (CustomHero.Pick pick : homeless) {
            if (pick.entry() instanceof HeroAbility a && a.hasAlt() && HeroAbility.BOTH.equals(pick.option())) {
                continue; // (its variant needs the Sneak cell of its own plain key)
            }
            for (int column = 1; column <= CustomHero.KEYS; column++) {
                if (sneak[column] == null && !hasVariant(hero, plain[column])) {
                    sneak[column] = pick.id();
                    break;
                }
            }
        }
        List<String> keys = new ArrayList<>();
        List<String> sneakKeys = new ArrayList<>();
        for (int column = 1; column <= CustomHero.KEYS; column++) {
            keys.add(plain[column] == null ? "" : plain[column]);
            sneakKeys.add(sneak[column] == null ? "" : sneak[column]);
        }
        List<String> moves = new ArrayList<>();
        for (int i = 0; i < CustomHero.MOVES; i++) {
            moves.add(CustomHero.AUTO);
        }
        CustomHero laid = hero.withKeys(keys).withSneakKeys(sneakKeys).withMoves(moves);
        // Only the rules of the keys: the budget and the count are the Powers tab's business.
        CustomHeroRules.Limits keysOnly = new CustomHeroRules.Limits(Integer.MAX_VALUE, Integer.MAX_VALUE, limits.maxStrength(),
                limits.maxSkinBytes(), limits.disabled(), limits.heroModules(), limits.worldPowers(), limits.layers(), limits.spread());
        CustomHero ruled = CustomHeroRules.sanitize(laid, keysOnly).hero();
        return laid.withKeys(ruled.keys()).withSneakKeys(ruled.sneakKeys());
    }

    /** The key a generic power (or a hero's ability whose own key was taken) goes on first. */
    public static int preferred(CatalogEntry entry) {
        if (entry instanceof HeroAbility a && a.home >= 1 && a.home <= CustomHero.KEYS) {
            return a.home;
        }
        return switch (entry.group()) {
            case ENERGY, ELEMENT -> 1;
            case MOVE -> 2;
            case MIND -> 3;
            case BODY -> 4;
        };
    }

    private static int freeFrom(String[] plain, int first) {
        for (int i = 0; i < CustomHero.KEYS; i++) {
            int column = (first - 1 + i) % CustomHero.KEYS + 1;
            if (plain[column] == null) {
                return column;
            }
        }
        return 0;
    }

    private static boolean hasVariant(CustomHero hero, String id) {
        if (id == null) {
            return false;
        }
        CustomHero.Pick pick = null;
        for (CustomHero.Pick p : hero.powers()) {
            if (p.id().equals(id)) {
                pick = p;
            }
        }
        return pick != null && pick.entry() instanceof HeroAbility a && a.hasAlt() && HeroAbility.BOTH.equals(pick.option());
    }

    private KeyLayout() {
    }
}
