package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.PhoenixGestures;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The animation of the Phoenix as far as a server can check it: the timing and the keys of her ten gestures (what each
 * one is meant to look like, arm by arm), how they hand over to her stances and to her flight, the routes that start
 * them, the key presses that act before the server answers, and her flight profile. What the poses and the hands look
 * like in the world is checked by eye in a real game (the {@code /enhanced_anim} tools freeze each one).
 */
public final class PhoenixAnimationTests {
    private static final String BATCH = PhoenixTests.QUICK;

    /** Her ten gestures. */
    private static final List<GestureId> HERS = List.of(GestureId.TAKEOFF_PHOENIX, GestureId.TK_GRAB_PHOENIX, GestureId.TK_FLING_PHOENIX,
            GestureId.PHOENIX_READ_ON, GestureId.PHOENIX_READ_OFF, GestureId.PHOENIX_SENSE, GestureId.PHOENIX_MIND_CAST,
            GestureId.PHOENIX_PSIONIC_CAST, GestureId.PHOENIX_CHARGE_START, GestureId.PHOENIX_BURST);

    private static Gestures.ArmKey last(List<Gestures.ArmKey> keys) {
        return keys.get(keys.size() - 1);
    }

    private static float[] pose(Gestures.ArmKey key) {
        return new float[]{key.x, key.y, key.z};
    }

    /** The arm of a gesture at tick {@code t}, in degrees (an absolute key is its own angles). */
    private static float[] arm(GestureId id, boolean main, float t) {
        float[] out = new float[3];
        Gestures.Def def = Gestures.get(id);
        def.sampleArm(main, t, (key, isMain, to) -> {
            to[0] = key.x;
            to[1] = key.y;
            to[2] = key.z;
        }, out, new float[2], false);
        return out;
    }

    private static void assertPose(float[] actual, float[] expected, double tolerance, String what) {
        for (int i = 0; i < 3; i++) {
            assertNear(actual[i], expected[i], tolerance, what + ", angle " + i);
        }
    }

    /**
     * Where the tip of an arm points, as the model draws it: the unit vector (0, 1, 0) along the arm turned by x about
     * the side axis, then y about the vertical, then z about the front-back axis (the order of the model part), in the
     * model's space: +x is the player's left, +y down, -z forward. Degrees.
     */
    private static double[] tip(float[] pose) {
        double x = Math.toRadians(pose[0]);
        double y = Math.toRadians(pose[1]);
        double z = Math.toRadians(pose[2]);
        double[] v = {0.0, Math.cos(x), Math.sin(x)};
        v = new double[]{v[0] * Math.cos(y) + v[2] * Math.sin(y), v[1], -v[0] * Math.sin(y) + v[2] * Math.cos(y)};
        return new double[]{v[0] * Math.cos(z) - v[1] * Math.sin(z), v[0] * Math.sin(z) + v[1] * Math.cos(z), v[2]};
    }

