package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.boss.BossBars;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One fight against Apocalypse (spec 3.7), the state machine of the film:
 *
 * <pre>
 * AWAKENING (160) -&gt; P1 -&gt; [window 1 ends at the 2/3 floor] -&gt; T1 (120) -&gt; P2 -&gt; [window 2 ends at the 1/3 floor] -&gt; T2 (160) -&gt; P3
 *    -&gt; [third break: the armour falls] ARMOUR_FALL (60) -&gt; TRUE_FORM -&gt; [health &lt;= 15%, once] ESCAPE (100) -&gt; TRUE_FORM
 *    -&gt; [health &lt;= 5%] KNEEL (100) -&gt; DEATH (160) -&gt; VICTORY
 * Failure from any state before DEATH (a wipe, an abandonment, no one to break the shield, the time limit, a stop): WITHDRAW (80)
 * </pre>
 *
 * <p>It extends the framework's {@link BossFight}, which gives it the participants, the ledger (his {@code damage} and the
 * {@code shield} strength the fighters wore down, both of which earn the Essence), the two boss bars, the arena, the
 * rewards, the refunds, the wipe, the abandonment and the time limit; what is left is his own: the phases and their set
 * pieces, the shield's windows and floors, the Four Horsemen, the speech, the Escape, the end in sand or in the
 * Phoenix's fire, and the fail-safe that sends him away if no one in the arena can break his shield.
 *
 * <p>It never places or breaks a block, and nothing it makes outlives it: the hazards are cleared, the cocoons let go,
 * the Horsemen and the boss discarded when it finishes.
 */
public final class ApocalypseFight extends BossFight {
    /** The stages of the fight. */
    public enum Stage {
        AWAKENING, FIGHT, TRANSITION_1, TRANSITION_2, ARMOUR_FALL, TRUE_FORM, ESCAPE, VANISHED, KNEEL, DEATH, WITHDRAW
    }

    // The timing of the set pieces, in ticks.
    public static final int AWAKENING_TICKS = 160;
    public static final int PORTAL_AT = 40;
    public static final int EMERGE_AT = 100;
    public static final int TRANSITION_1_TICKS = 120;
    public static final int TRANSITION_2_TICKS = 160;
    public static final int ARMOUR_FALL_TICKS = 60;
    public static final int KNEEL_TICKS = 100;
    public static final int DEATH_TICKS = 160;
    public static final int WITHDRAW_TICKS = 80;
    public static final int VANISH_TICKS = 200;
    public static final int ESCAPE_STAGGER_TICKS = 120;
    /** The speech's colour: the steel blue of his skin. */
    public static final int SPEECH_COLOR = 0x4A9BC4;
    /** Ticks without a blow after which the true form heals. */
    public static final int TRUE_FORM_REST = 80;
    /** The part of his health at which he tries to escape, and at which he kneels. */
    public static final double ESCAPE_AT = 0.15;
    public static final double KNEEL_AT = 0.05;

    private static final Component NAME = Component.translatable("entity.enhanced.apocalypse");

    private final Vec3 centre;
    private final Horsemen horsemen = new Horsemen(this);
    private final Map<UUID, List<float[]>> recentMind = new HashMap<>();
    @Nullable
    private ApocalypseEntity boss;
    private Stage stage = Stage.AWAKENING;
    private int stageTicks;
    private int phase = 1;
    private List<ServerPlayer> foes = List.of();
    private int staggerLeft;
    private boolean escapeUsed;
    private float escapeStartHealth;
    private int noBreakerTicks;
    private boolean spokeBreak;
    private boolean spokeDeflect;
    private boolean spokeFall;
    @Nullable
    private UUID lastDamager;
    private long lastDamageTick;
    private long phoenixAt = Long.MIN_VALUE / 2;
    private float yawToStarter;
    private List<Vec3> portalPoints = List.of();

    public ApocalypseFight(ServerLevel level, ApocalypseArena arena) {
        super(level, arena, new BossBars(Component.translatable("boss.enhanced.apocalypse.phase1"), BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.NOTCHED_6));
        this.centre = arena.center();
        bars().second().setColor(BossEvent.BossBarColor.YELLOW);
        bars().health().setVisible(false);
    }

