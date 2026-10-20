package com.kolodkingal.enhanced.client.eternity;

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
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Eternity's shape (3.8, PLAN 10.4): a giant seated cross-legged, meditating, his hands on his knees, a faceted helmet
 * with a pointed crown, and four tall antenna-horns, two on each side, bent like an insect's legs in three segments
 * (the upper pair lean out, rise tall, then turn a little outward; the lower pair reach straight out, then hook
 * down). Our own shapes after the reference ({@code SCRATCH/v38/ref/eternity_ref.jpg}), drawn by
 * {@link EternityRenderer}: the body is a window onto space (its texture mapped by world position, not by these boxes'
 * UVs), the helmet and horns dark metal.
 *
 * <p>Units: pixels of the model, drawn at {@link #SCALE} times a pixel's size, so 4 pixels are a block; y up, the face
 * toward +z, the origin on the ground under the middle of his lap. About 44 blocks from the ground to the crown.
 */
public class EternityModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("eternity"), "main");
    /** How many times its pixel size the model is drawn (a pixel is a quarter of a block). */
    public static final float SCALE = 4.0F;
    /** The breath's rise of the chest, shoulders, arms and head (pixels) at the top of a breath. */
    static final float BREATH = 1.6F;
    /** The starfield's tint: a little deeper than the texture, as space is in the reference. */
    static final int BODY_TINT = 0xFFB4B2D0;

    private final ModelPart lap;
    private final ModelPart torso;
    private final ModelPart upper;
    private final ModelPart head;
    private final ModelPart helmet;
    private final ModelPart horns;
    private final float upperY;

    public EternityModel(ModelPart root) {
        lap = root.getChild("lap");
        torso = root.getChild("torso");
        upper = root.getChild("upper");
        head = upper.getChild("head");
        helmet = head.getChild("helmet");
        horns = head.getChild("horns");
        upperY = upper.y;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The lap: the hips, the two thighs out to the knees and the crossed shins in front.
        PartDefinition lap = root.addOrReplaceChild("lap", CubeListBuilder.create()
                .addBox(-40, 0, -24, 80, 24, 44)
                .addBox(-66, 0, -10, 30, 21, 46)
                .addBox(36, 0, -10, 30, 21, 46), PartPose.ZERO);
        lap.addOrReplaceChild("shin_l", CubeListBuilder.create().addBox(-50, 0, -9, 100, 17, 18),
                PartPose.offsetAndRotation(0, 0, 40, 0, 0.26F, 0));
        lap.addOrReplaceChild("shin_r", CubeListBuilder.create().addBox(-50, 0, -9, 100, 15, 18),
                PartPose.offsetAndRotation(0, 2, 40, 0, -0.26F, 0));
        // The torso, narrow at the waist.
        root.addOrReplaceChild("torso", CubeListBuilder.create()
                .addBox(-28, 22, -20, 56, 44, 36)
                .addBox(-32, 50, -22, 64, 18, 40), PartPose.ZERO);
        // What the breath lifts: the chest and shoulders, the arms, the neck and the head.
        PartDefinition upper = root.addOrReplaceChild("upper", CubeListBuilder.create()
                .addBox(-36, 0, -22, 72, 40, 40)
                .addBox(-46, 22, -18, 18, 20, 34)
                .addBox(28, 22, -18, 18, 20, 34)
                .addBox(-10, 40, -10, 20, 14, 20), PartPose.offset(0, 64, 0));
        // The arms: upper arms hanging a little out and forward, the forearms down to the hands on the knees.
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "_l" : "_r";
            PartDefinition arm = upper.addOrReplaceChild("arm" + s, CubeListBuilder.create()
                    .addBox(-10, -46, -10, 20, 48, 20), PartPose.offsetAndRotation(side * 40, 32, 0, -0.10F, 0,
                    side * 0.20F));
            arm.addOrReplaceChild("forearm" + s, CubeListBuilder.create()
                    .addBox(-9, -46, -9, 18, 48, 18)
                    .addBox(-11, -54, -6, 22, 12, 26), PartPose.offsetAndRotation(0, -44, 0, -0.85F, 0,
                    -side * 0.20F));
        }
        // The head: the faceted helmet with its pointed crown, and the horns.
        PartDefinition head = upper.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0, 52, 0));
        PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create()
                .addBox(-18, 0, -18, 36, 32, 36)
                .addBox(-14, 3, 17, 28, 22, 4)
                .addBox(-22, 6, -12, 4, 22, 24)
                .addBox(18, 6, -12, 4, 22, 24), PartPose.ZERO);
        helmet.addOrReplaceChild("crown", CubeListBuilder.create().addBox(-14, 0, -14, 28, 12, 28),
                PartPose.offsetAndRotation(0, 30, 0, 0, Mth.PI / 4, 0));
        helmet.addOrReplaceChild("crown_top", CubeListBuilder.create().addBox(-8, 0, -8, 16, 10, 16),
                PartPose.offsetAndRotation(0, 41, 0, 0, Mth.PI / 4, 0));
        helmet.addOrReplaceChild("point", CubeListBuilder.create().addBox(-3, 0, -3, 6, 9, 6),
                PartPose.offsetAndRotation(0, 50, 0, 0, Mth.PI / 4, 0));
        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "_l" : "_r";
            // (A segment runs along +y from its joint; a rotation of -a about z leans it toward +x.)
            horn(horns, "upper" + s, side, 18, 25, -2, new float[]{-1.15F, 0.95F, -0.35F}, new float[]{24, 40, 16});
            horn(horns, "lower" + s, side, 18, 12, -2, new float[]{-1.57F, -0.85F, -0.45F}, new float[]{28, 16, 12});
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * One antenna-horn of three segments from the joint ({@code side} * x, y, z) on the helmet: each segment's turn
     * (radians about z, mirrored for the left) after the one before, and its length.
     */
    private static void horn(PartDefinition parent, String name, int side, float x, float y, float z, float[] turns,
                             float[] lengths) {
        PartDefinition joint = parent;
        float at = 0;
        for (int i = 0; i < 3; i++) {
            float width = 9 - i * 2.0F;
            float jx = i == 0 ? side * x : 0;
            float jy = i == 0 ? y : at;
            float jz = i == 0 ? z : 0;
            joint = joint.addOrReplaceChild(name + "_" + i, CubeListBuilder.create()
                    .addBox(-width / 2, 0, -width / 2, width, lengths[i] + 1, width),
                    PartPose.offsetAndRotation(jx, jy, jz, 0, 0, side * turns[i]));
            at = lengths[i];
        }
    }

    /**
     * Poses him for this frame: the breath (0..1, its rise) and the head's turn toward the traveller (radians).
     */
    public void pose(float breath, float headYaw) {
        upper.y = upperY + breath * BREATH;
        head.yRot = headYaw;
    }

    /** The body (the starfield): the lap, the torso, the chest, shoulders and arms, the neck. */
    public void renderBody(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        lap.render(pose, buffer, light, overlay, BODY_TINT);
        torso.render(pose, buffer, light, overlay, BODY_TINT);
        helmet.visible = false;
        horns.visible = false;
        upper.render(pose, buffer, light, overlay, BODY_TINT);
        helmet.visible = true;
        horns.visible = true;
    }

    /** The helmet and the four horns (dark metal). */
    public void renderHelmet(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        pose.pushPose();
        upper.translateAndRotate(pose);
        head.translateAndRotate(pose);
        helmet.render(pose, buffer, light, overlay);
        horns.render(pose, buffer, light, overlay);
        pose.popPose();
    }

    /**
     * Points on the front of his body (model pixels) where the bright stars twinkle: scattered over the chest, the
     * torso, the lap and the arms, fixed for every frame (a seeded spread).
     */
    public static List<Vector3f> starPoints() {
        List<Vector3f> points = new ArrayList<>();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(20220708L);
        float[][] areas = {
                {-34, 66, 104, 34, 19}, {-26, 24, 64, 26, 17}, {-38, 2, 22, 38, 21}, {-60, 2, 20, 60, 37}};
        for (float[] a : areas) {
            for (int i = 0; i < 9; i++) {
                float x = a[0] + random.nextFloat() * (a[3] - a[0]);
                float y = a[1] + random.nextFloat() * (a[2] - a[1]);
                points.add(new Vector3f(x, y, a[4] + 0.6F));
            }
        }
        return points;
    }
}
