package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.AimMath;
import com.kolodkingal.enhanced.client.render.AnimMath;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GripMath;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The pure maths of the hero animation (easing, springs, aiming, Thor's grip, the flight curves): no Minecraft client
 * classes, so it runs with the other GameTests.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class AnimationMathTests {
    private static final float RAD = (float) (Math.PI / 180.0);

    /** Both spring closed forms give the same trajectory at any step size, and the damped one overshoots as it should. */
    @GameTest(template = ARENA)
    public static void springsAreExactAtAnyFrameRate(GameTestHelper helper) {
        float[] steps = {1.0F / 60.0F, 1.0F / 20.0F, 1.0F, 3.0F};
        float[][] critical = new float[steps.length][41];
        float[][] damped = new float[steps.length][41];
        for (int k = 0; k < steps.length; k++) {
            AnimMath.Spring c = new AnimMath.Spring();
            AnimMath.Spring d = new AnimMath.Spring();
            c.x = 1.0F;
            d.x = 1.0F;
            float dt = steps[k];
            int perTick = Math.round(1.0F / dt);
            for (int tick = 1; tick <= 40; tick++) {
                // Whole ticks only: a step of 3 ticks is compared at multiples of 3.
                int n = dt >= 1.0F ? 1 : perTick;
                if (dt > 1.0F && tick % Math.round(dt) != 0) {
                    critical[k][tick] = Float.NaN;
                    damped[k][tick] = Float.NaN;
                    continue;
                }
                for (int i = 0; i < n; i++) {
                    c.critical(0.0F, 0.42F, dt);
                    d.damped(0.0F, 0.42F, 0.56F, dt);
                }
                critical[k][tick] = c.x;
                damped[k][tick] = d.x;
            }
        }
        for (int k = 1; k < steps.length; k++) {
            for (int tick = 1; tick <= 40; tick++) {
                if (Float.isNaN(critical[k][tick])) {
                    continue;
                }
                assertNear(critical[k][tick], critical[0][tick], 2.0E-4, "critical spring at tick " + tick + " with dt " + steps[k]);
                assertNear(damped[k][tick], damped[0][tick], 2.0E-4, "damped spring at tick " + tick + " with dt " + steps[k]);
            }
        }
        // The under-damped peak: 12 percent past the target for zeta 0.56.
        AnimMath.Spring d = new AnimMath.Spring();
        d.x = 1.0F;
        float lowest = 0.0F;
        for (int i = 0; i < 4000; i++) {
            d.damped(0.0F, 0.42F, 0.56F, 0.01F);
            lowest = Math.min(lowest, d.x);
        }
        double expected = -Math.exp(-Math.PI * 0.56 / Math.sqrt(1.0 - 0.56 * 0.56));
        assertNear(lowest, expected, 0.02 * Math.abs(expected), "the damped spring overshoots by exp(-pi zeta / sqrt(1 - zeta^2))");
        helper.succeed();
    }

    /** Easing is frame-rate independent; the envelope has its documented values. */
    @GameTest(template = ARENA)
    public static void easingAndEnvelope(GameTestHelper helper) {
        float a = 0.0F;
        float b = 0.0F;
        for (int i = 0; i < 60; i++) {
            a = AnimMath.ease(a, 1.0F, 1.0F / 3.0F, 4.0F);
        }
        for (int i = 0; i < 20; i++) {
            b = AnimMath.ease(b, 1.0F, 1.0F, 4.0F);
        }
        assertNear(a, b, 1.0E-5, "60 steps of a third of a tick equal 20 steps of a tick");
        assertNear(AnimMath.ease(0.0F, 1.0F, 4.0F, 4.0F), 1.0 - Math.exp(-1.0), 1.0E-6, "63 percent in one time constant");
        check(AnimMath.ease(0.3F, 1.0F, 5.0F, 0.0F) == 1.0F, "tau 0 is at once");

        float attack = 4.0F;
        float dur = 20.0F;
        float release = 6.0F;
        assertNear(AnimMath.envelope(0.0F, attack, dur, release), 0.0, 1.0E-6, "the envelope starts at 0");
        assertNear(AnimMath.envelope(attack / 2.0F, attack, dur, release), 0.875, 1.0E-6, "half way through the attack");
        assertNear(AnimMath.envelope(attack, attack, dur, release), 1.0, 1.0E-6, "full after the attack");
        assertNear(AnimMath.envelope(dur - release, attack, dur, release), 1.0, 1.0E-6, "full until the release");
        assertNear(AnimMath.envelope(dur - release / 2.0F, attack, dur, release), 0.5, 1.0E-6, "half way through the release");
        assertNear(AnimMath.envelope(dur, attack, dur, release), 0.0, 1.0E-6, "and 0 at the end");
        assertNear(AnimMath.ss(0.0F, 1.0F, 0.5F), 0.5, 1.0E-6, "smoothstep at the middle");
        for (AnimMath.Ease ease : AnimMath.Ease.values()) {
            assertNear(ease.apply(0.0F), 0.0, 1.0E-5, ease + " starts at 0");
            assertNear(ease.apply(1.0F), 1.0, 1.0E-5, ease + " ends at 1");
        }
        helper.succeed();
    }

    /** The world direction an arm points in, through the renderer's own chain (model part, flip, lean, turn). */
    private static Vector3f armTip(float xRot, float yRot, float pitchDeg, float rollDeg, float yawDeg) {
        Vector3f d = new Quaternionf().rotationZYX(0.0F, yRot, xRot).transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f l = new Vector3f(-d.x, -d.y, d.z);
        l = new Quaternionf().rotationZ(-rollDeg * RAD).mul(new Quaternionf().rotationX(-pitchDeg * RAD)).transform(l);
        l.rotateY(-yawDeg * RAD);
        return new Vector3f(l.x, l.y, -l.z);
    }

    /** Aimed arms point where they are told on a pitched, rolled, turned body; the heads look where the camera looks. */
    @GameTest(template = ARENA)
    public static void aimingInThreeDimensions(GameTestHelper helper) {
        Random random = new Random(7);
        double worst = 0.0;
        for (int i = 0; i < 50; i++) {
            float pitch = -20.0F + random.nextFloat() * 110.0F;
            float roll = -25.0F + random.nextFloat() * 50.0F;
            float yaw = -30.0F + random.nextFloat() * 60.0F;
            float x = (float) (-Math.PI * random.nextFloat());
            float y = (-80.0F + random.nextFloat() * 160.0F) * RAD;
            Vector3f world = armTip(x, y, pitch, roll, yaw);
            float[] arm = AimMath.arm(world, pitch, roll, yaw);
            Vector3f again = armTip(arm[0], arm[1], pitch, roll, yaw);
            double error = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, world.dot(again)))));
            worst = Math.max(worst, error);
        }
        check(worst < 0.1, "the arm points where it was aimed to within " + worst + " degrees");

        float[] forward = AimMath.arm(new Vector3f(0.0F, 0.0F, 1.0F), 0.0F, 0.0F, 0.0F);
        assertNear(Math.toDegrees(forward[0]), -90.0, 0.01, "upright, forward is -90");
        assertNear(Math.toDegrees(forward[1]), 0.0, 0.01, "and straight");
        float[] leaning = AimMath.arm(new Vector3f(0.0F, 0.0F, 1.0F), 40.0F, 0.0F, 0.0F);
        assertNear(Math.toDegrees(leaning[0]), -130.0, 0.01, "leaning 40 degrees, forward is -130");
        float[] banked = AimMath.arm(new Vector3f(0.2F, 0.0F, 1.0F).normalize(), 72.0F, 10.0F, 0.0F);
        assertNear(Math.toDegrees(banked[0]), -160.6, 0.1, "pitch 72, roll 10: xRot");
        assertNear(Math.toDegrees(banked[1]), 35.5, 0.1, "pitch 72, roll 10: yRot");

        // Straight up behind the back is not reachable: the arm is clamped to the shoulder plane, never beyond.
        float[] behind = AimMath.arm(new Vector3f(0.0F, 0.0F, -1.0F), 0.0F, 0.0F, 0.0F);
        check(Float.isFinite(behind[0]) && Float.isFinite(behind[1]), "an unreachable direction still gives angles");

        // The head of an upright body looks along the vanilla head angles.
        float[] head = AimMath.head(AimMath.lookDirection(0.3F, 0.5F), 0.0F, 0.0F, 0.0F);
        assertNear(head[0], 0.3, 1.0E-4, "head pitch");
        assertNear(head[1], 0.5, 1.0E-4, "head yaw");
        helper.succeed();
    }

    /** Thor's weapon at rest sits in the fist exactly as the models' own third-person transforms put it. */
    @GameTest(template = ARENA)
    public static void thorsGripMatchesTheItemModels(GameTestHelper helper) {
        // The third-person display transforms of mjolnir.json and stormbreaker.json (translation in pixels).
        float[][] json = {{0.0F, 0.62F, 3.012F}, {0.0F, 1.572F, 3.562F}};
        Vector3f[] points = {new Vector3f(8, 2.5F, 8), new Vector3f(8, 13, 8), new Vector3f(3, 12, 6), new Vector3f(13, 15, 10),
                new Vector3f(8, -4, 8), new Vector3f(1, 7, 3)};
        GripMath.Kind[] kinds = GripMath.Kind.values();
        for (int k = 0; k < kinds.length; k++) {
            for (boolean left : new boolean[]{false, true}) {
                // ItemTransform.apply: translate, rotationXYZ with the left hand's y and z negated, scale; then the model
                // is moved by (-0.5, -0.5, -0.5).
                float ry = left ? -180.0F : 0.0F;
                Matrix4f vanilla = new Matrix4f().translate(json[k][0] / 16.0F, json[k][1] / 16.0F, json[k][2] / 16.0F)
                        .rotate(new Quaternionf().rotationXYZ(30.0F * RAD, ry * RAD, 0.0F)).scale(0.55F).translate(-0.5F, -0.5F, -0.5F);
                GripMath.Placement placement = GripMath.placement(kinds[k], left, false, GripMath.BETA_REST, 0.0F, 0.0F);
                Matrix4f mine = placement.matrix();
                for (Vector3f point : points) {
                    Vector3f expected = vanilla.transformPosition(new Vector3f(point).div(16.0F));
                    Vector3f actual = mine.transformPosition(new Vector3f(point).div(16.0F));
                    check(expected.distance(actual) * 16.0F < 0.005F, kinds[k] + (left ? " left" : " right") + " hand: item point "
                            + point + " is at " + actual + " not " + expected);
                }
                Vector3f held = vanilla.transformPosition(new Vector3f(placement.held).div(16.0F)).mul(16.0F);
                check(held.distance(GripMath.FIST) < 0.005F, kinds[k] + ": the held point lands in the middle of the fist, " + held);
            }
        }
        // Whirling: the handle's end stays on the arm's axis, and the head sweeps a full circle about it.
        for (GripMath.Kind kind : kinds) {
            float far = 0.0F;
            for (int spin = 0; spin < 360; spin += 30) {
                GripMath.Placement whirl = GripMath.placement(kind, false, false, GripMath.BETA_LEAD, 1.0F, spin);
                Vector3f head = whirl.toHand(kind.head);
                far = Math.max(far, head.distance(whirl.position));
                Vector3f held = whirl.toHand(whirl.held);
                check(held.distance(whirl.position) < 0.01F, "the whirl turns the weapon about its held point");
            }
            check(far > 4.0F, kind + "'s head is a good way out from the pivot while it whirls: " + far);
        }
        helper.succeed();
    }

    /** The flight profile: the reference speeds, and a pitch curve that only ever leans further as he speeds up. */
    @GameTest(template = ARENA)
    public static void flightProfileCurves(GameTestHelper helper) {
        assertNear(FlightProfile.vRef(0.075F), 0.758, 0.001, "Magneto's cruise speed");
        assertNear(FlightProfile.vRef(0.09F), 0.910, 0.001, "Wanda's cruise speed");
        assertNear(FlightProfile.vRef(0.08F), 0.809, 0.001, "Vision's and Billy's cruise speed");
        assertNear(FlightProfile.vRef(0.11F), 1.112, 0.001, "Thor's cruise speed");
        assertNear(FlightProfile.vyRef(0.09F), 0.405, 0.001, "Wanda's climb speed");
        assertNear(FlightProfile.normalised(5.0F, 0.09F), 3.0, 1.0E-6, "speed is clamped to 3");
        assertNear(FlightProfile.normalised(-5.0F, 0.09F), -1.0, 1.0E-6, "and to -1 backwards");
        for (Power power : new Power[]{Power.MAGNETO, Power.SCARLET_WITCH, Power.VISION, Power.BILLY, Power.THOR}) {
            FlightProfile profile = FlightProfile.of(power, false);
            check(profile != null, power + " flies");
            float before = profile.pitchTarget(0.0F, 0.0F);
            assertNear(before, 0.0, 1.0E-6, power + " hovers upright");
            for (float s = 0.05F; s <= 2.0F; s += 0.05F) {
                float now = profile.pitchTarget(s, 0.0F);
                check(now >= before - 1.0E-4F, power + ": the lean never decreases with speed, at s = " + s);
                before = now;
            }
            assertNear(profile.pitchTarget(1.0F, 0.0F), profile.p1, 1.0E-4, power + " cruises at p1");
            assertNear(profile.pitchTarget(2.0F, 0.0F), profile.p2, 1.0E-4, power + " sprints at p2");
            check(profile.pitchTarget(1.0F, 1.0F) < profile.pitchTarget(1.0F, 0.0F), power + " straightens when climbing");
            check(profile.pitchTarget(1.0F, -1.0F) > profile.pitchTarget(1.0F, 0.0F), power + " tips over when diving");
            check(profile.pitchTarget(3.0F, -1.0F) <= 112.0F, power + " never tips past 112 degrees");
            check(profile.pitchTarget(-0.6F, 0.0F) < 0.0F, power + " leans back when flying backwards");
            check(Math.abs(profile.rollTarget(100.0F, 3.0F, 5.0F)) <= profile.maxRoll + 1.0E-4F, power + " banks no further than its maximum");
            assertNear(profile.rollTarget(10.0F, 0.0F, 0.0F), 0.0, 1.0E-6, power + " does not bank hovering");
            float[] pose = new float[10];
            profile.blend(0.0F, pose);
            for (int i = 0; i < 10; i++) {
                assertNear(pose[i], profile.anchor(FlightProfile.HOVER, i), 1.0E-5, power + " hover anchor " + i);
            }
            profile.blend(2.0F, pose);
            for (int i = 0; i < 10; i++) {
                assertNear(pose[i], profile.anchor(FlightProfile.SPRINT, i), 1.0E-5, power + " sprint anchor " + i);
            }
        }
        check(FlightProfile.of(Power.THOR, false).spring, "Thor's lean is a spring");
        check(FlightProfile.of(Power.QUICKSILVER, false) == null && FlightProfile.of(Power.LOKI, false) == null, "runners do not fly");
        helper.succeed();
    }
}
