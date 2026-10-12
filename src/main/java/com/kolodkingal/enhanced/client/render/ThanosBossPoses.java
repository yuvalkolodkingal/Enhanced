package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.render.AnimMath.Ease;
import com.kolodkingal.enhanced.thanos.ThanosMove;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.B;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;

/**
 * The poses of the Mad Titan's body (DESIGN section 15.6.1): one per kind of move, and one each for a new stone, a
 * stagger, the arrival and the death. A pose is a list of keys; a key is a tick, the easing of the segment that arrives
 * at it and the value of every channel from then on (a channel a key does not mention keeps the value it had). The
 * model reads the pose at the tick of the move (or the time in the state) and puts the channels on its parts.
 *
 * <p>{@code R} is the sword arm (the right one), {@code G} the Gauntlet arm (the left); arm values are the model's raw
 * angles in degrees ({@code xRot} -90 is straight forward, -180 straight up; {@code zRot} positive is outward on the
 * right arm and negative on the left). A key tick below zero counts from the end of the pose, so a pose that holds for
 * as long as its move lasts needs no number of its own. No Minecraft classes: the game tests check the table.
 */
public final class ThanosBossPoses {
    /** The channels of a pose, in the order of {@link Out#values}. */
    public enum Ch {
        /** The sword arm. */
        RX, RY, RZ,
        /** The Gauntlet arm. */
        GX, GY, GZ,
        /** The body's turn and its lean forward, in degrees. */
        TWIST, LEAN,
        /** How far the whole body sinks, in model pixels. */
        DROP,
        /** The legs' swing (right, left), in degrees. */
        LEG_R, LEG_L,
        /** The head's pitch (positive looks down), yaw and roll added to where it looks, in degrees. */
        HEAD_X, HEAD_Y, HEAD_Z,
        /** How much the Gauntlet arm points along the move's aim instead (0 to 1). */
        AIM,
        /** The amplitude of the Gauntlet arm's tremble, in degrees. */
        TREMBLE,
        /** The whirl of the sword about its grip, in degrees per tick. */
        SPIN
    }

    /** Which pose a move or a state is. */
    public enum Pose {
        STAND(0), SWEEP(0), SMASH(0), JAB(0), THROW(0), CHOKE(0), LEAP(0), POINT(0), GAUNTLET_UP(0), SLAM(0), PALM_OUT(0), STEP(0),
        WIDE(0), PRESS(0), SIGHT(0), INFINITY(0),
        /** Falling through the portal: the limbs flung out and held until he lands. */
        FALL(20),
        /** A new stone: he kneels, the Gauntlet arm shaking. */
        SURGE(60),
        /** His poise broke: a stumble. */
        STAGGER(40),
        /** The landing: kneeling, then rising. */
        ARRIVE(60),
        /** The death: one knee, then seated. */
        DEATH(100);

        private final int length;

        Pose(int length) {
            this.length = length;
        }

        /** How long a pose of a state lasts in ticks; 0 for a pose that lasts as long as its move does. */
        public int length() {
            return length;
        }
    }

    /** What a pose says at one moment: the value of every channel. */
    public static final class Out {
        public final float[] values = new float[Ch.values().length];

        public float get(Ch channel) {
            return values[channel.ordinal()];
        }

        /** The pose he stands in. */
        public Out stand() {
            System.arraycopy(STAND, 0, values, 0, values.length);
            return this;
        }
    }

    /** One key of a pose. */
    private record Key(float tick, Ease ease, float[] values) {
    }

    private static final float[] STAND = new float[Ch.values().length];
    /** The whirl of the Throw's wind-up and the Smash's: the guard forms a ring. */
    private static final float WHIRL = 40.0F;
    /** The tremble of a hand that holds a stone's power. */
    private static final float SHAKE = 1.3F;

    /**
     * The angle the whirling sword has turned through after one more frame: the angle so far (degrees) and the whirl in
     * degrees per tick, for {@code ticks} ticks since the last frame (a pause of more than four is a stall, not a
     * spin). Carried from frame to frame, so a whirl that begins, speeds up or ends never makes the sword jump (the
     * whirl times the age of the entity would, each time the rate changes).
     */
    public static float turned(float angle, float rate, float ticks) {
        return (angle + rate * AnimMath.clamp(ticks, 0.0F, 4.0F)) % 360.0F;
    }

