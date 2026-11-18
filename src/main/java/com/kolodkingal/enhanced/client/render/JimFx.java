package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Jim Hammond's fire that is not the body's own (3.17): the flames and sparks that stream from every part of his burning body
 * opposite to the way he moves ({@link #tick}: from the points of the body the model was drawn at, {@link HandTracker}: head,
 * shoulders, hands, feet; so they follow the tilt of flight and every pose), and the Nova Flame's light: the energy gathering
 * round his chest, then the shell of fire racing out from him ({@link #nova}, drawn with the other beams).
 */
public final class JimFx {
    private static final Map<Integer, float[]> NOVA = new HashMap<>();

    private JimFx() {
    }

    public static void clear() {
        NOVA.clear();
        TRAIL.clear();
        EMBERS.clear();
        BALLS.clear();
        PENDING.clear();
        FLARE.clear();
        THROWING.clear();
        JimFlameLayer.clear();
    }

    /** Nothing within this many blocks of the camera is spawned or drawn: no flame may ever sit in front of the eye. */
    private static final double CAMERA_CULL = 7.0;
    /** The positions of each burning body's centre over the last ticks (oldest first), for the trail. */
    private static final Map<Integer, java.util.ArrayDeque<Vec3>> TRAIL = new HashMap<>();
    private static final int TRAIL_TICKS = 16;

    private static float density() {
        return switch (Minecraft.getInstance().options.particles().get()) {
            case ALL -> 1.0F;
            case DECREASED -> 0.5F;
            case MINIMAL -> 0.15F;
        };
    }

    private static boolean firstPerson(Minecraft mc, AbstractClientPlayer player) {
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /**
     * Every client tick, for every burning body in view: the trail's history, and the embers and flames that stream from the body
     * opposite to its motion (more at speed, almost none when he hovers; fewer at a distance and with the game's particle option;
     * at most {@code BUDGET} a body). Nothing near the camera, and nothing at all for the camera's own body in first person.
     */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        float density = density();
        fireballs(mc, level);
        EMBERS.keySet().removeIf(id -> !TRAIL.containsKey(id));
        TRAIL.keySet().removeIf(id -> !(level.getEntity(id) instanceof AbstractClientPlayer p) || !JimFlameLayer.burning(Powers.get(p), p));
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (!JimFlameLayer.burning(data, player) || player.isInvisible()) {
                continue;
            }
            Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0);
            java.util.ArrayDeque<Vec3> trail = TRAIL.computeIfAbsent(player.getId(), k -> new java.util.ArrayDeque<>());
            trail.addFirst(centre);
            while (trail.size() > TRAIL_TICKS) {
                trail.removeLast();
            }
            double distance = centre.distanceTo(camera);
            if (distance > 48.0 || firstPerson(mc, player)) {
                continue;
            }
            Vec3 velocity = player.getDeltaMovement();
            double speed = velocity.length();
            float scale = density * (distance > 24.0 ? 0.5F : 1.0F);
            // the flames of the tongues' roots: the head, shoulders, hands and feet (where the body is really drawn)
            Vec3[] roots = {
                    HandTracker.forehead(player, 1.0F), HandTracker.shoulder(player, HumanoidArm.RIGHT, 1.0F),
                    HandTracker.shoulder(player, HumanoidArm.LEFT, 1.0F), HandTracker.hand(player, HumanoidArm.RIGHT, 1.0F),
                    HandTracker.hand(player, HumanoidArm.LEFT, 1.0F), HandTracker.foot(player, HumanoidArm.RIGHT, 1.0F),
                    HandTracker.foot(player, HumanoidArm.LEFT, 1.0F)};
            java.util.List<Ember> embers = EMBERS.computeIfAbsent(player.getId(), k -> new java.util.ArrayList<>());
            embers.removeIf(e -> ++e.age > e.life);
            for (Ember e : embers) {
                e.pos = e.pos.add(e.vel);
                e.vel = e.vel.scale(0.95).add(0, 0.006, 0);
            }
            int budget = Math.round(BUDGET * scale);
            Vec3 back = velocity.scale(-0.3);
            int count = Math.round((0.6F + (float) Math.min(4.0, speed * 12.0)) * scale);
            for (int n = 0; n < count && budget > 0 && embers.size() < 70; n++, budget--) {
                Vec3 root = roots[level.random.nextInt(roots.length)];
                if (root == null) {
                    root = centre.add(rnd(level, 0.3), rnd(level, 0.8), rnd(level, 0.3));
                }
                // somewhere along the way the body just came, so the stream is continuous at speed
                Vec3 at = root.subtract(velocity.scale(level.random.nextDouble()));
                if (at.distanceToSqr(camera) < CAMERA_CULL * CAMERA_CULL) {
                    continue;
                }
                embers.add(new Ember(at, new Vec3(back.x + rnd(level, 0.02), back.y + 0.03 + level.random.nextDouble() * 0.03,
                        back.z + rnd(level, 0.02)), 14 + level.random.nextInt(14)));
            }
        }
    }

    /** One of the soft embers the mod draws itself (small streaks of light, no game particle). */
    private static final class Ember {
        Vec3 pos;
        Vec3 vel;
        int age;
        final int life;

        Ember(Vec3 pos, Vec3 vel, int life) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
        }
    }

    private static final Map<Integer, java.util.List<Ember>> EMBERS = new HashMap<>();

    /** The most particles one burning body lays in a tick. */
    private static final int BUDGET = 8;

    private static double rnd(ClientLevel level, double spread) {
        return (level.random.nextDouble() - 0.5) * 2.0 * spread;
    }

    /**
     * The stream of flame behind a moving body: a ribbon along the path it took (the last ticks), widest at the body and fading
     * to nothing, with a thinner one beside it; nothing when he hovers. Not drawn for the camera's own body in first person.
     */
    static void trail(Minecraft mc, AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        java.util.ArrayDeque<Vec3> trail = TRAIL.get(player.getId());
        if (trail == null || firstPerson(mc, player)) {
            return;
        }
        for (Ember e : EMBERS.getOrDefault(player.getId(), java.util.List.of())) {
            float a = 1.0F - e.age / (float) e.life;
            Vec3 p = e.pos.add(e.vel.scale(partialTick)).subtract(camera);
            if (p.lengthSqr() < 9.0) {
                continue;
            }
            Vec3 dir = e.vel.lengthSqr() < 1.0E-6 ? new Vec3(0, 0.1, 0) : e.vel.normalize().scale(0.16);
            EnergyGeometry.ribbon(pose, vc, p, p.add(dir), 0.035F, 0.0F, EnergyGeometry.argb(0xFFC040, 0.85F * a),
                    EnergyGeometry.argb(0xFF5010, 0.0F));
        }
        if (trail.size() < 3) {
            return;
        }
        Vec3 now = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0);
        java.util.List<Vec3> points = new java.util.ArrayList<>(trail.size() + 1);
        points.add(now);
        points.addAll(trail);
        double length = 0;
        for (int i = 1; i < points.size(); i++) {
            length += points.get(i - 1).distanceTo(points.get(i));
        }
        if (length < 1.2) {
            return;
        }
        float strength = (float) Mth.clamp((length - 1.2) / 4.0, 0.0, 1.0);
        for (int lane = 0; lane < 3; lane++) {
            float spread = lane == 0 ? 0.0F : (lane == 1 ? 0.28F : -0.28F);
            for (int i = 1; i < points.size(); i++) {
                float a = 1.0F - (i - 1) / (float) points.size();
                float b = 1.0F - i / (float) points.size();
                Vec3 p = points.get(i - 1).add(0, spread * 0.6, 0).subtract(camera);
                Vec3 q = points.get(i).add(0, spread * 0.6, 0).subtract(camera);
                if (p.lengthSqr() < CAMERA_CULL * CAMERA_CULL || q.lengthSqr() < CAMERA_CULL * CAMERA_CULL) {
                    continue;
                }
                float wide = lane == 0 ? 0.55F : 0.28F;
                EnergyGeometry.ribbon(pose, vc, p, q, wide * a, wide * b, EnergyGeometry.argb(0xFF5A10, 0.42F * a * strength),
                        EnergyGeometry.argb(0xFF7A18, 0.42F * b * strength));
            }
        }
    }


    // ---- the fireballs ----

    /** One fireball in flight, as every client sees it (the server's ball is only a point: this is its picture, flown by the same rule). */
    private static final class Ball {
        Vec3 prev;
        Vec3 pos;
        final Vec3 velocity;
        double flown;
        final java.util.ArrayDeque<Vec3> tail = new java.util.ArrayDeque<>();

        Ball(Vec3 pos, Vec3 velocity) {
            this.prev = pos;
            this.pos = pos;
            this.velocity = velocity;
        }
    }

    private static final Map<Integer, java.util.List<Ball>> BALLS = new HashMap<>();
    /** The game time at which each player's pending ball leaves his hand, and until when the flare at his hand shows. */
    private static final Map<Integer, Long> PENDING = new HashMap<>();
    private static final Map<Integer, Long> FLARE = new HashMap<>();
    private static final java.util.Set<Integer> THROWING = new java.util.HashSet<>();
    /** The ticks from the begin of a throw to the release (the server's {@code JimFireballs.THROW_DELAY}, less the sync). */
    private static final int RELEASE_AFTER = 3;

    /** Every client tick: a throw that begins (the state turns on) is released a few ticks later; the balls fly. */
    private static void fireballs(Minecraft mc, ClientLevel level) {
        long now = level.getGameTime();
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            boolean throwing = data.power() == com.kolodkingal.enhanced.power.Power.JIM_HAMMOND && data.is(Live.JIM_THROW);
            if (throwing && THROWING.add(player.getId())) {
                PENDING.put(player.getId(), now + RELEASE_AFTER);
            } else if (!throwing) {
                THROWING.remove(player.getId());
            }
            Long due = PENDING.get(player.getId());
            if (due != null && now >= due) {
                PENDING.remove(player.getId());
                Vec3 look = player.getLookAngle();
                double speed = EnhancedConfig.SPEC.isLoaded() ? JimConfig.FIREBALL_SPEED.get() : 1.5;
                BALLS.computeIfAbsent(player.getId(), k -> new java.util.ArrayList<>())
                        .add(new Ball(player.getEyePosition().add(look.scale(0.7)).add(0, -0.25, 0), look.scale(speed)));
                FLARE.put(player.getId(), now + 6);
            }
        }
        double range = EnhancedConfig.SPEC.isLoaded() ? JimConfig.FIREBALL_RANGE.get() : 48;
        BALLS.entrySet().removeIf(entry -> {
            if (!(level.getEntity(entry.getKey()) instanceof AbstractClientPlayer owner)) {
                return true;
            }
            entry.getValue().removeIf(ball -> !step(level, owner, ball, range));
            return entry.getValue().isEmpty();
        });
        FLARE.values().removeIf(until -> until < now);
    }

    /** Flies one ball a tick (in two halves); false when it struck a block or a creature or went out. */
    private static boolean step(ClientLevel level, AbstractClientPlayer owner, Ball ball, double range) {
        ball.prev = ball.pos;
        ball.tail.addFirst(ball.pos);
        while (ball.tail.size() > 8) {
            ball.tail.removeLast();
        }
        for (int half = 0; half < 2; half++) {
            Vec3 to = ball.pos.add(ball.velocity.scale(0.5));
            net.minecraft.world.phys.BlockHitResult block = level.clip(new ClipContext(ball.pos, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, owner));
            if (block.getType() != HitResult.Type.MISS) {
                ball.pos = block.getLocation();
                return false;
            }
            ball.pos = to;
            ball.flown += ball.velocity.length() * 0.5;
            if (ball.flown > range || !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(to, to).inflate(0.35), e -> e != owner && e.isAlive() && !e.isSpectator()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The fireballs of one player and the flare at his hand at the release: each ball is a glowing core that turns slowly (a cross of
     * light about the line of flight and a soft halo) with flame tongues streaming back from it and a ribbon of its path: the
     * mod's own fire, drawn smoothly, never a game particle. Faded out near the camera.
     */
    static void balls(Minecraft mc, AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        if (!firstPerson(mc, player)) {
            Long until = FLARE.get(player.getId());
            Vec3 hand = until == null ? null : HandTracker.hand(player, player.getMainArm(), partialTick);
            if (hand != null) {
                float a = Mth.clamp((until - (time)) / 6.0F, 0.0F, 1.0F) * 0.9F + 0.1F;
                Vec3 at = hand.subtract(camera);
                ChargedBeamFx.flareAt(pose, vc, at, 0.32F * a, ChargedBeamFx.FIRE);
                for (int i = 0; i < 5; i++) {
                    float angle = i * 1.257F + time * 0.4F;
                    Vec3 tip = at.add(Mth.cos(angle) * 0.28, 0.12 + 0.2 * Mth.sin(angle * 1.3F + i), Mth.sin(angle) * 0.28);
                    EnergyGeometry.ribbon(pose, vc, at, tip, 0.09F, 0.0F, EnergyGeometry.argb(0xFFD060, 0.7F * a), EnergyGeometry.argb(0xFF4010, 0.0F));
                }
            }
        }
        for (Ball ball : BALLS.getOrDefault(player.getId(), java.util.List.of())) {
            Vec3 p = ball.prev.lerp(ball.pos, partialTick).subtract(camera);
            double dist = p.length();
            float fade = (float) Mth.clamp((dist - 1.2) / 2.5, 0.0, 1.0);
            if (fade <= 0.0F) {
                continue;
            }
            Vec3 dir = ball.velocity.normalize();
            Vec3 side = dir.cross(new Vec3(0, 1, 0));
            side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
            Vec3 up = side.cross(dir).normalize();
            float spin = time * 0.11F + player.getId();
            // the core: a hot white-yellow heart in a soft orange halo
            ChargedBeamFx.flareAt(pose, vc, p, 0.30F * fade, ChargedBeamFx.FIRE);
            ChargedBeamFx.flareAt(pose, vc, p, 0.52F * fade, new ChargedBeamFx.Colors(0xFF5A10, 0xFFB030, 0xFF7A18));
            // turning: a cross of light that wheels slowly about the line of flight
            for (int k = 0; k < 3; k++) {
                float angle = spin + k * 2.094F;
                Vec3 radial = side.scale(Mth.cos(angle)).add(up.scale(Mth.sin(angle)));
                EnergyGeometry.ribbon(pose, vc, p.subtract(radial.scale(0.34)), p.add(radial.scale(0.34)), 0.05F, 0.05F,
                        EnergyGeometry.argb(0xFFE8A0, 0.5F * fade), EnergyGeometry.argb(0xFFC040, 0.5F * fade));
            }
            // the flame tongues streaming back, wavering a little
            for (int k = 0; k < 6; k++) {
                float angle = spin * 0.6F + k * 1.047F;
                float wave = 0.5F + 0.5F * Mth.sin(time * 0.3F + k * 1.9F);
                Vec3 radial = side.scale(Mth.cos(angle)).add(up.scale(Mth.sin(angle)));
                Vec3 base = p.add(radial.scale(0.16));
                Vec3 tip = base.subtract(dir.scale(1.5 + 0.7 * wave)).add(radial.scale(0.12 + 0.12 * wave));
                EnergyGeometry.ribbon(pose, vc, base, tip, 0.2F, 0.0F, EnergyGeometry.argb(0xFFB030, 0.5F * fade), EnergyGeometry.argb(0xFF3A08, 0.0F));
            }
            // the path it just flew: a fading ribbon
            Vec3 last = p;
            int n = 0;
            for (Vec3 old : ball.tail) {
                Vec3 q = old.subtract(camera);
                float a = 1.0F - n / 8.0F;
                float b = 1.0F - (n + 1) / 8.0F;
                EnergyGeometry.ribbon(pose, vc, last, q, 0.2F * a, 0.2F * b, EnergyGeometry.argb(0xFF8A20, 0.4F * a * fade),
                        EnergyGeometry.argb(0xFF6A14, 0.4F * b * fade));
                last = q;
                n++;
            }
        }
    }

    // ---- the Nova Flame ----

    /** Where the Nova's beam starts: the android's gold diamond on his chest, the classic suit's jewel on his forehead (both followed in every pose), the chest centre for the human disguise. */
    private static Vec3 novaOrigin(Minecraft mc, AbstractClientPlayer player, PowerData data, float partialTick, Vec3 look) {
        if (firstPerson(mc, player)) {
            return player.getEyePosition(partialTick).add(look.scale(0.9)).add(0, -0.55, 0);
        }
        // the classic suit's jewel is on the forehead (tracked on the head part: it follows turns and tilts); the others' on the chest
        Vec3 chest = com.kolodkingal.enhanced.ability.JimAbilities.novaStartsAtHead(data) ? HandTracker.jewel(player, partialTick)
                : HandTracker.chest(player, partialTick);
        if (chest != null) {
            return chest;
        }
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        return player.position().add(0, player.getBbHeight() * 0.7, 0).add(flat.scale(0.25));
    }

    /** The Nova Flame's light for this player this frame: the build-up at his jewel, then the great beam from it. */
    static void nova(Minecraft mc, AbstractClientPlayer player, PowerData data, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                     float partialTick, float time) {
        float[] seen = NOVA.computeIfAbsent(player.getId(), k -> new float[]{-1, -1, 0.5F});
        boolean gathering = data.is(Live.JIM_NOVA_CHARGING);
        boolean firing = data.is(Live.JIM_NOVA_BLAST);
        if (!gathering) {
            seen[0] = -1;
        }
        if (!firing) {
            seen[1] = -1;
            return_gather(seen, gathering);
        }
        Vec3 look = player.getViewVector(partialTick);
        if (!gathering && !firing) {
            return;
        }
        Vec3 origin = novaOrigin(mc, player, data, partialTick, look);
        if (gathering) {
            if (seen[0] < 0) {
                seen[0] = time;
            }
            float full = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_CHARGE_TICKS.get() : 60;
            float t = Mth.clamp((time - seen[0]) / full, 0.0F, 1.0F);
            seen[2] = t; // (how charged it was when it is let go: the beam that follows is that wide and that far)
            ChargedBeamFx.gatherAround(pose, vc, origin.subtract(camera), look, ChargedBeamFx.FIRE, t, time, 0.9F);
        }
        if (firing) {
            if (seen[1] < 0) {
                seen[1] = time;
            }
            float duration = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_DURATION_TICKS.get() : 14;
            float age = time - seen[1];
            float strength = Mth.clamp(age / 1.5F, 0.0F, 1.0F) * Mth.clamp((duration - age) / 5.0F, 0.0F, 1.0F);
            if (strength <= 0.0F) {
                return;
            }
            float charge = seen[2];
            double far = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_RANGE.get() : 96;
            double near = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_MIN_RANGE.get() : 24;
            double range = Mth.lerp(charge, (float) near, (float) Math.max(near, far));
            Vec3 end = com.kolodkingal.enhanced.ability.Raycasts.block(player, range, partialTick).getLocation();
            if (end.subtract(origin).length() < 5.0) {
                end = origin.add(look.scale(5.0));
            }
            Vec3 m = origin.subtract(camera);
            Vec3 e = end.subtract(camera);
            long seed = player.getId() * 7349L;
            // the one great beam from his jewel: wider and hotter the more it was charged, with the flames licking along it
            float width = Mth.lerp(charge, 0.55F, firstPerson(mc, player) ? 1.0F : 2.8F); // (in first person the view stays clear: slimmer)
            ChargedBeamFx.beam(pose, vc, m, e, ChargedBeamFx.FIRE, width * strength, strength, time, seed + 99L);
            ChargedBeamFx.flareAt(pose, vc, m, (0.3F + 0.6F * charge) * strength + 0.1F, ChargedBeamFx.FIRE);
            tongues(pose, vc, m, e, strength * (0.4F + 0.6F * charge), time, seed, 0.35F + 0.65F * charge);
            ChargedBeamFx.flareAt(pose, vc, e, (0.8F + 1.9F * charge + 0.5F * Mth.sin(time * 1.7F)) * strength + 0.1F, ChargedBeamFx.FIRE);
        }
    }

    private static void return_gather(float[] seen, boolean gathering) {
        // (nothing to forget: the gathering clock is reset above)
    }

    /** Flame tongues licking off the great beam and embers streaming along it, and the flames at its end. */
    private static void tongues(Matrix4f pose, VertexConsumer vc, Vec3 m, Vec3 e, float strength, float time, long seed, float scale) {
        Vec3 axis = e.subtract(m);
        double length = axis.length();
        if (length < 0.5) {
            return;
        }
        Vec3 dir = axis.scale(1.0 / length);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(dir).normalize();
        int count = (int) Math.min(40, 10 + length * 0.5);
        for (int i = 0; i < count; i++) {
            float phase = ((time * 0.045F + i * 0.618F + (seed % 7) * 0.1F) % 1.0F);
            double along = Math.min(length, phase * length * 1.0);
            float angle = i * 2.399F + time * 0.05F;
            float radius = 0.5F + 0.5F * Mth.sin(i * 1.7F + time * 0.2F);
            Vec3 base = m.add(dir.scale(along)).add(side.scale(Mth.cos(angle) * radius * 1.2F * scale)).add(up.scale(Mth.sin(angle) * radius * 1.2F * scale));
            // a tongue: a streak that leans back along the beam and rises, fading
            Vec3 tip = base.add(dir.scale(-0.9 - 0.6 * radius)).add(side.scale(Mth.cos(angle) * 0.5F)).add(up.scale(Mth.sin(angle) * 0.5F));
            float a = Mth.sin(phase * Mth.PI) * strength;
            EnergyGeometry.ribbon(pose, vc, base, tip, 0.16F * scale, 0.0F, EnergyGeometry.argb(0xFFC040, 0.55F * a), EnergyGeometry.argb(0xFF4010, 0.0F));
        }
        // embers streaming along it
        for (int i = 0; i < 24; i++) {
            float phase = ((time * 0.08F + i * 0.381F) % 1.0F);
            Vec3 p = m.add(dir.scale(phase * length)).add(side.scale(Mth.sin(i * 3.1F + time * 0.3F) * 1.6F * scale))
                    .add(up.scale(Mth.cos(i * 2.3F + time * 0.27F) * 1.6F * scale));
            EnergyGeometry.ribbon(pose, vc, p, p.add(dir.scale(-0.25)), 0.04F, 0.0F, EnergyGeometry.argb(0xFFD060, 0.9F * strength),
                    EnergyGeometry.argb(0xFF5010, 0.0F));
        }
        // at the end: flames rising off the strike
        for (int i = 0; i < 10; i++) {
            float angle = i * 0.628F + time * 0.1F;
            Vec3 base = e.add(side.scale(Mth.cos(angle) * 1.0F * scale)).add(up.scale(Mth.sin(angle) * 0.4F * scale));
            Vec3 tip = base.add(0, (1.4F + 0.8F * Mth.sin(time * 0.5F + i)) * scale, 0);
            EnergyGeometry.ribbon(pose, vc, base, tip, 0.3F * scale, 0.0F, EnergyGeometry.argb(0xFFB030, 0.5F * strength), EnergyGeometry.argb(0xFF4010, 0.0F));
        }
    }
}
