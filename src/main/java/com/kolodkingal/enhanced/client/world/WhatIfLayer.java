package com.kolodkingal.enhanced.client.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * The look of a creature of a What If universe (3.19.1), on every creature's renderer: the zombies' rot in Earth-2149
 * and the Gamma Bomb's swollen green hide and glowing veins ({@link WhatIfLooks}). Draws nothing anywhere else.
 * (1.21.4 has its own copy of this class, for render states.)
 */
public class WhatIfLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public WhatIfLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        WhatIfLooks.draw(getParentModel(), poseStack, buffer, packedLight, entity, ageInTicks);
    }
}
