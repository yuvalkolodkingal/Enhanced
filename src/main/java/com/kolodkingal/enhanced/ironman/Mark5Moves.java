package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Mark 5's moves (3.8, PLAN 5.7): the suitcase suit of Monaco, silver and red, light. R the Repulsor and V the
 * Unibeam (the Mark 3's, a little weaker); B the Dash Punch (a thruster dash; the first creature in the way is punched
 * and thrown); N the Laser Burst (a thin laser ring sweeps once round him at chest height and cuts every foe in it).
 */
public final class Mark5Moves implements SuitMoves {
    public static final Mark5Moves INSTANCE = new Mark5Moves();

    /** The boom of its repulsor (PLAN 5.7: 0.6; no option of its own). */
    static final double REPULSOR_POWER = 0.6;
    /** The Dash Punch's length in ticks (its distance is covered evenly). */
    public static final int DASH_TICKS = 6;
    /** How close (blocks) a creature must be to his path to be punched. */
    static final double DASH_REACH = 1.2;

    private static final Map<UUID, Dash> DASHES = new HashMap<>();

    static final class Dash {
        final long start;
        final Vec3 direction;
        final double step;
        boolean struck;

        Dash(long start, Vec3 direction, double step) {
            this.start = start;
            this.direction = direction;
            this.step = step;
        }
    }

    private Mark5Moves() {
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        long now = player.level().getGameTime();
        if (!down) {
            if (ability == 3) {
                Mark3Moves.stopUnibeam(player);
            }
            return;
        }
        switch (ability) {
            case 1 -> Mark3Moves.repulsor(player, data, now,
                    IronManAbilities.cfg(IronManConfig.MARK5_REPULSOR_DAMAGE, 5.0),
                    REPULSOR_POWER,
                    IronManAbilities.cfg(IronManConfig.MARK5_REPULSOR_ENERGY, 3.0));
            case 3 -> Mark3Moves.startUnibeam(player, now);
            case 4 -> dash(player, data, now);
            case 5 -> laserBurst(player, data, now);
            default -> {
            }
        }
    }

    @Override
    public void tick(ServerPlayer player, PowerData data, long now) {
        Mark3Moves.unibeamTick(player, now, IronManAbilities.cfg(IronManConfig.MARK5_UNIBEAM_DAMAGE, 3.0),
                IronManAbilities.cfg(IronManConfig.MARK5_UNIBEAM_ENERGY_PER_SECOND, 5.0));
        Dash dash = DASHES.get(player.getUUID());
        if (dash != null) {
            dashTick(player, data, dash, now);
        }
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        Mark3Moves.stopUnibeam(player);
        if (DASHES.remove(player.getUUID()) != null) {
            data.set(Live.IRON_DASHING, false);
        }
    }

    // ---- B: the Dash Punch ----

