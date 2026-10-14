package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.BuildHints;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.build.HudDraw.Picture;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * The first-time tips (D-B 6.7): a small card above the hotbar that plays for {@value #TIP_TICKS} ticks, a picture of
 * the input with its button blinking and a few words (D-B 15.3). Client option {@code tips}.
 *
 * <ul>
 *     <li><b>The first two times</b> in a session: a spell chosen (build mode turned on chooses the spell it has),
 *     its tip ("Click two corners", "Look at the kind to change", "Hold right-click and sweep"...), and as build mode
 *     comes on, "Double-click: the whole thing" and "Hold Y for more spells"; a box ready for Fill, Vanish or Change,
 *     "Right-click to make it happen" and "Scroll on a side to grow it"; a spell of his done, "Ctrl + Z takes it
 *     back" and, for a spell with styles, "Left Alt and scroll: another style". They wait in line, one card at a time;
 *     one that no longer fits when its turn comes (the box was made meanwhile) is skipped.</li>
 *     <li><b>Whenever the kid does something that does nothing</b> ({@link #nudge}): a right click with no box in a
 *     box spell, a right click with no block for a spell that conjures one, a scroll beside a box that adjusts
 *     nothing, Ctrl + V with an empty pocket. The card says how at once (each such card at most once in
 *     {@value #NUDGE_GAP} ticks).</li>
 * </ul>
 *
 * <p>The counts are kept in memory only: a new session shows the tips again ({@link #clear} at logout).
 */
final class BuildTips {
    /** How long a card plays: 3 seconds. */
    static final int TIP_TICKS = 60;
    /** How many times a first-time tip is shown in a session. */
    static final int FIRST_TIMES = 2;
    /** The least ticks between two showings of the same "that did nothing" card. */
    static final int NUDGE_GAP = 100;

    /** When a tip still fits, checked as its turn comes. */
    enum When {
        /** Always. */
        ALWAYS,
        /** Nothing is selected yet (or only corner A): "Click two corners". */
        NO_BOX,
        /** A box (or a thing) is ready. */
        BOX,
        /** The spell chosen has styles (Alt + scroll changes them). */
        STYLED
    }

    /** One tip: its words (a lang key, with a key's name for {@code %s}), its picture, when it fits. */
    record Tip(String key, Picture picture, @Nullable KeyMapping named, When when) {
        Component text() {
            return named == null ? Component.translatable(key) : Component.translatable(key, named.getTranslatedKeyMessage());
        }
    }

    private static final String TIP = "hud.enhanced.build.tip.";
    static final Tip CORNERS = new Tip(TIP + "corners", Picture.LEFT, null, When.NO_BOX);
    static final Tip WHOLE = new Tip(TIP + "whole", Picture.LEFT_TWICE, null, When.NO_BOX);
    static final Tip DO_IT = new Tip(TIP + "do_it", Picture.RIGHT, null, When.BOX);
    static final Tip GROW = new Tip(TIP + "grow", Picture.WHEEL, null, When.BOX);
    static final Tip STYLE = new Tip(TIP + "style", Picture.MORE_WHEEL, BuildKeys.MORE, When.STYLED);
    static final Tip CHANGE = new Tip(TIP + "change", Picture.LOOK, null, When.ALWAYS);
    static final Tip PAINT = new Tip(TIP + "paint", Picture.RIGHT_HELD, null, When.ALWAYS);
    static final Tip PULL = new Tip(TIP + "pull", Picture.WHEEL, null, When.ALWAYS);
    static final Tip SHAPE = new Tip(TIP + "shape", Picture.WHEEL, null, When.ALWAYS);
    static final Tip COPY = new Tip(TIP + "copy", Picture.RIGHT, null, When.ALWAYS);
    static final Tip MOVE = new Tip(TIP + "move", Picture.RIGHT, null, When.ALWAYS);
    static final Tip REPEAT = new Tip(TIP + "repeat", Picture.LOOK, null, When.ALWAYS);
    static final Tip BRUSH = new Tip(TIP + "brush", Picture.RIGHT_HELD, null, When.ALWAYS);
    static final Tip SPELLS = new Tip(TIP + "spells", Picture.MAGIC_KEY, BuildKeys.MAGIC, When.ALWAYS);
    static final Tip UNDO = new Tip(TIP + "undo", Picture.UNDO_KEY, BuildKeys.UNDO, When.ALWAYS);
    /** "Hold a block, or middle-click one in the world" (D-B 5.1: the hint for a spell with no block). */
    static final Tip NO_BLOCK = new Tip("message.enhanced.build.no_block", Picture.MIDDLE, null, When.ALWAYS);

    private static final Deque<Tip> WAITING = new ArrayDeque<>();
    private static final Map<String, Integer> SHOWN = new HashMap<>();
    private static final Map<String, Long> NUDGED = new HashMap<>();
    private static @Nullable Tip current;
    private static long startedAt;

    /** The tips of a spell chosen. */
    static void chosen(Spell spell) {
        switch (spell) {
            case FILL, VANISH -> offer(CORNERS);
            case CHANGE -> {
                offer(CORNERS);
                offer(CHANGE);
            }
            case COPY -> {
                offer(CORNERS);
                offer(COPY);
            }
            case MOVE -> {
                offer(CORNERS);
                offer(MOVE);
            }
            case REPEAT -> {
                offer(CORNERS);
                offer(REPEAT);
            }
            case PAINT -> offer(PAINT);
            case PULL -> offer(PULL);
            case SHAPE -> offer(SHAPE);
            case LAND, GROW -> offer(BRUSH);
        }
    }

    /** Build mode came on with {@code spell} chosen. */
    static void started(Spell spell) {
        chosen(spell);
        if (spell.needsBox()) {
            offer(WHOLE);
        }
        offer(SPELLS);
    }

    /** A box (or a thing) became ready for {@code spell}. */
    static void boxReady(Spell spell) {
        if (spell == Spell.FILL || spell == Spell.VANISH || spell == Spell.CHANGE) {
            offer(DO_IT);
            offer(GROW);
        }
    }

    /** A spell of his is done. */
    static void done(Spell spell) {
        offer(UNDO);
        if (spell.styles() > 1) {
            offer(STYLE);
        }
    }

    /** A first-time tip, in line, while it has been shown fewer than {@value #FIRST_TIMES} times. */
    static void offer(Tip tip) {
        if (!EnhancedClientConfig.buildTips() || SHOWN.getOrDefault(tip.key(), 0) >= FIRST_TIMES || tip.equals(current) || WAITING.contains(tip)) {
            return;
        }
        WAITING.add(tip);
    }

    /** Something the kid did did nothing: the card that says how, at once (not again in {@value #NUDGE_GAP} ticks). */
    static void nudge(Tip tip) {
        long now = BuildClient.now();
        if (!EnhancedClientConfig.buildTips() || tip.equals(current) || now - NUDGED.getOrDefault(tip.key(), Long.MIN_VALUE / 2) < NUDGE_GAP) {
            return;
        }
        NUDGED.put(tip.key(), now);
        WAITING.remove(tip);
        current = tip;
        startedAt = now;
    }

    /** One tick in build mode ({@code building} false: every card goes). */
    static void tick(boolean building, BuildHints.Stage stage) {
        if (!building || !EnhancedClientConfig.buildTips()) {
            WAITING.clear();
            current = null;
            return;
        }
        long now = BuildClient.now();
        if (current != null && now - startedAt >= TIP_TICKS) {
            current = null;
        }
        while (current == null && !WAITING.isEmpty()) {
            Tip next = WAITING.poll();
            if (fits(next, stage)) {
                current = next;
                startedAt = now;
                SHOWN.merge(next.key(), 1, Integer::sum);
            }
        }
    }

    private static boolean fits(Tip tip, BuildHints.Stage stage) {
        return switch (tip.when()) {
            case NO_BOX -> stage == BuildHints.Stage.NOTHING || stage == BuildHints.Stage.CORNER;
            case BOX -> stage == BuildHints.Stage.BOX || stage == BuildHints.Stage.BOX_HOLD;
            case STYLED -> BuildClient.grammar().spell().styles() > 1;
            case ALWAYS -> true;
        };
    }

    /** The card playing now (null: none). */
    static @Nullable Tip current() {
        return current;
    }

    /** How far the card playing now is, in ticks (with the frame's part of a tick). */
    static float age(float partialTick) {
        return BuildClient.now() - startedAt + partialTick;
    }

    /**
     * The card for a right click that did nothing in build mode: no box for a box spell, or no block for a spell that
     * conjures one; nothing when the click had something else to do.
     */
    static void rightClickDidNothing(BuildGrammar grammar, boolean noMaterial) {
        Spell spell = grammar.spell();
        if (noMaterial) {
            nudge(NO_BLOCK);
        } else if (spell.needsBox() && !grammar.finished() && !grammar.ghost()) {
            nudge(CORNERS);
        }
    }

    /** The card for a scroll that adjusted nothing: a box is there, but no side of it was looked at. */
    static void scrollDidNothing(BuildGrammar grammar) {
        if (grammar.spell().needsBox() && grammar.finished() && !grammar.ghost()) {
            nudge(GROW);
        }
    }

    /** Forgets every count and card (a new session shows the tips again). */
    static void clear() {
        WAITING.clear();
        SHOWN.clear();
        NUDGED.clear();
        current = null;
        startedAt = 0L;
    }

    private BuildTips() {
    }
}
