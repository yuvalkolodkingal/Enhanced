package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.block.MultiverseBridgeBlock;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import com.kolodkingal.enhanced.strange.SlingPortals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reed Richards' Multiverse Bridge (3.19): crafted from eight netherite blocks round an empty middle. Set down, its core
 * raises a great upright ring of {@link #RADIUS} blocks round the place above it ({@link #build}), facing the way the
 * player looked. To use it:
 * <ol>
 *     <li>give the core <b>power</b>: a redstone signal (a lever, a button held, a redstone block beside it);</li>
 *     <li><b>stand in the middle</b> of the ring, on the core, and use the core (look down at it): the Multiverse screen
 *     opens ({@link UniverseTravel#BRIDGE}); a renamed item in a hand tunes it to the universe its name names;</li>
 *     <li>the ring gathers its energy for {@link #SPOOL_TICKS} (stay in the middle), then sends everyone standing in it
 *     and holds the way open as a pair of portals for a while, so that the others can follow and come back.</li>
 * </ol>
 * Breaking the core takes the whole ring down (the core drops itself; the ring is the bridge's own).
 */
public final class MultiverseBridge {
    /** The ring's radius (blocks): the ring is {@code 2 * RADIUS + 1} wide and high, its lowest block the core. */
    public static final int RADIUS = 5;
    /** How long the ring gathers its energy before it sends (ticks). */
    public static final int SPOOL_TICKS = 60;
    /** How many of the ring's blocks may be missing for it still to work. */
    static final int MISSING_ALLOWED = 2;

    /** A bridge gathering its energy: who started it, where to, until when. */
    private record Spool(GlobalPos core, UUID starter, int universe, long until) {
    }

    private static final Map<GlobalPos, Spool> SPOOLS = new HashMap<>();

    private MultiverseBridge() {
    }

    // ---- the ring ----

    /** The way along the ring's face, sideways (the ring stands across the way the core faces). */
    static Direction side(Direction facing) {
        return facing.getClockWise();
    }

    /** The places of the ring's blocks, the core's own first. */
    public static List<BlockPos> ring(BlockPos core, Direction facing) {
        List<BlockPos> blocks = new ArrayList<>();
        Direction side = side(facing);
        for (int b = -RADIUS; b <= RADIUS; b++) {
            for (int a = -RADIUS; a <= RADIUS; a++) {
                double d = Math.sqrt(a * a + b * b);
                if (Math.abs(d - RADIUS) < 0.5) {
                    BlockPos pos = core.relative(side, a).above(b + RADIUS);
                    if (a == 0 && b == -RADIUS) {
                        blocks.add(0, pos);
                    } else {
                        blocks.add(pos);
                    }
                }
            }
        }
        return blocks;
    }

    /** The middle of the ring (a point). */
    static Vec3 centre(BlockPos core) {
        return Vec3.atBottomCenterOf(core).add(0.0, RADIUS + 0.5, 0.0);
    }

    /** Raises the ring round a core just set down (where there is room; the rest stays as it is). */
    public static void build(Level level, BlockPos core, Direction facing) {
        BlockState frame = ModBlocks.MULTIVERSE_BRIDGE_FRAME.get().defaultBlockState();
        List<BlockPos> blocks = ring(core, facing);
        for (int i = 1; i < blocks.size(); i++) {
            BlockPos pos = blocks.get(i);
            if (level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, frame, 3);
            }
        }
        level.playSound(null, core, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    /** Takes the ring of a broken core down. */
    public static void tearDown(Level level, BlockPos core, Direction facing) {
        List<BlockPos> blocks = ring(core, facing);
        for (int i = 1; i < blocks.size(); i++) {
            BlockPos pos = blocks.get(i);
            if (level.getBlockState(pos).is(ModBlocks.MULTIVERSE_BRIDGE_FRAME.get())) {
                level.removeBlock(pos, false);
            }
        }
        SPOOLS.remove(GlobalPos.of(level.dimension(), core));
        level.playSound(null, core, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    /** How many of the ring's blocks are missing. */
    static int missing(Level level, BlockPos core, Direction facing) {
        int missing = 0;
        List<BlockPos> blocks = ring(core, facing);
        for (int i = 1; i < blocks.size(); i++) {
            if (!level.getBlockState(blocks.get(i)).is(ModBlocks.MULTIVERSE_BRIDGE_FRAME.get())) {
                missing++;
            }
        }
        return missing;
    }

    /** Whether the player stands in the middle of the ring: on the core, or just above it. */
    public static boolean standsInMiddle(Player player, BlockPos core) {
        Vec3 feet = player.position();
        double dx = feet.x - (core.getX() + 0.5);
        double dz = feet.z - (core.getZ() + 0.5);
        double up = feet.y - (core.getY() + 1.0);
        return dx * dx + dz * dz <= 0.9 * 0.9 && up >= -0.1 && up <= 1.5;
    }

    // ---- using it ----

    /** The player uses the core: if all is ready, the Multiverse screen opens. */
    public static void use(ServerPlayer player, BlockPos core, BlockState state) {
        ServerLevel level = player.serverLevel();
        Direction facing = state.getValue(MultiverseBridgeBlock.FACING);
        Component refusal = refusal(level, core, facing);
        if (refusal == null && !standsInMiddle(player, core)) {
            refusal = Component.translatable("message.enhanced.multiverse.bridge.stand");
        }
        if (refusal != null) {
            player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        UniverseTravel.open(player, UniverseTravel.BRIDGE, core.immutable());
    }

    /** Why the bridge cannot work now, or null. */
    static Component refusal(ServerLevel level, BlockPos core, Direction facing) {
        if (SPOOLS.containsKey(GlobalPos.of(level.dimension(), core))) {
            return Component.translatable("message.enhanced.multiverse.bridge.busy");
        }
        if (missing(level, core, facing) > MISSING_ALLOWED) {
            return Component.translatable("message.enhanced.multiverse.bridge.broken");
        }
        if (!level.hasNeighborSignal(core)) {
            return Component.translatable("message.enhanced.multiverse.bridge.no_power");
        }
        return null;
    }

    /** The universe is chosen: the ring gathers its energy. */
    static void start(ServerPlayer player, BlockPos core, int universe) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(core);
        if (!state.is(ModBlocks.MULTIVERSE_BRIDGE.get())) {
            return;
        }
        Direction facing = state.getValue(MultiverseBridgeBlock.FACING);
        Component refusal = refusal(level, core, facing);
        if (refusal == null && !standsInMiddle(player, core)) {
            refusal = Component.translatable("message.enhanced.multiverse.bridge.stand");
        }
        if (refusal != null) {
            player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        GlobalPos at = GlobalPos.of(level.dimension(), core.immutable());
        SPOOLS.put(at, new Spool(at, player.getUUID(), universe, level.getGameTime() + SPOOL_TICKS));
        level.playSound(null, core, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5F, 0.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.multiverse.bridge.charging", Multiverse.name(universe))
                .withStyle(ChatFormatting.AQUA), true);
    }

    /** Every server tick: the bridges gathering their energy glow, and send when they are ready. */
    public static void serverTick(MinecraftServer server) {
        if (SPOOLS.isEmpty()) {
            return;
        }
        Iterator<Spool> it = SPOOLS.values().iterator();
        List<Spool> ready = new ArrayList<>();
        while (it.hasNext()) {
            Spool spool = it.next();
            ServerLevel level = server.getLevel(spool.core().dimension());
            BlockPos core = spool.core().pos();
            if (level == null || !level.getBlockState(core).is(ModBlocks.MULTIVERSE_BRIDGE.get())) {
                it.remove();
                continue;
            }
            Direction facing = level.getBlockState(core).getValue(MultiverseBridgeBlock.FACING);
            long left = spool.until() - level.getGameTime();
            glow(level, core, facing, 1.0F - (float) left / SPOOL_TICKS);
            if (left <= 0) {
                it.remove();
                ready.add(spool);
            }
        }
        for (Spool spool : ready) {
            fire(server, spool);
        }
    }

    /** The ring fills with blue light as it gathers its energy. */
    static void glow(ServerLevel level, BlockPos core, Direction facing, float progress) {
        Vec3 c = centre(core);
        Direction side = side(facing);
        int n = 6 + (int) (progress * 18);
        for (int i = 0; i < n; i++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double r = RADIUS - 0.6 - level.random.nextDouble() * (RADIUS - 0.6) * progress;
            double a = Math.cos(angle) * r;
            double b = Math.sin(angle) * r;
            level.sendParticles(new EnergyParticleOptions(i % 3 == 0 ? 0xFFFFFF : UniverseTravel.BRIDGE_BLUE, 1.2F),
                    c.x + side.getStepX() * a, c.y + b, c.z + side.getStepZ() * a, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (level.getGameTime() % 10 == 0) {
            level.playSound(null, core, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.2F, 0.6F + progress);
        }
    }

    /** The energy is gathered: everyone standing in the ring goes, and the way stays open. */
    static void fire(MinecraftServer server, Spool spool) {
        ServerLevel level = server.getLevel(spool.core().dimension());
        ServerPlayer starter = server.getPlayerList().getPlayer(spool.starter());
        BlockPos core = spool.core().pos();
        if (level == null || starter == null || starter.level() != level || !standsInMiddle(starter, core)) {
            if (starter != null) {
                starter.displayClientMessage(Component.translatable("message.enhanced.multiverse.bridge.left"), true);
            }
            return;
        }
        Direction facing = level.getBlockState(core).getValue(MultiverseBridgeBlock.FACING);
        SpaceRealms.Arrival arrival = UniverseTravel.arrival(starter, spool.universe());
        if (arrival == null) {
            starter.displayClientMessage(Component.translatable("message.enhanced.stones.space.no_ground"), true);
            return;
        }
        // The others standing in the ring go with him.
        Vec3 c = centre(core);
        Direction side = side(facing);
        AABB box = new AABB(c, c).inflate(Math.abs(side.getStepX()) * RADIUS + 0.8, RADIUS, Math.abs(side.getStepZ()) * RADIUS + 0.8);
        List<ServerPlayer> going = new ArrayList<>();
        for (Player p : level.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isSpectator())) {
            if (p instanceof ServerPlayer sp && sp != starter && c.distanceTo(sp.position()) <= RADIUS) {
                going.add(sp);
            }
        }
        Vec3 nearFeet = Vec3.atBottomCenterOf(core.above());
        float yaw = facing.toYRot();
        if (!UniverseTravel.send(starter, spool.universe(), arrival, UniverseTravel.BRIDGE_BLUE)) {
            return;
        }
        for (ServerPlayer p : going) {
            UniverseTravel.send(p, spool.universe(), new SpaceRealms.Arrival(arrival.level(), arrival.spot(), arrival.yaw()),
                    UniverseTravel.BRIDGE_BLUE);
        }
        // The way back: a pair of portals, the ring's and one beside where they landed.
        Vec3 far = arrival.spot().add(Vec3.directionFromRotation(0.0F, arrival.yaw()).scale(SlingPortals.NEAR + 1.0));
        if (!SlingPortals.roomFor(arrival.level(), far)) {
            far = arrival.spot();
        }
        SlingPortals.open(starter, level, nearFeet, yaw, arrival.level(), far, arrival.yaw() + 180.0F,
                UniverseTravel.BRIDGE_BLUE);
    }

    /** Forgets every bridge's energy (the server stops). */
    public static void clear() {
        SPOOLS.clear();
    }
}
