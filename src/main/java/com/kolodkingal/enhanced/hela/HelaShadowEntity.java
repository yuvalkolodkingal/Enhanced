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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A Shadow of Hela (3.5): a small wisp of black-green smoke that hides in the city during a player's Guardian's Watch
 * (lane L2 spawns, moves and judges them: their drift, their flight from the watcher and who may strike them are the
 * Watch's, from outside this class). It has no AI of its own; {@link #setWatcher} is an optional guard (only that player
 * can hurt it). It never despawns by itself, and is never saved (the entity type), so a Watch cut short by a restart
 * leaves none behind. Its renderer ({@code HelaShadowRenderer}) draws a dark, smoky, flickering silhouette of Hela.
 */
public class HelaShadowEntity extends PathfinderMob {
    @Nullable
    private UUID watcher;

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

    /** It has no mind of its own: the Watch moves it (lane L2). The client trails its wisps of smoke. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getRandomX(0.4), getY() + random.nextDouble() * 0.6, getRandomZ(0.4), 0.0, 0.01, 0.0);
            }
            if (random.nextInt(5) == 0) {
                level().addParticle(new EnergyParticleOptions(HelaRules.GREEN, 0.35F), getRandomX(0.3), getY() + 0.3, getRandomZ(0.3), 0.0, 0.0, 0.0);
            }
        }
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
