package com.kolodkingal.enhanced.hex;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where the Hex Runes sit on the four walls of a Hex (3.7.1: the runes of the WandaVision finale, spread over every wall and
 * growing with the Hex). Pure maths, shared by the client's {@code HexRunesRenderer} and the GameTests; nothing here allocates.
 *
 * <p>A wall is a quarter of the dome: side 0 faces south (+Z), 1 east (+X), 2 north (−Z), 3 west (−X). Its runes stand in
 * rows between the ground and {@link #TOP} (above it the four walls meet in the roof), and in each row the columns share the
 * quarter evenly, so the four walls together ring the dome without a gap or a crowd at their seams. Everything follows the
 * radius: a rune is {@link #SIZE_PER_BLOCK} of it tall (never smaller than {@link #MIN_SIZE}, never a giant over
 * {@link #MAX_SIZE}); once the runes have reached their full size, a bigger Hex gets more of them instead (up to {@link
 * #MAX_ROWS} rows of {@link #MAX_COLUMNS}).
 */
public final class HexRuneLayout {
    /** How many rune shapes there are (the renderer draws them; 0 and 1 are the two of the film). */
    public static final int GLYPHS = 6;
    /** A rune's height (blocks) per block of the Hex's radius. */
    public static final double SIZE_PER_BLOCK = 0.2;
    /** The smallest and the biggest a rune is drawn (blocks tall). */
    public static final double MIN_SIZE = 2.0;
    public static final double MAX_SIZE = 14.0;
    public static final int MAX_ROWS = 4;
    public static final int MAX_COLUMNS = 8;
    /** The most runes one wall can show. */
    public static final int MAX_PER_WALL = MAX_ROWS * MAX_COLUMNS;
    /** From one rune to the next in a row, and from one row to the next, in rune heights. */
    public static final double COLUMN_PITCH = 2.0;
    public static final double ROW_PITCH = 1.7;
    /** The highest a row of runes stands on the dome (radians above the Hex's middle): above it is the roof. */
    public static final double TOP = Math.toRadians(60.0);
    /** Blocks the foot of the lowest rune keeps above the Hex's middle (the ground Wanda stood on). */
    public static final double GROUND = 1.0;
    public static final double QUARTER = Math.PI * 0.5;

    /** The fields of one rune in the array {@link #place} fills: {@link #FIELDS} floats per rune. */
    public static final int AZIMUTH = 0;
    public static final int ELEVATION = 1;
    public static final int SIZE = 2;
    public static final int GLYPH = 3;
    public static final int TILT = 4;
    public static final int PHASE = 5;
    public static final int FIELDS = 6;

    /** Which rune each place of a wall shows after its first two (which are always the film's two). */
    private static final int[] PATTERN = {2, 0, 3, 1, 4, 5, 1, 0, 3, 2, 5, 4};

    /** How tall (blocks) the runes of a Hex of this radius are. */
    public static double size(double radius) {
        return Mth.clamp(radius * SIZE_PER_BLOCK, MIN_SIZE, MAX_SIZE);
    }

    /** The elevation (radians) from which the rows start: the lowest rune's foot stays {@link #GROUND} above the middle. */
    static double lowest(double radius) {
        double foot = (GROUND + size(radius) * 0.6) / Math.max(radius, 1.0E-3);
        return Math.asin(Math.min(foot, Math.sin(TOP) * 0.9));
    }

    /** How many rows of runes a wall of a Hex of this radius has. */
    public static int rows(double radius) {
        double band = radius * (TOP - lowest(radius));
        return Mth.clamp(Mth.floor(band / (size(radius) * ROW_PITCH)), 1, MAX_ROWS);
    }

    /** The elevation (radians above the Hex's middle) of row {@code row} of {@code rows}. */
    public static double elevation(double radius, int rows, int row) {
        double low = lowest(radius);
        return low + (TOP - low) * (row + 0.5) / rows;
    }

    /** How many runes a row at this elevation holds on one wall. */
    public static int columns(double radius, double elevation) {
        double arc = radius * Math.cos(elevation) * QUARTER;
        return Mth.clamp(Mth.floor(arc / (size(radius) * COLUMN_PITCH)), 1, MAX_COLUMNS);
    }

    /** How many runes one wall of a Hex of this radius shows (each of the four the same). */
    public static int count(double radius) {
        int rows = rows(radius);
        int count = 0;
        for (int row = 0; row < rows; row++) {
            count += columns(radius, elevation(radius, rows, row));
        }
        return count;
    }

    /** Whether Hexes of these two radii show the same runes in the same places (only their size differs). */
    public static boolean sameLayout(int a, int b) {
        int rows = rows(a);
        if (rows != rows(b)) {
            return false;
        }
        for (int row = 0; row < rows; row++) {
            if (columns(a, elevation(a, rows, row)) != columns(b, elevation(b, rows, row))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fills {@code out} (from {@code offset}, {@link #FIELDS} floats each) with the runes of wall {@code side}: how many there
     * are and their pattern follow {@code countRadius} (the radius the Hex is set to), their size and place follow {@code
     * radius} (the radius its wall is drawn at now, which eases to the set one when Wanda resizes it). Returns how many it
     * wrote (at most {@link #MAX_PER_WALL}). The azimuth is from the middle of the wall, the tilt in radians, the phase the
     * rune's own beat.
     */
    public static int place(double radius, int countRadius, int side, float[] out, int offset) {
        int rows = rows(countRadius);
        float size = (float) size(radius);
        int k = 0;
        for (int row = 0; row < rows; row++) {
            int columns = columns(countRadius, elevation(countRadius, rows, row));
            // The rows follow the radius drawn now (they rise with a growing wall); a little wobble so they are not a grid.
            double elevation = elevation(radius, rows, row)
                    + (Math.floorMod(k * 29 + side * 13, 5) - 2) * 0.06 * (TOP - lowest(radius)) / rows;
            for (int column = 0; column < columns; column++, k++) {
                double jitter = (Math.floorMod(k * 53 + side * 7 + row * 3, 5) - 2) * 0.06;
                int at = offset + k * FIELDS;
                out[at + AZIMUTH] = (float) (((column + 0.5 + jitter) / columns - 0.5) * QUARTER);
                out[at + ELEVATION] = (float) elevation;
                out[at + SIZE] = size;
                out[at + GLYPH] = glyph(side, k);
                out[at + TILT] = (float) Math.toRadians((Math.floorMod(k * 37 + side * 11, 9) - 4) * 3.0);
                out[at + PHASE] = k * 1.7F + side * 0.9F;
            }
        }
        return k;
    }

    /** The rune at place {@code k} of wall {@code side}: every wall starts with the film's two, then a mix of all six. */
    public static int glyph(int side, int k) {
        if (k < 2) {
            return (k + side) % 2;
        }
        return PATTERN[(k - 2 + side * 3) % PATTERN.length];
    }

    /** The unit direction from the Hex's middle to a point of wall {@code side} at this azimuth (from the wall's middle) and elevation. */
    public static Vec3 direction(int side, double azimuth, double elevation) {
        double a = side * QUARTER + azimuth;
        double flat = Math.cos(elevation);
        return new Vec3(Math.sin(a) * flat, Math.sin(elevation), Math.cos(a) * flat);
    }

    private HexRuneLayout() {
    }
}
