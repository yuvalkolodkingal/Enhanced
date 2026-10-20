package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.FalconActing;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Falcon's Exo-Wings on the wearer's back (3.8, PLAN 8.2): the pack and its nozzles, and the two wings in the pose of
 * {@link FalconWingsState} (folded, spread and beating while he hovers, flat for the glide, swept back for the Dive and
 * the Boost Climb, wrapped round the front for the Wing Shield, flung out for the Wing Slash), the nozzles' flames
 * bright while he flies. Drawn for anyone wearing them (folded for anyone but Falcon) and for a custom hero with
 * Falcon's rows. In first person, the tips of the spread wings show at the edges of his view ({@link #onRenderHand}).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public class FalconWingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The model the first-person view draws with (any player renderer's: they are all the same). */
    private static FalconWingsModel firstPersonModel;

    private final FalconWingsModel model;

    public FalconWingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                            ModelPart wings) {
        super(parent);
        this.model = new FalconWingsModel(wings);
        firstPersonModel = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible() || !FalconWingsState.drawsWings(player)) {
            return;
        }
        float[] p = new float[FalconWingsState.SIZE];
        float flap = FalconWingsState.pose(player, partialTick, p);
        model.pose(p, flap, ageInTicks, flame(player, ageInTicks));
        ResourceLocation texture = texture(player);
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, -1);
        model.renderFlames(poseStack, buffer.getBuffer(RenderType.eyes(texture)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** The wings' texture: the Winter Soldier's darker EXO-7 for that look, the Endgame steel and red otherwise. */
    static ResourceLocation texture(Player player) {
        return FalconWingsState.winterSoldier(player) ? FalconWingsModel.WINTER_SOLDIER : FalconWingsModel.ENDGAME;
    }

    /** The nozzles' flames' length now (0: none): while he flies, glides, dives or climbs, flickering. */
    static float flame(Player player, float age) {
        PowerData data = Powers.get(player);
        if (!FalconWingsState.alive(player, data) || player.onGround()) {
            return 0.0F;
        }
        boolean roar = data.is(Live.FALCON_DIVING) || FalconActing.climb(player, 0.0F) > 0.0F;
        if (!roar && !data.flying() && !player.isFallFlying()) {
            return 0.0F;
        }
        float flicker = 0.85F + 0.18F * Mth.sin(age * 1.9F) + 0.1F * Mth.sin(age * 3.7F + 1.3F);
        return (roar ? 1.6F : player.isFallFlying() ? 0.7F : 1.0F) * flicker;
    }

    /**
     * In first person, the tips of his spread wings at the edges of his view while he flies or glides (drawn once a
     * frame, with the main hand).
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        FalconWingsModel model = firstPersonModel;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (model == null || player == null || event.getHand() != InteractionHand.MAIN_HAND || player.isInvisible()
                || !FalconWingsState.drawsWings(player)) {
            return;
        }
        float partialTick = event.getPartialTick();
        float[] p = new float[FalconWingsState.SIZE];
        float flap = FalconWingsState.pose(player, partialTick, p);
        if (p[FalconWingsState.EXTEND] < 0.3F || p[FalconWingsState.TOP] < -45.0F
                || Math.abs(p[FalconWingsState.SWEEP]) > 70.0F) {
            return; // (folded, swept back or wrapped: nothing at the edges of his view)
        }
        float age = player.tickCount + partialTick;
        model.pose(p, flap, age, 0.0F);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // From the camera to his back, then into the body's model space (x to his left, y down, z behind him).
        poseStack.translate(0.0F, -0.42F, -0.30F);
        poseStack.mulPose(Axis.XP.rotationDegrees(player.isFallFlying() ? -12.0F : 4.0F));
        poseStack.scale(-1.0F / 16.0F, -1.0F / 16.0F, 1.0F / 16.0F);
        var buffer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(texture(player)));
        int light = event.getPackedLight();
        model.renderTips(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, 0, 3);
        model.renderTips(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, 1, 3);
        poseStack.popPose();
    }
}
