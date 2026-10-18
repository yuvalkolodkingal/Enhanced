package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SpeedSurfaces;
import com.kolodkingal.enhanced.ability.SurfaceRun;
import com.kolodkingal.enhanced.network.SurfaceRunPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The speedsters on walls and ceilings (3.3, H1): the rules of {@link SurfaceRun}, run on a real server player in a
 * built course and moved with the game's own collision ({@code Player.move}), the way his client moves him: up a wall
 * and over its top, onto a ceiling and round the lip of an overhang, along a ceiling and down the far wall, a drop when
 * he slows down, and never a tick inside a block. Then the server's side ({@link SpeedSurfaces}): whose word it takes,
 * the flags everyone sees, the kick counter and the fall.
 *
 * <p>A course is built of stone in the arena (the arena's own barrier ceiling is out of the way, 9 blocks up); each test
 * builds its own and has a batch of its own, so no other test's blocks are near.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SpeedsterSurfaceTests {
    /** The run speed the simulated client asks for (blocks per tick) and the minimum below which he drops. */
    private static final double SPEED = 0.8;
    private static final double MIN = 0.22;
    /** A west-facing wall's yaw: running east is yaw -90 (EAST), west is +90. */
    private static final float WEST = 90.0F;

    // ------------------------------------------------------------------------------------------------------------
    // The simulated client

    /** What a run did: the events in order, the ticks, and whether it ever had him inside a block. */
    private static final class Trace {
        final List<SurfaceRun.Event> events = new ArrayList<>();
        final List<SurfaceRun.Kind> kinds = new ArrayList<>();
        int ticks;
        String inside;

        boolean saw(SurfaceRun.Event event) {
            return events.contains(event);
        }

        int index(SurfaceRun.Event event) {
            return events.indexOf(event);
        }
    }

    /** The keys of a tick of the simulated client: W held or not, and running at super speed or not. */
    private interface Keys {
        float forward(int tick);
    }

    /**
     * Runs the client's movement for {@code ticks} ticks (or until {@code until} says so): each tick the step of the rule,
     * or, when it leaves him to the game, a plain run along his facing on the ground (W at the run speed) and a fall in the
     * air; then the move with the game's collision, after which his box is checked against the blocks. The camera's turn
     * is made as his client makes it (30 degrees a tick).
     */
    private static Trace run(GameTestHelper helper, ServerPlayer player, SurfaceRun.Runner runner, float yaw, Keys keys, int ticks,
                             java.util.function.Predicate<Trace> until) {
        Trace trace = new Trace();
        player.setYRot(yaw);
        Vec3 last = Vec3.ZERO;
        for (int t = 1; t <= ticks; t++) {
            if (runner.turn > 0.0F) {
                float step = Math.min(30.0F, runner.turn);
                runner.turn -= step;
                player.setYRot(player.getYRot() + step);
            }
            float forward = keys.forward(t);
            SurfaceRun.Input input = new SurfaceRun.Input(forward, 0.0F, player.getYRot(), true, SPEED, MIN, true);
            Vec3 velocity = SurfaceRun.step(helper.getLevel(), player, runner, input);
            if (runner.event != SurfaceRun.Event.NONE) {
                trace.events.add(runner.event);
            }
            if (velocity == null) {
                if (player.onGround()) {
                    float rad = player.getYRot() * Mth.DEG_TO_RAD;
                    velocity = new Vec3(-Mth.sin(rad) * SPEED * forward, -0.08, Mth.cos(rad) * SPEED * forward);
                } else {
                    velocity = new Vec3(last.x * 0.91, (last.y - 0.08) * 0.98, last.z * 0.91);
                }
            }
            player.setDeltaMovement(velocity);
            player.move(MoverType.SELF, velocity);
            last = velocity;
            trace.kinds.add(runner.surface.kind());
            trace.ticks = t;
            if (!helper.getLevel().noCollision(player, player.getBoundingBox().deflate(1.0E-4)) && trace.inside == null) {
                trace.inside = "tick " + t + " at " + helper.relativeVec(player.position());
            }
            if (until.test(trace)) {
                break;
            }
        }
        return trace;
    }

    private static Keys held() {
        return t -> 1.0F;
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static Vec3 at(GameTestHelper helper, ServerPlayer player) {
        return helper.relativeVec(player.position());
    }

    // ------------------------------------------------------------------------------------------------------------
    // The rules

    @GameTest(template = ARENA, batch = "speed_surface_wall_up")
    public static void heRunsUpAWallAndOverItsTop(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A block of stone 5 high (its top at y 7) and 3 deep, its face to the west at x 12.
        fill(helper, 12, 2, 6, 14, 6, 14, Blocks.STONE);
        ServerPlayer player = f.player(8.5, FLOOR, 10.5, Power.QUICKSILVER);
        SurfaceRun.Runner runner = new SurfaceRun.Runner();
        double[] highest = {0};
        Trace trace = run(helper, player, runner, EAST, held(), 60, tr -> {
            highest[0] = Math.max(highest[0], at(helper, player).y);
            return tr.saw(SurfaceRun.Event.OVER_TOP) && player.onGround();
        });
        check(trace.inside == null, "never inside a block: " + trace.inside);
        check(trace.saw(SurfaceRun.Event.ENTER_WALL), "sprinting into the wall takes him up it: " + trace.events);
        check(trace.kinds.contains(SurfaceRun.Kind.WALL), "he ran on the wall");
        check(trace.saw(SurfaceRun.Event.OVER_TOP) && trace.index(SurfaceRun.Event.ENTER_WALL) < trace.index(SurfaceRun.Event.OVER_TOP),
                "and hopped over its top: " + trace.events);
        Vec3 end = at(helper, player);
        check(player.onGround() && Math.abs(end.y - 7.0) < 0.01 && end.x > 12.0, "he stands on top of it: " + end);
        check(trace.ticks < 30, "at speed (" + trace.ticks + " ticks for 5 blocks up)");
        check(!trace.saw(SurfaceRun.Event.DROP), "with no drop on the way: " + trace.events);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "speed_surface_overhang")
    public static void heRunsOntoACeilingAndRoundTheLipOfAnOverhang(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A wall at x 13 up to y 8, and an overhang at y 8 that sticks out west from it to x 8 (its lip), its top at y 9.
        fill(helper, 13, 2, 6, 14, 8, 14, Blocks.STONE);
        fill(helper, 8, 8, 6, 12, 8, 14, Blocks.STONE);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, Power.TOMMY);
        SurfaceRun.Runner runner = new SurfaceRun.Runner();
        Trace trace = run(helper, player, runner, EAST, held(), 120,
                tr -> tr.saw(SurfaceRun.Event.ROUND_LIP) && tr.saw(SurfaceRun.Event.OVER_TOP) && player.onGround());
        check(trace.inside == null, "never inside a block: " + trace.inside);
        check(trace.saw(SurfaceRun.Event.ENTER_WALL) && trace.saw(SurfaceRun.Event.ONTO_CEILING), "up the wall and onto the ceiling: " + trace.events);
        check(trace.kinds.contains(SurfaceRun.Kind.CEILING), "he ran along the ceiling upside down");
        check(trace.saw(SurfaceRun.Event.ROUND_LIP), "round the lip of the overhang and up its outer face: " + trace.events);
        Vec3 end = at(helper, player);
        check(player.onGround() && Math.abs(end.y - 9.0) < 0.01 && end.x > 8.0 && end.x < 13.0, "and he stands on top of the overhang: " + end);
        check(!trace.saw(SurfaceRun.Event.DROP), "with no drop on the way: " + trace.events);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "speed_surface_tunnel")
    public static void heRunsAlongACeilingAndDownTheFarWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A hall: walls at x 14 and x 4, a ceiling at y 7 between them.
        fill(helper, 14, 2, 6, 15, 7, 14, Blocks.STONE);
        fill(helper, 3, 2, 6, 4, 7, 14, Blocks.STONE);
        fill(helper, 5, 7, 6, 13, 7, 14, Blocks.STONE);
        ServerPlayer player = f.player(11.5, FLOOR, 10.5, Power.QUICKSILVER);
        SurfaceRun.Runner runner = new SurfaceRun.Runner();
        Trace trace = run(helper, player, runner, EAST, held(), 120, tr -> tr.saw(SurfaceRun.Event.TO_GROUND));
        check(trace.inside == null, "never inside a block: " + trace.inside);
        check(trace.saw(SurfaceRun.Event.ONTO_CEILING), "up the near wall onto the ceiling: " + trace.events);
        check(trace.saw(SurfaceRun.Event.DOWN_WALL), "along it to the far wall, and down that: " + trace.events);
        check(trace.saw(SurfaceRun.Event.TO_GROUND), "to the floor: " + trace.events);
        Vec3 end = at(helper, player);
        check(player.onGround() && Math.abs(end.y - FLOOR) < 0.01 && end.x < 6.0, "he is on the floor by the far wall: " + end);
        check(!trace.saw(SurfaceRun.Event.DROP), "with no drop on the way: " + trace.events);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "speed_surface_slow")
    public static void slowingDownDropsHimOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A tall wall (to y 9) he will not reach the top of.
        fill(helper, 12, 2, 6, 13, 9, 14, Blocks.STONE);
        ServerPlayer player = f.player(9.5, FLOOR, 10.5, Power.QUICKSILVER);
        SurfaceRun.Runner runner = new SurfaceRun.Runner();
        int[] letGo = {-1};
        // W is held until he is a few ticks up the wall, then let go.
        Trace trace = run(helper, player, runner, EAST, t -> letGo[0] < 0 || t < letGo[0] ? 1.0F : 0.0F, 80, tr -> {
            if (letGo[0] < 0 && runner.surface.kind() == SurfaceRun.Kind.WALL && runner.age >= 3) {
                letGo[0] = tr.ticks + 1;
            }
            return tr.saw(SurfaceRun.Event.DROP) && player.onGround();
        });
        check(trace.inside == null, "never inside a block: " + trace.inside);
        check(letGo[0] > 0 && trace.saw(SurfaceRun.Event.DROP), "letting go of W, he slows and drops off: " + trace.events);
        check(!trace.saw(SurfaceRun.Event.OVER_TOP) && !trace.saw(SurfaceRun.Event.ONTO_CEILING), "he never reached the top");
        int drop = trace.ticks;
        check(trace.kinds.indexOf(SurfaceRun.Kind.NONE) >= 0 && player.onGround(), "and falls to the floor (" + drop + " ticks)");
        check(at(helper, player).x < 12.0, "on his side of the wall: " + at(helper, player));

        // Nor does the rule take him up a wall when he is not running at super speed.
        SurfaceRun.Runner walker = new SurfaceRun.Runner();
        player.horizontalCollision = true;
        Vec3 none = SurfaceRun.step(helper.getLevel(), player, walker,
                new SurfaceRun.Input(1.0F, 0.0F, EAST, false, SPEED, MIN, true));
        check(none == null && !walker.surface.touches(), "a plain sprint into a wall is only a sprint");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "speed_surface_tight")
    public static void tightPlacesNeverPutHimInsideABlock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // A corridor two blocks high (its ceiling at y 4) ending in a wall at x 14, with a slab and a stair in the way, and a
        // corner at its end.
        fill(helper, 5, 4, 8, 14, 4, 12, Blocks.STONE);
        fill(helper, 14, 2, 8, 15, 3, 12, Blocks.STONE);
        fill(helper, 5, 2, 12, 13, 3, 12, Blocks.STONE);
        helper.setBlock(new BlockPos(9, 2, 10), Blocks.SMOOTH_STONE_SLAB);
        helper.setBlock(new BlockPos(11, 2, 9), Blocks.STONE_STAIRS);
        for (float yaw : new float[]{EAST, EAST + 30.0F, EAST - 35.0F}) {
            ServerPlayer player = f.player(6.5, FLOOR, 10.5, Power.QUICKSILVER);
            SurfaceRun.Runner runner = new SurfaceRun.Runner();
            Trace trace = run(helper, player, runner, yaw, held(), 60, tr -> false);
            check(trace.inside == null, "running at yaw " + yaw + " never put him inside a block: " + trace.inside + " " + trace.events);
            check(at(helper, player).y < 4.0, "he is still in the corridor: " + at(helper, player));
        }
        // And the box that is checked is the standing one: a run on a surface never changes his size.
        ServerPlayer player = f.player(6.5, FLOOR, 10.5, Power.QUICKSILVER);
        check(Math.abs(player.getBbHeight() - 1.8F) < 1.0E-4, "the hitbox stays upright and whole");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "speed_surface_leap")
    public static void spaceLeapsOffAWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        fill(helper, 12, 2, 6, 13, 9, 14, Blocks.STONE);
        ServerPlayer player = f.player(9.5, FLOOR, 10.5, Power.TOMMY);
        SurfaceRun.Runner runner = new SurfaceRun.Runner();
        run(helper, player, runner, EAST, held(), 40, tr -> runner.surface.kind() == SurfaceRun.Kind.WALL && runner.age >= 2);
        check(runner.surface.kind() == SurfaceRun.Kind.WALL && runner.surface.normal() == Direction.WEST, "on the wall facing west: " + runner.surface);
        Vec3 leap = SurfaceRun.leap(runner);
        check(leap != null && leap.x < -0.3 && leap.y > 0.3, "Space leaps him off, away from the wall and up: " + leap);
        check(!runner.surface.touches(), "and he runs on nothing then");
        check(SurfaceRun.leap(runner) == null, "a leap from nothing is nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The server

    private static void report(ServerPlayer player, SurfaceRun.Surface surface) {
        int kind = surface.kind() == SurfaceRun.Kind.WALL ? SurfaceRunPayload.WALL
                : surface.kind() == SurfaceRun.Kind.CEILING ? SurfaceRunPayload.CEILING : SurfaceRunPayload.NONE;
        SpeedSurfaces.onReport(player, kind, surface.normal().get3DDataValue());
    }

    @GameTest(template = ARENA, batch = "speed_surface_server")
    public static void theServerBelievesOnlyARunItCanCheck(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        fill(helper, 12, 2, 8, 12, 8, 12, Blocks.STONE);
        // Quicksilver against the wall (his east side touches it), in the air.
        ServerPlayer peter = f.player(11.7, 4.0, 10.5, Power.QUICKSILVER);
        PowerData data = Powers.get(peter);
        check(data.wallRun(), "Wall & Water Run is on from the start");
        report(peter, SurfaceRun.Surface.wall(Direction.WEST));
        PowerTicker.tick(peter);
        check(data.is(Live.SURFACE_WALL) && !data.is(Live.SURFACE_CEILING), "his client's word, with a wall right there, is believed");
        check(peter.fallDistance == 0.0F && SpeedSurfaces.fallSafe(peter), "a fall does not count");
        peter.connection.aboveGroundTickCount = 70;
        PowerTicker.tick(peter);
        check(peter.connection.aboveGroundTickCount == 0, "the fly-kick counter is reset while he runs on it");

        // A ceiling where there is none, or a wall on the wrong side, is not believed.
        report(peter, SurfaceRun.Surface.CEILING);
        PowerTicker.tick(peter);
        check(!data.is(Live.SURFACE_WALL) && !data.is(Live.SURFACE_CEILING), "no ceiling over him: no flag");
        report(peter, SurfaceRun.Surface.wall(Direction.EAST));
        PowerTicker.tick(peter);
        check(!data.is(Live.SURFACE_WALL), "no wall to his west: no flag");

        // With the toggle off he runs on nothing.
        report(peter, SurfaceRun.Surface.wall(Direction.WEST));
        AbilityHandler.onKey(peter, 4, true);
        check(!data.wallRun(), "B turns Wall & Water Run off");
        PowerTicker.tick(peter);
        check(!data.is(Live.SURFACE_WALL), "and then his word is not believed");
        AbilityHandler.onKey(peter, 4, true);

        // Tommy: on unless he turns it off (Sneak + R).
        ServerPlayer tommy = f.player(11.7, 4.0, 9.5, Power.TOMMY);
        PowerData tommyData = Powers.get(tommy);
        check(!tommyData.toggle(Toggle.TOMMY_WALL_RUN_OFF) && SpeedSurfaces.toggleOn(tommyData), "Tommy's Wall Run is on from the start");
        report(tommy, SurfaceRun.Surface.wall(Direction.WEST));
        PowerTicker.tick(tommy);
        check(tommyData.is(Live.SURFACE_WALL), "Tommy runs on the wall too");
        tommy.setShiftKeyDown(true);
        AbilityHandler.onKey(tommy, 1, true);
        tommy.setShiftKeyDown(false);
        check(tommyData.toggle(Toggle.TOMMY_WALL_RUN_OFF) && !tommyData.toggle(Toggle.TOMMY_TIME), "Sneak + R turns his Wall Run off (and not his slow time)");
        PowerTicker.tick(tommy);
        check(!tommyData.is(Live.SURFACE_WALL), "and the flag goes");

        // Someone who is no speedster is never heard.
        ServerPlayer thor = f.player(11.7, 4.0, 11.5, Power.THOR);
        report(thor, SurfaceRun.Surface.wall(Direction.WEST));
        check(!SpeedSurfaces.fallSafe(thor), "a Thor's word changes nothing");

        // The word goes stale: twelve ticks after the last one, the flag is down.
        report(peter, SurfaceRun.Surface.wall(Direction.WEST));
        PowerTicker.tick(peter);
        check(data.is(Live.SURFACE_WALL), "believed again");
        f.everyTick(SpeedSurfaces.REPORT_TICKS + 3, tick -> {
            PowerTicker.tick(peter);
            if (tick <= SpeedSurfaces.REPORT_TICKS - 1) {
                return false;
            }
            return !data.is(Live.SURFACE_WALL);
        });
    }

    @GameTest(template = ARENA, batch = "speed_surface_fall")
    public static void droppingOffNeverHurts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        fill(helper, 12, 2, 8, 12, 9, 12, Blocks.STONE);
        ServerPlayer peter = f.player(11.7, 8.0, 10.5, Power.QUICKSILVER);
        report(peter, SurfaceRun.Surface.wall(Direction.WEST));
        PowerTicker.tick(peter);
        check(Powers.get(peter).is(Live.SURFACE_WALL), "on the wall");
        // He drops off: his client says so, and he falls a long way (more than a tick's reset would catch).
        report(peter, SurfaceRun.Surface.NONE);
        PowerTicker.tick(peter);
        check(SpeedSurfaces.fallSafe(peter), "until he lands, a fall does not count");
        float health = peter.getHealth();
        peter.causeFallDamage(20.0F, 1.0F, peter.damageSources().fall());
        check(peter.getHealth() == health, "a fall of 20 blocks after the run does not hurt: " + peter.getHealth());
        // Once he has landed, falls count again.
        peter.setOnGround(true);
        PowerTicker.tick(peter);
        check(!SpeedSurfaces.fallSafe(peter), "landed: the protection is over");
        TestKit.clearSpawnInvulnerability(peter);
        peter.causeFallDamage(20.0F, 1.0F, peter.damageSources().fall());
        check(peter.getHealth() < health, "a later fall hurts as ever: " + peter.getHealth());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "speed_surface_speed")
    public static void theSpeedOnASurfaceComesFromHisRun(GameTestHelper helper) {
        check(Math.abs(SurfaceRun.targetSpeed(0.13, 1.0, 3.0) - 0.28) < 0.01, "a vanilla sprint is about 0.28 blocks a tick");
        check(SurfaceRun.targetSpeed(0.52, 0.8, 1.0) > 0.85 && SurfaceRun.targetSpeed(0.52, 0.8, 1.0) <= 1.0, "Quicksilver's run, four fifths, under the cap");
        check(SurfaceRun.targetSpeed(5.0, 0.8, 1.0) == 1.0, "never over the cap");
        check(SurfaceRun.wish(1.0F, 0.0F, EAST).x > 0.99, "W at yaw -90 points east");
        check(SurfaceRun.wish(0.0F, 0.0F, 0.0F) == Vec3.ZERO, "no key, no wish");
        helper.succeed();
    }

    private SpeedsterSurfaceTests() {
    }
}
