package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SpiderMath;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.SwingAssist;
import com.kolodkingal.enhanced.network.SpiderFxPayload;
import com.kolodkingal.enhanced.network.SpiderSwingPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;

/**
 * Web Assist from the floor of a forest (3.3.1): under a canopy where nothing is clear to swing from, the assist leaps him
 * up through a gap in it ({@link SwingAssist#climb}) and the next web swings him from above the trees; a step of the
 * forest floor in front of him does not stop the first swing; the server takes the leap and keeps the key that comes
 * with it quiet, and refuses a leap from high up or into a closed column; and a swing the assist flies (a point of the
 * sky, an anchor pulled to his way) is not cut short by the server's check of the web's slack, even with its copy of him
 * a few ticks behind.
 */
public final class SwingAssistForestTests {
    private static final String BATCH = "spider_assist_forest";
    private static final SwingAssist.Params PARAMS = new SwingAssist.Params(30.0, 0.06, 1.8);

    private static ServerPlayer spider(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z, Power.SPIDER_MAN);
        PowerTicker.tick(player);
        return player;
    }

    private static AABB inside(GameTestHelper helper) {
        return new AABB(helper.absoluteVec(new Vec3(0.01, 0.0, 0.01)), helper.absoluteVec(new Vec3(20.99, 10.99, 20.99)));
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static void openTheSky(GameTestHelper helper, TestKit.Fixture f) {
        fill(helper, -1, 11, -1, 21, 11, 21, Blocks.AIR);
        f.onClose(() -> fill(helper, -1, 11, -1, 21, 11, 21, Blocks.BARRIER));
    }

    /**
     * A forest canopy: leaves from four to six blocks over the floor (relative y 6 to 8) over the arena, trunks under it,
     * and one gap two blocks wide open to the sky, ahead of a hero who starts at x 10.5, z 2.5.
     */
    private static void canopyWithAGap(GameTestHelper helper) {
        fill(helper, 1, 6, 1, 19, 8, 19, Blocks.OAK_LEAVES);
        for (int[] trunk : new int[][]{{4, 4}, {16, 4}, {6, 12}, {14, 12}, {10, 16}}) {
            fill(helper, trunk[0], 2, trunk[1], trunk[0], 8, trunk[1], Blocks.OAK_LOG);
        }
        fill(helper, 10, 6, 6, 11, 8, 7, Blocks.AIR);
    }

    private static Vec3 runningSouth(ServerPlayer player) {
        look(player, 0.0F, -20.0F);
        return new Vec3(0, 0, 0.35);
    }

    private static SwingAssist.Anchor choose(GameTestHelper helper, ServerPlayer player, Vec3 at, Vec3 velocity, SwingAssist.OpenLand openLand) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.onGround() || at.equals(player.position())
                ? SwingAssist.hop(velocity, at, at.add(look.scale(10.0)), look, 0.35, 0.3) : velocity;
        return SwingAssist.choose(helper.getLevel(), player, at, look, start, PARAMS, openLand, inside(helper));
    }

    private static List<SpiderFxPayload> fx(ServerPlayer player) {
        List<SpiderFxPayload> found = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            collect(packet, found);
        }
        return found;
    }

    private static void collect(Object packet, List<SpiderFxPayload> found) {
        if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SpiderFxPayload effect) {
            found.add(effect);
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> inner : bundle.subPackets()) {
                collect(inner, found);
            }
        }
    }

    /**
     * Flies a climb the way his client moves him: the dash along the floor to the foot of the gap, then the leap up it
     * (the game's gravity, the kept speed between webs) to its top. Returns his feet there.
     */
    private static Vec3 flyTheClimb(GameTestHelper helper, ServerPlayer player, Vec3 feet, SwingAssist.Anchor climb) {
        ServerLevel level = helper.getLevel();
        Vec3 spot = climb.pivot();
        int dash = 0;
        while (Math.hypot(spot.x - feet.x, spot.z - feet.z) > 0.2) {
            Vec3 to = new Vec3(spot.x - feet.x, 0, spot.z - feet.z);
            feet = feet.add(to.scale(Math.min(SwingAssist.CLIMB_DASH, to.length()) / to.length()));
            AABB body = new AABB(feet.x - 0.29, feet.y + 0.01, feet.z - 0.29, feet.x + 0.29, feet.y + 1.79, feet.z + 0.29);
            check(level.noCollision(player, body), "the dash to the gap touches no trunk, at " + feet);
            check(++dash <= SwingAssist.CLIMB_DASH_TICKS, "the dash reaches the gap in time");
        }
        Vec3 v = new Vec3(0, SwingAssist.leapUp(climb), 0);
        for (int tick = 0; tick < 60 && v.y > 0.0; tick++) {
            feet = feet.add(v);
            AABB body = new AABB(feet.x - 0.29, feet.y + 0.01, feet.z - 0.29, feet.x + 0.29, feet.y + 1.79, feet.z + 0.29);
            check(level.noCollision(player, body), "the leap touches no leaf nor trunk, tick " + tick + " at " + feet);
            v = new Vec3(v.x * SwingAssist.LEAP_DRAG, (v.y - SwingAssist.LEAP_GRAVITY) * SwingAssist.LEAP_DRAG, v.z * SwingAssist.LEAP_DRAG);
        }
        return feet;
    }

    // ------------------------------------------------------------------------------------------------------------

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void fromAForestFloorHeLeapsUpThroughAGapAndSwingsOverTheTrees(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        canopyWithAGap(helper);
        ServerPlayer player = spider(f, 10.5, 2.5);
        Vec3 velocity = runningSouth(player);
        // Whatever the assist takes from under the canopy is safe: a swing that flies clear, or the climb.
        SwingAssist.Anchor first = choose(helper, player, player.position(), velocity, SwingAssist.OpenLand.SKY);
        check(first != null, "under the canopy the assist has a way out");
        if (first.kind() == SwingAssist.Kind.BLOCK) {
            Vec3 look = player.getLookAngle();
            Vec3 hop = SwingAssist.hop(velocity, player.position(), player.position().add(look.scale(10.0)), look, 0.35, 0.3);
            check(SwingAssist.fly(helper.getLevel(), player, player.position(), hop, first.pivot(), first.rope(), PARAMS).clear(), "a swing flies clear");
        }
        SwingAssist.Anchor climb = SwingAssist.climb(helper.getLevel(), player, player.position(), new Vec3(0, 0, 1), inside(helper));
        check(climb != null && climb.kind() == SwingAssist.Kind.CLIMB, "under the canopy: a climb up through the gap: " + climb);
        BlockPos column = BlockPos.containing(climb.point());
        BlockPos gap = helper.absolutePos(new BlockPos(10, 6, 6));
        check(column.getX() >= gap.getX() && column.getX() <= gap.getX() + 1 && column.getZ() >= gap.getZ() && column.getZ() <= gap.getZ() + 1,
                "the gap ahead: " + column + " " + gap);
        check(Math.abs(climb.pivot().y - player.getY()) < 1.0E-6, "the leap begins on the floor, at the foot of the gap: " + climb.pivot());
        Vec3 top = flyTheClimb(helper, player, player.position(), climb);
        double canopy = helper.absoluteVec(new Vec3(0, 9.0, 0)).y;
        check(top.y >= canopy + 0.9, "at the top of the leap his feet are over the canopy: " + top.y + " over " + canopy);
        // From there (falling, as the chain sends the next web) the next web swings him over the trees, never into them.
        SwingAssist.Anchor next = choose(helper, player, top, new Vec3(0, -0.05, 0.3), SwingAssist.OpenLand.SKY);
        check(next != null && (next.kind() == SwingAssist.Kind.SKY || next.kind() == SwingAssist.Kind.BLOCK), "the next web: " + next);
        SwingAssist.Flight flight = SwingAssist.fly(helper.getLevel(), player, top, new Vec3(0, -0.05, 0.3), next.pivot(), next.rope(), PARAMS);
        check(flight.clear(), "that swing flies clear of the canopy: " + flight);
        // With no gap there is no climb.
        fill(helper, 10, 6, 6, 11, 8, 7, Blocks.OAK_LEAVES);
        check(SwingAssist.climb(helper.getLevel(), player, player.position(), new Vec3(0, 0, 1), inside(helper)) == null,
                "a closed canopy has no gap to climb through");
        // Nor high in the air.
        check(SwingAssist.climb(helper.getLevel(), player, player.position().add(0, 2.5, 0), new Vec3(0, 0, 1), inside(helper)) == null,
                "no climb away from the floor");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void inTheOpenThereIsNoLeap(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer player = spider(f, 10.5, 1.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor sky = choose(helper, player, player.position(), velocity, SwingAssist.OpenLand.SKY);
        check(sky != null && sky.kind() == SwingAssist.Kind.SKY, "flat land: still the sky: " + sky);
        check(SwingAssist.climb(helper.getLevel(), player, player.position(), new Vec3(0, 0, 1), inside(helper)) == null, "nothing over him: no leap");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aStepOfTheForestFloorDoesNotStopTheFirstSwing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A tall tree ahead, and a step of the floor right in front of his feet.
        fill(helper, 11, 7, 11, 15, 9, 15, Blocks.OAK_LEAVES);
        fill(helper, 13, 2, 13, 13, 9, 13, Blocks.OAK_LOG);
        fill(helper, 7, 2, 4, 13, 2, 4, Blocks.GRASS_BLOCK);
        ServerPlayer player = spider(f, 10.5, 3.69);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor anchor = choose(helper, player, player.position(), velocity, SwingAssist.OpenLand.NONE);
        check(anchor != null && anchor.kind() == SwingAssist.Kind.BLOCK, "the tall tree is still taken over the step: " + anchor);
        // A trunk right in front of him is no step: that swing snags.
        fill(helper, 7, 2, 4, 13, 2, 4, Blocks.AIR);
        fill(helper, 10, 2, 4, 10, 5, 4, Blocks.OAK_LOG);
        Vec3 look = player.getLookAngle();
        Vec3 start = SwingAssist.hop(velocity, player.position(), player.position().add(look.scale(10.0)), look, 0.35, 0.3);
        SwingAssist.Flight flight = SwingAssist.fly(helper.getLevel(), player, player.position(), start, anchor.pivot(), anchor.rope(), PARAMS);
        check(!flight.clear(), "a trunk in front of him still snags: " + flight);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theServerTakesTheLeapAndTheKeyWithItStaysQuiet(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        canopyWithAGap(helper);
        ServerPlayer player = spider(f, 10.5, 2.5);
        runningSouth(player);
        SwingAssist.Anchor climb = SwingAssist.climb(helper.getLevel(), player, player.position(), new Vec3(0, 0, 1), inside(helper));
        check(climb != null && climb.kind() == SwingAssist.Kind.CLIMB, "a climb: " + climb);
        fx(player);
        SpiderServer.onAssist(player, SpiderSwingPayload.CLIMB, climb.point(), 0, 0.0F, false);
        long now = helper.getLevel().getGameTime();
        check(!Powers.get(player).isReady(2, now), "the leap starts a short cooldown");
        List<SpiderFxPayload> told = fx(player);
        check(told.stream().anyMatch(p -> p.kind() == SpiderFxPayload.Kind.SHOT && p.to().equals(climb.point())), "a web shoots up the gap: " + told);
        // The ability key that came with Sneak + G is neither a swing nor a zip, and says nothing.
        player.setShiftKeyDown(true);
        AbilityHandler.onKey(player, 2, true);
        player.setShiftKeyDown(false);
        check(!Powers.get(player).is(Live.SWINGING) && !Powers.get(player).is(Live.ZIPPING), "no swing, no zip");
        check(fx(player).isEmpty(), "nothing is refused either");
        AbilityHandler.onKey(player, 2, false);

        // Refused: high in the air, or into a column closed by the leaves.
        Powers.get(player).startCooldown(2, now, 0);
        player.teleportTo(player.getX(), player.getY() + 3.5, player.getZ());
        SpiderServer.onAssist(player, SpiderSwingPayload.CLIMB, climb.point(), 0, 0.0F, false);
        List<SpiderFxPayload> high = fx(player);
        check(high.size() == 1 && high.get(0).kind() == SpiderFxPayload.Kind.SWING_END && high.get(0).arg() == 2, "not from high up: " + high);
        player.teleportTo(player.getX(), player.getY() - 3.5, player.getZ());
        Vec3 closed = helper.absoluteVec(new Vec3(14.5, 11.0, 6.5));
        SpiderServer.onAssist(player, SpiderSwingPayload.CLIMB, closed, 0, 0.0F, false);
        List<SpiderFxPayload> shut = fx(player);
        check(shut.size() == 1 && shut.get(0).arg() == 2, "not into the leaves: " + shut);
        check(Powers.get(player).isReady(2, helper.getLevel().getGameTime()), "a refused leap costs no cooldown");
        helper.succeed();
    }

    /**
     * The server's copy of him follows the swing the assist flies, {@code lag} ticks behind; the swing must still be on
     * when the assist lets go of it.
     */
    private static void followTheSwing(GameTestHelper helper, ServerPlayer player, SwingAssist.Anchor anchor, Vec3 velocity, int lag, String what) {
        ServerLevel level = helper.getLevel();
        Vec3 look = player.getLookAngle();
        Vec3 v = SwingAssist.hop(velocity, player.position(), player.position().add(look.scale(10.0)), look, 0.35, 0.3);
        Vec3 centre = player.position().add(0, SwingAssist.CENTRE, 0);
        double rope = anchor.rope();
        List<Vec3> path = new ArrayList<>();
        int released = -1;
        for (int tick = 1; tick <= 60; tick++) {
            if (SwingAssist.releaseNow(anchor.pivot(), centre, v, tick)) {
                released = tick;
                break;
            }
            rope = SwingAssist.reel(level, anchor.pivot(), rope, centre, v);
            SpiderMath.Swing step = SpiderMath.swingStep(centre, v, anchor.pivot(), rope, SwingAssist.pump(v, centre, anchor.pivot(), 1, 1.8),
                    0.06, SwingAssist.DRAG, 1.8);
            centre = step.position();
            v = step.velocity();
            path.add(centre.subtract(0, SwingAssist.CENTRE, 0));
        }
        check(released > 0, what + ": the assist lets go of it");
        for (int tick = 0; tick < path.size(); tick++) {
            Vec3 at = path.get(Math.max(0, tick - lag));
            player.moveTo(at.x, at.y, at.z);
            player.setOnGround(false);
            PowerTicker.tick(player);
            check(Powers.get(player).is(Live.SWINGING), what + ": the server keeps the swing on, tick " + tick + " of " + path.size() + " (lag " + lag + ")");
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theServerDoesNotCutAnAssistedSwingShort(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        for (int lag : new int[]{0, 3}) {
            ServerPlayer player = spider(f, 10.5, 1.5);
            Vec3 velocity = runningSouth(player);
            SwingAssist.Anchor sky = choose(helper, player, player.position(), velocity, SwingAssist.OpenLand.SKY);
            check(sky != null && sky.kind() == SwingAssist.Kind.SKY, "a sky anchor: " + sky);
            SpiderServer.onAssist(player, SpiderSwingPayload.ATTACH, sky.point(), sky.face().get3DDataValue(),
                    (float) sky.web(player.position().add(0, SwingAssist.CENTRE, 0)), true);
            check(Powers.get(player).is(Live.SWINGING), "taken");
            followTheSwing(helper, player, sky, velocity, lag, "a point of the sky");
            AbilityHandler.onKey(player, 2, false);
            player.discard();
        }
        // An anchor on a wall beside his way, swung about the point pulled to his way.
        fill(helper, 3, 2, 0, 4, 9, 20, Blocks.STONE_BRICKS);
        for (int lag : new int[]{0, 3}) {
            ServerPlayer player = spider(f, 8.0, 1.5);
            Vec3 velocity = runningSouth(player);
            SwingAssist.Anchor wall = choose(helper, player, player.position(), velocity, SwingAssist.OpenLand.NONE);
            check(wall != null && wall.kind() == SwingAssist.Kind.BLOCK, "the wall: " + wall);
            SpiderServer.onAssist(player, SpiderSwingPayload.ATTACH, wall.point(), wall.face().get3DDataValue(),
                    (float) wall.web(player.position().add(0, SwingAssist.CENTRE, 0)), false);
            check(Powers.get(player).is(Live.SWINGING), "taken");
            followTheSwing(helper, player, wall, velocity, lag, "a wall beside him");
            AbilityHandler.onKey(player, 2, false);
            player.discard();
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void aChainedSwingIsNotTakenForAJumpWhileTheServerCatchesUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer player = spider(f, 10.5, 1.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor sky = choose(helper, player, player.position(), velocity, SwingAssist.OpenLand.SKY);
        check(sky != null && sky.kind() == SwingAssist.Kind.SKY, "a sky anchor: " + sky);
        SpiderServer.onAssist(player, SpiderSwingPayload.ATTACH, sky.point(), sky.face().get3DDataValue(),
                (float) sky.web(player.position().add(0, SwingAssist.CENTRE, 0)), true);
        check(Powers.get(player).is(Live.SWINGING), "taken");
        // The next position from his client is 4.5 blocks on (he was already flying fast when the web went out).
        player.moveTo(player.getX(), player.getY() + 1.0, player.getZ() + 4.4);
        PowerTicker.tick(player);
        check(Powers.get(player).is(Live.SWINGING), "a fresh assisted swing lets the server's copy of him catch up");
        // Later, a leap of 20 blocks in one tick is a jump (a teleport) as ever.
        for (int i = 0; i < 6; i++) {
            PowerTicker.tick(player);
        }
        player.moveTo(player.getX(), player.getY(), player.getZ() + 20.0);
        PowerTicker.tick(player);
        check(!Powers.get(player).is(Live.SWINGING), "a jump later in the swing still ends it");
        helper.succeed();
    }

    private SwingAssistForestTests() {
    }
}
