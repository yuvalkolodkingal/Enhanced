package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.odin.RavenEntity;
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
import net.minecraft.util.Mth;

/**
 * Huginn or Muninn (3.5): a raven of cuboids, our own model: a sleek body, a head with a heavy beak, a wedge of a tail and
 * two long wings that beat slowly and glide (the beat eased by the age with the partial tick, so it never jumps). Black
 * feathers with a blue-violet sheen; a glint of gold at the eye.
 */
public class RavenModel extends EntityModel<RavenEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("raven"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart tail;

    public RavenModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightWing = body.getChild("right_wing");
        this.leftWing = body.getChild("left_wing");
        this.tail = body.getChild("tail");
    }

    /** The model, in pixels: the body along z (its front at -z), the wings from its shoulders. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 9).addBox(-1.5F, -1.5F, -2.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(12, 9).addBox(-0.5F, -0.5F, -4.5F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, -1.0F, -3.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-7.0F, -0.5F, -2.0F, 7.0F, 1.0F, 5.0F), PartPose.offset(-1.5F, -1.0F, -1.0F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(0.0F, -0.5F, -2.0F, 7.0F, 1.0F, 5.0F), PartPose.offset(1.5F, -1.0F, -1.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(18, 0).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, 0.2F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(RavenEntity raven, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // A slow beat (about twice a second) that glides between beats: the wings stay up a while, then sweep down.
        float phase = (ageInTicks + raven.getId() * 7) * 0.32F;
        float beat = Mth.sin(phase);
        float glide = 0.5F + 0.5F * Mth.sin(phase * 0.25F);
        float flap = (0.15F + 0.75F * (1.0F - glide)) * beat;
        rightWing.zRot = 0.15F + flap;
        leftWing.zRot = -0.15F - flap;
        tail.xRot = 0.2F + 0.08F * Mth.sin(phase * 0.5F);
        head.yRot = 0.15F * Mth.sin(ageInTicks * 0.05F + raven.getId());
        head.xRot = 0.1F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
