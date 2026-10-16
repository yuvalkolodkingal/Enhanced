package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Locale;

/**
 * Billy's Hex Barrier (3.2, Sneak + B held): a plate of blue magic held in front of him that follows his look and stops
 * what comes at him <em>from the front arc only</em> ({@code billy.barrierArc}, half-angle 60 degrees): projectiles, the
 * blows of creatures and players, most of an explosion, Thor's lightning. It is his own protective magic, not a copy of the
 * Scarlet Witch's Chaos Shield: it is a plate, not a dome; it is <em>limited</em> (a strength to wear down, a time to hold,
 * a recharge after it, a slower walk while it is up and a little hunger a second) and grows with his Build Magic rank.
 *
 * <p>It stops exactly what the Chaos Shield stops ({@link WandaAbilities#projectilePower} for a projectile's price,
 * {@link WandaAbilities#isMeleeHit}, {@link WandaAbilities#neutralize}), and nothing a boss does that the Chaos Shield does
 * not stop (the Entomb, the Mind Crush, the Hex...): it is a damage hook, never an invulnerability. A blow from the side or
 * the back passes whole.
 *
 * <p>State: it reuses the synced fields of the Chaos Shield in {@link PowerData} ({@code chaosShieldActive}, when it formed,
 * its strength), which no other power of Billy's uses, so the clients know it is up, how much is left and how long it
 * has held; the recharge is the cooldown of B (shared with Freeze).
 */
public final class BillyBarrier {
    private static final ResourceLocation SLOW = Enhanced.id("billy_barrier_slow");
    /** How hard a blow that reaches the plate throws its attacker back (a sword's knockback is 0.4). */
    private static final double MELEE_KNOCKBACK = 0.6;
    private static final int SHATTER_SHARDS = 40;

    // ---- the numbers (read on both sides: the server config is synced to the clients) ----

