package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;

/** A player is drawn (NeoForge's RenderPlayerEvent). */
public abstract class RenderPlayerEvent extends PlayerEvent {
    private final PlayerRenderer renderer;
    private final float partialTick;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;

    protected RenderPlayerEvent(
            Player entity, PlayerRenderer renderer, float partialTick, PoseStack poseStack,
            MultiBufferSource multiBufferSource, int packedLight) {
        super(entity);
        this.renderer = renderer;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
    }

    public PlayerRenderer getRenderer() {
        return renderer;
    }

    public float getPartialTick() {
        return partialTick;
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

    /** Before a player is drawn; cancelling hides him. */
    public static class Pre extends RenderPlayerEvent implements Cancellable {
        private boolean canceled;

        public Pre(
                Player entity, PlayerRenderer renderer, float partialTick, PoseStack poseStack,
                MultiBufferSource multiBufferSource, int packedLight) {
            super(entity, renderer, partialTick, poseStack, multiBufferSource, packedLight);
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

    /** After a player was drawn. */
    public static class Post extends RenderPlayerEvent {
        public Post(
                Player entity, PlayerRenderer renderer, float partialTick, PoseStack poseStack,
                MultiBufferSource multiBufferSource, int packedLight) {
            super(entity, renderer, partialTick, poseStack, multiBufferSource, packedLight);
        }
    }
}
