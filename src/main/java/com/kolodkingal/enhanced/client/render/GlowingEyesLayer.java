package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
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
import org.jetbrains.annotations.Nullable;

/**
 * Eyes that glow, drawn full-bright over the skin's eyes:
 * <ul>
 *     <li>the Scarlet Witch's turn red while she wields chaos magic (WandaVision finale, Multiverse of Madness);</li>
 *     <li>Photon's turn azure in Spectrum Vision (WandaVision);</li>
 *     <li>Thor's blaze white-blue when the God of Thunder is unleashed (Ragnarok).</li>
 * </ul>
 */
public class GlowingEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType WANDA = RenderType.eyes(Enhanced.id("textures/entity/scarlet_witch_eyes.png"));
    private static final RenderType PHOTON = RenderType.eyes(Enhanced.id("textures/entity/photon_eyes.png"));
    private static final RenderType THOR = RenderType.eyes(Enhanced.id("textures/entity/thor_eyes.png"));

    public GlowingEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Nullable
    private static RenderType eyes(PowerData data) {
        return switch (data.power()) {
            case SCARLET_WITCH -> data.beamActive() || data.chaosShieldActive() || data.heldEntityId() != -1 ? WANDA : null;
            case PHOTON -> data.spectrumVision() ? PHOTON : null;
            case THOR -> data.toggle(Toggle.GOD_OF_THUNDER) ? THOR : null;
            default -> null;
        };
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType type = eyes(Powers.get(player));
        if (type == null || player.isInvisible()) {
            return;
        }
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(type), 0xF00000, OverlayTexture.NO_OVERLAY);
    }
}
