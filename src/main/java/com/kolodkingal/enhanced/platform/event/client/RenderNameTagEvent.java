package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.EntityEvent;
import com.kolodkingal.enhanced.platform.event.TriState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** An entity's name tag is about to be drawn: change its text or whether it shows. */
public class RenderNameTagEvent extends EntityEvent {
    private Component content;
    private final EntityRenderer<?> entityRenderer;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;
    private final float partialTick;
    private TriState canRender;

    public RenderNameTagEvent(
            Entity entity, Component content, EntityRenderer<?> entityRenderer, PoseStack poseStack,
            MultiBufferSource multiBufferSource, int packedLight, float partialTick) {
        super(entity);
        this.content = content;
        this.entityRenderer = entityRenderer;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
        this.partialTick = partialTick;
        this.canRender = TriState.DEFAULT;
    }

    public Component getContent() {
        return content;
    }

    public void setContent(Component content) {
        this.content = content;
    }

    public EntityRenderer<?> getEntityRenderer() {
        return entityRenderer;
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

    public float getPartialTick() {
        return partialTick;
    }

    public TriState canRender() {
        return canRender;
    }

    public void setCanRender(TriState canRender) {
        this.canRender = canRender;
    }
}
