package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.build.BuildHud;
import com.kolodkingal.enhanced.client.build.BuildKeys;
import com.kolodkingal.enhanced.client.music.CustomMusic;
import com.kolodkingal.enhanced.client.particle.EnergyParticle;
import com.kolodkingal.enhanced.client.render.GauntletLayer;
import com.kolodkingal.enhanced.client.render.ApocalypseAuraLayer;
import com.kolodkingal.enhanced.client.render.ApocalypseModel;
import com.kolodkingal.enhanced.client.render.ApocalypseRenderer;
import com.kolodkingal.enhanced.client.render.ApocalypseViewer;
import com.kolodkingal.enhanced.client.render.GlowingEyesLayer;
import com.kolodkingal.enhanced.client.render.HandTrackerLayer;
import com.kolodkingal.enhanced.client.render.HexVisionRenderer;
import com.kolodkingal.enhanced.client.render.LokiDaggerRenderer;
import com.kolodkingal.enhanced.client.render.LokiIllusionRenderer;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.AgathaHatLayer;
import com.kolodkingal.enhanced.client.render.BillyCrownLayer;
import com.kolodkingal.enhanced.client.render.BlackEyesLayer;
import com.kolodkingal.enhanced.client.render.CyclopsBeamFx;
import com.kolodkingal.enhanced.client.render.CyclopsVisorLayer;
import com.kolodkingal.enhanced.client.render.MindStoneLayer;
import com.kolodkingal.enhanced.client.render.MjolnirFragmentRenderer;
import com.kolodkingal.enhanced.client.render.MjolnirPinRenderer;
import com.kolodkingal.enhanced.client.render.PhoenixFlameLayer;
import com.kolodkingal.enhanced.client.render.PowerAuraLayer;
import com.kolodkingal.enhanced.client.render.SpiderLegsLayer;
import com.kolodkingal.enhanced.client.render.SoulSight;
import com.kolodkingal.enhanced.client.render.SandBoulderRenderer;
import com.kolodkingal.enhanced.client.render.SandCocoonRenderer;
import com.kolodkingal.enhanced.client.render.SandLanceRenderer;
import com.kolodkingal.enhanced.client.render.TelekineticBlockRenderer;
import com.kolodkingal.enhanced.client.render.ThanosMeteorRenderer;
import com.kolodkingal.enhanced.client.render.ThanosModel;
import com.kolodkingal.enhanced.client.render.ThanosOrbRenderer;
import com.kolodkingal.enhanced.client.render.ThanosPhantomRenderer;
import com.kolodkingal.enhanced.client.render.ThanosRenderer;
import com.kolodkingal.enhanced.client.render.ThanosSwordModel;
import com.kolodkingal.enhanced.client.render.ThanosSwordRenderer;
import com.kolodkingal.enhanced.client.render.ThanosViewer;
import com.kolodkingal.enhanced.client.render.ThrownWeaponRenderer;
import com.kolodkingal.enhanced.client.render.UltronBoltRenderer;
import com.kolodkingal.enhanced.client.render.UltronModel;
import com.kolodkingal.enhanced.client.render.UltronRenderer;
import com.kolodkingal.enhanced.client.render.UltronSentryRenderer;
import com.kolodkingal.enhanced.client.render.UltronViewer;
import com.kolodkingal.enhanced.client.render.VibraniumKeyModel;
import com.kolodkingal.enhanced.client.render.VibraniumKeyRenderer;
import com.kolodkingal.enhanced.item.RubyQuartzGlassesItem;
import com.kolodkingal.enhanced.client.render.XavierChair;
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
            EntityGlow.register(MindSense::reveals, MindSense::color);
            // ===== spider =====
            EntityGlow.register(SpiderSenseClient::reveals, SpiderSenseClient::color);
            // ===== thanos =====
            EntityGlow.register(SoulSight::reveals, SoulSight::color);
            ThanosBossBar.register();
            ThanosViewer.register();
            // ===== ultron =====
            UltronViewer.register();
            // ===== apocalypse =====
            ApocalypseViewer.register();
            // ===== cyclops (3.3) =====
            BeamRenderer.registerDrawer(RubyQuartzGlassesItem.OPTIC, CyclopsBeamFx.OPTIC);
            BeamRenderer.registerDrawer(RubyQuartzGlassesItem.UNLEASHED, CyclopsBeamFx.UNLEASHED);
            // 3.3: a custom hero's Super Senses ("xray") and Telepathy ("sense").
            EntityGlow.register(com.kolodkingal.enhanced.client.custom.CustomSenses::reveals, com.kolodkingal.enhanced.client.custom.CustomSenses::color);
        });
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : KeyBindings.ABILITIES) {
            event.register(key);
        }
        event.register(KeyBindings.CHANGE_LOOK);
        event.register(KeyBindings.VISION_COMMAND);
        // 3.1: Build Magic's ten (Y, and the shortcuts of build mode and of a witch's undo and redo).
        for (KeyMapping key : BuildKeys.ALL) {
            event.register(key);
        }
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
        event.registerEntityRenderer(ModEntities.THANOS.get(), ThanosRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_SWORD.get(), ThanosSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_METEOR.get(), ThanosMeteorRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_ORB.get(), ThanosOrbRenderer::new);
        event.registerEntityRenderer(ModEntities.THANOS_PHANTOM.get(), ThanosPhantomRenderer::new);
        // ===== ultron =====
        event.registerEntityRenderer(ModEntities.ULTRON.get(), UltronRenderer::new);
        event.registerEntityRenderer(ModEntities.ULTRON_SENTRY.get(), UltronSentryRenderer::new);
        event.registerEntityRenderer(ModEntities.VIBRANIUM_KEY.get(), VibraniumKeyRenderer::new);
        event.registerEntityRenderer(ModEntities.ULTRON_BOLT.get(), UltronBoltRenderer::new);
        // ===== apocalypse =====
        event.registerEntityRenderer(ModEntities.APOCALYPSE.get(), ApocalypseRenderer::new);
        event.registerEntityRenderer(ModEntities.SAND_LANCE.get(), SandLanceRenderer::new);
        event.registerEntityRenderer(ModEntities.SAND_BOULDER.get(), SandBoulderRenderer::new);
        event.registerEntityRenderer(ModEntities.SAND_COCOON.get(), SandCocoonRenderer::new);
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
        event.registerLayerDefinition(ThanosModel.LAYER, ThanosModel::createBodyLayer);
        event.registerLayerDefinition(ThanosSwordModel.LAYER, ThanosSwordModel::createBodyLayer);
        event.registerLayerDefinition(GauntletLayer.LAYER, GauntletLayer::createLayer);
        // ===== ultron =====
        event.registerLayerDefinition(UltronModel.LAYER, UltronModel::createBodyLayer);
        event.registerLayerDefinition(VibraniumKeyModel.LAYER, VibraniumKeyModel::createBodyLayer);
        // ===== apocalypse =====
        event.registerLayerDefinition(ApocalypseModel.LAYER, ApocalypseModel::createBodyLayer);
        event.registerLayerDefinition(SandCocoonRenderer.LAYER, SandCocoonRenderer::createBodyLayer);
        // ===== cyclops (3.3) =====
        event.registerLayerDefinition(CyclopsVisorLayer.LAYER, CyclopsVisorLayer::createLayer);
        // ===== professor_x (3.3) =====
        event.registerLayerDefinition(XavierChair.LAYER, XavierChair::createChairLayer);
        event.registerLayerDefinition(XavierChair.HELMET_LAYER, XavierChair::createHelmetLayer);
        // ===== agatha (3.3) =====
        event.registerLayerDefinition(AgathaHatLayer.LAYER, AgathaHatLayer::createLayer);
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
                renderer.addLayer(new PhoenixFlameLayer(renderer,
                        new PlayerModel<>(event.getContext().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim)));
                // ===== spider =====
                renderer.addLayer(new SpiderLegsLayer(renderer));
                renderer.addLayer(new BlackEyesLayer(renderer));
                // ===== thanos =====
                renderer.addLayer(new GauntletLayer(renderer, event.getContext().bakeLayer(GauntletLayer.LAYER)));
                // ===== ultron =====
                // ===== apocalypse =====
                renderer.addLayer(new ApocalypseAuraLayer(renderer, energy));
                // ===== cyclops (3.3) =====
                renderer.addLayer(new CyclopsVisorLayer(renderer, event.getContext().bakeLayer(CyclopsVisorLayer.LAYER)));
                // ===== professor_x (3.3) =====
                renderer.addLayer(new XavierChair(renderer, event.getContext().bakeLayer(XavierChair.LAYER),
                        event.getContext().bakeLayer(XavierChair.HELMET_LAYER)));
                // ===== agatha (3.3) =====
                renderer.addLayer(new AgathaHatLayer(renderer, event.getContext().bakeLayer(AgathaHatLayer.LAYER)));
                // ===== custom hero (3.4): the layers of his look =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.custom.CustomLooksLayer(renderer));
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
        // 3.3: the speed lines of a speedster running on a wall or a ceiling.
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("speed_lines"), new SpeedSurfaceFx.SpeedLines());
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("anim_state"), new AnimStateOverlay());
        // The screens of 3.0, one region per hero (the // ===== lines).
        // ===== phoenix =====
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("mind_read"), new MindReadingOverlay());
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("phoenix_light"), new PhoenixOverlay());
        // ===== spider =====
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("spider_sense"), new SpiderSenseOverlay());
        // ===== thanos =====
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("thanos_atmosphere"), new ThanosAtmosphere.Overlay());
        // ===== ultron =====
        // ===== apocalypse =====
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("apocalypse"), new ApocalypseOverlay());
        // ===== professor_x (3.3) =====
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("xavier"), new XavierOverlay());
        // 3.1: Build Magic's badge, rings, hint strip and tips.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("build_magic"), new BuildHud());
        // 3.2: the Darkhold's hint panel while it is held.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("darkhold"), new DarkholdHud());
        // 3.3: a custom hero's panel, in his colours.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("custom_hero"), new com.kolodkingal.enhanced.client.custom.CustomHeroHud());
        // 3.3, lane PROT: the Hex Barrier's health bar and the Hex Runes' line.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("protect"), new ProtectHud());
    }

    private ClientSetup() {
    }
}
