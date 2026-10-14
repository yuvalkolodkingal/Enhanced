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
import net.minecraft.server.level.ServerPlayer;
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
import com.kolodkingal.enhanced.ability.BlockLifting;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.boss.ArenaLoans;
import com.kolodkingal.enhanced.boss.BossFights;

import java.util.UUID;

/**
 * A block Wanda, Billy or Jean Grey tore out of the ground. While held it floats wherever the holder looks; launched, it
 * flies like a shell, hurts the first creature it hits and shatters (dropping its normal item). Put down gently, it lands
 * like sand and becomes a block again where the one who let go of it may place one ({@link BlockLifting#mayPlace});
 * otherwise it drops as an item. It glows in the colour of the telekinesis that holds it: Wanda's red, Billy's blue,
 * Jean's violet.
 *
 * <p>A block torn out of a boss's protected arena is <em>borrowed</em> ({@link #isBorrowed}: it carries the fight's mark,
 * {@link ArenaLoans}): it never drops anything. Thrown, it turns to dust where it hits, and at the edge of a gated arena;
 * set down, it stands for a while under the arena's rules, or crumbles at once; it never goes through a portal, and once
 * its fight is gone (a crash) it is discarded.
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

    /** Who holds it, or last held or threw it (null if nobody ever did). */
    @Nullable
    public UUID thrower() {
        return thrower;
    }

    /**
     * Whether it was borrowed from a boss's protected arena ({@link ArenaLoans}): it carries the fight's mark, so it never
     * drops anything, stands only under the arena's rules, and stays in its dimension.
     */
    public boolean isBorrowed() {
        return BossFights.isMarked(this);
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
        if (!level().isClientSide && isBorrowed() && BossFights.fightOf(this) == null) {
            discard(); // its fight is gone (a crash, a forced stop): what was borrowed was never this world's to keep
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
            // A borrowed block thrown across the field or the wall of its arena stops there (D-T 4.5).
            Vec3 edge = BossFights.edgeStop(this, start.add(motion));
            Vec3 center = start.add(0, getBbHeight() * 0.5, 0);
            Vec3 reach = edge == null ? center.add(motion) : edge.add(0, getBbHeight() * 0.5, 0);
            EntityHitResult entityHit = Raycasts.entityBetween(level(), this, center, reach, 0.3,
                    e -> e instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                            && !e.getUUID().equals(thrower) && !hit.contains(e.getId()));
            if (entityHit != null) {
                hitCreature((LivingEntity) entityHit.getEntity(), motion);
                return;
            }
            if (edge != null) {
                setPos(edge.x, edge.y, edge.z);
                shatter();
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

    /** Breaks apart with block particles and drops the block's normal loot (a thrown block, or one that lived too long). */
    private void shatter() {
        shatter(false);
    }

    /**
     * Breaks apart with block particles and drops the block's normal loot, as mined with a bare hand. A block that was set
     * down and may not stand where it landed ({@code setDown}) gives the block itself instead when that loot is empty (glass,
     * ice): nothing the holder carried is lost, and nothing that has a loot (an ore) becomes a free Silk Touch (R44). A
     * borrowed block turns to dust instead: it drops nothing ({@link #crumble}).
     */
    private void shatter(boolean setDown) {
        if (isBorrowed()) {
            crumble();
            return;
        }
        if (level() instanceof ServerLevel server) {
            BlockPos pos = BlockPos.containing(getX(), getY() + 0.5, getZ());
            BlockState state = getBlockState();
            server.levelEvent(2001, pos, Block.getId(state));
            if (server.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                if (setDown && Block.getDrops(state, server, pos, null).isEmpty()) {
                    Block.popResource(server, pos, new ItemStack(state.getBlock())); // nothing, if the block has no item
                } else {
                    Block.dropResources(state, server, pos);
                }
            }
        }
        discard();
    }

    /**
     * A borrowed block turns to dust and is gone: the sound and the particles of a broken block, no loot, and its arena no
     * longer counts it.
     */
    public void crumble() {
        if (level() instanceof ServerLevel server) {
            server.levelEvent(2001, BlockPos.containing(getX(), getY() + 0.5, getZ()), Block.getId(getBlockState()));
            BossFights.borrowedGone(this);
        }
        discard();
    }

    /**
     * Put down gently: becomes a block again where it landed if the cell takes it and the one who let go of it may place
     * it there ({@link BlockLifting#mayPlace}: room, the arenas, his rights, never for someone who left); otherwise it
     * drops as an item. Nothing lands on the ground the clients hide under Ultron's island in the sky. A borrowed block
     * stands where its arena's rules let it ({@link BossFights#standBorrowed}), and crumbles to dust anywhere else.
     */
    private void land() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        BlockPos pos = blockPosition();
        if (isBorrowed()) {
            if (BossFights.standBorrowed(this, pos)) {
                discard();
            } else {
                crumble();
            }
            return;
        }
        if (BossFights.hidesGround(server, pos)) {
            shatter(true);
            return;
        }
        BlockState here = server.getBlockState(pos);
        BlockState state = getBlockState();
        ServerPlayer holder = thrower == null ? null : server.getServer().getPlayerList().getPlayer(thrower);
        boolean replaceable = here.canBeReplaced(new DirectionalPlaceContext(server, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
        if (replaceable && state.canSurvive(server, pos) && BlockLifting.mayPlace(server, pos, state, holder)
                && server.setBlock(pos, state, Block.UPDATE_ALL)) {
            server.playSound(null, pos, state.getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            discard();
        } else {
            shatter(true);
        }
    }

    /** A borrowed block never leaves the dimension of its arena (D-T 7). */
    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return !isBorrowed() && super.canChangeDimensions(from, to);
    }

    /** ... nor even begins to go through a portal (no destination is looked for). */
    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return !isBorrowed() && super.canUsePortal(allowPassengers);
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
