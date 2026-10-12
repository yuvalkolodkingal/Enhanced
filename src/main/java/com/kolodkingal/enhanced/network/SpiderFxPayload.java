package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.SpiderEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client: a moment of Spider-Man's power everyone nearby should see (a web shot, the burst, a zip or a swing
 * beginning or ending, a leg striking). The meaning of the fields depends on the kind.
 *
 * <ul>
 *   <li>{@link Kind#SHOT}: {@code from} his hand, {@code to} the impact, {@code normal} the face hit (zero for a
 *       creature), {@code arg} the target's entity id or -1.</li>
 *   <li>{@link Kind#BURST}: {@code from} his chest, {@code points} where each strand ended, {@code arg} how many were
 *       blocked by a shield.</li>
 *   <li>{@link Kind#ZIP_START}, {@link Kind#SWING_START}: {@code to} the anchor, {@code normal} the face,
 *       {@code value} the rope length (swing); also sent to the hero himself as the acknowledgement. A zip also carries
 *       the point the server will have him stand at as {@code points[0]} (the client moves him there), and, for a zip
 *       aimed at a creature (it ends in a kick), the creature's entity id as {@code arg} (otherwise -1).</li>
 *   <li>{@link Kind#ZIP_END}, {@link Kind#SWING_END}: {@code arg} the reason (0 arrived, 1 cancelled, 2 refused,
 *       3 blocked, 4 timeout, 5 released, 6 anchor lost).</li>
 *   <li>{@link Kind#LEG_STRIKE}: {@code arg} the leg 0 to 3, {@code to} where the tip goes.</li>
 * </ul>
 *
 * @param player  the hero's entity id
 * @param organic whether his webs are organic (the Black Eyes)
 * @param seed    makes the curve of each strand the same on every client
 * @param points  at most 96
 */
public record SpiderFxPayload(Kind kind, int player, Vec3 from, Vec3 to, Vec3 normal, int arg, float value, boolean organic,
                              long seed, List<Vec3> points) implements CustomPacketPayload {
    public enum Kind {
        SHOT, BURST, ZIP_START, ZIP_END, SWING_START, SWING_END, LEG_STRIKE
    }

    public static final Type<SpiderFxPayload> TYPE = new Type<>(Enhanced.id("spider_fx"));

    public static final StreamCodec<ByteBuf, SpiderFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.kind.ordinal());
                ByteBufCodecs.VAR_INT.encode(buf, p.player);
                PayloadCodecs.VEC3.encode(buf, p.from);
                PayloadCodecs.VEC3.encode(buf, p.to);
                PayloadCodecs.VEC3.encode(buf, p.normal);
                ByteBufCodecs.VAR_INT.encode(buf, p.arg);
                ByteBufCodecs.FLOAT.encode(buf, p.value);
                ByteBufCodecs.BOOL.encode(buf, p.organic);
                ByteBufCodecs.VAR_LONG.encode(buf, p.seed);
                PayloadCodecs.VEC3.apply(ByteBufCodecs.list(96)).encode(buf, p.points);
            },
            buf -> {
                int kind = ByteBufCodecs.VAR_INT.decode(buf);
                Kind[] kinds = Kind.values();
                return new SpiderFxPayload(kinds[Math.floorMod(kind, kinds.length)], ByteBufCodecs.VAR_INT.decode(buf),
                        PayloadCodecs.VEC3.decode(buf), PayloadCodecs.VEC3.decode(buf), PayloadCodecs.VEC3.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf), PayloadCodecs.VEC3.apply(ByteBufCodecs.list(96)).decode(buf));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Draws the lines, splats and strikes, and tells the local hero's movement. */
    static void handle(SpiderFxPayload payload, IPayloadContext context) {
        SpiderEffects.add(payload);
    }
}
