package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.SpellMath;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.platform.client.BlockModels;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A spell's ghost as a picture on the graphics card (D-B 6.2), made the way the Sokovia island's is
 * ({@code client/sokovia/SokoviaMesh}): every block of it drawn once, with its real model, textures, shade and biome
 * tint, into one buffer on a worker thread ({@link Util#backgroundExecutor()}), uploaded on the first frame after it is
 * ready, then drawn every frame where the ghost stands with the terrain's own translucent shader, whose chunk offset
 * puts the buffer's 0, 0, 0 there. So a ghost that follows the crosshair (a Copy's, a Move's, a shape's) moves, turns
 * and flips for nothing: only a change of what it holds makes a new one.
 *
 * <p>A face between two cells of the ghost is culled (the model renderer is shown the ghost's cells and nothing else),
 * so a solid fill draws only its outside. Water and lava are drawn as the terrain draws them. A block a block-entity
 * renderer draws (a chest, a sign, a bed) has no model to show: it is a cube outline instead. The cells a spell empties
 * get no block at all but a veil of dust on their open faces, in two speckled halves that the renderer fades into each
 * other, so the dust seems to dissolve. Colour and transparency are the renderer's ({@link Ghost}): the buffers hold
 * white dust and the blocks as they look.
 *
 * <p>Only ever loaded on a client; made on its thread, drawn and closed on its render thread.
 */
final class GhostMesh implements AutoCloseable {
    /** How far the dust stands out of the face it lies on, so it never fights the block's own face. */
    private static final float OUT = 0.004F;
    /** Open faces up to this many get specks of dust; more get one plain veil each (a Scarlet Witch's huge Vanish). */
    private static final int SPECKLED = 16_384;
    /** The specks across a face (a grid of this many each way, about half of them shown in each half of the dust). */
    private static final int SPECKS = 3;
    /** How far a block-entity block's cube outline stands inside its cell, and how thick its edges are. */
    private static final float CUBE_INSET = 0.08F;
    private static final float CUBE_EDGE = 0.045F;

    /**
     * What a ghost is made of, gathered on the client's thread (it reads the client's world) for a worker to draw: its
     * blocks and the open faces of the cells it empties, at offsets from {@link #base}. Not changed once handed over.
     */
    static final class Cells {
        /** Where the offset 0, 0, 0 stood in the world when the cells were gathered (biome tints, model variants). */
        final BlockPos base;
        /** Each block's offset (a packed {@link BlockPos}) and the block. */
        final LongArrayList blocks = new LongArrayList();
        final List<BlockState> states = new ArrayList<>();
        /** Each open face of a cell the spell empties: the cell's offset and its side ({@link Direction} ordinal). */
        final LongArrayList going = new LongArrayList();
        final ByteArrayList sides = new ByteArrayList();

        Cells(BlockPos base) {
            this.base = base.immutable();
        }

        void block(int x, int y, int z, BlockState state) {
            blocks.add(BlockPos.asLong(x, y, z));
            states.add(state);
        }

        void face(long offset, Direction side) {
            going.add(offset);
            sides.add((byte) side.ordinal());
        }

        boolean isEmpty() {
            return blocks.isEmpty() && going.isEmpty();
        }

        /** The box of every offset in it (null when it is empty). */
        @Nullable BoundingBox bounds() {
            if (isEmpty()) {
                return null;
            }
            long first = !blocks.isEmpty() ? blocks.getLong(0) : going.getLong(0);
            int[] box = {BlockPos.getX(first), BlockPos.getY(first), BlockPos.getZ(first), BlockPos.getX(first), BlockPos.getY(first),
                    BlockPos.getZ(first)};
            for (LongArrayList list : List.of(blocks, going)) {
                for (int i = 0; i < list.size(); i++) {
                    long cell = list.getLong(i);
                    box[0] = Math.min(box[0], BlockPos.getX(cell));
                    box[1] = Math.min(box[1], BlockPos.getY(cell));
                    box[2] = Math.min(box[2], BlockPos.getZ(cell));
                    box[3] = Math.max(box[3], BlockPos.getX(cell));
                    box[4] = Math.max(box[4], BlockPos.getY(cell));
                    box[5] = Math.max(box[5], BlockPos.getZ(cell));
                }
            }
            return new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]);
        }
    }

    /** The geometry as the worker made it (each piece null when there is none), and the memory it lies in. */
    private record Built(@Nullable MeshData blocks, @Nullable MeshData cubes, @Nullable MeshData dustA, @Nullable MeshData dustB,
                         List<ByteBufferBuilder> memory) {
        void close() {
            for (MeshData mesh : new MeshData[] {blocks, cubes, dustA, dustB}) {
                if (mesh != null) {
                    mesh.close();
                }
            }
            memory.forEach(ByteBufferBuilder::close);
        }
    }

    private final CompletableFuture<Built> building;
    private @Nullable VertexBuffer blocks;
    private @Nullable VertexBuffer cubes;
    private @Nullable VertexBuffer dustA;
    private @Nullable VertexBuffer dustB;
    private boolean uploaded;
    private boolean closed;

    private GhostMesh(CompletableFuture<Built> building) {
        this.building = building;
    }

    /** Starts drawing {@code cells} on a worker thread; {@code level} gives the biome tints and the shade. */
    static GhostMesh build(Cells cells, ClientLevel level) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        return new GhostMesh(CompletableFuture.supplyAsync(() -> make(dispatcher, level, cells), Util.backgroundExecutor()));
    }

    private static Built make(BlockRenderDispatcher dispatcher, ClientLevel level, Cells cells) {
        List<ByteBufferBuilder> memory = new ArrayList<>();
        try {
            return make(dispatcher, level, cells, memory);
        } catch (RuntimeException | Error failed) {
            // A block that cannot be drawn (a broken model of a mod's): no ghost, and no memory left behind.
            memory.forEach(ByteBufferBuilder::close);
            throw failed;
        }
    }

    private static Built make(BlockRenderDispatcher dispatcher, ClientLevel level, Cells cells, List<ByteBufferBuilder> memory) {
        BlockPos base = cells.base;
        // The world the block renderer is shown: the ghost's own blocks (in world positions) and nothing else.
        Long2ObjectOpenHashMap<BlockState> shown = new Long2ObjectOpenHashMap<>(cells.blocks.size() * 2);
        for (int i = 0; i < cells.blocks.size(); i++) {
            long cell = cells.blocks.getLong(i);
            shown.put(BlockPos.asLong(BlockPos.getX(cell) + base.getX(), BlockPos.getY(cell) + base.getY(), BlockPos.getZ(cell) + base.getZ()),
                    cells.states.get(i));
        }
        View view = new View(level, shown);
        BufferBuilder blocks = begin(memory, DefaultVertexFormat.BLOCK, 256 * Math.max(1, cells.blocks.size()));
        BufferBuilder cubes = null;
        PoseStack pose = new PoseStack();
        RandomSource random = RandomSource.create();
        ModelBlockRenderer.enableCaching();
        try {
            for (int i = 0; i < cells.blocks.size(); i++) {
                long cell = cells.blocks.getLong(i);
                int x = BlockPos.getX(cell);
                int y = BlockPos.getY(cell);
                int z = BlockPos.getZ(cell);
                BlockState state = cells.states.get(i);
                BlockPos at = base.offset(x, y, z);
                if (state.getRenderShape() == RenderShape.MODEL) {
                    random.setSeed(state.getSeed(at));
                    pose.pushPose();
                    pose.translate(x, y, z);
                    BlockModels.renderBatched(dispatcher, state, at, view, pose, true, random, type -> blocks);
                    pose.popPose();
                } else if (state.getBlock() instanceof LiquidBlock) {
                    // The liquid renderer writes a section's offsets (each axis & 15): moved to the cell's own.
                    dispatcher.renderLiquid(at, view, new Shifted(blocks, x - (at.getX() & 15), y - (at.getY() & 15), z - (at.getZ() & 15)),
                            state, state.getFluidState());
                } else if (!state.isAir()) {
                    if (cubes == null) {
                        cubes = begin(memory, DefaultVertexFormat.POSITION_COLOR, 4096);
                    }
                    cube(cubes, x, y, z);
                }
            }
        } finally {
            ModelBlockRenderer.clearCache();
        }
        MeshData dustA = null;
        MeshData dustB = null;
        if (!cells.going.isEmpty()) {
            BufferBuilder a = begin(memory, DefaultVertexFormat.POSITION_COLOR, 64 * cells.going.size());
            BufferBuilder b = begin(memory, DefaultVertexFormat.POSITION_COLOR, 64 * cells.going.size());
            boolean specks = cells.going.size() <= SPECKLED;
            for (int i = 0; i < cells.going.size(); i++) {
                long cell = cells.going.getLong(i);
                Direction side = Direction.values()[cells.sides.getByte(i)];
                dust(a, cell, side, specks, 1);
                dust(b, cell, side, specks, 2);
            }
            dustA = a.build();
            dustB = b.build();
        }
        return new Built(blocks.build(), cubes == null ? null : cubes.build(), dustA, dustB, memory);
    }

    private static BufferBuilder begin(List<ByteBufferBuilder> memory, VertexFormat format, int bytes) {
        ByteBufferBuilder room = new ByteBufferBuilder(Math.min(Math.max(bytes, 4096), 1 << 24));
        memory.add(room);
        return new BufferBuilder(room, VertexFormat.Mode.QUADS, format);
    }

    /** A block-entity block's cube outline: faint faces and bright edges, white (the renderer tints them). */
    private static void cube(BufferBuilder out, int x, int y, int z) {
        float lo = CUBE_INSET;
        float hi = 1.0F - CUBE_INSET;
        Vector3f min = new Vector3f(x + lo, y + lo, z + lo);
        Vector3f max = new Vector3f(x + hi, y + hi, z + hi);
        int faint = 0x40FFFFFF;
        int bright = 0xE0FFFFFF;
        for (Direction side : Direction.values()) {
            Vector3f[] face = face(min, max, side);
            for (Vector3f p : face) {
                out.addVertex(p.x, p.y, p.z).setColor(faint);
            }
        }
        // Each edge as two thin crossed strips, so it is seen from every side.
        float h = CUBE_EDGE / 2.0F;
        for (int axis = 0; axis < 3; axis++) {
            for (int corner = 0; corner < 4; corner++) {
                float[] at = new float[3];
                int u = (axis + 1) % 3;
                int v = (axis + 2) % 3;
                at[u] = (corner & 1) == 0 ? component(min, u) : component(max, u);
                at[v] = (corner & 2) == 0 ? component(min, v) : component(max, v);
                float from = component(min, axis);
                float to = component(max, axis);
                strip(out, axis, u, at, from, to, h, bright);
                strip(out, axis, v, at, from, to, h, bright);
            }
        }
    }

    private static void strip(BufferBuilder out, int axis, int across, float[] at, float from, float to, float half, int argb) {
        float[][] corners = {{from, -half}, {to, -half}, {to, half}, {from, half}};
        for (float[] c : corners) {
            float[] p = at.clone();
            p[axis] = c[0];
            p[across] += c[1];
            out.addVertex(p[0], p[1], p[2]).setColor(argb);
        }
    }

    private static float component(Vector3f v, int axis) {
        return axis == 0 ? v.x : axis == 1 ? v.y : v.z;
    }

    /** The four corners of a box's face on {@code side}. */
    private static Vector3f[] face(Vector3f min, Vector3f max, Direction side) {
        int axis = side.getAxis().ordinal();
        int u = (axis + 1) % 3;
        int v = (axis + 2) % 3;
        float plane = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? component(max, axis) : component(min, axis);
        Vector3f[] out = new Vector3f[4];
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int i = 0; i < 4; i++) {
            float[] p = new float[3];
            p[axis] = plane;
            p[u] = uv[i][0] == 0 ? component(min, u) : component(max, u);
            p[v] = uv[i][1] == 0 ? component(min, v) : component(max, v);
            out[i] = new Vector3f(p[0], p[1], p[2]);
        }
        return out;
    }

    /**
     * One half ({@code half} 1 or 2) of the dust on an open face of a cell that goes: about half of a grid of small
     * specks, each as bright as the hash of where it is says, standing just out of the face; one plain veil when
     * {@code specks} is false.
     */
    private static void dust(BufferBuilder out, long cell, Direction side, boolean specks, int half) {
        int x = BlockPos.getX(cell);
        int y = BlockPos.getY(cell);
        int z = BlockPos.getZ(cell);
        int axis = side.getAxis().ordinal();
        int u = (axis + 1) % 3;
        int v = (axis + 2) % 3;
        float[] origin = {x, y, z};
        float plane = origin[axis] + (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F + OUT : -OUT);
        if (!specks) {
            square(out, axis, u, v, plane, origin[u], origin[u] + 1.0F, origin[v], origin[v] + 1.0F, 0x99FFFFFF);
            return;
        }
        float step = 1.0F / SPECKS;
        for (int i = 0; i < SPECKS * SPECKS; i++) {
            long hash = SpellMath.hash(half * 7919L + side.ordinal() * 31L + i, x, y, z);
            if ((hash & 1) == 0) {
                continue;
            }
            float size = step * (0.45F + 0.4F * ((hash >>> 8) & 0xFF) / 255.0F);
            float shiftU = (step - size) * (((hash >>> 16) & 0xFF) / 255.0F);
            float shiftV = (step - size) * (((hash >>> 24) & 0xFF) / 255.0F);
            float u0 = origin[u] + (i % SPECKS) * step + shiftU;
            float v0 = origin[v] + (i / SPECKS) * step + shiftV;
            int alpha = 128 + (int) ((hash >>> 32) & 0x7F);
            square(out, axis, u, v, plane, u0, u0 + size, v0, v0 + size, alpha << 24 | 0xFFFFFF);
        }
    }

    private static void square(BufferBuilder out, int axis, int u, int v, float plane, float u0, float u1, float v0, float v1, int argb) {
        float[][] corners = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        for (float[] c : corners) {
            float[] p = new float[3];
            p[axis] = plane;
            p[u] = c[0];
            p[v] = c[1];
            out.addVertex(p[0], p[1], p[2]).setColor(argb);
        }
    }

    // ---- drawing ----

    /** Whether the worker is done (the ghost can be drawn on the next frame). */
    boolean isReady() {
        return !closed && building.isDone() && !building.isCompletedExceptionally();
    }

    /** Whether the worker failed (no ghost is drawn; the spell is cast all the same). */
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
        blocks = upload(built.blocks());
        cubes = upload(built.cubes());
        dustA = upload(built.dustA());
        dustB = upload(built.dustB());
        built.memory().forEach(ByteBufferBuilder::close);
    }

    private static @Nullable VertexBuffer upload(@Nullable MeshData mesh) {
        if (mesh == null) {
            return null;
        }
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();
        return buffer;
    }

    /**
     * Draws the blocks in {@code type} ({@link EnhancedRenderTypes#BUILD_GHOST} and its flat and glowing kin) with the
     * shader colour {@code r, g, b, a}: {@code offset} (the buffer's 0, 0, 0 from the camera) is the chunk offset, and
     * {@code modelView} already holds any turn or flip about it.
     */
    void drawBlocks(RenderType type, Matrix4f modelView, Matrix4f projection, Vector3f offset, float r, float g, float b, float a) {
        upload();
        draw(blocks, type, modelView, projection, offset, r, g, b, a);
    }

    /** Draws the block-entity blocks' cube outlines in {@code rgb}; {@code modelView} stands at the buffer's origin. */
    void drawCubes(Matrix4f modelView, Matrix4f projection, int rgb, float a) {
        upload();
        draw(cubes, EnhancedRenderTypes.ENERGY_VEIL, modelView, projection, null, (rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F, a);
    }

    /** Draws the dust, its two halves as bright as {@code a} and {@code b}, in {@code rgb}. */
    void drawDust(Matrix4f modelView, Matrix4f projection, int rgb, float a, float b) {
        upload();
        float red = (rgb >> 16 & 0xFF) / 255.0F;
        float green = (rgb >> 8 & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        draw(dustA, EnhancedRenderTypes.ENERGY_VEIL, modelView, projection, null, red, green, blue, a);
        draw(dustB, EnhancedRenderTypes.ENERGY_VEIL, modelView, projection, null, red, green, blue, b);
    }

    private static void draw(@Nullable VertexBuffer buffer, RenderType type, Matrix4f modelView, Matrix4f projection,
                             @Nullable Vector3f offset, float r, float g, float b, float a) {
        if (buffer == null || a <= 0.0F) {
            return;
        }
        type.setupRenderState();
        RenderSystem.setShaderColor(r, g, b, a);
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            Uniform chunkOffset = shader.CHUNK_OFFSET;
            if (chunkOffset != null) {
                chunkOffset.set(offset != null ? offset.x : 0.0F, offset != null ? offset.y : 0.0F, offset != null ? offset.z : 0.0F);
            }
            buffer.bind();
            buffer.drawWithShader(modelView, projection, shader);
            VertexBuffer.unbind();
            if (chunkOffset != null) {
                chunkOffset.set(0.0F, 0.0F, 0.0F);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        type.clearRenderState();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (VertexBuffer buffer : new VertexBuffer[] {blocks, cubes, dustA, dustB}) {
            if (buffer != null) {
                buffer.close();
            }
        }
        if (building.isDone() && !building.isCompletedExceptionally() && !uploaded) {
            building.join().close();
        } else if (!building.isDone()) {
            // The worker is still at it: whatever it makes is let go when it is done.
            building.thenAccept(Built::close);
        }
    }

    // ---- the ghost, as the block renderer sees the world ----

    /** A vertex consumer that moves every vertex by a fixed offset (the liquid renderer's section offsets). */
    private record Shifted(VertexConsumer to, float dx, float dy, float dz) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            to.addVertex(x + dx, y + dy, z + dz);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            to.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            to.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            to.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            to.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            to.setNormal(x, y, z);
            return this;
        }
    }

    /**
     * What the block renderer is shown instead of the world: the ghost's blocks (anything else is air, so a ghost's
     * outside faces are drawn and those between two of its cells are not), the world's biome tints and shade, and full
     * light, so a ghost glows the same by night.
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
            return 15;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int amount) {
            return 15;
        }
    }
}
