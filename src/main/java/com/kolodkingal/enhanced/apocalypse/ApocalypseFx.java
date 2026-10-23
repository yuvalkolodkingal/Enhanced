package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The server's end of {@link ApocalypseFxPayload}: sends one effect to the players who can see it. Everything the
 * clients draw of the boss and of the hero's sand (the hazards, the tells, the shield's ripples, the arena) goes through
 * here, so the radius at which it is sent is decided in one place.
 */
public final class ApocalypseFx {
    /** How far from an effect a player still gets it: the sand wall and the amber sky are seen from a long way off. */
    public static final double RADIUS = 96.0;

    /** Sends the effect to everyone within {@link #RADIUS} of {@code near}. */
    public static void send(ServerLevel level, Vec3 near, byte kind, int id, Vec3 a, Vec3 b, int c, int d) {
        Net.sendToPlayersNear(level, null, near.x, near.y, near.z, RADIUS, new ApocalypseFxPayload(kind, id, a, b, c, d));
    }

    /** Sends the effect to everyone within {@code radius} of {@code near}. */
    public static void send(ServerLevel level, Vec3 near, double radius, byte kind, int id, Vec3 a, Vec3 b, int c, int d) {
        Net.sendToPlayersNear(level, null, near.x, near.y, near.z, radius, new ApocalypseFxPayload(kind, id, a, b, c, d));
    }

    /**
     * Sends the effect to every player in the level, wherever they are. The end of the arena is told like this: a player who
     * walked away (or was thrown out of range) while the fight lasted must not keep the wall and the amber air of a fight
     * that is over.
     */
    public static void sendToLevel(ServerLevel level, byte kind, int id, Vec3 a, Vec3 b, int c, int d) {
        Net.sendToPlayersInDimension(level, new ApocalypseFxPayload(kind, id, a, b, c, d));
    }

    /** Sends the effect to one player (a late joiner who starts tracking the boss is told where the arena is). */
    public static void sendTo(ServerPlayer player, byte kind, int id, Vec3 a, Vec3 b, int c, int d) {
        Net.sendToPlayer(player, new ApocalypseFxPayload(kind, id, a, b, c, d));
    }

    /** The effect that has a place and nothing else: its position is both the origin and where it is sent from. */
    public static void at(ServerLevel level, Vec3 at, byte kind, int id, int c, int d) {
        send(level, at, kind, id, at, Vec3.ZERO, c, d);
    }

    private ApocalypseFx() {
    }
}
