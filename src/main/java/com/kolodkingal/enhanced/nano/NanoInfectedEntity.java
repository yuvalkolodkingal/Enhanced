package com.kolodkingal.enhanced.nano;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
 * ({@link #free}), he is himself again: a villager (or a golem).
 *
 * <p>3.12.2: what the nanotech took ({@link Kind}): a villager, an iron golem or a zombie, drawn as one with the
 * nanotech's orange and blue threads over it.
 */
public class NanoInfectedEntity extends Monster {
    /** What the nanotech took. (PERSON: 3.12's grey people, still in old worlds.) */
    public enum Kind { PERSON, VILLAGER, GOLEM, ZOMBIE }

    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_KIND =
            net.minecraft.network.syncher.SynchedEntityData.defineId(NanoInfectedEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BYTE);
    private static final ResourceLocation GOLEM_HEALTH = com.kolodkingal.enhanced.Enhanced.id("nano_infected_golem_health");

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
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, (byte) Kind.PERSON.ordinal());
    }

    public Kind kind() {
        int k = entityData.get(DATA_KIND);
        return k >= 0 && k < Kind.values().length ? Kind.values()[k] : Kind.PERSON;
    }

    /** What the nanotech took; a golem is bigger and stronger. */
    public void setKind(Kind kind) {
        entityData.set(DATA_KIND, (byte) kind.ordinal());
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(GOLEM_HEALTH);
            if (kind == Kind.GOLEM) {
                health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(GOLEM_HEALTH, 36.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                setHealth(getMaxHealth());
            }
        }
        refreshDimensions();
    }

    /** One of the three, as chance will have it: villagers and zombies more often than golems. */
    public static Kind randomKind(net.minecraft.util.RandomSource random) {
        int r = random.nextInt(5);
        return r < 2 ? Kind.VILLAGER : r < 4 ? Kind.ZOMBIE : Kind.GOLEM;
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_KIND.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return kind() == Kind.GOLEM ? net.minecraft.world.entity.EntityDimensions.scalable(1.4F, 2.7F) : super.getDefaultDimensions(pose);
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
        Entity freed = kind() == Kind.GOLEM ? net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level)
                : net.minecraft.world.entity.EntityType.VILLAGER.create(level);
        if (freed != null) {
            freed.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            level.addFreshEntity(freed);
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
        tag.putString("Kind", kind().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        life = tag.contains("Life") ? tag.getInt("Life") : -1;
        try {
            setKind(Kind.valueOf(tag.getString("Kind")));
        } catch (IllegalArgumentException e) {
            setKind(Kind.PERSON);
        }
    }
}
