package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.stones.TimeRewind;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Doctor Strange, the Sorcerer Supreme (3.6), as a {@link HeroServer} plug-in: it routes his keys
 * (R, V, B, N and the Eye on Y as key 6), ticks, body and what ends when he leaves to {@link StrangeAbilities}. For
 * every player it keeps the Eye's time of a sorcerer who is not Strange ({@link EyeTime}). His flight on G is the
 * shared flight of every flier ({@code Power.canFly}); his events are {@link StrangeEvents}.
 */
public final class StrangeServer implements HeroServer {
    @Override
    public Power power() {
        return Power.DOCTOR_STRANGE;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        StrangeAbilities.onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /** 3.4: the key with its Sneak variant said by the caller (a custom hero's binding, the Y key). */
    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        StrangeAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        StrangeAbilities.tick(player, data);
    }

    /**
     * Every player: a sorcerer who is not Strange but carries the Eye keeps a history for its Rewind, and the Eye's Slow
     * Time he started keeps its limits.
     */
    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        if (data.acts(Power.DOCTOR_STRANGE) || !player.isAlive()) {
            return;
        }
        long now = player.level().getGameTime();
        EyeTime.tickHolder(player, data, now);
        if (now % TimeRewind.SNAPSHOT_EVERY == 0 && Traits.has(data, Trait.SORCERER) && EyeTime.hasEye(player)) {
            TimeRewind.remember(player, now, EyeTime.rewindSeconds() * 20L);
        }
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        StrangeAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        StrangeAbilities.endAll(player, data);
        if (data.power() == Power.DOCTOR_STRANGE) {
            EyeOfAgamotto.release(player);
        }
    }

    @Override
    public void stopHold(PowerData data) {
        StrangeAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        StrangeAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        StrangeAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        // (A spirit that crossed into another world returns to its body first, in the body's world.)
        StrangeAbilities.onGone(player);
        Powers.syncIfDirty(player);
    }

    @Override
    public void onServerStopped() {
        StrangeAbilities.clear();
    }
}
