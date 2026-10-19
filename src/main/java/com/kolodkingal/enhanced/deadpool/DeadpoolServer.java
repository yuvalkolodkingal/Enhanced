package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Deadpool (3.7), as a {@link HeroServer} plug-in: his keys, his body and his tick are {@link
 * DeadpoolAbilities} (with the moves {@link Katanas}, {@link Pistols} and their {@link DeadpoolBulletEntity}, the
 * {@link TeleportBelt}, the {@link Taunt} and its tricks, {@link DogpoolEntity Dogpool}); his lines {@link
 * DeadpoolLines}; his event hooks {@link DeadpoolEvents}; how he is made, the Weapon X Pod's {@link PodTreatment} (it
 * ticks for every player: a powerless one is in the pod). His healing factor is the shared one ({@code healing/}, lane
 * L1).
 */
public final class DeadpoolServer implements HeroServer {
    static {
        DeadpoolLines.init(); // (his lines listen to the healing factor's DOWN and rise from the start)
    }

    @Override
    public Power power() {
        return Power.DEADPOOL;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        DeadpoolAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        DeadpoolAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        DeadpoolAbilities.tick(player, data);
    }

    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        PodTreatment.tick(player);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        DeadpoolAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        DeadpoolAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        DeadpoolAbilities.stopHold(data);
    }

    @Override
    public void onLogin(ServerPlayer player) {
        PodTreatment.onLogin(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        DeadpoolAbilities.onGone(player);
        DeadpoolLines.forget(player.getUUID());
        PodTreatment.forget(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        DeadpoolAbilities.onGone(player);
        PodTreatment.forget(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        DeadpoolAbilities.onGone(player);
        PodTreatment.forget(player);
    }

    @Override
    public void onServerStopped() {
        DeadpoolAbilities.clear();
        PodTreatment.clear();
    }
}
