package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Cyclops (3.3), as a {@link HeroServer} plug-in: his abilities and his body are
 * {@link CyclopsAbilities}, his glasses and the giant beam that pours out without them {@link CyclopsGlasses}, and his one
 * event (no fall damage after an Optic Leap) is in {@code CyclopsEvents}.
 */
public final class CyclopsServer implements HeroServer {
    @Override
    public Power power() {
        return Power.CYCLOPS;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        CyclopsAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        CyclopsAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        CyclopsAbilities.tick(player, data);
    }

    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        CyclopsAbilities.everyPlayerTick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        CyclopsAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        CyclopsAbilities.endAll(player, data);
    }

    @Override
    public void onLogin(ServerPlayer player) {
        CyclopsAbilities.onBack(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        CyclopsAbilities.onGone(player, true);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        CyclopsAbilities.onGone(player, false);
    }

    @Override
    public void onRespawn(ServerPlayer player) {
        CyclopsAbilities.onBack(player);
    }

    @Override
    public void onServerStopped() {
        CyclopsAbilities.clear();
    }
}
