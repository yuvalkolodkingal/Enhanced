package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.MagicSelect;
import com.kolodkingal.enhanced.build.SpellMath;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The glow of a thing Build Magic has selected (D-B 3.4): every face of its cells that touches open air lights up in
 * the hero's colour as the client's flood ({@link BuildClient#flood}) reaches it, so the whole tree or house is seen
 * at once, inside its dotted box.
 *
 * <p>A thing may have a hundred thousand cells (Wanda's {@code magicSelectMax}), far too many faces to send every
 * frame, so the faces are kept in one buffer on the graphics card, relative to the thing's first seed and drawn at an
 * offset from the camera. The buffer is made again only when the flood has found more cells (a few times while it
 * spreads, at most once a frame), from the faces found so far: a cell's open faces are looked for once, when it is
 * first seen. At most {@value #MAX_FACES} faces glow.
 *
 * <p>Only ever loaded on a client, and only used on its render thread.
 */
final class SelectGlow {
    /** The most faces that glow (half a million vertices). */
    static final int MAX_FACES = 131_072;
    /** How far a glowing face stands out of its block's face. */
    private static final float OUT = 0.002F;
    /** The corners of each face of a cell (bit 0: x, bit 1: y, bit 2: z), in the order of {@link Direction}. */
    private static final int[][] FACES = {{0, 1, 5, 4}, {2, 6, 7, 3}, {0, 2, 3, 1}, {4, 5, 7, 6}, {0, 4, 6, 2}, {1, 3, 7, 5}};
    private static final Direction[] SIDES = Direction.values();

    /** The flood the glow is of, and the colour it was made in. */
    private static @Nullable MagicSelect source;
    private static int colour;
    /** Where the buffer's 0, 0, 0 is: the thing's first seed. */
    private static BlockPos origin = BlockPos.ZERO;
    /** The cells already looked at, and the faces found: a cell and its side. */
    private static final LongOpenHashSet SEEN = new LongOpenHashSet();
    private static final LongArrayList CELLS = new LongArrayList();
    private static final ByteArrayList SIDE = new ByteArrayList();
    /** How many cells the flood had when it was last looked at, and how many faces the buffer holds. */
    private static int seenSize = -1;
    private static int built = -1;
    private static @Nullable VertexBuffer buffer;

    /**
     * Draws the glow of {@code flood} (none: the last glow is let go) in {@code rgb}, as bright as {@code alpha}.
     * Called once a frame from {@link BuildRenderer}, between its batches.
     */
    static void draw(RenderLevelStageEvent event, ClientLevel level, @Nullable MagicSelect flood, int rgb, float alpha) {
        if (flood == null) {
            clear();
            return;
        }
        if (flood != source || rgb != colour) {
            clear();
            source = flood;
            colour = rgb;
            origin = flood.seeds().isEmpty() ? BlockPos.ZERO : flood.seeds().get(0);
        }
        if (flood.size() != seenSize) {
            gather(level, flood);
        }
        if (CELLS.size() != built) {
            build();
        }
        BoundingBox bounds = flood.bounds();
        if (buffer == null || built == 0 || bounds == null || !event.getFrustum().isVisible(new AABB(bounds.minX(), bounds.minY(),
                bounds.minZ(), bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1))) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f modelView = new Matrix4f(event.getModelViewMatrix()).translate((float) (origin.getX() - camera.x),
                (float) (origin.getY() - camera.y), (float) (origin.getZ() - camera.z));
        EnhancedRenderTypes.BUILD_GLOW.setupRenderState();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            buffer.bind();
            buffer.drawWithShader(modelView, event.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        EnhancedRenderTypes.BUILD_GLOW.clearRenderState();
    }

    /** Looks at the cells the flood found since the last look: each face of one that touches open air glows. */
    private static void gather(ClientLevel level, MagicSelect flood) {
        LongSet found = flood.found();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        LongIterator cells = found.iterator();
        while (cells.hasNext() && CELLS.size() < MAX_FACES) {
            long cell = cells.nextLong();
            if (!SEEN.add(cell)) {
                continue;
            }
            for (Direction side : SIDES) {
                long next = BlockPos.offset(cell, side);
                // A cell of the thing is never open (the flood takes no air), so it hides the face without a read.
                if (found.contains(next) || !SpellMath.isEmpty(level.getBlockState(at.set(next)))) {
                    continue;
                }
                CELLS.add(cell);
                SIDE.add((byte) side.ordinal());
            }
        }
        seenSize = flood.size();
    }

    /** Puts the faces found so far into the buffer on the graphics card. */
    private static void build() {
        built = CELLS.size();
        if (buffer == null) {
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int argb = 0xFF000000 | colour;
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        for (int i = 0; i < built; i++) {
            long cell = CELLS.getLong(i);
            Direction side = SIDES[SIDE.getByte(i)];
            float x = BlockPos.getX(cell) - ox;
            float y = BlockPos.getY(cell) - oy;
            float z = BlockPos.getZ(cell) - oz;
            float dx = side.getStepX() * OUT;
            float dy = side.getStepY() * OUT;
            float dz = side.getStepZ() * OUT;
            for (int corner : FACES[side.ordinal()]) {
                builder.addVertex(x + (corner & 1) + dx, y + ((corner >> 1) & 1) + dy, z + ((corner >> 2) & 1) + dz).setColor(argb);
            }
        }
        MeshData mesh = builder.build();
        if (mesh == null) {
            built = 0;
            return;
        }
        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();
    }

    /** Lets go of the glow and its buffer (no thing selected, build mode off, a logout). */
    static void clear() {
        source = null;
        SEEN.clear();
        SEEN.trim();
        CELLS.clear();
        CELLS.trim();
        SIDE.clear();
        SIDE.trim();
        seenSize = -1;
        built = -1;
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    private SelectGlow() {
    }
}
