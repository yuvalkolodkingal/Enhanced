package com.kolodkingal.enhanced.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * The convention ({@code c:}) tags the mod reads, under the ids both loaders use for them (port P0, PLAN2 3.3):
 * NeoForge's {@code Tags} and Fabric's convention tags fill the same {@code c:} tags. Tag keys are interned, so each
 * constant is the very key NeoForge's {@code Tags} holds. Add a tag here (with its NeoForge name) when the mod
 * needs another one.
 */
public final class CommonTags {
    private CommonTags() {
    }

    /** Block tags. */
    public static final class Blocks {
        /** {@code c:glass_blocks} (NeoForge: {@code Tags.Blocks.GLASS_BLOCKS}). */
        public static final TagKey<Block> GLASS_BLOCKS = tag("glass_blocks");
        /** {@code c:glass_panes} ({@code Tags.Blocks.GLASS_PANES}). */
        public static final TagKey<Block> GLASS_PANES = tag("glass_panes");
        /** {@code c:ores} ({@code Tags.Blocks.ORES}). */
        public static final TagKey<Block> ORES = tag("ores");
        /** {@code c:ores_in_ground/deepslate} ({@code Tags.Blocks.ORES_IN_GROUND_DEEPSLATE}). */
        public static final TagKey<Block> ORES_IN_GROUND_DEEPSLATE = tag("ores_in_ground/deepslate");
        /** {@code c:ores_in_ground/netherrack} ({@code Tags.Blocks.ORES_IN_GROUND_NETHERRACK}). */
        public static final TagKey<Block> ORES_IN_GROUND_NETHERRACK = tag("ores_in_ground/netherrack");
        /** {@code c:ores_in_ground/stone} ({@code Tags.Blocks.ORES_IN_GROUND_STONE}). */
        public static final TagKey<Block> ORES_IN_GROUND_STONE = tag("ores_in_ground/stone");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", name));
        }

        private Blocks() {
        }
    }
}
