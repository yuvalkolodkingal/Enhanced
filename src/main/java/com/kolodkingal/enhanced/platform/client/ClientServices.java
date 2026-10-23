package com.kolodkingal.enhanced.platform.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * SPI: the loader's client services behind {@link Keys}, {@link KeyContext} and {@link BlockModels} (port P0).
 * Gameplay code calls those, not this. One implementation per loader, found once with {@link ServiceLoader}
 * ({@code META-INF/services}) or by class name, on first use (so static initialisers may use it before the client
 * registrar is installed). Client only.
 */
public interface ClientServices {
    /** The loader's client services, found once. */
    static ClientServices get() {
        return Lookup.INSTANCE;
    }

    /** See {@link Keys#newKey(String, KeyContext, KeyMod, InputConstants.Type, int, String)}. */
    KeyMapping newKey(String name, KeyContext context, KeyMod modifier, InputConstants.Type type, int keyCode,
                      String category);

    /** See {@link Keys#matches}. */
    boolean matches(KeyMapping mapping, InputConstants.Key key);

    /** See {@link Keys#boundKey}. */
    InputConstants.Key boundKey(KeyMapping mapping);

    /** {@link KeyContext#isActive()} of the loader's own contexts. */
    boolean isActive(KeyContext.Vanilla context);

    /** {@link KeyContext#conflicts(KeyContext)} of the loader's own contexts. */
    boolean conflicts(KeyContext.Vanilla context, KeyContext other);

    /** See {@link BlockModels#renderBatched}. */
    void renderBatched(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter view,
                       PoseStack pose, boolean checkSides, RandomSource random,
                       Function<RenderType, VertexConsumer> bufferFor);

    /** Finds the loader's client services once. */
    final class Lookup {
        private static final ClientServices INSTANCE = load();

        private static ClientServices load() {
            Optional<ClientServices> found = ServiceLoader.load(ClientServices.class,
                    ClientServices.class.getClassLoader()).findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            for (String name : new String[]{"com.kolodkingal.enhanced.platform.neoforge.client.NeoClientServices",
                    "com.kolodkingal.enhanced.platform.fabric.client.FabricClientServices"}) {
                try {
                    return (ClientServices) Class.forName(name).getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    // not this loader
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot create the client services " + name, e);
                }
            }
            throw new IllegalStateException("No Enhanced client services for this mod loader");
        }

        private Lookup() {
        }
    }
}
