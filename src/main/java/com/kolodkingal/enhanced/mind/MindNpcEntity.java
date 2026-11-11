package com.kolodkingal.enhanced.mind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;

/**
 * Someone who lives in Vision's mind (3.12): Dum-E, E.D.I.T.H., F.R.I.D.A.Y., the riddle robots, Ultron in a human face,
 * the Human Torch and Dr. Phineas Horton ({@link Role}). They do not move, cannot be hurt, and talk when used
 * ({@link MindTalk}). They are never saved: {@link MindRealm} puts them in their places when their mind is visited.
 */
public class MindNpcEntity extends PathfinderMob {
    public enum Role {
        DUM_E, EDITH, FRIDAY, ROBOT, ULTRON, TORCH, HORTON;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The AIs float, as holograms. */
        public boolean floats() {
            return this == EDITH || this == FRIDAY;
        }

        static Role of(int i) {
            Role[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(MindNpcEntity.class, EntityDataSerializers.INT);
    /** The Human Torch before he gives his flames back: on fire, his own red body. */
    private static final EntityDataAccessor<Boolean> ABLAZE = SynchedEntityData.defineId(MindNpcEntity.class, EntityDataSerializers.BOOLEAN);

    /** The mind (its plot) this one lives in. */
    private int plot = -1;

    public MindNpcEntity(EntityType<? extends MindNpcEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, 0);
        builder.define(ABLAZE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
    }

    public Role role() {
        return Role.of(entityData.get(ROLE));
    }

    public void setRole(Role role) {
        entityData.set(ROLE, role.ordinal());
        setNoGravity(role.floats());
    }

    public boolean ablaze() {
        return entityData.get(ABLAZE);
    }

    public void setAblaze(boolean ablaze) {
        entityData.set(ABLAZE, ablaze);
    }

    public int plot() {
        return plot;
    }

    public void setPlot(int plot) {
        this.plot = plot;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(0.0, role().floats() ? 0.0 : 1.0, 0.0));
        if (level().isClientSide() && ablaze() && tickCount % 2 == 0) {
            MindFx.flames(this);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer server) {
            MindTalk.talkTo(server, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Never saved: the realm puts them back for the next visit. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return ablaze();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", role().ordinal());
        tag.putInt("Plot", plot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRole(Role.of(tag.getInt("Role")));
        plot = tag.getInt("Plot");
    }
}
