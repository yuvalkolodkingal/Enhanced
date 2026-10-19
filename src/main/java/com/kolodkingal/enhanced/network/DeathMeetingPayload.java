package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.death.DeathMeetingClient;
import com.kolodkingal.enhanced.death.DeathMeeting;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The meeting with Death (3.7, lane L8): the server offers it to a player who dies ({@link Offer}, sent before the
 * vanilla death packet, so his client shows Rio's screen instead of the plain one), and the player answers "Start
 * again in the Nether" ({@link Choose}; "Start again" is vanilla's own respawn). The server checks the answer again
 * ({@link DeathMeeting#choose}): only a dead player is heard.
 */
public final class DeathMeetingPayload {
    private DeathMeetingPayload() {
    }

    /**
     * Server → client: Death waits for this player; {@code nether}: whether she offers the road to the Nether, and the
     * seconds of Fire Resistance it gives (for the button's tooltip). No offer (Death switched off, a hardcore world,
     * {@code doImmediateRespawn}): the vanilla screen.
     */
    public record Offer(boolean nether, int fireSeconds) implements CustomPacketPayload {
        public static final Type<Offer> TYPE = new Type<>(Enhanced.id("death_meeting_offer"));

        public static final StreamCodec<ByteBuf, Offer> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Offer::nether,
                ByteBufCodecs.VAR_INT, Offer::fireSeconds,
                Offer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Offer payload, IPayloadContext context) {
            DeathMeetingClient.receive(payload);
        }
    }

    /**
     * Client → server: the dead player's choice, a constant of {@link DeathMeeting} ({@code AGAIN}, {@code NETHER}).
     */
    public record Choose(int choice) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(Enhanced.id("death_meeting_choose"));

        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Choose::choice,
                Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Choose payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                DeathMeeting.choose(player, payload.choice());
            }
        }
    }
}
