package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/** Client → server: Wanda changed something on the Hex Control screen. */
public record HexActionPayload(HexAction action, Optional<UUID> target, int value) implements CustomPacketPayload {
    public static final Type<HexActionPayload> TYPE = new Type<>(Enhanced.id("hex_action"));

    private static final StreamCodec<ByteBuf, HexAction> ACTION_CODEC =
            ByteBufCodecs.idMapper(i -> HexAction.values()[Math.floorMod(i, HexAction.values().length)], HexAction::ordinal);

    public static final StreamCodec<ByteBuf, HexActionPayload> STREAM_CODEC = StreamCodec.composite(
            ACTION_CODEC, HexActionPayload::action,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), HexActionPayload::target,
            ByteBufCodecs.VAR_INT, HexActionPayload::value,
            HexActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HexActionPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HexManager.handleAction(player, payload.action(), payload.target(), payload.value());
        }
    }
}
