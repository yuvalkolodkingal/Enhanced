package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.CosmicSpider;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.kolodkingal.enhanced.client.render.FlightEffects.FOOT_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.FOOT_R;
import static com.kolodkingal.enhanced.client.render.FlightEffects.HAND_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.HAND_R;
import static com.kolodkingal.enhanced.client.render.FlightEffects.SHOULDER_L;
import static com.kolodkingal.enhanced.client.render.FlightEffects.SHOULDER_R;

/**
 * What Cosmic Spider-Man looks like besides his black-and-white suit (3.18): the white-blue aura of the Enigma Force that
 * hangs round him (motes rising off his body and a few stars), and in flight a trail of cosmic light from his hands and
 * feet, with white streams from his shoulders at a sprint. Thinned by the client's <i>Flight effects</i> option.
 */
final class CosmicSpiderFx {
    private static final int AURA = CosmicSpider.AURA;
    private static final int WHITE = CosmicSpider.WHITE;
    private static final int DEEP = 0x4F8FEA;

    /** His lenses, blazing white (a mask on the face's eyes of his suit, drawn full-bright). */
    static net.minecraft.client.renderer.RenderType eyes() {
        return Eyes.TYPE;
    }

    private static final class Eyes {
        static final net.minecraft.client.renderer.RenderType TYPE = net.minecraft.client.renderer.RenderType.eyes(
                com.kolodkingal.enhanced.Enhanced.id("textures/entity/spider_cosmic_eyes.png"));
    }

    /** Every client tick: the aura of every cosmic player in view. */
    static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) {
            return;
        }
        float density = EnhancedClientConfig.SPEC.isLoaded() && !EnhancedClientConfig.flightEffects() ? 0.35F : 1.0F;
        RandomSource random = level.random;
        for (Player player : level.players()) {
            if (!CosmicSpider.isCosmic(Powers.get(player)) || player.isInvisible() || player.isSpectator()
                    || player.distanceToSqr(mc.player) > 64 * 64) {
                continue;
            }
            boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
            int count = firstPerson ? 1 : 3;
            for (int i = 0; i < count; i++) {
                if (random.nextFloat() > density) {
                    continue;
                }
                double a = random.nextDouble() * Math.PI * 2;
                double r = 0.45 + random.nextDouble() * 0.25;
                double y = player.getY() + random.nextDouble() * player.getBbHeight();
                int color = random.nextInt(4) == 0 ? WHITE : random.nextInt(3) == 0 ? DEEP : AURA;
                level.addParticle(new EnergyParticleOptions(color, 0.55F + random.nextFloat() * 0.35F),
                        player.getX() + Math.cos(a) * r, y, player.getZ() + Math.sin(a) * r,
                        -Math.cos(a) * 0.01, 0.02 + random.nextDouble() * 0.02, -Math.sin(a) * 0.01);
            }
            if (!firstPerson && level.getGameTime() % 6 == 0 && random.nextFloat() < density) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(ParticleTypes.END_ROD, player.getX() + Math.cos(a) * 0.6,
                        player.getY() + 0.4 + random.nextDouble() * 1.4, player.getZ() + Math.sin(a) * 0.6, 0, 0.01, 0);
            }
        }
    }

    /** In flight: cosmic light along the path of his hands and feet, stars at a sprint. */
    static void flightParticles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density) {
        if (!CosmicSpider.isCosmic(Powers.get(player))) {
            return;
        }
        boolean sprint = st.s >= 1.8F;
        int budget = sprint ? 14 : 7;
        for (int source : new int[]{HAND_R, HAND_L, FOOT_R, FOOT_L}) {
            budget -= FlightEffects.lay(level, fx, source, 0.35, 3, 0.12, budget, density,
                    i -> new EnergyParticleOptions(i % 3 == 2 ? WHITE : i % 4 == 0 ? DEEP : AURA, 0.9F), 0.0);
        }
        if (sprint && level.getGameTime() % 3 == 0 && level.random.nextFloat() < density) {
            Vec3 p = FlightEffects.at(fx, FOOT_R, 1);
            if (p != null) {
                level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0, 0);
            }
        }
    }

    /** Ribbons of cosmic light from his feet, and from his shoulders at a sprint. */
    static void flightRibbons(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st,
                              FlightEffects.Fx fx, float time) {
        if (!CosmicSpider.isCosmic(Powers.get(player))) {
            return;
        }
        float sprint = st.s >= 1.8F ? 1.5F : 1.0F;
        FlightEffects.source(pose, vc, camera, fx, FOOT_R, 4.0 * sprint, 0.07F, AURA, 0.55F, 0.05F, 0, 0, time);
        FlightEffects.source(pose, vc, camera, fx, FOOT_L, 4.0 * sprint, 0.07F, AURA, 0.55F, 0.05F, 1.3F, 0, time);
        FlightEffects.source(pose, vc, camera, fx, HAND_R, 2.5 * sprint, 0.04F, WHITE, 0.45F, 0.08F, 0.6F, 0, time);
        FlightEffects.source(pose, vc, camera, fx, HAND_L, 2.5 * sprint, 0.04F, WHITE, 0.45F, 0.08F, 2.1F, 0, time);
        if (st.s >= 1.8F) {
            FlightEffects.source(pose, vc, camera, fx, SHOULDER_R, 5.0, 0.09F, WHITE, 0.4F, 0.0F, 0, 0, time);
            FlightEffects.source(pose, vc, camera, fx, SHOULDER_L, 5.0, 0.09F, WHITE, 0.4F, 0.0F, 0, 0, time);
        }
    }

    private CosmicSpiderFx() {
    }
}
