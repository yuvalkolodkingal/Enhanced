package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components the mod's items carry. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Enhanced.MODID);

    /** Whose Mjolnir this is. Absent until the first worthy player lifts it. Saved with the item, sent to clients. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MjolnirOwner>> OWNER =
            DATA_COMPONENTS.registerComponentType("owner", builder -> builder
                    .persistent(MjolnirOwner.CODEC)
                    .networkSynchronized(MjolnirOwner.STREAM_CODEC));

    /**
     * 3.5: the bond of a bound weapon (Hofund, Gungnir) with its owner ({@link #OWNER}): the owner's bond count when it was
     * made. A copy whose bond is lower than the owner's current one (his {@code AsgardProgress}) crumbles when it next ticks:
     * that is how a lost weapon is replaced. Saved with the item, sent to clients.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BOND =
            DATA_COMPONENTS.registerComponentType("bond", builder -> builder
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
