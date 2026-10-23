package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.level.LevelAccessor;

/** An event about a level (NeoForge's LevelEvent). */
public abstract class LevelEvent {
    private final LevelAccessor level;

    protected LevelEvent(LevelAccessor level) {
        this.level = level;
    }

    public LevelAccessor getLevel() {
        return level;
    }

    /** A level was loaded. */
    public static class Load extends LevelEvent {
        public Load(LevelAccessor level) {
            super(level);
        }
    }

    /** A level is being saved. */
    public static class Save extends LevelEvent {
        public Save(LevelAccessor level) {
            super(level);
        }
    }

    /** A level is being unloaded. */
    public static class Unload extends LevelEvent {
        public Unload(LevelAccessor level) {
            super(level);
        }
    }
}
