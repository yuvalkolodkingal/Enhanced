package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * The Hulk's own body (3.6), drawn instead of the player's while a Hulk look is on ({@link HulkPlayerRenderer}). It is
 * a {@link PlayerModel} with the player model's parts (so every pose of the mod's animation, {@code HeroPoses}, and
 * every layer that asks for an arm or the head works on it), built in the Hulk's proportions: a broad hunched torso (14
 * x 13 x 8, the chest wider than the waist, a hump of muscle behind the neck), a head set low and forward (8 x 8 x 8, a
 * heavy brow), huge arms (6 x 14 x 6, the forearm wider than the upper arm) and thick short legs (5 x 11 x 6). The
 * second layer holds the clothes: the Ragnarok gladiator's belt, strip kilt, bracers and the pauldron on his left
 * shoulder; the Brand New Day Hulk's torn trousers. Skins are 128 x 128 ({@code SCRATCH/v36s/art/l7/gen_hulk.py} draws
 * them on the same atlas as the boxes below). At scale 1 he is 30 pixels tall: the attribute scale makes him 2.7
 * (Ragnarok) or 3.15 (Brand New Day) blocks.
 */
public class HulkModel extends PlayerModel<AbstractClientPlayer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("hulk"), "main");
    /** The same body a little bigger: the aura and the glow drawn over him. */
    public static final ModelLayerLocation ENERGY = new ModelLayerLocation(Enhanced.id("hulk"), "energy");

    /** The shoulders stand this far from the middle (the player's are 5 pixels). */
    public static final float SHOULDER_X = 8.5F;
    /** The head sits this much lower and further forward than the player's; the legs' tops this much lower. */
    public static final float HEAD_DY = 1.5F;
    public static final float HEAD_DZ = -2.0F;
    public static final float LEG_DY = 1.0F;
    public static final float HIP_X = 3.5F;

    public HulkModel(ModelPart root) {
        super(root, false);
    }

    /** The boxes on their 128 x 128 atlas; {@code grow} inflates every box (the energy layer). */
    public static LayerDefinition createLayer(float grow) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation g = new CubeDeformation(grow);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, g)
                // The heavy brow ridge over the eyes.
                .texOffs(60, 66).addBox(-4.5F, -6.5F, -4.6F, 9, 1, 1, g), PartPose.offset(0.0F, HEAD_DY, HEAD_DZ));
        root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, g.extend(0.5F)), PartPose.offset(0.0F, HEAD_DY,
                        HEAD_DZ));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-7.0F, 0.0F, -4.5F, 14, 8, 9, g)
                .texOffs(0, 16).addBox(-5.5F, 8.0F, -3.5F, 11, 5, 7, g)
                // The hump of muscle behind the neck that sets the head low.
                .texOffs(88, 66).addBox(-5.0F, -1.5F, -1.0F, 10, 2, 5, g), PartPose.ZERO);
        root.addOrReplaceChild("jacket", CubeListBuilder.create()
                .texOffs(36, 16).addBox(-6.0F, 11.0F, -4.0F, 12, 3, 8, g.extend(0.25F))
                .texOffs(76, 17).addBox(-6.0F, 13.5F, -4.0F, 12, 6, 8, g.extend(0.3F)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.5F, -2.0F, -3.0F, 6, 6, 6, g)
                .texOffs(24, 32).addBox(-5.0F, 4.0F, -3.5F, 7, 8, 7, g), PartPose.offset(-SHOULDER_X, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(52, 32).addBox(-1.5F, -2.0F, -3.0F, 6, 6, 6, g)
                .texOffs(76, 32).addBox(-2.0F, 4.0F, -3.5F, 7, 8, 7, g), PartPose.offset(SHOULDER_X, 2.0F, 0.0F));
        root.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(88, 48).addBox(-5.0F, 6.0F, -3.5F, 7, 4, 7, g.extend(0.3F)), PartPose.offset(-SHOULDER_X, 2.0F,
                        0.0F));
        root.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(0, 66).addBox(-2.0F, 6.0F, -3.5F, 7, 4, 7, g.extend(0.3F))
                // The steel pauldron of the arena.
                .texOffs(28, 66).addBox(-2.5F, -3.0F, -4.0F, 8, 4, 8, g.extend(0.2F)), PartPose.offset(SHOULDER_X, 2.0F,
                        0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-2.5F, 0.0F, -3.0F, 5, 11, 6, g), PartPose.offset(-HIP_X, 12.0F + LEG_DY, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(22, 48).addBox(-2.5F, 0.0F, -3.0F, 5, 11, 6, g), PartPose.offset(HIP_X, 12.0F + LEG_DY, 0.0F));
        root.addOrReplaceChild("right_pants", CubeListBuilder.create()
                .texOffs(44, 48).addBox(-2.5F, 0.0F, -3.0F, 5, 11, 6, g.extend(0.25F)), PartPose.offset(-HIP_X, 12.0F
                        + LEG_DY, 0.0F));
        root.addOrReplaceChild("left_pants", CubeListBuilder.create()
                .texOffs(66, 48).addBox(-2.5F, 0.0F, -3.0F, 5, 11, 6, g.extend(0.25F)), PartPose.offset(HIP_X, 12.0F
                        + LEG_DY, 0.0F));
        root.addOrReplaceChild("ear", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("cloak", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw,
                          float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // The vanilla animation puts the parts where a player's are (shoulders 5 out, the head on top, the legs at 12):
        // move them to where the Hulk's are, keeping what the animation did to them.
        float k = SHOULDER_X / 5.0F;
        rightArm.x *= k;
        rightArm.z *= k;
        leftArm.x *= k;
        leftArm.z *= k;
        head.y += HEAD_DY;
        head.z = HEAD_DZ; // (the vanilla animation never sets the head's z: set, not added, or it would run away)
        rightLeg.y += LEG_DY;
        leftLeg.y += LEG_DY;
        rightLeg.x = Math.signum(rightLeg.x) * Math.max(Math.abs(rightLeg.x), HIP_X);
        leftLeg.x = Math.signum(leftLeg.x) * Math.max(Math.abs(leftLeg.x), HIP_X);
        if (rightLeg.x == 0.0F) {
            rightLeg.x = -HIP_X;
        }
        if (leftLeg.x == 0.0F) {
            leftLeg.x = HIP_X;
        }
        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }
}
