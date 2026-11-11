package com.kolodkingal.enhanced.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.4 only: a renderer of the mod written against the 1.21.1 renderer, which draws from the entity: it gets the
 * render state's entity ({@link RenderBridge}), and its {@code super.render(...)} draws what 1.21.1's did (the name).
 */
public abstract class LegacyEntityRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {
    private EntityRenderState current;

    protected LegacyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void render(EntityRenderState state, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity entity = RenderBridge.entity(state);
        if (entity == null) {
            super.render(state, pose, buffers, light);
            return;
        }
        EntityRenderState before = current;
        current = state;
        try {
            render((T) entity, Mth.rotLerp(state.partialTick, entity.yRotO, entity.getYRot()), state.partialTick, pose, buffers, light);
        } finally {
            current = before;
        }
    }

    /** 1.21.1's render: draws the entity; this default draws what vanilla adds (the name tag). */
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (current != null) {
            super.render(current, pose, buffers, light);
        }
    }

    /** 1.21.1's texture of the entity (nothing asks for it from 1.21.2; kept for the renderers that use it themselves). */
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
