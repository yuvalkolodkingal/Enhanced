package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.ironman.SuitModels;
import com.kolodkingal.enhanced.ironman.SuitItemState;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The suit art's doors into the client setup (3.8, PLAN 7; called from {@code ClientSetup}'s {@code suit_art (3.8)}
 * regions): the layer definitions of the six suits and their props, the models baked from them on every resource
 * reload ({@link SuitModels#register}), the six choreographies ({@code SuitAssembly.Registry}) and the Mark 5's item
 * property {@code enhanced:folded} (its suitcase model).
 */
public final class SuitArt {
    /** The extras a part may carry (children {@code x_<name>} of the meshes). */
    static final List<String> EXTRAS = List.of("pod", "blade", "shield", "cannon", "eblade", "flap0r", "flap0l",
            "flap1r", "flap1l");
    /**
     * Whether the suits draw their nanotech gear (the Mark 50's blade, round shield and cannon, the Mark 85's energy
     * blades and hexagonal shield). The nanotech lane's own gear layer draws them as light too: keep one of the two.
     */
    public static boolean NANO_GEAR = true;
    private static final Map<SuitKind, ModelLayerLocation> LAYERS = new EnumMap<>(SuitKind.class);
    public static final ModelLayerLocation SUITCASE = new ModelLayerLocation(Enhanced.id("suitcase"), "main");
    public static final ModelLayerLocation GANTRY = new ModelLayerLocation(Enhanced.id("suit_gantry"), "main");
    /** The model set the models were last baked from (the event fires once per player skin). */
    private static EntityModelSet baked;

    static {
        for (SuitKind kind : SuitKind.VALUES) {
            LAYERS.put(kind, new ModelLayerLocation(Enhanced.id("suit_" + kind.id()), "main"));
        }
    }

    /** The layer of a suit's model. */
    public static ModelLayerLocation layer(SuitKind kind) {
        return LAYERS.get(kind);
    }

    /** The layer definitions (the {@code RegisterLayerDefinitions} event). */
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (SuitKind kind : SuitKind.VALUES) {
            event.registerLayerDefinition(LAYERS.get(kind), mesh(kind));
        }
        event.registerLayerDefinition(SUITCASE, SuitMeshes::suitcase);
        event.registerLayerDefinition(GANTRY, SuitMeshes::suitGantry);
        event.registerLayerDefinition(HulkbusterModel.LAYER, HulkbusterModel::createLayer);
        event.registerLayerDefinition(NanoDroneModel.LAYER, SuitMeshes::nanoDrone);
    }

    /** The models, baked from the loaded model set (the {@code AddLayers} event: after every resource reload). */
    public static void bake(EntityModelSet models) {
        if (models == baked) {
            return;
        }
        baked = models;
        for (SuitKind kind : SuitKind.VALUES) {
            SuitModels.register(kind, new SuitArtModel(kind, models.bakeLayer(LAYERS.get(kind)), info(kind)));
        }
        Suitcase.bake(models.bakeLayer(SUITCASE));
    }

    /** The choreographies and the item property (the client setup, on the main thread). */
    public static void register() {
        SuitChoreographies.register();
        ItemProperties.register(ModItems.IRON_MAN_MARK_5.get(), Enhanced.id("folded"),
                (stack, level, entity, seed) -> stack.getOrDefault(ModDataComponents.SUIT.get(),
                        SuitItemState.FOLDED).folded() ? 1.0F : 0.0F);
    }

    private static Supplier<LayerDefinition> mesh(SuitKind kind) {
        return switch (kind) {
            case MARK_1 -> SuitMeshes::mark1;
            case MARK_3 -> SuitMeshes::mark3;
            case MARK_5 -> SuitMeshes::mark5;
            case MARK_50 -> SuitMeshes::mark50;
            case MARK_85 -> SuitMeshes::mark85;
            case HULKBUSTER -> SuitMeshes::hulkbuster;
        };
    }

    static SuitMeshes.Info info(SuitKind kind) {
        return switch (kind) {
            case MARK_1 -> SuitMeshes.MARK_1;
            case MARK_3 -> SuitMeshes.MARK_3;
            case MARK_5 -> SuitMeshes.MARK_5;
            case MARK_50 -> SuitMeshes.MARK_50;
            case MARK_85 -> SuitMeshes.MARK_85;
            case HULKBUSTER -> SuitMeshes.HULKBUSTER;
        };
    }

    private SuitArt() {
    }
}
