package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

/**
 * The Einherjar's helmet (3.5): a golden dome a little bigger than the head, a brow band, cheek guards down the sides of
 * the face, a neck guard, and a tall crest from brow to nape. Drawn in the head's space; its texture is
 * {@code textures/entity/asgardian/einherjar_helmet.png} (64x32, made by {@code SCRATCH/v35/art/l5/gen_asgardians.py}
 * with the same boxes).
 */
public class AsgardianHelmetModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("asgardian_helmet"), "main");
    private final ModelPart root;

    public AsgardianHelmetModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation d = new CubeDeformation(0.05F);
        root.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -8.6F, -4.5F, 9, 3, 9, d), PartPose.ZERO);
        root.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 12).addBox(-4.7F, -6.0F, -4.7F, 9, 1, 9, new CubeDeformation(0.2F)),
                PartPose.ZERO);
        root.addOrReplaceChild("cheek_right", CubeListBuilder.create().texOffs(36, 0).addBox(-4.9F, -5.0F, -4.4F, 1, 5, 4, d), PartPose.ZERO);
        root.addOrReplaceChild("cheek_left", CubeListBuilder.create().texOffs(36, 0).mirror().addBox(3.9F, -5.0F, -4.4F, 1, 5, 4, d),
                PartPose.ZERO);
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(36, 9).addBox(-4.5F, -5.0F, 3.6F, 9, 4, 1, d), PartPose.ZERO);
        root.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(36, 14).addBox(-0.5F, -11.5F, -5.0F, 1, 3, 10, d), PartPose.ZERO);
        root.addOrReplaceChild("crest_front", CubeListBuilder.create().texOffs(58, 14).addBox(-0.5F, -10.5F, -5.6F, 1, 3, 1, d),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
