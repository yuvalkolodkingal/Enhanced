package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SpiderServer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server (3.3): what the local Spider-Man's Web Assist did. His own client moves him and chooses the anchor
 * ({@code SwingAssist}); the server checks it like a swing it aimed itself and confirms it with the same
 * {@link SpiderFxPayload} as a manual swing, or refuses it (see {@link SpiderServer#onAssist}).
 *
 * <ul>
 *   <li>{@link #ATTACH}: a swing on {@code anchor}, on the face {@code face} (3D data value) of a block, or a point of the
 *       open sky when {@code sky}, with a rope of {@code rope}.</li>
 *   <li>{@link #RELEASE}: the assist let go of the swing at its best moment (G is still held).</li>
 *   <li>{@link #BOOST}: the open-land fallback "zip": a short web boost towards {@code anchor}.</li>
 * </ul>
 */
public record SpiderSwingPayload(int action, Vec3 anchor, int face, float rope, boolean sky) implements CustomPacketPayload {
    public static final int ATTACH = 0;
    public static final int RELEASE = 1;
    public static final int BOOST = 2;

    public static final Type<SpiderSwingPayload> TYPE = new Type<>(Enhanced.id("spider_swing"));

    public static final StreamCodec<ByteBuf, SpiderSwingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpiderSwingPayload::action,
            PayloadCodecs.VEC3, SpiderSwingPayload::anchor,
            ByteBufCodecs.VAR_INT, SpiderSwingPayload::face,
            ByteBufCodecs.FLOAT, SpiderSwingPayload::rope,
            ByteBufCodecs.BOOL, SpiderSwingPayload::sky,
            SpiderSwingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SpiderSwingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SpiderServer.onAssist(player, payload.action(), payload.anchor(), payload.face(), payload.rope(), payload.sky());
        }
    }
}
