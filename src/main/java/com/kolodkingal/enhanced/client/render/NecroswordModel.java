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
import net.minecraft.resources.ResourceLocation;

/**
 * A necrosword (3.5): Hela's blade of black obsidian with a green edge, long and narrow, tapering to a point in three
 * steps, with a jagged guard of three teeth on each side and a short dark grip. The model's origin is the grip; the
 * blade points up (along -y, as models are drawn). One model for every blade of hers: in flight ({@link
 * NecroswordRenderer}), in her hands ({@code HelaBladesLayer}, the boss's renderer) and, scaled up, the spikes of her walls
 * ({@link BladeSpikeRenderer}).
 */
public class NecroswordModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("necrosword"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/necrosword.png");
    /** Where the point is, in pixels above the origin (what a spike's height is measured by). */
    public static final float LENGTH = 22.0F;

    private final ModelPart root;

    public NecroswordModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The grip and its pommel, under the guard.
        root.addOrReplaceChild("grip", CubeListBuilder.create().texOffs(0, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                .texOffs(4, 24).addBox(-1.0F, 4.0F, -0.75F, 2.0F, 1.0F, 1.5F), PartPose.ZERO);
        // The guard: a bar and three teeth each side, swept up toward the blade.
        root.addOrReplaceChild("guard", CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -1.0F, -0.75F, 6.0F, 1.0F, 1.5F)
                .texOffs(14, 20).addBox(-3.5F, -2.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(18, 20).addBox(2.5F, -2.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(22, 20).addBox(-2.25F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(26, 20).addBox(1.25F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        // The blade: wide at the guard, narrowing in three steps to the point.
        root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0).addBox(-1.25F, -10.0F, -0.25F, 2.5F, 9.0F, 0.5F)
                .texOffs(6, 0).addBox(-0.9F, -17.0F, -0.25F, 1.8F, 7.0F, 0.5F)
                .texOffs(11, 0).addBox(-0.5F, -21.0F, -0.25F, 1.0F, 4.0F, 0.5F)
                .texOffs(14, 0).addBox(-0.2F, -22.0F, -0.2F, 0.4F, 1.0F, 0.4F), PartPose.ZERO);
        // The green edge: a thin line down each side (its own pixels of the texture, bright).
        root.addOrReplaceChild("edge", CubeListBuilder.create().texOffs(20, 0).addBox(-1.4F, -10.0F, -0.1F, 0.3F, 9.0F, 0.2F)
                .texOffs(22, 0).addBox(1.1F, -10.0F, -0.1F, 0.3F, 9.0F, 0.2F)
                .texOffs(24, 0).addBox(-1.0F, -17.0F, -0.1F, 0.25F, 7.0F, 0.2F)
                .texOffs(26, 0).addBox(0.75F, -17.0F, -0.1F, 0.25F, 7.0F, 0.2F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** Only the green edges (drawn again full-bright, so they glow in the dark of the throne room). */
    public void renderEdges(PoseStack poseStack, VertexConsumer buffer, int packedOverlay, int color) {
        root.getChild("edge").render(poseStack, buffer, 0xF000F0, packedOverlay, color);
    }
}
