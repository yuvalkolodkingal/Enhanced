package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CustomBeams;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.HexBoltPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Billy Maximoff (Wiccan): Wanda's son, a witch of the Witches' Road. His magic is blue with a bright white core,
 * crackling and branching like Tesla-coil lightning (Agatha All Along). While he flies, and for a moment after each
 * spell, his crown forms on his head.
 */
public final class BillyAbilities {
    // ---- the colours of his magic, from the white-hot core out to the fringe ----
    public static final int CORE = 0xFFFFFF;
    public static final int INNER = 0xCFEFFF;
    public static final int MAIN = 0x5CC8FF;
    public static final int BODY = 0x2F8CFF;
    public static final int FRINGE = 0x1B4FCC;

    public static final double HEX_BOLT_RANGE = 24.0;
    /** A Hex Bolt forks to creatures this close to the one it struck. */
    public static final double FORK_RANGE = 5.0;
    public static final int FORK_TARGETS = 2;
    public static final int HEX_BOLT_COOLDOWN = 10;
    public static final double FREEZE_RANGE = 12.0;
    /** Half the angle of the cone Freeze reaches (90 degrees in all). */
    public static final double FREEZE_HALF_ANGLE = 45.0;
    public static final int FREEZE_COOLDOWN = 240;
    /** How long the crown stays out after a spell (3 s). */
    public static final int CROWN_TICKS = 60;
    /** How long the casting poses last. */
    private static final int BOLT_POSE_TICKS = 6;
    private static final int FREEZE_POSE_TICKS = 16;

    /** When each of Billy's momentary states ends (game time), per Billy. */
    private static final class Spells {
        long crown;
        long bolt;
        long freeze;
        /** When he may tear the next block out of the ground ({@code billy.blockLiftCooldown} after the last one). */
        long blockReady;
    }

    private static final Map<UUID, Spells> SPELLS = new HashMap<>();

    // ---- input ----

