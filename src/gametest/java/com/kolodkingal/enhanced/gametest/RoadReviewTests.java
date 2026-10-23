package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadCells;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadBuild;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadRules;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.boat.IceTrack;
import com.kolodkingal.enhanced.road.boat.IceTrackGenerator;
import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import com.kolodkingal.enhanced.road.copy.StructureSupply;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The fixes after the independent review of the Witches' Road (R1-R18): the Gene cannot be thrown away, nobody travels into the Road
 * from outside, a refused way out keeps the trial, a trial that throws is given up, a chunk that cannot be cleared does not stop the
 * rest, the Road's world border, entities removed late, the put-aside backpack saved with the player, courses that cannot be drawn
 * for a seed, and the small things.
 */
public final class RoadReviewTests {
    static final String BATCH = "road_review";

    private static int genes(ServerPlayer player) {
        int n = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                n += stack.getCount();
            }
        }
        return n;
    }

    // ---- R1: the Gene is not destroyed by trying to drop it ----

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theGeneIsGivenBackWhenHeTriesToThrowItAwayWithTheKeyOrByClicking(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 3));
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 5));
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    ServerLevel road = RoadTestSupport.road(helper);
                    AABB around = new AABB(player.blockPosition()).inflate(6.0);
                    // Q: one of the stack; Ctrl+Q: the whole stack.
                    player.drop(false);
                    check(genes(player) == 3, "Q: the Gene is still in his inventory: " + genes(player));
                    player.drop(true);
                    check(genes(player) == 3 && player.getMainHandItem().is(ModItems.SCARLET_WITCH_GENE.get()) && player.getMainHandItem().getCount() == 3,
                            "Ctrl+Q: all three, in the same hand: " + player.getMainHandItem());
                    // A click outside the inventory screen, and a throw click on its slot (the hotbar slot 0 is slot 36 of the player's menu).
                    player.inventoryMenu.clicked(36, 1, ClickType.THROW, player);
                    check(genes(player) == 3, "a throw click: still three");
                    player.inventoryMenu.clicked(36, 0, ClickType.THROW, player);
                    check(genes(player) == 3, "and the other throw click: still three");
                    check(road.getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "nothing lies on the floor of the Road");
                    // Anything else can be thrown as ever.
                    player.getInventory().selected = 1;
                    player.drop(false);
                    check(player.getInventory().getItem(1).getCount() == 4 && !road.getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "a diamond can be thrown");
                    player.getInventory().selected = 0;
                })
                .thenSucceed();
    }

    // ---- R7 and R8: nobody travels into the Road, a refused way out keeps the trial ----

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void noOneOutsideTheTrialsTravelsIntoTheRoadButOperatorsAndTheRoadItself(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadTestSupport.road(helper);
        ServerPlayer player = f.player(10.5, TestKit.FLOOR, 10.5);
        Vec3 into = new Vec3(5000.5, 100.0, 5000.5);
        check(!HexManager.onTeleport(road, player, into), "a mod's home, a saved place, a /tpa: refused");
        check(!RoadDimension.in(player), "he is still where he was");
        // The way the Road's event is fired by a travel into the dimension.
        EntityTravelToDimensionEvent event = new EntityTravelToDimensionEvent(player, RoadDimension.KEY);
        NeoForge.EVENT_BUS.post(event);
        check(event.isCanceled(), "so is a change of dimension into it");
        // An operator and the Road's own moves are free.
        ServerPlayer op = f.player(14.5, TestKit.FLOOR, 14.5);
        server.getPlayerList().getOps().add(new net.minecraft.server.players.ServerOpListEntry(op.getGameProfile(), 2, false));
        f.onClose(() -> server.getPlayerList().getOps().remove(op.getGameProfile()));
        check(HexManager.onTeleport(road, op, into), "an operator's own teleport is free");
        RoadTrials.moving(player, () -> check(HexManager.onTeleport(road, player, into), "the Road's own move is free"));
        check(!HexManager.onTeleport(road, player, into), "and it was only for that move");
        helper.succeed();
    }

    /** Another mod forbids the way out of the Road for as long as it is registered. */
    private static Consumer<EntityTravelToDimensionEvent> forbidOut(ServerPlayer player, boolean[] active) {
        Consumer<EntityTravelToDimensionEvent> listener = event -> {
            if (active[0] && event.getEntity() == player && !event.getDimension().equals(RoadDimension.KEY)) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, EntityTravelToDimensionEvent.class, listener);
        return listener;
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void aWayOutThatIsRefusedKeepsTheTrialAndNothingIsTakenDown(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        Vec3 home = player.position();
        boolean[] active = {true};
        Consumer<EntityTravelToDimensionEvent> listener = forbidOut(player, active);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(listener));
        check(RoadTrials.enter(player), "he walks the Road");
        RoadTrial[] trial = new RoadTrial[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    trial[0] = RoadTestSupport.trial(player);
                    check(!RoadTrials.leave(player, RoadChallenge.End.LEFT), "the way out is refused");
                    check(RoadDimension.in(player) && RoadTrials.trialOf(player.getUUID()) == trial[0] && trial[0].phase() == RoadTrial.Phase.ACTIVE,
                            "he is still in his trial, and it stands");
                    check(RoadSave.get(server).progress(player.getUUID()).inside() && RoadSave.get(server).progress(player.getUUID()).entry() != null,
                            "his progress still says he is inside, and where he came from");
                    check(!RoadSweeper.isClearing(trial[0].cell()), "his cell is not being cleared");
                    check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "(and damage is still turned away: he is in the trial)");
                    active[0] = false;
                    check(RoadTrials.leave(player, RoadChallenge.End.LEFT), "when the way is open again he leaves");
                    check(!RoadDimension.in(player) && player.position().distanceTo(home) < 0.01 && genes(player) == 1, "back where he came from, with the Gene");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void anArrivalThatIsRefusedLeavesHimAsHeWasWithAMessage(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.setGameMode(GameType.ADVENTURE);
        Consumer<EntityTravelToDimensionEvent> listener = event -> {
            if (event.getEntity() == player && event.getDimension().equals(RoadDimension.KEY)) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, EntityTravelToDimensionEvent.class, listener);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(listener));
        TestKit.sentPackets(player);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(() -> check(!RoadTrials.isInside(player), "the Road gave up"))
                .thenExecute(() -> {
                    check(!RoadDimension.in(player) && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "he is where he was, in the game mode he had");
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    check(!progress.inside() && progress.entry() == null, "his progress says he never went in");
                    RoadSweeper.drain(server);
                    check(RoadSave.get(server).cellCount() == 0 || RoadSave.get(server).cells().stream().noneMatch(c -> c.owner().equals(player.getUUID())), "the trial is taken down");
                    check(TestKit.sentPackets(player).stream().anyMatch(p -> p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && chat.overlay()
                            && chat.content().getString().contains("could not open")), "and he was told");
                })
                .thenSucceed();
    }

    // ---- R3, R9: a trial that throws is given up, a course that cannot be drawn is drawn with another seed ----

    /** A small trial that throws in its tick (after a few), or cannot be drawn for the seeds before the one it accepts. */
    private static final class Flaky extends RoadTestSupport.ShortParkour {
        final AtomicInteger builds = new AtomicInteger();
        volatile long accept = Long.MIN_VALUE;
        volatile boolean throwInTick;

        Flaky(String id) {
            super(id);
        }

        @Override
        public RoadLayout build(RoadBuild build) {
            builds.incrementAndGet();
            if (accept != Long.MIN_VALUE && build.seed() != accept) {
                throw new IllegalStateException("boxed itself in (a test)");
            }
            return super.build(build);
        }

        @Override
        public void tick(RoadTrial trial, ServerPlayer player) {
            if (throwInTick) {
                throw new IllegalStateException("a trial that is broken (a test)");
            }
        }
    }

    @EnhancedTest(template = ARENA, batch = "road_review_swap", timeoutTicks = 500)
    public static void aTrialThatThrowsIsGivenUpAndTheServerGoesOn(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        Flaky flaky = new Flaky("flaky_tick");
        f.onClose(RoadChallenges.swap(List.of(flaky)));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        Vec3 home = player.position();
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> flaky.throwInTick = true)
                .thenExecuteAfter(3, () -> {
                    check(!RoadDimension.in(player) && !RoadTrials.isInside(player), "the Road gave the trial up and sent him back");
                    check(player.position().distanceTo(home) < 0.01 && genes(player) == 1, "to where he came from, with his Gene");
                    check(!RoadSave.get(server).progress(player.getUUID()).inside(), "and his save no longer says he is inside (no crash loop at the next start)");
                    RoadSweeper.drain(server);
                    check(RoadSave.get(server).cellCount() == 0 || RoadSave.get(server).cells().stream().noneMatch(c -> c.owner().equals(player.getUUID())), "the cell is taken down");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = "road_review_swap2", timeoutTicks = 500)
    public static void aCourseThatCannotBeDrawnForASeedIsDrawnWithTheNextOneTheSameEveryTime(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        Flaky flaky = new Flaky("flaky_seed");
        f.onClose(RoadChallenges.swap(List.of(flaky)));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        // Only the third seed of this player is accepted.
        flaky.accept = RoadTrials.seed(player.getUUID(), "flaky_seed", 2 * 7919L);
        check(RoadTrials.enter(player), "the Road draws it with another seed");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    check(RoadTestSupport.trial(player).seed() == flaky.accept && flaky.builds.get() == 3, "after three tries: " + flaky.builds.get());
                    check(RoadTrials.leave(player, RoadChallenge.End.LEFT), "he leaves");
                    flaky.builds.set(0);
                    check(RoadTrials.enter(player), "and comes again");
                })
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    check(RoadTestSupport.trial(player).seed() == flaky.accept, "the same course");
                    // No seed works at all: the trial is not drawn, and he is told.
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    flaky.accept = 1234L;
                    flaky.builds.set(0);
                    check(!RoadTrials.enter(player), "if no seed works, the door says so");
                    check(flaky.builds.get() == 8, "after eight tries: " + flaky.builds.get());
                    check(!RoadTrials.isInside(player), "and he is not taken in");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = "road_review_swap3", timeoutTicks = 600)
    public static void aGateThatCannotDrawTheNextTrialDoesNotTryEveryTick(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        RoadChallenge first = new RoadTestSupport.ShortParkour("gate_first");
        Flaky second = new Flaky("gate_second");
        second.accept = 99L;
        f.onClose(RoadChallenges.swap(List.of(first, second)));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        RoadTrial[] trial = new RoadTrial[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    trial[0] = RoadTestSupport.trial(player);
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial[0].layout().finish()));
                })
                .thenWaitUntil(() -> check(trial[0].phase() == RoadTrial.Phase.WON, "won"))
                .thenExecute(() -> RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial[0].layout().gate().zone())))
                .thenIdle(40)
                .thenExecute(() -> {
                    check(second.builds.get() >= 8 && second.builds.get() <= 16, "he stood in the gate for two seconds: the next trial was tried once or twice, not 40 times: " + second.builds.get());
                    check(RoadTrials.trialOf(player.getUUID()) == trial[0] && RoadDimension.in(player), "he is still on the finish platform");
                })
                .thenSucceed();
    }

    // ---- R4, R5: the sweeper ----

    private static int cellWithBlocks(ServerLevel road, MinecraftServer server, UUID owner, int chunksWide) {
        RoadSave save = RoadSave.get(server);
        int cell = save.allocate(owner, "parkour", 64);
        RoadCanvas canvas = new RoadCanvas(cell, road.getMinBuildHeight(), road.getMaxBuildHeight());
        for (int x = 0; x < chunksWide; x++) {
            canvas.set(x * 16 + 3, 2, 3, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        canvas.seal();
        save.plan(cell, canvas.chunks(), canvas.minY(), canvas.maxY());
        while (!canvas.placeSome(road, 1000)) {
            // placing
        }
        return cell;
    }

    @EnhancedTest(template = ARENA, batch = "road_review_sweep", timeoutTicks = 300)
    public static void aChunkThatCannotBeClearedDoesNotStopTheRestAndIsTriedAgain(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadTestSupport.road(helper);
        RoadSave save = RoadSave.get(server);
        UUID owner = UUID.randomUUID();
        int cell = cellWithBlocks(road, server, owner, 4);
        long[] chunks = save.cell(cell).chunks();
        check(chunks.length == 4, "four chunks");
        f.onClose(() -> {
            RoadSweeper.injectFault(null);
            RoadSweeper.queue(server, cell, false);
            RoadSweeper.drain(server);
        });
        // The second chunk throws the first time.
        AtomicInteger thrown = new AtomicInteger();
        RoadSweeper.injectFault(key -> key == chunks[1] && thrown.getAndIncrement() < 1);
        RoadSweeper.queue(server, cell, false);
        RoadSweeper.drain(server);
        check(thrown.get() >= 1, "it threw");
        check(RoadTestSupport.blocksIn(road, chunks) == 0, "every chunk is clear, the failed one too (tried again)");
        check(save.cell(cell) == null && !RoadSweeper.isClearing(cell), "and the cell is free");
        // Now one chunk that always throws: the rest is cleared, the cell is not given away.
        int second = cellWithBlocks(road, server, owner, 4);
        long[] chunks2 = save.cell(second).chunks();
        RoadSweeper.injectFault(key -> key == chunks2[2]);
        RoadSweeper.queue(server, second, false);
        RoadSweeper.drain(server);
        RoadSweeper.injectFault(null);
        int left = 0;
        for (int i = 0; i < chunks2.length; i++) {
            if (i != 2) {
                check(RoadTestSupport.blocksIn(road, new long[]{chunks2[i]}) == 0, "chunk " + i + " is clear");
            } else {
                left += RoadTestSupport.blocksIn(road, new long[]{chunks2[i]});
            }
        }
        check(left > 0, "the chunk that could not be cleared still has its blocks");
        check(save.cell(second) != null, "so the cell stays in the ledger (nobody is dealt a dirty one): the next start tries again");
        check(save.allocate(owner, "x", 64) != second, "and it is not allocated again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "road_review_sweep2", timeoutTicks = 400)
    public static void theEntitiesOfACellAreRemovedAMomentAfterItsBlocksWithTheChunksHeld(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadTestSupport.road(helper);
        RoadSave save = RoadSave.get(server);
        UUID owner = UUID.randomUUID();
        int cell = cellWithBlocks(road, server, owner, 1);
        long[] chunks = save.cell(cell).chunks();
        BlockPos origin = RoadCells.origin(cell);
        ArmorStand stand = EntityType.ARMOR_STAND.create(road);
        stand.moveTo(origin.getX() + 3.5, origin.getY() + 3.0, origin.getZ() + 3.5);
        road.addFreshEntity(stand);
        f.onClose(() -> {
            RoadSweeper.queue(server, cell, false);
            RoadSweeper.drain(server);
        });
        RoadSweeper.queue(server, cell, false);
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    check(RoadTestSupport.blocksIn(road, chunks) == 0, "the blocks are gone");
                    check(!stand.isRemoved() && RoadSweeper.isClearing(cell), "the entity is not yet: the chunks are held for a moment, in case entities are still arriving");
                    check(RoadTestSupport.tickets(road, owner) == chunks.length, "with their tickets");
                })
                .thenWaitUntil(() -> check(stand.isRemoved() && !RoadSweeper.isClearing(cell), "later the entity goes and the cell is free"))
                .thenExecute(() -> check(RoadTestSupport.tickets(road, owner) == 0 && save.cell(cell) == null, "and the tickets are let go"))
                .thenSucceed();
    }

    // ---- R2: the border ----

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theRoadsWorldBorderIsAlwaysAsBigAsTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerLevel road = RoadTestSupport.road(helper);
        // What the overworld's /worldborder does to every dimension.
        road.getWorldBorder().setCenter(500.0, 500.0);
        road.getWorldBorder().setSize(100.0);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    check(road.getWorldBorder().getSize() > 5.0E7 && road.getWorldBorder().getCenterX() == 0.0 && road.getWorldBorder().getCenterZ() == 0.0, "the border is the world's");
                    BlockPos far = RoadCells.origin(RoadTestSupport.trial(player).cell());
                    check(road.getWorldBorder().isWithinBounds(far.above(5)), "a cell is inside it: " + far);
                    road.getWorldBorder().setSize(100.0);
                })
                .thenWaitUntil(() -> check(road.getWorldBorder().getSize() > 5.0E7, "and it is put right again whenever it changes"))
                .thenSucceed();
    }

    // ---- R6: the put-aside backpack is saved with the player ----

    @EnhancedTest(template = ARENA, batch = "road_review_stash", timeoutTicks = 500)
    public static void thePutAsideBackpackIsSavedInThePlayersOwnDataNotTheWorlds(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        f.onClose(RoadChallenges.swap(List.of(RoadChallenges.byId(StructureCopyChallenge.ID))));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 7));
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    check(StructureSupply.hasStash(player, StructureCopyChallenge.ID), "his things are put aside");
                    CompoundTag saved = player.saveWithoutId(new CompoundTag());
                    check(saved.toString().contains("enhanced:road_stash") && saved.toString().contains("minecraft:diamond"), "the stash is in the player's own save");
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    check(progress.data(StructureCopyChallenge.ID).isEmpty(), "and not in the world's save of the Road");
                    check(!ModAttachments.ROAD_STASH.get(player).isEmpty(), "(the attachment)");
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(player.getInventory().getItem(5).is(Items.DIAMOND) && player.getInventory().getItem(5).getCount() == 7, "he has his diamonds back");
                    check(ModAttachments.ROAD_STASH.get(player).isEmpty() && !player.saveWithoutId(new CompoundTag()).toString().contains("enhanced:road_stash"),
                            "and the stash goes in the same moment, so the two can never be saved apart");
                })
                .thenSucceed();
    }

    // ---- R12, R14, R15, R18 and the small ones ----

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void aPlayerWhoseTrialIsStillBeingBuiltDoesNotDie(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        check(RoadTrials.isInside(player) && RoadTrials.trialOf(player.getUUID()) == null, "the trial is being built");
        player.die(player.damageSources().genericKill());
        check(player.isAlive() && player.getHealth() == player.getMaxHealth(), "a death now is turned away");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void whatHeCraftsFromTheTrialsBlocksIsTheTrialsToo(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        ServerLevel road = RoadTestSupport.road(helper);
        RoadTestSupport.put(player, road, new Vec3(0.5, 150.0, 0.5));
        ItemStack sticks = new ItemStack(Items.STICK, 4);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, sticks, player.inventoryMenu.getCraftSlots()));
        check(StructureSupply.isSupply(sticks), "crafted in the Road: marked, so it is taken back with the blocks");
        ItemStack elsewhere = new ItemStack(Items.STICK, 4);
        RoadTestSupport.put(player, helper.getLevel(), helper.absoluteVec(new Vec3(10.5, 2.0, 10.5)));
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, elsewhere, player.inventoryMenu.getCraftSlots()));
        check(!StructureSupply.isSupply(elsewhere), "crafted in the world: not touched");
        player.inventoryMenu.getCraftSlots().setItem(0, StructureSupply.stack(Items.OAK_PLANKS, 2));
        player.inventoryMenu.getCraftSlots().setItem(1, new ItemStack(Items.OAK_PLANKS, 2));
        StructureSupply.strip(player);
        check(player.inventoryMenu.getCraftSlots().isEmpty(), "and the crafting square is emptied when the trial's things are taken back");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "road_review_limit", timeoutTicks = 300)
    public static void theBoatRacesTimeLimitIsNeverShorterThanThreeTimesThePar(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        IceTrack track = IceTrackGenerator.generate(5L, IceBoatChallenge.params());
        int par = IceBoatChallenge.parSeconds(track);
        f.config(WitchesRoadConfig.BOAT_RACE_TIME_LIMIT_SECONDS, 10);
        check(IceBoatChallenge.timeLimitTicks(track) == Math.max(200, par * 3 * 20) && IceBoatChallenge.timeLimitTicks(track) >= par * 60, "a short limit is lifted to three times the par: "
                + IceBoatChallenge.timeLimitTicks(track) + " for par " + par);
        f.config(WitchesRoadConfig.BOAT_RACE_FAIR_FACTOR, 0.0);
        check(IceBoatChallenge.timeLimitTicks(track) == 200, "with the factor 0 it is exactly the limit");
        f.config(WitchesRoadConfig.BOAT_RACE_FAIR_FACTOR, 3.0);
        f.config(WitchesRoadConfig.BOAT_RACE_TIME_LIMIT_SECONDS, 0);
        check(IceBoatChallenge.timeLimitTicks(track) == 0, "none stays none");
        f.config(WitchesRoadConfig.BOAT_RACE_TIME_LIMIT_SECONDS, 3600);
        check(IceBoatChallenge.timeLimitTicks(track) == 3600 * 20, "a long one is kept");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void theParkoursCheckpointCountsOnTheWholePadAndTheDiscardedBoatIsForgotten(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    AABB zone = trial.layout().checkpoint(1).zone();
                    check(zone.getXsize() >= 5.0 && zone.getZsize() >= 5.0, "a checkpoint is the whole five by five pad: " + zone);
                    // A player on the rim of the pad has reached it.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), new Vec3(zone.minX + 0.3, zone.minY + 0.1, zone.minZ + 0.3));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 1, "the rim counts: checkpoint " + trial.checkpoint());
                    ArmorStand a = EntityType.ARMOR_STAND.create(RoadTestSupport.road(helper));
                    trial.track(a);
                    check(trial.tracked().contains(a), "tracked");
                    trial.untrack(a);
                    check(!trial.tracked().contains(a), "and forgotten when the trial takes it down itself");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRoadBansWhatPutsAnEntityInHisCellAndHisHebrewIsInThePlural(GameTestHelper helper) throws IOException {
        for (ItemStack stack : new ItemStack[]{new ItemStack(Items.ARMOR_STAND), new ItemStack(Items.OAK_BOAT), new ItemStack(Items.MINECART), new ItemStack(Items.PAINTING),
                new ItemStack(Items.ITEM_FRAME), new ItemStack(Items.ZOMBIE_SPAWN_EGG), new ItemStack(Items.ENDER_PEARL)}) {
            check(RoadRules.isBanned(stack), stack + " cannot be used in the Road");
        }
        check(!RoadRules.isBanned(new ItemStack(Items.DIAMOND)) && !RoadRules.isBanned(new ItemStack(Items.OAK_PLANKS)), "blocks and the rest are free");
        JsonObject he;
        try (InputStreamReader reader = new InputStreamReader(RoadReviewTests.class.getResourceAsStream("/assets/enhanced/lang/he_il.json"), StandardCharsets.UTF_8)) {
            he = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (String key : he.keySet()) {
            if (key.startsWith("message.enhanced.road.") || key.startsWith("screen.enhanced.road.") || key.startsWith("road.enhanced.")) {
                String text = " " + he.get(key).getAsString() + " ";
                for (String singular : new String[]{" לך ", "שלך", "שבך", "בשבילך", " אתה ", " עליך "}) {
                    check(!text.contains(singular), key + " speaks to one person (" + singular.trim() + "): " + text.trim());
                }
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyTrialsTitleFitsTheScreenInBothLanguages(GameTestHelper helper) throws IOException {
        JsonObject en;
        JsonObject he;
        try (InputStreamReader reader = new InputStreamReader(RoadReviewTests.class.getResourceAsStream("/assets/enhanced/lang/en_us.json"), StandardCharsets.UTF_8)) {
            en = JsonParser.parseReader(reader).getAsJsonObject();
        }
        try (InputStreamReader reader = new InputStreamReader(RoadReviewTests.class.getResourceAsStream("/assets/enhanced/lang/he_il.json"), StandardCharsets.UTF_8)) {
            he = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (RoadChallenge challenge : RoadChallenges.all()) {
            String key = ((net.minecraft.network.chat.contents.TranslatableContents) challenge.titleShort().getContents()).getKey();
            check(en.has(key) && he.has(key), challenge.id() + " has a short title in both languages: " + key);
            // ("n. " and the name, at four times the size of text, in a window 854 wide at the usual scale: about 16 characters.)
            check(en.get(key).getAsString().length() <= 13 && he.get(key).getAsString().length() <= 13, key + " is short enough for a title");
        }
        helper.succeed();
    }

    /**
     * The race that made {@code anOperatorTakingThePlayerOutOfTheRoadEndsHisTrial} fail once in five full runs: tests of one batch run side by
     * side and share the ledger. When another test's sweep (RoadSweeper.drain is global) has cleared a cell, the lowest free number goes to the
     * next trial at once, so "the cell number is not in the ledger" or "is not being cleared" says nothing about THIS player's cell: judge by owner.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCellThatIsFreeAgainIsGivenToTheNextTrialAtOnceSoTestsJudgeByOwner(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RoadSave save = RoadSave.get(server);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        int cell = save.allocate(first, "parkour", 64);
        save.release(cell);
        int again = save.allocate(second, "parkour", 64);
        try {
            check(again <= cell, "the lowest free number is handed out again at once: " + cell + " then " + again);
            RoadSave.Cell held = save.cell(cell);
            boolean taken = again == cell;
            // The check of that test: the number is in the ledger and nothing is clearing it, yet it is not the first player's any more.
            boolean oldCheckFails = taken && !RoadSweeper.isClearing(cell) && held != null;
            boolean newCheckHolds = RoadSweeper.isClearing(cell) || held == null || !held.owner().equals(first);
            check(!taken || (oldCheckFails && newCheckHolds), "judged by number it looks as if the cell were still his; judged by owner it is not");
        } finally {
            save.release(again);
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aWorldOfThreeDotOneWithNothingOfTheRoadLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        // A save with no Road in it at all (a 3.1 world): empty, no player, no cell.
        RoadSave empty = RoadSave.load(new CompoundTag(), server.registryAccess());
        check(empty.players().isEmpty() && empty.cellCount() == 0, "nothing of the Road");
        check(empty.progress(UUID.randomUUID()).isBlank() && empty.allocate(UUID.randomUUID(), "parkour", 8) == 0, "and the first cell is cell 0");
        // The attachments a 3.1 player has not got read as empty.
        ServerPlayer player = TestKit.fixture(helper).player(10.5, TestKit.FLOOR, 10.5);
        check(ModAttachments.ROAD_STASH.get(player).isEmpty(), "a player without the stash reads as empty");
        check(RoadChallenges.count() >= 1 && new ChunkPos(0, 0).x == 0, "(the trials are there)");
        helper.succeed();
    }

    private RoadReviewTests() {
    }
}