    // ------------------------------------------------------------------------------------------------------------
    // The gestures

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herTenGesturesAreTimedAsDesigned(GameTestHelper helper) {
        // id, group, duration, attack, release, force (the table of the design, section 6.2)
        Object[][] table = {
                {GestureId.TAKEOFF_PHOENIX, GestureId.Group.TAKEOFF, 24, 3, 8, true},
                {GestureId.TK_GRAB_PHOENIX, GestureId.Group.TK, 12, 2, 6, false},
                {GestureId.TK_FLING_PHOENIX, GestureId.Group.TK, 10, 1, 5, true},
                {GestureId.PHOENIX_READ_ON, GestureId.Group.CAST, 14, 2, 5, false},
                {GestureId.PHOENIX_READ_OFF, GestureId.Group.CAST, 8, 1, 3, false},
                {GestureId.PHOENIX_SENSE, GestureId.Group.CAST, 20, 2, 6, false},
                {GestureId.PHOENIX_MIND_CAST, GestureId.Group.CAST, 16, 2, 6, false},
                {GestureId.PHOENIX_PSIONIC_CAST, GestureId.Group.CAST, 24, 2, 8, true},
                {GestureId.PHOENIX_CHARGE_START, GestureId.Group.CAST, 14, 2, 5, false},
                {GestureId.PHOENIX_BURST, GestureId.Group.CAST, 24, 1, 9, true},
        };
        check(table.length == HERS.size(), "every one of her gestures is in the table");
        for (Object[] row : table) {
            GestureId id = (GestureId) row[0];
            check(HERS.contains(id), id + " is one of hers");
            check(id.group == row[1], id + " is in the group " + row[1]);
            check(id.dur == (int) row[2] && id.attack == (int) row[3] && id.release == (int) row[4], id + " is timed " + row[2] + "/" + row[3] + "/" + row[4]);
            check(id.force == (boolean) row[5], id + (id.force ? " replaces" : " does not replace") + " the punch");
            Gestures.Def def = Gestures.get(id);
            check(def != null && def.problems().isEmpty(), id + " has well-formed keys: " + (def == null ? "none" : def.problems()));
        }
        // Her casts share the group, so the release of the Burst takes over from the gathering of the charge.
        check(GestureId.PHOENIX_BURST.group == GestureId.PHOENIX_CHARGE_START.group, "the Burst takes the place of the charge's gesture");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herTwoArmedGesturesMirrorTheirArmsAndTheOthersUseOnlyOne(GameTestHelper helper) {
        for (GestureId id : new GestureId[]{GestureId.TAKEOFF_PHOENIX, GestureId.PHOENIX_SENSE, GestureId.PHOENIX_PSIONIC_CAST,
                GestureId.PHOENIX_CHARGE_START, GestureId.PHOENIX_BURST}) {
            Gestures.Def def = Gestures.get(id);
            check(def.m.size() == def.o.size() && !def.m.isEmpty(), id + " moves both arms through the same keys");
            for (int k = 0; k < def.m.size(); k++) {
                Gestures.ArmKey m = def.m.get(k);
                Gestures.ArmKey o = def.o.get(k);
                check(m.t == o.t, id + " key " + k + " is at the same tick for both arms");
                assertNear(o.x, m.x, 1.0E-4, id + " key " + k + ": both arms swing the same way");
                assertNear(o.y, -m.y, 1.0E-4, id + " key " + k + ": the arms turn in mirror (y)");
                assertNear(o.z, -m.z, 1.0E-4, id + " key " + k + ": the arms spread in mirror (z)");
            }
        }
        // The fingers to one temple: the main hand only, and the head tilts the opposite way a little.
        for (GestureId id : new GestureId[]{GestureId.PHOENIX_READ_ON, GestureId.PHOENIX_MIND_CAST}) {
            Gestures.Def def = Gestures.get(id);
            check(!def.m.isEmpty() && def.o.isEmpty(), id + " lifts the main hand and leaves the other alone");
            check(!def.head.isEmpty(), id + " tilts the head");
        }
        // Shaking the head moves nothing else.
        Gestures.Def off = Gestures.get(GestureId.PHOENIX_READ_OFF);
        check(off.m.isEmpty() && off.o.isEmpty() && !off.head.isEmpty(), "the shake of the head is the head alone");
        assertNear(off.head.value(1)[1], 9.0, 1.0E-4, "to one side");
        assertNear(off.head.value(2)[1], -9.0, 1.0E-4, "to the other");
        assertNear(off.head.value(off.head.size() - 1)[1], 0.0, 1.0E-4, "and straight again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herWingsUnfurlOverheadAndSweepOutToTheHover(GameTestHelper helper) {
        Gestures.Def def = Gestures.get(GestureId.TAKEOFF_PHOENIX);
        assertPose(arm(GestureId.TAKEOFF_PHOENIX, true, 6), new float[]{-168, 6, 16}, 1.0E-3, "at tick 6 the arms are thrown overhead");
        assertPose(arm(GestureId.TAKEOFF_PHOENIX, true, 14), new float[]{-8, 0, 66}, 1.0E-3, "at tick 14 they are swept down and out, the wings spread");
        // And she ends on the hover of the flight layer (the shared test also checks it for every flier).
        FlightProfile profile = FlightProfile.of(Power.PHOENIX, false);
        float[] hover = {profile.anchor(FlightProfile.HOVER, 0), profile.anchor(FlightProfile.HOVER, 1), profile.anchor(FlightProfile.HOVER, 2)};
        assertPose(pose(last(def.m)), hover, 0.5, "the take-off ends on the hover pose");
        check(def.head.value(1)[0] < 0.0F, "the chin lifts as the wings go up");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herGraspAndThrowAreThoseOfTheWitchMadeGentler(GameTestHelper helper) {
        Gestures.Def grab = Gestures.get(GestureId.TK_GRAB_PHOENIX);
        check(last(grab.m).mode == Gestures.Mode.AIM && last(grab.m).target == Gestures.Target.HELD, "the grab ends with the palm aimed at the thing");
        assertPose(pose(last(grab.m)), new float[]{-8, -4, 0}, 1.0E-4, "the open, steady palm the stance holds");
        assertPose(pose(last(grab.o)), new float[]{-14, 0, -24}, 1.0E-4, "and the other hand raised a little");
        Gestures.Def fling = Gestures.get(GestureId.TK_FLING_PHOENIX);
        check(fling.m.get(0).mode == Gestures.Mode.AIM && fling.m.get(0).target == Gestures.Target.LOOK, "the throw follows the look");
        Gestures.Def witch = Gestures.get(GestureId.TK_FLING_WANDA);
        check(Math.abs(fling.m.get(0).x) < Math.abs(witch.m.get(0).x), "the wind-up is shorter than the Scarlet Witch's");
        check(fling.twist.value(0)[0] < 0.0F && fling.twist.value(1)[0] > 0.0F, "the shoulders wind back and then follow through");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hand-overs

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theChargeGathersIntoTheVAndTheBurstThrowsItOpenAndLetsGo(GameTestHelper helper) {
        Gestures.Def start = Gestures.get(GestureId.PHOENIX_CHARGE_START);
        assertPose(pose(last(start.m)), PhoenixGestures.CHARGE_M, 1.0E-4, "the charge's gesture ends on the V, main arm");
        assertPose(pose(last(start.o)), PhoenixGestures.CHARGE_O, 1.0E-4, "and the off arm");
        assertNear(start.head.value(start.head.size() - 1)[0], PhoenixGestures.CHARGE_HEAD, 1.0E-4, "with the head thrown back as the stance has it");
        assertNear(start.lean.value(start.lean.size() - 1)[0], PhoenixGestures.CHARGE_LEAN, 1.0E-4, "and the same lean");
        assertPose(arm(GestureId.PHOENIX_CHARGE_START, true, 6), new float[]{-60, 0, 70}, 1.0E-3, "at tick 6 the arms are sweeping out and up");

        Gestures.Def burst = Gestures.get(GestureId.PHOENIX_BURST);
        assertPose(pose(burst.m.get(0)), PhoenixGestures.CHARGE_M, 1.0E-4, "the Burst starts where the charge was, main arm");
        assertPose(pose(burst.o.get(0)), PhoenixGestures.CHARGE_O, 1.0E-4, "and the off arm");
        assertPose(arm(GestureId.PHOENIX_BURST, true, 2), new float[]{-12, 0, 88}, 1.0E-3, "two ticks later the V is thrown wide open");
        assertPose(arm(GestureId.PHOENIX_BURST, false, 9), new float[]{-12, 0, -88}, 1.0E-3, "and held there");
        assertNear(burst.head.value(2)[0], -22.0, 1.0E-4, "the head back further, the scream");
        assertNear(burst.lean.value(2)[0], -8.0, 1.0E-4, "the back arched");
        // It ends on the hover (she is still in the air as the wave goes out, and the flight layer takes over).
        FlightProfile profile = FlightProfile.of(Power.PHOENIX, false);
        for (int i = 0; i < 3; i++) {
            assertNear(pose(last(burst.m))[i], profile.anchor(FlightProfile.HOVER, i), 0.5, "the Burst ends on the hover pose, main arm " + i);
            assertNear(pose(last(burst.o))[i], profile.anchor(FlightProfile.HOVER, 3 + i), 0.5, "and the off arm " + i);
        }
        assertNear(burst.head.value(burst.head.size() - 1)[0], 0.0, 1.0E-4, "with the head straight");
        helper.succeed();
    }

    /**
     * The V of the charge really is a V: each arm points up and out (the right arm toward -x, the left toward +x) and a
     * little forward, and the two arms do not cross. An arm that points up is tipped outward by a z of the other sign
     * than a hanging one, so the first numbers of the design (-150 and +52) crossed them above the head: this keeps
     * that out.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theVOfTheChargeThrowsHerArmsUpAndOutAndDoesNotCrossThem(GameTestHelper helper) {
        double[] m = tip(PhoenixGestures.CHARGE_M);
        double[] o = tip(PhoenixGestures.CHARGE_O);
        check(-m[1] > 0.4 && -o[1] > 0.4, "both arms point up (" + -m[1] + ", " + -o[1] + ")");
        check(-m[0] > 0.4, "the right arm points out, to the right (" + -m[0] + ")");
        check(o[0] > 0.4, "the left arm points out, to the left (" + o[0] + ")");
        check(m[2] < -0.2 && o[2] < -0.2, "a little forward, as the gesture of the burst (" + m[2] + ")");
        assertNear(m[0], -o[0], 1.0E-6, "the arms are mirrors of each other");
        // The Burst throws it wider still: the arms level with the shoulders, to the sides.
        Gestures.Def burst = Gestures.get(GestureId.PHOENIX_BURST);
        double[] open = tip(pose(burst.m.get(2)));
        check(-open[0] > 0.95 && Math.abs(open[1]) < 0.35, "at the scream the arms are level and out (" + -open[0] + ", " + open[1] + ")");
        // And so does the gathering, on its way: out to the sides at tick 6, rising to the V.
        double[] gather = tip(arm(GestureId.PHOENIX_CHARGE_START, true, 6));
        check(-gather[0] > 0.4, "at tick 6 the arms sweep out (" + -gather[0] + ")");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGestureThatCarriesTheVTakesTheStancesHeadAndLeanFromIt(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.PHOENIX);
        HeroAnimState state = new HeroAnimState(UUID.randomUUID());
        state.stance[Stance.PHOENIX_CHARGE.ordinal()] = 1.0F;
        assertNear(PhoenixGestures.owningTheV(state), 0.0, 1.0E-6, "nothing plays: the stance holds the V");
        assertNear(hero.lean(state), -4.0, 1.0E-6, "and its lean");

        HeroAnimState.Slot slot = state.slots[0];
        slot.active = true;
        slot.id = GestureId.PHOENIX_CHARGE_START;
        slot.weight = 0.5F;
        assertNear(PhoenixGestures.owningTheV(state), 0.5, 1.0E-6, "the gesture of the charge carries half of it");
        assertNear(hero.lean(state), -2.0, 1.0E-6, "the stance gives up half its lean (the gesture's own makes up the rest)");
        slot.weight = 1.0F;
        assertNear(hero.lean(state), 0.0, 1.0E-6, "all of it while the gesture holds the V");

        slot.id = GestureId.PHOENIX_BURST;
        assertNear(PhoenixGestures.owningTheV(state), 1.0, 1.0E-6, "the Burst's gesture carries it too");
        slot.id = GestureId.PHOENIX_READ_ON;
        assertNear(PhoenixGestures.owningTheV(state), 0.0, 1.0E-6, "a gesture of the mind does not");
        slot.id = GestureId.PHOENIX_BURST;
        slot.active = false;
        assertNear(PhoenixGestures.owningTheV(state), 0.0, 1.0E-6, "and one that has finished does not either");
        assertNear(hero.lean(state), -4.0, 1.0E-6, "the stance has its lean again");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The routes

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herAbilitiesAreActedAndNoneIsPending(GameTestHelper helper) {
        String[] expected = {"toggle:MIND_READING", "flight", "held", "live:CASTING", "live:PHOENIX_CHARGING"};
        for (int ability = 1; ability <= 5; ability++) {
            check(GestureRoutes.acting(Power.PHOENIX, ability).equals(expected[ability - 1]), "ability " + ability + " is acted by " + expected[ability - 1]);
        }
        check(GestureRoutes.toggle(Power.PHOENIX, Toggle.MIND_READING, true) == GestureId.PHOENIX_READ_ON, "reading begins with the fingers to the temple");
        check(GestureRoutes.toggle(Power.PHOENIX, Toggle.MIND_READING, false) == GestureId.PHOENIX_READ_OFF, "and ends with a shake of the head");
        check(GestureRoutes.toggle(Power.BILLY, Toggle.MIND_READING, true) == null && GestureRoutes.toggle(Power.THOR, Toggle.MIND_READING, false) == null,
                "nobody else reads minds");
        check(GestureRoutes.live(Power.PHOENIX, Live.CASTING, false) == GestureId.PHOENIX_MIND_CAST, "Mind Control");
        check(GestureRoutes.live(Power.PHOENIX, Live.PHOENIX_CHARGING, true) == GestureId.PHOENIX_CHARGE_START, "the charge");
        check(GestureRoutes.live(Power.PHOENIX, Live.MIND_SENSING, false) == GestureId.PHOENIX_SENSE, "Mind Sense (Sneak + R)");
        check(GestureRoutes.live(Power.PHOENIX, Live.PHOENIX_PSIONIC, false) == GestureId.PHOENIX_PSIONIC_CAST, "the Psionic Field (Sneak + B)");
        check(GestureRoutes.live(Power.PHOENIX, Live.PHOENIX_BLAZE, false) == null && GestureRoutes.reason(Live.PHOENIX_BLAZE) != null
                && !GestureRoutes.reason(Live.PHOENIX_BLAZE).startsWith(GestureRoutes.PENDING_REASON), "the blaze is a layer, and says so");
        for (Live live : new Live[]{Live.PHOENIX_CHARGING, Live.MIND_SENSING, Live.PHOENIX_PSIONIC}) {
            check(GestureRoutes.reason(live) == null, live + " has a gesture, so it needs no reason");
        }
        check(GestureRoutes.grab(Power.PHOENIX) == GestureId.TK_GRAB_PHOENIX && GestureRoutes.fling(Power.PHOENIX) == GestureId.TK_FLING_PHOENIX,
                "her telekinesis grabs and throws in her own style");
        for (int ability = 1; ability <= 5; ability++) {
            check(GestureRoutes.cooldown(Power.PHOENIX, ability, 20) == null, "a cooldown starting is nothing she acts (every cast is a state)");
        }
        for (GestureRoutes.Flag flag : GestureRoutes.Flag.values()) {
            check(GestureRoutes.flag(Power.PHOENIX, flag, true, false, false) == null, "no flag of the older heroes is hers: " + flag);
        }
        // The older heroes are left as they were.
        check(GestureRoutes.live(Power.LOKI, Live.CASTING, false) == GestureId.LOKI_CAST && GestureRoutes.grab(Power.BILLY) == GestureId.TK_GRAB_BILLY,
                "Loki's casting and Billy's grab are unchanged");
        // None of what she acts is the stand-in of the lanes that have not routed their hero.
        for (int ability = 1; ability <= 5; ability++) {
            check(!GestureRoutes.acting(Power.PHOENIX, ability).equals(GestureRoutes.PENDING_ACTING), "ability " + ability + " is not pending");
        }
        check(GestureRoutes.toggle(Power.PHOENIX, Toggle.MIND_READING, true) != GestureRoutes.PENDING_GESTURE, "reading is not pending");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyGestureOfHersIsStartedBySomething(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.PHOENIX);
        check(hero.takeoffGesture() == GestureId.TAKEOFF_PHOENIX, "she takes off with her own gesture");
        check(hero.startedElsewhere(GestureId.TAKEOFF_PHOENIX) && hero.startedElsewhere(GestureId.PHOENIX_BURST),
                "the take-off is the animator's edge and the release a payload");
        for (GestureId id : HERS) {
            boolean routed = id == GestureId.PHOENIX_READ_ON || id == GestureId.PHOENIX_READ_OFF || id == GestureId.PHOENIX_SENSE
                    || id == GestureId.PHOENIX_MIND_CAST || id == GestureId.PHOENIX_PSIONIC_CAST || id == GestureId.PHOENIX_CHARGE_START
                    || id == GestureId.TK_GRAB_PHOENIX || id == GestureId.TK_FLING_PHOENIX;
            check(routed || GestureRoutes.startedElsewhere(id), id + " is started by nothing");
            check(!routed || !GestureRoutes.startedElsewhere(id), id + " is routed, so nothing else needs to start it");
        }
        // The older heroes' gestures are not hers.
        check(!hero.startedElsewhere(GestureId.TAKEOFF_THOR) && !hero.startedElsewhere(GestureId.LOOK_CHANGE), "and only hers are told");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The key presses

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herKeyPressesActBeforeTheServerAnswersOnlyWhereTheyWillWork(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 1000L;
        check(PhoenixGestures.predicted(data, 5, now, false, true, false) == GestureId.PHOENIX_CHARGE_START, "N, rested and fed: the arms gather at once");
        check(PhoenixGestures.predicted(data, 5, now, false, false, false) == null, "N, too hungry: the server will turn her away, so nothing");
        check(PhoenixGestures.predicted(data, 5, now, true, true, false) == GestureId.PHOENIX_CHARGE_START, "Sneak does not change the Burst");
        data.startCooldown(5, now, 600);
        check(PhoenixGestures.predicted(data, 5, now, false, true, false) == null, "N, still resting: nothing");
        check(PhoenixGestures.predicted(data, 5, now + 600, false, true, false) == GestureId.PHOENIX_CHARGE_START, "and again when the rest is over");
        data.set(Live.PHOENIX_CHARGING, true);
        check(PhoenixGestures.predicted(data, 5, now + 600, false, true, false) == null, "N, already charging: the gesture is not played twice");
        data.set(Live.PHOENIX_CHARGING, false);

        check(PhoenixGestures.predicted(data, 4, now, false, true, true) == GestureId.PHOENIX_MIND_CAST, "B at a mind in her sight: the hand goes to the temple");
        check(PhoenixGestures.predicted(data, 4, now, false, true, false) == null, "B at nothing: the server answers with a message, so no gesture");
        check(PhoenixGestures.predicted(data, 4, now, true, true, true) == null, "Sneak + B is the Psionic Field, which waits for the server");
        data.startCooldown(4, now, 400);
        check(PhoenixGestures.predicted(data, 4, now, false, true, true) == null, "B while Mind Control is recovering: nothing");
        for (int ability = 1; ability <= 3; ability++) {
            check(PhoenixGestures.predicted(new PowerData(), ability, now, false, true, true) == null, "ability " + ability + " is acted by the data, not the press");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Flight and the walk

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herFlightIsCalmWithArmsThatSpreadLikeWingsAsSheSpeedsUp(GameTestHelper helper) {
        FlightProfile p = FlightProfile.of(Power.PHOENIX, false);
        check(p != null && Power.PHOENIX.canFly(), "she flies with a profile of her own");
        check(!p.spring && !p.mWorld && !p.noBrake, "a plain pitch (not Thor's spring), no weapon arm, and she brakes");
        assertNear(p.p1, 30.0, 1.0E-6, "she leans 30 degrees at cruise");
        assertNear(p.p2, 58.0, 1.0E-6, "and 58 at a sprint");
        assertNear(p.maxRoll, 14.0, 1.0E-6, "banking at most 14 degrees");
        assertNear(p.bobAmp, 0.05, 1.0E-6, "a hover bob of five centimetres");
        assertNear(p.bobPeriod, 76.0, 1.0E-6, "every 76 ticks");
        // Arms: low and a little out at the hover, spread like a bird's at cruise, swept back and out at a sprint.
        float hover = p.anchor(FlightProfile.HOVER, 2);
        float cruise = p.anchor(FlightProfile.CRUISE, 2);
        float sprint = p.anchor(FlightProfile.SPRINT, 2);
        check(hover < cruise && cruise < sprint, "the arms spread wider as she speeds up: " + hover + ", " + cruise + ", " + sprint);
        check(p.anchor(FlightProfile.SPRINT, 0) > 0.0F && p.anchor(FlightProfile.HOVER, 0) < 0.0F, "and sweep back at a sprint");
        for (int anchor = 0; anchor < 3; anchor++) {
            assertNear(p.anchor(anchor, 0), p.anchor(anchor, 3), 1.0E-6, "the wings are level, anchor " + anchor);
            assertNear(p.anchor(anchor, 2), -p.anchor(anchor, 5), 1.0E-6, "and mirror each other, anchor " + anchor);
        }
        // A Phoenix does not fly like the Scarlet Witch: her arms are the wider.
        check(p.anchor(FlightProfile.CRUISE, 2) > FlightProfile.of(Power.SCARLET_WITCH, false).anchor(FlightProfile.CRUISE, 2),
                "at cruise her arms are wider than the Scarlet Witch's");
        helper.succeed();
    }
}
