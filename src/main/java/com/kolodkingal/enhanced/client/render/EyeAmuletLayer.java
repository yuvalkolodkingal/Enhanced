package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.strange.EyeTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * The Eye of Agamotto on Doctor Strange's chest (3.6): a gold amulet on a short chain, its lids closed, or parted on the
 * green gem with a green glow while the Eye's time runs ({@link Toggle#TIME_SLOW}). Doctor Strange always wears it (the
 * Multiverse of Madness look higher, as the film's); a custom hero with the Eye's row wears it while he carries an Eye.
 * Its texture {@code strange_eye_amulet.png} (16x16): the closed Eye u 0-7, the open one u 8-15, the glow below (v 8-15).
 */
public class EyeAmuletLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/strange_eye_amulet.png");
    /** The amulet's half-size (pixels), and how far before the chest it hangs. */
    private static final float HALF = 1.9F;
    private static final float FRONT = -2.35F;

    public EyeAmuletLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Whether this player wears the Eye now. */
    static boolean wears(AbstractClientPlayer player, PowerData data) {
        if (player.isInvisible() || !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            return false;
        }
        return data.power() == Power.DOCTOR_STRANGE
                || data.power() == Power.CUSTOM && data.has(HeroAbility.STRANGE_EYE) && EyeTime.hasEye(player);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        PowerData data = Powers.get(player);
        if (!wears(player, data)) {
            return;
        }
        boolean open = data.toggle(Toggle.TIME_SLOW);
        boolean high = data.power() == Power.DOCTOR_STRANGE && "multiverse".equals(Variants.current(data).id());
        draw(poseStack, buffers, light, LivingEntityRenderer.getOverlayCoords(player, 0.0F), getParentModel().body, open,
                high, ageInTicks);
    }

    /** Draws the Eye on {@code body} (the pose stack at the model's root). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, ModelPart body,
                            boolean open, boolean high, float age) {
        poseStack.pushPose();
        body.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        float cy = high ? 2.4F : 3.6F;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float u0 = open ? 0.5F : 0.0F;
        quad(vc, pose, 0.0F, cy, FRONT, HALF, u0, u0 + 0.5F, 0.0F, 0.5F, light, overlay, 0xFFFFFFFF);
        // The chain: two short gold links up to the collar, taken from the amulet's rim.
        for (int side = -1; side <= 1; side += 2) {
            float x0 = side * 1.0F;
            float x1 = side * 2.6F;
            link(vc, pose, x0, cy - HALF, x1, 0.2F, FRONT + 0.05F, light, overlay);
        }
        if (open) {
            float pulse = 0.75F + 0.25F * Mth.sin(age * 0.25F);
            VertexConsumer glow = buffers.getBuffer(RenderType.eyes(TEXTURE));
            int a = (int) (255 * pulse);
            quad(glow, pose, 0.0F, cy, FRONT - 0.05F, HALF * 1.6F, 0.0F, 0.5F, 0.5F, 1.0F,
                    LightTexture.FULL_BRIGHT, overlay, (a << 24) | 0xFFFFFF);
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float z, float half, float u0,
                             float u1, float v0, float v1, int light, int overlay, int color) {
        vertex(vc, pose, cx - half, cy - half, z, u1, v0, light, overlay, color);
        vertex(vc, pose, cx + half, cy - half, z, u0, v0, light, overlay, color);
        vertex(vc, pose, cx + half, cy + half, z, u0, v1, light, overlay, color);
        vertex(vc, pose, cx - half, cy + half, z, u1, v1, light, overlay, color);
    }

    private static void link(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z,
                             int light, int overlay) {
        float w = 0.25F;
        // (u, v of a gold pixel of the rim)
        float u = 1.5F / 16.0F;
        float v = 3.5F / 16.0F;
        vertex(vc, pose, x0 - w, y0, z, u, v, light, overlay, 0xFFFFFFFF);
        vertex(vc, pose, x0 + w, y0, z, u, v, light, overlay, 0xFFFFFFFF);
        vertex(vc, pose, x1 + w, y1, z, u, v, light, overlay, 0xFFFFFFFF);
        vertex(vc, pose, x1 - w, y1, z, u, v, light, overlay, 0xFFFFFFFF);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int light, int overlay, int color) {
        vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
