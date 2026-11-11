package com.kolodkingal.enhanced.compat;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.21.4 only: a model of the mod on the humanoid model, written against the 1.21.1 model: posed from the entity, its
 * {@code super.setupAnim(entity, ...)} the vanilla humanoid animation with those numbers.
 */
public class LegacyHumanoidModel<T extends LivingEntity> extends net.minecraft.client.model.HumanoidModel<PlayerRenderState> {
    private PlayerRenderState current;
    /** 1.21.1's model fields, read from the render state before posing and given back to it for the vanilla pose. */
    public boolean crouching;
    public float swimAmount;
    public float attackTime;
    public boolean riding;

    public LegacyHumanoidModel(ModelPart root) {
        super(Meshes.humanoid(root));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setupAnim(PlayerRenderState state) {
        Entity entity = RenderBridge.entity(state);
        if (entity instanceof LivingEntity) {
            current = state;
            crouching = state.isCrouching;
            swimAmount = state.swimAmount;
            attackTime = state.attackTime;
            riding = state.isPassenger;
            prepareMobModel((T) entity, state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
            setupAnim((T) entity, state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot, state.xRot);
        } else {
            super.setupAnim(state);
        }
    }

    /** The vanilla humanoid animation with these numbers (a subclass's super call). */
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerRenderState state = current != null ? current : new PlayerRenderState();
        state.walkAnimationPos = limbSwing;
        state.walkAnimationSpeed = limbSwingAmount;
        state.ageInTicks = ageInTicks;
        state.yRot = netHeadYaw;
        state.xRot = headPitch;
        state.isCrouching = crouching;
        state.swimAmount = swimAmount;
        state.attackTime = attackTime;
        state.isPassenger = riding;
        super.setupAnim(state);
    }

    /** 1.21.1's prepareMobModel. */
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
    }

    /** 1.21.1's setupAttackAnimation. */
    protected void setupAttackAnimation(T entity, float ageInTicks) {
        PlayerRenderState state = current != null ? current : new PlayerRenderState();
        state.attackTime = attackTime;
        super.setupAttackAnimation(state, ageInTicks);
    }
}
