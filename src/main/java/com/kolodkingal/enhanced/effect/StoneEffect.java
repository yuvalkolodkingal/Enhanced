package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Blocks;

/**
 * What the Infinity Stones do to a creature Thanos turns them on: one class for the three effects, each with its own
 * colour.
 *
 * <ul>
 *     <li><b>Petrified</b> (the Reality Stone): turned to stone. It cannot move, jump, attack or use anything, forgets
 *     whom it hunted, lets go of any spell it held (a beam, a shield), and takes only part of the damage (the damage rule
 *     is {@code ThanosEvents.onStoneDamage}, the rule for attacks and for using items is there too).</li>
 *     <li><b>In the Soul World</b> (the Soul Stone): frozen where it stands, and nothing hurts it: it is not in this
 *     world. It lets go of held spells too.</li>
 *     <li><b>Time locked</b> (the Time Stone): slowed to 40 percent, swings and digs at half speed.</li>
 * </ul>
 */
public class StoneEffect extends MobEffect {
    /** The three effects. */
    public enum Kind {
        PETRIFIED(0x7A7A82),
        SOULWORLD(0xFF8A1E),
        TIME_LOCKED(0x5BDB6A);

        private final int color;

        Kind(int color) {
            this.color = color;
        }
    }

    private final Kind kind;

    public StoneEffect(Kind kind) {
        super(MobEffectCategory.HARMFUL, kind.color);
        this.kind = kind;
        AttributeModifier.Operation total = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        String name = kind.name().toLowerCase(java.util.Locale.ROOT);
        if (kind == Kind.TIME_LOCKED) {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, Enhanced.id(name + "_movement"), -0.6, total);
            addAttributeModifier(Attributes.ATTACK_SPEED, Enhanced.id(name + "_attack_speed"), -0.5, total);
            addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Enhanced.id(name + "_mining"), -0.5, total);
        } else {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, Enhanced.id(name + "_movement"), -1.0, total);
            addAttributeModifier(Attributes.FLYING_SPEED, Enhanced.id(name + "_flying"), -1.0, total);
            addAttributeModifier(Attributes.JUMP_STRENGTH, Enhanced.id(name + "_jump"), -1.0, total);
            addAttributeModifier(Attributes.ATTACK_DAMAGE, Enhanced.id(name + "_attack"), -1.0, total);
        }
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (kind != Kind.TIME_LOCKED && entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            mob.getNavigation().stop();
        }
        if (kind != Kind.TIME_LOCKED && entity instanceof ServerPlayer player) {
            // A hand turned to stone cannot hold a beam or a shield up: the held spells stop.
            PowerTicker.stopHoldAbilities(Powers.get(player));
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 3 == 0) {
            double x = entity.getX();
            double y = entity.getY() + entity.getBbHeight() * 0.5;
            double z = entity.getZ();
            double spread = entity.getBbWidth() * 0.4;
            switch (kind) {
                case PETRIFIED -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                        x, y, z, 3, spread, entity.getBbHeight() * 0.3, spread, 0.0);
                case SOULWORLD -> level.sendParticles(new EnergyParticleOptions(0xFF8A1E, 1.0F),
                        x, y, z, 3, spread, entity.getBbHeight() * 0.4, spread, 0.02);
                case TIME_LOCKED -> {
                    double angle = entity.tickCount * 0.3;
                    level.sendParticles(new EnergyParticleOptions(0x35D46A, 0.9F), x + Mth.cos((float) angle) * spread * 1.5, y,
                            z + Mth.sin((float) angle) * spread * 1.5, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
        return true;
    }
}
