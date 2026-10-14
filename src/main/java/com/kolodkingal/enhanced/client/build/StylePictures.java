package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.ShapeKind;
import com.kolodkingal.enhanced.build.Spell;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * The pictures of the wheel's style row (D-B 6.5): Fill's six styles, Vanish's four, Change's three, the eight shapes,
 * Land's and Grow's three. ART's {@code textures/gui/build/<spell>_<style>.png} and {@code shape_<kind>.png} (32 × 32)
 * are drawn when they are there; until then each is a 12 × 12 picture made of chunky pixels, like the blocks the spell
 * makes: the hero's colour is the spell's own work, white and dim grey what it leaves, green, brown and gold the rest.
 * A hollow shape shows its outside only.
 *
 * <p>Only ever loaded on a client.
 */
final class StylePictures {
    /** Pixels along a side of a picture. */
    static final int SIZE = 12;

    private static final int WHITE = 0xF2F2F2;
    private static final int GREEN = 0x58B84A;
    private static final int BROWN = 0x8B5A2B;
    private static final int GOLD = 0xFFD54A;
    private static final int PINK = 0xFF7FB0;
    private static final int RED = 0xE04848;

    // ---- Fill: solid, walls, room, floor, frame, only the empty
    private static final String[][] FILL = {
            {
                    "............",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    "............"},
            {
                    "............",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    ".###....###.",
                    "............"},
            {
                    "............",
                    ".##########.",
                    ".##########.",
                    ".##dddddd##.",
                    ".##dddddd##.",
                    ".##dddddd##.",
                    ".##dddddd##.",
                    ".##dddddd##.",
                    ".##dddddd##.",
                    ".##########.",
                    ".##########.",
                    "............"},
            {
                    "............",
                    ".d........d.",
                    ".d........d.",
                    ".d........d.",
                    ".d........d.",
                    ".d........d.",
                    ".d........d.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    ".##########.",
                    "............"},
            {
                    "...#########",
                    "..##.......#",
                    ".#.#.......#",
                    "#########..#",
                    "#.......#..#",
                    "#.......#..#",
                    "#.......#..#",
                    "#.......#..#",
                    "#.......####",
                    "#.......#.#.",
                    "#.......##..",
                    "#########..."},
            {
                    "............",
                    ".##dd##dd##.",
                    ".##dd##dd##.",
                    ".dd##dd##dd.",
                    ".dd##dd##dd.",
                    ".##dd##dd##.",
                    ".##dd##dd##.",
                    ".dd##dd##dd.",
                    ".dd##dd##dd.",
                    ".##dd##dd##.",
                    ".##dd##dd##.",
                    "............"},
    };
    // ---- Vanish: everything, hollow out, plants and trees, this kind
    private static final String[][] VANISH = {
            {
                    "............",
                    "............",
                    "....ooo.....",
                    "..ooooooo...",
                    ".ooooooooo..",
                    ".oooooooooo.",
                    ".oooooooooo.",
                    "..oooooooo..",
                    "...oooooo...",
                    "....d..d....",
                    "...d....d...",
                    "............"},
            {
                    "............",
                    "###.###.###.",
                    "###.###.###.",
                    "###.###.###.",
                    "............",
                    "###.....###.",
                    "###.....###.",
                    "###.....###.",
                    "............",
                    "###.###.###.",
                    "###.###.###.",
                    "###.###.###."},
            {
                    "....gggg....",
                    "...gggggg...",
                    "..gggggggg..",
                    "..gggggggg..",
                    "...gggggg...",
                    "....gggg....",
                    ".....bb.....",
                    ".....bb.....",
                    ".....bb.....",
                    ".....bb.....",
                    ".gggggggggg.",
                    "............"},
            {
                    "............",
                    "###.ooo.###.",
                    "###.ooo.###.",
                    "###.ooo.###.",
                    "............",
                    "ooo.###.ooo.",
                    "ooo.###.ooo.",
                    "ooo.###.ooo.",
                    "............",
                    "###.ooo.###.",
                    "###.ooo.###.",
                    "###.ooo.###."},
    };
    // ---- Change: this kind, everything, only the outside
    private static final String[][] CHANGE = {
            {
                    "............",
                    "#####.ooooo.",
                    "#####.ooooo.",
                    "#####.ooooo.",
                    "#####.ooooo.",
                    "#####.ooooo.",
                    "............",
                    "#####.#####.",
                    "#####.#####.",
                    "#####.#####.",
                    "#####.#####.",
                    "#####.#####."},
            {
                    "............",
                    "ooooo.ooooo.",
                    "ooooo.ooooo.",
                    "ooooo.ooooo.",
                    "ooooo.ooooo.",
                    "ooooo.ooooo.",
                    "............",
                    "ooooo.ooooo.",
                    "ooooo.ooooo.",
                    "ooooo.ooooo.",
                    "ooooo.ooooo.",
                    "ooooo.ooooo."},
            {
                    "............",
                    "ooo.ooo.ooo.",
                    "ooo.ooo.ooo.",
                    "ooo.ooo.ooo.",
                    "............",
                    "ooo.###.ooo.",
                    "ooo.###.ooo.",
                    "ooo.###.ooo.",
                    "............",
                    "ooo.ooo.ooo.",
                    "ooo.ooo.ooo.",
                    "ooo.ooo.ooo."},
    };
    // ---- Shapes: ball, dome, tower, ring, cone, pyramid, arch, heart
    private static final String[][] SHAPES = {
            {
                    "....####....",
                    "..########..",
                    ".##########.",
                    ".##########.",
                    "############",
                    "############",
                    "############",
                    "############",
                    ".##########.",
                    ".##########.",
                    "..########..",
                    "....####...."},
            {
                    "............",
                    "............",
                    "....####....",
                    "..########..",
                    ".##########.",
                    ".##########.",
                    "############",
                    "############",
                    "############",
                    "############",
                    "############",
                    "............"},
            {
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######...",
                    "...######..."},
            {
                    "............",
                    "............",
                    "...######...",
                    "..########..",
                    ".###....###.",
                    ".##......##.",
                    ".##......##.",
                    ".###....###.",
                    "..########..",
                    "...######...",
                    "............",
                    "............"},
            {
                    ".....##.....",
                    ".....##.....",
                    ".....##.....",
                    "....####....",
                    "....####....",
                    "...######...",
                    "...######...",
                    "..########..",
                    "..########..",
                    ".##########.",
                    "############",
                    "############"},
            {
                    ".....oo.....",
                    ".....##.....",
                    "....oooo....",
                    "....####....",
                    "...oooooo...",
                    "...######...",
                    "..oooooooo..",
                    "..########..",
                    ".oooooooooo.",
                    ".##########.",
                    "oooooooooooo",
                    "############"},
            {
                    "....####....",
                    "..########..",
                    ".####..####.",
                    ".###....###.",
                    "###......###",
                    "###......###",
                    "###......###",
                    "###......###",
                    "###......###",
                    "###......###",
                    "###......###",
                    "###......###"},
            {
                    "............",
                    ".####..####.",
                    "############",
                    "############",
                    "############",
                    ".##########.",
                    "..########..",
                    "...######...",
                    "....####....",
                    ".....##.....",
                    "............",
                    "............"},
    };
    // ---- Land: raise, lower, smooth
    private static final String[][] LAND = {
            {
                    ".....yy.....",
                    "....yyyy....",
                    "...yyyyyy...",
                    ".....yy.....",
                    ".....yy.....",
                    "............",
                    "....gggg....",
                    "..gggggggg..",
                    ".gggggggggg.",
                    "gggggggggggg",
                    "bbbbbbbbbbbb",
                    "bbbbbbbbbbbb"},
            {
                    ".....yy.....",
                    ".....yy.....",
                    "...yyyyyy...",
                    "....yyyy....",
                    ".....yy.....",
                    "............",
                    "ggg......ggg",
                    "gggg....gggg",
                    "ggggg..ggggg",
                    "gggggggggggg",
                    "bbbbbbbbbbbb",
                    "bbbbbbbbbbbb"},
            {
                    "............",
                    "............",
                    "............",
                    ".yyy....yyy.",
                    "yy.yy..yy.yy",
                    "............",
                    "..g...g.....",
                    ".ggg.ggg....",
                    "gggggggggggg",
                    "gggggggggggg",
                    "bbbbbbbbbbbb",
                    "bbbbbbbbbbbb"},
    };
    // ---- Grow: swell, shrink, bloom
    private static final String[][] GROW = {
            {
                    "y..........y",
                    ".oooooooooo.",
                    ".o........o.",
                    ".o..####..o.",
                    ".o..####..o.",
                    ".o..####..o.",
                    ".o..####..o.",
                    ".o........o.",
                    ".oooooooooo.",
                    "y..........y",
                    "............",
                    "............"},
            {
                    "............",
                    "y.oooooooo.y",
                    "..o######o..",
                    "..o######o..",
                    "..o######o..",
                    "..o######o..",
                    "..o######o..",
                    "..o######o..",
                    "y.oooooooo.y",
                    "............",
                    "............",
                    "............"},
            {
                    ".....pp.....",
                    "....pppp....",
                    "..pp.yy.pp..",
                    ".pppyyyyppp.",
                    "..pp.yy.pp..",
                    "....pppp....",
                    ".....pp.....",
                    ".....gg.....",
                    "..g..gg.....",
                    "...ggggg....",
                    ".....gg.....",
                    ".....gg....."},
    };

