package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.BerserkerEntity;
import net.minecraft.client.model.PlayerModel;
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
 * A berserker (3.5): a player-shaped body in a skin of its own (the left half of a 128 x 64 texture: grey dead skin, the
 * cracked dark-green armour of Asgard's old army; the right half holds the sword and the crest), with a crested helmet and
 * a notched black sword in the right hand, both cuboids of the model (no item is held). Its arms hang forward as the
 * undead's do when it walks; a blow is a hacking downswing; while it climbs out of the floor its arms claw upward
 * ({@link BerserkerEntity#risen}).
 */
public class BerserkerModel extends PlayerModel<BerserkerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("berserker"), "main");

    public BerserkerModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // The crest of the helmet: a ridge front to back, and two short cheek guards.
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(96, 0).addBox(-0.5F, -11.0F, -4.5F, 1.0F, 3.0F, 3.0F)
                .texOffs(96, 8).addBox(-0.5F, -10.0F, -1.5F, 1.0F, 2.0F, 5.0F), PartPose.ZERO);
        PartDefinition arm = root.getChild("right_arm");
        // The sword: down from the fist, blade forward (as a held sword is drawn), notched.
        arm.addOrReplaceChild("sword", CubeListBuilder.create().texOffs(64, 0).addBox(-0.5F, -1.0F, -14.0F, 1.0F, 1.5F, 12.0F)
                .texOffs(64, 16).addBox(-2.0F, -1.25F, -2.5F, 4.0F, 2.0F, 1.0F)
                .texOffs(64, 22).addBox(-0.5F, -0.75F, -2.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-1.0F, 10.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(BerserkerEntity berserker, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(berserker, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float partial = ageInTicks - berserker.tickCount;
        float risen = berserker.risen(berserker.level().getGameTime() + partial);
        boolean striking = attackTime > 0.0F;
        if (!striking) {
            // The undead reach: both arms forward, swaying with the walk.
            float sway = Mth.sin(ageInTicks * 0.09F) * 0.05F;
            rightArm.xRot = -1.25F + Mth.cos(limbSwing * 0.66F) * 0.25F * limbSwingAmount + sway;
            leftArm.xRot = -1.35F - Mth.cos(limbSwing * 0.66F) * 0.25F * limbSwingAmount - sway;
            rightArm.zRot = 0.05F;
            leftArm.zRot = -0.05F;
        } else {
            // A hacking downswing of the sword arm.
            float k = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -2.6F + 2.0F * attackTime;
            rightArm.yRot = -0.3F * k;
            leftArm.xRot = -0.9F;
            body.yRot = 0.25F * k;
        }
        if (risen < 1.0F) {
            float k = 1.0F - risen;
            rightArm.xRot = Mth.lerp(k, rightArm.xRot, -2.8F + Mth.sin(ageInTicks * 0.8F) * 0.3F);
            leftArm.xRot = Mth.lerp(k, leftArm.xRot, -2.8F + Mth.cos(ageInTicks * 0.8F) * 0.3F);
            head.xRot = Mth.lerp(k, head.xRot, -0.5F);
        }
        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
    }
}
