package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.healing.HealingFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client (3.7): a moment of a healer's healing factor everyone near should see (lane L1's): he goes DOWN,
 * rises, bursts, is rescued from a hazard, his healing goes OFF (poisoned) or back ON, a blow is CUT short of killing
 * him (the ring the attacker sees). The client hands each kind to the drawer lane L1 registered ({@code
 * HealingFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId the healer
 * @param arg      a number (the meaning depends on the kind)
 */
public record HealingFxPayload(int kind, int entityId, int arg) implements CustomPacketPayload {
    public static final int DOWN = 0;
    public static final int RISE = 1;
    public static final int BURST = 2;
    public static final int RESCUE = 3;
    public static final int OFF = 4;
    public static final int ON = 5;
    public static final int CUT = 6;

    public static final Type<HealingFxPayload> TYPE = new Type<>(Enhanced.id("healing_fx"));

    public static final StreamCodec<ByteBuf, HealingFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HealingFxPayload::kind,
            ByteBufCodecs.VAR_INT, HealingFxPayload::entityId,
            ByteBufCodecs.VAR_INT, HealingFxPayload::arg,
            HealingFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HealingFxPayload payload, PacketContext context) {
        HealingFx.receive(payload);
    }
}
