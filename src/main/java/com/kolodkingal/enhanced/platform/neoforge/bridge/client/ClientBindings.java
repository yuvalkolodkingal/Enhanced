package com.kolodkingal.enhanced.platform.neoforge.bridge.client;

import com.kolodkingal.enhanced.platform.event.TriState;
import com.kolodkingal.enhanced.platform.event.client.CalculateDetachedCameraDistanceEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.ComputeFovModifierEvent;
import com.kolodkingal.enhanced.platform.event.client.CustomizeGuiOverlayEvent;
import com.kolodkingal.enhanced.platform.event.client.FMLClientSetupEvent;
import com.kolodkingal.enhanced.platform.event.client.InputEvent;
import com.kolodkingal.enhanced.platform.event.client.MovementInputUpdateEvent;
import com.kolodkingal.enhanced.platform.event.client.RegisterClientCommandsEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderFrameEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderGuiEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderGuiLayerEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderHandEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderHighlightEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderNameTagEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderPlayerEvent;
import com.kolodkingal.enhanced.platform.event.client.ScreenEvent;
import com.kolodkingal.enhanced.platform.event.client.SelectMusicEvent;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import com.kolodkingal.enhanced.platform.neoforge.bridge.Binding;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generated from the port's event table (do not edit by hand): how each NeoForge client event becomes its
 * common event. An event with something to change (a setter, cancelling) gets an adapter that reads and writes the
 * NeoForge event itself, so NeoForge sees every change at once; any other is copied.
 */
public final class ClientBindings {
    private ClientBindings() {
    }

