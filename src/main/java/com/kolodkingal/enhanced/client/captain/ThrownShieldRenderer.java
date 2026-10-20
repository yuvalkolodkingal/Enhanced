package com.kolodkingal.enhanced.client.captain;

import com.kolodkingal.enhanced.captain.ThrownShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The thrown shield in flight (3.8, PLAN 3.4): the disc face up and banked like a thrown disc,
 * spinning fast round its own axis (a turn every {@link #TURN_TICKS} ticks, interpolated with the frame's partial
 * tick), as the MCU's shield flies. A plain shield a custom hero threw is drawn as the item it is.
 */
public class ThrownShieldRenderer extends EntityRenderer<ThrownShieldEntity> {
    /** Ticks for a whole turn of the spin. */
    static final float TURN_TICKS = 6.0F;
    /** How far from flat it is banked (degrees): seen from behind it is a bright oval, not a line. */
    static final float BANK = 50.0F;
    /** Its size in flight (a little larger than in the hand: it is far away at once). */
    static final float SCALE = 1.15F;

    public ThrownShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.3F;
    }

    @Override
    public void render(ThrownShieldEntity shield, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        Vec3 v = shield.getDeltaMovement();
        float heading = v.horizontalDistanceSqr() < 1.0E-6 ? 0.0F
                : (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
        poseStack.mulPose(Axis.YP.rotationDegrees(heading));
        // Face up (the face is -z in the model), banked like a thrown disc; then the spin round its axis.
        poseStack.mulPose(Axis.ZP.rotationDegrees(BANK));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        float spin = (shield.tickCount + partialTick) * 360.0F / TURN_TICKS;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        if (com.kolodkingal.enhanced.captain.ShieldRules.isVibranium(shield.shield())) {
            ShieldModel model = ShieldItemRenderer.get().model();
            model.renderAll(poseStack, buffer.getBuffer(ShieldModel.renderType()), light, OverlayTexture.NO_OVERLAY);
        } else {
            poseStack.scale(1.0F, 1.0F, 1.0F);
            Minecraft.getInstance().getItemRenderer().renderStatic(shield.shield(),
                    net.minecraft.world.item.ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack,
                    buffer, shield.level(), shield.getId());
        }
        poseStack.popPose();
        super.render(shield, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownShieldEntity shield) {
        return ShieldModel.TEXTURE;
    }
}
