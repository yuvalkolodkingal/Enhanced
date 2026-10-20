package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Falcon (3.8), as a {@link HeroServer} plug-in: his keys, his body and his tick are {@link
 * FalconAbilities} (with the moves {@link WristWeapons} and its {@link FalconDartEntity} and Iron Man's missile for
 * the rocket, the {@link Dive} and the Boost Climb, the {@link WingShield} and the Wing Slash, {@link Redwing} and its
 * {@link RedwingEntity}); the wings' flight and glide {@link FalconWings}; his event hooks {@link FalconEvents}.
 */
public final class FalconServer implements HeroServer {
    @Override
    public Power power() {
        return Power.FALCON;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        FalconAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        FalconAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        FalconAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        FalconAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        FalconAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        FalconAbilities.stopHold(data);
    }

    @Override
    public void onLogin(ServerPlayer player) {
        // (Redwing is never saved with the world: a Falcon who logs in has it in his pack.)
        com.kolodkingal.enhanced.power.PowerData data = com.kolodkingal.enhanced.power.Powers.get(player);
        if (data.toggle(Toggle.REDWING_OUT)) {
            data.setToggle(Toggle.REDWING_OUT, false);
        }
        Redwing.forgetDrone(player);
        FalconAbilities.quietAwaken(player);
    }

    @Override
    public void onRespawn(ServerPlayer player) {
        FalconAbilities.quietAwaken(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        FalconAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        FalconAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        FalconAbilities.onGone(player);
    }

    @Override
    public void onServerStopped() {
        FalconAbilities.clear();
    }
}
