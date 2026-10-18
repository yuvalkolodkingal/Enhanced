package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.hela.HelaState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The poses of the boss Hela's body (3.5): one per {@link HelaState}, keyed on the ticks of the state, so that every
 * gesture of hers meets the server's moment (the blades fly at the volley's tick 12, the blows of the combo fall at 8,
 * 16 and 24). A pose is a list of keys; a key is a tick and the value of the channels it names from then on (a channel a
 * key does not name keeps the value it had); between two keys the values move with a smoothstep. No Minecraft classes:
 * the game tests check the table.
 *
 * <p>Arm values are the model's raw angles in degrees ({@code xRot} -90 is straight forward, -180 straight up; {@code zRot}
 * positive is outward on the right arm and negative on the left). {@code DROP} sinks the body in model pixels (a kneel),
 * {@code ALPHA} is how much of her is there (she forms out of smoke, steps through it, dissolves into it), {@code SPIN}
 * turns her about herself in degrees per tick (the Blade Storm).
 */
public final class HelaBossPoses {
    /** The channels of a pose. */
    public enum Ch {
        RX, RY, RZ, LX, LY, LZ, TWIST, LEAN, DROP, LEG_R, LEG_L, HEAD_X, HEAD_Y, ALPHA, SPIN
    }

    public static final int CHANNELS = Ch.values().length;

    /** One key: its tick and the values it sets (NaN: keep). */
    private record Key(float tick, float[] values) {
    }

    private static final Map<HelaState, List<Key>> POSES = new EnumMap<>(HelaState.class);
    /** Her stance between attacks: the blades lowered at her sides, the points out. */
    static final float[] STAND = values(-12, 4, 14, -12, -4, -14, 0, 0, 0, 0, 0, 0, 0, 1, 0);

