package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronState;
import net.minecraft.util.Mth;

import static com.kolodkingal.enhanced.client.render.AnimMath.lerp;
import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * What every state of Ultron and of his Sentries looks like, as a pose that is a pure function of the state, the ticks it
 * has lasted and the clock: every client that knows the same state and the same game time poses the same tell, which is
 * why the state and its start are synced and nothing else about the animation is. The timelines are the server's own (the
 * lengths of {@link UltronAttack} and {@link UltronFight}), so an arm comes down when the attack is over and not before.
 *
 * <p>The rule the poses are drawn by is the one of the fight: <b>every blow has a tell a player can read</b>, in the body
 * first. The Volley raises both arms at the target and lights the fingertips; the Beams throw the arms wide and then
 * swing them in; the Dive crouches, rises, hangs with the arms swept back and dives with the body pitched forward; the
 * Pull holds a fist overhead and then drives it into the ground; the Call throws both arms up; the Punch draws the fist
 * back and twists the body. The glow of the fingertips says an attack is armed; the eyes say who he is looking at.
 * Pure (no Minecraft client classes): the game tests check that each tell is there before the blow.
 */
public final class UltronPoses {
    /** The colour of Prime's eyes (red) and of the later bodies' (orange-red), as 0 to 1 components. */
    private static final float[] PRIME_EYES = {0.90F, 0.20F, 0.17F};
    private static final float[] LATER_EYES = {1.00F, 0.42F, 0.16F};
    /** Stunned, staggered or sleeping machines show blue; a tell shows white. */
    private static final float[] BLUE = {0.18F, 0.50F, 1.00F};
    private static final float[] RED = {1.00F, 0.22F, 0.18F};
    private static final float[] WHITE = {1.00F, 1.00F, 1.00F};

    /** How long the first part of a transfer (the old body shudders, then the new one wakes) and a death last. */
    private static final int TRANSFER_SHUDDER = UltronFight.TRANSFER_MOVES_AT;
    private static final int TRANSFER_TOTAL = UltronFight.TRANSFER_TICKS;
    private static final int DEATH_TOTAL = UltronFight.DEATH_TICKS;

    /**
     * The pose of Ultron's body.
     *
     * @param state the state the server says he is in
     * @param t     the ticks it has lasted (with the partial tick)
     * @param body  the index of the body (0 Prime, 1 the Hybrid, 2 and after the Sentry-Ultrons)
     * @param age   a clock that never stops (the game time and the partial tick), for the drifts and the flickers
     */
    public static void boss(UltronPose out, UltronState state, float t, int body, float age) {
        out.clear();
        rest(out, age, body, false);
        float[] eyes = body == 0 ? PRIME_EYES : LATER_EYES;
        out.eyeColor(eyes[0], eyes[1], eyes[2], 1.0F);
        switch (state) {
            case IDLE -> {
            }
            case VOLLEY -> volley(out, t, body);
            case BEAMS -> beams(out, t);
            case DIVE -> dive(out, t);
            case PULL -> pull(out, t);
            case CALL -> call(out, t, age);
            case PUNCH -> punch(out, t);
            case STAGGER -> stagger(out, t, age);
            case LIFT_CHANNEL -> lift(out, t, age);
            case TRANSFER -> transfer(out, t, age);
            case DEATH -> death(out, t, age);
            case AWAKENING -> awakening(out, age);
        }
    }

