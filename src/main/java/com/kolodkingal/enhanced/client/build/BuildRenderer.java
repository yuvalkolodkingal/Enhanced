package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildGrammar;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.MagicSelect;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Build Magic in the world, as its builder sees it (D-B 3, 6.1, 6.2) and as everyone near sees another builder's box
 * (D-B 7.1). The ghost ({@link Ghost}) is drawn after the world's translucent blocks, so it blends over water; the rest
 * after the particles like {@code WorldEffectsRenderer} (identity pose, positions relative to the camera):
 *
 * <ul>
 *     <li><b>The aimed cell</b>: a thin outline in the hero's colour on the block under the crosshair (vanilla's own
 *     outline is hidden in build mode: it marks the block a hand would reach, not the one the spell takes); on the sky,
 *     a faint outline on the floating cell 6 blocks ahead when a click there would mark a corner or place a shape or a
 *     ghost.</li>
 *     <li><b>The box</b> (the fixed box or the rubber band): 12 glowing edges, 0.06 wide, from the hero's core colour
 *     to its highlight, over a line of the core colour itself (an additive glow alone turns scarlet orange on grass),
 *     slowly pulsing; Billy's carry a short jagged arc on a random edge every half second, Wanda's a
 *     wave of brightness; faces at 8%; the face a scroll would push at 22% with a white arrow pointing where scroll-up
 *     moves it; a rune ring (Billy) or a turning hexagram (Wanda) at corner A and at corner B, which follows the
 *     crosshair while the box rubber-bands. With {@code seeThrough} the edges are drawn again at 25% over everything,
 *     so a box inside a hill is never lost.</li>
 *     <li><b>A selected thing</b>: the glow on its open faces as the flood finds it ({@link SelectGlow}) and its
 *     bounding box dotted.</li>
 *     <li><b>A brush's ghost</b>: Land's disc on the ground and Grow's sphere round the aimed block, as wide as the
 *     brush (the others' ghosts are blocks).</li>
 *     <li><b>The label</b> over the top of the box (or of a shape's, a Pull's or a ghost's cells):
 *     {@code 7 × 4 × 5 · 140} (Billy's number is what it costs him, {@code 140 ✦}), a shape's radius, a Repeat's
 *     copies, a Pull's layers; {@code counting... 380,000} while the client still counts, {@code about} when some
 *     cells are in chunks this client has not loaded; the server's preflight replaces the client's count when it
 *     comes. It is drawn through walls like a name tag, grows with the distance so it stays readable, flashes while
 *     Billy's side holds the rubber band back, and turns orange (not yet) or red (never) when his number is more than
 *     his meter has.</li>
 *     <li><b>The marks</b> of the server's preflight, and of a cast's verdict for {@value #MARK_TICKS} ticks: on each
 *     cell it reported, a flashing red ✗ where it is protected (an arena, spawn, a claim, a Hex, beyond the world,
 *     beyond magic), a small yellow cube for a chest that stays because it holds things, white where someone stands,
 *     grey where the chunk is not loaded.</li>
 *     <li><b>Other builders' boxes</b> ({@code showOthers}): thin edges in each one's colour, no faces, no label, for
 *     every builder within {@value #OTHERS_RANGE} blocks, from the {@link BuildView} the server syncs.</li>
 * </ul>
 *
 * <p>Thin things stay about two pixels wide however far they are (a Wanda-sized box is seen from its far end).
 * Reduced motion ({@link EnhancedClientConfig#reducedMotion()}) keeps every glow steady: no pulse, no wave, no arc, no
 * turning, no flashing. Only ever loaded on a client.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class BuildRenderer {
    /** How far another builder's box is seen (D-B 14.7). */
    public static final int OTHERS_RANGE = 64;
    /** How long the marks of a cast's verdict stay; a preflight's stay while its plan is the one shown. */
    public static final int MARK_TICKS = 100;

    /** The box's edges, and the thin outline of the aimed cell (D-B 6.1, 3.1). */
    private static final float EDGE = 0.06F;
    private static final float THIN = 0.022F;
    /** Another builder's edges: thinner than one's own. */
    private static final float OTHERS_EDGE = 0.035F;
    /** How wide a thin thing gets per block of distance: about two pixels on a big screen. */
    private static final float PIXEL = 0.0025F;
    /** The longest piece of an edge drawn at one width, and the most pieces an edge is cut into. */
    private static final double PIECE = 8.0;
    private static final int MAX_PIECES = 32;
    /** A dash and its gap on a selected thing's dotted box, and the most dashes an edge has. */
    private static final double DASH = 0.5;
    private static final int MAX_DASHES = 128;
    /** How far outside its blocks the box is drawn, so its faces never fight the blocks' own. */
    private static final double INFLATE = 0.01;

    /** How bright the faces are, the looked face, and the box through hills (D-B 6.1). */
    private static final float FACE = 0.08F;
    private static final float LOOKED_FACE = 0.22F;
    private static final float XRAY = 0.25F;
    /** The marks through hills: brighter than the box, they are the server's word. */
    private static final float XRAY_MARKS = 0.45F;
    /** How bright a selected thing's glow is. */
    private static final float GLOW = 0.4F;

    /** The marks' colours (D-B 6.2). */
    private static final int MARK_RED = 0xFF2E2E;
    private static final int MARK_YELLOW = 0xFFD23A;
    private static final int MARK_WHITE = 0xFFFFFF;
    private static final int MARK_GREY = 0x9A9A9A;
    /** The label's warnings: Billy's "not yet" and "never", and a rubber band held back. */
    private static final int ORANGE = 0xFFAA33;
    private static final int RED = 0xFF5050;

    /** The 12 edges of a box, as pairs of its 8 corners (bit 0: x, bit 1: y, bit 2: z). */
    private static final int[][] EDGES = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6},
            {3, 7}};
    /** The corners of each face of a box, in the order of {@link Direction}. */
    private static final int[][] FACES = {{0, 1, 5, 4}, {2, 6, 7, 3}, {0, 2, 3, 1}, {4, 5, 7, 6}, {0, 4, 6, 2}, {1, 3, 7, 5}};
    private static final Vec3 UP = new Vec3(0, 1, 0);
    /** Where the arrow's light comes from: high above, a little to the south-east. */
    private static final Vec3 LIGHT = new Vec3(0.3, 1.0, 0.45).normalize();

    /** A hero's two colours: the core and the highlight ({@link Power#color}, {@link Power#glow}). */
    private record Look(int core, int glow, boolean billy) {
        static Look of(Power power) {
            return new Look(power.color, power.glow, power == Power.BILLY);
        }
    }

    /** A box in camera space, already drawn a little outside its blocks. */
    record Frame(Vec3 min, Vec3 max) {
        static Frame of(BoundingBox box, Vec3 camera, double inflate) {
            return new Frame(new Vec3(box.minX() - inflate - camera.x, box.minY() - inflate - camera.y, box.minZ() - inflate - camera.z),
                    new Vec3(box.maxX() + 1 + inflate - camera.x, box.maxY() + 1 + inflate - camera.y, box.maxZ() + 1 + inflate - camera.z));
        }

        Vec3 corner(int i) {
            return new Vec3((i & 1) == 0 ? min.x : max.x, (i & 2) == 0 ? min.y : max.y, (i & 4) == 0 ? min.z : max.z);
        }

        Vec3 centre(Direction face) {
            Vec3 sum = Vec3.ZERO;
            for (int i : FACES[face.ordinal()]) {
                sum = sum.add(corner(i));
            }
            return sum.scale(0.25);
        }
    }

    /** A mark of the server's: a cell and its kind ({@link BuildVerdictPayload#MARK_PROTECTED}...). */
    private record Mark(BlockPos cell, byte kind) {
    }

    /** The label: where its text stands (its top), what it says, and its colour. */
    private record Label(Vec3 at, Component text, int colour) {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        boolean ghost = event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
        if (!ghost && event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        float time = level.getGameTime() + partialTick;
        boolean calm = EnhancedClientConfig.reducedMotion();
        if (ghost) {
            // The ghost, after the world's translucent blocks: it blends over water (D-B 11.10).
            if (BuildClient.buildMode()) {
                Look look = Look.of(Powers.power(player));
                Ghost.draw(event, player, look.core(), look.glow(), time, calm);
            }
            return;
        }
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        if (EnhancedClientConfig.buildShowOthers()) {
            VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            others(level, player, pose, beams, camera);
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
        // The spells at work near here, theirs and everyone's, and the builders' own sparks (D-B 7.1, 7.2).
        BuildFx.render(mc, level, pose, buffers, camera, partialTick, time, calm);
        if (!BuildClient.buildMode()) {
            SelectGlow.clear();
            return;
        }
        Look look = Look.of(Powers.power(player));
        BuildGrammar g = BuildClient.grammar();
        MagicSelect flood = g.hasMask() ? BuildClient.flood() : null;
        BoundingBox box = g.box();
        BoundingBox mask = !g.hasMask() ? null : g.maskBounds() != null ? g.maskBounds() : flood != null ? flood.bounds() : null;
        Direction looked = lookedFace(g, box, mask, player.getEyePosition(partialTick), player.getViewVector(partialTick));
        List<Mark> marks = marks();
        float pulse = calm ? 1.0F : 0.82F + 0.18F * Mth.sin(time * 0.15F);

        // The glow of a selected thing, from its own buffer.
        SelectGlow.draw(event, level, flood, look.core(), GLOW * (calm ? 1.0F : 0.85F + 0.15F * Mth.sin(time * 0.2F)));

        // Glowing lines: the aimed cell, the box (or the thing's dotted box), its corners and arrow.
        VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        aimedCell(g, look, pose, beams, camera);
        brush(g, look, pose, beams, camera);
        if (box != null) {
            Frame frame = Frame.of(box, camera, INFLATE);
            edges(pose, beams, frame, EDGE * 1.6F, EnergyGeometry.argb(look.core(), 0.35F * pulse));
            edges(pose, beams, frame, EDGE * 0.4F, EnergyGeometry.argb(look.glow(), 0.8F * pulse));
            if (!calm) {
                if (look.billy()) {
                    arc(pose, beams, frame, time);
                } else {
                    waves(pose, beams, frame, look.glow(), time);
                }
            }
            corners(g, box, look, pose, beams, camera, time, calm, 1.0F);
        }
        if (mask != null) {
            dottedEdges(pose, beams, Frame.of(mask, camera, INFLATE), EDGE * 0.6F, EnergyGeometry.argb(look.glow(), 0.8F * pulse));
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);

        // See-through tints, true to their colours: the box's faces and edges, the looked face and its arrow, the marks.
        BoundingBox pushed = box != null ? box : mask;
        VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        if (box != null) {
            Frame frame = Frame.of(box, camera, INFLATE);
            faces(pose, veil, frame, looked, look, true);
            // Under the glow, the edges in their own colour: an additive glow alone turns scarlet orange on grass.
            edges(pose, veil, frame, EDGE, EnergyGeometry.argb(look.core(), 0.65F * pulse));
        } else if (mask != null && looked != null) {
            faces(pose, veil, Frame.of(mask, camera, INFLATE), looked, look, false);
        }
        if (looked != null && pushed != null) {
            arrow(pose, veil, Frame.of(pushed, camera, INFLATE).centre(looked), looked, 0.95F);
        }
        for (Mark mark : marks) {
            mark(pose, veil, mark, camera, time, calm, 1.0F);
            markCube(pose, veil, mark, camera);
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);

        // The same lines once more, faintly, over everything: a box inside a hill is never lost.
        if (EnhancedClientConfig.buildSeeThrough() && (box != null || mask != null || !marks.isEmpty())) {
            VertexConsumer xray = buffers.getBuffer(EnhancedRenderTypes.BUILD_XRAY);
            if (box != null) {
                edges(pose, xray, Frame.of(box, camera, INFLATE), EDGE, EnergyGeometry.argb(look.core(), XRAY));
                corners(g, box, look, pose, xray, camera, time, calm, XRAY);
            }
            if (mask != null) {
                dottedEdges(pose, xray, Frame.of(mask, camera, INFLATE), EDGE * 0.6F, EnergyGeometry.argb(look.glow(), XRAY));
            }
            if (looked != null && pushed != null) {
                arrow(pose, xray, Frame.of(pushed, camera, INFLATE).centre(looked), looked, XRAY);
            }
            for (Mark mark : marks) {
                mark(pose, xray, mark, camera, time, calm, XRAY_MARKS);
            }
            buffers.endBatch(EnhancedRenderTypes.BUILD_XRAY);
        }

        Label label = label(g, player, level, box, mask, flood, time, calm);
        if (label != null) {
            drawLabel(mc, event.getCamera(), buffers, label, camera, looked == Direction.UP && pushed != null);
        }
    }

    /** Vanilla's block outline is hidden in build mode: the spell takes the aimed cell, which has its own outline. */
    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        if (BuildClient.buildMode()) {
            event.setCanceled(true);
        }
    }

    /** Lets go of everything kept between frames (at logout). */
    static void clear() {
        SelectGlow.clear();
        Ghost.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What is drawn

    /**
     * The face of the box (or of the selected thing's box) a scroll would push now, as {@link BuildGrammar#scroll}
     * decides it: a fixed box or a found thing, a spell whose scroll pushes a face, the second knob not held.
     */
    private static @Nullable Direction lookedFace(BuildGrammar g, @Nullable BoundingBox box, @Nullable BoundingBox mask, Vec3 eyes, Vec3 look) {
        boolean pushes = switch (g.spell()) {
            case FILL, VANISH, CHANGE -> true;
            case COPY, MOVE -> !g.ghost();
            default -> false;
        };
        BoundingBox target = g.rubberBand() ? null : box != null ? box : g.maskBounds();
        if (!pushes || target == null || BuildKeys.MORE.isDown() || mask != null && g.maskBounds() == null) {
            return null;
        }
        return g.pushFace(target, eyes, look, BuildClient.now());
    }

    /**
     * The thin outline on the aimed cell; on the sky, the faint one on the floating cell when a click there would use
     * it: a corner of a box not yet made, a shape that follows the crosshair, a ghost to place (D-B 3.1).
     */
    private static void aimedCell(BuildGrammar g, Look look, Matrix4f pose, VertexConsumer vc, Vec3 camera) {
        BuildGrammar.Aim aim = BuildClient.aim();
        if (aim == null) {
            return;
        }
        if (aim.cell() != null) {
            edges(pose, vc, Frame.of(new BoundingBox(aim.cell()), camera, 0.004), THIN, EnergyGeometry.argb(look.core(), 0.85F));
            return;
        }
        boolean floats = switch (g.spell()) {
            case FILL, VANISH, CHANGE, COPY, MOVE, REPEAT -> g.ghost() || !g.finished();
            case SHAPE -> !g.planted();
            default -> false;
        };
        if (floats) {
            edges(pose, vc, Frame.of(new BoundingBox(aim.floating()), camera, 0.0), THIN, EnergyGeometry.argb(look.glow(), 0.4F));
        }
    }

    /**
     * The ghost of a brush that plans its pulses itself (D-B 4.1): Land's disc on the ground, Grow's sphere, round the
     * aimed block, as wide as the brush.
     */
    private static void brush(BuildGrammar g, Look look, Matrix4f pose, VertexConsumer vc, Vec3 camera) {
        BuildGrammar.Aim aim = BuildClient.aim();
        if (aim == null || g.spell() != Spell.LAND && g.spell() != Spell.GROW) {
            return;
        }
        BlockPos at = aim.cell() != null ? aim.cell() : aim.picked();
        if (at == null) {
            return;
        }
        float radius = g.brush() + 0.5F;
        Vec3 centre = Vec3.atCenterOf(at).subtract(camera);
        int argb = EnergyGeometry.argb(look.core(), 0.9F);
        float width = wide(centre, 0.05F);
        if (g.spell() == Spell.LAND) {
            EnergyGeometry.ring(pose, vc, centre.add(0, 0.52, 0), UP, radius, width, 48, argb);
            EnergyGeometry.ring(pose, vc, centre.add(0, 0.52, 0), UP, 0.25F, width, 12, argb);
        } else {
            EnergyGeometry.ring(pose, vc, centre, UP, radius, width, 48, argb);
            EnergyGeometry.ring(pose, vc, centre, new Vec3(1, 0, 0), radius, width, 48, argb);
            EnergyGeometry.ring(pose, vc, centre, new Vec3(0, 0, 1), radius, width, 48, argb);
        }
    }

    /** The 12 edges of a box, each a camera-facing ribbon about {@code width} wide (wider far away). */
    static void edges(Matrix4f pose, VertexConsumer vc, Frame frame, float width, int argb) {
        for (int[] edge : EDGES) {
            line(pose, vc, frame.corner(edge[0]), frame.corner(edge[1]), width, argb);
        }
    }

    /** The 12 edges of a selected thing's box, dotted. */
    private static void dottedEdges(Matrix4f pose, VertexConsumer vc, Frame frame, float width, int argb) {
        for (int[] edge : EDGES) {
            Vec3 a = frame.corner(edge[0]);
            Vec3 b = frame.corner(edge[1]);
            int dashes = Mth.clamp(Mth.ceil(a.distanceTo(b) / DASH), 1, MAX_DASHES);
            for (int i = 0; i < dashes; i++) {
                Vec3 from = a.lerp(b, i / (double) dashes);
                Vec3 to = a.lerp(b, (i + 0.5) / dashes);
                EnergyGeometry.ribbon(pose, vc, from, to, wide(from, width), wide(to, width), argb, argb);
            }
        }
    }

    /** A line from {@code a} to {@code b}, in pieces so that each end of each piece is wide enough for its distance. */
    static void line(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, int argb) {
        int pieces = Mth.clamp(Mth.ceil(a.distanceTo(b) / PIECE), 1, MAX_PIECES);
        Vec3 from = a;
        float fromWidth = wide(a, width);
        for (int i = 1; i <= pieces; i++) {
            Vec3 to = i == pieces ? b : a.lerp(b, i / (double) pieces);
            float toWidth = wide(to, width);
            EnergyGeometry.ribbon(pose, vc, from, to, fromWidth, toWidth, argb, argb);
            from = to;
            fromWidth = toWidth;
        }
    }

    /** {@code width}, or about two pixels where that is more: a thin line {@code p} away from the camera. */
    private static float wide(Vec3 p, float width) {
        return Math.max(width, (float) p.length() * PIXEL);
    }

    /** How much bigger a mark is drawn {@code p} away from the camera, so it keeps about its size on screen. */
    private static float grow(Vec3 p, double from) {
        return (float) Math.max(1.0, p.length() / from);
    }

    /** Wanda's edges: a wave of brightness runs along each, one after another. */
    private static void waves(Matrix4f pose, VertexConsumer vc, Frame frame, int rgb, float time) {
        for (int i = 0; i < EDGES.length; i++) {
            Vec3 a = frame.corner(EDGES[i][0]);
            Vec3 b = frame.corner(EDGES[i][1]);
            double length = a.distanceTo(b);
            if (length < 1.0E-3) {
                continue;
            }
            double at = Mth.frac(time * 0.02F + i * 0.29F);
            double half = Math.min(1.2, length * 0.25) / length;
            Vec3 from = a.lerp(b, Math.max(0.0, at - half));
            Vec3 middle = a.lerp(b, at);
            Vec3 to = a.lerp(b, Math.min(1.0, at + half));
            float bright = 0.7F * Mth.sin((float) at * Mth.PI);
            int on = EnergyGeometry.argb(rgb, bright);
            int off = EnergyGeometry.argb(rgb, 0.0F);
            EnergyGeometry.ribbon(pose, vc, from, middle, wide(from, EDGE * 1.6F), wide(middle, EDGE * 1.6F), off, on);
            EnergyGeometry.ribbon(pose, vc, middle, to, wide(middle, EDGE * 1.6F), wide(to, EDGE * 1.6F), on, off);
        }
    }

    /** Billy's edges: every half second a short jagged arc crackles somewhere along one of them. */
    private static void arc(Matrix4f pose, VertexConsumer vc, Frame frame, float time) {
        long slot = Mth.floor(time / 10.0F);
        RandomSource random = RandomSource.create(slot * 341_873_128_712L + 132_897_987_541L);
        int[] edge = EDGES[random.nextInt(EDGES.length)];
        Vec3 a = frame.corner(edge[0]);
        Vec3 b = frame.corner(edge[1]);
        double length = a.distanceTo(b);
        if (length < 0.2) {
            return;
        }
        double piece = Math.min(length, 0.8 + random.nextDouble() * 1.6);
        double start = random.nextDouble() * (length - piece);
        Vec3 from = a.lerp(b, start / length);
        Vec3 to = a.lerp(b, (start + piece) / length);
        float fade = Mth.sin(Mth.frac(time / 10.0F) * Mth.PI);
        float scale = grow(from, 16.0);
        long flicker = Mth.floor(time / 2.0F);
        EnergyGeometry.bolt(pose, vc, from, to, 0.05F * scale, 0.14F * scale, EnergyGeometry.argb(BillyAbilities.MAIN, 0.6F * fade), flicker);
        EnergyGeometry.bolt(pose, vc, from, to, 0.022F * scale, 0.14F * scale, EnergyGeometry.argb(BillyAbilities.CORE, 0.9F * fade), flicker);
    }

    /**
     * The marks at the corners: at A, and at B (the aimed corner while the box rubber-bands), each on the corner of
     * the box that lies in that cell (on a side one block thick, the one nearer the camera).
     */
    private static void corners(BuildGrammar g, BoundingBox box, Look look, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time,
                                boolean calm, float alpha) {
        BlockPos a = g.cornerA();
        if (a == null) {
            return;
        }
        BlockPos b = new BlockPos(opposite(a.getX(), box.minX(), box.maxX()), opposite(a.getY(), box.minY(), box.maxY()),
                opposite(a.getZ(), box.minZ(), box.maxZ()));
        Frame frame = Frame.of(box, camera, INFLATE);
        cornerMark(pose, vc, vertex(frame, box, a), look, time, calm, alpha);
        if (!b.equals(a)) {
            cornerMark(pose, vc, vertex(frame, box, b), look, time, calm, alpha);
        }
    }

    private static int opposite(int at, int min, int max) {
        return at == min ? max : min;
    }

    /**
     * The corner of the box in {@code cell}: on each axis its low side or its high side, the nearer to the camera when
     * the box is one block thick there.
     */
    private static Vec3 vertex(Frame frame, BoundingBox box, BlockPos cell) {
        return new Vec3(side(cell.getX(), box.minX(), box.maxX(), frame.min().x, frame.max().x),
                side(cell.getY(), box.minY(), box.maxY(), frame.min().y, frame.max().y),
                side(cell.getZ(), box.minZ(), box.maxZ(), frame.min().z, frame.max().z));
    }

    private static double side(int at, int min, int max, double low, double high) {
        if (min == max) {
            // One block thick: the side nearer the camera (camera space: the camera is at 0).
            return Math.abs(low) < Math.abs(high) ? low : high;
        }
        return at == min ? low : high;
    }

    /** Billy's rune ring (runes round a ring, turning slowly) or Wanda's turning hexagram, facing the camera. */
    private static void cornerMark(Matrix4f pose, VertexConsumer vc, Vec3 at, Look look, float time, boolean calm, float alpha) {
        double distance = at.length();
        Vec3 normal = distance < 1.0E-4 ? new Vec3(0, 0, 1) : at.scale(-1.0 / distance);
        Vec3 u = normal.cross(UP);
        if (u.lengthSqr() < 1.0E-6) {
            u = normal.cross(new Vec3(1, 0, 0));
        }
        u = u.normalize();
        Vec3 v = normal.cross(u).normalize();
        float scale = grow(at, 16.0);
        float radius = 0.22F * scale;
        float spin = calm ? 0.0F : time * 0.05F;
        int core = EnergyGeometry.argb(look.core(), 0.9F * alpha);
        int glow = EnergyGeometry.argb(look.glow(), 0.9F * alpha);
        EnergyGeometry.ring(pose, vc, at, normal, radius, 0.05F * scale, 24, core);
        if (look.billy()) {
            EnergyGeometry.ring(pose, vc, at, normal, radius * 0.55F, 0.025F * scale, 16, glow);
            for (int k = 0; k < 8; k++) {
                double angle = spin + k * Math.PI / 4.0;
                Vec3 out = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
                double reach = k % 2 == 0 ? 1.5 : 1.32;
                EnergyGeometry.ribbon(pose, vc, at.add(out.scale(radius * 1.15)), at.add(out.scale(radius * reach)), 0.045F * scale, glow, glow);
            }
        } else {
            Vec3[] star = new Vec3[6];
            for (int k = 0; k < 6; k++) {
                double angle = spin + k * Math.PI / 3.0;
                star[k] = at.add(u.scale(Math.cos(angle) * radius * 0.92)).add(v.scale(Math.sin(angle) * radius * 0.92));
            }
            for (int k = 0; k < 6; k++) {
                // Two triangles: the even points, and the odd ones.
                EnergyGeometry.ribbon(pose, vc, star[k], star[(k + 2) % 6], 0.04F * scale, glow, glow);
            }
        }
    }

    /**
     * The white arrow on the face a scroll pushes, pointing where scroll-up moves it: a solid shaft and a pointed head,
     * shaded as if lit from above, so its depth reads even when it points at the camera. Seen end on (looking straight
     * at the face) it gives way to a ring with a dot on the face, the sign of an arrow coming out of it.
     */
    private static void arrow(Matrix4f pose, VertexConsumer vc, Vec3 centre, Direction face, float alpha) {
        Vec3 out = Vec3.atLowerCornerOf(face.getNormal());
        float scale = grow(centre, 16.0);
        Vec3 base = centre.add(out.scale(0.04 * scale));
        Vec3 neck = centre.add(out.scale(0.8 * scale));
        Vec3 tip = centre.add(out.scale(1.35 * scale));
        double along = neck.lengthSqr() < 1.0E-6 ? 1.0 : Math.abs(neck.normalize().dot(out));
        float endOn = (float) Mth.clamp((along - 0.85) / 0.1, 0.0, 1.0);
        // Two directions across the arrow, and the four corners of its square cross-section.
        Vec3 across = face.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : UP;
        Vec3 other = out.cross(across);
        Vec3[] square = {across.add(other), across.scale(-1).add(other), across.add(other).scale(-1), across.subtract(other)};
        float body = alpha * (1.0F - endOn);
        if (body > 0.0F) {
            for (int k = 0; k < 4; k++) {
                Vec3 one = square[k];
                Vec3 two = square[(k + 1) % 4];
                int shade = shaded(one.add(two), body);
                quad(pose, vc, base.add(one.scale(0.07 * scale)), base.add(two.scale(0.07 * scale)), neck.add(two.scale(0.07 * scale)),
                        neck.add(one.scale(0.07 * scale)), shade);
                Vec3 slope = one.add(two).normalize().add(out.scale(0.6));
                quad(pose, vc, neck.add(one.scale(0.27 * scale)), neck.add(two.scale(0.27 * scale)), tip, tip, shaded(slope, body));
            }
            quad(pose, vc, neck.add(square[0].scale(0.27 * scale)), neck.add(square[1].scale(0.27 * scale)),
                    neck.add(square[2].scale(0.27 * scale)), neck.add(square[3].scale(0.27 * scale)), shaded(out.scale(-1), body));
        }
        if (endOn > 0.0F) {
            int sign = EnergyGeometry.argb(0xFFFFFF, alpha * endOn);
            EnergyGeometry.ring(pose, vc, base, out, 0.3F * scale, 0.07F * scale, 24, sign);
            EnergyGeometry.ring(pose, vc, base, out, 0.04F * scale, 0.09F * scale, 8, sign);
        }
    }

    /** White, darker where {@code normal} turns away from a light high above (the arrow's shading). */
    private static int shaded(Vec3 normal, float alpha) {
        double lit = normal.lengthSqr() < 1.0E-8 ? 0.5 : Math.max(0.0, normal.normalize().dot(LIGHT));
        int grey = (int) (255 * (0.55 + 0.45 * lit));
        return EnergyGeometry.argb(grey << 16 | grey << 8 | grey, alpha);
    }

    private static void quad(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
        for (Vec3 p : new Vec3[] {a, b, c, d}) {
            vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
        }
    }

    /** One face of the cube {@code size} wide whose low corner is {@code min} (camera space). */
    static void cellFace(Matrix4f pose, VertexConsumer vc, Vec3 min, double size, Direction side, int argb) {
        for (int i : FACES[side.ordinal()]) {
            vc.addVertex(pose, (float) (min.x + ((i & 1) == 0 ? 0 : size)), (float) (min.y + ((i & 2) == 0 ? 0 : size)),
                    (float) (min.z + ((i & 4) == 0 ? 0 : size))).setColor(argb);
        }
    }

    /**
     * The box's faces at 8%, the looked face at 22% in the highlight colour; only the looked one when {@code all} is
     * false.
     */
    private static void faces(Matrix4f pose, VertexConsumer vc, Frame frame, @Nullable Direction looked, Look look, boolean all) {
        for (Direction face : Direction.values()) {
            boolean lit = face == looked;
            if (!all && !lit) {
                continue;
            }
            int argb = lit ? EnergyGeometry.argb(look.glow(), LOOKED_FACE) : EnergyGeometry.argb(look.core(), FACE);
            for (int i : FACES[face.ordinal()]) {
                Vec3 p = frame.corner(i);
                vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The marks

    /** The marks to draw: the current preflight's, and a cast's for {@value #MARK_TICKS} ticks after its verdict. */
    private static List<Mark> marks() {
        List<Mark> marks = new ArrayList<>();
        BuildVerdictPayload preflight = BuildClient.preflight();
        if (preflight != null) {
            addMarks(marks, preflight);
        }
        Optional<BuildVerdictPayload> last = BuildClient.lastVerdict();
        if (last.isPresent() && BuildClient.ticksSinceVerdict() <= MARK_TICKS) {
            addMarks(marks, last.get());
        }
        return marks;
    }

    private static void addMarks(List<Mark> marks, BuildVerdictPayload verdict) {
        long[] cells = verdict.marks();
        byte[] kinds = verdict.kinds();
        for (int i = 0; i < cells.length; i++) {
            marks.add(new Mark(BlockPos.of(cells[i]), i < kinds.length ? kinds[i] : BuildVerdictPayload.MARK_PROTECTED));
        }
    }

    /** Whether a mark says the cell is refused (a red ✗): protected, beyond the world, beyond magic. */
    private static boolean refused(byte kind) {
        return kind != BuildVerdictPayload.MARK_KEEPS && kind != BuildVerdictPayload.MARK_BODY && kind != BuildVerdictPayload.MARK_UNLOADED;
    }

    private static int markColour(byte kind) {
        return switch (kind) {
            case BuildVerdictPayload.MARK_KEEPS -> MARK_YELLOW;
            case BuildVerdictPayload.MARK_BODY -> MARK_WHITE;
            case BuildVerdictPayload.MARK_UNLOADED -> MARK_GREY;
            default -> MARK_RED;
        };
    }

    /** A mark's lines: the outline of its cell and, where the cell is refused, a flashing ✗ facing the camera. */
    private static void mark(Matrix4f pose, VertexConsumer vc, Mark mark, Vec3 camera, float time, boolean calm, float alpha) {
        int rgb = markColour(mark.kind());
        edges(pose, vc, Frame.of(new BoundingBox(mark.cell()), camera, 0.02), 0.03F, EnergyGeometry.argb(rgb, 0.8F * alpha));
        if (!refused(mark.kind())) {
            return;
        }
        Vec3 centre = Vec3.atCenterOf(mark.cell()).subtract(camera);
        double distance = centre.length();
        Vec3 normal = distance < 1.0E-4 ? new Vec3(0, 0, 1) : centre.scale(-1.0 / distance);
        Vec3 u = normal.cross(UP);
        if (u.lengthSqr() < 1.0E-6) {
            u = normal.cross(new Vec3(1, 0, 0));
        }
        u = u.normalize();
        Vec3 v = normal.cross(u).normalize();
        float scale = grow(centre, 16.0);
        Vec3 one = u.add(v).scale(0.42 * scale);
        Vec3 two = u.subtract(v).scale(0.42 * scale);
        float flash = calm ? 1.0F : 0.6F + 0.4F * Mth.sin(time * 0.6F);
        int red = EnergyGeometry.argb(rgb, flash * alpha);
        EnergyGeometry.ribbon(pose, vc, centre.subtract(one), centre.add(one), 0.15F * scale, red, red);
        EnergyGeometry.ribbon(pose, vc, centre.subtract(two), centre.add(two), 0.15F * scale, red, red);
    }

    /** A mark's small cube in its colour, for the cells that are kept, held by someone or not loaded. */
    private static void markCube(Matrix4f pose, VertexConsumer vc, Mark mark, Vec3 camera) {
        if (refused(mark.kind())) {
            return;
        }
        Vec3 centre = Vec3.atCenterOf(mark.cell()).subtract(camera);
        Frame cube = new Frame(centre.subtract(0.18, 0.18, 0.18), centre.add(0.18, 0.18, 0.18));
        int argb = EnergyGeometry.argb(markColour(mark.kind()), 0.6F);
        for (int[] face : FACES) {
            for (int i : face) {
                Vec3 p = cube.corner(i);
                vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Other builders

    /** Every other builder's box within {@value #OTHERS_RANGE} blocks: thin edges in his colour (D-B 7.1). */
    private static void others(ClientLevel level, LocalPlayer self, Matrix4f pose, VertexConsumer vc, Vec3 camera) {
        for (AbstractClientPlayer other : level.players()) {
            if (other == self || other.distanceToSqr(self) > (double) OTHERS_RANGE * OTHERS_RANGE) {
                continue;
            }
            Power power = Powers.power(other);
            BuildView view = other.getExistingData(ModAttachments.BUILD_VIEW).orElse(BuildView.EMPTY);
            if (!BuildClient.isBuilder(power) || !view.active() || view.box().isEmpty()) {
                continue;
            }
            BuildView.Box box = view.box().get();
            BoundingBox bounds = new BoundingBox(box.min().getX(), box.min().getY(), box.min().getZ(), box.max().getX(), box.max().getY(),
                    box.max().getZ());
            edges(pose, vc, Frame.of(bounds, camera, INFLATE), OTHERS_EDGE, EnergyGeometry.argb(power.color, 0.8F));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The label

    /**
     * What the label says and where it stands: over the top of the box or the selected thing, or of the cells a shape,
     * a Pull or a ghost writes; null for a brush, or with nothing to stand over.
     */
    private static @Nullable Label label(BuildGrammar g, LocalPlayer player, ClientLevel level, @Nullable BoundingBox box,
                                         @Nullable BoundingBox mask, @Nullable MagicSelect flood, float time, boolean calm) {
        Spell spell = g.spell();
        if (spell.isBrush()) {
            return null;
        }
        SpellPlanner.Plan plan = BuildClient.plan();
        boolean planned = plan != null && plan.prepared() && plan.refusal() == SpellPlanner.NONE;
        BoundingBox over;
        if (spell == Spell.SHAPE || spell == Spell.PULL || g.ghost()) {
            over = planned ? plan.bounds() : null;
        } else {
            over = box != null ? box : mask;
        }
        if (over == null) {
            return null;
        }
        MutableComponent text = Component.empty();
        if (spell == Spell.SHAPE) {
            text.append(Component.translatable("hud.enhanced.build.radius", number(g.radius())));
            if (g.shapeKind().hasHeight()) {
                text.append(" · ").append(Component.translatable("hud.enhanced.build.height", number(g.height())));
            }
        } else if (spell == Spell.PULL) {
            text.append(Component.translatable("hud.enhanced.build.layers", number(Math.abs(g.layers()))));
        } else {
            text.append(Component.translatable("hud.enhanced.build.size", number(over.getXSpan()), number(over.getYSpan()),
                    number(over.getZSpan())));
        }
        if (spell == Spell.REPEAT) {
            text.append(" · ").append(Component.translatable("hud.enhanced.build.copies", number(g.copies())));
            if (g.gap() > 0) {
                text.append(" · ").append(Component.translatable("hud.enhanced.build.gap", number(g.gap())));
            }
        }
        Component count = count(g, player, level, box, flood, plan, planned);
        if (count != null) {
            text.append(" · ").append(count);
        }
        int colour = 0xFFFFFF;
        if (g.heldBack()) {
            // Billy's side holds the rubber band: the label flashes.
            colour = calm || Mth.floor(time / 3.0F) % 2 == 0 ? ORANGE : 0xFFFFFF;
        }
        Vec3 top = new Vec3((over.minX() + over.maxX() + 1) / 2.0, over.maxY() + 1.0, (over.minZ() + over.maxZ() + 1) / 2.0);
        return new Label(top, text, colour);
    }

    /**
     * The label's number: the rubber band's volume; the cells of a selected thing as its flood finds them; while the
     * client counts a plan, {@code counting...} and how far it got; then the blocks it changes (the server's preflight,
     * once it came, over the client's count), "about" when some are in chunks not loaded, and for Billy what it costs
     * him, orange when more than his meter has now, red when more than his whole meter. Null when there is none.
     */
    private static @Nullable Component count(BuildGrammar g, LocalPlayer player, ClientLevel level, @Nullable BoundingBox box,
                                             @Nullable MagicSelect flood, @Nullable SpellPlanner.Plan plan, boolean planned) {
        if (g.rubberBand() && box != null) {
            return Component.literal(number((long) box.getXSpan() * box.getYSpan() * box.getZSpan()));
        }
        if (flood != null && !flood.done()) {
            return Component.literal(number(flood.size()));
        }
        if (!planned || !plan.writes()) {
            return flood != null ? Component.literal(number(flood.size())) : null;
        }
        SpellPlanner.Count count = BuildClient.count();
        if (count == null) {
            return null;
        }
        if (!count.done()) {
            return Component.translatable("hud.enhanced.build.counting", number(count.walked()));
        }
        BuildVerdictPayload preflight = BuildClient.preflight();
        long changed = BuildClient.changed();
        boolean about = count.unloaded() > 0 || preflight != null && (preflight.flags() & BuildVerdictPayload.UNLOADED) != 0;
        if (Powers.power(player) != Power.BILLY) {
            return about ? Component.translatable("hud.enhanced.build.about", number(changed)) : Component.literal(number(changed));
        }
        BuildData data = player.getExistingData(ModAttachments.BUILD_DATA).orElse(BuildData.EMPTY);
        long cost = BuildClient.cost(data);
        MutableComponent paid = about ? Component.translatable("hud.enhanced.build.about", number(cost)) : Component.literal(number(cost));
        paid.append(" ✦");
        if (cost > BillyMagic.top(data)) {
            paid.setStyle(Style.EMPTY.withColor(RED));
        } else if (cost > BillyMagic.meter(data, level.getGameTime())) {
            paid.setStyle(Style.EMPTY.withColor(ORANGE));
        }
        return paid;
    }

    /** A whole number with separators: {@code 1,240,000}. */
    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * Draws the label as a name tag is drawn: facing the camera, faint through walls and bright where it is seen, on a
     * dark background, growing with the distance past 6 blocks so it stays readable.
     */
    private static void drawLabel(Minecraft mc, Camera view, MultiBufferSource.BufferSource buffers, Label label, Vec3 camera,
                                  boolean overArrow) {
        Vec3 at = label.at().subtract(camera);
        float scale = 0.03F * grow(at, 6.0);
        // Over the arrow of a top face a scroll pushes, rather than through it.
        double lift = 0.3 + (overArrow ? 1.5 * grow(at, 16.0) : 0.0);
        PoseStack stack = new PoseStack();
        stack.translate(at.x, at.y + lift + 10.0 * scale, at.z);
        stack.mulPose(view.rotation());
        stack.scale(scale, -scale, scale);
        Matrix4f matrix = stack.last().pose();
        Font font = mc.font;
        float x = -font.width(label.text()) / 2.0F;
        int background = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        int colour = label.colour() & 0xFFFFFF;
        font.drawInBatch(label.text(), x, 0.0F, 0x80000000 | colour, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background,
                LightTexture.FULL_BRIGHT);
        font.drawInBatch(label.text(), x, 0.0F, 0xFF000000 | colour, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
    }

    private BuildRenderer() {
    }
}
