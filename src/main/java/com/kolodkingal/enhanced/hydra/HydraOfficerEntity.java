package com.kolodkingal.enhanced.hydra;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Hydra officer (3.6, the plan's 15.2): he commands an outpost from its command room by day and its radio room by
 * night, fires an energy pistol (3, every 25 ticks, range 16), and <b>raises the alarm the moment he sees an
 * intruder</b> ({@link HydraAlarm}). While he lives, the soldiers of his side within 16 blocks have Resistance I. In a
 * black field jacket with red shoulder boards and a headset; no helmet. Everything else is a soldier's ({@link
 * HydraSoldierEntity}).
 */
public class HydraOfficerEntity extends HydraSoldierEntity {
    /** How far his presence steadies his men (Resistance I). */
    public static final double COMMAND_RANGE = 16.0;
    /** How many skins officers have. */
    public static final int SKINS = 2;

    public HydraOfficerEntity(EntityType<? extends HydraOfficerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.ARMOR, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    public boolean isOfficer() {
        return true;
    }

    @Override
    public Arms arms() {
        return Arms.PISTOL;
    }

    /** An officer has no rank among the soldiers; he only changes skin. */
    @Override
    public void setRank(Rank rank, int variant) {
        setVariant(Math.floorMod(variant, SKINS));
        this.xpReward = 12;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && (tickCount + getId()) % 40 == 0 && isAlive()
                && !HydraLoyalty.isTraitor(this)) {
            for (HydraSoldierEntity soldier : level.getEntitiesOfClass(HydraSoldierEntity.class,
                    getBoundingBox().inflate(COMMAND_RANGE), s -> s != this && HydraLoyalty.sameSide(this, s)
                            && s.distanceToSqr(this) <= COMMAND_RANGE * COMMAND_RANGE)) {
                soldier.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false), this);
            }
        }
    }
}
