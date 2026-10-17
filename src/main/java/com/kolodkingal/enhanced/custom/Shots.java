package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What the custom powers share to aim, hit and show it (3.3): the first creature along the hero's view, the creatures of
 * an area or a cone that his area attacks may hurt ({@link AreaTargets#mayHurt}: never himself, his pets, players with
 * PvP off), lines and bursts of particles in the power's colours, and the two rules no power of his crosses: a Hex wall
 * (a shot from one side of it never reaches the other) and a complete Chaos Shield (it stops the shot at its surface and
 * is worn by it).
 */
public final class Shots {
    /**
     * Where a shot along the hero's view ends: on the first creature (then {@link Aim#target} is it), else on the first
     * block, else at full range; cut short where it would cross a Hex wall or meet a Chaos Shield.
     *
     * @param target the creature hit, or null
     * @param end    where the shot ends
     */
    public record Aim(@Nullable LivingEntity target, Vec3 end) {
    }

    public static Aim aim(ServerPlayer player, double range) {
        HitResult hit = Raycasts.any(player, range, e -> e != player && WandaAbilities.BEAM_TARGET.test(e) && e.isAlive()
                && !player.isPassengerOfSameVehicle(e), 1.0F);
        Vec3 from = player.getEyePosition();
        Vec3 end = hit.getLocation();
        LivingEntity target = null;
        if (hit instanceof EntityHitResult entityHit) {
            Entity e = entityHit.getEntity() instanceof PartEntity<?> part ? part.getParent() : entityHit.getEntity();
            target = e instanceof LivingEntity living ? living : null;
        }
        Vec3 cut = hexCut(player.serverLevel(), from, end);
        if (cut != null) {
            return new Aim(null, cut);
        }
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(player.level(), player, from, end, 1.0F);
        if (shield != null) {
            if (shield.owner() instanceof ServerPlayer wanda && player.canHarmPlayer(wanda)) {
                WandaAbilities.wear(wanda, 2.0F, player);
            }
            return new Aim(null, shield.point());
        }
        return new Aim(target, end);
    }

    /** The point where the segment first leaves or enters a Hex, or null if it crosses no Hex wall. */
    @Nullable
    public static Vec3 hexCut(ServerLevel level, Vec3 from, Vec3 to) {
        List<Hex> hexes = HexManager.data(level).hexes();
        if (hexes.isEmpty()) {
            return null;
        }
        double length = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(length * 2));
        for (Hex hex : hexes) {
            boolean inside = hex.contains(from);
            for (int i = 1; i <= steps; i++) {
                Vec3 at = from.lerp(to, i / (double) steps);
                if (hex.contains(at) != inside) {
                    return from.lerp(to, (i - 1) / (double) steps);
                }
            }
        }
        return null;
    }

    /** Whether the hero's power may reach {@code target} from where he stands (no Hex wall between them). */
    public static boolean reaches(ServerPlayer player, Entity target) {
        return hexCut(player.serverLevel(), player.getEyePosition(), target.position().add(0, target.getBbHeight() * 0.5, 0)) == null;
    }

    /** The creatures within {@code radius} of {@code center} that the hero's area powers may hurt, and that he can reach. */
    public static List<LivingEntity> area(ServerPlayer player, Vec3 center, double radius) {
        List<LivingEntity> list = new ArrayList<>();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(center) <= radius + e.getBbWidth() * 0.5
                    && AreaTargets.mayHurt(player, e, false) && reaches(player, e)) {
                list.add(e);
            }
        }
        return list;
    }

    /** The creatures in front of the hero, within {@code range} and {@code degrees} either side of where he looks. */
    public static List<LivingEntity> cone(ServerPlayer player, double range, double degrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(degrees));
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : area(player, eye, range)) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double d = to.length();
            if (d < 0.8 || to.scale(1.0 / d).dot(look) >= cos) {
                list.add(e);
            }
        }
        return list;
    }

    /** Hurts with the hero's power: damage type {@code hero_power} (or {@code hero_beam}), blamed on him. */
    public static boolean hurt(ServerPlayer player, Entity target, float amount) {
        return hurt(player, target, amount, ModDamageTypes.HERO_POWER);
    }

    public static boolean hurt(ServerPlayer player, Entity target, float amount, ResourceKey<DamageType> type) {
        if (amount <= 0) {
            return false;
        }
        return target.hurt(ModDamageTypes.source(player.level(), type, player, player), amount);
    }

    /** Pushes away from {@code from} (horizontally, and a little up). */
    public static void push(Entity target, Vec3 from, double strength, double up) {
        Vec3 away = target.position().subtract(from);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(0, 0, 1);
        }
        away = away.normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().add(away.x, up, away.z));
        target.hurtMarked = true;
    }

    /** A line of particles of the power's colour (and its glow at its core), from {@code from} to {@code to}. */
    public static void line(ServerLevel level, Vec3 from, Vec3 to, int color, int glow) {
        double length = from.distanceTo(to);
        int count = Math.min(96, Math.max(2, (int) (length * 2.5)));
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.lerp(to, i / (double) count);
            level.sendParticles(new EnergyParticleOptions(color, 1.1F), at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
            if (i % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(glow, 0.6F), at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** A line of any particle. */
    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double perBlock) {
        double length = from.distanceTo(to);
        int count = Math.min(120, Math.max(2, (int) (length * perBlock)));
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.lerp(to, i / (double) count);
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** A ring of particles of the power's colour on the ground round {@code center}. */
    public static void ring(ServerLevel level, Vec3 center, double radius, int color) {
        int count = Math.min(90, Math.max(12, (int) (radius * 9)));
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            level.sendParticles(new EnergyParticleOptions(color, 1.3F), center.x + Math.cos(a) * radius, center.y + 0.15,
                    center.z + Math.sin(a) * radius, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Where the hero's hand is, for a shot that leaves it (a little in front of him, at chest height, to the right). */
    public static Vec3 hand(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        return player.getEyePosition().add(look.scale(0.6)).add(right.scale(0.35)).add(0, -0.35, 0);
    }

    private Shots() {
    }
}
