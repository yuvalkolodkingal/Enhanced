package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.kolodkingal.enhanced.client.render.HeroPoses;
import com.kolodkingal.enhanced.compat.ArmourPose;
import com.kolodkingal.enhanced.compat.RenderBridge;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.11 (3.14.1): the armour wears the hero's pose. From 1.21.9 the armour layer's models are posed when they are drawn,
 * by vanilla from the render state, instead of being copied from the player's model: a flying hero's armour stood upright
 * on his flying body. The pose the hero's model was given this frame is put on them (as 1.21.1's
 * {@code copyPropertiesTo}).
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void enhanced$wearTheHeroPose(HumanoidRenderState state, CallbackInfo ci) {
        if (ArmourPose.marked(this) && RenderBridge.entity(state) instanceof Player player) {
            HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
            boolean before = !com.kolodkingal.enhanced.client.dev.FlickerProbe.ENABLED || HeroPoses.wears(model, player, state.ageInTicks);
            HeroPoses.dress(model, player, state.ageInTicks);
            if (com.kolodkingal.enhanced.client.dev.FlickerProbe.ENABLED) {
                com.kolodkingal.enhanced.client.dev.FlickerProbe.armour(player, before, HeroPoses.wears(model, player, state.ageInTicks));
            }
        }
    }
}
