package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.build.BuildSounds;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.HandTracker;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.client.render.WorldEffectsRenderer;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.HexBoltPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Build Magic spells this client sees at work, and what a builder looks like to everyone who sees him (D-B 7.1, 7.2,
 * 8.2, 8.3). Everything here comes from {@link BuildFxPayload}, which the server sends to everyone within 128 blocks of a
 * spell and to its caster, and from the builders' own synced state ({@link BuildView}); no block ever travels in it.
 *
 * <ul>
 *     <li><b>The cast</b> (START): Wanda plays her {@code WANDA_HEX_TWEAK} gesture, or {@code WANDA_HEX_RISE} for a spell of
 *     {@value BuildSounds#HUGE_CAST} blocks or more; Billy plays {@code BILLY_BOLT}, through
 *     {@link HeroAnimator#play}, and his blue arc leaps from his hand to the spell's origin and forks to the box's top
 *     corners (an ordinary {@code HexBoltPayload} made here, as the server sends none), which also aims his casting hand at
 *     the spell. While it works, scarlet or blue wisps stream from the
 *     caster's hands to the spell.</li>
 *     <li><b>The frame and the ripple</b>: the spell's box glows in the hero's colour while it works (a faint copy of the
 *     lines over everything, as the builder's own box has), and a translucent band sweeps it along the order of the work:
 *     a rising plane (fills, stamps), a falling plane (vanishing), an expanding shell (round shapes), a wall that moves out
 *     from the caster, column after column (a valley-sized spell). Wanda's is scarlet with a lattice of faint lines,
 *     Billy's blue with white-hot points. The progress comes every 10 ticks and is followed smoothly between them. Under
 *     reduced motion: only the outline, fading.</li>
 *     <li><b>Undo</b> (UNDO_START): the same ripple backwards, lighter, with a shimmer. <b>Stopped</b>: it rewinds in
 *     twelve ticks. <b>Fizzle</b> (the caster only): the frame flickers red and grey, and smoke curls off his hand.</li>
 *     <li><b>Mom's thread</b>: while one of Billy's spells is cast with a Scarlet Witch's help ({@code momId}), a thin
 *     scarlet thread runs from her hand to his, drawn like the telekinesis tether.</li>
 *     <li><b>Rank-up</b> (RANK_UP, within 32 blocks of Billy): rings widen round him and a ring of runes spins up his
 *     body for two seconds.</li>
 *     <li><b>Builders</b>: everyone in build mode has wisps (Wanda, two a tick per hand, curling up) or sparks (Billy, one a
 *     tick per hand, and a tiny arc between his fingers every second) at the hands; halved under reduced motion.</li>
 * </ul>
 *
 * <p>A spell not heard of for {@value #STALE_TICKS} ticks (the caster left, the server stopped) is forgotten; everything
 * is cleared at logout and with the world. Only ever loaded on a client, from the handler of {@link BuildFxPayload} and
 * from {@link BuildInput} and {@link BuildRenderer}.
 */
public final class BuildFx {
    /** Ticks a spell may go unheard of before it is forgotten. */
    static final int STALE_TICKS = 100;
    /** Ticks the frame takes to fade once a spell is done. */
    static final int FADE_TICKS = 20;
    /** Ticks a stopped spell takes to rewind. */
    static final int REWIND_TICKS = 12;
    /** Ticks a fizzle's flicker lasts. */
    static final int FIZZLE_TICKS = 12;
    /** Ticks the flash of an Instant spell's box lasts. */
    static final int POP_TICKS = 8;
    /** Ticks a rank-up's rings and runes last. */
    static final int RANK_TICKS = 40;
    /** How far a builder's hands are worth their particles. */
    static final double HAND_RANGE = 32.0;
    /** Most spells drawn at once (the nearest ones matter). */
    private static final int MAX_RUNS = 24;
    /** Ticks the sweep of a spell takes when the server has not said (a guess from its size, until it does). */
    private static final int MIN_SWEEP = 10;
    private static final int MAX_SWEEP = 200;
    private static final float EDGE = 0.07F;
    private static final int RUNES = 10;
    private static final double INFLATE = 0.02;
    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF4A4A;
    private static final int GREY = 0xB0B0B0;
    private static final int SHIMMER = 0xDCEBFF;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** The strokes of six runes, each a list of line segments x0, y0, x1, y1 in a square from -1 to 1. */
    private static final float[][] GLYPHS = {
            {0, -1, 0, 1, 0, 0.1F, 0.8F, 0.8F, 0, -0.3F, 0.8F, 0.4F},
            {-0.7F, -1, -0.7F, 1, -0.7F, 1, 0.7F, 0, 0.7F, 0, -0.7F, -1},
            {-0.8F, 0.9F, 0, -1, 0, -1, 0.8F, 0.9F, -0.5F, 0.1F, 0.5F, 0.1F},
            {0, -1, 0, 1, -0.8F, 0.6F, 0.8F, -0.6F, -0.8F, -0.6F, 0.8F, 0.6F},
            {-0.8F, -1, 0.8F, -1, 0.8F, -1, -0.8F, 1, -0.8F, 1, 0.8F, 1},
            {-0.6F, -1, -0.6F, 1, 0.6F, -1, 0.6F, 1, -0.6F, 0, 0.6F, 0}
    };

    /** How a spell is going. */
    private enum State {
        /** The server is still sending: the ripple follows the progress. */
        WORKING,
        /** Done: the ripple reaches its end, then the frame fades. */
        DONE,
        /** Stopped and taken back: the ripple rewinds. */
        REWINDING,
        /** Refused (the caster's only): a flicker. */
        FIZZLED
    }

    /** One spell, seen from its START (or an undo's) to its end. */
    private static final class Run {
        final int job;
        final int caster;
        final Power hero;
        final BoundingBox box;
        final Vec3 origin;
        int order;
        final int momId;
        final boolean reverse;
        final long seed;
        final int cells;
        final long bornAt;
        State state = State.WORKING;
        /** The game time of the last word of the server, of the last progress, and of the one before. */
        long heard;
        long progressAt;
        long previousAt;
        int permille;
        int previous;
        /** The game time it ended (done, stopped, refused), and how far it was then, from 0 to 1. */
        long endedAt;
        float endedFrom;

        Run(BuildFxPayload p, long now, boolean reverse) {
            this.job = p.job();
            this.caster = p.casterId();
            this.hero = p.hero() == BuildFxPayload.BILLY ? Power.BILLY : Power.SCARLET_WITCH;
            this.box = new BoundingBox(Math.min(p.min().getX(), p.max().getX()), Math.min(p.min().getY(), p.max().getY()),
                    Math.min(p.min().getZ(), p.max().getZ()), Math.max(p.min().getX(), p.max().getX()),
                    Math.max(p.min().getY(), p.max().getY()), Math.max(p.min().getZ(), p.max().getZ()));
            this.origin = Vec3.atCenterOf(p.origin());
            this.order = p.order();
            this.momId = p.momId();
            this.reverse = reverse;
            this.cells = p.cells();
            this.seed = p.job() * 6364136223846793005L + 1442695040888963407L;
            this.bornAt = now;
            this.heard = now;
            this.progressAt = now;
            this.previousAt = now;
        }

        /** How many ticks the sweep takes when nothing better is known. */
        float guessedTicks() {
            return Mth.clamp(cells / 800.0F, MIN_SWEEP, MAX_SWEEP);
        }

        /**
         * How far the spell is, from 0 to 1, at {@code time}: the last progress, carried on at the pace of the last two
         * (or the pace its size guesses before there are two), never more than 15 ticks ahead and never quite to the end
         * before the server says it is done.
         */
        float progress(float time) {
            float last = permille / 1000.0F;
            float pace = progressAt > previousAt ? (permille - previous) / 1000.0F / (progressAt - previousAt) : 1.0F / guessedTicks();
            float ahead = Math.min(Math.max(0.0F, time - progressAt), 15.0F);
            return Mth.clamp(last + Math.max(0.0F, pace) * ahead, 0.0F, 0.995F);
        }

        /** Where the ripple is, from 0 to 1 along its work order (an undo's runs backwards). */
        float sweep(float time) {
            float p = switch (state) {
                case WORKING -> progress(time);
                case DONE -> Mth.lerp(Mth.clamp((time - endedAt) / 8.0F, 0.0F, 1.0F), endedFrom, 1.0F);
                case REWINDING -> Mth.lerp(Mth.clamp((time - endedAt) / REWIND_TICKS, 0.0F, 1.0F), endedFrom, 0.0F);
                case FIZZLED -> endedFrom;
            };
            return reverse ? 1.0F - p : p;
        }

        /** How bright the frame is, from 0 to 1: full while it works, fading after. */
        float glow(float time) {
            return switch (state) {
                case WORKING -> 1.0F;
                case DONE -> 1.0F - Mth.clamp((time - endedAt - 8.0F) / FADE_TICKS, 0.0F, 1.0F);
                case REWINDING -> 1.0F - Mth.clamp((time - endedAt - REWIND_TICKS) / (FADE_TICKS / 2.0F), 0.0F, 1.0F);
                case FIZZLED -> 1.0F - Mth.clamp((time - endedAt) / FIZZLE_TICKS, 0.0F, 1.0F);
            };
        }

        boolean over(float time) {
            return state != State.WORKING && glow(time) <= 0.0F;
        }

        void end(State to, float time) {
            if (state == State.WORKING) {
                endedFrom = progress(time);
                endedAt = (long) Math.floor(time);
                state = to;
            }
        }
    }

    /** A rank-up: whose, and when it began. */
    private record Rank(int entityId, long bornAt) {
    }

    /** The spells seen, by the server's job number, oldest first. */
    private static final Map<Integer, Run> RUNS = new LinkedHashMap<>();
    private static final List<Rank> RANKS = new ArrayList<>();
    private static @Nullable ClientLevel seenLevel;

    /** A moment of a spell near this client (the payload's handler, on the client's main thread). */
    public static void accept(BuildFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        switch (payload.kind()) {
            case BuildFxPayload.START -> start(level, payload, now, false);
            case BuildFxPayload.UNDO_START -> start(level, payload, now, true);
            case BuildFxPayload.PROGRESS -> {
                Run run = RUNS.get(payload.job());
                if (run != null) {
                    run.heard = now;
                    if (run.order == BuildFxPayload.POP && payload.order() != BuildFxPayload.POP) {
                        run.order = payload.order(); // (an Instant spell that fell behind goes on at normal speed: its real ripple)
                    }
                    if (payload.permille() != run.permille) {
                        run.previous = run.permille;
                        run.previousAt = run.progressAt;
                        run.permille = payload.permille();
                        run.progressAt = now;
                    }
                }
            }
            case BuildFxPayload.DONE -> {
                end(payload.job(), State.DONE, now);
                Run run = RUNS.get(payload.job());
                if (run != null && run.order == BuildFxPayload.POP && !run.reverse) {
                    pop(level, run);
                }
            }
            case BuildFxPayload.STOPPED -> end(payload.job(), State.REWINDING, now);
            case BuildFxPayload.FIZZLE -> fizzle(level, payload, now);
            case BuildFxPayload.RANK_UP -> {
                RANKS.removeIf(r -> r.entityId() == payload.casterId());
                RANKS.add(new Rank(payload.casterId(), now));
            }
            default -> {
            }
        }
    }

    /** A spell begins: its gesture, and from now on its frame, ripple, thread and wisps. */
    private static void start(ClientLevel level, BuildFxPayload payload, long now, boolean undo) {
        Run run = new Run(payload, now, undo);
        RUNS.remove(payload.job());
        RUNS.put(payload.job(), run);
        while (RUNS.size() > 2 * MAX_RUNS) {
            RUNS.remove(RUNS.keySet().iterator().next());
        }
        if (!undo && level.getEntity(payload.casterId()) instanceof Player caster) {
            GestureId gesture = run.hero == Power.BILLY ? GestureId.BILLY_BOLT
                    : payload.cells() >= BuildSounds.HUGE_CAST ? GestureId.WANDA_HEX_RISE : GestureId.WANDA_HEX_TWEAK;
            HeroAnimator.play(caster, gesture, 0.0F);
            if (run.hero == Power.BILLY) {
                arc(level, caster, run, now);
            }
        }
    }

    /**
     * Billy's cast arc (D-B 7.2): a blue Hex Bolt leaps from his hand to the spell's origin and forks to up to four of the
     * box's top corners. Lane BS sends no payload for it: it is an ordinary {@link HexBoltPayload}, made here from the
     * START and handed to the Hex Bolt's own renderer, which also aims his casting hand at the spell.
     */
    private static void arc(ClientLevel level, Player caster, Run run, long now) {
        Vec3 from = caster instanceof AbstractClientPlayer billy ? WorldEffectsRenderer.handPosition(Minecraft.getInstance(), billy, 1.0F)
                : caster.getEyePosition();
        BoundingBox b = run.box;
        double top = b.maxY() + 1;
        List<Vec3> forks = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            Vec3 corner = new Vec3(i % 2 == 0 ? b.minX() : b.maxX() + 1, top, i < 2 ? b.minZ() : b.maxZ() + 1);
            if (corner.distanceToSqr(run.origin) > 1.0 && !forks.contains(corner)) {
                forks.add(corner);
            }
        }
        WorldEffectsRenderer.addHexBolt(new HexBoltPayload(caster.getId(), from, run.origin, forks, run.job * 31L + now));
    }

    /**
     * An Instant spell (3.2) appears all at once: sparks fly off the surface of its box, in its hero's colour, at the same
     * moment the blocks arrive (and {@link #ripple} flashes the box instead of sweeping it).
     */
    private static void pop(ClientLevel level, Run run) {
        RandomSource random = level.getRandom();
        BoundingBox b = run.box;
        double sx = b.getXSpan();
        double sy = b.getYSpan();
        double sz = b.getZSpan();
        double faces = sx * sy + sy * sz + sx * sz;
        int count = (int) Mth.clamp(faces / 8.0, 12.0, EnhancedClientConfig.reducedMotion() ? 16.0 : 64.0);
        int colour = run.hero == Power.BILLY ? BillyAbilities.MAIN : Power.SCARLET_WITCH.glow;
        for (int i = 0; i < count; i++) {
            // A random point of the box's surface: pick a face (by its area), then a spot on it.
            double pick = random.nextDouble() * faces;
            double x = b.minX() + random.nextDouble() * sx;
            double y = b.minY() + random.nextDouble() * sy;
            double z = b.minZ() + random.nextDouble() * sz;
            double nx = 0.0;
            double ny = 0.0;
            double nz = 0.0;
            if (pick < sx * sy) {
                z = random.nextBoolean() ? b.minZ() : b.maxZ() + 1.0;
                nz = z > b.minZ() ? 1.0 : -1.0;
            } else if (pick < sx * sy + sy * sz) {
                x = random.nextBoolean() ? b.minX() : b.maxX() + 1.0;
                nx = x > b.minX() ? 1.0 : -1.0;
            } else {
                y = random.nextBoolean() ? b.minY() : b.maxY() + 1.0;
                ny = y > b.minY() ? 1.0 : -1.0;
            }
            level.addParticle(new EnergyParticleOptions(colour, 0.8F + random.nextFloat() * 0.5F), x, y, z, nx * 0.12 + (random.nextDouble() - 0.5) * 0.04,
                    ny * 0.12 + (random.nextDouble() - 0.5) * 0.04, nz * 0.12 + (random.nextDouble() - 0.5) * 0.04);
        }
    }

    private static void end(int job, State to, long now) {
        Run run = RUNS.get(job);
        if (run != null) {
            run.heard = now;
            run.end(to, now);
        }
    }

    /** A refused spell: a short flicker of its box, and smoke off the caster's hand. */
    private static void fizzle(ClientLevel level, BuildFxPayload payload, long now) {
        Run run = new Run(payload, now, false);
        run.endedAt = now;
        run.state = State.FIZZLED;
        RUNS.remove(payload.job());
        RUNS.put(payload.job(), run);
        if (level.getEntity(payload.casterId()) instanceof AbstractClientPlayer player) {
            Vec3 hand = WorldEffectsRenderer.handPosition(Minecraft.getInstance(), player, 1.0F);
            RandomSource random = level.getRandom();
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.SMOKE, hand.x, hand.y, hand.z, (random.nextDouble() - 0.5) * 0.04, 0.03 + random.nextDouble() * 0.03,
                        (random.nextDouble() - 0.5) * 0.04);
            }
        }
    }

    /** Forgets every spell seen (at logout). */
    public static void clear() {
        RUNS.clear();
        RANKS.clear();
        seenLevel = null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every tick: pruning, wisps to the spells, the builders' hands

    /** One client tick (called by {@link BuildInput}): spells pruned, wisps sent, the builders' hands lit. */
    static void tick(Minecraft mc, ClientLevel level) {
        if (level != seenLevel) {
            // A spell belongs to the world it was cast in.
            seenLevel = level;
            RUNS.clear();
            RANKS.clear();
        }
        long now = level.getGameTime();
        float time = now;
        boolean calm = EnhancedClientConfig.reducedMotion();
        RANKS.removeIf(r -> now - r.bornAt() > RANK_TICKS || now < r.bornAt());
        RUNS.values().removeIf(run -> {
            if (run.state == State.WORKING && now - run.heard > STALE_TICKS) {
                run.end(State.DONE, time);
            }
            return run.over(time) || now < run.bornAt;
        });
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        RandomSource random = level.getRandom();
        for (Run run : RUNS.values()) {
            if (run.state != State.WORKING || run.reverse || !(level.getEntity(run.caster) instanceof AbstractClientPlayer caster)) {
                continue;
            }
            // Wisps from the hands to the spell, along its work.
            if (caster.distanceToSqr(camera) > 128.0 * 128.0 || calm && now % 2 != 0) {
                continue;
            }
            Vec3 target = wisp(run, run.sweep(time));
            int colour = run.hero == Power.BILLY ? BillyAbilities.MAIN : Power.SCARLET_WITCH.glow;
            boolean mine = firstPerson(mc, caster);
            for (HumanoidArm arm : HumanoidArm.values()) {
                Vec3 hand = handOf(mc, caster, arm);
                if (hand == null) {
                    continue;
                }
                Vec3 to = target.subtract(hand);
                double length = to.length();
                if (length < 0.5) {
                    continue;
                }
                double speed = Math.min(0.55, length / 10.0);
                Vec3 v = to.scale(speed / length).add((random.nextDouble() - 0.5) * 0.03, (random.nextDouble() - 0.5) * 0.03,
                        (random.nextDouble() - 0.5) * 0.03);
                if (mine) {
                    // His own hand is right in front of the camera: the wisps start a little way ahead, so they stay small.
                    hand = hand.add(to.scale(1.2 / length));
                }
                level.addParticle(new EnergyParticleOptions(colour, 0.8F + random.nextFloat() * 0.4F), hand.x, hand.y, hand.z, v.x, v.y, v.z);
            }
        }
        for (AbstractClientPlayer player : level.players()) {
            if (player.distanceToSqr(camera) > HAND_RANGE * HAND_RANGE) {
                continue;
            }
            Power power = Powers.power(player);
            BuildView view = player.getExistingData(ModAttachments.BUILD_VIEW).orElse(BuildView.EMPTY);
            if (!BuildClient.isBuilder(power) || !view.active() || player.isSpectator() || player.isInvisible()) {
                continue;
            }
            if (!firstPerson(mc, player)) {
                handParticles(mc, level, player, power, now, calm, random);
            }
        }
    }

    /** Whether this is the player whose eyes the screen shows: his hands are too close to the camera for particles. */
    private static boolean firstPerson(Minecraft mc, AbstractClientPlayer player) {
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /** Where the wisps of a spell fly to: the ripple's front, in the middle of the box across (never the far side). */
    private static Vec3 wisp(Run run, float sweep) {
        BoundingBox b = run.box;
        double cx = (b.minX() + b.maxX() + 1) / 2.0;
        double cz = (b.minZ() + b.maxZ() + 1) / 2.0;
        double y0 = b.minY();
        double y1 = b.maxY() + 1;
        return switch (run.order) {
            case BuildFxPayload.RISE -> new Vec3(cx, Mth.lerp(sweep, y0, y1), cz);
            case BuildFxPayload.MELT -> new Vec3(cx, Mth.lerp(sweep, y1, y0), cz);
            case BuildFxPayload.COLUMNS -> run.origin;
            default -> new Vec3(cx, (y0 + y1) / 2.0, cz);
        };
    }

    /** The middle of a hand: tracked from the model, or the main hand's resting place. */
    private static @Nullable Vec3 handOf(Minecraft mc, AbstractClientPlayer player, HumanoidArm arm) {
        Vec3 tracked = HandTracker.hand(player, arm, 1.0F);
        if (tracked != null) {
            return tracked;
        }
        return arm == player.getMainArm() ? WorldEffectsRenderer.handPosition(mc, player, 1.0F) : null;
    }

    /** What a builder's hands throw off while he is in build mode (D-B 7.1). */
    private static void handParticles(Minecraft mc, ClientLevel level, AbstractClientPlayer player, Power power, long now, boolean calm,
                                      RandomSource random) {
        if (calm && now % 2 != 0) {
            return;
        }
        boolean billy = power == Power.BILLY;
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 hand = handOf(mc, player, arm);
            if (hand == null) {
                continue;
            }
            if (billy) {
                level.addParticle(new EnergyParticleOptions(random.nextInt(3) == 0 ? BillyAbilities.INNER : BillyAbilities.MAIN, 0.7F), hand.x, hand.y,
                        hand.z, (random.nextDouble() - 0.5) * 0.03, 0.01 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.03);
            } else {
                // Two wisps, curling up as they rise.
                for (int i = 0; i < 2; i++) {
                    double turn = (now + i * 9) * 0.5;
                    level.addParticle(new EnergyParticleOptions(i == 0 ? Power.SCARLET_WITCH.glow : Power.SCARLET_WITCH.color, 0.9F), hand.x, hand.y,
                            hand.z, Math.sin(turn) * 0.012, 0.03 + random.nextDouble() * 0.015, Math.cos(turn) * 0.012);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Every frame

    /**
     * Draws everything this class keeps, from {@link BuildRenderer} at {@code AFTER_PARTICLES}: the spells' frames and
     * ripples, Mom's threads, the rank-ups, the sparks between Billy's fingers.
     */
    static void render(Minecraft mc, ClientLevel level, Matrix4f pose, MultiBufferSource.BufferSource buffers, Vec3 camera, float partialTick,
                       float time, boolean calm) {
        if (RUNS.isEmpty() && RANKS.isEmpty() && !anyBilly(level)) {
            return;
        }
        List<Run> runs = new ArrayList<>(RUNS.values());
        if (runs.size() > MAX_RUNS) {
            runs.sort((a, b) -> Double.compare(a.origin.distanceToSqr(camera), b.origin.distanceToSqr(camera)));
            runs = runs.subList(0, MAX_RUNS);
        }
        VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Run run : runs) {
            frame(pose, beams, run, camera, time, calm);
            thread(mc, level, pose, beams, run, camera, partialTick, time);
            if (!calm) {
                lattice(pose, beams, run, camera, time);
            }
        }
        for (Rank rank : RANKS) {
            rankUp(level, pose, beams, rank, camera, partialTick, time, calm);
        }
        if (!calm) {
            fingers(mc, level, pose, beams, camera, partialTick, time);
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);

        if (!calm && !runs.isEmpty()) {
            VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
            for (Run run : runs) {
                ripple(pose, veil, run, camera, time);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
        }
        if (EnhancedClientConfig.buildSeeThrough() && !runs.isEmpty()) {
            VertexConsumer xray = buffers.getBuffer(EnhancedRenderTypes.BUILD_XRAY);
            for (Run run : runs) {
                float glow = run.glow(time);
                if (glow > 0.0F) {
                    BuildRenderer.edges(pose, xray, BuildRenderer.Frame.of(run.box, camera, INFLATE), EDGE,
                            EnergyGeometry.argb(core(run, time), 0.22F * glow));
                }
            }
            buffers.endBatch(EnhancedRenderTypes.BUILD_XRAY);
        }
    }

    private static boolean anyBilly(ClientLevel level) {
        for (AbstractClientPlayer player : level.players()) {
            if (Powers.power(player) == Power.BILLY && player.getExistingData(ModAttachments.BUILD_VIEW).map(BuildView::active).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    /** The colour of a spell's frame: its hero's, pulsing between grey and red when refused (3.3: it blinked). */
    private static int core(Run run, float time) {
        if (run.state == State.FIZZLED) {
            return FastColor.ARGB32.lerp(0.5F + 0.5F * Mth.cos(time * Mth.PI / 2.0F), GREY, RED);
        }
        return run.hero.color;
    }

    private static int glowOf(Run run) {
        return run.reverse ? SHIMMER : run.hero.glow;
    }

    /** The box of a spell, glowing in its hero's colour while it works, then fading (the box edges, two layers). */
    private static void frame(Matrix4f pose, VertexConsumer vc, Run run, Vec3 camera, float time, boolean calm) {
        float glow = run.glow(time);
        if (glow <= 0.0F) {
            return;
        }
        float pulse = calm || run.state != State.WORKING ? 1.0F : 0.85F + 0.15F * Mth.sin(time * 0.4F);
        BuildRenderer.Frame frame = BuildRenderer.Frame.of(run.box, camera, INFLATE);
        float flash = run.state == State.DONE ? 1.0F + Mth.clamp(1.0F - (time - run.endedAt) / 6.0F, 0.0F, 1.0F) : 1.0F;
        BuildRenderer.edges(pose, vc, frame, EDGE * 1.8F, EnergyGeometry.argb(core(run, time), 0.4F * glow * pulse));
        BuildRenderer.edges(pose, vc, frame, EDGE * 0.5F, EnergyGeometry.argb(glowOf(run), Math.min(1.0F, 0.85F * glow * pulse * flash)));
    }

    // ---- The ripple

    /** The band that sweeps the box along the spell's work order, translucent, in the hero's colour. */
    private static void ripple(Matrix4f pose, VertexConsumer vc, Run run, Vec3 camera, float time) {
        float glow = run.glow(time);
        if (glow <= 0.0F || run.state == State.FIZZLED) {
            return;
        }
        float p = run.sweep(time);
        float edge = run.state == State.WORKING ? 1.0F : glow;
        BoundingBox b = run.box;
        if (run.order == BuildFxPayload.POP) {
            // Instant (3.2): no sweep, the whole box flashes once where the blocks have just appeared.
            if (run.state != State.DONE) {
                return;
            }
            float flash = 1.0F - Mth.clamp((time - run.endedAt) / POP_TICKS, 0.0F, 1.0F);
            if (flash > 0.0F) {
                slab(pose, vc, new Vec3(b.minX() - camera.x - INFLATE, b.minY() - camera.y, b.minZ() - camera.z - INFLATE),
                        new Vec3(b.maxX() + 1 - camera.x + INFLATE, b.maxY() + 1 - camera.y, b.maxZ() + 1 - camera.z + INFLATE), run.hero.color,
                        glowOf(run), 0.3F * flash * flash);
            }
            return;
        }
        double x0 = b.minX() - camera.x - INFLATE;
        double x1 = b.maxX() + 1 - camera.x + INFLATE;
        double y0 = b.minY() - camera.y;
        double y1 = b.maxY() + 1 - camera.y;
        double z0 = b.minZ() - camera.z - INFLATE;
        double z1 = b.maxZ() + 1 - camera.z + INFLATE;
        int rgb = run.hero.color;
        int shine = glowOf(run);
        double height = y1 - y0;
        double band = Mth.clamp(height * 0.06, 0.7, 2.5);
        switch (run.order) {
            case BuildFxPayload.RISE, BuildFxPayload.MELT -> {
                double at = run.order == BuildFxPayload.RISE ? y0 + p * height : y1 - p * height;
                double low = Math.max(y0, at - band);
                double high = Math.min(y1, at + band);
                if (high <= low) {
                    return;
                }
                slab(pose, vc, new Vec3(x0, low, z0), new Vec3(x1, high, z1), rgb, shine, 0.2F * edge);
            }
            case BuildFxPayload.SHELLS -> {
                Vec3 centre = new Vec3((x0 + x1) / 2.0, (y0 + y1) / 2.0, (z0 + z1) / 2.0);
                float r = Math.max(0.02F, p);
                ellipsoid(pose, vc, centre, (x1 - x0) / 2.0 * r, height / 2.0 * r, (z1 - z0) / 2.0 * r, rgb, 0.12F * edge, 0.4F * edge);
            }
            default -> {
                // Chunk column by chunk column, outward from the caster: a wall that moves out, cut to the box.
                double ox = run.origin.x - camera.x;
                double oz = run.origin.z - camera.z;
                double far = Math.max(Math.max(Math.hypot(x0 - ox, z0 - oz), Math.hypot(x1 - ox, z0 - oz)),
                        Math.max(Math.hypot(x0 - ox, z1 - oz), Math.hypot(x1 - ox, z1 - oz)));
                double radius = Math.max(0.5, p * far);
                wall(pose, vc, ox, oz, radius, Math.max(1.5, band * 1.5), x0, x1, z0, z1, y0, y1, rgb, 0.22F * edge);
            }
        }
    }

    /** A translucent block: six faces, the top and the bottom brighter, as a plane of light with some thickness. */
    private static void slab(Matrix4f pose, VertexConsumer vc, Vec3 lo, Vec3 hi, int rgb, int shine, float alpha) {
        int side = EnergyGeometry.argb(rgb, alpha);
        int cap = EnergyGeometry.argb(shine, Math.min(1.0F, alpha * 1.8F));
        quad(pose, vc, new Vec3(lo.x, hi.y, lo.z), new Vec3(hi.x, hi.y, lo.z), new Vec3(hi.x, hi.y, hi.z), new Vec3(lo.x, hi.y, hi.z), cap);
        quad(pose, vc, new Vec3(lo.x, lo.y, lo.z), new Vec3(hi.x, lo.y, lo.z), new Vec3(hi.x, lo.y, hi.z), new Vec3(lo.x, lo.y, hi.z), cap);
        quad(pose, vc, new Vec3(lo.x, lo.y, lo.z), new Vec3(hi.x, lo.y, lo.z), new Vec3(hi.x, hi.y, lo.z), new Vec3(lo.x, hi.y, lo.z), side);
        quad(pose, vc, new Vec3(lo.x, lo.y, hi.z), new Vec3(hi.x, lo.y, hi.z), new Vec3(hi.x, hi.y, hi.z), new Vec3(lo.x, hi.y, hi.z), side);
        quad(pose, vc, new Vec3(lo.x, lo.y, lo.z), new Vec3(lo.x, lo.y, hi.z), new Vec3(lo.x, hi.y, hi.z), new Vec3(lo.x, hi.y, lo.z), side);
        quad(pose, vc, new Vec3(hi.x, lo.y, lo.z), new Vec3(hi.x, lo.y, hi.z), new Vec3(hi.x, hi.y, hi.z), new Vec3(hi.x, hi.y, lo.z), side);
    }

    /** An egg-shaped shell round {@code centre}, its rim brighter than its middle (an expanding shell for round shapes). */
    private static void ellipsoid(Matrix4f pose, VertexConsumer vc, Vec3 centre, double rx, double ry, double rz, int rgb, float base, float rim) {
        int segments = 28;
        int rings = 14;
        for (int ring = 0; ring < rings; ring++) {
            float lat0 = Mth.PI * ring / rings - Mth.HALF_PI;
            float lat1 = Mth.PI * (ring + 1) / rings - Mth.HALF_PI;
            for (int seg = 0; seg < segments; seg++) {
                float lon0 = Mth.TWO_PI * seg / segments;
                float lon1 = Mth.TWO_PI * (seg + 1) / segments;
                Vec3 a = onShell(centre, rx, ry, rz, lat0, lon0);
                Vec3 b = onShell(centre, rx, ry, rz, lat0, lon1);
                Vec3 c = onShell(centre, rx, ry, rz, lat1, lon1);
                Vec3 d = onShell(centre, rx, ry, rz, lat1, lon0);
                float mid = (float) Math.abs(Math.sin((lat0 + lat1) / 2.0));
                quad(pose, vc, a, b, c, d, EnergyGeometry.argb(rgb, Mth.lerp(mid * mid, base, rim)));
            }
        }
    }

    private static Vec3 onShell(Vec3 centre, double rx, double ry, double rz, float lat, float lon) {
        double cl = Math.cos(lat);
        return new Vec3(centre.x + rx * cl * Math.cos(lon), centre.y + ry * Math.sin(lat), centre.z + rz * cl * Math.sin(lon));
    }

    /**
     * A wall of light, {@code thick} wide, standing on the circle of {@code radius} round ({@code ox}, {@code oz}) and as
     * tall as the box, drawn only where it is inside the box's footprint.
     */
    private static void wall(Matrix4f pose, VertexConsumer vc, double ox, double oz, double radius, double thick, double x0, double x1, double z0,
                             double z1, double y0, double y1, int rgb, float alpha) {
        int steps = Mth.clamp((int) (radius * Math.PI * 2.0 / 3.0), 24, 360);
        int colour = EnergyGeometry.argb(rgb, alpha);
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2.0 * i / steps;
            double a1 = Math.PI * 2.0 * (i + 1) / steps;
            double ax = ox + Math.cos(a0) * radius;
            double az = oz + Math.sin(a0) * radius;
            double bx = ox + Math.cos(a1) * radius;
            double bz = oz + Math.sin(a1) * radius;
            if (!inside(ax, az, x0, x1, z0, z1) || !inside(bx, bz, x0, x1, z0, z1)) {
                continue;
            }
            double ix = Math.cos(a0) * thick / 2.0;
            double iz = Math.sin(a0) * thick / 2.0;
            double jx = Math.cos(a1) * thick / 2.0;
            double jz = Math.sin(a1) * thick / 2.0;
            quad(pose, vc, new Vec3(ax - ix, y0, az - iz), new Vec3(bx - jx, y0, bz - jz), new Vec3(bx - jx, y1, bz - jz), new Vec3(ax - ix, y1, az - iz), colour);
            quad(pose, vc, new Vec3(ax + ix, y0, az + iz), new Vec3(bx + jx, y0, bz + jz), new Vec3(bx + jx, y1, bz + jz), new Vec3(ax + ix, y1, az + iz), colour);
        }
    }

    private static boolean inside(double x, double z, double x0, double x1, double z0, double z1) {
        return x >= x0 && x <= x1 && z >= z0 && z <= z1;
    }

    /** The marks on the plane of a rising or falling ripple: Wanda's faint hex lattice, Billy's white-hot points. */
    private static void lattice(Matrix4f pose, VertexConsumer vc, Run run, Vec3 camera, float time) {
        float glow = run.glow(time);
        if (glow <= 0.0F || run.state == State.FIZZLED || run.order != BuildFxPayload.RISE && run.order != BuildFxPayload.MELT) {
            return;
        }
        BoundingBox b = run.box;
        double x0 = b.minX() - camera.x;
        double x1 = b.maxX() + 1 - camera.x;
        double z0 = b.minZ() - camera.z;
        double z1 = b.maxZ() + 1 - camera.z;
        double y0 = b.minY() - camera.y;
        double height = b.getYSpan();
        float p = run.sweep(time);
        double y = run.order == BuildFxPayload.RISE ? y0 + p * height : y0 + height - p * height;
        double top = Mth.clamp(y, y0, y0 + height) + 0.02;
        float alpha = 0.55F * (run.state == State.WORKING ? 1.0F : glow);
        if (run.hero == Power.BILLY) {
            // White-hot points, a new set every other tick, each fading in and out (3.3: they popped).
            EnergyGeometry.crossfade(run.seed, time / 2.0F, (random, weight) -> {
                for (int i = 0; i < 14; i++) {
                    double x = Mth.lerp(random.nextDouble(), x0, x1);
                    double z = Mth.lerp(random.nextDouble(), z0, z1);
                    double s = 0.18 + random.nextDouble() * 0.2;
                    int white = EnergyGeometry.argb(WHITE, (alpha + 0.3F) * weight);
                    BuildRenderer.line(pose, vc, new Vec3(x - s, top, z), new Vec3(x + s, top, z), 0.05F, white);
                    BuildRenderer.line(pose, vc, new Vec3(x, top, z - s), new Vec3(x, top, z + s), 0.05F, white);
                }
            });
            return;
        }
        double step = Math.max(2.0, Math.max(x1 - x0, z1 - z0) / 8.0);
        int colour = EnergyGeometry.argb(run.hero.glow, alpha * 0.6F);
        for (double x = x0 + step / 2.0; x < x1; x += step) {
            BuildRenderer.line(pose, vc, new Vec3(x, top, z0), new Vec3(x, top, z1), 0.03F, colour);
        }
        for (double z = z0 + step / 2.0; z < z1; z += step) {
            BuildRenderer.line(pose, vc, new Vec3(x0, top, z), new Vec3(x1, top, z), 0.03F, colour);
        }
    }

    private static void quad(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(argb);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(argb);
        vc.addVertex(pose, (float) c.x, (float) c.y, (float) c.z).setColor(argb);
        vc.addVertex(pose, (float) d.x, (float) d.y, (float) d.z).setColor(argb);
    }

    // ---- Mom's thread

    /** A thin scarlet thread from the Scarlet Witch's hand to Billy's while she helps his spell (D-B 8.3). */
    private static void thread(Minecraft mc, ClientLevel level, Matrix4f pose, VertexConsumer vc, Run run, Vec3 camera, float partialTick, float time) {
        if (run.momId < 0 || run.state != State.WORKING || run.reverse) {
            return;
        }
        if (!(level.getEntity(run.caster) instanceof AbstractClientPlayer billy) || !(level.getEntity(run.momId) instanceof AbstractClientPlayer mom)) {
            return;
        }
        Vec3 start = WorldEffectsRenderer.handPosition(mc, mom, partialTick).subtract(camera);
        Vec3 end = WorldEffectsRenderer.handPosition(mc, billy, partialTick).subtract(camera);
        // (3.3) It writhes from one shape to the next instead of jumping every third tick.
        long seed = mom.getId() * 6364136223846793005L;
        EnergyGeometry.bolt(pose, vc, start, end, 0.03F, 0.25F, EnergyGeometry.argb(Power.SCARLET_WITCH.color, 0.7F),
                seed, time / 3.0F);
        EnergyGeometry.bolt(pose, vc, start, end, 0.018F, 0.4F, EnergyGeometry.argb(Power.SCARLET_WITCH.glow, 0.45F),
                seed + 31, time / 3.0F);
    }

    // ---- Billy's rank-up

    /** The rings that widen round Billy and the runes that spin up him (D-B 7.2, 8.2). */
    private static void rankUp(ClientLevel level, Matrix4f pose, VertexConsumer vc, Rank rank, Vec3 camera, float partialTick, float time, boolean calm) {
        if (!(level.getEntity(rank.entityId()) instanceof AbstractClientPlayer billy) || billy.distanceToSqr(camera) > 48.0 * 48.0) {
            return;
        }
        float age = Math.max(0.0F, time - rank.bornAt());
        float life = age / RANK_TICKS;
        float envelope = Math.min(1.0F, age / 4.0F) * (1.0F - Mth.clamp((age - (RANK_TICKS - 12)) / 12.0F, 0.0F, 1.0F));
        Vec3 feet = billy.getPosition(partialTick).subtract(camera);
        float height = billy.getBbHeight();
        // Rings: three, one after another, widening and rising along him.
        for (int i = 0; i < 3; i++) {
            float at = Mth.clamp((age - i * 5.0F) / 22.0F, 0.0F, 1.0F);
            if (at <= 0.0F || at >= 1.0F) {
                continue;
            }
            float radius = 0.55F + 1.1F * at;
            Vec3 centre = feet.add(0, height * (0.15 + 0.85 * (calm ? 0.5F : at)), 0);
            float fade = (1.0F - at) * envelope;
            EnergyGeometry.ring(pose, vc, centre, UP, radius, 0.06F, 28, EnergyGeometry.argb(BillyAbilities.MAIN, 0.55F * fade));
            EnergyGeometry.ring(pose, vc, centre, UP, radius, 0.025F, 28, EnergyGeometry.argb(WHITE, 0.85F * fade));
        }
        // His crown flares white: a halo about his head that widens and fades in the first ten ticks.
        float flare = 1.0F - Mth.clamp(age / 10.0F, 0.0F, 1.0F);
        if (flare > 0.0F) {
            Vec3 head = feet.add(0, height + 0.05, 0);
            float widen = 0.3F + 0.4F * (1.0F - flare);
            EnergyGeometry.ring(pose, vc, head, UP, widen, 0.07F, 24, EnergyGeometry.argb(WHITE, 0.8F * flare));
            EnergyGeometry.ring(pose, vc, head, UP, widen * 0.7F, 0.04F, 24, EnergyGeometry.argb(BillyAbilities.INNER, 0.9F * flare));
        }
        // Runes: a ring of them spins round him and climbs.
        float spin = calm ? 0.0F : age * 0.3F;
        double rise = calm ? 0.5 : Mth.clamp(life * 1.4F, 0.0F, 1.0F);
        for (int i = 0; i < RUNES; i++) {
            double a = spin + Mth.TWO_PI * i / RUNES;
            double y = height * (0.1 + 0.9 * ((rise + (i % 3) * 0.12) % 1.0));
            Vec3 at = feet.add(Math.cos(a) * 1.05, y, Math.sin(a) * 1.05);
            if (at.lengthSqr() < 2.0 * 2.0) {
                // A rune right against the camera (his own, in first person) would fill the screen.
                continue;
            }
            rune(pose, vc, at, 0.13F, GLYPHS[i % GLYPHS.length], EnergyGeometry.argb(i % 2 == 0 ? WHITE : BillyAbilities.INNER, 0.9F * envelope));
        }
    }

    /** One rune, facing the camera: its strokes as short ribbons. */
    private static void rune(Matrix4f pose, VertexConsumer vc, Vec3 at, float size, float[] strokes, int argb) {
        Vec3 toCamera = at.scale(-1.0);
        Vec3 right = toCamera.cross(UP);
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(toCamera).normalize();
        float width = Math.max(0.03F * size / 0.13F, (float) at.length() * 0.0025F);
        for (int i = 0; i + 3 < strokes.length; i += 4) {
            Vec3 from = at.add(right.scale(strokes[i] * size)).add(up.scale(strokes[i + 1] * size));
            Vec3 to = at.add(right.scale(strokes[i + 2] * size)).add(up.scale(strokes[i + 3] * size));
            EnergyGeometry.ribbon(pose, vc, from, to, width, argb, argb);
        }
    }

    // ---- Billy's fingers

    /** A tiny arc between Billy's fingers every second, while he is in build mode (D-B 7.1). */
    private static void fingers(Minecraft mc, ClientLevel level, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        for (AbstractClientPlayer player : level.players()) {
            if (Powers.power(player) != Power.BILLY || player.isInvisible() || player.isSpectator()
                    || player.distanceToSqr(camera) > HAND_RANGE * HAND_RANGE
                    || !player.getExistingData(ModAttachments.BUILD_VIEW).map(BuildView::active).orElse(false)) {
                continue;
            }
            float phase = (time + player.getId() * 7) % 20.0F;
            if (phase >= 3.0F) {
                continue;
            }
            HumanoidArm arm = ((int) ((time + player.getId() * 7) / 20.0F) & 1) == 0 ? player.getMainArm() : player.getMainArm().getOpposite();
            Vec3 hand = HandTracker.hand(player, arm, partialTick);
            if (hand == null) {
                if (arm != player.getMainArm()) {
                    continue;
                }
                hand = WorldEffectsRenderer.handPosition(mc, player, partialTick);
            }
            // A new spark every tick, each fading into the next (3.3: they jumped).
            Vec3 at = hand.subtract(camera);
            float fade = 1.0F - phase / 3.0F;
            EnergyGeometry.crossfade(player.getId() * 341873128712L, time, (random, weight) -> {
                Vec3 along = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.2,
                        random.nextDouble() - 0.5).normalize()
                        .scale(0.14 + random.nextDouble() * 0.1);
                Vec3 a = at.subtract(along.scale(0.5));
                Vec3 b = a.add(along);
                long seed = random.nextLong();
                EnergyGeometry.bolt(pose, vc, a, b, 0.02F, 0.03F, EnergyGeometry.argb(BillyAbilities.MAIN,
                        0.7F * fade * weight), seed);
                EnergyGeometry.bolt(pose, vc, a, b, 0.009F, 0.03F, EnergyGeometry.argb(BillyAbilities.CORE,
                        0.95F * fade * weight), seed + 1);
            });
        }
    }

    private BuildFx() {
    }
}
