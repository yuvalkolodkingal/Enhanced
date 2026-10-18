package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
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

/**
 * Gungnir in flight (3.5): Odin's Gungnir Throw (Sneak + R). It flies straight along his look, piercing the first {@link
 * #PIERCE} creatures it strikes ({@code odin.throwDamage} each), until it strikes a wall, a third creature or the end of
 * its range; then it turns and flies home to his hand through everything, as the spear of the Allfather always comes
 * home. R while it flies calls it back at once. If its thrower is gone (dead, away, in another realm) it falls where it
 * is, an item like any other (whose bond still holds).
 *
 * <p>It is a {@link ShieldCost} projectile: a shield that stops it (the Chaos Shield, Agatha's, Odin's own) turns it home
 * instead of destroying it ({@link #oneOfAKind}), as a shield does with Thanos's sword.
 */
public class GungnirEntity extends Projectile implements ShieldCost {
    private static final EntityDataAccessor<ItemStack> DATA_SPEAR =
            SynchedEntityData.defineId(GungnirEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(GungnirEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double THROW_SPEED = 2.2;
    public static final double RETURN_SPEED = 2.6;
    /** How far it flies before it turns home. */
    public static final double RANGE = 40.0;
    /** How many creatures it passes through; the next one stops it. */
    public static final int PIERCE = 2;
    /** Even if something goes wrong, it never flies longer than this (ticks) before it falls. */
    private static final int MAX_LIFE = 400;

    private final IntSet hitOut = new IntOpenHashSet();
    private final IntSet hitBack = new IntOpenHashSet();
    private double travelled;
    private int life;

    public GungnirEntity(EntityType<? extends GungnirEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    /** Gungnir thrown by {@code odin} from his eyes along his look. */
    public GungnirEntity(ServerLevel level, ServerPlayer odin, ItemStack spear) {
        this(ModEntities.GUNGNIR.get(), level);
        setOwner(odin);
        setSpear(spear);
        Vec3 look = odin.getLookAngle();
        setPos(odin.getEyePosition().add(look.scale(0.7)).subtract(0, 0.15, 0));
        setDeltaMovement(look.scale(THROW_SPEED));
        setYRot((float) Math.toDegrees(Math.atan2(look.x, look.z)));
        setXRot((float) Math.toDegrees(Math.asin(-look.y)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SPEAR, ItemStack.EMPTY);
        builder.define(DATA_RETURNING, false);
    }

    /** The spear it is (its owner and bond go with it). */
    public ItemStack spear() {
        ItemStack stack = entityData.get(DATA_SPEAR);
        return stack.isEmpty() ? new ItemStack(ModItems.GUNGNIR.get()) : stack;
    }

    private void setSpear(ItemStack stack) {
        entityData.set(DATA_SPEAR, stack.copyWithCount(1));
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    /** Turns it homeward (through walls from now on). */
    public void setReturning(boolean returning) {
        entityData.set(DATA_RETURNING, returning);
        noPhysics = returning;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        if (level().isClientSide) {
            // The server sends the real position every tick; meanwhile it keeps moving smoothly, shedding golden motes.
            setPos(from.add(getDeltaMovement()));
            if (random.nextInt(2) == 0) {
                level().addParticle(new EnergyParticleOptions(OdinAbilities.GOLD, 0.5F), getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        ServerPlayer odin = odin();
        if (++life > MAX_LIFE || odin == null) {
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
                // A hair short of the face it struck, so it stays on its own side of the wall.
                to = wall.getLocation().subtract(motion.normalize().scale(0.05));
            }
            boolean stopped = pierce(level, odin, from, to, hitOut, true);
            setPos(to);
            travelled += to.distanceTo(from);
            if (hitWall) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.GUNGNIR_BLAST.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
                level.sendParticles(new EnergyParticleOptions(OdinAbilities.GOLD, 1.0F), getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.15);
            }
            if (hitWall || stopped || travelled >= RANGE) {
                setReturning(true);
            }
        } else {
            Vec3 hand = odin.getEyePosition().subtract(0, 0.5, 0);
            Vec3 toHand = hand.subtract(from);
            double distance = toHand.length();
            if (distance < 1.6) {
                catchBy(odin);
                return;
            }
            motion = toHand.scale(Math.min(RETURN_SPEED, distance) / distance);
            Vec3 to = from.add(motion);
            pierce(level, odin, from, to, hitBack, false);
            setPos(to);
        }
        setDeltaMovement(motion);
        Vec3 d = motion.normalize();
        setYRot((float) Math.toDegrees(Math.atan2(d.x, d.z)));
        setXRot((float) Math.toDegrees(Math.asin(-Math.max(-1.0, Math.min(1.0, d.y)))));
        hasImpulse = true;
    }

    /**
     * Strikes every creature along the segment it has not struck on this leg (as his area attacks choose: his team and pets
     * are spared, players only with PvP on).
     *
     * @return whether a creature beyond the first {@link #PIERCE} stopped it (only on the way out)
     */
    private boolean pierce(ServerLevel level, ServerPlayer odin, Vec3 from, Vec3 to, IntSet already, boolean out) {
        float damage = OdinAbilities.throwDamage();
        AABB sweep = new AABB(from, to).inflate(0.8);
        for (Entity e : level.getEntities(this, sweep, e -> e != odin && e.isAlive() && e instanceof LivingEntity && !already.contains(e.getId()))) {
            AABB box = e.getBoundingBox().inflate(0.4);
            if (!box.contains(from) && box.clip(from, to).isEmpty()) {
                continue;
            }
            LivingEntity victim = (LivingEntity) e;
            if (!AreaTargets.mayHurt(odin, victim, false)) {
                continue;
            }
            already.add(e.getId());
            if (victim.hurt(ModDamageTypes.source(level, ModDamageTypes.GUNGNIR, odin, this), damage)) {
                Vec3 push = to.subtract(from);
                if (push.lengthSqr() > 1.0E-6) {
                    victim.knockback(0.8, -push.x, -push.z);
                }
                level.sendParticles(new EnergyParticleOptions(OdinAbilities.GOLD, 1.0F), victim.getX(), victim.getY(0.6), victim.getZ(),
                        8, 0.2, 0.3, 0.2, 0.1);
            }
            if (out && already.size() > PIERCE) {
                return true;
            }
        }
        return false;
    }

    /** Its thrower, if he can still catch it: alive, in this realm, with the Gungnir ability (or Odin's spear in his kit). */
    @Nullable
    private ServerPlayer odin() {
        return getOwner() instanceof ServerPlayer player && player.isAlive() && !player.isRemoved() && player.level() == level()
                && Powers.get(player).has(HeroAbility.ODIN_GUNGNIR) ? player : null;
    }

    /** Back in his hand (or his inventory, if his hand is full). */
    private void catchBy(ServerPlayer odin) {
        ItemStack spear = spear().copy();
        if (odin.getMainHandItem().isEmpty()) {
            odin.setItemInHand(InteractionHand.MAIN_HAND, spear);
        } else if (!odin.getInventory().add(spear)) {
            odin.drop(spear, false);
        }
        level().playSound(null, odin.getX(), odin.getY(), odin.getZ(), ModSounds.GUNGNIR_CATCH.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
        forget();
        discard();
    }

    /** Called home by R while it flies. */
    public void recall() {
        if (!isReturning()) {
            setReturning(true);
        }
    }

    /** Back in its thrower's inventory at once (he leaves the world while it flies). */
    void giveBack(ServerPlayer odin) {
        ItemStack spear = spear().copy();
        if (!odin.getInventory().add(spear)) {
            odin.drop(spear, false);
        }
        forget();
        discard();
    }

    /** Nobody to fly home to: it falls, and lies wherever it lands. */
    void drop() {
        ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), spear().copy());
        item.setDeltaMovement(0, -0.2, 0);
        item.setPickUpDelay(10);
        level().addFreshEntity(item);
        forget();
        discard();
    }

    /** Its thrower no longer has a spear in flight. */
    private void forget() {
        if (getOwner() instanceof ServerPlayer player) {
            OdinAbilities.spearGone(player, this);
        }
    }

    // ---- what a shield does to it ----

    @Override
    public float shieldCost() {
        return OdinAbilities.throwDamage();
    }

    @Override
    public boolean oneOfAKind() {
        return true;
    }

    @Override
    public void turnHome() {
        setReturning(true);
    }

    @Override
    public boolean returning() {
        return isReturning();
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
        tag.put("Spear", spear().save(registryAccess()));
        tag.putBoolean("Returning", isReturning());
        tag.putDouble("Travelled", travelled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpear(ItemStack.parseOptional(registryAccess(), tag.getCompound("Spear")));
        setReturning(tag.getBoolean("Returning"));
        travelled = tag.getDouble("Travelled");
    }
}