    /** The pose of a Sentry (or of a hero's drone) in an act. */
    public static void sentry(UltronPose out, UltronSentryEntity.Act act, UltronSentryEntity.Eyes eyes, float t, float age, float aim) {
        out.clear();
        rest(out, age, 3, true);
        float[] colour = switch (eyes) {
            case BLUE -> BLUE;
            case RED -> RED;
            case WHITE -> WHITE;
        };
        out.eyeColor(colour[0], colour[1], colour[2], 1.0F);
        out.to(UltronPose.PALM_GLOW, 0.25F, 1.0F);
        switch (act) {
            case IDLE -> {
            }
            case SHOT -> {
                // The right arm comes up and points where the Sentry looks, the palm cannon glows cyan.
                float w = ss(0, 5, t);
                out.to(UltronPose.R_ARM_X, -88.0F + aim, w);
                out.to(UltronPose.R_ARM_Y, -3.0F, w);
                out.to(UltronPose.R_ARM_Z, 3.0F, w);
                out.to(UltronPose.R_FORE, -2.0F, w);
                out.to(UltronPose.R_FINGERS, 0.0F, w);
                out.to(UltronPose.FINGER_GLOW, 1.0F, ss(0, 8, t));
                out.to(UltronPose.PALM_GLOW, 1.0F, ss(0, 8, t));
                out.to(UltronPose.HEAD_X, 4.0F, w);
            }
            case PUNCH -> {
                float draw = ss(0, 4, t);
                float strike = ss(3.5F, 5.0F, t);
                out.to(UltronPose.R_ARM_X, lerp(strike, 50.0F, -86.0F), draw);
                out.to(UltronPose.R_ARM_Y, lerp(strike, -18.0F, 6.0F), draw);
                out.to(UltronPose.R_FORE, lerp(strike, -80.0F, -4.0F), draw);
                out.to(UltronPose.R_FINGERS, 1.0F, draw);
                out.to(UltronPose.HIPS_Y, lerp(strike, -18.0F, 10.0F), draw);
                out.to(UltronPose.FINGER_GLOW, 1.0F, draw);
            }
            case STUNNED -> {
                out.to(UltronPose.HEAD_X, 22.0F, 1.0F);
                out.arms(14.0F, 0.0F, 5.0F, -6.0F, 0.2F, 1.0F);
                out.to(UltronPose.HIPS_X, 12.0F, 1.0F);
                out.to(UltronPose.SHAKE, 3.0F, 1.0F);
                // The eyes waver blue, dim and bright (3.3: a smooth waver, not an on-off strobe).
                out.to(UltronPose.EYES, waver(age, 0.15F, 0.9F), 1.0F);
                out.to(UltronPose.PALM_GLOW, 0.1F, 1.0F);
            }
            case ASSEMBLING -> {
                float u = Mth.clamp(t / UltronSentryEntity.ASSEMBLE_TICKS, 0.0F, 1.0F);
                // Out of a heap of parts: the limbs are tucked in and come out as the body forms.
                float limbs = ss(0.4F, 1.0F, u);
                out.arms(lerp(limbs, 28.0F, 10.0F), 0.0F, lerp(limbs, 2.0F, 12.0F), lerp(limbs, -50.0F, -14.0F), 0.8F, 1.0F);
                out.to(UltronPose.HIPS_X, lerp(limbs, 25.0F, 5.0F), 1.0F);
                out.to(UltronPose.SCALE, 0.25F + 0.75F * AnimMath.easeOutBack(u), 1.0F);
                out.to(UltronPose.EYES, ss(0.7F, 0.85F, u) * waver(age, 0.3F, 0.8F), 1.0F);
                out.to(UltronPose.FLASH, 0.5F * (1.0F - u), 1.0F);
                out.to(UltronPose.PALM_GLOW, 0.0F, 1.0F);
            }
        }
    }

    // ---- the rest pose ----

    /** Hovering: arms a little out from the sides and drifting, fingers curled, legs hanging, a slow bob. */
    private static void rest(UltronPose p, float age, int body, boolean sentry) {
        float drift = Mth.sin(age * Mth.TWO_PI / 90.0F);
        float sway = Mth.sin(age * 0.05F);
        p.to(UltronPose.HIPS_X, sentry ? 5.0F : 3.0F, 1.0F);
        p.to(UltronPose.BOB, 0.03F * Mth.sin(age * 0.07F), 1.0F);
        p.to(UltronPose.R_ARM_X, 6.0F + 2.0F * drift, 1.0F);
        p.to(UltronPose.R_ARM_Z, 9.0F + 3.0F * drift, 1.0F);
        p.to(UltronPose.R_FORE, -14.0F, 1.0F);
        p.to(UltronPose.R_FINGERS, 0.55F, 1.0F);
        p.to(UltronPose.L_ARM_X, 6.0F - 2.0F * drift, 1.0F);
        p.to(UltronPose.L_ARM_Z, 9.0F - 3.0F * drift, 1.0F);
        p.to(UltronPose.L_FORE, -14.0F, 1.0F);
        p.to(UltronPose.L_FINGERS, 0.55F, 1.0F);
        p.to(UltronPose.R_LEG_X, -3.0F + 1.5F * sway, 1.0F);
        p.to(UltronPose.R_SHIN, 9.0F, 1.0F);
        p.to(UltronPose.L_LEG_X, 2.0F - 1.5F * sway, 1.0F);
        p.to(UltronPose.L_SHIN, 7.0F, 1.0F);
        p.to(UltronPose.EYES, 1.0F, 1.0F);
        p.to(UltronPose.PALM_GLOW, 0.55F, 1.0F);
        p.to(UltronPose.TORSO_GLOW, 0.65F + 0.15F * Mth.sin(age * 0.15F), 1.0F);
    }

