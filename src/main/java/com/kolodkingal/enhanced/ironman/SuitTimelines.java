package com.kolodkingal.enhanced.ironman;

import net.minecraft.util.Mth;

/**
 * The suit-up's timelines (3.8, PLAN 7.2): pure functions of the phase's progress, so every client draws the same
 * moment and the GameTests check them (monotonic, 1 at the end, the helmet last, DOWN the reverse). Each part of a suit
 * has a window of the phase (fractions 0-1) in which it goes on; a take-off plays the suit-up backwards (its own,
 * shorter length is {@link SuitKind#downTicks}), so the helmet is the first piece off. Lane L7's choreographies (the
 * client's {@code SuitAssembly}s) move the pieces inside these windows:
 * <ul>
 * <li><b>Mark 1</b>: the plates clank on one after another, boots to helmet, a little more than 3 ticks each;</li>
 * <li><b>Mark 3</b>: boots, shins, thighs, torso, upper arms, forearms, gauntlets, helmet, each pair together and
 * overlapping, then the faceplate swings shut and the eyes light;</li>
 * <li><b>Mark 5</b>: the suitcase opens at his feet ({@link #suitcase}), the panels climb up the legs, wrap the body,
 * run up the arms, the helmet folds over and the faceplate slams;</li>
 * <li><b>Mark 50, Mark 85</b>: the nanite wave runs out of the reactor ({@link #segment}: each part in segments, the
 * nearest to the reactor first), the helmet closes last from the back over the face;</li>
 * <li><b>Hulkbuster</b>: Veronica's frame falls, opens, takes him in and closes ({@link #frameOpen}); the shells close
 * legs first, the faceplate last, then he grows.</li>
 * </ul>
 * The skeleton's signatures ({@link #progress}, {@link #reveal}) stay.
 */
public final class SuitTimelines {
    /** Mark 1's order: one piece after another. */
    private static final SuitParts[] MARK_1_ORDER = {SuitParts.RIGHT_BOOT, SuitParts.LEFT_BOOT, SuitParts.RIGHT_SHIN,
            SuitParts.LEFT_SHIN, SuitParts.RIGHT_THIGH, SuitParts.LEFT_THIGH, SuitParts.CHEST, SuitParts.BACK,
            SuitParts.RIGHT_UPPER_ARM, SuitParts.LEFT_UPPER_ARM, SuitParts.RIGHT_FOREARM, SuitParts.LEFT_FOREARM,
            SuitParts.RIGHT_GAUNTLET, SuitParts.LEFT_GAUNTLET, SuitParts.HELMET, SuitParts.FACEPLATE};
    /** Mark 1: each piece's share of the phase (of 60 ticks: 3.5 ticks a piece). */
    private static final float MARK_1_PIECE = 0.058F;
    /** The Hulkbuster's frame (ticks of its age): it falls, opens, waits for him to step in, closes. */
    public static final int FRAME_FALL = 30;
    public static final int FRAME_OPEN = 10;
    public static final int FRAME_STEP_IN = 10;
    public static final int FRAME_CLOSE = 15;

    /**
     * The progress (0-1) of a phase of {@code ticks} that began at game time {@code start}, at {@code gameTime} plus
     * the render's {@code partialTick}.
     */
    public static float progress(long gameTime, long start, int ticks, float partialTick) {
        if (ticks <= 0) {
            return 1.0F;
        }
        return Mth.clamp((gameTime - start + partialTick) / ticks, 0.0F, 1.0F);
    }

