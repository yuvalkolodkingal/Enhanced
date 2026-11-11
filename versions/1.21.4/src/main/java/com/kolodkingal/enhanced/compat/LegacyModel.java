package com.kolodkingal.enhanced.compat;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.4 only: a model of the mod written against the 1.21.1 model (posed from the entity). It is posed from the render
 * state's entity ({@link RenderBridge}), with the same numbers 1.21.1 gave it.
 */
public abstract class LegacyModel<T extends Entity> extends EntityModel<LivingEntityRenderState> {
    /** 1.21.1's model fields, set from the entity before posing. */
    public float attackTime;
    public boolean riding;
    public boolean young;

    protected LegacyModel(ModelPart root) {
        super(root);
    }

    public abstract void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch);

    @Override
    @SuppressWarnings("unchecked")
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        Entity entity = RenderBridge.entity(state);
        if (entity != null) {
            attackTime = entity instanceof net.minecraft.world.entity.LivingEntity living ? living.getAttackAnim(state.partialTick) : 0.0F;
            riding = entity.isPassenger();
            young = state.isBaby;
            prepareMobModel((T) entity, state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
            setupAnim((T) entity, state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot, state.xRot);
        }
    }

    /** 1.21.1's prepareMobModel. */
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
    }
}
