package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.AgathaFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client (3.3): one effect of Agatha Harkness that everyone near should see. Everything its picture needs is sent
 * once, when it starts; the client draws it in step with the game time (smoothly, with the partial tick) and forgets it
 * when it is over.
 *
 * <table>
 *     <caption>The kinds of effect</caption>
 *     <tr><th>kind</th><th>id</th><th>a</th><th>other</th><th>c</th><th>d</th></tr>
 *     <tr><td>{@link #RUNES}</td><td>Agatha's entity id</td><td>the centre of the circle (on the ground)</td><td>0</td>
 *     <td>ticks the runes burn</td><td>radius x 10</td></tr>
 *     <tr><td>{@link #RUNES_END}</td><td>Agatha's entity id</td><td>the centre</td><td>0</td><td>0</td><td>0</td></tr>
 *     <tr><td>{@link #DRAIN}</td><td>Agatha's entity id</td><td>where the target stood</td><td>the drained creature's
 *     entity id</td><td>ticks the stream flows</td><td>1 if a player's power was drained, 0 for a creature's life</td></tr>
 *     <tr><td>{@link #SHIELD_BREAK}</td><td>Agatha's entity id</td><td>the centre of the shield</td><td>0</td>
 *     <td>radius x 10</td><td>0</td></tr>
 * </table>
 *
 * @param kind  what to draw (one of the constants of this record)
 * @param id    Agatha's entity id
 * @param a     a position (see the table)
 * @param other a second entity id, or 0
 * @param c     the first number
 * @param d     the second number
 */
public record AgathaFxPayload(byte kind, int id, Vec3 a, int other, int c, int d) implements CustomPacketPayload {
    /** The Rune Trap is laid: a circle of glowing runes on the ground. */
    public static final byte RUNES = 1;
    /** The Rune Trap ends early (she lost the power, left, laid another one). */
    public static final byte RUNES_END = 2;
    /** The Power Drain: a violet stream flows from the target into her hands. */
    public static final byte DRAIN = 3;
    /** Her shield shatters into violet shards. */
    public static final byte SHIELD_BREAK = 4;

    public static final Type<AgathaFxPayload> TYPE = new Type<>(Enhanced.id("agatha_fx"));

    public static final StreamCodec<ByteBuf, AgathaFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, AgathaFxPayload::kind,
            ByteBufCodecs.VAR_INT, AgathaFxPayload::id,
            PayloadCodecs.VEC3, AgathaFxPayload::a,
            ByteBufCodecs.VAR_INT, AgathaFxPayload::other,
            ByteBufCodecs.VAR_INT, AgathaFxPayload::c,
            ByteBufCodecs.VAR_INT, AgathaFxPayload::d,
            AgathaFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The effect arrived: the client draws it (one call into a client class, loaded only on a client). */
    static void handle(AgathaFxPayload payload, IPayloadContext context) {
        AgathaFx.handle(payload);
    }
}
