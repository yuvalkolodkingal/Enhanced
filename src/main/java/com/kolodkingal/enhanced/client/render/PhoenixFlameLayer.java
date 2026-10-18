package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The cosmic fire burning round the Phoenix while the flames are up ({@link Live#PHOENIX_BLAZE}: while she charges and
 * for a moment after the Burst): tongues of flame scrolling up her body, drawn over the skin and full-bright, a second
 * fatter pass of gold behind them, and a ball of fire in each hand while she gathers the Burst. They brighten once the
 * charge passes nine tenths. Her own layer, because {@code PowerAuraLayer} belongs to the older heroes.
 */
public class PhoenixFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int FLAME = 0x78FF8A1F;
    private static final int GLOW = 0x40FFD65A;
    private static final int FULL_BRIGHT = 0xF000F0;

    private final PlayerModel<AbstractClientPlayer> model;

    public PhoenixFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, PlayerModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        // (3.4) Phoenix, or a custom hero with her Burst.
        if (!data.has(HeroAbility.PHOENIX_BURST) || !data.is(Live.PHOENIX_BLAZE) || player.isInvisible()) {
            return;
        }
        float t = player.tickCount + partialTicks;
        float charge = PhoenixFx.chargeOf(player, t);
        boolean bright = charge >= PhoenixMath.BRIGHT_CHARGE;
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float pulse = 0.7F + 0.3F * Mth.sin(0.5F * t);
        drawFlames(poseStack, buffer, model, t, pulse * (bright ? 1.3F : 1.0F), FLAME, GLOW);

        if (data.is(Live.PHOENIX_CHARGING)) {
            VertexConsumer orbs = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            float swell = 1.0F + 0.5F * Math.max(charge, 0.0F);
            handOrb(poseStack, orbs, model.rightArm, -1.0F / 16.0F, t, swell);
            handOrb(poseStack, orbs, model.leftArm, 1.0F / 16.0F, t + 20.0F, swell);
        }
    }

    /**
     * The flames over a (posed) model at {@code strength} (1: as hers at rest), in two colours (ARGB, the alpha their full
     * strength): the Phoenix's, and (3.4) the flame aura of a custom hero's look, in his colours.
     */
    public static void drawFlames(PoseStack poseStack, MultiBufferSource buffer, PlayerModel<AbstractClientPlayer> model, float t,
                                  float strength, int flameArgb, int glowArgb) {
        model.setAllVisible(true);
        // The second layers of the skin are not part of the flame.
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
        // Fatter and slower behind, then the main tongues scrolling upward over the body.
        poseStack.pushPose();
        poseStack.scale(1.1F, 1.0F, 1.1F);
        VertexConsumer glow = buffer.getBuffer(RenderType.energySwirl(PhoenixFx.FLAME_TEXTURE, (t * 0.004F) % 1.0F, (-t * 0.02F) % 1.0F));
        model.renderToBuffer(poseStack, glow, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, scaleAlpha(glowArgb, strength));
        poseStack.popPose();
        VertexConsumer flame = buffer.getBuffer(RenderType.energySwirl(PhoenixFx.FLAME_TEXTURE, (t * 0.01F) % 1.0F, (-t * 0.04F) % 1.0F));
        model.renderToBuffer(poseStack, flame, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, scaleAlpha(flameArgb, strength));
    }

    /** An ARGB colour with its alpha multiplied (and kept in range). */
    private static int scaleAlpha(int argb, float factor) {
        int alpha = Mth.clamp(Math.round(((argb >>> 24) & 0xFF) * factor), 0, 255);
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** A spinning ball of fire cupped in the hand: three crossed squares, gold outside and white-hot in the middle. */
    private static void handOrb(PoseStack poseStack, VertexConsumer vc, ModelPart arm, float x, float t, float swell) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.translate(x, 10.0F / 16.0F, 0.0F);
        float pulse = (0.85F + 0.15F * Mth.sin(t * 0.5F)) * swell;
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 9.0F + i * 60.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(t * 6.0F + i * 45.0F));
            Matrix4f m = poseStack.last().pose();
            quad(vc, m, 0.28F * pulse, 0x80000000 | PhoenixAbilities.FIRE_MAIN);
            quad(vc, m, 0.13F * pulse, 0xC0000000 | PhoenixAbilities.FIRE_CORE);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float s, int argb) {
        vc.addVertex(m, -s, -s, 0).setColor(argb);
        vc.addVertex(m, s, -s, 0).setColor(argb);
        vc.addVertex(m, s, s, 0).setColor(argb);
        vc.addVertex(m, -s, s, 0).setColor(argb);
    }
}
