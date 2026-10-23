package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.client.screen.AsgardTalkScreen;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * A talk with an Asgardian (3.5): the server opens the talk screen ({@link Open}), the player chooses an answer
 * ({@link Choose}). Written by the 3.5 skeleton; the client screen and the server's rules are lane L5's.
 */
public final class AsgardTalkPayload {
    /** The longest node, option or argument key sent. */
    public static final int MAX_KEY = 128;
    /** The most answers a node offers. */
    public static final int MAX_OPTIONS = 3;
    /** The most arguments a line takes. */
    public static final int MAX_ARGS = 4;

    /**
     * Server → client: open the talk screen.
     *
     * @param entityId the Asgardian
     * @param role     his {@code Role} (its ordinal)
     * @param node     the lang key of the line ({@code npc.enhanced.<role>.<node>[.<n>]})
     * @param args     the line's arguments (plain text)
     * @param options  the option keys offered ({@code npc.enhanced.option.<key>}), at most {@link #MAX_OPTIONS}
     */
    public record Open(int entityId, int role, String node, List<String> args, List<String> options) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Enhanced.id("asgard_talk_open"));

        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::entityId,
                ByteBufCodecs.VAR_INT, Open::role,
                ByteBufCodecs.stringUtf8(MAX_KEY), Open::node,
                ByteBufCodecs.stringUtf8(MAX_KEY).apply(ByteBufCodecs.list(MAX_ARGS)), Open::args,
                ByteBufCodecs.stringUtf8(MAX_KEY).apply(ByteBufCodecs.list(MAX_OPTIONS)), Open::options,
                Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** The talk begins: the client opens the screen (one call into a client class, loaded only on a client). */
        static void handle(Open payload, PacketContext context) {
            AsgardTalkScreen.open(payload);
        }
    }

    /**
     * Client → server: the player chose an answer.
     *
     * @param entityId the Asgardian
     * @param option   the option's key
     */
    public record Choose(int entityId, String option) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(Enhanced.id("asgard_talk_choose"));

        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Choose::entityId,
                ByteBufCodecs.stringUtf8(MAX_KEY), Choose::option,
                Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Choose payload, PacketContext context) {
            if (context.player() instanceof ServerPlayer player && player.isAlive()) {
                AsgardTalk.choose(player, payload.entityId(), payload.option());
            }
        }
    }

    private AsgardTalkPayload() {
    }
}
