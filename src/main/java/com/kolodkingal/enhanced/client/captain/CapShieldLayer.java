package com.kolodkingal.enhanced.client.captain;

import com.kolodkingal.enhanced.captain.CaptainState;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;

/**
 * The Vibranium Shield on Captain America's back (3.8, PLAN 3.3) when he carries it but does not hold it (and none is in
 * flight): the synced {@link CaptainState#BACK} flag says so for everyone who sees him (the client knows only its own
 * player's inventory). Its face out, between his shoulder blades; an elytra or a cape hides it.
 */
public class CapShieldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Its size on the back, and how high on it its centre sits (pixels below the neck). */
    static final float SCALE = 0.9F;
    static final float HEIGHT = 6.0F;
    /** How far behind the back the disc's rim stands (pixels): clear of the body. */
    static final float CLEARANCE = 0.95F;

    public CapShieldLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Whether the player's shield is drawn on his back. */
    public static boolean onBack(AbstractClientPlayer player) {
        if (player.isInvisible() || !player.getData(ModAttachments.CAPTAIN).has(CaptainState.BACK)) {
            return false;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return false;
        }
        return !(player.isModelPartShown(PlayerModelPart.CAPE) && player.getSkin().capeTexture() != null);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!onBack(player)) {
            return;
        }
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, HEIGHT / 16.0F, (2.0F + CLEARANCE) / 16.0F);
        // Its face out behind him (the model's face is -z), the star upright.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        ShieldItemRenderer.get().model().renderOnBack(poseStack, buffer.getBuffer(ShieldModel.renderType()),
                packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
