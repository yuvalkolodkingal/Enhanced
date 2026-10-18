package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.render.AnimMath.Ease;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;

/**
 * The keys of the gestures ({@link GestureId} has their timing): a gesture is a set of tracks (the two arms, the legs, the
 * head, the body's twist, lean, roll, drop and spin), each a list of keys in ticks with the easing code of the segment that
 * arrives at the key. Arm values are raw model angles in degrees for a right-handed player; the pose pipeline mirrors them
 * for a left-handed one.
 *
 * <p>An arm key is one of
 * <ul>
 *     <li>{@code ABS}: absolute angles; the gesture's weight lerps the arm from what is below to them;</li>
 *     <li>{@code ADD}: added to what is below, times the weight;</li>
 *     <li>{@code WORLD}: the arm given by its world elevation {@code phi} (0 hangs, 90 level forward, 180 straight up), so it
 *     points at the same place whatever the body's lean; {@code plant} adds the weapon's planting angle;</li>
 *     <li>{@code AIM}: toward a {@link Target}, plus an offset; computed every frame.</li>
 * </ul>
 * A channel no key sets is not driven (the pose below shows through); before its first key a channel holds its first
 * value, after its last key the last. No Minecraft imports: the tables and the sampler can be checked by the game tests.
 */
public final class Gestures {
    /** What an arm key means. */
    public enum Mode {
        ABS, ADD, WORLD, AIM
    }

    /** What an {@code AIM} key points at. */
    public enum Target {
        /** The way the hero looks. */
        LOOK,
        /** The entity held with telekinesis. */
        HELD,
        /** One of the projectiles hanging in Magneto's field. */
        PROJECTILE,
        /** The end of the last Hex Bolt. */
        BOLT,
        /** The thrown weapon. */
        WEAPON,
        /** Where the weapon was just before it came back to his hand. */
        WEAPON_LAST,
        /** Where Thor last brought his weapon down (the creature he pinned). */
        STRIKE,
        /** The end of Spider-Man's web: the anchor of the zip or the swing he is on. */
        ANCHOR
    }

    /** One key of an arm track. */
    public static final class ArmKey {
        public final float t;
        public final Ease ease;
        public final Mode mode;
        /** Degrees: raw angles ({@code ABS}, {@code ADD}), the world elevation ({@code WORLD}) or an offset ({@code AIM}). */
        public final float x;
        public final float y;
        public final float z;
        public final Target target;
        /** {@code WORLD}: the planting angle of the weapon is added to {@code x}. */
        public final boolean plant;
        /** The tremble: amplitude in degrees and speed in radians per tick. */
        public final float tremble;
        public final float trembleSpeed;

        ArmKey(float t, Ease ease, Mode mode, float x, float y, float z, Target target, boolean plant, float tremble, float trembleSpeed) {
            this.t = t;
            this.ease = ease;
            this.mode = mode;
            this.x = x;
            this.y = y;
            this.z = z;
            this.target = target;
            this.plant = plant;
            this.tremble = tremble;
            this.trembleSpeed = trembleSpeed;
        }
    }

    /** Turns an arm key into absolute (or, for {@code ADD}, additive) model angles in radians; the pose pipeline provides it. */
    public interface ArmSolver {
        void resolve(ArmKey key, boolean main, float[] out);
    }

    /** A track of numbers: {@code width} values per key. */
    public static final class Track {
        final int width;
        final List<float[]> keys = new ArrayList<>();
        final List<Ease> eases = new ArrayList<>();
        final List<Float> times = new ArrayList<>();
        /** A scratch cell for {@link #scalar}, so reading a one-wide track every frame allocates nothing. */
        private final float[] one = new float[1];

        Track(int width) {
            this.width = width;
        }

        void add(float t, Ease ease, float... values) {
            times.add(t);
            eases.add(ease);
            keys.add(values);
        }

        public boolean isEmpty() {
            return keys.isEmpty();
        }

        public int size() {
            return keys.size();
        }

        public float time(int i) {
            return times.get(i);
        }

        public float[] value(int i) {
            return keys.get(i);
        }

        /** The track at time {@code t}, into {@code out}; false when the track has no keys. */
        public boolean sample(float t, float[] out) {
            int n = keys.size();
            if (n == 0) {
                return false;
            }
            if (t <= times.get(0) || n == 1) {
                System.arraycopy(keys.get(0), 0, out, 0, width);
                return true;
            }
            if (t >= times.get(n - 1)) {
                System.arraycopy(keys.get(n - 1), 0, out, 0, width);
                return true;
            }
            int i = 0;
            while (i + 2 < n && t >= times.get(i + 1)) {
                i++;
            }
            float t0 = times.get(i);
            float t1 = times.get(i + 1);
            float u = t1 > t0 ? eases.get(i + 1).apply((t - t0) / (t1 - t0)) : 1.0F;
            float[] a = keys.get(i);
            float[] b = keys.get(i + 1);
            for (int k = 0; k < width; k++) {
                out[k] = a[k] + (b[k] - a[k]) * u;
            }
            return true;
        }