    @Override
    public BossKind kind() {
        return BossKind.APOCALYPSE;
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the rest asks

    @Nullable
    public ApocalypseEntity boss() {
        return boss;
    }

    public Stage stage() {
        return stage;
    }

    public int stageTicks() {
        return stageTicks;
    }

    /** The phase, 1 to 3 (the true form is the third). */
    public int phase() {
        return phase;
    }

    /** The phase his attacks are chosen for. */
    public int attackPhase() {
        return phase;
    }

    public Horsemen horsemen() {
        return horsemen;
    }

    public boolean escapeUsed() {
        return escapeUsed;
    }

    /** The ticks left of the stagger at the start of a window (or of an interrupted Escape). */
    public int staggerLeft() {
        return staggerLeft;
    }

    /** Whether the fight has a window open and the stagger is over: the shield is broken and he attacks at half the rate. */
    public boolean inWindow() {
        return boss != null && boss.shield().state() == ApocalypseShield.State.BROKEN && staggerLeft == 0;
    }

    /** The fighters he can go after: alive, in the arena, not Creative and not Spectators; the nearest to him first. */
    public List<ServerPlayer> foes() {
        return foes;
    }

    /** How many fighters the fight counts for its numbers: those who took part, at most six. */
    public int fighterCount() {
        return Math.max(1, Math.min(6, participants().size()));
    }

    /** Whether nothing can hurt him now (a set piece, the end), except what bypasses invulnerability. */
    public boolean bossInvulnerable() {
        return isOver() || switch (stage) {
            case AWAKENING, TRANSITION_1, TRANSITION_2, ARMOUR_FALL, VANISHED, KNEEL, DEATH, WITHDRAW -> true;
            case FIGHT, TRUE_FORM, ESCAPE -> false;
        };
    }

    /** The ticks of the window the shield opens when it breaks: none for the third shield, which is gone for good. */
    public int windowTicksOfThisShield() {
        return phase >= 3 ? 0 : ApocalypseBossConfig.WINDOW_TICKS.get();
    }

    /** How much mind-power damage this player did to his shield in the last ten seconds. */
    public float recentMindDamage(UUID player) {
        List<float[]> list = recentMind.get(player);
        if (list == null) {
            return 0.0F;
        }
        long now = level().getGameTime();
        float sum = 0.0F;
        for (float[] entry : list) {
            if (now - (long) entry[0] <= 200) {
                sum += entry[1];
            }
        }
        return sum;
    }

    @Override
    protected String status() {
        String boss = this.boss == null ? "no boss yet" : "health " + Math.round(this.boss.designHealth()) + "/" + Math.round(this.boss.designMax())
                + ", shield " + this.boss.shield().state() + " " + Math.round(this.boss.shield().strength()) + "/" + Math.round(this.boss.shield().max());
        return stage + " (phase " + phase + "), " + boss;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The fight begins

    @Override
    protected void onBegin(ServerPlayer starter) {
        ServerLevel level = level();
        yawToStarter = starter.getYRot() + 180.0F;
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.APOCALYPSE_AWAKEN.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        ApocalypseFx.send(level, centre, ApocalypseFxPayload.RITUAL, 0, centre, Vec3.ZERO, 0, 0);
        arenaFx(true);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(centre) <= 96.0 * 96.0) {
                player.sendSystemMessage(Component.translatable("message.enhanced.apocalypse.trembles").withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        }
    }

    /** The picture of the sand wall and the amber sky for everyone near (and for whoever comes later: see the events). */
    private void arenaFx(boolean on) {
        ApocalypseArena arena = (ApocalypseArena) arena();
        ApocalypseFx.send(level(), centre, arena.fightRadius() + 64.0, on ? ApocalypseFxPayload.ARENA_ON : ApocalypseFxPayload.ARENA_OFF,
                id().hashCode(), centre, Vec3.ZERO, (int) Math.round(arena.containRadius()), (int) Math.round(arena.top() - centre.y));
    }

    /** Tells a player who has just come into view where the arena is. */
    public void tellArena(ServerPlayer player) {
        ApocalypseArena arena = (ApocalypseArena) arena();
        ApocalypseFx.sendTo(player, ApocalypseFxPayload.ARENA_ON, id().hashCode(), centre, Vec3.ZERO, (int) Math.round(arena.containRadius()),
                (int) Math.round(arena.top() - centre.y));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick

    @Override
    protected void onTick() {
        ServerLevel level = level();
        long now = level.getGameTime();
        stageTicks++;
        foes = computeFoes();
        if (boss != null && (boss.isRemoved() || !boss.isAlive())) {
            end(BossEnd.ABORT); // /kill, the void, whatever took him: no reward
            return;
        }
        if (boss != null) {
            boolean windowEnded = boss.shield().tick(now);
            switch (stage) {
                case AWAKENING -> tickAwakening(now);
                case FIGHT -> tickFight(now, windowEnded);
                case TRANSITION_1 -> tickTransitionOne();
                case TRANSITION_2 -> tickTransitionTwo();
                case ARMOUR_FALL -> tickArmourFall();
                case TRUE_FORM -> tickTrueForm(now);
                case ESCAPE -> tickEscape();
                case VANISHED -> tickVanished();
                case KNEEL -> tickKneel();
                case DEATH, WITHDRAW -> {
                }
            }
            if (!isOver()) {
                keepHome();
                horsemen.tick(level, foes);
                boss.syncShield();
            }
        } else if (stage == Stage.AWAKENING) {
            tickAwakening(now);
        }
        refreshBars();
    }

    private List<ServerPlayer> computeFoes() {
        List<ServerPlayer> list = new ArrayList<>();
        double reach = arena().containRadius() + 2.0;
        for (ServerPlayer player : present()) {
            if (ApocalypseEntity.isTargetable(player) && Math.hypot(player.getX() - centre.x, player.getZ() - centre.z) <= reach) {
                list.add(player);
            }
        }
        Vec3 from = boss != null ? boss.position() : centre;
        list.sort(Comparator.comparingDouble(p -> p.position().distanceToSqr(from)));
        return list;
    }

    // ---- the awakening ----

    private void tickAwakening(long now) {
        if (stageTicks == PORTAL_AT) {
            ApocalypseFx.send(level(), centre, ApocalypseFxPayload.PORTAL, -1, centre.add(0, 1.0, 0), centre.add(0, 1.0, 0), 60, 25);
            level().playSound(null, centre.x, centre.y, centre.z, ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
        }
        if (stageTicks == EMERGE_AT && boss == null) {
            emerge();
        }
        if (stageTicks == 120) {
            say("speech.enhanced.apocalypse.awaken");
        }
        if (stageTicks == 150) {
            say("speech.enhanced.apocalypse.strong");
        }
        if (stageTicks >= AWAKENING_TICKS && boss != null) {
            enterFight();
        }
    }

    /** He steps out of the portal: invulnerable, his eyes dark, his shield knitting from the ground up. */
    private void emerge() {
        ServerLevel level = level();
        ApocalypseEntity body = ModEntities.APOCALYPSE.get().create(level);
        body.moveTo(centre.x, centre.y, centre.z, yawToStarter, 0.0F);
        body.applyBody(fighterCount());
        body.setPhase(phase);
        body.setState(ApocalypseState.AWAKENING);
        body.shield().form(ApocalypseShield.strengthFor(phase, fighterCount()));
        body.syncShield();
        boss = body;
        spawn(body);
        bars().health().setVisible(true);
        bars().showSecond(true);
        ApocalypseFx.send(level, centre, ApocalypseFxPayload.SHIELD_REFORM, body.getId(), centre, Vec3.ZERO, 0, ApocalypseShield.State.FORMING.ordinal());
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.APOCALYPSE_SHIELD_UP.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
    }

    private void enterFight() {
        stage = Stage.FIGHT;
        stageTicks = 0;
        boss.setState(ApocalypseState.IDLE);
        boss.brain().clearCooldowns();
    }

    // ---- the fight proper ----

    private void tickFight(long now, boolean windowEnded) {
        ApocalypseShield shield = boss.shield();
        // His healing while a shield is up, never above the ceiling of the phase.
        if (shield.isUp() && now % 20 == 0) {
            boss.heal(ApocalypseBossConfig.REGEN_PER_SECOND.get().floatValue(), ceiling());
        }
        checkBreaker(shield);
        if (shield.state() == ApocalypseShield.State.BROKEN) {
            if (staggerLeft > 0 && --staggerLeft == 0) {
                endStagger();
            }
            if (boss.getHealth() <= boss.floorHealth() + 1.0E-3F) {
                startTransition();
                return;
            }
            if (windowEnded) {
                closeWindow();
            }
        }
        if (staggerLeft == 0 && !isOver()) {
            boss.brain().tick(boss, this);
        }
    }

    /** The most health the phase's shield lets him have: all of it, then two thirds, then one third. */
    private double ceiling() {
        return phase == 1 ? 1.0 : phase == 2 ? 2.0 / 3.0 : 1.0 / 3.0;
    }

    /** The fail-safe: no one in the arena can break the shield for a minute, and he withdraws. */
    private void checkBreaker(ApocalypseShield shield) {
        if (!shield.isUp()) {
            noBreakerTicks = 0;
            return;
        }
        boolean breaker = false;
        for (ServerPlayer player : present()) {
            breaker |= MindPower.isBreaker(player);
        }
        noBreakerTicks = breaker ? 0 : noBreakerTicks + 1;
        if (noBreakerTicks >= ApocalypseBossConfig.NO_BREAKER_SECONDS.get() * 20) {
            end(BossEnd.WITHDRAW);
        }
    }

    /**
     * Mind power wore the shield. Called by the boss as the blow lands: the fighter is credited with the strength they
     * removed (as much as damage to his health), the shield rings, and if it broke the window opens.
     */
    void onShieldWorn(ServerPlayer attacker, float amount, boolean broke, Vec3 at) {
        ServerLevel level = level();
        credit(attacker, "shield", amount);
        recentMind.computeIfAbsent(attacker.getUUID(), k -> new ArrayList<>()).add(new float[]{level.getGameTime(), amount});
        if (boss == null) {
            return;
        }
        ApocalypseFx.send(level, at, broke ? ApocalypseFxPayload.SHIELD_BREAK : ApocalypseFxPayload.SHIELD_HIT, boss.getId(), at, Vec3.ZERO,
                Math.round(boss.shield().fraction() * 100.0F), boss.shield().state().ordinal());
        level.playSound(null, at.x, at.y, at.z, broke ? ModSounds.APOCALYPSE_SHIELD_BREAK.get() : ModSounds.APOCALYPSE_SHIELD_HIT.get(),
                SoundSource.HOSTILE, broke ? 2.0F : 1.0F, 1.0F);
        if (broke) {
            onShieldBroken(attacker);
        }
    }

    /** The shield is at zero: the first two open a window; the third is the armour falling away for good. */
    private void onShieldBroken(@Nullable ServerPlayer breaker) {
        boss.brain().reset();
        if (phase >= 3) {
            startArmourFall();
            return;
        }
        if (!spokeBreak) {
            spokeBreak = true;
            say("speech.enhanced.apocalypse.impressive");
        }
        // Everyone close is pushed back (no damage), and he kneels, staggered, for the first moments of the window.
        for (ServerPlayer player : foes) {
            if (player.distanceTo(boss) <= 6.0F) {
                Vec3 out = SandControl.flat(boss.position(), player.position());
                player.knockback(1.0, -out.x, -out.z);
                player.hurtMarked = true;
            }
        }
        boss.setFloor(phase == 1 ? 2.0 / 3.0 : 1.0 / 3.0);
        staggerLeft = ApocalypseBossConfig.STAGGER_TICKS.get();
        boss.brain().setInWindow(false);
        if (staggerLeft > 0) {
            boss.setState(ApocalypseState.STAGGERED);
        } else {
            endStagger();
        }
    }

    private void endStagger() {
        staggerLeft = 0;
        boss.setOpen(false);
        boss.setState(ApocalypseState.IDLE);
        ApocalypseBrain brain = boss.brain();
        brain.setAfterStagger(true);
        brain.setInWindow(true);
        brain.clearGap();
    }

    /** The window ran out before the floor: the shield knits again at its full strength, and he is no longer open. */
    private void closeWindow() {
        boss.shield().reform(ApocalypseShield.strengthFor(phase, fighterCount()));
        boss.setFloor(0.0);
        boss.setArmourCut(false);
        boss.brain().setInWindow(false);
        boss.brain().setAfterStagger(false);
        staggerLeft = 0;
        if (boss.state() == ApocalypseState.STAGGERED) {
            boss.setState(ApocalypseState.IDLE);
        }
        ApocalypseFx.send(level(), boss.position(), ApocalypseFxPayload.SHIELD_REFORM, boss.getId(), boss.position(), Vec3.ZERO, 0, ApocalypseShield.State.REFORMING.ordinal());
        level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_SHIELD_UP.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
    }

    /** A blow of any kind reached him, shielded or not: the fight notes who and what (his speech, the Phoenix's fire). */
    void noteAttack(DamageSource source) {
        if (PhoenixPower.isPhoenixMindPower(source)) {
            phoenixAt = level().getGameTime();
        }
    }

    /** A blow took health from him: whose it was (the last blow credit is the one who took him to the end). */
    void noteDamage(ServerPlayer player) {
        lastDamager = player.getUUID();
        lastDamageTick = level().getGameTime();
    }

    /** The shield turned something away: the first time he says why. */
    void onDeflected(DamageSource source) {
        if (!spokeDeflect && boss != null && boss.shield().isUp()) {
            spokeDeflect = true;
            say("speech.enhanced.apocalypse.babel");
        }
    }

    /** A fighter fell: the first one earns a word. */
    void participantFell() {
        if (!spokeFall && !isOver()) {
            spokeFall = true;
            say("speech.enhanced.apocalypse.useless");
        }
    }

    /** He adapted to Quicksilver Time: an Entomb on the feet of the speedster (the film's answer). */
    void onAdapted() {
        ServerPlayer speedster = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : foes) {
            boolean quick = Powers.is(player, Power.QUICKSILVER) || (Powers.is(player, Power.TOMMY) && TommyAbilities.isRunning(player));
            if (quick && SandControl.entombRefusal(player) == null && boss != null && player.distanceToSqr(boss) < best) {
                best = player.distanceToSqr(boss);
                speedster = player;
            }
        }
        if (speedster != null) {
            SandControl.entomb(level(), boss, speedster, 40, ApocalypseBossConfig.ENTOMB_DAMAGE_PER_SECOND.get().floatValue(),
                    ApocalypseBossConfig.COCOON_HEALTH.get().floatValue(), this);
        }
    }

    /** {@code /kill} on him (or anything that kills him outside the film): the fight is over, no one is rewarded. */
    void abortKilled() {
        if (!isOver()) {
            end(BossEnd.ABORT);
        }
    }

    // ---- the transitions ----

    private void startTransition() {
        boss.setFloor(0.0);
        boss.setArmourCut(false);
        boss.brain().reset();
        boss.brain().setInWindow(false);
        boss.shield().remove();
        staggerLeft = 0;
        boss.rescale(fighterCount());
        stageTicks = 0;
        boss.setNoGravity(true);
        boss.setState(ApocalypseState.HOVER);
        if (phase == 1) {
            stage = Stage.TRANSITION_1;
            say("speech.enhanced.apocalypse.horsemen");
        } else {
            stage = Stage.TRANSITION_2;
            horsemen.crumble();
            say("speech.enhanced.apocalypse.fall");
        }
    }

    /** He rises {@code height} blocks above the ground of the middle and hangs there. */
    private void hover(double height) {
        double target = centre.y + height;
        double dy = Math.max(-0.15, Math.min(0.15, (target - boss.getY()) * 0.2));
        boss.setDeltaMovement(0.0, dy, 0.0);
        boss.getNavigation().stop();
    }

    private void landed() {
        boss.setNoGravity(false);
        boss.setState(ApocalypseState.IDLE);
        boss.brain().clearCooldowns();
        stage = Stage.FIGHT;
        stageTicks = 0;
    }

    private void tickTransitionOne() {
        hover(1.0);
        ServerLevel level = level();
        if (stageTicks == 20) {
            portalPoints = ring(Math.max(1, Horsemen.countFor(fighterCount())), 8.0);
            for (Vec3 point : portalPoints) {
                ApocalypseFx.send(level, point, ApocalypseFxPayload.PORTAL, -1, point.add(0, 1.0, 0), point.add(0, 1.0, 0), 50, 15);
            }
            level.playSound(null, centre.x, centre.y, centre.z, ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.HOSTILE, 3.0F, 0.8F);
            for (ServerPlayer player : foes) {
                if (Powers.is(player, Power.MAGNETO)) {
                    player.sendSystemMessage(Component.literal("<").append(NAME).append("> ")
                            .append(Component.translatable("speech.enhanced.apocalypse.erik")).withStyle(s -> s.withColor(SPEECH_COLOR)));
                }
            }
        }
        if (stageTicks == 60) {
            callHorsemen();
        }
        if (stageTicks == 100) {
            boss.shield().form(ApocalypseShield.strengthFor(2, fighterCount()));
            level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_SHIELD_UP.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        if (stageTicks >= TRANSITION_1_TICKS) {
            phase = 2;
            boss.setPhase(2);
            landed();
        }
    }

    /** The Horsemen step out of the four portals. */
    private void callHorsemen() {
        int count = Horsemen.countFor(fighterCount());
        List<Vec3> points = portalPoints.size() == count ? portalPoints : ring(count, 8.0);
        for (int i = 0; i < count; i++) {
            Vec3 point = points.get(i);
            Vec3 ground = new Vec3(point.x, SandControl.groundAt(level(), point.x, centre.y + 4.0, point.z), point.z);
            // Whoever stands in the portal is pushed out of it (no damage).
            for (ServerPlayer player : foes) {
                if (player.position().distanceTo(ground) < 1.8) {
                    Vec3 out = SandControl.flat(ground, player.position());
                    player.knockback(1.0, -out.x, -out.z);
                    player.hurtMarked = true;
                }
            }
            horsemen.spawn(level(), Horsemen.Kind.values()[i % Horsemen.Kind.values().length], ground);
        }
    }

    /** {@code count} points evenly round the middle at {@code radius}. */
    private List<Vec3> ring(int count, double radius) {
        List<Vec3> points = new ArrayList<>();
        double start = level().random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < count; i++) {
            double angle = start + Math.PI * 2.0 * i / Math.max(1, count);
            points.add(new Vec3(centre.x + Math.cos(angle) * radius, centre.y, centre.z + Math.sin(angle) * radius));
        }
        return points;
    }

    private void tickTransitionTwo() {
        hover(3.0);
        ServerLevel level = level();
        double maxRadius = ((ApocalypseArena) arena()).fightRadius() + 2.0;
        for (int at : new int[]{24, 54, 84}) {
            if (stageTicks == at) {
                ApocalypseFx.send(level, centre, ApocalypseFxPayload.RING_TELL, boss.getId(), centre, Vec3.ZERO, 16, (int) maxRadius);
            }
        }
        for (int at : new int[]{40, 70, 100}) {
            if (stageTicks == at) {
                SandHazards.ring(level, boss, id(), new Vec3(boss.getX(), centre.y, boss.getZ()), 0.9, 1.5, 1.1, maxRadius,
                        ApocalypseBossConfig.RING_DAMAGE.get().floatValue());
                level.playSound(null, centre.x, centre.y, centre.z, ModSounds.SAND_WAVE.get(), SoundSource.HOSTILE, 3.0F, 0.6F);
            }
        }
        if (stageTicks == 120) {
            boss.shield().form(ApocalypseShield.strengthFor(3, fighterCount()));
            level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_SHIELD_UP.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        if (stageTicks >= TRANSITION_2_TICKS) {
            phase = 3;
            boss.setPhase(3);
            landed();
        }
    }

    // ---- the armour falls ----

    /** The third break: the shield is gone for good and the armour falls (the film). */
    private void startArmourFall() {
        ServerLevel level = level();
        stage = Stage.ARMOUR_FALL;
        stageTicks = 0;
        boss.setFloor(escapeUsed ? KNEEL_AT : ESCAPE_AT);
        boss.setState(ApocalypseState.ARMOUR_FALL);
        boss.setTrueForm(true);
        boss.setArmourCut(false);
        boss.brain().reset();
        bars().showSecond(false);
        for (ServerPlayer player : foes) {
            if (player.distanceTo(boss) <= 8.0F) {
                Vec3 out = SandControl.flat(boss.position(), player.position());
                player.knockback(1.2, -out.x, -out.z);
                player.hurtMarked = true;
            }
        }
        ApocalypseFx.send(level, boss.position(), ApocalypseFxPayload.ARMOUR_FALL, boss.getId(), boss.position(), Vec3.ZERO, ARMOUR_FALL_TICKS, 0);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_SPEAK.get(), SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_SHIELD_BREAK.get(), SoundSource.HOSTILE, 3.0F, 0.6F);
    }

    private void tickArmourFall() {
        if (stageTicks >= ARMOUR_FALL_TICKS) {
            stage = Stage.TRUE_FORM;
            stageTicks = 0;
            boss.setState(ApocalypseState.IDLE);
            boss.brain().clearCooldowns();
        }
    }

    // ---- the true form ----

    private void tickTrueForm(long now) {
        if (staggerLeft > 0) {
            if (--staggerLeft == 0) {
                endStagger();
                boss.brain().setAfterStagger(false);
                boss.brain().setInWindow(false);
            }
        } else {
            boss.brain().tick(boss, this);
        }
        // What the blow that brought him to the floor did comes first: his healing must not mend him past the line (he is held
        // at 15 or 5 percent) in the same tick, and so spare him the Escape or the kneel that the blow earned.
        float fraction = boss.healthFraction();
        if (fraction <= KNEEL_AT + 1.0E-3F) {
            startKneel();
            return;
        }
        if (!escapeUsed && fraction <= ESCAPE_AT + 1.0E-3F) {
            startEscape();
            return;
        }
        // His healing factor: left alone, he mends.
        if (now - lastDamageTick >= TRUE_FORM_REST && now % 20 == 0) {
            boss.heal(ApocalypseBossConfig.TRUE_FORM_REGEN_PER_SECOND.get().floatValue(), ceiling());
        }
    }

    /** Once, at 15 percent, he tries to teleport away; it can be interrupted. */
    private void startEscape() {
        escapeUsed = true;
        stage = Stage.ESCAPE;
        stageTicks = 0;
        boss.setFloor(KNEEL_AT);
        escapeStartHealth = boss.getHealth();
        boss.takeLightning();
        boss.brain().reset();
        boss.setState(ApocalypseState.ESCAPING);
        say("speech.enhanced.apocalypse.no");
        ApocalypseFx.send(level(), boss.position(), ApocalypseFxPayload.ESCAPE, boss.getId(), boss.position(), Vec3.ZERO,
                ApocalypseBossConfig.ESCAPE_CHANNEL_TICKS.get(), 0);
        level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.HOSTILE, 3.0F, 0.6F);
    }

    private void tickEscape() {
        float lost = escapeStartHealth - boss.getHealth();
        if (boss.takeLightning() || lost >= ApocalypseBossConfig.ESCAPE_INTERRUPT_FRACTION.get() * boss.getMaxHealth()) {
            interruptEscape();
            return;
        }
        if (stageTicks >= ApocalypseBossConfig.ESCAPE_CHANNEL_TICKS.get()) {
            // A Hex wall refuses the teleport: he cannot leave a Hex he is in.
            if (Teleports.crossesHex(level(), boss, boss.position().add(80.0, 0.0, 0.0))) {
                interruptEscape();
            } else {
                vanish();
            }
        }
    }

    /** The escape fails: he is staggered for six seconds (open to every blow, a quarter harder) and cannot try again. */
    private void interruptEscape() {
        stage = Stage.TRUE_FORM;
        stageTicks = 0;
        staggerLeft = ESCAPE_STAGGER_TICKS;
        boss.setOpen(true);
        boss.setState(ApocalypseState.STAGGERED);
        boss.brain().setInWindow(false);
        level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_SHIELD_BREAK.get(), SoundSource.HOSTILE, 2.0F, 0.7F);
        ApocalypseFx.send(level(), boss.position(), ApocalypseFxPayload.PORTAL, boss.getId(), boss.position(), boss.position(), 10, 25);
    }

    /** He is gone for ten seconds. */
    private void vanish() {
        stage = Stage.VANISHED;
        stageTicks = 0;
        boss.setInvisible(true);
        boss.setNoAi(true);
        boss.noPhysics = true;
        boss.setSilent(true);
        boss.setState(ApocalypseState.IDLE);
        bars().showSecond(false);
        SandControl.puff(level(), boss.position().add(0, 1.0, 0), 30, 0.8);
        level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.HOSTILE, 3.0F, 1.2F);
    }

