package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.client.render.ApocalypseHeroMath;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.client.render.ApocalypseView;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.ShieldStyle;
import com.kolodkingal.enhanced.client.render.ShieldStyles;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.client.render.StancePoses;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The hero half of the client stage of Apocalypse: his eight gestures and two stances, the way the shared tables route the
 * changes in his data to them, his flight profile, the numbers behind his HUD lines and his shield's look. Pure (no screen,
 * no world), so they share a batch of their own with {@link ApocalypseClientTests}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public class ApocalypseAnimationTests {
    private static final String PURE = "apocalypse_client_pure";
    private static final GestureId[] CAST_GESTURES = {GestureId.APOC_SAND_WAVE, GestureId.APOC_ENTOMB, GestureId.APOC_PORTAL, GestureId.APOC_SHIELD_RAISE,
            GestureId.APOC_SHIELD_DROP, GestureId.APOC_BESTOW, GestureId.APOC_STORM_START};

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    // ------------------------------------------------------------------------------------------------------------
    // The gestures and the stances

    @GameTest(template = ARENA, batch = PURE)
    public static void theEightGesturesHaveTheirEnvelopesAndKeys(GameTestHelper helper) {
        Object[][] table = {
                // id, group, duration, attack, release, force
                {GestureId.TAKEOFF_APOC, GestureId.Group.TAKEOFF, 22, 3, 8, true},
                {GestureId.APOC_SAND_WAVE, GestureId.Group.CAST, 18, 2, 6, false},
                {GestureId.APOC_ENTOMB, GestureId.Group.CAST, 26, 3, 8, false},
                {GestureId.APOC_PORTAL, GestureId.Group.CAST, 20, 2, 6, false},
                {GestureId.APOC_SHIELD_RAISE, GestureId.Group.CAST, 15, 2, 5, true},
                {GestureId.APOC_SHIELD_DROP, GestureId.Group.CAST, 8, 1, 4, false},
                {GestureId.APOC_BESTOW, GestureId.Group.CAST, 22, 2, 7, false},
                {GestureId.APOC_STORM_START, GestureId.Group.CAST, 18, 2, 6, true},
        };
        for (Object[] row : table) {
            GestureId id = (GestureId) row[0];
            check(id.group == row[1], id + " is in the group " + row[1]);
            check(id.dur == (int) row[2] && id.attack == (int) row[3] && id.release == (int) row[4], id + " has the envelope of the design");
            check(id.force == (boolean) row[5], id + " forces or not as the design says");
            Gestures.Def def = Gestures.get(id);
            check(def != null, id + " has its keys");
            check(def.problems().isEmpty(), id + ": " + def.problems());
            check(!def.m.isEmpty(), id + " drives the main arm");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void everyGestureLeavesTheArmsWhereTheNextOneFindsThem(GameTestHelper helper) {
        // They all begin and end on the same relaxed pose, so one runs into the next with no pop (the take-off, the raise, the
        // drop and the storm hand over to the flight pose or to a stance instead, and are checked against those).
        for (GestureId id : new GestureId[]{GestureId.APOC_SAND_WAVE, GestureId.APOC_ENTOMB, GestureId.APOC_PORTAL, GestureId.APOC_BESTOW}) {
            Gestures.Def def = Gestures.get(id);
            sameAsRest(def.m.get(0), true, id + " begins");
            sameAsRest(def.m.get(def.m.size() - 1), true, id + " ends");
            if (!def.o.isEmpty()) {
                sameAsRest(def.o.get(0), false, id + " begins (off arm)");
                sameAsRest(def.o.get(def.o.size() - 1), false, id + " ends (off arm)");
            }
        }
        Gestures.Def raise = Gestures.get(GestureId.APOC_SHIELD_RAISE);
        sameAsRest(raise.m.get(0), true, "the raise begins");
        sameAsRest(raise.o.get(0), false, "the raise begins (off arm)");
        Gestures.Def drop = Gestures.get(GestureId.APOC_SHIELD_DROP);
        sameAsRest(drop.m.get(drop.m.size() - 1), true, "the drop ends");
        sameAsRest(drop.o.get(drop.o.size() - 1), false, "the drop ends (off arm)");
        // The drop begins in the stance it lets go of.
        arms(drop.m.get(0), StancePoses.APOC_SHIELD_M, "the drop begins in the shield's stance");
        arms(drop.o.get(0), StancePoses.APOC_SHIELD_O, "the drop begins in the shield's stance (off arm)");
        Gestures.Def storm = Gestures.get(GestureId.APOC_STORM_START);
        sameAsRest(storm.m.get(0), true, "the storm begins");
        // The take-off begins at rest, too.
        Gestures.Def takeoff = Gestures.get(GestureId.TAKEOFF_APOC);
        sameAsRest(takeoff.m.get(0), true, "the take-off begins");
        helper.succeed();
    }

    private static void sameAsRest(Gestures.ArmKey key, boolean main, String what) {
        float[] rest = main ? new float[]{-10, 0, 10} : new float[]{-10, 0, -10};
        arms(key, rest, what + " on the relaxed pose");
    }

    private static void arms(Gestures.ArmKey key, float[] pose, String what) {
        check(key.mode == Gestures.Mode.ABS, what + ": absolute angles");
        assertNear(key.x, pose[0], 0.5, what + " (x)");
        assertNear(key.y, pose[1], 0.5, what + " (y)");
        assertNear(key.z, pose[2], 0.5, what + " (z)");
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void theShieldRaiseIsPlayedAtTheSpeedOfItsWindUp(GameTestHelper helper) {
        check(GestureRoutes.SHIELD_KEYED_WIND_UP == GestureId.APOC_SHIELD_RAISE.dur, "the raise is keyed for the wind-up the keys are made for");
        assertNear(ApocalypseHeroMath.raiseRate(10), 1.5, 1.0E-6, "a wind-up of ten ticks plays the fifteen of the keys in ten");
        assertNear(ApocalypseHeroMath.raiseRate(15), 1.0, 1.0E-6, "fifteen plays them as keyed");
        assertNear(ApocalypseHeroMath.raiseRate(30), 0.5, 1.0E-6, "thirty twice as slowly");
        assertNear(ApocalypseHeroMath.raiseRate(0), 1.0, 1.0E-6, "none plays them as keyed");
        for (int windUp = 4; windUp <= 60; windUp += 4) {
            assertNear(GestureId.APOC_SHIELD_RAISE.dur / ApocalypseHeroMath.raiseRate(windUp), windUp, 1.0E-3, "the arms are open at tick " + windUp);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void theTwoStancesHoldTheArmsAndLeanTheBody(GameTestHelper helper) {
        check(Stance.APOC_SHIELD.aimed && Stance.APOC_STORM.aimed, "both hold the arms (the swing of an attack is muted)");
        assertNear(Stance.APOC_SHIELD.tauIn, 1.5, 1.0E-6, "the shield comes up quickly");
        assertNear(Stance.APOC_SHIELD.tauOut, 3.0, 1.0E-6, "and goes down slowly");
        assertNear(Stance.APOC_STORM.tauIn, 3.0, 1.0E-6, "the storm rises more slowly");
        assertNear(Stance.APOC_STORM.tauOut, 5.0, 1.0E-6, "and settles more slowly");
        assertNear(ApocalypseHeroMath.lean(0, 0), 0.0, 1.0E-6, "no lean without a stance");
        assertNear(ApocalypseHeroMath.lean(1, 0), -2.0, 1.0E-6, "the shield leans him back two degrees");
        assertNear(ApocalypseHeroMath.lean(0, 1), -3.0, 1.0E-6, "the storm three");
        assertNear(ApocalypseHeroMath.lean(0.5F, 0.5F), -2.5, 1.0E-6, "and they sum by weight");
        HeroClient hero = HeroClients.of(Power.APOCALYPSE);
        check(hero != null && hero.power() == Power.APOCALYPSE, "Apocalypse has a client plug-in");
        // The arms of the shield are raised in front of the chest, the storm's flung wide and up.
        check(StancePoses.APOC_SHIELD_M[0] == -78 && StancePoses.APOC_SHIELD_O[2] == -24, "the shield's pose");
        check(StancePoses.APOC_STORM_M[0] == -118 && StancePoses.APOC_STORM_M[2] == 74 && StancePoses.APOC_STORM_O[2] == -74, "the storm's pose");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The routes

    @GameTest(template = ARENA, batch = PURE)
    public static void theChangesInHisDataStandForTheirGestures(GameTestHelper helper) {
        Power p = Power.APOCALYPSE;
        check(GestureRoutes.cooldown(p, 1, 60) == GestureId.APOC_SAND_WAVE, "the short cooldown of R is the Wave");
        check(GestureRoutes.cooldown(p, 1, 240) == GestureId.APOC_ENTOMB, "the long one is the Entomb");
        check(GestureRoutes.cooldown(p, 3, 160) == GestureId.APOC_PORTAL, "V is the Portal");
        check(GestureRoutes.cooldown(p, 5, 400) == GestureId.APOC_BESTOW, "N is the Bestowal");
        check(GestureRoutes.cooldown(p, 2, 99) == null && GestureRoutes.cooldown(p, 4, 99) == null, "flight and the shield are not cooldowns");
        check(GestureRoutes.flag(p, GestureRoutes.Flag.CHAOS_SHIELD, true, false, false) == GestureId.APOC_SHIELD_RAISE, "the shield rises");
        check(GestureRoutes.flag(p, GestureRoutes.Flag.CHAOS_SHIELD, false, false, false) == GestureId.APOC_SHIELD_DROP, "and falls");
        for (GestureRoutes.Flag flag : GestureRoutes.Flag.values()) {
            if (flag != GestureRoutes.Flag.CHAOS_SHIELD) {
                check(GestureRoutes.flag(p, flag, true, false, false) == null && GestureRoutes.flag(p, flag, false, false, false) == null,
                        "the flag " + flag + " is not his");
            }
        }
        check(GestureRoutes.live(p, Live.SANDSTORM, true) == GestureId.APOC_STORM_START, "the storm rising");
        check(GestureRoutes.reason(Live.SANDSTORM) == null, "so the storm needs no reason for having none");
        for (Live live : Live.values()) {
            if (live != Live.SANDSTORM) {
                check(GestureRoutes.live(p, live, true) == null, "the state " + live + " is not his");
            }
        }
        check(GestureRoutes.live(Power.SCARLET_WITCH, Live.SANDSTORM, true) == null, "nor does the storm move anyone else");
        check(GestureRoutes.flag(Power.SCARLET_WITCH, GestureRoutes.Flag.CHAOS_SHIELD, true, false, false) == GestureId.WANDA_SHIELD_RAISE,
                "and his shield does not change Wanda's");
        check(GestureRoutes.startedElsewhere(GestureId.TAKEOFF_APOC), "the take-off is started by the animator's own edge");
        for (GestureId id : CAST_GESTURES) {
            check(!GestureRoutes.startedElsewhere(id), id + " is started by a route of the table");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void everyAbilityIsActedByTheWayTheTableSays(GameTestHelper helper) {
        Power p = Power.APOCALYPSE;
        check(GestureRoutes.acting(p, 1).equals("cooldown"), "R is acted by a cooldown");
        check(GestureRoutes.acting(p, 2).equals("flight"), "G by the flight poses");
        check(GestureRoutes.acting(p, 3).equals("cooldown"), "V by a cooldown");
        check(GestureRoutes.acting(p, 4).equals("flag:CHAOS_SHIELD"), "B by the flag of the shield");
        check(GestureRoutes.acting(p, 5).equals("cooldown"), "N by a cooldown");
        HeroClient hero = HeroClients.of(p);
        check(hero.takeoffGesture() == GestureId.TAKEOFF_APOC, "he takes off in his own gesture");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void aKeyPressStartsItsGestureOnlyWhereTheAbilityWillWork(GameTestHelper helper) {
        HeroClient hero = HeroClients.of(Power.APOCALYPSE);
        PowerData data = new PowerData();
        long now = 1000L;
        check(hero.predictedGesture(null, data, 1, now, false) == GestureId.APOC_SAND_WAVE, "R is the Wave");
        check(hero.predictedGesture(null, data, 1, now, true) == GestureId.APOC_ENTOMB, "Sneak + R the Entomb");
        check(hero.predictedGesture(null, data, 3, now, false) == GestureId.APOC_PORTAL, "V the Portal");
        check(hero.predictedGesture(null, data, 5, now, false) == GestureId.APOC_BESTOW, "N the Bestowal");
        check(hero.predictedGesture(null, data, 5, now, true) == GestureId.APOC_STORM_START, "Sneak + N the storm");
        check(hero.predictedGesture(null, data, 2, now, false) == null, "G is flight, not a gesture");
        data.startCooldown(1, now, 60);
        data.startCooldown(5, now, 400);
        check(hero.predictedGesture(null, data, 1, now + 10, false) == null, "nothing while R recharges");
        check(hero.predictedGesture(null, data, 5, now + 10, true) == null, "nor N");
        check(hero.predictedGesture(null, data, 1, now + 60, false) == GestureId.APOC_SAND_WAVE, "and again when it is ready");
        data.raiseChaosShield(now, 10, 60.0F);
        check(hero.predictedGesture(null, data, 4, now + 5, false) == null, "a shield that is up is not raised again");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Flight and run

    @GameTest(template = ARENA, batch = PURE)
    public static void heFliesCalmAndUprightWithTheArmsALittleOpen(GameTestHelper helper) {
        FlightProfile profile = FlightProfile.of(Power.APOCALYPSE, false);
        check(profile != null, "he flies with a profile of his own");
        assertNear(profile.p1, 30, 1.0E-6, "he leans 30 degrees at a cruise");
        assertNear(profile.p2, 56, 1.0E-6, "and 56 in a sprint");
        assertNear(profile.pBack, -6, 1.0E-6, "and a little back when he flies backwards");
        assertNear(profile.maxRoll, 10, 1.0E-6, "he banks no further than ten degrees");
        check(profile.noBrake, "he stops without a braking gesture");
        check(!profile.spring && !profile.mWorld, "a plain lean, the arm held by its angle");
        assertNear(profile.defaultSpeed, 0.08, 1.0E-6, "at the speed of the others of his kind");
        float[][] anchors = {
                {-8, 0, 14, -8, 0, -14, 2, 1, 1, -1},
                {-14, 0, 20, -14, 0, -20, 4, 1, 3, -1},
                {-26, 0, 26, -26, 0, -26, 6, 1, 5, -2},
        };
        for (int a = 0; a < 3; a++) {
            for (int i = 0; i < 10; i++) {
                assertNear(profile.anchor(a, i), anchors[a][i], 1.0E-6, "anchor " + a + " value " + i);
            }
        }
        // The arms open further the faster he goes.
        check(profile.anchor(FlightProfile.HOVER, 2) < profile.anchor(FlightProfile.CRUISE, 2)
                && profile.anchor(FlightProfile.CRUISE, 2) < profile.anchor(FlightProfile.SPRINT, 2), "the arms open with speed");
        // The take-off ends on the hover pose.
        Gestures.Def takeoff = Gestures.get(GestureId.TAKEOFF_APOC);
        Gestures.ArmKey m = takeoff.m.get(takeoff.m.size() - 1);
        assertNear(m.x, profile.anchor(FlightProfile.HOVER, 0), 0.5, "the take-off ends on the hover pose");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The HUD, the eyes and the shield

    @GameTest(template = ARENA, batch = PURE)
    public static void theFiveLinesOfTheHudSayWhatTheyShould(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 5000L;
        check(ApocalypseView.status(data, 1, now) == null, "R is ready: nothing to say");
        data.startCooldown(1, now, 60);
        Component cooldown = ApocalypseView.status(data, 1, now + 10);
        check(cooldown != null && cooldown.getString().equals("2.5s"), "R shows the seconds it has left, " + cooldown);
        check(cooldown.getStyle().getColor().getValue() == ChatFormatting.GRAY.getColor(), "in grey");
        data.startCooldown(3, now, 160);
        check(ApocalypseView.status(data, 3, now).getString().equals("8.0s"), "and V");
        // G is flight, on or off.
        check(key(ApocalypseView.status(data, 2, now)).equals("hud.enhanced.off"), "levitation is off");
        data.setFlying(true);
        check(key(ApocalypseView.status(data, 2, now)).equals("hud.enhanced.on"), "and on");
        // B is the shield: forming, then what is left, then the cooldown.
        check(ApocalypseView.status(data, 4, now) == null, "no shield, no line");
        int windUp = 10;
        data.raiseChaosShield(now, windUp, ApocalypseGuard.maxStrength());
        check(key(ApocalypseView.status(data, 4, now + 3)).equals("hud.enhanced.shield_forming"), "forming");
        Component up = ApocalypseView.status(data, 4, now + windUp);
        check(key(up).equals("hud.enhanced.shield_strength"), "then its strength");
        check(up.getStyle().getColor().getValue() == ChatFormatting.GREEN.getColor(), "in green while it is whole");
        data.setShieldStrength(ApocalypseGuard.maxStrength() * 0.2F);
        check(ApocalypseView.status(data, 4, now + windUp).getStyle().getColor().getValue() == ChatFormatting.RED.getColor(), "and red when it is nearly spent");
        data.setChaosShieldActive(false);
        data.startCooldown(4, now + 40, 100);
        check(ApocalypseView.status(data, 4, now + 50).getString().equals("4.5s"), "after it drops, the cooldown");
        // N is the cooldown of the Bestowal, or the seconds the storm has left.
        data.startCooldown(5, now, 400);
        check(ApocalypseView.status(data, 5, now).getString().equals("20.0s"), "N shows the cooldown");
        data.set(Live.SANDSTORM, true);
        data.startCooldown(5, now, ApocalypseConfig.STORM_COOLDOWN.get());
        Component storm = ApocalypseView.status(data, 5, now + 36);
        check(key(storm).equals("hud.enhanced.apocalypse_storm"), "and 'storm' while it rages");
        String seconds = String.format(java.util.Locale.ROOT, "%.1fs", (ApocalypseConfig.STORM_TICKS.get() - 36) / 20.0);
        check(((TranslatableContents) storm.getContents()).getArgs()[0].equals(seconds), "with the seconds it has left, " + seconds);
        check(storm.getStyle().getColor().getValue() == ChatFormatting.GOLD.getColor(), "in gold");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void theStormTimerCountsDownFromTheCooldownOfItsKey(GameTestHelper helper) {
        // A storm of 120 ticks with a cooldown of 600: both begin together.
        check(ApocalypseHeroMath.stormLeft(600, 120, 600) == 120, "all of it at the start");
        check(ApocalypseHeroMath.stormLeft(540, 120, 600) == 60, "half after sixty ticks");
        check(ApocalypseHeroMath.stormLeft(480, 120, 600) == 0, "none after one hundred and twenty");
        check(ApocalypseHeroMath.stormLeft(100, 120, 600) == 0, "and never below none");
        check(ApocalypseHeroMath.stormLeft(900, 120, 600) == 120, "nor above all of it");
        // The Entomb is the longer cooldown of R.
        check(!ApocalypseHeroMath.isEntomb(60, 60, 240), "sixty is the Wave");
        check(ApocalypseHeroMath.isEntomb(240, 60, 240), "two hundred and forty the Entomb");
        check(!ApocalypseHeroMath.isEntomb(59, 60, 240), "a tick under the Wave's is still the Wave");
        check(ApocalypseHeroMath.isEntomb(99, 60, 240), "and anything above it is the Entomb");
        check(!ApocalypseHeroMath.isEntomb(240, 240, 240), "with equal cooldowns it is always the Wave");
        check(ApocalypseHeroMath.eyesGlow(true, false) && ApocalypseHeroMath.eyesGlow(false, true) && ApocalypseHeroMath.eyesGlow(true, true), "the eyes glow");
        check(!ApocalypseHeroMath.eyesGlow(false, false), "unless neither");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void hisShieldIsGoldOnTheGoldenWebWithTheRadiusOfTheConfig(GameTestHelper helper) {
        ShieldStyle style = ShieldStyles.of(Power.APOCALYPSE);
        check(style != null, "he has a shield style");
        check(style.veil() == 0xF5C84A && style.lines() == 0xFFF0B8, "gold, with a paler gold for the lattice");
        check(style.texture().getPath().equals("textures/entity/golden_web.png"), "on the golden web");
        assertNear(style.radius(), ApocalypseGuard.radius(), 1.0E-4, "the radius the server asks for");
        assertNear(style.maxStrength().get(), ApocalypseGuard.maxStrength(), 1.0E-4, "and its strength");
        check(style.maxTicks().getAsInt() == ApocalypseGuard.maxTicks(), "and the time it can be held");
        check(ShieldStyles.of(Power.SCARLET_WITCH) == ShieldStyles.CHAOS_SHIELD, "and Wanda's is still hers");
        // The dome grows from his hands to its full size as it forms.
        PowerData data = new PowerData();
        data.raiseChaosShield(1000, 10, ApocalypseGuard.maxStrength());
        assertNear(style.look(data, 1000.0F).radius(), style.radius() * 0.2, 1.0E-4, "a fifth of its size at first");
        assertNear(style.look(data, 1010.0F).radius(), style.radius(), 1.0E-4, "all of it when it is complete");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PURE)
    public static void hisEyesBelongToTheLookOfHisSuit(GameTestHelper helper) {
        boolean ancient = false;
        boolean modern = false;
        for (var variant : Variants.of(Power.APOCALYPSE)) {
            ancient |= variant.id().equals(ApocalypseView.ANCIENT_LOOK);
            modern |= variant.id().equals("apocalypse");
        }
        check(ancient, "the ancient look is the one whose id the eyes know");
        check(modern, "and the film's look has its own");
        check(ApocalypseView.EYES.getPath().equals("textures/entity/apocalypse_eyes.png"), "the film's mask");
        check(ApocalypseView.EYES_ANCIENT.getPath().equals("textures/entity/apocalypse_eyes_ancient.png"), "and the ancient one");
        helper.succeed();
    }
}
