package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thanos, the Mad Titan, the boss of the Infinity Stones: a 3.3-block giant with a Double-Edged Sword in his right hand
 * and the Infinity Gauntlet on his left, whose sockets light one by one with the fight's clock (Power at the start,
 * then Space, Reality, Soul and Time every 90 seconds of combat; never the Mind Stone). He is a state machine ({@link
 * ThanosState}): he picks a target, walks to it, winds up a move (the tell everyone sees), strikes, rests; a new stone
 * is a surge that leaves him kneeling; a broken clench or a locked Gauntlet is a stagger.
 *
 * <p>The numbers of his moves are in {@link ThanosMove} and the config; what each move does is in {@link ThanosMoves}; the
 * damage he takes, in order, is {@link ThanosDefence}; the fight he belongs to is {@link ThanosFight}. The entity is
 * never saved (no fight survives a restart), never despawns by distance or in Peaceful (the fight ends itself) and
 * cannot be summoned: a fight makes him.
 *
 * <p>The state the clients need to draw him (stones, state, move and its tick, plates, flags, aim, target) is synced
 * entity data; the accessors are public for the model, the renderer and the bar.
 */
public class ThanosEntity extends Monster implements BossHooks {
    /** The Space Barrier is up: ranged damage does nothing (the Chaos Beam, the Mind Stone's beam and Stormbreaker pass). */
    public static final int FLAG_BARRIER = 1;
    /** The Reality Veil is up: projectiles near him turn to bubbles. */
    public static final int FLAG_VEIL = 2;
    /** The Gauntlet is locked: the stones do not answer. */
    public static final int FLAG_GAUNTLET_LOCKED = 4;
    /** He is hidden among his phantoms. */
    public static final int FLAG_HIDDEN = 8;
    /** He is enraged: his damage grows. */
    public static final int FLAG_ENRAGED = 16;
    /** The sword is away (thrown). */
    public static final int FLAG_SWORD_AWAY = 32;

