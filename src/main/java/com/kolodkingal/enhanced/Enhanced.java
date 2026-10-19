package com.kolodkingal.enhanced;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.registry.ModArmorMaterials;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModCreativeTab;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModParticles;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModStructures;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Enhanced: become an MCU hero.
 */
@Mod(Enhanced.MODID)
public class Enhanced {
    public static final String MODID = "enhanced";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Enhanced(IEventBus modBus, ModContainer container) {
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        // 3.3: the world structures (the Hydra outposts, Mjolnir's landing sites, the Mutant School).
        ModStructures.STRUCTURE_TYPES.register(modBus);
        ModStructures.STRUCTURE_PIECES.register(modBus);
        // 3.5: the chunk generator of Asgard.
        com.kolodkingal.enhanced.registry.ModChunkGenerators.CHUNK_GENERATORS.register(modBus);
        ModStructures.PROCESSORS.register(modBus);
        // 3.6: the stones' recipe (a stone out of its vessel) and loot function (a site stone in a chest).
        com.kolodkingal.enhanced.registry.ModRecipes.SERIALIZERS.register(modBus);
        com.kolodkingal.enhanced.registry.ModLootFunctions.FUNCTIONS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModEntities::registerAttributes);
        container.registerConfig(ModConfig.Type.SERVER, EnhancedConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, EnhancedClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
