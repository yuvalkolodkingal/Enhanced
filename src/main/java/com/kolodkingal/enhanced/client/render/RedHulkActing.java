package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * How Red Hulk acts what he does (3.8, lane L2): his stances (the Hulk's hunched rest, which his renderer draws on the
 * Hulk's body; steaming hot from anger 75; deep in the Super Jump's crouch, sinking with the charge; tucked in the air;
 * planted for the Energy Absorb), what they and his gestures add to his hands in first person, and the gestures his
 * moves' payloads start ({@link RedHulkGestures#gestureOf}). Ross acts as anyone.
 */
public final class RedHulkActing {
    /** Each charging Red Hulk (by id): the game time his charge began and its full length. */
    private static final Map<Integer, long[]> CHARGES = new HashMap<>();

    private RedHulkActing() {
    }

    /** Whether the data's player has Red Hulk's body now (not Ross). */
    static boolean red(PowerData data) {
        return data.has(HeroAbility.RED_HULK_BODY) && !data.toggle(Toggle.ROSS_FORM);
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        boolean red = red(data);
        boolean air = data.is(Live.RED_HULK_LEAPING) && !s.onGround && !data.is(Live.RED_HULK_JUMP_CHARGING);
        boolean charging = data.is(Live.RED_HULK_JUMP_CHARGING);
        boolean absorbing = data.is(Live.RED_HULK_ABSORBING);
        boolean heated = red && data.is(Live.RED_HULK_HEATED);
        boolean busy = air || charging || absorbing || data.is(Live.RED_HULK_TRANSFORMING);
        on[Stance.RED_HULK_AIR.ordinal()] = air;
        on[Stance.RED_HULK_CHARGING.ordinal()] = charging;
        on[Stance.RED_HULK_ABSORBING.ordinal()] = absorbing;
        on[Stance.RED_HULK_HEATED.ordinal()] = heated && !busy;
        on[Stance.HULK_IDLE.ordinal()] = red && !heated && !busy && !p.isUsingItem();
    }

    /** The ambient rows: the Hulk's hunched rest, and steaming hot. */
    static void ambient(PoseContext c) {
        HulkActing.ambient(c);
        float w = c.st.stance(Stance.RED_HULK_HEATED);
        if (w > 0.001F) {
            float heave = 3.0F * Mth.sin(c.age * 0.35F);
            c.arm(true, RedHulkGestures.HEATED_M[0] + heave, RedHulkGestures.HEATED_M[1],
                    RedHulkGestures.HEATED_M[2], w);
            c.arm(false, RedHulkGestures.HEATED_O[0] + heave, RedHulkGestures.HEATED_O[1],
                    RedHulkGestures.HEATED_O[2], w);
            c.tremble(true, 1.8F, 1.1F, w);
            c.tremble(false, 1.8F, 1.1F, w);
            c.head(-2 + heave * 0.5F, 0, 0, w);
        }
    }

