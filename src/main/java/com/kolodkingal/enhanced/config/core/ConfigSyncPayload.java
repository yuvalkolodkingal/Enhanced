package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, while the client joins (port P0, lane L7): a server config file of the mod, as it is on the
 * server's disk. The client uses its values while connected, as it did with NeoForge's own config sync until 3.8; a
 * client playing its own world ignores it (it shares the server's values).
 */
public record ConfigSyncPayload(String fileName, String contents) implements CustomPacketPayload {
    /** The longest file sent (the server file is about 210 KB). */
    public static final int MAX_LENGTH = 4 * 1024 * 1024;
    public static final Type<ConfigSyncPayload> TYPE = new Type<>(Enhanced.id("config_file"));
    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), ConfigSyncPayload::fileName,
            ByteBufCodecs.stringUtf8(MAX_LENGTH), ConfigSyncPayload::contents,
            ConfigSyncPayload::new);

    @Override
    public Type<ConfigSyncPayload> type() {
        return TYPE;
    }
}
