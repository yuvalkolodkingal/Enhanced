package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.asgard.SeatEntity;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.asgard.npc.DefendAsgardGoal;
import com.kolodkingal.enhanced.asgard.npc.NpcState;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.hydra.HydraGarrison;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;

/**
 * The daily life of the cottage's residents (3.7, the plan's 9.6), by the time of day, all in and round the cottage:
 *
 * <pre>
 *   time (day ticks)    Agatha                        Nicky                             Rio
 *   0-2000 morning      waters the herbs (or stirs)   feeds Señor Scratchy at the hutch the greenhouse, the beds
 *   2000-11000 day      stirs the pot, reads at the   plays in the yard with his toy    gardens, sits on the bench,
 *                       table, rocks on the porch     broom, chases Scratchy, sits on   gives Nicky a flower
 *                                                     the porch or the bench
 *   11000-12500 dusk    the family dinner: all three at the table, hearts now and then
 *   12500-23000 night   in their beds, asleep
 * </pre>
 *
 * When it rains they stay in (Agatha in the kitchen, Nicky in his room, Rio in her greenhouse); from monsters they run
 * home (a door is shut behind them: the Asgardians' {@code OpenDoorGoal}). Nobody ever fights. {@code
 * AsgardianEntity.makeGoals} hands the roles AGATHA, NICKY and RIO here, after the goals every role has (floating,
 * holding still, fleeing, returning home; talking in pairs and strolling come after these). Three goals: {@link Clock}
 * (no controls: wakes them in the morning, the hearts at the table, Rio's flower), {@link Shelter}, {@link Seated}
 * (a sitter keeps facing the way his chair faces) and {@link Life} (the activity of the hour). The tests drive a
 * resident's hour with {@link #settle}.
 */
