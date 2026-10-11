package com.kolodkingal.enhanced.client.sokovia;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.network.SokoviaLiftPayload;
import com.kolodkingal.enhanced.ultron.SokoviaShape;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import com.kolodkingal.enhanced.ultron.SokoviaTemplate;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The client side of the Sokovia lift: what the player sees of the island moving. The server tells the client, in about
 * 70 bytes, that the island rises or comes down ({@link SokoviaLiftPayload}); everything else the client works out from
 * the world it has and the numbers of the shape ({@link SokoviaShape}):
 *
 * <ul>
 *     <li><b>The rise.</b> The client copies the island from its own ground with the rules of the server ({@link
 *     SokoviaTemplate}), builds a picture of it ({@link SokoviaMesh}) and draws it at the height the time says ({@link
 *     SokoviaShape#offset}), out of the place it was copied from. Meanwhile it <em>hides the original ground</em> (it
 *     sets those blocks to air in its own copy of the world, a few hundred a tick; the server's ground never changes),
 *     so that it looks as if the ground itself rose. When the server has placed the island in the sky, the picture goes
 *     and the real blocks are there; the ground stays hidden while the island is up.</li>
 *     <li><b>The descent</b> is the mirror: the client reads the real island in the sky into a picture, the server
 *     clears it, and the picture comes down; when it has landed the ground is shown again (the blocks the client hid
 *     are put back where they are still air).</li>
 *     <li><b>The dust</b> of the move: pieces of ground fall from the rim; <b>the shake</b> is {@link
 *     SokoviaCamera}.</li>
 * </ul>
 *
 * <p>A client without the chunks sees holes; a client that reloads the chunks sees the ground again; the server's world
 * is the same in every case. This class is loaded only when a payload arrives, on a client.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SokoviaClient {
    /** The ground is hidden and shown again this many blocks a tick. */
    private static final int SLICE = 700;
    /** The dust of the move is made within this far of the player. */
    private static final double DUST_RANGE = 96.0;

    /** What the client knows of the island it is showing. */
    static final class Island {
        final Spec spec;
        final BlockPos centre;
        final int ticks;
        final ClientLevel level;
        /**
         * {@link SokoviaLiftPayload#RISE}, {@link SokoviaLiftPayload#LOWER}, or {@link #HELD} once the island is real.
         */
        int phase;
        long start;
        @Nullable
        SokoviaMesh mesh;
        boolean hide;
        /** The ground cells still to be hidden, and the originals of those that are. */
        final Deque<BlockPos> toHide = new ArrayDeque<>();
        final Long2ObjectMap<BlockState> hidden = new Long2ObjectOpenHashMap<>();
        final Deque<BlockPos> toShow = new ArrayDeque<>();
        boolean groundCaptured;
        boolean ending;

        Island(Spec spec, BlockPos centre, int ticks, ClientLevel level) {
            this.spec = spec;
            this.centre = centre;
            this.ticks = ticks;
            this.level = level;
        }
    }

    /** The island is real and up: only the hidden ground remains of the picture. */
    static final int HELD = 3;

    @Nullable
    private static Island island;

    // ---------------------------------------------------------------------------------------------------------
    // From the server

    /** A payload arrived (on the main thread). */
    public static void receive(SokoviaLiftPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) {
            return;
        }
        Spec spec = new Spec(payload.radius(), payload.height(), payload.skin(), payload.cover(), payload.coneDepth(), payload.seed());
        Island current = island;
        boolean same = current != null && current.level == level && current.spec.equals(spec) && current.centre.equals(payload.centre());
        switch (payload.phase()) {
            case SokoviaLiftPayload.RISE -> {
                if (same && current.phase == SokoviaLiftPayload.RISE) {
                    current.start = payload.start();
                    return;
                }
                drop(current, false);
                Island next = new Island(spec, payload.centre(), payload.ticks(), level);
                next.phase = SokoviaLiftPayload.RISE;
                next.start = payload.start();
                next.hide = payload.hide();
                SokoviaTemplate ground = SokoviaTemplate.capture(level, spec, payload.centre());
                next.mesh = SokoviaMesh.build(ground, level);
                queueTheGround(next, ground, level);
                island = next;
            }
            case SokoviaLiftPayload.LOWER -> {
                if (same && current.phase == SokoviaLiftPayload.LOWER) {
                    current.start = payload.start();
                    return;
                }
                Island next;
                if (same) {
                    next = current;
                    if (next.mesh != null) {
                        next.mesh.close();
                    }
                } else {
                    drop(current, false);
                    next = new Island(spec, payload.centre(), payload.ticks(), level);
                    next.hide = payload.hide();
                }
                next.phase = SokoviaLiftPayload.LOWER;
                next.start = payload.start();
                next.ending = false;
                // The island as it stands in the sky now (the server is about to clear it): that is what comes down.
                next.mesh = SokoviaMesh.build(SokoviaTemplate.read(level, spec, payload.centre(), spec.height()), level);
                if (!same && payload.hide()) {
                    queueTheGround(next, SokoviaTemplate.capture(level, spec, payload.centre()), level);
                }
                island = next;
            }
            default -> {
                // END: the picture goes. If the island is up the ground stays hidden; if not everything is shown again.
                if (current == null || !same && payload.hide() == false) {
                    drop(current, false);
                    return;
                }
                if (!same) {
                    // A client that arrives while the island is up: hide the ground it never saw rise.
                    drop(current, false);
                    Island next = new Island(spec, payload.centre(), payload.ticks(), level);
                    next.hide = payload.hide();
                    if (payload.hide()) {
                        queueTheGround(next, SokoviaTemplate.capture(level, spec, payload.centre()), level);
                    }
                    next.phase = HELD;
                    island = next;
                    return;
                }
                if (current.mesh != null) {
                    current.mesh.close();
                    current.mesh = null;
                }
                if (payload.hide()) {
                    current.phase = HELD;
                } else {
                    current.ending = true;
                    current.toHide.clear();
                    for (long key : current.hidden.keySet()) {
                        current.toShow.add(BlockPos.of(key));
                    }
                }
            }
        }
    }

    /**
     * The cells of the ground the island was copied from, to be hidden once the picture is ready to take their place.
     */
    private static void queueTheGround(Island next, SokoviaTemplate ground, ClientLevel level) {
        if (!next.hide) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < ground.size(); i++) {
            int dx = ground.x(i) - ground.centre().getX();
            int dz = ground.z(i) - ground.centre().getZ();
            int g = ground.groundAt(dx, dz);
            // The skin and the cover came from the ground (the cone is made up); only a block that is really there is
            // hidden.
            if (g != SokoviaTemplate.NO_GROUND && ground.y(i) > g - next.spec.skin()) {
                pos.set(ground.x(i), ground.y(i), ground.z(i));
                if (level.getBlockState(pos) == ground.state(i)) {
                    next.toHide.add(pos.immutable());
                }
            }
        }
        next.groundCaptured = true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Island current = island;
        if (current == null) {
            return;
        }
        if (mc.level != current.level || mc.player == null) {
            drop(current, true);
            return;
        }
        ClientLevel level = current.level;
        if (current.mesh != null && current.mesh.failed()) {
            current.mesh.close();
            current.mesh = null;
            Enhanced.LOGGER.warn("The picture of Sokovia could not be built; the island is real all the same");
        }
        // Hide the ground once the picture is there to stand in for it.
        boolean ready = current.mesh == null || current.mesh.isReady();
        if (ready && !current.toHide.isEmpty() && !current.ending) {
            for (int i = 0; i < SLICE && !current.toHide.isEmpty(); i++) {
                BlockPos pos = current.toHide.poll();
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    current.hidden.put(pos.asLong(), state);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        if (current.ending) {
            for (int i = 0; i < SLICE && !current.toShow.isEmpty(); i++) {
                BlockPos pos = current.toShow.poll();
                BlockState original = current.hidden.remove(pos.asLong());
                if (original != null && level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, original, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
            if (current.toShow.isEmpty()) {
                drop(current, false);
                return;
            }
        }
        if (current.mesh != null && moving(current, 0.0F)) {
            dust(current, level);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        drop(island, true);
    }

    /** Lets the island go. With {@code abandon} the world is gone too, so there is nothing to show again. */
    private static void drop(@Nullable Island current, boolean abandon) {
        if (current == null) {
            return;
        }
        if (current.mesh != null) {
            current.mesh.close();
            current.mesh = null;
        }
        if (!abandon && !current.hidden.isEmpty() && Minecraft.getInstance().level == current.level) {
            // (A hurried end: the ground is put back at once.)
            for (Long2ObjectMap.Entry<BlockState> entry : current.hidden.long2ObjectEntrySet()) {
                BlockPos pos = BlockPos.of(entry.getLongKey());
                if (current.level.getBlockState(pos).isAir()) {
                    current.level.setBlock(pos, entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        current.hidden.clear();
        current.toHide.clear();
        current.toShow.clear();
        if (island == current) {
            island = null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the others ask

    /** The island that is being shown, or null. */
    @Nullable
    static Island current() {
        return island;
    }

    /** How far up the picture of the island is, {@code partial} of a tick after this one. */
    static double offset(Island current, float partial) {
        double height = current.spec.height();
        if (current.phase == HELD) {
            return height;
        }
        double t = current.level.getGameTime() + partial - current.start;
        double eased = SokoviaShape.smoother(t / Math.max(1, current.ticks));
        return current.phase == SokoviaLiftPayload.RISE ? height * eased : height * (1.0 - eased);
    }

    /** Whether the island is on the move (it has begun and has not arrived), or is about to (the lock). */
    static boolean moving(Island current, float partial) {
        if (current.phase == HELD) {
            return false;
        }
        double t = current.level.getGameTime() + partial - current.start;
        return t > 0.0 && t < current.ticks;
    }

    /**
     * How hard the ground shakes under the island's move now, 0 to 1: nothing before and after, a low rumble in the
     * second before the rise, and the speed of the ease at its peak while it moves.
     */
    static float shake(Island current, float partial) {
        if (current.phase == HELD || current.start == Long.MAX_VALUE) {
            return current.phase == SokoviaLiftPayload.LOWER && current.start == Long.MAX_VALUE ? 0.25F : 0.0F;
        }
        double t = current.level.getGameTime() + partial - current.start;
        if (t < 0.0) {
            return t > -20.0 ? 0.25F : 0.0F;
        }
        double u = t / Math.max(1, current.ticks);
        if (u >= 1.0) {
            return 0.0F;
        }
        return (float) (16.0 * u * u * (1.0 - u) * (1.0 - u)); // (the ease's speed, 1 at its peak)
    }

    // ---------------------------------------------------------------------------------------------------------
    // The dust

    private static void dust(Island current, ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (EnhancedClientConfig.reducedMotion() || mc.player == null) {
            return;
        }
        RandomSource random = level.getRandom();
        double cx = current.centre.getX() + 0.5;
        double cz = current.centre.getZ() + 0.5;
        if (mc.player.distanceToSqr(cx, mc.player.getY(), cz) > DUST_RANGE * DUST_RANGE) {
            return;
        }
        double offset = offset(current, 0.0F);
        List<BlockState> debris = List.of(Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState());
        int base = current.phase == SokoviaLiftPayload.RISE ? current.centre.getY() : current.centre.getY();
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double rim = SokoviaShape.rim(current.spec, angle);
            double x = cx + Math.cos(angle) * rim;
            double z = cz + Math.sin(angle) * rim;
            BlockState state = debris.get(random.nextInt(debris.size()));
            // Pieces fall from the underside of the rim, and a curtain of dust hangs over the edge of the hole.
            double y = base + offset - current.spec.skin() - random.nextDouble() * 2.0;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 0.0, -0.2 - random.nextDouble() * 0.3, 0.0);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.POOF, cx + Math.cos(angle) * (rim - 0.5), base + 0.5, cz + Math.sin(angle) * (rim - 0.5),
                        0.0, 0.05, 0.0);
            }
        }
    }

    private SokoviaClient() {
    }

    /** For the tests of the numbers above (and nobody else): a list of what the current island is doing. */
    static List<String> describe() {
        List<String> found = new ArrayList<>();
        Island current = island;
        if (current != null) {
            found.add("phase " + current.phase + " start " + current.start + " hidden " + current.hidden.size() + " toHide " + current.toHide.size());
        }
        return found;
    }
}
