package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sneak + R: the Grab Smash (3.6; Avengers, 2012: Loki slammed left and right). He grabs the creature in front of him
 * (his reach and one more) by the legs and slams it into the ground left, right, left over 30 ticks: {@code
 * grabDamage1} + {@code grabDamage2} + {@code grabDamage3}, the last throwing it. He cannot move while he does it, and
 * the victim cannot act. A boss, a {@code #enhanced:grab_immune} creature (the Stonekeeper, the Asgardians) or a pinned
 * one is not grabbed: it takes one heavy punch ({@code grabBossPunch}) instead. A player only with PvP.
 */
public final class GrabSmash implements HulkAbilities.Move {
    /** The ticks of the three slams (after the grab), and the whole move. */
    static final int[] SLAMS = {10, 20, 30};
    static final int TICKS = 30;
    /** The last slam's throw. */
    static final double THROW_PUSH = 1.2;
    static final double THROW_LIFT = 0.45;

    private final LivingEntity victim;
    private final boolean wasNoAi;
    /** The slams' damages, fixed at the grab. */
    private float[] slams = new float[3];

    private GrabSmash(LivingEntity victim) {
        this.victim = victim;
        this.wasNoAi = victim instanceof Mob mob && mob.isNoAi();
    }

    /**
     * Sneak + R pressed: the grab, or the punch for what cannot be grabbed.
     *
     * @return the creature grabbed or punched, or null when nothing was in front of him
     */
    @Nullable
    public static LivingEntity start(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_GRAB_SMASH, now)) {
            return null;
        }
        LivingEntity target = HulkAbilities.inFront(player, 1.0);
        if (target == null || !HulkAbilities.mayHurt(player, target)) {
            return null;
        }
        return grab(player, data, target);
    }

    /**
     * 3.8: the Grab Smash with the numbers given (Red Hulk's Grab & Slam, lane L2): the three slams' {@code damages}
     * (the last throws) and the one heavy punch {@code punchDamage} for what cannot be grabbed, taken as they are (not
     * the Hulk's look or rage factors). No cooldown is asked or started (the caller's own); the move is the Hulk's
     * ({@link HulkAbilities#driveMove} plays it for a hero who is not the Hulk).
     *
     * @return the creature grabbed or punched, or null when nothing was in front of him
     */
    @Nullable
    public static LivingEntity start(ServerPlayer player, PowerData data, double[] damages, double punchDamage) {
        LivingEntity target = HulkAbilities.inFront(player, 1.0);
        if (target == null || !HulkAbilities.mayHurt(player, target)) {
            return null;
        }
        float[] slams = new float[3];
        for (int i = 0; i < slams.length; i++) {
            slams[i] = (float) damages[Math.min(i, damages.length - 1)];
        }
        return grab(player, target, slams, (float) punchDamage);
    }

    /** Grabs (or punches) this creature. */
    public static LivingEntity grab(ServerPlayer player, PowerData data, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        data.startCooldown(HeroAbility.HULK_GRAB_SMASH, now, HulkAbilities.cfg(HulkConfig.GRAB_COOLDOWN, 160));
        return grab(player, target, new float[]{
                HulkAbilities.damage(player, data, HulkAbilities.cfg(HulkConfig.GRAB_DAMAGE1, 4.0)),
                HulkAbilities.damage(player, data, HulkAbilities.cfg(HulkConfig.GRAB_DAMAGE2, 4.0)),
                HulkAbilities.damage(player, data, HulkAbilities.cfg(HulkConfig.GRAB_DAMAGE3, 6.0))},
                HulkAbilities.damage(player, data, HulkAbilities.cfg(HulkConfig.GRAB_BOSS_PUNCH, 8.0)));
    }

    /** The grab with its numbers fixed: the three slams', and the punch for what cannot be grabbed. */
    private static LivingEntity grab(ServerPlayer player, LivingEntity target, float[] slams, float punch) {
        ServerLevel level = player.serverLevel();
        if (HulkAbilities.grabImmune(target)) {
            float amount = punch;
            if (HulkAbilities.hurt(player, target, ModDamageTypes.HULK_GRAB, amount, player)) {
                HulkAbilities.push(target, player.position(), 0.6, 0.1, player);
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            HulkAbilities.sound(level, target.position(), ModSounds.HULK_SLAM.get(), 1.2F, 1.1F);
            HulkAbilities.send(level, player, HulkFxPayload.GRAB_SLAM, List.of(target.position()), 0);
            return target;
        }
        GrabSmash grab = new GrabSmash(target);
        grab.slams = slams;
        HulkAbilities.startMove(player, HulkAbilities.state(player.getUUID()), grab);
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }
        HulkAbilities.sound(level, player.position(), ModSounds.HULK_GRAB.get(), 1.2F, 1.0F);
        HulkAbilities.send(level, player, HulkFxPayload.GRAB_SLAM, List.of(target.position()), target.getId() + 1);
        return target;
    }

    @Override
    public Live live() {
        return Live.HULK_GRABBING;
    }

    /** Where the victim is held at tick {@code t}: swung from his left over his head to his right and back. */
    static Vec3 held(ServerPlayer player, int t) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double reach = 1.2 + player.getBbWidth();
        // The phase: -1 at his left on the ground, +1 at his right; between slams it swings over (high in the middle).
        double phase;
        if (t < SLAMS[0]) {
            phase = -t / (double) SLAMS[0];
        } else if (t < SLAMS[1]) {
            phase = -1.0 + 2.0 * (t - SLAMS[0]) / (SLAMS[1] - SLAMS[0]);
        } else {
            phase = 1.0 - 2.0 * (t - SLAMS[1]) / (SLAMS[2] - SLAMS[1]);
        }
        double lift = (1.0 - Math.abs(phase)) * player.getBbHeight() * 0.9;
        Vec3 side = right.scale(phase * reach).add(forward.scale(reach * (1.0 - Math.abs(phase) * 0.6)));
        return player.position().add(side).add(0, lift, 0);
    }

    @Override
    public boolean tick(ServerPlayer player, PowerData data, int t) {
        ServerLevel level = player.serverLevel();
        if (!victim.isAlive() || victim.level() != level || victim.distanceTo(player) > 8.0) {
            return true;
        }
        Vec3 at = held(player, Math.min(t, TICKS));
        victim.teleportTo(at.x, at.y, at.z);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.hurtMarked = true;
        victim.fallDistance = 0.0F;
        if (victim instanceof Player held) {
            held.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 6, false, false));
            held.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3, 4, false, false));
            held.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3, 4, false, false));
        }
        for (int i = 0; i < SLAMS.length; i++) {
            if (t == SLAMS[i]) {
                slam(player, data, i);
            }
        }
        return t >= TICKS;
    }

    /** One slam: the {@code i}th damage, the dust, the last one throws. */
    private void slam(ServerPlayer player, PowerData data, int i) {
        ServerLevel level = player.serverLevel();
        HulkAbilities.hurt(player, victim, ModDamageTypes.HULK_GRAB, slams[i], player);
        HulkAbilities.sound(level, victim.position(), ModSounds.HULK_SLAM.get(), 1.4F, 0.9F + 0.1F * i);
        HulkAbilities.send(level, player, HulkFxPayload.GRAB_SLAM, List.of(victim.position()), 0);
        if (i == SLAMS.length - 1) {
            release();
            HulkAbilities.push(victim, player.position(), THROW_PUSH, THROW_LIFT, player);
        }
    }

    private void release() {
        if (victim instanceof Mob mob && !wasNoAi) {
            mob.setNoAi(false);
        }
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        release();
    }

    LivingEntity victim() {
        return victim;
    }
}
