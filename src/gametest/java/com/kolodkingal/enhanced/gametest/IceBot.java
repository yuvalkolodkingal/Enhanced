package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.road.boat.IceBoatModel;
import com.kolodkingal.enhanced.road.boat.IceTrack;
import com.kolodkingal.enhanced.road.boat.IceTrack.Feature;
import com.kolodkingal.enhanced.road.boat.IceTrack.FeatureKind;
import com.kolodkingal.enhanced.road.boat.IceTrack.Piece;
import com.kolodkingal.enhanced.road.boat.IceTrack.Sample;
import com.kolodkingal.enhanced.road.boat.IceTrackGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * A driver for the Ice-Boat Race that plays by the keys a player has (W, A, D): it follows the line of the course, slows down for the
 * curves by the speed profile of the physics, winds round the snow walls and drifts, and either takes the shortcuts or not. It is
 * used to prove that every course can be driven from the start to the finish, on the simulated boat ({@link IceBoatSim}) and on the
 * real one. A "careless" driver reacts late and presses keys wrongly now and then.
 */
final class IceBot {
    /** The keys of one tick: left, right, up (W), down. */
    record Keys(boolean left, boolean right, boolean up, boolean down) {
    }

    final IceTrack track;
    final boolean shortcuts;
    final double[] px;
    final double[] pz;
    final double[] pcap;
    final double[] ps;
    final double[] target;
    private int index;
    private int slow;
    private boolean lost;
    private int reverse;
    private int recover;
    private final SplittableRandom noise;
    private final double carelessness;
    private final int delay;
    private final java.util.ArrayDeque<Keys> queue = new java.util.ArrayDeque<>();
    /** The index of the first point after each shortcut gap (to tell the run apart in the tests). */
    final List<Integer> gapPoints = new ArrayList<>();

    IceBot(IceTrack track, boolean shortcuts, long noiseSeed, double carelessness, int delay) {
        this.track = track;
        this.shortcuts = shortcuts;
        this.noise = new SplittableRandom(noiseSeed);
        this.carelessness = carelessness;
        this.delay = delay;
        List<double[]> pts = new ArrayList<>();
        List<Sample> main = track.main().samples();
        int i = 0;
        double top = IceBoatModel.terminalSpeed(IceBoatModel.PACKED_ICE_FRICTION);
        while (i < main.size()) {
            Sample sm = main.get(i);
            Piece fork = shortcuts ? forkAt(sm.s()) : null;
            if (fork != null && Math.abs(sm.s() - fork.s0()) < 1.0E-9) {
                IceTrack.Loop loop = loopOf(fork);
                pts.add(new double[]{sm.x(), sm.z(), sm.dx(), sm.dz(), top, 0, sm.hw()});
                int dx = IceTrack.dirX(loop.heading());
                int dz = IceTrack.dirZ(loop.heading());
                // The short way: along the straight, across the gap.
                int chord = chordLength(loop);
                for (int u = 1; u <= chord; u++) {
                    if (u == loop.gapFrom() + loop.gap()) {
                        gapPoints.add(pts.size());
                    }
                    pts.add(new double[]{loop.forkX() + dx * u, loop.forkZ() + dz * u, dx, dz, top, 0, IceTrackGenerator.CHORD_HALF_WIDTH});
                }
                // The long way is skipped.
                while (i < main.size() && main.get(i).s() < fork.s1() - 1.0E-9) {
                    i++;
                }
                continue;
            }
            double cap = top;
            if (sm.turn() != 0) {
                cap = Math.min(cap, IceBoatModel.cornerSpeed(sm.radius()));
            }
            cap = Math.min(cap, snowCap(sm.x(), sm.z(), sm.hw()));
            pts.add(new double[]{sm.x(), sm.z(), sm.dx(), sm.dz(), cap, sm.turn(), sm.hw()});
            i++;
        }
        int n = pts.size();
        px = new double[n];
        pz = new double[n];
        pcap = new double[n];
        ps = new double[n];
        double[] ds = new double[n];
        for (int k = 0; k < n; k++) {
            double[] q = pts.get(k);
            double offset = q[5] == 0 ? lateralOffset(q[0], q[1], q[2], q[3], q[6]) : 0.0;
            px[k] = q[0] + (-q[3]) * offset;
            pz[k] = q[1] + q[2] * offset;
            pcap[k] = q[4];
            if (k > 0) {
                ds[k] = Math.hypot(px[k] - px[k - 1], pz[k] - pz[k - 1]);
                ps[k] = ps[k - 1] + ds[k];
            }
        }
        target = IceBoatModel.speedProfile(pcap, ds, IceBoatModel.PACKED_ICE_FRICTION, 0.0);
    }

