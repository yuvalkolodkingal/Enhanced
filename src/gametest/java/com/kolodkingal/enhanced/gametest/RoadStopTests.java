package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** A server that stops while players are in the Road takes every trial down first (a batch of its own: it stops every trial there is). */
public final class RoadStopTests {

    @EnhancedTest(template = ARENA, batch = "road_stop", timeoutTicks = 500)
    public static void aStoppingServerTakesEveryTrialDownAndKeepsTheProgress(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer a = RoadTestSupport.walker(f, 8.5, 8.5);
        ServerPlayer b = RoadTestSupport.walker(f, 12.5, 12.5);
        check(RoadTrials.enter(a) && RoadTrials.enter(b), "two players walk the Road");
        long[][] chunks = new long[2][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(a))
                .thenWaitUntil(RoadTestSupport.active(b))
                .thenExecute(() -> {
                    RoadTrial ta = RoadTestSupport.trial(a);
                    RoadTrial tb = RoadTestSupport.trial(b);
                    chunks[0] = ta.chunks();
                    chunks[1] = tb.chunks();
                    check(RoadSave.get(server).cellCount() >= 2, "two cells in the ledger");
                    RoadTrials.onServerStopping(server);
                    check(RoadTrials.playing() == 0, "no trial is left");
                    check(!RoadSweeper.busy() && RoadTestSupport.blocksIn(RoadTestSupport.road(helper), chunks[0]) == 0
                            && RoadTestSupport.blocksIn(RoadTestSupport.road(helper), chunks[1]) == 0, "and not a block of them");
                    check(RoadSave.get(server).cell(ta.cell()) == null && RoadSave.get(server).cell(tb.cell()) == null, "their cells are out of the ledger");
                    check(RoadSave.get(server).progress(a.getUUID()).inside() && RoadSave.get(server).progress(b.getUUID()).inside(),
                            "the players are still inside as far as the save is concerned, so they are taken up again at login");
                })
                .thenSucceed();
    }

    private RoadStopTests() {
    }
}
