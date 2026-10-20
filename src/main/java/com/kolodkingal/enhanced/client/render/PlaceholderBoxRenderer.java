package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;

/**
 * A skeleton's stand-in for an entity whose look is a lane's (3.8: Eternity, the Hulkbuster's frame, the nanotech
 * drones, Redwing): the outline of its box in a dark colour, so it can be seen and found until the lane draws it.
 */
public class PlaceholderBoxRenderer<T extends Entity> extends EntityRenderer<T> {
    private final int color;

    public PlaceholderBoxRenderer(EntityRendererProvider.Context context, int color) {
        super(context);
        this.color = color;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        AABB box = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
