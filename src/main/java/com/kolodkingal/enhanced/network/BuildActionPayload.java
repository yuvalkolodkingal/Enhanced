package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildMagic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: something a builder does that is not a cast (at most 22 bytes). What {@code a}, {@code b} and
 * {@code value} mean depends on the action:
 *
 * <ul>
 *     <li>{@link #MODE}: build mode on ({@code value} 1) or off (0).</li>
 *     <li>{@link #UNDO}, {@link #REDO}: Ctrl + Z, Ctrl + Y, also with build mode off.</li>
 *     <li>{@link #PICK}: middle click; {@code a} is the block to read (the server reads its state itself).</li>
 *     <li>{@link #PICK_BLOCK}: a block chosen on the palette page; {@code value} is its registry id.</li>
 *     <li>{@link #HAND}: back to the block in the hand.</li>
 *     <li>{@link #STROKE_END}: the right button let go of a Paint or brush stroke; {@code value} is the stroke.</li>
 *     <li>{@link #VIEW}: the box others see, {@code a}-{@code b}; {@code value} 0 for none, else the spell's id + 1.</li>
 *     <li>{@link #POCKET}: the client lacks the copy {@code value} (after a relog) and asks for it.</li>
 * </ul>
 */
public record BuildActionPayload(int action, BlockPos a, BlockPos b, int value) implements CustomPacketPayload {
    public static final int MODE = 0;
    public static final int UNDO = 1;
    public static final int REDO = 2;
    public static final int PICK = 3;
    public static final int PICK_BLOCK = 4;
    public static final int HAND = 5;
    public static final int STROKE_END = 6;
    public static final int VIEW = 7;
    public static final int POCKET = 8;

    public static final Type<BuildActionPayload> TYPE = new Type<>(Enhanced.id("build_action"));

    public static final StreamCodec<ByteBuf, BuildActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.action);
                BlockPos.STREAM_CODEC.encode(buf, p.a);
                BlockPos.STREAM_CODEC.encode(buf, p.b);
                ByteBufCodecs.VAR_INT.encode(buf, p.value);
            },
            buf -> new BuildActionPayload(buf.readByte(), BlockPos.STREAM_CODEC.decode(buf), BlockPos.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));

    /** An action that needs no box. */
    public BuildActionPayload(int action, int value) {
        this(action, BlockPos.ZERO, BlockPos.ZERO, value);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(BuildActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BuildMagic.action(player, payload.action(), payload.a(), payload.b(), payload.value());
        }
    }
}
