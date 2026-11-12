package com.kolodkingal.enhanced.compat;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.11 (as 1.21.4): from 1.21.2 a renderer draws from a render state, not from the entity. The mod's poses, layers
 * and effects read the entity (its bending, its gestures), so every render state carries the entity it was made from
 * (NeoForge's render data), and {@link #entity} gives it back.
 */
public final class RenderBridge {
    private static final ContextKey<Entity> ENTITY = new ContextKey<>(Enhanced.id("entity"));

    /**
     * The render state of the player the poses are being applied to now (set by PlayerModelMixin): what was the
     * model's own arm poses, crouch and swing in 1.21.1.
     */
    public static AvatarRenderState pose = new AvatarRenderState();

    private RenderBridge() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) EntityRenderer.class, (entity, state) -> {
            ((EntityRenderState) state).setRenderData(ENTITY, (Entity) entity);
            STATES.put((Entity) entity, (EntityRenderState) state);
        });
    }

    /** The entity a render state was made from (null for one made by hand). */
    @Nullable
    public static Entity entity(EntityRenderState state) {
        return state.getRenderData(ENTITY);
    }

    /** The render state's entity as the type the caller expects (a layer of that entity's renderer). */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <E> E entityAs(EntityRenderState state) {
        return (E) state.getRenderData(ENTITY);
    }

    private static final java.util.Map<Entity, EntityRenderState> STATES = new java.util.WeakHashMap<>();

    /** The last render state made from this player (for a layer's helper that poses its own model). */
    public static AvatarRenderState state(Entity entity) {
        return STATES.get(entity) instanceof AvatarRenderState p ? p : new AvatarRenderState();
    }

    /** The texture an entity's renderer draws it with (1.21.1's layers asked their renderer by entity). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Identifier texture(Entity entity) {
        if (entity instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            EntityRenderer<?, ?> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (r instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living) {
                return living.getTextureLocation((net.minecraft.client.renderer.entity.state.LivingEntityRenderState) state(entity));
            }
            return player.getSkin().body().texturePath();
        }
        EntityRenderer r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        try {
            java.lang.reflect.Method m = r.getClass().getMethod("getTextureLocation", entity.getClass());
            return (Identifier) m.invoke(r, entity);
        } catch (ReflectiveOperationException e) {
            for (java.lang.reflect.Method m : r.getClass().getMethods()) {
                if (m.getName().equals("getTextureLocation") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isInstance(entity)) {
                    try {
                        return (Identifier) m.invoke(r, entity);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
            return net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
        }
    }

    /** The camera of the frame being drawn (a renderer submits with it). */
    public static CameraRenderState camera() {
        return Minecraft.getInstance().gameRenderer.getLevelRenderState().cameraRenderState;
    }

    /**
     * 1.21.1's {@code renderer.render(entity, yaw, partialTick, pose, buffers, light)}: from 1.21.9 the renderer submits,
     * to the collector of the {@link SubmitBuffers} it is drawn into (nothing is drawn into other buffers).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(EntityRenderer<?, ?> renderer, Entity entity, float partialTick,
                              com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                              int light) {
        EntityRenderer raw = renderer;
        EntityRenderState state = raw.createRenderState(entity, partialTick);
        state.lightCoords = light;
        if (buffers instanceof SubmitBuffers sb) {
            sb.flush();
        }
        SubmitBuffers.submit(buffers, collector -> raw.submit(state, pose, collector, camera()));
    }

    /** 1.21.1's {@code dispatcher.render(entity, x, y, z, partialTick, pose, buffers, light)} (with its renderer's offset). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(Entity entity, double x, double y, double z, float partialTick,
                              com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                              int light) {
        EntityRenderer raw = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = raw.createRenderState(entity, partialTick);
        state.lightCoords = light;
        net.minecraft.world.phys.Vec3 offset = raw.getRenderOffset(state);
        pose.pushPose();
        pose.translate(x + offset.x, y + offset.y, z + offset.z);
        if (buffers instanceof SubmitBuffers sb) {
            sb.flush();
        }
        SubmitBuffers.submit(buffers, collector -> raw.submit(state, pose, collector, camera()));
        pose.popPose();
    }
}
