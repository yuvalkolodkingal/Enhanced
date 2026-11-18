package com.kolodkingal.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Jim Hammond's fireball as every player sees it (3.17). The server flies it ({@code JimFireballs}: it hits, burns and sets the
 * ground alight there); this entity is its body in the world, so each client draws it where it really is, with its size (how long
 * it was charged) and its speed for the flames streaming behind it. It is never saved and does nothing on its own.
 */
public class JimFireballEntity extends Projectile {
    /** How big it is: 1 for a quick throw, up to about 2.2 fully charged. */
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(JimFireballEntity.class, EntityDataSerializers.FLOAT);

    public JimFireballEntity(EntityType<? extends JimFireballEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1.0F);
    }

    public float size() {
        return entityData.get(SIZE);
    }

    public void setSize(float size) {
        entityData.set(SIZE, size);
    }

    /** Puts it where the server's ball is now, moving on at {@code velocity} (per tick). */
    public void follow(Vec3 at, Vec3 velocity) {
        setPos(at);
        setDeltaMovement(velocity);
        hasImpulse = true;
    }

    @Override
    public void tick() {
        // the server moves it (follow); a client carries it on between the server's updates
        xo = getX();
        yo = getY();
        zo = getZ();
        if (level().isClientSide()) {
            setPos(position().add(getDeltaMovement()));
            // sparks shed behind it as it flies: short-lived bright motes of the mod's own energy particle
            Vec3 v = getDeltaMovement();
            for (int i = 0; i < 2; i++) {
                double f = random.nextDouble();
                level().addParticle(new com.kolodkingal.enhanced.particle.EnergyParticleOptions(i == 0 ? 0xFFB040 : 0xFF6A18, 0.28F * size()),
                        getX() - v.x * f + (random.nextDouble() - 0.5) * 0.3 * size(), getY() + getBbHeight() * 0.5 - v.y * f
                        + (random.nextDouble() - 0.5) * 0.3 * size(), getZ() - v.z * f + (random.nextDouble() - 0.5) * 0.3 * size(),
                        -v.x * 0.05 + (random.nextDouble() - 0.5) * 0.04, 0.02 + random.nextDouble() * 0.03,
                        -v.z * 0.05 + (random.nextDouble() - 0.5) * 0.04);
            }
        } else if (tickCount > 400) {
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }
}