    public static void install(Map<Class<?>, Binding<?, ?>> table) {
        Binding.put(table, ClientTickEvent.Pre.class, net.neoforged.neoforge.client.event.ClientTickEvent.Pre.class,
                n -> new ClientTickEvent.Pre());
        Binding.put(table, ClientTickEvent.Post.class, net.neoforged.neoforge.client.event.ClientTickEvent.Post.class,
                n -> new ClientTickEvent.Post());
        Binding.put(table, ClientPlayerNetworkEvent.LoggingIn.class, net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn.class,
                n -> new ClientPlayerNetworkEvent.LoggingIn(n.getMultiPlayerGameMode(), n.getPlayer(), n.getConnection()));
        Binding.put(table, ClientPlayerNetworkEvent.LoggingOut.class, net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut.class,
                n -> new ClientPlayerNetworkEvent.LoggingOut(n.getMultiPlayerGameMode(), n.getPlayer(), n.getConnection()));
        Binding.put(table, ClientPlayerNetworkEvent.Clone.class, net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.Clone.class,
                n -> new ClientPlayerNetworkEvent.Clone(n.getMultiPlayerGameMode(), n.getNewPlayer(), n.getConnection(), n.getOldPlayer()));
        Binding.put(table, RenderLevelStageEvent.class, net.neoforged.neoforge.client.event.RenderLevelStageEvent.class,
                n -> new RenderLevelStageEvent(ClientConvert.stage(n.getStage()), n.getLevelRenderer(), n.getPoseStack(), n.getModelViewMatrix(), n.getProjectionMatrix(), n.getRenderTick(), n.getPartialTick(), n.getCamera(), n.getFrustum()));
        Binding.put(table, ViewportEvent.ComputeCameraAngles.class, net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles.class,
                NeoViewportEventComputeCameraAngles::new);
        Binding.put(table, ViewportEvent.ComputeFogColor.class, net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor.class,
                NeoViewportEventComputeFogColor::new);
        Binding.put(table, ViewportEvent.RenderFog.class, net.neoforged.neoforge.client.event.ViewportEvent.RenderFog.class,
                NeoViewportEventRenderFog::new);
        Binding.put(table, RenderPlayerEvent.Pre.class, net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre.class,
                NeoRenderPlayerEventPre::new);
        Binding.put(table, RenderPlayerEvent.Post.class, net.neoforged.neoforge.client.event.RenderPlayerEvent.Post.class,
                n -> new RenderPlayerEvent.Post(n.getEntity(), n.getRenderer(), n.getPartialTick(), n.getPoseStack(), n.getMultiBufferSource(), n.getPackedLight()));
        Binding.put(table, RenderArmEvent.class, net.neoforged.neoforge.client.event.RenderArmEvent.class,
                NeoRenderArmEvent::new);
        Binding.put(table, RenderHandEvent.class, net.neoforged.neoforge.client.event.RenderHandEvent.class,
                NeoRenderHandEvent::new);
        Binding.put(table, InputEvent.Key.class, net.neoforged.neoforge.client.event.InputEvent.Key.class,
                n -> new InputEvent.Key(n.getKey(), n.getScanCode(), n.getAction(), n.getModifiers()));
        Binding.put(table, InputEvent.MouseButton.Pre.class, net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre.class,
                NeoInputEventMouseButtonPre::new);
        Binding.put(table, InputEvent.MouseButton.Post.class, net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post.class,
                n -> new InputEvent.MouseButton.Post(n.getButton(), n.getAction(), n.getModifiers()));
        Binding.put(table, InputEvent.MouseScrollingEvent.class, net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent.class,
                NeoInputEventMouseScrollingEvent::new);
        Binding.put(table, InputEvent.InteractionKeyMappingTriggered.class, net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered.class,
                NeoInputEventInteractionKeyMappingTriggered::new);
        Binding.put(table, RenderGuiLayerEvent.Pre.class, net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre.class,
                NeoRenderGuiLayerEventPre::new);
        Binding.put(table, RenderGuiLayerEvent.Post.class, net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post.class,
                n -> new RenderGuiLayerEvent.Post(n.getGuiGraphics(), n.getPartialTick(), n.getName(), n.getLayer()));
        Binding.put(table, RenderGuiEvent.Pre.class, net.neoforged.neoforge.client.event.RenderGuiEvent.Pre.class,
                NeoRenderGuiEventPre::new);
        Binding.put(table, RenderGuiEvent.Post.class, net.neoforged.neoforge.client.event.RenderGuiEvent.Post.class,
                n -> new RenderGuiEvent.Post(n.getGuiGraphics(), n.getPartialTick()));
        Binding.put(table, RenderFrameEvent.Pre.class, net.neoforged.neoforge.client.event.RenderFrameEvent.Pre.class,
                n -> new RenderFrameEvent.Pre(n.getPartialTick()));
        Binding.put(table, RenderFrameEvent.Post.class, net.neoforged.neoforge.client.event.RenderFrameEvent.Post.class,
                n -> new RenderFrameEvent.Post(n.getPartialTick()));
        Binding.put(table, RenderHighlightEvent.Block.class, net.neoforged.neoforge.client.event.RenderHighlightEvent.Block.class,
                NeoRenderHighlightEventBlock::new);
        Binding.put(table, CustomizeGuiOverlayEvent.BossEventProgress.class, net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress.class,
                NeoCustomizeGuiOverlayEventBossEventProgress::new);
        Binding.put(table, ScreenEvent.Opening.class, net.neoforged.neoforge.client.event.ScreenEvent.Opening.class,
                NeoScreenEventOpening::new);
        Binding.put(table, SelectMusicEvent.class, net.neoforged.neoforge.client.event.SelectMusicEvent.class,
                NeoSelectMusicEvent::new);
        Binding.put(table, MovementInputUpdateEvent.class, net.neoforged.neoforge.client.event.MovementInputUpdateEvent.class,
                n -> new MovementInputUpdateEvent(n.getEntity(), n.getInput()));
        Binding.put(table, ComputeFovModifierEvent.class, net.neoforged.neoforge.client.event.ComputeFovModifierEvent.class,
                NeoComputeFovModifierEvent::new);
        Binding.put(table, CalculateDetachedCameraDistanceEvent.class, net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent.class,
                NeoCalculateDetachedCameraDistanceEvent::new);
        Binding.put(table, RenderNameTagEvent.class, net.neoforged.neoforge.client.event.RenderNameTagEvent.class,
                NeoRenderNameTagEvent::new);
        Binding.put(table, RegisterClientCommandsEvent.class, net.neoforged.neoforge.client.event.RegisterClientCommandsEvent.class,
                n -> new RegisterClientCommandsEvent(n.getDispatcher(), n.getBuildContext()));
        Binding.put(table, FMLClientSetupEvent.class, net.neoforged.fml.event.lifecycle.FMLClientSetupEvent.class,
                NeoFMLClientSetupEvent::new);
    }

    private static final class NeoViewportEventComputeCameraAngles extends ViewportEvent.ComputeCameraAngles {
        private final net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles n;

        NeoViewportEventComputeCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles n) {
            super(n.getCamera(), n.getPartialTick(), n.getYaw(), n.getPitch(), n.getRoll());
            this.n = n;
        }

        @Override
        public float getYaw() {
            return n.getYaw();
        }

        @Override
        public void setYaw(float yaw) {
            n.setYaw(yaw);
        }

        @Override
        public float getPitch() {
            return n.getPitch();
        }

        @Override
        public void setPitch(float pitch) {
            n.setPitch(pitch);
        }

        @Override
        public float getRoll() {
            return n.getRoll();
        }

