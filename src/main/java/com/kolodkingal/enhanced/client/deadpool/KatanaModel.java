package com.kolodkingal.enhanced.client.deadpool;

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
import net.minecraft.resources.ResourceLocation;

/**
 * One of Deadpool's katanas (3.7): a straight silver blade with a bright edge and a little tip, a gold collar, a round
 * black guard, a red grip wrapped in black and a black pommel; and its black scabbard with a red stripe, drawn on his
 * back. Our own drawing ({@code textures/entity/deadpool/katana.png}, by {@code SCRATCH/v37/art/l3/gen_deadpool.py}).
 * The model's origin is the guard; the blade points up (along -y, as models are drawn) and the grip down.
 */
public class KatanaModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("deadpool_katana"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/deadpool/katana.png");

    private final ModelPart blade;
    private final ModelPart hilt;
    private final ModelPart sheath;

    public KatanaModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.blade = root.getChild("blade");
        this.hilt = root.getChild("hilt");
        this.sheath = root.getChild("sheath");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The blade (its edge forward, -z) and the tip.
        root.addOrReplaceChild("blade", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.3F, -15.0F, -0.7F, 0.6F, 14.0F, 1.4F)
                .texOffs(5, 0).addBox(-0.3F, -16.4F, -0.45F, 0.6F, 1.4F, 0.9F), PartPose.ZERO);
        // The collar, the guard, the wrapped grip and the pommel.
        root.addOrReplaceChild("hilt", CubeListBuilder.create()
                .texOffs(10, 0).addBox(-0.45F, -1.6F, -0.85F, 0.9F, 0.6F, 1.7F)
                .texOffs(10, 4).addBox(-1.2F, -1.0F, -1.6F, 2.4F, 0.5F, 3.2F)
                .texOffs(0, 17).addBox(-0.5F, -0.5F, -0.6F, 1.0F, 5.0F, 1.2F)
                .texOffs(6, 17).addBox(-0.6F, 4.5F, -0.7F, 1.2F, 0.8F, 1.4F), PartPose.ZERO);
        // The scabbard: from the guard up over the blade.
        root.addOrReplaceChild("sheath", CubeListBuilder.create()
                .texOffs(12, 10).addBox(-0.6F, -16.8F, -1.0F, 1.2F, 15.8F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        blade.render(poseStack, buffer, packedLight, packedOverlay, color);
        hilt.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** The sword drawn: blade and hilt. */
    public void renderSword(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        blade.render(poseStack, buffer, light, overlay);
        hilt.render(poseStack, buffer, light, overlay);
    }

    /** The scabbard, and the hilt in it when the sword is sheathed. */
    public void renderSheathed(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, boolean withSword) {
        sheath.render(poseStack, buffer, light, overlay);
        if (withSword) {
            hilt.render(poseStack, buffer, light, overlay);
        }
    }
}
