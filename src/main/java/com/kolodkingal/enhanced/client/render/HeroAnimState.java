package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Everything remembered about one hero between frames: the motion measured from their position, the smoothed weights of
 * every layer of the pose, the springs of the body's lean, the stances and gestures, and what was seen of their synced
 * data last tick (to find the edges). {@link HeroAnimator} advances it once per frame (and once per tick for what needs
 * the tick); nothing here allocates per frame.
 */
public final class HeroAnimState {
    /** One playing gesture. */
    public static final class Slot {
        public GestureId id;
        /** Game age (ticks plus partial tick) the gesture started at. */
        public float start;
        /** When it was cut short: the weight it was fading from and the age the fade started; NaN while it plays on. */
        public float releaseFrom;
        public float releaseStart = Float.NaN;
        public float param;
        /**
         * How fast the gesture's own clock runs against the game's: 1 plays it as keyed, 0.5 twice as slowly. Only the
         * Chaos Shield's raise is ever stretched (to the shield's wind-up, so the arms open as the dome completes).
         */
        public float rate = 1.0F;
        /** Ticks the last pose is still to be held ({@code HeroAnimator.hold}). */
        public float holdTicks;
        /** Set by the frame: the weight and the time into the gesture. */
        public float weight;
        public float t;
        public boolean predicted;
        public boolean active;
    }

    public static final int SLOTS = 3;

    // ---- identity ----
    public final UUID uuid;
    public int entityId;
    public Power power = Power.NONE;
    public String look = "";
    /** The first frame snaps every weight and spring to its target. */
    public boolean fresh = true;
    /** The age (ticks plus partial tick) of the last frame update; -1 before the first. */
    public double time = -1.0;
    /** Whether the tick has looked at this hero yet (the first tick only remembers). */
    public boolean seeded;
    /**
     * Whether the model being drawn right now was posed from this state ({@link HeroPoses#apply} sets it for every draw). It is
     * false for the inventory's preview of the player, which the layers must draw as vanilla does: no whirl, no tracked points.
     */
    public boolean posed;

    // ---- the custom hero's animation identity (3.4; L5 fills and reads these) ----
    /** The custom hero's definition last seen (identity), to reseed when it changes; null for a built-in hero. */
    @Nullable
    public Object kitStamp;
    /** The built-in heroes whose abilities the custom hero has (empty for a built-in hero). */
    public final EnumSet<Power> kitSources = EnumSet.noneOf(Power.class);
    /** Whose flight profile, take-off and flight effects he flies with ({@code NONE}: the plain flight); the hero for a built-in. */
    public Power flightAs = Power.NONE;
    /** With {@link #flightAs} VISION: White Vision's. */
    public boolean flightWhite;
    /** Whose run character he walks and sprints with (a {@code AnimCatalog.RUN_CHARACTERS} name; empty: the hero's own). */
    public String runAs = "";
    /** The speedster run style a custom hero uses while he speeds: 0 none, 1 QS_TIME, 2 QS_SPEED, 3 TOMMY. */
    public int speedStyle;
    /** The idle stance at rest, or null. */
    @Nullable
    public Stance idle;
    /** The custom hero's cast marker last seen ({@code CustomState.castSeq}); -1 before the first. */
    public int lastCastSeq = -1;
    /** The custom hero's switched-on modules last seen ({@code CustomState.onBits}). */
    public int lastCustomOn;
    /**
     * The heroes whose plug-in rows (poses, lean, dip) are drawn for a custom hero this tick: those his abilities come from,
     * and the owners of the stances he has raised (a stance he chose of another hero's).
     */
    public final EnumSet<Power> rowHeroes = EnumSet.noneOf(Power.class);
    /** A custom hero's speed in the air (his Flight's strength), in the units of {@code Power.flightSpeed}. */
    public float customFlightSpeed = 0.05F;
    /** The ability of the last binding the cast marker told, and the age it was told at. */
    @Nullable
    public com.kolodkingal.enhanced.custom.HeroAbility lastCastRow;
    public float lastCastAge = -1000.0F;
    /**
     * Until when (age) each hero ability's own routes stay quiet: its binding has a move of the definition's, which the
     * marker played instead (by {@code HeroAbility} ordinal).
     */
    public final float[] rowQuietUntil = new float[com.kolodkingal.enhanced.custom.HeroAbility.values().length];

