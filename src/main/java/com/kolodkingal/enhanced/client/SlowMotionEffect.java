package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SlowMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/** While time is slowed around you, the world loses most of its colour and turns cold blue, like a film slow-motion shot. */
public final class SlowMotionEffect {
    private static final ResourceLocation SHADER = Enhanced.id("shaders/post/slow_motion.json");
    private static boolean active;

    static void tick(Minecraft mc) {
        boolean slowed = mc.player != null && mc.level != null && SlowMotion.isInZone(mc.level, mc.player.position());
        PostChain current = mc.gameRenderer.currentEffect();
        if (slowed && !active && current == null) {
            mc.gameRenderer.loadEffect(SHADER);
            active = true;
        } else if (!slowed && active) {
            stop(mc);
        } else if (active && (current == null || !current.getName().equals(SHADER.toString()))) {
            // Something else (e.g. spectating a creeper) replaced our effect.
            active = false;
        }
    }

    static void stop(Minecraft mc) {
        PostChain current = mc.gameRenderer.currentEffect();
        if (active && current != null && current.getName().equals(SHADER.toString())) {
            mc.gameRenderer.shutdownEffect();
        }
        active = false;
    }

    private SlowMotionEffect() {
    }
}
