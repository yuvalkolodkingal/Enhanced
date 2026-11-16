package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * (3.16.1) 26.3: 1.21.1's {@code RenderType.eyes}, glowing eyes and glow masks drawn full-bright and ADDED to what is
 * under them. 26.1's own eyes pipeline blends them as plain translucency instead, so a mask's dim pixels covered the face
 * like paint (the Scarlet Witch's eyes became red blocks). Its energy swirl is the old eyes exactly: emissive, additive,
 * cut out where the mask is clear; with no offset its texture stays still. One render type per texture, made once.
 */
public final class GlowTypes {
    private static final Map<Identifier, RenderType> TYPES = new ConcurrentHashMap<>();

    private GlowTypes() {
    }

    public static RenderType eyes(Identifier texture) {
        return TYPES.computeIfAbsent(texture, t -> RenderTypes.energySwirl(t, 0.0F, 0.0F));
    }
}
