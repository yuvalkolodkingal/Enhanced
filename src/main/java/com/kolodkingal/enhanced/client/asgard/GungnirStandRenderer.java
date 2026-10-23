package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.block.asgard.GungnirStandBlockEntity;
import com.kolodkingal.enhanced.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Gungnir on its stand in the vault (3.5): the spear upright between the stand's prongs, gently rising and falling. */
public class GungnirStandRenderer implements BlockEntityRenderer<GungnirStandBlockEntity> {
    public GungnirStandRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GungnirStandBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null || !be.shows(Minecraft.getInstance().player)) {
            return; // (3.6.1: the spear is another player's: the stand is empty for this one)
        }
        float time = be.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        // (the spear's own 3D model, point up, centred on its middle: lane L3's)
        pose.translate(0.5, 1.9 + 0.05 * Math.sin(time / 20.0), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 0.5F));
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.GUNGNIR.get()), ItemDisplayContext.NONE, light, overlay,
                pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }

    // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
    public AABB getRenderBoundingBox(GungnirStandBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 3, 0);
    }
}
