package com.kolodkingal.enhanced.client.render;

import java.util.List;

/**
 * The boxes of the Ultron model: Prime, his other bodies and the Sentries are one model, and this table is its shape and
 * its place on the skin. Pure data (no Minecraft client classes), so the game tests can check that no two boxes overlap
 * on the skin, and so the people who paint it ({@code ultron.png}, {@code ultron_form1.png}, {@code ultron_form2.png},
 * {@code ultron_sentry.png} and the glow masks, all 128x128) know where every face is.
 *
 * <p><b>The skin.</b> The boxes are cut in model units of one sixteenth of a block, and the skin is read as a 64x64 skin
 * stretched over a 128x128 picture: one model unit is two pixels of the picture, so the plates can carry bevels, seams
 * and rivets. Every box is unwrapped as in the vanilla models: with width {@code w}, height {@code h} and depth {@code d},
 * its faces fill the rectangle of {@code 2d + 2w} by {@code d + h} units at ({@code u}, {@code v}): along the top, the
 * <em>top</em> {@code (u+d, v)} and the <em>bottom</em> {@code (u+d+w, v)}, each {@code w} by {@code d}; along the bottom,
 * the <em>right</em> {@code (u, v+d)} and <em>left</em> {@code (u+d+w, v+d)} sides, {@code d} by {@code h}, the
 * <em>front</em> {@code (u+d, v+d)} and the <em>back</em> {@code (u+2d+w, v+d)}, {@code w} by {@code h}. A box with
 * {@code mirror} set is the left twin of one on the right: it draws the same rectangle, turned the other way round.
 *
 * <p><b>The forms.</b> {@code P} is Prime, {@code H} the Hybrid (his second body), {@code U} the Sentry-Ultrons (his
 * third and every later body) and {@code S} a Sentry of the army. A box is drawn only in the forms it lists: the
 * Sentry-Ultron has no pauldron overhang and thin blades for antennae, the Hybrid has a Sentry's right forearm and
 * a broken right antenna, and so on.
 *
 * <p>Positions are relative to the pivot of the part the box hangs on (see {@link #PARTS}); the model's feet are at
 * y = 24 and a body is 29 units tall (1.8 blocks), drawn at the scale of its body.
 */
public final class UltronCubes {
    /** The form of Prime, of the Hybrid, of a Sentry-Ultron and of a Sentry of the army. */
    public static final int PRIME = 0;
    public static final int HYBRID = 1;
    public static final int ULTRON = 2;
    public static final int SENTRY = 3;
    /** The letters a box lists its forms with, by form. */
    private static final String FORMS = "PHUS";
    /** The size of the skin as the model reads it (the picture is twice as large each way). */
    public static final int SKIN = 64;

    /** A part of the model's skeleton: where it pivots (absolute, y down) and what it hangs on. */
    public record Part(String name, String parent, float x, float y, float z) {
    }

    /** One box: the part it hangs on, its place on the skin, its corner and size, and the forms it is drawn in. */
    public record Cube(String name, String part, int u, int v, float x, float y, float z, int w, int h, int d, String forms, boolean mirror) {
        /** Whether the box is drawn in this form. */
        public boolean in(int form) {
            return forms.indexOf(FORMS.charAt(form)) >= 0;
        }

        /** The width of the rectangle of the skin the box fills, in units. */
        public int footW() {
            return 2 * d + 2 * w;
        }

        /** The height of the rectangle of the skin the box fills, in units. */
        public int footH() {
            return d + h;
        }
    }

    /** The skeleton: the hips carry the body, the head and the arms; the legs hang from the root. */
    public static final List<Part> PARTS = List.of(
            new Part("hips", "root", 0F, 13F, 0F),
            new Part("body", "hips", 0F, 3F, 0F),
            new Part("head", "hips", 0F, 2F, 0F),
            new Part("rightArm", "hips", -6.5F, 4.5F, 0F),
            new Part("rightForearm", "rightArm", -6.5F, 10.5F, 0F),
            new Part("rightHand", "rightForearm", -6.5F, 15.5F, 0F),
            new Part("rightFingers", "rightHand", -6.5F, 17.5F, 0F),
            new Part("leftArm", "hips", 6.5F, 4.5F, 0F),
            new Part("leftForearm", "leftArm", 6.5F, 10.5F, 0F),
            new Part("leftHand", "leftForearm", 6.5F, 15.5F, 0F),
            new Part("leftFingers", "leftHand", 6.5F, 17.5F, 0F),
            new Part("rightLeg", "root", -2.5F, 13F, 0F),
            new Part("rightShin", "rightLeg", -2.5F, 19F, 0F),
            new Part("leftLeg", "root", 2.5F, 13F, 0F),
            new Part("leftShin", "leftLeg", 2.5F, 19F, 0F));

