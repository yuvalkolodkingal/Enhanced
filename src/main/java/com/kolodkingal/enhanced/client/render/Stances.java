package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.Gestures.Target;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.util.Mth;

/**
 * The pose rows of the stances ({@link Stance}): ambient ones first, then the aimed and holding ones, each blended into
 * the model by its eased weight. Rows are written for a right-handed player (see {@link PoseContext}); {@code AIM}
 * rows point an arm in three dimensions at a target, so a hand still points where the hero looks on a leaning body.
 *
 * <p>Besides the arms a stance can add a lean, a dip and a twist to the whole body; those are summed by
 * {@link #lean}, {@link #drop} and applied by the frame, not here.
 */
final class Stances {
    private static final float RAD = Mth.DEG_TO_RAD;

    /** Applies every ambient stance. */
    static void ambient(PoseContext c) {
        HeroAnimState st = c.st;
        float w;
        if ((w = st.stance(Stance.MAG_FIELD)) > 0.001F) {
            // Palms down, the field around him; a slow drift even on the ground.
            pose(c, StancePoses.MAG_FIELD_M, StancePoses.MAG_FIELD_O, w);
            drift(c, 2.5F, w, true);
        }
        if ((w = st.stance(Stance.VISION_PHASED)) > 0.001F) {
            // Serene and weightless: open palms a little forward.
            boolean white = "white_vision".equals(st.look);
            float sway = white ? 0.0F : 2.9F * Mth.sin(0.05F * c.age);
            float[] m = StancePoses.VISION_PHASED_M;
            float[] o = StancePoses.VISION_PHASED_O;
            c.arm(true, m[0] + sway, m[1], m[2], w);
            c.arm(false, o[0] + (white ? 0.0F : 2.9F * Mth.cos(0.05F * c.age)), o[1], o[2], w);
        }
        if ((w = st.stance(Stance.VISION_DENSE)) > 0.001F) {
            // Heavy: arms braced a little out, a wide stance, the head up.
            pose(c, StancePoses.VISION_DENSE_M, StancePoses.VISION_DENSE_O, w);
            float ground = w * (1.0F - st.wAir);
            c.leg(true, 0, 4, ground);
            c.leg(false, 0, -4, ground);
            c.head(2, 0, 0, w);
        }
        if ((w = st.stance(Stance.LOKI_FROST)) > 0.001F) {
            // The Jotun stance: arms a little out, legs a little wider.
            c.armAdd(true, 0, 0, StancePoses.LOKI_FROST_Z, w);
            c.armAdd(false, 0, 0, -StancePoses.LOKI_FROST_Z, w);
            float ground = w * (1.0F - st.wAir);
            c.legAdd(true, 0, 3, ground);
            c.legAdd(false, 0, -3, ground);
        }
        if ((w = st.stance(Stance.THOR_UNLEASHED)) > 0.001F) {
            // Arms held out, crackling; in flight the weapon arm keeps its world pose.
            c.armAdd(true, 0, 0, StancePoses.THOR_UNLEASHED_Z, w * (1.0F - st.wAir));
            c.armAdd(false, 0, 0, -StancePoses.THOR_UNLEASHED_Z, w);
            c.tremble(true, 1.7F, 1.9F, w * (1.0F - st.wAir));
            c.tremble(false, 1.7F, 1.7F, w);
            c.head(-4, 0, 0, w);
        }
        if ((w = st.stance(Stance.NOISE)) > 0.001F) {
            // A head tilted as if listening.
            c.head(0, 0, 5 + 1.7F * Mth.sin(0.05F * c.age), w);
        }
        if ((w = st.stance(Stance.WANDA_READY)) > 0.001F) {
            // Hands up, open and low, rolling at the wrists.
            c.arm(true, -35, 0, 28, w);
            c.arm(false, -35, 0, -28, w);
            c.tremble(true, 4, 0.5F, w);
            c.tremble(false, 4, 0.5F, w);
        }
        if ((w = st.stance(Stance.WANDA_HEX_CONTROL)) > 0.001F) {
            c.arm(true, -100, -15, 4, w);
            c.arm(false, -20, 0, -20, w);
            c.tremble(true, 6, 0.5F, w);
        }
        if ((w = st.stance(Stance.THOR_BIFROST)) > 0.001F) {
            // The weapon raised to the sky and the other palm up, as the light gathers.
            if (st.weaponKind > 0) {
                c.armWorld(true, 170, 0, 0, w);
            } else {
                c.arm(true, -150, 0, 10, w);
            }
            c.arm(false, -55, 0, -55, w);
            c.head(-18, 0, 0, w);
        }
        // The heroes of 3.0, last.
        HeroClient hero = HeroClients.of(st.power);
        if (hero != null) {
            hero.ambient(c);
        }
    }

