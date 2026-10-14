package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;

/**
 * The hint strip at the bottom right of a builder's screen (D-B 6.4): three to five rows, each a small picture of an input
 * (a mouse with a button lit, the wheel, a key cap) and one or two words, for the spell chosen and the state of its
 * selection. Pure: {@link #lines} is a function of a few values, so a GameTest checks it with no client class (B10); the
 * client's {@code BuildHud} draws the pictures, with the key's own name for a key (rebound keys included).
 */
public final class BuildHints {
    /** Where the builder is in the grammar ({@link BuildGrammar#stage}), as far as the strip cares. */
    public enum Stage {
        /** Nothing selected (and, for Pull and the brushes, which select nothing, always). */
        NOTHING,
        /** Corner A set: the box follows the aim. */
        CORNER,
        /** A box or a selected thing is ready. */
        BOX,
        /** A box is ready whose spell removes or replaces more than 64 blocks: the right button must be held. */
        BOX_HOLD,
        /** A Copy's or a Move's ghost follows the crosshair. */
        GHOST,
        /** A shape follows the crosshair. */
        FOLLOWING,
        /** A shape is planted (and its height, if it has one, follows the look). */
        PLANTED,
        /** A planted shape's height is fixed. */
        FIXED
    }

    /** The picture of a row's input. */
    public enum Input {
        /** A left click. */
        LEFT,
        /** A double left click. */
        LEFT_TWICE,
        /** A right click. */
        RIGHT,
        /** The right button held. */
        RIGHT_HELD,
        /** The scroll wheel. */
        WHEEL,
        /** The second knob (Left Alt) and the scroll wheel. */
        MORE_WHEEL,
        /** An eye: where the builder looks. */
        LOOK,
        /** The Build Magic key (Y), held. */
        MAGIC_KEY,
        /** The Undo key (Ctrl + Z). */
        UNDO_KEY,
        /** The Flip key (Ctrl + F). */
        FLIP_KEY
    }

    /** One row: an input's picture and what it does. */
    public record Row(Input input, Component text) {
        /** The lang key of the words (for tests and for the HUD's layout). */
        public String key() {
            return text.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : text.getString();
        }
    }

    private static final String HINT = "hud.enhanced.build.hint.";

    /**
     * The rows for a builder now.
     *
     * @param spell   the spell chosen
     * @param style   its style or shape (its ordinal), named in the Alt + scroll row
     * @param stage   the selection's stage ({@link BuildGrammar#stage}, made {@link Stage#BOX_HOLD} by the client's count)
     * @param hero    {@link Power#SCARLET_WITCH} or {@link Power#BILLY}: while one of his spells works, Billy (one spell at
     *                a time) sees only how to stop it; the Scarlet Witch, whose spells wait in a queue, goes on building
     * @param working whether a spell of the builder's is at work
     * @param hasUndo whether something can be undone (the Ctrl + Z row)
     */
    public static List<Row> lines(Spell spell, int style, Stage stage, Power hero, boolean working, boolean hasUndo) {
        List<Row> rows = new ArrayList<>(5);
        if (working && hero == Power.BILLY) {
            rows.add(row(Input.UNDO_KEY, "stop"));
            return rows;
        }
        boolean undoRow = true;
        switch (spell) {
            case PAINT, LAND, GROW -> {
                rows.add(new Row(Input.RIGHT_HELD, hint(spell == Spell.PAINT ? "paint" : spell == Spell.LAND ? "raise" : "grow")));
                rows.add(row(Input.WHEEL, "brush"));
                if (spell != Spell.PAINT) {
                    rows.add(new Row(Input.MORE_WHEEL, Component.translatable(HINT + "style", styleName(spell, style))));
                }
                undoRow = false;
            }
            case PULL -> {
                rows.add(row(Input.RIGHT, "pull"));
                rows.add(row(Input.WHEEL, "layers"));
                undoRow = false;
            }
            case SHAPE -> {
                if (stage == Stage.PLANTED || stage == Stage.FIXED) {
                    rows.add(row(Input.RIGHT, "place"));
                    if (stage == Stage.PLANTED && ShapeKind.byId(style).hasHeight()) {
                        rows.add(row(Input.LOOK, "taller"));
                        rows.add(row(Input.LEFT, "done"));
                    }
                    rows.add(row(Input.LEFT, "let_go"));
                    undoRow = false;
                } else {
                    rows.add(row(Input.RIGHT, "place"));
                    rows.add(row(Input.WHEEL, "size"));
                    rows.add(new Row(Input.MORE_WHEEL, Component.translatable(HINT + "shape", styleName(spell, style))));
                    rows.add(row(Input.LEFT, "plant"));
                    undoRow = false;
                }
            }
            default -> boxRows(rows, spell, style, stage);
        }
        if (stage == Stage.CORNER || stage == Stage.BOX_HOLD || stage == Stage.GHOST
                || (stage == Stage.BOX && (spell == Spell.COPY || spell == Spell.MOVE || spell == Spell.REPEAT))) {
            undoRow = false;
        }
        if (working) {
            // A spell of hers works: Ctrl + Z stops it and takes it back (D-B 12.2).
            if (rows.size() >= 5) {
                rows.remove(rows.size() - 1);
            }
            rows.add(row(Input.UNDO_KEY, "stop"));
        } else if (undoRow && hasUndo) {
            rows.add(row(Input.UNDO_KEY, "undo"));
        }
        return rows;
    }

