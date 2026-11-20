package com.kolodkingal.enhanced.spellbook;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.18.1: what the client knows of its own player's witch level (the last {@link WitchLevelPayload}): plain data, no client
 * class, so the HUD, the Book of Spells screen and the book's use read it on the client and nothing loads on a server.
 */
public final class WitchLevelView {
    private static volatile WitchLevelPayload last = WitchLevelPayload.NONE;

    private WitchLevelView() {
    }

    public static void set(WitchLevelPayload payload) {
        last = payload == null ? WitchLevelPayload.NONE : payload;
    }

    /** Forgets it (the client left the server). */
    public static void clear() {
        last = WitchLevelPayload.NONE;
    }

    /** Whether the player is a witch (of a book, or of the Demiurge's gift). */
    public static boolean active() {
        return last.source() != 0;
    }

    /** 1-3: a book ({@code ordinal + 1}); 4: the Demiurge's gift; 0: none. */
    public static int source() {
        return last.source();
    }

    public static boolean gift() {
        return last.source() == 4;
    }

    public static int level() {
        return last.level();
    }

    public static int xp() {
        return last.xp();
    }

    public static int maxLevel() {
        return last.maxLevel();
    }

    /** The level at which key {@code column} (1-5) first holds a spell of her own (0: never). */
    public static int keyLevel(int column) {
        return column >= 1 && column <= 5 ? last.keyLevels()[column - 1] : 0;
    }

    /** What level {@code level} (1-5) opens: each "{@code column|sneak|cell}" (column 0: a power on no key). */
    public static List<String[]> opens(int level) {
        List<String[]> out = new ArrayList<>();
        if (level < 1 || level > last.opens().size()) {
            return out;
        }
        for (String s : last.opens().get(level - 1).split(";")) {
            if (!s.isEmpty()) {
                String[] p = s.split("\\|", 3);
                if (p.length == 3) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    /** The share of the way to the next level (1 at the highest level). */
    public static float progress() {
        int level = level();
        if (level >= maxLevel() || level >= SpellLevels.MAX) {
            return 1.0F;
        }
        int from = SpellLevels.threshold(level);
        int to = SpellLevels.threshold(level + 1);
        return Math.max(0.0F, Math.min(1.0F, (xp() - from) / (float) Math.max(1, to - from)));
    }

    /** The experience still missing to the next level (0 at the highest). */
    public static int missing() {
        int level = level();
        return level >= maxLevel() || level >= SpellLevels.MAX ? 0 : Math.max(0, SpellLevels.threshold(level + 1) - xp());
    }

    /** The colour of her magic: the book's, else -1 (the gift's is her hero's). */
    public static int color() {
        int s = source();
        return s >= 1 && s <= 3 ? Spellbook.values()[s - 1].color : -1;
    }

    public static int glow() {
        int s = source();
        return s >= 1 && s <= 3 ? Spellbook.values()[s - 1].glow : -1;
    }
}
