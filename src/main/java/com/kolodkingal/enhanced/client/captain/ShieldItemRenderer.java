package com.kolodkingal.enhanced.client.captain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the Vibranium Shield as an item (3.8, PLAN 3.3): in a hand, raised, in the GUI, in a frame and on the ground,
 * by our round {@link ShieldModel} (the item's model is {@code builtin/entity}, its display transforms vanilla's
 * shield's; {@code IClientItemExtensions} hands the item to this renderer). Made on first use, when the game's model
 * set is ready.
 */
public final class ShieldItemRenderer extends BlockEntityWithoutLevelRenderer {
    @Nullable
    private static ShieldItemRenderer instance;
    @Nullable
    private ShieldModel model;

    private ShieldItemRenderer(Minecraft mc) {
        super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
    }

    /** The one renderer (made on first use). */
    public static ShieldItemRenderer get() {
        if (instance == null) {
            instance = new ShieldItemRenderer(Minecraft.getInstance());
        }
        return instance;
    }

    /** The model, baked from the game's model set (again after a resource reload). */
    public ShieldModel model() {
        if (model == null) {
            model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ShieldModel.LAYER));
        }
        return model;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        model = null;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        // (Vanilla's shield's frame: its display transforms are made for it.)
        poseStack.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffer, ShieldModel.renderType(), true,
                stack.hasFoil());
        model().renderAll(poseStack, vc, light, overlay);
        poseStack.popPose();
    }
}
