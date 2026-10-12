package com.kolodkingal.enhanced.client.render;

import net.minecraft.util.Mth;

/**
 * How one Ultron body stands at one moment: the angles of its joints in degrees, and how brightly each of its lights
 * burns. {@link UltronPoses} works out the pose a state asks for; the model ({@code UltronModel}) draws it, after
 * {@link #approach} has eased the pose it drew a moment ago toward it, so a tell never jumps from one pose to another.
 * Pure numbers, no Minecraft client classes: the game tests read them.
 *
 * <p>The signs are the model's: a positive {@link #HIPS_X} leans the body forward, a negative arm x raises the arm
 * forward (-90 is level, -180 straight up), a positive arm z swings the right arm out (the left arm's is mirrored by the
 * model, so both are written for the right and the left alike), a positive head x looks down.
 */
public final class UltronPose {
    /** The lean of the hips (forward is positive), their twist and their roll. */
    public static final int HIPS_X = 0;
    public static final int HIPS_Y = 1;
    public static final int HIPS_Z = 2;
    /** How far the body is lifted, in blocks at scale 1 (a hover bob), and how far the whole body is pitched forward. */
    public static final int BOB = 3;
    public static final int PITCH = 4;
    public static final int HEAD_X = 5;
    public static final int HEAD_Y = 6;
    public static final int HEAD_Z = 7;
    /** The right arm: x raises it, y turns it in, z swings it out; the forearm's bend; how far the fingers are curled (0 to 1). */
    public static final int R_ARM_X = 8;
    public static final int R_ARM_Y = 9;
    public static final int R_ARM_Z = 10;
    public static final int R_FORE = 11;
    public static final int R_FINGERS = 12;
    /** The left arm: the same numbers, mirrored by the model. */
    public static final int L_ARM_X = 13;
    public static final int L_ARM_Y = 14;
    public static final int L_ARM_Z = 15;
    public static final int L_FORE = 16;
    public static final int L_FINGERS = 17;
    /** The legs: the thigh's swing and spread, and the knee's bend. */
    public static final int R_LEG_X = 18;
    public static final int R_LEG_Z = 19;
    public static final int R_SHIN = 20;
    public static final int L_LEG_X = 21;
    public static final int L_LEG_Z = 22;
    public static final int L_SHIN = 23;
    /** How far the plates have fallen open (0 closed, 1 open), and the shudder in degrees. */
    public static final int OPEN = 24;
    public static final int SHAKE = 25;
    /** How brightly the eyes burn (0 dark, 1 lit, above 1 blazing) and their colour. */
    public static final int EYES = 26;
    public static final int EYE_R = 27;
    public static final int EYE_G = 28;
    public static final int EYE_B = 29;
    /** The glow of the fingertips (an attack is armed), of the palms (the anti-gravity), of the light in the torso. */
    public static final int FINGER_GLOW = 30;
    public static final int PALM_GLOW = 31;
    public static final int TORSO_GLOW = 32;
    /** A flash over the whole body, and the cyan the lift bathes it in. */
    public static final int FLASH = 33;
    public static final int BATH = 34;
    /** The size of the body as a fraction of its own (a Sentry is assembled from small to whole). */
    public static final int SCALE = 35;
    public static final int COUNT = 36;

    /** The numbers; index with the constants above. */
    public final float[] v = new float[COUNT];

    /** Sets the pose to nothing at all: every angle 0, every light off, full size. */
    public void clear() {
        java.util.Arrays.fill(v, 0.0F);
        v[SCALE] = 1.0F;
    }

    /** Copies another pose. */
    public void set(UltronPose other) {
        System.arraycopy(other.v, 0, v, 0, COUNT);
    }

    /**
     * Eases this pose toward the target by {@code dt} ticks, with an exponential time constant of {@code tau} ticks (the
     * same law as the heroes' poses: {@link AnimMath#ease}). The eyes, the lights and the colour of the eyes follow
     * faster, so a flash is a flash.
     */
    public void approach(UltronPose target, float dt, float tau) {
        float body = 1.0F - (float) Math.exp(-dt / Math.max(0.01F, tau));
        float quick = 1.0F - (float) Math.exp(-dt / Math.max(0.01F, tau * 0.5F));
        for (int i = 0; i < COUNT; i++) {
            boolean light = i >= EYES && i <= FLASH;
            v[i] += (target.v[i] - v[i]) * (light ? quick : body);
        }
    }

    /** Sets a value as a fraction {@code w} of the way from what it is to {@code value}. */
    public void to(int index, float value, float w) {
        v[index] += (value - v[index]) * Mth.clamp(w, 0.0F, 1.0F);
    }

    /** Sets both arms to the same pose, the right one as given and the left one as the mirror of it. */
    public void arms(float x, float y, float z, float fore, float fingers, float w) {
        to(R_ARM_X, x, w);
        to(R_ARM_Y, y, w);
        to(R_ARM_Z, z, w);
        to(R_FORE, fore, w);
        to(R_FINGERS, fingers, w);
        to(L_ARM_X, x, w);
        to(L_ARM_Y, y, w);
        to(L_ARM_Z, z, w);
        to(L_FORE, fore, w);
        to(L_FINGERS, fingers, w);
    }

    /** Sets the colour of the eyes. */
    public void eyeColor(float r, float g, float b, float w) {
        to(EYE_R, r, w);
        to(EYE_G, g, w);
        to(EYE_B, b, w);
    }

    public float get(int index) {
        return v[index];
    }
}
