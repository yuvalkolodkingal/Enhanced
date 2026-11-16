package com.kolodkingal.enhanced.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * (3.16.1) Glowing eyes and glow masks: full-bright, and ADDED to what is under them, as 1.21.1's {@code RenderType.eyes}
 * draws them. From 1.21.2 the game's eyes blend as plain translucency, so a mask's dim pixels covered the face like paint
 * (the Scarlet Witch's glowing eyes became red blocks); there every {@code RenderType.eyes} call is renamed to this, and
 * this is the energy swirl with no offset: emissive, additive, cut out where the mask is clear, its texture still. One
 * render type per texture, made once.
 */
public final class GlowTypes {
    private static final Map<ResourceLocation, RenderType> TYPES = new ConcurrentHashMap<>();

    private GlowTypes() {
    }

    public static RenderType eyes(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, t ->
                //#if >=1.21.4
                //$$ RenderType.energySwirl(t, 0.0F, 0.0F));
                //#else
                RenderType.eyes(t));
                //#endif
    }
}
