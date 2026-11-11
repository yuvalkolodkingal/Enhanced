package com.kolodkingal.enhanced.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.21.4 only: the player model lost its cloak in 1.21.2 (the cape is a model of its own). This draws the cape's box
 * where 1.21.1's {@code renderCloak} drew it: at the pose stack's origin, unrotated, so the mod's cape layers keep their
 * own placement.
 */
public final class Capes {
    private static ModelPart cape;

    private Capes() {
    }

    public static void render(Object model, PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        if (cape == null) {
            cape = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_CAPE)
                    .getChild("body").getChild("cape");
        }
        cape.setPos(0.0F, 0.0F, 0.0F);
        cape.setRotation(0.0F, 0.0F, 0.0F);
        cape.render(poseStack, buffer, light, overlay);
    }
}
