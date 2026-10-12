package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of one hero, as a plug-in: {@link HeroServers} finds it by {@link Power} and the shared switch
 * statements ({@link AbilityHandler}, {@link PowerTicker}, and {@code CommonEvents} through {@link HeroLifecycle}) call
 * it, so a hero built after Thor never touches those files. (The heroes before Thor keep their {@code switch} arms.)
 *
 * <p>Every method does nothing by default; a hero overrides the ones it needs. Those that say "every player" are called
 * for all players whatever their power, like {@code ThorAbilities.everyPlayerTick}: a hero checks for itself whether
 * the player is its own. The rest are called for the hero's own player only.
 *
 * <p>Events that are not about the power's life (damage, targeting, effects, projectiles) are not here: a hero
 * subscribes to them in a class of its own ({@code @EventBusSubscriber}), with the priorities listed in
 * {@code docs/HEROES-AND-BOSSES.md} so that independent subscribers compose.
 */
public interface HeroServer {
    /** The hero this plug-in is the server side of. */
    Power power();

    /**
     * An ability key went down or came up.
     *
     * @param ability 1-5
     */
    default void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
    }

    /** Called at the end of every server tick of the hero's player (alive). */
    default void tick(ServerPlayer player, PowerData data) {
    }

    /**
     * Every player, every server tick, before the passives: for rules that apply to anyone (a status effect, an item).
     */
    default void everyPlayerTick(ServerPlayer player, PowerData data) {
    }

    /**
     * Every player, whenever the passives are applied: the hero's body (health, armour, speed, scale) while the player
     * has the power, and none of it (the modifiers removed) while they do not.
     */
    default void applyBody(ServerPlayer player, PowerData data) {
    }

    /** The player is about to lose this power: everything it left in the world ends. */
    default void endAll(ServerPlayer player, PowerData data) {
    }

    /**
     * Every power data, when the hold-to-use abilities are released (the player died, or lost the power): clears the
     * hero's {@code Live} bits and flags that stand for a held key. See {@link PowerTicker#stopHoldAbilities}.
     */
    default void stopHold(PowerData data) {
    }

    // ---- lifecycle: every player, once per event (see HeroLifecycle) ----

    default void onLogin(ServerPlayer player) {
    }

    /** The player left the server (their data is still on them; their entities in the world are theirs to remove). */
    default void onLogout(ServerPlayer player) {
    }

    default void onDeath(ServerPlayer player) {
    }

    default void onRespawn(ServerPlayer player) {
    }

    default void onChangeDimension(ServerPlayer player) {
    }

    /** The server has stopped: forget every static map and list the hero keeps. */
    default void onServerStopped() {
    }
}
