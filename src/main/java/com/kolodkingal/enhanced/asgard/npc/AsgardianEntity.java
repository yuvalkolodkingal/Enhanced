package com.kolodkingal.enhanced.asgard.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * An Asgardian (3.5): the one entity {@code enhanced:asgardian} of every {@link Role} (the named gods and warriors, the
 * Einherjar, the citizens). The 3.5 skeleton registers the entity (so that Heimdall's and Odin's lanes can name it); lane L5
 * owns this class and fills it (role, look, idle life, talk, trades, never killed by players).
 */
public class AsgardianEntity extends PathfinderMob {
    public AsgardianEntity(EntityType<? extends AsgardianEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }
}
