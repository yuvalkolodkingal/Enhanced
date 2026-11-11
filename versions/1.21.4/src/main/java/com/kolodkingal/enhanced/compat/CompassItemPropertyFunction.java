package com.kolodkingal.enhanced.compat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 1.21.4 only: 1.21.1's compass needle towards a target of the caller's (1.21.4's compass knows only its own targets). */
public final class CompassItemPropertyFunction {
    @FunctionalInterface
    public interface CompassTarget {
        @Nullable
        GlobalPos getPos(ClientLevel level, ItemStack stack, Entity entity);
    }

    private final CompassTarget target;

    public CompassItemPropertyFunction(CompassTarget target) {
        this.target = target;
    }

    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder, int seed) {
        Entity entity = holder != null ? holder : stack.getEntityRepresentation();
        if (entity == null) {
            return 0.0F;
        }
        if (level == null && entity.level() instanceof ClientLevel client) {
            level = client;
        }
        GlobalPos pos = level == null ? null : target.getPos(level, stack, entity);
        long time = level == null ? 0L : level.getGameTime();
        if (pos == null || pos.dimension() != level.dimension()
                || Vec3.atCenterOf(pos.pos()).distanceToSqr(entity.position()) < 1.0E-5) {
            // nowhere to point: the needle turns, as vanilla's does
            return Mth.positiveModulo((time + seed) / 40.0F, 1.0F);
        }
        double rotation = Mth.positiveModulo(entity.getVisualRotationYInDegrees() / 360.0, 1.0);
        Vec3 to = Vec3.atCenterOf(pos.pos());
        double angle = Math.atan2(to.z - entity.getZ(), to.x - entity.getX()) / (Math.PI * 2.0);
        return (float) Mth.positiveModulo(0.5 - (rotation - 0.25 - angle), 1.0);
    }
}
