package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.odin.OdinAbilities;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * How Odin acts what he does (3.5): the rows of his three stances (Gungnir planted upright while the Odinforce Shield holds,
 * kneeling in the Odinsleep, and the king's bearing at rest with the spear), what they and his gestures add to his hands
 * in first person, the catch when his spear comes home, and the small flourishes of gold at his hand.
 */
final class OdinActing {
    /** Below this speed (blocks per tick) he stands still, and at rest with Gungnir he takes the king's bearing. */
    static final float STILL = 0.02F;
    /** Whether each hero's spear was in flight at the last tick (for the catch). */
    private static final Map<HeroAnimState, Boolean> THROWN = new WeakHashMap<>();

    private OdinActing() {
    }

    // ---- the stances ----

    /** Which of his stances are on: the shield's (his own), the Odinsleep's, the bearing at rest. */
    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        boolean hero = data.power() == Power.ODIN;
        boolean shield = data.chaosShieldActive() && (hero || data.has(HeroAbility.ODIN_ODINFORCE));
        boolean sleep = data.toggle(Toggle.ODINSLEEP) && (hero || data.has(HeroAbility.ODIN_COMMAND));
        on[Stance.ODIN_SHIELD.ordinal()] = shield && !sleep;
        on[Stance.ODIN_SLEEP.ordinal()] = sleep;
        on[Stance.ODIN_REGAL.ordinal()] = hero && !shield && !sleep && s.onGround && s.speed < STILL && !p.isCrouching()
                && OdinAbilities.holdsGungnir(p) && !p.isUsingItem();
    }

    /** The ambient row: the king's bearing (both hands on the upright spear, the chin up). */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.ODIN_REGAL);
        if (w > 0.001F) {
            c.arm(true, OdinGestures.REGAL_M[0], OdinGestures.REGAL_M[1], OdinGestures.REGAL_M[2], w);
            c.arm(false, OdinGestures.REGAL_O[0], OdinGestures.REGAL_O[1], OdinGestures.REGAL_O[2], w);
            c.head(-4, 0, 0, w);
        }
    }

    /** The aimed rows: the shield (the spear planted, the other hand open) and the Odinsleep (kneeling over the spear). */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w;
        if ((w = st.stance(Stance.ODIN_SHIELD)) > 0.001F) {
            c.arm(true, OdinGestures.SHIELD_M[0], OdinGestures.SHIELD_M[1], OdinGestures.SHIELD_M[2], w);
            c.arm(false, OdinGestures.SHIELD_O[0], OdinGestures.SHIELD_O[1], OdinGestures.SHIELD_O[2], w);
            c.tremble(false, 0.6F, 1.2F, w);
            float planted = w * (1.0F - st.wAir);
            c.leg(true, -6, 2, planted);
            c.leg(false, 6, -2, planted);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.ODIN_SLEEP)) > 0.001F) {
            c.arm(true, OdinGestures.SLEEP_M[0], OdinGestures.SLEEP_M[1], OdinGestures.SLEEP_M[2], w);
            c.arm(false, OdinGestures.SLEEP_O[0], OdinGestures.SLEEP_O[1], OdinGestures.SLEEP_O[2], w);
            c.leg(true, OdinGestures.SLEEP_LEG_R, 2, w);
            c.leg(false, OdinGestures.SLEEP_LEG_L, -2, w);
            c.head(OdinGestures.SLEEP_HEAD, 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The lean of his stances: forward over the spear in the Odinsleep. */
    static float lean(HeroAnimState st) {
        return OdinGestures.SLEEP_LEAN * st.stance(Stance.ODIN_SLEEP);
    }

    /** The sink of his stances: down on one knee in the Odinsleep. */
    static float drop(HeroAnimState st) {
        return OdinGestures.SLEEP_DROP * st.stance(Stance.ODIN_SLEEP);
    }

    // ---- the edges ----

    /** His spear came home this tick: the catch. */
    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        boolean thrown = data.is(Live.ODIN_THROWN);
        Boolean was = THROWN.put(s, thrown);
        if (was != null && was && !thrown && GungnirItem.isGungnir(p.getMainHandItem())) {
            HeroAnimator.start(s, GestureId.ODIN_CATCH, 0.0F, p.tickCount);
        }
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.ODIN_SHIELD)) > 0.001F) {
            // The spear held upright at the right of the view; the left hand open toward the dome.
            m.add(w, 0.04F, 0.10F, -0.06F, 12, 0, 0);
            o.add(w, 0.06F, 0.30F, -0.04F, -40, 10, 0);
        }
        if ((w = s.stance(Stance.ODIN_SLEEP)) > 0.001F) {
            m.add(w, 0.02F, -0.12F, 0.06F, 20, 0, 0);
            o.add(w, 0.02F, -0.12F, 0.06F, 20, 0, 0);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            switch (slot.id) {
                case ODIN_BLAST, ODIN_BANISH -> m.add(w2, -0.06F, 0.06F, -0.18F, -10, 0, 0);
                case ODIN_THROW -> m.add(w2, 0.02F, 0.30F, 0.16F, -60, 0, 0);
                case ODIN_CATCH -> m.add(w2, 0.0F, 0.10F, -0.06F, -20, 0, 0);
                case ODIN_HEAL -> o.add(w2, 0.10F, 0.28F, -0.08F, -50, 0, 0);
                case ODIN_RAVENS -> o.add(w2, -0.10F, 0.36F, 0.0F, -70, 0, 20);
                case ODIN_WAVE -> m.add(w2, 0.0F, -0.10F, 0.0F, 30, 0, 0);
                case ODIN_COMMAND, ODIN_CROWN -> m.add(w2, -0.04F, 0.50F, 0.10F, -80, 0, 0);
                default -> {
                }
            }
        }
    }

    // ---- the flourishes ----

    /** A gesture of his began: motes of gold at his hand (and the spear's tip). */
    static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        int count = switch (id) {
            case ODIN_BLAST, ODIN_BANISH, ODIN_HEAL, ODIN_COMMAND -> 8;
            case ODIN_WAVE, ODIN_CROWN -> 14;
            default -> 0;
        };
        if (count == 0) {
            return;
        }
        RandomSource r = level.random;
        Vec3 hand = hand(player, id == GestureId.ODIN_HEAL || id == GestureId.ODIN_RAVENS);
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? Powers.glow(player) : Powers.color(player), 0.5F),
                    hand.x + (r.nextDouble() - 0.5) * 0.3, hand.y + (r.nextDouble() - 0.5) * 0.3, hand.z + (r.nextDouble() - 0.5) * 0.3,
                    (r.nextDouble() - 0.5) * 0.04, 0.02, (r.nextDouble() - 0.5) * 0.04);
        }
    }

    /** About where a hand of his is (the main or the other), from his body's turn. */
    private static Vec3 hand(Player player, boolean off) {
        double yaw = Math.toRadians(player.yBodyRot);
        double side = off ? -0.38 : 0.38;
        return player.position().add(-Math.cos(yaw) * side - Math.sin(yaw) * 0.3, 1.2, -Math.sin(yaw) * side + Math.cos(yaw) * 0.3);
    }

    // ---- what the local hero can do ----

    /** Whether a blast or a throw of his will be answered: the hero needs a Gungnir in his hand (a custom hero blasts anyway). */
    static boolean spearReady(Player player, PowerData data, boolean throwing) {
        if (data.is(Live.ODIN_THROWN)) {
            return false;
        }
        if (throwing) {
            return GungnirItem.isGungnir(player.getMainHandItem()) || data.power() != Power.ODIN;
        }
        return data.power() != Power.ODIN || OdinAbilities.holdsGungnir(player);
    }

    /** The gesture the local hero's own key press starts at once, or null (only what the server will answer). */
    static GestureId predicted(PowerData data, int ability, long now, boolean sneaking) {
        Player self = Minecraft.getInstance().player;
        if (self == null || data.toggle(Toggle.ODINSLEEP)) {
            return null;
        }
        return switch (ability) {
            case 1 -> data.isReady(HeroAbility.ODIN_GUNGNIR, now) && spearReady(self, data, sneaking)
                    ? (sneaking ? GestureId.ODIN_THROW : GestureId.ODIN_BLAST) : null;
            case 2 -> sneaking && data.isReady(HeroAbility.ODIN_ODINFORCE, now) ? GestureId.ODIN_HEAL : null;
            case 3 -> data.isReady(HeroAbility.ODIN_BANISH, now) ? (sneaking ? GestureId.ODIN_WAVE : GestureId.ODIN_BANISH) : null;
            case 4 -> data.isReady(HeroAbility.ODIN_RAVENS, now) ? GestureId.ODIN_RAVENS : null;
            case 5 -> !sneaking && data.isReady(HeroAbility.ODIN_COMMAND, now) ? GestureId.ODIN_COMMAND : null;
            default -> null;
        };
    }

    static void clear() {
        THROWN.clear();
    }
}
