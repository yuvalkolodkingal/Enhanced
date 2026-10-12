package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

/**
 * Thanos's Double-Edged Sword (DESIGN section 15.2.7): two broad, flat blades back to back on a short grip, with a
 * small guard in the middle and a ridge down each blade. It is drawn in his hand by a layer and flying by an entity
 * renderer; the model is the same, centred on the grip, a blade pointing up and a blade down.
 */
public class ThanosSwordModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("thanos_sword"), "main");

    private final ModelPart root;

    public ThanosSwordModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("sword_grip", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("sword_guard", CubeListBuilder.create().texOffs(0, 24).addBox(-4.0F, -1.5F, -1.5F, 8.0F, 3.0F, 3.0F), PartPose.ZERO);
        root.addOrReplaceChild("sword_blade_a", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -26.0F, -0.5F, 4.0F, 22.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("sword_blade_b", CubeListBuilder.create().texOffs(11, 0).addBox(-2.0F, 4.0F, -0.5F, 4.0F, 22.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("sword_spine_a", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, -25.0F, -1.0F, 2.0F, 20.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("sword_spine_b", CubeListBuilder.create().texOffs(31, 0).addBox(-1.0F, 5.0F, -1.0F, 2.0F, 20.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
