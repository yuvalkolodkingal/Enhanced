package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.wolverine.ClawClimb;
import com.kolodkingal.enhanced.wolverine.Claws;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How Wolverine acts what he does (3.7): his three stances (the claws-out guard, the hunch of the Rage, the climb on a
 * wall), what they and his gestures add to his hands in first person (both forearms in view while the claws are out),
 * the gestures his moves' payloads start (each names him: the slashes, the spin, the pounce's flight and stab, the
 * Feral Leap, the Sniff), and the dust of his leaps.
 */
public final class WolverineActing {
    private WolverineActing() {
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        boolean raging = data.is(Live.WOLVERINE_BERSERK) && data.has(HeroAbility.WOLVERINE_BERSERKER);
        boolean climbing = !s.onGround && ClawClimb.on(p, data) && ClawClimb.wall(p) != null;
        on[Stance.WOLVERINE_CLIMBING.ordinal()] = climbing;
        on[Stance.WOLVERINE_BERSERK.ordinal()] = raging && !climbing && !p.isUsingItem();
        on[Stance.WOLVERINE_CLAWS.ordinal()] = Claws.out(data) && !raging && !climbing && !p.isUsingItem()
                && !data.is(Live.WOLVERINE_POUNCING);
    }

    /** The ambient rows: the claws' guard, and the Rage's hunch. */
    static void ambient(PoseContext c) {
        HeroAnimState st = c.st;
        float w = st.stance(Stance.WOLVERINE_CLAWS);
        if (w > 0.001F) {
            c.arm(true, WolverineGestures.GUARD_M[0], WolverineGestures.GUARD_M[1], WolverineGestures.GUARD_M[2], w);
            c.arm(false, WolverineGestures.GUARD_O[0], WolverineGestures.GUARD_O[1], WolverineGestures.GUARD_O[2], w);
        }
        if ((w = st.stance(Stance.WOLVERINE_BERSERK)) > 0.001F) {
            // The chest heaves: a quick breath swells the shoulders.
            float breath = 3.0F * Mth.sin(c.age * 0.35F);
            c.arm(true, WolverineGestures.RAGE_M[0] + breath, WolverineGestures.RAGE_M[1],
                    WolverineGestures.RAGE_M[2] + breath, w);
            c.arm(false, WolverineGestures.RAGE_O[0] + breath, WolverineGestures.RAGE_O[1],
                    WolverineGestures.RAGE_O[2] - breath, w);
            c.tremble(true, 1.2F, 1.1F, w);
            c.tremble(false, 1.2F, 1.1F, w);
            c.head(-6, 0, 0, w);
        }
    }

