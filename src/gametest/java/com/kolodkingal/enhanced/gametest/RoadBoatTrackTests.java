package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadCells;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.boat.IceBoatModel;
import com.kolodkingal.enhanced.road.boat.IceTrack;
import com.kolodkingal.enhanced.road.boat.IceTrack.Piece;
import com.kolodkingal.enhanced.road.boat.IceTrack.Raster;
import com.kolodkingal.enhanced.road.boat.IceTrack.Sample;
import com.kolodkingal.enhanced.road.boat.IceTrackGenerator;
import com.kolodkingal.enhanced.road.boat.IceTrackGenerator.Params;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Ice-Boat Race (trial 3), the course: {@code IceTrackGenerator} is pure code, so these tests run it over many seeds and settings
 * and prove, without a world, that every course can be driven. The proof has three legs: the properties of the course (width, curves,
 * straights before them, shortcuts, checkpoints, nothing crossing, inside the cell), a second simulation of the vanilla boat
 * ({@link IceBoatSim}, written out from the game's code and itself checked against the real entity in {@code RoadBoatPhysicsTests}) and
 * a driver ({@link IceBot}) that has to finish every course on it.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadBoatTrackTests {
    static final String BATCH = "road_boat_track";

    /** The settings the tests vary: the difficulty and the length (the rest is what the config has by default). */
    static Params params(int difficulty, int length, int width, int shortcuts) {
        return new Params(length, width, difficulty, shortcuts, true, true, 100);
    }

    /** A set of courses: {@code count} seeds from {@code first}, spread over the difficulties, lengths and widths. */
    static List<IceTrack> courses(long first, int count) {
        List<IceTrack> tracks = new ArrayList<>();
        int[] lengths = {300, 600, 800, 800, 1000};
        int[] widths = {9, 9, 7, 11, 9};
        for (int i = 0; i < count; i++) {
            int difficulty = 1 + i % 3;
            tracks.add(IceTrackGenerator.generate(first + i, params(difficulty, lengths[i % lengths.length], widths[(i / 2) % widths.length], i % 4)));
        }
        return tracks;
    }

    // ---- the physics numbers ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theModelUsesTheGamesOwnNumbers(GameTestHelper helper) {
        // The friction of the blocks the course is made of is the game's, read from the blocks.
        check(Blocks.PACKED_ICE.getFriction() == (float) IceBoatModel.PACKED_ICE_FRICTION, "packed ice: " + Blocks.PACKED_ICE.getFriction());
        check(Blocks.ICE.getFriction() == (float) IceBoatModel.PACKED_ICE_FRICTION, "ice is the same");
        check(Blocks.BLUE_ICE.getFriction() == (float) IceBoatModel.BLUE_ICE_FRICTION, "blue ice: " + Blocks.BLUE_ICE.getFriction());
        check(Blocks.SNOW_BLOCK.getFriction() == (float) IceBoatModel.SNOW_FRICTION, "snow: " + Blocks.SNOW_BLOCK.getFriction());
        check(Blocks.DARK_OAK_PLANKS.getFriction() == 0.6F && Blocks.AMETHYST_BLOCK.getFriction() == 0.6F, "planks and amethyst are ordinary");
        // The top speeds that follow: 2 blocks a tick on packed ice, 3.6 on blue ice, a tenth on snow.
        TestKit.assertNear(IceBoatModel.terminalSpeed(0.98), 2.0, 1.0E-9, "40 blocks a second on packed ice");
        TestKit.assertNear(IceBoatModel.terminalSpeed(0.989), 3.636, 0.001, "72 blocks a second on blue ice");
        TestKit.assertNear(IceBoatModel.terminalSpeed(0.6), 0.1, 1.0E-9, "snow holds the boat to 2 blocks a second");
        TestKit.assertNear(IceBoatModel.maxTurnRate(0.98), 50.0, 1.0E-9, "the boat can turn 50 degrees a tick on packed ice");
        // The sine table of the game is the one the second simulation uses.
        SplittableRandom random = new SplittableRandom(7);
        for (int i = 0; i < 200000; i++) {
            float angle = (float) ((random.nextDouble() - 0.5) * 2000.0);
            check(IceBoatSim.sin(angle) == Mth.sin(angle) && IceBoatSim.cos(angle) == Mth.cos(angle), "the sine table agrees at " + angle);
        }
        // What the model says of the speed after a run, and of a jump (the course asks only for what is possible).
        check(IceBoatModel.speedAfterDistance(0.98, 20.0) > 0.9 && IceBoatModel.speedAfterDistance(0.98, 20.0) < 1.1, "20 blocks from rest is about 1 block a tick: "
                + IceBoatModel.speedAfterDistance(0.98, 20.0));
        check(IceBoatModel.speedAfterDistance(0.98, 70.0) > 1.4, "70 blocks: " + IceBoatModel.speedAfterDistance(0.98, 70.0));
        double jump = IceBoatModel.minJumpSpeed(IceTrackGenerator.GAP_WIDTH, IceTrackGenerator.GAP_DROP);
        check(jump > 0.7 && jump < 1.0, "a gap of five and a drop of one needs about 0.85 blocks a tick: " + jump);
        check(IceBoatModel.minJumpSpeed(6, 1) > jump && IceBoatModel.minJumpSpeed(5, 2) < jump, "a wider gap needs more speed, a deeper drop less");
        check(IceBoatModel.cornerSpeed(16) > 0.5 && IceBoatModel.cornerSpeed(100) > IceBoatModel.cornerSpeed(25), "tighter curves are slower");
        TestKit.assertNear(IceBoatModel.cornerRadius(IceBoatModel.cornerSpeed(30)), 30.0, 1.0E-9, "and the radius is the inverse");
        helper.succeed();
    }

    // ---- the generator ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void coursesAreTheSameForTheSameSeedAndDifferentForOthers(GameTestHelper helper) {
        Params params = params(2, 800, 9, 1);
        IceTrack a = IceTrackGenerator.generate(1234L, params);
        IceTrack b = IceTrackGenerator.generate(1234L, params);
        check(a.pieces().equals(b.pieces()) && a.main().samples().equals(b.main().samples()) && a.checkpoints().equals(b.checkpoints()), "the same seed makes the same course");
        check(a.raster().count() == b.raster().count() && a.platformX() == b.platformX() && a.platformZ() == b.platformZ(), "and the same blocks");
        int different = 0;
        for (long seed = 1; seed <= 12; seed++) {
            if (!IceTrackGenerator.generate(seed, params).pieces().equals(a.pieces())) {
                different++;
            }
        }
        check(different == 12, "other seeds make other courses: " + different + " of 12");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void everyCourseHasTheLengthAskedFor(GameTestHelper helper) {
        for (int length : new int[]{200, 400, 800, 1200, 1400}) {
            for (int difficulty = 1; difficulty <= 3; difficulty++) {
                for (long seed = 1; seed <= 4; seed++) {
                    IceTrack track = IceTrackGenerator.generate(seed * 31 + length, params(difficulty, length, 9, 1));
                    check(track.length() >= length - 1.0E-6 && track.length() <= length + 260.0, "a course of " + length + " is " + track.length() + " long (seed " + seed
                            + ", difficulty " + difficulty + ")");
                    check(track.shortest() <= track.length() && track.shortest() >= track.length() * 0.6, "and the shortest way " + track.shortest());
                }
            }
        }
        // The shortcuts the settings allow: none when 0.
        for (long seed = 1; seed <= 20; seed++) {
            check(IceTrackGenerator.generate(seed, params(2, 800, 9, 0)).loops().isEmpty(), "no shortcuts when the config says none");
            check(IceTrackGenerator.generate(seed, params(2, 800, 9, 2)).loops().size() <= 2, "and never more than allowed");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void theTrackIsNeverNarrowerThanThreeAndStaysInItsCell(GameTestHelper helper) {
        for (IceTrack track : courses(100, 36)) {
            check(track.narrowest() >= 3.0, "never narrower than three blocks: " + track.narrowest());
            check(track.narrowest() >= IceTrackGenerator.NARROW_WIDTH - 1.0E-9 || track.pieces().stream().noneMatch(p -> p.kind() == IceTrack.Kind.NARROW), "the narrow stretches are five");
            Raster r = track.raster();
            check(Math.abs(r.minX()) <= RoadCells.HALF_EXTENT && Math.abs(r.maxX()) <= RoadCells.HALF_EXTENT
                    && Math.abs(r.minZ()) <= RoadCells.HALF_EXTENT && Math.abs(r.maxZ()) <= RoadCells.HALF_EXTENT, "the grid is inside the cell");
            for (Sample sm : track.main().samples()) {
                check(Math.abs(sm.x()) + sm.hw() + IceTrackGenerator.WALL <= IceTrackGenerator.LIMIT + 1.5
                        && Math.abs(sm.z()) + sm.hw() + IceTrackGenerator.WALL <= IceTrackGenerator.LIMIT + 1.5, "the line and its walls are inside the limit: " + sm.x() + " " + sm.z());
            }
            // Across the floor, along every straight row of the line: at least three cells of floor in a row.
            for (Sample sm : track.main().samples()) {
                if (sm.turn() != 0 || sm.hw() < 1.0) {
                    continue;
                }
                int cx = (int) Math.floor(sm.x());
                int cz = (int) Math.floor(sm.z());
                int across = 0;
                int rx = (int) -Math.round(sm.dz());
                int rz = (int) Math.round(sm.dx());
                for (int l = -8; l <= 8; l++) {
                    if (r.isFloor(cx + rx * l, cz + rz * l)) {
                        across++;
                    }
                }
                check(across >= 3, "three cells of floor across the line at " + cx + " " + cz + ": " + across);
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void everyCurveIsOneTheBoatCanDriveAndStraightsComeBetween(GameTestHelper helper) {
        double turnLimit = IceBoatModel.maxTurnRate(IceBoatModel.PACKED_ICE_FRICTION);
        int[] minRadius = {0, 30, 24, 20};
        int hairpins = 0;
        for (IceTrack track : courses(200, 45)) {
            int difficulty = track.params().difficulty();
            List<Piece> pieces = track.pieces();
            for (int i = 0; i < pieces.size(); i++) {
                Piece piece = pieces.get(i);
                if (piece.radius() == 0.0) {
                    continue;
                }
                double radius = piece.radius();
                double speed = IceBoatModel.cornerSpeed(radius);
                // The boat's steering is not the limit: even at its top speed the turn is far less than it can turn in a tick.
                double needed = Math.toDegrees(IceBoatModel.terminalSpeed(IceBoatModel.PACKED_ICE_FRICTION) / radius);
                check(needed < turnLimit * 0.3, "the turn rate at top speed on a curve of " + radius + " is " + needed + " degrees a tick, the boat's limit " + turnLimit);
                // The curve is one the boat can follow at a speed that makes the course worth driving (at least 10 blocks a second).
                check(speed >= 0.5, "a curve of radius " + radius + " can be driven at " + speed * 20 + " blocks a second");
                if (piece.kind() == IceTrack.Kind.HAIRPIN) {
                    hairpins++;
                    check(radius >= 13 && radius >= (difficulty == 3 ? 13 : difficulty == 2 ? 16 : 20), "a hairpin of " + radius);
                } else if (piece.kind() == IceTrack.Kind.S_BEND) {
                    check(radius >= minRadius[difficulty] - 6, "an S-bend of " + radius);
                } else if (piece.kind() == IceTrack.Kind.TURN || piece.kind() == IceTrack.Kind.FORK) {
                    check(radius >= Math.min(minRadius[difficulty] - 2, 22), "a turn of " + radius);
                }
            }
            // Before every curve (that is not the start of a hairpin or a fork, which bring their own approach) there is a straight of at
            // least MIN_STRAIGHT, and after it.
            Piece before = null;
            for (Piece piece : pieces) {
                boolean curve = piece.kind() == IceTrack.Kind.TURN || piece.kind() == IceTrack.Kind.S_BEND || piece.kind() == IceTrack.Kind.HAIRPIN;
                if (curve && before != null) {
                    boolean straight = before.radius() == 0.0 && before.length() >= IceTrackGenerator.MIN_STRAIGHT - 1.0E-6;
                    check(straight || before.kind() == IceTrack.Kind.FORK && piece.kind() != IceTrack.Kind.HAIRPIN, "a straight of " + IceTrackGenerator.MIN_STRAIGHT
                            + " before a " + piece.kind() + " (not a " + before.kind() + " of " + before.length() + ")");
                }
                before = piece;
            }
            // A hairpin is approached on a straight with a strip of snow across all of the track near its end.
            for (int i = 1; i < pieces.size(); i++) {
                if (pieces.get(i).kind() == IceTrack.Kind.HAIRPIN) {
                    Piece approach = pieces.get(i - 1);
                    check(approach.kind() == IceTrack.Kind.BRAKE && approach.length() >= IceTrackGenerator.APPROACH - 1.0E-6, "a hairpin comes after a braking straight");
                    boolean snow = false;
                    for (IceTrack.Feature f : track.features()) {
                        if (f.kind() == IceTrack.FeatureKind.SNOW && Math.max(f.x1() - f.x0(), f.z1() - f.z0()) + 1 >= track.params().width()) {
                            snow = true;
                        }
                    }
                    check(snow, "with a strip of snow across it");
                }
            }
        }
        check(hairpins > 10, "hairpins were tested: " + hairpins);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void everyPartOfEveryCourseIsReachableAndNothingTouchesAnythingElse(GameTestHelper helper) {
        for (IceTrack track : courses(300, 30)) {
            Raster r = track.raster();
            // One piece of floor: from the start, every floor cell is reached by going from cell to cell (the gap of a shortcut is bridged by
            // the two ends of the shortcut each meeting the long way round).
            int x0 = (int) Math.floor(track.startAlongX(IceTrackGenerator.BOAT_PARK));
            int z0 = (int) Math.floor(track.startAlongZ(IceTrackGenerator.BOAT_PARK));
            check(r.isFloor(x0, z0), "the start is on the floor");
            int w = r.maxX() - r.minX() + 1;
            boolean[] seen = new boolean[w * (r.maxZ() - r.minZ() + 1)];
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{x0, z0});
            seen[(z0 - r.minZ()) * w + (x0 - r.minX())] = true;
            int reached = 0;
            int floors = 0;
            while (!queue.isEmpty()) {
                int[] c = queue.poll();
                reached++;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = c[0] + d[0];
                    int nz = c[1] + d[1];
                    if (r.has(nx, nz) && r.isFloor(nx, nz) && !seen[(nz - r.minZ()) * w + (nx - r.minX())]) {
                        seen[(nz - r.minZ()) * w + (nx - r.minX())] = true;
                        queue.add(new int[]{nx, nz});
                    }
                }
            }
            for (int z = r.minZ(); z <= r.maxZ(); z++) {
                for (int x = r.minX(); x <= r.maxX(); x++) {
                    if (r.isFloor(x, z)) {
                        floors++;
                    }
                }
            }
            check(reached == floors, "all the floor can be reached from the start: " + reached + " of " + floors);
            // The finish is on it too.
            double[] zone = track.finishZone();
            check(r.isFloor((int) Math.floor((zone[0] + zone[2]) / 2), (int) Math.floor((zone[1] + zone[3]) / 2)), "and the finish");
            // Two parts of the course that are far apart along the line are never close in the world (the walls do not touch).
            List<Sample> samples = track.main().samples();
            Map<Long, IntArrayList> grid = new HashMap<>();
            for (int i = 0; i < samples.size(); i += 2) {
                Sample sm = samples.get(i);
                grid.computeIfAbsent((((long) Math.floor(sm.x() / 16)) << 32) ^ ((long) Math.floor(sm.z() / 16) & 0xFFFFFFFFL), k -> new IntArrayList()).add(i);
            }
            for (int i = 0; i < samples.size(); i += 2) {
                Sample a = samples.get(i);
                int gx = (int) Math.floor(a.x() / 16);
                int gz = (int) Math.floor(a.z() / 16);
                for (int ox = -2; ox <= 2; ox++) {
                    for (int oz = -2; oz <= 2; oz++) {
                        IntArrayList cell = grid.get((((long) (gx + ox)) << 32) ^ ((long) (gz + oz) & 0xFFFFFFFFL));
                        if (cell == null) {
                            continue;
                        }
                        for (int j : cell) {
                            Sample b = samples.get(j);
                            // The two ends of a shortcut's long way round are near the line they leave; skip pairs that are near along the line.
                            if (Math.abs(a.s() - b.s()) < 60.0) {
                                continue;
                            }
                            double apart = Math.hypot(a.x() - b.x(), a.z() - b.z());
                            check(apart >= a.hw() + b.hw() + 2 * IceTrackGenerator.WALL + 1.0, "the line is " + apart + " from itself at " + a.s() + " and " + b.s());
                        }
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void checkpointsAreOnStraightsAndSpacedOutAndTheCanvasTakesTheCourse(GameTestHelper helper) {
        SplittableRandom random = new SplittableRandom(11);
        int total = 0;
        for (IceTrack track : courses(400, 30)) {
            int every = track.params().checkpointEvery();
            double last = track.startLine();
            check(track.checkpoints().size() >= (int) (track.length() / (every * 3.2)), "enough checkpoints: " + track.checkpoints().size() + " for " + track.length());
            for (IceTrack.Checkpoint cp : track.checkpoints()) {
                check(cp.s() > last + every * 0.4 - 1.0E-9, "checkpoints are spaced out: " + (cp.s() - last));
                check(cp.s() - last <= every * 3.2 + 10.0, "and not too far apart: " + (cp.s() - last));
                check(cp.s() < track.startLine() + track.length() - 60.0, "none near the finish");
                last = cp.s();
                check(track.raster().isFloor((int) Math.floor(cp.x()), (int) Math.floor(cp.z())), "a checkpoint is on the floor");
                check(Math.abs(cp.x() - (Math.floor(cp.x()) + 0.5)) < 1.0E-9 && Math.abs(cp.z() - (Math.floor(cp.z()) + 0.5)) < 1.0E-9, "in the middle of a block");
                total++;
            }
            RoadCanvas canvas = new RoadCanvas(random.nextInt(60), 0, 320);
            RoadLayout layout = IceBoatChallenge.draw(canvas, track, 12, 4800);
            check(layout.checkpoints().size() == track.checkpoints().size() + 2, "the start, the line after the barrier and the checkpoints: " + layout.checkpoints().size());
            check(layout.finish() == null && layout.gate() != null && layout.exitDoor() != null && layout.timeLimitTicks() == 4800, "a gate, an exit door, a time limit");
            check(canvas.blockCount() > 10000 && canvas.minY() >= RoadCells.ORIGIN_Y - 12 && canvas.maxY() <= RoadCells.ORIGIN_Y + 12, "blocks drawn, within a few blocks of the course: "
                    + canvas.blockCount() + " " + canvas.minY() + " " + canvas.maxY());
            for (int i = 0; i < layout.checkpoints().size(); i++) {
                RoadLayout.Checkpoint cp = layout.checkpoints().get(i);
                check(RoadCells.contains(canvas.cell(), net.minecraft.core.BlockPos.containing(cp.respawn())), "inside the cell");
                check(cp.failY() < cp.respawn().y - 10.0, "a fall is a good way below the boat: " + (cp.respawn().y - cp.failY()));
                if (i > 0) {
                    check(cp.zone() != null && cp.zone().contains(cp.respawn().add(0, 0.5, 0)), "the boat stands in the zone of its checkpoint");
                }
            }
        }
        check(total > 80, "checkpoints were tested: " + total);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void aShortcutIsAJumpThatTheStraightBeforeItAllowsAndTheLongWayStaysOpen(GameTestHelper helper) {
        int loops = 0;
        for (long seed = 1; seed <= 60; seed++) {
            IceTrack track = IceTrackGenerator.generate(seed, params(2, 800, 9, 2));
            Raster r = track.raster();
            for (IceTrack.Loop loop : track.loops()) {
                loops++;
                check(loop.gap() == IceTrackGenerator.GAP_WIDTH && loop.drop() == IceTrackGenerator.GAP_DROP, "a gap of five, a drop of one");
                check(loop.run() >= IceTrackGenerator.RUN_UP, "a run-up of " + loop.run());
                // From rest at the fork, the straight gives well over the speed the jump needs (a boat put back at the last checkpoint has that much).
                double needed = IceBoatModel.minJumpSpeed(loop.gap(), loop.drop());
                double got = IceBoatModel.speedAfterDistance(IceBoatModel.PACKED_ICE_FRICTION, loop.run());
                check(got >= needed * 1.5, "the run-up gives " + got + " blocks a tick, the jump needs " + needed);
                check(loop.saving() >= 60.0, "the shortcut saves " + loop.saving());
                // The cells: floor before the gap, air in it with rails beside, a floor one block lower after it.
                int dx = IceTrack.dirX(loop.heading());
                int dz = IceTrack.dirZ(loop.heading());
                int fx = (int) Math.floor(loop.forkX());
                int fz = (int) Math.floor(loop.forkZ());
                check(r.isFloor(fx + dx * (loop.gapFrom() - 1), fz + dz * (loop.gapFrom() - 1)) && r.level(fx + dx * (loop.gapFrom() - 1), fz + dz * (loop.gapFrom() - 1)) == 0,
                        "floor before the gap");
                for (int u = loop.gapFrom(); u < loop.gapFrom() + loop.gap(); u++) {
                    check(r.kind(fx + dx * u, fz + dz * u) == IceTrack.VOID, "air in the gap");
                    check(r.kind(fx + dx * u + (-dz) * 3, fz + dz * u + dx * 3) == IceTrack.WALL, "a rail beside the gap");
                }
                int after = loop.gapFrom() + loop.gap();
                check(r.isFloor(fx + dx * after, fz + dz * after) && r.level(fx + dx * after, fz + dz * after) == -loop.drop(), "floor one block lower after it");
                // The long way round: its cells are floor all the way (it is the line itself).
                for (Sample sm : track.main().samples()) {
                    check(r.isFloor((int) Math.floor(sm.x()), (int) Math.floor(sm.z())), "the long way is floor at " + sm.x() + " " + sm.z());
                }
            }
        }
        check(loops > 30, "shortcuts were tested: " + loops);
        helper.succeed();
    }

    // ---- the driver ----

    /** Drives the courses of one difficulty: the long way round, and the shortcut. */
    private static void driveAll(GameTestHelper helper, int difficulty, long first, int count) {
        int jumps = 0;
        int taken = 0;
        double ratio = 0;
        int n = 0;
        for (int i = 0; i < count; i++) {
            IceTrack track = IceTrackGenerator.generate(first + i, params(difficulty, i % 5 == 4 ? 1000 : 800, i % 3 == 2 ? 11 : 9, 2));
            int limit = 240 * 20;
            IceBot.Result longWay = IceBot.run(track, false, limit, 0.0, i);
            check(longWay.finished(), "the long way of seed " + (first + i) + " (difficulty " + difficulty + ") is driven: " + longWay);
            check(longWay.ticks() < limit / 2, "in well under the time limit: " + longWay.ticks());
            // The par is what the physics allow times the factor: a good driver beats it.
            double par = IceBoatChallenge.parSeconds(track) * 20.0;
            ratio += longWay.ticks() / par;
            n++;
            check(longWay.ticks() <= par * 1.15, "a good driver is about par: " + longWay.ticks() + " ticks against " + par);
            if (!track.loops().isEmpty()) {
                // The shortcut: the way onto it is narrow (five blocks in a track of nine), and a driver can end up against the wall of the
                // fork; nearly every driver gets over the gap, and nobody falls into it.
                IceBot.Result shortcut = IceBot.run(track, true, limit, 0.0, i);
                jumps++;
                check(!shortcut.fell(), "nobody falls off the shortcut (seed " + (first + i) + "): " + shortcut);
                if (shortcut.finished()) {
                    taken++;
                    check(shortcut.crossedGap(), "the shortcut is driven, gap and all: " + shortcut);
                    check(shortcut.ticks() < longWay.ticks(), "and is quicker: " + shortcut.ticks() + " against " + longWay.ticks());
                }
            }
        }
        check(jumps >= 4 && taken >= jumps * 0.7, "shortcuts were driven: " + taken + " of " + jumps);
        check(ratio / n > 0.55 && ratio / n < 1.0, "the par is a good time that can be beaten: " + ratio / n);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1500)
    public static void aDriverFinishesEveryEasyCourse(GameTestHelper helper) {
        driveAll(helper, 1, 1000, 24);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1500)
    public static void aDriverFinishesEveryNormalCourse(GameTestHelper helper) {
        driveAll(helper, 2, 2000, 24);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1500)
    public static void aDriverFinishesEveryHardCourse(GameTestHelper helper) {
        driveAll(helper, 3, 3000, 24);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1500)
    public static void aCarelessDriverWhoLateAndSometimesPressesTheWrongKeyStillFinishesTheLongWay(GameTestHelper helper) {
        int finished = 0;
        int runs = 0;
        for (int difficulty = 1; difficulty <= 3; difficulty++) {
            for (int i = 0; i < 16; i++) {
                IceTrack track = IceTrackGenerator.generate(5000L + difficulty * 100 + i, params(difficulty, 800, 9, 1));
                IceBot.Result result = IceBot.run(track, false, 240 * 20, 1.0, i);
                runs++;
                if (result.finished()) {
                    finished++;
                    check(result.ticks() < 240 * 20 * 0.75, "inside the time limit with room to spare: " + result.ticks());
                } else if (difficulty < 3) {
                    check(false, "a careless driver finishes at difficulty " + difficulty + ": " + result);
                }
            }
        }
        check(finished >= runs - 2, "nearly all careless runs finish: " + finished + " of " + runs);
        helper.succeed();
    }

    private RoadBoatTrackTests() {
    }
}
