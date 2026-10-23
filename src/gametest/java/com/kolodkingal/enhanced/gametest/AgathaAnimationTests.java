package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.client.render.AgathaGestures;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.ShieldStyles;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.client.render.StancePoses;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * How Agatha is acted (3.3), with no client class: which gesture stands for each of her abilities, her take-off and her
 * shield ending on the poses that take over, what her own key press starts at once, when her eyes burn, and her shield's
 * HUD line. (The rows of the shared tables are in {@code AnimationMathTests}' regions.)
 */
public final class AgathaAnimationTests {
    private static final String BATCH = "agatha_animation";

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyAbilityOfHersIsActed(GameTestHelper helper) {
        Power p = Power.AGATHA;
        check(GestureRoutes.acting(p, 1).equals("live:BEAMING"), "R by the beam's state");
        check(GestureRoutes.acting(p, 2).equals("flight"), "G by the flight poses");
        check(GestureRoutes.acting(p, 3).equals("cooldown") && GestureRoutes.cooldown(p, 3, 300) == GestureId.AGATHA_DRAIN, "V by its cooldown");
        check(GestureRoutes.acting(p, 4).equals("flag:CHAOS_SHIELD"), "B by the shield's flag");
        check(GestureRoutes.flag(p, GestureRoutes.Flag.CHAOS_SHIELD, true, false, false) == GestureId.AGATHA_SHIELD_RAISE
                && GestureRoutes.flag(p, GestureRoutes.Flag.CHAOS_SHIELD, false, false, false) == GestureId.AGATHA_SHIELD_DROP, "raised and let go");
        check(GestureRoutes.acting(p, 5).equals("cooldown") && GestureRoutes.cooldown(p, 5, 400) == GestureId.AGATHA_RUNES, "N by its cooldown");
        check(GestureRoutes.live(p, Live.BEAMING, false) == GestureId.AGATHA_BEAM, "the beam begins with her own thrust");
        check(GestureRoutes.live(p, Live.CASTING, false) == GestureId.AGATHA_HEX, "the hex mark (Sneak + R) raises her hand");
        check(GestureRoutes.cooldown(p, 1, 20) == null && GestureRoutes.cooldown(p, 4, 40) == null, "no other cooldown plays a gesture");
        HeroClient hero = HeroClients.of(p);
        check(hero != null && hero.takeoffGesture() == GestureId.TAKEOFF_AGATHA && hero.startedElsewhere(GestureId.TAKEOFF_AGATHA),
                "the take-off is hers and started by the animator's edge");
        for (GestureId id : List.of(GestureId.AGATHA_BEAM, GestureId.AGATHA_HEX, GestureId.AGATHA_DRAIN, GestureId.AGATHA_SHIELD_RAISE,
                GestureId.AGATHA_SHIELD_DROP, GestureId.AGATHA_RUNES)) {
            check(!GestureRoutes.startedElsewhere(id), id + " is started by a route of the table");
            check(Gestures.get(id) != null && Gestures.get(id).problems().isEmpty(), id + " has well-formed keys");
        }
        check(Stance.AGATHA_SHIELD.aimed, "the shield's stance holds the arms");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herTakeOffAndShieldEndOnThePosesThatTakeOver(GameTestHelper helper) {
        FlightProfile profile = FlightProfile.of(Power.AGATHA, false);
        check(profile != null, "she flies with a profile of her own");
        for (int i = 0; i < 3; i++) {
            assertNear(AgathaGestures.HOVER_M[i], profile.anchor(FlightProfile.HOVER, i), 1.0E-4, "the hover arm " + i);
            assertNear(AgathaGestures.HOVER_O[i], profile.anchor(FlightProfile.HOVER, 3 + i), 1.0E-4, "the hover off arm " + i);
        }
        assertNear(AgathaGestures.HOVER_LEG_R, profile.anchor(FlightProfile.HOVER, 6), 1.0E-4, "the right leg");
        assertNear(AgathaGestures.HOVER_LEG_L, profile.anchor(FlightProfile.HOVER, 8), 1.0E-4, "the left leg");
        check(profile.p1 < 30.0F && profile.p1 > 0.0F, "she leans little into a cruise: " + profile.p1);
        Gestures.Def raise = Gestures.get(GestureId.AGATHA_SHIELD_RAISE);
        Gestures.ArmKey m = raise.m.get(raise.m.size() - 1);
        assertNear(m.x, StancePoses.AGATHA_SHIELD_M[0], 0.5, "the raise ends on the stance");
        Gestures.Def drop = Gestures.get(GestureId.AGATHA_SHIELD_DROP);
        Gestures.ArmKey first = drop.m.get(0);
        assertNear(first.x, StancePoses.AGATHA_SHIELD_M[0], 0.5, "and the drop starts from it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herOwnPressActsOnlyWhatTheServerWillDo(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 1000L;
        check(AgathaGestures.predicted(data, 4, now, true) == GestureId.AGATHA_SHIELD_RAISE, "B, rested and fed: the shield at once");
        check(AgathaGestures.predicted(data, 4, now, false) == null, "not on an empty stomach");
        check(AgathaGestures.predicted(data, 5, now, true) == GestureId.AGATHA_RUNES, "N, rested: the runes");
        data.startCooldown(5, now, 400);
        check(AgathaGestures.predicted(data, 5, now + 10, true) == null, "not while they recharge");
        check(AgathaGestures.predicted(data, 3, now, true) == null && AgathaGestures.predicted(data, 1, now, true) == null,
                "the drain and the beam wait for the server");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herEyesBurnWhileSheWorksMagic(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 1000L;
        check(!AgathaGestures.eyesGlow(data, now, 300, 400), "at rest they do not");
        data.set(Live.BEAMING, true);
        check(AgathaGestures.eyesGlow(data, now, 300, 400), "the beam");
        data.set(Live.BEAMING, false);
        data.set(Live.CASTING, true);
        check(AgathaGestures.eyesGlow(data, now, 300, 400), "the hex");
        data.set(Live.CASTING, false);
        data.startCooldown(3, now, 300);
        check(AgathaGestures.eyesGlow(data, now + 5, 300, 400), "just after the drain");
        check(!AgathaGestures.eyesGlow(data, now + AgathaGestures.GLOW_AFTER_CAST + 1, 300, 400), "and not long after");
        helper.succeed();
    }

    private static String key(net.minecraft.network.chat.Component line) {
        return line != null && line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herShieldsHudLineIsTheSharedOne(GameTestHelper helper) {
        PowerData data = new PowerData();
        data.raiseChaosShield(100L, 8, 40.0F);
        check(key(ShieldStyles.hudLine(data, 104L, 40.0F, 200)).equals("hud.enhanced.shield_forming"), "forming");
        check(key(ShieldStyles.hudLine(data, 120L, 40.0F, 200)).equals("hud.enhanced.shield_strength"), "then its strength");
        data.setChaosShieldActive(false);
        check(ShieldStyles.hudLine(data, 130L, 40.0F, 200) == null, "and nothing once it is down");
        check(ShieldStyles.of(Power.AGATHA) != null && ShieldStyles.of(Power.AGATHA).veil() != ShieldStyles.CHAOS_SHIELD.veil(),
                "her shield is drawn in a style of its own");
        helper.succeed();
    }
}
