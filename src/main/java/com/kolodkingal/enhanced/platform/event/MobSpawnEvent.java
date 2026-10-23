package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** A mob spawns (NeoForge's MobSpawnEvent). */
public abstract class MobSpawnEvent extends EntityEvent {
    private final ServerLevelAccessor level;
    private final double x;
    private final double y;
    private final double z;

    protected MobSpawnEvent(Mob entity, ServerLevelAccessor level, double x, double y, double z) {
        super(entity);
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Mob getEntity() {
        return (Mob) super.getEntity();
    }

    public ServerLevelAccessor getLevel() {
        return level;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    /** May a mob spawn where it stands? setResult(FAIL) refuses it. */
    public static class PositionCheck extends MobSpawnEvent {
        private final MobSpawnType spawnType;
        private final @Nullable BaseSpawner spawner;
        private Result result;

        public PositionCheck(
                Mob entity, ServerLevelAccessor level, MobSpawnType spawnType, @Nullable BaseSpawner spawner) {
            super(entity, level, entity.getX(), entity.getY(), entity.getZ());
            this.spawnType = spawnType;
            this.spawner = spawner;
            this.result = Result.DEFAULT;
        }

        public MobSpawnType getSpawnType() {
            return spawnType;
        }

        @Nullable
        public BaseSpawner getSpawner() {
            return spawner;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        /** Let the mob spawn here, refuse it, or leave it to the game's own checks. */
        public enum Result {
            SUCCEED,
            DEFAULT,
            FAIL
        }
    }

    /** May a mob of a type spawn at a position (before it exists)? setResult(FAIL) refuses it. */
    public static class SpawnPlacementCheck {
        private final EntityType<?> entityType;
        private final ServerLevelAccessor level;
        private final MobSpawnType spawnType;
        private final BlockPos pos;
        private final RandomSource random;
        private final boolean defaultResult;
        private Result result;

        public SpawnPlacementCheck(
                EntityType<?> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos,
                RandomSource random, boolean defaultResult) {
            this.entityType = entityType;
            this.level = level;
            this.spawnType = spawnType;
            this.pos = pos;
            this.random = random;
            this.defaultResult = defaultResult;
            this.result = Result.DEFAULT;
        }

        public EntityType<?> getEntityType() {
            return entityType;
        }

        public ServerLevelAccessor getLevel() {
            return level;
        }

        public MobSpawnType getSpawnType() {
            return spawnType;
        }

        public BlockPos getPos() {
            return pos;
        }

        public RandomSource getRandom() {
            return random;
        }

        public boolean getDefaultResult() {
            return defaultResult;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        /** Let the mob spawn here, refuse it, or leave it to the game's own check. */
        public enum Result {
            SUCCEED,
            DEFAULT,
            FAIL
        }
    }
}
