package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StonePowers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * What the nanotech suits and the Hulkbuster share (3.8, lane L4, PLAN 6): their numbers read from the config (the
 * plan's defaults when it is not loaded), who their blows may reach, the repulsor's instant ray, the small booms that
 * never break a block unless {@code [iron_man] boomsBreakBlocks} and {@code mobGriefing} both say so, the effects sent
 * to everyone near ({@link IronFxPayload}) and the sounds. The moves are {@link Mark50Moves}, {@link Mark85Moves}
 * (with {@link EnergyBlades}, {@link EnergyShield}, {@link LightningRefocuser}, {@link HomingMissiles} and {@link
 * NanoDroneEntity}) and {@code hulkbuster.HulkbusterMoves}; the housings' wear and rebuild {@link NanoHousings}.
 */
public final class NanoTech {
    /** How far the effects are sent. */
    public static final double FX_RANGE = 64.0;
    /** The arc reactor's blue, the energy blades' and shield's cyan, the suits' gold. */
    public static final int REACTOR_BLUE = 0x9FE8FF;
    public static final int BLADE_BLUE = 0x6FD8FF;
    public static final int GOLD = 0xE8B23A;

    // ---- the numbers ----

    public static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    public static double cfg(@Nullable ConfigSpec.DoubleValue value, double fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    public static int cfg(@Nullable ConfigSpec.IntValue value, int fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    public static boolean cfg(@Nullable ConfigSpec.BooleanValue value, boolean fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    // ---- who ----

    /** Whether this suit is the one he wears (or has deployed) now. */
    public static boolean wears(Player player, SuitKind kind) {
        return IronSuits.worn(player) == kind;
    }

    /** Whether his blows may hurt the creature (PvP, teams, pets, Creative, the area rule; passives too: aimed). */
    public static boolean mayHurt(ServerPlayer player, Entity target) {
        Entity parent = Platform.get().parts().parentOf(target);
        LivingEntity living = parent instanceof LivingEntity p ? p : target instanceof LivingEntity l ? l : null;
        return living != null && living != player && living.isAlive() && !living.isSpectator()
                && AreaTargets.mayHurt(player, living, true);
    }

    /** Whether the creature is a boss (it is never grabbed). */
    public static boolean isBoss(LivingEntity target) {
        return target.getType().is(ModTags.BOSSES);
    }

    /**
     * The creature in front of him within {@code reach} of his body's edge (his width counts: the Hulkbuster's fists
     * reach further), in a cone of 70 degrees either side of his facing (the pitch does not count: the Hulkbuster's
     * eyes are far above a zombie's), the nearest first; null when there is none he may hurt.
     */
    @Nullable
    public static LivingEntity inFront(ServerPlayer player, double reach) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : near(player, reach, 70.0)) {
            double dist = e.position().subtract(player.position()).horizontalDistance();
            if (dist < bestDist) {
                best = e;
                bestDist = dist;
            }
        }
        return best;
    }

    /**
     * Every creature he may hurt within {@code reach} of his body's edge, in a cone of {@code halfAngle} degrees either
     * side of his facing (the pitch does not count), up to a quarter of his height above or below him.
     */
    public static List<LivingEntity> near(ServerPlayer player, double reach, double halfAngle) {
        double r = reach + player.getBbWidth() * 0.5;
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 at = player.position();
        double cos = Math.cos(Math.toRadians(halfAngle));
        AABB box = player.getBoundingBox().inflate(r, Math.max(1.0, player.getBbHeight() * 0.25), r);
        List<LivingEntity> found = new java.util.ArrayList<>();
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> mayHurt(player, e))) {
            Vec3 to = e.position().subtract(at);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (flat.length() - e.getBbWidth() * 0.5 > r) {
                continue;
            }
            if (flat.lengthSqr() > 1.0E-4 && flat.normalize().dot(facing) < cos) {
                continue;
            }
            found.add(e);
        }
        return found;
    }

    /** Hurts the creature with one of his damage types, the blow always landing (the stones' rule). */
    public static boolean hurt(ServerPlayer player, Entity target, ResourceKey<DamageType> type, float amount) {
        return StonePowers.hurt(player.serverLevel(), player, target, type, amount, player);
    }

    /** Throws the creature away from {@code from} (credited to him for the Soul Stone's sacrifice). */
    public static void push(LivingEntity victim, Vec3 from, double strength, double lift, LivingEntity by) {
        StonePowers.shove(victim, from, strength, lift, by);
    }

    // ---- cooldowns of their own ----

    /**
     * Whether a long move that shares its key with a quick one is ready (3.8, lane L4): the drones (Sneak + N, 900
     * ticks) and the Mark 50's nano blade (Sneak + R, 160). For Iron Man himself their cooldowns are their own, so that
     * the drones never hold up the missiles for 45 seconds, nor the blade the repulsor (the HUD's key line shows the
     * key's quick move); not ready, he is told how long is left. A custom hero's rows wait on their keys as every row.
     */
    public static boolean ownReady(ServerPlayer player, PowerData data, HeroAbility row, long readyAt, String waitKey) {
        long now = player.level().getGameTime();
        if (data.power() != Power.IRON_MAN) {
            return data.isReady(row, now);
        }
        if (readyAt <= now || readyAt - now > 20L * 3600) {
            return true;
        }
        player.displayClientMessage(Component.translatable(waitKey, (readyAt - now + 19) / 20)
                .withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    /** Starts a long move's own cooldown (Iron Man) or its row's (a custom hero); the game time it ends for him. */
    public static long startOwn(ServerPlayer player, PowerData data, HeroAbility row, int ticks) {
        long now = player.level().getGameTime();
        if (data.power() != Power.IRON_MAN) {
            data.startCooldown(row, now, ticks);
        }
        return now + ticks;
    }

    // ---- the repulsor's ray and the small booms ----

    /**
     * What a repulsor's instant ray met: the point, the creature (or null for a block, a wall or the empty air),
     * whether it met a creature or a block, and (3.8.2) the wall it ran into first (a Chaos Shield, a Hex Barrier...),
     * where it ends instead: then it hurts nothing and makes no boom ({@link #stopped}).
     */
    public record Ray(Vec3 end, @Nullable Entity target, boolean hit, @Nullable WandaAbilities.ShieldStop wall) {
        /** Whether a wall stopped it. */
        public boolean blocked() {
            return wall != null;
        }
    }

    /**
     * An instant-hit ray along his look ({@code range} blocks): the first creature or block, as the crosshair sees it
     * (PLAN 5.7's repulsor, which the nanotech suits share), or the wall before them.
     */
    public static Ray ray(ServerPlayer player, double range) {
        return ray(player, range, ContinuousBeam.targets(player));
    }

    /**
     * {@link #ray(ServerPlayer, double)} that hits only what {@code targets} lets it (the classic suits': creatures).
     */
    public static Ray ray(ServerPlayer player, double range, Predicate<Entity> targets) {
        HitResult hit = Raycasts.any(player, range, targets, 1.0F);
        // 3.8.2: a complete Chaos Shield in the way (or another wall) stops it at its surface, as it stops every beam.
        WandaAbilities.ShieldStop wall = WandaAbilities.shieldAcross(player.level(), player, player.getEyePosition(),
                hit.getLocation(), 1.0F);
        if (wall != null) {
            return new Ray(wall.point(), null, false, wall);
        }
        Entity target = hit instanceof EntityHitResult e ? e.getEntity() : null;
        if (target != null && Platform.get().parts().isPart(target)) {
            target = Platform.get().parts().parentOf(target);
        }
        return new Ray(hit.getLocation(), target, hit.getType() != HitResult.Type.MISS, null);
    }

    /**
     * 3.8.2: where a laser or a bolt of {@code firer}'s from {@code from} to {@code to} ends: {@code to}, or the first
     * wall in between (a Chaos Shield, a Hex Barrier...), returned as the stop. (A drone's laser, a laser of the Laser
     * Burst, a link of the Refocuser's chain; a line that starts inside a shield is not stopped by it.)
     */
    @Nullable
    public static WandaAbilities.ShieldStop wallAcross(Entity firer, Vec3 from, Vec3 to) {
        return WandaAbilities.shieldAcross(firer.level(), firer, from, to, 1.0F);
    }

    /**
     * A ray, laser or bolt of his ran into a wall: it ends there in a flash, and the wall pays {@code amount}, what the
     * hit would have done (see {@link WandaAbilities#stopBeam}).
     */
    public static void stopped(ServerPlayer player, WandaAbilities.ShieldStop wall, float amount, int color) {
        WandaAbilities.stopBeam(player.serverLevel(), player, wall, amount, color);
    }

    /**
     * A small boom of power {@code power} at {@code at}: creatures within {@code 2 x power} (PLAN 5.7: power 0.8 → 1.5
     * blocks, the nano cannon's 1.5 → 3) take {@code 2.5 x power} and are pushed, the one who fired spared; smoke,
     * flash and a bang. Blocks break only with {@code boomsBreakBlocks} and {@code mobGriefing}.
     */
    public static void boom(ServerPlayer player, Vec3 at, double power, ResourceKey<DamageType> type,
            @Nullable Entity spared) {
        ServerLevel level = player.serverLevel();
        double radius = 2.0 * power;
        float damage = (float) (2.5 * power);
        for (LivingEntity e : StonePowers.around(level, player, at, radius, e -> e != spared && mayHurt(player, e))) {
            if (hurt(player, e, type, damage)) {
                push(e, at, 0.35 * power, 0.15, player);
            }
        }
        level.sendParticles(power >= 1.2 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, at.x, at.y, at.z,
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(new EnergyParticleOptions(REACTOR_BLUE, 1.0F), at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.15);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                (float) Math.min(1.5, 0.5 + 0.5 * power), 1.3F);
        if (cfg(IronManConfig.BOOMS_BREAK_BLOCKS, false)
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            level.explode(player, at.x, at.y, at.z, (float) power, Level.ExplosionInteraction.MOB);
        }
    }

    // ---- effects ----

    /** An effect of his for everyone near ({@code kind} a constant of {@link IronFxPayload}). */
    public static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > IronFxPayload.MAX_POINTS ? points.subList(0, IronFxPayload.MAX_POINTS)
                : points;
        Net.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new IronFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Where his right (or left) palm is, roughly: at the shoulder's height, a little forward and to the side. */
    public static Vec3 palm(Player player, boolean right) {
        float scale = player.getScale();
        Vec3 look = player.getLookAngle();
        Vec3 side = Vec3.directionFromRotation(0.0F, player.getYRot() + (right ? 90.0F : -90.0F));
        return player.getEyePosition().add(0.0, -0.35 * scale, 0.0).add(look.scale(0.55 * scale))
                .add(side.scale(0.35 * scale));
    }

    /** The reactor on his chest. */
    public static Vec3 chest(Player player) {
        float scale = player.getScale();
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.yBodyRot);
        return player.position().add(0.0, 1.25 * scale, 0.0).add(facing.scale(0.2 * scale));
    }

    private NanoTech() {
    }
}
