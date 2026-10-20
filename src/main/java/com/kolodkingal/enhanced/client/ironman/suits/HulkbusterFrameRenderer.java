package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.kolodkingal.enhanced.ironman.hulkbuster.HulkbusterFrameEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;


/**
 * The Hulkbuster's frame that Veronica drops (3.8, PLAN 6.4, 7.2; Age of Ultron): the empty suit at human height,
 * falling with its thrusters burning under the boots and the back, then opening its front shells (the chest's two
 * doors, the forearms' and shins' guards, the faceplate), the arms out, for Iron Man to step in, and closing round him.
 * How open it is comes from {@link #OPENNESS} (the entity's synced stages). Its eyes stay dark until he is in.
 */
public class HulkbusterFrameRenderer extends EntityRenderer<HulkbusterFrameEntity> {
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/iron_man/hulkbuster.png");

    /** How open a frame is (0 shut, 1 open) at a partial tick. */
    @FunctionalInterface
    public interface Openness {
        float of(HulkbusterFrameEntity frame, float partialTick);
    }

    /**
     * How open a frame is: the entity's own {@link HulkbusterFrameEntity#openness} (the Hulkbuster lane's synced stages:
     * falls, opening, closing, assembling, lifting).
     */
    public static Openness OPENNESS = HulkbusterFrameRenderer::openness;
    /** Whether its thrusters burn (while it moves: falling from the sky or lifting away). */
    public static Openness THRUST = (frame, partialTick) -> Math.abs(frame.getY() - frame.yo) > 0.01
            || frame.tickCount < SuitTimelines.FRAME_FALL && !frame.onGround() ? 1.0F : 0.0F;

    private final HulkbusterModel<LivingEntity> model;

    public HulkbusterFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HulkbusterModel<>(context.bakeLayer(HulkbusterModel.LAYER));
        this.shadowRadius = 0.9F;
    }

    @Override
    public void render(HulkbusterFrameEntity frame, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        float open = OPENNESS.of(frame, partialTick);
        float time = frame.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.rest(open);
        SuitLook look = SuitLook.still(SuitKind.HULKBUSTER, time);
        look.frameOpen = open;
        look.lit = 0.0F;
        SuitRender.drawSuit(poseStack, buffers, light, OverlayTexture.NO_OVERLAY, model, SuitKind.HULKBUSTER, look);
        if (THRUST.of(frame, partialTick) > 0.0F) {
            for (float x : new float[] {-HulkbusterModel.LEG_X, HulkbusterModel.LEG_X}) {
                poseStack.pushPose();
                poseStack.translate(x / 16.0F, 24.0F / 16.0F, 0.0F);
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                EnergyShapes.flame(poseStack, buffers, 3.0F, 14.0F, time);
                poseStack.popPose();
            }
            for (float x : new float[] {-3.0F, 3.0F}) {
                poseStack.pushPose();
                poseStack.translate(x / 16.0F, 8.0F / 16.0F, 7.5F / 16.0F);
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                EnergyShapes.flame(poseStack, buffers, 1.6F, 9.0F, time + 3.0F);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        super.render(frame, entityYaw, partialTick, poseStack, buffers, light);
    }

    private static float openness(HulkbusterFrameEntity frame, float partialTick) {
        return frame.openness(partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(HulkbusterFrameEntity frame) {
        return TEXTURE;
    }
}
