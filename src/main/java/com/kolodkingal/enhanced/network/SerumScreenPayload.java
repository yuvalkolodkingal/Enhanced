package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.serum.SerumScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client (3.8): the player used a Super Soldier Serum; open its choice screen (lane L1's). The server checks
 * the answer ({@link SerumChoicePayload}) again: {@code falconUnlocked} only draws the Falcon card.
 *
 * @param hand           the hand that holds the serum
 * @param falconUnlocked whether he carries the Falcon Exo-Wings (the Falcon card is open)
 */
public record SerumScreenPayload(InteractionHand hand, boolean falconUnlocked) implements CustomPacketPayload {
    public static final Type<SerumScreenPayload> TYPE = new Type<>(Enhanced.id("serum_screen"));

    private static final StreamCodec<ByteBuf, InteractionHand> HAND = ByteBufCodecs.BOOL.map(
            off -> off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                    hand -> hand == InteractionHand.OFF_HAND);

    public static final StreamCodec<ByteBuf, SerumScreenPayload> STREAM_CODEC = StreamCodec.composite(
            HAND, SerumScreenPayload::hand,
            ByteBufCodecs.BOOL, SerumScreenPayload::falconUnlocked,
            SerumScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the screen class is only ever loaded there. */
    static void handle(SerumScreenPayload payload, IPayloadContext context) {
        SerumScreen.open(payload);
    }
}
