package com.kolodkingal.enhanced.client.sokovia;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ultron.SokoviaTemplate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.shaders.Uniform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The island as a picture: every block of it drawn once into buffers, one for each kind of terrain geometry, from the
 * client's own copy of the world, with no block data sent to it. It is built on a worker thread (the way the game
 * builds a section of the terrain), uploaded to the graphics card on the first frame after it is ready, and drawn each
 * frame at the height the move says, by {@link SokoviaIslandRenderer}.
 *
 * <p>The blocks are drawn as the game draws the terrain, through the block renderer, with their own models, textures
 * and biome tints; the light is a fixed bright value (so that nothing flickers while it moves), and the faces on the
 * island's surface are culled against its own blocks, not the world's. The picture is made of the same blocks the
 * server will place in the sky, because both read them with the same rules ({@link SokoviaTemplate}).
 */
final class SokoviaMesh implements AutoCloseable {
    /** What one kind of geometry is made of before it is uploaded. */
    private static final class Layer {
        final ByteBufferBuilder memory;
        final BufferBuilder builder;

        Layer(RenderType type) {
            this.memory = new ByteBufferBuilder(type.bufferSize());
            this.builder = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        }
    }

    /** The mesh as the worker made it: one piece of geometry for each kind that was needed. */
    private record Built(Map<RenderType, MeshData> meshes, Map<RenderType, ByteBufferBuilder> memory) {
    }

    /** The block the cells are measured from (the template's middle). */
    private final BlockPos origin;
    /** Where the picture is when its move has not begun: the ground the island came from. */
    private final BlockPos ground;
    private final CompletableFuture<Built> building;
    private final Map<RenderType, VertexBuffer> buffers = new LinkedHashMap<>();
    private boolean uploaded;
    private boolean closed;

    private SokoviaMesh(BlockPos origin, BlockPos ground, CompletableFuture<Built> building) {
        this.origin = origin;
        this.ground = ground;
        this.building = building;
    }

    /**
     * Starts building the mesh of an island on a worker thread.
     *
     * @param template the island's blocks (read from the client's world, in the coordinates they are in)
     * @param level    the client's world, for the biome tints and the shade
     * @param lifted   how far above the ground the template's blocks are: 0 for the ground, the height of the lift for
     *                 the island in the sky (the picture is drawn from the ground up, so it comes down to the ground)
     */
    static SokoviaMesh build(SokoviaTemplate template, ClientLevel level, int lifted) {
        BlockPos origin = template.centre();
        // The view of the island: its own blocks and nothing else, copied so that the worker never reads the live
        // world.
        Long2ObjectOpenHashMap<BlockState> cells = new Long2ObjectOpenHashMap<>(template.size() * 2);
        BlockPos[] positions = new BlockPos[template.size()];
        for (int i = 0; i < template.size(); i++) {
            positions[i] = new BlockPos(template.x(i), template.y(i), template.z(i));
            cells.put(positions[i].asLong(), template.state(i));
        }
        View view = new View(level, cells);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        CompletableFuture<Built> future = CompletableFuture.supplyAsync(() -> make(dispatcher, view, positions, cells, origin), Util.backgroundExecutor());
        return new SokoviaMesh(origin, origin.below(lifted), future);
    }

