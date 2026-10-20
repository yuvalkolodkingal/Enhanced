package com.kolodkingal.enhanced.client.captain;

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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Vibranium Shield's model (3.8, PLAN 3.3): a round, slightly convex disc 14 pixels across, its front painted with
 * the red, silver and red rings round the blue centre and its white star, its back brushed vibranium, a red rim; behind
 * it a leather pad and the grip the hand holds. The disc is our own mesh (a dome of quads, drawn by {@link
 * #renderDisc}: boxes cannot make a circle); the pad and the grip are the parts of a layer definition ({@link #LAYER}).
 * Our own drawing ({@code textures/entity/captain/shield.png}, by {@code SCRATCH/v38/art/l1/gen_captain.py}).
 *
 * <p>Its frame is vanilla's shield's (so the item's display transforms are vanilla's): the centre of the disc at the
 * origin, its face toward -z (+z once the item renderer has flipped it), the grip along +z, pixels as units.
 */
public final class ShieldModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("vibranium_shield"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/captain/shield.png");
    /** The disc's radius, and how far its centre stands out (pixels). */
    public static final float RADIUS = 7.0F;
    public static final float DOME = 1.1F;
    /** Where the front's rim and the back's rim are (pixels along z). */
    public static final float FRONT = -2.0F;
    public static final float BACK = -1.3F;
    /** Segments round the disc and rings out from its centre. */
    private static final int SEGMENTS = 40;
    private static final int RINGS = 5;
    /** The texture is 128 x 128: the front at (0, 0) and the back at (64, 0), 64 square; the rim strip at (0, 64). */
    private static final float TEX = 128.0F;

    private final ModelPart pad;
    private final ModelPart grip;

    public ShieldModel(ModelPart root) {
        this.pad = root.getChild("pad");
        this.grip = root.getChild("grip");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The leather pad on the back and the strap across it (where the forearm lies).
        root.addOrReplaceChild("pad", CubeListBuilder.create()
                .texOffs(0, 80).addBox(-1.5F, -3.0F, -1.3F, 3.0F, 6.0F, 0.6F)
                .texOffs(0, 90).addBox(-4.0F, -0.6F, -1.1F, 8.0F, 1.2F, 0.5F), PartPose.ZERO);
        // The grip the hand holds, standing off the back (as vanilla's handle).
        root.addOrReplaceChild("grip", CubeListBuilder.create()
                .texOffs(24, 80).addBox(-0.75F, -2.5F, -0.7F, 1.5F, 5.0F, 4.7F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static RenderType renderType() {
        return RenderType.entityCutoutNoCull(TEXTURE);
    }

    /** The whole shield: the disc, the pad and the grip. */
    public void renderAll(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        renderDisc(pose, buffer, light, overlay, 0xFFFFFFFF);
        pad.render(pose, buffer, light, overlay);
        grip.render(pose, buffer, light, overlay);
    }

    /** The shield on a back: the disc and the pad (the grip would go into him). */
    public void renderOnBack(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        renderDisc(pose, buffer, light, overlay, 0xFFFFFFFF);
        pad.render(pose, buffer, light, overlay);
    }

    /** The front's depth at a distance {@code r} from the centre (pixels). */
    static float frontZ(float r) {
        float k = r / RADIUS;
        return FRONT - DOME * (1.0F - k * k);
    }

    static float backZ(float r) {
        float k = r / RADIUS;
        return BACK - DOME * 0.85F * (1.0F - k * k);
    }

    /** The disc: the domed front, the back that follows it, and the rim between them. */
    public void renderDisc(PoseStack poseStack, VertexConsumer vc, int light, int overlay, int color) {
        PoseStack.Pose pose = poseStack.last();
        for (int ring = 0; ring < RINGS; ring++) {
            float r0 = RADIUS * ring / RINGS;
            float r1 = RADIUS * (ring + 1) / RINGS;
            for (int s = 0; s < SEGMENTS; s++) {
                float a0 = Mth.TWO_PI * s / SEGMENTS;
                float a1 = Mth.TWO_PI * (s + 1) / SEGMENTS;
                // The front (toward -z).
                front(vc, pose, r0, a0, light, overlay, color);
                front(vc, pose, r0, a1, light, overlay, color);
                front(vc, pose, r1, a1, light, overlay, color);
                front(vc, pose, r1, a0, light, overlay, color);
                // The back (toward +z).
                back(vc, pose, r0, a0, light, overlay, color);
                back(vc, pose, r1, a0, light, overlay, color);
                back(vc, pose, r1, a1, light, overlay, color);
                back(vc, pose, r0, a1, light, overlay, color);
            }
        }
        for (int s = 0; s < SEGMENTS; s++) {
            float a0 = Mth.TWO_PI * s / SEGMENTS;
            float a1 = Mth.TWO_PI * (s + 1) / SEGMENTS;
            rim(vc, pose, a0, FRONT, 64.0F, s, light, overlay, color);
            rim(vc, pose, a0, BACK, 72.0F, s, light, overlay, color);
            rim(vc, pose, a1, BACK, 72.0F, s + 1, light, overlay, color);
            rim(vc, pose, a1, FRONT, 64.0F, s + 1, light, overlay, color);
        }
    }

    private static void front(VertexConsumer vc, PoseStack.Pose pose, float r, float a, int light, int overlay,
            int color) {
        float x = r * Mth.cos(a);
        float y = r * Mth.sin(a);
        float z = frontZ(r);
        float k = 2.0F * DOME / (RADIUS * RADIUS);
        float nx = k * x;
        float ny = k * y;
        float len = Mth.sqrt(nx * nx + ny * ny + 1.0F);
        vc.addVertex(pose, x / 16.0F, y / 16.0F, z / 16.0F).setColor(color)
                .setUv((32.0F + 32.0F * x / RADIUS) / TEX, (32.0F + 32.0F * y / RADIUS) / TEX)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx / len, ny / len, -1.0F / len);
    }

    private static void back(VertexConsumer vc, PoseStack.Pose pose, float r, float a, int light, int overlay,
            int color) {
        float x = r * Mth.cos(a);
        float y = r * Mth.sin(a);
        float z = backZ(r);
        float k = 2.0F * DOME * 0.85F / (RADIUS * RADIUS);
        float nx = -k * x;
        float ny = -k * y;
        float len = Mth.sqrt(nx * nx + ny * ny + 1.0F);
        vc.addVertex(pose, x / 16.0F, y / 16.0F, z / 16.0F).setColor(color)
                .setUv((96.0F - 32.0F * x / RADIUS) / TEX, (32.0F + 32.0F * y / RADIUS) / TEX)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx / len, ny / len, 1.0F / len);
    }

    private static void rim(VertexConsumer vc, PoseStack.Pose pose, float a, float z, float v, int s, int light,
            int overlay, int color) {
        float cos = Mth.cos(a);
        float sin = Mth.sin(a);
        vc.addVertex(pose, RADIUS * cos / 16.0F, RADIUS * sin / 16.0F, z / 16.0F).setColor(color)
                .setUv(64.0F * s / SEGMENTS / TEX, v / TEX).setOverlay(overlay).setLight(light)
                .setNormal(pose, cos, sin, 0.0F);
    }
}
