package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * How Iron Man acts what he does (3.8, lane L3): his four stances (the arms held out while a suit goes on or comes
 * off, the chest out for the Unibeam, the forearms forward for the Mark 1's flamethrower, the arms crossed for its
 * Brace), what they and his gestures add to his hands in first person, and the gestures his moves' effects start (each
 * names him). Who is flaming is told by the effects FLAME_ON / FLAME_OFF; the rest is in his synced data.
 */
public final class IronManActing {
    /** Who has the Mark 1's flamethrower on (entity id → client game time it began). */
    private static final Map<Integer, Long> FLAMING = new HashMap<>();

    private IronManActing() {
    }

    // ---- the payloads ----

    /** An effect of his arrived: the gesture it stands for plays on him. */
    public static void onPayload(IronFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.entityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        switch (payload.kind()) {
            case IronFxPayload.FLAME_ON -> FLAMING.put(player.getId(), level.getGameTime());
            case IronFxPayload.FLAME_OFF -> FLAMING.remove(player.getId());
            default -> {
            }
        }
        GestureId id = IronManGestures.gestureOf(payload.kind(), payload.arg());
        if (id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s != null) {
            HeroAnimator.start(s, id, 0.0F, player.tickCount);
        }
    }

    /** Whether the player's flamethrower burns (as the effects told). */
    public static boolean flaming(Player player) {
        return FLAMING.containsKey(player.getId());
    }

    /** Whether the player's beam is the Unibeam (the Mark 3's and the Mark 5's, from the chest). */
    public static boolean unibeam(Player player, PowerData data) {
        if (!data.is(Live.BEAMING)) {
            return false;
        }
        BeamStyle style = BeamRenderer.styleOf(player.getId());
        return style == AvengersBeams.UNIBEAM;
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        if (data.power() != Power.IRON_MAN) {
            return;
        }
        IronManState state = IronSuits.state(p);
        on[Stance.IRON_SUITING.ordinal()] = state.inPhase();
        on[Stance.IRON_UNIBEAM.ordinal()] = !state.inPhase() && unibeam(p, data);
        on[Stance.IRON_FLAME.ordinal()] = !state.inPhase() && flaming(p);
        on[Stance.IRON_BRACING.ordinal()] = state.has(IronManState.BRACING);
    }

    /** The aimed rows: the suit-up's arms, the Unibeam's chest, the flamethrower's forearms, the Brace's cross. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.IRON_SUITING);
        if (w > 0.001F) {
            c.arm(true, IronManGestures.SUIT_M[0], IronManGestures.SUIT_M[1], IronManGestures.SUIT_M[2], w);
            c.arm(false, IronManGestures.SUIT_O[0], IronManGestures.SUIT_O[1], IronManGestures.SUIT_O[2], w);
            c.head(-6, 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = c.st.stance(Stance.IRON_UNIBEAM)) > 0.001F) {
            float pulse = 2.0F * Mth.sin(c.age * 0.8F);
            c.arm(true, IronManGestures.BEAM_M[0] + pulse, IronManGestures.BEAM_M[1], IronManGestures.BEAM_M[2], w);
            c.arm(false, IronManGestures.BEAM_O[0] + pulse, IronManGestures.BEAM_O[1], IronManGestures.BEAM_O[2], w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = c.st.stance(Stance.IRON_FLAME)) > 0.001F) {
            float sweep = 4.0F * Mth.sin(c.age * 0.5F);
            c.arm(true, IronManGestures.FLAME_M[0], IronManGestures.FLAME_M[1] + sweep, IronManGestures.FLAME_M[2], w);
            c.arm(false, IronManGestures.FLAME_O[0], IronManGestures.FLAME_O[1] + sweep, IronManGestures.FLAME_O[2],
                    w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = c.st.stance(Stance.IRON_BRACING)) > 0.001F) {
            c.arm(true, IronManGestures.BRACE_M[0], IronManGestures.BRACE_M[1], IronManGestures.BRACE_M[2], w);
            c.arm(false, IronManGestures.BRACE_O[0], IronManGestures.BRACE_O[1], IronManGestures.BRACE_O[2], w);
            c.leg(true, -12, 6, w);
            c.leg(false, 8, -6, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The lean of his stances: back for the Unibeam's chest. */
    static float lean(HeroAnimState st) {
        return -8.0F * st.stance(Stance.IRON_UNIBEAM) + 3.0F * st.stance(Stance.IRON_BRACING);
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.IRON_SUITING)) > 0.001F) {
            m.add(w, 0.12F, -0.18F, 0.05F, 10, 0, -10);
            o.add(w, 0.12F, -0.18F, 0.05F, 10, 0, -10);
        }
        if ((w = s.stance(Stance.IRON_FLAME)) > 0.001F) {
            m.add(w, -0.08F, 0.10F, -0.10F, -12, 8, 0);
            o.add(w, -0.08F, 0.10F, -0.10F, -12, 8, 0);
        }
        if ((w = s.stance(Stance.IRON_BRACING)) > 0.001F) {
            m.add(w, -0.25F, 0.05F, 0.0F, -10, 40, 0);
            o.add(w, -0.25F, 0.05F, 0.0F, -10, 40, 0);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case IRON_REPULSOR_R -> m.add(w2, -0.04F, 0.10F + kick(t), -0.12F, -18 - 40 * kick(t), 0, 0);
                case IRON_REPULSOR_L, IRON_ARM_ROCKET -> o.add(w2, -0.04F, 0.10F + kick(t), -0.12F,
                        -18 - 40 * kick(t), 0, 0);
                case IRON_DOUBLE_BLAST -> {
                    m.add(w2, -0.10F, 0.10F + kick(t), -0.12F, -18 - 40 * kick(t), 10, 0);
                    o.add(w2, -0.10F, 0.10F + kick(t), -0.12F, -18 - 40 * kick(t), 10, 0);
                }
                case IRON_HEAVY_PUNCH -> {
                    float p = Mth.clamp((t - 4.0F) / 3.0F, 0.0F, 1.0F);
                    m.add(w2, 0.10F - 0.30F * p, 0.05F, 0.12F - 0.35F * p, -20 * p, 30 - 50 * p, 0);
                }
                case IRON_DASH_PUNCH -> m.add(w2, -0.05F, 0.06F, -0.25F, -10, 0, 0);
                case IRON_FLARES, IRON_MISSILES -> {
                    m.add(w2, 0.08F, -0.10F, 0.0F, 10, 0, 0);
                    o.add(w2, 0.08F, -0.10F, 0.0F, 10, 0, 0);
                }
                case IRON_LASER_BURST -> {
                    m.add(w2, 0.25F, 0.05F, 0.0F, 0, 0, -25);
                    o.add(w2, 0.25F, 0.05F, 0.0F, 0, 0, -25);
                }
                default -> {
                }
            }
        }
    }

    /** The recoil of a blast at {@code t} ticks into its gesture (0 → 1 at tick 2 → 0 by tick 7). */
    static float kick(float t) {
        return t < 2.0F ? t / 2.0F : Math.max(0.0F, 1.0F - (t - 2.0F) / 5.0F);
    }

    // ---- the client's life ----

    /** Every client tick: forget the flames of players gone. */
    static void tick(ClientLevel level) {
        long now = level.getGameTime();
        FLAMING.entrySet().removeIf(e -> now - e.getValue() > 200 || now < e.getValue()
                || level.getEntity(e.getKey()) == null);
    }

    static void clear() {
        FLAMING.clear();
    }
}
