package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.stones.StoneFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → client (3.6): a moment of the Infinity Stones everyone near should see. The kinds are listed here (lane
 * L1's: the rejection, the slip, the pedestal, the Snap; lane L2's: the powers; lane L5's: the soul given; lane L4's:
 * the site ambience); the client hands each kind to the drawer its lane registered ({@code StoneFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId the stone, its holder or its pedestal (the meaning depends on the kind), or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record StoneFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (the payload stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int BUBBLE = 0;
    public static final int REJECT = 1;
    public static final int SLIP = 2;
    public static final int REWIND = 3;
    public static final int SLOW_ON = 4;
    public static final int SLOW_OFF = 5;
    public static final int POWER_BLAST = 6;
    public static final int REALM_DEPART = 7;
    public static final int REALM_ARRIVE = 8;
    public static final int SOUL_GIVEN = 9;
    public static final int SNAP = 10;
    public static final int PEDESTAL_TAKE = 11;
    /** The site ambience to loop for this player (arg: {@code SiteAmbience.NONE|SANCTUM_LOOP|END_TUNNEL_LOOP}). */
    public static final int SITE_AMBIENCE = 12;
    /**
     * 3.7: the Stone Locator's spark trail (lane L5): points from the holder toward the site (at most 16), arg the
     * stone's kind (a {@code StoneKind} ordinal).
     */
    public static final int LOCATOR_TRAIL = 13;

    public static final Type<StoneFxPayload> TYPE = new Type<>(Enhanced.id("stone_fx"));

    public static final StreamCodec<ByteBuf, StoneFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoneFxPayload::kind,
            ByteBufCodecs.VAR_INT, StoneFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), StoneFxPayload::points,
            ByteBufCodecs.VAR_INT, StoneFxPayload::arg,
            StoneFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(StoneFxPayload payload, PacketContext context) {
        StoneFx.receive(payload);
    }
}
