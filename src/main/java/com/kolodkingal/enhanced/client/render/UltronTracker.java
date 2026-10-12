package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Where the fingertips, the palms and the middle of the chest of an Ultron body really are this frame, as world
 * positions, so that a beam leaves the finger it is drawn from whatever the pose. The layer ({@link Layer}) records them
 * from the pose stack the body is drawn with, as {@link HandTracker} does for the heroes; a point that was not drawn
 * lately (the body is out of sight) is null, and the caller keeps its own estimate.
 */
public final class UltronTracker {
    /** The fingertips: five on the right hand (thumb side first), then five on the left. */
    public static final int FINGERS = 10;
    private static final float FRESH_TICKS = 3.0F;

    private static final class Points {
        float age = Float.NEGATIVE_INFINITY;
        final Vec3[] fingers = new Vec3[FINGERS];
        Vec3 palmRight;
        Vec3 palmLeft;
        Vec3 chest;
    }

    private static final Map<Integer, Points> POINTS = new HashMap<>();

    /** The ten fingertips, or null if the body was not drawn lately. */
    @Nullable
    public static Vec3[] fingertips(Entity entity, float partialTick) {
        Points p = fresh(entity, partialTick);
        return p == null ? null : p.fingers;
    }

    /** The middle of a palm (the right one, or the left), or null. */
    @Nullable
    public static Vec3 palm(Entity entity, boolean right, float partialTick) {
        Points p = fresh(entity, partialTick);
        return p == null ? null : right ? p.palmRight : p.palmLeft;
    }

    /** The middle of the chest, where the light in his torso is, or null. */
    @Nullable
    public static Vec3 chest(Entity entity, float partialTick) {
        Points p = fresh(entity, partialTick);
        return p == null ? null : p.chest;
    }

    @Nullable
    private static Points fresh(Entity entity, float partialTick) {
        Points p = POINTS.get(entity.getId());
        return p == null || Math.abs(entity.tickCount + partialTick - p.age) > FRESH_TICKS ? null : p;
    }

    /** Forgets every point (a new world). */
    public static void clear() {
        POINTS.clear();
    }

    /** The last layer drawn: it measures the model as it was just posed and drawn. */
    public static final class Layer<T extends LivingEntity> extends RenderLayer<T, UltronModel<T>> {
        public Layer(RenderLayerParent<T, UltronModel<T>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            UltronModel<T> model = getParentModel();
            Points p = POINTS.computeIfAbsent(entity.getId(), id -> new Points());
            p.age = entity.tickCount + partialTick;
            for (int hand = 0; hand < 2; hand++) {
                boolean right = hand == 0;
                String side = right ? "right" : "left";
                poseStack.pushPose();
                model.root().translateAndRotate(poseStack);
                for (String name : new String[]{"hips", side + "Arm", side + "Forearm", side + "Hand"}) {
                    model.part(name).translateAndRotate(poseStack);
                }
                Vec3 palm = HandTracker.world(poseStack, 0.0F, 1.0F / 16.0F, 0.0F);
                ModelPart fingers = model.part(side + "Fingers");
                fingers.translateAndRotate(poseStack);
                if (right) {
                    p.palmRight = palm;
                } else {
                    p.palmLeft = palm;
                }
                for (int i = 0; i < 5; i++) {
                    UltronCubes.Cube finger = UltronCubes.named("finger" + i + (right ? "R" : "L"));
                    p.fingers[hand * 5 + i] = HandTracker.world(poseStack, (finger.x() + 0.5F) / 16.0F, 2.0F / 16.0F, 0.0F);
                }
                poseStack.popPose();
            }
            poseStack.pushPose();
            model.root().translateAndRotate(poseStack);
            model.part("hips").translateAndRotate(poseStack);
            model.part("body").translateAndRotate(poseStack);
            p.chest = HandTracker.world(poseStack, 0.0F, 3.0F / 16.0F, 0.0F);
            poseStack.popPose();
        }
    }

    private UltronTracker() {
    }
}