        @Override
        public void setRoll(float roll) {
            n.setRoll(roll);
        }
    }

    private static final class NeoViewportEventComputeFogColor extends ViewportEvent.ComputeFogColor {
        private final net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor n;

        NeoViewportEventComputeFogColor(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor n) {
            super(n.getCamera(), n.getPartialTick(), n.getRed(), n.getGreen(), n.getBlue());
            this.n = n;
        }

        @Override
        public float getRed() {
            return n.getRed();
        }

        @Override
        public void setRed(float red) {
            n.setRed(red);
        }

        @Override
        public float getGreen() {
            return n.getGreen();
        }

        @Override
        public void setGreen(float green) {
            n.setGreen(green);
        }

        @Override
        public float getBlue() {
            return n.getBlue();
        }

        @Override
        public void setBlue(float blue) {
            n.setBlue(blue);
        }
    }

    private static final class NeoViewportEventRenderFog extends ViewportEvent.RenderFog {
        private final net.neoforged.neoforge.client.event.ViewportEvent.RenderFog n;

        NeoViewportEventRenderFog(net.neoforged.neoforge.client.event.ViewportEvent.RenderFog n) {
            super(n.getCamera(), n.getPartialTick(), n.getMode(), n.getType(), n.getFarPlaneDistance(),
                    n.getNearPlaneDistance(), n.getFogShape());
            this.n = n;
        }

        @Override
        public float getFarPlaneDistance() {
            return n.getFarPlaneDistance();
        }

        @Override
        public void setFarPlaneDistance(float farPlaneDistance) {
            n.setFarPlaneDistance(farPlaneDistance);
        }

        @Override
        public float getNearPlaneDistance() {
            return n.getNearPlaneDistance();
        }

        @Override
        public void setNearPlaneDistance(float nearPlaneDistance) {
            n.setNearPlaneDistance(nearPlaneDistance);
        }

        @Override
        public FogShape getFogShape() {
            return n.getFogShape();
        }

        @Override
        public void setFogShape(FogShape fogShape) {
            n.setFogShape(fogShape);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoRenderPlayerEventPre extends RenderPlayerEvent.Pre {
        private final net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre n;

        NeoRenderPlayerEventPre(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre n) {
            super(n.getEntity(), n.getRenderer(), n.getPartialTick(), n.getPoseStack(), n.getMultiBufferSource(),
                    n.getPackedLight());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoRenderArmEvent extends RenderArmEvent {
        private final net.neoforged.neoforge.client.event.RenderArmEvent n;

        NeoRenderArmEvent(net.neoforged.neoforge.client.event.RenderArmEvent n) {
            super(n.getPoseStack(), n.getMultiBufferSource(), n.getPackedLight(), n.getPlayer(), n.getArm());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoRenderHandEvent extends RenderHandEvent {
        private final net.neoforged.neoforge.client.event.RenderHandEvent n;

        NeoRenderHandEvent(net.neoforged.neoforge.client.event.RenderHandEvent n) {
            super(n.getHand(), n.getPoseStack(), n.getMultiBufferSource(), n.getPackedLight(), n.getPartialTick(),
                    n.getInterpolatedPitch(), n.getSwingProgress(), n.getEquipProgress(), n.getItemStack());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoInputEventMouseButtonPre extends InputEvent.MouseButton.Pre {
        private final net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre n;

        NeoInputEventMouseButtonPre(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre n) {
            super(n.getButton(), n.getAction(), n.getModifiers());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoInputEventMouseScrollingEvent extends InputEvent.MouseScrollingEvent {
        private final net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent n;

        NeoInputEventMouseScrollingEvent(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent n) {
            super(n.getScrollDeltaX(), n.getScrollDeltaY(), n.isLeftDown(), n.isMiddleDown(), n.isRightDown(),
                    n.getMouseX(), n.getMouseY());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoInputEventInteractionKeyMappingTriggered extends InputEvent.InteractionKeyMappingTriggered {
        private final net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered n;

        NeoInputEventInteractionKeyMappingTriggered(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered n) {
            super((n.isAttack() ? 0 : n.isUseItem() ? 1 : 2), n.getKeyMapping(), n.getHand());
            this.n = n;
        }

        @Override
        public boolean shouldSwingHand() {
            return n.shouldSwingHand();
        }

        @Override
        public void setSwingHand(boolean swingHand) {
            n.setSwingHand(swingHand);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoRenderGuiLayerEventPre extends RenderGuiLayerEvent.Pre {
        private final net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre n;

        NeoRenderGuiLayerEventPre(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre n) {
            super(n.getGuiGraphics(), n.getPartialTick(), n.getName(), n.getLayer());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoRenderGuiEventPre extends RenderGuiEvent.Pre {
        private final net.neoforged.neoforge.client.event.RenderGuiEvent.Pre n;

        NeoRenderGuiEventPre(net.neoforged.neoforge.client.event.RenderGuiEvent.Pre n) {
            super(n.getGuiGraphics(), n.getPartialTick());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoRenderHighlightEventBlock extends RenderHighlightEvent.Block {
        private final net.neoforged.neoforge.client.event.RenderHighlightEvent.Block n;

        NeoRenderHighlightEventBlock(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block n) {
            super(n.getLevelRenderer(), n.getCamera(), n.getTarget(), n.getDeltaTracker(), n.getPoseStack(),
                    n.getMultiBufferSource());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoCustomizeGuiOverlayEventBossEventProgress extends CustomizeGuiOverlayEvent.BossEventProgress {
        private final net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress n;

        NeoCustomizeGuiOverlayEventBossEventProgress(net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress n) {
            super(n.getWindow(), n.getGuiGraphics(), n.getPartialTick(), n.getBossEvent(), n.getX(), n.getY(),
                    n.getIncrement());
            this.n = n;
        }

        @Override
        public int getIncrement() {
            return n.getIncrement();
        }

        @Override
        public void setIncrement(int increment) {
            n.setIncrement(increment);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoScreenEventOpening extends ScreenEvent.Opening {
        private final net.neoforged.neoforge.client.event.ScreenEvent.Opening n;

        NeoScreenEventOpening(net.neoforged.neoforge.client.event.ScreenEvent.Opening n) {
            super(n.getCurrentScreen(), n.getScreen());
            this.n = n;
        }

        @Override
        public Screen getNewScreen() {
            return n.getNewScreen();
        }

        @Override
        public void setNewScreen(Screen newScreen) {
            n.setNewScreen(newScreen);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoSelectMusicEvent extends SelectMusicEvent {
        private final net.neoforged.neoforge.client.event.SelectMusicEvent n;

        NeoSelectMusicEvent(net.neoforged.neoforge.client.event.SelectMusicEvent n) {
            super(n.getOriginalMusic(), n.getPlayingMusic());
            this.n = n;
        }

        @Override
        public Music getMusic() {
            return n.getMusic();
        }

        @Override
        public void setMusic(Music music) {
            n.setMusic(music);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoComputeFovModifierEvent extends ComputeFovModifierEvent {
        private final net.neoforged.neoforge.client.event.ComputeFovModifierEvent n;

        NeoComputeFovModifierEvent(net.neoforged.neoforge.client.event.ComputeFovModifierEvent n) {
            super(n.getPlayer(), n.getFovModifier());
            this.n = n;
        }

        @Override
        public float getNewFovModifier() {
            return n.getNewFovModifier();
        }

        @Override
        public void setNewFovModifier(float newFovModifier) {
            n.setNewFovModifier(newFovModifier);
        }
    }

    private static final class NeoCalculateDetachedCameraDistanceEvent extends CalculateDetachedCameraDistanceEvent {
        private final net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent n;

        NeoCalculateDetachedCameraDistanceEvent(net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent n) {
            super(n.getCamera(), n.isCameraFlipped(), n.getEntityScalingFactor(), n.getDistance());
            this.n = n;
        }

        @Override
        public float getDistance() {
            return n.getDistance();
        }

        @Override
        public void setDistance(float distance) {
            n.setDistance(distance);
        }
    }

    private static final class NeoRenderNameTagEvent extends RenderNameTagEvent {
        private final net.neoforged.neoforge.client.event.RenderNameTagEvent n;

        NeoRenderNameTagEvent(net.neoforged.neoforge.client.event.RenderNameTagEvent n) {
            super(n.getEntity(), n.getContent(), n.getEntityRenderer(), n.getPoseStack(), n.getMultiBufferSource(),
                    n.getPackedLight(), n.getPartialTick());
            this.n = n;
        }

        @Override
        public Component getContent() {
            return n.getContent();
        }

        @Override
        public void setContent(Component content) {
            n.setContent(content);
        }

        @Override
        public TriState canRender() {
            return TriState.valueOf(n.canRender().name());
        }

        @Override
        public void setCanRender(TriState canRender) {
            n.setCanRender(net.neoforged.neoforge.common.util.TriState.valueOf(canRender.name()));
        }
    }

    private static final class NeoFMLClientSetupEvent extends FMLClientSetupEvent {
        private final net.neoforged.fml.event.lifecycle.FMLClientSetupEvent n;

        NeoFMLClientSetupEvent(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent n) {
            super();
            this.n = n;
        }

        @Override
        public CompletableFuture<Void> enqueueWork(Runnable work) {
            return n.enqueueWork(work);
        }
    }
}
