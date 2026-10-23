package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A player's spawn point is about to change; cancelling keeps the old one. */
public class PlayerSetSpawnEvent extends PlayerEvent implements Cancellable {
    private final ResourceKey<Level> spawnLevel;
    private final @Nullable BlockPos newSpawn;
    private final boolean forced;
    private boolean canceled;

    public PlayerSetSpawnEvent(
            Player entity, ResourceKey<Level> spawnLevel, @Nullable BlockPos newSpawn, boolean forced) {
        super(entity);
        this.spawnLevel = spawnLevel;
        this.newSpawn = newSpawn;
        this.forced = forced;
    }

    public ResourceKey<Level> getSpawnLevel() {
        return spawnLevel;
    }

    @Nullable
    public BlockPos getNewSpawn() {
        return newSpawn;
    }

    public boolean isForced() {
        return forced;
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
