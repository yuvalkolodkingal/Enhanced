package com.kolodkingal.enhanced.client.world;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.multiverse.UniverseRecipe;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

/**
 * The air of the other universes (3.19.1): the colour of a random universe's sky haze and fog ({@link UniverseRecipe#fog()}),
 * the green of the Gamma Bomb's world, the smog of Hydra's city (until it is freed: {@code HydraSkyPayload}) and the
 * dark red haze over Ultron's ruins, which also close in round the player.
 */
@Subscriber(side = Side.CLIENT)
public final class WhatIfSky {
    static final int GAMMA = 0x6FD24A;
    static final int HYDRA = 0x6A635A;
    static final int ULTRON = 0x3A2222;
    /** How far one sees in the smog and over the ruins (blocks). */
    static final float HYDRA_FOG = 96.0F;
    static final float ULTRON_FOG = 72.0F;

    private static boolean hydraFreed;

    private WhatIfSky() {
    }

    public static void hydraFreed(boolean freed) {
        hydraFreed = freed;
    }

    /** The colour of the air where the camera is, or -1 for the game's own. */
    static int colour(ResourceKey<Level> dimension, Vec3 at) {
        if (dimension == Multiverse.GAMMA_KEY) {
            return GAMMA;
        }
        if (dimension == Multiverse.HYDRA_KEY) {
            return hydraFreed ? -1 : HYDRA;
        }
        if (dimension == Multiverse.ULTRON_KEY) {
            return ULTRON;
        }
        if (Multiverse.isRandomDimension(dimension)) {
            return UniverseRecipe.of(Multiverse.universeAt(dimension, at.x, at.z)).fog();
        }
        return -1;
    }

    @Subscribe
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int colour = colour(mc.level.dimension(), event.getCamera().getPosition());
        if (colour < 0) {
            return;
        }
        float k = 0.75F;
        event.setRed(Mth.lerp(k, event.getRed(), ((colour >> 16) & 0xFF) / 255.0F));
        event.setGreen(Mth.lerp(k, event.getGreen(), ((colour >> 8) & 0xFF) / 255.0F));
        event.setBlue(Mth.lerp(k, event.getBlue(), (colour & 0xFF) / 255.0F));
    }

    @Subscribe
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        ResourceKey<Level> dimension = mc.level.dimension();
        float wanted = dimension == Multiverse.ULTRON_KEY ? ULTRON_FOG
                : dimension == Multiverse.HYDRA_KEY && !hydraFreed ? HYDRA_FOG : Float.MAX_VALUE;
        float far = event.getFarPlaneDistance();
        if (wanted < far) {
            event.setFarPlaneDistance(wanted);
            event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), wanted * 0.1F));
            event.setCanceled(true);
        }
    }
}
