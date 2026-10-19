package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.effect.ConfusedEffect;
import com.kolodkingal.enhanced.item.LokisScepterItem;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Mind Stone (3.6). Use (held), the <b>Mind Beam</b>: Vision's beam from the stone's hand, on the shared {@link
 * ContinuousBeam} (pale gold, {@code MIND_STONE_BEAM}, two hits a second), so anyone with the stone fires it without
 * being Vision; every hit also confuses ({@link LokisScepterItem#confuse}: a player Nausea and Slowness, a creature the
 * confused effect; {@code confused_immune} and bosses shrug it off), at most once per target every {@code
 * confuseEvery} ticks. Six seconds at most, then a cooldown (a shorter one after letting go). Loki's Scepter fires it
 * for anyone with Sneak and an empty other hand. Sneak + use, the <b>Mind Command</b> (the stone bare, in a Gauntlet or
 * on a row: the Scepter's Sneak is the beam): the creature he looks at becomes his ally for a while, follows him and
 * attacks what he attacks or what attacks him; a player (PvP on) is only confused, hard. Server only.
 */
public final class MindStonePowers {
    /** Ticks a hit of the beam waits for the next (two hits a second, Vision's). */
    static final int BEAM_INTERVAL = 10;
    /** The cooldown after a beam let go of before its limit. */
    static final int RELEASE_COOLDOWN = 20;
    /** Hunger of one Mind Command. */
    static final float COMMAND_EXHAUSTION = 1.5F;
    /** How long a player the Mind Command reaches is muddled (ticks). */
    static final int PLAYER_MUDDLE_TICKS = 120;
    /** How far an ally keeps from his commander before he follows him. */
    static final double FOLLOW_DISTANCE = 6.0;

    /** A creature under the Mind Command: whose, and until when. */
    private record Command(UUID commander, long until) {
    }

    /** Creature -> its command. */
    private static final Map<UUID, Command> COMMANDED = new HashMap<>();
    /** Target -> the game time the Mind Stone last confused it. */
    private static final Map<UUID, Long> CONFUSED = new HashMap<>();

    // ---- the beam ----

    /** The beam's numbers for one tick. */
    static BeamSpec spec() {
        return new BeamSpec(StoneBeams.MIND, StonesConfig.MIND_BEAM_RANGE.get(),
                StonesConfig.MIND_BEAM_DAMAGE.get().floatValue(), BEAM_INTERVAL,
                StonesConfig.MIND_EXHAUSTION_PER_TICK.get().floatValue(), ModDamageTypes.MIND_STONE_BEAM,
                MindStonePowers::hit);
    }

    static void beamStart(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        if (ContinuousBeam.isFiring(player)) {
            return; // (another beam of his burns: one at a time)
        }
        StoneUse.begin(cast);
        ContinuousBeam.start(player, StoneBeams.MIND, StonesConfig.MIND_BEAM_RANGE.get());
        ServerLevel level = player.serverLevel();
        StoneKind.MIND.voice().use(level, player.position(), 0.8F, 1.0F);
    }

    static boolean beamTick(StoneUse.Hold hold) {
        ServerPlayer player = hold.cast.player();
        if (hold.ticks > StonesConfig.MIND_BEAM_MAX_TICKS.get() || !ContinuousBeam.isFiring(player)) {
            return false;
        }
        ContinuousBeam.tick(player, spec());
        return true;
    }

    static void beamEnd(StoneUse.Hold hold, boolean released) {
        ServerPlayer player = hold.cast.player();
        ContinuousBeam.stop(player);
        boolean full = hold.ticks > StonesConfig.MIND_BEAM_MAX_TICKS.get();
        hold.cast.startCooldown(released && !full ? RELEASE_COOLDOWN : StonesConfig.MIND_BEAM_COOLDOWN.get());
    }

    /** A hit of the beam: it confuses, once every {@code confuseEvery} ticks per target. */
    private static void hit(ServerPlayer shooter, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive() || boss(living)) {
            return;
        }
        long now = shooter.level().getGameTime();
        Long last = CONFUSED.get(living.getUUID());
        if (last != null && now - last < StonesConfig.MIND_CONFUSE_EVERY.get() && now >= last) {
            return;
        }
        CONFUSED.put(living.getUUID(), now);
        LokisScepterItem.confuse(living, shooter);
        glowEyes(shooter.serverLevel(), living);
    }

    /** A boss, or a creature no mind trick reaches. */
    static boolean boss(LivingEntity target) {
        return BossFights.isFightEntity(target) || target.getType().is(ModTags.BOSSES)
                || target.getType().is(ModTags.CONFUSED_IMMUNE);
    }

    /** The Scepter's tell: the eyes of whom the stone touched glow blue a moment. */
    private static void glowEyes(ServerLevel level, LivingEntity target) {
        level.sendParticles(new EnergyParticleOptions(0x6FC8FF, 0.8F), target.getX(), target.getEyeY(), target.getZ(),
                4, 0.12, 0.05, 0.12, 0.01);
    }

    // ---- the command ----

    /** The creature he looks at is his ally for a while; a player is muddled instead. */
    static void command(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = player.serverLevel();
        StoneAim.Aim aim = StoneAim.aim(player, StonesConfig.MIND_COMMAND_RANGE.get(), 0.3,
                target -> !(target instanceof ArmorStand));
        LivingEntity target = aim.creature();
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_target"), true);
            return;
        }
        if (target instanceof Player other) {
            if (StoneAim.pvpRefuses(player, other) || other.isCreative() || other.isSpectator()) {
                return;
            }
            cast.startCooldown(StonesConfig.MIND_COMMAND_COOLDOWN.get());
            player.causeFoodExhaustion(COMMAND_EXHAUSTION);
            other.addEffect(new MobEffectInstance(MobEffects.CONFUSION, PLAYER_MUDDLE_TICKS, 0), player);
            other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PLAYER_MUDDLE_TICKS, 1), player);
            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PLAYER_MUDDLE_TICKS, 0), player);
            ConfusedEffect.burst(level, other);
            sound(level, player);
            return;
        }
        if (!(target instanceof Mob mob) || boss(target)) {
            player.displayClientMessage(Component.translatable("message.enhanced.stones.mind.resists"), true);
            return;
        }
        cast.startCooldown(StonesConfig.MIND_COMMAND_COOLDOWN.get());
        player.causeFoodExhaustion(COMMAND_EXHAUSTION);
        COMMANDED.put(mob.getUUID(), new Command(player.getUUID(),
                level.getGameTime() + StonesConfig.MIND_COMMAND_TICKS.get()));
        mob.setTarget(null);
        mob.getNavigation().stop();
        ConfusedEffect.burst(level, mob);
        glowEyes(level, mob);
        sound(level, player);
        player.displayClientMessage(Component.translatable("message.enhanced.stones.mind.ally", mob.getName()), true);
    }

    private static void sound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MIND_COMMAND.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StoneKind.MIND.voice().use(level, player.position(), 0.6F, 1.2F);
    }

    /** The player under whose Mind Command this creature is, or null. */
    @Nullable
    public static UUID commanderOf(Entity entity) {
        Command command = COMMANDED.get(entity.getUUID());
        return command == null ? null : command.commander();
    }

    /** Whether {@code target} is under {@code owner}'s Mind Command (his area attacks spare it). */
    public static boolean isCommandedBy(LivingEntity owner, LivingEntity target) {
        Command command = COMMANDED.get(target.getUUID());
        return command != null && command.commander().equals(owner.getUUID());
    }

    /** Whether a commanded creature may take {@code target} as its target (never its commander nor his allies). */
    public static boolean mayTarget(Entity mob, @Nullable LivingEntity target) {
        Command command = COMMANDED.get(mob.getUUID());
        if (command == null || target == null) {
            return true;
        }
        if (target.getUUID().equals(command.commander())) {
            return false;
        }
        Command other = COMMANDED.get(target.getUUID());
        return other == null || !other.commander().equals(command.commander());
    }

    /** The commander's allies follow him and fight his fights; a command that ran out is over. */
    static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (!CONFUSED.isEmpty() && player.tickCount % 200 == 0) {
            CONFUSED.values().removeIf(t -> now - t > 400 || now < t);
        }
        if (COMMANDED.isEmpty() || player.tickCount % 5 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        LivingEntity enemy = enemyOf(player);
        for (Iterator<Map.Entry<UUID, Command>> it = COMMANDED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Command> entry = it.next();
            if (!entry.getValue().commander().equals(player.getUUID())) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || now >= entry.getValue().until()) {
                if (entity instanceof Mob mob && mob.getTarget() != null && !mayTarget(mob, mob.getTarget())) {
                    mob.setTarget(null);
                }
                it.remove();
                continue;
            }
            if (!mayTarget(mob, mob.getTarget())) {
                mob.setTarget(null);
            }
            if (enemy != null && enemy != mob && mob.getTarget() != enemy && mayTarget(mob, enemy)) {
                mob.setTarget(enemy);
            } else if (mob.getTarget() == null && mob.distanceToSqr(player) > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
                mob.getNavigation().moveTo(player, 1.1);
            }
            if (player.tickCount % 20 == 0) {
                glowEyes(level, mob);
            }
        }
    }

    /** What the commander fights: whom he hit lately, else who hit him (alive, near). */
    @Nullable
    private static LivingEntity enemyOf(ServerPlayer player) {
        for (LivingEntity candidate : new LivingEntity[]{player.getLastHurtMob(), player.getLastHurtByMob()}) {
            if (candidate != null && candidate.isAlive() && candidate.level() == player.level()
                    && candidate.distanceToSqr(player) < 24 * 24 && !isCommandedBy(player, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static void forget(Player player) {
        COMMANDED.values().removeIf(c -> c.commander().equals(player.getUUID()));
    }

    static void clear() {
        COMMANDED.clear();
        CONFUSED.clear();
    }

    private MindStonePowers() {
    }
}
