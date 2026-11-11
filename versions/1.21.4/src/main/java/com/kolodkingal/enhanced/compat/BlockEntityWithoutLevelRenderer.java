package com.kolodkingal.enhanced.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.4 only: 1.21.1's item drawn by code. From 1.21.4 such an item is a special model ({@link LegacyItems}' special
 * renderer) that calls {@link #renderByItem}.
 */
public abstract class BlockEntityWithoutLevelRenderer {
    protected BlockEntityWithoutLevelRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
    }

    public void onResourceManagerReload(ResourceManager manager) {
    }

    public abstract void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                      MultiBufferSource buffer, int light, int overlay);
}
