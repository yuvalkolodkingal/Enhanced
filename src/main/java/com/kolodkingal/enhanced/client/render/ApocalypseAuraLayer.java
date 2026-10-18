package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * What lies over the body of a player because of Apocalypse: while En Sabah Nur holds his shield up, a faint gold lattice
 * shimmers on him (so it is seen from afar, and from the outside of the sphere); while anyone holds the Essence to take it
 * in, a blue tint grows over their skin, to the end of the transference.
 */
public final class ApocalypseAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The alpha of the lattice on the body while the shield is up. */
    public static final float SHIELD_ALPHA = 0.25F;
    /** The alpha the blue tint reaches at the end of the transference. */
    public static final float TRANSFERENCE_ALPHA = 0.6F;
    private static final int GOLD = SandPalette.GOLD;
    private static final int SAPPHIRE = SandPalette.SAPPHIRE;

    private final PlayerModel<AbstractClientPlayer> model;

    public ApocalypseAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, PlayerModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    /** How far the transference has gone (0 to 1), or 0 when the player is not taking in the Essence. */
    public static float transference(AbstractClientPlayer player) {
        if (!player.isUsingItem() || !player.getUseItem().is(ModItems.ESSENCE.get())) {
            return 0.0F;
        }
        return Mth.clamp(player.getTicksUsingItem() / (float) Math.max(1, ModItems.ESSENCE.get().holdTicks()), 0.0F, 1.0F);
    }

    /** The tint of the lattice and of the transference as a colour (ARGB), for an alpha. */
    static int argb(float alpha, int rgb) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        PowerData data = Powers.get(player);
        // (3.4) Apocalypse, or a custom hero with his shield.
        boolean shielded = data.has(HeroAbility.APOCALYPSE_SHIELD) && data.chaosShieldUp(player.level().getGameTime());
        float transference = transference(player);
        if (!shielded && transference <= 0.0F) {
            return;
        }
        float t = player.tickCount + partialTicks;
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.setAllVisible(true);
        if (shielded) {
            VertexConsumer lattice = buffer.getBuffer(RenderType.energySwirl(ShieldFx.WEB, (t * 0.01F) % 1.0F, (t * 0.007F) % 1.0F));
            model.renderToBuffer(poseStack, lattice, packedLight, OverlayTexture.NO_OVERLAY, argb(SHIELD_ALPHA, GOLD));
        }
        if (transference > 0.0F) {
            float pulse = 0.9F + 0.1F * Mth.sin(t * 0.5F);
            VertexConsumer swirl = buffer.getBuffer(RenderType.energySwirl(PowerAuraLayer.SWIRL, (t * 0.02F) % 1.0F, (t * 0.013F) % 1.0F));
            model.renderToBuffer(poseStack, swirl, packedLight, OverlayTexture.NO_OVERLAY, argb(TRANSFERENCE_ALPHA * transference * pulse, SAPPHIRE));
        }
    }
}
