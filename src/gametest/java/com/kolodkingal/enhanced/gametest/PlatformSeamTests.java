package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.platform.CommonTags;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.platform.test.TestHooks;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
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

    /**
     * S5-3: a new watcher's first sync carries a player's power; replayed on another player, it gives him that power.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theFirstSyncCarriesThePowerAndReplays(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(4.5, FLOOR, 4.5, Power.SCARLET_WITCH);
        ServerPlayer watcher = f.player(8.5, FLOOR, 4.5);
        ServerPlayer copy = f.player(12.5, FLOOR, 4.5);
        TestHooks hooks = TestHooks.get();
        List<Object> packets = hooks.initialSyncPackets(wanda, watcher);
        check(packets.stream().anyMatch(p -> hooks.isAttachmentSync(p, ModAttachments.POWER, wanda.getId())),
                "her power is in the first sync: " + packets);
        check(Powers.get(copy).power() == Power.NONE, "the copy has no power yet");
        packets.forEach(p -> hooks.receiveSync(p, copy));
        check(Powers.get(copy).power() == Power.SCARLET_WITCH, "the replay gives the copy her power");
        helper.succeed();
    }

    /** CommonTags: the convention tags hold what NeoForge's did (glass, panes, the ores and their ground). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theConventionTagsHoldTheirBlocks(GameTestHelper helper) {
        check(Blocks.GLASS.defaultBlockState().is(CommonTags.Blocks.GLASS_BLOCKS), "glass");
        check(Blocks.RED_STAINED_GLASS.defaultBlockState().is(CommonTags.Blocks.GLASS_BLOCKS), "stained glass");
        check(Blocks.GLASS_PANE.defaultBlockState().is(CommonTags.Blocks.GLASS_PANES), "a glass pane");
        check(!Blocks.STONE.defaultBlockState().is(CommonTags.Blocks.GLASS_BLOCKS), "stone is no glass");
        check(Blocks.IRON_ORE.defaultBlockState().is(CommonTags.Blocks.ORES), "iron ore is an ore");
        check(!Blocks.STONE.defaultBlockState().is(CommonTags.Blocks.ORES), "stone is no ore");
        check(Blocks.IRON_ORE.defaultBlockState().is(CommonTags.Blocks.ORES_IN_GROUND_STONE), "iron ore, in stone");
        check(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState().is(CommonTags.Blocks.ORES_IN_GROUND_DEEPSLATE),
                "deepslate iron ore, in deepslate");
        check(Blocks.NETHER_GOLD_ORE.defaultBlockState().is(CommonTags.Blocks.ORES_IN_GROUND_NETHERRACK),
                "nether gold ore, in netherrack");
        helper.succeed();
    }
}
