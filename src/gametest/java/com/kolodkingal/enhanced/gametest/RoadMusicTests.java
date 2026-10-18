package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.RoadMusicPayload;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.EndingHall;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadMusic;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** The music of the Witches' Road (3.3, lane WORLD): which stage plays where, and what the client is told. */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadMusicTests {

    @GameTest(template = ARENA, batch = WorldStructureTests.BATCH)
    public static void everyStageHasItsOwnLoop(GameTestHelper helper) {
        check(RoadMusic.soundOf(RoadMusic.NONE) == null, "no music off the Road");
        check(RoadMusic.soundOf(RoadMusic.ENTRANCE) == ModSounds.ROAD_MUSIC_ENTRANCE.get(), "the entrance");
        check(RoadMusic.soundOf("parkour") == ModSounds.ROAD_MUSIC_PARKOUR.get(), "the parkour");
        check(RoadMusic.soundOf(StructureCopyChallenge.ID) == ModSounds.ROAD_MUSIC_COPY.get(), "the copy of the structure");
        check(RoadMusic.soundOf(IceBoatChallenge.ID) == ModSounds.ROAD_MUSIC_BOAT.get(), "the ice-boat race");
        check(RoadMusic.soundOf(EndingHall.ID) == ModSounds.ROAD_MUSIC_ENDING.get(), "the hall at the end");
        check(RoadMusic.soundOf("a_trial_of_later") == ModSounds.ROAD_MUSIC_PARKOUR.get(), "a trial with no arrangement of its own");
        for (RoadChallenge trial : RoadChallenges.all()) {
            check(RoadMusic.soundOf(trial.id()) != null, trial.id() + " has music");
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RoadMusicPayload.STREAM_CODEC.encode(buf, new RoadMusicPayload(IceBoatChallenge.ID));
        check(RoadMusicPayload.STREAM_CODEC.decode(buf).stage().equals(IceBoatChallenge.ID), "the stage goes to the client and back");
        helper.succeed();
    }

    /** At the door the entrance plays, then the first trial's music, and nothing once he has left the Road. */
    @GameTest(template = ARENA, batch = "road_door", timeoutTicks = 400)
    public static void theMusicFollowsTheSeekerDownTheRoad(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        BlockPos rel = new BlockPos(10, 2, 10);
        helper.setBlock(rel, ModBlocks.WITCHES_ROAD_DOOR.get());
        BlockPos door = helper.absolutePos(rel);
        ServerPlayer seeker = RoadTestSupport.walker(f, 10.5, 12.5);
        check(RoadMusic.stageOf(seeker).equals(RoadMusic.NONE), "no music before he asks");
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(door).add(0, WitchesRoad.DOOR_HEIGHT, 0), Direction.UP, door, false);
        seeker.gameMode.useItemOn(seeker, seeker.serverLevel(), seeker.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        String first = RoadChallenges.get(0).id();
        helper.startSequence()
                .thenExecute(() -> check(RoadMusic.stageOf(seeker).equals(RoadMusic.ENTRANCE), "the entrance plays while the door's ritual runs"))
                .thenWaitUntil(() -> check(RoadMusic.sentTo(seeker.getUUID()).equals(RoadMusic.ENTRANCE), "and his client is told"))
                .thenIdle(WitchesRoad.RITUAL_TICKS + 2)
                .thenExecute(() -> {
                    check(RoadMusic.stageOf(seeker).equals(RoadMusic.ENTRANCE), "and while the door stands open for him");
                    check(WitchesRoad.enter(seeker, door), "he walks the Road");
                    check(RoadMusic.stageOf(seeker).equals(RoadMusic.ENTRANCE), "and on the way in");
                })
                .thenWaitUntil(RoadTestSupport.active(seeker))
                .thenExecute(() -> check(RoadMusic.stageOf(seeker).equals(first), "the first trial's music in the first trial"))
                .thenWaitUntil(() -> check(RoadMusic.sentTo(seeker.getUUID()).equals(first), "his client is told"))
                .thenExecute(() -> {
                    RoadTrials.leave(seeker, RoadChallenge.End.LEFT);
                    check(RoadMusic.stageOf(seeker).equals(RoadMusic.NONE), "no music once he has left the Road (the door has shut)");
                })
                .thenWaitUntil(() -> check(RoadMusic.sentTo(seeker.getUUID()).equals(RoadMusic.NONE), "and his client is told to stop"))
                .thenSucceed();
    }

    private RoadMusicTests() {
    }
}
