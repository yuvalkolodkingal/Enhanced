package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.SandFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: one effect of Apocalypse (the boss or the hero) that everyone nearby should see, the one small
 * payload that carries all of them (waves, spikes, tornadoes, rings, portals, the hits and the breaking of his shield,
 * his armour falling away, the arena beginning and ending). The client's effects class switches on {@code kind}.
 *
 * <p>The kinds, and what each one's fields mean (everything a hazard's picture needs is in its one payload, sent when it
 * starts: the client simulates it in step, and only the Tornado, which drifts, is sent again every 4 ticks):
 *
 * <table>
 *     <caption>The kinds of effect</caption>
 *     <tr><th>kind</th><th>id</th><th>a</th><th>b</th><th>c, d</th></tr>
 *     <tr><td>{@link #WAVE}</td><td>the owner's entity id</td><td>origin</td><td>direction (unit)</td><td>speed x 100, length</td></tr>
 *     <tr><td>{@link #WAVE_TELL}, {@link #SPIKE_TELL}, {@link #STREAK}, {@link #RING_TELL}, {@link #TORNADO_TELL}</td>
 *     <td>the boss's id</td><td>the position (or the first of several)</td><td>the second position (or the aim)</td>
 *     <td>ticks of the tell, aux (a radius x 10, a target's id)</td></tr>
 *     <tr><td>{@link #SPIKES}</td><td>the boss's or the hero's id</td><td>centre</td><td></td><td>radius x 10, count</td></tr>
 *     <tr><td>{@link #TORNADO}</td><td>hazard id</td><td>position</td><td>target position</td><td>ticks left, radius x 10</td></tr>
 *     <tr><td>{@link #RING}</td><td>the boss's id</td><td>centre</td><td></td><td>speed x 100, max radius</td></tr>
 *     <tr><td>{@link #PORTAL}</td><td>traveller id (or -1)</td><td>where</td><td>where to</td><td>ticks, size x 10</td></tr>
 *     <tr><td>{@link #SHIELD_HIT}, {@link #SHIELD_BREAK}, {@link #SHIELD_REFORM}</td><td>the entity id (boss or hero)</td>
 *     <td>the impact point</td><td></td><td>strength fraction x 100, state (the ordinal of the shield's state)</td></tr>
 *     <tr><td>{@link #ARMOUR_FALL}, {@link #DEATH_SAND}, {@link #DEATH_PHOENIX}, {@link #ESCAPE}</td><td>the boss's id</td>
 *     <td></td><td></td><td>ticks</td></tr>
 *     <tr><td>{@link #ARENA_ON}, {@link #ARENA_OFF}</td><td>the fight id's low bits</td><td>centre</td><td></td><td>radius, height</td></tr>
 *     <tr><td>{@link #RITUAL}</td><td>0</td><td>the click</td><td></td><td></td></tr>
 * </table>
 *
 * @param kind what to draw (one of the constants of this record)
 * @param id   the entity (the boss, the hero, a hazard, a fight) it is about
 * @param a    the first position, or the origin
 * @param b    the second position, or a direction
 * @param c    the first number (ticks, a strength, a speed, a radius times ten: the kind decides)
 * @param d    the second number
 */
public record ApocalypseFxPayload(byte kind, int id, Vec3 a, Vec3 b, int c, int d) implements CustomPacketPayload {
    public static final byte WAVE = 1;
    public static final byte WAVE_TELL = 2;
    public static final byte SPIKE_TELL = 3;
    public static final byte STREAK = 4;
    public static final byte RING_TELL = 5;
    public static final byte SPIKES = 6;
    public static final byte TORNADO = 7;
    public static final byte RING = 8;
    public static final byte PORTAL = 9;
    public static final byte SHIELD_HIT = 10;
    public static final byte SHIELD_BREAK = 11;
    public static final byte SHIELD_REFORM = 12;
    public static final byte ARMOUR_FALL = 13;
    public static final byte DEATH_SAND = 14;
    public static final byte DEATH_PHOENIX = 15;
    public static final byte ESCAPE = 16;
    public static final byte ARENA_ON = 17;
    public static final byte ARENA_OFF = 18;
    public static final byte RITUAL = 19;
    /** The dust that spirals up where a Tornado will start (the second tell of a Tornado is {@link #TORNADO} itself). */
    public static final byte TORNADO_TELL = 20;
    /** The hero's Sandstorm rises (or ends, with {@code c} 0) round {@code id}; {@code d} is its radius x 10. */
    public static final byte SANDSTORM = 21;
    /** The hero's cocoon-less Entomb tell: sand climbs the legs of {@code c} (an entity id) for {@code d} ticks. */
    public static final byte ENTOMB = 22;

    public static final Type<ApocalypseFxPayload> TYPE = new Type<>(Enhanced.id("apocalypse_fx"));

    public static final StreamCodec<ByteBuf, ApocalypseFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, ApocalypseFxPayload::kind,
            ByteBufCodecs.VAR_INT, ApocalypseFxPayload::id,
            PayloadCodecs.VEC3, ApocalypseFxPayload::a,
            PayloadCodecs.VEC3, ApocalypseFxPayload::b,
            ByteBufCodecs.VAR_INT, ApocalypseFxPayload::c,
            ByteBufCodecs.VAR_INT, ApocalypseFxPayload::d,
            ApocalypseFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The effect arrived: the client draws it (one call into a client class, loaded only on a client). */
    static void handle(ApocalypseFxPayload payload, IPayloadContext context) {
        SandFx.handle(payload);
    }
}
