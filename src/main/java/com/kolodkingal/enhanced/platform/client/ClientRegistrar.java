package com.kolodkingal.enhanced.platform.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Everything the mod's client registers with the loader (port P0, PLAN2 3.3): renderers, model layers, player render
 * layers, particles, keys, HUD layers, item colours and renderers, sky effects, reload listeners, client commands and
 * the config screen. Call it while the mod is being set up (the loader's client entry point installs it first); each
 * registration reaches the loader when the loader asks for that kind, in the order it was made. Client only.
 */
public interface ClientRegistrar {
    /** The client's registrar, installed by the loader's client entry point. */
    static ClientRegistrar get() {
        ClientRegistrar registrar = Installed.instance;
        if (registrar == null) {
            throw new IllegalStateException("The client registrar is not installed yet");
        }
        return registrar;
    }

    /** For the loader's client entry point only. */
    static void install(ClientRegistrar registrar) {
        Installed.instance = registrar;
    }

    <E extends Entity> void entityRenderer(Supplier<? extends EntityType<? extends E>> type,
                                           EntityRendererProvider<E> provider);

    <B extends BlockEntity> void blockEntityRenderer(Supplier<? extends BlockEntityType<? extends B>> type,
                                                     BlockEntityRendererProvider<B> provider);

    void layerDefinition(ModelLayerLocation layer, Supplier<LayerDefinition> definition);

    /** Adds render layers to the renderers once they are built (players' skins and other entities'). */
    void addLayers(Consumer<LayerContext> adder);

    <T extends ParticleOptions> void particleSprites(Supplier<? extends ParticleType<T>> type,
                                                     ParticleEngine.SpriteParticleRegistration<T> provider);

    void keyMapping(KeyMapping key);

    /** A HUD layer {@code anchor} the vanilla layer {@code target}. */
    void guiLayer(ResourceLocation id, HudAnchor anchor, VanillaHud target, LayeredDraw.Layer layer);

    void itemColor(ItemColor color, List<? extends Supplier<? extends ItemLike>> items);

    /**
     * Items drawn by the mod's own renderer ({@code renderer} null: items that only declare the loader's client item
     * extension, as the 3.8 suits do).
     */
    void specialItemRenderer(Supplier<BlockEntityWithoutLevelRenderer> renderer,
                             List<? extends Supplier<? extends ItemLike>> items);

    void dimensionEffects(ResourceLocation dimensionType, DimensionSpecialEffects effects);

    void reloadListener(PreparableReloadListener listener);

    /** Client-side commands (registered again whenever the loader rebuilds the client's commands). */
    void clientCommands(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> registrar);

    /** The screen the loader's mod list opens for the mod's settings ({@code factory} gets the screen to return to). */
    void configScreen(Function<Screen, Screen> factory);

    /** What {@link #addLayers} gets: the built renderers. */
    interface LayerContext {
        Set<PlayerSkin.Model> skins();

        /** The player renderer of a skin model (wide or slim), or null. */
        EntityRenderer<? extends Player> playerRenderer(PlayerSkin.Model skin);

        <T extends Entity, R extends EntityRenderer<T>> R renderer(EntityType<? extends T> type);

        EntityRendererProvider.Context context();

        default ModelPart bakeLayer(ModelLayerLocation layer) {
            return context().bakeLayer(layer);
        }
    }

    /** Whether a HUD layer goes above or below its vanilla neighbour. */
    enum HudAnchor {
        ABOVE,
        BELOW
    }

    /** The vanilla HUD layers a mod's layer can sit next to. */
    enum VanillaHud {
        CAMERA_OVERLAYS, CROSSHAIR, HOTBAR, JUMP_METER, EXPERIENCE_BAR, PLAYER_HEALTH, ARMOR_LEVEL, FOOD_LEVEL,
        VEHICLE_HEALTH, AIR_LEVEL, SELECTED_ITEM_NAME, SPECTATOR_TOOLTIP, EXPERIENCE_LEVEL, EFFECTS, BOSS_OVERLAY,
        SLEEP_OVERLAY, DEMO_OVERLAY, DEBUG_OVERLAY, SCOREBOARD_SIDEBAR, OVERLAY_MESSAGE, TITLE, CHAT, TAB_LIST,
        SUBTITLE_OVERLAY, SAVING_INDICATOR
    }

    /** Holds the installed registrar. */
    final class Installed {
        private static volatile ClientRegistrar instance;

        private Installed() {
        }
    }
}
