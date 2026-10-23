package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server: an ability key went down or came up, with the client's Sneak at that moment (3.5: the server's crouch
 * lags behind a quick Sneak + key).
 */
public record AbilityKeyPayload(int ability, boolean down, boolean sneak) implements CustomPacketPayload {
    public static final Type<AbilityKeyPayload> TYPE = new Type<>(Enhanced.id("ability_key"));

    public static final StreamCodec<ByteBuf, AbilityKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityKeyPayload::ability,
            ByteBufCodecs.BOOL, AbilityKeyPayload::down,
            ByteBufCodecs.BOOL, AbilityKeyPayload::sneak,
            AbilityKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(AbilityKeyPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            AbilityHandler.onKey(player, payload.ability(), payload.down(), payload.sneak());
        }
    }
}
