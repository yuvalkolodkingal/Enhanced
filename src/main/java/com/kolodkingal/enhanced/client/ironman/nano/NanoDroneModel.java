package com.kolodkingal.enhanced.client.ironman.nano;

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

/**
 * One <b>wing blade</b> of the Mark 85's nanotech drones (3.8.3, the Endgame look; was a small round drone): a long
 * curved crescent built from {@link #SEGMENTS} short boxes along an arc, widest in the middle and tapering to points.
 * Each segment has a dark-red metal frame, a darker rim along the inner (concave) edge and a glowing cyan panel set
 * into it that shows on both faces; gold trims the outer edge, two gold bars split the panel into three cells, the tips
 * are gold, and a small grey thruster pod with glowing nozzles sits on the back. Its own layer definition ({@code
 * ClientSetup}, region nanotech), textures {@code wing_drone.png} (metal) and {@code wing_drone_glow.png} (the light).
 *
 * <p>Units are half pixels ({@link WingDraw} draws it at half size, for finer boxes and texels). Its frame: {@code x}
 * outward (away from Iron Man; the arc bulges that way), {@code y} up, {@code z} the face (forward). The origin is the
 * middle of the inner edge, where the blade's laser leaves.
 */
public final class NanoDroneModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("nano_drone"), "main");

    /** Boxes along the arc. */
    static final int SEGMENTS = 22;
    /** The outer edge's radius, the arc's half angle (degrees), the width in the middle and at the tips, thickness. */
    static final float RADIUS = 27.0F;
    static final float HALF_SPAN = 52.0F;
    static final float WIDTH = 12.0F;
    static final float TIP = 2.0F;
    static final float THICK = 3.0F;
    /** The frame around the panel (each side). */
    static final float FRAME = 1.3F;
    /** The arc's centre (x), so that the inner edge's middle is at the origin. */
    static final float CX = -(RADIUS - WIDTH);
    /** The segments where the gold bars cross the panel. */
    static final int BAR_A = 6;
    static final int BAR_B = SEGMENTS - 1 - 6;

    final ModelPart[] frame = new ModelPart[SEGMENTS];
    final ModelPart[] rim = new ModelPart[SEGMENTS];
    final ModelPart[] panel = new ModelPart[SEGMENTS];
    final ModelPart[] trim = new ModelPart[SEGMENTS];
    final ModelPart bars;
    final ModelPart pod;
    final ModelPart nozzle;

    public NanoDroneModel(ModelPart root) {
        for (int i = 0; i < SEGMENTS; i++) {
            frame[i] = root.getChild("frame_" + i);
            rim[i] = root.getChild("rim_" + i);
            panel[i] = hasPanel(i) ? root.getChild("panel_" + i) : null;
            trim[i] = hasTrim(i) ? root.getChild("trim_" + i) : null;
        }
        bars = root.getChild("bars");
        pod = root.getChild("pod");
        nozzle = root.getChild("nozzle");
    }

    /** Segment {@code i}'s angle (degrees, 0 the middle, up positive). */
    static float angle(int i) {
        return -HALF_SPAN + 2.0F * HALF_SPAN * (i + 0.5F) / SEGMENTS;
    }

    /** How far segment {@code i} is from the middle (0) to a tip (1). */
    static float fromMiddle(int i) {
        return Math.abs(angle(i)) / HALF_SPAN;
    }

    /** Segment {@code i}'s width (the crescent: wide in the middle, pointed at the tips). */
    static float width(int i) {
        double d = fromMiddle(i);
        return (float) (TIP + (WIDTH - TIP) * Math.pow(Math.cos(d * Math.PI / 2.0), 0.7));
    }

    static boolean hasPanel(int i) {
        return width(i) - 2.0F * FRAME >= 1.2F;
    }

    /** Gold along the outer edge between the bars and the tips. */
    static boolean hasTrim(int i) {
        float d = fromMiddle(i);
        return d > 0.3F && d < 0.75F;
    }

    /** The two segments at each end are gold. */
    static boolean goldTip(int i) {
        return i < 2 || i >= SEGMENTS - 2;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // A segment is a little longer than its share of the outer arc, so the boxes overlap with no gap.
        float length = (float) (RADIUS * Math.toRadians(2.0 * HALF_SPAN / SEGMENTS)) + 0.5F;
        for (int i = 0; i < SEGMENTS; i++) {
            float rad = (float) Math.toRadians(angle(i));
            float w = width(i);
            // The box's x runs out along the radius, its y along the arc: turned by the angle about z.
            float mid = RADIUS - w / 2.0F;
            PartPose at = PartPose.offsetAndRotation(CX + mid * (float) Math.cos(rad), mid * (float) Math.sin(rad),
                    0.0F, 0.0F, 0.0F, rad);
            root.addOrReplaceChild("frame_" + i, CubeListBuilder.create().texOffs(goldTip(i) ? 32 : 0, (i * 5) % 22)
                    .addBox(-w / 2.0F, -length / 2.0F, -THICK / 2.0F, w, length, THICK), at);
            // The darker rim along the inner edge stands out a little from the faces.
            float rimW = Math.min(1.0F, w / 2.0F);
            root.addOrReplaceChild("rim_" + i, CubeListBuilder.create().texOffs(0, 32 + (i * 3) % 8)
                    .addBox(-w / 2.0F, -length / 2.0F, -THICK / 2.0F - 0.25F, rimW, length, THICK + 0.5F), at);
            if (hasPanel(i)) {
                float pw = w - 2.0F * FRAME;
                root.addOrReplaceChild("panel_" + i, CubeListBuilder.create().texOffs(0, (i * 7) % 40)
                        .addBox(-pw / 2.0F + 0.15F, -length / 2.0F, -THICK / 2.0F - 0.3F, pw, length, THICK + 0.6F),
                        at);
            }
            if (hasTrim(i)) {
                root.addOrReplaceChild("trim_" + i, CubeListBuilder.create().texOffs(32, (i * 3) % 20)
                        .addBox(w / 2.0F - 1.1F, -length / 2.0F, -THICK / 2.0F - 0.2F, 1.1F, length, THICK + 0.4F),
                        at);
            }
        }
        // Two gold bars across the panel (each its own child, turned with its segment).
        PartDefinition bars = root.addOrReplaceChild("bars", CubeListBuilder.create(), PartPose.ZERO);
        for (int i : new int[] {BAR_A, BAR_B}) {
            float rad = (float) Math.toRadians(angle(i));
            float w = width(i);
            float mid = RADIUS - w / 2.0F;
            bars.addOrReplaceChild("bar_" + i, CubeListBuilder.create().texOffs(32, 22)
                            .addBox(-w / 2.0F - 0.2F, -0.9F, -THICK / 2.0F - 0.3F, w + 0.4F, 1.8F, THICK + 0.6F),
                    PartPose.offsetAndRotation(CX + mid * (float) Math.cos(rad), mid * (float) Math.sin(rad), 0.0F,
                            0.0F, 0.0F, rad));
        }
        // The thruster pod on the back, in the middle, with a gold ring; its nozzles above and below.
        float podX = WIDTH / 2.0F - 3.5F;
        root.addOrReplaceChild("pod", CubeListBuilder.create().texOffs(32, 32)
                .addBox(podX, -5.0F, -THICK / 2.0F - 4.0F, 7.0F, 10.0F, 4.0F)
                .texOffs(32, 50)
                .addBox(podX - 0.5F, -1.0F, -THICK / 2.0F - 4.5F, 8.0F, 2.0F, 5.0F), PartPose.ZERO);
        root.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(40, 50)
                .addBox(podX + 1.5F, -7.0F, -THICK / 2.0F - 3.5F, 4.0F, 2.0F, 3.0F)
                .addBox(podX + 1.5F, 5.0F, -THICK / 2.0F - 3.5F, 4.0F, 2.0F, 3.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Which segments show (a blade forming or folding away). */
    @FunctionalInterface
    interface Shown {
        boolean test(int segment);
    }

    /** Draws the metal of the segments that show, and the pod when {@code pod}. */
    void renderMetal(PoseStack pose, VertexConsumer vc, int light, int overlay, int color, Shown shown, boolean pod) {
        for (int i = 0; i < SEGMENTS; i++) {
            if (!shown.test(i)) {
                continue;
            }
            frame[i].render(pose, vc, light, overlay, color);
            rim[i].render(pose, vc, light, overlay, color);
            if (trim[i] != null) {
                trim[i].render(pose, vc, light, overlay, color);
            }
        }
        if (shown.test(BAR_A) && shown.test(BAR_B)) {
            bars.render(pose, vc, light, overlay, color);
        }
        if (pod) {
            this.pod.render(pose, vc, light, overlay, color);
        }
    }

    /** Draws the light: the panels of the segments that show, and the pod's nozzles when {@code pod}. */
    void renderLight(PoseStack pose, VertexConsumer vc, int light, int overlay, int color, Shown shown, boolean pod) {
        for (int i = 0; i < SEGMENTS; i++) {
            if (panel[i] != null && shown.test(i)) {
                panel[i].render(pose, vc, light, overlay, color);
            }
        }
        if (pod) {
            nozzle.render(pose, vc, light, overlay, color);
        }
    }
}
