package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws Thor's weapon in his hand, from numbers instead of the item model's display transform: the handle really passes
 * through the fist, in every pose. {@link GripMath} has the placement; this class builds it on the pose stack (in the
 * hand frame vanilla's {@code ItemInHandLayer} has built), draws the item, remembers where its head is for the effects,
 * and draws what a whirling weapon leaves behind it: the strap it hangs on and a fading sweep of light.
 *
 * <p>Called by {@link com.kolodkingal.enhanced.mixin.client.ItemInHandLayerMixin}. It draws only for a hero Thor, in the
 * third-person hand contexts; everyone else (and first person) gets vanilla's drawing with the item models' own transforms,
 * which were fixed to the same resting grip.
 */
public final class ThorGrip {
    private static final float RAD = Mth.DEG_TO_RAD;
    /** The strap of Mjolnir: dark leather. */
    private static final int LEATHER = 0xFF6B4A2B;
    private static final int LIGHT = 0x4DB8FF;
    private static final int SWOOSH_SLICES = 8;
    private static final float SWOOSH_TRAIL = 80.0F;

    /** Draws the weapon and returns true, or returns false to let vanilla draw it. */
    public static boolean render(ItemInHandRenderer renderer, LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                 boolean left, PoseStack ps, MultiBufferSource buffer, int light, Object model) {
        if (!(entity instanceof AbstractClientPlayer player) || !(model instanceof PlayerModel<?> playerModel)
                || (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                || !EnhancedClientConfig.animations() || !ThorAbilities.isWeapon(stack)) {
            return false;
        }
        HeroAnimState state = HeroAnimator.peek(player);
        // (3.4: a custom hero with Thor's hammer holds the weapon as Thor does.)
        if (state == null || state.power != Power.THOR && !Powers.get(player).has(HeroAbility.THOR_HAMMER) || state.wVanilla > 0.0F) {
            return false;
        }
        GripMath.Kind kind = ThorAbilities.isStormbreaker(stack) ? GripMath.Kind.STORMBREAKER : GripMath.Kind.MJOLNIR;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        // A model that was not posed from the state (the inventory's preview) gets the resting grip: nothing of the flight.
        boolean posed = state.posed;
        // The arm's world elevation: 0 hangs, 90 level forward, 180 straight up.
        float xRot = (arm == HumanoidArm.RIGHT ? playerModel.rightArm : playerModel.leftArm).xRot;
        float phi = -xRot / RAD - (posed ? state.renderPitch : 0.0F);
        float lead = GripMath.leadWeight(phi, posed ? state.leadForce : 0.0F);
        float beta = GripMath.beta(lead);
        float whirl = posed ? state.wSpin : 0.0F;
        GripMath.Placement p = GripMath.placement(kind, left, slim, beta, whirl, state.spinAngle);

        ps.pushPose();
        ps.translate(p.position.x / 16.0F, p.position.y / 16.0F, p.position.z / 16.0F);
        ps.mulPose(p.rotation);
        ps.scale(p.scale, p.scale, p.scale);
        ps.translate(-(p.held.x - 8.0F) / 16.0F, -(p.held.y - 8.0F) / 16.0F, -(p.held.z - 8.0F) / 16.0F);
        renderer.renderItem(entity, stack, ItemDisplayContext.NONE, left, ps, buffer, light);
        // Only the weapon the arm of the poses holds is the muzzle of the beam, not the one in his other hand.
        if (posed && arm == weaponArm(player)) {
            HandTracker.recordWeapon(player, (float) state.time, ps, kind.head);
        }
        ps.popPose();

        if (whirl > 0.02F) {
            if (kind.gap > 0.0F) {
                strap(ps, buffer, p);
            }
            swoosh(ps, buffer, kind, left, state.spinAngle, whirl);
        }
        return true;
    }

    /**
     * The arm of the weapon the poses are built around: of the main hand's weapon, else of the off hand's (see
     * {@link PoseContext}).
     */
    private static HumanoidArm weaponArm(AbstractClientPlayer player) {
        return ThorAbilities.weaponHand(player) == InteractionHand.OFF_HAND ? player.getMainArm().getOpposite() : player.getMainArm();
    }

    /** The leather strap between the fist's pivot and the hammer's own strap: a thin cross of two quads. */
    private static void strap(PoseStack ps, MultiBufferSource buffer, GripMath.Placement p) {
        Vector3f dir = new Vector3f(p.position).sub(p.pivot);
        float length = dir.length();
        if (length < 0.05F) {
            return;
        }
        dir.div(length);
        Vector3f perp1 = dir.cross(new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f());
        if (perp1.lengthSquared() < 1.0E-4F) {
            perp1 = dir.cross(new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f());
        }
        perp1.normalize().mul(0.25F);
        Vector3f perp2 = dir.cross(perp1, new Vector3f()).normalize().mul(0.25F);
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        Matrix4f m = ps.last().pose();
        quad(vc, m, p.pivot, p.position, perp1, LEATHER);
        quad(vc, m, p.pivot, p.position, perp2, LEATHER);
    }

    /**
     * A fan of light behind the whirling head, in the plane it sweeps: nine slices from the head back along its path,
     * fading to nothing, with a thin white leading edge.
     */
    private static void swoosh(PoseStack ps, MultiBufferSource buffer, GripMath.Kind kind, boolean left, float spin, float whirl) {
        float m = left ? -1.0F : 1.0F;
        float trail = SWOOSH_TRAIL * whirl;
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        Matrix4f pose = ps.last().pose();
        Vector3f innerA = new Vector3f();
        Vector3f outerA = new Vector3f();
        Vector3f innerB = new Vector3f();
        Vector3f outerB = new Vector3f();
        for (int k = 0; k < SWOOSH_SLICES; k++) {
            float thetaA = spin - m * trail * k / SWOOSH_SLICES;
            float thetaB = spin - m * trail * (k + 1) / SWOOSH_SLICES;
            disk(innerA, m, thetaA, kind.swooshInner);
            disk(outerA, m, thetaA, kind.swooshOuter);
            disk(innerB, m, thetaB, kind.swooshInner);
            disk(outerB, m, thetaB, kind.swooshOuter);
            float fade = (float) Math.pow(1.0F - k / (float) SWOOSH_SLICES, 1.6);
            int colorA = EnergyGeometry.argb(LIGHT, 0.50F * fade * whirl);
            int colorB = EnergyGeometry.argb(LIGHT, 0.50F * (float) Math.pow(1.0F - (k + 1) / (float) SWOOSH_SLICES, 1.6) * whirl);
            vertex(vc, pose, innerA, colorA);
            vertex(vc, pose, outerA, colorA);
            vertex(vc, pose, outerB, colorB);
            vertex(vc, pose, innerB, colorB);
        }
        // The bright leading edge.
        disk(innerA, m, spin, kind.swooshInner);
        disk(outerA, m, spin, kind.swooshOuter);
        disk(innerB, m, spin - m * 6.0F, kind.swooshInner);
        disk(outerB, m, spin - m * 6.0F, kind.swooshOuter);
        int edge = EnergyGeometry.argb(0xFFFFFF, 0.9F * whirl);
        int fadeEdge = EnergyGeometry.argb(0xFFFFFF, 0.0F);
        vertex(vc, pose, innerA, edge);
        vertex(vc, pose, outerA, edge);
        vertex(vc, pose, outerB, fadeEdge);
        vertex(vc, pose, innerB, fadeEdge);
    }

    /** A point of the whirl's disc: the centre of the whirl, then out {@code r} pixels at angle {@code theta}. */
    private static void disk(Vector3f out, float m, float thetaDeg, float r) {
        out.set(0.0F, r, 0.0F).rotateZ(-m * thetaDeg * RAD).rotateY(m * GripMath.TILT * RAD).add(GripMath.WHIRL);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Vector3f px, int argb) {
        vc.addVertex(pose, px.x / 16.0F, px.y / 16.0F, px.z / 16.0F).setColor(argb);
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, Vector3f a, Vector3f b, Vector3f side, int argb) {
        vc.addVertex(pose, (a.x + side.x) / 16.0F, (a.y + side.y) / 16.0F, (a.z + side.z) / 16.0F).setColor(argb);
        vc.addVertex(pose, (a.x - side.x) / 16.0F, (a.y - side.y) / 16.0F, (a.z - side.z) / 16.0F).setColor(argb);
        vc.addVertex(pose, (b.x - side.x) / 16.0F, (b.y - side.y) / 16.0F, (b.z - side.z) / 16.0F).setColor(argb);
        vc.addVertex(pose, (b.x + side.x) / 16.0F, (b.y + side.y) / 16.0F, (b.z + side.z) / 16.0F).setColor(argb);
    }

    private ThorGrip() {
    }
}
