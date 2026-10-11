package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.hex.HexVisions;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * The Vision of the Hex: when Wanda raises a Hex, Vision appears beside her and fights for her while it stands, "so
 * she sort of brings Vision back to life in the Hex". He is drawn like Vision (the player model with his skin and cape
 * and the Mind Stone glowing in his forehead), floats where he likes, and is bound to the Hex that raised him: he can
 * never leave it (see {@code HexManager}).
 *
 * <p>He follows Wanda (or holds his post, as she orders), goes for whatever attacks her or what she attacks, like a
 * tamed wolf, and for what she points him at. He punches hard, and a little way off fires a short Mind Stone beam. He
 * never harms Wanda, the players she allowed into the Hex or their pets, nor any player while PvP is off. When he falls
 * he fades to nothing and Wanda rebuilds him after a while, as long as the Hex stands. Never saved: whoever has a Hex
 * has him, and he is made again when the world is loaded.
 */
public class HexVisionEntity extends PathfinderMob implements OwnableEntity {
    /** Ticks between two hits of his Mind Stone beam. */
    public static final int BEAM_INTERVAL = 10;
    /** Ticks between two punches. */
    public static final int PUNCH_INTERVAL = 20;
    /** The Mind Stone's gold, and the orange he comes apart in. */
    private static final int GOLD = Power.VISION.color;
    private static final int GLOW = Power.VISION.glow;
    /** How far from his place at Wanda's side he lets her get before he follows, and how close he settles to it. */
    private static final double FOLLOW_DISTANCE = 1.8;
    private static final double SETTLE_DISTANCE = 0.4;
    private static final double TELEPORT_DISTANCE = 18.0;
    /** A target farther than this is forgotten. */
    private static final double FORGET_DISTANCE = 56.0;
    /** How long he may be unable to touch his target (out past the wall, behind rock) before he gives up on it. */
    private static final int GIVE_UP_TICKS = 160;

