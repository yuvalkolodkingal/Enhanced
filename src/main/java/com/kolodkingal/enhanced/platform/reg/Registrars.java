package com.kolodkingal.enhanced.platform.reg;

import com.kolodkingal.enhanced.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Makes the mod's registrars (port P0, PLAN2 3.3): {@code Registrars.create(Registries.SOUND_EVENT, MODID)},
 * {@code Registrars.items(MODID)}... On NeoForge each is a {@code DeferredRegister} underneath; the loader's entry point
 * hands it to the mod bus.
 */
public final class Registrars {
    public static <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registry, String namespace) {
        return service().create(registry, namespace);
    }

    public static ItemRegistrar items(String namespace) {
        return service().items(namespace);
    }

    public static BlockRegistrar blocks(String namespace) {
        return service().blocks(namespace);
    }

    public static ComponentRegistrar dataComponents(String namespace) {
        return service().dataComponents(namespace);
    }

    /** Gives an entity type of the mod its attributes when the loader asks for them. */
    public static void entityAttributes(Supplier<? extends EntityType<? extends LivingEntity>> type,
                                        Supplier<AttributeSupplier> attributes) {
        service().entityAttributes(type, attributes);
    }

    /** A spawn egg for an entity type of the mod (call it inside the item's factory). */
    public static Item spawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight,
                                Item.Properties properties) {
        return service().spawnEgg(type, background, highlight, properties);
    }

    private static RegService service() {
        return Platform.get().registries();
    }

    private Registrars() {
    }
}
