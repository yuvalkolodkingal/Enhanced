package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Odin, the Allfather (3.5), as a {@link HeroServer} plug-in: his abilities and his body are {@link
 * OdinAbilities} (with {@link GungnirEntity}, the spear in flight, and {@link RavenEntity}, Huginn and Muninn), his shield's
 * damage hook {@link OdinEvents}; for every player, the coronation on the Throne of Asgard ({@link Coronation}) and the
 * watch over the vault's door ({@link Vault}). What Odin says when spoken to is {@link OdinDialogue}.
 */
public final class OdinServer implements HeroServer {
    static {
        OdinDialogue.register();
    }

    @Override
    public Power power() {
        return Power.ODIN;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        OdinAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        OdinAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        OdinAbilities.tick(player, data);
    }

    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        Coronation.tick(player, data);
        Vault.watch(player);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        OdinAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        OdinAbilities.endAll(player, data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        OdinAbilities.onGone(player, true);
        Coronation.forget(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        OdinAbilities.onGone(player, false);
        Coronation.forget(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        OdinAbilities.onChangeDimension(player);
        Coronation.forget(player);
    }

    @Override
    public void onServerStopped() {
        OdinAbilities.clear();
        Coronation.clear();
    }
}
