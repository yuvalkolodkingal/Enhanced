package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SlowMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import com.kolodkingal.enhanced.power.Power;

/**
 * While time is slowed around you, the world loses most of its colour and turns cold blue, like a film slow-motion shot;
 * (3.3) inside Tommy's slow time it turns teal-green instead.
 */
public final class SlowMotionEffect {
    private static final ResourceLocation SHADER = Enhanced.id("shaders/post/slow_motion.json");
    private static final ResourceLocation TOMMY_SHADER = Enhanced.id("shaders/post/slow_motion_tommy.json");
    private static boolean active;
    /** The shader that is on while {@link #active}. */
    private static ResourceLocation shader = SHADER;

    static void tick(Minecraft mc) {
        Power owner = mc.player != null && mc.level != null ? SlowMotion.zoneOwner(mc.level, mc.player.position()) : Power.NONE;
        boolean slowed = owner != Power.NONE;
        ResourceLocation wanted = owner == Power.TOMMY ? TOMMY_SHADER : SHADER;
        if (active && slowed && !wanted.equals(shader)) {
            // Another speedster's slow time holds him now: change the tint.
            stop(mc);
        }
        PostChain current = mc.gameRenderer.currentEffect();
        if (slowed && !active && current == null) {
            mc.gameRenderer.loadEffect(wanted);
            shader = wanted;
            active = true;
        } else if (!slowed && active) {
            stop(mc);
        } else if (active && (current == null || !current.getName().equals(shader.toString()))) {
            // Something else (e.g. spectating a creeper) replaced our effect.
            active = false;
        }
    }

    static void stop(Minecraft mc) {
        PostChain current = mc.gameRenderer.currentEffect();
        if (active && current != null && current.getName().equals(shader.toString())) {
            mc.gameRenderer.shutdownEffect();
        }
        active = false;
    }

    private SlowMotionEffect() {
    }
}
