package com.kolodkingal.enhanced.road.boat;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.RoadBuild;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadFx;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadPalette;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.kolodkingal.enhanced.road.boat.IceTrack.Checkpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Trial 3, the Ice-Boat Race: a long course of packed ice between walls, floating in the Road's fog, that the player drives a boat
 * along from the start line to the finish line, against a clock (par time, best time, a generous limit). The course is made by
 * {@link IceTrackGenerator} to what the boat can do on ice ({@link IceBoatModel}); this class draws it, parks the boat, runs the
 * countdown, follows the race, and puts the boat and the player back on the last checkpoint when he falls or is stuck.
 *
 * <p>The boat is driven by the player's own client (that is how boats work in the game); the server only follows. What the server does
 * is: spawn the boat (an invulnerable one, never broken by a fall), seat the player, hold the start barrier shut until "go", open the
 * checkpoints and the finish, and replace the boat (and seat him again) after every return, because that is the only way the
 * client's idea of where the boat is is surely put right.
 */
public final class IceBoatChallenge implements RoadChallenge {
    /** The key of the trial in the saved progress. */
    public static final String ID = "ice_boat";
    /** The best time (ticks) in the player's saved data of the trial. */
    public static final String BEST = "best";

    private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
    private static final BlockState BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState();
    private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.defaultBlockState();

    /** The part of the race the player is in. */
    enum Phase {
        /** On the platform, the boat waits at the start line. */
        GRID,
        /** In the boat, the countdown runs and the barrier is shut. */
        COUNTDOWN,
        /** The race. */
        RACING
    }

    /** What the race of one player remembers (the trial object itself is stateless). */
    static final class Race {
        final IceTrack track;
        Phase phase = Phase.GRID;
        Boat boat;
        ServerBossEvent bar;
        long countdownEnd;
        int lastSecond = -1;
        long goElapsed;
        long lastReturn = Long.MIN_VALUE / 2;
        boolean barrierOpen;
        /** He climbed out of the boat at the start line: it does not take him in again until he has walked away from it. */
        boolean disarmed;
        boolean ranked = true;
        Vec3 lastPos = Vec3.ZERO;
        double speed;
        int stillTicks;
        long lastHint;

        Race(IceTrack track) {
            this.track = track;
        }
    }

    private static final Map<UUID, Race> RACES = new HashMap<>();
    private static final Map<Integer, IceTrack> TRACKS = new HashMap<>();

    @Override
    public String id() {
        return ID;
    }