    // ---- the editor's preview puppet (3.4) ----
    /** How the puppet is said to move, or null for a hero in the world. */
    @Nullable
    public HeroAnimator.PreviewMotion preview;
    /** The puppet's own clock (ticks, with the fraction): it lives outside the world, so it has no game time. */
    public double previewClock;
    /** The nanosecond time of the last preview call (to advance the clock), and the whole tick last run. */
    public long previewNanos;
    public long previewTick = Long.MIN_VALUE;
    /** The stances the preview holds up ("Try" of a held or switched-on power). */
    public final boolean[] previewHeld = new boolean[Stance.COUNT];

    // ---- tick samples ----
    /** Vertical motion of the last three ticks, newest first (blocks per tick) and the horizontal speed of the last four. */
    public final float[] dy = new float[3];
    public final float[] hv = new float[4];
    /** The normalised speed {@link #s} of the last four ticks, newest first. */
    public final float[] sRing = new float[4];
    public int airTicks;
    public int prevAirTicks;
    public int groundTicks;
    /** Ticks since the hero last flew, saturating at 100. */
    public int sinceFlying = 100;
    public boolean flyingEff;
    public boolean prevFlyingEff;
    public boolean wasOnGround = true;
    public boolean onGround = true;
    public boolean sprinting;
    public boolean crouching;
    public boolean speeding;
    /** The speedster's run style: 0 none, 1 Quicksilver Time, 2 Super Speed, 3 Tommy. */
    public int styleKind;
    public boolean wallRunning;
    public boolean walkman;
    public boolean slowMo;

    // ---- measured motion (frame, smoothed) ----
    /** Blocks per tick forward, to the right, and up; the body's turn in degrees per tick (right positive). */
    public float fwd;
    public float side;
    public float climb;
    public float turn;
    /** The horizontal speed (blocks per tick). */
    public float speed;
    /** The speeds normalised to the hero's flight: 1 is cruise, 2 a sprint. {@link #u} is the climb, -1..1. */
    public float s;
    public float sSide;
    public float sH;
    public float u;
    /** The vanilla walk phase and amount of this frame. */
    public float limbSwing;
    public float limbAmount;
    /** The body's yaw of this frame (degrees) and the height of the lean's pivot (blocks). */
    public float bodyYaw;
    public float pivot = 0.4F;

    // ---- weights ----
    public float wAir;
    public float wRun;
    public float wSprint;
    /** 1 while vanilla owns the body (swimming, gliding, sleeping, riding): nothing here is applied. */
    public float wVanilla;
    public final float[] stance = new float[Stance.COUNT];
    public final boolean[] stanceOn = new boolean[Stance.COUNT];
    /** The weight of the landing gestures: the lean pivots about the feet then. */
    public float landWeight;

    // ---- renderer channels ----
    /** The lean spring (degrees, forward positive) without what stances and gestures add. */
    public final AnimMath.Spring pitch = new AnimMath.Spring();
    public float roll;
    /** What the stances and gestures add to the lean, in degrees, and the pitch and roll actually drawn. */
    public float addPitch;
    public float addRoll;
    public float renderPitch;
    public float renderRoll;
    /** An extra turn of the whole body about its vertical (degrees, right positive) and its rate. */
    public float yawAdd;
    /** The turn of Tommy's whirlwind and the turn of the playing gestures, summed into {@link #yawAdd}. */
    public float whirlYaw;
    public float gestureYaw;
    public float bob;
    /** The landing dip (blocks, positive sinks), plus what gestures and stances add. */
    public final AnimMath.Spring drop = new AnimMath.Spring();
    public float addDrop;
    public float jx;
    public float jz;

    // ---- running ----
    public float runPhase;
    public float runOmega;
    public float runAmp;
    public int lastPhaseTick = Integer.MIN_VALUE;
    /** The run phases of the last six ticks (newest first), for the trail ghosts. */
    public final float[] phaseRing = new float[6];

    // ---- Thor's weapon ----
    public float wSpin;
    public float spinAngle;
    public float spinOmega;
    public float leadForce;
    /** How strongly a playing gesture asks for the spin or the lead this frame (0..1). */
    public float gestureSpin;
    public float gestureLead;

    // ---- first person ----
    /** The age at which the first-person hands were last moved ({@link FirstPersonArms}). */
    public double fpAge = Double.NEGATIVE_INFINITY;

