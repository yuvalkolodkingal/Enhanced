package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * Everything of a golden shield that is not the plain dome of the hero (that is the generic loop of {@link
 * WorldEffectsRenderer}, which draws the dome of every hero whose {@link HeroClient#shieldStyle} says one): the boss's
 * dome, a lattice of hexagons (DESIGN section 17, 3.5.6) whose state comes from the entity's synced data, brighter at the
 * top, that knits itself upward from the ground while it forms or re-forms, flickers when it is nearly spent and shows
 * <em>cracks</em> in proportion to how much of it is gone; the ripple that runs out over the sphere from every blow it
 * turns away; and the golden hexagonal shards that fly outward and fall when it breaks. The hero's dome has the same cracks,
 * ripples and shards (his dome itself is the world renderer's).
 *
 * <p>Ripples and shatterings arrive as {@link com.kolodkingal.enhanced.network.ApocalypseFxPayload}s (the impact point and
 * the entity); the rest is read from the entity every frame, so a player who comes into view late sees the dome as it is.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ShieldFx {
    /** The lattice of the dome: a hexagonal web in the shield's gold. */
    public static final ResourceLocation WEB = Enhanced.id("textures/entity/golden_web.png");
    /** How long a ripple runs, and how long the shards of a broken shield fly. */
    public static final int RIPPLE_TICKS = 16;
    public static final int SHATTER_TICKS = 44;
    /** How long a shield takes to knit (the server's number). */
    public static final int KNIT_TICKS = 40;
    private static final int MAX_RIPPLES = 24;
    private static final int SHARDS = 40;
    /** Nothing is drawn of a shield farther from the camera than this. */
    private static final double MAX_DISTANCE = 120.0;

    private record Ripple(int id, Vec3 point, long start) {
    }

    private record Shatter(int id, Vec3 centre, double radius, long start, long seed) {
    }

    /** What state a boss's shield was last seen in and since when (the knitting is timed from it). */
    private static final class Mark {
        int state = -1;
        long since;
    }

    private static final List<Ripple> RIPPLES = new ArrayList<>();
    private static final List<Shatter> SHATTERS = new ArrayList<>();
    private static final Map<Integer, Mark> MARKS = new HashMap<>();

    /** A blow was turned away at {@code point} (world coordinates) by the shield of the entity {@code id}. */
    public static void hit(int id, Vec3 point) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (RIPPLES.size() >= MAX_RIPPLES) {
            RIPPLES.remove(0);
        }
        RIPPLES.add(new Ripple(id, point, level.getGameTime()));
    }

    /** The shield of the entity {@code id} broke: its shards fly. */
    public static void broke(int id, Vec3 centre, double radius) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        SHATTERS.add(new Shatter(id, centre, radius, level.getGameTime(), level.random.nextLong()));
        if (SHATTERS.size() > 6) {
            SHATTERS.remove(0);
        }
        RIPPLES.removeIf(ripple -> ripple.id() == id);
    }

    /** A shield began to knit again: the next time it is drawn it grows from the ground. */
    public static void reform(int id) {
        Mark mark = MARKS.get(id);
        if (mark != null) {
            mark.state = -1;
        }
    }

    public static void clear() {
        RIPPLES.clear();
        SHATTERS.clear();
        MARKS.clear();
    }

    /**
     * How far a shield that is knitting has grown (0 to 1), eased: from the ground up, over {@link #KNIT_TICKS}.
     *
     * @param ticks the ticks since it began to knit
     */
    public static float knit(float ticks) {
        return AnimMath.ss(0.0F, KNIT_TICKS, ticks);
    }

    /**
     * The angle (radians from the south pole) the lattice of a dome reaches while it knits: it starts at the ground (a
     * little below the middle of the sphere) and climbs to the pole. {@code groundLat} is the latitude of the ground.
     */
    public static float knitLat(float grown, float groundLat) {
        return Mth.lerp(grown, Mth.HALF_PI + groundLat, Mth.PI);
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
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        float time = level.getGameTime() + partial;
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean any = false;

        RenderType lattice = EnhancedRenderTypes.energyField(WEB);
        // The boss's dome: the veil (a thin gold body), then the lattice on it.
        List<Dome> domes = new ArrayList<>();
        for (ApocalypseEntity boss : ApocalypseBosses.all()) {
            Dome dome = bossDome(boss, camera, time, partial, level);
            if (dome != null) {
                domes.add(dome);
            }
        }
        for (AbstractClientPlayer player : level.players()) {
            Dome dome = heroDome(player, camera, time, partial);
            if (dome != null) {
                domes.add(dome);
            }
        }
        if (!domes.isEmpty()) {
            any = true;
            VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
            for (Dome dome : domes) {
                if (dome.veil() > 0.0F) {
                    EnergyGeometry.veil(pose, veil, dome.centre(), dome.radius(), 32, 16, SandPalette.GOLD, dome.veil() * 0.2F, dome.veil() * 0.6F);
                }
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
            VertexConsumer web = buffers.getBuffer(lattice);
            for (Dome dome : domes) {
                if (dome.web() > 0.0F) {
                    SandShapes.dome(pose, web, dome.centre(), dome.radius(), 32, 16, 6.0F, 3.0F, time * 0.004F, SandPalette.GOLD, 0.30F * dome.web(),
                            0.70F * dome.web(), 0.35F * dome.web(), dome.maxLat());
                }
            }
            buffers.endBatch(lattice);
            VertexConsumer cracks = buffers.getBuffer(SandRenderTypes.FLAT);
            for (Dome dome : domes) {
                cracks(pose, cracks, dome, time);
            }
            buffers.endBatch(SandRenderTypes.FLAT);
            VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            for (Dome dome : domes) {
                ripples(pose, beams, dome, level.getGameTime(), partial);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
        if (!SHATTERS.isEmpty()) {
            SHATTERS.removeIf(shatter -> level.getGameTime() - shatter.start() > SHATTER_TICKS);
            VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            for (Shatter shatter : SHATTERS) {
                shards(pose, beams, shatter, camera, time);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
        RIPPLES.removeIf(ripple -> level.getGameTime() - ripple.start() > RIPPLE_TICKS);
        if (any) {
            buffers.endBatch();
        }
    }

    /** One dome to draw this frame. */
    private record Dome(int id, Vec3 centre, Vec3 world, float radius, float web, float veil, float maxLat, float strength, boolean cracked, boolean mine) {
    }

    /** The dome of a boss: what his synced shield state and strength say, knitted from the ground as it forms. */
    private static Dome bossDome(ApocalypseEntity boss, Vec3 camera, float time, float partial, ClientLevel level) {
        if (boss.isInvisible()) {
            return null;
        }
        ApocalypseShield.State state = boss.shieldState();
        if (state == ApocalypseShield.State.GONE || state == ApocalypseShield.State.BROKEN) {
            MARKS.remove(boss.getId());
            return null;
        }
        Vec3 world = boss.getPosition(partial).add(0.0, boss.getBbHeight() * 0.5, 0.0);
        if (world.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) {
            return null;
        }
        Mark mark = MARKS.computeIfAbsent(boss.getId(), id -> new Mark());
        if (mark.state != state.ordinal()) {
            // A boss seen for the first time already up has been up for a while; one that is knitting is timed from now.
            boolean knitting = state == ApocalypseShield.State.FORMING || state == ApocalypseShield.State.REFORMING;
            mark.since = mark.state == -1 && !knitting ? level.getGameTime() - KNIT_TICKS : level.getGameTime();
            mark.state = state.ordinal();
        }
        float fraction = boss.shieldFraction();
        float grown = state == ApocalypseShield.State.UP ? 1.0F : knit(time - mark.since);
        float flicker = fraction < 0.25F && state == ApocalypseShield.State.UP && !EnhancedClientConfig.reducedMotion()
                ? 0.75F + 0.25F * Mth.sin(time * 0.8F) : 1.0F; // (3.3) a slow throb, not a six-a-second strobe
        float alpha = (0.4F + 0.6F * grown) * flicker;
        float radius = (float) ApocalypseShield.RADIUS;
        float groundLat = (float) Math.asin(-Math.min(0.95, boss.getBbHeight() * 0.5 / radius));
        float maxLat = state == ApocalypseShield.State.UP ? Mth.PI : knitLat(grown, groundLat);
        return new Dome(boss.getId(), world.subtract(camera), world, radius, alpha, grown >= 0.85F ? alpha : 0.0F, maxLat, fraction, true, false);
    }

    /** The extras of the hero's dome (the dome itself is the world renderer's): his cracks and ripples when it is complete. */
    private static Dome heroDome(AbstractClientPlayer player, Vec3 camera, float time, float partial) {
        PowerData data = Powers.get(player);
        if (!data.has(HeroAbility.APOCALYPSE_SHIELD) || !data.chaosShieldActive() || player.isInvisible() || player.isSpectator()) {
            return null;
        }
        Vec3 world = player.getPosition(partial).add(0.0, player.getBbHeight() * 0.5, 0.0);
        if (world.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE || data.chaosShieldProgress(time) < 1.0F) {
            return null;
        }
        float max = ApocalypseGuard.maxStrength();
        float fraction = max <= 0.0F ? 1.0F : Mth.clamp(data.shieldStrength() / max, 0.0F, 1.0F);
        boolean mine = player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        // No lattice of its own (the world renderer draws it), only the cracks and the ripples.
        return new Dome(player.getId(), world.subtract(camera), world, (float) ApocalypseGuard.radius(), 0.0F, 0.0F, Mth.PI, fraction, true, mine);
    }

    /** The cracks: dark lines on the sphere, more of them as it weakens. */
    private static void cracks(Matrix4f pose, VertexConsumer vc, Dome dome, float time) {
        if (!dome.cracked()) {
            return;
        }
        int count = Math.round((1.0F - dome.strength()) * 14.0F);
        if (count <= 0) {
            return;
        }
        float visible = dome.mine() ? 0.35F : 1.0F;
        for (int i = 0; i < count; i++) {
            long seed = dome.id() * 7919L + i * 104729L;
            RandomSource random = RandomSource.create(seed);
            Vec3 start = new Vec3(random.nextDouble() - 0.5, random.nextDouble() * 1.4 - 0.5, random.nextDouble() - 0.5).normalize();
            List<Vec3> line = SandShapes.crackOnSphere(dome.centre(), dome.radius() * 1.004, start, 5 + random.nextInt(5), 0.16, seed);
            EnergyGeometry.polyline(pose, vc, line, 0.055F, argb(0x2A1C08, 0.8F * visible));
            EnergyGeometry.polyline(pose, vc, line, 0.018F, argb(SandPalette.GOLD_CORE, 0.55F * visible));
        }
    }

    /** The ripples of the blows it turned away: a ring that runs out over the sphere from the point of impact. */
    private static void ripples(Matrix4f pose, VertexConsumer vc, Dome dome, long now, float partial) {
        for (Ripple ripple : RIPPLES) {
            if (ripple.id() != dome.id()) {
                continue;
            }
            float age = now - ripple.start() + partial;
            float u = age / RIPPLE_TICKS;
            if (u < 0.0F || u > 1.0F) {
                continue;
            }
            Vec3 direction = ripple.point().subtract(dome.world());
            if (direction.lengthSqr() < 1.0E-6) {
                direction = SandShapes.UP;
            }
            direction = direction.normalize();
            double arc = 0.06 + 1.25 * AnimMath.easeOutCubic(u);
            float fade = (1.0F - u) * (dome.mine() ? 0.4F : 1.0F);
            List<Vec3> ring = SandShapes.circleOnSphere(dome.centre(), dome.radius() * 1.01, direction, arc, 28);
            EnergyGeometry.polyline(pose, vc, ring, 0.16F, argb(SandPalette.GOLD, 0.30F * fade));
            EnergyGeometry.polyline(pose, vc, ring, 0.05F, argb(SandPalette.GOLD_CORE, 0.9F * fade));
        }
    }

    /** The shards of a broken shield: golden hexagons flung outward that tumble and fall, fading. */
    private static void shards(Matrix4f pose, VertexConsumer vc, Shatter shatter, Vec3 camera, float time) {
        float age = time - shatter.start();
        if (age < 0.0F) {
            return;
        }
        RandomSource random = RandomSource.create(shatter.seed());
        float fade = 1.0F - AnimMath.ss(SHATTER_TICKS * 0.55F, SHATTER_TICKS, age);
        for (int i = 0; i < SHARDS; i++) {
            Vec3 dir = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.25, random.nextDouble() - 0.5).normalize();
            double speed = 0.10 + random.nextDouble() * 0.12;
            double size = 0.10 + random.nextDouble() * 0.12;
            double spin = random.nextDouble() * 0.5;
            Vec3 at = shatter.centre().add(dir.scale(shatter.radius())).add(dir.scale(speed * age)).add(0.0, -0.5 * 0.035 * age * age, 0.0);
            int color = argb(i % 3 == 0 ? SandPalette.GOLD_CORE : SandPalette.GOLD, 0.85F * fade);
            SandShapes.hexShard(pose, vc, at.subtract(camera), dir.add(0.0, 0.2, 0.0), size, spin * age, color);
        }
    }

    /** Whether a thing the client holds belongs to an entity (a test of the clean-up). */
    static boolean holds(Entity entity) {
        return MARKS.containsKey(entity.getId());
    }

    private ShieldFx() {
    }
}
