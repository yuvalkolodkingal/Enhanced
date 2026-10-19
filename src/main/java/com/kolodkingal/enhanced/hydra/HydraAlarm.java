package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.network.HydraFxPayload;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.world.HydraOutpostPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The alarm of a Hydra outpost and its reinforcements (3.6, the plan's 15.5).
 *
 * <ul>
 *   <li><b>Raising it</b>: the officer raises it the moment he sees an intruder; any other soldier after
 *   {@link HydraSoldierEntity#ALARM_AFTER 3 s} of fighting, if the officer lives and the soldier can reach a radio
 *   console (he runs there: killing him on the way stops it). A frozen, turned, steered or possessed soldier raises
 *   nothing; nor does anyone once the radio console has switched the alarm off for the day.</li>
 *   <li><b>The alarm</b> ({@code alarmTicks} 600): a siren from the radio room (heard 48 blocks), the four redstone
 *   lamps of the radio and command rooms lit (put back after; nothing else is touched), every soldier of the outpost
 *   goes for the intruders, and after {@link #WAVE_DELAY} ticks the <b>reinforcements</b>: {@code reinforcements} (4:
 *   riflemen and an enforcer; one fewer on Easy) come up the cellar stairs, two of them run in through the gate from
 *   the road 32 blocks out when it is loaded. At most {@code maxAlarmsPerDay} (2) waves an outpost a day; they are not
 *   posts (they never come back) and leave, vanishing unseen, 2 minutes after the last intruder is gone.</li>
 *   <li><b>Silencing</b>: kill the officer before he sees you, or hold use 3 s on a radio console in the radio room
 *   ("Alarm off.": the lamps go dark, no wave comes, and the alarm stays off for the rest of the day). Killing the
 *   officer before the reinforcements come stops them too.</li>
 * </ul>
 */
public final class HydraAlarm {
    /** Ticks from the alarm to the reinforcements. */
    public static final int WAVE_DELAY = 200;
    /** How long the reinforcements stay after the last intruder is gone (2 minutes). */
    public static final int LINGER = 2400;
    /** How long holding use on the console takes to switch the alarm off (3 s). */
    public static final int SILENCE_TICKS = 60;
    /** How long the console keeps the alarm off (an in-game day). */
    public static final int SILENCED_FOR = 24000;
    /** How often (ticks) a sounding alarm tells the players near again (they keep its siren's loop going). */
    public static final int SIREN_REFRESH = 80;
    /** How far the siren, the lines and the convergence reach. */
    public static final double REACH = 48.0;
    /** How near a runner must come to a console's spot. */
    public static final double CONSOLE_REACH = 2.5;
    /** How long a runner tries to reach a console before he gives up. */
    public static final int RUN_TICKS = 300;
    /** How far out on the road the reinforcements who come by the gate appear. */
    public static final int ROAD = 32;

    /** Players holding use on a console: the outpost's key, when the hold began, when it was last renewed. */
    private static final Map<UUID, long[]> HOLDS = new HashMap<>();
    /** When each runner started running (game time). */
    private static final Map<UUID, Long> RUN_SINCE = new HashMap<>();

    // ---- the numbers ----

    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || HydraConfig.ALARM.get();
    }

    static int alarmTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.ALARM_TICKS.get() : 600;
    }

    static int maxWaves() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.MAX_ALARMS_PER_DAY.get() : 2;
    }

    /** How many reinforcements a wave brings on this difficulty (the option; one fewer on Easy). */
    public static int waveSize(Difficulty difficulty) {
        int n = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.REINFORCEMENTS.get() : 4;
        return difficulty == Difficulty.EASY ? Math.max(0, n - 1) : n;
    }

    /** The in-game day of {@code level} for the waves' count (by its clock of day, so {@code /time add} counts). */
    public static long day(ServerLevel level) {
        return Math.floorDiv(level.getDayTime(), 24000L);
    }

    // ---- raising it ----

    /**
     * A soldier of an outpost fights {@code target} (every tick of the fight): the officer raises the alarm at once
     * for an intruder; any other soldier, after 3 s, becomes the outpost's runner (one at a time) if its officer
     * lives.
     */
    static void fighting(ServerLevel level, HydraSoldierEntity soldier, LivingEntity target, long now) {
        HydraOutposts.Outpost outpost = soldier.outpost();
        if (outpost == null || !enabled() || outpost.alarmOn(now) || outpost.silenced(now)) {
            return;
        }
        boolean intruder = target instanceof Player || HydraLoyalty.isTraitor(target);
        if (!intruder) {
            return;
        }
        if (soldier.isOfficer()) {
            raise(level, outpost, soldier, target);
            return;
        }
        if (soldier.fightingFor() < HydraSoldierEntity.ALARM_AFTER || soldier.alarmTried() || outpost.runner != null) {
            return;
        }
        soldier.markAlarmTried();
        if (officer(level, outpost) == null) {
            return; // nobody answers the radio
        }
        outpost.runner = soldier.getUUID();
        RUN_SINCE.put(soldier.getUUID(), now);
    }

    /** Whether {@code soldier} is his outpost's runner now (and may still be). */
    static boolean isRunner(HydraSoldierEntity soldier) {
        HydraOutposts.Outpost outpost = soldier.outpost();
        if (outpost == null || !soldier.getUUID().equals(outpost.runner)) {
            return false;
        }
        long now = soldier.level().getGameTime();
        Long since = RUN_SINCE.get(soldier.getUUID());
        if (!soldier.mayRaiseAlarm() || outpost.alarmOn(now) || (since != null && now - since > RUN_TICKS)) {
            outpost.runner = null;
            RUN_SINCE.remove(soldier.getUUID());
            return false;
        }
        return true;
    }

    /** The runner reached a console: the alarm (if the officer still lives). */
    static void runnerArrived(ServerLevel level, HydraOutposts.Outpost outpost, HydraSoldierEntity runner) {
        outpost.runner = null;
        RUN_SINCE.remove(runner.getUUID());
        if (officer(level, outpost) != null && runner.mayRaiseAlarm()) {
            raise(level, outpost, runner, runner.getTarget());
        }
    }

    /** The runner found no way to a console: nobody raises it this time. */
    static void runnerFailed(HydraOutposts.Outpost outpost, HydraSoldierEntity runner) {
        outpost.runner = null;
        RUN_SINCE.remove(runner.getUUID());
    }

    /** The spot in front of the console nearest {@code from}. */
    static BlockPos nearestConsoleSpot(HydraOutposts.Outpost outpost, Vec3 from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos local : Post.CONSOLE_SPOTS) {
            BlockPos at = outpost.where(local);
            double d = Vec3.atBottomCenterOf(at).distanceToSqr(from);
            if (d < bestDistance) {
                best = at;
                bestDistance = d;
            }
        }
        return best;
    }

    /** The outpost's officer, if he is alive and loaded, else null. */
    @Nullable
    public static HydraOfficerEntity officer(ServerLevel level, HydraOutposts.Outpost outpost) {
        UUID id = outpost.soldier(Post.OFFICER.ordinal());
        return id != null && level.getEntity(id) instanceof HydraOfficerEntity officer && officer.isAlive()
                ? officer : null;
    }

    /**
     * Raises the alarm of {@code outpost} (by {@code raiser}, toward {@code intruder}): nothing if the alarms are off,
     * it already sounds, the console switched it off, or the raiser may not.
     *
     * @return whether it was raised
     */
    public static boolean raise(ServerLevel level, HydraOutposts.Outpost outpost, @Nullable HydraSoldierEntity raiser,
                                @Nullable LivingEntity intruder) {
        long now = level.getGameTime();
        if (!enabled() || outpost.alarmOn(now) || outpost.silenced(now)
                || (raiser != null && !raiser.mayRaiseAlarm())) {
            return false;
        }
        outpost.alarmUntil = now + alarmTicks();
        long day = day(level);
        if (outpost.waves(day) < maxWaves()) {
            outpost.waveDay = day;
            outpost.waves = outpost.waves(day) + 1;
            outpost.waveAt = now + WAVE_DELAY;
        } else {
            outpost.waveAt = -1L;
        }
        outpost.runner = null;
        if (intruder instanceof Player) {
            outpost.lastIntruder = now;
        }
        outpost.changed();
        lamps(level, outpost, true);
        tell(level, outpost, "message.enhanced.hydra.alarm");
        send(level, outpost, HydraFxPayload.ALARM_ON);
        if (intruder != null) {
            for (HydraSoldierEntity soldier : members(level, outpost)) {
                if (soldier.getTarget() == null && !HydraLoyalty.isTraitor(soldier) && soldier != intruder) {
                    soldier.alertTo(intruder.position(), false);
                    if (soldier.distanceToSqr(intruder) < REACH * REACH && (soldier.hasLineOfSight(intruder)
                            || soldier.distanceToSqr(intruder) < 16 * 16)) {
                        soldier.setTarget(intruder);
                    }
                }
            }
        }
        return true;
    }

    /**
     * A player held use on a radio console of an outpost ({@code consolePos}): after {@link #SILENCE_TICKS} of
     * holding (the clicks of a held button come every few ticks) the alarm is off for the day.
     *
     * @return whether {@code consolePos} is a radio console of an outpost (the use is taken)
     */
    public static boolean useConsole(ServerPlayer player, BlockPos consolePos) {
        ServerLevel level = player.serverLevel();
        HydraOutposts store = HydraOutposts.peek(level);
        HydraOutposts.Outpost outpost = store == null ? null : consoleOf(store, consolePos);
        if (outpost == null) {
            return false;
        }
        long now = level.getGameTime();
        long[] hold = HOLDS.get(player.getUUID());
        if (hold == null || hold[0] != outpost.key() || now - hold[2] > 10) {
            hold = new long[]{outpost.key(), now, now};
            HOLDS.put(player.getUUID(), hold);
        }
        hold[2] = now;
        long held = now - hold[1];
        if (held >= SILENCE_TICKS) {
            HOLDS.remove(player.getUUID());
            silence(level, outpost);
            player.displayClientMessage(Component.translatable("message.enhanced.hydra.alarm_off"), true);
        } else {
            int seconds = (int) Math.ceil((SILENCE_TICKS - held) / 20.0);
            player.displayClientMessage(Component.translatable("message.enhanced.hydra.console", seconds), true);
        }
        return true;
    }

    /** The outpost one of whose radio consoles is at {@code pos}, or null. */
    @Nullable
    static HydraOutposts.Outpost consoleOf(HydraOutposts store, BlockPos pos) {
        for (HydraOutposts.Outpost outpost : store.all()) {
            for (BlockPos console : Post.RADIO_CONSOLES) {
                if (outpost.where(console).equals(pos)) {
                    return outpost;
                }
            }
        }
        return null;
    }

    /** Switches the alarm off for the day: the lamps go dark and no wave comes. */
    public static void silence(ServerLevel level, HydraOutposts.Outpost outpost) {
        long now = level.getGameTime();
        boolean was = outpost.alarmOn(now);
        outpost.alarmUntil = -1L;
        outpost.waveAt = -1L;
        outpost.runner = null;
        outpost.silencedUntil = now + SILENCED_FOR;
        outpost.changed();
        lamps(level, outpost, false);
        if (was) {
            send(level, outpost, HydraFxPayload.ALARM_OFF);
        }
    }

    /** Ends a sounding alarm (its time is up): the lamps go dark; a wave not yet come does not come. */
    public static void end(ServerLevel level, HydraOutposts.Outpost outpost) {
        outpost.alarmUntil = -1L;
        outpost.waveAt = -1L;
        outpost.runner = null;
        outpost.changed();
        lamps(level, outpost, false);
        send(level, outpost, HydraFxPayload.ALARM_OFF);
    }

    /** The officer fell: before the reinforcements came, the alarm stops and they do not come. */
    static void officerDown(ServerLevel level, HydraOutposts.Outpost outpost) {
        long now = level.getGameTime();
        if (outpost.alarmOn(now) && outpost.waveAt > now) {
            outpost.alarmUntil = -1L;
            outpost.waveAt = -1L;
            outpost.changed();
            lamps(level, outpost, false);
            send(level, outpost, HydraFxPayload.ALARM_OFF);
        }
    }

    // ---- every tick ----

    /** Every tick of a level: the siren, the wave when it is due, the end of the alarm, the reinforcements leaving. */
    public static void tick(ServerLevel level) {
        HydraOutposts store = HydraOutposts.peek(level);
        if (store == null) {
            return;
        }
        long now = level.getGameTime();
        for (HydraOutposts.Outpost outpost : store.all()) {
            if (outpost.alarmOn(now)) {
                if ((outpost.alarmUntil - now) % SIREN_REFRESH == 0) {
                    // Players who came near since hear it too (the client keeps one loop per console).
                    send(level, outpost, HydraFxPayload.ALARM_ON);
                }
                if (outpost.lampsLit && now % 20 == 0) {
                    lamps(level, outpost, true);
                }
                if (outpost.waveAt > 0 && now >= outpost.waveAt) {
                    outpost.waveAt = -1L;
                    outpost.changed();
                    wave(level, outpost);
                }
            } else if (outpost.lampsLit || outpost.alarmUntil > 0) {
                outpost.alarmUntil = -1L;
                outpost.waveAt = -1L;
                outpost.changed();
                lamps(level, outpost, false);
                send(level, outpost, HydraFxPayload.ALARM_OFF);
            }
            if (!outpost.reinforcements.isEmpty() && now % 20 == 0) {
                reinforcements(level, outpost, now);
            }
        }
    }

    /** The reinforcements leave 2 minutes after the last intruder; a leaving one vanishes where nobody sees him. */
    private static void reinforcements(ServerLevel level, HydraOutposts.Outpost outpost, long now) {
        boolean over = !outpost.alarmOn(now) && now - outpost.lastIntruder > LINGER;
        for (UUID id : new ArrayList<>(outpost.reinforcements)) {
            Entity e = level.getEntity(id);
            if (!(e instanceof HydraSoldierEntity soldier) || !soldier.isAlive()) {
                if (e == null && !level.isLoaded(outpost.centre())) {
                    continue;
                }
                outpost.reinforcements.remove(id);
                outpost.changed();
                continue;
            }
            if (over) {
                soldier.leave(now);
            }
            if (soldier.isLeaving() && (now - soldier.leavingSince() > 200 || soldier.getTarget() == null)
                    && HydraGarrison.unseen(level, soldier.position(), 16.0)) {
                outpost.reinforcements.remove(id);
                outpost.changed();
                soldier.discard();
            }
        }
    }

    /** The reinforcements: up the cellar stairs, two of them by the gate from the road when it is loaded. */
    static List<HydraSoldierEntity> wave(ServerLevel level, HydraOutposts.Outpost outpost) {
        List<HydraSoldierEntity> came = new ArrayList<>();
        int size = waveSize(level.getDifficulty());
        if (size <= 0 || level.getDifficulty() == Difficulty.PEACEFUL) {
            return came;
        }
        BlockPos cellar = outpost.where(HydraOutpostPlan.CELLAR_SPAWN);
        BlockPos road = roadPoint(level, outpost);
        boolean roadOpen = outpost.real() && level.isLoaded(road);
        LivingEntity intruder = nearestIntruder(level, outpost);
        for (int i = 0; i < size; i++) {
            if (!HydraGarrison.roomFor(level, outpost)) {
                break;
            }
            Rank rank = i % 3 == 2 ? Rank.ENFORCER : Rank.RIFLEMAN;
            BlockPos at = roadOpen && i >= size - 2 ? road : cellar;
            HydraSoldierEntity soldier = ModEntities.HYDRA_SOLDIER.get().create(level);
            if (soldier == null) {
                continue;
            }
            soldier.setRank(rank, level.random.nextInt(rank.skins()));
            soldier.belongTo(outpost, -1, true);
            soldier.moveTo(at.getX() + 0.5 + (i % 2) * 0.3, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360.0F,
                    0.0F);
            level.addFreshEntity(soldier);
            outpost.reinforcements.add(soldier.getUUID());
            if (intruder != null) {
                soldier.setTarget(intruder);
            }
            came.add(soldier);
        }
        outpost.changed();
        if (!came.isEmpty()) {
            tell(level, outpost, "message.enhanced.hydra.reinforcements");
            level.playSound(null, cellar, ModSounds.HYDRA_RADIO.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        return came;
    }

    /** The point on the road 32 blocks out of the gate (on the ground, where it is loaded). */
    static BlockPos roadPoint(ServerLevel level, HydraOutposts.Outpost outpost) {
        BlockPos gate = outpost.where(HydraOutpostPlan.GATE);
        BlockPos out = outpost.where(HydraOutpostPlan.GATE.relative(Direction.SOUTH, 1));
        int dx = Integer.signum(out.getX() - gate.getX());
        int dz = Integer.signum(out.getZ() - gate.getZ());
        int reach = outpost.real() ? ROAD : 4;
        BlockPos road = new BlockPos(gate.getX() + dx * reach, gate.getY(), gate.getZ() + dz * reach);
        if (outpost.real() && level.isLoaded(road)) {
            road = new BlockPos(road.getX(), surface(level, road), road.getZ());
        }
        return road;
    }

    /** The nearest player the soldiers of the outpost fight, or the nearest player within its reach. */
    @Nullable
    static LivingEntity nearestIntruder(ServerLevel level, HydraOutposts.Outpost outpost) {
        Vec3 centre = Vec3.atCenterOf(outpost.centre());
        Player best = null;
        double bestDistance = REACH * REACH;
        for (Player player : level.players()) {
            double d = player.distanceToSqr(centre);
            if (d < bestDistance && !HydraSenses.unseeable(player)) {
                best = player;
                bestDistance = d;
            }
        }
        return best;
    }

    /** The soldiers of the outpost that are loaded: its posts' and its reinforcements. */
    static List<HydraSoldierEntity> members(ServerLevel level, HydraOutposts.Outpost outpost) {
        return level.getEntitiesOfClass(HydraSoldierEntity.class, outpost.box().inflate(REACH),
                s -> s.isAlive() && s.outpost() == outpost);
    }

    /** Lights (or puts out) the alarm lamps: only where a redstone lamp still stands, and not one a circuit powers. */
    static void lamps(ServerLevel level, HydraOutposts.Outpost outpost, boolean lit) {
        for (BlockPos local : HydraOutpostPlan.ALARM_LAMPS) {
            BlockPos at = outpost.where(local);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (!state.is(Blocks.REDSTONE_LAMP) || state.getValue(RedstoneLampBlock.LIT) == lit) {
                continue;
            }
            if (!lit && level.hasNeighborSignal(at)) {
                continue;
            }
            level.setBlock(at, state.setValue(RedstoneLampBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
        if (outpost.lampsLit != lit) {
            outpost.lampsLit = lit;
            outpost.changed();
        }
    }

    private static void tell(ServerLevel level, HydraOutposts.Outpost outpost, String key) {
        Vec3 centre = Vec3.atCenterOf(outpost.centre());
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(centre) <= REACH * REACH) {
                player.displayClientMessage(Component.translatable(key), true);
            }
        }
    }

    /**
     * Tells the players near what the alarm does: {@code ALARM_ON} (the siren, a loop the client plays from the radio
     * console while it keeps hearing of it) or {@code ALARM_OFF}. The first point is the console, the rest the lamps.
     */
    private static void send(ServerLevel level, HydraOutposts.Outpost outpost, int kind) {
        List<Vec3> points = new ArrayList<>();
        points.add(Vec3.atCenterOf(outpost.where(HydraOutpostPlan.ALARM_CONSOLE)));
        for (BlockPos lamp : HydraOutpostPlan.ALARM_LAMPS) {
            points.add(Vec3.atCenterOf(outpost.where(lamp)));
        }
        Vec3 centre = Vec3.atCenterOf(outpost.centre());
        PacketDistributor.sendToPlayersNear(level, null, centre.x, centre.y, centre.z, REACH + 16.0,
                new HydraFxPayload(kind, -1, points, 0));
    }

    /**
     * Where a soldier badly hurt fighting {@code target} takes cover: beside the cover point of his outpost nearest
     * him (within 16 blocks), on its side away from the target; null if none is near or he has no outpost.
     */
    @Nullable
    static BlockPos coverFor(HydraSoldierEntity soldier, LivingEntity target) {
        HydraOutposts.Outpost outpost = soldier.outpost();
        if (outpost == null) {
            return null;
        }
        BlockPos best = null;
        double bestDistance = 16.0 * 16.0;
        for (BlockPos local : HydraOutpostPlan.COVER) {
            BlockPos cover = outpost.where(local);
            Vec3 away = Vec3.atCenterOf(cover).subtract(target.position());
            Direction side = Direction.getNearest(away.x, 0.0, away.z);
            BlockPos spot = cover.relative(side);
            if (!soldier.level().getBlockState(spot).isAir() && outpost.real()) {
                continue;
            }
            double d = soldier.distanceToSqr(Vec3.atBottomCenterOf(spot));
            if (d < bestDistance) {
                best = spot;
                bestDistance = d;
            }
        }
        return best;
    }

    /** Forgets a player's hold on a console (he left). */
    public static void forget(UUID player) {
        HOLDS.remove(player);
    }

    /** Forgets every hold and runner (the server stops, or a test ends). */
    public static void clear() {
        HOLDS.clear();
        RUN_SINCE.clear();
    }

    /** The surface y at a point (for the road). */
    static int surface(ServerLevel level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    }

    private HydraAlarm() {
    }
}