    private void tickVanished() {
        if (stageTicks >= VANISH_TICKS) {
            // He returns healed to escapeHealTo, with a new third shield: a complete re-run of the third break.
            boss.moveTo(centre.x, centre.y, centre.z, boss.getYRot(), 0.0F);
            boss.setInvisible(false);
            boss.setNoAi(false);
            boss.noPhysics = false;
            boss.setSilent(false);
            double heal = ApocalypseBossConfig.ESCAPE_HEAL_TO.get();
            if (boss.healthFraction() < heal) {
                boss.setFraction(heal);
            }
            boss.setFloor(0.0);
            boss.shield().form(ApocalypseShield.strengthFor(3, fighterCount()));
            bars().showSecond(true);
            stage = Stage.FIGHT;
            stageTicks = 0;
            boss.brain().clearCooldowns();
            ApocalypseFx.send(level(), boss.position(), ApocalypseFxPayload.PORTAL, boss.getId(), boss.position(), boss.position(), 20, 25);
        }
    }

    // ---- the end ----

    /** At 5 percent he kneels: "All is revealed." */
    private void startKneel() {
        stage = Stage.KNEEL;
        stageTicks = 0;
        boss.setFloor(KNEEL_AT);
        boss.brain().reset();
        boss.setState(ApocalypseState.KNEELING);
        say("speech.enhanced.apocalypse.revealed");
        if (lastDamager != null) {
            ServerPlayer finisher = server().getPlayerList().getPlayer(lastDamager);
            if (finisher != null) {
                lastBlow(finisher);
            }
        }
        boolean fire = level().getGameTime() - phoenixAt <= ApocalypseBossConfig.PHOENIX_FINISH_SECONDS.get() * 20L;
        boss.setDiesInFire(fire);
    }

