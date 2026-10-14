package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.ChangeStyle;
import com.kolodkingal.enhanced.build.FillStyle;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.build.MagicSelect;
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
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cells;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Build Magic's pure code (D-B 19.1, B1 to B8 and B12): the shapes and their counts, the Fill styles, turn and flip, the
 * family rule, the mix, the planner's cells and order, magic select, and a recipe clamped to the hero's limits, all
 * over little worlds kept in a map; also Billy's meter, practice and ranks as arithmetic over his data. Nothing here
 * changes the level (one test stands players in the arena to read their limits), so the whole batch finishes in its
 * first tick.
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
        check(ShapeKind.TOWER.defaultHeight(3) == 7 && ShapeKind.PYRAMID.defaultHeight(3) == 4 && ShapeKind.RING.defaultHeight(3) == 1,
                "a tower starts 2r + 1 tall, a pyramid r + 1, a ring 1");
        check(Shape.of(ShapeKind.TOWER, 3, 7, true, 0).height() == 7 && Shape.of(ShapeKind.TOWER, 3, 0, true, 0).height() == 1
                && Shape.of(ShapeKind.BALL, 3, 9, true, 0).height() == 0, "a shape's height is the recipe's, at least 1 (as the clamp makes it)");
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
        // The same blocks and weights in another order, or in other stacks, are the same mix in every cell: the
        // server and a client each read their own hotbar.
        SpellMath.Mix split = new SpellMath.Mix(List.of(new SpellMath.Mix.Part(STONE, 32), new SpellMath.Mix.Part(GLASS, 16),
                new SpellMath.Mix.Part(STONE, 16)));
        SpellMath.Mix turned = new SpellMath.Mix(List.of(new SpellMath.Mix.Part(GLASS, 16), new SpellMath.Mix.Part(STONE, 48)));
        check(split.equals(server) && turned.equals(server) && split.blocks().size() == 2, "two stacks of stone are one part of 48: " + split);
        for (BlockPos pos : BlockPos.betweenClosed(0, 64, 0, 15, 67, 15)) {
            BlockState block = server.pick(pos.getX(), pos.getY(), pos.getZ(), 42L);
            check(split.pick(pos.getX(), pos.getY(), pos.getZ(), 42L) == block && turned.pick(pos.getX(), pos.getY(), pos.getZ(), 42L) == block,
                    "every cell the same, whatever the order and the stacks, at " + pos);
        }
        List<ItemStack> hotbar = List.of(new ItemStack(Items.STONE, 32), ItemStack.EMPTY, new ItemStack(Items.STICK, 5), new ItemStack(Items.GLASS, 16),
                new ItemStack(Items.STONE, 16), new ItemStack(Items.OAK_DOOR, 3));
        SpellMath.Mix read = SpellMath.Mix.ofHotbar(hotbar, stack -> stack.is(Items.OAK_DOOR) ? null : ((BlockItem) stack.getItem()).getBlock().defaultBlockState());
        check(server.equals(read), "a hotbar of 32 stone, a stick, 16 glass and 16 stone (a door left out) is 48 stone and 16 glass: " + read);
        check(SpellMath.Mix.ofHotbar(List.of(new ItemStack(Items.STICK, 3), ItemStack.EMPTY), stack -> STONE) == null, "a hotbar without a block has no mix");
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
        // Before its flood a pull says at once how big it may be, and floods only when it is asked to.
        Plan capped = SpellPlanner.plan(wall, recipe(Spell.PULL).at(new BlockPos(5, 65, 1), Direction.EAST).size(2).build(), Setup.at(CASTER).withSurface(4, 0));
        check(!capped.prepared() && capped.sizeBound() == 8 && !capped.prepared(), "a pull of 2 layers with a cap of 4: at most 8, not flooded yet");
        check(capped.size() == 8 && capped.prepared() && capped.sizeBound() == 8, "flooded, it is 8");
        Plan spanned = SpellPlanner.plan(wall, recipe(Spell.PULL).at(new BlockPos(5, 65, 1), Direction.EAST).size(-1).build(), Setup.at(CASTER).withSurface(100, 3));
        check(spanned.sizeBound() == 9 && spanned.prepare(1000) && spanned.size() == 9, "a span of 3 holds at most 9 a layer");
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

    /**
     * A plan ends where the world ends (D-B 10.3 P2) and never loads a chunk to look (11.7): a dome standing on the top
     * of the world counts and shows only its cells under the build height, the rest counted apart; a Repeat with no
     * count stops at the top and at the bottom of the world, and one with no room plans nothing; the server's level
     * knows its border. A Move's or a Repeat's source read live (a ghost's) reads no chunk that is not loaded, and
     * leaves the far chunk unloaded; its size is the selected thing's cells, not its box's.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void aPlanEndsWhereTheWorldEnds(GameTestHelper helper) {
        World world = new World();
        int top = world.getMaxBuildHeight() - 1;
        Plan dome = SpellPlanner.plan(world, recipe(Spell.SHAPE).style(ShapeKind.DOME).size(3).hollow(true).at(new BlockPos(0, top - 1, 0), Direction.UP).build(),
                Setup.at(CASTER).withMaterial(STONE));
        SpellPlanner.Count count = dome.count(world);
        count.step(Integer.MAX_VALUE);
        List<Cell> shown = cells(dome, world);
        check(count.done() && count.walked() == 57 && count.outside() > 0 && count.changed() + count.outside() == 57 && count.unloaded() == 0,
                "a dome on the top of the world: " + count.changed() + " of its 57 inside, " + count.outside() + " above");
        check(shown.size() == count.changed() && shown.stream().allMatch(cell -> cell.pos().getY() == top), "the ghost shows only its base layer");

        World pillar = new World().set(0, 64, 0, STONE).set(0, 65, 0, STONE);
        BlockPos low = new BlockPos(0, 64, 0);
        BlockPos high = new BlockPos(0, 65, 0);
        Plan up = SpellPlanner.plan(pillar, recipe(Spell.REPEAT).box(low, high).at(BlockPos.ZERO, Direction.UP).size(Integer.MAX_VALUE).build(), Setup.at(CASTER));
        check(up.bounds() != null && up.bounds().maxY() == top && up.size() == 254,
                "a Repeat up with no count stops at the top of the world: " + up.bounds() + ", " + up.size());
        Plan down = SpellPlanner.plan(pillar, recipe(Spell.REPEAT).box(low, high).at(BlockPos.ZERO, Direction.DOWN).size(Integer.MAX_VALUE).build(), Setup.at(CASTER));
        check(down.bounds() != null && down.bounds().minY() == world.getMinBuildHeight() && down.size() == 128, "and down at its bottom: " + down.bounds());
        World roof = new World().set(0, top - 1, 0, STONE).set(0, top, 0, STONE);
        Plan none = SpellPlanner.plan(roof, recipe(Spell.REPEAT).box(new BlockPos(0, top - 1, 0), new BlockPos(0, top, 0)).at(BlockPos.ZERO, Direction.UP)
                .size(3).build(), Setup.at(CASTER));
        check(none.refusal() == SpellPlanner.NONE && !none.writes() && none.size() == 0, "a Repeat up from the top of the world has no room: nothing");

        ServerLevel level = helper.getLevel();
        BlockPos beyond = new BlockPos(30_000_000, 64, 0);
        check(SpellMath.outside(level, beyond) && SpellMath.outside(level, new BlockPos(0, level.getMaxBuildHeight(), 0))
                && !SpellMath.outside(level, helper.absolutePos(new BlockPos(5, 2, 5))), "the level's world ends at its border and its build height");
        check(SpellMath.world(level).equals(Limits.world(level)) && !SpellMath.world(level).isInside(beyond), "one box of the world for both");

        // A Move and a Repeat of a box in a chunk that is not loaded, as a ghost would plan them before CAPTURE.
        BlockPos far = new BlockPos(1_000_000, 64, 1_000_000);
        check(!level.hasChunkAt(far), "the far chunk is not loaded");
        BoundingBox away = new BoundingBox(far.getX(), far.getY(), far.getZ(), far.getX() + 2, far.getY() + 2, far.getZ() + 2);
        BlockPos here = helper.absolutePos(new BlockPos(10, 1, 10));
        Plan move = SpellPlanner.plan(level, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(away).at(here, Direction.UP).build(), Setup.at(here));
        Plan row = SpellPlanner.plan(level, recipe(Spell.REPEAT).box(away).at(here, Direction.EAST).size(2).build(), Setup.at(here));
        check(cells(move, level).isEmpty() && move.count(level).step(Integer.MAX_VALUE), "a Move from an unloaded chunk has nothing to put down");
        row.count(level).step(Integer.MAX_VALUE);
        check(!level.hasChunkAt(far) && !level.hasChunkAt(far.offset(16, 0, 0)), "and the far chunks are still not loaded");

        // A selected thing of 3 cells in a box of 4: its Move is 3 cells, its Repeat 4 times 3.
        World corner = new World().set(0, 64, 0, STONE).set(1, 64, 0, STONE).set(0, 64, 1, STONE);
        LongSet thing = new LongOpenHashSet(new long[]{new BlockPos(0, 64, 0).asLong(), new BlockPos(1, 64, 0).asLong(), new BlockPos(0, 64, 1).asLong()});
        Plan carried = SpellPlanner.plan(corner, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).mask().at(new BlockPos(5, 63, 5), Direction.UP).build(),
                Setup.at(CASTER).withMask(SpellMath.cells(thing)));
        Plan repeated = SpellPlanner.plan(corner, recipe(Spell.REPEAT).mask().at(BlockPos.ZERO, Direction.UP).size(4).build(),
                Setup.at(CASTER).withMask(SpellMath.cells(thing)));
        check(carried.size() == 3 && cells(carried, corner).size() == 3, "a Move of the thing is its 3 cells, not its box of 4: " + carried.size());
        check(repeated.size() == 12 && cells(repeated, corner).size() == 12, "4 copies of it are 12: " + repeated.size());
        helper.succeed();
    }

    // B8

    /**
     * Magic select (D-B 3.4): a five-log trunk with twenty leaves on a grass meadow, four of them touching the rest
     * only at a corner, gives exactly its 25 cells: no grass block, no dirt, no grass tuft round its foot, not the
     * cobblestone three cells away; the grass gives only the grass its faces connect (not the dirt under it, not a
     * grass block touching it at an edge); with a cap of 512 a 30 x 1 x 30 grass floor gives nothing, with the reason.
     * Also: two seeds give the union and a seed already selected adds nothing; Billy's side, the reach (and the
     * server's slack), the biggest bounding box, a seed on air; the flood in slices gives the same cells; and the
     * planner fills exactly the selected thing.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void magicSelectFindsTheThingNotTheGround(GameTestHelper helper) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        BlockState tuft = Blocks.SHORT_GRASS.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        World meadow = new World().fill(new BoundingBox(-4, 62, -4, 4, 62, 4), DIRT).fill(new BoundingBox(-4, 63, -4, 4, 63, 4), grass)
                .set(4, 64, 4, grass).set(5, 63, 5, grass).set(-3, 64, 3, cobble)
                .set(1, 64, 0, tuft).set(0, 64, -1, tuft).set(1, 64, 1, tuft).set(-1, 64, -1, tuft);
        for (int y = 64; y <= 68; y++) {
            meadow.set(0, y, 0, log);
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            meadow.set(side.getStepX(), 68, side.getStepZ(), leaves).set(side.getStepX(), 69, side.getStepZ(), leaves);
        }
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                // A leaf at the trunk's corner, and one that touches only that leaf, at a corner.
                meadow.set(dx, 68, dz, leaves).set(2 * dx, 69, 2 * dz, leaves);
            }
        }
        meadow.set(0, 69, 0, leaves).set(0, 70, 0, leaves).set(1, 70, 0, leaves).set(-1, 70, 0, leaves);
        check(meadow.count(log) == 5 && meadow.count(leaves) == 20, "a trunk of 5 and 20 leaves");

        MagicSelect tree = new MagicSelect(meadow, List.of(new BlockPos(0, 66, 0)), 512).run();
        check(tree.refusal() == SpellPlanner.NONE && tree.size() == 25, "the tree is its 25 cells: " + tree.size() + ", " + tree.refusal());
        check(tree.found().longStream().allMatch(p -> meadow.getBlockState(BlockPos.of(p)) == log || meadow.getBlockState(BlockPos.of(p)) == leaves),
                "only logs and leaves: no grass, no tuft, no dirt, no cobblestone");
        check(tree.found().contains(new BlockPos(2, 69, 2).asLong()) && tree.found().contains(new BlockPos(-2, 69, -2).asLong()),
                "the leaves touching only at a corner come too");
        MagicSelect fromLeaf = new MagicSelect(meadow, List.of(new BlockPos(-2, 69, 2)), 512).run();
        check(fromLeaf.size() == 25 && fromLeaf.found().equals(tree.found()), "a double click on any of its cells gives the same thing");
        check(tree.bounds() != null && tree.bounds().equals(new BoundingBox(-2, 64, -2, 2, 70, 2)), "its box: " + tree.bounds());

        MagicSelect lawn = new MagicSelect(meadow, List.of(new BlockPos(3, 63, -3)), 512).run();
        check(lawn.size() == 82 && lawn.found().longStream().allMatch(p -> meadow.getBlockState(BlockPos.of(p)) == grass),
                "the grass gives its 81 and the grass block on it, nothing else: " + lawn.size());
        check(!lawn.found().contains(new BlockPos(5, 63, 5).asLong()), "a grass block touching it only at an edge is not connected");

        World field = new World().fill(new BoundingBox(0, 63, 0, 29, 63, 29), grass);
        MagicSelect tooBig = new MagicSelect(field, List.of(new BlockPos(10, 63, 10)), 512).run();
        check(tooBig.done() && tooBig.refusal() == BuildVerdictPayload.TOO_BIG_TO_SELECT && tooBig.limit() == 512, "900 grass blocks are over the cap of 512");
        check(tooBig.size() == 0 && tooBig.mask() == null && tooBig.bounds() == null, "and then nothing is selected, not a part");
        check(said(tooBig.message(false), "message.enhanced.build.too_big_to_select", 512), "Billy hears: too big to hold in mind (more than 512)");
        Component toWanda = tooBig.message(true);
        check(toWanda != null && said(toWanda, "message.enhanced.build.too_big_to_select", 512) && toWanda.getSiblings().stream()
                .anyMatch(part -> said(part, "message.enhanced.build.mark_corners")), "the Scarlet Witch also hears: mark it with two corners instead");
        check(new MagicSelect(field, List.of(new BlockPos(10, 63, 10)), 1000).run().size() == 900, "under a cap of 1000 the whole field");

        MagicSelect two = new MagicSelect(meadow, List.of(new BlockPos(0, 66, 0), new BlockPos(-3, 64, 3), new BlockPos(1, 68, 0)), 512).run();
        check(two.size() == 26 && two.seeds().size() == 3, "two things are their union, and a seed already selected adds nothing: " + two.size());
        List<BlockPos> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nine.add(new BlockPos(i, 63, 0));
        }
        check(new MagicSelect(meadow, nine, 512).seeds().size() == SpellRecipe.MAX_SEEDS, "at most 8 seeds");
        MagicSelect sky = new MagicSelect(meadow, List.of(new BlockPos(0, 80, 0), new BlockPos(1, 64, 0)), 512).run();
        check(sky.refusal() == BuildVerdictPayload.NO_BOX && sky.size() == 0, "seeds on air and on a tuft select nothing");

        World beam = new World().fill(new BoundingBox(0, 64, 0, 29, 64, 0), PLANKS);
        MagicSelect wide = new MagicSelect(beam, List.of(new BlockPos(3, 64, 0)), 512, 24, null).run();
        check(wide.refusal() == BuildVerdictPayload.TOO_WIDE && wide.limit() == 24 && wide.size() == 0, "a beam of 30 is wider than Billy's 24");
        check(new MagicSelect(beam, List.of(new BlockPos(3, 64, 0)), 512, 0, null).run().size() == 30, "with no side limit it is 30");

        Vec3 eyes = new Vec3(0.5, 66.0, -9.5);
        MagicSelect far = new MagicSelect(meadow, List.of(new BlockPos(0, 66, 0)), 512, 0, new MagicSelect.Reach(eyes, 12, 0)).run();
        check(far.refusal() == BuildVerdictPayload.TOO_FAR && far.limit() == 12 && far.size() == 0,
                "a tree reaching 13 blocks from the eyes is beyond a reach of 12: nothing");
        check(said(far.message(false), "message.enhanced.build.too_far", 12), "too far for your magic (at most 12)");
        check(new MagicSelect(meadow, List.of(new BlockPos(0, 66, 0)), 512, 0, new MagicSelect.Reach(eyes, 12, Limits.REACH_SLACK)).run().size() == 25,
                "with the server's slack of 2 it is within reach");

        World chain = new World();
        for (int i = 0; i < 257; i++) {
            chain.set(i, -60 + i, i, PLANKS);
        }
        MagicSelect diagonal = new MagicSelect(chain, List.of(new BlockPos(0, -60, 0)), 1_000_000).run();
        check(diagonal.refusal() == BuildVerdictPayload.TOO_BIG_TO_SELECT && diagonal.size() == 0 && diagonal.limit() == MagicSelect.MAX_BOUNDS,
                "257 planks touching at their corners make a box of more than 16,777,216 cells: nothing");
        check(said(diagonal.message(true), "message.enhanced.build.too_big_to_select", 16_777_216),
                "and the number said is the box's, not the cap of a million the thing is far under");
        chain.set(256, 196, 256, Blocks.AIR.defaultBlockState());
        check(new MagicSelect(chain, List.of(new BlockPos(0, -60, 0)), 1_000_000).run().size() == 256, "256 make a box of exactly 256^3, which is held");

        MagicSelect sliced = new MagicSelect(meadow, List.of(new BlockPos(0, 66, 0)), 512);
        int slices = 0;
        int before = 0;
        boolean grows = true;
        while (!sliced.step(10)) {
            slices++;
            grows &= sliced.size() >= before;
            before = sliced.size();
            check(sliced.mask() == null, "no mask while it floods");
        }
        check(slices > 5 && grows && sliced.found().equals(tree.found()), "ten reads at a time it floods in " + slices + " slices to the same 25");

        Plan gold = SpellPlanner.plan(meadow, recipe(Spell.FILL).mask().build(), Setup.at(CASTER).withMask(tree.mask())
                .withMaterial(Blocks.GOLD_BLOCK.defaultBlockState()));
        meadow.build(gold);
        check(meadow.count(Blocks.GOLD_BLOCK.defaultBlockState()) == 25 && meadow.count(tuft) == 4 && meadow.count(grass) == 83 && meadow.count(cobble) == 1,
                "turn this tree into gold: its 25 cells, and the meadow round it as it was");
        helper.succeed();
    }

    // B12

    /**
     * A recipe as a stale or modified client might send it is clamped by {@code SpellRecipe.clamped} to the hero's
     * {@code Limits} (D-B 13.4): turn 7 is 3, nine seeds are eight, a size of -5 is each spell's smallest, a position
     * outside the world comes inside it, every other number into its range; Billy's numbers at ranks 1 and 4 and the
     * Scarlet Witch's (no count in a Repeat, R39); a recipe in range is unchanged, and clamping twice is clamping once.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void aRecipeIsClamped(GameTestHelper helper) {
        BoundingBox world = Limits.world(helper.getLevel());
        check(world.minY() == helper.getLevel().getMinBuildHeight() && world.maxY() == helper.getLevel().getMaxBuildHeight() - 1
                && world.getXSpan() > 1_000_000, "the world's cells: its build height and its border " + world);
        Limits billy = Limits.billy(1, world);
        Limits wiccan = Limits.billy(4, world);
        Limits wanda = Limits.wanda(world);
        check(billy.side() == 24 && billy.reach() == 24 && billy.shapeRadius() == 11 && billy.shapeHeight() == 24 && billy.repeatCopies() == 8
                && billy.brush() == 3 && billy.magicSelect() == 512 && billy.undoSteps() == 10, "an Apprentice: 24, 24, 11, 24, 8, 3, 512, 10");
        check(billy.limited() && billy.sideLimit() == 24 && billy.castPause() == 10 && billy.maxQueued() == 0 && !billy.volatileAllowed()
                && billy.volatilePerSpell() == 0 && !billy.copiesContents() && billy.maxBlocks() == 0, "one spell at a time, never TNT, empty copies");
        check(wiccan.side() == 48 && wiccan.shapeRadius() == 23 && wiccan.repeatCopies() == 20 && wiccan.brush() == 6 && wiccan.magicSelect() == 4096
                && wiccan.undoSteps() == 16, "Wiccan: 48, 23, 20, 6, 4096, 16");
        check(!wanda.limited() && wanda.reach() == 256 && wanda.shapeRadius() == 512 && wanda.shapeHeight() == world.getYSpan() && wanda.brush() == 16
                && wanda.magicSelect() == 100000 && wanda.undoSteps() == 0 && wanda.repeatCopies() == Integer.MAX_VALUE,
                "the Scarlet Witch: reach 256, radius 512, the world's height, brush 16, select 100000, no undo count, no Repeat count");
        check(wanda.sideLimit() == 0 && wanda.side() >= world.getXSpan() && wanda.fits(world) && wanda.volatileAllowed() && wanda.volatilePerSpell() == 4096
                && wanda.maxQueued() == 4 && wanda.castPause() == 0 && wanda.copiesContents(), "her side is the world's; her queue, her TNT cap");
        check(billy.fits(new BoundingBox(0, 0, 0, 23, 23, 23)) && !billy.fits(new BoundingBox(0, 0, 0, 24, 0, 0)), "24 fits Billy, 25 does not");
        Vec3 eyes = new Vec3(0.5, 64.5, 0.5);
        check(billy.inReach(eyes, new BlockPos(0, 64, 24), false) && !billy.inReach(eyes, new BlockPos(0, 64, 25), false)
                && billy.inReach(eyes, new BlockPos(0, 64, 26), true) && !billy.inReach(eyes, new BlockPos(0, 64, 27), true),
                "reach 24 from the eyes, 26 with the server's slack");

        BlockPos outside = new BlockPos(40_000_000, 1000, -40_000_000);
        BlockPos corner = new BlockPos(world.maxX(), world.maxY(), world.minZ());
        List<BlockPos> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nine.add(i == 0 ? outside : new BlockPos(i, 64, 0));
        }
        SpellRecipe wild = new SpellRecipe(Spell.SHAPE, SpellRecipe.Op.CAST, 200, 7, outside, new BlockPos(3, -1000, 3), nine, outside, 9, -5, 9999, 7, 6,
                0xFF, 5, 42L, 3);
        SpellRecipe clamped = wild.clamped(billy);
        check(clamped.turn() == 3 && clamped.flip() == SpellRecipe.FLIP_Z && clamped.region() == SpellRecipe.BOX && clamped.style() == 0
                && clamped.shapeKind() == ShapeKind.BALL, "turn 7 is 3, flip 6 is 2, region 7 is a box, style 200 is the Ball");
        check(clamped.size() == 1 && clamped.size2() == 0, "a radius of -5 is 1, and a Ball has no height");
        check(clamped.flags() == (SpellRecipe.HOLLOW | SpellRecipe.MIX | SpellRecipe.PLANTED) && clamped.material() == SpellRecipe.PICKED
                && clamped.face() == SpellRecipe.NO_FACE && clamped.direction() == null, "unknown flags dropped, material 5 is 1, face 9 none");
        check(clamped.seeds().size() == 8 && clamped.seeds().get(0).equals(corner) && clamped.seeds().subList(1, 8).equals(nine.subList(1, 8)),
                "nine seeds are the first eight, the one outside moved inside");
        check(clamped.a().equals(corner) && clamped.anchor().equals(corner) && clamped.b().equals(new BlockPos(3, world.minY(), 3)),
                "positions outside the world come to its nearest cell");
        check(clamped.seed() == 42L && clamped.stroke() == 3 && clamped.spell() == Spell.SHAPE && clamped.op() == SpellRecipe.Op.CAST,
                "the seed, the stroke, the spell and the op are kept");
        check(clamped.clamped(billy).equals(clamped), "clamping twice is clamping once");

        // Each spell's numbers, for Billy, Wiccan and the Scarlet Witch.
        check(sized(Spell.SHAPE, 2, -5, -5, billy) == pair(1, 1) && sized(Spell.SHAPE, 2, 1000, 9999, billy) == pair(11, 24)
                && sized(Spell.SHAPE, 2, 1000, 9999, wiccan) == pair(23, 48) && sized(Spell.SHAPE, 2, 2000, 9999, wanda) == pair(512, world.getYSpan()),
                "a Tower: radius 1 to 11 and height 1 to 24 for Billy, 23 and 48 for Wiccan, 512 and the world's height for her");
        check(sized(Spell.PAINT, 0, -5, 7, billy) == pair(0, 0) && sized(Spell.PAINT, 0, 50, 0, billy) == pair(3, 0)
                && sized(Spell.PAINT, 0, 50, 0, wanda) == pair(16, 0) && sized(Spell.LAND, 0, 50, 0, wiccan) == pair(6, 0),
                "a brush: 0 to 3 for Billy, 16 for her, 6 for Wiccan");
        check(sized(Spell.PULL, 0, -5, 0, billy) == pair(-5, 0) && sized(Spell.PULL, 0, -100, 0, billy) == pair(-24, 0)
                && sized(Spell.PULL, 0, 100, 0, billy) == pair(24, 0) && sized(Spell.PULL, 0, -100, 0, wanda) == pair(-100, 0),
                "Pull & Push: up to his side either way; hers are the world's");
        check(sized(Spell.COPY, 0, -5, 0, billy) == pair(-5, 0) && sized(Spell.MOVE, 0, 1000, 0, billy) == pair(24, 0)
                && sized(Spell.COPY, 0, 1000, 0, wanda) == pair(256, 0), "a ghost's lift: within the reach");
        check(sized(Spell.REPEAT, 0, -5, -3, billy) == pair(1, 0) && sized(Spell.REPEAT, 0, 300, 99, billy) == pair(8, SpellRecipe.MAX_GAP)
                && sized(Spell.REPEAT, 0, 300, 2, wanda) == pair(300, 2), "a Repeat: 1 to 8 copies for Billy, 300 for her (no count), a gap of 0 to 16");
        check(sized(Spell.FILL, 9, 7, 7, billy) == pair(0, 0) && sized(Spell.VANISH, 9, 7, 7, wanda) == pair(0, 0), "a size Fill does not use is 0");

        SpellRecipe tidy = new SpellRecipe(Spell.FILL, SpellRecipe.Op.CAST, FillStyle.ROOM.ordinal(), SpellRecipe.BOX, new BlockPos(1, 64, 1),
                new BlockPos(5, 68, 5), List.of(new BlockPos(2, 64, 2)), new BlockPos(3, 64, 3), Direction.UP.get3DDataValue(), 0, 0, 1,
                SpellRecipe.NO_FLIP, SpellRecipe.HOLLOW, SpellRecipe.HAND, 7L, 0);
        check(tidy.clamped(billy).equals(tidy) && tidy.clamped(wanda).equals(tidy), "a recipe in range is unchanged");
        // A tower sent with a height of 0 is the same tower whether its recipe is clamped (the server) or not (a ghost).
        for (int height : new int[]{0, ShapeKind.TOWER.defaultHeight(3)}) {
            SpellRecipe sent = BuildTestSupport.recipe(Spell.SHAPE).style(ShapeKind.TOWER).size(3).size2(height).at(new BlockPos(0, 63, 0), Direction.UP).build();
            Plan ghost = SpellPlanner.plan(new World(), sent, Setup.at(CASTER).withMaterial(STONE));
            Plan cast = SpellPlanner.plan(new World(), sent.clamped(billy), Setup.at(CASTER).withMaterial(STONE));
            check(ghost.bounds() != null && ghost.bounds().equals(cast.bounds()) && ghost.size() == cast.size()
                    && ghost.bounds().getYSpan() == Math.max(1, height), "a tower sent " + height + " high: the ghost is the cast, " + ghost.bounds());
        }
        helper.succeed();
    }

    /** The two sizes of a recipe of {@code spell} (style {@code style}) with sizes {@code size, size2}, clamped. */
    private static long sized(Spell spell, int style, int size, int size2, Limits limits) {
        SpellRecipe recipe = new SpellRecipe(spell, SpellRecipe.Op.CAST, style, SpellRecipe.BOX, BlockPos.ZERO, BlockPos.ZERO, List.of(), BlockPos.ZERO,
                SpellRecipe.NO_FACE, size, size2, 0, 0, 0, 0, 0L, 0).clamped(limits);
        return pair(recipe.size(), recipe.size2());
    }

    private static long pair(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    /**
     * Billy's magic (D-B 8.1 to 8.3), as arithmetic over his data: 512 at the start, 372 after 140, 388 a second later;
     * 100 in the meter and a spell of 140 is refilling with 3 seconds to wait, one of 600 too big (600 of 512); Mom
     * halves the cost, rounded up; an undo gives back, never above the top; 1,990 practice and 20 more make him a
     * Spellcaster with his new top full and a side of 32, an undo takes the 20 back and the rank stays; a jump of two
     * ranks at once; an operator's rank; the pause after a spell; what he is told.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void billysMagicPaysRefillsAndLearns(GameTestHelper helper) {
        BuildData fresh = BuildData.EMPTY;
        check(BillyMagic.meter(fresh, 0L) == 512 && BillyMagic.top(fresh) == 512 && BillyMagic.verdict(fresh, 0L, 512) == SpellPlanner.NONE,
                "512 at the start, enough for 512");
        BuildData paid = BillyMagic.pay(fresh, 1000L, 140);
        check(BillyMagic.meter(paid, 1000L) == 372 && BillyMagic.meter(paid, 1020L) == 388, "372 after 140, 388 a second later");
        check(BillyMagic.meter(paid, 1000L + 20 * 9) == 512 && BillyMagic.meter(paid, 1_000_000L) == 512, "full again after 9 seconds, never above");

        BuildData low = fresh.withMeter(100.0, 2000L);
        check(BillyMagic.verdict(low, 2000L, 140) == BuildVerdictPayload.REFILLING && BillyMagic.secondsToPay(low, 2000L, 140) == 3,
                "100 for a spell of 140: refilling, 3 seconds");
        check(said(BillyMagic.cannotPay(low, 2000L, 140), "message.enhanced.build.refilling", 3), "your magic is refilling: 3 more seconds");
        check(BillyMagic.verdict(low, 2000L, 100) == SpellPlanner.NONE && BillyMagic.cannotPay(low, 2000L, 100) == null
                && BillyMagic.verdict(low, 2050L, 140) == SpellPlanner.NONE, "100 pays 100; 2.5 seconds later 140 too");
        check(BillyMagic.verdict(low, 2000L, 600) == BuildVerdictPayload.TOO_BIG && said(BillyMagic.cannotPay(low, 2000L, 600),
                "message.enhanced.build.too_big", 600, 512), "600 is bigger than his magic yet: 600 of 512");

        check(BillyMagic.cost(100, true) == 50 && BillyMagic.cost(101, true) == 51 && BillyMagic.cost(100, false) == 100
                && BillyMagic.cost(0, true) == 0, "near Mom 100 cost 50 and 101 cost 51; far, 100");
        BuildData undone = BillyMagic.refund(BillyMagic.pay(fresh, 0L, 27), 0L, 27);
        check(BillyMagic.meter(undone, 0L) == 512 && BillyMagic.meter(BillyMagic.refund(paid, 1000L, 1000), 1000L) == 512,
                "an undo gives the 27 back; never above the top");
        check(BillyMagic.meter(BillyMagic.setMeter(fresh, 0L, 10), 0L) == 10 && BillyMagic.meter(BillyMagic.setMeter(fresh, 0L, 99999), 0L) == 512,
                "an operator's meter, within 0 and the top");

        BuildData practised = BillyMagic.pay(fresh.withPractice(1990L), 3000L, 20);
        BillyMagic.Practiced learned = BillyMagic.practise(practised, 3000L, 20);
        check(learned.rankedUp() && learned.data().rank() == 2 && learned.data().practice() == 2010L, "1,990 and 20 more: rank 2");
        check(BillyMagic.top(learned.data()) == 1024 && BillyMagic.meter(learned.data(), 3000L) == 1024, "his new top, 1,024, full as a gift");
        check(Limits.billy(BillyRanks.rank(learned.data()), Limits.world(helper.getLevel())).side() == 32, "and a side of 32");
        BuildData forgot = BillyMagic.unpractise(learned.data(), 20);
        check(forgot.practice() == 1990L && forgot.rank() == 2, "an undo takes the 20 practice back, the rank stays 2");
        BillyMagic.Practiced again = BillyMagic.practise(forgot, 3100L, 5);
        check(!again.rankedUp() && again.data().rank() == 2, "practice under the next threshold is no rank-up");
        BillyMagic.Practiced leap = BillyMagic.practise(fresh, 0L, 9000);
        check(leap.rankedUp() && leap.data().rank() == 3 && BillyMagic.meter(leap.data(), 0L) == 2048, "9,000 at once: a Sorcerer, 2,048 full");
        check(BillyMagic.unpractise(fresh, 50).practice() == 0L, "practice never goes below 0");

        BuildData sorcerer = BillyMagic.setRank(fresh, 0L, 3);
        check(sorcerer.rank() == 3 && sorcerer.practice() == 8000L && BillyMagic.meter(sorcerer, 0L) == 2048, "an operator's rank 3: 2,048");
        BuildData back = BillyMagic.setRank(sorcerer, 0L, 1);
        check(back.rank() == 1 && back.practice() == 0L && !BillyMagic.practise(back, 0L, 10).rankedUp(), "and back to 1, which his next spell keeps");

        BuildData resting = BillyMagic.pauseAfter(fresh, 4000L, 10);
        check(BillyMagic.paused(resting, 4009L) && !BillyMagic.paused(resting, 4010L), "the pause: 10 ticks after his spell");
        check(BillyMagic.saysMomAgain(0L, 1200L) && !BillyMagic.saysMomAgain(100L, 1200L), "Mom is near: once a minute");
        check(said(BillyMagic.rankUp(2), "message.enhanced.build.rank_up", 2) && ((TranslatableContents) BillyMagic.rankUp(2).getContents()).getArgs()[2]
                .equals(1024), "you learned more magic! Rank 2, Spellcaster, 1024");
        check(said(BillyMagic.waitForSpell(), "message.enhanced.build.wait") && said(BillyMagic.tooDangerous(), "message.enhanced.build.volatile")
                && said(BillyMagic.forgotten(), "message.enhanced.build.forgotten") && said(BillyMagic.momNear(), "message.enhanced.build.mom")
                && said(Limits.tooWide(24), "message.enhanced.build.too_wide", 24), "and the rest of what he is told");
        helper.succeed();
    }

    /**
     * Whose numbers a player has ({@code Limits.of}): the Scarlet Witch's, Billy's by the rank in his data, Billy's in
     * Creative too (with {@code limitsInCreative} on, the default), nobody else's; and Mom's help: the nearest Scarlet
     * Witch within 16 blocks, not a Spectator, not one 24 blocks away.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_pure")
    public static void everyBuilderHasHerNumbers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(2.5, TestKit.FLOOR, 2.5, Power.BILLY);
        ServerPlayer wanda = f.player(7.5, TestKit.FLOOR, 2.5, Power.SCARLET_WITCH);
        ServerPlayer magneto = f.player(10.5, TestKit.FLOOR, 10.5, Power.MAGNETO);
        Limits his = Limits.of(billy);
        Limits hers = Limits.of(wanda);
        check(his != null && his.billy() && his.limited() && his.side() == 24 && his.rank() == 1, "Billy at rank 1: 24");
        check(hers != null && hers.hero() == Power.SCARLET_WITCH && !hers.limited() && hers.reach() == 256, "the Scarlet Witch: no limit, reach 256");
        check(Limits.of(magneto) == null && Limits.of(f.player(12.5, TestKit.FLOOR, 12.5)) == null, "Magneto and a powerless player have none");
        check(hers.inWorld(new BlockPos(0, 5000, 0)).getY() == helper.getLevel().getMaxBuildHeight() - 1, "her world ends at the build height");
        billy.setData(ModAttachments.BUILD_DATA, BuildData.EMPTY.withRank(3));
        Limits sorcerer = Limits.of(billy);
        check(sorcerer != null && sorcerer.side() == 40 && sorcerer.magicSelect() == 2048, "a Sorcerer: side 40, a double click of 2,048");
        billy.setGameMode(GameType.CREATIVE);
        Limits creative = Limits.of(billy);
        check(creative != null && creative.limited() && creative.side() == 40, "in Creative his limits still apply");

        check(BillyMagic.mom(billy) == wanda, "a Scarlet Witch 5 blocks away helps him");
        ServerPlayer nearer = f.player(4.5, TestKit.FLOOR, 2.5, Power.SCARLET_WITCH);
        check(BillyMagic.mom(billy) == nearer, "the nearest one");
        nearer.setGameMode(GameType.SPECTATOR);
        check(BillyMagic.mom(billy) == wanda, "not a Spectator");
        wanda.moveTo(helper.absoluteVec(new Vec3(19.5, TestKit.FLOOR, 19.5)));
        check(BillyMagic.mom(billy) == null, "24 blocks away she is too far: " + wanda.distanceTo(billy));
        helper.succeed();
    }

    /** Whether {@code message} is the lang key {@code key} with these first arguments. */
    private static boolean said(@Nullable Component message, String key, Object... args) {
        if (message == null || !(message.getContents() instanceof TranslatableContents translatable) || !translatable.getKey().equals(key)) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (translatable.getArgs().length <= i || !args[i].equals(translatable.getArgs()[i])) {
                return false;
            }
        }
        return true;
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
