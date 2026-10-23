package com.kolodkingal.enhanced.platform.neoforge.test;

import com.kolodkingal.enhanced.platform.CommonTags;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.Tags;

/** The port P0 seams compared with the NeoForge API they replace (NeoForge only). */
public final class NeoSeamTests {
    private NeoSeamTests() {
    }

    /** Each of {@link CommonTags} is the very key NeoForge's {@code Tags} holds. */
    @EnhancedTest(template = "arena", batch = "platform_seams")
    public static void theConventionTagsAreNeoForgesKeys(GameTestHelper helper) {
        same(CommonTags.Blocks.GLASS_BLOCKS, Tags.Blocks.GLASS_BLOCKS);
        same(CommonTags.Blocks.GLASS_PANES, Tags.Blocks.GLASS_PANES);
        same(CommonTags.Blocks.ORES, Tags.Blocks.ORES);
        same(CommonTags.Blocks.ORES_IN_GROUND_DEEPSLATE, Tags.Blocks.ORES_IN_GROUND_DEEPSLATE);
        same(CommonTags.Blocks.ORES_IN_GROUND_NETHERRACK, Tags.Blocks.ORES_IN_GROUND_NETHERRACK);
        same(CommonTags.Blocks.ORES_IN_GROUND_STONE, Tags.Blocks.ORES_IN_GROUND_STONE);
        helper.succeed();
    }

    private static void same(Object ours, Object neo) {
        if (ours != neo) {
            throw new GameTestAssertException(ours + " is not NeoForge's " + neo);
        }
    }
}
