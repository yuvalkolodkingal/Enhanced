package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Sneak + N on the Mark 85: the <b>Nanotech Drones</b> (3.8, lane L4, PLAN 6.2): {@code droneCount} 2 {@link
 * NanoDroneEntity}s form behind his shoulders for {@code droneTicks} 400 (20 seconds). 30 energy, cooldown 900 (its
 * own: {@link NanoTech#ownReady}).
 * {@link IronManState#DRONES} and {@code dronesUntil} say so to the clients (the HUD's drones' time).
 */
public final class NanoDrones {
    /**
     * Sends the drones out.
     *
     * @return the drones made (empty when refused)
     */
    public static List<NanoDroneEntity> deploy(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        NanoState own = NanoState.of(player.getUUID());
        if (!data.has(HeroAbility.IRON_MAN_DRONES) || !NanoTech.ownReady(player, data, HeroAbility.IRON_MAN_DRONES,
                own.dronesReadyAt, "message.enhanced.nanotech.drones_wait")
                || !SuitEnergy.spend(player, NanoTech.cfg(IronManConfig.MARK85_DRONE_ENERGY, 30.0))) {
            return List.of();
        }
        own.dronesReadyAt = NanoTech.startOwn(player, data, HeroAbility.IRON_MAN_DRONES,
                NanoTech.cfg(IronManConfig.MARK85_DRONE_COOLDOWN, 900));
        recall(player);
        long until = now + NanoTech.cfg(IronManConfig.MARK85_DRONE_TICKS, 400);
        int count = NanoTech.cfg(IronManConfig.MARK85_DRONE_COUNT, 2);
        int gap = NanoTech.cfg(IronManConfig.MARK85_DRONE_SHOT_GAP, 15);
        ServerLevel level = player.serverLevel();
        List<NanoDroneEntity> drones = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // The first shots a few ticks after they form, staggered so the two do not fire together.
            NanoDroneEntity drone = NanoDroneEntity.of(player, i % 2, until, 10 + i * Math.max(1, gap / 2));
            level.addFreshEntity(drone);
            drones.add(drone);
        }
        IronSuits.setState(player, IronSuits.state(player).with(IronManState.DRONES, true).withDronesUntil(until));
        NanoTech.send(level, player, IronFxPayload.DRONES_OUT,
                drones.stream().map(d -> d.position()).toList(), count);
        NanoTech.sound(level, player.position(), ModSounds.NANO_DRONES_DEPLOY.get(), 1.0F, 1.0F);
        return drones;
    }

    /** Whether his drones may stay: alive, in the Mark 85 (or a custom hero with the drones), their time not up. */
    static boolean keeps(ServerPlayer player) {
        PowerData data = Powers.get(player);
        IronManState state = IronSuits.state(player);
        return state.has(IronManState.DRONES) && player.level().getGameTime() < state.dronesUntil()
                && data.has(HeroAbility.IRON_MAN_DRONES)
                && (data.power() == Power.CUSTOM || IronSuits.worn(player) == SuitKind.MARK_85);
    }

    /** His drones near him (in this level). */
    public static List<NanoDroneEntity> of(Player player) {
        return player.level().getEntitiesOfClass(NanoDroneEntity.class, new AABB(player.blockPosition()).inflate(48.0),
                d -> d.owner() == player);
    }

    /** Every tick: their time up, the flag goes (the drones go by themselves). */
    static void tick(ServerPlayer player, long now) {
        IronManState state = IronSuits.state(player);
        if (state.has(IronManState.DRONES) && (now >= state.dronesUntil() || !keeps(player))) {
            recall(player);
        }
    }

    /** The drones go now (his time, the suit off, death, logout). */
    public static void recall(ServerPlayer player) {
        boolean had = false;
        for (NanoDroneEntity drone : of(player)) {
            drone.discard();
            had = true;
        }
        IronManState state = IronSuits.state(player);
        if (state.has(IronManState.DRONES)) {
            IronSuits.setState(player, state.with(IronManState.DRONES, false).withDronesUntil(0L));
            had = true;
        }
        if (had) {
            NanoTech.send(player.serverLevel(), player, IronFxPayload.DRONES_IN, List.of(player.position()), 0);
        }
    }

    private NanoDrones() {
    }
}