    /** The key of old, which reads Sneak from the player (Billy himself: {@code AbilityHandler}). */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his (3.4: also one of a custom hero's bindings, {@code HeroKeys.press}, with the key the ability has for him).
     * {@code alt}: its Sneak variant (Sneak + B is the Hex Barrier).
     */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (ability == 4 && BillyBarrier.enabled() && ((data.chaosShieldActive() && BillyBarrier.owns(data)) || (down && alt))) {
            // The Hex Barrier is held (Sneak + B): letting go must always get through, even for a Billy who just became a spectator.
            BillyBarrier.key(player, data, down);
            return;
        }
        if (ability == 1 && beamEnabled()) {
            // The Hex Beam is held: letting go must always get through, even for a Billy who just became a spectator.
            hexBeamKey(player, data, down);
            return;
        }
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> hexBolt(player, data);
            case 2 -> PowerTicker.toggleFlight(player);
            case 3 -> Telekinesis.press(player, Telekinesis.Mode.BILLY);
            case 4 -> freeze(player, data);
            case 5 -> theNoise(player, data);
            default -> {
                return;
            }
        }
        spells(player).crown = player.level().getGameTime() + CROWN_TICKS;
    }

    private static Spells spells(Player player) {
        return SPELLS.computeIfAbsent(player.getUUID(), id -> new Spells());
    }

    /**
     * Keeps the crown and the casting poses in sync with what he is doing. The crown stays on while he builds. His record
     * is forgotten only once every time in it is over, the recharge of his block lifting included (it may be longer than
     * the crown, R25).
     */
    public static void tick(ServerPlayer player, PowerData data) {
        Spells spells = SPELLS.get(player.getUUID());
        long now = player.level().getGameTime();
        boolean flying = player.getAbilities().flying;
        boolean building = player.getExistingData(ModAttachments.BUILD_VIEW).map(BuildView::active).orElse(false);
        data.set(Live.CROWNED, flying || building || (spells != null && now < spells.crown));
        // (3.4: a custom hero's beam is his Hex Beam only if he has it: the one continuous beam may be another hero's.)
        if ((data.is(Live.BEAMING) || ContinuousBeam.isFiring(player)) && !DarkholdBook.firing(player) && data.has(HeroAbility.BILLY_HEX_BEAM)) {
            hexBeamTick(player, data);
            if (spells != null && data.is(Live.BEAMING)) {
                spells.crown = Math.max(spells.crown, now + CROWN_TICKS);
            }
        }
        if (BillyBarrier.owns(data)) {
            BillyBarrier.tick(player, data);
        }
        data.set(Live.CASTING, spells != null && now < spells.bolt);
        data.set(Live.FREEZING, spells != null && now < spells.freeze);
        if (spells != null && now >= Math.max(Math.max(spells.crown, spells.blockReady), Math.max(spells.bolt, spells.freeze))) {
            SPELLS.remove(player.getUUID());
        }
    }

    // ---- Ability 3: Telekinesis (the blocks he tears out of the ground) ----

    /**
     * Whether Billy's magic is ready to tear another block out of the ground: {@code billy.blockLiftCooldown} ticks after
     * the last one. Catching anything else never waits.
     */
    public static boolean blockLiftReady(ServerPlayer player) {
        Spells spells = SPELLS.get(player.getUUID());
        return spells == null || player.level().getGameTime() >= spells.blockReady;
    }

    /** He just tore a block out of the ground: the next one waits for {@code billy.blockLiftCooldown}. */
    public static void markBlockLift(ServerPlayer player) {
        spells(player).blockReady = player.level().getGameTime() + EnhancedConfig.BILLY_BLOCK_LIFT_COOLDOWN.get();
    }

    // ---- Ability 1: the Hex Beam (3.2: R held) ----

    /** Whether R is the continuous Hex Beam ({@code billy.beam}) and not the single Hex Bolt. (Before the config loads: the beam.) */
    public static boolean beamEnabled() {
        return !EnhancedConfig.SPEC.isLoaded() || EnhancedConfig.BILLY_BEAM.get();
    }

    /**
     * How far along his ladder this Billy is, from 0 (his first rank) to 1 (the highest, {@code build_magic_billy.maxRank}):
     * {@code (rank - 1) / (maxRank - 1)}, read through {@link BillyRanks#rank} and {@link BillyRanks#maxRank} alone. The
     * beam's damage and range grow with it.
     */
    public static double beamProgress(BuildData build) {
        int max = BillyRanks.maxRank();
        return max <= 1 ? 0.0 : Math.max(0.0, Math.min(1.0, (BillyRanks.rank(build) - 1) / (double) (max - 1)));
    }

    /** Damage a second of his beam does at this rank. */
    public static float beamDamagePerSecond(BuildData build) {
        return (float) (EnhancedConfig.BILLY_BEAM_DAMAGE_PER_SECOND.get() * (1.0 + EnhancedConfig.BILLY_BEAM_RANK_DAMAGE_BONUS.get() * beamProgress(build)));
    }

    /** How far his beam reaches at this rank, in blocks. */
    public static double beamRange(BuildData build) {
        return EnhancedConfig.BILLY_BEAM_RANGE.get() * (1.0 + EnhancedConfig.BILLY_BEAM_RANK_RANGE_BONUS.get() * beamProgress(build));
    }

    /** The numbers of his beam for one tick, for his rank now. */
    public static BeamSpec beamSpec(ServerPlayer player) {
        BuildData build = player.getData(ModAttachments.BUILD_DATA);
        int interval = EnhancedConfig.BILLY_BEAM_INTERVAL.get();
        float perHit = beamDamagePerSecond(build) * interval / 20.0F;
        float exhaustion = EnhancedConfig.BILLY_BEAM_EXHAUSTION_PER_SECOND.get().floatValue() / 20.0F;
        return new BeamSpec(beamStyle(player), beamRange(build), perHit, interval, exhaustion, ModDamageTypes.HEX_BEAM, null);
    }

    /**
     * The style of his beam: Billy's blue, or (3.5) a custom hero's Hex Beam in the colour his pick chose (the shared beam
     * system makes a style of any two colours, {@link CustomBeams}).
     */
    public static BeamStyle beamStyle(Player player) {
        int chosen = AbilityColors.chosen(player, HeroAbility.BILLY_HEX_BEAM);
        return chosen < 0 ? BeamStyles.BILLY : CustomBeams.style(chosen, AbilityColors.glowOf(chosen));
    }

    /** With an empty hunger bar there is nothing left to fuel the beam (not in Creative). */
    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    /** R down: the beam begins (if he is fed and not a spectator); R up: it stops. */
    private static void hexBeamKey(ServerPlayer player, PowerData data, boolean down) {
        if (DarkholdBook.firing(player)) {
            return; // (the beam that burns is the Darkhold's: R neither starts nor stops it)
        }
        if (!down || player.isSpectator()) {
            // (a Darkhold beam he fires is not his to stop: R lets go of the Hex Beam only)
            if ((ContinuousBeam.isFiring(player) || data.is(Live.BEAMING)) && !DarkholdBook.firing(player)) {
                ContinuousBeam.stop(player);
            }
            return;
        }
        if (ContinuousBeam.isFiring(player)) {
            return;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.billy_beam.hungry"), true);
            return;
        }
        BuildData build = player.getData(ModAttachments.BUILD_DATA);
        ContinuousBeam.start(player, beamStyle(player), beamRange(build));
        spells(player).crown = player.level().getGameTime() + CROWN_TICKS;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEX_BOLT.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    /** One tick of a held beam: it ends when he can no longer feed it, otherwise it burns. */
    private static void hexBeamTick(ServerPlayer player, PowerData data) {
        if (ContinuousBeam.isFiring(player) && !data.is(Live.BEAMING)) {
            ContinuousBeam.stop(player); // (something that lets go of every held ability took the flag away)
            return;
        }
        if (player.isSpectator() || starving(player) || !beamEnabled()) {
            if (starving(player)) {
                player.displayClientMessage(Component.translatable("message.enhanced.billy_beam.hungry"), true);
            }
            ContinuousBeam.stop(player);
            return;
        }
        if (!ContinuousBeam.isFiring(player)) {
            data.set(Live.BEAMING, false); // (the flag outlived the beam: a server that restarted, a state cleared)
            return;
        }
        ContinuousBeam.tick(player, beamSpec(player));
    }

    // ---- Ability 1: Hex Bolt (the old single shot, billy.beam = false) ----

    /** What a Hex Bolt can strike first: creatures, Ender Dragon parts and End Crystals (like the Chaos Beam). */
    private static boolean canStrike(Entity e) {
        return WandaAbilities.BEAM_TARGET.test(e) && e.isAlive();
    }

    /** What a Hex Bolt forks to: any other living creature (Billy and armor stands aside). */
    private static boolean canFork(ServerPlayer billy, Entity struck, LivingEntity e) {
        Entity body = struck instanceof PartEntity<?> part ? part.getParent() : struck;
        return e != billy && e != struck && e != body && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand)
                && e.distanceTo(struck) <= FORK_RANGE;
    }

    /**
     * An instant arc of crackling blue lightning from his hand to the first creature he looks at (or the block he looks
     * at, or the end of its range). It then forks to up to {@link #FORK_TARGETS} other creatures near the one it struck,
     * at half the damage.
     */
    private static void hexBolt(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.BILLY_HEX_BEAM, now)) {
            return;
        }
        data.startCooldown(HeroAbility.BILLY_HEX_BEAM, now, HEX_BOLT_COOLDOWN);
        spells(player).bolt = now + BOLT_POSE_TICKS;

        HitResult hit = Raycasts.any(player, HEX_BOLT_RANGE, BillyAbilities::canStrike, 1.0F);
        Vec3 end = hit.getLocation();
        List<Vec3> forks = new ArrayList<>();
        float damage = EnhancedConfig.HEX_BOLT_DAMAGE.get().floatValue();
        // 3.3: a wall in the way (a Chaos Shield, another Billy's barrier from the front, a Hex that keeps him out) takes the bolt.
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, player.getEyePosition(), end, 1.0F);
        if (shield != null) {
            end = shield.point();
            WandaAbilities.pay(shield, player, damage);
        } else if (hit instanceof EntityHitResult entityHit) {
            Entity struck = entityHit.getEntity();
            end = struck.position().add(0, struck.getBbHeight() * 0.5, 0);
            DamageSource source = ModDamageTypes.source(level, ModDamageTypes.HEX_BOLT, player, player);
            struck.hurt(source, damage);
            Vec3 strikeAt = end;
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(FORK_RANGE),
                    e -> canFork(player, struck, e) && !Shelters.shelters(player, strikeAt, e));
            near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(struck)));
            for (LivingEntity fork : near.subList(0, Math.min(FORK_TARGETS, near.size()))) {
                fork.hurt(source, damage * 0.5F);
                Vec3 at = fork.position().add(0, fork.getBbHeight() * 0.5, 0);
                forks.add(at);
                sparks(level, player, at, 6);
            }
            level.playSound(null, end.x, end.y, end.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.8F);
        }
        sparks(level, player, end, 12);

        Vec3 from = player.getEyePosition().add(0, -0.3, 0).add(player.getLookAngle().scale(0.75));
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 64.0,
                new HexBoltPayload(player.getId(), from, end, forks, player.getRandom().nextLong()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEX_BOLT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** White-hot sparks where a bolt strikes. */
    private static void sparks(ServerLevel level, Player player, Vec3 at, int count) {
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.BILLY_HEX_BEAM, MAIN), 1.2F), at.x, at.y, at.z, count,
                0.15, 0.15, 0.15, 0.12);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.BILLY_HEX_BEAM, CORE), 0.9F), at.x, at.y, at.z,
                count / 2, 0.08, 0.08, 0.08, 0.06);
    }

    // ---- Ability 4: Freeze ----

    /**
     * WandaVision episode 9: he freezes the S.W.O.R.D. soldiers. Every creature and player in front of him (a 90 degree
     * cone, {@link #FREEZE_RANGE} blocks, in line of sight) is spellbound.
     */
    private static void freeze(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.BILLY_FREEZE, now)) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(FREEZE_HALF_ANGLE));
        int duration = EnhancedConfig.FREEZE_DURATION.get();
        int frozen = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(FREEZE_RANGE),
                e -> canFreeze(player, e) && inCone(eye, look, cos, e))) {
            if (!e.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, duration, 0, false, false, true), player)) {
                continue;
            }
            frozen++;
            if (e instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            Vec3 head = new Vec3(e.getX(), e.getEyeY(), e.getZ());
            level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.BILLY_FREEZE, MAIN), 1.2F), head.x, head.y, head.z, 10,
                    e.getBbWidth() * 0.4, 0.3, e.getBbWidth() * 0.4, 0.02);
            stream(level, player, eye.add(0, -0.3, 0).add(look.scale(0.6)), head);
        }
        if (frozen == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.freeze.nothing"), true);
            return;
        }
        data.startCooldown(HeroAbility.BILLY_FREEZE, now, FREEZE_COOLDOWN);
        spells(player).freeze = now + FREEZE_POSE_TICKS;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FREEZE_SPELL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean canFreeze(ServerPlayer billy, LivingEntity e) {
        if (e == billy || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.getType().is(ModTags.SPELLBOUND_IMMUNE)) {
            return false;
        }
        if (e instanceof Player p && p.isCreative()) {
            return false;
        }
        return billy.hasLineOfSight(e);
    }

    private static boolean inCone(Vec3 eye, Vec3 look, double cos, LivingEntity e) {
        Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
        double distance = to.length();
        return distance <= FREEZE_RANGE && (distance < 1.0E-3 || to.scale(1.0 / distance).dot(look) >= cos);
    }

    /** A thin stream of blue motes from his hands to a creature he holds. */
    private static void stream(ServerLevel level, Player billy, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(40, delta.length() * 2.5);
        RandomSource random = level.getRandom();
        for (int i = 1; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            int color = AbilityColors.shade(billy, HeroAbility.BILLY_FREEZE, random.nextInt(3) == 0 ? INNER : MAIN);
            level.sendParticles(new EnergyParticleOptions(color, 0.7F), p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    // ---- Ability 5: The Noise ----

    /**
     * WandaVision episode 7: "It's so noisy in my head". While on, Billy hears the thoughts of everything around him:
     * the client makes them glow through walls, coloured by their mood, and plays their whispers (see TheNoise).
     */
    private static void theNoise(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.THE_NOISE);
        data.setToggle(Toggle.THE_NOISE, on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.the_noise.on" : "message.enhanced.the_noise.off"), true);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, on ? 0.6F : 1.2F);
    }

    /** How far The Noise reaches, as the client should read it (the server's config is synced). */
    public static double noiseRange() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.NOISE_RANGE.get() : 32;
    }

    /** Whether a creature Billy hears means harm right now (aggressive mobs: the flag is synced to clients). */
    public static boolean meansHarm(Entity entity) {
        return entity instanceof Mob mob && mob.isAggressive();
    }

    // ---- lifecycle ----

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        SPELLS.remove(player.getUUID());
        if (data.has(HeroAbility.BILLY_HEX_BEAM)) { // (3.4: a custom hero's beam may be another hero's)
            if (ContinuousBeam.isFiring(player)) {
                ContinuousBeam.stop(player);
            }
            data.set(Live.BEAMING, false);
        }
        if (BillyBarrier.owns(data)) {
            BillyBarrier.endAll(player, data);
        }
        if (data.power() == Power.CUSTOM && data.has(HeroAbility.BILLY_BUILD_MAGIC)) {
            // 3.4: a custom hero's Build Magic ends with it (Billy's own ends when his power changes: BuildMagic.onPowerChanged).
            BuildMagic.onPowerChanged(player, Power.BILLY, Power.CUSTOM);
        }
        data.set(Live.CROWNED, false);
        data.set(Live.CASTING, false);
        data.set(Live.FREEZING, false);
    }

    public static void clear() {
        SPELLS.clear();
        ContinuousBeam.clear();
    }

    private BillyAbilities() {
    }
}
