package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SpiderMath;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.SwingAssist;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.network.SpiderFxPayload;
import com.kolodkingal.enhanced.network.SpiderSwingPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
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
 * Web Assist (3.3, requirement G): the anchor it chooses in a forest, on flat land and in a street of a city, that a
 * swing it chooses never snags on a block nor comes down to the ground, its reeling, release and steering, and how the
 * server checks what a client's assist chose ({@link SpiderServer#onAssist}). The choice is the pure
 * {@link SwingAssist#choose}, the same code the client runs; it is kept to the test's own arena by its bounds, under the
 * arena's barrier ceiling (at relative y = 11; the flat land takes the ceiling away, so that the sky is open).
 */
public final class SwingAssistTests {
    private static final String BATCH = "spider_assist";
    /** The numbers of a plain swing: the default range of the assist, the swing's gravity and top speed. */
    private static final SwingAssist.Params PARAMS = new SwingAssist.Params(30.0, 0.06, 1.8);

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static ServerPlayer spider(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z, Power.SPIDER_MAN);
        PowerTicker.tick(player);
        return player;
    }

    /** The arena's air inside its barriers: an anchor must be in it. */
    private static AABB inside(GameTestHelper helper) {
        Vec3 low = helper.absoluteVec(new Vec3(0.01, 0.0, 0.01));
        Vec3 high = helper.absoluteVec(new Vec3(20.99, 10.99, 20.99));
        return new AABB(low, high);
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

    /** A tree: a trunk from the floor up to {@code top}, and a square of leaves of the radius from {@code leafFrom} up. */
    private static void tree(GameTestHelper helper, int x, int z, int top, int leafFrom, int radius) {
        fill(helper, x - radius, leafFrom, z - radius, x + radius, top, z + radius, Blocks.OAK_LEAVES);
        fill(helper, x, 2, z, x, top, z, Blocks.OAK_LOG);
    }

    /** Takes the barrier ceiling off the arena (the sky is open), and puts it back when the test ends. */
    private static void openTheSky(GameTestHelper helper, TestKit.Fixture f) {
        fill(helper, -1, 11, -1, 21, 11, 21, Blocks.AIR);
        f.onClose(() -> fill(helper, -1, 11, -1, 21, 11, 21, Blocks.BARRIER));
    }

    /** He stands at the relative x, z facing +z (south), looking 20 degrees up, running. */
    private static Vec3 runningSouth(ServerPlayer player) {
        look(player, 0.0F, -20.0F);
        return new Vec3(0, 0, 0.35);
    }

    private static SwingAssist.Anchor choose(GameTestHelper helper, ServerPlayer player, Vec3 velocity, SwingAssist.OpenLand openLand) {
        Vec3 look = player.getLookAngle();
        Vec3 start = SwingAssist.hop(velocity, player.position(), player.position().add(look.scale(10.0)), look, 0.35, 0.3);
        return SwingAssist.choose(helper.getLevel(), player, player.position(), look, start, PARAMS, openLand, inside(helper));
    }

    private static BlockPos holder(SwingAssist.Anchor anchor) {
        Direction face = anchor.face();
        return BlockPos.containing(anchor.point().subtract(face.getStepX() * 0.1, face.getStepY() * 0.1, face.getStepZ() * 0.1));
    }

    /**
     * Flies the assisted swing on {@code anchor} the way the client does (reel, push, pendulum, release) and checks every
     * tick that his body touches no block and stays clear of the ground; returns the tick it let go (or -1).
     */
    private static int flySafely(GameTestHelper helper, ServerPlayer player, Vec3 velocity, SwingAssist.Anchor anchor, String where) {
        ServerLevel level = helper.getLevel();
        Vec3 look = player.getLookAngle();
        Vec3 v = SwingAssist.hop(velocity, player.position(), player.position().add(look.scale(10.0)), look, 0.35, 0.3);
        Vec3 pivot = anchor.pivot();
        Vec3 centre = player.position().add(0, SwingAssist.CENTRE, 0);
        double rope = anchor.rope();
        for (int tick = 1; tick <= 60; tick++) {
            if (SwingAssist.releaseNow(pivot, centre, v, tick)) {
                return tick;
            }
            rope = SwingAssist.reel(level, pivot, rope, centre, v);
            check(rope >= SwingAssist.MIN_ROPE - 1.0E-9, where + ": the rope is never reeled in under its shortest");
            SpiderMath.Swing step = SpiderMath.swingStep(centre, v, pivot, rope, SwingAssist.pump(v, centre, pivot, 0, 1.8),
                    0.06, SwingAssist.DRAG, 1.8);
            centre = step.position();
            v = step.velocity();
            double feet = centre.y - SwingAssist.CENTRE;
            AABB body = new AABB(centre.x - 0.29, feet + 0.01, centre.z - 0.29, centre.x + 0.29, feet + 1.79, centre.z + 0.29);
            check(level.noCollision(player, body), where + ": his body touches no block, tick " + tick + " at " + centre);
            double ground = SwingAssist.groundBelow(level, centre);
            check(centre.y - ground >= 0.9, where + ": he never comes down to the ground, tick " + tick + ": " + (centre.y - ground));
        }
        return -1;
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

    private static String message(List<Object> packets) {
        String key = null;
        for (Object packet : packets) {
            if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && chat.content().getContents() instanceof TranslatableContents t) {
                key = t.getKey();
            }
        }
        return key;
    }

    private static SwingAssist.Anchor anchor(Vec3 point, Direction face, SwingAssist.Kind kind) {
        return new SwingAssist.Anchor(point, face, kind, point, 8.0, 0.0);
    }

    private static void attach(ServerPlayer player, SwingAssist.Anchor anchor) {
        double web = anchor.web(player.position().add(0, SwingAssist.CENTRE, 0));
        SpiderServer.onAssist(player, SpiderSwingPayload.ATTACH, anchor.point(), anchor.face().get3DDataValue(), (float) web,
                anchor.kind() == SwingAssist.Kind.SKY);
    }

    /** A street: two rows of houses nine blocks high along z, with their faces at x = 5 and x = 17. */
    private static void street(GameTestHelper helper) {
        fill(helper, 3, 2, 0, 4, 9, 20, Blocks.STONE_BRICKS);
        fill(helper, 17, 2, 0, 18, 9, 20, Blocks.STONE_BRICKS);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Choosing the anchor

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void inAForestTheTallTreeAheadIsTaken(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A low oak (its leaves four blocks up: too low to swing under) and a tall tree further on, beside his way.
        tree(helper, 5, 8, 5, 4, 1);
        tree(helper, 13, 13, 9, 7, 2);
        ServerPlayer player = spider(f, 9.5, 2.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor anchor = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        check(anchor != null && anchor.kind() == SwingAssist.Kind.BLOCK, "a forest has an anchor: " + anchor);
        BlockPos block = holder(anchor);
        BlockPos tall = helper.absolutePos(new BlockPos(13, 8, 13));
        check(block.getY() >= tall.getY() - 1 && Math.abs(block.getX() - tall.getX()) <= 2 && Math.abs(block.getZ() - tall.getZ()) <= 2,
                "the tall tree's leaves or trunk, not the low oak: " + block + " " + anchor);
        check(helper.getLevel().getBlockState(block).is(Blocks.OAK_LEAVES) || helper.getLevel().getBlockState(block).is(Blocks.OAK_LOG), "leaves and logs count");
        check(anchor.point().y >= player.getEyeY() + SwingAssist.MIN_RISE, "above his eyes");
        check(anchor.point().z > player.getZ() + 1.0, "ahead of him");
        flySafely(helper, player, velocity, anchor, "the forest");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void underALowCanopyNothingSnags(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A canopy of leaves four blocks over the floor, over the whole way ahead: whatever is taken flies clear.
        fill(helper, 2, 6, 4, 18, 9, 18, Blocks.OAK_LEAVES);
        ServerPlayer player = spider(f, 10.5, 2.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor anchor = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        if (anchor != null) {
            flySafely(helper, player, velocity, anchor, "under the canopy");
        }
        // A trunk where the swing would go: that swing is flown through and found to snag.
        fill(helper, 10, 2, 9, 11, 5, 9, Blocks.OAK_LOG);
        Vec3 start = SwingAssist.hop(velocity, player.position(), player.position().add(0, 0, 10), player.getLookAngle(), 0.35, 0.3);
        Vec3 over = helper.absoluteVec(new Vec3(10.5, 5.99, 7.0));
        SwingAssist.Flight flight = SwingAssist.fly(helper.getLevel(), player, player.position(), start, over, 4.0, PARAMS);
        check(!flight.clear(), "a swing into a trunk snags: " + flight);
        SwingAssist.Anchor now = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        if (now != null) {
            flySafely(helper, player, velocity, now, "beside the trunk");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void inAStreetAWallAheadIsTaken(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        street(helper);
        ServerPlayer player = spider(f, 11.0, 2.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor anchor = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        check(anchor != null && anchor.kind() == SwingAssist.Kind.BLOCK, "a street has an anchor: " + anchor);
        check(anchor.face() == Direction.EAST || anchor.face() == Direction.WEST, "on the face of a house along the street: " + anchor);
        check(anchor.point().z > player.getZ() + 1.0 && anchor.point().y >= player.getEyeY() + SwingAssist.MIN_RISE, "ahead and above: " + anchor);
        flySafely(helper, player, velocity, anchor, "the street");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aTreeBehindHimOrBelowHisEyesIsNoAnchor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        tree(helper, 10, 5, 9, 7, 2);
        fill(helper, 8, 2, 16, 12, 3, 18, Blocks.STONE);
        ServerPlayer player = spider(f, 10.5, 11.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor anchor = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        check(anchor == null, "a tree behind him and a low wall ahead: nothing: " + anchor);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onFlatLandTheSkyHoldsTheWeb(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer player = spider(f, 10.5, 1.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor sky = choose(helper, player, velocity, SwingAssist.OpenLand.SKY);
        check(sky != null && sky.kind() == SwingAssist.Kind.SKY, "flat land: a web to the sky: " + sky);
        check(sky.point().y >= player.getEyeY() + 8.0 && sky.point().z > player.getZ() + 3.0, "ahead and high above: " + sky);
        check(SwingAssist.openSky(helper.getLevel(), player, player.getEyePosition(), sky.point()), "with the sky open over it");
        flySafely(helper, player, velocity, sky, "the plain");
        SwingAssist.Anchor boost = choose(helper, player, velocity, SwingAssist.OpenLand.ZIP);
        check(boost != null && boost.kind() == SwingAssist.Kind.BOOST && boost.point().z > player.getZ(), "or a web boost ahead: " + boost);
        check(choose(helper, player, velocity, SwingAssist.OpenLand.NONE) == null, "or nothing");
        // Under a roof there is no sky: the roof itself is what holds the web.
        fill(helper, 0, 9, 0, 20, 9, 20, Blocks.GLASS);
        SwingAssist.Anchor roofed = choose(helper, player, velocity, SwingAssist.OpenLand.SKY);
        check(roofed == null || roofed.kind() == SwingAssist.Kind.BLOCK, "no sky under a roof: " + roofed);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theCrosshairWinsAmongGoodAnchors(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        street(helper);
        ServerPlayer player = spider(f, 11.0, 2.5);
        Vec3 velocity = runningSouth(player);
        // He looks at the right-hand houses (x = 5 is to his west... east: the face at x = 17 looks west).
        TestKit.lookAt(player, helper.absoluteVec(new Vec3(17.0, 8.5, 12.0)));
        SwingAssist.Anchor right = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        TestKit.lookAt(player, helper.absoluteVec(new Vec3(5.0, 8.5, 12.0)));
        SwingAssist.Anchor left = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        check(right != null && right.face() == Direction.WEST, "aimed at the houses at x 17: their face: " + right);
        check(left != null && left.face() == Direction.EAST, "aimed at the houses at x 5: theirs: " + left);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // While he swings

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRopeIsReeledInAboveTheGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 anchor = helper.absoluteVec(new Vec3(10.5, 9.0, 10.5));
        Vec3 centre = helper.absoluteVec(new Vec3(10.5, 6.0, 4.5));
        double rope = 12.0;
        int ticks = 0;
        while (rope > 9.0 - 2.0 - SwingAssist.CLEARANCE + 1.0E-6 && ticks < 100) {
            double next = SwingAssist.reel(level, anchor, rope, centre, Vec3.ZERO);
            check(rope - next <= SwingAssist.REEL + 1.0E-9 && next < rope, "reeled in by " + SwingAssist.REEL + " a tick at most: " + rope + " to " + next);
            rope = next;
            ticks++;
        }
        check(Math.abs(anchor.y - rope - (helper.absoluteVec(new Vec3(0, FLOOR, 0)).y + SwingAssist.CLEARANCE)) < 1.0E-6,
                "until the bottom of the arc is " + SwingAssist.CLEARANCE + " over the floor: " + rope);
        check(SwingAssist.reel(level, anchor, rope, centre, Vec3.ZERO) == rope, "and no further");
        check(SwingAssist.reel(level, anchor, 4.0, centre, Vec3.ZERO) == 4.0, "a short rope is left alone");
        // An anchor that is too low for any swing is reeled to the shortest rope, never under it.
        Vec3 low = helper.absoluteVec(new Vec3(10.5, 5.0, 10.5));
        check(SwingAssist.reel(level, low, 3.1, centre, Vec3.ZERO) == SwingAssist.MIN_ROPE, "never under the shortest rope");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theReleaseComesPastTheBottomAndTheSteeringIsGentle(GameTestHelper helper) {
        Vec3 anchor = new Vec3(0, 20, 0);
        check(!SwingAssist.releaseNow(anchor, new Vec3(0, 10, -5), new Vec3(0, -0.5, 0.8), 20), "on the way down: no");
        check(!SwingAssist.releaseNow(anchor, new Vec3(0, 10, 1), new Vec3(0, 0.2, 0.9), 20), "just past the bottom: no");
        check(SwingAssist.releaseNow(anchor, new Vec3(0, 12, 7), new Vec3(0, 0.5, 0.7), 20), "well past it, rising: yes");
        check(!SwingAssist.releaseNow(anchor, new Vec3(0, 12, 7), new Vec3(0, 0.5, 0.7), 3), "not in its first ticks");
        check(SwingAssist.releaseNow(anchor, new Vec3(0, 19.5, 2), new Vec3(0, 0.1, 0.4), 20), "up level with the anchor: yes");
        Vec3 velocity = new Vec3(1.0, -0.2, 0.0);
        Vec3 turned = SwingAssist.steer(velocity, new Vec3(0, 0, 1), 3.0);
        double angle = Math.toDegrees(Math.atan2(turned.z, turned.x));
        check(Math.abs(angle - 3.0) < 1.0E-6, "turned by 3 degrees towards the camera: " + angle);
        check(Math.abs(Math.hypot(turned.x, turned.z) - 1.0) < 1.0E-9 && turned.y == -0.2, "its speed kept");
        check(SwingAssist.steer(velocity, new Vec3(1, 0, 0), 3.0).equals(velocity), "already that way: unchanged");
        Vec3 push = SwingAssist.pump(new Vec3(0.3, 0, 0), new Vec3(0, 10, 0), anchor, 0, 1.8);
        check(push.x > 0.0 && push.y == 0.0, "a slow swing is pushed along: " + push);
        check(SwingAssist.pump(new Vec3(1.5, 0, 0), new Vec3(0, 10, 0), anchor, 0, 1.8).equals(Vec3.ZERO), "a fast one is not");
        check(SwingAssist.pump(new Vec3(1.5, 0, 0), new Vec3(0, 10, 0), anchor, -1, 1.8).x < 0.0, "S brakes");
        Vec3 heading = SwingAssist.heading(new Vec3(0, -1, 0), Vec3.ZERO, 0.0F);
        check(heading.z > 0.99, "looking straight down, his heading is his yaw: " + heading);
        check(SwingAssist.OpenLand.of("zip") == SwingAssist.OpenLand.ZIP && SwingAssist.OpenLand.of("none") == SwingAssist.OpenLand.NONE
                && SwingAssist.OpenLand.of("sky") == SwingAssist.OpenLand.SKY && SwingAssist.OpenLand.of("?") == SwingAssist.OpenLand.SKY, "the options");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The server checks it

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theServerTakesAnAssistedSwingAndItsRelease(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        street(helper);
        ServerPlayer player = spider(f, 11.0, 2.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor anchor = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        check(anchor != null, "an anchor");
        fx(player);
        attach(player, anchor);
        check(Powers.get(player).is(Live.SWINGING), "the server takes it");
        check(SpiderServer.swingAnchor(player).equals(anchor.point()), "on the anchor his client chose");
        double web = anchor.web(player.position().add(0, SwingAssist.CENTRE, 0));
        check(Math.abs(SpiderServer.swingRope(player) - web) < 1.0E-3, "with the web's length: " + SpiderServer.swingRope(player));
        List<SpiderFxPayload> told = fx(player);
        check(told.size() == 1 && told.get(0).kind() == SpiderFxPayload.Kind.SWING_START && told.get(0).to().equals(anchor.point()),
                "everyone is told, as for a manual swing: " + told);
        // The ability key that follows finds the swing on, and leaves it.
        player.setShiftKeyDown(true);
        AbilityHandler.onKey(player, 2, true);
        player.setShiftKeyDown(false);
        check(SpiderServer.swingAnchor(player).equals(anchor.point()), "the key after it changes nothing");
        PowerTicker.tick(player);
        check(Powers.get(player).is(Live.SWINGING), "it goes on");
        SpiderServer.onAssist(player, SpiderSwingPayload.RELEASE, Vec3.ZERO, 0, 0.0F, false);
        check(!Powers.get(player).is(Live.SWINGING), "the assist lets go");
        List<SpiderFxPayload> end = fx(player);
        check(!end.isEmpty() && end.get(end.size() - 1).kind() == SpiderFxPayload.Kind.SWING_END && end.get(end.size() - 1).arg() == 5,
                "as 'released': " + end);
        AbilityHandler.onKey(player, 2, false);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theServerRefusesABadAnchor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        street(helper);
        ServerPlayer player = spider(f, 11.0, 2.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor good = choose(helper, player, velocity, SwingAssist.OpenLand.NONE);
        check(good != null, "an anchor");
        List<SwingAssist.Anchor> bad = List.of(
                // in the air, where no block holds it
                anchor(helper.absoluteVec(new Vec3(11.0, 8.0, 10.0)), Direction.WEST, SwingAssist.Kind.BLOCK),
                // too far
                anchor(good.point().add(0, 0, 60), good.face(), SwingAssist.Kind.BLOCK),
                // a point of the sky under the barrier ceiling
                anchor(helper.absoluteVec(new Vec3(11.0, 10.0, 10.0)), Direction.DOWN, SwingAssist.Kind.SKY));
        for (SwingAssist.Anchor anchor : bad) {
            fx(player);
            attach(player, anchor);
            check(!Powers.get(player).is(Live.SWINGING), "refused: " + anchor);
            List<SpiderFxPayload> told = fx(player);
            check(told.size() == 1 && told.get(0).kind() == SpiderFxPayload.Kind.SWING_END && told.get(0).arg() == 2,
                    "and his client is told to stop: " + told);
        }
        // Out of sight: a wall between him and the anchor.
        fill(helper, 7, 2, 4, 15, 9, 4, Blocks.GLASS);
        attach(player, good);
        check(!Powers.get(player).is(Live.SWINGING), "refused behind a wall");
        fill(helper, 7, 2, 4, 15, 9, 4, Blocks.AIR);
        // Held by Mjolnir's kind of hold (a passenger can't act), or on a cooldown.
        Powers.get(player).startCooldown(2, helper.getLevel().getGameTime(), 40);
        attach(player, good);
        check(!Powers.get(player).is(Live.SWINGING), "refused on a cooldown");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aSkySwingNeedsNoBlockAndAZipAtNothingStaysQuiet(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer player = spider(f, 10.5, 1.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor sky = choose(helper, player, velocity, SwingAssist.OpenLand.SKY);
        check(sky != null && sky.kind() == SwingAssist.Kind.SKY, "a sky anchor: " + sky);
        TestKit.sentPackets(player);
        attach(player, sky);
        check(Powers.get(player).is(Live.SWINGING), "the server takes a sky anchor");
        // He presses G (no Sneak) at nothing in mid-air: his client made it this swing, the server says nothing of it.
        look(player, 0.0F, -80.0F);
        AbilityHandler.onKey(player, 2, true);
        String said = message(TestKit.sentPackets(player));
        check(said == null, "no 'Nothing to zip to': " + said);
        check(Powers.get(player).is(Live.SWINGING), "and the swing goes on");
        for (int tick = 0; tick < 5; tick++) {
            PowerTicker.tick(player);
        }
        check(Powers.get(player).is(Live.SWINGING), "no block is needed behind a point of the sky");
        AbilityHandler.onKey(player, 2, false);
        check(!Powers.get(player).is(Live.SWINGING), "letting go of G ends it, as ever");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_assist_skyless", timeoutTicks = 100)
    public static void theServerSaysWhichFallbackThereIs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer player = spider(f, 10.5, 1.5);
        Vec3 velocity = runningSouth(player);
        SwingAssist.Anchor sky = choose(helper, player, velocity, SwingAssist.OpenLand.SKY);
        check(sky != null, "a sky anchor");
        f.config(SpiderConfig.WEB_ASSIST_OPEN_LAND, "none");
        attach(player, sky);
        check(!Powers.get(player).is(Live.SWINGING), "no sky anchor where the server says none");
        f.config(SpiderConfig.WEB_ASSIST_OPEN_LAND, "zip");
        attach(player, sky);
        check(!Powers.get(player).is(Live.SWINGING), "nor where it says zip");
        TestKit.sentPackets(player);
        SpiderServer.onAssist(player, SpiderSwingPayload.BOOST, sky.point(), 0, 0.0F, false);
        long now = helper.getLevel().getGameTime();
        check(!Powers.get(player).isReady(2, now), "the boost starts its cooldown");
        check(fx(player).stream().anyMatch(p -> p.kind() == SpiderFxPayload.Kind.SHOT), "and a web shoots out");
        f.config(SpiderConfig.WEB_ASSIST_OPEN_LAND, "sky");
        f.config(SpiderConfig.WEB_ASSIST, false);
        Powers.get(player).startCooldown(2, now, 0);
        attach(player, sky);
        check(!Powers.get(player).is(Live.SWINGING), "a server without Web Assist takes none");
        f.config(SpiderConfig.WEB_ASSIST, true);
        attach(player, sky);
        check(Powers.get(player).is(Live.SWINGING), "with it, the same anchor is taken");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theMessageRoundTrips(GameTestHelper helper) {
        SpiderSwingPayload payload = new SpiderSwingPayload(SpiderSwingPayload.ATTACH, new Vec3(1.5, 70.25, -3.0), 2, 9.5F, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SpiderSwingPayload.STREAM_CODEC.encode(buf, payload);
        check(SpiderSwingPayload.STREAM_CODEC.decode(buf).equals(payload), "the Web Assist message");
        helper.succeed();
    }
}
