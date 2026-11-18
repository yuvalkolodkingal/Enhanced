package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;

/**
 * (3.17) The mod's own small embers for its fire heroes (Jim Hammond, Source Code Vision): bright orange motes of the mod's energy
 * particle, in place of the game's flame, small flame and lava particles.
 */
public final class Embers {
    public static final int ORANGE = 0xFF8A1E;

    private Embers() {
    }

    /** {@code count} embers round (x, y, z), spread by (dx, dy, dz) and flying out at {@code speed}. */
    public static void burst(ServerLevel level, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        level.sendParticles(new EnergyParticleOptions(ORANGE, 0.42F), x, y, z, count, dx, dy, dz, speed);
    }
}
