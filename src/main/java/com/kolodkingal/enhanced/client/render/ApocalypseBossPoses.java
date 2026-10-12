package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.client.render.AnimMath.Ease;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;

/**
 * The poses of En Sabah Nur's body (DESIGN section 17, 6.8): one for each state the server syncs, built key by key. A pose
 * is a list of keys; a key is a tick, the easing of the segment that arrives at it and the value of every channel from
 * then on (a channel a key does not mention keeps the value it had). The model reads the pose at the tick of the state and
 * puts the channels on its parts.
 *
 * <p>An attack's state ends the moment the blow lands, so the poses of the attacks go on for a few ticks past the end of
 * the tell ({@link #FOLLOW_TICKS}): the throw, the slam, the swing. The model keeps playing the pose of the state that has
 * just ended for that long, and then eases into the standing pose.
 *
 * <p>Arm values are the model's raw angles in degrees ({@code x} -90 is straight forward, -180 straight up; {@code z}
 * positive is outward on the right arm and negative on the left; {@code y} positive turns a forward arm to the right).
 * No Minecraft classes: the game tests check the table.
 */
public final class ApocalypseBossPoses {
    /** The ticks the pose of an attack goes on after its tell, so that the blow is seen. */
    public static final int FOLLOW_TICKS = 6;

    /** The channels of a pose, in the order of {@link Out#values}. */
    public enum Ch {
        /** The right arm. */
        RX, RY, RZ,
        /** The left arm. */
        LX, LY, LZ,
        /** The body's turn and its lean forward, in degrees. */
        TWIST, LEAN,
        /** How far the whole body sinks (positive) or floats (negative), in model pixels. */
        DROP,
        /** The legs' swing (right, left), in degrees: negative is forward. */
        LEG_R, LEG_L,
        /** The head's pitch (positive looks down), yaw and roll added to where it looks, in degrees. */
        HEAD_X, HEAD_Y, HEAD_Z,
        /** How much the right arm points along the move's aim instead (0 to 1); RX, RY, RZ are then added to it. */
        AIM,
        /** The glow of the cap's cracks (0 to 1). */
        GLOW
    }

    /** Which pose a state is, and how long its tell lasts (0 for a pose that is held for as long as its state lasts). */
    public enum Pose {
        STAND(0),
        /** Stepping out of the portal: the head bowed, then raised. */
        AWAKEN(60),
        LANCE(14), WAVE(22), SPIKES(24), BOULDERS(30), ENTOMB(30), MIND_CRUSH(40), PORTAL(16), BACKHAND(8), TORNADO(40),
        /** Kneeling after the shield broke. */
        STAGGER(0),
        /** Floating, the arms wide and up. */
        HOVER(0),
        /** The right hand raised to the sky. */
        ESCAPE(0),
        /** Kneeling, the head back: "All is revealed." */
        KNEEL(0),
        /** The arms crossed over the chest, flung out as the plates fly. */
        ARMOUR_FALL(60),
        /** Slumping forward. */
        DEATH(40),
        /** Stepping back into the portal. */
        WITHDRAW(80);

        private final int length;

        Pose(int length) {
            this.length = length;
        }

        /** The ticks of the tell (the key after which the blow lands), or 0 for a pose that is held. */
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
    /** The legs folded under him when he kneels, and how far the hips sink for the feet to reach the ground (px). */
    private static final float KNEEL_LEG = 70.0F;
    private static final float KNEEL_DROP = 7.9F;

    static {
        STAND[Ch.RX.ordinal()] = -6.0F;
        STAND[Ch.RZ.ordinal()] = 12.0F;
        STAND[Ch.LX.ordinal()] = -6.0F;
        STAND[Ch.LZ.ordinal()] = -12.0F;
    }

