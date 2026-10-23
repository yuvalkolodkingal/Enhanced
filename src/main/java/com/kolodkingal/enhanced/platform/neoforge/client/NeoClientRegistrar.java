package com.kolodkingal.enhanced.platform.neoforge.client;

import com.kolodkingal.enhanced.platform.client.ClientRegistrar;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayerLocation;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The client registrar on NeoForge: it keeps every registration and hands them to NeoForge's registration events
 * (mod bus; client commands on the game bus), in the order they were made.
 */
public final class NeoClientRegistrar implements ClientRegistrar {
    private final ModContainer container;
    private final List<Consumer<EntityRenderersEvent.RegisterRenderers>> renderers = new ArrayList<>();
    private final List<Consumer<EntityRenderersEvent.RegisterLayerDefinitions>> layerDefinitions = new ArrayList<>();
    private final List<Consumer<LayerContext>> layers = new ArrayList<>();
    private final List<Consumer<RegisterParticleProvidersEvent>> particles = new ArrayList<>();
    private final List<KeyMapping> keys = new ArrayList<>();
    private final List<Consumer<RegisterGuiLayersEvent>> guiLayers = new ArrayList<>();
    private final List<Consumer<RegisterColorHandlersEvent.Item>> itemColors = new ArrayList<>();
    private final List<Consumer<RegisterClientExtensionsEvent>> itemExtensions = new ArrayList<>();
    private final List<Consumer<RegisterDimensionSpecialEffectsEvent>> dimensionEffects = new ArrayList<>();
    private final List<PreparableReloadListener> reloadListeners = new ArrayList<>();
    private final List<BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext>> commands = new ArrayList<>();

    private NeoClientRegistrar(ModContainer container) {
        this.container = container;
    }