    /** The speed to keep near snow walls and drifts: a player slows down for them. */
    private double snowCap(double x, double z, double hw) {
        double cap = Double.MAX_VALUE;
        int hwi = (int) Math.floor(hw);
        for (Feature f : track.features()) {
            if (f.kind() != FeatureKind.SNOW && f.kind() != FeatureKind.SNOW_WALL) {
                continue;
            }
            if (Math.max(f.x1() - f.x0(), f.z1() - f.z0()) + 1 >= 2 * hwi + 1 && f.kind() == FeatureKind.SNOW) {
                continue;
            }
            double dx = Math.max(0.0, Math.max(f.x0() - x, x - (f.x1() + 1)));
            double dz = Math.max(0.0, Math.max(f.z0() - z, z - (f.z1() + 1)));
            if (Math.hypot(dx, dz) < 14.0) {
                cap = Math.min(cap, f.kind() == FeatureKind.SNOW_WALL ? 1.1 : 1.4);
            }
        }
        return cap;
    }

    private Piece forkAt(double s) {
        for (Piece piece : track.pieces()) {
            if (piece.kind() == IceTrack.Kind.FORK && Math.abs(piece.s0() - s) < 1.0E-9) {
                return piece;
            }
        }
        return null;
    }

    private IceTrack.Loop loopOf(Piece fork) {
        for (IceTrack.Loop loop : track.loops()) {
            // The loops are in the order of the forks.
            double[] at = {loop.forkX(), loop.forkZ()};
            for (Sample sm : track.main().samples()) {
                if (Math.abs(sm.s() - fork.s0()) < 1.0E-9 && Math.abs(sm.x() - at[0]) < 1.0E-6 && Math.abs(sm.z() - at[1]) < 1.0E-6) {
                    return loop;
                }
            }
        }
        throw new IllegalStateException("no loop for the fork");
    }

    private int chordLength(IceTrack.Loop loop) {
        for (IceTrack.Ribbon ribbon : track.ribbons()) {
            if (ribbon.route() != IceTrack.Route.CHORD) {
                continue;
            }
            Sample first = ribbon.samples().get(0);
            if (Math.abs(first.x() - loop.forkX()) < 1.0E-6 && Math.abs(first.z() - loop.forkZ()) < 1.0E-6) {
                return (int) Math.round(ribbon.samples().get(ribbon.samples().size() - 1).s());
            }
        }
        throw new IllegalStateException("no chord");
    }

    /** How far to the side of the line the driver keeps to get past the snow on a straight here. */
    private double lateralOffset(double x, double z, double dx, double dz, double hw) {
        double offset = 0.0;
        int hwi = (int) Math.floor(hw);
        for (Feature f : track.features()) {
            if (f.kind() != FeatureKind.SNOW && f.kind() != FeatureKind.SNOW_WALL) {
                continue;
            }
            int across = dx != 0 ? f.z1() - f.z0() + 1 : f.x1() - f.x0() + 1;
            if (across >= 2 * hwi + 1) {
                continue;
            }
            double cx = (f.x0() + f.x1() + 1) / 2.0;
            double cz = (f.z0() + f.z1() + 1) / 2.0;
            // Is the feature on this stretch of the track at all?
            double lat = (cx - x) * (-dz) + (cz - z) * dx;
            double along = (cx - x) * dx + (cz - z) * dz;
            double halfLength = dx != 0 ? (f.x1() - f.x0() + 1) / 2.0 : (f.z1() - f.z0() + 1) / 2.0;
            if (Math.abs(lat) > hw + 1.0 || Math.abs(along) > halfLength + 12.0) {
                continue;
            }
            double outside = Math.max(0.0, Math.abs(along) - halfLength);
            double weight = Math.max(0.0, 1.0 - outside / 12.0);
            weight = weight * weight * (3.0 - 2.0 * weight);
            double free = f.kind() == FeatureKind.SNOW_WALL ? hwi - 2 : hwi - 1;
            double side = lat >= 0 ? -1.0 : 1.0;
            // On the side of the covered part the line shifts the other way, to the middle of the clear lane.
            if (Math.abs(offset) < weight * free) {
                offset = side * weight * free;
            }
        }
        return offset;
    }

