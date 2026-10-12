package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.client.render.ThanosGestures;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The animation of the playing Thanos as far as a server can check it: the timing and the keys of his eleven gestures
 * (what each is meant to look like, arm by arm), how they hand over to his two stances, the routes that start them
 * (the two uses of a key told apart by the length of a cooldown), the key presses that act before the server answers,
 * and the fact that he has no flight profile. What the poses, the hands in first person and the flourishes look like
 * in the world is checked by eye in a real game (the {@code /enhanced_anim} tools freeze each pose).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ThanosAnimationTests {
    private static final String BATCH = "thanos_anim";

    /** His eleven gestures. */
    private static final List<GestureId> HIS = List.of(GestureId.THANOS_BLAST, GestureId.THANOS_SLAM, GestureId.THANOS_STEP, GestureId.THANOS_PULL,
            GestureId.THANOS_WARP, GestureId.THANOS_VEIL_ON, GestureId.THANOS_VEIL_OFF, GestureId.THANOS_DRAIN_CAST, GestureId.THANOS_SIGHT,
            GestureId.THANOS_REWIND, GestureId.THANOS_TIMELOCK);

    private static Gestures.ArmKey last(List<Gestures.ArmKey> keys) {
        return keys.get(keys.size() - 1);
    }

    private static float[] pose(Gestures.ArmKey key) {
        return new float[]{key.x, key.y, key.z};
    }

    private static void assertPose(float[] actual, float[] expected, double tolerance, String what) {
        for (int i = 0; i < 3; i++) {
            assertNear(actual[i], expected[i], tolerance, what + ", angle " + i);
        }
    }

    /** The key of an arm track at exactly tick {@code t}, or null. */
    private static Gestures.ArmKey keyAt(List<Gestures.ArmKey> keys, float t) {
        for (Gestures.ArmKey key : keys) {
            if (key.t == t) {
                return key;
            }
        }
        return null;
    }

    private static HeroClient hero() {
        return HeroClients.of(Power.THANOS);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The gestures

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisElevenGesturesAreTimedAsDesigned(GameTestHelper helper) {
        // id, group, duration, attack, release, force (the table of the design, section 15.12)
        Object[][] table = {
                {GestureId.THANOS_BLAST, 10, 1, 5, true},
                {GestureId.THANOS_SLAM, 22, 2, 8, true},
                {GestureId.THANOS_STEP, 12, 1, 5, false},
                {GestureId.THANOS_PULL, 16, 2, 6, true},
                {GestureId.THANOS_WARP, 14, 1, 5, true},
                {GestureId.THANOS_VEIL_ON, 16, 2, 6, false},
                {GestureId.THANOS_VEIL_OFF, 10, 1, 4, false},
                {GestureId.THANOS_DRAIN_CAST, 12, 1, 5, true},
                {GestureId.THANOS_SIGHT, 14, 2, 5, false},
                {GestureId.THANOS_REWIND, 22, 2, 8, false},
                {GestureId.THANOS_TIMELOCK, 20, 2, 8, false},
        };
        check(table.length == HIS.size(), "every one of his gestures is in the table");
        for (Object[] row : table) {
            GestureId id = (GestureId) row[0];
            check(HIS.contains(id), id + " is one of his");
            check(id.group == GestureId.Group.CAST, id + " is a cast, so only one of his plays at a time");
            check(id.dur == (int) row[1] && id.attack == (int) row[2] && id.release == (int) row[3], id + " is timed " + row[1] + "/" + row[2] + "/" + row[3]);
            check(id.force == (boolean) row[4], id + (id.force ? " replaces" : " does not replace") + " the punch");
            Gestures.Def def = Gestures.get(id);
            check(def != null && def.problems().isEmpty(), id + " has well-formed keys: " + (def == null ? "none" : def.problems()));
        }
        helper.succeed();
    }

    /**
     * The Gauntlet is on the off arm, so every gesture of his drives it; the main arm only balances (the Blast, the
     * Pull), or joins in for the four that use both hands (the Slam, the Veil closing and opening, the Time Lock).
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theGauntletArmDrivesEveryGestureAndTheMainArmOnlyBalancesOrJoinsIn(GameTestHelper helper) {
        for (GestureId id : HIS) {
            check(!Gestures.get(id).o.isEmpty(), id + " drives the arm the Gauntlet is on");
        }
        for (GestureId id : new GestureId[]{GestureId.THANOS_STEP, GestureId.THANOS_WARP, GestureId.THANOS_DRAIN_CAST, GestureId.THANOS_SIGHT,
                GestureId.THANOS_REWIND}) {
            check(Gestures.get(id).m.isEmpty(), id + " leaves the main arm to what it was doing");
        }
        for (GestureId id : new GestureId[]{GestureId.THANOS_SLAM, GestureId.THANOS_VEIL_ON, GestureId.THANOS_VEIL_OFF, GestureId.THANOS_TIMELOCK}) {
            Gestures.Def def = Gestures.get(id);
            check(def.m.size() == def.o.size(), id + " moves both arms through the same keys");
        }
        // The two-handed ones are in mirror: the same swing, outward on both sides.
        for (GestureId id : new GestureId[]{GestureId.THANOS_VEIL_ON, GestureId.THANOS_TIMELOCK}) {
            Gestures.Def def = Gestures.get(id);
            Gestures.ArmKey m = keyAt(def.m, id == GestureId.THANOS_VEIL_ON ? 5 : 6);
            Gestures.ArmKey o = keyAt(def.o, id == GestureId.THANOS_VEIL_ON ? 5 : 6);
            assertNear(o.x, m.x, 1.0E-4, id + ": both arms swing the same way");
            assertNear(o.y, -m.y, 1.0E-4, id + ": and turn in mirror (y)");
            assertNear(o.z, -m.z, 1.0E-4, id + ": and spread in mirror (z)");
        }
        helper.succeed();
    }

    /** No key throws an arm somewhere an arm cannot go (the model's arm turns from 190 degrees overhead to 60 behind). */
    @GameTest(template = ARENA, batch = BATCH)
    public static void noKeyAsksAnArmForAnImpossibleAngle(GameTestHelper helper) {
        for (GestureId id : HIS) {
            Gestures.Def def = Gestures.get(id);
            for (List<Gestures.ArmKey> keys : List.of(def.m, def.o)) {
                for (Gestures.ArmKey key : keys) {
                    if (key.mode == Gestures.Mode.AIM) {
                        check(key.target == Gestures.Target.LOOK, id + " aims along the look only");
                        check(Math.abs(key.x) <= 10.0F && Math.abs(key.y) <= 10.0F && Math.abs(key.z) <= 10.0F, id + ": an aimed key is a small offset from the aim");
                    } else {
                        check(key.mode == Gestures.Mode.ABS, id + " writes its arms as raw angles");
                        check(key.x >= -190.0F && key.x <= 60.0F, id + " t" + key.t + ": the arm's x " + key.x + " is within the reach of the shoulder");
                        check(Math.abs(key.y) <= 100.0F && Math.abs(key.z) <= 100.0F, id + " t" + key.t + ": the arm's y and z are sane");
                    }
                }
            }
            // The legs are only ever asked to swing a stride's worth, and the body to sink a hand's breadth.
            for (int i = 0; i < def.right.size(); i++) {
                check(Math.abs(def.right.value(i)[0]) <= 30.0F && Math.abs(def.left.value(i)[0]) <= 30.0F, id + ": the legs bend within a stride");
            }
            for (int i = 0; i < def.drop.size(); i++) {
                check(def.drop.value(i)[0] >= 0.0F && def.drop.value(i)[0] <= 0.3F, id + ": the body sinks by " + def.drop.value(i)[0] + " blocks at most");
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBlastIsAThrustAtTheAimAndTheSlamIsRaisedOverheadAndDrivenDown(GameTestHelper helper) {
        Gestures.Def blast = Gestures.get(GestureId.THANOS_BLAST);
        Gestures.ArmKey thrust = keyAt(blast.o, 2);
        check(thrust != null && thrust.mode == Gestures.Mode.AIM && thrust.target == Gestures.Target.LOOK, "at tick 2 the Gauntlet is aimed along the look");
        Gestures.ArmKey hold = keyAt(blast.o, 4);
        check(hold != null && hold.mode == Gestures.Mode.AIM && hold.tremble > 0.0F, "and holds, trembling, at tick 4");
        check(blast.o.get(0).mode == Gestures.Mode.ABS && last(blast.o).mode == Gestures.Mode.ABS, "it starts and ends on a pose of its own");
        // The chest follows the arm that goes forward: a left arm forward turns it to the right (positive).
        check(blast.twist.scalar(2.0F, 0.0F) > 0.0F && blast.twist.scalar(0.0F, -1.0F) == 0.0F && last(blast.o).t == 10.0F, "the chest turns with the arm that is thrust forward");
        check(blast.lean.scalar(2.0F, 0.0F) > 0.0F, "and he leans into it");

        Gestures.Def slam = Gestures.get(GestureId.THANOS_SLAM);
        assertPose(pose(keyAt(slam.o, 6)), new float[]{-175, 0, -12}, 1.0E-4, "at tick 6 the Gauntlet is overhead");
        assertPose(pose(keyAt(slam.o, 10)), new float[]{-175, 0, -12}, 1.0E-4, "and held there to tick 10");
        assertPose(pose(keyAt(slam.o, 12)), new float[]{12, 0, -10}, 1.0E-4, "at tick 12 it has been driven down past the hip");
        assertNear(slam.drop.scalar(12.0F, -1.0F), 0.20, 1.0E-4, "the body sinks 0.2 blocks with the blow");
        for (float t = 0.0F; t <= 10.0F; t += 1.0F) {
            assertNear(slam.drop.scalar(t, -1.0F), 0.0, 1.0E-6, "and does not sink while the Gauntlet is going up, tick " + t);
        }
        check(slam.lean.scalar(6.0F, 0.0F) < 0.0F && slam.lean.scalar(12.0F, 0.0F) > 0.0F, "he leans back to lift it and forward to drive it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSpaceStoneDrawsACircleAndDragsBack(GameTestHelper helper) {
        Gestures.Def step = Gestures.get(GestureId.THANOS_STEP);
        float[] x = new float[5];
        float[] y = new float[5];
        for (int i = 0; i < 5; i++) {
            x[i] = step.o.get(i).x;
            y[i] = step.o.get(i).y;
        }
        // Up and out, round to the far side, and down again: the hand does not go back and forth along one line.
        check(x[1] < x[0] && x[1] < x[2] && x[2] < x[3], "the hand rises, then comes down again (" + x[0] + ", " + x[1] + ", " + x[2] + ", " + x[3] + ")");
        check(y[1] < y[0] && y[2] < y[1], "and swings out to the side as it goes round (" + y[0] + ", " + y[1] + ", " + y[2] + ")");
        check(step.o.size() == 5 && last(step.o).t == 12.0F, "five keys in twelve ticks");

        Gestures.Def pull = Gestures.get(GestureId.THANOS_PULL);
        assertPose(pose(keyAt(pull.o, 0)), new float[]{-90, 0, -10}, 1.0E-4, "the palm is held out level to begin with");
        check(keyAt(pull.o, 10).x > keyAt(pull.o, 5).x, "and dragged back and down");
        check(pull.lean.scalar(10.0F, 0.0F) < 0.0F, "while he leans away from the pull");
        assertNear(pull.lean.value(pull.lean.size() - 1)[0], 0.0, 1.0E-6, "and stands straight at the end");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFlickTheSightAndTheTimeStoneEachHaveTheirOwnMove(GameTestHelper helper) {
        Gestures.Def warp = Gestures.get(GestureId.THANOS_WARP);
        check(keyAt(warp.o, 2).mode == Gestures.Mode.AIM && keyAt(warp.o, 6).mode == Gestures.Mode.AIM, "the flick is aimed at the creature he looks at");
        check(keyAt(warp.o, 2).tremble > 1.0F, "the fingers are spread and trembling");

        Gestures.Def sight = Gestures.get(GestureId.THANOS_SIGHT);
        check(keyAt(sight.o, 4).x < -140.0F && keyAt(sight.o, 9).x > keyAt(sight.o, 4).x, "the Gauntlet goes up to the eye, then comes across");
        check(keyAt(sight.o, 4).y < 0.0F && keyAt(sight.o, 9).y > 0.0F, "from the outside of the face to the inside");
        float[] turned = new float[3];
        check(sight.head.sample(9.0F, turned) && turned[1] > 0.0F, "and his head turns with it");

        Gestures.Def rewind = Gestures.get(GestureId.THANOS_REWIND);
        assertPose(pose(keyAt(rewind.o, 4)), new float[]{-120, 0, -40}, 1.0E-4, "the palm goes up and out");
        check(keyAt(rewind.o, 4).tremble > 1.0F && keyAt(rewind.o, 12).tremble > 1.0F, "trembling");
        check(keyAt(rewind.o, 16).x > keyAt(rewind.o, 12).x && rewind.lean.scalar(16.0F, 0.0F) < 0.0F, "then drawn back, and he leans away");

        Gestures.Def lock = Gestures.get(GestureId.THANOS_TIMELOCK);
        assertPose(pose(keyAt(lock.m, 6)), new float[]{-80, 20, 20}, 1.0E-4, "both palms go up and forward");
        check(keyAt(lock.m, 10).x > keyAt(lock.m, 6).x, "then press down");
        check(lock.drop.scalar(10.0F, 0.0F) > lock.drop.scalar(6.0F, 0.0F) && lock.lean.scalar(10.0F, 0.0F) > 0.0F, "and the whole body goes down with the stamp");
        assertNear(lock.drop.scalar(20.0F, -1.0F), 0.0, 1.0E-6, "and rises again");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hand-overs

    /**
     * The Veil's closing gesture ends on the pose of the stance that takes over: the Gauntlet arm held, the main arm
     * spread by the stance's own added angle, the head back as the stance has it; and the opening begins from it.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theVeilClosesIntoItsStanceAndOpensOutOfIt(GameTestHelper helper) {
        Gestures.Def on = Gestures.get(GestureId.THANOS_VEIL_ON);
        assertPose(pose(last(on.o)), ThanosGestures.VEIL_O, 1.0E-4, "the Veil closes with the Gauntlet hand where the stance holds it");
        assertPose(pose(last(on.m)), new float[]{0, 0, ThanosGestures.VEIL_M_Z}, 1.0E-4, "and the other arm spread by the stance's angle, level with the body");
        assertNear(on.head.value(on.head.size() - 1)[0], ThanosGestures.VEIL_HEAD, 1.0E-4, "the head is where the stance has it");
        assertPose(pose(keyAt(on.m, 5)), new float[]{-20, 0, 45}, 1.0E-4, "on the way both arms go out wide");

        Gestures.Def off = Gestures.get(GestureId.THANOS_VEIL_OFF);
        assertPose(pose(off.o.get(0)), ThanosGestures.VEIL_O, 1.0E-4, "the Veil opens from the stance's pose, Gauntlet arm");
        assertPose(pose(off.m.get(0)), new float[]{0, 0, ThanosGestures.VEIL_M_Z}, 1.0E-4, "and the other");
        assertNear(off.head.value(0)[0], ThanosGestures.VEIL_HEAD, 1.0E-4, "and the head");
        check(last(off.m).z < keyAt(off.m, 4).z && keyAt(off.m, 4).z > ThanosGestures.VEIL_M_Z, "the arms open wider and then drop");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDrainsChainLashesOutAndHandsOverToItsStance(GameTestHelper helper) {
        Gestures.Def cast = Gestures.get(GestureId.THANOS_DRAIN_CAST);
        Gestures.ArmKey end = last(cast.o);
        check(end.mode == Gestures.Mode.AIM && end.target == Gestures.Target.LOOK, "the chain ends with the Gauntlet aimed along the look, as the stance has it");
        assertPose(pose(end), new float[]{0, 0, 0}, 1.0E-4, "with no offset, as the stance has none");
        assertNear(end.tremble, 1.4, 1.0E-4, "and the stance's tremble");
        assertNear(end.trembleSpeed, 1.9, 1.0E-4, "at the stance's speed");
        // The lash leans him in, and the stance's own lean (backwards) takes over from nothing: the gesture's ends at 0.
        check(cast.lean.scalar(2.0F, 0.0F) > 0.0F, "the lash throws him forward");
        assertNear(cast.lean.value(cast.lean.size() - 1)[0], 0.0, 1.0E-6, "and the gesture gives the lean back to the stance");
        assertNear(ThanosGestures.DRAIN_LEAN, -2.0, 1.0E-6, "which leans him back a little");
        check(cast.m.isEmpty(), "the stance braces the main arm, the gesture leaves it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theLeanOfTheDrainIsTheStancesAndGoesWithItsWeight(GameTestHelper helper) {
        HeroAnimState state = new HeroAnimState(UUID.randomUUID());
        assertNear(hero().lean(state), 0.0, 1.0E-6, "no stance, no lean");
        state.stance[Stance.THANOS_DRAIN.ordinal()] = 1.0F;
        assertNear(hero().lean(state), -2.0, 1.0E-6, "the Drain leans him back two degrees");
        state.stance[Stance.THANOS_DRAIN.ordinal()] = 0.5F;
        assertNear(hero().lean(state), -1.0, 1.0E-6, "by its weight");
        state.stance[Stance.THANOS_DRAIN.ordinal()] = 0.0F;
        state.stance[Stance.THANOS_VEIL.ordinal()] = 1.0F;
        assertNear(hero().lean(state), 0.0, 1.0E-6, "the Veil leans him nowhere");
        assertNear(hero().drop(state), 0.0, 1.0E-6, "and sinks him nowhere");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisStancesFollowTheVeilAndTheDrain(GameTestHelper helper) {
        PowerData data = new PowerData();
        boolean[] on = new boolean[Stance.COUNT];
        ThanosGestures.stances(data, on);
        check(!on[Stance.THANOS_VEIL.ordinal()] && !on[Stance.THANOS_DRAIN.ordinal()], "with nothing up neither stance is on");
        data.setToggle(Toggle.REALITY_VEIL, true);
        ThanosGestures.stances(data, on);
        check(on[Stance.THANOS_VEIL.ordinal()] && !on[Stance.THANOS_DRAIN.ordinal()], "the Veil up holds its stance");
        data.set(Live.SOUL_DRAINING, true);
        ThanosGestures.stances(data, on);
        check(on[Stance.THANOS_VEIL.ordinal()] && on[Stance.THANOS_DRAIN.ordinal()], "the Drain holding holds its own");
        data.setToggle(Toggle.REALITY_VEIL, false);
        data.set(Live.SOUL_DRAINING, false);
        data.set(Live.SOUL_SIGHT, true);
        ThanosGestures.stances(data, on);
        check(!on[Stance.THANOS_VEIL.ordinal()] && !on[Stance.THANOS_DRAIN.ordinal()], "the Sight is a gesture and has no stance");
        check(!Stance.THANOS_VEIL.aimed && Stance.THANOS_DRAIN.aimed, "the Veil is ambient and the Drain is an aimed stance (the arm it holds counts as occupied)");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The routes

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisAbilitiesAreActedAndNoneIsPending(GameTestHelper helper) {
        String[] expected = {"cooldown", "cooldown", "cooldown", "live:SOUL_DRAINING", "cooldown"};
        for (int ability = 1; ability <= 5; ability++) {
            String how = GestureRoutes.acting(Power.THANOS, ability);
            check(how.equals(expected[ability - 1]), "ability " + ability + " is acted by " + expected[ability - 1] + ", not " + how);
            check(!how.equals(GestureRoutes.PENDING_ACTING), "ability " + ability + " is not pending");
        }
        check(GestureRoutes.toggle(Power.THANOS, Toggle.REALITY_VEIL, true) == GestureId.THANOS_VEIL_ON, "the Veil going up closes round him");
        check(GestureRoutes.toggle(Power.THANOS, Toggle.REALITY_VEIL, false) == GestureId.THANOS_VEIL_OFF, "and going down opens");
        check(GestureRoutes.toggle(Power.THANOS, Toggle.REALITY_VEIL, true) != GestureRoutes.PENDING_GESTURE, "the Veil is not pending");
        for (Power other : Power.values()) {
            if (other != Power.THANOS) {
                check(GestureRoutes.toggle(other, Toggle.REALITY_VEIL, true) == null && GestureRoutes.toggle(other, Toggle.REALITY_VEIL, false) == null,
                        "nobody else veils reality: " + other);
            }
        }
        check(GestureRoutes.live(Power.THANOS, Live.SOUL_DRAINING, false) == GestureId.THANOS_DRAIN_CAST, "the chain of the Drain lashes out");
        check(GestureRoutes.live(Power.THANOS, Live.SOUL_SIGHT, false) == GestureId.THANOS_SIGHT, "the Sight goes to the eye");
        for (Live live : new Live[]{Live.SOUL_DRAINING, Live.SOUL_SIGHT}) {
            check(GestureRoutes.reason(live) == null, live + " has a gesture, so it needs no reason");
            for (Power other : Power.values()) {
                if (other != Power.THANOS) {
                    check(GestureRoutes.live(other, live, true) == null, "nobody else drains or sees souls: " + other);
                }
            }
        }
        for (GestureRoutes.Flag flag : GestureRoutes.Flag.values()) {
            check(GestureRoutes.flag(Power.THANOS, flag, true, false, false) == null && GestureRoutes.flag(Power.THANOS, flag, false, false, false) == null,
                    "no flag of the older heroes is his: " + flag);
        }
        check(GestureRoutes.grab(Power.THANOS) == null && GestureRoutes.fling(Power.THANOS) == null, "he holds nothing with telekinesis");
        check(GestureRoutes.live(Power.LOKI, Live.CASTING, false) == GestureId.LOKI_CAST && GestureRoutes.grab(Power.BILLY) == GestureId.TK_GRAB_BILLY,
                "the older heroes are left as they were");
        helper.succeed();
    }

    /** The two small probes of the completeness check (1 and 99 ticks left), and what each ability answers. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aCooldownThatJustBeganIsTheFirstUseOfItsKey(GameTestHelper helper) {
        for (long left : new long[]{1, 99}) {
            check(GestureRoutes.cooldown(Power.THANOS, 1, left) == GestureId.THANOS_BLAST, "R with " + left + " left is the Blast");
            check(GestureRoutes.cooldown(Power.THANOS, 2, left) == GestureId.THANOS_STEP, "G with " + left + " left is the Step");
            check(GestureRoutes.cooldown(Power.THANOS, 3, left) == GestureId.THANOS_WARP, "V with " + left + " left is the Transmute");
            check(GestureRoutes.cooldown(Power.THANOS, 4, left) == null, "B with " + left + " left is nothing: the Drain and the Sight are told by their states");
            check(GestureRoutes.cooldown(Power.THANOS, 5, left) == GestureId.THANOS_TIMELOCK, "N with " + left + " left is the Lock");
        }
        helper.succeed();
    }

    /**
     * The two uses of R, G and N start cooldowns of different lengths; what is left when the data arrives (a few ticks
     * short of the whole) tells them apart, by whichever configured cooldown it is nearer to.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theLengthOfACooldownTellsTheTwoUsesOfAKeyApart(GameTestHelper helper) {
        int blast = ThanosConfig.POWER_BLAST_COOLDOWN.get();
        int slam = ThanosConfig.SLAM_COOLDOWN.get();
        int step = ThanosConfig.SPACE_STEP_COOLDOWN.get();
        int pull = ThanosConfig.PULL_COOLDOWN.get();
        int rewind = ThanosConfig.REWIND_COOLDOWN.get();
        int lock = ThanosConfig.TIME_LOCK_COOLDOWN.get();
        // The data arrives up to five ticks after the cooldown began.
        for (int late = 0; late <= 5; late++) {
            check(ThanosGestures.cooldown(1, blast - late) == GestureId.THANOS_BLAST, "the Blast, " + late + " ticks late");
            check(ThanosGestures.cooldown(1, slam - late) == GestureId.THANOS_SLAM, "the Slam, " + late + " ticks late");
            check(ThanosGestures.cooldown(2, step - late) == GestureId.THANOS_STEP, "the Step, " + late + " ticks late");
            check(ThanosGestures.cooldown(2, pull - late) == GestureId.THANOS_PULL, "the Pull, " + late + " ticks late");
            check(ThanosGestures.cooldown(5, rewind - late) == GestureId.THANOS_REWIND, "the Rewind, " + late + " ticks late");
            check(ThanosGestures.cooldown(5, lock - late) == GestureId.THANOS_TIMELOCK, "the Lock, " + late + " ticks late");
            check(ThanosGestures.cooldown(3, ThanosConfig.TRANSMUTE_COOLDOWN.get() - late) == GestureId.THANOS_WARP, "the Transmute, " + late + " ticks late");
        }
        // The plug-in answers the same.
        check(hero().cooldownGesture(1, slam - 2) == GestureId.THANOS_SLAM && hero().cooldownGesture(5, rewind - 2) == GestureId.THANOS_REWIND,
                "the plug-in asks the same table");

        // And it follows the configuration: a Slam made quicker than the Blast, and a Pull as quick as the Step.
        check(ThanosGestures.cooldown(1, 30, 70, 20, 100, 220, 900, 600) == GestureId.THANOS_SLAM, "a Slam that rests 20 ticks is told by being nearer 20 than 70");
        check(ThanosGestures.cooldown(1, 66, 70, 20, 100, 220, 900, 600) == GestureId.THANOS_BLAST, "and the Blast by being nearer 70");
        check(ThanosGestures.cooldown(2, 98, 100, 220, 100, 100, 900, 600) == GestureId.THANOS_STEP, "when two uses rest alike, nothing tells them apart: the first plays");
        check(ThanosGestures.cooldown(5, 590, 70, 160, 100, 220, 300, 600) == GestureId.THANOS_TIMELOCK, "the Lock, with a Rewind that rests 300");
        // Nothing for the keys the cooldown does not tell: B, and a key with no ability.
        check(ThanosGestures.cooldown(4, 400, 70, 160, 100, 220, 900, 600) == null && ThanosGestures.cooldown(0, 100, 70, 160, 100, 220, 900, 600) == null,
                "the Soul Stone's cooldowns play nothing");
        helper.succeed();
    }

    /**
     * Every one of his gestures can be reached: by the cooldown of the key it belongs to, by his toggle or by one of
     * his states. (The completeness check probes two small cooldowns and so reaches only the first use of a key; the
     * second use of R, G and N is declared as started elsewhere, and this proves that it is.)
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyGestureOfHisIsStartedBySomething(GameTestHelper helper) {
        Set<GestureId> reached = EnumSet.noneOf(GestureId.class);
        int[] cooldowns = {ThanosConfig.POWER_BLAST_COOLDOWN.get(), ThanosConfig.SLAM_COOLDOWN.get(), ThanosConfig.SPACE_STEP_COOLDOWN.get(),
                ThanosConfig.PULL_COOLDOWN.get(), ThanosConfig.TRANSMUTE_COOLDOWN.get(), ThanosConfig.REWIND_COOLDOWN.get(), ThanosConfig.TIME_LOCK_COOLDOWN.get()};
        for (int ability = 1; ability <= 5; ability++) {
            for (int cooldown : cooldowns) {
                GestureId id = GestureRoutes.cooldown(Power.THANOS, ability, cooldown - 2);
                if (id != null) {
                    reached.add(id);
                }
            }
        }
        reached.add(GestureRoutes.toggle(Power.THANOS, Toggle.REALITY_VEIL, true));
        reached.add(GestureRoutes.toggle(Power.THANOS, Toggle.REALITY_VEIL, false));
        for (Live live : Live.values()) {
            GestureId id = GestureRoutes.live(Power.THANOS, live, false);
            if (id != null) {
                reached.add(id);
            }
        }
        for (GestureId id : HIS) {
            check(reached.contains(id), id + " is reached by no cooldown, toggle or state of his");
            boolean told = ThanosGestures.toldByCooldown(id);
            check(hero().startedElsewhere(id) == told, id + (told ? " is declared as started elsewhere (the length of a cooldown tells it)" : " is routed, so needs no declaration"));
            check(GestureRoutes.startedElsewhere(id) == told, id + " is answered by the shared check the same way");
        }
        check(!hero().startedElsewhere(GestureId.TAKEOFF_THOR) && !hero().startedElsewhere(GestureId.LOOK_CHANGE) && !hero().startedElsewhere(GestureId.LAND_SOFT),
                "only his are told");
        check(hero().takeoffGesture() == null, "he has no take-off: he does not fly");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The key presses

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisKeyPressesActBeforeTheServerAnswersOnlyWhereTheyWillWork(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 1000L;
        // R: the Blast fires at the sky as readily as at a creature; the Slam needs only the ground.
        check(ThanosGestures.predicted(data, 1, now, false, true, false, false, false) == GestureId.THANOS_BLAST, "R, rested: the Blast");
        check(ThanosGestures.predicted(data, 1, now, true, true, false, false, false) == GestureId.THANOS_SLAM, "Sneak + R, rested: the Slam");
        check(ThanosGestures.predicted(data, 1, now, false, false, true, true, true) == null, "R, held or starving: the server will turn him away");
        data.startCooldown(1, now, 70);
        check(ThanosGestures.predicted(data, 1, now, false, true, true, true, true) == null, "R, still resting: nothing");
        check(ThanosGestures.predicted(data, 1, now + 70, false, true, false, false, false) == GestureId.THANOS_BLAST, "R, rested again");

        // G: the Step needs something to step to, the Pull a creature.
        data = new PowerData();
        check(ThanosGestures.predicted(data, 2, now, false, true, false, true, false) == GestureId.THANOS_STEP, "G, with a block in range: the Step");
        check(ThanosGestures.predicted(data, 2, now, false, true, true, true, false) == GestureId.THANOS_STEP, "G, with a creature in range: the Step");
        check(ThanosGestures.predicted(data, 2, now, false, true, false, false, false) == null, "G at nothing: the server answers with a message");
        check(ThanosGestures.predicted(data, 2, now, true, true, true, true, false) == GestureId.THANOS_PULL, "Sneak + G at a creature: the Pull");
        check(ThanosGestures.predicted(data, 2, now, true, true, false, true, false) == null, "Sneak + G at a block: nothing to pull");

        // V: the Veil switches at once whatever the Transmute is doing; the Transmute needs a creature.
        check(ThanosGestures.predicted(data, 3, now, false, true, true, true, false) == GestureId.THANOS_WARP, "V at a creature: the flick");
        check(ThanosGestures.predicted(data, 3, now, false, true, false, true, false) == null, "V at nothing: no flick");
        check(ThanosGestures.predicted(data, 3, now, true, true, false, false, false) == GestureId.THANOS_VEIL_ON, "Sneak + V with the Veil down: it closes");
        data.setToggle(Toggle.REALITY_VEIL, true);
        check(ThanosGestures.predicted(data, 3, now, true, true, false, false, false) == GestureId.THANOS_VEIL_OFF, "Sneak + V with the Veil up: it opens");
        check(ThanosGestures.predicted(data, 3, now, true, false, false, false, false) == null, "Sneak + V, held: the server refuses it");
        data.startCooldown(3, now, 240);
        check(ThanosGestures.predicted(data, 3, now, true, true, false, false, false) == GestureId.THANOS_VEIL_OFF, "the Veil does not wait for the Transmute to rest");
        check(ThanosGestures.predicted(data, 3, now, false, true, true, true, false) == null, "but the Transmute does");

        // B: the Drain needs a creature, the Sight only to be rested.
        data = new PowerData();
        check(ThanosGestures.predicted(data, 4, now, false, true, true, true, false) == GestureId.THANOS_DRAIN_CAST, "B at a creature: the chain");
        check(ThanosGestures.predicted(data, 4, now, false, true, false, true, false) == null, "B at nothing: no chain");
        check(ThanosGestures.predicted(data, 4, now, true, true, false, false, false) == GestureId.THANOS_SIGHT, "Sneak + B: the Sight");
        data.set(Live.SOUL_DRAINING, true);
        check(ThanosGestures.predicted(data, 4, now, false, true, true, true, false) == null, "B while draining: the gesture is not played twice");
        data.set(Live.SOUL_DRAINING, false);
        data.startCooldown(4, now, 400);
        check(ThanosGestures.predicted(data, 4, now, true, true, false, false, false) == null, "Sneak + B while the Sight rests: nothing");

        // N: the Rewind needs two seconds of history, the Lock only to be rested.
        data = new PowerData();
        check(ThanosGestures.predicted(data, 5, now, false, true, false, false, true) == GestureId.THANOS_REWIND, "N with history: the palm goes up");
        check(ThanosGestures.predicted(data, 5, now, false, true, false, false, false) == null, "N with none: the server says there is nothing to rewind");
        check(ThanosGestures.predicted(data, 5, now, true, true, false, false, false) == GestureId.THANOS_TIMELOCK, "Sneak + N: the stamp");
        data.startCooldown(5, now, 600);
        check(ThanosGestures.predicted(data, 5, now, true, true, false, false, true) == null && ThanosGestures.predicted(data, 5, now, false, true, false, false, true) == null,
                "N while resting: nothing");
        check(ThanosGestures.predicted(new PowerData(), 6, now, false, true, true, true, true) == null, "a key with no ability does nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Flight, the walk and the landing

    @GameTest(template = ARENA, batch = BATCH)
    public static void heDoesNotFlyAndHasNoFlightProfile(GameTestHelper helper) {
        check(!Power.THANOS.canFly(), "Thanos does not fly");
        check(FlightProfile.of(Power.THANOS, false) == null, "so he has no flight profile: no take-off, banking or braking");
        check(hero().takeoffGesture() == null, "and no take-off gesture");
        helper.succeed();
    }

    /**
     * A Titan's landing is for a fall of more than three blocks: just after nine ticks of falling, when the speed
     * first passes 0.6 blocks per tick. A drop from a fence or a stair is nothing, as it is for anyone.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aLandingIsATitansFromMoreThanThreeBlocks(GameTestHelper helper) {
        double speed = 0.0;
        double fallen = 0.0;
        int firstHard = -1;
        double fellThen = 0.0;
        for (int tick = 1; tick <= 30 && firstHard < 0; tick++) {
            speed = (speed + 0.08) * 0.98;
            fallen += speed;
            if (speed >= ThanosGestures.HARD_LANDING) {
                firstHard = tick;
                fellThen = fallen;
            }
        }
        check(firstHard >= 0, "a falling player does reach the speed of a hard landing");
        check(fellThen > 3.0, "the first speed that counts is that of a fall of more than three blocks, " + fellThen);
        // One tick earlier it was less than three blocks.
        double speedBefore = 0.0;
        double fallenBefore = 0.0;
        for (int tick = 1; tick < firstHard; tick++) {
            speedBefore = (speedBefore + 0.08) * 0.98;
            fallenBefore += speedBefore;
        }
        check(fallenBefore < 3.0 && speedBefore < ThanosGestures.HARD_LANDING, "and one tick before it was a fall of " + fallenBefore + " blocks, which is not");
        helper.succeed();
    }
}
