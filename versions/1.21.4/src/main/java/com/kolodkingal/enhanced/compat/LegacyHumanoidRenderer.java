package com.kolodkingal.enhanced.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** 1.21.4 only: a {@code HumanoidMobRenderer} of the mod written against 1.21.1 (see {@link LegacyLiving}). */
public abstract class LegacyHumanoidRenderer<T extends Mob, M extends net.minecraft.client.model.HumanoidModel<PlayerRenderState>> extends net.minecraft.client.renderer.entity.HumanoidMobRenderer<T, PlayerRenderState, M>
        implements LegacyLiving.Hooks<T> {
    private PlayerRenderState current;
    private double distance;

    protected LegacyHumanoidRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    @Override
    public PlayerRenderState createRenderState() {
        return new PlayerRenderState();
    }

    @Override
    public PlayerRenderState legacyCurrent() {
        return current;
    }

    @Override
    public void legacySetCurrent(PlayerRenderState state) {
        current = state;
    }

    @Override
    public final void render(PlayerRenderState state, PoseStack pose, MultiBufferSource buffers, int light) {
        LegacyLiving.render(this, state, pose, buffers, light);
    }

    @Override
    public void vanillaRender(PlayerRenderState state, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(state, pose, buffers, light);
    }

    @Override
    public void legacyRender(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** 1.21.1's render: this default draws the model as vanilla does. */
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(current, pose, buffers, light);
    }

    @Override
    public final ResourceLocation getTextureLocation(PlayerRenderState state) {
        return LegacyLiving.texture(this, state);
    }

    @Override
    public abstract ResourceLocation getTextureLocation(T entity);

    @Override
    @SuppressWarnings("unchecked")
    protected final void scale(PlayerRenderState state, PoseStack pose) {
        super.scale(state, pose);
        Entity entity = RenderBridge.entity(state);
        if (entity instanceof LivingEntity) {
            scale((T) entity, pose, state.partialTick);
        }
    }

    /** 1.21.1's scale. */
    protected void scale(T entity, PoseStack pose, float partialTick) {
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final void setupRotations(PlayerRenderState state, PoseStack pose, float bodyRot, float scale) {
        Entity entity = RenderBridge.entity(state);
        if (!(entity instanceof LivingEntity)) {
            super.setupRotations(state, pose, bodyRot, scale);
            return;
        }
        PlayerRenderState before = current;
        current = state;
        try {
            setupRotations((T) entity, pose, state.ageInTicks, bodyRot, state.partialTick, scale);
        } finally {
            current = before;
        }
    }

    /** 1.21.1's setupRotations: this default turns the body as vanilla does. */
    protected void setupRotations(T entity, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(current, pose, yBodyRot, scale);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final boolean isShaking(PlayerRenderState state) {
        Entity entity = RenderBridge.entity(state);
        return entity instanceof LivingEntity ? isShaking((T) entity) : super.isShaking(state);
    }

    /** 1.21.1's isShaking. */
    protected boolean isShaking(T entity) {
        return entity.isFullyFrozen();
    }

    @Override
    protected final boolean shouldShowName(T entity, double distanceSq) {
        distance = distanceSq;
        return shouldShowName(entity);
    }

    /** 1.21.1's shouldShowName. */
    protected boolean shouldShowName(T entity) {
        return super.shouldShowName(entity, distance);
    }
    // ---- 1.21.1's per-entity hooks, called with the entity of the render state ----

    @SuppressWarnings("unchecked")
    private T legacyEntityOf(PlayerRenderState state) {
        Entity entity = state == null ? null : RenderBridge.entity(state);
        return entity instanceof LivingEntity ? (T) entity : null;
    }

    private PlayerRenderState legacyHookState;

    @Override
    protected final float getFlipDegrees() {
        T entity = legacyEntityOf(legacyCurrent());
        return entity != null ? getFlipDegrees(entity) : 90.0F;
    }

    /** 1.21.1's getFlipDegrees. */
    protected float getFlipDegrees(T entity) {
        return 90.0F;
    }

    @Override
    protected final float getWhiteOverlayProgress(PlayerRenderState state) {
        T entity = legacyEntityOf(state);
        return entity != null ? getWhiteOverlayProgress(entity, state.partialTick) : 0.0F;
    }

    /** 1.21.1's getWhiteOverlayProgress. */
    protected float getWhiteOverlayProgress(T entity, float partialTick) {
        return 0.0F;
    }

    @Override
    protected final float getShadowRadius(PlayerRenderState state) {
        T entity = legacyEntityOf(state);
        legacyHookState = state;
        return entity != null ? getShadowRadius(entity) : super.getShadowRadius(state);
    }

    /** 1.21.1's getShadowRadius. */
    protected float getShadowRadius(T entity) {
        return super.getShadowRadius(legacyHookState);
    }

    @Override
    protected final net.minecraft.client.renderer.RenderType getRenderType(PlayerRenderState state, boolean visible,
            boolean translucent, boolean glowing) {
        T entity = legacyEntityOf(state);
        legacyHookState = state;
        return entity != null ? getRenderType(entity, visible, translucent, glowing)
                : super.getRenderType(state, visible, translucent, glowing);
    }

    /** 1.21.1's getRenderType. */
    protected net.minecraft.client.renderer.RenderType getRenderType(T entity, boolean visible, boolean translucent,
            boolean glowing) {
        return super.getRenderType(legacyHookState, visible, translucent, glowing);
    }

    @Override
    protected final void renderNameTag(PlayerRenderState state, net.minecraft.network.chat.Component name,
            PoseStack pose, MultiBufferSource buffers, int light) {
        T entity = legacyEntityOf(state);
        legacyHookState = state;
        if (entity != null) {
            renderNameTag(entity, name, pose, buffers, light, state.partialTick);
        } else {
            super.renderNameTag(state, name, pose, buffers, light);
        }
    }

    /** 1.21.1's renderNameTag. */
    protected void renderNameTag(T entity, net.minecraft.network.chat.Component name, PoseStack pose,
            MultiBufferSource buffers, int light, float partialTick) {
        super.renderNameTag(legacyHookState, name, pose, buffers, light);
    }
}
