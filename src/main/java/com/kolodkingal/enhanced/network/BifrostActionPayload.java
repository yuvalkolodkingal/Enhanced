package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.power.BifrostData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server: a button on the Bifrost screen. {@code index} and {@code name} name a saved place (the server checks
 * both still match, in case the list changed meanwhile); for {@link #SAVE}, {@code name} is what the player typed. 3.5:
 * {@link #ASGARD} (the Bifrost to Asgard) and {@link #SEND} (Heimdall sends the player whose entity id is {@code index} to
 * {@code name}: {@code "home"}, {@code "asgard"} or one of Heimdall's places).
 */
public record BifrostActionPayload(int action, int index, String name) implements CustomPacketPayload {
    public static final int HOME = 0;
    public static final int TRAVEL = 1;
    public static final int SAVE = 2;
    public static final int FORGET = 3;
    public static final int ASGARD = 4;
    public static final int SEND = 5;

    public static final Type<BifrostActionPayload> TYPE = new Type<>(Enhanced.id("bifrost_action"));

    public static final StreamCodec<ByteBuf, BifrostActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BifrostActionPayload::action,
            ByteBufCodecs.VAR_INT, BifrostActionPayload::index,
            ByteBufCodecs.stringUtf8(BifrostData.MAX_NAME_LENGTH * 4), BifrostActionPayload::name,
            BifrostActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(BifrostActionPayload payload, PacketContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        switch (payload.action()) {
            case HOME -> Bifrost.home(player);
            case TRAVEL -> Bifrost.toPlace(player, payload.index(), payload.name());
            case SAVE -> Bifrost.savePlace(player, payload.name());
            case FORGET -> Bifrost.forgetPlace(player, payload.index(), payload.name());
            case ASGARD -> Bifrost.toAsgard(player);
            case SEND -> Bifrost.send(player, payload.index(), payload.name());
            default -> {
            }
        }
    }
}
