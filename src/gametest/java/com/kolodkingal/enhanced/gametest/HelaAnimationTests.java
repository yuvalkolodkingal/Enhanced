package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HelaBossPoses;
import com.kolodkingal.enhanced.client.render.HelaGestures;
import com.kolodkingal.enhanced.client.render.HelaMusicMath;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hela.HelaBrain;
import com.kolodkingal.enhanced.hela.HelaState;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * How Hela is acted (3.5), with no client class: which gesture stands for each of her keys, the gestures' keys, the stances
 * the form's and the grasp's gestures hand over to, what her own press starts, the boss's poses (one for every state,
 * meeting the server's moments), her walk, and the layers of her music.
 */
public final class HelaAnimationTests {
    private static final String BATCH = "hela_animation";

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyKeyOfHersIsActed(GameTestHelper helper) {
        Power p = Power.HELA;
        check(GestureRoutes.acting(p, 1).startsWith("none: ") && GestureRoutes.acting(p, 2).startsWith("none: "), "R and G by their payloads");
        check(GestureRoutes.acting(p, 3).equals("toggle:HELA_FORM"), "V by the form's toggle");
        check(GestureRoutes.toggle(p, Toggle.HELA_FORM, true) == GestureId.HELA_FORM_ON
                && GestureRoutes.toggle(p, Toggle.HELA_FORM, false) == GestureId.HELA_FORM_OFF, "on and off");
        check(GestureRoutes.acting(p, 4).equals("live:HELA_SUMMONING") && GestureRoutes.live(p, Live.HELA_SUMMONING, false) == GestureId.HELA_RAISE,
                "B by her dead rising");
        check(GestureRoutes.acting(p, 5).equals("live:HELA_GRASPING") && GestureRoutes.live(p, Live.HELA_GRASPING, false) == GestureId.HELA_GRASP,
                "N by the grasp");
        check(GestureRoutes.reason(Live.HELA_GRASPING) == null && GestureRoutes.reason(Live.HELA_SUMMONING) == null, "no pending reason left");
        HeroClient hero = HeroClients.of(p);
        for (GestureId id : List.of(GestureId.HELA_THROW, GestureId.HELA_RAIN, GestureId.HELA_WALL, GestureId.HELA_BURST, GestureId.HELA_DASH,
                GestureId.HELA_FENRIS)) {
            check(hero != null && hero.startedElsewhere(id), id + " is started by its payload");
        }
        for (GestureId id : GestureId.values()) {
            if (id.name().startsWith("HELA_")) {
                check(Gestures.get(id) != null && Gestures.get(id).problems().isEmpty(), id + " has well-formed keys");
            }
        }
        check(Stance.HELA_GRASP.aimed && !Stance.HELA_BLADES.aimed, "the grasp holds the arm; the blades are ambient");
        check(RunCharacter.of(p, false) == RunCharacter.HELA, "her glide");
        check(HeroAbilityAnims.shown(HeroAbility.HELA_NECROSWORDS, true) == GestureId.HELA_RAIN, "the rain is the volley's variant");
        check(HeroAbilityAnims.stances(HeroAbility.HELA_GODDESS).contains(Stance.HELA_BLADES), "the form's stance");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herGesturesEndOnTheirStances(GameTestHelper helper) {
        Gestures.Def on = Gestures.get(GestureId.HELA_FORM_ON);
        Gestures.ArmKey last = on.m.get(on.m.size() - 1);
        assertNear(last.x, HelaGestures.BLADES_M[0], 0.5, "the form ends on the blades' stance");
        Gestures.Def grasp = Gestures.get(GestureId.HELA_GRASP);
        assertNear(grasp.m.get(grasp.m.size() - 1).x, HelaGestures.GRASP_M[0], 0.5, "the grasp ends on its stance");
        PowerData data = new PowerData();
        check(HelaGestures.predicted(data, 1, 100L, false) == GestureId.HELA_THROW, "R rested: the throw at once");
        check(HelaGestures.predicted(data, 1, 100L, true) == null, "Sneak + R waits for the server");
        data.startCooldown(1, 100L, 30);
        check(HelaGestures.predicted(data, 1, 110L, false) == null, "not while it recharges");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBossHasAPoseForEveryStateThatMeetsTheServer(GameTestHelper helper) {
        check(HelaBossPoses.complete(), "every state has a pose from tick 0");
        float[] tell = HelaBossPoses.at(HelaState.VOLLEY, HelaBrain.VOLLEY_THROW - 2);
        float[] thrown = HelaBossPoses.at(HelaState.VOLLEY, HelaBrain.VOLLEY_THROW + 1);
        int rx = HelaBossPoses.Ch.RX.ordinal();
        check(tell[rx] < -140 && thrown[rx] > -110, "the hands are raised for the tell and thrown at the throw");
        int alpha = HelaBossPoses.Ch.ALPHA.ordinal();
        check(HelaBossPoses.at(HelaState.ARRIVING, 0)[alpha] < 0.05F && HelaBossPoses.at(HelaState.ARRIVING, 110)[alpha] > 0.95F,
                "she forms out of smoke");
        check(HelaBossPoses.at(HelaState.DYING, HelaState.DYING.ticks)[alpha] < 0.05F, "and dissolves");
        check(HelaBossPoses.at(HelaState.CHANNEL, 20)[HelaBossPoses.Ch.DROP.ordinal()] > 5, "she kneels to channel");
        for (HelaState state : HelaState.values()) {
            float[] a = HelaBossPoses.at(state, 3.0F);
            float[] b = HelaBossPoses.at(state, 3.05F);
            for (int c = 0; c < a.length; c++) {
                check(Math.abs(a[c] - b[c]) < 8.0F, state + ": no jump in channel " + c);
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herMusicLayersFollowThePhase(GameTestHelper helper) {
        check(HelaMusicMath.layerWanted(1, 1) && !HelaMusicMath.layerWanted(2, 1) && !HelaMusicMath.layerWanted(3, 1), "phase 1: the drone");
        check(HelaMusicMath.layerWanted(2, 2) && !HelaMusicMath.layerWanted(3, 2), "phase 2: the strings and the choir");
        check(HelaMusicMath.layerWanted(3, 3), "phase 3: everything");
        assertNear(HelaMusicMath.volumeAt(10.0), 1.0, 1.0E-6, "near: all of it");
        assertNear(HelaMusicMath.volumeAt(HelaMusicMath.REACH + 1.0), 0.0, 1.0E-6, "far: none");
        helper.succeed();
    }
}
