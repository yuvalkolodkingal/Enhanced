package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One of Falcon's energy darts (3.8, PLAN 8.4): a bright red bolt, no gravity, never slows, never breaks a block (it
 * ends in a red spark). A projectile ({@code #minecraft:is_projectile} through its damage type {@code falcon_dart}), so
 * shields stop it. Whom it hits is the heroes' rule ({@link AreaTargets#mayHurt}: monsters, those after him, players
 * only with PvP); it passes his own Redwing by. Drawn by {@code client/falcon/FalconDartRenderer}.
 */
public class FalconDartEntity extends ThrowableProjectile {
    private float damage = 4.0F;
    /** How many ticks it flies before it fades. */
    private int maxLife = 14;
    private int life;

    public FalconDartEntity(EntityType<? extends FalconDartEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /**
     * A dart from {@code from} along {@code direction} at {@code speed} blocks a tick, fired by {@code owner}, that
     * fades after {@code range} blocks. Not added to the level.
     */
    public static FalconDartEntity create(Level level, @Nullable Entity owner, Vec3 from, Vec3 direction, double speed,
                                          float damage, double range) {
        FalconDartEntity dart = new FalconDartEntity(ModEntities.FALCON_DART.get(), level);
        dart.setOwner(owner);
        dart.setPos(from);
        dart.damage = damage;
        dart.maxLife = Math.max(1, (int) Math.ceil(range / Math.max(0.1, speed)));
        dart.setDeltaMovement(direction.normalize().scale(speed));
        dart.updateRotation();
        return dart;
    }

    public float damage() {
        return damage;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 at = position();
            level().addParticle(new EnergyParticleOptions(FalconAbilities.RED, 0.45F), at.x, at.y, at.z, 0, 0, 0);
            return;
        }
        if (isRemoved()) {
            return;
        }
        if (!isInWater()) {
            // A thrown thing loses a hundredth of its speed every tick; an energy dart does not.
            setDeltaMovement(getDeltaMovement().scale(1.0 / 0.99));
        }
        if (++life > maxLife && level() instanceof ServerLevel server) {
            spark(server, position(), false);
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (!super.canHitEntity(target) || target == owner || target instanceof FalconDartEntity) {
            return false;
        }
        if (!(target instanceof LivingEntity living) || target instanceof ArmorStand || !living.isAlive()) {
            return false;
        }
        if (target instanceof RedwingEntity drone && owner != null && owner.getUUID().equals(drone.ownerId())) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (owner instanceof LivingEntity shooter) {
            return AreaTargets.mayHurt(shooter, living, false);
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (owner instanceof ServerPlayer shooter && target instanceof LivingEntity living) {
            FalconAbilities.hurt(shooter, living, ModDamageTypes.FALCON_DART, damage, this);
        } else {
            target.invulnerableTime = 0;
            target.hurt(ModDamageTypes.source(server, ModDamageTypes.FALCON_DART, owner, this), damage);
        }
        spark(server, result.getLocation(), true);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        spark(server, result.getLocation(), true);
        discard();
    }

    /** A red spark where it ends (brighter when it hit something). */
    private void spark(ServerLevel server, Vec3 at, boolean hit) {
        server.sendParticles(new EnergyParticleOptions(FalconAbilities.RED, hit ? 1.0F : 0.6F), at.x, at.y, at.z,
                hit ? 6 : 2, 0.1, 0.1, 0.1, 0.12);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("maxLife", maxLife);
        tag.putInt("life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        maxLife = Math.max(1, tag.getInt("maxLife"));
        life = tag.getInt("life");
    }
}
