package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.boat.IceTrack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * A scripted drive of the Ice-Boat Race: a test player has no client to steer his boat, so a driver ({@link IceBot}) does what the
 * client of a player does, on the boat of the trial, with the real physics of the game ({@link RealBoat}), while the Road itself runs
 * as it does for a player (checkpoints, falls, the finish). The driver finishes the whole course on its own.
 */
public final class RoadBoatRunTests {
    static final String BATCH = "road_boat_run";

    /** A boat driven by the bot, between the Road's ticks. */
    private static final class Drive {
        final IceTrack track;
        final IceBot bot;
        final IceBoatSim sim;
        final BlockPos origin;
        RealBoat real;
        int steps;
        int falls;

        Drive(RoadTrial trial, boolean shortcuts) {
            this.track = RoadBoatTests.trackOf(trial);
            this.bot = new IceBot(track, shortcuts, 1, 0.0, 3);
            this.sim = new IceBoatSim(new IceBoatSim.TrackWorld(track.raster()), 0, 0, 0, 0.0F);
            this.origin = trial.canvas().origin();
        }

        /** One tick of the client: the keys the driver presses, and the real boat's physics for them. */
        void step(RoadTrial trial, ServerPlayer player) {
            Boat boat = RoadBoatTests.boatOf(trial);
            if (real == null || real.boat != boat) {
                if (real != null) {
                    falls++;
                }
                real = RealBoat.adopt(boat);
                bot.relocated();
            }
            real.copyTo(sim, origin.getX(), origin.getY(), origin.getZ());
            IceBot.Keys keys = bot.drive(sim);
            real.step(keys.left(), keys.right(), keys.up(), keys.down());
            boat.positionRider(player);
            steps++;
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 3200)
    public static void aDriverSailsTheWholeCourseOnTheRealBoatAndWinsTheTrial(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerLevel road = RoadDimension.level(helper.getLevel().getServer());
        ServerPlayer player = RoadBoatTests.walker(f, 10.5, 10.5, 11);
        check(RoadTrials.enter(player, RoadBoatTests.trialNumber()), "he walks the Road");
        Drive[] drive = new Drive[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> RoadBoatTests.walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenWaitUntil(() -> check(RoadBoatTests.barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> drive[0] = new Drive(RoadTestSupport.trial(player), false))
                .thenWaitUntil(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    if (trial.phase() == RoadTrial.Phase.ACTIVE) {
                        // The client moves the boat twice a tick here (the course is long): the Road's checkpoints are six blocks deep, a boat
                        // goes at most four blocks in two steps.
                        drive[0].step(trial, player);
                        drive[0].step(trial, player);
                    }
                    check(trial.phase() == RoadTrial.Phase.WON, "the boat is still on its way: " + drive[0].steps + " steps, checkpoint " + trial.checkpoint() + " of "
                            + (trial.layout().checkpoints().size() - 1) + ", at " + player.position());
                })
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    RoadLayout layout = trial.layout();
                    check(drive[0].falls == 0, "he never fell off: " + drive[0].falls);
                    check(trial.checkpoint() == layout.checkpoints().size() - 1, "every checkpoint was reached, in order: " + trial.checkpoint());
                    RoadProgress progress = RoadSave.get(player.server).progress(player.getUUID());
                    check(progress.completed() == RoadBoatTests.trialNumber() + 1 && progress.data(IceBoatChallenge.ID).getInt(IceBoatChallenge.BEST) > 300, "the trial is won, with a time: "
                            + progress.data(IceBoatChallenge.ID));
                    check(drive[0].steps > 700 && drive[0].steps < 3000, "after a long drive: " + drive[0].steps + " steps");
                    check(player.getVehicle() == null && !player.isDeadOrDying(), "and the player stands on the finish platform");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = "road_boat_gap", timeoutTicks = 1200)
    public static void aBoatThatIsTooSlowForTheShortcutFallsIntoTheGapAndIsPutBackAtTheCheckpoint(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerLevel road = RoadDimension.level(helper.getLevel().getServer());
        // The first player whose course has a shortcut.
        long id = 20;
        while (true) {
            UUID uuid = new UUID(0x1CEB0A7L, id);
            IceTrack track = com.kolodkingal.enhanced.road.boat.IceTrackGenerator.generate(RoadTrials.seed(uuid, IceBoatChallenge.ID, 0L), IceBoatChallenge.params());
            if (!track.loops().isEmpty()) {
                break;
            }
            id++;
        }
        ServerPlayer player = RoadBoatTests.walker(f, 10.5, 10.5, id);
        check(RoadTrials.enter(player, RoadBoatTests.trialNumber()), "he walks the Road");
        Boat[] first = new Boat[1];
        Drive[] drive = new Drive[1];
        int[] before = new int[1];
        Vec3[] gapAt = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> RoadBoatTests.walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenWaitUntil(() -> check(RoadBoatTests.barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    IceTrack track = RoadBoatTests.trackOf(trial);
                    IceTrack.Loop loop = track.loops().get(0);
                    int dx = IceTrack.dirX(loop.heading());
                    int dz = IceTrack.dirZ(loop.heading());
                    BlockPos origin = trial.canvas().origin();
                    // He is on the straight of the shortcut, three blocks before the gap, at rest, and holds W: far too slow.
                    Vec3 at = new Vec3(origin.getX() + Math.floor(loop.forkX()) + 0.5 + dx * (loop.gapFrom() - 3), origin.getY() + 0.0 + 1.0,
                            origin.getZ() + Math.floor(loop.forkZ()) + 0.5 + dz * (loop.gapFrom() - 3));
                    first[0] = RoadBoatTests.boatOf(trial);
                    // First over the line after the barrier (checkpoint 1), so that the fall puts him back on the course, not at the start.
                    RoadBoatTests.carry(player, trial.layout().checkpoint(1).respawn());
                    drive[0] = new Drive(trial, false);
                    gapAt[0] = at;
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 1, "the start line is his checkpoint: " + trial.checkpoint());
                    before[0] = trial.checkpoint();
                    RoadBoatTests.carry(player, gapAt[0]);
                    drive[0].real = RealBoat.adopt(first[0]);
                })
                .thenWaitUntil(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    Boat boat = RoadBoatTests.boatOf(trial);
                    if (boat == first[0]) {
                        drive[0].real.step(false, false, true, false);
                        first[0].positionRider(player);
                    }
                    check(boat != first[0], "the boat falls into the gap and is replaced: y " + first[0].getY());
                })
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    Boat now = RoadBoatTests.boatOf(trial);
                    RoadLayout.Checkpoint cp = trial.layout().checkpoint(trial.checkpoint());
                    check(player.getVehicle() == now && now.position().distanceTo(cp.respawn()) < 0.01, "he is on the last checkpoint in a new boat");
                    check(trial.checkpoint() == before[0] && RoadSave.get(player.server).progress(player.getUUID()).falls() == 1, "the fall is counted");
                })
                .thenSucceed();
    }

    private RoadBoatRunTests() {
    }
}
