package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.network.MusicManifestPayload;
import com.kolodkingal.enhanced.platform.CommonTags;
import com.kolodkingal.enhanced.platform.ItemAccess;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.platform.test.TestHooks;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    /**
     * S5-2: a pig with water over its eyes can drown in it; one on the dry floor cannot; a zombie under water cannot
     * (it breathes there: on NeoForge water's drowning rule asks {@code canBreatheUnderwater}).
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void waterAtTheEyesDrowns(GameTestHelper helper) {
        for (int x = 3; x <= 7; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 3; z <= 5; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
        Pig wet = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new BlockPos(4, 2, 4));
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new BlockPos(6, 2, 4));
        Pig dry = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new BlockPos(14, 2, 14));
        var hooks = Platform.get().hooks();
        helper.succeedWhen(() -> {
            check(wet.isEyeInFluid(FluidTags.WATER) && zombie.isEyeInFluid(FluidTags.WATER), "both under water");
            check(hooks.drownsInFluidAtEyes(wet), "the pig, water over its eyes");
            check(!hooks.drownsInFluidAtEyes(zombie), "the zombie breathes under water");
            check(!hooks.drownsInFluidAtEyes(dry), "air at the dry pig's eyes");
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

    /**
     * Net.hasChannel: a player on the tests' fake connection (whose client "has" every channel) has the music channel;
     * a player with no connection has none.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aClientHasTheChannelsItAccepts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer connected = f.player(4.5, FLOOR, 4.5);
        check(Net.hasChannel(connected, MusicManifestPayload.TYPE), "the fake client has the music channel");
        ServerPlayer unconnected = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "unconnected"), ClientInformation.createDefault());
        check(!Net.hasChannel(unconnected, MusicManifestPayload.TYPE), "no connection, no channel");
        helper.succeed();
    }

    /**
     * SR-L1-1: a chest's slots take what fits and give back the rest; a simulated insert changes nothing.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aStoreTakesWhatFits(GameTestHelper helper) {
        BlockPos at = new BlockPos(4, 2, 4);
        helper.setBlock(at, Blocks.CHEST);
        BlockPos abs = helper.absolutePos(at);
        ItemAccess.Slots slots = Platform.get().itemAccess().blockSlots(helper.getLevel(), abs);
        check(slots != null && slots.getSlots() == 27, "a chest has 27 slots");
        ItemStack dirt = new ItemStack(Items.DIRT, 5);
        check(slots.insertItem(0, dirt, true).isEmpty() && slots.getStackInSlot(0).isEmpty(),
                "a simulated insert fits and changes nothing");
        check(slots.insertItem(0, dirt, false).isEmpty() && dirt.getCount() == 5, "the insert fits; the stack is kept");
        check(((ChestBlockEntity) helper.getLevel().getBlockEntity(abs)).getItem(0).getCount() == 5, "5 dirt inside");
        ItemStack left = slots.insertItem(0, new ItemStack(Items.DIRT, 64), false);
        check(left.getCount() == 5 && slots.getStackInSlot(0).getCount() == 64, "what did not fit comes back: " + left);
        check(slots.insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1,
                "a full slot of dirt takes no stone");
        helper.succeed();
    }

    /** SR-L1-2: the plain block-break event reaches the mod's listeners, and their cancellation is the answer. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBreakEventIsPostedAndCancelled(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 4.5);
        BlockPos at = helper.absolutePos(new BlockPos(6, 2, 6));
        helper.setBlock(new BlockPos(6, 2, 6), Blocks.STONE);
        List<BlockPos> seen = new ArrayList<>();
        boolean cancelled;
        try (EventHub.Registration r = EventHub.of(BlockEvent.BreakEvent.class).register(Priority.LOWEST, e -> {
            if (e.getPlayer() == player) {
                seen.add(e.getPos());
                e.setCanceled(true);
            }
        })) {
            cancelled = Platform.get().hooks().blockBreakEventCancelled(helper.getLevel(), at,
                    Blocks.STONE.defaultBlockState(), player);
        }
        check(seen.equals(List.of(at)), "the listener saw the break at " + at + ": " + seen);
        check(cancelled, "its cancellation is the answer");
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 2, 6));
        helper.succeed();
    }
}
