package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import com.kolodkingal.enhanced.stones.StoneWheel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server (3.6): the Infinity Stones' own input. {@link #SELECT}: the Stone Wheel picked the stone of ordinal
 * {@code arg} in the Gauntlet in the player's hand (-1: the next one, a tap of J; lane L1); {@link #REALM}: the Realm
 * Picker's button {@code arg} (lane L2); {@link #WHEEL_OPEN}: the player holds J (lane L1).
 */
public record StoneActionPayload(int action, int arg) implements CustomPacketPayload {
    public static final int SELECT = 0;
    public static final int REALM = 1;
    public static final int WHEEL_OPEN = 2;

    public static final Type<StoneActionPayload> TYPE = new Type<>(Enhanced.id("stone_action"));

    public static final StreamCodec<ByteBuf, StoneActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoneActionPayload::action,
            ByteBufCodecs.VAR_INT, StoneActionPayload::arg,
            StoneActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(StoneActionPayload payload, PacketContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        switch (payload.action()) {
            case SELECT -> StoneWheel.select(player, payload.arg());
            case REALM -> SpaceRealms.choose(player, payload.arg());
            case WHEEL_OPEN -> StoneWheel.open(player);
            default -> {
            }
        }
    }
}
