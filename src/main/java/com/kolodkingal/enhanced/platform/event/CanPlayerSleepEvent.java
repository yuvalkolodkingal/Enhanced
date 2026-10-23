package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** A player tries to sleep: a problem keeps him awake. */
public class CanPlayerSleepEvent extends PlayerEvent {
    private final BlockPos pos;
    private final @Nullable Player.BedSleepingProblem vanillaProblem;
    private @Nullable Player.BedSleepingProblem problem;

    public CanPlayerSleepEvent(ServerPlayer entity, BlockPos pos, @Nullable Player.BedSleepingProblem vanillaProblem) {
        super(entity);
        this.pos = pos;
        this.vanillaProblem = vanillaProblem;
        this.problem = vanillaProblem;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public Player.BedSleepingProblem getVanillaProblem() {
        return vanillaProblem;
    }

    @Nullable
    public Player.BedSleepingProblem getProblem() {
        return problem;
    }

    public void setProblem(@Nullable Player.BedSleepingProblem problem) {
        this.problem = problem;
    }
}
