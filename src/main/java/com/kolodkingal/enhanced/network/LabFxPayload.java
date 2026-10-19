package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.school.LabFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client (3.7): a moment of the X-Men lower lab (lane L4's; the pod's lane L3's, the console's lane L5's): the
 * lift's fade down or up (to its rider), a secret bookshelf swinging open or shut, the Weapon X Pod starting and done,
 * an injection taken from the rack, the Cerebro console on and off; and (lane L4) whether the player is inside a lab
 * (its ambience). The client hands each kind to the drawer its lane registered ({@code LabFx.register}).
 *
 * @param kind what happened (a constant of this record)
 * @param pos  the block it happened at
 */
public record LabFxPayload(int kind, BlockPos pos) implements CustomPacketPayload {
    public static final int LIFT_DOWN = 0;
    public static final int LIFT_UP = 1;
    public static final int SHELF_OPEN = 2;
    public static final int SHELF_CLOSE = 3;
    public static final int POD_START = 4;
    public static final int POD_DONE = 5;
    public static final int RACK_TAKE = 6;
    public static final int CONSOLE_ON = 7;
    public static final int CONSOLE_OFF = 8;
    /** (Lane L4) The lab's ambience for the player told: {@code pos} (0, 1, 0) he is inside a lab, (0, 0, 0) not. */
    public static final int AMBIENCE = 9;

    public static final Type<LabFxPayload> TYPE = new Type<>(Enhanced.id("lab_fx"));

    public static final StreamCodec<ByteBuf, LabFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LabFxPayload::kind,
            PayloadCodecs.BLOCK_POS, LabFxPayload::pos,
            LabFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(LabFxPayload payload, IPayloadContext context) {
        LabFx.receive(payload);
    }
}
