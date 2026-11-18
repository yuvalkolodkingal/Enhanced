package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.client.hulk.HulkModel;
import com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.gametest.framework.GameTestHelper;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Hulk's body (3.17): its parts are baked where a player's are. The model moves them to the Hulk's places after the
 * vanilla animation, every frame; from Minecraft 1.21.2 that animation starts from the baked places, so parts baked at
 * the Hulk's own places were moved twice (his arms hung in the air beside him until 3.17).
 */
public final class HulkModelTests {
    private static final String BATCH = "hulk_model_pure";

    private HulkModelTests() {
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theHulksPartsAreBakedWhereAPlayersAre(GameTestHelper helper) {
        for (float grow : new float[] {0.0F, 0.35F}) {
            ModelPart root = HulkModel.createLayer(grow).bakeRoot();
            for (String side : new String[] {"right", "left"}) {
                float sign = side.equals("right") ? -1.0F : 1.0F;
                for (String name : new String[] {"_arm", "_sleeve"}) {
                    ModelPart part = root.getChild(side + name);
                    assertNear(part.x, sign * 5.0F, 1.0E-4, side + name + " is baked at a player's shoulder, 5 out");
                    assertNear(part.y, 2.0F, 1.0E-4, side + name + " at a player's shoulder height");
                    assertNear(part.z, 0.0F, 1.0E-4, side + name + " not forward of it");
                }
                for (String name : new String[] {"_leg", "_pants"}) {
                    ModelPart part = root.getChild(side + name);
                    assertNear(part.x, sign * 1.9F, 1.0E-4, side + name + " is baked at a player's hip");
                    assertNear(part.y, 12.0F, 1.0E-4, side + name + " at a player's hip height");
                }
            }
            for (String name : new String[] {"head", "hat", "body", "jacket"}) {
                ModelPart part = root.getChild(name);
                check(part.x == 0.0F && part.y == 0.0F && part.z == 0.0F, name + " is baked at the model's middle");
            }
        }
        check(HulkModel.SHOULDER_X > 5.0F, "the model itself moves the shoulders out, every frame");
        check(HulkPlayerRenderer.FIRST_PERSON_ARM > 0.4F && HulkPlayerRenderer.FIRST_PERSON_ARM < 0.8F,
                "his arm in the first-person view is drawn smaller than his body's");
        helper.succeed();
    }
}
