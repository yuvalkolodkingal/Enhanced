package com.kolodkingal.enhanced.client.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.21.4 on: the What If look ({@link WhatIfLooks}) from a render state, whose entity {@code RenderBridge} keeps.
 */
public class WhatIfLayer<M extends EntityModel<? super net.minecraft.client.renderer.entity.state.LivingEntityRenderState>>
        extends RenderLayer<net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M> {
    public WhatIfLayer(RenderLayerParent<net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, float netHeadYaw, float headPitch) {
        if (com.kolodkingal.enhanced.compat.RenderBridge.entity(state) instanceof LivingEntity entity) {
            WhatIfLooks.draw(getParentModel(), poseStack, buffer, packedLight, entity, state.ageInTicks);
        }
    }
}
