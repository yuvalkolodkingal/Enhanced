package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.power.Powers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server (3.17): the player's own choice to wear his own Minecraft skin instead of his hero's suit, or the suit again
 * (the Hero Creator's Look tab). Any hero, built-in or custom; it changes only how he is drawn
 * ({@link Powers#setOwnSkin}), and the server syncs it to everyone who sees him.
 */
public record OwnSkinPayload(boolean own) implements CustomPacketPayload {
    public static final Type<OwnSkinPayload> TYPE = new Type<>(Enhanced.id("own_skin"));

    public static final StreamCodec<ByteBuf, OwnSkinPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OwnSkinPayload::own,
            OwnSkinPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(OwnSkinPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive() && Powers.power(player).hasPowers()) {
            Powers.setOwnSkin(player, payload.own());
            player.displayClientMessage(Component.translatable(payload.own() ? "message.enhanced.own_skin.on"
                    : "message.enhanced.own_skin.off"), true);
        }
    }
}