    private static final EntityDataAccessor<Byte> DATA_STONES = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_NEXT_STONE_IN = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_MOVE = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_MOVE_TICK = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_PLATES = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_FLAGS = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_AIM = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ThanosEntity.class, EntityDataSerializers.INT);

    /**
     * The health of his bar in vanilla points. The vanilla maximum-health attribute cannot pass 1024, and he has 3000 or
     * more: his health is kept as a fraction of this bar and every damage is scaled by {@code REAL_HEALTH / maxPoints()}
     * (see {@link #maxPoints}). Everything the design says in points (3000, 6 percent a hit, a Gauntlet at 3 percent) is
     * in those {@linkplain #points() points}.
     */
    public static final double REAL_HEALTH = 1000.0;

    /** How long he is invulnerable after he lands, kneeling and rising. */
    public static final int ARRIVE_TICKS = 60;
    /** How long the death takes: the dust begins at tick 60. */
    public static final int DYING_TICKS = 100;
    /** How long a stagger lasts. */
    public static final int STAGGER_TICKS = 40;
    /** How long he keeps the target he chose before he looks again at who deserves it. */
    public static final int TARGET_HOLD_TICKS = 60;
    /**
     * How long he may fall through the portal before he counts as landed whatever is under him: a pool or a lake, a block
     * placed in the column during the countdown, a cobweb. (The arrival is invulnerable, so a Thanos that never landed
     * would stand in it, untouchable, until the fight timed out.)
     */
    public static final int LAND_TIMEOUT_TICKS = 200;
    /** How long he stays invisible among his phantoms, and how long he is seen when something reveals him. */
    public static final int HIDDEN_TICKS = 80;
    public static final int REVEAL_TICKS = 40;
    /** How long the snapshots he can rewind to go back. */
    private static final int HISTORY_INTERVAL = 5;

    /** A moment of his past, for the Rewind. */
    record Snapshot(int tick, float health, int plates, Vec3 position, boolean swordAway) {
    }

    /** Damage he took, for the second's cap and for the Rewind's trigger. */
    record Damage(int tick, float amount) {
    }

    // ---- the fight ----
    @Nullable
    private ThanosFight fight;
    private long combatTicks;
    private long lastStoneTick;
    private long idleTicks;
    private boolean warned;
    private int stoneBoost;
    private int arriveLeft = ARRIVE_TICKS;
    private boolean landed;
    private int fallTicks;
    private int dyingTicks;
    /** His maximum health in design points (3000 for one fighter, up to 7500). */
    private double virtualMax = ThanosClock.healthFor(1);
    private boolean leaving;
    private boolean passive;
    @Nullable
    private UUID lastBlow;

    // ---- the state machine ----
    @Nullable
    private MoveState move;
    private int recoverLeft;
    private int surgeLeft;
    private int staggerLeft;
    private int lockedLeft;
    private int barrierLeft;
    private int veilLeft;
    private int hiddenLeft;
    private int revealLeft;
    private float poise;
    private final EnumMap<ThanosMove, Integer> readyAt = new EnumMap<>(ThanosMove.class);
    private final Deque<ThanosMove> recentMoves = new ArrayDeque<>();
    private final Map<UUID, Integer> pulseReadyAt = new HashMap<>();
    private final Map<UUID, Integer> hiding = new HashMap<>();
    private int nextHideCheck;
    @Nullable
    private ThanosSwordEntity sword;

    // ---- the defence ----
    private final Deque<Damage> damageWindow = new ArrayDeque<>();
    private final Deque<Integer> rangedHits = new ArrayDeque<>();
    private int lastRangedTick = -1000;
    private int chaosBeamTicks;
    private int lastChaosBeamTick = -1000;
    private int lastSlamTear = -1000;
    private final Deque<Snapshot> history = new ArrayDeque<>();
    private final Map<UUID, Deque<Damage>> recentFrom = new HashMap<>();

    // ---- the animation clock (both sides count; only the clients read it) ----
    /** The state and move the clock below was last started for. */
    private ThanosState clockState = ThanosState.ARRIVING;
    private int clockMove = -2;
    /** Ticks since the state last changed. */
    private int stateClock;
    /** Ticks since the move last began (kept in step with the move tick the server syncs). */
    private int moveClock;
    /** Ticks he has stood on the ground since the portal let go of him (clients: the landing is not synced). */
    private int groundClock;
    /**
     * Whether the viewer put him in his pose: the partial tick is then ignored, so a pose held for a photograph holds
     * still.
     */
    private boolean shown;

    // ---- walking and aiming ----
    @Nullable
    private UUID targetId;
    /** The game tick until which he keeps the target he chose (the choice is judged again only at his idle tick after it). */
    private int targetUntil;
    private int stuckTicks;
    private double stuckDistance = Double.MAX_VALUE;
    private int stuckLeaps;
    private int lastSpeech;
    private List<ServerPlayer> mortalsCache = List.of();
    private int mortalsAt = -1;

    public ThanosEntity(EntityType<? extends ThanosEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, REAL_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.4)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 1000.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STONES, (byte) 1);
        builder.define(DATA_NEXT_STONE_IN, -1);
        builder.define(DATA_STATE, (byte) ThanosState.ARRIVING.ordinal());
        builder.define(DATA_MOVE, (byte) -1);
        builder.define(DATA_MOVE_TICK, 0);
        builder.define(DATA_PLATES, (byte) ThanosDefence.ALL_PLATES);
        builder.define(DATA_FLAGS, 0);
        builder.define(DATA_AIM, new Vector3f(0.0F, 0.0F, 1.0F));
        builder.define(DATA_TARGET, -1);
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the clients and the tests read

    /** How many sockets of the Gauntlet are lit (Power is 1, and there are never more than 5). */
    public int stones() {
        return entityData.get(DATA_STONES);
    }

    /** Whether the stone is lit. */
    public boolean hasStone(StoneKind stone) {
        return stone.ordinal() < stones() && stone != StoneKind.MIND;
    }

    /** Whether he has the Time Stone: Quicksilver's slow time does not touch him. */
    public boolean hasTimeStone() {
        return hasStone(StoneKind.TIME);
    }

    /** The newest stone he holds. */
    public StoneKind newestStone() {
        StoneKind stone = StoneKind.nth(stones());
        return stone != null ? stone : StoneKind.POWER;
    }

    /** Ticks until the next stone arrives, or -1 if there is none to come. */
    public int nextStoneIn() {
        return entityData.get(DATA_NEXT_STONE_IN);
    }

    public ThanosState state() {
        ThanosState[] states = ThanosState.values();
        return states[Mth.clamp(entityData.get(DATA_STATE), 0, states.length - 1)];
    }

    /** The move he is making, or null. */
    @Nullable
    public ThanosMove currentMove() {
        int index = entityData.get(DATA_MOVE);
        return index < 0 || index >= ThanosMove.values().length ? null : ThanosMove.values()[index];
    }

    /** The ticks since his move began (the wind-up included). */
    public int moveTick() {
        return entityData.get(DATA_MOVE_TICK);
    }

    /** The mask of his intact plates ({@link ThanosDefence#HELMET} and the rest). */
    public int plates() {
        return entityData.get(DATA_PLATES);
    }

    public int flags() {
        return entityData.get(DATA_FLAGS);
    }

    public boolean flag(int flag) {
        return (flags() & flag) != 0;
    }

    /** Where his move is aimed (a unit direction for a blow or beam, or a point for those that mark a spot). */
    public Vec3 aim() {
        Vector3f aim = entityData.get(DATA_AIM);
        return new Vec3(aim.x, aim.y, aim.z);
    }

    /** The id of the creature he faces, or -1. */
    public int targetEntityId() {
        return entityData.get(DATA_TARGET);
    }

    /** The ticks of combat so far (the clock of the stones). */
    public long combatTicks() {
        return combatTicks;
    }

    /** Whether the Gauntlet is locked (a stagger, a web). */
    public boolean gauntletLocked() {
        return lockedLeft > 0;
    }

    /** Whether the sword is out of his hand. */
    public boolean swordAway() {
        return flag(FLAG_SWORD_AWAY);
    }

    /** Whether he is invisible among his phantoms. */
    public boolean hidden() {
        return flag(FLAG_HIDDEN);
    }

    /** What he does to everyone: the difficulty, the stones beyond the first and the enrage. */
    public float damageScale() {
        return ThanosClock.damageScale(stones(), combatTicks - lastStoneTick);
    }

    /** The damage each fighter has done to him: the fight's ledger. */
    public Map<UUID, Float> ledger() {
        Map<UUID, Float> ledger = new HashMap<>();
        if (fight != null) {
            for (var participant : fight.participants()) {
                ledger.put(participant.id(), participant.damage());
            }
        }
        return ledger;
    }

    @Nullable
    public ThanosFight fight() {
        return fight;
    }

    void attach(ThanosFight fight) {
        this.fight = fight;
    }

    // ---- the places of his body ----

    /** The way he faces, as a flat unit vector. */
    public Vec3 forward() {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    /** Where the Gauntlet's hand is (his left, a little ahead, at the height of the chest). */
    public Vec3 handPosition() {
        Vec3 forward = forward();
        Vec3 left = new Vec3(forward.z, 0.0, -forward.x);
        return position().add(left.scale(0.95)).add(forward.scale(0.5)).add(0.0, 2.2, 0.0);
    }

    /** Where the sword's hand is (his right). */
    public Vec3 swordHandPosition() {
        Vec3 forward = forward();
        Vec3 left = new Vec3(forward.z, 0.0, -forward.x);
        return position().subtract(left.scale(0.95)).add(forward.scale(0.5)).add(0.0, 2.0, 0.0);
    }

    /** His chest: where a beam leaves from and where his eyes look. */
    public Vec3 chest() {
        return position().add(0.0, 2.3, 0.0);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The animation clock

    @Override
    public void tick() {
        super.tick();
        tickAnimation();
    }

    /**
     * Counts the ticks since the state and the move began, so that the model needs no clock of its own: a state has no
     * synced tick except a move's, and the move's arrives with the network's jitter, so the count runs locally and is
     * only pulled forward to the synced tick when that is ahead of it.
     */
    private void tickAnimation() {
        ThanosState state = state();
        int move = entityData.get(DATA_MOVE);
        int synced = moveTick();
        if (state != clockState) {
            clockState = state;
            stateClock = 0;
        } else {
            stateClock++;
        }
        if (move != clockMove) {
            clockMove = move;
            moveClock = synced;
        } else {
            moveClock = Math.max(moveClock + 1, synced);
        }
        groundClock = state == ThanosState.ARRIVING && onGround() ? groundClock + 1 : 0;
    }

    /**
     * The ticks since his state last changed, with the partial tick: how far into a surge, a stagger or the death he
     * is.
     */
    public float stateTime(float partialTick) {
        return stateClock + (shown ? 0.0F : partialTick);
    }

    /** The ticks since he landed, with the partial tick, or -1 while he is still falling through the portal. */
    public float landedTime(float partialTick) {
        return groundClock > 0 ? groundClock - 1 + (shown ? 0.0F : partialTick) : -1.0F;
    }

    /** The ticks since his move began, with the partial tick: the time the pose of the move is read at. */
    public float moveTime(float partialTick) {
        return moveClock + (shown ? 0.0F : partialTick);
    }

    /**
     * Whether this is the client-only copy of {@code /enhanced_model}: it is a picture, not a Thanos (no bar, no music,
     * no fog).
     */
    public boolean shownByViewer() {
        return shown;
    }

    /**
     * Puts the client-only copy that {@code /enhanced_model} shows in a state: what the model reads, and nothing else.
     * (The viewer sets it every tick; no server ever calls this.)
     *
     * @param moveTick the tick of the move, or the time in the state for those that have no move
     */
    public void showState(ThanosState state, @Nullable ThanosMove move, int moveTick, int stones, int plates, int flags, Vec3 aim) {
        entityData.set(DATA_STATE, (byte) state.ordinal());
        entityData.set(DATA_MOVE, (byte) (move == null ? -1 : move.ordinal()));
        entityData.set(DATA_MOVE_TICK, moveTick);
        entityData.set(DATA_STONES, (byte) Mth.clamp(stones, 1, StoneKind.granted().size()));
        entityData.set(DATA_PLATES, (byte) (plates & ThanosDefence.ALL_PLATES));
        entityData.set(DATA_FLAGS, flags);
        entityData.set(DATA_AIM, new Vector3f((float) aim.x, (float) aim.y, (float) aim.z));
        clockState = state;
        clockMove = move == null ? -1 : move.ordinal();
        stateClock = moveTick;
        moveClock = moveTick;
        groundClock = moveTick + 1;
        shown = true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Test hooks and the lane's own controls

    /** Sets the clock of the fight: stones come as if that much combat had passed. */
    public void forceCombatTicks(long ticks) {
        this.combatTicks = Math.max(0L, ticks);
        while (stones() < ThanosClock.stonesAt(combatTicks)) {
            addStone(stones() + 1 >= ThanosClock.stonesAt(combatTicks));
        }
    }

    /** Gives him stones up to {@code stones} at once, in order; never more than he can hold (five, and {@code maxStones}). */
    public void forceStones(int stones) {
        int limit = Math.min(Math.min(stones, ThanosBossConfig.MAX_STONES.get()), StoneKind.granted().size());
        while (stones() < limit) {
            addStone(stones() + 1 >= limit);
        }
        combatTicks = Math.max(combatTicks, (long) (stones() - 1) * ThanosBossConfig.STONE_INTERVAL_TICKS.get());
    }

    /** Starts a move at once (as if he had chosen it), without waiting for his rest or his cooldowns. */
    public void forceMove(ThanosMove move, @Nullable LivingEntity target) {
        if (state() == ThanosState.ARRIVING || state() == ThanosState.DYING) {
            return;
        }
        if (this.move != null) {
            interruptMove();
        }
        surgeLeft = 0;
        staggerLeft = 0;
        recoverLeft = 0;
        focusOn(target);
        beginMove(move, target, 0);
    }

    /** A passive Thanos chooses no move and walks nowhere (the clock, the defence and the moves forced on him still run). */
    @VisibleForTesting
    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    /** Recomputes his health for the fighters there are now, keeping the fraction he has left. */
    @VisibleForTesting
    public void rescaleToFighters() {
        rescale(mortals().size());
    }

    /**
     * The move he would choose against {@code target} now, or null if he would walk closer: the weighted choice, the
     * triggers and the cooldowns, without starting anything.
     */
    @VisibleForTesting
    @Nullable
    public ThanosMove pickMove(LivingEntity target) {
        ThanosMoves.Choice choice = ThanosMoves.choose(this, target);
        return choice == null ? null : choice.move();
    }

    /**
     * Whom he would go after now, as his idle tick decides it (and he then faces them): the Vision first, then the one who
     * dealt him the most lately, then the nearest, and he keeps the choice for sixty ticks.
     *
     * @param fresh forget the hold on the last choice
     */
    @VisibleForTesting
    @Nullable
    public LivingEntity chooseTargetNow(boolean fresh) {
        if (fresh) {
            targetUntil = 0;
        }
        LivingEntity chosen = chooseTarget((ServerLevel) level());
        focusOn(chosen);
        return chosen;
    }

    /** Sets how long he has been falling through the portal (a test that does not want to wait for the landing's deadline). */
    @VisibleForTesting
    public void forceFallTicks(int ticks) {
        this.fallTicks = ticks;
    }

    /** Whether the portal has let go of him: he has landed (or counts as landed) and is rising from it. */
    @VisibleForTesting
    public boolean hasLanded() {
        return landed;
    }

    /**
     * Frees him as the stuck rule does when he has made no progress toward {@code target}: to a free spot near it, if a
     * teleport to there is allowed him.
     */
    @VisibleForTesting
    public void forceUnstick(LivingEntity target) {
        if (level() instanceof ServerLevel level) {
            freeSpotNear(level, target);
        }
    }

    /** Skips the arrival: he stands ready at once. */
    public void forceArrived() {
        landed = true;
        arriveLeft = 0;
        setState(ThanosState.IDLE);
    }

    /** Puts him at a state (a test that wants him already surging or staggered). */
    void forceState(ThanosState state) {
        setState(state);
    }

    /** The move being made, for the tests. */
    @Nullable
    MoveState moveState() {
        return move;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Setting what is synced

    void setState(ThanosState state) {
        entityData.set(DATA_STATE, (byte) state.ordinal());
    }

    private void setFlag(int flag, boolean on) {
        int flags = flags();
        int next = on ? flags | flag : flags & ~flag;
        if (next != flags) {
            entityData.set(DATA_FLAGS, next);
        }
    }

    void setAim(Vec3 aim) {
        entityData.set(DATA_AIM, new Vector3f((float) aim.x, (float) aim.y, (float) aim.z));
    }

    /** Remembers whom he faces (the id the tells are drawn toward). */
    private void focusOn(@Nullable LivingEntity target) {
        targetId = target == null ? null : target.getUUID();
        entityData.set(DATA_TARGET, target == null ? -1 : target.getId());
    }

    // ---------------------------------------------------------------------------------------------------------
    // The fighters

    /**
     * The players he fights: alive, in his dimension and arena, neither Creative nor Spectator, and not away in the Soul
     * World. Computed once per tick.
     */
    public List<ServerPlayer> mortals() {
        if (mortalsAt == tickCount) {
            return mortalsCache;
        }
        List<ServerPlayer> found = new ArrayList<>();
        if (fight != null) {
            for (ServerPlayer player : fight.present()) {
                if (!player.isCreative() && !player.hasEffect(ModEffects.SOULWORLD)) {
                    found.add(player);
                }
            }
        } else if (level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (player.isAlive() && !player.isCreative() && !player.isSpectator() && distanceToSqr(player) < 64.0 * 64.0) {
                    found.add(player);
                }
            }
        }
        mortalsCache = found;
        mortalsAt = tickCount;
        return found;
    }

    /** The creature he faces, if it is still there. */
    @Nullable
    public LivingEntity target() {
        if (targetId == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        Entity found = level.getEntity(targetId);
        return found instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (fight == null || fight.isFinished()) {
            // A Thanos with no fight is an orphan.
            discard();
            return;
        }
        if (leaving) {
            navigation.stop();
            return;
        }
        tickFlags();
        tickHistory();
        ThanosState state = state();
        switch (state) {
            case ARRIVING -> tickArriving(level);
            case DYING -> tickDying(level);
            case SURGE -> {
                if (--surgeLeft <= 0) {
                    setState(ThanosState.IDLE);
                }
            }
            case STAGGERED -> {
                if (--staggerLeft <= 0) {
                    setState(ThanosState.IDLE);
                }
            }
            case RECOVER -> {
                if (--recoverLeft <= 0) {
                    setState(ThanosState.IDLE);
                }
            }
            case TELL, ACTIVE -> tickMove(level);
            case IDLE -> tickIdle(level);
        }
        if (state != ThanosState.ARRIVING && state != ThanosState.DYING) {
            tickClock(level);
            tickHiding(level);
            tickFootsteps(level);
            if (!passive && state != ThanosState.IDLE) {
                // The stuck rule keeps counting through a move, a rest, a stagger: only the leaping waits for the idle tick.
                LivingEntity hunted = target();
                if (hunted != null) {
                    trackProgress(level, hunted, false);
                }
            }
        }
    }

    /** Counts down the things that wear off: the Barrier, the Veil, the lock, the hiding. */
    private void tickFlags() {
        if (barrierLeft > 0 && --barrierLeft == 0) {
            setFlag(FLAG_BARRIER, false);
        }
        if (veilLeft > 0) {
            veilLeft--;
            if (veilLeft % 2 == 0) {
                StonePowers.bubbleProjectiles((ServerLevel) level(), position().add(0, 1.5, 0), 12.0, projectile -> projectile.getOwner() != this);
            }
            if (veilLeft == 0) {
                setFlag(FLAG_VEIL, false);
            }
        }
        if (lockedLeft > 0 && --lockedLeft == 0) {
            setFlag(FLAG_GAUNTLET_LOCKED, false);
        }
        if (stoneBoost > 0) {
            stoneBoost--;
        }
        if (hiddenLeft > 0) {
            hiddenLeft--;
            if (hasEffect(MobEffects.GLOWING)) {
                reveal();
            }
            if (revealLeft > 0) {
                revealLeft--;
            }
            boolean hide = hiddenLeft > 0 && revealLeft == 0;
            setFlag(FLAG_HIDDEN, hide);
            setInvisible(hide);
            if (hiddenLeft == 0) {
                setFlag(FLAG_HIDDEN, false);
                setInvisible(false);
            }
        }
        if (lastSpeech > 0) {
            lastSpeech--;
        }
    }

    /** Hidden among his phantoms for {@link #HIDDEN_TICKS}. */
    void hide() {
        hiddenLeft = HIDDEN_TICKS;
        revealLeft = 0;
        setFlag(FLAG_HIDDEN, true);
        setInvisible(true);
    }

    /** Something shows him: he is seen for {@link #REVEAL_TICKS}. */
    public void reveal() {
        if (hiddenLeft > 0) {
            revealLeft = REVEAL_TICKS;
            setFlag(FLAG_HIDDEN, false);
            setInvisible(false);
        }
    }

    /** A hidden Thanos still kicks up dust where he walks. */
    private void tickFootsteps(ServerLevel level) {
        if (hidden() && tickCount % 4 == 0 && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            BlockState ground = level.getBlockState(blockPosition().below());
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 8, 0.5, 0.05, 0.5, 0.05);
            }
        }
    }

    /** One snapshot every few ticks, for the Rewind. */
    private void tickHistory() {
        if (tickCount % HISTORY_INTERVAL != 0 || state() == ThanosState.ARRIVING || state() == ThanosState.DYING) {
            return;
        }
        history.addLast(new Snapshot(tickCount, getHealth(), plates(), position(), swordAway()));
        int keep = ThanosBossConfig.REWIND_SECONDS.get() * 20 / HISTORY_INTERVAL + 2;
        while (history.size() > keep) {
            history.removeFirst();
        }
        long cutoff = tickCount - 20L * 20;
        while (!damageWindow.isEmpty() && damageWindow.peekFirst().tick() < cutoff) {
            damageWindow.removeFirst();
        }
    }

    // ---- arriving ----

    /** Lets him fall from the portal: invulnerable until he lands and has risen. */
    void beginArrival() {
        landed = false;
        fallTicks = 0;
        arriveLeft = ARRIVE_TICKS;
        setState(ThanosState.ARRIVING);
        rescale(1);
        setHealth(getMaxHealth());
    }

    private void tickArriving(ServerLevel level) {
        fallDistance = 0.0F;
        if (!landed) {
            // He has landed on the ground, in water or lava (where he floats), or has fallen long enough.
            if (onGround() || isInWater() || isInLava() || ++fallTicks > LAND_TIMEOUT_TICKS) {
                landed = true;
                landingEffects(level);
            }
            return;
        }
        getNavigation().stop();
        if (--arriveLeft <= 0) {
            setState(ThanosState.IDLE);
            rescale(mortals().size());
            fight.arrived();
        }
    }

    /** The landing: a cosmetic ring that shoves anyone close, and the roar. */
    private void landingEffects(ServerLevel level) {
        ThanosEffects.shockRing(level, position(), 6.0F, 10, StoneKind.POWER.color());
        ThanosSounds.ARRIVE.play(level, position());
        ThanosSounds.ROAR.play(level, position());
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0), e -> e != this && e.isAlive())) {
            StonePowers.shove(near, position(), 0.6, 0.0);
        }
    }

    // ---- the clock and the stones ----

    private void tickClock(ServerLevel level) {
        if (mortals().isEmpty()) {
            idleTicks++;
            if (idleTicks > 1200 && getHealth() < getMaxHealth()) {
                heal((float) (getMaxHealth() * ThanosBossConfig.IDLE_REGEN_PER_MINUTE.get() / 1200.0));
            }
            return;
        }
        idleTicks = 0;
        combatTicks++;
        int stones = stones();
        int nextIn = ThanosClock.nextStoneIn(combatTicks, stones);
        if (nextIn != nextStoneIn()) {
            entityData.set(DATA_NEXT_STONE_IN, nextIn);
        }
        if (nextIn > 0 && nextIn <= ThanosClock.WARNING_TICKS && !warned) {
            warned = true;
            StoneKind coming = StoneKind.nth(stones + 1);
            if (coming != null) {
                fight.tell(Component.translatable("message.enhanced.thanos.stone.soon", coming.displayName()).withColor(coming.color()), true);
                coming.voice().use(level, position(), 0.3F, 1.0F);
            }
        }
        if (stones < ThanosClock.stonesAt(combatTicks)) {
            addStone(true);
        }
        boolean enraged = ThanosClock.enrage(combatTicks - lastStoneTick) > 0.0 && nextIn < 0;
        if (enraged != flag(FLAG_ENRAGED)) {
            setFlag(FLAG_ENRAGED, enraged);
            if (enraged) {
                ThanosSounds.ROAR.play(level, position(), 1.0F, 0.8F);
            }
        }
    }

    /**
     * The next stone fuses into the Gauntlet. He is hurt by it (the film's "sudden and painful surge"): he kneels for the
     * surge and takes extra damage, and the arena sees it coming.
     *
     * @param surge whether this is the last of the stones being gained now (the others only light their sockets)
     */
    private void addStone(boolean surge) {
        int stones = stones();
        StoneKind stone = StoneKind.nth(stones + 1);
        if (stone == null || stone == StoneKind.MIND || stones >= ThanosBossConfig.MAX_STONES.get()) {
            return;
        }
        entityData.set(DATA_STONES, (byte) (stones + 1));
        entityData.set(DATA_NEXT_STONE_IN, ThanosClock.nextStoneIn(combatTicks, stones + 1));
        lastStoneTick = combatTicks;
        warned = false;
        history.clear();
        stoneBoost = 20;
        if (stone == StoneKind.TIME) {
            // The Infinity Beam comes ten seconds after the fifth stone.
            delay(ThanosMove.INFINITY_BEAM, 200);
        }
        if (move != null) {
            interruptMove();
        }
        if (state() == ThanosState.ARRIVING || state() == ThanosState.DYING || !(level() instanceof ServerLevel level)) {
            return;
        }
        rescale(mortals().size());
        if (surge) {
            setState(ThanosState.SURGE);
            surgeLeft = ThanosBossConfig.SURGE_TICKS.get();
            navigation.stop();
        }
        if (fight != null) {
            fight.stoneArrived(stone, this);
        }
        Vec3 sky = position().add(0.0, 30.0, 0.0);
        ThanosEffects.star(level, sky, handPosition(), 24, stone.color());
        ThanosEffects.shockRing(level, position(), 6.0F, 10, stone.color());
        ThanosEffects.flash(level, position(), 20);
        ThanosSounds.ROAR.play(level, position());
        ThanosSounds.SURGE.play(level, position());
        stone.voice().use(level, position(), 1.0F, 1.0F);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0), e -> e != this && e.isAlive())) {
            StonePowers.shove(near, position(), 0.6, 0.0);
        }
    }

    /**
     * Recomputes his maximum health for the fighters there are: more points in the same bar, so the fraction he has left
     * is kept.
     */
    void rescale(int fighters) {
        virtualMax = ThanosClock.healthFor(fighters);
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null && Math.abs(health.getBaseValue() - REAL_HEALTH) > 1.0E-6) {
            health.setBaseValue(REAL_HEALTH);
        }
    }

    /** His maximum health in the points of the design: 3000 for one fighter, 4500 for two, up to 7500. */
    public double maxPoints() {
        return virtualMax;
    }

    /** His health in those points. */
    public double points() {
        return getMaxHealth() <= 0.0F ? 0.0 : getHealth() / getMaxHealth() * virtualMax;
    }

    /** Points of the design as health of the bar (what the bar loses for that many points of damage). */
    float toBar(double points) {
        return (float) (points * getMaxHealth() / virtualMax);
    }

    /** Health of the bar as points of the design. */
    float toPoints(float bar) {
        return (float) (bar * virtualMax / getMaxHealth());
    }

    /** Heals him by so many points of the design. */
    void healPoints(float points) {
        heal(toBar(points));
    }

    // ---- idle: choose a target, walk to it, choose a move ----

    private void tickIdle(ServerLevel level) {
        if (passive) {
            navigation.stop();
            return;
        }
        LivingEntity target = chooseTarget(level);
        focusOn(target);
        if (target == null) {
            navigation.stop();
            return;
        }
        faceToward(target.position());
        // The stuck rule: no progress toward a target for four seconds (counted all the while; acted on between moves).
        if (trackProgress(level, target, true)) {
            return;
        }
        ThanosMoves.Choice next = ThanosMoves.choose(this, target);
        if (next != null) {
            beginMove(next.move(), next.target(), 0);
            return;
        }
        double distance = distanceTo(target);
        if (distance > 3.0) {
            navigation.moveTo(target, 1.0);
        } else {
            navigation.stop();
        }
    }

    /** Looks at a point and turns his body to it. */
    void faceToward(Vec3 point) {
        double dx = point.x - getX();
        double dz = point.z - getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        yRotO = yaw;
        yBodyRotO = yaw;
        getLookControl().setLookAt(point.x, point.y, point.z);
    }

    /** @return whether it started a Leap (he then has his move) */
    private boolean trackProgress(ServerLevel level, LivingEntity target, boolean mayAct) {
        double distance = distanceTo(target);
        if (distance < 4.5 || distance > 40.0) {
            stuckTicks = 0;
            stuckDistance = distance;
            stuckLeaps = 0;
            return false;
        }
        if (distance < stuckDistance - 0.5) {
            stuckDistance = distance;
            stuckTicks = 0;
            return false;
        }
        if (stuckTicks < 80) {
            stuckTicks++;
        }
        if (stuckTicks < 80 || !mayAct) {
            return false;
        }
        stuckTicks = 0;
        stuckDistance = distance;
        if (stuckLeaps < 2 && distance >= 5.0) {
            stuckLeaps++;
            beginMove(ThanosMove.LEAP, target, 0);
            return true;
        }
        stuckLeaps = 0;
        freeSpotNear(level, target);
        return false;
    }

    /**
     * Moves him to the nearest free ground near the target (at the target's own height), with a purple flash. It is a
     * teleport like any of his, so a Hex wall (or anything else that refuses a step) is asked first: a Thanos in a Hex is
     * kept in it, however far the one he hunts stands outside, and one outside does not get in.
     */
    private void freeSpotNear(ServerLevel level, LivingEntity target) {
        for (int radius = 2; radius <= 6; radius += 2) {
            for (int k = 0; k < 8; k++) {
                double angle = k * Math.PI / 4.0;
                Vec3 spot = standingSpotAt(level, target.getX() + Math.cos(angle) * radius, target.getY(), target.getZ() + Math.sin(angle) * radius);
                if (spot != null && mayTeleportTo(level, spot)) {
                    flashAt(level, position());
                    moveTo(spot.x, spot.y, spot.z, getYRot(), getXRot());
                    navigation.stop();
                    flashAt(level, spot);
                    return;
                }
            }
        }
    }

    /**
     * Where he could stand in the column of {@code (x, z)} within four blocks of {@code y}: solid dry ground under free room for
     * his whole body (the same height as whoever he hunts, so a roof or a cave over the surface is no trouble), or null.
     */
    @Nullable
    private Vec3 standingSpotAt(ServerLevel level, double x, double y, double z) {
        int base = Mth.floor(y);
        for (int step : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4}) {
            BlockPos feet = new BlockPos(Mth.floor(x), base + step, Mth.floor(z));
            BlockPos under = feet.below();
            BlockState ground = level.getBlockState(under);
            if (!ground.isFaceSturdy(level, under, net.minecraft.core.Direction.UP) || !ground.getFluidState().isEmpty()
                    || ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CACTUS)) {
                continue;
            }
            Vec3 spot = Vec3.atBottomCenterOf(feet);
            if (com.kolodkingal.enhanced.ability.Teleports.fits(this, spot) && !level.containsAnyLiquid(getBoundingBox().move(spot.subtract(position())))) {
                return spot;
            }
        }
        return null;
    }

    private void flashAt(ServerLevel level, Vec3 at) {
        level.sendParticles(new com.kolodkingal.enhanced.particle.EnergyParticleOptions(StoneKind.POWER.color(), 1.6F),
                at.x, at.y + 1.5, at.z, 24, 0.5, 1.2, 0.5, 0.1);
    }

    /**
     * Picks whom to go after: a Vision first (the stone he hunts), then whoever dealt him the most in the last ten
     * seconds, then the nearest. He keeps a target for sixty ticks of the game (not sixty idle ticks: he is idle for a
     * moment between one move and the next, so a count of those would hold the first target for minutes), and goes for the
     * nearest of Loki's copies until he has the Soul Stone.
     */
    @Nullable
    private LivingEntity chooseTarget(ServerLevel level) {
        List<ServerPlayer> mortals = mortals();
        LivingEntity current = target();
        boolean valid = current != null && (current instanceof ServerPlayer player ? mortals.contains(player) : current.isAlive());
        if (valid && tickCount < targetUntil) {
            return current;
        }
        ServerPlayer best = null;
        double bestScore = -1.0;
        for (ServerPlayer player : mortals) {
            double score = distanceToSqr(player) > 0 ? 1.0 / (1.0 + distanceToSqr(player) * 0.01) : 1.0;
            if (Powers.is(player, Power.VISION)) {
                score += 1000.0;
            }
            score += 10.0 * recentDamageFrom(player.getUUID());
            if (score > bestScore) {
                bestScore = score;
                best = player;
            }
        }
        LivingEntity chosen = best;
        if (stones() < 4) {
            LokiIllusionEntity lure = null;
            double lureDistance = 24.0 * 24.0;
            for (LokiIllusionEntity illusion : level.getEntitiesOfClass(LokiIllusionEntity.class, getBoundingBox().inflate(24.0))) {
                double distance = distanceToSqr(illusion);
                if (distance < lureDistance && (chosen == null || distance < distanceToSqr(chosen))) {
                    lure = illusion;
                    lureDistance = distance;
                }
            }
            if (lure != null) {
                chosen = lure;
            }
        }
        targetUntil = tickCount + TARGET_HOLD_TICKS;
        return chosen;
    }

    private float recentDamageFrom(UUID player) {
        Deque<Damage> damage = recentFrom.get(player);
        if (damage == null) {
            return 0.0F;
        }
        float total = 0.0F;
        for (Damage entry : damage) {
            if (tickCount - entry.tick() <= 200) {
                total += entry.amount();
            }
        }
        return total;
    }

    // ---- moves ----

    /** Begins a move: the wind-up, with its first tell. {@code tellOverride} shortens the wind-up (0: the move's own). */
    void beginMove(ThanosMove which, @Nullable LivingEntity target, int tellOverride) {
        int tell = tellOverride > 0 ? Math.min(tellOverride, which.tell()) : which.tell();
        MoveState state = new MoveState(which, target, tell);
        this.move = state;
        readyAt.put(which, tickCount + which.cooldown());
        recentMoves.addLast(which);
        while (recentMoves.size() > 2) {
            recentMoves.removeFirst();
        }
        poise = 0.0F;
        navigation.stop();
        focusOn(target);
        setState(ThanosState.TELL);
        entityData.set(DATA_MOVE, (byte) which.ordinal());
        entityData.set(DATA_MOVE_TICK, 0);
        if (hiddenLeft > 0 && which != ThanosMove.PHANTOMS) {
            reveal();
        }
        ThanosMoves.begin(this, state);
    }

    private void tickMove(ServerLevel level) {
        MoveState state = move;
        if (state == null) {
            setState(ThanosState.IDLE);
            return;
        }
        state.t++;
        entityData.set(DATA_MOVE_TICK, state.t);
        ThanosMoves.tick(this, state);
        if (this.move != state) {
            return; // the move ended or was interrupted from inside
        }
        if (state.t >= state.tell && state() == ThanosState.TELL) {
            setState(ThanosState.ACTIVE);
        }
        if (state.t >= state.tell + state.move.after()) {
            endMove();
        }
    }

    /** The move is over: the rest, then he chooses again. A Portal Step's chained move begins at once. */
    void endMove() {
        MoveState state = move;
        ThanosMoves.finish(this, state);
        move = null;
        entityData.set(DATA_MOVE, (byte) -1);
        entityData.set(DATA_MOVE_TICK, 0);
        if (state != null && state.chain != null && state.target != null && state.target.isAlive()) {
            beginMove(state.chain, state.target, 8);
            return;
        }
        recoverLeft = ThanosClock.recoverTicks(stones(), Math.max(1, mortals().size()));
        setState(ThanosState.RECOVER);
    }

    /** The move is broken off (a stagger, a new stone): whatever it held is let go. */
    void interruptMove() {
        MoveState state = move;
        if (state == null) {
            return;
        }
        ThanosMoves.interrupted(this, state);
        move = null;
        entityData.set(DATA_MOVE, (byte) -1);
        entityData.set(DATA_MOVE_TICK, 0);
    }

    /** Whether he is in the wind-up of a move that needs the Gauntlet: a clench poise can break. */
    public boolean clenching() {
        return move != null && state() == ThanosState.TELL && move.move.clench();
    }

    /** He stumbles for {@link #STAGGER_TICKS}: whatever he was doing stops. */
    void stagger() {
        if (state() == ThanosState.ARRIVING || state() == ThanosState.DYING) {
            return;
        }
        interruptMove();
        poise = 0.0F;
        staggerLeft = STAGGER_TICKS;
        setState(ThanosState.STAGGERED);
        navigation.stop();
        lockedLeft = Math.max(lockedLeft, ThanosBossConfig.LOCK_TICKS.get());
        setFlag(FLAG_GAUNTLET_LOCKED, true);
        if (level() instanceof ServerLevel level) {
            ThanosSounds.HURT.play(level, position());
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2.5, getZ(), 12, 0.5, 0.4, 0.5, 0.1);
        }
    }

    /**
     * The Gauntlet is held shut: his stones do not answer for {@code ticks}. A wind-up in progress breaks (he is
     * staggered); a move of the sword and the fist goes on.
     *
     * @param source whoever holds it (a Spider-Man's web), or null
     */
    public void lockGauntlet(int ticks, @Nullable Entity source) {
        if (ticks <= 0 || state() == ThanosState.ARRIVING || state() == ThanosState.DYING) {
            return;
        }
        lockedLeft = Math.max(lockedLeft, ticks);
        setFlag(FLAG_GAUNTLET_LOCKED, true);
        if (clenching()) {
            stagger();
            lockedLeft = Math.max(lockedLeft, ticks);
            setFlag(FLAG_GAUNTLET_LOCKED, true);
        }
    }

    // ---- the sword ----

    void swordThrown(ThanosSwordEntity thrown) {
        sword = thrown;
        setFlag(FLAG_SWORD_AWAY, true);
    }

    /** The sword is back in his hand. */
    void catchSword(ThanosSwordEntity returning) {
        if (level() instanceof ServerLevel level) {
            ThanosSounds.HIT.play(level, handPosition(), 0.6F, 1.3F);
        }
        returning.discard();
    }

    /** The sword entity is gone, however it went. */
    void swordGone(ThanosSwordEntity gone) {
        if (sword == gone) {
            sword = null;
            setFlag(FLAG_SWORD_AWAY, false);
        }
    }

    /** Takes the sword back into his hand wherever it is (the Rewind). */
    void recallSword() {
        if (sword != null) {
            sword.discard();
        }
        sword = null;
        setFlag(FLAG_SWORD_AWAY, false);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Hiding fighters: the Power Pulse

    /** Checks the fighters he can neither reach nor see; one of them long enough is found by the Pulse. */
    private void tickHiding(ServerLevel level) {
        if (tickCount < nextHideCheck) {
            return;
        }
        nextHideCheck = tickCount + 10;
        for (ServerPlayer player : mortals()) {
            boolean seen = hasLineOfSight(player);
            if (!seen && distanceToSqr(player) < 48.0 * 48.0) {
                var path = getNavigation().createPath(player, 0);
                seen = path != null && path.canReach();
            }
            if (seen || distanceToSqr(player) >= 48.0 * 48.0) {
                hiding.remove(player.getUUID());
            } else {
                hiding.merge(player.getUUID(), 10, Integer::sum);
            }
        }
        hiding.keySet().removeIf(id -> mortals().stream().noneMatch(p -> p.getUUID().equals(id)));
    }

    /** The fighter who has hidden from him long enough and may be found now, or null. */
    @Nullable
    ServerPlayer hiddenFighter() {
        for (ServerPlayer player : mortals()) {
            int hidden = hiding.getOrDefault(player.getUUID(), 0);
            if (hidden >= ThanosBossConfig.UNREACHABLE_TICKS.get() && pulseReadyAt.getOrDefault(player.getUUID(), 0) <= tickCount) {
                return player;
            }
        }
        return null;
    }

    /** Starts the Pulse's cooldown for that player. */
    void pulsed(ServerPlayer player) {
        pulseReadyAt.put(player.getUUID(), tickCount + ThanosBossConfig.PULSE_COOLDOWN.get());
    }

    /** Which way the Infinity Beam sweeps this time; it alternates. */
    boolean infinityDirection() {
        return infinityFlipped;
    }

    void flipInfinityDirection() {
        infinityFlipped = !infinityFlipped;
    }

    private boolean infinityFlipped;

    // ---------------------------------------------------------------------------------------------------------
    // Cooldowns, for the choice of moves

    /** Whether the move's cooldown is over. */
    @VisibleForTesting
    public boolean ready(ThanosMove which) {
        return readyAt.getOrDefault(which, 0) <= tickCount;
    }

    /** Puts a move on cooldown for {@code ticks} from now (the Infinity Beam waits for the fifth stone). */
    void delay(ThanosMove which, int ticks) {
        readyAt.put(which, tickCount + ticks);
    }

    /** Whether the move was one of his last two. */
    boolean usedRecently(ThanosMove which) {
        return recentMoves.contains(which);
    }

    /** The ticks since a new stone arrived while its show of power lasts (x2.5 weight for its moves), or 0. */
    boolean showingOff() {
        return stoneBoost > 0;
    }

    long ticksSinceLastStone() {
        return combatTicks - lastStoneTick;
    }

    int ticksSinceRangedHit() {
        return tickCount - lastRangedTick;
    }

    /** The ranged hits of the last {@code window} ticks. */
    int rangedHitsIn(int window) {
        int count = 0;
        for (int tick : rangedHits) {
            if (tickCount - tick <= window) {
                count++;
            }
        }
        return count;
    }

    /** The damage he took in the last {@code window} ticks. */
    float damageIn(int window) {
        float total = 0.0F;
        for (Damage entry : damageWindow) {
            if (tickCount - entry.tick() <= window) {
                total += entry.amount();
            }
        }
        return total;
    }

    boolean hasBarrier() {
        return flag(FLAG_BARRIER);
    }

    boolean hasVeil() {
        return flag(FLAG_VEIL);
    }

    void raiseBarrier(int ticks) {
        barrierLeft = ticks;
        setFlag(FLAG_BARRIER, true);
    }

    void raiseVeil(int ticks) {
        veilLeft = ticks;
        setFlag(FLAG_VEIL, true);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The damage he takes

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !isAlive()) {
            return false;
        }
        boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        ThanosState state = state();
        if (!bypass && (state == ThanosState.ARRIVING || state == ThanosState.DYING || leaving)) {
            return false;
        }
        Entity attacker = source.getEntity();
        boolean stoneBorn = source.is(ModTags.STONE_BORN);
        boolean stormbreaker = source.is(ModDamageTypes.STORMBREAKER) || heldStormbreaker(source);
        // (A Power Blast of the hero is a beam like the lightning: the Barrier turns it away.)
        boolean ranged = source.is(DamageTypeTags.IS_PROJECTILE) || source.is(ModDamageTypes.LIGHTNING_BEAM)
                || source.is(ModDamageTypes.STONE_POWER) || source.getDirectEntity() instanceof Projectile;
        boolean fromMob = attacker instanceof LivingEntity living && !(living instanceof Player) && !(living instanceof ThanosEntity)
                && !(living instanceof HexVisionEntity);
        ThanosDefence.Hit hit = new ThanosDefence.Hit(bypass, fromMob, ranged, stoneBorn, stormbreaker);
        ThanosDefence.Stance stance = new ThanosDefence.Stance(state, hasBarrier(), plates(), gauntletLocked(), (float) virtualMax, damageIn(20));
        // (In points of the design; the bar loses its share of them.)
        float takenPoints = ThanosDefence.damageTaken(hit, stance, amount);
        float taken = bypass ? takenPoints : toBar(takenPoints);

        if (ranged) {
            lastRangedTick = tickCount;
            rangedHits.addLast(tickCount);
            while (!rangedHits.isEmpty() && tickCount - rangedHits.peekFirst() > 100) {
                rangedHits.removeFirst();
            }
        }
        noteWho(source, attacker, hit);
        if (fromMob && !bypass && attacker instanceof LivingEntity mob && !mob.getType().is(ModTags.BOSSES)
                && !BossFights.sameFight(this, mob)) {
            // A golem or a wolf that hits him is swatted away with the back of his hand.
            StonePowers.hurt((ServerLevel) level(), this, mob, ModDamageTypes.THANOS_BLOW, 1000.0F, this);
        }
        if (takenPoints <= 0.0F && !bypass) {
            return false;
        }
        if (mjolnirBlow(source)) {
            // Captain America broke his helmet with Mjolnir.
            breakPlates(ThanosDefence.HELMET);
        }
        float before = getHealth();
        float lethalLimit = before - 1.0F;
        boolean lethal = !bypass && taken >= before;
        if (lethal) {
            taken = Math.max(0.0F, lethalLimit);
        }
        boolean hurt = taken > 0.0F ? super.hurt(source, taken) : lethal;
        float applied = Math.max(0.0F, before - getHealth());
        if (bypass && hurt && !isAlive()) {
            return true;
        }
        if (applied > 0.0F) {
            afterHit(source, attacker, hit, toPoints(applied), before);
        }
        if (lethal && (hurt || before <= 1.0F)) {
            startDying(attacker instanceof ServerPlayer player ? player : null);
        }
        return hurt;
    }

    /** Whether the blow is a swing of Stormbreaker in the hand of a player. */
    private static boolean heldStormbreaker(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof LivingEntity holder
                && ThorAbilities.isStormbreaker(holder.getMainHandItem());
    }

    /** Whether the blow is Mjolnir's: thrown, a fragment of it, or a swing with it in the hand. */
    private static boolean mjolnirBlow(DamageSource source) {
        return source.is(ModDamageTypes.MJOLNIR) || (source.is(DamageTypes.PLAYER_ATTACK)
                && source.getDirectEntity() instanceof LivingEntity holder && ThorAbilities.isMjolnir(holder.getMainHandItem()));
    }

    /** What the hit tells him whatever it did: it shows him if he hides, tears plates, counts the beam on him. */
    private void noteWho(DamageSource source, @Nullable Entity attacker, ThanosDefence.Hit hit) {
        reveal();
        if (source.is(ModDamageTypes.CHAOS_BEAM) && attacker instanceof Player) {
            chaosBeamTicks = tickCount - lastChaosBeamTick <= 3 ? chaosBeamTicks + 1 : 1;
            lastChaosBeamTick = tickCount;
            if (chaosBeamTicks >= ThanosBossConfig.PLATE_TEAR_SECONDS.get() * 20) {
                chaosBeamTicks = 0;
                tearPlate();
            }
        } else if (source.is(ModDamageTypes.TELEKINETIC_SLAM) && attacker instanceof Player && tickCount - lastSlamTear >= 120) {
            lastSlamTear = tickCount;
            tearPlate();
        }
    }

    /** After a hit that landed: the ledgers, the plates at their thresholds, the poise, and what Stormbreaker does to a beam. */
    private void afterHit(DamageSource source, @Nullable Entity attacker, ThanosDefence.Hit hit, float applied, float before) {
        damageWindow.addLast(new Damage(tickCount, applied));
        if (attacker instanceof ServerPlayer player) {
            recentFrom.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>()).addLast(new Damage(tickCount, applied));
            Deque<Damage> own = recentFrom.get(player.getUUID());
            while (own.size() > 64) {
                own.removeFirst();
            }
        }
        if (move != null) {
            move.taken += applied;
        }
        breakPlates(ThanosDefence.crossed(before / getMaxHealth(), getHealth() / getMaxHealth()));
        if (applied > 4.0F && tickCount - lastSpeech > 20) {
            lastSpeech = 20;
            ThanosSounds.HURT.play(level(), position(), 0.8F, 1.0F);
        }
        float added = ThanosDefence.poise(applied, hit.stoneBorn());
        if (clenching()) {
            poise += added;
            if (poise >= ThanosBossConfig.STAGGER_POISE.get()) {
                stagger();
                return;
            }
        } else if (move != null && state() == ThanosState.ACTIVE) {
            move.poise += added;
        }
        if (source.is(ModDamageTypes.STORMBREAKER) && move != null && ThanosMoves.cutsBeam(move.move)) {
            // Stormbreaker cuts the Power Blast and the Infinity Beam, and staggers him.
            stagger();
        }
    }

    /** Breaks the plates in the mask (the ones still intact): gold chips fly. */
    void breakPlates(int mask) {
        int broken = mask & plates();
        if (broken == 0) {
            return;
        }
        entityData.set(DATA_PLATES, (byte) (plates() & ~broken));
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState()),
                    getX(), getY() + 2.0, getZ(), 24, 0.6, 0.8, 0.6, 0.1);
            ThanosSounds.HIT.play(level, position(), 1.0F, 1.5F);
        }
    }

    /** Wanda's beam tears a random plate away. */
    private void tearPlate() {
        breakPlates(ThanosDefence.firstIntact(plates(), random.nextInt(4)));
    }

    /** The Rewind brings the plates back (a snapshot's mask). */
    void restorePlates(int mask) {
        entityData.set(DATA_PLATES, (byte) (plates() | (mask & ThanosDefence.ALL_PLATES)));
    }

    // ---- the Rewind ----

    /** The snapshot as old as the Rewind's window, or the oldest he has; null if there is no history. */
    @Nullable
    Snapshot rewindSnapshot() {
        return history.peekFirst();
    }

    /** Forgets the past: nothing before this moment can be rewound to. */
    void forgetHistory() {
        history.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Dying

    /**
     * Defeated: he goes down on one knee, says his last words and turns to dust. Invulnerable throughout; the fight is
     * won and the fighters are rewarded at once.
     */
    void startDying(@Nullable ServerPlayer lastBlow) {
        if (state() == ThanosState.DYING) {
            return;
        }
        interruptMove();
        recallSword();
        clearMinions();
        setState(ThanosState.DYING);
        dyingTicks = 0;
        navigation.stop();
        setHealth(1.0F);
        setFlag(FLAG_BARRIER, false);
        setFlag(FLAG_VEIL, false);
        setFlag(FLAG_HIDDEN, false);
        setInvisible(false);
        if (lastBlow != null) {
            this.lastBlow = lastBlow.getUUID();
        }
        if (fight != null) {
            fight.bossFell(lastBlow);
        }
    }

    /** The fight is over and he goes through a ring of Space: he stops, and everything he made goes with him. */
    void interruptForLeaving() {
        leaving = true;
        interruptMove();
        clearMinions();
        navigation.stop();
    }

    /** His sword, meteors, orbs and phantoms are gone. */
    private void clearMinions() {
        if (fight != null) {
            for (Entity entity : fight.entities()) {
                if (entity != this) {
                    entity.discard();
                }
            }
        }
    }

    private void tickDying(ServerLevel level) {
        dyingTicks++;
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        if (dyingTicks == 1) {
            ThanosSounds.DEATH.play(level, position());
        }
        if (dyingTicks == 40 && fight != null) {
            fight.speak(Component.translatable("entity.enhanced.thanos"), StoneKind.POWER.color(),
                    Component.translatable("message.enhanced.thanos.last_words"), null);
        }
        if (dyingTicks >= 60) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                    getX(), getY() + 1.2, getZ(), 14, 0.6, 1.0, 0.6, 0.05);
            level.sendParticles(new com.kolodkingal.enhanced.particle.EnergyParticleOptions(StoneKind.POWER.color(), 1.3F),
                    getX(), getY() + 1.5, getZ(), 8, 0.6, 1.0, 0.6, 0.08);
        }
        if (dyingTicks >= DYING_TICKS) {
            int xp = ThanosBossConfig.XP_REWARD.get();
            if (xp > 0) {
                net.minecraft.world.entity.ExperienceOrb.award(level, position(), xp);
            }
            if (fight != null) {
                fight.bossGone();
            }
            discard();
        }
    }

    /** Whether the dying is over (the fight waits for it before it lets the arena go). */
    boolean dyingFinished() {
        return state() == ThanosState.DYING && dyingTicks >= DYING_TICKS;
    }

    /** The ticks since he began to die (0 if he has not). */
    public int dyingTicks() {
        return dyingTicks;
    }

    // ---------------------------------------------------------------------------------------------------------
    // BossHooks

    /** Quicksilver Time slows him to one half, and not at all once he has the Time Stone. */
    @Override
    public int slowFactor(int zoneFactor) {
        return hasTimeStone() ? 0 : Math.min(zoneFactor, 2);
    }

    /** He wears no metal that Magneto could take: the strip does nothing. */
    @Override
    public boolean onMagnetoStrip(ServerPlayer magneto) {
        return true;
    }

    /** A Spider-Man's web reaches the Gauntlet and locks it, at most once per cooldown. */
    @Override
    public void onWebShot(ServerPlayer spider) {
        int ticks = ThanosBossConfig.WEB_LOCK_TICKS.get();
        if (ticks > 0 && tickCount - lastWebLock >= ThanosBossConfig.WEB_LOCK_COOLDOWN.get()) {
            lastWebLock = tickCount;
            lockGauntlet(ticks, spider);
        }
    }

    private int lastWebLock = -100000;

    // ---------------------------------------------------------------------------------------------------------
    // What he is immune to

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.isInvulnerableTo(source);
        }
        return super.isInvulnerableTo(source) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.SWEET_BERRY_BUSH) || source.is(DamageTypes.FREEZE)
                || source.is(DamageTypes.STARVE) || source.is(DamageTypes.MAGIC) && source.getEntity() == null;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.is(MobEffects.WITHER) || effect.is(MobEffects.POISON) || effect.is(MobEffects.HUNGER) || effect.is(MobEffects.CONFUSION)
                || effect.is(MobEffects.LEVITATION) || effect.is(MobEffects.DIG_SLOWDOWN) || effect.is(MobEffects.WEAKNESS)
                || effect.is(ModEffects.PETRIFIED) || effect.is(ModEffects.SOULWORLD) || effect.is(ModEffects.TIME_LOCKED)) {
            return false;
        }
        // Slowness never takes his speed below half (one level is 15 percent, two 30).
        if (effect.is(MobEffects.MOVEMENT_SLOWDOWN) && effect.getAmplifier() > 1) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        // Cobwebs and powder snow do not slow him.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    /** A fight never survives a restart: neither he nor anything of his is written to the world. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Nothing else drops: the XP is dropped when the dust settles, the reward is the Gauntlet.
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (fight != null && state() != ThanosState.DYING) {
            fight.bossLost();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        // A fight never survives a restart: nothing of him is saved.
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
    }

    /** Whether Loki or another trick near him calls for the Soul Sight: duplicates, a hidden player or an invisible one. */
    boolean tricksNear(ServerLevel level, double radius) {
        if (!level.getEntitiesOfClass(LokiIllusionEntity.class, getBoundingBox().inflate(radius)).isEmpty()) {
            return true;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && distanceToSqr(player) <= radius * radius
                    && (player.isInvisible() || LokiAbilities.isHidden(player) || Powers.is(player, Power.LOKI) && !Powers.get(player).form().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    /** Teleports him if the Hex and the arena allow it; false if a wall refuses (he walks instead). */
    boolean mayTeleportTo(ServerLevel level, Vec3 to) {
        return HexManager.onTeleport(level, this, to);
    }

    /** Whether a message is due to the arena (kept short): speaks as the Titan. */
    void say(Component line) {
        if (fight != null) {
            fight.speak(Component.translatable("entity.enhanced.thanos"), StoneKind.POWER.color(), line, null);
        }
    }

    /** The shared difficulty scale, for the moves that scale a number by hand. */
    static float difficulty() {
        return BossConfig.DIFFICULTY_SCALE.get().floatValue();
    }
}
