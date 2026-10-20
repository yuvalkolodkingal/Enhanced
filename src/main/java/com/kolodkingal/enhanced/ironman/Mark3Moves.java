package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Mark 3's moves (3.8, PLAN 5.7): the classic red and gold suit of the first film. R the Repulsor (an instant ray
 * to the first creature or block, a small boom where it lands; Sneak + R the Double-Hand Blast); V the Unibeam (held,
 * from the chest reactor); B the Flares (every mob after him loses him for a while; missiles chasing him chase the
 * flares); N the Shoulder Mini-Missiles (four, straight, to where he aims). The Repulsor and the Unibeam are shared
 * with the Mark 5 ({@link #repulsor}, {@link #startUnibeam}).
 */
public final class Mark3Moves implements SuitMoves {
    public static final Mark3Moves INSTANCE = new Mark3Moves();

    /** Flared mobs: who may not pick whom until when (game time). */
    private static final Map<UUID, Flared> FLARED = new HashMap<>();
    /** When each one's Unibeam began (-1 or absent: none). */
    private static final Map<UUID, Long> UNIBEAM = new HashMap<>();

    record Flared(UUID from, long until) {
    }

    private Mark3Moves() {
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        long now = player.level().getGameTime();
        if (!down) {
            if (ability == 3) {
                stopUnibeam(player);
            }
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    doubleBlast(player, data, now);
                } else {
                    repulsor(player, data, now, IronManAbilities.cfg(IronManConfig.MARK3_REPULSOR_DAMAGE, 6.0),
                            IronManAbilities.cfg(IronManConfig.MARK3_REPULSOR_POWER, 0.8),
                            IronManAbilities.cfg(IronManConfig.MARK3_REPULSOR_ENERGY, 4.0));
                }
            }
            case 3 -> startUnibeam(player, now);
            case 4 -> flares(player, data, now);
            case 5 -> missiles(player, data, now);
            default -> {
            }
        }
    }

    @Override
    public void tick(ServerPlayer player, PowerData data, long now) {
        unibeamTick(player, now, IronManAbilities.cfg(IronManConfig.MARK3_UNIBEAM_DAMAGE, 4.0),
                IronManAbilities.cfg(IronManConfig.MARK3_UNIBEAM_ENERGY_PER_SECOND, 6.0));
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        stopUnibeam(player);
    }

    // ---- R: the Repulsor and the Double-Hand Blast ----

    /**
     * A repulsor blast (the Mark 3's and the Mark 5's): an instant ray from the palm along his look ({@code
     * repulsorRange}) to the first creature or block: {@code damage} to the creature, a small boom of {@code power}
     * where it lands (to the others near). A gap of {@code repulsorGap} ticks between two.
     *
     * @return whether it fired
     */
    static boolean repulsor(ServerPlayer player, PowerData data, long now, double damage, double power, double energy) {
        if (!IronManAbilities.ready(data, 1, now) || !SuitEnergy.spend(player, energy)) {
            return false;
        }
        IronManAbilities.cooldown(data, 1, now, IronManAbilities.cfg(IronManConfig.MARK3_REPULSOR_GAP, 10));
        blast(player, damage, power, false);
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.REPULSOR_BLAST.get(), 1.0F,
                0.95F + player.getRandom().nextFloat() * 0.1F);
        return true;
    }

    private static void doubleBlast(ServerPlayer player, PowerData data, long now) {
        if (!IronManAbilities.ready(data, 1, now)
                || !SuitEnergy.spend(player, IronManAbilities.cfg(IronManConfig.MARK3_DOUBLE_BLAST_ENERGY, 8.0))) {
            return;
        }
        IronManAbilities.cooldown(data, 1, now, IronManAbilities.cfg(IronManConfig.MARK3_DOUBLE_BLAST_COOLDOWN, 40));
        blast(player, IronManAbilities.cfg(IronManConfig.MARK3_DOUBLE_BLAST_DAMAGE, 10.0),
                IronManAbilities.cfg(IronManConfig.MARK3_DOUBLE_BLAST_POWER, 1.2), true);
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.DOUBLE_BLAST.get(), 1.0F, 1.0F);
    }

    /** The ray itself: what it hits, the damage, the boom, the effect (from one palm or both). */
    private static void blast(ServerPlayer player, double damage, double power, boolean both) {
        ServerLevel level = player.serverLevel();
        double range = IronManAbilities.cfg(IronManConfig.MARK3_REPULSOR_RANGE, 32);
        HitResult hit = Raycasts.any(player, range, e -> e instanceof LivingEntity && ContinuousBeam.targets(player)
                .test(e), 1.0F);
        Vec3 end = hit.getLocation();
        LivingEntity struck = null;
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target instanceof PartEntity<?> part) {
                target = part.getParent();
            }
            if (target instanceof LivingEntity living
                    && IronManAbilities.hurt(player, living, ModDamageTypes.REPULSOR, damage, player)) {
                struck = living;
                IronManAbilities.push(player, living, player.position(), 0.4 * power);
            }
        }
        if (hit.getType() != HitResult.Type.MISS) {
            IronManAbilities.boom(player, end, power, IronMissileEntity.splashRadius(power),
                    IronMissileEntity.splashDamage(power), ModDamageTypes.REPULSOR, player, struck);
        }
        boolean left = !both && player.getRandom().nextBoolean();
        List<Vec3> points = new ArrayList<>();
        points.add(IronManAbilities.palm(player, left));
        points.add(end);
        if (both) {
            points.add(IronManAbilities.palm(player, true));
        }
        IronManAbilities.send(level, player, both ? IronFxPayload.DOUBLE_BLAST : IronFxPayload.REPULSOR, points,
                left ? 1 : 0);
    }

    // ---- V: the Unibeam ----

    /** The Unibeam begins (the Mark 3's and the Mark 5's): from the chest reactor, held. */
    static void startUnibeam(ServerPlayer player, long now) {
        if (UNIBEAM.containsKey(player.getUUID())) {
            return;
        }
        if (SuitEnergy.empty(player) && !player.isCreative()) {
            SuitEnergy.refuse(player);
            return;
        }
        UNIBEAM.put(player.getUUID(), now);
        ContinuousBeam.start(player, AvengersBeams.UNIBEAM,
                IronManAbilities.cfg(IronManConfig.MARK3_UNIBEAM_RANGE, 24));
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.UNIBEAM_START.get(), 1.0F, 1.0F);
    }

    /** A tick of a burning Unibeam: {@code damage} every 10 ticks, {@code perSecond} energy; it ends at its longest. */
    static void unibeamTick(ServerPlayer player, long now, double damage, double perSecond) {
        Long start = UNIBEAM.get(player.getUUID());
        if (start == null) {
            return;
        }
        int max = IronManAbilities.cfg(IronManConfig.MARK3_UNIBEAM_MAX_TICKS, 60);
        if (now - start >= max || !SuitEnergy.drain(player, perSecond)) {
            stopUnibeam(player);
            return;
        }
        if ((now - start) % 20 == 0) {
            IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.UNIBEAM.get(), 0.8F, 1.0F);
        }
        ContinuousBeam.tick(player, new BeamSpec(AvengersBeams.UNIBEAM,
                IronManAbilities.cfg(IronManConfig.MARK3_UNIBEAM_RANGE, 24), (float) damage, 10, 0.0F,
                ModDamageTypes.UNIBEAM, null));
    }

    static void stopUnibeam(ServerPlayer player) {
        if (UNIBEAM.remove(player.getUUID()) != null) {
            ContinuousBeam.stop(player);
        }
    }

    /** Whether his Unibeam burns. */
    public static boolean unibeaming(ServerPlayer player) {
        return UNIBEAM.containsKey(player.getUUID());
    }

    // ---- B: the Flares ----

    private static void flares(ServerPlayer player, PowerData data, long now) {
        if (!IronManAbilities.ready(data, 4, now)
                || !SuitEnergy.spend(player, IronManAbilities.cfg(IronManConfig.MARK3_FLARE_ENERGY, 10.0))) {
            return;
        }
        IronManAbilities.cooldown(data, 4, now, IronManAbilities.cfg(IronManConfig.MARK3_FLARE_COOLDOWN, 300));
        ServerLevel level = player.serverLevel();
        double radius = IronManAbilities.cfg(IronManConfig.MARK3_FLARE_RADIUS, 16.0);
        int ticks = IronManAbilities.cfg(IronManConfig.MARK3_FLARE_TICKS, 100);
        AABB box = player.getBoundingBox().inflate(radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m.getTarget() == player
                && m.distanceTo(player) <= radius)) {
            mob.setTarget(null);
            FLARED.put(mob.getUUID(), new Flared(player.getUUID(), now + ticks));
        }
        // The fan of flares: from the hips, out behind and to both sides, falling.
        List<Vec3> points = new ArrayList<>();
        double yaw = Math.toRadians(player.getYRot());
        for (int i = 0; i < 8; i++) {
            double a = yaw + Math.PI / 2 + (i - 3.5) * 0.35;
            Vec3 at = player.position().add(-Math.sin(a) * 3.0, 1.5 + (i % 3) * 0.6, Math.cos(a) * 3.0);
            points.add(at);
            level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
        Vec3 decoy = points.get(points.size() / 2);
        for (IronMissileEntity missile : level.getEntitiesOfClass(IronMissileEntity.class, box,
                m -> m.getOwner() != player)) {
            missile.decoy(decoy);
        }
        for (ShulkerBullet bullet : level.getEntitiesOfClass(ShulkerBullet.class, box)) {
            level.sendParticles(ParticleTypes.CRIT, bullet.getX(), bullet.getY(), bullet.getZ(), 6, 0.1, 0.1, 0.1,
                    0.1);
            bullet.discard();
        }
        IronManAbilities.sound(level, player.position(), ModSounds.FLARES.get(), 1.0F, 1.0F);
        IronManAbilities.send(level, player, IronFxPayload.FLARES, points, 0);
    }

    /** Whether a flare keeps {@code mob} from picking {@code target} now (the targeting event asks). */
    public static boolean flared(Mob mob, LivingEntity target) {
        Flared f = FLARED.get(mob.getUUID());
        if (f == null) {
            return false;
        }
        if (mob.level().getGameTime() >= f.until()) {
            FLARED.remove(mob.getUUID());
            return false;
        }
        return f.from().equals(target.getUUID());
    }

    // ---- N: the Shoulder Mini-Missiles ----

    private static void missiles(ServerPlayer player, PowerData data, long now) {
        if (!IronManAbilities.ready(data, 5, now)
                || !SuitEnergy.spend(player, IronManAbilities.cfg(IronManConfig.MARK3_MISSILE_ENERGY, 12.0))) {
            return;
        }
        IronManAbilities.cooldown(data, 5, now, IronManAbilities.cfg(IronManConfig.MARK3_MISSILE_COOLDOWN, 100));
        int count = IronManAbilities.cfg(IronManConfig.MARK3_MISSILE_COUNT, 4);
        float damage = (float) IronManAbilities.cfg(IronManConfig.MARK3_MISSILE_DAMAGE, 3.0);
        float power = (float) IronManAbilities.cfg(IronManConfig.MARK3_MISSILE_POWER, 0.6);
        Vec3 aim = aimPoint(player, 32.0);
        double yaw = Math.toRadians(player.getYRot());
        Vec3 side = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boolean left = i % 2 == 1;
            double out = (left ? -1 : 1) * (0.3 + 0.1 * (i / 2));
            Vec3 from = player.position().add(0, player.getBbHeight() * 0.9, 0).add(side.scale(out));
            // (Each one aims a little off the point, so they arrive as a small cluster, not one dot.)
            Vec3 target = aim.add(side.scale(out * 0.25)).add(0, (i / 2) * 0.15 - 0.1, 0);
            IronMissileEntity missile = IronMissileEntity.create(player.level(), player, IronMissileEntity.SHOULDER,
                    from, target.subtract(from), 1.8, damage, power, 40.0);
            player.level().addFreshEntity(missile);
            points.add(from);
        }
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.MINI_MISSILE_LAUNCH.get(), 1.0F,
                1.0F);
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.MISSILES, points, count);
    }

    /** Where he aims: the first creature or block along his look within {@code range}, or the end of it. */
    static Vec3 aimPoint(ServerPlayer player, double range) {
        HitResult hit = Raycasts.any(player, range, e -> e instanceof LivingEntity && ContinuousBeam.targets(player)
                .test(e), 1.0F);
        if (hit instanceof EntityHitResult entityHit) {
            Entity e = entityHit.getEntity();
            return e.position().add(0, e.getBbHeight() * 0.55, 0);
        }
        return hit.getLocation();
    }

    static void forget(UUID id) {
        UNIBEAM.remove(id);
    }

    static void clear() {
        UNIBEAM.clear();
        FLARED.clear();
    }
}
