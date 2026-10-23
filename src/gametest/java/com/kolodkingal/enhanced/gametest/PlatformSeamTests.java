package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The platform seams the lanes asked for during port P0 (SCRATCH/port/p0/notes/seams.md): each answers on NeoForge
 * as the NeoForge call it replaces did.
 */
public final class PlatformSeamTests {
    static final String BATCH = "platform_seams";

    private PlatformSeamTests() {
    }

    /** S5-1: vanilla's shield and the Vibranium Shield are shields; a sword and nothing are not. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void shieldsAreShields(GameTestHelper helper) {
        var hooks = Platform.get().hooks();
        check(hooks.isShield(new ItemStack(Items.SHIELD)), "vanilla's shield");
        check(hooks.isShield(new ItemStack(ModItems.VIBRANIUM_SHIELD.get())), "the Vibranium Shield");
        check(!hooks.isShield(new ItemStack(Items.IRON_SWORD)), "a sword is no shield");
        check(!hooks.isShield(ItemStack.EMPTY), "nothing is no shield");
        helper.succeed();
    }

    /** S5-2: a zombie with water over his eyes can drown in it; one on the dry floor cannot. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void waterAtTheEyesDrowns(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 3; z <= 5; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
        Zombie wet = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        Zombie dry = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new BlockPos(14, 2, 14));
        var hooks = Platform.get().hooks();
        helper.succeedWhen(() -> {
            check(hooks.drownsInFluidAtEyes(wet), "water over his eyes");
            check(!hooks.drownsInFluidAtEyes(dry), "air at his eyes");
        });
    }
}