    /** The settings from the server config. */
    public static IceTrackGenerator.Params params() {
        return new IceTrackGenerator.Params(WitchesRoadConfig.BOAT_RACE_LENGTH.get(), WitchesRoadConfig.BOAT_RACE_TRACK_WIDTH.get(),
                WitchesRoadConfig.BOAT_RACE_DIFFICULTY.get(), WitchesRoadConfig.BOAT_RACE_SHORTCUTS.get(), WitchesRoadConfig.BOAT_RACE_OBSTACLES.get(),
                WitchesRoadConfig.BOAT_RACE_BLUE_ICE.get(), WitchesRoadConfig.BOAT_RACE_CHECKPOINT_EVERY.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    @Override
    public RoadLayout build(RoadBuild build) {
        IceTrack track = IceTrackGenerator.generate(build.seed(), params());
        TRACKS.put(build.canvas().cell(), track);
        return draw(build.canvas(), track, WitchesRoadConfig.FALL_DEPTH.get(), timeLimitTicks(track));
    }

    /**
     * The time allowed: the config's, but never less than {@code boatRaceFairFactor} times the par time of the course (a long course with the
     * default limit would otherwise be unfair to a driver who is not fast); 0 = none.
     */
    public static int timeLimitTicks(IceTrack track) {
        int limit = WitchesRoadConfig.BOAT_RACE_TIME_LIMIT_SECONDS.get() * 20;
        return limit == 0 ? 0 : Math.max(limit, (int) Math.round(parSeconds(track) * WitchesRoadConfig.BOAT_RACE_FAIR_FACTOR.get() * 20));
    }

    /** The yaw of a boat or a player facing the way of this heading (0 = +x, 1 = +z, 2 = -x, 3 = -z). */
    public static float yawOf(int heading) {
        return (float) (Mth.atan2(-IceTrack.dirX(heading), IceTrack.dirZ(heading)) * Mth.RAD_TO_DEG);
    }

    /** Draws the course on the canvas and says where everything is. */
    public static RoadLayout draw(RoadCanvas canvas, IceTrack track, int fallDepth, int timeLimitTicks) {
        IceTrack.Raster r = track.raster();
        for (int z = r.minZ(); z <= r.maxZ(); z++) {
            for (int x = r.minX(); x <= r.maxX(); x++) {
                byte kind = r.kind(x, z);
                if (kind == IceTrack.VOID) {
                    continue;
                }
                int level = r.level(x, z);
                switch (kind) {
                    case IceTrack.PACKED -> floor(canvas, x, level, z, PACKED_ICE);
                    case IceTrack.BLUE -> floor(canvas, x, level, z, BLUE_ICE);
                    case IceTrack.SNOW -> floor(canvas, x, level, z, SNOW_BLOCK);
                    case IceTrack.OBSTACLE -> {
                        floor(canvas, x, level, z, PACKED_ICE);
                        for (int h = 1; h <= r.wallHeight(x, z); h++) {
                            canvas.set(x, level + h, z, SNOW_BLOCK);
                        }
                    }
                    case IceTrack.WALL -> {
                        int height = r.wallHeight(x, z);
                        for (int y = level - 1; y < level + height; y++) {
                            canvas.set(x, y, z, PACKED_ICE);
                        }
                        canvas.set(x, level + height, z, RoadPalette.TRIM);
                    }
                    default -> {
                    }
                }
            }
        }
        lights(canvas, track);
        int hwi = (int) Math.floor(track.params().halfWidth());
        int h0 = track.heading();
        int d0x = IceTrack.dirX(h0);
        int d0z = IceTrack.dirZ(h0);
        // The start: the platform, an arch over the start line, and the barrier that holds the boat until "go".
        int pcx = (int) Math.floor(track.platformX());
        int pcz = (int) Math.floor(track.platformZ());
        RoadPalette.startPlatform(canvas, pcx, 0, pcz);
        arch(canvas, track, pcx + d0x * IceTrackGenerator.START_LINE, pcz + d0z * IceTrackGenerator.START_LINE, h0, 0, hwi);
        for (BlockPos pos : barrierCells(track, canvas.origin())) {
            canvas.set(pos, RoadPalette.GLASS);
        }
        // The finish: an arch over the finish line, the platform, and the gate to the next trial.
        int fh = track.finishHeading();
        int fdx = IceTrack.dirX(fh);
        int fdz = IceTrack.dirZ(fh);
        int lowest = track.lowestLevel();
        int fcx = (int) Math.floor(track.finishX());
        int fcz = (int) Math.floor(track.finishZ());
        int endX = fcx - fdx * 4;
        int endZ = fcz - fdz * 4;
        arch(canvas, track, endX - fdx * IceTrackGenerator.FINISH_LINE_BACK, endZ - fdz * IceTrackGenerator.FINISH_LINE_BACK, fh, lowest, hwi);
        RoadPalette.finishPlatform(canvas, fcx, lowest, fcz);
        int gx = fcx + fdx * 2;
        int gz = fcz + fdz * 2;
        Direction.Axis axis = fdx != 0 ? Direction.Axis.Z : Direction.Axis.X;
        RoadPalette.gate(canvas, gx, lowest + 1, gz, axis);
        // The checkpoints.
        BlockPos origin = canvas.origin();
        List<RoadLayout.Checkpoint> checkpoints = new ArrayList<>();
        float startYaw = yawOf(h0);
        Vec3 startAt = new Vec3(origin.getX() + track.platformX() + d0x * 3.0, origin.getY() + 1.0, origin.getZ() + track.platformZ() + d0z * 3.0);
        checkpoints.add(new RoadLayout.Checkpoint(startAt, startYaw, null, failY(track, origin, -1.0, fallDepth)));
        List<Checkpoint> marks = checkpoints(track);
        for (Checkpoint cp : marks) {
            arch(canvas, track, (int) Math.floor(cp.x()), (int) Math.floor(cp.z()), cp.heading(), cp.level(), hwi);
            checkpoints.add(zoneOf(origin, track, cp, fallDepth));
        }
        BlockPos exit = RoadPalette.startExitDoor(canvas, pcx, 0, pcz);
        RoadLayout.Gate gate = new RoadLayout.Gate(canvas.at(gx, lowest + 1, gz), axis);
        return new RoadLayout(checkpoints, null, gate, exit, timeLimitTicks);
    }

    /** The checkpoints of the course: the start line first (a boat that falls early is put back there, with no countdown), then the track's. */
    static List<Checkpoint> checkpoints(IceTrack track) {
        List<Checkpoint> all = new ArrayList<>();
        int h = track.heading();
        double u = IceTrackGenerator.BARRIER + 3.0;
        all.add(new Checkpoint(track.startAlongX(u), track.startAlongZ(u), h, track.params().halfWidth(), 0, IceTrackGenerator.PLATFORM_BACK + u));
        all.addAll(track.checkpoints());
        return all;
    }

    private static RoadLayout.Checkpoint zoneOf(BlockPos origin, IceTrack track, Checkpoint cp, int fallDepth) {
        int dx = IceTrack.dirX(cp.heading());
        int dz = IceTrack.dirZ(cp.heading());
        double cx = origin.getX() + cp.x();
        double cz = origin.getZ() + cp.z();
        double surface = origin.getY() + cp.level() + 1.0;
        double along = 3.0;
        double across = cp.hw() + 1.0;
        double ex = Math.abs(dx) * along + Math.abs(dz) * across;
        double ez = Math.abs(dz) * along + Math.abs(dx) * across;
        AABB zone = new AABB(cx - ex, surface - 0.6, cz - ez, cx + ex, surface + 4.0, cz + ez);
        Vec3 at = new Vec3(cx, surface, cz);
        return new RoadLayout.Checkpoint(at, yawOf(cp.heading()), zone, failY(track, origin, cp.s(), fallDepth));
    }

    /** The height below which the boat has fallen off the course: {@code fallDepth} under the lowest floor from here on. */
    private static double failY(IceTrack track, BlockPos origin, double s, int fallDepth) {
        int lowest = 0;
        for (IceTrack.Sample sm : track.main().samples()) {
            if (sm.s() >= s) {
                lowest = Math.min(lowest, sm.level());
            }
        }
        return origin.getY() + lowest + 1.0 - fallDepth;
    }

    private static void floor(RoadCanvas canvas, int x, int level, int z, BlockState top) {
        canvas.set(x, level, z, top);
        canvas.set(x, level - 1, z, PACKED_ICE);
    }

    /** The cells of the glass barrier across the track in front of the boat. */
    public static List<BlockPos> barrierCells(IceTrack track, BlockPos origin) {
        List<BlockPos> cells = new ArrayList<>();
        int h = track.heading();
        int dx = IceTrack.dirX(h);
        int dz = IceTrack.dirZ(h);
        int hwi = (int) Math.floor(track.params().halfWidth());
        int cx = (int) Math.floor(track.platformX()) + dx * IceTrackGenerator.BARRIER;
        int cz = (int) Math.floor(track.platformZ()) + dz * IceTrackGenerator.BARRIER;
        for (int l = -hwi; l <= hwi; l++) {
            for (int y = 1; y <= 3; y++) {
                cells.add(origin.offset(cx + (-dz) * l, y, cz + dx * l));
            }
        }
        return cells;
    }

    /** An arch of amethyst over the track at this cell (the middle of the track): two pillars on the walls, a lintel, lanterns. */
    private static void arch(RoadCanvas canvas, IceTrack track, int cx, int cz, int heading, int level, int hwi) {
        int dx = IceTrack.dirX(heading);
        int dz = IceTrack.dirZ(heading);
        int rx = -dz;
        int rz = dx;
        int top = level + 6;
        for (int side = -1; side <= 1; side += 2) {
            int px = cx + rx * side * (hwi + 1);
            int pz = cz + rz * side * (hwi + 1);
            for (int a = 0; a <= 1; a++) {
                int ax = px + dx * a;
                int az = pz + dz * a;
                IceTrack.Raster r = track.raster();
                int base = level + (r.kind(ax, az) == IceTrack.WALL ? r.wallHeight(ax, az) : 2) + 1;
                for (int y = base; y <= top; y++) {
                    canvas.set(ax, y, az, y == top ? RoadPalette.AMETHYST : RoadPalette.TRIM);
                }
                canvas.set(ax, top + 1, az, RoadPalette.LANTERN);
            }
        }
        for (int l = -hwi - 1; l <= hwi + 1; l++) {
            for (int a = 0; a <= 1; a++) {
                canvas.set(cx + rx * l + dx * a, top, cz + rz * l + dz * a, RoadPalette.AMETHYST);
            }
        }
        for (int l = -hwi + 1; l <= hwi - 1; l += 4) {
            canvas.set(cx + rx * l, top - 1, cz + rz * l, RoadPalette.HANGING_LANTERN);
        }
    }

    /** Sea lanterns sunk in the walls, soul lanterns on the walls, and lanterns hanging under the floor, along the course. */
    private static void lights(RoadCanvas canvas, IceTrack track) {
        IceTrack.Raster r = track.raster();
        List<IceTrack.Sample> samples = track.main().samples();
        int hwi = (int) Math.floor(track.params().halfWidth());
        int sinceSea = 0;
        int sinceSoul = 0;
        int sinceUnder = 0;
        boolean left = false;
        IceTrack.Sample prev = null;
        for (IceTrack.Sample sm : samples) {
            double step = prev == null ? 0.0 : Math.hypot(sm.x() - prev.x(), sm.z() - prev.z());
            prev = sm;
            sinceSea += (int) Math.round(step * 2.0);
            sinceSoul += (int) Math.round(step * 2.0);
            sinceUnder += (int) Math.round(step * 2.0);
            double rx = -sm.dz();
            double rz = sm.dx();
            double off = sm.hw() + 1.0;
            if (sinceSea >= 14) {
                sinceSea = 0;
                left = !left;
                int cx = (int) Math.floor(sm.x() + rx * off * (left ? -1 : 1));
                int cz = (int) Math.floor(sm.z() + rz * off * (left ? -1 : 1));
                if (r.kind(cx, cz) == IceTrack.WALL) {
                    canvas.set(cx, r.level(cx, cz) + 1, cz, SEA_LANTERN);
                }
            }
            if (sinceSoul >= 40) {
                sinceSoul = 0;
                for (int side = -1; side <= 1; side += 2) {
                    int cx = (int) Math.floor(sm.x() + rx * (sm.hw() + 2.0) * side);
                    int cz = (int) Math.floor(sm.z() + rz * (sm.hw() + 2.0) * side);
                    if (r.kind(cx, cz) == IceTrack.WALL) {
                        canvas.set(cx, r.level(cx, cz) + r.wallHeight(cx, cz) + 1, cz, RoadPalette.LANTERN);
                    }
                }
            }
            if (sinceUnder >= 56) {
                sinceUnder = 0;
                int cx = (int) Math.floor(sm.x());
                int cz = (int) Math.floor(sm.z());
                if (r.isFloor(cx, cz)) {
                    canvas.set(cx, r.level(cx, cz) - 2, cz, RoadPalette.HANGING_LANTERN);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The race

    private static Vec3 start(RoadTrial trial, IceTrack track, double along) {
        BlockPos origin = trial.canvas().origin();
        return new Vec3(origin.getX() + track.startAlongX(along), origin.getY() + 1.0, origin.getZ() + track.startAlongZ(along));
    }

    /** Whether this player has a race going (the boat is out, or the countdown, or the clock). */
    public static boolean hasRace(UUID owner) {
        return RACES.containsKey(owner);
    }

    @Override
    public void onStart(RoadTrial trial, ServerPlayer player) {
        IceTrack track = TRACKS.get(trial.cell());
        if (track == null) {
            track = IceTrackGenerator.generate(trial.seed(), params());
            TRACKS.put(trial.cell(), track);
        }
        Race race = new Race(track);
        Race old = RACES.put(trial.owner(), race);
        if (old != null && old.bar != null) {
            old.bar.removeAllPlayers();
        }
        race.bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
        race.bar.addPlayer(player);
        race.lastPos = player.position();
        if (trial.checkpoint() == 0) {
            park(trial, player, race);
        } else {
            race.ranked = false;
            launch(trial, player, race);
        }
    }

    @Override
    public void onReset(RoadTrial trial, ServerPlayer player, int checkpoint) {
        Race race = RACES.get(trial.owner());
        if (race == null || trial.phase() == RoadTrial.Phase.WON) {
            return;
        }
        race.stillTicks = 0;
        race.speed = 0.0;
        if (checkpoint == 0) {
            park(trial, player, race);
        } else {
            launch(trial, player, race);
        }
    }

    @Override
    public void onEnd(RoadTrial trial, End reason) {
        Race race = RACES.remove(trial.owner());
        if (race != null && race.bar != null) {
            race.bar.removeAllPlayers();
        }
        TRACKS.remove(trial.cell());
    }

    /** The start: the barrier shut, the boat parked at the start line, the player on the platform. */
    private static void park(RoadTrial trial, ServerPlayer player, Race race) {
        setBarrier(trial, race, false);
        race.phase = Phase.GRID;
        race.disarmed = false;
        race.lastSecond = -1;
        Vec3 at = start(trial, race.track, IceTrackGenerator.BOAT_PARK);
        newBoat(trial, race, at, yawOf(race.track.heading()));
        race.lastHint = -1000;
    }

    /** The race goes on from the last checkpoint: a new boat there, with the player in it. */
    private static void launch(RoadTrial trial, ServerPlayer player, Race race) {
        setBarrier(trial, race, true);
        race.phase = Phase.RACING;
        RoadLayout.Checkpoint cp = trial.layout().checkpoint(trial.checkpoint());
        newBoat(trial, race, cp.respawn(), cp.yaw());
        player.startRiding(race.boat, true);
    }

    private static void newBoat(RoadTrial trial, Race race, Vec3 at, float yaw) {
        if (race.boat != null) {
            if (!race.boat.isRemoved()) {
                race.boat.discard();
            }
            trial.untrack(race.boat);
        }
        ServerLevel level = trial.level();
        Boat boat = EntityType.BOAT.create(level);
        boat.setVariant(Boat.Type.DARK_OAK);
        boat.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        boat.setInvulnerable(true);
        level.addFreshEntity(trial.track(boat));
        race.boat = boat;
        race.lastPos = at;
    }

    private static void setBarrier(RoadTrial trial, Race race, boolean open) {
        if (race.barrierOpen == open) {
            return;
        }
        race.barrierOpen = open;
        ServerLevel level = trial.level();
        for (BlockPos pos : barrierCells(race.track, trial.canvas().origin())) {
            trial.canvas().live(level, pos, open ? RoadPalette.AIR : RoadPalette.GLASS);
        }
    }

    @Override
    public void tick(RoadTrial trial, ServerPlayer player) {
        Race race = RACES.get(trial.owner());
        if (race == null) {
            return;
        }
        ServerLevel level = trial.level();
        long now = level.getGameTime();
        if (race.boat == null || race.boat.isRemoved()) {
            if (race.phase == Phase.RACING) {
                // The boat is gone (a command, a bug): the player is put back at his checkpoint with a new one.
                trial.putBack(player, false);
                return;
            }
            park(trial, player, race);
        }
        Boat boat = race.boat;
        boat.resetFallDistance();
        boolean riding = player.getVehicle() == boat;
        Vec3 pos = boat.position();
        race.speed = Math.hypot(pos.x - race.lastPos.x, pos.z - race.lastPos.z);
        race.lastPos = pos;
        switch (race.phase) {
            case GRID -> tickGrid(trial, player, race, riding, now);
            case COUNTDOWN -> tickCountdown(trial, player, race, riding, now);
            case RACING -> tickRacing(trial, player, race, riding, now);
        }
        if (now % 5 == 0) {
            hud(trial, player, race, now);
        }
        effects(level, player, race, now);
    }

    private static void tickGrid(RoadTrial trial, ServerPlayer player, Race race, boolean riding, long now) {
        if (riding) {
            race.phase = Phase.COUNTDOWN;
            race.countdownEnd = now + WitchesRoadConfig.BOAT_RACE_COUNTDOWN_SECONDS.get() * 20L;
            race.lastSecond = -1;
            return;
        }
        Vec3 d = race.boat.position().subtract(player.position());
        double near = Math.hypot(d.x, d.z);
        if (race.disarmed && near > 3.5) {
            race.disarmed = false;
        }
        if (!race.disarmed && !player.isShiftKeyDown() && near < 2.4 && Math.abs(d.y) < 2.0) {
            player.startRiding(race.boat, true);
            return;
        }
        if (now - race.lastHint >= 60) {
            race.lastHint = now;
            player.displayClientMessage(Component.translatable("message.enhanced.road.boat.board").withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), true);
        }
    }

    private static void tickCountdown(RoadTrial trial, ServerPlayer player, Race race, boolean riding, long now) {
        if (!riding) {
            // He climbed out (Shift): back to the platform; he can leave by the door, or walk to the boat again.
            race.phase = Phase.GRID;
            race.disarmed = true;
            race.lastHint = -1000;
            return;
        }
        long left = race.countdownEnd - now;
        int second = (int) Math.ceil(left / 20.0);
        if (left > 0 && second != race.lastSecond) {
            race.lastSecond = second;
            title(player, Component.literal(Integer.toString(second)).withStyle(s -> s.withColor(WitchesRoad.LIGHT).withBold(true)),
                    second == WitchesRoadConfig.BOAT_RACE_COUNTDOWN_SECONDS.get() ? Component.translatable("message.enhanced.road.boat.keys")
                            .withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)) : Component.empty(), 0, 25, 5);
            Vec3 at = player.position();
            trial.level().playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F + 0.1F * second);
        }
        if (left <= 0) {
            setBarrier(trial, race, true);
            race.phase = Phase.RACING;
            race.goElapsed = trial.elapsed();
            race.stillTicks = 0;
            title(player, Component.translatable("message.enhanced.road.boat.go").withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE).withBold(true)),
                    Component.translatable("message.enhanced.road.boat.par", RoadTrial.clock(parSeconds(race.track))).withStyle(s -> s.withColor(WitchesRoad.LIGHT)),
                    0, 30, 10);
            Vec3 at = player.position();
            ServerLevel level = trial.level();
            level.playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
            level.playSound(null, at.x, at.y, at.z, ModSounds.WITCHES_ROAD.get(), SoundSource.PLAYERS, 0.6F, 1.3F);
            RoadFx.sparkle(level, at.add(0.0, 0.5, 0.0), 40);
        }
    }