    static {
        pose(HelaState.IDLE).key(0, STAND);
        // Out of the smoke of the seal: crouched and unseen, then rising with her arms flung wide, the head thrown back.
        pose(HelaState.ARRIVING).key(0, STAND).set(0, Ch.DROP, 9, Ch.RX, 25, Ch.RZ, 30, Ch.LX, 25, Ch.LZ, -30, Ch.HEAD_X, 35, Ch.ALPHA, 0,
                        Ch.LEG_R, -40, Ch.LEG_L, 10)
                .set(50, Ch.ALPHA, 0.35F)
                .set(90, Ch.ALPHA, 0.75F, Ch.DROP, 4, Ch.RX, -10, Ch.RZ, 25, Ch.LX, -10, Ch.LZ, -25, Ch.HEAD_X, 15, Ch.LEG_R, -15, Ch.LEG_L, 4)
                .set(104, Ch.DROP, 0, Ch.RX, -45, Ch.RZ, 70, Ch.LX, -45, Ch.LZ, -70, Ch.HEAD_X, -18, Ch.ALPHA, 1, Ch.LEG_R, 0, Ch.LEG_L, 0)
                .set(116, Ch.RX, -45, Ch.RZ, 70, Ch.LX, -45, Ch.LZ, -70, Ch.HEAD_X, -10)
                .key(128, STAND);
        // The volley: both hands raised behind her shoulders as the blades appear, then flung forward at tick 12.
        pose(HelaState.VOLLEY).key(0, STAND)
                .set(4, Ch.RX, -160, Ch.RZ, 38, Ch.LX, -160, Ch.LZ, -38, Ch.LEAN, -6, Ch.HEAD_X, -6)
                .set(10, Ch.RX, -165, Ch.RZ, 42, Ch.LX, -165, Ch.LZ, -42)
                .set(13, Ch.RX, -88, Ch.RZ, 18, Ch.LX, -88, Ch.LZ, -18, Ch.LEAN, 9, Ch.HEAD_X, 4)
                .key(20, STAND);
        // The wall: the right hand drawn low and back, then swept forward along the floor, the body bent into it.
        pose(HelaState.WALL).key(0, STAND)
                .set(3, Ch.RX, 30, Ch.RZ, 35, Ch.LEAN, 10, Ch.DROP, 2, Ch.TWIST, -15)
                .set(6, Ch.RX, -55, Ch.RY, -35, Ch.RZ, 6, Ch.LEAN, 16, Ch.DROP, 3, Ch.TWIST, 18, Ch.LX, 15, Ch.LZ, -25)
                .set(16, Ch.RX, -55, Ch.RY, -35)
                .key(24, STAND);
        // Into the smoke: the arms crossed over the chest as she fades.
        pose(HelaState.STEP_OUT).key(0, STAND).set(4, Ch.RX, -70, Ch.RY, 45, Ch.RZ, 0, Ch.LX, -70, Ch.LY, -45, Ch.LZ, 0, Ch.ALPHA, 0.8F)
                .set(9, Ch.ALPHA, 0);
        // Out of it: flung open, blades ready.
        pose(HelaState.STEP_IN).key(0, STAND).set(0, Ch.RX, -40, Ch.RZ, 65, Ch.LX, -40, Ch.LZ, -65, Ch.ALPHA, 0)
                .set(5, Ch.ALPHA, 1).set(8, Ch.RX, -140, Ch.RZ, 30, Ch.LX, -30, Ch.LZ, -30);
        // The combo: right high and down, left high and down, both together.
        pose(HelaState.MELEE).key(0, STAND)
                .set(5, Ch.RX, -150, Ch.RZ, 30, Ch.TWIST, -20)
                .set(8, Ch.RX, -35, Ch.RY, -40, Ch.RZ, 0, Ch.TWIST, 25, Ch.LEAN, 8)
                .set(12, Ch.LX, -150, Ch.LZ, -30, Ch.TWIST, 20, Ch.RX, -20, Ch.RY, 0, Ch.RZ, 20)
                .set(16, Ch.LX, -35, Ch.LY, 40, Ch.LZ, 0, Ch.TWIST, -25, Ch.LEAN, 8)
                .set(20, Ch.RX, -170, Ch.RZ, 14, Ch.LX, -170, Ch.LY, 0, Ch.LZ, -14, Ch.TWIST, 0, Ch.LEAN, -4)
                .set(24, Ch.RX, -48, Ch.RZ, 10, Ch.LX, -48, Ch.LZ, -10, Ch.LEAN, 15, Ch.DROP, 2)
                .key(30, STAND);
        // The rain: an arm raised high, palm down, held while the blades hang.
        pose(HelaState.RAIN).key(0, STAND)
                .set(5, Ch.RX, -172, Ch.RZ, 10, Ch.HEAD_X, -20, Ch.LX, -20, Ch.LZ, -30)
                .set(20, Ch.RX, -172, Ch.RZ, 10)
                .set(23, Ch.RX, -120, Ch.HEAD_X, 0)
                .key(26, STAND);
        // The storm: arms out straight to the sides, blades out, and she whirls.
        pose(HelaState.STORM).key(0, STAND)
                .set(4, Ch.RX, -90, Ch.RZ, 88, Ch.LX, -90, Ch.LZ, -88, Ch.SPIN, 30)
                .set(34, Ch.SPIN, 30)
                .set(40, Ch.RX, -12, Ch.RZ, 14, Ch.LX, -12, Ch.LZ, -14, Ch.SPIN, 0);
        // The Army of the Dead: both arms rise slowly, palms up, the head lifted.
        pose(HelaState.RISE).key(0, STAND)
                .set(10, Ch.RX, -20, Ch.RZ, 40, Ch.LX, -20, Ch.LZ, -40)
                .set(45, Ch.RX, -150, Ch.RZ, 38, Ch.LX, -150, Ch.LZ, -38, Ch.HEAD_X, -22)
                .set(55, Ch.RX, -150, Ch.RZ, 38, Ch.LX, -150, Ch.LZ, -38)
                .key(60, STAND);
        // The Strength of Asgard: she kneels, hands spread to the floor, the head bowed.
        pose(HelaState.CHANNEL).key(0, STAND)
                .set(8, Ch.DROP, 8, Ch.LEG_R, -80, Ch.LEG_L, 12, Ch.RX, -28, Ch.RZ, 42, Ch.LX, -28, Ch.LZ, -42, Ch.HEAD_X, 28, Ch.LEAN, 8);
        // Broken out of it: she reels back, the head thrown back, arms loose, then gathers herself.
        pose(HelaState.STUNNED).key(0, STAND).set(0, Ch.DROP, 8, Ch.LEG_R, -80, Ch.LEG_L, 12)
                .set(5, Ch.DROP, 3, Ch.LEG_R, -20, Ch.LEG_L, 5, Ch.HEAD_X, -28, Ch.LEAN, -12, Ch.RX, 12, Ch.RZ, 30, Ch.LX, 12, Ch.LZ, -30)
                .set(45, Ch.DROP, 2, Ch.HEAD_X, -10, Ch.LEAN, -6)
                .key(60, STAND);
        // The catch: the right hand out at the hammer, closed on it, crushing.
        pose(HelaState.CATCH).key(0, STAND)
                .set(3, Ch.RX, -105, Ch.RY, -10, Ch.RZ, 0, Ch.LEAN, -4)
                .set(10, Ch.RX, -98, Ch.LEAN, -2)
                .set(18, Ch.RX, -98)
                .key(24, STAND);
        // Her fall: a stagger, down on one knee, and she dissolves.
        pose(HelaState.DYING).key(0, STAND)
                .set(6, Ch.LEAN, -12, Ch.HEAD_X, -24, Ch.RX, 10, Ch.RZ, 35, Ch.LX, 10, Ch.LZ, -35)
                .set(16, Ch.DROP, 8, Ch.LEG_R, -80, Ch.LEG_L, 12, Ch.LEAN, 10, Ch.HEAD_X, 30, Ch.RX, -20, Ch.LX, 0)
                .set(22, Ch.ALPHA, 1)
                .set(40, Ch.ALPHA, 0);
        // She leaves: the arms folded, she sinks into the floor and is smoke.
        pose(HelaState.LEAVING).key(0, STAND)
                .set(6, Ch.RX, -70, Ch.RY, 45, Ch.RZ, 0, Ch.LX, -70, Ch.LY, -45, Ch.LZ, 0, Ch.HEAD_X, -8)
                .set(10, Ch.DROP, 0, Ch.ALPHA, 1)
                .set(40, Ch.DROP, 30, Ch.ALPHA, 0);
    }

