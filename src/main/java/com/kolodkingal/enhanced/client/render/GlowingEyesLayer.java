package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.SuitSkins;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
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
 *     <li>Thor's blaze white-blue when the God of Thunder is unleashed (Ragnarok), and while he fires lightning.</li>
 * </ul>
 * The glow has to sit exactly on the eyes of the skin being drawn, and skins don't keep their eyes in one place.
 * Every ordinary Minecraft skin has them on the face's fifth row (y = 12 of the texture), where
 * {@code scarlet_witch_eyes.png} and {@code thor_eyes.png} paint the glow: that serves her own skin (no crown) and
 * every Thor look, "your own skin" included. But the Scarlet Witch's suit has its eyes two rows lower (y = 14:
 * {@code scarlet_witch_suit_eyes.png}), and Thor's Ragnarok look has an eyepatch over one of them, so only the other
 * glows ({@code thor_ragnarok_eyes.png}). The glow is the same wherever it sits: three rows, dim, bright, dim.
 */
public class GlowingEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType WANDA = eyeTexture("scarlet_witch_eyes");
    private static final RenderType WANDA_SUIT = eyeTexture("scarlet_witch_suit_eyes");
    private static final RenderType THOR = eyeTexture("thor_eyes");
    private static final RenderType THOR_RAGNAROK = eyeTexture("thor_ragnarok_eyes");

    public GlowingEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static RenderType eyeTexture(String name) {
        return RenderType.eyes(Enhanced.id("textures/entity/" + name + ".png"));
    }

    /** The eyes this player glows with right now (the ones that match the skin being drawn), or null if they don't. */
    @Nullable
    private static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return switch (data.power()) {
            case SCARLET_WITCH -> data.beamActive() || data.chaosShieldActive() || data.heldEntityId() != -1
                    ? (SuitSkins.suitFor(player) != null ? WANDA_SUIT : WANDA) : null;
            case THOR -> data.toggle(Toggle.GOD_OF_THUNDER) || data.is(Live.CALLING_LIGHTNING)
                    ? ("ragnarok".equals(Variants.current(data).id()) ? THOR_RAGNAROK : THOR) : null;
            default -> null;
        };
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType type = eyes(player, Powers.get(player));
        if (type == null || player.isInvisible()) {
            return;
        }
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(type), 0xF00000, OverlayTexture.NO_OVERLAY);
    }
}
