package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Professor X's gestures (3.3) and the poses of his stances: a man of the mind, who moves little. Almost
 * everything he does is the two fingers at his temple (the gesture of the films); the gestures start and end on the
 * arms at rest, so one runs into the next without a pop, and the ones that hand over to a stance (the trance, Cerebro)
 * end exactly on its pose. Pure data (no Minecraft client classes), so the game tests can check it.
 */
public final class XavierGestures {
    /** The arms at rest. */
    static final float[] REST_M = {-6, 0, 4};
    static final float[] REST_O = {-6, 0, -4};
    /** The main hand's two fingers at the temple: the arm raised beside the head, turned in. */
    public static final float[] TEMPLE_M = {-150, -12, 20};
    /** The off hand at the other temple (Cerebro). */
    public static final float[] TEMPLE_O = {-150, 12, -20};
    /** The off hand held out, palm open, toward the minds he holds (Freeze Minds). */
    static final float[] HOLD_O = {-78, 8, -6};
    /** The trance: the head bowed (degrees). */
    public static final float TRANCE_HEAD = 14;
    /** The old professor in his chair: the forearms laid on the armrests (as near as a straight arm comes). */
    public static final float[] ARMREST_M = {-24, 0, -6};
    public static final float[] ARMREST_O = {-24, 0, 6};
    /** The legs in the chair: forward and a little down to the footrest, a little apart. */
    public static final float SEATED_LEG_X = -76;
    public static final float SEATED_LEG_Z = 5;

    static void register() {
        // The Mental Blast: the fingers to the temple, a push of the head toward the target, and back.
        define(GestureId.XAVIER_BLAST)
                .key(0, S).m(REST_M).head(0, 0, 0)
                .key(4, O).m(TEMPLE_M).head(-6, 0, 0)
                .key(7, O).m(TEMPLE_M).head(6, 0, 0)
                .key(16, S).m(REST_M).head(0, 0, 0);

        // Freeze Minds: the fingers to the temple, the other hand thrown out and held while the room stops, then lowered.
        define(GestureId.XAVIER_FREEZE)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(5, O).m(TEMPLE_M).o(HOLD_O).head(4, 0, 0)
                .key(17, S).m(TEMPLE_M).o(HOLD_O).head(4, 0, 0)
                .key(24, S).m(REST_M).o(REST_O).head(0, 0, 0);

        // The mind leaves: the fingers rise to the temple, the head bows, and the stance of the trance takes over.
        define(GestureId.XAVIER_LINK)
                .key(0, S).m(REST_M).head(0, 0, 0)
                .key(6, O).m(TEMPLE_M).head(TRANCE_HEAD * 0.5F, 0, 0)
                .key(14, E).m(TEMPLE_M).head(TRANCE_HEAD, 0, 0);

        // Cerebro: both hands lift the helmet, over the head, and settle at its sides (the stance of Cerebro).
        define(GestureId.XAVIER_CEREBRO)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(6, O).m(-170, -6, 10).o(-170, 6, -10).head(-6, 0, 0)
                .key(12, O).m(TEMPLE_M).o(TEMPLE_O).head(-2, 0, 0)
                .key(18, E).m(TEMPLE_M).o(TEMPLE_O).head(0, 0, 0);

        // Hear Thoughts on: the fingers brush the temple and the head tilts, as if to listen.
        define(GestureId.XAVIER_THOUGHTS_ON)
                .key(0, S).m(REST_M).head(0, 0, 0)
                .key(5, O).m(TEMPLE_M).head(2, 0, 8)
                .key(14, S).m(REST_M).head(0, 0, 0);

        // Hear Thoughts off: a small shake of the head, the voices let go.
        define(GestureId.XAVIER_THOUGHTS_OFF)
                .key(0, S).head(0, 0, 0)
                .key(3, O).head(0, -8, 0)
                .key(6, O).head(0, 8, 0)
                .key(10, S).head(0, 0, 0);
    }

    private XavierGestures() {
    }
}
