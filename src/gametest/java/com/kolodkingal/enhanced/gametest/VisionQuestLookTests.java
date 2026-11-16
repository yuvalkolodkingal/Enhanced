package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.power.VisionQuest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The VisionQuest suit (3.16.1): it is the Source Code's alone, the look earned in Vision's mind; the VisionQuest look of
 * 3.12.1 is gone (a player saved in it is plain Vision); its eyes glow only while a power of his is in use.
 */
public final class VisionQuestLookTests {

    @EnhancedTest(template = ARENA)
    public static void theVisionQuestSuitIsTheSourceCodesAlone(GameTestHelper helper) {
        check(Variants.of(Power.VISION).stream().noneMatch(v -> v.id().equals("vision_visionquest")),
                "no VisionQuest look of its own");
        Variant source = Variants.get(Power.VISION, "source_code");
        check(source.id().equals("source_code")
                && Enhanced.id("textures/entity/suit/vision_visionquest.png").equals(source.skin())
                && Enhanced.id("textures/entity/suit/vision_cape.png").equals(source.cape()),
                "the Source Code wears the suit and Vision's cape: " + source.skin());
        check(VisionQuestLookTests.class.getResource("/assets/enhanced/textures/entity/suit/vision_visionquest.png") != null
                && VisionQuestLookTests.class.getResource("/assets/enhanced/textures/entity/suit/vision_visionquest_eyes.png") != null,
                "the skin and its eyes are in the jar");
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5);
        Powers.awaken(vision, Power.VISION, "vision_visionquest");
        Variant look = Variants.current(Powers.get(vision));
        check(!look.id().equals("vision_visionquest") && !VisionQuest.suit(Powers.get(vision).variant()),
                "a player asking for the old look is plain Vision: " + look.id());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void itsEyesGlowOnlyWhileAPowerIsInUse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5);
        // (the Source Code is earned in Vision's mind first)
        com.kolodkingal.enhanced.power.EarnedLooks.earn(vision, Variants.get(Power.VISION, "source_code"));
        Powers.awaken(vision, Power.VISION, "source_code");
        PowerData data = Powers.get(vision);
        check("source_code".equals(data.variant()), "in the Source Code: " + data.variant());
        check(!VisionQuest.eyesLit(data), "at rest: plain yellow eyes");
        data.setVisionPhased(true);
        check(VisionQuest.eyesLit(data), "phasing: they glow");
        data.setVisionPhased(false);
        data.setDense(true);
        check(VisionQuest.eyesLit(data), "dense: they glow");
        data.setDense(false);
        check(!VisionQuest.eyesLit(data), "and stop");
        Powers.setVariant(vision, Variants.get(Power.VISION, "vision"));
        data = Powers.get(vision);
        data.setVisionPhased(true);
        check(!VisionQuest.eyesLit(data), "only in the Source Code");
        helper.succeed();
    }
}