    /**
     * How far the part {@code part} of the suit {@code kind} is on (0: not there, 1: in place) at the phase's {@code
     * progress}; a take-off ({@code up} false) plays the suit-up backwards.
     */
    public static float reveal(SuitKind kind, SuitParts part, float progress, boolean up) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        float s = start(kind, part);
        float e = end(kind, part);
        return up ? ramp(t, s, e) : ramp(1.0F - t, s, e);
    }

    /**
     * How far segment {@code index} of {@code count} of a part is on: for the nanotech suits the segments of a part go
     * on one after another (overlapping by half), the one nearest the reactor ({@code index} 0) first, and the last
     * ends with the part; for the other suits the whole part at once ({@link #reveal}).
     */
    public static float segment(SuitKind kind, SuitParts part, int index, int count, float progress, boolean up) {
        if (!kind.nanotech() || count <= 1) {
            return reveal(kind, part, progress, up);
        }
        float s = start(kind, part);
        float e = end(kind, part);
        float length = (e - s) * 2.0F / (count + 1);
        int k = Mth.clamp(index, 0, count - 1);
        float s0 = s + k * length * 0.5F;
        // (the last segment ends exactly with the part)
        float e0 = k == count - 1 ? e : s0 + length;
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        return up ? ramp(t, s0, e0) : ramp(1.0F - t, s0, e0);
    }

    /** How lit the eyes and the reactor are: they light up last on the way up and go dark first on the way down. */
    public static float lit(SuitKind kind, float progress, boolean up) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        float from = switch (kind) {
            case MARK_1 -> 0.94F;
            case MARK_3, MARK_50, MARK_85 -> 0.90F;
            case MARK_5 -> 0.95F;
            case HULKBUSTER -> 0.8125F;
        };
        float to = kind == SuitKind.HULKBUSTER ? 0.86F : 1.0F;
        return up ? ramp(t, from, to) : ramp(1.0F - t, from, to);
    }

    /**
     * The Mark 5's suitcase at his feet: {@code [0]} how far its lid is open, {@code [1]} how much of the case is still
     * there (it snaps open, the panels climb out of it, it folds away into the boots). A take-off: the reverse.
     */
    public static float[] suitcase(float progress, boolean up) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        if (!up) {
            t = 1.0F - t;
        }
        return new float[] {ramp(t, 0.0F, 0.1F), 1.0F - ramp(t, 0.30F, 0.42F)};
    }

    /**
     * How open the Hulkbuster's frame is at its age (ticks plus the partial tick): closed while it falls, it opens,
     * waits for him to step in, and closes round him.
     */
    public static float frameOpen(float age) {
        float open = FRAME_FALL;
        float wait = open + FRAME_OPEN;
        float close = wait + FRAME_STEP_IN;
        if (age < open) {
            return 0.0F;
        }
        if (age < wait) {
            return smooth((age - open) / FRAME_OPEN);
        }
        if (age < close) {
            return 1.0F;
        }
        return 1.0F - smooth((age - close) / FRAME_CLOSE);
    }

    /** The phase fraction at which a part starts going on (its window's start). */
    public static float start(SuitKind kind, SuitParts part) {
        return window(kind, part)[0];
    }

    /** The phase fraction at which a part is fully on (its window's end). */
    public static float end(SuitKind kind, SuitParts part) {
        return window(kind, part)[1];
    }

    private static float[] window(SuitKind kind, SuitParts part) {
        return switch (kind) {
            case MARK_1 -> {
                int i = 0;
                while (MARK_1_ORDER[i] != part) {
                    i++;
                }
                float s = 0.01F + i * MARK_1_PIECE;
                yield new float[] {s, s + MARK_1_PIECE};
            }
            case MARK_3 -> {
                if (part == SuitParts.FACEPLATE) {
                    yield new float[] {0.80F, 0.90F};
                }
                float s = group(part) * 0.09F;
                yield new float[] {s, s + 0.15F};
            }
            case MARK_5 -> switch (part) {
                case RIGHT_BOOT, LEFT_BOOT -> new float[] {0.10F, 0.24F};
                case RIGHT_SHIN, LEFT_SHIN -> new float[] {0.17F, 0.31F};
                case RIGHT_THIGH, LEFT_THIGH -> new float[] {0.24F, 0.38F};
                case CHEST -> new float[] {0.34F, 0.50F};
                case BACK -> new float[] {0.38F, 0.52F};
                case RIGHT_UPPER_ARM, LEFT_UPPER_ARM -> new float[] {0.46F, 0.60F};
                case RIGHT_FOREARM, LEFT_FOREARM -> new float[] {0.53F, 0.67F};
                case RIGHT_GAUNTLET, LEFT_GAUNTLET -> new float[] {0.60F, 0.74F};
                case HELMET -> new float[] {0.72F, 0.88F};
                case FACEPLATE -> new float[] {0.90F, 0.95F};
            };
            case MARK_50, MARK_85 -> switch (part) {
                case CHEST -> new float[] {0.00F, 0.18F};
                case BACK -> new float[] {0.06F, 0.24F};
                case RIGHT_UPPER_ARM, LEFT_UPPER_ARM -> new float[] {0.14F, 0.32F};
                case RIGHT_FOREARM, LEFT_FOREARM -> new float[] {0.26F, 0.44F};
                case RIGHT_GAUNTLET, LEFT_GAUNTLET -> new float[] {0.38F, 0.54F};
                case RIGHT_THIGH, LEFT_THIGH -> new float[] {0.18F, 0.36F};
                case RIGHT_SHIN, LEFT_SHIN -> new float[] {0.32F, 0.50F};
                case RIGHT_BOOT, LEFT_BOOT -> new float[] {0.46F, 0.62F};
                case HELMET -> new float[] {0.60F, 0.84F};
                case FACEPLATE -> new float[] {0.78F, 0.92F};
            };
            case HULKBUSTER -> switch (part) {
                case RIGHT_BOOT, LEFT_BOOT, RIGHT_SHIN, LEFT_SHIN, RIGHT_THIGH, LEFT_THIGH ->
                        new float[] {0.625F, 0.70F};
                case CHEST, BACK -> new float[] {0.66F, 0.74F};
                case RIGHT_UPPER_ARM, LEFT_UPPER_ARM, RIGHT_FOREARM, LEFT_FOREARM, RIGHT_GAUNTLET, LEFT_GAUNTLET ->
                        new float[] {0.68F, 0.77F};
                case HELMET -> new float[] {0.74F, 0.79F};
                case FACEPLATE -> new float[] {0.76F, 0.8125F};
            };
        };
    }

    /** The Mark 3's groups: both sides of a pair together, boots first, the helmet last. */
    private static int group(SuitParts part) {
        return switch (part) {
            case RIGHT_BOOT, LEFT_BOOT -> 0;
            case RIGHT_SHIN, LEFT_SHIN -> 1;
            case RIGHT_THIGH, LEFT_THIGH -> 2;
            case CHEST, BACK -> 3;
            case RIGHT_UPPER_ARM, LEFT_UPPER_ARM -> 4;
            case RIGHT_FOREARM, LEFT_FOREARM -> 5;
            case RIGHT_GAUNTLET, LEFT_GAUNTLET -> 6;
            case HELMET, FACEPLATE -> 7;
        };
    }

    /** 0 before {@code from}, 1 after {@code to}, linear between. */
    public static float ramp(float t, float from, float to) {
        if (t <= from) {
            return 0.0F;
        }
        if (t >= to) {
            return 1.0F;
        }
        return (t - from) / (to - from);
    }

    /** Smoothstep of a 0-1 value. */
    public static float smooth(float t) {
        float k = Mth.clamp(t, 0.0F, 1.0F);
        return k * k * (3.0F - 2.0F * k);
    }

    private SuitTimelines() {
    }
}
