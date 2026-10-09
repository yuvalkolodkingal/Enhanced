package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.music.CustomMusic;
import com.kolodkingal.enhanced.client.particle.EnergyParticle;
import com.kolodkingal.enhanced.client.render.MindStoneLayer;
import com.kolodkingal.enhanced.client.render.PhotonEyesLayer;
import com.kolodkingal.enhanced.client.render.PowerAuraLayer;
import com.kolodkingal.enhanced.client.render.TelekineticBlockRenderer;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModParticles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : KeyBindings.ABILITIES) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.IRON_SHRAPNEL.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.75F, false));
        event.registerEntityRenderer(ModEntities.PHOTON_BLAST.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.4F, true));
        event.registerEntityRenderer(ModEntities.TELEKINETIC_BLOCK.get(), TelekineticBlockRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ENERGY_WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3F), false), 64, 64));
        event.registerLayerDefinition(ENERGY_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3F), true), 64, 64));
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                PlayerModel<net.minecraft.client.player.AbstractClientPlayer> energy =
                        new PlayerModel<>(event.getContext().bakeLayer(slim ? ENERGY_SLIM : ENERGY_WIDE), slim);
                renderer.addLayer(new PowerAuraLayer(renderer, energy));
                renderer.addLayer(new PhotonEyesLayer(renderer));
                renderer.addLayer(new MindStoneLayer(renderer));
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
    }

    private ClientSetup() {
    }
}