    private static final Map<Pose, List<Key>> POSES = new EnumMap<>(Pose.class);
    /** The poses whose keys go on past the tell: a throw, a slam, a swing, a hand that closes. */
    private static final Set<Pose> FOLLOWING = EnumSet.of(Pose.LANCE, Pose.WAVE, Pose.SPIKES, Pose.BOULDERS, Pose.ENTOMB, Pose.BACKHAND);

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
        // He stands with his arms a little out and the palms open (the walk and the breathing are the model's).
        new Builder().stand(0, S).as(Pose.STAND);

        // Stepping out of the portal: the head bowed and the eyes dark, then the head lifts.
        new Builder().key(0, S, Ch.HEAD_X, 18, Ch.LEAN, 3, Ch.RZ, 8, Ch.LZ, -8)
                .key(20, S)
                .key(44, E, Ch.HEAD_X, 0, Ch.LEAN, 0, Ch.RZ, 14, Ch.LZ, -14, Ch.RX, -10, Ch.LX, -10)
                .key(60, S, Ch.RX, -6, Ch.LX, -6, Ch.RZ, 12, Ch.LZ, -12).as(Pose.AWAKEN);

        // The Lances: the right hand raised, the fingers pinched; the left arm back; the throw (past the tell) snaps the hand
        // forward and it settles.
        new Builder().stand(0, S)
                .key(6, O, Ch.RX, -82, Ch.RZ, 4, Ch.LX, 14, Ch.TWIST, 8, Ch.LEAN, 2)
                .key(14, S)
                .key(16, O, Ch.RX, -94, Ch.LEAN, 5)
                .key(20, S, Ch.RX, -88, Ch.LEAN, 3).as(Pose.LANCE);

        // The Wave: both arms raised, then swept down and forward, the body leaning into it.
        new Builder().stand(0, S)
                .key(7, O, Ch.RX, -140, Ch.LX, -140, Ch.RZ, 8, Ch.LZ, -8, Ch.LEAN, -3)
                .key(11, S)
                .key(22, I, Ch.RX, -30, Ch.LX, -30, Ch.RZ, 6, Ch.LZ, -6, Ch.LEAN, 10)
                .key(28, S).as(Pose.WAVE);

        // The Spikes: a fist raised, held, and slammed down; the body leans into the slam.
        new Builder().stand(0, S)
                .key(8, O, Ch.RX, -150, Ch.RZ, 6, Ch.LX, -20, Ch.LEAN, -4)
                .key(17, S)
                .key(20, I, Ch.RX, -20, Ch.LEAN, 12, Ch.DROP, 1.2F)
                .key(30, S).as(Pose.SPIKES);

        // The Boulders: both arms raised overhead and a little apart (above the shoulder a negative z on the right arm spreads it),
        // palms up, the body arched back, floating a little.
        new Builder().stand(0, S)
                .key(10, O, Ch.RX, -160, Ch.RZ, -24, Ch.LX, -160, Ch.LZ, 24, Ch.LEAN, -8, Ch.DROP, -2.4F, Ch.HEAD_X, -8)
                .key(30, S)
                .key(33, I, Ch.RX, -70, Ch.LX, -70, Ch.RZ, 10, Ch.LZ, -10, Ch.LEAN, 8, Ch.DROP, 0)
                .key(38, S).as(Pose.BOULDERS);

        // The grasp: the right arm reaches along the aim and the hand closes (the arm pulls in at the end).
        new Builder().stand(0, S)
                .key(8, O, Ch.AIM, 1, Ch.RX, 0, Ch.RZ, 0, Ch.LX, 8, Ch.LZ, -10, Ch.LEAN, 3)
                .key(26, S)
                .key(29, I, Ch.RX, 10)
                .key(36, S, Ch.RX, 0).as(Pose.ENTOMB);

        // The Mind Crush: two fingers to the temple, the head tilted.
        new Builder().stand(0, S)
                .key(10, O, Ch.RX, -170, Ch.RZ, 8, Ch.LX, -4, Ch.HEAD_Z, 12, Ch.HEAD_X, 4)
                .key(40, S).as(Pose.MIND_CRUSH);

