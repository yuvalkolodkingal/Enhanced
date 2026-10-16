package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.boat.IceTrack;
import com.kolodkingal.enhanced.road.boat.IceTrackGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Ice-Boat Race inside the Road (trial 3): the course drawn for the player, the boat parked at the start line, boarding and the
 * countdown, the barrier that opens at "go", falling off the course (the boat is replaced and the player put in it on the last
 * checkpoint), sneaking out of the boat (back to the checkpoint, with a cooldown), the time limit, the finish and the best time, logging
 * out and in, and the options. A test player has no client to drive the boat, so these move the boat and the player the way the
 * game's packets would; the drive itself is in {@link RoadBoatRunTests}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadBoatTests {
    static final String BATCH = "road_boat";

    /** The number of the Ice-Boat Race among the trials. */
    static int trialNumber() {
        return RoadChallenges.indexOf(RoadChallenges.byId(IceBoatChallenge.ID));
    }

    /** A Survival player without powers holding a Gene (a fixed UUID gives a fixed course). */
    static ServerPlayer walker(TestKit.Fixture f, double x, double z, long id) {
        ServerPlayer player = f.player(x, FLOOR, z, GameType.SURVIVAL, new UUID(0x1CEB0A7L, id));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 1));
        f.onClose(() -> RoadTestSupport.forget(f.level().getServer(), player));
        return player;
    }

    /** The boat of the trial that is standing (the last one it spawned and has not removed). */
    static Boat boatOf(RoadTrial trial) {
        List<net.minecraft.world.entity.Entity> tracked = trial.tracked();
        for (int i = tracked.size() - 1; i >= 0; i--) {
            if (tracked.get(i) instanceof Boat boat && !boat.isRemoved()) {
                return boat;
            }
        }
        check(false, "the trial has a boat");
        return null;
    }

    /** The course of the trial (made again from its seed: the same). */
    static IceTrack trackOf(RoadTrial trial) {
        return IceTrackGenerator.generate(trial.seed(), IceBoatChallenge.params());
    }

    /** The player walks up to the boat (and the next tick he is in it). */
    static void walkToTheBoat(ServerPlayer player, RoadTrial trial, ServerLevel road) {
        Boat boat = boatOf(trial);
        Vec3 at = boat.position().add(0.0, 0.0, 0.0);
        IceTrack track = trackOf(trial);
        RoadTestSupport.put(player, road, at.subtract(IceTrack.dirX(track.heading()) * 1.5, 0.0, IceTrack.dirZ(track.heading()) * 1.5));
    }

    /** Moves the boat the player is in, with him, as his client's packets would. */
    static void carry(ServerPlayer player, Vec3 to) {
        check(player.getVehicle() instanceof Boat, "he is in a boat");
        Boat boat = (Boat) player.getVehicle();
        boat.setPos(to);
        boat.positionRider(player);
    }

    /** The glass of the start barrier: every cell is glass, or every cell is open. */
    static boolean barrierShut(RoadTrial trial, ServerLevel road) {
        boolean shut = true;
        for (BlockPos pos : IceBoatChallenge.barrierCells(trackOf(trial), trial.canvas().origin())) {
            shut &= road.getBlockState(pos).is(Blocks.PURPLE_STAINED_GLASS);
        }
        return shut;
    }

    static boolean barrierOpen(RoadTrial trial, ServerLevel road) {
        boolean open = true;
        for (BlockPos pos : IceBoatChallenge.barrierCells(trackOf(trial), trial.canvas().origin())) {
            open &= road.getBlockState(pos).isAir();
        }
        return open;
    }

    // ------------------------------------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void theCourseIsDrawnWithTheBoatAtTheStartLineAndNothingIsLeftAfterwards(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        ServerPlayer player = walker(f, 10.5, 10.5, 1);
        check(trialNumber() >= 1, "the race is a trial of the Road after the parkour: " + trialNumber());
        check(RoadTrials.enter(player, trialNumber()), "the Road builds the race for him");
        long[][] chunks = new long[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    chunks[0] = trial.chunks();
                    RoadLayout layout = trial.layout();
                    IceTrack track = trackOf(trial);
                    check(trial.challenge().id().equals("ice_boat") && trial.number() == trialNumber(), "it is the ice-boat race");
                    check(layout.finish() == null && layout.gate() != null && layout.exitDoor() != null, "with a gate, an exit door, and a finish the race itself judges");
                    check(layout.timeLimitTicks() == WitchesRoadConfig.BOAT_RACE_TIME_LIMIT_SECONDS.get() * 20, "and the time limit of the config: " + layout.timeLimitTicks());
                    check(layout.checkpoints().size() == track.checkpoints().size() + 2, "the start, the start line and the checkpoints: " + layout.checkpoints().size());
                    check(player.position().distanceTo(layout.start()) < 0.01 && player.getVehicle() == null, "the player stands on the start platform, not in the boat");
                    check(road.getBlockState(layout.exitDoor()).is(ModBlocks.WITCHES_ROAD_DOOR.get()), "the exit door is there");
                    check(RoadTestSupport.blocksIn(road, chunks[0]) > 20000, "the course is drawn: " + RoadTestSupport.blocksIn(road, chunks[0]) + " blocks");
                    Boat boat = boatOf(trial);
                    BlockPos origin = trial.canvas().origin();
                    int h = track.heading();
                    Vec3 park = new Vec3(origin.getX() + track.platformX() + IceTrack.dirX(h) * IceTrackGenerator.BOAT_PARK, origin.getY() + 1.0,
                            origin.getZ() + track.platformZ() + IceTrack.dirZ(h) * IceTrackGenerator.BOAT_PARK);
                    check(boat.position().distanceTo(park) < 0.01, "the boat is parked at the start line: " + boat.position() + " vs " + park);
                    check(boat.isInvulnerable() && boat.getFirstPassenger() == null, "an invulnerable boat with nobody in it");
                    check(Math.abs(Math.abs(boat.getYRot() - IceBoatChallenge.yawOf(h)) % 360.0F) < 0.01F, "pointing along the track");
                    check(road.getBlockState(BlockPos.containing(park).below()).is(Blocks.PACKED_ICE), "on packed ice");
                    check(barrierShut(trial, road), "the barrier in front of it is shut");
                    // The floor and the walls of the first stretch.
                    int dx = IceTrack.dirX(h);
                    int dz = IceTrack.dirZ(h);
                    BlockPos mid = BlockPos.containing(origin.getX() + track.platformX() + dx * 20.0, origin.getY(), origin.getZ() + track.platformZ() + dz * 20.0);
                    check(road.getBlockState(mid).is(Blocks.PACKED_ICE) || road.getBlockState(mid).is(Blocks.BLUE_ICE), "ice on the track: " + road.getBlockState(mid));
                    BlockPos wall = mid.offset(-dz * 6, 1, dx * 6);
                    check(road.getBlockState(wall).is(Blocks.PACKED_ICE) || road.getBlockState(wall).is(Blocks.SEA_LANTERN), "a wall of ice beside it: " + road.getBlockState(wall));
                    check(road.getBlockState(wall.above()).isSolid() || road.getBlockState(wall.above().above()).isSolid(), "two blocks high");
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    RoadSweeper.drain(server);
                    check(RoadTestSupport.blocksIn(road, chunks[0]) == 0, "when he leaves nothing of the course is left");
                    check(!IceBoatChallenge.hasRace(player.getUUID()), "and the race forgets him");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void theBarrierHoldsUntilGoAndTheNewBoatCarriesThePlayer(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerLevel road = RoadDimension.level(helper.getLevel().getServer());
        ServerPlayer player = walker(f, 10.5, 10.5, 2);
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    walkToTheBoat(player, trial, road);
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(player.getVehicle() == boatOf(trial), "he is in the boat: he walked up to it");
                    check(barrierShut(trial, road), "the barrier holds while the countdown runs");
                })
                .thenExecuteAfter(30, () -> check(barrierShut(RoadTestSupport.trial(player), road), "still shut after a second and a half"))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "at \"go\" the barrier is gone"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(player.getVehicle() == boatOf(trial), "he is still in his boat when the race begins");
                    check(trial.elapsed() >= WitchesRoadConfig.BOAT_RACE_COUNTDOWN_SECONDS.get() * 20, "after the countdown: " + trial.elapsed());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void aPlayerWhoClimbsOutDuringTheCountdownIsBackOnTheGridAndTheCountdownStartsAgain(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerLevel road = RoadDimension.level(helper.getLevel().getServer());
        ServerPlayer player = walker(f, 10.5, 10.5, 3);
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenExecuteAfter(10, () -> {
                    check(player.getVehicle() != null, "in the boat");
                    player.stopRiding();
                })
                .thenExecuteAfter(2, () -> {
                    check(player.getVehicle() == null, "he is out: the race does not drag him back at once");
                    // Away from the boat (to the exit door, say).
                    RoadTrial trial = RoadTestSupport.trial(player);
                    IceTrack track = trackOf(trial);
                    RoadTestSupport.put(player, road, boatOf(trial).position().subtract(IceTrack.dirX(track.heading()) * 6.0, 0.0, IceTrack.dirZ(track.heading()) * 6.0));
                })
                // The countdown would be over by now.
                .thenExecuteAfter(80, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(barrierShut(trial, road), "the barrier is still shut: the countdown stopped");
                    check(player.getVehicle() == null, "and he is still out");
                    walkToTheBoat(player, trial, road);
                })
                .thenExecuteAfter(2, () -> check(player.getVehicle() != null, "walking up to the boat puts him in it again"))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "and a new countdown ends with the barrier open"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void aBoatThatFallsOffIsReplacedAndThePlayerIsPutInTheNewOneOnTheLastCheckpoint(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        ServerPlayer player = walker(f, 10.5, 10.5, 4);
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        Boat[] first = new Boat[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    first[0] = boatOf(trial);
                    // He sails to the second checkpoint (past the start line's) ...
                    carry(player, trial.layout().checkpoint(2).respawn());
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 2, "checkpoint 2 is reached: " + trial.checkpoint());
                    check(RoadSave.get(player.server).progress(player.getUUID()).checkpoint() == 2, "and saved");
                    // ... and goes off the edge.
                    carry(player, trial.layout().checkpoint(2).respawn().add(40.0, -60.0, 0.0));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    RoadLayout.Checkpoint cp = trial.layout().checkpoint(2);
                    Boat now = boatOf(trial);
                    check(now != first[0] && first[0].isRemoved(), "the old boat is gone and there is a new one");
                    check(player.getVehicle() == now, "the player is in it");
                    check(now.position().distanceTo(cp.respawn()) < 0.01, "on the checkpoint: " + now.position() + " vs " + cp.respawn());
                    check(Math.abs(Math.abs(now.getYRot() - cp.yaw()) % 360.0F) < 0.01F, "pointing along the track");
                    check(trial.checkpoint() == 2 && RoadSave.get(player.server).progress(player.getUUID()).falls() == 1, "the fall is counted, the checkpoint kept");
                    check(now.getDeltaMovement().lengthSqr() < 1.0E-9 && player.getHealth() == player.getMaxHealth(), "he is at rest, and unhurt");
                    check(RoadTestSupport.tickets(road, player.getUUID()) > 0, "and the course is still held");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_boat_sneak", timeoutTicks = 900)
    public static void sneakingOutOfTheBoatGoesBackToTheCheckpointAndAnEarlySneakOnlyKeepsHimInTheBoat(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.BOAT_RACE_RESPAWN_COOLDOWN_SECONDS, 2);
        ServerLevel road = RoadDimension.level(helper.getLevel().getServer());
        ServerPlayer player = walker(f, 10.5, 10.5, 5);
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        Boat[] boats = new Boat[3];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    carry(player, trial.layout().checkpoint(1).respawn());
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 1, "checkpoint 1: " + trial.checkpoint());
                    boats[0] = boatOf(trial);
                    player.stopRiding();
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    boats[1] = boatOf(trial);
                    check(boats[1] != boats[0] && boats[0].isRemoved(), "he sneaked out: he is put on the checkpoint in a new boat");
                    check(player.getVehicle() == boats[1] && boats[1].position().distanceTo(trial.layout().checkpoint(1).respawn()) < 0.01, "in it, on the checkpoint");
                    // At once again: too soon.
                    player.stopRiding();
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(player.getVehicle() == boats[1] && boatOf(trial) == boats[1] && !boats[1].isRemoved(), "a second sneak at once only keeps him in the same boat");
                })
                .thenExecuteAfter(45, () -> player.stopRiding())
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    boats[2] = boatOf(trial);
                    check(boats[2] != boats[1] && boats[1].isRemoved() && player.getVehicle() == boats[2], "after the cooldown it works again");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_boat_time", timeoutTicks = 900)
    public static void whenTheTimeRunsOutTheBoatAndThePlayerAreBackAtTheStartAndTheBarrierIsShutAgain(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.BOAT_RACE_TIME_LIMIT_SECONDS, 5);
        f.config(WitchesRoadConfig.BOAT_RACE_COUNTDOWN_SECONDS, 1);
        ServerLevel road = RoadDimension.level(helper.getLevel().getServer());
        ServerPlayer player = walker(f, 10.5, 10.5, 6);
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        Boat[] first = new Boat[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.layout().timeLimitTicks() == 100, "five seconds: " + trial.layout().timeLimitTicks());
                    walkToTheBoat(player, trial, road);
                })
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> first[0] = boatOf(RoadTestSupport.trial(player)))
                .thenWaitUntil(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(boatOf(trial) != first[0], "the time runs out: there is a new boat");
                })
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 0 && player.position().distanceTo(trial.layout().start()) < 0.01, "he is on the start platform");
                    check(player.getVehicle() == null && first[0].isRemoved(), "not in a boat, and the old boat is gone");
                    check(barrierShut(trial, road), "the barrier is shut again");
                    check(boatOf(trial).getFirstPassenger() == null && boatOf(trial) != first[0], "a new boat waits at the start line");
                    walkToTheBoat(player, trial, road);
                })
                .thenExecuteAfter(2, () -> check(player.getVehicle() != null, "he can sail again"))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "and the barrier opens again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void crossingTheFinishLineWinsTheTrialOpensTheGateAndKeepsTheBestTime(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        ServerPlayer player = walker(f, 10.5, 10.5, 7);
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        Boat[] boat = new Boat[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    boat[0] = boatOf(trial);
                    IceTrack track = trackOf(trial);
                    double[] zone = track.finishZone();
                    BlockPos origin = trial.canvas().origin();
                    check(trial.phase() == RoadTrial.Phase.ACTIVE, "the trial is on");
                    carry(player, new Vec3(origin.getX() + (zone[0] + zone[2]) / 2, origin.getY() + track.lowestLevel() + 1.0, origin.getZ() + (zone[1] + zone[3]) / 2));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    IceTrack track = trackOf(trial);
                    check(trial.phase() == RoadTrial.Phase.WON, "the finish line wins the trial: " + trial.phase());
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    check(progress.completed() == trialNumber() + 1, "and counts: " + progress.completed());
                    int best = progress.data(IceBoatChallenge.ID).getInt(IceBoatChallenge.BEST);
                    check(best > 0 && best < 200, "the time is saved as the best: " + best);
                    check(boat[0].isRemoved() && player.getVehicle() == null, "the boat is taken away");
                    BlockPos origin = trial.canvas().origin();
                    int fh = track.finishHeading();
                    Vec3 platform = new Vec3(origin.getX() + track.finishX(), origin.getY() + track.lowestLevel() + 1.0, origin.getZ() + track.finishZ());
                    check(player.position().distanceTo(platform) < 3.0 && Math.abs(player.getY() - platform.y) < 0.01, "he stands on the finish platform: " + player.position() + " vs " + platform);
                    RoadLayout.Gate gate = trial.layout().gate();
                    check(road.getBlockState(gate.base().above()).isAir() && road.getBlockState(gate.base().above(2)).isAir(), "the gate is open");
                    check(IceTrack.dirX(fh) != 0 || IceTrack.dirZ(fh) != 0, "(the way the gate faces)");
                    // Through the gate: the end of the Road (there is no trial after this one here, or the next is built).
                    RoadTestSupport.put(player, road, gate.zone().getCenter().subtract(0.0, 1.0, 0.0));
                })
                .thenExecuteAfter(3, () -> {
                    if (RoadChallenges.count() == trialNumber() + 1) {
                        // The last trial: the gate is the end of the Road, and he is taken back to the world with his Gene.
                        check(!RoadDimension.in(player) && !RoadTrials.isInside(player), "the end of the Road takes him back to the world");
                        check(RoadTrials.walkedAll(player), "he has walked the whole Road");
                    } else {
                        check(RoadTrials.trialOf(player.getUUID()) != null || RoadDimension.in(player), "the gate leads on to the next trial");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1100)
    public static void loggingBackInDuringTheRaceBuildsTheCourseAgainAndPutsHimInABoatOnHisCheckpoint(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        ServerPlayer player = walker(f, 10.5, 10.5, 8);
        // (He has won the trials before this one, so that logging in builds this one again.)
        RoadTrials.setCompleted(server, player.getUUID(), trialNumber());
        check(RoadTrials.enter(player), "he walks the Road");
        Vec3[] second = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    second[0] = trial.layout().checkpoint(2).respawn().subtract(Vec3.atLowerCornerOf(trial.canvas().origin()));
                    carry(player, trial.layout().checkpoint(2).respawn());
                })
                .thenExecuteAfter(2, () -> {
                    check(RoadTestSupport.trial(player).checkpoint() == 2, "checkpoint 2");
                    RoadTrial before = RoadTestSupport.trial(player);
                    check(before.phase() == RoadTrial.Phase.ACTIVE && player.getVehicle() instanceof Boat, "he is racing in a boat: " + before.phase() + " " + player.getVehicle());
                    Boat boat = boatOf(before);
                    RoadTrials.onLogout(player);
                    check(boat.isRemoved() && !IceBoatChallenge.hasRace(player.getUUID()), "logging out takes the boat and the race away");
                    RoadSweeper.drain(server);
                    RoadTrials.reconcile(player);
                })
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 2, "he goes on at checkpoint 2");
                    check(!trial.tracked().isEmpty(), "the new trial has spawned a boat: " + trial.tracked() + " " + trial.phase() + " " + player.getVehicle());
                    Boat boat = boatOf(trial);
                    check(player.getVehicle() == boat && boat.position().distanceTo(trial.layout().checkpoint(2).respawn()) < 0.01, "in a boat on it");
                    check(barrierOpen(trial, road), "with no countdown and the barrier open");
                    check(trial.layout().checkpoint(2).respawn().subtract(Vec3.atLowerCornerOf(trial.canvas().origin())).distanceTo(second[0]) < 0.01,
                            "on the same course, drawn again");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void leavingTheRoadInTheMiddleOfTheRaceTakesEverythingAway(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        ServerPlayer player = walker(f, 10.5, 10.5, 9);
        Vec3 home = player.position();
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        Boat[] boat = new Boat[1];
        long[][] chunks = new long[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> walkToTheBoat(player, RoadTestSupport.trial(player), road))
                .thenWaitUntil(() -> check(barrierOpen(RoadTestSupport.trial(player), road), "go"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    boat[0] = boatOf(trial);
                    chunks[0] = trial.chunks();
                    check(IceBoatChallenge.hasRace(player.getUUID()), "a race is on");
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    RoadSweeper.drain(server);
                    check(boat[0].isRemoved() && player.getVehicle() == null, "the boat is gone and he is out of it");
                    check(!RoadDimension.in(player) && player.position().distanceTo(home) < 0.01, "he is back where he came from");
                    check(!IceBoatChallenge.hasRace(player.getUUID()) && RoadTestSupport.blocksIn(road, chunks[0]) == 0, "the race and the course are gone");
                    check(player.getInventory().contains(new ItemStack(ModItems.SCARLET_WITCH_GENE.get())), "and he keeps his Gene");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_boat_config", timeoutTicks = 500)
    public static void theOptionsShapeTheCourseAndTheClock(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.BOAT_RACE_LENGTH, 300);
        f.config(WitchesRoadConfig.BOAT_RACE_TRACK_WIDTH, 7);
        f.config(WitchesRoadConfig.BOAT_RACE_DIFFICULTY, 1);
        f.config(WitchesRoadConfig.BOAT_RACE_SHORTCUTS, 0);
        f.config(WitchesRoadConfig.BOAT_RACE_OBSTACLES, false);
        f.config(WitchesRoadConfig.BOAT_RACE_BLUE_ICE, false);
        f.config(WitchesRoadConfig.BOAT_RACE_CHECKPOINT_EVERY, 60);
        f.config(WitchesRoadConfig.BOAT_RACE_TIME_LIMIT_SECONDS, 90);
        IceTrackGenerator.Params params = IceBoatChallenge.params();
        check(params.length() == 300 && params.width() == 7 && params.difficulty() == 1 && params.shortcuts() == 0 && !params.obstacles() && !params.blueIce()
                && params.checkpointEvery() == 60, "the options reach the generator: " + params);
        ServerPlayer player = walker(f, 10.5, 10.5, 10);
        check(RoadTrials.enter(player, trialNumber()), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    IceTrack track = trackOf(trial);
                    check(trial.layout().timeLimitTicks() == 1800, "the time limit: " + trial.layout().timeLimitTicks());
                    check(track.length() >= 300 && track.length() < 560 && track.loops().isEmpty() && track.narrowest() >= 5.0, "a short course without shortcuts: " + track.length());
                    check(track.features().stream().noneMatch(feature -> feature.kind() != IceTrack.FeatureKind.LINE && feature.kind() != IceTrack.FeatureKind.SNOW), "no snow walls and no blue ice (the snow strips before hairpins stay)");
                    check(trial.layout().checkpoints().size() >= 3, "and checkpoints: " + trial.layout().checkpoints().size());
                })
                .thenSucceed();
    }

    private RoadBoatTests() {
    }
}
