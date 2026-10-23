package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.client.render.ApocalypseBossPoses;
import com.kolodkingal.enhanced.client.render.ApocalypseCubes;
import com.kolodkingal.enhanced.client.render.ApocalypseFxMath;
import com.kolodkingal.enhanced.client.render.SandMath;
import com.kolodkingal.enhanced.client.render.SandPalette;
import com.kolodkingal.enhanced.client.render.SandShapes;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The client stage of the Apocalypse boss, as far as it can be checked with no screen: the table of his cubes fits the
 * texture it is painted on, every state of his controller has a pose that starts where he stands and ends where its tell
 * does, the formulas of the sand agree with the server (the boulders end where it throws them from), the colour of his eyes,
 * the music and the amber air follow the tables of the design, and every texture the stage needs is there at the size the
 * model reads. Pure: nothing here draws, and nothing touches the world, so they share a batch of their own.
 */
public class ApocalypseClientTests {
    private static final String PURE = "apocalypse_client_pure";

    // ------------------------------------------------------------------------------------------------------------
    // The model

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theBossHasTwentyFourCubesThatFitTheTexture(GameTestHelper helper) {
        List<ApocalypseCubes.Cube> all = ApocalypseCubes.ALL;
        check(all.size() == 24, "24 cubes, not " + all.size());
        Set<String> names = new HashSet<>();
        for (ApocalypseCubes.Cube c : all) {
            check(names.add(c.name()), "the name " + c.name() + " is used once");
            check(c.u() >= 0 && c.v() >= 0, c.name() + " starts inside the texture");
            check(c.u() + c.regionWidth() <= ApocalypseCubes.WIDTH, c.name() + " fits the width of the texture");
            check(c.v() + c.regionHeight() <= ApocalypseCubes.HEIGHT, c.name() + " fits the height of the texture");
            check(c.w() > 0 && c.h() > 0 && c.d() > 0, c.name() + " has a size");
        }
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                ApocalypseCubes.Cube a = all.get(i);
                ApocalypseCubes.Cube b = all.get(j);
                if (ApocalypseCubes.sharesRegion(a, b)) {
                    // The cables and the panels of the cloak that repeat read the same texels: they must be the same box.
                    check(a.w() == b.w() && a.h() == b.h() && a.d() == b.d(), a.name() + " and " + b.name() + " share texels and a size");
                    continue;
                }
                boolean apart = a.u() + a.regionWidth() <= b.u() || b.u() + b.regionWidth() <= a.u()
                        || a.v() + a.regionHeight() <= b.v() || b.v() + b.regionHeight() <= a.v();
                check(apart, a.name() + " and " + b.name() + " are painted over each other");
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void aMirroredCubeIsTheBoxReflectedAboutItsPivot(GameTestHelper helper) {
        // The left arm is the right arm turned about the pivot: a box from x to x + w becomes the box from -(x + w) to -x.
        assertNear(ApocalypseCubes.mirroredX(-3, 4), -1, 1.0E-6, "an arm that spans -3 to 1 spans -1 to 3 on the other side");
        assertNear(ApocalypseCubes.mirroredX(-6.5F, 6), 0.5, 1.0E-6, "and a pauldron that spans -6.5 to -0.5 spans 0.5 to 6.5");
        for (ApocalypseCubes.Cube c : ApocalypseCubes.ALL) {
            if (c.mirrored()) {
                float left = ApocalypseCubes.mirroredX(c.x(), c.w());
                assertNear(left + c.w() + c.x(), 0.0, 1.0E-5, c.name() + ": the box and its reflection mirror about x = 0");
            }
        }
        check(ApocalypseCubes.SCALE_ARMOURED > ApocalypseCubes.SCALE_TRUE, "he is bigger in his armour than without it");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The poses

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void everyStateOfTheBossHasAPoseThatStartsWhereHeStands(GameTestHelper helper) {
        ApocalypseBossPoses.Out stand = new ApocalypseBossPoses.Out().stand();
        ApocalypseBossPoses.Out out = new ApocalypseBossPoses.Out();
        for (ApocalypseState state : ApocalypseState.values()) {
            ApocalypseBossPoses.Pose pose = ApocalypseBossPoses.poseFor(state);
            check(pose != null, state + " has a pose");
            if (state.tellTicks() > 0) {
                check(pose.length() == state.tellTicks(), state + ": the pose lasts as long as the tell the server counts, " + state.tellTicks());
            }
        }
        for (ApocalypseBossPoses.Pose pose : ApocalypseBossPoses.Pose.values()) {
            ApocalypseBossPoses.sample(pose, 0.0F, out);
            if (pose == ApocalypseBossPoses.Pose.AWAKEN) {
                check(out.get(ApocalypseBossPoses.Ch.HEAD_X) > 10.0F, "he steps out of the portal with his head bowed");
                continue;
            }
            for (ApocalypseBossPoses.Ch ch : ApocalypseBossPoses.Ch.values()) {
                assertNear(out.get(ch), stand.get(ch), 1.0E-5, pose + " starts at the standing pose in " + ch);
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void aPoseEndsWithItsTellAndTheAttacksFollowThrough(GameTestHelper helper) {
        for (ApocalypseBossPoses.Pose pose : ApocalypseBossPoses.Pose.values()) {
            float end = ApocalypseBossPoses.end(pose);
            check(end >= pose.length(), pose + " goes on to at least the end of its tell (" + end + " vs " + pose.length() + ")");
            if (ApocalypseBossPoses.follows(pose)) {
                // The state ends the moment the blow lands: the pose goes on so that the throw, the slam, the swing is seen.
                check(pose.length() > 0, pose + " follows through, so it is an attack with a tell");
                check(end - pose.length() >= ApocalypseBossPoses.FOLLOW_TICKS, pose + " has keys for the " + ApocalypseBossPoses.FOLLOW_TICKS
                        + " ticks after the tell");
            } else if (pose.length() > 0) {
                assertNear(end, pose.length(), 1.0E-5, pose + " ends with its tell");
            }
        }
        for (ApocalypseBossPoses.Pose attack : new ApocalypseBossPoses.Pose[]{ApocalypseBossPoses.Pose.LANCE, ApocalypseBossPoses.Pose.WAVE,
                ApocalypseBossPoses.Pose.SPIKES, ApocalypseBossPoses.Pose.BOULDERS, ApocalypseBossPoses.Pose.ENTOMB, ApocalypseBossPoses.Pose.BACKHAND}) {
            check(ApocalypseBossPoses.follows(attack), attack + " follows through");
        }
        check(!ApocalypseBossPoses.follows(ApocalypseBossPoses.Pose.MIND_CRUSH), "the Mind Crush ends with its ring");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void thePosesAreSmoothAndHoldTheirLastKey(GameTestHelper helper) {
        ApocalypseBossPoses.Out a = new ApocalypseBossPoses.Out();
        ApocalypseBossPoses.Out b = new ApocalypseBossPoses.Out();
        for (ApocalypseBossPoses.Pose pose : ApocalypseBossPoses.Pose.values()) {
            float end = ApocalypseBossPoses.end(pose);
            for (float t = 0.0F; t < end + 10.0F; t += 0.05F) {
                ApocalypseBossPoses.sample(pose, t, a);
                ApocalypseBossPoses.sample(pose, t + 0.05F, b);
                for (ApocalypseBossPoses.Ch ch : ApocalypseBossPoses.Ch.values()) {
                    check(Float.isFinite(a.get(ch)), pose + " " + ch + " is a number at " + t);
                    // (A fast key moves 150 degrees a tick at its quickest: 7.5 in a twentieth of one. Anything above is a jump.)
                    check(Math.abs(b.get(ch) - a.get(ch)) < 12.0F, pose + " " + ch + " jumps at tick " + t);
                }
            }
            ApocalypseBossPoses.sample(pose, end, a);
            ApocalypseBossPoses.sample(pose, end + 50.0F, b);
            for (ApocalypseBossPoses.Ch ch : ApocalypseBossPoses.Ch.values()) {
                assertNear(b.get(ch), a.get(ch), 1.0E-6, pose + " holds its last key in " + ch);
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theTellsAreReadableBeforeTheBlowLands(GameTestHelper helper) {
        // Every attack must show something before it lands: its pose has moved well away from the standing one by the tell's
        // second half. (The arm, the lean or the drop: the largest change of any channel.)
        ApocalypseBossPoses.Out stand = new ApocalypseBossPoses.Out().stand();
        ApocalypseBossPoses.Out out = new ApocalypseBossPoses.Out();
        for (ApocalypseState state : ApocalypseState.values()) {
            if (state.tellTicks() == 0 || state == ApocalypseState.BACKHAND) {
                continue;
            }
            ApocalypseBossPoses.Pose pose = ApocalypseBossPoses.poseFor(state);
            float biggest = 0.0F;
            for (float t = state.tellTicks() * 0.4F; t <= state.tellTicks(); t += 1.0F) {
                ApocalypseBossPoses.sample(pose, t, out);
                for (ApocalypseBossPoses.Ch ch : ApocalypseBossPoses.Ch.values()) {
                    if (ch != ApocalypseBossPoses.Ch.AIM && ch != ApocalypseBossPoses.Ch.GLOW) {
                        biggest = Math.max(biggest, Math.abs(out.get(ch) - stand.get(ch)));
                    }
                }
            }
            check(biggest > 12.0F, state + " moves his body by at least a hand's breadth of angle before it lands (" + biggest + ")");
        }
        // The grasp is aimed at its target; the others are not.
        ApocalypseBossPoses.sample(ApocalypseBossPoses.Pose.ENTOMB, 20.0F, out);
        assertNear(out.get(ApocalypseBossPoses.Ch.AIM), 1.0, 1.0E-5, "the Entomb points his arm along the aim");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The sand

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theBouldersEndTheirOrbitWhereTheServerThrowsThemFrom(GameTestHelper helper) {
        Vec3 feet = new Vec3(10.0, 64.0, -3.0);
        double height = 2.7;
        for (int i = 0; i < 3; i++) {
            // ApocalypseAttacks.boulders: feet + (cos a, height + 0.8, sin a) * 1.2 with a = 2 pi i / 3 (the radius on the sides only).
            double a = Math.PI * 2.0 * i / 3;
            Vec3 server = feet.add(Math.cos(a) * 1.2, height + 0.8, Math.sin(a) * 1.2);
            Vec3 client = SandMath.boulder(feet, height, SandMath.BOULDER_TELL, i);
            assertNear(client.x, server.x, 1.0E-4, "boulder " + i + " ends at the server's x");
            assertNear(client.y, server.y, 1.0E-4, "boulder " + i + " ends at the server's y");
            assertNear(client.z, server.z, 1.0E-4, "boulder " + i + " ends at the server's z");
            Vec3 after = SandMath.boulder(feet, height, SandMath.BOULDER_TELL + 10, i);
            assertNear(after.x, client.x, 1.0E-4, "and they stay there if the throw is a tick late");
        }
        Vec3 start = SandMath.boulder(feet, height, 0.0F, 0);
        check(start.y < feet.y + 0.5, "they begin in the ground");
        check(SandMath.boulder(feet, height, SandMath.BOULDER_RISE, 0).y > feet.y + height, "and are over his head once they have risen");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theSpikesRiseSettleAndSink(GameTestHelper helper) {
        assertNear(SandMath.spike(-1.0F), 0.0, 1.0E-6, "nothing before the eruption");
        assertNear(SandMath.spike(0.0F), 0.0, 1.0E-6, "nothing at its first tick");
        check(SandMath.spike(2.0F) > 0.0F && SandMath.spike(2.0F) < 1.0F, "rising at two ticks");
        assertNear(SandMath.spike(4.0F), 1.0, 1.0E-5, "full at four ticks");
        float settled = SandMath.spike(10.0F);
        assertNear(settled, 0.6, 1.0E-5, "settled at 0.6 by the tenth");
        check(SandMath.spike(16.0F) < settled, "sinking after");
        assertNear(SandMath.spike(20.0F), 0.0, 1.0E-6, "gone by the twentieth");
        assertNear(SandMath.coneScale(0), 1.0, 1.0E-6, "the middle cone is the tallest");
        for (int i = 1; i < 6; i++) {
            check(SandMath.coneScale(i) < 1.0F && SandMath.coneScale(i) > 0.4F, "cone " + i + " is a little shorter");
        }
        Vec3 centre = new Vec3(4, 70, 4);
        check(SandMath.cone(centre, 1.8, 0).equals(centre), "cone 0 stands in the middle");
        for (int i = 1; i < 6; i++) {
            assertNear(SandMath.cone(centre, 1.8, i).subtract(centre).horizontalDistance(), 1.8 * 0.62, 1.0E-6, "cone " + i + " stands on the inner ring");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theRidgeAndTheRingsFollowTheirFormulas(GameTestHelper helper) {
        assertNear(SandMath.ridge(0.0, 1.1), 1.1, 1.0E-9, "the crest stands 1.1 tall");
        assertNear(SandMath.ridge(SandMath.RIDGE_LENGTH, 1.1), 0.0, 1.0E-9, "the tail is on the ground");
        double before = SandMath.ridge(0.0, 1.1);
        for (double behind = 0.25; behind <= SandMath.RIDGE_LENGTH; behind += 0.25) {
            double now = SandMath.ridge(behind, 1.1);
            check(now <= before + 1.0E-9, "the ridge only falls away behind the crest, at " + behind);
            before = now;
        }
        assertNear(SandMath.contracting(0.0F, 40, 28.0), 28.0, 1.0E-6, "the Mind Crush starts at the range");
        assertNear(SandMath.contracting(40.0F, 40, 28.0), 2.0, 1.0E-6, "and ends at two blocks");
        assertNear(SandMath.contracting(80.0F, 40, 28.0), 2.0, 1.0E-6, "and stays there");
        assertNear(SandMath.crawl(0.0F), 0.0, 1.0E-6, "the stripe starts at him");
        assertNear(SandMath.crawl(20.0F), 1.0, 1.0E-6, "and has reached the feet in twenty ticks");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theCracksAndRipplesLieOnTheSphereOfTheShield(GameTestHelper helper) {
        Vec3 centre = new Vec3(2.0, 65.0, -4.0);
        double radius = 2.6;
        // A circle of a ripple, and a crack: every point is on the sphere.
        for (Vec3 p : SandShapes.circleOnSphere(centre, radius, new Vec3(0, 1, 0), 0.6, 24)) {
            assertNear(p.distanceTo(centre), radius, 1.0E-6, "a point of a ripple is on the sphere");
        }
        List<Vec3> crack = SandShapes.crackOnSphere(centre, radius, new Vec3(0.3, 0.8, 0.5), 10, 0.12, 77L);
        check(crack.size() == 11, "a crack of ten steps has eleven points, not " + crack.size());
        for (Vec3 p : crack) {
            assertNear(p.distanceTo(centre), radius, 1.0E-6, "a point of a crack is on the sphere");
        }
        List<Vec3> again = SandShapes.crackOnSphere(centre, radius, new Vec3(0.3, 0.8, 0.5), 10, 0.12, 77L);
        check(again.equals(crack), "the same seed cracks the same way, on every client");
        check(!SandShapes.crackOnSphere(centre, radius, new Vec3(0.3, 0.8, 0.5), 10, 0.12, 78L).equals(crack), "another seed another crack");
        // A wider arc is a wider ripple.
        Vec3 axis = new Vec3(0, 0, 1);
        double small = SandShapes.circleOnSphere(centre, radius, axis, 0.3, 8).get(0).distanceTo(centre.add(axis.scale(radius)));
        double big = SandShapes.circleOnSphere(centre, radius, axis, 0.9, 8).get(0).distanceTo(centre.add(axis.scale(radius)));
        check(big > small, "a ripple grows as its arc does");
        assertNear(SandShapes.onSphere(centre, new Vec3(1, 0, 0), radius).distanceTo(centre), radius, 1.0E-9, "a point in a direction is a radius away");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theColoursOfTheSandMixAndTheEnergiesAreDistinct(GameTestHelper helper) {
        check(SandShapes.mixRgb(0x000000, 0xFFFFFF, 0.0F) == 0x000000, "the first colour at 0");
        check(SandShapes.mixRgb(0x000000, 0xFFFFFF, 1.0F) == 0xFFFFFF, "the second at 1");
        int half = SandShapes.mixRgb(0x000000, 0xFFFFFF, 0.5F);
        check((half & 0xFF) >= 0x7E && (half & 0xFF) <= 0x81, "grey halfway");
        Set<Integer> colours = new HashSet<>(List.of(SandPalette.LIGHT, SandPalette.MAIN, SandPalette.SHADE, SandPalette.DEEP, SandPalette.HAZE,
                SandPalette.GOLD_CORE, SandPalette.GOLD, SandPalette.GOLD_EDGE, SandPalette.GOLD_DEEP, SandPalette.VIOLET_CORE, SandPalette.VIOLET,
                SandPalette.VIOLET_BODY, SandPalette.VIOLET_FRINGE, SandPalette.SAPPHIRE, SandPalette.SAPPHIRE_CORE, SandPalette.FIRE_CORE,
                SandPalette.FIRE_GOLD, SandPalette.FIRE_MAIN, SandPalette.FIRE_EMBER));
        check(colours.size() == 19, "the nineteen colours of the palette are all different");
        for (int rgb : colours) {
            check((rgb >>> 24) == 0, "a palette colour carries no alpha");
        }
        check(SandPalette.GOLD == 0xF5C84A && SandPalette.VIOLET == 0xB04FD8, "the gold of the shield and the violet of the portal");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The eyes, the music and the air

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theEyesGlowAsTheDesignSays(GameTestHelper helper) {
        check(ApocalypseFxMath.eyeTint(ApocalypseState.AWAKENING, 5.0F, 100.0F, true) == 0, "dark while he steps out of the portal");
        check(alpha(ApocalypseFxMath.eyeTint(ApocalypseState.AWAKENING, 40.0F, 100.0F, true)) == 255, "lit by the end of it");
        int crush = ApocalypseFxMath.eyeTint(ApocalypseState.TELL_MIND_CRUSH, 10.0F, 100.0F, true);
        check((crush & 0xFFFFFF) == SandPalette.VIOLET, "violet for the Mind Crush");
        check(alpha(crush) > 180, "and bright");
        check((ApocalypseFxMath.eyeTint(ApocalypseState.ESCAPING, 10.0F, 100.0F, true) & 0xFFFFFF) == SandPalette.VIOLET, "and for the Escape");
        int stagger = ApocalypseFxMath.eyeTint(ApocalypseState.STAGGERED, 10.0F, 100.0F, true);
        check(alpha(stagger) > 80 && alpha(stagger) < 100, "dim while he kneels, shield broken");
        check((ApocalypseFxMath.eyeTint(ApocalypseState.KNEELING, 10.0F, 100.0F, false) & 0xFFFFFF) == SandPalette.GOLD, "gold when the armour is gone");
        check((ApocalypseFxMath.eyeTint(ApocalypseState.KNEELING, 10.0F, 100.0F, true) & 0xFFFFFF) == 0xFFFFFF, "white while it is on");
        check(alpha(ApocalypseFxMath.eyeTint(ApocalypseState.DYING, 0.0F, 100.0F, false)) == 255, "bright as he begins to die");
        check(alpha(ApocalypseFxMath.eyeTint(ApocalypseState.DYING, 60.0F, 100.0F, false)) == 0, "out when he is dead");
        int idle = ApocalypseFxMath.eyeTint(ApocalypseState.IDLE, 0.0F, 0.0F, true);
        check(alpha(idle) > 200 && (idle & 0xFFFFFF) == 0xFFFFFF, "white and steady when he stands");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theMusicIsLoudNearHimAndGoneFarAway(GameTestHelper helper) {
        check(!ApocalypseFxMath.wantsFinale(1, false) && !ApocalypseFxMath.wantsFinale(2, false), "the first two phases play the theme");
        check(ApocalypseFxMath.wantsFinale(3, false) && ApocalypseFxMath.wantsFinale(4, false), "from the third the fuller one");
        check(ApocalypseFxMath.wantsFinale(1, true), "and in the true form, whatever the phase");
        assertNear(ApocalypseFxMath.musicVolume(0.0), 1.0, 1.0E-6, "full beside him");
        assertNear(ApocalypseFxMath.musicVolume(64.0), 1.0, 1.0E-6, "full at 64 blocks");
        assertNear(ApocalypseFxMath.musicVolume(80.0), 0.5, 1.0E-6, "half at 80");
        assertNear(ApocalypseFxMath.musicVolume(96.0), 0.0, 1.0E-6, "none at 96");
        assertNear(ApocalypseFxMath.musicVolume(300.0), 0.0, 1.0E-6, "and none beyond");
        float before = 1.0F;
        for (double d = 0; d < 120; d += 2) {
            float now = ApocalypseFxMath.musicVolume(d);
            check(now <= before + 1.0E-6F, "it only gets quieter with distance, at " + d);
            before = now;
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theWallRisesAndTheAirTurnsAmberInsideIt(GameTestHelper helper) {
        assertNear(ApocalypseFxMath.wallRisen(0.0F), 0.0, 1.0E-6, "the wall begins on the ground");
        assertNear(ApocalypseFxMath.wallRisen(20.0F), 0.5, 1.0E-6, "is half up after half of forty ticks");
        assertNear(ApocalypseFxMath.wallRisen(40.0F), 1.0, 1.0E-6, "and whole after forty");
        assertNear(ApocalypseFxMath.wallRisen(400.0F), 1.0, 1.0E-6, "and stays");
        assertNear(ApocalypseFxMath.tintReach(10.0, 40.0), 1.0, 1.0E-6, "the air is amber all through the arena");
        assertNear(ApocalypseFxMath.tintReach(40.0, 40.0), 1.0, 1.0E-6, "to the wall");
        assertNear(ApocalypseFxMath.tintReach(72.0, 40.0), 0.5, 1.0E-6, "half a way out");
        assertNear(ApocalypseFxMath.tintReach(104.0, 40.0), 0.0, 1.0E-6, "and gone sixty-four blocks outside it");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The art

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void everyTextureOfTheStageIsThereAtTheSizeTheModelReads(GameTestHelper helper) {
        String[][] sized = {
                {"apocalypse_boss", "128", "64"}, {"apocalypse_boss_true", "128", "64"},
                {"apocalypse_boss_eyes", "128", "64"}, {"apocalypse_boss_true_eyes", "128", "64"},
                {"sand_cocoon", "64", "64"}, {"sand_cocoon_cracks", "64", "64"}, {"golden_web", "64", "64"},
                // The masks of the hero's eyes are laid over his skin: the size of a skin.
                {"apocalypse_eyes", "64", "64"}, {"apocalypse_eyes_ancient", "64", "64"},
        };
        for (String[] row : sized) {
            BufferedImage image = read("textures/entity/" + row[0] + ".png");
            check(image.getWidth() == Integer.parseInt(row[1]) && image.getHeight() == Integer.parseInt(row[2]),
                    row[0] + " is " + row[1] + "x" + row[2] + ", not " + image.getWidth() + "x" + image.getHeight());
        }
        // The swirl of the wall and the funnels is an animation of square frames, four of them.
        BufferedImage swirl = read("textures/entity/sand_swirl.png");
        check(swirl.getHeight() == swirl.getWidth() * 4, "the swirl is four square frames, one above the other");
        check(resource("textures/entity/sand_swirl.png.mcmeta") != null, "and has its animation");
        // The skins of the hero's three looks.
        check(read("textures/entity/suit/apocalypse.png").getHeight() == 64, "the film's suit is a skin");
        check(read("textures/entity/suit/apocalypse_ancient.png").getHeight() == 64, "and so is the ancient one");
        check(resource("textures/mob_effect/entombed.png") != null && resource("textures/mob_effect/empowered.png") != null, "the icons of his two effects");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = PURE)
    public static void theEyesOfTheBossGlowOnlyWhereTheyAreMeantTo(GameTestHelper helper) {
        // The glow textures are black wherever they should not glow: most of the image is transparent or black, and what is
        // lit is a small part of it (the eyes, the cracks).
        for (String name : new String[]{"apocalypse_boss_eyes", "apocalypse_boss_true_eyes", "apocalypse_eyes", "apocalypse_eyes_ancient"}) {
            BufferedImage image = read("textures/entity/" + name + ".png");
            int lit = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    if ((argb >>> 24) > 0 && (argb & 0xFFFFFF) != 0) {
                        lit++;
                    }
                }
            }
            check(lit > 0, name + " has something that glows");
            check(lit < image.getWidth() * image.getHeight() / 8, name + " glows in a small part of the image (" + lit + " texels)");
        }
        helper.succeed();
    }

    private static int alpha(int argb) {
        return argb >>> 24;
    }

    private static InputStream resource(String path) {
        return ApocalypseClientTests.class.getResourceAsStream("/assets/" + Enhanced.MODID + "/" + path);
    }

    private static BufferedImage read(String path) {
        try (InputStream in = resource(path)) {
            check(in != null, path + " is there");
            BufferedImage image = ImageIO.read(in);
            check(image != null, path + " is an image");
            return image;
        } catch (java.io.IOException e) {
            throw new AssertionError(path + ": " + e);
        }
    }
}
