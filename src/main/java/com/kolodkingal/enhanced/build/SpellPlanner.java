package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.build.SpellMath.Placement;
import com.kolodkingal.enhanced.build.SpellMath.PullSurface;
import com.kolodkingal.enhanced.build.SpellMath.Shape;
import com.kolodkingal.enhanced.build.SpellMath.Volume;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The one planner of Build Magic (D-B 11.2-11.4), shared by the server and every client: from a {@link SpellRecipe},
 * the world as a {@link BlockGetter} and a few inputs ({@link Setup}: the selected thing, the material, the pocket,
 * where the caster stands), the cells of a cast and the block each one gets, in the order that gives each spell its
 * look. The client draws its ghost and counts it with it, the server builds with it, so the ghost is the result
 * whenever the two worlds agree. It never writes a block and never holds a list of cells: a {@link Plan} is a few
 * numbers and a {@link Volume}, and its {@link Cursor} walks the cells one at a time (the server's job keeps it as its
 * place).
 *
 * <p><b>Two steps per cell.</b> The cursor first moves to the next cell of the plan's geometry ({@link Cursor#next}),
 * which reads nothing, so the job can check the cell's chunk first; then {@link Cursor#resolve} reads the cell (and for
 * Change's skin and Paint its neighbours) and answers the block it gets, or null when the spell leaves it as it is.
 * Whether the cell already holds that block (nothing to do, nothing to pay) is for the caller to see.
 *
 * <p><b>What each spell plans</b> (D-B 4):
 * <ul>
 *     <li><b>Fill</b>: the cells of its style in the box ({@link SpellMath#fill}), or every cell of a selected thing,
 *     take the material; Room empties its inside; Only the empty fills only air and replaceable cells.</li>
 *     <li><b>Vanish</b>: everything, the inside (Hollow out), plants and trees ({@code enhanced:build_plants}), or the
 *     kind of block looked at; a block that held water leaves it ({@link SpellMath#vanished}).</li>
 *     <li><b>Change</b>: the kind looked at, everything, or the skin (blocks touching air), by the family rule
 *     ({@link SpellMath#swap}).</li>
 *     <li><b>Paint</b> (one dab): the skin within a ball of the brush's radius round the block looked at.</li>
 *     <li><b>Pull &amp; Push</b>: copies of the surface ({@link PullSurface}) pulled out layer by layer into air or
 *     replaceable cells, or that many layers behind it (the surface included) pushed in.</li>
 *     <li><b>Shapes</b>: the {@link Shape} standing on the face looked at ({@link SpellMath#stand}).</li>
 *     <li><b>Copy</b>: the first click (take) writes nothing; a stamp is the pocket, turned and flipped, standing on
 *     the face looked at and lifted. A box copy brings its air, a mask copy only the thing (the pocket says which cells
 *     it has).</li>
 *     <li><b>Move</b>: the pick-up writes nothing; the put-down is the source, read into a pocket, stamped where the
 *     ghost is, every cell knowing the cell it came {@linkplain Cursor#from from}.</li>
 *     <li><b>Repeat</b>: copies of the box or the thing in a row, {@code size} of them, {@code size2} apart, towards
 *     the face the recipe names.</li>
 *     <li><b>Land</b> and <b>Grow</b> are brushes whose pulses their own classes plan; here they plan nothing.</li>
 * </ul>
 *
 * <p><b>The work order</b> (Appendix A.3): a plan whose bounds fit in {@value #LAYERED_MAX} x {@value #LAYERED_MAX}
 * blocks (every spell of Billy's) goes layer by layer over the whole plan, placing bottom-up (Fill, Change, Paint,
 * Pull, the stamps, Repeat, Tower, Cone, Pyramid) and clearing top-down (Vanish, Push), each layer in rows from the
 * side nearest the caster; a bigger one goes chunk column by chunk column, in a square spiral from the column nearest
 * the caster, each column layer by layer the same way. The round shapes (Ball, Dome, Ring, Arch, Heart) that fit grow
 * shell by shell from their heart instead (a bigger one goes by columns like every big spell, so the job's chunk
 * tickets can follow it). A Move over itself by a plain shift goes in the order that moves every source cell before the
 * Move writes over it.
 */
public final class SpellPlanner {
    /** The widest plan (blocks, along x and along z) that is built layer by layer over the whole plan. */
    public static final int LAYERED_MAX = 64;
    /** No refusal: the plan can be cast (what the world says about its cells is the rules'). */
    public static final int NONE = -1;

    // ------------------------------------------------------------------------------------------------------------
    // Inputs

    /**
     * One cell of a plan: where, the block it gets, the block entity data it gets (a stamp's, a Move's), and for a Move
     * the cell it comes from.
     */
    public record Cell(BlockPos pos, BlockState state, @Nullable CompoundTag data, @Nullable BlockPos from) {
    }

    /** The material of a cast, per cell: one block, or the hotbar's mix ({@link SpellMath.Mix}). */
    @FunctionalInterface
    public interface Material {
        BlockState at(int x, int y, int z);

        /** One block everywhere: the hand's, in the state it would be placed in, or a picked one. */
        static Material of(BlockState state) {
            return (x, y, z) -> state;
        }

        /** The hotbar's mix, chosen per cell by the cast's seed. */
        static Material mix(SpellMath.Mix mix, long seed) {
            return (x, y, z) -> mix.pick(x, y, z, seed);
        }
    }

    /**
     * Copied cells (the server's pocket, a client's copy of it, a Move's or a Repeat's source read at its start) in a
     * box of {@code sizeX x sizeY x sizeZ} offsets. A cell it does not have (outside a mask, or a block magic never
     * copies, which a stamp then leaves alone) is null.
     */
    public interface Source {
        int sizeX();

        int sizeY();

        int sizeZ();

        /** The block of the cell {@code x, y, z} (offsets in the box), or null when the copy has none there. */
        @Nullable BlockState state(int x, int y, int z);

        /** The block entity data of the cell, or null (none, or not copied). */
        default @Nullable CompoundTag data(int x, int y, int z) {
            return null;
        }

        /** How many cells it has at most (the plan's size; the box's volume unless the source knows better). */
        default long cells() {
            return (long) sizeX() * sizeY() * sizeZ();
        }

        /**
         * The cells of {@code region} as they are in {@code level} now, read when asked (a client's ghost of a Move or
         * a Repeat): the box's air included, a mask's cells only, never a block magic does not touch
         * ({@link SpellMath#untouchable}) or half of a door or a bed whose other half is outside, never a cell in a
         * chunk that is not loaded (it is read as absent, and never loaded). No data. It is read while the cursor
         * moves, so it is for ghosts: the server passes the pocket its job captured instead ({@link Setup#source}).
         */
        static Source live(BlockGetter level, Volume region) {
            BoundingBox box = region.bounds();
            return new Source() {
                @Override
                public long cells() {
                    return region.count();
                }

                @Override
                public int sizeX() {
                    return box.getXSpan();
                }

                @Override
                public int sizeY() {
                    return box.getYSpan();
                }

                @Override
                public int sizeZ() {
                    return box.getZSpan();
                }

                @Override
                public @Nullable BlockState state(int x, int y, int z) {
                    int wx = box.minX() + x;
                    int wy = box.minY() + y;
                    int wz = box.minZ() + z;
                    if (!region.contains(wx, wy, wz)) {
                        return null;
                    }
                    BlockPos pos = new BlockPos(wx, wy, wz);
                    if (!SpellMath.loaded(level, pos)) {
                        return null;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (SpellMath.untouchable(state, level, pos)) {
                        return null;
                    }
                    BlockPos partner = SpellMath.partner(state, pos);
                    return partner == null || region.contains(partner.getX(), partner.getY(), partner.getZ()) ? state : null;
                }
            };
        }
    }

    /**
     * What a plan needs besides the recipe and the world.
     *
     * @param mask      the selected thing, when the recipe's region is a {@linkplain SpellRecipe#MASK mask} (the magic
     *                  selection's flood from its seeds; the planner never floods it itself)
     * @param material  the material of Fill, Change, Paint and Shapes, as the server or the client resolved it (a mix
     *                  with the recipe's seed)
     * @param source    a stamp's pocket; for a Move's put-down and a Repeat their source as it was read when the spell
     *                  started (null: read from the world as it is, as a client's ghost does)
     * @param caster    where the caster stands (the work order starts on his side)
     * @param selectCap the most cells of a Pull surface (the magic-select cap)
     * @param span      the widest a Pull surface may be (Billy's side; 0 for no limit)
     */
    public record Setup(@Nullable Volume mask, @Nullable Material material, @Nullable Source source, BlockPos caster, int selectCap,
                        int span) {
        /** Nothing but where the caster stands: no mask, material or pocket, a Pull surface of any size. */
        public static Setup at(BlockPos caster) {
            return new Setup(null, null, null, caster, Integer.MAX_VALUE, 0);
        }

        public Setup withMask(@Nullable Volume mask) {
            return new Setup(mask, material, source, caster, selectCap, span);
        }

        public Setup withMaterial(@Nullable Material material) {
            return new Setup(mask, material, source, caster, selectCap, span);
        }

        public Setup withMaterial(BlockState state) {
            return withMaterial(Material.of(state));
        }

        public Setup withSource(@Nullable Source source) {
            return new Setup(mask, material, source, caster, selectCap, span);
        }

        public Setup withSurface(int selectCap, int span) {
            return new Setup(mask, material, source, caster, selectCap, span);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Planning

    /**
     * The plan of a cast. {@code recipe} should be {@linkplain SpellRecipe#clamped clamped} already (the server's first
     * step). {@code level} is read only for what the plan needs before its first cell: the kind of block looked at
     * (Vanish's and Change's This kind) now, and a Pull's surface in {@link Plan#prepare}. It must not load chunks it
     * is asked about (a level does not: {@link SpellMath#loaded} is asked first).
     */
    public static Plan plan(BlockGetter level, SpellRecipe recipe, Setup setup) {
        return switch (recipe.spell()) {
            case FILL -> fill(recipe, setup);
            case VANISH -> vanish(level, recipe, setup);
            case CHANGE -> change(level, recipe, setup);
            case PAINT -> paint(recipe, setup);
            case PULL -> pull(level, recipe, setup);
            case SHAPE -> shape(recipe, setup);
            case COPY -> recipe.op() == SpellRecipe.Op.STAMP ? stamp(recipe, setup) : capture(recipe, setup);
            case MOVE -> recipe.op() == SpellRecipe.Op.PUT_DOWN ? move(level, recipe, setup) : capture(recipe, setup);
            case REPEAT -> repeat(level, recipe, setup);
            case LAND, GROW -> new Plan(recipe, NONE, null, null, null, Order.RISE, setup.caster);
        };
    }

    /** The box or the selected thing of a recipe, or null when it has none. */
    public static @Nullable Volume region(SpellRecipe recipe, Setup setup) {
        return switch (recipe.region()) {
            case SpellRecipe.BOX -> SpellMath.solid(SpellMath.box(recipe.a(), recipe.b()));
            case SpellRecipe.MASK -> setup.mask;
            default -> null;
        };
    }

    private static Plan refused(SpellRecipe recipe, Setup setup, int refusal) {
        return new Plan(recipe, refusal, null, null, null, Order.RISE, setup.caster);
    }

    private static Plan fill(SpellRecipe recipe, Setup setup) {
        Volume region = region(recipe, setup);
        if (region == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BOX);
        }
        Material material = setup.material;
        if (material == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BLOCK);
        }
        if (recipe.region() != SpellRecipe.BOX) {
            return new Plan(recipe, NONE, region, null, conjure(material), Order.RISE, setup.caster);
        }
        BoundingBox box = region.bounds();
        Rule rule = switch (recipe.fillStyle()) {
            case ROOM -> {
                Volume inside = SpellMath.inside(box);
                Rule walls = conjure(material);
                yield (level, pos, cursor) -> inside.contains(pos.getX(), pos.getY(), pos.getZ())
                        ? SpellMath.vanished(level.getBlockState(pos)) : walls.apply(level, pos, cursor);
            }
            case EMPTY -> (level, pos, cursor) -> {
                BlockState old = level.getBlockState(pos);
                return SpellMath.isEmpty(old) ? SpellMath.placedInto(material.at(pos.getX(), pos.getY(), pos.getZ()), old) : null;
            };
            default -> conjure(material);
        };
        Volume cells = recipe.fillStyle() == FillStyle.ROOM ? region : SpellMath.fill(recipe.fillStyle(), box);
        return new Plan(recipe, NONE, cells, null, rule, Order.RISE, setup.caster);
    }

    private static Plan vanish(BlockGetter level, SpellRecipe recipe, Setup setup) {
        Volume region = region(recipe, setup);
        if (region == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BOX);
        }
        Rule gone = (getter, pos, cursor) -> {
            BlockState old = getter.getBlockState(pos);
            return old.isAir() ? null : SpellMath.vanished(old);
        };
        return switch (recipe.vanishStyle()) {
            case HOLLOW -> {
                Plan plan = new Plan(recipe, NONE, SpellMath.inside(region), null, gone, Order.MELT, setup.caster);
                // The inside of a selected thing is counted only cell by cell; the thing's own size bounds it at once.
                plan.size = recipe.region() == SpellRecipe.BOX ? -1 : region.count();
                yield plan;
            }
            case PLANTS -> new Plan(recipe, NONE, region, null, (getter, pos, cursor) -> {
                BlockState old = getter.getBlockState(pos);
                return old.is(ModTags.BUILD_PLANTS) ? SpellMath.vanished(old) : null;
            }, Order.MELT, setup.caster);
            case KIND -> {
                Block kind = kind(level, recipe, region);
                if (kind == null) {
                    yield refused(recipe, setup, BuildVerdictPayload.LOOK_INSIDE);
                }
                yield new Plan(recipe, NONE, region, null, (getter, pos, cursor) -> {
                    BlockState old = getter.getBlockState(pos);
                    return old.is(kind) ? SpellMath.vanished(old) : null;
                }, Order.MELT, setup.caster);
            }
            default -> new Plan(recipe, NONE, region, null, gone, Order.MELT, setup.caster);
        };
    }

    private static Plan change(BlockGetter level, SpellRecipe recipe, Setup setup) {
        Volume region = region(recipe, setup);
        if (region == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BOX);
        }
        Material material = setup.material;
        if (material == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BLOCK);
        }
        Rule rule = switch (recipe.changeStyle()) {
            case KIND -> {
                Block kind = kind(level, recipe, region);
                if (kind == null) {
                    yield null;
                }
                yield (getter, pos, cursor) -> {
                    BlockState old = getter.getBlockState(pos);
                    return old.is(kind) && SpellMath.canChange(old) ? SpellMath.swap(old, material.at(pos.getX(), pos.getY(), pos.getZ())) : null;
                };
            }
            case ALL -> (getter, pos, cursor) -> {
                BlockState old = getter.getBlockState(pos);
                return SpellMath.canChange(old) ? SpellMath.swap(old, material.at(pos.getX(), pos.getY(), pos.getZ())) : null;
            };
            case OUTSIDE -> skin(material);
        };
        if (rule == null) {
            return refused(recipe, setup, BuildVerdictPayload.LOOK_INSIDE);
        }
        return new Plan(recipe, NONE, region, null, rule, Order.RISE, setup.caster);
    }

    private static Plan paint(SpellRecipe recipe, Setup setup) {
        Material material = setup.material;
        if (material == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BLOCK);
        }
        BlockPos centre = recipe.anchor();
        int radius = Math.max(0, recipe.size());
        Volume ball = SpellMath.shifted(new Shape(ShapeKind.BALL, radius, 0, 1, false, 0), centre.getX(), centre.getY() - radius, centre.getZ());
        return new Plan(recipe, NONE, ball, null, skin(material), Order.RISE, setup.caster);
    }

    private static Plan pull(BlockGetter level, SpellRecipe recipe, Setup setup) {
        Direction face = recipe.direction();
        if (face == null || recipe.size() == 0) {
            return new Plan(recipe, NONE, null, null, null, Order.RISE, setup.caster);
        }
        PullSurface surface = new PullSurface(level, recipe.anchor(), face, setup.selectCap, setup.span);
        int layers = recipe.size();
        Rule rule = layers > 0 ? (getter, pos, cursor) -> {
            BlockState old = getter.getBlockState(pos);
            if (!SpellMath.isEmpty(old)) {
                return null;
            }
            BlockPos from = PullCells.surfaceCell(pos, face, recipe.anchor());
            return SpellMath.loaded(getter, from) ? SpellMath.placedInto(getter.getBlockState(from), old) : null;
        } : (getter, pos, cursor) -> {
            BlockState old = getter.getBlockState(pos);
            return old.isAir() ? null : SpellMath.vanished(old);
        };
        return new Plan(recipe, NONE, null, null, rule, layers > 0 ? Order.RISE : Order.MELT, setup.caster, null, surface);
    }

    private static Plan shape(SpellRecipe recipe, Setup setup) {
        Material material = setup.material;
        if (material == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BLOCK);
        }
        Shape shape = Shape.of(recipe.shapeKind(), recipe.size(), recipe.size2(), recipe.hollow(), recipe.turn());
        BoundingBox local = shape.bounds();
        BlockPos corner = SpellMath.stand(local.getXSpan(), local.getYSpan(), local.getZSpan(), recipe.anchor(), recipe.direction(), 0);
        int dx = corner.getX() - local.minX();
        int dy = corner.getY() - local.minY();
        int dz = corner.getZ() - local.minZ();
        Volume cells = SpellMath.shifted(shape, dx, dy, dz);
        BlockPos heart = shape.round() ? shape.heart().offset(dx, dy, dz) : null;
        Plan plan = new Plan(recipe, NONE, cells, null, conjure(material), shape.round() ? Order.SHELLS : Order.RISE, setup.caster, heart, null);
        plan.origin = new BlockPos(dx, dy, dz);
        return plan;
    }

    private static Plan capture(SpellRecipe recipe, Setup setup) {
        Volume region = region(recipe, setup);
        if (region == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BOX);
        }
        return new Plan(recipe, NONE, null, region, null, Order.RISE, setup.caster);
    }

    private static Plan stamp(SpellRecipe recipe, Setup setup) {
        Source source = setup.source;
        if (source == null) {
            return refused(recipe, setup, BuildVerdictPayload.POCKET_EMPTY);
        }
        Placed placed = Placed.at(source, recipe, null);
        Plan plan = new Plan(recipe, NONE, placed, null, placed, Order.RISE, setup.caster);
        plan.origin = placed.corner;
        return plan;
    }

    private static Plan move(BlockGetter level, SpellRecipe recipe, Setup setup) {
        Volume region = region(recipe, setup);
        if (region == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BOX);
        }
        Source source = setup.source != null ? setup.source : Source.live(level, region);
        BoundingBox from = region.bounds();
        Placed placed = Placed.at(source, recipe, new BlockPos(from.minX(), from.minY(), from.minZ()));
        Order order = Order.RISE;
        if (recipe.turn() == 0 && recipe.flip() == SpellRecipe.NO_FLIP && placed.bounds.intersects(from)) {
            order = Order.SHIFT;
        }
        Plan plan = new Plan(recipe, NONE, placed, region, placed, order, setup.caster);
        plan.origin = placed.corner;
        return plan;
    }

    private static Plan repeat(BlockGetter level, SpellRecipe recipe, Setup setup) {
        Volume region = region(recipe, setup);
        if (region == null) {
            return refused(recipe, setup, BuildVerdictPayload.NO_BOX);
        }
        Direction direction = recipe.direction();
        if (direction == null) {
            return new Plan(recipe, NONE, null, region, null, Order.RISE, setup.caster);
        }
        Source source = setup.source != null ? setup.source : Source.live(level, region);
        BoundingBox box = region.bounds();
        int gap = Math.max(0, recipe.size2());
        int length = direction.getAxis().choose(source.sizeX(), source.sizeY(), source.sizeZ());
        // The row stops where the world ends (its build height, its border): a copy beyond could never be built, and
        // a Repeat with no count would otherwise plan millions of copies above the sky.
        int count = (int) Math.min(Math.max(1, recipe.size()), copiesInside(SpellMath.world(level), box, direction, length, length + gap));
        if (count < 1) {
            return new Plan(recipe, NONE, null, region, null, Order.RISE, setup.caster);
        }
        Repeated copies = new Repeated(source, new BlockPos(box.minX(), box.minY(), box.minZ()), direction, count, gap);
        return new Plan(recipe, NONE, copies, region, copies, Order.RISE, setup.caster);
    }

    /**
     * How many copies of a Repeat ({@code length} long along {@code direction}, one every {@code step} blocks from the
     * source {@code box}) reach into the {@code world} at all.
     */
    private static long copiesInside(BoundingBox world, BoundingBox box, Direction direction, int length, int step) {
        Direction.Axis axis = direction.getAxis();
        long from = axis.choose(box.minX(), box.minY(), box.minZ());
        long room = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? axis.choose(world.maxX(), world.maxY(), world.maxZ()) - from
                : from + length - 1 - axis.choose(world.minX(), world.minY(), world.minZ());
        return room < 0 ? 0 : room / Math.max(1, step);
    }

    /** The block looked at for This kind, when it is in the region and not air; else null. */
    private static @Nullable Block kind(BlockGetter level, SpellRecipe recipe, Volume region) {
        BlockPos at = recipe.anchor();
        if (!region.contains(at.getX(), at.getY(), at.getZ()) || !SpellMath.loaded(level, at)) {
            return null;
        }
        BlockState state = level.getBlockState(at);
        return state.isAir() ? null : state.getBlock();
    }

    /** The material in every cell, waterlogged where the cell holds still water. */
    private static Rule conjure(Material material) {
        return (level, pos, cursor) -> {
            BlockState state = material.at(pos.getX(), pos.getY(), pos.getZ());
            return state.hasProperty(BlockStateProperties.WATERLOGGED) ? SpellMath.placedInto(state, level.getBlockState(pos)) : state;
        };
    }

    /** The family rule on every block that touches open space: Change's Only the outside, and Paint. */
    private static Rule skin(Material material) {
        return (level, pos, cursor) -> {
            BlockState old = level.getBlockState(pos);
            return SpellMath.canChange(old) && SpellMath.touchesOpen(level, pos) ? SpellMath.swap(old, material.at(pos.getX(), pos.getY(), pos.getZ()))
                    : null;
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // The plan

    /** How a plan's cells are walked; see the class comment. */
    private enum Order {
        /** Layer by layer bottom-up (or by columns when big). */
        RISE,
        /** Layer by layer top-down (or by columns when big). */
        MELT,
        /** Shell by shell from the heart (or by columns, bottom-up, when big). */
        SHELLS,
        /** A Move over itself: each axis against the shift, so a source cell moves before it is written over. */
        SHIFT
    }

    /**
     * One cast, planned: its cells (a {@link Volume}), the rule that gives each its block, and the order to walk them.
     * Made by {@link SpellPlanner#plan}. A plan holds no list of cells and can be walked any number of times
     * ({@link #cursor}).
     */
    public static final class Plan {
        private final SpellRecipe recipe;
        private final int refusal;
        private @Nullable Volume cells;
        private final @Nullable Volume region;
        private final @Nullable Rule rule;
        private final Order order;
        private final BlockPos caster;
        private final @Nullable BlockPos heart;
        private final @Nullable PullSurface surface;
        private @Nullable BlockPos origin;
        private boolean prepared;
        private long size = -1;

        private Plan(SpellRecipe recipe, int refusal, @Nullable Volume cells, @Nullable Volume region, @Nullable Rule rule, Order order,
                     BlockPos caster) {
            this(recipe, refusal, cells, region, rule, order, caster, null, null);
        }

        private Plan(SpellRecipe recipe, int refusal, @Nullable Volume cells, @Nullable Volume region, @Nullable Rule rule, Order order,
                     BlockPos caster, @Nullable BlockPos heart, @Nullable PullSurface surface) {
            this.recipe = recipe;
            this.refusal = refusal;
            this.cells = cells;
            this.region = region;
            this.rule = rule;
            this.order = order;
            this.caster = caster.immutable();
            this.heart = heart;
            this.surface = surface;
            this.prepared = surface == null;
        }

        public SpellRecipe recipe() {
            return recipe;
        }

        /**
         * Why the recipe cannot be cast at all ({@link BuildVerdictPayload#NO_BOX},
         * {@link BuildVerdictPayload#NO_BLOCK}, {@link BuildVerdictPayload#LOOK_INSIDE},
         * {@link BuildVerdictPayload#POCKET_EMPTY}), or {@link #NONE}.
         */
        public int refusal() {
            return refusal;
        }

        /**
         * Finishes what the plan needs from the world before its first cell (a Pull's surface) with about {@code reads}
         * block reads; true once ready. A plan that needs nothing is ready at once; anything that walks or measures an
         * unready plan finishes it first, whatever it costs.
         */
        public boolean prepare(int reads) {
            if (prepared) {
                return true;
            }
            if (surface == null || !surface.step(reads)) {
                return false;
            }
            prepared = true;
            if (surface.size() > 0) {
                cells = new PullCells(surface.cells(), surface.face(), recipe.anchor(), recipe.size());
            }
            return true;
        }

        private void ready() {
            prepare(Integer.MAX_VALUE);
        }

        /** Whether the cast changes any block (not a Copy's take, a Move's pick-up, a refusal or an empty Pull). */
        public boolean writes() {
            ready();
            return cells != null && rule != null;
        }

        /** The cells it may write (null when it writes nothing). */
        public @Nullable Volume cells() {
            ready();
            return cells;
        }

        /**
         * Where a shape's base centre (its offset 0, 0, 0) or a stamp's low corner (a Copy's, a Move's) stands in the
         * world, or null for the other spells: what a ghost drawn at an offset needs.
         */
        public @Nullable BlockPos origin() {
            return origin;
        }

        /** The cells it reads as its source (the box or the thing of a Copy's take, a Move, a Repeat), or null. */
        public @Nullable Volume region() {
            return region;
        }

        /** The box of the cells it may write, or null. */
        public @Nullable BoundingBox bounds() {
            Volume volume = cells();
            return volume == null ? null : volume.bounds();
        }

        /** Whether it writes the cell (for a Move: a source cell it lands on is written, not emptied). */
        public boolean covers(int x, int y, int z) {
            Volume volume = cells();
            return volume != null && volume.contains(x, y, z);
        }

        /**
         * How many cells it may write at most, before the world has its say (the size that decides the long hold,
         * Wanda's ceiling and the job's pace): exact for a box, a shape, a surface, a selected thing; for a stamp, a
         * Move or a Repeat the cells its source has (a pocket's, or the box's or the selected thing's read live), times
         * the copies. Cells beyond the build height or the world border are in it; a {@link Count} leaves them out.
         */
        public long size() {
            if (size < 0) {
                Volume volume = cells();
                size = volume == null ? 0 : volume instanceof Placed placed ? placed.source.cells()
                        : volume instanceof Repeated copies ? copies.source.cells() * copies.copies : volume.count();
            }
            return size;
        }

        /**
         * How its ripple sweeps, for everyone who sees it ({@link BuildFxPayload#RISE}, {@link BuildFxPayload#MELT},
         * {@link BuildFxPayload#SHELLS}, {@link BuildFxPayload#COLUMNS}).
         */
        public int order() {
            BoundingBox box = bounds();
            if (box != null && !fits(box) && order != Order.SHIFT) {
                return BuildFxPayload.COLUMNS;
            }
            return switch (order) {
                case MELT -> BuildFxPayload.MELT;
                case SHELLS -> heart != null ? BuildFxPayload.SHELLS : BuildFxPayload.RISE;
                default -> BuildFxPayload.RISE;
            };
        }

        /**
         * For a stamp, the pocket's cell that lands on {@code target}; for a Move or a Repeat, the cell of the world it
         * copies; null if no cell of the plan lands there.
         */
        public @Nullable BlockPos sourceOf(BlockPos target) {
            Volume volume = cells();
            if (volume instanceof Placed placed) {
                BlockPos local = placed.local(target.getX(), target.getY(), target.getZ());
                return local == null ? null : placed.origin == null ? local : placed.origin.offset(local);
            }
            if (volume instanceof Repeated copies) {
                BlockPos local = copies.local(target.getX(), target.getY(), target.getZ());
                return local == null ? null : copies.origin.offset(local);
            }
            return null;
        }

        /** A new walk over its cells, from the first. */
        public Cursor cursor() {
            Volume volume = cells();
            if (volume == null || rule == null) {
                return new Cursor(new EmptyWalk(), (level, pos, cursor) -> null);
            }
            BoundingBox box = volume.bounds();
            Walk walk;
            if (order == Order.SHIFT) {
                BoundingBox from = region == null ? box : region.bounds();
                int dx = box.minX() - from.minX();
                int dy = box.minY() - from.minY();
                int dz = box.minZ() - from.minZ();
                walk = new LayerWalk(volume, box, dy <= 0, dz == 0 ? caster.getZ() <= middle(box.minZ(), box.maxZ()) : dz < 0,
                        dx == 0 ? caster.getX() <= middle(box.minX(), box.maxX()) : dx < 0);
            } else if (!fits(box)) {
                walk = new ColumnWalk(volume, box, order != Order.MELT, caster);
            } else if (order == Order.SHELLS && heart != null) {
                walk = new ShellWalk(volume, box, heart);
            } else {
                walk = LayerWalk.from(volume, box, order != Order.MELT, caster);
            }
            return new Cursor(walk, rule);
        }

        /**
         * Every cell it changes, resolved against {@code level} as the cursor reaches it: a cell the spell leaves alone
         * is skipped, and so is one in a chunk that is not loaded (never read: a ghost leaves it out) or outside the
         * build height or the world border (never written); one that already holds its block is not. For a client's
         * ghost, and for tests.
         */
        public Iterator<Cell> walk(BlockGetter level) {
            Cursor cursor = cursor();
            return new Iterator<>() {
                private @Nullable Cell next;

                @Override
                public boolean hasNext() {
                    while (next == null && cursor.next()) {
                        if (!SpellMath.loaded(level, cursor.pos()) || SpellMath.outside(level, cursor.pos())) {
                            continue;
                        }
                        BlockState state = cursor.resolve(level);
                        if (state != null) {
                            next = new Cell(cursor.pos().immutable(), state, cursor.data(), cursor.from());
                        }
                    }
                    return next != null;
                }

                @Override
                public Cell next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    Cell cell = next;
                    next = null;
                    return cell;
                }
            };
        }

        /** A count of what it changes in {@code level}, done {@link Count#step a slice at a time}. */
        public Count count(BlockGetter level) {
            return new Count(cursor(), level);
        }
    }

    private static boolean fits(BoundingBox box) {
        return box.getXSpan() <= LAYERED_MAX && box.getZSpan() <= LAYERED_MAX;
    }

    private static int middle(int min, int max) {
        return (int) (((long) min + max) / 2);
    }

    /**
     * A walk over a plan's cells. {@link #next} moves to the next cell of its geometry and reads nothing (but for a
     * Move's or a Repeat's source read live, {@link Source#live}, which reads loaded chunks only); {@link #resolve}
     * then reads the world and answers the block the cell gets. {@link #pos} is the cursor's own position, valid until
     * the next move ({@code immutable()} to keep it).
     */
    public static final class Cursor {
        private final Walk walk;
        private final Rule rule;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private @Nullable CompoundTag data;
        private @Nullable BlockPos from;
        private long walked;

        private Cursor(Walk walk, Rule rule) {
            this.walk = walk;
            this.rule = rule;
        }

        /** Moves to the next cell; false when every cell has been walked. */
        public boolean next() {
            data = null;
            from = null;
            if (!walk.next(pos)) {
                return false;
            }
            walked++;
            return true;
        }

        /** The cell the cursor is on. */
        public BlockPos pos() {
            return pos;
        }

        /**
         * The block the cell gets, read against {@code level} now (the cell, and for the skin its neighbours), or null
         * when the spell leaves it as it is. Sets {@link #data} and {@link #from}.
         */
        public @Nullable BlockState resolve(BlockGetter level) {
            data = null;
            from = null;
            return rule.apply(level, pos, this);
        }

        /**
         * The block entity data the cell gets with its block (a stamp's, a Move's, a Repeat's), after {@link #resolve}:
         * the source's own tag, to be copied before anything changes it.
         */
        public @Nullable CompoundTag data() {
            return data;
        }

        /** For a Move, the cell the block comes from (which the Move empties), after {@link #resolve}. */
        public @Nullable BlockPos from() {
            return from;
        }

        /** How many cells the cursor has moved over. */
        public long walked() {
            return walked;
        }
    }

    /**
     * What a plan changes in a world, counted a slice at a time (the client's 50,000 cells a tick, the job's COUNT
     * phase): the cells walked, those whose block changes (a cell that gets data counts as changed: the data is not
     * compared), those of them that remove or replace an existing block (not air, not replaceable: the short hold), and
     * those that make a volatile block ({@code enhanced:build_volatile}: Wanda's cap). A cell in a chunk the world has
     * not loaded (D-B 10.3 P1), and then one outside the build height or the world border (P2), is skipped and counted
     * apart: neither ever changes, so neither is paid for.
     */
    public static final class Count {
        private final Cursor cursor;
        private final BlockGetter level;
        private long walked;
        private long changed;
        private long removed;
        private long volatiles;
        private long unloaded;
        private long outside;
        private boolean done;

        private Count(Cursor cursor, BlockGetter level) {
            this.cursor = cursor;
            this.level = level;
        }

        /** Counts on for at most {@code cells} cells; true once every cell is counted. */
        public boolean step(int cells) {
            for (int i = 0; i < cells && !done; i++) {
                if (!cursor.next()) {
                    done = true;
                    break;
                }
                walked++;
                BlockPos pos = cursor.pos();
                if (!SpellMath.loaded(level, pos)) {
                    unloaded++;
                    continue;
                }
                if (SpellMath.outside(level, pos)) {
                    outside++;
                    continue;
                }
                BlockState old = level.getBlockState(pos);
                BlockState now = cursor.resolve(level);
                if (now == null || (now.equals(old) && cursor.data() == null)) {
                    continue;
                }
                changed++;
                if (!old.isAir() && !old.canBeReplaced() && old.getBlock() != now.getBlock()) {
                    removed++;
                }
                if (now.is(ModTags.BUILD_VOLATILE)) {
                    volatiles++;
                }
            }
            return done;
        }

        public boolean done() {
            return done;
        }

        public long walked() {
            return walked;
        }

        public long changed() {
            return changed;
        }

        public long removed() {
            return removed;
        }

        public long volatiles() {
            return volatiles;
        }

        public long unloaded() {
            return unloaded;
        }

        /** The cells outside the build height or the world border, which no spell writes. */
        public long outside() {
            return outside;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Rules and the volumes only the planner makes

    /** The block a cell gets (reading the world through {@code level}), or null to leave it. */
    @FunctionalInterface
    private interface Rule {
        @Nullable BlockState apply(BlockGetter level, BlockPos pos, Cursor cursor);
    }

    /** A source stamped where the ghost stands: the cells it has, turned and flipped; also the rule that gives them. */
    private static final class Placed implements Volume, Rule {
        private final Source source;
        private final Placement placement;
        private final BlockPos corner;
        private final BoundingBox bounds;
        private final @Nullable BlockPos origin;

        private Placed(Source source, Placement placement, BlockPos corner, @Nullable BlockPos origin) {
            this.source = source;
            this.placement = placement;
            this.corner = corner;
            this.origin = origin;
            this.bounds = new BoundingBox(corner.getX(), corner.getY(), corner.getZ(), corner.getX() + placement.targetSizeX() - 1,
                    corner.getY() + placement.sizeY() - 1, corner.getZ() + placement.targetSizeZ() - 1);
        }

        /** The source stamped as the recipe says: turned, flipped, standing on the face looked at, lifted. */
        static Placed at(Source source, SpellRecipe recipe, @Nullable BlockPos origin) {
            Placement placement = new Placement(source.sizeX(), source.sizeY(), source.sizeZ(), recipe.turn(), recipe.flip());
            BlockPos corner = SpellMath.stand(placement.targetSizeX(), placement.sizeY(), placement.targetSizeZ(), recipe.anchor(),
                    recipe.direction(), recipe.size());
            return new Placed(source, placement, corner, origin);
        }

        @Nullable BlockPos local(int x, int y, int z) {
            BlockPos local = placement.source(x - corner.getX(), y - corner.getY(), z - corner.getZ());
            return local == null || source.state(local.getX(), local.getY(), local.getZ()) == null ? null : local;
        }

        @Override
        public BoundingBox bounds() {
            return bounds;
        }

        @Override
        public boolean contains(int x, int y, int z) {
            return bounds.isInside(x, y, z) && local(x, y, z) != null;
        }

        @Override
        public @Nullable BlockState apply(BlockGetter level, BlockPos pos, Cursor cursor) {
            BlockPos local = placement.source(pos.getX() - corner.getX(), pos.getY() - corner.getY(), pos.getZ() - corner.getZ());
            if (local == null) {
                return null;
            }
            BlockState state = source.state(local.getX(), local.getY(), local.getZ());
            if (state == null) {
                return null;
            }
            cursor.data = source.data(local.getX(), local.getY(), local.getZ());
            cursor.from = origin == null ? null : origin.offset(local);
            return placement.state(state);
        }
    }

    /** Copies of a source in a row: copy {@code i} (1 to {@code copies}) is {@code i} times its size and gap away. */
    private static final class Repeated implements Volume, Rule {
        private final Source source;
        private final BlockPos origin;
        private final Direction direction;
        private final int copies;
        private final int step;
        private final int length;
        private final BoundingBox bounds;

        private Repeated(Source source, BlockPos origin, Direction direction, int copies, int gap) {
            this.source = source;
            this.origin = origin;
            this.direction = direction;
            this.copies = copies;
            Direction.Axis axis = direction.getAxis();
            this.length = axis == Direction.Axis.X ? source.sizeX() : axis == Direction.Axis.Y ? source.sizeY() : source.sizeZ();
            this.step = length + gap;
            BoundingBox first = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + source.sizeX() - 1,
                    origin.getY() + source.sizeY() - 1, origin.getZ() + source.sizeZ() - 1);
            BoundingBox near = first.moved(direction.getStepX() * step, direction.getStepY() * step, direction.getStepZ() * step);
            BoundingBox far = first.moved(direction.getStepX() * step * copies, direction.getStepY() * step * copies,
                    direction.getStepZ() * step * copies);
            this.bounds = new BoundingBox(Math.min(near.minX(), far.minX()), Math.min(near.minY(), far.minY()), Math.min(near.minZ(), far.minZ()),
                    Math.max(near.maxX(), far.maxX()), Math.max(near.maxY(), far.maxY()), Math.max(near.maxZ(), far.maxZ()));
        }

        /** The source's cell copied to {@code x, y, z}, or null. */
        @Nullable BlockPos local(int x, int y, int z) {
            if (!bounds.isInside(x, y, z)) {
                return null;
            }
            int lx = x - origin.getX();
            int ly = y - origin.getY();
            int lz = z - origin.getZ();
            int along = direction.getAxis().choose(lx, ly, lz);
            int offset = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? along : length - 1 - along;
            int copy = Math.floorDiv(offset, step);
            int in = offset - copy * step;
            if (copy < 1 || copy > copies || in >= length) {
                return null;
            }
            int local = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? in : length - 1 - in;
            switch (direction.getAxis()) {
                case X -> lx = local;
                case Y -> ly = local;
                case Z -> lz = local;
            }
            return source.state(lx, ly, lz) == null ? null : new BlockPos(lx, ly, lz);
        }

        @Override
        public BoundingBox bounds() {
            return bounds;
        }

        @Override
        public boolean contains(int x, int y, int z) {
            return local(x, y, z) != null;
        }

        @Override
        public @Nullable BlockState apply(BlockGetter level, BlockPos pos, Cursor cursor) {
            BlockPos local = local(pos.getX(), pos.getY(), pos.getZ());
            if (local == null) {
                return null;
            }
            cursor.data = source.data(local.getX(), local.getY(), local.getZ());
            return source.state(local.getX(), local.getY(), local.getZ());
        }
    }

    /**
     * The layers of Pull &amp; Push: {@code layers} above 0 are the surface's copies 1 to {@code layers} steps in front
     * of it; below 0, the surface itself and the layers behind it, {@code -layers} in all.
     */
    private record PullCells(Volume surface, Direction face, BlockPos anchor, int layers) implements Volume {
        /** The surface's cell in line with {@code pos} along the face. */
        static BlockPos surfaceCell(BlockPos pos, Direction face, BlockPos anchor) {
            return switch (face.getAxis()) {
                case X -> new BlockPos(anchor.getX(), pos.getY(), pos.getZ());
                case Y -> new BlockPos(pos.getX(), anchor.getY(), pos.getZ());
                case Z -> new BlockPos(pos.getX(), pos.getY(), anchor.getZ());
            };
        }

        /** How many steps in front of the surface (0 is the surface, below 0 behind it) the cell is. */
        private int step(int x, int y, int z) {
            int along = face.getAxis().choose(x, y, z) - face.getAxis().choose(anchor.getX(), anchor.getY(), anchor.getZ());
            return face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? along : -along;
        }

        @Override
        public BoundingBox bounds() {
            BoundingBox plane = surface.bounds();
            int near = layers > 0 ? 1 : layers + 1;
            int far = layers > 0 ? layers : 0;
            BoundingBox a = plane.moved(face.getStepX() * near, face.getStepY() * near, face.getStepZ() * near);
            BoundingBox b = plane.moved(face.getStepX() * far, face.getStepY() * far, face.getStepZ() * far);
            return new BoundingBox(Math.min(a.minX(), b.minX()), Math.min(a.minY(), b.minY()), Math.min(a.minZ(), b.minZ()),
                    Math.max(a.maxX(), b.maxX()), Math.max(a.maxY(), b.maxY()), Math.max(a.maxZ(), b.maxZ()));
        }

        @Override
        public boolean contains(int x, int y, int z) {
            int k = step(x, y, z);
            if (layers > 0 ? k < 1 || k > layers : k > 0 || k <= layers) {
                return false;
            }
            BlockPos cell = surfaceCell(new BlockPos(x, y, z), face, anchor);
            return surface.contains(cell.getX(), cell.getY(), cell.getZ());
        }

        @Override
        public long count() {
            return surface.count() * Math.abs(layers);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Walks

    /** The positions of a plan in its order. */
    private abstract static class Walk {
        /** Moves {@code out} to the next cell; false at the end. */
        abstract boolean next(BlockPos.MutableBlockPos out);
    }

    private static final class EmptyWalk extends Walk {
        @Override
        boolean next(BlockPos.MutableBlockPos out) {
            return false;
        }
    }

    /**
     * Layer by layer over a part of a volume ({@code area}): up or down, the rows of each layer one way along z, the
     * cells of each row one way along x.
     */
    private static final class LayerWalk extends Walk {
        private final Volume volume;
        private final BoundingBox area;
        private final boolean up;
        private final boolean zUp;
        private final boolean xUp;
        private int y;
        private int z;
        private int[] row = SpellMath.NO_RUNS;
        private int run;
        private int x;
        private boolean started;

        LayerWalk(Volume volume, BoundingBox area, boolean up, boolean zUp, boolean xUp) {
            this.volume = volume;
            this.area = area;
            this.up = up;
            this.zUp = zUp;
            this.xUp = xUp;
        }

        /** Rows from the side nearest the caster. */
        static LayerWalk from(Volume volume, BoundingBox area, boolean up, BlockPos caster) {
            return new LayerWalk(volume, area, up, caster.getZ() <= middle(area.minZ(), area.maxZ()), caster.getX() <= middle(area.minX(), area.maxX()));
        }

        @Override
        boolean next(BlockPos.MutableBlockPos out) {
            while (true) {
                if (run >= 0 && run < row.length) {
                    int from = row[run];
                    int to = row[run + 1];
                    if (xUp ? x <= to : x >= from) {
                        out.set(x, y, z);
                        x += xUp ? 1 : -1;
                        if (xUp ? x > to : x < from) {
                            run += xUp ? 2 : -2;
                            if (run >= 0 && run < row.length) {
                                x = xUp ? row[run] : row[run + 1];
                            }
                        }
                        return true;
                    }
                }
                if (!nextRow()) {
                    return false;
                }
            }
        }

        private boolean nextRow() {
            if (!started) {
                started = true;
                y = up ? area.minY() : area.maxY();
                z = zUp ? area.minZ() : area.maxZ();
            } else {
                z += zUp ? 1 : -1;
                if (zUp ? z > area.maxZ() : z < area.minZ()) {
                    z = zUp ? area.minZ() : area.maxZ();
                    y += up ? 1 : -1;
                }
            }
            if (up ? y > area.maxY() : y < area.minY()) {
                return false;
            }
            row = volume.runs(y, z, area.minX(), area.maxX());
            if (row.length == 0) {
                run = -1;
                return true;
            }
            run = xUp ? 0 : row.length - 2;
            x = xUp ? row[run] : row[run + 1];
            return true;
        }
    }

    /**
     * Chunk column by chunk column, in a square spiral from the column nearest the caster, each column layer by layer
     * (rows from the caster's side).
     */
    private static final class ColumnWalk extends Walk {
        private final Volume volume;
        private final BoundingBox box;
        private final boolean up;
        private final BlockPos caster;
        private final int minCx;
        private final int maxCx;
        private final int minCz;
        private final int maxCz;
        private final int startCx;
        private final int startCz;
        private final int rings;
        private int ring;
        private int index = -1;
        private @Nullable LayerWalk column;

        ColumnWalk(Volume volume, BoundingBox box, boolean up, BlockPos caster) {
            this.volume = volume;
            this.box = box;
            this.up = up;
            this.caster = caster;
            this.minCx = box.minX() >> 4;
            this.maxCx = box.maxX() >> 4;
            this.minCz = box.minZ() >> 4;
            this.maxCz = box.maxZ() >> 4;
            this.startCx = Math.clamp(caster.getX() >> 4, minCx, maxCx);
            this.startCz = Math.clamp(caster.getZ() >> 4, minCz, maxCz);
            this.rings = Math.max(Math.max(startCx - minCx, maxCx - startCx), Math.max(startCz - minCz, maxCz - startCz));
        }

        @Override
        boolean next(BlockPos.MutableBlockPos out) {
            while (true) {
                if (column != null && column.next(out)) {
                    return true;
                }
                if (!nextColumn()) {
                    return false;
                }
            }
        }

        /** The next column of the spiral that is in the box; false after the last. */
        private boolean nextColumn() {
            while (ring <= rings) {
                index++;
                int perimeter = ring == 0 ? 1 : 8 * ring;
                if (index >= perimeter) {
                    ring++;
                    index = -1;
                    continue;
                }
                int cx;
                int cz;
                if (ring == 0) {
                    cx = startCx;
                    cz = startCz;
                } else {
                    // Round the ring clockwise from its north-west corner: the north side, the east, the south, the
                    // west.
                    int side = index / (2 * ring);
                    int along = index % (2 * ring);
                    switch (side) {
                        case 0 -> {
                            cx = startCx - ring + along;
                            cz = startCz - ring;
                        }
                        case 1 -> {
                            cx = startCx + ring;
                            cz = startCz - ring + along;
                        }
                        case 2 -> {
                            cx = startCx + ring - along;
                            cz = startCz + ring;
                        }
                        default -> {
                            cx = startCx - ring;
                            cz = startCz + ring - along;
                        }
                    }
                }
                if (cx < minCx || cx > maxCx || cz < minCz || cz > maxCz) {
                    continue;
                }
                BoundingBox area = new BoundingBox(Math.max(box.minX(), cx << 4), box.minY(), Math.max(box.minZ(), cz << 4),
                        Math.min(box.maxX(), (cx << 4) + 15), box.maxY(), Math.min(box.maxZ(), (cz << 4) + 15));
                column = LayerWalk.from(volume, area, up, caster);
                return true;
            }
            return false;
        }
    }

    /**
     * Shell by shell from the heart: shell {@code k} is every cell whose squared distance from the heart is at least
     * {@code k^2} and below {@code (k + 1)^2}; inside a shell, by layer, row and x.
     */
    private static final class ShellWalk extends Walk {
        private final Volume volume;
        private final BoundingBox box;
        private final BlockPos heart;
        private final int lastShell;
        private int k;
        private int dy;
        private int dz;
        private boolean started;
        private int[] row = SpellMath.NO_RUNS;
        private int run;
        private int x;

        ShellWalk(Volume volume, BoundingBox box, BlockPos heart) {
            this.volume = volume;
            this.box = box;
            this.heart = heart;
            long far = 0;
            for (int cx : new int[]{box.minX(), box.maxX()}) {
                for (int cy : new int[]{box.minY(), box.maxY()}) {
                    for (int cz : new int[]{box.minZ(), box.maxZ()}) {
                        long ddx = cx - heart.getX();
                        long ddy = cy - heart.getY();
                        long ddz = cz - heart.getZ();
                        far = Math.max(far, ddx * ddx + ddy * ddy + ddz * ddz);
                    }
                }
            }
            this.lastShell = SpellMath.isqrt(far);
        }

        @Override
        boolean next(BlockPos.MutableBlockPos out) {
            while (true) {
                if (run < row.length) {
                    out.set(x, heart.getY() + dy, heart.getZ() + dz);
                    x++;
                    if (x > row[run + 1]) {
                        run += 2;
                        if (run < row.length) {
                            x = row[run];
                        }
                    }
                    return true;
                }
                if (!nextRow()) {
                    return false;
                }
            }
        }

        private boolean nextRow() {
            while (true) {
                if (!started) {
                    started = true;
                    k = 0;
                    dy = lowY();
                    dz = lowZ() - 1;
                }
                dz++;
                if (dz > highZ()) {
                    dy++;
                    dz = lowZ();
                    if (dy > highY()) {
                        k++;
                        if (k > lastShell) {
                            row = SpellMath.NO_RUNS;
                            run = 0;
                            return false;
                        }
                        dy = lowY();
                        dz = lowZ();
                    }
                }
                long s = (long) dy * dy + (long) dz * dz;
                long high = (long) (k + 1) * (k + 1) - 1 - s;
                if (high < 0) {
                    continue;
                }
                int a = SpellMath.isqrt(high);
                long low = (long) k * k - s;
                int c = low <= 0 ? 0 : SpellMath.ceilSqrt(low);
                if (c > a) {
                    continue;
                }
                int hx = heart.getX();
                int y = heart.getY() + dy;
                int z = heart.getZ() + dz;
                int[] cells = c == 0 ? volume.runs(y, z, hx - a, hx + a)
                        : join(volume.runs(y, z, hx - a, hx - c), volume.runs(y, z, hx + c, hx + a));
                if (cells.length > 0) {
                    row = cells;
                    run = 0;
                    x = row[0];
                    return true;
                }
            }
        }

        private int lowY() {
            return Math.max(box.minY() - heart.getY(), -(k + 1));
        }

        private int highY() {
            return Math.min(box.maxY() - heart.getY(), k + 1);
        }

        private int lowZ() {
            return Math.max(box.minZ() - heart.getZ(), -(k + 1));
        }

        private int highZ() {
            return Math.min(box.maxZ() - heart.getZ(), k + 1);
        }

        private static int[] join(int[] left, int[] right) {
            if (left.length == 0) {
                return right;
            }
            if (right.length == 0) {
                return left;
            }
            int[] out = new int[left.length + right.length];
            System.arraycopy(left, 0, out, 0, left.length);
            System.arraycopy(right, 0, out, left.length, right.length);
            return out;
        }
    }

    private SpellPlanner() {
    }
}
