package com.kolodkingal.enhanced.deadpool;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

/**
 * Dogpool (3.7): Deadpool's dog in a red-and-black hood and goggles (Deadpool &amp; Wolverine), on the vanilla wolf's
 * body. He follows Deadpool, bites his target and whoever attacks him, never a player without PvP, and "needs a nap"
 * (poofs) at 0 health or when his time is up. Registered by the 3.7 skeleton as a plain wolf with the plan's health and
 * bite; lane L3 owns the class (his goals, his time, his poof).
 */
public class DogpoolEntity extends Wolf {
    public DogpoolEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
    }
}
