package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.strange.SlingRingActions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server (3.6): a button on the Sling Ring's screen (the PlaceBook kind {@code SLING_RING}, lane L3), as the
 * Bifrost's: {@link #TRAVEL} opens a portal pair to the place {@code index} if it is still called {@code name}, {@link
 * #SAVE} saves where he stands as {@code name}, {@link #FORGET} forgets the place.
 */
public record SlingRingActionPayload(int action, int index, String name) implements CustomPacketPayload {
    public static final int TRAVEL = 1;
    public static final int SAVE = 2;
    public static final int FORGET = 3;

    public static final Type<SlingRingActionPayload> TYPE = new Type<>(Enhanced.id("sling_ring_action"));

    public static final StreamCodec<ByteBuf, SlingRingActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlingRingActionPayload::action,
            ByteBufCodecs.VAR_INT, SlingRingActionPayload::index,
            ByteBufCodecs.stringUtf8(BifrostData.MAX_NAME_LENGTH * 4), SlingRingActionPayload::name,
            SlingRingActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SlingRingActionPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive()) {
            SlingRingActions.handle(player, payload.action(), payload.index(), payload.name());
        }
    }
}