    /** The id of the creature his beam is on, or -1: the client draws the beam from it. */
    private static final EntityDataAccessor<Integer> DATA_BEAM_TARGET =
            SynchedEntityData.defineId(HexVisionEntity.class, EntityDataSerializers.INT);
    /** Wanda (also known to the clients, which keep her own beam from hitting him). */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(HexVisionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    @Nullable
    private UUID hexId;
    /** The target is one Wanda pointed him at, not one he picked himself. */
    private boolean commanded;
    private int punchCooldown;
    private int beamCooldown;
    private int idleOnTarget;
    private int stuckTicks;
    private Vec3 lastPos = Vec3.ZERO;

    public HexVisionEntity(EntityType<? extends HexVisionEntity> type, Level level) {
        super(type, level);
        this.moveControl = new VisionMoveControl(this);
        setNoGravity(true);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                // his top speed in blocks per tick (the client's creative flight is 0.5 or so)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    /** Binds him to Wanda and her Hex, with the strength the config gives him. */
    public void bind(UUID owner, UUID hex) {
        entityData.set(DATA_OWNER, Optional.of(owner));
        this.hexId = hex;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(EnhancedConfig.HEX_VISION_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(EnhancedConfig.HEX_VISION_PUNCH_DAMAGE.get());
        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BEAM_TARGET, -1);
        builder.define(DATA_OWNER, Optional.empty());
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FightGoal());
        goalSelector.addGoal(2, new GuardGoal());
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // ---- who he is ----

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    @Nullable
    public UUID hexId() {
        return hexId;
    }

    /** The Hex that raised him, if it still stands. */
    @Nullable
    private Hex hex() {
        return hexId == null || !(level() instanceof ServerLevel level) ? null : HexManager.byId(level, hexId).orElse(null);
    }

    /** The creature his Mind Stone beam is on, or null (the client draws it from his forehead to there). */
    @Nullable
    public LivingEntity beamTarget() {
        int id = entityData.get(DATA_BEAM_TARGET);
        return id == -1 ? null : level().getEntity(id) instanceof LivingEntity living ? living : null;
    }

    public boolean isBeaming() {
        return entityData.get(DATA_BEAM_TARGET) != -1;
    }

    private boolean stays() {
        Hex hex = hex();
        return hex != null && hex.visionStays();
    }

    // ---- whom he may harm ----

    /** Whether he may harm {@code target}: see {@link HexVisions#mayHarm}. */
    public boolean mayHarm(LivingEntity target) {
        Hex hex = hex();
        return hex != null && level().getServer() != null && HexVisions.mayHarm(level().getServer(), hex, target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return mayHarm(target) && super.canAttack(target);
    }

    /** He never even sets his mind on someone he may not harm. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target != null && !mayHarm(target) ? null : target);
    }

    /** Wanda, the players she allowed and their pets never hurt him. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Hex hex = hex();
        if (attacker != null && (attacker.getUUID().equals(getOwnerUUID()) || (hex != null && HexVisions.isTrusted(hex, attacker)))) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && attacker instanceof LivingEntity enemy) {
            defend(enemy);
        }
        return hurt;
    }

    // ---- orders ----

    /** Something attacked Wanda, or him: he goes for it. */
    public void defend(LivingEntity enemy) {
        if (enemy != getTarget() && mayHarm(enemy)) {
            commanded = false;
            setTarget(enemy);
        }
    }

    /** Wanda attacked something: he joins in, unless he is busy with someone already. */
    public void join(LivingEntity victim) {
        if (getTarget() == null && mayHarm(victim)) {
            commanded = false;
            setTarget(victim);
        }
    }

    /** "Vision, that one!" */
    public void command(LivingEntity target) {
        commanded = true;
        idleOnTarget = 0;
        setTarget(target);
    }

    /** Back to Wanda's side. */
    public void standDown() {
        commanded = false;
        setTarget(null);
        stopBeam();
        getNavigation().stop();
    }

    public boolean isCommanded() {
        return commanded;
    }

    // ---- life ----

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        setAirSupply(getMaxAirSupply());
        if (punchCooldown > 0) {
            punchCooldown--;
        }
        if (beamCooldown > 0) {
            beamCooldown--;
        }
        Hex hex = hex();
        UUID ownerId = getOwnerUUID();
        Player owner = ownerId == null ? null : level().getPlayerByUUID(ownerId);
        // Without the Hex that raised him, or the Wanda who raised it here, he is gone; so is a second one.
        if (hex == null || owner == null || !owner.isAlive() || HexVisions.of(ownerId) != this) {
            vanish(false);
            return;
        }
        // The wall holds him; should something carry him through it anyway, he is back with Wanda.
        if (!hex.contains(position().add(0, getBbHeight() * 0.5, 0))) {
            moveToPost(hex, owner, true);
            return;
        }
        if (tickCount % 5 == 0) {
            checkTarget();
        }
        // Falling behind Wanda, or stuck behind something: he is at her side again.
        if (getTarget() == null && !hex.visionStays()) {
            if (distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE || stuckTicks > 50) {
                moveToPost(hex, owner, true);
            }
        }
        stuckTicks = getNavigation().isInProgress() && position().distanceToSqr(lastPos) < 0.0004 ? stuckTicks + 1 : 0;
        lastPos = position();
    }

    /** Drops the target that is dead, out of reach, or no longer his to harm. */
    private void checkTarget() {
        LivingEntity target = getTarget();
        if (target == null) {
            commanded = false;
            return;
        }
        if (!target.isAlive() || target.level() != level() || !mayHarm(target) || distanceToSqr(target) > FORGET_DISTANCE * FORGET_DISTANCE
                || idleOnTarget > GIVE_UP_TICKS) {
            standDown();
        }
    }

    /** Where he hovers by Wanda: on her right, a little behind her, and a bit above the ground she stands on. */
    private static Vec3 sideOf(Player owner) {
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        return owner.position().add(right.scale(1.9)).add(forward.scale(-0.8)).add(0, 0.9, 0);
    }

    /** The spot nearest to {@code wanted} that is inside the wall (a little clear of it). */
    private static Vec3 insideWall(Hex hex, Vec3 wanted) {
        Vec3 radial = wanted.subtract(hex.center());
        double limit = hex.radius() - 2.5;
        return radial.lengthSqr() <= limit * limit ? wanted : hex.center().add(radial.normalize().scale(limit));
    }

    /** At Wanda's side (or, with her out past the wall, at the wall nearest to her), in a puff of his gold. */
    private void moveToPost(Hex hex, Player owner, boolean puff) {
        Vec3 spot = insideWall(hex, sideOf(owner));
        if (puff && level() instanceof ServerLevel server) {
            server.sendParticles(new EnergyParticleOptions(GOLD, 1.2F), getX(), getY() + 1, getZ(), 12, 0.3, 0.6, 0.3, 0.04);
        }
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        teleportTo(spot.x, spot.y, spot.z);
        stuckTicks = 0;
        if (puff && level() instanceof ServerLevel server) {
            server.sendParticles(new EnergyParticleOptions(GOLD, 1.2F), spot.x, spot.y + 1, spot.z, 12, 0.3, 0.6, 0.3, 0.04);
        }
    }

    // ---- appearing and fading ----

    /** He appears: a burst of the Mind Stone's gold with Wanda's red in it. */
    public void materialize() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new EnergyParticleOptions(GOLD, 1.8F), getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.1);
            server.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 1.6F), getX(), getY() + 1, getZ(), 30, 0.5, 0.9, 0.5, 0.15);
            server.sendParticles(new EnergyParticleOptions(GLOW, 1.2F), getX(), getY() + 1.9, getZ(), 6, 0.1, 0.1, 0.1, 0.02);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.VISION_PHASE.get(), SoundSource.PLAYERS, 1.0F, 1.15F);
        }
    }

    /** He fades to nothing: pieces of him rise away in the Mind Stone's gold. */
    private void fade() {
        if (level() instanceof ServerLevel server) {
            AABB box = getBoundingBox();
            for (int i = 0; i < 48; i++) {
                double x = box.minX + random.nextDouble() * box.getXsize();
                double y = box.minY + random.nextDouble() * box.getYsize();
                double z = box.minZ + random.nextDouble() * box.getZsize();
                // count 0: the "spread" is the particle's own velocity, straight up, so each piece streaks away
                server.sendParticles(new EnergyParticleOptions(i % 3 == 0 ? GLOW : GOLD, 1.3F), x, y, z, 0, 0.0, 1.0, 0.0, 0.05 + random.nextDouble() * 0.12);
            }
            server.playSound(null, getX(), getY(), getZ(), ModSounds.VISION_PHASE.get(), SoundSource.PLAYERS, 1.0F, 0.75F);
        }
    }

    /** He goes (the Hex came down, Wanda left): he fades, and no one is left to rebuild. */
    public void vanish(boolean visibly) {
        if (isRemoved()) {
            return;
        }
        if (visibly) {
            fade();
        }
        discard();
    }

    /** He falls: he fades, and Wanda starts to rebuild him. Not a death: nothing dropped, no body. */
    @Override
    public void die(DamageSource source) {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        fade();
        HexVisions.fell(this);
        discard();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            HexVisions.joined(this);
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!level().isClientSide) {
            HexVisions.left(this);
        }
    }

    // ---- fighting ----

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || !mayHarm(living)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel server) {
            // Dense: the punch lands like a falling building (the density the real Vision can take on).
            target.push(0, 0.25, 0);
            target.hurtMarked = true;
            Vec3 at = living.position().add(0, living.getBbHeight() * 0.6, 0);
            server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0.3);
            server.sendParticles(new EnergyParticleOptions(GOLD, 1.3F), at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.05);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        return hit;
    }

    /** The Mind Stone beam is on the target: a hit every {@link #BEAM_INTERVAL} ticks. */
    private void beamAt(LivingEntity target) {
        if (!isBeaming() && level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.8F);
        }
        entityData.set(DATA_BEAM_TARGET, target.getId());
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (beamCooldown <= 0) {
            beamCooldown = BEAM_INTERVAL;
            target.hurt(ModDamageTypes.source(server, ModDamageTypes.MIND_STONE_BEAM, this, this),
                    EnhancedConfig.HEX_VISION_BEAM_DAMAGE.get().floatValue());
        }
        if (tickCount % 2 == 0) {
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.6, 0);
            server.sendParticles(new EnergyParticleOptions(GLOW, 1.4F), end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.12);
        }
    }

    private void stopBeam() {
        if (isBeaming()) {
            entityData.set(DATA_BEAM_TARGET, -1);
        }
    }

    /** Whether his fist reaches the target from where he is. */
    private boolean withinFist(LivingEntity target) {
        return getBoundingBox().inflate(1.1).intersects(target.getBoundingBox());
    }

    private boolean clearTo(Vec3 point) {
        return level().clip(new ClipContext(getEyePosition(), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    /** Flies to {@code point}: straight there if nothing is in the way, round it if there is. */
    private void flyTo(Vec3 point, double speed) {
        if (clearTo(point)) {
            getNavigation().stop();
            getMoveControl().setWantedPosition(point.x, point.y, point.z, speed);
        } else if (tickCount % 5 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(point.x, point.y, point.z, speed);
        }
    }

    /** Goes for the target: closes in on it, punches it up close and beams it from a way off. */
    private class FightGoal extends Goal {
        FightGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            setAggressive(true);
            idleOnTarget = 0;
        }

        @Override
        public void stop() {
            setAggressive(false);
            stopBeam();
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            Hex hex = hex();
            if (target == null || hex == null) {
                return;
            }
            getLookControl().setLookAt(target, 40.0F, 40.0F);
            boolean touching = withinFist(target);
            double reach = EnhancedConfig.HEX_VISION_BEAM_RANGE.get();
            Vec3 chest = target.position().add(0, target.getBbHeight() * 0.6, 0);
            boolean beaming = !touching && distanceToSqr(chest) <= reach * reach && hasLineOfSight(target);
            if (touching) {
                stopBeam();
                getNavigation().stop();
                if (punchCooldown <= 0) {
                    punchCooldown = PUNCH_INTERVAL;
                    swing(InteractionHand.MAIN_HAND);
                    doHurtTarget(target);
                }
            } else if (beaming) {
                beamAt(target);
            } else {
                stopBeam();
            }
            if (!hex.visionStays() && !touching) {
                // Closes in on the target, but not past the wall: out beyond it he can only beam.
                flyTo(insideWall(hex, chest), 1.2);
            }
            idleOnTarget = touching || beaming ? 0 : idleOnTarget + 1;
        }
    }

    /** Stays by Wanda's side, hovering, while there is nothing to fight (and he is not told to hold his post). */
    private class GuardGoal extends Goal {
        /** On his way to his place: from {@link #FOLLOW_DISTANCE} off it, until within {@link #SETTLE_DISTANCE}. */
        private boolean following;

        GuardGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            UUID ownerId = getOwnerUUID();
            return getTarget() == null && !stays() && ownerId != null && level().getPlayerByUUID(ownerId) != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            UUID ownerId = getOwnerUUID();
            Player owner = ownerId == null ? null : level().getPlayerByUUID(ownerId);
            Hex hex = hex();
            if (owner == null || hex == null) {
                return;
            }
            Vec3 post = insideWall(hex, sideOf(owner));
            double distance = position().distanceTo(post);
            following = distance > FOLLOW_DISTANCE || (following && distance > SETTLE_DISTANCE);
            if (following) {
                flyTo(post, Mth.clamp(distance / 5.0, 0.4, 1.6));
            } else {
                getNavigation().stop();
            }
        }
    }

    /**
     * Flying by his own steering: he accelerates smoothly towards where he is wanted and eases off as he gets there
     * (the stock flying control would overshoot and swing about his spot).
     */
    private static class VisionMoveControl extends MoveControl {
        VisionMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            operation = Operation.WAIT;
            mob.setNoGravity(true);
            Vec3 to = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
            double distance = to.length();
            if (distance < 0.1) {
                return;
            }
            double top = mob.getAttributeValue(Attributes.FLYING_SPEED) * speedModifier;
            Vec3 want = to.scale(Math.min(top, distance * 0.25) / distance);
            Vec3 velocity = mob.getDeltaMovement();
            mob.setDeltaMovement(velocity.add(want.subtract(velocity).scale(0.3)));
            // He turns to where he is going, unless a goal has his eyes on something.
            if (to.x * to.x + to.z * to.z > 0.25) {
                float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
                mob.setYRot(rotlerp(mob.getYRot(), yaw, 40.0F));
            }
        }
    }

    // ---- what he is made of ----

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
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
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }
}