        /** The value of a one-wide track at {@code t}, or {@code unset} when it has no keys. */
        public float scalar(float t, float unset) {
            if (keys.isEmpty()) {
                return unset;
            }
            sample(t, one);
            return one[0];
        }
    }

    /** The definition of one gesture. */
    public static final class Def {
        public final GestureId id;
        public final List<ArmKey> m = new ArrayList<>();
        public final List<ArmKey> o = new ArrayList<>();
        /** Leg x angles (degrees). */
        public final Track right = new Track(1);
        public final Track left = new Track(1);
        public final Track head = new Track(3);
        public final Track twist = new Track(1);
        public final Track lean = new Track(1);
        public final Track roll = new Track(1);
        public final Track drop = new Track(1);
        public final Track yaw = new Track(1);
        /** 1 while the gesture asks the weapon to whirl or to lead. */
        public final Track spin = new Track(1);
        public final Track lead = new Track(1);
        /** A scratch cell for {@link #sampleArm} (the render thread samples one arm at a time). */
        private final float[] next = new float[3];

        Def(GestureId id) {
            this.id = id;
        }

        /** Whether the gesture turns the whole body (it is never cut short). */
        public boolean hasYaw() {
            return !yaw.isEmpty();
        }

        /** Checks the table: keys in order, none past the end, a yaw that starts at 0 and ends facing the same way. */
        public List<String> problems() {
            List<String> out = new ArrayList<>();
            for (List<ArmKey> keys : List.of(m, o)) {
                float last = -1.0F;
                for (ArmKey k : keys) {
                    if (k.t < last) {
                        out.add(id + ": arm keys out of order at t" + k.t);
                    }
                    if (k.t > id.dur) {
                        out.add(id + ": an arm key at t" + k.t + " is past the end " + id.dur);
                    }
                    last = k.t;
                }
            }
            for (Track track : List.of(right, left, head, twist, lean, roll, drop, yaw, spin, lead)) {
                float last = -1.0F;
                for (float t : track.times) {
                    if (t < last) {
                        out.add(id + ": keys out of order at t" + t);
                    }
                    if (t > id.dur) {
                        out.add(id + ": a key at t" + t + " is past the end " + id.dur);
                    }
                    last = t;
                }
            }
            if (!yaw.isEmpty()) {
                if (yaw.value(0)[0] != 0.0F) {
                    out.add(id + ": the yaw starts at " + yaw.value(0)[0] + " instead of 0");
                }
                float end = yaw.value(yaw.size() - 1)[0];
                if (Math.abs(end / 360.0F - Math.round(end / 360.0F)) > 1.0E-4F) {
                    out.add(id + ": the yaw ends at " + end + ", not a whole number of turns");
                }
            }
            return out;
        }

        /**
         * The arm track at {@code t}: {@code out} gets the absolute angles in radians (or, for an additive track, the angles to
         * add), {@code tremble} the amplitude (degrees) and the speed. Returns the mode of the keys, or null when the arm is not
         * driven. A {@code mechanical} hero (White Vision) moves between keys with one easing only, the ease-in-out sine.
         */
        public Mode sampleArm(boolean main, float t, ArmSolver solver, float[] out, float[] tremble, boolean mechanical) {
            List<ArmKey> keys = main ? m : o;
            int n = keys.size();
            if (n == 0) {
                return null;
            }
            int i = 0;
            if (t >= keys.get(n - 1).t) {
                i = n - 1;
            } else if (t > keys.get(0).t) {
                while (i + 2 < n && t >= keys.get(i + 1).t) {
                    i++;
                }
            }
            ArmKey a = keys.get(i);
            solver.resolve(a, main, out);
            tremble[0] = a.tremble;
            tremble[1] = a.trembleSpeed;
            if (i + 1 < n && t > a.t) {
                ArmKey b = keys.get(i + 1);
                float u = b.t > a.t ? (mechanical ? Ease.E : b.ease).apply((t - a.t) / (b.t - a.t)) : 1.0F;
                solver.resolve(b, main, next);
                for (int k = 0; k < 3; k++) {
                    out[k] = out[k] + (next[k] - out[k]) * u;
                }
                tremble[0] = a.tremble + (b.tremble - a.tremble) * u;
                tremble[1] = b.trembleSpeed;
            }
            return a.mode == Mode.ADD ? Mode.ADD : Mode.ABS;
        }
    }

    /** Fills a {@link Def} key by key: {@code key(t, ease)} starts a key, the calls after it set that key's channels. */
    public static final class Builder {
        private final Def def;
        private float t;
        private Ease ease = S;

        Builder(Def def) {
            this.def = def;
        }

        public Builder key(float t, Ease ease) {
            this.t = t;
            this.ease = ease;
            return this;
        }

        private ArmKey arm(Mode mode, float x, float y, float z, Target target, boolean plant, float tremble, float speed) {
            return new ArmKey(t, ease, mode, x, y, z, target, plant, tremble, speed);
        }

