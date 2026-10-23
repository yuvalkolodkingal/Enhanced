package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A mob's chance to drop what it wears in a slot: Magneto's strip and pull let gear a creature never drops (chance 0, a
 * Horseman's) crumble instead of being given. Was an access transformer line before the port.
 */
@Mixin(Mob.class)
public interface MobInvoker {
    @Invoker("getEquipmentDropChance")
    float enhanced$getEquipmentDropChance(EquipmentSlot slot);
}
