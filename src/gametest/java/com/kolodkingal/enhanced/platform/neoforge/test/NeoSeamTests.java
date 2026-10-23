package com.kolodkingal.enhanced.platform.neoforge.test;

import com.kolodkingal.enhanced.platform.CommonTags;
import com.kolodkingal.enhanced.platform.ext.BlockEntityExt;
import com.kolodkingal.enhanced.platform.ext.EntityExt;
import com.kolodkingal.enhanced.platform.ext.ItemExt;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import java.lang.reflect.Method;

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

    /**
     * Each method of {@link ItemExt}, {@link EntityExt} and {@link BlockEntityExt} is the NeoForge extension method of
     * the same name, parameters and return type, so an item's (entity's, block entity's) implementation overrides it
     * and NeoForge calls it.
     */
    @EnhancedTest(template = "arena", batch = "platform_seams")
    public static void theExtensionHooksAreNeoForgesMethods(GameTestHelper helper) {
        int checked = matches(ItemExt.class, IItemExtension.class) + matches(EntityExt.class, IEntityExtension.class)
                + matches(BlockEntityExt.class, IBlockEntityExtension.class);
        if (checked != 13) {
            throw new GameTestAssertException("13 hooks, checked " + checked);
        }
        helper.succeed();
    }

    private static int matches(Class<?> holder, Class<?> neo) {
        int checked = 0;
        for (Class<?> hook : holder.getDeclaredClasses()) {
            for (Method m : hook.getDeclaredMethods()) {
                Method theirs;
                try {
                    theirs = neo.getMethod(m.getName(), m.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    throw new GameTestAssertException(hook.getSimpleName() + "." + m.getName() + " is not in "
                            + neo.getSimpleName());
                }
                if (!theirs.getGenericReturnType().equals(m.getGenericReturnType())) {
                    throw new GameTestAssertException(hook.getSimpleName() + "." + m.getName() + " returns "
                            + m.getGenericReturnType() + ", NeoForge's " + theirs.getGenericReturnType());
                }
                checked++;
            }
        }
        return checked;
    }
}