    // ---- Spider-Man ----
    /** The end of his web while he zips or swings (the aim of the stances SPIDER_ZIP and SPIDER_SWING), or null. */
    public Vec3 anchor;
    /** The horizontal unit vector from him to the wall he clings to (zero before he has clung to one). */
    public Vec3 wallDir = Vec3.ZERO;
    /** His speed along the surface he clings to (blocks per tick, this tick), and the cycle of his hands and knees. */
    public float crawlSpeed;
    public float crawlPhase;
    public float crawlOmega;
    public float crawlAmp;
    /** The yaw (degrees) his body faces while it is turned to a wall or an anchor, and whether it has been set. */
    public float faceYaw;
    public boolean faceSet;
    /** What the cling adds to the body: the turn to the wall (in {@link #yawAdd}'s signs), the ceiling's flip, the offset. */
    public float stanceYaw;
    public float ceilingRoll;
    public float clingOffX;
    public float clingOffZ;

    // ---- a speedster on a wall or a ceiling (3.3, SpeedSurfacePoses) ----
    /** What he runs on this tick: 0 the ground, 1 up a wall, 2 a ceiling, 3 down a wall. */
    public int surfaceKind;
    /** Ticks another player's surface flags have been down while he was still drawn on a surface (see SpeedSurfacePoses). */
    public int surfaceLostTicks;
    /** The wall's normal (from the wall towards him), or the ceiling run's heading (horizontal unit vectors). */
    public net.minecraft.world.phys.Vec3 surfaceNormal = net.minecraft.world.phys.Vec3.ZERO;
    public net.minecraft.world.phys.Vec3 surfaceHeading = net.minecraft.world.phys.Vec3.ZERO;
    /** His speed along the surface (blocks per tick, this tick). */
    public float surfaceSpeed;
    /** The eased pitch (degrees) that turns the body onto the surface: -90 up a wall, -180 on a ceiling, +90 down a wall. */
    public float surfacePitch;
    /** How much the body faces {@link #surfaceYaw} (eased), and that yaw. */
    public float surfaceFace;
    public float surfaceYaw;
    /** The lift (blocks) and the pivot of the turn onto the surface, so the feet are on it. */
    public float surfaceLift;
    public float surfacePivot;

    // ---- gestures ----
    public final Slot[] slots = new Slot[SLOTS];

    // ---- edge memory ----
    public int lastHeld = -1;
    public int lastHammer = -1;
    public boolean lastBeam;
    public boolean lastShield;
    public boolean lastShrapnel;
    public boolean lastSpeeding;
    public long spellEndedAt = Long.MIN_VALUE / 2;
    public long lastBrakeAt = Long.MIN_VALUE / 2;
    public long lastBoomAt = Long.MIN_VALUE / 2;
    public long lastFastAt = Long.MIN_VALUE / 2;
    public boolean lastWasFast;

    /** What the gesture table saw of the data last tick, to find the edges. */
    public final GestureTable.Memory table = new GestureTable.Memory();

    // ---- world references (set each tick) ----
    public Entity heldEntity;
    public Entity thrownWeapon;
    public float heldMass;
    public Vec3 lastWeaponPos;
    public long lastWeaponTick;
    /** Where the thrown weapon was in the last four ticks (newest first), and where it was just before it came home. */
    public final Vec3[] weaponTrail = new Vec3[4];
    public Vec3 catchFrom;
    public final Vec3[] caught = new Vec3[8];
    public int caughtCount;
    public Vec3 boltTarget;
    public long boltTick;
    /** 3.2: the continuous beam this player fires leaves the off hand (the pose aims that arm). */
    public boolean beamOffHand;
    public Vec3 strikeTarget;
    public long strikeTick;
    /** Thor holds a weapon: 0 none, 1 Mjolnir, 2 Stormbreaker (as the tick last saw it). */
    public int weaponKind;
    public boolean weaponInMainHand = true;
    /** The tick the weapon was last seen in a hand: for a few ticks after it is thrown, the throwing arm is still the one that held it. */
    public int weaponHandTick = Integer.MIN_VALUE / 2;

    public HeroAnimState(UUID uuid) {
        this.uuid = uuid;
        for (int i = 0; i < SLOTS; i++) {
            slots[i] = new Slot();
        }
    }

    public float stance(Stance stance) {
        return this.stance[stance.ordinal()];
    }

    /** Clears everything tied to the hero's power (a new power, or a new world). */
    public void reseed(Power power) {
        this.power = power;
        for (Slot slot : slots) {
            slot.active = false;
            slot.id = null;
        }
        java.util.Arrays.fill(stance, 0.0F);
        java.util.Arrays.fill(stanceOn, false);
        table.clear();
        catchFrom = null;
        lastCastSeq = -1;
        lastCastRow = null;
        java.util.Arrays.fill(rowQuietUntil, 0.0F);
        fresh = true;
        seeded = false;
        time = -1.0;
    }
}
