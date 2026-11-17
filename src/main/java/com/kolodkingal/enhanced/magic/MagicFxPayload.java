package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.magic.MagicFxRenderer;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client (3.17): a spell's or an ultimate move's light, which every client near draws for {@code duration}
 * ticks ({@link MagicFx} names the kinds; {@code client.magic.MagicFxRenderer} draws them). Lasting shapes (a circle, a
 * dome, a portal) carry an {@code id}, so the server can take one down early ({@link MagicFx#CANCEL}).
 *
 * @param kind     what to draw ({@link MagicFx})
 * @param id       the shape's id (0: none), or for {@link MagicFx#CANCEL} the shape to take down
 * @param entity   the entity it follows (-1: none)
 * @param origin   where it is
 * @param points   the places it reaches (targets, a path, a direction)
 * @param color    its colour (RGB)
 * @param color2   its second colour (RGB), or a small number some kinds read
 * @param size     its size (a radius, a length)
 * @param duration how long it lasts (ticks)
 */
public record MagicFxPayload(int kind, int id, int entity, Vec3 origin, List<Vec3> points, int color, int color2,
                             float size, int duration) implements CustomPacketPayload {
    public static final Type<MagicFxPayload> TYPE = new Type<>(Enhanced.id("magic_fx"));
    /** The most points one message carries. */
    public static final int MAX_POINTS = 64;

    public static final StreamCodec<ByteBuf, MagicFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind());
                buf.writeInt(p.id());
                buf.writeInt(p.entity());
                writeVec(buf, p.origin());
                int n = Math.min(MAX_POINTS, p.points().size());
                buf.writeByte(n);
                for (int i = 0; i < n; i++) {
                    writeVec(buf, p.points().get(i));
                }
                buf.writeInt(p.color());
                buf.writeInt(p.color2());
                buf.writeFloat(p.size());
                buf.writeShort(Math.min(Short.MAX_VALUE, Math.max(0, p.duration())));
            },
            buf -> {
                int kind = buf.readByte();
                int id = buf.readInt();
                int entity = buf.readInt();
                Vec3 origin = readVec(buf);
                int n = Math.min(MAX_POINTS, buf.readUnsignedByte());
                List<Vec3> points = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    points.add(readVec(buf));
                }
                return new MagicFxPayload(kind, id, entity, origin, points, buf.readInt(), buf.readInt(), buf.readFloat(),
                        buf.readShort());
            });

    private static void writeVec(ByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(ByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the renderer class is only ever loaded there. */
    public static void handle(MagicFxPayload payload, PacketContext context) {
        MagicFxRenderer.add(payload);
    }
}
