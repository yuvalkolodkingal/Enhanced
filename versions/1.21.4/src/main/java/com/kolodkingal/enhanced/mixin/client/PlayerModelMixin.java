package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.HeroPoses;
import com.kolodkingal.enhanced.compat.RenderBridge;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The heroes' poses, blended on top of the vanilla animation (1.21.4: from the render state's entity). */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("HEAD"))
    private void enhanced$resetPoses(PlayerRenderState state, CallbackInfo ci) {
        RenderBridge.pose = state;
        HeroPoses.reset((PlayerModel) (Object) this);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("TAIL"))
    private void enhanced$heroPoses(PlayerRenderState state, CallbackInfo ci) {
        RenderBridge.pose = state;
        if (RenderBridge.entity(state) instanceof LivingEntity entity) {
            HeroPoses.apply((PlayerModel) (Object) this, entity, state.walkAnimationPos, state.walkAnimationSpeed,
                    state.ageInTicks);
        }
    }
}
