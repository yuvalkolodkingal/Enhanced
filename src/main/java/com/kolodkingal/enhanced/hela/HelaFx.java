package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.network.AsgardFxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The moments of Hela that everyone near should see, as {@link AsgardFxPayload}s of her lane's range (400-499); the
 * client draws them ({@code HelaFx} of the client) and, where the one who did it is a player, plays her gesture for it
 * (the hero's keys are acted by the payload of their effect, which names the caster).
 */
public final class HelaFx {
    /** The fan of blades about to be thrown appears behind her: {@code points} = where each blade hangs, arg = ticks. */
    public static final int VOLLEY = AsgardFxPayload.HELA;
    /** The cracks of a Blade Wall run along the floor: {@code points} = the spikes' feet in order, arg = ticks to rise. */
    public static final int WALL = AsgardFxPayload.HELA + 1;
    /** Smoke where she leaves and where she comes out: {@code points} = from, to. */
    public static final int SHADOW_STEP = AsgardFxPayload.HELA + 2;
    /** The circle of a Blade Rain on the ground: {@code points} = its centre, arg = radius in tenths. */
    public static final int RAIN = AsgardFxPayload.HELA + 3;
    /** The ring of a Blade Storm sweeping out: {@code points} = its centre, arg = the radius in tenths it reaches. */
    public static final int STORM = AsgardFxPayload.HELA + 4;
    /** She channels the Strength of Asgard: green-gold threads from the floor to her; arg = ticks (0 = it ends). */
    public static final int CHANNEL = AsgardFxPayload.HELA + 5;
    /** The channel breaks: a burst of shards; {@code points} = her chest. */
    public static final int CHANNEL_BREAK = AsgardFxPayload.HELA + 6;
    /** She comes out of the seal: smoke pours from it to the dais; {@code points} = the seal, the dais; arg = ticks. */
    public static final int ARRIVE = AsgardFxPayload.HELA + 7;
    /** She dissolves into black smoke; {@code points} = her feet. */
    public static final int DISSOLVE = AsgardFxPayload.HELA + 8;
    /** A thrown Mjolnir caught in her hand and crushed; {@code points} = the hand. */
    public static final int CRUSH = AsgardFxPayload.HELA + 9;
    /** A berserker climbs out of the floor; {@code points} = his feet. */
    public static final int RISE = AsgardFxPayload.HELA + 10;
    /** The hero's Death Dash: a trail of smoke; {@code points} = from, to. */
    public static final int DASH = AsgardFxPayload.HELA + 11;
    /** The hero's Blade Burst: a ring of spikes round her; {@code points} = the centre, arg = radius in tenths. */
    public static final int BURST = AsgardFxPayload.HELA + 12;
    /** A projectile crumbles in Death's Grasp; {@code points} = where. */
    public static final int CRUMBLE = AsgardFxPayload.HELA + 13;
    /** The headdress grows (arg 1) or fades (arg 0) on the hero. */
    public static final int FORM = AsgardFxPayload.HELA + 14;
    /** Fenris comes at her call; {@code points} = where he appears. */
    public static final int FENRIS = AsgardFxPayload.HELA + 15;
    /** The hero throws a fan of necroswords (the gesture; the blades are entities): {@code points} = her hand. */
    public static final int THROW = AsgardFxPayload.HELA + 16;
    /** The hero raises her dead (the gesture; the risings have their own {@link #RISE}): {@code points} = her feet. */
    public static final int RAISE = AsgardFxPayload.HELA + 17;

    /** How far the effects are sent: the length of the throne room and more. */
    public static final double RANGE = 96.0;

    public static void send(ServerLevel level, int kind, int entityId, List<Vec3> points, int arg) {
        Vec3 at = points.isEmpty() ? Vec3.ZERO : points.get(0);
        List<Vec3> capped = points.size() > AsgardFxPayload.MAX_POINTS ? points.subList(0, AsgardFxPayload.MAX_POINTS) : points;
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, RANGE, new AsgardFxPayload(kind, entityId, List.copyOf(capped), arg));
    }

    public static void send(ServerLevel level, int kind, int entityId, Vec3 at, int arg) {
        send(level, kind, entityId, List.of(at), arg);
    }

    /** To one player only (one who comes into a fight that is already channelling, for instance). */
    public static void sendTo(ServerPlayer player, int kind, int entityId, List<Vec3> points, int arg) {
        PacketDistributor.sendToPlayer(player, new AsgardFxPayload(kind, entityId, List.copyOf(points), arg));
    }

    private HelaFx() {
    }
}
