package com.kolodkingal.enhanced.client.ironman.suits;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The six suits' layer definitions (3.8, PLAN 7.1), written by {@code SCRATCH/v38/art/l7/gen_suits.py} from the same
 * description as their textures ({@code textures/entity/iron_man/<suit>.png} and {@code _glow.png}): change the script,
 * not this file. Each suit's root has one child per {@code SuitParts} (lower-case); each part has its segments {@code
 * s0}, {@code s1}... (the nanite wave reveals them one by one; the Hulkbuster's hinged front shells are segments too)
 * placed at their pivots, and its extras {@code x_<name>} (the Mark 3's pods, the Mark 50's blade, shield and cannon,
 * the Mark 85's back flaps and energy blades). Pixels in the space of the player model's part each rides on. Then the
 * props: the Mark 5's suitcase and the display's gantry.
 */
final class SuitMeshes {
    /**
     * A suit's layout: per part, its segments' count; the axis (0 x, 1 y, 2 z) a segment grows along in the nanite
     * wave; and per part its segments' hinges ({axis, angle} pairs, axis -1 for none; null when none has one).
     */
    record Info(int[] segments, int[] axes, float[][] hinges) {
    }

    /** Mark 1: 33 cubes. */
    static LayerDefinition mark1() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p;
        p = root.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(52, 0).addBox(-4F, -4F, -8F, 8, 2, 8, new CubeDeformation(0.9F))
                .texOffs(0, 0).addBox(-4F, -2F, -7F, 8, 6, 7, new CubeDeformation(0.9F, 0.81F, 0.9F))
                , PartPose.offset(0F, -4F, 4F));
        p = root.addOrReplaceChild("faceplate", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(38, 23).addBox(-4F, 0F, -1F, 8, 6, 1, new CubeDeformation(1.02F, 0.95F, 1.02F))
                , PartPose.offset(0F, -6F, -3F));
        p = root.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(84, 0).addBox(-4F, -3F, -1.525F, 8, 6, 4, new CubeDeformation(1F))
                .texOffs(102, 31).addBox(-2F, -2F, -2.975F, 4, 4, 1, new CubeDeformation(0F))
                .texOffs(16, 23).addBox(-3F, -4.5F, -2.025F, 6, 2, 5, new CubeDeformation(0.3F))
                , PartPose.offset(0F, 3F, -0.475F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(0, 13).addBox(-4F, 0F, -2F, 8, 6, 4, new CubeDeformation(0.96F, 0F, 0.96F))
                , PartPose.offset(0F, 6F, 0F));
        p = root.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(30, 0).addBox(-4F, -3F, 0F, 8, 10, 3, new CubeDeformation(0.5F, 0.5F, 1.25F))
                , PartPose.offset(0F, 3F, 2F));
        p = root.addOrReplaceChild("right_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(64, 13).addBox(-2.5F, 0F, -2.5F, 5, 4, 5, new CubeDeformation(1.25F))
                , PartPose.offset(-1F, -2.5F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(0, 31).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.85F, 0F, 0.85F))
                , PartPose.offset(-1F, 2F, 0F));
        p = root.addOrReplaceChild("left_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(84, 13).addBox(-2.5F, 0F, -2.5F, 5, 4, 5, new CubeDeformation(1.25F))
                , PartPose.offset(1F, -2.5F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(16, 31).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.85F, 0F, 0.85F))
                , PartPose.offset(1F, 2F, 0F));
        p = root.addOrReplaceChild("right_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(24, 13).addBox(-2.5F, 0F, -2.5F, 5, 5, 5, new CubeDeformation(1.05F))
                , PartPose.offset(-1F, 4F, 0F));
        p = root.addOrReplaceChild("left_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(44, 13).addBox(-2.95F, 0F, -2.5F, 5, 5, 5, new CubeDeformation(1.05F))
                .texOffs(96, 31).addBox(1.95F, 0F, -1F, 1, 4, 2, new CubeDeformation(0.1F))
                , PartPose.offset(1.45F, 4F, 0F));
        p = root.addOrReplaceChild("right_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 31).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.9F))
                .texOffs(124, 31).addBox(-1.5F, 1.5F, -1.8F, 1, 2, 1, new CubeDeformation(0.15F))
                .texOffs(4, 37).addBox(0.5F, 1.5F, -1.8F, 1, 2, 1, new CubeDeformation(0.15F))
                , PartPose.offset(-1F, 8F, 0F));
        p = root.addOrReplaceChild("left_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(48, 31).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.9F))
                .texOffs(0, 37).addBox(0.5F, 1.5F, -1.8F, 1, 2, 1, new CubeDeformation(0.15F))
                .texOffs(8, 37).addBox(-1.5F, 1.5F, -1.8F, 1, 2, 1, new CubeDeformation(0.15F))
                , PartPose.offset(1F, 8F, 0F));
        p = root.addOrReplaceChild("right_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(56, 23).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.95F, 0F, 0.95F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(88, 23).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.93F, 0F, 0.93F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("left_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(72, 23).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.95F, 0F, 0.95F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(104, 23).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.93F, 0F, 0.93F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("right_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(104, 13).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(1.1F))
                .texOffs(112, 31).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 1F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("left_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 23).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(1.1F))
                .texOffs(118, 31).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 1F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("right_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(64, 31).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(1.25F))
                .texOffs(12, 37).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(1.25F, 0F, 1.55F))
                , PartPose.offset(0F, 10F, -0.5F));
        p = root.addOrReplaceChild("left_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(80, 31).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(1.25F))
                .texOffs(22, 37).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(1.25F, 0F, 1.55F))
                , PartPose.offset(0F, 10F, -0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Mark 1: per part (SuitParts order) its segments, the axis the nanites grow along, its hinges. */
    static final Info MARK_1 = new Info(new int[] {1, 1, 2, 1, 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1},
            new int[] {2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new float[][] {null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null});

    /** Mark 3: 29 cubes. */
    static LayerDefinition mark3() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p;
        p = root.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(30, 0).addBox(-4F, -4F, -8F, 8, 2, 8, new CubeDeformation(0.65F))
                .texOffs(0, 0).addBox(-4F, -2F, -7F, 8, 6, 7, new CubeDeformation(0.65F, 0.585F, 0.65F))
                , PartPose.offset(0F, -4F, 4F));
        p = root.addOrReplaceChild("faceplate", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(96, 13).addBox(-4F, 0F, -1F, 8, 6, 1, new CubeDeformation(0.77F, 0.7F, 0.77F))
                , PartPose.offset(0F, -6F, -3F));
        p = root.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(62, 0).addBox(-4F, -3F, -1.425F, 8, 6, 4, new CubeDeformation(0.7F))
                .texOffs(52, 28).addBox(-1.5F, -1.5F, -2.575F, 3, 3, 1, new CubeDeformation(0F))
                , PartPose.offset(0F, 3F, -0.575F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(86, 0).addBox(-4F, 0F, -2F, 8, 6, 4, new CubeDeformation(0.66F, 0F, 0.66F))
                , PartPose.offset(0F, 6F, 0F));
        p = root.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(110, 0).addBox(-3F, -2F, 0F, 6, 8, 1, new CubeDeformation(0.5F, 0.5F, 0.95F))
                , PartPose.offset(0F, 3F, 2F));
        p = root.addOrReplaceChild("right_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.95F))
                , PartPose.offset(-1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(64, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.55F, 0F, 0.55F))
                , PartPose.offset(-1F, 2F, 0F));
        p.addOrReplaceChild("x_pod", CubeListBuilder.create()
                .texOffs(32, 28).addBox(-1F, 0F, -3F, 2, 1, 3, new CubeDeformation(0.05F))
                , PartPose.offset(-1F, -3.9F, 1.5F));
        p = root.addOrReplaceChild("left_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.95F))
                , PartPose.offset(1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(80, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.55F, 0F, 0.55F))
                , PartPose.offset(1F, 2F, 0F));
        p.addOrReplaceChild("x_pod", CubeListBuilder.create()
                .texOffs(42, 28).addBox(-1F, 0F, -3F, 2, 1, 3, new CubeDeformation(0.05F))
                , PartPose.offset(1F, -3.9F, 1.5F));
        p = root.addOrReplaceChild("right_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.75F))
                , PartPose.offset(-1F, 4F, 0F));
        p = root.addOrReplaceChild("left_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(48, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.75F))
                , PartPose.offset(1F, 4F, 0F));
        p = root.addOrReplaceChild("right_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(96, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.6F))
                , PartPose.offset(-1F, 8F, 0F));
        p = root.addOrReplaceChild("left_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(112, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.6F))
                , PartPose.offset(1F, 8F, 0F));
        p = root.addOrReplaceChild("right_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.6F, 0F, 0.6F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(32, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.58F, 0F, 0.58F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("left_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.6F, 0F, 0.6F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(48, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.58F, 0F, 0.58F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("right_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(64, 13).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.75F))
                .texOffs(60, 28).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.65F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("left_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(80, 13).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.75F))
                .texOffs(66, 28).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.65F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("right_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.9F))
                .texOffs(72, 28).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.9F, 0F, 1.2F))
                , PartPose.offset(0F, 10F, -0.5F));
        p = root.addOrReplaceChild("left_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 28).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.9F))
                .texOffs(82, 28).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.9F, 0F, 1.2F))
                , PartPose.offset(0F, 10F, -0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Mark 3: per part (SuitParts order) its segments, the axis the nanites grow along, its hinges. */
    static final Info MARK_3 = new Info(new int[] {1, 1, 2, 1, 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1},
            new int[] {2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new float[][] {null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null});

    /** Mark 5: 28 cubes. */
    static LayerDefinition mark5() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p;
        p = root.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(30, 0).addBox(-4F, -4F, -8F, 8, 2, 8, new CubeDeformation(0.62F))
                .texOffs(0, 0).addBox(-4F, -2F, -7F, 8, 6, 7, new CubeDeformation(0.62F, 0.558F, 0.62F))
                , PartPose.offset(0F, -4F, 4F));
        p = root.addOrReplaceChild("faceplate", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(96, 13).addBox(-4F, 0F, -1F, 8, 6, 1, new CubeDeformation(0.74F, 0.67F, 0.74F))
                , PartPose.offset(0F, -6F, -3F));
        p = root.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(62, 0).addBox(-4F, -3F, -1.55F, 8, 6, 4, new CubeDeformation(0.45F))
                .texOffs(56, 28).addBox(-1.5F, -1.5F, -2.45F, 3, 3, 1, new CubeDeformation(0F))
                .texOffs(32, 28).addBox(-4F, -3.6F, -1.55F, 8, 1, 4, new CubeDeformation(0.75F, 0F, 0.75F))
                , PartPose.offset(0F, 3F, -0.45F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(86, 0).addBox(-4F, 0F, -2F, 8, 6, 4, new CubeDeformation(0.41F, 0F, 0.41F))
                , PartPose.offset(0F, 6F, 0F));
        p = root.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(110, 0).addBox(-3F, -2F, 0F, 6, 8, 1, new CubeDeformation(0.5F, 0.5F, 0.7F))
                , PartPose.offset(0F, 3F, 2F));
        p = root.addOrReplaceChild("right_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.75F))
                , PartPose.offset(-1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(64, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.35F, 0F, 0.35F))
                , PartPose.offset(-1F, 2F, 0F));
        p = root.addOrReplaceChild("left_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.75F))
                , PartPose.offset(1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(80, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.35F, 0F, 0.35F))
                , PartPose.offset(1F, 2F, 0F));
        p = root.addOrReplaceChild("right_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.55F))
                , PartPose.offset(-1F, 4F, 0F));
        p = root.addOrReplaceChild("left_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(48, 13).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.55F))
                , PartPose.offset(1F, 4F, 0F));
        p = root.addOrReplaceChild("right_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(96, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.4F))
                , PartPose.offset(-1F, 8F, 0F));
        p = root.addOrReplaceChild("left_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(112, 21).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.4F))
                , PartPose.offset(1F, 8F, 0F));
        p = root.addOrReplaceChild("right_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.4F, 0F, 0.4F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(32, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.38F, 0F, 0.38F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("left_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.4F, 0F, 0.4F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(48, 21).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.38F, 0F, 0.38F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("right_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(64, 13).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.55F))
                .texOffs(64, 28).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.45F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("left_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(80, 13).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.55F))
                .texOffs(70, 28).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.45F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("right_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.7F))
                .texOffs(76, 28).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.7F, 0F, 1F))
                , PartPose.offset(0F, 10F, -0.5F));
        p = root.addOrReplaceChild("left_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 28).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.7F))
                .texOffs(86, 28).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.7F, 0F, 1F))
                , PartPose.offset(0F, 10F, -0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Mark 5: per part (SuitParts order) its segments, the axis the nanites grow along, its hinges. */
    static final Info MARK_5 = new Info(new int[] {1, 1, 2, 1, 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1},
            new int[] {2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new float[][] {null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null});

    /** Mark 50: 36 cubes. */
    static LayerDefinition mark50() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p;
        p = root.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(24, 16).addBox(-4F, -4F, -8F, 8, 2, 8, new CubeDeformation(0.62F))
                .texOffs(62, 0).addBox(-4F, -2F, -7F, 8, 6, 7, new CubeDeformation(0.62F, 0.558F, 0.62F))
                , PartPose.offset(0F, -4F, 4F));
        p = root.addOrReplaceChild("faceplate", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 35).addBox(-4F, 0F, -1F, 8, 6, 1, new CubeDeformation(0.74F, 0.67F, 0.74F))
                , PartPose.offset(0F, -6F, -3F));
        p = root.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(56, 16).addBox(-4F, -3F, -1.5F, 8, 6, 4, new CubeDeformation(0.55F))
                .texOffs(64, 42).addBox(-2F, -1.5F, -2.5F, 4, 3, 1, new CubeDeformation(0F))
                , PartPose.offset(0F, 3F, -0.5F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(80, 16).addBox(-4F, 0F, -2F, 8, 6, 4, new CubeDeformation(0.51F, 0F, 0.51F))
                , PartPose.offset(0F, 6F, 0F));
        p = root.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(104, 16).addBox(-3F, -2F, 0F, 6, 8, 1, new CubeDeformation(0.5F, 0.5F, 0.8F))
                , PartPose.offset(0F, 3F, 2F));
        p = root.addOrReplaceChild("right_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.85F))
                , PartPose.offset(-1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(82, 35).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.45F, 0F, 0.45F))
                , PartPose.offset(-1F, 2F, 0F));
        p = root.addOrReplaceChild("left_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 27).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.85F))
                , PartPose.offset(1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(98, 35).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.45F, 0F, 0.45F))
                , PartPose.offset(1F, 2F, 0F));
        p = root.addOrReplaceChild("right_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 27).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                , PartPose.offset(-1F, 4F, 0F));
        p.addOrReplaceChild("x_blade", CubeListBuilder.create()
                .texOffs(54, 0).addBox(-0.5F, 0F, -1.5F, 1, 13, 3, new CubeDeformation(0F))
                .texOffs(74, 42).addBox(-0.5F, 13F, -1F, 1, 2, 2, new CubeDeformation(0F))
                , PartPose.offset(-1F, 5F, 0F));
        p.addOrReplaceChild("x_cannon", CubeListBuilder.create()
                .texOffs(92, 0).addBox(-3F, 0F, -3F, 6, 5, 6, new CubeDeformation(0F))
                .texOffs(96, 27).addBox(-2F, 5F, -2F, 4, 4, 4, new CubeDeformation(0F))
                , PartPose.offset(-1F, 4F, 0F));
        p = root.addOrReplaceChild("left_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(48, 27).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                , PartPose.offset(1F, 4F, 0F));
        p.addOrReplaceChild("x_cannon", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3F, 0F, -3F, 6, 5, 6, new CubeDeformation(0F))
                .texOffs(112, 27).addBox(-2F, 5F, -2F, 4, 4, 4, new CubeDeformation(0F))
                , PartPose.offset(1F, 4F, 0F));
        p.addOrReplaceChild("x_shield", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.2F, -3F, -5F, 1, 6, 10, new CubeDeformation(0F))
                .texOffs(40, 0).addBox(-0.2F, -5F, -3F, 1, 10, 6, new CubeDeformation(0F))
                .texOffs(22, 0).addBox(-0.6F, -4F, -4F, 1, 8, 8, new CubeDeformation(0F))
                , PartPose.offset(4.2F, 6F, 0F));
        p = root.addOrReplaceChild("right_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 42).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.5F))
                , PartPose.offset(-1F, 8F, 0F));
        p = root.addOrReplaceChild("left_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 42).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.5F))
                , PartPose.offset(1F, 8F, 0F));
        p = root.addOrReplaceChild("right_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(18, 35).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.5F, 0F, 0.5F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(50, 35).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.48F, 0F, 0.48F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("left_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(34, 35).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.5F, 0F, 0.5F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(66, 35).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.48F, 0F, 0.48F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("right_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(64, 27).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                .texOffs(80, 42).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.55F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("left_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(80, 27).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                .texOffs(86, 42).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.55F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("right_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 42).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.8F))
                .texOffs(92, 42).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.8F, 0F, 1.1F))
                , PartPose.offset(0F, 10F, -0.5F));
        p = root.addOrReplaceChild("left_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(48, 42).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.8F))
                .texOffs(102, 42).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.8F, 0F, 1.1F))
                , PartPose.offset(0F, 10F, -0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Mark 50: per part (SuitParts order) its segments, the axis the nanites grow along, its hinges. */
    static final Info MARK_50 = new Info(new int[] {1, 1, 2, 1, 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1},
            new int[] {2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new float[][] {null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null});

    /** Mark 85: 35 cubes. */
    static LayerDefinition mark85() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p;
        p = root.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(54, 0).addBox(-4F, -4F, -8F, 8, 2, 8, new CubeDeformation(0.62F))
                .texOffs(24, 0).addBox(-4F, -2F, -7F, 8, 6, 7, new CubeDeformation(0.62F, 0.558F, 0.62F))
                , PartPose.offset(0F, -4F, 4F));
        p = root.addOrReplaceChild("faceplate", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 43).addBox(-4F, 0F, -1F, 8, 6, 1, new CubeDeformation(0.74F, 0.67F, 0.74F))
                , PartPose.offset(0F, -6F, -3F));
        p = root.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(86, 0).addBox(-4F, -3F, -1.5F, 8, 6, 4, new CubeDeformation(0.55F))
                .texOffs(96, 51).addBox(-2F, -1.5F, -2.5F, 4, 3, 1, new CubeDeformation(0F))
                , PartPose.offset(0F, 3F, -0.5F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(0, 33).addBox(-4F, 0F, -2F, 8, 6, 4, new CubeDeformation(0.51F, 0F, 0.51F))
                , PartPose.offset(0F, 6F, 0F));
        p = root.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(36, 33).addBox(-3F, -2F, 0F, 6, 8, 1, new CubeDeformation(0.5F, 0.5F, 0.8F))
                , PartPose.offset(0F, 3F, 2F));
        p.addOrReplaceChild("x_flap0l", CubeListBuilder.create()
                .texOffs(56, 33).addBox(-1F, -8F, 0F, 2, 8, 1, new CubeDeformation(0F))
                , PartPose.offset(3.2F, 9.5F, 3.4F));
        p.addOrReplaceChild("x_flap0r", CubeListBuilder.create()
                .texOffs(50, 33).addBox(-1F, -8F, 0F, 2, 8, 1, new CubeDeformation(0F))
                , PartPose.offset(-3.2F, 9.5F, 3.4F));
        p.addOrReplaceChild("x_flap1l", CubeListBuilder.create()
                .texOffs(30, 33).addBox(-1F, -9F, 0F, 2, 9, 1, new CubeDeformation(0F))
                , PartPose.offset(1.1F, 9.5F, 3.4F));
        p.addOrReplaceChild("x_flap1r", CubeListBuilder.create()
                .texOffs(24, 33).addBox(-1F, -9F, 0F, 2, 9, 1, new CubeDeformation(0F))
                , PartPose.offset(-1.1F, 9.5F, 3.4F));
        p = root.addOrReplaceChild("right_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(62, 33).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.85F))
                , PartPose.offset(-1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(0, 51).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.45F, 0F, 0.45F))
                , PartPose.offset(-1F, 2F, 0F));
        p = root.addOrReplaceChild("left_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(78, 33).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.85F))
                , PartPose.offset(1F, -2F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(16, 51).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.45F, 0F, 0.45F))
                , PartPose.offset(1F, 2F, 0F));
        p = root.addOrReplaceChild("right_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(94, 33).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                , PartPose.offset(-1F, 4F, 0F));
        p = root.addOrReplaceChild("left_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(110, 33).addBox(-2F, 0F, -2F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                , PartPose.offset(1F, 4F, 0F));
        p = root.addOrReplaceChild("right_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 51).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.5F))
                , PartPose.offset(-1F, 8F, 0F));
        p.addOrReplaceChild("x_eblade", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, 0F, -1.5F, 1, 30, 3, new CubeDeformation(0F))
                .texOffs(16, 0).addBox(-0.5F, 0F, -0.5F, 1, 32, 1, new CubeDeformation(-0.2F, 0F, -0.2F))
                , PartPose.offset(-3.4F, 8.6F, 0F));
        p = root.addOrReplaceChild("left_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(48, 51).addBox(-2F, 0F, -2F, 4, 2, 4, new CubeDeformation(0.5F))
                , PartPose.offset(1F, 8F, 0F));
        p.addOrReplaceChild("x_eblade", CubeListBuilder.create()
                .texOffs(8, 0).addBox(-0.5F, 0F, -1.5F, 1, 30, 3, new CubeDeformation(0F))
                .texOffs(20, 0).addBox(-0.5F, 0F, -0.5F, 1, 32, 1, new CubeDeformation(-0.2F, 0F, -0.2F))
                , PartPose.offset(3.4F, 8.6F, 0F));
        p = root.addOrReplaceChild("right_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(50, 43).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.5F, 0F, 0.5F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(82, 43).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.48F, 0F, 0.48F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("left_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(66, 43).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.5F, 0F, 0.5F))
                , PartPose.offset(0F, 0F, 0F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(98, 43).addBox(-2F, 0F, -2F, 4, 3, 4, new CubeDeformation(0.48F, 0F, 0.48F))
                , PartPose.offset(0F, 3F, 0F));
        p = root.addOrReplaceChild("right_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 43).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                .texOffs(106, 51).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.55F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("left_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(16, 43).addBox(-2F, 1F, -1.5F, 4, 4, 4, new CubeDeformation(0.65F, 0.25F, 0.65F))
                .texOffs(112, 51).addBox(-1F, 0F, -2.5F, 2, 2, 1, new CubeDeformation(0.1F, 0.1F, 0.55F))
                , PartPose.offset(0F, 5F, -0.5F));
        p = root.addOrReplaceChild("right_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(64, 51).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.8F))
                .texOffs(118, 51).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.8F, 0F, 1.1F))
                , PartPose.offset(0F, 10F, -0.5F));
        p = root.addOrReplaceChild("left_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(80, 51).addBox(-2F, 0F, -1.5F, 4, 2, 4, new CubeDeformation(0.8F))
                .texOffs(0, 57).addBox(-2F, 1F, -2.5F, 4, 1, 1, new CubeDeformation(0.8F, 0F, 1.1F))
                , PartPose.offset(0F, 10F, -0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Mark 85: per part (SuitParts order) its segments, the axis the nanites grow along, its hinges. */
    static final Info MARK_85 = new Info(new int[] {1, 1, 2, 1, 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1},
            new int[] {2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new float[][] {null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null});

    /** Hulkbuster: 27 cubes. */
    static LayerDefinition hulkbuster() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p;
        p = root.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(28, 82).addBox(-3F, -3F, -4F, 6, 6, 4, new CubeDeformation(0.3F))
                .texOffs(0, 94).addBox(-3F, -3F, -6F, 6, 1, 2, new CubeDeformation(0.3F))
                , PartPose.offset(0F, -3F, 3F));
        p = root.addOrReplaceChild("faceplate", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(88, 82).addBox(-3F, 0F, -2F, 6, 5, 2, new CubeDeformation(0.42F, 0.35F, 0.42F))
                , PartPose.offset(0F, -5F, -1F));
        p = root.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8F, -6F, -3F, 16, 12, 6, new CubeDeformation(0F))
                , PartPose.offset(0F, 3F, 2F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(70, 45).addBox(0F, -3F, 0F, 8, 9, 4, new CubeDeformation(0.05F))
                .texOffs(104, 82).addBox(6F, 0F, -0.6F, 4, 4, 1, new CubeDeformation(0F))
                , PartPose.offset(-8F, 0F, -5F));
        p.addOrReplaceChild("s2", CubeListBuilder.create()
                .texOffs(94, 45).addBox(-8F, -3F, 0F, 8, 9, 4, new CubeDeformation(0.05F))
                , PartPose.offset(8F, 0F, -5F));
        p.addOrReplaceChild("s3", CubeListBuilder.create()
                .texOffs(64, 18).addBox(-6F, 3F, -4F, 12, 5, 8, new CubeDeformation(0F))
                , PartPose.offset(0F, 6F, 0F));
        p = root.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(36, 45).addBox(-7F, -5F, 0F, 14, 10, 3, new CubeDeformation(0F))
                , PartPose.offset(0F, 3F, 5F));
        p = root.addOrReplaceChild("right_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(44, 0).addBox(-4.5F, 0F, -5F, 9, 7, 10, new CubeDeformation(0F))
                .texOffs(30, 70).addBox(-3F, 7F, -3.5F, 7, 5, 7, new CubeDeformation(0F))
                , PartPose.offset(-0.5F, -5F, 0F));
        p = root.addOrReplaceChild("left_upper_arm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(82, 0).addBox(-4.5F, 0F, -5F, 9, 7, 10, new CubeDeformation(0F))
                .texOffs(58, 70).addBox(-4F, 7F, -3.5F, 7, 5, 7, new CubeDeformation(0F))
                , PartPose.offset(0.5F, -5F, 0F));
        p = root.addOrReplaceChild("right_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.5F, 0F, -4.5F, 9, 4, 9, new CubeDeformation(0F))
                , PartPose.offset(-0.5F, 7F, -0.5F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(72, 32).addBox(0F, 0F, 0F, 9, 4, 9, new CubeDeformation(0.05F))
                , PartPose.offset(-5F, 11F, -5F));
        p = root.addOrReplaceChild("left_forearm", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(36, 32).addBox(-4.5F, 0F, -4.5F, 9, 4, 9, new CubeDeformation(0F))
                , PartPose.offset(0.5F, 7F, -0.5F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(0, 45).addBox(-9F, 0F, 0F, 9, 4, 9, new CubeDeformation(0.05F))
                , PartPose.offset(5F, 11F, -5F));
        p = root.addOrReplaceChild("right_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-4F, 0F, -4F, 8, 6, 8, new CubeDeformation(0F))
                , PartPose.offset(-0.5F, 15F, -0.5F));
        p = root.addOrReplaceChild("left_gauntlet", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(32, 18).addBox(-4F, 0F, -4F, 8, 6, 8, new CubeDeformation(0F))
                , PartPose.offset(0.5F, 15F, -0.5F));
        p = root.addOrReplaceChild("right_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(86, 70).addBox(-3.5F, 0F, -3.5F, 7, 5, 7, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        p = root.addOrReplaceChild("left_thigh", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 82).addBox(-3.5F, 0F, -3.5F, 7, 5, 7, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        p = root.addOrReplaceChild("right_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(76, 58).addBox(-4F, 0F, -3.5F, 8, 5, 7, new CubeDeformation(0F))
                , PartPose.offset(0F, 5F, 0.5F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(48, 82).addBox(0F, 0F, 0F, 8, 6, 2, new CubeDeformation(0.05F))
                , PartPose.offset(-4F, 4F, -4.5F));
        p = root.addOrReplaceChild("left_shin", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 70).addBox(-4F, 0F, -3.5F, 8, 5, 7, new CubeDeformation(0F))
                , PartPose.offset(0F, 5F, 0.5F));
        p.addOrReplaceChild("s1", CubeListBuilder.create()
                .texOffs(68, 82).addBox(-8F, 0F, 0F, 8, 6, 2, new CubeDeformation(0.05F))
                , PartPose.offset(4F, 4F, -4.5F));
        p = root.addOrReplaceChild("right_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(0, 58).addBox(-4.5F, 0F, -5F, 9, 2, 10, new CubeDeformation(0F))
                , PartPose.offset(0F, 10F, -0.5F));
        p = root.addOrReplaceChild("left_boot", CubeListBuilder.create(), PartPose.ZERO);
        p.addOrReplaceChild("s0", CubeListBuilder.create()
                .texOffs(38, 58).addBox(-4.5F, 0F, -5F, 9, 2, 10, new CubeDeformation(0F))
                , PartPose.offset(0F, 10F, -0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Hulkbuster: per part (SuitParts order) its segments, the axis the nanites grow along, its hinges. */
    static final Info HULKBUSTER = new Info(new int[] {1, 1, 4, 1, 1, 1, 2, 2, 1, 1, 1, 1, 2, 2, 1, 1},
            new int[] {2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new float[][] {null, {0F, -1.3F}, {-1F, 0F, 1F, 1.4F, 1F, -1.4F, -1F, 0F}, null, null, null, {-1F,
            0F, 1F, 0.9F}, {-1F, 0F, 1F, -0.9F}, null, null, null, null, {-1F, 0F, 1F, 1.1F}, {-1F, 0F, 1F,
            -1.1F}, null, null});

    /** The suitcase prop. */
    static LayerDefinition suitcase() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6F, -2F, -4.5F, 12, 2, 9, new CubeDeformation(0F))
                .texOffs(42, 11).addBox(-2F, -1.5F, -5.5F, 4, 1, 1, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("lid", CubeListBuilder.create()
                .texOffs(0, 11).addBox(-6F, -1F, -9F, 12, 1, 9, new CubeDeformation(0F))
                , PartPose.offset(0F, -2F, 4.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** The suit gantry prop. */
    static LayerDefinition suitGantry() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("upright", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1F, -31F, -1F, 2, 31, 2, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("beam", CubeListBuilder.create()
                .texOffs(0, 33).addBox(-8F, -1F, -1.5F, 16, 2, 3, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("link", CubeListBuilder.create()
                .texOffs(8, 0).addBox(-1F, 0F, -1F, 2, 16, 2, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("joint", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(38, 33).addBox(-1.5F, 0F, -1.5F, 3, 2, 3, new CubeDeformation(0F))
                .texOffs(50, 33).addBox(-0.5F, 2F, -0.5F, 1, 3, 1, new CubeDeformation(0F))
                , PartPose.offset(0F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
    private SuitMeshes() {
    }
}
