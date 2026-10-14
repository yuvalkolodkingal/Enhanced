package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellMath;
import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The ghost of the spell the builder is about to cast (D-B 6.2): the blocks it will make, seen through, tinted with the
 * hero's glow, where they will stand; a veil of dust on what it will empty (Vanish, a Room's inside, Push, a Move's
 * source). The cells come from the same planner the server builds with ({@link SpellPlanner}), resolved against this
 * client's world, so the ghost is the result whenever the two worlds agree.
 *
 * <p><b>What is shown.</b> Only what changes: a Fill's new cells, a Change's changed ones, a Paint dab's painted
 * skin, a Pull's new layers, a Repeat's copies, each over (or instead of) the block it replaces. A Copy's stamp shows
 * the pocket and a Move's put-down the box it carries, as they are; a shape shows its every cell. A cell in a chunk
 * this client has not loaded is left out (the label says "about").
 *
 * <p><b>Made once, moved for nothing.</b> The cells are gathered on the client's tick, {@value #GATHER_PER_TICK} a
 * tick, and drawn into a {@link GhostMesh} on a worker thread. A new mesh is made only when what the ghost holds
 * changes (its plan, its material, its pocket, the world under it after a spell); a ghost that follows the crosshair
 * (a stamp, a put-down, a shape) is a mesh in its own frame, placed each frame where the plan stands, turned and
 * flipped by a matrix. Until a new mesh is ready the last one stays.
 *
 * <p><b>The detail cap</b> ({@code ghostDetail}, 32,768): a ghost of more blocks than that shows only those with an
 * open side; one of more than four times that is a box tinted with the material's map colour, with a one-block grid on
 * its near faces, and the blocks within {@value #NEAR} of the crosshair (at most {@code ghostDetail} of them) in
 * detail, gathered again as the crosshair moves.
 *
 * <p><b>Grey.</b> A spell the server refused (every cell of it, or the whole spell: too big for Billy, a block magic
 * never makes) or one that costs Billy more than his whole meter shows flat grey and does not glow; the cells round the
 * preflight's marks are veiled grey.
 *
 * <p>Only ever loaded on a client; its tick and its drawing run on the client's thread.
 */
final class Ghost {
    /** Cells looked at a client tick while a ghost is gathered (the client count's pace, D-B 14.7). */
    static final int GATHER_PER_TICK = 50_000;
    /** How far round the crosshair a ghost too big to draw whole still shows its blocks (D-B 6.2). */
    static final int NEAR = 32;
    /** The crosshair moves this far (a power of two) before the blocks round it are gathered again. */
    private static final int NEAR_STEP = 8;
    /** The most cells of its plan a big ghost walks to find the blocks round the crosshair. */
    private static final long NEAR_WALK = 8_000_000;
    /** A cell walked but too far from the crosshair costs this fraction of the gathering's pace. */
    private static final int FAR_COST = 16;

    /** How see-through the blocks are: 0.45, pulsing between 0.35 and 0.55 (steady under reduced motion). */
    private static final float ALPHA = 0.45F;
    private static final float PULSE = 0.10F;
    /** How bright the hero's glow over the blocks is, and a refused ghost's grey. */
    private static final float GLOW = 0.15F;
    private static final float GREY = 0.6F;
    /** How bright the dust is. */
    private static final float DUST = 0.6F;
    /** A big ghost's box: its faces, and its grid round the camera (blocks each way). */
    private static final float BOX_FACE = 0.2F;
    private static final float GRID = 0.45F;
    private static final int GRID_REACH = 24;

    /** How a layer's cells stand in the world. */
    private enum Placing {
        /** Where they were gathered: box spells, Paint, Pull, Repeat, a Move's source, a big ghost's near blocks. */
        WORLD,
        /** On the plan's low corner, turned and flipped as its recipe says: a Copy's stamp, a Move's put-down. */
        CARRIED,
        /** On the plan's origin: a shape. */
        SHAPE
    }

    /**
     * How a mesh stands: its frame, where its 0, 0, 0 is (for {@link Placing#WORLD}; the others follow the plan), the
     * size of the box a carried copy turns in, and the box of its cells in its own frame.
     */
    private record Frame(Placing placing, BlockPos base, int sizeX, int sizeY, int sizeZ, BoundingBox local) {
    }

    /** The ghost's two pictures: what the spell makes and empties, and a Move's source, which stays where it is. */
    private static final Layer MAIN = new Layer();
    private static final Layer SOURCE = new Layer();
    /** A ghost too big to show whole: the colour of its box (-1: the hero's), else null. */
    private static @Nullable Integer boxColour;

    // ------------------------------------------------------------------------------------------------------------
    // Every tick

    /**
     * Brings the ghost up to the plan the builder sees now ({@link BuildClient#ghostPlan}): a new gathering when what
     * it holds changed, the gathering walked on, a gathered ghost handed to a worker, a finished mesh shown.
     */
    static void tick(ClientLevel level, LocalPlayer player) {
        int detail = Math.max(0, EnhancedClientConfig.buildGhostDetail());
        SpellRecipe recipe = BuildClient.ghostRecipe();
        SpellPlanner.Plan plan = BuildClient.ghostPlan();
        BuildGrammar.Aim aim = BuildClient.aim();
        if (recipe != null && plan != null && !plan.prepared()) {
            // A Pull's surface is still being found: the last ghost stays until the new plan is ready.
            walk(level);
            return;
        }
        if (recipe == null || plan == null || aim == null || plan.refusal() != SpellPlanner.NONE || !plan.writes()) {
            clear();
            return;
        }
        SpellRecipe planned = plan.recipe();
        BlockPos near = crosshair(aim, plan.bounds());
        long most = 4L * detail;
        switch (planned.spell()) {
            case COPY, MOVE -> carried(level, plan, planned, near, detail, most);
            case SHAPE -> shape(level, plan, planned, near, detail, most);
            default -> world(level, plan, near, detail, most);
        }
        if (planned.spell() != Spell.MOVE) {
            SOURCE.clear();
        }
        walk(level);
    }

    /** A box spell, Paint, Pull or Repeat: its changed cells where they are. */
    private static void world(ClientLevel level, SpellPlanner.Plan plan, BlockPos near, int detail, long most) {
        BoundingBox bounds = plan.bounds();
        if (bounds == null) {
            clear();
            return;
        }
        boolean big = plan.size() > most;
        Object key = List.of("world", BuildClient.ghostKey() != null ? BuildClient.ghostKey() : "none", detail, big ? nearKey(near) : 0);
        if (MAIN.wants(key)) {
            boxColour = big ? mapColour(level, plan.recipe(), bounds) : null;
            if (big) {
                // The same plan walked from the crosshair (a Pull's surface is found already: its own plan).
                SpellPlanner.Plan around = plan.recipe().spell() == Spell.PULL ? plan
                        : SpellPlanner.plan(level, plan.recipe(), BuildClient.setup(plan.recipe(), near).withWindow(0));
                MAIN.gather = new PlanWalk(around, Placing.WORLD, near, true, near, detail);
            } else {
                MAIN.gather = new PlanWalk(plan, Placing.WORLD, new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()), true, null, detail);
            }
        }
    }

    /** A shape: its every cell in its own frame, carried where the plan stands. */
    private static void shape(ClientLevel level, SpellPlanner.Plan plan, SpellRecipe planned, BlockPos near, int detail, long most) {
        BlockPos origin = plan.origin();
        BoundingBox bounds = plan.bounds();
        if (origin == null || bounds == null) {
            clear();
            return;
        }
        boolean big = plan.size() > most;
        Object key = List.of("shape", planned.shapeKind(), planned.size(), planned.size2(), planned.hollow(), planned.turn(),
                BuildClient.materialKey() != null ? BuildClient.materialKey() : "none", planned.mix() ? origin : 0, detail,
                big ? nearKey(near.subtract(origin)) : 0);
        if (MAIN.wants(key)) {
            boxColour = big ? mapColour(level, planned, bounds) : null;
            SpellPlanner.Plan walked = big ? SpellPlanner.plan(level, planned, BuildClient.setup(planned, near).withWindow(0)) : plan;
            MAIN.gather = new PlanWalk(walked, Placing.SHAPE, origin, false, big ? near : null, detail);
        }
    }

    /** A Copy's stamp (the pocket) or a Move's put-down (the box it lifts, read live): carried at the crosshair. */
    private static void carried(ClientLevel level, SpellPlanner.Plan plan, SpellRecipe planned, BlockPos near, int detail, long most) {
        boolean move = planned.spell() == Spell.MOVE;
        SpellMath.Volume region = plan.region();
        SpellPlanner.Source source = move ? (region == null ? null : SpellPlanner.Source.live(level, region)) : BuildClient.pocket();
        BlockPos origin = plan.origin();
        if (source == null || origin == null) {
            clear();
            return;
        }
        long volume = (long) source.sizeX() * source.sizeY() * source.sizeZ();
        boolean big = source.cells() > most || volume > 16 * Math.max(most, 1L);
        BlockPos from = null;
        if (big) {
            // The pocket's cell under the crosshair: the stamp's cell there, turned and flipped back.
            SpellMath.Placement placement = new SpellMath.Placement(source.sizeX(), source.sizeY(), source.sizeZ(), planned.turn(), planned.flip());
            BlockPos at = near.subtract(origin);
            from = placement.source(Mth.clamp(at.getX(), 0, placement.targetSizeX() - 1), Mth.clamp(at.getY(), 0, source.sizeY() - 1),
                    Mth.clamp(at.getZ(), 0, placement.targetSizeZ() - 1));
        }
        Object what = move ? List.of(planned.region(), planned.a(), planned.b(), planned.seeds(), BuildClient.epoch()) : source;
        Object key = List.of("carried", what, detail, from != null ? nearKey(from) : 0);
        if (MAIN.wants(key)) {
            boxColour = big ? -1 : null;
            MAIN.gather = new SourceWalk(source, from, detail);
        }
        if (move && region != null) {
            // What the Move empties stays where it is, under its dust.
            Object source2 = List.of("source", planned.region(), planned.a(), planned.b(), planned.seeds(), BuildClient.epoch(), detail);
            if (SOURCE.wants(source2)) {
                BoundingBox box = region.bounds();
                SOURCE.gather = new SourceDust(SpellPlanner.Source.live(level, region), new BlockPos(box.minX(), box.minY(), box.minZ()), detail);
            }
        }
    }

    /**
     * The cell of the ghost under the crosshair: where the view ray enters the plan's box (just inside it), the
     * builder's own cell inside it, or the aimed cell when the ray misses it. A big ghost shows its blocks round it.
     */
    private static BlockPos crosshair(BuildGrammar.Aim aim, @Nullable BoundingBox bounds) {
        if (bounds == null) {
            return aim.target();
        }
        AABB box = new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1);
        if (box.contains(aim.eyes())) {
            return BlockPos.containing(aim.eyes());
        }
        Vec3 look = aim.look().normalize();
        return box.clip(aim.eyes(), aim.eyes().add(look.scale(4096))).map(hit -> BlockPos.containing(hit.add(look.scale(0.5))))
                .orElse(aim.target());
    }

    /** Where the crosshair is, to the nearest {@value #NEAR_STEP} blocks: a big ghost's blocks round it follow it. */
    private static Object nearKey(BlockPos at) {
        int shift = Integer.numberOfTrailingZeros(NEAR_STEP);
        return List.of(at.getX() >> shift, at.getY() >> shift, at.getZ() >> shift);
    }

    /**
     * The map colour of the material in the middle of a big ghost, or -1 (the hero's colour) for a spell that makes no
     * block of its own (Vanish, Pull, Repeat).
     */
    private static int mapColour(ClientLevel level, SpellRecipe recipe, BoundingBox box) {
        SpellPlanner.Material material = BuildClient.material();
        if (material == null || !recipe.spell().needsMaterial()) {
            return -1;
        }
        BlockPos middle = box.getCenter();
        BlockState state = material.at(middle.getX(), middle.getY(), middle.getZ());
        int colour = state.getMapColor(level, middle).col;
        return colour == 0 ? -1 : colour;
    }

    /** Walks the layers' gatherings on, within the tick's pace, and hands what is gathered to the workers. */
    private static void walk(ClientLevel level) {
        int left = MAIN.tick(level, GATHER_PER_TICK);
        SOURCE.tick(level, Math.max(left, GATHER_PER_TICK / 4));
    }

    /** Lets go of the ghost and its meshes (nothing to show, build mode off, a new world, a logout). */
    static void clear() {
        MAIN.clear();
        SOURCE.clear();
        boxColour = null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    /**
     * Draws the ghost after the world's translucent blocks (D-B 11.10: it blends over water). {@code core} and
     * {@code glow} are the hero's colours, {@code time} the game time with the frame's part of a tick.
     */
    static void draw(RenderLevelStageEvent event, LocalPlayer player, int core, int glow, float time, boolean calm) {
        SpellRecipe recipe = BuildClient.ghostRecipe();
        SpellPlanner.Plan plan = BuildClient.ghostPlan();
        if (recipe == null || plan == null || !plan.prepared() || plan.refusal() != SpellPlanner.NONE) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f modelView = event.getModelViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
        boolean grey = BuildClient.refused(player);
        float alpha = calm ? ALPHA : ALPHA + PULSE * Mth.sin(time * 0.12F);
        float swirl = calm ? 0.5F : 0.5F + 0.5F * Mth.sin(time * 0.2F);
        int dust = grey ? 0x9A9A9A : core;
        // The glow: the hero's colour, a third of the way to its highlight (Billy's highlight alone is nearly white).
        int tint = mix(core, glow, 0.35F);
        // A big ghost's box, tinted with its material, and its grid.
        BoundingBox bounds = plan.bounds();
        if (boxColour != null && bounds != null) {
            box(event, bounds, camera, grey ? 0x9A9A9A : boxColour < 0 ? glow : boxColour, glow);
        }
        for (Layer layer : new Layer[] {MAIN, SOURCE}) {
            GhostMesh mesh = layer.shown;
            Frame frame = layer.frame;
            if (mesh == null || frame == null || !mesh.isReady()) {
                continue;
            }
            Matrix4f turned = new Matrix4f();
            BlockPos base = place(frame, plan, turned);
            if (base == null) {
                continue;
            }
            BoundingBox local = frame.local();
            AABB seen = frame.placing() == Placing.CARRIED && bounds != null
                    ? new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1)
                    : new AABB(base.getX() + local.minX(), base.getY() + local.minY(), base.getZ() + local.minZ(), base.getX() + local.maxX() + 1,
                    base.getY() + local.maxY() + 1, base.getZ() + local.maxZ() + 1);
            if (!event.getFrustum().isVisible(seen.inflate(1.0))) {
                continue;
            }
            Vector3f offset = new Vector3f((float) (base.getX() - camera.x), (float) (base.getY() - camera.y), (float) (base.getZ() - camera.z));
            // The blocks: the turn is about the buffer's 0, 0, 0, which the chunk offset puts in its place.
            Matrix4f blocks = new Matrix4f(modelView).translate(offset).mul(turned).translate(-offset.x, -offset.y, -offset.z);
            Matrix4f placed = new Matrix4f(modelView).translate(offset).mul(turned);
            mesh.drawCubes(placed, projection, grey ? 0x9A9A9A : glow, 0.9F);
            mesh.drawDust(placed, projection, dust, DUST * swirl, DUST * (1.0F - swirl));
            // The blocks: their nearest faces into the depth buffer first, so only those are seen, then their look
            // and the hero's glow over it (or, refused, flat grey).
            mesh.drawBlocks(EnhancedRenderTypes.BUILD_GHOST_DEPTH, blocks, projection, offset, 1.0F, 1.0F, 1.0F, 1.0F);
            if (grey) {
                mesh.drawBlocks(EnhancedRenderTypes.BUILD_GHOST_FLAT, blocks, projection, offset, GREY, GREY, GREY, alpha + 0.1F);
            } else {
                mesh.drawBlocks(EnhancedRenderTypes.BUILD_GHOST, blocks, projection, offset, 1.0F, 1.0F, 1.0F, alpha);
                mesh.drawBlocks(EnhancedRenderTypes.BUILD_GHOST_GLOW, blocks, projection, offset, red(tint), green(tint), blue(tint),
                        GLOW * alpha / ALPHA);
            }
        }
        if (!grey) {
            greyRoundMarks(plan, camera);
        }
    }

    /**
     * Where a layer's buffer stands in the world now, with its turn and flip set in {@code turned}; null when the plan
     * shown is not one this mesh fits (a mesh of another spell, waiting for its successor).
     */
    private static @Nullable BlockPos place(Frame frame, SpellPlanner.Plan plan, Matrix4f turned) {
        SpellRecipe planned = plan.recipe();
        return switch (frame.placing()) {
            case WORLD -> frame.base();
            case SHAPE -> planned.spell() == Spell.SHAPE ? plan.origin() : null;
            case CARRIED -> {
                boolean carried = planned.op() == SpellRecipe.Op.STAMP || planned.op() == SpellRecipe.Op.PUT_DOWN;
                if (!carried || plan.origin() == null) {
                    yield null;
                }
                turned.set(placement(frame.sizeX(), frame.sizeY(), frame.sizeZ(), planned.turn(), planned.flip()));
                yield plan.origin();
            }
        };
    }

    /**
     * The turn and flip of a stamp as a matrix on its pocket's offsets: {@link SpellMath.Placement#target} for whole
     * cells, here for every point of them (cell {@code x} spans {@code x} to {@code x + 1}).
     */
    private static Matrix4f placement(int sizeX, int sizeY, int sizeZ, int turn, int flip) {
        Vector3f o = target(0, 0, 0, sizeX, sizeY, sizeZ, turn, flip);
        Vector3f x = target(1, 0, 0, sizeX, sizeY, sizeZ, turn, flip).sub(o);
        Vector3f y = target(0, 1, 0, sizeX, sizeY, sizeZ, turn, flip).sub(o);
        Vector3f z = target(0, 0, 1, sizeX, sizeY, sizeZ, turn, flip).sub(o);
        return new Matrix4f(x.x, x.y, x.z, 0.0F, y.x, y.y, y.z, 0.0F, z.x, z.y, z.z, 0.0F, o.x, o.y, o.z, 1.0F);
    }

    private static Vector3f target(float x, float y, float z, int sizeX, int sizeY, int sizeZ, int turn, int flip) {
        float tx;
        float tz;
        switch (Math.floorMod(turn, 4)) {
            case 1 -> {
                tx = sizeZ - z;
                tz = x;
            }
            case 2 -> {
                tx = sizeX - x;
                tz = sizeZ - z;
            }
            case 3 -> {
                tx = z;
                tz = sizeX - x;
            }
            default -> {
                tx = x;
                tz = z;
            }
        }
        float ty = y;
        boolean across = (Math.floorMod(turn, 4) & 1) == 1;
        switch (flip) {
            case SpellRecipe.FLIP_X -> tx = (across ? sizeZ : sizeX) - tx;
            case SpellRecipe.FLIP_Z -> tz = (across ? sizeX : sizeZ) - tz;
            case SpellRecipe.UPSIDE_DOWN -> ty = sizeY - ty;
            default -> {
            }
        }
        return new Vector3f(tx, ty, tz);
    }

    /**
     * A big ghost's box: its faces in {@code rgb} (the material's map colour), and on the faces turned to the camera a
     * one-block grid round the point nearest it, fading out {@value #GRID_REACH} blocks away.
     */
    private static void box(RenderLevelStageEvent event, BoundingBox box, Vec3 camera, int rgb, int glow) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Matrix4f pose = new Matrix4f();
        double[] min = {box.minX() - 0.02, box.minY() - 0.02, box.minZ() - 0.02};
        double[] max = {box.maxX() + 1.02, box.maxY() + 1.02, box.maxZ() + 1.02};
        double[] eye = {camera.x, camera.y, camera.z};
        VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        int face = EnergyGeometry.argb(rgb, BOX_FACE);
        for (Direction side : Direction.values()) {
            int axis = side.getAxis().ordinal();
            int u = (axis + 1) % 3;
            int v = (axis + 2) % 3;
            double plane = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max[axis] : min[axis];
            double[][] corners = {{min[u], min[v]}, {max[u], min[v]}, {max[u], max[v]}, {min[u], max[v]}};
            for (double[] c : corners) {
                double[] p = new double[3];
                p[axis] = plane;
                p[u] = c[0];
                p[v] = c[1];
                veil.addVertex(pose, (float) (p[0] - eye[0]), (float) (p[1] - eye[1]), (float) (p[2] - eye[2])).setColor(face);
            }
            boolean near = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? eye[axis] > plane : eye[axis] < plane;
            if (near) {
                grid(pose, veil, axis, u, v, plane, min, max, eye, glow);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
    }

    /** The grid lines on one face of a big ghost's box, round the camera's foot on it. */
    private static void grid(Matrix4f pose, VertexConsumer vc, int axis, int u, int v, double plane, double[] min, double[] max, double[] eye,
                             int rgb) {
        double footU = Mth.clamp(eye[u], min[u], max[u]);
        double footV = Mth.clamp(eye[v], min[v], max[v]);
        for (int across = 0; across < 2; across++) {
            int a = across == 0 ? u : v;
            int b = across == 0 ? v : u;
            double footA = across == 0 ? footU : footV;
            double footB = across == 0 ? footV : footU;
            double from = Math.max(min[b], footB - GRID_REACH);
            double to = Math.min(max[b], footB + GRID_REACH);
            for (int line = Mth.ceil(Math.max(min[a], footA - GRID_REACH)); line <= Math.min(max[a], footA + GRID_REACH); line++) {
                float fade = 1.0F - (float) Math.abs(line - footA) / GRID_REACH;
                double[] p = new double[3];
                double[] q = new double[3];
                p[axis] = q[axis] = plane;
                p[a] = q[a] = line;
                p[b] = from;
                q[b] = to;
                BuildRenderer.line(pose, vc, new Vec3(p[0] - eye[0], p[1] - eye[1], p[2] - eye[2]), new Vec3(q[0] - eye[0], q[1] - eye[1],
                        q[2] - eye[2]), 0.02F, EnergyGeometry.argb(rgb, GRID * fade));
            }
        }
    }

    /** The ghost's cells round each mark of the preflight veiled grey: they will be left as they are (D-B 6.2). */
    private static void greyRoundMarks(SpellPlanner.Plan plan, Vec3 camera) {
        BuildVerdictPayload preflight = BuildClient.preflight();
        if (preflight == null || preflight.marks().length == 0) {
            return;
        }
        LongOpenHashSet seen = new LongOpenHashSet();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        Matrix4f pose = new Matrix4f();
        int grey = EnergyGeometry.argb(0x8A8A8A, 0.55F);
        for (long mark : preflight.marks()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        long cell = BlockPos.offset(mark, dx, dy, dz);
                        int x = BlockPos.getX(cell);
                        int y = BlockPos.getY(cell);
                        int z = BlockPos.getZ(cell);
                        if (!seen.add(cell) || !plan.covers(x, y, z)) {
                            continue;
                        }
                        Vec3 lo = new Vec3(x - 0.012 - camera.x, y - 0.012 - camera.y, z - 0.012 - camera.z);
                        for (Direction side : Direction.values()) {
                            BuildRenderer.cellFace(pose, veil, lo, 1.024, side, grey);
                        }
                    }
                }
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }

    private static float red(int rgb) {
        return (rgb >> 16 & 0xFF) / 255.0F;
    }

    private static float green(int rgb) {
        return (rgb >> 8 & 0xFF) / 255.0F;
    }

    private static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The layers and their gatherings

    /**
     * One picture of the ghost: the key of what it holds, the gathering of its cells, the cells waiting for a worker,
     * the mesh being made, and the mesh drawn (the last one stays until the next is ready).
     */
    private static final class Layer {
        private @Nullable Object key;
        private @Nullable Gather gather;
        private @Nullable GhostMesh.Cells waiting;
        private @Nullable Frame waitingFrame;
        private @Nullable GhostMesh making;
        private @Nullable Frame makingFrame;
        private @Nullable GhostMesh shown;
        private @Nullable Frame frame;

        /** Whether {@code next} is new: the caller then starts its gathering (an unfinished one is dropped). */
        boolean wants(Object next) {
            if (next.equals(key)) {
                return false;
            }
            key = next;
            gather = null;
            waiting = null;
            return true;
        }

        /** Walks the gathering on with at most {@code budget} cells, then the worker's side; the budget left. */
        int tick(ClientLevel level, int budget) {
            int left = budget;
            if (gather != null) {
                left -= gather.step(level, budget);
                if (gather.done) {
                    waiting = gather.cells(level);
                    waitingFrame = gather.frame(waiting);
                    gather = null;
                }
            }
            if (making != null && (making.isReady() || making.failed())) {
                if (making.isReady()) {
                    close(shown);
                    shown = making;
                    frame = makingFrame;
                } else {
                    making.close();
                }
                making = null;
            }
            if (making == null && waiting != null) {
                if (waiting.isEmpty()) {
                    // Nothing changes: no ghost (the box and its label say the rest).
                    close(shown);
                    shown = null;
                    frame = null;
                } else {
                    making = GhostMesh.build(waiting, level);
                    makingFrame = waitingFrame;
                }
                waiting = null;
            }
            return Math.max(0, left);
        }

        void clear() {
            key = null;
            gather = null;
            waiting = null;
            close(making);
            making = null;
            close(shown);
            shown = null;
            frame = null;
        }

        private static void close(@Nullable GhostMesh mesh) {
            if (mesh != null) {
                mesh.close();
            }
        }
    }

    /** Gathering a ghost's cells on the client's thread, a slice at a time. */
    private abstract static class Gather {
        protected final int detail;
        protected boolean done;

        Gather(int detail) {
            this.detail = detail;
        }

        /** Gathers on with at most {@code budget} cells' worth of work; how much it used. Sets {@link #done}. */
        abstract int step(ClientLevel level, int budget);

        /** The cells gathered (once done). */
        abstract GhostMesh.Cells cells(ClientLevel level);

        /** How they stand. */
        abstract Frame frame(GhostMesh.Cells cells);

        /**
         * Over {@code detail} blocks, only those with a side open to a cell that is not the ghost's (D-B 6.2): the
         * others draw nothing anyway, every face of theirs is between two cells of the ghost.
         */
        protected static void outsideOnly(GhostMesh.Cells cells, int detail) {
            if (cells.blocks.size() <= detail) {
                return;
            }
            LongOpenHashSet all = new LongOpenHashSet(cells.blocks);
            LongArrayList blocks = new LongArrayList();
            List<BlockState> states = new ArrayList<>();
            for (int i = 0; i < cells.blocks.size(); i++) {
                long cell = cells.blocks.getLong(i);
                for (Direction side : Direction.values()) {
                    if (!all.contains(BlockPos.offset(cell, side))) {
                        blocks.add(cell);
                        states.add(cells.states.get(i));
                        break;
                    }
                }
            }
            cells.blocks.clear();
            cells.blocks.addAll(blocks);
            cells.states.clear();
            cells.states.addAll(states);
        }

        /**
         * The open faces of the cells that go ({@code going}: world positions), as offsets from {@code base}: a face
         * whose neighbour is open space now and does not go too. At most {@code most}.
         */
        protected static void openFaces(ClientLevel level, LongOpenHashSet going, BlockPos base, GhostMesh.Cells cells, long most) {
            BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
            for (long cell : going) {
                for (Direction side : Direction.values()) {
                    long beside = BlockPos.offset(cell, side);
                    next.set(beside);
                    if (going.contains(beside) || !SpellMath.loaded(level, next) || !SpellMath.isOpen(level.getBlockState(next))) {
                        continue;
                    }
                    if (cells.going.size() >= most) {
                        return;
                    }
                    cells.face(BlockPos.asLong(BlockPos.getX(cell) - base.getX(), BlockPos.getY(cell) - base.getY(), BlockPos.getZ(cell) - base.getZ()),
                            side);
                }
            }
        }
    }

    /**
     * A plan's cells, as its cursor walks them. In the world ({@code world}): the cells whose block changes, in loaded
     * chunks inside the world, a block where one is made and dust where one goes; for a shape, every cell it makes.
     * With {@code near}, only the cells within {@value #NEAR} of it, at most {@code detail} of them.
     */
    private static final class PlanWalk extends Gather {
        private final SpellPlanner.Plan plan;
        private final SpellPlanner.Cursor cursor;
        private final Placing placing;
        private final BlockPos base;
        private final boolean world;
        private final @Nullable BlockPos near;
        private final GhostMesh.Cells cells;
        private final LongOpenHashSet going = new LongOpenHashSet();
        /** Whether the plan goes by chunk columns, and the column its spiral starts from. */
        private final boolean columns;
        private final int ringX;
        private final int ringZ;
        private long walked;

        PlanWalk(SpellPlanner.Plan plan, Placing placing, BlockPos base, boolean world, @Nullable BlockPos near, int detail) {
            super(detail);
            this.plan = plan;
            this.cursor = plan.cursor();
            this.placing = placing;
            this.base = base.immutable();
            this.world = world;
            this.near = near == null ? null : near.immutable();
            this.cells = new GhostMesh.Cells(base);
            // A big plan goes column by column in a spiral from the one nearest its caster (here the crosshair): past
            // the ring of columns round the crosshair, no cell is near it any more.
            BoundingBox box = plan.bounds();
            this.columns = box != null && (box.getXSpan() > SpellPlanner.LAYERED_MAX || box.getZSpan() > SpellPlanner.LAYERED_MAX);
            this.ringX = near == null || box == null ? 0 : Mth.clamp(near.getX(), box.minX(), box.maxX()) >> 4;
            this.ringZ = near == null || box == null ? 0 : Mth.clamp(near.getZ(), box.minZ(), box.maxZ()) >> 4;
        }

        @Override
        int step(ClientLevel level, int budget) {
            int used = 0;
            int far = 0;
            long keep = near != null ? detail : 4L * detail;
            while (used + far / FAR_COST < budget) {
                if (!cursor.next()) {
                    done = true;
                    break;
                }
                BlockPos pos = cursor.pos();
                if (near != null) {
                    walked++;
                    int ring = Math.max(Math.abs((pos.getX() >> 4) - ringX), Math.abs((pos.getZ() >> 4) - ringZ));
                    if (walked > NEAR_WALK || columns && ring > (NEAR >> 4) + 1) {
                        done = true;
                        break;
                    }
                    if (Math.abs(pos.getX() - near.getX()) > NEAR || Math.abs(pos.getY() - near.getY()) > NEAR || Math.abs(pos.getZ() - near.getZ()) > NEAR) {
                        far++;
                        continue;
                    }
                }
                used++;
                if (world && (!SpellMath.loaded(level, pos) || SpellMath.outside(level, pos))) {
                    continue;
                }
                BlockState now = cursor.resolve(level);
                if (now == null) {
                    continue;
                }
                if (world) {
                    BlockState old = level.getBlockState(pos);
                    if (now.equals(old)) {
                        continue;
                    }
                    if (!SpellMath.isOpen(old) && SpellMath.isOpen(now)) {
                        going.add(pos.asLong());
                        continue;
                    }
                }
                if (now.isAir()) {
                    continue;
                }
                if (cells.blocks.size() >= keep) {
                    done = true;
                    break;
                }
                cells.block(pos.getX() - base.getX(), pos.getY() - base.getY(), pos.getZ() - base.getZ(), now);
            }
            return used + far / FAR_COST;
        }

        @Override
        GhostMesh.Cells cells(ClientLevel level) {
            if (near == null) {
                outsideOnly(cells, detail);
            }
            openFaces(level, going, base, cells, Math.max(detail, 1));
            return cells;
        }

        @Override
        Frame frame(GhostMesh.Cells gathered) {
            BoundingBox local = gathered.bounds();
            return new Frame(placing, base, 0, 0, 0, local != null ? local : new BoundingBox(0, 0, 0, 0, 0, 0));
        }
    }

    /**
     * The cells of a copy (a pocket, or a Move's box read live) in its own frame, as it is before any turn: all of
     * them, or with {@code near} (a cell of it) only those within {@value #NEAR} of it, at most {@code detail}.
     */
    private static final class SourceWalk extends Gather {
        private final SpellPlanner.Source source;
        private final int x0;
        private final int y0;
        private final int z0;
        private final int x1;
        private final int y1;
        private final int z1;
        private final boolean near;
        private final GhostMesh.Cells cells = new GhostMesh.Cells(BlockPos.ZERO);
        private int x;
        private int y;
        private int z;

        SourceWalk(SpellPlanner.Source source, @Nullable BlockPos near, int detail) {
            super(detail);
            this.source = source;
            this.near = near != null;
            this.x0 = near == null ? 0 : Math.max(0, near.getX() - NEAR);
            this.y0 = near == null ? 0 : Math.max(0, near.getY() - NEAR);
            this.z0 = near == null ? 0 : Math.max(0, near.getZ() - NEAR);
            this.x1 = near == null ? source.sizeX() - 1 : Math.min(source.sizeX() - 1, near.getX() + NEAR);
            this.y1 = near == null ? source.sizeY() - 1 : Math.min(source.sizeY() - 1, near.getY() + NEAR);
            this.z1 = near == null ? source.sizeZ() - 1 : Math.min(source.sizeZ() - 1, near.getZ() + NEAR);
            this.x = x0;
            this.y = y0;
            this.z = z0;
        }

        @Override
        int step(ClientLevel level, int budget) {
            int used = 0;
            long keep = near ? detail : 4L * detail;
            while (used < budget) {
                if (y > y1 || cells.blocks.size() >= keep) {
                    done = true;
                    break;
                }
                BlockState state = source.state(x, y, z);
                if (state != null && !state.isAir()) {
                    cells.block(x, y, z, state);
                }
                used++;
                if (++x > x1) {
                    x = x0;
                    if (++z > z1) {
                        z = z0;
                        y++;
                    }
                }
            }
            return used;
        }

        @Override
        GhostMesh.Cells cells(ClientLevel level) {
            if (!near) {
                outsideOnly(cells, detail);
            }
            return cells;
        }

        @Override
        Frame frame(GhostMesh.Cells gathered) {
            BoundingBox local = gathered.bounds();
            return new Frame(Placing.CARRIED, BlockPos.ZERO, source.sizeX(), source.sizeY(), source.sizeZ(),
                    local != null ? local : new BoundingBox(0, 0, 0, 0, 0, 0));
        }
    }

    /** A Move's source, read live: dust on the open faces of every block it will empty, where they are. */
    private static final class SourceDust extends Gather {
        private final SpellPlanner.Source source;
        private final BlockPos base;
        private final LongOpenHashSet going = new LongOpenHashSet();
        private int x;
        private int y;
        private int z;

        SourceDust(SpellPlanner.Source source, BlockPos base, int detail) {
            super(detail);
            this.source = source;
            this.base = base.immutable();
        }

        @Override
        int step(ClientLevel level, int budget) {
            int used = 0;
            while (used < budget) {
                if (y >= source.sizeY() || going.size() >= 4L * Math.max(detail, 1)) {
                    done = true;
                    break;
                }
                BlockState state = source.state(x, y, z);
                if (state != null && !SpellMath.isOpen(state)) {
                    going.add(BlockPos.asLong(base.getX() + x, base.getY() + y, base.getZ() + z));
                }
                used++;
                if (++x >= source.sizeX()) {
                    x = 0;
                    if (++z >= source.sizeZ()) {
                        z = 0;
                        y++;
                    }
                }
            }
            return used;
        }

        @Override
        GhostMesh.Cells cells(ClientLevel level) {
            GhostMesh.Cells cells = new GhostMesh.Cells(base);
            openFaces(level, going, base, cells, Math.max(detail, 1));
            return cells;
        }

        @Override
        Frame frame(GhostMesh.Cells gathered) {
            BoundingBox local = gathered.bounds();
            return new Frame(Placing.WORLD, base, 0, 0, 0, local != null ? local : new BoundingBox(0, 0, 0, 0, 0, 0));
        }
    }

    private Ghost() {
    }
}
