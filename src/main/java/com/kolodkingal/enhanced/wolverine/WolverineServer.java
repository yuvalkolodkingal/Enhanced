package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Wolverine (3.7), as a {@link HeroServer} plug-in: his keys, his body and his tick are {@link
 * WolverineAbilities} (with {@link Claws}, {@link ClawSlash}, {@link Pounce}, {@link Berserker}, {@link Senses}, {@link
 * ClawClimb} and {@link Adamantium}); his damage, item and fall hooks {@link WolverineEvents}; how he is made, the
 * Adamantium Injection ({@code item/AdamantiumInjectionItem}). His healing factor is the shared one of {@code
 * healing/}.
 */
public final class WolverineServer implements HeroServer {
    public WolverineServer() {
        WolverineEvents.listen();
    }

    @Override
    public Power power() {
        return Power.WOLVERINE;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        WolverineAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        WolverineAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        WolverineAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        WolverineAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        WolverineAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        WolverineAbilities.clearData(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        WolverineAbilities.onGone(player);
        WolverineEvents.forget(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        WolverineAbilities.onGone(player);
        WolverineEvents.forget(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        WolverineAbilities.onGone(player);
    }

    @Override
    public void onServerStopped() {
        WolverineAbilities.clear();
        WolverineEvents.clear();
    }
}