    /** Whether Sneak + B raises a barrier ({@code billy.barrier}); before the config has loaded, yes. */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || EnhancedConfig.BILLY_BARRIER.get();
    }

    /** Points of damage a fresh barrier stops at this rank. */
    public static float maxStrength(BuildData build) {
        return (float) (EnhancedConfig.BILLY_BARRIER_STRENGTH.get()
                * (1.0 + EnhancedConfig.BILLY_BARRIER_RANK_STRENGTH_BONUS.get() * BillyAbilities.beamProgress(build)));
    }

    /** How long (ticks) a complete barrier can be held at this rank. */
    public static int maxTicks(BuildData build) {
        return (int) Math.round(EnhancedConfig.BILLY_BARRIER_DURATION.get()
                * (1.0 + EnhancedConfig.BILLY_BARRIER_RANK_DURATION_BONUS.get() * BillyAbilities.beamProgress(build)));
    }

    /** Half the angle of the arc it covers, in degrees. */
    public static double arc() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.BILLY_BARRIER_ARC.get() : 60.0;
    }

    public static double width() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.BILLY_BARRIER_WIDTH.get() : 1.4;
    }

    public static double height() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.BILLY_BARRIER_HEIGHT.get() : 2.4;
    }

    public static double distance() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.BILLY_BARRIER_DISTANCE.get() : 1.6;
    }

    /** The middle of his body, from which the plate is held and the arc measured. */
    public static Vec3 body(Entity billy) {
        return billy.position().add(0, billy.getBbHeight() * 0.55, 0);
    }

    /**
     * Where the plate is: its middle, the way it faces, and the two directions across it. Shared by the server (what it
     * stops) and the client (where it is drawn).
     */
    public record Frame(Vec3 center, Vec3 forward, Vec3 right, Vec3 up) {
    }

    /** The plate for a Billy at {@code body} looking along {@code look}. */
    public static Frame frame(Vec3 body, Vec3 look) {
        Vec3 forward = look.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        return new Frame(body.add(forward.scale(distance())), forward, right, up);
    }

    // ---- is it up, and what does it cover ----

    /** Whether this player holds a complete Hex Barrier (Billy, raised and formed). */
    public static boolean isUp(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.BILLY && data.chaosShieldUp(player.level().getGameTime());
    }

    /** Whether a point is in the arc the barrier covers: within the half-angle of the way he looks, seen from his body. */
    public static boolean covers(Player billy, Vec3 point) {
        Vec3 to = point.subtract(body(billy));
        double length = to.length();
        if (length < 1.0E-4) {
            return true; // right on top of him: in front of him as much as anywhere
        }
        return to.scale(1.0 / length).dot(billy.getLookAngle()) >= Math.cos(Math.toRadians(arc()));
    }

    // ---- the key ----

    /** Sneak + B went down (or B came up while it is raised): the barrier forms, or comes down. */
    static void key(ServerPlayer player, PowerData data, boolean down) {
        if (!down) {
            if (data.chaosShieldActive()) {
                lower(player, data, false);
            }
            return;
        }
        if (player.isSpectator() || data.chaosShieldActive() || !enabled()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(4, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.billy_barrier.recharging",
                    String.format(Locale.ROOT, "%.1f", (data.cooldownUntil(4) - now) / 20.0)), true);
            return;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.billy_barrier.hungry"), true);
            return;
        }
        BuildData build = player.getData(ModAttachments.BUILD_DATA);
        data.raiseChaosShield(now, EnhancedConfig.BILLY_BARRIER_WIND_UP.get(), maxStrength(build));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.9F);
    }

    /** With an empty hunger bar there is nothing left to fuel the barrier (not in Creative). */
    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    /**
     * It comes down (the key came up, the time ran out, hunger): the recharge starts, if it was ever complete.
     *
     * @param ended whether its time ran out (the same recharge; it only says so)
     */
    private static void lower(ServerPlayer player, PowerData data, boolean ended) {
        long now = player.level().getGameTime();
        boolean formed = data.chaosShieldUp(now);
        data.setChaosShieldActive(false);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (formed) {
            data.startCooldown(4, now, EnhancedConfig.BILLY_BARRIER_COOLDOWN.get());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.9F);
    }

    // ---- every tick ----

    /** Keeps the barrier up while it is held: the time, the hunger, the slow walk, and the projectiles that reach the plate. */
    static void tick(ServerPlayer player, PowerData data) {
        if (!data.chaosShieldActive()) {
            PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            return;
        }
        if (player.isSpectator() || !enabled()) {
            lower(player, data, false);
            return;
        }
        if (starving(player)) {
            lower(player, data, false);
            player.displayClientMessage(Component.translatable("message.enhanced.billy_barrier.hungry"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        BuildData build = player.getData(ModAttachments.BUILD_DATA);
        if (now >= data.shieldFormedAt() + maxTicks(build)) {
            lower(player, data, true);
            return;
        }
        player.causeFoodExhaustion(EnhancedConfig.BILLY_BARRIER_EXHAUSTION_PER_SECOND.get().floatValue() / 20.0F);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW, EnhancedConfig.BILLY_BARRIER_SPEED_FACTOR.get() - 1.0, true,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        Frame plate = frame(body(player), player.getLookAngle());
        RandomSource random = player.getRandom();
        if (!data.chaosShieldUp(now)) {
            // Still forming: motes gather onto the plate. It stops nothing yet.
            for (int i = 0; i < 2; i++) {
                Vec3 at = plate.center().add(plate.right().scale((random.nextDouble() - 0.5) * width())).add(plate.up().scale((random.nextDouble() - 0.5) * height()));
                level.sendParticles(new EnergyParticleOptions(BillyAbilities.MAIN, 0.9F), at.x, at.y, at.z, 0, 0, 0, 0, 0.0);
            }
            return;
        }
        // Projectiles that cross the plate's plane within its outline.
        double halfWidth = width() * 0.5 + 0.3;
        double halfHeight = height() * 0.5 + 0.3;
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(distance() + 5.0),
                p -> p.getOwner() != player && !WandaAbilities.isHeadingHome(p))) {
            Vec3 velocity = projectile.getDeltaMovement();
            double toward = velocity.dot(plate.forward());
            if (toward >= 0) {
                continue; // not coming at him
            }
            Vec3 offset = projectile.position().subtract(plate.center());
            double ahead = offset.dot(plate.forward());
            // It crosses the plate's plane this tick: it is in front of it (or only just through) and ends behind it (or on it).
            if (ahead < -0.3 || ahead + toward > 0.3) {
                continue;
            }
            if (Math.abs(offset.dot(plate.right())) <= halfWidth && Math.abs(offset.dot(plate.up())) <= halfHeight) {
                block(player, data, projectile);
                if (!data.chaosShieldActive()) {
                    return; // broken: the rest get through
                }
            }
        }
    }

    // ---- what it stops ----

    /** A projectile reaches the plate: it is neutralised in Billy's blue, and the barrier pays what it would have cost him. */
    private static void block(ServerPlayer player, PowerData data, Projectile projectile) {
        float power = WandaAbilities.projectilePower(projectile);
        WandaAbilities.neutralize(player.serverLevel(), projectile, BillyAbilities.MAIN);
        wear(player, data, power);
    }

    /**
     * An incoming blow on a Billy who holds a complete barrier: what the plate stops is cancelled (or reduced), and wears it
     * down. Called by the damage hook before anything else of his is looked at.
     *
     * @return whether the blow was stopped altogether
     */
    public static boolean absorb(ServerPlayer billy, LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        PowerData data = Powers.get(billy);
        Entity direct = source.getDirectEntity();
        if (source.is(DamageTypeTags.IS_PROJECTILE) && direct != null && source.getEntity() != billy) {
            if (!covers(billy, direct.position())) {
                return false;
            }
            sparks(billy, direct.position());
            wear(billy, data, event.getAmount());
            event.setCanceled(true);
            return true;
        }
        if (WandaAbilities.isMeleeHit(source) && direct instanceof LivingEntity attacker && attacker != billy) {
            if (!covers(billy, attacker.position().add(0, attacker.getBbHeight() * 0.5, 0))) {
                return false;
            }
            blockMelee(billy, data, attacker, event.getAmount());
            event.setCanceled(true);
            return true;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            Vec3 from = source.getSourcePosition();
            if (from != null && covers(billy, from)) {
                float absorbable = event.getAmount() * (1.0F - EnhancedConfig.BILLY_BARRIER_EXPLOSION_FACTOR.get().floatValue());
                sparks(billy, from);
                event.setAmount(event.getAmount() - wear(billy, data, absorbable));
            }
            return false;
        }
        if (source.is(DamageTypeTags.IS_LIGHTNING) && ThorAbilities.isThor(source.getEntity())) {
            Vec3 from = source.getEntity().position().add(0, source.getEntity().getBbHeight() * 0.5, 0);
            if (!covers(billy, from)) {
                return false;
            }
            wear(billy, data, event.getAmount());
            event.setCanceled(true);
            return true;
        }
        return false;
    }

    /** A creature's or player's blow lands on the plate: sparks and a ring of sound there, the attacker thrown back, and the price paid. */
    private static void blockMelee(ServerPlayer billy, PowerData data, LivingEntity attacker, float amount) {
        ServerLevel level = billy.serverLevel();
        Vec3 center = body(billy);
        Vec3 away = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0).subtract(center);
        double distance = away.length();
        Vec3 contact = distance < 1.0E-3 ? center : center.add(away.scale(Math.min(distance, distance() + 0.2) / distance));
        sparks(billy, contact);
        level.playSound(null, contact.x, contact.y, contact.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.9F);
        level.playSound(null, contact.x, contact.y, contact.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.6F);
        double flat = Math.hypot(away.x, away.z);
        Vec3 out = flat < 1.0E-3 ? billy.getLookAngle() : new Vec3(away.x / flat, 0, away.z / flat);
        attacker.knockback(MELEE_KNOCKBACK, -out.x, -out.z);
        attacker.hurtMarked = true;
        wear(billy, data, amount);
    }

    /** Blue and white-hot sparks where something struck the plate. */
    private static void sparks(ServerPlayer billy, Vec3 from) {
        Frame plate = frame(body(billy), billy.getLookAngle());
        // (on the plate, where the line from his body to the thing crosses it; far things are clamped to its outline)
        Vec3 dir = from.subtract(body(billy));
        double along = dir.dot(plate.forward());
        Vec3 at = along > 1.0E-3 ? body(billy).add(dir.scale(distance() / along)) : plate.center();
        ServerLevel level = billy.serverLevel();
        level.sendParticles(new EnergyParticleOptions(BillyAbilities.MAIN, 1.2F), at.x, at.y, at.z, 8, 0.12, 0.12, 0.12, 0.12);
        level.sendParticles(new EnergyParticleOptions(BillyAbilities.CORE, 0.9F), at.x, at.y, at.z, 4, 0.06, 0.06, 0.06, 0.06);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 4, 0.12, 0.12, 0.12, 0.25);
    }

    /**
     * Wears the barrier down by what it just stopped; when nothing is left it breaks.
     *
     * @return how much of {@code amount} it took (all of it, unless it had less strength left)
     */
    static float wear(ServerPlayer billy, PowerData data, float amount) {
        float taken = Math.min(amount, data.shieldStrength());
        data.setShieldStrength(data.shieldStrength() - taken);
        if (data.shieldStrength() <= 1.0E-4F) {
            data.setShieldStrength(0F);
            shatter(billy, data);
        }
        return taken;
    }

    /** The barrier took all it could: a burst of blue shards and a glassy sound, and a long wait before the next. */
    private static void shatter(ServerPlayer billy, PowerData data) {
        ServerLevel level = billy.serverLevel();
        data.setChaosShieldActive(false);
        PowerTicker.setModifier(billy, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        data.startCooldown(4, level.getGameTime(), EnhancedConfig.BILLY_BARRIER_BROKEN_COOLDOWN.get());
        Frame plate = frame(body(billy), billy.getLookAngle());
        RandomSource random = billy.getRandom();
        ItemParticleOption shard = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.LIGHT_BLUE_DYE));
        for (int i = 0; i < SHATTER_SHARDS; i++) {
            Vec3 at = plate.center().add(plate.right().scale((random.nextDouble() - 0.5) * width())).add(plate.up().scale((random.nextDouble() - 0.5) * height()));
            Vec3 out = plate.forward().add(random.nextGaussian() * 0.5, random.nextGaussian() * 0.5, random.nextGaussian() * 0.5).normalize();
            level.sendParticles(shard, at.x, at.y, at.z, 0, out.x, out.y, out.z, 0.3);
            if (i % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(i % 4 == 0 ? BillyAbilities.CORE : BillyAbilities.MAIN, 1.4F), at.x, at.y, at.z, 0, out.x, out.y, out.z, 0.3);
            }
        }
        Vec3 c = plate.center();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 1.4F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
        billy.displayClientMessage(Component.translatable("message.enhanced.billy_barrier.broken").withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    // ---- lifecycle ----

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        data.setChaosShieldActive(false);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private BillyBarrier() {
    }
}
