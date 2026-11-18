package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client (3.17): one server setting changed while the client is connected ({@link ConfigEdits}); the client
 * uses the new value at once, as it uses the whole file it was sent on joining ({@link ConfigSyncPayload}). With an
 * empty key it asks the client to open the settings screen ({@code /enhanced config}).
 *
 * @param key   the setting ({@code custom_hero.budget}), or "" to open the settings screen
 * @param value its value as text ({@link ConfigEdits#text})
 */
public record ConfigValuePayload(String key, String value) implements CustomPacketPayload {
    public static final Type<ConfigValuePayload> TYPE = new Type<>(Enhanced.id("config_value"));
    public static final StreamCodec<ByteBuf, ConfigValuePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), ConfigValuePayload::key,
            ByteBufCodecs.stringUtf8(32767), ConfigValuePayload::value,
            ConfigValuePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the screen is only ever loaded there. */
    public static void handle(ConfigValuePayload payload, PacketContext context) {
        com.kolodkingal.enhanced.config.screen.ConfigClient.receive(payload);
    }
}