        public Builder m(float x, float y, float z) {
            def.m.add(arm(Mode.ABS, x, y, z, null, false, 0.0F, 0.0F));
            return this;
        }

        public Builder o(float x, float y, float z) {
            def.o.add(arm(Mode.ABS, x, y, z, null, false, 0.0F, 0.0F));
            return this;
        }

        /** The main arm at a pose of {@link StancePoses} (so a gesture that hands over to a stance ends exactly on it). */
        public Builder m(float[] pose) {
            return m(pose[0], pose[1], pose[2]);
        }

        public Builder o(float[] pose) {
            return o(pose[0], pose[1], pose[2]);
        }

        public Builder mAdd(float x, float y, float z) {
            def.m.add(arm(Mode.ADD, x, y, z, null, false, 0.0F, 0.0F));
            return this;
        }

        public Builder oAdd(float x, float y, float z) {
            def.o.add(arm(Mode.ADD, x, y, z, null, false, 0.0F, 0.0F));
            return this;
        }

        /** The main arm by its world elevation. */
        public Builder mWorld(float phi, float y, float z) {
            def.m.add(arm(Mode.WORLD, phi, y, z, null, false, 0.0F, 0.0F));
            return this;
        }

        /** The main arm by the world elevation of the weapon's planting angle plus {@code phiOffset}. */
        public Builder mPlant(float phiOffset, float y, float z) {
            def.m.add(arm(Mode.WORLD, phiOffset, y, z, null, true, 0.0F, 0.0F));
            return this;
        }

        public Builder mAim(Target target, float x, float y, float z) {
            def.m.add(arm(Mode.AIM, x, y, z, target, false, 0.0F, 0.0F));
            return this;
        }

        public Builder oAim(Target target, float x, float y, float z) {
            def.o.add(arm(Mode.AIM, x, y, z, target, false, 0.0F, 0.0F));
            return this;
        }

        /** Makes the main arm key just written tremble: amplitude in degrees, speed in radians per tick. */
        public Builder mTremble(float amplitude, float speed) {
            retremble(def.m, amplitude, speed);
            return this;
        }

        /** Makes the off arm key just written tremble. */
        public Builder oTremble(float amplitude, float speed) {
            retremble(def.o, amplitude, speed);
            return this;
        }

        private static void retremble(List<ArmKey> keys, float amplitude, float speed) {
            ArmKey k = keys.remove(keys.size() - 1);
            keys.add(new ArmKey(k.t, k.ease, k.mode, k.x, k.y, k.z, k.target, k.plant, amplitude, speed));
        }

        public Builder legs(float right, float left) {
            def.right.add(t, ease, right);
            def.left.add(t, ease, left);
            return this;
        }

        public Builder head(float x, float y, float z) {
            def.head.add(t, ease, x, y, z);
            return this;
        }

        public Builder twist(float v) {
            def.twist.add(t, ease, v);
            return this;
        }

        public Builder lean(float v) {
            def.lean.add(t, ease, v);
            return this;
        }

        public Builder roll(float v) {
            def.roll.add(t, ease, v);
            return this;
        }

        public Builder drop(float v) {
            def.drop.add(t, ease, v);
            return this;
        }

        public Builder yaw(float v) {
            def.yaw.add(t, ease, v);
            return this;
        }

        /** The weapon whirls from here on. */
        public Builder spin() {
            def.spin.add(t, ease, 1.0F);
            return this;
        }

        /** The weapon is forced into the lead grip from here on. */
        public Builder lead() {
            def.lead.add(t, ease, 1.0F);
            return this;
        }
    }

    private static final Map<GestureId, Def> DEFS = new EnumMap<>(GestureId.class);
    /** 3.4: the hero plug-in that defined each gesture (its flourishes and first-person rows go with it). */
    private static final Map<GestureId, HeroClient> OWNERS = new EnumMap<>(GestureId.class);
    @Nullable
    private static HeroClient defining;

    /** Starts the definition of a gesture. */
    public static Builder define(GestureId id) {
        Def def = new Def(id);
        DEFS.put(id, def);
        if (defining != null) {
            OWNERS.put(id, defining);
        }
        return new Builder(def);
    }

    /** 3.4: the plug-in whose {@code registerGestures} is running now (null: the shared tables). */
    static void defining(@Nullable HeroClient hero) {
        defining = hero;
    }

    /**
     * 3.4: the hero plug-in that defined this gesture, or null for the gestures of the shared tables (the heroes before
     * 3.0 and the flight). A custom hero who plays a borrowed gesture lets its owner add its flourishes.
     */
    @Nullable
    public static HeroClient owner(GestureId id) {
        return OWNERS.get(id);
    }

    /** The keys of a gesture, or null when it has none (it then does nothing). */
    public static Def get(GestureId id) {
        return DEFS.get(id);
    }

    public static Map<GestureId, Def> all() {
        return DEFS;
    }

    static {
        FlightGestures.register();
        FieldGestures.register();
        WitchGestures.register();
        SpeedGestures.register();
        AsgardGestures.register();
        HeroClients.registerGestures();
    }

    private Gestures() {
    }
}
