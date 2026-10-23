package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.build.BuildHud;
import com.kolodkingal.enhanced.client.build.BuildKeys;
import com.kolodkingal.enhanced.client.music.CustomMusic;
import com.kolodkingal.enhanced.client.particle.EnergyParticle;
import com.kolodkingal.enhanced.client.render.AgathaHatLayer;
import com.kolodkingal.enhanced.client.render.ApocalypseAuraLayer;
import com.kolodkingal.enhanced.client.render.ApocalypseModel;
import com.kolodkingal.enhanced.client.render.ApocalypseRenderer;
import com.kolodkingal.enhanced.client.render.ApocalypseViewer;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.BillyCrownLayer;
import com.kolodkingal.enhanced.client.render.BlackEyesLayer;
import com.kolodkingal.enhanced.client.render.CyclopsBeamFx;
import com.kolodkingal.enhanced.client.render.CyclopsVisorLayer;
import com.kolodkingal.enhanced.client.render.GauntletLayer;
import com.kolodkingal.enhanced.client.render.GlowingEyesLayer;
import com.kolodkingal.enhanced.client.render.HandTrackerLayer;
import com.kolodkingal.enhanced.client.render.HexVisionRenderer;
import com.kolodkingal.enhanced.client.render.LokiDaggerRenderer;
import com.kolodkingal.enhanced.client.render.LokiIllusionRenderer;
import com.kolodkingal.enhanced.client.render.MindStoneLayer;
import com.kolodkingal.enhanced.client.render.MjolnirFragmentRenderer;
import com.kolodkingal.enhanced.client.render.MjolnirPinRenderer;
import com.kolodkingal.enhanced.client.render.PhoenixFlameLayer;
import com.kolodkingal.enhanced.client.render.PowerAuraLayer;
import com.kolodkingal.enhanced.client.render.SandBoulderRenderer;
import com.kolodkingal.enhanced.client.render.SandCocoonRenderer;
import com.kolodkingal.enhanced.client.render.SandLanceRenderer;
import com.kolodkingal.enhanced.client.render.SoulSight;
import com.kolodkingal.enhanced.client.render.SpiderLegsLayer;
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
import com.kolodkingal.enhanced.client.render.XavierChair;
import com.kolodkingal.enhanced.item.RubyQuartzGlassesItem;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar.HudAnchor;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar.VanillaHud;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.FMLClientSetupEvent;
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

import java.util.List;

/** Client registration (through the platform's {@link ClientRegistrar}): keys, renderers, model layers, HUD. */
@Subscriber(side = Side.CLIENT)
public final class ClientSetup {
    /** A slightly inflated player model, for energy drawn just above the skin. */
    public static final ModelLayerLocation ENERGY_WIDE = new ModelLayerLocation(Enhanced.id("player_energy"), "wide");
    public static final ModelLayerLocation ENERGY_SLIM = new ModelLayerLocation(Enhanced.id("player_energy"), "slim");

