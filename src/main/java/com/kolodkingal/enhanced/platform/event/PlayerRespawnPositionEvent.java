package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.DimensionTransition;

/** Where a player respawns. */
public class PlayerRespawnPositionEvent extends PlayerEvent {
    private final DimensionTransition originalDimensionTransition;
    private final boolean fromEndFight;
    private DimensionTransition dimensionTransition;
    private boolean copyOriginalSpawnPosition;

    public PlayerRespawnPositionEvent(
            Player entity, DimensionTransition originalDimensionTransition, boolean fromEndFight) {
        super(entity);
        this.originalDimensionTransition = originalDimensionTransition;
        this.fromEndFight = fromEndFight;
        this.dimensionTransition = originalDimensionTransition;
        this.copyOriginalSpawnPosition = !originalDimensionTransition.missingRespawnBlock();
    }

    public DimensionTransition getOriginalDimensionTransition() {
        return originalDimensionTransition;
    }

    public boolean isFromEndFight() {
        return fromEndFight;
    }

    public DimensionTransition getDimensionTransition() {
        return dimensionTransition;
    }

    public void setDimensionTransition(DimensionTransition dimensionTransition) {
        this.dimensionTransition = dimensionTransition;
    }

    public boolean copyOriginalSpawnPosition() {
        return copyOriginalSpawnPosition;
    }

    public void setCopyOriginalSpawnPosition(boolean copyOriginalSpawnPosition) {
        this.copyOriginalSpawnPosition = copyOriginalSpawnPosition;
    }
}
