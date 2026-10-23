package com.kolodkingal.enhanced;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.platform.neoforge.NeoRegistrars;
import com.kolodkingal.enhanced.platform.neoforge.NeoSetup;
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
        // Port P0: the platform layer's NeoForge glue (attachments, messages, entity attributes) comes first.
        NeoSetup.init(modBus);
        NeoRegistrars.register(ModArmorMaterials.ARMOR_MATERIALS, modBus);
        NeoRegistrars.register(ModDataComponents.DATA_COMPONENTS, modBus);
        NeoRegistrars.register(ModBlocks.BLOCKS, modBus);
        NeoRegistrars.register(ModBlockEntities.BLOCK_ENTITIES, modBus);
        NeoRegistrars.register(ModItems.ITEMS, modBus);
        NeoRegistrars.register(ModEffects.EFFECTS, modBus);
        NeoRegistrars.register(ModEntities.ENTITIES, modBus);
        ModAttachments.init();
        NeoRegistrars.register(ModParticles.PARTICLES, modBus);
        NeoRegistrars.register(ModSounds.SOUNDS, modBus);
        // 3.3: the world structures (the Hydra outposts, Mjolnir's landing sites, the Mutant School).
        NeoRegistrars.register(ModStructures.STRUCTURE_TYPES, modBus);
        NeoRegistrars.register(ModStructures.STRUCTURE_PIECES, modBus);
        // 3.5: the chunk generator of Asgard.
        NeoRegistrars.register(com.kolodkingal.enhanced.registry.ModChunkGenerators.CHUNK_GENERATORS, modBus);
        NeoRegistrars.register(ModStructures.PROCESSORS, modBus);
        // 3.6: the stones' recipe (a stone out of its vessel) and loot function (a site stone in a chest).
        NeoRegistrars.register(com.kolodkingal.enhanced.registry.ModRecipes.SERIALIZERS, modBus);
        NeoRegistrars.register(com.kolodkingal.enhanced.registry.ModLootFunctions.FUNCTIONS, modBus);
        NeoRegistrars.register(ModCreativeTab.TABS, modBus);
        // 3.8: the beams of Iron Man's suits and Love's eyes (their styles, by id, on both sides).
        com.kolodkingal.enhanced.beam.AvengersBeams.init();
        // Port P0: the messages are declared on the platform's channels; NeoSetup registers them.
        ModNetwork.register();
        com.kolodkingal.enhanced.network.SpeedNetwork.register();
        com.kolodkingal.enhanced.network.SpiderNetwork.register();
        ModEntities.registerAttributes();
        container.registerConfig(ModConfig.Type.SERVER, EnhancedConfig.SPEC.neoSpec());
        container.registerConfig(ModConfig.Type.CLIENT, EnhancedClientConfig.SPEC.neoSpec());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
