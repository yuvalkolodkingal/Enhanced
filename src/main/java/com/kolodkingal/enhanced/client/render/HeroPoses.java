package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Hand-made body language for the heroes, applied on top of the vanilla animation every frame:
 * <ul>
 *     <li>Scarlet Witch: both hands thrust forward and trembling while the Chaos Beam pours out; arms open and
 *     swaying, legs together, while she floats; one hand raised and twitching while she holds something.</li>
 *     <li>Magneto: an outstretched, steady hand aimed at whatever he controls or shoots; regal, arms-low hover.</li>
 *     <li>Quicksilver: leaning into the run with exaggerated, blurring arm swings.</li>
 *     <li>Vision: upright, composed hover; arms braced while dense.</li>
 *     <li>Photon: a fist forward like a superhero in flight.</li>
 * </ul>
 */
public final class HeroPoses {

    public static void apply(PlayerModel<?> model, LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
        // The first-person hand is drawn with an all-zero animation call: leave it alone.
        if (!(entity instanceof Player player) || ageInTicks == 0.0F) {
            return;
        }
        PowerData data = Powers.get(player);
        Power power = data.power();
        if (!power.hasPowers()) {
            return;
        }
        boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
        ModelPart main = rightHanded ? model.rightArm : model.leftArm;
        ModelPart off = rightHanded ? model.leftArm : model.rightArm;
        float side = rightHanded ? 1F : -1F;
        boolean airborne = data.flying() && !player.onGround();
        boolean holding = data.heldEntityId() != -1;
        float t = ageInTicks;

        if (airborne) {
            hover(model, t, power);
        }

        switch (power) {
            case SCARLET_WITCH -> {
                if (data.beamActive()) {
                    aim(model, model.rightArm, -0.18F, t, 0.05F);
                    aim(model, model.leftArm, 0.18F, t + 7, 0.05F);
                } else if (holding) {
                    aim(model, main, -0.1F * side, t, 0.0F);
                    main.xRot -= 0.35F;
                    main.zRot += Mth.sin(t * 0.9F) * 0.06F;
                    off.zRot += 0.3F * side * -1;
                } else if (data.chaosShieldActive()) {
                    model.rightArm.xRot = -1.1F + Mth.sin(t * 0.3F) * 0.05F;
                    model.leftArm.xRot = -1.1F + Mth.cos(t * 0.3F) * 0.05F;
                    model.rightArm.zRot = 0.55F;
                    model.leftArm.zRot = -0.55F;
                } else if (airborne) {
                    // Arms open, palms glowing, slowly drifting.
                    model.rightArm.xRot = -0.25F + Mth.sin(t * 0.08F) * 0.08F;
                    model.leftArm.xRot = -0.25F + Mth.cos(t * 0.08F) * 0.08F;
                    model.rightArm.zRot = 0.55F + Mth.sin(t * 0.11F) * 0.06F;
                    model.leftArm.zRot = -0.55F - Mth.sin(t * 0.11F) * 0.06F;
                }
            }
            case MAGNETO -> {
                if (holding || data.shrapnelActive()) {
                    aim(model, main, -0.05F * side, t, data.shrapnelActive() ? 0.03F : 0.0F);
                    off.zRot += 0.15F * -side;
                } else if (airborne) {
                    model.rightArm.xRot = -0.1F;
                    model.leftArm.xRot = -0.1F;
                    model.rightArm.zRot = 0.3F;
                    model.leftArm.zRot = -0.3F;
                }
            }
            case QUICKSILVER -> {
                if (data.slowMo()) {
                    lean(model, 0.35F);
                    // Pumping arms, bigger and faster than a normal sprint.
                    float swing = Mth.cos(limbSwing * 0.6662F * 1.4F) * 1.6F * Math.max(limbSwingAmount, 0.6F);
                    model.rightArm.xRot = swing + 0.3F;
                    model.leftArm.xRot = -swing + 0.3F;
                    model.rightArm.zRot = 0.1F;
                    model.leftArm.zRot = -0.1F;
                }
            }
            case VISION -> {
                if (data.dense()) {
                    model.rightArm.zRot = 0.25F;
                    model.leftArm.zRot = -0.25F;
                    model.rightArm.xRot = -0.15F;
                    model.leftArm.xRot = -0.15F;
                } else if (airborne && !data.beamActive()) {
                    model.rightArm.xRot = 0.05F;
                    model.leftArm.xRot = 0.05F;
                    model.rightArm.zRot = 0.08F + Mth.sin(t * 0.05F) * 0.03F;
                    model.leftArm.zRot = -0.08F - Mth.sin(t * 0.05F) * 0.03F;
                }
            }
            case PHOTON -> {
                if (airborne) {
                    main.xRot = -2.6F;
                    main.zRot = 0.0F;
                    off.xRot = 0.3F;
                    off.zRot = 0.15F * -side;
                }
            }
            case NONE -> {
            }
        }
        sync(model);
    }

    /** Points an arm along the head's direction, with an optional tremble. */
    private static void aim(PlayerModel<?> model, ModelPart arm, float yaw, float t, float tremble) {
        arm.xRot = -Mth.HALF_PI + model.head.xRot + Mth.sin(t * 1.7F) * tremble;
        arm.yRot = model.head.yRot + yaw;
        arm.zRot = Mth.cos(t * 1.3F) * tremble;
    }

    /** Floating instead of walking: legs together and gently swaying. */
    private static void hover(PlayerModel<?> model, float t, Power power) {
        float sway = Mth.sin(t * 0.07F) * 0.06F;
        model.rightLeg.xRot = 0.12F + sway;
        model.leftLeg.xRot = 0.08F + sway;
        model.rightLeg.zRot = 0.03F;
        model.leftLeg.zRot = -0.03F;
        model.rightLeg.yRot = 0.0F;
        model.leftLeg.yRot = 0.0F;
        if (power != Power.QUICKSILVER) {
            model.rightArm.xRot = Mth.sin(t * 0.06F) * 0.05F;
            model.leftArm.xRot = -Mth.sin(t * 0.06F) * 0.05F;
        }
    }

    /** Leans the whole body forward (a lighter version of vanilla's crouch geometry). */
    private static void lean(PlayerModel<?> model, float amount) {
        float k = amount / 0.5F;
        model.body.xRot = amount;
        model.body.y = 3.2F * k;
        model.head.y = 4.2F * k;
        model.rightArm.y = 2.0F + 3.2F * k;
        model.leftArm.y = 2.0F + 3.2F * k;
        model.rightLeg.z = 4.0F * k;
        model.leftLeg.z = 4.0F * k;
        model.rightLeg.y = 12.0F + 0.2F * k;
        model.leftLeg.y = 12.0F + 0.2F * k;
    }

    /** Overlay parts (hat, sleeves, jacket, trousers) follow the parts they cover. */
    private static void sync(PlayerModel<?> model) {
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    private HeroPoses() {
    }
}
