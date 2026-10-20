package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.ironman.IronMissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Falcon's Wrist Rocket in flight (3.8): a small grey rocket with a red nose and four little fins (our own model,
 * {@code textures/entity/falcon/rocket.png}), turned along its flight, and a short orange flame behind it.
 */
public class FalconRocketRenderer extends EntityRenderer<IronMissileEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("falcon_rocket"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/falcon/rocket.png");
    private static final int FLAME = 0xFFB040;
    private static final int CORE = 0xFFF4C0;

    private final ModelPart rocket;

    public FalconRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.rocket = context.bakeLayer(LAYER);
    }

    /** The rocket along +z (its nose at the front). */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("rocket", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F)
                .texOffs(16, 0).addBox(-0.6F, -0.6F, 3.0F, 1.2F, 1.2F, 1.5F)
                .texOffs(0, 8).addBox(-2.0F, -0.25F, -3.0F, 4.0F, 0.5F, 1.5F)
                .texOffs(0, 11).addBox(-0.25F, -2.0F, -3.0F, 0.5F, 4.0F, 1.5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public ResourceLocation getTextureLocation(IronMissileEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(IronMissileEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(IronMissileEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        Vec3 dir = motion.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : motion.normalize();
        float yaw = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(dir.y, dir.horizontalDistance()) * Mth.RAD_TO_DEG);
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 24.0F));
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        rocket.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        // The flame behind it, facing the camera.
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        Vec3 centre = entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.5, 0).subtract(camera);
        Vec3 tailStart = centre.subtract(dir.scale(0.2));
        float flicker = 0.85F + 0.15F * Mth.sin((entity.tickCount + partialTick) * 3.1F);
        Vec3 tailEnd = tailStart.subtract(dir.scale(0.55 * flicker));
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        EnergyGeometry.ribbon(pose, vc, tailEnd, tailStart, 0.0F, 0.16F, EnergyGeometry.argb(FLAME, 0.0F),
                EnergyGeometry.argb(FLAME, 0.9F));
        EnergyGeometry.ribbon(pose, vc, tailEnd, tailStart, 0.0F, 0.07F, EnergyGeometry.argb(CORE, 0.0F),
                EnergyGeometry.argb(CORE, 1.0F));
    }
}
