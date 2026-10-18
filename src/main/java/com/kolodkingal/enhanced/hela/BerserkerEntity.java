package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A berserker of Hela's army (3.5): an undead Asgardian warrior with grey skin, cracked dark-green armour and glowing green
 * eyes, a notched black sword in his hand (a part of his model, not an item). 24 health, 6 damage, walks at 0.3; he climbs
 * out of the floor in a second ({@link ArmyMob#beginRise}) before he moves; he does not burn in the sun and drops nothing.
 * The boss raises him in waves; the hero raises three (five in Asgard) to fight for her.
 */
public class BerserkerEntity extends ArmyMob {
    public static final int RISE_TICKS = 20;

    public BerserkerEntity(EntityType<? extends BerserkerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    public int riseTicks() {
        return RISE_TICKS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerTargetGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers());
        addArmyTargets(3);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BERSERKER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BERSERKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BERSERKER_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }
}
