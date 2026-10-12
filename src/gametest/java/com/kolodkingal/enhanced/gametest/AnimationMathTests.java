package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.AimMath;
import com.kolodkingal.enhanced.client.render.AnimMath;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.GripMath;
import com.kolodkingal.enhanced.client.render.HeroPoses;
import com.kolodkingal.enhanced.client.render.StancePoses;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    // ---- Thor's weapons: the item models, the fist, the head and the body ----

    /** The item models of the two weapons, in the order of {@link GripMath.Kind}. */
    private static final String[] WEAPON_MODELS = {"mjolnir", "stormbreaker"};
    /**
     * The head and the torso of the player model (pixels; x left, y down, z back): what a whirling weapon must not
     * touch.
     */
    private static final float[][] HEAD_BOX = {{-4, -8, -4}, {4, 0, 4}};
    private static final float[][] TORSO_BOX = {{-4, 0, -2}, {4, 12, 2}};

    /** An item model of the mod, as shipped (the same JSON the game draws the weapon from). */
    private static JsonObject itemModel(String name) throws IOException {
        Path file = ModList.get().getModFileById(Enhanced.MODID).getFile().findResource("assets", Enhanced.MODID, "models", "item", name + ".json");
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static float[] floats(JsonObject object, String key) {
        JsonArray array = object.getAsJsonArray(key);
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    /**
     * The boxes (from, to; item pixels) of the elements of a model whose name starts with one of the prefixes (all, if
     * none).
     */
    private static List<float[][]> boxes(JsonObject model, String... prefixes) {
        List<float[][]> out = new ArrayList<>();
        for (JsonElement e : model.getAsJsonArray("elements")) {
            JsonObject element = e.getAsJsonObject();
            String name = element.has("name") ? element.get("name").getAsString() : "";
            boolean wanted = prefixes.length == 0;
            for (String prefix : prefixes) {
                wanted |= name.startsWith(prefix);
            }
            if (wanted) {
                out.add(new float[][]{floats(element, "from"), floats(element, "to")});
            }
        }
        check(!out.isEmpty(), "the model has the element(s) " + Arrays.toString(prefixes));
        return out;
    }

    /** The handle of a weapon's model (its "grip" or "handle" element): from, to. */
    private static float[][] handle(int kind, JsonObject model) {
        return boxes(model, kind == 0 ? "grip" : "handle").get(0);
    }

    /**
     * Points all over the weapon (item pixels): a grid at most 2 px apart through every element, so nothing thin slips
     * between.
     */
    private static List<Vector3f> weaponPoints(JsonObject model) {
        List<Vector3f> points = new ArrayList<>();
        for (float[][] box : boxes(model)) {
            int[] n = new int[3];
            for (int a = 0; a < 3; a++) {
                n[a] = Math.max(2, (int) Math.ceil((box[1][a] - box[0][a]) / 2.0F) + 1);
            }
            for (int i = 0; i < n[0]; i++) {
                for (int j = 0; j < n[1]; j++) {
                    for (int k = 0; k < n[2]; k++) {
                        points.add(new Vector3f(Mth.lerp(i / (n[0] - 1.0F), box[0][0], box[1][0]), Mth.lerp(j / (n[1] - 1.0F), box[0][1], box[1][1]),
                                Mth.lerp(k / (n[2] - 1.0F), box[0][2], box[1][2])));
                    }
                }
            }
        }
        return points;
    }

    private static boolean inside(Vector3f p, float[][] box, float margin) {
        return p.x > box[0][0] - margin && p.x < box[1][0] + margin && p.y > box[0][1] - margin && p.y < box[1][1] + margin
                && p.z > box[0][2] - margin && p.z < box[1][2] + margin;
    }

    /**
     * A point of the hand frame H (pixels) in the player model (pixels; x left, y down, z back), for the arm of a hand,
     * raised to {@code xRotDeg} (the model's own arm rotation: -90 is level forward, -180 straight up).
     */
    private static Vector3f handToModel(Vector3f h, boolean left, float xRotDeg) {
        float xl = left ? 1.0F - h.x : -1.0F - h.x;
        float yl = 10.0F - h.z;
        float zl = -2.0F - h.y;
        float c = Mth.cos(xRotDeg * RAD);
        float s = Mth.sin(xRotDeg * RAD);
        return new Vector3f((left ? 5.0F : -5.0F) + xl, 2.0F + yl * c - zl * s, yl * s + zl * c);
    }

    /** Thor's weapon at rest sits in the fist exactly as the models' own third-person transforms put it. */
    @GameTest(template = ARENA)
    public static void thorsGripMatchesTheItemModels(GameTestHelper helper) throws IOException {
        Vector3f[] points = {new Vector3f(8, 2.5F, 8), new Vector3f(8, 13, 8), new Vector3f(3, 12, 6), new Vector3f(13, 15, 10),
                new Vector3f(8, -4, 8), new Vector3f(1, 7, 3), new Vector3f(15, 18, 8), new Vector3f(8, -6, 8)};
        GripMath.Kind[] kinds = GripMath.Kind.values();
        for (int k = 0; k < kinds.length; k++) {
            JsonObject display = itemModel(WEAPON_MODELS[k]).getAsJsonObject("display");
            for (boolean left : new boolean[]{false, true}) {
                // The transform the game applies is the model's own third-person display transform, numbers and all.
                JsonObject hand = display.getAsJsonObject(left ? "thirdperson_lefthand" : "thirdperson_righthand");
                float[] rotation = floats(hand, "rotation");
                float[] translation = floats(hand, "translation");
                float[] scale = floats(hand, "scale");
                check(scale[0] == scale[1] && scale[1] == scale[2] && Math.abs(scale[0] - kinds[k].scale) < 1.0E-6F,
                        kinds[k] + ": the model draws it at the size the grip maths uses (" + kinds[k].scale + "), not " + Arrays.toString(scale));
                // ItemTransform.apply: translate, rotationXYZ, scale (the left hand's x, y and z are negated); then the
                // model is moved by (-0.5, -0.5, -0.5).
                float m = left ? -1.0F : 1.0F;
                Matrix4f vanilla = new Matrix4f().translate(m * translation[0] / 16.0F, translation[1] / 16.0F, translation[2] / 16.0F)
                        .rotate(new Quaternionf().rotationXYZ(rotation[0] * RAD, m * rotation[1] * RAD, m * rotation[2] * RAD))
                        .scale(scale[0], scale[1], scale[2]).translate(-0.5F, -0.5F, -0.5F);
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
        // Whirling: the held point stays on the arm's axis, and the head sweeps a full circle about it, a good way out.
        for (GripMath.Kind kind : kinds) {
            float far = 0.0F;
            for (int spin = 0; spin < 360; spin += 30) {
                GripMath.Placement whirl = GripMath.placement(kind, false, false, GripMath.BETA_LEAD, 1.0F, spin);
                Vector3f head = whirl.toHand(kind.head);
                far = Math.max(far, head.distance(whirl.position));
                Vector3f held = whirl.toHand(whirl.held);
                check(held.distance(whirl.position) < 0.01F, "the whirl turns the weapon about its held point");
            }
            check(far > 7.0F * kind.scale, kind + "'s head is a good way out from the pivot while it whirls: " + far);
        }
        helper.succeed();
    }

    /**
     * The handle really passes through Thor's fist, at rest, in the lead and in between, with either hand (and either
     * arm width), and while the weapon whirls the fist stays on the line of the handle (Mjolnir hangs from it on its
     * strap).
     */
    @GameTest(template = ARENA)
    public static void theHandlePassesThroughThorsFistInEveryGrip(GameTestHelper helper) throws IOException {
        GripMath.Kind[] kinds = GripMath.Kind.values();
        for (int k = 0; k < kinds.length; k++) {
            float[][] handle = handle(k, itemModel(WEAPON_MODELS[k]));
            Vector3f low = new Vector3f(8, handle[0][1], 8);
            Vector3f high = new Vector3f(8, handle[1][1], 8);
            for (boolean left : new boolean[]{false, true}) {
                for (boolean slim : new boolean[]{false, true}) {
                    for (float beta : new float[]{GripMath.BETA_REST, 0.0F, -40.0F, GripMath.BETA_LEAD}) {
                        GripMath.Placement p = GripMath.placement(kinds[k], left, slim, beta, 0.0F, 0.0F);
                        Vector3f fist = new Vector3f(slim ? (left ? 0.5F : -0.5F) : 0.0F, GripMath.FIST.y, GripMath.FIST.z);
                        check(distanceToSegment(fist, p.toHand(low), p.toHand(high)) < 0.01F, kinds[k] + (left ? " left" : " right") + (slim ? " slim" : "")
                                + ": at an angle of " + beta + " the handle passes through the fist");
                    }
                    for (int spin = 0; spin < 360; spin += 45) {
                        GripMath.Placement p = GripMath.placement(kinds[k], left, slim, GripMath.BETA_LEAD, 1.0F, spin);
                        // The line of the item's axis (x = z = 8), through the whirl's pivot.
                        Vector3f a = p.toHand(new Vector3f(8, -20, 8));
                        Vector3f b = p.toHand(new Vector3f(8, 30, 8));
                        check(distanceToLine(p.pivot, a, b) < 0.01F, kinds[k] + " whirling: the pivot is on the line of the handle (and its strap)");
                    }
                }
            }
        }
        helper.succeed();
    }

    private static float distanceToSegment(Vector3f p, Vector3f a, Vector3f b) {
        Vector3f ab = new Vector3f(b).sub(a);
        float t = Mth.clamp(new Vector3f(p).sub(a).dot(ab) / ab.lengthSquared(), 0.0F, 1.0F);
        return p.distance(new Vector3f(a).fma(t, ab));
    }

    private static float distanceToLine(Vector3f p, Vector3f a, Vector3f b) {
        Vector3f ab = new Vector3f(b).sub(a);
        return new Vector3f(p).sub(a).cross(ab).length() / ab.length();
    }

    /**
     * The points the grip maths calls the head, the grip and the strap really are the head, the grip and the strap of
     * the models.
     */
    @GameTest(template = ARENA)
    public static void thorsGripMarksTheRealHeadGripAndStrap(GameTestHelper helper) throws IOException {
        GripMath.Kind[] kinds = GripMath.Kind.values();
        for (int k = 0; k < kinds.length; k++) {
            JsonObject model = itemModel(WEAPON_MODELS[k]);
            GripMath.Kind kind = kinds[k];
            // The head (where the lightning beam leaves, where the shatter's fragments burst out): inside the head of
            // the weapon, about in the middle of it (the hammer of Mjolnir, the blade of the axe).
            float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
            float[] hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (float[][] box : boxes(model, k == 0 ? "head" : "blade")) {
                for (int a = 0; a < 3; a++) {
                    lo[a] = Math.min(lo[a], box[0][a]);
                    hi[a] = Math.max(hi[a], box[1][a]);
                }
            }
            Vector3f middle = new Vector3f((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2);
            check(inside(kind.head, new float[][]{lo, hi}, 0.0F), kind + ": its head point is inside the head of the model");
            check(kind.head.distance(middle) < 1.0F, kind + ": and about in the middle of it, " + kind.head + " for " + middle);
            // The held point is on the handle, the strap's end on the strap's end.
            float[][] handle = handle(k, model);
            check(kind.grip.x == 8.0F && kind.grip.z == 8.0F && kind.grip.y > handle[0][1] + 0.5F && kind.grip.y < handle[1][1] - 0.5F,
                    kind + ": the held point is on the handle: " + kind.grip + " for " + Arrays.deepToString(handle));
            if (k == 0) {
                check(Math.abs(kind.strap.y - boxes(model, "strap").get(0)[0][1]) < 1.0E-4F, "Mjolnir whirls on the end of its strap");
            } else {
                check(kind.strap.equals(kind.grip) && kind.gap == 0.0F, "Stormbreaker whirls on its handle");
            }
        }
        // The head of Mjolnir is about as long as Thor's head is wide (8 px of the player model, drawn at 0.9375); the
        // axe is the bigger of the two.
        float[][] head = boxes(itemModel("mjolnir"), "head").get(0);
        check((head[1][0] - head[0][0]) * GripMath.Kind.MJOLNIR.scale * 0.9375F >= 7.5F, "Mjolnir's head is about as long as Thor's head is wide");
        check(GripMath.Kind.STORMBREAKER.scale > GripMath.Kind.MJOLNIR.scale, "Stormbreaker is the bigger of the two");
        helper.succeed();
    }

    /**
     * The whirl on the strap clears Thor's head and body: for every elevation of the weapon arm from -45 to -195
     * degrees (the arm pointing level forward is -90, straight up -180), every angle of the spin, either hand, both
     * weapons, every point of the real models stays out of the head and the torso, by a quarter of a pixel at least.
     * (So does the weapon thrust ahead in the lead grip, at any elevation. The few ticks of the hand-over between the
     * two, when the whirl has taken hold of only a part of the weapon, are not covered.)
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void theWhirlClearsThorsHeadAndBody(GameTestHelper helper) throws IOException {
        GripMath.Kind[] kinds = GripMath.Kind.values();
        for (int k = 0; k < kinds.length; k++) {
            List<Vector3f> points = weaponPoints(itemModel(WEAPON_MODELS[k]));
            for (boolean left : new boolean[]{false, true}) {
                for (int elevation = 45; elevation <= 195; elevation += 5) {
                    for (int spin = 0; spin < 360; spin += 10) {
                        GripMath.Placement p = GripMath.placement(kinds[k], left, false, GripMath.BETA_LEAD, 1.0F, spin);
                        for (Vector3f point : points) {
                            Vector3f model = handToModel(p.toHand(point), left, -elevation);
                            check(!inside(model, HEAD_BOX, 0.25F) && !inside(model, TORSO_BOX, 0.25F), kinds[k] + (left ? " left" : " right")
                                    + " whirling, the arm at " + elevation + " degrees and the spin at " + spin + ": the item point " + point
                                    + " is at " + model + ", in him");
                        }
                    }
                }
                GripMath.Placement lead = GripMath.placement(kinds[k], left, false, GripMath.BETA_LEAD, 0.0F, 0.0F);
                for (int elevation = 0; elevation <= 195; elevation += 5) {
                    for (Vector3f point : points) {
                        Vector3f model = handToModel(lead.toHand(point), left, -elevation);
                        check(!inside(model, HEAD_BOX, 0.25F) && !inside(model, TORSO_BOX, 0.25F), kinds[k] + " in the lead with the arm at "
                                + elevation + " degrees: the item point " + point + " is inside him, at " + model);
                    }
                }
            }
        }
        helper.succeed();
    }

    /**
     * In a hero landing (the body leaned 40 degrees about a pivot 0.4 blocks up, sunk by 0.28 so the front foot is on
     * the ground) the weapon arm is held at the planting angle of its weapon with the lead grip: the lowest point of
     * the weapon, whichever the size of it, touches the ground (within a centimetre or two), well ahead of the body.
     */
    @GameTest(template = ARENA)
    public static void aHeroLandingPlantsTheHeadOfEachWeaponOnTheGround(GameTestHelper helper) throws IOException {
        float[] plant = {HeroPoses.PLANT_MJOLNIR, HeroPoses.PLANT_STORMBREAKER};
        GripMath.Kind[] kinds = GripMath.Kind.values();
        for (int k = 0; k < kinds.length; k++) {
            List<Vector3f> points = weaponPoints(itemModel(WEAPON_MODELS[k]));
            GripMath.Placement p = GripMath.placement(kinds[k], false, false, GripMath.BETA_LEAD, 0.0F, 0.0F);
            double lowest = Double.MAX_VALUE;
            double ahead = 0.0;
            for (Vector3f point : points) {
                Vector3f model = handToModel(p.toHand(point), false, -(plant[k] + 40.0F));
                double y = worldHeight(model, 40.0F, 0.4F, 0.28F);
                if (y < lowest) {
                    lowest = y;
                    // How far in front of the body this lowest point is (blocks; the model's -z is forward).
                    ahead = -model.z * 0.9375 / 16.0;
                }
            }
            check(Math.abs(lowest) < 0.015, kinds[k] + ": the lowest point of the weapon is " + lowest + " blocks above the ground in the landing");
            check(ahead > 0.4, kinds[k] + ": and it is well ahead of the body: " + ahead);
        }
        helper.succeed();
    }

    /**
     * Height (blocks above the feet) of a point of the player model, drawn leaned forward about a pivot and sunk by
     * {@code drop}.
     */
    private static double worldHeight(Vector3f model, float pitchDeg, float pivot, float drop) {
        double up = (1.501 - model.y / 16.0) * 0.9375 - pivot;
        double back = model.z * 0.9375 / 16.0;
        double pitch = pitchDeg * RAD;
        return up * Math.cos(pitch) + back * Math.sin(pitch) + pivot - drop;
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
        for (Power power : new Power[]{Power.MAGNETO, Power.SCARLET_WITCH, Power.VISION, Power.BILLY, Power.THOR,
                // The heroes of 3.0 that fly with a profile of their own, one line each in the region of their lane (a
                // hero with no flight profile yet is not listed: it flies with the vanilla pose).
                // ===== phoenix =====
                // ===== ultron =====
                Power.ULTRON,
                // ===== apocalypse =====
        }) {
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
        check(!FlightProfile.of(Power.SCARLET_WITCH, false).spring, "only Thor's lean is a spring");
        helper.succeed();
    }

    /** Every gesture table is well formed, and each take-off ends exactly on its hero's hover pose: no pop as it hands over. */
    @GameTest(template = ARENA)
    public static void flightGesturesAreWellFormedAndHandOverWithoutAPop(GameTestHelper helper) {
        for (Gestures.Def def : Gestures.all().values()) {
            check(def.problems().isEmpty(), "gesture " + def.id + ": " + def.problems());
        }
        GestureId[] stageA = {GestureId.LOOK_CHANGE, GestureId.TAKEOFF_MAGNETO, GestureId.TAKEOFF_WANDA, GestureId.TAKEOFF_VISION,
                GestureId.TAKEOFF_BILLY, GestureId.TAKEOFF_THOR, GestureId.BRAKE, GestureId.BRAKE_THOR, GestureId.LAND_SOFT,
                GestureId.LAND_HERO, GestureId.THOR_ARRIVAL, GestureId.RUN_SKID, GestureId.THOR_SHATTER, GestureId.THOR_REFORM,
                GestureId.THOR_PIN};
        for (GestureId id : stageA) {
            check(Gestures.get(id) != null, id + " has its keys");
        }
        // One take-off per flying hero with a profile; a hero of 3.0 adds its gesture to the first array and its power to the
        // second, in the same position, each in the region of its lane.
        GestureId[] ids = {GestureId.TAKEOFF_MAGNETO, GestureId.TAKEOFF_WANDA, GestureId.TAKEOFF_VISION, GestureId.TAKEOFF_BILLY,
                GestureId.TAKEOFF_THOR,
                // ===== phoenix =====
                // ===== ultron =====
                GestureId.TAKEOFF_ULTRON,
                // ===== apocalypse =====
        };
        Power[] powers = {Power.MAGNETO, Power.SCARLET_WITCH, Power.VISION, Power.BILLY, Power.THOR,
                // ===== phoenix =====
                // ===== ultron =====
                Power.ULTRON,
                // ===== apocalypse =====
        };
        for (int k = 0; k < ids.length; k++) {
            Gestures.Def def = Gestures.get(ids[k]);
            FlightProfile profile = FlightProfile.of(powers[k], false);
            Gestures.ArmKey m = def.m.get(def.m.size() - 1);
            Gestures.ArmKey o = def.o.get(def.o.size() - 1);
            float[] mine = {m.x, m.y, m.z, o.x, o.y, o.z};
            for (int i = 0; i < 6; i++) {
                assertNear(mine[i], profile.anchor(FlightProfile.HOVER, i), 0.5, def.id + ": the last key is the hover pose, value " + i);
            }
            check(m.mode == (profile.mWorld ? Gestures.Mode.WORLD : Gestures.Mode.ABS), def.id + ": the arm mode matches the hover pose");
            if (!def.right.isEmpty()) {
                assertNear(def.right.value(def.right.size() - 1)[0], profile.anchor(FlightProfile.HOVER, 6), 0.5,
                        def.id + ": the right leg ends on the hover pose");
                assertNear(def.left.value(def.left.size() - 1)[0], profile.anchor(FlightProfile.HOVER, 8), 0.5,
                        def.id + ": the left leg ends on the hover pose");
            }
        }
        check(Gestures.get(GestureId.TAKEOFF_BILLY).hasYaw(), "Billy's take-off is a lazy turn that ends facing the way he faces");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The ability gestures

    /** Every gesture has its keys, a sane envelope, and a table that is in order. */
    @GameTest(template = ARENA)
    public static void everyGestureHasWellFormedKeys(GameTestHelper helper) {
        for (GestureId id : GestureId.values()) {
            Gestures.Def def = Gestures.get(id);
            check(def != null, id + " has no keys");
            check(def.problems().isEmpty(), id + ": " + def.problems());
            check(id.attack >= 1 && id.release >= 1 && id.attack + id.release <= id.dur, id + ": the attack and the release fit in " + id.dur + " ticks");
            boolean drives = !def.m.isEmpty() || !def.o.isEmpty() || !def.right.isEmpty() || !def.head.isEmpty() || !def.twist.isEmpty()
                    || !def.lean.isEmpty() || !def.drop.isEmpty() || !def.yaw.isEmpty() || !def.roll.isEmpty();
            check(drives, id + " drives nothing");
            // An arm track is all absolute (or aimed or by world elevation) or all additive: the two cannot be blended.
            for (java.util.List<Gestures.ArmKey> keys : java.util.List.of(def.m, def.o)) {
                boolean additive = false;
                boolean absolute = false;
                for (Gestures.ArmKey key : keys) {
                    additive |= key.mode == Gestures.Mode.ADD;
                    absolute |= key.mode != Gestures.Mode.ADD;
                }
                check(!(additive && absolute), id + " mixes added and absolute arm keys");
            }
        }
        helper.succeed();
    }

    /** A gesture that hands over to a stance ends exactly on that stance's pose, so nothing pops when it lets go. */
    @GameTest(template = ARENA)
    public static void gesturesHandOverToStancesWithoutAPop(GameTestHelper helper) {
        // Both arms at the pose of the stance.
        Object[][] pairs = {
                {GestureId.WANDA_SHIELD_RAISE, StancePoses.SHIELD_WANDA_M, StancePoses.SHIELD_WANDA_O},
                {GestureId.WANDA_SHIELD_SNAP, StancePoses.SHIELD_WANDA_M, StancePoses.SHIELD_WANDA_O},
                {GestureId.MAG_SHIELD_ON, StancePoses.MAG_FIELD_M, StancePoses.MAG_FIELD_O},
                {GestureId.VISION_PHASE_IN, StancePoses.VISION_PHASED_M, StancePoses.VISION_PHASED_O},
                {GestureId.VISION_DENSE_ON, StancePoses.VISION_DENSE_M, StancePoses.VISION_DENSE_O},
                {GestureId.BILLY_FREEZE_CAST, StancePoses.BILLY_FREEZE_M, StancePoses.BILLY_FREEZE_O},
                // The gestures of 3.0 that hand over to a stance, one row each in the region of their lane.
                // ===== phoenix =====
                // ===== spider =====
                // ===== thanos =====
                // ===== ultron =====
                // ===== apocalypse =====
        };
        for (Object[] pair : pairs) {
            Gestures.Def def = Gestures.get((GestureId) pair[0]);
            Gestures.ArmKey m = def.m.get(def.m.size() - 1);
            Gestures.ArmKey o = def.o.get(def.o.size() - 1);
            float[] stanceM = (float[]) pair[1];
            float[] stanceO = (float[]) pair[2];
            float[] mine = {m.x, m.y, m.z, o.x, o.y, o.z};
            float[] theirs = {stanceM[0], stanceM[1], stanceM[2], stanceO[0], stanceO[1], stanceO[2]};
            for (int i = 0; i < 6; i++) {
                assertNear(mine[i], theirs[i], 0.5, def.id + ": the last key is the stance's pose, value " + i);
            }
            check(m.mode == Gestures.Mode.ABS && o.mode == Gestures.Mode.ABS, def.id + " ends on absolute arms");
        }
        // The stances that spread the arm a little (added to what is below): the gesture ends at the same spread.
        assertNear(Gestures.get(GestureId.LOKI_FROST_ON).m.get(Gestures.get(GestureId.LOKI_FROST_ON).m.size() - 1).z, StancePoses.LOKI_FROST_Z, 0.5,
                "the Frost Giant's roar ends in his stance");
        assertNear(Gestures.get(GestureId.THOR_UNLEASH_ON).m.get(Gestures.get(GestureId.THOR_UNLEASH_ON).m.size() - 1).z, StancePoses.THOR_UNLEASHED_Z, 0.5,
                "the God of Thunder's roar ends in his stance");
        // The beams: the arms end aimed along the look at the stance's offsets, the kick's twist is the stance's.
        Gestures.Def beam = Gestures.get(GestureId.WANDA_BEAM_KICK);
        Gestures.ArmKey bm = beam.m.get(beam.m.size() - 1);
        Gestures.ArmKey bo = beam.o.get(beam.o.size() - 1);
        for (int i = 0; i < 3; i++) {
            assertNear(new float[]{bm.x, bm.y, bm.z}[i], StancePoses.BEAM_WANDA_M_AIM[i], 0.5, "the Chaos Beam kick ends on the stance, main arm " + i);
            assertNear(new float[]{bo.x, bo.y, bo.z}[i], StancePoses.BEAM_WANDA_O_AIM[i], 0.5, "the Chaos Beam kick ends on the stance, off arm " + i);
        }
        for (GestureId id : new GestureId[]{GestureId.THOR_BEAM_KICK, GestureId.THOR_BEAM_KICK_BARE}) {
            Gestures.Def kick = Gestures.get(id);
            assertNear(kick.twist.value(kick.twist.size() - 1)[0], StancePoses.THOR_BEAM_TWIST, 0.5, id + " ends on the beam's twist");
        }
        // A gesture and its stance hold the same lean and head: the shield's wind-up ends in them.
        Gestures.Def raise = Gestures.get(GestureId.WANDA_SHIELD_RAISE);
        assertNear(raise.lean.value(raise.lean.size() - 1)[0], StancePoses.SHIELD_WANDA_LEAN, 0.5, "the shield's lean");
        assertNear(raise.head.value(raise.head.size() - 1)[0], StancePoses.SHIELD_WANDA_HEAD[0], 0.5, "the shield's head");
        helper.succeed();
    }

    /**
     * The Chaos Shield's raise is acted over the shield's own wind-up, whatever the config says: the keys are played
     * faster or slower so the arms are thrown open (key 15) at the very tick the shield is complete, and a wind-up too
     * short to act is a snap. A raise the shield never formed for (let go of at once, or refused) stops its effects.
     */
    @GameTest(template = ARENA)
    public static void theShieldsRaiseKeepsTimeWithItsWindUp(GameTestHelper helper) {
        int opens = GestureRoutes.SHIELD_KEYED_WIND_UP;
        Gestures.Def raise = Gestures.get(GestureId.WANDA_SHIELD_RAISE);
        Gestures.ArmKey thrownOpen = null;
        for (Gestures.ArmKey key : raise.m) {
            if (key.t >= opens - 0.01F && thrownOpen == null) {
                thrownOpen = key;
            }
        }
        check(thrownOpen != null && Math.abs(thrownOpen.t - opens) < 0.01F, "the arms are thrown open at tick " + opens + " of the keys");
        for (int windUp : new int[]{4, 8, 15, 30, 60, 200}) {
            float rate = GestureRoutes.shieldRate(windUp);
            // The gesture's own clock reaches the opening key exactly when the shield is complete.
            assertNear(windUp * rate, opens, 1.0E-4, "the arms open when a " + windUp + "-tick shield is complete");
            check(GestureRoutes.shieldRaise(windUp) == GestureId.WANDA_SHIELD_RAISE, "a " + windUp + "-tick shield is acted out");
        }
        assertNear(GestureRoutes.shieldRate(15), 1.0, 1.0E-6, "the keyed wind-up plays as keyed");
        assertNear(GestureRoutes.shieldRate(30), 0.5, 1.0E-6, "a shield twice as slow gets a raise twice as slow");
        for (int windUp : new int[]{0, 1, 3}) {
            check(GestureRoutes.shieldRaise(windUp) == GestureId.WANDA_SHIELD_SNAP, "a " + windUp + "-tick shield is a snap");
        }
        check(GestureRoutes.shieldRate(0) == 1.0F, "no wind-up: no stretching either");

        // The effects of a raise: believed while the data has not caught up with the key press, over once it fell or
        // never came.
        check(!GestureRoutes.shieldRaiseAborted(true, true, 5), "held: on");
        check(!GestureRoutes.shieldRaiseAborted(false, false, 2), "the key was just pressed: the data is on its way");
        check(GestureRoutes.shieldRaiseAborted(false, false, GestureRoutes.SHIELD_DATA_GRACE + 1), "refused: the data never came");
        check(GestureRoutes.shieldRaiseAborted(false, true, 3), "let go of before it formed: over at once");
        helper.succeed();
    }

    /** Every ability of every hero, every toggle, momentary state and flag is acted by a gesture, or has a reason why not. */
    @GameTest(template = ARENA)
    public static void everyAbilityIsActedOrExplained(GameTestHelper helper) {
        // Every power that has abilities: the heroes of 3.0 are in without their lanes editing this line (a lane that
        // routes a toggle or a state to its own hero only would otherwise fail "has no gesture" here).
        Power[] heroes = java.util.Arrays.stream(Power.values()).filter(Power::hasPowers).toArray(Power[]::new);
        java.util.Set<GestureId> used = new java.util.HashSet<>();
        for (Power power : heroes) {
            for (int ability = 1; ability <= 5; ability++) {
                String how = GestureRoutes.acting(power, ability);
                String where = power + " ability " + ability + " (" + how + ")";
                if (how.startsWith("none: ")) {
                    check(how.length() > 12, where + " needs a reason");
                } else if (how.equals("cooldown")) {
                    check(GestureRoutes.cooldown(power, ability, 1) != null || GestureRoutes.cooldown(power, ability, 99) != null, where + " has no gesture");
                } else if (how.startsWith("flag:")) {
                    GestureRoutes.Flag flag = GestureRoutes.Flag.valueOf(how.substring(5));
                    check(GestureRoutes.flag(power, flag, true, false, false) != null, where + " has no gesture");
                } else if (how.startsWith("toggle:")) {
                    check(GestureRoutes.toggle(power, Toggle.valueOf(how.substring(7)), true) != null, where + " has no gesture");
                } else if (how.startsWith("live:")) {
                    check(GestureRoutes.live(power, Live.valueOf(how.substring(5)), true) != null, where + " has no gesture");
                } else if (how.equals("held")) {
                    check(GestureRoutes.grab(power) != null && GestureRoutes.fling(power) != null, where + " has no grab and fling");
                } else {
                    check(how.equals("hammer") || how.equals("hex") || how.equals("flight"), where + " is not a known way of acting");
                }
            }
            for (int ability = 1; ability <= 5; ability++) {
                for (long left : new long[]{1, 99}) {
                    add(used, GestureRoutes.cooldown(power, ability, left));
                }
            }
            for (Toggle toggle : Toggle.values()) {
                add(used, GestureRoutes.toggle(power, toggle, true));
                add(used, GestureRoutes.toggle(power, toggle, false));
            }
            for (GestureRoutes.Flag flag : GestureRoutes.Flag.values()) {
                add(used, GestureRoutes.flag(power, flag, true, false, false));
                add(used, GestureRoutes.flag(power, flag, false, false, false));
            }
            for (Live live : Live.values()) {
                add(used, GestureRoutes.live(power, live, true));
                add(used, GestureRoutes.live(power, live, false));
            }
            add(used, GestureRoutes.grab(power));
            add(used, GestureRoutes.fling(power));
        }
        for (GestureRoutes.Hammer hammer : GestureRoutes.Hammer.values()) {
            add(used, GestureRoutes.hammer(hammer));
        }
        for (GestureRoutes.HexChange change : GestureRoutes.HexChange.values()) {
            add(used, GestureRoutes.hex(change));
        }
        // Every toggle, flag and state is acted for some hero or explained.
        for (Toggle toggle : Toggle.values()) {
            boolean acted = false;
            for (Power power : heroes) {
                acted |= GestureRoutes.toggle(power, toggle, true) != null;
            }
            check(acted, "the toggle " + toggle + " has no gesture");
        }
        for (GestureRoutes.Flag flag : GestureRoutes.Flag.values()) {
            boolean acted = false;
            for (Power power : heroes) {
                acted |= GestureRoutes.flag(power, flag, true, false, false) != null || GestureRoutes.flag(power, flag, false, false, false) != null;
            }
            check(acted, "the flag " + flag + " has no gesture");
        }
        for (Live live : Live.values()) {
            boolean acted = false;
            for (Power power : heroes) {
                acted |= GestureRoutes.live(power, live, true) != null;
            }
            check(acted || GestureRoutes.reason(live) != null, "the state " + live + " has neither a gesture nor a reason");
        }
        // No gesture is orphaned: each is started by the table or by something that says so.
        for (GestureId id : GestureId.values()) {
            check(used.contains(id) || GestureRoutes.startedElsewhere(id), id + " is started by nothing");
        }
        // White Vision and a phased Vision have no temple gesture; a sneaking Loki's long cooldown is a fan.
        check(GestureRoutes.flag(Power.VISION, GestureRoutes.Flag.BEAM, true, true, false) == null, "White Vision has no temple gesture");
        check(GestureRoutes.flag(Power.VISION, GestureRoutes.Flag.BEAM, true, false, true) == null, "a phased Vision has no temple gesture");
        check(GestureRoutes.cooldown(Power.LOKI, 1, 8) == GestureId.LOKI_DAGGER && GestureRoutes.cooldown(Power.LOKI, 1, 30) == GestureId.LOKI_FAN,
                "Loki's two cooldowns tell a dagger from a fan");
        helper.succeed();
    }

    private static void add(java.util.Set<GestureId> used, GestureId id) {
        if (id != null) {
            used.add(id);
        }
    }
}
