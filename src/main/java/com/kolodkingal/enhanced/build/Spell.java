package com.kolodkingal.enhanced.build;

import java.util.Locale;

/**
 * The spells of Build Magic (3.1), the building power of the Scarlet Witch and of Billy, in the order of the Spell Wheel,
 * clockwise from the top. The same eleven for both heroes: what differs is how big Billy may make them.
 *
 * <p>The ordinal is what a {@link SpellRecipe} carries (one byte) and what the undo history saves, so a spell is only ever
 * appended, never moved. {@link #byId} reads any number as a spell ({@code floorMod}), as the Hex Control's actions are
 * read ({@code network/HexActionPayload}), so a stale or modified client can name no spell that does not exist.
 */
public enum Spell {
    /** Fills the box, or the selected thing, with the material, in one of the {@link FillStyle}s. */
    FILL,
    /** Makes what is in the box disappear, in one of the {@link VanishStyle}s. */
    VANISH,
    /** Turns blocks into the material and keeps their shapes, in one of the {@link ChangeStyle}s. */
    CHANGE,
    /** A brush: hold and sweep to change the surfaces under it into the material, keeping their shapes. */
    PAINT,
    /** Pull &amp; Push: layers pulled out of the face looked at, or pushed into it. */
    PULL,
    /** One of the eight {@link ShapeKind}s, standing on the surface looked at, that grows from its heart. */
    SHAPE,
    /** Copies the box into the hero's pocket, then stamps the copy wherever she looks. */
    COPY,
    /** Picks up the box and puts it down somewhere else, as one spell. */
    MOVE,
    /** Copies of the box in a row, in the direction looked: bridges, towers, streets. */
    REPEAT,
    /** A brush that raises, lowers or smooths the ground ({@link LandStyle}). */
    LAND,
    /** A brush that swells or shrinks rock, or makes the ground bloom ({@link GrowStyle}); the last to be built. */
    GROW;

    /** The spell with this id; any number is a spell. */
    public static Spell byId(int id) {
        Spell[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** What travels in a recipe. */
    public int id() {
        return ordinal();
    }

    /** How many styles (or shapes) Alt + scroll goes through: 1 for a spell without styles. */
    public int styles() {
        return switch (this) {
            case FILL -> FillStyle.values().length;
            case VANISH -> VanishStyle.values().length;
            case CHANGE -> ChangeStyle.values().length;
            case SHAPE -> ShapeKind.values().length;
            case LAND -> LandStyle.values().length;
            case GROW -> GrowStyle.values().length;
            default -> 1;
        };
    }

    /** Whether the spell works on a box or a selected thing (Fill, Vanish, Change, Copy, Move, Repeat). */
    public boolean needsBox() {
        return switch (this) {
            case FILL, VANISH, CHANGE, COPY, MOVE, REPEAT -> true;
            default -> false;
        };
    }

    /** Whether the spell conjures blocks of a material (the hand, a picked block or the hotbar's mix). */
    public boolean needsMaterial() {
        return switch (this) {
            case FILL, CHANGE, PAINT, SHAPE -> true;
            default -> false;
        };
    }

    /** Whether the spell is a brush: hold the right button and sweep; one stroke is one undo step. */
    public boolean isBrush() {
        return this == PAINT || this == LAND || this == GROW;
    }

    /** The lang key of its name; {@code key() + ".desc"} is the line under it on the wheel. */
    public String key() {
        return "screen.enhanced.build.spell." + name().toLowerCase(Locale.ROOT);
    }
}
