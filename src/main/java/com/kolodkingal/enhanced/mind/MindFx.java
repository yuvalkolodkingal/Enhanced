package com.kolodkingal.enhanced.mind;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** The little effects of Vision's mind (3.12). */
public final class MindFx {
    private MindFx() {
    }

    /** The Human Torch's flames, on the client, every other tick. */
    static void flames(Entity torch) {
        var level = torch.level();
        for (int i = 0; i < 3; i++) {
            double x = torch.getX() + (level.random.nextDouble() - 0.5) * 0.8;
            double y = torch.getY() + level.random.nextDouble() * 2.0;
            double z = torch.getZ() + (level.random.nextDouble() - 0.5) * 0.8;
            level.addParticle(level.random.nextInt(3) == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, x, y, z, 0.0, 0.03, 0.0);
        }
    }

    static void burst(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 40, 0.4, 0.8, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 30, 0.4, 0.8, 0.4, 0.02);
    }
}
