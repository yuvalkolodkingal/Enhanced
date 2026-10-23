package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.serum.SerumServer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server (3.8): the serum screen's choice (lane L1's): Captain America, Red Hulk or Falcon. The server checks
 * everything again ({@link SerumServer#choose}: a pending serum, the same hand, the wings for Falcon); an ordinal this
 * version does not know reads as {@link Power#NONE}, which is refused.
 *
 * @param choice the hero chosen
 */
public record SerumChoicePayload(Power choice) implements CustomPacketPayload {
    public static final Type<SerumChoicePayload> TYPE = new Type<>(Enhanced.id("serum_choice"));

    private static final StreamCodec<ByteBuf, Power> POWER = ByteBufCodecs.VAR_INT.map(
            i -> i >= 0 && i < Power.values().length ? Power.values()[i] : Power.NONE, Power::ordinal);

    public static final StreamCodec<ByteBuf, SerumChoicePayload> STREAM_CODEC = StreamCodec.composite(
            POWER, SerumChoicePayload::choice,
            SerumChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SerumChoicePayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SerumServer.choose(player, payload.choice());
        }
    }
}
