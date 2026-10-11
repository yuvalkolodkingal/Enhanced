package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kolodkingal.enhanced.ability.Raycasts;

import java.util.UUID;

/**
 * A block Wanda (or Jean Grey) tore out of the ground. While held it floats wherever she looks; launched, it flies like
 * a shell, hurts the first creature it hits and shatters (dropping its normal item). Put down gently, it lands like
 * sand. It glows in the colour of the telekinesis that holds it: Wanda's red, Jean's violet.
 */
public class TelekineticBlockEntity extends Entity {
    private static final EntityDataAccessor<BlockState> DATA_BLOCK =
            SynchedEntityData.defineId(TelekineticBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Boolean> DATA_HELD =
            SynchedEntityData.defineId(TelekineticBlockEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(TelekineticBlockEntity.class, EntityDataSerializers.INT);

    /** Damage of a thrown block hitting a creature at normal speed. */
    public static final float BASE_DAMAGE = 8.0F;
    private static final int MAX_LIFE = 20 * 60;

    @Nullable
    private UUID thrower;
    private boolean launched;
    /** Ticks since the holder last moved this block; above 1 it is no longer held. */
    private int ticksSinceHeld = 100;
    private int life;
    private final IntSet hit = new IntOpenHashSet();

    public TelekineticBlockEntity(EntityType<? extends TelekineticBlockEntity> type, Level level) {
        super(type, level);
    }

    public TelekineticBlockEntity(Level level, BlockPos pos, BlockState state, Player holder) {
        this(ModEntities.TELEKINETIC_BLOCK.get(), level);
        setBlockState(state);
        setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        xo = getX();
        yo = getY();
        zo = getZ();
        thrower = holder.getUUID();
        ticksSinceHeld = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK, Blocks.STONE.defaultBlockState());
        builder.define(DATA_HELD, false);
        builder.define(DATA_COLOR, Power.SCARLET_WITCH.color);
    }

    public BlockState getBlockState() {
        return entityData.get(DATA_BLOCK);
    }

    /** The colour of the telekinesis that holds (or last held) this block. */
    public int getColor() {
        return entityData.get(DATA_COLOR);
    }

    private void setBlockState(BlockState state) {
        entityData.set(DATA_BLOCK, state);
    }

    // ---- control from Telekinesis ----

    public void grab(Player holder, int color) {
        thrower = holder.getUUID();
        entityData.set(DATA_COLOR, color);
        launched = false;
        ticksSinceHeld = 0;
        hit.clear();
    }

    public void keepHeld() {
        ticksSinceHeld = 0;
    }

    public void drop() {
        launched = false;
        ticksSinceHeld = 100;
        setDeltaMovement(Vec3.ZERO);
    }

    public void launch(Player thrower, Vec3 velocity) {
        this.thrower = thrower.getUUID();
        launched = true;
        ticksSinceHeld = 100;
        hit.clear();
        setDeltaMovement(velocity);
        hasImpulse = true;
    }

    /** Server: decided by the holder refreshing us every tick. Client: synced flag. */
    private boolean isHeld() {
        if (level().isClientSide) {
            return entityData.get(DATA_HELD);
        }
        boolean held = ticksSinceHeld <= 1;
        if (entityData.get(DATA_HELD) != held) {
            entityData.set(DATA_HELD, held);
        }
        return held;
    }

    // ---- behaviour ----

    @Override
    public void tick() {
        super.tick();
        BlockState state = getBlockState();
        if (state.isAir()) {
            discard();
            return;
        }
        ticksSinceHeld++;
        if (isHeld()) {
            // Telekinesis moves us; nothing to simulate.
            if (level().isClientSide && tickCount % 3 == 0) {
                level().addParticle(new EnergyParticleOptions(getColor(), 1.0F),
                        getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble(), getZ() + (random.nextDouble() - 0.5), 0, 0.02, 0);
            }
            return;
        }

        Vec3 start = position();
        applyGravity();
        Vec3 motion = getDeltaMovement();

        if (!level().isClientSide && launched) {
            Vec3 center = start.add(0, getBbHeight() * 0.5, 0);
            EntityHitResult entityHit = Raycasts.entityBetween(level(), this, center, center.add(motion), 0.3,
                    e -> e instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                            && !e.getUUID().equals(thrower) && !hit.contains(e.getId()));
            if (entityHit != null) {
                hitCreature((LivingEntity) entityHit.getEntity(), motion);
                return;
            }
        }

        move(MoverType.SELF, motion);
        setDeltaMovement(getDeltaMovement().scale(0.98));

        if (level().isClientSide) {
            if (launched) {
                level().addParticle(new EnergyParticleOptions(getColor(), 1.2F), getX(), getY() + 0.5, getZ(), 0, 0, 0);
            }
            return;
        }

        if (++life > MAX_LIFE) {
            shatter();
            return;
        }
        if (launched) {
            if (horizontalCollision || verticalCollision || onGround()) {
                shatter();
            }
        } else if (onGround()) {
            land();
        }
    }

    private void hitCreature(LivingEntity victim, Vec3 motion) {
        hit.add(victim.getId());
        Player owner = thrower == null ? null : level().getPlayerByUUID(thrower);
        float damage = BASE_DAMAGE * (float) Mth.clamp(motion.length() / 2.0, 0.75, 1.5);
        DamageSource source = ModDamageTypes.source(level(), ModDamageTypes.TELEKINETIC_SLAM, owner, this);
        victim.hurt(source, damage);
        if (motion.horizontalDistanceSqr() > 1.0E-4) {
            victim.knockback(1.0, -motion.x, -motion.z);
        }
        setPos(victim.getX(), getY(), victim.getZ());
        shatter();
    }

    /** Breaks apart with block particles and drops the block's normal loot. */
    private void shatter() {
        if (level() instanceof ServerLevel server) {
            BlockPos pos = BlockPos.containing(getX(), getY() + 0.5, getZ());
            server.levelEvent(2001, pos, Block.getId(getBlockState()));
            if (server.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                Block.dropResources(getBlockState(), server, pos);
            }
        }
        discard();
    }

    /** Put down gently: becomes a block again if there is room, otherwise drops as an item. */
    private void land() {
        BlockPos pos = blockPosition();
        BlockState here = level().getBlockState(pos);
        BlockState state = getBlockState();
        boolean replaceable = here.canBeReplaced(new DirectionalPlaceContext(level(), pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
        if (replaceable && state.canSurvive(level(), pos) && level().setBlock(pos, state, Block.UPDATE_ALL)) {
            level().playSound(null, pos, state.getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            discard();
        } else {
            shatter();
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    // ---- save ----

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setBlockState(NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState")));
        launched = tag.getBoolean("Launched");
        life = tag.getInt("Life");
        if (tag.contains("Color")) {
            entityData.set(DATA_COLOR, tag.getInt("Color"));
        }
        if (tag.hasUUID("Thrower")) {
            thrower = tag.getUUID("Thrower");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(getBlockState()));
        tag.putBoolean("Launched", launched);
        tag.putInt("Life", life);
        tag.putInt("Color", getColor());
        if (thrower != null) {
            tag.putUUID("Thrower", thrower);
        }
    }
}
