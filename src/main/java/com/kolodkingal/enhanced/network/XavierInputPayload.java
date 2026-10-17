package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.MindLink;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server, from Professor X while his mind is in another body (3.3), once a tick: what his keys and his mouse
 * ask the body to do. It is a wish, never a position: the server moves the body by its own rules ({@link MindLink}),
 * and drops the message if he steers nobody.
 *
 * @param forward the forward key (1), the back key (-1), or between
 * @param strafe  the left key (1), the right key (-1), or between
 * @param jump    the jump key is down
 * @param sneak   the sneak key is down (the body walks slowly)
 * @param yaw     where he looks (degrees), which is where the body looks
 * @param pitch   the same, up and down
 * @param attack  the attack button was pressed since the last message
 */
public record XavierInputPayload(float forward, float strafe, boolean jump, boolean sneak, float yaw, float pitch, boolean attack)
        implements CustomPacketPayload {
    public static final Type<XavierInputPayload> TYPE = new Type<>(Enhanced.id("xavier_input"));

    public static final StreamCodec<ByteBuf, XavierInputPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.FLOAT.encode(buf, p.forward);
        ByteBufCodecs.FLOAT.encode(buf, p.strafe);
        ByteBufCodecs.BOOL.encode(buf, p.jump);
        ByteBufCodecs.BOOL.encode(buf, p.sneak);
        ByteBufCodecs.FLOAT.encode(buf, p.yaw);
        ByteBufCodecs.FLOAT.encode(buf, p.pitch);
        ByteBufCodecs.BOOL.encode(buf, p.attack);
    }, buf -> new XavierInputPayload(ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.BOOL.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(XavierInputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            MindLink.input(player, payload);
        }
    }
}