    /** Applies the aimed and holding stances: later rows win where they overlap. */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w;
        if ((w = st.stance(Stance.THOR_WAIT)) > 0.001F) {
            // A hand out, waiting for the weapon to come home.
            c.aim(true, Target.WEAPON, 0, 0, 0, w);
            c.tremble(true, 1.1F, 1.7F, w);
            c.armAdd(false, 0, 0, -9, w);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.TK_HOLD)) > 0.001F) {
            telekinesis(c, st.power, w);
        }
        if ((w = st.stance(Stance.TK_STRAIN)) > 0.001F) {
            // Both arms at the thing, trembling with the effort.
            c.aim(true, Target.HELD, -6, -10, 0, w);
            c.aim(false, Target.HELD, -6, 10, 0, w);
            c.tremble(true, 3.4F, 1.9F, w);
            c.tremble(false, 3.4F, 2.1F, w);
            c.head(-6, 0, 0, w);
            c.leg(true, -8, 0, w * (1.0F - st.wAir));
            c.leg(false, 8, 0, w * (1.0F - st.wAir));
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.MAG_SHRAPNEL)) > 0.001F) {
            float pulse = Math.max(0.0F, Mth.cos(Mth.TWO_PI * c.age / 3.0F));
            pulse *= pulse;
            pulse *= pulse;
            c.aim(true, Target.LOOK, 0, -2, 0, w);
            c.tremble(true, 1.7F, 1.7F, w);
            c.armAdd(true, -3.5F * pulse, 0, 0, w);
            c.armAdd(false, 0, 0, -9, w);
            c.head(-3, 0, 0, w);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.MAG_CATCH)) > 0.001F) {
            c.aim(true, Target.PROJECTILE, 0, 0, 0, w);
            c.armAdd(false, 0, 0, -9, w);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.BILLY_FREEZE)) > 0.001F) {
            // Both hands raised toward the creatures he holds, trembling with the effort.
            float look = 0.5F * c.lookPitch / RAD;
            float[] m = StancePoses.BILLY_FREEZE_M;
            float[] o = StancePoses.BILLY_FREEZE_O;
            c.arm(true, m[0] + look, m[1], m[2], w);
            c.arm(false, o[0] + look, o[1], o[2], w);
            c.tremble(true, 3.4F, 1.9F, w);
            c.tremble(false, 3.4F, 2.3F, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.VISION_BEAM)) > 0.001F) {
            // The stone fires from his forehead: chest out, chin up, the arms pulled back.
            if ("white_vision".equals(st.look)) {
                c.arm(true, 30, 0, 8, w);
                c.arm(false, 30, 0, -8, w);
                c.head(-14, 0, 0, w);
            } else {
                c.arm(true, 23, 0, 20, w);
                c.arm(false, 23, 0, -20, w);
                c.tremble(true, 1.1F, 1.7F, w);
                c.tremble(false, 1.1F, 1.7F, w);
                c.head(-10, 0, 0, w);
            }
        }
        if ((w = st.stance(Stance.BEAM_WANDA)) > 0.001F) {
            // Both hands thrust out along the look, converging a little, trembling.
            float[] m = StancePoses.BEAM_WANDA_M_AIM;
            float[] o = StancePoses.BEAM_WANDA_O_AIM;
            c.aim(true, Target.LOOK, m[0], m[1], m[2], w);
            c.aim(false, Target.LOOK, o[0], o[1], o[2], w);
            c.tremble(true, 2.9F, 1.7F, w);
            c.tremble(false, 2.9F, 1.7F, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.SHIELD_WANDA)) > 0.001F) {
            pose(c, StancePoses.SHIELD_WANDA_M, StancePoses.SHIELD_WANDA_O, w);
            c.tremble(true, 2.9F, 0.3F, w);
            c.tremble(false, 2.9F, 0.3F, w);
            c.head(StancePoses.SHIELD_WANDA_HEAD[0], 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.BARRIER_BILLY)) > 0.001F) {
            // The plate held up on the left forearm, the body turned behind it, the feet planted wide.
            pose(c, StancePoses.BARRIER_BILLY_M, StancePoses.BARRIER_BILLY_O, w);
            c.tremble(false, 1.2F, 0.4F, w);
            c.twist(StancePoses.BARRIER_BILLY_TWIST, w);
            c.leg(true, -10, 4, w * (1.0F - st.wAir));
            c.leg(false, 10, -4, w * (1.0F - st.wAir));
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.BEAM_HAND)) > 0.001F) {
            // A continuous beam: the hand it leaves from stretched out along the look and trembling a little, the other arm
            // braced back and out, the feet planted.
            boolean main = !st.beamOffHand;
            c.aim(main, Target.LOOK, 0, main ? -3 : 3, 0, w);
            c.tremble(main, 1.4F, 2.3F, w);
            c.arm(!main, 14, main ? -10 : 10, main ? -22 : 22, w);
            c.leg(true, -10, 2, w * (1.0F - st.wAir));
            c.leg(false, 10, -2, w * (1.0F - st.wAir));
            c.occupy(main, w);
        }
        if ((w = st.stance(Stance.THOR_BEAM)) > 0.001F) {
            float pw = HeroAnimator.beamPower(st);
            if (st.weaponKind > 0) {
                // One-handed: the weapon extends the arm, its head the muzzle; the other arm braced back and out.
                c.aim(true, Target.LOOK, 0, 0, 0, w);
                c.tremble(true, 2.0F * pw / 1.7F, 2.1F, w);
                c.arm(false, 12, -10, -24, w);
                c.leg(true, -12, 2, w * (1.0F - st.wAir));
                c.leg(false, 12, -2, w * (1.0F - st.wAir));
                c.twist(StancePoses.THOR_BEAM_TWIST, w);
            } else {
                // Two-handed, like Wanda's beam.
                c.aim(true, Target.LOOK, 0, -8, 0, w);
                c.aim(false, Target.LOOK, 0, 8, 0, w);
                c.tremble(true, 1.1F, 2.1F, w);
                c.tremble(false, 1.1F, 2.1F, w);
                c.occupy(false, w);
            }
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.TOMMY_WHIRL)) > 0.001F) {
            // Arms out, spinning in the eye of the storm; the legs run (RunPoses).
            c.arm(true, -5, 0, 75, w);
            c.arm(false, -5, 0, -75, w);
            c.tremble(true, 5.7F, 0.8F, w);
            c.tremble(false, 5.7F, 0.8F, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        // The heroes of 3.0, last.
        HeroClient hero = HeroClients.of(st.power);
        if (hero != null) {
            hero.aimed(c);
        }
    }

    /** The hand that holds something: a style per hero. */
    private static void telekinesis(PoseContext c, Power power, float w) {
        switch (power) {
            case MAGNETO -> {
                // A steady outstretched hand: the coin, the chess piece, the car.
                c.aim(true, Target.HELD, 0, 0, 0, w);
                c.armAdd(false, 0, 0, -9, w);
            }
            case BILLY -> {
                // One arm straight out, palm out: stopping the bullet, stopping Tommy.
                c.aim(true, Target.HELD, -4, -4, 0, w);
                c.tremble(true, 1.7F, 0.9F, w);
                c.arm(false, -6, 0, -20, w);
            }
            default -> {
                // The raised, shaking claw of the films.
                c.aim(true, Target.HELD, -20, -6, 0, w);
                c.tremble(true, 2.9F, 0.9F, w);
                c.armAdd(false, 0, 0, -17, w);
            }
        }
        c.occupy(true, w);
    }

    /** Both arms at a pose of {@link StancePoses}, by weight {@code w}. */
    private static void pose(PoseContext c, float[] m, float[] o, float w) {
        c.arm(true, m[0], m[1], m[2], w);
        c.arm(false, o[0], o[1], o[2], w);
    }

    /** The slow drift of arms floating on air: a sine on each arm's x and z, a different phase for the off arm. */
    static void drift(PoseContext c, float amp, float w, boolean both) {
        if (amp <= 0.0F || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        c.armAdd(true, amp * Mth.sin(0.07F * c.age), 0, amp * Mth.sin(0.09F * c.age), w);
        if (both) {
            c.armAdd(false, amp * Mth.sin(0.07F * c.age + 1.7F), 0, amp * Mth.sin(0.09F * c.age + 1.7F), w);
        }
    }

    /** The lean (degrees) the stances add to the body: a sum of {@code lean * weight}. */
    static float lean(HeroAnimState st) {
        float lean = 0.0F;
        lean += -2.0F * st.stance(Stance.MAG_SHRAPNEL);
        lean += -5.0F * st.stance(Stance.TK_STRAIN);
        lean += StancePoses.SHIELD_WANDA_LEAN * st.stance(Stance.SHIELD_WANDA);
        lean += ("white_vision".equals(st.look) ? -6.0F : -3.0F) * st.stance(Stance.VISION_BEAM);
        lean += (st.weaponKind > 0 ? 6.0F : 4.0F) * st.stance(Stance.THOR_BEAM);
        lean += 10.0F * st.stance(Stance.TOMMY_WHIRL);
        lean += 2.0F * st.stance(Stance.NOISE);
        lean += HeroClients.lean(st);
        return lean;
    }

    /** The dip (blocks, positive sinks) the stances add. */
    static float drop(HeroAnimState st) {
        return 0.02F * st.stance(Stance.TK_STRAIN) + 0.03F * st.stance(Stance.VISION_DENSE)
                + 0.03F * HeroAnimator.beamPower(st) * st.stance(Stance.THOR_BEAM) + HeroClients.drop(st);
    }

    private Stances() {
    }
}
