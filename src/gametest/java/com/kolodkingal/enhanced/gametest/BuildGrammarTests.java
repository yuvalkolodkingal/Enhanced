package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.BuildGrammar.Aim;
import com.kolodkingal.enhanced.build.BuildGrammar.Result;
import com.kolodkingal.enhanced.build.BuildHints;
import com.kolodkingal.enhanced.build.BuildHints.Input;
import com.kolodkingal.enhanced.build.BuildHints.Row;
import com.kolodkingal.enhanced.build.BuildHints.Stage;
import com.kolodkingal.enhanced.build.FillStyle;
import com.kolodkingal.enhanced.build.GrowStyle;
import com.kolodkingal.enhanced.build.LandStyle;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.build.ShapeKind;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.VanishStyle;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Build Magic's input as pure code (lane BC, D-B 19.1 B9 and B10): the mouse grammar ({@link BuildGrammar}) driven
 * click by click, drag by drag and scroll by scroll with made-up aims, and the hint strip ({@link BuildHints}) for each
 * spell and state. Nothing here touches the level (the hero's numbers are read for a world as big as the test's), so
 * the batch finishes in its first tick; no client class is named.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildGrammarTests {
    private static final Vec3 EYES = new Vec3(0.5, 70.62, 0.5);
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 WEST = new Vec3(-1.0, 0.0, 0.0);

    // B9

    /**
     * The mouse grammar (D-B 2.3, 3): two clicks make a box and a third a new corner; a press let go on another block 4
     * ticks later is a box at once, a quicker one is not; a double click selects the thing (a seed), and one on another
     * thing adds it while one on the same thing adds nothing; a scroll on the box's top pushes it up one (Ctrl: 8), never
     * past the opposite face, and from inside the box the far face moves; Billy's face and rubber band stop at his side
     * (24); the sky lets go of a box, and with nothing selected marks corner A on the floating cell 6 blocks ahead; a
     * shape follows, is planted by a click, takes its height from the look, keeps it once a second click fixes it, and a
     * click on the sky then lets it follow again; each state casts the recipe it shows.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_grammar")
    public static void theMouseGrammar(GameTestHelper helper) {
        BoundingBox world = Limits.world(helper.getLevel());
        Limits wanda = Limits.wanda(world);
        Limits billy = Limits.billy(1, world);
        BlockPos p = new BlockPos(0, 64, 0);
        BlockPos q = new BlockPos(4, 66, 3);

        // Two clicks a box; a third a new corner.
        BuildGrammar g = new BuildGrammar(wanda);
        check(g.stage() == Stage.NOTHING && g.recipe(0) == null, "nothing selected: nothing to cast");
        check(g.press(on(p), 0) == Result.CORNER_A && g.release(on(p), 1) == Result.NOTHING, "the first click is corner A");
        check(g.stage() == Stage.CORNER && g.rubberBand(), "then the box follows the aim");
        g.aim(on(q));
        check(box(g).equals(new BoundingBox(0, 64, 0, 4, 66, 3)), "the rubber band reaches the aimed cell: " + box(g));
        check(g.recipe(0) == null, "a rubber band is not a box to cast on yet");
        check(g.press(on(q), 20) == Result.CORNER_B && g.release(on(q), 21) == Result.NOTHING, "the second click is corner B");
        g.aim(on(new BlockPos(9, 64, 9)));
        check(box(g).equals(new BoundingBox(0, 64, 0, 4, 66, 3)) && g.finished() && g.stage() == Stage.BOX, "the box stays fixed: " + box(g));
        SpellRecipe fill = g.recipe(0);
        check(fill != null && fill.spell() == Spell.FILL && fill.op() == SpellRecipe.Op.CAST && fill.region() == SpellRecipe.BOX
                && fill.a().equals(p) && fill.b().equals(q) && fill.seeds().isEmpty(), "the box is Fill's: " + fill);
        check(g.press(on(new BlockPos(10, 64, 10)), 40) == Result.CORNER_A && g.rubberBand()
                && g.cornerA().equals(new BlockPos(10, 64, 10)), "a third click starts a new box");

        // A drag; too quick a one is a click.
        g = new BuildGrammar(wanda);
        g.press(on(p), 0);
        g.aim(on(new BlockPos(5, 64, 2)));
        check(g.release(on(new BlockPos(5, 64, 2)), 6) == Result.DRAGGED && box(g).equals(new BoundingBox(0, 64, 0, 5, 64, 2)) && g.finished(),
                "pressed on one block and let go on another 6 ticks later: the box at once");
        g = new BuildGrammar(wanda);
        g.press(on(p), 0);
        check(g.release(on(new BlockPos(5, 64, 2)), 2) == Result.NOTHING && g.rubberBand(), "let go 2 ticks later: still a click");

        // A double click: a seed; on another thing: two seeds; on the same thing again: nothing more.
        BlockPos tree = new BlockPos(3, 65, 3);
        BlockPos rock = new BlockPos(-6, 64, 2);
        g = new BuildGrammar(wanda);
        g.press(on(tree), 0);
        g.release(on(tree), 1);
        check(g.press(on(tree), 5) == Result.SELECTED && g.seeds().equals(List.of(tree)) && g.box() == null && g.finished()
                && g.stage() == Stage.BOX, "a double click selects the whole thing: " + g.seeds());
        g.release(on(tree), 6);
        SpellRecipe mask = g.recipe(0);
        check(mask != null && mask.region() == SpellRecipe.MASK && mask.seeds().equals(List.of(tree)), "it casts on the thing: " + mask);
        g.press(on(rock), 30);
        g.release(on(rock), 31);
        check(g.press(on(rock), 34) == Result.ADDED && g.seeds().equals(List.of(tree, rock)), "a double click outside it adds that thing");
        g.release(on(rock), 35);
        g.press(on(tree), 60);
        check(g.press(on(tree), 62) == Result.NOTHING && g.seeds().equals(List.of(tree, rock)), "one on the thing again adds nothing");
        g.press(on(rock), 80);
        check(g.press(on(rock), 89) == Result.CORNER_B && g.finished() && !g.hasMask() && box(g).equals(BoundingBox.fromCorners(rock, rock)),
                "two clicks 9 ticks apart are two corners, not a double click");

        // A scroll on the +Y face pushes it, Ctrl 8; never past the opposite face; off the box, it is the hotbar's.
        g = boxed(new BuildGrammar(wanda), p, q);
        Aim fromAbove = Aim.sky(new Vec3(2.5, 80.0, 2.0), DOWN);
        check(g.scroll(1, false, false, fromAbove) == Result.PUSHED && box(g).maxY() == 67, "scroll up on the top: one higher");
        check(g.scroll(1, true, false, fromAbove) == Result.PUSHED && box(g).maxY() == 75, "Ctrl + scroll: 8 at a time");
        check(g.scroll(-2, false, false, fromAbove) == Result.PUSHED && box(g).maxY() == 73, "scroll down pulls it in");
        check(g.scroll(-100, false, false, fromAbove) == Result.HELD_BACK && box(g).maxY() == 64 && box(g).minY() == 64,
                "a face never passes the opposite one: " + box(g));
        check(g.scroll(1, false, false, Aim.sky(new Vec3(2.5, 80.0, 2.0), new Vec3(0.0, 1.0, 0.0))) == Result.UNUSED
                && box(g).maxY() == 64, "looking away from the box, the wheel is the hotbar's");
        g = boxed(new BuildGrammar(wanda), p, q);
        check(BuildGrammar.faceLookedAt(box(g), new Vec3(2.0, 65.5, 1.5), EAST) == Direction.EAST
                && g.scroll(2, false, false, Aim.sky(new Vec3(2.0, 65.5, 1.5), EAST)) == Result.PUSHED && box(g).maxX() == 6,
                "from inside the box, the far face moves: " + box(g));
        check(BuildGrammar.faceLookedAt(box(g), new Vec3(-5.0, 65.5, 1.5), EAST) == Direction.WEST, "from outside, the near face");
        g.spell(Spell.SHAPE);
        g.spell(Spell.CHANGE);
        check(box(g).maxX() == 6 && g.finished(), "choosing another spell keeps the box");

        // Billy's face and his rubber band stop at his side.
        g = boxed(new BuildGrammar(billy), p, new BlockPos(4, 64, 4));
        check(g.scroll(5, true, false, Aim.sky(new Vec3(30.5, 64.5, 2.5), WEST)) == Result.HELD_BACK && box(g).getXSpan() == 24,
                "Billy's face stops at 24 blocks: " + box(g));
        g = new BuildGrammar(billy);
        g.press(on(p), 0);
        g.aim(on(new BlockPos(40, 64, -3)));
        check(box(g).equals(new BoundingBox(0, 64, -3, 23, 64, 0)) && g.heldBack(), "his rubber band holds at his side: " + box(g));
        g.press(on(new BlockPos(40, 64, -3)), 10);
        check(box(g).getXSpan() == 24 && g.finished(), "and the box he fixes is no wider: " + box(g));

        // The sky lets go; with nothing selected, it marks corner A on the floating cell 6 ahead.
        g = boxed(new BuildGrammar(wanda), p, q);
        check(g.press(Aim.sky(EYES, EAST), 40) == Result.LET_GO && !g.finished() && g.box() == null && g.stage() == Stage.NOTHING,
                "a click on the sky lets go of the box");
        Aim ahead = Aim.sky(new Vec3(0.5, 70.5, 0.5), EAST);
        check(ahead.floating().equals(new BlockPos(6, 70, 0)), "the floating cell is 6 blocks ahead: " + ahead.floating());
        check(g.press(ahead, 60) == Result.CORNER_A && g.cornerA().equals(new BlockPos(6, 70, 0)), "nothing selected: corner A in the air");
        g.aim(Aim.sky(new Vec3(0.5, 70.5, 0.5), new Vec3(0.0, 0.0, 1.0)));
        check(box(g).equals(new BoundingBox(0, 70, 0, 6, 70, 6)), "the rubber band follows the floating cell: " + box(g));
        check(g.press(Aim.sky(new Vec3(0.5, 70.5, 0.5), new Vec3(0.0, 0.0, 1.0)), 80) == Result.CORNER_B && g.finished(),
                "corner A set: corner B in the air");

        // A shape: it follows, a click plants it, the look sets its height, a second click fixes it, the sky lets it go.
        g = new BuildGrammar(wanda);
        g.spell(Spell.SHAPE);
        g.style(ShapeKind.TOWER.ordinal());
        check(g.stage() == Stage.FOLLOWING && g.radius() == BuildGrammar.FIRST_RADIUS && g.height() == 7 && g.hollow(),
                "a hollow tower of radius 3 is 7 high until planted");
        check(g.scroll(1, false, false, on(p)) == Result.NUMBER && g.radius() == 4 && g.height() == 9, "the wheel makes it bigger");
        SpellRecipe following = g.recipe(0);
        check(following != null && !following.planted() && following.anchor().equals(p) && following.direction() == Direction.UP
                && following.size() == 4 && following.size2() == 9 && following.hollow(), "it stands on the looked face: " + following);
        check(g.press(on(p), 100) == Result.PLANTED && g.planted() && g.stage() == Stage.PLANTED, "a click plants it");
        // The ray from (10.5, 65.62, 0.5) toward (0.5, 70.5, 0.5) meets the tower's axis 70.5 high; its base is at 65.
        Aim up = Aim.sky(new Vec3(10.5, 65.62, 0.5), new Vec3(-10.0, 4.88, 0.0).normalize());
        g.aim(up);
        check(g.height() == 6, "looking up at its axis, the tower is 6 high: " + g.height());
        check(g.press(up, 120) == Result.FIXED && g.heightFixed() && g.stage() == Stage.FIXED,
                "a second click fixes the height, even against the sky");
        g.aim(Aim.sky(new Vec3(10.5, 65.62, 0.5), new Vec3(-10.0, 20.0, 0.0).normalize()));
        SpellRecipe tower = g.recipe(0);
        check(g.height() == 6 && tower != null && tower.planted() && tower.anchor().equals(p) && tower.direction() == Direction.UP
                && tower.size2() == 6, "fixed, it keeps its height wherever he looks: " + tower);
        check(g.press(Aim.sky(EYES, EAST), 140) == Result.LET_GO && !g.planted() && g.stage() == Stage.FOLLOWING,
                "then a click on the sky lets it follow the crosshair again");
        g.style(ShapeKind.BALL.ordinal());
        check(g.press(on(p), 160) == Result.PLANTED && g.press(on(q), 170) == Result.PLANTED && g.recipe(0).anchor().equals(q),
                "a planted ball is planted again where he clicks");

        // Copy: the box, then the ghost; Ctrl + R turns it, Ctrl + F flips it, the wheel lifts it.
        g = boxed(new BuildGrammar(wanda), p, q);
        g.spell(Spell.COPY);
        check(g.recipe(0).op() == SpellRecipe.Op.TAKE, "Copy's first right click takes the box");
        g.holdGhost();
        check(g.stage() == Stage.GHOST && g.turnGhost() && g.flipGhost(EAST)
                && g.scroll(2, false, false, on(p)) == Result.NUMBER && g.scroll(1, false, true, on(p)) == Result.STYLE, "the ghost turns");
        SpellRecipe stamp = g.recipe(0);
        check(stamp.op() == SpellRecipe.Op.STAMP && stamp.turn() == 2 && stamp.flip() == SpellRecipe.FLIP_X && stamp.size() == 2
                && stamp.anchor().equals(p), "then it stamps the ghost where he looks: " + stamp);
        check(g.press(on(q), 200) == Result.CORNER_A && !g.ghost(), "a left click starts a new box and the ghost hides");
        helper.succeed();
    }

    // B10

    /**
     * The hint strip (D-B 6.4): Fill with a box shows fill, push a side, its style and undo; a removal over 64 "hold:
     * vanish"; Copy holding a ghost stamp, up / down, turn, flip; a planted tower place, look up, done, and once fixed only
     * place and let go; Shapes following its shape; a working spell "stop and undo" (all Billy sees; the Scarlet Witch's
     * other rows stay); nothing selected the corner, the whole thing, the spells and undo only when there is one; the
     * brushes have no corner line; and every row's words are in both languages.
     */
    @GameTest(template = TestKit.ARENA, batch = "build_grammar")
    public static void theHintsMatchTheSpellAndState(GameTestHelper helper) throws IOException {
        List<Row> fill = BuildHints.lines(Spell.FILL, FillStyle.ROOM.ordinal(), Stage.BOX, Power.BILLY, false, true);
        check(keys(fill).equals(List.of("fill", "push_side", "style", "undo")) && fill.get(0).input() == Input.RIGHT
                && argument(fill.get(2).text()).equals(FillStyle.ROOM.key()), "Fill with a box: " + keys(fill));
        List<Row> hold = BuildHints.lines(Spell.VANISH, VanishStyle.ALL.ordinal(), Stage.BOX_HOLD, Power.SCARLET_WITCH, false, true);
        check(keys(hold).equals(List.of("held", "push_side", "let_go")) && hold.get(0).input() == Input.RIGHT_HELD
                && argument(hold.get(0).text()).equals("hud.enhanced.build.hint.vanish"), "a removal over 64: hold: vanish " + keys(hold));
        List<Row> ghost = BuildHints.lines(Spell.COPY, 0, Stage.GHOST, Power.BILLY, false, true);
        check(keys(ghost).equals(List.of("stamp", "lift", "turn", "flip")) && ghost.get(3).input() == Input.FLIP_KEY,
                "Copy holding a ghost: " + keys(ghost));
        check(keys(BuildHints.lines(Spell.MOVE, 0, Stage.GHOST, Power.BILLY, false, true)).get(0).equals("put_down"), "Move puts down");
        check(keys(BuildHints.lines(Spell.MOVE, 0, Stage.BOX, Power.BILLY, false, true)).equals(List.of("pick_up", "push_side")),
                "Move with a box picks it up");
        List<Row> planted = BuildHints.lines(Spell.SHAPE, ShapeKind.TOWER.ordinal(), Stage.PLANTED, Power.BILLY, false, true);
        check(keys(planted).equals(List.of("place", "taller", "done", "let_go")) && planted.get(1).input() == Input.LOOK,
                "a planted tower: " + keys(planted));
        check(keys(BuildHints.lines(Spell.SHAPE, ShapeKind.TOWER.ordinal(), Stage.FIXED, Power.BILLY, false, true))
                .equals(List.of("place", "let_go")), "its height fixed: place, let go");
        check(keys(BuildHints.lines(Spell.SHAPE, ShapeKind.BALL.ordinal(), Stage.PLANTED, Power.BILLY, false, true))
                .equals(List.of("place", "let_go")), "a planted ball has no height to look for");
        List<Row> following = BuildHints.lines(Spell.SHAPE, ShapeKind.TOWER.ordinal(), Stage.FOLLOWING, Power.BILLY, false, true);
        check(keys(following).equals(List.of("place", "size", "shape", "plant")) && argument(following.get(2).text()).equals(ShapeKind.TOWER.key()),
                "Shapes following: " + keys(following));
        check(keys(BuildHints.lines(Spell.FILL, 0, Stage.BOX, Power.BILLY, true, true)).equals(List.of("stop")),
                "Billy's spell at work: stop and undo, and nothing else");
        check(keys(BuildHints.lines(Spell.FILL, 0, Stage.BOX, Power.SCARLET_WITCH, true, true)).equals(List.of("fill", "push_side", "style", "stop")),
                "hers at work: she goes on building, and Ctrl + Z stops it");
        check(keys(BuildHints.lines(Spell.FILL, 0, Stage.NOTHING, Power.BILLY, false, true)).equals(List.of("corner", "whole", "spells", "undo"))
                && keys(BuildHints.lines(Spell.VANISH, 0, Stage.NOTHING, Power.BILLY, false, false)).equals(List.of("corner", "whole", "spells")),
                "nothing selected: the corner, the whole thing, the spells, and undo when there is one");
        check(keys(BuildHints.lines(Spell.CHANGE, 0, Stage.CORNER, Power.BILLY, false, true)).equals(List.of("corner2", "in_air")),
                "corner A set: the second corner, or in the air");
        check(keys(BuildHints.lines(Spell.REPEAT, 0, Stage.BOX, Power.BILLY, false, true)).equals(List.of("repeat", "copies", "gap")), "Repeat");
        check(keys(BuildHints.lines(Spell.PULL, 0, Stage.NOTHING, Power.BILLY, false, true)).equals(List.of("pull", "layers")), "Pull & Push");
        List<List<Row>> brushes = List.of(BuildHints.lines(Spell.PAINT, 0, Stage.NOTHING, Power.BILLY, false, true),
                BuildHints.lines(Spell.LAND, LandStyle.SMOOTH.ordinal(), Stage.NOTHING, Power.BILLY, false, true),
                BuildHints.lines(Spell.GROW, GrowStyle.BLOOM.ordinal(), Stage.NOTHING, Power.BILLY, false, true));
        for (List<Row> brush : brushes) {
            check(!keys(brush).contains("corner") && !keys(brush).contains("corner2") && brush.get(0).input() == Input.RIGHT_HELD
                    && keys(brush).contains("brush"), "a brush has no corner line: " + keys(brush));
        }
        check(keys(brushes.get(0)).equals(List.of("paint", "brush")) && keys(brushes.get(1)).equals(List.of("raise", "brush", "style"))
                && argument(brushes.get(1).get(2).text()).equals(LandStyle.SMOOTH.key()), "Paint has no style; Land names its own");

        // Every row of every spell and state has its words in both languages.
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        JsonObject en = lang(mod, "en_us");
        JsonObject he = lang(mod, "he_il");
        for (Spell spell : Spell.values()) {
            for (Stage stage : Stage.values()) {
                for (int style = 0; style < spell.styles(); style++) {
                    for (Power hero : List.of(Power.SCARLET_WITCH, Power.BILLY)) {
                        for (boolean working : new boolean[]{false, true}) {
                            List<Row> rows = BuildHints.lines(spell, style, stage, hero, working, true);
                            check(!rows.isEmpty() && rows.size() <= 5, spell + " " + stage + ": 1 to 5 rows, not " + rows.size());
                            for (Row row : rows) {
                                for (String key : List.of(row.key(), argument(row.text()))) {
                                    check(key.isEmpty() || en.has(key) && he.has(key), "the hint " + key + " is in both languages");
                                }
                            }
                        }
                    }
                }
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------

    /** Looking at the top of {@code cell}, from 6 blocks above it. */
    private static Aim on(BlockPos cell) {
        return new Aim(cell, Direction.UP, cell, Vec3.atCenterOf(cell).add(0.0, 6.0, 0.0), DOWN);
    }

    /** {@code grammar} with the box {@code a}-{@code b} marked by two clicks. */
    private static BuildGrammar boxed(BuildGrammar grammar, BlockPos a, BlockPos b) {
        grammar.press(on(a), -100);
        grammar.release(on(a), -99);
        grammar.press(on(b), -80);
        grammar.release(on(b), -79);
        check(grammar.finished() && box(grammar).equals(BoundingBox.fromCorners(a, b)), "two clicks mark the box " + a + " to " + b);
        return grammar;
    }

    private static BoundingBox box(BuildGrammar grammar) {
        BoundingBox box = grammar.box();
        check(box != null, "a box is shown");
        return box;
    }

    /** The short keys of the rows' words ({@code hud.enhanced.build.hint.} left out). */
    private static List<String> keys(List<Row> rows) {
        return rows.stream().map(row -> row.key().replace("hud.enhanced.build.hint.", "")).toList();
    }

    /** The lang key of a row's first argument (a style, a shape, a verb), or "" for none. */
    private static String argument(Component text) {
        if (text.getContents() instanceof TranslatableContents contents && contents.getArgs().length > 0
                && contents.getArgs()[0] instanceof Component argument && argument.getContents() instanceof TranslatableContents inner) {
            return inner.getKey();
        }
        return "";
    }

    private static JsonObject lang(IModFile mod, String name) throws IOException {
        Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