    /** Fill, Vanish, Change, Copy, Move and Repeat: the box's rows. */
    private static void boxRows(List<Row> rows, Spell spell, int style, Stage stage) {
        switch (stage) {
            case CORNER -> {
                rows.add(row(Input.LEFT, "corner2"));
                rows.add(row(Input.LEFT, "in_air"));
            }
            case BOX -> {
                if (spell == Spell.REPEAT) {
                    rows.add(row(Input.RIGHT, "repeat"));
                    rows.add(row(Input.WHEEL, "copies"));
                    rows.add(row(Input.MORE_WHEEL, "gap"));
                } else if (spell == Spell.COPY || spell == Spell.MOVE) {
                    rows.add(row(Input.RIGHT, spell == Spell.COPY ? "copy" : "pick_up"));
                    rows.add(row(Input.WHEEL, "push_side"));
                } else {
                    rows.add(row(Input.RIGHT, verb(spell)));
                    rows.add(row(Input.WHEEL, "push_side"));
                    rows.add(new Row(Input.MORE_WHEEL, Component.translatable(HINT + "style", styleName(spell, style))));
                }
            }
            case BOX_HOLD -> {
                rows.add(new Row(Input.RIGHT_HELD, Component.translatable(HINT + "held", hint(verb(spell)))));
                rows.add(row(Input.WHEEL, "push_side"));
                rows.add(row(Input.LEFT, "let_go"));
            }
            case GHOST -> {
                rows.add(row(Input.RIGHT, spell == Spell.MOVE ? "put_down" : "stamp"));
                rows.add(row(Input.WHEEL, "lift"));
                rows.add(row(Input.MORE_WHEEL, "turn"));
                rows.add(row(Input.FLIP_KEY, "flip"));
            }
            default -> {
                rows.add(row(Input.LEFT, "corner"));
                rows.add(row(Input.LEFT_TWICE, "whole"));
                rows.add(row(Input.MAGIC_KEY, "spells"));
            }
        }
    }

    /** What the right button does to a box: fill, vanish or change. */
    private static String verb(Spell spell) {
        return switch (spell) {
            case VANISH -> "vanish";
            case CHANGE -> "change";
            case COPY -> "copy";
            case MOVE -> "pick_up";
            case REPEAT -> "repeat";
            default -> "fill";
        };
    }

    /** The name of a spell's style or shape. */
    public static Component styleName(Spell spell, int style) {
        return Component.translatable(switch (spell) {
            case FILL -> FillStyle.byId(style).key();
            case VANISH -> VanishStyle.byId(style).key();
            case CHANGE -> ChangeStyle.byId(style).key();
            case SHAPE -> ShapeKind.byId(style).key();
            case LAND -> LandStyle.byId(style).key();
            case GROW -> GrowStyle.byId(style).key();
            default -> spell.key();
        });
    }

    private static Row row(Input input, String hint) {
        return new Row(input, hint(hint));
    }

    private static Component hint(String hint) {
        return Component.translatable(HINT + hint);
    }

    private BuildHints() {
    }
}
