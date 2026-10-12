package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Empowered as the others see it: for as long as the effect lasts, gold motes rise off the one who bears it. Found from the
 * effect itself (the server sends no payload for it), so a late joiner sees the aura too. (The flow of light from his hand to
 * the one he empowers is the server's own particles.) The one who is Empowered, in first person, sees only a few, small ones:
 * the camera is inside them.
 */
final class ApocalypseEmpower {
    /** How far from the camera the aura is drawn, and every how many ticks a mote rises off one who bears it. */
    private static final double VIEW = 64.0;
    static final int EVERY = 4;

    static void tick(ClientLevel level) {
        if (ApocalypseActing.quiet() || level.getGameTime() % EVERY != 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        RandomSource r = level.random;
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || !living.hasEffect(ModEffects.EMPOWERED)
                    || living.position().distanceToSqr(camera) > VIEW * VIEW) {
                continue;
            }
            boolean mine = firstPerson && living == mc.getCameraEntity();
            if (mine && r.nextBoolean()) {
                continue;
            }
            double a = r.nextDouble() * Math.PI * 2.0;
            double across = living.getBbWidth() * (mine ? 1.0 : 0.6);
            level.addParticle(new EnergyParticleOptions(r.nextBoolean() ? SandPalette.GOLD : SandPalette.GOLD_CORE, mine ? 0.25F : 0.6F),
                    living.getX() + Math.cos(a) * across, living.getY() + r.nextDouble() * living.getBbHeight(),
                    living.getZ() + Math.sin(a) * across, 0.0, 0.03, 0.0);
        }
    }

    static void clear() {
    }

    private ApocalypseEmpower() {
    }
}
