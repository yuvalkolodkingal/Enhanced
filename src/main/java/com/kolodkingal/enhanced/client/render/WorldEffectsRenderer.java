package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.network.HexBoltPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the energy effects that live in the world rather than on a body: the Hex walls, the Chaos Beam, the Chaos
 * Shield, the telekinetic tether and Billy's Hex Bolts. Everything is drawn for every player, including yourself in
 * first person.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class WorldEffectsRenderer {
    public static final ResourceLocation HEX_TEXTURE = Enhanced.id("textures/entity/hex_wall.png");
    /** The Hex from outside, as in WandaVision: glowing CRT 'cathodic bricks' and scanlines. */
    public static final ResourceLocation HEX_BARRIER = Enhanced.id("textures/entity/hex_barrier.png");
    /** The angry red Hex (WandaVision episodes 5-9): raspberry scanlines over a dark magenta-red body. */
    private static final int HEX_LINES = 0xC03762;
    private static final int HEX_BODY = 0x6F0D28;
    /** The Chaos Shield: a translucent crimson dome with a hot pink rim. */
    private static final int SHIELD_BODY = 0xC72D4B;
    private static final Vec3 UP = new Vec3(0, 1, 0);

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
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float time = level.getGameTime() + partialTick;

        double renderDistance = mc.options.getEffectiveRenderDistance() * 16.0;
        // Two passes for every energy dome: a see-through red veil, then glowing lines on top (the Hex's scanlines and
        // bricks; the Chaos Shield's hexagons).
        for (int pass = 0; pass < 2; pass++) {
            boolean lines = pass == 1;
            RenderType hexType = lines ? EnhancedRenderTypes.energyField(HEX_BARRIER) : EnhancedRenderTypes.ENERGY_VEIL;
            renderHexes(level, pose, buffers.getBuffer(hexType), lines, camera, time, renderDistance);
            buffers.endBatch(hexType);
            RenderType type = lines ? EnhancedRenderTypes.energyField(HEX_TEXTURE) : EnhancedRenderTypes.ENERGY_VEIL;
            VertexConsumer vc = buffers.getBuffer(type);
            for (AbstractClientPlayer player : level.players()) {
                PowerData data = Powers.get(player);
                if (data.power() == Power.SCARLET_WITCH && data.chaosShieldActive() && !player.isInvisible()) {
                    Vec3 center = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0).subtract(camera);
                    float flicker = 0.9F + 0.1F * Mth.sin(time * 0.6F);
                    float radius = (float) WandaAbilities.SHIELD_RADIUS;
                    if (lines) {
                        EnergyGeometry.sphere(pose, vc, center, radius, 32, 16, 6, 3, time * 0.01F,
                                Power.SCARLET_WITCH.color, 0.35F * flicker, 0.65F * flicker);
                    } else {
                        EnergyGeometry.veil(pose, vc, center, radius, 32, 16, SHIELD_BODY, 0.12F * flicker, 0.45F * flicker);
                    }
                }
            }
            buffers.endBatch(type);
        }

        VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.SCARLET_WITCH && data.beamActive()) {
                renderBeam(mc, player, handPosition(mc, player, partialTick), CHAOS, pose, beams, camera, partialTick, time);
            }
            if (data.power() == Power.VISION && data.beamActive()) {
                Palette palette = "white_vision".equals(data.variant()) ? WHITE_VISION : MIND_STONE;
                renderBeam(mc, player, foreheadPosition(mc, player, partialTick), palette, pose, beams, camera, partialTick, time);
            }
            if (data.heldEntityId() != -1) {
                Entity held = level.getEntity(data.heldEntityId());
                if (held != null) {
                    renderTether(mc, player, held, data.power(), pose, beams, camera, partialTick, time);
                }
            }
            if (data.power() == Power.MAGNETO && data.magneticShield() && !player.isInvisible()) {
                renderMagneticField(mc, player, pose, beams, camera, partialTick, time);
            }
        }
        renderHexBolts(mc, level, pose, beams, camera, partialTick, time);
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    // ---- The Hex ----

    private static void renderHexes(ClientLevel level, Matrix4f pose, VertexConsumer vc, boolean lines, Vec3 camera, float time,
                                    double renderDistance) {
        for (Hex hex : level.getData(ModAttachments.HEXES).hexes()) {
            float grow = Mth.clamp((time - hex.createdAt()) / 50F, 0F, 1F);
            grow = grow * grow * (3 - 2 * grow);
            float radius = hex.radius() * grow;
            if (radius < 0.5F) {
                continue;
            }
            Vec3 center = hex.center().subtract(camera);
            double distance = center.length();
            if (distance - radius > renderDistance + 32 || radius - distance > renderDistance + 32) {
                continue;
            }
            int segments = Mth.clamp((int) (radius * 2), 32, 128);
            float pulse = 0.85F + 0.15F * Mth.sin(time * 0.06F);
            // A wall that is still rising glows brighter.
            float rising = 1F + (1F - grow) * 1.5F;
            if (lines) {
                float tilesU = Math.max(6, Math.round(Mth.TWO_PI * radius / 6F));
                float tilesV = Math.max(3, Math.round(Mth.PI * radius / 6F));
                EnergyGeometry.sphere(pose, vc, center, radius, segments, segments / 2, tilesU, tilesV, time * 0.0015F,
                        HEX_LINES, 0.32F * pulse * rising, 0.68F * pulse * rising);
            } else {
                // Dense enough to tint the sky behind it raspberry red, as in the show.
                EnergyGeometry.veil(pose, vc, center, radius, segments, segments / 2, HEX_BODY,
                        0.36F * pulse, 0.72F * pulse * rising);
            }
        }
    }

    // ---- Beams (Chaos Beam, Mind Stone beam) ----

    /** Colours of a beam: wide glow, inner glow, hot core, crackling bolts, impact flare. */
    private record Palette(int glow, int core, int bolt) {
    }

    /** Chaos magic: crimson, hot pink wisps, a near-white core (WandaVision, Multiverse of Madness). */
    private static final Palette CHAOS = new Palette(Power.SCARLET_WITCH.color, 0xFFE3E6, 0xF07C8D);
    /** The Mind Stone's beam: pale gold with a cream-white core (Age of Ultron). */
    private static final Palette MIND_STONE = new Palette(0xF5D77A, 0xFFF6D5, 0xF2C66A);
    /** White Vision's beam: pale cyan-white (WandaVision episode 9). */
    private static final Palette WHITE_VISION = new Palette(0x7EBADF, 0xE8FBFF, 0xA6E2F0);

    private static void renderBeam(Minecraft mc, AbstractClientPlayer player, Vec3 origin, Palette palette, Matrix4f pose,
                                   VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        double range = EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_BEAM_RANGE.get() : 32;
        Vec3 end = Raycasts.any(player, range, WandaAbilities.BEAM_TARGET, partialTick).getLocation().subtract(camera);
        Vec3 start = origin.subtract(camera);

        float flicker = 0.85F + 0.15F * Mth.sin(time * 1.7F) * Mth.cos(time * 0.9F);
        // The beam starts thin at its source and swells a little towards the target, so it never fills the screen in first
        // person; a source right in front of the camera is drawn thinner still.
        float near = (float) Mth.clamp(start.length() / 1.5, 0.25, 1.0);
        EnergyGeometry.ribbon(pose, vc, start, end, 0.12F * near, 0.55F * flicker, EnergyGeometry.argb(palette.glow, 0.10F), EnergyGeometry.argb(palette.glow, 0.30F));
        EnergyGeometry.ribbon(pose, vc, start, end, 0.06F * near, 0.26F * flicker, EnergyGeometry.argb(palette.glow, 0.45F), EnergyGeometry.argb(palette.glow, 0.55F));
        EnergyGeometry.ribbon(pose, vc, start, end, 0.02F * near, 0.08F, EnergyGeometry.argb(palette.core, 0.75F), EnergyGeometry.argb(palette.core, 0.85F));

        long seed = ((long) time / 2) * 341873128712L + player.getId() * 132897987541L;
        for (int k = 0; k < 3; k++) {
            EnergyGeometry.bolt(pose, vc, start, end, 0.03F, 0.28F, EnergyGeometry.argb(palette.bolt, 0.8F), seed + k * 7919L);
        }
        // A flare where the beam hits.
        float flare = 0.5F + 0.2F * Mth.sin(time * 2.3F);
        Vec3 toCamera = end.scale(-1).normalize();
        Vec3 side = toCamera.cross(UP);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(toCamera).normalize();
        EnergyGeometry.ribbon(pose, vc, end.subtract(side.scale(flare)), end.add(side.scale(flare)), flare, EnergyGeometry.argb(palette.glow, 0.7F), EnergyGeometry.argb(palette.glow, 0.7F));
        EnergyGeometry.ribbon(pose, vc, end.subtract(up.scale(flare)), end.add(up.scale(flare)), flare * 0.5F, EnergyGeometry.argb(palette.core, 0.6F), EnergyGeometry.argb(palette.core, 0.6F));
    }

    // ---- Magnetic Shield ----

    /**
     * Magneto's field while the shield is on: faint looping field lines around him (like iron filings round a magnet),
     * with a glint running along each, and a shimmer around every projectile hanging in the field.
     */
    private static void renderMagneticField(Minecraft mc, AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                                            float partialTick, float time) {
        Vec3 center = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0);
        Vec3 c = center.subtract(camera);
        int lines = 7;
        int steps = 28;
        float[] shells = {2.4F, (float) MagnetoAbilities.SHIELD_RADIUS};
        for (int shell = 0; shell < shells.length; shell++) {
            float l = shells[shell];
            for (int k = 0; k < lines; k++) {
                double phi = Mth.TWO_PI * k / lines + time * (shell == 0 ? 0.006 : -0.004);
                double cos = Math.cos(phi);
                double sin = Math.sin(phi);
                float glint = ((time * 0.015F + k * 0.37F + shell * 0.5F) % 1.0F);
                Vec3 prev = null;
                for (int i = 0; i <= steps; i++) {
                    // A dipole field line: r = L sin^2(theta), from near the top pole round to near the bottom one.
                    double theta = 0.12 * Math.PI + 0.76 * Math.PI * i / steps;
                    double r = l * Math.sin(theta) * Math.sin(theta);
                    Vec3 point = c.add(r * Math.sin(theta) * cos, r * Math.cos(theta), r * Math.sin(theta) * sin);
                    if (prev != null) {
                        float t = (float) i / steps;
                        float fade = Mth.sin(t * Mth.PI); // fades out towards the poles
                        float near = Math.max(0.0F, 1.0F - Math.abs(t - glint) * 9.0F);
                        int color = EnergyGeometry.argb(Power.MAGNETO.glow, (0.10F + 0.45F * near) * fade);
                        EnergyGeometry.ribbon(pose, vc, prev, point, 0.035F + 0.03F * near, color, color);
                    }
                    prev = point;
                }
            }
        }
        // Projectiles hanging in the field: a shimmering ring round each and a faint line of force to his hand.
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(MagnetoAbilities.SHIELD_RADIUS + 2.0),
                p -> p.isNoGravity() && p.getDeltaMovement().lengthSqr() < 1.0E-6 && MagnetoAbilities.isMetalProjectile(p))) {
            Vec3 at = projectile.getPosition(partialTick).add(0, projectile.getBbHeight() * 0.5, 0).subtract(camera);
            float pulse = 0.28F + 0.06F * Mth.sin(time * 0.5F + projectile.getId());
            EnergyGeometry.ring(pose, vc, at, at.scale(-1).normalize(), pulse, 0.03F, 16, EnergyGeometry.argb(Power.MAGNETO.glow, 0.7F));
            long seed = ((long) time / 3) * 2862933555777941757L + projectile.getId();
            EnergyGeometry.bolt(pose, vc, handPosition(mc, player, partialTick).subtract(camera), at, 0.018F, 0.12F,
                    EnergyGeometry.argb(Power.MAGNETO.glow, 0.35F), seed);
        }
    }

    // ---- Billy's Hex Bolts ----

    /** How long a Hex Bolt stays on screen. */
    private static final int HEX_BOLT_TICKS = 6;

    private record HexBolt(int caster, Vec3 from, Vec3 to, List<Vec3> forks, long seed, long born) {
    }

    private static final List<HexBolt> HEX_BOLTS = new ArrayList<>();

    /** The server says a Hex Bolt was cast nearby. */
    public static void addHexBolt(HexBoltPayload bolt) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            HeroAnimator.onHexBolt(bolt.caster(), bolt.to());
            HEX_BOLTS.add(new HexBolt(bolt.caster(), bolt.from(), bolt.to(), List.copyOf(bolt.forks()), bolt.seed(), level.getGameTime()));
        }
    }

    public static void clearHexBolts() {
        HEX_BOLTS.clear();
    }

    /**
     * Every Hex Bolt still crackling: a jagged arc from Billy's hand to what it struck, with short branches, and an arc on
     * to each creature it forked to. It reshapes every other tick and fades out.
     */
    private static void renderHexBolts(Minecraft mc, ClientLevel level, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick,
                                       float time) {
        HEX_BOLTS.removeIf(b -> time - b.born() > HEX_BOLT_TICKS || time < b.born() - 1);
        for (HexBolt bolt : HEX_BOLTS) {
            float age = Math.max(0.0F, time - bolt.born());
            float fade = 1.0F - age / HEX_BOLT_TICKS;
            Entity caster = level.getEntity(bolt.caster());
            Vec3 from = (caster instanceof AbstractClientPlayer player ? handPosition(mc, player, partialTick) : bolt.from()).subtract(camera);
            Vec3 to = bolt.to().subtract(camera);
            RandomSource random = RandomSource.create(bolt.seed() + (long) (age / 2) * 7919L);
            // Thinner where it leaves a hand right in front of the camera, so it never fills the screen.
            float near = (float) Mth.clamp(from.length() / 1.5, 0.3, 1.0);
            lightning(pose, vc, from, to, near, fade, random, 2 + random.nextInt(3));
            for (Vec3 fork : bolt.forks()) {
                lightning(pose, vc, to, fork.subtract(camera), 0.7F, fade, random, 1);
            }
            float flare = 0.35F * fade;
            EnergyGeometry.ring(pose, vc, to, to.scale(-1), flare, 0.12F, 12, EnergyGeometry.argb(BillyAbilities.MAIN, 0.5F * fade));
            EnergyGeometry.ring(pose, vc, to, to.scale(-1), flare * 0.4F, 0.08F, 8, EnergyGeometry.argb(BillyAbilities.CORE, 0.7F * fade));
        }
    }

    /** One arc of Billy's lightning, with a few short branches crackling off it. */
    private static void lightning(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float scale, float fade, RandomSource random, int branches) {
        double length = a.distanceTo(b);
        if (length < 1.0E-3) {
            return;
        }
        List<Vec3> path = EnergyGeometry.lightningPath(a, b, Math.max(0.15, length * 0.08), random);
        boltLayers(pose, vc, path, scale, fade);
        Vec3 dir = b.subtract(a).scale(1.0 / length);
        for (int i = 0; i < branches && path.size() > 2; i++) {
            Vec3 start = path.get(1 + random.nextInt(path.size() - 2));
            double reach = 0.4 + random.nextDouble() * Math.min(1.8, length * 0.2);
            Vec3 side = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
            Vec3 end = start.add(dir.scale(reach * 0.6)).add(side.scale(reach * 0.8));
            boltLayers(pose, vc, EnergyGeometry.lightningPath(start, end, reach * 0.2, random), scale * 0.5F, fade * 0.8F);
        }
    }

    /** Billy's magic in layers: the dark blue fringe outside, then blue, light blue, and a white-hot core. */
    private static void boltLayers(Matrix4f pose, VertexConsumer vc, List<Vec3> path, float scale, float fade) {
        EnergyGeometry.polyline(pose, vc, path, 0.34F * scale, EnergyGeometry.argb(BillyAbilities.FRINGE, 0.22F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.18F * scale, EnergyGeometry.argb(BillyAbilities.BODY, 0.45F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.09F * scale, EnergyGeometry.argb(BillyAbilities.MAIN, 0.8F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.045F * scale, EnergyGeometry.argb(BillyAbilities.INNER, 0.9F * fade));
        EnergyGeometry.polyline(pose, vc, path, 0.02F * scale, EnergyGeometry.argb(BillyAbilities.CORE, fade));
    }

    // ---- Telekinesis ----

    private static void renderTether(Minecraft mc, AbstractClientPlayer player, Entity held, Power power, Matrix4f pose,
                                     VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        Vec3 start = handPosition(mc, player, partialTick).subtract(camera);
        Vec3 end = held.getPosition(partialTick).add(0, held.getBbHeight() * 0.5, 0).subtract(camera);
        long seed = ((long) time / 3) * 6364136223846793005L + player.getId();
        EnergyGeometry.bolt(pose, vc, start, end, 0.04F, 0.35F, EnergyGeometry.argb(power.color, 0.7F), seed);
        EnergyGeometry.bolt(pose, vc, start, end, 0.025F, 0.5F, EnergyGeometry.argb(power.glow, 0.4F), seed + 31);
    }

    /** Vision's Mind Stone, in the middle of his forehead (just above the screen centre in first person). */
    public static Vec3 foreheadPosition(Minecraft mc, AbstractClientPlayer player, float partialTick) {
        Vec3 tracked = HandTracker.forehead(player, partialTick);
        if (tracked != null) {
            return tracked;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            // A little way out from the top of the view, so the beam is seen pouring forward rather than end-on.
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            return eye.add(look.scale(0.9)).add(up.scale(0.24));
        }
        return BodyLean.follow(player, eye, partialTick).add(look.scale(0.28)).add(0, 0.18, 0);
    }

    /** Where energy leaves the player's hands: in front of the chest, or bottom-right of the screen in first person. */
    public static Vec3 handPosition(Minecraft mc, AbstractClientPlayer player, float partialTick) {
        Vec3 tracked = HandTracker.hand(player, player.getMainArm(), partialTick);
        if (tracked != null) {
            return tracked;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            // Where the hand is drawn: its resting place, moved with the first-person gesture (the view-space offset is
            // about 0.8 blocks from the camera, so 0.4 of it is the offset in the world).
            float[] hand = new float[3];
            if (player == mc.player) {
                FirstPersonArms.mainHand(mc.player, hand);
            }
            double side = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0 : -1.0;
            return eye.add(look.scale(0.55 + 0.4 * hand[2])).add(right.scale(side * (0.2 + 0.4 * hand[0]))).add(up.scale(-0.22 + 0.4 * hand[1]));
        }
        return BodyLean.follow(player, eye, partialTick).add(0, -0.3, 0).add(look.scale(0.75));
    }

    private WorldEffectsRenderer() {
    }
}