    static {
        STAND[Ch.RX.ordinal()] = -22.0F;
        STAND[Ch.RZ.ordinal()] = 6.0F;
        STAND[Ch.GX.ordinal()] = -8.0F;
        STAND[Ch.GZ.ordinal()] = -8.0F;
        STAND[Ch.LEAN.ordinal()] = 3.0F;
        STAND[Ch.HEAD_X.ordinal()] = 4.0F;
    }

    private static final Map<Pose, List<Key>> POSES = new EnumMap<>(Pose.class);

    /** Builds a pose key by key. */
    private static final class Builder {
        private final List<Key> keys = new ArrayList<>();
        private float[] now = STAND.clone();

        /** A key that returns every channel to the standing pose. */
        Builder stand(float tick, Ease ease) {
            now = STAND.clone();
            keys.add(new Key(tick, ease, now.clone()));
            return this;
        }

        /** A key that sets the channels listed (channel, value, channel, value ...) and keeps the rest as they were. */
        Builder key(float tick, Ease ease, Object... set) {
            for (int i = 0; i < set.length; i += 2) {
                now[((Ch) set[i]).ordinal()] = ((Number) set[i + 1]).floatValue();
            }
            keys.add(new Key(tick, ease, now.clone()));
            return this;
        }

        void as(Pose pose) {
            POSES.put(pose, keys);
        }
    }

