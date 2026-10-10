package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.WorldEffectsRenderer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client: Billy cast a Hex Bolt, so every client nearby draws the arc for a moment.
 *
 * @param caster entity id of Billy (where a client can see him, the arc leaves from his hand)
 * @param from   where the arc starts on the server
 * @param to     where it strikes
 * @param forks  where it forks to from there, one arc each
 * @param seed   shapes the jagged path, so everyone sees the same bolt
 */
public record HexBoltPayload(int caster, Vec3 from, Vec3 to, List<Vec3> forks, long seed) implements CustomPacketPayload {
    public static final Type<HexBoltPayload> TYPE = new Type<>(Enhanced.id("hex_bolt"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.of(
            (buf, v) -> {
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    public static final StreamCodec<ByteBuf, HexBoltPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HexBoltPayload::caster,
            VEC3, HexBoltPayload::from,
            VEC3, HexBoltPayload::to,
            VEC3.apply(ByteBufCodecs.list(8)), HexBoltPayload::forks,
            ByteBufCodecs.VAR_LONG, HexBoltPayload::seed,
            HexBoltPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the renderer class is only ever loaded there. */
    static void handle(HexBoltPayload payload, IPayloadContext context) {
        WorldEffectsRenderer.addHexBolt(payload);
    }
}
