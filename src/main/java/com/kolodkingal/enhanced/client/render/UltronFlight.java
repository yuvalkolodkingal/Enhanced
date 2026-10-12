package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.kolodkingal.enhanced.client.render.FlightEffects.FOOT_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.FOOT_R;
import static com.kolodkingal.enhanced.client.render.FlightEffects.HAND_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.HAND_R;

/**
 * Ultron in the air: the anti-gravity of his palms is pale-blue motes that sink from his hands, the thrust at his feet
 * is the amber of his plasma, and at a sprint two thin ribbons of the pale blue stream behind his hands. A machine's
 * flight: even, a steady few motes, no sparkle.
 */
final class UltronFlight {
    private static final int GRAVITY = 0x9FD8FF;
    private static final int GRAVITY_CORE = 0xEAF8FF;
    private static final int THRUST = 0xFFA23A;
    private static final int THRUST_CORE = 0xFFE9B8;

    /** Motes from the palms (they fall, the air being pushed down) and from the feet (they linger); thickest at a sprint. */
    static void particles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density, boolean local) {
        int budget = st.s >= 1.8F ? 10 : 5;
        // (The local player's own palms are in his face in first person: his feet and the others' hands carry it.)
        int[] sources = local ? new int[]{FOOT_R, FOOT_L} : new int[]{HAND_R, HAND_L, FOOT_R, FOOT_L};
        for (int source : sources) {
            boolean hand = source == HAND_R || source == HAND_L;
            budget -= FlightEffects.lay(level, fx, source, 0.5, 2, 0.12, budget, density,
                    i -> new EnergyParticleOptions(hand ? (i % 3 == 0 ? GRAVITY_CORE : GRAVITY) : (i % 3 == 0 ? THRUST_CORE : THRUST), hand ? 0.6F : 0.8F),
                    hand ? -0.02 : 0.0);
        }
    }

    /** Two thin ribbons of the anti-gravity from the palms at a sprint. */
    static void ribbons(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float time) {
        if (st.s >= 1.8F) {
            FlightEffects.source(pose, vc, camera, fx, HAND_R, 3.0, 0.05F, GRAVITY, 0.4F, 0.0F, 0, 0, time);
            FlightEffects.source(pose, vc, camera, fx, HAND_L, 3.0, 0.05F, GRAVITY, 0.4F, 0.0F, 0, 0, time);
        }
    }

    private UltronFlight() {
    }
}
