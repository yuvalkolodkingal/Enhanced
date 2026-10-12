package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.SpiderWebs;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.SpiderMotion;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * What the client does to act Spider-Man besides the poses of {@link SpiderPoses} and the keys of {@link
 * SpiderGestures}: the gesture his own key press starts before the server has answered (with the guards only a client
 * can see), the gestures the animator starts from his motion (a zip or a swing that his own movement has begun, the
 * push off a wall, the grab of a wall at the end of a zip, the landing after a fast fall), the movement of his hands in
 * first person, and the flourishes that go with his gestures ({@link SpiderGestureFx}). Client only: {@link
 * SpiderClient} hands its arguments on to it.
 */
final class SpiderActing {
    /** What each hero was seen doing the tick before. The state of a hero who is no longer drawn is let go with it. */
    private static final Map<HeroAnimState, SpiderEdges.Memory> MEMORY = new WeakHashMap<>();
    /** The game time from which the local hero's own (hidden) Web Shot timer allows another. */
    private static long shotReadyAt;
    /**
     * The tick the local hero's zip or swing was last seen to begin, to tell a hero in the middle of one from a new
     * one.
     */
    private static boolean wasZipping;
    private static boolean wasSwinging;

    // ---- the key press ----

    /**
     * The gesture the local Spider-Man's key press starts at once (see {@link SpiderGestures#predicted}), with what a
     * client can see for the guards: a creature or a block in his sight for the Web Shot, someone in range for the
     * burst, his look and whether his nanites are whole, the confirmation of Instant Kill waiting, and his stomach.
     */
    @Nullable
    static GestureId predicted(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || me.isSpectator() || !EnhancedConfig.SPEC.isLoaded()) {
            return null;
        }
        if (SpiderMotion.keysIgnored(me, data)) {
            // Held (a spell, a pin, telekinesis, a tomb): the server ignores every key, so no hand may thrust a web.
            return null;
        }
        boolean creative = me.getAbilities().invulnerable;
        int food = me.getFoodData().getFoodLevel();
        SpiderGestures.Sight sight = new SpiderGestures.Sight(now >= shotReadyAt, ability == 1 && !sneaking && shotInSight(me),
                ability == 1 && sneaking && burstInRange(me), SpiderServer.IRON_SPIDER.equals(Variants.current(data).id()),
                SpiderClient.scattered(), SpiderMotion.confirmPending(now), creative || food > 0, creative || food >= SpiderServer.EYES_MIN_FOOD);
        GestureId id = SpiderGestures.predicted(data, ability, now, sneaking, sight);
        if (id == GestureId.SPIDER_SHOOT) {
            shotReadyAt = now + SpiderConfig.WEB_SHOT_COOLDOWN.get();
        }
        return id;
    }

    /** Whether a web would find something to stick to: a creature it can hit, or a block, within the shot's range. */
    private static boolean shotInSight(LocalPlayer me) {
        return Raycasts.any(me, SpiderConfig.WEB_SHOT_RANGE.get(), SpiderWebs::webTarget, 1.0F).getType() != HitResult.Type.MISS;
    }

    /** Whether the burst would find someone: a creature it can web, within its range and in sight. */
    private static boolean burstInRange(LocalPlayer me) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        double range = SpiderConfig.WEB_BURST_RANGE.get();
        Vec3 chest = me.position().add(0, me.getBbHeight() * 0.7, 0);
        for (LivingEntity creature : level.getEntitiesOfClass(LivingEntity.class, me.getBoundingBox().inflate(range),
                e -> e != me && SpiderWebs.webTarget(e) && e.distanceTo(me) <= range)) {
            Vec3 to = creature.position().add(0, creature.getBbHeight() * 0.5, 0);
            if (level.clip(new ClipContext(chest, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, me)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    // ---- the edges ----

    /**
     * The gestures of his motion. The local hero's own movement knows at once what he is doing (the server's flags come
     * a tick or two later and outlast a cling by a few ticks); everyone else's is read from the synced data. A zip or a
     * swing that his own movement has begun starts its gesture on the spot, and the change in the data that arrives a
     * moment later is swallowed; his arm aims at the anchor his movement holds.
     */
    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        boolean local = p == Minecraft.getInstance().player;
        SpiderMotion.Mode mode = local ? SpiderMotion.mode() : SpiderMotion.Mode.NONE;
        boolean zipping = local ? mode == SpiderMotion.Mode.ZIP : data.is(Live.ZIPPING);
        boolean swinging = local ? mode == SpiderMotion.Mode.SWING : data.is(Live.SWINGING);
        boolean clinging = local ? mode == SpiderMotion.Mode.CLING : data.is(Live.CLINGING_WALL) || data.is(Live.CLINGING_CEILING);
        float age = p.tickCount;
        SpiderEdges.Memory m = MEMORY.computeIfAbsent(s, key -> new SpiderEdges.Memory());

        if (local) {
            if (zipping && !wasZipping) {
                startPredicted(s, p, GestureId.SPIDER_ZIP_START);
            }
            if (swinging && !wasSwinging) {
                startPredicted(s, p, GestureId.SPIDER_SWING_START);
            }
            wasZipping = zipping;
            wasSwinging = swinging;
        }

        switch (SpiderEdges.step(m, clinging, zipping, swinging, s.dy[0])) {
            case LEAP -> {
                HeroAnimator.start(s, GestureId.SPIDER_LEAP, 0.0F, age);
                GestureEffects.started(p, s, GestureId.SPIDER_LEAP, 0.0F);
            }
            case ARRIVE -> {
                HeroAnimator.start(s, GestureId.SPIDER_ARRIVE, 0.0F, age);
                GestureEffects.started(p, s, GestureId.SPIDER_ARRIVE, 0.0F);
            }
            default -> {
            }
        }

        // The landing: his feet touch the ground after a fall.
        if (!s.wasOnGround && s.onGround && s.prevAirTicks >= SpiderEdges.LAND_MIN_AIR) {
            float fall = -Math.min(s.dy[0], Math.min(s.dy[1], s.dy[2]));
            SpiderEdges.Landing landing = SpiderEdges.landing(s.prevAirTicks, fall, m.sinceWeb());
            // Every landing dips the body a little; a hero's is deeper.
            float soft = landing == SpiderEdges.Landing.HARD ? 1.4F : 0.4F;
            s.drop.v += soft * 0.084F * AnimMath.clamp((fall - 0.15F) / 0.45F, 0.0F, 1.5F);
            if (landing == SpiderEdges.Landing.HARD) {
                HeroAnimator.start(s, GestureId.SPIDER_LAND, 0.0F, age);
                SpiderGestureFx.landing(p, fall);
            } else if (landing == SpiderEdges.Landing.SOFT) {
                HeroAnimator.start(s, GestureId.LAND_SOFT, 0.0F, age);
                if (fall >= 0.25F) {
                    FlightEffects.softLanding(p, s, fall);
                }
            }
        }
    }

    /**
     * Starts a gesture because his own movement began it, and marks it as waiting for the change in the data that will
     * come a moment later, which must not play it twice ({@link GestureTable}).
     */
    private static void startPredicted(HeroAnimState s, AbstractClientPlayer p, GestureId id) {
        HeroAnimator.start(s, id, 0.0F, p.tickCount);
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.id == id && slot.start == p.tickCount) {
                slot.predicted = true;
            }
        }
        GestureEffects.started(p, s, id, 0.0F);
    }

    /** A hero is no longer remembered (the client left the world). */
    static void clear() {
        MEMORY.clear();
        shotReadyAt = 0L;
        wasZipping = false;
        wasSwinging = false;
    }

    // ---- first person ----

    /**
     * His hands at the edges of the screen (DESIGN section 14, "Animation", first person): the palm thrust out for a
     * Web Shot, both hands gathered in and then flung out for the burst, the web hand raised to the upper right and the
     * other low while he zips or swings, the hands that reach up the wall one after the other as he climbs, the slow
     * heartbeat of the Black Eyes, and a shrug as the legs unfold. Nothing crosses the middle of the screen.
     */
    static void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float age = (float) s.time;
        boolean calm = EnhancedClientConfig.reducedMotion();
        float w;
        if ((w = s.stance(Stance.SPIDER_ZIP) + s.stance(Stance.SPIDER_SWING)) > 0.001F) {
            w = Math.min(1.0F, w);
            m.add(w, 0.14F, 0.22F, 0.08F, -35, 0, 0);
            o.add(w, -0.06F, -0.10F, 0.0F, 0, 0, 0);
        }
        if ((w = s.stance(Stance.SPIDER_CLING_WALL)) > 0.001F) {
            // The reaching hands bob as he climbs; the off hand is drawn.
            float amp = s.crawlAmp;
            m.add(w, 0.0F, 0.10F * Mth.sin(s.crawlPhase) * amp, 0.05F, 0, 0, 0);
            o.add(w, 0.0F, 0.10F * Mth.sin(s.crawlPhase + Mth.PI) * amp, 0.05F, 0, 0, 0);
        }
        if ((w = s.stance(Stance.SPIDER_SURGE)) > 0.001F) {
            // A tremble, and a heartbeat at 70 a minute: a dip and a rebound (none with reduced motion).
            FirstPersonArms.tremble(m, o, w, 0.01F, 1.9F, age, calm);
            if (!calm) {
                float beat = Mth.cos(Mth.TWO_PI * age / SURGE_BEAT_TICKS);
                float dip = -0.02F * w * Math.max(0.0F, beat) * Math.max(0.0F, beat) * Math.max(0.0F, beat) * Math.max(0.0F, beat);
                m.ambient(1.0F, 0.0F, dip, 0.0F);
                o.ambient(1.0F, 0.0F, dip, 0.0F);
            }
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float gw = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case SPIDER_SHOOT ->
                        // The palm thrust out for four ticks.
                        m.add(gw * (1.0F - ss(3.5F, 5.5F, t)), 0.0F, 0.10F, 0.18F, -18, 0, 0);
                case SPIDER_BURST -> {
                    // Gathered in for six ticks, then flung out.
                    float in = ss(0.0F, 5.0F, t) * (1.0F - ss(8.0F, 11.0F, t));
                    float out = ss(9.0F, 12.0F, t);
                    FirstPersonArms.both(gw, -0.12F * in + 0.30F * out, 0.10F * out, 0.05F * in + 0.10F * out, 0, 0, 25.0F * out);
                }
                case SPIDER_LEGS_OUT -> {
                    // A shrug: the hands drop a moment.
                    float shrug = ss(0.0F, 3.0F, t) * (1.0F - ss(5.0F, 10.0F, t));
                    FirstPersonArms.both(gw, 0.0F, -0.05F * shrug, 0.0F, 0, 0, 0);
                }
                default -> {
                }
            }
        }
    }

    /** A heartbeat every this many ticks (70 a minute). */
    private static final float SURGE_BEAT_TICKS = 20.0F * 60.0F / 70.0F;

    /** A gesture of his just began: the flourish that goes with it. */
    static void started(AbstractClientPlayer player, GestureId id) {
        SpiderGestureFx.started(player, id);
    }

    private SpiderActing() {
    }
}
