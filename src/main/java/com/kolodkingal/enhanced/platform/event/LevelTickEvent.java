package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.level.Level;

import java.util.function.BooleanSupplier;

/** A level's tick (NeoForge's LevelTickEvent). */
public abstract class LevelTickEvent {
    private final BooleanSupplier haveTime;
    private final Level level;

    protected LevelTickEvent(BooleanSupplier haveTime, Level level) {
        this.haveTime = haveTime;
        this.level = level;
    }

    public BooleanSupplier haveTime() {
        return haveTime;
    }

    public Level getLevel() {
        return level;
    }

    /** Whether the server has time left in this tick. */
    public boolean hasTime() {
        return haveTime.getAsBoolean();
    }

    /** Before a level's tick (both sides). */
    public static class Pre extends LevelTickEvent {
        public Pre(BooleanSupplier haveTime, Level level) {
            super(haveTime, level);
        }
    }

    /** After a level's tick (both sides). */
    public static class Post extends LevelTickEvent {
        public Post(BooleanSupplier haveTime, Level level) {
            super(haveTime, level);
        }
    }
}
