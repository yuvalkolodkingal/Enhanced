package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Swirling energy drawn over the body:
 * <ul>
 *     <li>the Scarlet Witch in flight: glowing crimson energy around both hands;</li>
 *     <li>Vision while phased: a greenish-yellow aura (Age of Ultron), white for White Vision.</li>
 * </ul>
 */
public class PowerAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation SWIRL = Enhanced.id("textures/entity/energy_swirl.png");
    /** How much of each player's aura shows: the hands, the phased body, and the clock of the last frame. */
    private static final Map<Integer, float[]> SHOWN = new HashMap<>();

    private final PlayerModel<AbstractClientPlayer> model;

    public PowerAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, PlayerModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        boolean hands = data.power() == Power.SCARLET_WITCH && data.flying();
        // (3.4) A custom hero with Vision's Phase glows as Vision does, in his own colour.
        boolean phased = data.has(HeroAbility.VISION_PHASE) && data.visionPhased();
        if (player.isInvisible()) {
            SHOWN.remove(player.getId());
            return;
        }
        float t = player.tickCount + partialTicks;
        // (3.3) The aura eases in and out over a few ticks instead of popping on and off.
        float[] shown = SHOWN.computeIfAbsent(player.getId(), id -> new float[]{0.0F, 0.0F, t});
        float dt = Mth.clamp(t - shown[2], 0.0F, 4.0F);
        shown[2] = t;
        shown[0] = AnimMath.ease(shown[0], hands ? 1.0F : 0.0F, dt, 1.5F);
        shown[1] = AnimMath.ease(shown[1], phased ? 1.0F : 0.0F, dt, 1.5F);
        float handsW = shown[0];
        float phasedW = shown[1];
        if (handsW < 0.01F && phasedW < 0.01F) {
            if (!hands && !phased) {
                SHOWN.remove(player.getId());
            }
            return;
        }
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        boolean body = phasedW >= 0.01F;
        model.setAllVisible(body);
        if (!body) {
            model.rightArm.visible = true;
            model.leftArm.visible = true;
            model.rightSleeve.visible = false;
            model.leftSleeve.visible = false;
        }
        int rgb = body ? (data.power() == Power.CUSTOM ? AbilityColors.color(player, HeroAbility.VISION_PHASE, Powers.color(player))
                : "white_vision".equals(data.variant()) ? 0xF4F8FF : 0xE3DC6E) : 0xE0193F;
        int color = Math.round(0x90 * (body ? phasedW : handsW)) << 24 | rgb;
        drawSwirl(poseStack, buffer, packedLight, model, SWIRL, t, 1.0F, color);

        if (handsW >= 0.01F) {
            VertexConsumer glow = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            // The orbs swell as she speeds up.
            HeroAnimState state = HeroAnimator.peek(player);
            float swell = (state == null ? 1F : 1F + 0.35F * AnimMath.ss(1F, 2F, state.s)) * handsW;
            handOrb(poseStack, glow, model.rightArm, -1F / 16F, t, swell);
            handOrb(poseStack, glow, model.leftArm, 1F / 16F, t + 20F, swell);
        }
    }

    /**
     * A swirl of energy over a (posed) model: {@code texture} scrolling at {@code speed} times the usual pace, tinted
     * {@code argb}. The Scarlet Witch's and Vision's, and (3.4) the auras of a custom hero's look.
     */
    public static void drawSwirl(PoseStack poseStack, MultiBufferSource buffer, int light, PlayerModel<AbstractClientPlayer> model,
                                 ResourceLocation texture, float t, float speed, int argb) {
        VertexConsumer swirl = buffer.getBuffer(RenderType.energySwirl(texture, (t * 0.02F * speed) % 1F, (t * 0.013F * speed) % 1F));
        model.renderToBuffer(poseStack, swirl, light, OverlayTexture.NO_OVERLAY, argb);
    }

    /** A spinning ball of chaos energy cupped in the hand. */
    private static void handOrb(PoseStack poseStack, VertexConsumer vc, ModelPart arm, float x, float t, float swell) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.translate(x, 10F / 16F, 0F);
        float pulse = (0.85F + 0.15F * (float) Math.sin(t * 0.5F)) * swell;
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 9F + i * 60F));
            poseStack.mulPose(Axis.XP.rotationDegrees(t * 6F + i * 45F));
            Matrix4f m = poseStack.last().pose();
            quad(vc, m, 0.28F * pulse, 0x80E0193F);
            quad(vc, m, 0.13F * pulse, 0xC0FF6F86);
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
