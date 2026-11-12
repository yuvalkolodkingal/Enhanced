package com.kolodkingal.enhanced.client.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.nano.NanoInfectedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Those the nanotech has taken (3.12; their kinds 3.12.2): a villager, an iron golem or a zombie, drawn with the game's
 * own model and look for each, and the nanotech's orange and blue threads glowing over them. (3.12's grey people are
 * drawn as they were.)
 */
public class NanoInfectedRenderer extends MobRenderer<NanoInfectedEntity, PlayerModel<NanoInfectedEntity>> {
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/mind/nano_infected.png");
    private static final ResourceLocation VILLAGER = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    private static final ResourceLocation GOLEM = ResourceLocation.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");
    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final ResourceLocation THREADS = Enhanced.id("textures/entity/mind/nano_overlay.png");
    private static final ResourceLocation THREADS_GOLEM = Enhanced.id("textures/entity/mind/nano_overlay_golem.png");

    private final ModelPart villager;
    private final ModelPart golem;
    private final ModelPart zombie;

    public NanoInfectedRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.villager = context.bakeLayer(ModelLayers.VILLAGER);
        this.golem = context.bakeLayer(ModelLayers.IRON_GOLEM);
        this.zombie = context.bakeLayer(ModelLayers.ZOMBIE);
    }

    @Override
    public void render(NanoInfectedEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        NanoInfectedEntity.Kind kind = entity.kind();
        if (kind == NanoInfectedEntity.Kind.PERSON) {
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        ModelPart root = kind == NanoInfectedEntity.Kind.GOLEM ? golem : kind == NanoInfectedEntity.Kind.ZOMBIE ? zombie : villager;
        ResourceLocation texture = kind == NanoInfectedEntity.Kind.GOLEM ? GOLEM : kind == NanoInfectedEntity.Kind.ZOMBIE ? ZOMBIE : VILLAGER;
        ResourceLocation threads = kind == NanoInfectedEntity.Kind.GOLEM ? THREADS_GOLEM : THREADS;
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot) - bodyYaw;
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float walk = entity.walkAnimation.position(partialTicks);
        float amount = Math.min(1.0F, entity.walkAnimation.speed(partialTicks));
        pose(root, kind, walk, amount, headYaw, pitch, entity.tickCount + partialTicks, entity.isAggressive());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, overlay);
        root.render(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(threads)), LightTexture.FULL_BRIGHT, overlay);
        poseStack.popPose();
    }

    private static ModelPart part(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    /** A plain walk for each kind: the legs swing, a zombie reaches out, a golem swings its long arms. */
    private static void pose(ModelPart root, NanoInfectedEntity.Kind kind, float walk, float amount, float headYaw, float pitch,
                             float time, boolean aggressive) {
        float swing = Mth.cos(walk * 0.6662F) * 1.4F * amount;
        ModelPart head = part(root, "head");
        if (head != null) {
            head.yRot = headYaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
        }
        ModelPart rightLeg = part(root, "right_leg");
        ModelPart leftLeg = part(root, "left_leg");
        if (rightLeg != null && leftLeg != null) {
            rightLeg.xRot = swing;
            leftLeg.xRot = -swing;
        }
        ModelPart rightArm = part(root, "right_arm");
        ModelPart leftArm = part(root, "left_arm");
        if (rightArm != null && leftArm != null) {
            if (kind == NanoInfectedEntity.Kind.ZOMBIE) {
                float reach = -Mth.HALF_PI + 0.08F * Mth.sin(time * 0.067F);
                rightArm.xRot = reach;
                leftArm.xRot = reach;
            } else {
                rightArm.xRot = -swing * 0.6F - (aggressive ? 0.4F : 0.0F);
                leftArm.xRot = swing * 0.6F;
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NanoInfectedEntity entity) {
        return TEXTURE;
    }
}
