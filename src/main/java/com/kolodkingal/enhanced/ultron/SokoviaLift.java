package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.network.SokoviaLiftPayload;
import com.kolodkingal.enhanced.ultron.SokoviaJournal.Header;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Sokovia lift, in its shipped mode, the <b>Sky Island</b>: the island is a copy of the ground, built in the sky,
 * and the real ground is never edited. Everyone on the ground disc is carried up with it, locked and safe; the clients
 * draw the island rising out of the place it was copied from; at the end of the rise the copy is placed in the sky, in
 * one batch of block edits (about 16,600 blocks, seven ticks at 2,500 a tick), and everyone is set down on it; when the
 * fight is over the copy is cleared, in one batch, and everyone is carried down and set down on the real ground at the
 * place they left it.
 *
 * <pre>
 *   warn      (200 ticks) the room is checked again; nothing changes
 *   lock      ( 20)       the island is copied into a template; everybody (a fighter, the boss, the Sentries, the key,
 *                         a hero's drones) is locked and carried; the clients are told
 *   rise      (liftTicks) the carried are moved up by what the island moves; at the end the journal is written and
 *                         the template is placed in the sky, a slice a tick; everyone is released on it
 *   island                the fight; the arena holds everyone on the island, and takes back a fighter who comes
 *                         to stand under it
 *   descent   (20 + clear + liftTicks) everything on the island is locked and carried; the island is cleared;
 *                         everything is carried down and set down on the ground; the journal is deleted
 * </pre>
 *
 * <p>If anything is wrong the fight goes on as the Tremor: the room above is not empty when the warning begins, the
 * chunks are not loaded, the mode is not Sky Island. A stop of the server puts everything back at once ({@link
 * #finish}), and a crash leaves at worst a floating island of ordinary blocks, which the next start clears ({@link
 * SokoviaRecovery}). Nothing here ever removes or changes a block of the ground.
 *
 * <p>A carried player is immune to everything but what bypasses invulnerability (the void, {@code /kill}), and for the
 * 40 ticks after they are set down to the fight's creatures and bolts ({@link SokoviaEvents}).
 */
public final class SokoviaLift implements LiftHook {
    /** The steps of a lift, in the order they happen. */
    public enum Step {
        /** The warning began and the room was there; nothing has changed. */
        WARNED,
        /** The island is copied and everybody is locked, waiting for the rise to begin. */
        LOCKED,
        RISING,
        /** The journal is written and the island is being placed in the sky. */
        PLACING,
        /** The island is in the sky and the fight goes on. */
        PLACED,
        /** The descent began: everything on the island is locked and carried. */
        DESCENT_LOCK,
        CLEARING,
        FALLING,
        /** Everything is as it was. */
        DONE,
        /** The lift could not happen and the fight goes on as the Tremor. */
        TREMOR
    }

    /** The clients are told the move is over this long after the last block of it. */
    public static final int END_DELAY = 2;
    /** After they are set down, the fight's creatures cannot hurt a player for this long. */
    public static final int GRACE_TICKS = 40;
    /** How long the pit's tractor takes to carry one fighter up. */
    public static final int TRACTOR_TICKS = 60;
    /** How long a fighter must stand on the ground under the island for the tractor to take them. */
    public static final int TRACTOR_DWELL = 40;
    /** The island is looked after (the rim, the tractor) this often, in ticks. */
    private static final int CHECK = 5;
    /** The fight's rim field takes back a fighter this far past the rim. */
    private static final double RIM_FIELD = 4.0;
    /** ... or this far under the cone's tip. */
    private static final double FALL_FIELD = 3.0;
    /** ... or this high over the island's top. */
    private static final double SKY_FIELD = 80.0;

    private static final Map<UUID, Lift> LIFTS = new LinkedHashMap<>();

    // ---------------------------------------------------------------------------------------------------------
    // The hook

    @Nullable
    @Override
    public Component refusal(ServerLevel level, BlockPos centre, int radius, @Nullable ServerPlayer starter) {
        return SokoviaValidator.refusal(level, Spec.ofConfig(0L), centre, starter);
    }

    @Nullable
    @Override
    public Component refusal(ServerLevel level, BlockPos centre, int radius) {
        return refusal(level, centre, radius, null);
    }

    @Override
    public boolean warn(UltronFight fight) {
        if (UltronBossConfig.LIFT_MODE.get() != UltronBossConfig.LiftMode.SKY_ISLAND) {
            return false; // (CUT_GROUND is not in this release)
        }
        Lift old = LIFTS.remove(fight.id());
        if (old != null) {
            old.finishNow();
        }
        Lift lift = new Lift(fight);
        Component why = lift.room();
        if (why != null) {
            Enhanced.LOGGER.info("Ultron's island cannot rise: {}", why.getString());
            return false;
        }
        LIFTS.put(fight.id(), lift);
        return true;
    }

    @Override
    public void lock(UltronFight fight) {
        Lift lift = LIFTS.get(fight.id());
        if (lift != null) {
            lift.lock();
        }
    }

    @Override
    public boolean rise(UltronFight fight, int tick) {
        Lift lift = LIFTS.get(fight.id());
        if (lift == null || lift.step == Step.TREMOR || lift.step == Step.DONE) {
            return tick >= UltronBossConfig.LIFT_TICKS.get(); // (time passes as in the Tremor)
        }
        return lift.rise(tick);
    }

    @Override
    public void beginDescent(UltronFight fight) {
        Lift lift = LIFTS.get(fight.id());
        if (lift != null) {
            lift.beginDescent();
        }
    }

    @Override
    public boolean descend(UltronFight fight, int tick) {
        Lift lift = LIFTS.get(fight.id());
        return lift == null || lift.descend(tick);
    }

    @Override
    public void tick(UltronFight fight) {
        Lift lift = LIFTS.get(fight.id());
        if (lift != null) {
            lift.tick();
        }
    }

    @Override
    public void finish(UltronFight fight, boolean forced) {
        Lift lift = LIFTS.remove(fight.id());
        if (lift != null) {
            lift.finishNow();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the rest of the mod asks

    /** The lift of the fight, or null (the fight has none: the Tremor, or it has not reached the warning). */
    @Nullable
    public static Lift of(UltronFight fight) {
        return LIFTS.get(fight.id());
    }

    /** Whether any lift is running on the server (a cheap check for the events that look at carried players). */
    public static boolean any() {
        return !LIFTS.isEmpty();
    }

    /** Whether the entity is locked and carried by a lift now. */
    public static boolean isCarried(Entity entity) {
        if (LIFTS.isEmpty()) {
            return false;
        }
        UUID fight = SokoviaCarry.markOf(entity);
        Lift lift = fight == null ? null : LIFTS.get(fight);
        return lift != null && lift.carries(entity.getUUID());
    }

    /** Whether the fight's creatures and bolts must not hurt this player: they were just set down. */
    public static boolean inGrace(ServerPlayer player) {
        if (LIFTS.isEmpty()) {
            return false;
        }
        for (Lift lift : LIFTS.values()) {
            Long until = lift.grace.get(player.getUUID());
            if (until != null && player.level().getGameTime() < until) {
                return true;
            }
        }
        return false;
    }

    /** Forgets every lift (the server has stopped): the world went with it. */
    public static void clear() {
        LIFTS.clear();
    }

    /** The lifts that run (for the operators and the tests). */
    public static List<Lift> all() {
        return List.copyOf(LIFTS.values());
    }

    /**
     * A player comes within sight of the island's key: the state of the lift is sent to them, so that they see the
     * island moving, or the ground gone from under it.
     */
    public static void tellNewcomer(ServerPlayer player, Entity key) {
        for (Lift lift : LIFTS.values()) {
            if (lift.level == player.level() && lift.fight.key() == key) {
                SokoviaLiftPayload payload = lift.currentPayload();
                if (payload != null) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
    }

    /**
     * A player logs in: a player saved while they were carried (a crash, a stop) is let go, and one saved in the sky
     * where the island no longer is, is set down on the ground (see {@link SokoviaRecovery#onLogin}). One who logs in
     * while their fight still carries or holds the island is put back with it.
     */
    public static void onLogin(ServerPlayer player) {
        for (Lift lift : LIFTS.values()) {
            lift.welcomeBack(player);
        }
    }

    /** A player logs out while the island carries them: they are let go where they are and remembered. */
    public static void onLogout(ServerPlayer player) {
        for (Lift lift : LIFTS.values()) {
            lift.goodbye(player);
        }
    }

    /**
     * A player died: they are no longer on the island, and no longer carried. When they come back they are on the
     * ground like a latecomer, and the pit's tractor (which counts how often it takes them) is what brings them up.
     */
    public static void onDeath(ServerPlayer player) {
        for (Lift lift : LIFTS.values()) {
            lift.islanders.remove(player.getUUID());
            lift.dwell.remove(player.getUUID());
            Tractor tractor = lift.tractors.remove(player.getUUID());
            if (tractor != null) {
                tractor.carry.releaseAll();
            }
            lift.carry.unbind(player.getUUID());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // One lift

    /** A fighter that the pit's tractor is carrying up to the island. */
    private static final class Tractor {
        final SokoviaCarry carry;
        int ticks;

        Tractor(SokoviaCarry carry) {
            this.carry = carry;
        }
    }

    /** The lift of one fight: its template, what it carries, the state of the move. */
    public static final class Lift {
        private final UltronFight fight;
        private final ServerLevel level;
        private final Spec spec;
        private final BlockPos centre;
        private final SokoviaCarry carry;
        private final Map<UUID, Tractor> tractors = new HashMap<>();
        private final Set<UUID> islanders = new HashSet<>();
        private final Set<UUID> absent = new HashSet<>();
        private final Map<UUID, Long> rescuedAt = new HashMap<>();
        private final Map<UUID, Integer> dwell = new HashMap<>();
        private final Map<UUID, Integer> rejoins = new HashMap<>();
        private final Map<UUID, Long> grace = new HashMap<>();
        @Nullable
        private SokoviaTemplate template;
        private Step step = Step.WARNED;
        @Nullable
        private SokoviaJob job;
        @Nullable
        private Header header;
        /** How far up the island is now (0 until it rises, the lift's height once it is placed). */
        private double altitude;
        /** Whether the island is on its way down, or about to be (the carried move by negative amounts). */
        private boolean descending;
        private long moveStart;
        private int fallTicks;
        private long endAt = -1;
        private boolean endKeepsGround;

        private Lift(UltronFight fight) {
            this.fight = fight;
            this.level = fight.level();
            long seed = fight.id().getMostSignificantBits() ^ fight.id().getLeastSignificantBits();
            this.spec = Spec.ofConfig(seed);
            this.centre = BlockPos.containing(fight.arena().center());
            this.carry = new SokoviaCarry(level, fight.id());
        }

        // ---- what it is ----

        public Step step() {
            return step;
        }

        public Spec spec() {
            return spec;
        }

        public BlockPos centre() {
            return centre;
        }

        /** The island's blocks, once it has been copied (at the lock). */
        @Nullable
        public SokoviaTemplate template() {
            return template;
        }

        /** How far up the island is now. */
        public double altitude() {
            return altitude;
        }

        /** The job that places or clears the island, while there is one. */
        @Nullable
        public SokoviaJob job() {
            return job;
        }

        /** The note on disk, as last written, or null if there is none. */
        @Nullable
        public Header header() {
            return header;
        }

        public boolean carries(UUID id) {
            return carry.contains(id) || tractors.containsKey(id);
        }

        /** The ids of everything locked and carried by the main move. */
        public List<UUID> carried() {
            return carry.riders().stream().map(r -> r.id).toList();
        }

        /** The players who stand on the island (set down on it, or taken up by the tractor). */
        public Set<UUID> islanders() {
            return Set.copyOf(islanders);
        }

        public UltronFight fight() {
            return fight;
        }

        // ---- the warning ----

        /** Whether the room is there: null if so, else why not. */
        @Nullable
        Component room() {
            return SokoviaValidator.refusal(level, spec, centre, null);
        }

        // ---- the lock ----

        void lock() {
            if (step != Step.WARNED) {
                return;
            }
            Component why = room();
            if (why != null) {
                Enhanced.LOGGER.warn("Ultron's island cannot rise after all: {}", why.getString());
                step = Step.TREMOR;
                return;
            }
            template = SokoviaTemplate.capture(level, spec, centre);
            carry.restart();
            collectAscent();
            moveStart = level.getGameTime() + UltronFight.LOCK_TICKS;
            send(SokoviaLiftPayload.RISE, moveStart, UltronBossConfig.HIDE_GROUND.get());
            step = Step.LOCKED;
        }

        /**
         * Locks everything that goes up: the fighters (a stray one is brought in), the fight's creatures, the drones.
         */
        private void collectAscent() {
            for (ServerPlayer player : fight.present()) {
                if (!inRegion(player.position(), 0.0)) {
                    bringIn(player, 0.0);
                }
                carry.bind(player, false);
            }
            for (Entity entity : fight.entities()) {
                if (entity instanceof UltronBoltEntity) {
                    entity.discard(); // (a bolt in the air cannot be carried)
                } else {
                    carry.bind(entity, true);
                }
            }
            for (UltronSentryEntity drone : drones()) {
                carry.bind(drone, true);
            }
            for (UltronSentryEntity sentry : fight.sentries()) {
                sentry.setHeld(true);
            }
        }

        private List<UltronSentryEntity> drones() {
            AABB box = regionBox(altitude).inflate(2.0);
            return level.getEntitiesOfClass(UltronSentryEntity.class, box,
                    s -> s.isAlive() && s.getOwnerUUID() != null && fight.isParticipant(s.getOwnerUUID()));
        }

        /**
         * Puts a fighter who is not on the island on its surface: where they stand if that is inside the rim, or the
         * nearest point inside it.
         */
        private void bringIn(ServerPlayer player, double up) {
            Vec3 middle = fight.arena().center();
            double dx = player.getX() - middle.x;
            double dz = player.getZ() - middle.z;
            double angle = dx == 0.0 && dz == 0.0 ? 0.0 : Math.atan2(dz, dx);
            // (Whoever is inside the rim stays where they are; whoever is outside is put just inside it.)
            double reach = dx == 0.0 && dz == 0.0 ? 0.0 : Math.min(Math.hypot(dx, dz), Math.max(1.0, SokoviaShape.rim(spec, angle) - 2.0));
            double x = middle.x + Math.cos(angle) * reach;
            double z = middle.z + Math.sin(angle) * reach;
            Vec3 spot = new Vec3(x, surfaceAt(x, z) + up, z);
            SokoviaCarry.teleport(player, SokoviaCarry.freeSpot(player, spot));
        }

        /** The y of the standing surface over the column of this point, on the ground (the top of its ground block). */
        private double surfaceAt(double x, double z) {
            SokoviaTemplate t = template;
            int ground = t == null ? SokoviaTemplate.NO_GROUND : t.groundAt(Mth.floor(x) - centre.getX(), Mth.floor(z) - centre.getZ());
            return ground == SokoviaTemplate.NO_GROUND ? centre.getY() : ground + 1.0;
        }

        /** Whether the point is on the island's ground disc (or its disc in the sky, {@code up} blocks higher). */
        private boolean inRegion(Vec3 point, double up) {
            SokoviaTemplate t = template;
            Vec3 middle = fight.arena().center();
            double dx = point.x - middle.x;
            double dz = point.z - middle.z;
            double rim = SokoviaShape.rim(spec, Math.atan2(dz, dx)) + SokoviaValidator.CYLINDER_MARGIN;
            if (Math.hypot(dx, dz) > rim || t == null) {
                return false;
            }
            return point.y >= t.lowestGround() - spec.skin() + up && point.y <= t.highestGround() + spec.cover() + 10 + up;
        }

        private AABB regionBox(double up) {
            SokoviaTemplate t = template;
            double low = (t == null ? centre.getY() : t.lowestGround()) - spec.skin() + up;
            double high = (t == null ? centre.getY() : t.highestGround()) + spec.cover() + 10 + up;
            int reach = spec.extent() + 2;
            Vec3 middle = fight.arena().center();
            return new AABB(middle.x - reach, low, middle.z - reach, middle.x + reach, high, middle.z + reach);
        }

        // ---- the rise ----

        boolean rise(int tick) {
            int ticks = Math.max(1, UltronBossConfig.LIFT_TICKS.get());
            if (step == Step.LOCKED) {
                step = Step.RISING;
            }
            if (step == Step.RISING) {
                altitude = SokoviaShape.offset(spec.height(), ticks, tick);
                fight.island().setIslandOffset(altitude);
                if (tick >= ticks) {
                    altitude = spec.height();
                    beginPlacing();
                }
            }
            // (The blocks are placed by this lift's own tick, which also runs when the fight is over.)
            return step == Step.PLACED || step == Step.TREMOR;
        }

        /** The rise is over: the note is written, then the first block of the island is placed. */
        private void beginPlacing() {
            SokoviaTemplate t = template;
            if (t == null) {
                step = Step.TREMOR;
                return;
            }
            header = new Header(fight.id(), level.dimension(), centre, spec, t.minY() + spec.height(), t.roomTop() + spec.height(),
                    SokoviaJournal.PASTED, fight.starter(), fight.programUsed(), memberIds());
            try {
                SokoviaJournal.write(level.getServer(), header);
            } catch (IOException e) {
                // No note, no island: nothing was changed. Everyone is set down where they were, and the fight goes on
                // as the Tremor.
                Enhanced.LOGGER.error("Ultron's island cannot be placed: its journal cannot be written", e);
                header = null;
                setDownInPlace();
                step = Step.TREMOR;
                return;
            }
            job = SokoviaJob.paste(level, t, spec.height());
            step = Step.PLACING;
        }

        /** The island is in the sky: everyone is set down on it, free of the blocks, and the clients are told. */
        private void placed() {
            carry.move(spec.height());
            carry.snapAll();
            long now = level.getGameTime();
            for (SokoviaCarry.Rider rider : carry.riders()) {
                if (rider.player) {
                    islanders.add(rider.id);
                    grace.put(rider.id, now + GRACE_TICKS);
                }
            }
            carry.releaseAll();
            SokoviaTemplate t = template;
            fight.island().raiseIsland(spec.height(), t == null ? centre.getY() : t.minY() + spec.height());
            job = null;
            step = Step.PLACED;
            askToEnd(true);
        }

        /** The clients are told, a couple of ticks on (the blocks reach them first), that the mesh is done. */
        private void askToEnd(boolean groundStaysHidden) {
            endAt = level.getGameTime() + END_DELAY;
            endKeepsGround = groundStaysHidden;
        }

        // ---- the descent ----

        void beginDescent() {
            switch (step) {
                case PLACED, PLACING -> startDescent();
                case LOCKED, RISING -> {
                    setDownInPlace();
                    step = Step.DONE;
                }
                case WARNED -> {
                    // The fight ended during the warning: nothing was copied, nothing is carried, nothing is to be lowered.
                    // (Left as it is, the descent would never be over and the fight would hold its fighters for five minutes.)
                    step = Step.DONE;
                }
                default -> {
                }
            }
        }

        private void startDescent() {
            SokoviaTemplate t = template;
            if (t == null) {
                step = Step.DONE;
                return;
            }
            fight.tell(Component.translatable("message.enhanced.ultron.lift.down").withStyle(ChatFormatting.YELLOW), true);
            double high = spec.height();
            if (job != null) {
                job.finishNow(); // (a placing that was cut short is finished before it comes down)
                job = null;
            }
            descending = true;
            carry.restart();
            for (ServerPlayer player : fight.present()) {
                if (!inRegion(player.position(), high)) {
                    bringIn(player, high);
                }
            }
            for (Entity entity : islandEntities(high)) {
                carry.bind(entity, entity instanceof UltronEntity || entity instanceof UltronSentryEntity);
            }
            for (UltronSentryEntity sentry : fight.sentries()) {
                sentry.setHeld(true);
            }
            islanders.clear();
            moveStart = Long.MAX_VALUE;
            fallTicks = 0;
            send(SokoviaLiftPayload.LOWER, Long.MAX_VALUE, UltronBossConfig.HIDE_GROUND.get());
            step = Step.DESCENT_LOCK;
        }

        /** Everything alive or lying on the island, as it stands at {@code up} blocks over the ground. */
        private List<Entity> islandEntities(double up) {
            List<Entity> found = new ArrayList<>();
            for (Entity entity : level.getEntities((Entity) null, regionBox(up), e -> e.isAlive() && carriable(e))) {
                if (inRegion(entity.position(), up)) {
                    found.add(entity);
                }
            }
            return found;
        }

        private static boolean carriable(Entity entity) {
            if (entity instanceof ServerPlayer player) {
                return !player.isSpectator();
            }
            return entity instanceof LivingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof VibraniumKeyEntity;
        }

        boolean descend(int tick) {
            switch (step) {
                case DESCENT_LOCK -> {
                    if (tick >= UltronFight.LOCK_TICKS) {
                        beginClearing();
                    }
                }
                default -> {
                }
            }
            if (step == Step.CLEARING) {
                SokoviaJob clearing = job;
                if (clearing != null && clearing.step()) {
                    job = null;
                    beginFalling();
                }
            } else if (step == Step.FALLING) {
                fallTicks++;
                int ticks = Math.max(1, UltronBossConfig.LIFT_TICKS.get());
                double moved = -SokoviaShape.offset(spec.height(), ticks, fallTicks);
                altitude = spec.height() + moved;
                fight.island().setIslandOffset(altitude);
                if (fallTicks >= ticks) {
                    landed();
                    return true;
                }
            }
            return step == Step.DONE || step == Step.TREMOR;
        }

        private void beginClearing() {
            SokoviaTemplate t = template;
            if (t == null) {
                step = Step.DONE;
                return;
            }
            Header note = header != null ? header.withState(SokoviaJournal.CLEARING) : null;
            if (note != null) {
                try {
                    SokoviaJournal.write(level.getServer(), note);
                    header = note;
                } catch (IOException e) {
                    Enhanced.LOGGER.error("Ultron's journal could not be updated", e);
                }
            }
            job = SokoviaJob.clear(level, spec, centre, t.minY() + spec.height(), t.roomTop() + spec.height());
            step = Step.CLEARING;
        }

        private void beginFalling() {
            moveStart = level.getGameTime() + 1;
            fallTicks = 0;
            carry.restart();
            send(SokoviaLiftPayload.LOWER, moveStart, UltronBossConfig.HIDE_GROUND.get());
            step = Step.FALLING;
        }

        /** The island is down: everything is set on the ground, the note is deleted, the clients are told. */
        private void landed() {
            carry.move(-spec.height());
            carry.snapAll();
            long now = level.getGameTime();
            for (SokoviaCarry.Rider rider : carry.riders()) {
                if (rider.player) {
                    grace.put(rider.id, now + GRACE_TICKS);
                }
            }
            carry.releaseAll();
            altitude = 0.0;
            fight.island().landIsland();
            send(SokoviaLiftPayload.END, now, false);
            endAt = -1;
            deleteNote();
            sendAbsentHome();
            step = Step.DONE;
        }

        // ---- every tick ----

        void tick() {
            if (step == Step.DONE || step == Step.TREMOR || step == Step.WARNED) {
                return;
            }
            if (header != null && header.refund() && fight.isOver()) {
                settleTheProgram();
            }
            if (fight.isOver() && (step == Step.LOCKED || step == Step.RISING)) {
                // The fight ended before the island was placed (an operator, a defeat): nobody is left hanging in the
                // air.
                setDownInPlace();
                step = Step.DONE;
                return;
            }
            if (step == Step.PLACING) {
                SokoviaJob placing = job;
                if (placing != null && placing.step()) {
                    placed();
                }
            }
            moveCarry();
            if (endAt >= 0 && level.getGameTime() >= endAt) {
                endAt = -1;
                send(SokoviaLiftPayload.END, level.getGameTime(), endKeepsGround);
            }
            if (step == Step.PLACED) {
                keepTheIsland();
            }
            for (var it = tractors.values().iterator(); it.hasNext(); ) {
                Tractor tractor = it.next();
                if (tractor.carry.riders().isEmpty()) {
                    it.remove();
                    continue;
                }
                tractor.ticks++;
                tractor.carry.move(SokoviaShape.offset(spec.height(), TRACTOR_TICKS, tractor.ticks));
                if (tractor.ticks >= TRACTOR_TICKS) {
                    finishTractor(tractor);
                    it.remove();
                }
            }
            if (level.getGameTime() % 20 == 0) {
                SokoviaRecovery.sweepStale(level);
            }
        }

        /**
         * The fight is over, so its Program is settled (it was given back, or the fight was won or lost and none is owed):
         * the note stops saying it is owed, or a crash during the wind-down would give a second one on top of the reward.
         */
        private void settleTheProgram() {
            Header settled = header.withoutRefund();
            header = settled;
            try {
                SokoviaJournal.write(level.getServer(), settled);
            } catch (IOException e) {
                Enhanced.LOGGER.error("Ultron's journal could not be updated", e);
            }
        }

        /** Carries what is carried by what the island has moved so far in the move that is on. */
        private void moveCarry() {
            double moved = switch (step) {
                case RISING -> altitude;
                case PLACING, PLACED -> spec.height();
                case FALLING -> altitude - spec.height();
                default -> 0.0;
            };
            carry.move(moved);
        }

        // ---- while the island is up ----

        /** The rim field, the pit's tractor, and the note's list of who was there. */
        private void keepTheIsland() {
            long now = level.getGameTime();
            if (now % CHECK != 0) {
                return;
            }
            SokoviaTemplate t = template;
            if (t == null) {
                return;
            }
            double lift = spec.height();
            for (ServerPlayer player : fight.fighters()) {
                UUID id = player.getUUID();
                if (carries(id)) {
                    continue;
                }
                if (islanders.contains(id) || player.getY() >= t.minY() + lift - 4.0) {
                    islanders.add(id);
                    Vec3 middle = fight.arena().center();
                    double dx = player.getX() - middle.x;
                    double dz = player.getZ() - middle.z;
                    double rim = SokoviaShape.rim(spec, Math.atan2(dz, dx));
                    boolean out = Math.hypot(dx, dz) > rim + RIM_FIELD || player.getY() < t.minY() + lift - FALL_FIELD
                            || player.getY() > t.maxY() + lift + SKY_FIELD;
                    if (out) {
                        rescue(player, now);
                    }
                } else {
                    underTheIsland(player);
                }
            }
            if (header != null && header.members().size() != fight.participants().size()) {
                try {
                    header = header.withMembers(memberIds());
                    SokoviaJournal.write(level.getServer(), header);
                } catch (IOException e) {
                    Enhanced.LOGGER.error("Ultron's journal could not be updated", e);
                }
            }
        }

        /** A fighter who left the island is put back on it: 2 damage that cannot kill, a few seconds of Slowness. */
        private void rescue(ServerPlayer player, long now) {
            Long last = rescuedAt.get(player.getUUID());
            if (last != null && now - last < BossArena.FIELD_COOLDOWN) {
                return;
            }
            rescuedAt.put(player.getUUID(), now);
            Vec3 spot = fight.island().islandSpot(player);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.6, 0.3, 0.05);
            SokoviaCarry.teleport(player, SokoviaCarry.freeSpot(player, spot));
            player.resetFallDistance();
            player.connection.aboveGroundTickCount = 0;
            float damage = (float) (double) UltronBossConfig.RESCUE_DAMAGE.get();
            float amount = Math.min(damage, player.getHealth() - 1.0F);
            if (!player.isCreative() && amount > 0.0F) {
                player.hurt(level.damageSources().generic(), amount);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            player.displayClientMessage(Component.translatable("message.enhanced.boss.pushed"), true);
        }

        /**
         * A fighter on the ground under the island: after a while the tractor takes them up (at most {@code
         * rejoinLimit} times).
         */
        private void underTheIsland(ServerPlayer player) {
            SokoviaTemplate t = template;
            UUID id = player.getUUID();
            if (t == null) {
                return;
            }
            if (!inRegion(player.position(), 0.0) || tractors.containsKey(id)) {
                dwell.remove(id);
                return;
            }
            int limit = UltronBossConfig.REJOIN_LIMIT.get();
            if (rejoins.getOrDefault(id, 0) >= limit) {
                return;
            }
            int waited = dwell.merge(id, CHECK, Integer::sum);
            if (waited >= TRACTOR_DWELL) {
                dwell.remove(id);
                rejoins.merge(id, 1, Integer::sum);
                SokoviaCarry one = new SokoviaCarry(level, fight.id());
                one.bind(player, false);
                tractors.put(id, new Tractor(one));
            }
        }

        private void finishTractor(Tractor tractor) {
            tractor.carry.move(spec.height());
            tractor.carry.snapAll();
            long now = level.getGameTime();
            for (SokoviaCarry.Rider rider : tractor.carry.riders()) {
                islanders.add(rider.id);
                grace.put(rider.id, now + GRACE_TICKS);
            }
            tractor.carry.releaseAll();
        }

        // ---- a player comes and goes ----

        /**
         * A member who logs out is let go where they are and remembered; they are set down on login if the island went.
         */
        void goodbye(ServerPlayer player) {
            UUID id = player.getUUID();
            boolean was = carry.contains(id) || tractors.containsKey(id);
            if (was) {
                Tractor tractor = tractors.remove(id);
                if (tractor != null) {
                    tractor.carry.releaseAll();
                }
                carry.unbind(id);
            }
            if (was || islanders.contains(id)) {
                absent.add(id);
            }
        }

        void welcomeBack(ServerPlayer player) {
            UUID id = player.getUUID();
            if (!absent.remove(id) || step == Step.DONE || step == Step.TREMOR || step == Step.WARNED) {
                return;
            }
            SokoviaTemplate t = template;
            if (t == null) {
                return;
            }
            switch (step) {
                case LOCKED, RISING -> {
                    // They were up with the rest and the island has gone on rising without them: back among it.
                    bringIn(player, altitude);
                    carry.bind(player, false);
                }
                case PLACING, PLACED -> {
                    // They stand on the island if they were on it; one who was saved on the way up (they left during
                    // the rise) is put on it now.
                    if (player.getY() < t.lowestGround() + spec.height() - 3.0) {
                        bringIn(player, spec.height());
                    }
                    islanders.add(id);
                }
                default -> {
                    // The island is coming down or gone: set them on the ground.
                    SokoviaRecovery.setDown(player, column(t));
                }
            }
        }

        // ---- the end ----

        /**
         * Puts everything back at once: the fight is gone (the server is stopping, an operator cleans up, a fight that
         * failed). The island is cleared whole, everything that was on it is set down on the ground below, the note is
         * deleted, the clients are told. Safe to call in any step, and again.
         */
        void finishNow() {
            switch (step) {
                case LOCKED, RISING -> setDownInPlace();
                case PLACING, PLACED, DESCENT_LOCK, CLEARING, FALLING -> restoreEverything();
                default -> {
                }
            }
            for (Tractor tractor : tractors.values()) {
                tractor.carry.releaseAll();
            }
            tractors.clear();
            carry.releaseAll();
            if (step != Step.DONE && step != Step.TREMOR && step != Step.WARNED) {
                fight.island().landIsland();
                send(SokoviaLiftPayload.END, level.getGameTime(), false);
            }
            deleteNote();
            sendAbsentHome();
            if (step != Step.TREMOR && step != Step.WARNED) {
                step = Step.DONE;
            }
        }

        /** The rise was cut short: nothing was placed, so everyone is simply set down where they were taken from. */
        private void setDownInPlace() {
            carry.move(0.0);
            carry.snapAll();
            long now = level.getGameTime();
            for (SokoviaCarry.Rider rider : carry.riders()) {
                if (rider.player) {
                    grace.put(rider.id, now + GRACE_TICKS);
                }
            }
            carry.releaseAll();
            altitude = 0.0;
            fight.island().landIsland();
            send(SokoviaLiftPayload.END, level.getGameTime(), false);
        }

        /**
         * The island is in the sky (whole, or half placed, or half taken down) and must go now: everything on it, and
         * everything it was carrying, is set on the ground below, then every block of the island is cleared.
         */
        private void restoreEverything() {
            double high = spec.height();
            double now = descending ? high + carry.moved() : high;
            List<Entity> down = new ArrayList<>(islandEntities(now));
            for (Entity entity : carry.entities()) {
                if (!down.contains(entity)) {
                    down.add(entity);
                }
            }
            carry.releaseAll();
            SokoviaTemplate t = template;
            for (Entity entity : down) {
                Vec3 spot = new Vec3(entity.getX(), entity.getY() - now, entity.getZ());
                SokoviaCarry.release(entity);
                SokoviaCarry.teleport(entity, SokoviaCarry.freeSpot(entity, spot));
                if (entity instanceof ServerPlayer player) {
                    grace.put(player.getUUID(), level.getGameTime() + GRACE_TICKS);
                }
            }
            if (t != null) {
                // (A placing that was cut short is dropped: what it did place is cleared with the rest.)
                SokoviaJob clearing = job != null && !job.isPasting() ? job : SokoviaJob.clear(level, spec, centre, t.minY() + spec.height(), t.roomTop() + spec.height());
                clearing.finishNow();
            }
            job = null;
            altitude = 0.0;
        }

        private void deleteNote() {
            if (header != null) {
                SokoviaJournal.delete(level.getServer(), header.fight());
                header = null;
            }
        }

        /** The column of the island, from its bottom on the ground to above its place in the sky (for the notes). */
        private SokoviaRelocations.Entry column(SokoviaTemplate t) {
            Vec3 middle = fight.arena().center();
            return new SokoviaRelocations.Entry(middle.x, middle.z, spec.extent() + 2.0, t.minY(),
                    t.roomTop() + spec.height() + SKY_FIELD, spec.height(), t.lowestGround() + spec.height() - 3.0);
        }

        /**
         * The members who are offline when the island goes get a note to be set down on the ground when they log in.
         */
        private void sendAbsentHome() {
            if (absent.isEmpty()) {
                return;
            }
            SokoviaTemplate t = template;
            if (t == null) {
                absent.clear();
                return;
            }
            SokoviaRelocations.Entry entry = column(t);
            SokoviaRelocations data = SokoviaRelocations.of(level.getServer());
            for (UUID id : absent) {
                data = data.with(id, entry);
            }
            SokoviaRelocations.store(level.getServer(), data);
            absent.clear();
        }

        // ---- the clients ----

        /** Tells every player in the dimension (the payload is about 70 bytes). */
        private void send(int phase, long start, boolean hide) {
            PacketDistributor.sendToPlayersInDimension(level, payload(phase, start, hide));
        }

        private SokoviaLiftPayload payload(int phase, long start, boolean hide) {
            VibraniumKeyEntity key = fight.key();
            return new SokoviaLiftPayload(phase, key == null ? -1 : key.getId(), centre, spec.radius(), spec.height(), spec.cone(), spec.seed(),
                    Math.max(1, UltronBossConfig.LIFT_TICKS.get()), spec.skin(), spec.cover(), hide, start);
        }

        /** What a client that comes in now needs to be told, or null if there is nothing to see. */
        @Nullable
        SokoviaLiftPayload currentPayload() {
            return switch (step) {
                case LOCKED, RISING -> payload(SokoviaLiftPayload.RISE, moveStart, UltronBossConfig.HIDE_GROUND.get());
                case PLACING, PLACED -> payload(SokoviaLiftPayload.END, level.getGameTime(), UltronBossConfig.HIDE_GROUND.get());
                case DESCENT_LOCK, CLEARING -> payload(SokoviaLiftPayload.LOWER, Long.MAX_VALUE, UltronBossConfig.HIDE_GROUND.get());
                case FALLING -> payload(SokoviaLiftPayload.LOWER, moveStart, UltronBossConfig.HIDE_GROUND.get());
                default -> null;
            };
        }

        private List<UUID> memberIds() {
            return fight.participants().stream().map(p -> p.id()).toList();
        }
    }

    /** Whether the damage came from a creature or a bolt of a fight (what the 40 ticks of grace guard against). */
    public static boolean fromAFight(DamageSource source) {
        Entity cause = source.getEntity();
        Entity direct = source.getDirectEntity();
        return (cause != null && BossFights.fightOf(cause) != null) || (direct != null && BossFights.fightOf(direct) != null);
    }
}