    static {
        // Sword Sweep (tell 16, strike 6): the sword raised behind the shoulder, then across in a great arc.
        new Builder().stand(0, S)
                .key(8, E, Ch.RX, -160, Ch.RY, -20, Ch.RZ, -18, Ch.GX, -10, Ch.GZ, -30, Ch.TWIST, 28, Ch.LEAN, -6)
                .key(12, S)
                .key(16, B, Ch.RX, -85, Ch.RY, -70, Ch.RZ, 0, Ch.TWIST, -35)
                .key(22, O, Ch.RY, 70, Ch.TWIST, 40)
                .stand(30, S).as(Pose.SWEEP);
        // Overhead Smash (tell 24, impact 4 blocks ahead): a crouch, the leap with the sword in both hands, the blow.
        new Builder().stand(0, S)
                .key(6, E, Ch.DROP, 2.4, Ch.LEG_R, -30, Ch.LEG_L, -30, Ch.RX, -60, Ch.RY, 0, Ch.RZ, 0, Ch.GX, -60, Ch.GZ, 0)
                .key(14, O, Ch.DROP, -8, Ch.RX, -175, Ch.GX, -175, Ch.LEAN, -10, Ch.LEG_R, -10, Ch.LEG_L, -10, Ch.SPIN, WHIRL)
                .key(24, I, Ch.RX, -30, Ch.GX, -30, Ch.LEAN, 22, Ch.DROP, 3.2, Ch.SPIN, 0, Ch.LEG_R, -30, Ch.LEG_L, -30)
                .key(30, S)
                .stand(40, S).as(Pose.SMASH);
        // Gauntlet Jab (tell 8): the Gauntlet cocked back, thrust twice, eight ticks apart.
        new Builder().key(0, S, Ch.GX, 20, Ch.GZ, -8, Ch.TWIST, -20)
                .key(8, O, Ch.GX, -90, Ch.GZ, -5, Ch.TWIST, 20)
                .key(12, S, Ch.GX, -20, Ch.GZ, -8)
                .key(16, O, Ch.GX, -90, Ch.GZ, -5, Ch.TWIST, 20)
                .stand(22, S).as(Pose.JAB);
        // Sword Throw (tell 14): the sword spins in his hand, then is thrown.
        new Builder().stand(0, S)
                .key(10, E, Ch.RX, -150, Ch.RY, 10, Ch.RZ, 10, Ch.TWIST, 24, Ch.SPIN, WHIRL)
                .key(14, B, Ch.RX, -85, Ch.RY, -5, Ch.RZ, 0, Ch.TWIST, -30, Ch.SPIN, 0)
                .stand(24, S).as(Pose.THROW);
        // Choke (tell 14, then the hold): the hand reaches out open, closes and lifts, and at the end throws.
        new Builder().stand(0, S)
                .key(8, O, Ch.RX, -90, Ch.RY, -10)
                .key(14, S, Ch.RX, -125, Ch.RY, 0, Ch.LEAN, -4)
                .key(-10, S)
                .key(-6, O, Ch.RX, -30, Ch.RY, 10, Ch.TWIST, -20, Ch.LEAN, 6)
                .stand(-0.01F, S).as(Pose.CHOKE);
        // Leap (tell 18): a deep crouch, the jump with the arms thrown up, the landing.
        new Builder().stand(0, S)
                .key(10, E, Ch.DROP, 3, Ch.LEG_R, -40, Ch.LEG_L, -40, Ch.RX, 25, Ch.RZ, 6, Ch.GX, 25)
                .key(18, B, Ch.DROP, -4, Ch.LEG_R, -15, Ch.LEG_L, 10, Ch.RX, -160, Ch.RZ, 0, Ch.GX, -160, Ch.LEAN, -8)
                .key(30, S)
                .key(36, I, Ch.DROP, 3, Ch.LEG_R, -30, Ch.LEG_L, -30, Ch.RX, -30, Ch.GX, -30, Ch.LEAN, 14)
                .stand(-0.01F, S).as(Pose.LEAP);
        // The clench of a stone that aims (Power Blast, Overload, the Pulse, the Orb, Soul Drain): the Gauntlet arm
        // along the aim, trembling, the other arm out for balance, the head toward the target.
        new Builder().stand(0, S)
                .key(5, O, Ch.AIM, 1, Ch.RX, -40, Ch.RY, 0, Ch.RZ, 12, Ch.LEAN, -2, Ch.TREMBLE, SHAKE, Ch.HEAD_X, 0)
                .key(-5, S)
                .stand(-0.01F, S).as(Pose.POINT);
        // The Gauntlet raised to the sky and held, trembling (Meteor Hail, the Rewind).
        new Builder().stand(0, S)
                .key(7, O, Ch.GX, -175, Ch.GZ, -6, Ch.RX, -30, Ch.RZ, 10, Ch.LEAN, -5, Ch.HEAD_X, -10, Ch.TREMBLE, 1.6)
                .key(-6, S)
                .stand(-0.01F, S).as(Pose.GAUNTLET_UP);
        // The Titan Shockwave (tell 20): the Gauntlet raised over his head, fist glowing, then slammed down.
        new Builder().stand(0, S)
                .key(8, O, Ch.GX, -175, Ch.GZ, -6, Ch.RX, -30, Ch.RZ, 10, Ch.LEAN, -5, Ch.HEAD_X, -10, Ch.TREMBLE, 1.6)
                .key(16, S)
                .key(20, I, Ch.GX, -20, Ch.GZ, -8, Ch.LEAN, 14, Ch.DROP, 2, Ch.HEAD_X, 10, Ch.TREMBLE, 0)
                .key(26, S)
                .stand(36, S).as(Pose.SLAM);
        // The palm held out (Gravity Pull; the Barrier is a fist closing).
        new Builder().stand(0, S)
                .key(5, O, Ch.GX, -95, Ch.GZ, -12, Ch.RX, -30, Ch.RZ, 10, Ch.LEAN, -4, Ch.TREMBLE, 0.8)
                .key(-5, S)
                .stand(-0.01F, S).as(Pose.PALM_OUT);
        // The Portal Step (tell 16): the Gauntlet draws a circle in the air.
        new Builder().stand(0, S)
                .key(3, E, Ch.GX, -30, Ch.GY, 10, Ch.GZ, -10)
                .key(7, E, Ch.GX, -80, Ch.GY, -25, Ch.GZ, -30)
                .key(11, E, Ch.GX, -60, Ch.GY, -45, Ch.GZ, -10)
                .key(15, E, Ch.GX, -30, Ch.GY, -20, Ch.GZ, -5)
                .key(18, S, Ch.GX, -8, Ch.GY, 0, Ch.GZ, -8)
                .stand(24, S).as(Pose.STEP);
        // Arms flung wide (the Reality Veil's wave, the Phantoms).
        new Builder().stand(0, S)
                .key(5, O, Ch.RX, -20, Ch.RZ, 45, Ch.GX, -20, Ch.GZ, -45, Ch.HEAD_X, -6, Ch.LEAN, 0)
                .key(-5, S)
                .stand(-0.01F, S).as(Pose.WIDE);
        // Both hands pressing down (the Time Snare's discs, the Soul World's pillar).
        new Builder().stand(0, S)
                .key(8, O, Ch.RX, -80, Ch.RY, 20, Ch.RZ, 20, Ch.GX, -80, Ch.GY, -20, Ch.GZ, -20, Ch.DROP, 0.5)
                .key(-10, S)
                .key(-5, I, Ch.RX, -30, Ch.RY, 0, Ch.RZ, 12, Ch.GX, -30, Ch.GY, 0, Ch.GZ, -12, Ch.DROP, 1.5, Ch.LEAN, 6)
                .stand(-0.01F, S).as(Pose.PRESS);
        // The Gauntlet to the eye and swept across (the Soul Sight): his eyes flash.
        new Builder().stand(0, S)
                .key(4, O, Ch.GX, -150, Ch.GY, -30, Ch.GZ, 0, Ch.HEAD_X, -6)
                .key(9, E, Ch.GX, -110, Ch.GY, 40, Ch.GZ, -10, Ch.HEAD_X, -6, Ch.HEAD_Y, 10)
                .stand(-0.01F, S).as(Pose.SIGHT);
        // The Infinity Beam (tell 50): the Gauntlet up while the five stones flare one after another, then thrust out.
        new Builder().stand(0, S)
                .key(8, O, Ch.GX, -175, Ch.GZ, -6, Ch.RX, -30, Ch.RZ, 10, Ch.LEAN, -5, Ch.HEAD_X, -10, Ch.TREMBLE, 1.6)
                .key(44, S, Ch.TREMBLE, 2.4)
                .key(50, I, Ch.AIM, 1, Ch.TREMBLE, 1.0, Ch.LEAN, 4, Ch.HEAD_X, 0)
                .key(-6, S)
                .stand(-0.01F, S).as(Pose.INFINITY);
        // Falling through the portal (20, held at the end for as long as he falls): arms and legs flung out.
        new Builder().stand(0, S)
                .key(6, O, Ch.RX, -30, Ch.RZ, 40, Ch.GX, -30, Ch.GZ, -40, Ch.LEG_R, -12, Ch.LEG_L, 12, Ch.LEAN, -4)
                .as(Pose.FALL);
        // A new stone (60): one knee down, the Gauntlet arm shaking, the sword's tip planted.
        new Builder().key(0, O, Ch.DROP, 7, Ch.LEG_R, -85, Ch.LEG_L, 0, Ch.GX, -30, Ch.GZ, -15, Ch.TREMBLE, 3.0, Ch.RX, 10, Ch.RZ, 4, Ch.HEAD_X, 18, Ch.LEAN, 6)
                .key(50, S)
                .stand(60, S).as(Pose.SURGE);
        // A stumble (40): he lurches forward, the Gauntlet arm flung out, the head down.
        new Builder().stand(0, S)
                .key(6, O, Ch.LEAN, 20, Ch.GX, 10, Ch.GZ, -30, Ch.RX, 5, Ch.RZ, 20, Ch.HEAD_X, 25, Ch.LEG_R, -10, Ch.LEG_L, 10, Ch.TREMBLE, 1.0)
                .key(30, S)
                .stand(40, S).as(Pose.STAGGER);
        // The landing (60): arms out, kneeling, then rising.
        new Builder().key(0, S, Ch.DROP, 6, Ch.RX, -20, Ch.RZ, 30, Ch.GX, -20, Ch.GZ, -30, Ch.LEG_R, -50, Ch.LEG_L, -50, Ch.LEAN, 8)
                .key(30, S)
                .key(48, S, Ch.DROP, 1.5, Ch.LEG_R, -12, Ch.LEG_L, -12, Ch.LEAN, 4)
                .stand(60, S).as(Pose.ARRIVE);
        // The death (100): one knee down, then sitting, the Gauntlet arm on his knee, the head bowed.
        new Builder().key(0, O, Ch.DROP, 7, Ch.LEG_R, -85, Ch.LEG_L, 0, Ch.GX, -30, Ch.GZ, -15, Ch.TREMBLE, 2.0, Ch.HEAD_X, 12)
                .key(30, S, Ch.DROP, 11, Ch.LEG_R, -90, Ch.LEG_L, -90, Ch.LEAN, 5, Ch.GX, -60, Ch.GZ, -10, Ch.RX, -20, Ch.RZ, 8, Ch.HEAD_X, 20, Ch.TREMBLE, 0.4)
                .key(100, S).as(Pose.DEATH);
        POSES.put(Pose.STAND, List.of(new Key(0, S, STAND.clone())));
    }

