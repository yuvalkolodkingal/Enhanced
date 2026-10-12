package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.SpiderEdges;
import com.kolodkingal.enhanced.client.render.SpiderGestures;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The animation of Spider-Man as far as a server can check it: the timing and the keys of his fifteen gestures (what
 * each one is meant to look like, arm by arm), how they hand over to his stances, the routes that start them, the rules
 * by which the animator starts the three it owns, the key presses that act before the server answers, and the
 * completeness of his acting (no ability, toggle or state of his is pending). What the poses, the web arm and the hands
 * in first person look like in the world is checked by eye in a real game (the {@code /enhanced_anim} tools freeze each
 * one). A test class has no client class in a signature, so nothing here names one.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SpiderAnimationTests {
    private static final String BATCH = "spider_anim";

    /** His fifteen gestures. */
    private static final List<GestureId> HIS = List.of(GestureId.SPIDER_SHOOT, GestureId.SPIDER_BURST, GestureId.SPIDER_ZIP_START,
            GestureId.SPIDER_SWING_START, GestureId.SPIDER_CRAWL_ON, GestureId.SPIDER_CRAWL_OFF, GestureId.SPIDER_SENSE_TOGGLE,
            GestureId.SPIDER_ALERT_START, GestureId.SPIDER_LEGS_OUT, GestureId.SPIDER_LEGS_IN, GestureId.SPIDER_KILL_ON,
            GestureId.SPIDER_SURGE_ON, GestureId.SPIDER_LAND, GestureId.SPIDER_LEAP, GestureId.SPIDER_ARRIVE);

    private static Gestures.ArmKey last(List<Gestures.ArmKey> keys) {
        return keys.get(keys.size() - 1);
    }

    private static float[] pose(Gestures.ArmKey key) {
        return new float[]{key.x, key.y, key.z};
    }

    /** The arm of a gesture at tick {@code t}, in degrees (an aimed key is its offsets). */
    private static float[] arm(GestureId id, boolean main, float t) {
        float[] out = new float[3];
        Gestures.get(id).sampleArm(main, t, (key, isMain, to) -> {
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

    /** The last value of a one-wide track of a gesture. */
    private static float end(Gestures.Track track) {
        check(!track.isEmpty(), "the track has keys");
        return track.value(track.size() - 1)[0];
    }

    /** The key of a track at a tick (the track must have a key exactly there). */
    private static float at(Gestures.Track track, float t) {
        for (int i = 0; i < track.size(); i++) {
            if (Math.abs(track.time(i) - t) < 1.0E-4F) {
                return track.value(i)[0];
            }
        }
        throw new AssertionError("no key at tick " + t);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The gestures

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisFifteenGesturesAreTimedAsDesigned(GameTestHelper helper) {
        // id, group, duration, attack, release, force (the table of the design, section 6.3)
        Object[][] table = {
                {GestureId.SPIDER_SHOOT, GestureId.Group.CAST, 8, 1, 4, true},
                {GestureId.SPIDER_BURST, GestureId.Group.CAST, 20, 3, 6, true},
                {GestureId.SPIDER_ZIP_START, GestureId.Group.CAST, 10, 1, 4, true},
                {GestureId.SPIDER_SWING_START, GestureId.Group.CAST, 10, 1, 4, true},
                {GestureId.SPIDER_CRAWL_ON, GestureId.Group.CAST, 8, 1, 3, false},
                {GestureId.SPIDER_CRAWL_OFF, GestureId.Group.CAST, 6, 1, 3, false},
                {GestureId.SPIDER_SENSE_TOGGLE, GestureId.Group.CAST, 8, 1, 3, false},
                {GestureId.SPIDER_ALERT_START, GestureId.Group.CAST, 12, 1, 5, false},
                {GestureId.SPIDER_LEGS_OUT, GestureId.Group.CAST, 16, 2, 6, false},
                {GestureId.SPIDER_LEGS_IN, GestureId.Group.CAST, 10, 1, 4, false},
                {GestureId.SPIDER_KILL_ON, GestureId.Group.CAST, 14, 2, 5, false},
                {GestureId.SPIDER_SURGE_ON, GestureId.Group.CAST, 22, 2, 8, false},
                {GestureId.SPIDER_LAND, GestureId.Group.LAND, 22, 2, 8, true},
                {GestureId.SPIDER_LEAP, GestureId.Group.CAST, 10, 1, 4, true},
                {GestureId.SPIDER_ARRIVE, GestureId.Group.CAST, 12, 1, 5, true},
        };
        check(table.length == HIS.size(), "every one of his gestures is in the table");
        for (Object[] row : table) {
            GestureId id = (GestureId) row[0];
            check(HIS.contains(id), id + " is one of his");
            check(id.group == row[1], id + " is in the group " + row[1]);
            check(id.dur == (int) row[2] && id.attack == (int) row[3] && id.release == (int) row[4], id + " is timed " + row[2] + "/" + row[3] + "/" + row[4]);
            check(id.force == (boolean) row[5], id + (id.force ? " replaces" : " does not replace") + " the punch");
            Gestures.Def def = Gestures.get(id);
            check(def != null && def.problems().isEmpty(), id + " has well-formed keys: " + (def == null ? "none" : def.problems()));
        }
        // Everything he casts shares the group, so a new cast takes over from the one before; the landing is the only
        // one that pivots the body about the feet.
        long casts = HIS.stream().filter(id -> id.group == GestureId.Group.CAST).count();
        check(casts == 14, "fourteen of the fifteen are casts: " + casts);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisTwoArmedGesturesMirrorTheirArms(GameTestHelper helper) {
        for (GestureId id : new GestureId[]{GestureId.SPIDER_BURST, GestureId.SPIDER_CRAWL_ON, GestureId.SPIDER_CRAWL_OFF,
                GestureId.SPIDER_ALERT_START, GestureId.SPIDER_LEGS_OUT, GestureId.SPIDER_LEGS_IN, GestureId.SPIDER_KILL_ON,
                GestureId.SPIDER_SURGE_ON, GestureId.SPIDER_LEAP, GestureId.SPIDER_ARRIVE}) {
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
        // The stances he hands over to are symmetric too.
        float[][][] stances = {{SpiderGestures.CLING_WALL_M, SpiderGestures.CLING_WALL_O}, {SpiderGestures.ALERT_M, SpiderGestures.ALERT_O},
                {SpiderGestures.KILL_M, SpiderGestures.KILL_O}, {SpiderGestures.SURGE_M, SpiderGestures.SURGE_O}};
        for (float[][] stance : stances) {
            assertNear(stance[1][0], stance[0][0], 1.0E-4, "a symmetric stance: x");
            assertNear(stance[1][1], -stance[0][1], 1.0E-4, "a symmetric stance: y");
            assertNear(stance[1][2], -stance[0][2], 1.0E-4, "a symmetric stance: z");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theThwipSnapsTheArmToTheAimAndTheOtherArmBalances(GameTestHelper helper) {
        Gestures.Def shoot = Gestures.get(GestureId.SPIDER_SHOOT);
        check(shoot.m.size() == 4 && shoot.o.size() == 3, "the web arm has four keys, the balancing arm three");
        check(shoot.m.get(1).mode == Gestures.Mode.AIM && shoot.m.get(1).target == Gestures.Target.LOOK && shoot.m.get(2).mode == Gestures.Mode.AIM,
                "at tick 2 the arm snaps to the aim along the look and holds it at tick 4");
        check(shoot.m.get(0).mode == Gestures.Mode.ABS && shoot.m.get(3).mode == Gestures.Mode.ABS, "and starts and ends on its own pose");
        assertNear(shoot.m.get(2).tremble, 1.1, 1.0E-4, "the wrist flicks (a tremble)");
        // The body twists a little against the throw and comes back.
        assertNear(at(shoot.twist, 0), 0.0, 1.0E-4, "no twist at the start");
        assertNear(at(shoot.twist, 2), -6.0, 1.0E-4, "a twist of -6 at the snap");
        assertNear(end(shoot.twist), 0.0, 1.0E-4, "and none at the end");
        assertNear(end(shoot.lean), 0.0, 1.0E-4, "upright at the end");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBurstGathersInACrouchAndIsFlungWide(GameTestHelper helper) {
        Gestures.Def burst = Gestures.get(GestureId.SPIDER_BURST);
        // Gathered across the chest: at the ninth tick both forearms cross in front of him (the right arm to its left).
        float[] gathered = arm(GestureId.SPIDER_BURST, true, 9);
        check(gathered[1] < -30 && gathered[0] < -70, "at tick 9 the right arm is across the chest: " + gathered[0] + ", " + gathered[1]);
        check(at(burst.drop, 9) > at(burst.drop, 6) && at(burst.drop, 9) >= 0.14F - 1.0E-4F, "and he is at his deepest crouch");
        // Flung wide at tick 12 and past: the right arm turned out, spread high, with a little overshoot.
        float[] flung = arm(GestureId.SPIDER_BURST, true, 12);
        check(flung[1] > 40 && flung[2] > 30, "at tick 12 the right arm is thrown out and up: " + flung[1] + ", " + flung[2]);
        assertNear(at(burst.drop, 12), 0.02, 1.0E-4, "he has straightened");
        check(at(burst.lean, 12) < 0, "and leans back");
        check(burst.right.size() == 4 && burst.left.size() == 4, "the legs move with the crouch");
        assertNear(end(burst.drop), 0.0, 1.0E-4, "he ends upright");
        // The only key with an overshoot is the throw.
        check(burst.m.get(3).ease == com.kolodkingal.enhanced.client.render.AnimMath.Ease.B, "the throw overshoots");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theWebArmEndsOnTheStanceOfAZipAndOfASwing(GameTestHelper helper) {
        for (Object[] row : new Object[][]{
                {GestureId.SPIDER_ZIP_START, SpiderGestures.ZIP_AIM, SpiderGestures.ZIP_O, SpiderGestures.ZIP_LEAN},
                {GestureId.SPIDER_SWING_START, SpiderGestures.SWING_AIM, SpiderGestures.SWING_O, SpiderGestures.SWING_LEAN}}) {
            GestureId id = (GestureId) row[0];
            Gestures.Def def = Gestures.get(id);
            Gestures.ArmKey m = last(def.m);
            check(m.mode == Gestures.Mode.AIM && m.target == Gestures.Target.ANCHOR, id + ": the web arm is aimed at the anchor of the web");
            assertPose(pose(m), (float[]) row[1], 0.01, id + ": the arm is aimed as the stance aims it");
            assertPose(pose(last(def.o)), (float[]) row[2], 0.01, id + ": the other arm is swept back as the stance has it");
            assertNear(end(def.lean), (float) row[3], 1.0E-4, id + ": he leans into it as the stance does");
            // The arm is thrown at the anchor at tick 2, and the body tips over the first two ticks.
            check(def.m.get(1).mode == Gestures.Mode.AIM && def.m.get(1).target == Gestures.Target.ANCHOR && def.m.get(1).t == 2, id + ": at tick 2");
            assertNear(at(def.lean, 0), 0.0, 1.0E-4, id + ": upright when it starts");
        }
        assertNear(end(Gestures.get(GestureId.SPIDER_SWING_START).right), SpiderGestures.SWING_LEGS[0], 1.0E-4, "the knees tuck for a swing: right");
        assertNear(end(Gestures.get(GestureId.SPIDER_SWING_START).left), SpiderGestures.SWING_LEGS[1], 1.0E-4, "and left");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theGesturesThatLeadIntoAStanceEndOnItsNumbers(GameTestHelper helper) {
        // The alert: the guard, the head a little up, a dip.
        Gestures.Def alert = Gestures.get(GestureId.SPIDER_ALERT_START);
        assertNear(end(alert.head) , SpiderGestures.ALERT_HEAD, 1.0E-4, "the alert ends on the stance's head");
        assertNear(end(alert.drop), SpiderGestures.ALERT_DROP, 1.0E-4, "and its dip");
        // Instant Kill: the head down further than the stance (a flinch), then the stance's.
        Gestures.Def kill = Gestures.get(GestureId.SPIDER_KILL_ON);
        assertNear(end(kill.head), SpiderGestures.KILL_HEAD, 1.0E-4, "Instant Kill ends on the stance's head");
        assertNear(end(kill.lean), SpiderGestures.KILL_LEAN, 1.0E-4, "and its lean");
        check(at(kill.head, 4) > SpiderGestures.KILL_HEAD && at(kill.lean, 4) > SpiderGestures.KILL_LEAN, "after a deeper flinch at tick 4");
        // The Black Eyes: the head snaps up at tick 16 and settles into the hunch.
        Gestures.Def surge = Gestures.get(GestureId.SPIDER_SURGE_ON);
        assertNear(end(surge.head), SpiderGestures.SURGE_HEAD, 1.0E-4, "the surge ends on the stance's head");
        assertNear(end(surge.lean), SpiderGestures.SURGE_LEAN, 1.0E-4, "and its lean");
        assertNear(end(surge.drop), SpiderGestures.SURGE_DROP, 1.0E-4, "and its dip");
        check(at(surge.head, 16) < 0, "the head snaps back at tick 16");
        // Clutching his head: both fists to the temples between ticks 4 and 12, trembling harder.
        check(surge.m.get(1).tremble > 0 && surge.m.get(2).tremble > surge.m.get(1).tremble, "the shudder grows while he clutches his head");
        // The arrival: the hands reach for the wall and settle into the climbing stance.
        Gestures.Def arrive = Gestures.get(GestureId.SPIDER_ARRIVE);
        assertNear(end(arrive.right), SpiderGestures.CLING_WALL_LEGS, 1.0E-4, "the arrival ends with the knees where the climb has them: right");
        assertNear(end(arrive.left), SpiderGestures.CLING_WALL_LEGS, 1.0E-4, "and left");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theGestureThatLeadsIntoAStanceTakesItsLeanDipAndHeadFromIt(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.SPIDER_MAN);
        HeroAnimState state = new HeroAnimState(UUID.randomUUID());
        // Which gesture leads into which stance (the ones whose last key is the stance's pose).
        check(SpiderGestures.leading(Stance.SPIDER_ZIP) == GestureId.SPIDER_ZIP_START && SpiderGestures.leading(Stance.SPIDER_SWING) == GestureId.SPIDER_SWING_START
                && SpiderGestures.leading(Stance.SPIDER_KILL) == GestureId.SPIDER_KILL_ON && SpiderGestures.leading(Stance.SPIDER_SURGE) == GestureId.SPIDER_SURGE_ON
                && SpiderGestures.leading(Stance.SPIDER_ALERT) == GestureId.SPIDER_ALERT_START, "each stance has the gesture that hands over to it");
        check(SpiderGestures.leading(Stance.SPIDER_CLING_WALL) == null && SpiderGestures.leading(Stance.SPIDER_CLING_CEILING) == null,
                "the climbing stances are reached by no gesture of their own (the arrival adds to them)");

        state.stance[Stance.SPIDER_ZIP.ordinal()] = 1.0F;
        assertNear(hero.lean(state), SpiderGestures.ZIP_LEAN, 1.0E-6, "the zip stance leans by itself");
        HeroAnimState.Slot slot = state.slots[0];
        slot.active = true;
        slot.id = GestureId.SPIDER_ZIP_START;
        slot.weight = 0.5F;
        assertNear(SpiderGestures.owning(state, Stance.SPIDER_ZIP), 0.5, 1.0E-6, "the gesture carries half of it");
        assertNear(hero.lean(state), SpiderGestures.ZIP_LEAN / 2, 1.0E-6, "the stance gives up half its lean");
        slot.weight = 1.0F;
        assertNear(hero.lean(state), 0.0, 1.0E-6, "all of it while the gesture holds the pose (the gesture's own lean is the stance's)");
        slot.id = GestureId.SPIDER_SWING_START;
        assertNear(hero.lean(state), SpiderGestures.ZIP_LEAN, 1.0E-6, "a gesture of another stance carries none of it");
        slot.active = false;
        assertNear(hero.lean(state), SpiderGestures.ZIP_LEAN, 1.0E-6, "a gesture that has finished carries none either");

        // The surge: lean, dip and (in the pose rows) head are shared the same way.
        state = new HeroAnimState(UUID.randomUUID());
        state.stance[Stance.SPIDER_SURGE.ordinal()] = 1.0F;
        state.stance[Stance.SPIDER_ALERT.ordinal()] = 1.0F;
        state.stance[Stance.SPIDER_CLING_WALL.ordinal()] = 1.0F;
        assertNear(hero.lean(state), SpiderGestures.SURGE_LEAN + SpiderGestures.CLING_WALL_LEAN, 1.0E-6, "the surge and the wall add their lean");
        assertNear(hero.drop(state), SpiderGestures.SURGE_DROP + SpiderGestures.ALERT_DROP + SpiderGestures.CLING_WALL_DROP, 1.0E-6, "and their dip");
        slot = state.slots[1];
        slot.active = true;
        slot.id = GestureId.SPIDER_SURGE_ON;
        slot.weight = 1.0F;
        assertNear(hero.lean(state), SpiderGestures.CLING_WALL_LEAN, 1.0E-6, "while the Black Eyes take hold their gesture has the hunch");
        assertNear(hero.drop(state), SpiderGestures.ALERT_DROP + SpiderGestures.CLING_WALL_DROP, 1.0E-6, "and the dip");
        slot.id = GestureId.SPIDER_ARRIVE;
        assertNear(hero.drop(state), SpiderGestures.SURGE_DROP + SpiderGestures.ALERT_DROP + SpiderGestures.CLING_WALL_DROP, 1.0E-6,
                "the arrival adds to the climbing stance instead of replacing its dip");
        check(hero.lean(new HeroAnimState(UUID.randomUUID())) == 0.0F && hero.drop(new HeroAnimState(UUID.randomUUID())) == 0.0F, "with no stance he adds nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSuperheroLandingSinksHimAndRisesAgain(GameTestHelper helper) {
        Gestures.Def land = Gestures.get(GestureId.SPIDER_LAND);
        check(GestureId.SPIDER_LAND.group == GestureId.Group.LAND, "it is a landing: a landing before it is replaced");
        assertNear(at(land.drop, 0), 0.0, 1.0E-4, "level at the start");
        assertNear(at(land.drop, 2), SpiderGestures.LAND_DROP, 1.0E-4, "down at the second tick");
        check(at(land.drop, 10) > 0.9F * SpiderGestures.LAND_DROP, "held through the tenth");
        assertNear(end(land.drop), 0.0, 1.0E-4, "and up again at the end");
        check(at(land.right, 2) < -40 && at(land.left, 2) > 40, "one leg planted forward, the other trailing behind with its toe down");
        assertNear(end(land.right), 0.0, 1.0E-4, "and both straight at the end");
        assertNear(end(land.left), 0.0, 1.0E-4, "(the left too)");
        assertNear(at(land.lean, 2), SpiderGestures.LAND_LEAN, 1.0E-4, "he pitches over the front foot");
        check(SpiderGestures.LAND_LEAN >= 20.0F && SpiderGestures.LAND_LEAN <= 40.0F, "by a quarter turn or less, not upright and not on his face");
        assertNear(end(land.lean), 0.0, 1.0E-4, "and is upright at the end");
        check(land.m.get(0).mode == Gestures.Mode.WORLD && land.m.get(0).x == 0.0F, "one fist hangs straight down in the world, whatever the lean");
        check(at(land.head, 2) < 0, "the head up, looking where he is going");
        // A hero's deepest sink is a thing of the body, not a lie: less than a third of a block.
        check(SpiderGestures.LAND_DROP <= 0.33F, "the dip is under a third of a block");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theHandsThatClimbSpreadAndShakeLoose(GameTestHelper helper) {
        float[] spread = arm(GestureId.SPIDER_CRAWL_ON, true, 3);
        check(spread[0] < -100 && spread[1] > 10, "Wall Crawl on: the hands go up and out at tick 3");
        float[] shake = arm(GestureId.SPIDER_CRAWL_OFF, true, 2);
        check(shake[0] > -60 && shake[0] < -30, "Wall Crawl off: the hands are shaken loose, down at the sides");
        Gestures.Def temple = Gestures.get(GestureId.SPIDER_SENSE_TOGGLE);
        check(temple.o.isEmpty() && !temple.m.isEmpty(), "the sense is switched with one hand to the temple");
        check(arm(GestureId.SPIDER_SENSE_TOGGLE, true, 3)[0] < -140, "up at the temple at tick 3");
        assertNear(end(temple.head), 0.0, 1.0E-4, "his head is straight again");
        // The legs: the shoulders roll back (the arms go behind him) and the head lifts, then it all settles.
        float[] roll = arm(GestureId.SPIDER_LEGS_OUT, true, 5);
        check(roll[0] > 0 && roll[2] > 30, "the legs unfold: the arms go back and out");
        check(at(Gestures.get(GestureId.SPIDER_LEGS_OUT).head, 5) < -10, "the head lifts");
        check(arm(GestureId.SPIDER_LEGS_IN, true, 4)[0] > 0, "the legs fold: the arms swing back once");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The routes

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisAbilitiesAreActedAndNoneIsPending(GameTestHelper helper) {
        String[] expected = {"cooldown", "live:ZIPPING", "toggle:WALL_CRAWL", "toggle:SPIDER_LEGS", "live:BLACK_EYES"};
        for (int ability = 1; ability <= 5; ability++) {
            check(GestureRoutes.acting(Power.SPIDER_MAN, ability).equals(expected[ability - 1]), "ability " + ability + " is acted by " + expected[ability - 1]);
            check(!GestureRoutes.acting(Power.SPIDER_MAN, ability).equals(GestureRoutes.PENDING_ACTING), "ability " + ability + " is not pending");
        }
        check(GestureRoutes.toggle(Power.SPIDER_MAN, Toggle.WALL_CRAWL, true) == GestureId.SPIDER_CRAWL_ON
                && GestureRoutes.toggle(Power.SPIDER_MAN, Toggle.WALL_CRAWL, false) == GestureId.SPIDER_CRAWL_OFF, "Wall Crawl on and off");
        check(GestureRoutes.toggle(Power.SPIDER_MAN, Toggle.SPIDER_SENSE, true) == GestureId.SPIDER_SENSE_TOGGLE
                && GestureRoutes.toggle(Power.SPIDER_MAN, Toggle.SPIDER_SENSE, false) == GestureId.SPIDER_SENSE_TOGGLE, "the sense either way");
        check(GestureRoutes.toggle(Power.SPIDER_MAN, Toggle.SPIDER_LEGS, true) == GestureId.SPIDER_LEGS_OUT
                && GestureRoutes.toggle(Power.SPIDER_MAN, Toggle.SPIDER_LEGS, false) == GestureId.SPIDER_LEGS_IN, "the legs out and in");
        for (Toggle toggle : new Toggle[]{Toggle.WALL_CRAWL, Toggle.SPIDER_SENSE, Toggle.SPIDER_LEGS}) {
            check(GestureRoutes.toggle(Power.SPIDER_MAN, toggle, true) != GestureRoutes.PENDING_GESTURE, toggle + " is not pending");
            for (Power other : new Power[]{Power.PHOENIX, Power.THOR, Power.LOKI, Power.BILLY, Power.THANOS, Power.ULTRON, Power.APOCALYPSE}) {
                check(GestureRoutes.toggle(other, toggle, true) == null && GestureRoutes.toggle(other, toggle, false) == null,
                        toggle + " is nobody else's: " + other);
            }
        }
        check(GestureRoutes.live(Power.SPIDER_MAN, Live.CASTING, false) == GestureId.SPIDER_SHOOT, "the Web Shot");
        check(GestureRoutes.live(Power.SPIDER_MAN, Live.ZIPPING, false) == GestureId.SPIDER_ZIP_START, "the zip");
        check(GestureRoutes.live(Power.SPIDER_MAN, Live.SWINGING, false) == GestureId.SPIDER_SWING_START, "the swing");
        check(GestureRoutes.live(Power.SPIDER_MAN, Live.SENSING, false) == GestureId.SPIDER_ALERT_START, "the sense tingles");
        check(GestureRoutes.live(Power.SPIDER_MAN, Live.BLACK_EYES, false) == GestureId.SPIDER_SURGE_ON, "the Black Eyes");
        check(GestureRoutes.live(Power.SPIDER_MAN, Live.INSTANT_KILL, false) == GestureId.SPIDER_KILL_ON, "Instant Kill");
        for (Live live : new Live[]{Live.ZIPPING, Live.SWINGING, Live.SENSING, Live.BLACK_EYES, Live.INSTANT_KILL}) {
            check(GestureRoutes.reason(live) == null, live + " has a gesture, so it needs no reason");
        }
        for (Live live : new Live[]{Live.CLINGING_WALL, Live.CLINGING_CEILING}) {
            check(GestureRoutes.live(Power.SPIDER_MAN, live, false) == null, live + " is a stance, not a gesture");
            String reason = GestureRoutes.reason(live);
            check(reason != null && !reason.startsWith(GestureRoutes.PENDING_REASON) && reason.contains("SPIDER_CLING"), live + " says it is a stance");
        }
        // Nothing else is his: the casting of Loki and of the Phoenix is theirs.
        check(GestureRoutes.live(Power.LOKI, Live.CASTING, false) == GestureId.LOKI_CAST, "Loki's casting is unchanged");
        check(GestureRoutes.live(Power.THOR, Live.ZIPPING, false) == null, "nobody else zips");
        for (int ability = 1; ability <= 5; ability++) {
            check(GestureRoutes.cooldown(Power.SPIDER_MAN, ability, 20) == (ability == 1 ? GestureId.SPIDER_BURST : null),
                    "only the burst's cooldown is acted (ability " + ability + ")");
        }
        check(GestureRoutes.cooldown(Power.SPIDER_MAN, 1, 1) == GestureId.SPIDER_BURST && GestureRoutes.cooldown(Power.SPIDER_MAN, 1, 240) == GestureId.SPIDER_BURST,
                "whatever the burst's cooldown is configured to");
        check(GestureRoutes.cooldown(Power.SPIDER_MAN, 1, 0) == null, "a cooldown that is not running is no burst");
        check(GestureRoutes.cooldown(Power.PHOENIX, 1, 240) == null && GestureRoutes.cooldown(Power.LOKI, 1, 8) == GestureId.LOKI_DAGGER, "and it is his alone");
        for (GestureRoutes.Flag flag : GestureRoutes.Flag.values()) {
            check(GestureRoutes.flag(Power.SPIDER_MAN, flag, true, false, false) == null && GestureRoutes.flag(Power.SPIDER_MAN, flag, false, false, false) == null,
                    "no flag of the older heroes is his: " + flag);
        }
        check(GestureRoutes.grab(Power.SPIDER_MAN) == null && GestureRoutes.fling(Power.SPIDER_MAN) == null, "he has no telekinesis");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyGestureOfHisIsStartedBySomething(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.SPIDER_MAN);
        check(hero != null && hero.power() == Power.SPIDER_MAN, "he has a plug-in");
        check(hero.takeoffGesture() == null, "he does not fly");
        for (GestureId id : new GestureId[]{GestureId.SPIDER_LAND, GestureId.SPIDER_LEAP, GestureId.SPIDER_ARRIVE}) {
            check(hero.startedElsewhere(id) && GestureRoutes.startedElsewhere(id), id + " is the animator's own edge");
        }
        for (GestureId id : HIS) {
            boolean edge = id == GestureId.SPIDER_LAND || id == GestureId.SPIDER_LEAP || id == GestureId.SPIDER_ARRIVE;
            boolean routed = id == GestureId.SPIDER_SHOOT || id == GestureId.SPIDER_BURST || id == GestureId.SPIDER_ZIP_START
                    || id == GestureId.SPIDER_SWING_START || id == GestureId.SPIDER_CRAWL_ON || id == GestureId.SPIDER_CRAWL_OFF
                    || id == GestureId.SPIDER_SENSE_TOGGLE || id == GestureId.SPIDER_ALERT_START || id == GestureId.SPIDER_LEGS_OUT
                    || id == GestureId.SPIDER_LEGS_IN || id == GestureId.SPIDER_KILL_ON || id == GestureId.SPIDER_SURGE_ON;
            check(edge != routed, id + " is either an edge of the animator or a route, not both and not neither");
            check(GestureRoutes.startedElsewhere(id) == edge, id + (edge ? " is told to the completeness check" : " is routed, so nothing else needs to start it"));
        }
        // The older heroes' gestures are not his.
        check(!hero.startedElsewhere(GestureId.TAKEOFF_THOR) && !hero.startedElsewhere(GestureId.LAND_SOFT) && !hero.startedElsewhere(GestureId.LOOK_CHANGE),
                "and only his are told");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The key presses

    private static SpiderGestures.Sight sight(boolean shotReady, boolean shotTarget, boolean burstTarget, boolean iron, boolean stripped, boolean confirming) {
        return new SpiderGestures.Sight(shotReady, shotTarget, burstTarget, iron, stripped, confirming, true, true);
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisKeyPressesActBeforeTheServerAnswersOnlyWhereTheyWillWork(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 1000L;
        SpiderGestures.Sight all = sight(true, true, true, true, false, false);

        // R: the Web Shot needs something in sight and its hidden timer out.
        check(SpiderGestures.predicted(data, 1, now, false, all) == GestureId.SPIDER_SHOOT, "R with a target in sight: the thwip at once");
        check(SpiderGestures.predicted(data, 1, now, false, sight(true, false, true, true, false, false)) == null, "R at nothing: no hand thrust at the air");
        check(SpiderGestures.predicted(data, 1, now, false, sight(false, true, true, true, false, false)) == null, "R before the timer is out: nothing");
        // Sneak + R: the burst needs its cooldown out and someone in range.
        check(SpiderGestures.predicted(data, 1, now, true, all) == GestureId.SPIDER_BURST, "Sneak + R with someone in range: the burst");
        check(SpiderGestures.predicted(data, 1, now, true, sight(true, true, false, true, false, false)) == null, "Sneak + R with nobody near: nothing");
        data.startCooldown(1, now, 240);
        check(SpiderGestures.predicted(data, 1, now, true, all) == null, "Sneak + R while the burst rests: nothing");
        check(SpiderGestures.predicted(data, 1, now, false, all) == GestureId.SPIDER_SHOOT, "but the Web Shot has no cooldown the data shows");
        check(SpiderGestures.predicted(data, 1, now + 240, true, all) == GestureId.SPIDER_BURST, "and the burst is back when it is rested");

        // G is told by his own movement; the press starts nothing here.
        check(SpiderGestures.predicted(new PowerData(), 2, now, false, all) == null && SpiderGestures.predicted(new PowerData(), 2, now, true, all) == null,
                "G is acted by his movement beginning");

        // V: the switches always work.
        data = new PowerData();
        data.setToggle(Toggle.WALL_CRAWL, true);
        check(SpiderGestures.predicted(data, 3, now, false, all) == GestureId.SPIDER_CRAWL_OFF, "V with Wall Crawl on: it goes off");
        data.setToggle(Toggle.WALL_CRAWL, false);
        check(SpiderGestures.predicted(data, 3, now, false, all) == GestureId.SPIDER_CRAWL_ON, "V with it off: it goes on");
        check(SpiderGestures.predicted(data, 3, now, true, all) == GestureId.SPIDER_SENSE_TOGGLE, "Sneak + V: the finger to the temple");

        // B: the legs, for the Iron Spider with whole nanites.
        data = new PowerData();
        check(SpiderGestures.predicted(data, 4, now, false, all) == GestureId.SPIDER_LEGS_OUT, "B with the legs in: they unfold");
        data.setToggle(Toggle.SPIDER_LEGS, true);
        check(SpiderGestures.predicted(data, 4, now, false, all) == GestureId.SPIDER_LEGS_IN, "B with the legs out: they fold");
        check(SpiderGestures.predicted(data, 4, now, false, sight(true, true, true, false, false, false)) == null, "B in another suit: the server only says so");
        check(SpiderGestures.predicted(data, 4, now, false, sight(true, true, true, true, true, false)) == null, "B with the nanites scattered: nothing");
        // Sneak + B: Instant Kill only at the press that confirms, with the legs out, rested and fed.
        check(SpiderGestures.predicted(data, 4, now, true, sight(true, true, true, true, false, true)) == null, "the first press only asks: nothing");
        check(SpiderGestures.predicted(data, 4, now, true, all) == GestureId.SPIDER_KILL_ON, "the press that confirms: the menace");
        check(SpiderGestures.predicted(data, 4, now, true, new SpiderGestures.Sight(true, true, true, true, false, false, false, true)) == null,
                "too hungry for it: nothing");
        data.startCooldown(4, now, 400);
        check(SpiderGestures.predicted(data, 4, now, true, all) == null, "still recharging: nothing");
        data = new PowerData();
        check(SpiderGestures.predicted(data, 4, now, true, all) == null, "with the legs in he must unfold them first: nothing");
        data.setToggle(Toggle.SPIDER_LEGS, true);
        data.set(Live.INSTANT_KILL, true);
        check(SpiderGestures.predicted(data, 4, now, true, all) == null, "pressed while it lasts, it ends: no menace");

        // N: the Black Eyes when rested and fed enough.
        data = new PowerData();
        check(SpiderGestures.predicted(data, 5, now, false, all) == GestureId.SPIDER_SURGE_ON, "N, rested: he clutches his head");
        check(SpiderGestures.predicted(data, 5, now, true, all) == GestureId.SPIDER_SURGE_ON, "Sneak does not change it");
        check(SpiderGestures.predicted(data, 5, now, false, new SpiderGestures.Sight(true, true, true, true, false, false, true, false)) == null,
                "N, too hungry: the server turns him away");
        data.startCooldown(5, now, 600);
        check(SpiderGestures.predicted(data, 5, now, false, all) == null, "N while recharging: nothing");
        data = new PowerData();
        data.set(Live.BLACK_EYES, true);
        check(SpiderGestures.predicted(data, 5, now, false, all) == null, "N while the eyes are black calms them: no surge");
        check(SpiderGestures.predicted(new PowerData(), 6, now, false, all) == null, "there is no sixth key");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the animator sees of his motion

    @GameTest(template = ARENA, batch = BATCH)
    public static void aClingThatBeginsJustAfterAZipIsTheArrival(GameTestHelper helper) {
        SpiderEdges.Memory m = new SpiderEdges.Memory();
        // On the ground, then a zip for five ticks, then he clings the very next tick.
        check(SpiderEdges.step(m, false, false, false, 0.0F) == SpiderEdges.Event.NONE, "standing");
        for (int i = 0; i < 5; i++) {
            check(SpiderEdges.step(m, false, true, false, 0.5F) == SpiderEdges.Event.NONE, "zipping, tick " + i);
        }
        check(SpiderEdges.step(m, true, false, false, 0.0F) == SpiderEdges.Event.ARRIVE, "the cling after the zip is its arrival");
        check(SpiderEdges.step(m, true, false, false, 0.2F) == SpiderEdges.Event.NONE, "and only once");

        // The flags of other players come a few ticks apart: a cling within six ticks of the zip's end still counts.
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, false, true, false, 0.5F);
        for (int i = 0; i < SpiderEdges.ARRIVAL_TICKS - 1; i++) {
            SpiderEdges.step(m, false, false, false, 0.0F);
        }
        check(SpiderEdges.step(m, true, false, false, 0.0F) == SpiderEdges.Event.ARRIVE, "five ticks after the zip fell, it still is");

        // Six ticks free of the zip is the first that is too late (it counts the tick the zip fell and the five after it).
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, false, true, false, 0.5F);
        for (int i = 0; i < SpiderEdges.ARRIVAL_TICKS; i++) {
            SpiderEdges.step(m, false, false, false, 0.0F);
        }
        check(SpiderEdges.step(m, true, false, false, 0.0F) == SpiderEdges.Event.NONE, "six ticks after the zip fell it is too late");

        // Too late: he has been in the air for a while since, it is a plain cling.
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, false, true, false, 0.5F);
        for (int i = 0; i < SpiderEdges.ARRIVAL_TICKS + 1; i++) {
            SpiderEdges.step(m, false, false, false, 0.0F);
        }
        check(SpiderEdges.step(m, true, false, false, 0.0F) == SpiderEdges.Event.NONE, "seven ticks later it is just a cling");

        // A cling without a zip (walking up to a wall) is no arrival; neither is the cling that begins as a swing ends.
        m = new SpiderEdges.Memory();
        check(SpiderEdges.step(m, true, false, false, 0.0F) == SpiderEdges.Event.NONE, "walking up to a wall");
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, false, false, true, 0.5F);
        check(SpiderEdges.step(m, true, false, false, 0.0F) == SpiderEdges.Event.NONE, "the end of a swing is not a zip");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aClingThatEndsWithAnUpwardPushIsALeap(GameTestHelper helper) {
        SpiderEdges.Memory m = new SpiderEdges.Memory();
        SpiderEdges.step(m, true, false, false, 0.0F);
        SpiderEdges.step(m, true, false, false, 0.0F);
        check(SpiderEdges.step(m, false, false, false, 0.4F) == SpiderEdges.Event.LEAP, "he lets go moving up fast: a leap");
        check(SpiderEdges.step(m, false, false, false, 0.3F) == SpiderEdges.Event.NONE, "and once");

        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, true, false, false, 0.0F);
        check(SpiderEdges.step(m, false, false, false, 0.05F) == SpiderEdges.Event.NONE, "a slow rise is a slip, not a leap");
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, true, false, false, 0.0F);
        check(SpiderEdges.step(m, false, false, false, 0.28F) == SpiderEdges.Event.NONE, "climbing over an edge at his crawl speed is not a leap");
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, true, false, false, 0.0F);
        check(SpiderEdges.step(m, false, false, false, -0.3F) == SpiderEdges.Event.NONE, "dropping off is not a leap");
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, true, false, false, 0.0F);
        check(SpiderEdges.step(m, false, true, false, 0.5F) == SpiderEdges.Event.NONE, "a zip that takes him off the wall is the zip's");
        m = new SpiderEdges.Memory();
        SpiderEdges.step(m, true, false, false, 0.0F);
        check(SpiderEdges.step(m, false, false, true, 0.5F) == SpiderEdges.Event.NONE, "and so is a swing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hardLandingsAreHeroLandingsAndHisOwnJumpsAreNot(GameTestHelper helper) {
        int away = 50;
        check(SpiderEdges.landing(10, 0.58F, away) == SpiderEdges.Landing.NONE, "his own jump comes down at 0.58: no landing of its own");
        check(SpiderEdges.landing(10, 0.66F, away) == SpiderEdges.Landing.SOFT, "a Black Eyes jump bends his knees");
        check(SpiderEdges.landing(10, 0.70F, away) == SpiderEdges.Landing.SOFT, "so does a three-block drop");
        check(SpiderEdges.landing(10, 0.78F, away) == SpiderEdges.Landing.HARD, "a four-block drop is the superhero landing");
        check(SpiderEdges.landing(10, 1.8F, away) == SpiderEdges.Landing.HARD, "and a long one");
        check(SpiderEdges.landing(SpiderEdges.LAND_MIN_AIR - 1, 1.0F, away) == SpiderEdges.Landing.NONE, "a hop of five ticks is no fall");
        check(SpiderEdges.landing(SpiderEdges.LAND_MIN_AIR, 1.0F, away) == SpiderEdges.Landing.HARD, "six ticks in the air are enough");
        check(SpiderEdges.landing(20, 1.0F, SpiderEdges.WEB_GRACE) == SpiderEdges.Landing.NONE, "the touch-down that ends a zip is the zip's");
        check(SpiderEdges.landing(20, 1.0F, SpiderEdges.WEB_GRACE + 1) == SpiderEdges.Landing.HARD, "a tick later it is a landing");
        // The memory counts the ticks since he held a web or a surface.
        SpiderEdges.Memory m = new SpiderEdges.Memory();
        check(m.sinceWeb() > 50, "a hero never seen on a web has not touched one lately");
        SpiderEdges.step(m, true, false, false, 0.0F);
        check(m.sinceWeb() == 0, "clinging this tick");
        SpiderEdges.step(m, false, false, false, 0.0F);
        SpiderEdges.step(m, false, false, false, 0.0F);
        check(m.sinceWeb() == 2, "two ticks free of it: two counted since");
        m.clear();
        check(m.sinceWeb() > 50, "forgotten");
        helper.succeed();
    }
}
