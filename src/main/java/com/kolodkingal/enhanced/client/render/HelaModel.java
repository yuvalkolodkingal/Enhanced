package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.HelaEntity;
import com.kolodkingal.enhanced.hela.HelaState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * The boss Hela's body (3.5): the slim body of the hero's skins (the same textures: {@code hela_goddess}), posed by
 * {@link HelaBossPoses} from her synced state and its time with the partial tick, plus a long cloak from the shoulders that
 * the walk and the wind of her attacks move ({@link #CLOAK}, a model of its own drawn with the body's turn). How much of her
 * is there ({@link #alpha}) is read by the renderer: she forms out of smoke and dissolves into it.
 */
public class HelaModel extends PlayerModel<HelaEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("hela_boss"), "main");
    public static final ModelLayerLocation CLOAK = new ModelLayerLocation(Enhanced.id("hela_boss"), "cloak");

    /** How much of her is drawn (0 to 1), and her whirl (degrees), for the renderer. */
    public float alpha = 1.0F;
    public float spin;
    /** The pose's values now (the renderer reads the drop for the cloak and the blades). */
    public float[] pose = HelaBossPoses.STAND.clone();

    public HelaModel(ModelPart root) {
        super(root, true);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, true);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The cloak: one long panel from the shoulders to the ankles, hanging behind her (texture: a cape's layout, 64 x 32). */
    public static LayerDefinition createCloakLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition cloak = root.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -0.5F, 10.0F, 22.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 2.6F));
        cloak.addOrReplaceChild("hem", CubeListBuilder.create().texOffs(22, 0).addBox(-5.5F, 0.0F, -0.5F, 11.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(HelaEntity hela, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(hela, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float partial = ageInTicks - hela.tickCount;
        HelaState state = hela.state();
        float time = hela.stateTime(partial);
        float[] p = HelaBossPoses.at(state, time);
        pose = p;
        float r = Mth.DEG_TO_RAD;
        // A slow, menacing sway over every pose: the blades turning in her hands.
        float sway = Mth.sin(ageInTicks * 0.07F);
        float walk = Math.min(1.0F, limbSwingAmount);
        rightArm.xRot = p[HelaBossPoses.Ch.RX.ordinal()] * r + sway * 0.04F + Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.35F * walk;
        rightArm.yRot = p[HelaBossPoses.Ch.RY.ordinal()] * r;
        rightArm.zRot = p[HelaBossPoses.Ch.RZ.ordinal()] * r + sway * 0.03F;
        leftArm.xRot = p[HelaBossPoses.Ch.LX.ordinal()] * r - sway * 0.04F + Mth.cos(limbSwing * 0.6662F) * 0.35F * walk;
        leftArm.yRot = p[HelaBossPoses.Ch.LY.ordinal()] * r;
        leftArm.zRot = p[HelaBossPoses.Ch.LZ.ordinal()] * r - sway * 0.03F;
        body.yRot = p[HelaBossPoses.Ch.TWIST.ordinal()] * r;
        body.xRot = p[HelaBossPoses.Ch.LEAN.ordinal()] * r;
        float legR = p[HelaBossPoses.Ch.LEG_R.ordinal()];
        float legL = p[HelaBossPoses.Ch.LEG_L.ordinal()];
        if (legR != 0.0F || legL != 0.0F) {
            rightLeg.xRot = legR * r;
            leftLeg.xRot = legL * r;
            rightLeg.yRot = 0.0F;
            leftLeg.yRot = 0.0F;
        } else {
            // (The glide of her walk: a short, even stride.)
            rightLeg.xRot *= 0.7F;
            leftLeg.xRot *= 0.7F;
        }
        head.xRot += p[HelaBossPoses.Ch.HEAD_X.ordinal()] * r;
        head.yRot += p[HelaBossPoses.Ch.HEAD_Y.ordinal()] * r;
        float drop = p[HelaBossPoses.Ch.DROP.ordinal()];
        // The lean turns the chest about the neck: the hips go back and the legs with them (as a crouch does); the twist
        // carries the shoulders round. The drop sinks everything (a kneel).
        float lean = body.xRot;
        float twist = body.yRot;
        head.y = drop;
        body.y = drop;
        rightArm.x = -Mth.cos(twist) * 5.0F;
        rightArm.z = Mth.sin(twist) * 5.0F + Mth.sin(lean) * 2.0F;
        leftArm.x = Mth.cos(twist) * 5.0F;
        leftArm.z = -Mth.sin(twist) * 5.0F + Mth.sin(lean) * 2.0F;
        rightArm.y = 2.0F + drop;
        leftArm.y = 2.0F + drop;
        rightLeg.y = 12.0F * Mth.cos(lean) + drop;
        leftLeg.y = 12.0F * Mth.cos(lean) + drop;
        rightLeg.z = 12.0F * Mth.sin(lean);
        leftLeg.z = 12.0F * Mth.sin(lean);
        alpha = Mth.clamp(p[HelaBossPoses.Ch.ALPHA.ordinal()], 0.0F, 1.0F);
        float spinRate = p[HelaBossPoses.Ch.SPIN.ordinal()];
        spin = state == HelaState.STORM ? spinAngle(time) : 0.0F;
        if (spinRate == 0.0F && state != HelaState.STORM) {
            spin = 0.0F;
        }
        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }

    /** How far she has turned in the storm by {@code time} ticks into it: the integral of the pose's whirl. */
    static float spinAngle(float time) {
        float angle = 0.0F;
        float step = 0.5F;
        for (float t = 0.0F; t < time; t += step) {
            angle += HelaBossPoses.at(HelaState.STORM, t)[HelaBossPoses.Ch.SPIN.ordinal()] * Math.min(step, time - t);
        }
        return angle;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, (a << 24) | (color & 0xFFFFFF));
    }
}
