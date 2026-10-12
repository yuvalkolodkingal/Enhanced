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
import com.kolodkingal.enhanced.client.render.UltronCubes;
import com.kolodkingal.enhanced.client.render.UltronGestures;
import com.kolodkingal.enhanced.client.render.UltronPose;
import com.kolodkingal.enhanced.client.render.UltronPoses;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The client side of Ultron as far as a server can check it: the hero's gestures (their timing and keys, how they hand
 * over to the stances and to his flight), the routes that start them, the key presses that act before the server
 * answers, his flight profile and his eyes; and the boss: the table of his body, the pose of every state, and the rule
 * of the fight that every blow has a tell in the body before it lands. What the model, the lights and the effects look
 * like in the world is checked by eye in a real game ({@code /enhanced_model ultron}, {@code /enhanced_ultron_fx}).
 *
 * <p>Pure checks only: no client class appears in a test method, and the plug-in is reached through its interface (it
 * is built on a server too, so that these tests can read it).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class UltronClientTests {
    private static final String BATCH = "ultron_client_pure";

    /** His ten gestures. */
    private static final List<GestureId> HIS = List.of(GestureId.TAKEOFF_ULTRON, GestureId.ULTRON_FIRE_START, GestureId.ULTRON_CHARGE_START,
            GestureId.ULTRON_BLAST, GestureId.ULTRON_PULL, GestureId.ULTRON_SLAM, GestureId.ULTRON_UPLOAD, GestureId.ULTRON_HACK,
            GestureId.ULTRON_ARMY, GestureId.ULTRON_COMMAND);

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

    // ------------------------------------------------------------------------------------------------------------
    // The gestures

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisTenGesturesAreTimedAsDesigned(GameTestHelper helper) {
        // id, group, duration, attack, release, force
        Object[][] table = {
                {GestureId.TAKEOFF_ULTRON, GestureId.Group.TAKEOFF, 18, 2, 8, true},
                {GestureId.ULTRON_FIRE_START, GestureId.Group.CAST, 8, 1, 4, false},
                {GestureId.ULTRON_CHARGE_START, GestureId.Group.CAST, 12, 3, 4, false},
                {GestureId.ULTRON_BLAST, GestureId.Group.CAST, 12, 1, 5, true},
                {GestureId.ULTRON_PULL, GestureId.Group.CAST, 16, 2, 6, true},
                {GestureId.ULTRON_SLAM, GestureId.Group.CAST, 22, 3, 8, true},
                {GestureId.ULTRON_UPLOAD, GestureId.Group.CAST, 20, 2, 6, false},
                {GestureId.ULTRON_HACK, GestureId.Group.CAST, 22, 2, 6, false},
                {GestureId.ULTRON_ARMY, GestureId.Group.CAST, 22, 2, 8, false},
                {GestureId.ULTRON_COMMAND, GestureId.Group.CAST, 14, 2, 5, false},
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
        // The blow of the Overcharge takes over from the gathering of it, in the same group.
        check(GestureId.ULTRON_BLAST.group == GestureId.ULTRON_CHARGE_START.group, "the blast takes the place of the charge's gesture");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisTwoArmedGesturesMirrorTheirArmsAndTheOthersUseOnlyOne(GameTestHelper helper) {
        for (GestureId id : new GestureId[]{GestureId.TAKEOFF_ULTRON, GestureId.ULTRON_SLAM, GestureId.ULTRON_UPLOAD, GestureId.ULTRON_ARMY}) {
            Gestures.Def def = Gestures.get(id);
            check(def.m.size() == def.o.size() && !def.m.isEmpty(), id + " moves both arms through the same keys");
            for (int k = 0; k < def.m.size(); k++) {
                Gestures.ArmKey m = def.m.get(k);
                Gestures.ArmKey o = def.o.get(k);
                check(m.t == o.t, id + " key " + k + " is at the same tick for both arms");
                assertNear(o.x, m.x, 1.0E-4, id + " key " + k + ": both arms swing the same way");
                // (The upload crosses the hands: the turn is in mirror too.)
                assertNear(o.y, -m.y, 1.0E-4, id + " key " + k + ": the arms turn in mirror (y)");
                assertNear(o.z, -m.z, 1.0E-4, id + " key " + k + ": the arms spread in mirror (z)");
            }
        }
        // One hand held out: the main hand only.
        for (GestureId id : new GestureId[]{GestureId.ULTRON_PULL, GestureId.ULTRON_HACK, GestureId.ULTRON_COMMAND}) {
            Gestures.Def def = Gestures.get(id);
            check(!def.m.isEmpty() && def.o.isEmpty(), id + " moves the main hand and leaves the other alone");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theTakeOffPushesTheAirDownAndOutAndEndsOnTheHover(GameTestHelper helper) {
        Gestures.Def def = Gestures.get(GestureId.TAKEOFF_ULTRON);
        // The palms push the air down and out (a negative x swings an arm forward, a positive z spreads it) before they settle.
        check(def.m.get(1).x < def.m.get(0).x - 15.0F && def.m.get(1).z > def.m.get(0).z + 10.0F, "the palms push forward and out first");
        check(def.drop.isEmpty(), "the anti-gravity lifts him at once: no dip before it");
        FlightProfile profile = FlightProfile.of(Power.ULTRON, false);
        float[] hover = {profile.anchor(FlightProfile.HOVER, 0), profile.anchor(FlightProfile.HOVER, 1), profile.anchor(FlightProfile.HOVER, 2)};
        assertPose(pose(last(def.m)), hover, 0.5, "the take-off ends on the hover pose, main arm");
        for (int i = 0; i < 3; i++) {
            assertNear(pose(last(def.o))[i], profile.anchor(FlightProfile.HOVER, 3 + i), 0.5, "and the off arm " + i);
        }
        assertNear(def.right.value(def.right.size() - 1)[0], profile.anchor(FlightProfile.HOVER, 6), 0.5, "and the right leg");
        assertNear(def.left.value(def.left.size() - 1)[0], profile.anchor(FlightProfile.HOVER, 8), 0.5, "and the left leg");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theUploadGlitchesHisHeadBeforeTheArmsAreThrownWide(GameTestHelper helper) {
        Gestures.Def def = Gestures.get(GestureId.ULTRON_UPLOAD);
        // The head jerks one way, then the other, then settles, while the arms are held drawn in.
        float previous = 0.0F;
        int turns = 0;
        float widest = 0.0F;
        for (int k = 0; k < def.head.size(); k++) {
            float yaw = def.head.value(k)[1];
            if (yaw * previous < 0.0F) {
                turns++;
            }
            if (yaw != 0.0F) {
                previous = yaw;
            }
            widest = Math.max(widest, Math.abs(yaw));
        }
        check(turns >= 2 && widest >= 10.0F, "the head jerks both ways (" + turns + " turns, up to " + widest + " degrees)");
        assertNear(def.head.value(def.head.size() - 1)[1], 0.0, 1.0E-6, "and ends straight");
        Gestures.ArmKey held = def.m.get(1);
        for (int k = 2; k < 5; k++) {
            assertPose(pose(def.m.get(k)), pose(held), 1.0E-4, "the arms stay drawn in while the head glitches, key " + k);
        }
        check(def.m.get(5).z > 60.0F, "and are thrown wide after it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void thePlasmaGesturesHandOverToTheStancesAndTheBlastBeginsWhereTheChargeEnds(GameTestHelper helper) {
        // R held: both palms along the look, as the stance has them.
        Gestures.Def fire = Gestures.get(GestureId.ULTRON_FIRE_START);
        Gestures.ArmKey fm = last(fire.m);
        Gestures.ArmKey fo = last(fire.o);
        check(fm.mode == Gestures.Mode.AIM && fm.target == Gestures.Target.LOOK && fo.mode == Gestures.Mode.AIM && fo.target == Gestures.Target.LOOK,
                "the first bolt is fired with both palms aimed along the look");
        assertPose(pose(fm), UltronGestures.FIRE_M_AIM, 1.0E-4, "the main palm ends on the stance's aim");
        assertPose(pose(fo), UltronGestures.FIRE_O_AIM, 1.0E-4, "and the off palm");
        check(fire.lean.isEmpty() && fire.head.isEmpty(), "the stance carries the lean and the head, so they are not thrown back twice");

        // The Overcharge: the main palm aimed and raised, the other hand across.
        Gestures.Def charge = Gestures.get(GestureId.ULTRON_CHARGE_START);
        Gestures.ArmKey cm = last(charge.m);
        check(cm.mode == Gestures.Mode.AIM && cm.target == Gestures.Target.LOOK, "the charge ends with the palm aimed along the look");
        assertPose(pose(cm), UltronGestures.CHARGE_M_AIM, 1.0E-4, "on the stance's aim");
        assertPose(pose(last(charge.o)), UltronGestures.CHARGE_O, 1.0E-4, "with the stance's brace for the other hand");
        assertNear(charge.head.value(charge.head.size() - 1)[0], UltronGestures.CHARGE_HEAD, 1.0E-4, "and its head");
        assertNear(charge.lean.value(charge.lean.size() - 1)[0], UltronGestures.CHARGE_LEAN, 1.0E-4, "and its lean");

        // The blast starts there, is thrown up by the shot, and ends at rest.
        Gestures.Def blast = Gestures.get(GestureId.ULTRON_BLAST);
        assertPose(pose(blast.m.get(0)), UltronGestures.CHARGE_M_AIM, 1.0E-4, "the blast starts where the charge was, main arm");
        assertPose(pose(blast.o.get(0)), UltronGestures.CHARGE_O, 1.0E-4, "and the other hand");
        check(blast.m.get(1).x < blast.m.get(0).x - 10.0F, "the shot throws the palm up");
        check(blast.lean.value(1)[0] < UltronGestures.CHARGE_LEAN - 3.0F, "and shoves the body back");
        assertNear(blast.lean.value(blast.lean.size() - 1)[0], 0.0, 1.0E-4, "then it settles with the body upright");
        assertNear(blast.head.value(blast.head.size() - 1)[0], 0.0, 1.0E-4, "and the head straight");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theGestureThatCarriesTheChargeTakesTheStancesLean(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.ULTRON);
        HeroAnimState state = new HeroAnimState(UUID.randomUUID());
        state.stance[Stance.ULTRON_CHARGE.ordinal()] = 1.0F;
        assertNear(UltronGestures.owningTheCharge(state), 0.0, 1.0E-6, "nothing plays: the stance holds the palm");
        assertNear(hero.lean(state), UltronGestures.CHARGE_LEAN, 1.0E-6, "and its lean");

        HeroAnimState.Slot slot = state.slots[0];
        slot.active = true;
        slot.id = GestureId.ULTRON_CHARGE_START;
        slot.weight = 0.5F;
        assertNear(UltronGestures.owningTheCharge(state), 0.5, 1.0E-6, "the gesture of the charge carries half of it");
        assertNear(hero.lean(state), UltronGestures.CHARGE_LEAN * 0.5, 1.0E-6, "the stance gives up half its lean");
        slot.weight = 1.0F;
        assertNear(hero.lean(state), 0.0, 1.0E-6, "all of it while the gesture holds the palm");
        slot.id = GestureId.ULTRON_BLAST;
        assertNear(UltronGestures.owningTheCharge(state), 1.0, 1.0E-6, "the blast's gesture carries it too");
        slot.id = GestureId.ULTRON_PULL;
        assertNear(UltronGestures.owningTheCharge(state), 0.0, 1.0E-6, "a gesture of gravity does not");
        slot.id = GestureId.ULTRON_BLAST;
        slot.active = false;
        assertNear(hero.lean(state), UltronGestures.CHARGE_LEAN, 1.0E-6, "and one that has finished does not either");

        state.stance[Stance.ULTRON_CHARGE.ordinal()] = 0.0F;
        state.stance[Stance.ULTRON_FIRE.ordinal()] = 1.0F;
        assertNear(hero.lean(state), UltronGestures.FIRE_LEAN, 1.0E-6, "firing leans the body back a little");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The routes

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisAbilitiesAreActedAndNoneIsPending(GameTestHelper helper) {
        String[] expected = {"flag:BEAM", "flight", null, "cooldown", "cooldown"};
        for (int ability = 1; ability <= 5; ability++) {
            String how = GestureRoutes.acting(Power.ULTRON, ability);
            check(!how.equals(GestureRoutes.PENDING_ACTING), "ability " + ability + " is not pending");
            if (expected[ability - 1] != null) {
                check(how.equals(expected[ability - 1]), "ability " + ability + " is acted by " + expected[ability - 1] + ", not " + how);
            } else {
                check(how.startsWith("none: ") && how.contains("payload"), "the Pull and the Slam say they are told by their payload: " + how);
            }
        }
        check(GestureRoutes.flag(Power.ULTRON, GestureRoutes.Flag.BEAM, true, false, false) == GestureId.ULTRON_FIRE_START, "R down: both palms snap up");
        check(GestureRoutes.flag(Power.ULTRON, GestureRoutes.Flag.BEAM, false, false, false) == null, "R up: the stance lets go by itself");
        check(GestureRoutes.cooldown(Power.ULTRON, 1, 160) == GestureId.ULTRON_BLAST, "the Overcharge's cooldown starts when its blast flies");
        check(GestureRoutes.cooldown(Power.ULTRON, 4, 500) == GestureId.ULTRON_UPLOAD && GestureRoutes.cooldown(Power.ULTRON, 4, 6000) == GestureId.ULTRON_UPLOAD,
                "the Upload and Second Life are one gesture");
        check(GestureRoutes.cooldown(Power.ULTRON, 5, 400) == GestureId.ULTRON_ARMY, "the Sentry Army");
        check(GestureRoutes.cooldown(Power.ULTRON, 2, 20) == null && GestureRoutes.cooldown(Power.ULTRON, 3, 80) == null,
                "flight and the gravity moves are not cooldown routes (the payload tells those)");
        check(GestureRoutes.live(Power.ULTRON, Live.ULTRON_CHARGING, false) == GestureId.ULTRON_CHARGE_START, "the Overcharge gathers");
        check(GestureRoutes.live(Power.ULTRON, Live.CASTING, false) == GestureId.ULTRON_HACK, "the Hack");
        check(GestureRoutes.reason(Live.ULTRON_CHARGING) == null, "the charge has a gesture, so it needs no reason");
        check(GestureRoutes.reason(Live.LIFTED) != null && !GestureRoutes.reason(Live.LIFTED).startsWith(GestureRoutes.PENDING_REASON),
                "being lifted is explained");
        check(GestureRoutes.grab(Power.ULTRON) == null && GestureRoutes.fling(Power.ULTRON) == null, "he has no telekinesis");
        // The older heroes are left as they were.
        check(GestureRoutes.live(Power.LOKI, Live.CASTING, false) == GestureId.LOKI_CAST && GestureRoutes.grab(Power.BILLY) == GestureId.TK_GRAB_BILLY,
                "Loki's casting and Billy's grab are unchanged");
        check(GestureRoutes.flag(Power.SCARLET_WITCH, GestureRoutes.Flag.BEAM, true, false, false) == GestureId.WANDA_BEAM_KICK,
                "and the Scarlet Witch's beam");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyGestureOfHisIsStartedBySomething(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.ULTRON);
        check(hero.takeoffGesture() == GestureId.TAKEOFF_ULTRON, "he takes off with his own gesture");
        for (GestureId id : HIS) {
            boolean told = id == GestureId.TAKEOFF_ULTRON || id == GestureId.ULTRON_PULL || id == GestureId.ULTRON_SLAM || id == GestureId.ULTRON_COMMAND;
            check(hero.startedElsewhere(id) == told, id + (told ? " is told by the animator or a payload" : " is started by a route"));
            boolean routed = GestureRoutes.flag(Power.ULTRON, GestureRoutes.Flag.BEAM, true, false, false) == id
                    || GestureRoutes.cooldown(Power.ULTRON, 1, 1) == id || GestureRoutes.cooldown(Power.ULTRON, 4, 1) == id
                    || GestureRoutes.cooldown(Power.ULTRON, 5, 1) == id || GestureRoutes.live(Power.ULTRON, Live.ULTRON_CHARGING, false) == id
                    || GestureRoutes.live(Power.ULTRON, Live.CASTING, false) == id;
            check(routed != told, id + " is started by exactly one of the two");
            check(GestureRoutes.startedElsewhere(id) == told, id + ": the shared table agrees");
        }
        check(!hero.startedElsewhere(GestureId.TAKEOFF_THOR) && !hero.startedElsewhere(GestureId.LOOK_CHANGE), "and only his are told");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The key presses and the stances

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisKeyPressesActBeforeTheServerAnswersOnlyWhereTheyWillWork(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 1000L;
        UltronGestures.Guards all = UltronGestures.Guards.ALL;
        UltronGestures.Guards none = new UltronGestures.Guards(false, false, false, false, false);

        check(UltronGestures.predicted(data, 1, now, false, all) == GestureId.ULTRON_FIRE_START, "R with plasma: the palms snap up at once");
        check(UltronGestures.predicted(data, 1, now, false, none) == null, "R with a dry pool: the server will say so, so nothing");
        data.setBeamActive(true);
        check(UltronGestures.predicted(data, 1, now, false, all) == null, "R while already firing: not played twice");
        data.setBeamActive(false);
        check(UltronGestures.predicted(data, 1, now, true, all) == GestureId.ULTRON_CHARGE_START, "Sneak + R rested, the pool can pay: the Overcharge gathers");
        check(UltronGestures.predicted(data, 1, now, true, none) == null, "Sneak + R without the plasma for it: nothing");
        data.startCooldown(1, now, 160);
        check(UltronGestures.predicted(data, 1, now, true, all) == null, "Sneak + R while it rests: nothing");
        check(UltronGestures.predicted(data, 1, now + 160, true, all) == GestureId.ULTRON_CHARGE_START, "and again when the rest is over");
        data.set(Live.ULTRON_CHARGING, true);
        check(UltronGestures.predicted(data, 1, now + 160, true, all) == null && UltronGestures.predicted(data, 1, now + 160, false, all) == null,
                "while the charge builds neither key does anything");
        data.set(Live.ULTRON_CHARGING, false);

        data = new PowerData();
        check(UltronGestures.predicted(data, 3, now, false, all) == GestureId.ULTRON_PULL, "V at a creature in sight: the palm goes out");
        check(UltronGestures.predicted(data, 3, now, false, none) == null, "V at nothing: the server does nothing, so no gesture");
        check(UltronGestures.predicted(data, 3, now, true, none) == GestureId.ULTRON_SLAM, "Sneak + V needs no target: the stamp");
        data.startCooldown(3, now, 80);
        check(UltronGestures.predicted(data, 3, now, true, all) == null && UltronGestures.predicted(data, 3, now, false, all) == null,
                "V while the gravity rests: nothing");

        data = new PowerData();
        check(UltronGestures.predicted(data, 4, now, false, all) == GestureId.ULTRON_UPLOAD, "B with a drone to jump into");
        check(UltronGestures.predicted(data, 4, now, false, none) == null, "B with no drone: a message, no gesture");
        check(UltronGestures.predicted(data, 4, now, true, all) == null, "Sneak + B, the Hack, waits for the server");
        check(UltronGestures.predicted(data, 5, now, false, all) == GestureId.ULTRON_ARMY, "N with room for drones");
        check(UltronGestures.predicted(data, 5, now, false, none) == null && UltronGestures.predicted(data, 5, now, true, all) == null,
                "N with no room, and Sneak + N (Command), act when the server says");
        data.startCooldown(5, now, 400);
        check(UltronGestures.predicted(data, 5, now, false, all) == null, "N while the Army rests: nothing");
        check(UltronGestures.predicted(data, 2, now, false, all) == null, "G is acted by the data, not the press");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisStancesComeFromHisData(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.ULTRON);
        HeroAnimState state = new HeroAnimState(UUID.randomUUID());
        boolean[] on = new boolean[Stance.COUNT];
        PowerData data = new PowerData();
        hero.stanceTargets(state, null, data, on, 0L);
        check(!on[Stance.ULTRON_FIRE.ordinal()] && !on[Stance.ULTRON_CHARGE.ordinal()], "at rest, neither");
        data.setBeamActive(true);
        hero.stanceTargets(state, null, data, on, 0L);
        check(on[Stance.ULTRON_FIRE.ordinal()] && !on[Stance.ULTRON_CHARGE.ordinal()], "R held: both palms up");
        data.set(Live.ULTRON_CHARGING, true);
        java.util.Arrays.fill(on, false);
        hero.stanceTargets(state, null, data, on, 0L);
        check(!on[Stance.ULTRON_FIRE.ordinal()] && on[Stance.ULTRON_CHARGE.ordinal()], "the Overcharge builds: the raised palm, and no plasma stream with it");
        check(Stance.ULTRON_FIRE.aimed && Stance.ULTRON_CHARGE.aimed, "both hold the arms, so the swing of an attack is muted");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Flight

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisFlightIsUprightAndStiff(GameTestHelper helper) {
        FlightProfile profile = FlightProfile.of(Power.ULTRON, false);
        check(profile != null && !profile.spring && !profile.mWorld, "he has a plain profile of his own");
        assertNear(profile.pitchTarget(0.0F, 0.0F), 0.0, 1.0E-6, "he hovers upright");
        check(profile.p1 <= 12.0F && profile.p2 <= 36.0F && profile.p2 > profile.p1 + 15.0F,
                "and leans only a little as he speeds up (" + profile.p1 + ", " + profile.p2 + "): a machine, not a diver");
        check(profile.pitchTarget(1.0F, -1.0F) < 40.0F, "a dive tips him over only a little");
        check(profile.noBrake && profile.tauPitchOut < profile.tauPitchIn, "he stops as dead as a machine: no braking gesture, he stands up at once");
        check(profile.lazyRoll == 0.0F && profile.legSway == 0.0F && profile.legFlutter == 0.0F && profile.bobAmp <= 0.03F,
                "a machine does not sway, flutter or bob");
        check(profile.maxRoll <= 8.0F, "and banks only slightly");
        // Palms down, the arms a little out; nearer the body and swept forward the faster he goes.
        check(profile.anchor(FlightProfile.HOVER, 2) > 10.0F, "hovering, the arms are held a little out");
        check(profile.anchor(FlightProfile.SPRINT, 2) < profile.anchor(FlightProfile.CRUISE, 2)
                && profile.anchor(FlightProfile.CRUISE, 2) < profile.anchor(FlightProfile.HOVER, 2), "closer to the body the faster he goes");
        check(profile.anchor(FlightProfile.SPRINT, 0) < profile.anchor(FlightProfile.HOVER, 0), "and swept slightly forward at a sprint");
        assertNear(profile.anchor(FlightProfile.HOVER, 0), profile.anchor(FlightProfile.HOVER, 3), 1.0E-6, "the arms are a pair, main");
        assertNear(profile.anchor(FlightProfile.HOVER, 2), -profile.anchor(FlightProfile.HOVER, 5), 1.0E-6, "mirrored");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // His eyes and the textures

    private static BufferedImage image(String name) {
        try (InputStream in = UltronClientTests.class.getResourceAsStream("/assets/enhanced/textures/entity/" + name + ".png")) {
            check(in != null, name + " is there");
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new AssertionError(name + " cannot be read: " + e);
        }
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisEyesGlowOnTheEyesOfTheSkinInEachLook(GameTestHelper helper) {
        for (String look : new String[]{"prime", "sentry", "human"}) {
            BufferedImage idle = image("ultron_eyes_" + look);
            BufferedImage hot = image("ultron_eyes_" + look + "_hot");
            check(idle.getWidth() == 64 && idle.getHeight() == 64 && hot.getWidth() == 64 && hot.getHeight() == 64, look + ": masks the size of the skin");
            int idleLit = 0;
            int hotLit = 0;
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    idleLit += alpha(idle, x, y) > 0 ? 1 : 0;
                    hotLit += alpha(hot, x, y) > 0 ? 1 : 0;
                }
            }
            // Four pixels wide, two rows, on the face's fifth row (y = 11 and 12 of the texture), like the other heroes'.
            for (int x : new int[]{9, 10, 13, 14}) {
                check(alpha(idle, x, 11) > 0 && alpha(idle, x, 12) > 0, look + ": the eyes are painted at x " + x);
                check(alpha(hot, x, 12) > 0, look + ": and burn brighter when he works");
            }
            check(idleLit == 8, look + ": nothing but the eyes is painted (" + idleLit + ")");
            check(hotLit > idleLit, look + ": the hot mask glows over more");
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    if (alpha(hot, x, y) > 0) {
                        check(x >= 8 && x < 16 && y >= 8 && y < 16, look + ": the hot glow stays on the face (" + x + ", " + y + ")");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theTexturesOfTheBodyAreThereAtTheSizeTheModelReadsThemAt(GameTestHelper helper) {
        // The model reads a 64 x 64 skin and is drawn on 128 x 128 (twice the texel density); the lights are masks of the same size.
        for (String name : new String[]{"ultron", "ultron_form1", "ultron_form2", "ultron_sentry", "ultron_eyes", "ultron_torso", "ultron_fingers",
                "ultron_palms", "ultron_sentry_eyes"}) {
            BufferedImage image = image(name);
            check(image.getWidth() == 2 * UltronCubes.SKIN && image.getHeight() == 2 * UltronCubes.SKIN, name + " is " + image.getWidth() + " x " + image.getHeight());
        }
        BufferedImage key = image("vibranium_key");
        BufferedImage glow = image("vibranium_key_glow");
        check(key.getWidth() == glow.getWidth() && key.getHeight() == glow.getHeight(), "the Key's glow is a mask of its skin");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The boss's body

    @GameTest(template = ARENA, batch = BATCH)
    public static void theTableOfTheBodyHangsEveryBoxOnAPartAndFitsTheSkin(GameTestHelper helper) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        seen.add("root");
        for (UltronCubes.Part part : UltronCubes.PARTS) {
            check(seen.contains(part.parent()), part.name() + " hangs on " + part.parent() + ", which comes after it (or is missing)");
            check(seen.add(part.name()), part.name() + " is listed twice");
        }
        java.util.Set<String> names = new java.util.HashSet<>();
        for (UltronCubes.Cube cube : UltronCubes.CUBES) {
            check(seen.contains(cube.part()), cube.name() + " hangs on a part that is not there: " + cube.part());
            check(names.add(cube.name()), cube.name() + " is listed twice");
            check(cube.w() > 0 && cube.h() > 0 && cube.d() > 0, cube.name() + " has a size");
            check(cube.u() >= 0 && cube.v() >= 0 && cube.u() + cube.footW() <= UltronCubes.SKIN && cube.v() + cube.footH() <= UltronCubes.SKIN,
                    cube.name() + " fits the skin: " + cube.u() + ", " + cube.v() + " + " + cube.footW() + " x " + cube.footH());
            check(!cube.forms().isEmpty() && cube.forms().chars().allMatch(c -> "PHUS".indexOf(c) >= 0), cube.name() + " lists forms: " + cube.forms());
        }
        // Boxes drawn together do not share a piece of the skin.
        for (int form = 0; form < 4; form++) {
            List<UltronCubes.Cube> drawn = new java.util.ArrayList<>();
            for (UltronCubes.Cube cube : UltronCubes.CUBES) {
                if (cube.in(form)) {
                    drawn.add(cube);
                }
            }
            check(drawn.size() >= 30, "form " + form + " is made of " + drawn.size() + " boxes");
            for (int a = 0; a < drawn.size(); a++) {
                for (int b = a + 1; b < drawn.size(); b++) {
                    UltronCubes.Cube p = drawn.get(a);
                    UltronCubes.Cube q = drawn.get(b);
                    boolean apart = p.u() + p.footW() <= q.u() || q.u() + q.footW() <= p.u() || p.v() + p.footH() <= q.v() || q.v() + q.footH() <= p.v();
                    // (Boxes that repeat share one piece of the skin on purpose: the two halves of a pair, one drawn mirrored, and the
                    // five fingers of a hand.)
                    boolean repeat = p.u() == q.u() && p.v() == q.v() && p.w() == q.w() && p.h() == q.h() && p.d() == q.d();
                    check(apart || repeat, "form " + form + ": " + p.name() + " and " + q.name() + " share a piece of the skin");
                }
            }
        }
        // The model moves these boxes (the plates fall open) and these parts (the pose), so they must be there.
        for (String needed : new String[]{"jaw", "chestR", "chestL", "pauldronAR", "pauldronAL", "thighPlateR", "thighPlateL"}) {
            check(UltronCubes.named(needed) != null, "the model moves the box " + needed);
        }
        for (String needed : new String[]{"hips", "head", "rightArm", "leftArm", "rightForearm", "leftForearm", "rightHand", "leftHand", "rightFingers",
                "leftFingers", "rightLeg", "leftLeg", "rightShin", "leftShin"}) {
            check(seen.contains(needed), "the model poses the part " + needed);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyStateOfEveryBodyHasAFinitePoseThatKeepsTheJointsInTheirRange(GameTestHelper helper) {
        UltronPose pose = new UltronPose();
        for (UltronState state : UltronState.values()) {
            for (int body = 0; body < 4; body++) {
                for (float t = 0.0F; t <= 130.0F; t += 2.5F) {
                    UltronPoses.boss(pose, state, t, body, 1000.0F + t);
                    for (int i = 0; i < UltronPose.COUNT; i++) {
                        check(Float.isFinite(pose.v[i]), state + " body " + body + " t " + t + ": joint " + i + " is " + pose.v[i]);
                    }
                    for (int arm : new int[]{UltronPose.R_ARM_X, UltronPose.L_ARM_X}) {
                        check(pose.v[arm] >= -200.0F && pose.v[arm] <= 80.0F, state + " t " + t + ": an arm at " + pose.v[arm] + " degrees");
                    }
                    check(pose.v[UltronPose.SCALE] > 0.0F, state + ": a body has a size");
                    for (int light : new int[]{UltronPose.FINGER_GLOW, UltronPose.PALM_GLOW, UltronPose.TORSO_GLOW}) {
                        check(pose.v[light] >= 0.0F && pose.v[light] <= 2.0F, state + " t " + t + ": a light at " + pose.v[light]);
                    }
                }
            }
        }
        for (UltronSentryEntity.Act act : UltronSentryEntity.Act.values()) {
            for (UltronSentryEntity.Eyes eyes : UltronSentryEntity.Eyes.values()) {
                for (float t = 0.0F; t <= 40.0F; t += 2.0F) {
                    UltronPoses.sentry(pose, act, eyes, t, 500.0F + t, 12.0F);
                    for (int i = 0; i < UltronPose.COUNT; i++) {
                        check(Float.isFinite(pose.v[i]), act + " " + eyes + " t " + t + ": joint " + i + " is " + pose.v[i]);
                    }
                    check(pose.v[UltronPose.SCALE] > 0.0F, act + ": a Sentry has a size");
                }
            }
        }
        helper.succeed();
    }

    /** How different two poses look: the joints in degrees and the lights as sixty degrees for a full light. */
    private static float difference(UltronPose a, UltronPose b) {
        float sum = 0.0F;
        for (int i = UltronPose.HIPS_X; i <= UltronPose.L_SHIN; i++) {
            float d = Math.abs(a.v[i] - b.v[i]);
            // (The bob is in blocks and the fingers' curl a fraction.)
            sum += i == UltronPose.BOB ? 40.0F * d : (i == UltronPose.R_FINGERS || i == UltronPose.L_FINGERS) ? 30.0F * d : d;
        }
        for (int light : new int[]{UltronPose.FINGER_GLOW, UltronPose.PALM_GLOW, UltronPose.TORSO_GLOW, UltronPose.FLASH}) {
            sum += 60.0F * Math.abs(a.v[light] - b.v[light]);
        }
        return sum;
    }

    /** The rule of the fight: every blow has a tell, in the body, before it lands. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyAttackShowsItsTellInTheBodyBeforeTheBlowLands(GameTestHelper helper) {
        UltronPose rest = new UltronPose();
        UltronPose tell = new UltronPose();
        UltronPose start = new UltronPose();
        float age = 4000.0F;
        for (UltronAttack attack : UltronAttack.values()) {
            for (int body = 0; body < 4; body++) {
                if (!attack.allowedFor(body)) {
                    continue;
                }
                UltronPoses.boss(rest, UltronState.IDLE, 0.0F, body, age);
                UltronPoses.boss(tell, attack.state(), attack.tell() - 1, body, age);
                UltronPoses.boss(start, attack.state(), 0.0F, body, age);
                float shown = difference(rest, tell);
                check(shown >= 25.0F, attack + " (body " + body + "): one tick before the first damage the body differs from rest by only " + shown);
                check(difference(rest, start) < shown, attack + " (body " + body + "): the tell builds, it is not there from the first tick");
            }
        }
        // And the armed glow: the fingertips are lit for the Volley and the Beams before anything flies.
        UltronPose volley = new UltronPose();
        UltronPoses.boss(volley, UltronState.VOLLEY, UltronAttack.VOLLEY.tell() - 1, 0, age);
        check(volley.v[UltronPose.FINGER_GLOW] > 0.6F, "the Volley lights the fingertips (" + volley.v[UltronPose.FINGER_GLOW] + ")");
        UltronPose beams = new UltronPose();
        UltronPoses.boss(beams, UltronState.BEAMS, UltronAttack.BEAMS.tell() - 1, 0, age);
        check(beams.v[UltronPose.FINGER_GLOW] > 0.6F, "the Beams light the fingertips (" + beams.v[UltronPose.FINGER_GLOW] + ")");
        helper.succeed();
    }
}
