package com.kolodkingal.enhanced.compat;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 1.21.4 only: 1.21.1's client item hooks, made from code, behind 1.21.4's data-driven item models
 * ({@code assets/enhanced/items}, written by tools/overlays.py):
 * <ul>
 * <li>item properties ({@code ItemProperties.register}) as the range property {@code enhanced:legacy}
 * ({@code "name"}: the property's id);</li>
 * <li>item colours as the tint source {@code enhanced:legacy} ({@code "item"}, {@code "index"});</li>
 * <li>items drawn by code as the special model {@code enhanced:legacy} ({@code "id"}: the item).</li>
 * </ul>
 */
public final class LegacyItems {
    /** 1.21.1's item property function. */
    @FunctionalInterface
    public interface Function {
        float call(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder, int seed);
    }

    private static final Map<Item, Map<ResourceLocation, Function>> PROPERTIES = new HashMap<>();
    private static final Map<Item, ItemColor> COLOURS = new HashMap<>();
    private static final Map<ResourceLocation, Supplier<BlockEntityWithoutLevelRenderer>> RENDERERS = new HashMap<>();
    /** Given at mod construction, before the items exist: resolved on first use. */
    private static final List<Runnable> PENDING = new java.util.ArrayList<>();

    private static synchronized void resolve() {
        if (!PENDING.isEmpty()) {
            List<Runnable> todo = List.copyOf(PENDING);
            PENDING.clear();
            todo.forEach(Runnable::run);
        }
    }

    private LegacyItems() {
    }

    public static synchronized void register(Item item, ResourceLocation name, Function function) {
        PROPERTIES.computeIfAbsent(item, i -> new HashMap<>()).put(name, function);
    }

    public static synchronized void colour(ItemColor colour, List<? extends Supplier<? extends ItemLike>> items) {
        PENDING.add(() -> {
            for (Supplier<? extends ItemLike> item : items) {
                COLOURS.put(item.get().asItem(), colour);
            }
        });
    }

    public static synchronized void renderer(Supplier<BlockEntityWithoutLevelRenderer> renderer,
                                             List<? extends Supplier<? extends ItemLike>> items) {
        PENDING.add(() -> {
            for (Supplier<? extends ItemLike> item : items) {
                RENDERERS.put(BuiltInRegistries.ITEM.getKey(item.get().asItem()), renderer);
            }
        });
    }

    /** The three types, on the mod bus. */
    public static void install(IEventBus modBus) {
        ResourceLocation id = Enhanced.id("legacy");
        modBus.addListener(RegisterRangeSelectItemModelPropertyEvent.class, e -> e.register(id, Property.CODEC));
        modBus.addListener(RegisterColorHandlersEvent.ItemTintSources.class, e -> e.register(id, Tint.CODEC));
        modBus.addListener(RegisterSpecialModelRendererEvent.class, e -> e.register(id, Special.CODEC));
    }

    public record Property(ResourceLocation name) implements RangeSelectItemModelProperty {
        static final MapCodec<Property> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("name").forGetter(Property::name)).apply(i, Property::new));

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder, int seed) {
            Map<ResourceLocation, Function> functions = PROPERTIES.get(stack.getItem());
            Function function = functions == null ? null : functions.get(name);
            return function == null ? 0.0F : function.call(stack, level, holder, seed);
        }

        @Override
        public MapCodec<Property> type() {
            return CODEC;
        }
    }

    public record Tint(ResourceLocation item, int index) implements ItemTintSource {
        static final MapCodec<Tint> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Tint::item),
                Codec.INT.fieldOf("index").forGetter(Tint::index)).apply(i, Tint::new));

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder) {
            resolve();
            ItemColor colour = COLOURS.get(stack.getItem());
            return colour == null ? -1 : 0xFF000000 | colour.getColor(stack, index);
        }

        @Override
        public MapCodec<Tint> type() {
            return CODEC;
        }
    }

    public record Special(ResourceLocation id) implements SpecialModelRenderer.Unbaked {
        static final MapCodec<Special> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Special::id)).apply(i, Special::new));

        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet models) {
            resolve();
            Supplier<BlockEntityWithoutLevelRenderer> renderer = RENDERERS.get(id);
            if (renderer == null) {
                return null;
            }
            BlockEntityWithoutLevelRenderer r = renderer.get();
            r.onResourceManagerReload(null);
            return new SpecialModelRenderer<ItemStack>() {
                @Override
                public void render(@Nullable ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                   MultiBufferSource buffer, int light, int overlay, boolean foil) {
                    if (stack != null) {
                        r.renderByItem(stack, context, poseStack, buffer, light, overlay);
                    }
                }

                @Override
                public ItemStack extractArgument(ItemStack stack) {
                    return stack;
                }
            };
        }

        @Override
        public MapCodec<Special> type() {
            return CODEC;
        }
    }
}
