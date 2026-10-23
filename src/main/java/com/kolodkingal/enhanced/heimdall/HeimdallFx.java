package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.network.AsgardFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Heimdall's kinds of {@link AsgardFxPayload} (3.5: his lane's range, 200-299) and the calls that send them. What every
 * client near sees: the golden arc of a blow, the streak of the lunge, the column of the Bifrost Strike, the open hand of a
 * send, the flash of the Einherjar called and of Hofund returning (each also starts its gesture on the caster); and what
 * only one player's client is told: Heimdall's Bifrost screen opening in Send mode, the shadows of his Watch.
 */
public final class HeimdallFx {
    /** A blow of the Hofund combo: {@code entityId} the caster, points [the centre of the arc, the look], {@code arg} the blow (1-3). */
    public static final int SLASH = AsgardFxPayload.HEIMDALL;
    /** The Hofund Lunge: points [from, to]. */
    public static final int LUNGE = AsgardFxPayload.HEIMDALL + 1;
    /** The Bifrost Strike: points [where it strikes], {@code arg} its radius in tenths of a block. */
    public static final int STRIKE = AsgardFxPayload.HEIMDALL + 2;
    /** Heimdall sends a friend by the Bifrost: points [the friend]. */
    public static final int SEND = AsgardFxPayload.HEIMDALL + 3;
    /** The Gatekeeper's Call: points [where each Einherjar stands]. */
    public static final int CALL = AsgardFxPayload.HEIMDALL + 4;
    /** Hofund returns to its Heimdall's hand. */
    public static final int RETURN = AsgardFxPayload.HEIMDALL + 5;
    /** To Heimdall alone: open the Bifrost screen in Send mode for the player {@code entityId}. */
    public static final int OPEN_SEND = AsgardFxPayload.HEIMDALL + 6;
    /**
     * To the watcher alone, every second of his Guardian's Watch: points [the shadows still hidden], {@code arg} the seconds
     * left; {@code arg} -1: the Watch is over.
     */
    public static final int WATCH = AsgardFxPayload.HEIMDALL + 7;
    /** A Shadow of Hela struck: points [where]. */
    public static final int SHADOW_STRUCK = AsgardFxPayload.HEIMDALL + 8;

    /** How far away the effects are seen. */
    public static final double RANGE = 96.0;

    /** Sends an effect to everyone near {@code at} (the caster's own client too). */
    public static void near(ServerLevel level, Vec3 at, int kind, Entity who, List<Vec3> points, int arg) {
        Net.sendToPlayersNear(level, null, at.x, at.y, at.z, RANGE,
                new AsgardFxPayload(kind, who == null ? -1 : who.getId(), cap(points), arg));
    }

    /** Sends an effect to one player alone. */
    public static void to(ServerPlayer player, int kind, int entityId, List<Vec3> points, int arg) {
        Net.sendToPlayer(player, new AsgardFxPayload(kind, entityId, cap(points), arg));
    }

    private static List<Vec3> cap(List<Vec3> points) {
        return points.size() <= AsgardFxPayload.MAX_POINTS ? points : List.copyOf(points.subList(0, AsgardFxPayload.MAX_POINTS));
    }

    private HeimdallFx() {
    }
}