    private static void tickRacing(RoadTrial trial, ServerPlayer player, Race race, boolean riding, long now) {
        if (!riding) {
            dismounted(trial, player, race, now);
            return;
        }
        Vec3 pos = race.boat.position();
        double[] zone = zoneOfFinish(trial, race.track);
        if (pos.x >= zone[0] && pos.x <= zone[2] && pos.z >= zone[1] && pos.z <= zone[3]) {
            finish(trial, player, race);
            return;
        }
        // Not moving for a while: say how to get out.
        race.stillTicks = race.speed < 0.02 ? race.stillTicks + 1 : 0;
        if (race.stillTicks >= 80 && now - race.lastHint >= 100) {
            race.lastHint = now;
            player.displayClientMessage(Component.translatable("message.enhanced.road.boat.stuck").withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), true);
        }
    }

    /** The player is out of the boat in the race (he sneaked, or something threw him out): back to the last checkpoint, if the cooldown allows. */
    private static void dismounted(RoadTrial trial, ServerPlayer player, Race race, long now) {
        long cooldown = WitchesRoadConfig.BOAT_RACE_RESPAWN_COOLDOWN_SECONDS.get() * 20L;
        long waited = now - race.lastReturn;
        double near = race.boat.position().distanceTo(player.position());
        if (waited >= cooldown || near > 4.0 || !race.boat.isAlive()) {
            race.lastReturn = now;
            player.displayClientMessage(Component.translatable("message.enhanced.road.boat.respawn").withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), true);
            trial.putBack(player, false);
            return;
        }
        // Too soon after the last time: he stays in his boat.
        player.startRiding(race.boat, true);
        long seconds = (cooldown - waited + 19) / 20;
        if (now - race.lastHint >= 20) {
            race.lastHint = now;
            player.displayClientMessage(Component.translatable("message.enhanced.road.boat.wait", seconds).withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
        }
    }

    /** The zone that wins the race, in the world's coordinates: {@code [minX, minZ, maxX, maxZ]}. */
    static double[] zoneOfFinish(RoadTrial trial, IceTrack track) {
        BlockPos origin = trial.canvas().origin();
        double[] z = track.finishZone();
        return new double[]{origin.getX() + z[0], origin.getZ() + z[1], origin.getX() + z[2], origin.getZ() + z[3]};
    }

    /** The time a good driver needs and a bit (the config's factor), in seconds. */
    public static int parSeconds(IceTrack track) {
        return (int) Math.round(track.parTicks() * WitchesRoadConfig.BOAT_RACE_PAR_FACTOR.get() / 20.0);
    }

    /** The boat crosses the finish line: the time, the par and the best time, the gate opens, and the player stands on the finish platform. */
    private static void finish(RoadTrial trial, ServerPlayer player, Race race) {
        long ticks = trial.elapsed() - race.goElapsed;
        int par = parSeconds(race.track);
        RoadProgress progress = trial.progress();
        CompoundTag data = progress.data(ID);
        int best = data.contains(BEST) ? data.getInt(BEST) : 0;
        boolean record = race.ranked && (best == 0 || ticks < best);
        if (record) {
            data.putInt(BEST, (int) ticks);
            progress.putData(ID, data);
        }
        String time = RoadTrial.clock(Math.round(ticks / 20.0));
        player.sendSystemMessage(Component.translatable("message.enhanced.road.boat.finish", time, RoadTrial.clock(par)).withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)));
        if (ticks / 20.0 <= par) {
            player.sendSystemMessage(Component.translatable("message.enhanced.road.boat.par_beat").withStyle(s -> s.withColor(WitchesRoad.LIGHT)));
        }
        if (record) {
            player.sendSystemMessage(Component.translatable("message.enhanced.road.boat.best", time).withStyle(s -> s.withColor(WitchesRoad.LIGHT)));
        } else if (best > 0) {
            player.sendSystemMessage(Component.translatable("message.enhanced.road.boat.best_was", RoadTrial.clock(Math.round(best / 20.0))).withStyle(s -> s.withColor(WitchesRoad.LIGHT)));
        }
        title(player, Component.translatable("message.enhanced.road.boat.finish.title").withStyle(s -> s.withColor(WitchesRoad.LIGHT).withBold(true)),
                Component.translatable("message.enhanced.road.boat.finish.sub", time, RoadTrial.clock(par)).withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), 0, 70, 20);
        Boat boat = race.boat;
        ServerLevel level = trial.level();
        trial.succeed();
        race.bar.removeAllPlayers();
        player.stopRiding();
        if (boat != null && !boat.isRemoved()) {
            boat.discard();
        }
        // The platform: standing in front of the gate.
        BlockPos origin = trial.canvas().origin();
        int fh = race.track.finishHeading();
        double sx = origin.getX() + race.track.finishX() - IceTrack.dirX(fh) * 1.5;
        double sz = origin.getZ() + race.track.finishZ() - IceTrack.dirZ(fh) * 1.5;
        player.teleportTo(level, sx, origin.getY() + race.track.lowestLevel() + 1.0, sz, yawOf(fh), 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private static void title(ServerPlayer player, Component title, Component subtitle, int in, int stay, int out) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the player sees

    private static void hud(RoadTrial trial, ServerPlayer player, Race race, long now) {
        int limit = trial.layout().timeLimitTicks();
        int total = trial.layout().checkpoints().size() - 1;
        long left = limit > 0 ? Math.max(0L, limit - trial.elapsed()) : -1L;
        Component text;
        if (left >= 0) {
            text = Component.translatable("message.enhanced.road.boat.hud", RoadTrial.clock((left + 19) / 20), trial.checkpoint(), total);
            float progress = Mth.clamp(left / (float) limit, 0.0F, 1.0F);
            race.bar.setProgress(progress);
            race.bar.setColor(progress > 0.25F ? BossEvent.BossBarColor.BLUE : progress > 0.1F ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
        } else {
            text = Component.translatable("message.enhanced.road.boat.hud.free", trial.checkpoint(), total);
            race.bar.setProgress(1.0F);
        }
        race.bar.setName(text);
        if (race.phase == Phase.RACING && player.getVehicle() == race.boat) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.boat.speed", String.format("%.0f", race.speed * 20.0))
                    .withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
        }
    }

    /** Frost round the boat, and the lights of the sky: slow green, blue and violet motes high above the course, like an aurora. */
    private static void effects(ServerLevel level, ServerPlayer player, Race race, long now) {
        Vec3 at = race.boat.position();
        RandomSource random = level.getRandom();
        if (now % 2 == 0 && race.speed > 0.25) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.3, at.z, 3, 0.4, 0.1, 0.4, 0.03);
        }
        if (now % 6 == 0) {
            int[] colors = {0x39E6A0, 0x4FD8FF, 0x9B7BFF, 0x6CFFD2};
            for (int i = 0; i < 3; i++) {
                double ox = (random.nextDouble() - 0.5) * 70.0;
                double oz = (random.nextDouble() - 0.5) * 70.0;
                level.sendParticles(new EnergyParticleOptions(colors[random.nextInt(colors.length)], 3.0F), at.x + ox, at.y + 14.0 + random.nextDouble() * 12.0,
                        at.z + oz, 0, (random.nextDouble() - 0.5), 0.05, (random.nextDouble() - 0.5), 0.12);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
}
