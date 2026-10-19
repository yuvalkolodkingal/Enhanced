package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * How Deadpool acts what he does (3.7): his two stances (the swords spinning before him while he parries, the pistols
 * up along his look while they are drawn), what they and his gestures add to his hands in first person, the gestures
 * his moves' effects start (each names him), and what his weapons are doing for {@link DeadpoolGearLayer}: the swords
 * are in his hands while he parries and for a moment after a cut or a Drop-In, the pistols while they are drawn or
 * reloading; otherwise both are sheathed (the swords crossed on his back, the pistols at his hips).
 */
public final class DeadpoolActing {
    /** How long (ticks) the swords stay in his hands after a cut or a Drop-In. */
    static final int SWORDS_OUT = 24;
    /** How long the pistols stay out after a reload began (its twirl). */
    static final int RELOAD_OUT = 44;

    /** Each player's last cut (client game time) and reload. */
    private static final Map<Integer, Long> CUT = new HashMap<>();
    private static final Map<Integer, Long> RELOAD = new HashMap<>();
    /** When each player's Parry began (for the spin of the swords). */
    private static final Map<Integer, Long> PARRY = new HashMap<>();

    private DeadpoolActing() {
    }

    // ---- the payloads ----

    /** An effect of his arrived: the gesture it stands for plays on him; the weapons it brings out. */
    public static void onPayload(DeadpoolFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.entityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        long now = level.getGameTime();
        switch (payload.kind()) {
            case DeadpoolFxPayload.SLASH, DeadpoolFxPayload.DROP_IN -> CUT.put(player.getId(), now);
            case DeadpoolFxPayload.RELOAD -> RELOAD.put(player.getId(), now);
            case DeadpoolFxPayload.PARRY_ON -> PARRY.put(player.getId(), now);
            default -> {
            }
        }
        GestureId id = DeadpoolGestures.gestureOf(payload.kind(), payload.arg());
        if (id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s != null) {
            HeroAnimator.start(s, id, 0.0F, player.tickCount);
        }
    }

    // ---- what his weapons do ----

    /** Whether the player has Deadpool's weapons at all (the hero, or a custom hero with his swords or pistols). */
    public static boolean armed(Player player, PowerData data) {
        return data.has(HeroAbility.DEADPOOL_KATANAS) || data.has(HeroAbility.DEADPOOL_PISTOLS);
    }

