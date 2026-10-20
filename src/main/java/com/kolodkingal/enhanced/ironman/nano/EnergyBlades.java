package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The nanotech blades (3.8, lane L4). The Mark 50's <b>Nano Blade</b> (Sneak + R, PLAN 6.1): a blade forms on the right
 * forearm for {@code bladeTicks} 100, his empty main hand hitting for {@code bladeDamage} 9 with a sweep; 8 energy,
 * cooldown 160. The Mark 85's <b>Energy Blades</b> (Sneak + R, on and off, PLAN 6.2): long blue blades come out of the
 * backs of both hands; while they are out R swings them (a three-hit combo {@code bladeDamage1/2/3} 10 / 10 / 14,
 * reach 4, the third sweeping up to 3 creatures) and his empty-hand blow is {@code bladeHandDamage} 12 with a sweep;
 * 1 energy a second, off at 0. Out, either sets {@link Live#IRON_BLADES} (the client draws them from the bit and the
 * suit). The blows are {@code iron_blade} damage.
 */
public final class EnergyBlades {
    /** The slash combo's reach, the creatures its last hit sweeps, the ticks between two slashes, its reset. */
    public static final double SLASH_REACH = 4.0;
    public static final int SWEEP_COUNT = 3;
    public static final int SLASH_GAP = 8;
    public static final int COMBO_RESET = 30;
    /** The sweep of an empty-hand blow with the blades out: its share of the blow, its reach round the one struck. */
    public static final float SWEEP_SHARE = 0.5F;
    public static final double SWEEP_REACH = 1.25;

    /** Whether his blades are out (either suit's). */
    public static boolean out(Player player) {
        return Powers.get(player).is(Live.IRON_BLADES);
    }

    // ---- the Mark 85's: on and off, and the combo ----

