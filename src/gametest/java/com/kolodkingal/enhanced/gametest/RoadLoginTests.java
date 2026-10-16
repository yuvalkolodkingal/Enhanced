package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadCells;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Road and the world's save: a player who logs out in a trial and in again, who logs in inside the Road without a trial, who is
 * taken out by an operator, who comes back from the dead somewhere else; a server that starts with cells in its ledger and one that
 * stops with trials running. (A test player cannot really log out; the tests call what the logout and login events call.)
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadLoginTests {
    static final String BATCH = "road_login";

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void loggingOutTakesTheTrialDownAndLoggingInResumesItAtTheCheckpoint(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        long[][] chunks = new long[1][];
        int[] cell = new int[1];
        Vec3[] second = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    second[0] = trial.layout().checkpoint(2).respawn();
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial.layout().checkpoint(2).zone()));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 2, "he is at checkpoint 2");
                    chunks[0] = trial.chunks();
                    cell[0] = trial.cell();
                    RoadTrials.onLogout(player);
                    check(RoadTrials.trialOf(player.getUUID()) == null && !RoadTrials.isInside(player), "the trial is taken down when he logs out");
                    check(RoadSweeper.isClearing(cell[0]), "its cell is queued for clearing");
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    check(progress.inside() && progress.checkpoint() == 2 && progress.entry() != null, "but the save says where he is, and where he came from");
                    RoadSweeper.drain(server);
                    check(RoadTestSupport.blocksIn(RoadTestSupport.road(helper), chunks[0]) == 0 && RoadSave.get(server).cell(cell[0]) == null, "nothing is left of it");
                    // He logs in again, in the Road dimension, over the void.
                    RoadTrials.reconcile(player);
                    check(RoadTrials.isInside(player), "the Road builds it again for him");
                })
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(RoadDimension.in(player) && trial.checkpoint() == 2, "he goes on at checkpoint 2");
                    Vec3 at = trial.layout().checkpoint(2).respawn();
                    check(player.position().distanceTo(at) < 0.01, "on his checkpoint");
                    check(RoadTestSupport.blocksIn(RoadTestSupport.road(helper), trial.chunks()) > 500, "on the same course, drawn again");
                    check(trial.layout().checkpoint(2).respawn().subtract(Vec3.atLowerCornerOf(RoadCells.origin(trial.cell())))
                            .distanceTo(second[0].subtract(Vec3.atLowerCornerOf(RoadCells.origin(cell[0])))) < 0.01, "exactly the same course");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void aPlayerInTheRoadWithNoTrialIsSentHome(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadTestSupport.road(helper);
        ServerPlayer lost = RoadTestSupport.walker(f, 10.5, 10.5);
        ServerPlayer known = RoadTestSupport.walker(f, 6.5, 6.5);
        Vec3 home = known.position();
        // Nobody knows where the first came from: the world's spawn. The second has a way home saved.
        RoadTestSupport.put(lost, road, new Vec3(0.5, 150.0, 0.5));
        RoadTestSupport.put(known, road, new Vec3(0.5, 150.0, 2000.5));
        lost.hasChangedDimension();
        known.hasChangedDimension();
        RoadProgress progress = RoadSave.get(server).progress(known.getUUID());
        progress.setEntry(new RoadProgress.Entry(helper.getLevel().dimension(), home, 90.0F, 10.0F, GameType.SURVIVAL.getId()));
        check(RoadDimension.in(lost) && RoadDimension.in(known), "both are in the Road with no trial");
        RoadTrials.reconcile(lost);
        RoadTrials.reconcile(known);
        check(!RoadDimension.in(lost) && lost.serverLevel() == server.overworld(), "the first is taken to the world");
        BlockPos spawn = server.overworld().getSharedSpawnPos();
        check(Math.abs(lost.getX() - (spawn.getX() + 0.5)) < 1.0 && Math.abs(lost.getZ() - (spawn.getZ() + 0.5)) < 1.0 && server.overworld().noCollision(lost),
                "to its spawn, in the open: " + lost.position());
        check(!RoadDimension.in(known) && known.serverLevel() == helper.getLevel() && known.position().distanceTo(home) < 0.01, "the second is taken to where he came from");
        check(known.getYRot() == 90.0F, "looking the way he looked");
        check(!RoadSave.get(server).progress(known.getUUID()).inside() && RoadSave.get(server).progress(known.getUUID()).entry() == null, "and the save is clean");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aSaveThatSaysInsideForAPlayerOutsideIsCorrected(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
        progress.setInside(true);
        progress.setEntry(new RoadProgress.Entry(helper.getLevel().dimension(), player.position(), 0.0F, 0.0F, 0));
        Vec3 where = player.position();
        RoadTrials.reconcile(player);
        check(!progress.inside() && progress.entry() == null, "the save no longer says he is inside");
        check(player.position().distanceTo(where) < 0.01, "and he was not moved");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void aServerStartClearsEveryCellItFindsButNoLiveTrial(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadTestSupport.road(helper);
        RoadSave save = RoadSave.get(server);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        // What a crash left behind: a cell in the ledger, with blocks in it, that nobody is using.
        UUID gone = UUID.randomUUID();
        int orphan = save.allocate(gone, "parkour", 64);
        RoadCanvas canvas = new RoadCanvas(orphan, road.getMinBuildHeight(), road.getMaxBuildHeight());
        for (int x = -40; x <= 40; x += 5) {
            canvas.set(x, 2, 0, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        canvas.seal();
        save.plan(orphan, canvas.chunks(), canvas.minY(), canvas.maxY());
        while (!canvas.placeSome(road, 1000)) {
            // placing
        }
        long[] orphanChunks = canvas.chunks();
        f.onClose(() -> {
            RoadSweeper.queue(server, orphan, false);
            RoadSweeper.drain(server);
        });
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(RoadTestSupport.blocksIn(road, orphanChunks) == canvas.blockCount() && save.cell(orphan) != null, "the cell is there, with its blocks");
                    RoadTrials.onServerStarted(server);
                    check(RoadSweeper.isClearing(orphan) && !RoadSweeper.isClearing(trial.cell()), "the server's start queues it, not a trial that is being played");
                    RoadSweeper.drain(server);
                    check(RoadTestSupport.blocksIn(road, orphanChunks) == 0 && save.cell(orphan) == null, "and it is cleared");
                    check(save.cell(trial.cell()) != null && RoadTestSupport.blocksIn(road, trial.chunks()) > 500, "the live trial is untouched");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void anOperatorTakingThePlayerOutOfTheRoadEndsHisTrial(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        makeOperator(f, server, player);
        check(RoadTrials.enter(player), "he walks the Road");
        int[] cell = new int[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    cell[0] = RoadTestSupport.trial(player).cell();
                    player.teleportTo(helper.getLevel(), helper.absoluteVec(new Vec3(10.5, 2.0, 10.5)).x, helper.absoluteVec(new Vec3(10.5, 2.0, 10.5)).y,
                            helper.absoluteVec(new Vec3(10.5, 2.0, 10.5)).z, 0.0F, 0.0F);
                    check(!RoadDimension.in(player), "an operator's teleport takes him out (the Road does not stop operators): op " + player.hasPermissions(2) + " in " + player.level().dimension().location());
                })
                .thenExecuteAfter(3, () -> {
                    check(RoadTrials.trialOf(player.getUUID()) == null, "the Road lets him go: no trial");
                    check(RoadSweeper.isClearing(cell[0]) || RoadSave.get(server).cell(cell[0]) == null, "and takes it down");
                    check(!RoadSave.get(server).progress(player.getUUID()).inside(), "he is not inside");
                    check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && player.getMainHandItem().getCount() == 1, "with his game mode and his Gene");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void aPlayerBroughtBackFromTheDeadElsewhereIsPutBackInHisTrial(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        makeOperator(f, server, player);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    Vec3 world = helper.absoluteVec(new Vec3(10.5, 2.0, 10.5));
                    player.teleportTo(helper.getLevel(), world.x, world.y, world.z, 0.0F, 0.0F);
                    // (What the respawn event does when somebody dies in the Road in a way the Road did not catch.)
                    RoadTrials.reconcile(player);
                    check(RoadDimension.in(player) && player.position().distanceTo(trial.layout().checkpoint(0).respawn()) < 0.01, "he is put back at his checkpoint");
                })
                .thenSucceed();
    }

    /** An operator for the length of the test (the GameTest server gives the operators it makes the permission level 0). */
    private static void makeOperator(TestKit.Fixture f, MinecraftServer server, ServerPlayer player) {
        server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 4, false));
        f.onClose(() -> server.getPlayerList().getOps().remove(player.getGameProfile()));
    }

    private RoadLoginTests() {
    }
}
