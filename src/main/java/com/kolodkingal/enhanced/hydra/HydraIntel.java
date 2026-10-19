package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * "Hydra intel" (3.6, the plan's 15.7): the map an officer may leave, to the nearest Avengers Tower (the structures
 * of {@code #enhanced:on_avengers_tower_maps}; MCU: Hydra hunts the Tesseract) — an explorer map like a
 * cartographer's, marked with a red X.
 */
public final class HydraIntel {
    /** How far (in structure-placement steps, as a cartographer's map) the search reaches. */
    public static final int SEARCH = 50;

    /** The map to the Avengers Tower nearest {@code from}, or empty if none can be found. */
    public static ItemStack make(ServerLevel level, BlockPos from) {
        BlockPos tower = level.findNearestMapStructure(ModTags.ON_AVENGERS_TOWER_MAPS, from, SEARCH, true);
        return tower == null ? ItemStack.EMPTY : mapTo(level, tower);
    }

    /** A Hydra intel map centred on {@code target}, marked there. */
    public static ItemStack mapTo(ServerLevel level, BlockPos target) {
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("item.enhanced.hydra_intel"));
        return map;
    }

    private HydraIntel() {
    }
}
