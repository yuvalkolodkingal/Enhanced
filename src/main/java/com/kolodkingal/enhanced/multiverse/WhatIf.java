package com.kolodkingal.enhanced.multiverse;

import net.minecraft.network.chat.Component;

/**
 * The twist of a What If universe (3.19), as in the series: each universe of the infinite multiverse asks one question,
 * always the same for its number. The twist works on the players and the creatures of that universe alone
 * ({@code MultiverseEvents}).
 */
public enum WhatIf {
    /** Nothing changed: a world like ours, somewhere else. */
    NOTHING("nothing"),
    /** The dead rose: helmeted zombies by day and night, as in 2149 (twice a normal night). */
    ZOMBIES("zombies"),
    /** Gravity failed: everyone jumps high and falls slowly. */
    GRAVITY("gravity"),
    /** Monsters never existed: no monster comes to be. */
    PEACE("peace"),
    /** Everyone was Quicksilver: everyone runs fast. */
    SPEED("speed"),
    /** The gamma bomb went off over the whole world: everyone hits hard and digs fast. */
    GAMMA("gamma"),
    /** The Darkhold won: a darkness falls now and then, and the dead walk by day. */
    DARKHOLD("darkhold");

    private final String id;

    WhatIf(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** "What If... the Dead Rose?" */
    public Component title() {
        return Component.translatable("what_if.enhanced." + id);
    }

    /** The twist of a universe number (only What If universes have one). */
    public static WhatIf of(int universe) {
        if (!Multiverse.isWhatIf(universe)) {
            return NOTHING;
        }
        int h = universe * 0x9E3779B1;
        h ^= h >>> 16;
        return values()[Math.floorMod(h, values().length)];
    }
}
