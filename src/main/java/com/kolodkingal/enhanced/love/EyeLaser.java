package com.kolodkingal.enhanced.love;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.BeamPayload;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Love's Eye Laser (3.8, PLAN 10.6; B, held): a purple beam from her eyes along her view, through every creature to the
 * first block (or a Chaos Shield) that stops it. Every creature it touches takes {@code [love] eyeLaserDamage} (6:
 * three hearts, {@code love_eye_laser}, energy damage) at most once every {@code eyeLaserHitGap} ticks each, and is set
 * burning for {@code eyeLaserFireSeconds}; blocks never catch fire. It burns for at most {@code eyeLaserMaxTicks}, then
 * (or when B comes up) rests for {@code eyeLaserCooldown}. While it burns {@link Live#BEAMING} is on and the clients
 * draw the {@code love_eyes} beam ({@link BeamPayload}, kept alive every few ticks) and her violet eyes ({@link
 * EternityFxPayload#EYES_ON}).
 */
public final class EyeLaser {
    /** Ticks between the payloads that say the beam goes on. */
    static final int KEEP_ALIVE = 8;

    private static final class State {
        long started;
        long lastSent = Long.MIN_VALUE / 2;
        /** Creature id → when the beam last hurt it. */
        final Map<Integer, Long> hit = new HashMap<>();
    }

    private static final Map<UUID, State> FIRING = new HashMap<>();

    /** Whether her laser burns now. */
    public static boolean firing(ServerPlayer player) {
        return FIRING.containsKey(player.getUUID());
    }

    /** B went down (the laser starts, when it is ready) or up (it stops and rests). */
    static void key(ServerPlayer player, PowerData data, boolean down) {
        if (!down) {
            stop(player, data, true);
            return;
        }
        if (firing(player) || player.isSpectator() || !player.isAlive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.LOVE_EYE_LASER, now)) {
            return;
        }
        State state = new State();
        state.started = now;
        FIRING.put(player.getUUID(), state);
        data.set(Live.BEAMING, true);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.LOVE_EYE_LASER_START.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 96,
                new EternityFxPayload(EternityFxPayload.EYES_ON, player.getId(), player.blockPosition(), 0));
        send(player, true);
        state.lastSent = now;
    }

    /** The laser ends; with {@code rest}, its cooldown starts (not when the power is taken away). */
    static void stop(ServerPlayer player, PowerData data, boolean rest) {
        State state = FIRING.remove(player.getUUID());
        data.set(Live.BEAMING, false);
        if (state == null) {
            return;
        }
        if (rest) {
            data.startCooldown(HeroAbility.LOVE_EYE_LASER, player.level().getGameTime(),
                    LoveAbilities.cfg(EternityConfig.LOVE_EYE_LASER_COOLDOWN, 60));
        }
        send(player, false);
        PacketDistributor.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(), player.getZ(), 96,
                new EternityFxPayload(EternityFxPayload.EYES_OFF, player.getId(), player.blockPosition(), 0));
    }

    /** Every tick while it burns: who it touches is hurt and set alight; it ends at its longest. */
    static void tick(ServerPlayer player, PowerData data) {
        State state = FIRING.get(player.getUUID());
        if (state == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.has(HeroAbility.LOVE_EYE_LASER) || player.isSpectator() || !player.isAlive()) {
            stop(player, data, false);
            return;
        }
        if (now - state.started >= LoveAbilities.cfg(EternityConfig.LOVE_EYE_LASER_MAX_TICKS, 100)) {
            stop(player, data, true);
            player.displayClientMessage(Component.translatable("message.enhanced.love.eyes_tired")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        data.set(Live.BEAMING, true);
        if (now - state.lastSent >= KEEP_ALIVE) {
            send(player, true);
            state.lastSent = now;
        }
        if ((now - state.started) % 20 == 0) {
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.LOVE_EYE_LASER.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        fire(player, state, now);
    }

    /** One tick of the beam: along her view to the first block (or shield), hurting what it touches. */
    private static void fire(ServerPlayer player, State state, long now) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.getEyePosition();
        double range = LoveAbilities.cfg(EternityConfig.LOVE_EYE_LASER_RANGE, 24);
        Vec3 to = from.add(player.getLookAngle().scale(range));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, from, end, 1.0F);
        if (shield != null) {
            end = shield.point();
        }
        float damage = (float) LoveAbilities.cfg(EternityConfig.LOVE_EYE_LASER_DAMAGE, 6.0);
        int gap = LoveAbilities.cfg(EternityConfig.LOVE_EYE_LASER_HIT_GAP, 20);
        int fire = LoveAbilities.cfg(EternityConfig.LOVE_EYE_LASER_FIRE_SECONDS, 4);
        AABB sweep = new AABB(from, end).inflate(0.6);
        List<LivingEntity> touched = level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player
                && e.isAlive() && !e.isSpectator() && !player.isPassengerOfSameVehicle(e));
        for (LivingEntity target : touched) {
            AABB box = target.getBoundingBox().inflate(0.25);
            if (!box.contains(from) && box.clip(from, end).isEmpty()) {
                continue;
            }
            Long last = state.hit.get(target.getId());
            if (last != null && now - last < gap && now >= last) {
                continue;
            }
            if (!AreaTargets.mayHurt(player, target, true)) {
                continue;
            }
            state.hit.put(target.getId(), now);
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.LOVE_EYE_LASER, player, player), damage)
                    && fire > 0) {
                target.igniteForSeconds(fire);
            }
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
            level.sendParticles(new EnergyParticleOptions(AvengersBeams.LOVE_EYES.main(), 1.1F), at.x, at.y, at.z, 6,
                    0.15, 0.2, 0.15, 0.08);
            level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 3, 0.15, 0.2, 0.15, 0.02);
        }
        if (shield != null) {
            if (now % gap == 0) {
                WandaAbilities.pay(shield, player, damage);
            }
        }
        if (wall.getType() != HitResult.Type.MISS && now % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(AvengersBeams.LOVE_EYES.main(), 1.0F), end.x, end.y, end.z,
                    2, 0.08, 0.08, 0.08, 0.05);
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private static void send(ServerPlayer player, boolean on) {
        PacketDistributor.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(), player.getZ(),
                96.0, new BeamPayload(player.getId(), AvengersBeams.LOVE_EYES.id(),
                        (float) LoveAbilities.cfg(EternityConfig.LOVE_EYE_LASER_RANGE, 24), on));
    }

    /** The ticks the laser has burned (for the tests), or -1. */
    public static long burnedTicks(ServerPlayer player) {
        State state = FIRING.get(player.getUUID());
        return state == null ? -1 : player.level().getGameTime() - state.started;
    }

    static void forget(Entity player) {
        FIRING.remove(player.getUUID());
    }

    static void clear() {
        FIRING.clear();
    }

    private EyeLaser() {
    }
}
