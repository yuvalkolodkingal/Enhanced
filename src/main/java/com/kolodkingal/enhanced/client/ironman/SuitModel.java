package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.ironman.SuitParts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * How one suit is drawn (3.8, PLAN 7.1): its texture, its emissive texture, and each of its parts drawn in the space of
 * the player model's part it rides on (the caller has applied that part's transform and the {@code SuitAssembly}'s
 * pose). Lane L7 implements one per suit ({@link SuitModels#get}); lanes L3 and L4 call only this.
 */
public interface SuitModel {
    /** The suit's texture (128 x 128). */
    ResourceLocation texture();

    /** The emissive texture (eyes, reactor, repulsors, thrusters), or null. */
    @Nullable
    ResourceLocation glowTexture();

    /**
     * Draws one part.
     *
     * @param color ARGB tint (0xFFFFFFFF: none)
     */
    void renderPart(SuitParts part, PoseStack pose, VertexConsumer buffer, int light, int overlay, int color);
}
