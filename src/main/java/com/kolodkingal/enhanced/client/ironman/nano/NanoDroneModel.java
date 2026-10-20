package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * A Mark 85 nanotech drone (3.8, lane L4; PLAN 7.1: "small red-gold, blue eye"): a rounded red body, a gold crown and
 * gold side fins, a short gun under its nose and the blue eye in front. Each part is drawn in its own colour on a white
 * texture ({@link NanoDroneRenderer}), the eye in the emissive pass. Its own layer definition ({@code ClientSetup},
 * region nanotech).
 */
public final class NanoDroneModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("nano_drone"), "main");

    final ModelPart body;
    final ModelPart crown;
    final ModelPart fins;
    final ModelPart gun;
    final ModelPart eye;

    public NanoDroneModel(ModelPart root) {
        body = root.getChild("body");
        crown = root.getChild("crown");
        fins = root.getChild("fins");
        gun = root.getChild("gun");
        eye = root.getChild("eye");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .addBox(-3.0F, -2.0F, -3.0F, 6, 4, 6)
                .addBox(-2.0F, 2.0F, -2.0F, 4, 1, 4), PartPose.ZERO);
        root.addOrReplaceChild("crown", CubeListBuilder.create()
                .addBox(-2.0F, -3.0F, -2.0F, 4, 1, 4)
                .addBox(-0.5F, -4.0F, -0.5F, 1, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("fins", CubeListBuilder.create()
                .addBox(-5.5F, -1.0F, -1.5F, 2.5F, 1, 3)
                .addBox(3.0F, -1.0F, -1.5F, 2.5F, 1, 3), PartPose.ZERO);
        root.addOrReplaceChild("gun", CubeListBuilder.create()
                .addBox(-0.5F, 1.0F, -5.0F, 1, 1, 3), PartPose.ZERO);
        root.addOrReplaceChild("eye", CubeListBuilder.create()
                .addBox(-1.5F, -1.0F, -3.4F, 3, 2, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** Draws the solid parts in their colours. */
    void renderSolid(PoseStack pose, VertexConsumer vc, int light, int overlay, int red, int gold, int dark) {
        body.render(pose, vc, light, overlay, red);
        crown.render(pose, vc, light, overlay, gold);
        fins.render(pose, vc, light, overlay, gold);
        gun.render(pose, vc, light, overlay, dark);
    }
}
