package com.kolodkingal.enhanced;

import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.registry.ModArmorMaterials;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModCreativeTab;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModParticles;
import com.kolodkingal.enhanced.registry.ModSounds;
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
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModEntities::registerAttributes);
        container.registerConfig(ModConfig.Type.SERVER, EnhancedConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
