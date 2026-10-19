package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.network.StrangeFxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The spells of Doctor Strange that every client near sees (3.6), sent as {@link StrangeFxPayload}s; the client's
 * drawers are {@code StrangeSpellFx}'. Each kind also starts its gesture on the caster. What the points and the number
 * mean, per kind:
 * <ul>
 * <li>{@link StrangeFxPayload#WHIP}: points [where the lash ends]; arg the lash of the combo (1-3), or {@link #GRAPPLE}
 * for the Whip Grapple.</li>
 * <li>{@link StrangeFxPayload#BANDS}: points [the bound one]; arg the bound one's entity id (the ribbons follow it).</li>
 * <li>{@link StrangeFxPayload#SHIELD}: arg 1 raised, 0 lowered; {@link #EYE_REWIND}: the Eye's Rewind (points [where he
 * stands after it]).</li>
 * <li>{@link StrangeFxPayload#DOME}: points [the centre, (its radius in tenths, 0, 0)]; arg its life in ticks. A
 * negative arg is the Eye of Agamotto opening at an awakening: green rings for {@code -arg} ticks.</li>
 * <li>{@link StrangeFxPayload#PORTAL_OPEN}: points [the near ring, the far ring]; {@link StrangeFxPayload#PORTAL_CLOSE}:
 * points [the rings].</li>
 * <li>{@link StrangeFxPayload#ASTRAL_OUT}, {@link StrangeFxPayload#ASTRAL_IN}: points [the body].</li>
 * <li>{@link StrangeFxPayload#MIRROR_ON}: points [the centre]; arg the radius in tenths of a block. {@link
 * StrangeFxPayload#MIRROR_OFF}: points [the centre].</li>
 * </ul>
 */
public final class StrangeSignals {
    /** The arg of a {@link StrangeFxPayload#WHIP} that is the Whip Grapple. */
    public static final int GRAPPLE = 4;
    /** How long the Eye's green rings of an awakening turn (ticks). */
    public static final int AWAKEN_TICKS = 30;
    /** The arg of a {@link StrangeFxPayload#SHIELD} that is the Eye's Rewind (its green rings run backward). */
    public static final int EYE_REWIND = 2;
    /** How far away the spells are seen (a Mirror Dimension's warp needs only its own sphere). */
    public static final double RANGE = 96.0;

    /** Sends a spell to everyone near {@code at} (the caster's own client too). */
    public static void near(ServerLevel level, Vec3 at, int kind, Entity who, List<Vec3> points, int arg) {
        List<Vec3> capped = points.size() <= StrangeFxPayload.MAX_POINTS ? points
                : List.copyOf(points.subList(0, StrangeFxPayload.MAX_POINTS));
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, RANGE,
                new StrangeFxPayload(kind, who == null ? -1 : who.getId(), capped, arg));
    }

    private StrangeSignals() {
    }
}
