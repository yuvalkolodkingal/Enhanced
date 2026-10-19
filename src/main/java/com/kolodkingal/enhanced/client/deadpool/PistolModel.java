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
 * One of Deadpool's pistols (3.7): a chunky silver cartoon pistol (PLAN 5.1: no real-gun realism), a big square slide
 * with a red stripe, a black grip and trigger guard, a little front sight; and its black holster, drawn at his hip. Our
 * own drawing ({@code textures/entity/deadpool/pistol.png}). The model's origin is the top of the grip, where the hand
 * holds it; the barrel points forward (along -z) and the grip down (+y).
 */
public class PistolModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("deadpool_pistol"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/deadpool/pistol.png");

    private final ModelPart gun;
    private final ModelPart holster;

    public PistolModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.gun = root.getChild("gun");
        this.holster = root.getChild("holster");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition gun = root.addOrReplaceChild("gun", CubeListBuilder.create()
                // The slide, the frame under it and the front sight.
                .texOffs(0, 0).addBox(-0.75F, -2.2F, -6.0F, 1.5F, 1.6F, 7.0F)
                .texOffs(0, 10).addBox(-0.7F, -0.7F, -5.5F, 1.4F, 0.8F, 5.8F)
                .texOffs(25, 0).addBox(-0.2F, -2.6F, -5.6F, 0.4F, 0.4F, 0.4F)
                // The trigger guard.
                .texOffs(18, 8).addBox(-0.3F, 0.0F, -1.8F, 0.6F, 1.0F, 1.2F), PartPose.ZERO);
        // The grip, raked back a little.
        gun.addOrReplaceChild("grip", CubeListBuilder.create()
                .texOffs(18, 0).addBox(-0.65F, 0.0F, -0.6F, 1.3F, 3.6F, 1.6F), PartPose.rotation(0.28F, 0.0F, 0.0F));
        // The holster round the barrel (the pistol in it points down when it hangs at his hip).
        root.addOrReplaceChild("holster", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-1.0F, -2.5F, -6.4F, 2.0F, 3.0F, 5.6F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        gun.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** The pistol alone (in his hand, or in the holster). */
    public void renderGun(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        gun.render(poseStack, buffer, light, overlay);
    }

    /** The holster alone. */
    public void renderHolster(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        holster.render(poseStack, buffer, light, overlay);
    }
}