    private static Built make(BlockRenderDispatcher dispatcher, View view, BlockPos[] positions, Long2ObjectOpenHashMap<BlockState> cells, BlockPos origin) {
        Map<RenderType, Layer> layers = new LinkedHashMap<>();
        PoseStack pose = new PoseStack();
        RandomSource random = RandomSource.create();
        ModelBlockRenderer.enableCaching();
        try {
            for (BlockPos pos : positions) {
                BlockState state = cells.get(pos.asLong());
                if (state == null || state.getRenderShape() != RenderShape.MODEL) {
                    continue;
                }
                BakedModel model = dispatcher.getBlockModel(state);
                ModelData data = model.getModelData(view, pos, state, ModelData.EMPTY);
                random.setSeed(state.getSeed(pos));
                for (RenderType type : model.getRenderTypes(state, random, data)) {
                    Layer layer = layers.computeIfAbsent(type, Layer::new);
                    pose.pushPose();
                    pose.translate(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
                    dispatcher.renderBatched(state, pos, view, pose, layer.builder, true, random, data, type);
                    pose.popPose();
                }
            }
        } finally {
            ModelBlockRenderer.clearCache();
        }
        Map<RenderType, MeshData> meshes = new LinkedHashMap<>();
        Map<RenderType, ByteBufferBuilder> memory = new LinkedHashMap<>();
        for (Map.Entry<RenderType, Layer> entry : layers.entrySet()) {
            MeshData mesh = entry.getValue().builder.build();
            if (mesh != null) {
                meshes.put(entry.getKey(), mesh);
            }
            memory.put(entry.getKey(), entry.getValue().memory);
        }
        return new Built(meshes, memory);
    }

    // ---- drawing ----

    /** Whether the worker is done (the picture can be drawn on the next frame). */
    boolean isReady() {
        return !closed && building.isDone() && !building.isCompletedExceptionally();
    }

    /** Whether the worker failed (the island is not drawn; the server's island is real all the same). */
    boolean failed() {
        return building.isCompletedExceptionally();
    }

    /** Uploads the buffers to the graphics card, once, on the render thread. */
    private void upload() {
        if (uploaded || !isReady()) {
            return;
        }
        uploaded = true;
        Built built = building.join();
        for (Map.Entry<RenderType, MeshData> entry : built.meshes().entrySet()) {
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(entry.getValue());
            VertexBuffer.unbind();
            buffers.put(entry.getKey(), buffer);
        }
        built.memory().values().forEach(ByteBufferBuilder::close);
    }

    /**
     * Draws one kind of geometry of the island, {@code offset} blocks above where it was copied from. The render state
     * of the kind is already set (this runs in the stage that follows the terrain's own pass of that kind), exactly as
     * the game draws a section of the terrain: the shader's chunk offset puts the island's origin in the right place.
     */
    void draw(RenderType type, org.joml.Matrix4f modelView, org.joml.Matrix4f projection, Vec3 camera, double offset) {
        upload();
        VertexBuffer buffer = buffers.get(type);
        ShaderInstance shader = RenderSystem.getShader();
        if (buffer == null || shader == null) {
            return;
        }
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, modelView, projection, Minecraft.getInstance().getWindow());
        shader.apply();
        Uniform chunkOffset = shader.CHUNK_OFFSET;
        if (chunkOffset != null) {
            chunkOffset.set((float) (ground.getX() - camera.x), (float) (ground.getY() + offset - camera.y), (float) (ground.getZ() - camera.z));
            chunkOffset.upload();
        }
        buffer.bind();
        buffer.draw();
        if (chunkOffset != null) {
            chunkOffset.set(0.0F, 0.0F, 0.0F);
        }
        shader.clear();
        VertexBuffer.unbind();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        buffers.values().forEach(VertexBuffer::close);
        buffers.clear();
        if (building.isDone() && !building.isCompletedExceptionally() && !uploaded) {
            Built built = building.join();
            built.meshes().values().forEach(MeshData::close);
            built.memory().values().forEach(ByteBufferBuilder::close);
        } else if (!building.isDone()) {
            // The worker is still at it: whatever it makes is let go when it is done.
            building.thenAccept(built -> {
                built.meshes().values().forEach(MeshData::close);
                built.memory().values().forEach(ByteBufferBuilder::close);
            });
        }
    }

    // ---- the island, as the block renderer sees the world ----

    /**
     * What the block renderer is shown instead of the world: the island's blocks (anything else is air, so the faces on
     * the island's surface are drawn), the world's biome tints and shade, and a fixed bright light.
     */
    private static final class View implements BlockAndTintGetter {
        private final ClientLevel level;
        private final Long2ObjectOpenHashMap<BlockState> cells;
        private final BlockState air = Blocks.AIR.defaultBlockState();

        View(ClientLevel level, Long2ObjectOpenHashMap<BlockState> cells) {
            this.level = level;
            this.cells = cells;
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            BlockState state = cells.get(pos.asLong());
            return state == null ? air : state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return level.getMinBuildHeight();
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return level.getShade(direction, shade);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return level.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return level.getBlockTint(pos, resolver);
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return layer == LightLayer.SKY ? 15 : 0;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int amount) {
            return Math.max(0, 15 - amount);
        }
    }

    /** For the log. */
    @Override
    public String toString() {
        return Enhanced.MODID + ":sokovia_mesh@" + origin.toShortString();
    }
}
