package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.CyclopsFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.3): one blast of Cyclops's that everyone nearby should see, drawn by the client's {@code CyclopsFx}
 * (his two beams are {@link BeamPayload}s of the shared beam system). The kinds, and what their fields mean:
 *
 * <table>
 *     <caption>The kinds of effect</caption>
 *     <tr><th>kind</th><th>points</th><th>param</th></tr>
 *     <tr><td>{@link #RICOCHET}</td><td>the bolt's path: where it left his eyes, every bounce, where it ended</td><td>the number of creatures it hit</td></tr>
 *     <tr><td>{@link #CONCUSSION}</td><td>where it left his eyes, and the direction (unit)</td><td>its reach x 10</td></tr>
 *     <tr><td>{@link #LEAP}</td><td>where the blast struck the ground, and where it left his eyes</td><td>its radius x 10</td></tr>
 *     <tr><td>{@link #RALLY}</td><td>where he stands</td><td>its radius</td></tr>
 * </table>
 *
 * @param kind    what to draw (one of the constants of this record)
 * @param shooter Cyclops's entity id
 * @param points  the positions (at most {@link #MAX_POINTS})
 * @param param   a number whose meaning the kind decides
 */
public record CyclopsFxPayload(byte kind, int shooter, List<Vec3> points, int param) implements CustomPacketPayload {
    public static final byte RICOCHET = 1;
    public static final byte CONCUSSION = 2;
    public static final byte LEAP = 3;
    public static final byte RALLY = 4;

    /** The most points a payload carries (a ricochet of eight bounces is ten). */
    public static final int MAX_POINTS = 16;

    public static final Type<CyclopsFxPayload> TYPE = new Type<>(Enhanced.id("cyclops_fx"));

    public static final StreamCodec<ByteBuf, CyclopsFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, CyclopsFxPayload::kind,
            ByteBufCodecs.VAR_INT, CyclopsFxPayload::shooter,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), CyclopsFxPayload::points,
            ByteBufCodecs.VAR_INT, CyclopsFxPayload::param,
            CyclopsFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the effects class is only ever loaded there. */
    static void handle(CyclopsFxPayload payload, IPayloadContext context) {
        CyclopsFx.handle(payload);
    }
}
