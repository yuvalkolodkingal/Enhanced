package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.kolodkingal.enhanced.client.render.FlightEffects.FOOT_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.FOOT_R;
import static com.kolodkingal.enhanced.client.render.FlightEffects.HAND_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.HAND_R;
import static com.kolodkingal.enhanced.client.render.FlightEffects.SHOULDER_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.SHOULDER_R;

/**
 * The Phoenix in the air: warm embers drifting up from her hands and feet (and a flame from her hands when she moves),
 * and at a sprint two streams of gold from her shoulders like the wings of the firebird.
 */
final class PhoenixFlight {
    /**
     * Embers from the hands and feet, thickest at a sprint; a flame from each hand every fourth tick while she moves.
     */
    static void particles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density, boolean local) {
        int budget = st.s >= 1.8F ? 12 : 6;
        for (int source : new int[]{HAND_R, HAND_L, FOOT_R, FOOT_L}) {
            budget -= FlightEffects.lay(level, fx, source, 0.5, 2, 0.2, budget, density, i -> new EnergyParticleOptions(
                    i % 3 == 2 ? PhoenixMath.FIRE_CORE : i % 5 == 0 ? PhoenixMath.FIRE_EMBER : PhoenixMath.FIRE_MAIN, 0.8F), 0.01);
        }
        if (st.sH > 0.05F && level.getGameTime() % 4 == 0 && level.random.nextFloat() < density) {
            for (int source : new int[]{HAND_R, HAND_L}) {
                Vec3 hand = FlightEffects.at(fx, source, 0);
                if (hand != null) {
                    level.addParticle(ParticleTypes.FLAME, hand.x, hand.y, hand.z, 0.0, 0.02, 0.0);
                }
            }
        }
    }

    /** Two streams of gold from the shoulders at a sprint. */
    static void ribbons(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float time) {
        if (st.s >= 1.8F) {
            FlightEffects.source(pose, vc, camera, fx, SHOULDER_R, 3.5, 0.06F, PhoenixMath.FIRE_GOLD, 0.4F, 0.0F, 0, 0, time);
            FlightEffects.source(pose, vc, camera, fx, SHOULDER_L, 3.5, 0.06F, PhoenixMath.FIRE_GOLD, 0.4F, 0.0F, 0, 0, time);
        }
    }

    private PhoenixFlight() {
    }
}