    /** The pose of a move. */
    public static Pose poseFor(ThanosMove move) {
        return switch (move) {
            case SWORD_SWEEP -> Pose.SWEEP;
            case OVERHEAD_SMASH -> Pose.SMASH;
            case GAUNTLET_JAB -> Pose.JAB;
            case SWORD_THROW -> Pose.THROW;
            case CHOKE -> Pose.CHOKE;
            case LEAP -> Pose.LEAP;
            case POWER_BLAST, OVERLOAD, POWER_PULSE, PETRIFY_ORB, SOUL_DRAIN -> Pose.POINT;
            case SHOCKWAVE -> Pose.SLAM;
            case PORTAL_STEP -> Pose.STEP;
            case METEOR_HAIL, REWIND -> Pose.GAUNTLET_UP;
            case GRAVITY_PULL, SPACE_BARRIER -> Pose.PALM_OUT;
            case REALITY_VEIL, PHANTOMS -> Pose.WIDE;
            case SOUL_SIGHT -> Pose.SIGHT;
            case SOULWORLD, TIME_SNARE -> Pose.PRESS;
            case INFINITY_BEAM -> Pose.INFINITY;
        };
    }

    /** How long a pose plays: its own length for a state's, the move's for a move's. */
    public static int lengthOf(Pose pose, ThanosMove move) {
        return pose.length() > 0 ? pose.length() : move == null ? 1 : move.length();
    }

