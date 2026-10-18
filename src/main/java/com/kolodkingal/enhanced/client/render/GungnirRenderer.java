package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.odin.GungnirEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gungnir in flight (3.5): the spear's own item model, point first along its flight, out and home alike, with a slow roll
 * about its shaft; its heading eased between the last two ticks (as {@link ThrownWeaponRenderer} does), so a spear curving
 * home turns smoothly.
 */
public class GungnirRenderer extends EntityRenderer<GungnirEntity> {
    /** How big the spear is drawn in flight (a little bigger than in the hand, so it reads from far away). */
    private static final float SCALE = 1.15F;

    private final ItemRenderer itemRenderer;
    private final Map<GungnirEntity, Aim> aims = new WeakHashMap<>();

    private static final class Aim {
        int tick = Integer.MIN_VALUE;
        float yaw0;
        float pitch0;
        float yaw1;
        float pitch1;
    }

    public GungnirRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.2F;
    }

    @Override
    public void render(GungnirEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Aim aim = aims.computeIfAbsent(entity, e -> new Aim());
        if (aim.tick != entity.tickCount) {
            Vec3 motion = entity.getDeltaMovement();
            float turn = motion.horizontalDistanceSqr() < 1.0E-6 ? entity.getYRot() : (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
            float tilt = (float) (Mth.atan2(motion.y, Math.sqrt(motion.horizontalDistanceSqr())) * Mth.RAD_TO_DEG);
            boolean next = aim.tick == entity.tickCount - 1;
            aim.yaw0 = next ? aim.yaw1 : turn;
            aim.pitch0 = next ? aim.pitch1 : tilt;
            aim.yaw1 = turn;
            aim.pitch1 = tilt;
            aim.tick = entity.tickCount;
        }
        float heading = Mth.rotLerp(partialTick, aim.yaw0, aim.yaw1);
        float pitch = Mth.lerp(partialTick, aim.pitch0, aim.pitch1);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(heading));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        // The item model stands point up: lay it so the point leads along the flight, rolling slowly about its shaft.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 18.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        itemRenderer.renderStatic(entity.spear(), ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(GungnirEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
