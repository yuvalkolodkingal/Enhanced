package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
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
 * Mjolnir or Stormbreaker in flight: thrown, it tumbles end over end; coming home, it flies head first with the handle
 * trailing, the way it always returns to Thor's hand.
 */
public class ThrownWeaponRenderer extends EntityRenderer<ThrownWeaponEntity> {
    /**
     * How big each weapon is drawn in flight (blocks per pixel of the item model, over 16): a little bigger than in the
     * hand (see {@link GripMath.Kind#scale}), so it reads from far away, and Stormbreaker bigger than Mjolnir.
     */
    private static final float MJOLNIR_SCALE = 1.25F;
    private static final float STORMBREAKER_SCALE = 1.3F;

    private final ItemRenderer itemRenderer;
    /**
     * The heading and pitch of each weapon in flight at its last two ticks (3.3): drawn eased between them, as the game
     * draws a body's turn; read from the motion of this tick alone, a weapon curving home turned in steps, one a tick.
     */
    private final Map<ThrownWeaponEntity, Aim> aims = new WeakHashMap<>();

    private static final class Aim {
        int tick = Integer.MIN_VALUE;
        float yaw0;
        float pitch0;
        float yaw1;
        float pitch1;
    }

    public ThrownWeaponRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(ThrownWeaponEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Aim aim = aims.computeIfAbsent(entity, e -> new Aim());
        if (aim.tick != entity.tickCount) {
            Vec3 motion = entity.getDeltaMovement();
            float turn = motion.horizontalDistanceSqr() < 1.0E-6 ? entity.getYRot()
                    : (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
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
        poseStack.translate(0.0F, 0.3F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(heading));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        if (entity.isReturning()) {
            // The item model stands handle-down, head-up: lay it so the head leads along the flight.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            // A gentle roll about its own long axis, so it does not look nailed to its path.
            poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 25.0F));
        } else {
            float spin = (entity.tickCount + partialTick) * 40.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
        }
        float scale = ThorAbilities.isStormbreaker(entity.weapon()) ? STORMBREAKER_SCALE : MJOLNIR_SCALE;
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(entity.weapon(), ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(ThrownWeaponEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