    /** The aimed rows: the charge, the air, the absorb. */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w = st.stance(Stance.RED_HULK_CHARGING);
        if (w > 0.001F) {
            float k = charge(c.player.getId(), c.player.level().getGameTime() + c.pt);
            c.arm(true, RedHulkGestures.CHARGE_M[0] + 8 * k, RedHulkGestures.CHARGE_M[1],
                    RedHulkGestures.CHARGE_M[2], w);
            c.arm(false, RedHulkGestures.CHARGE_O[0] + 8 * k, RedHulkGestures.CHARGE_O[1],
                    RedHulkGestures.CHARGE_O[2], w);
            c.leg(true, RedHulkGestures.CHARGE_LEGS - 14 * k, 6, w);
            c.leg(false, RedHulkGestures.CHARGE_LEGS - 14 * k, -6, w);
            c.tremble(true, 0.6F + 1.8F * k, 1.6F, w);
            c.tremble(false, 0.6F + 1.8F * k, 1.6F, w);
            c.head(-10 - 8 * k, 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.RED_HULK_AIR)) > 0.001F) {
            c.arm(true, RedHulkGestures.AIR_M[0], RedHulkGestures.AIR_M[1], RedHulkGestures.AIR_M[2], w);
            c.arm(false, RedHulkGestures.AIR_O[0], RedHulkGestures.AIR_O[1], RedHulkGestures.AIR_O[2], w);
            c.leg(true, RedHulkGestures.AIR_LEG_R, 4, w);
            c.leg(false, RedHulkGestures.AIR_LEG_L, -4, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.RED_HULK_ABSORBING)) > 0.001F) {
            c.arm(true, RedHulkGestures.ABSORB_M[0], RedHulkGestures.ABSORB_M[1], RedHulkGestures.ABSORB_M[2], w);
            c.arm(false, RedHulkGestures.ABSORB_O[0], RedHulkGestures.ABSORB_O[1], RedHulkGestures.ABSORB_O[2], w);
            c.tremble(true, 1.2F, 1.4F, w);
            c.tremble(false, 1.2F, 1.4F, w);
            c.leg(true, -8, 10, w);
            c.leg(false, -8, -10, w);
            c.head(10, 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The lean of his stances (the Hulk's rest, the heat, the crouch). */
    static float lean(HeroAnimState st) {
        return HulkActing.lean(st) + RedHulkGestures.HEATED_LEAN * st.stance(Stance.RED_HULK_HEATED)
                + RedHulkGestures.CHARGE_LEAN * st.stance(Stance.RED_HULK_CHARGING) + 6.0F
                * st.stance(Stance.RED_HULK_ABSORBING);
    }

    /** The dip of the crouch and the absorb's wide stance. */
    static float drop(HeroAnimState st) {
        return RedHulkGestures.CHARGE_DROP * st.stance(Stance.RED_HULK_CHARGING) + 0.12F
                * st.stance(Stance.RED_HULK_ABSORBING);
    }

    /** How far the charge of a player has come (0-1), or 0. */
    static float charge(int id, float time) {
        long[] c = CHARGES.get(id);
        return c == null ? 0.0F : Mth.clamp((time - c[0]) / Math.max(1.0F, c[1]), 0.0F, 1.0F);
    }

    /** The charge of the local player (the HUD's ring), or -1 when he is not charging. */
    public static float localCharge() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !CHARGES.containsKey(mc.player.getId())) {
            return -1.0F;
        }
        return charge(mc.player.getId(), mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false));
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.RED_HULK_HEATED)) > 0.001F) {
            m.add(w, -0.02F, 0.05F, -0.05F, -8, 0, 0);
            o.add(w, 0.02F, 0.05F, -0.05F, -8, 0, 0);
        }
        if ((w = s.stance(Stance.RED_HULK_CHARGING)) > 0.001F) {
            m.add(w, 0.0F, -0.35F, 0.05F, 35, 0, 0);
            o.add(w, 0.0F, -0.35F, 0.05F, 35, 0, 0);
        }
        if ((w = s.stance(Stance.RED_HULK_AIR)) > 0.001F) {
            m.add(w, 0.0F, 0.30F, 0.05F, -50, 0, 0);
            o.add(w, 0.0F, 0.30F, 0.05F, -50, 0, 0);
        }
        if ((w = s.stance(Stance.RED_HULK_ABSORBING)) > 0.001F) {
            m.add(w, -0.18F, 0.08F, 0.05F, -20, 25, 0);
            o.add(w, 0.18F, 0.08F, 0.05F, -20, -25, 0);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            switch (slot.id) {
                case RED_HULK_HEAT_WAVE -> {
                    // Wide, then both palms driven into the middle of the view.
                    float push = slot.t < 5.0F ? 0.0F : Math.min(1.0F, (slot.t - 5.0F) / 3.0F);
                    m.add(w2, 0.25F - 0.40F * push, 0.10F, -0.20F * push, -10 * push, 15 * push, 0);
                    o.add(w2, -0.25F + 0.40F * push, 0.10F, -0.20F * push, -10 * push, -15 * push, 0);
                }
                case RED_HULK_BURST -> {
                    m.add(w2, 0.15F, -0.20F, 0.10F, 30, 0, 20);
                    o.add(w2, -0.15F, -0.20F, 0.10F, 30, 0, -20);
                }
                case RED_HULK_JUMP -> {
                    m.add(w2, 0.0F, 0.35F, 0.0F, -60, 0, 0);
                    o.add(w2, 0.0F, 0.35F, 0.0F, -60, 0, 0);
                }
                case RED_HULK_TRANSFORM_OUT -> {
                    m.add(w2, 0.05F, -0.05F, 0.05F, 10, 0, 15);
                    o.add(w2, -0.05F, -0.05F, 0.05F, 10, 0, -15);
                }
                case RED_HULK_TRANSFORM_IN -> m.add(w2, 0.0F, -0.20F, 0.05F, 25, 0, 0);
                case RED_HULK_ROSS -> m.add(w2, -0.25F, 0.15F, 0.0F, -30, 30, 0);
                default -> {
                }
            }
        }
    }

    // ---- the payloads: the gesture each of his moves plays ----

    /** A payload of his arrived: the gesture it stands for plays on him; the charge's start is noted for the pose. */
    public static void onPayload(RedHulkFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.entityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (payload.kind() == RedHulkFxPayload.JUMP_CHARGE) {
            CHARGES.put(player.getId(), new long[]{level.getGameTime(), Math.max(1, payload.arg())});
        } else if (payload.kind() == RedHulkFxPayload.JUMP) {
            CHARGES.remove(player.getId());
        }
        GestureId id = RedHulkGestures.gestureOf(payload.kind(), payload.arg());
        if (id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s != null) {
            HeroAnimator.start(s, id, 0.0F, player.tickCount);
        }
    }

    /** Every client tick: a charge that is over is forgotten. */
    static void tick(ClientLevel level) {
        CHARGES.keySet().removeIf(id -> !(level.getEntity(id) instanceof AbstractClientPlayer p)
                || !com.kolodkingal.enhanced.power.Powers.get(p).is(Live.RED_HULK_JUMP_CHARGING)
                && level.getGameTime() - CHARGES.get(id)[0] > 5);
    }

    /** A gesture of his began: the dust at his feet for the heavy ones. */
    static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        int count = switch (id) {
            case RED_HULK_JUMP_CROUCH -> 10;
            case RED_HULK_JUMP -> 24;
            case RED_HULK_TRANSFORM_OUT -> 12;
            default -> 0;
        };
        if (count > 0) {
            HulkActing.dust(level, player, count);
        }
    }

    static void clear() {
        CHARGES.clear();
    }
}
