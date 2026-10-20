package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitMoves;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Mark 50's moves (3.8, lane L4, PLAN 6.1; Infinity War's nanotech suit):
 * <ul>
 * <li>R <b>Nano Repulsor</b>: an instant blast, {@code repulsorDamage} 7 and a small boom of 0.8; 4 energy, a gap of
 * 10. Sneak + R the <b>Nano Blade</b> ({@link EnergyBlades#nanoBlade}).</li>
 * <li>V <b>Unibeam</b> (held ≤ 60): {@code unibeamDamage} 5 every 10 ticks to the first creature; 6 energy a
 * second.</li>
 * <li>B <b>Nano Shield</b> (held ≤ {@code shieldMaxTicks} 160, {@link EnergyShield}).</li>
 * <li>N <b>Nano Cannon</b>: held, both forearms merge into a cannon ({@link Live#IRON_CHARGING}); released after {@code
 * cannonChargeTicks} 20 it fires: {@code cannonDamage} 14 at the first creature or block within 32 and a boom of {@code
 * cannonPower} 1.5 (radius 3 for creatures, no block broken). Released early it is refused and half its energy comes
 * back. 25 energy (paid when the charge begins), cooldown 160. Held to {@link #CANNON_HOLD_MAX} it fires by
 * itself.</li> </ul>
 */
public final class Mark50Moves implements SuitMoves {
    public static final Mark50Moves INSTANCE = new Mark50Moves();
    /** The repulsor's gap and the Unibeam's numbers the plan gives without an option of the Mark 50's own. */
    static final int REPULSOR_GAP = 10;
    static final double REPULSOR_POWER = 0.8;
    static final double REPULSOR_ENERGY = 4.0;
    static final double UNIBEAM_ENERGY_PER_SECOND = 6.0;
    /** The cannon's reach, and how long it may be held before it fires by itself. */
    static final double CANNON_RANGE = 32.0;
    public static final int CANNON_HOLD_MAX = 100;

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        switch (ability) {
            case 1 -> {
                if (down) {
                    if (alt) {
                        EnergyBlades.nanoBlade(player, data);
                    } else {
                        repulsor(player, data);
                    }
                }
            }
            case 3 -> {
                if (down) {
                    NanoBeams.start(player, data, unibeam());
                } else {
                    NanoBeams.stop(player, data);
                }
            }
            case 4 -> {
                if (down) {
                    EnergyShield.raise(player, data, SuitKind.MARK_50);
                } else {
                    EnergyShield.lower(player, data);
                }
            }
            case 5 -> {
                if (down) {
                    charge(player, data);
                } else {
                    release(player, data);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(ServerPlayer player, PowerData data, long now) {
        NanoBeams.tick(player, data, unibeam());
        EnergyShield.tick(player, data, SuitKind.MARK_50, now);
        EnergyBlades.tick(player, data, SuitKind.MARK_50, now);
        NanoState state = NanoState.peek(player.getUUID());
        if (state != null && state.cannonSince != 0L && now - state.cannonSince >= CANNON_HOLD_MAX) {
            release(player, data);
        }
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        NanoBeams.stop(player, data);
        EnergyShield.lower(player, data);
        EnergyBlades.sheathe(player, data);
        NanoState state = NanoState.peek(player.getUUID());
        if (state != null) {
            state.cannonSince = 0L;
        }
        data.set(Live.IRON_CHARGING, false);
    }

    static NanoBeams.Spec unibeam() {
        return new NanoBeams.Spec(NanoBeams.UNIBEAM, AvengersBeams.UNIBEAM,
                NanoTech.cfg(IronManConfig.MARK3_UNIBEAM_RANGE, 24), 0.35,
                (float) NanoTech.cfg(IronManConfig.MARK50_UNIBEAM_DAMAGE, 5.0), UNIBEAM_ENERGY_PER_SECOND,
                NanoTech.cfg(IronManConfig.MARK3_UNIBEAM_MAX_TICKS, 60), false);
    }

    /** R: the Nano Repulsor. */
    @Nullable
    public static LivingEntity repulsor(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.IRON_MAN_REPULSOR, now) || !SuitEnergy.spend(player, REPULSOR_ENERGY)) {
            return null;
        }
        data.startCooldown(HeroAbility.IRON_MAN_REPULSOR, now, REPULSOR_GAP);
        return Mark85Moves.repulsorRay(player, true, NanoTech.cfg(IronManConfig.MARK3_REPULSOR_RANGE, 32),
                (float) NanoTech.cfg(IronManConfig.MARK50_REPULSOR_DAMAGE, 7.0), REPULSOR_POWER);
    }

    /** N went down: the cannon forms and charges (its energy paid now). */
    public static boolean charge(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        NanoState state = NanoState.of(player.getUUID());
        if (state.cannonSince != 0L || !data.isReady(HeroAbility.IRON_MAN_MISSILES, now)
                || !SuitEnergy.spend(player, cannonEnergy())) {
            return false;
        }
        state.cannonSince = now;
        data.set(Live.IRON_CHARGING, true);
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.CANNON_CHARGE, List.of(NanoTech.chest(player)),
                NanoTech.cfg(IronManConfig.MARK50_CANNON_CHARGE_TICKS, 20));
        NanoTech.sound(level, player.position(), ModSounds.NANO_CANNON_CHARGE.get(), 1.0F, 1.0F);
        return true;
    }

    /**
     * N came up: the cannon fires if it charged long enough, else it is refused and half its energy comes back.
     *
     * @return whether it fired
     */
    public static boolean release(ServerPlayer player, PowerData data) {
        NanoState state = NanoState.peek(player.getUUID());
        if (state == null || state.cannonSince == 0L) {
            return false;
        }
        long now = player.level().getGameTime();
        long held = now - state.cannonSince;
        state.cannonSince = 0L;
        data.set(Live.IRON_CHARGING, false);
        if (held < NanoTech.cfg(IronManConfig.MARK50_CANNON_CHARGE_TICKS, 20)) {
            SuitEnergy.add(player, cannonEnergy() * 0.5);
            NanoTech.sound(player.serverLevel(), player.position(), ModSounds.NANO_RETRACT.get(), 0.6F, 1.3F);
            return false;
        }
        data.startCooldown(HeroAbility.IRON_MAN_MISSILES, now, NanoTech.cfg(IronManConfig.MARK50_CANNON_COOLDOWN,
                160));
        NanoTech.Ray ray = NanoTech.ray(player, CANNON_RANGE);
        LivingEntity struck = null;
        if (ray.target() instanceof LivingEntity living && NanoTech.mayHurt(player, living)
                && NanoTech.hurt(player, living, ModDamageTypes.REPULSOR,
                (float) NanoTech.cfg(IronManConfig.MARK50_CANNON_DAMAGE, 14.0))) {
            struck = living;
            NanoTech.push(living, player.position(), 1.0, 0.25, player);
        }
        if (ray.hit()) {
            NanoTech.boom(player, ray.end(), NanoTech.cfg(IronManConfig.MARK50_CANNON_POWER, 1.5),
                    ModDamageTypes.REPULSOR, struck);
        }
        ServerLevel level = player.serverLevel();
        Vec3 from = NanoTech.chest(player).add(player.getLookAngle().scale(0.8));
        NanoTech.send(level, player, IronFxPayload.CANNON_FIRE, List.of(from, ray.end()), 0);
        NanoTech.sound(level, from, ModSounds.NANO_CANNON_FIRE.get(), 1.4F, 0.9F);
        return true;
    }

    static double cannonEnergy() {
        return NanoTech.cfg(IronManConfig.MARK50_CANNON_ENERGY, 25.0);
    }
}
