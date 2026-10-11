package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The hero's army and his network: the Sentries that are his (the Sentry Army he calls, the enemy Sentries he hacks),
 * the orders he gives them, and what he does through them (Upload, the Hack, Second Life). Vision cuts every one of the
 * network's abilities: while a Vision stands within {@code visionCutRange} of him there is no Upload, no Hack and no
 * Second Life (the film: Vision "hacked into Ultron's programming and disabled his ability to escape through the net").
 *
 * <p>A drone is a {@link UltronSentryEntity} with an owner, set before it joins the level; it acts for him in its own
 * code ({@link UltronSentryEntity#tickDrone}), and falls apart when he dies, logs out, changes dimension or loses the
 * power ({@link #dissolveAll}).
 */
public final class UltronDrones {
    /** How long the Hack takes to take hold of a Sentry, in ticks. */
    public static final int HACK_TICKS = 20;
    /** The harmful effects an Upload clears. */
    private static final List<Holder<MobEffect>> HARMFUL = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.CONFUSION,
            MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.DIG_SLOWDOWN, MobEffects.POISON, MobEffects.WITHER);
    /** How far from the hero his drones are looked for when they are counted or ordered. */
    private static final double SEARCH = 128.0;
    /** How long a player who attacked the hero, or a drone of his, stays fair game for his drones, in ticks. */
    private static final int PROVOKED_TICKS = 200;
    /** The range of the order that sends the drones at a creature. */
    private static final double COMMAND_RANGE = 32.0;

    /** A Hack in progress: the Sentry being taken over and the game time it is his. */
    private record Hack(UUID target, long completesAt) {
    }

    private static final Map<UUID, Hack> HACKS = new HashMap<>();
    /** Hero → the players who attacked him or a drone of his lately → the game time that stops counting. */
    private static final Map<UUID, Map<UUID, Long>> PROVOKED = new HashMap<>();

    // ---- who has what ----

    /** The drones of the hero that are loaded and alive in his world. */
    public static List<UltronSentryEntity> drones(ServerPlayer hero) {
        return hero.serverLevel().getEntitiesOfClass(UltronSentryEntity.class, hero.getBoundingBox().inflate(SEARCH),
                s -> s.isAlive() && hero.getUUID().equals(s.getOwnerUUID()));
    }

    /** How many of the hero's drones he took over rather than called. */
    public static int hackedCount(ServerPlayer hero) {
        return (int) drones(hero).stream().filter(UltronSentryEntity::isHacked).count();
    }

    /** The nearest of the hero's drones within {@code range} of him, or null. */
    @Nullable
    private static UltronSentryEntity nearest(ServerPlayer hero, double range) {
        return drones(hero).stream().filter(s -> s.distanceTo(hero) <= range).min(Comparator.comparingDouble(s -> s.distanceToSqr(hero))).orElse(null);
    }

    // ---- the net ----

    /** Whether a Vision stands near enough to cut the hero's connection. */
    public static boolean isCut(ServerPlayer hero) {
        return UltronFight.visionNear(hero.serverLevel(), hero.position());
    }

    /** Tells the hero his connection is cut, if it is. */
    private static boolean cutOff(ServerPlayer hero) {
        if (!isCut(hero)) {
            return false;
        }
        hero.displayClientMessage(Component.translatable("message.enhanced.ultron.cut_off").withStyle(ChatFormatting.RED), true);
        return true;
    }

    // ---- ability 5: the Sentry Army ----

    /** N: {@code droneCount} Sentries of his own assemble round him, up to {@code droneMax} in all. */
    public static void army(ServerPlayer hero, PowerData data) {
        ServerLevel level = hero.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(5, now)) {
            return;
        }
        int room = UltronConfig.DRONE_MAX.get() - drones(hero).size();
        if (room <= 0) {
            hero.displayClientMessage(Component.translatable("message.enhanced.ultron.drones.max"), true);
            return;
        }
        int count = Math.min(UltronConfig.DRONE_COUNT.get(), room);
        double turn = hero.getRandom().nextDouble() * Mth.TWO_PI;
        for (int i = 0; i < count; i++) {
            double angle = turn + Mth.TWO_PI * i / count;
            UltronSentryEntity drone = UltronSentryEntity.drone(level, spawnPoint(level, hero, angle), hero.getUUID(), true, false);
            level.addFreshEntity(drone);
            UltronFx.from(level, UltronFxPayload.DRONE_ASSEMBLE, drone, drone.position(), Vec3.ZERO, 0.0F, UltronSentryEntity.ASSEMBLE_TICKS);
        }
        data.startCooldown(5, now, UltronConfig.DRONE_COOLDOWN.get());
        UltronSounds.UPLOAD.play(level, hero.position(), SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    /**
     * A free spot round the hero for a drone to form in: two and a half blocks out at the angle, or higher if that is
     * built in.
     */
    private static Vec3 spawnPoint(ServerLevel level, ServerPlayer hero, double angle) {
        Vec3 ring = hero.position().add(Math.cos(angle) * 2.5, 0.0, Math.sin(angle) * 2.5);
        for (double up : new double[]{1.0, 2.0, 3.0}) {
            Vec3 spot = ring.add(0, up, 0);
            AABB box = new AABB(spot.x - 0.35, spot.y, spot.z - 0.35, spot.x + 0.35, spot.y + 1.7, spot.z + 0.35);
            if (level.noCollision(box)) {
                return spot;
            }
        }
        return hero.position().add(0, 1.5, 0);
    }

    // ---- Sneak + N: Command ----

    /**
     * Sneak + N: every drone attacks the creature he looks at (a hostile creature, or a player when PvP allows it)
     * within 32 blocks; looking at nothing, they gather round him, and those more than 24 blocks away come to him at
     * once.
     */
    public static void command(ServerPlayer hero) {
        ServerLevel level = hero.serverLevel();
        EntityHitResult hit = Raycasts.entity(hero, COMMAND_RANGE, 0.5, e -> e instanceof LivingEntity living && commandable(hero, living), 1.0F);
        List<UltronSentryEntity> drones = drones(hero);
        if (hit != null) {
            Entity target = hit.getEntity();
            for (UltronSentryEntity drone : drones) {
                drone.order(target.getUUID());
            }
            UltronFx.at(level, UltronFxPayload.COMMAND_MARK, target.position().add(0, target.getBbHeight() * 0.5, 0), 1.5F, 10);
        } else {
            for (UltronSentryEntity drone : drones) {
                drone.order(null);
                if (drone.distanceToSqr(hero) > UltronSentryEntity.DRONE_LEASH * UltronSentryEntity.DRONE_LEASH) {
                    drone.catchUp(level, hero);
                }
            }
            UltronFx.at(level, UltronFxPayload.COMMAND_MARK, hero.position(), 1.5F, 10);
        }
    }

    /**
     * Whether the hero may send his drones at this creature: an enemy of his (the area rule), and a player only when
     * PvP allows it.
     */
    private static boolean commandable(ServerPlayer hero, LivingEntity creature) {
        if (creature == hero || !creature.isAlive() || creature.isSpectator() || creature instanceof ArmorStand) {
            return false;
        }
        if (creature instanceof UltronSentryEntity sentry && hero.getUUID().equals(sentry.getOwnerUUID())) {
            return false;
        }
        return AreaTargets.mayHurt(hero, creature, false);
    }

    // ---- ability 4: Upload ----

    /**
     * B: he jumps into one of his drones, the one he looks at or else the nearest within {@code uploadRange}: it bursts
     * into data and he stands where it was, healed, rid of every harmful effect and no longer on fire. Without a drone
     * he is told so; with a Vision near, or where a Hex or an arena would not let him through, nothing happens.
     */
    public static void upload(ServerPlayer hero, PowerData data) {
        ServerLevel level = hero.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(4, now) || cutOff(hero)) {
            return;
        }
        int range = UltronConfig.UPLOAD_RANGE.get();
        UltronSentryEntity drone = lookedAtDrone(hero, range);
        if (drone == null) {
            drone = nearest(hero, range);
        }
        Vec3 to = drone == null ? null : landing(hero, drone);
        if (to == null) {
            hero.displayClientMessage(Component.translatable("message.enhanced.ultron.no_drone"), true);
            return;
        }
        if (!HexManager.onTeleport(level, hero, to)) {
            return;
        }
        jumpInto(level, hero, drone, to);
        hero.heal(UltronConfig.UPLOAD_HEAL.get().floatValue());
        for (Holder<MobEffect> effect : HARMFUL) {
            hero.removeEffect(effect);
        }
        hero.clearFire();
        data.startCooldown(4, now, UltronConfig.UPLOAD_COOLDOWN.get());
    }

    @Nullable
    private static UltronSentryEntity lookedAtDrone(ServerPlayer hero, double range) {
        EntityHitResult hit = Raycasts.entity(hero, range, 0.6, e -> e instanceof UltronSentryEntity s && s.isAlive() && hero.getUUID().equals(s.getOwnerUUID()), 1.0F);
        return hit == null ? null : (UltronSentryEntity) hit.getEntity();
    }

    /**
     * Where the hero stands when he takes the drone's place: its feet, or the nearest spot to them with room for his
     * body.
     */
    @Nullable
    private static Vec3 landing(ServerPlayer hero, UltronSentryEntity drone) {
        Vec3 at = drone.position();
        for (Vec3 offset : new Vec3[]{Vec3.ZERO, new Vec3(0, -1, 0), new Vec3(0, 1, 0), new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
                new Vec3(0, 0, 1), new Vec3(0, 0, -1)}) {
            Vec3 spot = at.add(offset);
            if (Teleports.fits(hero, spot)) {
                return spot;
            }
        }
        return null;
    }

    /** The data jump: flashes at both ends, a line of data between them, the hero moved, the drone gone. */
    private static void jumpInto(ServerLevel level, ServerPlayer hero, UltronSentryEntity drone, Vec3 to) {
        Vec3 from = hero.position();
        UltronFx.at(level, UltronFxPayload.NETWORK_FLASH, from.add(0, 1.0, 0), 2.0F, 10);
        UltronFx.line(level, UltronFxPayload.DATA_LINE, from.add(0, 1.0, 0), to.add(0, 1.0, 0), 20);
        UltronSounds.UPLOAD.play(level, from, SoundSource.PLAYERS, 1.0F, 1.0F);
        hero.teleportTo(level, to.x, to.y, to.z, hero.getYRot(), hero.getXRot());
        hero.resetFallDistance();
        UltronFx.at(level, UltronFxPayload.NETWORK_FLASH, to.add(0, 1.0, 0), 2.0F, 10);
        UltronSounds.UPLOAD.play(level, to, SoundSource.PLAYERS, 1.0F, 1.3F);
        drone.dissolve();
    }

    // ---- passive: Second Life ----

    /**
     * A blow that would kill the hero: if his fourth ability is ready, no Vision is near and one of his drones is
     * within {@code secondLifeRange}, he does not die. He is set to {@code secondLifeHealth} of his maximum, jumps into
     * the drone (as in Upload, where a Hex or an arena allows it) and has Resistance IV for three seconds, and the
     * ability recharges for {@code secondLifeCooldown}. The void and {@code /kill} are not saved.
     *
     * @return true if he was saved
     */
    public static boolean secondLife(ServerPlayer hero, DamageSource source) {
        PowerData data = Powers.get(hero);
        ServerLevel level = hero.serverLevel();
        long now = level.getGameTime();
        if (data.power() != Power.ULTRON || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !data.isReady(4, now) || isCut(hero)) {
            return false;
        }
        UltronSentryEntity drone = nearest(hero, UltronConfig.SECOND_LIFE_RANGE.get());
        if (drone == null) {
            return false;
        }
        hero.setHealth(Math.max(1.0F, hero.getMaxHealth() * UltronConfig.SECOND_LIFE_HEALTH.get().floatValue()));
        hero.clearFire();
        Vec3 to = landing(hero, drone);
        if (to != null && HexManager.onTeleport(level, hero, to)) {
            jumpInto(level, hero, drone, to);
        } else {
            UltronFx.at(level, UltronFxPayload.NETWORK_FLASH, hero.position().add(0, 1.0, 0), 3.0F, 10);
        }
        hero.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        data.startCooldown(4, now, UltronConfig.SECOND_LIFE_COOLDOWN.get());
        return true;
    }

    // ---- Sneak + B: the Hack ----

    /**
     * Sneak + B: he reaches for the enemy Sentry he looks at (within {@code hackRange}); twenty ticks later it is his
     * drone. At most {@code hackMax} hacked drones and {@code droneMax} drones in all; never a body of Ultron's.
     */
    public static void hack(ServerPlayer hero, PowerData data) {
        if (HACKS.containsKey(hero.getUUID()) || cutOff(hero)) {
            return;
        }
        ServerLevel level = hero.serverLevel();
        EntityHitResult hit = Raycasts.entity(hero, UltronConfig.HACK_RANGE.get(), 0.6, UltronDrones::hackable, 1.0F);
        if (hit == null) {
            hero.displayClientMessage(Component.translatable("message.enhanced.ultron.hack.none"), true);
            return;
        }
        if (hackedCount(hero) >= UltronConfig.HACK_MAX.get() || drones(hero).size() >= UltronConfig.DRONE_MAX.get()) {
            hero.displayClientMessage(Component.translatable("message.enhanced.ultron.drones.max"), true);
            return;
        }
        Entity target = hit.getEntity();
        HACKS.put(hero.getUUID(), new Hack(target.getUUID(), level.getGameTime() + HACK_TICKS));
        data.set(Live.CASTING, true);
        UltronFx.from(level, UltronFxPayload.HACK_BEAM, hero, hero.getEyePosition().add(hero.getLookAngle().scale(0.6)),
                target.position().add(0, target.getBbHeight() * 0.5, 0), 0.0F, HACK_TICKS);
        UltronSounds.UPLOAD.play(level, hero.position(), SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    /** An enemy Sentry: one that belongs to nobody (a fight's, or any other) and is whole. */
    private static boolean hackable(Entity entity) {
        return entity instanceof UltronSentryEntity sentry && sentry.isAlive() && sentry.getOwnerUUID() == null && !sentry.isAssembling();
    }

    /** Each tick of the hero: the Hack he is casting reaches its Sentry, or loses it (dead, gone, too far, cut off). */
    public static void tickHack(ServerPlayer hero, PowerData data) {
        Hack hack = HACKS.get(hero.getUUID());
        if (hack == null) {
            return;
        }
        ServerLevel level = hero.serverLevel();
        Entity target = level.getEntity(hack.target());
        double reach = UltronConfig.HACK_RANGE.get() * 1.5;
        if (!hackable(target) || target.distanceTo(hero) > reach || isCut(hero)) {
            stopHack(hero, data);
            return;
        }
        if (level.getGameTime() < hack.completesAt()) {
            return;
        }
        stopHack(hero, data);
        if (hackedCount(hero) >= UltronConfig.HACK_MAX.get() || drones(hero).size() >= UltronConfig.DRONE_MAX.get()) {
            hero.displayClientMessage(Component.translatable("message.enhanced.ultron.drones.max"), true);
            return;
        }
        takeOver(level, hero, (UltronSentryEntity) target);
    }

    /**
     * The Sentry becomes his drone. It is replaced by a new one that has an owner before it joins the level (and no
     * mark of a boss fight, so it leaves the fight's count and its bolts hurt the fight's Sentries): the budget is
     * untouched, and the fight forgets the old one at once.
     */
    private static void takeOver(ServerLevel level, ServerPlayer hero, UltronSentryEntity enemy) {
        UltronSentryEntity drone = UltronSentryEntity.drone(level, enemy.position(), hero.getUUID(), false, true);
        drone.setYRot(enemy.getYRot());
        drone.setYHeadRot(enemy.getYHeadRot());
        drone.setHealth(Math.max(1.0F, drone.getMaxHealth() * enemy.getHealth() / enemy.getMaxHealth()));
        UltronFight fight = enemy.fight();
        enemy.discard();
        level.addFreshEntity(drone);
        if (fight != null) {
            fight.refresh();
        }
        UltronFx.at(level, UltronFxPayload.NETWORK_FLASH, drone.position().add(0, 1.0, 0), 1.5F, 10);
        UltronSounds.UPLOAD.play(level, drone.position(), SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void stopHack(ServerPlayer hero, PowerData data) {
        HACKS.remove(hero.getUUID());
        data.set(Live.CASTING, false);
    }

    /** Whether the hero is in the middle of a Hack. */
    public static boolean isHacking(ServerPlayer hero) {
        return HACKS.containsKey(hero.getUUID());
    }

    // ---- who a drone may go for ----

    /** A player attacked the hero, or a drone of his: for a while his drones may hit back (with PvP on). */
    public static void provoke(ServerPlayer hero, Player attacker) {
        PROVOKED.computeIfAbsent(hero.getUUID(), id -> new HashMap<>()).put(attacker.getUUID(), hero.serverLevel().getGameTime() + PROVOKED_TICKS);
    }

    /** Whether the player attacked the hero or a drone of his lately. */
    public static boolean provoked(ServerPlayer hero, Player player) {
        Map<UUID, Long> attackers = PROVOKED.get(hero.getUUID());
        Long until = attackers == null ? null : attackers.get(player.getUUID());
        return until != null && until >= hero.serverLevel().getGameTime();
    }

    // ---- the rules the rest of the mod asks about a drone ----

    /** A bolt a drone fired: a hero's, so the orphan sweep of the boss framework leaves it alone. */
    public static boolean isDroneBolt(Entity entity) {
        return entity instanceof UltronBoltEntity bolt && bolt.getOwner() instanceof UltronSentryEntity sentry && sentry.isDrone();
    }

    /** Whether the blow was struck by a drone (its punch, or its bolt) for its master, not by the master himself. */
    public static boolean isDroneBlow(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct instanceof UltronSentryEntity || (direct instanceof UltronBoltEntity bolt && bolt.getOwner() instanceof UltronSentryEntity);
    }

    /** The area rule's ally: an attack of the hero's that hits everything in an area spares his own drones. */
    public static boolean isHisDrone(LivingEntity owner, LivingEntity target) {
        return target instanceof UltronSentryEntity sentry && owner.getUUID().equals(sentry.getOwnerUUID());
    }

    // ---- when the hero is gone ----

    /** Every drone of the hero, in every world, falls apart in sparks. */
    public static void dissolveAll(MinecraftServer server, UUID hero) {
        List<UltronSentryEntity> found = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof UltronSentryEntity sentry && hero.equals(sentry.getOwnerUUID())) {
                    found.add(sentry);
                }
            }
        }
        found.forEach(UltronSentryEntity::dissolve);
    }

    /** Forgets what the hero's army and net had in memory: a Hack in progress, who provoked his drones. */
    public static void forget(UUID hero) {
        HACKS.remove(hero);
        PROVOKED.remove(hero);
    }

    /** The server stopped: nothing is remembered. */
    public static void clear() {
        HACKS.clear();
        PROVOKED.clear();
    }

    private UltronDrones() {
    }
}
