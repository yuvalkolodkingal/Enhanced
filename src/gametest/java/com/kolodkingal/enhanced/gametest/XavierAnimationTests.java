package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.XavierGestures;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The animation of Professor X (3.3): every gesture has its keys, the ones that hand over to a stance end on its pose,
 * every ability, toggle and state is routed, and he comes after the heroes of 3.0. Pure data (no client classes).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class XavierAnimationTests {
    private static final String BATCH = "xavier_animation";

    @GameTest(template = ARENA, batch = BATCH)
    public static void professorXComesAfterTheHeroesOf3(GameTestHelper helper) {
        check(Power.PROFESSOR_X.ordinal() > Power.APOCALYPSE.ordinal(), "Professor X is appended after Apocalypse");
        check(!Power.PROFESSOR_X.canFly() && Power.PROFESSOR_X.flightAbility() == -1, "he does not fly");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hisGesturesAreKeyedAndHandOverWithoutAPop(GameTestHelper helper) {
        GestureId[] mine = {GestureId.XAVIER_BLAST, GestureId.XAVIER_FREEZE, GestureId.XAVIER_LINK, GestureId.XAVIER_CEREBRO,
                GestureId.XAVIER_THOUGHTS_ON, GestureId.XAVIER_THOUGHTS_OFF};
        for (GestureId id : mine) {
            Gestures.Def def = Gestures.get(id);
            check(def != null && def.problems().isEmpty(), id + " is keyed and well formed");
        }
        // The trance and Cerebro: the last keys are the poses of the stances.
        Gestures.Def link = Gestures.get(GestureId.XAVIER_LINK);
        Gestures.ArmKey m = link.m.get(link.m.size() - 1);
        float[] temple = XavierGestures.TEMPLE_M;
        assertNear(m.x, temple[0], 0.5, "the link ends with the fingers at the temple (x)");
        assertNear(m.y, temple[1], 0.5, "(y)");
        assertNear(m.z, temple[2], 0.5, "(z)");
        Gestures.Def cerebro = Gestures.get(GestureId.XAVIER_CEREBRO);
        Gestures.ArmKey o = cerebro.o.get(cerebro.o.size() - 1);
        float[] other = XavierGestures.TEMPLE_O;
        assertNear(o.x, other[0], 0.5, "Cerebro ends with both hands at the helmet (x)");
        assertNear(o.y, other[1], 0.5, "(y)");
        assertNear(o.z, other[2], 0.5, "(z)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyAbilityToggleAndStateOfHisIsRouted(GameTestHelper helper) {
        for (int ability = 1; ability <= 5; ability++) {
            String how = GestureRoutes.acting(Power.PROFESSOR_X, ability);
            check(!how.startsWith("none"), "ability " + ability + " is acted: " + how);
        }
        check(GestureRoutes.toggle(Power.PROFESSOR_X, Toggle.HEAR_THOUGHTS, true) == GestureId.XAVIER_THOUGHTS_ON
                && GestureRoutes.toggle(Power.PROFESSOR_X, Toggle.HEAR_THOUGHTS, false) == GestureId.XAVIER_THOUGHTS_OFF, "G is acted");
        check(GestureRoutes.toggle(Power.BILLY, Toggle.HEAR_THOUGHTS, true) == null, "only for him");
        check(GestureRoutes.reason(Live.MIND_LINKED) == null && GestureRoutes.reason(Live.CEREBRO) == null, "the trance and Cerebro have gestures");
        check(GestureRoutes.reason(Live.MIND_CONTROLLED) != null && GestureRoutes.reason(Live.XAVIER_FOCUS) != null, "the rest say why not");
        helper.succeed();
    }

    private XavierAnimationTests() {
    }
}
