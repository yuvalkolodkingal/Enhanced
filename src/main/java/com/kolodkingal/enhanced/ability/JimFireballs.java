package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
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
        /** 0 for a quick throw, 1 fully charged. */
        final float charge;
        /** Its body in the world, so every client draws it where it is (3.17). */
        final com.kolodkingal.enhanced.entity.JimFireballEntity body;

        Ball(Vec3 pos, Vec3 velocity, float charge, com.kolodkingal.enhanced.entity.JimFireballEntity body) {
            this.pos = pos;
            this.velocity = velocity;
            this.charge = charge;
            this.body = body;
        }

        void out() {
            if (body != null) {
                body.discard();
            }
        }
    }

    /** When each player began to charge a fireball (V held). */
    private static final Map<UUID, Long> CHARGING = new HashMap<>();
    /** The charge (0 to 1) of the ball each player is throwing now. */
    private static final Map<UUID, Float> THROW_CHARGE = new HashMap<>();

    private static final Map<UUID, List<Ball>> BALLS = new HashMap<>();
    /** When each player began a throw (the ball is released {@link #THROW_DELAY} ticks later). */
    private static final Map<UUID, Long> THROWN_AT = new HashMap<>();
    private static final Map<UUID, Integer> PULSE_OFF = new HashMap<>();
    /** Ticks from the press to the release of the ball (the wind-up of the throw). */
    public static final int THROW_DELAY = 4;
    /** How long (ticks) the throw's state stays on after the release, for every client to see it begin and play the gesture. */
    private static final int PULSE_TICKS = 4;

    private JimFireballs() {
    }

    /** V pressed: he starts to charge a fireball in his hand (a cooldown first); let go, and it is thrown. */
    static void onKey(ServerPlayer player, PowerData data, boolean down) {
        if (player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        if (down) {
            if (!data.is(Live.JIM_FIREBALL_CHARGING)) {
                CHARGING.remove(player.getUUID()); // (a charge cut short: a screen opened while V was held)
            }
            if (CHARGING.containsKey(player.getUUID()) || THROWN_AT.containsKey(player.getUUID())) {
                return;
            }
            if (data.cooldownUntil(data.slot(HeroAbility.JIM_FIREBALL)) > now && !player.getAbilities().instabuild) {
                return;
            }
            CHARGING.put(player.getUUID(), now);
            data.set(Live.JIM_FIREBALL_CHARGING, true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.6F);
            return;
        }
        Long began = CHARGING.remove(player.getUUID());
        data.set(Live.JIM_FIREBALL_CHARGING, false);
        if (began == null) {
            return;
        }
        launch(player, data, charge(now - began));
    }

    /** The charge (0 to 1) after holding V for {@code ticks}. */
    public static float charge(long ticks) {
        return Math.min(1.0F, Math.max(0.0F, ticks / (float) Math.max(1, JimConfig.FIREBALL_CHARGE_TICKS.get())));
    }

    /** How big a fireball of this charge is (its body and its flames). */
    public static float size(float charge) {
        return 1.0F + 1.2F * charge;
    }

    /** The throw itself: the ball leaves his hand after the wind-up, as big and strong as it was charged. */
    static void launch(ServerPlayer player, PowerData data, float charge) {
        long now = player.level().getGameTime();
        if (!JimSolar.spend(player, data, Math.round(JimConfig.FIREBALL_COST.get() * (1.0F + charge)))) {
            return;
        }
        THROW_CHARGE.put(player.getUUID(), charge);
        // the throw: every client plays the wind-up now, and the ball leaves his hand at the release, THROW_DELAY ticks on
        data.set(Live.JIM_THROW, true);
        THROWN_AT.put(player.getUUID(), now);
        data.startCooldown(HeroAbility.JIM_FIREBALL, now, JimConfig.FIREBALL_COOLDOWN.get());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    /** The release: the ball leaves where he looks now. */
    private static void release(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(look.scale(0.7)).add(0, -0.25, 0);
        float charge = THROW_CHARGE.getOrDefault(player.getUUID(), 0.0F);
        THROW_CHARGE.remove(player.getUUID());
        Vec3 velocity = look.scale(JimConfig.FIREBALL_SPEED.get());
        com.kolodkingal.enhanced.entity.JimFireballEntity body = new com.kolodkingal.enhanced.entity.JimFireballEntity(
                com.kolodkingal.enhanced.registry.ModEntities.JIM_FIREBALL.get(), player.level());
        body.setOwner(player);
        body.setSize(size(charge));
        body.follow(from, velocity);
        player.level().addFreshEntity(body);
        BALLS.computeIfAbsent(player.getUUID(), u -> new ArrayList<>()).add(new Ball(from, velocity, charge, body));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /** How many fireballs of this player are in the air (for the tests). */
    public static int inFlight(ServerPlayer player) {
        List<Ball> list = BALLS.get(player.getUUID());
        return list == null ? 0 : list.size();
    }

    static void tick(ServerPlayer player, PowerData data) {
        Long thrown = THROWN_AT.get(player.getUUID());
        if (thrown != null) {
            long age = player.level().getGameTime() - thrown;
            if (!player.isAlive() || player.isSpectator()) {
                THROWN_AT.remove(player.getUUID());
                data.set(Live.JIM_THROW, false);
            } else if (age >= THROW_DELAY) {
                THROWN_AT.remove(player.getUUID());
                PULSE_OFF.put(player.getUUID(), 0);
                release(player);
            }
        }
        Integer shown = PULSE_OFF.get(player.getUUID());
        if (shown != null) {
            if (shown + 1 >= PULSE_TICKS) {
                PULSE_OFF.remove(player.getUUID());
                data.set(Live.JIM_THROW, false);
            } else {
                PULSE_OFF.put(player.getUUID(), shown + 1);
            }
        } else if (thrown == null && data.is(Live.JIM_THROW)) {
            data.set(Live.JIM_THROW, false); // (a stale state)
        }
        List<Ball> list = BALLS.get(player.getUUID());
        if (list == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Iterator<Ball> it = list.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            // 3.18.0: in slowed time a fireball moves on every Nth tick only, a whole tick's way (so it hits as hard), on
            // its body's beat, so the clients' copy of the body (tick-skipped the same way) keeps up with it
            if (SlowMotion.skipsPoint(level, ball.pos, ball.body != null ? ball.body.getId() : System.identityHashCode(ball))) {
                continue;
            }
            if (!step(player, level, ball)) {
                ball.out();
                it.remove();
            } else if (ball.body != null) {
                ball.body.follow(ball.pos, ball.velocity);
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
                strike(player, level, entity.getLocation(), entity, ball.charge);
                return false;
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                strike(player, level, block.getLocation(), block, ball.charge);
                return false;
            }
            ball.pos = to;
            ball.flown += ball.velocity.length() * 0.5;
            if (ball.flown > JimConfig.FIREBALL_RANGE.get()) {
                burst(level, ball.pos);
                return false;
            }
        }
        return true;
    }

    private static void strike(ServerPlayer player, ServerLevel level, Vec3 at, HitResult hit, float charge) {
        // a charged one hits up to two and a half times as hard and splashes twice as wide
        float damage = JimConfig.FIREBALL_DAMAGE.get().floatValue() * (1.0F + 1.5F * charge);
        int burn = JimConfig.FIREBALL_BURN_SECONDS.get();
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (!(target instanceof LivingEntity living) || AreaTargets.mayHurt(player, living, true)) {
                target.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), damage);
                BeamFire.ignite(target, burn);
            }
        }
        // the flames splash a little: what stands next to it catches too
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.5 + 1.5 * charge),
                e -> e != player && e.isAlive() && !(hit instanceof EntityHitResult h && h.getEntity() == e))) {
            if (AreaTargets.mayHurt(player, e, true)) {
                e.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), damage * 0.4F);
                BeamFire.ignite(e, Math.max(1, burn / 2));
            }
        }
        if (JimConfig.FIREBALL_FIRE.get()) {
            if (hit instanceof BlockHitResult) {
                BeamFire.strike(player, level, hit, 0, 2 + Math.round(3 * charge));
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
        Embers.burst(level, at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.08);
        Embers.burst(level, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    static void forget(UUID player) {
        List<Ball> list = BALLS.remove(player);
        if (list != null) {
            list.forEach(Ball::out);
        }
        CHARGING.remove(player);
        THROW_CHARGE.remove(player);
        THROWN_AT.remove(player);
        PULSE_OFF.remove(player);
    }

    /** The server stopped: no fireball is left. */
    public static void clear() {
        BALLS.values().forEach(list -> list.forEach(Ball::out));
        BALLS.clear();
        CHARGING.clear();
        THROW_CHARGE.clear();
        THROWN_AT.clear();
        PULSE_OFF.clear();
    }
}