        // The Portal Step: the arms spread, the body dips.
        new Builder().stand(0, S)
                .key(5, O, Ch.RZ, 80, Ch.LZ, -80, Ch.RX, -15, Ch.LX, -15, Ch.DROP, 1.2F, Ch.HEAD_X, -6)
                .key(16, S).as(Pose.PORTAL);

        // The Backhand: the arm drawn across the chest (the tell), then swung out (the blow).
        new Builder().stand(0, S)
                .key(3, O, Ch.RX, -80, Ch.RY, -70, Ch.RZ, 0, Ch.TWIST, -12)
                .key(8, S)
                .key(11, I, Ch.RY, 70, Ch.TWIST, 25)
                .key(16, S, Ch.RY, 60).as(Pose.BACKHAND);

        // The Tornado: both arms raised overhead and apart, the head back.
        new Builder().stand(0, S)
                .key(12, O, Ch.RX, -150, Ch.LX, -150, Ch.RZ, -25, Ch.LZ, 25, Ch.LEAN, -6, Ch.HEAD_X, -10)
                .key(40, S).as(Pose.TORNADO);

        // Kneeling, staggered: the legs folded, the body forward, the head down, the arms hanging.
        new Builder().stand(0, S)
                .key(10, O, Ch.LEG_R, -KNEEL_LEG, Ch.LEG_L, KNEEL_LEG, Ch.DROP, KNEEL_DROP, Ch.LEAN, 25, Ch.HEAD_X, 30, Ch.RX, 8, Ch.LX, 8,
                        Ch.RZ, 6, Ch.LZ, -6).as(Pose.STAGGER);

        // Floating (the body itself is raised by the server): the arms wide and up in a V, the head back, the legs hanging. (The
        // design's x -120, z 70 points the arms forward and then tips them inward: they cross in front of the chest.)
        new Builder().stand(0, S)
                .key(14, O, Ch.DROP, -1.5F, Ch.RX, -14, Ch.RZ, 132, Ch.LX, -14, Ch.LZ, -132, Ch.HEAD_X, -12, Ch.LEG_R, 8, Ch.LEG_L, -4)
                .as(Pose.HOVER);

        // The Escape: the right hand raised to the sky.
        new Builder().stand(0, S)
                .key(12, O, Ch.RX, -170, Ch.RZ, 10, Ch.LX, 8, Ch.HEAD_X, -25, Ch.LEAN, -3).as(Pose.ESCAPE);

        // The end: kneeling, the head back, the arms open, the cracks of the cap blazing.
        new Builder().stand(0, S)
                .key(16, E, Ch.LEG_R, -KNEEL_LEG, Ch.LEG_L, KNEEL_LEG, Ch.DROP, KNEEL_DROP, Ch.HEAD_X, -35, Ch.RZ, 40, Ch.LZ, -40, Ch.RX, -8, Ch.LX, -8,
                        Ch.LEAN, -5, Ch.GLOW, 1).as(Pose.KNEEL);

        // The armour falls: the arms crossed over the chest, the head back (the shock goes out as it begins); at tick 8 the plates
        // fly and the arms are flung out.
        new Builder().stand(0, S)
                .key(5, O, Ch.RX, -60, Ch.RY, -45, Ch.RZ, 0, Ch.LX, -60, Ch.LY, 45, Ch.LZ, 0, Ch.HEAD_X, -10, Ch.GLOW, 0.5F)
                .key(8, S)
                .key(14, O, Ch.RX, -20, Ch.RY, 0, Ch.RZ, 80, Ch.LX, -20, Ch.LY, 0, Ch.LZ, -80, Ch.HEAD_X, -30, Ch.LEAN, -6, Ch.GLOW, 1)
                .key(60, S, Ch.RX, -10, Ch.RZ, 40, Ch.LX, -10, Ch.LZ, -40, Ch.HEAD_X, -8, Ch.LEAN, 0, Ch.GLOW, 0.3F).as(Pose.ARMOUR_FALL);

