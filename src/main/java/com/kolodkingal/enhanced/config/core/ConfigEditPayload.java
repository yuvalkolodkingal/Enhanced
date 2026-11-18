package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server (3.17): an operator changed a server setting on his settings screen while on a server that is not
 * his own. The client is never trusted: the server checks that the sender is an operator and checks the value by the
 * config's own rules ({@link ConfigEdits#fromClient}).
 *
 * @param key   the setting ({@code custom_hero.budget})
 * @param value its new value as text ({@link ConfigEdits#text}); ignored for a reset
 * @param reset back to its default
 */
public record ConfigEditPayload(String key, String value, boolean reset) implements CustomPacketPayload {
    public static final Type<ConfigEditPayload> TYPE = new Type<>(Enhanced.id("config_edit"));
    public static final StreamCodec<ByteBuf, ConfigEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), ConfigEditPayload::key,
            ByteBufCodecs.stringUtf8(32767), ConfigEditPayload::value,
            ByteBufCodecs.BOOL, ConfigEditPayload::reset,
            ConfigEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigEditPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ConfigEdits.fromClient(player, payload.key(), payload.value(), payload.reset());
        }
    }
}