    /** Makes the client registrar, installs it and connects it to the buses. Called by the client entry point. */
    public static NeoClientRegistrar install(ModContainer container, IEventBus modBus) {
        NeoClientRegistrar registrar = new NeoClientRegistrar(container);
        ClientRegistrar.install(registrar);
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, e -> registrar.renderers.forEach(r -> r.accept(e)));
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class,
                e -> registrar.layerDefinitions.forEach(r -> r.accept(e)));
        modBus.addListener(EntityRenderersEvent.AddLayers.class, registrar::addLayers);
        modBus.addListener(RegisterParticleProvidersEvent.class, e -> registrar.particles.forEach(r -> r.accept(e)));
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> registrar.keys.forEach(e::register));
        modBus.addListener(RegisterGuiLayersEvent.class, e -> registrar.guiLayers.forEach(r -> r.accept(e)));
        modBus.addListener(RegisterColorHandlersEvent.Item.class, e -> registrar.itemColors.forEach(r -> r.accept(e)));
        modBus.addListener(RegisterClientExtensionsEvent.class, e -> registrar.itemExtensions.forEach(r -> r.accept(e)));
        modBus.addListener(RegisterDimensionSpecialEffectsEvent.class,
                e -> registrar.dimensionEffects.forEach(r -> r.accept(e)));
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                e -> registrar.reloadListeners.forEach(e::registerReloadListener));
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class,
                e -> registrar.commands.forEach(c -> c.accept(e.getDispatcher(), e.getBuildContext())));
        return registrar;
    }

    private void addLayers(EntityRenderersEvent.AddLayers event) {
        LayerContext context = new LayerContext() {
            @Override
            public Set<PlayerSkin.Model> skins() {
                return event.getSkins();
            }

            @Override
            public EntityRenderer<? extends Player> playerRenderer(PlayerSkin.Model skin) {
                return event.getSkin(skin);
            }

            @Override
            public <T extends Entity, R extends EntityRenderer<T>> R renderer(EntityType<? extends T> type) {
                return event.getRenderer(type);
            }

            @Override
            public EntityRendererProvider.Context context() {
                return event.getContext();
            }
        };
        layers.forEach(adder -> adder.accept(context));
    }

    @Override
    public <E extends Entity> void entityRenderer(Supplier<? extends EntityType<? extends E>> type,
                                                  EntityRendererProvider<E> provider) {
        renderers.add(e -> e.registerEntityRenderer(type.get(), provider));
    }

    @Override
    public <B extends BlockEntity> void blockEntityRenderer(Supplier<? extends BlockEntityType<? extends B>> type,
                                                            BlockEntityRendererProvider<B> provider) {
        renderers.add(e -> e.registerBlockEntityRenderer(type.get(), provider));
    }

    @Override
    public void layerDefinition(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        layerDefinitions.add(e -> e.registerLayerDefinition(layer, definition));
    }

    @Override
    public void addLayers(Consumer<LayerContext> adder) {
        layers.add(adder);
    }

    @Override
    public <T extends ParticleOptions> void particleSprites(Supplier<? extends ParticleType<T>> type,
                                                            ParticleEngine.SpriteParticleRegistration<T> provider) {
        particles.add(e -> e.registerSpriteSet(type.get(), provider));
    }

    @Override
    public void keyMapping(KeyMapping key) {
        keys.add(key);
    }

    @Override
    public void guiLayer(ResourceLocation id, HudAnchor anchor, VanillaHud target, LayeredDraw.Layer layer) {
        ResourceLocation other = vanilla(target);
        guiLayers.add(e -> {
            if (anchor == HudAnchor.ABOVE) {
                e.registerAbove(other, id, layer);
            } else {
                e.registerBelow(other, id, layer);
            }
        });
    }

    @Override
    public void itemColor(ItemColor color, List<? extends Supplier<? extends ItemLike>> items) {
        itemColors.add(e -> e.register(color, items.stream().map(Supplier::get).toArray(ItemLike[]::new)));
    }

    @Override
    public void specialItemRenderer(Supplier<BlockEntityWithoutLevelRenderer> renderer,
                                    List<? extends Supplier<? extends ItemLike>> items) {
        IClientItemExtensions extensions = renderer == null ? new IClientItemExtensions() {
        } : new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer.get();
            }
        };
        itemExtensions.add(e -> e.registerItem(extensions,
                items.stream().map(item -> item.get().asItem()).toArray(Item[]::new)));
    }

    @Override
    public void dimensionEffects(ResourceLocation dimensionType, DimensionSpecialEffects effects) {
        dimensionEffects.add(e -> e.register(dimensionType, effects));
    }

    @Override
    public void reloadListener(PreparableReloadListener listener) {
        reloadListeners.add(listener);
    }

    @Override
    public void clientCommands(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> registrar) {
        commands.add(registrar);
    }

    @Override
    public void configScreen(Function<Screen, Screen> factory) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> factory.apply(parent));
    }

    private static ResourceLocation vanilla(VanillaHud hud) {
        return switch (hud) {
            case CAMERA_OVERLAYS -> VanillaGuiLayers.CAMERA_OVERLAYS;
            case CROSSHAIR -> VanillaGuiLayers.CROSSHAIR;
            case HOTBAR -> VanillaGuiLayers.HOTBAR;
            case JUMP_METER -> VanillaGuiLayers.JUMP_METER;
            case EXPERIENCE_BAR -> VanillaGuiLayers.EXPERIENCE_BAR;
            case PLAYER_HEALTH -> VanillaGuiLayers.PLAYER_HEALTH;
            case ARMOR_LEVEL -> VanillaGuiLayers.ARMOR_LEVEL;
            case FOOD_LEVEL -> VanillaGuiLayers.FOOD_LEVEL;
            case VEHICLE_HEALTH -> VanillaGuiLayers.VEHICLE_HEALTH;
            case AIR_LEVEL -> VanillaGuiLayers.AIR_LEVEL;
            case SELECTED_ITEM_NAME -> VanillaGuiLayers.SELECTED_ITEM_NAME;
            case SPECTATOR_TOOLTIP -> VanillaGuiLayers.SPECTATOR_TOOLTIP;
            case EXPERIENCE_LEVEL -> VanillaGuiLayers.EXPERIENCE_LEVEL;
            case EFFECTS -> VanillaGuiLayers.EFFECTS;
            case BOSS_OVERLAY -> VanillaGuiLayers.BOSS_OVERLAY;
            case SLEEP_OVERLAY -> VanillaGuiLayers.SLEEP_OVERLAY;
            case DEMO_OVERLAY -> VanillaGuiLayers.DEMO_OVERLAY;
            case DEBUG_OVERLAY -> VanillaGuiLayers.DEBUG_OVERLAY;
            case SCOREBOARD_SIDEBAR -> VanillaGuiLayers.SCOREBOARD_SIDEBAR;
            case OVERLAY_MESSAGE -> VanillaGuiLayers.OVERLAY_MESSAGE;
            case TITLE -> VanillaGuiLayers.TITLE;
            case CHAT -> VanillaGuiLayers.CHAT;
            case TAB_LIST -> VanillaGuiLayers.TAB_LIST;
            case SUBTITLE_OVERLAY -> VanillaGuiLayers.SUBTITLE_OVERLAY;
            case SAVING_INDICATOR -> VanillaGuiLayers.SAVING_INDICATOR;
        };
    }
}
