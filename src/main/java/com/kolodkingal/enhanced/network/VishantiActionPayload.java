package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: a button on the screen of the Book of the Vishanti. As the Bifrost's, {@code index} and {@code name} name
 * a saved place (the server checks both still match); for {@link #SAVE} and {@link #SAVE_BASE} {@code name} is what the
 * player typed. The server checks everything again (that the book is in hand and the player is a sorcerer).
 */
public record VishantiActionPayload(int action, int index, String name) implements CustomPacketPayload {
    public static final int TRAVEL = 0;
    public static final int SAVE = 1;
    public static final int SAVE_BASE = 2;
    public static final int FORGET = 3;
    public static final int GO_BASE = 4;

    public static final Type<VishantiActionPayload> TYPE = new Type<>(Enhanced.id("vishanti_action"));

    public static final StreamCodec<ByteBuf, VishantiActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VishantiActionPayload::action,
            ByteBufCodecs.VAR_INT, VishantiActionPayload::index,
            ByteBufCodecs.stringUtf8(BifrostData.MAX_NAME_LENGTH * 4), VishantiActionPayload::name,
            VishantiActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(VishantiActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        switch (payload.action()) {
            case TRAVEL -> VishantiBook.travelToPlace(player, payload.index(), payload.name());
            case SAVE -> VishantiBook.savePlace(player, payload.name(), false);
            case SAVE_BASE -> VishantiBook.savePlace(player, payload.name(), true);
            case FORGET -> VishantiBook.forgetPlace(player, payload.index(), payload.name());
            case GO_BASE -> VishantiBook.travelToBase(player);
            default -> {
            }
        }
    }
}