    private void tickKneel() {
        if (stageTicks >= KNEEL_TICKS) {
            stage = Stage.DEATH;
            stageTicks = 0;
            end(BossEnd.VICTORY);
        }
    }

    @Override
    protected void onEnd(BossEnd why) {
        horsemen.crumble();
        SandHazards.clearFight(id());
        releaseCocoons();
        if (boss != null) {
            boss.brain().reset();
            boss.setNoGravity(false);
        }
        if (why == BossEnd.VICTORY) {
            stage = Stage.DEATH;
            if (boss != null && boss.isAlive()) {
                boss.setState(ApocalypseState.DYING);
                ApocalypseFx.send(level(), boss.position(), boss.diesInFire() ? ApocalypseFxPayload.DEATH_PHOENIX : ApocalypseFxPayload.DEATH_SAND,
                        boss.getId(), boss.position(), Vec3.ZERO, DEATH_TICKS, 0);
                level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_DEATH.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
            }
            return;
        }
        stage = Stage.WITHDRAW;
        if (boss != null && boss.isAlive()) {
            boss.setState(ApocalypseState.WITHDRAWING);
            boss.setTarget(null);
            ApocalypseFx.send(level(), boss.position(), ApocalypseFxPayload.PORTAL, boss.getId(), boss.position(), boss.position(), WITHDRAW_TICKS, 25);
            level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
            if (why == BossEnd.WIPE || why == BossEnd.WITHDRAW) {
                say("speech.enhanced.apocalypse.unworthy");
            }
        }
    }

