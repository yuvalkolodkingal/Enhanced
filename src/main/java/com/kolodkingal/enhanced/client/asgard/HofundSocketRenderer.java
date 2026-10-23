package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.block.asgard.HofundSocketBlockEntity;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Hofund in its socket (3.5): the sword stands point down in the drum, turning slowly; not while a player near it holds a
 * Hofund (the sword is in his hand).
 */
public class HofundSocketRenderer implements BlockEntityRenderer<HofundSocketBlockEntity> {
    private static final double HELD_NEAR = 8.0;

    public HofundSocketRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HofundSocketBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        for (Player p : be.getLevel().players()) {
            if (p.distanceToSqr(be.getBlockPos().getCenter()) < HELD_NEAR * HELD_NEAR
                    && (HofundItem.isHofund(p.getMainHandItem()) || HofundItem.isHofund(p.getOffhandItem()))) {
                return;
            }
        }
        float time = be.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, 1.45, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 0.8F));
        pose.mulPose(Axis.ZP.rotationDegrees(135.0F));
        pose.scale(1.7F, 1.7F, 1.7F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.HOFUND.get()), ItemDisplayContext.FIXED, light, overlay,
                pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }

    // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
    public AABB getRenderBoundingBox(HofundSocketBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 2, 0);
    }
}
