package com.kolodkingal.enhanced.client.healing;

import com.kolodkingal.enhanced.client.render.GestureId;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the healing factor's gestures (3.7), shared by Wolverine, Deadpool and a custom hero with the healing
 * factor: DOWN (to one knee, a hand to the ground, onto his side; held while he is DOWN, then up again), the RISE (a
 * shake of the head, the shoulders rolled back) and the BURST (the arms flung wide). Nothing of a wound is ever drawn:
 * the light of the healing factor is the glints ({@link HealingClient}). Pure data (no Minecraft client classes), so
 * the game tests can check it; {@code Gestures} registers it with the shared tables.
 */
public final class HealingGestures {
    /** The arms at rest: the main arm, then the other. */
    static final float[] REST_M = {0, 0, 4};
    static final float[] REST_O = {0, 0, -4};
    /** On one knee: the right leg forward, its foot planted; the left knee down; how far he sinks and bows. */
    public static final float KNEEL_LEG_R = -82;
    public static final float KNEEL_LEG_L = 12;
    public static final float KNEEL_DROP = 0.42F;
    public static final float KNEEL_LEAN = 16;
    /** Lying on his side: the roll of the body, how far it sinks, the knees a little drawn up. */
    public static final float LIE_ROLL = 84;
    public static final float LIE_DROP = 0.18F;
    public static final float LIE_LEG_R = -34;
    public static final float LIE_LEG_L = -22;
    /** The tick DOWN holds at while he is DOWN (its length less its release). */
    public static final int LIE_TICK = 24;

    private HealingGestures() {
    }

    public static void register() {
        // DOWN: the knees give (one knee to the ground), the off hand goes down to catch him, he rolls onto his side
        // and lies still (held here while he is DOWN); then the hand pushes, he comes up on one knee and stands.
        define(GestureId.HEALING_DOWN)
                .key(0, S).m(REST_M).o(REST_O).legs(0, 0).drop(0).lean(0).roll(0).head(0, 0, 0)
                .key(6, O).m(-24, 0, 10).o(-44, 0, -12).legs(KNEEL_LEG_R, KNEEL_LEG_L).drop(KNEEL_DROP)
                .lean(KNEEL_LEAN).roll(0).head(22, 0, 0)
                .key(12, S).m(-30, 0, 14).o(-70, 0, -16).legs(KNEEL_LEG_R, KNEEL_LEG_L).drop(KNEEL_DROP + 0.04F)
                .lean(KNEEL_LEAN + 10).roll(10).head(26, 0, 0)
                .key(LIE_TICK, E).m(-62, 0, 26).o(-128, 0, -18).legs(LIE_LEG_R, LIE_LEG_L).drop(LIE_DROP).lean(4)
                .roll(LIE_ROLL).head(10, 0, -18)
                .key(30, O).m(-30, 0, 14).o(-72, 0, -16).legs(KNEEL_LEG_R, KNEEL_LEG_L).drop(KNEEL_DROP)
                .lean(KNEEL_LEAN + 6).roll(6).head(18, 0, 0)
                .key(38, S).m(-8, 0, 8).o(-10, 0, -8).legs(-10, 4).drop(0.06F).lean(4).roll(0).head(4, 0, 0)
                .key(44, S).m(REST_M).o(REST_O).legs(0, 0).drop(0).lean(0).roll(0).head(0, 0, 0);

        // The rise: he shakes his head (left, right, left) and rolls his shoulders back, the chest up.
        define(GestureId.HEALING_RISE)
                .key(0, S).mAdd(0, 0, 0).oAdd(0, 0, 0).head(0, 0, 0).lean(0)
                .key(3, O).head(8, 24, 0)
                .key(7, E).head(8, -24, 0).mAdd(10, 0, 8).oAdd(10, 0, -8).lean(-3)
                .key(11, E).head(6, 16, 0).mAdd(-4, 0, 4).oAdd(-4, 0, -4)
                .key(15, S).head(0, 0, 0).mAdd(0, 0, 0).oAdd(0, 0, 0).lean(0)
                .key(18, S).head(0, 0, 0);

        // The burst: the arms flung wide and a little up, the chest out, the head back; held while the light runs
        // through him, then down.
        define(GestureId.HEALING_BURST)
                .key(0, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0)
                .key(4, O).m(-34, 0, 96).o(-34, 0, -96).lean(-8).head(-18, 0, 0)
                .key(11, S).m(-30, 0, 92).o(-30, 0, -92).lean(-7).head(-16, 0, 0)
                .key(18, I).m(REST_M).o(REST_O).lean(0).head(0, 0, 0);
    }
}
