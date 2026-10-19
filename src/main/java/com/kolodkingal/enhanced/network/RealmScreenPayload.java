package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.screen.RealmScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.6): open the Space Stone's Realm Picker with the realms the server allows now (lane L2). Each
 * button answers with {@code StoneActionPayload(REALM, index)}; the server checks again.
 *
 * @param realms the realms in order (the Overworld, the Nether, the End, Asgard), at most {@link #MAX_REALMS}
 */
public record RealmScreenPayload(List<RealmOption> realms) implements CustomPacketPayload {
    public static final int MAX_REALMS = 8;

    /**
     * One realm's button.
     *
     * @param id      the realm ({@code overworld}, {@code the_nether}, {@code the_end}, {@code asgard})
     * @param allowed whether it can be chosen now
     * @param reason  the lang key of why not (empty when allowed)
     */
    public record RealmOption(String id, boolean allowed, String reason) {
        public static final StreamCodec<ByteBuf, RealmOption> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), RealmOption::id,
                ByteBufCodecs.BOOL, RealmOption::allowed,
                ByteBufCodecs.stringUtf8(128), RealmOption::reason,
                RealmOption::new);
    }

    public static final Type<RealmScreenPayload> TYPE = new Type<>(Enhanced.id("realm_screen"));

    public static final StreamCodec<ByteBuf, RealmScreenPayload> STREAM_CODEC = StreamCodec.composite(
            RealmOption.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_REALMS)), RealmScreenPayload::realms,
            RealmScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(RealmScreenPayload payload, IPayloadContext context) {
        RealmScreen.open(payload);
    }
}
