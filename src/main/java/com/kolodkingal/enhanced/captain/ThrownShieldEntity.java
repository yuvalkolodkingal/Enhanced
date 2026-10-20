package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Captain America's thrown shield (3.8, PLAN 3.4): it flies along his look, strikes the first creature in its path (or
 * a wall, or the end of its range) and turns home, flying straight back through walls to the hand it left (Mjolnir's
 * rule: the flight home mirrors {@code ThrownWeaponEntity}'s). Thrown to ricochet, each hit sends it on to the nearest
 * other foe within {@code ricochetRadius} the hit can see, up to {@code ricochets} times, a little weaker each time;
 * off a wall it bounces once more toward a foe near the wall. If he is gone (dead, logged out, in another dimension, no
 * longer one who throws it) it drops where it is, like any shield nobody holds.
 */
public class ThrownShieldEntity extends Projectile {
    private static final EntityDataAccessor<ItemStack> DATA_SHIELD =
            SynchedEntityData.defineId(ThrownShieldEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(ThrownShieldEntity.class, EntityDataSerializers.BOOLEAN);
    /** The speed it flies home (blocks a tick). */
    public static final double RETURN_SPEED = 2.2;
    /** Within this of his hand it is caught. */
    static final double CATCH_REACH = 1.6;
    /** Even if something goes wrong, a shield never flies longer than this (ticks) before it drops. */
    static final int MAX_LIFE = 400;
    /** How far round its path a creature is struck (the shield is broad). */
    static final double SWEEP = 0.45;

    private final IntSet hit = new IntOpenHashSet();
    private final List<Float> strikes = new ArrayList<>();
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private boolean ricochet;
    private int bouncesLeft;
    private float damage;
    private double speed = 1.8;
    private double range = 24.0;
    private boolean wallBounced;
    private int cooldown;
    private double travelled;
    private int life;
    /** The foe a ricochet is flying to (it steers to him), or null. */
    @Nullable
    private LivingEntity target;

    public ThrownShieldEntity(EntityType<? extends ThrownShieldEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    /**
     * A shield thrown by {@code cap} from {@code hand} along his look.
     *
     * @param ricochet whether it bounces on from foe to foe
     * @param bounces  how many times it may bounce on
     * @param damage   the first strike's damage (each bounce is {@code ricochetFalloff} less)
     * @param cooldown the cooldown his throw starts when he catches it
     */
    public static ThrownShieldEntity thrown(ServerLevel level, ServerPlayer cap, ItemStack shield,
            InteractionHand hand, boolean ricochet, int bounces, float damage, double speed, double range,
            int cooldown) {
        ThrownShieldEntity e = new ThrownShieldEntity(ModEntities.THROWN_SHIELD.get(), level);
        e.setOwner(cap);
        e.setShield(shield);
        e.hand = hand;
        e.ricochet = ricochet;
        e.bouncesLeft = ricochet ? bounces : 0;
        e.damage = damage;
        e.speed = speed;
        e.range = range;
        e.cooldown = cooldown;
        Vec3 look = cap.getLookAngle();
        e.setPos(cap.getEyePosition().add(look.scale(0.6)).subtract(0.0, 0.25, 0.0));
        e.setDeltaMovement(look.scale(speed));
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SHIELD, ItemStack.EMPTY);
        builder.define(DATA_RETURNING, false);
    }

    /** The shield that flies (the Vibranium Shield when none was set). */
    public ItemStack shield() {
        ItemStack stack = entityData.get(DATA_SHIELD);
        return stack.isEmpty() ? new ItemStack(ModItems.VIBRANIUM_SHIELD.get()) : stack;
    }

    private void setShield(ItemStack stack) {
        entityData.set(DATA_SHIELD, stack.copyWithCount(1));
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    /** Turns it around, homeward (through walls). */
    public void setReturning(boolean returning) {
        entityData.set(DATA_RETURNING, returning);
        noPhysics = returning;
        if (returning) {
            target = null;
        }
    }

    /** The hand it left (and goes back to). */
    public InteractionHand hand() {
        return hand;
    }

    /** Whether it was thrown to ricochet. */
    public boolean ricochet() {
        return ricochet;
    }

    /** The damage of each creature it has struck so far, in order. */
    public List<Float> strikes() {
        return List.copyOf(strikes);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        if (level().isClientSide) {
            // The server sends the real position every tick; meanwhile keep it moving smoothly, a faint trail behind.
            setPos(from.add(getDeltaMovement()));
            level().addParticle(new EnergyParticleOptions(tickCount % 2 == 0 ? 0xE8EEF5 : 0x6FA0FF, 0.45F),
                    from.x, from.y, from.z, 0.0, 0.0, 0.0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        ServerPlayer cap = thrower();
        if (++life > MAX_LIFE || cap == null) {
            drop();
            return;
        }
        Vec3 motion;
        if (!isReturning()) {
            if (target != null) {
                if (!target.isAlive() || target.level() != level) {
                    setReturning(true);
                    setDeltaMovement(Vec3.ZERO);
                    hasImpulse = true;
                    return;
                }
                Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
                motion = aim.lengthSqr() < 1.0E-6 ? getDeltaMovement() : aim.normalize().scale(speed);
            } else {
                motion = getDeltaMovement();
            }
            Vec3 to = from.add(motion);
            BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            boolean hitWall = wall.getType() != HitResult.Type.MISS;
            if (hitWall) {
                // A hair short of the face it struck, so it stays on its own side of the wall.
                to = wall.getLocation().subtract(motion.normalize().scale(0.05));
            }
            LivingEntity struck = firstAlong(level, cap, from, to);
            if (struck != null) {
                Vec3 at = struck.getBoundingBox().clip(from, to).orElse(struck.getBoundingBox().getCenter());
                setPos(at);
                travelled += at.distanceTo(from);
                strike(level, cap, struck, from);
                return;
            }
            setPos(to);
            travelled += to.distanceTo(from);
            if (hitWall) {
                CaptainAbilities.sound(level, to, ModSounds.SHIELD_RICOCHET.get(), 0.9F, 1.3F);
                level.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 8, 0.15, 0.15, 0.15, 0.25);
                LivingEntity next = ricochet && !wallBounced ? nextFoe(level, cap, to, null) : null;
                if (next != null) {
                    wallBounced = true;
                    bounceTo(level, cap, to, next);
                    return;
                }
                setReturning(true);
            } else if (travelled >= range && target == null) {
                setReturning(true);
            }
        } else {
            Vec3 hand = cap.getEyePosition().subtract(0.0, 0.5, 0.0);
            Vec3 toHand = hand.subtract(from);
            double distance = toHand.length();
            if (distance < CATCH_REACH) {
                catchBy(level, cap);
                return;
            }
            motion = toHand.scale(Math.min(RETURN_SPEED, distance) / distance);
            setPos(from.add(motion));
        }
        setDeltaMovement(motion);
        hasImpulse = true;
    }

    /** The first creature along the segment it may strike (not one struck already), or null. */
    @Nullable
    private LivingEntity firstAlong(ServerLevel level, ServerPlayer cap, Vec3 from, Vec3 to) {
        AABB sweep = new AABB(from, to).inflate(SWEEP + 0.5);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> !hit.contains(e.getId()) && CaptainAbilities.mayHurt(cap, e))) {
            AABB box = e.getBoundingBox().inflate(SWEEP);
            Optional<Vec3> clip = box.contains(from) ? Optional.of(from) : box.clip(from, to);
            if (clip.isPresent() && clip.get().distanceToSqr(from) < bestDistance) {
                bestDistance = clip.get().distanceToSqr(from);
                best = e;
            }
        }
        return best;
    }

    /** It strikes a creature: the damage and the knock, then on to the next foe (a ricochet) or home. */
    private void strike(ServerLevel level, ServerPlayer cap, LivingEntity victim, Vec3 from) {
        hit.add(victim.getId());
        float amount = Math.max(0.0F, damage);
        if (CaptainAbilities.hurt(cap, victim, ModDamageTypes.CAP_SHIELD, amount, this)) {
            strikes.add(amount);
            CaptainAbilities.push(victim, from, 0.8, 0.1, cap);
        }
        Vec3 at = position();
        CaptainAbilities.sound(level, at, ModSounds.SHIELD_RICOCHET.get(), 1.0F, 1.0F + 0.1F * strikes.size());
        LivingEntity next = ricochet && bouncesLeft > 0 ? nextFoe(level, cap, victim.getBoundingBox().getCenter(),
                victim) : null;
        if (next != null) {
            bouncesLeft--;
            damage -= (float) CaptainAbilities.cfg(CaptainConfig.RICOCHET_FALLOFF, 1.0);
            bounceTo(level, cap, at, next);
            return;
        }
        CaptainAbilities.send(level, this, CaptainFxPayload.RICOCHET, List.of(at), 0);
        setReturning(true);
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
    }

    /** On to the next foe: it steers to him from here. */
    private void bounceTo(ServerLevel level, ServerPlayer cap, Vec3 at, LivingEntity next) {
        target = next;
        Vec3 aim = next.getBoundingBox().getCenter().subtract(at);
        setDeltaMovement(aim.lengthSqr() < 1.0E-6 ? getDeltaMovement() : aim.normalize().scale(speed));
        hasImpulse = true;
        CaptainAbilities.send(level, this, CaptainFxPayload.RICOCHET, List.of(at,
                next.getBoundingBox().getCenter()), bouncesLeft + 1);
        CaptainState cs = cap.getData(ModAttachments.CAPTAIN);
        if (cs.shieldEntity() == getId()) {
            cap.setData(ModAttachments.CAPTAIN, cs.withBouncesLeft(bouncesLeft));
        }
    }

    /**
     * The nearest foe within {@code ricochetRadius} of {@code at} that he may hurt, has not been struck by this throw,
     * and that {@code at} can see (no wall between), or null.
     */
    @Nullable
    private LivingEntity nextFoe(ServerLevel level, ServerPlayer cap, Vec3 at, @Nullable LivingEntity besides) {
        double radius = CaptainAbilities.cfg(CaptainConfig.RICOCHET_RADIUS, 8.0);
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                        e -> e != besides && !hit.contains(e.getId()) && CaptainAbilities.mayHurt(cap, e)
                                && e.getBoundingBox().getCenter().distanceToSqr(at) <= radius * radius
                                && seen(level, at, e))
                .stream()
                .min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at)))
                .orElse(null);
    }

    private boolean seen(ServerLevel level, Vec3 at, LivingEntity e) {
        Vec3 to = e.getBoundingBox().getCenter();
        return level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    /** He who threw it, if he can still catch it: alive, in this dimension, still one who throws it. */
    @Nullable
    private ServerPlayer thrower() {
        if (getOwner() instanceof ServerPlayer player && player.isAlive() && !player.isRemoved()
                && player.level() == level()) {
            return Powers.get(player).has(HeroAbility.CAPTAIN_SHIELD_THROW) ? player : null;
        }
        return null;
    }

    /** Back in the hand it left (or his inventory if that hand is full, or at his feet if that is full too). */
    private void catchBy(ServerLevel level, ServerPlayer cap) {
        ItemStack stack = shield().copy();
        if (cap.getItemInHand(hand).isEmpty()) {
            cap.setItemInHand(hand, stack);
        } else if (!cap.getInventory().add(stack)) {
            cap.drop(stack, false);
        }
        CaptainAbilities.sound(level, cap.position(), ModSounds.SHIELD_CATCH.get(), 1.0F, 1.0F);
        CaptainAbilities.send(level, cap, CaptainFxPayload.CATCH, List.of(position()),
                hand == InteractionHand.OFF_HAND ? 1 : 0);
        PowerData data = Powers.get(cap);
        data.startCooldown(ricochet ? HeroAbility.CAPTAIN_RICOCHET : HeroAbility.CAPTAIN_SHIELD_THROW,
                level.getGameTime(), cooldown);
        forget(cap);
        discard();
    }

    /** Nobody to fly home to (or he lost the power): it falls, and lies wherever it lands. */
    void drop() {
        ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), shield().copy());
        item.setDeltaMovement(0.0, -0.2, 0.0);
        item.setPickUpDelay(10);
        level().addFreshEntity(item);
        if (getOwner() instanceof ServerPlayer player) {
            forget(player);
        }
        discard();
    }

    /** He no longer has a shield in flight. */
    private void forget(ServerPlayer cap) {
        CaptainState cs = cap.getData(ModAttachments.CAPTAIN);
        if (cs.shieldEntity() == getId()) {
            cap.setData(ModAttachments.CAPTAIN, cs.withShield(-1).with(CaptainState.OFF_HAND, false)
                    .with(CaptainState.RICOCHET, false).withBouncesLeft(0));
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
        tag.put("Shield", shield().save(registryAccess()));
        tag.putBoolean("Returning", isReturning());
        tag.putBoolean("OffHand", hand == InteractionHand.OFF_HAND);
        tag.putBoolean("Ricochet", ricochet);
        tag.putInt("Bounces", bouncesLeft);
        tag.putFloat("Damage", damage);
        tag.putDouble("Speed", speed);
        tag.putDouble("Range", range);
        tag.putInt("Cooldown", cooldown);
        tag.putDouble("Travelled", travelled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setShield(ItemStack.parseOptional(registryAccess(), tag.getCompound("Shield")));
        setReturning(tag.getBoolean("Returning"));
        hand = tag.getBoolean("OffHand") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ricochet = tag.getBoolean("Ricochet");
        bouncesLeft = tag.getInt("Bounces");
        damage = tag.getFloat("Damage");
        speed = tag.contains("Speed") ? tag.getDouble("Speed") : 1.8;
        range = tag.contains("Range") ? tag.getDouble("Range") : 24.0;
        cooldown = tag.getInt("Cooldown");
        travelled = tag.getDouble("Travelled");
    }
}
