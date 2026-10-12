package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * The body of Ultron and of his Sentries: one hand-made model of about fifty boxes ({@link UltronCubes} is its table and
 * its place on the 128x128 skin), a biped with a ridged helmet and antennae, a jaw, a chest in two plates with a gap
 * for the red light of his torso, pauldrons, forearm pistons, five fingers on each hand and knees. Why not the player
 * model: it has no room for what must read at three and a half blocks, and Ultron is not shaped like a person.
 *
 * <p>The same model serves Prime, the Hybrid, the Sentry-Ultrons and the Sentries; the form (which boxes are drawn: the
 * Sentries have blades for antennae and no pauldron overhang) is set from the entity. How it stands is
 * {@link UltronPoses}, eased by {@link UltronAnimation}; this class only moves the joints to the angles of the pose.
 */
public class UltronModel<T extends LivingEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("ultron"), "main");

    private final ModelPart root;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private final Map<String, ModelPart> boxes = new HashMap<>();

    public UltronModel(ModelPart root) {
        this.root = root;
        collect();
    }

    private void collect() {
        // (The table lists every part after the one it hangs on.)
        for (UltronCubes.Part definition : UltronCubes.PARTS) {
            ModelPart parent = definition.parent().equals("root") ? root : parts.get(definition.parent());
            parts.put(definition.name(), parent.getChild(definition.name()));
        }
        for (UltronCubes.Cube cube : UltronCubes.CUBES) {
            boxes.put(cube.name(), parts.get(cube.part()).getChild(cube.name()));
        }
    }

    /** The skeleton and the boxes of {@link UltronCubes}, on a skin that is read as 64x64 and drawn on 128x128. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Map<String, PartDefinition> made = new HashMap<>();
        Map<String, UltronCubes.Part> definitions = new HashMap<>();
        for (UltronCubes.Part part : UltronCubes.PARTS) {
            definitions.put(part.name(), part);
        }
        for (UltronCubes.Part part : UltronCubes.PARTS) {
            PartDefinition parent = part.parent().equals("root") ? root : made.get(part.parent());
            UltronCubes.Part above = definitions.get(part.parent());
            float ox = part.x() - (above == null ? 0.0F : above.x());
            float oy = part.y() - (above == null ? 0.0F : above.y());
            float oz = part.z() - (above == null ? 0.0F : above.z());
            made.put(part.name(), parent.addOrReplaceChild(part.name(), CubeListBuilder.create(), PartPose.offset(ox, oy, oz)));
        }
        for (UltronCubes.Cube cube : UltronCubes.CUBES) {
            made.get(cube.part()).addOrReplaceChild(cube.name(), CubeListBuilder.create().texOffs(cube.u(), cube.v()).mirror(cube.mirror())
                    .addBox(cube.x(), cube.y(), cube.z(), cube.w(), cube.h(), cube.d()), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, UltronCubes.SKIN, UltronCubes.SKIN);
    }

    /** The part of the skeleton with this name ({@code rightFingers}, {@code head}), for the layers and the trackers. */
    public ModelPart part(String name) {
        return parts.get(name);
    }

    /** The root, the first part of every chain. */
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = ageInTicks - entity.tickCount;
        UltronPose pose = UltronAnimation.pose(entity, partial);
        int form = UltronAnimation.form(entity);
        for (UltronCubes.Cube cube : UltronCubes.CUBES) {
            boxes.get(cube.name()).visible = cube.in(form);
        }
        float[] v = pose.v;
        float r = Mth.DEG_TO_RAD;
        float shake = v[UltronPose.SHAKE] * r;
        float jx = shake * Mth.sin(ageInTicks * 17.3F);
        float jz = shake * Mth.cos(ageInTicks * 21.1F);
        root.y = -v[UltronPose.BOB] * 16.0F;
        ModelPart hips = parts.get("hips");
        hips.setRotation(v[UltronPose.HIPS_X] * r + jx, v[UltronPose.HIPS_Y] * r, v[UltronPose.HIPS_Z] * r + jz);
        // The head turns to where he looks, a little behind the look, on top of the pose.
        ModelPart head = parts.get("head");
        head.setRotation(v[UltronPose.HEAD_X] * r + Mth.clamp(headPitch, -35.0F, 35.0F) * r * 0.6F - jx * 0.5F,
                v[UltronPose.HEAD_Y] * r + Mth.clamp(netHeadYaw, -45.0F, 45.0F) * r * 0.7F,
                v[UltronPose.HEAD_Z] * r + jz * 0.5F);
        arm(true, v);
        arm(false, v);
        leg(true, v);
        leg(false, v);
        open(v[UltronPose.OPEN]);
    }

    private void arm(boolean right, float[] v) {
        float r = Mth.DEG_TO_RAD;
        String side = right ? "right" : "left";
        int x = right ? UltronPose.R_ARM_X : UltronPose.L_ARM_X;
        float s = right ? 1.0F : -1.0F;
        parts.get(side + "Arm").setRotation(v[x] * r, v[x + 1] * s * r, v[x + 2] * s * r);
        parts.get(side + "Forearm").setRotation(v[x + 3] * r, 0.0F, 0.0F);
        // The fingers curl in toward the body; the hand follows the forearm.
        parts.get(side + "Hand").setRotation(0.0F, 0.0F, 0.0F);
        parts.get(side + "Fingers").setRotation(0.0F, 0.0F, v[x + 4] * s * 85.0F * r);
    }

    private void leg(boolean right, float[] v) {
        float r = Mth.DEG_TO_RAD;
        String side = right ? "right" : "left";
        int x = right ? UltronPose.R_LEG_X : UltronPose.L_LEG_X;
        float s = right ? 1.0F : -1.0F;
        parts.get(side + "Leg").setRotation(v[x] * r, 0.0F, v[x + 1] * s * r);
        parts.get(side + "Shin").setRotation(v[x + 2] * r, 0.0F, 0.0F);
    }

    /** The plates fall open: the chest halves, the pauldrons and the jaw move away from the body. */
    private void open(float amount) {
        boxes.get("chestR").x = -amount * 3.0F;
        boxes.get("chestL").x = amount * 3.0F;
        boxes.get("chestR").y = amount * 1.0F;
        boxes.get("chestL").y = amount * 1.0F;
        boxes.get("pauldronAR").x = -amount * 2.0F;
        boxes.get("pauldronAL").x = amount * 2.0F;
        boxes.get("pauldronAR").y = -amount * 1.0F;
        boxes.get("pauldronAL").y = -amount * 1.0F;
        boxes.get("jaw").y = amount * 2.0F;
        boxes.get("jaw").z = -amount * 0.5F;
        boxes.get("thighPlateR").z = -amount * 1.0F;
        boxes.get("thighPlateL").z = -amount * 1.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