    @Subscribe
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
            com.kolodkingal.enhanced.client.redhulk.RedHulkEffects.register();
            // ===== iron_man (3.8) =====
            // The classic suits' and the suit-up's effects, the Unibeam's beam from the chest.
            com.kolodkingal.enhanced.client.ironman.IronClientFx.setup();
            // ===== nanotech (3.8) =====
            com.kolodkingal.enhanced.client.ironman.nano.NanoClient.setup();
            // ===== suit_art (3.8) =====
            com.kolodkingal.enhanced.client.ironman.suits.SuitArt.register();
            // ===== falcon (3.8) =====
            // (Falcon's effects, his gestures' payloads, and Redwing's marks glowing for him.)
            com.kolodkingal.enhanced.client.falcon.FalconEffects.setup();
            // ===== tower (3.8) =====
            com.kolodkingal.enhanced.client.tower.TowerClient.setup();
            // ===== eternity (3.8) =====
            // The wish screen, the altar's and the realm's moments, Love's eye beams and her strike (lane L8).
            com.kolodkingal.enhanced.client.eternity.EternityClient.setup();
            // ===== love (3.8) =====
            // 3.3: a custom hero's Super Senses ("xray") and Telepathy ("sense").
            EntityGlow.register(com.kolodkingal.enhanced.client.custom.CustomSenses::reveals, com.kolodkingal.enhanced.client.custom.CustomSenses::color);
        });
    }

    static void registerKeys(ClientRegistrar registrar) {
        for (KeyMapping key : KeyBindings.ABILITIES) {
            registrar.keyMapping(key);
        }
        registrar.keyMapping(KeyBindings.CHANGE_LOOK);
        registrar.keyMapping(KeyBindings.VISION_COMMAND);
        // 3.6: the Stone Wheel (J).
        registrar.keyMapping(KeyBindings.STONE_WHEEL);
        // 3.1: Build Magic's ten (Y, and the shortcuts of build mode and of a witch's undo and redo).
        for (KeyMapping key : BuildKeys.ALL) {
            registrar.keyMapping(key);
        }
    }

    static void registerRenderers(ClientRegistrar registrar) {
        registrar.entityRenderer(ModEntities.IRON_SHRAPNEL, ctx -> new ThrownItemRenderer<>(ctx, 0.75F, false));
        registrar.entityRenderer(ModEntities.TELEKINETIC_BLOCK, TelekineticBlockRenderer::new);
        registrar.entityRenderer(ModEntities.THROWN_WEAPON, ThrownWeaponRenderer::new);
        registrar.entityRenderer(ModEntities.MJOLNIR_FRAGMENT, MjolnirFragmentRenderer::new);
        registrar.entityRenderer(ModEntities.MJOLNIR_PIN, MjolnirPinRenderer::new);
        registrar.entityRenderer(ModEntities.LOKI_DAGGER, LokiDaggerRenderer::new);
        registrar.entityRenderer(ModEntities.LOKI_ILLUSION, LokiIllusionRenderer::new);
        registrar.entityRenderer(ModEntities.HEX_VISION, HexVisionRenderer::new);

        // The entities of 3.0, one region per hero (the // ===== lines): drawn as nothing until the lane that builds
        // them replaces its line with a renderer of its own.
        // ===== phoenix =====
        // ===== spider =====
        // ===== thanos =====
        registrar.entityRenderer(ModEntities.THANOS, ThanosRenderer::new);
        registrar.entityRenderer(ModEntities.THANOS_SWORD, ThanosSwordRenderer::new);
        registrar.entityRenderer(ModEntities.THANOS_METEOR, ThanosMeteorRenderer::new);
        registrar.entityRenderer(ModEntities.THANOS_ORB, ThanosOrbRenderer::new);
        registrar.entityRenderer(ModEntities.THANOS_PHANTOM, ThanosPhantomRenderer::new);
        // ===== ultron =====
        registrar.entityRenderer(ModEntities.ULTRON, UltronRenderer::new);
        registrar.entityRenderer(ModEntities.ULTRON_SENTRY, UltronSentryRenderer::new);
        registrar.entityRenderer(ModEntities.VIBRANIUM_KEY, VibraniumKeyRenderer::new);
        registrar.entityRenderer(ModEntities.ULTRON_BOLT, UltronBoltRenderer::new);
        // ===== apocalypse =====
        registrar.entityRenderer(ModEntities.APOCALYPSE, ApocalypseRenderer::new);
        registrar.entityRenderer(ModEntities.SAND_LANCE, SandLanceRenderer::new);
        registrar.entityRenderer(ModEntities.SAND_BOULDER, SandBoulderRenderer::new);
        registrar.entityRenderer(ModEntities.SAND_COCOON, SandCocoonRenderer::new);
        // The renderers (and block entity renderers) of 3.5, one region per lane: the skeleton draws its entities with
        // nothing until their lanes give them renderers.
        // ===== asgard (3.5) =====
        registrar.entityRenderer(ModEntities.SEAT, NoopRenderer::new);
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.BIFROST_MECHANISM,
                com.kolodkingal.enhanced.client.asgard.BifrostMechanismRenderer::new);
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.HOFUND_SOCKET,
                com.kolodkingal.enhanced.client.asgard.HofundSocketRenderer::new);
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.GUNGNIR_STAND,
                com.kolodkingal.enhanced.client.asgard.GungnirStandRenderer::new);
        // ===== heimdall (3.5) =====
        // ===== odin (3.5) =====
        registrar.entityRenderer(ModEntities.GUNGNIR,
                com.kolodkingal.enhanced.client.render.GungnirRenderer::new);
        registrar.entityRenderer(ModEntities.RAVEN,
                com.kolodkingal.enhanced.client.render.RavenRenderer::new);
        // ===== hela (3.5) =====
        registrar.entityRenderer(ModEntities.HELA_SHADOW,
                com.kolodkingal.enhanced.client.render.HelaShadowRenderer::new);
        registrar.entityRenderer(ModEntities.HELA, com.kolodkingal.enhanced.client.render.HelaRenderer::new);
        registrar.entityRenderer(ModEntities.BERSERKER, com.kolodkingal.enhanced.client.render.BerserkerRenderer::new);
        registrar.entityRenderer(ModEntities.FENRIS, com.kolodkingal.enhanced.client.render.FenrisRenderer::new);
        registrar.entityRenderer(ModEntities.NECROSWORD,
                com.kolodkingal.enhanced.client.render.NecroswordRenderer::new);
        registrar.entityRenderer(ModEntities.BLADE_SPIKE,
                com.kolodkingal.enhanced.client.render.BladeSpikeRenderer::new);
        // ===== asgardians (3.5) =====
        registrar.entityRenderer(ModEntities.ASGARDIAN, com.kolodkingal.enhanced.client.asgard.AsgardianRenderer::new);
        // The renderers (and block entity renderers) of 3.6, one region per lane: the skeleton draws its entities with
        // nothing (or a stand-in) until their lanes give them renderers; the blocks are plain models until then.
        // ===== stones (3.6) =====
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.STONE_PEDESTAL,
                com.kolodkingal.enhanced.client.render.StonePedestalRenderer::new);
        // ===== strange (3.6) =====
        registrar.entityRenderer(ModEntities.SLING_PORTAL,
                com.kolodkingal.enhanced.client.render.SlingPortalRenderer::new);
        registrar.entityRenderer(ModEntities.ASTRAL_BODY,
                com.kolodkingal.enhanced.client.render.AstralBodyRenderer::new);
        // ===== stone_sites (3.6) =====
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.TESSERACT_CRADLE,
                com.kolodkingal.enhanced.client.render.TesseractCradleRenderer::new);
        // ===== vormir (3.6) =====
        registrar.entityRenderer(ModEntities.STONEKEEPER,
                com.kolodkingal.enhanced.client.vormir.StonekeeperRenderer::new);
        // ===== hulk (3.6) =====
        registrar.entityRenderer(ModEntities.HULK_BOULDER,
                com.kolodkingal.enhanced.client.hulk.HulkBoulderRenderer::new);
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.GAMMA_EMITTER,
                com.kolodkingal.enhanced.client.hulk.GammaEmitterRenderer::new);
        // ===== hydra (3.6) =====
        registrar.entityRenderer(ModEntities.HYDRA_SOLDIER,
                com.kolodkingal.enhanced.client.hydra.HydraSoldierRenderer::new);
        registrar.entityRenderer(ModEntities.HYDRA_OFFICER,
                com.kolodkingal.enhanced.client.hydra.HydraSoldierRenderer::new);
        registrar.entityRenderer(ModEntities.HYDRA_BOLT,
                com.kolodkingal.enhanced.client.hydra.HydraBoltRenderer::new);
        // The renderers of 3.7, one region per lane (the skeleton's stand-ins until the lanes draw their own).
        // ===== healing (3.7) =====
        // ===== wolverine (3.7) =====
        // ===== deadpool (3.7) =====
        registrar.entityRenderer(ModEntities.DEADPOOL_BULLET,
                com.kolodkingal.enhanced.client.deadpool.DeadpoolBulletRenderer::new);
        registrar.entityRenderer(ModEntities.DOGPOOL,
                com.kolodkingal.enhanced.client.deadpool.DogpoolRenderer::new);
        // ===== school_lab (3.7) =====
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.ADAMANTIUM_RACK,
                com.kolodkingal.enhanced.client.school.LabRenderers.Rack::new);
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.WEAPON_X_POD,
                com.kolodkingal.enhanced.client.school.LabRenderers.Pod::new);
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.CEREBRO_CONSOLE,
                com.kolodkingal.enhanced.client.school.LabRenderers.Console::new);
        // ===== finders (3.7) =====
        // ===== cottage (3.7) =====
        registrar.entityRenderer(ModEntities.COTTAGE_RESIDENT,
                com.kolodkingal.enhanced.client.asgard.AsgardianRenderer::new);
        // The renderers of 3.8, one region per lane (the skeleton's stand-ins: nothing, or a box's outline, until the
        // lanes draw their own).
        // ===== captain (3.8) =====
        registrar.entityRenderer(ModEntities.THROWN_SHIELD,
                com.kolodkingal.enhanced.client.captain.ThrownShieldRenderer::new);
        // ===== iron_man (3.8) =====
        registrar.entityRenderer(ModEntities.IRON_MISSILE,
                com.kolodkingal.enhanced.client.ironman.IronMissileRenderer::new);
        // ===== nanotech (3.8) =====
        registrar.entityRenderer(ModEntities.NANO_DRONE,
                com.kolodkingal.enhanced.client.ironman.nano.NanoDroneRenderer::new);
        // (The Hulkbuster's frame: lane L7's renderer, in the suit_art region.)
        // ===== suit_art (3.8) =====
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.SUIT_DISPLAY,
                com.kolodkingal.enhanced.client.ironman.suits.SuitDisplayRenderer::new);
        registrar.entityRenderer(ModEntities.HULKBUSTER_FRAME,
                com.kolodkingal.enhanced.client.ironman.suits.HulkbusterFrameRenderer::new);
        // ===== falcon (3.8) =====
        // (The Wrist Rocket is one of Iron Man's missiles, of the kind FALCON: lane L3's renderer draws it.)
        registrar.entityRenderer(ModEntities.FALCON_ROCKET,
                com.kolodkingal.enhanced.client.ironman.IronMissileRenderer::new);
        registrar.entityRenderer(ModEntities.FALCON_DART,
                com.kolodkingal.enhanced.client.falcon.FalconDartRenderer::new);
        registrar.entityRenderer(ModEntities.REDWING,
                com.kolodkingal.enhanced.client.falcon.RedwingRenderer::new);
        // ===== tower (3.8) =====
        // (the suit display: lane L7's SuitDisplayRenderer, in the suit_art region)
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.TECH_CASE,
                com.kolodkingal.enhanced.client.tower.TechCaseRenderer::new);
        // ===== eternity (3.8) =====
        registrar.entityRenderer(ModEntities.ETERNITY,
                com.kolodkingal.enhanced.client.eternity.EternityRenderer::new);
        registrar.blockEntityRenderer(com.kolodkingal.enhanced.registry.ModBlockEntities.ETERNITY_ALTAR,
                com.kolodkingal.enhanced.client.eternity.AltarRenderer::new);
        // ===== love (3.8) =====
    }

    /** 3.5: the sky of Asgard (the dimension type names the effects {@code enhanced:asgard}). */
    static void registerDimensionEffects(ClientRegistrar registrar) {
        registrar.dimensionEffects(Enhanced.id("asgard"),
                new com.kolodkingal.enhanced.client.asgard.AsgardSkyEffects());
        // ===== eternity (3.8) =====
        registrar.dimensionEffects(Enhanced.id("eternity"),
                new com.kolodkingal.enhanced.client.eternity.EternitySkyEffects());
    }

    /** Frost Giant skins are painted from the current textures, so new resource packs mean painting them again. */
    static void registerReloadListeners(ClientRegistrar registrar) {
        registrar.reloadListener((ResourceManagerReloadListener) resourceManager -> FrostGiantSkins.clear());
    }

    static void registerLayerDefinitions(ClientRegistrar registrar) {
        registrar.layerDefinition(ENERGY_WIDE,
                () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3F), false), 64, 64));
        registrar.layerDefinition(ENERGY_SLIM,
                () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3F), true), 64, 64));
        // The model layers of 3.0, one region per hero (the // ===== lines).
        // ===== phoenix =====
        // ===== spider =====
        // ===== thanos =====
        registrar.layerDefinition(ThanosModel.LAYER, ThanosModel::createBodyLayer);
        registrar.layerDefinition(ThanosSwordModel.LAYER, ThanosSwordModel::createBodyLayer);
        registrar.layerDefinition(GauntletLayer.LAYER, GauntletLayer::createLayer);
        // ===== ultron =====
        registrar.layerDefinition(UltronModel.LAYER, UltronModel::createBodyLayer);
        registrar.layerDefinition(VibraniumKeyModel.LAYER, VibraniumKeyModel::createBodyLayer);
        // ===== apocalypse =====
        registrar.layerDefinition(ApocalypseModel.LAYER, ApocalypseModel::createBodyLayer);
        registrar.layerDefinition(SandCocoonRenderer.LAYER, SandCocoonRenderer::createBodyLayer);
        // ===== cyclops (3.3) =====
        registrar.layerDefinition(CyclopsVisorLayer.LAYER, CyclopsVisorLayer::createLayer);
        // ===== professor_x (3.3) =====
        registrar.layerDefinition(XavierChair.LAYER, XavierChair::createChairLayer);
        registrar.layerDefinition(XavierChair.HELMET_LAYER, XavierChair::createHelmetLayer);
        // ===== agatha (3.3) =====
        registrar.layerDefinition(AgathaHatLayer.LAYER, AgathaHatLayer::createLayer);
        // ===== asgard (3.5) =====
        // ===== heimdall (3.5) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.LAYER,
                com.kolodkingal.enhanced.client.render.HeimdallHelmLayer::createLayer);
        // ===== odin (3.5) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.OdinHelmLayer.LAYER,
                com.kolodkingal.enhanced.client.render.OdinHelmLayer::createLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.RavenModel.LAYER,
                com.kolodkingal.enhanced.client.render.RavenModel::createBodyLayer);
        // ===== hela (3.5) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.HelaModel.LAYER,
                com.kolodkingal.enhanced.client.render.HelaModel::createBodyLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.HelaModel.CLOAK,
                com.kolodkingal.enhanced.client.render.HelaModel::createCloakLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.HelaHeaddressLayer.LAYER,
                com.kolodkingal.enhanced.client.render.HelaHeaddressLayer::createLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.NecroswordModel.LAYER,
                com.kolodkingal.enhanced.client.render.NecroswordModel::createBodyLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.BerserkerModel.LAYER,
                com.kolodkingal.enhanced.client.render.BerserkerModel::createBodyLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.render.FenrisModel.LAYER,
                com.kolodkingal.enhanced.client.render.FenrisModel::createBodyLayer);
        // ===== asgardians (3.5) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.asgard.AsgardianHelmetModel.LAYER,
                com.kolodkingal.enhanced.client.asgard.AsgardianHelmetModel::createLayer);
        // ===== stones (3.6) =====
        // ===== strange (3.6) =====
        // ===== vormir (3.6) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.vormir.StonekeeperModel.LAYER,
                com.kolodkingal.enhanced.client.vormir.StonekeeperModel::createBodyLayer);
        // ===== hulk (3.6) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.hulk.HulkModel.LAYER,
                () -> com.kolodkingal.enhanced.client.hulk.HulkModel.createLayer(0.0F));
        registrar.layerDefinition(com.kolodkingal.enhanced.client.hulk.HulkModel.ENERGY,
                () -> com.kolodkingal.enhanced.client.hulk.HulkModel.createLayer(0.35F));
        // ===== hydra (3.6) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.hydra.HydraGearModel.LAYER,
                com.kolodkingal.enhanced.client.hydra.HydraGearModel::createLayer);
        // ===== healing (3.7) =====
        // ===== wolverine (3.7) =====
        // ===== deadpool (3.7) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.deadpool.KatanaModel.LAYER,
                com.kolodkingal.enhanced.client.deadpool.KatanaModel::createLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.deadpool.PistolModel.LAYER,
                com.kolodkingal.enhanced.client.deadpool.PistolModel::createLayer);
        // ===== school_lab (3.7) =====
        // ===== finders (3.7) =====
        // ===== cottage (3.7) =====
        // ===== captain (3.8) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.captain.ShieldModel.LAYER,
                com.kolodkingal.enhanced.client.captain.ShieldModel::createLayer);
        // ===== red_hulk (3.8) =====
        // ===== iron_man (3.8) =====
        // ===== nanotech (3.8) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.ironman.nano.NanoDroneModel.LAYER,
                com.kolodkingal.enhanced.client.ironman.nano.NanoDroneModel::createLayer);
        // ===== suit_art (3.8) =====
        com.kolodkingal.enhanced.client.ironman.suits.SuitArt.registerLayers(registrar);
        // ===== falcon (3.8) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.falcon.FalconWingsModel.LAYER,
                com.kolodkingal.enhanced.client.falcon.FalconWingsModel::createLayer);
        registrar.layerDefinition(com.kolodkingal.enhanced.client.falcon.RedwingModel.LAYER,
                com.kolodkingal.enhanced.client.falcon.RedwingModel::createLayer);
        // ===== eternity (3.8) =====
        registrar.layerDefinition(com.kolodkingal.enhanced.client.eternity.EternityModel.LAYER,
                com.kolodkingal.enhanced.client.eternity.EternityModel::createLayer);
        // ===== love (3.8) =====
    }

    static void addLayers(ClientRegistrar.LayerContext layers) {
        for (PlayerSkin.Model skin : layers.skins()) {
            if (layers.playerRenderer(skin) instanceof PlayerRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                PlayerModel<net.minecraft.client.player.AbstractClientPlayer> energy =
                        new PlayerModel<>(layers.context().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim);
                renderer.addLayer(new PowerAuraLayer(renderer, energy));
                renderer.addLayer(new GlowingEyesLayer(renderer));
                renderer.addLayer(new MindStoneLayer(renderer));
                renderer.addLayer(new BillyCrownLayer(renderer));
                // The layers of 3.0, one region per hero (the // ===== lines), before the last one.
                // ===== phoenix =====
                renderer.addLayer(new PhoenixFlameLayer(renderer,
                        new PlayerModel<>(layers.context().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim)));
                // ===== spider =====
                renderer.addLayer(new SpiderLegsLayer(renderer));
                renderer.addLayer(new BlackEyesLayer(renderer));
                // ===== thanos =====
                renderer.addLayer(new GauntletLayer(renderer, layers.context().bakeLayer(GauntletLayer.LAYER)));
                // ===== ultron =====
                // ===== apocalypse =====
                renderer.addLayer(new ApocalypseAuraLayer(renderer, energy));
                // ===== cyclops (3.3) =====
                renderer.addLayer(new CyclopsVisorLayer(renderer, layers.context().bakeLayer(CyclopsVisorLayer.LAYER)));
                // ===== professor_x (3.3) =====
                renderer.addLayer(new XavierChair(renderer, layers.context().bakeLayer(XavierChair.LAYER),
                        layers.context().bakeLayer(XavierChair.HELMET_LAYER)));
                // ===== agatha (3.3) =====
                renderer.addLayer(new AgathaHatLayer(renderer, layers.context().bakeLayer(AgathaHatLayer.LAYER)));
                // ===== heimdall (3.5) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.HeimdallHelmLayer(renderer,
                        layers.context().bakeLayer(com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.LAYER)));
                // ===== odin (3.5) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.OdinHelmLayer(renderer,
                        layers.context().bakeLayer(com.kolodkingal.enhanced.client.render.OdinHelmLayer.LAYER)));
                // ===== hela (3.5) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.HelaHeaddressLayer(renderer,
                        layers.context().bakeLayer(com.kolodkingal.enhanced.client.render.HelaHeaddressLayer.LAYER)));
                renderer.addLayer(new com.kolodkingal.enhanced.client.render.HelaBladesLayer(renderer, new com.kolodkingal.enhanced.client.render.NecroswordModel(
                        layers.context().bakeLayer(com.kolodkingal.enhanced.client.render.NecroswordModel.LAYER))));
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
                        new com.kolodkingal.enhanced.client.deadpool.KatanaModel(layers.context().bakeLayer(
                                com.kolodkingal.enhanced.client.deadpool.KatanaModel.LAYER)),
                        new com.kolodkingal.enhanced.client.deadpool.PistolModel(layers.context().bakeLayer(
                                com.kolodkingal.enhanced.client.deadpool.PistolModel.LAYER))));
                // ===== captain (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.captain.CapShieldLayer(renderer));
                // ===== red_hulk (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.redhulk.HeatLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enhanced.client.redhulk.RossSuitLayer(renderer,
                        new PlayerModel<>(layers.context().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim)));
                // ===== iron_man (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.ironman.SuitLayer(renderer));
                // ===== nanotech (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.ironman.nano.NanoGearLayer(renderer));
                if (!slim) {
                    // The Hulkbuster's renderer swap (one renderer: the suit covers the arms of both skins).
                    com.kolodkingal.enhanced.client.ironman.HulkbusterRenderer.set(
                            new com.kolodkingal.enhanced.client.ironman.HulkbusterRenderer(layers.context()));
                }
                // ===== suit_art (3.8) =====
                com.kolodkingal.enhanced.client.ironman.suits.SuitArt.bake(layers.context().getModelSet());
                renderer.addLayer(new com.kolodkingal.enhanced.client.ironman.suits.SuitExtrasLayer(renderer));
                // ===== falcon (3.8) =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.falcon.FalconWingsLayer(renderer,
                        layers.context().bakeLayer(com.kolodkingal.enhanced.client.falcon.FalconWingsModel.LAYER)));
                // ===== love (3.8) =====
                // ===== custom hero (3.4): the layers of his look =====
                renderer.addLayer(new com.kolodkingal.enhanced.client.custom.CustomLooksLayer(renderer,
                        new PlayerModel<>(layers.context().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim),
                        layers.context().bakeLayer(AgathaHatLayer.LAYER),
                                layers.context().bakeLayer(XavierChair.HELMET_LAYER),
                        layers.context().bakeLayer(CyclopsVisorLayer.LAYER),
                        new net.minecraft.client.model.HumanoidModel<>(layers.context().bakeLayer(
                                net.minecraft.client.model.geom.ModelLayers.PLAYER_OUTER_ARMOR))));
                // The last layer: it records where the hands, feet and weapon are, after everything else is drawn.
                renderer.addLayer(new HandTrackerLayer(renderer));
            }
        }
        // ===== hulk (3.6): the Hulk's own body, a renderer of its own with the layers that fit him =====
        com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer hulk =
                new com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer(layers.context());
        com.kolodkingal.enhanced.client.hulk.HulkLayers.addTo(hulk, layers.context());
        // ===== red_hulk (3.8): Red Hulk is drawn on the Hulk's body; his heat glows on it =====
        hulk.addLayer(new com.kolodkingal.enhanced.client.redhulk.HeatLayer(hulk));
        com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer.set(hulk);
        // ===== asgardians (3.5): the Heimdall and Odin NPCs wear the heroes' helms =====
        npcHelm(com.kolodkingal.enhanced.asgard.npc.Role.HEIMDALL,
                layers.context().bakeLayer(com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.LAYER),
                com.kolodkingal.enhanced.client.render.HeimdallHelmLayer.TEXTURE);
        npcHelm(com.kolodkingal.enhanced.asgard.npc.Role.ODIN,
                layers.context().bakeLayer(com.kolodkingal.enhanced.client.render.OdinHelmLayer.LAYER),
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

    static void registerParticles(ClientRegistrar registrar) {
        registrar.particleSprites(ModParticles.ENERGY, EnergyParticle.Provider::new);
    }

    static void registerGui(ClientRegistrar registrar) {
        registrar.guiLayer(Enhanced.id("powers"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY, new PowerHud());
        registrar.guiLayer(Enhanced.id("slow_motion"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new SlowMotionOverlay());
        // 3.3: the speed lines of a speedster running on a wall or a ceiling.
        registrar.guiLayer(Enhanced.id("speed_lines"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new SpeedSurfaceFx.SpeedLines());
        registrar.guiLayer(Enhanced.id("anim_state"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new AnimStateOverlay());
        // The screens of 3.0, one region per hero (the // ===== lines).
        // ===== phoenix =====
        registrar.guiLayer(Enhanced.id("mind_read"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new MindReadingOverlay());
        registrar.guiLayer(Enhanced.id("phoenix_light"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new PhoenixOverlay());
        // ===== spider =====
        registrar.guiLayer(Enhanced.id("spider_sense"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new SpiderSenseOverlay());
        // ===== thanos =====
        registrar.guiLayer(Enhanced.id("thanos_atmosphere"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new ThanosAtmosphere.Overlay());
        // ===== ultron =====
        // ===== apocalypse =====
        registrar.guiLayer(Enhanced.id("apocalypse"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new ApocalypseOverlay());
        // ===== professor_x (3.3) =====
        registrar.guiLayer(Enhanced.id("xavier"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS, new XavierOverlay());
        // 3.1: Build Magic's badge, rings, hint strip and tips.
        registrar.guiLayer(Enhanced.id("build_magic"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY, new BuildHud());
        // 3.2: the Darkhold's hint panel while it is held.
        registrar.guiLayer(Enhanced.id("darkhold"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY, new DarkholdHud());
        // 3.3: a custom hero's panel, in his colours.
        registrar.guiLayer(Enhanced.id("custom_hero"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new com.kolodkingal.enhanced.client.custom.CustomHeroHud());
        // 3.3, lane PROT: the Hex Barrier's health bar and the Hex Runes' line.
        registrar.guiLayer(Enhanced.id("protect"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY, new ProtectHud());
        // ===== stones (3.6) =====
        registrar.guiLayer(Enhanced.id("stones"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY, new StoneHud());
        // 3.6, lane L7: the Hulk's rage bar, Banner's heart rate, the gamma flash.
        registrar.guiLayer(Enhanced.id("hulk"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new com.kolodkingal.enhanced.client.hulk.HulkHud());
        // ===== healing (3.7) =====
        // 3.7, lane L1: the healing factor's line under the powers' panel; DOWN, the dark edges and the countdown.
        registrar.guiLayer(Enhanced.id("healing"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new com.kolodkingal.enhanced.client.healing.HealingHud());
        // ===== wolverine (3.7) =====
        // (The Rage's red edge and the injection's silver flash.)
        registrar.guiLayer(Enhanced.id("wolverine"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new com.kolodkingal.enhanced.client.wolverine.WolverineHud());
        // ===== deadpool (3.7) =====
        // ===== school_lab (3.7) =====
        // the lift's doors closing and opening on the rider's screen
        registrar.guiLayer(Enhanced.id("lab_lift"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new com.kolodkingal.enhanced.client.school.LabClient.FadeLayer());
        // ===== finders (3.7) =====
        // The Stone Locator's line over the hotbar and the amplified Cerebro's stone sites.
        registrar.guiLayer(Enhanced.id("finders"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new com.kolodkingal.enhanced.client.finders.FindersHud());
        // ===== cottage (3.7) =====
        // ===== serum (3.8) =====
        // ===== captain (3.8) =====
        // (The super-soldier's line under the powers' panel.)
        registrar.guiLayer(Enhanced.id("captain"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new com.kolodkingal.enhanced.client.captain.CaptainHud());
        // ===== red_hulk (3.8) =====
        // The Anger bar, Ross's line, the jump's charge ring, the absorb's time, the burst's flash.
        registrar.guiLayer(Enhanced.id("red_hulk"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new com.kolodkingal.enhanced.client.redhulk.RedHulkHud());
        // ===== iron_man (3.8) =====
        // 3.8, lane L3: the suit's panel (name, energy, durability), the helmet frame and the faceplate.
        registrar.guiLayer(Enhanced.id("iron_man"), HudAnchor.BELOW, VanillaHud.DEBUG_OVERLAY,
                new com.kolodkingal.enhanced.client.ironman.IronHud());
        // ===== nanotech (3.8) =====
        // ===== falcon (3.8) =====
        // ===== tower (3.8) =====
        // ===== eternity (3.8) =====
        // The white flash of the journey to Eternity and home.
        registrar.guiLayer(Enhanced.id("eternity_flash"), HudAnchor.ABOVE, VanillaHud.CAMERA_OVERLAYS,
                new com.kolodkingal.enhanced.client.eternity.EternityClient.FlashLayer());
        // ===== love (3.8) =====
    }

    /**
     * 3.8: the items drawn by the mod's own models ({@code IClientItemExtensions}): the Vibranium Shield (its round
     * model), the Exo-Wings, the suits and the reactors (their worn models are the mod's layers; their armour textures
     * are empty). The skeleton registers the plain extensions; each lane gives its item its model in its region.
     */
    static void registerItemRenderers(ClientRegistrar registrar) {
        // ===== captain (3.8) =====
        registrar.specialItemRenderer(com.kolodkingal.enhanced.client.captain.ShieldItemRenderer::get,
                List.of(com.kolodkingal.enhanced.registry.ModItems.VIBRANIUM_SHIELD));
        // ===== iron_man (3.8) =====
        registrar.specialItemRenderer(null, List.of(com.kolodkingal.enhanced.registry.ModItems.ARC_REACTOR,
                com.kolodkingal.enhanced.registry.ModItems.NANO_ARC_REACTOR,
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_1,
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_3,
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_5,
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_50,
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_MARK_85,
                com.kolodkingal.enhanced.registry.ModItems.IRON_MAN_HULKBUSTER));
        // ===== nanotech (3.8) =====
        // ===== suit_art (3.8) =====
        // ===== falcon (3.8) =====
        registrar.specialItemRenderer(null, List.of(com.kolodkingal.enhanced.registry.ModItems.FALCON_EXO_WINGS));
    }

    /**
     * Port P0: every client registration of the mod, handed to the loader through the platform's registrar (called by
     * the client entry point while the mod is constructed). Each kind reaches the loader in the order it is registered
     * here, which is the order NeoForge called the old registration listeners in (docs/port/listener-order.txt).
     */
    public static void register(ClientRegistrar registrar) {
        // HUD layers: Vormir's, the Stones', Heimdall's, then the mod's own (below).
        com.kolodkingal.enhanced.client.vormir.VormirClient.Setup.register(registrar);
        com.kolodkingal.enhanced.client.stones.StoneClient.Setup.register(registrar);
        com.kolodkingal.enhanced.client.render.HeimdallSightView.Setup.register(registrar);
        // Keys: Spider-Man's Web Assist, then the mod's own.
        SpiderAssistKey.Registration.register(registrar);
        registerKeys(registrar);
        registerRenderers(registrar);
        registerLayerDefinitions(registrar);
        registrar.addLayers(ClientSetup::addLayers);
        registerParticles(registrar);
        registerGui(registrar);
        com.kolodkingal.enhanced.client.finders.LocatorClient.registerColors(registrar);
        registerItemRenderers(registrar);
        registerDimensionEffects(registrar);
        registerReloadListeners(registrar);
        // Client commands, in the order NeoForge called them.
        registrar.clientCommands(ModelViewer::register);
        registrar.clientCommands(AnimDebugCommands::register);
        registrar.clientCommands(com.kolodkingal.enhanced.client.render.UltronFxCommand::register);
    }

    private ClientSetup() {
    }
}
