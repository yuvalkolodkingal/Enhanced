package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.falcon.RedwingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Redwing (3.8), our own drawing of Falcon's drone: a sleek black body with a red eye at its nose, two swept red
 * wings with black tips, a tail fin and a small thruster; it banks and bobs as it hovers, and its wings tremble. {@code
 * textures/entity/falcon/redwing.png} by {@code SCRATCH/v38/art/l5/gen_falcon.py}; the eye and the thruster also glow
 * (the renderer's bright pass).
 */
public class RedwingModel extends EntityModel<RedwingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("redwing"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/falcon/redwing.png");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart glow;

    public RedwingModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.rightWing = body.getChild("right_wing");
        this.leftWing = body.getChild("left_wing");
        this.glow = body.getChild("glow");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // (Mob models stand on y 24: the drone's middle hovers 3 pixels over its feet.)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 7.0F)
                .texOffs(0, 9).addBox(-1.0F, -1.6F, -2.5F, 2.0F, 1.0F, 3.0F)
                .texOffs(20, 0).addBox(-0.3F, -2.6F, 1.5F, 0.6F, 2.0F, 2.0F)
                .texOffs(10, 9).addBox(-1.0F, -0.6F, 3.0F, 2.0F, 1.2F, 1.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-6.0F, -0.3F, -1.5F, 6.0F, 0.6F, 4.0F),
                PartPose.offsetAndRotation(-1.4F, 0.0F, 0.0F, 0.0F, -0.45F, 0.0F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().mirror()
                .texOffs(0, 14).addBox(0.0F, -0.3F, -1.5F, 6.0F, 0.6F, 4.0F),
                PartPose.offsetAndRotation(1.4F, 0.0F, 0.0F, 0.0F, 0.45F, 0.0F));
        // The eye at the nose and the thruster's mouth (drawn bright).
        body.addOrReplaceChild("glow", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-0.8F, -0.6F, -4.3F, 1.6F, 0.9F, 0.5F)
                .texOffs(6, 20).addBox(-0.7F, -0.4F, 3.9F, 1.4F, 0.8F, 0.4F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(RedwingEntity drone, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        body.y = 21.0F + 0.6F * Mth.sin(ageInTicks * 0.18F);
        body.zRot = 0.10F * Mth.sin(ageInTicks * 0.11F);
        body.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F + (drone.striking() ? 0.35F : 0.0F);
        float tremble = 0.04F * Mth.sin(ageInTicks * 2.3F);
        rightWing.zRot = tremble;
        leftWing.zRot = -tremble;
        float fold = drone.striking() ? 0.5F : 0.0F;
        rightWing.yRot = -0.45F - fold;
        leftWing.yRot = 0.45F + fold;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(poseStack, buffer, light, overlay, color);
    }

    /** The eye and the thruster, again, for the bright pass. */
    public void renderGlow(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        poseStack.pushPose();
        body.translateAndRotate(poseStack);
        glow.render(poseStack, buffer, light, overlay);
        poseStack.popPose();
    }
}