    /** The key ticks of a pose, resolved against its length, in order (for the tests). */
    public static float[] ticks(Pose pose, int length) {
        List<Key> keys = POSES.get(pose);
        float[] ticks = new float[keys.size()];
        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = resolve(keys.get(i), length);
        }
        return ticks;
    }

    private static float resolve(Key key, int length) {
        return key.tick() < 0.0F ? length + key.tick() : key.tick();
    }

    /**
     * The pose at {@code tick} ticks into it, into {@code out}. Before the first key it is the first, after the last
     * the last.
     *
     * @param length how long the pose lasts (the move's {@code length()}, or a state pose's own)
     */
    public static void sample(Pose pose, float tick, int length, Out out) {
        List<Key> keys = POSES.get(pose);
        int n = keys.size();
        float[] result = out.values;
        if (n == 1 || tick <= resolve(keys.get(0), length)) {
            System.arraycopy(keys.get(0).values(), 0, result, 0, result.length);
            return;
        }
        if (tick >= resolve(keys.get(n - 1), length)) {
            System.arraycopy(keys.get(n - 1).values(), 0, result, 0, result.length);
            return;
        }
        int i = 0;
        while (i + 2 < n && tick >= resolve(keys.get(i + 1), length)) {
            i++;
        }
        Key from = keys.get(i);
        Key to = keys.get(i + 1);
        float t0 = resolve(from, length);
        float t1 = resolve(to, length);
        float u = t1 <= t0 ? 1.0F : to.ease().apply(AnimMath.clamp((tick - t0) / (t1 - t0), 0.0F, 1.0F));
        for (int c = 0; c < result.length; c++) {
            result[c] = from.values()[c] + (to.values()[c] - from.values()[c]) * u;
        }
    }

    /** The first fault in the table, or null: a key out of order or past the end, a pose without a first key. */
    public static String problem() {
        for (Pose pose : Pose.values()) {
            List<Key> keys = POSES.get(pose);
            if (keys == null || keys.isEmpty()) {
                return pose + " has no keys";
            }
            for (int length : lengthsOf(pose)) {
                float previous = -1.0F;
                for (Key key : keys) {
                    float tick = resolve(key, length);
                    if (tick < 0.0F || tick > length + 0.01F) {
                        return pose + " has a key at " + tick + " outside its " + length + " ticks";
                    }
                    if (tick <= previous && keys.size() > 1) {
                        return pose + " has keys out of order at " + tick + " (length " + length + ")";
                    }
                    previous = tick;
                }
            }
        }
        return null;
    }

    /** Every length a pose is played for: its own, or those of the moves it stands for. */
    public static List<Integer> lengthsOf(Pose pose) {
        List<Integer> lengths = new ArrayList<>();
        if (pose.length() > 0) {
            lengths.add(pose.length());
            return lengths;
        }
        for (ThanosMove move : ThanosMove.values()) {
            if (poseFor(move) == pose && !lengths.contains(move.length())) {
                lengths.add(move.length());
            }
        }
        if (lengths.isEmpty()) {
            lengths.add(1);
        }
        return lengths;
    }

    private ThanosBossPoses() {
    }
}
