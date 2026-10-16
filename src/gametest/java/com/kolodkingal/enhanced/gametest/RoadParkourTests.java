package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadCells;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.parkour.JumpModel;
import com.kolodkingal.enhanced.road.parkour.ParkourChallenge;
import com.kolodkingal.enhanced.road.parkour.ParkourGenerator;
import com.kolodkingal.enhanced.road.parkour.ParkourGenerator.Kind;
import com.kolodkingal.enhanced.road.parkour.ParkourGenerator.Pad;
import com.kolodkingal.enhanced.road.parkour.ParkourGenerator.Params;
import com.kolodkingal.enhanced.road.parkour.ParkourGenerator.Plan;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Parkour (trial 1): the generator is pure code, so these tests run it over thousands of seeds and settings and prove,
 * without a world, that every course can be finished: no gap is wider than a running jump can cross (worked out here by a
 * second, separate simulation of the game's physics), no step is too high, the stones never run into each other, the course stays
 * inside its cell, and it gets harder.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadParkourTests {
    static final String BATCH = "road_parkour";

    /**
     * An independent simulation of a player's jump, tick by tick (written out here on purpose, not shared with the mod): how
     * far the middle of the player travels, on the flat, while his feet fall from the top of the jump back to the height {@code dy}
     * above where he left the ground.
     */
    static double referenceTravel(int dy, int runUpTicks) {
        double velocity = 0.0;
        for (int t = 0; t < runUpTicks; t++) {
            velocity = (velocity + 0.13) * 0.546;
        }
        velocity += 0.2;
        double x = 0.0;
        double y = 0.0;
        double vy = 0.42;
        double lastX = 0.0;
        double lastY = 0.0;
        for (int t = 0; t < 100; t++) {
            if (t == 0) {
                velocity += 0.13;
                x += velocity;
                velocity *= 0.546;
            } else {
                velocity += 0.026;
                x += velocity;
                velocity *= 0.91;
            }
            y += vy;
            vy = (vy - 0.08) * 0.98;
            if (t > 2 && lastY >= dy && y < dy) {
                return lastX + (lastY - dy) / (lastY - y) * (x - lastX);
            }
            lastX = x;
            lastY = y;
        }
        throw new IllegalStateException("never came down");
    }

    static List<Plan> manyCourses() {
        List<Plan> plans = new ArrayList<>();
        SplittableRandom seeds = new SplittableRandom(32);
        for (int difficulty = 1; difficulty <= 3; difficulty++) {
            for (int jumps : new int[]{8, 20, 40, 75, 150}) {
                for (int every : new int[]{2, 6, 30}) {
                    for (int n = 0; n < 40; n++) {
                        plans.add(ParkourGenerator.generate(seeds.nextLong(), new Params(jumps, difficulty, every)));
                    }
                }
            }
        }
        return plans;
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theJumpModelAgreesWithAnIndependentSimulation(GameTestHelper helper) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int runUp = 0; runUp <= 10; runUp += 2) {
                TestKit.assertNear(JumpModel.travel(dy, runUp, true), referenceTravel(dy, runUp), 1.0E-9, "travel dy " + dy + " run-up " + runUp);
            }
        }
        // What every player knows: a running jump on the flat crosses four blocks of air and not five, a step up is shorter, a step down longer.
        check(JumpModel.maxGap(0, 10) > 4.0 && JumpModel.maxGap(0, 10) < 4.5, "four blocks of air, not five: " + JumpModel.maxGap(0, 10));
        check(JumpModel.maxGap(1, 10) > 3.0 && JumpModel.maxGap(1, 10) < 3.5, "up one block: three: " + JumpModel.maxGap(1, 10));
        check(JumpModel.maxGap(-1, 10) > JumpModel.maxGap(0, 10) && JumpModel.maxGap(0, 10) > JumpModel.maxGap(1, 10), "down is longer, up is shorter");
        check(JumpModel.travel(0, 10, false) < JumpModel.travel(0, 10, true) - 1.0, "a sprint is a lot farther than a walk");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void everyJumpOfEveryCourseIsWithinWhatAPlayerCanDo(GameTestHelper helper) {
        List<Plan> plans = manyCourses();
        int jumps = 0;
        for (Plan plan : plans) {
            List<Pad> pads = plan.pads();
            for (int i = 1; i < pads.size(); i++) {
                Pad a = pads.get(i - 1);
                Pad b = pads.get(i);
                int dy = b.y() - a.y();
                check(dy >= -1 && dy <= 1, "no step is more than one block: " + dy);
                double[] air = ParkourGenerator.air(a, b);
                double gap = Math.hypot(air[0], air[1]);
                check(gap >= 1.0, "every jump has air in it: " + gap);
                // With a running start of two ticks only (the stone he stands on), less half a block to spare.
                double allowed = referenceTravel(dy, 2) + 0.6 - 0.5;
                check(gap <= allowed + 1.0E-9, "a gap of " + gap + " blocks, up/down " + dy + ", is more than a player can jump (" + allowed + ")");
                check(gap <= 3.0 + 1.0E-9, "and never more than three blocks of air: " + gap);
                if (dy == 1) {
                    check(gap <= 2.5, "a step up is never more than two blocks of air: " + gap);
                }
                jumps++;
            }
        }
        check(jumps > 100000, "a lot of jumps were checked: " + jumps);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void courseStonesNeverRunIntoEachOtherAndStayInTheCell(GameTestHelper helper) {
        for (Plan plan : manyCourses()) {
            List<Pad> pads = plan.pads();
            for (int i = 0; i < pads.size(); i++) {
                Pad a = pads.get(i);
                check(Math.abs(a.minX()) <= ParkourGenerator.BOUND && Math.abs(a.maxX()) <= ParkourGenerator.BOUND
                        && Math.abs(a.minZ()) <= ParkourGenerator.BOUND && Math.abs(a.maxZ()) <= ParkourGenerator.BOUND, "in its bounds");
                check(Math.abs(a.y()) <= ParkourGenerator.HEIGHT_BOUND, "and in height");
                check(ParkourGenerator.BOUND + 5 < RoadCells.HALF_EXTENT, "inside the cell");
                for (int j = i + 2; j < pads.size(); j++) {
                    Pad b = pads.get(j);
                    boolean overlap = a.minX() <= b.maxX() && b.minX() <= a.maxX() && a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ();
                    check(!overlap || Math.abs(a.y() - b.y()) >= 8, "stone " + i + " and " + j + " are on top of each other");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void coursesAreDeterministicAndDifferentForDifferentSeeds(GameTestHelper helper) {
        Params params = new Params(40, 2, 6);
        Plan a = ParkourGenerator.generate(1234L, params);
        Plan b = ParkourGenerator.generate(1234L, params);
        check(a.pads().equals(b.pads()) && a.checkpoints().equals(b.checkpoints()), "the same seed makes the same course");
        int different = 0;
        for (long seed = 1; seed <= 20; seed++) {
            if (!ParkourGenerator.generate(seed, params).pads().equals(a.pads())) {
                different++;
            }
        }
        check(different == 20, "other seeds make other courses: " + different + " of 20");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void aCourseHasItsCheckpointsAndFinishAndGetsHarder(GameTestHelper helper) {
        for (int every : new int[]{2, 5, 6, 9}) {
            Plan plan = ParkourGenerator.generate(99L, new Params(40, 2, every));
            List<Pad> pads = plan.pads();
            check(pads.size() == 41, "a start and 40 jumps: " + pads.size());
            check(pads.get(0).kind() == Kind.START && pads.get(40).kind() == Kind.FINISH, "from the start to the finish");
            int expected = (40 - 1) / every;
            check(plan.checkpoints().size() == expected, "a checkpoint after every " + every + " jumps: " + plan.checkpoints().size() + " vs " + expected);
            for (int n = 0; n < plan.checkpoints().size(); n++) {
                int index = plan.checkpoints().get(n);
                check(index == (n + 1) * every && pads.get(index).kind() == Kind.CHECKPOINT && pads.get(index).size() == 5, "checkpoint " + n + " is where it should be");
            }
        }
        // Harder: the stones are smaller and the gaps wider in the second half than in the first (on average over many courses).
        double firstGaps = 0;
        double lastGaps = 0;
        double firstSize = 0;
        double lastSize = 0;
        int count = 0;
        for (int n = 0; n < 60; n++) {
            Plan plan = ParkourGenerator.generate(7000L + n, new Params(60, 2, 12));
            List<Pad> pads = plan.pads();
            for (int i = 1; i < pads.size() - 1; i++) {
                if (pads.get(i).kind() != Kind.STONE) {
                    continue;
                }
                double gap = ParkourGenerator.gap(pads.get(i - 1), pads.get(i));
                if (i <= 30) {
                    firstGaps += gap;
                    firstSize += pads.get(i).size();
                } else {
                    lastGaps += gap;
                    lastSize += pads.get(i).size();
                }
            }
            count++;
        }
        check(lastGaps > firstGaps * 1.15, "the gaps grow: " + firstGaps + " then " + lastGaps);
        check(lastSize < firstSize * 0.95, "the stones shrink: " + firstSize + " then " + lastSize);
        // The settings: easy never asks for more than two blocks of air, hard asks for three early.
        for (int n = 0; n < 40; n++) {
            Plan easy = ParkourGenerator.generate(n, new Params(60, 1, 6));
            for (int i = 1; i < easy.pads().size(); i++) {
                check(ParkourGenerator.gap(easy.pads().get(i - 1), easy.pads().get(i)) <= 2.0 + 1.0E-9, "easy: at most two blocks of air");
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void aDrawnCourseHasEverythingTheFrameworkNeeds(GameTestHelper helper) {
        SplittableRandom seeds = new SplittableRandom(5);
        for (int n = 0; n < 30; n++) {
            Plan plan = ParkourGenerator.generate(seeds.nextLong(), new Params(10 + seeds.nextInt(100), 1 + seeds.nextInt(3), 3 + seeds.nextInt(8)));
            RoadCanvas canvas = new RoadCanvas(seeds.nextInt(50), 0, 320);
            RoadLayout layout = ParkourChallenge.draw(canvas, plan, 12, 0);
            int cps = plan.checkpoints().size();
            check(layout.checkpoints().size() == cps + 1, "the start and every checkpoint: " + layout.checkpoints().size());
            check(layout.finish() != null && layout.gate() != null && layout.exitDoor() != null, "a finish, a gate and an exit door");
            check(layout.timeLimitTicks() == 0, "no time limit by default");
            check(canvas.blockCount() > 100 && canvas.chunks().length >= 1, "blocks were drawn");
            for (int i = 0; i < layout.checkpoints().size(); i++) {
                RoadLayout.Checkpoint cp = layout.checkpoints().get(i);
                check(RoadCells.contains(canvas.cell(), net.minecraft.core.BlockPos.containing(cp.respawn())), "inside the cell");
                check(cp.failY() < cp.respawn().y - 8.0, "a fall is a good way below him: " + (cp.respawn().y - cp.failY()));
                if (i > 0) {
                    AABB zone = cp.zone();
                    check(zone != null && zone.contains(cp.respawn().add(0, 0.5, 0)), "he is inside the zone of his checkpoint");
                }
            }
            AABB finish = layout.finish();
            check(finish.contains(layout.gate().base().getX() + 0.5 - 3.0 * 0, finish.minY + 0.5, finish.getCenter().z) || finish.getXsize() > 5.0, "the finish is a platform");
            // The ground under the start and the exit door
            check(canvas.drawn(net.minecraft.core.BlockPos.containing(layout.start()).below()) != null, "ground under the start");
            check(canvas.drawn(layout.exitDoor()) != null, "the exit door is drawn");
            // The gate's opening is drawn sealed: glass.
            check(canvas.drawn(layout.gate().base().above()) != null, "the gate is sealed");
        }
        helper.succeed();
    }

    private RoadParkourTests() {
    }
}
