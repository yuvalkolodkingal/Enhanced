package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.FeastHall;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The goals of the Asgardians (3.5): their idle life and their part in a fight. Every goal that walks recomputes its path
 * at most once a second (the city may hold 40 of them); the pairs (talking, sparring) are led by one of the two, who sets
 * both states, and the other follows by his own goal of the same kind.
 */
final class AsgardianGoals {
    /**
     * How many ticks at least between two path computations of an idle goal. Every "every N ticks" gate here counts
     * {@code tickCount + id}: a mob asks its goals (and ticks most running ones) only on ticks where that sum is even
     * ({@code Mob.serverAiStep}), so a gate on {@code tickCount} alone never opens for half of the residents.
     */
    static final int REPATH = 20;

    private AsgardianGoals() {
    }

    /** Index of a slot ({@code EINHERJAR#5} → 5; a named one 0; none -1). */
    static int slotIndex(AsgardianEntity npc) {
        String slot = npc.slot();
        int hash = slot.indexOf('#');
        if (slot.isEmpty()) {
            return -1;
        }
        try {
            return hash < 0 ? 0 : Integer.parseInt(slot.substring(hash + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Einherjar 0-3 stand guard at their homes (the bridge gate and the throne room door); the others patrol. */
    static boolean guard(AsgardianEntity npc) {
        int index = slotIndex(npc);
        return npc.role() == Role.EINHERJAR && index >= 0 && index < 4;
    }

    static boolean patroller(AsgardianEntity npc) {
        return npc.role() == Role.EINHERJAR && slotIndex(npc) >= 4 && !CityPlan.patrols().isEmpty();
    }

    /** Faces {@code target} at once (body and head). */
    static void face(Mob mob, Vec3 target) {
        double dx = target.x - mob.getX();
        double dz = target.z - mob.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        mob.setYRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);
    }

    /** Where an Asgardian stands on a seat: on top of whatever the seat block is (a bench, a slab), in its middle. */
    static Vec3 seatTop(ServerLevel level, BlockPos seat) {
        BlockState state = level.getBlockState(seat);
        double top = state.getCollisionShape(level, seat).isEmpty() ? 0.0 : state.getCollisionShape(level, seat).max(Direction.Axis.Y);
        return new Vec3(seat.getX() + 0.5, seat.getY() + top, seat.getZ() + 0.5);
    }

    /** The persistent tags of the way a seated Asgardian faces (his seat's front) and where he sits (3.7.2). */
    static final String SEAT_YAW = "enhanced:asgard_seat_yaw";
    static final String SEAT_AT = "enhanced:asgard_seat_at";

    /**
     * Where a sitter sits on a seat that faces {@code facing}: on top of it, a little back from its front (so a diner's
     * knees stay out of his table).
     */
    static Vec3 sitAt(ServerLevel level, BlockPos seat, Direction facing) {
        return seatTop(level, seat).subtract(facing.getStepX() * 0.12, 0.0, facing.getStepZ() * 0.12);
    }

    /**
     * Sits him down at {@code at}, facing {@code facing} (kept by {@link Seated}), in {@code state} for {@code ticks}.
     */
    static void sitDown(AsgardianEntity npc, Vec3 at, Direction facing, NpcState state, int ticks) {
        float yaw = facing.toYRot();
        npc.getNavigation().stop();
        npc.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        ModAttachments.PERSISTENT_DATA.get(npc).putFloat(SEAT_YAW, yaw);
        net.minecraft.nbt.ListTag where = new net.minecraft.nbt.ListTag();
        where.add(net.minecraft.nbt.DoubleTag.valueOf(at.x));
        where.add(net.minecraft.nbt.DoubleTag.valueOf(at.y));
        where.add(net.minecraft.nbt.DoubleTag.valueOf(at.z));
        ModAttachments.PERSISTENT_DATA.get(npc).put(SEAT_AT, where);
        npc.setState(state, ticks);
    }

    /** Whether someone (a living thing, or a player on a seat) is on the seat block {@code seat}. */
    static boolean taken(AsgardianEntity npc, BlockPos seat) {
        AABB around = new AABB(seat).inflate(0.05, 0.6, 0.05);
        return !npc.level().getEntities(npc, around, e -> e instanceof LivingEntity && e.isAlive()
                || e instanceof com.kolodkingal.enhanced.asgard.SeatEntity && e.isVehicle()).isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------------

    /** Keeps him still while he sleeps, kneels, bows or talks to a player; he looks at whoever he is with. */
    static final class HoldStill extends Goal {
        private final AsgardianEntity npc;

        HoldStill(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            NpcState state = npc.state();
            return state.still() || state == NpcState.TALKING && npc.talkingWith() != null;
        }

        @Override
        public void start() {
            npc.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Entity partner = npc.partner();
            if (partner != null && npc.state() != NpcState.DOWN && npc.state() != NpcState.SLEEPING) {
                npc.getLookControl().setLookAt(partner, 30.0F, 30.0F);
            }
        }
    }

    /** A warrior (or a summoned Einherjar) fights his target hand to hand. */
    static final class Fight extends MeleeAttackGoal {
        private final AsgardianEntity npc;

        Fight(AsgardianEntity npc) {
            super(npc, 1.15, true);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return (npc.role().warrior() || npc.summoned()) && !npc.state().still() && npc.state() != NpcState.TALKING
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.state().still() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            npc.setState(NpcState.FIGHTING, 0);
        }

        @Override
        public void stop() {
            super.stop();
            if (npc.state() == NpcState.FIGHTING) {
                npc.setState(NpcState.IDLE, 0);
            }
        }
    }

    /** The citizens run from Hela's army and from monsters, then go home. */
    static final class Flee extends Goal {
        private final AsgardianEntity npc;
        @Nullable
        private Vec3 away;
        private int repath;

        Flee(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Nullable
        private LivingEntity danger() {
            List<LivingEntity> near = npc.level().getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(10.0),
                    e -> e.isAlive() && (DefendAsgardGoal.army(e) || DefendAsgardGoal.hostileMonster(e)));
            LivingEntity nearest = null;
            for (LivingEntity e : near) {
                if (nearest == null || e.distanceToSqr(npc) < nearest.distanceToSqr(npc)) {
                    nearest = e;
                }
            }
            return nearest;
        }

        @Override
        public boolean canUse() {
            if (npc.role() != Role.CITIZEN || npc.state().still() || (npc.tickCount + npc.getId()) % 10 != 0) {
                return false;
            }
            LivingEntity danger = danger();
            if (danger == null) {
                return false;
            }
            away = DefaultRandomPos.getPosAway(npc, 16, 7, danger.position());
            return away != null;
        }

        @Override
        public void start() {
            if (npc.state() != NpcState.IDLE) {
                npc.setState(NpcState.IDLE, 0);
            }
            repath = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return away != null && !npc.getNavigation().isDone();
        }

        @Override
        public void tick() {
            if (away != null && --repath <= 0) {
                repath = REPATH;
                npc.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
            }
        }
    }

    /** A summoned Einherjar keeps by his master (and is brought to him if he falls far behind). */
    static final class FollowOwner extends Goal {
        private final AsgardianEntity npc;
        private int repath;

        FollowOwner(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ServerPlayer owner = npc.leader();
            return owner != null && npc.getTarget() == null && npc.distanceToSqr(owner) > 36.0;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = npc.leader();
            return owner != null && npc.getTarget() == null && npc.distanceToSqr(owner) > 9.0;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer owner = npc.leader();
            if (owner == null) {
                return;
            }
            npc.getLookControl().setLookAt(owner, 10.0F, npc.getMaxHeadXRot());
            if (--repath > 0) {
                return;
            }
            repath = 10;
            if (npc.distanceToSqr(owner) > 24.0 * 24.0) {
                Vec3 at = owner.position().add(owner.getLookAngle().multiply(-1.5, 0, -1.5));
                npc.moveTo(at.x, owner.getY(), at.z, owner.getYRot(), 0.0F);
                npc.getNavigation().stop();
            } else {
                npc.getNavigation().moveTo(owner, 1.2);
            }
        }
    }

    /**
     * Back home: beyond his home radius + 8 he walks back; beyond 64, or stuck for 30 s, he is set down at home while no
     * player within 32 can see him.
     */
    static final class ReturnHome extends Goal {
        private final AsgardianEntity npc;
        private int repath;
        private int stuck;

        ReturnHome(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos home = npc.activeHome();
            if (home == null || npc.leader() != null || patroller(npc) || npc.getTarget() != null || npc.state().still()
                    || npc.state() == NpcState.TALKING || npc.state() == NpcState.DINING
                    || (npc.tickCount + npc.getId()) % 20 != 0) {
                return false;
            }
            double far = npc.homeRadius() + 8.0;
            return npc.distanceToSqr(Vec3.atBottomCenterOf(home)) > far * far;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos home = npc.activeHome();
            return home != null && npc.getTarget() == null && !npc.state().still()
                    && npc.distanceToSqr(Vec3.atBottomCenterOf(home)) > Math.pow(Math.max(2, npc.homeRadius() - 2), 2);
        }

        @Override
        public void start() {
            repath = 0;
            stuck = 0;
            if (npc.state() != NpcState.IDLE) {
                npc.setState(NpcState.IDLE, 0);
            }
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos home = npc.activeHome();
            if (home == null) {
                return;
            }
            stuck++;
            boolean far = npc.distanceToSqr(Vec3.atBottomCenterOf(home)) > 64.0 * 64.0;
            if ((far || stuck > 600) && stuck % 20 == 0 && unseen(npc)) {
                BlockPos to = FeastHall.arrival(home);
                npc.moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, npc.getYRot(), 0.0F);
                npc.getNavigation().stop();
                stuck = 0;
                return;
            }
            if (--repath <= 0) {
                repath = REPATH * 2;
                npc.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.9);
            }
        }

        /** Whether no player within 32 blocks has him in sight. */
        static boolean unseen(AsgardianEntity npc) {
            for (Player player : npc.level().players()) {
                if (player.distanceToSqr(npc) < 32.0 * 32.0 && player.hasLineOfSight(npc)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Heimdall at his post by the socket, and the Einherjar who guard the gate and the door: back to the post, stand. */
    static final class KeepPost extends Goal {
        private final AsgardianEntity npc;
        private final boolean heimdall;
        private int repath;

        KeepPost(AsgardianEntity npc, boolean heimdall) {
            this.npc = npc;
            this.heimdall = heimdall;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return (heimdall || guard(npc)) && npc.free() && npc.post() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            // (3.5.1) While a player keeps Heimdall's post, his post is his rest spot nearby; he walks back after.
            BlockPos post = npc.post();
            if (post == null || --repath > 0) {
                return;
            }
            repath = REPATH;
            Vec3 at = Vec3.atBottomCenterOf(post);
            if (npc.distanceToSqr(at) > 2.25) {
                npc.getNavigation().moveTo(at.x, at.y, at.z, 0.7);
            } else if (npc.getNavigation().isDone() && npc.restAt() != null && npc.home() != null) {
                // Resting, he watches his post (and whoever keeps it).
                face(npc, Vec3.atBottomCenterOf(npc.home()));
            } else if (npc.getNavigation().isDone() && npc.level().getNearestPlayer(npc, 8.0) == null) {
                // Heimdall looks out of the Observatory to the stars, a guard out of his gate or door (both south).
                face(npc, at.add(0, 0, 8));
            }
        }
    }

    /** The Einherjar on patrol walk their loop of waypoints ({@code CityPlan.patrols()}). */
    static final class Patrol extends Goal {
        private final AsgardianEntity npc;
        private int next = -1;
        private int repath;
        private int stuck;

        Patrol(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private List<BlockPos> loop() {
            List<List<BlockPos>> loops = CityPlan.patrols();
            return loops.get(Math.floorMod(slotIndex(npc) - 4, loops.size()));
        }

        @Override
        public boolean canUse() {
            return patroller(npc) && npc.free() && !loop().isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            List<BlockPos> loop = loop();
            if (next < 0) {
                next = Math.floorMod(slotIndex(npc), loop.size());
            }
            repath = 0;
        }

        @Override
        public void tick() {
            List<BlockPos> loop = loop();
            next = Math.floorMod(next, loop.size());
            BlockPos at = loop.get(next);
            Vec3 target = Vec3.atBottomCenterOf(at);
            if (npc.distanceToSqr(target) < 4.0) {
                next = (next + 1) % loop.size();
                repath = 0;
                stuck = 0;
                return;
            }
            stuck++;
            if (stuck > 600 && stuck % 20 == 0 && ReturnHome.unseen(npc)) {
                npc.moveTo(target.x, target.y, target.z, npc.getYRot(), 0.0F);
                stuck = 0;
                return;
            }
            if (--repath <= 0) {
                repath = REPATH * 2;
                npc.getNavigation().moveTo(target.x, target.y, target.z, 0.6);
            }
        }
    }

    /** Odin sleeps the Odinsleep in his chamber until Hela first falls; then he wakes and walks the throne room. */
    static final class Sleep extends Goal {
        private final AsgardianEntity npc;

        Sleep(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        static boolean awake(AsgardianEntity npc) {
            return npc.level() instanceof ServerLevel server && AsgardState.get(server.getServer()).odinAwake();
        }

        @Override
        public boolean canUse() {
            if ((npc.tickCount + npc.getId()) % 20 != 0) {
                return npc.state() == NpcState.SLEEPING;
            }
            if (awake(npc) || npc.home() == null) {
                if (npc.state() == NpcState.SLEEPING) {
                    npc.setState(NpcState.IDLE, 0);
                }
                return false;
            }
            return npc.state() == NpcState.IDLE || npc.state() == NpcState.SLEEPING;
        }

        @Override
        public void start() {
            BlockPos bed = npc.home();
            if (bed != null && npc.distanceToSqr(Vec3.atBottomCenterOf(bed)) > 1.0) {
                npc.moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.0F, 0.0F);
            }
            npc.getNavigation().stop();
            npc.setState(NpcState.SLEEPING, 0);
        }

        @Override
        public boolean canContinueToUse() {
            return npc.state() == NpcState.SLEEPING;
        }
    }

    /** Sif and two of the Warriors Three spar in pairs in the training ring: blows that clash, no harm done. */
    static final class Spar extends Goal {
        private final AsgardianEntity npc;
        private int beat;
        private long nextTry;
        private int repath;

        Spar(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private static boolean sparrer(AsgardianEntity e) {
            return e.role() == Role.SIF || e.role() == Role.FANDRAL || e.role() == Role.HOGUN || e.role() == Role.VOLSTAGG;
        }

        @Override
        public boolean canUse() {
            if (npc.state() == NpcState.SPARRING && npc.partner() instanceof AsgardianEntity) {
                return true; // led by the other
            }
            long now = npc.level().getGameTime();
            boolean feast = FeastHall.feastNow(npc.level());
            if (!npc.free() || now < nextTry || npc.getRandom().nextInt(40) != 0 || feast) {
                return false; // (at the feast they dine, not spar)
            }
            nextTry = now + 200 + npc.getRandom().nextInt(200);
            for (AsgardianEntity other : npc.level().getEntitiesOfClass(AsgardianEntity.class, npc.getBoundingBox().inflate(14.0),
                    e -> e != npc && sparrer(e) && e.free())) {
                int ticks = 240 + npc.getRandom().nextInt(200);
                npc.setState(NpcState.SPARRING, ticks);
                npc.setPartner(other);
                other.setState(NpcState.SPARRING, ticks);
                other.setPartner(npc);
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return npc.state() == NpcState.SPARRING && npc.partner() instanceof AsgardianEntity other && other.isAlive()
                    && other.state() == NpcState.SPARRING;
        }

        @Override
        public void start() {
            beat = npc.getId() % 2 == 0 ? 0 : 12;
            repath = 0;
        }

        @Override
        public void stop() {
            if (npc.state() == NpcState.SPARRING) {
                npc.setState(NpcState.IDLE, 0);
            }
            npc.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(npc.partner() instanceof AsgardianEntity other)) {
                return;
            }
            npc.getLookControl().setLookAt(other, 30.0F, 30.0F);
            double distance = npc.distanceTo(other);
            if (distance > 2.8) {
                if (--repath <= 0) {
                    repath = REPATH;
                    npc.getNavigation().moveTo(other, 0.7);
                }
                return;
            }
            npc.getNavigation().stop();
            face(npc, other.position());
            if (++beat % 24 == 0) {
                npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                if (npc.level() instanceof ServerLevel level) {
                    Vec3 mid = npc.position().add(other.position()).scale(0.5).add(0, 1.3, 0);
                    level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 6, 0.15, 0.15, 0.15, 0.25);
                    level.playSound(null, mid.x, mid.y, mid.z, ModSounds.NPC_SPAR_CLASH.get(), npc.getSoundSource(), 0.7F,
                            0.9F + npc.getRandom().nextFloat() * 0.2F);
                }
            }
        }
    }

    /**
     * Dining at a table (3.7.2): Volstagg at his place at the high table of the Hall of Feasts now and then, and at
     * dusk (the feast, {@link FeastHall}) the Warriors Three and Sif with him until the night. He walks to his seat
     * (someone sits there: the first free seat of the hall), sits on the bench facing the table and eats, bite after
     * bite. One whose home is not the hall (made by a command, the tests) dines at a table near his home if it has one.
     * A diner who cannot get through for a long while, and whom nobody sees, sits down at once.
     */
    static final class Dine extends Goal {
        private final AsgardianEntity npc;
        @Nullable
        private CityPlan.Bench seat;
        private long nextTry;
        private int repath;
        private int walk;
        private int bite;
        /** He sat down for the feast: he gets up when it is over (the night came, or a bed skipped it). */
        private boolean forFeast;

        Dine(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        /** Whether the feast is on for him (a guest of the high table, at dusk). */
        private boolean feast() {
            return FeastHall.guest(npc.role()) && FeastHall.feastNow(npc.level());
        }

        @Override
        public boolean canUse() {
            if (!npc.free() || npc.home() == null) {
                return false;
            }
            long now = npc.level().getGameTime();
            boolean feast = feast();
            boolean invited = ModAttachments.PERSISTENT_DATA.get(npc).getBoolean(FeastHall.INVITED);
            if (!invited && (now < nextTry || !feast && (npc.role() != Role.VOLSTAGG
                    || npc.getRandom().nextInt(30) != 0))) {
                return false;
            }
            ModAttachments.PERSISTENT_DATA.get(npc).remove(FeastHall.INVITED);
            nextTry = now + (feast ? 100 : 400 + npc.getRandom().nextInt(400));
            seat = choose();
            return seat != null && npc.distanceToSqr(Vec3.atBottomCenterOf(seat.pos())) < 40.0 * 40.0;
        }

        /** His own place at the high table, or the hall's first free seat; away from the hall, a seat near his home. */
        @Nullable
        private CityPlan.Bench choose() {
            Level level = npc.level();
            if (AsgardDimension.is(level) && FeastHall.guest(npc.role())) {
                CityPlan.Bench own = FeastHall.seatOf(npc.role());
                if (own != null && free(own.pos())) {
                    return own;
                }
                for (CityPlan.Bench s : FeastHall.seats()) {
                    if (free(s.pos())) {
                        return s;
                    }
                }
                return null;
            }
            BlockPos home = npc.home();
            int r = Math.min(12, npc.homeRadius());
            for (BlockPos p : BlockPos.betweenClosed(home.offset(-r, -2, -r), home.offset(r, 2, r))) {
                if (free(p)) {
                    return new CityPlan.Bench(p.immutable(),
                            level.getBlockState(p).getValue(HorizontalDirectionalBlock.FACING));
                }
            }
            return null;
        }

        private boolean free(BlockPos pos) {
            return FeastHall.diningSeat(npc.level(), pos) && !taken(npc, pos);
        }

        @Override
        public boolean canContinueToUse() {
            if (npc.state() == NpcState.DINING) {
                return seat != null && !(forFeast && !feast());
            }
            return seat != null && npc.free() && walk < 600;
        }

        @Override
        public void start() {
            repath = 0;
            walk = 0;
            bite = 0;
            forFeast = false;
        }

        @Override
        public void stop() {
            if (npc.state() == NpcState.DINING) {
                npc.setState(NpcState.IDLE, 0);
            }
            npc.getNavigation().stop();
            seat = null;
        }

        @Override
        public void tick() {
            if (seat == null || !(npc.level() instanceof ServerLevel level)) {
                return;
            }
            if (npc.state() == NpcState.DINING) {
                if (++bite % 30 == 0) {
                    ItemStack food = npc.getMainHandItem().isEmpty() ? new ItemStack(Items.COOKED_CHICKEN)
                            : npc.getMainHandItem();
                    Vec3 mouth = npc.getEyePosition().add(npc.getLookAngle().scale(0.35)).subtract(0, 0.2, 0);
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food), mouth.x, mouth.y, mouth.z, 5,
                            0.08, 0.05,
                            0.08, 0.04);
                    level.playSound(null, npc.getX(), npc.getY(), npc.getZ(), SoundEvents.GENERIC_EAT,
                            npc.getSoundSource(), 0.6F,
                            0.8F + npc.getRandom().nextFloat() * 0.3F);
                }
                return;
            }
            walk++;
            if (walk % REPATH == 0 && taken(npc, seat.pos())) {
                // someone sat down there first: another seat
                seat = choose();
                repath = 0;
                return;
            }
            Vec3 at = sitAt(level, seat.pos(), seat.facing());
            // beside his seat (his path ends a step from it) he sits down; one stuck for long, unseen, at once
            if (npc.distanceToSqr(at) < 2.6 && Math.abs(npc.getY() - at.y) < 1.0 || walk > 400 && walk % 20 == 0
                    && ReturnHome.unseen(npc)) {
                long day = level.getDayTime();
                forFeast = feast();
                int ticks = forFeast ? FeastHall.feastLeft(day) + npc.getRandom().nextInt(80)
                        : 300 + npc.getRandom().nextInt(300);
                sitDown(npc, at, seat.facing(), NpcState.DINING, Math.max(40, ticks));
            } else if (--repath <= 0) {
                repath = REPATH;
                npc.getNavigation().moveTo(seat.pos().getX() + 0.5, seat.pos().getY() + 0.5, seat.pos().getZ() + 0.5,
                        0.7);
            }
        }
    }

    /**
     * A sitter (on a bench, at a table) keeps facing the way his seat faces (3.7.2; before, he sat facing wherever he
     * had walked from): he looks ahead, or at a player near him and before him.
     */
    static final class Seated extends Goal {
        private final AsgardianEntity npc;

        Seated(AsgardianEntity npc) {
            this.npc = npc;
            // (LOOK only: the goal that sat him down, Sit or Dine, keeps him there)
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return (npc.state() == NpcState.SITTING || npc.state() == NpcState.DINING)
                    && ModAttachments.PERSISTENT_DATA.get(npc).contains(SEAT_YAW);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            float yaw = ModAttachments.PERSISTENT_DATA.get(npc).getFloat(SEAT_YAW);
            npc.setYRot(yaw);
            npc.setYBodyRot(yaw);
            // He stays on his seat: one walking past (along the benches behind the high table) slips by, not shoves him.
            net.minecraft.nbt.ListTag where = ModAttachments.PERSISTENT_DATA.get(npc)
                    .getList(SEAT_AT, net.minecraft.nbt.Tag.TAG_DOUBLE);
            if (where.size() == 3) {
                Vec3 at = new Vec3(where.getDouble(0), where.getDouble(1), where.getDouble(2));
                if (npc.position().distanceToSqr(at) > 0.0025) {
                    npc.setPos(at.x, at.y, at.z);
                    npc.setDeltaMovement(Vec3.ZERO);
                }
            }
            Player near = npc.level().getNearestPlayer(npc.getX(), npc.getY(), npc.getZ(), 6.0, p -> !p.isSpectator());
            float toNear = near == null ? 0.0F
                    : (float) (Mth.atan2(near.getZ() - npc.getZ(), near.getX() - npc.getX()) * Mth.RAD_TO_DEG) - 90.0F;
            if (near != null && Math.abs(Mth.wrapDegrees(toNear - yaw)) < 70.0F) {
                npc.getLookControl().setLookAt(near, 30.0F, 30.0F);
            } else {
                Vec3 ahead = npc.getEyePosition().add(Vec3.directionFromRotation(npc.state() == NpcState.DINING
                        ? 25.0F : 0.0F, yaw)
                        .scale(2.0));
                npc.getLookControl().setLookAt(ahead.x, ahead.y, ahead.z);
            }
        }

        @Override
        public void stop() {
            if (npc.state() != NpcState.SITTING && npc.state() != NpcState.DINING) {
                ModAttachments.PERSISTENT_DATA.get(npc).remove(SEAT_YAW);
                ModAttachments.PERSISTENT_DATA.get(npc).remove(SEAT_AT);
            }
        }
    }

    /** Frigga and the citizens sit a while on a bench near their home, facing the way it faces (3.7.2). */
    static final class Sit extends Goal {
        private final AsgardianEntity npc;
        @Nullable
        private CityPlan.Bench seat;
        private long nextTry;
        private int repath;
        private int walk;

        Sit(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            long now = npc.level().getGameTime();
            if (!npc.free() || now < nextTry || npc.getRandom().nextInt(60) != 0 || npc.home() == null) {
                return false;
            }
            nextTry = now + 600 + npc.getRandom().nextInt(600);
            seat = freeSeat();
            return seat != null;
        }

        @Nullable
        private CityPlan.Bench freeSeat() {
            BlockPos home = npc.home();
            double reach = npc.homeRadius() + 8.0;
            for (CityPlan.Bench s : CityPlan.benches()) {
                if (home == null || s.pos().distSqr(home) > reach * reach) {
                    continue;
                }
                AABB around = new AABB(s.pos()).inflate(0.4, 1.0, 0.4);
                if (npc.level().getEntitiesOfClass(AsgardianEntity.class, around, e -> e != npc).isEmpty()) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public void start() {
            repath = 0;
            walk = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return seat != null && (npc.state() == NpcState.SITTING || npc.free() && walk < 400);
        }

        @Override
        public void stop() {
            if (npc.state() == NpcState.SITTING) {
                npc.setState(NpcState.IDLE, 0);
            }
            seat = null;
        }

        @Override
        public void tick() {
            if (seat == null || npc.state() == NpcState.SITTING || !(npc.level() instanceof ServerLevel level)) {
                return;
            }
            walk++;
            Vec3 top = seatTop(level, seat.pos());
            if (npc.distanceToSqr(top) < 1.8) {
                sitDown(npc, top, seat.facing(), NpcState.SITTING, 300 + npc.getRandom().nextInt(400));
            } else if (--repath <= 0) {
                repath = REPATH;
                npc.getNavigation().moveTo(top.x, top.y, top.z, 0.6);
            }
        }
    }

    /** Two idle Asgardians meet and talk a while, face to face, with gestures and a murmur. */
    static final class TalkInPairs extends Goal {
        private final AsgardianEntity npc;
        private long nextTry;

        TalkInPairs(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (npc.state() == NpcState.TALKING && npc.talkingWith() == null && npc.partner() instanceof AsgardianEntity) {
                return true; // the other began it
            }
            long now = npc.level().getGameTime();
            if (!npc.free() || now < nextTry || npc.getRandom().nextInt(50) != 0) {
                return false;
            }
            nextTry = now + 500 + npc.getRandom().nextInt(700);
            for (AsgardianEntity other : npc.level().getEntitiesOfClass(AsgardianEntity.class, npc.getBoundingBox().inflate(6.0),
                    e -> e != npc && e.free() && e.role() != Role.HEIMDALL && e.role() != Role.ODIN && !patroller(e))) {
                if (npc.role() == Role.HEIMDALL || patroller(npc)) {
                    return false;
                }
                int ticks = 120 + npc.getRandom().nextInt(120);
                npc.setState(NpcState.TALKING, ticks);
                npc.setPartner(other);
                other.setState(NpcState.TALKING, ticks);
                other.setPartner(npc);
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return npc.state() == NpcState.TALKING && npc.talkingWith() == null && npc.partner() instanceof AsgardianEntity other
                    && other.isAlive() && other.state() == NpcState.TALKING;
        }

        @Override
        public void start() {
            npc.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (npc.state() == NpcState.TALKING && npc.talkingWith() == null) {
                npc.setState(NpcState.IDLE, 0);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(npc.partner() instanceof AsgardianEntity other)) {
                return;
            }
            npc.getLookControl().setLookAt(other, 30.0F, 30.0F);
            if (npc.distanceToSqr(other) > 6.0) {
                if ((npc.tickCount + npc.getId()) % REPATH == 0) {
                    npc.getNavigation().moveTo(other, 0.5);
                }
            } else {
                npc.getNavigation().stop();
            }
            if ((npc.tickCount + npc.getId() * 7) % 50 == 0 && npc.getRandom().nextBoolean()) {
                npc.murmur(0.35F);
            }
        }
    }

    /** A stroll round his home (only when nothing else holds him). */
    static final class Stroll extends WaterAvoidingRandomStrollGoal {
        private final AsgardianEntity npc;

        Stroll(AsgardianEntity npc) {
            super(npc, 0.55, 0.001F);
            this.npc = npc;
            setInterval(160);
        }

        @Override
        public boolean canUse() {
            return npc.free() && npc.role() != Role.HEIMDALL && !guard(npc) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return npc.free() && super.canContinueToUse();
        }
    }

    // ---- targets ----

    /**
     * Whom a warrior answers: what his master (a summoned Einherjar's) attacks or is attacked by, Odin's command, and
     * whoever (no player, no Asgardian) struck him.
     */
    static final class Defend extends Goal {
        private final AsgardianEntity npc;

        Defend(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Nullable
        private LivingEntity pick() {
            LivingEntity commanded = npc.commanded();
            if (commanded != null) {
                return commanded;
            }
            ServerPlayer owner = npc.ownerPlayer();
            if (owner != null) {
                for (LivingEntity e : new LivingEntity[]{owner.getLastHurtByMob(), owner.getLastHurtMob()}) {
                    if (fair(e, owner)) {
                        return e;
                    }
                }
            }
            if (!npc.role().warrior() && !npc.summoned()) {
                return null;
            }
            LivingEntity attacker = npc.getLastHurtByMob();
            return attacker != null && !(attacker instanceof Player) && fair(attacker, owner) && attacker.distanceToSqr(npc) < 256 ? attacker : null;
        }

        /** Never the master, his pets, other Asgardians; a player only when the master may fight him. */
        private boolean fair(@Nullable LivingEntity e, @Nullable ServerPlayer owner) {
            if (e == null || !e.isAlive() || e == npc || e instanceof AsgardianEntity || e == owner) {
                return false;
            }
            if (owner != null && e instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) {
                return false;
            }
            if (e instanceof Player player) {
                return npc.mayHurtPlayer(player);
            }
            return true;
        }

        @Override
        public boolean canUse() {
            if (npc.state().still() || npc.tickCount % 5 != 0) {
                return false;
            }
            LivingEntity target = pick();
            if (target == null || target == npc.getTarget()) {
                return false;
            }
            npc.setTarget(target);
            return false;
        }
    }

    // ---- Sif's training ----

    /**
     * "Train with us" (Sif): a duel with a player, wooden-sword rules: blows land as touches, nobody is hurt; the first to
     * five touches wins, within 60 s. The player wins Sif's Token.
     */
    static final class SparPlayer extends Goal {
        /** The duels running: Sif's entity id → the duel. */
        private static final Map<Integer, Duel> DUELS = new HashMap<>();
        static final int TOUCHES = 5;
        static final int DUEL_TICKS = 1200;

        private final AsgardianEntity npc;
        private int cooldown;
        private int repath;

        /** A duel: who, the touches of each, when it ends. */
        static final class Duel {
            final UUID player;
            int playerTouches;
            int sifTouches;
            final long until;

            Duel(UUID player, long until) {
                this.player = player;
                this.until = until;
            }
        }

        SparPlayer(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        /** Starts a duel of Sif with {@code player}. */
        static void begin(AsgardianEntity sif, ServerPlayer player) {
            DUELS.put(sif.getId(), new Duel(player.getUUID(), sif.level().getGameTime() + DUEL_TICKS));
            sif.endTalk();
            sif.setState(NpcState.SPARRING, DUEL_TICKS);
            sif.setPartner(player);
        }

        /** Forgets every duel (the server stops). */
        static void clear() {
            DUELS.clear();
        }

        @Nullable
        static Duel duel(AsgardianEntity sif) {
            return DUELS.get(sif.getId());
        }

        /** The player touched Sif in a duel: counted instead of manners. Returns whether it was a duel's touch. */
        static boolean hit(AsgardianEntity sif, ServerPlayer player) {
            Duel duel = DUELS.get(sif.getId());
            if (duel == null || !duel.player.equals(player.getUUID())) {
                return false;
            }
            if (sif.invulnerableTime > 0) {
                return true; // one touch per blow
            }
            sif.invulnerableTime = 10;
            duel.playerTouches++;
            touched(sif, player, sif);
            check(sif, duel);
            return true;
        }

        private static void touched(AsgardianEntity sif, ServerPlayer player, Entity struck) {
            if (sif.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, struck.getX(), struck.getY() + 1.2, struck.getZ(), 8, 0.2, 0.3, 0.2, 0.2);
                level.playSound(null, struck.getX(), struck.getY(), struck.getZ(), ModSounds.NPC_SPAR_CLASH.get(), sif.getSoundSource(), 0.8F, 1.1F);
            }
            Duel duel = DUELS.get(sif.getId());
            if (duel != null) {
                player.displayClientMessage(Component.translatable("npc.enhanced.sif.duel_score", duel.playerTouches, duel.sifTouches)
                        .withStyle(s -> s.withColor(Role.SIF.color())), true);
            }
        }

        private static void check(AsgardianEntity sif, Duel duel) {
            ServerPlayer player = sif.level() instanceof ServerLevel level ? (ServerPlayer) level.getPlayerByUUID(duel.player) : null;
            boolean timeUp = sif.level().getGameTime() >= duel.until;
            if (player == null || duel.playerTouches >= TOUCHES || duel.sifTouches >= TOUCHES || timeUp
                    || player.distanceTo(sif) > 24) {
                DUELS.remove(sif.getId());
                sif.setState(NpcState.IDLE, 0);
                if (player == null) {
                    return;
                }
                if (duel.playerTouches >= TOUCHES) {
                    AsgardTrades.giveToken(player);
                    AsgardTalk.speak(sif, player, "npc.enhanced.sif.duel_won", List.of());
                } else if (duel.sifTouches >= TOUCHES) {
                    AsgardTalk.speak(sif, player, "npc.enhanced.sif.duel_lost", List.of());
                } else {
                    AsgardTalk.speak(sif, player, "npc.enhanced.sif.duel_draw", List.of());
                }
            }
        }

        @Override
        public boolean canUse() {
            return DUELS.containsKey(npc.getId()) && !npc.state().still();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            cooldown = 30;
            repath = 0;
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Duel duel = DUELS.get(npc.getId());
            if (duel == null || !(npc.level() instanceof ServerLevel level)) {
                return;
            }
            Player player = level.getPlayerByUUID(duel.player);
            if (!(player instanceof ServerPlayer target)) {
                check(npc, duel);
                return;
            }
            npc.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = npc.distanceTo(target);
            if (distance > 2.2) {
                if (--repath <= 0) {
                    repath = 10;
                    npc.getNavigation().moveTo(target, 0.9);
                }
            } else {
                npc.getNavigation().stop();
            }
            if (--cooldown <= 0 && distance <= 2.6) {
                cooldown = 26 + npc.getRandom().nextInt(14);
                npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                // Sif is good: she lands two blows in three.
                if (npc.getRandom().nextInt(3) != 0) {
                    duel.sifTouches++;
                    Vec3 push = target.position().subtract(npc.position()).multiply(1, 0, 1).normalize().scale(0.3);
                    target.push(push.x, 0.1, push.z);
                    target.hurtMarked = true;
                    touched(npc, target, target);
                }
            }
            check(npc, duel);
        }
    }
}
