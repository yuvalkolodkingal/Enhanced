package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Thor's lightning and the Bifrost, drawn in the world: the lightning beam from his hand (or the head of his weapon) to
 * whatever he looks at, arcs from creature to creature, the God of Thunder crackling with it, the glow of the fragments
 * of a shattered Mjolnir, the Bifrost's rainbow beam and the circle of runes it burns into the ground.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ThorEffects {
    public static final ResourceLocation BIFROST_MARK = Enhanced.id("textures/effect/bifrost_mark.png");
    /** MCU lightning: a white-hot core, electric blue glow and a deep blue halo. */
    private static final int CORE = 0xFFFFFF;
    private static final int BLUE = 0x4DB8FF;
    private static final int HALO = 0x1E6BFF;
    private static final int ARC_TICKS = 6;
    private static final int ARRIVAL_TICKS = 24;
    private static final int MARK_TICKS = 1200;

    private record Effect(int kind, int entityId, List<Vec3> points, ResourceKey<Level> dimension, long start) {
        int length() {
            return switch (kind) {
                case ThorFxPayload.ARC -> ARC_TICKS;
                case ThorFxPayload.BIFROST_ARRIVAL -> ARRIVAL_TICKS;
                default -> MARK_TICKS;
            };
        }
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();

    public static void add(ThorFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !payload.points().isEmpty()) {
            HeroAnimator.onThorFx(payload);
            if (payload.kind() == ThorFxPayload.PIN) {
                return;
            }
            EFFECTS.add(new Effect(payload.kind(), payload.entityId(), payload.points(), level.dimension(), level.getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            EFFECTS.clear();
            return;
        }
        long now = level.getGameTime();
        EFFECTS.removeIf(e -> e.dimension() != level.dimension() || now - e.start() > e.length() || now < e.start());
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            // The Bifrost takes anyone who wields Mjolnir or Stormbreaker, so its light is drawn for everyone.
            if (data.is(Live.BIFROST)) {
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    level.addParticle(ParticleTypes.END_ROD, player.getX() + Math.cos(angle) * 1.4, player.getY() + random.nextDouble() * 2.5,
                            player.getZ() + Math.sin(angle) * 1.4, 0, 0.25, 0);
                }
            }
            if (data.power() == Power.THOR && !player.isInvisible() && data.toggle(Toggle.GOD_OF_THUNDER) && random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, player.getRandomX(0.7), player.getRandomY(), player.getRandomZ(0.7),
                        (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.2);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // The rune circles first: flat on the ground, textured.
        VertexConsumer marks = buffers.getBuffer(EnhancedRenderTypes.energyField(BIFROST_MARK));
        for (Effect e : EFFECTS) {
            if (e.kind() == ThorFxPayload.BIFROST_MARK) {
                float age = (time - e.start()) / MARK_TICKS;
                // Burning bright at first, then slowly cooling and fading.
                float alpha = age < 0.02F ? age / 0.02F : 1.0F - Mth.clamp((age - 0.15F) / 0.85F, 0, 1);
                runeCircle(pose, marks, e.points().get(0).subtract(camera), 3.2F, alpha);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.energyField(BIFROST_MARK));

        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Effect e : EFFECTS) {
            float age = time - e.start();
            switch (e.kind()) {
                case ThorFxPayload.ARC -> {
                    float fade = 1.0F - age / ARC_TICKS;
                    long seed = e.start() * 104729L;
                    for (int i = 0; i + 1 < e.points().size(); i++) {
                        lightning(pose, vc, e.points().get(i).subtract(camera), e.points().get(i + 1).subtract(camera),
                                0.2F, fade, seed + i,
                                time / 2.0F);
                    }
                }
                case ThorFxPayload.BIFROST_ARRIVAL -> {
                    float fade = 1.0F - age / ARRIVAL_TICKS;
                    rainbowBeam(pose, vc, e.points().get(0).subtract(camera), 1.6F + 0.8F * fade, time, fade);
                }
                default -> {
                }
            }
        }
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.is(Live.BIFROST)) {
                Vec3 feet = player.getPosition(partialTick).subtract(camera);
                float pulse = 0.85F + 0.15F * Mth.sin(time * 0.9F);
                rainbowBeam(pose, vc, feet, 1.3F * pulse, time, 1.0F);
            }
            if (data.power() != Power.THOR) {
                continue;
            }
            if (data.toggle(Toggle.GOD_OF_THUNDER) && !player.isInvisible()) {
                crackle(pose, vc, player, camera, partialTick, time);
            }
            // The beam is drawn even for an invisible Thor: it hurts all the same.
            if (data.is(Live.CALLING_LIGHTNING)) {
                beam(mc, player, ThorAbilities.holdsWeapon(player), data.toggle(Toggle.GOD_OF_THUNDER), pose, vc, camera, partialTick, time);
            }
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof MjolnirFragmentEntity fragment) {
                fragmentGlow(fragment, pose, vc, camera, partialTick, time);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    // ---- the lightning beam ----

    /**
     * Thor's lightning beam, from his hand (or the head of the weapon in it) to what he looks at: a jagged bolt with a
     * white-hot core, an electric blue body and a deep blue halo, writhing from one shape to the next, with a few
     * strands crackling beside it and short forks branching off. Through Mjolnir or Stormbreaker it is thicker and
     * brighter, and the God of Thunder's is thicker still. It starts thin, so it never fills the screen in first person.
     */
    private static void beam(Minecraft mc, AbstractClientPlayer player, boolean weapon, boolean thunder, Matrix4f pose, VertexConsumer vc,
                             Vec3 camera, float partialTick, float time) {
        Vec3 origin = beamOrigin(mc, player, weapon, partialTick);
        double range = ThorAbilities.beamRange(ThorAbilities.heldWeapon(player));
        Vec3 target = Raycasts.any(player, range, ThorAbilities.beamTargets(player), partialTick).getLocation();
        // A Scarlet Witch's complete Chaos Shield in the way stops it at its surface (as on the server).
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(player.level(), player, player.getEyePosition(partialTick), target, partialTick);
        if (shield != null) {
            target = shield.point();
        }
        Vec3 a = origin.subtract(camera);
        Vec3 b = target.subtract(camera);
        double length = a.distanceTo(b);
        if (length < 0.05) {
            return;
        }
        float scale = (weapon ? 1.9F : 1.0F) * (thunder ? 1.2F : 1.0F);
        float flicker = 0.85F + 0.15F * Mth.sin(time * 2.9F) * Mth.cos(time * 1.3F);
        long seed = player.getId() * 104729L;
        // (3.3) The bolt flows from one shape to the next, and the strands and forks fade across, instead of jumping.
        float phase = time / BeamRenderer.SHAPE_TICKS;
        double jitter = Math.max(0.12, Math.min(1.2, length * 0.035)) * (weapon ? 1.3 : 1.0);

        // A soft glow all along the line, brighter through the weapon.
        EnergyGeometry.ribbon(pose, vc, a, b, 0.12F * scale, 0.16F * scale, EnergyGeometry.argb(HALO, 0.10F * flicker), EnergyGeometry.argb(HALO, 0.14F * flicker));
        List<Vec3> path = EnergyGeometry.flowingPath(a, b, jitter, seed, phase);
        taperedLine(pose, vc, path, 0.30F * scale, EnergyGeometry.argb(HALO, 0.30F * flicker));
        taperedLine(pose, vc, path, 0.15F * scale, EnergyGeometry.argb(BLUE, 0.75F * flicker));
        taperedLine(pose, vc, path, 0.055F * scale, EnergyGeometry.argb(CORE, 0.97F));
        Vec3 dir = b.subtract(a).scale(1.0 / length);
        EnergyGeometry.crossfade(seed + 31L, phase, (random, weight) -> {
            // Loose strands that crackle round the main bolt.
            int strands = weapon ? 3 : 2;
            for (int i = 0; i < strands; i++) {
                List<Vec3> strand = EnergyGeometry.lightningPath(a, b, jitter * 1.6, random);
                taperedLine(pose, vc, strand, 0.05F * scale, EnergyGeometry.argb(BLUE, 0.55F * weight));
            }
            // Forks.
            int forks = weapon ? 4 : 2;
            for (int i = 0; i < forks && path.size() > 3; i++) {
                Vec3 start = path.get(1 + random.nextInt(path.size() - 2));
                double reach = 0.5 + random.nextDouble() * Math.min(2.0, length * 0.15);
                Vec3 side = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5,
                        random.nextDouble() - 0.5).normalize();
                Vec3 end = start.add(dir.scale(reach * 0.6)).add(side.scale(reach * 0.8));
                List<Vec3> fork = EnergyGeometry.lightningPath(start, end, reach * 0.2, random);
                EnergyGeometry.polyline(pose, vc, fork, 0.06F * scale, EnergyGeometry.argb(BLUE, 0.6F * weight));
                EnergyGeometry.polyline(pose, vc, fork, 0.02F * scale, EnergyGeometry.argb(CORE, 0.8F * weight));
            }
        });
        // Flares where it leaves (smaller the closer it is to the camera) and where it lands.
        float near = (float) Mth.clamp(a.length() / 2.5, 0.12, 1.0);
        flare(pose, vc, a, 0.2F * scale * flicker * near, 0.9F);
        flare(pose, vc, b, 0.34F * scale * flicker, 1.0F);
    }

    /**
     * The path as ribbons of {@code width}, a quarter of it at the start growing to the full width three blocks along
     * (so a beam that starts right in front of the camera does not fill the screen).
     */
    private static void taperedLine(Matrix4f pose, VertexConsumer vc, List<Vec3> path, float width, int color) {
        double along = 0.0;
        for (int i = 1; i < path.size(); i++) {
            Vec3 p = path.get(i - 1);
            Vec3 q = path.get(i);
            double next = along + p.distanceTo(q);
            EnergyGeometry.ribbon(pose, vc, p, q, taper(width, along), taper(width, next), color, color);
            along = next;
        }
    }

    private static float taper(float width, double along) {
        return width * (float) Math.min(1.0, 0.25 + along / 4.0);
    }

    /**
     * A glow facing the camera: rings of blue light, thicker and brighter towards the middle (so it fades towards its
     * edge), a white-hot heart, and four thin spikes through it.
     */
    private static void flare(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, float alpha) {
        Vec3 toCamera = center.scale(-1);
        Vec3 side = toCamera.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(toCamera.normalize()).normalize();
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.8F, radius * 0.45F, 14, EnergyGeometry.argb(HALO, 0.16F * alpha));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.5F, radius * 0.4F, 14, EnergyGeometry.argb(BLUE, 0.3F * alpha));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.22F, radius * 0.3F, 12, EnergyGeometry.argb(CORE, 0.85F * alpha));
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 4.0;
            Vec3 spike = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).scale(radius * (i % 2 == 0 ? 1.5 : 1.0));
            EnergyGeometry.ribbon(pose, vc, center.subtract(spike), center.add(spike), radius * 0.07F, EnergyGeometry.argb(BLUE, 0.6F * alpha),
                    EnergyGeometry.argb(BLUE, 0.6F * alpha));
        }
    }

    /**
     * Where the beam leaves Thor: the head of the weapon in his hand, or his bare hand. From the shoulder of the arm
     * that points where he looks (the body leans in flight, so the point is carried to where the lean has put it); in
     * first person from where the weapon or the hand appears at the right of the screen.
     */
    private static Vec3 beamOrigin(Minecraft mc, AbstractClientPlayer player, boolean weapon, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            // Where the head of the held weapon, or the bare fist, is drawn: at the right of the screen (the left, for a
            // weapon in the off hand), moved with the first-person pose of the beam (the view-space offset is about 0.8
            // blocks from the camera, so 0.4 of it is the offset in the world). The numbers were measured on screenshots
            // of the pose, which the hand draws with a field of view of 70 degrees whatever the player's: they are
            // scaled to the player's own, so the beam leaves the weapon at any field of view.
            Vec3 right = look.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            InteractionHand weaponHand = weapon ? ThorAbilities.weaponHand(player) : null;
            HumanoidArm arm = weaponHand == InteractionHand.OFF_HAND ? player.getMainArm().getOpposite() : player.getMainArm();
            double side = arm == HumanoidArm.RIGHT ? 1.0 : -1.0;
            float[] hand = new float[3];
            if (player == mc.player) {
                FirstPersonArms.hand(mc.player, arm, hand);
            }
            boolean mjolnir = weaponHand != null && ThorAbilities.isMjolnir(player.getItemInHand(weaponHand));
            double across = mjolnir ? 0.35 : weapon ? 0.33 : 0.42;
            double high = mjolnir ? 0.08 : weapon ? 0.0 : -0.28;
            double fov = Math.tan(Math.toRadians(mc.options.fov().get() * 0.5)) / Math.tan(Math.toRadians(35.0));
            return player.getEyePosition(partialTick).add(look.scale(0.75 + 0.4 * hand[2]))
                    .add(right.scale(side * (across + 0.4 * hand[0]) * fov)).add(up.scale((high + 0.4 * hand[1]) * fov));
        }
        // Where the body is really drawn: the head of the weapon in his fist, or the fist itself.
        Vec3 tracked = weapon ? HandTracker.weaponHead(player, partialTick) : HandTracker.hand(player, player.getMainArm(), partialTick);
        if (tracked != null) {
            return tracked;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double side = player.getMainArm() == HumanoidArm.RIGHT ? -0.31 : 0.31;
        Vec3 shoulder = BodyLean.follow(player, new Vec3(eye.x + Math.cos(yaw) * side, eye.y - 0.25, eye.z + Math.sin(yaw) * side), partialTick);
        Vec3 hand = shoulder.add(look.scale(0.7));
        return weapon ? hand.add(look.scale(0.7)).add(0, 0.1, 0) : hand;
    }

    // ---- the fragments of a shattered Mjolnir ----

    /** A fragment glows blue-white, trails a streak of light along its flight and crackles. */
    private static void fragmentGlow(MjolnirFragmentEntity fragment, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        Vec3 centre = fragment.getPosition(partialTick).add(0, fragment.getBbHeight() * 0.5, 0);
        Vec3 c = centre.subtract(camera);
        Vec3 motion = fragment.getDeltaMovement();
        float flicker = 0.8F + 0.2F * Mth.sin(time * 1.7F + fragment.getId());
        // Smaller the closer it is to the camera: a fragment bursting out of his own hand would fill the screen.
        float near = (float) Mth.clamp(c.length() / 3.0, 0.15, 1.0);
        if (motion.lengthSqr() > 1.0E-4) {
            Vec3 tail = c.subtract(motion.normalize().scale(Math.min(2.4, motion.length() * 1.2)));
            EnergyGeometry.ribbon(pose, vc, tail, c, 0.0F, 0.20F * near, EnergyGeometry.argb(HALO, 0.0F), EnergyGeometry.argb(BLUE, 0.55F * flicker));
            EnergyGeometry.ribbon(pose, vc, tail, c, 0.0F, 0.07F * near, EnergyGeometry.argb(CORE, 0.0F), EnergyGeometry.argb(CORE, 0.85F));
        }
        flare(pose, vc, c, 0.34F * flicker * near, 0.9F);
        EnergyGeometry.crossfade(fragment.getId() * 31L, time * 2.0F, (random, weight) -> {
            for (int i = 0; i < 2; i++) {
                Vec3 end = c.add((random.nextDouble() - 0.5) * 0.9, (random.nextDouble() - 0.5) * 0.9,
                        (random.nextDouble() - 0.5) * 0.9);
                EnergyGeometry.bolt(pose, vc, c, end, 0.03F, 0.08F, EnergyGeometry.argb(BLUE, 0.8F * weight),
                        random.nextLong());
            }
        });
    }

    /**
     * A bolt of MCU lightning: a wide deep-blue halo, an electric blue bolt and a white-hot core, writhing from one
     * shape
     * to the next as {@code phase} grows (3.3: it jumped to a new shape every other tick).
     */
    private static void lightning(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, float fade, long seed,
            float phase) {
        if (fade <= 0) {
            return;
        }
        float jitter = (float) Math.min(1.6, a.distanceTo(b) * 0.06 + 0.2);
        EnergyGeometry.bolt(pose, vc, a, b, width * 1.6F, jitter, EnergyGeometry.argb(HALO, 0.3F * fade), seed, phase);
        EnergyGeometry.bolt(pose, vc, a, b, width * 0.7F, jitter, EnergyGeometry.argb(BLUE, 0.75F * fade), seed, phase);
        EnergyGeometry.bolt(pose, vc, a, b, width * 0.25F, jitter, EnergyGeometry.argb(CORE, 0.95F * fade), seed,
                phase);
        // A forked branch.
        Vec3 mid = a.lerp(b, 0.45);
        Vec3 fork = mid.add(new Vec3((seed % 7) - 3, -Math.abs(seed % 5), (seed % 3) - 1).normalize().scale(a.distanceTo(b) * 0.15));
        EnergyGeometry.bolt(pose, vc, mid, fork, width * 0.35F, jitter * 0.6F, EnergyGeometry.argb(BLUE, 0.5F * fade),
                seed + 5, phase);
    }

    /**
     * Short arcs of lightning crawling over the God of Thunder's body, a new set every other tick, each fading in and
     * out
     * (3.3: they popped).
     */
    private static void crackle(Matrix4f pose, VertexConsumer vc, AbstractClientPlayer player, Vec3 camera, float partialTick, float time) {
        Vec3 feet = player.getPosition(partialTick);
        EnergyGeometry.crossfade(player.getId() * 31L, time / 2.0F, (random, weight) -> {
            for (int i = 0; i < 4; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double y = 0.2 + random.nextDouble() * 1.6;
                // Over the body as it is drawn, leaning into a flight or not.
                Vec3 a = BodyLean.follow(player, feet.add(Math.cos(angle) * 0.38, y, Math.sin(angle) * 0.38),
                        partialTick)
                        .subtract(camera);
                Vec3 b = a.add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.7,
                        (random.nextDouble() - 0.5) * 0.6);
                EnergyGeometry.bolt(pose, vc, a, b, 0.03F, 0.12F, EnergyGeometry.argb(BLUE, 0.8F * weight),
                        random.nextLong());
                EnergyGeometry.bolt(pose, vc, a, b, 0.012F, 0.12F, EnergyGeometry.argb(CORE, 0.9F * weight),
                        random.nextLong());
            }
        });
    }

    /** The Bifrost: a column of rainbow light, its colours swirling round, a white core, from the sky to {@code feet}. */
    private static void rainbowBeam(Matrix4f pose, VertexConsumer vc, Vec3 feet, float radius, float time, float fade) {
        int sides = 24;
        float height = 220F;
        for (int k = 0; k < sides; k++) {
            float a0 = Mth.TWO_PI * k / sides + time * 0.05F;
            float a1 = Mth.TWO_PI * (k + 1) / sides + time * 0.05F;
            int color = hue((float) k / sides + time * 0.01F);
            Vec3 p0 = feet.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 p1 = feet.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            quad(pose, vc, p0, p1, height, EnergyGeometry.argb(color, 0.42F * fade), EnergyGeometry.argb(color, 0.0F));
            // An inner, brighter sheath.
            Vec3 q0 = feet.add(Math.cos(a0) * radius * 0.45, 0, Math.sin(a0) * radius * 0.45);
            Vec3 q1 = feet.add(Math.cos(a1) * radius * 0.45, 0, Math.sin(a1) * radius * 0.45);
            quad(pose, vc, q0, q1, height, EnergyGeometry.argb(0xFFFFFF, 0.16F * fade), EnergyGeometry.argb(color, 0.04F * fade));
        }
    }

    /** A vertical quad from the ground edge {@code a}-{@code b} up {@code height} blocks, fading upward. */
    private static void quad(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float height, int bottom, int top) {
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(bottom);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(bottom);
        vc.addVertex(pose, (float) b.x, (float) b.y + height, (float) b.z).setColor(top);
        vc.addVertex(pose, (float) a.x, (float) a.y + height, (float) a.z).setColor(top);
    }

    /** A bright, saturated colour around the colour wheel (0..1). */
    private static int hue(float h) {
        return Mth.hsvToRgb(((h % 1.0F) + 1.0F) % 1.0F, 0.75F, 1.0F);
    }

    /** The circle of runes: a textured square lying flat on the ground. */
    private static void runeCircle(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        int color = EnergyGeometry.argb(0xFFD9A0, alpha);
        float x = (float) center.x;
        float y = (float) center.y + 0.02F;
        float z = (float) center.z;
        vc.addVertex(pose, x - radius, y, z - radius).setUv(0, 0).setColor(color);
        vc.addVertex(pose, x + radius, y, z - radius).setUv(1, 0).setColor(color);
        vc.addVertex(pose, x + radius, y, z + radius).setUv(1, 1).setColor(color);
        vc.addVertex(pose, x - radius, y, z + radius).setUv(0, 1).setColor(color);
    }

    public static void clear() {
        EFFECTS.clear();
    }

    private ThorEffects() {
    }
}