    // ---- the six attacks ----

    /** The Repulsor Volley: both arms come up at the target and the fingertips light; a recoil at every burst. */
    private static void volley(UltronPose p, float t, int body) {
        int tell = UltronAttack.VOLLEY.tell();
        int length = UltronAttack.VOLLEY.length(body);
        float w = ss(0, 9, t) * (1.0F - ss(length - 8, length, t));
        p.arms(-86.0F, -5.0F, 7.0F, -6.0F, 0.0F, w);
        p.to(UltronPose.HEAD_X, 4.0F, w);
        p.to(UltronPose.FINGER_GLOW, 1.0F, ss(0, tell - 2, t) * (1.0F - ss(length - 8, length, t)));
        int bursts = body >= 2 ? 1 : 3;
        for (int b = 0; b < bursts; b++) {
            // Each burst kicks the arms up and the body back, and settles within four ticks.
            float kick = bump(t, tell + UltronAttack.BURST_GAP * b, 4.0F);
            p.v[UltronPose.R_ARM_X] -= 9.0F * kick;
            p.v[UltronPose.L_ARM_X] -= 9.0F * kick;
            p.v[UltronPose.HIPS_X] -= 3.0F * kick;
        }
    }

    /** The Finger Beams: the arms thrown wide for the tell, then swung in along the locked line, fingers spread. */
    private static void beams(UltronPose p, float t) {
        int tell = UltronAttack.BEAMS.tell();
        int length = UltronAttack.BEAMS.length(0);
        float spread = ss(0, 8, t);
        float swing = ss(tell, tell + 4, t);
        float release = ss(length - 6, length, t);
        float w = spread * (1.0F - release);
        p.arms(lerp(swing, -8.0F, -90.0F), lerp(swing, 0.0F, -8.0F), lerp(swing, 78.0F, 8.0F), lerp(swing, -10.0F, -4.0F), 0.0F, w);
        p.to(UltronPose.HEAD_X, -6.0F, w);
        p.to(UltronPose.HIPS_X, lerp(swing, -4.0F, 2.0F), w);
        p.to(UltronPose.FINGER_GLOW, 1.0F, ss(0, tell - 4, t) * (1.0F - release));
        p.to(UltronPose.TORSO_GLOW, 1.2F, w * 0.6F);
    }

    /** The Thruster Dive: a crouch, a rise with the arms swept back, a hang, and the plunge with the body pitched forward. */
    private static void dive(UltronPose p, float t) {
        int rise = UltronAttack.DIVE_RISE_TICKS;
        int tell = UltronAttack.DIVE.tell();
        float crouch = ss(0, 5, t) * (1.0F - ss(5, rise, t));
        float hang = ss(2, rise, t);
        float plunge = ss(tell, tell + 3, t);
        // Legs together, bent in the crouch; stretched out behind him once he hangs.
        p.to(UltronPose.HIPS_X, 12.0F, crouch);
        p.arms(lerp(hang, 22.0F, 58.0F), 0.0F, 6.0F, -6.0F, 0.3F, ss(0, 6, t));
        p.to(UltronPose.R_LEG_X, 12.0F, hang);
        p.to(UltronPose.L_LEG_X, 12.0F, hang);
        p.to(UltronPose.R_SHIN, 4.0F, hang);
        p.to(UltronPose.L_SHIN, 4.0F, hang);
        p.to(UltronPose.R_LEG_Z, 0.0F, hang);
        p.to(UltronPose.HEAD_X, 20.0F, hang);
        p.to(UltronPose.PITCH, 70.0F, plunge);
        p.to(UltronPose.PALM_GLOW, 1.0F, hang);
        p.to(UltronPose.TORSO_GLOW, 1.1F, hang * 0.6F);
    }

