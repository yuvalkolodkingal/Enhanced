package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.falcon.RedwingEntity;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How Falcon acts what he does (3.8): his three stances (gliding with the arms out along the wings, crossed behind the
 * Wing Shield, tipped head first in the Dive), what they and his gestures add to his hands in first person, and the
 * gestures his moves' effects start (each names him, or his Redwing for its Strike). It also remembers when his last
 * Wing Slash and Boost Climb began, for the wings' poses ({@code client/falcon/FalconWingsLayer}).
 */
public final class FalconActing {
    /** How long (ticks) the wings stay flung out after a Wing Slash, and swept back after a Boost Climb. */
    public static final int SLASH_TICKS = 12;
    public static final int CLIMB_TICKS = 20;

    /** Each player's last Wing Slash and Boost Climb (client game time). */
    private static final Map<Integer, Long> SLASH = new HashMap<>();
    private static final Map<Integer, Long> CLIMB = new HashMap<>();

    private FalconActing() {
    }

    // ---- the payloads ----

    /** An effect of his arrived: the gesture it stands for plays on him. */
    public static void onPayload(FalconFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.entityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (entity instanceof RedwingEntity drone) {
            UUID owner = drone.ownerId();
            entity = owner == null ? null : level.getPlayerByUUID(owner);
        }
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        long now = level.getGameTime();
        switch (payload.kind()) {
            case FalconFxPayload.SLASH -> SLASH.put(player.getId(), now);
            case FalconFxPayload.CLIMB -> {
                if (payload.arg() == 0) {
                    CLIMB.put(player.getId(), now);
                }
            }
            default -> {
            }
        }
        GestureId id = FalconGestures.gestureOf(payload.kind(), payload.arg());
        if (id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s != null) {
            HeroAnimator.start(s, id, 0.0F, player.tickCount);
        }
    }

    /** How far (0-1) into a Wing Slash the player is now (0: none), with the frame's part of a tick. */
    public static float slash(Player player, float partialTick) {
        return progress(SLASH.get(player.getId()), player, partialTick, SLASH_TICKS);
    }

    /** How far (0-1) into a Boost Climb the player is now (0: none). */
    public static float climb(Player player, float partialTick) {
        return progress(CLIMB.get(player.getId()), player, partialTick, CLIMB_TICKS);
    }

    private static float progress(Long start, Player player, float partialTick, int ticks) {
        if (start == null) {
            return 0.0F;
        }
        float t = player.level().getGameTime() - start + partialTick;
        return t < 0.0F || t >= ticks ? 0.0F : Math.max(0.001F, t / ticks);
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        boolean shielding = data.is(Live.FALCON_WING_SHIELD) && data.has(HeroAbility.FALCON_WING_SHIELD);
        boolean diving = data.is(Live.FALCON_DIVING) && data.has(HeroAbility.FALCON_DIVE);
        on[Stance.FALCON_SHIELDING.ordinal()] = shielding;
        on[Stance.FALCON_DIVING.ordinal()] = diving && !shielding;
        on[Stance.FALCON_GLIDE.ordinal()] = p.isFallFlying() && !shielding && !diving;
    }

