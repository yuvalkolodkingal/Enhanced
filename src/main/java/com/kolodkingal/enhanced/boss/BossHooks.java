package com.kolodkingal.enhanced.boss;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The ways the heroes reach a boss or one of its minions without knowing which boss it is: implemented by the boss and
 * minion <em>entities</em>, called from one line in each hero's code. Every hook does what a creature without the
 * interface would get by default.
 */
public interface BossHooks {
    /**
     * How many times slower Quicksilver Time makes this entity, given what the zone does to everything ({@code
     * zoneFactor}, the config's slow-motion factor): 0 means not slowed at all, 1 not slowed more than ordinary time.
     */
    default int slowFactor(int zoneFactor) {
        return zoneFactor;
    }

    /**
     * Magneto's Armor & Weapon Strip reached this entity.
     *
     * @return true if it was handled here and the normal strip must be skipped
     */
    default boolean onMagnetoStrip(ServerPlayer magneto) {
        return false;
    }

    /** A Spider-Man's web shot reached this web-immune entity (Thanos locks the Gauntlet). */
    default void onWebShot(ServerPlayer spider) {
    }

    /** Lightning reached this entity (Apocalypse's Escape is interrupted, a Sentry is stunned). */
    default void onLightning(@Nullable ServerPlayer thor, float amount) {
    }
}
