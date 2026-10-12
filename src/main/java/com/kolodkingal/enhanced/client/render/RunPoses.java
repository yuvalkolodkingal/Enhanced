package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;

import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * How the heroes run and walk.
 * <ul>
 *     <li>The speedsters (Quicksilver in Quicksilver Time or Super Speed, Tommy) get their own run cycle with its own
 *     phase and amplitude, one frequency for arms and legs: vanilla's walk phase advances by at most a unit per tick, so at
 *     super speed the arms and legs would drift apart.</li>
 *     <li>Everyone sprints and walks with a character of their own: Magneto regal, Vision composed, White Vision stiff,
 *     Billy light and bouncy, Thor heavy. That scales the vanilla swing, which keeps the feet matching the ground.</li>
 * </ul>
 * The pose is a pure function of the run phase and amplitude, so it is as smooth as they are.
 */
final class RunPoses {
    /** The three run styles. */
    enum Style {
        /** Quicksilver in Quicksilver Time: the loose X-Men jog whatever his real speed. */
        QS_TIME(14, 50, 10, 32, -8, 6, 5, 0.05F, 2.5F, 12),
        /** Quicksilver in Super Speed: the real sprint. */
        QS_SPEED(22, 62, 16, 48, -14, 8, 7, 0.05F, 2.5F, 12),
        /** Tommy: Pietro's aggressive lean with the arms pumping, a blur. */
        TOMMY(28, 66, 20, 58, -22, 12, 9, 0.06F, 4.0F, 25);

        final float lean;
        final float legs;
        final float kick;
        final float arms;
        final float base;
        final float cross;
        final float twist;
        final float bob;
        final float kTurn;
        final float maxRoll;

        Style(float lean, float legs, float kick, float arms, float base, float cross, float twist, float bob, float kTurn, float maxRoll) {
            this.lean = lean;
            this.legs = legs;
            this.kick = kick;
            this.arms = arms;
            this.base = base;
            this.cross = cross;
            this.twist = twist;
            this.bob = bob;
            this.kTurn = kTurn;
            this.maxRoll = maxRoll;
        }

        /** The cadence (radians per tick) at a horizontal speed {@code v} (blocks per tick). */
        float omega(float v) {
            return switch (this) {
                case QS_TIME -> 0.40F * ss(0.04F, 0.25F, v);
                case QS_SPEED -> (0.55F + 0.35F * ss(0.3F, 1.2F, v)) * ss(0.04F, 0.25F, v);
                case TOMMY -> (0.70F + 0.50F * ss(0.3F, 1.4F, v)) * ss(0.04F, 0.20F, v);
            };
        }
    }

    /** The character of a hero's walk and sprint (see the table of the animation design). */
    private record Character(float walkArms, float sprintArms, float sprintLegs, float lean, float twist, float bob) {
    }

    private static final Character DEFAULT = new Character(1.0F, 1.20F, 1.10F, 6, 4, 0.015F);
    private static final Character MAGNETO = new Character(0.85F, 1.0F, 1.0F, 4, 3, 0.015F);
    private static final Character WANDA = new Character(0.80F, 1.10F, 1.05F, 5, 4, 0.015F);
    private static final Character VISION = new Character(0.60F, 0.80F, 1.0F, 3, 2, 0.010F);
    private static final Character WHITE_VISION = new Character(0.30F, 0.35F, 1.0F, 0, 0, 0.0F);
    private static final Character BILLY = new Character(1.0F, 1.20F, 1.10F, 6, 4, 0.025F);
    private static final Character LOKI = new Character(0.75F, 1.15F, 1.10F, 7, 5, 0.015F);
    private static final Character THOR = new Character(1.15F, 1.30F, 1.15F, 10, 6, 0.030F);

    /** Ultron: a stiff, deliberate march, the arms hardly swinging, no lean, almost no bob. */
    private static final Character ULTRON = new Character(0.55F, 0.70F, 1.0F, 2, 2, 0.008F);

    private static Character character(Power power, boolean whiteVision) {
        return switch (power) {
            case MAGNETO -> MAGNETO;
            case SCARLET_WITCH -> WANDA;
            case VISION -> whiteVision ? WHITE_VISION : VISION;
            case BILLY -> BILLY;
            case LOKI -> LOKI;
            case THOR -> THOR;
            // The heroes of 3.0, one line each (each lane's own): they walk and sprint as anyone does until their lane
            // gives them a character.
            case PHOENIX -> DEFAULT;
            case SPIDER_MAN -> DEFAULT;
            case THANOS -> DEFAULT;
            case ULTRON -> ULTRON;
            case APOCALYPSE -> DEFAULT;
            default -> DEFAULT;
        };
    }

