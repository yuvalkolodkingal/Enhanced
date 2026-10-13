package com.kolodkingal.enhanced.build;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What one cast of Build Magic is, as the client sends it ({@code BuildCastPayload}): never cells or block states, only
 * the recipe the server plans the same cells from, with the same planner, as the client planned its ghost. About 60 bytes
 * plus 8 a seed; nothing in it is written into the world as it came (D-B 13.4): the server {@link #clamped clamps} every
 * number, reads the materials itself and checks every cell.
 *
 * <p>The meaning of the numbers depends on the spell:
 *
 * <table>
 *     <caption>The numbers of a recipe</caption>
 *     <tr><th>spell</th><th>{@code style}</th><th>{@code size}</th><th>{@code size2}</th></tr>
 *     <tr><td>Fill, Vanish, Change</td><td>its {@link FillStyle}, {@link VanishStyle}, {@link ChangeStyle}</td><td>0</td><td>0</td></tr>
 *     <tr><td>Paint</td><td>0</td><td>the brush's radius</td><td>0</td></tr>
 *     <tr><td>Pull &amp; Push</td><td>0</td><td>layers: out of the face above 0, into it below</td><td>0</td></tr>
 *     <tr><td>Shapes</td><td>its {@link ShapeKind}</td><td>the radius</td><td>the height</td></tr>
 *     <tr><td>Copy, Move</td><td>0</td><td>the ghost's lift off its surface (below 0: into it)</td><td>0</td></tr>
 *     <tr><td>Repeat</td><td>0</td><td>how many copies</td><td>the gap between them</td></tr>
 *     <tr><td>Land, Grow</td><td>its {@link LandStyle}, {@link GrowStyle}</td><td>the brush's radius</td><td>0</td></tr>
 * </table>
 *
 * @param spell    which spell
 * @param op       what the right button does with it
 * @param style    the spell's style or shape kind (its ordinal)
 * @param region   {@link #NO_REGION}, {@link #BOX} (the box {@code a}-{@code b}) or {@link #MASK} (the thing the magic
 *                 selection floods from {@code seeds}; {@code a}-{@code b} is then its bounding hint)
 * @param a        one corner of the box
 * @param b        the opposite corner
 * @param seeds    where the magic selection starts, at most {@link #MAX_SEEDS}
 * @param anchor   where the spell sits: the ghost's base, a shape's base, the brush's middle, the block of the face Pull
 *                 works on, the block of "this kind"
 * @param face     the face looked at ({@link Direction#get3DDataValue()}), or {@link #NO_FACE}
 * @param size     the main number (see the table)
 * @param size2    the second number (see the table)
 * @param turn     quarter turns clockwise seen from above, 0 to 3
 * @param flip     {@link #NO_FLIP}, {@link #FLIP_X}, {@link #FLIP_Z} or {@link #UPSIDE_DOWN}
 * @param flags    {@link #HOLLOW}, {@link #MIX}, {@link #PLANTED}
 * @param material {@link #HAND} (the block in the main hand) or {@link #PICKED} (the block the hero last picked)
 * @param seed     shapes the mix of the hotbar's blocks and the brushes' choices, so the server makes what the client
 *                 showed
 * @param stroke   the number of the Paint or brush stroke a {@link Op#DAB} belongs to
 */
public record SpellRecipe(Spell spell, Op op, int style, int region, BlockPos a, BlockPos b, List<BlockPos> seeds, BlockPos anchor,
                          int face, int size, int size2, int turn, int flip, int flags, int material, long seed, int stroke) {
    /** What the right button does. The ordinal travels: append only. */
    public enum Op {
        /** The spell itself. */
        CAST,
        /** Copy's first click: the box goes into the hero's pocket (nothing in the world changes). */
        TAKE,
        /** Copy's next clicks: a copy of the pocket where the ghost is. */
        STAMP,
        /** Move's first click: the box is picked up (it stays where it is until it is put down). */
        PICK_UP,
        /** Move's second click: what was picked up is put down where the ghost is, and its old place emptied. */
        PUT_DOWN,
        /** One dab of a Paint or brush stroke, sent while the button is held. */
        DAB;

        /** The op with this id; any number is an op. */
        public static Op byId(int id) {
            Op[] all = values();
            return all[Math.floorMod(id, all.length)];
        }
    }

    public static final int NO_REGION = 0;
    public static final int BOX = 1;
    public static final int MASK = 2;
    private static final int REGIONS = 3;

    public static final int NO_FACE = -1;

    public static final int NO_FLIP = 0;
    /** Mirrored across the x axis (east and west swap). */
    public static final int FLIP_X = 1;
    /** Mirrored across the z axis (north and south swap). */
    public static final int FLIP_Z = 2;
    /** Upside down: {@code half} top and bottom swap, and {@code facing} up and down. */
    public static final int UPSIDE_DOWN = 3;
    private static final int FLIPS = 4;

    /** A shape is hollow (a one-block shell). */
    public static final int HOLLOW = 1;
    /** The material is the hotbar's mix, not one block. */
    public static final int MIX = 2;
    /** A shape is planted: it stays where the hero clicked instead of following the crosshair. */
    public static final int PLANTED = 4;
    private static final int ALL_FLAGS = HOLLOW | MIX | PLANTED;

    public static final int HAND = 0;
    public static final int PICKED = 1;
    private static final int MATERIALS = 2;

    /** The most places a magic selection may start from. */
    public static final int MAX_SEEDS = 8;
    /** The widest gap between the copies of a Repeat. */
    public static final int MAX_GAP = 16;

    private static final StreamCodec<ByteBuf, List<BlockPos>> SEEDS = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SEEDS));

    /**
     * Written by hand, as {@code Hex.STREAM_CODEC}: {@code byte spell, op, style, region}, {@code BlockPos a, b},
     * {@code List<BlockPos> seeds} (at most 8; more is refused when read), {@code BlockPos anchor}, {@code byte face},
     * {@code VAR_INT size, size2}, {@code byte turn, flip, flags, material}, {@code VAR_LONG seed}, {@code VAR_INT stroke}:
     * at most 123 bytes. A spell or an op read from any byte is one that exists ({@code floorMod}); everything else is
     * clamped by {@link #clamped} on the server.
     */
    public static final StreamCodec<ByteBuf, SpellRecipe> STREAM_CODEC = StreamCodec.of((buf, r) -> {
        buf.writeByte(r.spell.id());
        buf.writeByte(r.op.ordinal());
        buf.writeByte(r.style);
        buf.writeByte(r.region);
        BlockPos.STREAM_CODEC.encode(buf, r.a);
        BlockPos.STREAM_CODEC.encode(buf, r.b);
        SEEDS.encode(buf, r.seeds);
        BlockPos.STREAM_CODEC.encode(buf, r.anchor);
        buf.writeByte(r.face);
        ByteBufCodecs.VAR_INT.encode(buf, r.size);
        ByteBufCodecs.VAR_INT.encode(buf, r.size2);
        buf.writeByte(r.turn);
        buf.writeByte(r.flip);
        buf.writeByte(r.flags);
        buf.writeByte(r.material);
        ByteBufCodecs.VAR_LONG.encode(buf, r.seed);
        ByteBufCodecs.VAR_INT.encode(buf, r.stroke);
    }, buf -> {
        Spell spell = Spell.byId(buf.readByte());
        Op op = Op.byId(buf.readByte());
        int style = buf.readByte();
        int region = buf.readByte();
        BlockPos a = BlockPos.STREAM_CODEC.decode(buf);
        BlockPos b = BlockPos.STREAM_CODEC.decode(buf);
        List<BlockPos> seeds = SEEDS.decode(buf);
        BlockPos anchor = BlockPos.STREAM_CODEC.decode(buf);
        int face = buf.readByte();
        int size = ByteBufCodecs.VAR_INT.decode(buf);
        int size2 = ByteBufCodecs.VAR_INT.decode(buf);
        int turn = buf.readByte();
        int flip = buf.readByte();
        int flags = buf.readByte();
        int material = buf.readByte();
        long seed = ByteBufCodecs.VAR_LONG.decode(buf);
        int stroke = ByteBufCodecs.VAR_INT.decode(buf);
        return new SpellRecipe(spell, op, style, region, a, b, seeds, anchor, face, size, size2, turn, flip, flags, material, seed, stroke);
    });

    public SpellRecipe {
        seeds = List.copyOf(seeds);
    }

    /**
     * This recipe with every number forced into range, the first thing the server does with a cast: the style by
     * {@code floorMod} over the spell's styles, the region, the turn, the flip and the material likewise, unknown flags
     * dropped, a face that is no direction made none, at most {@link #MAX_SEEDS} seeds, every position moved inside the
     * world, and the two sizes into the spell's range from {@code limits} (the table above; an unused size is 0, as is
     * the height of a shape without one, {@link ShapeKind#hasHeight()}).
     */
    public SpellRecipe clamped(RecipeLimits limits) {
        List<BlockPos> inWorld = seeds.stream().limit(MAX_SEEDS).map(limits::inWorld).toList();
        int cleanFace = face >= 0 && face < Direction.values().length ? face : NO_FACE;
        int first;
        int second;
        switch (spell) {
            case PAINT, LAND, GROW -> {
                first = Mth.clamp(size, 0, Math.max(0, limits.brush()));
                second = 0;
            }
            case PULL -> {
                int side = Math.max(1, limits.side());
                first = Mth.clamp(size, -side, side);
                second = 0;
            }
            case SHAPE -> {
                first = Mth.clamp(size, 1, Math.max(1, limits.shapeRadius()));
                boolean tall = ShapeKind.byId(Math.floorMod(style, spell.styles())).hasHeight();
                second = tall ? Mth.clamp(size2, 1, Math.max(1, limits.shapeHeight())) : 0;
            }
            case COPY, MOVE -> {
                int reach = Math.max(0, limits.reach());
                first = Mth.clamp(size, -reach, reach);
                second = 0;
            }
            case REPEAT -> {
                first = Mth.clamp(size, 1, Math.max(1, limits.repeatCopies()));
                second = Mth.clamp(size2, 0, MAX_GAP);
            }
            default -> {
                first = 0;
                second = 0;
            }
        }
        return new SpellRecipe(spell, op, Math.floorMod(style, spell.styles()), Math.floorMod(region, REGIONS), limits.inWorld(a),
                limits.inWorld(b), inWorld, limits.inWorld(anchor), cleanFace, first, second, Math.floorMod(turn, 4),
                Math.floorMod(flip, FLIPS), flags & ALL_FLAGS, Math.floorMod(material, MATERIALS), seed, stroke);
    }

    public FillStyle fillStyle() {
        return FillStyle.byId(style);
    }

    public VanishStyle vanishStyle() {
        return VanishStyle.byId(style);
    }

    public ChangeStyle changeStyle() {
        return ChangeStyle.byId(style);
    }

    public ShapeKind shapeKind() {
        return ShapeKind.byId(style);
    }

    public LandStyle landStyle() {
        return LandStyle.byId(style);
    }

    public GrowStyle growStyle() {
        return GrowStyle.byId(style);
    }

    /** The face looked at, or null for none. */
    public @Nullable Direction direction() {
        return face >= 0 && face < Direction.values().length ? Direction.from3DDataValue(face) : null;
    }

    /** The turn as vanilla's rotation of blocks (clockwise seen from above). */
    public Rotation rotation() {
        return Rotation.values()[Math.floorMod(turn, 4)];
    }

    public boolean hollow() {
        return (flags & HOLLOW) != 0;
    }

    public boolean mix() {
        return (flags & MIX) != 0;
    }

    public boolean planted() {
        return (flags & PLANTED) != 0;
    }
}