    /**
     * The Gravity Pull: a fist raised overhead and wrapped in the vortex, held while the ring contracts and the pull
     * drags, and then driven into the ground at {@link UltronAttack#PULL_PUNCH}.
     */
    private static void pull(UltronPose p, float t) {
        int punchAt = UltronAttack.PULL_PUNCH;
        int length = UltronAttack.PULL.length(0);
        float raise = ss(0, 10, t);
        float slam = ss(punchAt - 4, punchAt, t);
        float release = ss(length - 10, length, t);
        float w = raise * (1.0F - release);
        p.to(UltronPose.R_ARM_X, lerp(slam, -172.0F, -38.0F), w);
        p.to(UltronPose.R_ARM_Z, lerp(slam, 12.0F, 4.0F), w);
        p.to(UltronPose.R_FORE, lerp(slam, -6.0F, -4.0F), w);
        p.to(UltronPose.R_FINGERS, 1.0F, w);
        p.to(UltronPose.L_ARM_X, -48.0F, w);
        p.to(UltronPose.L_ARM_Z, -22.0F, w);
        p.to(UltronPose.L_FORE, -20.0F, w);
        p.to(UltronPose.HIPS_X, lerp(slam, -6.0F, 14.0F), w);
        p.to(UltronPose.HEAD_X, lerp(slam, -8.0F, 12.0F), w);
        p.to(UltronPose.PALM_GLOW, 1.0F, w);
        p.to(UltronPose.FINGER_GLOW, 0.8F, w * (1.0F - slam));
    }

    /** The Call of the Sentries: both arms thrown wide and up, the head back, the light in his chest pulsing. */
    private static void call(UltronPose p, float t, float age) {
        int length = UltronAttack.CALL.length(0);
        float w = ss(0, 10, t) * (1.0F - ss(length - 8, length, t));
        // Both arms thrown out and up: swung out sideways past the horizontal.
        p.arms(-8.0F, 0.0F, 145.0F, -5.0F, 0.0F, w);
        p.to(UltronPose.HEAD_X, -14.0F, w);
        p.to(UltronPose.HIPS_X, -5.0F, w);
        float pulse = 0.5F + 0.5F * Mth.sin(t * 0.9F);
        p.to(UltronPose.TORSO_GLOW, 1.0F + 0.6F * pulse, w);
        // Eyes and mouth pulse white and red (3.3: they swapped colour every other tick).
        float[] c = mix(RED, WHITE, waver(age, 0.0F, 1.0F));
        p.eyeColor(c[0], c[1], c[2], w);
        p.to(UltronPose.EYES, 1.4F, w);
        p.to(UltronPose.FLASH, 0.25F * pulse, w);
    }

    /** The Punch: the fist drawn back and the body twisted for eight ticks, then one blow, and a recovery. */
    private static void punch(UltronPose p, float t) {
        int tell = UltronAttack.PUNCH.tell();
        int length = UltronAttack.PUNCH.length(0);
        float draw = ss(0, tell, t);
        float strike = ss(tell, tell + 2, t);
        float w = ss(0, 3, t) * (1.0F - ss(length - 6, length, t));
        p.to(UltronPose.R_ARM_X, lerp(strike, 55.0F, -88.0F), w);
        p.to(UltronPose.R_ARM_Y, lerp(strike, -20.0F, 8.0F), w);
        p.to(UltronPose.R_ARM_Z, lerp(strike, 14.0F, 4.0F), w);
        p.to(UltronPose.R_FORE, lerp(strike, -85.0F, -5.0F), w);
        p.to(UltronPose.R_FINGERS, 1.0F, w);
        p.to(UltronPose.HIPS_Y, lerp(strike, -25.0F * draw, 15.0F), w);
        p.to(UltronPose.HIPS_X, lerp(strike, -2.0F, 9.0F), w);
        p.to(UltronPose.L_ARM_X, -30.0F, w * 0.6F);
        // The knuckles flash amber as the fist is drawn back.
        p.to(UltronPose.FINGER_GLOW, 1.0F, ss(tell - 5, tell - 1, t) * (1.0F - ss(tell + 2, tell + 8, t)));
    }

