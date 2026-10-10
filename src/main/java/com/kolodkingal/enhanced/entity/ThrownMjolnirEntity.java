package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Mjolnir in flight. Thrown, it flies straight, smashing everything in its path, until it hits a wall or reaches the end
 * of its range; then it turns back and flies home to Thor's hand, straight through walls, hitting everything on the way
 * back too. If Thor is gone (dead, logged out, in another dimension) it drops to the ground and stays there, like any
 * Mjolnir nobody holds.
 */
public class ThrownMjolnirEntity extends Projectile {
    private static final EntityDataAccessor<ItemStack> DATA_HAMMER =
            SynchedEntityData.defineId(ThrownMjolnirEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(ThrownMjolnirEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double THROW_SPEED = 2.0;
    public static final double RETURN_SPEED = 2.6;
    /** Even if something goes wrong, a hammer never flies longer than this (ticks) before it drops. */
    private static final int MAX_LIFE = 400;

    private final IntSet hitOut = new IntOpenHashSet();
    private final IntSet hitBack = new IntOpenHashSet();
    private double travelled;
    private int life;

    public ThrownMjolnirEntity(EntityType<? extends ThrownMjolnirEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    /** A hammer thrown by Thor from his eyes along his view. */
    public ThrownMjolnirEntity(ServerLevel level, ServerPlayer thor, ItemStack hammer) {
        this(ModEntities.THROWN_MJOLNIR.get(), level);
        setOwner(thor);
        setHammer(hammer);
        Vec3 look = thor.getLookAngle();
        setPos(thor.getEyePosition().add(look.scale(0.6)).subtract(0, 0.2, 0));
        setDeltaMovement(look.scale(THROW_SPEED));
    }

    /** A hammer Thor called back: it is already on its way home from {@code from}. */
    public static ThrownMjolnirEntity recalled(ServerLevel level, ServerPlayer thor, ItemStack hammer, Vec3 from) {
        ThrownMjolnirEntity e = new ThrownMjolnirEntity(ModEntities.THROWN_MJOLNIR.get(), level);
        e.setOwner(thor);
        e.setHammer(hammer);
        e.setPos(from);
        e.setReturning(true);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HAMMER, ItemStack.EMPTY);
        builder.define(DATA_RETURNING, false);
    }

    public ItemStack hammer() {
        ItemStack stack = entityData.get(DATA_HAMMER);
        return stack.isEmpty() ? new ItemStack(ModItems.MJOLNIR.get()) : stack;
    }

    private void setHammer(ItemStack stack) {
        entityData.set(DATA_HAMMER, stack.copyWithCount(1));
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    /** Turns it around, homeward. */
    public void setReturning(boolean returning) {
        entityData.set(DATA_RETURNING, returning);
        noPhysics = returning;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        if (level().isClientSide) {
            // The server sends the real position every tick; meanwhile keep it moving smoothly and crackling.
            setPos(from.add(getDeltaMovement()));
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        ServerPlayer thor = thor();
        if (++life > MAX_LIFE || (thor == null && isReturning())) {
            drop();
            return;
        }
        Vec3 motion;
        if (!isReturning()) {
            motion = getDeltaMovement();
            Vec3 to = from.add(motion);
            BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean hitWall = wall.getType() != HitResult.Type.MISS;
            if (hitWall) {
                to = wall.getLocation();
            }
            smash(level, from, to, hitOut);
            setPos(to);
            travelled += to.distanceTo(from);
            if (hitWall) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 0.7F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.3);
            }
            if (hitWall || travelled >= EnhancedConfig.MJOLNIR_THROW_RANGE.get()) {
                if (thor == null) {
                    drop();
                    return;
                }
                setReturning(true);
            }
        } else {
            Vec3 hand = thor.getEyePosition().subtract(0, 0.5, 0);
            Vec3 toHand = hand.subtract(from);
            double distance = toHand.length();
            if (distance < 1.6) {
                catchBy(thor);
                return;
            }
            motion = toHand.scale(Math.min(RETURN_SPEED, distance) / distance);
            Vec3 to = from.add(motion);
            smash(level, from, to, hitBack);
            setPos(to);
        }
        setDeltaMovement(motion);
        hasImpulse = true;
    }

    /** Everything along the segment that hasn't been hit on this leg takes the full force of the hammer. */
    private void smash(ServerLevel level, Vec3 from, Vec3 to, IntSet alreadyHit) {
        Entity owner = getOwner();
        AABB sweep = new AABB(from, to).inflate(0.8);
        for (Entity e : level.getEntities(this, sweep, e -> e != owner && e.isAlive() && !e.isSpectator()
                && (e instanceof LivingEntity || e instanceof EndCrystal) && !alreadyHit.contains(e.getId()))) {
            AABB box = e.getBoundingBox().inflate(0.5);
            if (!box.contains(from) && box.clip(from, to).isEmpty()) {
                continue;
            }
            alreadyHit.add(e.getId());
            float damage = EnhancedConfig.MJOLNIR_THROW_DAMAGE.get().floatValue();
            if (e.hurt(ModDamageTypes.source(level, ModDamageTypes.MJOLNIR, owner, this), damage)) {
                Vec3 push = to.subtract(from);
                if (e instanceof LivingEntity living && push.lengthSqr() > 1.0E-6) {
                    living.knockback(1.3, -push.x, -push.z);
                }
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6F, 1.4F);
                if (owner instanceof ServerPlayer thor && Powers.get(thor).toggle(Toggle.GOD_OF_THUNDER)) {
                    ThorAbilities.arc(level, thor, position(), e);
                }
            }
        }
    }

    /** Thor, if he can still catch it: alive, in this dimension, still Thor. */
    private ServerPlayer thor() {
        return getOwner() instanceof ServerPlayer player && player.isAlive() && !player.isRemoved() && player.level() == level()
                && Powers.is(player, Power.THOR) ? player : null;
    }

    /** Back in Thor's hand (or his inventory, if his hand is full). */
    private void catchBy(ServerPlayer thor) {
        ItemStack hammer = hammer().copy();
        if (thor.getMainHandItem().isEmpty()) {
            thor.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        } else if (!thor.getInventory().add(hammer)) {
            thor.drop(hammer, false);
        }
        level().playSound(null, thor.getX(), thor.getY(), thor.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.2F, 0.7F);
        forget();
        discard();
    }

    /** Nobody to fly home to: it falls, and comes to rest wherever it lands. */
    private void drop() {
        ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), hammer().copy());
        item.setDeltaMovement(0, -0.2, 0);
        item.setPickUpDelay(10);
        level().addFreshEntity(item);
        forget();
        discard();
    }

    /** Thor no longer has a hammer in flight. */
    private void forget() {
        if (getOwner() instanceof ServerPlayer player) {
            PowerData data = Powers.get(player);
            if (data.hammerEntityId() == getId()) {
                data.setHammerEntityId(-1);
                Powers.syncIfDirty(player);
            }
        }
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
        super.addAdditionalSaveData(tag);
        tag.put("Hammer", hammer().save(registryAccess()));
        tag.putBoolean("Returning", isReturning());
        tag.putDouble("Travelled", travelled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHammer(ItemStack.parseOptional(registryAccess(), tag.getCompound("Hammer")));
        setReturning(tag.getBoolean("Returning"));
        travelled = tag.getDouble("Travelled");
    }
}
