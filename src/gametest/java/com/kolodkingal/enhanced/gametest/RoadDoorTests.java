package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.WitchesRoadDoorBlock;
import com.kolodkingal.enhanced.network.OpenRoadPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Witches' Road door with the trials on (3.2): the Gene and the ritual are as in 3.1, but the door asks to walk the Road, not to
 * choose; the choice comes only to someone who has walked it. (With the trials off the door is the 3.1 door: {@code BillyTests}.)
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadDoorTests {
    static final String BATCH = "road_door";

    private static BlockPos placeDoor(GameTestHelper helper, int x, int z) {
        BlockPos rel = new BlockPos(x, 2, z);
        helper.setBlock(rel, ModBlocks.WITCHES_ROAD_DOOR.get());
        return helper.absolutePos(rel);
    }

    private static void use(ServerPlayer player, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, WitchesRoad.DOOR_HEIGHT, 0), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static boolean isOpen(ServerLevel level, BlockPos door) {
        BlockState state = level.getBlockState(door);
        return WitchesRoad.isDoor(state) && state.getValue(WitchesRoadDoorBlock.OPEN);
    }

    /** The "Seekest thou the Road?" screens the server has sent the player since the last call. */
    private static List<OpenRoadPayload> screens(ServerPlayer player) {
        return TestKit.sentPackets(player).stream().filter(p -> p instanceof ClientboundCustomPayloadPacket packet && packet.payload() instanceof OpenRoadPayload)
                .map(p -> (OpenRoadPayload) ((ClientboundCustomPayloadPacket) p).payload()).toList();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aSeekerWithAGeneIsAskedToWalkTheRoadAndTheChoiceCannotBeSkipped(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos door = placeDoor(helper, 10, 10);
        ServerPlayer seeker = RoadTestSupport.walker(f, 10.5, 12.5);
        TestKit.sentPackets(seeker);
        use(seeker, door);
        helper.startSequence()
                .thenIdle(WitchesRoad.RITUAL_TICKS + 2)
                .thenExecute(() -> {
                    check(isOpen(level, door), "the ring of light has run and the door opens");
                    List<OpenRoadPayload> asked = screens(seeker);
                    check(asked.size() == 1 && asked.get(0).trials() && asked.get(0).done() == 0 && asked.get(0).total() == RoadChallenges.count() && asked.get(0).door().equals(door),
                            "the door asks him to walk the Road: " + asked);
                    check(!WitchesRoad.choose(seeker, door, Power.BILLY) && !WitchesRoad.choose(seeker, door, Power.TOMMY), "he cannot skip the trials by choosing");
                    check(!Powers.power(seeker).hasPowers() && seeker.getMainHandItem().getCount() == 1, "no power, and the Gene is still his");
                    ServerPlayer far = RoadTestSupport.walker(f, 10.5, 19.5);
                    check(!WitchesRoad.enter(far, door), "nine blocks from the door is too far");
                    check(WitchesRoad.enter(seeker, door), "he answers \"Walk the Road\"");
                    check(RoadTrials.isInside(seeker), "the trial is being built");
                })
                .thenWaitUntil(RoadTestSupport.active(seeker))
                .thenExecute(() -> {
                    check(RoadDimension.in(seeker), "he is in the Road");
                    check(seeker.getMainHandItem().getCount() == 1, "with his Gene");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void whoHasWalkedTheRoadIsOfferedTheChoiceAtTheDoor(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = helper.getLevel();
        BlockPos door = placeDoor(helper, 10, 10);
        ServerPlayer seeker = RoadTestSupport.walker(f, 10.5, 12.5);
        RoadTrials.setCompleted(server, seeker.getUUID(), RoadChallenges.count());
        TestKit.sentPackets(seeker);
        use(seeker, door);
        helper.startSequence()
                .thenIdle(WitchesRoad.RITUAL_TICKS + 2)
                .thenExecute(() -> {
                    check(isOpen(level, door), "the door opens");
                    List<OpenRoadPayload> asked = screens(seeker);
                    check(asked.size() == 1 && !asked.get(0).trials() && asked.get(0).done() == RoadChallenges.count(), "it asks him to choose: " + asked);
                    check(!WitchesRoad.enter(seeker, door) && !RoadTrials.isInside(seeker), "there is nothing left to walk");
                    check(WitchesRoad.choose(seeker, door, Power.BILLY) && Powers.is(seeker, Power.BILLY), "he chooses Billy's road");
                    check(seeker.getMainHandItem().isEmpty(), "and the Gene is used up");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theDoorTurnsAwayWhoCannotWalkTheRoad(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos door = placeDoor(helper, 10, 10);
        ServerPlayer creative = RoadTestSupport.walker(f, 10.5, 12.5);
        creative.setGameMode(GameType.CREATIVE);
        use(creative, door);
        helper.startSequence()
                .thenIdle(WitchesRoad.RITUAL_TICKS + 5)
                .thenExecute(() -> {
                    check(!isOpen(level, door), "the door stays shut for a Creative player");
                    check(!RoadTrials.isInside(creative) && creative.getMainHandItem().getCount() == 1, "and keeps his Gene");
                })
                .thenSucceed();
    }

    private RoadDoorTests() {
    }
}
