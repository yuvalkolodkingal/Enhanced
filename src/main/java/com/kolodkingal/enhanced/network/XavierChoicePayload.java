package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.XavierAbilities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server (3.3): Professor X chose his version on the screen that opens when he awakens ({@code young} or
 * {@code old}). The server checks that he is Professor X and that the version is one of his.
 */
public record XavierChoicePayload(String version) implements CustomPacketPayload {
    public static final Type<XavierChoicePayload> TYPE = new Type<>(Enhanced.id("xavier_choice"));

    public static final StreamCodec<ByteBuf, XavierChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), XavierChoicePayload::version,
            XavierChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(XavierChoicePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            XavierAbilities.choose(player, payload.version());
        }
    }
}
