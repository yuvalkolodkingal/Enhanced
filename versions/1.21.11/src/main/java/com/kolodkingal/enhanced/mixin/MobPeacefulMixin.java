package com.kolodkingal.enhanced.mixin;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * 1.21.11 only: 1.21.1's {@code Mob.shouldDespawnInPeaceful} (a monster's yes, every other mob's no, the mod's own mobs
 * saying for themselves) is the entity type's "not in peaceful" from 1.21.5, which the mod's types do not set: a mob of
 * the mod goes in peaceful as it did in 1.21.1 (its own {@code shouldDespawnInPeaceful}, kept by the build).
 */
@Mixin(Mob.class)
public abstract class MobPeacefulMixin {
    private static final ClassValue<Method> OWN = new ClassValue<>() {
        @Override
        protected Method computeValue(Class<?> type) {
            try {
                Method m = type.getMethod("shouldDespawnInPeaceful");
                return m.getReturnType() == boolean.class ? m : null;
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    };

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void enhanced$peaceful(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level().getDifficulty() != Difficulty.PEACEFUL || !self.getType().isAllowedInPeaceful()
                || !BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getNamespace().equals(Enhanced.MODID)) {
            return;
        }
        boolean goes = self instanceof Monster;
        Method own = OWN.get(self.getClass());
        if (own != null) {
            try {
                goes = (boolean) own.invoke(self);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // (as a monster would)
            }
        }
        if (goes) {
            self.discard();
            ci.cancel();
        }
    }
}
