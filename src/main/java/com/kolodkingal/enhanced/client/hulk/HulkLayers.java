package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.GauntletLayer;
import com.kolodkingal.enhanced.client.render.GlowingEyesLayer;
import com.kolodkingal.enhanced.client.render.HandTrackerLayer;
import com.kolodkingal.enhanced.client.render.PowerAuraLayer;
import com.kolodkingal.enhanced.hulk.HulkHooks;
import com.kolodkingal.enhanced.hulk.HulkRage;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The layers of the Hulk (3.6). On {@link HulkPlayerRenderer}, the mod's layers that make sense on his body: the
 * power's aura (on a bigger copy of his body), the glowing eyes (gamma green while he is enraged), the Infinity
 * Gauntlet at his hand (he may wield it, Endgame), his own details ({@link Details}: the arm the Snap burned) and,
 * last, the hand tracker (where his hands are, for the effects). On the player renderers: Bruce Banner's Gamma
 * Inhibitor, a black band on his left wrist ({@link InhibitorBand}). Armour is not drawn on the Hulk ({@code
 * HeroArmour}); it is on Banner.
 */
public final class HulkLayers {
    /** The burned arm over the Hulk's skin (128 x 128, only the right arm painted). */
    public static final ResourceLocation BURNED = Enhanced.id("textures/entity/hulk/burned_arm.png");
    /** Banner's band (64 x 64, the left forearm of the player model). */
    public static final ResourceLocation BAND = Enhanced.id("textures/entity/hulk/inhibitor_band.png");

    /** Adds the layers to the Hulk's renderer (made at every resource reload). */
    public static void addTo(HulkPlayerRenderer renderer, EntityRendererProvider.Context context) {
        renderer.addLayer(new PowerAuraLayer(renderer, new HulkModel(context.bakeLayer(HulkModel.ENERGY))));
        renderer.addLayer(new GlowingEyesLayer(renderer));
        renderer.addLayer(new GauntletLayer(renderer, context.bakeLayer(GauntletLayer.LAYER)));
        renderer.addLayer(new Details(renderer));
        renderer.addLayer(new HandTrackerLayer(renderer));
    }

    /** Whether this player's right arm is burned from his Snap (his own state: only he knows it). */
    static boolean burned(Player player) {
        return player == Minecraft.getInstance().player && HulkHooks.armBurned(player);
    }

    /** In first person: the burned arm. */
    static void firstPersonExtras(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
            ModelPart arm,
                                  boolean right) {
        if (right && burned(player)) {
            arm.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(BURNED)), light,
                    OverlayTexture.NO_OVERLAY);
        }
    }

    /** The Hulk's own details over his skin. */
    static final class Details extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        Details(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                                   float headPitch) {
            if (player.isInvisible() || !burned(player)) {
                return;
            }
            PlayerModel<AbstractClientPlayer> model = getParentModel();
            model.rightArm.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(BURNED)), light,
                    LivingEntityRenderer.getOverlayCoords(player, 0.0F));
        }
    }

    /** Bruce Banner's Gamma Inhibitor on his left wrist (the player model; his own view, his inventory is his). */
    public static final class InhibitorBand extends RenderLayer<AbstractClientPlayer,
            PlayerModel<AbstractClientPlayer>> {
        public InhibitorBand(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                                   float headPitch) {
            if (player.isInvisible() || Powers.power(player) != Power.HULK
                    || !com.kolodkingal.enhanced.hulk.HulkForms.isHuman(player)
                    || !HulkRage.inhibited(player)) {
                return;
            }
            getParentModel().leftArm.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(BAND)), light,
                    LivingEntityRenderer.getOverlayCoords(player, 0.0F));
        }
    }

    private HulkLayers() {
    }
}
