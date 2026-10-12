package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * How the sockets of the Gauntlet of a playing Thanos burn (DESIGN section 15.13.2): all five stones glow softly, a
 * stone flares when its ability is used (the cooldown of its key starts, or the drain begins), and burns on while
 * something it made lasts (the Veil, the Drain, the Sight). The flares are found by watching each Thanos's cooldowns
 * tick by tick; the sockets are read by {@link GauntletLayer} for every frame it draws one.
 */
public final class ThanosHeroGlow {
    /** How long a flare burns at full before it dies away, and how long that takes (ticks). */
    private static final float FLARE = 8.0F;
    private static final float FADE = 10.0F;
    /** How bright a stone burns while its effect lasts. */
    private static final float HELD = 0.7F;

    /** What is known of one Thanos: when each key last flared, and the cooldowns last seen. */
    private static final class State {
        final long[] flaredAt = new long[PowerData.ABILITIES + 1];
        final long[] seenUntil = new long[PowerData.ABILITIES + 1];
        boolean wasDraining;

        State() {
            java.util.Arrays.fill(flaredAt, Long.MIN_VALUE / 2);
        }
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    /** Watches every Thanos in view: a cooldown that has just started is a stone being used. Once per client tick. */
    public static void tick(ClientLevel level) {
        long now = level.getGameTime();
        STATES.keySet().removeIf(id -> !(level.getEntity(id) instanceof AbstractClientPlayer p) || Powers.get(p).power() != Power.THANOS);
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.power() != Power.THANOS) {
                continue;
            }
            State state = STATES.computeIfAbsent(player.getId(), id -> new State());
            for (int slot = 1; slot <= PowerData.ABILITIES; slot++) {
                long until = data.cooldownUntil(slot);
                if (until > state.seenUntil[slot] && until > now + 1) {
                    state.flaredAt[slot] = now;
                }
                state.seenUntil[slot] = until;
            }
            boolean draining = data.is(Live.SOUL_DRAINING);
            if (draining && !state.wasDraining) {
                state.flaredAt[4] = now;
            }
            state.wasDraining = draining;
        }
    }

    /**
     * The brightness of each socket of this Thanos's Gauntlet, and the glow round his fist: which stone he used last
     * and how strongly, written to {@code alpha} (and the returned array {color, strength}).
     *
     * @param time the game time with the partial tick
     * @return the colour of the glow round the fist (0 for none) and its strength
     */
    public static float[] sockets(AbstractClientPlayer player, PowerData data, float time, float[] alpha) {
        State state = STATES.get(player.getId());
        for (int i = 0; i < ThanosGlow.MIND; i++) {
            alpha[i] = ThanosGlow.IDLE + 0.05F * Mth.sin(time * 0.15F + i);
        }
        alpha[ThanosGlow.MIND] = 0.0F;
        // What lasts keeps its stone burning.
        if (data.toggle(Toggle.REALITY_VEIL)) {
            alpha[2] = Math.max(alpha[2], HELD);
        }
        if (data.is(Live.SOUL_DRAINING) || data.is(Live.SOUL_SIGHT)) {
            alpha[3] = Math.max(alpha[3], HELD);
        }
        float[] aura = {0.0F, 0.0F};
        if (state == null) {
            return aura;
        }
        for (int slot = 1; slot <= ThanosGlow.MIND; slot++) {
            float since = time - state.flaredAt[slot];
            if (since < 0.0F || since > FLARE + FADE) {
                continue;
            }
            float flare = since < FLARE ? 1.0F : 1.0F - (since - FLARE) / FADE;
            alpha[slot - 1] = Math.max(alpha[slot - 1], flare);
            if (flare > aura[1]) {
                aura[0] = slot;
                aura[1] = flare;
            }
        }
        return aura;
    }

    public static void clear() {
        STATES.clear();
    }

    private ThanosHeroGlow() {
    }
}