    // ------------------------------------------------------------------------------------------------------------

    /** Whether a boat can go in a straight line from one point to the other without touching a wall or a snow wall (air does not count). */
    private boolean visible(double x0, double z0, double x1, double z1) {
        IceTrack.Raster r = track.raster();
        double length = Math.hypot(x1 - x0, z1 - z0);
        int steps = (int) Math.ceil(length / 0.5);
        for (int k = 1; k <= steps; k++) {
            double x = x0 + (x1 - x0) * k / steps;
            double z = z0 + (z1 - z0) * k / steps;
            for (int cx = (int) Math.floor(x - 0.75); cx <= (int) Math.floor(x + 0.75); cx++) {
                for (int cz = (int) Math.floor(z - 0.75); cz <= (int) Math.floor(z + 0.75); cz++) {
                    if (r.isSolid(cx, cz)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The path index nearest to the boat, looking only a little way back and a good way ahead. */
    int nearest(double x, double z) {
        int best = index;
        double bestD = Double.MAX_VALUE;
        int from = Math.max(0, index - 10);
        int to = Math.min(px.length - 1, index + 80);
        if (lost) {
            from = 0;
            to = px.length - 1;
            lost = false;
        }
        for (int k = from; k <= to; k++) {
            double d = (px[k] - x) * (px[k] - x) + (pz[k] - z) * (pz[k] - z);
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        index = best;
        return best;
    }

    /** The boat was put somewhere else (back on a checkpoint): look for the place on the line again. */
    void relocated() {
        lost = true;
        slow = 0;
        reverse = 0;
        recover = 0;
    }

    /** The keys for this tick. */
    Keys drive(IceBoatSim boat) {
        int i = nearest(boat.x, boat.z);
        double speed = boat.speed();
        double look = recover > 0 ? 3.0 : Math.max(6.0, Math.min(20.0, 5.0 + 6.0 * speed));
        if (recover > 0) {
            recover--;
        }
        int j = i;
        while (j < px.length - 1 && ps[j] - ps[i] < look) {
            j++;
        }
        // Not through a wall: aim at the farthest point of the line that the boat can see.
        while (j > i + 1 && !visible(boat.x, boat.z, px[j], pz[j])) {
            j--;
        }
        double tx = px[j] - boat.x;
        double tz = pz[j] - boat.z;
        double dist = Math.hypot(tx, tz);
        if (dist < 1.0E-6) {
            dist = 1.0;
        }
        double wanted = target[i] * (1.0 - 0.06 * carelessness);
        // Where the thrust should point: the velocity that is wanted, less the one there is.
        double dvx = tx / dist * wanted - boat.vx;
        double dvz = tz / dist * wanted - boat.vz;
        double ax = dvx;
        double az = dvz;
        if (Math.hypot(ax, az) < 0.03) {
            ax = tx;
            az = tz;
        }
        double headingTarget = Math.toDegrees(Math.atan2(-ax, az));
        double err = wrap(headingTarget - boat.yaw);
        double rateWanted = Math.max(-5.0, Math.min(5.0, 0.12 * err));
        boolean left = false;
        boolean right = false;
        if (rateWanted > boat.deltaRotation + 0.6) {
            right = true;
        } else if (rateWanted < boat.deltaRotation - 0.6) {
            left = true;
        }
        // Thrust when it is wanted, and the boat points near the way it is to be pushed.
        boolean up = false;
        double along = boat.vx * (-IceBoatSim.sin((float) Math.toRadians(boat.yaw))) + boat.vz * IceBoatSim.cos((float) Math.toRadians(boat.yaw));
        if (Math.abs(err) < 75.0 && (speed < wanted || Math.abs(err) > 25.0 && speed < 0.4) && along < wanted + 0.2) {
            up = true;
        }
        // Stuck against a wall: back out for a moment.
        slow = up && reverse == 0 && speed < 0.04 ? slow + 1 : 0;
        if (slow > 24 && reverse == 0) {
            reverse = 30;
            slow = 0;
        }
        boolean down = false;
        if (reverse > 0) {
            reverse--;
            up = false;
            down = true;
            recover = 90;
        }
        Keys keys = new Keys(left, right, up, down);
        if (carelessness > 0.0) {
            if (noise.nextDouble() < 0.04 * carelessness) {
                keys = new Keys(noise.nextBoolean(), noise.nextBoolean(), noise.nextBoolean(), false);
            }
            queue.addLast(keys);
            keys = queue.size() > delay ? queue.pollFirst() : new Keys(false, false, true, false);
        }
        return keys;
    }

    /** Where the boat is, for a failure message: its place, speed, the point of the line it is nearest to, and where that is. */
    String where(IceBoatSim boat) {
        int k = nearest(boat.x, boat.z);
        return String.format("boat at %.1f %.1f %.1f speed %.2f yaw %.0f; line point %d of %d at %.1f %.1f (cap %.2f)", boat.x, boat.y, boat.z, boat.speed(), boat.yaw,
                k, px.length, px[k], pz[k], pcap[k]);
    }

    static double wrap(double angle) {
        double a = angle % 360.0;
        if (a > 180.0) {
            a -= 360.0;
        }
        if (a < -180.0) {
            a += 360.0;
        }
        return a;
    }

    /** What a run of the bot came to. */
    record Result(boolean finished, boolean fell, boolean stuck, int ticks, double topSpeed, double minSpeedAfterStart, boolean crossedGap,
                  String where) {
    }

    /** Drives the boat of the course from the start, parked as in the game, until it finishes, falls, sticks, or time runs out. */
    static Result run(IceTrack track, boolean shortcuts, int maxTicks, double carelessness, long noiseSeed) {
        IceBoatSim.TrackWorld world = new IceBoatSim.TrackWorld(track.raster());
        int h = track.heading();
        double bx = track.platformX() + IceTrack.dirX(h) * IceTrackGenerator.BOAT_PARK;
        double bz = track.platformZ() + IceTrack.dirZ(h) * IceTrackGenerator.BOAT_PARK;
        float yaw = (float) Math.toDegrees(Math.atan2(-IceTrack.dirX(h), IceTrack.dirZ(h)));
        IceBoatSim boat = new IceBoatSim(world, bx, 1.0, bz, yaw);
        IceBot bot = new IceBot(track, shortcuts, noiseSeed, carelessness, 3);
        double[] zone = track.finishZone();
        double top = 0.0;
        int stillTicks = 0;
        boolean crossed = false;
        double lowest = track.lowestLevel() + 1.0;
        for (int tick = 0; tick < maxTicks; tick++) {
            Keys keys = bot.drive(boat);
            boat.step(keys.left(), keys.right(), keys.up(), keys.down());
            double speed = boat.speed();
            top = Math.max(top, speed);
            if (boat.x >= zone[0] && boat.x <= zone[2] && boat.z >= zone[1] && boat.z <= zone[3]) {
                return new Result(true, false, false, tick + 1, top, 0.0, crossed, bot.where(boat));
            }
            if (boat.y < lowest - 6.0) {
                return new Result(false, true, false, tick + 1, top, 0.0, crossed, bot.where(boat));
            }
            if (!bot.gapPoints.isEmpty() && bot.index >= bot.gapPoints.get(0)) {
                crossed = true;
            }
            stillTicks = speed < 0.02 && tick > 40 ? stillTicks + 1 : 0;
            if (stillTicks > 100) {
                return new Result(false, false, true, tick + 1, top, 0.0, crossed, bot.where(boat));
            }
        }
        return new Result(false, false, false, maxTicks, top, 0.0, crossed, bot.where(boat));
    }
}
