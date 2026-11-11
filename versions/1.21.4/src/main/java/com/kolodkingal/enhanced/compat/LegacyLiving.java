package com.kolodkingal.enhanced.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.21.4 only: what the living renderers of the mod that were written against 1.21.1 share ({@link LegacyLivingRenderer},
 * {@link LegacyMobRenderer}, {@link LegacyHumanoidRenderer}): every one draws a {@link PlayerRenderState} (the state of the
 * player model, which some of them use, and a living entity's state for the others), and calls back the 1.21.1
 * methods with the render state's entity.
 */
final class LegacyLiving {
    private LegacyLiving() {
    }

    interface Hooks<T extends LivingEntity> {
        PlayerRenderState legacyCurrent();

        void legacySetCurrent(PlayerRenderState state);

        void legacyRender(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light);

        void vanillaRender(PlayerRenderState state, PoseStack pose, MultiBufferSource buffers, int light);

        ResourceLocation getTextureLocation(T entity);
    }

    @SuppressWarnings("unchecked")
    static <T extends LivingEntity> void render(Hooks<T> hooks, PlayerRenderState state, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity entity = RenderBridge.entity(state);
        if (!(entity instanceof LivingEntity)) {
            hooks.vanillaRender(state, pose, buffers, light);
            return;
        }
        PlayerRenderState before = hooks.legacyCurrent();
        hooks.legacySetCurrent(state);
        // (1.21.1's renderers set the model's crouch and seat before drawing: RenderBridge.pose is this state now)
        RenderBridge.pose = state;
        try {
            hooks.legacyRender((T) entity, state.bodyRot, state.partialTick, pose, buffers, light);
        } finally {
            hooks.legacySetCurrent(before);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends LivingEntity> ResourceLocation texture(Hooks<T> hooks, PlayerRenderState state) {
        Entity entity = RenderBridge.entity(state);
        return entity instanceof LivingEntity ? hooks.getTextureLocation((T) entity) : state.skin.texture();
    }
}
