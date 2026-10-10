package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.network.HexBoltPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
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
    }

    private static final Map<UUID, Spells> SPELLS = new HashMap<>();

    // ---- input ----

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
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

    /** Keeps the crown and the casting poses in sync with what he is doing. */
    public static void tick(ServerPlayer player, PowerData data) {
        Spells spells = SPELLS.get(player.getUUID());
        long now = player.level().getGameTime();
        boolean flying = player.getAbilities().flying;
        data.set(Live.CROWNED, flying || (spells != null && now < spells.crown));
        data.set(Live.CASTING, spells != null && now < spells.bolt);
        data.set(Live.FREEZING, spells != null && now < spells.freeze);
        if (spells != null && now >= Math.max(spells.crown, Math.max(spells.bolt, spells.freeze))) {
            SPELLS.remove(player.getUUID());
        }
    }

    // ---- Ability 1: Hex Bolt ----

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
        if (!data.isReady(1, now)) {
            return;
        }
        data.startCooldown(1, now, HEX_BOLT_COOLDOWN);
        spells(player).bolt = now + BOLT_POSE_TICKS;

        HitResult hit = Raycasts.any(player, HEX_BOLT_RANGE, BillyAbilities::canStrike, 1.0F);
        Vec3 end = hit.getLocation();
        List<Vec3> forks = new ArrayList<>();
        float damage = EnhancedConfig.HEX_BOLT_DAMAGE.get().floatValue();
        if (hit instanceof EntityHitResult entityHit) {
            Entity struck = entityHit.getEntity();
            end = struck.position().add(0, struck.getBbHeight() * 0.5, 0);
            DamageSource source = ModDamageTypes.source(level, ModDamageTypes.HEX_BOLT, player, player);
            struck.hurt(source, damage);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(FORK_RANGE),
                    e -> canFork(player, struck, e));
            near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(struck)));
            for (LivingEntity fork : near.subList(0, Math.min(FORK_TARGETS, near.size()))) {
                fork.hurt(source, damage * 0.5F);
                Vec3 at = fork.position().add(0, fork.getBbHeight() * 0.5, 0);
                forks.add(at);
                sparks(level, at, 6);
            }
            level.playSound(null, end.x, end.y, end.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.8F);
        }
        sparks(level, end, 12);

        Vec3 from = player.getEyePosition().add(0, -0.3, 0).add(player.getLookAngle().scale(0.75));
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 64.0,
                new HexBoltPayload(player.getId(), from, end, forks, player.getRandom().nextLong()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEX_BOLT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** White-hot sparks where a bolt strikes. */
    private static void sparks(ServerLevel level, Vec3 at, int count) {
        level.sendParticles(new EnergyParticleOptions(MAIN, 1.2F), at.x, at.y, at.z, count, 0.15, 0.15, 0.15, 0.12);
        level.sendParticles(new EnergyParticleOptions(CORE, 0.9F), at.x, at.y, at.z, count / 2, 0.08, 0.08, 0.08, 0.06);
    }

    // ---- Ability 4: Freeze ----

    /**
     * WandaVision episode 9: he freezes the S.W.O.R.D. soldiers. Every creature and player in front of him (a 90 degree
     * cone, {@link #FREEZE_RANGE} blocks, in line of sight) is spellbound.
     */
    private static void freeze(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(4, now)) {
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
            level.sendParticles(new EnergyParticleOptions(MAIN, 1.2F), head.x, head.y, head.z, 10, e.getBbWidth() * 0.4, 0.3, e.getBbWidth() * 0.4, 0.02);
            stream(level, eye.add(0, -0.3, 0).add(look.scale(0.6)), head);
        }
        if (frozen == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.freeze.nothing"), true);
            return;
        }
        data.startCooldown(4, now, FREEZE_COOLDOWN);
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
    private static void stream(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(40, delta.length() * 2.5);
        RandomSource random = level.getRandom();
        for (int i = 1; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            int color = random.nextInt(3) == 0 ? INNER : MAIN;
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
        data.set(Live.CROWNED, false);
        data.set(Live.CASTING, false);
        data.set(Live.FREEZING, false);
    }

    public static void clear() {
        SPELLS.clear();
    }

    private BillyAbilities() {
    }
}
