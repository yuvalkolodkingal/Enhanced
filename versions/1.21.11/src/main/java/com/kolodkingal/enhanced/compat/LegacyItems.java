package com.kolodkingal.enhanced.compat;
//#norenames

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
import net.minecraft.resources.Identifier;
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

    private static final Map<Item, Map<Identifier, Function>> PROPERTIES = new HashMap<>();
    private static final Map<Item, ItemColor> COLOURS = new HashMap<>();
    private static final Map<Identifier, Supplier<BlockEntityWithoutLevelRenderer>> RENDERERS = new HashMap<>();
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

    public static synchronized void register(Item item, Identifier name, Function function) {
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
        Identifier id = Enhanced.id("legacy");
        modBus.addListener(RegisterRangeSelectItemModelPropertyEvent.class, e -> e.register(id, Property.CODEC));
        modBus.addListener(RegisterColorHandlersEvent.ItemTintSources.class, e -> e.register(id, Tint.CODEC));
        modBus.addListener(RegisterSpecialModelRendererEvent.class, e -> e.register(id, Special.CODEC));
    }

    public record Property(Identifier name) implements RangeSelectItemModelProperty {
        static final MapCodec<Property> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("name").forGetter(Property::name)).apply(i, Property::new));

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable net.minecraft.world.entity.ItemOwner owner, int seed) {
            LivingEntity holder = owner == null ? null : owner.asLivingEntity();
            Map<Identifier, Function> functions = PROPERTIES.get(stack.getItem());
            Function function = functions == null ? null : functions.get(name);
            return function == null ? 0.0F : function.call(stack, level, holder, seed);
        }

        @Override
        public MapCodec<Property> type() {
            return CODEC;
        }
    }

    public record Tint(Identifier item, int index) implements ItemTintSource {
        static final MapCodec<Tint> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("item").forGetter(Tint::item),
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

    public record Special(Identifier id) implements SpecialModelRenderer.Unbaked {
        static final MapCodec<Special> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(Special::id)).apply(i, Special::new));

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            resolve();
            Supplier<BlockEntityWithoutLevelRenderer> renderer = RENDERERS.get(id);
            if (renderer == null) {
                return null;
            }
            BlockEntityWithoutLevelRenderer r = renderer.get();
            r.onResourceManagerReload(null);
            return new SpecialModelRenderer<ItemStack>() {
                // (1.21.9: submitted; the renderer draws into SubmitBuffers, submitted after)
                @Override
                public void submit(@Nullable ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                   net.minecraft.client.renderer.SubmitNodeCollector collector, int light, int overlay,
                                   boolean foil, int outline) {
                    if (stack != null) {
                        SubmitBuffers buffers = new SubmitBuffers(collector);
                        buffers.outline(outline);
                        r.renderByItem(stack, context, poseStack, buffers, light, overlay);
                        buffers.flush();
                    }
                }

                @Override
                public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> out) {
                    // (the item's box: a block's, as 1.21.4's special models were drawn in)
                    out.accept(new org.joml.Vector3f(0.0F, 0.0F, 0.0F));
                    out.accept(new org.joml.Vector3f(1.0F, 1.0F, 1.0F));
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
