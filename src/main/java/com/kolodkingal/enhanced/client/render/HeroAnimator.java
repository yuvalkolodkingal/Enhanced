package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.screen.HexScreen;
import com.kolodkingal.enhanced.client.stones.StoneArmLayer;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.stones.StoneBeams;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * The brain of the heroes' animation. It keeps a {@link HeroAnimState} per hero and moves it on:
 * <ul>
 *     <li>{@link #tick}, once per client tick: samples each hero's motion, finds the edges in their synced data (a
 *     take-off, a landing, braking, a run starting or ending) and starts the matching gestures, sets which stances are
 *     on, and caches the entities and points the poses aim at;</li>
 *     <li>{@link #frame}, once per frame the hero is drawn: measures the smoothed motion, eases every weight, runs the
 *     lean and bank springs, plays the gesture slots, spins the weapon, and says how the body is to be moved on the pose
 *     stack ({@link #applyBody}). Everything eases with the frame's own {@code dt}, so it looks the same at any frame
 *     rate.</li>
 * </ul>
 * Only what every client knows about every player is used: positions, the body yaw and the synced {@link PowerData}.
 * The pose of the model parts is made later, in {@link HeroPoses}, from the state this class leaves.
 */
public final class HeroAnimator {
    /** Time constant (ticks) of the measured motion: other players' positions arrive in jumps. */
    private static final float TAU_MOTION = 2.0F;
    /** Blocks moved in one tick from which it is a teleport. */
    private static final double TELEPORT = 6.0;
    private static final float EPSILON = 0.05F;
    /** The spin of Thor's whirling weapon (degrees per tick at full spin). */
    private static final float SPIN_RATE = 54.0F;

    private static final Map<UUID, HeroAnimState> STATES = new HashMap<>();
    /** 3.4: the states of the editor's preview puppets (they live outside the world, so the tick never prunes them). */
    private static final Map<UUID, HeroAnimState> PREVIEWS = new HashMap<>();
    /**
     * 3.4: while a hero's plug-in runs for a custom hero (its edges), the kit every gesture it starts must belong to
     * ({@code HeroAbilityAnims.allows}); null otherwise.
     */
    static com.kolodkingal.enhanced.custom.HeroKit startFilter;

    // The debugging tools of /enhanced_anim: pretended motion for the local player, and a freeze.
    /** Whether the overlay of the numbers is on. */
    public static boolean debugOverlay;
    private static boolean debugFly;
    private static float debugS;
    private static float debugU;
    private static float debugTurn;
    private static boolean debugFrozen;
    /** The player the debugging tools act on: another player's name, or null for the local player. */
    private static java.util.UUID debugTarget;
    /** Stances forced on (1) or off (-1) for the debug target by {@code /enhanced_anim stance}; 0 leaves them to the data. */
    private static final byte[] DEBUG_STANCE = new byte[Stance.COUNT];

    /** The state of a player, or null when the animation does not run for them. */
    public static HeroAnimState peek(Player player) {
        HeroAnimState s = STATES.get(player.getUUID());
        return s != null || PREVIEWS.isEmpty() ? s : PREVIEWS.get(player.getUUID());
    }

    public static void clear() {
        STATES.clear();
        PREVIEWS.clear();
    }

    // ====================================================================================================================
    // Tick
    // ====================================================================================================================

    /** Samples every hero in view, finds the edges in their data and sets their stances. */
    public static void tick(ClientLevel level) {
        if (!EnhancedClientConfig.animations()) {
            STATES.clear();
            HandTracker.clear();
            return;
        }
        STATES.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        HandTracker.prune(level);
        long now = level.getGameTime();
        Minecraft mc = Minecraft.getInstance();
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            // Anyone holding Thor's weapon can ride the Bifrost, so that state exists for everyone.
            // (3.6: so does anyone firing a beam: the Scepter's Mind Beam is anyone's, and raises his arm.)
            boolean tracked = data.power().hasPowers() || data.is(Live.BIFROST) || data.is(Live.BEAMING);
            HeroAnimState s = STATES.get(player.getUUID());
            if (!tracked) {
                if (s != null) {
                    STATES.remove(player.getUUID());
                }
                continue;
            }
            if (s == null) {
                s = new HeroAnimState(player.getUUID());
                STATES.put(player.getUUID(), s);
                s.reseed(data.power());
            } else if (s.power != data.power()) {
                s.reseed(data.power());
            }
            s.entityId = player.getId();
            CustomActing.identity(s, player, data);
            boolean remember = !s.seeded;
            sample(s, player, data, now);
            worldReferences(s, player, data, level, now);
            if (!remember) {
                edges(s, player, data, now);
                GestureTable.edges(s, player, data, level, now);
            }
            stanceTargets(s, player, data, mc, now);
            rememberData(s, data, now);
            GestureTable.remember(s, player, data, level);
            s.look = data.variant();
            s.seeded = true;
            // A hero who is not being drawn (the local player in first person) still moves on, once per tick.
            if (s.time < player.tickCount - 1.0) {
                frame(player, player.yBodyRot, 0.0F);
            }
        }
    }

    /** The motion of the last ticks, the ground and air counters, the flags the frame needs. */
    private static void sample(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        double dx = p.getX() - p.xOld;
        double dy = p.getY() - p.yOld;
        double dz = p.getZ() - p.zOld;
        if (dx * dx + dy * dy + dz * dz > TELEPORT * TELEPORT) {
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
        }
        shift(s.dy, (float) dy);
        shift(s.hv, (float) Math.sqrt(dx * dx + dz * dz));
        shift(s.sRing, s.s);
        boolean ground = p.onGround();
        s.prevAirTicks = s.airTicks;
        s.wasOnGround = s.onGround;
        s.onGround = ground;
        s.airTicks = ground ? 0 : s.airTicks + 1;
        s.groundTicks = ground ? s.groundTicks + 1 : 0;
        s.prevFlyingEff = s.flyingEff;
        // The local flag flips at once; everyone else's arrives with the synced data.
        s.flyingEff = p instanceof LocalPlayer lp ? lp.getAbilities().flying : data.flying();
        s.sinceFlying = s.flyingEff ? 0 : Math.min(100, s.sinceFlying + 1);
        s.sprinting = p.isSprinting();
        s.crouching = p.isCrouching();
        s.speeding = data.is(Live.SPEEDING);
        s.slowMo = data.slowMo();
        s.walkman = data.walkman();
        s.wallRunning = s.speeding && s.climb > 0.2F && s.sH < 0.15F;
        s.styleKind = switch (data.power()) {
            case QUICKSILVER -> s.speeding ? (data.slowMo() ? 1 : 2) : 0;
            case TOMMY -> s.speeding ? 3 : 0;
            // 3.4: a custom hero speeds with the run his definition names (or the one of his speed).
            case CUSTOM -> CustomActing.styleKind(s, p, data);
            default -> 0;
        };
        // 3.3: a speedster on a wall or a ceiling (3.4: also a custom hero with a speedster's abilities).
        if (data.acts(Power.QUICKSILVER) || data.acts(Power.TOMMY)) {
            SpeedSurfacePoses.sample(s, p, data);
        } else {
            s.surfaceKind = 0;
        }
    }

    private static void shift(float[] ring, float value) {
        for (int i = ring.length - 1; i > 0; i--) {
            ring[i] = ring[i - 1];
        }
        ring[0] = value;
    }

    /** The entities and points the poses aim at, found once per tick. */
    private static void worldReferences(HeroAnimState s, AbstractClientPlayer p, PowerData data, ClientLevel level, long now) {
        s.heldEntity = data.heldEntityId() != -1 ? level.getEntity(data.heldEntityId()) : null;
        Entity held = s.heldEntity;
        s.heldMass = held != null && (held instanceof IronGolem || held instanceof VehicleEntity || held instanceof TelekineticBlockEntity
                || held instanceof Player || held.getBoundingBox().getSize() >= 1.2) ? 1.0F : 0.0F;
        Entity hammer = data.hammerEntityId() != -1 ? level.getEntity(data.hammerEntityId()) : null;
        s.thrownWeapon = hammer instanceof ThrownWeaponEntity ? hammer : null;
        if (s.thrownWeapon != null) {
            s.lastWeaponPos = s.thrownWeapon.position().add(0.0, 0.3, 0.0);
            s.lastWeaponTick = now;
            System.arraycopy(s.weaponTrail, 0, s.weaponTrail, 1, s.weaponTrail.length - 1);
            s.weaponTrail[0] = s.lastWeaponPos;
            s.catchFrom = null;
        } else if (now - s.lastWeaponTick > 5) {
            s.lastWeaponPos = null;
            java.util.Arrays.fill(s.weaponTrail, null);
        }
        s.weaponKind = 0;
        if (data.has(com.kolodkingal.enhanced.custom.HeroAbility.THOR_HAMMER)) {
            InteractionHand hand = ThorAbilities.weaponHand(p);
            if (hand != null) {
                ItemStack stack = p.getItemInHand(hand);
                s.weaponKind = ThorAbilities.isStormbreaker(stack) ? 2 : 1;
                s.weaponInMainHand = hand == InteractionHand.MAIN_HAND;
                s.weaponHandTick = p.tickCount;
            }
        }
        s.caughtCount = 0;
        if (data.has(com.kolodkingal.enhanced.custom.HeroAbility.MAGNETO_SHIELD) && data.magneticShield()) {
            AABB box = p.getBoundingBox().inflate(MagnetoAbilities.SHIELD_RADIUS + 2.0);
            Vec3 eye = p.getEyePosition();
            Vec3[] sorted = new Vec3[s.caught.length];
            double[] dist = new double[s.caught.length];
            int count = 0;
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box,
                    pr -> pr.isNoGravity() && pr.getDeltaMovement().lengthSqr() < 1.0E-6 && MagnetoAbilities.isMetalProjectile(pr))) {
                double d = projectile.position().distanceToSqr(eye);
                Vec3 at = projectile.position();
                int i = count < sorted.length ? count : sorted.length - 1;
                if (count >= sorted.length && d >= dist[i]) {
                    continue;
                }
                while (i > 0 && dist[i - 1] > d) {
                    dist[i] = dist[i - 1];
                    sorted[i] = sorted[i - 1];
                    i--;
                }
                dist[i] = d;
                sorted[i] = at;
                count = Math.min(count + 1, sorted.length);
            }
            System.arraycopy(sorted, 0, s.caught, 0, count);
            s.caughtCount = count;
        }
    }

    /** The edges of this tick: a take-off, a landing, braking, the start and the end of a run. */
    private static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        Power power = data.power();
        // 3.4: whose flight it is (a custom hero flies with the style of his definition).
        Power flier = s.flightAs;
        FlightProfile profile = FlightProfile.of(s);
        float age = p.tickCount;
        // 2: take-off.
        if (profile != null && !s.prevFlyingEff && s.flyingEff && power.canFly() && (s.onGround || s.airTicks <= 8)) {
            GestureId id = switch (flier) {
                case MAGNETO -> GestureId.TAKEOFF_MAGNETO;
                case SCARLET_WITCH -> GestureId.TAKEOFF_WANDA;
                case VISION -> GestureId.TAKEOFF_VISION;
                case BILLY -> GestureId.TAKEOFF_BILLY;
                default -> {
                    // (Thor's take-off is the default; a hero of 3.0 may have one of its own.)
                    HeroClient hero = HeroClients.of(flier);
                    GestureId own = hero == null ? null : hero.takeoffGesture();
                    yield own != null ? own : GestureId.TAKEOFF_THOR;
                }
            };
            start(s, id, 0.0F, age);
            FlightEffects.takeoff(p, s, flier);
        }
        // 3: landing.
        if (!s.wasOnGround && s.onGround && s.prevAirTicks >= 3 && s.sinceFlying <= 8 && profile != null
                && !data.is(Live.BIFROST)) {
            float fall = -Math.min(s.dy[0], Math.min(s.dy[1], s.dy[2]));
            float horiz = s.hv[1];
            float fs = flightSpeed(s, profile);
            boolean hard = fall >= 0.70F || (fall >= 0.6F * FlightProfile.vyRef(fs) && horiz >= 0.8F * FlightProfile.vRef(fs));
            float soft = 0.4F;
            if (flier == Power.THOR && hard && (s.weaponKind > 0 || power == Power.CUSTOM)) {
                // (A custom hero who flies like Thor lands like him, weapon or not: the fists go to the ground.)
                start(s, GestureId.LAND_HERO, 0.0F, age);
                soft = 1.4F;
                FlightEffects.impact(p, s, s.weaponKind == 2 ? 1.1F : 1.0F);
            } else if (flier == Power.THOR) {
                start(s, GestureId.LAND_SOFT, 0.0F, age);
                soft = 1.4F;
            } else if (data.has(com.kolodkingal.enhanced.custom.HeroAbility.VISION_DENSITY) && data.dense()) {
                soft = 1.8F;
                FlightEffects.denseLanding(p, s);
            } else {
                start(s, GestureId.LAND_SOFT, 0.0F, age);
            }
            // Every landing dips the body a little; a hero's dip is deeper.
            s.drop.v += soft * 0.084F * AnimMath.clamp((fall - 0.15F) / 0.45F, 0.0F, 1.5F);
            if (fall >= 0.25F) {
                FlightEffects.softLanding(p, s, fall);
            }
        }
        // 4: a speedster's run starts or ends.
        if (s.speeding && !s.lastSpeeding) {
            s.pitch.v += 1.2F;
            s.runPhase = 0.0F;
            s.runOmega = 0.0F;
        }
        if (!s.speeding && s.lastSpeeding && s.hv[1] >= 0.5F) {
            start(s, GestureId.RUN_SKID, 0.0F, age);
        }
        // 14: braking in flight.
        if (profile != null && !profile.noBrake && s.flyingEff && s.sRing[3] >= 0.8F && s.s <= s.sRing[3] - 0.45F
                && now - s.lastBrakeAt > 15) {
            s.lastBrakeAt = now;
            start(s, flier == Power.THOR ? GestureId.BRAKE_THOR : GestureId.BRAKE, 0.0F, age);
        }
        // The sonic boom: the flight speed crosses 1.85 after being below 1.5 within the last 40 ticks.
        if (profile != null && (flier == Power.THOR || flier == Power.VISION) && s.flyingEff) {
            if (s.s < 1.5F) {
                s.lastWasFast = false;
                s.lastFastAt = now;
            } else if (s.s >= 1.85F && now - s.lastFastAt <= 40 && now - s.lastBoomAt >= 60) {
                s.lastBoomAt = now;
                FlightEffects.boom(p, s, flier);
            }
        }
        // A new look: a quick turn as the new suit shimmers in.
        if (!s.look.equals(data.variant()) && s.seeded) {
            start(s, GestureId.LOOK_CHANGE, 0.0F, age);
        }
        // The heroes of 3.0 find their own edges.
        HeroClient hero = HeroClients.of(power);
        if (hero != null) {
            hero.edges(s, p, data, now);
        }
    }

    /** What the next tick compares against. */
    private static void rememberData(HeroAnimState s, PowerData data, long now) {
        boolean beam = data.beamActive();
        boolean shield = data.chaosShieldActive();
        boolean holding = data.heldEntityId() != -1;
        if ((s.lastBeam && !beam) || (s.lastShield && !shield) || (s.lastHeld != -1 && !holding)) {
            s.spellEndedAt = now;
        }
        s.lastBeam = beam;
        s.lastShield = shield;
        s.lastHeld = data.heldEntityId();
        s.lastHammer = data.hammerEntityId();
        s.lastShrapnel = data.shrapnelActive();
        s.lastSpeeding = s.speeding;
    }

    /** Which stances are on, from the synced data. */
    private static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, Minecraft mc, long now) {
        boolean[] on = s.stanceOn;
        java.util.Arrays.fill(on, false);
        Power power = data.power();
        // (3.4: each by the ability, so a custom hero who has it stands as its hero does; for the hero it is the same.)
        on[Stance.MAG_FIELD.ordinal()] = data.has(HeroAbility.MAGNETO_SHIELD) && data.magneticShield();
        on[Stance.VISION_PHASED.ordinal()] = data.has(HeroAbility.VISION_PHASE) && data.visionPhased();
        on[Stance.VISION_DENSE.ordinal()] = data.has(HeroAbility.VISION_DENSITY) && data.dense();
        on[Stance.LOKI_FROST.ordinal()] = data.has(HeroAbility.LOKI_FROST_GIANT) && data.toggle(Toggle.FROST_GIANT);
        on[Stance.THOR_UNLEASHED.ordinal()] = data.has(HeroAbility.THOR_THUNDER) && data.toggle(Toggle.GOD_OF_THUNDER);
        on[Stance.NOISE.ordinal()] = data.has(HeroAbility.BILLY_NOISE) && data.toggle(Toggle.THE_NOISE);
        on[Stance.WANDA_READY.ordinal()] = data.acts(Power.SCARLET_WITCH) && now - s.spellEndedAt <= 60;
        on[Stance.WANDA_HEX_CONTROL.ordinal()] = data.has(HeroAbility.WANDA_HEX) && p == mc.player && mc.screen instanceof HexScreen;
        on[Stance.THOR_BIFROST.ordinal()] = data.is(Live.BIFROST);
        on[Stance.THOR_WAIT.ordinal()] = data.has(HeroAbility.THOR_HAMMER) && s.thrownWeapon instanceof ThrownWeaponEntity flying
                && flying.isReturning() && flying.distanceToSqr(p) < 20 * 20;
        boolean holding = data.heldEntityId() != -1 && s.heldEntity != null && power.hasPowers();
        on[Stance.TK_HOLD.ordinal()] = holding;
        on[Stance.TK_STRAIN.ordinal()] = holding && s.heldMass > 0.5F;
        on[Stance.MAG_SHRAPNEL.ordinal()] = data.has(HeroAbility.MAGNETO_SHRAPNEL) && data.shrapnelActive();
        on[Stance.MAG_CATCH.ordinal()] = data.has(HeroAbility.MAGNETO_SHIELD) && data.magneticShield() && s.caughtCount > 0 && !holding
                && !data.shrapnelActive();
        on[Stance.BILLY_FREEZE.ordinal()] = data.has(HeroAbility.BILLY_FREEZE) && data.is(Live.FREEZING);
        on[Stance.VISION_BEAM.ordinal()] = data.has(HeroAbility.VISION_MIND_BEAM) && data.beamActive();
        on[Stance.BEAM_WANDA.ordinal()] = data.has(HeroAbility.WANDA_CHAOS_BEAM) && data.beamActive();
        on[Stance.SHIELD_WANDA.ordinal()] = data.has(HeroAbility.WANDA_CHAOS_SHIELD) && data.chaosShieldActive();
        on[Stance.THOR_BEAM.ordinal()] = data.has(HeroAbility.THOR_LIGHTNING) && data.is(Live.CALLING_LIGHTNING);
        on[Stance.BEAM_HAND.ordinal()] = data.is(Live.BEAMING);
        on[Stance.BARRIER_BILLY.ordinal()] = data.has(HeroAbility.BILLY_FREEZE) && data.chaosShieldActive();
        if (on[Stance.BEAM_HAND.ordinal()]) {
            BeamStyle beam = BeamRenderer.styleOf(p.getId());
            // (3.6: a stone's beam leaves the hand that holds the stone, the Gauntlet or the Scepter.)
            s.beamOffHand = stoneBeam(p) ? StoneArmLayer.stoneArm(p) != p.getMainArm()
                    : beam != null && beam.offHand();
        }
        on[Stance.TOMMY_WHIRL.ordinal()] = data.has(HeroAbility.TOMMY_WHIRLWIND) && data.is(Live.WHIRLWIND);
        HeroClient hero = HeroClients.of(power);
        if (hero != null) {
            hero.stanceTargets(s, p, data, on, now);
        }
        if (isDebugTarget(p)) {
            for (int i = 0; i < on.length; i++) {
                if (DEBUG_STANCE[i] != 0) {
                    on[i] = DEBUG_STANCE[i] > 0;
                }
            }
        }
    }

    /**
     * Whether this player fires a stone's beam (3.6, the Mind or the Power Stone's): held through an item in use, whose
     * arm the beam's stance raises instead of leaving it to vanilla's pose of an item in use ({@code HeroPoses}).
     */
    static boolean stoneBeam(Player p) {
        BeamStyle beam = BeamRenderer.styleOf(p.getId());
        return (beam == StoneBeams.MIND || beam == StoneBeams.POWER) && Powers.get(p).is(Live.BEAMING);
    }

    // ====================================================================================================================
    // Frame
    // ====================================================================================================================

    /**
     * Moves a hero's state on to the frame being drawn and returns it (null when the animation does not run for them: not
     * a hero, the inventory preview, the animation switched off). A second call in the same frame changes nothing.
     */
    public static HeroAnimState frame(AbstractClientPlayer player, float yBodyRot, float pt) {
        HeroAnimState s = STATES.get(player.getUUID());
        if (s == null && !PREVIEWS.isEmpty() && (s = PREVIEWS.get(player.getUUID())) != null) {
            // The editor's puppet runs on its own clock, whatever partial tick it is drawn with.
            pt = s.previewPt;
        }
        // The inventory preview draws the player with a partial tick of 1 and its own made-up turn.
        if (s == null || pt >= 1.0F || !EnhancedClientConfig.animations()) {
            return null;
        }
        double time = s.preview != null ? s.previewClock : player.tickCount + pt;
        if (debugFrozen && isDebugTarget(player) && !s.fresh) {
            // A frozen frame: keep every weight and spring, but stay current.
            s.time = time;
            s.bodyYaw = yBodyRot;
            return s;
        }
        boolean first = s.fresh || time < s.time - 1.5;
        float dt = first ? 0.0F : (float) Mth.clamp(time - s.time, 0.0, 4.0);
        if (!first && dt <= 0.0F) {
            return s;
        }
        s.time = time;
        s.fresh = false;
        s.bodyYaw = yBodyRot;
        float age = (float) time;
        PowerData data = Powers.get(player);
        FlightProfile profile = FlightProfile.of(s);

        if (vanillaOwnsBody(player, pt)) {
            s.wVanilla = 1.0F;
            s.wAir = 0.0F;
            s.wRun = 0.0F;
            s.wSprint = 0.0F;
            java.util.Arrays.fill(s.stance, 0.0F);
            s.pitch.snap(0.0F);
            s.roll = 0.0F;
            s.addPitch = 0.0F;
            s.addRoll = 0.0F;
            s.renderPitch = 0.0F;
            s.renderRoll = 0.0F;
            s.yawAdd = 0.0F;
            s.whirlYaw = 0.0F;
            s.bob = 0.0F;
            s.drop.snap(0.0F);
            s.addDrop = 0.0F;
            s.wSpin = 0.0F;
            s.landWeight = 0.0F;
            for (HeroAnimState.Slot slot : s.slots) {
                slot.active = false;
            }
            return s;
        }
        s.wVanilla = 0.0F;
        boolean snap = first;

        measure(s, player, yBodyRot, dt, profile);

        // ---- weights ----
        boolean air = s.flyingEff && !s.onGround && s.power.canFly() && profile != null;
        float tauAirOut = s.onGround ? 1.5F : 3.0F;
        s.wAir = snap ? (air ? 1.0F : 0.0F) : AnimMath.ease(s.wAir, air ? 1.0F : 0.0F, dt, 2.0F, tauAirOut);
        boolean inWater = player.isInWater();
        boolean run = s.speeding && (s.onGround || s.airTicks < 8 || s.surfaceKind != 0) && !inWater;
        boolean sprint = s.sprinting && s.onGround && !s.speeding && !s.crouching && !inWater;
        s.wRun = snap ? (run ? 1.0F : 0.0F) : AnimMath.ease(s.wRun, run ? 1.0F : 0.0F, dt, 2.0F, 3.5F);
        s.wSprint = snap ? (sprint ? 1.0F : 0.0F) : AnimMath.ease(s.wSprint, sprint ? 1.0F : 0.0F, dt, 3.0F, 4.0F);
        for (Stance stance : Stance.VALUES) {
            int i = stance.ordinal();
            float target = s.stanceOn[i] ? (stance == Stance.WANDA_READY ? 0.7F : 1.0F) : 0.0F;
            s.stance[i] = snap ? target : AnimMath.ease(s.stance[i], target, dt, stance.tauIn, stance.tauOut);
        }

        // ---- gestures ----
        sampleSlots(s, age, dt);

        // ---- the speedster's run cycle (on a wall or a ceiling too, 3.3) ----
        SpeedSurfacePoses.advance(s, player, dt, snap);
        RunPoses.advance(s, dt, snap);
        if (s.power == Power.SPIDER_MAN || s.kitSources.contains(Power.SPIDER_MAN)) {
            SpiderPoses.advance(s, player, dt, snap);
        }
        RunPoses.Style style = RunPoses.style(s);
        shiftPhases(s);

        // ---- the lean of the body ----
        float groundPitch = (style != null ? RunPoses.runLean(s, style) * s.wRun : 0.0F) + RunPoses.sprintLean(s);
        float flightPitch = profile != null ? profile.pitchTarget(s.s, s.u) : 0.0F;
        float pitchTarget = AnimMath.lerp(s.wAir, groundPitch, flightPitch);
        if (snap) {
            s.pitch.snap(pitchTarget);
        } else if (profile != null && profile.spring && s.wAir > 0.01F) {
            s.pitch.damped(pitchTarget, profile.springOmega, profile.springZeta, dt);
        } else {
            float tauIn = profile != null ? AnimMath.lerp(s.wAir, 4.5F, profile.tauPitchIn) : 4.5F;
            float tauOut = profile != null ? AnimMath.lerp(s.wAir, 4.0F, profile.tauPitchOut) : 4.0F;
            float next = AnimMath.ease(s.pitch.x, pitchTarget, dt, tauIn, tauOut);
            s.pitch.v = (next - s.pitch.x) / Math.max(dt, 1.0E-3F);
            s.pitch.x = next;
        }
        float calm = EnhancedClientConfig.reducedMotion() ? 0.3F : 1.0F;
        float hover = (1.0F - ss(0.2F, 0.8F, s.sH)) * s.wAir;
        float flightRoll = profile != null ? profile.rollTarget(s.turn, s.sH, s.sSide) + profile.lazyRoll * calm * hover * Mth.sin(0.04F * age) : 0.0F;
        float runRoll = style != null ? AnimMath.clamp(style.kTurn * s.turn * s.runAmp, -style.maxRoll, style.maxRoll) * s.wRun : 0.0F;
        float rollTarget = AnimMath.lerp(s.wAir, runRoll, flightRoll);
        float tauRoll = profile != null ? AnimMath.lerp(s.wAir, 4.0F, profile.tauRoll) : 4.0F;
        s.roll = snap ? rollTarget : AnimMath.ease(s.roll, rollTarget, dt, tauRoll);

        // ---- the weapon ----
        advanceWeapon(s, dt, snap);

        // ---- the body's other channels ----
        float bobFlight = profile != null ? profile.bobAmp * calm * Mth.sin(Mth.TWO_PI * age / profile.bobPeriod) * hover : 0.0F;
        s.bob = bobFlight + RunPoses.bob(s) * calm;
        if (snap) {
            s.drop.snap(0.0F);
        } else {
            s.drop.damped(0.0F, 0.8F, 0.45F, dt);
        }
        advanceWhirl(s, dt);
        compose(s);
        return s;
    }

    /** Puts the pieces of the body's motion together: the turn, the lean and bank that are drawn, the pivot of the lean. */
    private static void compose(HeroAnimState s) {
        // A number gone wrong must never make a player vanish (every matrix would be NaN): start over from upright.
        if (!Float.isFinite(s.pitch.x) || !Float.isFinite(s.pitch.v) || !Float.isFinite(s.roll) || !Float.isFinite(s.drop.x)
                || !Float.isFinite(s.drop.v) || !Float.isFinite(s.bob) || !Float.isFinite(s.addPitch) || !Float.isFinite(s.addDrop)) {
            s.pitch.snap(0.0F);
            s.roll = 0.0F;
            s.drop.snap(0.0F);
            s.bob = 0.0F;
            s.addPitch = 0.0F;
            s.addDrop = 0.0F;
        }
        s.yawAdd = s.whirlYaw + s.gestureYaw + s.stanceYaw;
        s.jx += s.clingOffX;
        s.jz += s.clingOffZ;
        float addPitch = s.addPitch + Stances.lean(s);
        float addDrop = s.addDrop + Stances.drop(s);
        s.addDrop = addDrop;
        float bifrost = s.stance(Stance.THOR_BIFROST);
        s.renderPitch = (s.pitch.x + addPitch + s.surfacePitch) * (1.0F - bifrost);
        s.renderRoll = (s.roll + s.addRoll + s.ceilingRoll) * (1.0F - bifrost);
        s.pivot = AnimMath.lerp(s.landWeight, AnimMath.lerp(s.wAir, 0.40F, 0.90F), 0.40F);
        // Hanging from a ceiling the body is flipped over about its middle.
        s.pivot = AnimMath.lerp(Math.min(1.0F, Math.abs(s.ceilingRoll) / 180.0F), s.pivot, 0.90F);
        // A speedster turned onto a wall or a ceiling: about the pivot that keeps his feet on it, lifted on a wall.
        if (s.surfacePitch != 0.0F) {
            s.pivot = AnimMath.lerp(Math.min(1.0F, Math.abs(s.surfacePitch) / 20.0F), s.pivot, s.surfacePivot);
            s.bob += s.surfaceLift;
        }
    }

    /** Vanilla already turns the body of a dying, sleeping, gliding, swimming or riding player (and a spectator floats). */
    private static boolean vanillaOwnsBody(AbstractClientPlayer player, float pt) {
        return player.deathTime > 0 || player.isSleeping() || player.isFallFlying() || player.isAutoSpinAttack()
                || player.isPassenger() || player.getSwimAmount(pt) > 0.0F || player.isSpectator();
    }

    /** Takes the motion of the player from their position and body yaw into the smoothed motion and the normalised speeds. */
    private static void measure(HeroAnimState s, AbstractClientPlayer p, float yBodyRot, float dt, FlightProfile profile) {
        double dx = p.getX() - p.xOld;
        double dy = p.getY() - p.yOld;
        double dz = p.getZ() - p.zOld;
        if (dx * dx + dy * dy + dz * dz > TELEPORT * TELEPORT) {
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        float forward = (float) (-dx * Mth.sin(yaw) + dz * Mth.cos(yaw));
        float sideways = (float) (-dx * Mth.cos(yaw) - dz * Mth.sin(yaw));
        if (s.preview != null) {
            // The puppet stands still: it moves as it is said to.
            forward = previewSpeed(s, profile);
            sideways = 0.0F;
            dy = 0.0;
        }
        s.fwd = smooth(s.fwd, forward, dt, TAU_MOTION);
        s.side = smooth(s.side, sideways, dt, TAU_MOTION);
        s.climb = smooth(s.climb, (float) dy, dt, TAU_MOTION);
        s.turn = s.preview != null ? 0.0F : smooth(s.turn, Mth.wrapDegrees(p.yBodyRot - p.yBodyRotO), dt, TAU_MOTION);
        s.speed = Mth.sqrt(s.fwd * s.fwd + s.side * s.side);
        if (profile != null) {
            float fs = flightSpeed(s, profile);
            float vRef = FlightProfile.vRef(fs);
            s.s = FlightProfile.normalised(s.fwd, fs);
            s.sSide = s.side / vRef;
            s.sH = s.speed / vRef;
            s.u = AnimMath.clamp(s.climb / FlightProfile.vyRef(fs), -1.0F, 1.0F);
        } else {
            s.s = 0.0F;
            s.sSide = 0.0F;
            s.sH = 0.0F;
            s.u = 0.0F;
        }
        if (debugFly && isDebugTarget(p) && profile != null) {
            float vRef = FlightProfile.vRef(flightSpeed(s, profile));
            s.s = debugS;
            s.sSide = 0.0F;
            s.sH = Math.abs(debugS);
            s.u = debugU;
            s.turn = debugTurn;
            s.fwd = debugS * vRef;
            s.speed = Math.abs(debugS) * vRef;
        }
        s.limbSwing = p.walkAnimation.position(s.fresh ? 0.0F : (float) (s.time - p.tickCount));
        s.limbAmount = Math.min(1.0F, p.walkAnimation.speed((float) (s.time - p.tickCount)));
        if (p.isBaby()) {
            s.limbSwing *= 3.0F;
        }
    }

    /** The hero's configured flight speed (the fallback until the config has loaded); a custom hero's is his Flight's. */
    static float flightSpeed(HeroAnimState s, FlightProfile profile) {
        if (!EnhancedConfig.SPEC.isLoaded()) {
            return profile.defaultSpeed;
        }
        return s.power == Power.CUSTOM ? s.customFlightSpeed : s.power.flightSpeed();
    }

    private static float smooth(float current, float measured, float dt, float tau) {
        return dt <= 0.0F ? measured : AnimMath.ease(current, measured, dt, tau);
    }

    /** Remembers the run phase of each tick for the trail ghosts. */
    private static void shiftPhases(HeroAnimState s) {
        int tick = (int) Math.floor(s.time);
        if (tick != s.lastPhaseTick) {
            s.lastPhaseTick = tick;
            for (int i = s.phaseRing.length - 1; i > 0; i--) {
                s.phaseRing[i] = s.phaseRing[i - 1];
            }
            s.phaseRing[0] = s.runPhase;
        }
    }

    /** Thor's weapon whirls while he hovers or flies slowly, leads at speed; the spin eases up and winds down. */
    private static void advanceWeapon(HeroAnimState s, float dt, boolean snap) {
        float spinTarget = 0.0F;
        if (s.weaponKind > 0) {
            float calmSpeed = (1.0F - ss(0.35F, 0.80F, s.s)) * (1.0F - s.stance(Stance.THOR_BEAM)) * (1.0F - s.stance(Stance.THOR_BIFROST));
            if (s.wAir > 0.3F) {
                spinTarget = calmSpeed;
            }
            if (s.gestureSpin > 0.0F) {
                spinTarget = Math.max(spinTarget, s.gestureSpin);
            }
        }
        s.wSpin = snap ? spinTarget : AnimMath.ease(s.wSpin, spinTarget, dt, 3.0F, 6.0F);
        s.spinOmega = SPIN_RATE * s.wSpin;
        s.spinAngle = (s.spinAngle + s.spinOmega * dt) % 360.0F;
        float leadTarget = s.weaponKind > 0 && s.stance(Stance.THOR_BEAM) > 0.5F ? 1.0F : 0.0F;
        leadTarget = Math.max(leadTarget, s.gestureLead);
        s.leadForce = snap ? leadTarget : AnimMath.ease(s.leadForce, leadTarget, dt, 2.0F);
    }

    /** Tommy's whirlwind turns his whole body; when it ends he settles on the way he faces. */
    private static void advanceWhirl(HeroAnimState s, float dt) {
        float w = s.stance(Stance.TOMMY_WHIRL);
        if (w > 0.05F) {
            s.whirlYaw = (s.whirlYaw + 40.0F * w * dt) % 360.0F;
        } else if (s.whirlYaw != 0.0F) {
            float target = Math.round(s.whirlYaw / 360.0F) * 360.0F;
            s.whirlYaw = AnimMath.ease(s.whirlYaw, target, dt, 4.0F);
            if (Math.abs(s.whirlYaw - target) < 0.5F) {
                s.whirlYaw = 0.0F;
            }
        }
    }

    // ---- gestures ----

    /**
     * Starts a gesture on a hero: the others of its group fade out, and a free (or the weakest) slot plays it.
     * {@code param} is the gesture's own number: for the Chaos Shield's raise the shield's wind-up in ticks (the keys
     * are played at the speed that has the arms thrown open exactly then; 0 plays them as keyed), for every other
     * gesture unused.
     */
    static void start(HeroAnimState s, GestureId id, float param, float age) {
        Gestures.Def def = Gestures.get(id);
        if (def == null || startFilter != null && !com.kolodkingal.enhanced.client.custom.HeroAbilityAnims.allows(startFilter, id)) {
            return;
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.id.group == id.group && Float.isNaN(slot.releaseStart) && !Gestures.get(slot.id).hasYaw()) {
                slot.releaseFrom = slot.weight;
                slot.releaseStart = age;
            }
        }
        HeroAnimState.Slot target = null;
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active) {
                target = slot;
                break;
            }
        }
        if (target == null) {
            for (HeroAnimState.Slot slot : s.slots) {
                if (!Gestures.get(slot.id).hasYaw() && (target == null || slot.weight < target.weight)) {
                    target = slot;
                }
            }
        }
        if (target == null) {
            return;
        }
        target.id = id;
        target.start = age;
        target.releaseStart = Float.NaN;
        target.releaseFrom = 0.0F;
        target.param = param;
        target.rate = id == GestureId.WANDA_SHIELD_RAISE ? GestureRoutes.shieldRate(Math.round(param)) : 1.0F;
        target.holdTicks = 0.0F;
        target.weight = 0.0F;
        target.t = 0.0F;
        target.predicted = false;
        target.active = true;
    }

    /** Lets a playing gesture go early: it fades out over 3 ticks instead of running to its end. */
    static void release(HeroAnimState s, GestureId id, float age) {
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.id == id && Float.isNaN(slot.releaseStart)) {
                slot.releaseFrom = slot.weight;
                slot.releaseStart = age;
            }
        }
    }

    /** A deterministic noise in -1..1 for step {@code n} and an entity: the same on every client, whatever the frame rate. */
    static float noise(long n, int entityId) {
        double v = Math.sin(n * 12.9898 + entityId * 78.233) * 43758.5453;
        return (float) (2.0 * (v - Math.floor(v)) - 1.0);
    }

    /** Plays the slots for this frame: weights, and what they add to the lean, the dip, the turn, the spin and the lead. */
    private static void sampleSlots(HeroAnimState s, float age, float dt) {
        s.jx = 0.0F;
        s.jz = 0.0F;
        s.addPitch = 0.0F;
        s.addRoll = 0.0F;
        s.addDrop = 0.0F;
        s.gestureYaw = 0.0F;
        s.gestureSpin = 0.0F;
        s.gestureLead = 0.0F;
        s.landWeight = 0.0F;
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active) {
                continue;
            }
            GestureId id = slot.id;
            Gestures.Def def = Gestures.get(id);
            float t = (age - slot.start) * slot.rate;
            if (slot.holdTicks > 0.0F && t >= id.dur - id.release && Float.isNaN(slot.releaseStart)) {
                slot.start += dt;
                slot.holdTicks -= dt;
                t = (age - slot.start) * slot.rate;
            }
            float weight;
            if (!Float.isNaN(slot.releaseStart)) {
                float u = (age - slot.releaseStart) / 3.0F;
                weight = u >= 1.0F ? 0.0F : slot.releaseFrom * (1.0F - AnimMath.easeInOutSine(Math.max(u, 0.0F)));
                if (u >= 1.0F) {
                    slot.active = false;
                    continue;
                }
            } else {
                if (t >= id.dur) {
                    slot.active = false;
                    continue;
                }
                weight = AnimMath.envelope(Math.max(t, 0.0F), id.attack, id.dur, id.release);
            }
            slot.weight = weight;
            slot.t = t;
            s.addPitch += def.lean.scalar(t, 0.0F) * weight;
            s.addRoll += def.roll.scalar(t, 0.0F) * weight;
            s.addDrop += def.drop.scalar(t, 0.0F) * weight;
            if (def.hasYaw()) {
                s.gestureYaw += def.yaw.scalar(t, 0.0F);
            }
            s.gestureSpin = Math.max(s.gestureSpin, def.spin.scalar(t, 0.0F) * weight);
            s.gestureLead = Math.max(s.gestureLead, def.lead.scalar(t, 0.0F) * weight);
            if (id.group == GestureId.Group.LAND) {
                s.landWeight = Math.max(s.landWeight, weight);
            }
            if (id == GestureId.TOMMY_VIBRATE && !EnhancedClientConfig.reducedMotion()) {
                // His body shivers at 30 Hz: a few centimetres each way.
                long k = (long) Math.floor(age * 1.5F);
                s.jx += 0.035F * noise(k, s.entityId) * weight;
                s.jz += 0.035F * noise(k + 7, s.entityId) * weight;
            }
        }
    }

    // ====================================================================================================================
    // Body transform
    // ====================================================================================================================

    /**
     * Moves the pose stack (in the entity's frame: x right, y up, -z forward, already turned to its body yaw) to where the
     * hero's body is this frame: lifted by the bob, sunk by a landing, turned about its vertical, and leaned about the
     * pivot: the feet on the ground, the waist in the air.
     */
    public static void applyBody(PoseStack poseStack, HeroAnimState s, Player player) {
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float crouchFix = player.isCrouching() ? 0.125F * s.wAir : 0.0F;
        float dy = s.bob - s.drop.x - s.addDrop + crouchFix;
        if (Math.abs(dy) > 1.0E-4F || s.jx != 0.0F || s.jz != 0.0F) {
            poseStack.translate(s.jx, dy, s.jz);
        }
        if (Math.abs(s.yawAdd) > 1.0E-3F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-s.yawAdd * side));
        }
        if (Math.abs(s.renderPitch) < EPSILON && Math.abs(s.renderRoll) < EPSILON) {
            return;
        }
        poseStack.translate(0.0F, s.pivot, 0.0F);
        // Rolled about the way the hero faces, then pitched about the body's own sideways axis.
        poseStack.mulPose(Axis.ZP.rotationDegrees(-s.renderRoll));
        poseStack.mulPose(Axis.XP.rotationDegrees(-s.renderPitch));
        poseStack.translate(0.0F, -s.pivot, 0.0F);
    }

    /**
     * Moves a point of the upright body (a world position) to where this frame's body has put it, so that energy leaves the
     * head or hand that is drawn. A body that has not been drawn lately (the local player in first person) leaves it be.
     */
    public static Vec3 follow(Player player, Vec3 point, float partialTick) {
        HeroAnimState s = peek(player);
        if (s == null || s.wVanilla > 0.0F || Math.abs(player.tickCount + partialTick - s.time) > 2.0F) {
            return point;
        }
        if (Math.abs(s.renderPitch) < EPSILON && Math.abs(s.renderRoll) < EPSILON && Math.abs(s.bob) < 1.0E-4F
                && Math.abs(s.drop.x + s.addDrop) < 1.0E-4F && Math.abs(s.yawAdd) < 1.0E-3F) {
            return point;
        }
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        Vec3 feet = player.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        // The body is drawn in a frame turned to its yaw: turn there, move the body, and turn back.
        Quaternionf yaw = Axis.YP.rotationDegrees(180.0F - bodyYaw);
        Quaternionf inverse = yaw.conjugate(new Quaternionf());
        Vector3f v = new Vector3f((float) (point.x - feet.x), (float) (point.y - feet.y), (float) (point.z - feet.z));
        inverse.transform(v);
        // Same chain as applyBody, applied to the point (outermost first, so innermost last).
        v.y -= s.pivot;
        v.rotateX(-s.renderPitch * Mth.DEG_TO_RAD).rotateZ(-s.renderRoll * Mth.DEG_TO_RAD);
        v.y += s.pivot;
        v.rotateY(-s.yawAdd * side * Mth.DEG_TO_RAD);
        v.add(s.jx, s.bob - s.drop.x - s.addDrop, s.jz);
        yaw.transform(v);
        return feet.add(v.x, v.y, v.z);
    }

    // ====================================================================================================================
    // Public API for the branches that add moves
    // ====================================================================================================================

    // ---- the editor's preview puppet (3.4: stubs of S0; L5 makes them play, L6 calls them) ----

    /** How the editor's preview puppet moves: what its measured motion is said to be instead of sampled from positions. */
    public enum PreviewMotion {
        IDLE, WALK, RUN, SPEED_RUN, FLY_HOVER, FLY_CRUISE, FLY_SPRINT
    }

    /**
     * Sets the motion of the puppet's state for this frame (the puppet stands still; its pose moves), and moves its own clock
     * on: call it once every frame the puppet is drawn, before it is drawn. The puppet is a player outside the world (the
     * editor's), whose attachments say what it is: its power data, and for a custom hero his definition and state. Its
     * {@code tickCount} and walk are set here from the clock; draw it with {@link #previewPartialTick} (a partial tick of 1,
     * as the inventory's preview passes, works too: the animation then takes its own).
     */
    public static void preview(AbstractClientPlayer puppet, PreviewMotion motion, float partialTick) {
        if (!EnhancedClientConfig.animations()) {
            PREVIEWS.remove(puppet.getUUID());
            return;
        }
        PowerData data = Powers.get(puppet);
        HeroAnimState s = PREVIEWS.get(puppet.getUUID());
        long nanos = System.nanoTime();
        if (s == null || s.power != data.power()) {
            s = new HeroAnimState(puppet.getUUID());
            s.reseed(data.power());
            s.previewNanos = nanos;
            s.previewClock = puppet.tickCount;
            PREVIEWS.put(puppet.getUUID(), s);
        }
        s.entityId = puppet.getId();
        s.preview = motion;
        // 20 ticks a second of real time (at most 4 at once, after a pause).
        s.previewClock += Mth.clamp((nanos - s.previewNanos) / 5.0E7, 0.0, 4.0);
        s.previewNanos = nanos;
        long tick = (long) Math.floor(s.previewClock);
        s.previewPt = (float) (s.previewClock - tick);
        if (s.previewTick == Long.MIN_VALUE || tick - s.previewTick > 4) {
            s.previewTick = tick - 1;
        }
        while (s.previewTick < tick) {
            s.previewTick++;
            puppet.tickCount = (int) s.previewTick;
            previewTick(s, puppet, data);
        }
    }

    /** The partial tick the puppet's clock is at (draw it with this one). */
    public static float previewPartialTick(AbstractClientPlayer puppet) {
        HeroAnimState s = PREVIEWS.get(puppet.getUUID());
        return s == null ? 0.0F : s.previewPt;
    }

    /** The puppet is gone (the editor closed): forget its state. */
    public static void previewEnd(AbstractClientPlayer puppet) {
        PREVIEWS.remove(puppet.getUUID());
    }

    /**
     * Plays on the puppet exactly what a cast marker of binding ({@code column} 1-5, plain or with Sneak) plays; where its
     * hero ability is acted by a route of its hero (a toggle, a state), the gesture that route plays.
     */
    public static void previewCast(AbstractClientPlayer puppet, int column, boolean sneak) {
        HeroAnimState s = PREVIEWS.get(puppet.getUUID());
        PowerData data = Powers.get(puppet);
        if (s == null || data.power() != Power.CUSTOM) {
            return;
        }
        com.kolodkingal.enhanced.custom.HeroKit kit = CustomActing.kit(puppet, data);
        com.kolodkingal.enhanced.custom.CustomHero hero = kit.hero();
        com.kolodkingal.enhanced.custom.HeroKit.Binding binding = sneak ? kit.sneak(column) : kit.plain(column);
        if (binding == null && sneak) {
            binding = kit.plain(column);
            sneak = false;
        }
        if (binding == null) {
            return;
        }
        GestureId id = com.kolodkingal.enhanced.client.custom.CustomAnimRoutes.cast(hero, kit, column, sneak, true);
        HeroAbility row = binding.ability();
        if (id == null && row != null && !com.kolodkingal.enhanced.client.custom.CustomAnimRoutes.chosen(hero, column, sneak)) {
            id = com.kolodkingal.enhanced.client.custom.HeroAbilityAnims.shown(row, binding.isAlt());
        }
        if (id != null) {
            start(s, id, id == GestureId.WANDA_SHIELD_RAISE ? GestureRoutes.SHIELD_KEYED_WIND_UP : 0.0F, (float) s.previewClock);
        }
    }

    /** Raises (or lowers) on the puppet the stance of a held or switched-on power. */
    public static void previewHold(AbstractClientPlayer puppet, com.kolodkingal.enhanced.custom.CatalogEntry entry, boolean on) {
        HeroAnimState s = PREVIEWS.get(puppet.getUUID());
        PowerData data = Powers.get(puppet);
        if (s == null || data.power() != Power.CUSTOM) {
            return;
        }
        Stance stance = com.kolodkingal.enhanced.client.custom.CustomAnimRoutes.previewStance(CustomActing.kit(puppet, data).hero(), entry);
        if (stance != null) {
            s.previewHeld[stance.ordinal()] = on;
        }
    }

    /** One tick of the puppet: what the tick samples for a hero in the world, made up from the motion it is said to have. */
    private static void previewTick(HeroAnimState s, AbstractClientPlayer puppet, PowerData data) {
        PreviewMotion m = s.preview;
        CustomActing.identity(s, puppet, data);
        boolean fly = m == PreviewMotion.FLY_HOVER || m == PreviewMotion.FLY_CRUISE || m == PreviewMotion.FLY_SPRINT;
        FlightProfile profile = FlightProfile.of(s);
        float speed = previewSpeed(s, profile);
        shift(s.dy, 0.0F);
        shift(s.hv, speed);
        shift(s.sRing, s.s);
        s.prevAirTicks = s.airTicks;
        s.wasOnGround = s.onGround;
        s.onGround = !fly;
        s.airTicks = fly ? s.airTicks + 1 : 0;
        s.groundTicks = fly ? 0 : s.groundTicks + 1;
        s.prevFlyingEff = s.flyingEff;
        s.flyingEff = fly;
        s.sinceFlying = fly ? 0 : Math.min(100, s.sinceFlying + 1);
        s.sprinting = m == PreviewMotion.RUN;
        s.crouching = false;
        s.speeding = m == PreviewMotion.SPEED_RUN;
        s.slowMo = false;
        s.walkman = false;
        s.wallRunning = false;
        s.surfaceKind = 0;
        if (s.speeding) {
            s.styleKind = data.power() == Power.CUSTOM ? CustomActing.styleKind(s, puppet, data)
                    : data.power() == Power.TOMMY ? 3 : 2;
        } else {
            s.styleKind = 0;
        }
        if (s.seeded && profile != null && s.flyingEff && !s.prevFlyingEff) {
            // The take-off of the flight style (without its effects: the puppet is not in the world).
            HeroClient hero = HeroClients.of(s.flightAs);
            GestureId own = switch (s.flightAs) {
                case MAGNETO -> GestureId.TAKEOFF_MAGNETO;
                case SCARLET_WITCH -> GestureId.TAKEOFF_WANDA;
                case VISION -> GestureId.TAKEOFF_VISION;
                case BILLY -> GestureId.TAKEOFF_BILLY;
                default -> hero == null || hero.takeoffGesture() == null ? GestureId.TAKEOFF_THOR : hero.takeoffGesture();
            };
            start(s, own, 0.0F, (float) s.previewClock);
        }
        boolean[] on = s.stanceOn;
        java.util.Arrays.fill(on, false);
        for (int i = 0; i < on.length; i++) {
            on[i] = s.previewHeld[i];
        }
        if (m == PreviewMotion.IDLE) {
            CustomActing.idle(s, on);
        }
        CustomActing.rowHeroes(s, on);
        // The walk of the model (vanilla's swing of the arms and legs), as a walking player's.
        float walk = switch (m) {
            case WALK -> 0.8F;
            case RUN, SPEED_RUN -> 1.0F;
            default -> 0.0F;
        };
        puppet.walkAnimation.update(walk, 0.4F);
        s.seeded = true;
    }

    /** How fast (blocks a tick, forward) the puppet is said to move. */
    private static float previewSpeed(HeroAnimState s, FlightProfile profile) {
        float vRef = profile == null ? 1.0F : FlightProfile.vRef(flightSpeed(s, profile));
        return switch (s.preview) {
            case WALK -> 0.215F;
            case RUN -> 0.28F;
            case SPEED_RUN -> 1.2F;
            case FLY_CRUISE -> 1.0F * vRef;
            case FLY_SPRINT -> 2.0F * vRef;
            default -> 0.0F;
        };
    }

    /** Plays a gesture on a hero, ignoring its usual trigger (the hook of the branches that add moves). */
    public static void play(Player player, GestureId id, float param) {
        HeroAnimState s = peek(player);
        if (s != null) {
            start(s, id, param, player.tickCount);
        }
    }

    /** Holds the last pose of a gesture that is playing for this many more ticks (a pin held while it lasts). */
    public static void hold(Player player, GestureId id, int ticks) {
        HeroAnimState s = peek(player);
        if (s == null) {
            return;
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.id == id) {
                slot.holdTicks = Math.max(slot.holdTicks, ticks);
            }
        }
    }

    /** How strong Thor's lightning beam is: 1.7 with his weapon in a hand, 1.0 bare-handed. */
    public static float beamPower(Player player) {
        HeroAnimState s = peek(player);
        return s != null ? beamPower(s) : (ThorAbilities.holdsWeapon(player) ? 1.7F : 1.0F);
    }

    static float beamPower(HeroAnimState s) {
        return s.weaponKind > 0 ? 1.7F : 1.0F;
    }

    /**
     * Thor brings Mjolnir down on someone (the slam; where it landed is remembered for the arms that aim at it), or the
     * Bifrost's arrival: the hero lands on both feet with the weapon planted.
     */
    public static void onThorFx(ThorFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(payload.entityId()) instanceof AbstractClientPlayer player)) {
            return;
        }
        HeroAnimState s = peek(player);
        if (s == null) {
            return;
        }
        if (payload.kind() == ThorFxPayload.PIN && !payload.points().isEmpty()) {
            s.strikeTarget = payload.points().get(0);
            s.strikeTick = level.getGameTime();
            start(s, GestureId.THOR_PIN, 0.0F, player.tickCount);
        } else if (payload.kind() == ThorFxPayload.BIFROST_ARRIVAL) {
            start(s, GestureId.THOR_ARRIVAL, 0.0F, player.tickCount);
            FlightEffects.impact(player, s, 1.0F);
        }
    }

    /** A zip or a swing begins: the arm of his web aims at its anchor while he holds on. */
    public static void onSpiderFx(int playerId, Vec3 anchor) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(playerId) instanceof AbstractClientPlayer player) {
            HeroAnimState s = peek(player);
            if (s != null) {
                s.anchor = anchor;
            }
        }
    }

    /** A Hex Bolt was cast: the caster's casting hand aims at where it struck. */
    public static void onHexBolt(int casterId, Vec3 to) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(casterId) instanceof AbstractClientPlayer caster) {
            HeroAnimState s = peek(caster);
            if (s != null) {
                s.boltTarget = to;
                s.boltTick = level.getGameTime();
            }
        }
    }

    /**
     * The local player pressed the key of an ability: start its gesture now instead of a network round trip later, where
     * the client's own copy of the data says it will work (the shield, the Hex, Billy's bolt, Loki's daggers, Thor's throw).
     * The change in the data that arrives a moment later is swallowed so the gesture does not play twice.
     */
    public static void predict(LocalPlayer player, int ability) {
        HeroAnimState s = peek(player);
        if (s == null || s.wVanilla > 0.0F || !EnhancedClientConfig.animations()) {
            return;
        }
        if (s.power == Power.SCARLET_WITCH && ability == 4 && !WandaAbilities.canAffordShield(player)) {
            // The server refuses a shield she cannot pay for: do not raise her arms for nothing.
            return;
        }
        PowerData data = Powers.get(player);
        boolean ownHex = s.table.hasHex;
        GestureId id = GestureTable.predicted(s, data, ability, player.level().getGameTime(), ownHex, player.isShiftKeyDown());
        if (id == null) {
            return;
        }
        float param = 0.0F;
        if (id == GestureId.WANDA_SHIELD_RAISE) {
            // The wind-up the server will give it (the config), so the arms open as the dome completes.
            param = WandaAbilities.windUpTicks();
            id = GestureRoutes.shieldRaise(Math.round(param));
        }
        start(s, id, param, player.tickCount);
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.id == id && slot.start == player.tickCount) {
                slot.predicted = true;
            }
        }
        GestureEffects.started(player, s, id, param);
    }

    // ---- the debugging tools ----

    /** Pretends the motion of the local player (or stops pretending): speed, climb and turn as the pose curves see them. */
    public static void debugFly(boolean on, float s, float u, float turn) {
        debugFly = on;
        debugS = s;
        debugU = u;
        debugTurn = turn;
    }

    /** Whether the debugging tools act on this player (the local player unless {@link #debugTarget(String)} named another). */
    private static boolean isDebugTarget(Player player) {
        return debugTarget == null ? player == Minecraft.getInstance().player : player.getUUID().equals(debugTarget);
    }

    private static Player debugPlayer() {
        Minecraft mc = Minecraft.getInstance();
        return debugTarget == null || mc.level == null ? mc.player : mc.level.getPlayerByUUID(debugTarget);
    }

    /** Points the debugging tools at another player in view by name (so a second client can photograph a frozen pose from any side). */
    public static boolean debugTarget(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (name == null || name.equals("self")) {
            debugTarget = null;
            return true;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(name)) {
                debugTarget = player.getUUID();
                return true;
            }
        }
        return false;
    }

    /** Starts a gesture on the local player, ignoring its trigger. */
    public static void debugPlay(GestureId id) {
        Player player = debugPlayer();
        HeroAnimState s = player == null ? null : peek(player);
        if (s != null) {
            for (HeroAnimState.Slot slot : s.slots) {
                slot.active = false;
            }
            start(s, id, 0.0F, player.tickCount);
        }
    }

    /** Jumps every playing gesture of the local player to this tick and freezes their animation. */
    public static void debugHold(int ticks) {
        Player player = debugPlayer();
        HeroAnimState s = player == null ? null : peek(player);
        if (s == null) {
            return;
        }
        debugFrozen = false;
        float age = (float) s.time;
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active) {
                slot.start = age - ticks / slot.rate;
            }
        }
        sampleSlots(s, age, 0.0F);
        compose(s);
        debugFrozen = true;
    }

    public static void debugRelease() {
        debugFrozen = false;
    }

    /** Forces a stance of the debug target on or off (or, with {@code null}, hands them all back to the data). */
    public static void debugStance(Stance stance, Boolean on) {
        if (stance == null) {
            java.util.Arrays.fill(DEBUG_STANCE, (byte) 0);
        } else {
            DEBUG_STANCE[stance.ordinal()] = (byte) (on == null ? 0 : on ? 1 : -1);
        }
    }

    private HeroAnimator() {
    }
}
