package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.ironman.IronFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → client (3.8): a moment of Iron Man everyone near should see (lanes L3 and L4): a suit-up, a take-off and a
 * skip (arg: the {@code SuitKind}), the repulsors, the flares and missiles, the Mark 1's flamethrower, rocket, boost,
 * crash, brace and punch, the Mark 5's dash and laser burst, the nano blades, shields and cannon, the Lightning
 * Refocuser (points: the chain), a missile lock (arg: the target's id), the drones, the Hulkbuster's grab and throw,
 * Veronica's arm and frame, the energy running out, a suit going offline. The client hands each kind to the drawer its
 * lane registered ({@code IronFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId Iron Man, or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record IronFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (an effect of ten points stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int SUIT_UP = 0;
    public static final int SUIT_DOWN = 1;
    public static final int SUIT_SKIP = 2;
    public static final int REPULSOR = 3;
    public static final int DOUBLE_BLAST = 4;
    public static final int FLARES = 5;
    public static final int MISSILES = 6;
    public static final int FLAME_ON = 7;
    public static final int FLAME_OFF = 8;
    public static final int ARM_ROCKET = 9;
    public static final int BOOST = 10;
    public static final int CRASH = 11;
    public static final int BRACE = 12;
    public static final int PUNCH = 13;
    public static final int DASH = 14;
    public static final int LASER_BURST = 15;
    public static final int BLADE_OUT = 16;
    public static final int BLADE_IN = 17;
    public static final int BLADE_SLASH = 18;
    public static final int SHIELD_HIT = 19;
    public static final int CANNON_CHARGE = 20;
    public static final int CANNON_FIRE = 21;
    public static final int REFOCUSER = 22;
    public static final int LOCK = 23;
    public static final int DRONES_IN = 24;
    public static final int DRONES_OUT = 25;
    public static final int DRONE_SHOT = 26;
    public static final int GRAB = 27;
    public static final int THROW = 28;
    public static final int VERONICA = 29;
    public static final int FRAME_LAND = 30;
    public static final int ENERGY_EMPTY = 31;
    public static final int OFFLINE = 32;

    public static final Type<IronFxPayload> TYPE = new Type<>(Enhanced.id("iron_fx"));

    public static final StreamCodec<ByteBuf, IronFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IronFxPayload::kind,
            ByteBufCodecs.VAR_INT, IronFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), IronFxPayload::points,
            ByteBufCodecs.VAR_INT, IronFxPayload::arg,
            IronFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(IronFxPayload payload, PacketContext context) {
        IronFx.receive(payload);
    }
}
