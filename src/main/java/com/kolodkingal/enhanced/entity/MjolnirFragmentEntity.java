package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One fragment of a shattered Mjolnir (Thor: Love and Thunder): a glowing shard of Uru that bursts out of Thor's hand,
 * curves in on the enemy it was sent for, strikes it, and then flies back to him. Thor's {@code MjolnirShatter} sends
 * them out and, when the last one is home, puts the hammer together again in his hand. A fragment with no enemy to
 * strike (nobody was around) just flies out to a point and comes back.
 * <p>
 * Fragments are never saved: the hammer itself is kept in Thor's own data while it is in pieces, so nothing is lost if
 * the world is closed with them in the air.
 */
public class MjolnirFragmentEntity extends Projectile {
    /** Blocks per tick on the way out and on the way home. */
    private static final double OUT_SPEED = 1.9;
    private static final double HOME_SPEED = 2.6;
    /** How sharply the flight bends towards where it is headed (0 = not at all, 1 = at once). */
    private static final double OUT_TURN = 0.3;
    private static final double HOME_TURN = 0.45;
    /** A fragment this close to its enemy has struck it. */
    private static final double REACH = 0.9;
    /** A fragment this close to Thor's hand is home. */
    private static final double HOME_REACH = 1.5;
    /** Even if something goes wrong, a fragment never flies longer than this (ticks). */
    private static final int MAX_LIFE = 160;

    @Nullable
    private UUID targetId;
    /** Where a fragment with no enemy flies to before it turns back. */
    private Vec3 ringPoint = Vec3.ZERO;
    private boolean returning;
    private int life;

    public MjolnirFragmentEntity(EntityType<? extends MjolnirFragmentEntity> type, Level level) {
        super(type, level);
        noCulling = true;
        noPhysics = true;
    }

    /**
     * A fragment bursting out of {@code from} with {@code burst} as its first velocity, sent for {@code target} (or, if
     * that is null, to {@code ringPoint}).
     */
    public MjolnirFragmentEntity(ServerLevel level, ServerPlayer thor, Vec3 from, Vec3 burst, @Nullable LivingEntity target, Vec3 ringPoint) {
        this(ModEntities.MJOLNIR_FRAGMENT.get(), level);
        setOwner(thor);
        setPos(from);
        setDeltaMovement(burst);
        this.targetId = target == null ? null : target.getUUID();
        this.ringPoint = ringPoint;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Turns it around, homeward (Thor called it back, or something turned it away). */
    public void setReturning() {
        returning = true;
    }

    /** Whether it is on its way home already (it struck its enemy, or was called or turned back). Server side. */
    public boolean isReturning() {
        return returning;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        if (level().isClientSide) {
            // The server sends the real position every tick; meanwhile keep it moving smoothly and crackling.
            setPos(from.add(getDeltaMovement()));
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 0.2, getY() + (random.nextDouble() - 0.5) * 0.2,
                        getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(new EnergyParticleOptions(AbilityColors.shade(getOwner(), HeroAbility.THOR_SHATTER,
                        random.nextBoolean() ? Power.THOR.color : Power.THOR.glow), 0.7F), getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        ServerPlayer thor = thor();
        if (thor == null || ++life > MAX_LIFE) {
            discard();
            return;
        }
        Vec3 hand = thor.getEyePosition().subtract(0, 0.5, 0);
        LivingEntity target = target(level);
        if (!returning && targetId != null && target == null) {
            // The enemy is dead, or gone: nothing to strike.
            returning = true;
        }
        Vec3 aim;
        if (returning) {
            aim = hand;
        } else if (target != null) {
            aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
        } else {
            aim = ringPoint;
        }
        Vec3 velocity = getDeltaMovement();
        double distance = from.distanceTo(aim);
        if (returning && distance < HOME_REACH) {
            arrive(level, thor);
            return;
        }
        if (!returning && distance < Math.max(REACH, velocity.length())) {
            if (target != null) {
                strike(level, thor, target);
            } else {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.2);
            }
            returning = true;
            aim = hand;
            distance = from.distanceTo(aim);
        }
        double speed = returning ? HOME_SPEED : OUT_SPEED;
        Vec3 wanted = distance < 1.0E-4 ? velocity : aim.subtract(from).scale(speed / distance);
        velocity = velocity.lerp(wanted, returning ? HOME_TURN : OUT_TURN);
        setPos(from.add(velocity));
        setDeltaMovement(velocity);
        hasImpulse = true;
    }

    /** The enemy this fragment was sent for, if it is still alive and here. */
    @Nullable
    private LivingEntity target(ServerLevel level) {
        return targetId != null && level.getEntity(targetId) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /** Thor, if he is still here to catch it: online, alive, in this dimension. */
    @Nullable
    private ServerPlayer thor() {
        return getOwner() instanceof ServerPlayer player && player.isAlive() && !player.isRemoved() && player.level() == level() ? player : null;
    }

    /** A fragment hits its enemy: damage and knockback, thunder in the God of Thunder's case. */
    private void strike(ServerLevel level, ServerPlayer thor, LivingEntity target) {
        Vec3 heading = getDeltaMovement();
        if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.MJOLNIR, thor, this), EnhancedConfig.MJOLNIR_SHATTER_DAMAGE.get().floatValue())) {
            if (heading.horizontalDistanceSqr() > 1.0E-6) {
                target.knockback(1.0, -heading.x, -heading.z);
            }
            if (Powers.get(thor).toggle(Toggle.GOD_OF_THUNDER)) {
                ThorAbilities.arc(level, thor, position(), target);
            }
        }
        level.playSound(null, getX(), getY(), getZ(), ModSounds.MJOLNIR_IMPACT.get(), SoundSource.PLAYERS, 0.8F, 1.35F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 14, 0.15, 0.15, 0.15, 0.3);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(getOwner(), HeroAbility.THOR_SHATTER, Power.THOR.glow), 1.2F), getX(), getY(), getZ(),
                6, 0.1, 0.1, 0.1, 0.12);
    }

    /** Home: a spark, and the fragment is part of the hammer again. */
    private void arrive(ServerLevel level, ServerPlayer thor) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.1);
        level.playSound(null, thor.getX(), thor.getY(), thor.getZ(), ModSounds.MJOLNIR_RETURN.get(), SoundSource.PLAYERS, 0.15F, 1.8F);
        discard();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Never saved (see the entity type), but a stray one that loads anyway has nothing to come back to.
        discard();
    }
}
