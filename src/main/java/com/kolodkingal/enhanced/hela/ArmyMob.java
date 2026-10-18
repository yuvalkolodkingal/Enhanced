package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * What Hela's dead have in common, the berserkers and Fenris (3.5): they climb out of the floor ({@link #riseTicks}: they
 * neither move nor strike until they stand), they belong either to a fight (the boss's army: marked by the fight, they go
 * after its fighters and the Asgardians who stand against them) or to a hero Hela ({@link #owner}: they attack what she
 * attacks or what attacks her, never her or her side, follow her, and crumble when their time is over or she is gone), and
 * nothing of them is ever saved, dropped or burned by the sun.
 */
public abstract class ArmyMob extends Monster {
    /** The game time at which the rising out of the floor is over (synced: the client draws it). */
    private static final EntityDataAccessor<Long> DATA_RISEN_AT = SynchedEntityData.defineId(ArmyMob.class, EntityDataSerializers.LONG);
    /** The hero's ally has a little gold-green mote over it: whose it is is drawn by nothing else. */
    private static final EntityDataAccessor<Boolean> DATA_ALLY = SynchedEntityData.defineId(ArmyMob.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerId;
    private long expiresAt = Long.MAX_VALUE;

    protected ArmyMob(EntityType<? extends ArmyMob> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RISEN_AT, 0L);
        builder.define(DATA_ALLY, false);
    }

    /** How long it takes to climb out of the floor. */
    public abstract int riseTicks();

    /** It begins to climb out of the floor now. */
    public void beginRise() {
        entityData.set(DATA_RISEN_AT, level().getGameTime() + riseTicks());
    }

    /** How far it has risen at the game time {@code time} (with the partial tick), 0 to 1. */
    public float risen(float time) {
        long at = entityData.get(DATA_RISEN_AT);
        if (at == 0L) {
            return 1.0F;
        }
        float left = at - time;
        return left <= 0.0F ? 1.0F : Math.max(0.0F, 1.0F - left / riseTicks());
    }

    /** Whether it is still climbing out of the floor. */
    public boolean rising() {
        return level().getGameTime() < entityData.get(DATA_RISEN_AT);
    }

    // ---- a hero's ally ----

    /** Makes it an ally of {@code owner} for {@code ticks}. */
    public void serve(ServerPlayer owner, int ticks) {
        ownerId = owner.getUUID();
        expiresAt = level().getGameTime() + ticks;
        entityData.set(DATA_ALLY, true);
    }

    @Nullable
    public UUID ownerId() {
        return ownerId;
    }

    /** Whether it is a hero's ally (synced). */
    public boolean isAlly() {
        return entityData.get(DATA_ALLY);
    }

    /** The hero it serves, if she is here. */
    @Nullable
    public Player owner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    /** The game time its service ends. */
    public long expiresAt() {
        return expiresAt;
    }

    /** Whether {@code other} is on its side: its hero, her team, her other dead; for the boss's army, its fight. */
    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == this) {
            return true;
        }
        if (ownerId != null) {
            Player owner = owner();
            if (other.getUUID().equals(ownerId) || other instanceof ArmyMob mob && ownerId.equals(mob.ownerId)) {
                return true;
            }
            return owner != null && owner.isAlliedTo(other);
        }
        return BossFights.sameFight(this, other) || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (!super.canAttack(target)) {
            return false;
        }
        LivingEntity owner = ownerId != null ? owner() : this;
        return owner != null && HelaRules.canHurt(owner, target) && !(ownerId == null && target instanceof ArmyMob mob && mob.ownerId == null);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (rising() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.6), getY() + 0.1, getRandomZ(0.6), 0.0, 0.03, 0.0);
            }
            return;
        }
        if (ownerId != null) {
            Player owner = owner();
            if (owner == null || !owner.isAlive() || level().getGameTime() >= expiresAt) {
                crumble();
                return;
            }
        } else if (BossFights.fightOf(this) == null && !getPersistentData().getBoolean(HelaArmy.LOOSE_TAG)) {
            // The boss's army outlives no fight (a fight's entities are discarded when it finishes; this is a stray one).
            crumble();
            return;
        }
        if (rising()) {
            getNavigation().stop();
            setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
        }
    }

    /** It falls to dust: black smoke and green motes, gone. */
    public void crumble() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.5, getZ(), 14, getBbWidth() * 0.4,
                    getBbHeight() * 0.3, getBbWidth() * 0.4, 0.02);
            server.sendParticles(new EnergyParticleOptions(HelaRules.GREEN, 0.8F), getX(), getY() + getBbHeight() * 0.5, getZ(), 8,
                    getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.04);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && !source.is(DamageTypes.GENERIC_KILL)) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.5, getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        }
    }

    /** Never a stray: the fight or the hero keeps them, and nothing of them is saved. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return ownerId == null;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The goals they share

    /** The targets of the boss's army: the players of the fight, the Asgardians who stand in its way. */
    protected void addArmyTargets(int priority) {
        targetSelector.addGoal(priority, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> ownerId == null && HelaRules.canHurt(this, p)));
        targetSelector.addGoal(priority + 1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                m -> ownerId == null && m.getType().is(ModTags.ASGARDIANS) && m.distanceToSqr(this) < 16.0 * 16.0 && HelaRules.canHurt(this, m)));
    }

    /** The hero's ally takes her target: what she struck last, else what struck her last (within 24 blocks, recently). */
    protected static final class OwnerTargetGoal extends Goal {
        private static final int MEMORY = 120;
        private final ArmyMob mob;
        private int next;

        OwnerTargetGoal(ArmyMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (mob.ownerId == null || mob.rising() || --next > 0) {
                return false;
            }
            next = 10;
            LivingEntity want = choice();
            return want != null && want != mob.getTarget();
        }

        @Nullable
        private LivingEntity choice() {
            Player owner = mob.owner();
            if (owner == null) {
                return null;
            }
            LivingEntity struck = owner.getLastHurtMob();
            if (struck != null && owner.tickCount - owner.getLastHurtMobTimestamp() <= MEMORY && fits(owner, struck)) {
                return struck;
            }
            LivingEntity hurtBy = owner.getLastHurtByMob();
            if (hurtBy != null && owner.tickCount - owner.getLastHurtByMobTimestamp() <= MEMORY && fits(owner, hurtBy)) {
                return hurtBy;
            }
            LivingEntity mine = mob.getLastHurtByMob();
            return mine != null && fits(owner, mine) ? mine : null;
        }

        private boolean fits(Player owner, LivingEntity target) {
            return target.isAlive() && target.distanceToSqr(mob) <= 24.0 * 24.0 && HelaRules.canHurt(owner, target);
        }

        @Override
        public void start() {
            mob.setTarget(choice());
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    /** The hero's ally keeps near her: it walks to her past 10 blocks and is at her side past 28. */
    protected static final class FollowOwnerGoal extends Goal {
        private final ArmyMob mob;
        private final double speed;
        private int repath;

        FollowOwnerGoal(ArmyMob mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player owner = mob.owner();
            return owner != null && !mob.rising() && mob.getTarget() == null && owner.distanceToSqr(mob) > 10.0 * 10.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = mob.owner();
            return owner != null && mob.getTarget() == null && owner.distanceToSqr(mob) > 4.0 * 4.0;
        }

        @Override
        public void tick() {
            Player owner = mob.owner();
            if (owner == null) {
                return;
            }
            if (owner.distanceToSqr(mob) > 28.0 * 28.0) {
                Vec3 at = HelaRules.onGround(mob.level(), owner.position().add(owner.getLookAngle().scale(-1.5)));
                mob.moveTo(at.x, at.y, at.z, mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                return;
            }
            if (--repath <= 0) {
                repath = 20;
                mob.getNavigation().moveTo(owner, speed);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }
}
