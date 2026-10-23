package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SpeedConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The speed strikes of 3.3: attacks any speedster can make, written as modules so that a hero only says which of them
 * he has, on which key, in which colours (Quicksilver and Tommy now; a custom speed hero may call them too). Each
 * returns what it did, so the caller starts the cooldown only when it worked and says why it did not.
 * <ul>
 *     <li>{@link #dash}: a straight dash along the look that strikes everything in its path.</li>
 *     <li>{@link #barrage}: pebbles scooped up and flung at super speed, a narrow cone of hits.</li>
 *     <li>{@link #sonicBoom}: he stops dead and the air he pushed breaks as a shockwave.</li>
 * </ul>
 * Creatures are hit; a player only when PvP lets the striker harm him (the game's own rule). Bosses take the hits as
 * any creature. The damage is the Speed Blitz's.
 */
public final class SpeedStrikes {
    /** How long (ticks) the Sonic Boom's flag ({@link Live#SONIC_BOOM}) stays up for every client to act it. */
    public static final int BOOM_FLAG_TICKS = 6;
    /** Half the angle (degrees) of the cone the pebbles of a barrage spread over. */
    public static final double BARRAGE_SPREAD = 5.0;

    /** The colours a strike is drawn in: the hero's deep colour and his glow (0xRRGGBB). */
    public record Style(int color, int glow) {
        public static Style of(Power power) {
            return new Style(power.color, power.glow);
        }

        /**
         * 3.4: the colours of {@code hero}'s strike as this player makes it: the hero's own, or a custom hero's two colours
         * when he makes it with one of the hero's abilities.
         */
        public static Style of(Player player, Power hero) {
            return Powers.get(player).power() == hero ? of(hero) : new Style(Powers.color(player), Powers.glow(player));
        }

        /**
         * 3.5: the colours of one ability's strike: the colour its pick chose (and its glow), else {@link #of(Player,
         * Power)}.
         */
        public static Style of(Player player, Power hero, HeroAbility row) {
            int chosen = AbilityColors.chosen(player, row);
            return chosen < 0 ? of(player, hero) : new Style(chosen, AbilityColors.glowOf(chosen));
        }
    }

    /** When each striker's Sonic Boom flag comes down (game time). */
    private static final Map<UUID, Long> BOOMS = new HashMap<>();

    // ---- Dash Strike ----

    /** What a dash did: how far it carried him (0: no room) and whom it struck. */
    public record DashResult(double distance, List<LivingEntity> struck) {
        public boolean moved() {
            return distance > 0.0;
        }
    }

    /**
     * Dashes him straight along his look (kept level: a dash runs, it does not fly), up to {@code range} blocks, and stops
     * at the first block his body would run into (his box is swept along the path a quarter of a block at a time, so it
     * never ends inside a block) or at a Hex wall he may not cross. Every creature his path crosses takes {@code damage}
     * and is knocked aside off the path. Nothing happens (distance 0) when there is no room for a block of dash.
     */
    public static DashResult dash(ServerPlayer player, Style style, double range, float damage) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0.0, look.z);
        if (dir.lengthSqr() < 1.0E-4) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            dir = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        }
        dir = dir.normalize();
        Vec3 start = player.position();
        AABB box = player.getBoundingBox();
        double travelled = 0.0;
        for (double d = 0.25; d <= range + 1.0E-6; d += 0.25) {
            if (!level.noCollision(player, box.move(dir.scale(d)))) {
                break;
            }
            travelled = d;
        }
        if (travelled < 1.0) {
            return new DashResult(0.0, List.of());
        }
        Vec3 end = start.add(dir.scale(travelled));
        if (!HexManager.onTeleport(level, player, end)) {
            return new DashResult(0.0, List.of());
        }
        Set<LivingEntity> path = new LinkedHashSet<>();
        AABB swept = box.expandTowards(dir.scale(travelled)).inflate(0.4);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, swept, e -> e != player && e.isAlive() && !e.isSpectator())) {
            // Only what the path really crosses (the swept box of a diagonal dash is wider than the path).
            if (target.getBoundingBox().inflate(0.5).clip(start.add(0, 0.9, 0), end.add(0, 0.9, 0)).isPresent()
                    || target.getBoundingBox().inflate(0.5).contains(start.add(0, 0.9, 0))) {
                path.add(target);
            }
        }
        player.teleportTo(level, end.x, end.y, end.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        player.setDeltaMovement(dir.scale(0.4));
        player.hurtMarked = true;
        List<LivingEntity> struck = new ArrayList<>();
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
        for (LivingEntity target : path) {
            if (!mayHit(player, target)) {
                continue;
            }
            target.invulnerableTime = 0;
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.SPEED_BLITZ, player, player), damage)) {
                struck.add(target);
            }
            // Knocked off the path, to the side it stood on.
            Vec3 off = target.position().subtract(start);
            double sign = off.dot(side) >= 0.0 ? 1.0 : -1.0;
            Vec3 push = side.scale(sign);
            target.knockback(0.9, -push.x, -push.z);
        }
        streak(level, style, start, end);
        level.playSound(null, start.x, start.y, start.z, ModSounds.QUICKSILVER_DASH.get(), SoundSource.PLAYERS, 1.0F, 1.1F);
        level.playSound(null, end.x, end.y, end.z, ModSounds.QUICKSILVER_WHOOSH.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
        if (!struck.isEmpty()) {
            level.playSound(null, end.x, end.y, end.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return new DashResult(travelled, struck);
    }

    // ---- Pebble Barrage ----

    /**
     * Flings {@code pebbles} pebbles at super speed from his eyes along his look, spread evenly over a narrow cone
     * ({@link #BARRAGE_SPREAD} degrees; the same pattern every time). Each hits the first creature or block in its way
     * within {@code range}; a creature takes {@code damage} for every pebble (the hit cooldown is skipped, so they add
     * up) and a small push. Returns the number of pebbles that hit a creature.
     */
    public static int barrage(ServerPlayer player, Style style, int pebbles, float damage, double range) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Two axes across the look, to spread the pebbles around it.
        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 a = look.cross(up).normalize();
        Vec3 b = a.cross(look).normalize();
        double spread = Math.tan(BARRAGE_SPREAD * Mth.DEG_TO_RAD);
        int hits = 0;
        EnergyParticleOptions trail = new EnergyParticleOptions(style.glow(), 0.6F);
        for (int i = 0; i < pebbles; i++) {
            // A sunflower pattern: even over the disc, the first pebble dead on the look.
            double r = spread * Math.sqrt(i / (double) Math.max(1, pebbles - 1));
            double angle = i * 2.39996;
            Vec3 dir = look.add(a.scale(r * Math.cos(angle))).add(b.scale(r * Math.sin(angle))).normalize();
            Vec3 end = eye.add(dir.scale(range));
            BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            EntityHitResult hit = Raycasts.entityBetween(level, player, eye, stop, 0.2,
                    e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator() && e != player);
            if (hit != null) {
                stop = hit.getLocation();
                LivingEntity target = (LivingEntity) hit.getEntity();
                if (mayHit(player, target)) {
                    target.invulnerableTime = 0;
                    if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.SPEED_BLITZ, player, player), damage)) {
                        hits++;
                    }
                    target.knockback(0.15, -dir.x, -dir.z);
                }
            }
            line(level, trail, eye.add(dir.scale(0.8)), stop, 0.7);
            level.sendParticles(ParticleTypes.CRIT, stop.x, stop.y, stop.z, 2, 0.05, 0.05, 0.05, 0.05);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUICKSILVER_WHOOSH.get(), SoundSource.PLAYERS, 0.6F, 1.6F);
        return hits;
    }

    // ---- Sonic Boom ----

    /**
     * He stops dead, and the air he pushed ahead of him breaks as a shockwave: every creature within {@code radius} takes
     * {@code damage} (half as much again if he was running at super speed, less towards the edge: the full amount at his
     * side, half at the edge) and is thrown away from him and a little up; arrows and thrown things in it are blown away.
     * Raises {@link Live#SONIC_BOOM} for a moment so every client acts it. Returns the creatures it hit.
     */
    public static List<LivingEntity> sonicBoom(ServerPlayer player, PowerData data, Style style, double radius, float damage) {
        ServerLevel level = player.serverLevel();
        boolean running = data.is(Live.SPEEDING) || data.is(Live.SURFACE_WALL) || data.is(Live.SURFACE_CEILING);
        float full = damage * (running ? 1.5F : 1.0F);
        Vec3 center = player.position().add(0.0, 0.9, 0.0);
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            double dist = target.position().add(0.0, target.getBbHeight() / 2.0, 0.0).distanceTo(center);
            if (dist > radius || !mayHit(player, target)) {
                continue;
            }
            float falloff = (float) (1.0 - 0.5 * dist / radius);
            target.invulnerableTime = 0;
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.SPEED_BLITZ, player, player), full * falloff)) {
                hit.add(target);
            }
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
            target.knockback(1.4 * falloff, -away.x, -away.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.35 * falloff, 0.0));
            target.hurtMarked = true;
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius),
                p -> p.position().distanceTo(center) <= radius)) {
            Vec3 away = projectile.position().subtract(center);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize();
            projectile.setDeltaMovement(away.scale(Math.max(0.6, projectile.getDeltaMovement().length())));
            projectile.hasImpulse = true;
        }
        // He stops dead: that is what makes the boom.
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        data.set(Live.SONIC_BOOM, true);
        BOOMS.put(player.getUUID(), level.getGameTime() + BOOM_FLAG_TICKS);
        rings(level, style, center, radius);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4F, 1.35F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        return hit;
    }

    /** Once per tick of a speedster: the Sonic Boom's flag comes down after a moment. */
    public static void tick(ServerPlayer player, PowerData data) {
        Long until = BOOMS.get(player.getUUID());
        if (until != null && player.serverLevel().getGameTime() >= until) {
            BOOMS.remove(player.getUUID());
            data.set(Live.SONIC_BOOM, false);
        } else if (until == null && data.is(Live.SONIC_BOOM)) {
            data.set(Live.SONIC_BOOM, false);
        }
    }

    public static void end(Player player, PowerData data) {
        BOOMS.remove(player.getUUID());
        data.set(Live.SONIC_BOOM, false);
    }

    public static void clear() {
        BOOMS.clear();
    }

    // ---- rules ----

    /** Whether he may strike this creature: anything alive, a player only if PvP (and the teams) let him. */
    public static boolean mayHit(ServerPlayer striker, LivingEntity target) {
        return !(target instanceof Player other) || (striker.canHarmPlayer(other) && !other.isCreative());
    }

    /** A config number, or its default before the config has loaded (the game tests load it). */
    public static int value(ConfigSpec.IntValue option, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? option.get() : fallback;
    }

    public static double value(ConfigSpec.DoubleValue option, double fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? option.get() : fallback;
    }

    /** The configured Dash Strike of a hero ({@link SpeedConfig}): range, damage. */
    public static DashResult configuredDash(ServerPlayer player, Style style) {
        return dash(player, style, value(SpeedConfig.DASH_RANGE, 12), (float) value(SpeedConfig.DASH_DAMAGE, 5.0));
    }

    // ---- effects ----

    /** A blur in the hero's colours along a path, at the height of his body. */
    private static void streak(ServerLevel level, Style style, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(90, delta.length() * 4);
        EnergyParticleOptions deep = new EnergyParticleOptions(style.color(), 1.2F);
        EnergyParticleOptions light = new EnergyParticleOptions(style.glow(), 1.0F);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
            level.sendParticles(i % 2 == 0 ? deep : light, p.x, p.y + 0.9, p.z, 1, 0.08, 0.45, 0.08, 0.0);
        }
    }

    private static void line(ServerLevel level, EnergyParticleOptions particle, Vec3 from, Vec3 to, double every) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(40, delta.length() / every);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Three rings spreading out over the ground and a burst at his middle, in his colours. */
    private static void rings(ServerLevel level, Style style, Vec3 center, double radius) {
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (int ring = 1; ring <= 3; ring++) {
            double r = radius * ring / 3.0;
            int count = (int) (r * 8);
            EnergyParticleOptions particle = new EnergyParticleOptions(ring == 2 ? style.color() : style.glow(), 1.3F);
            for (int i = 0; i < count; i++) {
                double angle = i * Mth.TWO_PI / count;
                double x = center.x + Math.cos(angle) * r;
                double z = center.z + Math.sin(angle) * r;
                // Moving outwards, so the ring keeps spreading.
                level.sendParticles(particle, x, center.y - 0.7 + ring * 0.2, z, 0, Math.cos(angle), 0.0, Math.sin(angle), 0.25);
            }
        }
    }

    private SpeedStrikes() {
    }
}
