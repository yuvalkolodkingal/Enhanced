package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A blade spike (3.5): one of the black blades of a Blade Wall or a Blade Burst, erupting from the floor. After its
 * telegraph ({@link #delay()} ticks: cracks and green light in the floor, which the client draws) it rises in
 * {@link #RISE_TICKS} ticks, striking everything its maker may hurt in its box for its damage of {@code blade_wall} and
 * throwing it up; then it stands for {@link #stand()} ticks as a solid post that blocks the way (other entities collide
 * with it as with a boat) and hurts whatever touches it at most once in {@link #TOUCH_COOLDOWN} ticks; then it sinks back
 * into the floor. It never replaces a block: it is an entity standing on the ground. Never saved.
 */
public class BladeSpikeEntity extends Entity {
    /** How long the rise takes. */
    public static final int RISE_TICKS = 4;
    /** How long the sinking takes, at the end of its life. */
    public static final int SINK_TICKS = 6;
    /** One creature is hurt by a touch of the same spike at most this often. */
    public static final int TOUCH_COOLDOWN = 10;
    /** How hard the eruption throws a creature up. */
    public static final double KNOCK_UP = 0.62;

    private static final EntityDataAccessor<Integer> DATA_DELAY = SynchedEntityData.defineId(BladeSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAND = SynchedEntityData.defineId(BladeSpikeEntity.class, EntityDataSerializers.INT);
    /** Which of the two looks: 0 a spike of the wall, 1 a smaller blade of a ring. */
    private static final EntityDataAccessor<Byte> DATA_STYLE = SynchedEntityData.defineId(BladeSpikeEntity.class, EntityDataSerializers.BYTE);

    private final Map<Integer, Integer> touched = new HashMap<>();
    @Nullable
    private UUID ownerId;
    @Nullable
    private LivingEntity owner;
    private float damage = 8.0F;
    private boolean erupted;

    public BladeSpikeEntity(EntityType<? extends BladeSpikeEntity> type, Level level) {
        super(type, level);
    }

    /**
     * A spike of {@code owner}'s at {@code feet} (on the ground), rising after {@code delay} ticks and standing for
     * {@code stand}.
     */
    public static BladeSpikeEntity at(ServerLevel level, LivingEntity owner, Vec3 feet, float yaw, int delay, int stand, float damage, int style) {
        BladeSpikeEntity spike = new BladeSpikeEntity(ModEntities.BLADE_SPIKE.get(), level);
        spike.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        spike.owner = owner;
        spike.ownerId = owner.getUUID();
        spike.damage = damage;
        spike.entityData.set(DATA_DELAY, Math.max(0, delay));
        spike.entityData.set(DATA_STAND, Math.max(1, stand));
        spike.entityData.set(DATA_STYLE, (byte) style);
        return spike;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DELAY, 10);
        builder.define(DATA_STAND, 120);
        builder.define(DATA_STYLE, (byte) 0);
    }

    /** Ticks of telegraph before it rises. */
    public int delay() {
        return entityData.get(DATA_DELAY);
    }

    /** Ticks it stands once risen. */
    public int stand() {
        return entityData.get(DATA_STAND);
    }

    public int style() {
        return entityData.get(DATA_STYLE);
    }

    /** The whole of its life: telegraph, rise, standing, sinking. */
    public int lifetime() {
        return delay() + RISE_TICKS + stand() + SINK_TICKS;
    }

    /**
     * How far out of the floor it is at {@code time} (ticks with the partial tick), 0 to 1: nothing through the telegraph,
     * up over the rise, all of it while it stands, down over the sinking.
     */
    public float height(float time) {
        float t = time - delay();
        if (t <= 0.0F) {
            return 0.0F;
        }
        if (t < RISE_TICKS) {
            float k = t / RISE_TICKS;
            return 1.0F - (1.0F - k) * (1.0F - k);
        }
        float left = lifetime() - time;
        if (left < SINK_TICKS) {
            return Math.max(0.0F, left / SINK_TICKS);
        }
        return 1.0F;
    }

    /** Whether it stands out of the floor now (and blocks the way). */
    public boolean standing() {
        return tickCount >= delay() + 1 && tickCount < lifetime() - SINK_TICKS / 2;
    }

    @Nullable
    public LivingEntity owner() {
        if (owner == null && ownerId != null && level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof LivingEntity living) {
            owner = living;
        }
        return owner;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount < delay() && random.nextInt(3) == 0) {
                level().addParticle(new EnergyParticleOptions(HelaRules.GREEN, 0.5F), getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 0.05,
                        getZ() + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
            }
            return;
        }
        if (!onGround() && !standing() && tickCount < delay()) {
            move(MoverType.SELF, new Vec3(0.0, -0.3, 0.0)); // (settles onto the floor during its telegraph)
        }
        if (tickCount >= lifetime()) {
            discard();
            return;
        }
        LivingEntity maker = owner();
        if (maker == null || !maker.isAlive()) {
            if (tickCount < delay()) {
                discard(); // nobody left to make it rise
                return;
            }
        }
        if (tickCount == delay()) {
            erupt(maker);
        }
        if (erupted && standing() && tickCount % 2 == 0) {
            touch(maker);
        }
    }

    /** It bursts from the floor: everything in its box is struck and thrown up. */
    private void erupt(@Nullable LivingEntity maker) {
        erupted = true;
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(blockPosition().below()).isAir()
                ? Blocks.DEEPSLATE.defaultBlockState() : level.getBlockState(blockPosition().below())), getX(), getY() + 0.1, getZ(), 8, 0.2, 0.05, 0.2, 0.1);
        level.sendParticles(new EnergyParticleOptions(HelaRules.GREEN, 0.9F), getX(), getY() + 0.8, getZ(), 4, 0.1, 0.5, 0.1, 0.03);
        if (maker == null) {
            return;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hitBox(), e -> HelaRules.canHurt(maker, e))) {
            if (HelaRules.hurt(e, ModDamageTypes.BLADE_WALL, maker, this, damage)) {
                touched.put(e.getId(), tickCount);
                Vec3 v = e.getDeltaMovement();
                e.setDeltaMovement(v.x * 0.4, Math.max(v.y, KNOCK_UP), v.z * 0.4);
                e.hurtMarked = true;
            }
        }
    }

    /** A creature that touches it while it stands is cut, at most once in {@link #TOUCH_COOLDOWN} ticks. */
    private void touch(@Nullable LivingEntity maker) {
        if (maker == null) {
            return;
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, hitBox().inflate(0.05), e -> HelaRules.canHurt(maker, e))) {
            Integer last = touched.get(e.getId());
            if (last != null && tickCount - last < TOUCH_COOLDOWN) {
                continue;
            }
            touched.put(e.getId(), tickCount);
            HelaRules.hurt(e, ModDamageTypes.BLADE_WALL, maker, this, damage * 0.5F);
        }
    }

    private AABB hitBox() {
        return getBoundingBox().inflate(0.15, 0.0, 0.15);
    }

    /** While it stands it is solid: creatures walk into it as into a boat, not through it. */
    @Override
    public boolean canBeCollidedWith() {
        return standing();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public float damage() {
        return damage;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }
}
