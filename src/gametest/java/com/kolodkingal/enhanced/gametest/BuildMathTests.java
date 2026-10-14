package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.ChangeStyle;
import com.kolodkingal.enhanced.build.FillStyle;
import com.kolodkingal.enhanced.build.ShapeKind;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellMath;
import com.kolodkingal.enhanced.build.SpellMath.Placement;
import com.kolodkingal.enhanced.build.SpellMath.Shape;
import com.kolodkingal.enhanced.build.SpellMath.Volume;
import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.build.SpellPlanner.Cell;
import com.kolodkingal.enhanced.build.SpellPlanner.Plan;
import com.kolodkingal.enhanced.build.SpellPlanner.Setup;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.VanishStyle;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Pocket;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.World;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cells;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Build Magic's pure code (D-B 19.1, B1 to B7): the shapes and their counts, the Fill styles, turn and flip, the family
 * rule, the mix, and the planner's cells and order, all over little worlds kept in a map. Nothing here touches the
 * level, so the whole batch finishes in its first tick.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildMathTests {
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
    private static final BlockState PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState SPRUCE = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockPos CASTER = new BlockPos(-10, 64, -10);

    /** A shape and its counts in Appendix A.2 (hollow -1: a Ring and an Arch are one block thick already). */
    private record Counted(ShapeKind kind, int radius, int height, int solid, int hollow) {
        Shape shape(boolean hollow, int turn) {
            return new Shape(kind, radius, height, 1, hollow, turn);
        }
    }

    // B1

    /**
     * Every count of Appendix A.2, solid and hollow, row by row and cell by cell alike; every shape but the Heart and
     * the Arch the same under the four turns (and those two turned as a whole); a row cut to a window is that window of
     * the row; and a ball of radius 50 has 540,113 cells.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void shapesHaveTheirExactCells(GameTestHelper helper) {
        List<Counted> table = new ArrayList<>();
        int[][] balls = {{2, 81, 54}, {3, 179, 98}, {4, 389, 186}, {5, 739, 278}, {6, 1189, 414}, {7, 1791, 530}};
        for (int[] row : balls) {
            table.add(new Counted(ShapeKind.BALL, row[0], 0, row[1], row[2]));
        }
        int[][] domes = {{2, 51, 33}, {3, 108, 57}, {4, 229, 105}, {5, 418, 153}, {6, 663, 225}, {7, 984, 285}};
        for (int[] row : domes) {
            table.add(new Counted(ShapeKind.DOME, row[0], 0, row[1], row[2]));
        }
        int[][] towers = {{3, 5, 185, 101}, {3, 7, 259, 133}, {4, 8, 552, 237}, {4, 9, 621, 261}, {5, 11, 1067, 377}};
        for (int[] row : towers) {
            table.add(new Counted(ShapeKind.TOWER, row[0], row[1], row[2], row[3]));
        }
        int[][] rings = {{2, 1, 12}, {4, 1, 32}, {5, 1, 28}, {5, 4, 112}, {7, 1, 40}};
        for (int[] row : rings) {
            table.add(new Counted(ShapeKind.RING, row[0], row[1], row[2], -1));
        }
        int[][] cones = {{3, 6, 102, 61}, {3, 7, 115, 65}, {4, 9, 233, 109}, {5, 11, 435, 173}};
        for (int[] row : cones) {
            table.add(new Counted(ShapeKind.CONE, row[0], row[1], row[2], row[3]));
        }
        int[][] pyramids = {{3, 84, 49}, {4, 165, 81}, {5, 286, 121}, {6, 455, 169}, {7, 680, 225}};
        for (int[] row : pyramids) {
            table.add(new Counted(ShapeKind.PYRAMID, row[0], row[0] + 1, row[1], row[2]));
        }
        int[][] arches = {{2, 7}, {3, 9}, {4, 17}, {5, 15}, {7, 21}, {8, 46}};
        for (int[] row : arches) {
            table.add(new Counted(ShapeKind.ARCH, row[0], 0, row[1], -1));
        }
        int[][] hearts = {{2, 18, 12}, {3, 34, 16}, {4, 58, 22}, {5, 90, 30}, {7, 163, 40}};
        for (int[] row : hearts) {
            table.add(new Counted(ShapeKind.HEART, row[0], 0, row[1], row[2]));
        }
        for (Counted counted : table) {
            for (boolean hollow : new boolean[]{false, true}) {
                int expected = hollow && counted.hollow >= 0 ? counted.hollow : counted.solid;
                Shape shape = counted.shape(hollow, 0);
                String what = counted.kind + " r" + counted.radius + (counted.height > 0 ? " h" + counted.height : "") + (hollow ? " hollow" : " solid");
                check(shape.count() == expected, what + " has " + expected + " cells by its rows, not " + shape.count());
                check(cellByCell(shape) == expected, what + " has " + expected + " cells one by one, not " + cellByCell(shape));
                for (int turn = 1; turn < 4; turn++) {
                    Shape turned = counted.shape(hollow, turn);
                    check(turned.count() == expected, what + " keeps its count turned " + turn + " times");
                    BoundingBox box = turned.bounds();
                    for (BlockPos pos : BlockPos.betweenClosed(box.minX() - 1, box.minY() - 1, box.minZ() - 1, box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1)) {
                        // Turned back, the cell of the turned shape is a cell of the shape (a quarter turn clockwise is
                        // x, z -> -z, x).
                        int x = pos.getX();
                        int z = pos.getZ();
                        for (int back = 0; back < turn; back++) {
                            int t = x;
                            x = z;
                            z = -t;
                        }
                        check(turned.contains(pos.getX(), pos.getY(), pos.getZ()) == shape.contains(x, pos.getY(), z),
                                what + " turned " + turn + " times is the shape turned, at " + pos);
                    }
                    if (!counted.kind.upright()) {
                        check(sameCells(turned, shape), what + " is the same under every turn");
                    } else if (turn % 2 == 1) {
                        check(!sameCells(turned, shape), what + " stands facing the builder, so a quarter turn moves it");
                    }
                }
                check(windowsAreRows(shape), what + ": a row cut to a window is that window of the row");
            }
        }
        // Deeper upright shapes, turned: their rows are still exact in every window.
        for (int turn = 0; turn < 4; turn++) {
            check(windowsAreRows(new Shape(ShapeKind.HEART, 7, 0, 3, true, turn)), "a hollow heart 3 deep, turned " + turn);
            check(windowsAreRows(new Shape(ShapeKind.ARCH, 8, 0, 3, false, turn)), "an arch 3 deep, turned " + turn);
        }
        check(Shape.of(ShapeKind.HEART, 8, 0, false, 0).depth() == 3, "a heart is 1 + r / 4 deep");
        check(Shape.of(ShapeKind.TOWER, 3, 0, true, 0).height() == 7 && Shape.of(ShapeKind.PYRAMID, 3, 0, false, 0).height() == 4,
                "a tower starts 2r + 1 tall and a pyramid r + 1");
        Shape big = new Shape(ShapeKind.BALL, 50, 0, 1, false, 0);
        check(big.count() == 540_113, "a ball of radius 50 has 540,113 cells, not " + big.count());
        helper.succeed();
    }

    // B2

    /** The heart of radius 3, top to bottom, as Appendix A.2 draws it. */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void theHeartHasItsRows(GameTestHelper helper) {
        String[] rows = {".##.##.", "#######", "#######", "#######", ".#####.", "..###..", "...#..."};
        Shape heart = new Shape(ShapeKind.HEART, 3, 0, 1, false, 0);
        for (int i = 0; i < rows.length; i++) {
            int v = rows.length - 1 - i;
            StringBuilder row = new StringBuilder();
            for (int u = -3; u <= 3; u++) {
                row.append(heart.contains(u, v, 0) ? '#' : '.');
            }
            check(row.toString().equals(rows[i]), "row " + v + " of the heart is " + rows[i] + ", not " + row);
        }
        Shape outline = new Shape(ShapeKind.HEART, 3, 0, 1, true, 0);
        check(!outline.contains(0, 3, 0) && outline.contains(0, 0, 0) && outline.contains(-3, 3, 0), "the hollow heart is its outline");
        helper.succeed();
    }

    // B3

    /**
     * The Fill styles of D-B 4.2 take their cells (5 x 4 x 5: Solid 100, Walls 64, Room 82 and 18 emptied, Floor 25,
     * Frame 40; 7 x 5 x 7 and 9 x 6 x 9 Rooms; a 16 x 1 x 16 floor), by their rows and through the planner: a Room over
     * stone leaves planks on the shell and air inside, and Only the empty keeps the stone it finds.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void fillStylesTakeTheirCells(GameTestHelper helper) {
        BoundingBox small = new BoundingBox(0, 64, 0, 4, 67, 4);
        int[][] counts = {{100, 64, 82, 25, 40}, {245, 120, 170, 49, 60}, {486, 192, 290, 81, 80}};
        BoundingBox[] boxes = {small, new BoundingBox(0, 64, 0, 6, 68, 6), new BoundingBox(0, 64, 0, 8, 69, 8)};
        int[] emptied = {18, 75, 196};
        FillStyle[] styles = {FillStyle.SOLID, FillStyle.WALLS, FillStyle.ROOM, FillStyle.FLOOR, FillStyle.FRAME};
        for (int b = 0; b < boxes.length; b++) {
            for (int s = 0; s < styles.length; s++) {
                Volume cells = SpellMath.fill(styles[s], boxes[b]);
                check(cells.count() == counts[b][s] && cellByCell(cells) == counts[b][s],
                        styles[s] + " of box " + b + " has " + counts[b][s] + " cells, not " + cells.count() + " / " + cellByCell(cells));
                // Through the planner in an empty world: every cell of the style changes (a Room's inside is air
                // already).
                Plan plan = SpellPlanner.plan(new World(), recipe(Spell.FILL).style(styles[s]).box(boxes[b]).build(), Setup.at(CASTER).withMaterial(PLANKS));
                SpellPlanner.Count count = plan.count(new World());
                count.step(Integer.MAX_VALUE);
                check(count.done() && count.changed() == counts[b][s], styles[s] + " of box " + b + " changes " + counts[b][s] + ", not " + count.changed());
            }
            Volume inside = SpellMath.inside(boxes[b]);
            check(inside.count() == emptied[b] && cellByCell(inside) == emptied[b], "the inside of box " + b + " is " + emptied[b]);
        }
        check(SpellMath.fill(FillStyle.SOLID, new BoundingBox(0, 64, 0, 15, 64, 15)).count() == 256, "16 x 1 x 16 is 256");
        // A Room over a box of stone: planks on the shell, the 18 inside cells air; it removes all 100 stones.
        World world = new World().fill(small, STONE);
        Plan room = SpellPlanner.plan(world, recipe(Spell.FILL).style(FillStyle.ROOM).box(small).build(), Setup.at(CASTER).withMaterial(PLANKS));
        SpellPlanner.Count count = room.count(world);
        count.step(Integer.MAX_VALUE);
        check(count.changed() == 100 && count.removed() == 100, "a Room over stone changes and removes 100: " + count.changed() + " / " + count.removed());
        world.build(room);
        check(world.count(PLANKS) == 82 && world.count(STONE) == 0, "the shell is planks and no stone is left");
        check(world.getBlockState(new BlockPos(2, 65, 2)).isAir() && world.getBlockState(new BlockPos(2, 66, 2)).isAir(), "the inside is air");
        // Only the empty over scattered stone: the stone stays, the air becomes planks.
        World scattered = new World().set(1, 64, 1, STONE).set(3, 65, 2, STONE).set(0, 67, 4, STONE).set(2, 66, 2, WATER);
        Plan empty = SpellPlanner.plan(scattered, recipe(Spell.FILL).style(FillStyle.EMPTY).box(small).build(), Setup.at(CASTER).withMaterial(PLANKS));
        check(cells(empty, scattered).size() == 97, "Only the empty fills the 96 air cells and the water");
        scattered.build(empty);
        check(scattered.count(STONE) == 3 && scattered.count(PLANKS) == 97, "the three stones stay");
        helper.succeed();
    }

    // B4

    /**
     * Turn and flip (D-B 4.8): a 3 x 1 x 2 pocket with oak stairs facing north turned once puts the stairs facing east
     * in the turned cell; four turns and two flips of any kind give the start back, cells and blocks; a flip looking
     * north (east and west swap) makes east-facing stairs face west and swaps a door's hinge; upside down swaps tops
     * and bottoms; and every target cell's inverse lookup gives its source cell, for every turn and flip, also through
     * a planned stamp.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void turnAndFlipMovePositionsAndStates(GameTestHelper helper) {
        BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        Placement once = new Placement(3, 1, 2, 1, SpellRecipe.NO_FLIP);
        check(once.targetSizeX() == 2 && once.targetSizeZ() == 3, "a turned 3 x 2 pocket is 2 x 3");
        check(once.state(north).getValue(StairBlock.FACING) == Direction.EAST, "one turn: the stairs face east");
        check(once.target(0, 0, 0).equals(new BlockPos(1, 0, 0)), "the north-west cell goes north-east");
        check(once.target(2, 0, 0).equals(new BlockPos(1, 0, 2)), "the north-east cell goes south-east");
        check(once.target(0, 0, 1).equals(new BlockPos(0, 0, 0)), "the south-west cell goes north-west");
        // Four turns give the start back.
        BlockPos cell = new BlockPos(2, 0, 1);
        BlockState state = north;
        int sx = 3;
        int sz = 2;
        for (int i = 0; i < 4; i++) {
            Placement turn = new Placement(sx, 1, sz, 1, SpellRecipe.NO_FLIP);
            cell = turn.target(cell.getX(), cell.getY(), cell.getZ());
            state = turn.state(state);
            sx = turn.targetSizeX();
            sz = turn.targetSizeZ();
        }
        check(cell.equals(new BlockPos(2, 0, 1)) && state == north && sx == 3 && sz == 2, "four turns give the start back");
        // Two flips of each kind give the start back.
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        BlockState east = north.setValue(StairBlock.FACING, Direction.EAST);
        for (int flip = 1; flip < 4; flip++) {
            Placement mirror = new Placement(3, 2, 2, 0, flip);
            for (BlockState block : new BlockState[]{north, east, slab, door, Blocks.LANTERN.defaultBlockState(), Blocks.OBSERVER.defaultBlockState()}) {
                check(mirror.state(mirror.state(block)) == block, "flip " + flip + " twice gives " + block + " back");
            }
            BlockPos twice = mirror.target(2, 1, 0);
            twice = mirror.target(twice.getX(), twice.getY(), twice.getZ());
            check(twice.equals(new BlockPos(2, 1, 0)), "flip " + flip + " twice gives the cell back");
        }
        Placement lookingNorth = new Placement(3, 1, 2, 0, SpellRecipe.FLIP_X);
        check(lookingNorth.state(east).getValue(StairBlock.FACING) == Direction.WEST, "a flip looking north makes east-facing stairs face west");
        check(lookingNorth.state(door).getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT, "and swaps a door's hinge");
        check(lookingNorth.target(0, 0, 0).equals(new BlockPos(2, 0, 0)), "and swaps east and west");
        Placement upside = new Placement(3, 2, 2, 0, SpellRecipe.UPSIDE_DOWN);
        check(upside.state(north).getValue(StairBlock.HALF) == Half.TOP && upside.state(slab).getValue(SlabBlock.TYPE) == SlabType.TOP,
                "upside down, stairs and slabs are on top");
        check(upside.state(Blocks.LANTERN.defaultBlockState()).getValue(BlockStateProperties.HANGING), "and a lantern hangs");
        check(upside.target(0, 0, 0).equals(new BlockPos(0, 1, 0)), "and the bottom layer goes on top");
        // The inverse lookup, for every turn and flip.
        for (int turn = 0; turn < 4; turn++) {
            for (int flip = 0; flip < 4; flip++) {
                Placement placement = new Placement(4, 3, 2, turn, flip);
                Set<BlockPos> targets = new HashSet<>();
                for (BlockPos source : BlockPos.betweenClosed(0, 0, 0, 3, 2, 1)) {
                    BlockPos target = placement.target(source.getX(), source.getY(), source.getZ());
                    targets.add(target);
                    check(source.equals(placement.source(target.getX(), target.getY(), target.getZ())), "turn " + turn + " flip " + flip + ": back to " + source);
                }
                check(targets.size() == 24, "every cell lands on a cell of its own");
                check(placement.source(placement.targetSizeX(), 0, 0) == null, "outside the target, no source");
            }
        }
        // A stamp of the pocket, turned once, standing on the floor at (10, 64, 10): the planner agrees cell by cell.
        Pocket pocket = new Pocket(3, 1, 2).set(0, 0, 0, north).set(1, 0, 0, north).set(2, 0, 0, north).set(0, 0, 1, STONE);
        World world = new World();
        Plan stamp = SpellPlanner.plan(world, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(new BlockPos(10, 64, 10), Direction.UP).turn(1).build(),
                Setup.at(CASTER).withSource(pocket));
        List<Cell> stamped = cells(stamp, world);
        check(stamped.size() == 4, "the stamp has the pocket's 4 cells (the two it lacks stay as they are): " + stamped.size());
        BlockPos corner = SpellMath.stand(2, 1, 3, new BlockPos(10, 64, 10), Direction.UP, 0);
        check(corner.equals(new BlockPos(10, 65, 9)), "the turned pocket stands on the looked block, centred: " + corner);
        for (Cell placed : stamped) {
            BlockPos local = stamp.sourceOf(placed.pos());
            check(local != null && placed.pos().equals(corner.offset(once.target(local.getX(), local.getY(), local.getZ()))),
                    "the stamped cell " + placed.pos() + " comes from the pocket's " + local);
            check(placed.state().equals(once.state(pocket.state(local.getX(), local.getY(), local.getZ()))), "and has its block turned");
        }
        helper.succeed();
    }

    // B5

    /**
     * The family rule (D-B 4.4): oak stairs (east, top, waterlogged) with spruce planks in hand become spruce stairs
     * (east, top, waterlogged); an oak slab (top) a spruce slab (top); holding spruce stairs is the same as holding
     * spruce planks; with glass in hand oak planks become glass and oak stairs stay (glass has no stairs); and Change's
     * This kind aimed at glass with spruce planks changes only the glass.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void theFamilyRuleKeepsTheShape(GameTestHelper helper) {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.WATERLOGGED, true);
        BlockState spruceStairs = SpellMath.swap(stairs, SPRUCE);
        check(spruceStairs != null && spruceStairs.is(Blocks.SPRUCE_STAIRS) && spruceStairs.getValue(StairBlock.FACING) == Direction.EAST
                && spruceStairs.getValue(StairBlock.HALF) == Half.TOP && spruceStairs.getValue(StairBlock.WATERLOGGED), "spruce stairs, east, top, in water: " + spruceStairs);
        BlockState slab = SpellMath.swap(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), SPRUCE);
        check(slab != null && slab.is(Blocks.SPRUCE_SLAB) && slab.getValue(SlabBlock.TYPE) == SlabType.TOP, "a spruce slab on top: " + slab);
        BlockState held = Blocks.SPRUCE_STAIRS.defaultBlockState();
        check(SPRUCE.equals(SpellMath.swap(PLANKS, held)), "holding spruce stairs turns oak planks into spruce planks");
        check(Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.WATERLOGGED, true).equals(SpellMath.swap(stairs, held)), "and oak stairs into spruce stairs");
        check(GLASS.equals(SpellMath.swap(PLANKS, GLASS)), "oak planks with glass in hand become glass");
        check(SpellMath.swap(stairs, GLASS) == null, "oak stairs with glass in hand stay oak stairs");
        check(Blocks.STONE_BRICKS.defaultBlockState().equals(SpellMath.swap(DIRT, Blocks.STONE_BRICK_STAIRS.defaultBlockState())),
                "a block of no family becomes the base of the material's family");
        BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        check(Blocks.BIRCH_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X).equals(SpellMath.swap(log, Blocks.BIRCH_LOG.defaultBlockState())),
                "a log keeps its axis");
        // Change, This kind, aimed at the glass of a little wall of glass, oak stairs and stone.
        World world = new World().set(0, 64, 0, GLASS).set(1, 64, 0, stairs).set(2, 64, 0, STONE).set(0, 65, 0, GLASS);
        BoundingBox box = new BoundingBox(0, 64, 0, 2, 65, 0);
        Plan kind = SpellPlanner.plan(world, recipe(Spell.CHANGE).style(ChangeStyle.KIND).box(box).at(new BlockPos(0, 64, 0), Direction.SOUTH).build(),
                Setup.at(CASTER).withMaterial(SPRUCE));
        world.build(kind);
        check(world.count(SPRUCE) == 2 && world.getBlockState(new BlockPos(1, 64, 0)) == stairs && world.count(STONE) == 1,
                "only the two glass blocks changed");
        Plan outside = SpellPlanner.plan(world, recipe(Spell.CHANGE).style(ChangeStyle.KIND).box(box).at(new BlockPos(5, 64, 0), Direction.SOUTH).build(),
                Setup.at(CASTER).withMaterial(SPRUCE));
        check(outside.refusal() == BuildVerdictPayload.LOOK_INSIDE, "This kind looking outside the box: look inside");
        helper.succeed();
    }

    // B6

    /**
     * The mix (D-B 5.3), seed 42, a hotbar of 48 stone and 16 glass: every cell gives the same block every time (two
     * mixes alike, as the server's and the client's are), over 4,096 cells stone is between 70 and 80 percent and
     * nothing but stone and glass appears; a planned Fill with the mix is the same plan twice, and another seed another
     * mix.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void theMixIsTheGhost(GameTestHelper helper) {
        SpellMath.Mix server = new SpellMath.Mix(List.of(new SpellMath.Mix.Part(STONE, 48), new SpellMath.Mix.Part(GLASS, 16)));
        SpellMath.Mix client = new SpellMath.Mix(List.of(new SpellMath.Mix.Part(STONE, 48), new SpellMath.Mix.Part(GLASS, 16)));
        int stone = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 64, 0, 15, 79, 15)) {
            BlockState block = server.pick(pos.getX(), pos.getY(), pos.getZ(), 42L);
            check(block == server.pick(pos.getX(), pos.getY(), pos.getZ(), 42L) && block == client.pick(pos.getX(), pos.getY(), pos.getZ(), 42L),
                    "the same block every time at " + pos);
            check(block == STONE || block == GLASS, "only stone and glass");
            if (block == STONE) {
                stone++;
            }
        }
        check(stone >= 4096 * 70 / 100 && stone <= 4096 * 80 / 100, "about three quarters stone: " + stone + " of 4096");
        BoundingBox box = new BoundingBox(0, 64, 0, 9, 66, 9);
        SpellRecipe fill = recipe(Spell.FILL).box(box).seed(42L).build();
        List<Cell> first = cells(SpellPlanner.plan(new World(), fill, Setup.at(CASTER).withMaterial(SpellPlanner.Material.mix(server, 42L))), new World());
        List<Cell> second = cells(SpellPlanner.plan(new World(), fill, Setup.at(CASTER).withMaterial(SpellPlanner.Material.mix(client, 42L))), new World());
        check(first.equals(second) && first.size() == 300, "the ghost is the mix that will be built");
        List<Cell> other = cells(SpellPlanner.plan(new World(), fill, Setup.at(CASTER).withMaterial(SpellPlanner.Material.mix(server, 43L))), new World());
        check(!other.equals(first), "another seed, another mix");
        helper.succeed();
    }

    // B7

    /**
     * The planner's order (Appendix A.3): a 10 x 10 x 10 fill rises layer by layer from the caster's side and its
     * Vanish melts top-down; a 200 x 4 x 200 fill goes chunk column by chunk column, every column once, starting with
     * the one nearest the caster; a radius-3 ball swells shell by shell (the first tenth all nearer its centre than the
     * last tenth); a Move over itself moves every cell before it writes over it; and a 1000 x 300 x 1000 plan gives its
     * first million cells at once, holding only its cursor.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void thePlannerWalksInItsOrder(GameTestHelper helper) {
        BoundingBox cube = new BoundingBox(0, 64, 0, 9, 73, 9);
        World world = new World();
        Plan fill = SpellPlanner.plan(world, recipe(Spell.FILL).box(cube).build(), Setup.at(new BlockPos(-5, 64, 4)).withMaterial(STONE));
        List<Cell> rising = cells(fill, world);
        check(rising.size() == 1000 && distinct(rising) == 1000, "1000 cells, each once");
        check(rising.getFirst().pos().equals(new BlockPos(0, 64, 0)), "it starts at the bottom, on the caster's side: " + rising.getFirst().pos());
        for (int i = 1; i < rising.size(); i++) {
            check(rising.get(i).pos().getY() >= rising.get(i - 1).pos().getY(), "a fill rises layer by layer");
        }
        check(fill.order() == BuildFxPayload.RISE, "and says so");
        world.fill(cube, STONE);
        Plan vanish = SpellPlanner.plan(world, recipe(Spell.VANISH).box(cube).build(), Setup.at(new BlockPos(-5, 64, 4)));
        List<Cell> melting = cells(vanish, world);
        check(melting.size() == 1000 && melting.getFirst().pos().getY() == 73, "a vanish starts at the top");
        for (int i = 1; i < melting.size(); i++) {
            check(melting.get(i).pos().getY() <= melting.get(i - 1).pos().getY(), "a vanish melts top-down");
        }
        check(vanish.order() == BuildFxPayload.MELT, "and says so");
        // A valley-sized fill: chunk column by chunk column, each once, the first the one nearest the caster.
        BoundingBox valley = new BoundingBox(0, 64, 0, 199, 67, 199);
        BlockPos caster = new BlockPos(-40, 70, 90);
        Plan wide = SpellPlanner.plan(world, recipe(Spell.FILL).box(valley).build(), Setup.at(caster).withMaterial(STONE));
        check(wide.order() == BuildFxPayload.COLUMNS, "a wide fill goes by columns");
        SpellPlanner.Cursor cursor = wide.cursor();
        LongSet seen = new LongOpenHashSet();
        Set<Long> columnsDone = new HashSet<>();
        long column = Long.MIN_VALUE;
        long firstColumn = Long.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        while (cursor.next()) {
            BlockPos pos = cursor.pos();
            check(seen.add(pos.asLong()), "every cell once");
            long here = ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
            if (here != column) {
                check(columnsDone.add(here), "a column is never come back to");
                column = here;
                lastY = Integer.MIN_VALUE;
                if (firstColumn == Long.MIN_VALUE) {
                    firstColumn = here;
                }
            }
            check(pos.getY() >= lastY, "each column rises layer by layer");
            lastY = pos.getY();
        }
        check(seen.size() == 160_000 && columnsDone.size() == 13 * 13, "160,000 cells in 169 columns: " + seen.size());
        check(firstColumn == ((0L << 32) | (5L & 0xFFFFFFFFL)), "the first column is the one nearest the caster (0, 5)");
        // A radius-3 ball swells from its centre.
        Plan ball = SpellPlanner.plan(new World(), recipe(Spell.SHAPE).style(ShapeKind.BALL).size(3).at(new BlockPos(0, 63, 0), Direction.UP).build(),
                Setup.at(CASTER).withMaterial(STONE));
        List<Cell> swelling = cells(ball, new World());
        check(swelling.size() == 179 && distinct(swelling) == 179 && ball.order() == BuildFxPayload.SHELLS, "the ball's 179 cells, shell by shell");
        BlockPos centre = new BlockPos(0, 67, 0);
        int tenth = swelling.size() / 10;
        double nearest = Double.MAX_VALUE;
        double farthest = 0.0;
        for (int i = 0; i < tenth; i++) {
            farthest = Math.max(farthest, swelling.get(i).pos().distSqr(centre));
        }
        for (int i = swelling.size() - tenth; i < swelling.size(); i++) {
            nearest = Math.min(nearest, swelling.get(i).pos().distSqr(centre));
        }
        check(farthest < nearest, "the first tenth is nearer the centre than the last tenth: " + farthest + " < " + nearest);
        // Round shapes too big to swell in one piece go by columns like every big spell, every cell once.
        for (ShapeKind kind : new ShapeKind[]{ShapeKind.BALL, ShapeKind.HEART, ShapeKind.RING}) {
            Plan big = SpellPlanner.plan(new World(), recipe(Spell.SHAPE).style(kind).size(40).size2(3).hollow(true).turn(1).at(new BlockPos(5, 63, 7), null).build(),
                    Setup.at(CASTER).withMaterial(STONE));
            Shape shape = Shape.of(kind, 40, 3, true, 1);
            List<Cell> all = cells(big, new World());
            check(big.order() == BuildFxPayload.COLUMNS && all.size() == shape.count() && distinct(all) == all.size(),
                    "a big hollow " + kind + " goes by columns, its " + shape.count() + " cells each once: " + all.size());
            check(big.origin() != null && all.stream().allMatch(cell -> shape.contains(cell.pos().getX() - big.origin().getX(),
                    cell.pos().getY() - big.origin().getY(), cell.pos().getZ() - big.origin().getZ())), "every cell is the shape's, from its base centre");
        }
        // A Move of a 3 x 3 x 3 pattern one block up and east: each cell's block leaves its cell before the Move lands
        // on it.
        BoundingBox source = new BoundingBox(0, 64, 0, 2, 66, 2);
        World pattern = new World();
        for (BlockPos pos : BlockPos.betweenClosed(0, 64, 0, 2, 66, 2)) {
            pattern.set(pos, (pos.getX() + pos.getY() + pos.getZ()) % 2 == 0 ? STONE : GLASS);
        }
        Plan move = SpellPlanner.plan(pattern, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(source).at(new BlockPos(2, 64, 2), Direction.UP).size(0).build(),
                Setup.at(CASTER));
        Set<BlockPos> moved = new HashSet<>();
        List<Cell> steps = cells(move, pattern);
        check(steps.size() == 27, "27 cells move");
        for (Cell step : steps) {
            check(step.from() != null && step.pos().equals(step.from().offset(1, 1, 1)), "each cell moves one up, east and south: " + step);
            check(!moved.contains(step.from()), "a cell moves before anything lands on it: " + step.from());
            moved.add(step.pos());
        }
        // A million cells of a 300-million-cell plan, at once: the plan is its numbers and its cursor.
        long start = System.nanoTime();
        Plan huge = SpellPlanner.plan(new World(), recipe(Spell.FILL).box(new BlockPos(0, -60, 0), new BlockPos(999, 239, 999)).build(),
                Setup.at(CASTER).withMaterial(STONE));
        check(huge.size() == 300_000_000L, "300 million cells: " + huge.size());
        SpellPlanner.Cursor walk = huge.cursor();
        World empty = new World();
        for (int i = 0; i < 1_000_000; i++) {
            check(walk.next(), "the cursor goes on");
            if (walk.resolve(empty) != STONE) {
                check(false, "every cell gets stone");
            }
        }
        long millis = (System.nanoTime() - start) / 1_000_000L;
        check(millis < 10_000L, "the first million cells come at once, not after a list of 300 million: " + millis + " ms");
        helper.succeed();
    }

    /**
     * What the world tests of D-B 19.2 will build, planned here first: Hollow out keeps the 98 outer cells of a 5 x 5 x
     * 5 cube; Plants and trees and This kind; Only the outside changes the 26 outer cells of a 3 x 3 x 3 cube; a
     * waterlogged slab vanishes into water and a slab filled into water is waterlogged; Pull +2 adds 18 in front of a
     * 3 x 3 wall and Push -1 takes its 9; a hollow dome of radius 3 stands on the looked floor with 57 cells and no
     * floor; a Repeat of a pillar 3 times east with a gap of 1 puts pillars at +2, +4 and +6; a Paint dab turns the
     * stairs under it; a selected thing's Fill and a missing box, material or pocket.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void everySpellPlansItsCells(GameTestHelper helper) {
        BoundingBox five = new BoundingBox(0, 64, 0, 4, 68, 4);
        World cube = new World().fill(five, STONE);
        cube.build(SpellPlanner.plan(cube, recipe(Spell.VANISH).style(VanishStyle.HOLLOW).box(five).build(), Setup.at(CASTER)));
        check(cube.count(STONE) == 98, "Hollow out keeps the 98 outer cells: " + cube.count(STONE));
        World garden = new World().set(0, 64, 0, STONE).set(1, 64, 0, Blocks.OAK_LOG.defaultBlockState()).set(2, 64, 0, Blocks.OAK_LEAVES.defaultBlockState())
                .set(0, 65, 0, Blocks.POPPY.defaultBlockState()).set(1, 65, 0, DIRT).set(2, 65, 0, DIRT);
        BoundingBox row = new BoundingBox(0, 64, 0, 2, 65, 0);
        garden.build(SpellPlanner.plan(garden, recipe(Spell.VANISH).style(VanishStyle.PLANTS).box(row).build(), Setup.at(CASTER)));
        check(garden.count(STONE) == 1 && garden.count(DIRT) == 2 && garden.getBlockState(new BlockPos(1, 64, 0)).isAir()
                && garden.getBlockState(new BlockPos(0, 65, 0)).isAir(), "Plants and trees takes the log, the leaves and the poppy");
        garden.build(SpellPlanner.plan(garden, recipe(Spell.VANISH).style(VanishStyle.KIND).box(row).at(new BlockPos(1, 65, 0), Direction.NORTH).build(),
                Setup.at(CASTER)));
        check(garden.count(STONE) == 1 && garden.count(DIRT) == 0, "This kind aimed at dirt leaves the stone");
        BoundingBox three = new BoundingBox(0, 64, 0, 2, 66, 2);
        World skin = new World().fill(three, STONE);
        skin.build(SpellPlanner.plan(skin, recipe(Spell.CHANGE).style(ChangeStyle.OUTSIDE).box(three).build(), Setup.at(CASTER).withMaterial(SPRUCE)));
        check(skin.count(SPRUCE) == 26 && skin.getBlockState(new BlockPos(1, 65, 1)) == STONE, "Only the outside changes 26, the centre stays stone");
        BlockState wetSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true);
        World pond = new World().set(0, 64, 0, wetSlab).set(1, 64, 0, WATER);
        BoundingBox two = new BoundingBox(0, 64, 0, 1, 64, 0);
        pond.build(SpellPlanner.plan(pond, recipe(Spell.VANISH).box(two).build(), Setup.at(CASTER)));
        check(pond.getBlockState(new BlockPos(0, 64, 0)) == WATER && pond.getBlockState(new BlockPos(1, 64, 0)).isAir(),
                "the waterlogged slab leaves its water, the water itself vanishes");
        pond.build(SpellPlanner.plan(pond, recipe(Spell.FILL).box(two).build(), Setup.at(CASTER).withMaterial(Blocks.OAK_SLAB.defaultBlockState())));
        check(pond.getBlockState(new BlockPos(0, 64, 0)).getValue(SlabBlock.WATERLOGGED) && !pond.getBlockState(new BlockPos(1, 64, 0)).getValue(SlabBlock.WATERLOGGED),
                "a slab filled into water is waterlogged, into air it is not");
        // Pull and Push on a 3 x 3 stone wall facing east.
        World wall = new World().fill(new BoundingBox(5, 64, 0, 5, 66, 2), STONE).set(4, 65, 1, STONE);
        Plan pull = SpellPlanner.plan(wall, recipe(Spell.PULL).at(new BlockPos(5, 65, 1), Direction.EAST).size(2).build(), Setup.at(CASTER));
        check(!pull.prepare(1), "a pull floods its surface a few reads at a time");
        check(pull.size() == 18 && cells(pull, wall).size() == 18, "Pull +2 adds 18");
        Plan push = SpellPlanner.plan(wall, recipe(Spell.PULL).at(new BlockPos(5, 65, 1), Direction.EAST).size(-1).build(), Setup.at(CASTER));
        List<Cell> pushed = cells(push, wall);
        check(pushed.size() == 9 && pushed.stream().allMatch(cell -> cell.pos().getX() == 5 && cell.state().isAir()), "Push -1 takes the wall's 9");
        Plan billy = SpellPlanner.plan(wall, recipe(Spell.PULL).at(new BlockPos(5, 65, 1), Direction.EAST).size(1).build(), Setup.at(CASTER).withSurface(4, 0));
        check(billy.size() == 4, "a surface stops at its cap: " + billy.size());
        Plan narrow = SpellPlanner.plan(wall, recipe(Spell.PULL).at(new BlockPos(5, 65, 1), Direction.EAST).size(1).build(), Setup.at(CASTER).withSurface(100, 1));
        check(narrow.size() == 1, "and inside the span round the looked block: " + narrow.size());
        // A hollow dome of radius 3 aimed at the floor.
        World floor = new World().fill(new BoundingBox(-5, 63, -5, 5, 63, 5), Blocks.SMOOTH_STONE.defaultBlockState());
        Plan dome = SpellPlanner.plan(floor, recipe(Spell.SHAPE).style(ShapeKind.DOME).size(3).hollow(true).at(new BlockPos(0, 63, 0), Direction.UP).build(),
                Setup.at(CASTER).withMaterial(STONE));
        List<Cell> domeCells = cells(dome, floor);
        check(domeCells.size() == 57 && domeCells.stream().allMatch(cell -> cell.pos().getY() >= 64), "a hollow dome of 57 on the floor, none in it");
        check(domeCells.stream().anyMatch(cell -> cell.pos().equals(new BlockPos(0, 67, 0))), "its top is 3 above its base");
        Plan tower = SpellPlanner.plan(floor, recipe(Spell.SHAPE).style(ShapeKind.TOWER).size(2).size2(6).hollow(true).at(new BlockPos(0, 63, 0), Direction.UP).build(),
                Setup.at(CASTER).withMaterial(STONE));
        check(tower.bounds() != null && tower.bounds().getYSpan() == 6 && tower.bounds().minY() == 64, "a tower of height 6 has 6 layers");
        // A Repeat: a 1 x 2 x 1 pillar, 3 copies east with a gap of 1.
        World pillar = new World().set(0, 64, 0, STONE).set(0, 65, 0, STONE);
        Plan repeat = SpellPlanner.plan(pillar, recipe(Spell.REPEAT).box(new BlockPos(0, 64, 0), new BlockPos(0, 65, 0)).at(BlockPos.ZERO, Direction.EAST)
                .size(3).size2(1).build(), Setup.at(CASTER));
        pillar.build(repeat);
        check(pillar.count(STONE) == 8 && pillar.getBlockState(new BlockPos(2, 65, 0)) == STONE && pillar.getBlockState(new BlockPos(4, 64, 0)) == STONE
                && pillar.getBlockState(new BlockPos(6, 65, 0)) == STONE && pillar.getBlockState(new BlockPos(1, 64, 0)).isAir(), "pillars at +2, +4 and +6");
        // A Paint dab of radius 1 on oak stairs facing north.
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        World steps = new World().set(0, 64, 0, stairs).set(1, 64, 0, stairs).set(2, 64, 0, stairs);
        steps.build(SpellPlanner.plan(steps, recipe(Spell.PAINT).op(SpellRecipe.Op.DAB).at(new BlockPos(1, 64, 0), Direction.UP).size(1).build(),
                Setup.at(CASTER).withMaterial(SPRUCE)));
        check(steps.count(Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH)) == 3, "three spruce stairs facing north");
        // A selected thing: a log and its leaves, filled with gold.
        LongSet tree = new LongOpenHashSet(new long[]{new BlockPos(0, 64, 0).asLong(), new BlockPos(0, 65, 0).asLong(), new BlockPos(1, 65, 0).asLong()});
        Plan gold = SpellPlanner.plan(new World(), recipe(Spell.FILL).mask().build(), Setup.at(CASTER).withMask(SpellMath.cells(tree))
                .withMaterial(Blocks.GOLD_BLOCK.defaultBlockState()));
        check(cells(gold, new World()).size() == 3, "a selected thing's fill turns exactly its cells");
        check(SpellPlanner.plan(new World(), recipe(Spell.FILL).build(), Setup.at(CASTER).withMaterial(STONE)).refusal() == BuildVerdictPayload.NO_BOX,
                "a fill with no box: no box");
        check(SpellPlanner.plan(new World(), recipe(Spell.FILL).box(two).build(), Setup.at(CASTER)).refusal() == BuildVerdictPayload.NO_BLOCK,
                "a fill with nothing in hand: no block");
        check(SpellPlanner.plan(new World(), recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).build(), Setup.at(CASTER)).refusal() == BuildVerdictPayload.POCKET_EMPTY,
                "a stamp of an empty pocket: pocket empty");
        Plan nothing = SpellPlanner.plan(new World(), recipe(Spell.PULL).at(new BlockPos(0, 70, 0), Direction.UP).size(3).build(), Setup.at(CASTER));
        check(!nothing.writes() && nothing.bounds() == null && nothing.size() == 0, "a pull of the sky pulls nothing");
        Plan endless = SpellPlanner.plan(pillar, recipe(Spell.REPEAT).box(new BlockPos(0, 64, 0), new BlockPos(0, 65, 0)).at(BlockPos.ZERO, Direction.EAST)
                .size(Integer.MAX_VALUE).build(), Setup.at(CASTER));
        check(endless.bounds() != null && endless.bounds().maxX() > 0 && endless.bounds().maxX() <= 60_000_000 && endless.size() > 0,
                "a Repeat with no count stops where worlds end: " + endless.bounds());
        Plan take = SpellPlanner.plan(new World(), recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(two).build(), Setup.at(CASTER));
        check(!take.writes() && take.region() != null && take.refusal() == SpellPlanner.NONE, "a copy's first click writes nothing and reads its box");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The volume's cells counted one by one over its bounds (and one block round them). */
    private static long cellByCell(Volume volume) {
        BoundingBox box = volume.bounds();
        long n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX() - 1, box.minY() - 1, box.minZ() - 1, box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1)) {
            if (volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
                check(box.isInside(pos), "a cell outside its bounds: " + pos);
                n++;
            }
        }
        return n;
    }

    private static boolean sameCells(Volume a, Volume b) {
        BoundingBox x = a.bounds();
        BoundingBox y = b.bounds();
        for (BlockPos pos : BlockPos.betweenClosed(Math.min(x.minX(), y.minX()) - 1, Math.min(x.minY(), y.minY()) - 1, Math.min(x.minZ(), y.minZ()) - 1,
                Math.max(x.maxX(), y.maxX()) + 1, Math.max(x.maxY(), y.maxY()) + 1, Math.max(x.maxZ(), y.maxZ()) + 1)) {
            if (a.contains(pos.getX(), pos.getY(), pos.getZ()) != b.contains(pos.getX(), pos.getY(), pos.getZ())) {
                return false;
            }
        }
        return true;
    }

    /** Whether every row of the volume, cut to any window five wide, holds exactly the cells {@code contains} says. */
    private static boolean windowsAreRows(Volume volume) {
        BoundingBox box = volume.bounds();
        for (int y = box.minY() - 1; y <= box.maxY() + 1; y++) {
            for (int z = box.minZ() - 1; z <= box.maxZ() + 1; z++) {
                for (int x0 = box.minX() - 2; x0 <= box.maxX() + 1; x0++) {
                    int x1 = x0 + 4;
                    int[] runs = volume.runs(y, z, x0, x1);
                    for (int x = x0; x <= x1; x++) {
                        boolean inRuns = false;
                        for (int i = 0; i < runs.length; i += 2) {
                            check(runs[i] <= runs[i + 1] && runs[i] >= x0 && runs[i + 1] <= x1, "a run inside its window");
                            inRuns |= x >= runs[i] && x <= runs[i + 1];
                        }
                        if (inRuns != volume.contains(x, y, z)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static int distinct(List<Cell> cells) {
        Set<BlockPos> positions = new HashSet<>();
        for (Cell cell : cells) {
            positions.add(cell.pos());
        }
        return positions.size();
    }
}
