package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.SpiderClimb;
import com.kolodkingal.enhanced.ability.SpiderMath;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.SpiderWebs;
import com.kolodkingal.enhanced.ability.SwingAssist;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.network.SpiderClingPayload;
import com.kolodkingal.enhanced.network.SpiderFxPayload;
import com.kolodkingal.enhanced.network.SpiderSwingPayload;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.InputEvent;
import com.kolodkingal.enhanced.platform.event.client.MovementInputUpdateEvent;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Spider-Man's own movement, done by the player's own client like Quicksilver's wall-run, because vanilla lets a client
 * move itself and the server only checks (DESIGN section 14): clinging to a wall or a ceiling and walking along it, the
 * pull of a Web Zip, and the pendulum of a Web Swing. The server decides what others see (the {@code Live} bits),
 * places the webs, and resets the "flying is not enabled" counter; it never moves him.
 *
 * <p>The rules are the pure ones of {@link SpiderClimb} and {@link SpiderMath}, the same code the server and the game
 * tests run. While he clings, zips or swings, gravity does not apply because his whole velocity is set at the start of
 * every tick; the keys that would move him are cleared on their way into the game (see {@link #onMovementInput}) after
 * being read from the key bindings, so vanilla neither pushes nor jumps him.
 *
 * <p>A zip or swing begins at once when the key is pressed, on this client's own aim (<em>prediction</em>), and the
 * server's answer ({@link #onFx}) confirms it, or takes over if its anchor differs; an answer that never comes (the Hex
 * stopped the web, a shield took it) cancels the prediction after a few ticks. A zip the server has seen to its end
 * (arrived) is finished by the client alone: the server calls it arrived a block short, to let others see him cling at
 * once.
 *
 * <p>Web Assist (3.3, on by default, {@link EnhancedClientConfig#webAssist}): the swing's anchor is chosen by
 * {@link SwingAssist} when G is pressed, and told to the server at that very moment ({@link SpiderSwingPayload}, before
 * the ability key), which checks it and confirms it like a manual swing. While it swings the rope is reeled in near the
 * ground, the swing is pushed along and turns gently after the camera, it lets go at its best moment, and while G is
 * still held the next web goes out as he begins to fall; between two webs the air's drag is eased so that he keeps his
 * speed. The anchor it would take next is shown ({@link #preview}).
 */
@Subscriber(side = Side.CLIENT)
public final class SpiderMotion {
    /** What he is doing with his own movement. */
    public enum Mode {
        NONE, CLING, ZIP, SWING
    }

    /** The reasons a zip or a swing ends (the codes of {@link SpiderFxPayload}). */
    private static final int ARRIVED = 0;
    private static final int CANCELLED = 1;
    private static final int REFUSED = 2;
    private static final int BLOCKED = 3;
    private static final int TIMEOUT = 4;
    private static final int RELEASED = 5;
    private static final int ANCHOR_LOST = 6;

    /** A prediction the server has not confirmed within this many ticks is dropped. */
    private static final int PREDICTION_TICKS = 12;
    /** Vanilla sends the player's position when it moved further than this (squared) since the last one. */
    private static final double VANILLA_SENDS = 2.0E-4;
    /** A zip has arrived this near its stand point (blocks). */
    private static final double ARRIVE = 0.45;
    /** A zip the server has called arrived goes on this many ticks at most to get there. */
    private static final int FINISH_TICKS = 12;
    /** A cling cannot be left by a leap before this many ticks. */
    private static final int LEAP_DELAY = 4;
    /** The contact with the surface is lost for this many ticks before he lets go. */
    private static final int LOST_TICKS = 4;
    /** The walk onto the top of a block he climbed: ticks, and blocks a tick. */
    private static final int MANTLE_TICKS = 6;
    private static final double MANTLE_PUSH = 0.2;
    /** A swing begun on the ground hops him off it. */
    private static final double SWING_HOP = 0.35;
    private static final double SWING_PUSH = 0.3;
    /** The shortest rope a swing is reeled in to, and how much longer than at first it is let out to. */
    private static final double MIN_ROPE = 3.0;
    private static final double MAX_ROPE_FACTOR = 1.25;
    private static final double REEL = 0.12;
    private static final double AIR_CONTROL = 0.02;
    private static final double SWING_DRAG = 0.992;
    /** The speed factor of a plain Spider-Man: his swing is at its configured speed with it. */
    private static final double PLAIN_FACTOR = 1.15;
    /** A swing on the ground this long is let go of (after its first ticks). */
    private static final int SWING_GRACE = 10;
    /** What he clings to is told to the server again this often while it lasts (the server believes it for 12 ticks). */
    private static final int REPORT_TICKS = 8;
    /** A zip that has just arrived is not let go of for this many ticks, and is drawn in to the wall at this speed. */
    private static final int SETTLE_TICKS = 8;
    private static final double SETTLE_PULL = 0.2;

    private static final SpiderClimb.Surface CEILING = new SpiderClimb.Surface(SpiderClimb.Kind.CEILING, Direction.DOWN);

    private static LocalPlayer owner;
    private static Mode mode = Mode.NONE;
    private static boolean jumpWas;
    /** Where the hero last was when the position was sent to the server (while he clings). */
    @Nullable
    private static Vec3 lastSent;
    /** A press of G, seen as it happened, to be acted on by the next tick of the player. */
    private static boolean pressedG;
    private static boolean pressedGSneaking;
    /** Until when a second press of Sneak + B turns Instant Kill on (a game time). */
    private static long confirmUntil;
    /** What the server was last told he clings to ({@link SpiderClingPayload}), and when (his ticks); -1 before the first word. */
    private static int toldKind = -1;
    private static int toldNormal;
    private static int toldAt;
    /** A zip has just arrived and he is being drawn in to the wall: nothing is lost for a few ticks. */
    private static boolean settling;

    // ---- cling ----
    private static SpiderClimb.Surface surface = SpiderClimb.Surface.NONE;
    private static int clingAge;
    private static int lost;
    private static int groundTicks;
    private static Vec3 lastVelocity = Vec3.ZERO;
    private static int restickUntil;
    private static SpiderClimb.Surface mantleWall = SpiderClimb.Surface.NONE;
    private static int mantleTicks;

    // ---- zip ----
    private static Vec3 zipAnchor = Vec3.ZERO;
    private static Vec3 zipStand = Vec3.ZERO;
    private static Direction zipFace = Direction.UP;
    private static int zipCreature = -1;
    private static int zipAge;
    private static int zipStalled;
    private static boolean zipConfirmed;
    private static int zipFinish;
    private static Vec3 zipLast = Vec3.ZERO;
    private static Vec3 zipVelocity = Vec3.ZERO;

    // ---- swing ----
    private static Vec3 swingAnchor = Vec3.ZERO;
    /** The point he swings about: the anchor, or for Web Assist the anchor pulled towards his way (SwingAssist.pivot). */
    private static Vec3 swingPivot = Vec3.ZERO;
    private static Direction swingFace = Direction.UP;
    private static double swingRope;
    private static double swingRopeMax;
    private static int swingAge;
    private static int swingGround;
    private static boolean swingConfirmed;
    private static Vec3 swingLast = Vec3.ZERO;
    private static Vec3 swingVelocity = Vec3.ZERO;

    // ---- Web Assist ----
    /** The steering of an assisted swing turns his motion after the camera by at most this many degrees a tick. */
    private static final double STEER = 3.0;
    /** Between two webs the air keeps this much of his horizontal speed a tick (vanilla's 0.91), for this many ticks. */
    private static final double CARRY_DRAG = 0.98;
    private static final double AIR_DRAG = 0.91;
    private static final int CARRY_TICKS = 40;
    /** After an assisted release with G still held, the next web goes out after this many ticks, at the latest these. */
    private static final int CHAIN_MIN = 3;
    private static final int CHAIN_MAX = 12;
    private static final int CHAIN_GROUND = 6;
    /** An assisted swing held still (against a trunk, a wall) this many ticks lets go. */
    private static final double STALL_SPEED = 0.06;
    private static final int STALL_TICKS = 4;
    private static int swingStalled;
    /** The web boost of the open-land "zip": speed ahead, and up. */
    private static final double BOOST_AHEAD = 1.0;
    private static final double BOOST_UP = 0.6;
    /** The swing he is on was begun by Web Assist (and is on a point of the sky). */
    private static boolean assisted;
    private static boolean swingSky;
    /** An anchor Web Assist chose as G was pressed: the next tick begins its swing. */
    @Nullable
    private static SwingAssist.Anchor pendingAssist;
    private static Vec3 pendingHeading = Vec3.ZERO;
    /** The climb out of a forest (3.3.1): the foot of the gap he dashes to, the speed of his leap up it, its ticks. */
    @Nullable
    private static Vec3 climbSpot;
    private static double climbUp;
    private static int climbTicks;
    /** G is held after an assisted release: the next web goes out as he begins to fall. */
    private static boolean chain;
    private static int chainTicks;
    private static int carry;
    /** The anchor Web Assist would take now (the preview), and the tick it was found. */
    @Nullable
    private static SwingAssist.Anchor planned;
    private static int plannedAt;

    // =================================================================================================================
    // What others (the effects, the animation, the HUD) may ask
    // =================================================================================================================

    /** What the local hero is doing with his own movement right now. */
    public static Mode mode() {
        return mode;
    }

    /** The surface the local hero clings to by his own reckoning, or {@link SpiderClimb.Surface#NONE}. */
    public static SpiderClimb.Surface surface() {
        return mode == Mode.CLING ? surface : SpiderClimb.Surface.NONE;
    }

    /** The end of the web the local hero's own movement holds (a zip's or a swing's anchor), or null when it holds none. */
    @Nullable
    public static Vec3 anchor() {
        return mode == Mode.ZIP ? zipAnchor : mode == Mode.SWING ? swingAnchor : null;
    }

    /** The anchor Web Assist would take next, to be shown, or null. */
    @Nullable
    public static SwingAssist.Anchor preview() {
        return planned;
    }

    /** Whether the swing the local hero is on was chosen by Web Assist. */
    public static boolean assisted() {
        return mode == Mode.SWING && assisted;
    }

    /** Whether a second press of Sneak + B would turn Instant Kill on (the first press asked for it). */
    public static boolean confirmPending(long now) {
        return now <= confirmUntil;
    }

    /** Forgets everything (a new world, a new body, the power gone). */
    public static void reset() {
        owner = null;
        mode = Mode.NONE;
        surface = SpiderClimb.Surface.NONE;
        mantleTicks = 0;
        lost = 0;
        pressedG = false;
        confirmUntil = 0;
        jumpWas = false;
        toldKind = -1;
        settling = false;
        assisted = false;
        pendingAssist = null;
        chain = false;
        carry = 0;
        planned = null;
        climbSpot = null;
    }

    // =================================================================================================================
    // Keys
    // =================================================================================================================

    /**
     * The movement keys as the game's key bindings say, so that they can be read after being cleared on their way in.
     */
    private record Keys(boolean forward, boolean back, boolean left, boolean right, boolean jump) {
        static Keys read(Minecraft mc) {
            if (mc.screen != null) {
                return new Keys(false, false, false, false, false);
            }
            return new Keys(mc.options.keyUp.isDown(), mc.options.keyDown.isDown(), mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(),
                    mc.options.keyJump.isDown());
        }

        /** +1 forward, -1 back. */
        int forwardAxis() {
            return (forward ? 1 : 0) - (back ? 1 : 0);
        }

        /** +1 to the left (as the game's impulses are), -1 to the right. */
        int leftAxis() {
            return (left ? 1 : 0) - (right ? 1 : 0);
        }
    }

    /**
     * The press of a key, seen as it happens (a short tap can begin and end between two ticks, so the key's state at
     * the start of a tick would miss it, and the clicks are the input handler's to consume): G starts a zip or a swing
     * on this client's own aim, and a press of Sneak + B asks for (or confirms) Instant Kill, which the HUD shows.
     */
    @Subscribe
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            pressedKey(InputConstants.getKey(event.getKey(), event.getScanCode()));
        }
    }

    @Subscribe
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            pressedKey(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    private static void pressedKey(InputConstants.Key key) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.isSpectator() || !Powers.get(player).acts(Power.SPIDER_MAN)) {
            return;
        }
        PowerData data = Powers.get(player);
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            // TODO(port seam) S1b: NeoForge's KeyMapping extension method until the platform has one.
            if (KeyBindings.ability(i).isActiveAndMatches(key)) {
                // (Sneak as the keys are now: a press of Sneak and G together comes before the tick that crouches him.)
                boolean sneaking = player.isShiftKeyDown() || mc.options.keyShift.isDown();
                HeroKit kit = data.kit();
                if (data.power() == Power.SPIDER_MAN) {
                    pressed(player, data, i, sneaking);
                } else if (kit != null) {
                    // (3.4: a custom hero's key is the Spider-Man key its binding holds, and its Sneak variant the binding's.)
                    HeroKit.Binding binding = kit.binding(i, sneaking);
                    HeroAbility row = binding == null ? null : binding.ability();
                    if (row != null && row.hero == Power.SPIDER_MAN) {
                        pressed(player, data, row.home, binding.isAlt());
                    }
                }
            }
        }
    }

    /**
     * The key the zip and the swing are on, which a swing and Web Assist's next web wait on while it is held: G for
     * Spider-Man, the column of a custom hero's zip (3.4).
     */
    private static boolean zipKeyDown(PowerData data) {
        int column = data.power() == Power.CUSTOM ? data.slot(HeroAbility.SPIDER_ZIP) : 2;
        return column >= 1 && KeyBindings.ability(column).isDown();
    }

    private static void pressed(LocalPlayer player, PowerData data, int ability, boolean sneaking) {
        long now = player.level().getGameTime();
        if (ability == 2) {
            pressedG = true;
            pressedGSneaking = sneaking;
            pressAssisted(player, data, sneaking);
        } else if (ability == 4 && sneaking && !keysIgnored(player, data)) {
            // The same asking as the server's, so that the HUD can say "confirm?" (nothing is decided here).
            if (data.is(Live.INSTANT_KILL) || !legsOut(data) || !data.isReady(HeroAbility.SPIDER_LEGS, now)) {
                confirmUntil = 0;
            } else if (now > confirmUntil) {
                confirmUntil = now + value(SpiderConfig.INSTANT_KILL_CONFIRM_TICKS, 60);
            } else {
                confirmUntil = 0;
            }
        }
    }

    /** The keys that would move him are cleared on their way into the game while his own movement has him. */
    @Subscribe
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() != owner || (mode == Mode.NONE && mantleTicks <= 0)) {
            return;
        }
        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
    }

    // =================================================================================================================
    // The tick
    // =================================================================================================================

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (player != owner) {
            reset();
            owner = player;
        }
        PowerData data = Powers.get(player);
        if (!data.acts(Power.SPIDER_MAN)) {
            if (mode != Mode.NONE || mantleTicks > 0) {
                mode = Mode.NONE;
                mantleTicks = 0;
                surface = SpiderClimb.Surface.NONE;
            }
            pressedG = false;
            return;
        }
        Keys keys = Keys.read(mc);
        boolean pressedJump = keys.jump() && !jumpWas;
        jumpWas = keys.jump();
        boolean blocked = blocked(player, data);
        boolean spellbound = player.hasEffect(ModEffects.SPELLBOUND);
        if (pendingAssist != null) {
            SwingAssist.Anchor anchor = pendingAssist;
            pendingAssist = null;
            pressedG = false;
            if (!blocked && !spellbound) {
                beginAssisted(player, anchor, pendingHeading);
            }
        }
        if (pressedG) {
            pressedG = false;
            if (!blocked && !spellbound) {
                if (pressedGSneaking) {
                    predictSwing(player, data);
                } else {
                    predictZip(player, data);
                }
            }
        }
        if (blocked && mode != Mode.NONE) {
            stop(player, BLOCKED);
        }
        if (climbSpot != null && (blocked || spellbound || mode != Mode.NONE || player.isInWaterOrBubble())) {
            climbSpot = null;
        }
        switch (mode) {
            case ZIP -> zipTick(player, data, pressedJump);
            case SWING -> swingTick(mc, player, data, keys, pressedJump);
            case CLING -> clingTick(mc, player, data, keys, pressedJump, spellbound);
            case NONE -> {
                if (climbSpot != null) {
                    climbTick(player);
                } else if (mantleTicks > 0) {
                    mantleTick(player);
                } else if (!blocked && !spellbound && !chain) {
                    // (Not while G is held for the next web of Web Assist: a child holding W would stick to every trunk.)
                    tryToCling(player, data, keys);
                }
            }
        }
        assistTick(mc, player, data, blocked || spellbound);
    }

    /**
     * A hero who hangs still sends no position to the server (vanilla sends one only when the player moves, or every
     * second). The server then lets its copy of him sag with gravity from the wall or the ceiling he hangs on, and does
     * not see that he clings. So while he hangs, the position goes to the server every tick that vanilla would not send
     * it.
     */
    @Subscribe
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player != owner || player.connection == null) {
            lastSent = null;
            return;
        }
        tellTheServer(player);
        if (mode != Mode.CLING) {
            lastSent = null;
            return;
        }
        Vec3 at = player.position();
        if (lastSent == null || at.distanceToSqr(lastSent) > VANILLA_SENDS) {
            lastSent = at; // (vanilla sent this one)
        } else {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(at.x, at.y, at.z, player.onGround()));
        }
    }

    /**
     * Tells the server what he clings to when that changes, and again every few ticks while he clings (the server
     * believes it for twelve), because it cannot tell a climb, a crawl down a wall and a fall apart from his positions
     * alone, and the others see what the server says. Nothing is sent by anyone who is not Spider-Man.
     */
    private static void tellTheServer(LocalPlayer player) {
        if (!Powers.get(player).acts(Power.SPIDER_MAN)) {
            toldKind = -1;
            return;
        }
        SpiderClimb.Surface on = mode == Mode.CLING ? surface : SpiderClimb.Surface.NONE;
        int kind = on.kind() == SpiderClimb.Kind.WALL ? SpiderClingPayload.WALL
                : on.kind() == SpiderClimb.Kind.CEILING ? SpiderClingPayload.CEILING : SpiderClingPayload.NONE;
        int normal = on.touches() ? on.normal().get3DDataValue() : 0;
        boolean changed = kind != toldKind || normal != toldNormal;
        if (changed || (kind != SpiderClingPayload.NONE && player.tickCount - toldAt >= REPORT_TICKS)) {
            if (changed && kind == SpiderClingPayload.NONE && toldKind < 0) {
                toldKind = kind; // (nothing has been told yet, and there is nothing to tell)
                toldNormal = normal;
                return;
            }
            Net.sendToServer(new SpiderClingPayload(kind, normal));
            toldKind = kind;
            toldNormal = normal;
            toldAt = player.tickCount;
        }
    }

    /**
     * What stops him acting: the things that hold or carry a player (Billy's spell holds him in place, see the cling).
     */
    /**
     * Whether the server ignores every key of his now ({@code SpiderServer.blocked}): what stops his own movement here,
     * and Billy's spell or the Phoenix's hold, which leave a cling frozen in place but let no web, zip or leg out.
     */
    public static boolean keysIgnored(LocalPlayer player, PowerData data) {
        return blocked(player, data) || player.hasEffect(ModEffects.SPELLBOUND);
    }

    private static boolean blocked(LocalPlayer player, PowerData data) {
        if (player.isSpectator() || player.isPassenger() || player.getAbilities().flying || player.isFallFlying() || data.is(Live.PINNED)
                || data.is(Live.LIFTED) || player.hasEffect(ModEffects.ENTOMBED)) {
            return true;
        }
        for (AbstractClientPlayer other : Minecraft.getInstance().level.players()) {
            if (other != player && Powers.get(other).heldEntityId() == player.getId()) {
                return true; // telekinesis has him
            }
        }
        return false;
    }

    private static boolean starving(LocalPlayer player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    private static boolean legsOut(PowerData data) {
        return data.toggle(Toggle.SPIDER_LEGS) && SpiderServer.mayGrowLegs(data);
    }

    private static double value(ConfigSpec.DoubleValue option, double fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? option.get() : fallback;
    }

    private static int value(ConfigSpec.IntValue option, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? option.get() : fallback;
    }

    /** Where his keys point, as a horizontal unit vector (zero when none is pressed). */
    private static Vec3 wish(Keys keys, LocalPlayer player) {
        return SpiderClimb.wish(keys.forwardAxis(), keys.leftAxis(), player.getYRot());
    }

    // =================================================================================================================
    // Clinging
    // =================================================================================================================

    /**
     * Walking: does he begin to cling? A wall he presses into (in the air, or with Space), a ceiling he reaches with
     * Space.
     */
    private static void tryToCling(LocalPlayer player, PowerData data, Keys keys) {
        if (!data.toggle(Toggle.WALL_CRAWL) || player.tickCount < restickUntil || player.isInWaterOrBubble() || player.isInLava()
                || starving(player)) {
            return;
        }
        Vec3 wish = wish(keys, player);
        SpiderClimb.Surface touched = SpiderClimb.detect(player.level(), player, wish, null);
        if (touched.touches() && SpiderClimb.enters(touched, wish, player.onGround(), keys.jump())) {
            enterCling(player, touched);
        }
    }

    private static void enterCling(LocalPlayer player, SpiderClimb.Surface on) {
        mode = Mode.CLING;
        surface = on;
        settling = false;
        clingAge = 0;
        lost = 0;
        groundTicks = 0;
        lastVelocity = Vec3.ZERO;
        // A cling begun by a press of Space is not left again by that same press.
        jumpWas = true;
        player.setSprinting(false);
    }

    private static void clingTick(Minecraft mc, LocalPlayer player, PowerData data, Keys keys, boolean pressedJump, boolean spellbound) {
        Level level = player.level();
        clingAge++;
        Vec3 wish = wish(keys, player);
        float forward = spellbound ? 0.0F : keys.forwardAxis();
        float right = spellbound ? 0.0F : -keys.leftAxis();

        // What does he touch: the surface he is on (or the one he presses into at a corner); a ceiling above a wall he
        // climbs up; a wall beside the ceiling he hangs from.
        SpiderClimb.Surface touched = SpiderClimb.detect(level, player, wish, surface);
        if (touched.kind() == SpiderClimb.Kind.WALL && surface.kind() == SpiderClimb.Kind.WALL && lastVelocity.y > 0.05
                && SpiderClimb.detect(level, player, Vec3.ZERO, CEILING).kind() == SpiderClimb.Kind.CEILING) {
            touched = CEILING;
        } else if (surface.kind() == SpiderClimb.Kind.CEILING && touched.kind() == SpiderClimb.Kind.CEILING) {
            SpiderClimb.Surface wall = SpiderClimb.detect(level, player, wish, null);
            if (wall.kind() == SpiderClimb.Kind.WALL) {
                touched = wall;
            }
        }
        boolean drawingIn = settling && clingAge <= SETTLE_TICKS && !touched.touches();
        if (touched.touches()) {
            lost = 0;
            surface = touched;
            settling = false;
        } else if (!drawingIn) {
            lost++;
        }
        groundTicks = player.onGround() ? groundTicks + 1 : 0;

        // Does he let go?
        if (!data.toggle(Toggle.WALL_CRAWL) || player.isInWaterOrBubble() || player.isInLava() || starving(player) || groundTicks >= 2) {
            letGo(player, null);
            return;
        }
        if (lost == 1 && SpiderClimb.mantles(lastVelocity.y > 0.05, true, SpiderClimb.topAboveFeet(level, player, surface))) {
            // His hands lost the wall at the top of the block he climbed: he walks up onto it.
            mantleWall = surface;
            mantleTicks = MANTLE_TICKS;
            mode = Mode.NONE;
            surface = SpiderClimb.Surface.NONE;
            mantleTick(player);
            return;
        }
        if (lost >= LOST_TICKS) {
            letGo(player, null);
            return;
        }
        if (pressedJump && clingAge >= LEAP_DELAY && !spellbound) {
            Vec3 leap = SpiderClimb.leap(surface, player.getLookAngle(), value(SpiderConfig.LEAP_SPEED, 0.9), SpiderMath.speedFactor(player));
            letGo(player, leap);
            restickUntil = player.tickCount + SpiderClimb.RESTICK_DELAY;
            return;
        }

        // Move along the surface: his whole velocity is set, so gravity does not apply.
        double speed = spellbound ? 0.0 : SpiderMath.crawlSpeed(value(SpiderConfig.CRAWL_SPEED, 0.22), SpiderMath.speedFactor(player), legsOut(data));
        if (forward != 0.0F && right != 0.0F) {
            forward *= Mth.SQRT_OF_TWO / 2.0F;
            right *= Mth.SQRT_OF_TWO / 2.0F;
        }
        Vec3 velocity = drawingIn ? surface.normalVector().scale(-SETTLE_PULL)
                : SpiderClimb.velocity(surface, player.getLookAngle(), player.getYRot(), forward, right, speed);
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
        player.setSprinting(false);
        lastVelocity = velocity;
    }

    /** He lets go of the surface (with a leap's velocity, or none: he falls from where he is). */
    private static void letGo(LocalPlayer player, @Nullable Vec3 velocity) {
        mode = Mode.NONE;
        surface = SpiderClimb.Surface.NONE;
        if (velocity != null) {
            player.setDeltaMovement(velocity);
        } else {
            Vec3 now = player.getDeltaMovement();
            player.setDeltaMovement(0.0, Math.min(now.y, 0.0), 0.0);
        }
    }

    /** The walk onto the top of the block: a gentle push into the former wall, raised if his feet are under the top. */
    private static void mantleTick(LocalPlayer player) {
        mantleTicks--;
        double top = SpiderClimb.topAboveFeet(player.level(), player, mantleWall);
        if (Double.isNaN(top) || player.onGround() || mantleTicks <= 0) {
            mantleTicks = 0;
            return;
        }
        Vec3 into = mantleWall.normalVector().scale(-1);
        boolean level = top <= 0.02;
        double rise = level ? 0.0 : Math.min(0.25, top + 0.05);
        double push = level ? MANTLE_PUSH : 0.05;
        player.setDeltaMovement(into.x * push, rise, into.z * push);
        player.resetFallDistance();
    }

    // =================================================================================================================
    // The zip
    // =================================================================================================================

    /** G was pressed: if the aim finds a block, the zip begins at once on this client's own reckoning. */
    private static void predictZip(LocalPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.SPIDER_ZIP, now)) {
            return;
        }
        double range = value(SpiderConfig.ZIP_RANGE, 40);
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            return;
        }
        if (value(SpiderConfig.ZIP_KICK_DAMAGE, 4.0) > 0.0
                && Raycasts.entity(player, range, 0.0, SpiderWebs::webTarget, 1.0F) != null) {
            return; // (a creature in the way is the server's to choose: it kicks it)
        }
        Vec3 stand = SpiderMath.standAt(spot -> Teleports.fits(player, spot), hit.getLocation(), hit.getDirection(), player.getBbHeight());
        if (stand != null) {
            beginZip(player, hit.getLocation(), stand, hit.getDirection(), -1, false);
        }
    }

    private static void beginZip(LocalPlayer player, Vec3 anchor, Vec3 stand, Direction face, int creature, boolean confirmed) {
        if (mode == Mode.SWING) {
            stopSwing(player, CANCELLED, false);
        }
        surface = SpiderClimb.Surface.NONE;
        mantleTicks = 0;
        mode = Mode.ZIP;
        zipAnchor = anchor;
        zipStand = stand;
        zipFace = face;
        zipCreature = creature;
        zipAge = 0;
        zipStalled = 0;
        zipConfirmed = confirmed;
        zipFinish = 0;
        zipLast = player.position();
        zipVelocity = player.getDeltaMovement();
    }

    private static void zipTick(LocalPlayer player, PowerData data, boolean pressedJump) {
        Vec3 position = player.position();
        if (!zipConfirmed && zipAge > PREDICTION_TICKS) {
            stopZip(player, CANCELLED);
            return;
        }
        if (position.distanceTo(zipLast) > 3.0) {
            stopZip(player, CANCELLED); // the Hex, or a teleport
            return;
        }
        if (pressedJump) {
            stopZip(player, CANCELLED); // Space cancels, and he keeps his speed
            return;
        }
        if (zipAge >= value(SpiderConfig.ZIP_MAX_TICKS, 60)) {
            stopZip(player, TIMEOUT);
            return;
        }
        double distance = position.distanceTo(zipStand);
        if (zipAge > 0) {
            // Stopped against something: nearer than the length of a step is arrived, further is a collision.
            double step = position.distanceTo(zipLast);
            zipStalled = zipAge > 3 && step < 0.3 * zipVelocity.length() ? zipStalled + 1 : 0;
        }
        if (distance < ARRIVE || zipStalled >= 2 && distance <= 1.6 || zipFinish > FINISH_TICKS) {
            arrive(player, data);
            return;
        }
        if (zipStalled >= 3) {
            stopZip(player, CANCELLED);
            return;
        }
        if (zipFinish > 0) {
            zipFinish++;
        }
        double speed = SpiderMath.zipSpeed(zipAge, distance, value(SpiderConfig.ZIP_SPEED, 1.3), SpiderMath.speedFactor(player));
        Vec3 velocity = zipStand.subtract(position).normalize().scale(Math.min(speed, distance));
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
        player.setSprinting(false);
        zipLast = position;
        zipVelocity = velocity;
        zipAge++;
    }

    /**
     * The zip is over at its stand point: on a wall or under a ceiling he clings to it at once, on top of a block he
     * lands.
     */
    private static void arrive(LocalPlayer player, PowerData data) {
        Direction face = zipFace;
        boolean wallOrCeiling = face == Direction.DOWN || face.getAxis().isHorizontal();
        Vec3 velocity = zipVelocity;
        mode = Mode.NONE;
        if (wallOrCeiling && zipCreature < 0 && data.toggle(Toggle.WALL_CRAWL) && !starving(player)) {
            enterCling(player, new SpiderClimb.Surface(face == Direction.DOWN ? SpiderClimb.Kind.CEILING : SpiderClimb.Kind.WALL, face));
            // (He stops a little short of the stand point: the first ticks draw him in to the surface.)
            settling = true;
            player.setDeltaMovement(surface.normalVector().scale(-SETTLE_PULL));
            lastVelocity = player.getDeltaMovement();
        } else if (wallOrCeiling && zipCreature < 0) {
            player.setDeltaMovement(velocity.scale(0.4));
        } else {
            player.setDeltaMovement(velocity.normalize().scale(0.25));
        }
        player.resetFallDistance();
    }

    /** The zip is over before it arrived: he goes on with the speed he had. */
    private static void stopZip(LocalPlayer player, int reason) {
        if (mode == Mode.ZIP) {
            mode = Mode.NONE;
            if (reason == BLOCKED || reason == TIMEOUT) {
                // (Blocked is held by something else; timed out he has been at it long enough: he slows to a fall.)
                player.setDeltaMovement(player.getDeltaMovement().scale(0.5));
            } else if (reason == REFUSED) {
                player.setDeltaMovement(player.getDeltaMovement().scale(0.1));
            }
        }
    }

    // =================================================================================================================
    // The swing
    // =================================================================================================================

    /** Sneak + G was pressed: if the aim finds a block, the swing begins at once on this client's own reckoning. */
    private static void predictSwing(LocalPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.SPIDER_ZIP, now) || mode == Mode.SWING) {
            return;
        }
        double range = value(SpiderConfig.SWING_RANGE, 40);
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        if (hit.getType() != HitResult.Type.MISS) {
            double rope = Mth.clamp(player.getEyePosition().distanceTo(hit.getLocation()), 4.0, range);
            beginSwing(player, hit.getLocation(), hit.getDirection(), rope, false);
        }
    }

    private static void beginSwing(LocalPlayer player, Vec3 anchor, Direction face, double rope, boolean confirmed) {
        surface = SpiderClimb.Surface.NONE;
        mantleTicks = 0;
        mode = Mode.SWING;
        swingAnchor = anchor;
        swingPivot = anchor;
        swingFace = face;
        swingRope = rope;
        swingRopeMax = rope * MAX_ROPE_FACTOR;
        swingAge = 0;
        swingGround = 0;
        swingStalled = 0;
        swingConfirmed = confirmed;
        assisted = false;
        swingSky = false;
        chain = false;
        carry = 0;
        Vec3 velocity = player.getDeltaMovement();
        if (player.onGround()) {
            // A swing begun on the ground hops him off it, and towards the anchor: from rest the pendulum's way down is
            // mostly into the floor (and Sneak keeps him from stepping off a ledge), so it would never get going.
            velocity = SwingAssist.hop(velocity, player.position(), anchor, player.getLookAngle(), SWING_HOP, SWING_PUSH);
        }
        swingVelocity = velocity;
        swingLast = player.position();
        player.setDeltaMovement(velocity);
    }

    private static void swingTick(Minecraft mc, LocalPlayer player, PowerData data, Keys keys, boolean pressedJump) {
        Level level = player.level();
        if (!swingConfirmed && swingAge > PREDICTION_TICKS) {
            stopSwing(player, CANCELLED, false);
            return;
        }
        if (!zipKeyDown(data) || mc.screen != null) {
            stopSwing(player, RELEASED, false);
            return;
        }
        if (pressedJump) {
            stopSwing(player, RELEASED, true);
            return;
        }
        if (swingAge >= value(SpiderConfig.SWING_MAX_TICKS, 300)) {
            stopSwing(player, TIMEOUT, false);
            return;
        }
        BlockPos behind = BlockPos.containing(swingAnchor.subtract(new Vec3(swingFace.getStepX(), swingFace.getStepY(), swingFace.getStepZ()).scale(0.1)));
        if (swingConfirmed && !swingSky && level.getBlockState(behind).getCollisionShape(level, behind).isEmpty()) {
            stopSwing(player, ANCHOR_LOST, false);
            return;
        }
        swingGround = player.onGround() && swingAge >= SWING_GRACE ? swingGround + 1 : 0;
        if (swingGround >= SWING_GRACE) {
            stopSwing(player, CANCELLED, false);
            return;
        }
        if (assisted && swingSky && swingAge >= value(SpiderConfig.SKY_ANCHOR_MAX_TICKS, 60)) {
            autoRelease(player);
            return;
        }
        swingAge++;

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 ahead = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 aside = new Vec3(-ahead.z, 0, ahead.x);
        Vec3 position = player.position();
        Vec3 centre = position.add(0, 0.9, 0);
        Vec3 actual = swingAge == 1 ? player.getDeltaMovement() : position.subtract(swingLast);
        double maxSpeed = maxSwingSpeed(player);
        Vec3 control;
        if (assisted) {
            // Web Assist: it lets go by itself at the best moment; the rope is reeled in near the ground, W pumps and S
            // brakes instead of reeling, A and D push a little, and the arc turns gently after the camera.
            swingStalled = actual.lengthSqr() < STALL_SPEED * STALL_SPEED && swingAge > SWING_GRACE ? swingStalled + 1 : 0;
            if (SwingAssist.releaseNow(swingPivot, centre, actual, swingAge) || swingStalled >= STALL_TICKS) {
                // (Let go at the best moment, or when something has stopped him: the next web takes him on.)
                autoRelease(player);
                return;
            }
            swingRope = SwingAssist.reel(level, swingPivot, swingRope, centre, actual);
            actual = SwingAssist.steer(actual, SwingAssist.heading(player.getLookAngle(), Vec3.ZERO, player.getYRot()), STEER);
            control = SwingAssist.pump(actual, centre, swingPivot, keys.forwardAxis(), maxSpeed).add(aside.scale(-keys.leftAxis() * AIR_CONTROL));
        } else {
            // W reels the web in, S lets it out; the keys also push him a little, in the camera's frame.
            swingRope = Mth.clamp(swingRope - keys.forwardAxis() * REEL, MIN_ROPE, swingRopeMax);
            Vec3 push = ahead.scale(keys.forwardAxis()).add(aside.scale(-keys.leftAxis()));
            control = push.lengthSqr() > 1.0 ? push.normalize().scale(AIR_CONTROL) : push.scale(AIR_CONTROL);
        }
        SpiderMath.Swing next = SpiderMath.swingStep(centre, actual, swingPivot, swingRope, control, value(SpiderConfig.SWING_GRAVITY, 0.06),
                assisted ? SwingAssist.DRAG : SWING_DRAG, maxSpeed);
        swingVelocity = next.velocity();
        swingLast = position;
        player.setDeltaMovement(next.velocity());
        player.resetFallDistance();
        player.setSprinting(false);
    }

    /** The web is let go: he keeps his speed (a little more, and a push up, if he jumped). */
    private static void stopSwing(LocalPlayer player, int reason, boolean jumped) {
        if (mode != Mode.SWING) {
            return;
        }
        mode = Mode.NONE;
        if (assisted && !jumped && reason != BLOCKED && reason != REFUSED && zipKeyDown(Powers.get(player))) {
            // (3.3.1) However an assisted swing ends (it landed, met a trunk, the server ended it), while G is held the next
            // web follows: he does not cling to the trunk he came down by, and the chain goes on.
            chain = true;
            chainTicks = 0;
        }
        double maxSpeed = maxSwingSpeed(player);
        Vec3 actual = swingAge <= 1 ? player.getDeltaMovement() : player.position().subtract(swingLast);
        if (reason == BLOCKED) {
            player.setDeltaMovement(actual.scale(0.5));
        } else if (reason == REFUSED) {
            player.setDeltaMovement(actual.scale(0.1));
        } else if (reason != CANCELLED || swingAge > 1) {
            player.setDeltaMovement(SpiderMath.releaseVelocity(actual, jumped, maxSpeed));
        }
    }

    /** The movement of the hero ends because something holds him (the same for a zip and a swing). */
    private static void stop(LocalPlayer player, int reason) {
        if (mode == Mode.ZIP) {
            stopZip(player, reason);
        } else if (mode == Mode.SWING) {
            stopSwing(player, reason, false);
        } else if (mode == Mode.CLING) {
            letGo(player, null);
        }
    }

    /** His top speed on a swing: the config's, scaled by his speed factor against a plain Spider-Man's. */
    private static double maxSwingSpeed(LocalPlayer player) {
        double factor = SpiderMath.speedFactor(player);
        return value(SpiderConfig.SWING_MAX_SPEED, 1.8) * Mth.clamp(Math.sqrt(factor / PLAIN_FACTOR), 0.2, 1.6);
    }

    // =================================================================================================================
    // Web Assist
    // =================================================================================================================

    /** Whether the local hero's swings are assisted: his own setting, and the server allows it. */
    public static boolean assistOn() {
        return EnhancedClientConfig.webAssist() && (!EnhancedConfig.SPEC.isLoaded() || SpiderConfig.WEB_ASSIST.get());
    }

    /** The Web Assist key: the setting switches, and he is told. */
    public static void toggleAssist(LocalPlayer player) {
        boolean on = !EnhancedClientConfig.webAssist();
        EnhancedClientConfig.setWebAssist(on);
        String key = !on ? "message.enhanced.web_assist.off"
                : EnhancedConfig.SPEC.isLoaded() && !SpiderConfig.WEB_ASSIST.get() ? "message.enhanced.web_assist.server_off" : "message.enhanced.web_assist.on";
        player.displayClientMessage(Component.translatable(key), true);
        planned = null;
    }

    private static SwingAssist.Params params() {
        double range = Math.min(value(SpiderConfig.SWING_RANGE, 40), value(SpiderConfig.WEB_ASSIST_RANGE, 30));
        return new SwingAssist.Params(range, value(SpiderConfig.SWING_GRAVITY, 0.06), value(SpiderConfig.SWING_MAX_SPEED, 1.8));
    }

    private static SwingAssist.OpenLand openLand() {
        return SwingAssist.OpenLand.of(EnhancedConfig.SPEC.isLoaded() ? SpiderConfig.WEB_ASSIST_OPEN_LAND.get() : "sky");
    }

    /** Whether Web Assist may send a web now (the same asking as the server's). */
    private static boolean assistReady(LocalPlayer player, PowerData data) {
        return assistOn() && !keysIgnored(player, data) && data.isReady(HeroAbility.SPIDER_ZIP, player.level().getGameTime())
                && !(SpiderServer.organic(player, data) && starving(player));
    }

    /** The anchor Web Assist would take from where he is now, or null. */
    @Nullable
    private static SwingAssist.Anchor plan(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        if (player.onGround()) {
            velocity = SwingAssist.hop(velocity, player.position(), player.position().add(look.scale(10.0)), look, SWING_HOP, SWING_PUSH);
        }
        return SwingAssist.choose(player.level(), player, player.position(), look, velocity, params(), openLand(), null);
    }

    /**
     * G was pressed: Sneak + G, or G in mid-air at nothing a zip could reach, is an assisted swing. Its anchor is chosen
     * and told to the server now, before the ability key goes, so that the key finds the swing already on; the next tick
     * begins it here.
     */
    private static void pressAssisted(LocalPlayer player, PowerData data, boolean sneaking) {
        if (mode == Mode.SWING || mode == Mode.CLING || !assistReady(player, data)) {
            return;
        }
        if (!sneaking) {
            boolean inTheAir = !player.onGround() && !player.isInWaterOrBubble();
            if (!inTheAir || Raycasts.block(player, value(SpiderConfig.ZIP_RANGE, 40), 1.0F).getType() != HitResult.Type.MISS) {
                return; // (a zip as before)
            }
        }
        SwingAssist.Anchor anchor = plan(player);
        if (anchor != null) {
            send(player, anchor);
            pendingAssist = anchor;
            pendingHeading = SwingAssist.heading(player.getLookAngle(), player.getDeltaMovement(), player.getYRot());
        } else {
            // Nothing yet: while G is held the assist keeps looking (he may run or jump to where there is something).
            chain = true;
            chainTicks = 0;
        }
    }

    /** Tells the server (the rope it is told is the web's own length: it measures him against the anchor). */
    private static void send(LocalPlayer player, SwingAssist.Anchor anchor) {
        int action = anchor.kind() == SwingAssist.Kind.BOOST ? SpiderSwingPayload.BOOST
                : anchor.kind() == SwingAssist.Kind.CLIMB ? SpiderSwingPayload.CLIMB : SpiderSwingPayload.ATTACH;
        float web = (float) anchor.web(player.position().add(0, SwingAssist.CENTRE, 0));
        Net.sendToServer(new SpiderSwingPayload(action, anchor.point(), anchor.face().get3DDataValue(), web,
                anchor.kind() == SwingAssist.Kind.SKY));
    }

    /** An assisted web goes out: a swing on its anchor, the boost of the open land, or the leap out of a forest. */
    private static void beginAssisted(LocalPlayer player, SwingAssist.Anchor anchor, Vec3 heading) {
        if (anchor.kind() == SwingAssist.Kind.BOOST) {
            if (mode == Mode.SWING) {
                stopSwing(player, CANCELLED, false);
            }
            Vec3 v = player.getDeltaMovement();
            double flat = Math.max(Math.hypot(v.x, v.z), BOOST_AHEAD);
            player.setDeltaMovement(heading.x * flat, Math.max(v.y, BOOST_UP), heading.z * flat);
            player.resetFallDistance();
            carry = CARRY_TICKS;
            chain = zipKeyDown(Powers.get(player));
            chainTicks = 0;
            return;
        }
        if (anchor.kind() == SwingAssist.Kind.CLIMB) {
            // (3.3.1) A dash to the foot of the gap in the canopy and a leap up it (climbTick); at the top of the leap the
            // next web swings him over the trees.
            if (mode == Mode.SWING) {
                stopSwing(player, CANCELLED, false);
            } else if (mode == Mode.ZIP) {
                stopZip(player, CANCELLED);
            } else if (mode == Mode.CLING) {
                letGo(player, null);
            }
            climbSpot = anchor.pivot();
            climbUp = SwingAssist.leapUp(anchor);
            climbTicks = 0;
            chain = false;
            planned = null;
            return;
        }
        if (mode == Mode.ZIP) {
            stopZip(player, CANCELLED);
        }
        boolean grounded = player.onGround();
        Vec3 before = player.getDeltaMovement();
        beginSwing(player, anchor.point(), anchor.face(), anchor.rope(), false);
        swingPivot = anchor.pivot();
        if (grounded) {
            // (Hopped the way he looks, as the assist planned it, not straight at an anchor to his side.)
            Vec3 look = player.getLookAngle();
            swingVelocity = SwingAssist.hop(before, player.position(), player.position().add(look.scale(10.0)), look, SWING_HOP, SWING_PUSH);
            player.setDeltaMovement(swingVelocity);
        }
        assisted = true;
        swingSky = anchor.kind() == SwingAssist.Kind.SKY;
        planned = null;
    }

    /**
     * The climb out of a forest (3.3.1): he dashes along the floor to the foot of the gap, then leaps straight up it; the
     * speed is kept as between two webs, and while G is held the next web goes out at the top of the leap.
     */
    private static void climbTick(LocalPlayer player) {
        climbTicks++;
        Vec3 to = new Vec3(climbSpot.x - player.getX(), 0, climbSpot.z - player.getZ());
        double distance = to.length();
        if (distance > 0.2 && climbTicks <= SwingAssist.CLIMB_DASH_TICKS) {
            Vec3 dash = to.scale(Math.min(SwingAssist.CLIMB_DASH, distance) / distance);
            // (A step of the floor on the way is hopped over.)
            double up = player.horizontalCollision && player.onGround() ? 0.42 : player.getDeltaMovement().y;
            player.setDeltaMovement(dash.x, up, dash.z);
            return;
        }
        player.setDeltaMovement(to.x * 0.5, climbUp, to.z * 0.5);
        player.resetFallDistance();
        climbSpot = null;
        carry = CARRY_TICKS;
        chain = zipKeyDown(Powers.get(player));
        chainTicks = 0;
    }

    /** The assisted swing lets go at its best moment, with a little launch; G still held, the next web follows. */
    private static void autoRelease(LocalPlayer player) {
        Net.sendToServer(new SpiderSwingPayload(SpiderSwingPayload.RELEASE, swingAnchor, 0, 0.0F, swingSky));
        stopSwing(player, RELEASED, true);
        carry = CARRY_TICKS;
        chain = zipKeyDown(Powers.get(player));
        chainTicks = 0;
    }

    /**
     * Every tick: the speed kept between two webs, the next web of a chain, and the anchor shown. Out of a swing his
     * horizontal speed is drawn from vanilla's air drag only as far as {@link #CARRY_DRAG} (until he lands, clings or
     * swims, or {@link #CARRY_TICKS} pass).
     */
    private static void assistTick(Minecraft mc, LocalPlayer player, PowerData data, boolean held) {
        boolean free = mode == Mode.NONE && mantleTicks <= 0 && !held;
        boolean airborne = !player.onGround() && !player.isInWaterOrBubble() && !player.isInLava();
        if (!free || !airborne) {
            carry = 0;
        }
        if (carry > 0) {
            carry--;
            Vec3 v = player.getDeltaMovement();
            double flat = Math.hypot(v.x, v.z);
            double kept = Math.min(flat * CARRY_DRAG / AIR_DRAG, Math.max(flat, maxSwingSpeed(player)));
            if (flat > 1.0E-3) {
                player.setDeltaMovement(v.x * kept / flat, v.y, v.z * kept / flat);
            }
        }
        if (chain && (!free || player.isInWaterOrBubble() || player.isInLava() || !zipKeyDown(data) || mc.screen != null)) {
            chain = false;
        }
        boolean show = free && assistOn() && EnhancedClientConfig.webAssistPreview() && (airborne || player.isShiftKeyDown());
        if (chain) {
            chainTicks++;
            // In the air the next web goes out as he begins to fall; on the ground (a landing, a tree top) a moment later.
            boolean falling = player.getDeltaMovement().y < 0.08;
            boolean due = airborne ? falling || chainTicks >= CHAIN_MAX : chainTicks >= CHAIN_GROUND;
            if (chainTicks >= CHAIN_MIN && due && chainTicks % 2 == 0 && assistReady(player, data)) {
                SwingAssist.Anchor next = plan(player);
                if (next != null) {
                    send(player, next);
                    beginAssisted(player, next, SwingAssist.heading(player.getLookAngle(), player.getDeltaMovement(), player.getYRot()));
                    return;
                }
            }
        }
        if (!show && !chain) {
            planned = null;
        } else if (player.tickCount - plannedAt >= 2 || chain) {
            planned = assistReady(player, data) ? plan(player) : null;
            plannedAt = player.tickCount;
        }
    }

    // =================================================================================================================
    // The server says
    // =================================================================================================================

    /**
     * A web of the local hero began or ended, as the server saw it: confirm what was predicted, take over from it, or
     * stop.
     */
    public static void onFx(SpiderFxPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getId() != payload.player() || !Powers.get(player).acts(Power.SPIDER_MAN)) {
            return;
        }
        switch (payload.kind()) {
            case ZIP_START -> {
                Direction face = Direction.getNearest(payload.normal().x, payload.normal().y, payload.normal().z);
                Vec3 stand = payload.points().isEmpty() ? zipStand : payload.points().get(0);
                if (mode == Mode.ZIP && !zipConfirmed) {
                    zipConfirmed = true;
                    if (zipAnchor.distanceToSqr(payload.to()) > 1.0) {
                        // The server aimed elsewhere: its anchor wins, and he moves towards it.
                        zipAnchor = payload.to();
                        zipStand = stand;
                        zipFace = face;
                        zipAge = 0;
                    }
                    zipCreature = payload.arg();
                } else {
                    beginZip(player, payload.to(), stand, face, payload.arg(), true);
                }
            }
            case SWING_START -> {
                Direction face = Direction.getNearest(payload.normal().x, payload.normal().y, payload.normal().z);
                if (mode == Mode.SWING && !swingConfirmed && assisted && swingAnchor.distanceToSqr(payload.to()) > 1.0) {
                    // (The server takes an assisted anchor as it is or not at all: this one is of a web before it.)
                    break;
                }
                if (mode == Mode.SWING && !swingConfirmed) {
                    swingConfirmed = true;
                    if (swingAnchor.distanceToSqr(payload.to()) > 1.0) {
                        swingAnchor = payload.to();
                        swingFace = face;
                        swingRope = payload.value();
                        swingRopeMax = swingRope * MAX_ROPE_FACTOR;
                    }
                } else if (mode != Mode.SWING) {
                    beginSwing(player, payload.to(), face, payload.value(), true);
                }
            }
            case ZIP_END -> {
                if (payload.arg() == REFUSED) {
                    // The server never made the web (the Hex, a shield, a cooldown ...): the zip this client began on its own
                    // prediction stops where it is, and does not fly on with its speed.
                    if (mode == Mode.ZIP && !zipConfirmed) {
                        stopZip(player, REFUSED);
                    }
                } else if (mode == Mode.ZIP && zipConfirmed && zipAnchor.distanceToSqr(payload.to()) < 1.0) {
                    if (payload.arg() == ARRIVED) {
                        // (The server calls it arrived a block short; he finishes the last of it himself.)
                        zipFinish = Math.max(zipFinish, 1);
                    } else {
                        stopZip(player, payload.arg());
                    }
                }
            }
            case SWING_END -> {
                if (payload.arg() == REFUSED) {
                    if (mode == Mode.SWING && !swingConfirmed) {
                        stopSwing(player, REFUSED, false);
                    }
                } else if (mode == Mode.SWING && swingConfirmed && swingAnchor.distanceToSqr(payload.to()) < 1.0) {
                    stopSwing(player, payload.arg(), false);
                }
            }
            default -> {
            }
        }
    }

    private SpiderMotion() {
    }
}
