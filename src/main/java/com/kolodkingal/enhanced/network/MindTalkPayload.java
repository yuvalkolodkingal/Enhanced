package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.screen.MindTalkScreen;
import com.kolodkingal.enhanced.mind.MindTalk;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** A talk in Vision's mind (3.12): the server shows a line ({@link Open}), the player answers ({@link Choose}). */
public final class MindTalkPayload {
    public static final int MAX_KEY = 96;
    public static final int MAX_OPTIONS = 8;

    /**
     * Server → client: show a line, or close the screen (an empty {@code line}).
     *
     * @param npcId   who speaks (-1: nobody in the world)
     * @param speaker the speaker's id ({@code mind.enhanced.speaker.<id>})
     * @param line    {@code <scene>.<line>} ({@code mind.enhanced.<scene>.<line>})
     * @param options the answers offered
     * @param forced  the talk cannot be left
     */
    public record Open(int npcId, String speaker, String line, List<String> options, boolean forced) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Enhanced.id("mind_talk_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId,
                ByteBufCodecs.stringUtf8(MAX_KEY), Open::speaker,
                ByteBufCodecs.stringUtf8(MAX_KEY), Open::line,
                ByteBufCodecs.stringUtf8(MAX_KEY).apply(ByteBufCodecs.list(MAX_OPTIONS)), Open::options,
                ByteBufCodecs.BOOL, Open::forced,
                Open::new);

        public static Open close() {
            return new Open(-1, "", "", List.of(), false);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Open payload, PacketContext context) {
            MindTalkScreen.open(payload);
        }
    }

    /** Client → server: the answer chosen ({@code ""}: the screen was closed). */
    public record Choose(String option) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(Enhanced.id("mind_talk_choose"));
        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MAX_KEY), Choose::option, Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Choose payload, PacketContext context) {
            if (context.player() instanceof ServerPlayer player && player.isAlive()) {
                MindTalk.choose(player, payload.option());
            }
        }
    }

    private MindTalkPayload() {
    }
}
