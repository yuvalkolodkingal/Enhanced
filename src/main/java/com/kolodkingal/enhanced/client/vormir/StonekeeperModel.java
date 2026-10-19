package com.kolodkingal.enhanced.client.vormir;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.vormir.StonekeeperEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Stonekeeper (3.6, lane L5; our own design): a tall figure in a hooded robe with no feet, hovering. A gaunt
 * crimson face deep in a hood open in front (its front face is a frame of the texture: the face shows through), a
 * ragged mantle over the shoulders, a robe that widens to a frayed hem with four loose strips, and wide sleeves held
 * forward and in, the bony hands nearly meeting at the waist. The 64 x 64 skin is drawn by {@code
 * SCRATCH/v36s/art/l5/stonekeeper_skin.py} (every box's cube unwrap). Moving: the head turns to the one he watches, the
 * sleeves breathe and the hem's strips sway (the hover's bob is the renderer's).
 */
public class StonekeeperModel extends HierarchicalModel<StonekeeperEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("stonekeeper"), "main");

    private static final float ARM_FORWARD = -0.72F;
    private static final float ARM_IN = 0.42F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart[] tatters;

    public StonekeeperModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        ModelPart robe = body.getChild("robe");
        this.tatters = new ModelPart[] {robe.getChild("tatter_0"), robe.getChild("tatter_1"), robe.getChild("tatter_2"),
                robe.getChild("tatter_3")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The head with its hood (a frame in front), and the hood's peak falling back.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F)
                .texOffs(28, 0).addBox(-4.5F, -8.5F, -4.8F, 9.0F, 9.0F, 9.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));
        head.addOrReplaceChild("peak", CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 2.0F,
                4.0F), PartPose.offsetAndRotation(0.0F, -8.5F, 2.5F, 0.35F, 0.0F, 0.0F));
        // The body: the chest, the mantle over the shoulders.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 11.0F, 5.0F)
                .texOffs(26, 20).addBox(-5.0F, -0.5F, -3.0F, 10.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));
        // The robe below the waist, widening to the hem, and the hem's four loose strips.
        PartDefinition robe = body.addOrReplaceChild("robe", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-4.5F, 0.0F, -3.0F, 9.0F, 7.0F, 6.0F)
                .texOffs(30, 36).addBox(-5.0F, 7.0F, -3.5F, 10.0F, 8.0F, 7.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        float[][] strips = {{-3.0F, -3.0F}, {2.0F, -3.0F}, {-1.0F, 2.5F}, {3.5F, 1.0F}};
        for (int i = 0; i < strips.length; i++) {
            robe.addOrReplaceChild("tatter_" + i, CubeListBuilder.create().texOffs(40, 56).addBox(-1.0F, 0.0F, -0.5F,
                    2.0F, 5.0F, 1.0F), PartPose.offset(strips[i][0], 15.0F, strips[i][1]));
        }
        // The wide sleeves (cuffed) and the bony hands, held forward and in.
        for (int side = 0; side < 2; side++) {
            float x = side == 0 ? -5.0F : 5.0F;
            body.addOrReplaceChild(side == 0 ? "right_arm" : "left_arm", CubeListBuilder.create()
                    .texOffs(0, 49).mirror(side == 1).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 10.0F, 5.0F)
                    .texOffs(18, 51).mirror(side == 1).addBox(-2.5F, 6.0F, -3.0F, 5.0F, 4.0F, 6.0F,
                            new CubeDeformation(0.1F))
                    .texOffs(40, 51).mirror(side == 1).addBox(-1.0F, 9.5F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offset(x, 1.5F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(StonekeeperEntity keeper, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float breath = Mth.sin(ageInTicks * 0.06F);
        rightArm.xRot = ARM_FORWARD + breath * 0.03F;
        leftArm.xRot = ARM_FORWARD + breath * 0.03F;
        rightArm.yRot = -ARM_IN;
        leftArm.yRot = ARM_IN;
        rightArm.zRot = 0.08F;
        leftArm.zRot = -0.08F;
        body.xRot = 0.04F + breath * 0.01F;
        for (int i = 0; i < tatters.length; i++) {
            float phase = ageInTicks * 0.11F + i * 1.7F;
            tatters[i].xRot = 0.12F + Mth.sin(phase) * 0.16F;
            tatters[i].zRot = Mth.cos(phase * 0.8F) * 0.1F;
        }
    }
}
