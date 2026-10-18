package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.XavierInputPayload;
import com.kolodkingal.enhanced.network.XavierPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Professor X's Take Over a Body (3.3, V): his mind goes into another body and steers it, as he takes over the guard in
 * Days of Future Past. His own body sits still where he left it, two fingers to his temple ({@link Live#MIND_LINKED}),
 * and his camera goes to the eyes of the body he steers (his client, {@code XavierControl}).
 *
 * <p><b>A creature</b> walks where his keys say, looks where he looks, and attacks what he looks at when he clicks
 * (a creeper lights its fuse, a skeleton shoots); its own will is switched off while he is in it (the goals that move,
 * look, jump and choose targets), and given back when he leaves, with a moment of confusion. <b>A player</b> (only with
 * PvP on, for a short time) cannot move by their own keys ({@link Live#MIND_CONTROLLED}: their client stops them, as
 * for a pinned player); the server gives their body the velocity his keys ask for, a motion packet their own client's
 * physics carries out, as Sokovia carries a player. Nothing he sends is a position: {@link XavierInputPayload} is a
 * wish, clamped and checked.
 *
 * <p>The link is safe: it ends when he lets go (V again), when its time is up, when the body goes further than {@code
 * linkMaxDistance} from his own or into another dimension, when either of them dies, logs out or changes dimension,
 * when his own body is hurt ({@code linkBreaksOnHurt}), when the mind closes (Magneto's helmet, a hero with
 * telepathy), when PvP is switched off (a player), when he loses the power, and when the server stops. A player's
 * {@link Live#MIND_CONTROLLED} is cleared on every one of those paths, and once a tick a player who carries it with
 * no link that names them is freed ({@link #watchdog}): nobody is ever left stuck.
 *
 * <p>Whose mind is open is the rule book's ({@link Telepathy}): a telepath of his rank keeps him out, and a stronger
 * one (the Scarlet Witch, the Phoenix, Billy) throws him out ({@link #backlash}).
 */
public final class MindLink {
    /** The speed (blocks per tick) a steered player walks at, and sneaks at. A player's own walk is about 0.22. */
    static final double PLAYER_WALK = 0.2;
    static final double PLAYER_SNEAK = 0.065;
    /** The vertical speed of a jump, and gravity, as the vanilla player has them. */
    static final double JUMP = 0.42;
    static final double GRAVITY = 0.08;
    /** The fewest ticks between two attacks of a body he steers. */
    static final int ATTACK_INTERVAL = 10;
    /** How far a steered body reaches with a blow, and how far a steered archer aims. */
    static final double REACH = 3.5;
    static final double RANGED_REACH = 32.0;

    /** Why a link ended (the message he is told, and whether the body wakes confused). */
    public enum End {
        LET_GO, TIME, DISTANCE, HOST_GONE, XAVIER_GONE, HURT, CLOSED, PVP_OFF, POWER_LOST, SERVER
    }

    /** One link: Professor X, the body he steers, and the wish of his last message. */
    static final class Link {
        final UUID xavier;
        final UUID host;
        final boolean player;
        final ResourceKey<Level> dimension;
        final long until;
        float forward;
        float strafe;
        boolean jump;
        boolean sneak;
        float yaw;
        float pitch;
        boolean attack;
        long lastAttack = Long.MIN_VALUE / 2;
        /** A steered player's vertical speed, as the server reckons it (see {@link #steerPlayer}). */
        double vy;
        boolean toldNoAttack;

        Link(UUID xavier, UUID host, boolean player, ResourceKey<Level> dimension, long until, float yaw, float pitch) {
            this.xavier = xavier;
            this.host = host;
            this.player = player;
            this.dimension = dimension;
            this.until = until;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final Map<UUID, Link> BY_XAVIER = new HashMap<>();
    private static final Map<UUID, Link> BY_HOST = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Queries

    /** Whether this player's mind is in another body right now. */
    public static boolean isLinked(Entity xavier) {
        return BY_XAVIER.containsKey(xavier.getUUID());
    }

    /** Whether Professor X steers this body right now. */
    public static boolean isHost(Entity entity) {
        return BY_HOST.containsKey(entity.getUUID());
    }

    /** The body this Professor X steers, if it is loaded, else null. */
    @Nullable
    public static Entity host(ServerPlayer xavier) {
        Link link = BY_XAVIER.get(xavier.getUUID());
        return link == null ? null : find(xavier.server, link.host);
    }

    /** The Professor X who steers this body, if he is online, else null. */
    @Nullable
    public static ServerPlayer controller(Entity host) {
        Link link = BY_HOST.get(host.getUUID());
        return link == null || host.getServer() == null ? null : host.getServer().getPlayerList().getPlayer(link.xavier);
    }

    /** The ticks his link has left, or 0. */
    public static long ticksLeft(Player xavier, long now) {
        Link link = BY_XAVIER.get(xavier.getUUID());
        return link == null ? 0 : Math.max(0, link.until - now);
    }

    /** How many links there are (for the tests). */
    public static int count() {
        return BY_XAVIER.size();
    }

    @Nullable
    private static Entity find(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            return player;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Taking a body

    /**
     * Why this body cannot be taken (a lang key), or null if it can. A stronger telepath is not refused here: {@link
     * #take} throws him out instead.
     */
    @Nullable
    static String refusal(ServerPlayer xavier, LivingEntity target) {
        if (target == xavier || !target.isAlive() || target instanceof ArmorStand || target instanceof LokiIllusionEntity
                || target.level() != xavier.level()) {
            return "message.enhanced.xavier.link.cannot";
        }
        if (isHost(target) || PhoenixAbilities.isHost(target)) {
            return "message.enhanced.xavier.link.taken";
        }
        Telepathy.Verdict verdict = Telepathy.reach(xavier, target);
        if (verdict == Telepathy.Verdict.SHIELDED) {
            return "message.enhanced.xavier.shielded";
        }
        if (verdict == Telepathy.Verdict.TELEPATH) {
            return "message.enhanced.xavier.telepath";
        }
        if (verdict == Telepathy.Verdict.THROWS_OUT) {
            return null;
        }
        if (MjolnirPin.isPinned(target)) {
            return "message.enhanced.xavier.link.pinned";
        }
        if (target instanceof Player other) {
            if (other.isCreative() || other.isSpectator() || RoadTrials.isInside(other)) {
                return "message.enhanced.xavier.link.cannot";
            }
            if (!xavier.server.isPvpAllowed() || !xavier.canHarmPlayer(other)) {
                return "message.enhanced.xavier.no_pvp";
            }
            return XavierConfig.LINK_PLAYER_TICKS.get() > 0 ? null : "message.enhanced.xavier.link.players_off";
        }
        if (!(target instanceof Mob)) {
            return "message.enhanced.xavier.link.cannot";
        }
        if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null && !xavier.getUUID().equals(pet.getOwnerUUID())
                && !xavier.server.isPvpAllowed()) {
            // Nobody turns a stranger's wolf on its owner with PvP off.
            return "message.enhanced.xavier.link.cannot";
        }
        return null;
    }

    /**
     * Reaches for the mind of {@code target}: takes the body, or (a stronger telepath) is thrown out, or is told why not.
     *
     * @return true if he is now in the body
     */
    static boolean take(ServerPlayer xavier, LivingEntity target) {
        if (isLinked(xavier)) {
            return false;
        }
        String refusal = refusal(xavier, target);
        if (refusal != null) {
            xavier.displayClientMessage(Component.translatable(refusal), true);
            return false;
        }
        if (Telepathy.reach(xavier, target) == Telepathy.Verdict.THROWS_OUT) {
            backlash(xavier, target);
            return false;
        }
        ServerLevel level = xavier.serverLevel();
        long now = level.getGameTime();
        boolean player = target instanceof Player;
        int ticks = player ? XavierConfig.LINK_PLAYER_TICKS.get() : XavierConfig.LINK_CREATURE_TICKS.get();
        Link link = new Link(xavier.getUUID(), target.getUUID(), player, level.dimension(), now + ticks, xavier.getYRot(), xavier.getXRot());
        BY_XAVIER.put(link.xavier, link);
        BY_HOST.put(link.host, link);
        Powers.get(xavier).set(Live.MIND_LINKED, true);
        if (target instanceof ServerPlayer host) {
            host.stopUsingItem();
            Powers.get(host).set(Live.MIND_CONTROLLED, true);
            Powers.syncIfDirty(host);
            host.displayClientMessage(Component.translatable("message.enhanced.xavier.controlled", xavier.getDisplayName()), true);
        } else if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            willOff(mob);
        }
        Powers.syncIfDirty(xavier);
        xavier.connection.send(new XavierPayload(XavierPayload.LINK_START, target.getId(), ticks));
        xavier.displayClientMessage(Component.translatable("message.enhanced.xavier.link.in", target.getDisplayName()), true);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.XAVIER_LINK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        stream(level, xavier, target);
        return true;
    }

    /**
     * A stronger telepath throws him out: he takes {@code backlashDamage} (the Mind Blast, from them), is dizzy and
     * slowed for {@code backlashStunTicks}, is pushed back, and V rests. They are told they threw him out.
     */
    static void backlash(ServerPlayer xavier, LivingEntity telepath) {
        ServerLevel level = xavier.serverLevel();
        int stun = XavierConfig.BACKLASH_STUN_TICKS.get();
        float damage = XavierConfig.BACKLASH_DAMAGE.get().floatValue();
        if (damage > 0.0F) {
            xavier.hurt(ModDamageTypes.source(level, ModDamageTypes.MIND_BLAST, telepath, telepath), damage);
        }
        if (stun > 0) {
            xavier.addEffect(new MobEffectInstance(MobEffects.CONFUSION, stun + 40, 0, false, true, true), telepath);
            xavier.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 3, false, true, true), telepath);
            xavier.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 1, false, true, true), telepath);
        }
        Vec3 away = xavier.position().subtract(telepath.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize();
            xavier.setDeltaMovement(away.x * 0.8, 0.35, away.z * 0.8);
            xavier.hurtMarked = true;
        }
        PowerData data = Powers.get(xavier);
        data.startCooldown(HeroAbility.XAVIER_TAKE_OVER, level.getGameTime(), XavierConfig.LINK_COOLDOWN.get());
        Powers.syncIfDirty(xavier);
        xavier.connection.send(XavierPayload.of(XavierPayload.THROWN_OUT));
        xavier.displayClientMessage(Component.translatable("message.enhanced.xavier.thrown_out", telepath.getDisplayName()), true);
        if (telepath instanceof ServerPlayer other) {
            other.displayClientMessage(Component.translatable("message.enhanced.xavier.threw_out", xavier.getDisplayName()), true);
        }
        level.playSound(null, xavier.getX(), xavier.getY(), xavier.getZ(), ModSounds.XAVIER_BACKLASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(new EnergyParticleOptions(0xFF4D6A, 1.2F), xavier.getX(), xavier.getEyeY(), xavier.getZ(), 24, 0.3, 0.3, 0.3, 0.15);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Leaving a body

    /** Ends the link of this Professor X (whoever he is, online or not), for this reason. Nothing if he has none. */
    public static void end(MinecraftServer server, UUID xavierId, End why) {
        Link link = BY_XAVIER.remove(xavierId);
        if (link == null) {
            return;
        }
        BY_HOST.remove(link.host);
        Entity host = find(server, link.host);
        ServerPlayer xavier = server.getPlayerList().getPlayer(xavierId);
        if (host instanceof ServerPlayer player) {
            PowerData data = Powers.get(player);
            data.set(Live.MIND_CONTROLLED, false);
            Powers.syncIfDirty(player);
            if (player.isAlive()) {
                player.setDeltaMovement(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0);
                player.hurtMarked = true;
                player.displayClientMessage(Component.translatable("message.enhanced.xavier.freed"), true);
            }
        } else if (host instanceof Mob mob) {
            willOn(mob);
            if (mob.isAlive() && why != End.SERVER) {
                // He leaves the body as the Phoenix leaves a host: it wakes confused for a moment.
                mob.addEffect(new MobEffectInstance(ModEffects.CONFUSED, PhoenixAbilities.FREED_CONFUSED_TICKS, 0, false, true, true));
            }
        }
        if (xavier != null) {
            PowerData data = Powers.get(xavier);
            data.set(Live.MIND_LINKED, false);
            if (data.has(HeroAbility.XAVIER_TAKE_OVER)) {
                data.startCooldown(HeroAbility.XAVIER_TAKE_OVER, xavier.serverLevel().getGameTime(), XavierConfig.LINK_COOLDOWN.get());
            }
            Powers.syncIfDirty(xavier);
            xavier.connection.send(XavierPayload.of(XavierPayload.LINK_END));
            if (why != End.SERVER && why != End.XAVIER_GONE) {
                xavier.displayClientMessage(Component.translatable("message.enhanced.xavier.link.out." + why.name().toLowerCase(java.util.Locale.ROOT)), true);
            }
            if (xavier.isAlive()) {
                xavier.serverLevel().playSound(null, xavier.getX(), xavier.getY(), xavier.getZ(), ModSounds.XAVIER_UNLINK.get(),
                        SoundSource.PLAYERS, 0.9F, 1.0F);
            }
        }
    }

    /** A player left, died or changed dimension: whichever side of a link they were on, it ends. */
    public static void onGone(ServerPlayer player) {
        if (isLinked(player)) {
            end(player.server, player.getUUID(), End.XAVIER_GONE);
        }
        Link asHost = BY_HOST.get(player.getUUID());
        if (asHost != null) {
            end(player.server, asHost.xavier, End.HOST_GONE);
        }
        PowerData data = Powers.get(player);
        data.set(Live.MIND_LINKED, false);
        data.set(Live.MIND_CONTROLLED, false);
    }

    /** A creature died: if it was a body he steered, he comes back. */
    public static void onDeath(LivingEntity entity) {
        Link link = BY_HOST.get(entity.getUUID());
        if (link != null && entity.getServer() != null) {
            end(entity.getServer(), link.xavier, End.HOST_GONE);
        }
    }

    /** The server is stopping: every link ends, every body is given back its will. */
    public static void endAll(MinecraftServer server) {
        for (UUID xavier : new ArrayList<>(BY_XAVIER.keySet())) {
            end(server, xavier, End.SERVER);
        }
    }

    /** Forget every link (the server stopped). */
    public static void clear() {
        BY_XAVIER.clear();
        BY_HOST.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every tick

    /**
     * His wish, from his client: kept for the next tick. Dropped unless he steers a body; every number is clamped, and
     * one that is not a number keeps the old value.
     */
    public static void input(ServerPlayer xavier, XavierInputPayload p) {
        Link link = BY_XAVIER.get(xavier.getUUID());
        if (link == null) {
            return;
        }
        link.forward = clampUnit(p.forward());
        link.strafe = clampUnit(p.strafe());
        link.jump = p.jump();
        link.sneak = p.sneak();
        if (Float.isFinite(p.yaw())) {
            link.yaw = Mth.wrapDegrees(p.yaw());
        }
        if (Float.isFinite(p.pitch())) {
            link.pitch = Mth.clamp(p.pitch(), -90.0F, 90.0F);
        }
        link.attack |= p.attack();
    }

    private static float clampUnit(float v) {
        return Float.isFinite(v) ? Mth.clamp(v, -1.0F, 1.0F) : 0.0F;
    }

    /**
     * His tick (alive): the link is checked against every rule that ends it, then the body is steered (a player) and
     * strikes (either kind).
     */
    static void tick(ServerPlayer xavier) {
        Link link = BY_XAVIER.get(xavier.getUUID());
        if (link == null) {
            return;
        }
        End why = check(xavier, link);
        if (why != null) {
            end(xavier.server, xavier.getUUID(), why);
            return;
        }
        Entity host = find(xavier.server, link.host);
        if (host instanceof ServerPlayer player) {
            steerPlayer(player, link);
        }
        if (link.attack) {
            link.attack = false;
            strike(xavier, (LivingEntity) host, link);
        }
    }

    /** Why the link must end now, or null. */
    @Nullable
    private static End check(ServerPlayer xavier, Link link) {
        if (!Powers.get(xavier).has(HeroAbility.XAVIER_TAKE_OVER)) {
            return End.POWER_LOST; // (3.4: or a custom hero who no longer has the ability)
        }
        if (xavier.serverLevel().getGameTime() >= link.until) {
            return End.TIME;
        }
        Entity host = find(xavier.server, link.host);
        if (!(host instanceof LivingEntity living) || !living.isAlive() || host.isRemoved()) {
            return End.HOST_GONE;
        }
        if (host.level() != xavier.level() || !host.level().dimension().equals(link.dimension)) {
            return End.DISTANCE;
        }
        double max = XavierConfig.LINK_MAX_DISTANCE.get();
        if (host.distanceToSqr(xavier) > max * max) {
            return End.DISTANCE;
        }
        if (Telepathy.reach(xavier, host) != Telepathy.Verdict.OPEN) {
            return End.CLOSED;
        }
        if (host instanceof Player player) {
            if (player.isCreative() || player.isSpectator() || RoadTrials.isInside(player)) {
                return End.HOST_GONE;
            }
            if (!xavier.server.isPvpAllowed() || !xavier.canHarmPlayer(player)) {
                return End.PVP_OFF;
            }
        }
        return null;
    }

    /** Where his keys ask the body to go: a unit (or shorter) vector on the ground, turned by where he looks. */
    static Vec3 wish(float forward, float strafe, float yaw) {
        double len = Math.sqrt(forward * forward + strafe * strafe);
        if (len < 1.0E-3) {
            return Vec3.ZERO;
        }
        double f = forward / Math.max(1.0, len);
        double s = strafe / Math.max(1.0, len);
        float sin = Mth.sin(yaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(yaw * Mth.DEG_TO_RAD);
        return new Vec3(s * cos - f * sin, 0.0, f * cos + s * sin);
    }

    /** The same, up and down too, for a body that flies or swims: forward is along the look. */
    static Vec3 wish3d(float forward, float strafe, float yaw, float pitch) {
        Vec3 flat = wish(0.0F, strafe, yaw);
        Vec3 look = Vec3.directionFromRotation(pitch, yaw).scale(forward);
        Vec3 v = flat.add(look);
        return v.lengthSqr() > 1.0 ? v.normalize() : v;
    }

    /** Whether a creature moves up and down by itself (it flies or swims): it follows his look in three dimensions. */
    static boolean free3d(Mob mob) {
        return mob instanceof FlyingMob || mob instanceof FlyingAnimal || mob instanceof WaterAnimal || mob.isNoGravity();
    }

    /**
     * The body's will is switched off: the goals that move it, turn its head, make it jump and choose its targets do not
     * run (the flags are set again every tick: {@code Mob.tick} turns three of them back on every five ticks).
     */
    private static void willOff(Mob mob) {
        mob.goalSelector.disableControlFlag(Goal.Flag.MOVE);
        mob.goalSelector.disableControlFlag(Goal.Flag.LOOK);
        mob.goalSelector.disableControlFlag(Goal.Flag.JUMP);
        mob.targetSelector.disableControlFlag(Goal.Flag.TARGET);
    }

    /** And on again (the moving, looking and jumping flags as {@code Mob.updateControlFlags} would have them). */
    private static void willOn(Mob mob) {
        mob.goalSelector.enableControlFlag(Goal.Flag.MOVE);
        mob.goalSelector.enableControlFlag(Goal.Flag.LOOK);
        mob.goalSelector.enableControlFlag(Goal.Flag.JUMP);
        mob.targetSelector.enableControlFlag(Goal.Flag.TARGET);
    }

    /**
     * A creature's tick is about to run ({@code EntityTickEvent.Pre}): if he steers it, its will stays off, its brain
     * forgets where it meant to walk and whom to hit, and its controls are given his wish: walk (or fly, or swim) where
     * his keys say at its own speed (half when he sneaks), jump, look where he looks.
     */
    public static void steerMob(Mob mob) {
        if (BY_HOST.isEmpty() || mob.level().isClientSide) {
            return;
        }
        Link link = BY_HOST.get(mob.getUUID());
        if (link == null || link.player) {
            return;
        }
        willOff(mob);
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        double speed = link.sneak ? 0.5 : 1.0;
        boolean free = free3d(mob);
        Vec3 dir = free ? wish3d(link.forward, link.strafe, link.yaw, link.pitch) : wish(link.forward, link.strafe, link.yaw);
        if (dir.lengthSqr() > 1.0E-4) {
            Vec3 to = mob.position().add(dir.scale(3.0));
            mob.getMoveControl().setWantedPosition(to.x, free ? to.y : mob.getY(), to.z, speed);
        }
        if (link.jump && (mob.onGround() || mob.isInWater())) {
            mob.getJumpControl().jump();
        }
        Vec3 eye = mob.getEyePosition();
        Vec3 look = eye.add(Vec3.directionFromRotation(link.pitch, link.yaw).scale(8.0));
        mob.getLookControl().setLookAt(look.x, look.y, look.z, 30.0F, 40.0F);
    }

    /**
     * A steered player's body: the velocity his keys ask for (walking speed, slower when he sneaks, scaled by the body's
     * own speed), and a vertical speed the server reckons itself from the vanilla jump and gravity (their client's
     * position reports would lag a tick behind), sent as a motion packet that their own client carries out. A body in
     * the air in Creative-like flight follows his look up and down.
     */
    static void steerPlayer(ServerPlayer host, Link link) {
        double scale = host.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1;
        double speed = (link.sneak ? PLAYER_SNEAK : PLAYER_WALK) * Mth.clamp(scale, 0.0, 3.0);
        Vec3 motion;
        if (host.getAbilities().flying) {
            Vec3 dir = wish3d(link.forward, link.strafe, link.yaw, link.pitch);
            double up = (link.jump ? 0.15 : 0.0) - (link.sneak ? 0.15 : 0.0);
            link.vy = 0.0;
            motion = new Vec3(dir.x * speed * 1.5, dir.y * speed * 1.5 + up, dir.z * speed * 1.5);
        } else {
            Vec3 dir = wish(link.forward, link.strafe, link.yaw);
            if (host.isInWater() || host.isInLava()) {
                link.vy = link.jump ? 0.08 : -0.03;
            } else if (link.jump && host.onGround() && link.vy <= 0.0) {
                link.vy = JUMP * host.getAttributeValue(Attributes.JUMP_STRENGTH) / 0.42;
            } else if (host.onGround() && link.vy <= 0.0) {
                link.vy = 0.0;
            } else {
                link.vy = Math.max(-3.92, (link.vy - GRAVITY) * 0.98);
            }
            motion = new Vec3(dir.x * speed, link.vy, dir.z * speed);
        }
        host.setDeltaMovement(motion);
        host.hurtMarked = true;
        host.resetFallDistance();
        host.connection.aboveGroundTickCount = 0;
    }

    /**
     * He clicked: the body strikes what he looks at, from its own eyes. A player swings what they hold; a creeper lights
     * its fuse; an archer (any creature that shoots) shoots at what he looks at; any other creature with a blow of its own
     * hits; one with none cannot fight (he is told once). A creature never hurts a player he could not hurt himself (PvP
     * off), and the body never strikes his own.
     */
    private static void strike(ServerPlayer xavier, LivingEntity body, Link link) {
        long now = xavier.serverLevel().getGameTime();
        if (body == null || now - link.lastAttack < ATTACK_INTERVAL) {
            return;
        }
        Vec3 eye = body.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(link.pitch, link.yaw);
        if (body instanceof Creeper creeper) {
            link.lastAttack = now;
            creeper.ignite();
            return;
        }
        boolean ranged = body instanceof RangedAttackMob;
        double reach = ranged ? RANGED_REACH : REACH + body.getBbWidth() * 0.5;
        EntityHitResult hit = Raycasts.entityBetween(body.level(), body, eye, eye.add(look.scale(reach)), 0.3,
                e -> e instanceof LivingEntity living && living != body && living != xavier && living.isAlive() && !living.isSpectator()
                        && !(living instanceof ArmorStand));
        LivingEntity target = hit == null ? null : (LivingEntity) hit.getEntity();
        if (target instanceof Player other && (!xavier.server.isPvpAllowed() || !xavier.canHarmPlayer(other))) {
            xavier.displayClientMessage(Component.translatable("message.enhanced.xavier.no_pvp"), true);
            return;
        }
        link.lastAttack = now;
        if (body instanceof ServerPlayer player) {
            player.swing(InteractionHand.MAIN_HAND, true);
            if (target != null) {
                player.attack(target);
            }
            return;
        }
        Mob mob = (Mob) body;
        if (ranged) {
            if (target != null) {
                mob.setYRot(link.yaw);
                mob.setYHeadRot(link.yaw);
                ((RangedAttackMob) mob).performRangedAttack(target, 1.0F);
                mob.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) == null) {
            if (!link.toldNoAttack) {
                link.toldNoAttack = true;
                xavier.displayClientMessage(Component.translatable("message.enhanced.xavier.link.no_attack", mob.getDisplayName()), true);
            }
            return;
        }
        mob.swing(InteractionHand.MAIN_HAND);
        if (target != null) {
            mob.doHurtTarget(target);
        }
    }

    /**
     * Every player, every tick: a player who carries {@link Live#MIND_CONTROLLED} with no link that names them, or {@link
     * Live#MIND_LINKED} with no link of their own, is freed of it (whatever went wrong, nobody stays stuck).
     */
    static void watchdog(ServerPlayer player, PowerData data) {
        if (data.is(Live.MIND_CONTROLLED) && !isHost(player)) {
            data.set(Live.MIND_CONTROLLED, false);
        }
        if (data.is(Live.MIND_LINKED) && !isLinked(player)) {
            data.set(Live.MIND_LINKED, false);
        }
    }

    /** A thin stream of blue-white motes from his temple to the body he enters. */
    static void stream(ServerLevel level, ServerPlayer from, LivingEntity to) {
        Vec3 start = from.getEyePosition().add(from.getLookAngle().scale(0.3));
        Vec3 end = to.getEyePosition();
        Vec3 delta = end.subtract(start);
        int steps = (int) Math.min(40, delta.length() * 2.5);
        for (int i = 1; i <= steps; i++) {
            Vec3 p = start.add(delta.scale(i / (double) steps));
            int colour = level.random.nextInt(3) == 0 ? XavierAbilities.CORE : XavierAbilities.MAIN;
            level.sendParticles(new EnergyParticleOptions(colour, 0.7F), p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    private MindLink() {
    }
}
