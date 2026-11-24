package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.multiverse.UniverseTravel;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Client → server (3.19): the Multiverse screen's window {@code index} was chosen; the server checks again. */
public record UniverseChoicePayload(int index) implements CustomPacketPayload {
    public static final Type<UniverseChoicePayload> TYPE = new Type<>(Enhanced.id("universe_choice"));

    public static final StreamCodec<ByteBuf, UniverseChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UniverseChoicePayload::index,
            UniverseChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(UniverseChoicePayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive()) {
            UniverseTravel.choose(player, payload.index());
        }
    }
}
