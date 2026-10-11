package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.boss.BossBars;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One fight against Ultron: the whole of his encounter, from the Mind Stone's call to the last Sentry. The framework
 * ({@link BossFight}) gives it the participants, the ledger, the bars, the end, the rewards and the rules; this class is
 * what is Ultron's own:
 *
 * <pre>
 *   AWAKENING (120)  scrap assembles (tick 0-50), Prime and six Sentries appear (50), he speaks (60, 100), invulnerable until 120
 *   GROUND           Prime and the Sentries fight; five attacks, waves from a finite budget
 *   LIFT_WARN (200)  at Prime 55 percent and idle: he floats to the middle, invulnerable; the Vibranium Key rises (100)
 *   LIFT_LOCK (20)   everybody is locked (the lift's work: {@link LiftHook})
 *   RISING / TREMOR  the island rises, or, if it cannot, the ground shakes (nothing moves)
 *   ISLAND           the fight goes on with bigger waves, falling debris and the bodies:
 *                    a body that dies uploads him into a Sentry, 20 percent weaker (TRANSFER, 80 ticks), until no Sentry
 *                    is left; the last body dies for good (60 ticks), the Core goes to every qualifying fighter, the
 *                    island lowers.
 * </pre>
 *
 * <p>His finish rule is Eilon's: he is destroyed for good only when his body dies and no Sentry is alive and none is
 * left in the budget (or after {@code maxUploads} uploads, the safety valve). Prime cannot die before the island is up.
 * The Sokovia lift itself is not here: the fight calls a {@link LiftHook} at the moments of its phases, and until a lift is
 * installed ({@link LiftHooks}) the lift is the Tremor.
 */
public final class UltronFight extends BossFight {
    /** The phases of the fight, in order. */
    public enum Phase {
        AWAKENING, GROUND, LIFT_WARN, LIFT_LOCK, RISING, TREMOR, ISLAND
    }

    public static final int APPEAR_AT = 50;
    public static final int SPEECH_1_AT = 60;
    public static final int SPEECH_2_AT = 100;
    public static final int AWAKENING_TICKS = 120;
    public static final int WARN_TICKS = 200;
    public static final int KEY_AT = 100;
    public static final int LOCK_TICKS = 20;
    public static final int TRANSFER_TICKS = 80;
    public static final int TRANSFER_MOVES_AT = 40;
    public static final int DEATH_TICKS = 60;
    /** How long the cracks and pillars of a wave show before the Sentries emerge. */
    public static final int WAVE_TELL = 30;
    /** After a victory the island waits this long (the reward is in the players' hands) before it lowers. */
    public static final int VICTORY_PAUSE = 100;
    /** After a defeat Ultron uploads himself away for this long, then the island lowers. */
    public static final int UPLOAD_AWAY_TICKS = 60;
    /** The ground fight's Sentries stand on a ring of this radius when he wakes. */
    public static final int START_RING = 6;
    /** A vessel for an upload must be within this far of the body when Vision cuts the net. */
    public static final double CUT_VESSEL_RANGE = 12.0;
    /** His speech is in this colour: the red of his eyes. */
    public static final int SPEECH_COLOR = 0xD8262B;

    private static final Component NAME = Component.translatable("entity.enhanced.ultron");

    private final UltronArena island;
    private final int radius;
    private Phase phase = Phase.AWAKENING;
    private int phaseTicks;
    @Nullable
    private UUID bossId;
    @Nullable
    private UUID keyId;
    private boolean appeared;
    /** The participants counted for his strength (at most 6). */
    private int counted = 1;
    private int uploads;
    private int budget;
    private int budgetSpent;
    private int startSentries;
    private long nextWaveAt;
    private long nextDebrisAt;
    private final List<Pending> wave = new ArrayList<>();
    private final List<Pending> debris = new ArrayList<>();
    @Nullable
    private Transfer transfer;
    private boolean dying;
    private int deathTicks;
    @Nullable
    private BossEnd ended;
    private boolean tremor;
    private boolean lifting;
    private boolean spokeGround;
    private boolean spokeFirstDeath;
    private boolean spokeLow;
    private boolean victoryTold;

    // Per-tick caches.
    private long cachedAt = -1;
    private List<LivingEntity> targetCache = List.of();
    private List<UltronSentryEntity> sentryCache = List.of();

    /** A Sentry or a ring that waits for its moment: a point, and the game time it happens. */
    private record Pending(Vec3 at, long when) {
    }

    private static final class Transfer {
        final UUID vessel;
        int ticks;

        Transfer(UUID vessel) {
            this.vessel = vessel;
        }
    }

    public UltronFight(ServerLevel level, UltronArena arena) {
        super(level, arena, new BossBars(Component.translatable("boss.enhanced.ultron"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6));
        this.island = arena;
        this.radius = arena.islandRadius();
        bars().second().setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
        // The bars appear with the body, at tick 50.
        bars().health().setVisible(false);
    }

    @Override
    public BossKind kind() {
        return BossKind.ULTRON;
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the world can ask

    /** The arena, as Ultron's. */
    public UltronArena island() {
        return island;
    }

    /** The radius of the island (and of the ground fight before it). */
    public int islandRadius() {
        return radius;
    }

    public Phase phase() {
        return phase;
    }

    public int phaseTicks() {
        return phaseTicks;
    }

    /** The body he is in, or null before he appears and after he is gone. */
    @Nullable
    public UltronEntity boss() {
        return bossId != null && level().getEntity(bossId) instanceof UltronEntity body && body.isAlive() ? body : null;
    }

    /** The Vibranium Key, or null before it rises. */
    @Nullable
    public VibraniumKeyEntity key() {
        return keyId != null && level().getEntity(keyId) instanceof VibraniumKeyEntity key ? key : null;
    }

    /** How many uploads he has done (the body he is in). */
    public int uploads() {
        return uploads;
    }

    /** The participants counted for his strength and the Sentry budget. */
    public int counted() {
        return counted;
    }

    /** Whether the island really rises in this fight (false: the Tremor). Decided when the warning begins. */
    public boolean isLifting() {
        return lifting;
    }

    /** Whether the lift became the Tremor in this fight. */
    public boolean isTremor() {
        return tremor;
    }

    /** Whether he is changing bodies right now. */
    public boolean isTransferring() {
        return transfer != null;
    }

    /** Whether the last body is in its long death. */
    public boolean isDying() {
        return dying;
    }

    /** Whether the body can be brought down yet: only when the island is up (or the Tremor has done its work). */
    public boolean canFall() {
        return phase == Phase.ISLAND;
    }

    /** The least health the body may have: Prime keeps a tenth until the island is up, so the cinematic always happens. */
    public float healthFloor(UltronEntity body) {
        return canFall() ? 0.0F : body.getMaxHealth() * 0.10F;
    }

    // ---- the Sentries and the budget ----

    /** The Sentries of the fight that are alive and belong to nobody: the robots Eilon's finish rule counts. */
    public List<UltronSentryEntity> sentries() {
        refreshCache();
        return sentryCache;
    }

    /** The creatures his army hunts: the fighters in the arena (never Creative or Spectator), and their decoys. */
    public List<LivingEntity> targets() {
        refreshCache();
        return targetCache;
    }

    private void refreshCache() {
        long now = level().getGameTime();
        if (cachedAt == now) {
            return;
        }
        cachedAt = now;
        List<LivingEntity> targets = new ArrayList<>();
        for (ServerPlayer player : present()) {
            if (!player.isCreative() && !player.isSpectator()) {
                targets.add(player);
            }
        }
        Vec3 c = island.center();
        double reach = island.joinRadius();
        AABB box = new AABB(c.x - reach, level().getMinBuildHeight(), c.z - reach, c.x + reach, level().getMaxBuildHeight(), c.z + reach);
        for (LokiIllusionEntity illusion : level().getEntitiesOfClass(LokiIllusionEntity.class, box, i -> i.isAlive() && isParticipant(i.ownerId()))) {
            targets.add(illusion);
        }
        targetCache = targets;
        sentryCache = level().getEntitiesOfClass(UltronSentryEntity.class, box, s -> s.isAlive() && s.getOwnerUUID() == null && owns(s));
    }

    /** Whether the creature is one his army and he may hunt now. */
    public boolean isTargetable(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity instanceof ServerPlayer player) {
            return !player.isCreative() && !player.isSpectator() && island.inJoinRange(player.position());
        }
        return entity instanceof LokiIllusionEntity illusion && isParticipant(illusion.ownerId());
    }

    /** How many Sentries may be alive at once: {@code sentryMaxAlive} and {@code sentryMaxAlivePerPlayer} for each fighter after the first. */
    public int maxAlive() {
        return UltronBossConfig.SENTRY_MAX_ALIVE.get() + UltronBossConfig.SENTRY_MAX_ALIVE_PER_PLAYER.get() * (counted - 1);
    }

    /** The whole budget: {@code sentryBudgetBase} and {@code sentryBudgetPerPlayer} for each fighter after the first. */
    public int budget() {
        return budget;
    }

    /** How many Sentries are still to come (not counting a wave that is on its way). */
    public int budgetLeft() {
        return Math.max(0, budget - budgetSpent);
    }

    /** The Sentries of a wave that is announced and has not emerged yet. */
    public int pendingSentries() {
        return wave.size();
    }

    /** What the second bar counts: the Sentries alive plus those still to come. Eilon's finish rule is about it reaching zero. */
    public int sentriesLeft() {
        return sentries().size() + budgetLeft() + pendingSentries();
    }

    /** Whether Ultron may call a wave now: fewer Sentries live than may and some of the budget is left. */
    public boolean mayCall() {
        return wave.isEmpty() && sentries().size() < maxAlive() && budgetLeft() > 0;
    }

    /** Whether a wave is due, or fewer than half the most Sentries live: he calls then. */
    public boolean callUrgent() {
        return level().getGameTime() >= nextWaveAt || sentries().size() * 2 < maxAlive();
    }

    // ---------------------------------------------------------------------------------------------------------
    // The life of the fight

    @Override
    protected void onBegin(ServerPlayer starter) {
        counted = UltronBodies.counted(participants().size());
        budget = UltronBossConfig.SENTRY_BUDGET_BASE.get() + UltronBossConfig.SENTRY_BUDGET_PER_PLAYER.get() * (counted - 1);
    }

    @Override
    protected void onTick() {
        phaseTicks++;
        long now = level().getGameTime();
        UltronEntity boss = boss();
        if (appeared && !isOver() && !dying && transfer == null && boss == null) {
            // The body is gone without the fight's say (/kill, the void): the fight is over, with no reward.
            end(BossEnd.ABORT);
            return;
        }
        switch (phase) {
            case AWAKENING -> tickAwakening(boss);
            case GROUND -> tickGround(boss, now);
            case LIFT_WARN -> tickLiftWarn(boss);
            case LIFT_LOCK -> tickLiftLock(boss);
            case RISING, TREMOR -> tickRising(boss);
            case ISLAND -> {
                // (Everything of this phase runs in the waves, the debris and the bodies' own states.)
            }
        }
        if (isOver()) {
            return;
        }
        if (transfer != null) {
            tickTransfer(boss);
        }
        if (dying) {
            tickDeath(boss);
        }
        if (isOver()) {
            return;
        }
        tickWave(now);
        tickDebris(now);
        if (boss != null && !spokeLow && canFall() && boss.getHealth() <= boss.getMaxHealth() * 0.25F && boss.bodyIndex() == uploads) {
            spokeLow = true;
            say("low");
        }
        updateBars(boss);
    }

    // ---- speech and messages ----

    /** Ultron speaks a line ({@code speech.enhanced.ultron.<key>}), in the red of his eyes, with his voice. */
    public void say(String key) {
        speak(NAME, SPEECH_COLOR, Component.translatable("speech.enhanced.ultron." + key), UltronSounds.SPEAK.event());
    }

    /** A participant fell: the first one earns a line. */
    public void participantFell() {
        if (!spokeFirstDeath && appeared && !isOver()) {
            spokeFirstDeath = true;
            say("first_death");
        }
    }

    // ---- placing things ----

    /** The ground height at a column of the arena, as the ritual's ground has it. */
    public double groundAt(double x, double z) {
        return UltronMotion.surface(level(), x, island.center().y + 4.0, z);
    }

    private Vec3 onRing(double ring, double angle) {
        Vec3 c = island.center();
        double x = c.x + Math.cos(angle) * ring;
        double z = c.z + Math.sin(angle) * ring;
        return new Vec3(x, groundAt(x, z), z);
    }

    // ---- AWAKENING ----

    private void tickAwakening(@Nullable UltronEntity boss) {
        if (phaseTicks == 1) {
            UltronFx.at(level(), UltronFxPayload.ASSEMBLE, island.center(), 3.4F, APPEAR_AT);
            UltronSounds.AWAKEN.play(level(), island.center());
        }
        if (phaseTicks == APPEAR_AT) {
            appear();
        }
        if (phaseTicks == SPEECH_1_AT) {
            say("awaken1");
            if (boss != null) {
                boss.setState(UltronState.IDLE); // the eyes light
            }
        }
        if (phaseTicks == SPEECH_2_AT) {
            say("awaken2");
            for (UltronSentryEntity sentry : sentries()) {
                sentry.setEyes(UltronSentryEntity.Eyes.RED);
            }
        }
        if (phaseTicks >= AWAKENING_TICKS) {
            enter(Phase.GROUND);
        }
    }

    /** Prime and the six Sentries appear, among a flash, and the bars with them. */
    private void appear() {
        if (appeared) {
            return;
        }
        appeared = true;
        // (Those who stood near when the ritual was done have joined by now: the fight is as big as they make it.)
        counted = UltronBodies.counted(participants().size());
        budget = Math.max(budget, UltronBossConfig.SENTRY_BUDGET_BASE.get() + UltronBossConfig.SENTRY_BUDGET_PER_PLAYER.get() * (counted - 1));
        Vec3 c = island.center();
        UltronEntity boss = new UltronEntity(ModEntities.ULTRON.get(), level());
        boss.setPos(c.x, groundAt(c.x, c.z) + 3.0, c.z);
        boss.applyBody(0, counted);
        boss.setShielded(true);
        boss.setState(UltronState.AWAKENING);
        boss.brain().setEnabled(false);
        spawn(boss);
        bossId = boss.getUUID();
        int count = UltronBossConfig.SENTRIES_AT_START.get();
        for (int i = 0; i < count; i++) {
            Vec3 at = onRing(START_RING, Mth.TWO_PI * i / Math.max(1, count));
            UltronSentryEntity sentry = UltronSentryEntity.army(level(), at.add(0, 0.3, 0), true);
            sentry.setHeld(true);
            spawn(sentry);
        }
        startSentries = count;
        UltronFx.at(level(), UltronFxPayload.FLASH, c.add(0, 2.0, 0), 8.0F, 10);
        bars().health().setVisible(true);
        bars().showSecond(true);
        refreshCacheNow();
    }

    private void refreshCacheNow() {
        cachedAt = -1;
        refreshCache();
    }

    /**
     * Forgets the lists of the fighters and the Sentries that are kept for the tick: something else added or removed a
     * Sentry of the fight in the middle of it (the tests, an operator's command).
     */
    public void refresh() {
        cachedAt = -1;
    }

    // ---- GROUND ----

    private void tickGround(@Nullable UltronEntity boss, long now) {
        if (!spokeGround && phaseTicks >= 800) {
            spokeGround = true;
            say("ground");
        }
        if (boss != null && boss.bodyIndex() == 0 && boss.brain().isIdle() && boss.brain().isEnabled()
                && boss.getHealth() <= boss.getMaxHealth() * UltronBossConfig.LIFT_AT.get().floatValue()) {
            enter(Phase.LIFT_WARN);
        }
    }

    // ---- LIFT_WARN: he floats to the middle and the key rises ----

    private void tickLiftWarn(@Nullable UltronEntity boss) {
        Vec3 c = island.center();
        if (boss != null) {
            boss.brain().flyTo(boss, new Vec3(c.x, groundAt(c.x, c.z) + 3.5, c.z), 0.25);
            boss.getLookControl().setLookAt(c.x, boss.getEyeY(), c.z);
        }
        if (phaseTicks == 1 || phaseTicks == 101) {
            tell(Component.translatable("message.enhanced.ultron.lift.warn", (WARN_TICKS - phaseTicks + 1) / 20).withStyle(ChatFormatting.YELLOW), true);
            UltronSounds.RUMBLE.play(level(), c, 1.2F, 1.0F);
        }
        if (phaseTicks == KEY_AT) {
            raiseKey();
        }
        if (phaseTicks >= WARN_TICKS) {
            enter(Phase.LIFT_LOCK);
        }
    }

    private void raiseKey() {
        if (key() != null) {
            return;
        }
        Vec3 c = island.center();
        VibraniumKeyEntity key = new VibraniumKeyEntity(ModEntities.VIBRANIUM_KEY.get(), level());
        key.setPos(c.x, groundAt(c.x, c.z), c.z);
        key.emerge();
        spawn(key);
        keyId = key.getUUID();
        UltronSounds.RUMBLE.play(level(), c, 1.5F, 0.8F);
    }

    // ---- LIFT_LOCK, RISING, TREMOR ----

    private void tickLiftLock(@Nullable UltronEntity boss) {
        Vec3 c = island.center();
        if (boss != null) {
            boss.brain().flyTo(boss, new Vec3(c.x, groundAt(c.x, c.z) + 3.5, c.z), 0.25);
        }
        if (phaseTicks >= LOCK_TICKS) {
            enter(lifting ? Phase.RISING : Phase.TREMOR);
        }
    }

    private void tickRising(@Nullable UltronEntity boss) {
        Vec3 c = island.center();
        if (boss != null) {
            boss.brain().flyTo(boss, new Vec3(c.x, groundAt(c.x, c.z) + island.islandOffset() + 3.5, c.z), 0.25);
        }
        if (phaseTicks % 40 == 1) {
            UltronSounds.RUMBLE.play(level(), c, 1.5F, 1.0F);
        }
        boolean done = phase == Phase.RISING ? LiftHooks.get().rise(this, phaseTicks) : phaseTicks >= UltronBossConfig.LIFT_TICKS.get();
        if (done) {
            enter(Phase.ISLAND);
        }
    }

    // ---- phases ----

    /**
     * Moves the fight to a phase, doing what entering it does. Also how the tests and an operator reach a later phase
     * quickly: entering AWAKENING again does nothing, any later phase makes the body appear first if it has not.
     */
    public void forceState(Phase to) {
        if (to != Phase.AWAKENING && !appeared) {
            appear();
        }
        enter(to);
    }

    private void enter(Phase to) {
        phase = to;
        phaseTicks = 0;
        UltronEntity boss = boss();
        switch (to) {
            case AWAKENING -> {
            }
            case GROUND -> {
                if (boss != null) {
                    boss.setShielded(false);
                    boss.setState(UltronState.IDLE);
                    boss.brain().setEnabled(true);
                }
                releaseSentries();
                nextWaveAt = level().getGameTime() + waveInterval();
            }
            case LIFT_WARN -> beginLift(boss);
            case LIFT_LOCK -> {
                if (lifting) {
                    LiftHooks.get().lock(this);
                    setHexExempt(true);
                }
                UltronSounds.LIFT_START.play(level(), island.center());
                UltronFx.at(level(), UltronFxPayload.TREMOR, island.center(), (float) island.radius(), LOCK_TICKS + UltronBossConfig.LIFT_TICKS.get());
            }
            case RISING -> tell(Component.translatable("message.enhanced.ultron.lift.up").withStyle(ChatFormatting.YELLOW), true);
            case TREMOR -> {
                if (!tremor) {
                    tremor = true;
                    tell(Component.translatable("message.enhanced.ultron.tremor").withStyle(ChatFormatting.YELLOW), false);
                }
            }
            case ISLAND -> {
                if (boss != null) {
                    boss.setShielded(false);
                    boss.setState(UltronState.IDLE);
                    boss.brain().setEnabled(true);
                    boss.brain().resetCooldowns();
                }
                releaseSentries();
                nextWaveAt = level().getGameTime() + waveInterval();
                nextDebrisAt = level().getGameTime() + UltronBossConfig.DEBRIS_INTERVAL.get();
                say("island");
            }
        }
    }

    private void releaseSentries() {
        for (UltronSentryEntity sentry : sentries()) {
            sentry.setHeld(false);
        }
    }

    /** The warning begins: he stops where he is and floats to the middle; the lift decides whether the island will rise. */
    private void beginLift(@Nullable UltronEntity boss) {
        counted = Math.max(counted, UltronBodies.counted(participants().size()));
        int newBudget = UltronBossConfig.SENTRY_BUDGET_BASE.get() + UltronBossConfig.SENTRY_BUDGET_PER_PLAYER.get() * (counted - 1);
        budget = Math.max(budget, newBudget);
        if (boss != null) {
            boss.rescaleHealth(counted);
            boss.brain().abort(boss);
            boss.brain().setEnabled(false);
            boss.setShielded(true);
            boss.setState(UltronState.LIFT_CHANNEL);
        }
        boolean wanted = UltronBossConfig.LIFT_ENABLED.get() && UltronBossConfig.LIFT_MODE.get() != UltronBossConfig.LiftMode.TREMOR;
        lifting = wanted && LiftHooks.get().warn(this);
        if (!lifting) {
            tremor = true;
            tell(Component.translatable("message.enhanced.ultron.tremor").withStyle(ChatFormatting.YELLOW), false);
        }
        say("lift_warn");
        UltronFx.at(level(), UltronFxPayload.LIFT_RING, island.center(), radius, WARN_TICKS);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Waves of Sentries

    private int waveSize() {
        double size = UltronBossConfig.SENTRY_WAVE_SIZE.get() * (phase == Phase.ISLAND ? 1.5 : 1.0);
        return Math.max(1, (int) Math.round(size));
    }

    private long waveInterval() {
        double interval = UltronBossConfig.SENTRY_WAVE_INTERVAL.get() * (phase == Phase.ISLAND ? 0.8 : 1.0);
        return Math.max(20L, Math.round(interval));
    }

    /**
     * Announces a wave: cracks and a pillar of pale light at every spawn point for {@link #WAVE_TELL} ticks, then the
     * Sentries emerge. The wave is as big as the fight allows (the size, the room under the cap, what is left of the
     * budget). Called when Ultron uses Call and when the wave timer runs out.
     *
     * @return whether a wave was announced
     */
    public boolean callWave(boolean fromCall) {
        if (!mayCall() || isOver()) {
            return false;
        }
        int count = Math.min(waveSize(), Math.min(maxAlive() - sentries().size(), budgetLeft()));
        if (count <= 0) {
            return false;
        }
        long when = level().getGameTime() + WAVE_TELL;
        for (Vec3 point : spawnPoints(count)) {
            wave.add(new Pending(point, when));
            UltronFx.at(level(), UltronFxPayload.SPAWN_PILLAR, point, 1.0F, WAVE_TELL);
        }
        nextWaveAt = level().getGameTime() + waveInterval();
        return !wave.isEmpty();
    }

    /** Spawn points on a ring of 6 to {@code R - 3} blocks from the middle, on the ground, five blocks or more from every player. */
    private List<Vec3> spawnPoints(int count) {
        List<Vec3> points = new ArrayList<>();
        double inner = START_RING;
        double outer = Math.max(inner, radius - 3.0);
        var random = level().getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 best = null;
            for (int attempt = 0; attempt < 24; attempt++) {
                double ring = inner + random.nextDouble() * (outer - inner);
                Vec3 candidate = onRing(ring, random.nextDouble() * Mth.TWO_PI);
                if (clearOfPlayers(candidate)) {
                    best = candidate;
                    break;
                }
                if (best == null) {
                    best = candidate;
                }
            }
            points.add(best);
        }
        return points;
    }

    private boolean clearOfPlayers(Vec3 point) {
        for (ServerPlayer player : fighters()) {
            if (player.position().distanceToSqr(point) < 25.0) {
                return false;
            }
        }
        return true;
    }

    private void tickWave(long now) {
        if (!wave.isEmpty()) {
            List<Pending> due = wave.stream().filter(p -> now >= p.when()).toList();
            wave.removeAll(due);
            for (Pending p : due) {
                spawn(UltronSentryEntity.army(level(), p.at().add(0, 0.3, 0), true));
                budgetSpent++;
            }
            if (!due.isEmpty()) {
                refreshCacheNow();
            }
        }
        boolean calm = phase == Phase.GROUND || phase == Phase.ISLAND;
        if (calm && !dying && transfer == null && now >= nextWaveAt && boss() != null) {
            if (!callWave(false)) {
                nextWaveAt = now + 40; // (the cap or the budget: look again in two seconds)
            }
        }
    }

    // ---- debris (the island, and the Tremor) ----

    private void tickDebris(long now) {
        if (!debris.isEmpty()) {
            List<Pending> landed = debris.stream().filter(p -> now >= p.when()).toList();
            debris.removeAll(landed);
            for (Pending p : landed) {
                land(p.at());
            }
        }
        if (phase != Phase.ISLAND || dying || transfer != null || now < nextDebrisAt) {
            return;
        }
        nextDebrisAt = now + UltronBossConfig.DEBRIS_INTERVAL.get();
        List<ServerPlayer> candidates = new ArrayList<>(present());
        candidates.removeIf(p -> p.isCreative() || p.isSpectator());
        int rings = (candidates.size() + 1) / 2;
        for (int i = 0; i < rings && !candidates.isEmpty(); i++) {
            ServerPlayer victim = candidates.remove(level().getRandom().nextInt(candidates.size()));
            Vec3 at = victim.position();
            debris.add(new Pending(at, now + WAVE_TELL));
            UltronFx.at(level(), UltronFxPayload.DEBRIS_RING, at, 3.0F, WAVE_TELL);
        }
    }

    /** A piece of Sokovia lands: dust, stone and a thud, 10 damage to everything in the ring. Nothing in the world changes. */
    private void land(Vec3 at) {
        UltronEntity boss = boss();
        UltronSounds.GRAVITY.play(level(), at, 1.0F, 0.9F);
        UltronFx.at(level(), UltronFxPayload.SHOCKWAVE, at, 3.0F, 8);
        if (boss == null) {
            return;
        }
        AABB box = new AABB(at.x - 3.0, at.y - 2.0, at.z - 3.0, at.x + 3.0, at.y + 4.0, at.z + 3.0);
        float damage = UltronAttacks.scaled(UltronBossConfig.DEBRIS_DAMAGE.get());
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, e -> UltronAttacks.mayHit(boss, e))) {
            double dx = victim.getX() - at.x;
            double dz = victim.getZ() - at.z;
            if (dx * dx + dz * dz <= 9.0) {
                victim.hurt(level().damageSources().fallingBlock(boss), damage);
                victim.knockback(0.6, -dx, -dz);
                victim.hurtMarked = true;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Bodies

    /**
     * A lethal blow reached the body (or Wanda tore his core out). What happens is Eilon's rule: before the island is up
     * nothing, he cannot fall; with Sentries alive or in the budget, he uploads into the nearest one (a Vision within 48
     * blocks limits the vessels to those within 12 of the body) and the new body is weaker; with none, or after {@code
     * maxUploads}, he dies for good.
     *
     * @param ripper the Scarlet Witch who tore out his core, or null
     * @return true if the fight kept him alive (he is changing bodies or in his death), so the blow must not kill him
     */
    public boolean onBodyDestroyed(UltronEntity body, @Nullable DamageSource source, @Nullable ServerPlayer ripper) {
        if (isOver() || isFinished() || !appeared || body != boss()) {
            return false;
        }
        if (dying || transfer != null || !canFall()) {
            return true;
        }
        ServerPlayer striker = ripper != null ? ripper : source != null && source.getEntity() instanceof ServerPlayer player ? player : null;
        List<UltronSentryEntity> vessels = vessels(body);
        if (vessels.isEmpty() && budgetLeft() > 0) {
            vessels = spawnTheRest(body);
        }
        if (vessels.isEmpty() || uploads >= UltronBossConfig.MAX_UPLOADS.get()) {
            beginDeath(body, striker);
            return true;
        }
        UltronSentryEntity vessel = vessels.stream().min(Comparator.comparingDouble(s -> s.distanceToSqr(body))).orElseThrow();
        beginTransfer(body, vessel);
        return true;
    }

    /** The Sentries Ultron can upload into: alive, assembled, nobody's; and, with a Vision near, within 12 blocks of him. */
    private List<UltronSentryEntity> vessels(UltronEntity body) {
        boolean cut = visionNear(body.position());
        return sentries().stream()
                .filter(s -> !s.isAssembling() && s.isAlive())
                .filter(s -> !cut || s.distanceTo(body) <= CUT_VESSEL_RANGE)
                .toList();
    }

    /** With nobody to jump into and Sentries still in the budget, the rest of it (at most 6) comes at once, round him. */
    private List<UltronSentryEntity> spawnTheRest(UltronEntity body) {
        int count = Math.min(6, budgetLeft());
        List<UltronSentryEntity> fresh = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count;
            Vec3 at = body.position().add(Math.cos(angle) * 3.0, 0.0, Math.sin(angle) * 3.0);
            UltronSentryEntity sentry = UltronSentryEntity.army(level(), at, false);
            spawn(sentry);
            budgetSpent++;
            fresh.add(sentry);
        }
        refreshCacheNow();
        return fresh;
    }

    /** Whether a Vision (or White Vision) stands within {@code visionCutRange} of the point: the film's hack. */
    public boolean visionNear(Vec3 at) {
        return visionNear(level(), at);
    }

    /**
     * Whether a Vision (or White Vision) of this level stands within {@code visionCutRange} of the point. The boss's
     * uploads and the hero's (Upload, Hack and Second Life) are cut by the same rule.
     */
    public static boolean visionNear(ServerLevel level, Vec3 at) {
        double range = UltronConfig.VISION_CUT_RANGE.get();
        if (range <= 0.0) {
            return false;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && Powers.is(player, Power.VISION) && player.position().distanceToSqr(at) <= range * range) {
                return true;
            }
        }
        return false;
    }

    private void beginTransfer(UltronEntity body, UltronSentryEntity vessel) {
        uploads++;
        transfer = new Transfer(vessel.getUUID());
        body.brain().abort(body);
        body.brain().setEnabled(false);
        body.setShielded(true);
        body.setState(UltronState.TRANSFER);
        for (UltronSentryEntity sentry : sentries()) {
            sentry.freeze(TRANSFER_MOVES_AT);
        }
        Vec3 from = body.position().add(0, body.getBbHeight() * 0.6, 0);
        UltronFx.line(level(), UltronFxPayload.DATA_LINE, from, vessel.position().add(0, vessel.getBbHeight() * 0.6, 0), TRANSFER_MOVES_AT);
        UltronSounds.TRANSFER.play(level(), body.position());
    }

    private void tickTransfer(@Nullable UltronEntity body) {
        Transfer t = transfer;
        if (t == null || body == null) {
            return;
        }
        t.ticks++;
        if (t.ticks == TRANSFER_MOVES_AT) {
            Entity vessel = level().getEntity(t.vessel);
            Vec3 from = body.position();
            Vec3 to = vessel != null && vessel.isAlive() ? vessel.position() : from;
            if (vessel instanceof UltronSentryEntity sentry && sentry.isAlive()) {
                sentry.dissolve(); // it became him
            }
            UltronFx.at(level(), UltronFxPayload.WRECK, from, 0.0F, 200);
            UltronFx.at(level(), UltronFxPayload.FLASH, to.add(0, 1.0, 0), 3.0F, 10);
            UltronSounds.UPLOAD.play(level(), to);
            body.moveTo(to.x, to.y, to.z);
            body.setDeltaMovement(Vec3.ZERO);
            body.applyBody(uploads, counted);
            refreshCacheNow();
        }
        if (t.ticks >= TRANSFER_TICKS) {
            transfer = null;
            body.setShielded(false);
            body.setState(UltronState.IDLE);
            body.brain().resetCooldowns();
            body.brain().setEnabled(true);
            say("transfer" + Math.min(3, uploads));
        }
    }

    private void beginDeath(UltronEntity body, @Nullable ServerPlayer striker) {
        dying = true;
        deathTicks = 0;
        body.brain().abort(body);
        body.brain().setEnabled(false);
        body.setShielded(true);
        body.setState(UltronState.DEATH);
        if (striker != null) {
            lastBlow(striker);
        }
        // The robots that are left fall and explode: there is no one left to command them.
        for (UltronSentryEntity sentry : sentries()) {
            sentry.dissolve();
        }
        wave.clear();
        refreshCacheNow();
        UltronFx.send(level(), UltronFxPayload.DEATH, body.getId(), body.position(), Vec3.ZERO, 0.0F, DEATH_TICKS);
        UltronSounds.DEATH.play(level(), body.position());
        if (visionNear(body.position())) {
            say("final_vision");
        }
        say("final");
    }

    private void tickDeath(@Nullable UltronEntity body) {
        deathTicks++;
        if (body != null && deathTicks < DEATH_TICKS) {
            // He sags toward the ground as the plates fall open.
            double ground = UltronMotion.groundBelow(level(), body.getX(), body.getY() + 0.1, body.getZ());
            UltronMotion.steer(body, new Vec3(0, Math.max(-0.08, (ground + 1.0 - body.getY()) * 0.05), 0), 0.02);
        }
        if (deathTicks >= DEATH_TICKS) {
            dying = false;
            if (body != null) {
                UltronFx.at(level(), UltronFxPayload.FLASH, body.position().add(0, 1.5, 0), 6.0F, 20);
                body.discard();
            }
            end(BossEnd.VICTORY);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Heart rip (Wanda)

    /**
     * Wanda pressed her telekinesis key holding nothing and looks at a body: below {@code heartRipThreshold} of its
     * health, once the island is up, she tears out its core and the body is destroyed at once, through the upload; above
     * it, he is too powerful to lift.
     */
    @Override
    protected boolean onTelekinesisPress(ServerPlayer wanda) {
        EntityHitResult looked = Raycasts.entity(wanda, EnhancedConfig.TELEKINESIS_RANGE.get(), 0.3,
                e -> e instanceof UltronEntity && owns(e), 1.0F);
        if (looked == null || !(looked.getEntity() instanceof UltronEntity body)) {
            return false;
        }
        double threshold = UltronBossConfig.HEART_RIP_THRESHOLD.get();
        if (threshold > 0.0 && canFall() && !body.isShielded() && body.getHealth() <= body.getMaxHealth() * threshold) {
            UltronSounds.TRANSFER.play(level(), body.position(), 1.0F, 1.4F);
            UltronFx.at(level(), UltronFxPayload.PULSE, body.position(), 0.0F, 10);
            credit(wanda, DAMAGE, body.getHealth());
            onBodyDestroyed(body, null, wanda);
        } else {
            wanda.displayClientMessage(Component.translatable("message.enhanced.ultron.too_powerful"), true);
        }
        return true;
    }

    /** Wanda throws a Sentry she holds: its core is crushed instead, and it dies at once in red sparks. */
    @Override
    protected boolean onTelekinesisLaunch(ServerPlayer wanda, Entity held) {
        if (!(held instanceof UltronSentryEntity sentry) || sentry.getOwnerUUID() != null || !owns(sentry) || !sentry.isAlive()) {
            return false;
        }
        credit(wanda, DAMAGE, sentry.getHealth());
        level().sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 1.3F),
                sentry.getX(), sentry.getY() + 0.8, sentry.getZ(), 24, 0.3, 0.4, 0.3, 0.2);
        sentry.kill();
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The bars

    private void updateBars(@Nullable UltronEntity boss) {
        BossBars bars = bars();
        Component name;
        BossEvent.BossBarColor color = BossEvent.BossBarColor.RED;
        float progress;
        if (dying) {
            name = Component.translatable("boss.enhanced.ultron");
            progress = 1.0F - deathTicks / (float) DEATH_TICKS;
        } else if (transfer != null) {
            name = Component.translatable("boss.enhanced.ultron.upload");
            color = BossEvent.BossBarColor.WHITE;
            progress = transfer.ticks / (float) TRANSFER_TICKS;
        } else if (phase == Phase.LIFT_WARN || phase == Phase.LIFT_LOCK || phase == Phase.RISING || phase == Phase.TREMOR) {
            name = Component.translatable("boss.enhanced.ultron.lift");
            color = BossEvent.BossBarColor.YELLOW;
            progress = switch (phase) {
                case LIFT_WARN -> phaseTicks / (float) WARN_TICKS * 0.5F;
                case LIFT_LOCK -> 0.5F + phaseTicks / (float) LOCK_TICKS * 0.1F;
                default -> 0.6F + 0.4F * Math.min(1.0F, phaseTicks / (float) Math.max(1, UltronBossConfig.LIFT_TICKS.get()));
            };
        } else {
            int body = boss == null ? uploads : boss.bodyIndex();
            name = body == 0 ? Component.translatable("boss.enhanced.ultron") : Component.translatable("boss.enhanced.ultron.body", body + 1);
            progress = boss == null ? 0.0F : boss.getHealth() / boss.getMaxHealth();
        }
        bars.health().setName(name);
        bars.health().setColor(color);
        bars.health().setProgress(Mth.clamp(progress, 0.0F, 1.0F));
        bars.second().setName(Component.translatable("boss.enhanced.ultron.sentries", sentriesLeft()));
        int total = Math.max(1, startSentries + budget);
        bars.second().setProgress(Mth.clamp(sentriesLeft() / (float) total, 0.0F, 1.0F));
    }

    // ---------------------------------------------------------------------------------------------------------
    // The end

    @Override
    protected List<ItemStack> rewards(Participant participant) {
        return List.of(new ItemStack(ModItems.ULTRON_CORE.get()));
    }

    @Override
    protected void onEnd(BossEnd why) {
        ended = why;
        UltronEntity boss = boss();
        VibraniumKeyEntity key = key();
        if (key != null) {
            key.powerDown();
        }
        wave.clear();
        debris.clear();
        if (why == BossEnd.VICTORY) {
            if (!victoryTold) {
                victoryTold = true;
                tell(Component.translatable("message.enhanced.ultron.victory").withStyle(ChatFormatting.GOLD), false);
            }
            return;
        }
        // Defeat, an abort, an operator, Peaceful: Ultron uploads himself away, and his army with him.
        if (boss != null) {
            boss.brain().abort(boss);
            boss.brain().setEnabled(false);
            boss.setShielded(true);
            boss.setState(UltronState.TRANSFER);
        }
        for (UltronSentryEntity sentry : sentries()) {
            sentry.dissolve();
        }
        if (why == BossEnd.WIPE) {
            say("wipe");
        }
    }

    @Override
    protected boolean windDown(int ticks) {
        boolean victory = ended == BossEnd.VICTORY;
        int pause = victory ? VICTORY_PAUSE : UPLOAD_AWAY_TICKS;
        if (ticks < pause) {
            return false;
        }
        if (ticks == pause) {
            UltronEntity boss = boss();
            if (boss != null) {
                UltronFx.at(level(), UltronFxPayload.FLASH, boss.position().add(0, 1.5, 0), 5.0F, 20);
                boss.discard();
            }
            LiftHooks.get().beginDescent(this);
        }
        return LiftHooks.get().descend(this, ticks - pause);
    }

    @Override
    protected void onFinished(boolean forced) {
        setHexExempt(false);
        island.setIslandOffset(0.0);
        LiftHooks.get().finish(this, forced);
    }

    @Override
    protected String status() {
        UltronEntity boss = boss();
        return (isOver() ? "ending (" + ended + ")" : phase.name().toLowerCase(Locale.ROOT)) + ", body " + (boss == null ? "-" : boss.bodyIndex())
                + ", " + sentries().size() + " Sentries + " + budgetLeft() + " to come";
    }

    // ---------------------------------------------------------------------------------------------------------
    // For the tests and the operators

    /** The game time the next wave is due. */
    public long nextWaveAt() {
        return nextWaveAt;
    }

    /** Sets when the next wave is due (the tests). */
    public void setNextWaveAt(long when) {
        this.nextWaveAt = when;
    }

    /** Lets the debris fall now (the tests). */
    public void setNextDebrisAt(long when) {
        this.nextDebrisAt = when;
    }

    /** Uses up some of the budget without a Sentry (the tests). */
    public void spendBudget(int sentries) {
        budgetSpent += sentries;
    }

    /** How many Sentries were made from the budget so far. */
    public int budgetSpent() {
        return budgetSpent;
    }
}
