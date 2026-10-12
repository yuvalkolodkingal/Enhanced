package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import org.jetbrains.annotations.Nullable;

/**
 * How brightly each socket of the Gauntlet burns (DESIGN section 15.2.6): a stone he holds glows softly; the one a move
 * calls on swells through the clench and flares, and fades after it; the newest flares in its surge; the next one
 * pulses in the last ten seconds before it arrives; the five flare in turn for the Infinity Beam; they go dark one by
 * one when he dies; and the sixth socket, the Mind Stone's, only glimmers now and then once he holds five. Pure
 * arithmetic, so the game tests can read it.
 */
public final class ThanosGlow {
    /** The sockets: the five stones he can hold, then the sixth. */
    public static final int SOCKETS = 6;
    /** The sixth socket, which never lights for long. */
    public static final int MIND = 5;
    /** How bright a stone he holds burns when he is not using it. */
    public static final float IDLE = 0.25F;
    /** The ticks before a stone arrives in which its socket starts to pulse. */
    public static final int WARNING = 200;

    /**
     * Fills {@code alpha} with the brightness of each socket, 0 (a dark gem) to 1 (a flare).
     *
     * @param stones    how many sockets are lit
     * @param nextIn    the ticks until the next stone, or -1 if none is coming
     * @param move      the move he is making, or null
     * @param moveTime  the ticks since the move began
     * @param stateTime the ticks since his state changed
     * @param age       a clock that runs on, for the pulses
     */
    public static void sockets(float[] alpha, int stones, int nextIn, ThanosState state, @Nullable ThanosMove move, float moveTime,
                               float stateTime, float age) {
        for (int i = 0; i < MIND; i++) {
            alpha[i] = i < stones ? IDLE + 0.05F * (float) Math.sin(age * 0.15F + i) : 0.0F;
        }
        alpha[MIND] = 0.0F;
        if (state == ThanosState.ARRIVING) {
            // The Power Stone lights as he rises.
            alpha[0] = IDLE * Math.min(1.0F, stateTime / 40.0F);
            return;
        }
        if (stones < MIND && nextIn >= 0 && nextIn <= WARNING) {
            float closeness = 1.0F - nextIn / (float) WARNING;
            float pulse = 0.5F + 0.5F * (float) Math.sin(age * (0.25F + 0.3F * closeness));
            alpha[stones] = (0.12F + 0.4F * closeness) * (0.4F + 0.6F * pulse);
        }
        if (stones >= MIND) {
            // The tease: the sixth socket glimmers yellow for a second every twenty.
            float phase = age % 400.0F;
            if (phase < 20.0F) {
                alpha[MIND] = 0.9F * (float) Math.sin(Math.PI * phase / 20.0F);
            }
        }
        switch (state) {
            case SURGE -> {
                // The newest stone fuses: it flares and settles over the surge.
                float flare = 1.0F - Math.min(1.0F, stateTime / 50.0F);
                alpha[Math.max(0, stones - 1)] = Math.max(alpha[Math.max(0, stones - 1)], 0.35F + 0.65F * flare);
            }
            case DYING -> {
                // The sockets go dark one by one, the newest first.
                for (int i = 0; i < stones; i++) {
                    float darkAt = 8.0F * (stones - i);
                    alpha[i] *= 1.0F - Math.min(1.0F, Math.max(0.0F, (stateTime - darkAt + 6.0F) / 6.0F));
                }
                alpha[MIND] = 0.0F;
            }
            case TELL, ACTIVE -> {
                if (move != null) {
                    moveGlow(alpha, stones, move, state, moveTime);
                }
            }
            default -> {
            }
        }
    }

    private static void moveGlow(float[] alpha, int stones, ThanosMove move, ThanosState state, float moveTime) {
        StoneKind stone = move.stone();
        if (stone == null) {
            return;
        }
        if (move == ThanosMove.INFINITY_BEAM) {
            // The five flare in turn, eight ticks apart, and stay lit through the beam.
            for (int i = 0; i < Math.min(stones, MIND); i++) {
                float since = moveTime - (1.0F + 8.0F * i);
                if (since >= 0.0F) {
                    alpha[i] = Math.max(alpha[i], 0.6F + 0.4F * Math.min(1.0F, since / 4.0F));
                }
            }
            return;
        }
        int index = stone.ordinal();
        if (index >= MIND) {
            return;
        }
        // The clench: the stone swells through the wind-up, burns full while the effect runs.
        float swell = state == ThanosState.TELL ? Math.min(1.0F, 0.3F + moveTime / Math.max(1.0F, move.tell())) : 1.0F;
        alpha[index] = Math.max(alpha[index], swell);
    }

    /** How bright the halo of a socket is for a stone's brightness (a lit stone has one; a dark gem none). */
    public static float halo(float alpha) {
        return alpha > IDLE + 0.1F ? 0.5F * alpha : 0.0F;
    }

    private ThanosGlow() {
    }
}
