package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * A mob is about to be finalized and added; cancelling skips the finalization, setSpawnCancelled(true) stops the spawn.
 */
public class FinalizeSpawnEvent extends MobSpawnEvent implements Cancellable {
    private final DifficultyInstance difficulty;
    private final MobSpawnType spawnType;
    private @Nullable SpawnGroupData spawnData;
    private boolean spawnCancelled;
    private boolean canceled;

    public FinalizeSpawnEvent(
            Mob entity, ServerLevelAccessor level, double x, double y, double z, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        super(entity, level, x, y, z);
        this.difficulty = difficulty;
        this.spawnType = spawnType;
        this.spawnData = spawnData;
        this.spawnCancelled = false;
    }

    public DifficultyInstance getDifficulty() {
        return difficulty;
    }

    public MobSpawnType getSpawnType() {
        return spawnType;
    }

    @Nullable
    public SpawnGroupData getSpawnData() {
        return spawnData;
    }

    public void setSpawnData(@Nullable SpawnGroupData spawnData) {
        this.spawnData = spawnData;
    }

    public boolean isSpawnCancelled() {
        return spawnCancelled;
    }

    public void setSpawnCancelled(boolean spawnCancelled) {
        this.spawnCancelled = spawnCancelled;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
