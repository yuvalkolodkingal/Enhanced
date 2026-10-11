package com.kolodkingal.enhanced.boss;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * What a boss lane registers with {@link BossFights#register} to make its fight: the rituals and {@code /enhanced boss
 * start} both go through it. The framework has already checked what is the same for every boss (the difficulty, the
 * limit on fights, the rest); the lane checks what is its own and builds its {@link BossFight}.
 */
public interface BossLane {
    /**
     * Why this fight cannot start, as the player is told, or null if it can: the lane's own checks of the site, the
     * items and who is near. All of them come <em>before</em> anything is used up.
     *
     * @param force an operator's start ({@code /enhanced boss start <boss> force}), which skips the ritual's own
     *              checks (the site, the items, a hero nearby) but never the shared ones
     */
    @Nullable
    Component refusal(ServerPlayer starter, boolean force);

    /**
     * Uses up what the ritual consumes (and registers it with {@link BossFight#refundOnFailure}) and makes the fight.
     * Called only when {@link #refusal} answered null. The framework registers the fight, which begins at once.
     *
     * @return the fight, or null if it could not be made after all
     */
    @Nullable
    BossFight create(ServerPlayer starter, boolean force);
}
