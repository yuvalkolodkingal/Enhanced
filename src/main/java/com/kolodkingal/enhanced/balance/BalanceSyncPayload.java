package com.kolodkingal.enhanced.balance;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;

/**
 * Server → client (3.16): the server's SMP balance, sent to a player who logs in and to everyone when it changes, so the
 * client reads the power numbers as the server does.
 */
public record BalanceSyncPayload(String preset, Map<String, Double> overrides) implements CustomPacketPayload {
    public static final Type<BalanceSyncPayload> TYPE = new Type<>(Enhanced.id("balance_sync"));
    public static final StreamCodec<ByteBuf, BalanceSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), BalanceSyncPayload::preset,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(128), ByteBufCodecs.DOUBLE, 1024),
            BalanceSyncPayload::overrides,
            BalanceSyncPayload::new);

    /** The balance as it is now. */
    public static BalanceSyncPayload now() {
        return new BalanceSyncPayload(Balance.preset().id(), Balance.overrides());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BalanceSyncPayload payload, PacketContext context) {
        Balance.Preset p = Balance.Preset.byId(payload.preset());
        Balance.apply(p == null ? Balance.Preset.FULL : p, payload.overrides());
    }
}
