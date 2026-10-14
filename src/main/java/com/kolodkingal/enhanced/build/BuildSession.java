package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A builder's live Build Magic state on the server (D-B 11.1): whether she is in build mode, the block she last picked,
 * the token buckets that bound what her client may send (C6: {@code maxRequestsPerSecond} for every build payload, 4
 * preflights a second), how many spells she has cast since she logged in (the first five say how many blocks), and the
 * lines she is to be told at her next login ("While you were away, your spell finished").
 *
 * <p>One per player, by UUID; it is not saved. It outlives a logout (so a spell that finishes while she is away can
 * leave her its line), and {@link #clear} forgets them all (the server's stop, the end of a test).
 */
public final class BuildSession {
    /** The most preflights a builder may ask for each second (D-B 10.1); more are dropped, not queued. */
    public static final int PREFLIGHTS_PER_SECOND = 4;
    /** How often (ticks) a builder's dropped requests are logged: once a minute (C6). */
    private static final int DROPS_LOGGED_EVERY = 1200;
    /** The spells after a login whose done line says how many blocks they changed; after them, just the pop. */
    public static final int SAID_SPELLS = 5;

    private static final Map<UUID, BuildSession> ALL = new HashMap<>();

    private final UUID player;
    private boolean mode;
    private @Nullable BlockState picked;
    private double requests = Double.NaN;
    private long requestsAt;
    private double preflights = PREFLIGHTS_PER_SECOND;
    private long preflightsAt;
    private int casts;
    private long droppedLoggedAt = Long.MIN_VALUE / 2;
    private long momSaidAt = Long.MIN_VALUE / 2;
    private final List<Component> atLogin = new ArrayList<>();

    private BuildSession(UUID player) {
        this.player = player;
    }

    /** The session of the player {@code player} (made the first time it is asked for). */
    public static BuildSession of(UUID player) {
        return ALL.computeIfAbsent(player, BuildSession::new);
    }

    /** The session of {@code player}, if there is one. */
    public static @Nullable BuildSession existing(UUID player) {
        return ALL.get(player);
    }

    /** Forgets every session. */
    public static void clear() {
        ALL.clear();
    }

    public UUID player() {
        return player;
    }

    /** Whether the builder is in build mode. */
    public boolean mode() {
        return mode;
    }

    public void setMode(boolean mode) {
        this.mode = mode;
    }

    /** The block she last picked with the middle button (the server's own read of it), or null for the hand. */
    public @Nullable BlockState picked() {
        return picked;
    }

    public void setPicked(@Nullable BlockState picked) {
        this.picked = picked;
    }

    /**
     * Whether a build payload that arrives at server tick {@code tick} is taken (C6): a bucket of
     * {@code build_magic.maxRequestsPerSecond} tokens that refills at that many a second.
     */
    public boolean takeRequest(long tick) {
        int perSecond = EnhancedConfig.SPEC.isLoaded() ? BuildConfig.MAX_REQUESTS_PER_SECOND.get() : 40;
        if (Double.isNaN(requests)) {
            requests = perSecond;
            requestsAt = tick;
        }
        requests = Math.min(perSecond, requests + Math.max(0L, tick - requestsAt) * perSecond / 20.0);
        requestsAt = tick;
        if (requests < 1.0) {
            return false;
        }
        requests -= 1.0;
        return true;
    }

    /** Whether a dropped request at server tick {@code tick} is to be logged: once a minute at most (C6). */
    public boolean logDropped(long tick) {
        if (tick - droppedLoggedAt < DROPS_LOGGED_EVERY) {
            return false;
        }
        droppedLoggedAt = tick;
        return true;
    }

    /** Whether a preflight that arrives at server tick {@code tick} is answered: at most 4 a second (D-B 10.1). */
    public boolean takePreflight(long tick) {
        preflights = Math.min(PREFLIGHTS_PER_SECOND, preflights + Math.max(0L, tick - preflightsAt) * PREFLIGHTS_PER_SECOND / 20.0);
        preflightsAt = tick;
        if (preflights < 1.0) {
            return false;
        }
        preflights -= 1.0;
        return true;
    }

    /** Counts a spell done; true for the first {@link #SAID_SPELLS} since the login (their line says how many blocks). */
    public boolean saysDone() {
        return ++casts <= SAID_SPELLS;
    }

    /** A new login: the first spells say their size again. */
    public void loggedIn() {
        casts = 0;
    }

    /** When "Mom is near" was last said (game time). */
    public long momSaidAt() {
        return momSaidAt;
    }

    public void setMomSaidAt(long gameTime) {
        this.momSaidAt = gameTime;
    }

    /** A line to tell the builder when she next logs in. */
    public void tellAtLogin(Component line) {
        atLogin.add(line);
    }

    /** The lines waiting for her login, oldest first; they are forgotten as they are given. */
    public List<Component> takeLoginLines() {
        List<Component> lines = List.copyOf(atLogin);
        atLogin.clear();
        return lines;
    }
}
