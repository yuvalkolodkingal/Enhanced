package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.build.BuildFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → everyone within 128 blocks of a Build Magic spell, and its caster: a moment of the spell everyone near should
 * see (the cast gesture, the frame and the ripple that sweeps the box, the undo running backwards, Billy's rank-up). At
 * most 52 bytes. No block travels in it: the blocks change in the world as any block does.
 *
 * @param kind     {@link #START}, {@link #PROGRESS} (every 10 ticks), {@link #DONE}, {@link #UNDO_START},
 *                 {@link #STOPPED}, {@link #FIZZLE} (the caster only), {@link #RANK_UP} (within 32 blocks of Billy)
 * @param job      the spell's number on the server (the client follows one spell from START to its end)
 * @param casterId the caster's entity id
 * @param hero     {@link #WANDA} or {@link #BILLY}
 * @param min      the spell's box, its low corner
 * @param max      its high corner
 * @param origin   where the ripple starts (the point of the box nearest the caster)
 * @param permille how far the spell is, in thousandths
 * @param cells    how many blocks it changes
 * @param momId    the entity id of the Scarlet Witch who helps Billy (her scarlet thread), or -1
 * @param order    how the ripple sweeps: {@link #RISE}, {@link #MELT}, {@link #SHELLS}, {@link #COLUMNS}, or {@link #POP}
 *                 (an Instant spell: no sweep)
 */
public record BuildFxPayload(int kind, int job, int casterId, int hero, BlockPos min, BlockPos max, BlockPos origin, int permille,
                             int cells, int momId, int order) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int PROGRESS = 1;
    public static final int DONE = 2;
    public static final int UNDO_START = 3;
    public static final int STOPPED = 4;
    public static final int FIZZLE = 5;
    public static final int RANK_UP = 6;

    public static final int WANDA = 0;
    public static final int BILLY = 1;

    /** Layer by layer, from the ground up (fills, stamps). */
    public static final int RISE = 0;
    /** Layer by layer, from the top down (vanishing). */
    public static final int MELT = 1;
    /** Shell by shell from the heart (round shapes). */
    public static final int SHELLS = 2;
    /** Chunk column by chunk column, outward from the caster (a valley-sized spell). */
    public static final int COLUMNS = 3;

    /** All at once (Wanda's Instant level): no sweep, the box flashes where the blocks appear. */
    public static final int POP = 4;

    /** No Scarlet Witch helps. */
    public static final int NO_MOM = -1;

    public static final Type<BuildFxPayload> TYPE = new Type<>(Enhanced.id("build_fx"));

    public static final StreamCodec<ByteBuf, BuildFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind);
                ByteBufCodecs.VAR_INT.encode(buf, p.job);
                ByteBufCodecs.VAR_INT.encode(buf, p.casterId);
                buf.writeByte(p.hero);
                BlockPos.STREAM_CODEC.encode(buf, p.min);
                BlockPos.STREAM_CODEC.encode(buf, p.max);
                BlockPos.STREAM_CODEC.encode(buf, p.origin);
                ByteBufCodecs.VAR_INT.encode(buf, p.permille);
                ByteBufCodecs.VAR_INT.encode(buf, p.cells);
                ByteBufCodecs.VAR_INT.encode(buf, p.momId);
                buf.writeByte(p.order);
            },
            buf -> new BuildFxPayload(buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(),
                    BlockPos.STREAM_CODEC.decode(buf), BlockPos.STREAM_CODEC.decode(buf), BlockPos.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The running spells are drawn here (the client class is loaded only when this runs, on a client). */
    static void handle(BuildFxPayload payload, IPayloadContext context) {
        BuildFx.accept(payload);
    }
}
