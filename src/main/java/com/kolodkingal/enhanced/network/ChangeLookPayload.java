package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Powers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: the Change Look key was pressed (step +1 for the next look, -1 for the previous one). */
public record ChangeLookPayload(int step) implements CustomPacketPayload {
    public static final Type<ChangeLookPayload> TYPE = new Type<>(Enhanced.id("change_look"));

    public static final StreamCodec<ByteBuf, ChangeLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChangeLookPayload::step,
            ChangeLookPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ChangeLookPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
            if (!Powers.cycleVariant(player, payload.step() < 0 ? -1 : 1) && Powers.power(player).hasPowers()) {
                player.displayClientMessage(Component.translatable("message.enhanced.look.none"), true);
            }
        }
    }
}
