package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.RoadMusicPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client (3.3): the stage of the Witches' Road the player is at now, whose music his client plays
 * ({@link com.kolodkingal.enhanced.road.RoadMusic}); empty when he is off the Road.
 */
public record RoadMusicPayload(String stage) implements CustomPacketPayload {
    public static final Type<RoadMusicPayload> TYPE = new Type<>(Enhanced.id("road_music"));

    public static final StreamCodec<ByteBuf, RoadMusicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), RoadMusicPayload::stage,
            RoadMusicPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the player class is only ever loaded there. */
    static void handle(RoadMusicPayload payload, IPayloadContext context) {
        RoadMusicPlayer.setStage(payload.stage());
    }
}
