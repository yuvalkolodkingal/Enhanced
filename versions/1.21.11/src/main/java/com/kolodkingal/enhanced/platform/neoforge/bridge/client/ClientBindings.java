package com.kolodkingal.enhanced.platform.neoforge.bridge.client;
//#norenames

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

    /**
     * (1.21.9) The buffers a render event's listeners draw into: what they draw is submitted to the event's collector
     * once they have all run.
     */
    static com.kolodkingal.enhanced.compat.SubmitBuffers buffers(net.minecraft.client.renderer.SubmitNodeCollector collector) {
        com.kolodkingal.enhanced.compat.SubmitBuffers buffers = new com.kolodkingal.enhanced.compat.SubmitBuffers(collector);
        com.kolodkingal.enhanced.platform.neoforge.bridge.NeoForgeBridge.afterPost(buffers::flush);
        return buffers;
    }

    public static void install(Map<Class<?>, Binding<?, ?>> table) {
        Binding.put(table, ClientTickEvent.Pre.class, net.neoforged.neoforge.client.event.ClientTickEvent.Pre.class,
                n -> new ClientTickEvent.Pre());
        Binding.put(table, ClientTickEvent.Post.class, net.neoforged.neoforge.client.event.ClientTickEvent.Post.class,
                n -> new ClientTickEvent.Post());
        Binding.put(table, ClientPlayerNetworkEvent.LoggingIn.class,
                net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn.class,
                n -> new ClientPlayerNetworkEvent.LoggingIn(n.getMultiPlayerGameMode(), n.getPlayer(),
                        n.getConnection()));
        Binding.put(table, ClientPlayerNetworkEvent.LoggingOut.class,
                net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut.class,
                n -> new ClientPlayerNetworkEvent.LoggingOut(n.getMultiPlayerGameMode(), n.getPlayer(),
                        n.getConnection()));
        Binding.put(table, ClientPlayerNetworkEvent.Clone.class,
                net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.Clone.class,
                n -> new ClientPlayerNetworkEvent.Clone(n.getMultiPlayerGameMode(), n.getNewPlayer(),
                        n.getConnection(), n.getOldPlayer()));
        // (1.21.9: a sub-event per stage; the opaque blocks are one stage, the mod's three of them in order)
        Binding.putAll(table, RenderLevelStageEvent.class, net.neoforged.neoforge.client.event.RenderLevelStageEvent.class,
                ClientConvert.STAGE_EVENTS, ClientConvert::stageEvents);
        Binding.put(table, ViewportEvent.ComputeCameraAngles.class,
                net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles.class,
                NeoViewportEventComputeCameraAngles::new);
        Binding.put(table, ViewportEvent.ComputeFogColor.class,
                net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor.class,
                NeoViewportEventComputeFogColor::new);
        Binding.put(table, ViewportEvent.RenderFog.class,
                net.neoforged.neoforge.client.event.ViewportEvent.RenderFog.class,
                NeoViewportEventRenderFog::new);
        // (1.21.9: an avatar renderer draws mannequins and the mod's player-like mobs too: their events are not players')
        Binding.put(table, RenderPlayerEvent.Pre.class, net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre.class,
                n -> com.kolodkingal.enhanced.compat.RenderBridge.entity(n.getRenderState()) instanceof net.minecraft.world.entity.player.Player
                        ? new NeoRenderPlayerEventPre(n) : null);
        Binding.put(table, RenderPlayerEvent.Post.class,
                net.neoforged.neoforge.client.event.RenderPlayerEvent.Post.class,
                n -> !(com.kolodkingal.enhanced.compat.RenderBridge.entity(n.getRenderState()) instanceof net.minecraft.world.entity.player.Player) ? null
                        : new RenderPlayerEvent.Post((net.minecraft.world.entity.player.Player) com.kolodkingal.enhanced.compat.RenderBridge.entity(n.getRenderState()), n.getRenderer(), n.getPartialTick(),
                        n.getPoseStack(), buffers(n.getSubmitNodeCollector()), n.getRenderState().lightCoords));
        Binding.put(table, RenderArmEvent.class, net.neoforged.neoforge.client.event.RenderArmEvent.class,
                NeoRenderArmEvent::new);
        Binding.put(table, RenderHandEvent.class, net.neoforged.neoforge.client.event.RenderHandEvent.class,
                NeoRenderHandEvent::new);
        Binding.put(table, InputEvent.Key.class, net.neoforged.neoforge.client.event.InputEvent.Key.class,
                n -> new InputEvent.Key(n.getKey(), n.getScanCode(), n.getAction(), n.getModifiers()));
        Binding.put(table, InputEvent.MouseButton.Pre.class,
                net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre.class,
                NeoInputEventMouseButtonPre::new);
        Binding.put(table, InputEvent.MouseButton.Post.class,
                net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post.class,
                n -> new InputEvent.MouseButton.Post(n.getButton(), n.getAction(), n.getModifiers()));
        Binding.put(table, InputEvent.MouseScrollingEvent.class,
                net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent.class,
                NeoInputEventMouseScrollingEvent::new);
        Binding.put(table, InputEvent.InteractionKeyMappingTriggered.class,
                net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered.class,
                NeoInputEventInteractionKeyMappingTriggered::new);
        Binding.put(table, RenderGuiLayerEvent.Pre.class,
                net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre.class,
                NeoRenderGuiLayerEventPre::new);
        Binding.put(table, RenderGuiLayerEvent.Post.class,
                net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post.class,
                n -> new RenderGuiLayerEvent.Post(n.getGuiGraphics(), n.getPartialTick(), n.getName(),
                        com.kolodkingal.enhanced.compat.LayeredDraw.of(n.getLayer())));
        Binding.put(table, RenderGuiEvent.Pre.class, net.neoforged.neoforge.client.event.RenderGuiEvent.Pre.class,
                NeoRenderGuiEventPre::new);
        Binding.put(table, RenderGuiEvent.Post.class, net.neoforged.neoforge.client.event.RenderGuiEvent.Post.class,
                n -> new RenderGuiEvent.Post(n.getGuiGraphics(), n.getPartialTick()));
        Binding.put(table, RenderFrameEvent.Pre.class, net.neoforged.neoforge.client.event.RenderFrameEvent.Pre.class,
                n -> new RenderFrameEvent.Pre(n.getPartialTick()));
        Binding.put(table, RenderFrameEvent.Post.class, net.neoforged.neoforge.client.event.RenderFrameEvent.Post.class,
                n -> new RenderFrameEvent.Post(n.getPartialTick()));
        Binding.put(table, RenderHighlightEvent.Block.class,
                net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent.class,
                NeoRenderHighlightEventBlock::new);
        Binding.put(table, CustomizeGuiOverlayEvent.BossEventProgress.class,
                net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress.class,
                NeoCustomizeGuiOverlayEventBossEventProgress::new);
        Binding.put(table, ScreenEvent.Opening.class, net.neoforged.neoforge.client.event.ScreenEvent.Opening.class,
                NeoScreenEventOpening::new);
        Binding.put(table, SelectMusicEvent.class, net.neoforged.neoforge.client.event.SelectMusicEvent.class,
                NeoSelectMusicEvent::new);
        Binding.put(table, MovementInputUpdateEvent.class,
                net.neoforged.neoforge.client.event.MovementInputUpdateEvent.class,
                n -> new MovementInputUpdateEvent(n.getEntity(), n.getInput()));
        Binding.put(table, ComputeFovModifierEvent.class,
                net.neoforged.neoforge.client.event.ComputeFovModifierEvent.class,
                NeoComputeFovModifierEvent::new);
        Binding.put(table, CalculateDetachedCameraDistanceEvent.class,
                net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent.class,
                NeoCalculateDetachedCameraDistanceEvent::new);
        Binding.put(table, RenderNameTagEvent.class, net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender.class,
                NeoRenderNameTagEvent::new);
        Binding.put(table, RegisterClientCommandsEvent.class,
                net.neoforged.neoforge.client.event.RegisterClientCommandsEvent.class,
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
            // (1.21.6: the fog is the terrain's and the sky's at once; its far plane is the nearer of the environment's
            // and the drawn distance's; 1.21.1's listeners set the distances and cancel the event for them to apply)
            super(n.getCamera(), n.getPartialTick(), com.kolodkingal.enhanced.compat.FogMode.FOG_TERRAIN, n.getType(),
                    Math.min(n.getFogData().environmentalEnd, n.getFogData().renderDistanceEnd),
                    Math.min(n.getFogData().environmentalStart, n.getFogData().renderDistanceStart),
                    com.kolodkingal.enhanced.compat.FogShape.SPHERE);
            this.n = n;
        }

        @Override
        public void setCanceled(boolean canceled) {
            super.setCanceled(canceled);
            if (canceled) {
                net.minecraft.client.renderer.fog.FogData data = n.getFogData();
                data.environmentalEnd = Math.min(data.environmentalEnd, getFarPlaneDistance());
                data.renderDistanceEnd = Math.min(data.renderDistanceEnd, getFarPlaneDistance());
                data.environmentalStart = Math.min(data.environmentalStart, getNearPlaneDistance());
                data.renderDistanceStart = Math.min(data.renderDistanceStart, getNearPlaneDistance());
            }
        }
    }

    private static final class NeoRenderPlayerEventPre extends RenderPlayerEvent.Pre {
        private final net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre n;

        NeoRenderPlayerEventPre(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre n) {
            super((net.minecraft.world.entity.player.Player) com.kolodkingal.enhanced.compat.RenderBridge.entity(n.getRenderState()), n.getRenderer(), n.getPartialTick(), n.getPoseStack(), buffers(n.getSubmitNodeCollector()),
                    n.getRenderState().lightCoords);
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
            super(n.getPoseStack(), buffers(n.getSubmitNodeCollector()), n.getPackedLight(), n.getPlayer(), n.getArm());
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
            super(n.getHand(), n.getPoseStack(), buffers(n.getSubmitNodeCollector()), n.getPackedLight(), n.getPartialTick(),
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

    private static final class NeoInputEventInteractionKeyMappingTriggered
            extends InputEvent.InteractionKeyMappingTriggered {
        private final net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered n;

        NeoInputEventInteractionKeyMappingTriggered(
                net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered n) {
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
            super(n.getGuiGraphics(), n.getPartialTick(), n.getName(), com.kolodkingal.enhanced.compat.LayeredDraw.of(n.getLayer()));
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
        private final net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent n;

        // (1.21.11: the outline is extracted, then drawn; cancelling the extraction hides it)
        NeoRenderHighlightEventBlock(net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent n) {
            super(n.getLevelRenderer(), n.getCamera(), n.getHitResult(), net.minecraft.client.Minecraft.getInstance().getDeltaTracker(),
                    new com.mojang.blaze3d.vertex.PoseStack(), null);
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

    private static final class NeoCustomizeGuiOverlayEventBossEventProgress
            extends CustomizeGuiOverlayEvent.BossEventProgress {
        private final net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress n;

        NeoCustomizeGuiOverlayEventBossEventProgress(
                net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress n) {
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

        NeoCalculateDetachedCameraDistanceEvent(
                net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent n) {
            super(n.getCamera(), n.isCameraFlipped(), n.getEntityScalingFactor(), ((float) n.getDistance()));
            this.n = n;
        }

        @Override
        public float getDistance() {
            return ((float) n.getDistance());
        }

        @Override
        public void setDistance(float distance) {
            n.setDistance(distance);
        }
    }

    private static final class NeoRenderNameTagEvent extends RenderNameTagEvent {
        private final net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender n;

        NeoRenderNameTagEvent(net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender n) {
            super(n.getEntity(), n.getContent(), n.getEntityRenderer(), new com.mojang.blaze3d.vertex.PoseStack(), null, 0, n.getPartialTick());
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
            n.setCanRender(net.minecraft.util.TriState.valueOf(canRender.name()));
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
