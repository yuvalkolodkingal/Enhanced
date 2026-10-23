package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An illusory copy of Loki. It looks exactly like him (his skin, his look, what he holds) but it is only light: one hit
 * and it dissolves into warm-white streaks, with a flash of his green magic where it was touched. It never drops
 * anything, gives no experience and is never saved.
 *
 * <p>What a copy does depends on its {@link Mode}; the Duplicates trick uses {@link Mode#DECOY}.
 */
public class LokiIllusionEntity extends PathfinderMob {
    /** What an illusion is for. New modes (copies that fight) are added here. */
    public enum Mode {
        /** Walks off on its own to draw attention away from Loki; it deals no damage. */
        DECOY,
        /**
         * 3.3: a custom hero's Duplication ("clones"): it fights for its caster, the creatures that hurt him or that he hurt,
         * else the nearest monster (one hit still makes it dissolve).
         */
        FIGHTER,
        /**
         * 3.6: the Reality Stone's Illusion: a decoy that walks off while its caster is invisible, whoever its caster
         * is (it lasts while he lives).
         */
        REALITY
    }

    /** The warm-white light an illusion dissolves into (Thor, 2011). */
    public static final int LIGHT_COLOR = 0xDED2BA;
    public static final int DEFAULT_LIFETIME = 160;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(LokiIllusionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> DATA_MODE =
            SynchedEntityData.defineId(LokiIllusionEntity.class, EntityDataSerializers.BYTE);

    private int lifetime = DEFAULT_LIFETIME;
    /** Where a decoy is walking off to, or null once it got there. */
    @Nullable
    private Vec3 destination;

    public LokiIllusionEntity(EntityType<? extends LokiIllusionEntity> type, Level level) {
        super(type, level);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    /**
     * Conjures a copy of {@code owner} at {@code at}, looking like him and holding what he holds.
     *
     * @param walkTo where a decoy walks off to (null: it just wanders)
     */
    public static LokiIllusionEntity conjure(ServerPlayer owner, Vec3 at, @Nullable Vec3 walkTo, Mode mode, int lifetime) {
        LokiIllusionEntity illusion = new LokiIllusionEntity(ModEntities.LOKI_ILLUSION.get(), owner.level());
        illusion.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        illusion.entityData.set(DATA_MODE, (byte) mode.ordinal());
        illusion.lifetime = lifetime;
        illusion.destination = walkTo;
        float yaw = walkTo == null ? owner.getYRot()
                : (float) (Math.atan2(-(walkTo.x - at.x), walkTo.z - at.z) * (180.0 / Math.PI));
        illusion.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        illusion.setYHeadRot(yaw);
        illusion.setYBodyRot(yaw);
        illusion.setLeftHanded(owner.getMainArm() == HumanoidArm.LEFT);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ANIMAL_ARMOR) {
                illusion.setItemSlot(slot, owner.getItemBySlot(slot).copy());
            }
        }
        owner.level().addFreshEntity(illusion);
        return illusion;
    }

    /** Every illusion of this Loki in the level. */
    public static List<LokiIllusionEntity> ownedBy(ServerLevel level, UUID owner) {
        List<LokiIllusionEntity> found = new ArrayList<>();
        level.getEntities(ModEntities.LOKI_ILLUSION.get(), e -> owner.equals(e.ownerId()), found);
        return found;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_MODE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WalkAwayGoal());
        goalSelector.addGoal(2, new FighterAttackGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    public UUID ownerId() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    public Mode mode() {
        Mode[] modes = Mode.values();
        int i = entityData.get(DATA_MODE);
        return i >= 0 && i < modes.length ? modes[i] : Mode.DECOY;
    }

    @Nullable
    public Player owner() {
        UUID id = ownerId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (tickCount >= lifetime) {
            dissolve(null);
            return;
        }
        if (tickCount % 10 == 0) {
            // Without its caster (gone, dead, or no longer Loki, or a custom hero who duplicates) the spell breaks.
            Player owner = owner();
            if (owner == null || !owner.isAlive() || mode() != Mode.REALITY
                    && !Powers.get(owner).has(HeroAbility.LOKI_DUPLICATES)
                    && CustomHeroes.pick(owner, PowerModule.DUPLICATION) == null) {
                dissolve(null);
                return;
            }
            if (mode() == Mode.FIGHTER) {
                chooseFoe(owner);
            }
        }
    }

    /** One touch is enough: anything that hurts an illusion makes it dissolve. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        dissolve(source.getSourcePosition());
        return true;
    }

    /**
     * The illusion breaks up into warm-white light streaks rising off its body, with a flash of the caster's green
     * where it was touched ({@code touched}, or its middle).
     */
    public void dissolve(@Nullable Vec3 touched) {
        if (isRemoved()) {
            return;
        }
        if (level() instanceof ServerLevel server) {
            Player owner = owner();
            int color = AbilityColors.color(owner, HeroAbility.LOKI_DUPLICATES, owner != null ? Powers.color(owner)
                    : Power.LOKI.color);
            int glow = AbilityColors.glow(owner, HeroAbility.LOKI_DUPLICATES, owner != null ? Powers.glow(owner)
                    : Power.LOKI.glow);
            AABB box = getBoundingBox();
            EnergyParticleOptions light = new EnergyParticleOptions(AbilityColors.shade(owner,
                    HeroAbility.LOKI_DUPLICATES, LIGHT_COLOR), 1.3F);
            for (int i = 0; i < 36; i++) {
                double x = box.minX + random.nextDouble() * box.getXsize();
                double y = box.minY + random.nextDouble() * box.getYsize();
                double z = box.minZ + random.nextDouble() * box.getZsize();
                // count 0: the "spread" is the particle's own velocity, straight up, so each one streaks
                server.sendParticles(light, x, y, z, 0, 0.0, 1.0, 0.0, 0.08 + random.nextDouble() * 0.14);
            }
            Vec3 flash = touched == null ? box.getCenter()
                    : new Vec3(Mth.clamp(touched.x, box.minX, box.maxX), Mth.clamp(touched.y, box.minY, box.maxY),
                    Mth.clamp(touched.z, box.minZ, box.maxZ));
            server.sendParticles(new EnergyParticleOptions(color, 2.2F), flash.x, flash.y, flash.z, 14, 0.12, 0.12, 0.12, 0.08);
            server.sendParticles(new EnergyParticleOptions(glow, 1.6F), flash.x, flash.y, flash.z, 8, 0.05, 0.05, 0.05, 0.03);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.LOKI_DISSOLVE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        discard();
    }

    // ---- an illusion is only light ----

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // A decoy can't hurt anyone (3.6: nor can the Reality Stone's).
        return mode() == Mode.FIGHTER && super.doHurtTarget(target);
    }

    /** A fighter's foe: whoever hurt its caster, or whom he hurt, or the nearest monster; never its caster or his friends. */
    private void chooseFoe(Player owner) {
        LivingEntity current = getTarget();
        if (current != null && current.isAlive() && current != owner && distanceToSqr(current) < 24 * 24) {
            return;
        }
        LivingEntity foe = null;
        for (LivingEntity candidate : new LivingEntity[]{owner.getLastHurtByMob(), owner.getLastHurtMob()}) {
            if (candidate != null && candidate.isAlive() && !(candidate instanceof LokiIllusionEntity) && distanceToSqr(candidate) < 16 * 16
                    && AreaTargets.mayHurt(owner, candidate, false)) {
                foe = candidate;
                break;
            }
        }
        if (foe == null) {
            double best = 12 * 12;
            for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(12), m -> m instanceof Enemy && m.isAlive())) {
                if (distanceToSqr(mob) < best && AreaTargets.mayHurt(owner, mob, false)) {
                    best = distanceToSqr(mob);
                    foe = mob;
                }
            }
        }
        setTarget(foe);
    }

    /** A fighter walks at its foe and strikes it, as a monster would. */
    private class FighterAttackGoal extends MeleeAttackGoal {
        FighterAttackGoal() {
            super(LokiIllusionEntity.this, 1.2, true);
        }

        @Override
        public boolean canUse() {
            return mode() == Mode.FIGHTER && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return mode() == Mode.FIGHTER && super.canContinueToUse();
        }
    }

    /** A decoy walks off in its own direction, like the real Loki slipping away. */
    private class WalkAwayGoal extends Goal {
        WalkAwayGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return destination != null && mode() != Mode.FIGHTER;
        }

        @Override
        public boolean canContinueToUse() {
            return destination != null && !getNavigation().isDone();
        }

        @Override
        public void start() {
            if (destination != null && !getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0)) {
                destination = null;
            }
        }

        @Override
        public void stop() {
            destination = null;
        }
    }
}
