package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** The options of the Road: what each of them changes. Each test has a batch of its own, as every test that changes the config must. */
public final class RoadConfigTests {

    @EnhancedTest(template = ARENA, batch = "road_off", timeoutTicks = 200)
    public static void withTheTrialsOffTheDoorGivesTheInstantChoiceAndNobodyEntersTheRoad(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.ENABLED, false);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.doorMode(player) == RoadTrials.DoorMode.CHOOSE, "the door offers the choice at once");
        check("message.enhanced.road.off".equals(RoadTrials.refusal(player)) && !RoadTrials.enter(player), "and nobody walks the Road");
        check(!RoadTrials.isInside(player) && RoadSave.get(server).existing(player.getUUID()) == null, "nothing is built or saved");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "road_full", timeoutTicks = 500)
    public static void aRoadWithNoRoomMakesTheNextWait(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.MAX_PLAYERS, 1);
        ServerPlayer first = RoadTestSupport.walker(f, 8.5, 8.5);
        ServerPlayer second = RoadTestSupport.walker(f, 12.5, 12.5);
        check(RoadTrials.enter(first), "the first walks the Road");
        check("message.enhanced.road.full".equals(RoadTrials.refusal(second)) && !RoadTrials.enter(second), "the second is told to wait");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(first))
                .thenExecute(() -> {
                    RoadTrials.leave(first, RoadChallenge.End.LEFT);
                    check(RoadTrials.refusal(second) == null && RoadTrials.enter(second), "when the first has left the second walks");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = "road_timed", timeoutTicks = 500)
    public static void aTimedParkourPutsThePlayerBackAtTheStartWhenTheTimeRunsOut(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.PARKOUR_TIME_LIMIT_SECONDS, 2);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.layout().timeLimitTicks() == 40, "two seconds: " + trial.layout().timeLimitTicks());
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial.layout().checkpoint(1).zone()));
                })
                .thenExecuteAfter(5, () -> check(RoadTestSupport.trial(player).checkpoint() == 1, "he is at checkpoint 1"))
                .thenWaitUntil(() -> check(RoadTestSupport.trial(player).checkpoint() == 0, "the time runs out and he is back at the start"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(player.position().distanceTo(trial.layout().start()) < 0.01, "on the start platform");
                    check(RoadSave.get(player.server).progress(player.getUUID()).checkpoint() == 0, "and the save agrees");
                    // The clock starts again.
                    check(RoadDimension.in(player) && trial.elapsed() < 20, "a new clock: " + trial.elapsed());
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = "road_resume_off", timeoutTicks = 500)
    public static void withResumeOffALoginInTheRoadSendsThePlayerHome(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.RESUME_ON_LOGIN, false);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        Vec3 home = player.position();
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(RoadTestSupport.trial(player).layout().checkpoint(2).zone()));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrials.onLogout(player);
                    RoadTrials.reconcile(player);
                    check(!RoadDimension.in(player) && !RoadTrials.isInside(player), "logging in he is in the world");
                    check(player.position().distanceTo(home) < 0.01, "where he came from: " + player.position() + " in " + player.level().dimension().location() + " vs " + home);
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    check(!progress.inside() && progress.completed() == 0 && progress.checkpoint() == 2, "his progress is kept");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = "road_restart", timeoutTicks = 500)
    public static void withResumeAtCheckpointOffATrialStartsFromItsBeginning(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.RESUME_AT_CHECKPOINT, false);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(RoadTestSupport.trial(player).layout().checkpoint(2).zone())))
                .thenExecuteAfter(2, () -> {
                    check(RoadTestSupport.trial(player).checkpoint() == 2, "checkpoint 2");
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(RoadTrials.enter(player), "he walks again");
                })
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 0 && player.position().distanceTo(trial.layout().start()) < 0.01, "he starts at the start");
                })
                .thenSucceed();
    }

    private RoadConfigTests() {
    }
}
