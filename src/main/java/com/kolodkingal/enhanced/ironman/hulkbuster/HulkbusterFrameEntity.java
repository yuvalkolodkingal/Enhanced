package com.kolodkingal.enhanced.ironman.hulkbuster;

import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Hulkbuster's frame that Veronica drops (3.8, lane L4, PLAN 6.4; Age of Ultron). It is only a moment of the
 * suit-up and the take-off: {@link Hulkbuster}'s listener on the phase makes it and moves it through its stages, and it
 * goes when they are over (it is never saved). Its renderer (lane L7's frame model) draws it from the synced {@link
 * #stage()} and {@link #openness}:
 * <ul>
 * <li>{@link #FALLING}: from {@code dropHeight} 40 above the spot in front of him, down to the ground in {@link
 * #FALL_TICKS} with thruster trails;</li>
 * <li>{@link #OPENING} (it landed: dust, a thud): the front panels swing out over {@link #OPEN_TICKS};</li>
 * <li>{@link #CLOSING}: they close round him over {@link #CLOSE_TICKS} (then the player is drawn as the Hulkbuster and
 * the frame goes);</li>
 * <li>{@link #ASSEMBLING}: the indoor version, the pieces fold in round him over {@link #ASSEMBLE_TICKS};</li>
 * <li>{@link #LIFTING}: the take-off's end, the empty open frame flies up and away.</li>
 * </ul>
 */
public class HulkbusterFrameEntity extends Entity {
    public static final byte FALLING = 0;
    public static final byte OPENING = 1;
    public static final byte CLOSING = 2;
    public static final byte ASSEMBLING = 3;
    public static final byte LIFTING = 4;
    /** The stages' lengths (the UP phase's 80 ticks: fall 30, open 10, step in 10, close 15, grow 15). */
    public static final int FALL_TICKS = 30;
    public static final int OPEN_TICKS = 10;
    public static final int CLOSE_TICKS = 15;
    public static final int ASSEMBLE_TICKS = 25;
    public static final int LIFT_TICKS = 20;

    private static final EntityDataAccessor<Byte> STAGE = SynchedEntityData.defineId(HulkbusterFrameEntity.class,
            EntityDataSerializers.BYTE);
    /** The frame's age when its stage began. */
    private static final EntityDataAccessor<Integer> STAGE_AT = SynchedEntityData.defineId(
            HulkbusterFrameEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(HulkbusterFrameEntity.class,
            EntityDataSerializers.INT);

    /** Where it falls from and lands (server). */
    private Vec3 from = Vec3.ZERO;
    private Vec3 landing = Vec3.ZERO;

    public HulkbusterFrameEntity(EntityType<? extends HulkbusterFrameEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** A frame for {@code owner} that falls from {@code from} to {@code landing}. */
    public static HulkbusterFrameEntity dropping(ServerPlayer owner, Vec3 from, Vec3 landing) {
        HulkbusterFrameEntity frame = new HulkbusterFrameEntity(ModEntities.HULKBUSTER_FRAME.get(), owner.level());
        frame.entityData.set(OWNER, owner.getId());
        frame.from = from;
        frame.landing = landing;
        frame.moveTo(from.x, from.y, from.z, owner.getYRot() + 180.0F, 0.0F);
        frame.setStage(FALLING);
        return frame;
    }

    /** A frame that stands round {@code owner} already (the indoor assembly, the take-off). */
    public static HulkbusterFrameEntity standing(ServerPlayer owner, byte stage) {
        HulkbusterFrameEntity frame = new HulkbusterFrameEntity(ModEntities.HULKBUSTER_FRAME.get(), owner.level());
        frame.entityData.set(OWNER, owner.getId());
        frame.from = owner.position();
        frame.landing = owner.position();
        frame.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        frame.setStage(stage);
        return frame;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STAGE, FALLING);
        builder.define(STAGE_AT, 0);
        builder.define(OWNER, -1);
    }

    public byte stage() {
        return entityData.get(STAGE);
    }

    /** The ticks (with the partial tick) since its stage began. */
    public float stageAge(float partialTick) {
        return Math.max(0.0F, tickCount + partialTick - entityData.get(STAGE_AT));
    }

    public void setStage(byte stage) {
        entityData.set(STAGE, stage);
        entityData.set(STAGE_AT, tickCount);
    }

    /** Its Iron Man, or null. */
    @Nullable
    public Player owner() {
        return level().getEntity(entityData.get(OWNER)) instanceof Player p ? p : null;
    }

    /**
     * How open its front panels are now (0 shut, 1 wide open): shut while it falls, opening when it lands, closing
     * round him, open while it lifts away; the indoor assembly folds in from open.
     */
    public float openness(float partialTick) {
        float age = stageAge(partialTick);
        return switch (stage()) {
            case OPENING -> Mth.clamp(age / OPEN_TICKS, 0.0F, 1.0F);
            case CLOSING -> 1.0F - Mth.clamp(age / CLOSE_TICKS, 0.0F, 1.0F);
            case ASSEMBLING -> 1.0F - Mth.clamp(age / ASSEMBLE_TICKS, 0.0F, 1.0F);
            case LIFTING -> 1.0F;
            default -> 0.0F;
        };
    }

    /** Where it lands (server). */
    public Vec3 landing() {
        return landing;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 at = position();
            boolean lifting = stage() == LIFTING && stageAge(0.0F) > 4.0F;
            if (stage() == FALLING || lifting) {
                // The thrusters of Veronica's delivery (under it: a lifting frame first clears his head).
                for (int i = 0; i < 2; i++) {
                    level().addParticle(ParticleTypes.FLAME, at.x + (random.nextDouble() - 0.5) * 1.6, at.y,
                            at.z + (random.nextDouble() - 0.5) * 1.6, 0.0, -0.15, 0.0);
                }
                if (tickCount % 3 == 0) {
                    level().addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.02, 0.0);
                }
            }
            return;
        }
        float age = stageAge(0.0F);
        switch (stage()) {
            case FALLING -> {
                float t = Mth.clamp(age / FALL_TICKS, 0.0F, 1.0F);
                // Fast, braking at the end (Veronica's thrusters slow it down).
                double f = 1.0 - (1.0 - t) * (1.0 - t);
                Vec3 at = from.lerp(landing, f);
                setPos(at.x, at.y, at.z);
            }
            case LIFTING -> {
                setPos(getX(), getY() + 0.3 + age * 0.08, getZ());
                if (age >= LIFT_TICKS) {
                    discard();
                    return;
                }
            }
            default -> {
            }
        }
        // A frame whose suit-up or take-off is no longer playing goes (a lifting one flies off by itself).
        if (stage() != LIFTING && tickCount > 5 && !owned()) {
            discard();
        }
    }

    /** Whether its Iron Man is still in the Hulkbuster's phase. */
    private boolean owned() {
        Player owner = owner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        IronManState state = IronSuits.state(owner);
        return state.inPhase() && state.phaseKind() == SuitKind.HULKBUSTER;
    }

    /** It lifts off and flies away (the take-off's end, or an aborted suit-up). */
    public void liftOff() {
        if (stage() != LIFTING) {
            setStage(LIFTING);
        }
    }

    /** The dust of its landing. */
    void landed(ServerLevel level) {
        setPos(landing.x, landing.y, landing.z);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, landing.x, landing.y + 0.2, landing.z, 12, 1.2, 0.1,
                1.2, 0.02);
        level.sendParticles(ParticleTypes.POOF, landing.x, landing.y + 0.1, landing.z, 20, 1.5, 0.1, 1.5, 0.05);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
