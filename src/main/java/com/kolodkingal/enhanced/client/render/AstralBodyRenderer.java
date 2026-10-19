package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.strange.AstralBodyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Doctor Strange's body while his spirit is out (3.6): his own model and look (his skin as he wears it, wide or slim),
 * kneeling on both knees with the head bowed and the hands on the thighs, the eyes closed (lids from his forehead's
 * skin over the eyes), and the Cloak of Levitation and the Eye still on him: they stay with the body.
 */
public class AstralBodyRenderer extends LivingEntityRenderer<AstralBodyEntity, PlayerModel<AstralBodyEntity>> {
    private final PlayerModel<AstralBodyEntity> wide;
    private final PlayerModel<AstralBodyEntity> slim;

    public AstralBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new Kneeling(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = model;
        this.slim = new Kneeling(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new Lids(this));
        addLayer(new Gear(this));
    }

    @Nullable
    static AbstractClientPlayer owner(AstralBodyEntity body) {
        if (body.owner() == null) {
            return null;
        }
        Player owner = body.level().getPlayerByUUID(body.owner());
        return owner instanceof AbstractClientPlayer client ? client : null;
    }

    private static PlayerSkin skin(AstralBodyEntity body) {
        AbstractClientPlayer owner = owner(body);
        return owner != null ? owner.getSkin() : DefaultPlayerSkin.get(body.getUUID());
    }

    @Override
    public void render(AstralBodyEntity body, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        model = skin(body).model() == PlayerSkin.Model.SLIM ? slim : wide;
        super.render(body, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(AstralBodyEntity body) {
        return skin(body).texture();
    }

    @Override
    protected void scale(AstralBodyEntity body, PoseStack poseStack, float partialTick) {
        // Kneeling: the body lowered so the knees are on the ground (the legs lie back along it).
        poseStack.translate(0.0F, 10.0F / 16.0F, 0.0F);
    }

    @Override
    protected boolean shouldShowName(AstralBodyEntity body) {
        return false;
    }

    /** The kneeling pose: the legs laid back, the head bowed, the hands resting on the thighs. */
    static final class Kneeling extends PlayerModel<AstralBodyEntity> {
        Kneeling(net.minecraft.client.model.geom.ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(AstralBodyEntity body, float limbSwing, float limbSwingAmount, float age, float headYaw,
                              float headPitch) {
            super.setupAnim(body, 0.0F, 0.0F, age, 0.0F, 0.0F);
            float breath = (float) Math.sin(age * 0.05F) * 0.02F;
            head.xRot = 0.38F + breath;
            hat.copyFrom(head);
            rightLeg.xRot = 1.5F;
            leftLeg.xRot = 1.5F;
            rightLeg.yRot = 0.08F;
            leftLeg.yRot = -0.08F;
            rightArm.xRot = -0.42F;
            leftArm.xRot = -0.42F;
            rightArm.zRot = 0.12F;
            leftArm.zRot = -0.12F;
            rightArm.yRot = -0.15F;
            leftArm.yRot = 0.15F;
            body.yBodyRotO = body.yBodyRot;
            rightPants.copyFrom(rightLeg);
            leftPants.copyFrom(leftLeg);
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            jacket.copyFrom(this.body);
        }
    }

    /** The closed eyes: the forehead's skin drawn over the eyes (the eyes of a face are its fifth row, both sides). */
    static final class Lids extends RenderLayer<AstralBodyEntity, PlayerModel<AstralBodyEntity>> {
        Lids(AstralBodyRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AstralBodyEntity body,
                           float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            poseStack.pushPose();
            getParentModel().head.translateAndRotate(poseStack);
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            PoseStack.Pose pose = poseStack.last();
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(body)));
            int overlay = LivingEntityRenderer.getOverlayCoords(body, 0.0F);
            // The face is x -4..4, y -8..0 (the eyes at y -4..-3); its texture is u 8-16, v 8-16 (the forehead v 10).
            for (int side = -1; side <= 1; side += 2) {
                float x0 = side < 0 ? -3.0F : 1.0F;
                float x1 = x0 + 2.0F;
                float u0 = (side < 0 ? 13.0F : 9.0F) / 64.0F;
                float u1 = u0 + 2.0F / 64.0F;
                float v0 = 10.0F / 64.0F;
                float v1 = 11.0F / 64.0F;
                vertex(vc, pose, x0, -4.0F, -4.02F, u1, v0, light, overlay);
                vertex(vc, pose, x1, -4.0F, -4.02F, u0, v0, light, overlay);
                vertex(vc, pose, x1, -3.0F, -4.02F, u0, v1, light, overlay);
                vertex(vc, pose, x0, -3.0F, -4.02F, u1, v1, light, overlay);
            }
            poseStack.popPose();
        }

        private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                                   int light, int overlay) {
            vc.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(overlay).setLight(light)
                    .setNormal(pose, 0.0F, 0.0F, -1.0F);
        }
    }

    /** The Cloak of Levitation and the Eye, which stay on the body. */
    static final class Gear extends RenderLayer<AstralBodyEntity, PlayerModel<AstralBodyEntity>> {
        Gear(AstralBodyRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AstralBodyEntity body,
                           float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            AbstractClientPlayer owner = owner(body);
            if (owner == null) {
                return;
            }
            PowerData data = Powers.get(owner);
            int overlay = LivingEntityRenderer.getOverlayCoords(body, 0.0F);
            String look = data.power() == Power.DOCTOR_STRANGE ? Variants.current(data).id() : "sorcerer_supreme";
            if (data.power() == Power.DOCTOR_STRANGE) {
                CloakLayer.draw(poseStack, buffers, light, overlay, CloakLayer.texture(look),
                        getParentModel().body, new CloakLayer.Sway(30.0F, 0.0F, 0.0F, ageInTicks,
                                com.kolodkingal.enhanced.client.EnhancedClientConfig.reducedMotion()));
                EyeAmuletLayer.draw(poseStack, buffers, light, overlay, getParentModel().body, false,
                        "multiverse".equals(look), ageInTicks);
            }
        }
    }
}
