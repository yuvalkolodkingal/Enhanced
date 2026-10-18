package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.ThanosAtmosphere;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.network.ThanosFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * Everything the Mad Titan and the playable Thanos show that is not a body (DESIGN section 15.14.2): the tells a fighter
 * reads to dodge (the rings on the ground that close in on the impact, the lines that whiten as the aim locks, the
 * sigils that mark a player), the beams, the portals, the falling stars of the stones arriving, the domes, the chains,
 * the pillars of the Soul World and the Time Stone's mandalas. The server sends one {@link ThanosFxPayload} for each,
 * naming a kind and its place and length; this class keeps at most 48 of them, draws them for a few ticks and forgets
 * them.
 *
 * <p>What lasts as long as a flag of the boss does (the Barrier and the Veil) is drawn from the flag, so a player who
 * comes late sees it too. Nothing here changes the world; every shape is made of the shapes in {@link FxShapes}.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ThanosFx {
    public static final ResourceLocation PORTAL_RING = Enhanced.id("textures/effect/portal_ring.png");
    public static final ResourceLocation MANDALA = Enhanced.id("textures/effect/stone_mandala.png");
    private static final ResourceLocation LATTICE = Enhanced.id("textures/entity/hex_wall.png");

    /** Lines that are drawn through walls (the Power Pulse finds a fighter in a bunker): no depth test. */
    private static final RenderType THROUGH_WALLS = RenderType.create("enhanced_thanos_through_walls",
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 1024, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(net.minecraft.client.renderer.RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE)
                    .setCullState(net.minecraft.client.renderer.RenderStateShard.NO_CULL)
                    .setDepthTestState(net.minecraft.client.renderer.RenderStateShard.NO_DEPTH_TEST)
                    .createCompositeState(false));

    /** The most effects kept at once, and the most of the kinds that are heavy. */
    private static final int MAX_EFFECTS = 48;
    private static final int MAX_RINGS = 12;
    private static final int MAX_CHAINS = 6;
    private static final int MAX_DOMES = 4;
    /** Nothing is drawn farther from the camera than this. */
    private static final double MAX_DISTANCE = 96.0;
    private static final int SEGMENTS = 64;
    private static final int GOLD = 0xD9C48A;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private record Effect(int kind, int color, int entityId, int ticks, float radius, List<Vec3> points, ResourceKey<Level> dimension, long start) {
        /** How long it is drawn: its own length and the moment of impact that follows it. */
        int life() {
            return switch (kind) {
                case ThanosFxPayload.RING_TELL -> ticks + 6;
                case ThanosFxPayload.LINE_TELL -> ticks + 1;
                case ThanosFxPayload.PORTAL, ThanosFxPayload.PORTAL_SKY -> ticks + 8;
                case ThanosFxPayload.STAR -> ticks + 8;
                case ThanosFxPayload.SHOCK_RING -> ticks + 6;
                case ThanosFxPayload.MANDALA -> ticks + 4;
                case ThanosFxPayload.WAVE -> ticks + 16;
                case ThanosFxPayload.PILLAR -> ticks + 8;
                default -> ticks;
            };
        }
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();
    /** How hard the camera is shaking (degrees), from a landing or a blow near by. */
    private static float shake;

    /** The payload of an effect arrived: remember it (the handler of {@link ThanosFxPayload}). */
    public static void add(ThanosFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        switch (payload.kind()) {
            case ThanosFxPayload.SOULWORLD -> {
                if (mc.player != null && payload.entityId() == mc.player.getId()) {
                    ThanosAtmosphere.soulWorld(level.getGameTime(), payload.ticks());
                }
                return;
            }
            case ThanosFxPayload.FLASH -> {
                ThanosAtmosphere.flash(level.getGameTime(), payload.ticks());
                return;
            }
            default -> {
            }
        }
        if (payload.points().isEmpty() && payload.kind() != ThanosFxPayload.CHAINS) {
            return;
        }
        if (payload.kind() == ThanosFxPayload.DOME && bossMadeDome(level, payload)) {
            return;
        }
        if (payload.kind() == ThanosFxPayload.PORTAL_SKY) {
            ThanosAtmosphere.portalOpen(level.getGameTime(), payload.ticks());
        }
        if (payload.kind() == ThanosFxPayload.SHOCK_RING && payload.radius() >= 3.0F && mc.player != null && !payload.points().isEmpty()) {
            double near = mc.player.position().distanceTo(payload.points().get(0));
            if (near < 30.0) {
                shake = Math.max(shake, (float) (Math.min(1.2, payload.radius() / 8.0) * (1.0 - near / 30.0)));
            }
        }
        Effect effect = new Effect(payload.kind(), payload.color(), payload.entityId(), Math.max(1, payload.ticks()), payload.radius(), payload.points(),
                level.dimension(), level.getGameTime());
        // Make room: the oldest of the heavy kinds, then the oldest of all.
        limit(effect.kind(), mostOf(effect.kind()));
        if (EFFECTS.size() >= MAX_EFFECTS) {
            EFFECTS.remove(0);
        }
        EFFECTS.add(effect);
    }

    /** How many effects of a kind may live at once: the ones that fill the screen are kept few. */
    private static int mostOf(int kind) {
        return switch (kind) {
            case ThanosFxPayload.RING_TELL -> MAX_RINGS;
            case ThanosFxPayload.CHAINS -> MAX_CHAINS;
            case ThanosFxPayload.DOME -> MAX_DOMES;
            default -> MAX_EFFECTS;
        };
    }

    /** Keeps the effects of {@code kind} under {@code most}: the oldest go. */
    private static void limit(int kind, int most) {
        int count = 0;
        for (Effect e : EFFECTS) {
            count += e.kind() == kind ? 1 : 0;
        }
        while (count >= most) {
            for (int i = 0; i < EFFECTS.size(); i++) {
                if (EFFECTS.get(i).kind() == kind) {
                    EFFECTS.remove(i);
                    count--;
                    break;
                }
            }
        }
    }

    /** Whether a dome is the Barrier or the Veil of a Thanos right there: those are drawn from his flags. */
    private static boolean bossMadeDome(ClientLevel level, ThanosFxPayload payload) {
        if (payload.points().isEmpty()) {
            return false;
        }
        ThanosEntity boss = ThanosClientView.nearestBoss(payload.points().get(0));
        if (boss == null || boss.position().distanceToSqr(payload.points().get(0)) > 4.0) {
            return false;
        }
        return payload.color() == StoneKind.SPACE.color() && boss.flag(ThanosEntity.FLAG_BARRIER)
                || payload.color() == StoneKind.REALITY.color() && boss.flag(ThanosEntity.FLAG_VEIL);
    }

    /** The shake the camera is under, in degrees, for {@link ThanosAtmosphere}. */
    public static float shake() {
        return shake;
    }

    public static void clear() {
        EFFECTS.clear();
        shake = 0.0F;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick: the effects that leave particles, and the ones that have ended

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            EFFECTS.clear();
            return;
        }
        long now = level.getGameTime();
        EFFECTS.removeIf(e -> e.dimension() != level.dimension() || now - e.start() > e.life() || now < e.start());
        shake = Math.max(0.0F, shake - 0.06F);
        RandomSource random = level.random;
        boolean thin = !EnhancedClientConfig.flightEffects();
        for (Effect e : EFFECTS) {
            float age = now - e.start();
            Vec3 at = e.points().isEmpty() ? Vec3.ZERO : e.points().get(0);
            switch (e.kind()) {
                case ThanosFxPayload.SHOCK_RING -> {
                    // The dust the ring kicks up along its front.
                    float r = e.radius() * Math.min(1.0F, age / e.ticks());
                    int count = thin ? 4 : Mth.clamp((int) (r * 1.5F), 4, 18);
                    for (int i = 0; i < count; i++) {
                        double a = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(ParticleTypes.POOF, at.x + Math.cos(a) * r, at.y + 0.15, at.z + Math.sin(a) * r, 0.0, 0.03, 0.0);
                    }
                }
                case ThanosFxPayload.PILLAR -> {
                    for (int i = 0; i < (thin ? 1 : 3); i++) {
                        double a = random.nextDouble() * Mth.TWO_PI;
                        double d = Math.sqrt(random.nextDouble()) * e.radius();
                        level.addParticle(new EnergyParticleOptions(e.color(), 1.0F), at.x + Math.cos(a) * d, at.y + 0.1, at.z + Math.sin(a) * d,
                                0.0, 0.12 + random.nextDouble() * 0.1, 0.0);
                    }
                }
                case ThanosFxPayload.STAR -> {
                    Vec3 head = starHead(e, age);
                    level.addParticle(new EnergyParticleOptions(e.color(), 1.3F), head.x, head.y, head.z, 0.0, 0.0, 0.0);
                }
                case ThanosFxPayload.PORTAL -> {
                    if (age < e.ticks() && random.nextInt(2) == 0) {
                        double a = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(new EnergyParticleOptions(0x5DCCE1, 0.8F), at.x + Math.cos(a) * e.radius(), at.y + 0.05, at.z + Math.sin(a) * e.radius(),
                                0.0, 0.04, 0.0);
                    }
                }
                default -> {
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing

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
        List<ThanosEntity> bosses = bossesInView(event.getCamera().getPosition());
        if (EFFECTS.isEmpty() && bosses.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean calm = EnhancedClientConfig.reducedMotion();
        double maxDistance = Math.min(MAX_DISTANCE, mc.options.getEffectiveRenderDistance() * 16.0 + 16.0);

        // 1. the see-through fills: discs, walls, columns, the ghost pillars, the domes' bodies
        VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        for (Effect e : EFFECTS) {
            if (visible(e, camera, maxDistance)) {
                fills(e, pose, veil, camera, time - e.start());
            }
        }
        for (ThanosEntity boss : bosses) {
            fieldFill(boss, pose, veil, camera, partial, time);
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);

        // 2. the lattice and the mandalas: textured, additive
        RenderType portalType = EnhancedRenderTypes.energyField(PORTAL_RING);
        VertexConsumer portals = buffers.getBuffer(portalType);
        for (Effect e : EFFECTS) {
            if (visible(e, camera, maxDistance) && (e.kind() == ThanosFxPayload.PORTAL || e.kind() == ThanosFxPayload.PORTAL_SKY)) {
                portal(e, pose, portals, camera, time - e.start());
            }
        }
        buffers.endBatch(portalType);
        RenderType mandalaType = EnhancedRenderTypes.energyField(MANDALA);
        VertexConsumer mandalas = buffers.getBuffer(mandalaType);
        for (Effect e : EFFECTS) {
            if (visible(e, camera, maxDistance)) {
                mandala(e, pose, mandalas, camera, time - e.start(), calm);
            }
        }
        buffers.endBatch(mandalaType);
        RenderType latticeType = EnhancedRenderTypes.energyField(LATTICE);
        VertexConsumer lattice = buffers.getBuffer(latticeType);
        for (ThanosEntity boss : bosses) {
            fieldLines(boss, pose, lattice, camera, partial, time);
        }
        buffers.endBatch(latticeType);

        // 3. the light: rings, lines, beams, stars, chains, sparks
        VertexConsumer light = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Effect e : EFFECTS) {
            if (visible(e, camera, maxDistance)) {
                lights(mc, level, e, pose, light, camera, partial, time, time - e.start(), calm);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);

        // 4. what shows through the walls
        VertexConsumer xray = buffers.getBuffer(THROUGH_WALLS);
        for (Effect e : EFFECTS) {
            if (e.kind() == ThanosFxPayload.LINE_TELL && e.radius() >= 1.0F && visible(e, camera, maxDistance)) {
                line(level, e, pose, xray, camera, partial, time - e.start());
            }
        }
        buffers.endBatch(THROUGH_WALLS);
    }

    private static List<ThanosEntity> bossesInView(Vec3 camera) {
        ThanosEntity boss = ThanosClientView.nearestBoss(camera);
        return boss == null || boss.position().distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE ? List.of() : List.of(boss);
    }

    private static boolean visible(Effect e, Vec3 camera, double maxDistance) {
        if (e.points().isEmpty()) {
            return true;
        }
        Vec3 first = e.points().get(0);
        if (first.distanceToSqr(camera) <= maxDistance * maxDistance) {
            return true;
        }
        // A line or a beam is seen if any of its ends is near.
        return e.points().size() > 1 && e.points().get(e.points().size() - 1).distanceToSqr(camera) <= maxDistance * maxDistance;
    }

    // ---- the fills ----

    private static void fills(Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float age) {
        Vec3 at = e.points().isEmpty() ? Vec3.ZERO : e.points().get(0).subtract(camera);
        int color = e.color() == -1 ? 0xFFFFFF : e.color();
        switch (e.kind()) {
            case ThanosFxPayload.RING_TELL -> {
                float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
                float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 6.0F : 1.0F;
                // The disc of the zone, a little denser as the moment comes.
                FxShapes.disc(pose, vc, at.add(0.0, 0.04, 0.0), e.radius(), SEGMENTS, argb(color, (0.10F + 0.20F * p * p) * fade),
                        argb(color, (0.06F + 0.14F * p * p) * fade));
            }
            case ThanosFxPayload.SHOCK_RING -> {
                float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
                float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 6.0F : 1.0F;
                float r = Math.max(0.2F, e.radius() * p);
                // A wall 1.5 blocks high that a jump clears: bright at the foot, gone at the top.
                FxShapes.cylinder(pose, vc, at, r, 1.5F, SEGMENTS, argb(color, 0.55F * fade), argb(color, 0.0F));
                FxShapes.annulus(pose, vc, at.add(0.0, 0.05, 0.0), Math.max(0.0F, r - 1.6F), r, SEGMENTS, argb(color, 0.0F), argb(color, 0.35F * fade));
            }
            case ThanosFxPayload.WAVE -> {
                float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
                float r = e.radius() * p;
                float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 16.0F : 1.0F;
                // The ground tinted behind the front, and the front itself.
                FxShapes.disc(pose, vc, at.add(0.0, 0.04, 0.0), Math.max(0.1F, r), SEGMENTS, argb(color, 0.10F * fade), argb(color, 0.18F * fade));
                FxShapes.annulus(pose, vc, at.add(0.0, 0.06, 0.0), Math.max(0.0F, r - 2.4F), r, SEGMENTS, argb(color, 0.0F), argb(color, 0.45F * fade));
                FxShapes.cylinder(pose, vc, at, r, 0.9F, SEGMENTS, argb(color, 0.35F * fade), argb(color, 0.0F));
            }
            case ThanosFxPayload.PILLAR -> {
                float grow = Mth.clamp(age / 10.0F, 0.0F, 1.0F);
                float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 8.0F : 1.0F;
                float pulse = 0.85F + 0.15F * Mth.sin(age * 0.5F);
                FxShapes.cylinder(pose, vc, at, e.radius() * 0.95F, 16.0F * grow, 24, argb(color, 0.32F * fade * pulse), argb(color, 0.0F));
                FxShapes.cylinder(pose, vc, at, e.radius() * 0.35F, 20.0F * grow, 16, argb(0xFFD6A0, 0.5F * fade * pulse), argb(color, 0.0F));
                FxShapes.disc(pose, vc, at.add(0.0, 0.05, 0.0), e.radius(), 32, argb(color, 0.18F * fade), argb(color, 0.1F * fade));
            }
            case ThanosFxPayload.PORTAL -> {
                // A portal on the ground is a ring the eye misses from afar: let a veil of light rise out of it.
                float open = Mth.clamp(Math.min(age / 5.0F, (e.life() - age) / 8.0F), 0.0F, 1.0F);
                float rise = 3.2F * (0.4F + 0.6F * open);
                FxShapes.cylinder(pose, vc, at, e.radius() * 1.1F, e.radius() * 0.55F, rise, 24, argb(space(e, StoneKind.SPACE.color()), 0.42F * open),
                        argb(space(e, StoneKind.SPACE.core()), 0.0F));
                FxShapes.disc(pose, vc, at.add(0.0, 0.03, 0.0), e.radius() * 1.1F, 24, argb(space(e, StoneKind.SPACE.color()), 0.30F * open),
                        argb(space(e, StoneKind.SPACE.color()), 0.16F * open));
            }
            case ThanosFxPayload.DOME -> {
                float fade = domeFade(age, e.ticks());
                Vec3 centre = at.add(0.0, Math.min(e.radius(), 1.7F), 0.0);
                EnergyGeometry.veil(pose, vc, centre, e.radius(), 32, 16, color, 0.06F * fade, 0.40F * fade);
            }
            case ThanosFxPayload.ILLUSION -> {
                float grow = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
                float fade = Mth.clamp((e.ticks() - age) / 12.0F, 0.0F, 1.0F);
                FxShapes.disc(pose, vc, at.add(0.0, 0.04, 0.0), e.radius(), 32, argb(color, 0.15F * fade), argb(color, 0.08F * fade));
                for (int i = 0; i < 8; i++) {
                    // Ghost pillars of stone that were never there: they rise, stand, and sink.
                    float angle = Mth.TWO_PI * i / 8.0F + 0.4F;
                    float height = (1.6F + 1.8F * ((i * 37) % 5) / 4.0F) * grow;
                    Vec3 base = at.add(Math.cos(angle) * e.radius(), 0.0, Math.sin(angle) * e.radius());
                    FxShapes.box(pose, vc, base, 0.45F, height, 0x8A8A92, 0.35F * fade);
                }
            }
            default -> {
            }
        }
    }

    private static float domeFade(float age, int ticks) {
        return Mth.clamp(Math.min(age / 4.0F, (ticks - age) / 6.0F), 0.0F, 1.0F);
    }

    /** The Barrier's and the Veil's bodies on a Thanos that has them up. */
    private static void fieldFill(ThanosEntity boss, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, float time) {
        Vec3 centre = boss.getPosition(partial).add(0.0, 1.65, 0.0).subtract(camera);
        float flicker = 0.9F + 0.1F * Mth.sin(time * 0.7F);
        if (boss.flag(ThanosEntity.FLAG_BARRIER)) {
            EnergyGeometry.veil(pose, vc, centre, 3.0F, 32, 16, StoneKind.SPACE.color(), 0.08F * flicker, 0.45F * flicker);
        }
        if (boss.flag(ThanosEntity.FLAG_VEIL)) {
            // A faint red shimmer round him and a red haze to the edge of the Veil.
            EnergyGeometry.veil(pose, vc, centre, 2.4F, 24, 12, StoneKind.REALITY.color(), 0.03F * flicker, 0.22F * flicker);
            FxShapes.disc(pose, vc, boss.getPosition(partial).subtract(camera).add(0.0, 0.05, 0.0), 12.0F, SEGMENTS, argb(StoneKind.REALITY.color(), 0.06F),
                    argb(StoneKind.REALITY.color(), 0.12F * flicker));
        }
    }

    private static void fieldLines(ThanosEntity boss, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, float time) {
        Vec3 centre = boss.getPosition(partial).add(0.0, 1.65, 0.0).subtract(camera);
        if (boss.flag(ThanosEntity.FLAG_BARRIER)) {
            EnergyGeometry.sphere(pose, vc, centre, 3.0F, 32, 16, 6, 3, time * 0.01F, StoneKind.SPACE.core(), 0.30F, 0.60F);
        }
        if (boss.flag(ThanosEntity.FLAG_VEIL)) {
            EnergyGeometry.sphere(pose, vc, centre, 2.4F, 24, 12, 4, 2, -time * 0.008F, StoneKind.REALITY.core(), 0.12F, 0.30F);
        }
    }

    // ---- the textured ones ----

    private static void portal(Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float age) {
        float open = Mth.clamp(Math.min(age / 5.0F, (e.life() - age) / 8.0F), 0.0F, 1.0F);
        Vec3 at = e.points().get(0).subtract(camera);
        if (e.kind() == ThanosFxPayload.PORTAL_SKY) {
            at = at.add(0.0, 30.0, 0.0);
        }
        float half = e.radius() * 1.25F * (0.6F + 0.4F * open);
        FxShapes.flatTexture(pose, vc, at.add(0.0, 0.02, 0.0), half, age * 0.06F, argb(space(e, 0xFFFFFF), 0.9F * open));
        FxShapes.flatTexture(pose, vc, at.add(0.0, 0.04, 0.0), half * 0.72F, -age * 0.11F,
                argb(space(e, StoneKind.SPACE.core()), 0.7F * open));
    }

    private static void mandala(Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float age, boolean calm) {
        if (e.kind() != ThanosFxPayload.MANDALA) {
            return;
        }
        float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
        float fade = Mth.clamp(Math.min(age / 3.0F, (e.life() - age) / 4.0F), 0.0F, 1.0F);
        Vec3 at = e.points().get(0).subtract(camera);
        float grow = 0.55F + 0.45F * AnimMath.easeOutCubic(Math.min(1.0F, age / 10.0F));
        float half = e.radius() * grow;
        float turn = calm ? 0.0F : age * 0.05F;
        // Runes turning one way; a second, smaller disc the other; both denser as the moment comes.
        FxShapes.flatTexture(pose, vc, at.add(0.0, 0.05, 0.0), half, turn, argb(e.color(), (0.35F + 0.5F * p) * fade));
        FxShapes.flatTexture(pose, vc, at.add(0.0, 0.07, 0.0), half * 0.6F, -turn * 1.7F, argb(StoneKind.TIME.core(), (0.25F + 0.45F * p) * fade));
    }

    // ---- the light ----

    private static void lights(Minecraft mc, ClientLevel level, Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, float time, float age,
                               boolean calm) {
        int color = e.color() == -1 ? 0xFFFFFF : e.color();
        switch (e.kind()) {
            case ThanosFxPayload.RING_TELL -> ringTell(e, pose, vc, camera, age, color);
            case ThanosFxPayload.LINE_TELL -> {
                if (e.radius() < 1.0F) {
                    line(level, e, pose, vc, camera, partial, age);
                }
            }
            case ThanosFxPayload.BEAM -> beam(level, e, pose, vc, camera, partial, time, age, calm);
            case ThanosFxPayload.PORTAL, ThanosFxPayload.PORTAL_SKY -> portalLight(e, pose, vc, camera, age);
            case ThanosFxPayload.STAR -> star(e, pose, vc, camera, age);
            case ThanosFxPayload.SHOCK_RING -> {
                float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
                float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 6.0F : 1.0F;
                Vec3 at = e.points().get(0).subtract(camera);
                float r = Math.max(0.2F, e.radius() * p);
                // The bright foot of the wall.
                FxShapes.annulus(pose, vc, at.add(0.0, 0.07, 0.0), Math.max(0.0F, r - 0.35F), r + 0.1F, SEGMENTS, argb(color, 0.0F), argb(0xFFFFFF, 0.8F * fade));
                FxShapes.annulus(pose, vc, at.add(0.0, 1.5, 0.0), Math.max(0.0F, r - 0.15F), r, SEGMENTS, argb(color, 0.0F), argb(color, 0.5F * fade));
            }
            case ThanosFxPayload.MANDALA -> {
                float fade = Mth.clamp(Math.min(age / 3.0F, (e.life() - age) / 4.0F), 0.0F, 1.0F);
                float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
                Vec3 at = e.points().get(0).subtract(camera);
                // The thin ring that the disc grows to fill.
                float half = e.radius() * (0.55F + 0.45F * AnimMath.easeOutCubic(Math.min(1.0F, age / 10.0F)));
                FxShapes.annulus(pose, vc, at.add(0.0, 0.09, 0.0), half - 0.12F, half, SEGMENTS, argb(color, 0.8F * fade), argb(0xFFFFFF, 0.8F * fade));
                if (p > 0.0F) {
                    FxShapes.annulus(pose, vc, at.add(0.0, 0.09, 0.0), half * p - 0.06F > 0 ? half * p - 0.06F : 0.0F, half * p, SEGMENTS, argb(color, 0.4F * fade),
                            argb(color, 0.4F * fade));
                }
            }
            case ThanosFxPayload.WAVE -> {
                float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
                float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 16.0F : 1.0F;
                Vec3 at = e.points().get(0).subtract(camera);
                float r = e.radius() * p;
                FxShapes.annulus(pose, vc, at.add(0.0, 0.08, 0.0), Math.max(0.0F, r - 0.5F), r + 0.15F, SEGMENTS, argb(color, 0.0F), argb(StoneKind.REALITY.core(), 0.9F * fade));
            }
            case ThanosFxPayload.CHAINS -> chains(level, e, pose, vc, camera, partial, time, age);
            case ThanosFxPayload.PILLAR -> {
                float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 8.0F : 1.0F;
                Vec3 at = e.points().get(0).subtract(camera);
                float pulse = 0.8F + 0.2F * Mth.sin(age * 0.6F);
                FxShapes.annulus(pose, vc, at.add(0.0, 0.07, 0.0), e.radius() - 0.25F, e.radius(), 48, argb(color, 0.9F * fade * pulse), argb(0xFFFFFF, 0.9F * fade * pulse));
                // The ring fills as the moment comes: the zone to leave.
                float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
                FxShapes.annulus(pose, vc, at.add(0.0, 0.08, 0.0), Math.max(0.0F, e.radius() * p - 0.15F), e.radius() * p, 48, argb(color, 0.6F * fade), argb(color, 0.6F * fade));
            }
            case ThanosFxPayload.DOME -> {
                float fade = domeFade(age, e.ticks());
                Vec3 at = e.points().get(0).subtract(camera);
                // The foot of the dome, a ring on the ground.
                FxShapes.annulus(pose, vc, at.add(0.0, 0.08, 0.0), e.radius() - 0.2F, e.radius(), 64, argb(color, 0.0F), argb(color, 0.7F * fade));
            }
            default -> {
            }
        }
    }

    /** A tell: the ring that marks the zone, and the ring that closes on the moment of impact. */
    private static void ringTell(Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float age, int color) {
        float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
        float fade = age > e.ticks() ? 1.0F - (age - e.ticks()) / 6.0F : 1.0F;
        Vec3 at = e.points().get(0).subtract(camera).add(0.0, 0.06, 0.0);
        float pulse = 0.75F + 0.25F * Mth.sin(age * 0.9F);
        // The rim of the zone: a bright band, the stone's colour with only a little white in it so that the colour
        // reads.
        int hot = tint(color, 0.35F);
        FxShapes.annulus(pose, vc, at, e.radius() - 0.18F, e.radius(), SEGMENTS, argb(color, 0.85F * fade * pulse), argb(hot, 0.95F * fade * pulse));
        // The inner ring grows to meet it: the moment of impact is when they touch.
        float inner = e.radius() * p;
        if (inner > 0.3F) {
            FxShapes.annulus(pose, vc, at.add(0.0, 0.01, 0.0), Math.max(0.0F, inner - 0.12F), inner, SEGMENTS, argb(color, 0.9F * fade), argb(hot, 0.9F * fade));
        }
        if (age > e.ticks()) {
            // The impact: the zone flashes white and dies.
            float k = (age - e.ticks()) / 6.0F;
            FxShapes.disc(pose, vc, at.add(0.0, 0.02, 0.0), e.radius(), SEGMENTS, argb(0xFFFFFF, 0.45F * (1.0F - k)), argb(color, 0.3F * (1.0F - k)));
        }
    }

    /** {@code color} mixed {@code amount} of the way to white. */
    private static int tint(int color, float amount) {
        int r = (int) Mth.lerp(amount, (color >> 16) & 0xFF, 255.0F);
        int g = (int) Mth.lerp(amount, (color >> 8) & 0xFF, 255.0F);
        int b = (int) Mth.lerp(amount, color & 0xFF, 255.0F);
        return r << 16 | g << 8 | b;
    }

    /** A thin line from the caster's hand (or where the server said) to the aim; white-hot at its core. */
    private static void line(ClientLevel level, Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, float age) {
        if (e.points().size() < 2) {
            return;
        }
        Vec3 from = origin(level, e, partial).subtract(camera);
        Vec3 to = e.points().get(1).subtract(camera);
        float fade = Mth.clamp(Math.min(age / 2.0F + 0.3F, (e.life() - age) / 2.0F), 0.0F, 1.0F);
        float hot = Mth.clamp(age / Math.max(1.0F, e.ticks()), 0.0F, 1.0F);
        EnergyGeometry.ribbon(pose, vc, from, to, 0.07F, argb(e.color(), 0.55F * fade), argb(e.color(), 0.55F * fade));
        EnergyGeometry.ribbon(pose, vc, from, to, 0.025F, argb(0xFFFFFF, (0.5F + 0.5F * hot) * fade), argb(0xFFFFFF, (0.5F + 0.5F * hot) * fade));
    }

    /**
     * A beam: a white core, the colour's body, a soft halo and a spiral of motes along it, with a flare where it ends.
     * A colour of -1 is the Infinity Beam's: five coaxial bands, one for each stone, round the white core.
     */
    private static void beam(ClientLevel level, Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, float time, float age, boolean calm) {
        if (e.points().size() < 2) {
            return;
        }
        Vec3 world = origin(level, e, partial);
        Vec3 from = world.subtract(camera);
        Vec3 to = e.points().get(1).subtract(camera);
        double length = from.distanceTo(to);
        if (length < 0.05) {
            return;
        }
        float fade = Mth.clamp(1.0F - age / Math.max(1.0F, e.ticks()) * 0.6F, 0.0F, 1.0F);
        float width = Math.max(0.2F, e.radius());
        float flicker = 0.9F + 0.1F * Mth.sin(time * 3.1F + e.start());
        Vec3 direction = to.subtract(from).scale(1.0 / length);
        if (e.color() == -1) {
            // The five stones in bands, widest outermost, the light of all of them at the heart.
            for (int i = 0; i < 5; i++) {
                StoneKind stone = StoneKind.granted().get(i);
                float w = width * (1.0F - 0.17F * i);
                EnergyGeometry.ribbon(pose, vc, from, to, w, argb(stone.color(), 0.55F * fade * flicker), argb(stone.color(), 0.55F * fade * flicker));
            }
            EnergyGeometry.ribbon(pose, vc, from, to, width * 0.2F, argb(0xFFFFFF, 0.98F * fade), argb(0xFFFFFF, 0.98F * fade));
            EnergyGeometry.ribbon(pose, vc, from, to, width * 1.7F, argb(0xFFFFFF, 0.10F * fade), argb(0xFFFFFF, 0.10F * fade));
        } else {
            StoneKind stone = nearest(e.color());
            int body = e.color();
            int core = stone == null ? 0xFFFFFF : stone.core();
            EnergyGeometry.ribbon(pose, vc, from, to, width * 2.4F, argb(body, 0.14F * fade * flicker), argb(body, 0.14F * fade * flicker));
            EnergyGeometry.ribbon(pose, vc, from, to, width * 1.3F, argb(body, 0.55F * fade * flicker), argb(body, 0.55F * fade * flicker));
            EnergyGeometry.ribbon(pose, vc, from, to, width * 0.55F, argb(core, 0.9F * fade), argb(core, 0.9F * fade));
            EnergyGeometry.ribbon(pose, vc, from, to, width * 0.2F, argb(0xFFFFFF, 0.98F * fade), argb(0xFFFFFF, 0.98F * fade));
        }
        // A spiral of motes along the line.
        if (!calm || length < 12) {
            Vec3 across = direction.cross(UP);
            across = across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : across.normalize();
            Vec3 other = direction.cross(across).normalize();
            int motes = (int) Mth.clamp(length * 1.5, 6.0, 40.0);
            int colorSpiral = e.color() == -1 ? 0xFFFFFF : e.color();
            for (int i = 0; i < motes; i++) {
                float t = (i + (time * 0.9F) % 1.0F) / motes;
                float angle = t * Mth.TWO_PI * (float) Math.max(2.0, length / 3.0) + time * 0.7F;
                Vec3 at = from.add(direction.scale(length * t)).add(across.scale(Mth.cos(angle) * width * 0.9F)).add(other.scale(Mth.sin(angle) * width * 0.9F));
                EnergyGeometry.ribbon(pose, vc, at, at.add(direction.scale(0.35)), width * 0.18F, argb(colorSpiral, 0.7F * fade), argb(0xFFFFFF, 0.0F));
            }
        }
        // The flare at the far end, and the flare in the hand.
        FxShapes.glow(pose, vc, to, width * 1.6F * flicker, argb(e.color() == -1 ? 0xFFFFFF : e.color(), 0.55F * fade), argb(0xFFFFFF, 0.8F * fade));
        FxShapes.glow(pose, vc, from, width * 1.0F, argb(e.color() == -1 ? 0xFFFFFF : e.color(), 0.5F * fade), argb(0xFFFFFF, 0.7F * fade));
    }

    /** The stone a colour belongs to, or null. */
    @Nullable
    private static StoneKind nearest(int color) {
        for (StoneKind stone : StoneKind.granted()) {
            if (stone.color() == color) {
                return stone;
            }
        }
        return null;
    }

    private static void portalLight(Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float age) {
        float open = Mth.clamp(Math.min(age / 5.0F, (e.life() - age) / 8.0F), 0.0F, 1.0F);
        Vec3 at = e.points().get(0).subtract(camera);
        float radius = e.radius();
        if (e.kind() == ThanosFxPayload.PORTAL_SKY) {
            // The big portal in the sky and the thin column of Space light that falls from it to where he will land.
            Vec3 top = at.add(0.0, 30.0, 0.0);
            FxShapes.annulus(pose, vc, top.add(0.0, 0.1, 0.0), radius * 0.9F, radius * 1.05F, 72, argb(StoneKind.SPACE.color(), 0.0F), argb(0xBFE6FF, 0.9F * open));
            FxShapes.cylinder(pose, vc, at, 0.45F, 0.45F, 30.0F, 12, argb(StoneKind.SPACE.core(), 0.55F * open), argb(StoneKind.SPACE.color(), 0.25F * open));
            FxShapes.cylinder(pose, vc, at, 0.15F, 0.15F, 30.0F, 8, argb(0xFFFFFF, 0.7F * open), argb(0xFFFFFF, 0.5F * open));
            FxShapes.glow(pose, vc, top, radius * 1.1F, argb(StoneKind.SPACE.color(), 0.25F * open), argb(StoneKind.SPACE.core(), 0.35F * open));
            for (int i = 0; i < 10; i++) {
                float a = age * 0.08F + i * Mth.TWO_PI / 10.0F;
                FxShapes.glow(pose, vc, top.add(Mth.cos(a) * radius, 0.1, Mth.sin(a) * radius), 0.5F, argb(0xBFE6FF, 0.7F * open), argb(0xFFFFFF, 0.9F * open));
            }
            return;
        }
        // A ring portal: a bright rim and sparks that orbit it.
        FxShapes.annulus(pose, vc, at.add(0.0, 0.1, 0.0), radius * 0.86F, radius, 48, argb(space(e, StoneKind.SPACE.color()), 0.0F),
                argb(space(e, 0xBFE6FF), 0.95F * open));
        FxShapes.annulus(pose, vc, at.add(0.0, 0.1, 0.0), radius * 0.0F, radius * 0.86F, 48, argb(space(e, 0x0A1A40), 0.0F),
                argb(space(e, StoneKind.SPACE.color()), 0.12F * open));
        for (int i = 0; i < 6; i++) {
            float a = age * 0.2F + i * Mth.TWO_PI / 6.0F;
            FxShapes.glow(pose, vc, at.add(Mth.cos(a) * radius, 0.12, Mth.sin(a) * radius), radius * 0.16F, argb(space(e, 0xBFE6FF), 0.7F * open),
                    argb(0xFFFFFF, 0.9F * open));
        }
    }

    /**
     * 3.5: one of the Space Stone's colours of a portal, or it recoloured to the portal's own colour when the portal is
     * not the stone's blue (a custom hero's, in the colour his pick chose; the server sends it in the payload).
     */
    private static int space(Effect e, int rgb) {
        return e.color() == -1 || e.color() == StoneKind.SPACE.color() ? rgb : AbilityColors.recolor(rgb, e.color());
    }

    /** Where a falling star's head is {@code age} ticks in. */
    private static Vec3 starHead(Effect e, float age) {
        Vec3 from = e.points().get(0);
        Vec3 to = e.points().size() > 1 ? e.points().get(1) : from;
        float p = Mth.clamp(age / e.ticks(), 0.0F, 1.0F);
        return from.lerp(to, p * p * (3.0F - 2.0F * p) * 0.4F + p * 0.6F);
    }

    /** A falling star: a bright head and a long tapering ribbon behind it, in the stone's colour. */
    private static void star(Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float age) {
        if (e.points().size() < 2) {
            return;
        }
        Vec3 head = starHead(e, age).subtract(camera);
        Vec3 from = e.points().get(0).subtract(camera);
        Vec3 back = from.subtract(head);
        double length = back.length();
        float landed = age > e.ticks() ? Mth.clamp(1.0F - (age - e.ticks()) / 8.0F, 0.0F, 1.0F) : 1.0F;
        if (length > 0.01) {
            Vec3 tail = head.add(back.scale(Math.min(1.0, 16.0 / length)));
            EnergyGeometry.ribbon(pose, vc, head, tail, 0.7F, 0.0F, argb(e.color(), 0.8F * landed), argb(e.color(), 0.0F));
            EnergyGeometry.ribbon(pose, vc, head, tail, 0.25F, 0.0F, argb(0xFFFFFF, 0.95F * landed), argb(0xFFFFFF, 0.0F));
        }
        FxShapes.glow(pose, vc, head, 1.4F * landed, argb(e.color(), 0.6F * landed), argb(0xFFFFFF, 0.9F * landed));
        if (age > e.ticks()) {
            // The fusing: a flare on the Gauntlet.
            FxShapes.glow(pose, vc, e.points().get(1).subtract(camera), 2.4F * landed, argb(e.color(), 0.6F * landed), argb(0xFFFFFF, 0.95F * landed));
        }
    }

    /** Orange chains from the caster's hand to each target, tense, with motes flowing back along them. */
    private static void chains(ClientLevel level, Effect e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, float time, float age) {
        Vec3 world = origin(level, e, partial);
        Vec3 from = world.subtract(camera);
        float grow = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        float fade = Mth.clamp((e.life() - age) / 3.0F, 0.0F, 1.0F);
        for (Vec3 point : e.points()) {
            Vec3 to = from.lerp(point.subtract(camera), grow);
            double length = from.distanceTo(to);
            if (length < 0.1) {
                continue;
            }
            Vec3 direction = to.subtract(from).scale(1.0 / length);
            int links = (int) Math.max(2, length / 0.34);
            for (int i = 0; i < links; i++) {
                double t0 = (double) i / links;
                double t1 = (i + 0.7) / links;
                // The chain hangs a little between its ends and tenses as it grows taut.
                double sag = Math.sin(Math.PI * t0) * 0.25 * (1.0 - grow);
                Vec3 a = from.add(direction.scale(length * t0)).add(0.0, -sag, 0.0);
                Vec3 b = from.add(direction.scale(length * t1)).add(0.0, -sag, 0.0);
                boolean wide = i % 2 == 0;
                EnergyGeometry.ribbon(pose, vc, a, b, wide ? 0.15F : 0.07F, argb(0xFF8A1E, 0.9F * fade), argb(0xFF8A1E, 0.9F * fade));
                EnergyGeometry.ribbon(pose, vc, a, b, wide ? 0.07F : 0.03F, argb(0xFFD6A0, 0.95F * fade), argb(0xFFD6A0, 0.95F * fade));
            }
            // Motes flowing from the victim back to the hand.
            for (int i = 0; i < 6; i++) {
                float t = 1.0F - ((time * 0.12F + i / 6.0F) % 1.0F);
                Vec3 at = from.add(direction.scale(length * t));
                FxShapes.glow(pose, vc, at, 0.18F, argb(0xFF8A1E, 0.8F * fade), argb(0xFFFFFF, 0.8F * fade));
            }
            FxShapes.glow(pose, vc, to, 0.5F * grow, argb(0xFF8A1E, 0.5F * fade), argb(0xFFD6A0, 0.7F * fade));
        }
    }

    /**
     * Where a beam or a chain leaves the caster: his Gauntlet's hand while he is there to ask (the boss's, or the
     * hero's off hand), else the point the server named.
     */
    private static Vec3 origin(ClientLevel level, Effect e, float partial) {
        Vec3 fallback = e.points().isEmpty() ? Vec3.ZERO : e.points().get(0);
        if (e.entityId() < 0) {
            return fallback;
        }
        Entity caster = level.getEntity(e.entityId());
        if (caster instanceof ThanosEntity boss) {
            Vec3 shift = boss.getPosition(partial).subtract(boss.position());
            return boss.handPosition().add(shift);
        }
        if (caster instanceof AbstractClientPlayer player && Powers.get(player).acts(Power.THANOS)) {
            return gauntletHand(Minecraft.getInstance(), player, partial, fallback);
        }
        return fallback;
    }

    /**
     * Where the hero's Gauntlet is in the world: on his off hand as the body is drawn, else where the server computed
     * it.
     */
    static Vec3 gauntletHand(Minecraft mc, AbstractClientPlayer player, float partial, Vec3 fallback) {
        HumanoidArm off = player.getMainArm().getOpposite();
        Vec3 tracked = HandTracker.hand(player, off, partial);
        if (tracked != null) {
            return tracked;
        }
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            Vec3 look = player.getViewVector(partial);
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            double side = off == HumanoidArm.RIGHT ? 1.0 : -1.0;
            // Where the hand is drawn: its resting place, moved with the first-person gesture (the view-space offset is about
            // 0.8 blocks from the camera, so 0.4 of it is the offset in the world), as WorldEffectsRenderer.handPosition does
            // for the main hand.
            float[] moved = new float[3];
            if (player == mc.player) {
                FirstPersonArms.hand(mc.player, off, moved);
            }
            return player.getEyePosition(partial).add(look.scale(0.7 + 0.4 * moved[2])).add(right.scale(side * (0.3 + 0.4 * moved[0])))
                    .add(up.scale(-0.3 + 0.4 * moved[1]));
        }
        return ThanosAbilities.handPosition(player).add(player.getPosition(partial).subtract(player.position()));
    }

    private ThanosFx() {
    }
}
