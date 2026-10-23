package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
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
 * Mjolnir or Stormbreaker in flight. Thrown, it flies straight, smashing everything in its path, until it hits a wall or
 * reaches the end of its range; then it turns back and flies home to Thor's hand, straight through walls, hitting
 * everything on the way back too. If Thor is gone (dead, logged out, in another dimension) it drops to the ground, like
 * any weapon nobody holds (Mjolnir then comes to rest as a block).
 */
public class ThrownWeaponEntity extends Projectile {
    private static final EntityDataAccessor<ItemStack> DATA_WEAPON =
            SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double THROW_SPEED = 2.0;
    public static final double RETURN_SPEED = 2.6;
    /** Even if something goes wrong, a weapon never flies longer than this (ticks) before it drops. */
    private static final int MAX_LIFE = 400;

    private final IntSet hitOut = new IntOpenHashSet();
    private final IntSet hitBack = new IntOpenHashSet();
    private double travelled;
    private int life;

    public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    /** A weapon thrown by Thor from his eyes along his view. */
    public ThrownWeaponEntity(ServerLevel level, ServerPlayer thor, ItemStack weapon) {
        this(ModEntities.THROWN_WEAPON.get(), level);
        setOwner(thor);
        setWeapon(weapon);
        Vec3 look = thor.getLookAngle();
        setPos(thor.getEyePosition().add(look.scale(0.6)).subtract(0, 0.2, 0));
        setDeltaMovement(look.scale(THROW_SPEED));
    }

    /** A weapon Thor called back: it is already on its way home from {@code from}. */
    public static ThrownWeaponEntity recalled(ServerLevel level, ServerPlayer thor, ItemStack weapon, Vec3 from) {
        ThrownWeaponEntity e = new ThrownWeaponEntity(ModEntities.THROWN_WEAPON.get(), level);
        e.setOwner(thor);
        e.setWeapon(weapon);
        e.setPos(from);
        e.setReturning(true);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WEAPON, ItemStack.EMPTY);
        builder.define(DATA_RETURNING, false);
    }

    public ItemStack weapon() {
        ItemStack stack = entityData.get(DATA_WEAPON);
        return stack.isEmpty() ? new ItemStack(ModItems.MJOLNIR.get()) : stack;
    }

    private void setWeapon(ItemStack stack) {
        entityData.set(DATA_WEAPON, stack.copyWithCount(1));
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
        if (level().isClientSide()) {
            // The server sends the real position every tick; meanwhile keep it moving smoothly and crackling.
            setPos(from.add(getDeltaMovement()));
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
            }
            // 3.5: a custom hero's throw trails the colour his pick of the hammer chose.
            int tint = AbilityColors.chosen(getOwner(), HeroAbility.THOR_HAMMER);
            if (tint >= 0) {
                level().addParticle(new EnergyParticleOptions(tint, 0.8F), getX(), getY(), getZ(), 0, 0, 0);
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
                // A hair short of the face it struck, so it stays on its own side of the wall (a face can be the border
                // of a chunk that does not tick, where it would hang in the air and never fly home).
                to = wall.getLocation().subtract(motion.normalize().scale(0.05));
            }
            smash(level, from, to, hitOut);
            setPos(to);
            travelled += to.distanceTo(from);
            if (hitWall) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.MJOLNIR_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
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

    /** Everything along the segment that hasn't been hit on this leg takes the full force of the weapon. */
    private void smash(ServerLevel level, Vec3 from, Vec3 to, IntSet alreadyHit) {
        Entity owner = getOwner();
        boolean stormbreaker = ThorAbilities.isStormbreaker(weapon());
        float damage = (stormbreaker ? EnhancedConfig.STORMBREAKER_THROW_DAMAGE : EnhancedConfig.MJOLNIR_THROW_DAMAGE).get().floatValue();
        ResourceKey<DamageType> type = stormbreaker ? ModDamageTypes.STORMBREAKER : ModDamageTypes.MJOLNIR;
        AABB sweep = new AABB(from, to).inflate(0.8);
        for (Entity e : level.getEntities(this, sweep, e -> e != owner && e.isAlive() && !e.isSpectator()
                && (e instanceof LivingEntity || e instanceof EndCrystal) && !alreadyHit.contains(e.getId()))) {
            AABB box = e.getBoundingBox().inflate(0.5);
            if (!box.contains(from) && box.clip(from, to).isEmpty()) {
                continue;
            }
            alreadyHit.add(e.getId());
            if (e.hurt(ModDamageTypes.source(level, type, owner, this), damage)) {
                Vec3 push = to.subtract(from);
                if (e instanceof LivingEntity living && push.lengthSqr() > 1.0E-6) {
                    living.knockback(1.3, -push.x, -push.z);
                }
                level.playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.MJOLNIR_IMPACT.get(), SoundSource.PLAYERS, 0.9F,
                        stormbreaker ? 1.15F : 1.0F);
                if (owner instanceof ServerPlayer thor && Powers.get(thor).toggle(Toggle.GOD_OF_THUNDER)) {
                    ThorAbilities.arc(level, thor, position(), e);
                }
            }
        }
    }

    /** Thor, if he can still catch it: alive, in this dimension, still Thor (3.4: or a custom hero with Thor's hammer). */
    private ServerPlayer thor() {
        // (3.8: Love's axe throw is this entity too: it flies home to her as to Thor.)
        return getOwner() instanceof ServerPlayer player && player.isAlive() && !player.isRemoved() && player.level() == level()
                && (Powers.get(player).has(HeroAbility.THOR_HAMMER) || Powers.get(player).has(HeroAbility.LOVE_AXE))
                ? player : null;
    }

    /** Back in Thor's hand (or his inventory, if his hand is full). A Mjolnir nobody owned yet is his from now on. */
    private void catchBy(ServerPlayer thor) {
        ItemStack weapon = weapon().copy();
        Worthiness.claim(thor, weapon);
        if (thor.getMainHandItem().isEmpty()) {
            thor.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        } else if (!thor.getInventory().add(weapon)) {
            thor.drop(weapon, false);
        }
        level().playSound(null, thor.getX(), thor.getY(), thor.getZ(), ModSounds.MJOLNIR_RETURN.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        forget();
        discard();
    }

    /** Nobody to fly home to: it falls, and comes to rest wherever it lands. */
    private void drop() {
        ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), weapon().copy());
        item.setDeltaMovement(0, -0.2, 0);
        item.setPickUpDelay(10);
        level().addFreshEntity(item);
        forget();
        discard();
    }

    /** Thor no longer has a weapon in flight. */
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
        tag.put("Weapon", weapon().save(registryAccess()));
        tag.putBoolean("Returning", isReturning());
        tag.putDouble("Travelled", travelled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setWeapon(ItemStack.parseOptional(registryAccess(), tag.getCompound("Weapon")));
        setReturning(tag.getBoolean("Returning"));
        travelled = tag.getDouble("Travelled");
    }
}
