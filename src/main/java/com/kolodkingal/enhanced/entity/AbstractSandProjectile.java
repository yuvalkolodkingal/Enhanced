package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.ability.ApocalypseAbilities;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What the sand lance and the sand boulder have in common: a projectile of Apocalypse's sand that flies at a fixed speed
 * (the game's air drag is undone each tick, so a lance is as fast at the end of its flight as at the start), hurts the
 * first creature its owner may hurt with the damage type {@code sand}, vanishes in a puff on a block, lives at most 60
 * ticks, and never touches a block. It costs Wanda's Chaos Shield the damage it would have done
 * ({@link ShieldCost}). It is in Quicksilver Time like an arrow (it hangs in the air), and Billy or the Phoenix can catch
 * it in flight and throw it back, which gives it a new owner. Never saved and never summoned.
 */
public abstract class AbstractSandProjectile extends ThrowableProjectile implements ShieldCost {
    /** A shot lives at most this many ticks of its own time. */
    public static final int MAX_LIFE = 60;

    private float damage;
    private float speed;
    private float knockback;
    private double life;

    protected AbstractSandProjectile(EntityType<? extends AbstractSandProjectile> type, Level level, float damage, float knockback) {
        super(type, level);
        this.damage = damage;
        this.knockback = knockback;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Sets it flying from {@code from} along {@code direction} at {@code speed} blocks a tick, for {@code owner}. */
    public void launch(LivingEntity owner, Vec3 from, Vec3 direction, float speed, float damage) {
        setOwner(owner);
        setPos(from);
        this.speed = speed;
        this.damage = damage;
        shoot(direction.x, direction.y, direction.z, speed, 0.0F);
    }

    public float damage() {
        return damage;
    }

    @Override
    public float shieldCost() {
        return damage;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        double scale = SlowMotion.timeScale(this);
        if (getPersistentData().getBoolean(ApocalypseAbilities.SLOWED_TAG)) {
            scale *= 0.5; // a Sandstorm slowed it to half, once (the flight below would otherwise undo it at once)
        }
        if ((life += scale) > MAX_LIFE) {
            vanish();
            return;
        }
        // The air takes nothing from it: it flies at its speed, in its own time.
        Vec3 motion = getDeltaMovement();
        if (speed > 0.0F && motion.lengthSqr() > 1.0E-8) {
            setDeltaMovement(motion.normalize().scale(speed * scale));
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target) || !(target instanceof LivingEntity living)) {
            return false;
        }
        return !(getOwner() instanceof LivingEntity owner) || SandControl.canHurt(owner, living);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide() || !(result.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        Vec3 motion = getDeltaMovement();
        victim.hurt(ModDamageTypes.source(level(), ModDamageTypes.SAND, owner, this), damage);
        if (knockback > 0.0F && motion.horizontalDistanceSqr() > 1.0E-6) {
            victim.knockback(knockback, -motion.x, -motion.z);
        }
        vanish();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        vanish();
    }

    /** It ends in a puff of sand. */
    protected void vanish() {
        if (level() instanceof ServerLevel server) {
            SandControl.puff(server, position(), 10, 0.2);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 0.6F, 1.0F);
        }
        discard();
    }
}
