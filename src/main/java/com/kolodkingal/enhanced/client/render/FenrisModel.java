package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.FenrisEntity;
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
 * Fenris (3.5): a giant wolf of our own cuboids (no vanilla wolf), drawn at 1.5 times by {@link FenrisRenderer} (2.4
 * blocks high): a long body with a shaggy ruff over the shoulders, a heavy skull with a long snout and a jaw that opens,
 * pointed ears, four legs of two parts each and a brush of a tail. His poses are all from his synced action and its time
 * ({@link FenrisEntity#action}, {@link FenrisEntity#actionTime}) and his walk, so they are smooth at any frame rate:
 * the lope, the crouch and spring of the pounce, the head thrown back with the jaw wide in the howl, the snap of the bite,
 * and his climb out of the floor.
 */
public class FenrisModel extends HierarchicalModel<FenrisEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("fenris"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart ruff;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];

    public FenrisModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.ruff = root.getChild("ruff");
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.tail = root.getChild("tail");
        String[] names = {"leg_fr", "leg_fl", "leg_hr", "leg_hl"};
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild(names[i]);
            shins[i] = legs[i].getChild("shin");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -10.0F, 10.0F, 10.0F, 20.0F),
                PartPose.offset(0.0F, 8.0F, 1.0F));
        root.addOrReplaceChild("ruff", CubeListBuilder.create().texOffs(0, 30).addBox(-6.0F, -6.5F, -4.0F, 12.0F, 13.0F, 9.0F,
                new CubeDeformation(0.2F)), PartPose.offset(0.0F, 7.5F, -7.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-4.5F, -4.5F, -7.0F, 9.0F, 8.0F, 8.0F)
                .texOffs(60, 16).addBox(-2.5F, -1.5F, -13.0F, 5.0F, 4.0F, 6.0F)
                .texOffs(94, 0).addBox(-4.0F, -8.5F, -3.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(102, 0).addBox(2.0F, -8.5F, -3.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 5.0F, -12.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(60, 26).addBox(-2.25F, 0.0F, -6.0F, 4.5F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 2.5F, -7.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(86, 26).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 4.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, 10.5F, -0.7F, 0.0F, 0.0F));
        float[][] at = {{-3.6F, -6.5F}, {3.6F, -6.5F}, {-3.6F, 7.5F}, {3.6F, 7.5F}};
        String[] names = {"leg_fr", "leg_fl", "leg_hr", "leg_hl"};
        for (int i = 0; i < 4; i++) {
            PartDefinition leg = root.addOrReplaceChild(names[i], CubeListBuilder.create().texOffs(102 - (i % 2) * 16, 40)
                    .addBox(-2.0F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offset(at[i][0], 12.0F, at[i][1]));
            leg.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(102 - (i % 2) * 16, 52)
                    .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(0.0F, 5.5F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FenrisEntity wolf, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float partial = ageInTicks - wolf.tickCount;
        float swing = Math.min(1.0F, limbSwingAmount * 1.2F);
        float lope = limbSwing * 0.55F;
        // The lope: the legs in pairs, the shins following, the body rocking a little.
        for (int i = 0; i < 4; i++) {
            float phase = (i < 2 ? 0.0F : Mth.PI) + (i % 2) * 0.6F;
            legs[i].xRot = Mth.cos(lope + phase) * 0.8F * swing;
            shins[i].xRot = Math.max(0.0F, -Mth.sin(lope + phase)) * 0.7F * swing;
        }
        body.xRot = Mth.sin(lope * 2.0F) * 0.03F * swing;
        ruff.xRot = body.xRot;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.7F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F + Mth.sin(ageInTicks * 0.08F) * 0.03F;
        tail.xRot = -0.7F + Mth.sin(ageInTicks * 0.12F) * 0.08F + swing * 0.35F;
        tail.yRot = Mth.sin(ageInTicks * 0.2F + lope) * 0.25F;
        jaw.xRot = 0.05F + Math.max(0.0F, Mth.sin(ageInTicks * 0.06F)) * 0.06F;
        float t = wolf.actionTime(partial);
        switch (wolf.action()) {
            case FenrisEntity.ACT_HOWL -> howl(t);
            case FenrisEntity.ACT_POUNCE -> pounce(t);
            case FenrisEntity.ACT_BITE -> bite(t);
            default -> {
            }
        }
        float risen = wolf.risen(wolf.level().getGameTime() + partial);
        if (risen < 1.0F) {
            // Climbing out: the forelegs reach up and claw, the head raised to the hall.
            float k = 1.0F - risen;
            legs[0].xRot = -1.4F * k + Mth.sin(ageInTicks * 0.9F) * 0.4F * k;
            legs[1].xRot = -1.4F * k + Mth.cos(ageInTicks * 0.9F) * 0.4F * k;
            head.xRot = -0.6F * k;
            jaw.xRot = 0.6F * k;
        }
    }

    /** The howl: the head thrown back and the jaw wide, the forelegs braced, held, then let down. */
    private void howl(float t) {
        float up = AnimMath.ss(0.0F, 8.0F, t) * (1.0F - AnimMath.ss(FenrisEntity.HOWL_TICKS - 8.0F, FenrisEntity.HOWL_TICKS, t));
        head.xRot = -1.05F * up;
        jaw.xRot = 0.05F + 0.75F * up;
        body.xRot = -0.12F * up;
        ruff.xRot = body.xRot;
        legs[0].xRot = -0.25F * up;
        legs[1].xRot = -0.25F * up;
        legs[2].xRot = 0.2F * up;
        legs[3].xRot = 0.2F * up;
    }

    /** The pounce: a crouch, then the body stretched long through the leap, forelegs out, jaw open. */
    private void pounce(float t) {
        float crouch = AnimMath.ss(0.0F, 3.0F, t) * (1.0F - AnimMath.ss(3.0F, 5.0F, t));
        float leap = AnimMath.ss(3.0F, 6.0F, t) * (1.0F - AnimMath.ss(FenrisEntity.POUNCE_TICKS - 3.0F, FenrisEntity.POUNCE_TICKS, t));
        body.xRot = 0.15F * crouch - 0.2F * leap;
        ruff.xRot = body.xRot;
        legs[0].xRot = 0.6F * crouch - 1.3F * leap;
        legs[1].xRot = 0.6F * crouch - 1.3F * leap;
        legs[2].xRot = -0.5F * crouch + 1.1F * leap;
        legs[3].xRot = -0.5F * crouch + 1.1F * leap;
        jaw.xRot = 0.05F + 0.55F * leap;
        head.xRot = -0.25F * leap;
    }

    /** The bite: the jaw wide and snapped shut, the head lunging down and in. */
    private void bite(float t) {
        float open = AnimMath.ss(0.0F, 2.0F, t) * (1.0F - AnimMath.ss(3.0F, 4.0F, t));
        jaw.xRot = 0.05F + 0.8F * open;
        head.xRot = 0.3F * AnimMath.ss(2.0F, 4.0F, t) * (1.0F - AnimMath.ss(5.0F, FenrisEntity.BITE_TICKS, t)) - 0.2F * open;
    }
}
