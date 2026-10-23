package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** The experience a dead living entity drops; cancelling drops none. */
public class LivingExperienceDropEvent extends LivingEvent implements Cancellable {
    private final @Nullable Player attackingPlayer;
    private final int originalExperience;
    private int droppedExperience;
    private boolean canceled;

    public LivingExperienceDropEvent(LivingEntity entity, @Nullable Player attackingPlayer, int originalExperience) {
        super(entity);
        this.attackingPlayer = attackingPlayer;
        this.originalExperience = originalExperience;
        this.droppedExperience = originalExperience;
    }

    @Nullable
    public Player getAttackingPlayer() {
        return attackingPlayer;
    }

    public int getOriginalExperience() {
        return originalExperience;
    }

    public int getDroppedExperience() {
        return droppedExperience;
    }

    public void setDroppedExperience(int droppedExperience) {
        this.droppedExperience = droppedExperience;
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
