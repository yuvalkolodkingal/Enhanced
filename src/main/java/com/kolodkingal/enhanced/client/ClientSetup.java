package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.music.CustomMusic;
import com.kolodkingal.enhanced.client.particle.EnergyParticle;
import com.kolodkingal.enhanced.client.render.GlowingEyesLayer;
import com.kolodkingal.enhanced.client.render.HandTrackerLayer;
import com.kolodkingal.enhanced.client.render.HexVisionRenderer;
import com.kolodkingal.enhanced.client.render.LokiDaggerRenderer;
import com.kolodkingal.enhanced.client.render.LokiIllusionRenderer;
import com.kolodkingal.enhanced.client.render.BillyCrownLayer;
import com.kolodkingal.enhanced.client.render.BlackEyesLayer;
import com.kolodkingal.enhanced.client.render.MindStoneLayer;
import com.kolodkingal.enhanced.client.render.MjolnirFragmentRenderer;
import com.kolodkingal.enhanced.client.render.MjolnirPinRenderer;
import com.kolodkingal.enhanced.client.render.PowerAuraLayer;
import com.kolodkingal.enhanced.client.render.SpiderLegsLayer;
import com.kolodkingal.enhanced.client.render.TelekineticBlockRenderer;
import com.kolodkingal.enhanced.client.render.ThrownWeaponRenderer;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModParticles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Client registration (mod bus): keys, renderers, model layers, particles, HUD. */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    /** A slightly inflated player model, for energy drawn just above the skin. */
    public static final ModelLayerLocation ENERGY_WIDE = new ModelLayerLocation(Enhanced.id("player_energy"), "wide");
    public static final ModelLayerLocation ENERGY_SLIM = new ModelLayerLocation(Enhanced.id("player_energy"), "slim");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CustomMusic.prepareFolder();
        // Who glows through walls for the local player: Billy's Noise, and the senses of the heroes of 3.0 as they add
        // theirs.
        event.enqueueWork(() -> {
            EntityGlow.register(TheNoise::reveals, TheNoise::moodColor);
            // ===== phoenix =====
            // ===== spider =====
            EntityGlow.register(SpiderSenseClient::reveals, SpiderSenseClient::color);
            // ===== thanos =====
            // ===== ultron =====
            // ===== apocalypse =====
        });
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : KeyBindings.ABILITIES) {
            event.register(key);
        }
        event.register(KeyBindings.CHANGE_LOOK);
        event.register(KeyBindings.VISION_COMMAND);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.IRON_SHRAPNEL.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.75F, false));
        event.registerEntityRenderer(ModEntities.TELEKINETIC_BLOCK.get(), TelekineticBlockRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_WEAPON.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.MJOLNIR_FRAGMENT.get(), MjolnirFragmentRenderer::new);
        event.registerEntityRenderer(ModEntities.MJOLNIR_PIN.get(), MjolnirPinRenderer::new);
        event.registerEntityRenderer(ModEntities.LOKI_DAGGER.get(), LokiDaggerRenderer::new);
        event.registerEntityRenderer(ModEntities.LOKI_ILLUSION.get(), LokiIllusionRenderer::new);
        event.registerEntityRenderer(ModEntities.HEX_VISION.get(), HexVisionRenderer::new);

        // The entities of 3.0, one region per hero (the // ===== lines): drawn as nothing until the lane that builds
        // them replaces its line with a renderer of its own.
        // ===== phoenix =====
        // ===== spider =====
        // ===== thanos =====
        event.registerEntityRenderer(ModEntities.THANOS.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_SWORD.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_METEOR.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_ORB.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_PHANTOM.get(), NoopRenderer::new);
        // ===== ultron =====
        event.registerEntityRenderer(ModEntities.ULTRON.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ULTRON_SENTRY.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.VIBRANIUM_KEY.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ULTRON_BOLT.get(), NoopRenderer::new);
        // ===== apocalypse =====
        event.registerEntityRenderer(ModEntities.APOCALYPSE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.SAND_LANCE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.SAND_BOULDER.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.SAND_COCOON.get(), NoopRenderer::new);
    }

    /** Frost Giant skins are painted from the current textures, so new resource packs mean painting them again. */
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> FrostGiantSkins.clear());
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ENERGY_WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3F), false), 64, 64));
        event.registerLayerDefinition(ENERGY_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3F), true), 64, 64));
        // The model layers of 3.0, one region per hero (the // ===== lines).
        // ===== phoenix =====
        // ===== spider =====
        // ===== thanos =====
        // ===== ultron =====
        // ===== apocalypse =====
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                PlayerModel<net.minecraft.client.player.AbstractClientPlayer> energy =
                        new PlayerModel<>(event.getContext().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim);
                renderer.addLayer(new PowerAuraLayer(renderer, energy));
                renderer.addLayer(new GlowingEyesLayer(renderer));
                renderer.addLayer(new MindStoneLayer(renderer));
                renderer.addLayer(new BillyCrownLayer(renderer));
                // The layers of 3.0, one region per hero (the // ===== lines), before the last one.
                // ===== phoenix =====
                // ===== spider =====
                renderer.addLayer(new SpiderLegsLayer(renderer));
                renderer.addLayer(new BlackEyesLayer(renderer));
                // ===== thanos =====
                // ===== ultron =====
                // ===== apocalypse =====
                // The last layer: it records where the hands, feet and weapon are, after everything else is drawn.
                renderer.addLayer(new HandTrackerLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ENERGY.get(), EnergyParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterGui(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("powers"), new PowerHud());
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("slow_motion"), new SlowMotionOverlay());
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("anim_state"), new AnimStateOverlay());
        // The screens of 3.0, one region per hero (the // ===== lines).
        // ===== phoenix =====
        // ===== spider =====
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("spider_sense"), new SpiderSenseOverlay());
        // ===== thanos =====
        // ===== ultron =====
        // ===== apocalypse =====
    }

    private ClientSetup() {
    }
}