public final class CottageGoals {
    /** The persistent tag of the tick at which Nicky lets go of Rio's flower. */
    static final String FLOWER_UNTIL = "enhanced:flower_until";
    /** The persistent tag of the way a seated resident faces (his chair's front). */
    static final String SEAT_YAW = "enhanced:seat_yaw";
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.68F, 0.36F, 0.95F), 1.0F);
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.45F, 0.9F, 0.45F), 1.0F);

    /** The parts of a day (by {@code dayTime % 24000}). */
    public enum Phase {
        MORNING, DAY, DUSK, NIGHT;

        public static Phase at(long dayTime) {
            long t = Math.floorMod(dayTime, 24000L);
            return t < 2000L ? MORNING : t < 11000L ? DAY : t < 12500L ? DUSK : t < 23000L ? NIGHT : MORNING;
        }
    }

    /** What a resident does. */
    public enum Activity {
        STIR, HERBS, READ, PORCH, FEED, PLAY, CHASE, BENCH, INDOORS, GREENHOUSE, GARDEN, FLOWER, DINNER, BED
    }

    /** Adds the goals of {@code npc}'s role (AGATHA, NICKY or RIO). */
    public static void add(AsgardianEntity npc) {
        npc.goalSelector.addGoal(0, new Clock(npc));
        npc.goalSelector.addGoal(1, new Shelter(npc));
        npc.goalSelector.addGoal(2, new Seated(npc));
        npc.goalSelector.addGoal(4, new Life(npc));
    }

    /** The cottage a resident belongs to, or null (a stray one made by a command). */
    @Nullable
    static Cottages.Cottage cottage(AsgardianEntity npc) {
        return npc.level() instanceof ServerLevel level ? Cottages.get(level).of(npc) : null;
    }

    static boolean raining(ServerLevel level, Cottages.Cottage cottage) {
        return level.isRainingAt(cottage.points().yard().above(2)) || level.isThundering();
    }

    /** The activity of a role at a part of the day (rain keeps them in), one of its choices at random. */
    public static Activity pick(Role role, Phase phase, boolean rain, RandomSource random) {
        if (phase == Phase.NIGHT) {
            return Activity.BED;
        }
        if (phase == Phase.DUSK) {
            return Activity.DINNER;
        }
        int r = random.nextInt(100);
        return switch (role) {
            case NICKY -> phase == Phase.MORNING ? (rain ? Activity.INDOORS : Activity.FEED)
                    : rain ? (r < 70 ? Activity.INDOORS : Activity.PORCH)
                    : r < 40 ? Activity.PLAY : r < 65 ? Activity.CHASE : r < 80 ? Activity.PORCH
                    : r < 90 ? Activity.BENCH : Activity.INDOORS;
            case RIO -> phase == Phase.MORNING ? (rain || r < 50 ? Activity.GREENHOUSE : Activity.GARDEN)
                    : rain ? Activity.GREENHOUSE
                    : r < 45 ? Activity.GARDEN : r < 65 ? Activity.BENCH : r < 80 ? Activity.FLOWER
                    : Activity.GREENHOUSE;
            default -> phase == Phase.MORNING ? (r < 70 ? Activity.HERBS : Activity.STIR)
                    : r < 40 ? Activity.STIR : r < 70 ? Activity.READ : rain ? Activity.STIR : Activity.PORCH;
        };
    }

    /** Where an activity takes a resident (null: nowhere fixed, Rio's flower for Nicky). */
    @Nullable
    public static BlockPos spot(Activity activity, Role role, Cottages.Points p, RandomSource random) {
        return switch (activity) {
            case STIR -> p.stove();
            case HERBS -> p.herbs();
            case READ, DINNER -> p.seat(role);
            case PORCH -> p.porch().get(role == Role.NICKY ? 1 : 0);
            case FEED -> p.hutchGate();
            case PLAY, CHASE -> p.yard();
            case BENCH -> p.bench().get(role == Role.NICKY ? 1 : 0);
            case INDOORS -> role == Role.NICKY ? p.nickyHome() : p.agathaHome();
            case GREENHOUSE -> p.greenhouse();
            case GARDEN -> p.garden().get(random.nextInt(p.garden().size()));
            case FLOWER -> null;
            case BED -> p.bed(role);
        };
    }

    /**
     * Puts a resident where the hour {@code dayTime} sends him, at once, as if he had walked there (the tests' drive;
     * and a resident made at night goes straight to bed). Returns the activity.
     */
    public static Activity settle(AsgardianEntity npc, Cottages.Cottage cottage, long dayTime) {
        Activity activity = pick(npc.role(), Phase.at(dayTime), false, npc.getRandom());
        if (activity == Activity.FLOWER) {
            activity = Activity.GARDEN;
        }
        BlockPos spot = spot(activity, npc.role(), cottage.points(), npc.getRandom());
        if (spot != null) {
            arrive(npc, cottage, activity, spot, dayTime);
        }
        return activity;
    }

    /** What a resident does on reaching the spot of his activity (sits, lies down, or just stands there). */
    static void arrive(AsgardianEntity npc, Cottages.Cottage cottage, Activity activity, BlockPos spot, long dayTime) {
        switch (activity) {
            case READ -> sit(npc, freeSeat(npc, cottage, spot), 400 + npc.getRandom().nextInt(300));
            case PORCH, BENCH -> sit(npc, spot, 400 + npc.getRandom().nextInt(400));
            case DINNER -> sit(npc, freeSeat(npc, cottage, spot),
                    (int) Math.max(40L, 12500L - Math.floorMod(dayTime, 24000L)) + 20);
            case BED -> lieDown(npc, spot);
            default -> {
                npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, npc.getYRot(), 0.0F);
                npc.getNavigation().stop();
            }
        }
    }

    /** The resident's own chair, or (someone sits there: a guest, a player) the first free chair of the table. */
    static BlockPos freeSeat(AsgardianEntity npc, Cottages.Cottage cottage, BlockPos own) {
        if (!taken(npc, own)) {
            return own;
        }
        for (BlockPos seat : cottage.points().seats()) {
            if (!taken(npc, seat)) {
                return seat;
            }
        }
        return own;
    }

    static boolean taken(AsgardianEntity npc, BlockPos seat) {
        AABB around = new AABB(seat).inflate(0.1, 0.6, 0.1);
        return !npc.level().getEntities(npc, around, e -> e instanceof LivingEntity && e.isAlive()
                || e instanceof SeatEntity && e.isVehicle()).isEmpty();
    }

    /** Sits him on a chair or bench (a stair: on its seat, facing away from its back) for {@code ticks}. */
    static void sit(AsgardianEntity npc, BlockPos seat, int ticks) {
        BlockState state = npc.level().getBlockState(seat);
        float yaw = npc.getYRot();
        double top = 0.5;
        if (state.getBlock() instanceof StairBlock) {
            yaw = state.getValue(StairBlock.FACING).getOpposite().toYRot();
        } else if (!state.getCollisionShape(npc.level(), seat).isEmpty()) {
            top = state.getCollisionShape(npc.level(), seat).max(Direction.Axis.Y);
        } else {
            top = 0.0;
        }
        npc.getNavigation().stop();
        npc.moveTo(seat.getX() + 0.5, seat.getY() + top, seat.getZ() + 0.5, yaw, 0.0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        ModAttachments.PERSISTENT_DATA.get(npc).putFloat(SEAT_YAW, yaw);
        npc.setState(NpcState.SITTING, ticks);
    }

    /** Lies him in his bed (the head block {@code head}), asleep until the morning. */
    static void lieDown(AsgardianEntity npc, BlockPos head) {
        BlockState state = npc.level().getBlockState(head);
        Direction toHead = state.getBlock() instanceof BedBlock ? state.getValue(BedBlock.FACING) : Direction.NORTH;
        BlockPos foot = head.relative(toHead.getOpposite());
        // The body lies from the foot of the bed (feet) toward the pillow (head): it faces the head's way.
        Vec3 at = Vec3.atBottomCenterOf(foot).add(toHead.getOpposite().getStepX() * 0.35, 0.5625,
                toHead.getOpposite().getStepZ() * 0.35);
        float yaw = toHead.toYRot();
        npc.getNavigation().stop();
        npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        npc.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setState(NpcState.SLEEPING, 0);
    }

    static void face(AsgardianEntity npc, Vec3 target) {
        double dx = target.x - npc.getX();
        double dz = target.z - npc.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        npc.setYRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
    }

    static ItemStack named(Item item, String key) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.ITEM_NAME, Component.translatable(key));
        return stack;
    }

    @Nullable
    static BlockPos nearestFlowerPot(ServerLevel level, BlockPos at) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 3, 2))) {
            if (level.getBlockState(p).getBlock() instanceof FlowerPotBlock) {
                double d = p.distSqr(at);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    // ---- Nicky and his mom ----

    /**
     * Nicky runs to a player who is Agatha and greets her (3.5.1's one of each: the cottage's Agatha is away, so the
     * player is Mom): "Mom! You're home!", or, when two players are Agatha, "Two Moms?!". Returns whether he did (not
     * while asleep, knocked down or talking to someone).
     */
    public static boolean greetMom(AsgardianEntity nicky, ServerPlayer mom) {
        if (nicky.state().still() || nicky.talkingWith() != null || !(nicky.level() instanceof ServerLevel level)) {
            return false;
        }
        boolean two = Asgardians.heroesPlaying(level.getServer(), Power.AGATHA).size() >= 2;
        nicky.setState(NpcState.TALKING, 80);
        nicky.setPartner(mom);
        if (nicky.distanceTo(mom) > 2.5) {
            nicky.getNavigation().moveTo(mom, 1.3);
        }
        AsgardTalk.speak(nicky, mom, two ? "message.enhanced.cottage.two_moms" : "message.enhanced.cottage.mom_home",
                List.of());
        nicky.playSound(ModSounds.NICKY_CHEER.get(), 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HEART, nicky.getX(), nicky.getY() + nicky.getBbHeight() + 0.3, nicky.getZ(),
                4, 0.3, 0.2, 0.3, 0.0);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------

    /**
     * The clock (no controls, so it never stops another goal): every second, a sleeper wakes when the night is over,
     * diners stand at night, the table has hearts now and then at dinner, one who strayed out of the grounds goes
     * home, and Nicky lets go of Rio's flower after a while.
     */
    static final class Clock extends Goal {
        private final AsgardianEntity npc;

        Clock(AsgardianEntity npc) {
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            // (new goals are asked every other tick: every 20 ticks, whatever his id)
            if (((npc.tickCount >> 1) + npc.getId()) % 10 != 0 || !(npc.level() instanceof ServerLevel level)) {
                return false;
            }
            Cottages.Cottage cottage = cottage(npc);
            if (cottage == null) {
                return false;
            }
            Phase phase = Phase.at(level.getDayTime());
            NpcState state = npc.state();
            if (state == NpcState.SLEEPING && phase != Phase.NIGHT) {
                npc.setState(NpcState.IDLE, 0);
            } else if (state == NpcState.SITTING && phase == Phase.NIGHT) {
                npc.setState(NpcState.IDLE, 0); // dinner is over: off to bed
            } else if (state == NpcState.SITTING && phase == Phase.DUSK && npc.getRandom().nextInt(4) == 0) {
                level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.6, npc.getZ(), 1, 0.2, 0.1, 0.2,
                        0.0);
                if (npc.getRandom().nextBoolean()) {
                    npc.murmur(0.4F);
                }
            }
            if (!state.still() && cottage.distance(npc.position()) > 1.0) {
                // Out of the grounds (over a fence from a tree that grew by it, down a slope): home again.
                BlockPos home = cottage.points().home(npc.role());
                Vec3 at = Vec3.atBottomCenterOf(home);
                if (HydraGarrison.unseen(level, npc.position(), 16.0) && HydraGarrison.unseen(level, at, 16.0)) {
                    npc.getNavigation().stop();
                    npc.moveTo(at.x, at.y, at.z, npc.getYRot(), 0.0F);
                } else {
                    npc.getNavigation().moveTo(at.x, at.y, at.z, 0.8);
                }
            }
            long until = ModAttachments.PERSISTENT_DATA.get(npc).getLong(FLOWER_UNTIL);
            if (until > 0 && level.getGameTime() >= until) {
                ModAttachments.PERSISTENT_DATA.get(npc).remove(FLOWER_UNTIL);
                if (npc.getMainHandItem().is(Items.POPPY)) {
                    npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }
            return false;
        }
    }

    /**
     * A seated resident keeps facing the front of his chair (the idle looking about would turn his body round on it);
     * his head turns to a player near him in front.
     */
    static final class Seated extends Goal {
        private final AsgardianEntity npc;

        Seated(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return npc.state() == NpcState.SITTING && ModAttachments.PERSISTENT_DATA.get(npc).contains(SEAT_YAW);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            float yaw = ModAttachments.PERSISTENT_DATA.get(npc).getFloat(SEAT_YAW);
            npc.setYRot(yaw);
            npc.setYBodyRot(yaw);
            net.minecraft.world.entity.player.Player near = npc.level().getNearestPlayer(npc.getX(), npc.getY(),
                    npc.getZ(), 6.0, p -> !p.isSpectator());
            float toNear = near == null ? 0.0F
                    : (float) (Mth.atan2(near.getZ() - npc.getZ(), near.getX() - npc.getX()) * Mth.RAD_TO_DEG) - 90.0F;
            if (near != null && Math.abs(Mth.wrapDegrees(toNear - yaw)) < 70.0F) {
                npc.getLookControl().setLookAt(near, 30.0F, 30.0F);
            } else {
                Vec3 ahead = npc.getEyePosition().add(Vec3.directionFromRotation(0.0F, yaw).scale(2.0));
                npc.getLookControl().setLookAt(ahead.x, ahead.y, ahead.z);
            }
        }

        @Override
        public void stop() {
            if (npc.state() != NpcState.SITTING) {
                ModAttachments.PERSISTENT_DATA.get(npc).remove(SEAT_YAW);
            }
        }
    }

    /** A monster (any, not only one after a player): the family runs from it. */
    static boolean monster(LivingEntity e) {
        return e instanceof net.minecraft.world.entity.monster.Enemy
                && e.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER
                || DefendAsgardGoal.hostileMonster(e);
    }

    /** From monsters they run home and inside (Nicky to his room; Agatha and Rio to the kitchen). */
    static final class Shelter extends Goal {
        private final AsgardianEntity npc;
        @Nullable
        private BlockPos safe;
        private int ticks;

        Shelter(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if ((npc.tickCount + npc.getId()) % 10 != 0 || npc.state().still() || npc.talkingWith() != null) {
                return false;
            }
            Cottages.Cottage cottage = cottage(npc);
            if (cottage == null || npc.level().getEntitiesOfClass(LivingEntity.class,
                    npc.getBoundingBox().inflate(10.0), e -> e.isAlive() && monster(e)).isEmpty()) {
                return false;
            }
            safe = npc.role() == Role.NICKY ? cottage.points().nickyHome() : cottage.points().agathaHome();
            return npc.distanceToSqr(Vec3.atBottomCenterOf(safe)) > 4.0;
        }

        @Override
        public void start() {
            ticks = 0;
            if (npc.state() != NpcState.IDLE) {
                npc.setState(NpcState.IDLE, 0);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return safe != null && ticks < 200 && npc.distanceToSqr(Vec3.atBottomCenterOf(safe)) > 2.0;
        }

        @Override
        public void tick() {
            if (safe != null && ticks++ % 20 == 0) {
                npc.getNavigation().moveTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, 1.3);
            }
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
            safe = null;
        }
    }

    /**
     * The activity of the hour: picked when he is free (idle, nobody holding him), walked to, then done for a while
     * (stirring with a spoon, watering, gardening with sparkles, running round the yard...) or sat or slept through.
     * A pause between activities leaves room for strolling and talking in pairs.
     */
    static final class Life extends Goal {
        private final AsgardianEntity npc;
        @Nullable
        private Activity activity;
        @Nullable
        private BlockPos spot;
        private boolean arrived;
        private int ticks;
        private int doing;
        private int repath;
        private int hops;
        private int lost;
        private long nextPick;
        @Nullable
        private Phase phase;

        Life(AsgardianEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // (a mob asks its new goals on the ticks where tickCount + id is even)
            if (!npc.free() || (npc.tickCount + npc.getId()) % 10 != 0
                    || !(npc.level() instanceof ServerLevel level) || level.getGameTime() < nextPick) {
                return false;
            }
            Cottages.Cottage cottage = cottage(npc);
            if (cottage == null) {
                return false;
            }
            phase = Phase.at(level.getDayTime());
            activity = pick(npc.role(), phase, raining(level, cottage), npc.getRandom());
            spot = spot(activity, npc.role(), cottage.points(), npc.getRandom());
            if (activity == Activity.FLOWER) {
                AsgardianEntity nicky = CottageKeeper.resident(level, cottage, Role.NICKY);
                if (nicky == null || !nicky.free() || nicky.distanceToSqr(npc) > 24 * 24) {
                    activity = Activity.GARDEN;
                    spot = spot(activity, npc.role(), cottage.points(), npc.getRandom());
                }
            }
            return activity == Activity.FLOWER || spot != null;
        }

        @Override
        public void start() {
            arrived = false;
            ticks = 0;
            repath = 0;
            hops = 0;
            lost = 0;
            Activity a = activity == null ? Activity.INDOORS : activity;
            doing = switch (a) {
                case STIR, GREENHOUSE -> 300 + npc.getRandom().nextInt(300);
                case HERBS, FEED, CHASE -> 200 + npc.getRandom().nextInt(100);
                case PLAY -> 300;
                case GARDEN -> 120;
                case INDOORS -> 200;
                default -> 1;
            };
            ItemStack hand = switch (a) {
                case STIR -> named(Items.STICK, "item.enhanced.wooden_spoon");
                case HERBS -> new ItemStack(Items.WATER_BUCKET);
                case READ -> new ItemStack(Items.BOOK);
                case FEED -> new ItemStack(Items.CARROT);
                case PLAY -> named(Items.STICK, "item.enhanced.toy_broom");
                case GREENHOUSE, GARDEN -> new ItemStack(Items.BONE_MEAL);
                case FLOWER -> new ItemStack(Items.POPPY);
                default -> ItemStack.EMPTY;
            };
            if (!(hand.isEmpty() && npc.getMainHandItem().is(Items.POPPY))) {
                npc.setItemSlot(EquipmentSlot.MAINHAND, hand);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return activity != null && npc.free() && ticks < 900 && !(arrived && doing <= 0)
                    && Phase.at(npc.level().getDayTime()) == phase; // a new part of the day: its own activity
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
            boolean seated = npc.state() == NpcState.SITTING;
            boolean keepsBook = seated && npc.getMainHandItem().is(Items.BOOK);
            if (!keepsBook && !npc.getMainHandItem().is(Items.POPPY) || npc.state() == NpcState.SLEEPING) {
                npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            boolean evening = activity == Activity.BED || activity == Activity.DINNER;
            nextPick = npc.level().getGameTime() + (evening ? 40 : 60 + npc.getRandom().nextInt(140));
            activity = null;
            spot = null;
        }

        @Override
        public void tick() {
            ticks++;
            if (activity == null || !(npc.level() instanceof ServerLevel level)) {
                return;
            }
            Cottages.Cottage cottage = cottage(npc);
            if (cottage == null) {
                activity = null;
                return;
            }
            if (activity == Activity.FLOWER) {
                flower(level, cottage);
                return;
            }
            if (spot == null) {
                activity = null;
                return;
            }
            if (!arrived) {
                walk(level, cottage);
                return;
            }
            doing--;
            act(level, cottage);
        }

        private void walk(ServerLevel level, Cottages.Cottage cottage) {
            Vec3 to = Vec3.atBottomCenterOf(spot);
            double close = activity == Activity.PLAY || activity == Activity.CHASE ? 9.0 : 2.2;
            if (npc.distanceToSqr(to) < close) {
                arrived = true;
                arrive(npc, cottage, activity, spot, level.getDayTime());
                if (npc.state() != NpcState.IDLE) {
                    doing = 0; // sitting or asleep: the state holds him until the clock or its time ends it
                }
                return;
            }
            // No way there (a long way round, a door in the way, out over a fence): his path ends short of the spot.
            lost = npc.getNavigation().isDone() && ticks > 20 ? lost + 1 : 0;
            if ((ticks > 300 || lost > 60) && ticks % 20 == 0 && HydraGarrison.unseen(level, npc.position(), 16.0)
                    && HydraGarrison.unseen(level, to, 16.0)) {
                npc.moveTo(to.x, to.y, to.z, npc.getYRot(), 0.0F); // stuck: set down there while nobody looks
                return;
            }
            if (--repath <= 0) {
                repath = 20;
                npc.getNavigation().moveTo(to.x, to.y, to.z, npc.role() == Role.NICKY ? 0.75 : 0.6);
            }
        }

        /** The doing of an activity, every tick at its spot. */
        private void act(ServerLevel level, Cottages.Cottage cottage) {
            Cottages.Points p = cottage.points();
            switch (activity) {
                case STIR -> {
                    Vec3 pot = Vec3.atBottomCenterOf(p.pot());
                    face(npc, pot);
                    npc.getLookControl().setLookAt(pot.x, pot.y + 0.5, pot.z);
                    if (ticks % 15 == 0) {
                        npc.swing(InteractionHand.MAIN_HAND);
                    }
                    if (ticks % 5 == 0) {
                        level.sendParticles(ticks % 10 == 0 ? PURPLE : GREEN, pot.x, pot.y + 1.0, pot.z, 2, 0.2, 0.15,
                                0.2, 0.01);
                    }
                    if (ticks % 80 == 0) {
                        level.playSound(null, pot.x, pot.y, pot.z, ModSounds.COTTAGE_POT.get(), SoundSource.NEUTRAL,
                                0.6F, 1.0F);
                    }
                }
                case HERBS -> {
                    BlockPos pot = nearestFlowerPot(level, npc.blockPosition());
                    if (pot != null) {
                        Vec3 at = Vec3.atCenterOf(pot);
                        face(npc, at);
                        npc.getLookControl().setLookAt(at.x, at.y, at.z);
                        if (ticks % 20 == 0) {
                            npc.swing(InteractionHand.MAIN_HAND);
                            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.4, at.z, 6, 0.2, 0.1, 0.2, 0.0);
                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.3, at.z, 2, 0.2, 0.2, 0.2,
                                    0.0);
                        }
                    }
                }
                case FEED -> {
                    face(npc, Vec3.atBottomCenterOf(p.hutch()));
                    if (ticks % 25 == 0) {
                        npc.swing(InteractionHand.MAIN_HAND);
                        Rabbit scratchy = Scratchy.find(level, cottage);
                        if (scratchy != null && scratchy.distanceToSqr(npc) < 64.0) {
                            level.sendParticles(ParticleTypes.HEART, scratchy.getX(), scratchy.getY() + 0.6,
                                    scratchy.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                        }
                    }
                }
                case PLAY, CHASE -> run(level, cottage);
                case GREENHOUSE, GARDEN -> {
                    if (ticks % 20 == 0) {
                        npc.swing(InteractionHand.MAIN_HAND);
                        Vec3 at = npc.position().add(npc.getLookAngle().multiply(1.2, 0.0, 1.2));
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.4, at.z, 4, 0.4, 0.2, 0.4,
                                0.0);
                        level.sendParticles(ParticleTypes.COMPOSTER, at.x, at.y + 0.3, at.z, 3, 0.3, 0.1, 0.3, 0.0);
                    }
                    if (ticks % 60 == 0) {
                        float yaw = npc.getYRot() + 60.0F + npc.getRandom().nextFloat() * 120.0F;
                        npc.setYRot(yaw);
                        npc.setYBodyRot(yaw);
                        npc.setYHeadRot(yaw);
                    }
                }
                default -> {
                }
            }
        }

        /** Nicky's play: running from spot to spot round the yard (his broom out), or after Scratchy at his pen. */
        private void run(ServerLevel level, Cottages.Cottage cottage) {
            if (npc.getNavigation().isDone() || ticks % 40 == 0) {
                Vec3 to;
                Rabbit scratchy = activity == Activity.CHASE ? Scratchy.find(level, cottage) : null;
                BlockPos yard = cottage.points().yard();
                if (scratchy != null && scratchy.distanceToSqr(Vec3.atBottomCenterOf(yard)) < 144.0) {
                    to = scratchy.position();
                    if (npc.distanceToSqr(scratchy) < 9.0) {
                        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.4, npc.getZ(), 1, 0.2,
                                0.1, 0.2, 0.0);
                        npc.getJumpControl().jump();
                    }
                } else {
                    to = Vec3.atBottomCenterOf(yard.offset(npc.getRandom().nextInt(7) - 3, 0,
                            npc.getRandom().nextInt(3) - 1));
                }
                hops++;
                npc.getNavigation().moveTo(to.x, to.y, to.z, 1.0);
            }
            if (hops > 8) {
                doing = 0;
            }
        }

        /** Rio walks to Nicky with a flower; they talk a little, and he holds it a while. */
        private void flower(ServerLevel level, Cottages.Cottage cottage) {
            AsgardianEntity nicky = CottageKeeper.resident(level, cottage, Role.NICKY);
            if (nicky == null || nicky.state().still() || ticks > 600) {
                activity = null;
                return;
            }
            if (npc.distanceToSqr(nicky) > 5.0) {
                if (--repath <= 0) {
                    repath = 20;
                    npc.getNavigation().moveTo(nicky, 0.6);
                }
                return;
            }
            npc.getNavigation().stop();
            nicky.getNavigation().stop();
            npc.setState(NpcState.TALKING, 100);
            npc.setPartner(nicky);
            nicky.setState(NpcState.TALKING, 100);
            nicky.setPartner(npc);
            npc.swing(InteractionHand.MAIN_HAND);
            npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            nicky.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.POPPY));
            ModAttachments.PERSISTENT_DATA.get(nicky).putLong(FLOWER_UNTIL, level.getGameTime() + 600);
            level.sendParticles(ParticleTypes.HEART, nicky.getX(), nicky.getY() + 1.4, nicky.getZ(), 2, 0.2, 0.1, 0.2,
                    0.0);
            activity = null;
        }
    }

    private CottageGoals() {
    }
}
