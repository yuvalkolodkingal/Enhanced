package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.captain.ShieldRules;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
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
 * How Captain America acts what he does (3.8): his guard behind the raised Vibranium Shield (a stance: a little crouch,
 * leaning in; the arm is vanilla's blocking pose), what his gestures add to his hands in first person, the gestures his
 * moves' payloads start (each names him: the throw and the catch, the slam's hop and landing, the leap, the charge, the
 * bash, Assemble, Inspire, the punches, the kick, the flip), and the dust of his leaps and landings.
 */
public final class CaptainActing {
    private CaptainActing() {
    }

    // ---- the stance ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        on[Stance.CAP_GUARD.ordinal()] = p.isUsingItem() && ShieldRules.isVibranium(p.getUseItem())
                && ShieldRules.isCap(p);
    }

    /** The guard's head: a little down behind the rim. */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.CAP_GUARD);
        if (w > 0.001F) {
            c.head(10, 0, 0, w);
        }
    }

    static float lean(HeroAnimState st) {
        return CaptainGestures.GUARD_LEAN * st.stance(Stance.CAP_GUARD);
    }

    static float drop(HeroAnimState st) {
        return CaptainGestures.GUARD_DROP * st.stance(Stance.CAP_GUARD);
    }

    // ---- first person ----

    /** What his gestures add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case CAP_THROW -> m.add(w, throwX(t), 0.12F, -0.10F, -20, throwYaw(t), 0);
                case CAP_THROW_OFF -> o.add(w, throwX(t), 0.12F, -0.10F, -20, throwYaw(t), 0);
                case CAP_CATCH -> o.add(w, 0.0F, 0.10F * give(t), -0.12F * give(t), -10 * give(t), 0, 0);
                case CAP_CATCH_MAIN -> m.add(w, 0.0F, 0.10F * give(t), -0.12F * give(t), -10 * give(t), 0, 0);
                case CAP_PUNCH_1 -> m.add(w, -0.06F, 0.06F, -0.30F * strike(t, 3.0F), -12, 0, 0);
                case CAP_PUNCH_2 -> o.add(w, 0.10F * strike(t, 3.0F), 0.06F, -0.24F * strike(t, 3.0F), -12, 0,
                        -18 * strike(t, 3.0F));
                case CAP_BASH -> o.add(w, -0.10F, 0.10F, -0.34F * strike(t, 4.0F), -8, 0, 0);
                case CAP_CHARGE -> {
                    o.add(w, -0.18F, 0.14F, -0.10F, -10, 0, 0);
                    m.add(w, 0.0F, -0.12F, 0.05F, 10, 0, 0);
                }
                case CAP_SLAM_LEAP -> {
                    m.add(w, 0.0F, 0.30F, 0.0F, -30, 0, 0);
                    o.add(w, 0.0F, 0.30F, 0.0F, -30, 0, 0);
                }
                case CAP_SLAM_DOWN -> {
                    float down = t < 2.0F ? t / 2.0F : 1.0F;
                    m.add(w, 0.0F, 0.30F - 0.55F * down, -0.10F, -30 + 60 * down, 0, 0);
                    o.add(w, 0.0F, 0.30F - 0.55F * down, -0.10F, -30 + 60 * down, 0, 0);
                }
                case CAP_ASSEMBLE -> {
                    o.add(w, 0.0F, 0.42F, -0.05F, -40, 0, 0);
                    m.add(w, 0.0F, 0.12F + 0.05F * Mth.sin(t * 0.9F), -0.10F, -20, 0, 10 * Mth.sin(t * 0.9F));
                }
                case CAP_INSPIRE -> {
                    m.add(w, 0.18F, 0.04F, 0.05F, 0, 0, 18);
                    o.add(w, 0.18F, 0.04F, 0.05F, 0, 0, 18);
                }
                case CAP_KICK, CAP_FLIP_KICK -> {
                    m.add(w, 0.10F, -0.10F, 0.10F, 20, 0, 10);
                    o.add(w, 0.10F, -0.10F, 0.10F, 20, 0, 10);
                }
                case CAP_LEAP -> o.add(w, 0.0F, 0.18F, -0.18F, -30, 0, 0);
                default -> {
                }
            }
        }
    }

    /** The throw's hand across the view: in to the chest (tick 4), then out flat (tick 6). */
    private static float throwX(float t) {
        if (t < 4.0F) {
            return -0.30F * (t / 4.0F);
        }
        float k = Mth.clamp((t - 4.0F) / 2.0F, 0.0F, 1.0F);
        return -0.30F + 0.60F * k;
    }

    private static float throwYaw(float t) {
        return t < 4.0F ? 30 * (t / 4.0F) : 30 - 70 * Mth.clamp((t - 4.0F) / 2.0F, 0.0F, 1.0F);
    }

    /** The catch's give: a jolt back as the shield lands, then settling. */
    private static float give(float t) {
        return t < 2.0F ? t / 2.0F : Math.max(0.0F, 1.0F - (t - 2.0F) / 8.0F);
    }

    /** A blow's reach: out by tick {@code at}, back by the end. */
    private static float strike(float t, float at) {
        return t < at ? t / at : Math.max(0.0F, 1.0F - (t - at) / 5.0F);
    }

    // ---- the payloads: the gesture each of his moves plays ----

    /** A payload of his arrived: the gesture it stands for plays on him. */
    public static void onPayload(CaptainFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.entityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        GestureId id = CaptainGestures.gestureOf(payload.kind(), payload.arg());
        if (id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s != null) {
            HeroAnimator.start(s, id, 0.0F, player.tickCount);
        }
    }

    // ---- the flourishes ----

    /** A gesture of his began: the dust at his feet for the leaps and the slam's landing. */
    static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        int count = switch (id) {
            case CAP_LEAP, CAP_SLAM_LEAP -> 10;
            case CAP_CHARGE -> 8;
            default -> 0;
        };
        if (count > 0) {
            dust(level, player, count);
        }
    }

    /** Dust of the ground he stands on, kicked up round his feet. */
    public static void dust(ClientLevel level, AbstractClientPlayer player, int count) {
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
