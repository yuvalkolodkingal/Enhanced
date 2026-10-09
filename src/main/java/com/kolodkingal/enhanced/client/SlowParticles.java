package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.client.particle.EnergyParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Quicksilver Time for particles: smoke, splashes, sparks and explosions near him only get every Nth tick, and each of
 * those ticks is drawn stretched over N ticks, so they drift smoothly instead of stuttering. The heroes' own energy
 * particles (Quicksilver's streak included) stay at full speed.
 */
public final class SlowParticles {

    /** @return true if this particle must skip this tick */
    public static boolean skipTick(Particle particle) {
        SlowedParticle slowed = (SlowedParticle) particle;
        if (!slowed(particle)) {
            slowed.enhanced$setSlowPhase(-1);
            return false;
        }
        int phase = (slowed.enhanced$slowPhase() + 1) % SlowMotion.factor();
        slowed.enhanced$setSlowPhase(phase);
        return phase != 0;
    }

    /** Spreads the movement of the last real tick over the whole slowed cycle. */
    public static float partialTick(Particle particle, float partialTick) {
        int phase = ((SlowedParticle) particle).enhanced$slowPhase();
        return phase < 0 ? partialTick : (phase + partialTick) / SlowMotion.factor();
    }

    private static boolean slowed(Particle particle) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || particle instanceof EnergyParticle) {
            return false;
        }
        List<Vec3> zones = SlowMotion.zones(mc.level);
        return !zones.isEmpty() && SlowMotion.isInZone(mc.level, particle.getPos());
    }

    private SlowParticles() {
    }
}
