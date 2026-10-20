package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A suit display's workshop gantry (3.8, PLAN 7.1, 7.2): two steel uprights behind the suit and a beam with hazard
 * stripes, and two yellow robot arms hanging from the beam. Folded up while nobody suits up; when a Mark 3 suit-up runs
 * within reach ({@link SuitFx#gantryOf}), each arm reaches for the pieces going on on its side (the middle pieces for
 * both), telescoping to reach, its screwdriver head spinning as it bolts them on. Drawn in the display's model space
 * (pixels, y down, the floor at 24, the suit facing z-).
 */
final class GantryArms {
    static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/iron_man/suit_gantry.png");
    /** The beam's height (pixels above the floor) and the arms' mounts on it. */
    private static final float BEAM_Y = 24.0F - 30.0F;
    private static final float BACK_Z = 6.0F;
    private static final float MOUNT_X = 5.0F;
    private static final float MIN_LINK = 7.0F;
    private final ModelPart upright;
    private final ModelPart beam;
    private final ModelPart link;
    private final ModelPart joint;
    private final ModelPart head;

    GantryArms(ModelPart root) {
        upright = root.getChild("upright");
        beam = root.getChild("beam");
        link = root.getChild("link");
        joint = root.getChild("joint");
        head = root.getChild("head");
    }

    /**
     * The frame and the arms. {@code toModel} turns world positions into this space; {@code served} is the player
     * whose Mark 3 suit-up this gantry serves, or null.
     */
    void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, Matrix4f toModel,
                @Nullable Player served, float partialTick, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (float x : new float[] {-7.0F, 7.0F}) {
            poseStack.pushPose();
            poseStack.translate(x / 16.0F, 24.0F / 16.0F, BACK_Z / 16.0F);
            upright.render(poseStack, vc, light, overlay);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, BEAM_Y / 16.0F, BACK_Z / 16.0F);
        beam.render(poseStack, vc, light, overlay);
        poseStack.popPose();
        for (int side = -1; side <= 1; side += 2) {
            Vector3f mount = new Vector3f(side * MOUNT_X, BEAM_Y + 1.5F, BACK_Z - 1.0F);
            Vector3f rest = new Vector3f(side * (MOUNT_X + 1.5F), BEAM_Y + 11.0F, BACK_Z - 2.5F);
            Vector3f target = rest;
            float busy = 0.0F;
            if (served != null) {
                Vector3f[] reach = reach(served, side, toModel, partialTick);
                if (reach != null) {
                    target = reach[0];
                    busy = reach[1].x;
                }
            }
            arm(poseStack, vc, light, overlay, mount, target, side, busy, time);
        }
    }

    /**
     * Where an arm of this side reaches now: the pieces going on on its side (and the middle ones), each weighted by
     * how far through its window it is, blended with the folded rest; {@code [1].x} how busy it is.
     */
    @Nullable
    private static Vector3f[] reach(Player p, int side, Matrix4f toModel, float partialTick) {
        IronManState state = IronSuits.state(p);
        if (!state.inPhase() || state.phaseKind() != SuitKind.MARK_3 || state.phase() != IronManState.UP) {
            return null;
        }
        float progress = SuitTimelines.progress(p.level().getGameTime(), state.phaseStart(), state.phaseTicks(),
                partialTick);
        Vector3f sum = new Vector3f();
        float weight = 0.0F;
        for (SuitParts part : SuitParts.VALUES) {
            // the model's x- is the wearer's right; the side -1 arm takes the right side's pieces
            boolean mine = side < 0 ? !part.left() : !part.right();
            if (!mine) {
                continue;
            }
            float r = SuitTimelines.reveal(SuitKind.MARK_3, part, progress, true);
            if (r <= 0.0F || r >= 1.0F) {
                continue;
            }
            float w = Mth.sin(r * Mth.PI);
            Vec3 at = SuitFx.where(p, part);
            Vector3f local = toModel.transformPosition(new Vector3f((float) at.x, (float) at.y, (float) at.z));
            sum.add(local.mul(w));
            weight += w;
        }
        if (weight <= 0.001F) {
            return null;
        }
        sum.div(weight);
        return new Vector3f[] {sum, new Vector3f(Math.min(1.0F, weight), 0.0F, 0.0F)};
    }

    /** One arm: two telescoping links from the mount to the target, the elbow out and back, the head spinning. */
    private void arm(PoseStack poseStack, VertexConsumer vc, int light, int overlay, Vector3f mount, Vector3f target,
                     int side, float busy, float time) {
        Vector3f toT = new Vector3f(target).sub(mount);
        float d = Math.max(0.5F, toT.length());
        float length = Math.max(MIN_LINK, d * 0.53F);
        float half = d * 0.5F;
        float rise = (float) Math.sqrt(Math.max(0.0F, length * length - half * half));
        Vector3f dir = new Vector3f(toT).div(d);
        // the elbow bends out to its side and back, made square to the reach
        Vector3f bend = new Vector3f(side * 0.8F, -0.2F, 0.6F);
        bend.sub(new Vector3f(dir).mul(bend.dot(dir)));
        if (bend.lengthSquared() < 1.0E-4F) {
            bend.set(0.0F, 0.0F, 1.0F);
        }
        bend.normalize();
        Vector3f elbow = new Vector3f(mount).add(new Vector3f(toT).mul(0.5F)).add(bend.mul(rise));
        segment(poseStack, vc, light, overlay, mount, elbow);
        segment(poseStack, vc, light, overlay, elbow, target);
        place(poseStack, mount, () -> joint.render(poseStack, vc, light, overlay));
        place(poseStack, elbow, () -> joint.render(poseStack, vc, light, overlay));
        Vector3f along = new Vector3f(target).sub(elbow).normalize();
        poseStack.pushPose();
        poseStack.translate(target.x / 16.0F, target.y / 16.0F, target.z / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, along.x, along.y, along.z));
        poseStack.translate(0.0F, -2.0F / 16.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(busy > 0.05F ? time * 1.6F : 0.0F));
        head.render(poseStack, vc, light, overlay);
        poseStack.popPose();
    }

    /** A link from a to b (the unit link is 16 pixels along y). */
    private void segment(PoseStack poseStack, VertexConsumer vc, int light, int overlay, Vector3f a, Vector3f b) {
        Vector3f ab = new Vector3f(b).sub(a);
        float len = ab.length();
        if (len < 0.01F) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(a.x / 16.0F, a.y / 16.0F, a.z / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, ab.x / len, ab.y / len, ab.z / len));
        poseStack.scale(1.0F, len / 16.0F, 1.0F);
        link.render(poseStack, vc, light, overlay);
        poseStack.popPose();
    }

    private static void place(PoseStack poseStack, Vector3f at, Runnable draw) {
        poseStack.pushPose();
        poseStack.translate(at.x / 16.0F, at.y / 16.0F, at.z / 16.0F);
        draw.run();
        poseStack.popPose();
    }
}
