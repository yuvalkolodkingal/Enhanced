package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A cocoon of Apocalypse's sand round a creature he has entombed: a rough box of ochre grain, 1.1 blocks wide and 2.2 tall,
 * that follows the victim and trembles a little as it holds, with cracks that fade in over it as its health is lost (the
 * friends of the victim free them by hitting it). The sand that falls when it breaks is {@link SandFx}'s.
 */
public class SandCocoonRenderer extends EntityRenderer<SandCocoonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("sand_cocoon"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/sand_cocoon.png");
    public static final ResourceLocation CRACKS = Enhanced.id("textures/entity/sand_cocoon_cracks.png");
    /** The box in model pixels: 16 by 32 by 16, drawn at 1.1 times that (1.1 by 2.2 blocks). */
    private static final float SIZE = 1.1F;

    private final ModelPart box;

    public SandCocoonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.box = context.bakeLayer(LAYER);
    }

    /** The layer: one box, standing on its pivot at the feet of the victim. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -32.0F, -8.0F, 16.0F, 32.0F, 16.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(SandCocoonEntity cocoon, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = cocoon.tickCount + partialTicks;
        // It forms over six ticks (rising from the ground), and shakes a little as it holds.
        float form = AnimMath.ss(0.0F, 6.0F, t);
        float shake = 0.012F * Mth.sin(t * 1.9F) * form;
        poseStack.pushPose();
        poseStack.translate(shake, 0.0, shake);
        poseStack.scale(SIZE, SIZE * (0.35F + 0.65F * form), SIZE);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // (The box is built on the usual flipped model space: its feet are at y = 0, its top 32 pixels above them.)
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        box.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        float broken = 1.0F - Mth.clamp(cocoon.healthFraction(), 0.0F, 1.0F);
        if (broken > 0.02F) {
            int alpha = (int) (Mth.clamp(broken * 1.15F, 0.0F, 1.0F) * 255.0F);
            VertexConsumer cracks = buffer.getBuffer(RenderType.entityTranslucent(CRACKS));
            poseStack.scale(1.01F, 1.01F, 1.01F);
            box.render(poseStack, cracks, packedLight, OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF);
        }
        poseStack.popPose();
        super.render(cocoon, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SandCocoonEntity cocoon) {
        return TEXTURE;
    }
}
