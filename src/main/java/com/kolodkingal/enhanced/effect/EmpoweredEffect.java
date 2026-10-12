package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Empowered by Apocalypse's Power Bestowal (he enhances his own, as he did the Horsemen): three more attack damage, four
 * more armour, a tenth quicker on the feet, and one health back every two seconds. A hero who is empowered also recharges
 * his ability cooldowns faster, which is not done here but by {@code ApocalypseAbilities.hasten} (it needs the player's
 * power data); the effect is what it looks for. The effect is time-limited ({@code [apocalypse].bestowTicks}) and ends
 * with the bestower's power.
 */
public class EmpoweredEffect extends MobEffect {
    /** How often the healing comes: one health every two seconds. */
    public static final int HEAL_EVERY_TICKS = 40;

    public EmpoweredEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2B84B);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, Enhanced.id("empowered_attack"), 3.0, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ARMOR, Enhanced.id("empowered_armor"), 4.0, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Enhanced.id("empowered_speed"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % HEAL_EVERY_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1.0F);
        }
        return true;
    }
}