    /** The run style of a speedster, or null when the hero has none. */
    static Style style(HeroAnimState st) {
        return switch (st.styleKind) {
            case 1 -> Style.QS_TIME;
            case 2 -> Style.QS_SPEED;
            case 3 -> Style.TOMMY;
            default -> null;
        };
    }

    /** The lean (degrees) a sprint adds on the ground. */
    static float sprintLean(HeroAnimState st) {
        return character(st.power, "white_vision".equals(st.look)).lean * st.wSprint * ss(0.05F, 0.30F, st.speed);
    }

    /** The lean (degrees) of the speedster's run, a little less when running up a wall. */
    static float runLean(HeroAnimState st, Style style) {
        float lean = style.lean - (st.wallRunning ? 6.0F : 0.0F);
        return lean * ss(0.04F, 0.35F, st.speed);
    }

    /** The bob (blocks) of a run or a sprint: highest with the legs spread. */
    static float bob(HeroAnimState st) {
        float bob = 0.0F;
        Style style = style(st);
        if (style != null && st.wRun > 0.0F) {
            bob += st.wRun * style.bob * (Math.abs(Mth.cos(st.runPhase)) - 0.5F) * st.runAmp;
        }
        if (st.wSprint > 0.0F) {
            Character ch = character(st.power, "white_vision".equals(st.look));
            bob += ch.bob * (Math.abs(Mth.cos(st.limbSwing * 0.6662F)) - 0.5F) * Math.min(1.0F, st.limbAmount) * st.wSprint;
        }
        return bob;
    }

    /** Advances the speedster's phase, cadence and amplitude for a frame of {@code dt} ticks at speed {@code v}. */
    static void advance(HeroAnimState st, float dt, boolean snap) {
        Style style = style(st);
        float v = st.speed;
        float omegaTarget = style == null ? 0.0F : style.omega(v);
        float ampTarget = style == null ? 0.0F : ss(0.04F, 0.30F, v);
        if (st.stance(Stance.TOMMY_WHIRL) > 0.001F) {
            // The whirlwind: a fast cycle in place for the legs.
            float w = st.stance(Stance.TOMMY_WHIRL);
            omegaTarget = AnimMath.lerp(w, omegaTarget, 0.95F);
            ampTarget = AnimMath.lerp(w, ampTarget, 0.8F);
        }
        st.runOmega = snap ? omegaTarget : AnimMath.ease(st.runOmega, omegaTarget, dt, 3.0F);
        st.runPhase += st.runOmega * dt;
        if (st.runPhase > 1.0E4F) {
            st.runPhase -= 1.0E4F;
        }
        st.runAmp = snap ? ampTarget : AnimMath.ease(st.runAmp, ampTarget, dt, 3.0F);
    }

