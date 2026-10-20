package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.hex.HexRuneLayout;

/**
 * The shapes of the Hex Runes (3.7.1), as line drawings: each rune is a few strokes, each stroke a polyline of (x, y) points
 * in a box one unit tall (y from −0.5 at its foot to 0.5 at its top, x to the right as seen by whoever reads it).
 *
 * <p>The first two are the runes of the WandaVision finale: the branch (a stem forking into a hand of prongs, two long
 * parallel strokes to its right, a small ring and a foot below) and the gate (a cross bar with a spike over three slanted
 * strokes standing on a base that turns up at its end). The other four are ours, drawn in the same hand: forks, rings,
 * slanted parallel strokes and hooks.
 */
final class HexRuneGlyphs {
    /** The strokes of each rune ({@link HexRuneLayout#GLYPHS} of them). */
    static final float[][][] STROKES = {
            { // 0: the branch
                    {-0.11F, -0.36F, -0.10F, 0.05F, -0.11F, 0.42F},
                    {-0.10F, 0.0F, -0.17F, 0.21F, -0.28F, 0.40F},
                    {-0.09F, 0.03F, 0.12F, 0.40F},
                    {-0.08F, -0.16F, 0.30F, 0.36F},
                    ring(0.09F, -0.22F, 0.065F, 16),
                    {-0.11F, -0.36F, -0.05F, -0.44F, 0.15F, -0.44F},
            },
            { // 1: the gate
                    {-0.30F, 0.27F, 0.22F, 0.21F},
                    {-0.03F, 0.25F, -0.02F, 0.45F},
                    {-0.12F, 0.25F, -0.30F, -0.33F},
                    {0.04F, 0.23F, -0.13F, -0.26F},
                    {0.19F, 0.20F, 0.05F, -0.30F},
                    {-0.30F, -0.33F, 0.28F, -0.42F, 0.32F, -0.32F},
            },
            { // 2: the arrow
                    {0.0F, -0.45F, 0.0F, 0.40F},
                    {-0.22F, 0.18F, 0.0F, 0.45F, 0.22F, 0.18F},
                    {-0.22F, -0.06F, 0.20F, 0.05F},
                    ring(-0.18F, -0.28F, 0.07F, 16),
                    {0.0F, -0.45F, 0.17F, -0.37F},
            },
            { // 3: the lightning
                    {-0.25F, 0.42F, 0.12F, 0.15F, -0.12F, -0.10F, 0.25F, -0.42F},
                    {-0.31F, -0.12F, -0.19F, -0.34F},
                    {-0.20F, -0.06F, -0.08F, -0.28F},
                    ring(0.22F, 0.36F, 0.055F, 16),
            },
            { // 4: the eye
                    ring(0.0F, 0.05F, 0.14F, 16),
                    {0.0F, -0.09F, 0.0F, -0.45F, 0.13F, -0.39F},
                    {-0.10F, 0.15F, -0.27F, 0.44F},
                    {0.10F, 0.15F, 0.27F, 0.44F},
                    {0.0F, 0.19F, 0.0F, 0.38F},
            },
            { // 5: the ladder
                    {-0.20F, 0.44F, -0.32F, -0.40F},
                    {0.08F, 0.44F, -0.04F, -0.40F},
                    {-0.23F, 0.22F, 0.05F, 0.22F},
                    {-0.265F, -0.03F, 0.015F, -0.03F},
                    {-0.30F, -0.28F, -0.02F, -0.28F},
                    {-0.04F, -0.40F, 0.22F, -0.44F, 0.30F, -0.30F},
                    {0.20F, 0.40F, 0.30F, 0.18F},
            },
    };

    /** How many segments each rune has (its strokes are drawn in this order as it is inscribed). */
    static final int[] SEGMENTS = new int[STROKES.length];

    static {
        for (int g = 0; g < STROKES.length; g++) {
            for (float[] stroke : STROKES[g]) {
                SEGMENTS[g] += stroke.length / 2 - 1;
            }
        }
    }

    /** A closed ring of {@code n} segments round (x, y). */
    private static float[] ring(float x, float y, float r, int n) {
        float[] points = new float[(n + 1) * 2];
        for (int k = 0; k <= n; k++) {
            double a = Math.PI * 2.0 * k / n;
            points[2 * k] = x + (float) (Math.cos(a) * r);
            points[2 * k + 1] = y + (float) (Math.sin(a) * r);
        }
        return points;
    }

    private HexRuneGlyphs() {
    }
}
