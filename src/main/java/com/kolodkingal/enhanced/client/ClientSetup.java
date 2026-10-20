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
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
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
            // ===== odin (3.5) =====
            com.kolodkingal.enhanced.client.render.OdinClient.setup();
            // ===== 3.6: each lane registers its effects' drawers here (StoneFx, StrangeFx, HulkFx, HydraFx) =====
            // ===== stones (3.6) =====
            StoneCoreClient.setup();
            // ===== strange (3.6) =====
            com.kolodkingal.enhanced.client.render.StrangeSpellFx.register();
            // The Eye of Agamotto opens (its model's predicate) while held for the awakening or its holder's time runs.
            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.kolodkingal.enhanced.registry.ModItems.EYE_OF_AGAMOTTO.get(),
                    com.kolodkingal.enhanced.Enhanced.id("open"), (stack, level, holder, seed) -> holder != null
                            && (holder.getUseItem() == stack || holder instanceof net.minecraft.world.entity.player.Player p
                            && com.kolodkingal.enhanced.power.Powers.get(p).toggle(
                                    com.kolodkingal.enhanced.power.Toggle.TIME_SLOW)) ? 1.0F : 0.0F);
            // ===== stone_sites (3.6) =====
            com.kolodkingal.enhanced.client.world.SiteAmbienceClient.setup();
            // ===== vormir (3.6) =====
            com.kolodkingal.enhanced.client.vormir.VormirClient.setup();
            // ===== hulk (3.6) =====
            com.kolodkingal.enhanced.client.hulk.HulkEffects.register();
            // ===== hydra (3.6) =====
            com.kolodkingal.enhanced.client.hydra.HydraClient.setup();
            // ===== 3.7: each lane registers its effects' drawers here (HealingFx, WolverineFx, Scents, DeadpoolFx,
            // LabFx; StoneFx's LOCATOR_TRAIL) =====
            // ===== healing (3.7) =====
            com.kolodkingal.enhanced.client.healing.HealingClient.setup();
            // ===== wolverine (3.7) =====
            com.kolodkingal.enhanced.client.wolverine.WolverineEffects.register();
            com.kolodkingal.enhanced.client.wolverine.ScentView.register();
            // ===== deadpool (3.7) =====
            com.kolodkingal.enhanced.client.deadpool.DeadpoolEffects.register();
            // ===== school_lab (3.7) =====
            com.kolodkingal.enhanced.client.school.LabClient.setup();
            // ===== finders (3.7) =====
            com.kolodkingal.enhanced.client.finders.LocatorClient.setup();
            // The lab's console (L4) lifts its helmet off the stand while its Cerebro is on (L5).
            com.kolodkingal.enhanced.client.school.LabRenderers.Console.helmetOff =
                    com.kolodkingal.enhanced.client.finders.CerebroSphere::consoleOn;
            // ===== cottage (3.7) =====
            com.kolodkingal.enhanced.client.cottage.CottageClient.setup();
            // ===== 3.8: each lane registers its effects' drawers here (CaptainFx, RedHulkFx, IronFx, FalconFx,
            // TowerFx, EternityFx and its wish screen; SuitModels and SuitAssembly's registry for the suits) =====
            // ===== serum (3.8) =====
            // ===== captain (3.8) =====
            com.kolodkingal.enhanced.client.captain.CaptainEffects.register();
            // The Vibranium Shield's raised model (vanilla's "blocking" predicate, which vanilla gives its own shield).
            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.kolodkingal.enhanced.registry.ModItems.VIBRANIUM_SHIELD.get(),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("blocking"),
                    (stack, level, holder, seed) -> holder != null && holder.isUsingItem()
                            && holder.getUseItem() == stack ? 1.0F : 0.0F);
            // ===== red_hulk (3.8) =====
            // ===== iron_man (3.8) =====
            // ===== nanotech (3.8) =====
            // ===== suit_art (3.8) =====
            // ===== falcon (3.8) =====
            // ===== tower (3.8) =====
            // ===== eternity (3.8) =====
            // ===== love (3.8) =====
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
        // 3.6: the Stone Wheel (J).
        event.register(KeyBindings.STONE_WHEEL);
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
        // The renderers (and block entity renderers) of 3.5, one region per lane: the skeleton draws its entities with
        // nothing until their lanes give them renderers.
        // ===== asgard (3.5) =====
        event.registerEntityRenderer(ModEntities.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.BIFROST_MECHANISM.get(),
                com.kolodkingal.enhanced.client.asgard.BifrostMechanismRenderer::new);
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.HOFUND_SOCKET.get(),
                com.kolodkingal.enhanced.client.asgard.HofundSocketRenderer::new);
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.GUNGNIR_STAND.get(),
                com.kolodkingal.enhanced.client.asgard.GungnirStandRenderer::new);
        // ===== heimdall (3.5) =====
        // ===== odin (3.5) =====
        event.registerEntityRenderer(ModEntities.GUNGNIR.get(),
                com.kolodkingal.enhanced.client.render.GungnirRenderer::new);
        event.registerEntityRenderer(ModEntities.RAVEN.get(),
                com.kolodkingal.enhanced.client.render.RavenRenderer::new);
        // ===== hela (3.5) =====
        event.registerEntityRenderer(ModEntities.HELA_SHADOW.get(), com.kolodkingal.enhanced.client.render.HelaShadowRenderer::new);
        event.registerEntityRenderer(ModEntities.HELA.get(), com.kolodkingal.enhanced.client.render.HelaRenderer::new);
        event.registerEntityRenderer(ModEntities.BERSERKER.get(), com.kolodkingal.enhanced.client.render.BerserkerRenderer::new);
        event.registerEntityRenderer(ModEntities.FENRIS.get(), com.kolodkingal.enhanced.client.render.FenrisRenderer::new);
        event.registerEntityRenderer(ModEntities.NECROSWORD.get(), com.kolodkingal.enhanced.client.render.NecroswordRenderer::new);
        event.registerEntityRenderer(ModEntities.BLADE_SPIKE.get(), com.kolodkingal.enhanced.client.render.BladeSpikeRenderer::new);
        // ===== asgardians (3.5) =====
        event.registerEntityRenderer(ModEntities.ASGARDIAN.get(), com.kolodkingal.enhanced.client.asgard.AsgardianRenderer::new);
        // The renderers (and block entity renderers) of 3.6, one region per lane: the skeleton draws its entities with
        // nothing (or a stand-in) until their lanes give them renderers; the blocks are plain models until then.
        // ===== stones (3.6) =====
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.STONE_PEDESTAL.get(),
                com.kolodkingal.enhanced.client.render.StonePedestalRenderer::new);
        // ===== strange (3.6) =====
        event.registerEntityRenderer(ModEntities.SLING_PORTAL.get(),
                com.kolodkingal.enhanced.client.render.SlingPortalRenderer::new);
        event.registerEntityRenderer(ModEntities.ASTRAL_BODY.get(),
                com.kolodkingal.enhanced.client.render.AstralBodyRenderer::new);
        // ===== stone_sites (3.6) =====
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.TESSERACT_CRADLE.get(),
                com.kolodkingal.enhanced.client.render.TesseractCradleRenderer::new);
        // ===== vormir (3.6) =====
        event.registerEntityRenderer(ModEntities.STONEKEEPER.get(),
                com.kolodkingal.enhanced.client.vormir.StonekeeperRenderer::new);
        // ===== hulk (3.6) =====
        event.registerEntityRenderer(ModEntities.HULK_BOULDER.get(),
                com.kolodkingal.enhanced.client.hulk.HulkBoulderRenderer::new);
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.GAMMA_EMITTER.get(),
                com.kolodkingal.enhanced.client.hulk.GammaEmitterRenderer::new);
        // ===== hydra (3.6) =====
        event.registerEntityRenderer(ModEntities.HYDRA_SOLDIER.get(),
                com.kolodkingal.enhanced.client.hydra.HydraSoldierRenderer::new);
        event.registerEntityRenderer(ModEntities.HYDRA_OFFICER.get(),
                com.kolodkingal.enhanced.client.hydra.HydraSoldierRenderer::new);
        event.registerEntityRenderer(ModEntities.HYDRA_BOLT.get(),
                com.kolodkingal.enhanced.client.hydra.HydraBoltRenderer::new);
        // The renderers of 3.7, one region per lane (the skeleton's stand-ins until the lanes draw their own).
        // ===== healing (3.7) =====
        // ===== wolverine (3.7) =====
        // ===== deadpool (3.7) =====
        event.registerEntityRenderer(ModEntities.DEADPOOL_BULLET.get(),
                com.kolodkingal.enhanced.client.deadpool.DeadpoolBulletRenderer::new);
        event.registerEntityRenderer(ModEntities.DOGPOOL.get(),
                com.kolodkingal.enhanced.client.deadpool.DogpoolRenderer::new);
        // ===== school_lab (3.7) =====
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.ADAMANTIUM_RACK.get(),
                com.kolodkingal.enhanced.client.school.LabRenderers.Rack::new);
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.WEAPON_X_POD.get(),
                com.kolodkingal.enhanced.client.school.LabRenderers.Pod::new);
        event.registerBlockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.CEREBRO_CONSOLE.get(),
                com.kolodkingal.enhanced.client.school.LabRenderers.Console::new);
        // ===== finders (3.7) =====
        // ===== cottage (3.7) =====
        event.registerEntityRenderer(ModEntities.COTTAGE_RESIDENT.get(),
                com.kolodkingal.enhanced.client.asgard.AsgardianRenderer::new);
        // The renderers of 3.8, one region per lane (the skeleton's stand-ins: nothing, or a box's outline, until the
        // lanes draw their own).
        // ===== captain (3.8) =====
        event.registerEntityRenderer(ModEntities.THROWN_SHIELD.get(),
                com.kolodkingal.enhanced.client.captain.ThrownShieldRenderer::new);
        // ===== iron_man (3.8) =====
        event.registerEntityRenderer(ModEntities.IRON_MISSILE.get(), NoopRenderer::new);
        // ===== nanotech (3.8) =====
        event.registerEntityRenderer(ModEntities.NANO_DRONE.get(),
                ctx -> new com.kolodkingal.enhanced.client.render.PlaceholderBoxRenderer<>(ctx, 0xC0301E));
        event.registerEntityRenderer(ModEntities.HULKBUSTER_FRAME.get(),
                ctx -> new com.kolodkingal.enhanced.client.render.PlaceholderBoxRenderer<>(ctx, 0xC0301E));
        // ===== suit_art (3.8) =====
        // ===== falcon (3.8) =====
        event.registerEntityRenderer(ModEntities.FALCON_ROCKET.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.FALCON_DART.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.REDWING.get(),
                ctx -> new com.kolodkingal.enhanced.client.render.PlaceholderBoxRenderer<>(ctx, 0xD8323C));
        // ===== tower (3.8) =====
        // ===== eternity (3.8) =====
        event.registerEntityRenderer(ModEntities.ETERNITY.get(),
                ctx -> new com.kolodkingal.enhanced.client.render.PlaceholderBoxRenderer<>(ctx, 0x2A1F4A));
        // ===== love (3.8) =====
    }

    /** 3.5: the sky of Asgard (the dimension type names the effects {@code enhanced:asgard}). */
    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Enhanced.id("asgard"), new com.kolodkingal.enhanced.client.asgard.AsgardSkyEffects());
        // ===== eternity (3.8) =====
        event.register(Enhanced.id("eternity"), new com.kolodkingal.enhanced.client.eternity.EternitySkyEffects());
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
        // ===== asgard (3.5) =====
        // ===== heimdall (3.5) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.LAYER,
                com.kolodkingal.enhanced.client.render.HeimdallHelmLayer::createLayer);
        // ===== odin (3.5) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.OdinHelmLayer.LAYER,
                com.kolodkingal.enhanced.client.render.OdinHelmLayer::createLayer);
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.RavenModel.LAYER,
                com.kolodkingal.enhanced.client.render.RavenModel::createBodyLayer);
        // ===== hela (3.5) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.HelaModel.LAYER, com.kolodkingal.enhanced.client.render.HelaModel::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.HelaModel.CLOAK,
                com.kolodkingal.enhanced.client.render.HelaModel::createCloakLayer);
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.HelaHeaddressLayer.LAYER,
                com.kolodkingal.enhanced.client.render.HelaHeaddressLayer::createLayer);
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.NecroswordModel.LAYER,
                com.kolodkingal.enhanced.client.render.NecroswordModel::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.BerserkerModel.LAYER,
                com.kolodkingal.enhanced.client.render.BerserkerModel::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.render.FenrisModel.LAYER, com.kolodkingal.enhanced.client.render.FenrisModel::createBodyLayer);
        // ===== asgardians (3.5) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.asgard.AsgardianHelmetModel.LAYER,
                com.kolodkingal.enhanced.client.asgard.AsgardianHelmetModel::createLayer);
        // ===== stones (3.6) =====
        // ===== strange (3.6) =====
        // ===== vormir (3.6) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.vormir.StonekeeperModel.LAYER,
                com.kolodkingal.enhanced.client.vormir.StonekeeperModel::createBodyLayer);
        // ===== hulk (3.6) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.hulk.HulkModel.LAYER,
                () -> com.kolodkingal.enhanced.client.hulk.HulkModel.createLayer(0.0F));
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.hulk.HulkModel.ENERGY,
                () -> com.kolodkingal.enhanced.client.hulk.HulkModel.createLayer(0.35F));
        // ===== hydra (3.6) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.hydra.HydraGearModel.LAYER,
                com.kolodkingal.enhanced.client.hydra.HydraGearModel::createLayer);
        // ===== healing (3.7) =====
        // ===== wolverine (3.7) =====
        // ===== deadpool (3.7) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.deadpool.KatanaModel.LAYER,
                com.kolodkingal.enhanced.client.deadpool.KatanaModel::createLayer);
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.deadpool.PistolModel.LAYER,
                com.kolodkingal.enhanced.client.deadpool.PistolModel::createLayer);
        // ===== school_lab (3.7) =====
        // ===== finders (3.7) =====
        // ===== cottage (3.7) =====
        // ===== captain (3.8) =====
        event.registerLayerDefinition(com.kolodkingal.enhanced.client.captain.ShieldModel.LAYER,
                com.kolodkingal.enhanced.client.captain.ShieldModel::createLayer);
        // ===== red_hulk (3.8) =====
        // ===== iron_man (3.8) =====
        // ===== nanotech (3.8) =====
        // ===== suit_art (3.8) =====
        // ===== falcon (3.8) =====
        // ===== eternity (3.8) =====
        // ===== love (3.8) =====
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
                // ===== heimdall (3.5) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.HeimdallHelmLayer(renderer,
                        event.getContext().bakeLayer(com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.LAYER)));
                // ===== odin (3.5) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.OdinHelmLayer(renderer,
                        event.getContext().bakeLayer(com.kolodkingal.enhanced.client.render.OdinHelmLayer.LAYER)));
                // ===== hela (3.5) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.HelaHeaddressLayer(renderer,
                        event.getContext().bakeLayer(com.kolodkingal.enhanced.client.render.HelaHeaddressLayer.LAYER)));
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.HelaBladesLayer(renderer, new com.kolodkingal.enhanced.client.render.NecroswordModel(
                        event.getContext().bakeLayer(com.kolodkingal.enhanced.client.render.NecroswordModel.LAYER))));
                // ===== stones (3.6) =====
                // ===== strange (3.6) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.CloakLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.EyeAmuletLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.AstralLayer(renderer));
                // ===== hulk (3.6) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.hulk.HulkLayers.InhibitorBand(renderer));
                // ===== healing (3.7) =====
                // ===== wolverine (3.7) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.wolverine.ClawLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.WolverineCowlLayer(renderer));
                // ===== deadpool (3.7) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.DeadpoolGearLayer(renderer,
                        new com.kolodkingal.enhanced.client.deadpool.KatanaModel(event.getContext().bakeLayer(
                                com.kolodkingal.enhanced.client.deadpool.KatanaModel.LAYER)),
                        new com.kolodkingal.enhanced.client.deadpool.PistolModel(event.getContext().bakeLayer(
                                com.kolodkingal.enhanced.client.deadpool.PistolModel.LAYER))));
                // ===== captain (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.captain.CapShieldLayer(renderer));
                // ===== red_hulk (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.redhulk.HeatLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enhanced.client.redhulk.RossSuitLayer(renderer));
                // ===== iron_man (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.ironman.SuitLayer(renderer));
                // ===== nanotech (3.8) =====
                // ===== suit_art (3.8) =====
                // ===== falcon (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.falcon.FalconWingsLayer(renderer));
                // ===== love (3.8) =====
                // ===== custom hero (3.4): the layers of his look =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.custom.CustomLooksLayer(renderer,
                        new PlayerModel<>(event.getContext().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim),
                        event.getContext().bakeLayer(AgathaHatLayer.LAYER), event.getContext().bakeLayer(XavierChair.HELMET_LAYER),
                        event.getContext().bakeLayer(CyclopsVisorLayer.LAYER),
                        new net.minecraft.client.model.HumanoidModel<>(event.getContext().bakeLayer(
                                net.minecraft.client.model.geom.ModelLayers.PLAYER_OUTER_ARMOR))));
                // The last layer: it records where the hands, feet and weapon are, after everything else is drawn.
                renderer.addLayer(new HandTrackerLayer(renderer));
            }
        }
        // ===== hulk (3.6): the Hulk's own body, a renderer of its own with the layers that fit him =====
        com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer hulk =
                new com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer(event.getContext());
        com.kolodkingal.enhanced.client.hulk.HulkLayers.addTo(hulk, event.getContext());
        com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer.set(hulk);
        // ===== asgardians (3.5): the Heimdall and Odin NPCs wear the heroes' helms =====
        npcHelm(com.kolodkingal.enhanced.asgard.npc.Role.HEIMDALL,
                event.getContext().bakeLayer(com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.LAYER),
                com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.TEXTURE);
        npcHelm(com.kolodkingal.enhanced.asgard.npc.Role.ODIN,
                event.getContext().bakeLayer(com.kolodkingal.enhanced.client.render.OdinHelmLayer.LAYER),
                com.kolodkingal.enhanced.client.render.OdinHelmLayer.TEXTURE);
        // 3.7.2: the Rio of Death's screen wears Death's crown of branches over her hood
        com.kolodkingal.enhanced.client.asgard.AsgardianRenderer.registerHeadgear(
                com.kolodkingal.enhanced.asgard.npc.Role.RIO,
                        com.kolodkingal.enhanced.client.death.DeathLook::headgear);
    }

    /** A role's NPC wears {@code helm} (a hero's helmet model, in the pixels of the head; the pose is in head space). */
    private static void npcHelm(com.kolodkingal.enhanced.asgard.npc.Role role, net.minecraft.client.model.geom.ModelPart helm,
                                net.minecraft.resources.ResourceLocation texture) {
        com.kolodkingal.enhanced.client.asgard.AsgardianRenderer.registerHeadgear(role,
                (npc, model, pose, buffers, light, partial) -> helm.render(pose,
                        buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texture)), light,
                        net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(npc, 0.0F)));
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
        // ===== stones (3.6) =====
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("stones"), new StoneHud());
        // 3.6, lane L7: the Hulk's rage bar, Banner's heart rate, the gamma flash.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("hulk"),
                new com.kolodkingal.enhanced.client.hulk.HulkHud());
        // ===== healing (3.7) =====
        // 3.7, lane L1: the healing factor's line under the powers' panel; DOWN, the dark edges and the countdown.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("healing"),
                new com.kolodkingal.enhanced.client.healing.HealingHud());
        // ===== wolverine (3.7) =====
        // (The Rage's red edge and the injection's silver flash.)
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("wolverine"),
                new com.kolodkingal.enhanced.client.wolverine.WolverineHud());
        // ===== deadpool (3.7) =====
        // ===== school_lab (3.7) =====
        // the lift's doors closing and opening on the rider's screen
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("lab_lift"),
                new com.kolodkingal.enhanced.client.school.LabClient.FadeLayer());
        // ===== finders (3.7) =====
        // The Stone Locator's line over the hotbar and the amplified Cerebro's stone sites.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("finders"),
                new com.kolodkingal.enhanced.client.finders.FindersHud());
        // ===== cottage (3.7) =====
        // ===== serum (3.8) =====
        // ===== captain (3.8) =====
        // (The super-soldier's line under the powers' panel.)
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, Enhanced.id("captain"),
                new com.kolodkingal.enhanced.client.captain.CaptainHud());
        // ===== red_hulk (3.8) =====
        // ===== iron_man (3.8) =====
        // ===== nanotech (3.8) =====
        // ===== falcon (3.8) =====
        // ===== tower (3.8) =====
        // ===== eternity (3.8) =====
        // ===== love (3.8) =====
    }

    /**
     * 3.8: the items drawn by the mod's own models ({@code IClientItemExtensions}): the Vibranium Shield (its round
     * model), the Exo-Wings, the suits and the reactors (their worn models are the mod's layers; their armour textures
     * are empty). The skeleton registers the plain extensions; each lane gives its item its model in its region.
     */
    @SubscribeEvent
    public static void onRegisterClientExtensions(
            net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        // ===== captain (3.8) =====
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.kolodkingal.enhanced.client.captain.ShieldItemRenderer.get();
            }
        }, com.kolodkingal.enhanced.registry.ModItems.VIBRANIUM_SHIELD.get());
        // ===== iron_man (3.8) =====
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
        }, com.kolodkingal.enhanced.registry.ModItems.ARC_REACTOR.get(),
                com.kolodkingal.enhanced.registry.ModItems.NANO_ARC_REACTOR.get(),
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_1.get(),
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_3.get(),
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_5.get(),
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_50.get(),
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_85.get(),
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_HULKBUSTER.get());
        // ===== nanotech (3.8) =====
        // ===== suit_art (3.8) =====
        // ===== falcon (3.8) =====
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
        }, com.kolodkingal.enhanced.registry.ModItems.FALCON_EXO_WINGS.get());
    }

    private ClientSetup() {
    }
}
