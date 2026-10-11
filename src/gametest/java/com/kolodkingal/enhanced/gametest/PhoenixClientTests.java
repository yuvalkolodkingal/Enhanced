package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.Firebird;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.PhoenixMath;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Phoenix's client, as far as a server can check it: the numbers behind what she looks like (the Burst's shell, the
 * firebird, the dome and the ring, the placement of the Mind Reading panel, the colours of the fire), the plug-in's
 * stances, and the art her layers read. Nothing here names a client class that a server cannot load (the drawing and
 * the panel are checked by eye in a real game); the plug-in is reached through {@link HeroClients}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class PhoenixClientTests {
    private static final String BATCH = PhoenixTests.QUICK;

    // ------------------------------------------------------------------------------------------------------------
    // The Burst

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBurstsShellSpreadsAtTheWavesSpeedAndFadesInItsLastTenTicks(GameTestHelper helper) {
        // The same radius the server's wave has at each tick: speed * (age + 1), at most the radius.
        assertNear(PhoenixMath.shellRadius(24.0F, 1.0F, 0.0F), 1.0, 1.0E-6, "a block at the first tick");
        assertNear(PhoenixMath.shellRadius(24.0F, 1.0F, 9.0F), 10.0, 1.0E-6, "ten blocks at tick nine");
        assertNear(PhoenixMath.shellRadius(24.0F, 1.0F, 99.0F), 24.0, 1.0E-6, "never beyond the radius");
        assertNear(PhoenixMath.shellRadius(24.0F, 4.0F, 2.0F), 12.0, 1.0E-6, "twice as fast when the config says so");
        assertNear(PhoenixMath.shellTicks(24.0F, 1.0F), 24.0, 1.0E-6, "24 ticks to cross 24 blocks");

        assertNear(PhoenixMath.shellAlpha(24.0F, 1.0F, 0.0F), 0.5, 1.0E-6, "half bright at the start");
        assertNear(PhoenixMath.shellAlpha(24.0F, 1.0F, 13.0F), 0.5, 1.0E-6, "and still half bright until the last ten ticks");
        assertNear(PhoenixMath.shellAlpha(24.0F, 1.0F, 19.0F), 0.25, 1.0E-6, "half of that five ticks later");
        check(PhoenixMath.shellAlpha(24.0F, 1.0F, 23.9F) < 0.01F, "nearly gone as the wave reaches the edge");
        assertNear(PhoenixMath.shellAlpha(24.0F, 1.0F, 24.0F), 0.0, 1.0E-9, "and gone once it has");
        // A shell that crosses its radius in less than ten ticks fades over that time instead.
        check(PhoenixMath.shellAlpha(8.0F, 1.0F, 4.0F) < PhoenixMath.shellAlpha(8.0F, 1.0F, 0.0F), "a short shell fades from the first tick");
        check(PhoenixMath.shellAlpha(24.0F, 1.0F, -1.0F) == 0.0F, "nothing before the release");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFirebirdOfABurstGrowsFliesOutAndDissolves(GameTestHelper helper) {
        assertNear(PhoenixMath.burstBirdScale(24.0F, 0.0F), 1.0, 1.0E-6, "it starts at scale one");
        assertNear(PhoenixMath.burstBirdScale(24.0F, 24.0F), 4.0, 1.0E-6, "and ends at a sixth of the radius");
        check(PhoenixMath.burstBirdScale(24.0F, 12.0F) > 1.0F && PhoenixMath.burstBirdScale(24.0F, 12.0F) < 4.0F, "growing between");
        assertNear(PhoenixMath.burstBirdScale(3.0F, 24.0F), 1.0, 1.0E-6, "never smaller than scale one, even for a tiny Burst");
        assertNear(PhoenixMath.burstBirdDistance(24.0F, 0.0F), 0.0, 1.0E-6, "it leaves from her chest");
        assertNear(PhoenixMath.burstBirdDistance(24.0F, 24.0F), 18.0, 1.0E-6, "and ends three quarters of the radius out");
        assertNear(PhoenixMath.burstWingAngle(0.0F), -20.0, 1.0E-6, "flung open and down at the release");
        check(PhoenixMath.burstWingAngle(24.0F) > PhoenixMath.burstWingAngle(0.0F), "the wings rise again as it flies off");
        assertNear(PhoenixMath.burstBirdAlpha(0.0F), 1.0, 1.0E-6, "whole at first");
        assertNear(PhoenixMath.burstBirdAlpha(24.0F), 0.0, 1.0E-6, "dissolved at the end");

        assertNear(PhoenixMath.flashRadius(1.0F, 0.0F), 1.5, 1.0E-6, "the flash is a disc of 1.5 blocks at full charge");
        check(PhoenixMath.flashRadius(0.0F, 0.0F) < PhoenixMath.flashRadius(1.0F, 0.0F), "smaller for a weaker Burst");
        check(PhoenixMath.flashRadius(1.0F, 3.0F) == 0.0F, "and over after two ticks");
        assertNear(PhoenixMath.screenFlash(0.0F), 1.0, 1.0E-6, "the screen is gold at the first tick");
        check(PhoenixMath.screenFlash(5.9F) > 0.0F && PhoenixMath.screenFlash(6.0F) == 0.0F, "and clear after six");
        check(PhoenixMath.flashReaches(27.0, 24.0F) && !PhoenixMath.flashReaches(29.0, 24.0F), "it reaches whoever is in or just outside the wave");
        check(PhoenixMath.shake(1.0F, 0.0, 0.0F) > PhoenixMath.shake(1.0F, 12.0, 0.0F), "the camera shakes more near the centre");
        check(PhoenixMath.shake(1.0F, 30.0, 0.0F) == 0.0F && PhoenixMath.shake(1.0F, 0.0, 11.0F) == 0.0F, "and not far away or after ten ticks");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The charge

    @GameTest(template = ARENA, batch = BATCH)
    public static void theChargeFillsFromZeroToOneAndTheHudSaysItInWholePercents(GameTestHelper helper) {
        assertNear(PhoenixMath.charge(0.0F, 60), 0.0, 1.0E-6, "empty at the start");
        assertNear(PhoenixMath.charge(30.0F, 60), 0.5, 1.0E-6, "half at half the ticks");
        assertNear(PhoenixMath.charge(90.0F, 60), 1.0, 1.0E-6, "never above one");
        assertNear(PhoenixMath.charge(5.0F, 0), 1.0, 1.0E-6, "a charge of no length is full");
        check(PhoenixMath.percent(0.63F) == 63 && PhoenixMath.percent(1.0F) == 100 && PhoenixMath.percent(0.004F) == 0, "whole percents");
        check(PhoenixMath.BRIGHT_CHARGE == 0.9F, "the HUD turns orange, and the eyes and the screen edge brighten, above nine tenths");

        assertNear(PhoenixMath.chargeBirdScale(0.0F), 0.5, 1.0E-6, "the charging firebird starts at half size");
        assertNear(PhoenixMath.chargeBirdScale(1.0F), 1.3, 1.0E-6, "and ends at 1.3");
        check(!PhoenixMath.chargeBirdHasWings(0.2F) && PhoenixMath.chargeBirdHasWings(0.21F), "its wings come out once a fifth of the charge is in");
        for (int t = 0; t < 28; t++) {
            float angle = PhoenixMath.chargeWingAngle(t);
            check(angle >= 15.0F - 1.0E-4F && angle <= 55.0F + 1.0E-4F, "the wings beat between 15 and 55 degrees");
        }
        assertNear(PhoenixMath.chargeWingAngle(3.5F), 55.0, 1.0E-3, "at the top of the beat");
        assertNear(PhoenixMath.chargeWingAngle(14.0F), PhoenixMath.chargeWingAngle(0.0F), 1.0E-3, "a beat takes fourteen ticks");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The firebird

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFirebirdHasTheSkeletonOfTheDesign(GameTestHelper helper) {
        Vec3 chest = new Vec3(5, 70, -3);
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 up = new Vec3(0, 1, 0);
        List<Firebird.Strand> bird = Firebird.build(chest, forward, up, 1.0F, 0.0F, 0.0F, true);
        Map<Firebird.Part, Integer> parts = count(bird);
        check(parts.get(Firebird.Part.SPINE) == 1, "one spine");
        check(parts.get(Firebird.Part.HEAD) == 2, "a head of two rings");
        check(parts.get(Firebird.Part.BEAK) == 2, "a beak of two ribbons");
        check(parts.get(Firebird.Part.CREST) == 3, "three crest ribbons");
        check(parts.get(Firebird.Part.PRIMARY) == 14, "two wings of seven primary feathers");
        check(parts.get(Firebird.Part.COVERT) == 10, "and five shorter feathers behind each");
        check(parts.get(Firebird.Part.TAIL) == 5, "five tail streamers");

        // The spine runs from 3 blocks behind the chest to 2.2 ahead and 1 above it.
        Firebird.Strand spine = first(bird, Firebird.Part.SPINE);
        Vec3 tail = spine.points()[0];
        Vec3 head = spine.points()[spine.points().length - 1];
        assertNear(tail.z - chest.z, -3.0, 1.0E-6, "the tail is three blocks behind");
        assertNear(head.z - chest.z, 2.2, 1.0E-6, "the head is 2.2 ahead");
        assertNear(head.y - chest.y, 1.0, 1.0E-6, "and a block above");

        // Feathers: seven per wing, the longest at the tip, 3.0 to 6.0 long (the curve is a little longer than its
        // chord).
        check(Firebird.primaryLength(0) == 3.0F && Firebird.primaryLength(6) == 6.0F, "primaries are 3.0 to 6.0 long");
        check(Firebird.streamerLength(0) == 2.5F && Math.abs(Firebird.streamerLength(4) - 4.1F) < 1.0E-5F, "streamers are 2.5 to 4.1 long");
        double previous = 0.0;
        for (Firebird.Strand strand : bird) {
            if (strand.part() == Firebird.Part.PRIMARY && strand.points()[strand.points().length - 1].x > chest.x + 1.0E-6) {
                double chord = strand.points()[0].distanceTo(strand.points()[strand.points().length - 1]);
                check(chord > previous - 1.0E-9, "the primaries of a wing come out longer one after the other");
                previous = chord;
            }
        }
        check(previous > 5.0, "the tip feather is about six blocks long");

        // The two wings mirror each other across the bird's plane.
        double left = 0;
        double right = 0;
        for (Firebird.Strand strand : bird) {
            if (strand.part() == Firebird.Part.PRIMARY) {
                for (Vec3 p : strand.points()) {
                    if (p.x > chest.x) {
                        right = Math.max(right, p.x - chest.x);
                    } else {
                        left = Math.max(left, chest.x - p.x);
                    }
                }
            }
        }
        assertNear(left, right, 1.0E-6, "the wings are as wide on both sides");
        check(right > 4.5, "a span of nine blocks or more at scale one");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFirebirdScalesWithOneNumberAndHasNoWingsUntilToldTo(GameTestHelper helper) {
        Vec3 chest = new Vec3(0, 10, 0);
        Vec3 forward = new Vec3(1, 0, 0);
        Vec3 up = new Vec3(0, 1, 0);
        List<Firebird.Strand> small = Firebird.build(chest, forward, up, 1.0F, 30.0F, 5.0F, true);
        List<Firebird.Strand> big = Firebird.build(chest, forward, up, 2.5F, 30.0F, 5.0F, true);
        check(small.size() == big.size(), "the same strands at every size");
        for (int i = 0; i < small.size(); i++) {
            Vec3[] a = small.get(i).points();
            Vec3[] b = big.get(i).points();
            check(a.length == b.length, "the same points");
            for (int k = 0; k < a.length; k++) {
                Vec3 offsetA = a[k].subtract(chest);
                Vec3 offsetB = b[k].subtract(chest);
                check(offsetB.distanceTo(offsetA.scale(2.5)) < 1.0E-4, "every point is 2.5 times as far from the chest");
            }
            assertNear(big.get(i).width(), small.get(i).width() * 2.5, 1.0E-5, "and every ribbon 2.5 times as wide");
        }
        List<Firebird.Strand> bare = Firebird.build(chest, forward, up, 1.0F, 30.0F, 5.0F, false);
        check(!count(bare).containsKey(Firebird.Part.PRIMARY) && !count(bare).containsKey(Firebird.Part.COVERT), "no wings before the charge is a fifth full");
        check(count(bare).get(Firebird.Part.TAIL) == 5 && count(bare).get(Firebird.Part.SPINE) == 1, "but the body and the tail are there");

        // The tail waves with a period of seven ticks, and the flap lifts the wing tips.
        Vec3 now = streamerTip(Firebird.build(chest, forward, up, 1.0F, 30.0F, 0.0F, true));
        Vec3 later = streamerTip(Firebird.build(chest, forward, up, 1.0F, 30.0F, 7.0F, true));
        Vec3 between = streamerTip(Firebird.build(chest, forward, up, 1.0F, 30.0F, 3.5F, true));
        check(now.distanceTo(later) < 1.0E-4, "the tail is back where it was seven ticks later");
        check(now.distanceTo(between) > 0.05, "and has moved in between");
        double high = highestTip(Firebird.build(chest, forward, up, 1.0F, 55.0F, 0.0F, true), chest);
        double low = highestTip(Firebird.build(chest, forward, up, 1.0F, -20.0F, 0.0F, true), chest);
        check(high > low + 1.0, "wings held up reach higher than wings flung down");
        helper.succeed();
    }

    /** The tip of the middle streamer of the tail. */
    private static Vec3 streamerTip(List<Firebird.Strand> bird) {
        Firebird.Strand strand = bird.stream().filter(s -> s.part() == Firebird.Part.TAIL).skip(2).findFirst().orElseThrow();
        return strand.points()[strand.points().length - 1];
    }

    private static double highestTip(List<Firebird.Strand> bird, Vec3 chest) {
        double best = -1.0E9;
        for (Firebird.Strand strand : bird) {
            if (strand.part() == Firebird.Part.PRIMARY) {
                best = Math.max(best, strand.points()[strand.points().length - 1].y - chest.y);
            }
        }
        return best;
    }

    private static Map<Firebird.Part, Integer> count(List<Firebird.Strand> bird) {
        Map<Firebird.Part, Integer> counts = new HashMap<>();
        for (Firebird.Strand strand : bird) {
            counts.merge(strand.part(), 1, Integer::sum);
        }
        return counts;
    }

    private static Firebird.Strand first(List<Firebird.Strand> bird, Firebird.Part part) {
        return bird.stream().filter(s -> s.part() == part).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The dome, the ring and the panel

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDomeGrowsHoldsAndFadesAndTheSenseRingSpreadsTwoBlocksATick(GameTestHelper helper) {
        assertNear(PhoenixMath.domeRadius(16.0F, 0.0F), 0.0, 1.0E-6, "the dome starts at her");
        assertNear(PhoenixMath.domeRadius(16.0F, 8.0F), 16.0, 1.0E-6, "and has its full radius after eight ticks");
        assertNear(PhoenixMath.domeRadius(16.0F, 20.0F), 16.0, 1.0E-6, "and keeps it");
        check(PhoenixMath.domeRadius(16.0F, 4.0F) > 0.0F && PhoenixMath.domeRadius(16.0F, 4.0F) < 16.0F, "growing between");
        check(PhoenixMath.DOME_TICKS == 24, "it lives 8 + 4 + 12 ticks");
        assertNear(PhoenixMath.domeAlpha(10.0F), 1.0, 1.0E-6, "full while it holds");
        assertNear(PhoenixMath.domeAlpha(18.0F), 0.5, 1.0E-6, "half-faded at the middle of the fade");
        assertNear(PhoenixMath.domeAlpha(24.0F), 0.0, 1.0E-9, "gone after 24 ticks");
        check(PhoenixMath.domeAlpha(2.0F) < PhoenixMath.domeAlpha(8.0F), "building up while it grows");

        assertNear(PhoenixMath.senseRadius(48.0F, 10.0F), 20.0, 1.0E-6, "the ring is at 20 blocks after ten ticks");
        assertNear(PhoenixMath.senseRadius(48.0F, 100.0F), 48.0, 1.0E-6, "and stops at the range");
        check(PhoenixMath.senseRingTicks(48.0F) == 24, "twenty-four ticks to cross 48 blocks");
        check(PhoenixMath.senseReaches(19.0, 48.0F, 10.0F), "a mind 19 blocks away has been passed after ten ticks");
        check(!PhoenixMath.senseReaches(21.0, 48.0F, 10.0F), "one at 21 has not");
        check(!PhoenixMath.senseReaches(60.0, 48.0F, 100.0F), "nothing beyond the range ever is");
        check(!PhoenixMath.senseReaches(1.0, 48.0F, -1.0F), "and nothing before the ping");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theMindReadingPanelSitsRightOfTheCrosshairStaysOnScreenAndTimesOut(GameTestHelper helper) {
        // A 427 x 240 screen (a 854 x 480 window at GUI scale 2), the crosshair in the middle, a 150 x 120 panel.
        int[] at = PhoenixMath.panelOrigin(427, 240, 150, 120, 213, 120);
        check(at[0] == 213 + 14, "fourteen pixels to the right of the crosshair");
        check(at[1] == 120 - 60, "centred on it vertically");
        // Near the right edge it flips to the left.
        int[] flipped = PhoenixMath.panelOrigin(427, 240, 150, 120, 400, 120);
        check(flipped[0] + 150 == 400 - 14, "to the left of the crosshair when it would run off the screen");
        // Too tall or too low: kept on the screen.
        int[] low = PhoenixMath.panelOrigin(427, 240, 150, 120, 213, 235);
        check(low[1] + 120 <= 240 - PhoenixMath.PANEL_MARGIN, "never below the bottom edge");
        int[] high = PhoenixMath.panelOrigin(427, 240, 150, 120, 213, 5);
        check(high[1] == PhoenixMath.PANEL_MARGIN, "nor above the top edge");
        int[] huge = PhoenixMath.panelOrigin(427, 240, 500, 300, 213, 120);
        check(huge[0] >= PhoenixMath.PANEL_MARGIN && huge[1] >= PhoenixMath.PANEL_MARGIN, "a panel larger than the screen is pinned to its corner");

        check(PhoenixMath.panelFresh(100, 100) && PhoenixMath.panelFresh(115, 100), "a snapshot is good for fifteen ticks");
        check(!PhoenixMath.panelFresh(116, 100), "and not for sixteen");
        check(!PhoenixMath.panelFresh(100, Long.MIN_VALUE), "none at all means no panel");
        check(!PhoenixMath.panelFresh(99, 100), "and one from the future is not believed");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFireGoesFromWhiteHotToEmberRedAndTheMixerMixes(GameTestHelper helper) {
        check(PhoenixMath.fireGradient(0.0F) == PhoenixMath.FIRE_CORE, "a ribbon starts white-hot");
        check(PhoenixMath.fireGradient(1.0F / 3.0F) == PhoenixMath.FIRE_GOLD, "goes gold a third of the way");
        check(PhoenixMath.fireGradient(2.0F / 3.0F) == PhoenixMath.FIRE_MAIN, "orange at two thirds");
        check(PhoenixMath.fireGradient(1.0F) == PhoenixMath.FIRE_EMBER, "and ember red at the tip");
        check(PhoenixMath.fireGradient(2.0F) == PhoenixMath.FIRE_EMBER && PhoenixMath.fireGradient(-1.0F) == PhoenixMath.FIRE_CORE, "never outside");
        assertNear(PhoenixMath.fireAlpha(0.0F), 0.85, 1.0E-6, "0.85 bright at the root");
        assertNear(PhoenixMath.fireAlpha(1.0F), 0.10, 1.0E-6, "and 0.1 at the tip");
        check(PhoenixMath.mix(0x000000, 0xFEFEFE, 0.5F) == 0x7F7F7F || PhoenixMath.mix(0x000000, 0xFEFEFE, 0.5F) == 0x808080, "a half way mix");
        check(PhoenixMath.mix(0x123456, 0xABCDEF, 0.0F) == 0x123456 && PhoenixMath.mix(0x123456, 0xABCDEF, 1.0F) == 0xABCDEF, "the ends of a mix");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The plug-in

    @GameTest(template = ARENA, batch = BATCH)
    public static void thePhoenixHasAClientThatSwitchesItsStancesOnFromHerData(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.PHOENIX);
        check(hero != null && hero.power() == Power.PHOENIX, "Jean has a client plug-in");
        check(hero.getClass().getSimpleName().equals("PhoenixClient"), "of her own");

        HeroAnimState state = new HeroAnimState(UUID.randomUUID());
        PowerData data = new PowerData();
        boolean[] on = new boolean[Stance.COUNT];
        hero.stanceTargets(state, null, data, on, 0L);
        check(!on[Stance.PHOENIX_READ.ordinal()] && !on[Stance.PHOENIX_CHARGE.ordinal()], "neither stance is on at rest");

        data.setToggle(Toggle.MIND_READING, true);
        hero.stanceTargets(state, null, data, on, 0L);
        check(on[Stance.PHOENIX_READ.ordinal()] && !on[Stance.PHOENIX_CHARGE.ordinal()], "Mind Reading tilts her head");

        data.setToggle(Toggle.MIND_READING, false);
        data.set(Live.PHOENIX_CHARGING, true);
        hero.stanceTargets(state, null, data, on, 0L);
        check(!on[Stance.PHOENIX_READ.ordinal()] && on[Stance.PHOENIX_CHARGE.ordinal()], "the charge throws her arms up in a V");

        check(!Stance.PHOENIX_READ.aimed && Stance.PHOENIX_CHARGE.aimed, "reading is an ambient stance, the charge an aimed one that holds both arms");
        check(Stance.PHOENIX_READ.tauIn == 6 && Stance.PHOENIX_READ.tauOut == 6, "reading eases in and out over six ticks");
        check(Stance.PHOENIX_CHARGE.tauIn == 1.5F && Stance.PHOENIX_CHARGE.tauOut == 4, "the charge comes in fast and goes out slowly");

        // The lean: -4 degrees (back) for the charge, +1 (forward) for reading, in proportion to the weights.
        assertNear(hero.lean(state), 0.0, 1.0E-6, "no lean at rest");
        state.stance[Stance.PHOENIX_CHARGE.ordinal()] = 1.0F;
        assertNear(hero.lean(state), -4.0, 1.0E-6, "the charge arches her back four degrees");
        state.stance[Stance.PHOENIX_READ.ordinal()] = 1.0F;
        assertNear(hero.lean(state), -3.0, 1.0E-6, "and reading adds a degree forward");
        state.stance[Stance.PHOENIX_CHARGE.ordinal()] = 0.5F;
        assertNear(hero.lean(state), -1.0, 1.0E-6, "in proportion to the weights");
        assertNear(hero.drop(state), 0.0, 1.0E-6, "and sinks nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The art

    @GameTest(template = ARENA, batch = BATCH)
    public static void theEyeMasksPaintTheEightEyePixelsAndTheFlameTextureTiles(GameTestHelper helper) throws IOException {
        BufferedImage fire = texture("phoenix_eyes");
        BufferedImage psychic = texture("phoenix_eyes_psychic");
        check(fire.getWidth() == 64 && fire.getHeight() == 64 && psychic.getWidth() == 64 && psychic.getHeight() == 64, "both masks are the size of a skin");
        int[][] expected = {{0xFF8A1F, 0xFFF4C2}, {0x7C5CE0, 0xF4EEFF}};
        BufferedImage[] masks = {fire, psychic};
        for (int m = 0; m < 2; m++) {
            int painted = 0;
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int argb = masks[m].getRGB(x, y);
                    boolean eye = (x == 9 || x == 10 || x == 13 || x == 14) && (y == 11 || y == 12);
                    if (eye) {
                        check((argb >>> 24) == 0xFF, "the eye pixel (" + x + "," + y + ") is painted");
                        check((argb & 0xFFFFFF) == expected[m][y - 11], "in the colour of its row (" + x + "," + y + ")");
                        painted++;
                    } else {
                        check((argb >>> 24) == 0, "nothing outside the eyes (" + x + "," + y + ")");
                    }
                }
            }
            check(painted == 8, "eight pixels, two by two on each eye");
        }

        BufferedImage flame = texture("phoenix_flame");
        check(flame.getWidth() == 64 && flame.getHeight() == 64, "the flame texture is 64 by 64");
        int lit = 0;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                if (alpha(flame, x, y) > 16) {
                    lit++;
                }
            }
        }
        // It repeats in both directions: the step across the edge is about as big as a step anywhere else.
        double seam = 0.0;
        double inner = 0.0;
        for (int i = 0; i < 64; i++) {
            seam += Math.abs(alpha(flame, 63, i) - alpha(flame, 0, i)) + Math.abs(alpha(flame, i, 63) - alpha(flame, i, 0));
            for (int k = 0; k < 63; k++) {
                inner += Math.abs(alpha(flame, k, i) - alpha(flame, k + 1, i)) + Math.abs(alpha(flame, i, k) - alpha(flame, i, k + 1));
            }
        }
        double seamMean = seam / 128.0;
        double innerMean = inner / (2.0 * 63 * 64);
        check(lit > 1000 && lit < 4000, "tongues of flame on a dark ground, not a flat sheet");
        check(seamMean < innerMean * 1.5 + 2.0, "and it tiles without a seam (" + seamMean + " across the edge, " + innerMean + " elsewhere)");
        helper.succeed();
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    private static BufferedImage texture(String name) throws IOException {
        try (InputStream in = PhoenixClientTests.class.getResourceAsStream("/assets/enhanced/textures/entity/" + name + ".png")) {
            check(in != null, name + ".png is in the mod");
            return ImageIO.read(in);
        }
    }
}
