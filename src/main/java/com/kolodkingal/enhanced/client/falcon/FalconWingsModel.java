package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Falcon's Exo-Wings (3.8, PLAN 8.2), our own drawing: a flat back pack with two jet nozzles under it, and two wings of
 * five long segmented metal feathers each. Every feather is three nested segments that slide out of one another
 * (telescoping, so the folded wings fit on the pack) and hinge at the wing's knuckle on the pack, where the wing sweeps
 * and the feathers fan; {@link #pose} sets it all from the numbers of {@link FalconWingsState}. Model space is the
 * body's: x to his left, y down, z behind him; the pack sits on his back. Textures {@code
 * textures/entity/falcon/wings_endgame.png} (steel with red stripes) and {@code wings_winter_soldier.png} (the darker
 * EXO-7), by {@code SCRATCH/v38/art/l5/gen_falcon.py}; the nozzles' flames are drawn bright from the same texture.
 */
public class FalconWingsModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("falcon_wings"), "main");
    public static final ResourceLocation ENDGAME = Enhanced.id("textures/entity/falcon/wings_endgame.png");
    public static final ResourceLocation WINTER_SOLDIER =
            Enhanced.id("textures/entity/falcon/wings_winter_soldier.png");

    /** The feathers, top to bottom: the length (pixels) of each of their three segments. */
    public static final int[] SEGMENT = {12, 11, 10, 9, 8};
    public static final int FEATHERS = SEGMENT.length;
    /** How much of a segment stays inside the one before it, drawn out (pixels). */
    static final float OVERLAP = 1.0F;
    /** How far behind the one above it each feather lies (pixels), so the folded ones stack. */
    static final float STACK = 0.35F;

    private final ModelPart pack;
    private final ModelPart flames;
    /** [side][feather]: side 0 is the right wing, 1 the left. */
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[][] feathers = new ModelPart[2][FEATHERS];
    private final ModelPart[][][] segments = new ModelPart[2][FEATHERS][3];

    public FalconWingsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.pack = root.getChild("pack");
        this.flames = root.getChild("flames");
        for (int side = 0; side < 2; side++) {
            wings[side] = root.getChild(side == 0 ? "right_wing" : "left_wing");
            for (int i = 0; i < FEATHERS; i++) {
                feathers[side][i] = wings[side].getChild("feather" + i);
                for (int k = 0; k < 3; k++) {
                    segments[side][i][k] = feathers[side][i].getChild("segment" + k);
                }
            }
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The pack: a flat plate on his back, its two nozzles under it, and the knuckles of the wings.
        root.addOrReplaceChild("pack", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-3.5F, 0.5F, 2.0F, 7.0F, 9.0F, 2.0F)
                .texOffs(20, 40).addBox(-3.0F, 8.5F, 2.4F, 2.0F, 3.0F, 2.0F)
                .texOffs(20, 40).addBox(1.0F, 8.5F, 2.4F, 2.0F, 3.0F, 2.0F)
                .texOffs(0, 52).addBox(-2.5F, 2.0F, 4.0F, 5.0F, 5.0F, 1.0F), PartPose.ZERO);
        // The nozzles' flames (drawn bright, stretched by the flicker).
        root.addOrReplaceChild("flames", CubeListBuilder.create()
                .texOffs(32, 40).addBox(-2.75F, 0.0F, 2.65F, 1.5F, 4.0F, 1.5F)
                .texOffs(32, 40).addBox(1.25F, 0.0F, 2.65F, 1.5F, 4.0F, 1.5F), PartPose.offset(0.0F, 11.5F, 0.0F));
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1.0F : 1.0F;
            PartDefinition wing = root.addOrReplaceChild(side == 0 ? "right_wing" : "left_wing",
                    CubeListBuilder.create().texOffs(44, 40).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(sign * 3.0F, 2.0F, 3.9F));
            for (int i = 0; i < FEATHERS; i++) {
                PartDefinition feather = wing.addOrReplaceChild("feather" + i, CubeListBuilder.create(),
                        PartPose.ZERO);
                int len = SEGMENT[i];
                for (int k = 0; k < 3; k++) {
                    // Each segment a little thinner than the one it slides out of, so the nested ones never fight.
                    CubeDeformation thin = new CubeDeformation(-0.06F * k);
                    CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, i * 8)
                            .addBox(sign < 0 ? -len : 0.0F, -3.0F, -0.5F, len, 6.0F, 1.0F, thin);
                    if (k == 2) {
                        // The feather's point.
                        cubes.texOffs(28, i * 8).addBox(sign < 0 ? -len - 3.0F : len, -2.5F, -0.5F, 3.0F, 4.0F,
                                1.0F, thin);
                    }
                    feather.addOrReplaceChild("segment" + k, cubes, PartPose.ZERO);
                }
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Sets the wings from a pose of {@link FalconWingsState} ({@code p}), with the flap's strength and the age (ticks,
     * with the frame's part) for its beat; and the flames' length (0: none).
     */
    public void pose(float[] p, float flap, float age, float flame) {
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1.0F : 1.0F;
            ModelPart wing = wings[side];
            wing.x = sign * p[FalconWingsState.HX];
            wing.y = p[FalconWingsState.HY];
            wing.z = p[FalconWingsState.HZ];
            // (A positive sweep takes either wing back: the right one turns one way about y, the left the other.)
            wing.yRot = -sign * p[FalconWingsState.SWEEP] * Mth.DEG_TO_RAD;
            wing.xRot = p[FalconWingsState.TILT] * Mth.DEG_TO_RAD;
            wing.zRot = 0.0F;
            float extend = Mth.clamp(p[FalconWingsState.EXTEND], 0.0F, 1.0F);
            for (int i = 0; i < FEATHERS; i++) {
                // The beat runs down the feathers like a wave; the top ones move most.
                float beat = flap * (10.0F - i) * Mth.sin(age * 0.32F - i * 0.35F);
                float elevation = p[FalconWingsState.TOP] - i * p[FalconWingsState.STEP] + beat;
                ModelPart feather = feathers[side][i];
                feather.x = 0.0F;
                feather.y = 0.0F;
                feather.z = i * STACK;
                feather.zRot = -sign * elevation * Mth.DEG_TO_RAD;
                float step = (SEGMENT[i] - OVERLAP) * extend;
                for (int k = 0; k < 3; k++) {
                    segments[side][i][k].x = sign * step * k;
                }
            }
        }
        flames.visible = flame > 0.01F;
        flames.yScale = Math.max(0.01F, flame);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        pack.render(poseStack, buffer, light, overlay, color);
        wings[0].render(poseStack, buffer, light, overlay, color);
        wings[1].render(poseStack, buffer, light, overlay, color);
    }

    /** The nozzles' flames (drawn with a bright render type by the layer). */
    public void renderFlames(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        if (flames.visible) {
            flames.render(poseStack, buffer, light, overlay);
        }
    }

    /**
     * The outer segments (from segment {@code from}, 0-2, to the point) of one wing's top {@code count} feathers, for
     * the first-person view (side 0 right).
     */
    public void renderTips(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int side, int count,
                           int from) {
        poseStack.pushPose();
        wings[side].translateAndRotate(poseStack);
        for (int i = 0; i < Math.min(count, FEATHERS); i++) {
            poseStack.pushPose();
            feathers[side][i].translateAndRotate(poseStack);
            for (int k = Math.max(0, from); k < 3; k++) {
                segments[side][i][k].render(poseStack, buffer, light, overlay);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
