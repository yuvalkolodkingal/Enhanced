package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Sends the effects of the Ultron fight to the players who can see them: a tell decal, a beam's aim, a ring, a data
 * line. One payload each ({@link UltronFxPayload}, whose constants say what a kind means); the clients draw them. Every
 * tell of the boss goes out through here, a tick before anything can hurt.
 */
public final class UltronFx {
    /** Players within this many blocks of an effect are sent it. */
    public static final double RANGE = 96.0;

    /** Sends an effect at a point, from a caster, to nearby players. */
    public static void send(ServerLevel level, int kind, int entityId, Vec3 at, Vec3 to, float radius, int ticks) {
        Net.sendToPlayersNear(level, null, at.x, at.y, at.z, RANGE, new UltronFxPayload(kind, entityId, at, to, radius, ticks));
    }

    /** An effect at a point. */
    public static void at(ServerLevel level, int kind, Vec3 at, float radius, int ticks) {
        send(level, kind, -1, at, Vec3.ZERO, radius, ticks);
    }

    /** An effect of an entity at a point (the Beams' aim, a glint). */
    public static void from(ServerLevel level, int kind, Entity caster, Vec3 at, Vec3 to, float radius, int ticks) {
        send(level, kind, caster.getId(), at, to, radius, ticks);
    }

    /** An effect that runs along a line. */
    public static void line(ServerLevel level, int kind, Vec3 from, Vec3 to, int ticks) {
        send(level, kind, -1, from, to, 0.0F, ticks);
    }

    private UltronFx() {
    }
}