    /** The ambient rows: gliding, the arms out along the wings. */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.FALCON_GLIDE);
        if (w > 0.001F) {
            float sway = 2.0F * Mth.sin(c.age * 0.21F);
            c.arm(true, FalconGestures.GLIDE_M[0] + sway, FalconGestures.GLIDE_M[1], FalconGestures.GLIDE_M[2], w);
            c.arm(false, FalconGestures.GLIDE_O[0] - sway, FalconGestures.GLIDE_O[1], FalconGestures.GLIDE_O[2], w);
        }
    }

    /** The aimed rows: the arms crossed behind the shield, or tight along him in the Dive. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.FALCON_SHIELDING);
        if (w > 0.001F) {
            c.arm(true, FalconGestures.SHIELD_M[0], FalconGestures.SHIELD_M[1], FalconGestures.SHIELD_M[2], w);
            c.arm(false, FalconGestures.SHIELD_O[0], FalconGestures.SHIELD_O[1], FalconGestures.SHIELD_O[2], w);
            c.head(FalconGestures.SHIELD_HEAD, 0.0F, 0.0F, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = c.st.stance(Stance.FALCON_DIVING)) > 0.001F) {
            c.arm(true, FalconGestures.DIVE_M[0], FalconGestures.DIVE_M[1], FalconGestures.DIVE_M[2], w);
            c.arm(false, FalconGestures.DIVE_O[0], FalconGestures.DIVE_O[1], FalconGestures.DIVE_O[2], w);
            c.head(FalconGestures.DIVE_HEAD, 0.0F, 0.0F, w);
            c.leg(true, 4.0F, 1.5F, w);
            c.leg(false, 4.0F, -1.5F, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The Dive tips him over, head first. */
    static float lean(HeroAnimState st) {
        return FalconGestures.DIVE_LEAN * st.stance(Stance.FALCON_DIVING);
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.FALCON_SHIELDING)) > 0.001F) {
            // The forearms crossed before his eyes, the wings' edges round them.
            m.add(w, -0.22F, 0.12F, -0.05F, -15, 35, 0);
            o.add(w, -0.22F, 0.18F, -0.05F, -15, 35, 0);
        }
        if ((w = s.stance(Stance.FALCON_DIVING)) > 0.001F) {
            m.add(w, 0.04F, -0.45F, 0.10F, 40, 0, 0);
            o.add(w, 0.04F, -0.45F, 0.10F, 40, 0, 0);
        }
        if ((w = s.stance(Stance.FALCON_GLIDE)) > 0.001F) {
            m.add(w, 0.12F, -0.18F, 0.05F, 10, 0, -12);
            o.add(w, 0.12F, -0.18F, 0.05F, 10, 0, -12);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case FALCON_BLAST_R -> m.add(w2, -0.04F, 0.06F, kick(t, 3.0F), -12, 0, 0);
                case FALCON_BLAST_L -> o.add(w2, -0.04F, 0.06F, kick(t, 3.0F), -12, 0, 0);
                case FALCON_ROCKET -> {
                    m.add(w2, -0.08F, 0.10F, kick(t, 4.0F) * 1.5F, -16, 0, 0);
                    o.add(w2, -0.18F, 0.02F, 0.0F, -10, 20, 0);
                }
                case FALCON_DIVE_HIT -> {
                    float p = Mth.clamp(t / 2.0F, 0.0F, 1.0F);
                    m.add(w2, -0.10F, 0.10F * p, -0.30F * p, -30 * p, 0, 0);
                    o.add(w2, -0.10F, 0.10F * p, -0.30F * p, -30 * p, 0, 0);
                }
                case FALCON_CLIMB -> {
                    m.add(w2, 0.05F, -0.35F, 0.10F, 35, 0, 0);
                    o.add(w2, 0.05F, -0.35F, 0.10F, 35, 0, 0);
                }
                case FALCON_SHIELD -> {
                    float p = Mth.clamp(t / 6.0F, 0.0F, 1.0F);
                    m.add(w2 * (1.0F - p), -0.22F, 0.12F, -0.05F, -15, 35, 0);
                    o.add(w2 * (1.0F - p), -0.22F, 0.18F, -0.05F, -15, 35, 0);
                }
                case FALCON_SLASH -> {
                    m.add(w2, 0.35F, 0.0F, 0.0F, 0, 0, -30);
                    o.add(w2, 0.35F, 0.0F, 0.0F, 0, 0, -30);
                }
                case FALCON_REDWING_LAUNCH -> {
                    float flick = Mth.sin(Mth.PI * Mth.clamp(t / 8.0F, 0.0F, 1.0F));
                    m.add(w2, 0.05F, 0.40F * flick, 0.05F, -40 * flick, 0, 10 * flick);
                }
                case FALCON_REDWING_POINT -> m.add(w2, -0.10F, 0.10F, -0.12F, -10, 0, 0);
                default -> {
                }
            }
        }
    }

    /** A kick back of the wrist {@code at} ticks into a shot: out and back. */
    private static float kick(float t, float at) {
        return 0.10F * Math.max(0.0F, 1.0F - Math.abs(t - at) / 2.0F);
    }

    // ---- the client's life ----

    /** Every client tick: forget what is long over. */
    static void tick(ClientLevel level) {
        long now = level.getGameTime();
        SLASH.values().removeIf(t -> now - t > 200 || now < t);
        CLIMB.values().removeIf(t -> now - t > 200 || now < t);
    }

    static void clear() {
        SLASH.clear();
        CLIMB.clear();
    }
}