    /** Whether his swords are in his hands right now. */
    public static boolean swordsOut(Player player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_KATANAS)) {
            return false;
        }
        if (data.is(Live.DEADPOOL_PARRYING)) {
            return true;
        }
        Long cut = CUT.get(player.getId());
        return cut != null && player.level().getGameTime() - cut < SWORDS_OUT;
    }

    /** Whether his pistols are in his hands right now (never with the swords). */
    public static boolean pistolsOut(Player player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_PISTOLS) || swordsOut(player, data)) {
            return false;
        }
        Long reload = RELOAD.get(player.getId());
        return data.is(Live.DEADPOOL_AIMING) || reload != null && player.level().getGameTime() - reload < RELOAD_OUT;
    }

    /** The spin of the parrying swords (degrees) at this frame. */
    public static float parrySpin(Player player, float partialTick) {
        Long start = PARRY.get(player.getId());
        float t = (start == null ? player.tickCount : player.level().getGameTime() - start) + partialTick;
        return t * 47.0F;
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        boolean parrying = data.is(Live.DEADPOOL_PARRYING) && data.has(HeroAbility.DEADPOOL_KATANAS);
        on[Stance.DEADPOOL_SWORDS.ordinal()] = parrying;
        on[Stance.DEADPOOL_GUNS.ordinal()] = !parrying && data.is(Live.DEADPOOL_AIMING)
                && data.has(HeroAbility.DEADPOOL_PISTOLS) && p.getMainHandItem().isEmpty() && !p.isUsingItem();
    }

    /** The aimed rows: the swords before him, or the pistols up along his look. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.DEADPOOL_SWORDS);
        if (w > 0.001F) {
            float sway = 3.0F * Mth.sin(c.age * 0.9F);
            c.arm(true, DeadpoolGestures.SWORDS_M[0] + sway, DeadpoolGestures.SWORDS_M[1],
                    DeadpoolGestures.SWORDS_M[2], w);
            c.arm(false, DeadpoolGestures.SWORDS_O[0] - sway, DeadpoolGestures.SWORDS_O[1],
                    DeadpoolGestures.SWORDS_O[2], w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = c.st.stance(Stance.DEADPOOL_GUNS)) > 0.001F) {
            c.aim(true, Gestures.Target.LOOK, -2, -DeadpoolGestures.GUNS_SPREAD, 0, w);
            c.aim(false, Gestures.Target.LOOK, -2, DeadpoolGestures.GUNS_SPREAD, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.DEADPOOL_SWORDS)) > 0.001F) {
            m.add(w, -0.10F, 0.12F, -0.04F, -20, 12, 0);
            o.add(w, -0.10F, 0.12F, -0.04F, -20, 12, 0);
        }
        if ((w = s.stance(Stance.DEADPOOL_GUNS)) > 0.001F) {
            m.add(w, -0.06F, 0.08F, -0.06F, -6, 4, 0);
            o.add(w, -0.06F, 0.08F, -0.06F, -6, 4, 0);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case DEADPOOL_SLASH_1 -> {
                    // From the top right, across and down to the bottom left.
                    float p = Mth.clamp((t - 2.0F) / 3.0F, 0.0F, 1.0F);
                    m.add(w2, 0.05F - 0.45F * p, 0.30F - 0.45F * p, -0.10F, -50 + 70 * p, 20 * p, 35 * p);
                }
                case DEADPOOL_SLASH_2 -> {
                    float p = Mth.clamp((t - 2.0F) / 3.0F, 0.0F, 1.0F);
                    o.add(w2, 0.05F - 0.45F * p, 0.30F - 0.45F * p, -0.10F, -50 + 70 * p, 20 * p, 35 * p);
                }
                case DEADPOOL_SLASH_3 -> {
                    float p = Mth.clamp((t - 3.0F) / 4.0F, 0.0F, 1.0F);
                    m.add(w2, -0.20F + 0.35F * p, 0.40F - 0.55F * p, -0.10F, -60 + 80 * p, 0, -20 + 40 * p);
                    o.add(w2, -0.20F + 0.35F * p, 0.40F - 0.55F * p, -0.10F, -60 + 80 * p, 0, -20 + 40 * p);
                }
                case DEADPOOL_PARRY -> {
                    m.add(w2, -0.08F, 0.10F, -0.04F, -15, 10, 0);
                    o.add(w2, -0.08F, 0.10F, -0.04F, -15, 10, 0);
                }
                case DEADPOOL_SHOOT_R -> m.add(w2, 0.0F, 0.04F, 0.06F, -14, 0, 0);
                case DEADPOOL_SHOOT_L -> o.add(w2, 0.0F, 0.04F, 0.06F, -14, 0, 0);
                case DEADPOOL_RELOAD -> {
                    float spin = 25.0F * Mth.sin(t * 0.63F);
                    m.add(w2, -0.05F, -0.12F, 0.0F, 15, spin, 0);
                    o.add(w2, -0.05F, -0.12F, 0.0F, 15, spin, 0);
                }
                case DEADPOOL_TRICK_SHOT -> m.add(w2, -0.06F, 0.08F, -0.10F, -8, 0, -12);
                case DEADPOOL_BLINK -> m.add(w2, -0.05F, -0.25F, 0.0F, 30, 0, 0);
                case DEADPOOL_DROP_IN -> {
                    float down = t < 9.0F ? 0.0F : Mth.clamp((t - 9.0F) / 3.0F, 0.0F, 1.0F);
                    m.add(w2, -0.15F, 0.45F - 0.60F * down, -0.05F, -60 + 80 * down, 0, 0);
                    o.add(w2, -0.15F, 0.45F - 0.60F * down, -0.05F, -60 + 80 * down, 0, 0);
                }
                case DEADPOOL_TAUNT_WAVE -> m.add(w2, 0.0F, 0.35F, 0.0F, -40, 0, 12 * Mth.sin(t * 0.8F));
                case DEADPOOL_TAUNT_GUNS -> {
                    m.add(w2, -0.08F, 0.10F, -0.08F, -10, 0, 0);
                    o.add(w2, -0.08F, 0.10F, -0.08F, -10, 0, 0);
                }
                case DEADPOOL_TAUNT_HEART -> {
                    m.add(w2, -0.18F, 0.15F, -0.05F, -15, 25, 0);
                    o.add(w2, -0.18F, 0.15F, -0.05F, -15, 25, 0);
                }
                case DEADPOOL_WHISTLE -> m.add(w2, -0.18F, 0.30F, 0.05F, -35, 20, 0);
                case DEADPOOL_SMOKE -> {
                    float down = Mth.clamp((t - 3.0F) / 3.0F, 0.0F, 1.0F);
                    m.add(w2, 0.0F, 0.25F - 0.50F * down, 0.0F, -30 + 60 * down, 0, 0);
                }
                case DEADPOOL_EAT -> m.add(w2, -0.18F, 0.22F + 0.04F * Mth.sin(t * 0.8F), 0.05F, -30, 20, 0);
                default -> {
                }
            }
        }
    }

    // ---- the client's life ----

    /** Every client tick: forget what is long over. */
    static void tick(ClientLevel level) {
        long now = level.getGameTime();
        CUT.values().removeIf(t -> now - t > 200 || now < t);
        RELOAD.values().removeIf(t -> now - t > 200 || now < t);
        PARRY.values().removeIf(t -> now - t > 1200 || now < t);
    }

    static void clear() {
        CUT.clear();
        RELOAD.clear();
        PARRY.clear();
    }

    /** Whether the player is Deadpool himself, as his client sees him. */
    static boolean deadpoolOf(Player p) {
        return Powers.get(p).power() == com.kolodkingal.enhanced.power.Power.DEADPOOL;
    }
}