    /** Sneak + R on the Mark 85 (or a custom hero's {@code iron_man.energy_blades}): the blades out, or in again. */
    public static boolean toggle(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.IRON_MAN_ENERGY_BLADES)) {
            return false;
        }
        if (data.is(Live.IRON_BLADES)) {
            sheathe(player, data);
            return true;
        }
        if (!SuitEnergy.spend(player, perSecond() / 20.0)) {
            return false;
        }
        data.set(Live.IRON_BLADES, true);
        NanoState state = NanoState.of(player.getUUID());
        state.combo = -1;
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.BLADE_OUT, List.of(NanoTech.palm(player, true),
                NanoTech.palm(player, false)), SuitKind.MARK_85.ordinal());
        NanoTech.sound(level, player.position(), ModSounds.NANO_BLADE_OUT.get(), 1.0F, 0.9F);
        return true;
    }

    /** The blades go in (Sneak + R, the energy gone, the suit off). */
    public static void sheathe(ServerPlayer player, PowerData data) {
        if (!data.is(Live.IRON_BLADES)) {
            return;
        }
        data.set(Live.IRON_BLADES, false);
        NanoState state = NanoState.peek(player.getUUID());
        if (state != null) {
            state.nanoBladeUntil = 0L;
            state.combo = -1;
        }
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.BLADE_IN, List.of(player.position()), 0);
        NanoTech.sound(level, player.position(), ModSounds.NANO_BLADE_IN.get(), 0.8F, 1.0F);
    }

    static double perSecond() {
        return NanoTech.cfg(IronManConfig.MARK85_BLADE_ENERGY_PER_SECOND, 1.0);
    }

    /**
     * R with the Mark 85's blades out: the next slash of the combo at the creature in front (reach 4): 10, 10, then 14
     * that sweeps up to three. A slash comes at most every {@link #SLASH_GAP} ticks; the combo starts over after {@link
     * #COMBO_RESET} ticks without one.
     *
     * @return the creatures struck (empty when he slashed the air or it was too soon)
     */
    public static List<LivingEntity> slash(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        NanoState state = NanoState.of(player.getUUID());
        if (now < state.nextSlash) {
            return List.of();
        }
        int step = now - state.lastSlash > COMBO_RESET || state.combo >= 2 ? 0 : state.combo + 1;
        state.combo = step;
        state.lastSlash = now;
        state.nextSlash = now + SLASH_GAP;
        float damage = (float) switch (step) {
            case 0 -> NanoTech.cfg(IronManConfig.MARK85_BLADE_DAMAGE_1, 10.0);
            case 1 -> NanoTech.cfg(IronManConfig.MARK85_BLADE_DAMAGE_2, 10.0);
            default -> NanoTech.cfg(IronManConfig.MARK85_BLADE_DAMAGE_3, 14.0);
        };
        List<LivingEntity> targets = new ArrayList<>();
        if (step == 2) {
            List<LivingEntity> near = new ArrayList<>(NanoTech.near(player, SLASH_REACH, 75.0));
            near.sort(Comparator.comparingDouble(player::distanceToSqr));
            targets.addAll(near.subList(0, Math.min(SWEEP_COUNT, near.size())));
        } else {
            LivingEntity one = NanoTech.inFront(player, SLASH_REACH);
            if (one != null) {
                targets.add(one);
            }
        }
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity target : targets) {
            if (NanoTech.hurt(player, target, ModDamageTypes.IRON_BLADE, damage)) {
                NanoTech.push(target, player.position(), step == 2 ? 0.8 : 0.4, 0.1, player);
                struck.add(target);
            }
        }
        player.swing(step == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, true);
        ServerLevel level = player.serverLevel();
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        NanoTech.send(level, player, IronFxPayload.BLADE_SLASH, List.of(at), step);
        NanoTech.sound(level, at, ModSounds.ENERGY_BLADE_SWING.get(), 1.0F, 0.9F + 0.15F * step);
        return struck;
    }

    // ---- the Mark 50's ----

    /** Sneak + R on the Mark 50: the nano blade forms on the right forearm for {@code bladeTicks}. */
    public static boolean nanoBlade(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        NanoState own = NanoState.of(player.getUUID());
        if (data.is(Live.IRON_BLADES) || !NanoTech.ownReady(player, data, HeroAbility.IRON_MAN_ENERGY_BLADES,
                own.bladeReadyAt, "message.enhanced.nanotech.blade_wait")
                || !SuitEnergy.spend(player, NanoTech.cfg(IronManConfig.MARK50_BLADE_ENERGY, 8.0))) {
            return false;
        }
        own.bladeReadyAt = NanoTech.startOwn(player, data, HeroAbility.IRON_MAN_ENERGY_BLADES,
                NanoTech.cfg(IronManConfig.MARK50_BLADE_COOLDOWN, 160));
        NanoState.of(player.getUUID()).nanoBladeUntil = now + NanoTech.cfg(IronManConfig.MARK50_BLADE_TICKS, 100);
        data.set(Live.IRON_BLADES, true);
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.BLADE_OUT, List.of(NanoTech.palm(player, true)),
                SuitKind.MARK_50.ordinal());
        NanoTech.sound(level, player.position(), ModSounds.NANO_BLADE_OUT.get(), 1.0F, 1.1F);
        return true;
    }

    /** Every tick of a nanotech suit: the Mark 50's blade's time, the Mark 85's blades' upkeep. */
    static void tick(ServerPlayer player, PowerData data, SuitKind kind, long now) {
        if (!data.is(Live.IRON_BLADES)) {
            return;
        }
        NanoState state = NanoState.of(player.getUUID());
        if (kind == SuitKind.MARK_50) {
            if (state.nanoBladeUntil == 0L) {
                state.nanoBladeUntil = now + NanoTech.cfg(IronManConfig.MARK50_BLADE_TICKS, 100);
            }
            if (now >= state.nanoBladeUntil) {
                sheathe(player, data);
            }
        } else if (!SuitEnergy.drain(player, perSecond())) {
            sheathe(player, data);
        }
    }

    // ---- the empty hand ----

    /**
     * What a blow of his empty main hand does with the blades out (the suit's blade damage at full strength, less for
     * a weak one, as {@code WolverineEvents}), or -1 when the blades are in. A custom hero's are the Mark 85's.
     */
    public static float handDamage(Player player, float strength) {
        if (!out(player) || !player.getMainHandItem().isEmpty()) {
            return -1.0F;
        }
        boolean mark50 = IronSuits.worn(player) == SuitKind.MARK_50;
        double full = mark50 ? NanoTech.cfg(IronManConfig.MARK50_BLADE_DAMAGE, 9.0)
                : NanoTech.cfg(IronManConfig.MARK85_BLADE_HAND_DAMAGE, 12.0);
        return (float) (full * (0.2F + 0.8F * strength * strength));
    }

    /** The creatures beside the one struck take half the blow (a full-strength one only). */
    public static void sweep(ServerPlayer player, LivingEntity struck, float amount) {
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox()
                .inflate(SWEEP_REACH, 0.25, SWEEP_REACH), e -> e != struck && NanoTech.mayHurt(player, e)
                && player.distanceToSqr(e) < 25.0)) {
            NanoTech.hurt(player, e, ModDamageTypes.IRON_BLADE, amount * SWEEP_SHARE);
        }
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.BLADE_SLASH, List.of(struck.position()
                .add(0.0, struck.getBbHeight() * 0.5, 0.0)), 3);
        NanoTech.sound(level, struck.position(), ModSounds.ENERGY_BLADE_SWING.get(), 0.8F, 1.2F);
    }

    private EnergyBlades() {
    }
}
