package com.kolodkingal.enhanced.client.ironman;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Iron Man's suit on the player (3.8, PLAN 5.8): each part of the worn or deployed suit ({@code SuitModels}), posed by
 * the suit's {@code SuitAssembly} while a phase plays, with its emissive pass. Registered by the 3.8 skeleton on every
 * player renderer with an empty body; lane L3 fills it (lane L7's models and choreographies plug in).
 */
public class SuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public SuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
    }
}
