package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.CyclopsGlasses;
import com.kolodkingal.enhanced.client.render.CyclopsHeroMath;
import com.kolodkingal.enhanced.client.render.FlightProfile;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.client.render.StancePoses;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Cyclops's animation (3.3), the pure parts: every ability is acted, the cooldown of N tells the glasses coming off from going
 * on, the gestures are well formed and reach the visor exactly where the Optic Blast's stance holds the hand, his stances are
 * the aimed kind, he has no flight profile, and the glow of the visor and the breathing of the giant beam move without a jump.
 */
public final class CyclopsAnimationTests {
    private static final List<GestureId> GESTURES = List.of(GestureId.CYCLOPS_RICOCHET, GestureId.CYCLOPS_CONCUSSION, GestureId.CYCLOPS_LEAP,
            GestureId.CYCLOPS_RALLY, GestureId.CYCLOPS_GLASSES_OFF, GestureId.CYCLOPS_GLASSES_ON);

    @EnhancedTest(template = ARENA, batch = "cyclops_pure")
    public static void everyAbilityIsActedAndTheGlassesTellOffFromOn(GameTestHelper helper) {
        GestureId[] expected = {GestureId.CYCLOPS_RICOCHET, GestureId.CYCLOPS_CONCUSSION, GestureId.CYCLOPS_LEAP, GestureId.CYCLOPS_RALLY};
        for (int ability = 1; ability <= 5; ability++) {
            check(GestureRoutes.acting(Power.CYCLOPS, ability).equals("cooldown"), "ability " + ability + " is acted by its cooldown");
            if (ability <= 4) {
                check(GestureRoutes.cooldown(Power.CYCLOPS, ability, 40) == expected[ability - 1], "ability " + ability + "'s gesture");
            }
        }
        for (long late = 0; late <= 3; late++) {
            check(GestureRoutes.cooldown(Power.CYCLOPS, 5, CyclopsGlasses.OFF_TICKS - late) == GestureId.CYCLOPS_GLASSES_OFF,
                    "a long cooldown of N is the glasses coming off, seen " + late + " ticks late");
            check(GestureRoutes.cooldown(Power.CYCLOPS, 5, CyclopsGlasses.ON_TICKS - late) == GestureId.CYCLOPS_GLASSES_ON,
                    "a short one is the glasses going on, seen " + late + " ticks late");
        }
        String reason = GestureRoutes.reason(Live.CYCLOPS_UNLEASHED);
        check(reason != null && reason.contains("CYCLOPS_UNLEASHED"), "the giant beam is a stance: " + reason);
        check(GestureRoutes.live(Power.CYCLOPS, Live.BEAMING, true) == null, "his beams start no arm gesture (they leave his eyes)");
        check(FlightProfile.of(Power.CYCLOPS, false) == null, "he does not fly");
        check(Stance.CYCLOPS_BLAST.aimed && Stance.CYCLOPS_UNLEASHED.aimed, "his stances hold the arms");
        helper.succeed();
    }

    /** Every gesture is well formed, starts and ends with the arms at rest, and the hand that goes to the visor goes to the stance's pose. */
    @EnhancedTest(template = ARENA, batch = "cyclops_pure")
    public static void theGesturesAreWellFormedAndReachTheVisor(GameTestHelper helper) {
        for (GestureId id : GESTURES) {
            Gestures.Def def = Gestures.get(id);
            check(def != null && def.problems().isEmpty(), id + ": " + (def == null ? "no keys" : def.problems()));
            Gestures.ArmKey first = def.m.get(0);
            Gestures.ArmKey last = def.m.get(def.m.size() - 1);
            check(first.t == 0.0F && last.t <= id.dur, id + " is keyed from 0 to its end");
            assertNear(first.x, last.x, 0.01, id + " ends where it began (x)");
            assertNear(first.z, last.z, 0.01, id + " ends where it began (z)");
        }
        for (GestureId id : List.of(GestureId.CYCLOPS_RICOCHET, GestureId.CYCLOPS_GLASSES_OFF, GestureId.CYCLOPS_GLASSES_ON)) {
            boolean reaches = Gestures.get(id).m.stream().anyMatch(k -> k.x == StancePoses.CYCLOPS_BLAST_M[0] && k.y == StancePoses.CYCLOPS_BLAST_M[1]
                    && k.z == StancePoses.CYCLOPS_BLAST_M[2]);
            check(reaches, id + " puts the hand on the visor where the Optic Blast holds it");
        }
        helper.succeed();
    }

    /** The visor's glow rises in three ticks and then never drops below 0.8; the giant beam breathes without a jump. */
    @EnhancedTest(template = ARENA, batch = "cyclops_pure")
    public static void theGlowAndTheBeamMoveSmoothly(GameTestHelper helper) {
        check(CyclopsHeroMath.visorGlow(0.0F) == 0.0F, "dark before it begins");
        float previous = 0.0F;
        for (float t = 0.05F; t <= 3.0F; t += 0.05F) {
            float g = CyclopsHeroMath.visorGlow(t);
            check(Math.abs(g - previous) < 0.05F, "no jump at " + t);
            previous = g;
        }
        for (float t = 3.0F; t < 200.0F; t += 0.37F) {
            check(CyclopsHeroMath.visorGlow(t) >= 0.8F - 1.0E-4F && CyclopsHeroMath.visorGlow(t) <= 1.0F + 1.0E-4F, "steady at " + t);
        }
        for (double along = 0.0; along < 40.0; along += 0.5) {
            for (float t = 0.0F; t < 40.0F; t += 0.05F) {
                float a = CyclopsHeroMath.unleashedWidth(1.0F, along, t);
                float b = CyclopsHeroMath.unleashedWidth(1.0F, along, t + 0.05F);
                check(Math.abs(a - b) < 0.02F, "the giant beam breathes smoothly at " + along + ", " + t);
                check(a > 0.2F && a <= 1.0F, "and keeps its width");
            }
        }
        helper.succeed();
    }
}