    private static void dash(ServerPlayer player, PowerData data, long now) {
        if (DASHES.containsKey(player.getUUID()) || !IronManAbilities.ready(data, 4, now)
                || !SuitEnergy.spend(player, IronManAbilities.cfg(IronManConfig.MARK5_DASH_ENERGY, 10.0))) {
            return;
        }
        IronManAbilities.cooldown(data, 4, now, IronManAbilities.cfg(IronManConfig.MARK5_DASH_COOLDOWN, 60));
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, Math.max(-0.4, Math.min(0.4, look.y)), look.z).normalize();
        double distance = IronManAbilities.cfg(IronManConfig.MARK5_DASH_DISTANCE, 6.0);
        DASHES.put(player.getUUID(), new Dash(now, flat, distance / DASH_TICKS));
        data.set(Live.IRON_DASHING, true);
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.DASH_PUNCH.get(), 1.0F, 1.0F);
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.DASH, List.of(player.position()), 0);
    }

    /** A tick of the dash: a step along it (stopped by a wall), the first creature in the way punched. */
    private static void dashTick(ServerPlayer player, PowerData data, Dash dash, long now) {
        ServerLevel level = player.serverLevel();
        if (!dash.struck) {
            LivingEntity target = inPath(player, dash);
            if (target != null) {
                dash.struck = true;
                if (IronManAbilities.hurt(player, target, DamageTypes.PLAYER_ATTACK,
                        IronManAbilities.cfg(IronManConfig.MARK5_DASH_DAMAGE, 8.0), player)) {
                    IronManAbilities.push(player, target, player.position(), 1.6);
                }
                IronManAbilities.send(level, player, IronFxPayload.PUNCH, List.of(IronManAbilities.palm(player, false),
                        target.position().add(0, target.getBbHeight() * 0.6, 0)), 1);
                finishDash(player, data);
                return;
            }
        }
        Vec3 step = dash.direction.scale(dash.step);
        if (!level.noCollision(player, player.getBoundingBox().move(step))) {
            // (Aimed down at a foe on the ground: the dash skims along the ground instead of into it.)
            step = new Vec3(step.x, Math.max(0.0, step.y), step.z);
        }
        if (level.noCollision(player, player.getBoundingBox().move(step))) {
            player.teleportTo(player.getX() + step.x, player.getY() + step.y, player.getZ() + step.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 3, 0.1, 0.05,
                    0.1, 0.01);
        } else {
            finishDash(player, data);
            return;
        }
        if (now - dash.start >= DASH_TICKS - 1) {
            finishDash(player, data);
        }
    }

    private static void finishDash(ServerPlayer player, PowerData data) {
        DASHES.remove(player.getUUID());
        data.set(Live.IRON_DASHING, false);
    }

    /** The first creature he may hurt within reach of the next step of his path. */
    private static LivingEntity inPath(ServerPlayer player, Dash dash) {
        Vec3 from = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 to = from.add(dash.direction.scale(dash.step + DASH_REACH));
        AABB box = new AABB(from, to).inflate(DASH_REACH);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player
                && e.isAlive() && com.kolodkingal.enhanced.ability.AreaTargets.mayHurt(player, e, false))) {
            double d = e.getBoundingBox().distanceToSqr(from);
            if (e.getBoundingBox().inflate(DASH_REACH * 0.5).clip(from, to).isPresent() && d < bestDistance) {
                best = e;
                bestDistance = d;
            }
        }
        return best;
    }

    /** Whether he dashes now. */
    public static boolean dashing(ServerPlayer player) {
        return DASHES.containsKey(player.getUUID());
    }

    // ---- N: the Laser Burst ----

    private static void laserBurst(ServerPlayer player, PowerData data, long now) {
        if (!IronManAbilities.ready(data, 5, now)
                || !SuitEnergy.spend(player, IronManAbilities.cfg(IronManConfig.MARK5_LASER_BURST_ENERGY, 20.0))) {
            return;
        }
        IronManAbilities.cooldown(data, 5, now, IronManAbilities.cfg(IronManConfig.MARK5_LASER_BURST_COOLDOWN, 200));
        ServerLevel level = player.serverLevel();
        double radius = IronManAbilities.cfg(IronManConfig.MARK5_LASER_BURST_RADIUS, 6.0);
        double damage = IronManAbilities.cfg(IronManConfig.MARK5_LASER_BURST_DAMAGE, 6.0);
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.65, 0);
        List<Vec3> points = new ArrayList<>();
        points.add(centre);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive())) {
            if (e.distanceTo(player) <= radius && player.hasLineOfSight(e)
                    && IronManAbilities.hurt(player, e, ModDamageTypes.UNIBEAM, damage, player)) {
                if (points.size() < IronFxPayload.MAX_POINTS) {
                    points.add(e.position().add(0, e.getBbHeight() * 0.6, 0));
                }
            }
        }
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.2F), 0.8F);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            level.sendParticles(red, centre.x + Math.cos(a) * radius, centre.y, centre.z + Math.sin(a) * radius, 1, 0,
                    0, 0, 0);
        }
        IronManAbilities.sound(level, centre, ModSounds.LASER_BURST.get(), 1.0F, 1.0F);
        IronManAbilities.send(level, player, IronFxPayload.LASER_BURST, points, (int) Math.round(radius));
    }

    static void forget(UUID id) {
        DASHES.remove(id);
    }

    static void clear() {
        DASHES.clear();
    }
}
