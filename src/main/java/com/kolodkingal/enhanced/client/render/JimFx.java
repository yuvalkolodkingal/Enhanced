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
        float[] seen = NOVA.computeIfAbsent(player.getId(), k -> new float[]{-1, -1});
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
            ChargedBeamFx.gatherAround(pose, vc, origin.subtract(camera), look, ChargedBeamFx.FIRE, t, time, 0.9F);
        }
        if (firing) {
            if (seen[1] < 0) {
                seen[1] = time;
            }
            float duration = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_DURATION_TICKS.get() : 36;
            float age = time - seen[1];
            float strength = Mth.clamp(age / 3.0F, 0.0F, 1.0F) * Mth.clamp((duration - age) / 6.0F, 0.0F, 1.0F);
            if (strength <= 0.0F) {
                return;
            }
            double range = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_RANGE.get() : 96;
            Vec3 eye = player.getEyePosition(partialTick);
            Vec3 end = com.kolodkingal.enhanced.ability.Raycasts.block(player, range, partialTick).getLocation();
            if (end.subtract(origin).length() < 5.0) {
                end = origin.add(look.scale(5.0));
            }
            Vec3 m = origin.subtract(camera);
            Vec3 e = end.subtract(camera);
            long seed = player.getId() * 7349L;
            // the one great beam from his jewel: much wider, hotter, with the flames licking along it
            float width = firstPerson(mc, player) ? 0.9F : 2.8F; // (in first person the view stays clear: the beam is drawn slimmer)
            ChargedBeamFx.beam(pose, vc, m, e, ChargedBeamFx.FIRE, width * strength, strength, time, seed + 99L);
            ChargedBeamFx.flareAt(pose, vc, m, 0.9F * strength + 0.1F, ChargedBeamFx.FIRE);
            tongues(pose, vc, m, e, strength, time, seed);
            ChargedBeamFx.flareAt(pose, vc, e, (2.6F + 0.5F * Mth.sin(time * 1.7F)) * strength + 0.1F, ChargedBeamFx.FIRE);
        }
    }

    private static void return_gather(float[] seen, boolean gathering) {
        // (nothing to forget: the gathering clock is reset above)
    }

    /** Flame tongues licking off the great beam and embers streaming along it, and the flames at its end. */
    private static void tongues(Matrix4f pose, VertexConsumer vc, Vec3 m, Vec3 e, float strength, float time, long seed) {
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
            Vec3 base = m.add(dir.scale(along)).add(side.scale(Mth.cos(angle) * radius * 1.2F)).add(up.scale(Mth.sin(angle) * radius * 1.2F));
            // a tongue: a streak that leans back along the beam and rises, fading
            Vec3 tip = base.add(dir.scale(-0.9 - 0.6 * radius)).add(side.scale(Mth.cos(angle) * 0.5F)).add(up.scale(Mth.sin(angle) * 0.5F));
            float a = Mth.sin(phase * Mth.PI) * strength;
            EnergyGeometry.ribbon(pose, vc, base, tip, 0.16F, 0.0F, EnergyGeometry.argb(0xFFC040, 0.55F * a), EnergyGeometry.argb(0xFF4010, 0.0F));
        }
        // embers streaming along it
        for (int i = 0; i < 24; i++) {
            float phase = ((time * 0.08F + i * 0.381F) % 1.0F);
            Vec3 p = m.add(dir.scale(phase * length)).add(side.scale(Mth.sin(i * 3.1F + time * 0.3F) * 1.6F))
                    .add(up.scale(Mth.cos(i * 2.3F + time * 0.27F) * 1.6F));
            EnergyGeometry.ribbon(pose, vc, p, p.add(dir.scale(-0.25)), 0.04F, 0.0F, EnergyGeometry.argb(0xFFD060, 0.9F * strength),
                    EnergyGeometry.argb(0xFF5010, 0.0F));
        }
        // at the end: flames rising off the strike
        for (int i = 0; i < 10; i++) {
            float angle = i * 0.628F + time * 0.1F;
            Vec3 base = e.add(side.scale(Mth.cos(angle) * 1.0F)).add(up.scale(Mth.sin(angle) * 0.4F));
            Vec3 tip = base.add(0, 1.4F + 0.8F * Mth.sin(time * 0.5F + i), 0);
            EnergyGeometry.ribbon(pose, vc, base, tip, 0.3F, 0.0F, EnergyGeometry.argb(0xFFB030, 0.5F * strength), EnergyGeometry.argb(0xFF4010, 0.0F));
        }
    }
}
