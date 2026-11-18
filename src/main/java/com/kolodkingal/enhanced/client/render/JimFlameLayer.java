package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Jim Hammond's burning body (3.17): while he flies in flames ({@link Live#JIM_FLAMING}), and brighter while he gathers the
 * Nova Flame, every part of his body is wrapped in fire: head, torso, both arms, both legs.
 *
 * <p>How it is done, and why it cannot go wrong the way the first attempt did (white blobs, a sheet detached from the
 * tilted body, most of the body bare): the flames are the model itself, drawn again a little larger from the very pose
 * the body was just drawn in (so the tilt of flight, the swing of the arms and the head turned are the flames' too: nothing
 * is placed in the world), with a texture that paints flames on <em>every</em> face of every part (the whole 64 by 64 of the
 * skin's layout), drawn alpha-blended, full-bright, in four pictures that follow one another (so the tongues rise). It is
 * not additive: the colours are the texture's own oranges, reds and yellows however bright the sky or white the suit.
 */
public class JimFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType[] FRAMES = new RenderType[4];
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Ticks each picture of the flames lasts. */
    private static final float FRAME_TICKS = 1.6F;

    static {
        for (int i = 0; i < FRAMES.length; i++) {
            FRAMES[i] = RenderType.entityTranslucentEmissive(flame(i));
        }
    }

    public static ResourceLocation flame(int frame) {
        return Enhanced.id("textures/entity/jim/flame_" + frame + ".png");
    }

    private final PlayerModel<AbstractClientPlayer> model;

    public JimFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         PlayerModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    /** Whether this player's body burns now. */
    static boolean burning(PowerData data) {
        return data.is(Live.JIM_FLAMING) || data.is(Live.JIM_NOVA_CHARGING) || data.is(Live.JIM_NOVA_BLAST);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (!burning(data) || player.isInvisible()) {
            return;
        }
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.setAllVisible(true);
        // the second layers of the skin are not part of the flame
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
        float t = player.tickCount + partialTicks;
        int frame = Mth.floor(t / FRAME_TICKS) & 3;
        boolean nova = data.is(Live.JIM_NOVA_CHARGING) || data.is(Live.JIM_NOVA_BLAST);
        // the outer, larger sheet of fire, from another picture, a little fainter
        poseStack.pushPose();
        poseStack.scale(1.07F, 1.03F, 1.07F);
        model.renderToBuffer(poseStack, buffer.getBuffer(FRAMES[(frame + 2) & 3]), FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                nova ? 0xFFFFFFFF : 0xB8FFFFFF);
        poseStack.popPose();
        // the fire close to the body
        model.renderToBuffer(poseStack, buffer.getBuffer(FRAMES[frame]), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
