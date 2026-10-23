package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.client.custom.CustomAnimRoutes;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.client.render.StrangeGestures;
import com.kolodkingal.enhanced.custom.AnimCatalog;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * How Doctor Strange is acted (3.6), with no client class: which gesture stands for each of his keys and switches, the
 * gestures' keys, what starts them, his flight on the Cloak (its profile, its take-off ending on the hover pose), his
 * walk, and the Cloak as a custom hero's flight style.
 */
public final class StrangeAnimationTests {
    private static final String BATCH = "strange";

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyKeyAndSwitchOfHisIsActed(GameTestHelper helper) {
        Power p = Power.DOCTOR_STRANGE;
        check(GestureRoutes.acting(p, 1).startsWith("none: "), "R by the whip's payload");
        check(GestureRoutes.acting(p, 2).equals("flight"), "G by the Cloak's flight");
        check(GestureRoutes.acting(p, 3).equals("live:STRANGE_SHIELD")
                && GestureRoutes.live(p, Live.STRANGE_SHIELD, true) == GestureId.STRANGE_SHIELD_UP, "V by the Shield's state");
        check(GestureRoutes.acting(p, 4).startsWith("none: ") && GestureRoutes.acting(p, 5).startsWith("none: "),
                "B and N by their payloads and switches");
        check(GestureRoutes.toggle(p, Toggle.STRANGE_ASTRAL, true) == GestureId.STRANGE_ASTRAL
                && GestureRoutes.toggle(p, Toggle.STRANGE_ASTRAL, false) == GestureId.STRANGE_ASTRAL_IN, "the spirit out and back");
        check(GestureRoutes.toggle(p, Toggle.STRANGE_MIRROR, true) == GestureId.STRANGE_MIRROR, "the Mirror Dimension");
        check(GestureRoutes.toggle(p, Toggle.TIME_SLOW, true) == GestureId.STRANGE_EYE_OPEN, "the Eye opens");
        for (Live live : List.of(Live.STRANGE_SHIELD, Live.STRANGE_ASTRAL, Live.STRANGE_MIRROR, Live.STRANGE_CASTING)) {
            String reason = GestureRoutes.reason(live);
            check(reason == null || !reason.startsWith("pending"), live + " has a gesture or a reason of its own");
        }
        HeroClient hero = HeroClients.of(p);
        for (GestureId id : List.of(GestureId.STRANGE_WHIP_1, GestureId.STRANGE_WHIP_2, GestureId.STRANGE_WHIP_3,
                GestureId.STRANGE_GRAPPLE, GestureId.STRANGE_BANDS, GestureId.STRANGE_DOME, GestureId.STRANGE_SLING,
                GestureId.STRANGE_REWIND, GestureId.TAKEOFF_STRANGE)) {
            check(hero != null && hero.startedElsewhere(id), id + " is started by its payload or edge");
        }
        for (GestureId id : GestureId.values()) {
            if (id.name().startsWith("STRANGE_") || id == GestureId.TAKEOFF_STRANGE) {
                Gestures.Def def = Gestures.get(id);
                check(def != null && def.problems().isEmpty(), id + " has well-formed keys");
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisRowsPlayHisGesturesForACustomHero(GameTestHelper helper) {
        check(HeroAbilityAnims.shown(HeroAbility.STRANGE_WHIP, false) == GestureId.STRANGE_WHIP_1
                && HeroAbilityAnims.shown(HeroAbility.STRANGE_WHIP, true) == GestureId.STRANGE_GRAPPLE, "the whip and grapple");
        check(HeroAbilityAnims.stances(HeroAbility.STRANGE_SHIELDS).contains(Stance.STRANGE_SHIELD), "the Shield's stance");
        check(HeroAbilityAnims.owners(Toggle.STRANGE_ASTRAL).contains(HeroAbility.STRANGE_BANDS)
                && HeroAbilityAnims.owners(Live.STRANGE_SHIELD).contains(HeroAbility.STRANGE_SHIELDS), "who owns his bits");
        check(HeroAbilityAnims.castMain(HeroAbility.STRANGE_PORTAL) == null, "no cooldown tells his portals");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void heFliesOnTheCloakAndHisTakeOffEndsOnItsHover(GameTestHelper helper) {
        FlightProfile profile = FlightProfile.of(Power.DOCTOR_STRANGE, false);
        check(profile != null, "the Cloak has a flight profile");
        float[] hover = {StrangeGestures.HOVER_M[0], StrangeGestures.HOVER_M[1], StrangeGestures.HOVER_M[2],
                StrangeGestures.HOVER_O[0], StrangeGestures.HOVER_O[1], StrangeGestures.HOVER_O[2]};
        for (int i = 0; i < 6; i++) {
            assertNear(hover[i], profile.anchor(FlightProfile.HOVER, i), 0.5, "the hover pose " + i);
        }
        assertNear(profile.pitchTarget(1.0F, 0.0F), profile.p1, 1.0E-4, "a graceful cruise");
        check(profile.p2 < 50.0F, "he stays upright even at a sprint");
        check(HeroClients.of(Power.DOCTOR_STRANGE).takeoffGesture() == GestureId.TAKEOFF_STRANGE, "his take-off");
        check(RunCharacter.of(Power.DOCTOR_STRANGE, false) == RunCharacter.DOCTOR_STRANGE, "his walk");
        check(AnimCatalog.FLIGHT_STYLES.contains("doctor_strange") && AnimCatalog.RUN_CHARACTERS.contains("doctor_strange"),
                "the Creator offers his flight and walk");
        check(CustomAnimRoutes.flightPower("doctor_strange") == Power.DOCTOR_STRANGE, "the style flies as he does");
        helper.succeed();
    }

    private StrangeAnimationTests() {
    }
}
