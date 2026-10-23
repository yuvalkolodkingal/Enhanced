package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.road.boat.IceBoatModel;
import com.kolodkingal.enhanced.road.boat.IceTrack;
import com.kolodkingal.enhanced.road.boat.IceTrackGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The boat on ice, checked against the game: the real {@code Boat} entity is driven tick by tick, in the Road dimension, over blocks
 * of a real course, and the pure simulation ({@link IceBoatSim}) that the tests of the course rest on has to agree with it, tick for
 * tick. Then the facts the course is built on are measured on the real boat: it does not climb a slab, it slows down on snow, and it
 * crosses a gap of five blocks only above a speed that {@link IceBoatModel#minJumpSpeed} gets right.
 */
public final class RoadBoatPhysicsTests {
    static final String BATCH = "road_boat_physics";

    private static final BlockState ICE = Blocks.PACKED_ICE.defaultBlockState();
    private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();

    /** Drives the simulation and a real boat on the same course with the same driver; fails at the first tick they disagree. */
    private static void sameAsTheRealBoat(GameTestHelper helper, IceTrack track, boolean shortcuts, double carelessness, int ticks) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        RoadBoatSupport.Cell cell = RoadBoatSupport.course(f, track);
        IceBoatSim.TrackWorld world = new IceBoatSim.TrackWorld(track.raster());
        int h = track.heading();
        double bx = track.platformX() + IceTrack.dirX(h) * IceTrackGenerator.BOAT_PARK;
        double bz = track.platformZ() + IceTrack.dirZ(h) * IceTrackGenerator.BOAT_PARK;
        float yaw = (float) Math.toDegrees(Math.atan2(-IceTrack.dirX(h), IceTrack.dirZ(h)));
        IceBoatSim sim = new IceBoatSim(world, bx, 1.0, bz, yaw);
        RealBoat real = new RealBoat(cell.road, cell.at(bx, 1.0, bz), yaw);
        f.onClose(real::discard);
        IceBot bot = new IceBot(track, shortcuts, 5, carelessness, 3);
        double[] zone = track.finishZone();
        // Tick for tick the same to a millionth of a block; a wall that the careless driver rubs along can make a rounding of 1e-6 decide on which side of a
        // face the real boat is, so there the allowance is wider.
        double tolerance = carelessness > 0.0 ? 1.0E-3 : 1.0E-5;
        double worst = 0.0;
        int hits = 0;
        double top = 0.0;
        boolean fell = false;
        for (int tick = 0; tick < ticks; tick++) {
            IceBot.Keys keys = bot.drive(sim);
            sim.step(keys.left(), keys.right(), keys.up(), keys.down());
            real.step(keys.left(), keys.right(), keys.up(), keys.down());
            double dx = real.boat.getX() - cell.origin.getX() - sim.x;
            double dy = real.boat.getY() - cell.origin.getY() - sim.y;
            double dz = real.boat.getZ() - cell.origin.getZ() - sim.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            worst = Math.max(worst, dist);
            check(dist < tolerance, "the simulated boat and the real one are in the same place in tick " + tick + ": off by " + dist + " (sim " + sim.x + " " + sim.y + " " + sim.z
                    + ", real " + (real.boat.getX() - cell.origin.getX()) + " " + (real.boat.getY() - cell.origin.getY()) + " " + (real.boat.getZ() - cell.origin.getZ()) + ")");
            if (!(Math.abs(real.velocity.x - sim.vx) < tolerance && Math.abs(real.velocity.z - sim.vz) < tolerance && Math.abs(real.velocity.y - sim.vy) < tolerance)) {
                StringBuilder around = new StringBuilder();
                int cx = (int) Math.floor(sim.x);
                int cz = (int) Math.floor(sim.z);
                for (int oz = -2; oz <= 2; oz++) {
                    for (int ox = -2; ox <= 2; ox++) {
                        around.append(String.format("[%d %d: sim top %.1f kind %d; real %s %s] ", cx + ox, cz + oz, track.raster().top(cx + ox, cz + oz), track.raster().kind(cx + ox, cz + oz),
                                cell.road.getBlockState(cell.origin.offset(cx + ox, 1, cz + oz)).getBlock().getDescriptionId().replace("block.minecraft.", ""),
                                cell.road.getBlockState(cell.origin.offset(cx + ox, 2, cz + oz)).getBlock().getDescriptionId().replace("block.minecraft.", "")));
                    }
                    around.append("\n");
                }
                check(false, "velocity differs in tick " + tick + " at the box " + (sim.x - 0.6875) + " " + (sim.x + 0.6875) + " / " + (sim.z - 0.6875) + " " + (sim.z + 0.6875) + "\n" + around);
            }
            check(Math.abs(real.velocity.x - sim.vx) < tolerance && Math.abs(real.velocity.z - sim.vz) < tolerance && Math.abs(real.velocity.y - sim.vy) < tolerance,
                    "and move at the same speed in tick " + tick + ": sim " + sim.vx + " " + sim.vy + " " + sim.vz + ", real " + real.velocity + " (collision " + sim.horizontalCollision
                            + ", at " + sim.x + " " + sim.y + " " + sim.z + ")");
            check(Math.abs(real.deltaRotation() - sim.deltaRotation) < tolerance * 10 && Math.abs(real.boat.getYRot() - sim.yaw) < tolerance * 100, "and turn the same way in tick " + tick);
            if (sim.horizontalCollision) {
                hits++;
            }
            top = Math.max(top, sim.speed());
            if (sim.y < track.lowestLevel() + 1.0 - 6.0) {
                fell = true;
                break;
            }
            if (sim.x >= zone[0] && sim.x <= zone[2] && sim.z >= zone[1] && sim.z <= zone[3]) {
                break;
            }
        }
        check(top > 1.0 && !fell, "the run was a real one: top speed " + top);
        // (the walls were met on the careless runs)
        if (carelessness > 0.0) {
            check(hits > 0, "walls were touched: " + hits);
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theSimulationAgreesWithTheRealBoatOnAWholeCourse(GameTestHelper helper) {
        sameAsTheRealBoat(helper, IceTrackGenerator.generate(41, new IceTrackGenerator.Params(600, 9, 2, 1, true, true, 100)), false, 0.0, 1500);
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theSimulationAgreesWithTheRealBoatWhenItHitsWallsAndSnow(GameTestHelper helper) {
        sameAsTheRealBoat(helper, IceTrackGenerator.generate(77, new IceTrackGenerator.Params(600, 9, 3, 1, true, true, 100)), false, 1.0, 1200);
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theSimulationAgreesWithTheRealBoatOverAShortcutAndItsGap(GameTestHelper helper) {
        // The first course with a shortcut that the driver takes.
        for (long seed = 1; seed < 60; seed++) {
            IceTrack track = IceTrackGenerator.generate(seed, new IceTrackGenerator.Params(600, 9, 2, 1, false, false, 100));
            if (!track.loops().isEmpty() && IceBot.run(track, true, 4800, 0.0, 5).crossedGap() && IceBot.run(track, true, 4800, 0.0, 5).finished()) {
                sameAsTheRealBoat(helper, track, true, 0.0, 1800);
                return;
            }
        }
        check(false, "a course with a shortcut was found");
    }

    // ---- facts ----

    /** A strip of ice 7 across along +x with the given floor from x = 0 to x = {@code length}. */
    private static void strip(RoadBoatSupport.Cell cell, int from, int to, int y, BlockState top) {
        for (int x = from; x < to; x++) {
            for (int z = -3; z <= 3; z++) {
                cell.canvas.set(x, y, z, top);
                cell.canvas.set(x, y - 1, z, ICE);
            }
        }
    }

    private static void run(RealBoat real, int ticks, boolean up) {
        for (int i = 0; i < ticks; i++) {
            real.step(false, false, up, false);
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aBoatDoesNotClimbAnythingNotEvenASlabOrALayerOfSnow(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        BlockState[] steps = {
                Blocks.STONE_SLAB.defaultBlockState(),
                Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2),
                Blocks.STONE.defaultBlockState()};
        for (BlockState step : steps) {
            RoadBoatSupport.Cell cell = RoadBoatSupport.draw(f, c -> {
                strip(c, 0, 60, 0, ICE);
                for (int z = -3; z <= 3; z++) {
                    c.canvas.set(40, 1, z, step);
                }
            });
            RealBoat real = new RealBoat(cell.road, cell.at(20.0, 1.0, 0.0), -90.0F);
            real.velocity = new Vec3(1.5, 0.0, 0.0);
            run(real, 40, true);
            double x = real.boat.getX() - cell.origin.getX();
            check(x < 40.0 - IceBoatModel.BOAT_WIDTH / 2 + 0.01, step.getBlock().getName().getString() + " stops the boat: it is at " + x);
            check(real.velocity.x == 0.0 && real.boat.getY() - cell.origin.getY() < 1.01, "it does not get on it: speed " + real.velocity.x + ", height " + (real.boat.getY() - cell.origin.getY()));
            real.discard();
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void snowTakesTheSpeedOffTheBoatButNeverStopsIt(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        RoadBoatSupport.Cell cell = RoadBoatSupport.draw(f, c -> {
            strip(c, 0, 200, 0, ICE);
            strip(c, 80, 90, 0, SNOW);
        });
        RealBoat real = new RealBoat(cell.road, cell.at(20.0, 1.0, 0.0), -90.0F);
        real.velocity = new Vec3(2.0, 0.0, 0.0);
        double before = 0.0;
        double inside = 0.0;
        double after = 0.0;
        double slowest = 9.0;
        for (int i = 0; i < 120; i++) {
            real.step(false, false, true, false);
            double x = real.boat.getX() - cell.origin.getX();
            double v = real.velocity.x;
            if (x < 79.0) {
                before = v;
            } else if (x < 89.0) {
                inside = v;
                slowest = Math.min(slowest, v);
            } else if (after == 0.0) {
                after = v;
            }
        }
        check(before > 1.9, "full speed before the snow: " + before);
        check(slowest < 0.3 && slowest > 0.05, "the snow slows the boat to " + slowest + " blocks a tick, and not to a stop");
        check(after > 0.05 && real.velocity.x > after, "and the boat gains speed again after it: " + after + " then " + real.velocity.x);
        TestKit.assertNear(IceBoatModel.terminalSpeed(0.6), 0.1, 1.0E-9, "the model knows the top speed on snow");
        real.discard();
        helper.succeed();
    }

    /** Whether a real boat sent over the gap at this speed gets across (and lands on the lower floor). */
    private static boolean crosses(TestKit.Fixture f, RoadBoatSupport.Cell cell, double speed) {
        RealBoat real = new RealBoat(cell.road, cell.at(40.0, 1.0, 0.0), -90.0F);
        real.velocity = new Vec3(speed, 0.0, 0.0);
        boolean across = false;
        for (int i = 0; i < 80; i++) {
            real.step(false, false, false, false);
            double x = real.boat.getX() - cell.origin.getX();
            if (real.boat.getY() - cell.origin.getY() < -3.0) {
                break;
            }
            if (x > 47.0 && real.boat.getY() - cell.origin.getY() < 0.5) {
                across = true;
                break;
            }
        }
        real.discard();
        return across;
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aGapOfFiveBlocksWithALowerFarSideIsCrossedByASpeedTheModelKnows(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        RoadBoatSupport.Cell cell = RoadBoatSupport.draw(f, c -> {
            strip(c, 0, 40, 0, ICE);
            // a gap from x = 40 to x = 44, the far side one block lower
            strip(c, 45, 90, -1, ICE);
        });
        check(!crosses(f, cell, 0.5) && !crosses(f, cell, 0.7), "a boat that is too slow falls into the gap");
        check(crosses(f, cell, 1.1) && crosses(f, cell, 1.6), "a fast one crosses it");
        double lo = 0.5;
        double hi = 1.6;
        for (int i = 0; i < 14; i++) {
            double mid = 0.5 * (lo + hi);
            if (crosses(f, cell, mid)) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        double needed = IceBoatModel.minJumpSpeed(IceTrackGenerator.GAP_WIDTH, IceTrackGenerator.GAP_DROP);
        check(Math.abs(hi - needed) < 0.12, "the real boat needs " + hi + " blocks a tick, the model says " + needed);
        helper.succeed();
    }

    private RoadBoatPhysicsTests() {
    }
}
