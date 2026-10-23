package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/** The loader's side of {@link Registrars} (SPI). */
public interface RegService {
    <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registry, String namespace);

    ItemRegistrar items(String namespace);

    BlockRegistrar blocks(String namespace);

    ComponentRegistrar dataComponents(String namespace);

    void entityAttributes(Supplier<? extends EntityType<? extends LivingEntity>> type,
                          Supplier<AttributeSupplier> attributes);

    Item spawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight,
                  Item.Properties properties);
}
