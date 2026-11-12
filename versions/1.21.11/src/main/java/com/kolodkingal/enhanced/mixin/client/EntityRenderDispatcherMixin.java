package com.kolodkingal.enhanced.mixin.client;
//#norenames

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 1.21.11 only: from 1.21.9 the dispatcher finds a render state's renderer by its type, and any avatar's state goes to
 * the player renderers. The mod's living renderers draw from avatar states (compat.LegacyLivingRenderer and the others:
 * the player model's state): a state of an entity that is not a player or a mannequin goes to its type's renderer.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
            at = @At("HEAD"), cancellable = true)
    private void enhanced$ownRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        if (state instanceof AvatarRenderState && state.entityType != EntityType.PLAYER && state.entityType != EntityType.MANNEQUIN) {
            EntityRenderer<?, ?> own = renderers.get(state.entityType);
            if (own != null) {
                cir.setReturnValue(own);
            }
        }
    }
}
