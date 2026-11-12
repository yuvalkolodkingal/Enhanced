package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;

/**
 * 1.21.11 only: a {@code HumanoidMobRenderer} of the mod written against 1.21.1 (see {@link LegacyLiving}). From 1.21.9
 * a renderer submits instead of drawing: {@link #submit} gives the 1.21.1 render a {@link SubmitBuffers} and submits
 * what it drew; the vanilla body ({@link #vanillaRender}) is drawn there at once as 1.21.4 drew it, the layers submitted.
 */
public abstract class LegacyHumanoidRenderer<T extends Mob, M extends net.minecraft.client.model.HumanoidModel<AvatarRenderState>> extends net.minecraft.client.renderer.entity.HumanoidMobRenderer<T, AvatarRenderState, M>
        implements LegacyLiving.Hooks<T> {
    private AvatarRenderState current;
    private double distance;
    private SubmitNodeCollector collector;
    private CameraRenderState camera;

    protected LegacyHumanoidRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public AvatarRenderState legacyCurrent() {
        return current;
    }

    @Override
    public void legacySetCurrent(AvatarRenderState state) {
        current = state;
    }

    @Override
    public final void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        SubmitNodeCollector beforeCollector = this.collector;
        CameraRenderState beforeCamera = this.camera;
        this.collector = collector;
        this.camera = camera;
        SubmitBuffers buffers = new SubmitBuffers(collector);
        buffers.outline(state.outlineColor);
        try {
            LegacyLiving.render(this, state, pose, buffers, state.lightCoords);
        } finally {
            buffers.flush();
            this.collector = beforeCollector;
            this.camera = beforeCamera;
        }
    }

    /** What 1.21.4's {@code LivingEntityRenderer.render} drew: the body at once into the buffers, the layers submitted. */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void vanillaRender(AvatarRenderState state, PoseStack pose, MultiBufferSource buffers, int light) {
        SubmitNodeCollector c = collector;
        if (c == null) {
            return;
        }
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Pre(
                state, this, state.partialTick, pose, c)).isCanceled()) {
            return;
        }
        pose.pushPose();
        if (state.hasPose(Pose.SLEEPING) && state.bedOrientation != null) {
            float f = state.eyeHeight - 0.1F;
            pose.translate(-state.bedOrientation.getStepX() * f, 0.0F, -state.bedOrientation.getStepZ() * f);
        }
        float s = state.scale;
        pose.scale(s, s, s);
        this.setupRotations(state, pose, state.bodyRot, s);
        pose.scale(-1.0F, -1.0F, 1.0F);
        this.scale(state, pose);
        pose.translate(0.0F, -1.501F, 0.0F);
        boolean visible = this.isBodyVisible(state);
        boolean translucent = !visible && !state.isInvisibleToPlayer;
        RenderType type = this.getRenderType(state, visible, translucent, state.appearsGlowing());
        if (type != null) {
            int overlay = getOverlayCoords(state, this.getWhiteOverlayProgress(state));
            int color = ARGB.multiply(translucent ? 654311423 : -1, this.getModelTint(state));
            this.model.setupAnim(state);
            this.model.renderToBuffer(pose, buffers.getBuffer(type), light, overlay, color);
        }
        if (buffers instanceof SubmitBuffers sb) {
            sb.flush();
        }
        if (this.shouldRenderLayers(state) && !this.layers.isEmpty()) {
            this.model.setupAnim(state);
            for (RenderLayer<AvatarRenderState, M> layer : this.layers) {
                layer.submit(pose, c, light, state, state.yRot, state.xRot);
            }
        }
        pose.popPose();
        if (state.leashStates != null) {
            for (net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState leash : state.leashStates) {
                c.submitLeash(pose, leash);
            }
        }
        this.submitNameTag(state, pose, c, camera);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Post(
                state, this, state.partialTick, pose, c));
    }

    @Override
    public void legacyRender(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** 1.21.1's render: this default draws the model as vanilla does. */
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        vanillaRender(current, pose, buffers, light);
    }

    @Override
    public final Identifier getTextureLocation(AvatarRenderState state) {
        return LegacyLiving.texture(this, state);
    }

    @Override
    public abstract Identifier getTextureLocation(T entity);

    @Override
    @SuppressWarnings("unchecked")
    protected final void scale(AvatarRenderState state, PoseStack pose) {
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
    protected final void setupRotations(AvatarRenderState state, PoseStack pose, float bodyRot, float scale) {
        Entity entity = RenderBridge.entity(state);
        if (!(entity instanceof LivingEntity)) {
            super.setupRotations(state, pose, bodyRot, scale);
            return;
        }
        AvatarRenderState before = current;
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
    protected final boolean isShaking(AvatarRenderState state) {
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
    private T legacyEntityOf(AvatarRenderState state) {
        Entity entity = state == null ? null : RenderBridge.entity(state);
        return entity instanceof LivingEntity ? (T) entity : null;
    }

    private AvatarRenderState legacyHookState;

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
    protected final float getWhiteOverlayProgress(AvatarRenderState state) {
        T entity = legacyEntityOf(state);
        return entity != null ? getWhiteOverlayProgress(entity, state.partialTick) : 0.0F;
    }

    /** 1.21.1's getWhiteOverlayProgress. */
    protected float getWhiteOverlayProgress(T entity, float partialTick) {
        return 0.0F;
    }

    @Override
    protected final float getShadowRadius(AvatarRenderState state) {
        T entity = legacyEntityOf(state);
        legacyHookState = state;
        return entity != null ? getShadowRadius(entity) : super.getShadowRadius(state);
    }

    /** 1.21.1's getShadowRadius. */
    protected float getShadowRadius(T entity) {
        return super.getShadowRadius(legacyHookState);
    }

    @Override
    protected final RenderType getRenderType(AvatarRenderState state, boolean visible, boolean translucent, boolean glowing) {
        T entity = legacyEntityOf(state);
        legacyHookState = state;
        return entity != null ? getRenderType(entity, visible, translucent, glowing)
                : super.getRenderType(state, visible, translucent, glowing);
    }

    /** 1.21.1's getRenderType. */
    protected RenderType getRenderType(T entity, boolean visible, boolean translucent, boolean glowing) {
        return super.getRenderType(legacyHookState, visible, translucent, glowing);
    }

    @Override
    protected final void submitNameTag(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector,
            CameraRenderState camera) {
        T entity = legacyEntityOf(state);
        if (entity == null || state.nameTag == null) {
            super.submitNameTag(state, pose, collector, camera);
            return;
        }
        AvatarRenderState beforeState = legacyHookState;
        SubmitNodeCollector beforeCollector = this.collector;
        CameraRenderState beforeCamera = this.camera;
        legacyHookState = state;
        this.collector = collector;
        this.camera = camera;
        SubmitBuffers buffers = new SubmitBuffers(collector);
        try {
            renderNameTag(entity, state.nameTag, pose, buffers, state.lightCoords, state.partialTick);
        } finally {
            buffers.flush();
            legacyHookState = beforeState;
            this.collector = beforeCollector;
            this.camera = beforeCamera;
        }
    }

    /** 1.21.1's renderNameTag: this default submits vanilla's. */
    protected void renderNameTag(T entity, net.minecraft.network.chat.Component name, PoseStack pose,
            MultiBufferSource buffers, int light, float partialTick) {
        if (legacyHookState != null && collector != null) {
            super.submitNameTag(legacyHookState, pose, collector, camera);
        }
    }
}
