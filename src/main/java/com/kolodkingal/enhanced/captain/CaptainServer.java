package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Captain America (3.8), as a {@link HeroServer} plug-in: his keys, his body and his tick are {@link
 * CaptainAbilities} (with {@link ShieldThrow} and its {@link ThrownShieldEntity}, {@link ShieldSlam}, {@link
 * ShieldCharge}, {@link Assemble} and {@link CaptainCombo}); the Vibranium Shield's rules {@link ShieldRules} and his
 * damage, shield and projectile hooks {@link CaptainEvents}. How he is made: the Super Soldier Serum ({@code serum/}).
 */
public final class CaptainServer implements HeroServer {
    @Override
    public Power power() {
        return Power.CAPTAIN_AMERICA;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        CaptainAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        CaptainAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        CaptainAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        CaptainAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        CaptainAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        CaptainAbilities.clearData(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        CaptainAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        CaptainAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        CaptainAbilities.onGone(player);
    }

    @Override
    public void onServerStopped() {
        CaptainAbilities.clear();
    }
}
