package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.ironman.SuitKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * The suit's extras on the player (3.8, PLAN 7.2), after the Iron Man lane's suit layer drew its parts: the nanites'
 * glowing film while a Mark 50 or Mark 85 forms, the Mark 5's suitcase opening at his feet, the Mark 85's giant
 * hexagonal shield. Seen by everyone (it reads the synced state).
 */
public class SuitExtrasLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public SuitExtrasLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        SuitLook look = SuitLook.of(player, partialTick);
        if (look == null || look.kind == SuitKind.HULKBUSTER) {
            return;
        }
        SuitRender.drawExtras(poseStack, buffer, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                getParentModel(), look);
    }
}
