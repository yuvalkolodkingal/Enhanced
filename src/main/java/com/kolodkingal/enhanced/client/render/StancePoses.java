package com.kolodkingal.enhanced.client.render;

/**
 * The raw arm poses (degrees; x, y, z; a right-handed player's signs) of the stances that a gesture hands over to. The
 * stance rows in {@link Stances} and the last keys of those gestures both read them, so a gesture ends exactly on the pose
 * that takes over and nothing pops. Pure data, so the game tests can check the hand-overs.
 */
public final class StancePoses {
    /** Magneto's field: palms down. */
    public static final float[] MAG_FIELD_M = {-6, 0, 20};
    public static final float[] MAG_FIELD_O = {-6, 0, -20};
    /** Vision phased: serene, palms a little forward. */
    public static final float[] VISION_PHASED_M = {-20, 0, 10};
    public static final float[] VISION_PHASED_O = {-20, 0, -10};
    /** Vision dense: braced, arms a little out. */
    public static final float[] VISION_DENSE_M = {-9, 0, 14};
    public static final float[] VISION_DENSE_O = {-9, 0, -14};
    /** The Chaos Shield: both hands up and out, the head a little back. */
    public static final float[] SHIELD_WANDA_M = {-63, 10, 31};
    public static final float[] SHIELD_WANDA_O = {-63, -10, -31};
    public static final float[] SHIELD_WANDA_HEAD = {-4, 0, 0};
    public static final float SHIELD_WANDA_LEAN = -3;
    /** Billy's freeze: both hands raised toward the spellbound. */
    public static final float[] BILLY_FREEZE_M = {-126, 8, 11};
    public static final float[] BILLY_FREEZE_O = {-126, -8, -11};
    /** Billy's Hex Barrier (3.2): the off arm bent forward across the chest holding the plate, the main arm braced back and out. */
    public static final float[] BARRIER_BILLY_M = {10, 8, 20};
    public static final float[] BARRIER_BILLY_O = {-80, 25, -6};
    public static final float BARRIER_BILLY_TWIST = 8;
    /** The God of Thunder and the Frost Giant spread their arms a little (added to the arm below). */
    public static final float THOR_UNLEASHED_Z = 13;
    public static final float LOKI_FROST_Z = 11;
    /** Wanda's beam: both hands along the look, converging a little (offsets of an AIM row). */
    public static final float[] BEAM_WANDA_M_AIM = {0, -10, 0};
    public static final float[] BEAM_WANDA_O_AIM = {0, 10, 0};
    /** The Mind Stone's beam. */
    public static final float THOR_BEAM_TWIST = -10;

    // ===== apocalypse =====
    /** Apocalypse's Shield: both forearms raised before the chest, palms out, elbows wide. */
    public static final float[] APOC_SHIELD_M = {-78, 8, 24};
    public static final float[] APOC_SHIELD_O = {-78, -8, -24};
    public static final float APOC_SHIELD_HEAD = -3;
    public static final float APOC_SHIELD_LEAN = -2;
    /**
     * The Sandstorm: both arms flung wide and up in a V, the head back. (The design's {@code -118, 0, 74} points a forward-raised
     * arm and then tips it inward, so the arms cross in front of the chest: an arm raised above the shoulder is spread by a large
     * {@code z}, with little {@code x}.)
     */
    public static final float[] APOC_STORM_M = {-14, 0, 132};
    public static final float[] APOC_STORM_O = {-14, 0, -132};
    public static final float APOC_STORM_HEAD = -12;
    public static final float APOC_STORM_LEAN = -3;

    private StancePoses() {
    }
}
