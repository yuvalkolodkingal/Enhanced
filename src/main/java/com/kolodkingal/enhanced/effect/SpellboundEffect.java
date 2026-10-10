package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Held by Billy's magic (WandaVision: he freezes the S.W.O.R.D. soldiers). A spellbound creature can't move, jump or
 * hurt anyone; a mob also forgets its target and stops pathfinding (and can't pick a new target while it lasts: see
 * {@code CommonEvents.onChangeTarget}). Blue sparkles circle its head.
 */
public class SpellboundEffect extends MobEffect {

    public SpellboundEffect() {
        super(MobEffectCategory.HARMFUL, BillyAbilities.MAIN);
        AttributeModifier.Operation total = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Enhanced.id("spellbound_movement"), -1.0, total);
        addAttributeModifier(Attributes.FLYING_SPEED, Enhanced.id("spellbound_flying"), -1.0, total);
        addAttributeModifier(Attributes.JUMP_STRENGTH, Enhanced.id("spellbound_jump"), -1.0, total);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, Enhanced.id("spellbound_attack"), -1.0, total);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            mob.getNavigation().stop();
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 3 == 0) {
            // Three sparkles circling the head.
            double radius = entity.getBbWidth() * 0.5 + 0.25;
            double y = entity.getEyeY() + 0.35;
            for (int i = 0; i < 3; i++) {
                double angle = entity.tickCount * 0.35 + i * Mth.TWO_PI / 3;
                level.sendParticles(new EnergyParticleOptions(i == 0 ? BillyAbilities.CORE : BillyAbilities.MAIN, 0.8F),
                        entity.getX() + Math.cos(angle) * radius, y, entity.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }
}
