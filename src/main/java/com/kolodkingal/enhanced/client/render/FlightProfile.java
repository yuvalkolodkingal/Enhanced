package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * How each hero flies: the numbers behind the lean, the bank, the hover bob and the three anchor poses (hover, cruise,
 * sprint) that {@link FlightPoses} blends between. Pure data and maths (no Minecraft client imports), so the game tests
 * can check it.
 *
 * <p>Speeds are normalised per hero ({@link #normalised}): {@code s = 1} is the hero's cruise, {@code s = 2} a sprint,
 * whatever the configured flight speed, so one curve means the same thing for everyone.
 */
public final class FlightProfile {
    /** Index of the hover, cruise and sprint anchors. */
    public static final int HOVER = 0;
    public static final int CRUISE = 1;
    public static final int SPRINT = 2;

    /** Pitch (degrees, forward positive) at cruise and at a sprint, and the lean back of a backwards flight. */
    public final float p1;
    public final float p2;
    public final float pBack;
    /** The part of the lean a steep climb takes away, and the extra tip over of a dive (degrees). */
    public final float kClimb;
    public final float diveAdd;
    /** The farthest bank (degrees), degrees of bank per degree turned per tick, and per unit of sideways speed. */
    public final float maxRoll;
    public final float kTurn;
    public final float kStrafe;
    /** Time constants (ticks): the roll, and the pitch rising and falling. */
    public final float tauRoll;
    public final float tauPitchIn;
    public final float tauPitchOut;
    /** Thor's pitch is an under-damped spring instead: heavy stops, surging starts. */
    public final boolean spring;
    public final float springOmega;
    public final float springZeta;
    /** The hover bob: amplitude in blocks and period in ticks; arm drift and leg sway in degrees; leg flutter at speed. */
    public final float bobAmp;
    public final float bobPeriod;
    public final float armDrift;
    public final float legSway;
    public final float legFlutter;
    /** Billy sways lazily in a hover (degrees of roll). */
    public final float lazyRoll;
    /** Whether the hero stops as dead as a statue (no braking gesture). */
    public final boolean noBrake;
    /** Fallback flight speed when the config is not loaded (blocks per tick of acceleration). */
    public final float defaultSpeed;
    /** Whether the main arm of the anchors is held by its world elevation (Thor: it carries the weapon). */
    public final boolean mWorld;
    /** The anchor poses: per anchor, M arm (x, y, z), O arm (x, y, z), right leg (x, z), left leg (x, z); degrees. */
    private final float[][] anchors;

    private FlightProfile(float[] pitch, float kClimb, float diveAdd, float maxRoll, float kTurn, float kStrafe, float tauRoll,
                          boolean spring, float[] dynamics, float[] bob, float armDrift, float legSway, float legFlutter, float lazyRoll,
                          boolean noBrake, float defaultSpeed, boolean mWorld, float[][] anchors) {
        this.p1 = pitch[0];
        this.p2 = pitch[1];
        this.pBack = pitch[2];
        this.kClimb = kClimb;
        this.diveAdd = diveAdd;
        this.maxRoll = maxRoll;
        this.kTurn = kTurn;
        this.kStrafe = kStrafe;
        this.tauRoll = tauRoll;
        this.spring = spring;
        this.springOmega = spring ? dynamics[0] : 0.0F;
        this.springZeta = spring ? dynamics[1] : 0.0F;
        this.tauPitchIn = spring ? 0.0F : dynamics[0];
        this.tauPitchOut = spring ? 0.0F : dynamics[1];
        this.bobAmp = bob[0];
        this.bobPeriod = bob[1];
        this.armDrift = armDrift;
        this.legSway = legSway;
        this.legFlutter = legFlutter;
        this.lazyRoll = lazyRoll;
        this.noBrake = noBrake;
        this.defaultSpeed = defaultSpeed;
        this.mWorld = mWorld;
        this.anchors = anchors;
    }

    private static float[] pose(float... values) {
        return values;
    }

    private static final FlightProfile MAGNETO = new FlightProfile(
            new float[]{22, 40, -8}, 0.40F, 8, 8, 2.5F, 8, 8, false, new float[]{7, 7}, new float[]{0.03F, 80}, 2.5F, 1.5F, 2.5F, 0.0F,
            true, 0.075F, false, new float[][]{
            pose(-6, 0, 18, -6, 0, -18, 2, 2, 2, -2),
            pose(-18, 0, 24, -18, 0, -24, 8, 2, 8, -2),
            pose(-30, 0, 32, -30, 0, -32, 12, 2, 12, -2)});
    private static final FlightProfile WANDA = new FlightProfile(
            new float[]{38, 62, -8}, 0.45F, 18, 18, 4.0F, 12, 4, false, new float[]{4.5F, 4}, new float[]{0.06F, 78}, 4.5F, 3.4F, 2.5F, 0.0F,
            false, 0.09F, false, new float[][]{
            pose(-20, 6, 32, -20, -6, -32, 7, 0, 4, 0),
            pose(-108, 8, 10, -108, -8, -10, 12, 2, 7, -2),
            pose(-152, 10, 8, -152, -10, -8, 14, 2, 8, -2)});
    private static final FlightProfile VISION = new FlightProfile(
            new float[]{36, 66, -6}, 0.30F, 36, 12, 3.0F, 8, 5, false, new float[]{3.5F, 1.6F}, new float[]{0.015F, 100}, 1.7F, 0.0F, 0.0F, 0.0F,
            true, 0.08F, false, new float[][]{
            pose(2, 0, 8, 2, 0, -8, 0, 1, 0, -1),
            pose(8, 0, 6, 8, 0, -6, 3, 1, 3, -1),
            pose(16, 0, 8, 16, 0, -8, 5, 1, 5, -1)});
    private static final FlightProfile WHITE_VISION = new FlightProfile(
            new float[]{36, 66, -6}, 0.30F, 36, 8, 3.0F, 8, 4, false, new float[]{3.5F, 1.2F}, new float[]{0.008F, 100}, 0.0F, 0.0F, 0.0F, 0.0F,
            true, 0.08F, false, new float[][]{
            pose(2, 0, 8, 2, 0, -8, 0, 1, 0, -1),
            pose(8, 0, 6, 8, 0, -6, 3, 1, 3, -1),
            pose(16, 0, 8, 16, 0, -8, 5, 1, 5, -1)});
    private static final FlightProfile BILLY = new FlightProfile(
            new float[]{30, 55, -10}, 0.50F, 14, 20, 4.5F, 12, 5, false, new float[]{6, 6}, new float[]{0.08F, 70}, 3.4F, 5.0F, 2.5F, 2.0F,
            false, 0.08F, false, new float[][]{
            pose(-28, -12, 28, -28, 12, -28, 10, 3, -6, -3),
            pose(-50, -10, 38, -50, 10, -38, 14, 3, 6, -3),
            pose(-125, 0, 22, -125, 0, -22, 16, 2, 10, -2)});
    private static final FlightProfile THOR = new FlightProfile(
            new float[]{72, 88, -6}, 0.35F, 40, 25, 5.0F, 8, 3.5F, true, new float[]{0.42F, 0.56F}, new float[]{0.04F, 60}, 2.3F, 6.0F, 1.5F, 0.0F,
            false, 0.11F, true, new float[][]{
            pose(158, 0, 0, -8, 0, -30, 0, 2, 0, -2),
            pose(100, 0, 0, 12, 0, -8, 8, 2, 8, -2),
            pose(96, 0, 0, 20, 0, -4, 10, 2, 10, -2)});

    /**
     * The Phoenix: a calm telekinetic flier whose arms spread like wings as she speeds up, neither the Scarlet Witch's
     * forward thrust nor Vision's stiffness.
     */
    private static final FlightProfile PHOENIX = new FlightProfile(
            new float[]{30, 58, -8}, 0.45F, 16, 14, 3.5F, 10, 5, false, new float[]{4.5F, 4.5F}, new float[]{0.05F, 76}, 3.5F, 3.0F, 2.0F, 0.0F,
            false, 0.09F, false, new float[][]{
            pose(-10, 0, 24, -10, 0, -24, 4, 1, 2, -1),    // hover: arms low and a little out
            pose(-6, 0, 58, -6, 0, -58, 8, 2, 5, -2),      // cruise: arms spread like a bird's
            pose(14, 0, 72, 14, 0, -72, 10, 2, 10, -2)});  // sprint: arms swept back and out, wings

    // ===== ultron =====
    /**
     * Ultron: an upright, rigid, silent flier carried by the anti-gravity of his palms, which hang a little out with the
     * palms down. A machine does not lean into the wind like a man: ten degrees at a cruise and thirty-two at a sprint, a
     * dive tips him over only a little, the banking is slight and the arms and legs hold their poses (no sway, no flutter,
     * hardly a bob). He leans in deliberately and stands up straight again at once when he stops (a quick fall of the
     * pitch and no braking gesture): he stops as dead as a machine.
     */
    private static final FlightProfile ULTRON = new FlightProfile(
            new float[]{10, 32, -5}, 0.30F, 12, 6, 2.0F, 5, 7, false, new float[]{5, 2.5F}, new float[]{0.018F, 100}, 1.0F, 0.0F, 0.0F, 0.0F,
            true, 0.10F, false, new float[][]{
            pose(-6, 0, 18, -6, 0, -18, 2, 1, 2, -1),     // hover: palms down, arms a little out, legs straight
            pose(-10, 0, 14, -10, 0, -14, 4, 1, 4, -1),   // cruise: the arms nearer the body
            pose(-18, 0, 10, -18, 0, -10, 6, 1, 6, -1)}); // sprint: the arms swept slightly forward, a rigid dart

    // ===== apocalypse =====
    /** A calm, upright, composed flier, like Vision but with the arms a little open: a god at ease. */
    private static final FlightProfile APOCALYPSE = new FlightProfile(
            new float[]{30, 56, -6}, 0.30F, 28, 10, 3.0F, 8, 5, false, new float[]{3.5F, 1.8F}, new float[]{0.02F, 96},
            2.0F, 0.6F, 0.4F, 0.0F, true, 0.08F, false, new float[][]{
            pose(-8, 0, 14, -8, 0, -14, 2, 1, 1, -1),
            pose(-14, 0, 20, -14, 0, -20, 4, 1, 3, -1),
            pose(-26, 0, 26, -26, 0, -26, 6, 1, 5, -2)});

    // ===== agatha (3.3) =====
    /**
     * Agatha: a witch lifted by her own magic, upright and unhurried (WandaVision's finale), the arms a little open with the
     * hands low, the legs together like a skirt's. She leans little into a cruise, glides with the arms trailing back at a
     * sprint, and sways a little as she hovers.
     */
    private static final FlightProfile AGATHA = new FlightProfile(
            new float[]{22, 48, -6}, 0.35F, 22, 12, 3.0F, 9, 5, false, new float[]{4.0F, 2.5F}, new float[]{0.04F, 84}, 2.5F, 1.0F, 0.6F, 1.0F,
            false, 0.085F, false, new float[][]{
            pose(-14, 0, 26, -14, 0, -26, 2, 1, 1, -1),    // hover: the hands low and open, the legs together
            pose(-4, 0, 30, -4, 0, -30, 6, 1, 4, -1),      // cruise: the arms drift back a little
            pose(18, 0, 40, 18, 0, -40, 10, 1, 8, -1)});   // sprint: the arms trail behind, open

    /**
     * The profile a hero flies with: his own for a built-in hero, the one of his flight style for a custom hero (3.4,
     * {@link HeroAnimState#flightAs}), or null.
     */
    public static FlightProfile of(HeroAnimState st) {
        return of(st.flightAs, st.flightWhite);
    }

    /** The profile of a flier, or null for a hero that does not fly. */
    public static FlightProfile of(Power power, boolean whiteVision) {
        return switch (power) {
            case MAGNETO -> MAGNETO;
            case SCARLET_WITCH -> WANDA;
            case VISION -> whiteVision ? WHITE_VISION : VISION;
            case BILLY -> BILLY;
            case THOR -> THOR;
            // The heroes of 3.0, one line each (each lane's own): none has a flight profile until its lane makes one.
            case PHOENIX -> PHOENIX;
            case SPIDER_MAN -> null;
            case THANOS -> null; // he does not fly: no take-off, banking or braking (his landings are ThanosActing's)
            case ULTRON -> ULTRON;
            case APOCALYPSE -> APOCALYPSE;
            case AGATHA -> AGATHA;
            // ===== cyclops (3.3) =====
            case CYCLOPS -> null; // he runs, and leaps on his blast: no take-off, banking or braking
            case PROFESSOR_X -> null; // 3.3: he walks, or rolls in his chair
            // A custom hero flies with the profile of his flight style (3.4: of(HeroAnimState)); the plain style is the
            // game's own pose.
            case NONE, QUICKSILVER, TOMMY, LOKI, CUSTOM -> null;
        };
    }

    /** The terminal horizontal speed of normal flight (blocks per tick) for a flight speed setting: air friction 0.91. */
    public static float vRef(float flightSpeed) {
        return 10.11F * flightSpeed;
    }

    /** The terminal vertical speed (blocks per tick) holding jump or sneak. */
    public static float vyRef(float flightSpeed) {
        return 4.5F * flightSpeed;
    }

    /** The unit of speed: 1 is cruise, 2 a sprint. Clamped to {@code [-1, 3]}. */
    public static float normalised(float blocksPerTick, float flightSpeed) {
        return AnimMath.clamp(blocksPerTick / vRef(flightSpeed), -1.0F, 3.0F);
    }

    /**
     * The forward pitch (degrees) the body wants for a forward speed {@code s} (1 = cruise) and a climb {@code u}
     * (-1 diving at full speed, +1 climbing): upright when hovering, leaning back a little in reverse, straighter when
     * climbing, tipped over by a dive.
     */
    public float pitchTarget(float s, float u) {
        float level;
        if (s >= 0.0F) {
            level = p1 * AnimMath.ss(0.0F, 1.0F, s) + (p2 - p1) * AnimMath.ss(1.0F, 2.0F, s);
        } else {
            level = pBack * AnimMath.ss(0.0F, 0.6F, -s);
        }
        float pitch;
        if (u >= 0.0F) {
            pitch = level * (1.0F - kClimb * AnimMath.ss(0.0F, 1.0F, u));
        } else {
            pitch = level + diveAdd * AnimMath.ss(0.0F, 1.0F, -u) * AnimMath.ss(0.3F, 1.0F, s);
        }
        return AnimMath.clamp(pitch, pBack * 1.5F, 112.0F);
    }

    /**
     * The bank (degrees, right side down positive) for a turn of {@code turn} degrees per tick (right positive), a speed
     * {@code sH} and a sideways speed {@code sSide}.
     */
    public float rollTarget(float turn, float sH, float sSide) {
        float roll = kTurn * turn * AnimMath.ss(0.10F, 0.80F, sH) + kStrafe * AnimMath.clamp(sSide, -1.5F, 1.5F);
        return AnimMath.clamp(roll, -maxRoll, maxRoll);
    }

    /** One value of an anchor pose: {@code index} 0..9 = M(x y z), O(x y z), right leg (x z), left leg (x z). */
    public float anchor(int anchor, int index) {
        return anchors[anchor][index];
    }

    /**
     * The blended pose for a normalised speed: {@code lerp(wS, lerp(wC, hover, cruise), sprint)} with {@code wC = ss(0, 1, s)}
     * and {@code wS = ss(1, 2, s)}. {@code out} receives the ten values of {@link #anchor}.
     */
    public void blend(float s, float[] out) {
        float wC = AnimMath.ss(0.0F, 1.0F, Math.max(s, 0.0F));
        float wS = AnimMath.ss(1.0F, 2.0F, Math.max(s, 0.0F));
        for (int i = 0; i < 10; i++) {
            out[i] = AnimMath.lerp(wS, AnimMath.lerp(wC, anchors[HOVER][i], anchors[CRUISE][i]), anchors[SPRINT][i]);
        }
    }

    /** The hero's pitch (degrees) at one anchor, for the arms that are held by their world elevation. */
    public float anchorPitch(int anchor) {
        return switch (anchor) {
            case HOVER -> 0.0F;
            case CRUISE -> p1;
            default -> p2;
        };
    }
}
