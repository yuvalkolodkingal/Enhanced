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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Mark 85's moves (3.8, lane L4, PLAN 6.2; Endgame and the user's refs), and a custom hero's with Iron Man's rows
 * (PLAN 11.1: the Mark 85's core set, each move asking {@code data.has} of its row):
 * <ul>
 * <li>R <b>Boosted Repulsors</b>: two instant blasts, the right palm and 4 ticks later the left, {@code repulsorDamage}
 * 9 each and a small boom of {@code repulsorPower} 1.0; 6 energy, a gap of {@code repulsorGap} 12. Sneak + R: the
 * <b>Energy Blades</b> on and off ({@link EnergyBlades}); while they are out R swings them.</li>
 * <li>V <b>Giant Chest Laser</b> (held ≤ {@code laserMaxTicks} 80): a beam 1.5 wide from the reactor, {@code
 * laserRange} 32, {@code laserDamage} 7 every 10 ticks to every creature along it; 8 energy a second ({@link
 * NanoBeams}).</li>
 * <li>B <b>Giant Energy Shield</b> (held ≤ 200, {@link EnergyShield}); Sneak + B the <b>Lightning Refocuser</b> ({@link
 * LightningRefocuser}).</li>
 * <li>N <b>Homing Mini-Missiles</b> ({@link HomingMissiles}); Sneak + N the <b>Nanotech Drones</b> ({@link
 * NanoDrones}).</li> </ul>
 */
public final class Mark85Moves implements SuitMoves {
    public static final Mark85Moves INSTANCE = new Mark85Moves();
    /** The ticks between the two palms' blasts. */
    public static final int SECOND_BLAST_DELAY = 4;
    /** The repulsors' reach (the classic suits' {@code repulsorRange}). */
    static final int DEFAULT_RANGE = 32;

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        switch (ability) {
            case 1 -> {
                if (!down) {
                    return;
                }
                if (alt) {
                    EnergyBlades.toggle(player, data);
                } else if (data.is(Live.IRON_BLADES) && data.has(HeroAbility.IRON_MAN_ENERGY_BLADES)) {
                    EnergyBlades.slash(player, data);
                } else {
                    repulsors(player, data);
                }
            }
            case 3 -> {
                if (down) {
                    if (data.has(HeroAbility.IRON_MAN_UNIBEAM)) {
                        NanoBeams.start(player, data, laser());
                    }
                } else {
                    NanoBeams.stop(player, data);
                }
            }
            case 4 -> {
                if (!down) {
                    EnergyShield.lower(player, data);
                } else if (alt) {
                    LightningRefocuser.fire(player, data);
                } else if (data.has(HeroAbility.IRON_MAN_SHIELD)) {
                    EnergyShield.raise(player, data, SuitKind.MARK_85);
                }
            }
            case 5 -> {
                if (!down) {
                    return;
                }
                if (alt) {
                    NanoDrones.deploy(player, data);
                } else {
                    HomingMissiles.fire(player, data);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(ServerPlayer player, PowerData data, long now) {
        NanoState state = NanoState.of(player.getUUID());
        if (state.secondBlastAt != 0L && now >= state.secondBlastAt) {
            state.secondBlastAt = 0L;
            blast(player, false);
        }
        NanoBeams.tick(player, data, laser());
        EnergyShield.tick(player, data, SuitKind.MARK_85, now);
        EnergyBlades.tick(player, data, SuitKind.MARK_85, now);
        LightningRefocuser.tick(player, now);
        NanoDrones.tick(player, now);
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        NanoState state = NanoState.peek(player.getUUID());
        if (state != null) {
            state.secondBlastAt = 0L;
        }
        NanoBeams.stop(player, data);
        EnergyShield.lower(player, data);
        EnergyBlades.sheathe(player, data);
        NanoDrones.recall(player);
    }

    /** The chest laser's numbers. */
    static NanoBeams.Spec laser() {
        return new NanoBeams.Spec(NanoBeams.CHEST_LASER, AvengersBeams.CHEST_LASER,
                NanoTech.cfg(IronManConfig.MARK85_LASER_RANGE, 32), 1.5,
                (float) NanoTech.cfg(IronManConfig.MARK85_LASER_DAMAGE, 7.0),
                NanoTech.cfg(IronManConfig.MARK85_LASER_ENERGY_PER_SECOND, 8.0),
                NanoTech.cfg(IronManConfig.MARK85_LASER_MAX_TICKS, 80), true);
    }

    /**
     * R: the Boosted Repulsors, the right palm now and the left {@link #SECOND_BLAST_DELAY} ticks later.
     *
     * @return the creature the first blast struck, or null
     */
    @Nullable
    public static LivingEntity repulsors(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.IRON_MAN_REPULSOR) || !data.isReady(HeroAbility.IRON_MAN_REPULSOR, now)
                || !SuitEnergy.spend(player, NanoTech.cfg(IronManConfig.MARK85_REPULSOR_ENERGY, 6.0))) {
            return null;
        }
        data.startCooldown(HeroAbility.IRON_MAN_REPULSOR, now, NanoTech.cfg(IronManConfig.MARK85_REPULSOR_GAP, 12));
        NanoState.of(player.getUUID()).secondBlastAt = now + SECOND_BLAST_DELAY;
        return blast(player, true);
    }

    /** One palm's blast: the instant ray, its damage and its small boom. */
    @Nullable
    static LivingEntity blast(ServerPlayer player, boolean right) {
        double range = NanoTech.cfg(IronManConfig.MARK3_REPULSOR_RANGE, DEFAULT_RANGE);
        float damage = (float) NanoTech.cfg(IronManConfig.MARK85_REPULSOR_DAMAGE, 9.0);
        double power = NanoTech.cfg(IronManConfig.MARK85_REPULSOR_POWER, 1.0);
        return repulsorRay(player, right, range, damage, power);
    }

    /**
     * A repulsor's instant ray from a palm (the nanotech suits' and the Hulkbuster's): the first creature or block at
     * the crosshair takes the damage, a small boom where it lands.
     */
    @Nullable
    static LivingEntity repulsorRay(ServerPlayer player, boolean right, double range, float damage, double power) {
        NanoTech.Ray ray = NanoTech.ray(player, range);
        LivingEntity struck = null;
        if (ray.target() instanceof LivingEntity living && NanoTech.mayHurt(player, living)
                && NanoTech.hurt(player, living, ModDamageTypes.REPULSOR, damage)) {
            struck = living;
        }
        ServerLevel level = player.serverLevel();
        Vec3 from = NanoTech.palm(player, right);
        if (ray.hit()) {
            NanoTech.boom(player, ray.end(), power, ModDamageTypes.REPULSOR, struck);
        }
        player.swing(right ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
        NanoTech.send(level, player, IronFxPayload.REPULSOR, List.of(from, ray.end()), right ? 0 : 1);
        NanoTech.sound(level, from, ModSounds.REPULSOR_BLAST.get(), 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        return struck;
    }
}
