package com.kolodkingal.enhanced.client.hydra;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the Hydra soldiers and officers (3.6, lane L8): the player model in their skins ({@link HydraLooks}), their
 * gear ({@link HydraGearModel}) with its energy cells glowing Tesseract blue (and a turned soldier's eyes), and the
 * sniper's red laser from his rifle to whoever he aims at — the kid-fair warning before his shot.
 */
public class HydraSoldierRenderer extends MobRenderer<HydraSoldierEntity, HydraSoldierModel> {
    private static final ResourceLocation GEAR = Enhanced.id("textures/entity/hydra/gear.png");
    private static final ResourceLocation GEAR_GLOW = Enhanced.id("textures/entity/hydra/gear_glow.png");
    /** The laser's colours: a red beam with a hot core, and the dot where it lands. */
    private static final int LASER = 0xFF2A2A;
    private static final int LASER_CORE = 0xFFC8C8;

    public HydraSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HydraSoldierModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new GearLayer(this, new HydraGearModel(context.bakeLayer(HydraGearModel.LAYER))));
    }

    @Override
    public ResourceLocation getTextureLocation(HydraSoldierEntity soldier) {
        return HydraLooks.skin(soldier);
    }

    @Override
    protected void scale(HydraSoldierEntity soldier, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public boolean shouldRender(HydraSoldierEntity soldier, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(soldier, frustum, x, y, z) || soldier.laserTarget() >= 0;
    }

    @Override
    public void render(HydraSoldierEntity soldier, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        HydraClient.track(soldier);
        super.render(soldier, entityYaw, partialTick, poseStack, buffers, packedLight);
        int target = soldier.laserTarget();
        if (target >= 0) {
            Entity aimed = soldier.level().getEntity(target);
            if (aimed != null) {
                laser(soldier, aimed, partialTick, buffers);
            }
        }
    }

    /**
     * The sniper's laser: a thin red line from his rifle's muzzle to the target's chest (a little short of it), with a
     * red dot there; it flickers faintly and grows brighter as his aim settles.
     */
    private void laser(HydraSoldierEntity sniper, Entity target, float partialTick, MultiBufferSource buffers) {
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        float yaw = Mth.lerp(partialTick, sniper.yHeadRotO, sniper.yHeadRot);
        float pitch = Mth.lerp(partialTick, sniper.xRotO, sniper.getXRot());
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
        Vec3 eye = sniper.getEyePosition(partialTick);
        Vec3 muzzle = eye.add(0.0, -0.3, 0.0).add(right.scale(0.32)).add(look.scale(1.3));
        Vec3 end = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.6, 0.0);
        Vec3 from = muzzle.subtract(camera);
        Vec3 to = end.subtract(camera);
        float flicker = 0.85F + 0.15F * Mth.sin((sniper.tickCount + partialTick) * 2.3F);
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        EnergyGeometry.ribbon(pose, vc, from, to, 0.035F, 0.035F, EnergyGeometry.argb(LASER, 0.55F * flicker),
                EnergyGeometry.argb(LASER, 0.55F * flicker));
        EnergyGeometry.ribbon(pose, vc, from, to, 0.012F, 0.012F, EnergyGeometry.argb(LASER_CORE, 0.9F),
                EnergyGeometry.argb(LASER_CORE, 0.9F));
        Vec3 toCamera = to.scale(-1.0);
        Vec3 dot = to.add(toCamera.normalize().scale(0.35));
        EnergyGeometry.ring(pose, vc, dot, toCamera, 0.09F, 0.09F, 10, EnergyGeometry.argb(LASER, 0.85F * flicker));
        EnergyGeometry.ring(pose, vc, dot, toCamera, 0.035F, 0.035F, 8, EnergyGeometry.argb(LASER_CORE, 1.0F));
    }

    /** The gear: drawn on each body part, then its glowing parts full bright. */
    private static final class GearLayer extends RenderLayer<HydraSoldierEntity, HydraSoldierModel> {
        private final HydraGearModel gear;

        GearLayer(RenderLayerParent<HydraSoldierEntity, HydraSoldierModel> parent, HydraGearModel gear) {
            super(parent);
            this.gear = gear;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, HydraSoldierEntity soldier,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (soldier.isInvisible()) {
                return;
            }
            HydraSoldierModel model = getParentModel();
            int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(soldier, 0.0F);
            gear.show(HydraLooks.pieces(soldier));
            draw(poseStack, model, buffers.getBuffer(RenderType.entityCutoutNoCull(GEAR)), packedLight, overlay);
            gear.show(HydraLooks.glowing(soldier));
            draw(poseStack, model, buffers.getBuffer(RenderType.eyes(GEAR_GLOW)), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
        }

        private void draw(PoseStack poseStack, HydraSoldierModel model, VertexConsumer vc, int light, int overlay) {
            group(poseStack, model.head, "head", vc, light, overlay);
            group(poseStack, model.body, "body", vc, light, overlay);
            group(poseStack, model.rightArm, "right_arm", vc, light, overlay);
            group(poseStack, model.leftArm, "left_arm", vc, light, overlay);
        }

        private void group(PoseStack poseStack, ModelPart part, String name, VertexConsumer vc, int light,
                           int overlay) {
            poseStack.pushPose();
            part.translateAndRotate(poseStack);
            gear.group(name).render(poseStack, vc, light, overlay);
            poseStack.popPose();
        }
    }
}
