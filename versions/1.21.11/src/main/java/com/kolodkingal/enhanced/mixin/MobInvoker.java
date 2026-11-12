package com.kolodkingal.enhanced.mixin;
//#norenames

import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.11: a mob's chances to drop what it wears are a record of their own (Magneto's strip and pull read them). */
@Mixin(Mob.class)
public interface MobInvoker {
    @Accessor("dropChances")
    DropChances enhanced$dropChances();
}
