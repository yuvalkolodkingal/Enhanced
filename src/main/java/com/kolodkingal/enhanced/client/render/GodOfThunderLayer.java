package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** The God of Thunder unleashed (Thor: Ragnarok): his eyes blaze with white-blue lightning. */
public class GodOfThunderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType EYES = RenderType.eyes(Enhanced.id("textures/entity/thor_eyes.png"));

    public GodOfThunderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (data.power() != Power.THOR || !data.toggle(Toggle.GOD_OF_THUNDER) || player.isInvisible()) {
            return;
        }
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(EYES), 0xF00000, OverlayTexture.NO_OVERLAY);
    }
}
