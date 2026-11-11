package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Vision's VisionQuest look (3.12.1): his yellow eyes glow while one of his powers is in use (the Mind Stone beam,
 * Phase, Density), full-bright so they show in the dark, easing in and out over a few ticks. At rest they are the plain
 * yellow pixels of the skin. The glow is {@code suit/vision_visionquest_eyes.png}, the four eye pixels on the face's
 * fifth row.
 */
public class VisionQuestEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final String LOOK = com.kolodkingal.enhanced.power.VisionQuest.LOOK;
    private static final RenderType EYES = RenderType.eyes(Enhanced.id("textures/entity/suit/vision_visionquest_eyes.png"));
    /** Per player: how strong the glow is (0 to 1) and when it was last drawn. */
    private static final Map<Integer, float[]> SHOWN = new HashMap<>();

    public VisionQuestEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (data.power() != Power.VISION || !LOOK.equals(data.variant()) || player.isInvisible()) {
            SHOWN.remove(player.getId());
            return;
        }
        boolean on = com.kolodkingal.enhanced.power.VisionQuest.eyesLit(data);
        float t = player.tickCount + partialTicks;
        float[] shown = SHOWN.computeIfAbsent(player.getId(), id -> new float[]{0.0F, t});
        float dt = Mth.clamp(t - shown[1], 0.0F, 4.0F);
        shown[1] = t;
        // (about three ticks to light up, a little longer to die down)
        shown[0] = AnimMath.ease(shown[0], on ? 1.0F : 0.0F, dt, 1.2F, 2.0F);
        float glow = shown[0];
        if (glow < 0.01F) {
            return;
        }
        // The eyes render type adds its colour to what is below, so the fade dims the colour itself.
        int g = Math.round(255.0F * glow);
        GlowingEyesLayer.drawEyes(poseStack, buffer, getParentModel(), EYES, 0xFF000000 | g << 16 | g << 8 | g);
    }
}
