package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.hela.NecroswordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A necrosword in the air (3.5): the blade of {@link NecroswordModel}, point first along its flight, a little larger than
 * in a hand, its green edges full-bright and a short green streak behind it. A blade of the rain hangs point down over its
 * mark, trembling, until it drops.
 */
public class NecroswordRenderer extends EntityRenderer<NecroswordEntity> {
    private static final float SCALE = 1.25F;

    private final NecroswordModel model;

    public NecroswordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new NecroswordModel(context.bakeLayer(NecroswordModel.LAYER));
    }

    @Override
    public void render(NecroswordEntity blade, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = blade.tickCount + partialTicks;
        poseStack.pushPose();
        Vec3 motion = blade.getDeltaMovement();
        boolean hanging = blade.hang() > 0 || motion.lengthSqr() < 1.0E-6;
        float yaw;
        float pitch;
        if (hanging || blade.falling()) {
            // Point down; a slow turn and a tremble while it hangs.
            yaw = age * (hanging ? 6.0F : 0.0F);
            pitch = 180.0F;
            if (hanging) {
                poseStack.translate(Mth.sin(age * 1.7F) * 0.02F, Mth.sin(age * 0.4F) * 0.05F, Mth.cos(age * 1.3F) * 0.02F);
            }
        } else {
            yaw = (float) Math.toDegrees(Mth.atan2(motion.x, motion.z));
            pitch = (float) Math.toDegrees(Mth.atan2(motion.horizontalDistance(), motion.y));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        // (Models are drawn upside down in their own frame; the blade's point is at -y.)
        poseStack.scale(-SCALE, -SCALE, SCALE);
        poseStack.translate(0.0F, NecroswordModel.LENGTH * 0.5F / 16.0F, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(NecroswordModel.TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY, -1);
        model.renderEdges(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(NecroswordModel.TEXTURE)), OverlayTexture.NO_OVERLAY,
                EnergyGeometry.argb(HelaRules.PALE, 0.9F));
        poseStack.popPose();
        if (!hanging) {
            streak(poseStack, buffer, motion, blade.falling());
        }
        super.render(blade, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A thin green streak behind a flying blade. */
    private static void streak(PoseStack poseStack, MultiBufferSource buffer, Vec3 motion, boolean falling) {
        Vec3 back = motion.normalize().scale(-1.8);
        Vec3 side = Math.abs(motion.normalize().y) > 0.9 ? new Vec3(0.06, 0, 0) : motion.normalize().cross(new Vec3(0, 1, 0)).normalize().scale(0.06);
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        Matrix4f m = poseStack.last().pose();
        int head = EnergyGeometry.argb(HelaRules.GREEN, falling ? 0.45F : 0.6F);
        int tail = EnergyGeometry.argb(HelaRules.GREEN, 0.0F);
        vc.addVertex(m, (float) side.x, (float) side.y, (float) side.z).setColor(head);
        vc.addVertex(m, (float) -side.x, (float) -side.y, (float) -side.z).setColor(head);
        vc.addVertex(m, (float) (back.x - side.x), (float) (back.y - side.y), (float) (back.z - side.z)).setColor(tail);
        vc.addVertex(m, (float) (back.x + side.x), (float) (back.y + side.y), (float) (back.z + side.z)).setColor(tail);
    }

    @Override
    public ResourceLocation getTextureLocation(NecroswordEntity entity) {
        return NecroswordModel.TEXTURE;
    }
}
