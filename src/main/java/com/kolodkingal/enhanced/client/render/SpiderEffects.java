package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.SpiderMotion;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CustomBeams;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.SpiderFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spider-Man's web and body effects, drawn in the world for everyone in view (DESIGN section 14): the strand of a Web
 * Shot and the splat where it lands, the strands of a Web Burst, the line from his hand to the anchor while he zips or
 * swings and how it lets go, the white motes and the soft stick of his hands as he climbs, the streaks behind a fast
 * zip, the wisps of the Black Eyes, and the field of view's small kick as a zip begins.
 *
 * <p>The server says what happened ({@link SpiderFxPayload}); what lasts (a zip's line, a climb) is read from the
 * synced state of the hero ({@link Live}), so a late joiner sees it too. Everything is drawn where the body really is
 * ({@link HandTracker}), thinned by the client's flight effects option, and calmer with reduced motion.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SpiderEffects {
    /** The splat a web leaves on a wall: eight spokes, three rings and a bright centre. */
    public static final ResourceLocation SPLAT = Enhanced.id("textures/effect/web_splat.png");

    /** Silver-white webs and their shimmer; organic webs are thicker and creamier. */
    private static final int SILVER = 0xF2F6FA;
    private static final int SHIMMER = 0xBFD3E6;
    private static final int CREAM = 0xF4EFE3;
    private static final int CREAM_SHIMMER = 0xE7DCC4;
    private static final int WHITE = 0xFFFFFF;
    /** The Black Eyes' wisps: black, with a dark red fleck. */
    private static final int WISP = 0x1A0608;
    private static final int GLINT = 0x8A1020;

    private static final Vec3 UP = new Vec3(0, 1, 0);
    /** Beyond this far the effects of a hero are not made at all. */
    private static final double RANGE = 64.0;
    /** A hand leaves the wall every this many blocks of climbing. */
    private static final double STEP = 0.4;
    /** At this speed (blocks a tick) a zip, a swing or a black-eyed climb leaves streaks behind it. */
    private static final double STREAK_SPEED = 0.9;
    /**
     * A web that has not been told to end lets go when its hero has not been zipping or swinging for this many ticks.
     */
    private static final int LOST_TICKS = 6;

    /** One strand of a Web Shot. */
    private record Shot(int player, Vec3 from, Vec3 to, boolean organic, long born) {
    }

    /** A Web Burst: the strands go from the chest to where they ended, each bent by a little, fixed by the seed. */
    private record Burst(int player, Vec3 from, List<Vec3> ends, long seed, boolean organic, long born) {
    }

    /** A flat web on a face of a block (3.5: {@code tint} the colour chosen for the web that made it, or -1). */
    private record Splat(Vec3 at, Vec3 normal, float size, int life, float turn, long born, int tint) {
    }

    /** 3.5: the colour a custom hero's pick chose for the web being drawn, or -1 (silver, cream, white). */
    private static int tint = -1;

    /** The colour chosen for a hero's web ability ({@code player} an entity id), or -1. */
    private static int tintOf(int player, HeroAbility row) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? -1 : AbilityColors.chosen(level.getEntity(player), row);
    }

    /**
     * One of the web's colours, or (3.5) the chosen colour: a web is near white, so it does not take the hue as the
     * other effects do: the thread is the colour itself, its shimmer paler and its core paler still.
     */
    private static int web(int rgb) {
        if (tint < 0) {
            return rgb;
        }
        return switch (rgb) {
            case SILVER, CREAM -> tint;
            case SHIMMER, CREAM_SHIMMER -> CustomBeams.mix(tint, 0xFFFFFF, 0.35F);
            default -> CustomBeams.mix(tint, 0xFFFFFF, 0.6F);
        };
    }

    /** The line of a zip or a swing that is being held, from his hand to the anchor. */
    private static final class Web {
        final int player;
        final Vec3 from;
        final Vec3 anchor;
        /** The creature a zip is aimed at, followed as it moves, or -1. */
        final int creature;
        final boolean organic;
        final boolean swing;
        final long born;
        int lost;

        Web(int player, Vec3 from, Vec3 anchor, int creature, boolean organic, boolean swing, long born) {
            this.player = player;
            this.from = from;
            this.anchor = anchor;
            this.creature = creature;
            this.organic = organic;
            this.swing = swing;
            this.born = born;
        }
    }

    /** A line that let go: the hand end flies to the anchor while it fades. */
    private record Released(int player, Vec3 from, Vec3 anchor, boolean organic, long at, boolean swing) {
    }

    /** A leg of the Iron Spider striking at a point. */
    public record Strike(Vec3 to, long born) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final List<Splat> SPLATS = new ArrayList<>();
    private static final Map<Integer, Web> WEBS = new HashMap<>();
    private static final List<Released> RELEASED = new ArrayList<>();
    private static final Map<Long, Strike> STRIKES = new HashMap<>();
    /** How far each climbing hero has gone since a hand last touched the wall. */
    private static final Map<Integer, Double> STEPS = new HashMap<>();
    private static final Map<Integer, Integer> HANDS = new HashMap<>();
    /** The field of view's kick for the local hero, 0 to 1. */
    private static float kick;
    private static float kickOld;

    // =================================================================================================================
    // The server says
    // =================================================================================================================

    /** The server says something happened nearby: draw it, and tell the local hero's movement. */
    public static void add(SpiderFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        SpiderMotion.onFx(payload);
        long now = level.getGameTime();
        Entity owner = level.getEntity(payload.player());
        if (payload.kind() != SpiderFxPayload.Kind.ZIP_END && payload.kind() != SpiderFxPayload.Kind.SWING_END && owner == null) {
            return;
        }
        switch (payload.kind()) {
            case SHOT -> {
                tint = tintOf(payload.player(), HeroAbility.SPIDER_WEB);
                SHOTS.add(new Shot(payload.player(), payload.from(), payload.to(), payload.organic(), now));
                if (payload.normal().lengthSqr() > 0.01) {
                    SPLATS.add(new Splat(payload.to(), payload.normal(), 0.7F, WebTiming.SPLAT_TICKS, turn(payload.to()), now, tint));
                }
                impact(level, payload.to(), payload.arg() >= 0);
                tint = -1;
            }
            case BURST -> {
                tint = tintOf(payload.player(), HeroAbility.SPIDER_WEB_BURST);
                BURSTS.add(new Burst(payload.player(), payload.from(), List.copyOf(payload.points()), payload.seed(), payload.organic(), now));
                burstSplats(level, payload.from(), payload.points(), now);
                tint = -1;
            }
            case ZIP_START, SWING_START -> {
                boolean swing = payload.kind() == SpiderFxPayload.Kind.SWING_START;
                release(level, payload.player(), now);
                WEBS.put(payload.player(), new Web(payload.player(), payload.from(), payload.to(), swing ? -1 : payload.arg(), payload.organic(), swing, now));
                HeroAnimator.onSpiderFx(payload.player(), payload.to());
            }
            case ZIP_END, SWING_END -> {
                Web web = WEBS.get(payload.player());
                // (A line that was replaced already let go: only the one the message names ends.)
                if (web != null && web.swing == (payload.kind() == SpiderFxPayload.Kind.SWING_END) && web.anchor.distanceToSqr(payload.to()) < 1.0) {
                    release(level, payload.player(), now);
                }
            }
            case LEG_STRIKE -> {
                STRIKES.put(strikeKey(payload.player(), payload.arg()), new Strike(payload.to(), now));
                legSpark(level, payload.to(), owner);
            }
        }
    }

    private static long strikeKey(int player, int leg) {
        return ((long) player << 3) | (leg & 7);
    }

    /** The strike a leg of this hero is making, or null. */
    public static Strike strike(int player, int leg) {
        return STRIKES.get(strikeKey(player, leg));
    }

    /** A direction for a splat to turn to, from where it lands, the same on every client. */
    private static float turn(Vec3 at) {
        long h = Double.doubleToLongBits(at.x * 31.0 + at.y * 17.0 + at.z * 7.0);
        return (float) (((h ^ (h >>> 29)) & 0xFFFF) / 65536.0 * Mth.TWO_PI);
    }

    /** The web lands: motes of web, and fragments of cobweb if it landed on a creature. */
    private static void impact(ClientLevel level, Vec3 at, boolean creature) {
        RandomSource random = level.random;
        double density = EnhancedClientConfig.flightEffects() ? 1.0 : 0.25;
        for (int i = 0; i < 4; i++) {
            if (random.nextDouble() <= density) {
                level.addParticle(new EnergyParticleOptions(web(SILVER), 0.6F), at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.12,
                        random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.12);
            }
        }
        if (creature) {
            ItemParticleOption cobweb = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB));
            for (int i = 0; i < 3; i++) {
                level.addParticle(cobweb, at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.12,
                        (random.nextDouble() - 0.5) * 0.2);
            }
        }
    }

    /** Where a strand of a burst met a block, a small splat on its face (a creature or the air leaves none). */
    private static void burstSplats(ClientLevel level, Vec3 from, List<Vec3> ends, long now) {
        for (Vec3 end : ends) {
            Vec3 ray = end.subtract(from);
            double length = ray.length();
            if (length < 0.5) {
                continue;
            }
            Vec3 beyond = end.add(ray.scale(0.06 / length));
            BlockHitResult hit = level.clip(new ClipContext(from, beyond, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    Minecraft.getInstance().player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceToSqr(end) < 0.09) {
                Direction face = hit.getDirection();
                SPLATS.add(new Splat(hit.getLocation(), new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()), 0.35F,
                        WebTiming.BURST_SPLAT_TICKS, turn(hit.getLocation()), now, tint));
            }
        }
    }

    /**
     * The line of this hero lets go (if he had one): it flies to its anchor and fades, and a few motes fall from there.
     */
    private static void release(ClientLevel level, int player, long now) {
        Web web = WEBS.remove(player);
        if (web == null) {
            return;
        }
        Vec3 anchor = anchorOf(level, web);
        RELEASED.add(new Released(player, hand(level, web), anchor, web.organic, now, web.swing));
        RandomSource random = level.random;
        tint = tintOf(player, web.swing ? HeroAbility.SPIDER_WEB_SWING : HeroAbility.SPIDER_ZIP);
        for (int i = 0; i < 5; i++) {
            level.addParticle(new EnergyParticleOptions(web(WHITE), 0.5F), anchor.x + (random.nextDouble() - 0.5) * 0.3, anchor.y,
                    anchor.z + (random.nextDouble() - 0.5) * 0.3, 0, -0.04 - random.nextDouble() * 0.04, 0);
        }
        tint = -1;
    }

    /** The red-gold spark where a leg's tip strikes. */
    private static void legSpark(ClientLevel level, Vec3 at, @Nullable Entity owner) {
        RandomSource random = level.random;
        for (int i = 0; i < 3; i++) {
            level.addParticle(new EnergyParticleOptions(AbilityColors.shade(owner, HeroAbility.SPIDER_LEGS, i == 0
                    ? 0xFFC247 : 0xFF2A2A), 0.8F), at.x, at.y, at.z,
                    (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);
        }
    }

    // =================================================================================================================
    // Every tick
    // =================================================================================================================

    /** Once per client tick: the effects that last while a state does, and the end of those that ran out. */
    public static void tick(ClientLevel level) {
        long now = level.getGameTime();
        SHOTS.removeIf(s -> now - s.born() >= WebTiming.SHOT_TICKS || now < s.born());
        BURSTS.removeIf(b -> now - b.born() >= WebTiming.BURST_TICKS || now < b.born());
        SPLATS.removeIf(s -> now - s.born() >= s.life() || now < s.born());
        RELEASED.removeIf(r -> now - r.at() >= WebTiming.RELEASE_FADE + 1 || now < r.at());
        STRIKES.values().removeIf(s -> now - s.born() > 10 || now < s.born());
        double distance = RANGE * RANGE;
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        float density = EnhancedClientConfig.flightEffects() ? 1.0F : 0.1F;
        List<Integer> seen = new ArrayList<>();
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (!data.acts(Power.SPIDER_MAN) || player.isSpectator()) {
                continue;
            }
            seen.add(player.getId());
            if (player.position().distanceToSqr(camera) > distance) {
                continue;
            }
            Web web = WEBS.get(player.getId());
            if (web != null) {
                boolean held = data.is(web.swing ? Live.SWINGING : Live.ZIPPING);
                web.lost = held ? 0 : web.lost + 1;
                if (web.lost > LOST_TICKS || now - web.born > 400 || !player.isAlive()) {
                    release(level, player.getId(), now);
                }
            }
            if (player.isInvisible()) {
                continue;
            }
            boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
            tint = AbilityColors.chosen(player, HeroAbility.SPIDER_CRAWL);
            climb(level, player, data, density);
            tint = AbilityColors.chosen(player, data.is(Live.SWINGING) ? HeroAbility.SPIDER_WEB_SWING
                    : data.is(Live.ZIPPING) ? HeroAbility.SPIDER_ZIP
                    : HeroAbility.SPIDER_CRAWL);
            streaks(level, player, data, density);
            tint = -1;
            if (data.is(Live.BLACK_EYES) && !firstPerson) {
                wisps(level, player, density);
            }
        }
        WEBS.keySet().removeIf(id -> !seen.contains(id));
        STEPS.keySet().removeIf(id -> !seen.contains(id));
        HANDS.keySet().removeIf(id -> !seen.contains(id));
        // The field of view kicks as a zip begins and eases back over four ticks.
        kickOld = kick;
        boolean zipping = mc.player != null && (SpiderMotion.mode() == SpiderMotion.Mode.ZIP || Powers.get(mc.player).is(Live.ZIPPING));
        kick = Mth.clamp(kick + (zipping ? 0.25F : -0.25F), 0.0F, 1.0F);
    }

    /** A hand sticks to the wall every few steps: a soft sound, a white mote and two fibres where it meets it. */
    private static void climb(ClientLevel level, AbstractClientPlayer player, PowerData data, float density) {
        boolean wall = data.is(Live.CLINGING_WALL);
        boolean ceiling = data.is(Live.CLINGING_CEILING);
        if (!wall && !ceiling) {
            STEPS.remove(player.getId());
            return;
        }
        double moved = Math.hypot(Math.hypot(player.getX() - player.xOld, player.getY() - player.yOld), player.getZ() - player.zOld);
        if (moved < 0.02) {
            return;
        }
        double along = STEPS.merge(player.getId(), moved, Double::sum);
        if (along < STEP) {
            return;
        }
        STEPS.put(player.getId(), along - STEP);
        RandomSource random = level.random;
        level.playLocalSound(player.getX(), player.getY() + 0.9, player.getZ(), ModSounds.WEB_STICK.get(), SoundSource.PLAYERS,
                0.5F + random.nextFloat() * 0.3F, 0.9F + random.nextFloat() * 0.25F, false);
        int hand = HANDS.merge(player.getId(), 1, (a, b) -> a ^ b);
        HumanoidArm arm = hand == 0 ? player.getMainArm() : player.getMainArm().getOpposite();
        Vec3 at = HandTracker.hand(player, arm, 1.0F);
        if (at == null) {
            at = player.position().add(0, wall ? 1.6 : 1.9, 0);
        }
        if (random.nextFloat() < density) {
            level.addParticle(new EnergyParticleOptions(web(WHITE), 0.5F), at.x, at.y, at.z, 0, 0, 0);
            for (int i = 0; i < 2; i++) {
                level.addParticle(new EnergyParticleOptions(web(SILVER), 0.35F), at.x + (random.nextDouble() - 0.5) * 0.15, at.y + (random.nextDouble() - 0.5) * 0.15,
                        at.z + (random.nextDouble() - 0.5) * 0.15, 0, -0.01, 0);
            }
        }
    }

    /** Three white streaks a tick laid along the path he has just travelled, when a zip, a swing or a climb is fast. */
    private static void streaks(ClientLevel level, AbstractClientPlayer player, PowerData data, float density) {
        double dx = player.getX() - player.xOld;
        double dy = player.getY() - player.yOld;
        double dz = player.getZ() - player.zOld;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean moving = data.is(Live.ZIPPING) || data.is(Live.SWINGING) || data.is(Live.CLINGING_WALL) || data.is(Live.CLINGING_CEILING);
        if (speed < STREAK_SPEED || (!moving && speed < 1.4) || speed > 6.0) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < 3; i++) {
            if (random.nextFloat() > density) {
                continue;
            }
            double back = random.nextDouble();
            level.addParticle(new EnergyParticleOptions(web(WHITE), 0.5F), player.getX() - dx * back + (random.nextDouble() - 0.5) * 0.4,
                    player.getY() - dy * back + 0.2 + random.nextDouble() * 1.4, player.getZ() - dz * back + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
        }
    }

    /** Black wisps rising off the shoulders of the Black Eyes, with a dark red fleck now and then. */
    private static void wisps(ClientLevel level, AbstractClientPlayer player, float density) {
        RandomSource random = level.random;
        for (int i = 0; i < 2; i++) {
            if (random.nextFloat() > Math.max(density, 0.2F)) {
                continue;
            }
            Vec3 shoulder = HandTracker.shoulder(player, i == 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT, 1.0F);
            if (shoulder == null) {
                shoulder = player.position().add((random.nextDouble() - 0.5) * 0.6, 1.5, (random.nextDouble() - 0.5) * 0.6);
            }
            level.addParticle(new EnergyParticleOptions(WISP, 1.1F), shoulder.x, shoulder.y, shoulder.z, (random.nextDouble() - 0.5) * 0.02, 0.04 + random.nextDouble() * 0.03,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        if (player.tickCount % 3 == 0 && random.nextFloat() <= Math.max(density, 0.3F)) {
            level.addParticle(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.SPIDER_BLACK_EYES, GLINT), 0.5F), player.getRandomX(0.5), player.getY() + 0.8 + random.nextDouble() * 1.0, player.getRandomZ(0.5),
                    0, 0.03, 0);
        }
    }

    /** Where his hand is: the real one when the body was drawn lately, otherwise where the server said. */
    private static Vec3 hand(ClientLevel level, Web web) {
        Entity owner = level.getEntity(web.player);
        if (owner instanceof AbstractClientPlayer player) {
            return WorldEffectsRenderer.handPosition(Minecraft.getInstance(), player, 1.0F);
        }
        return web.from;
    }

    private static Vec3 anchorOf(ClientLevel level, Web web) {
        Entity target = web.creature >= 0 ? level.getEntity(web.creature) : null;
        if (target != null && !target.isRemoved()) {
            return target.position().add(0, target.getBbHeight() * 0.5, 0);
        }
        return web.anchor;
    }

    public static void clear() {
        SHOTS.clear();
        BURSTS.clear();
        SPLATS.clear();
        WEBS.clear();
        RELEASED.clear();
        STRIKES.clear();
        STEPS.clear();
        HANDS.clear();
        kick = 0.0F;
        kickOld = 0.0F;
    }

    // =================================================================================================================
    // The field of view
    // =================================================================================================================

    /** The field of view widens by a twentieth as a zip begins (not with reduced motion). */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (kick > 0.0F || kickOld > 0.0F) {
            if (EnhancedClientConfig.reducedMotion() || !EnhancedClientConfig.animations()) {
                return;
            }
            float eased = Mth.lerp(0.5F, kickOld, kick);
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.05F * eased * eased * (3.0F - 2.0F * eased)));
        }
    }

    // =================================================================================================================
    // Drawing
    // =================================================================================================================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || (SHOTS.isEmpty() && BURSTS.isEmpty() && SPLATS.isEmpty() && WEBS.isEmpty() && RELEASED.isEmpty())) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // The splats first: flat on the blocks, textured.
        if (!SPLATS.isEmpty()) {
            RenderType decal = EnhancedRenderTypes.webDecal(SPLAT);
            VertexConsumer decals = buffers.getBuffer(decal);
            for (Splat splat : SPLATS) {
                tint = splat.tint();
                splat(pose, decals, splat, camera, time);
            }
            tint = -1;
            buffers.endBatch(decal);
        }

        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        for (Shot shot : SHOTS) {
            tint = tintOf(shot.player(), HeroAbility.SPIDER_WEB);
            float age = time - shot.born();
            float alpha = WebTiming.shotAlpha(age);
            Vec3 a = origin(level, mc, shot.player(), shot.from(), partialTick);
            Vec3 b = a.lerp(shot.to(), WebTiming.shotReach(age));
            thread(pose, vc, a.subtract(camera), b.subtract(camera), shot.organic(), alpha, time, age);
        }
        for (Burst burst : BURSTS) {
            tint = tintOf(burst.player(), HeroAbility.SPIDER_WEB_BURST);
            float age = time - burst.born();
            burst(pose, vc, burst, camera, age, time);
        }
        for (Web web : WEBS.values()) {
            tint = tintOf(web.player, web.swing ? HeroAbility.SPIDER_WEB_SWING : HeroAbility.SPIDER_ZIP);
            Vec3 a = origin(level, mc, web.player, web.from, partialTick);
            thread(pose, vc, a.subtract(camera), anchorOf(level, web).subtract(camera), web.organic, 1.0F, time, time - web.born);
        }
        for (Released released : RELEASED) {
            tint = tintOf(released.player(), released.swing() ? HeroAbility.SPIDER_WEB_SWING : HeroAbility.SPIDER_ZIP);
            float age = time - released.at();
            Vec3 a = released.from().lerp(released.anchor(), WebTiming.releaseFly(age));
            thread(pose, vc, a.subtract(camera), released.anchor().subtract(camera), released.organic(), WebTiming.releaseAlpha(age), time, 99.0F);
        }
        tint = -1;
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
    }

    /** Where a web leaves the hero this frame: his real hand, or where the server guessed it. */
    private static Vec3 origin(ClientLevel level, Minecraft mc, int player, Vec3 fallback, float partialTick) {
        if (level.getEntity(player) instanceof AbstractClientPlayer hero) {
            return WorldEffectsRenderer.handPosition(mc, hero, partialTick);
        }
        return fallback;
    }

    /**
     * A web thread between two points (relative to the camera): a camera-facing ribbon, silver-white (cream if
     * organic), with a paler shimmer round it and a thin white core, rippling sideways while it is slack. Thinner near
     * the camera, so a thread that leaves a hand in front of it never fills the screen.
     */
    private static void thread(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, boolean organic, float alpha, float time, float age) {
        double length = a.distanceTo(b);
        if (alpha <= 0.01F || length < 0.02) {
            return;
        }
        float width = organic ? 0.055F : 0.03F;
        int main = web(organic ? CREAM : SILVER);
        int shimmer = web(organic ? CREAM_SHIMMER : SHIMMER);
        int steps = Mth.clamp((int) (length * 1.5), 2, 40);
        Vec3 dir = b.subtract(a).scale(1.0 / length);
        Vec3 side = dir.cross(UP);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 prev = a;
        float prevNear = near(a);
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            Vec3 point = a.add(dir.scale(length * t));
            if (i < steps) {
                point = point.add(side.scale(WebTiming.ripple(time, (float) (length * t), age, t)));
            }
            float near = near(point);
            EnergyGeometry.ribbon(pose, vc, prev, point, width * 2.2F * prevNear, width * 2.2F * near, EnergyGeometry.argb(shimmer, 0.35F * alpha),
                    EnergyGeometry.argb(shimmer, 0.35F * alpha));
            EnergyGeometry.ribbon(pose, vc, prev, point, width * prevNear, width * near, EnergyGeometry.argb(main, 0.9F * alpha),
                    EnergyGeometry.argb(main, 0.9F * alpha));
            EnergyGeometry.ribbon(pose, vc, prev, point, width * 0.35F * prevNear, width * 0.35F * near, EnergyGeometry.argb(web(WHITE), alpha),
                    EnergyGeometry.argb(web(WHITE), alpha));
            prev = point;
            prevNear = near;
        }
    }

    /** How thick a thread is at a point (camera-relative): a quarter near the camera, full from 1.5 blocks away. */
    private static float near(Vec3 point) {
        return (float) Mth.clamp(point.length() / 1.5, 0.25, 1.0);
    }

    /**
     * The strands of a burst: each from the chest, bent a quarter of a block sideways in a direction the seed fixes.
     */
    private static void burst(Matrix4f pose, VertexConsumer vc, Burst burst, Vec3 camera, float age, float time) {
        float reach = WebTiming.burstReach(age);
        float alpha = WebTiming.burstAlpha(age);
        if (alpha <= 0.01F) {
            return;
        }
        for (int i = 0; i < burst.ends().size(); i++) {
            Vec3 end = burst.ends().get(i);
            Vec3 ray = end.subtract(burst.from());
            double length = ray.length();
            if (length < 0.2) {
                continue;
            }
            Vec3 dir = ray.scale(1.0 / length);
            RandomSource random = RandomSource.create(burst.seed() * 31L + i * 7919L);
            Vec3 bend = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
            bend = bend.subtract(dir.scale(bend.dot(dir)));
            bend = bend.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : bend.normalize().scale(0.25);
            int segments = Mth.clamp((int) (length), 3, 10);
            Vec3 prev = burst.from().subtract(camera);
            float width = burst.organic() ? 0.055F : 0.03F;
            for (int s = 1; s <= segments; s++) {
                float t = reach * s / segments;
                Vec3 point = burst.from().add(ray.scale(t)).add(bend.scale(Mth.sin(Mth.PI * t))).subtract(camera);
                int main = web(burst.organic() ? CREAM : SILVER);
                EnergyGeometry.ribbon(pose, vc, prev, point, width * near(prev), width * near(point), EnergyGeometry.argb(main, 0.9F * alpha),
                        EnergyGeometry.argb(main, 0.9F * alpha));
                EnergyGeometry.ribbon(pose, vc, prev, point, width * 0.35F * near(prev), width * 0.35F * near(point), EnergyGeometry.argb(web(WHITE), alpha),
                        EnergyGeometry.argb(web(WHITE), alpha));
                prev = point;
            }
        }
    }

    /** A splat: a flat square on a block's face, a hair off it, turned by a fixed random angle. */
    private static void splat(Matrix4f pose, VertexConsumer vc, Splat splat, Vec3 camera, float time) {
        float alpha = WebTiming.splatAlpha(time - splat.born(), splat.life());
        if (alpha <= 0.01F) {
            return;
        }
        Vec3 n = splat.normal();
        Vec3 u = Math.abs(n.y) > 0.5 ? new Vec3(1, 0, 0) : new Vec3(-n.z, 0, n.x);
        u = u.normalize();
        Vec3 v = n.cross(u).normalize();
        double cos = Math.cos(splat.turn());
        double sin = Math.sin(splat.turn());
        Vec3 a = u.scale(cos).add(v.scale(sin)).scale(splat.size() * 0.5);
        Vec3 b = v.scale(cos).subtract(u.scale(sin)).scale(splat.size() * 0.5);
        Vec3 c = splat.at().add(n.scale(0.01)).subtract(camera);
        int color = EnergyGeometry.argb(web(WHITE), alpha);
        corner(pose, vc, c.subtract(a).subtract(b), 0, 0, color);
        corner(pose, vc, c.add(a).subtract(b), 1, 0, color);
        corner(pose, vc, c.add(a).add(b), 1, 1, color);
        corner(pose, vc, c.subtract(a).add(b), 0, 1, color);
    }

    private static void corner(Matrix4f pose, VertexConsumer vc, Vec3 p, float u, float v, int color) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(color);
    }

    private SpiderEffects() {
    }
}
