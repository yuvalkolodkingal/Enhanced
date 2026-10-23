package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildSession;
import com.kolodkingal.enhanced.build.GrowBrush;
import com.kolodkingal.enhanced.build.GrowStyle;
import com.kolodkingal.enhanced.build.LandBrush;
import com.kolodkingal.enhanced.build.LandStyle;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.UndoStep;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.World;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.set;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.steps;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The brushes of Build Magic (D-B 4.11, 19.7; spec 18.4 B82, B83): Land raises a hill, smooths it away and lowers the
 * ground, a pulse at a time, one stroke one undo step, and never moves a tree. Land is part of 3.1 (R46); Grow (B84,
 * B85) is phase 2.
 *
 * <p>The world tests stand on a meadow the test lays in the arena (grass on two layers of dirt over the arena's smooth
 * stone, relative x and z 2 to 18), with the builder on the bare corner. Each has a batch of its own
 * ({@code build_brushes_*}): the jobs, the strokes and the histories are server-wide, and {@code TestKit.bossFixture}
 * clears them when a test ends. The pulses are cast {@value #PULSE_GAP} ticks apart (a stroke pulses at most every
 * {@value LandBrush#PULSE_TICKS}), and the test steps each pulse's job itself.
 */
public final class BuildBrushTests {
    /** The ticks between two pulses of a test's stroke: one more than a stroke's pulses need. */
    private static final int PULSE_GAP = LandBrush.PULSE_TICKS + 1;
    /** The meadow's surface (relative y): grass, on dirt at 3 and 2, on the arena's smooth stone at 1. */
    private static final int MEADOW = 4;
    private static final int LOW = 2;
    private static final int HIGH = 18;

    // ------------------------------------------------------------------------------------------------------------
    // Pure: the columns of a pulse (D-B 4.11)

    /**
     * What a pulse moves, in a little world of blocks: a column moves whole (the grass, the dirt it leaves under it,
     * the plants and the snow riding on it), a log or a plank holds it, a rising column never copies an ore, a sinking
     * one never replaces a floor and leaves its water under water; a barrier above does not hide the ground; Smooth
     * moves a column toward the surfaces round it; the falloff and the stroke's memory decide which columns move,
     * nearest the middle first.
     */
    @EnhancedTest(template = ARENA, batch = "build_brushes_pure")
    public static void aPulseMovesWholeColumns(GameTestHelper helper) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState ore = Blocks.DIAMOND_ORE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState tuft = Blocks.SHORT_GRASS.defaultBlockState();
        BlockState tallLow = Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState tallHigh = Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        BlockPos at = new BlockPos(0, 64, 0);

        // Raise: the grass goes up, the dirt under it fills the cell it left, the tuft rides on top.
        World world = column(grass, dirt).set(0, 65, 0, tuft);
        pulse(world, LandStyle.RAISE, at, 0);
        check(world.getBlockState(at).equals(dirt) && world.getBlockState(at.above()).equals(grass) && world.getBlockState(at.above(2)).equals(tuft)
                && world.getBlockState(at.above(3)).isAir(), "the column rose whole: dirt, grass, the tuft");
        // Tall grass rides whole; a snow layer rides.
        world = column(grass, dirt).set(0, 65, 0, tallLow).set(0, 66, 0, tallHigh);
        pulse(world, LandStyle.RAISE, at, 0);
        check(world.getBlockState(at.above(2)).equals(tallLow) && world.getBlockState(at.above(3)).equals(tallHigh), "both halves of the tall grass rode up");
        world = column(grass, dirt).set(0, 65, 0, Blocks.SNOW.defaultBlockState());
        pulse(world, LandStyle.RAISE, at, 0);
        check(world.getBlockState(at.above(2)).is(Blocks.SNOW) && world.getBlockState(at.above()).equals(grass), "the snow rode up");
        // A log, a plank on top: the column holds.
        for (Block holder : List.of(Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_LEAVES)) {
            for (LandStyle style : List.of(LandStyle.RAISE, LandStyle.LOWER)) {
                world = column(grass, dirt).set(0, 65, 0, holder.defaultBlockState());
                check(pulse(world, style, at, 0).isEmpty() && world.getBlockState(at).equals(grass), holder + " holds the column: " + style);
            }
        }
        // Never an ore copied: stone over an ore rises as stone; an ore on an ore stays.
        world = column(stone, ore);
        pulse(world, LandStyle.RAISE, at, 0);
        check(world.getBlockState(at).equals(stone) && world.getBlockState(at.above()).equals(stone) && world.getBlockState(at.below()).equals(ore),
                "the cell the stone left is stone, not a second ore");
        world = column(ore, ore);
        check(pulse(world, LandStyle.RAISE, at, 0).isEmpty(), "an ore on an ore does not rise");

        // Lower: the grass sinks into the dirt, the flower rides down, the top is left empty.
        world = column(grass, dirt).set(0, 65, 0, Blocks.POPPY.defaultBlockState());
        pulse(world, LandStyle.LOWER, at, 0);
        check(world.getBlockState(at.below()).equals(grass) && world.getBlockState(at).is(Blocks.POPPY) && world.getBlockState(at.above()).isAir(),
                "the column sank whole");
        // Never into a floor; under water the cell left is water.
        world = column(grass, Blocks.OAK_PLANKS.defaultBlockState());
        check(pulse(world, LandStyle.LOWER, at, 0).isEmpty(), "a column never sinks into a floor");
        world = column(Blocks.SAND.defaultBlockState(), dirt).set(0, 65, 0, Blocks.WATER.defaultBlockState()).set(0, 66, 0, Blocks.WATER.defaultBlockState());
        pulse(world, LandStyle.LOWER, at, 0);
        check(world.getBlockState(at.below()).is(Blocks.SAND) && world.getBlockState(at).is(Blocks.WATER), "under water the cell left is water");

        // A barrier above does not hide the ground; a column of planks has no surface.
        world = column(grass, dirt).set(0, 70, 0, Blocks.BARRIER.defaultBlockState());
        check(LandBrush.surface(world, 0, 0, 64, 0) == 64, "the ground under a barrier is the surface");
        world = new World().set(0, 64, 0, Blocks.OAK_PLANKS.defaultBlockState());
        check(LandBrush.surface(world, 0, 0, 64, 0) == LandBrush.NO_SURFACE, "planks are no ground");
        world = column(grass, dirt);
        check(LandBrush.surface(world, 0, 0, 64 + 9, 0) == LandBrush.NO_SURFACE && LandBrush.surface(world, 0, 0, 64 + 8, 0) == 64,
                "the surface is looked for within r + 8 of the aimed height");

        // Smooth: a pillar two blocks above its flat neighbours comes down one a pulse, to one above, then stays.
        world = new World();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.set(x, 63, z, dirt).set(x, 64, z, grass);
            }
        }
        world.set(0, 64, 0, dirt).set(0, 65, 0, dirt).set(0, 66, 0, grass);
        LandBrush.Memory memory = new LandBrush.Memory();
        pulse(world, LandStyle.SMOOTH, at, 0, memory);
        check(world.getBlockState(at.above(2)).isAir() && world.getBlockState(at.above()).equals(grass), "the pillar came down one");
        pulse(world, LandStyle.SMOOTH, at, 0, memory);
        check(world.getBlockState(at.above()).isAir() && world.getBlockState(at).equals(grass), "and one more, to its neighbours' height");
        check(pulse(world, LandStyle.SMOOTH, at, 0, memory).isEmpty(), "level with them, it stays");

        // The falloff and the memory: with a brush of 2 the middle moves every pulse, the four next to it (0.5625 of
        // a block a pulse) every other, the diagonal ones (0.25) every fourth; the middle first.
        world = new World();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.set(x, 63, z, dirt).set(x, 64, z, grass);
            }
        }
        memory = new LandBrush.Memory();
        List<LandBrush.Column> first = pulse(world, LandStyle.RAISE, at, 2, memory);
        check(first.size() == 1 && first.get(0).x() == 0 && first.get(0).z() == 0, "the first pulse moves the middle alone: " + first.size());
        List<LandBrush.Column> second = pulse(world, LandStyle.RAISE, at, 2, memory);
        check(second.size() == 5 && second.get(0).x() == 0 && second.get(0).z() == 0, "the second the middle first, then the four beside it: " + second.size());
        pulse(world, LandStyle.RAISE, at, 2, memory);
        List<LandBrush.Column> fourth = pulse(world, LandStyle.RAISE, at, 2, memory);
        check(fourth.size() == 9, "the fourth the diagonal ones too: " + fourth.size());
        check(height(world, 0, 0) == 68 && height(world, 1, 0) == 66 && height(world, 1, 1) == 65 && height(world, 2, 0) == 64,
                "four pulses: 4, 2 and 1 up; the rim never: " + height(world, 0, 0) + " " + height(world, 1, 0) + " " + height(world, 1, 1));
        helper.succeed();
    }

    // B82

    /**
     * On a meadow, five Raise pulses of a brush of 4 make the middle column five higher than the rim, with grass on top
     * of every column (and dirt under it), and the cow standing on a rising column stands on it still, unhurt; twenty
     * Smooth pulses leave no two neighbouring columns more than one apart; each stroke is one undo step, and two undos
     * give the meadow back.
     */
    @EnhancedTest(template = ARENA, batch = "build_brushes_raise", timeoutTicks = 400)
    public static void landRaisesAHillAndSmoothsItAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        meadow(f);
        BlockPos middle = abs(f, 10, MEADOW, 10);
        int radius = 4;
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(10.5, MEADOW + 1, 12.5));
        float health = cow.getHealth();
        IntConsumer raise = i -> {
            cast(wanda, recipe(Spell.LAND).style(LandStyle.RAISE).dab(1).at(middle, Direction.UP).size(radius).build());
            finish(wanda);
        };
        pulses(helper, 5, raise, () -> {
            check(steps(wanda).isEmpty(), "the stroke is still open");
            int[][] hill = heights(f);
            check(hill[10][10] == MEADOW + 5 && hill[10 + radius][10] == MEADOW && hill[10][10 - radius] == MEADOW,
                    "the middle is five higher than the rim: " + hill[10][10] + " / " + hill[10 + radius][10]);
            for (int x = LOW; x <= HIGH; x++) {
                for (int z = LOW; z <= HIGH; z++) {
                    check(block(f, x, hill[x][z], z).is(Blocks.GRASS_BLOCK) && block(f, x, hill[x][z] - 1, z).is(Blocks.DIRT),
                            "grass on dirt on top of every column: " + x + ", " + z + " " + block(f, x, hill[x][z], z));
                }
            }
            check(neighbours(hill) >= 2, "the hill is steep: " + neighbours(hill));
            double onTop = helper.absoluteVec(new Vec3(0.0, hill[10][12] + 1, 0.0)).y;
            check(hill[10][12] == MEADOW + 2 && cow.isAlive() && cow.getHealth() == health && Math.abs(cow.getY() - onTop) < 1.0E-6,
                    "the cow rose with its column, unhurt: " + cow.getY() + " / " + onTop);
            Seen.of(wanda);
            BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 1);
            Seen seen = Seen.of(wanda);
            check(steps(wanda).size() == 1 && seen.code() == BuildVerdictPayload.DONE && seen.verdict().changed() >= steps(wanda).get(0).changed()
                    && steps(wanda).get(0).spell() == Spell.LAND, "the raise is one step: " + seen);

            BlockPos top = abs(f, 10, hill[10][10], 10);
            IntConsumer smooth = i -> {
                cast(wanda, recipe(Spell.LAND).style(LandStyle.SMOOTH).dab(2).at(top, Direction.UP).size(radius).build());
                finish(wanda);
            };
            pulses(helper, 20, smooth, () -> {
                BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 2);
                int[][] smoothed = heights(f);
                check(neighbours(smoothed) <= 1, "no two neighbouring columns more than one apart: " + neighbours(smoothed));
                check(smoothed[10][10] < hill[10][10], "the hill is lower: " + smoothed[10][10]);
                for (int x = LOW; x <= HIGH; x++) {
                    for (int z = LOW; z <= HIGH; z++) {
                        check(block(f, x, smoothed[x][z], z).is(Blocks.GRASS_BLOCK), "still grass on top: " + x + ", " + z);
                    }
                }
                check(steps(wanda).size() == 2, "two strokes, two steps: " + steps(wanda).size());

                BuildMagic.undo(wanda);
                finish(wanda);
                int[][] back = heights(f);
                for (int x = LOW; x <= HIGH; x++) {
                    for (int z = LOW; z <= HIGH; z++) {
                        check(back[x][z] == hill[x][z] && block(f, x, back[x][z], z).is(Blocks.GRASS_BLOCK), "one undo gives the hill back: " + x + ", " + z);
                    }
                }
                BuildMagic.undo(wanda);
                finish(wanda);
                int[][] meadow = heights(f);
                for (int x = LOW; x <= HIGH; x++) {
                    for (int z = LOW; z <= HIGH; z++) {
                        check(meadow[x][z] == MEADOW && block(f, x, MEADOW, z).is(Blocks.GRASS_BLOCK) && block(f, x, MEADOW - 1, z).is(Blocks.DIRT)
                                && block(f, x, MEADOW - 2, z).is(Blocks.DIRT), "the second the meadow: " + x + ", " + z);
                    }
                }
                helper.succeed();
            });
        });
    }

    // B83

    /**
     * Billy lowers the meadow round a little oak with his brush of 3, three pulses: the middle sinks two blocks and
     * stops at the smooth stone, the eight columns round it whose share made a block sink and still end in grass, the
     * oak's trunk (next to the middle) and its leaves have not moved, nor the grass under the trunk; one stroke is one
     * step, and his practice and his price are its blocks. With an empty meter his next stroke ends at its first pulse.
     */
    @EnhancedTest(template = ARENA, batch = "build_brushes_lower", timeoutTicks = 200)
    public static void landLowersAndLeavesTreesAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, null);
        meadow(f);
        // A little oak next to the middle: a trunk of three logs, and a crown of leaves round its top two (nothing on
        // the trunk itself: only the logs could hold its column).
        List<BlockPos> logs = List.of(abs(f, 11, 5, 10), abs(f, 11, 6, 10), abs(f, 11, 7, 10));
        for (BlockPos log : logs) {
            level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        List<BlockPos> leaves = new ArrayList<>();
        for (int x = 10; x <= 12; x++) {
            for (int z = 9; z <= 11; z++) {
                if (x != 11 || z != 10) {
                    leaves.add(abs(f, x, 7, z));
                    leaves.add(abs(f, x, 8, z));
                }
            }
        }
        for (BlockPos leaf : leaves) {
            level.setBlock(leaf, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
        }
        int radius = 3; // (his brush at rank 1: his side of 24 / 8)
        BlockPos middle = abs(f, 10, MEADOW, 10);
        IntConsumer lower = i -> {
            cast(billy, recipe(Spell.LAND).style(LandStyle.LOWER).dab(5).at(middle, Direction.UP).size(radius).build());
            finish(billy);
        };
        pulses(helper, 3, lower, () -> {
            Seen.of(billy);
            BuildMagic.action(billy, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 5);
            Seen seen = Seen.of(billy);
            int[][] after = heights(f);
            check(after[10][10] == MEADOW - 2 && block(f, 10, 1, 10).is(Blocks.SMOOTH_STONE), "the middle sank two and stopped at the stone: " + after[10][10]);
            // (A share of (1 - (d/3)^2)^2 a pulse: the middle sinks every pulse, the three columns beside it that are
            // not the trunk twice, the four diagonal ones once, the rest not yet.)
            int lowered = 0;
            for (int x = LOW; x <= HIGH; x++) {
                for (int z = LOW; z <= HIGH; z++) {
                    if (after[x][z] < MEADOW) {
                        lowered++;
                        BlockState under = block(f, x, after[x][z] - 1, z);
                        check(block(f, x, after[x][z], z).is(Blocks.GRASS_BLOCK) && (under.is(Blocks.DIRT) || under.is(Blocks.SMOOTH_STONE)),
                                "a lowered column still ends in grass: " + x + ", " + z + " " + block(f, x, after[x][z], z));
                    }
                }
            }
            check(lowered == 8 && after[9][10] == MEADOW - 2 && after[11][11] == MEADOW - 1 && after[12][10] == MEADOW,
                    "the middle, three beside it and the four diagonal ones are lower: " + lowered + " columns");
            check(logs.stream().allMatch(log -> level.getBlockState(log).is(Blocks.OAK_LOG)) && block(f, 11, MEADOW, 10).is(Blocks.GRASS_BLOCK)
                    && block(f, 11, MEADOW - 1, 10).is(Blocks.DIRT) && after[11][10] == MEADOW, "the trunk has not moved, nor the grass under it");
            for (BlockPos leaf : leaves) {
                check(level.getBlockState(leaf).is(Blocks.OAK_LEAVES), "the crown has not moved: " + leaf);
            }
            List<UndoStep> steps = steps(billy);
            BuildData data = ModAttachments.BUILD_DATA.get(billy);
            check(steps.size() == 1 && steps.get(0).changed() > 0 && seen.code() == BuildVerdictPayload.DONE, "one stroke, one step: " + seen);
            check(data.practice() == seen.verdict().changed() && seen.verdict().cost() == seen.verdict().changed(),
                    "his practice and his price are its blocks: " + data.practice() + " / " + seen.verdict().changed());

            // He pays at every pulse: with an empty meter his next stroke ends at its first pulse, with the fizzle, and
            // he is told why (not that nothing changed).
            ModAttachments.BUILD_DATA.set(billy, BillyMagic.setMeter(data.withPauseUntil(0L), level.getGameTime(), 0));
            BlockPos elsewhere = abs(f, 5, MEADOW, 15);
            cast(billy, recipe(Spell.LAND).style(LandStyle.LOWER).dab(6).at(elsewhere, Direction.UP).size(radius).build());
            finish(billy);
            Seen empty = Seen.of(billy);
            BuildSession session = BuildSession.existing(billy.getUUID());
            check(empty.code() == BuildVerdictPayload.REFILLING && empty.verdicts.size() == 1 && empty.lastBar().equals("message.enhanced.build.refilling")
                    && session != null && session.stroke() == null && steps(billy).size() == 1 && level.getBlockState(elsewhere).is(Blocks.GRASS_BLOCK),
                    "an empty meter ends the stroke, and says so: " + empty);
            helper.succeed();
        });
    }

    // The pulses' pace (D-B 4.11, 14.7)

    /**
     * A stroke pulses at most every {@value LandBrush#PULSE_TICKS} ticks, one pulse at a time: two dabs that come right
     * after a pulse wait, the newer in the older's place, and the server begins the newer when its turn comes; the
     * stroke is one step of its two pulses.
     */
    @EnhancedTest(template = ARENA, batch = "build_brushes_pace", timeoutTicks = 100)
    public static void aStrokePulsesEveryFourTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        meadow(f);
        cast(wanda, recipe(Spell.LAND).style(LandStyle.RAISE).dab(3).at(abs(f, 6, MEADOW, 6), Direction.UP).size(0).build());
        check(working(wanda) != null, "the first dab pulses at once");
        finish(wanda);
        cast(wanda, recipe(Spell.LAND).style(LandStyle.RAISE).dab(3).at(abs(f, 10, MEADOW, 10), Direction.UP).size(0).build());
        cast(wanda, recipe(Spell.LAND).style(LandStyle.RAISE).dab(3).at(abs(f, 14, MEADOW, 14), Direction.UP).size(0).build());
        check(working(wanda) == null, "within four ticks of the last pulse the dabs wait");
        helper.runAfterDelay(PULSE_GAP, () -> {
            BuildJob next = working(wanda);
            check(next != null && next.recipe() != null && next.recipe().anchor().equals(abs(f, 14, MEADOW, 14)),
                    "the newest dab pulses when its turn comes");
            finish(wanda);
            int[][] heights = heights(f);
            check(heights[6][6] == MEADOW + 1 && heights[14][14] == MEADOW + 1 && heights[10][10] == MEADOW,
                    "its column rose, the older dab's never: " + heights[10][10]);
            BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 3);
            check(steps(wanda).size() == 1 && steps(wanda).get(0).changed() == 4, "one step of the two pulses: " + steps(wanda).size());
            helper.succeed();
        });
    }

    // B84

    /**
     * One stone block in the air and one Swell pulse of radius 2: its six face neighbours become stone (and no other cell
     * does); two Shrink pulses take the bump away (the first its six arms, which touch air on five faces, the second the
     * block left standing); the swell is one undo step and gives the air back.
     */
    @EnhancedTest(template = ARENA, batch = "build_brushes_grow", timeoutTicks = 200)
    public static void growSwellsAndShrinks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BlockPos rock = abs(f, 10, 6, 10);
        f.level().setBlock(rock, Blocks.STONE.defaultBlockState(), 3);
        cast(wanda, recipe(Spell.GROW).style(GrowStyle.SWELL).dab(1).at(rock, Direction.UP).size(2).build());
        finish(wanda);
        BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 1);
        int stones = 0;
        for (BlockPos pos : BlockPos.betweenClosed(rock.offset(-3, -3, -3), rock.offset(3, 3, 3))) {
            if (f.level().getBlockState(pos).is(Blocks.STONE)) {
                stones++;
            }
        }
        boolean six = true;
        for (Direction direction : Direction.values()) {
            six &= f.level().getBlockState(rock.relative(direction)).is(Blocks.STONE);
        }
        check(six && stones == 7, "its six neighbours are stone, nothing else: " + stones);
        check(steps(wanda).size() == 1 && steps(wanda).get(0).spell() == Spell.GROW && steps(wanda).get(0).changed() == 6, "one step of six cells");
        Runnable shrink = () -> {
            cast(wanda, recipe(Spell.GROW).style(GrowStyle.SHRINK).dab(2).at(rock, Direction.UP).size(2).build());
            finish(wanda);
        };
        helper.runAfterDelay(PULSE_GAP, () -> {
            shrink.run();
            int left = 0;
            for (BlockPos pos : BlockPos.betweenClosed(rock.offset(-3, -3, -3), rock.offset(3, 3, 3))) {
                left += f.level().getBlockState(pos).is(Blocks.STONE) ? 1 : 0;
            }
            check(left == 1 && f.level().getBlockState(rock).is(Blocks.STONE), "the first shrink took the arms: " + left);
            helper.runAfterDelay(PULSE_GAP, () -> {
                shrink.run();
                int rest = 0;
                for (BlockPos pos : BlockPos.betweenClosed(rock.offset(-3, -3, -3), rock.offset(3, 3, 3))) {
                    rest += f.level().getBlockState(pos).is(Blocks.STONE) ? 1 : 0;
                }
                check(rest == 0, "the second shrink took the bump away: " + rest);
                BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 2);
                check(steps(wanda).size() == 2, "the shrinks are a step of their own: " + steps(wanda).size());
                helper.succeed();
            });
        });
    }

    // B85

    /**
     * Bloom on a meadow with a patch of dirt: the dirt becomes grass, and with a fixed seed the plants are exactly the
     * cells the planner predicts (the same pure code, planned before the cast); on the arena's smooth stone nothing changes,
     * and "Nothing grows here" is said once for the stroke.
     */
    @EnhancedTest(template = ARENA, batch = "build_brushes_bloom", timeoutTicks = 200)
    public static void bloomGrowsWhatThePlannerSaid(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        meadow(f);
        set(f, new BoundingBox(6, MEADOW, 6, 8, MEADOW, 14), Blocks.DIRT);
        BlockPos middle = abs(f, 10, MEADOW, 10);
        long seed = 777L;
        GrowBrush.Pulse planner = new GrowBrush.Pulse(f.level(), GrowStyle.BLOOM, middle, 4, seed);
        check(planner.step(Integer.MAX_VALUE) && planner.changes() > 0, "the planner finds something to grow");
        List<LandBrush.Column> predicted = planner.columns();
        int grass = 0;
        int plants = 0;
        for (LandBrush.Column column : predicted) {
            if (column.news()[0].is(Blocks.GRASS_BLOCK)) {
                grass++;
            } else {
                plants++;
            }
        }
        check(grass > 0 && plants > 0, "grass from dirt and plants from the seed: " + grass + ", " + plants);
        BoundingBox reach = GrowBrush.reach(middle, 4, GrowStyle.BLOOM);
        List<BlockPos> changed = new ArrayList<>();
        cast(wanda, recipe(Spell.GROW).style(GrowStyle.BLOOM).dab(1).at(middle, Direction.UP).size(4).seed(seed).build());
        finish(wanda);
        for (LandBrush.Column column : predicted) {
            for (int i = 0; i < column.size(); i++) {
                check(f.level().getBlockState(column.pos(i)).equals(column.news()[i]), "the planner's cell " + column.pos(i) + " grew: "
                        + f.level().getBlockState(column.pos(i)) + " / " + column.news()[i]);
                changed.add(column.pos(i));
            }
        }
        BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 1);
        check(steps(wanda).size() == 1 && steps(wanda).get(0).changed() == planner.changes(), "one step of exactly its cells: "
                + steps(wanda).get(0).changed() + " / " + planner.changes());
        for (BlockPos pos : BlockPos.betweenClosed(reach.minX(), reach.minY(), reach.minZ(), reach.maxX(), reach.maxY(), reach.maxZ())) {
            if (!changed.contains(pos) && !f.level().getBlockState(pos).isAir()) {
                boolean allowed = f.level().getBlockState(pos).is(Blocks.GRASS_BLOCK) || f.level().getBlockState(pos).is(Blocks.DIRT)
                        || f.level().getBlockState(pos).is(Blocks.STONE) || f.level().getBlockState(pos).is(Blocks.SMOOTH_STONE) || f.level().getBlockState(pos).is(Blocks.BARRIER);
                check(allowed, "nothing else grew: " + pos + " " + f.level().getBlockState(pos));
            }
        }

        // On smooth stone nothing grows, and the stroke says so once.
        BlockPos bare = abs(f, 1, 1, 1);
        Seen.of(wanda);
        cast(wanda, recipe(Spell.GROW).style(GrowStyle.BLOOM).dab(2).at(bare, Direction.UP).size(1).seed(seed).build());
        finish(wanda);
        Seen first = Seen.of(wanda);
        check(first.said("message.enhanced.build.nothing_grows"), "nothing grows on the stone: " + first);
        helper.runAfterDelay(PULSE_GAP, () -> {
            cast(wanda, recipe(Spell.GROW).style(GrowStyle.BLOOM).dab(2).at(bare, Direction.UP).size(1).seed(seed).build());
            finish(wanda);
            Seen second = Seen.of(wanda);
            check(!second.said("message.enhanced.build.nothing_grows"), "and says it once for the stroke: " + second);
            BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 2);
            check(steps(wanda).size() == 1, "the empty stroke leaves no step");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The column at 0, 0: {@code surface} at y 64 on {@code under} at 63. */
    private static World column(BlockState surface, BlockState under) {
        return new World().set(0, 64, 0, surface).set(0, 63, 0, under);
    }

    /** One pulse of a fresh stroke planned and written into {@code world}: its columns. */
    private static List<LandBrush.Column> pulse(World world, LandStyle style, BlockPos middle, int radius) {
        return pulse(world, style, middle, radius, new LandBrush.Memory());
    }

    /** One pulse of the stroke whose memory is {@code memory}, planned and written into {@code world}: its columns. */
    private static List<LandBrush.Column> pulse(World world, LandStyle style, BlockPos middle, int radius, LandBrush.Memory memory) {
        LandBrush.Pulse pulse = new LandBrush.Pulse(world, style, middle, radius, memory);
        check(pulse.step(Integer.MAX_VALUE), "a pulse is planned at once with reads enough");
        for (LandBrush.Column column : pulse.columns()) {
            for (int i = 0; i < column.size(); i++) {
                world.set(column.pos(i), column.news()[i]);
            }
        }
        return pulse.columns();
    }

    /** The y of the grass on the little world's column {@code x, z}. */
    private static int height(World world, int x, int z) {
        for (int y = 80; y >= 50; y--) {
            if (world.getBlockState(new BlockPos(x, y, z)).is(Blocks.GRASS_BLOCK)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Lays the meadow: grass at relative y 4 on dirt at 3 and 2, over x and z 2 to 18 (the edges stay bare stone). */
    private static void meadow(TestKit.Fixture f) {
        set(f, new BoundingBox(LOW, 2, LOW, HIGH, 3, HIGH), Blocks.DIRT);
        set(f, new BoundingBox(LOW, MEADOW, LOW, HIGH, MEADOW, HIGH), Blocks.GRASS_BLOCK);
    }

    /** Runs {@code pulse} {@code count} times, {@link #PULSE_GAP} ticks apart, then {@code then} a gap later. */
    private static void pulses(GameTestHelper helper, int count, IntConsumer pulse, Runnable then) {
        pulses(helper, 0, count, pulse, then);
    }

    private static void pulses(GameTestHelper helper, int i, int count, IntConsumer pulse, Runnable then) {
        if (i == count) {
            then.run();
            return;
        }
        pulse.accept(i);
        helper.runAfterDelay(PULSE_GAP, () -> pulses(helper, i + 1, count, pulse, then));
    }

    /**
     * The meadow's surface heights: the relative y of the highest block of each column x, z (2 to 18) that does not lie
     * loose on the ground (air, leaves and logs are passed by).
     */
    private static int[][] heights(TestKit.Fixture f) {
        int[][] heights = new int[HIGH + 1][HIGH + 1];
        for (int x = LOW; x <= HIGH; x++) {
            for (int z = LOW; z <= HIGH; z++) {
                int y = 10;
                while (y > 1 && LandBrush.loose(block(f, x, y, z))) {
                    y--;
                }
                heights[x][z] = y;
            }
        }
        return heights;
    }

    private static BlockState block(TestKit.Fixture f, int x, int y, int z) {
        return f.level().getBlockState(abs(f, x, y, z));
    }

    /** The largest difference in height between two columns side by side. */
    private static int neighbours(int[][] heights) {
        int most = 0;
        for (int x = LOW; x <= HIGH; x++) {
            for (int z = LOW; z <= HIGH; z++) {
                if (x < HIGH) {
                    most = Math.max(most, Math.abs(heights[x][z] - heights[x + 1][z]));
                }
                if (z < HIGH) {
                    most = Math.max(most, Math.abs(heights[x][z] - heights[x][z + 1]));
                }
            }
        }
        return most;
    }

    private BuildBrushTests() {
    }
}