        // The death: he slumps forward onto his knees.
        new Builder().stand(0, S)
                .key(14, O, Ch.LEG_R, -KNEEL_LEG, Ch.LEG_L, KNEEL_LEG, Ch.DROP, KNEEL_DROP, Ch.RZ, 30, Ch.LZ, -30, Ch.RX, -4, Ch.LX, -4, Ch.GLOW, 1)
                .key(40, E, Ch.LEAN, 48, Ch.HEAD_X, 38, Ch.RZ, 10, Ch.LZ, -10, Ch.RX, 14, Ch.LX, 14, Ch.GLOW, 0.4F).as(Pose.DEATH);

        // The withdrawal: the arms spread, and he sinks through the portal at his feet.
        new Builder().stand(0, S)
                .key(10, O, Ch.RZ, 80, Ch.LZ, -80, Ch.RX, -15, Ch.LX, -15, Ch.HEAD_X, -8)
                .key(20, S)
                .key(80, E, Ch.DROP, 44).as(Pose.WITHDRAW);
    }

    /** The pose that stands for a state of his controller. */
    public static Pose poseFor(ApocalypseState state) {
        return switch (state) {
            case IDLE -> Pose.STAND;
            case AWAKENING -> Pose.AWAKEN;
            case TELL_LANCE -> Pose.LANCE;
            case TELL_WAVE -> Pose.WAVE;
            case TELL_SPIKES -> Pose.SPIKES;
            case TELL_BOULDERS -> Pose.BOULDERS;
            case TELL_ENTOMB -> Pose.ENTOMB;
            case TELL_MIND_CRUSH -> Pose.MIND_CRUSH;
            case TELL_PORTAL -> Pose.PORTAL;
            case BACKHAND -> Pose.BACKHAND;
            case TELL_TORNADO -> Pose.TORNADO;
            case STAGGERED -> Pose.STAGGER;
            case HOVER -> Pose.HOVER;
            case ESCAPING -> Pose.ESCAPE;
            case KNEELING -> Pose.KNEEL;
            case ARMOUR_FALL -> Pose.ARMOUR_FALL;
            case DYING -> Pose.DEATH;
            case WITHDRAWING -> Pose.WITHDRAW;
        };
    }

    /** Whether the pose of this state goes on for {@link #FOLLOW_TICKS} after its state ends (the blow is seen). */
    public static boolean follows(Pose pose) {
        return FOLLOWING.contains(pose);
    }

    /** The ticks of the last key of a pose. */
    public static float end(Pose pose) {
        List<Key> keys = POSES.get(pose);
        return keys.get(keys.size() - 1).tick();
    }

    /** Samples a pose at a tick (held at its last key beyond the end) into {@code out}. */
    public static void sample(Pose pose, float tick, Out out) {
        List<Key> keys = POSES.get(pose);
        int n = keys.size();
        if (tick <= keys.get(0).tick() || n == 1) {
            System.arraycopy(keys.get(0).values(), 0, out.values, 0, out.values.length);
            return;
        }
        if (tick >= keys.get(n - 1).tick()) {
            System.arraycopy(keys.get(n - 1).values(), 0, out.values, 0, out.values.length);
            return;
        }
        int i = 0;
        while (i + 2 < n && tick >= keys.get(i + 1).tick()) {
            i++;
        }
        Key a = keys.get(i);
        Key c = keys.get(i + 1);
        float u = c.tick() > a.tick() ? c.ease().apply((tick - a.tick()) / (c.tick() - a.tick())) : 1.0F;
        for (int k = 0; k < out.values.length; k++) {
            out.values[k] = a.values()[k] + (c.values()[k] - a.values()[k]) * u;
        }
    }

    private ApocalypseBossPoses() {
    }
}
