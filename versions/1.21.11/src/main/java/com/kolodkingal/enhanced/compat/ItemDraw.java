package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.11 only: 1.21.1's {@code ItemRenderer.renderStatic}. From 1.21.9 an item is resolved into a render state and
 * submitted; it is submitted to the collector of the {@link SubmitBuffers} it is drawn into (the mod draws items only
 * from its renderers and layers, which draw into one).
 */
public final class ItemDraw {
    private ItemDraw() {
    }

    public static void renderStatic(ItemStack stack, ItemDisplayContext context, int light, int overlay, PoseStack pose,
                                    MultiBufferSource buffers, @Nullable Level level, int seed) {
        submit(null, stack, context, light, overlay, pose, buffers, level, seed);
    }

    public static void renderStatic(@Nullable LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                    PoseStack pose, MultiBufferSource buffers, @Nullable Level level, int light, int overlay,
                                    int seed) {
        submit(entity, stack, context, light, overlay, pose, buffers, level, seed);
    }

    private static void submit(@Nullable LivingEntity entity, ItemStack stack, ItemDisplayContext context, int light,
                               int overlay, PoseStack pose, MultiBufferSource buffers, @Nullable Level level, int seed) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, context, level, entity, seed);
        SubmitBuffers.submit(buffers, collector -> state.submit(pose, collector, light, overlay, 0));
    }

    /** The collector a {@link SubmitBuffers} submits to (the game's for other buffers). */
    public static SubmitNodeCollector collector(MultiBufferSource buffers) {
        return buffers instanceof SubmitBuffers sb ? sb.collector()
                : Minecraft.getInstance().gameRenderer.getSubmitNodeStorage();
    }

    /** 1.21.4's {@code BeaconRenderer.renderBeaconBeam} (1.21.9: submitted). */
    public static void beaconBeam(PoseStack pose, MultiBufferSource buffers, net.minecraft.resources.Identifier texture,
                                  float partialTick, float textureScale, long gameTime, int yOffset, int height, int color,
                                  float beamRadius, float glowRadius) {
        float time = Math.floorMod(gameTime, 40) + partialTick;
        SubmitBuffers.submit(buffers, c -> net.minecraft.client.renderer.blockentity.BeaconRenderer.submitBeaconBeam(pose, c,
                texture, textureScale, time, yOffset, height, color, beamRadius, glowRadius));
    }
}
