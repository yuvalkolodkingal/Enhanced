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
 * Vision's VisionQuest look (3.12.1): it is one of his looks, with its own skin and his cape; its eyes glow only while a
 * power of his is in use; and Vision, White Vision and Source Code are as they were.
 */
public final class VisionQuestLookTests {

    @EnhancedTest(template = ARENA)
    public static void theVisionQuestLookIsItsOwnSkin(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5);
        Powers.awaken(vision, Power.VISION, "vision_visionquest");
        PowerData data = Powers.get(vision);
        check(Powers.is(vision, Power.VISION) && "vision_visionquest".equals(data.variant()), "awakened in the VisionQuest look");
        Variant look = Variants.current(data);
        check(look.id().equals("vision_visionquest") && look.power() == Power.VISION && !look.slim(), "the look itself, wide arms");
        check(Enhanced.id("textures/entity/suit/vision_visionquest.png").equals(look.skin()), "its skin: " + look.skin());
        check(Enhanced.id("textures/entity/suit/vision_cape.png").equals(look.cape()), "Vision's cape: " + look.cape());
        check(VisionQuestLookTests.class.getResource("/assets/enhanced/textures/entity/suit/vision_visionquest.png") != null
                && VisionQuestLookTests.class.getResource("/assets/enhanced/textures/entity/suit/vision_visionquest_eyes.png") != null,
                "the skin and its eyes are in the jar");
        check(Variants.get(Power.VISION, "vision").skin().equals(Enhanced.id("textures/entity/suit/vision.png"))
                && Variants.get(Power.VISION, "white_vision").skin().equals(Enhanced.id("textures/entity/suit/white_vision.png"))
                && Variants.get(Power.VISION, "source_code").skin().equals(Enhanced.id("textures/entity/suit/vision_source_code.png")),
                "the other looks are as they were");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void itsEyesGlowOnlyWhileAPowerIsInUse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5);
        Powers.awaken(vision, Power.VISION, "vision_visionquest");
        PowerData data = Powers.get(vision);
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
        check(!VisionQuest.eyesLit(data), "only in the VisionQuest look");
        helper.succeed();
    }
}
