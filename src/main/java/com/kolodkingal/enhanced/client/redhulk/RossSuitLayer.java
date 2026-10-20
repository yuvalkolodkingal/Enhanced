package com.kolodkingal.enhanced.client.redhulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Ross's dark suit, white shirt and red tie over the player's own skin while Red Hulk is Ross (3.8, PLAN 4.5): General
 * (now President) Ross's dark navy suit, drawn on a copy of the player model a little bigger than his skin's outer
 * layer (so it is never lost in it), in the pose of the body under it: the jacket over the body and the arms (the
 * shirt's cuffs and his own hands at the ends), the trousers and black shoes on the legs; his own head. Built in the
 * client setup's red_hulk region with that bigger model ({@code ClientSetup.ENERGY_WIDE}/{@code ENERGY_SLIM}).
 */
public class RossSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation SUIT = Enhanced.id("textures/entity/red_hulk/ross_suit.png");

    /** The bigger copy of the player model the suit is drawn on (null: the parent's own outer layer). */
    @Nullable
    private final PlayerModel<AbstractClientPlayer> shell;

    public RossSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        this(parent, null);
    }

    public RossSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            @Nullable PlayerModel<AbstractClientPlayer> shell) {
        super(parent);
        this.shell = shell;
    }

    /** Whether the player is Red Hulk as Ross (the hero himself; a custom hero's Ross is his own skin). */
    public static boolean dressed(PowerData data) {
        return data.power() == Power.RED_HULK && data.toggle(Toggle.ROSS_FORM);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible() || !dressed(Powers.get(player))) {
            return;
        }
        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(SUIT));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        if (shell != null) {
            parent.copyPropertiesTo(shell);
            shell.body.render(poseStack, vc, packedLight, overlay);
            shell.rightArm.render(poseStack, vc, packedLight, overlay);
            shell.leftArm.render(poseStack, vc, packedLight, overlay);
            shell.rightLeg.render(poseStack, vc, packedLight, overlay);
            shell.leftLeg.render(poseStack, vc, packedLight, overlay);
            return;
        }
        parent.jacket.render(poseStack, vc, packedLight, overlay);
        parent.rightSleeve.render(poseStack, vc, packedLight, overlay);
        parent.leftSleeve.render(poseStack, vc, packedLight, overlay);
        parent.rightPants.render(poseStack, vc, packedLight, overlay);
        parent.leftPants.render(poseStack, vc, packedLight, overlay);
    }
}
