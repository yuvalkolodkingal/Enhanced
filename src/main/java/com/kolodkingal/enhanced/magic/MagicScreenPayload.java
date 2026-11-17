package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.magic.MagicScreens;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server → client (3.17): open one of the screens of 3.17's magic. The server checks every answer again
 * ({@link MagicActionPayload}).
 *
 * @param screen {@link #CLAIRVOYANCE}, {@link #GRANT} or {@link #ROAD_PRIZE}
 * @param names  the clairvoyance's players; the Demiurge's players; the Road's prizes (ids)
 * @param extra  the Demiurge's players' magic ({@code none}, {@code hero:<power>}, {@code <path>:<level>[:granted]});
 *               the Road's choices ({@code find:<name>}, {@code revive:<name>}, {@code path:<id>})
 */
public record MagicScreenPayload(int screen, List<String> names, List<String> extra, int flags) implements CustomPacketPayload {
    public static final int CLAIRVOYANCE = 0;
    public static final int GRANT = 1;
    public static final int ROAD_PRIZE = 2;

    public static final Type<MagicScreenPayload> TYPE = new Type<>(Enhanced.id("magic_screen"));
    public static final StreamCodec<ByteBuf, MagicScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MagicScreenPayload::screen,
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(256)), MagicScreenPayload::names,
            ByteBufCodecs.stringUtf8(96).apply(ByteBufCodecs.list(512)), MagicScreenPayload::extra,
            ByteBufCodecs.VAR_INT, MagicScreenPayload::flags,
            MagicScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the screens are only ever loaded there. */
    public static void handle(MagicScreenPayload payload, PacketContext context) {
        MagicScreens.open(payload);
    }
}
