package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.ThanosMeteorEntity;
import com.kolodkingal.enhanced.thanos.ThanosOrbEntity;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Lifting, carrying and launching things with the mind. Shared by Magneto (metal only: metal things, metal projectiles in
 * flight and metal blocks), Wanda (anything, including blocks), Billy (anything loose, including projectiles in flight:
 * he stops bullets in WandaVision; and blocks, within his own limits) and Jean Grey (all of that: blocks and projectiles
 * too). The boss of a fight, and the Wither, the Warden and the Elder Guardian, are taken only as bosses, under {@link
 * BossGrip}'s rules; whatever a press may not take, it says why.
 *
 * <p>While held, the target is moved every tick towards the point the holder is looking at; what a boss fight owns, and
 * its fighters, are never carried out of its arena ({@link BossFights#carryLimit}), and a boss tears free at the end of
 * his hold ({@link BossGrip#holdLimit}). Launched targets are tracked until they slam into something: they hurt every
 * creature they crash into and take heavy impact damage when they hit a wall or the floor; a throw of anything of a fight
 * stops dead at a gated arena's edge. A launched projectile flies again, where the holder looks, and is the holder's: a
 * boss fight's shot caught in flight (it stays still in the grip) leaves the fight when it is thrown, and Thanos's sword,
 * meteor and orb turn on him ({@link BossHooks#throwBack}).
 *
 * <p>Whoever a grip lets go of that its holder may not hurt (a player with PvP off, a teammate, another player's pet) is
 * spared the fall, the wall and the slam until it lands ({@link #isSpared}, the landing guard of {@code
 * TelekinesisEvents}), and a holder does not grip the same player again for {@code
 * magneto.telekinesisPlayerRegripTicks}.
 */
public final class Telekinesis {

    public enum Mode {
        MAGNETO(Power.MAGNETO),
        WANDA(Power.SCARLET_WITCH),
        BILLY(Power.BILLY),
        /** Jean Grey: Wanda's telekinesis, in the violet of her mind. */
        JEAN(Power.PHOENIX, PhoenixAbilities.PSY_MAIN);

        final Power power;
        /** The colour of the particles, the tether and the block it lifts. */
        public final int color;

        Mode(Power power) {
            this(power, power.color);
        }

        Mode(Power power, int color) {
            this.power = power;
            this.color = color;
        }
    }

    /** Max time a player can be held, so nobody is trapped forever. */
    public static final int MAX_PLAYER_HOLD_TICKS = 600;
    private static final double BREAK_DISTANCE = 48.0;
    private static final double MIN_HOLD_DISTANCE = 3.0;
    private static final double MAX_HOLD_DISTANCE = 12.0;
    /** A held projectile hangs weightless; this remembers whether it was weightless before (to restore it). */
    private static final String HELD_PROJECTILE_TAG = "enhanced:telekinesis_held";

    private static final class Grip {
        final UUID target;
        final Mode mode;
        final double distance;
        /** How many ticks the target may be held before it tears free ({@link BossGrip#holdLimit}); 0: no limit. */
        final int limit;
        int ticks;

        Grip(UUID target, Mode mode, double distance, int limit) {
            this.target = target;
            this.mode = mode;
            this.distance = distance;
            this.limit = limit;
        }
    }

    private static final class Flight {
        final UUID thrower;
        final ResourceKey<Level> dimension;
        /** It hits like a wall: 15 to every creature it crashes into (an Iron Golem, a boss). */
        final boolean heavyHit;
        /** It takes the impact itself when it slams into something (everything but an Iron Golem). */
        final boolean takesImpact;
        final int color;
        final IntSet hit = new IntOpenHashSet();
        Vec3 lastPos;
        double lastSpeed;
        int ticks;

        Flight(UUID thrower, ResourceKey<Level> dimension, Vec3 pos, double speed, boolean heavyHit, boolean takesImpact, int color) {
            this.thrower = thrower;
            this.dimension = dimension;
            this.lastPos = pos;
            this.lastSpeed = speed;
            this.heavyHit = heavyHit;
            this.takesImpact = takesImpact;
            this.color = color;
        }
    }

    /** One entity spared the landing (see {@link #isSpared}): where it is, and when the grip on it ended. */
    private record Spared(ResourceKey<Level> dimension, long since) {
    }

    /** A spared entity's mark is dropped once it has landed, at least this many ticks after the grip ended. */
    private static final int SPARED_MIN_TICKS = 2;
    /** ... and at the latest this many ticks after it. */
    private static final int SPARED_MAX_TICKS = 200;

    /** Holder UUID → what they hold. */
    private static final Map<UUID, Grip> GRIPS = new HashMap<>();
    /** Thrown entity UUID → its flight. */
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();
    /** Holder UUID → the game time their last grip ended, however it ended. */
    private static final Map<UUID, Long> GRIP_ENDS = new HashMap<>();
    /** Holder UUID → (player UUID → the game time a grip of that holder on that player ended), for the player regrip. */
    private static final Map<UUID, Map<UUID, Long>> PLAYERS_LET_GO = new HashMap<>();
    /** Entity UUID → its landing guard: what a grip let go of that its holder may not hurt, until it lands. */
    private static final Map<UUID, Spared> SPARED = new HashMap<>();
    /** What {@link #lastGripEnd} answers for a holder whose grip never ended: long enough ago for any wait. */
    public static final long NEVER = Long.MIN_VALUE / 2;

    // ------------------------------------------------------------------------------------------------------------
    // Input

    /**
     * The telekinesis key (spec 5.2), in this order:
     * <ol start="0">
     *     <li>a Spectator's press does nothing (the client never sends it, but a crafted packet could);</li>
     *     <li>holding something: launch it, or with Sneak put it down gently;</li>
     *     <li>a hero the Sokovia lift carries uses no telekinesis ({@code telekinesis.carried});</li>
     *     <li>Sneak with nothing held: Magneto's Magnetic Pull, which goes on as R if it declines the press; for Wanda, Billy
     *     and the Phoenix the block they look at and nothing else ({@link #pressForABlock});</li>
     *     <li>a boss's own use of Wanda's hand (Ultron's heart);</li>
     *     <li>the nearest thing in the line of sight that the form could take: held, or the holder is told why not
     *     ({@link #refusal}) and nothing behind it is taken;</li>
     *     <li>a projectile in flight, aimed at more generously (Billy, the Phoenix, Magneto for metal);</li>
     *     <li>a creature Mjolnir pins: said so;</li>
     *     <li>the block looked at (Magneto: a metal block; Billy while {@code billy.blockLifting});</li>
     *     <li>"No metal in sight" (Magneto) or "Nothing to lift".</li>
     * </ol>
     */
    public static void press(ServerPlayer player, Mode mode) {
        if (player.isSpectator()) {
            return;
        }
        Grip grip = GRIPS.get(player.getUUID());
        if (grip != null) {
            if (player.isShiftKeyDown()) {
                release(player, true);
            } else {
                launch(player);
            }
            return;
        }
        if (BossFights.isLocked(player)) {
            // The island carries them: no telekinesis now, not even the pull.
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.carried"), true);
            return;
        }
        if (player.isShiftKeyDown()) {
            if (mode != Mode.MAGNETO) {
                pressForABlock(player, mode);
                return;
            }
            if (MagnetPull.press(player)) {
                return; // Sneak + R with nothing held: the Magnetic Pull
            }
        }
        if (mode == Mode.WANDA && BossFights.telekinesisPress(player)) {
            return; // a boss has its own use for her hand (Ultron's heart)
        }

        double range = EnhancedConfig.TELEKINESIS_RANGE.get();
        // One ray, nearest first, for what the form could take whether or not it may: a refused boss (or a creature someone
        // else holds) answers the press, and nothing standing behind it is taken instead (D-T 4.6).
        EntityHitResult hit = Raycasts.entity(player, range, 0.3, e -> canLift(player, e, mode) || refusal(player, e, mode) != null, 1.0F);
        if (hit != null) {
            Entity target = hit.getEntity();
            String refused = refusal(player, target, mode);
            if (refused == null) {
                hold(player, target, mode);
                return;
            }
            player.displayClientMessage(refusalLine(refused, target), true);
            if (target instanceof BossHooks hooks) {
                hooks.onGripRefused(player, refused); // Apocalypse's ripple, once per press
            }
            return;
        }
        if (mode == Mode.BILLY || mode == Mode.JEAN || mode == Mode.MAGNETO) {
            // Arrows are small and fast: aim a little more generously at them (Magneto: at the metal ones).
            hit = Raycasts.entity(player, range, 1.0, e -> e instanceof Projectile && canLift(player, e, mode), 1.0F);
            if (hit != null) {
                hold(player, hit.getEntity(), mode);
                return;
            }
        }
        if (Raycasts.entity(player, range, 0.3, MjolnirPin::isPinned, 1.0F) != null) {
            // What they aim at is there, but Mjolnir holds it down: say so, rather than that there is nothing to lift.
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.pinned"), true);
            return;
        }
        if (liftsBlocks(mode) && BlockLifting.tryLift(player, mode)) {
            return;
        }
        player.displayClientMessage(Component.translatable(mode == Mode.MAGNETO
                ? "message.enhanced.telekinesis.no_metal" : "message.enhanced.telekinesis.nothing"), true);
    }

    /**
     * The action-bar line of a refusal: a boss's key names the boss, Vision's names the power (never the player, so the
     * Hebrew verb is right whoever plays him).
     */
    private static Component refusalLine(String key, Entity target) {
        if (key.equals(PHASED)) {
            return Component.translatable(key, Component.translatable("power.enhanced.vision"));
        }
        if (key.startsWith("message.enhanced.telekinesis.boss_")) {
            return Component.translatable(key, target.getName());
        }
        return Component.translatable(key);
    }

    /**
     * Sneak + V with nothing held (Wanda, Billy, the Phoenix): the block they look at and nothing else, even past a
     * creature, an item or a boss in front of it; a boss's own use of Wanda's hand (Ultron's heart) is never asked. For
     * {@code magneto.telekinesisSneakGraceTicks} after one of their grips ended it does nothing at all, so a press meant to
     * set down what had just slipped away tears nothing out of the ground (R41).
     */
    private static void pressForABlock(ServerPlayer player, Mode mode) {
        if (player.serverLevel().getGameTime() - lastGripEnd(player) < EnhancedConfig.TELEKINESIS_SNEAK_GRACE_TICKS.get()) {
            return;
        }
        if (liftsBlocks(mode) && BlockLifting.tryLift(player, mode)) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.nothing"), true);
    }

    /**
     * Whether this telekinesis tears blocks out of the ground: Wanda's and the Phoenix's, Billy's while {@code
     * billy.blockLifting}, and Magneto's for metal blocks ({@link BlockLifting#tryLift}).
     */
    private static boolean liftsBlocks(Mode mode) {
        return mode != Mode.BILLY || EnhancedConfig.BILLY_BLOCK_LIFTING.get();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Targets

    public static final String DRAGON = "message.enhanced.telekinesis.dragon";
    public static final String PHASED = "message.enhanced.telekinesis.phased";
    public static final String MJOLNIR = "message.enhanced.telekinesis.mjolnir";
    public static final String HELD_BY_OTHER = "message.enhanced.telekinesis.held_by_other";
    public static final String PLAYER_REGRIP = "message.enhanced.telekinesis.player_regrip";
    public static final String SLIPPED = "message.enhanced.telekinesis.slipped";

    /**
     * Whether the holder's telekinesis in {@code mode} takes hold of {@code e} now (spec 5.2), false if any of these, in
     * order:
     * <ol>
     *     <li>it is the holder himself, dead, a spectator, or already held;</li>
     *     <li>no telekinesis lifts its kind ({@code enhanced:telekinesis_immune}), unless Magneto's metal does ({@code
     *     enhanced:magnetic_entities}: the Hex's Vision);</li>
     *     <li>it rides the holder or he rides it; a Creative or Spectator player; a creature Mjolnir pins; an item no power
     *     can snatch (Mjolnir);</li>
     *     <li>a player this holder let go of less than {@code magneto.telekinesisPlayerRegripTicks} ago;</li>
     *     <li>Thanos's sword, meteor or orb while {@code boss.telekinesisCatchesBossShots} is off;</li>
     *     <li>{@link BossGrip#refusal} has a reason: the lift carries it, a boss's switch is off, his own state, his regrip
     *     time;</li>
     *     <li>the form does not take its kind ({@link #takes}).</li>
     * </ol>
     */
    public static boolean canLift(Player holder, Entity e, Mode mode) {
        if (e == holder || !e.isAlive() || e.isSpectator() || isHeld(e)) {
            return false;
        }
        if (e.getType().is(ModTags.TELEKINESIS_IMMUNE) && !(mode == Mode.MAGNETO && e.getType().is(ModTags.MAGNETIC_ENTITIES))) {
            return false;
        }
        if (e.hasPassenger(holder) || holder.hasPassenger(e)) {
            return false;
        }
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        if (MjolnirPin.isPinned(e)) {
            // Mjolnir lies on them: no power but its owner's can move them, any more than it can lift the hammer.
            return false;
        }
        if (e instanceof ItemEntity item && item.getItem().is(ModTags.CANNOT_BE_SNATCHED)) {
            // Mjolnir: no power can lift it, only its owner's own hands.
            return false;
        }
        if (e instanceof Player target && playerRegripLeft(holder, target) > 0) {
            return false;
        }
        if (isBossShot(e) && !BossConfig.TELEKINESIS_CATCHES_BOSS_SHOTS.get()) {
            return false;
        }
        if (awaitsBossGrip(e) || BossGrip.refusal(holder, e, mode) != null) {
            return false;
        }
        return takes(e, mode);
    }

    /**
     * Interim rule of lane TK: what left {@code telekinesis_immune} in 3.1 and whose grip is not written yet is not held,
     * as in 3.0: the Wither, the Warden and the Elder Guardian until a vanilla boss's {@code NoAI} is kept while he is held
     * (step 10). {@link #refusal} already names what refuses them meanwhile (a switch off, an invulnerable state).
     */
    private static boolean awaitsBossGrip(Entity e) {
        return BossGrip.isVanillaBoss(e);
    }

    /** The form's own test of what it takes (step 7 of {@link #canLift}): Magneto metal, the others anything loose. */
    private static boolean takes(Entity e, Mode mode) {
        return switch (mode) {
            case MAGNETO -> Metal.isMagnetic(e);
            case WANDA -> isLoose(e);
            case BILLY, JEAN -> isLoose(e) || (e instanceof Projectile projectile && Metal.inFlight(projectile));
        };
    }

    /** Thanos's sword, meteor or orb: the shots of a fight that only {@code boss.telekinesisCatchesBossShots} lets be caught. */
    private static boolean isBossShot(Entity e) {
        return e instanceof ThanosSwordEntity || e instanceof ThanosMeteorEntity || e instanceof ThanosOrbEntity;
    }

    /**
     * Why the holder's telekinesis in {@code mode} may not take hold of {@code e}, which the press tells him rather than
     * reaching past it; null if nothing stands in the way, or if the form could not take {@code e} anyway (Magneto at a
     * zombie with no metal: "No metal in sight"), except for the cases named below. A pure query: it changes, plays and
     * says nothing. The first that applies (spec 5.2):
     * <ol start="0">
     *     <li>the Ender Dragon or one of its parts, for every form: {@code telekinesis.dragon};</li>
     *     <li>Magneto at a phased Vision: {@code telekinesis.phased};</li>
     *     <li>Magneto at a dropped Mjolnir, or at a creature whose only metal is Mjolnir in a hand: {@code
     *     telekinesis.mjolnir};</li>
     *     <li>someone else holds it: {@code telekinesis.held_by_other};</li>
     *     <li>a player this holder let go of less than {@code magneto.telekinesisPlayerRegripTicks} ago: {@code
     *     telekinesis.player_regrip};</li>
     *     <li>{@link BossGrip#refusal}: the lift carries it, a boss's switch is off, his own state, his regrip time.</li>
     * </ol>
     */
    @Nullable
    public static String refusal(Player holder, Entity e, Mode mode) {
        if (e == holder || !e.isAlive() || e.isSpectator()) {
            return null;
        }
        boolean dragon = e instanceof EnderDragon || e instanceof EnderDragonPart;
        boolean phased = mode == Mode.MAGNETO && e instanceof Player player && VisionAbilities.isPhased(player);
        boolean mjolnir = mode == Mode.MAGNETO && answersOnlyToMjolnir(e);
        if (!dragon && !phased && !mjolnir && !takes(e, mode)) {
            return null;
        }
        if (dragon) {
            return DRAGON;
        }
        if (phased) {
            return PHASED;
        }
        if (mjolnir) {
            return MJOLNIR;
        }
        if (isHeld(e) && !holds(holder, e)) {
            return HELD_BY_OTHER;
        }
        if (e instanceof Player target && playerRegripLeft(holder, target) > 0) {
            return PLAYER_REGRIP;
        }
        return BossGrip.refusal(holder, e, mode);
    }

    /** A dropped Mjolnir, or a creature (or player) whose only metal is Mjolnir in one of its hands. */
    private static boolean answersOnlyToMjolnir(Entity e) {
        if (e instanceof ItemEntity item) {
            return ThorAbilities.isMjolnir(item.getItem());
        }
        return e instanceof LivingEntity living && !Metal.isMagnetic(e)
                && (ThorAbilities.isMjolnir(living.getMainHandItem()) || ThorAbilities.isMjolnir(living.getOffhandItem()));
    }

    /**
     * How many ticks are left before the holder may grip {@code target} again: {@code magneto.telekinesisPlayerRegripTicks}
     * after a grip of his on that player ended, however it ended (D-M 3.8). Other holders are not kept waiting.
     */
    public static long playerRegripLeft(Player holder, Player target) {
        Map<UUID, Long> letGo = PLAYERS_LET_GO.get(holder.getUUID());
        Long at = letGo == null ? null : letGo.get(target.getUUID());
        if (at == null) {
            return 0;
        }
        return Math.max(0, at + EnhancedConfig.TELEKINESIS_PLAYER_REGRIP_TICKS.get() - holder.level().getGameTime());
    }

    /** Creatures, dropped items, vehicles, primed TNT, falling (or lifted) blocks: anything not part of the world. */
    private static boolean isLoose(Entity e) {
        return e instanceof LivingEntity || e instanceof ItemEntity || e instanceof TelekineticBlockEntity
                || e instanceof VehicleEntity || e instanceof PrimedTnt || e instanceof FallingBlockEntity;
    }

    /**
     * Whether Magneto's telekinesis can take this entity. The rule is {@link Metal#isMagnetic}, the one every Magneto
     * power asks; this stays for the callers of 3.0.
     */
    public static boolean isMagnetic(Entity e) {
        return Metal.isMagnetic(e);
    }

    public static boolean isHeld(Entity e) {
        UUID id = e.getUUID();
        for (Grip g : GRIPS.values()) {
            if (g.target.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHolding(Player player) {
        return GRIPS.containsKey(player.getUUID());
    }

    /** Whether {@code holder} is the one who holds {@code e}. */
    public static boolean holds(Player holder, Entity e) {
        Grip grip = GRIPS.get(holder.getUUID());
        return grip != null && grip.target.equals(e.getUUID());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Holding

    /**
     * Takes hold of {@code target}. A boss (or a shot of a fight with a hold limit) is held for {@link BossGrip#holdLimit}
     * ticks at most, drops what he was doing, and his holder is told he is in the grip ({@link BossGrip#gripped}).
     */
    public static void hold(ServerPlayer player, Entity target, Mode mode) {
        SandCocoonEntity.freeFrom(target); // taking hold of a friend breaks the cocoon of Apocalypse's sand that held them
        double dist = player.getEyePosition().distanceTo(target.position().add(0, target.getBbHeight() * 0.5, 0));
        GRIPS.put(player.getUUID(), new Grip(target.getUUID(), mode, Mth.clamp(dist, MIN_HOLD_DISTANCE, MAX_HOLD_DISTANCE),
                BossGrip.holdLimit(target, mode)));
        FLIGHTS.remove(target.getUUID());
        Powers.get(player).setHeldEntityId(target.getId());

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            if (mob instanceof IronGolem && mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
        if (target instanceof TelekineticBlockEntity block) {
            block.grab(player, mode.color);
        }
        if (target instanceof Projectile projectile) {
            // Caught: it stops dead and hangs weightless in front of the holder.
            SlowMotion.unslow(projectile);
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.getPersistentData().putBoolean(HELD_PROJECTILE_TAG, projectile.isNoGravity());
            projectile.setNoGravity(true);
        }
        target.resetFallDistance();
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                mode == Mode.MAGNETO ? ModSounds.MAGNET_GRAB.get() : SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, mode == Mode.MAGNETO ? 1.0F : 0.8F, mode == Mode.MAGNETO ? 1.0F : 1.2F);
        BossGrip.gripped(player, target);
    }

    /**
     * Called every tick for every player: moves what they hold toward where they look. The grip is lost when the target is
     * gone, out of reach or no longer liftable (Magneto's: no longer metal; a phased Vision slips out and he is told so),
     * when the holder dies, changes his power or is entombed (whatever entombed him, D-T 6.3.1), and when a boss slips away
     * ({@link BossGrip#slips}); a boss breaks free at the end of his hold.
     */
    static void tickHolder(ServerPlayer player, PowerData data) {
        Grip grip = GRIPS.get(player.getUUID());
        if (grip == null) {
            if (data.heldEntityId() != -1) {
                data.setHeldEntityId(-1);
            }
            return;
        }
        grip.ticks++;
        ServerLevel level = player.serverLevel();
        Entity e = level.getEntity(grip.target);
        boolean lost = e == null || !e.isAlive() || e.level() != level || player.isDeadOrDying() || player.isSpectator()
                || data.power() != grip.mode.power
                || player.hasEffect(ModEffects.ENTOMBED)
                || e.distanceTo(player) > BREAK_DISTANCE
                || (grip.mode == Mode.MAGNETO && !Metal.staysMagnetic(e))
                || MjolnirPin.isPinned(e)
                || (e instanceof Player p && (p.isCreative() || p.isSpectator() || grip.ticks > MAX_PLAYER_HOLD_TICKS));
        if (lost) {
            boolean phasedAway = e != null && grip.mode == Mode.MAGNETO && e instanceof Player vision && VisionAbilities.isPhased(vision);
            release(player, false);
            if (phasedAway) {
                player.displayClientMessage(Component.translatable(SLIPPED, Component.translatable("power.enhanced.vision")), true);
            }
            return;
        }
        if (BossGrip.slips(player, e, grip.mode)) {
            release(player, false); // he became untouchable, or the lift took him: no word but the fight's
            return;
        }
        if (grip.limit > 0) {
            if (grip.ticks >= grip.limit) {
                end(player, false, true);
                BossGrip.breakFree(player, e, grip.mode.color);
                return;
            }
            BossGrip.struggle(player, e, grip.ticks, grip.limit);
        }
        if (data.heldEntityId() != e.getId()) {
            data.setHeldEntityId(e.getId());
        }

        Vec3 wanted = player.getEyePosition().add(player.getLookAngle().scale(grip.distance)).subtract(0, e.getBbHeight() * 0.5, 0);
        Vec3 target = BossFights.carryLimit(e, wanted); // nothing of a fight leaves its arena in a grip
        Vec3 diff = target.subtract(e.position());
        if (e instanceof ServerPlayer held) {
            // Players move themselves on their own computer, so steer them with velocity updates.
            Vec3 vel = clampLength(diff.scale(0.35), 1.6).add(0, 0.08, 0);
            held.setDeltaMovement(vel);
            held.hurtMarked = true;
            held.resetFallDistance();
            held.connection.aboveGroundTickCount = 0;
        } else {
            e.move(MoverType.SELF, clampLength(diff.scale(0.45), 1.6));
            // A held arrow keeps pointing where the holder looks (an arrow turns to face the way it moves).
            e.setDeltaMovement(e instanceof AbstractArrow ? player.getLookAngle().scale(1.0E-3) : Vec3.ZERO);
            e.hasImpulse = true; // send the new position to clients every tick, so it glides smoothly
            e.resetFallDistance();
            if (e instanceof Mob mob) {
                mob.getNavigation().stop();
                if (mob instanceof IronGolem && mob.getTarget() == player) {
                    mob.setTarget(null);
                }
            }
            if (e instanceof TelekineticBlockEntity block) {
                block.keepHeld();
            }
        }
        if (grip.ticks % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(grip.mode.color, 1.2F),
                    e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 3,
                    e.getBbWidth() * 0.6, e.getBbHeight() * 0.5, e.getBbWidth() * 0.6, 0.02);
        }
    }

    /** Lets go of whatever the player holds. */
    public static void release(ServerPlayer player, boolean gently) {
        end(player, gently, false);
    }

    /** Lets go of whatever the player holds; {@code brokeFree}: a boss (or a shot) tore free at the end of its hold. */
    private static void end(ServerPlayer player, boolean gently, boolean brokeFree) {
        Grip grip = GRIPS.remove(player.getUUID());
        Powers.get(player).setHeldEntityId(-1);
        if (grip == null) {
            return;
        }
        Entity e = find(player, grip.target);
        gripEnded(player, e);
        if (e == null) {
            return;
        }
        e.resetFallDistance();
        letGo(e);
        if (e instanceof TelekineticBlockEntity block) {
            block.drop();
        } else {
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
        }
        if (gently) {
            player.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        BossGrip.letGo(player, e, false, brokeFree);
    }

    /**
     * Throws what the player holds, where he looks: a shot of a fight may turn on its maker itself ({@link
     * BossHooks#throwBack}); a boss's fight may take what Wanda or Magneto throws (a Sentry is crushed, R40; Ultron's heart
     * is Wanda's alone); anything else flies, and a boss is heavy for what he hits but takes the impact himself, through
     * his own caps (D-T 4.5).
     */
    private static void launch(ServerPlayer player) {
        Grip grip = GRIPS.remove(player.getUUID());
        Powers.get(player).setHeldEntityId(-1);
        if (grip == null) {
            return;
        }
        Entity e = find(player, grip.target);
        gripEnded(player, e);
        if (e == null) {
            return;
        }
        double speed = EnhancedConfig.THROW_SPEED.get();
        Vec3 vel = player.getLookAngle().scale(speed);
        letGo(e);
        if (e instanceof BossHooks hooks && hooks.throwBack(player, vel)) {
            BossGrip.letGo(player, e, true, false);
            return; // a shot of a fight turned on its maker (Thanos's sword, meteor, orb)
        }
        if ((grip.mode == Mode.WANDA || grip.mode == Mode.MAGNETO) && BossFights.telekinesisLaunch(player, grip.mode, e)) {
            BossGrip.letGo(player, e, false, false);
            return; // a boss took what was thrown (a Sentry is crushed; by her hand, Ultron's heart is torn out)
        }
        if (e instanceof TelekineticBlockEntity block) {
            block.launch(player, vel);
        } else if (e instanceof Projectile projectile) {
            launchProjectile(player, projectile, vel);
        } else {
            e.setDeltaMovement(vel);
            e.hurtMarked = true;
            e.hasImpulse = true;
            boolean golem = e instanceof IronGolem;
            FLIGHTS.put(e.getUUID(), new Flight(player.getUUID(), player.level().dimension(), e.position(), speed,
                    golem || BossGrip.isBoss(e), !golem, grip.mode.color));
        }
        BossGrip.letGo(player, e, true, false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
    }

    /** The entity of this UUID: in the holder's level, or (if he has just changed dimension) in any level of the server. */
    @Nullable
    private static Entity find(ServerPlayer holder, UUID id) {
        Entity e = holder.serverLevel().getEntity(id);
        if (e != null) {
            return e;
        }
        for (ServerLevel level : holder.server.getAllLevels()) {
            e = level.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /**
     * A grip of this holder on {@code target} (null if it is gone) just ended, set down, thrown, lost, or he left:
     * remembered for the Sneak grace; when the target is a player, for the player regrip ({@link #playerRegripLeft}); and,
     * when the holder may not hurt it, the target is spared its landing ({@link #isSpared}).
     */
    private static void gripEnded(ServerPlayer holder, @Nullable Entity target) {
        long now = holder.serverLevel().getGameTime();
        GRIP_ENDS.put(holder.getUUID(), now);
        if (target instanceof Player) {
            Map<UUID, Long> letGo = PLAYERS_LET_GO.computeIfAbsent(holder.getUUID(), id -> new HashMap<>());
            long regrip = EnhancedConfig.TELEKINESIS_PLAYER_REGRIP_TICKS.get();
            letGo.values().removeIf(at -> now - at >= regrip); // forget the waits that are over
            letGo.put(target.getUUID(), now);
        }
        if (target != null && mayNotHurt(holder, target)) {
            SPARED.put(target.getUUID(), new Spared(target.level().dimension(), now));
        }
    }

    /**
     * Whether {@code holder} may not hurt {@code target} (D-M 3.8): a player he may not harm (PvP off, a teammate with
     * friendly fire off), or a creature another player owns whom he may not harm (a pet, a drone, the Vision of a Hex; with
     * that owner away, PvP decides).
     */
    private static boolean mayNotHurt(ServerPlayer holder, Entity target) {
        if (target instanceof Player player) {
            return !AreaTargets.mayHurt(holder, player, false);
        }
        if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null && !pet.getOwnerUUID().equals(holder.getUUID())) {
            ServerPlayer owner = holder.server.getPlayerList().getPlayer(pet.getOwnerUUID());
            if (owner != null) {
                return !AreaTargets.mayHurt(holder, owner, false);
            }
            return pet.getOwner() == null && !holder.server.isPvpAllowed();
        }
        return false;
    }

    /**
     * Whether {@code e} is spared its landing (D-M 3.8): a grip let go of it whose holder may not hurt it, and it has not
     * landed since. While {@code magneto.telekinesisSparesFriends} is on, the landing guard ({@code
     * TelekinesisEvents.onIncomingDamage}) takes no fall, wall or slam damage from it.
     */
    public static boolean isSpared(Entity e) {
        return SPARED.containsKey(e.getUUID());
    }

    /**
     * The game time the holder's last grip ended, however it ended: set down, thrown, lost (out of reach, the target gone
     * or no longer liftable, the 30 s cap on a player), or the holder died, left or changed his power; {@link #NEVER} if
     * none did. For a second after it, Sneak + the key with nothing held does nothing ({@code
     * magneto.telekinesisSneakGraceTicks}, R41).
     */
    public static long lastGripEnd(ServerPlayer holder) {
        return GRIP_ENDS.getOrDefault(holder.getUUID(), NEVER);
    }

    /** A held projectile gets its weight back. */
    private static void letGo(Entity e) {
        if (e instanceof Projectile projectile && projectile.getPersistentData().contains(HELD_PROJECTILE_TAG)) {
            projectile.setNoGravity(projectile.getPersistentData().getBoolean(HELD_PROJECTILE_TAG));
            projectile.getPersistentData().remove(HELD_PROJECTILE_TAG);
        }
    }

    /**
     * A caught projectile flies again, where the holder looks, and hits for them. A shot of a boss fight (Ultron's bolt,
     * Apocalypse's lance or boulder) leaves the fight with it, so that nothing of the boss's side is spared by it, unless
     * {@code boss.telekinesisCatchesBossShots} is off (then it keeps its mark, as in 3.0; D-T 6.1, 6.3).
     */
    private static void launchProjectile(ServerPlayer player, Projectile projectile, Vec3 velocity) {
        if (BossConfig.TELEKINESIS_CATCHES_BOSS_SHOTS.get()) {
            BossFights.disown(projectile);
        }
        if (projectile instanceof AbstractArrow arrow) {
            arrow.inGround = false;
            if (!(arrow instanceof ThrownTrident)) {
                // The holder gets the credit, but nobody gains the right to pick up an arrow they couldn't before.
                AbstractArrow.Pickup pickup = arrow.pickup;
                arrow.setOwner(player);
                arrow.pickup = pickup;
            }
            // Tridents keep their owner, so Loyalty and pickup still belong to whoever threw them.
        } else {
            projectile.setOwner(player);
        }
        projectile.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0.0F);
        projectile.hurtMarked = true;
        projectile.hasImpulse = true;
    }

    /** A projectile saved to disk while held comes back as a normal projectile. */
    public static void onJoin(Entity entity) {
        if (entity instanceof Projectile projectile && projectile.getPersistentData().contains(HELD_PROJECTILE_TAG) && !isHeld(projectile)) {
            letGo(projectile);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Flights

    /**
     * Called once per server tick: every thrown thing flies on, hits what it crashes into and slams into what stops it. A
     * throw of anything of a fight (or of a fighter) that is about to cross the edge of a gated arena (Ultron's field,
     * Apocalypse's wall) is stopped dead half a block inside it (D-T 4.5). When a flight ends, the boss may act again
     * ({@link BossGrip#landed}) and a boss or minion that ended outside its carry limit is brought back ({@link
     * BossFights#bringBack}). Then the landing guard of whoever has landed is dropped ({@link #isSpared}).
     */
    public static void tickFlights(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Flight>> it = FLIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Flight> entry = it.next();
            Flight f = entry.getValue();
            ServerLevel level = server.getLevel(f.dimension);
            Entity e = level == null ? null : level.getEntity(entry.getKey());
            if (e == null || !e.isAlive()) {
                it.remove();
                continue;
            }
            if (++f.ticks > 80) {
                it.remove();
                flightOver(e);
                continue;
            }
            ServerPlayer thrower = server.getPlayerList().getPlayer(f.thrower);
            if (e instanceof Mob mob && mob.isNoAi()) {
                // Mobs without AI don't run their own physics; fly them ourselves.
                Vec3 v = e.getDeltaMovement();
                e.move(MoverType.SELF, v);
                e.setDeltaMovement(v.x * 0.91, (v.y - 0.08) * 0.98, v.z * 0.91);
                e.hasImpulse = true;
            }
            Vec3 pos = e.position();
            double speed = pos.distanceTo(f.lastPos);
            double hitSpeed = Math.max(speed, f.lastSpeed);

            if (hitSpeed > 0.5) {
                AABB box = e.getBoundingBox().inflate(0.4);
                Vec3 dir = pos.subtract(f.lastPos);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                        v -> v != e && v != thrower && v.isAlive() && !v.isSpectator() && !f.hit.contains(v.getId()))) {
                    f.hit.add(victim.getId());
                    float damage = (f.heavyHit ? 15F : 6F) * (float) Mth.clamp(hitSpeed / 1.2, 0.6, 1.0);
                    victim.hurt(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, thrower, e), damage);
                    if (dir.horizontalDistanceSqr() > 1.0E-4) {
                        victim.knockback(1.2, -dir.x, -dir.z);
                    }
                    if (e instanceof IronGolem golem && victim instanceof Enemy) {
                        golem.setTarget(victim);
                    }
                }
            }

            if (e instanceof ServerPlayer held) {
                held.connection.aboveGroundTickCount = 0;
            }
            if (e instanceof IronGolem golem && golem.getTarget() == thrower) {
                golem.setTarget(null);
            }

            Vec3 edge = BossFights.edgeStop(e, pos.add(e.getDeltaMovement()));
            if (edge != null) {
                // It would cross the field or the wall of its arena next tick: it stops dead inside it, and crashes there.
                e.setDeltaMovement(Vec3.ZERO);
                e.teleportTo(edge.x, edge.y, edge.z);
                e.hurtMarked = true;
                impact(level, e, f, thrower, hitSpeed);
                it.remove();
                flightOver(e);
                continue;
            }
            boolean slammed = f.ticks > 1 && f.lastSpeed > 0.6 && (speed < f.lastSpeed * 0.45 || e.horizontalCollision);
            if (slammed) {
                impact(level, e, f, thrower, f.lastSpeed);
                it.remove();
                flightOver(e);
                continue;
            }
            if (f.ticks > 3 && speed < 0.05 && e.onGround()) {
                it.remove();
                flightOver(e);
                continue;
            }
            if (f.ticks % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(f.color, 1.0F), pos.x, pos.y + e.getBbHeight() * 0.5, pos.z,
                        2, 0.2, 0.2, 0.2, 0.0);
            }
            f.lastSpeed = speed;
            f.lastPos = pos;
        }
        tickSpared(server);
    }

    /**
     * The flight of {@code e} is over (it slammed, landed, was stopped or flew its time): a thrown boss may act again, and
     * a boss or minion that ended outside its carry limit is brought back by its fight.
     */
    private static void flightOver(Entity e) {
        BossGrip.landed(e);
        BossFights.bringBack(e);
    }

    /** A thrown entity's flight is forgotten (a dying Thanos: nothing may pause his death). */
    public static void forgetFlight(Entity e) {
        FLIGHTS.remove(e.getUUID());
    }

    /**
     * Drops the landing guard of whoever has landed: on the ground, in water or riding, at least {@link #SPARED_MIN_TICKS}
     * after the grip ended, and of everyone {@link #SPARED_MAX_TICKS} after it (or once they are gone).
     */
    private static void tickSpared(MinecraftServer server) {
        if (SPARED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Spared>> it = SPARED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Spared> entry = it.next();
            ServerLevel level = server.getLevel(entry.getValue().dimension());
            Entity e = level == null ? null : level.getEntity(entry.getKey());
            long since = level == null ? SPARED_MAX_TICKS : level.getGameTime() - entry.getValue().since();
            if (e == null || since >= SPARED_MAX_TICKS
                    || (since >= SPARED_MIN_TICKS && !isHeld(e) && (e.onGround() || e.isInWater() || e.isPassenger()))) {
                it.remove();
            }
        }
    }

    /**
     * What a thrown thing does when it slams into something: dust and a crash, and a body or a vehicle takes the impact,
     * {@code (speed - 0.6) x 8}, 2 to 16, credited to the thrower (an Iron Golem does not; a boss does, through his own
     * caps).
     */
    private static void impact(ServerLevel level, Entity e, Flight f, @Nullable ServerPlayer thrower, double impactSpeed) {
        BlockState ground = level.getBlockState(e.blockPosition().below());
        if (ground.isAir()) {
            ground = level.getBlockState(e.blockPosition());
        }
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), e.getX(), e.getY() + 0.2, e.getZ(),
                    30, e.getBbWidth() * 0.5, 0.2, e.getBbWidth() * 0.5, 0.15);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 1, 0, 0, 0, 0);
        if (f.heavyHit) {
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.0F, 0.6F);
        }
        if (f.takesImpact && (e instanceof LivingEntity || e instanceof VehicleEntity)) {
            // Only bodies and vehicles are hurt by the impact; a thrown item stack must not be destroyed.
            if (!f.heavyHit) {
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_BIG_FALL, SoundSource.NEUTRAL, 1.0F, 0.8F);
            }
            float damage = (float) Mth.clamp((impactSpeed - 0.6) * 8.0, 2.0, 16.0);
            e.hurt(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, thrower, thrower), damage);
        }
        e.resetFallDistance();
    }

    private static Vec3 clampLength(Vec3 v, double max) {
        double len = v.length();
        return len > max ? v.scale(max / len) : v;
    }

    /** Forget everything (server stopping). */
    public static void clear() {
        GRIPS.clear();
        FLIGHTS.clear();
        GRIP_ENDS.clear();
        PLAYERS_LET_GO.clear();
        SPARED.clear();
        BossGrip.clear();
    }

    /** A player left the game: drop what they hold. */
    public static void onLogout(ServerPlayer player) {
        release(player, false);
    }

    /**
     * Whoever holds {@code target} lets go of it, as if the grip were lost (the Hex holds the Vision a grip carried to its
     * wall, D-M 3.7).
     *
     * @return whether a grip held it
     */
    public static boolean releaseTarget(Entity target) {
        ServerPlayer holder = holderOf(target);
        if (holder == null) {
            return false;
        }
        release(holder, false);
        return true;
    }

    /** The player whose grip holds {@code target}, or null. */
    @Nullable
    public static ServerPlayer holderOf(Entity target) {
        if (target.getServer() == null) {
            return null;
        }
        UUID id = target.getUUID();
        for (Map.Entry<UUID, Grip> entry : GRIPS.entrySet()) {
            if (entry.getValue().target.equals(id)) {
                return target.getServer().getPlayerList().getPlayer(entry.getKey());
            }
        }
        return null;
    }

    private Telekinesis() {
    }
}
