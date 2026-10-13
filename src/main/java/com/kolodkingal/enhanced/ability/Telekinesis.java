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
 * <p>While held, the target is moved every tick towards the point the holder is looking at. Launched targets are
 * tracked until they slam into something: they hurt every creature they crash into and take heavy impact damage
 * when they hit a wall or the floor. A launched projectile simply flies again, where the holder looks.
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
        int ticks;

        Grip(UUID target, Mode mode, double distance) {
            this.target = target;
            this.mode = mode;
            this.distance = distance;
        }
    }

    private static final class Flight {
        final UUID thrower;
        final ResourceKey<Level> dimension;
        final boolean heavy;
        final int color;
        final IntSet hit = new IntOpenHashSet();
        Vec3 lastPos;
        double lastSpeed;
        int ticks;

        Flight(UUID thrower, ResourceKey<Level> dimension, Vec3 pos, double speed, boolean heavy, int color) {
            this.thrower = thrower;
            this.dimension = dimension;
            this.lastPos = pos;
            this.lastSpeed = speed;
            this.heavy = heavy;
            this.color = color;
        }
    }

    /** Holder UUID → what they hold. */
    private static final Map<UUID, Grip> GRIPS = new HashMap<>();
    /** Thrown entity UUID → its flight. */
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();
    /** Holder UUID → the game time their last grip ended, however it ended. */
    private static final Map<UUID, Long> GRIP_ENDS = new HashMap<>();
    /** Holder UUID → (player UUID → the game time a grip of that holder on that player ended), for the player regrip. */
    private static final Map<UUID, Map<UUID, Long>> PLAYERS_LET_GO = new HashMap<>();
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
     * Interim rule of lane TK, step 3 part 1: what left {@code telekinesis_immune} in 3.1 to be held as a boss (the three
     * villains, the Wither, the Warden and the Elder Guardian, Thanos's sword, meteor and orb) is not held yet, as in 3.0,
     * until the grip that holds it as a boss exists: the hold limit and the regrip of the villains (step 3 part 2, with the
     * bosses' own answers of step 4), a shot still in the grip (step 7), a vanilla boss's {@code NoAI} (step 10). {@link
     * #refusal} already names what refuses them meanwhile (a switch off, an invulnerable state, the lift).
     */
    private static boolean awaitsBossGrip(Entity e) {
        return e.getType().is(ModTags.PULL_IMMUNE) && !e.getType().is(ModTags.TELEKINESIS_IMMUNE);
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

    public static void hold(ServerPlayer player, Entity target, Mode mode) {
        SandCocoonEntity.freeFrom(target); // taking hold of a friend breaks the cocoon of Apocalypse's sand that held them
        double dist = player.getEyePosition().distanceTo(target.position().add(0, target.getBbHeight() * 0.5, 0));
        GRIPS.put(player.getUUID(), new Grip(target.getUUID(), mode, Mth.clamp(dist, MIN_HOLD_DISTANCE, MAX_HOLD_DISTANCE)));
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
    }

    /** Called every tick for every player. */
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
                || e.distanceTo(player) > BREAK_DISTANCE
                || (grip.mode == Mode.MAGNETO && !Metal.staysMagnetic(e))
                || MjolnirPin.isPinned(e)
                || (e instanceof Player p && (p.isCreative() || p.isSpectator() || grip.ticks > MAX_PLAYER_HOLD_TICKS));
        if (lost) {
            release(player, false);
            return;
        }
        if (data.heldEntityId() != e.getId()) {
            data.setHeldEntityId(e.getId());
        }

        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(grip.distance)).subtract(0, e.getBbHeight() * 0.5, 0);
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
        Grip grip = GRIPS.remove(player.getUUID());
        Powers.get(player).setHeldEntityId(-1);
        if (grip == null) {
            return;
        }
        Entity e = player.serverLevel().getEntity(grip.target);
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
    }

    private static void launch(ServerPlayer player) {
        Grip grip = GRIPS.remove(player.getUUID());
        Powers.get(player).setHeldEntityId(-1);
        if (grip == null) {
            return;
        }
        Entity e = player.serverLevel().getEntity(grip.target);
        gripEnded(player, e);
        if (e == null) {
            return;
        }
        if (grip.mode == Mode.WANDA && BossFights.telekinesisLaunch(player, e)) {
            letGo(e);
            return; // a boss took what she threw (a Sentry's core is crushed)
        }
        double speed = EnhancedConfig.THROW_SPEED.get();
        Vec3 vel = player.getLookAngle().scale(speed);
        letGo(e);
        if (e instanceof TelekineticBlockEntity block) {
            block.launch(player, vel);
        } else if (e instanceof Projectile projectile) {
            launchProjectile(player, projectile, vel);
        } else {
            e.setDeltaMovement(vel);
            e.hurtMarked = true;
            e.hasImpulse = true;
            FLIGHTS.put(e.getUUID(), new Flight(player.getUUID(), player.level().dimension(), e.position(), speed,
                    e instanceof IronGolem, grip.mode.color));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
    }

    /**
     * A grip of this holder on {@code target} (null if it is gone) just ended, set down, thrown, lost, or he left:
     * remembered for the Sneak grace, and, when the target is a player, for the player regrip ({@link #playerRegripLeft}).
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

    /** A caught projectile flies again, where the holder looks, and hits for them. */
    private static void launchProjectile(ServerPlayer player, Projectile projectile, Vec3 velocity) {
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

    /** Called once per server tick. */
    public static void tickFlights(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Flight>> it = FLIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Flight> entry = it.next();
            Flight f = entry.getValue();
            ServerLevel level = server.getLevel(f.dimension);
            Entity e = level == null ? null : level.getEntity(entry.getKey());
            if (e == null || !e.isAlive() || ++f.ticks > 80) {
                it.remove();
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
                    float damage = (f.heavy ? 15F : 6F) * (float) Mth.clamp(hitSpeed / 1.2, 0.6, 1.0);
                    victim.hurt(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, thrower, e), damage);
                    if (dir.horizontalDistanceSqr() > 1.0E-4) {
                        victim.knockback(1.2, -dir.x, -dir.z);
                    }
                    if (f.heavy && e instanceof IronGolem golem && victim instanceof Enemy) {
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

            boolean slammed = f.ticks > 1 && f.lastSpeed > 0.6 && (speed < f.lastSpeed * 0.45 || e.horizontalCollision);
            if (slammed) {
                impact(level, e, f, thrower, f.lastSpeed);
                it.remove();
                continue;
            }
            if (f.ticks > 3 && speed < 0.05 && e.onGround()) {
                it.remove();
                continue;
            }
            if (f.ticks % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(f.color, 1.0F), pos.x, pos.y + e.getBbHeight() * 0.5, pos.z,
                        2, 0.2, 0.2, 0.2, 0.0);
            }
            f.lastSpeed = speed;
            f.lastPos = pos;
        }
    }

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
        if (f.heavy) {
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.0F, 0.6F);
        } else if (e instanceof LivingEntity || e instanceof VehicleEntity) {
            // Only bodies and vehicles are hurt by the impact; a thrown item stack must not be destroyed.
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_BIG_FALL, SoundSource.NEUTRAL, 1.0F, 0.8F);
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
        BossGrip.clear();
    }

    /** A player left the game: drop what they hold. */
    public static void onLogout(ServerPlayer player) {
        release(player, false);
    }

    private Telekinesis() {
    }
}