    @Override
    protected boolean windDown(int ticks) {
        if (boss == null || !boss.isAlive() || boss.isRemoved()) {
            return true;
        }
        if (stage == Stage.DEATH) {
            if (ticks >= DEATH_TICKS) {
                boss.discard(); // no drops, no experience: the reward is the Essence
                return true;
            }
            return false;
        }
        if (ticks >= WITHDRAW_TICKS) {
            boss.discard();
            return true;
        }
        return false;
    }

    @Override
    protected void onFinished(boolean forced) {
        SandHazards.clearFight(id());
        releaseCocoons();
        horsemen.crumble();
        arenaFx(false);
    }

    /** Every cocoon of the fight lets go, so that nobody keeps the effect of a cocoon that is no more. */
    private void releaseCocoons() {
        for (Entity entity : entities()) {
            if (entity instanceof SandCocoonEntity cocoon) {
                cocoon.release();
            }
        }
    }

    @Override
    protected List<ItemStack> rewards(Participant participant) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ModItems.ESSENCE.get()));
        if (ApocalypseBossConfig.DROPS_PHOENIX_EMBER.get()) {
            ServerPlayer online = server().getPlayerList().getPlayer(participant.id());
            if (online != null && !Powers.power(online).hasPowers()) {
                stacks.add(new ItemStack(ModItems.PHOENIX_EMBER.get()));
            }
        }
        return stacks;
    }

    @Override
    protected java.util.Set<String> rewardCounters() {
        return java.util.Set.of(DAMAGE, "shield");
    }

    // ---------------------------------------------------------------------------------------------------------
    // Keeping him home, the bars, the speech

    /** He keeps within the circle, and if he falls out of the world he is put back. */
    private void keepHome() {
        double leash = ((ApocalypseArena) arena()).fightRadius() - 2.0;
        double dx = boss.getX() - centre.x;
        double dz = boss.getZ() - centre.z;
        boolean outside = Math.hypot(dx, dz) > leash + 3.0 || boss.getY() < centre.y - 24.0;
        if (outside && stage != Stage.VANISHED) {
            boss.moveTo(centre.x, centre.y, centre.z, boss.getYRot(), 0.0F);
            boss.setDeltaMovement(Vec3.ZERO);
            boss.getNavigation().stop();
        }
    }

    private void refreshBars() {
        BossBars bars = bars();
        if (boss == null) {
            return;
        }
        int shownPhase = Math.max(1, Math.min(3, phase));
        bars.health().setName(Component.translatable("boss.enhanced.apocalypse.phase" + shownPhase));
        bars.health().setProgress(Math.max(0.0F, Math.min(1.0F, boss.designHealth() / boss.designMax())));
        ApocalypseShield shield = boss.shield();
        switch (shield.state()) {
            case FORMING, REFORMING -> {
                bars.second().setName(Component.translatable("boss.enhanced.apocalypse.shield.forming"));
                bars.second().setProgress(shield.fraction());
            }
            case UP -> {
                bars.second().setName(Component.translatable("boss.enhanced.apocalypse.shield"));
                bars.second().setProgress(shield.fraction());
            }
            case BROKEN -> {
                bars.second().setName(Component.translatable("boss.enhanced.apocalypse.shield.broken", String.format(java.util.Locale.ROOT, "%.1f", shield.windowLeft() / 20.0)));
                bars.second().setProgress(Math.min(1.0F, shield.windowLeft() / (float) Math.max(1, ApocalypseBossConfig.WINDOW_TICKS.get())));
            }
            case GONE -> bars.showSecond(false);
        }
        if (shield.state() != ApocalypseShield.State.GONE && !isOver() && stage != Stage.VANISHED) {
            bars.showSecond(true);
        }
    }

    /** He speaks: a chat line in his colour with his voice. */
    private void say(String key) {
        speak(NAME, SPEECH_COLOR, Component.translatable(key), ModSounds.APOCALYPSE_SPEAK.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // For the tests

    /**
     * Jumps to a stage of the fight with everything in the state it would have: the boss exists, the phase's shield is up
     * (or not, as the stage says), his health is where the phase puts it. {@code phase} matters for {@link Stage#FIGHT}.
     */
    public void forceState(Stage target, int phase) {
        if (boss == null) {
            emerge();
        }
        horsemen.crumble();
        SandHazards.clearFight(id());
        boss.brain().reset();
        boss.setFloor(0.0);
        boss.setArmourCut(false);
        boss.setOpen(false);
        boss.setNoGravity(false);
        boss.setInvisible(false);
        boss.setNoAi(false);
        boss.noPhysics = false;
        boss.brain().setInWindow(false);
        boss.brain().setAfterStagger(false);
        staggerLeft = 0;
        stageTicks = 0;
        this.phase = phase;
        boss.setPhase(phase);
        boss.setTrueForm(false);
        boss.rescale(fighterCount());
        bars().health().setVisible(true);
        switch (target) {
            case AWAKENING -> {
                stage = Stage.AWAKENING;
            }
            case FIGHT -> {
                stage = Stage.FIGHT;
                boss.setState(ApocalypseState.IDLE);
                boss.setFraction(phase == 1 ? 1.0 : phase == 2 ? 2.0 / 3.0 : 1.0 / 3.0);
                boss.shield().raiseNow(ApocalypseShield.strengthFor(phase, fighterCount()));
            }
            case TRANSITION_1 -> {
                this.phase = 1;
                boss.setFraction(2.0 / 3.0);
                startTransition();
            }
            case TRANSITION_2 -> {
                this.phase = 2;
                boss.setFraction(1.0 / 3.0);
                startTransition();
            }
            case ARMOUR_FALL -> {
                this.phase = 3;
                boss.setPhase(3);
                boss.setFraction(1.0 / 3.0);
                boss.shield().remove();
                startArmourFall();
            }
            case TRUE_FORM, ESCAPE, VANISHED, KNEEL, DEATH, WITHDRAW -> {
                this.phase = 3;
                boss.setPhase(3);
                boss.setTrueForm(true);
                boss.shield().remove();
                boss.setFraction(1.0 / 3.0);
                boss.setFloor(escapeUsed ? KNEEL_AT : ESCAPE_AT);
                stage = Stage.TRUE_FORM;
                boss.setState(ApocalypseState.IDLE);
                if (target == Stage.ESCAPE) {
                    boss.setFraction(ESCAPE_AT);
                    startEscape();
                } else if (target == Stage.KNEEL) {
                    boss.setFraction(KNEEL_AT);
                    startKneel();
                }
            }
        }
        boss.syncShield();
    }

    /** Test hook: the Escape has been used up (or not). */
    public void setEscapeUsed(boolean used) {
        this.escapeUsed = used;
    }

    /** Test hook: who struck the last blow before the end. */
    @Nullable
    public UUID lastDamager() {
        return lastDamager;
    }

    /** The Husks the fight has called. */
    public List<Husk> horsemenAlive() {
        return horsemen.alive();
    }

    /** The living entity of the fight that is not the boss (for the tests that look for minions). */
    public List<LivingEntity> minions() {
        List<LivingEntity> minions = new ArrayList<>();
        for (Entity entity : entities()) {
            if (entity instanceof LivingEntity living && entity != boss) {
                minions.add(living);
            }
        }
        return minions;
    }

    @Nullable
    public SoundEvent speechVoice() {
        return ModSounds.APOCALYPSE_SPEAK.get();
    }
}
