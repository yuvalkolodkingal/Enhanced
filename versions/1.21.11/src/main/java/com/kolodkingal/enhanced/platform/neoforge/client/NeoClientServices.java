package com.kolodkingal.enhanced.platform.neoforge.client;
//#norenames

import com.kolodkingal.enhanced.platform.client.ClientServices;
import com.kolodkingal.enhanced.platform.client.KeyContext;
import com.kolodkingal.enhanced.platform.client.KeyMod;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
// (1.21.5: block models are BlockStateModels)
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
// (1.21.5: model data is NeoForge's own business)
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The client services on NeoForge. Key contexts: {@link KeyContext#IN_GAME}, {@code UNIVERSAL} and {@code GUI} ARE
 * {@code KeyConflictContext}'s three (so other mods' keys clash with the mod's exactly as before); a mod's own context
 * is wrapped once in an {@link IKeyConflictContext} whose {@code isActive} is the context's and whose
 * {@code conflicts(o)} asks the context about {@code o} seen as a {@code KeyContext} (NeoForge's three as the
 * constants, a wrapped context as itself, another mod's context as a stand-in that answers with that context's own
 * {@code conflicts}). {@link KeyMod} is {@link KeyModifier}. Batched block models: the model's {@link ModelData} and
 * render types, as the mod's meshes drew them before.
 */
public final class NeoClientServices implements ClientServices {
    private static final Map<KeyContext, Adapter> ADAPTERS = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<IKeyConflictContext, Foreign> FOREIGN =
            Collections.synchronizedMap(new IdentityHashMap<>());

    @Override
    public KeyMapping newKey(String name, KeyContext context, KeyMod modifier, InputConstants.Type type, int keyCode,
                             String category) {
        Objects.requireNonNull(context, () -> "The key mapping " + name + " has no context (declared before it?)");
        return new KeyMapping(name, toNeo(context), toNeo(modifier), type, keyCode, category(category));
    }

    @Override
    public boolean matches(KeyMapping mapping, InputConstants.Key key) {
        return mapping.isActiveAndMatches(key);
    }

    @Override
    public InputConstants.Key boundKey(KeyMapping mapping) {
        return mapping.getKey();
    }

    @Override
    public boolean isActive(KeyContext.Vanilla context) {
        return toNeo(context).isActive();
    }

    @Override
    public boolean conflicts(KeyContext.Vanilla context, KeyContext other) {
        return toNeo(context).conflicts(other == null ? null : toNeo(other));
    }

    @Override
    public int hudLeftHeight() {
        return Minecraft.getInstance().gui.leftHeight;
    }

    @Override
    public int hudRightHeight() {
        return Minecraft.getInstance().gui.rightHeight;
    }

    @Override
    public void renderBatched(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter view,
                              PoseStack pose, boolean checkSides, RandomSource random,
                              Function<RenderType, VertexConsumer> bufferFor) {
        // (1.21.11: a block's layers are chunk section layers; the mod's buffers are asked for the moving-block types)
        dispatcher.renderBatched(state, pos, view, pose, layer -> bufferFor.apply(switch (layer) {
            case SOLID -> net.minecraft.client.renderer.rendertype.RenderTypes.solidMovingBlock();
            case CUTOUT -> net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock();
            case TRIPWIRE -> net.minecraft.client.renderer.rendertype.RenderTypes.tripwireMovingBlock();
        }), checkSides, dispatcher.getBlockModel(state).collectParts(view, pos, state, random));
    }

    private static final Map<String, KeyMapping.Category> CATEGORIES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * (1.21.9) A key's category is an id: 1.21.1's {@code key.categories.<name>} is {@code enhanced:<name>}, named by
     * {@code key.category.enhanced.<name>} (registered with the keys, NeoClientRegistrar).
     */
    public static KeyMapping.Category category(String name) {
        return CATEGORIES.computeIfAbsent(name, n -> new KeyMapping.Category(com.kolodkingal.enhanced.Enhanced.id(
                n.startsWith("key.categories.") ? n.substring("key.categories.".length()) : n.replaceAll("[^a-z0-9_./-]", "_"))));
    }

    /** The categories made so far (for their registration). */
    public static java.util.Collection<KeyMapping.Category> categories() {
        return CATEGORIES.values();
    }

    /** NeoForge's context for {@code context} (the same object every time). */
    public static IKeyConflictContext toNeo(KeyContext context) {
        if (context instanceof KeyContext.Vanilla vanilla) {
            return switch (vanilla) {
                case UNIVERSAL -> KeyConflictContext.UNIVERSAL;
                case GUI -> KeyConflictContext.GUI;
                case IN_GAME -> KeyConflictContext.IN_GAME;
            };
        }
        if (context instanceof Foreign foreign) {
            return foreign.context;
        }
        return ADAPTERS.computeIfAbsent(context, Adapter::new);
    }

    /** The {@code KeyContext} of NeoForge's {@code context} (the same object every time; null for null). */
    public static KeyContext fromNeo(IKeyConflictContext context) {
        if (context == null) {
            return null;
        }
        if (context instanceof KeyConflictContext neo) {
            return switch (neo) {
                case UNIVERSAL -> KeyContext.UNIVERSAL;
                case GUI -> KeyContext.GUI;
                case IN_GAME -> KeyContext.IN_GAME;
            };
        }
        if (context instanceof Adapter adapter) {
            return adapter.context;
        }
        return FOREIGN.computeIfAbsent(context, Foreign::new);
    }

    public static KeyModifier toNeo(KeyMod modifier) {
        return switch (modifier) {
            case NONE -> KeyModifier.NONE;
            case CONTROL -> KeyModifier.CONTROL;
            case SHIFT -> KeyModifier.SHIFT;
            case ALT -> KeyModifier.ALT;
        };
    }

    /** A mod's own context, as NeoForge sees it. */
    private record Adapter(KeyContext context) implements IKeyConflictContext {
        @Override
        public boolean isActive() {
            return context.isActive();
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return context.conflicts(fromNeo(other));
        }
    }

    /** Another mod's context, as the mod's contexts see it: it answers as that context does. */
    private record Foreign(IKeyConflictContext context) implements KeyContext {
        @Override
        public boolean isActive() {
            return context.isActive();
        }

        @Override
        public boolean conflicts(KeyContext other) {
            return context.conflicts(other == null ? null : toNeo(other));
        }
    }
}
