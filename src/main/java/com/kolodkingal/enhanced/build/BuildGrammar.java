package com.kolodkingal.enhanced.build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * The one mouse grammar of Build Magic (D-B 2.3, 3, 4), as a state machine of pure code: what a builder is about to
 * cast, and how each click, drag, double click and scroll changes it. LEFT is <i>where</i> (two corners make a box, a
 * third starts a new one, a drag makes one at once, a double click selects the whole thing, the sky lets go or marks a
 * corner in the air, a shape is planted and its height taken from the look), the scroll is the main number (a side of
 * the box pushed out or in, a shape's size, a ghost's lift, the copies, the layers, the brush) and Alt + scroll the
 * second knob (a style, a shape, a turn, a gap). No Sneak anywhere. It ends in the {@link SpellRecipe} the right button
 * casts.
 *
 * <p>The client feeds it (its {@code BuildInput}, every tick and at every click) and keeps it; GameTests drive it
 * directly (B9). It names no client class and reads no world: the client tells it where the builder looks
 * ({@link Aim}, from its own raycasts), which cells the selected thing holds ({@link #selectedBy}) and its bounds once
 * its flood is done ({@link #maskBounds(BoundingBox)}), and the hero's numbers ({@link RecipeLimits}): Billy's longest
 * side stops a rubber band and a pushed face (he can never mark a box he could not cast), and every corner stays inside
 * the world.
 *
 * <p>Times are the client's ticks, as the caller counts them: two clicks on one block within
 * {@value #DOUBLE_CLICK_TICKS} ticks are a double click; a press let go on another block at least
 * {@value #DRAG_TICKS} ticks later is a drag.
 */
public final class BuildGrammar {
    /** Two left clicks on the same block within this many ticks are a double click: magic select (D-B 14.7). */
    public static final int DOUBLE_CLICK_TICKS = 8;
    /** A left press let go on another block at least this many ticks later drags a box (D-B 14.7). */
    public static final int DRAG_TICKS = 4;
    /** How far in front of the eyes the floating aim cell is, when the builder looks at the sky (D-B 3.1). */
    public static final int FLOATING_CELL = 6;
    /** How much one notch of Ctrl + scroll counts (D-B 2.3). */
    public static final int CTRL_STEP = 8;
    /** How many copies one notch of Ctrl + scroll adds to a Repeat (D-B 4.10). */
    public static final int REPEAT_CTRL_STEP = 4;
    /** A shape's radius when the builder first chooses Shapes. */
    public static final int FIRST_RADIUS = 3;
    /** A brush's radius when the builder first chooses one. */
    public static final int FIRST_BRUSH = 1;

    /** What one input did, for the sound and the tip the client plays with it (D-B 6.7, 6.8). */
    public enum Result {
        /** Nothing changed (a left click of a brush, a double click inside the selected thing). */
        NOTHING,
        /** A scroll the grammar has no use for: it stays the hotbar's. */
        UNUSED,
        /** The first corner of a box. */
        CORNER_A,
        /** The second corner: the box is fixed. */
        CORNER_B,
        /** A drag made a box at once. */
        DRAGGED,
        /** A double click selected the whole thing. */
        SELECTED,
        /** A double click added another thing to the selection. */
        ADDED,
        /** The box, the selected thing, a planted shape or a ghost was let go. */
        LET_GO,
        /** A shape was planted where the builder clicked. */
        PLANTED,
        /** A planted shape's height, which followed the look, is fixed. */
        FIXED,
        /** A face of the box moved. */
        PUSHED,
        /** Billy's longest side (or the edge of the world) stopped it: the soft "tock". */
        HELD_BACK,
        /** A number changed: a size, the copies, a lift, the layers, the gap, the brush. */
        NUMBER,
        /** The second knob turned: a style, a shape, a turn. */
        STYLE
    }

    /**
     * Where the builder looks, from the client's raycasts within the hero's reach (D-B 3.1).
     *
     * @param cell   the aimed cell: the first block the view meets ({@code COLLIDER}, so grass and torches are looked
     *               through), or null when it meets none within reach (the sky)
     * @param face   the face of {@code cell} the view meets (null with no cell)
     * @param picked the block the picking ray meets ({@code OUTLINE}: a flower or a rail is picked as what it is), for
     *               "this kind", the brushes and middle click; null when none is in reach
     * @param eyes   the eyes
     * @param look   the view direction (a unit vector)
     */
    public record Aim(@Nullable BlockPos cell, @Nullable Direction face, @Nullable BlockPos picked, Vec3 eyes, Vec3 look) {
        public Aim {
            cell = cell == null ? null : cell.immutable();
            face = cell == null ? null : face;
            picked = picked == null ? null : picked.immutable();
        }

        /** Looking at the sky: no block within reach. */
        public static Aim sky(Vec3 eyes, Vec3 look) {
            return new Aim(null, null, null, eyes, look);
        }

        /** Whether the view meets a block within reach. */
        public boolean onBlock() {
            return cell != null;
        }

        /**
         * The floating aim cell, {@value #FLOATING_CELL} blocks ahead of the eyes: how a platform is built in the air.
         */
        public BlockPos floating() {
            return BlockPos.containing(eyes.add(look.scale(FLOATING_CELL)));
        }

        /** The aimed cell, or the floating cell when the view meets no block. */
        public BlockPos target() {
            return cell != null ? cell : floating();
        }

        /** The face a recipe names: the looked face, or none for the floating cell. */
        int recipeFace() {
            return cell != null && face != null ? face.get3DDataValue() : SpellRecipe.NO_FACE;
        }
    }

    /** What a double click puts back: the selection before its first click. */
    private record Selection(@Nullable BlockPos cornerA, @Nullable BlockPos cornerB, List<BlockPos> seeds,
                             @Nullable BoundingBox maskBounds) {
    }

    private RecipeLimits limits;
    private Spell spell = Spell.FILL;
    private final int[] styles = new int[Spell.values().length];
    private final boolean[] hollow = new boolean[ShapeKind.values().length];

    // The box (corner A alone: the rubber band follows the aim), or the selected thing's seeds.
    private @Nullable BlockPos cornerA;
    private @Nullable BlockPos cornerB;
    private final List<BlockPos> seeds = new ArrayList<>();
    private @Nullable BoundingBox maskBounds;
    private Predicate<BlockPos> selected = seeds::contains;
    private boolean heldBack;

    // Clicks: the last one (a double click's first half) and the press a release may make a drag of.
    private @Nullable BlockPos lastClick;
    private long lastClickTick;
    private @Nullable Selection beforeClick;
    private @Nullable BlockPos pressCell;
    private long pressTick;

    private @Nullable Aim aim;

    // Shapes.
    private int radius = FIRST_RADIUS;
    private boolean planted;
    private boolean heightFixed;
    private int plantedHeight;
    private @Nullable BlockPos plantCell;
    private @Nullable Direction plantFace;
    private int facing;

    // Copy's and Move's ghost.
    private boolean ghost;
    private int lift;
    private int turn;
    private int flip;

    // The other numbers.
    private int copies = 1;
    private int gap;
    private int layers = 1;
    private int brush = FIRST_BRUSH;

    // The material.
    private int material = SpellRecipe.HAND;
    private boolean mix;
    private long seed;

    /** A grammar with nothing selected, Fill chosen, every shape as hollow as it starts, and these numbers. */
    public BuildGrammar(RecipeLimits limits) {
        this.limits = limits;
        for (ShapeKind kind : ShapeKind.values()) {
            hollow[kind.ordinal()] = kind.defaultHollow();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the client tells it

    /** The hero's numbers now (Billy's rank may have changed, the world border moved). */
    public void limits(RecipeLimits limits) {
        this.limits = limits;
    }

    /**
     * Where the builder looks now (every client tick): the rubber band and a ghost follow it, and a planted shape's
     * height while it is not fixed (D-B 4.7).
     */
    public void aim(Aim aim) {
        this.aim = aim;
        Vec3 look = aim.look();
        if (!planted) {
            // An upright shape (the Arch, the Heart) faces the builder until it is planted.
            if (Math.abs(look.x) + Math.abs(look.z) > 1.0E-6) {
                facing = SpellMath.turnOf(Direction.getNearest(look.x, 0.0, look.z));
            }
        } else if (shapeKind().hasHeight() && !heightFixed) {
            plantedHeight = heightByLook(aim);
        }
    }

    /**
     * Which cells the selected thing holds, as the client's flood has found them: a double click on one of them adds
     * nothing. Until the client says, the seeds themselves.
     */
    public void selectedBy(@Nullable Predicate<BlockPos> selected) {
        this.selected = selected != null ? selected : seeds::contains;
    }

    /** The selected thing's bounds once the client's flood has found it all (null while it spreads): its faces push. */
    public void maskBounds(@Nullable BoundingBox bounds) {
        this.maskBounds = bounds;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The left button

    /**
     * The left button went down at {@code tick}, looking at {@code aim}. The client cancels the vanilla attack whenever
     * the grammar takes clicks ({@link #takesClicks}), so a left click never breaks a block in build mode.
     */
    public Result press(Aim aim, long tick) {
        aim(aim);
        BlockPos cell = aim.cell();
        if (spell.needsBox() && cell != null && cell.equals(lastClick) && tick - lastClickTick <= DOUBLE_CLICK_TICKS) {
            // The first click set a corner for a moment: the selection before it comes back, and the thing joins it.
            lastClick = null;
            pressCell = null;
            if (beforeClick != null) {
                restore(beforeClick);
            }
            return select(cell);
        }
        beforeClick = new Selection(cornerA, cornerB, List.copyOf(seeds), maskBounds);
        lastClick = cell;
        lastClickTick = tick;
        pressCell = null;
        return switch (spell) {
            case SHAPE -> plant(aim);
            case PAINT, PULL, LAND, GROW -> Result.NOTHING;
            default -> corner(aim, tick);
        };
    }

    /**
     * The left button came up at {@code tick}, looking at {@code aim}: pressed on one block and let go on another at
     * least {@value #DRAG_TICKS} ticks later, the box is the two at once (D-B 3.2).
     */
    public Result release(Aim aim, long tick) {
        aim(aim);
        BlockPos from = pressCell;
        pressCell = null;
        if (from == null || ghost || !spell.needsBox() || tick - pressTick < DRAG_TICKS) {
            return Result.NOTHING;
        }
        BlockPos to = aim.target();
        if (to.equals(from)) {
            return Result.NOTHING;
        }
        seeds.clear();
        maskBounds = null;
        cornerA = limits.inWorld(from);
        cornerB = within(cornerA, to);
        lastClick = null;
        return Result.DRAGGED;
    }

    /** Whether a left click is the grammar's (every spell but the brushes and Pull, which do nothing with it). */
    public boolean takesClicks() {
        return !spell.isBrush() && spell != Spell.PULL;
    }

    private Result corner(Aim aim, long tick) {
        if (!aim.onBlock() && !ghost && finished()) {
            letGo();
            return Result.LET_GO;
        }
        if (ghost) {
            // A click with a Copy's or a Move's ghost hides it and starts a new box (D-B 2.3); on the sky it just goes.
            ghost = false;
            if (!aim.onBlock()) {
                return Result.LET_GO;
            }
        }
        BlockPos at = aim.target();
        pressTick = tick;
        if (cornerA != null && cornerB == null) {
            cornerB = within(cornerA, at);
            pressCell = at;
            return Result.CORNER_B;
        }
        seeds.clear();
        maskBounds = null;
        cornerA = limits.inWorld(at);
        cornerB = null;
        pressCell = at;
        return Result.CORNER_A;
    }

    private Result select(BlockPos cell) {
        ghost = false;
        if (!seeds.isEmpty()) {
            if (selected.test(cell) || seeds.size() >= SpellRecipe.MAX_SEEDS) {
                return Result.NOTHING;
            }
            seeds.add(cell.immutable());
            maskBounds = null;
            return Result.ADDED;
        }
        cornerA = null;
        cornerB = null;
        seeds.add(cell.immutable());
        maskBounds = null;
        return Result.SELECTED;
    }

    private void restore(Selection selection) {
        cornerA = selection.cornerA;
        cornerB = selection.cornerB;
        seeds.clear();
        seeds.addAll(selection.seeds);
        maskBounds = selection.maskBounds;
    }

    /**
     * A left click with Shapes (D-B 4.7): plants the shape where it is (on the sky, at the floating cell); while a
     * planted shape's height follows the look, fixes it wherever the click is (a tower raised by looking up is clicked
     * against the sky); once fixed, or for a shape with no height, a click on the sky lets it follow the crosshair
     * again and a click on a block plants it there.
     */
    private Result plant(Aim aim) {
        ShapeKind kind = shapeKind();
        if (planted && kind.hasHeight() && !heightFixed) {
            heightFixed = true;
            return Result.FIXED;
        }
        if (planted && !aim.onBlock()) {
            planted = false;
            heightFixed = false;
            return Result.LET_GO;
        }
        int before = height();
        planted = true;
        heightFixed = false;
        plantCell = limits.inWorld(aim.target());
        plantFace = aim.onBlock() ? aim.face() : null;
        plantedHeight = before;
        if (kind.hasHeight()) {
            plantedHeight = heightByLook(aim);
        }
        return Result.PLANTED;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The scroll wheel

    /**
     * {@code notches} of the scroll wheel (up above 0), with Ctrl ({@code fast}: 8 at a time) or with the second knob
     * held ({@code second}, Left Alt), looking at {@code aim}. {@link Result#UNUSED} leaves the scroll to the hotbar.
     */
    public Result scroll(int notches, boolean fast, boolean second, Aim aim) {
        aim(aim);
        if (notches == 0) {
            return Result.UNUSED;
        }
        if (second) {
            return secondKnob(notches, fast);
        }
        int step = notches * (fast ? (spell == Spell.REPEAT ? REPEAT_CTRL_STEP : CTRL_STEP) : 1);
        switch (spell) {
            case FILL, VANISH, CHANGE -> {
                return push(aim, step);
            }
            case COPY, MOVE -> {
                if (!ghost) {
                    return push(aim, step);
                }
                int reach = Math.max(0, limits.reach());
                int wanted = lift + step;
                lift = clamp(wanted, -reach, reach);
                return lift == wanted ? Result.NUMBER : Result.HELD_BACK;
            }
            case REPEAT -> {
                if (!finished()) {
                    return Result.UNUSED;
                }
                int wanted = copies + step;
                copies = clamp(wanted, 1, Math.max(1, limits.repeatCopies()));
                return copies == wanted ? Result.NUMBER : Result.HELD_BACK;
            }
            case SHAPE -> {
                int wanted = radius + step;
                radius = clamp(wanted, 1, Math.max(1, limits.shapeRadius()));
                return radius == wanted ? Result.NUMBER : Result.HELD_BACK;
            }
            case PULL -> {
                return pull(step);
            }
            default -> {
                int wanted = brush + step;
                brush = clamp(wanted, 0, Math.max(0, limits.brush()));
                return brush == wanted ? Result.NUMBER : Result.HELD_BACK;
            }
        }
    }

    private Result secondKnob(int notches, boolean fast) {
        int sign = Integer.signum(notches);
        switch (spell) {
            case FILL, VANISH, CHANGE, SHAPE, LAND, GROW -> {
                styles[spell.ordinal()] = Math.floorMod(styles[spell.ordinal()] + sign, spell.styles());
                return Result.STYLE;
            }
            case COPY, MOVE -> {
                if (!ghost) {
                    return Result.UNUSED;
                }
                turn = Math.floorMod(turn + sign, 4);
                return Result.STYLE;
            }
            case REPEAT -> {
                if (!finished()) {
                    return Result.UNUSED;
                }
                int wanted = gap + notches * (fast ? REPEAT_CTRL_STEP : 1);
                gap = clamp(wanted, 0, SpellRecipe.MAX_GAP);
                return gap == wanted ? Result.NUMBER : Result.HELD_BACK;
            }
            default -> {
                return Result.UNUSED;
            }
        }
    }

    /** Pull &amp; Push's layers: 0 would do nothing, so the wheel steps over it, from one layer out to one in. */
    private Result pull(int step) {
        int side = Math.max(1, limits.side());
        int next = layers + step;
        if (layers > 0 && next <= 0) {
            next--;
        } else if (layers < 0 && next >= 0) {
            next++;
        }
        layers = clamp(next, -side, side);
        return Math.abs(next) > side ? Result.HELD_BACK : Result.NUMBER;
    }

    private Result push(Aim aim, int step) {
        BoundingBox box = finishedBox();
        if (box == null) {
            return Result.UNUSED;
        }
        Direction face = faceLookedAt(box, aim.eyes(), aim.look());
        if (face == null) {
            return Result.UNUSED;
        }
        int side = Math.max(1, limits.side());
        Direction.Axis axis = face.getAxis();
        int min = axis.choose(box.minX(), box.minY(), box.minZ());
        int max = axis.choose(box.maxX(), box.maxY(), box.maxZ());
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        // A face never passes the opposite one (one layer at least), nor makes a side longer than the hero's.
        int wanted = positive ? max + step : min - step;
        int moved = positive ? clamp(wanted, min, min + side - 1) : clamp(wanted, max - side + 1, max);
        BlockPos low = new BlockPos(box.minX(), box.minY(), box.minZ());
        BlockPos high = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
        BlockPos edge = limits.inWorld(with(positive ? high : low, axis, moved));
        if (positive) {
            high = edge;
        } else {
            low = edge;
        }
        // Pushing a face turns a selected thing back into a plain box (D-B 3.4).
        seeds.clear();
        maskBounds = null;
        cornerA = low;
        cornerB = high;
        int now = axis.choose(edge.getX(), edge.getY(), edge.getZ());
        return now != wanted ? Result.HELD_BACK : Result.PUSHED;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Keys and spells

    /** Chooses a spell: the box or the selected thing stays (Fill, then Vanish, then Change on the same box). */
    public void spell(Spell spell) {
        if (spell != this.spell) {
            ghost = false;
            planted = false;
            heightFixed = false;
        }
        this.spell = spell;
    }

    /** Chooses the current spell's style (or shape), by its ordinal. */
    public void style(int style) {
        styles[spell.ordinal()] = Math.floorMod(style, spell.styles());
    }

    /** Whether the current shape is hollow (a switch on the wheel). */
    public void hollow(boolean hollow) {
        this.hollow[shapeKind().ordinal()] = hollow;
    }

    /** Ctrl + R: the ghost a quarter turn clockwise. False when there is no ghost to turn. */
    public boolean turnGhost() {
        if (!ghost) {
            return false;
        }
        turn = Math.floorMod(turn + 1, 4);
        return true;
    }

    /**
     * Ctrl + F: the ghost flipped along the way the builder looks (east-west or north-south; looking steeply up or
     * down, upside down); the same flip again puts it back. False when there is no ghost to flip.
     */
    public boolean flipGhost(Vec3 look) {
        if (!ghost) {
            return false;
        }
        int along;
        if (Math.abs(look.y) > 0.8) {
            along = SpellRecipe.UPSIDE_DOWN;
        } else {
            along = Math.abs(look.x) >= Math.abs(look.z) ? SpellRecipe.FLIP_X : SpellRecipe.FLIP_Z;
        }
        flip = flip == along ? SpellRecipe.NO_FLIP : along;
        return true;
    }

    /**
     * The material: the block in the hand ({@link SpellRecipe#HAND}) or the last one picked
     * ({@link SpellRecipe#PICKED}).
     */
    public void material(int material) {
        this.material = material;
    }

    /** Whether the material is the hotbar's mix, chosen per cell by {@code seed} so that the ghost is what is built. */
    public void mix(boolean mix, long seed) {
        this.mix = mix;
        this.seed = seed;
    }

    /**
     * Shows the pocket's ghost at the crosshair (Copy after its first right click, Ctrl + V, or Copy chosen with no
     * box; Move after it picked something up), with no lift, turn or flip.
     */
    public void holdGhost() {
        ghost = true;
        lift = 0;
        turn = 0;
        flip = SpellRecipe.NO_FLIP;
    }

    /** The ghost goes (a refused copy, a put-down done). */
    public void dropGhost() {
        ghost = false;
    }

    /** Lets go of the box, the selected thing, a planted shape and a ghost (a click on the sky, a change of world). */
    public void letGo() {
        cornerA = null;
        cornerB = null;
        seeds.clear();
        maskBounds = null;
        planted = false;
        heightFixed = false;
        ghost = false;
        pressCell = null;
        lastClick = null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What it shows and casts

    public Spell spell() {
        return spell;
    }

    /** The current spell's style or shape (its ordinal). */
    public int style() {
        return styles[spell.ordinal()];
    }

    public ShapeKind shapeKind() {
        return ShapeKind.byId(styles[Spell.SHAPE.ordinal()]);
    }

    /** Whether the current shape is hollow. */
    public boolean hollow() {
        return hollow[shapeKind().ordinal()];
    }

    /** Corner A, while it is the only one (the rubber band) or the box's first. */
    public @Nullable BlockPos cornerA() {
        return cornerA;
    }

    /** The places the selected thing is flooded from (empty when there is none). */
    public List<BlockPos> seeds() {
        return Collections.unmodifiableList(seeds);
    }

    /** Whether a selected thing (a magic selection) is shown. */
    public boolean hasMask() {
        return !seeds.isEmpty();
    }

    /** The selected thing's bounds, once the client's flood has found them. */
    public @Nullable BoundingBox maskBounds() {
        return maskBounds;
    }

    /** Whether a box is fixed (two corners) or a thing selected: what the box spells cast on. */
    public boolean finished() {
        return !seeds.isEmpty() || cornerA != null && cornerB != null;
    }

    /** Whether only corner A is set and the box follows the aim. */
    public boolean rubberBand() {
        return seeds.isEmpty() && cornerA != null && cornerB == null;
    }

    /**
     * The box shown now: the fixed box, or while only corner A is set, the rubber band from it to the aimed cell (the
     * floating cell on the sky), stopped at Billy's side; null with no box (a selected thing has {@link #maskBounds}).
     */
    public @Nullable BoundingBox box() {
        if (!seeds.isEmpty() || cornerA == null) {
            return null;
        }
        if (cornerB != null) {
            return SpellMath.box(cornerA, cornerB);
        }
        if (aim == null) {
            return SpellMath.box(cornerA, cornerA);
        }
        return SpellMath.box(cornerA, within(cornerA, aim.target()));
    }

    /** Whether Billy's side held the rubber band back at the last look (its label flashes). */
    public boolean heldBack() {
        if (rubberBand() && aim != null) {
            within(cornerA, aim.target());
        }
        return heldBack;
    }

    /** Whether a Copy's or a Move's ghost follows the crosshair. */
    public boolean ghost() {
        return ghost;
    }

    public int lift() {
        return lift;
    }

    public int turn() {
        return turn;
    }

    public int flip() {
        return flip;
    }

    public int radius() {
        return radius;
    }

    /** The current shape's height: its default until it is planted, then the look's, then fixed. 0 without one. */
    public int height() {
        ShapeKind kind = shapeKind();
        if (!kind.hasHeight()) {
            return 0;
        }
        return planted ? Math.max(1, plantedHeight) : Math.max(1, Math.min(kind.defaultHeight(radius), Math.max(1, limits.shapeHeight())));
    }

    /** Whether the shape stays where it was clicked, rather than following the crosshair. */
    public boolean planted() {
        return planted;
    }

    /** Whether a planted shape's height is fixed (no longer following the look). */
    public boolean heightFixed() {
        return heightFixed;
    }

    public int copies() {
        return copies;
    }

    public int gap() {
        return gap;
    }

    /** Pull &amp; Push's layers: out of the face above 0, into it below; never 0. */
    public int layers() {
        return layers;
    }

    public int brush() {
        return brush;
    }

    public int material() {
        return material;
    }

    public boolean mix() {
        return mix;
    }

    public long seed() {
        return seed;
    }

    /** The hint strip's stage for the selection (the client makes a box that needs holding {@code BOX_HOLD}). */
    public BuildHints.Stage stage() {
        return switch (spell) {
            case SHAPE -> !planted ? BuildHints.Stage.FOLLOWING : heightFixed ? BuildHints.Stage.FIXED : BuildHints.Stage.PLANTED;
            case PAINT, PULL, LAND, GROW -> BuildHints.Stage.NOTHING;
            default -> ghost ? BuildHints.Stage.GHOST : finished() ? BuildHints.Stage.BOX
                    : rubberBand() ? BuildHints.Stage.CORNER : BuildHints.Stage.NOTHING;
        };
    }

    /**
     * What the right button would cast now, looking where the last {@link #aim} looked, or null when the spell has
     * nothing ready (no box for a box spell, no face for Pull, no block in reach for a brush). Whether a material is
     * there is the client's to see. The op follows the ghost: Copy takes the box, then stamps; Move picks it up, then
     * puts it down. A brush's dab carries its {@code stroke}.
     */
    public @Nullable SpellRecipe recipe(int stroke) {
        Aim at = aim;
        if (at == null) {
            return null;
        }
        BlockPos near = at.target();
        int flags = (mix ? SpellRecipe.MIX : 0);
        return switch (spell) {
            case FILL, VANISH, CHANGE, REPEAT -> {
                if (!finished()) {
                    yield null;
                }
                // "This kind" is the block under the picking ray; every anchor stays within reach of the eyes.
                BlockPos anchor = at.picked() != null ? at.picked() : near;
                int face = spell == Spell.REPEAT ? Direction.getNearest(at.look().x, at.look().y, at.look().z).get3DDataValue()
                        : SpellRecipe.NO_FACE;
                yield region(SpellRecipe.Op.CAST, anchor, face, spell == Spell.REPEAT ? copies : 0, spell == Spell.REPEAT ? gap : 0,
                        0, SpellRecipe.NO_FLIP, flags, stroke);
            }
            case COPY, MOVE -> {
                if (ghost) {
                    SpellRecipe.Op op = spell == Spell.COPY ? SpellRecipe.Op.STAMP : SpellRecipe.Op.PUT_DOWN;
                    if (spell == Spell.MOVE && !finished()) {
                        yield null;
                    }
                    yield spell == Spell.COPY
                            ? new SpellRecipe(spell, op, 0, SpellRecipe.NO_REGION, near, near, List.of(), near, at.recipeFace(), lift, 0, turn,
                            flip, flags, material, seed, stroke)
                            : region(op, near, at.recipeFace(), lift, 0, turn, flip, flags, stroke);
                }
                if (!finished()) {
                    yield null;
                }
                yield region(spell == Spell.COPY ? SpellRecipe.Op.TAKE : SpellRecipe.Op.PICK_UP, near, SpellRecipe.NO_FACE, 0, 0, 0,
                        SpellRecipe.NO_FLIP, flags, stroke);
            }
            case SHAPE -> {
                ShapeKind kind = shapeKind();
                BlockPos anchor = planted && plantCell != null ? plantCell : limits.inWorld(near);
                int face = planted ? (plantFace != null ? plantFace.get3DDataValue() : SpellRecipe.NO_FACE) : at.recipeFace();
                int shapeFlags = flags | (hollow() ? SpellRecipe.HOLLOW : 0) | (planted ? SpellRecipe.PLANTED : 0);
                yield new SpellRecipe(spell, SpellRecipe.Op.CAST, kind.ordinal(), SpellRecipe.NO_REGION, anchor, anchor, List.of(), anchor,
                        face, radius, height(), kind.upright() ? facing : 0, SpellRecipe.NO_FLIP, shapeFlags, material, seed, stroke);
            }
            case PULL -> {
                if (at.cell() == null || at.face() == null) {
                    yield null;
                }
                yield new SpellRecipe(spell, SpellRecipe.Op.CAST, 0, SpellRecipe.NO_REGION, at.cell(), at.cell(), List.of(), at.cell(),
                        at.face().get3DDataValue(), layers, 0, 0, SpellRecipe.NO_FLIP, flags, material, seed, stroke);
            }
            case PAINT, LAND, GROW -> {
                BlockPos centre = spell == Spell.PAINT ? at.picked() : at.cell() != null ? at.cell() : at.picked();
                if (centre == null) {
                    yield null;
                }
                // A dab over a selected thing paints only the thing (it carries the seeds); otherwise it has no region.
                boolean thing = spell == Spell.PAINT && !seeds.isEmpty();
                BoundingBox bounds = thing ? maskOrSeedBounds() : null;
                BlockPos a = bounds != null ? new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()) : centre;
                BlockPos b = bounds != null ? new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()) : centre;
                yield new SpellRecipe(spell, SpellRecipe.Op.DAB, style(), thing ? SpellRecipe.MASK : SpellRecipe.NO_REGION, a, b,
                        thing ? List.copyOf(seeds) : List.of(), centre, at.recipeFace(), brush, 0, 0, SpellRecipe.NO_FLIP, flags, material,
                        seed, stroke);
            }
        };
    }

    /** A recipe on the box or the selected thing. */
    private SpellRecipe region(SpellRecipe.Op op, BlockPos anchor, int face, int size, int size2, int turn, int flip, int flags, int stroke) {
        if (!seeds.isEmpty()) {
            BoundingBox bounds = maskOrSeedBounds();
            return new SpellRecipe(spell, op, style(), SpellRecipe.MASK, new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
                    new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()), List.copyOf(seeds), anchor, face, size, size2, turn, flip, flags,
                    material, seed, stroke);
        }
        BoundingBox box = SpellMath.box(cornerA, cornerB);
        return new SpellRecipe(spell, op, style(), SpellRecipe.BOX, new BlockPos(box.minX(), box.minY(), box.minZ()),
                new BlockPos(box.maxX(), box.maxY(), box.maxZ()), List.of(), anchor, face, size, size2, turn, flip, flags, material, seed, stroke);
    }

    /**
     * What the ghost shows now (D-B 6.2): the recipe the right button would cast ({@link #recipe}), or, while the
     * rubber band of a Fill, a Vanish, a Change or a Repeat follows the aim, the same spell on the rubber band's box,
     * so that its ghost grows with it ("the ghost of 35 stone blocks inside", D-B 1). The right button casts nothing
     * until the box is fixed. Null when there is nothing to show.
     */
    public @Nullable SpellRecipe preview(int stroke) {
        SpellRecipe now = recipe(stroke);
        Aim at = aim;
        BoundingBox band = rubberBand() ? box() : null;
        if (now != null || band == null || at == null) {
            return now;
        }
        return switch (spell) {
            case FILL, VANISH, CHANGE, REPEAT -> {
                BlockPos anchor = at.picked() != null ? at.picked() : at.target();
                boolean row = spell == Spell.REPEAT;
                int face = row ? Direction.getNearest(at.look().x, at.look().y, at.look().z).get3DDataValue() : SpellRecipe.NO_FACE;
                yield new SpellRecipe(spell, SpellRecipe.Op.CAST, style(), SpellRecipe.BOX, new BlockPos(band.minX(), band.minY(), band.minZ()),
                        new BlockPos(band.maxX(), band.maxY(), band.maxZ()), List.of(), anchor, face, row ? copies : 0, row ? gap : 0, 0,
                        SpellRecipe.NO_FLIP, mix ? SpellRecipe.MIX : 0, material, seed, stroke);
            }
            default -> null;
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // Geometry

    /** The fixed box, or the selected thing's bounds once found; null otherwise. */
    private @Nullable BoundingBox finishedBox() {
        if (!seeds.isEmpty()) {
            return maskBounds;
        }
        return cornerA != null && cornerB != null ? SpellMath.box(cornerA, cornerB) : null;
    }

    private BoundingBox maskOrSeedBounds() {
        if (maskBounds != null) {
            return maskBounds;
        }
        BlockPos low = seeds.get(0);
        BlockPos high = seeds.get(0);
        for (BlockPos seed : seeds) {
            low = BlockPos.min(low, seed);
            high = BlockPos.max(high, seed);
        }
        return SpellMath.box(low, high);
    }

    /**
     * {@code pos} kept inside the world and, from {@code anchor}, inside the hero's longest side on every axis (Billy's
     * rubber band holds there, {@link #heldBack}).
     */
    private BlockPos within(BlockPos anchor, BlockPos pos) {
        BlockPos inWorld = limits.inWorld(pos);
        int reach = Math.max(1, limits.side()) - 1;
        BlockPos held = new BlockPos(clamp(inWorld.getX(), anchor.getX() - reach, anchor.getX() + reach),
                clamp(inWorld.getY(), anchor.getY() - reach, anchor.getY() + reach), clamp(inWorld.getZ(), anchor.getZ() - reach, anchor.getZ() + reach));
        heldBack = !held.equals(inWorld);
        return held;
    }

    /**
     * The face of {@code box} the view ray meets first, or, from inside the box, the face it leaves by (the far face);
     * null when the ray misses it (D-B 3.3).
     */
    public static @Nullable Direction faceLookedAt(BoundingBox box, Vec3 eyes, Vec3 look) {
        double[] low = {box.minX(), box.minY(), box.minZ()};
        double[] high = {box.maxX() + 1.0, box.maxY() + 1.0, box.maxZ() + 1.0};
        double[] from = {eyes.x, eyes.y, eyes.z};
        double[] dir = {look.x, look.y, look.z};
        double enter = Double.NEGATIVE_INFINITY;
        double leave = Double.POSITIVE_INFINITY;
        int enterAxis = -1;
        int leaveAxis = -1;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(dir[i]) < 1.0E-9) {
                if (from[i] < low[i] || from[i] > high[i]) {
                    return null;
                }
                continue;
            }
            double t1 = (low[i] - from[i]) / dir[i];
            double t2 = (high[i] - from[i]) / dir[i];
            double near = Math.min(t1, t2);
            double far = Math.max(t1, t2);
            if (near > enter) {
                enter = near;
                enterAxis = i;
            }
            if (far < leave) {
                leave = far;
                leaveAxis = i;
            }
        }
        if (enter > leave || leave < 0.0) {
            return null;
        }
        boolean inside = enter < 0.0;
        int axis = inside ? leaveAxis : enterAxis;
        if (axis < 0) {
            return null;
        }
        // Entering through a low face (the ray going up the axis) is that low face; leaving, the high one.
        boolean up = dir[axis] > 0.0;
        boolean positiveFace = inside == up;
        Direction.Axis which = Direction.Axis.values()[axis];
        return Direction.fromAxisAndDirection(which, positiveFace ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    /**
     * The height a planted shape takes from the look: where the view ray passes its vertical axis, counted from its
     * base (or down from its top, for a shape hanging under a ceiling), from 1 to the hero's tallest (D-B 4.7). The
     * height it has when the ray runs along the axis or away from it.
     */
    private int heightByLook(Aim aim) {
        BlockPos at = plantCell != null ? plantCell : aim.target();
        int max = Math.max(1, limits.shapeHeight());
        int keep = Math.max(1, plantedHeight);
        BoundingBox local = SpellMath.Shape.of(shapeKind(), radius, 1, hollow(), 0).bounds();
        BlockPos corner = SpellMath.stand(local.getXSpan(), 1, local.getZSpan(), at, plantFace, 0);
        double cx = corner.getX() + local.getXSpan() / 2.0;
        double cz = corner.getZ() + local.getZSpan() / 2.0;
        Vec3 eyes = aim.eyes();
        Vec3 look = aim.look();
        double flat = look.x * look.x + look.z * look.z;
        if (flat < 1.0E-6) {
            return Math.min(keep, max);
        }
        double t = ((cx - eyes.x) * look.x + (cz - eyes.z) * look.z) / flat;
        if (t <= 0.0) {
            return Math.min(keep, max);
        }
        int y = Mth.floor(eyes.y + t * look.y);
        int height = plantFace == Direction.DOWN ? at.getY() - 1 - y + 1 : y - corner.getY() + 1;
        return clamp(height, 1, max);
    }

    private static BlockPos with(BlockPos pos, Direction.Axis axis, int value) {
        return switch (axis) {
            case X -> new BlockPos(value, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), value, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), value);
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
