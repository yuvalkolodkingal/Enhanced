package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One player's trial while it exists: his challenge drawn in a cell of the Road, where he is in it, and what he has spawned
 * there. The framework runs it ({@link RoadTrials}); a {@link RoadChallenge} gets it in every hook and uses {@link #succeed()},
 * {@link #putBack}, {@link #track} and the layout.
 */
public final class RoadTrial {
    /** Where a trial is in its life. */
    public enum Phase {
        /** Its blocks are being placed; the player has not arrived. */
        PLACING,
        /** Built, waiting for the player to be taken in. */
        READY,
        /** The player is playing it. */
        ACTIVE,
        /** The player has won it and stands on its finish platform; the gate is open. */
        WON,
        /** It is being taken down. */
        CLEARING
    }

    private final MinecraftServer server;
    private final UUID owner;
    private final RoadChallenge challenge;
    private final int number;
    private final long seed;
    private final RoadCanvas canvas;
    private final RoadLayout layout;
    private final long[] chunks;

    private Phase phase = Phase.PLACING;
    private int checkpoint;
    private long startedAt;
    private long lastClock = Long.MIN_VALUE;
    private int voice;
    private final List<Entity> tracked = new ArrayList<>();

    RoadTrial(MinecraftServer server, UUID owner, RoadChallenge challenge, int number, long seed, RoadCanvas canvas, RoadLayout layout) {
        this.server = server;
        this.owner = owner;
        this.challenge = challenge;
        this.number = number;
        this.seed = seed;
        this.canvas = canvas;
        this.layout = layout;
        this.chunks = canvas.chunks();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What it is

    public MinecraftServer server() {
        return server;
    }

    public UUID owner() {
        return owner;
    }

    /** The player, if he is online. */
    @Nullable
    public ServerPlayer player() {
        return server.getPlayerList().getPlayer(owner);
    }

    public RoadChallenge challenge() {
        return challenge;
    }

    /** Which trial it is, 0 for the first. */
    public int number() {
        return number;
    }

    public long seed() {
        return seed;
    }

    public RoadCanvas canvas() {
        return canvas;
    }

    public RoadLayout layout() {
        return layout;
    }

    /** The cell this trial lives in. */
    public int cell() {
        return canvas.cell();
    }

    /** The chunks it holds tickets on. */
    public long[] chunks() {
        return chunks.clone();
    }

    public Phase phase() {
        return phase;
    }

    void setPhase(Phase phase) {
        this.phase = phase;
    }

    /** The Road dimension. */
    public ServerLevel level() {
        ServerLevel level = RoadDimension.level(server);
        if (level == null) {
            throw new IllegalStateException("The Road has no dimension");
        }
        return level;
    }

    /** The player's saved progress. */
    public RoadProgress progress() {
        return RoadSave.get(server).progress(owner);
    }

    /** The checkpoint he is at now (0 is the start). */
    public int checkpoint() {
        return checkpoint;
    }

    void setCheckpoint(int checkpoint) {
        this.checkpoint = checkpoint;
    }

    /** Ticks since the player was put at the start. */
    public long elapsed() {
        return phase == Phase.ACTIVE || phase == Phase.WON ? level().getGameTime() - startedAt : 0L;
    }

    void markStarted() {
        this.startedAt = level().getGameTime();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What a challenge can do

    /**
     * The player has won the trial: the gate opens (his progress is saved, the next trial is unlocked). Does nothing if he has
     * not got that far or has already won.
     */
    public void succeed() {
        if (phase == Phase.ACTIVE && !challenge.isEnding()) {
            phase = Phase.WON;
            RoadTrials.won(this);
        }
    }

    /**
     * Puts the player back at his checkpoint (a challenge that decides a fall by itself calls this), and tells the challenge.
     *
     * @param fell whether it counts as a fall (the Road's voice, and the player's tally)
     */
    public void putBack(ServerPlayer player, boolean fell) {
        RoadLayout.Checkpoint at = layout.checkpoint(checkpoint);
        RoadTrials.moveWithin(player, at.respawn(), at.yaw());
        if (fell) {
            progress().addFall();
            RoadVoice.say(player, RoadVoice.FALL, voice++);
        }
        RoadTrials.restore(player);
        RoadFx.putBack(level(), at.respawn());
        challenge.onReset(this, player, checkpoint);
    }

    /** Remembers an entity the trial spawned: it is removed when the trial is taken down. */
    public <T extends Entity> T track(T entity) {
        tracked.add(entity);
        return entity;
    }

    /** Forgets an entity the trial has taken down itself (a boat that was replaced). */
    public void untrack(Entity entity) {
        tracked.remove(entity);
    }

    public List<Entity> tracked() {
        return List.copyOf(tracked);
    }

    void discardTracked() {
        for (Entity entity : tracked) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
        tracked.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Playing

    /** Every tick while the player is in the Road and this is his trial. */
    void tick(ServerPlayer player) {
        ServerLevel level = level();
        RoadRules.enforce(player);
        Vec3 pos = player.position();
        if (phase == Phase.WON) {
            RoadLayout.Gate gate = layout.gate();
            if (gate != null) {
                if (level.getGameTime() % 3 == 0) {
                    RoadFx.gate(level, gate.zone());
                }
                if (gate.zone().contains(pos)) {
                    RoadTrials.advance(player);
                    return;
                }
            }
            if (pos.y < layout.checkpoint(checkpoint).failY()) {
                putBack(player, false);
            }
            return;
        }
        if (phase != Phase.ACTIVE) {
            return;
        }
        RoadLayout.Checkpoint here = layout.checkpoint(checkpoint);
        if (pos.y < here.failY()) {
            putBack(player, true);
            return;
        }
        List<RoadLayout.Checkpoint> all = layout.checkpoints();
        for (int i = all.size() - 1; i > checkpoint; i--) {
            if (all.get(i).zone() != null && all.get(i).zone().contains(pos)) {
                reach(player, i);
                break;
            }
        }
        if (layout.finish() != null && layout.finish().contains(pos)) {
            succeed();
            return;
        }
        if (layout.timeLimitTicks() > 0) {
            long left = layout.timeLimitTicks() - (level.getGameTime() - startedAt);
            if (left <= 0) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.time_up").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
                checkpoint = 0;
                progress().setCheckpoint(0);
                startedAt = level.getGameTime();
                putBack(player, false);
                return;
            }
            long second = left / 20;
            if (second != lastClock && left % 20 == 0) {
                lastClock = second;
                player.displayClientMessage(Component.translatable("message.enhanced.road.clock", clock(second)).withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
            }
        }
        challenge.tick(this, player);
    }

    private void reach(ServerPlayer player, int index) {
        checkpoint = index;
        progress().setCheckpoint(index);
        RoadLayout.Checkpoint at = layout.checkpoint(index);
        RoadFx.checkpoint(level(), at.respawn());
        if (WitchesRoadConfig.HEAL_AT_CHECKPOINT.get()) {
            RoadTrials.restore(player);
        }
        player.displayClientMessage(Component.translatable("message.enhanced.road.checkpoint", index, layout.checkpoints().size() - 1)
                .withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), true);
        RoadVoice.say(player, RoadVoice.CHECKPOINT, voice++);
    }

    /** Seconds as m:ss. */
    public static String clock(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
