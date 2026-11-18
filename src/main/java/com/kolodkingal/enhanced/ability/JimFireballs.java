package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Jim Hammond's fireballs (V): balls of fire he throws where he looks. Each flies on the server (a point with a velocity,
 * drawn by the flames and sparks the server sends), stops at the first block or creature, hurts and sets alight the creature
 * it hits (and, a little, the ones next to it), and leaves fire on the ground there: placed on the air, never with the game
 * rule {@code mobGriefing} off and only where the mod's block rules allow ({@link BeamFire}). Nothing is deleted.
 */
public final class JimFireballs {
    private static final class Ball {
        Vec3 pos;
        final Vec3 velocity;
        double flown;

        Ball(Vec3 pos, Vec3 velocity) {
            this.pos = pos;
            this.velocity = velocity;
        }
    }

    private static final Map<UUID, List<Ball>> BALLS = new HashMap<>();

    private JimFireballs() {
    }

    /** V: a fireball leaves his hand (a cooldown first). */
    static void launch(ServerPlayer player, PowerData data) {
        if (player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        if (data.cooldownUntil(data.slot(HeroAbility.JIM_FIREBALL)) > now && !player.getAbilities().instabuild) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(look.scale(0.7)).add(0, -0.25, 0);
        BALLS.computeIfAbsent(player.getUUID(), u -> new ArrayList<>()).add(new Ball(from, look.scale(JimConfig.FIREBALL_SPEED.get())));
        data.startCooldown(HeroAbility.JIM_FIREBALL, now, JimConfig.FIREBALL_COOLDOWN.get());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /** How many fireballs of this player are in the air (for the tests). */
    public static int inFlight(ServerPlayer player) {
        List<Ball> list = BALLS.get(player.getUUID());
        return list == null ? 0 : list.size();
    }

    static void tick(ServerPlayer player, PowerData data) {
        List<Ball> list = BALLS.get(player.getUUID());
        if (list == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Iterator<Ball> it = list.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            if (!step(player, level, ball)) {
                it.remove();
            }
        }
        if (list.isEmpty()) {
            BALLS.remove(player.getUUID());
        }
    }

    /** Moves one fireball a tick on (in two halves); false when it struck something or went out. */
    private static boolean step(ServerPlayer player, ServerLevel level, Ball ball) {
        for (int half = 0; half < 2; half++) {
            Vec3 from = ball.pos;
            Vec3 to = from.add(ball.velocity.scale(0.5));
            BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, from, stop, 1.0F);
            if (shield != null) {
                WandaAbilities.pay(shield, player, JimConfig.FIREBALL_DAMAGE.get().floatValue());
                burst(level, shield.point());
                return false;
            }
            EntityHitResult entity = Raycasts.entityBetween(level, player, from, stop, 0.3, WandaAbilities.BEAM_TARGET);
            if (entity != null) {
                strike(player, level, entity.getLocation(), entity);
                return false;
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                strike(player, level, block.getLocation(), block);
                return false;
            }
            ball.pos = to;
            ball.flown += ball.velocity.length() * 0.5;
            if (ball.flown > JimConfig.FIREBALL_RANGE.get()) {
                burst(level, ball.pos);
                return false;
            }
        }
        // the fireball itself: a core of flame, sparks falling away behind it
        Vec3 p = ball.pos;
        level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 4, 0.12, 0.12, 0.12, 0.01);
        level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 3, 0.2, 0.2, 0.2, 0.02);
        if (level.getGameTime() % 2 == 0) {
            level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
        return true;
    }

    private static void strike(ServerPlayer player, ServerLevel level, Vec3 at, HitResult hit) {
        float damage = JimConfig.FIREBALL_DAMAGE.get().floatValue();
        int burn = JimConfig.FIREBALL_BURN_SECONDS.get();
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (!(target instanceof LivingEntity living) || AreaTargets.mayHurt(player, living, true)) {
                target.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), damage);
                BeamFire.ignite(target, burn);
            }
        }
        // the flames splash a little: what stands next to it catches too
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.5),
                e -> e != player && e.isAlive() && !(hit instanceof EntityHitResult h && h.getEntity() == e))) {
            if (AreaTargets.mayHurt(player, e, true)) {
                e.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), damage * 0.4F);
                BeamFire.ignite(e, Math.max(1, burn / 2));
            }
        }
        if (JimConfig.FIREBALL_FIRE.get()) {
            if (hit instanceof BlockHitResult) {
                BeamFire.strike(player, level, hit, 0, 2);
            } else {
                // a creature standing on the ground: the fire goes under it, where it stands
                BlockHitResult floor = level.clip(new ClipContext(at.add(0, 0.2, 0), at.add(0, -2.0, 0), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                if (floor.getType() == HitResult.Type.BLOCK) {
                    BeamFire.strike(player, level, floor, 0, 0);
                }
            }
        }
        burst(level, at);
    }

    private static void burst(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.08);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    static void forget(UUID player) {
        BALLS.remove(player);
    }

    /** The server stopped: no fireball is left. */
    public static void clear() {
        BALLS.clear();
    }
}
