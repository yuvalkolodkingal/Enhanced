package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server (3.17): an answer of 3.17's magic screens: the Spell Wheel's choice, the name the clairvoyance seeks,
 * the Demiurge's gift or taking back, the prize of the Coven's Road. Each is checked again on the server.
 */
public record MagicActionPayload(int action, String a, String b, int n) implements CustomPacketPayload {
    public static final int SELECT_SPELL = 0;
    public static final int CLAIRVOYANCE = 1;
    public static final int GRANT = 2;
    public static final int REVOKE = 3;
    public static final int ROAD_PRIZE = 4;

    public static final Type<MagicActionPayload> TYPE = new Type<>(Enhanced.id("magic_action"));
    public static final StreamCodec<ByteBuf, MagicActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MagicActionPayload::action,
            ByteBufCodecs.stringUtf8(96), MagicActionPayload::a,
            ByteBufCodecs.stringUtf8(96), MagicActionPayload::b,
            ByteBufCodecs.VAR_INT, MagicActionPayload::n,
            MagicActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MagicActionPayload payload, PacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case SELECT_SPELL -> Casting.select(player, payload.a());
            case CLAIRVOYANCE -> {
                if (Magic.knows(player, Spell.CLAIRVOYANCE)) {
                    Clairvoyance.seek(player, payload.a());
                }
            }
            case GRANT -> Grants.fromScreen(player, payload.a(), payload.b(), payload.n(), false);
            case REVOKE -> Grants.fromScreen(player, payload.a(), "", 0, true);
            case ROAD_PRIZE -> CovenRoad.choose(player, payload.a(), payload.b());
            default -> {
            }
        }
    }
}
