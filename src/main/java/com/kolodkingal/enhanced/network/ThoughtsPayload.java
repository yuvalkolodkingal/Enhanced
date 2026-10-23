package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.ThoughtBubbles;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server → client, to the one who hears thoughts (3.3: Billy with The Noise on, Professor X with Hear Thoughts on), every
 * two seconds: what the players and creatures around him are thinking, drawn over their heads (see {@code Thoughts}).
 * At most {@link #MAX} of them, the nearest.
 */
public record ThoughtsPayload(List<Thought> thoughts) implements CustomPacketPayload {
    /** The most thoughts in one message. */
    public static final int MAX = 16;

    /** Calm, means harm, afraid, a mind closed to the listener. */
    public static final int CALM = 0;
    public static final int HOSTILE = 1;
    public static final int AFRAID = 2;
    public static final int CLOSED = 3;

    /** One creature's thought: who, the words, the mood. */
    public record Thought(int entityId, Component text, int mood) {
        static final StreamCodec<RegistryFriendlyByteBuf, Thought> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Thought::entityId,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Thought::text,
                ByteBufCodecs.VAR_INT, Thought::mood,
                Thought::new);
    }

    public static final Type<ThoughtsPayload> TYPE = new Type<>(Enhanced.id("thoughts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThoughtsPayload> STREAM_CODEC = StreamCodec.composite(
            Thought.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), ThoughtsPayload::thoughts,
            ThoughtsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client. */
    static void handle(ThoughtsPayload payload, PacketContext context) {
        ThoughtBubbles.accept(payload.thoughts());
    }
}
