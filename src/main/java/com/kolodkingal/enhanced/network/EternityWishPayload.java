package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server (3.8): the wish screen's answer (lane L8's): an item ({@link #ITEM}, {@code item} named), to become
 * Love ({@link #LOVE}), or to go home ({@link #LEAVE}). The server goes by where the player is and checks every rule
 * again ({@link EternityRealm#wish}); a forged answer is refused.
 *
 * @param kind {@link #ITEM}, {@link #LOVE} or {@link #LEAVE}
 * @param item the wished item (ignored but for {@link #ITEM})
 */
public record EternityWishPayload(byte kind, ResourceLocation item) implements CustomPacketPayload {
    public static final byte ITEM = 0;
    public static final byte LOVE = 1;
    public static final byte LEAVE = 2;

    public static final Type<EternityWishPayload> TYPE = new Type<>(Enhanced.id("eternity_wish"));

    public static final StreamCodec<ByteBuf, EternityWishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, EternityWishPayload::kind,
            ResourceLocation.STREAM_CODEC, EternityWishPayload::item,
            EternityWishPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(EternityWishPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            EternityRealm.wish(player, payload.kind(), payload.item());
        }
    }
}
