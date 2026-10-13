package com.kolodkingal.enhanced.build;

import java.util.Locale;

/**
 * The eight shapes of {@link Spell#SHAPE}, chosen with Alt + scroll. Each is solid or hollow (a switch on the wheel; a Ring
 * is always one block thick); the shapes standing on the ground have no floor, a Ball is closed all round, and the Arch and
 * the Heart stand upright, facing the hero. The cells of each are {@code SpellMath}'s. The ordinal travels in a recipe's
 * {@code style}: append only.
 */
public enum ShapeKind {
    BALL,
    DOME,
    TOWER,
    RING,
    CONE,
    PYRAMID,
    ARCH,
    HEART;

    /** The shape with this id; any number is a shape. */
    public static ShapeKind byId(int id) {
        ShapeKind[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** Whether it starts hollow: a Ball, a Dome and a Tower do, the others start solid. */
    public boolean defaultHollow() {
        return this == BALL || this == DOME || this == TOWER;
    }

    /**
     * Whether it has a height of its own (a Tower, a Ring, a Cone, a Pyramid): once it is planted, its top follows the
     * hero's look up and down until a second click fixes it.
     */
    public boolean hasHeight() {
        return this == TOWER || this == RING || this == CONE || this == PYRAMID;
    }

    /** Its height before the hero changes it, for a radius: Tower and Cone 2r + 1, Ring 1, Pyramid r + 1; 0 without one. */
    public int defaultHeight(int radius) {
        return switch (this) {
            case TOWER, CONE -> 2 * radius + 1;
            case RING -> 1;
            case PYRAMID -> radius + 1;
            default -> 0;
        };
    }

    /** Whether it stands upright, facing the hero (snapped to north, east, south or west): the Arch and the Heart. */
    public boolean upright() {
        return this == ARCH || this == HEART;
    }

    /** The lang key of its name. */
    public String key() {
        return "screen.enhanced.build.shape." + name().toLowerCase(Locale.ROOT);
    }
}