    // ---- the quiet states ----

    /** Grounded after the Dive: hanging limp, head down, eyes dim to blue, venting steam. */
    private static void stagger(UltronPose p, float t, float age) {
        float limp = ss(0, 4, t) * (1.0F - ss(40, 50, t));
        p.arms(8.0F, 0.0F, 4.0F, -4.0F, 0.2F, limp);
        p.to(UltronPose.HEAD_X, 25.0F, limp);
        p.to(UltronPose.HIPS_X, 18.0F, limp);
        p.to(UltronPose.R_LEG_X, 8.0F, limp);
        p.to(UltronPose.L_LEG_X, -4.0F, limp);
        p.to(UltronPose.SHAKE, 1.2F, limp);
        p.eyeColor(BLUE[0], BLUE[1], BLUE[2], limp);
        p.to(UltronPose.EYES, 0.55F, limp);
        p.to(UltronPose.PALM_GLOW, 0.15F, limp);
        p.to(UltronPose.TORSO_GLOW, 0.35F, limp);
    }

    /** Lifting Sokovia: both arms up in a V, the head back, the eyes ablaze, the whole body bathed in cyan. */
    private static void lift(UltronPose p, float t, float age) {
        float w = ss(0, 10, t);
        p.arms(-5.0F, 0.0F, 152.0F, -4.0F, 0.0F, w);
        p.to(UltronPose.HEAD_X, -18.0F, w);
        p.to(UltronPose.HIPS_X, -4.0F, w);
        p.to(UltronPose.EYES, 1.6F, w);
        p.eyeColor(1.0F, 0.85F, 0.7F, w);
        p.to(UltronPose.BATH, 0.55F + 0.2F * Mth.sin(age * 0.2F), w);
        p.to(UltronPose.PALM_GLOW, 1.0F, w);
        p.to(UltronPose.TORSO_GLOW, 1.3F, w);
        p.to(UltronPose.SHAKE, 0.6F, w);
    }

    /**
     * Changing bodies. For the first {@link UltronFight#TRANSFER_MOVES_AT} ticks the old body shudders, slumps, the plates
     * shift outward and the eyes flicker red and white and fade; then he is in the new body (smaller, with a different
     * form) which sits slumped, its plates closing, its eyes dark, until it wakes at the end.
     */
    private static void transfer(UltronPose p, float t, float age) {
        float slump;
        float open;
        float eyes;
        float shake;
        if (t < TRANSFER_SHUDDER) {
            float u = t / TRANSFER_SHUDDER;
            slump = ss(0, 1, u);
            open = ss(0, 0.75F, u);
            eyes = (1.0F - ss(0.2F, 1.0F, u)) * waver(age, 0.2F, 1.0F);
            shake = 1.0F + 5.0F * u;
        } else {
            float u = (t - TRANSFER_SHUDDER) / (TRANSFER_TOTAL - TRANSFER_SHUDDER);
            slump = 1.0F - ss(0.15F, 0.95F, u);
            open = 1.0F - ss(0.0F, 0.8F, u);
            // Dark until the last third, then wavering on.
            float wake = ss(0.6F, 1.0F, u);
            eyes = wake * Mth.lerp(ss(0.8F, 0.95F, wake), waver(age, 0.2F, 1.0F), 1.0F);
            shake = 3.0F * (1.0F - u);
        }
        slump(p, slump);
        p.to(UltronPose.OPEN, open, 1.0F);
        p.to(UltronPose.SHAKE, shake, 1.0F);
        p.to(UltronPose.EYES, eyes, 1.0F);
        float[] c = mix(RED, WHITE, waver(age, 0.0F, 1.0F));
        p.eyeColor(c[0], c[1], c[2], t < TRANSFER_SHUDDER ? 1.0F : 0.0F);
        p.to(UltronPose.TORSO_GLOW, 0.3F + 0.7F * eyes, 1.0F);
        p.to(UltronPose.PALM_GLOW, 0.15F, slump);
        p.to(UltronPose.FLASH, t < TRANSFER_SHUDDER ? 0.25F * Math.max(0.0F, Mth.sin(age * 1.9F)) : 0.0F, 1.0F);
    }

