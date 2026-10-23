package com.kolodkingal.enhanced.platform.neoforge;

import com.kolodkingal.enhanced.platform.reg.BlockEntry;
import com.kolodkingal.enhanced.platform.reg.BlockRegistrar;
import com.kolodkingal.enhanced.platform.reg.ComponentRegistrar;
import com.kolodkingal.enhanced.platform.reg.ItemEntry;
import com.kolodkingal.enhanced.platform.reg.ItemRegistrar;
import com.kolodkingal.enhanced.platform.reg.RegEntry;
import com.kolodkingal.enhanced.platform.reg.RegService;
import com.kolodkingal.enhanced.platform.reg.Registrar;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Registration on NeoForge: each registrar is a {@link DeferredRegister} whose entries are {@link DeferredHolder}s,
 * {@link DeferredItem}s and {@link DeferredBlock}s that also implement the mod's {@link RegEntry} types, so every
 * entry is the very object NeoForge made before the port and behaves exactly as one. The mod's NeoForge entry point
 * hands each registrar to the mod bus with {@link #register(Registrar, IEventBus)}, in its own order.
 */
public final class NeoRegistrars implements RegService {
    private record Attributes(Supplier<? extends EntityType<? extends LivingEntity>> type,
                              Supplier<AttributeSupplier> attributes) {
    }

    private final List<Attributes> attributes = new ArrayList<>();

    /** Hands a registrar of the mod to the mod bus (what {@code DeferredRegister#register(IEventBus)} was). */
    public static void register(Registrar<?> registrar, IEventBus modBus) {
        ((Neo<?>) registrar).deferred().register(modBus);
    }

    /** Gives the entity types their attributes ({@code EntityAttributeCreationEvent} on the mod bus). */
    void createAttributes(EntityAttributeCreationEvent event) {
        for (Attributes entry : attributes) {
            event.put(entry.type().get(), entry.attributes().get());
        }
    }

    @Override
    public <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registry, String namespace) {
        return new Plain<>(new EntryRegister<>(registry, namespace));
    }

    @Override
    public ItemRegistrar items(String namespace) {
        return new Items(new ItemRegister(namespace));
    }

    @Override
    public BlockRegistrar blocks(String namespace) {
        return new Blocks(new BlockRegister(namespace));
    }

    @Override
    public ComponentRegistrar dataComponents(String namespace) {
        return new Components(new EntryRegister<>(Registries.DATA_COMPONENT_TYPE, namespace));
    }

    @Override
    public void entityAttributes(Supplier<? extends EntityType<? extends LivingEntity>> type,
                                 Supplier<AttributeSupplier> attributes) {
        this.attributes.add(new Attributes(type, attributes));
    }

    @Override
    public Item spawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight,
                         Item.Properties properties) {
        return new DeferredSpawnEggItem(type, background, highlight, properties);
    }

    // ---- the entries: NeoForge's holders, as the mod's types ----

    /** A {@link DeferredHolder} that is a {@link RegEntry}. */
    static final class Entry<R, T extends R> extends DeferredHolder<R, T> implements RegEntry<R, T> {
        Entry(ResourceKey<R> key) {
            super(key);
        }
    }

    /** A {@link DeferredItem} that is an {@link ItemEntry}. */
    static final class ItemHolder<I extends Item> extends DeferredItem<I> implements ItemEntry<I> {
        ItemHolder(ResourceKey<Item> key) {
            super(key);
        }
    }

    /** A {@link DeferredBlock} that is a {@link BlockEntry}. */
    static final class BlockHolder<B extends Block> extends DeferredBlock<B> implements BlockEntry<B> {
        BlockHolder(ResourceKey<Block> key) {
            super(key);
        }
    }

    // ---- the deferred registers that make them ----

    static final class EntryRegister<T> extends DeferredRegister<T> {
        EntryRegister(ResourceKey<? extends Registry<T>> registry, String namespace) {
            super(registry, namespace);
        }

        @Override
        protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<? extends Registry<T>> registryKey,
                                                                  ResourceLocation key) {
            return new Entry<>(ResourceKey.create(registryKey, key));
        }
    }

    static final class ItemRegister extends DeferredRegister.Items {
        ItemRegister(String namespace) {
            super(namespace);
        }

        @Override
        protected <I extends Item> DeferredItem<I> createHolder(ResourceKey<? extends Registry<Item>> registryKey,
                                                                ResourceLocation key) {
            return new ItemHolder<>(ResourceKey.create(registryKey, key));
        }
    }

    static final class BlockRegister extends DeferredRegister.Blocks {
        BlockRegister(String namespace) {
            super(namespace);
        }

        @Override
        protected <B extends Block> DeferredBlock<B> createHolder(ResourceKey<? extends Registry<Block>> registryKey,
                                                                  ResourceLocation key) {
            return new BlockHolder<>(ResourceKey.create(registryKey, key));
        }
    }

    // ---- the registrars ----

    /** A registrar backed by a deferred register. */
    interface Neo<T> {
        DeferredRegister<T> deferred();
    }

    @SuppressWarnings("unchecked")
    private static <T, I extends T> RegEntry<T, I> entry(DeferredHolder<T, I> holder) {
        return (RegEntry<T, I>) holder;
    }

    @SuppressWarnings("unchecked")
    private static <I extends Item> ItemEntry<I> item(DeferredItem<I> holder) {
        return (ItemEntry<I>) holder;
    }

    @SuppressWarnings("unchecked")
    private static <B extends Block> BlockEntry<B> block(DeferredBlock<B> holder) {
        return (BlockEntry<B>) holder;
    }

    private abstract static class Base<T, D extends DeferredRegister<T>> implements Registrar<T>, Neo<T> {
        final D deferred;

        Base(D deferred) {
            this.deferred = deferred;
        }

        @Override
        public DeferredRegister<T> deferred() {
            return deferred;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Collection<RegEntry<T, ? extends T>> getEntries() {
            return (Collection) deferred.getEntries();
        }

        @Override
        public ResourceKey<? extends Registry<T>> getRegistryKey() {
            return deferred.getRegistryKey();
        }

        @Override
        public String getNamespace() {
            return deferred.getNamespace();
        }
    }

    private static class Plain<T> extends Base<T, DeferredRegister<T>> {
        Plain(DeferredRegister<T> deferred) {
            super(deferred);
        }

        @Override
        public <I extends T> RegEntry<T, I> register(String name, Supplier<? extends I> factory) {
            return entry(deferred.register(name, factory));
        }

        @Override
        public <I extends T> RegEntry<T, I> register(String name, Function<ResourceLocation, ? extends I> factory) {
            return entry(deferred.register(name, factory));
        }
    }

    private static final class Components extends Plain<DataComponentType<?>> implements ComponentRegistrar {
        Components(DeferredRegister<DataComponentType<?>> deferred) {
            super(deferred);
        }

        @Override
        public <D> RegEntry<DataComponentType<?>, DataComponentType<D>> registerComponentType(
                String name, UnaryOperator<DataComponentType.Builder<D>> builder) {
            // What DeferredRegister.DataComponents#registerComponentType does.
            return register(name, () -> builder.apply(DataComponentType.builder()).build());
        }
    }

    private static final class Items extends Base<Item, ItemRegister> implements ItemRegistrar {
        Items(ItemRegister deferred) {
            super(deferred);
        }

        @Override
        public <I extends Item> ItemEntry<I> register(String name, Supplier<? extends I> factory) {
            return item(deferred.register(name, factory));
        }

        @Override
        public <I extends Item> ItemEntry<I> register(String name, Function<ResourceLocation, ? extends I> factory) {
            return item(deferred.register(name, factory));
        }

        @Override
        public ItemEntry<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block,
                                                            Item.Properties properties) {
            return item(deferred.registerSimpleBlockItem(name, block, properties));
        }

        @Override
        public ItemEntry<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block) {
            return item(deferred.registerSimpleBlockItem(name, block));
        }

        @Override
        public ItemEntry<BlockItem> registerSimpleBlockItem(Holder<Block> block, Item.Properties properties) {
            return item(deferred.registerSimpleBlockItem(block, properties));
        }

        @Override
        public ItemEntry<BlockItem> registerSimpleBlockItem(Holder<Block> block) {
            return item(deferred.registerSimpleBlockItem(block));
        }

        @Override
        public <I extends Item> ItemEntry<I> registerItem(String name, Function<Item.Properties, ? extends I> factory,
                                                          Item.Properties properties) {
            return item(deferred.registerItem(name, factory, properties));
        }

        @Override
        public <I extends Item> ItemEntry<I> registerItem(String name, Function<Item.Properties, ? extends I> factory) {
            return item(deferred.registerItem(name, factory));
        }

        @Override
        public ItemEntry<Item> registerSimpleItem(String name, Item.Properties properties) {
            return item(deferred.registerSimpleItem(name, properties));
        }

        @Override
        public ItemEntry<Item> registerSimpleItem(String name) {
            return item(deferred.registerSimpleItem(name));
        }
    }

    private static final class Blocks extends Base<Block, BlockRegister> implements BlockRegistrar {
        Blocks(BlockRegister deferred) {
            super(deferred);
        }

        @Override
        public <B extends Block> BlockEntry<B> register(String name, Supplier<? extends B> factory) {
            return block(deferred.register(name, factory));
        }

        @Override
        public <B extends Block> BlockEntry<B> register(String name, Function<ResourceLocation, ? extends B> factory) {
            return block(deferred.register(name, factory));
        }

        @Override
        public <B extends Block> BlockEntry<B> registerBlock(String name,
                                                             Function<BlockBehaviour.Properties, ? extends B> factory,
                                                             BlockBehaviour.Properties properties) {
            return block(deferred.registerBlock(name, factory, properties));
        }

        @Override
        public <B extends Block> BlockEntry<B> registerBlock(String name,
                                                             Function<BlockBehaviour.Properties, ? extends B> factory) {
            return block(deferred.registerBlock(name, factory));
        }

        @Override
        public BlockEntry<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties) {
            return block(deferred.registerSimpleBlock(name, properties));
        }

        @Override
        public BlockEntry<Block> registerSimpleBlock(String name) {
            return block(deferred.registerSimpleBlock(name));
        }
    }
}
