package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Billy's Hex Barrier (3.2, Sneak + B held): a plate of blue magic held in front of him that follows his look and stops
 * <em>everything</em> that comes at him from the front arc ({@code billy.barrierArc}, half-angle 60 degrees), and nothing from
 * the side or the back. 3.3: projectiles (scanned where they cross the plate), beams (any beam's line stops at the plate:
 * {@link com.kolodkingal.enhanced.protect.Shelters#across}), blows, explosions (their damage and their push), lightning, and the
 * powers that reach for him (a telekinetic grip, Magneto's pull, a boss's attack: {@link
 * com.kolodkingal.enhanced.protect.Shelters#stops}). It is his own protective magic, not a copy of the Scarlet Witch's Chaos
 * Shield: it is a plate, not a dome.
 *
 * <p>It has no time limit and costs no hunger: it stays up while B is held. It has <em>health</em> ({@code billy.barrierHealth},
 * 30): every hit it stops takes what that hit would have done off it; at 0 it shatters (shards and a glassy crash) and cannot be
 * raised for {@code billy.barrierShatterCooldown} ticks. Its health is kept while it is down and mends slowly ({@code
 * billy.barrierRegenPerSecond}). Chaos magic breaks it at once: any power of the Scarlet Witch that hits it shatters it, whatever
 * health it has ({@link #isChaos}).
 *
 * <p>State: it reuses the synced fields of the Chaos Shield in {@link PowerData} ({@code chaosShieldActive}, when it formed, its
 * strength = its health), which no other power of Billy's uses, so the clients know it is up and how much is left; while it is
 * down the server keeps its health ({@link #health}). The recharge is the cooldown of B (shared with Freeze).
 */
public final class BillyBarrier {
    private static final ResourceLocation SLOW = Enhanced.id("billy_barrier_slow");
    /** How hard a blow that reaches the plate throws its attacker back (a sword's knockback is 0.4). */
    private static final double MELEE_KNOCKBACK = 0.6;
    private static final int SHATTER_SHARDS = 64;

    /** What is left of a lowered barrier, and since when it mends (server, per Billy). A Billy not in it has a whole barrier. */
    private record Kept(float health, long since) {
    }

    private static final Map<UUID, Kept> KEPT = new HashMap<>();
    /** When each Billy's barrier last paid for a power that reached for him (a grip, a pull): such a power costs at most twice a second. */
    private static final Map<UUID, Long> LAST_POWER = new HashMap<>();

    // ---- the numbers (read on both sides: the server config is synced to the clients) ----

    /** Whether Sneak + B raises a barrier ({@code billy.barrier}); before the config has loaded, yes. */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || EnhancedConfig.BILLY_BARRIER.get();
    }

    /** The health of a whole barrier at this rank ({@code billy.barrierHealth}, and more with the rank if the server says so). */
    public static float maxStrength(BuildData build) {
        double health = EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.BILLY_BARRIER_HEALTH.get() : 30.0;
        double bonus = EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.BILLY_BARRIER_RANK_STRENGTH_BONUS.get() : 0.0;
        return (float) (health * (1.0 + bonus * BillyAbilities.beamProgress(build)));
    }

    /** The health of a whole barrier of this Billy (his rank's). */
    public static float maxHealth(Player billy) {
        return maxStrength(billy.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY));
    }

    /**
     * What is left of this Billy's barrier now: while it is raised, its synced strength; while it is down, what it had when it came
     * down and what it has mended since ({@code billy.barrierRegenPerSecond}), up to whole. Server.
     */
    public static float health(ServerPlayer billy) {
        PowerData data = Powers.get(billy);
        if (data.chaosShieldActive()) {
            return data.shieldStrength();
        }
        float max = maxHealth(billy);
        Kept kept = KEPT.get(billy.getUUID());
        if (kept == null) {
            return max;
        }
        long since = Math.max(0L, billy.level().getGameTime() - kept.since());
        return (float) Math.min(max, kept.health() + EnhancedConfig.BILLY_BARRIER_REGEN.get() * since / 20.0);
    }

    /** Sets what is left of a lowered barrier (it mends from now on). Server; tests use it too. */
    public static void keep(ServerPlayer billy, float health) {
        KEPT.put(billy.getUUID(), new Kept(Math.max(0.0F, health), billy.level().getGameTime()));
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
        return owns(data) && data.chaosShieldUp(player.level().getGameTime());
    }

    /**
     * Whether the shield fields of this player's data are his Hex Barrier's when raised: Billy, or (3.4) a custom hero with
     * Billy's Freeze and its Sneak variant (the barrier) on. (The fields are shared by every hero's shield; a custom hero
     * has one shield at most: {@code Resource.SHIELD}.) Both sides.
     */
    public static boolean owns(PowerData data) {
        if (data.power() == Power.BILLY) {
            return true;
        }
        HeroKit kit = data.power() == Power.CUSTOM ? data.kit() : null;
        CustomHero.Pick pick = kit == null ? null : kit.hero().pick(HeroAbility.BILLY_FREEZE);
        return pick != null && !HeroAbility.MAIN.equals(pick.option());
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
            lower(player, data);
            return;
        }
        if (player.isSpectator() || data.chaosShieldActive() || !enabled()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.BILLY_FREEZE, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.billy_barrier.recharging",
                    String.format(Locale.ROOT, "%.1f", (data.cooldownUntil(data.slot(HeroAbility.BILLY_FREEZE)) - now) / 20.0)), true);
            return;
        }
        float health = health(player);
        if (health < 1.0F) {
            player.displayClientMessage(Component.translatable("message.enhanced.billy_barrier.mending"), true);
            return;
        }
        KEPT.remove(player.getUUID());
        data.raiseChaosShield(now, EnhancedConfig.BILLY_BARRIER_WIND_UP.get(), health);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.9F);
    }

    /** It comes down (the key came up, or he can no longer hold it): what is left of it is kept and mends, a short recharge starts if it ever formed. */
    public static void lower(ServerPlayer player, PowerData data) {
        if (!data.chaosShieldActive()) {
            return;
        }
        long now = player.level().getGameTime();
        boolean formed = data.chaosShieldUp(now);
        keep(player, data.shieldStrength());
        data.setChaosShieldActive(false);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (formed) {
            data.startCooldown(HeroAbility.BILLY_FREEZE, now, EnhancedConfig.BILLY_BARRIER_COOLDOWN.get());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.9F);
    }

    // ---- every tick ----

    /** Keeps the barrier up while it is held (no time limit, no hunger): the slow walk, and the projectiles that reach the plate. */
    static void tick(ServerPlayer player, PowerData data) {
        if (!data.chaosShieldActive()) {
            PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            return;
        }
        if (player.isSpectator() || !enabled()) {
            lower(player, data);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
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
        wear(player, data, power, projectile);
    }

    /**
     * An incoming hit on a Billy who holds a complete barrier: whatever comes from the front (a projectile, a blow, a beam, an
     * explosion, lightning, a curse: any harm that has a place it comes from) is stopped whole and wears the barrier down by what
     * it would have done; a blow throws its attacker back. Harm with no place (falling, hunger, poison, fire on him) and what
     * bypasses invulnerability are not its business. Called by the damage hook before anything else of his is looked at.
     *
     * @return whether the hit was stopped
     */
    public static boolean absorb(ServerPlayer billy, LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker == billy || (attacker == null && direct == billy)) {
            return false; // his own
        }
        Vec3 from = origin(source);
        if (from == null || !covers(billy, from)) {
            return false;
        }
        PowerData data = Powers.get(billy);
        if (WandaAbilities.isMeleeHit(source) && direct instanceof LivingEntity blow) {
            blockMelee(billy, data, blow, event.getAmount(), source);
        } else {
            sparks(billy, from);
            wear(billy, data, event.getAmount(), attacker != null ? attacker : direct, source);
        }
        event.setCanceled(true);
        return true;
    }

    /**
     * Where a hit comes from: the projectile or the striker for what has one (its middle), an explosion's centre, or whoever dealt
     * it; null for harm that comes from nowhere.
     */
    @Nullable
    static Vec3 origin(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct != null) {
            return direct.position().add(0, direct.getBbHeight() * 0.5, 0);
        }
        if (source.getSourcePosition() != null) {
            return source.getSourcePosition();
        }
        Entity attacker = source.getEntity();
        return attacker == null ? null : attacker.position().add(0, attacker.getBbHeight() * 0.5, 0);
    }

    /**
     * A power that is not a hit reaches for him from the front (a grip, a pull, a curse, a boss's spell): the plate stops it, sparks
     * and pays {@code cost} (at most twice a second for the same Billy; the Scarlet Witch's shatters it at once). Called from
     * {@link com.kolodkingal.enhanced.protect.Shelters#stops}.
     */
    public static void stopPower(ServerPlayer billy, @Nullable Entity source, Vec3 from, float cost) {
        sparks(billy, from);
        long now = billy.level().getGameTime();
        Long last = LAST_POWER.get(billy.getUUID());
        if (!isChaos(source) && last != null && now - last < 10 && now >= last) {
            return;
        }
        LAST_POWER.put(billy.getUUID(), now);
        ServerLevel level = billy.serverLevel();
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.7F);
        wear(billy, Powers.get(billy), cost, source);
    }

    /** A creature's or player's blow lands on the plate: sparks and a ring of sound there, the attacker thrown back, and the price paid. */
    private static void blockMelee(ServerPlayer billy, PowerData data, LivingEntity attacker, float amount, DamageSource source) {
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
        wear(billy, data, amount, attacker, source);
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
     * Whether a power is the Scarlet Witch's chaos magic: whoever is behind it (a projectile's owner, a thrown block's thrower...)
     * is a Scarlet Witch (3.4: or a custom hero with any of her abilities). It shatters a Hex Barrier at once.
     */
    public static boolean isChaos(@Nullable Entity cause) {
        Entity maker = com.kolodkingal.enhanced.protect.Shelters.maker(cause);
        return maker instanceof Player player && Powers.get(player).acts(Power.SCARLET_WITCH);
    }

    /**
     * Wears the barrier down by what it just stopped; when nothing is left it shatters. Chaos magic ({@link #isChaos}) shatters
     * it whatever it had left.
     *
     * @param cause who or what dealt it (a projectile counts as its owner), or null if it is not known
     * @return how much of {@code amount} it took (all of it: it stops the hit that breaks it too)
     */
    public static float wear(ServerPlayer billy, PowerData data, float amount, @Nullable Entity cause) {
        return wear(billy, data, amount, cause, null);
    }

    private static float wear(ServerPlayer billy, PowerData data, float amount, @Nullable Entity cause, @Nullable DamageSource source) {
        if (!data.chaosShieldActive()) {
            return 0.0F;
        }
        if (isChaos(cause) || (source != null && source.is(ModDamageTypes.CHAOS_BEAM))) {
            data.setShieldStrength(0F);
            shatter(billy, data, true);
            return amount;
        }
        data.setShieldStrength(Math.max(0.0F, data.shieldStrength() - amount));
        if (data.shieldStrength() <= 1.0E-4F) {
            data.setShieldStrength(0F);
            shatter(billy, data, false);
        }
        return amount;
    }

    /**
     * The barrier took all it could (or chaos magic hit it): it bursts into blue shards with a glassy crash that everyone near
     * hears, its health is gone (it mends from nothing) and it cannot be raised for {@code billy.barrierShatterCooldown}.
     */
    private static void shatter(ServerPlayer billy, PowerData data, boolean chaos) {
        ServerLevel level = billy.serverLevel();
        keep(billy, 0.0F);
        data.setChaosShieldActive(false);
        PowerTicker.setModifier(billy, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        data.startCooldown(HeroAbility.BILLY_FREEZE, level.getGameTime(), EnhancedConfig.BILLY_BARRIER_SHATTER_COOLDOWN.get());
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
        // A ring of light runs out from the plate as it goes, and red sparks if chaos magic broke it.
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            Vec3 dir = plate.right().scale(Math.cos(angle)).add(plate.up().scale(Math.sin(angle)));
            Vec3 at = plate.center().add(dir.scale(0.3));
            level.sendParticles(new EnergyParticleOptions(chaos && i % 2 == 0 ? Power.SCARLET_WITCH.color : BillyAbilities.CORE, 1.1F),
                    at.x, at.y, at.z, 0, dir.x, dir.y, dir.z, 0.35);
        }
        level.sendParticles(ParticleTypes.FLASH, plate.center().x, plate.center().y, plate.center().z, 1, 0, 0, 0, 0);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.4F, 1.2F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2F, 1.4F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 0.5F, 1.8F);
        billy.displayClientMessage(Component.translatable(chaos ? "message.enhanced.billy_barrier.chaos" : "message.enhanced.billy_barrier.broken")
                .withStyle(chaos ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.AQUA), true);
    }

    // ---- lifecycle ----

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        if (data.chaosShieldActive()) {
            keep(player, data.shieldStrength());
        }
        data.setChaosShieldActive(false);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW, 0.0, false, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Server stop: forget what every barrier had left. */
    public static void clear() {
        KEPT.clear();
        LAST_POWER.clear();
    }

    private BillyBarrier() {
    }
}
