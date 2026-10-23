package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.wolverine.WolverineFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → client (3.7): a moment of Wolverine everyone near should see (lane L2's): the claws out ("snikt") and in
 * ("shink"), a slash, the spin, the pounce and its hit, the Rage beginning and ending, the sniff, Track on and off, a
 * claw climb's marks. The client hands each kind to the drawer lane L2 registered ({@code WolverineFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId Wolverine, or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record WolverineFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (the payload stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int SNIKT = 0;
    public static final int SHINK = 1;
    public static final int SLASH = 2;
    public static final int SPIN = 3;
    public static final int POUNCE = 4;
    public static final int POUNCE_HIT = 5;
    public static final int BERSERK_ON = 6;
    public static final int BERSERK_OFF = 7;
    public static final int SNIFF = 8;
    public static final int TRACK_ON = 9;
    public static final int TRACK_OFF = 10;
    public static final int CLIMB = 11;

    public static final Type<WolverineFxPayload> TYPE = new Type<>(Enhanced.id("wolverine_fx"));

    public static final StreamCodec<ByteBuf, WolverineFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WolverineFxPayload::kind,
            ByteBufCodecs.VAR_INT, WolverineFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), WolverineFxPayload::points,
            ByteBufCodecs.VAR_INT, WolverineFxPayload::arg,
            WolverineFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(WolverineFxPayload payload, PacketContext context) {
        WolverineFx.receive(payload);
    }
}
