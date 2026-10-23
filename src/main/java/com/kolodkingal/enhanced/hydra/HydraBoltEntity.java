package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.network.HydraFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Hydra energy bolt (3.6, the plan's 15.2): Tesseract blue (0x4FB8FF, a 0xDDF4FF core), no gravity, never slows,
 * never burns or breaks a block (it ends in a spark). It is a projectile, so the Reality Stone bubbles it, Wanda's and
 * Strange's shields and Billy's barrier stop it ({@link ShieldCost}); being energy, Magneto cannot catch it.
 *
 * <p>Whom it hits: a soldier's bolt passes through the soldiers on his side (the ally rule, {@link
 * HydraLoyalty#sameSide}), so a turned soldier's bolt hits Hydra and theirs hit him; a bolt of a player's blaster
 * hurts players only when PvP allows it and spares the player's pets and the soldiers turned to his side. Nobody's
 * bolt touches a Creative or Spectator player, an armour stand, a painting or a boat.
 */
public class HydraBoltEntity extends ThrowableProjectile implements ShieldCost {
    /** What the bolt is drawn as: a rifle's or blaster's bolt, the sniper's longer one, the officer's small one. */
    public static final int RIFLE = 0;
    public static final int SNIPER = 1;
    public static final int PISTOL = 2;

    private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(HydraBoltEntity.class,
            EntityDataSerializers.BYTE);

    private float damage = 4.0F;
    /** How far (ticks of its own time) the bolt flies before it fades. */
    private int maxLife = 20;
    private double life;

    public HydraBoltEntity(EntityType<? extends HydraBoltEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /**
     * A bolt from {@code from} along {@code direction} at {@code speed} blocks a tick, owned by {@code owner}, that
     * fades after {@code range} blocks. Not added to the level.
     */
    public static HydraBoltEntity create(Level level, @Nullable Entity owner, Vec3 from, Vec3 direction, double speed,
                                         float damage, double range, int kind) {
        HydraBoltEntity bolt = new HydraBoltEntity(ModEntities.HYDRA_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.setPos(from);
        bolt.damage = damage;
        bolt.maxLife = Math.max(1, (int) Math.ceil(range / Math.max(0.1, speed)));
        bolt.entityData.set(DATA_KIND, (byte) kind);
        bolt.setDeltaMovement(direction.normalize().scale(speed));
        bolt.updateRotation();
        return bolt;
    }

    public float damage() {
        return damage;
    }

    /** {@link #RIFLE}, {@link #SNIPER} or {@link #PISTOL}. */
    public int kind() {
        return entityData.get(DATA_KIND);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, (byte) RIFLE);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    /** What the bolt costs a Chaos Shield that stops it: its damage. */
    @Override
    public float shieldCost() {
        return damage;
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && !isRemoved() && overlapping()) {
            return;
        }
        super.tick();
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        if (!isInWater()) {
            // A thrown thing loses a hundredth of its speed every tick; energy does not.
            setDeltaMovement(getDeltaMovement().scale(1.0 / 0.99));
        }
        if ((life += SlowMotion.timeScale(this)) > maxLife && level() instanceof ServerLevel server) {
            spark(server, position(), false);
            discard();
        }
    }

    /** Whether it starts this tick inside something it can hit (a creature that walked into it), and hits it. */
    private boolean overlapping() {
        AABB probe = getBoundingBox().inflate(0.2);
        for (Entity target : level().getEntities(this, probe, this::canHitEntity)) {
            EntityHitResult hit = new EntityHitResult(target, position());
            if (!Platform.get().hooks().projectileImpactCancelled(this, hit)) {
                hitTargetOrDeflectSelf(hit);
            }
            if (isRemoved()) {
                return true;
            }
        }
        return false;
    }

    /** The one the bolt is fired for: a pet's owner, otherwise whoever fired it. */
    @Nullable
    private Entity master() {
        Entity owner = getOwner();
        if (owner instanceof OwnableEntity pet && pet.getOwner() != null) {
            return pet.getOwner();
        }
        return owner;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (!super.canHitEntity(target) || target == owner || target == master() || target instanceof HydraBoltEntity) {
            return false;
        }
        if (!(target instanceof LivingEntity living) || target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (owner instanceof LivingEntity shooter && HydraLoyalty.sameSide(shooter, living)) {
            return false;
        }
        if (owner instanceof HydraSoldierEntity soldier && soldier.isTurned()) {
            // A turned soldier fights Hydra for his leader: never his leader, nor (kid-fair) any player.
            return !(target instanceof Player) && !soldier.turnedBy(living);
        }
        if (master() instanceof Player shooter) {
            if (target instanceof OwnableEntity pet && shooter.getUUID().equals(pet.getOwnerUUID())) {
                return false;
            }
            if (HydraLoyalty.turnedFor(living, shooter)) {
                return false;
            }
            if (target instanceof Player victim) {
                MinecraftServer server = level().getServer();
                return server != null && server.isPvpAllowed() && shooter.canHarmPlayer(victim);
            }
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
        DamageSource source = ModDamageTypes.source(server, ModDamageTypes.HYDRA_BOLT, master(), this);
        if (master() instanceof Player) {
            // A player's blaster: every bolt lands, whatever the last one did.
            target.invulnerableTime = 0;
        }
        target.hurt(source, damage);
        spark(server, result.getLocation(), true);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server) {
            spark(server, result.getLocation(), true);
            discard();
            HydraSenses.heard(server, result.getLocation());
        }
    }

    /** A blue spark where the bolt ends (a crack and a hiss when it hit something). */
    private void spark(ServerLevel server, Vec3 at, boolean hit) {
        server.sendParticles(new EnergyParticleOptions(0x4FB8FF, hit ? 1.0F : 0.6F), at.x, at.y, at.z, hit ? 5 : 2,
                0.1, 0.1, 0.1, 0.1);
        if (hit) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0.2);
            server.playSound(null, at.x, at.y, at.z, ModSounds.HYDRA_BOLT_HIT.get(), SoundSource.HOSTILE, 0.6F,
                    1.0F + server.random.nextFloat() * 0.2F);
            Net.sendToPlayersNear(server, null, at.x, at.y, at.z, 48.0,
                    new HydraFxPayload(HydraFxPayload.BOLT_HIT, getId(), List.of(at), kind()));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("maxLife", maxLife);
        tag.putDouble("life", life);
        tag.putByte("kind", entityData.get(DATA_KIND));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        maxLife = Math.max(1, tag.getInt("maxLife"));
        life = tag.getDouble("life");
        entityData.set(DATA_KIND, tag.getByte("kind"));
    }
}
