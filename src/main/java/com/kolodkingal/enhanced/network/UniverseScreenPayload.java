package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.screen.UniverseScreen;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server → client (3.19): open the Multiverse screen with the universes it offers ({@code UniverseTravel}). Each window
 * answers with {@link UniverseChoicePayload}; the server checks again.
 *
 * @param mode      how the player travels ({@code UniverseTravel.DREAM}, {@code RING}, {@code BRIDGE}): the screen's title
 * @param universes the universes in order, at most {@link #MAX}
 */
public record UniverseScreenPayload(int mode, List<UniverseOption> universes) implements CustomPacketPayload {
    public static final int MAX = 8;

    /**
     * One universe's window.
     *
     * @param universe its number (616, 10005, 2149 or a What If universe)
     * @param allowed  whether it can be chosen now
     * @param reason   the lang key of why not (empty when allowed)
     */
    public record UniverseOption(int universe, boolean allowed, String reason) {
        public static final StreamCodec<ByteBuf, UniverseOption> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UniverseOption::universe,
                ByteBufCodecs.BOOL, UniverseOption::allowed,
                ByteBufCodecs.stringUtf8(128), UniverseOption::reason,
                UniverseOption::new);
    }

    public static final Type<UniverseScreenPayload> TYPE = new Type<>(Enhanced.id("universe_screen"));

    public static final StreamCodec<ByteBuf, UniverseScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UniverseScreenPayload::mode,
            UniverseOption.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), UniverseScreenPayload::universes,
            UniverseScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(UniverseScreenPayload payload, PacketContext context) {
        UniverseScreen.open(payload);
    }
}
