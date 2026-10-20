package com.kolodkingal.enhanced.client.tower;

import com.kolodkingal.enhanced.block.SuitDisplayBlock;
import com.kolodkingal.enhanced.block.SuitDisplayBlockEntity;
import com.kolodkingal.enhanced.client.ironman.SuitModel;
import com.kolodkingal.enhanced.client.ironman.SuitModels;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The suit on a display until lane L7's display renderer takes its place (3.8, lane L6; registered first, so L7's
 * replaces it, {@link TowerClient}): the display's suit ({@code SuitModels.get}, the skeleton's boxes or L7's models)
 * on a man's frame standing on the plinth, facing as the display faces, its arms a little out; the Hulkbuster 1.75
 * times as large. Each suit part is drawn in the space of the player model's part it rides on, with its emissive pass.
 */
public class SuitDisplayStandIn implements BlockEntityRenderer<SuitDisplayBlockEntity> {
    /** The Hulkbuster's size on its display. */
    static final float HULKBUSTER_SCALE = 1.75F;
    /** The plinth's height (sixteenths): the suit's feet stand on it. */
    static final float PLINTH = 2.0F;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public SuitDisplayStandIn(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(ModelLayers.PLAYER);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightArm.zRot = 0.12F;
        leftArm.zRot = -0.12F;
    }

    private ModelPart bone(SuitParts part) {
        return switch (part) {
            case HELMET, FACEPLATE -> head;
            case RIGHT_UPPER_ARM, RIGHT_FOREARM, RIGHT_GAUNTLET -> rightArm;
            case LEFT_UPPER_ARM, LEFT_FOREARM, LEFT_GAUNTLET -> leftArm;
            case RIGHT_THIGH, RIGHT_SHIN, RIGHT_BOOT -> rightLeg;
            case LEFT_THIGH, LEFT_SHIN, LEFT_BOOT -> leftLeg;
            // the chest and back, and any part lane L7 adds (rides on the body)
            default -> body;
        };
    }

    @Override
    public void render(SuitDisplayBlockEntity display, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        SuitKind kind = display.kind();
        BlockState state = display.getBlockState();
        if (kind == null || !state.hasProperty(SuitDisplayBlock.FACING)) {
            return;
        }
        SuitModel model = SuitModels.get(kind);
        pose.pushPose();
        pose.translate(0.5, PLINTH / 16.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - state.getValue(SuitDisplayBlock.FACING).toYRot()));
        if (kind == SuitKind.HULKBUSTER) {
            pose.scale(HULKBUSTER_SCALE, HULKBUSTER_SCALE, HULKBUSTER_SCALE);
        }
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0, -1.501, 0.0);
        drawParts(model, model.texture(), false, pose, buffers, light);
        ResourceLocation glow = model.glowTexture();
        if (glow != null) {
            drawParts(model, glow, true, pose, buffers, LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    private void drawParts(SuitModel model, ResourceLocation texture, boolean glow, PoseStack pose,
                           MultiBufferSource buffers, int light) {
        var buffer = buffers.getBuffer(glow ? RenderType.eyes(texture) : RenderType.entityCutoutNoCull(texture));
        for (SuitParts part : SuitParts.VALUES) {
            pose.pushPose();
            bone(part).translateAndRotate(pose);
            model.renderPart(part, pose, buffer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(SuitDisplayBlockEntity display) {
        return new AABB(display.getBlockPos()).inflate(1.0).expandTowards(0.0, 3.0, 0.0);
    }
}
