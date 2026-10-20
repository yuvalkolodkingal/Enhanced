package com.kolodkingal.enhanced.love;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Love, Thor's daughter (3.8), as a {@link HeroServer} plug-in: her keys, her body and her tick are
 * {@link LoveAbilities} (with {@link EyeLaser} and {@link ThunderStrike}; her axe is Thor's thrown weapon, her
 * lightning Thor's beam). How she is made: Eternity's wish ({@code eternity/Wishes.becomeLove}).
 */
public final class LoveServer implements HeroServer {
    @Override
    public Power power() {
        return Power.LOVE;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        LoveAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        LoveAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        LoveAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        LoveAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        LoveAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        LoveAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        LoveAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        LoveAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        LoveAbilities.onGone(player);
    }

    @Override
    public void onServerStopped() {
        LoveAbilities.clear();
    }
}
