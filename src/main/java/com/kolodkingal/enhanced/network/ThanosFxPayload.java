package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client: one effect of Thanos (the boss or the hero) that everyone nearby should see, for a few ticks: a ring
 * on the ground that tells where a blow will land, a beam, a portal, a falling star, a dome, chains. The client draws
 * it for a short while.
 *
 * @param kind     what to draw (the constants below)
 * @param color    its colour ({@code 0xRRGGBB}, -1 for the banded halo of the five stones)
 * @param entityId the caster, for where its hand is, or the victim; or -1
 * @param ticks    how long it lasts, or the time to impact
 * @param radius   its size
 * @param points   where (the meaning depends on the kind), at most 16
 */
public record ThanosFxPayload(int kind, int color, int entityId, int ticks, float radius, List<Vec3> points)
        implements CustomPacketPayload {
    /** A ring on the ground at {@code points[0]} that grows to its full {@code radius} over {@code ticks}: the time to impact. */
    public static final int RING_TELL = 0;
    /** A thin line from {@code points[0]} to {@code points[1]}, white-hot after {@code ticks}; {@code radius} 1 draws it through walls. */
    public static final int LINE_TELL = 1;
    /** A thick beam along the line, {@code radius} wide, for {@code ticks}; colour -1 is the Infinity Beam's banded halo. */
    public static final int BEAM = 2;
    /** A ring portal at {@code points[0]}: the arrival, the Portal Step (both ends), the meteors' portals. */
    public static final int PORTAL = 3;
    /** The big portal in the sky above the arrival, with the column of light that falls from it to {@code points[0]}. */
    public static final int PORTAL_SKY = 4;
    /** A falling star from {@code points[0]} (the sky) to {@code points[1]} (the Gauntlet): a stone arriving. */
    public static final int STAR = 5;
    /** A ring that expands from nothing to {@code radius} over {@code ticks} at {@code points[0]}. */
    public static final int SHOCK_RING = 6;
    /** A mandala lying flat at {@code points[0]}; a thin ring that becomes a solid disc at the end (the Time Snare). */
    public static final int MANDALA = 7;
    /** A translucent dome of {@code radius} round {@code points[0]} (the Barrier, the Veil, the Time Lock). */
    public static final int DOME = 8;
    /** A red wave that runs out along the ground from {@code points[0]} to {@code radius}. */
    public static final int WAVE = 9;
    /** Orange chains from {@code entityId} (the caster) to every point (the targets' chests). */
    public static final int CHAINS = 10;
    /** A column of orange light rising from a ring of {@code radius} at {@code points[0]} (the Soul World's gateway). */
    public static final int PILLAR = 11;
    /** Only on the victim's client ({@code entityId}): the Soul World's orange sky, for {@code ticks}. */
    public static final int SOULWORLD = 12;
    /** False pillars of stone round the petrified target at {@code points[0]}: nothing in the world changes. */
    public static final int ILLUSION = 13;
    /** A white flash of the screen. */
    public static final int FLASH = 14;

    public static final Type<ThanosFxPayload> TYPE = new Type<>(Enhanced.id("thanos_fx"));

    public static final StreamCodec<ByteBuf, ThanosFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThanosFxPayload::kind,
            ByteBufCodecs.INT, ThanosFxPayload::color,
            ByteBufCodecs.VAR_INT, ThanosFxPayload::entityId,
            ByteBufCodecs.VAR_INT, ThanosFxPayload::ticks,
            ByteBufCodecs.FLOAT, ThanosFxPayload::radius,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(16)), ThanosFxPayload::points,
            ThanosFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The Thanos lane draws the effect here. */
    static void handle(ThanosFxPayload payload, IPayloadContext context) {
    }
}
