package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.HeroPoses;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The heroes' custom poses, applied after the vanilla animation. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void enhanced$heroPoses(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                    float netHeadYaw, float headPitch, CallbackInfo ci) {
        HeroPoses.apply((PlayerModel<?>) (Object) this, entity, limbSwing, limbSwingAmount, ageInTicks);
    }
}
