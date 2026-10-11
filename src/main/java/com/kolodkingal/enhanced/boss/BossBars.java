package com.kolodkingal.enhanced.boss;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * The two boss bars of a fight, built the same way for the three bosses so that they look alike: the first carries the
 * boss's health (its name says the phase), the second the thing the fight is also about (Ultron's Sentries,
 * Apocalypse's shield, the lift) and is shown only when the boss wants it. Neither darkens the sky, fogs the world or
 * plays the vanilla boss music: the bosses have their own.
 *
 * <p>Both bars are shown to the fight's participants and to everyone within {@link #SHOW_RADIUS} blocks of the centre
 * ({@link #refresh}, which the fight calls once a second). A boss that draws its own bar names the first one with a
 * translatable key and registers a drawer with the client's {@code BossBarOverride}.
 */
public final class BossBars {
    /** Who stands this near the centre sees the bars whether they fight or not: 64 blocks. */
    public static final double SHOW_RADIUS = 64.0;

    private final ServerBossEvent health;
    private final ServerBossEvent second;

    /** The first bar is the boss's health; the second starts hidden, named like it and white. */
    public BossBars(Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        this.health = bar(name, color, overlay);
        this.second = bar(name, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        this.second.setVisible(false);
    }

    private static ServerBossEvent bar(Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        ServerBossEvent bar = new ServerBossEvent(name, color, overlay);
        bar.setDarkenScreen(false);
        bar.setCreateWorldFog(false);
        bar.setPlayBossMusic(false);
        return bar;
    }

    /** The boss's health bar. */
    public ServerBossEvent health() {
        return health;
    }

    /** The second bar (shown only after {@link #showSecond}). */
    public ServerBossEvent second() {
        return second;
    }

    /** Shows or hides the second bar for everyone. */
    public void showSecond(boolean shown) {
        second.setVisible(shown);
    }

    /**
     * Shows both bars to the participants who are in the level and to everyone near the centre, and takes them from
     * everyone else.
     */
    public void refresh(ServerLevel level, Vec3 center, Iterable<ServerPlayer> participants) {
        Set<ServerPlayer> wanted = new HashSet<>();
        for (ServerPlayer player : participants) {
            if (player.level() == level) {
                wanted.add(player);
            }
        }
        double reach = SHOW_RADIUS * SHOW_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(center) <= reach) {
                wanted.add(player);
            }
        }
        for (ServerBossEvent bar : new ServerBossEvent[]{health, second}) {
            for (ServerPlayer seeing : Set.copyOf(bar.getPlayers())) {
                if (!wanted.contains(seeing)) {
                    bar.removePlayer(seeing);
                }
            }
            for (ServerPlayer player : wanted) {
                bar.addPlayer(player);
            }
        }
    }

    /** The players who see the first bar. */
    public Set<ServerPlayer> viewers() {
        return Set.copyOf(health.getPlayers());
    }

    /** Takes both bars from everyone (the fight is over). */
    public void close() {
        health.removeAllPlayers();
        second.removeAllPlayers();
    }
}
