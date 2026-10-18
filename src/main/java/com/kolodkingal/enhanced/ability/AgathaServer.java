package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Agatha Harkness (3.3), as a {@link HeroServer} plug-in: it routes the keys, the ticks, her body and
 * what ends when she leaves, dies, changes dimension or loses the power to {@link AgathaAbilities} (the shield is {@link
 * AgathaShield}); her events (the poison, the shield's damage hook, its projectiles) are in {@code AgathaEvents}.
 */
public final class AgathaServer implements HeroServer {
    @Override
    public Power power() {
        return Power.AGATHA;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        AgathaAbilities.onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /** 3.4: the key with its Sneak variant said by the caller (a custom hero's binding). */
    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        AgathaAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        AgathaAbilities.tick(player, data);
    }

    /** Every player: a hero she drained lets go of what was held. */
    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        AgathaAbilities.everyPlayerTick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        AgathaAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        AgathaAbilities.endAll(player, data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        AgathaAbilities.onGone(player, true);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        AgathaAbilities.onGone(player, false);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        AgathaAbilities.onGone(player, false);
    }

    @Override
    public void onServerStopped() {
        AgathaAbilities.clear();
    }
}
