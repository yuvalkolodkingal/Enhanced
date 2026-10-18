package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A Shadow of Hela (3.5): a small wisp of black-green smoke that hides in the city during a player's Guardian's Watch
 * (lane L2 spawns and judges them; one hit of the watcher's strikes it). It has no mind beyond its own drift: it bobs and
 * wanders within a block of where it was put, and flees four blocks from a player who comes within three. Owned by its
 * watcher ({@link #setWatcher}), nothing else can hurt it; it never despawns by itself, and is never saved (the entity
 * type), so a Watch cut short by a restart leaves none behind. Its renderer draws smoke only.
 */
public class HelaShadowEntity extends PathfinderMob {
    /** How far it wanders from where it was put, and how far it flees. */
    public static final double DRIFT = 1.0;
    public static final double FLEE = 4.0;
    public static final double FRIGHT = 3.0;

    @Nullable
    private UUID watcher;
    @Nullable
    private Vec3 home;
    private Vec3 goal = Vec3.ZERO;
    private int nextDrift;

    public HelaShadowEntity(EntityType<? extends HelaShadowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    /** The player whose Watch it belongs to: only he can strike it (null: anyone). */
    public void setWatcher(@Nullable UUID watcher) {
        this.watcher = watcher;
    }

    @Nullable
    public UUID watcher() {
        return watcher;
    }

    @Override
    protected void registerGoals() {
        // (Its drift is its own: see aiStep.)
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getRandomX(0.4), getY() + random.nextDouble() * 0.6, getRandomZ(0.4), 0.0, 0.01, 0.0);
            }
            if (random.nextInt(5) == 0) {
                level().addParticle(new EnergyParticleOptions(HelaRules.GREEN, 0.35F), getRandomX(0.3), getY() + 0.3, getRandomZ(0.3), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (home == null) {
            home = position();
            goal = home;
        }
        Player near = level().getNearestPlayer(this, FRIGHT);
        if (near != null && !near.isSpectator()) {
            Vec3 away = position().subtract(near.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(1.0, 0.0, 0.0);
            }
            goal = position().add(away.normalize().scale(FLEE));
            nextDrift = 60;
        } else if (--nextDrift <= 0) {
            nextDrift = 40 + random.nextInt(40);
            goal = home.add((random.nextDouble() - 0.5) * 2.0 * DRIFT, 0.0, (random.nextDouble() - 0.5) * 2.0 * DRIFT);
        }
        Vec3 to = goal.subtract(position());
        double bob = Math.sin(tickCount * 0.15) * 0.02;
        Vec3 step = to.lengthSqr() > 0.01 ? to.normalize().scale(Math.min(0.12, to.length())) : Vec3.ZERO;
        if (!level().noCollision(this, getBoundingBox().move(step))) {
            step = Vec3.ZERO;
            goal = position();
        }
        setDeltaMovement(step.x, bob, step.z);
    }

    /** Only its watcher can strike it (anyone, if it has none). */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (super.isInvulnerableTo(source)) {
            return true;
        }
        if (watcher == null) {
            return false;
        }
        Entity by = source.getEntity();
        if (by == null && source.getDirectEntity() instanceof Projectile shot) {
            by = shot.getOwner();
        }
        return by == null || !watcher.equals(by.getUUID());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }
}
