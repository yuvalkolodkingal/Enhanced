package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.asgard.AsgardFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.5): a moment of Asgard everyone near should see (the realm, Heimdall, Odin, Hela, the Asgardians). The
 * kinds sit in one range per lane, so no two lanes take the same number; the client hands each kind to the drawer its lane
 * registered ({@code AsgardFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId who did it, or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record AsgardFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    public static final int MAX_POINTS = 64;
    /** The first kind of each lane (each has 100). */
    public static final int REALM = 100;
    public static final int HEIMDALL = 200;
    public static final int ODIN = 300;
    public static final int HELA = 400;
    public static final int ASGARDIANS = 500;

    public static final Type<AsgardFxPayload> TYPE = new Type<>(Enhanced.id("asgard_fx"));

    public static final StreamCodec<ByteBuf, AsgardFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AsgardFxPayload::kind,
            ByteBufCodecs.VAR_INT, AsgardFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), AsgardFxPayload::points,
            ByteBufCodecs.VAR_INT, AsgardFxPayload::arg,
            AsgardFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(AsgardFxPayload payload, IPayloadContext context) {
        AsgardFx.receive(payload);
    }
}
