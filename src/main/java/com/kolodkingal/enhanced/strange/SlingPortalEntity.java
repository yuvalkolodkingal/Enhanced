package com.kolodkingal.enhanced.strange;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One ring of a pair of Sling-Ring portals (3.6, Doctor Strange's N and the Sling Ring): a vertical ring of orange sparks
 * of radius {@link #RADIUS}, standing on the ground (its centre {@link #CENTER_UP} above its feet) and facing {@link
 * #facing()}; whoever passes through comes out of its twin ({@link SlingPortals#pass}). Never saved. The pair, its twin
 * and its life are {@link SlingPortals}'; drawn by {@code SlingPortalRenderer} (a spinning ring of sparks round a
 * dark-gold swirl: the far side is never rendered).
 */
public class SlingPortalEntity extends Entity {
    /** The ring's radius, and how high its centre stands above its feet. */
    public static final double RADIUS = 1.2;
    public static final double CENTER_UP = 1.25;

    private static final EntityDataAccessor<Float> FACING = SynchedEntityData.defineId(SlingPortalEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SlingPortalEntity.class,
            EntityDataSerializers.INT);

    /** The pair this ring is of (server). */
    @Nullable
    UUID pair;
    /**
     * What was near the ring last tick (server): its centre then, so a pass is a crossing of the plane between two
     * ticks.
     */
    final Map<Integer, Vec3> last = new HashMap<>();

    public SlingPortalEntity(EntityType<? extends SlingPortalEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FACING, 0.0F);
        builder.define(LIFE, 160);
    }

    /** The yaw the ring faces (degrees, the game's: 0 south): a traveller crossing it forward goes this way. */
    public float facing() {
        return entityData.get(FACING);
    }

    public void setFacing(float yaw) {
        entityData.set(FACING, yaw);
        setYRot(yaw);
    }

    /** The ticks the ring lives (its spark ring closes at the end of them). */
    public int life() {
        return entityData.get(LIFE);
    }

    public void setLife(int ticks) {
        entityData.set(LIFE, ticks);
    }

    /** The centre of the ring. */
    public Vec3 center() {
        return position().add(0, CENTER_UP, 0);
    }

    /** The direction it faces (a unit vector in the horizontal plane). */
    public Vec3 normal() {
        return normal(facing());
    }

    /** The horizontal unit vector a yaw faces (the game's: 0 is south, +z). */
    public static Vec3 normal(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            SlingPortals.ringTick(level, this);
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        Vec3 c = position().add(0, CENTER_UP, 0);
        return new AABB(c, c).inflate(RADIUS + 0.1);
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
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
