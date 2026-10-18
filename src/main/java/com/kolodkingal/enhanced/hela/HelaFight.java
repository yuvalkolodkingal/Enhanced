package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityBuild;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossBars;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.HelaBossConfig;
import com.kolodkingal.enhanced.odin.Vault;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The fight against Hela in the throne room of Asgard (3.5, PLAN 6.2). The framework ({@link BossFight}) keeps the
 * participants, the ledger, the wipe, the abandonment, the rest, the rewards and the bars; this class is what is hers:
 *
 * <pre>
 * ARRIVING (6 s: the murals crack, smoke pours from the seal, she forms on the dais)
 *   -&gt; phase 1 "The Goddess" (volleys, walls, shadow steps, blade melee) -&gt; [phase2At] RISE (3 s, nothing hurts her)
 *   -&gt; phase 2 "The Army of the Dead" (berserker waves, Fenris with his bar, blade rain; the Asgardians join)
 *   -&gt; [phase3At] phase 3 "The Strength of Asgard" (a channel on the dais every 20 s: broken by 6% of her health, or she
 *      heals 10%; the Blade Storm every 15 s) -&gt; she falls: DYING (she dissolves) -&gt; VICTORY
 * any other end: LEAVING (she sinks back into the seal)
 * </pre>
 *
 * <p>Her health is {@code health} for one fighter and 35 percent more for each other, recomputed while fighters come in
 * the first 20 seconds. From the second phase she catches a thrown Mjolnir in her hand and crushes it (once a minute;
 * Stormbreaker she lets by). Every rewarded fighter gets Hela's Headdress and the right to Gungnir ({@code HELA_VICTOR}).
 * Her first defeat wakes Odin from the Odinsleep and opens the vault ({@link Vault#open}).
 */
public final class HelaFight extends BossFight {
    /** The colour of her lines: the green of her blades. */
    public static final int SPEECH_COLOR = 0x3FBF6F;
    /**
     * When in her arrival the headdress begins to grow (it is grown at {@link #BOOM_AT}), the boom of {@code hela_arrive}
     * (5.2 s into the 6-s sound, played at the fight's first tick: she is whole and flings her arms wide at it), and when
     * she speaks.
     */
    public static final int FORM_AT = 90;
    public static final int BOOM_AT = 104;
    public static final int SPEAK_AT = 108;
    /** Fighters who come within the first 20 seconds make her stronger. */
    public static final int SCALE_TICKS = 400;
    /** The radius of the throne room's arena, and its join radius. */
    public static final double THRONE_RADIUS = 34.0;
    /** The Asgardians are called against her army this often. */
    private static final int COMMAND_EVERY = 100;
    /** She catches a thrown Mjolnir within this reach of her hand. */
    public static final double CATCH_REACH = 3.5;
    public static final int CATCH_COOLDOWN = 1200;
    /** The share of her health a participant must have dealt to earn the reward (or strike the last blow). */
    public static final double REWARD_SHARE = 0.02;
    /** The tag a headdress of the reward carries: the UUID of the one it was given to (it gives him the right to Gungnir). */
    public static final String VICTOR_TAG = "hela_victor";

    private static final Component NAME = Component.translatable("entity.enhanced.hela");

    private final Vec3 dais;
    private final Vec3 seal;
    @Nullable
    private HelaEntity boss;
    @Nullable
    private FenrisEntity fenris;
    private boolean fenrisCalled;
    private int phase = 1;
    private int scaledFor;
    private List<ServerPlayer> foes = List.of();
    @Nullable
    private UUID lastAttacker;
    private long lastAttackAt;
    private long nextChannelAt;
    private long nextCatchAt;
    private boolean ended;
    private boolean muralsBack;

    private HelaFight(ServerLevel level, BossArena arena, Vec3 dais, Vec3 seal) {
        super(level, arena, new BossBars(Component.translatable("boss.enhanced.hela"), BossEvent.BossBarColor.GREEN,
                BossEvent.BossBarOverlay.NOTCHED_10));
        this.dais = dais;
        this.seal = seal;
        bars().second().setName(Component.translatable("boss.enhanced.hela.fenris"));
        bars().second().setColor(BossEvent.BossBarColor.RED);
        bars().second().setOverlay(BossEvent.BossBarOverlay.PROGRESS);
    }

    /**
     * A fight in {@code level} round {@code center} (radius {@code radius}, a wall, the blocks protected, from {@code floor}
     * to {@code ceiling}): she forms at {@code dais}, the smoke pours from {@code seal}.
     */
    public static HelaFight create(ServerLevel level, Vec3 center, double radius, double floor, double ceiling, Vec3 dais, Vec3 seal) {
        BossArena arena = new BossArena(level, center, radius, BossArena.Containment.WALL, true)
                .containAt(radius)
                .joinWithin(radius + 6.0)
                .abandonBeyond(radius + 24.0)
                .vertical(floor - 12.0, ceiling)
                .tickets((int) Math.ceil(radius + 12.0), 2)
                .tractorDepth(0.0) // (no tractor: the hall has a floor, and its roof is no "surface" to be lifted to)
                .keepDrops(true)
                .pushback(1.0F, 0);
        return new HelaFight(level, arena, dais, seal);
    }

    /** A fight begun at once round {@code center} in the starter's level, with her forming at {@code center} (the tests). */
    @VisibleForTesting
    public static HelaFight spawnAt(ServerLevel level, Vec3 center, double radius, ServerPlayer starter) {
        HelaFight fight = create(level, center, radius, center.y, center.y + 16.0, center, center.add(0.0, 2.0, -radius * 0.5));
        BossFights.begin(fight, starter);
        return fight;
    }

    @Override
    public BossKind kind() {
        return BossKind.HELA;
    }

    @Nullable
    public HelaEntity boss() {
        return boss;
    }

    @Nullable
    public FenrisEntity fenris() {
        return fenris;
    }

    public int phase() {
        return phase;
    }

    public Vec3 dais() {
        return dais;
    }

    /** The fighters she can go after: alive, in the arena, not Creative and not Spectators; the nearest to her first. */
    public List<ServerPlayer> foes() {
        return foes;
    }

    /**
     * How many fighters her numbers count: the participants who are not in Creative (an operator flying through does not
     * make her stronger), at least one.
     */
    public int fighterCount() {
        int count = 0;
        for (Participant participant : participants()) {
            if (!participant.wasCreative()) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    /** How many blades a volley throws: 3, or 5 with three fighters or more, and in the third phase. */
    public int volleyCount() {
        return fighterCount() >= 3 || phase >= 3 ? 5 : 3;
    }

    /** The size of a wave of berserkers: {@code berserkerWave} + {@code berserkerPerFighter} for each fighter after the first. */
    public int waveSize() {
        return HelaBossConfig.BERSERKER_WAVE.get() + HelaBossConfig.BERSERKER_PER_FIGHTER.get() * (fighterCount() - 1);
    }

    /** Her berserkers who stand. */
    public List<BerserkerEntity> berserkers() {
        List<BerserkerEntity> found = new ArrayList<>();
        for (Entity e : entities()) {
            if (e instanceof BerserkerEntity berserker) {
                found.add(berserker);
            }
        }
        return found;
    }

    /** The foe farthest from her (where a Shadow Step takes her), or null. */
    @Nullable
    public LivingEntity farthestFoe() {
        return foes.isEmpty() || boss == null ? null : foes.get(foes.size() - 1);
    }

    /** Marks what one of her attacks made as the fight's (the spikes, the falling blades). */
    <T extends Entity> void adoptAll(List<T> made) {
        for (T e : made) {
            adopt(e);
        }
    }

    /** A fighter struck her: she turns on him. */
    void noteAttacker(ServerPlayer player) {
        lastAttacker = player.getUUID();
        lastAttackAt = level().getGameTime();
    }

    @Override
    protected String status() {
        String body = boss == null ? "no boss" : boss.state().name().toLowerCase(Locale.ROOT) + ", health " + Math.round(boss.designHealth())
                + "/" + Math.round(boss.designMax());
        return (isOver() ? "ending (" + result() + ")" : "phase " + phase) + ", " + body + ", berserkers " + berserkers().size()
                + (fenris != null && fenris.isAlive() ? ", Fenris " + Math.round(fenris.getHealth()) : "");
    }

    // ---------------------------------------------------------------------------------------------------------
    // The arrival

    @Override
    protected void onBegin(ServerPlayer starter) {
        ServerLevel level = level();
        scaledFor = fighterCount();
        HelaEntity hela = new HelaEntity(ModEntities.HELA.get(), level);
        hela.moveTo(dais.x, dais.y, dais.z, yawToward(dais, arena().center()), 0.0F);
        hela.attach(this);
        hela.applyBody(scaledFor);
        hela.setHealth(hela.getMaxHealth());
        hela.setState(HelaState.ARRIVING);
        hela.setPhase(1);
        boss = hela;
        spawn(hela);
        murals(true);
        HelaFx.send(level, HelaFx.ARRIVE, hela.getId(), List.of(seal, dais), HelaState.ARRIVING.ticks);
        level.playSound(null, dais.x, dais.y, dais.z, ModSounds.SEAL_BREAK.get(), SoundSource.HOSTILE, 3.0F, 0.8F);
        level.playSound(null, dais.x, dais.y, dais.z, ModSounds.HELA_ARRIVE.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(arena().center()) <= 160.0 * 160.0) {
                title(player, Component.translatable("boss.enhanced.hela.title").withColor(SPEECH_COLOR).withStyle(ChatFormatting.BOLD),
                        Component.translatable("boss.enhanced.hela.subtitle").withStyle(ChatFormatting.GRAY));
            }
        }
        refreshBars();
    }

    static float yawToward(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }

    static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** The murals of the throne room show her conquests while she is here (the realm's own blocks; only in Asgard). */
    private void murals(boolean conquest) {
        ServerLevel asgard = AsgardDimension.level(server());
        if (asgard != null) {
            CityBuild.setMurals(asgard, conquest);
        }
        if (!conquest) {
            muralsBack = true;
        }
    }

    /** She speaks one of her lines ({@code boss.enhanced.hela.line.<key>}), with the breath of her voice. */
    public void say(String key) {
        speak(NAME, SPEECH_COLOR, Component.translatable("boss.enhanced.hela.line." + key), ModSounds.HELA_VOICE.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick

    @Override
    protected void onTick() {
        HelaEntity hela = boss;
        if (hela == null) {
            return;
        }
        if (hela.isRemoved() || !hela.isAlive()) {
            end(BossEnd.ABORT); // /kill or a command: no winner
            return;
        }
        ServerLevel level = level();
        long now = level.getGameTime();
        foes = computeFoes(hela);
        if (age() <= SCALE_TICKS && fighterCount() != scaledFor) {
            scaledFor = fighterCount();
            hela.applyBody(scaledFor);
        }
        HelaState state = hela.state();
        int t = hela.stateTicks();
        switch (state) {
            case ARRIVING -> {
                if (t == SPEAK_AT) {
                    say("arrive");
                }
                if (t >= HelaState.ARRIVING.ticks) {
                    hela.setState(HelaState.IDLE);
                    hela.brain().reset(now);
                }
            }
            case RISE -> tickRise(hela, t);
            case CHANNEL -> tickChannel(hela, t);
            default -> {
            }
        }
        if (isOver()) {
            return;
        }
        checkPhase(hela, now);
        if (phase >= 2) {
            tickArmy(hela, now);
            tryCatch(hela, now);
        }
        if (phase >= 3 && now >= nextChannelAt && hela.state() == HelaState.IDLE) {
            startChannel(hela);
        }
        LivingEntity target = chooseTarget(hela, now);
        hela.setTarget(target);
        HelaState current = hela.state();
        if (current != HelaState.ARRIVING && current != HelaState.RISE && current != HelaState.CHANNEL && !BossGrip.stills(hela)) {
            hela.brain().tick(this, target);
        }
        keepHome(hela);
        if (now % COMMAND_EVERY == 0) {
            callAsgardians(hela);
        }
        refreshBars();
    }

    private List<ServerPlayer> computeFoes(HelaEntity hela) {
        List<ServerPlayer> list = new ArrayList<>();
        double reach = arena().containRadius() + 2.0;
        for (ServerPlayer player : present()) {
            if (!player.isCreative() && !player.isSpectator() && arena().distance(player.position()) <= reach) {
                list.add(player);
            }
        }
        list.sort(Comparator.comparingDouble(p -> p.distanceToSqr(hela)));
        return list;
    }

    /** The fighter who struck her last (if in the last ten seconds and still a foe), else the nearest. */
    @Nullable
    private LivingEntity chooseTarget(HelaEntity hela, long now) {
        if (lastAttacker != null && now - lastAttackAt <= 200) {
            for (ServerPlayer player : foes) {
                if (player.getUUID().equals(lastAttacker)) {
                    return player;
                }
            }
        }
        return foes.isEmpty() ? null : foes.get(0);
    }

    // ---- the phases ----

    private void checkPhase(HelaEntity hela, long now) {
        float fraction = hela.fraction();
        if (phase == 1 && fraction <= HelaBossConfig.PHASE2_AT.get()) {
            enterPhase(2, now);
        } else if (phase == 2 && fraction <= HelaBossConfig.PHASE3_AT.get() && hela.state() != HelaState.RISE) {
            enterPhase(3, now);
        }
    }

    /** The fight goes into {@code next} (the tests call it to begin a phase at once). */
    public void enterPhase(int next, long now) {
        HelaEntity hela = boss;
        if (hela == null || next <= phase) {
            return;
        }
        phase = next;
        hela.setPhase(next);
        hela.brain().interrupt();
        if (next == 2) {
            hela.getNavigation().stop();
            hela.setState(HelaState.RISE);
            say("army");
            tell(Component.translatable("boss.enhanced.hela.phase2").withColor(SPEECH_COLOR), true);
        } else if (next == 3) {
            if (hela.state() == HelaState.RISE) {
                hela.setState(HelaState.IDLE);
            }
            say("strength");
            tell(Component.translatable("boss.enhanced.hela.phase3").withColor(SPEECH_COLOR), true);
            hela.brain().stormSoon(now);
            nextChannelAt = now;
        }
    }

    /** The rise of the second phase: the dead climb out of the floor at its first second, Fenris with them. */
    private void tickRise(HelaEntity hela, int t) {
        if (t == 20) {
            raiseWave(hela, waveSize());
            if (!fenrisCalled) {
                callFenris(hela);
            }
        }
        if (t >= HelaState.RISE.ticks) {
            hela.setState(HelaState.IDLE);
            hela.brain().reset(level().getGameTime());
        }
    }

    // ---- the army ----

    private void tickArmy(HelaEntity hela, long now) {
        if (fenris != null && (fenris.isRemoved() || !fenris.isAlive())) {
            fenris = null;
            bars().showSecond(false);
        }
        if (phase != 2 || hela.state() == HelaState.RISE || now % 20 != 0) {
            return;
        }
        int alive = berserkers().size();
        if (alive <= 2) {
            raiseWave(hela, waveSize());
        }
    }

    /** Raises a wave: as many berserkers as {@code count}, but never more than {@code berserkerMax} standing. */
    public int raiseWave(HelaEntity hela, int count) {
        ServerLevel level = level();
        int room = HelaBossConfig.BERSERKER_MAX.get() - berserkers().size();
        int n = Math.min(count, room);
        for (int i = 0; i < n; i++) {
            Vec3 at = riseSpot(hela, i, n);
            BerserkerEntity berserker = new BerserkerEntity(ModEntities.BERSERKER.get(), level);
            berserker.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
            berserker.beginRise();
            spawn(berserker);
            HelaFx.send(level, HelaFx.RISE, berserker.getId(), at, 0);
        }
        if (n > 0) {
            level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.BERSERKER_RISE.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        return Math.max(0, n);
    }

    /** Where the {@code i}-th of {@code n} rises: on a ring round her, 4 to 9 blocks out, kept in the arena, on the floor. */
    private Vec3 riseSpot(HelaEntity hela, int i, int n) {
        double angle = Math.PI * 2.0 * i / Math.max(1, n) + level().random.nextDouble() * 0.5;
        double r = 4.0 + level().random.nextDouble() * 5.0;
        Vec3 at = hela.position().add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
        Vec3 c = arena().center();
        double reach = arena().containRadius() - 3.0;
        double d = Math.hypot(at.x - c.x, at.z - c.z);
        if (d > reach) {
            at = new Vec3(c.x + (at.x - c.x) / d * reach, at.y, c.z + (at.z - c.z) / d * reach);
        }
        return HelaRules.onGround(level(), at.add(0.0, 1.5, 0.0));
    }

    /** Fenris comes (once a fight), with a bar of his own. */
    public FenrisEntity callFenris(HelaEntity hela) {
        ServerLevel level = level();
        fenrisCalled = true;
        Vec3 at = riseSpot(hela, 0, 1);
        FenrisEntity wolf = new FenrisEntity(ModEntities.FENRIS.get(), level);
        wolf.moveTo(at.x, at.y, at.z, hela.getYRot(), 0.0F);
        wolf.setBody(HelaBossConfig.FENRIS_HEALTH.get());
        wolf.beginRise();
        spawn(wolf);
        fenris = wolf;
        bars().showSecond(true);
        HelaFx.send(level, HelaFx.FENRIS, wolf.getId(), at, 0);
        level.playSound(null, at.x, at.y, at.z, ModSounds.FENRIS_HOWL.get(), SoundSource.HOSTILE, 3.0F, 0.9F);
        return wolf;
    }

    /** The warriors of Asgard near the arena go for her army (or for her when no army is near): L5's command. */
    private void callAsgardians(HelaEntity hela) {
        if (foes.isEmpty() || !AsgardDimension.is(level())) {
            return;
        }
        ServerPlayer leader = foes.get(0);
        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        for (Entity e : entities()) {
            if (e instanceof ArmyMob mob && mob.isAlive() && !mob.rising()) {
                double d = mob.distanceToSqr(leader);
                if (d < best) {
                    best = d;
                    target = mob;
                }
            }
        }
        Asgardians.command(leader, target != null ? target : hela, COMMAND_EVERY + 20);
    }

    // ---- the Strength of Asgard ----

    /** She steps through smoke onto the dais and kneels to channel. */
    public void startChannel(HelaEntity hela) {
        ServerLevel level = level();
        Vec3 from = hela.position();
        hela.brain().interrupt();
        hela.getNavigation().stop();
        if (from.distanceToSqr(dais) > 4.0) {
            hela.moveTo(dais.x, dais.y, dais.z, yawToward(dais, arena().center()), 0.0F);
            HelaFx.send(level, HelaFx.SHADOW_STEP, hela.getId(), List.of(from, dais), 0);
        }
        hela.setState(HelaState.CHANNEL);
        int ticks = HelaBossConfig.CHANNEL_SECONDS.get() * 20;
        HelaFx.send(level, HelaFx.CHANNEL, hela.getId(), hela.position(), ticks);
        level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.HELA_CHANNEL.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
        say("channel");
    }

    private void tickChannel(HelaEntity hela, int t) {
        ServerLevel level = level();
        long now = level.getGameTime();
        int ticks = HelaBossConfig.CHANNEL_SECONDS.get() * 20;
        ServerBossEvent bar = bars().health();
        bar.setColor(t % 10 < 5 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
        float needed = (float) (HelaBossConfig.CHANNEL_BREAK_FRACTION.get() * hela.getMaxHealth());
        if (hela.channelDamage() >= needed) {
            // Broken: she reels, and takes a quarter more while she does.
            hela.setState(HelaState.STUNNED);
            HelaFx.send(level, HelaFx.CHANNEL_BREAK, hela.getId(), hela.position().add(0.0, 1.4, 0.0), 0);
            HelaFx.send(level, HelaFx.CHANNEL, hela.getId(), hela.position(), 0);
            stopChannelSound(level, hela);
            level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.HELA_CHANNEL_BREAK.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
            tell(Component.translatable("boss.enhanced.hela.channel_broken").withColor(SPEECH_COLOR), true);
            nextChannelAt = now + HelaBossConfig.CHANNEL_EVERY.get() * 20L;
            bar.setColor(BossEvent.BossBarColor.GREEN);
            return;
        }
        if (t >= ticks) {
            hela.heal((float) (HelaBossConfig.CHANNEL_HEAL.get() * hela.getMaxHealth()));
            HelaFx.send(level, HelaFx.CHANNEL, hela.getId(), hela.position(), 0);
            tell(Component.translatable("boss.enhanced.hela.channel_healed").withColor(SPEECH_COLOR), true);
            hela.setState(HelaState.IDLE);
            hela.brain().reset(now);
            nextChannelAt = now + HelaBossConfig.CHANNEL_EVERY.get() * 20L;
            bar.setColor(BossEvent.BossBarColor.GREEN);
        }
    }

    /** The channel's sound never resolves by itself (L6): it is stopped for everyone near when the channel breaks. */
    private static void stopChannelSound(ServerLevel level, HelaEntity hela) {
        net.minecraft.network.protocol.game.ClientboundStopSoundPacket stop =
                new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(ModSounds.HELA_CHANNEL.getId(), SoundSource.HOSTILE);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(hela) <= 96.0 * 96.0) {
                player.connection.send(stop);
            }
        }
    }

    // ---- Mjolnir ----

    /** A Mjolnir thrown near her hand is caught and crushed (once a minute, from the second phase). */
    private void tryCatch(HelaEntity hela, long now) {
        if (!HelaBossConfig.CATCH_MJOLNIR.get() || now < nextCatchAt || hela.state().untouchable || hela.state() == HelaState.CHANNEL) {
            return;
        }
        Vec3 hand = hela.position().add(0.0, hela.getBbHeight() * 0.7, 0.0);
        if (HelaCatch.catchNear(level(), hela, hand, CATCH_REACH)) {
            nextCatchAt = now + CATCH_COOLDOWN;
            hela.brain().interrupt();
            hela.setState(HelaState.CATCH);
            say("catch");
        }
    }

    /** For the tests: her arrival is over at once (she stands on the dais, ready, choosing nothing until told). */
    @VisibleForTesting
    public void skipArrival() {
        if (boss != null) {
            boss.setState(HelaState.IDLE);
            boss.brain().reset(level().getGameTime());
            boss.brain().setPassive(true);
        }
    }

    /** For the tests: the next Mjolnir may be caught at once. */
    @VisibleForTesting
    public void readyCatch() {
        nextCatchAt = 0L;
    }

    // ---- staying in her hall ----

    /** She never leaves the arena: past its wall she is back on the dais. */
    private void keepHome(HelaEntity hela) {
        double d = arena().distance(hela.position());
        if (d > arena().containRadius() + 2.0 || hela.getY() < arena().center().y - 8.0) {
            BossGrip.forget(hela);
            Vec3 from = hela.position();
            hela.moveTo(dais.x, dais.y, dais.z, hela.getYRot(), 0.0F);
            hela.setDeltaMovement(Vec3.ZERO);
            hela.getNavigation().stop();
            HelaFx.send(level(), HelaFx.SHADOW_STEP, hela.getId(), List.of(from, dais), 0);
        }
    }

    @Override
    protected void bringBack(Entity entity) {
        if (entity == boss) {
            keepHome(boss);
        }
    }

    // ---- the bars ----

    private void refreshBars() {
        HelaEntity hela = boss;
        if (hela == null) {
            return;
        }
        ServerBossEvent bar = bars().health();
        bar.setProgress(hela.fraction());
        String key = phase == 1 ? "boss.enhanced.hela" : phase == 2 ? "boss.enhanced.hela.phase2" : "boss.enhanced.hela.phase3";
        if (!(bar.getName().getContents() instanceof TranslatableContents name) || !name.getKey().equals(key)) {
            bar.setName(Component.translatable(key));
        }
        if (fenris != null && fenris.isAlive()) {
            bars().second().setProgress(Math.max(0.0F, Math.min(1.0F, fenris.getHealth() / fenris.getMaxHealth())));
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The end

    /** A blow that would have killed her: she falls, and the fight is won. */
    void bossFell(@Nullable ServerPlayer lastBlow) {
        if (lastBlow != null) {
            lastBlow(lastBlow);
        }
        end(BossEnd.VICTORY);
    }

    @Override
    public int holdTicks() {
        return 40;
    }

    /** Two percent of her health: what earns the reward together with the time in the fight. */
    @Override
    protected float rewardMinDamage() {
        return boss == null ? Float.MIN_VALUE : (float) (boss.getMaxHealth() * REWARD_SHARE);
    }

    /** Hela's Headdress, and the right to Gungnir. */
    @Override
    protected List<ItemStack> rewards(Participant participant) {
        ServerPlayer player = server().getPlayerList().getPlayer(participant.id());
        if (player != null) {
            makeVictor(player);
            if (player.isAlive()) {
                title(player, Component.translatable("boss.enhanced.hela.reward").withColor(SPEECH_COLOR).withStyle(ChatFormatting.BOLD),
                        Component.translatable("item.enhanced.hela_headdress").withStyle(ChatFormatting.GRAY));
            }
        }
        return List.of(headdressFor(participant.id()));
    }

    /** A headdress of the reward: it remembers whom it was given to (an offline winner gets his right when it reaches him). */
    public static ItemStack headdressFor(UUID winner) {
        ItemStack stack = new ItemStack(ModItems.HELA_HEADDRESS.get());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putUUID(VICTOR_TAG, winner));
        return stack;
    }

    /** Gives a player the right to Gungnir ({@code HELA_VICTOR}). */
    public static void makeVictor(ServerPlayer player) {
        AsgardProgress progress = player.getData(ModAttachments.ASGARD);
        if (!progress.has(AsgardProgress.HELA_VICTOR)) {
            player.setData(ModAttachments.ASGARD, progress.with(AsgardProgress.HELA_VICTOR));
        }
    }

    /** The seal rests a whole {@code restTicks} after her defeat, half after she leaves for want of fighters. */
    @Override
    protected long restTicks(BossEnd why) {
        long rest = HelaBossConfig.REST_TICKS.get();
        return switch (why) {
            case VICTORY -> rest;
            case WIPE, ABANDON -> rest / 2;
            default -> 0L;
        };
    }

    @Override
    protected void onEnd(BossEnd why) {
        ended = true;
        ServerLevel level = level();
        long now = level.getGameTime();
        for (Entity e : entities()) {
            if (e instanceof ArmyMob mob) {
                mob.crumble(); // the dead go back to dust
            }
        }
        bars().showSecond(false);
        murals(false);
        AsgardState state = AsgardState.get(server());
        long rest = restTicks(why);
        if (rest > 0) {
            state.setSealReadyAt(now + rest);
        }
        HelaEntity hela = boss;
        if (why == BossEnd.VICTORY) {
            if (hela != null && !hela.isRemoved()) {
                hela.getNavigation().stop();
                hela.setState(HelaState.DYING);
                HelaFx.send(level, HelaFx.DISSOLVE, hela.getId(), hela.position(), HelaState.DYING.ticks);
                level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.HELA_DEFEAT.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
            }
            say("defeat");
            boolean first = state.helaDefeats() == 0 || !state.odinAwake();
            state.setHelaDefeats(state.helaDefeats() + 1);
            if (first) {
                wakeOdin();
            }
            return;
        }
        if (hela != null && !hela.isRemoved()) {
            hela.getNavigation().stop();
            hela.setInvisible(false);
            hela.setState(HelaState.LEAVING);
            HelaFx.send(level, HelaFx.DISSOLVE, hela.getId(), hela.position(), HelaState.LEAVING.ticks);
            if (why != BossEnd.SERVER_STOPPING) {
                say("wait");
            }
        }
    }

    /**
     * Her first defeat: Odin wakes from the Odinsleep (a title to everyone in Asgard) and the vault under the throne room
     * opens ({@link Vault#open}, lane L3's).
     */
    private void wakeOdin() {
        AsgardState.get(server()).setOdinAwake(true);
        ServerLevel asgard = AsgardDimension.level(server());
        if (asgard == null) {
            return;
        }
        for (ServerPlayer player : asgard.players()) {
            title(player, Component.translatable("boss.enhanced.hela.odin_wakes").withColor(0xFFE7A3).withStyle(ChatFormatting.BOLD),
                    Component.translatable("boss.enhanced.hela.odin_wakes.sub").withStyle(ChatFormatting.GRAY));
        }
        try {
            Vault.open(asgard);
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("The vault of Asgard failed to open", e);
        }
    }

    @Override
    protected boolean windDown(int ticks) {
        HelaEntity hela = boss;
        if (hela == null || hela.isRemoved()) {
            return true;
        }
        int length = result() == BossEnd.VICTORY ? HelaState.DYING.ticks : HelaState.LEAVING.ticks;
        if (ticks >= length) {
            hela.discard();
            return true;
        }
        return false;
    }

    @Override
    protected void onFinished(boolean forced) {
        if (boss != null && !boss.isRemoved()) {
            boss.discard();
        }
        if (!muralsBack) {
            murals(false);
        }
    }

    /** Whether the fight has run its end (the tests). */
    public boolean ended() {
        return ended;
    }
}
