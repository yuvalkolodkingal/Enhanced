package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Hand-made body language for the heroes, applied on top of the vanilla animation every frame:
 * <ul>
 *     <li>Scarlet Witch: both hands thrust forward and trembling while the Chaos Beam pours out; arms open and
 *     swaying, legs together, while she floats; one hand raised and twitching while she holds something.</li>
 *     <li>Magneto: an outstretched, steady hand aimed at whatever he controls or shoots; regal, arms-low hover.</li>
 *     <li>Quicksilver: leaning into the run with exaggerated, blurring arm swings.</li>
 *     <li>Vision: upright, composed hover; arms braced while dense, pulled back while the Mind Stone fires, open and
 *     weightless while phased.</li>
 *     <li>Loki: a throwing arm after each dagger, a raised open hand as he vanishes or changes shape, and arms held
 *     slightly out in his Frost Giant form.</li>
 *     <li>Billy: his casting hand thrust forward for a Hex Bolt; both hands raised and trembling while he holds creatures
 *     spellbound; calm and weightless in flight, hands open.</li>
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
                Vec3 caught = data.magneticShield() ? nearestHeldProjectile(player) : null;
                if (caught != null && !holding && !data.shrapnelActive()) {
                    // Holding a projectile still in mid-air: the hand points at it.
                    pointAt(main, player, caught, t);
                    off.zRot += 0.15F * -side;
                } else if (holding || data.shrapnelActive()) {
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
                } else if (data.beamActive()) {
                    // The beam comes from the stone in his forehead: arms pulled back, chest out, a faint strain.
                    model.rightArm.xRot = 0.4F + Mth.sin(t * 1.7F) * 0.02F;
                    model.leftArm.xRot = 0.4F + Mth.cos(t * 1.7F) * 0.02F;
                    model.rightArm.zRot = 0.35F;
                    model.leftArm.zRot = -0.35F;
                } else if (data.visionPhased()) {
                    // Serene and weightless: open palms slightly forward.
                    model.rightArm.xRot = -0.35F + Mth.sin(t * 0.05F) * 0.05F;
                    model.leftArm.xRot = -0.35F + Mth.cos(t * 0.05F) * 0.05F;
                    model.rightArm.zRot = 0.18F;
                    model.leftArm.zRot = -0.18F;
                } else if (airborne) {
                    model.rightArm.xRot = 0.05F;
                    model.leftArm.xRot = 0.05F;
                    model.rightArm.zRot = 0.08F + Mth.sin(t * 0.05F) * 0.03F;
                    model.leftArm.zRot = -0.08F - Mth.sin(t * 0.05F) * 0.03F;
                }
            }
            case TOMMY -> {
                if (data.is(Live.SPEEDING)) {
                    lean(model, 0.3F);
                    float swing = Mth.cos(limbSwing * 0.6662F * 1.4F) * 1.5F * Math.max(limbSwingAmount, 0.6F);
                    model.rightArm.xRot = swing + 0.3F;
                    model.leftArm.xRot = -swing + 0.3F;
                    model.rightArm.zRot = 0.1F;
                    model.leftArm.zRot = -0.1F;
                } else if (data.is(Live.WHIRLWIND)) {
                    // The real Tommy is a blur in the eye of the storm: arms out, spinning.
                    model.rightArm.zRot = 1.3F + Mth.sin(t * 0.8F) * 0.1F;
                    model.leftArm.zRot = -1.3F - Mth.sin(t * 0.8F) * 0.1F;
                    model.rightArm.xRot = 0.0F;
                    model.leftArm.xRot = 0.0F;
                }
            }
            case THOR -> {
                Entity hammer = data.hammerEntityId() != -1 ? player.level().getEntity(data.hammerEntityId()) : null;
                if (data.is(Live.CALLING_LIGHTNING)) {
                    // His weapon raised to the sky, catching the lightning.
                    main.xRot = -Mth.PI + 0.1F + Mth.sin(t * 2.1F) * 0.03F;
                    main.yRot = 0.0F;
                    main.zRot = 0.1F * side;
                    off.zRot += 0.25F * -side;
                } else if (data.is(Live.BIFROST)) {
                    model.rightArm.xRot = -0.3F;
                    model.leftArm.xRot = -0.3F;
                    model.rightArm.zRot = 0.45F;
                    model.leftArm.zRot = -0.45F;
                } else if (hammer instanceof ThrownWeaponEntity flying && flying.isReturning()
                        && flying.distanceToSqr(player) < 20 * 20) {
                    // Hand out, waiting for his weapon to come home.
                    pointAt(main, player, flying.position(), t);
                    off.zRot += 0.15F * -side;
                } else if (airborne) {
                    // Flying behind his weapon: hovering, it is held up to the sky; at speed the arm swings forward to lead
                    // the way. The other arm trails back.
                    double dx = player.getX() - player.xOld;
                    double dz = player.getZ() - player.zOld;
                    float speed = Mth.clamp((float) Math.sqrt(dx * dx + dz * dz) / 0.6F, 0.0F, 1.0F);
                    main.xRot = Mth.lerp(speed, -2.75F, -1.85F) + Mth.sin(t * 0.2F) * 0.04F;
                    main.yRot = 0.0F;
                    main.zRot = 0.0F;
                    off.xRot = 0.35F;
                    off.zRot = 0.12F * -side;
                } else if (data.toggle(Toggle.GOD_OF_THUNDER)) {
                    // Unleashed: arms held away from the body, crackling.
                    model.rightArm.zRot += 0.22F + Mth.sin(t * 1.9F) * 0.03F;
                    model.leftArm.zRot -= 0.22F + Mth.cos(t * 1.7F) * 0.03F;
                }
            }
            case LOKI -> {
                if (data.is(Live.THROWING)) {
                    // The follow-through of a throw: the arm swept forward from over the shoulder.
                    main.xRot = -1.7F + model.head.xRot * 0.6F;
                    main.yRot = model.head.yRot * 0.5F - 0.15F * side;
                    main.zRot = 0.0F;
                } else if (data.is(Live.CASTING)) {
                    // A hand raised, fingers spread, as the spell is cast.
                    main.xRot = -2.5F + Mth.sin(t * 0.9F) * 0.04F;
                    main.yRot = -0.25F * side;
                    main.zRot = 0.3F * side;
                }
                if (LokiAbilities.isFrostGiant(player)) {
                    // The Jotun stance: arms held slightly out from the body.
                    model.rightArm.zRot += 0.2F;
                    model.leftArm.zRot -= 0.2F;
                }
            }
            case BILLY -> {
                if (data.is(Live.FREEZING)) {
                    // Both hands raised towards the creatures he holds, trembling with the effort.
                    model.rightArm.xRot = -2.2F + model.head.xRot * 0.5F + Mth.sin(t * 1.9F) * 0.06F;
                    model.leftArm.xRot = -2.2F + model.head.xRot * 0.5F + Mth.cos(t * 2.3F) * 0.06F;
                    model.rightArm.yRot = model.head.yRot - 0.2F;
                    model.leftArm.yRot = model.head.yRot + 0.2F;
                    model.rightArm.zRot = 0.2F + Mth.sin(t * 2.7F) * 0.04F;
                    model.leftArm.zRot = -0.2F - Mth.cos(t * 2.5F) * 0.04F;
                } else if (data.is(Live.CASTING)) {
                    aim(model, main, -0.05F * side, t, 0.02F);
                    off.zRot += 0.25F * -side;
                } else if (holding) {
                    aim(model, main, -0.1F * side, t, 0.0F);
                    main.xRot -= 0.3F;
                    main.zRot += Mth.sin(t * 0.9F) * 0.05F;
                } else if (airborne) {
                    // Calm and weightless: arms a little away from the body, palms open.
                    model.rightArm.xRot = -0.3F + Mth.sin(t * 0.07F) * 0.06F;
                    model.leftArm.xRot = -0.3F + Mth.cos(t * 0.07F) * 0.06F;
                    model.rightArm.yRot = -0.25F;
                    model.leftArm.yRot = 0.25F;
                    model.rightArm.zRot = 0.4F + Mth.sin(t * 0.09F) * 0.05F;
                    model.leftArm.zRot = -0.4F - Mth.sin(t * 0.09F) * 0.05F;
                }
            }
            case NONE -> {
            }
        }
        sync(model);
    }

    /** The nearest projectile hanging in this Magneto's field, or null. */
    private static Vec3 nearestHeldProjectile(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (Projectile p : player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(MagnetoAbilities.SHIELD_RADIUS + 2.0),
                p -> p.isNoGravity() && p.getDeltaMovement().lengthSqr() < 1.0E-6 && MagnetoAbilities.isMetalProjectile(p))) {
            double d = p.position().distanceToSqr(eye);
            if (d < bestDist) {
                bestDist = d;
                best = p.position();
            }
        }
        return best;
    }

    /** Points an arm at a spot in the world, relative to the body's facing, with a faint strain. */
    private static void pointAt(ModelPart arm, Player player, Vec3 target, float t) {
        Vec3 d = target.subtract(player.getEyePosition());
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        float relYaw = Mth.wrapDegrees(yaw - player.yBodyRot);
        arm.xRot = -Mth.HALF_PI + pitch * Mth.DEG_TO_RAD + Mth.sin(t * 1.7F) * 0.02F;
        arm.yRot = Mth.clamp(relYaw, -80.0F, 80.0F) * Mth.DEG_TO_RAD;
        arm.zRot = 0.0F;
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