    /** Every box of the model. */
    public static final List<Cube> CUBES = List.of(
            cube("torso", "body", 0, 0, -5F, 0F, -2.5F, 10, 10, 5, "PHUS", false),
            cube("chestR", "body", 0, 46, -4.6F, 0.6F, -3.5F, 4, 4, 1, "PHUS", false),
            cube("chestL", "body", 0, 46, 0.6F, 0.6F, -3.5F, 4, 4, 1, "PHUS", true),
            cube("spine", "body", 16, 15, -1F, 0F, 2.5F, 2, 9, 1, "PHUS", false),
            cube("thrusterR", "body", 56, 46, -4F, 3F, 2.5F, 2, 2, 1, "PHUS", false),
            cube("thrusterL", "body", 56, 46, 2F, 3F, 2.5F, 2, 2, 1, "PHUS", true),
            cube("waist", "body", 0, 25, -5.5F, 8.5F, -3F, 11, 2, 6, "PHUS", false),
            cube("neck", "body", 10, 46, -1.5F, -1F, -1.5F, 3, 1, 3, "PHUS", false),
            cube("skull", "head", 30, 0, -3.5F, -7F, -3.5F, 7, 7, 7, "PHUS", false),
            cube("brow", "head", 4, 51, -4F, -5F, -4.5F, 8, 1, 1, "PH", false),
            cube("jaw", "head", 44, 46, -2.5F, -2F, -4.5F, 5, 2, 1, "PHUS", false),
            cube("crest", "head", 34, 25, -0.5F, -9F, -3F, 1, 2, 6, "PHU", false),
            cube("antennaR1", "head", 22, 46, -4.5F, -6F, -1F, 1, 2, 2, "PH", false),
            cube("antennaR2", "head", 28, 46, -5F, -8F, 0.5F, 1, 2, 2, "P", false),
            cube("antennaR3", "head", 34, 46, -5.5F, -9.5F, 2F, 1, 2, 2, "P", false),
            cube("antennaL1", "head", 22, 46, 3.5F, -6F, -1F, 1, 2, 2, "PH", true),
            cube("antennaL2", "head", 28, 46, 4F, -8F, 0.5F, 1, 2, 2, "PH", true),
            cube("antennaL3", "head", 34, 46, 4.5F, -9.5F, 2F, 1, 2, 2, "PH", true),
            cube("bladeR", "head", 40, 33, -4.5F, -12F, 0.5F, 1, 6, 1, "US", false),
            cube("bladeL", "head", 40, 33, 3.5F, -12F, 0.5F, 1, 6, 1, "US", true),
            cube("upperArmR", "rightArm", 22, 15, -1.5F, -1F, -1.5F, 3, 6, 3, "PHUS", false),
            cube("pauldronAR", "rightArm", 0, 33, -3F, -2.5F, -2.5F, 5, 2, 5, "PH", false),
            cube("pauldronBR", "rightArm", 12, 40, -2.5F, -0.5F, -2F, 4, 1, 4, "PHUS", false),
            cube("forearmR", "rightForearm", 48, 25, -1.5F, 0F, -1.5F, 3, 5, 3, "P", false),
            cube("pistonR", "rightForearm", 40, 46, -0.5F, 0.5F, -2.5F, 1, 3, 1, "P", false),
            cube("sentryForearmR", "rightForearm", 32, 33, -1F, 0F, -1F, 2, 5, 2, "HUS", false),
            cube("palmR", "rightHand", 28, 40, -2.5F, 0F, -1.5F, 5, 2, 3, "PHUS", false),
            cube("finger0R", "rightFingers", 0, 51, -2.5F, 0F, -0.5F, 1, 2, 1, "PHUS", false),
            cube("finger1R", "rightFingers", 0, 51, -1.25F, 0F, -0.5F, 1, 2, 1, "PHUS", false),
            cube("finger2R", "rightFingers", 0, 51, 0F, 0F, -0.5F, 1, 2, 1, "PHUS", false),
            cube("finger3R", "rightFingers", 0, 51, 1.25F, 0F, -0.5F, 1, 2, 1, "PHUS", false),
            cube("finger4R", "rightFingers", 0, 51, 2.5F, 0F, -0.5F, 1, 2, 1, "PHUS", false),
            cube("upperArmL", "leftArm", 22, 15, -1.5F, -1F, -1.5F, 3, 6, 3, "PHUS", true),
            cube("pauldronAL", "leftArm", 0, 33, -2F, -2.5F, -2.5F, 5, 2, 5, "PH", true),
            cube("pauldronBL", "leftArm", 12, 40, -1.5F, -0.5F, -2F, 4, 1, 4, "PHUS", true),
            cube("forearmL", "leftForearm", 48, 25, -1.5F, 0F, -1.5F, 3, 5, 3, "PH", true),
            cube("pistonL", "leftForearm", 40, 46, -0.5F, 0.5F, -2.5F, 1, 3, 1, "PH", true),
            cube("sentryForearmL", "leftForearm", 32, 33, -1F, 0F, -1F, 2, 5, 2, "US", true),
            cube("palmL", "leftHand", 28, 40, -2.5F, 0F, -1.5F, 5, 2, 3, "PHUS", true),
            cube("finger0L", "leftFingers", 0, 51, 1.5F, 0F, -0.5F, 1, 2, 1, "PHUS", true),
            cube("finger1L", "leftFingers", 0, 51, 0.25F, 0F, -0.5F, 1, 2, 1, "PHUS", true),
            cube("finger2L", "leftFingers", 0, 51, -1F, 0F, -0.5F, 1, 2, 1, "PHUS", true),
            cube("finger3L", "leftFingers", 0, 51, -2.25F, 0F, -0.5F, 1, 2, 1, "PHUS", true),
            cube("finger4L", "leftFingers", 0, 51, -3.5F, 0F, -0.5F, 1, 2, 1, "PHUS", true),
            cube("thighR", "rightLeg", 0, 15, -2F, 0F, -2F, 4, 6, 4, "PHUS", false),
            cube("thighPlateR", "rightLeg", 0, 40, -2.5F, 0.5F, -3F, 5, 5, 1, "PHU", false),
            cube("kneeR", "rightShin", 44, 40, -2F, -1F, -2.5F, 4, 2, 3, "PHUS", false),
            cube("shinR", "rightShin", 20, 33, -1.5F, 0F, -1.5F, 3, 4, 3, "PHUS", false),
            cube("footR", "rightShin", 44, 33, -2F, 4F, -3F, 4, 1, 5, "PHUS", false),
            cube("thighL", "leftLeg", 0, 15, -2F, 0F, -2F, 4, 6, 4, "PHUS", true),
            cube("thighPlateL", "leftLeg", 0, 40, -2.5F, 0.5F, -3F, 5, 5, 1, "PHU", true),
            cube("kneeL", "leftShin", 44, 40, -2F, -1F, -2.5F, 4, 2, 3, "PHUS", true),
            cube("shinL", "leftShin", 20, 33, -1.5F, 0F, -1.5F, 3, 4, 3, "PHUS", true),
            cube("footL", "leftShin", 44, 33, -2F, 4F, -3F, 4, 1, 5, "PHUS", true));

    private static Cube cube(String name, String part, int u, int v, float x, float y, float z, int w, int h, int d, String forms, boolean mirror) {
        return new Cube(name, part, u, v, x, y, z, w, h, d, forms, mirror);
    }

    /** The box with this name, or null. */
    public static Cube named(String name) {
        for (Cube cube : CUBES) {
            if (cube.name().equals(name)) {
                return cube;
            }
        }
        return null;
    }

    private UltronCubes() {
    }
}
