package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
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

/**
 * Ultimate Source Code Vision's burning body (3.17, Ultimate Mode only, while he flies or uses a power: {@link
 * VisionFlameFx#burning}): the Human Torch's look, on the model itself, every part of it (head, body, arms, legs).
 * <ul>
 *   <li>His skin is then the molten one ({@code client.SuitSkins}: a figure of deep glowing red).</li>
 *   <li>Over it, full-bright and added to it, the cracks of fire: bright yellow-white veins running over the whole body,
 *   alive (four pictures of them, each fading into the next).</li>
 *   <li>Round it, a sheath of flame hugging the silhouette: the model a little larger, a flame picture scrolling up it,
 *   and a fatter, paler one behind (the Phoenix's way, {@link PhoenixFlameLayer#drawFlames}, in his orange and gold).</li>
 * </ul>
 * The tongues streaming behind him, the trail and the embers are {@link VisionFlameFx}'s. Only light: nothing burns. With
 * Ultimate Mode off it draws nothing.
 */
public class VisionFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType CRACKS_0 = RenderType.eyes(Enhanced.id("textures/entity/suit/vision_molten_cracks_0.png"));
    private static final RenderType CRACKS_1 = RenderType.eyes(Enhanced.id("textures/entity/suit/vision_molten_cracks_1.png"));
    private static final RenderType CRACKS_2 = RenderType.eyes(Enhanced.id("textures/entity/suit/vision_molten_cracks_2.png"));
    private static final RenderType CRACKS_3 = RenderType.eyes(Enhanced.id("textures/entity/suit/vision_molten_cracks_3.png"));
    private static final RenderType[] CRACKS = {CRACKS_0, CRACKS_1, CRACKS_2, CRACKS_3};
    /**
     * The sheath's colours: the orange tongues, the gold behind them. Dim on purpose: the sheath is light added to the
     * body (its strength is in the colour itself), and the body under it must stay a figure of red, not bleach to white.
     */
    private static final int FLAME = 0xFF6A2A08;
    private static final int GLOW = 0xFF30200A;
    /** Ticks each picture of the cracks lasts before the next has taken its place. */
    private static final float FRAME_TICKS = 5.0F;

    private final PlayerModel<AbstractClientPlayer> model;

    public VisionFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, PlayerModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (!VisionFlameFx.burning(player, data) || player.isInvisible()) {
            return;
        }
        float t = player.tickCount + partialTicks;
        // the cracks of fire over the whole body: this picture fading out as the next fades in
        float at = t / FRAME_TICKS;
        int frame = Mth.floor(at) & 3;
        float blend = at - Mth.floor(at);
        float flicker = 0.88F + 0.12F * Mth.sin(t * 0.9F);
        int a = Math.round(255.0F * (1.0F - blend) * flicker);
        int b = Math.round(255.0F * blend * flicker);
        GlowingEyesLayer.drawEyes(poseStack, buffer, getParentModel(), CRACKS[frame], 0xFF000000 | a << 16 | a << 8 | a);
        GlowingEyesLayer.drawEyes(poseStack, buffer, getParentModel(), CRACKS[(frame + 1) & 3], 0xFF000000 | b << 16 | b << 8 | b);
        // the sheath of flame round the silhouette
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float pulse = 0.8F + 0.2F * Mth.sin(0.6F * t);
        PhoenixFlameLayer.drawFlames(poseStack, buffer, model, t * 1.6F, pulse, FLAME, GLOW, PhoenixFlameLayer.FLAME_GREY);
    }
}
