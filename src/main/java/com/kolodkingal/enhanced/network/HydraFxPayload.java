package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.hydra.HydraFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.6): a moment of the Hydra soldiers everyone near should see (lane L8's): a muzzle flash, a bolt's
 * hit, the sniper's aim, the alarm, a soldier turned. The client hands each kind to the drawer lane L8 registered
 * ({@code HydraFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId the soldier, or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record HydraFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (the payload stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int MUZZLE = 0;
    public static final int BOLT_HIT = 1;
    public static final int SNIPER_AIM = 2;
    public static final int ALARM_ON = 3;
    public static final int ALARM_OFF = 4;
    public static final int TURNED = 5;

    public static final Type<HydraFxPayload> TYPE = new Type<>(Enhanced.id("hydra_fx"));

    public static final StreamCodec<ByteBuf, HydraFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HydraFxPayload::kind,
            ByteBufCodecs.VAR_INT, HydraFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), HydraFxPayload::points,
            ByteBufCodecs.VAR_INT, HydraFxPayload::arg,
            HydraFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HydraFxPayload payload, IPayloadContext context) {
        HydraFx.receive(payload);
    }
}
