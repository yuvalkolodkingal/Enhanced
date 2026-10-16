package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The code the Mad Titan and the hero Thanos share: what a stone's power does in the world, whoever's hand it is in.
 * Every function takes the caster as a {@link LivingEntity} and asks {@link AreaTargets#mayHurt} who may be hurt, so the
 * same beam, ring and splash serve the boss (who hurts players whatever PvP says) and the hero (whose attacks on players
 * need PvP). None of it breaks a block ({@link #craterIfAllowed} is the single, opt-in exception); the Chaos Shield is
 * consulted wherever a beam crosses it.
 */
public final class StonePowers {

    // ---- who can be touched ----

    /** Whether the stones cannot reach the creature: Creative and Spectator players, and a Vision phased through everything. */
    public static boolean untouchable(LivingEntity creature) {
        return creature instanceof Player player && (player.isCreative() || player.isSpectator() || VisionAbilities.isPhased(player));
    }

    /**
     * Whether a stone can take hold of the creature (grab it, pull it): not untouchable, not pinned by Mjolnir, and not one
     * the villains' pulls pass by ({@code pull_immune}: what no telekinesis lifts, and the bosses).
     */
    public static boolean graspable(LivingEntity creature) {
        return !untouchable(creature) && !MjolnirPin.isPinned(creature) && !creature.getType().is(ModTags.PULL_IMMUNE);
    }

    // ---- hurting and pushing ----

    /**
     * Hurts {@code victim} with a damage of the stones, blamed on {@code caster} (and struck by {@code direct}: the
     * sword, a meteor; the caster itself if null). A hit of the stones always lands: it forgets the damage cooldown of
     * the last blow the victim took (and starts the usual one), so that two jabs eight ticks apart are two jabs.
     *
     * @return whether the damage landed
     */
    public static boolean hurt(ServerLevel level, LivingEntity caster, Entity victim, ResourceKey<DamageType> type, float amount, @Nullable Entity direct) {
        DamageSource source = ModDamageTypes.source(level, type, caster, direct != null ? direct : caster);
        if (victim instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        return victim.hurt(source, amount);
    }

    /** Throws the creature away from {@code from} by {@code strength} (what its knockback resistance leaves), lifting it by {@code lift};
     * not a Creative or Spectator player, and not a creature Mjolnir lies on. */
    public static void shove(LivingEntity target, Vec3 from, double strength, double lift) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator()) || MjolnirPin.isPinned(target)) {
            return;
        }
        target.knockback(strength, from.x - target.getX(), from.z - target.getZ());
        if (lift > 0.0 && target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) < 1.0) {
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, Math.max(motion.y, lift), motion.z);
        }
        target.hurtMarked = true;
    }

    /** A shield held up against the blow is disabled for {@code ticks} (the film: the sword shatters Cap's shield). */
    public static void disableShield(LivingEntity target, int ticks) {
        if (target instanceof Player player && player.isBlocking()) {
            player.getCooldowns().addCooldown(Items.SHIELD, ticks);
            player.stopUsingItem();
            player.level().broadcastEntityEvent(player, (byte) 30);
        }
    }

    // ---- beams ----

    /**
     * The result of one tick of a beam: where it ended, whether a Chaos Shield or a block stopped it, and the creatures
     * it hit for the first time.
     */
    public record BeamHit(Vec3 end, boolean shield, boolean block, List<LivingEntity> hit) {
    }

    /**
     * One tick of a beam from {@code from} along {@code direction}: it ends at the first block (if {@code
     * stopsAtBlocks}) or Chaos Shield, and hurts every creature along it that the caster may hurt and that is not in
     * {@code alreadyHit} (which it adds them to; a Chaos Shield is paid for once, the first tick it is met).
     *
     * @param width      how wide the beam is
     * @param shieldCost what a Chaos Shield loses stopping the beam (once)
     * @param push       how hard it throws what it hits along its way (0 for none)
     */
    public static BeamHit beam(ServerLevel level, LivingEntity caster, Vec3 from, Vec3 direction, double length, double width, float damage,
                               ResourceKey<DamageType> type, float shieldCost, double push, IntSet alreadyHit, boolean stopsAtBlocks) {
        Vec3 to = from.add(direction.normalize().scale(length));
        boolean block = false;
        boolean shield = false;
        if (stopsAtBlocks) {
            BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            if (wall.getType() != HitResult.Type.MISS) {
                to = wall.getLocation();
                block = true;
            }
        }
        WandaAbilities.ShieldStop stop = WandaAbilities.shieldAcross(level, caster, from, to, 1.0F);
        if (stop != null) {
            to = stop.point();
            shield = true;
            block = false;
            if (stop.owner() instanceof ServerPlayer wanda && alreadyHit.add(wanda.getId())) {
                WandaAbilities.wear(wanda, shieldCost, caster);
            }
        }
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 end = to;
        Vec3 start = from;
        AABB reach = new AABB(start, end).inflate(width + 1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reach, e -> e != caster && e.isAlive() && !e.isSpectator()
                && !alreadyHit.contains(e.getId()) && AreaTargets.mayHurt(caster, e, false))) {
            AABB box = e.getBoundingBox().inflate(width * 0.5 + 0.15);
            if (!box.contains(start) && box.clip(start, end).isEmpty()) {
                continue;
            }
            alreadyHit.add(e.getId());
            if (e instanceof ServerPlayer wanda && WandaAbilities.hasShieldUp(wanda)) {
                // A wide beam that grazes the sphere without its centre line meeting it is stopped by it just the same.
                WandaAbilities.wear(wanda, shieldCost, caster);
                shield = true;
                continue;
            }
            hit.add(e);
            if (hurt(level, caster, e, type, damage, caster) && push > 0.0) {
                shove(e, start, push, 0.2);
            }
        }
        return new BeamHit(end, shield, block, hit);
    }

    // ---- areas ----

    /** The creatures the caster may hurt within {@code radius} of {@code center} (a sphere). */
    public static List<LivingEntity> around(ServerLevel level, LivingEntity caster, Vec3 center, double radius, Predicate<LivingEntity> extra) {
        AABB box = new AABB(center, center).inflate(radius);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive() && !e.isSpectator()
                && AreaTargets.mayHurt(caster, e, false) && extra.test(e))) {
            Vec3 middle = e.position().add(0, e.getBbHeight() * 0.5, 0);
            if (middle.distanceToSqr(center) <= (radius + e.getBbWidth() * 0.5) * (radius + e.getBbWidth() * 0.5)) {
                found.add(e);
            }
        }
        return found;
    }

    /**
     * A burst: everything the caster may hurt within {@code radius} of {@code center} takes {@code damage} and is thrown
     * away from the centre.
     *
     * @param groundedOnly only creatures on the ground (or less than half a block above it) are hit
     * @return the creatures hit
     */
    public static List<LivingEntity> splash(ServerLevel level, LivingEntity caster, Vec3 center, double radius, float damage,
                                            ResourceKey<DamageType> type, double push, double lift, @Nullable Entity direct, boolean groundedOnly) {
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity e : around(level, caster, center, radius, e -> !groundedOnly || e.getY() - center.y <= 0.5)) {
            if (hurt(level, caster, e, type, damage, direct)) {
                shove(e, center, push, lift);
                hit.add(e);
            }
        }
        return hit;
    }

    /**
     * A column of power rising from {@code base}: everything the caster may hurt within {@code radius} of its axis, from
     * {@code below} blocks under the base to {@code above} over it (a flier is not safe above a sigil), takes {@code
     * damage} and is thrown.
     */
    public static List<LivingEntity> column(ServerLevel level, LivingEntity caster, Vec3 base, double radius, double below, double above,
                                            float damage, ResourceKey<DamageType> type, double push, double lift, @Nullable Entity direct) {
        List<LivingEntity> hit = new ArrayList<>();
        AABB box = new AABB(base.x - radius - 1.0, base.y - below - 1.0, base.z - radius - 1.0, base.x + radius + 1.0, base.y + above + 1.0, base.z + radius + 1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive() && !e.isSpectator()
                && AreaTargets.mayHurt(caster, e, false))) {
            double reach = radius + e.getBbWidth() * 0.5;
            if (Math.hypot(e.getX() - base.x, e.getZ() - base.z) > reach || e.getY() < base.y - below - e.getBbHeight() || e.getY() > base.y + above) {
                continue;
            }
            if (hurt(level, caster, e, type, damage, direct)) {
                shove(e, base, push, lift);
                hit.add(e);
            }
        }
        return hit;
    }

    /**
     * One tick of a ring that spreads from {@code center}: whoever it passes this tick (between {@code from} and {@code
     * to} blocks out) and is standing on the ground (their feet no more than half a block above where the ring runs) takes
     * {@code damage} once, and is thrown outward. A jump over it is a dodge.
     */
    public static List<LivingEntity> ring(ServerLevel level, LivingEntity caster, Vec3 center, double from, double to, float damage,
                                          ResourceKey<DamageType> type, double push, double lift, IntSet alreadyHit) {
        List<LivingEntity> hit = new ArrayList<>();
        AABB box = new AABB(center, center).inflate(to + 1.5, 3.0, to + 1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive() && !e.isSpectator()
                && !alreadyHit.contains(e.getId()) && AreaTargets.mayHurt(caster, e, false))) {
            double distance = Math.hypot(e.getX() - center.x, e.getZ() - center.z);
            double half = e.getBbWidth() * 0.5;
            if (distance + half < from || distance - half > to || e.getY() - center.y > 0.5 || e.getY() - center.y < -2.5) {
                continue;
            }
            alreadyHit.add(e.getId());
            if (hurt(level, caster, e, type, damage, caster)) {
                shove(e, center, push, lift);
                hit.add(e);
            }
        }
        return hit;
    }

    // ---- blocks ----

    /**
     * A small crater at {@code at}, only if the server asked for block damage ({@code blockDamage}) <em>and</em> the
     * {@code mobGriefing} rule is on: an explosion that harms nobody (the damage is the caller's own) and breaks blocks.
     * Everything else Thanos does breaks nothing.
     */
    public static void craterIfAllowed(ServerLevel level, LivingEntity caster, Vec3 at, float radius) {
        if (!ThanosBossConfig.BLOCK_DAMAGE.get() || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        level.explode(caster, null, new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return false;
            }
        }, at.x, at.y, at.z, radius, false, Level.ExplosionInteraction.MOB);
    }

    // ---- the Reality Stone ----

    /**
     * Turns the creature to stone for {@code ticks}. A creature that shakes it off (a boss, Creative, Spectator, a Vision
     * phased through everything) is untouched.
     *
     * @return whether it took
     */
    public static boolean petrify(ServerLevel level, LivingEntity caster, LivingEntity target, int ticks) {
        if (untouchable(target) || target.getType().is(ModTags.SPELLBOUND_IMMUNE)) {
            return false;
        }
        boolean applied = target.addEffect(new MobEffectInstance(ModEffects.PETRIFIED, ticks, 0, false, true, true), caster);
        if (applied) {
            ThanosEffects.illusion(level, target.position(), 3.0F, ticks);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 0.7F);
        }
        return applied;
    }

    /**
     * Turns the projectiles within {@code radius} that are heading for {@code center} into harmless bubbles that pop:
     * arrows, tridents, snowballs, fireballs, daggers, shrapnel, potions. Not those of the tag {@code reality_proof}
     * (Mjolnir, Stormbreaker, the stones' own).
     *
     * @return how many were turned
     */
    public static int bubbleProjectiles(ServerLevel level, Vec3 center, double radius, Predicate<Projectile> which) {
        int turned = 0;
        AABB box = new AABB(center, center).inflate(radius);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, p -> !p.getType().is(ModTags.REALITY_PROOF) && which.test(p))) {
            Vec3 toCenter = center.subtract(projectile.position());
            if (projectile.getDeltaMovement().dot(toCenter) <= 0.0 || toCenter.lengthSqr() > radius * radius) {
                continue;
            }
            Vec3 at = projectile.position();
            level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.02);
            level.sendParticles(new com.kolodkingal.enhanced.particle.EnergyParticleOptions(StoneKind.REALITY.core(), 1.4F), at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.02);
            ThanosSounds.REALITY_POP.play(level, at);
            projectile.discard();
            turned++;
        }
        return turned;
    }

    private StonePowers() {
    }
}
