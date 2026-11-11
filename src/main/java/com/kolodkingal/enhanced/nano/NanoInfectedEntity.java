package com.kolodkingal.enhanced.nano;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A person the nanotech has taken (3.12): fights for Nano Ultron (the boss's, against everyone) or for a player who is
 * Ultron in nanotech (his, against whatever he fights and whatever fights him; never against him). Freed
 * ({@link #free}), he is himself again: a villager.
 */
public class NanoInfectedEntity extends Monster {
    @Nullable
    private UUID owner;
    private int life = -1;

    public NanoInfectedEntity(EntityType<? extends NanoInfectedEntity> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && (owner == null || !owner.equals(getLastHurtByMob() == null ? null : getLastHurtByMob().getUUID()));
            }
        });
        targetSelector.addGoal(2, new ForOwner());
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return owner == null && super.canUse();
            }
        });
    }

    /** He fights for this player (Ultron in nanotech), for {@code ticks} ticks (-1: as long as he lives). */
    public void serve(UUID owner, int ticks) {
        this.owner = owner;
        this.life = ticks;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && life > 0 && --life == 0) {
            free();
        }
    }

    /** Himself again: a villager in his place. */
    public void free() {
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        Villager villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
        if (villager != null) {
            villager.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            level.addFreshEntity(villager);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 10, 0.3, 0.5, 0.3, 0.0);
        discard();
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (owner != null && (owner.equals(other.getUUID())
                || other instanceof NanoInfectedEntity n && owner.equals(n.owner()))) {
            return true;
        }
        if (owner == null && (other instanceof NanoUltronEntity || other instanceof NanoInfectedEntity n && n.owner() == null)) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    /** His player's fight is his: whom the player hit, and whoever hit the player. */
    private final class ForOwner extends TargetGoal {
        private LivingEntity found;

        ForOwner() {
            super(NanoInfectedEntity.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (owner == null || !(level() instanceof ServerLevel level)) {
                return false;
            }
            Player player = level.getPlayerByUUID(owner);
            if (player == null) {
                return false;
            }
            LivingEntity t = player.getLastHurtMob();
            if (t == null || !t.isAlive()) {
                t = player.getLastHurtByMob();
            }
            found = t != null && t.isAlive() && t != NanoInfectedEntity.this && !isAlliedTo(t) ? t : null;
            return found != null;
        }

        @Override
        public void start() {
            mob.setTarget(found);
            super.start();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return owner == null && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("Life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        life = tag.contains("Life") ? tag.getInt("Life") : -1;
    }
}
