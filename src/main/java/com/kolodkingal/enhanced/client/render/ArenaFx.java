package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * The arena and the air round it (DESIGN section 17, 3.2 and 5.2). When the Capstone is planted the sky and the fog turn
 * amber for every client within the arena's radius and 64 blocks over three seconds (the colour of the haze of Cairo, {@link
 * SandPalette#HAZE}); inside the circle the fog closes in to a far plane of 40 blocks; the wall of sand at the radius the
 * fighters are kept in rises from the ground over two seconds, a cylinder of whirling sand that is solid to everything
 * inside it, forty blocks tall, scrolling, with sand drifting off it; the camera trembles while the ground shakes (a hit
 * of the boss that lands near, the plates of his armour leaving him). The ritual itself is a column of gold light driven
 * from the sky into the click. Inside a storm the hero raised (or any) the sight is blotted out to a few blocks.
 *
 * <p>The server sends {@code ARENA_ON} when the fight begins, with the middle, the radius and the height (and again to a
 * player who comes into view later) and {@code ARENA_OFF} when it ends; everything here follows from those and from the
 * camera. With <em>Reduced motion</em> there is no tint, no fog, no tremor and the wall is a plain one that does not scroll.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ArenaFx {
    /** How far from the wall the amber air is seen: the arena's radius and this much more. */
    private static final double TINT_REACH = ApocalypseFxMath.TINT_REACH;
    /** The fog far plane inside the circle, and in a storm. */
    private static final float ARENA_FOG = 40.0F;
    private static final float STORM_FOG = 9.0F;
    /** The ticks the tint takes to come, and the wall to rise. */
    private static final float TINT_TICKS = 40.0F;
    private static final float WALL_TICKS = ApocalypseFxMath.WALL_TICKS;
    private static final int WALL_SEGMENTS = 96;
    /** The ritual's column of light: how long it stands, how high. */
    private static final int RITUAL_TICKS = 70;
    private static final double RITUAL_HEIGHT = 90.0;

    private static boolean on;
    private static int fight;
    /** The dimension the arena stands in: a client that left it (and so missed its end) does not keep its wall. */
    @Nullable
    private static ResourceKey<Level> dimension;
    private static Vec3 centre = Vec3.ZERO;
    private static double radius;
    private static double height;
    private static long since = Long.MIN_VALUE;
    /** How amber the air is (0 to 1), eased toward where it should be. */
    private static float fade;
    /** Whether the camera is inside the circle. */
    private static boolean inside;

    private static Vec3 ritualAt;
    private static long ritualStart = Long.MIN_VALUE;

    private static float shakeStrength;
    private static long shakeStart = Long.MIN_VALUE;
    private static int shakeTicks;

    /** The fight began (or the client came into view of it): the wall rises round {@code middle}. */
    public static void on(int id, Vec3 middle, int wallRadius, int wallHeight) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        boolean same = on && fight == id;
        on = true;
        fight = id;
        dimension = level.dimension();
        centre = middle;
        radius = wallRadius;
        height = wallHeight;
        if (!same) {
            // A client that is told late (it came into view) finds the wall standing.
            since = level.getGameTime();
        }
    }

    /** The fight ended. */
    public static void off() {
        on = false;
    }

    /** The ritual began at {@code click}: a column of gold light, and the ground trembles. */
    public static void ritual(Vec3 click) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        ritualAt = click;
        ritualStart = level.getGameTime();
        shake(0.45F, 60, click);
    }

    /** The ground shakes at {@code at}: the camera trembles if it is near, less the farther it is. */
    public static void shake(float strength, int ticks, Vec3 at) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getCameraEntity() == null || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        double distance = mc.getCameraEntity().position().distanceTo(at);
        float near = (float) Mth.clamp(1.0 - distance / 40.0, 0.0, 1.0);
        if (near <= 0.02F) {
            return;
        }
        shakeStrength = Math.max(shakeStrength * 0.5F, strength * near);
        shakeStart = mc.level.getGameTime();
        shakeTicks = ticks;
    }

    public static void clear() {
        on = false;
        fade = 0.0F;
        inside = false;
        since = Long.MIN_VALUE;
        ritualStart = Long.MIN_VALUE;
        shakeStart = Long.MIN_VALUE;
        shakeStrength = 0.0F;
    }

    /** Whether an arena is standing (for the music and the tests). */
    public static boolean isOn() {
        return on;
    }

    /** How amber the air is now (0 to 1). */
    public static float tint() {
        return fade;
    }

    /** The radius of the wall, or 0. */
    public static double wallRadius() {
        return on ? radius : 0.0;
    }

    /**
     * The fraction of the wall that has risen, {@code ticks} after the fight began: it stands from the ground to its full
     * height over {@link #WALL_TICKS}.
     */
    public static float risen(float ticks) {
        return ApocalypseFxMath.wallRisen(ticks);
    }

    /**
     * How strongly the air is tinted for a camera {@code distance} blocks horizontally from the middle of an arena of a wall
     * {@code radius}: full inside the wall, falling off to nothing {@link #TINT_REACH} blocks beyond it.
     */
    public static float reach(double distance, double radius) {
        return ApocalypseFxMath.tintReach(distance, radius);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Entity camera = mc.getCameraEntity();
        if (level == null || camera == null) {
            clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        float target = 0.0F;
        inside = false;
        if (on && !level.dimension().equals(dimension)) {
            on = false; // the fight is in another dimension now: this client missed its end
        }
        if (on) {
            double distance = Math.hypot(camera.getX() - centre.x, camera.getZ() - centre.z);
            inside = distance < radius;
            target = EnhancedClientConfig.reducedMotion() ? 0.0F : reach(distance, radius);
        }
        // Three seconds to turn (a client that is told late sees it already there: no, it also turns, it is only a fade).
        fade += Mth.clamp(target - fade, -1.0F / TINT_TICKS, 1.0F / TINT_TICKS);
        RandomSource random = level.random;
        float density = SandFx.density();
        if (on && camera.position().distanceTo(centre) < radius + 40.0) {
            wallParticles(level, camera, random, density);
        }
        ritualParticles(level, random, density);
    }

    /** Sand drifts off the wall near the camera. */
    private static void wallParticles(ClientLevel level, Entity camera, RandomSource random, float density) {
        double angleToCamera = Math.atan2(camera.getZ() - centre.z, camera.getX() - centre.x);
        float risen = risen(level.getGameTime() - since);
        for (int i = 0; i < Math.round(6 * density); i++) {
            double angle = angleToCamera + (random.nextDouble() - 0.5) * 0.8;
            double h = random.nextDouble() * Math.min(height, 30.0) * risen;
            double y = centre.y + h;
            level.addParticle(SandParticles.GRAIN, centre.x + Math.cos(angle) * radius,
                    y, centre.z + Math.sin(angle) * radius, -Math.sin(angle) * 0.25, 0.02, Math.cos(angle) * 0.25);
        }
    }

    /** Dust and motes of gold round the click while the ritual runs. */
    private static void ritualParticles(ClientLevel level, RandomSource random, float density) {
        long age = level.getGameTime() - ritualStart;
        if (ritualAt == null || age < 0 || age > RITUAL_TICKS) {
            return;
        }
        for (int i = 0; i < Math.round(6 * density); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = random.nextDouble() * 3.5;
            level.addParticle(SandParticles.GRAIN, ritualAt.x + Math.cos(angle) * r, ritualAt.y + 0.1,
                    ritualAt.z + Math.sin(angle) * r, Math.cos(angle) * 0.05, 0.12 + random.nextDouble() * 0.15, Math.sin(angle) * 0.05);
        }
        for (int i = 0; i < Math.round(3 * density); i++) {
            level.addParticle(new com.kolodkingal.enhanced.particle.EnergyParticleOptions(SandPalette.GOLD, 1.2F), ritualAt.x + (random.nextDouble() - 0.5) * 1.2,
                    ritualAt.y + random.nextDouble() * 6.0, ritualAt.z + (random.nextDouble() - 0.5) * 1.2, 0.0, 0.08, 0.0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The air

    /** The fog turns the amber of the haze of Cairo; in a storm it is the colour of the sand. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float k = fade * 0.8F;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !EnhancedClientConfig.reducedMotion() && SandFx.stormAt(mc.level, event.getCamera().getPosition()) > 0.0F) {
            k = Math.max(k, 0.85F);
        }
        if (k <= 0.001F) {
            return;
        }
        event.setRed(Mth.lerp(k, event.getRed(), ((SandPalette.HAZE >> 16) & 0xFF) / 255.0F));
        event.setGreen(Mth.lerp(k, event.getGreen(), ((SandPalette.HAZE >> 8) & 0xFF) / 255.0F));
        event.setBlue(Mth.lerp(k, event.getBlue(), (SandPalette.HAZE & 0xFF) / 255.0F));
    }

    /** Inside the circle the fog closes in to 40 blocks; inside a storm it blots the sight out to a few. */
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float far = event.getFarPlaneDistance();
        float wanted = far;
        if (inside && fade > 0.0F) {
            wanted = Math.min(wanted, Mth.lerp(fade, far, ARENA_FOG));
        }
        if (mc.level != null && SandFx.stormAt(mc.level, event.getCamera().getPosition()) > 0.0F) {
            wanted = Math.min(wanted, STORM_FOG);
        }
        if (wanted < far) {
            event.setFarPlaneDistance(wanted);
            event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), wanted * 0.05F));
            event.setCanceled(true);
        }
    }

    /** The camera trembles while the ground shakes (not with reduced motion). */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || shakeStrength <= 0.01F || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        float age = mc.level.getGameTime() + (float) event.getPartialTick() - shakeStart;
        if (age < 0.0F || age > shakeTicks) {
            shakeStrength = 0.0F;
            return;
        }
        float fadeOut = 1.0F - AnimMath.ss(shakeTicks * 0.5F, shakeTicks, age);
        float t = age * 2.3F;
        event.setPitch(event.getPitch() + shakeStrength * fadeOut * Mth.sin(t));
        event.setRoll(event.getRoll() + shakeStrength * 0.6F * fadeOut * Mth.sin(t * 0.77F + 1.0F));
    }

    // ---------------------------------------------------------------------------------------------------------
    // The wall and the column

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
        boolean column = ritualAt != null && level.getGameTime() - ritualStart <= RITUAL_TICKS;
        if (!on && !column) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        float time = level.getGameTime() + partial;
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        if (on) {
            float risen = risen(time - since);
            VertexConsumer wall = buffers.getBuffer(SandRenderTypes.textured(SandFx.SWIRL));
            double base = centre.y - 6.0;
            float scroll = EnhancedClientConfig.reducedMotion() ? 0.0F : time * 0.012F;
            SandShapes.wall(pose, wall, new Vec3(centre.x - camera.x, base - camera.y, centre.z - camera.z), radius, (height + 6.0) * risen, WALL_SEGMENTS, 8, 44.0F,
                    Math.max(2.0F, (float) (height * risen / 5.0)), scroll, SandPalette.LIGHT, 0.62F, 0.10F, 0, 1);
            buffers.endBatch(SandRenderTypes.textured(SandFx.SWIRL));
        }
        if (column) {
            long age = level.getGameTime() - ritualStart;
            float rise = AnimMath.ss(0.0F, 14.0F, age + partial);
            float fall = 1.0F - AnimMath.ss(RITUAL_TICKS - 20, RITUAL_TICKS, age + partial);
            float pulse = 0.8F + 0.2F * Mth.sin(time * 0.6F);
            VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            Vec3 c = ritualAt.subtract(camera);
            SandShapes.column(pose, beams, c, 0.45 + 0.35 * rise, RITUAL_HEIGHT * rise, 20, argb(SandPalette.GOLD_CORE, 0.7F * fall * pulse), argb(SandPalette.GOLD, 0.12F * fall));
            SandShapes.column(pose, beams, c, 1.4, RITUAL_HEIGHT * rise, 20, argb(SandPalette.GOLD, 0.25F * fall * pulse), argb(SandPalette.GOLD, 0.0F));
            SandShapes.flatRing(pose, beams, c.add(0.0, 0.1, 0.0), 2.0 + 3.0 * AnimMath.easeOutCubic(Math.min(1.0F, age / 30.0F)), 0.4, 48,
                    argb(SandPalette.GOLD, 0.6F * fall * (1.0F - Math.min(1.0F, age / 40.0F))));
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
    }

    private ArenaFx() {
    }
}