    /** The last body: the plates fall open, he sags, the eyes flicker red and white and fade to dark, a slow flash from the chest. */
    private static void death(UltronPose p, float t, float age) {
        float u = Mth.clamp(t / DEATH_TOTAL, 0.0F, 1.0F);
        slump(p, ss(0, 0.65F, u));
        p.to(UltronPose.OPEN, ss(0, 0.6F, u), 1.0F);
        float dying = ss(0.15F, 0.7F, u);
        p.to(UltronPose.EYES, (1.0F - dying) * Mth.lerp(ss(0.1F, 0.2F, u), 1.0F, waver(age, 0.25F, 1.0F)), 1.0F);
        float[] c = mix(RED, WHITE, waver(age, 0.0F, 1.0F));
        p.eyeColor(c[0], c[1], c[2], dying);
        p.to(UltronPose.SHAKE, 4.0F * (1.0F - ss(0.5F, 0.8F, u)), 1.0F);
        p.to(UltronPose.TORSO_GLOW, 1.0F - ss(0.0F, 0.8F, u), 1.0F);
        p.to(UltronPose.PALM_GLOW, 0.55F * (1.0F - ss(0.0F, 0.5F, u)), 1.0F);
        // The slow blue-white flash from the chest.
        p.to(UltronPose.FLASH, 0.45F * ss(0.4F, 1.0F, u), 1.0F);
    }

    /** Being assembled by the ritual: hanging slack, eyes dark, the light in his chest barely there. */
    private static void awakening(UltronPose p, float age) {
        p.arms(10.0F, 0.0F, 6.0F, -6.0F, 0.3F, 1.0F);
        p.to(UltronPose.HEAD_X, 14.0F, 1.0F);
        p.to(UltronPose.HIPS_X, 8.0F, 1.0F);
        p.to(UltronPose.EYES, 0.0F, 1.0F);
        p.to(UltronPose.TORSO_GLOW, 0.3F + 0.1F * Mth.sin(age * 0.2F), 1.0F);
        p.to(UltronPose.PALM_GLOW, 0.2F, 1.0F);
    }

    /**
     * A light that wavers between {@code low} and {@code high} about twice a second (3.3): the flickers and colour
     * swaps
     * of the eyes were on-off steps several times a second, a strobe.
     */
    private static float waver(float age, float low, float high) {
        return Mth.lerp(0.5F + 0.5F * Mth.sin(age * 0.6F), low, high);
    }

    /** The colour {@code f} of the way from {@code a} to {@code b}. */
    private static float[] mix(float[] a, float[] b, float f) {
        return new float[]{Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2])};
    }

    /** Hanging dead weight: the head down, the arms limp, the legs loose (the weight is how far, 0 to 1). */
    private static void slump(UltronPose p, float w) {
        p.arms(12.0F, 0.0F, 5.0F, -6.0F, 0.15F, w);
        p.to(UltronPose.HEAD_X, 36.0F, w);
        p.to(UltronPose.HIPS_X, 22.0F, w);
        p.to(UltronPose.R_LEG_X, 9.0F, w);
        p.to(UltronPose.L_LEG_X, -5.0F, w);
        p.to(UltronPose.R_SHIN, 12.0F, w);
        p.to(UltronPose.L_SHIN, 4.0F, w);
    }

    /** A bump that rises at tick {@code at} and falls to nothing over {@code length} ticks (1 at its peak). */
    private static float bump(float t, float at, float length) {
        if (t < at - 1.0F || t > at + length) {
            return 0.0F;
        }
        float up = ss(at - 1.0F, at, t);
        float down = 1.0F - ss(at, at + length, t);
        return up * down;
    }

    private UltronPoses() {
    }
}