    /**
     * Draws the picture of a spell's style (or, for Shapes, its shape; {@code hollow} shows only the outside) with its top
     * left at ({@code x}, {@code y}), each pixel {@code px} wide. ART's picture when it is there.
     */
    static void draw(GuiGraphics g, Spell spell, int style, boolean hollow, int x, int y, int px, int hero) {
        String name = spell == Spell.SHAPE ? "shape_" + ShapeKind.byId(style).name().toLowerCase(Locale.ROOT)
                : spell.name().toLowerCase(Locale.ROOT) + "_" + style;
        if (HudDraw.art(g, name, x, y, SIZE * px, 32)) {
            return;
        }
        String[] rows = rows(spell, style);
        if (rows == null) {
            return;
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                char c = rows[row].charAt(col);
                if (c == '.' || hollow && spell == Spell.SHAPE && inside(rows, col, row)) {
                    continue;
                }
                int argb = 0xFF000000 | colour(c, hero);
                g.fill(x + col * px, y + row * px, x + (col + 1) * px, y + (row + 1) * px, argb);
            }
        }
    }

    private static String[] rows(Spell spell, int style) {
        String[][] set = switch (spell) {
            case FILL -> FILL;
            case VANISH -> VANISH;
            case CHANGE -> CHANGE;
            case SHAPE -> SHAPES;
            case LAND -> LAND;
            case GROW -> GROW;
            default -> null;
        };
        return set == null ? null : set[Math.floorMod(style, set.length)];
    }

    /** Whether a pixel has all four neighbours filled: the inside of a solid shape, which a hollow one leaves out. */
    private static boolean inside(String[] rows, int col, int row) {
        return filled(rows, col - 1, row) && filled(rows, col + 1, row) && filled(rows, col, row - 1) && filled(rows, col, row + 1);
    }

    private static boolean filled(String[] rows, int col, int row) {
        return col >= 0 && row >= 0 && col < SIZE && row < SIZE && rows[row].charAt(col) != '.';
    }

    private static int colour(char c, int hero) {
        return switch (c) {
            case 'o' -> WHITE;
            case 'd' -> HudDraw.darker(HudDraw.darker(hero));
            case 'g' -> GREEN;
            case 'b' -> BROWN;
            case 'y' -> GOLD;
            case 'p' -> PINK;
            case 'r' -> RED;
            default -> hero;
        };
    }

    private StylePictures() {
    }
}
