package com.kolodkingal.enhanced.client.hydra;

import com.kolodkingal.enhanced.hydra.HydraSoldierEntity;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity.Act;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity.Arms;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The body of a Hydra soldier (3.6, lane L8): the player model (wide arms), posed after the vanilla walk and swing by
 * what he does ({@link Act}, synced) and what he carries: the rifle at low ready on patrol, shouldered with both arms
 * forward when he aims (a kick at each shot), the off hand at the cell when he reloads, a hand to his ear at the
 * radio, arms pumping when he runs, the riot shield up, the officer's pistol in one hand. A change of pose eases over
 * a quarter of a second ({@link HydraClient#actWeight}). Never a raised-arm salute.
 */
public class HydraSoldierModel extends PlayerModel<HydraSoldierEntity> {
    private float partialTick;

    /** One arm's angles. */
    private record Arm(float x, float y, float z) {
        static final Arm FREE = new Arm(Float.NaN, Float.NaN, Float.NaN);

        boolean free() {
            return Float.isNaN(x);
        }
    }

    /** Both arms' angles in a pose (FREE: leave the vanilla walk). */
    private record Pose(Arm right, Arm left) {
    }

    public HydraSoldierModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void prepareMobModel(HydraSoldierEntity soldier, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(soldier, limbSwing, limbSwingAmount, partialTick);
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(HydraSoldierEntity soldier, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(soldier, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (attackTime > 0.0F && soldier.arms() == Arms.BATON) {
            return; // the baton's blow is the vanilla swing
        }
        float w = HydraClient.actWeight(soldier, partialTick);
        Pose now = pose(soldier, soldier.act(), limbSwing, limbSwingAmount, ageInTicks);
        Pose before = pose(soldier, HydraClient.previousAct(soldier), limbSwing, limbSwingAmount, ageInTicks);
        apply(rightArm, before.right(), now.right(), w);
        apply(leftArm, before.left(), now.left(), w);
        float kick = HydraClient.recoil(soldier, partialTick);
        if (kick > 0.0F) {
            rightArm.xRot -= kick * 0.22F;
            if (soldier.arms() != Arms.PISTOL) {
                leftArm.xRot -= kick * 0.18F;
            }
        }
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
    }

    private static void apply(ModelPart part, Arm from, Arm to, float w) {
        float bx = part.xRot;
        float by = part.yRot;
        float bz = part.zRot;
        float fx = from.free() ? bx : from.x();
        float fy = from.free() ? by : from.y();
        float fz = from.free() ? bz : from.z();
        float tx = to.free() ? bx : to.x();
        float ty = to.free() ? by : to.y();
        float tz = to.free() ? bz : to.z();
        part.xRot = Mth.lerp(w, fx, tx);
        part.yRot = Mth.lerp(w, fy, ty);
        part.zRot = Mth.lerp(w, fz, tz);
    }

    /** The arms of a pose for this soldier. */
    private Pose pose(HydraSoldierEntity soldier, Act act, float limbSwing, float limbSwingAmount, float age) {
        Arms arms = soldier.arms();
        float sway = Mth.cos(limbSwing * 0.6662F) * 0.12F * Math.min(1.0F, limbSwingAmount);
        float breath = Mth.sin(age * 0.067F) * 0.03F;
        float aimX = -Mth.HALF_PI + head.xRot;
        float aimY = head.yRot;
        if (arms == Arms.BATON) {
            Arm shieldDown = new Arm(-0.35F + sway, 0.25F, 0.05F);
            return switch (act) {
                case SHIELD -> new Pose(Arm.FREE, new Arm(-1.35F + head.xRot * 0.5F, 0.65F + aimY * 0.5F, 0.0F));
                case RADIO -> new Pose(Arm.FREE, radioArm(age));
                default -> new Pose(Arm.FREE, shieldDown);
            };
        }
        if (arms == Arms.PISTOL) {
            return switch (act) {
                case AIM -> new Pose(new Arm(aimX, aimY - 0.05F, 0.0F), Arm.FREE);
                case RADIO -> new Pose(Arm.FREE, radioArm(age));
                case RUN -> new Pose(new Arm(-0.4F + sway * 2.0F, -0.1F, 0.0F), Arm.FREE);
                default -> new Pose(new Arm(-0.15F + sway + breath, -0.05F, 0.0F), Arm.FREE);
            };
        }
        // The rifles: low ready, shouldered, reloading.
        Arm lowRight = new Arm(-0.55F + sway * 0.5F + breath, -0.2F, 0.0F);
        Arm lowLeft = new Arm(-0.85F + sway * 0.5F + breath, 0.55F, 0.0F);
        return switch (act) {
            case AIM -> new Pose(new Arm(aimX, aimY - 0.1F, 0.0F), new Arm(aimX + 0.05F, aimY + 0.45F, 0.0F));
            case RELOAD -> new Pose(new Arm(-0.95F, -0.3F, 0.0F),
                    new Arm(-1.05F + Mth.sin(age * 0.6F) * 0.12F, 0.75F, 0.1F));
            case RADIO -> new Pose(lowRight, radioArm(age));
            case RUN -> new Pose(new Arm(-0.7F + sway * 1.6F, -0.25F, 0.0F),
                    new Arm(-0.95F + sway * 1.6F, 0.55F, 0.0F));
            case ALERT -> new Pose(new Arm(-0.9F + breath, -0.15F + aimY * 0.5F, 0.0F),
                    new Arm(-1.15F + breath, 0.5F + aimY * 0.5F, 0.0F));
            default -> new Pose(lowRight, lowLeft);
        };
    }

    /** The left hand at his ear (the radio): the elbow out, never above the head. */
    private static Arm radioArm(float age) {
        return new Arm(-2.35F + Mth.sin(age * 0.2F) * 0.03F, 0.15F, -0.75F);
    }
}
