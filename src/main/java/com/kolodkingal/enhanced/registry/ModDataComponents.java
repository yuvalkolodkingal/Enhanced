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

    /**
     * 3.6: what an Infinity Gauntlet holds ({@link com.kolodkingal.enhanced.stones.GauntletContents}): the item's
     * default is the Titan's Gauntlet, so every Gauntlet saved before 3.6 reads as it. Saved with the item, sent to
     * clients.
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<com.kolodkingal.enhanced.stones.GauntletContents>> GAUNTLET =
            DATA_COMPONENTS.registerComponentType("gauntlet", builder -> builder
                    .persistent(com.kolodkingal.enhanced.stones.GauntletContents.CODEC)
                    .networkSynchronized(com.kolodkingal.enhanced.stones.GauntletContents.STREAM_CODEC));

    /**
     * 3.7: what a Stone Locator knows ({@link com.kolodkingal.enhanced.finders.LocatorState}): the stone it was tuned
     * to, its filter, its target, what its HUD says. Saved with the item, sent to clients (the needle reads it).
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<com.kolodkingal.enhanced.finders.LocatorState>> LOCATOR =
            DATA_COMPONENTS.registerComponentType("locator", builder -> builder
                    .persistent(com.kolodkingal.enhanced.finders.LocatorState.CODEC)
                    .networkSynchronized(com.kolodkingal.enhanced.finders.LocatorState.STREAM_CODEC));

    /**
     * 3.8: a suit item's own state ({@link com.kolodkingal.enhanced.ironman.SuitItemState}: the Mark 5 folded into its
     * suitcase or not). Saved with the item, sent to clients (the item property {@code enhanced:folded} reads it).
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<com.kolodkingal.enhanced.ironman.SuitItemState>> SUIT =
            DATA_COMPONENTS.registerComponentType("suit", builder -> builder
                    .persistent(com.kolodkingal.enhanced.ironman.SuitItemState.CODEC)
                    .networkSynchronized(com.kolodkingal.enhanced.ironman.SuitItemState.STREAM_CODEC));

    private ModDataComponents() {
    }
}