    /** The values of a pose's channels at {@code time} ticks into {@code state}. */
    public static float[] at(HelaState state, float time) {
        List<Key> keys = POSES.get(state);
        float[] out = STAND.clone();
        if (keys == null || keys.isEmpty()) {
            return out;
        }
        // The values at each key, carrying forward what a key does not name.
        float[] prev = STAND.clone();
        float prevTick = 0.0F;
        for (Key key : keys) {
            float[] next = prev.clone();
            for (int c = 0; c < CHANNELS; c++) {
                if (!Float.isNaN(key.values[c])) {
                    next[c] = key.values[c];
                }
            }
            if (time <= key.tick) {
                float span = key.tick - prevTick;
                float k = span <= 0.0F ? 1.0F : AnimMath.ss(0.0F, 1.0F, (time - prevTick) / span);
                for (int c = 0; c < CHANNELS; c++) {
                    out[c] = prev[c] + (next[c] - prev[c]) * k;
                }
                return out;
            }
            prev = next;
            prevTick = key.tick;
        }
        return prev;
    }

    /** Whether every state has a pose that begins at tick 0 (the tests). */
    public static boolean complete() {
        for (HelaState state : HelaState.values()) {
            List<Key> keys = POSES.get(state);
            if (keys == null || keys.isEmpty() || keys.get(0).tick != 0.0F) {
                return false;
            }
        }
        return true;
    }

    // ---- the table's builder ----

    private static Builder pose(HelaState state) {
        return new Builder(POSES.computeIfAbsent(state, s -> new ArrayList<>()));
    }

    private static float[] values(float... v) {
        return v;
    }

    private static final class Builder {
        private final List<Key> keys;

        Builder(List<Key> keys) {
            this.keys = keys;
        }

        /** A key that sets every channel. */
        Builder key(float tick, float[] all) {
            keys.add(new Key(tick, all.clone()));
            return this;
        }

        /** A key that sets the channels named (pairs of a channel and its value). */
        Builder set(float tick, Object... pairs) {
            float[] v = new float[CHANNELS];
            java.util.Arrays.fill(v, Float.NaN);
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                v[((Ch) pairs[i]).ordinal()] = ((Number) pairs[i + 1]).floatValue();
            }
            Key last = keys.isEmpty() ? null : keys.get(keys.size() - 1);
            if (last != null && last.tick == tick) {
                for (int c = 0; c < CHANNELS; c++) {
                    if (!Float.isNaN(v[c])) {
                        last.values[c] = v[c];
                    }
                }
                return this;
            }
            keys.add(new Key(tick, v));
            return this;
        }
    }

    private HelaBossPoses() {
    }
}
