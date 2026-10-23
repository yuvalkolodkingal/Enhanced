package com.kolodkingal.enhanced.client.eternity;

import com.kolodkingal.enhanced.block.EternityAltarBlockEntity;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Eternity's altar while it charges (3.8, PLAN 10.3): the copy of the traveller's key rises over the altar and spins
 * there, faster as the charge grows, in a column of violet light that climbs from the altar's starry top (the stars of
 * the column are the server's particles); a soft violet glow on the top always. Everything moves with the frame's time.
 */
public class AltarRenderer implements BlockEntityRenderer<EternityAltarBlockEntity> {
    private static final int VIOLET = 0xB57BFF;
    private static final int PALE = 0xF0E0FF;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EternityAltarBlockEntity altar, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float time = mc.level.getGameTime() + partialTick;
        EternityClient.hum(altar.getBlockPos());
        // (The light is drawn in the camera's frame, as every energy effect: its ribbons turn to face the camera.)
        Matrix4f m = new Matrix4f();
        Vec3 base = Vec3.atLowerCornerOf(altar.getBlockPos()).subtract(mc.gameRenderer.getMainCamera().getPosition());
        VertexConsumer glow = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        // The starry top's breath of light.
        float breath = 0.5F + 0.5F * Mth.sin(time * 0.08F);
        Vec3 top = base.add(0.5, 0.96, 0.5);
        EnergyGeometry.ring(m, glow, top, new Vec3(0, 1, 0), 0.32F, 0.12F, 20,
                EnergyGeometry.argb(VIOLET, 0.18F + 0.12F * breath));
        ItemStack key = altar.key();
        if (key.isEmpty()) {
            return;
        }
        float charge = Mth.clamp((time - altar.start()) / EternityAltarBlockEntity.chargeTicks(), 0.0F, 1.0F);
        // The column of light, taller and brighter as the charge grows.
        float height = 1.0F + 6.0F * charge;
        for (int i = 0; i < 3; i++) {
            float width = (0.18F + i * 0.14F) * (0.8F + 0.2F * Mth.sin(time * 0.5F + i));
            int colour = i == 0 ? PALE : VIOLET;
            EnergyGeometry.ribbon(m, glow, top, top.add(0, height, 0), width, width * 0.4F,
                    EnergyGeometry.argb(colour, (0.55F - i * 0.15F) * (0.4F + 0.6F * charge)),
                    EnergyGeometry.argb(colour, 0.0F));
        }
        // The key floats up and spins.
        float lift = 0.6F + 0.5F * Mth.clamp(charge * 3.0F, 0.0F, 1.0F) + 0.06F * Mth.sin(time * 0.15F);
        float spin = time * (4.0F + 14.0F * charge);
        pose.pushPose();
        pose.translate(0.5, 1.0 + lift, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(0.9F, 0.9F, 0.9F);
        mc.getItemRenderer().renderStatic(key, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
        pose.popPose();
    }

    // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
    public AABB getRenderBoundingBox(EternityAltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).expandTowards(0, 8, 0).inflate(1.0);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
