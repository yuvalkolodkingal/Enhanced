package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFx;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Apocalypse, En Sabah Nur: a player who won the Essence from the boss. The first mutant: sand answers him, he steps
 * through violet portals, a golden shield stops everything but the mind, and he makes others stronger (the Horsemen of the
 * film). The plug-in is {@link ApocalypseServer}; the shield (B) is in {@link ApocalypseGuard}; the sand the boss throws is
 * the very code the hero uses ({@link SandControl}, {@link SandHazards}), so what he does to you in the fight is what you do
 * after it. DESIGN section 17.
 *
 * <p>R is the Sand Wave (Sneak: Entomb), G Levitation, V the Portal, B the Shield (held), N Power Bestowal (Sneak: the
 * Sandstorm). He never places or breaks a block, so mobGriefing and the protection of an arena change nothing.
 */
public final class ApocalypseAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("apocalypse_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("apocalypse_armor");
    private static final ResourceLocation BODY_TOUGHNESS = Enhanced.id("apocalypse_toughness");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("apocalypse_knockback");

    /** The cooldown slots of the abilities: R and Sneak + R, V, B, N and Sneak + N. */
    private static final int WAVE_SLOT = 1;
    private static final int PORTAL_SLOT = 3;
    private static final int BESTOW_SLOT = 5;

    /** How far below him the ground may be for the Wave. */
    private static final double GROUND_REACH = 6.0;
    /** The Wave is four wide and a little over a block tall; it throws what it hits a block along it. */
    private static final double WAVE_WIDTH = 4.0;
    private static final double WAVE_HEIGHT = 1.1;
    private static final double WAVE_KNOCKBACK = 1.0;
    /** The ticks the cocoon takes to close, for the picture. */
    private static final int ENTOMB_CLOSE_TICKS = 6;
    /** The portal's rings stay open this long, and are this big (a radius times ten). */
    private static final int PORTAL_TICKS = 20;
    private static final int PORTAL_SIZE = 12;
    /** The Sandstorm hurts every ten ticks and slows its victims for a little longer than that. */
    private static final int STORM_PULSE = 10;
    private static final int STORM_SLOW_TICKS = 30;
    /** Marks a projectile the Sandstorm has already slowed to half: it is slowed once. */
    public static final String SLOWED_TAG = "enhanced_apocalypse_slowed";
    private static final int VIOLET = 0xB04FD8;

    /** What each hero (and each empowered player) carries from tick to tick; forgotten when they leave or the server stops. */
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        /** The game time he last took damage (the healing factor waits for quiet). */
        long lastHurt = Long.MIN_VALUE / 2;
        /** The game time the Sandstorm began and ends (0: none). */
        long stormStart;
        long stormEnd;
        /** The part of a tick of extra cooldown recharge not yet given (Empowered). */
        double haste;
        /** The allies he has empowered, the oldest first. */
        final Deque<UUID> bestowed = new ArrayDeque<>();
    }

    private static State state(UUID id) {
        return STATES.computeIfAbsent(id, k -> new State());
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (!down) {
            if (ability == ApocalypseGuard.SLOT && data.chaosShieldActive()) {
                ApocalypseGuard.drop(player, data, false);
            }
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (player.isShiftKeyDown()) {
                    entomb(player, data);
                } else {
                    wave(player, data);
                }
            }
            case 2 -> PowerTicker.toggleFlight(player);
            case 3 -> portal(player, data);
            case 4 -> ApocalypseGuard.raise(player, data);
            case 5 -> {
                if (player.isShiftKeyDown()) {
                    sandstorm(player, data);
                } else {
                    bestow(player, data);
                }
            }
            default -> {
            }
        }
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        healingFactor(player);
        if (data.chaosShieldActive()) {
            ApocalypseGuard.tick(player, data);
        }
        stormTick(player, data);
    }

    /** For every player: a cocoon covers the hands (a held ability lets go), and Empowered recharges cooldowns faster. */
    static void everyPlayerTick(ServerPlayer player, PowerData data) {
        if (player.hasEffect(ModEffects.ENTOMBED)) {
            PowerTicker.stopHoldAbilities(data);
        }
        if (player.hasEffect(ModEffects.EMPOWERED)) {
            hasten(player, data);
        }
    }

    /** What is left of a held ability when the key cannot be held any more (the player died, or is held in sand). */
    static void stopHold(PowerData data) {
        data.set(Live.SANDSTORM, false);
    }

    // ---- the body ----

    /**
     * The body of the first mutant (40 health, 10 armour, 4 toughness, hard to knock back) while the player is Apocalypse,
     * and nothing of it on anyone else. The modifiers are permanent, so they are already right when a player logs in.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean hero = data.power() == Power.APOCALYPSE;
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, hero ? ApocalypseConfig.BODY_HEALTH_BONUS.get() : 0.0, hero);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, hero ? ApocalypseConfig.ARMOR.get() : 0.0, hero);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS, hero ? ApocalypseConfig.ARMOR_TOUGHNESS.get() : 0.0, hero);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK,
                hero ? ApocalypseConfig.KNOCKBACK_RESISTANCE.get() : 0.0, hero);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** He was hurt just now: the healing factor starts its wait over. */
    public static void hurt(ServerPlayer player) {
        state(player.getUUID()).lastHurt = player.level().getGameTime();
    }

    /**
     * The healing factor of the film: after {@code regenDelayTicks} without taking damage he heals {@code regenPerSecond} a
     * second (once a second, from the end of the wait). 0 turns it off.
     */
    private static void healingFactor(ServerPlayer player) {
        double regen = ApocalypseConfig.REGEN_PER_SECOND.get();
        if (regen <= 0.0 || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        State state = STATES.get(player.getUUID());
        long quiet = player.level().getGameTime() - (state == null ? Long.MIN_VALUE / 2 : state.lastHurt) - ApocalypseConfig.REGEN_DELAY_TICKS.get();
        if (quiet >= 0 && quiet % 20 == 0) {
            player.heal((float) regen);
        }
    }

    // ---- Ability 1: the Sand Wave ----

    /** R: a wave of sand rolls from his feet along the ground in the direction he faces. */
    private static void wave(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(WAVE_SLOT, now)) {
            return;
        }
        // A levitating Apocalypse casts it at the ground beneath; with none in reach there is no sand to pull.
        double ground = SandControl.groundAt(level, player.getX(), player.getY() + 0.5, player.getZ());
        if (player.getY() - ground > GROUND_REACH) {
            player.displayClientMessage(Component.translatable("message.enhanced.sand.no_ground"), true);
            return;
        }
        Vec3 dir = SandControl.flat(Vec3.ZERO, player.getLookAngle());
        if (dir.equals(Vec3.ZERO)) {
            dir = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        SandHazards.WaveSpec spec = new SandHazards.WaveSpec(ApocalypseConfig.WAVE_SPEED.get(), ApocalypseConfig.WAVE_LENGTH.get(),
                WAVE_WIDTH, WAVE_HEIGHT, ApocalypseConfig.WAVE_DAMAGE.get().floatValue(), ApocalypseConfig.WAVE_SLOW_TICKS.get(), WAVE_KNOCKBACK);
        SandHazards.wave(level, player, null, new Vec3(player.getX(), ground, player.getZ()), dir, spec, null, 0.0);
        data.startCooldown(WAVE_SLOT, now, ApocalypseConfig.WAVE_COOLDOWN.get());
        player.causeFoodExhaustion(ApocalypseConfig.WAVE_EXHAUSTION.get().floatValue());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SAND_WAVE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ---- Ability 1, Sneak: Entomb ----

    /**
     * Sneak + R: sand crawls up the legs of the creature he looks at and swallows it in a cocoon. It cannot move, jump, fly,
     * attack, use items or use hero abilities for {@code entombTicks}; a friend frees it by hitting the cocoon. Refused (and
     * no cooldown) for what the sand cannot hold: bosses, a phased Vision, Creative and Spectator players, a complete shield.
     */
    private static void entomb(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(WAVE_SLOT, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, ApocalypseConfig.ENTOMB_RANGE.get(), 0.3,
                e -> e instanceof LivingEntity && e != player && !e.isSpectator() && !(e instanceof ArmorStand), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.entomb.nothing"), true);
            return;
        }
        LivingEntity victim = (LivingEntity) hit.getEntity();
        String refusal = SandControl.entombRefusal(victim);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal), true);
            return;
        }
        // A player only with PvP on; never his own side.
        if (!AreaTargets.mayHurt(player, victim, true)) {
            player.displayClientMessage(Component.translatable("message.enhanced.entomb.nothing"), true);
            return;
        }
        SandCocoonEntity cocoon = SandControl.entomb(level, player, victim, ApocalypseConfig.ENTOMB_TICKS.get(),
                ApocalypseConfig.ENTOMB_DAMAGE_PER_SECOND.get().floatValue(), ApocalypseConfig.COCOON_HEALTH.get().floatValue(), null);
        if (cocoon == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.entomb.slips"), true);
            return;
        }
        data.startCooldown(WAVE_SLOT, now, ApocalypseConfig.ENTOMB_COOLDOWN.get());
        player.causeFoodExhaustion(ApocalypseConfig.ENTOMB_EXHAUSTION.get().floatValue());
        ApocalypseFx.send(level, victim.position(), ApocalypseFxPayload.ENTOMB, player.getId(), player.position(), victim.position(),
                victim.getId(), ENTOMB_CLOSE_TICKS);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.ENTOMB.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ---- Ability 3: the Portal ----

    /**
     * V: a violet portal to where he looks (on top of the block, or in front of its face), up to {@code portalRange}
     * blocks. He takes with him the tamed pets and teammates within {@code portalAllyRadius} and every other player within
     * it who is sneaking (stepping in is consent); each traveller is judged by the Hex wall and by the arena of a boss
     * fight, and one who is refused stays. Aimed at nothing, or where there is no room, nothing happens.
     */
    private static void portal(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(PORTAL_SLOT, now)) {
            return;
        }
        BlockHitResult hit = Raycasts.block(player, ApocalypseConfig.PORTAL_RANGE.get(), 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            player.displayClientMessage(Component.translatable("message.enhanced.portal.nothing"), true);
            return;
        }
        Vec3 to = Teleports.onto(player, hit);
        if (to == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.teleport.no_room"), true);
            return;
        }
        List<LivingEntity> companions = companions(player);
        if (!HexManager.onTeleport(level, player, to)) {
            return; // a Hex wall, the arena of a boss fight, or Mjolnir holds him
        }
        data.startCooldown(PORTAL_SLOT, now, ApocalypseConfig.PORTAL_COOLDOWN.get());
        player.causeFoodExhaustion(ApocalypseConfig.PORTAL_EXHAUSTION.get().floatValue());
        Vec3 from = player.position();
        player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        for (int i = 0; i < companions.size(); i++) {
            LivingEntity traveller = companions.get(i);
            Vec3 spot = spotNear(traveller, to, i, companions.size());
            if (spot == null || !HexManager.onTeleport(level, traveller, spot)) {
                continue; // no room, or the wall refuses this one: it stays
            }
            if (traveller instanceof ServerPlayer friend) {
                friend.teleportTo(level, spot.x, spot.y, spot.z, friend.getYRot(), friend.getXRot());
            } else {
                traveller.teleportTo(spot.x, spot.y, spot.z);
            }
            traveller.resetFallDistance();
        }
        ApocalypseFx.send(level, from, ApocalypseFxPayload.PORTAL, player.getId(), from.add(0, 1.0, 0), to.add(0, 1.0, 0), PORTAL_TICKS, PORTAL_SIZE);
        level.playSound(null, from.x, from.y, from.z, ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, to.x, to.y, to.z, ModSounds.APOCALYPSE_PORTAL.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /** Those who step through with him: his pets and teammates close by, and any player close by who is sneaking. */
    private static List<LivingEntity> companions(ServerPlayer hero) {
        double radius = ApocalypseConfig.PORTAL_ALLY_RADIUS.get();
        List<LivingEntity> found = new ArrayList<>();
        if (radius <= 0.0) {
            return found;
        }
        for (LivingEntity other : hero.level().getEntitiesOfClass(LivingEntity.class, hero.getBoundingBox().inflate(radius),
                e -> e != hero && e.isAlive() && !e.isSpectator())) {
            if (other.distanceTo(hero) > radius) {
                continue;
            }
            boolean along = other instanceof ServerPlayer friend ? friend.isShiftKeyDown() || friend.isAlliedTo(hero)
                    : other instanceof OwnableEntity pet && hero.getUUID().equals(pet.getOwnerUUID())
                    && !(other instanceof TamableAnimal tamed && tamed.isOrderedToSit());
            if (along) {
                found.add(other);
            }
        }
        return found;
    }

    /** Where a traveller arrives: standing on the ground within about two blocks of the hero's arrival, or nowhere. */
    @Nullable
    private static Vec3 spotNear(Entity traveller, Vec3 hero, int index, int total) {
        double turn = (index + 0.5) * Math.PI * 2.0 / Math.max(1, total);
        for (double reach : new double[]{1.5, 2.0, 1.0}) {
            for (double up : new double[]{0.0, 1.0, -1.0}) {
                Vec3 spot = hero.add(Math.cos(turn) * reach, up, Math.sin(turn) * reach);
                if (Teleports.fits(traveller, spot) && !Teleports.fits(traveller, spot.subtract(0, 0.6, 0))) {
                    return spot;
                }
            }
        }
        return null;
    }

    // ---- Ability 5: Power Bestowal ----

    /** Whom he can empower: his own, which is any player, any tamed pet, an Iron Golem or a villager. */
    private static boolean isOwn(LivingEntity target) {
        return target instanceof Player || target instanceof IronGolem || target instanceof AbstractVillager
                || target instanceof OwnableEntity pet && pet.getOwnerUUID() != null;
    }

    /**
     * N: the ally he looks at (within {@code bestowRange}) is empowered for {@code bestowTicks}: stronger, tougher, quicker
     * to heal, and, if a hero, recharging cooldowns {@code empowerCooldownBoost} times as fast. At most {@code bestowMax}
     * at once: a further one takes the place of the oldest. A hostile creature is refused, and nothing in sight costs nothing.
     */
    private static void bestow(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(BESTOW_SLOT, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, ApocalypseConfig.BESTOW_RANGE.get(), 0.3,
                e -> e instanceof LivingEntity && e != player && !e.isSpectator() && !(e instanceof ArmorStand), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.bestow.nothing"), true);
            return;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        if (!isOwn(target)) {
            player.displayClientMessage(Component.translatable("message.enhanced.bestow.hostile"), true);
            return;
        }
        State state = state(player.getUUID());
        MinecraftServer server = level.getServer();
        pruneBestowed(server, state);
        state.bestowed.remove(target.getUUID());
        while (state.bestowed.size() >= ApocalypseConfig.BESTOW_MAX.get()) {
            LivingEntity oldest = find(server, state.bestowed.pollFirst());
            if (oldest != null) {
                oldest.removeEffect(ModEffects.EMPOWERED);
            }
        }
        state.bestowed.addLast(target.getUUID());
        target.addEffect(new MobEffectInstance(ModEffects.EMPOWERED, ApocalypseConfig.BESTOW_TICKS.get(), 0, false, true, true), player);

        data.startCooldown(BESTOW_SLOT, now, ApocalypseConfig.BESTOW_COOLDOWN.get());
        player.causeFoodExhaustion(ApocalypseConfig.BESTOW_EXHAUSTION.get().floatValue());
        player.displayClientMessage(Component.translatable("message.enhanced.bestow.done", target.getDisplayName()), true);
        flow(level, player.getEyePosition().add(player.getLookAngle().scale(0.6)).subtract(0, 0.4, 0), target.position().add(0, target.getBbHeight() * 0.6, 0));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.APOCALYPSE_BESTOW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Gold and violet light flows from his hand to the one he empowers. */
    private static void flow(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 path = to.subtract(from);
        int steps = Math.max(2, (int) (path.length() * 2.0));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(path.scale((double) i / steps));
            level.sendParticles(new EnergyParticleOptions(i % 2 == 0 ? ApocalypseGuard.GOLD : VIOLET, 1.1F), at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        level.sendParticles(new EnergyParticleOptions(ApocalypseGuard.GOLD, 1.6F), to.x, to.y, to.z, 24, 0.4, 0.6, 0.4, 0.05);
    }

    /** Forgets those who no longer carry the effect (it ran out, or they are gone). */
    private static void pruneBestowed(MinecraftServer server, State state) {
        state.bestowed.removeIf(id -> {
            LivingEntity entity = find(server, id);
            return entity == null || !entity.hasEffect(ModEffects.EMPOWERED);
        });
    }

    @Nullable
    private static LivingEntity find(MinecraftServer server, @Nullable UUID id) {
        if (id == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /** The allies this hero has empowered and who still are (the oldest first), for tests and the HUD. */
    public static List<UUID> bestowed(ServerPlayer hero) {
        State state = STATES.get(hero.getUUID());
        if (state == null) {
            return List.of();
        }
        pruneBestowed(hero.server, state);
        return List.copyOf(state.bestowed);
    }

    /**
     * An empowered hero's cooldowns run out {@code empowerCooldownBoost} times as fast: each tick takes a further
     * {@code boost - 1} ticks off every cooldown that runs (the fraction is carried to the next tick).
     */
    private static void hasten(ServerPlayer player, PowerData data) {
        if (data.power() == Power.NONE) {
            return;
        }
        State state = state(player.getUUID());
        state.haste += ApocalypseConfig.EMPOWER_COOLDOWN_BOOST.get() - 1.0;
        long whole = (long) state.haste;
        if (whole > 0) {
            state.haste -= whole;
            data.shiftCooldowns(player.level().getGameTime(), whole);
        }
    }

    // ---- Ability 5, Sneak: the Sandstorm ----

    /**
     * Sneak + N: a storm of sand rises round him for {@code stormTicks}, {@code stormRadius} blocks wide and following him.
     * Every ten ticks it hurts whom the sand may hurt and slows them; projectiles that enter it are slowed to half.
     */
    private static void sandstorm(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(BESTOW_SLOT, now) || data.is(Live.SANDSTORM)) {
            return;
        }
        State state = state(player.getUUID());
        state.stormStart = now;
        state.stormEnd = now + ApocalypseConfig.STORM_TICKS.get();
        data.set(Live.SANDSTORM, true);
        data.startCooldown(BESTOW_SLOT, now, ApocalypseConfig.STORM_COOLDOWN.get());
        player.causeFoodExhaustion(ApocalypseConfig.STORM_EXHAUSTION.get().floatValue());
        ApocalypseFx.send(level, player.position(), ApocalypseFxPayload.SANDSTORM, player.getId(), player.position(), Vec3.ZERO,
                ApocalypseConfig.STORM_TICKS.get(), (int) Math.round(ApocalypseConfig.STORM_RADIUS.get() * 10.0));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SAND_WAVE.get(), SoundSource.PLAYERS, 1.2F, 0.6F);
    }

    /** The storm's step: it follows him, hurts every ten ticks, slows the projectiles that enter, and ends on time. */
    private static void stormTick(ServerPlayer player, PowerData data) {
        State state = STATES.get(player.getUUID());
        boolean raging = data.is(Live.SANDSTORM);
        if (!raging) {
            if (state != null && state.stormEnd != 0) {
                endStorm(player, data, state); // it was cut short (a cocoon holds his hands)
            }
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (state == null || state.stormEnd == 0 || now >= state.stormEnd) {
            endStorm(player, data, state);
            return;
        }
        double radius = ApocalypseConfig.STORM_RADIUS.get();
        AABB box = new AABB(player.getX() - radius, player.getY() - 1.0, player.getZ() - radius, player.getX() + radius,
                player.getY() + radius, player.getZ() + radius);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, p -> slowable(player, p, radius))) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.5));
            projectile.getPersistentData().putBoolean(SLOWED_TAG, true);
            projectile.hasImpulse = true;
        }
        long age = now - state.stormStart;
        if (age % STORM_PULSE == 0) {
            float damage = ApocalypseConfig.STORM_DAMAGE.get().floatValue();
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
                double reach = radius + victim.getBbWidth() * 0.5;
                if (flatDistanceSqr(player, victim) > reach * reach || !SandControl.canHurt(player, victim)) {
                    continue;
                }
                // The sand hurts, it does not shove: the storm would push everything it hurts out of itself.
                Vec3 motion = victim.getDeltaMovement();
                SandControl.hurt(level, player, victim, damage);
                victim.setDeltaMovement(motion);
                if (victim.isAlive()) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STORM_SLOW_TICKS, 0), player);
                }
            }
        }
        if (age % 2 == 0) {
            grains(level, player, radius);
        }
    }

    private static boolean slowable(ServerPlayer hero, Projectile projectile, double radius) {
        Entity owner = projectile.getOwner();
        return owner != hero && !(owner != null && owner.isAlliedTo(hero)) && !projectile.getPersistentData().getBoolean(SLOWED_TAG)
                && !ApocalypseGuard.headingHome(projectile)
                && Math.hypot(projectile.getX() - hero.getX(), projectile.getZ() - hero.getZ()) <= radius;
    }

    private static double flatDistanceSqr(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    /** A few grains of sand whirl round the edge of the storm (the funnel itself is drawn by the clients). */
    private static void grains(ServerLevel level, ServerPlayer hero, double radius) {
        BlockParticleOption sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
        for (int i = 0; i < 6; i++) {
            double angle = hero.getRandom().nextDouble() * Math.PI * 2.0;
            double r = radius * (0.4 + 0.6 * hero.getRandom().nextDouble());
            level.sendParticles(sand, hero.getX() + Math.cos(angle) * r, hero.getY() + hero.getRandom().nextDouble() * 3.0,
                    hero.getZ() + Math.sin(angle) * r, 1, 0.0, 0.0, 0.0, 0.05);
        }
    }

    private static void endStorm(ServerPlayer player, PowerData data, @Nullable State state) {
        data.set(Live.SANDSTORM, false);
        if (state != null && state.stormEnd != 0) {
            state.stormStart = 0;
            state.stormEnd = 0;
            ApocalypseFx.send(player.serverLevel(), player.position(), ApocalypseFxPayload.SANDSTORM, player.getId(), player.position(),
                    Vec3.ZERO, 0, 0);
        }
    }

    // ---- the end of the power, of the life, of the session ----

    /**
     * The power is being taken away: everything he left in the world ends. The shield and the storm stop, the sand still
     * rolling is forgotten, the cocoons he cast let go and the allies he empowered are no longer.
     */
    static void endAll(ServerPlayer player, PowerData data) {
        State state = STATES.get(player.getUUID());
        endStorm(player, data, state);
        data.setChaosShieldActive(false);
        data.setShieldStrength(0.0F);
        ApocalypseGuard.forget(player.getUUID());
        SandHazards.clearOwnedBy(player.getUUID());
        MinecraftServer server = player.server;
        for (ServerLevel level : server.getAllLevels()) {
            for (SandCocoonEntity cocoon : level.getEntities(EntityTypeTest.forClass(SandCocoonEntity.class),
                    c -> !c.isRemoved() && player.getUUID().equals(c.casterId()))) {
                cocoon.release();
            }
        }
        if (state != null) {
            for (UUID id : state.bestowed) {
                LivingEntity empowered = find(server, id);
                if (empowered != null) {
                    empowered.removeEffect(ModEffects.EMPOWERED);
                }
            }
            state.bestowed.clear();
        }
    }

    /** The hero died or left: what he was holding ends (the storm, the shield, the sand still rolling). */
    static void onGone(ServerPlayer player, boolean leaving) {
        PowerData data = Powers.get(player);
        if (data.power() == Power.APOCALYPSE) {
            endStorm(player, data, STATES.get(player.getUUID()));
            data.setChaosShieldActive(false);
            SandHazards.clearOwnedBy(player.getUUID());
        }
        ApocalypseGuard.forget(player.getUUID());
        if (leaving) {
            STATES.remove(player.getUUID());
        }
    }

    /** The server has stopped: nothing of the hero's is kept. */
    static void clear() {
        STATES.clear();
        ApocalypseGuard.clear();
    }

    private ApocalypseAbilities() {
    }
}