    /** The aimed row: the claws hooking in turn on a wall. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.WOLVERINE_CLIMBING);
        if (w > 0.001F) {
            float k = Mth.sin(c.age * 0.45F);
            c.arm(true, WolverineGestures.CLIMB_M[0] + 26.0F * k, WolverineGestures.CLIMB_M[1],
                    WolverineGestures.CLIMB_M[2], w);
            c.arm(false, WolverineGestures.CLIMB_O[0] - 26.0F * k, WolverineGestures.CLIMB_O[1],
                    WolverineGestures.CLIMB_O[2], w);
            c.leg(true, -30.0F - 20.0F * k, 4, w);
            c.leg(false, -30.0F + 20.0F * k, -4, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The lean of his stances: a little forward in the guard, hunched in the Rage. */
    static float lean(HeroAnimState st) {
        return WolverineGestures.GUARD_LEAN * st.stance(Stance.WOLVERINE_CLAWS)
                + WolverineGestures.RAGE_LEAN * st.stance(Stance.WOLVERINE_BERSERK);
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person: both claws in view while they are out. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.WOLVERINE_CLAWS)) > 0.001F) {
            m.add(w, -0.02F, 0.08F, -0.04F, -10, 0, 0);
            o.add(w, -0.02F, 0.08F, -0.04F, -10, 0, 0);
        }
        if ((w = s.stance(Stance.WOLVERINE_BERSERK)) > 0.001F) {
            m.add(w, 0.04F, 0.10F, -0.06F, -14, 0, 8);
            o.add(w, 0.04F, 0.10F, -0.06F, -14, 0, 8);
        }
        if ((w = s.stance(Stance.WOLVERINE_CLIMBING)) > 0.001F) {
            float k = Mth.sin((float) s.time * 0.45F);
            m.add(w, 0.0F, 0.40F + 0.12F * k, -0.05F, -60, 0, 0);
            o.add(w, 0.0F, 0.40F - 0.12F * k, -0.05F, -60, 0, 0);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            switch (slot.id) {
                case WOLVERINE_SNIKT -> {
                    // The fists jolt up into view as the blades shoot.
                    float up = slot.t < 3.0F ? slot.t / 3.0F : 1.0F;
                    m.add(w2, -0.06F, 0.16F * up, -0.08F * up, -20 * up, 0, 0);
                    o.add(w2, -0.06F, 0.16F * up, -0.08F * up, -20 * up, 0, 0);
                }
                case WOLVERINE_SHINK -> {
                    m.add(w2, 0.0F, -0.06F, 0.0F, 6, 0, 0);
                    o.add(w2, 0.0F, -0.06F, 0.0F, 6, 0, 0);
                }
                case WOLVERINE_SLASH_1 -> m.add(w2, slash(slot.t), 0.18F, -0.12F, -30, 0, slashRoll(slot.t));
                case WOLVERINE_SLASH_2 -> o.add(w2, slash(slot.t), 0.18F, -0.12F, -30, 0, slashRoll(slot.t));
                case WOLVERINE_SLASH_3 -> {
                    float k = slot.t < 3.0F ? 0.0F : Math.min(1.0F, slot.t - 3.0F);
                    m.add(w2, -0.25F + 0.45F * k, 0.42F - 0.50F * k, -0.10F, -60 + 50 * k, 0, 20 * k);
                    o.add(w2, -0.25F + 0.45F * k, 0.42F - 0.50F * k, -0.10F, -60 + 50 * k, 0, 20 * k);
                }
                case WOLVERINE_SPIN -> {
                    m.add(w2, 0.30F, 0.10F, 0.05F, 0, 0, -30);
                    o.add(w2, 0.30F, 0.10F, 0.05F, 0, 0, -30);
                }
                case WOLVERINE_POUNCE_CROUCH -> {
                    m.add(w2, 0.05F, -0.20F, 0.10F, 25, 0, 0);
                    o.add(w2, 0.05F, -0.20F, 0.10F, 25, 0, 0);
                }
                case WOLVERINE_POUNCE_AIR -> {
                    m.add(w2, -0.05F, 0.22F, -0.18F, -40, 0, 0);
                    o.add(w2, -0.05F, 0.22F, -0.18F, -40, 0, 0);
                }
                case WOLVERINE_POUNCE_STAB -> {
                    float down = slot.t < 2.0F ? slot.t / 2.0F : 1.0F;
                    m.add(w2, -0.04F, 0.30F - 0.55F * down, -0.20F, -40 + 70 * down, 0, 0);
                    o.add(w2, -0.04F, 0.30F - 0.55F * down, -0.20F, -40 + 70 * down, 0, 0);
                }
                case WOLVERINE_FERAL_LEAP -> o.add(w2, 0.0F, 0.20F, -0.20F, -35, 0, 0);
                case WOLVERINE_BERSERK_ROAR -> {
                    m.add(w2, 0.22F, -0.10F, 0.10F, 20, 0, 15);
                    o.add(w2, 0.22F, -0.10F, 0.10F, 20, 0, 15);
                }
                case WOLVERINE_TRACK -> m.add(w2, -0.05F, 0.15F, -0.15F, -25, 0, 0);
                case WOLVERINE_CLIMB -> {
                    m.add(w2, 0.0F, 0.40F, -0.05F, -60, 0, 0);
                    o.add(w2, 0.0F, 0.30F, -0.05F, -50, 0, 0);
                }
                default -> {
                }
            }
        }
    }

    /** A slash's hand across the view: from the outside high to the inside low, over the first three ticks. */
    private static float slash(float t) {
        float k = Mth.clamp((t - 1.0F) / 2.0F, 0.0F, 1.0F);
        return 0.18F - 0.50F * k;
    }

    private static float slashRoll(float t) {
        float k = Mth.clamp((t - 1.0F) / 2.0F, 0.0F, 1.0F);
        return -30 + 60 * k;
    }

    // ---- the payloads: the gesture each of his moves plays ----

    /** A payload of his arrived: the gesture it stands for plays on him. */
    public static void onPayload(WolverineFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.entityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        GestureId id = WolverineGestures.gestureOf(payload.kind(), payload.arg());
        if (id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s != null) {
            HeroAnimator.start(s, id, 0.0F, player.tickCount);
        }
    }

    // ---- the flourishes ----

    /** A gesture of his began: the dust at his feet for the leaps and the landing. */
    static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        int count = switch (id) {
            case WOLVERINE_POUNCE_AIR, WOLVERINE_FERAL_LEAP -> 12;
            case WOLVERINE_POUNCE_STAB -> 18;
            default -> 0;
        };
        if (count > 0) {
            dust(level, player, count);
        }
    }

    /** Dust of the ground he stands on, kicked up round his feet. */
    static void dust(ClientLevel level, AbstractClientPlayer player, int count) {
        BlockPos below = player.getOnPos();
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) {
            return;
        }
        RandomSource r = level.random;
        double spread = player.getBbWidth();
        for (int i = 0; i < count; i++) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX()
                    + (r.nextDouble() - 0.5) * spread * 2.0, player.getY() + 0.1, player.getZ()
                    + (r.nextDouble() - 0.5) * spread * 2.0, (r.nextDouble() - 0.5) * 0.3, 0.15,
                    (r.nextDouble() - 0.5) * 0.3);
        }
    }
}
