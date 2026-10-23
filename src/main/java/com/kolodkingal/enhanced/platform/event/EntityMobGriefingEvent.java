package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/** May an entity change the world (the mobGriefing rule, asked of each entity)? setCanGrief decides. */
public class EntityMobGriefingEvent extends EntityEvent {
    private final Level level;
    private boolean canGrief;

    public EntityMobGriefingEvent(Entity entity, Level level) {
        super(entity);
        this.level = level;
        this.canGrief = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    public Level getLevel() {
        return level;
    }

    public boolean canGrief() {
        return canGrief;
    }

    public void setCanGrief(boolean canGrief) {
        this.canGrief = canGrief;
    }

    public EntityMobGriefingEvent(Level level, Entity entity) {
        this(entity, level);
    }

    /** What the mobGriefing game rule says. */
    public boolean isMobGriefingEnabled() {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }
}
