package com.kolodkingal.enhanced.client.hydra;

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

import java.util.Set;

/**
 * The Hydra soldiers' gear (3.6, lane L8): helmets, the visor, the cap with its red band, the headset, goggles, the
 * black vest with the emblem and its pouches, the belt, the radio, the enforcer's riot armour and shield, the energy
 * rifle, the sniper's long rifle, the stun baton, the officer's pistol, and the turned soldier's blue eyes. Four
 * groups drawn in the space of the body part they belong to ({@link #group}); which pieces a soldier wears is his
 * look's ({@link HydraLooks}). Texture {@code textures/entity/hydra/gear.png} (128 x 128) and its glowing parts
 * {@code gear_glow.png}, made with these very boxes by {@code SCRATCH/v36s/art/l8/gen_hydra.py}.
 */
public class HydraGearModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("hydra_gear"), "main");
    /** The groups, each in the space of the body part of the same name. */
    public static final String[] GROUPS = {"head", "body", "right_arm", "left_arm"};

    private final ModelPart root;

    public HydraGearModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(94, 0)
                .addBox(-4.0F, -9.0F, -4.0F, 8, 4, 8, new CubeDeformation(0.6F)), PartPose.ZERO);
        head.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(68, 31)
                .addBox(-4.0F, -6.0F, -5.0F, 8, 3, 1, new CubeDeformation(0.35F)), PartPose.ZERO);
        head.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(24, 19)
                .addBox(-4.0F, -9.0F, -4.0F, 8, 2, 8, new CubeDeformation(0.4F)), PartPose.ZERO);
        head.addOrReplaceChild("cap_brim", CubeListBuilder.create().texOffs(86, 31)
                .addBox(-3.0F, -7.0F, -7.0F, 6, 1, 3, new CubeDeformation(0.0F)), PartPose.ZERO);
        head.addOrReplaceChild("headset", CubeListBuilder.create().texOffs(0, 37)
                .addBox(-4.0F, -9.0F, -1.0F, 8, 1, 2, new CubeDeformation(0.55F)), PartPose.ZERO);
        head.addOrReplaceChild("headset_ear_r", CubeListBuilder.create().texOffs(108, 19)
                .addBox(-5.0F, -5.0F, -1.0F, 1, 3, 3, new CubeDeformation(0.1F)), PartPose.ZERO);
        head.addOrReplaceChild("headset_ear_l", CubeListBuilder.create().texOffs(116, 19)
                .addBox(4.0F, -5.0F, -1.0F, 1, 3, 3, new CubeDeformation(0.1F)), PartPose.ZERO);
        head.addOrReplaceChild("headset_mic", CubeListBuilder.create().texOffs(104, 31)
                .addBox(-5.0F, -3.0F, -4.0F, 1, 1, 3, new CubeDeformation(0.0F)), PartPose.ZERO);
        head.addOrReplaceChild("goggles", CubeListBuilder.create().texOffs(20, 37)
                .addBox(-4.0F, -5.0F, -5.0F, 8, 2, 1, new CubeDeformation(0.2F)), PartPose.ZERO);
        head.addOrReplaceChild("riot_helmet", CubeListBuilder.create().texOffs(62, 0)
                .addBox(-4.0F, -9.0F, -4.0F, 8, 5, 8, new CubeDeformation(0.85F)), PartPose.ZERO);
        head.addOrReplaceChild("riot_shield", CubeListBuilder.create().texOffs(0, 31)
                .addBox(-4.0F, -7.0F, -5.0F, 8, 5, 1, new CubeDeformation(0.55F)), PartPose.ZERO);
        head.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(64, 37)
                .addBox(-4.0F, -4.0F, -4.0F, 8, 1, 0, new CubeDeformation(0.36F)), PartPose.ZERO);
        head.addOrReplaceChild("eyes_visor", CubeListBuilder.create().texOffs(80, 37)
                .addBox(-4.0F, -4.0F, -5.0F, 8, 1, 0, new CubeDeformation(0.62F)), PartPose.ZERO);
        body.addOrReplaceChild("vest", CubeListBuilder.create().texOffs(0, 19)
                .addBox(-4.0F, 1.0F, -2.0F, 8, 8, 4, new CubeDeformation(0.5F)), PartPose.ZERO);
        body.addOrReplaceChild("pouch_r", CubeListBuilder.create().texOffs(38, 37)
                .addBox(-4.0F, 6.0F, -3.0F, 2, 2, 1, new CubeDeformation(0.25F)), PartPose.ZERO);
        body.addOrReplaceChild("pouch_m", CubeListBuilder.create().texOffs(44, 37)
                .addBox(-1.0F, 6.0F, -3.0F, 2, 2, 1, new CubeDeformation(0.25F)), PartPose.ZERO);
        body.addOrReplaceChild("pouch_l", CubeListBuilder.create().texOffs(50, 37)
                .addBox(2.0F, 6.0F, -3.0F, 2, 2, 1, new CubeDeformation(0.25F)), PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(18, 31)
                .addBox(-4.0F, 10.0F, -2.0F, 8, 2, 4, new CubeDeformation(0.3F)), PartPose.ZERO);
        body.addOrReplaceChild("radio", CubeListBuilder.create().texOffs(42, 31)
                .addBox(-2.0F, 2.0F, 2.0F, 4, 5, 1, new CubeDeformation(0.55F)), PartPose.ZERO);
        body.addOrReplaceChild("riot_chest", CubeListBuilder.create().texOffs(20, 0)
                .addBox(-4.0F, 0.0F, -2.0F, 8, 10, 4, new CubeDeformation(0.9F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("pad_r", CubeListBuilder.create().texOffs(76, 19)
                .addBox(-3.0F, -3.0F, -2.0F, 4, 3, 4, new CubeDeformation(0.6F)), PartPose.ZERO);
        leftArm.addOrReplaceChild("pad_l", CubeListBuilder.create().texOffs(92, 19)
                .addBox(-1.0F, -3.0F, -2.0F, 4, 3, 4, new CubeDeformation(0.6F)), PartPose.ZERO);
        leftArm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-3.0F, -1.0F, -4.0F, 8, 13, 1, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("rifle", CubeListBuilder.create().texOffs(10, 0)
                .addBox(-2.0F, 7.0F, -2.0F, 2, 12, 3, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("rifle_barrel", CubeListBuilder.create().texOffs(52, 31)
                .addBox(-1.5F, 19.0F, -1.0F, 1, 4, 1, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("rifle_mag", CubeListBuilder.create().texOffs(56, 31)
                .addBox(-1.5F, 10.0F, 1.0F, 1, 3, 2, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("rifle_sight", CubeListBuilder.create().texOffs(112, 31)
                .addBox(-1.5F, 12.0F, -3.0F, 1, 3, 1, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("rifle_cell", CubeListBuilder.create().texOffs(62, 31)
                .addBox(-3.0F, 12.0F, -1.5F, 1, 3, 2, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("long_rifle", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.0F, 6.0F, -2.0F, 2, 16, 3, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("long_barrel", CubeListBuilder.create().texOffs(60, 19)
                .addBox(-1.5F, 22.0F, -1.0F, 1, 7, 1, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("long_scope", CubeListBuilder.create().texOffs(64, 19)
                .addBox(-1.5F, 9.0F, -4.0F, 1, 6, 2, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("long_cell", CubeListBuilder.create().texOffs(116, 31)
                .addBox(-3.0F, 13.0F, -1.5F, 1, 2, 2, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("baton", CubeListBuilder.create().texOffs(56, 19)
                .addBox(-1.5F, 8.0F, -0.5F, 1, 9, 1, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("baton_tip", CubeListBuilder.create().texOffs(56, 37)
                .addBox(-1.5F, 17.0F, -0.5F, 1, 2, 1, new CubeDeformation(0.1F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("pistol", CubeListBuilder.create().texOffs(70, 19)
                .addBox(-1.5F, 8.0F, -1.5F, 1, 6, 2, new CubeDeformation(0.0F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("pistol_cell", CubeListBuilder.create().texOffs(60, 37)
                .addBox(-2.5F, 10.0F, -1.0F, 1, 2, 1, new CubeDeformation(0.0F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** A group of the gear ({@link #GROUPS}). */
    public ModelPart group(String name) {
        return root.getChild(name);
    }

    /** Shows only {@code pieces} of the gear. */
    public void show(Set<String> pieces) {
        for (String g : GROUPS) {
            ModelPart group = root.getChild(g);
            group.getAllParts().forEach(p -> p.visible = true);
            for (String piece : HydraLooks.ALL_PIECES) {
                if (group.hasChild(piece)) {
                    group.getChild(piece).visible = pieces.contains(piece);
                }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
