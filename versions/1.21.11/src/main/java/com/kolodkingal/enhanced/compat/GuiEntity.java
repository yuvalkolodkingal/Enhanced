package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1.21.11 only: 1.21.1 drew an entity in a screen with the entity render dispatcher into the GUI's buffers (feet at a
 * point, so many pixels a block, turned). From 1.21.6 an entity in the GUI is a picture in the picture: drawn into a
 * texture of its own box, which is then drawn in the screen. The box is three blocks wide and tall round the feet, the
 * entity drawn in it as 1.21.1 drew it.
 */
public final class GuiEntity {
    private GuiEntity() {
    }

    /**
     * Draws {@code entity} with its feet at ({@code x}, {@code y}), {@code scale} pixels a block, turned {@code rotation}
     * (with 1.21.1's flip about the screen's normal), the camera turned {@code cameraAngle} for its lighting (or not).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void draw(GuiGraphics g, Entity entity, float x, float y, float scale, Quaternionf rotation,
                            @Nullable Quaternionf cameraAngle, float partialTick) {
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, partialTick);
        state.lightCoords = 0xF000F0;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.deathTime = 0.0F;
        }
        int x0 = Math.round(x - 1.5F * scale);
        int x1 = Math.round(x + 1.5F * scale);
        int y0 = Math.round(y - 2.6F * scale);
        int y1 = Math.round(y + 0.6F * scale);
        float centreY = (y0 + y1) / 2.0F;
        // (the box's middle is the picture's origin: the feet are (y - centre) pixels below it, in blocks)
        Vector3f feet = new Vector3f((x - (x0 + x1) / 2.0F) / scale, (y - centreY) / scale, 0.0F);
        g.submitEntityRenderState(state, scale, feet, new Quaternionf(rotation),
                cameraAngle == null ? null : new Quaternionf(cameraAngle), x0, y0, x1, y1);
    }
}
