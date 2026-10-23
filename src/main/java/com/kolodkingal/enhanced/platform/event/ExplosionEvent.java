package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.List;

/** An explosion (NeoForge's ExplosionEvent). */
public abstract class ExplosionEvent {
    private final Level level;
    private final Explosion explosion;

    protected ExplosionEvent(Level level, Explosion explosion) {
        this.level = level;
        this.explosion = explosion;
    }

    public Level getLevel() {
        return level;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    /** An explosion goes off: what it breaks and whom it hits (live lists). */
    public static class Detonate extends ExplosionEvent {
        private final List<Entity> affectedEntities;

        public Detonate(Level level, Explosion explosion, List<Entity> affectedEntities) {
            super(level, explosion);
            this.affectedEntities = affectedEntities;
        }

        public List<Entity> getAffectedEntities() {
            return affectedEntities;
        }

        /** The blocks the explosion breaks (a live list: remove a position to spare it). */
        public List<BlockPos> getAffectedBlocks() {
            return getExplosion().getToBlow();
        }
    }
}
