package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Enhanced.MODID);

    /** Whose Mjolnir this is. Absent until the first worthy player lifts it. Saved with the item and sent to clients. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MjolnirOwner>> OWNER =
            DATA_COMPONENTS.registerComponentType("owner", builder -> builder
                    .persistent(MjolnirOwner.CODEC)
                    .networkSynchronized(MjolnirOwner.STREAM_CODEC));

    private ModDataComponents() {
    }
}