    /** Applies the speedster's run cycle and the character of walking and sprinting. */
    static void apply(PoseContext c) {
        HeroAnimState st = c.st;
        PlayerModel<?> model = c.model;
        Style style = style(st);
        float wRun = Math.max(st.wRun, st.stance(Stance.TOMMY_WHIRL));
        if (style == null && st.stance(Stance.TOMMY_WHIRL) > 0.001F) {
            style = Style.TOMMY;
        }
        if (style != null && wRun > 0.001F && st.runAmp > 0.001F) {
            boolean legsOnly = st.stance(Stance.TOMMY_WHIRL) > 0.5F;
            float legScale = (st.wallRunning ? 1.2F : 1.0F);
            cycle(c, style, st.runPhase, st.runAmp, wRun * (1.0F - st.wAir), legsOnly, legScale);
            c.twist(-style.twist * Mth.cos(st.runPhase) * st.runAmp, wRun * (1.0F - st.wAir));
            if (st.walkman && st.slowMo && st.power == Power.QUICKSILVER) {
                model.head.xRot += 0.05F * Mth.sin(0.26F * c.age) * wRun;
            }
        }
        // Walk and sprint character: scales the vanilla swing that is already in the arms and legs.
        float free = (1.0F - st.wAir) * (1.0F - wRun);
        if (free > 0.001F) {
            Character ch = character(st.power, "white_vision".equals(st.look));
            float k = AnimMath.lerp(st.wSprint, ch.walkArms, ch.sprintArms);
            float swing = st.limbSwing * 0.6662F;
            float amount = st.limbAmount;
            float rightWalk = Mth.cos(swing + Mth.PI) * amount;
            float leftWalk = Mth.cos(swing) * amount;
            model.rightArm.xRot += (k - 1.0F) * share(model.rightArmPose) * rightWalk * free;
            model.leftArm.xRot += (k - 1.0F) * share(model.leftArmPose) * leftWalk * free;
            float kLegs = AnimMath.lerp(st.wSprint, 1.0F, ch.sprintLegs);
            model.rightLeg.xRot += (kLegs - 1.0F) * Mth.cos(swing) * 1.4F * amount * free;
            model.leftLeg.xRot += (kLegs - 1.0F) * Mth.cos(swing + Mth.PI) * 1.4F * amount * free;
            if (st.wSprint > 0.001F) {
                c.twist(-ch.twist * Mth.cos(swing) * Math.min(1.0F, amount), st.wSprint * free);
            }
        }
    }

    /** One pose of the run cycle, blended into the model by {@code w}. */
    private static void cycle(PoseContext c, Style style, float phase, float amp, float w, boolean legsOnly, float legScale) {
        float cos = Mth.cos(phase);
        float kickR = Math.max(0.0F, cos);
        float kickL = Math.max(0.0F, -cos);
        float rightLeg = (style.legs * legScale * cos + style.kick * kickR * kickR) * amp;
        float leftLeg = (-style.legs * legScale * cos + style.kick * kickL * kickL) * amp;
        c.physLegX(true, rightLeg, w);
        c.physLegX(false, leftLeg, w);
        if (legsOnly) {
            return;
        }
        float rightArm = style.base * amp - style.arms * cos * amp;
        float leftArm = style.base * amp + style.arms * cos * amp;
        c.physArm(true, rightArm, -style.cross * kickR * amp, 3.0F, w);
        c.physArm(false, leftArm, style.cross * kickL * amp, -3.0F, w);
    }

    /**
     * Poses a model as a speedster's ghost at a moment of the stride: the six parts from the run cycle at {@code phase},
     * with the sleeves, trousers, jacket and hat copied. Used by the trail ghosts, which draw the model themselves.
     */
    static void ghost(PlayerModel<?> model, Style style, float phase, float amp) {
        float cos = Mth.cos(phase);
        float kickR = Math.max(0.0F, cos);
        float kickL = Math.max(0.0F, -cos);
        model.head.xRot = 0.0F;
        model.head.yRot = 0.0F;
        model.head.zRot = 0.0F;
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;
        model.rightArm.x = -5.0F;
        model.leftArm.x = 5.0F;
        model.rightArm.z = 0.0F;
        model.leftArm.z = 0.0F;
        float rad = Mth.DEG_TO_RAD;
        model.rightLeg.xRot = (style.legs * cos + style.kick * kickR * kickR) * amp * rad;
        model.leftLeg.xRot = (-style.legs * cos + style.kick * kickL * kickL) * amp * rad;
        model.rightLeg.yRot = 0.0F;
        model.leftLeg.yRot = 0.0F;
        model.rightLeg.zRot = 0.0F;
        model.leftLeg.zRot = 0.0F;
        model.rightArm.xRot = (style.base - style.arms * cos) * amp * rad;
        model.leftArm.xRot = (style.base + style.arms * cos) * amp * rad;
        model.rightArm.yRot = -style.cross * kickR * amp * rad;
        model.leftArm.yRot = style.cross * kickL * amp * rad;
        model.rightArm.zRot = 3.0F * rad;
        model.leftArm.zRot = -3.0F * rad;
        sync(model);
    }

    /** The overlay parts follow the parts they cover. */
    static void sync(PlayerModel<?> model) {
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    /** How much of the vanilla swing an arm has: all of it empty-handed, half with an item, none in any other pose. */
    private static float share(ArmPose pose) {
        return pose == ArmPose.EMPTY ? 1.0F : pose == ArmPose.ITEM ? 0.5F : 0.0F;
    }

    private RunPoses() {
    }
}
