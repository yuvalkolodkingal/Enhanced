package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;

/** The first-person arm (empty hand) is about to be drawn; cancelling hides it. */
public class RenderArmEvent implements Cancellable {
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;
    private final AbstractClientPlayer player;
    private final HumanoidArm arm;
    private boolean canceled;

    public RenderArmEvent(
            PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, AbstractClientPlayer player,
            HumanoidArm arm) {
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
        this.player = player;
        this.arm = arm;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getMultiBufferSource() {
        return multiBufferSource;
    }

    public int getPackedLight() {
        return packedLight;
    }

    public AbstractClientPlayer getPlayer() {
        return player;
    }

    public HumanoidArm getArm() {
        return arm;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
